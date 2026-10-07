"""Persist structured Agent recommendations and report ratings for later review."""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
import hashlib
import json
from typing import Any

from fastapi.encoders import jsonable_encoder
from pydantic import ValidationError
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.ai_hub import ModelSkill, PredictionLedger, ResearchReportRecord
from app.models.governance import SkillExecutionRun
from app.schemas.predictions import PredictionCreate
from app.services.skill_registry import sync_skill_from_file


PREDICTION_WRITE_SERVICE = "prediction_ledger_service"
PREDICTION_WRITE_OPERATION = "CREATE_PREDICTION_LEDGER"
PREDICTION_WRITE_TABLE = "prediction_ledger"
PREDICTION_WRITER_SKILL_CODE = "PREDICTION_LEDGER_WRITER"


@dataclass(slots=True)
class PredictionCaptureResult:
    """Validated Agent recommendations and their controlled persistence result."""

    candidates: list[dict[str, Any]] = field(default_factory=list)
    prediction_ids: list[int] = field(default_factory=list)
    status: str = "NO_PREDICTIONS"
    reason: str | None = None
    writer_skill_code: str | None = None
    skill_execution_run_id: str | None = None

    def as_audit_json(self) -> dict[str, Any]:
        return {
            "status": self.status,
            "reason": self.reason,
            "candidate_predictions": self.candidates,
            "prediction_ids": self.prediction_ids,
            "writer_skill_code": self.writer_skill_code,
            "skill_execution_run_id": self.skill_execution_run_id,
        }


def record_agent_predictions(
    db: Session,
    response_text: str,
    *,
    allowed_skill_codes: set[str],
    model_instance_code: str | None,
    writer_skill: ModelSkill | None = None,
    idempotency_key: str | None = None,
    agent_execution_run_id: str | None = None,
    model_call_log_id: int | None = None,
    evidence_ids: list[str] | None = None,
) -> PredictionCaptureResult:
    """Validate advice and persist only through an authorized controlled-write Skill.

    A valid model recommendation is always returned as a candidate.  Database
    persistence is fail-closed unless the caller supplies a bound, enabled Skill
    whose contract explicitly permits the prediction-ledger service/table and an
    idempotency key.  This keeps legacy callers useful without allowing an Agent
    or LLM response to become an implicit database write.
    """
    try:
        response = json.loads(response_text)
    except (TypeError, ValueError):
        return PredictionCaptureResult()
    if not isinstance(response, dict):
        return PredictionCaptureResult()
    raw = response.get("predictions")
    if raw is None and "prediction" in response:
        raw = [response["prediction"]]
    if raw is None:
        return PredictionCaptureResult()
    if not isinstance(raw, list) or len(raw) > 50:
        raise ValueError("Agent predictions must be an array of at most 50 structured recommendations")
    validated: list[tuple[PredictionCreate, ModelSkill]] = []
    candidates: list[dict[str, Any]] = []
    for item in raw:
        try:
            recommendation = PredictionCreate.model_validate(item)
        except ValidationError as exc:
            raise ValueError(f"Agent prediction is incomplete: {exc.errors()[0]['loc']}") from exc
        if recommendation.skill_code not in allowed_skill_codes:
            raise ValueError("Agent prediction must cite a bound Skill")
        skill = db.scalar(select(ModelSkill).where(
            ModelSkill.skill_code == recommendation.skill_code,
            ModelSkill.enabled.is_(True),
            ModelSkill.lifecycle_status == "ENABLED",
        ))
        if skill is None:
            raise ValueError("Agent prediction cites a disabled or missing Skill")
        if PREDICTION_WRITE_SERVICE in (skill.permission_policy_json or {}).get("allowed_services", []):
            raise ValueError("Agent prediction must cite an analysis Skill, not the write-control Skill")
        if recommendation.research_report_id and db.get(ResearchReportRecord, recommendation.research_report_id) is None:
            raise ValueError("Agent prediction cites a missing research report")
        validated.append((recommendation, skill))
        candidates.append({
            **recommendation.model_dump(exclude={"model_instance_code"}),
            "skill_version_used": skill.version,
            "model_instance_code": model_instance_code,
        })

    result = PredictionCaptureResult(candidates=candidates, status="CANDIDATE_ONLY")
    if writer_skill is None:
        result.reason = "CONTROLLED_WRITE_SKILL_NOT_BOUND"
        return result
    result.writer_skill_code = writer_skill.skill_code
    denial_reason = _prediction_writer_denial_reason(writer_skill)
    if denial_reason:
        result.reason = denial_reason
        return result
    if not idempotency_key:
        result.reason = "IDEMPOTENCY_KEY_REQUIRED"
        return result

    write_key = _bounded_write_key(idempotency_key, writer_skill)
    input_payload = {
        "candidates": candidates,
        "idempotency_key": idempotency_key,
        "evidence_ids": list(dict.fromkeys(str(item) for item in (evidence_ids or []))),
        "model_instance_code": model_instance_code,
        "model_call_log_id": model_call_log_id,
    }
    input_hash = _payload_hash(input_payload)
    existing = db.scalar(select(SkillExecutionRun).where(SkillExecutionRun.idempotency_key == write_key))
    if existing is not None:
        if existing.skill_id != writer_skill.id or existing.input_hash != input_hash:
            raise ValueError("Prediction write idempotency key was reused with different input")
        if existing.status != "SUCCESS":
            result.reason = "IDEMPOTENT_WRITE_NOT_REUSABLE"
            result.skill_execution_run_id = existing.id
            return result
        output = existing.output_json or {}
        result.prediction_ids = [int(item) for item in output.get("prediction_ids", [])]
        result.status = "PERSISTED_REUSED"
        result.reason = None
        result.skill_execution_run_id = existing.id
        return result

    execution = SkillExecutionRun(
        agent_execution_run_id=agent_execution_run_id,
        skill_id=writer_skill.id,
        skill_code=writer_skill.skill_code,
        skill_version=writer_skill.version,
        status="RUNNING",
        idempotency_key=write_key,
        input_hash=input_hash,
        input_json=input_payload,
        side_effect_level=writer_skill.side_effect_level,
        write_scope_json={
            "allowed_operations": [PREDICTION_WRITE_OPERATION],
            "allowed_services": [PREDICTION_WRITE_SERVICE],
            "tables": [PREDICTION_WRITE_TABLE],
            "database_operations": ["INSERT"],
            "direct_sql": False,
        },
        evidence_ids_json=list(dict.fromkeys(str(item) for item in (evidence_ids or []))),
        model_call_log_id=model_call_log_id,
        max_attempts=1,
    )
    db.add(execution)
    db.commit()

    try:
        rows = []
        for recommendation, analysis_skill in validated:
            values = recommendation.model_dump(exclude={"model_instance_code"})
            rows.append(PredictionLedger(
                **values,
                skill_version_used=analysis_skill.version,
                model_instance_code=model_instance_code,
                status="PENDING",
            ))
        db.add_all(rows)
        db.flush()
        result.prediction_ids = [row.id for row in rows]
        result.status = "PERSISTED"
        result.reason = None
        result.skill_execution_run_id = execution.id
        execution.status = "SUCCESS"
        execution.output_json = {"prediction_ids": result.prediction_ids}
        execution.output_hash = _payload_hash(execution.output_json)
        execution.completed_at = datetime.now(timezone.utc)
        db.commit()
        return result
    except Exception as exc:
        db.rollback()
        failed = db.get(SkillExecutionRun, execution.id)
        if failed is not None:
            failed.status = "FAILED"
            failed.error_code = exc.__class__.__name__
            failed.error_message = str(exc)[:4000]
            failed.completed_at = datetime.now(timezone.utc)
            db.commit()
        raise


