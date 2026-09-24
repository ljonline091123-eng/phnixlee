"""Deterministic Skill regression scoring and reviewable repair proposals."""

from __future__ import annotations

import hashlib
import json
from datetime import datetime, timezone
from typing import Any

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.ai_hub import ModelCallLog, ModelSkill, SkillOptimizationDraft
from app.models.decision_review import (
    SelectionDecisionSnapshot,
    SelectionRetrospective,
    SkillEvaluationCase,
    SkillEvaluationResult,
    SkillEvaluationRun,
)
from app.services.model_hub import ModelHubService


def _now() -> datetime:
    return datetime.now(timezone.utc)


def _hash(value: Any) -> str:
    return hashlib.sha256(json.dumps(value, ensure_ascii=False, sort_keys=True,
                                     default=str, separators=(",", ":")).encode("utf-8")).hexdigest()


def create_case(
    db: Session,
    *,
    case_code: str,
    task_type: str,
    input_json: dict[str, Any],
    expected_json: dict[str, Any],
    rubric_json: dict[str, Any] | None = None,
    description: str | None = None,
    dataset_version: str = "v1",
    source: str = "HUMAN",
) -> SkillEvaluationCase:
    existing = db.scalar(select(SkillEvaluationCase).where(SkillEvaluationCase.case_code == case_code))
    if existing is not None:
        raise ValueError("evaluation case already exists")
    if not case_code.strip() or not task_type.strip():
        raise ValueError("case_code and task_type are required")
    row = SkillEvaluationCase(case_code=case_code.strip(), task_type=task_type.strip(), description=description,
                              input_json=input_json or {}, expected_json=expected_json or {}, rubric_json=rubric_json or {},
                              dataset_version=dataset_version, source=source)
    db.add(row)
    db.commit()
    db.refresh(row)
    return row


def _equal(actual: Any, expected: Any) -> bool:
    if isinstance(expected, float) and isinstance(actual, (float, int)):
        return abs(float(actual) - expected) <= 1e-6
    return actual == expected


def score_case(case: SkillEvaluationCase, output: dict[str, Any]) -> tuple[bool, dict[str, Any], str | None, list[Any]]:
    expected = case.expected_json or {}
    rubric = case.rubric_json or {}
    checks: dict[str, bool] = {}
    errors: list[str] = []
    for key, value in expected.items():
        if key == "required_evidence_refs":
            actual_refs = set(output.get("evidence_document_ids") or output.get("evidence_refs") or [])
            required_refs = set(value or [])
            checks[key] = required_refs.issubset(actual_refs)
        elif key == "forbidden_claims":
            text = json.dumps(output, ensure_ascii=False, default=str)
            checks[key] = not any(str(item) in text for item in (value or []))
        else:
            checks[key] = _equal(output.get(key), value)
    required_fields = rubric.get("required_fields") or []
    if required_fields:
        checks["required_fields"] = all(output.get(field) not in (None, "") for field in required_fields)
    allowed_refs = set((case.input_json or {}).get("allowed_evidence_ids") or (case.input_json or {}).get("evidence_document_ids") or [])
    if allowed_refs and (output.get("evidence_document_ids") or output.get("evidence_refs")):
        actual_refs = set(output.get("evidence_document_ids") or output.get("evidence_refs") or [])
        checks["evidence_grounded"] = actual_refs.issubset(allowed_refs)
    for key, passed in checks.items():
        if not passed:
            errors.append(key)
    passed = bool(checks) and not errors
    error_type = None if passed else ("EVIDENCE_UNGROUNDED" if "evidence_grounded" in errors else
                                      "MISSING_FIELD" if "required_fields" in errors else
                                      "EXPECTED_MISMATCH")
    metrics = {"checks": checks, "passed_checks": sum(checks.values()), "total_checks": len(checks),
               "grounded": checks.get("evidence_grounded", True), "error_tags": errors}
    refs = output.get("evidence_document_ids") or output.get("evidence_refs") or []
    return passed, metrics, error_type, refs if isinstance(refs, list) else []


