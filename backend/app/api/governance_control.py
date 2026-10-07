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
    ModelSkill,
)
from app.models.governance import (
    AgentExecutionRun,
    DataQualityIssue,
    DataQualityRule,
    DataQualityRun,
    SkillExecutionRun,
)
from app.models.lakehouse import (
    DocumentChunkVersion,
    LakeDataset,
    LakeDatasetVersion,
    LakeLineageEvent,
    LakeObject,
)
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

SUPPORTED_QUALITY_EVALUATORS = {
    "BUILTIN_CONTRACT",
    "QUALITY_GATE",
    "REQUIRED_FIELDS",
    "MIN_ROW_COUNT",
    "MAX_NULL_RATE",
    "FACT_EVIDENCE_LINK",
    "VERSION_QUALITY_STATUS",
    "CHUNK_LINEAGE",
    "HASH_VECTOR_KIND",
    "SKILL_CONTRACT",
}
QUALITY_TARGET_CODES = {
    "LAKE_OBJECT": "lake_object",
    "DOCUMENT_CHUNK": "document_chunk_version",
    "MODEL_SKILL": "model_skill",
    "LAKE_DATASET_VERSION": "lake_dataset_version",
}


def _now() -> datetime:
    return datetime.now(timezone.utc)


def _limit(value: int) -> int:
    if value < 1 or value > 500:
        raise HTTPException(status_code=422, detail="limit must be between 1 and 500")
    return value


def _blank(value: Any) -> bool:
    return value is None or isinstance(value, str) and not value.strip() or isinstance(value, (dict, list)) and not value


def _quality_rule_evaluator(values: dict[str, Any]) -> str:
    definition = values.get("definition_json") if isinstance(values.get("definition_json"), dict) else {}
    return str(definition.get("evaluator") or values.get("rule_type") or "").strip().upper()


def _validate_enabled_quality_rule(values: dict[str, Any]) -> None:
    if values.get("lifecycle_status") != "ENABLED" and values.get("enabled") is not True:
        return
    evaluator = _quality_rule_evaluator(values)
    if evaluator not in SUPPORTED_QUALITY_EVALUATORS:
        raise HTTPException(status_code=422, detail=f"Unsupported quality evaluator: {evaluator or 'EMPTY'}")
    scope = str(values.get("asset_scope") or "").strip().upper()
    target_code = str(values.get("target_code") or "").strip()
    supported_scopes = {"SOURCE_TABLE", *QUALITY_TARGET_CODES}
    if scope not in supported_scopes:
        raise HTTPException(status_code=422, detail=f"Unsupported enabled quality asset_scope: {scope or 'EMPTY'}")
    if scope == "SOURCE_TABLE" and target_code and target_code not in lakehouse.SUPPORTED_EXPORT_TABLES:
        raise HTTPException(status_code=422, detail=f"Unsupported governed source table: {target_code}")
    expected = QUALITY_TARGET_CODES.get(scope)
    if expected and target_code and target_code != expected:
        raise HTTPException(
            status_code=422,
            detail=f"Quality scope {scope} requires target_code={expected}",
        )


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
    values = payload.model_dump()
    _validate_enabled_quality_rule(values)
    row = DataQualityRule(**values)
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
    merged = {
        "asset_scope": row.asset_scope,
        "target_code": row.target_code,
        "rule_type": row.rule_type,
        "definition_json": row.definition_json,
        "lifecycle_status": row.lifecycle_status,
        "enabled": row.enabled,
        **values,
    }
    _validate_enabled_quality_rule(merged)
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
    if "passed" not in result:
        result.update({
            "passed": True,
            "level": "WARNING",
            "checks": {"version_catalog_present": True},
            "quality_snapshot_present": False,
            "warnings": ["历史数据集版本缺少质量快照，已标记为PARTIAL并等待重新发布"],
        })
    result.update({
        "dataset_code": dataset.dataset_code,
        "dataset_version": version.version,
        "dataset_status": version.status,
        "row_count": version.row_count,
    })
    return dataset.dataset_code, result


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


