"""Immutable audit records for selection decisions and tracking outcomes."""

from __future__ import annotations

import hashlib
import json
from datetime import datetime, timezone
from typing import Any

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models.ai_hub import ModelInstance, ModelSkill
from app.models.decision_review import SelectionDecisionSnapshot, SelectionRetrospective
from app.models.selection import SelectionCandidate, SelectionRun, SelectionSnapshot, SelectionTracking


def _now() -> datetime:
    return datetime.now(timezone.utc)


def _json(value: Any) -> Any:
    if isinstance(value, datetime):
        return value.isoformat()
    if isinstance(value, dict):
        return {str(key): _json(item) for key, item in value.items()}
    if isinstance(value, (list, tuple, set)):
        return [_json(item) for item in value]
    return value


def payload_hash(value: Any) -> str:
    return hashlib.sha256(json.dumps(_json(value), ensure_ascii=False, sort_keys=True,
                                     separators=(",", ":"), default=str).encode("utf-8")).hexdigest()


def _skill_version(db: Session, code: str | None) -> str | None:
    if not code:
        return None
    skill = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == code))
    return skill.version if skill else None


def _model_exists(db: Session, code: str | None) -> str | None:
    if not code:
        return None
    return code if db.scalar(select(ModelInstance.id).where(ModelInstance.instance_code == code)) else code


def capture_decision_snapshot(
    db: Session,
    candidate: SelectionCandidate,
    *,
    snapshot_kind: str,
    decision: str | None = None,
    tracking_id: int | None = None,
    prediction_id: int | None = None,
    evidence: dict[str, Any] | None = None,
    as_of: datetime | None = None,
) -> SelectionDecisionSnapshot:
    """Capture one idempotent event without changing an earlier snapshot."""
    run = db.get(SelectionRun, candidate.run_id)
    snapshot_time = as_of or getattr(run, "as_of", None) or _now()
    evidence_json = _json(evidence or candidate.evidence_json or {})
    payload = _json({
        "candidate_id": candidate.id,
        "run_id": candidate.run_id,
        "market": candidate.market,
        "symbol": candidate.symbol,
        "name": candidate.name,
        "entry_price": candidate.entry_price,
        "entry_date": candidate.entry_date,
        "hard_score": candidate.hard_score,
        "hard_rules": candidate.hard_rules_json,
        "analysis": candidate.analysis_json,
        "decision": decision or candidate.decision,
        "review_notes": candidate.review_notes,
        "target_price": candidate.target_price,
        "stop_price": candidate.stop_price,
        "target_return_pct": candidate.target_return_pct,
        "confidence": candidate.confidence,
        "knowledge_base_ids": getattr(run, "knowledge_base_ids_json", []) if run else [],
        "graph_ids": getattr(run, "graph_ids_json", []) if run else [],
        "run_criteria": getattr(run, "criteria_json", {}) if run else {},
    })
    event_basis = {
        "kind": snapshot_kind,
        "candidate_id": candidate.id,
        "decision": decision or candidate.decision,
        "payload_hash": payload_hash(payload),
        "evidence_hash": payload_hash(evidence_json),
    }
    event_key = f"{snapshot_kind}:{payload_hash(event_basis)}"
    existing = db.scalar(select(SelectionDecisionSnapshot).where(
        SelectionDecisionSnapshot.candidate_id == candidate.id,
        SelectionDecisionSnapshot.snapshot_kind == snapshot_kind,
        SelectionDecisionSnapshot.event_key == event_key,
    ))
    if existing is not None:
        return existing
    analysis = candidate.analysis_json or {}
    skill_code = str(analysis.get("skill_code") or "STOCK_SELECTION_ANALYST")
    model_code = getattr(run, "model_instance_code", None) if run else None
    row = SelectionDecisionSnapshot(
        candidate_id=candidate.id,
        run_id=candidate.run_id,
        tracking_id=tracking_id,
        prediction_id=prediction_id,
        snapshot_kind=snapshot_kind,
        event_key=event_key,
        market=candidate.market,
        symbol=candidate.symbol,
        decision=decision or candidate.decision,
        as_of=snapshot_time,
        data_cutoff=str(evidence_json.get("data_cutoff") or snapshot_time.date().isoformat()),
        data_version=str(evidence_json.get("lakehouse_version") or evidence_json.get("data_version") or "LOCAL_TABLES"),
        graph_version=str(evidence_json.get("graph_version") or "GRAPH_CONTEXT_UNAVAILABLE"),
        skill_code=skill_code,
        skill_version=str(evidence_json.get("skill_version") or _skill_version(db, skill_code) or "UNREGISTERED"),
        model_instance_code=_model_exists(db, model_code),
        payload_json=payload,
        evidence_json=evidence_json,
        payload_hash=payload_hash({"payload": payload, "evidence": evidence_json}),
    )
    db.add(row)
    db.flush()
    return row


def approved_snapshot(db: Session, candidate_id: int) -> SelectionDecisionSnapshot | None:
    return db.scalar(select(SelectionDecisionSnapshot).where(
        SelectionDecisionSnapshot.candidate_id == candidate_id,
        SelectionDecisionSnapshot.snapshot_kind.in_(["REVIEW", "APPROVED"]),
        SelectionDecisionSnapshot.decision == "APPROVED",
    ).order_by(SelectionDecisionSnapshot.created_at.desc(), SelectionDecisionSnapshot.id.desc()).limit(1))