def _model_output(db: Session, skill: ModelSkill, case: SkillEvaluationCase, instance_code: str | None) -> tuple[dict[str, Any], int | None]:
    skill_hash = _hash(skill.instructions)
    prompt = "请严格根据评测案例输入输出 JSON，不要添加输入之外的事实。案例：" + json.dumps(case.input_json, ensure_ascii=False, default=str)
    log = ModelHubService(db).chat(
        task_type=case.task_type,
        instance_code=instance_code,
        messages=[
            {"role": "system", "content": f"你正在执行 Skill {skill.skill_code} 版本 {skill.version}。以下是本次评测冻结的 Skill 指令（哈希 {skill_hash}），必须遵守：\n{skill.instructions}"},
            {"role": "user", "content": prompt},
        ],
        max_tokens=4096,
        metadata_json={"skill_code": skill.skill_code, "skill_version": skill.version,
                       "skill_content_hash": skill_hash, "evaluation_case": case.case_code},
    )
    if log.status != "SUCCESS" or not log.response_text:
        raise ValueError(log.error_message or "Skill model evaluation failed")
    try:
        parsed = json.loads(log.response_text)
    except json.JSONDecodeError as exc:
        raise ValueError("Skill output is not valid JSON") from exc
    if not isinstance(parsed, dict):
        raise ValueError("Skill output must be a JSON object")
    return parsed, log.id


def run_evaluation(
    db: Session,
    *,
    skill_code: str,
    task_type: str,
    case_ids: list[int] | None = None,
    outputs: dict[str, dict[str, Any]] | None = None,
    use_model: bool = False,
    instance_code: str | None = None,
    dataset_version: str = "v1",
) -> SkillEvaluationRun:
    skill = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == skill_code))
    if skill is None:
        raise LookupError("Skill not found")
    query = select(SkillEvaluationCase).where(SkillEvaluationCase.task_type == task_type,
                                               SkillEvaluationCase.enabled.is_(True))
    if case_ids:
        query = query.where(SkillEvaluationCase.id.in_(case_ids))
    cases = list(db.scalars(query.order_by(SkillEvaluationCase.id)).all())
    if not cases:
        raise ValueError("no enabled evaluation cases")
    skill_hash = _hash(skill.instructions)
    run = SkillEvaluationRun(skill_id=skill.id, skill_code=skill.skill_code, skill_version=skill.version,
                             task_type=task_type, dataset_version=dataset_version, evaluator="MODEL" if use_model else "RULE",
                             status="RUNNING", summary_json={"case_count": len(cases),
                                                               "skill_content_hash": skill_hash,
                                                               "skill_name": skill.skill_name})
    db.add(run)
    db.flush()
    outputs = outputs or {}
    passed_count = 0
    errors: list[dict[str, Any]] = []
    for case in cases:
        started = datetime.now(timezone.utc)
        output: dict[str, Any]
        log_id: int | None = None
        error_type: str | None = None
        try:
            if case.case_code in outputs:
                output = outputs[case.case_code]
            elif use_model:
                output, log_id = _model_output(db, skill, case, instance_code)
            else:
                output = {}
                error_type = "NOT_EXECUTED"
            passed, metrics, score_error, refs = score_case(case, output)
            error_type = error_type or score_error
        except Exception as exc:
            output, metrics, refs, passed = {"error": str(exc)[:500]}, {"checks": {}, "error": str(exc)[:500]}, [], False
            error_type = "EXECUTION_ERROR"
        if passed:
            passed_count += 1
        else:
            errors.append({"case_code": case.case_code, "error_type": error_type, "metrics": metrics})
        elapsed = int((datetime.now(timezone.utc) - started).total_seconds() * 1000)
        db.add(SkillEvaluationResult(run_id=run.id, case_id=case.id, output_json=output,
                                     metrics_json={**metrics, "model_call_log_id": log_id}, passed=passed,
                                     error_type=error_type, latency_ms=elapsed, evidence_refs_json=refs))
    run.status = "PASSED" if passed_count == len(cases) else "FAILED"
    run.completed_at = _now()
    run.summary_json = {"case_count": len(cases), "passed_count": passed_count,
                        "failed_count": len(cases) - passed_count, "pass_rate": passed_count / len(cases),
                        "errors": errors}
    db.commit()
    db.refresh(run)
    return run


_SKILL_REPAIRABLE_RETROSPECTIVE_TAGS = frozenset({
    "DIRECTION_MISS",
    "TARGET_NOT_HIT",
    "STOP_HIT",
})


def _retrospective_case_code(row: SelectionRetrospective) -> str:
    return f"RETROSPECTIVE:{row.id}:{row.state_hash[:24]}"


