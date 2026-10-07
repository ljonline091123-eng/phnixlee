"""Persistent quality controls and controlled Agent/Skill audit APIs."""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Any
from uuid import uuid4

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.ai_hub import (
    AgentDataAsset,
    AgentDefinition,
)
from app.models.governance import (
    AgentExecutionRun,
    DataQualityIssue,
    DataQualityRule,
    DataQualityRun,
    SkillExecutionRun,
)
from app.models.lakehouse import LakeDataset, LakeDatasetVersion
from app.models.market_data import DataSource
from app.schemas.governance import (
    DataQualityIssueResolve,
    DataQualityRuleCreate,
    DataQualityRuleRead,
    DataQualityRuleUpdate,
    DataQualityRunRequest,
)
from app.services import lakehouse

router = APIRouter(prefix="/governance", tags=["Data and AI Governance"])


def _now() -> datetime:
    return datetime.now(timezone.utc)


def _limit(value: int) -> int:
    if value < 1 or value > 500:
        raise HTTPException(status_code=422, detail="limit must be between 1 and 500")
    return value


def _quality_run_read(row: DataQualityRun) -> dict[str, Any]:
    return {
        "id": row.id, "run_key": row.run_key, "target_type": row.target_type,
        "target_id": row.target_id, "target_version": row.target_version,
        "pipeline_run_id": row.pipeline_run_id, "status": row.status, "score": row.score,
        "total_rules": row.total_rules, "passed_rules": row.passed_rules,
        "failed_rules": row.failed_rules, "warning_rules": row.warning_rules,
        "summary": row.summary_json or {}, "started_at": row.started_at,
        "completed_at": row.completed_at, "created_at": row.created_at,
    }


def _issue_read(row: DataQualityIssue) -> dict[str, Any]:
    return {
        "id": row.id, "quality_run_id": row.quality_run_id, "rule_id": row.rule_id,
        "rule_code": row.rule_code, "target_type": row.target_type, "target_id": row.target_id,
        "issue_code": row.issue_code, "severity": row.severity, "status": row.status,
        "message": row.message, "observed_value": row.observed_value_json,
        "expected_value": row.expected_value_json, "evidence_ids": row.evidence_ids_json or [],
        "details": row.details_json or {}, "first_detected_at": row.first_detected_at,
        "resolved_at": row.resolved_at, "resolution": row.resolution,
    }


def _agent_execution_read(row: AgentExecutionRun) -> dict[str, Any]:
    return {
        "id": row.id, "run_key": row.run_key, "parent_run_id": row.parent_run_id,
        "pipeline_run_id": row.pipeline_run_id, "pipeline_stage_run_id": row.pipeline_stage_run_id,
        "agent_id": row.agent_id, "agent_code": row.agent_code, "agent_version": row.agent_version,
        "model_instance_code": row.model_instance_code, "model_version": row.model_version,
        "model_call_log_id": row.model_call_log_id, "trigger_type": row.trigger_type,
        "status": row.status, "correlation_id": row.correlation_id,
        "idempotency_key": row.idempotency_key, "input_hash": row.input_hash,
        "output_hash": row.output_hash, "input": row.input_json or {}, "output": row.output_json or {},
        "accessible_assets": row.accessible_assets_json or [],
        "knowledge_versions": row.knowledge_versions_json or {},
        "skill_versions": row.skill_versions_json or {}, "evidence_ids": row.evidence_ids_json or [],
        "audit": row.audit_json or {}, "error_code": row.error_code,
        "error_message": row.error_message, "started_at": row.started_at,
        "completed_at": row.completed_at, "created_at": row.created_at,
    }


def _skill_execution_read(row: SkillExecutionRun) -> dict[str, Any]:
    return {
        "id": row.id, "agent_execution_run_id": row.agent_execution_run_id,
        "pipeline_stage_run_id": row.pipeline_stage_run_id, "skill_id": row.skill_id,
        "skill_code": row.skill_code, "skill_version": row.skill_version,
        "status": row.status, "attempt": row.attempt, "max_attempts": row.max_attempts,
        "idempotency_key": row.idempotency_key, "input_hash": row.input_hash,
        "output_hash": row.output_hash, "input": row.input_json or {}, "output": row.output_json or {},
        "side_effect_level": row.side_effect_level, "write_scope": row.write_scope_json or {},
        "evidence_ids": row.evidence_ids_json or [], "model_call_log_id": row.model_call_log_id,
        "error_code": row.error_code, "error_message": row.error_message,
        "started_at": row.started_at, "completed_at": row.completed_at, "created_at": row.created_at,
    }