def _prediction_writer_denial_reason(skill: ModelSkill) -> str | None:
    permissions = skill.permission_policy_json or {}
    scope = permissions.get("write_scope") or {}
    if not skill.enabled or skill.lifecycle_status != "ENABLED":
        return "CONTROLLED_WRITE_SKILL_NOT_ENABLED"
    if skill.skill_type != "EXECUTABLE_TOOL" or skill.side_effect_level != "CONTROLLED_WRITE":
        return "CONTROLLED_WRITE_SKILL_REQUIRED"
    if skill.idempotency_policy != "REQUIRED":
        return "CONTROLLED_WRITE_SKILL_REQUIRES_IDEMPOTENCY_POLICY"
    if permissions.get("arbitrary_sql") or permissions.get("unrestricted_database_write"):
        return "FORBIDDEN_DATABASE_PERMISSION"
    if permissions.get("database_write") is not True:
        return "DATABASE_WRITE_NOT_EXPLICITLY_ALLOWED"
    if PREDICTION_WRITE_OPERATION not in permissions.get("allowed_operations", []):
        return "PREDICTION_WRITE_OPERATION_NOT_ALLOWED"
    if PREDICTION_WRITE_SERVICE not in permissions.get("allowed_services", []):
        return "PREDICTION_WRITE_SERVICE_NOT_ALLOWED"
    if PREDICTION_WRITE_TABLE not in scope.get("tables", []):
        return "PREDICTION_WRITE_SCOPE_NOT_DECLARED"
    if "INSERT" not in scope.get("operations", []):
        return "PREDICTION_INSERT_SCOPE_NOT_DECLARED"
    return None


def _bounded_write_key(idempotency_key: str, writer_skill: ModelSkill) -> str:
    raw = f"{idempotency_key}:prediction-ledger:{writer_skill.skill_code}:{writer_skill.version}"
    if len(raw) <= 192:
        return raw
    digest = hashlib.sha256(raw.encode("utf-8")).hexdigest()
    return f"prediction-ledger:{digest}"


def _payload_hash(payload: Any) -> str:
    encoded = json.dumps(jsonable_encoder(payload), ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(encoded.encode("utf-8")).hexdigest()


def record_report_rating(
    db: Session,
    report: ResearchReportRecord,
    *,
    entry_price: float | None,
) -> PredictionLedger | None:
    """Map the report's existing A/B/C/D rating to a visible T+5 ledger entry."""
    if entry_price is None or entry_price <= 0:
        return None
    skill = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == "STOCK_TREND_ADVISOR"))
    if skill is None:
        return None
    sync_skill_from_file(db, skill)
    action = "BUY" if report.rating in {"A", "B"} else "SELL" if report.rating == "D" else "HOLD"
    row = PredictionLedger(
        market=report.market, stock_code=report.symbol, action_type=action,
        target_timeframe="T+5", reasoning_logic=(
            f"Research report #{report.id}; rating {report.rating or 'C'}; score {report.score}; "
            f"{report.conclusion or ''}"
        )[:20000],
        skill_code=skill.skill_code, skill_version_used=skill.version,
        model_instance_code=report.model_instance, research_report_id=report.id,
        entry_price=entry_price, status="PENDING",
    )
    db.add(row)
    db.flush()
    return row