def _evidence_document_ids(
    snapshot: SelectionDecisionSnapshot,
    retrospective: SelectionRetrospective,
) -> list[Any]:
    snapshot_evidence = snapshot.evidence_json or {}
    attribution = retrospective.attribution_json or {}
    values: list[Any] = []
    for key in ("model_document_ids", "document_ids", "context_document_ids"):
        candidate = snapshot_evidence.get(key)
        if isinstance(candidate, list):
            values.extend(candidate)
    candidate = attribution.get("evidence_document_ids")
    if isinstance(candidate, list):
        values.extend(candidate)
    # Evidence identifiers can be numeric or string-valued across sources.
    # JSON serialization gives a stable de-duplication key without coercing them.
    unique: dict[str, Any] = {}
    for value in values:
        if value is not None:
            unique.setdefault(json.dumps(value, ensure_ascii=False, sort_keys=True, default=str), value)
    return list(unique.values())


def ensure_retrospective_case(
    db: Session,
    retrospective_id: int,
) -> tuple[SkillEvaluationCase, SelectionRetrospective, SelectionDecisionSnapshot, bool]:
    """Freeze one retrospective into an idempotent regression case."""
    retrospective = db.get(SelectionRetrospective, retrospective_id)
    if retrospective is None:
        raise LookupError("选股复盘记录不存在")
    snapshot = db.get(SelectionDecisionSnapshot, retrospective.snapshot_id)
    if snapshot is None:
        raise LookupError("复盘对应的决策快照不存在")

    case_code = _retrospective_case_code(retrospective)
    existing = db.scalar(select(SkillEvaluationCase).where(SkillEvaluationCase.case_code == case_code))
    if existing is not None:
        return existing, retrospective, snapshot, False

    skill = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == snapshot.skill_code)) if snapshot.skill_code else None
    task_type = str((skill.config_json or {}).get("task_type") or "stock_screening") if skill else "stock_screening"
    evidence_ids = _evidence_document_ids(snapshot, retrospective)
    error_tags = list(retrospective.error_tags_json or [])
    outcome = {
        "status": retrospective.status,
        "observed_sessions": retrospective.observed_sessions,
        "return_pct": retrospective.return_pct,
        "max_drawdown_pct": retrospective.max_drawdown_pct,
        "target_hit": retrospective.target_hit,
        "stop_hit": retrospective.stop_hit,
        "direction_hit": retrospective.direction_hit,
        "error_tags": error_tags,
        "summary": retrospective.summary,
    }
    expected_decision = "PASS" if set(error_tags).intersection(
        _SKILL_REPAIRABLE_RETROSPECTIVE_TAGS
    ) else "WATCH"
    row = SkillEvaluationCase(
        case_code=case_code,
        task_type=task_type,
        description=(
            f"选股复盘 {retrospective.id}，标的 {retrospective.market}:{retrospective.symbol}；"
            f"误差标签={','.join(error_tags) or '无'}"
        ),
        input_json={
            "workflow": "SELECTION_RETROSPECTIVE",
            "retrospective_id": retrospective.id,
            "snapshot_id": snapshot.id,
            "prediction_id": snapshot.prediction_id,
            "market": retrospective.market,
            "symbol": retrospective.symbol,
            "decision_snapshot": snapshot.payload_json or {},
            "evidence": snapshot.evidence_json or {},
            "allowed_evidence_ids": evidence_ids,
        },
        # Outcome labels never enter the model input.  They only define the
        # historical blind-test expectation used by the deterministic scorer.
        expected_json={"decision": expected_decision},
        rubric_json={
            "required_fields": ["decision"],
            "review_gate": "HUMAN_REQUIRED",
            "case_role": "HISTORICAL_BLIND_REGRESSION",
            "retrospective_outcome": outcome,
        },
        dataset_version=(snapshot.data_version or f"retrospective-{retrospective.id}")[:64],
        source="RETROSPECTIVE",
        enabled=True,
    )
    db.add(row)
    db.commit()
    db.refresh(row)
    return row, retrospective, snapshot, True