@router.get("/quality/rules", response_model=list[DataQualityRuleRead])
def list_quality_rules(asset_scope: str | None = None, enabled: bool | None = None,
                       db: Session = Depends(get_db)) -> list[DataQualityRule]:
    query = select(DataQualityRule).order_by(DataQualityRule.rule_code, DataQualityRule.version.desc())
    if asset_scope:
        query = query.where(DataQualityRule.asset_scope == asset_scope)
    if enabled is not None:
        query = query.where(DataQualityRule.enabled == enabled)
    return list(db.scalars(query).all())


@router.post("/quality/rules", response_model=DataQualityRuleRead, status_code=status.HTTP_201_CREATED)
def create_quality_rule(payload: DataQualityRuleCreate, db: Session = Depends(get_db)) -> DataQualityRule:
    row = DataQualityRule(**payload.model_dump())
    db.add(row)
    try:
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        raise HTTPException(status_code=409, detail="Quality rule code and version already exist") from exc
    db.refresh(row)
    return row


@router.put("/quality/rules/{rule_id}", response_model=DataQualityRuleRead)
def update_quality_rule(rule_id: int, payload: DataQualityRuleUpdate,
                        db: Session = Depends(get_db)) -> DataQualityRule:
    row = db.get(DataQualityRule, rule_id)
    if row is None:
        raise HTTPException(status_code=404, detail="Quality rule not found")
    values = payload.model_dump(exclude_unset=True)
    if values.get("lifecycle_status") == "ENABLED":
        values["enabled"] = True
    if values.get("lifecycle_status") in {"DISABLED", "DEPRECATED"}:
        values["enabled"] = False
    for key, value in values.items():
        setattr(row, key, value)
    db.commit()
    db.refresh(row)
    return row


def _dataset_quality(db: Session, payload: DataQualityRunRequest) -> tuple[str, dict[str, Any]]:
    dataset = db.scalar(select(LakeDataset).where(LakeDataset.dataset_code == payload.target_id))
    if dataset is None and payload.target_id.isdigit():
        dataset = db.get(LakeDataset, int(payload.target_id))
    if dataset is None:
        raise HTTPException(status_code=404, detail="Lake dataset not found")
    version = db.scalar(select(LakeDatasetVersion).where(
        LakeDatasetVersion.dataset_id == dataset.id,
        LakeDatasetVersion.version == payload.target_version,
    ))
    if version is None:
        raise HTTPException(status_code=404, detail="Lake dataset version not found")
    result = dict(version.quality_json or {})
    result.update({
        "dataset_code": dataset.dataset_code,
        "dataset_version": version.version,
        "dataset_status": version.status,
        "row_count": version.row_count,
    })
    return str(version.id), result


def _source_quality(db: Session, payload: DataQualityRunRequest) -> tuple[str, dict[str, Any]]:
    asset = db.scalar(select(AgentDataAsset).where(AgentDataAsset.table_name == payload.target_id))
    if asset is None or not asset.enabled:
        raise HTTPException(status_code=404, detail="Enabled governed data asset not found")
    try:
        result = lakehouse.assess_dataset_source(
            db, source_table=payload.target_id, layer=payload.layer, limit=payload.limit,
        )
    except (ValueError, RuntimeError) as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    return payload.target_id, result


def _quality_counts(result: dict[str, Any]) -> tuple[int, int, int, int]:
    checks = result.get("checks") if isinstance(result.get("checks"), dict) else {}
    total = len(checks)
    passed = sum(1 for value in checks.values() if value is True)
    failed = sum(1 for value in checks.values() if value is False)
    warnings = 1 if result.get("level") == "WARNING" else 0
    if total == 0:
        total = 1
        passed = int(bool(result.get("passed")))
        failed = int(not bool(result.get("passed")))
    return total, passed, failed, warnings


