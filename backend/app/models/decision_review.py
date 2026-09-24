"""Immutable decision audit and skill-evaluation records.

These tables deliberately sit beside the existing selection workflow.  The
selection tables remain the operational state; this module stores append-only
snapshots and evaluation results so a later model call can never rewrite the
inputs that led to a decision.
"""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Any

from sqlalchemy import (
    Boolean,
    DateTime,
    ForeignKey,
    Index,
    Integer,
    JSON,
    String,
    Text,
    UniqueConstraint,
)
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


class SelectionDecisionSnapshot(Base):
    """An append-only, hash-addressed view of one selection decision.

    ``event_key`` makes capture idempotent for a decision event while retaining
    the candidate/run rows as the operational source of truth.  No update API
    is exposed for this table; a changed decision gets a new event key.
    """

    __tablename__ = "selection_decision_snapshot"
    __table_args__ = (
        UniqueConstraint(
            "candidate_id", "snapshot_kind", "event_key",
            name="uq_selection_decision_snapshot_event",
        ),
        Index("ix_selection_decision_snapshot_symbol_time", "market", "symbol", "as_of"),
        Index("ix_selection_decision_snapshot_skill", "skill_code", "skill_version"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    candidate_id: Mapped[int] = mapped_column(
        ForeignKey("selection_candidate.id", ondelete="CASCADE"), nullable=False, index=True
    )
    run_id: Mapped[int] = mapped_column(
        ForeignKey("selection_run.id", ondelete="CASCADE"), nullable=False, index=True
    )
    tracking_id: Mapped[int | None] = mapped_column(
        ForeignKey("selection_tracking.id", ondelete="SET NULL"), index=True
    )
    prediction_id: Mapped[int | None] = mapped_column(
        ForeignKey("prediction_ledger.id", ondelete="SET NULL"), index=True
    )
    snapshot_kind: Mapped[str] = mapped_column(String(32), nullable=False, default="DECISION")
    event_key: Mapped[str] = mapped_column(String(256), nullable=False)
    market: Mapped[str] = mapped_column(String(16), nullable=False)
    symbol: Mapped[str] = mapped_column(String(32), nullable=False)
    decision: Mapped[str] = mapped_column(String(16), nullable=False)
    as_of: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    data_cutoff: Mapped[str | None] = mapped_column(String(32))
    data_version: Mapped[str | None] = mapped_column(String(128))
    graph_version: Mapped[str | None] = mapped_column(String(128))
    skill_code: Mapped[str | None] = mapped_column(String(64), index=True)
    skill_version: Mapped[str | None] = mapped_column(String(32))
    model_instance_code: Mapped[str | None] = mapped_column(String(64))
    payload_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    evidence_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    payload_hash: Mapped[str] = mapped_column(String(64), nullable=False, index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)


class SelectionRetrospective(Base):
    """Versioned outcome record for a frozen decision snapshot.

    A new market-data refresh creates a new ``revision`` rather than changing
    an old review.  This preserves what was knowable at every review point.
    """

    __tablename__ = "selection_retrospective"
    __table_args__ = (
        UniqueConstraint("snapshot_id", "state_hash", name="uq_selection_retrospective_state"),
        Index("ix_selection_retrospective_status_time", "status", "evaluated_at"),
        Index("ix_selection_retrospective_symbol", "market", "symbol"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    snapshot_id: Mapped[int] = mapped_column(
        ForeignKey("selection_decision_snapshot.id", ondelete="CASCADE"), nullable=False, index=True
    )
    tracking_id: Mapped[int | None] = mapped_column(
        ForeignKey("selection_tracking.id", ondelete="SET NULL"), index=True
    )
    revision: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    market: Mapped[str] = mapped_column(String(16), nullable=False)
    symbol: Mapped[str] = mapped_column(String(32), nullable=False)
    status: Mapped[str] = mapped_column(String(24), nullable=False, default="IN_PROGRESS")
    evaluated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)
    observed_sessions: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    entry_price: Mapped[float | None] = mapped_column()
    final_price: Mapped[float | None] = mapped_column()
    return_pct: Mapped[float | None] = mapped_column()
    max_drawdown_pct: Mapped[float | None] = mapped_column()
    target_hit: Mapped[bool | None] = mapped_column(Boolean)
    stop_hit: Mapped[bool | None] = mapped_column(Boolean)
    direction_hit: Mapped[bool | None] = mapped_column(Boolean)
    attribution_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    error_tags_json: Mapped[list[str]] = mapped_column(JSON, nullable=False, default=list)
    summary: Mapped[str | None] = mapped_column(Text)
    reviewer: Mapped[str] = mapped_column(String(128), nullable=False, default="SYSTEM")
    state_hash: Mapped[str] = mapped_column(String(64), nullable=False, index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)


class SkillEvaluationCase(Base):
    """A versioned, reusable golden case for a Skill task."""

    __tablename__ = "skill_evaluation_case"
    __table_args__ = (Index("ix_skill_evaluation_case_task_enabled", "task_type", "enabled"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    case_code: Mapped[str] = mapped_column(String(128), unique=True, nullable=False)
    task_type: Mapped[str] = mapped_column(String(64), nullable=False, index=True)
    description: Mapped[str | None] = mapped_column(Text)
    input_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    expected_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    rubric_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    dataset_version: Mapped[str] = mapped_column(String(64), nullable=False, default="v1")
    source: Mapped[str] = mapped_column(String(32), nullable=False, default="HUMAN")
    enabled: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now, onupdate=utc_now)


class SkillEvaluationRun(Base):
    """One evaluation of a Skill version against a fixed case set."""

    __tablename__ = "skill_evaluation_run"
    __table_args__ = (Index("ix_skill_evaluation_run_skill_time", "skill_code", "started_at"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    skill_id: Mapped[int | None] = mapped_column(ForeignKey("model_skill.id", ondelete="SET NULL"), index=True)
    skill_code: Mapped[str] = mapped_column(String(64), nullable=False, index=True)
    skill_version: Mapped[str] = mapped_column(String(32), nullable=False)
    task_type: Mapped[str] = mapped_column(String(64), nullable=False)
    dataset_version: Mapped[str] = mapped_column(String(64), nullable=False, default="v1")
    evaluator: Mapped[str] = mapped_column(String(32), nullable=False, default="RULE")
    status: Mapped[str] = mapped_column(String(24), nullable=False, default="RUNNING")
    summary_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class SkillEvaluationResult(Base):
    """Immutable output and scored metrics for one run/case pair."""

    __tablename__ = "skill_evaluation_result"
    __table_args__ = (
        UniqueConstraint("run_id", "case_id", name="uq_skill_evaluation_result_case"),
        Index("ix_skill_evaluation_result_run_status", "run_id", "passed"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    run_id: Mapped[int] = mapped_column(ForeignKey("skill_evaluation_run.id", ondelete="CASCADE"), nullable=False, index=True)
    case_id: Mapped[int] = mapped_column(ForeignKey("skill_evaluation_case.id", ondelete="RESTRICT"), nullable=False, index=True)
    output_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    metrics_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    passed: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    error_type: Mapped[str | None] = mapped_column(String(64))
    latency_ms: Mapped[int | None] = mapped_column(Integer)
    evidence_refs_json: Mapped[list[Any]] = mapped_column(JSON, nullable=False, default=list)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)