def _lake_object_quality(db: Session, payload: DataQualityRunRequest) -> tuple[str, dict[str, Any]]:
    if payload.target_id == "lake_object":
        rows = list(db.scalars(select(LakeObject).order_by(LakeObject.created_at).limit(payload.limit)).all())
    else:
        row = db.get(LakeObject, payload.target_id)
        if row is None:
            raise HTTPException(status_code=404, detail="Lake object not found")
        rows = [row]
    fields = ("content_hash", "object_uri", "layer", "created_at")
    null_counts = {field: sum(_blank(getattr(row, field, None)) for row in rows) for field in fields}
    object_ids = [row.id for row in rows]
    lineage_ids: set[str] = set()
    for offset in range(0, len(object_ids), 500):
        lineage_ids.update(str(item) for item in db.scalars(select(LakeLineageEvent.downstream_id).where(
            LakeLineageEvent.downstream_type == "LAKE_OBJECT",
            LakeLineageEvent.downstream_id.in_(object_ids[offset:offset + 500]),
        )).all())
    raw_missing = []
    for row in rows:
        if str(row.layer or "").upper() != "RAW":
            continue
        has_source = bool(row.source_code or row.source_table)
        has_identity = not _blank(row.source_record_id) or not _blank(row.dataset_version)
        has_lineage = row.id in lineage_ids
        issues = []
        if not has_source:
            issues.append("RAW_SOURCE_MISSING")
        if not has_identity:
            issues.append("RAW_SOURCE_IDENTITY_MISSING")
        if not has_lineage:
            issues.append("RAW_LINEAGE_MISSING")
        if issues:
            raw_missing.append((row, issues))
    required_complete = not any(null_counts.values())
    raw_complete = not raw_missing
    samples = [{
        "object_id": row.id,
        "object_uri": row.object_uri,
        "issues": issues,
    } for row, issues in raw_missing[:20]]
    passed = required_complete and raw_complete
    return "lake_object", {
        "passed": passed,
        "level": "PASS" if passed else "FAILED",
        "contract_version": "LAKE_OBJECT_QUALITY_V1",
        "row_count": len(rows),
        "checks": {
            "catalog_fields_complete": required_complete,
            "raw_provenance_complete": raw_complete,
        },
        "null_counts": null_counts,
        "raw_object_count": sum(str(row.layer or "").upper() == "RAW" for row in rows),
        "raw_source_missing_count": sum("RAW_SOURCE_MISSING" in issues for _row, issues in raw_missing),
        "raw_identity_missing_count": sum("RAW_SOURCE_IDENTITY_MISSING" in issues for _row, issues in raw_missing),
        "raw_lineage_missing_count": sum("RAW_LINEAGE_MISSING" in issues for _row, issues in raw_missing),
        "issue_samples": samples,
    }