def _evaluate_quality_rule(rule: DataQualityRule, result: dict[str, Any]) -> dict[str, Any]:
    definition = dict(rule.definition_json or {})
    threshold = dict(rule.threshold_json or {})
    evaluator = str(definition.get("evaluator") or rule.rule_type or "").strip().upper()
    row_count = int(result.get("row_count") or 0)
    passed = False
    details: dict[str, Any] = {"evaluator": evaluator}
    message = f"质量规则 {rule.rule_name} 未通过"

    if evaluator in {"BUILTIN_CONTRACT", "QUALITY_GATE"}:
        passed = bool(result.get("passed"))
        details["base_level"] = result.get("level")
    elif evaluator == "REQUIRED_FIELDS":
        fields = [str(item) for item in definition.get("fields", []) if str(item).strip()]
        null_counts = result.get("null_counts") if isinstance(result.get("null_counts"), dict) else {}
        missing_columns = [field for field in fields if field not in null_counts]
        incomplete = {field: int(null_counts.get(field) or 0) for field in fields if int(null_counts.get(field) or 0) > 0}
        passed = bool(fields) and not missing_columns and not incomplete
        details.update(fields=fields, missing_columns=missing_columns, incomplete_counts=incomplete)
        if not fields:
            message = f"质量规则 {rule.rule_name} 未配置必填字段"
    elif evaluator == "MIN_ROW_COUNT":
        minimum = int(threshold.get("minimum", definition.get("minimum", 1)))
        passed = row_count >= minimum
        details.update(row_count=row_count, minimum=minimum)
    elif evaluator == "MAX_NULL_RATE":
        fields = [str(item) for item in definition.get("fields", []) if str(item).strip()]
        maximum = float(threshold.get("maximum", definition.get("maximum", 0)))
        null_counts = result.get("null_counts") if isinstance(result.get("null_counts"), dict) else {}
        rates = {
            field: (float(null_counts.get(field, row_count)) / row_count if row_count else 1.0)
            for field in fields
        }
        passed = bool(fields) and all(field in null_counts and rate <= maximum for field, rate in rates.items())
        details.update(fields=fields, maximum=maximum, rates=rates)
    elif evaluator == "FACT_EVIDENCE_LINK":
        minimum = int(definition.get("minimum_evidence", 1))
        traceable = int(result.get("traceable_record_count") or 0)
        # The aggregate source-quality result counts a fact only when its
        # association-table evidence link can be resolved.
        passed = minimum <= 1 and row_count > 0 and traceable == row_count
        details.update(row_count=row_count, traceable_record_count=traceable, minimum_evidence=minimum)
    elif evaluator == "VERSION_QUALITY_STATUS":
        accepted = {str(item).upper() for item in definition.get("accepted", ["PUBLISHED"])}
        dataset_status = str(result.get("dataset_status") or "").upper()
        passed = dataset_status in accepted and bool(result.get("passed", True))
        details.update(dataset_status=dataset_status, accepted=sorted(accepted))
    else:
        message = f"质量规则 {rule.rule_name} 使用了不支持的执行器 {evaluator or 'EMPTY'}"
        details["configuration_error"] = True

    return {
        "rule_id": rule.id,
        "rule_code": rule.rule_code,
        "rule_name": rule.rule_name,
        "severity": rule.severity,
        "version": rule.version,
        "passed": passed,
        "message": message if not passed else f"质量规则 {rule.rule_name} 通过",
        "details": details,
    }


def _configured_quality_results(db: Session, payload: DataQualityRunRequest,
                                resolved_target: str, result: dict[str, Any]) -> list[dict[str, Any]]:
    aliases = {payload.target_id, resolved_target}
    scopes = {payload.target_type}
    if payload.target_type == "LAKE_DATASET_VERSION":
        aliases.update({str(result.get("dataset_code") or ""), "lake_dataset_version"})
        scopes.add("LAKE_DATASET_VERSION")
    rules = db.scalars(select(DataQualityRule).where(
        DataQualityRule.enabled.is_(True),
        DataQualityRule.lifecycle_status == "ENABLED",
    ).order_by(DataQualityRule.rule_code, DataQualityRule.version)).all()
    applicable = [
        rule for rule in rules
        if (rule.target_code and rule.target_code in aliases)
        or (not rule.target_code and rule.asset_scope in scopes)
    ]
    return [_evaluate_quality_rule(rule, result) for rule in applicable]


