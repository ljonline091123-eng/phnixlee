"""Deterministic Skill regression scoring and reviewable repair proposals."""

from __future__ import annotations

import hashlib
import json
from datetime import datetime, timezone
from typing import Any

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.ai_hub import ModelCallLog, ModelSkill, SkillOptimizationDraft
from app.models.decision_review import SkillEvaluationCase, SkillEvaluationResult, SkillEvaluationRun
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
    signature = "EVALUATION:" + _hash({"run": run.id, "skill": skill.skill_code, "version": skill.version,
                                        "results": [row.id for row in results]})
    existing = db.scalar(select(SkillOptimizationDraft).where(SkillOptimizationDraft.failure_signature == signature))
    if existing is not None:
        return existing
    # A repair proposal is intentionally only a pending draft.  If no live LLA
    # is configured, return no draft rather than fabricating instructions.
    try:
        log = ModelHubService(db).chat(
            task_type="meta_review",
            messages=[
                {"role": "system", "content": "你是 Skill 回归评审器，只能基于失败评测案例提出待人工审核的 Prompt 修订，不得自动启用。请返回 JSON：proposed_instructions、rationale。"},
                {"role": "user", "content": json.dumps({"skill": skill.instructions, "run": run.summary_json,
                    "failed_results": [{"output": row.output_json, "metrics": row.metrics_json,
                                        "error_type": row.error_type} for row in results]}, ensure_ascii=False, default=str)},
            ],
            max_tokens=4096,
            metadata_json={"evaluation_run_id": run.id, "skill_code": skill.skill_code,
                           "skill_version": skill.version, "skill_content_hash": _hash(skill.instructions)},
        )
    except Exception:
        return None
    if log.status != "SUCCESS" or not log.response_text or log.provider_code == "MOCK":
        return None
    try:
        proposal = json.loads(log.response_text)
        instructions = str(proposal["proposed_instructions"])
        rationale = str(proposal["rationale"])
    except (ValueError, KeyError, TypeError):
        return None
    draft = SkillOptimizationDraft(skill_id=skill.id, base_skill_version=skill.version,
        failure_signature=signature, prediction_ids=[], proposed_instructions=instructions,
        rationale=rationale, model_call_log_id=log.id, status="PENDING_REVIEW")
    db.add(draft)
    db.commit()
    db.refresh(draft)
    return draft