def _chunk_quality(db: Session, payload: DataQualityRunRequest) -> tuple[str, dict[str, Any]]:
    if payload.target_id == "document_chunk_version":
        rows = list(db.scalars(
            select(DocumentChunkVersion).order_by(DocumentChunkVersion.created_at).limit(payload.limit)
        ).all())
    else:
        row = db.get(DocumentChunkVersion, payload.target_id)
        if row is None:
            raise HTTPException(status_code=404, detail="Document chunk not found")
        rows = [row]
    chunk_ids = [row.id for row in rows]
    lineage_ids: set[str] = set()
    for offset in range(0, len(chunk_ids), 500):
        lineage_ids.update(str(item) for item in db.scalars(select(LakeLineageEvent.downstream_id).where(
            LakeLineageEvent.downstream_type == "DOCUMENT_CHUNK",
            LakeLineageEvent.downstream_id.in_(chunk_ids[offset:offset + 500]),
        )).all())
    active = [row for row in rows if str(row.status or "").upper() != "QUARANTINED"]
    quarantined = [row for row in rows if str(row.status or "").upper() == "QUARANTINED"]
    missing_lineage = [
        row for row in active
        if _blank(row.source_object_id) or _blank(row.lineage_batch_id) or row.id not in lineage_ids
    ]
    hash_mismatches = [
        row for row in active
        if str(row.embedding_model or "").upper().startswith("HASH")
        and (row.embedding_kind != "HASH" or row.embedding_status != "HASH_ONLY")
    ]
    semantic_ready_without_vector = []
    for row in active:
        if row.embedding_kind != "SEMANTIC" or row.embedding_status != "READY":
            continue
        metadata = dict(row.metadata_json or {})
        embedding = metadata.get("embedding") if isinstance(metadata.get("embedding"), dict) else {}
        vector = embedding.get("vector") if isinstance(embedding, dict) else None
        if not isinstance(vector, list) or not vector:
            semantic_ready_without_vector.append(row)
    unresolved_sections = [row for row in active if row.section_status in {None, "", "UNRESOLVED"}]
    missing_source = [row for row in rows if row.section_status == "MISSING_SOURCE"]
    pending_semantic = [row for row in active if row.embedding_kind == "SEMANTIC" and row.embedding_status == "PENDING"]
    checks = {
        "chunk_versions_present": all(not _blank(row.chunk_version) and not _blank(row.parser_version) for row in active),
        "active_lineage_complete": not missing_lineage,
        "hash_vector_labels_truthful": not hash_mismatches,
        "semantic_ready_vectors_verifiable": not semantic_ready_without_vector,
        "section_status_recorded": not unresolved_sections,
    }
    passed = all(checks.values())
    warning_count = len(quarantined) + len(missing_source) + len(pending_semantic)
    level = "FAILED" if not passed else "WARNING" if warning_count else "PASS"
    samples = []
    for row, issue in [
        *((row, "CHUNK_LINEAGE_MISSING") for row in missing_lineage[:20]),
        *((row, "HASH_VECTOR_LABEL_INVALID") for row in hash_mismatches[:20]),
        *((row, "SEMANTIC_VECTOR_MISSING") for row in semantic_ready_without_vector[:20]),
        *((row, "SECTION_STATUS_UNRESOLVED") for row in unresolved_sections[:20]),
        *((row, "CHUNK_QUARANTINED") for row in quarantined[:20]),
    ]:
        samples.append({"chunk_id": row.id, "document_key": row.document_key, "issues": [issue]})
    fields = ("chunk_version", "parser_version", "source_object_id", "lineage_batch_id")
    null_counts = {field: sum(_blank(getattr(row, field, None)) for row in active) for field in fields}
    return "document_chunk_version", {
        "passed": passed,
        "level": level,
        "contract_version": "DOCUMENT_CHUNK_QUALITY_V1",
        "row_count": len(rows),
        "active_row_count": len(active),
        "quarantined_count": len(quarantined),
        "checks": checks,
        "null_counts": null_counts,
        "lineage_complete_count": len(active) - len(missing_lineage),
        "missing_lineage_count": len(missing_lineage),
        "hash_vector_mismatch_count": len(hash_mismatches),
        "semantic_ready_without_vector_count": len(semantic_ready_without_vector),
        "semantic_pending_count": len(pending_semantic),
        "unresolved_section_count": len(unresolved_sections),
        "missing_source_count": len(missing_source),
        "warnings": (["存在隔离切片或缺失来源"] if quarantined or missing_source else [])
                    + (["存在待生成的语义向量"] if pending_semantic else []),
        "issue_samples": samples[:50],
    }


def _skill_contract_errors(row: ModelSkill) -> list[str]:
    errors = []
    for field in (
        "input_contract_json", "output_contract_json", "permission_policy_json",
        "side_effect_level", "idempotency_policy", "retry_policy_json", "error_policy_json",
    ):
        if _blank(getattr(row, field, None)):
            errors.append(f"MISSING:{field}")
    permissions = dict(row.permission_policy_json or {})
    if any(bool(permissions.get(name)) for name in ("arbitrary_sql", "unrestricted_database_write", "raw_database_write")):
        errors.append("UNSAFE_DATABASE_PERMISSION")
    if row.side_effect_level == "CONTROLLED_WRITE":
        scope = permissions.get("write_scope") if isinstance(permissions.get("write_scope"), dict) else {}
        if permissions.get("database_write") is not True:
            errors.append("DATABASE_WRITE_PERMISSION_MISSING")
        if not permissions.get("allowed_services"):
            errors.append("ALLOWED_SERVICES_MISSING")
        if not scope.get("tables") or not scope.get("operations"):
            errors.append("WRITE_SCOPE_INCOMPLETE")
        if row.idempotency_policy != "REQUIRED":
            errors.append("IDEMPOTENCY_NOT_REQUIRED")
    return errors