@router.post("/quality/runs")
def run_quality(payload: DataQualityRunRequest, db: Session = Depends(get_db)) -> dict[str, Any]:
    run_key = payload.idempotency_key or f"quality:{payload.target_type}:{uuid4().hex}"
    existing = db.scalar(select(DataQualityRun).where(DataQualityRun.run_key == run_key))
    if existing is not None:
        return _quality_run_read(existing)
    started_at = _now()
    resolved_target, result = (
        _source_quality(db, payload) if payload.target_type == "SOURCE_TABLE" else _dataset_quality(db, payload)
    )
    configured_results = _configured_quality_results(db, payload, resolved_target, result)
    result["configured_rules"] = configured_results
    total, passed, failed, warnings = _quality_counts(result)
    total += len(configured_results)
    passed += sum(1 for item in configured_results if item["passed"])
    failed_configured = [item for item in configured_results if not item["passed"]]
    failed += sum(1 for item in failed_configured if str(item["severity"]).upper() in {"ERROR", "CRITICAL"})
    warnings += sum(1 for item in failed_configured if str(item["severity"]).upper() in {"INFO", "WARNING"})
    # A hard contract may pass while the dataset still has traceability or
    # coverage warnings. Keep that distinction visible instead of presenting
    # a warning-level dataset as fully governed.
    status_value = (
        "FAILED" if not bool(result.get("passed")) or failed > 0
        else "PARTIAL" if warnings > 0 or str(result.get("level") or "").upper() in {"WARNING", "PARTIAL"}
        else "PASSED"
    )
    score = round((passed / total) * 100, 2) if total else None
    row = DataQualityRun(
        run_key=run_key, target_type=payload.target_type, target_id=resolved_target,
        target_version=payload.target_version or payload.layer, status=status_value, score=score,
        total_rules=total, passed_rules=passed, failed_rules=failed, warning_rules=warnings,
        summary_json=result, started_at=started_at, completed_at=_now(),
    )
    db.add(row)
    db.flush()
    samples = result.get("issue_samples") if isinstance(result.get("issue_samples"), list) else []
    if not result.get("passed") or samples:
        details = {"samples": samples[:50], "checks": result.get("checks", {})}
        db.add(DataQualityIssue(
            quality_run_id=row.id, rule_code="BUILTIN_DATASET_CONTRACT", target_type=payload.target_type,
            target_id=resolved_target, issue_code="QUALITY_GATE_NOT_PASSED" if not result.get("passed") else "QUALITY_WARNING",
            severity="ERROR" if not result.get("passed") else "WARNING", status="OPEN",
            message="数据质量门禁未通过" if not result.get("passed") else "数据质量检查存在警告",
            observed_value_json={"level": result.get("level"), "issue_count": result.get("issue_count", len(samples))},
            expected_value_json={"passed": True}, evidence_ids_json=[], details_json=details,
        ))
    for item in failed_configured:
        db.add(DataQualityIssue(
            quality_run_id=row.id,
            rule_id=item["rule_id"],
            rule_code=item["rule_code"],
            target_type=payload.target_type,
            target_id=resolved_target,
            issue_code="QUALITY_RULE_FAILED",
            severity=str(item["severity"]).upper(),
            status="OPEN",
            message=item["message"],
            observed_value_json=item["details"],
            expected_value_json={"passed": True},
            evidence_ids_json=[],
            details_json={"rule_version": item["version"]},
        ))
    db.commit()
    db.refresh(row)
    return _quality_run_read(row)


@router.get("/quality/runs")
def list_quality_runs(target_type: str | None = None, target_id: str | None = None,
                      status_filter: str | None = None, limit: int = 100,
                      db: Session = Depends(get_db)) -> list[dict[str, Any]]:
    query = select(DataQualityRun).order_by(DataQualityRun.created_at.desc()).limit(_limit(limit))
    if target_type:
        query = query.where(DataQualityRun.target_type == target_type)
    if target_id:
        query = query.where(DataQualityRun.target_id == target_id)
    if status_filter:
        query = query.where(DataQualityRun.status == status_filter.upper())
    return [_quality_run_read(row) for row in db.scalars(query).all()]


@router.get("/quality/runs/{run_id}")
def quality_run_detail(run_id: str, db: Session = Depends(get_db)) -> dict[str, Any]:
    row = db.get(DataQualityRun, run_id)
    if row is None:
        raise HTTPException(status_code=404, detail="Quality run not found")
    payload = _quality_run_read(row)
    payload["issues"] = [_issue_read(item) for item in db.scalars(
        select(DataQualityIssue).where(DataQualityIssue.quality_run_id == run_id)
        .order_by(DataQualityIssue.first_detected_at.desc())
    ).all()]
    return payload


@router.get("/quality/issues")
def list_quality_issues(status_filter: str | None = None, severity: str | None = None,
                        limit: int = 100, db: Session = Depends(get_db)) -> list[dict[str, Any]]:
    query = select(DataQualityIssue).order_by(DataQualityIssue.first_detected_at.desc()).limit(_limit(limit))
    if status_filter:
        query = query.where(DataQualityIssue.status == status_filter.upper())
    if severity:
        query = query.where(DataQualityIssue.severity == severity.upper())
    return [_issue_read(row) for row in db.scalars(query).all()]


@router.put("/quality/issues/{issue_id}")
def resolve_quality_issue(issue_id: str, payload: DataQualityIssueResolve,
                          db: Session = Depends(get_db)) -> dict[str, Any]:
    row = db.get(DataQualityIssue, issue_id)
    if row is None:
        raise HTTPException(status_code=404, detail="Quality issue not found")
    row.status = payload.status
    row.resolution = payload.resolution
    row.resolved_at = _now() if payload.status in {"RESOLVED", "IGNORED"} else None
    db.commit()
    db.refresh(row)
    return _issue_read(row)