def _persisted_evaluation_output(snapshot: SelectionDecisionSnapshot) -> tuple[dict[str, Any] | None, str]:
    payload = snapshot.payload_json or {}
    analysis = payload.get("analysis")
    if not isinstance(analysis, dict) or not analysis:
        return None, "UNAVAILABLE"
    model_output = analysis.get("model")
    if isinstance(model_output, dict) and model_output:
        return dict(model_output), "PERSISTED_MODEL_OUTPUT"
    return dict(analysis), "PERSISTED_ANALYSIS"


def run_retrospective_repair_flow(
    db: Session,
    retrospective_id: int,
    *,
    evaluation_output: dict[str, Any] | None = None,
    request_repair_draft: bool = True,
) -> dict[str, Any]:
    """Create a regression case, score a frozen output, and request a review-only draft."""
    case, retrospective, snapshot, case_created = ensure_retrospective_case(db, retrospective_id)
    skill = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == snapshot.skill_code)) if snapshot.skill_code else None
    base = {
        "retrospective_id": retrospective.id,
        "case_id": case.id,
        "case_code": case.case_code,
        "case_created": case_created,
        "skill_code": snapshot.skill_code,
        "snapshot_skill_version": snapshot.skill_version,
        "active_skill_version": skill.version if skill else None,
        "evaluation_run_id": None,
        "evaluation_status": "NOT_RUN",
        "evaluation_source": "EXPLICIT_OUTPUT" if evaluation_output is not None else "UNAVAILABLE",
        "repair_draft_id": None,
        "repair_draft_status": None,
        "repair_status": "NOT_REQUESTED" if not request_repair_draft else "NOT_AVAILABLE",
        "message": None,
    }
    if skill is None:
        return {**base, "message": "决策快照记录的 Skill 尚未注册。"}

    output = dict(evaluation_output) if evaluation_output is not None else None
    output_source = "EXPLICIT_OUTPUT"
    if output is None:
        output, output_source = _persisted_evaluation_output(snapshot)
    base["evaluation_source"] = output_source
    if output is None:
        return {**base, "message": "没有可用的冻结模型输出或显式评测输出。"}
    if (evaluation_output is None and snapshot.skill_version not in (None, "UNREGISTERED", skill.version)):
        return {
            **base,
            "evaluation_status": "SKILL_VERSION_MISMATCH",
            "message": "当前 Skill 版本与冻结决策不一致，需要提供当前版本的显式评测输出。",
        }

    run = run_evaluation(
        db,
        skill_code=skill.skill_code,
        task_type=case.task_type,
        case_ids=[case.id],
        outputs={case.case_code: output},
        use_model=False,
        dataset_version=case.dataset_version,
    )
    base.update({
        "evaluation_run_id": run.id,
        "evaluation_status": run.status,
        "evaluation_source": output_source,
    })

    if not request_repair_draft:
        return base
    error_tags = set(retrospective.error_tags_json or [])
    if retrospective.status != "COMPLETED":
        return {**base, "repair_status": "NOT_ELIGIBLE", "message": "只有已完成的复盘可以申请 Skill 修复草案。"}
    if not error_tags.intersection(_SKILL_REPAIRABLE_RETROSPECTIVE_TAGS):
        return {**base, "repair_status": "NOT_ELIGIBLE", "message": "本次复盘没有可归因到 Skill 的结果误差。"}
    if output_source == "PERSISTED_ANALYSIS":
        return {**base, "repair_status": "NOT_ELIGIBLE", "message": "冻结决策没有成功的模型输出，不能归因到 Skill。"}
    if run.status != "FAILED":
        return {**base, "repair_status": "NOT_NEEDED", "message": "本次复盘回归评测已通过，无需生成修复草案。"}

    draft = propose_repair_draft(db, run.id)
    if draft is None:
        return {
            **base,
            "repair_status": "MODEL_UNAVAILABLE",
            "message": "没有可用的非模拟元评审模型返回有效方案，本次未生成修复草案。",
        }
    if draft.status != "PENDING_REVIEW":
        raise ValueError("Skill 修复草案违反人工审核门禁")
    return {
        **base,
        "repair_draft_id": draft.id,
        "repair_draft_status": draft.status,
        "repair_status": "PENDING_REVIEW",
        "message": "已生成待人工审核的修复草案，当前 Skill 未被修改。",
    }