def capture_retrospective(db: Session, tracking: SelectionTracking) -> SelectionRetrospective | None:
    """Append a review revision for the current tracking state.

    The same state hash is idempotent, while a late-arriving bar naturally
    produces a new revision instead of rewriting a previous review.
    """
    candidate = db.get(SelectionCandidate, tracking.candidate_id)
    if candidate is None:
        return None
    snapshot = approved_snapshot(db, candidate.id)
    if snapshot is None:
        snapshot = db.scalar(select(SelectionDecisionSnapshot).where(
            SelectionDecisionSnapshot.candidate_id == candidate.id,
        ).order_by(SelectionDecisionSnapshot.created_at.desc(), SelectionDecisionSnapshot.id.desc()).limit(1))
    if snapshot is None:
        return None
    snapshots = list(db.scalars(select(SelectionSnapshot).where(
        SelectionSnapshot.tracking_id == tracking.id,
    ).order_by(SelectionSnapshot.sequence)).all())
    state = {
        "tracking_id": tracking.id,
        "status": tracking.status,
        "observed_sessions": tracking.observed_sessions,
        "latest_date": tracking.latest_date,
        "latest_price": tracking.latest_price,
        "return_pct": tracking.cumulative_return_pct,
        "max_drawdown_pct": tracking.max_drawdown_pct,
        "target_hit": tracking.target_hit,
        "stop_hit": tracking.stop_hit,
        "direction_hit": tracking.direction_hit,
        "snapshots": [{"date": row.trade_date, "close": row.close_price, "return": row.return_pct}
                      for row in snapshots],
    }
    state_hash = payload_hash(state)
    existing = db.scalar(select(SelectionRetrospective).where(
        SelectionRetrospective.snapshot_id == snapshot.id,
        SelectionRetrospective.state_hash == state_hash,
    ))
    if existing is not None:
        return existing
    revision = int(db.scalar(select(func.max(SelectionRetrospective.revision)).where(
        SelectionRetrospective.snapshot_id == snapshot.id,
    )) or 0) + 1
    tags: list[str] = []
    if tracking.status == "PENDING_DATA":
        tags.append("MISSING_MARKET_DATA")
    if tracking.status == "COMPLETED" and tracking.direction_hit is False:
        tags.append("DIRECTION_MISS")
    if tracking.status == "COMPLETED" and tracking.target_hit is False:
        tags.append("TARGET_NOT_HIT")
    if tracking.stop_hit:
        tags.append("STOP_HIT")
    review = tracking.review_json or {}
    row = SelectionRetrospective(
        snapshot_id=snapshot.id,
        tracking_id=tracking.id,
        revision=revision,
        market=tracking.market,
        symbol=tracking.symbol,
        status="COMPLETED" if tracking.status == "COMPLETED" else "IN_PROGRESS",
        evaluated_at=_now(),
        observed_sessions=tracking.observed_sessions,
        entry_price=tracking.entry_price,
        final_price=tracking.latest_price,
        return_pct=tracking.cumulative_return_pct,
        max_drawdown_pct=tracking.max_drawdown_pct,
        target_hit=tracking.target_hit,
        stop_hit=tracking.stop_hit,
        direction_hit=tracking.direction_hit,
        attribution_json={
            "snapshot_id": snapshot.id,
            "graph_version": snapshot.graph_version,
            "data_version": snapshot.data_version,
            "skill_code": snapshot.skill_code,
            "skill_version": snapshot.skill_version,
            "model_instance_code": snapshot.model_instance_code,
            "evidence_document_ids": (snapshot.evidence_json or {}).get("document_ids", []),
            "evidence_fact_ids": (snapshot.evidence_json or {}).get("evidence_fact_ids", []),
            "context_hash": (snapshot.evidence_json or {}).get("context_hash"),
            "review": review,
        },
        error_tags_json=tags,
        summary=review.get("summary") if isinstance(review, dict) else None,
        reviewer="SYSTEM",
        state_hash=state_hash,
    )
    db.add(row)
    db.flush()
    return row


def snapshot_read(row: SelectionDecisionSnapshot) -> dict[str, Any]:
    return {key: _json(getattr(row, key)) for key in (
        "id", "candidate_id", "run_id", "tracking_id", "prediction_id", "snapshot_kind", "event_key",
        "market", "symbol", "decision", "as_of", "data_cutoff", "data_version", "graph_version",
        "skill_code", "skill_version", "model_instance_code", "payload_json", "evidence_json",
        "payload_hash", "created_at",
    )}


def retrospective_read(row: SelectionRetrospective) -> dict[str, Any]:
    return {key: _json(getattr(row, key)) for key in (
        "id", "snapshot_id", "tracking_id", "revision", "market", "symbol", "status", "evaluated_at",
        "observed_sessions", "entry_price", "final_price", "return_pct", "max_drawdown_pct", "target_hit",
        "stop_hit", "direction_hit", "attribution_json", "error_tags_json", "summary", "reviewer",
        "state_hash", "created_at",
    )}