@router.get("/agent-executions")
def list_agent_executions(agent_id: int | None = None, status_filter: str | None = None,
                          limit: int = 100, db: Session = Depends(get_db)) -> list[dict[str, Any]]:
    query = select(AgentExecutionRun).order_by(AgentExecutionRun.started_at.desc()).limit(_limit(limit))
    if agent_id is not None:
        query = query.where(AgentExecutionRun.agent_id == agent_id)
    if status_filter:
        query = query.where(AgentExecutionRun.status == status_filter.upper())
    return [_agent_execution_read(row) for row in db.scalars(query).all()]


@router.get("/agent-executions/{run_id}")
def agent_execution_detail(run_id: str, db: Session = Depends(get_db)) -> dict[str, Any]:
    row = db.get(AgentExecutionRun, run_id)
    if row is None:
        raise HTTPException(status_code=404, detail="Agent execution not found")
    payload = _agent_execution_read(row)
    payload["skill_executions"] = [_skill_execution_read(item) for item in db.scalars(
        select(SkillExecutionRun).where(SkillExecutionRun.agent_execution_run_id == run_id)
        .order_by(SkillExecutionRun.started_at)
    ).all()]
    return payload


@router.get("/skill-executions")
def list_skill_executions(skill_id: int | None = None, status_filter: str | None = None,
                          limit: int = 100, db: Session = Depends(get_db)) -> list[dict[str, Any]]:
    query = select(SkillExecutionRun).order_by(SkillExecutionRun.started_at.desc()).limit(_limit(limit))
    if skill_id is not None:
        query = query.where(SkillExecutionRun.skill_id == skill_id)
    if status_filter:
        query = query.where(SkillExecutionRun.status == status_filter.upper())
    return [_skill_execution_read(row) for row in db.scalars(query).all()]


@router.get("/agents/{agent_id}/manifest")
def agent_manifest(agent_id: int, db: Session = Depends(get_db)) -> dict[str, Any]:
    agent = db.get(AgentDefinition, agent_id)
    if agent is None:
        raise HTTPException(status_code=404, detail="Agent not found")
    skills = [link.skill for link in agent.skill_links]
    knowledge_bases = [link.knowledge_base for link in agent.knowledge_links]
    assets = [link.data_asset for link in agent.data_asset_links]
    source_ids = [link.data_source_id for link in agent.data_source_links]
    data_sources = list(db.scalars(
        select(DataSource).where(DataSource.id.in_(source_ids)).order_by(DataSource.priority, DataSource.source_code)
    ).all()) if source_ids else []
    return {
        "agent": {"id": agent.id, "agent_code": agent.agent_code, "display_name": agent.display_name,
                  "version": agent.version, "lifecycle_status": getattr(agent, "lifecycle_status", "ENABLED"),
                  "model_instance_code": agent.model_instance_code,
                  "policy": getattr(agent, "policy_json", {}) or {},
                  "knowledge_version_policy": getattr(agent, "knowledge_version_policy_json", {}) or {}},
        "skills": [{"id": item.id, "skill_code": item.skill_code, "skill_name": item.skill_name,
                    "version": item.version, "lifecycle_status": getattr(item, "lifecycle_status", "ENABLED"),
                    "input_contract": getattr(item, "input_contract_json", {}) or {},
                    "output_contract": getattr(item, "output_contract_json", {}) or {},
                    "permission_policy": getattr(item, "permission_policy_json", {}) or {},
                    "side_effect_level": getattr(item, "side_effect_level", "READ_ONLY"),
                    "idempotency_policy": getattr(item, "idempotency_policy", "NONE"),
                    "retry_policy": getattr(item, "retry_policy_json", {}) or {},
                    "error_policy": getattr(item, "error_policy_json", {}) or {}} for item in skills],
        "assets": [{"id": item.id, "asset_code": item.asset_code, "display_name": item.display_name,
                    "asset_type": getattr(item, "asset_type", "TABLE"),
                    "canonical_identity": getattr(item, "canonical_identity", None),
                    "governance_status": item.governance_status} for item in assets],
        "knowledge_bases": [{"id": item.id, "kb_code": item.kb_code, "kb_name": item.kb_name,
                             "version": item.version, "status": item.status} for item in knowledge_bases],
        "data_sources": [{"id": item.id, "source_code": item.source_code,
                          "source_name": item.source_name, "source_type": item.source_type,
                          "enabled": item.enabled} for item in data_sources],
    }