def propose_repair_draft(db: Session, run_id: int) -> SkillOptimizationDraft | None:
    run = db.get(SkillEvaluationRun, run_id)
    if run is None:
        raise LookupError("evaluation run not found")
    if run.status != "FAILED":
        return None
    skill = db.get(ModelSkill, run.skill_id) if run.skill_id else db.scalar(select(ModelSkill).where(ModelSkill.skill_code == run.skill_code))
    if skill is None:
        return None
    results = list(db.scalars(select(SkillEvaluationResult).where(
        SkillEvaluationResult.run_id == run.id, SkillEvaluationResult.passed.is_(False),
    )).all())
    cases = {row.id: row for row in db.scalars(select(SkillEvaluationCase).where(
        SkillEvaluationCase.id.in_([result.case_id for result in results])
    )).all()}
    failure_fingerprints = []
    for result in sorted(results, key=lambda item: item.case_id):
        case = cases.get(result.case_id)
        failure_fingerprints.append({
            "case_code": case.case_code if case else str(result.case_id),
            "case_hash": _hash({
                "input": case.input_json if case else {},
                "expected": case.expected_json if case else {},
                "rubric": case.rubric_json if case else {},
            }),
            "output_hash": _hash(result.output_json or {}),
            "error_type": result.error_type,
        })
    signature = "EVALUATION:" + _hash({
        "skill": skill.skill_code,
        "version": skill.version,
        "task_type": run.task_type,
        "failures": failure_fingerprints,
    })
    existing = db.scalar(select(SkillOptimizationDraft).where(SkillOptimizationDraft.failure_signature == signature))
    if existing is not None:
        return existing
    # A repair proposal is intentionally only a pending draft.  If no live LLA
    # is configured, return no draft rather than fabricating instructions.
    try:
        repair_cases = []
        for result in results:
            case = cases.get(result.case_id)
            case_input = (case.input_json or {}) if case else {}
            evidence = case_input.get("evidence") if isinstance(case_input.get("evidence"), dict) else {}
            repair_cases.append({
                "case_code": case.case_code if case else str(result.case_id),
                "description": case.description if case else None,
                "market": case_input.get("market"),
                "symbol": case_input.get("symbol"),
                "retrospective_id": case_input.get("retrospective_id"),
                "outcome": (case.rubric_json or {}).get("retrospective_outcome") if case else None,
                "allowed_evidence_ids": case_input.get("allowed_evidence_ids", []),
                "evidence_refs": {
                    key: evidence.get(key)
                    for key in (
                        "document_ids", "model_document_ids", "context_document_ids",
                        "evidence_fact_ids", "context_hash", "graph_version",
                        "data_version", "missing_data",
                    )
                    if evidence.get(key) is not None
                },
                "expected": case.expected_json if case else {},
                "rubric": case.rubric_json if case else {},
                "output": result.output_json,
                "metrics": result.metrics_json,
                "error_type": result.error_type,
            })
        log = ModelHubService(db).chat(
            task_type="meta_review",
            messages=[
                {"role": "system", "content": "你是 Skill 回归评审器，只能基于失败评测案例提出待人工审核的 Prompt 修订，不得自动启用。请返回 JSON：proposed_instructions、rationale。"},
                {"role": "user", "content": json.dumps({
                    "skill": skill.instructions,
                    "run": run.summary_json,
                    "failed_results": repair_cases,
                }, ensure_ascii=False, default=str)},
            ],
            max_tokens=4096,
            metadata_json={"evaluation_run_id": run.id, "skill_code": skill.skill_code,
                           "skill_version": skill.version, "skill_content_hash": _hash(skill.instructions)},
        )
    except Exception:
        return None
    if (log.status != "SUCCESS" or not log.response_text or
            str(log.provider_code or "").upper().startswith("MOCK")):
        return None
    try:
        proposal = json.loads(log.response_text)
        instructions = str(proposal["proposed_instructions"])
        rationale = str(proposal["rationale"])
    except (ValueError, KeyError, TypeError):
        return None
    prediction_ids = sorted({
        int(case.input_json["prediction_id"])
        for case in cases.values()
        if (case.input_json or {}).get("prediction_id") is not None
    })
    draft = SkillOptimizationDraft(skill_id=skill.id, base_skill_version=skill.version,
        failure_signature=signature, prediction_ids=prediction_ids, proposed_instructions=instructions,
        rationale=rationale, model_call_log_id=log.id, status="PENDING_REVIEW")
    db.add(draft)
    db.commit()
    db.refresh(draft)
    return draft