def _model_skill_quality(db: Session, payload: DataQualityRunRequest) -> tuple[str, dict[str, Any]]:
    if payload.target_id == "model_skill":
        rows = list(db.scalars(select(ModelSkill).order_by(ModelSkill.skill_code).limit(payload.limit)).all())
    else:
        row = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == payload.target_id))
        if row is None and payload.target_id.isdigit():
            row = db.get(ModelSkill, int(payload.target_id))
        if row is None:
            raise HTTPException(status_code=404, detail="Model skill not found")
        rows = [row]
    enabled = [row for row in rows if row.enabled and row.lifecycle_status == "ENABLED"]
    errors_by_skill = {row.skill_code: _skill_contract_errors(row) for row in enabled}
    invalid = {code: errors for code, errors in errors_by_skill.items() if errors}
    passed = bool(enabled) and not invalid
    fields = (
        "input_contract_json", "output_contract_json", "permission_policy_json",
        "side_effect_level", "idempotency_policy", "retry_policy_json", "error_policy_json",
    )
    null_counts = {field: sum(_blank(getattr(row, field, None)) for row in enabled) for field in fields}
    return "model_skill", {
        "passed": passed,
        "level": "PASS" if passed else "FAILED",
        "contract_version": "MODEL_SKILL_QUALITY_V1",
        "row_count": len(rows),
        "enabled_skill_count": len(enabled),
        "contract_complete_count": len(enabled) - len(invalid),
        "unsafe_permission_count": sum("UNSAFE_DATABASE_PERMISSION" in errors for errors in invalid.values()),
        "checks": {
            "enabled_skills_present": bool(enabled),
            "governance_contracts_complete": not invalid,
            "database_permissions_safe": not any("UNSAFE_DATABASE_PERMISSION" in errors for errors in invalid.values()),
        },
        "null_counts": null_counts,
        "issue_samples": [
            {"skill_code": code, "issues": errors} for code, errors in list(invalid.items())[:50]
        ],
    }


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
    elif evaluator == "CHUNK_LINEAGE":
        active = int(result.get("active_row_count") or 0)
        complete = int(result.get("lineage_complete_count") or 0)
        missing = int(result.get("missing_lineage_count") or 0)
        passed = active > 0 and missing == 0 and complete == active
        details.update(active_row_count=active, lineage_complete_count=complete, missing_lineage_count=missing)
    elif evaluator == "HASH_VECTOR_KIND":
        mismatch = int(result.get("hash_vector_mismatch_count") or 0)
        semantic_unverified = int(result.get("semantic_ready_without_vector_count") or 0)
        passed = row_count > 0 and mismatch == 0 and semantic_unverified == 0
        details.update(hash_vector_mismatch_count=mismatch, semantic_ready_without_vector_count=semantic_unverified)
    elif evaluator == "SKILL_CONTRACT":
        enabled = int(result.get("enabled_skill_count") or 0)
        complete = int(result.get("contract_complete_count") or 0)
        unsafe = int(result.get("unsafe_permission_count") or 0)
        passed = enabled > 0 and complete == enabled and unsafe == 0
        details.update(enabled_skill_count=enabled, contract_complete_count=complete, unsafe_permission_count=unsafe)
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
    runners = {
        "SOURCE_TABLE": _source_quality,
        "LAKE_DATASET_VERSION": _dataset_quality,
        "LAKE_OBJECT": _lake_object_quality,
        "DOCUMENT_CHUNK": _chunk_quality,
        "MODEL_SKILL": _model_skill_quality,
    }
    resolved_target, result = runners[payload.target_type](db, payload)
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
    result_warnings = result.get("warnings") if isinstance(result.get("warnings"), list) else []
    warning_level = str(result.get("level") or "").upper() in {"WARNING", "PARTIAL"}
    active_issue_codes = {item["rule_code"] for item in failed_configured}
    if not result.get("passed") or samples or result_warnings or warning_level:
        active_issue_codes.add("BUILTIN_DATASET_CONTRACT")
        details = {
            "samples": samples[:50],
            "checks": result.get("checks", {}),
            "warnings": result_warnings,
        }
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
    previous_open = db.scalars(select(DataQualityIssue).where(
        DataQualityIssue.target_type == payload.target_type,
        DataQualityIssue.target_id == resolved_target,
        DataQualityIssue.status == "OPEN",
        DataQualityIssue.quality_run_id != row.id,
    )).all()
    for issue in previous_open:
        issue.status = "RESOLVED"
        issue.resolved_at = _now()
        issue.resolution = (
            f"由后续质量运行 {row.run_key} 替代，问题在最新运行中继续跟踪"
            if issue.rule_code in active_issue_codes
            else f"后续质量运行 {row.run_key} 已通过该规则"
        )
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
