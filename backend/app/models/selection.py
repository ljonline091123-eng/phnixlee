"""Persistent state for the human-reviewed stock-selection watch workflow."""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Any

from sqlalchemy import Boolean, DateTime, Float, ForeignKey, Index, Integer, JSON, String, Text, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base
from app.models.market_data import TimestampMixin


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


class SelectionRun(TimestampMixin, Base):
    __tablename__ = "selection_run"
    __table_args__ = (Index("ix_selection_run_status_created", "status", "created_at"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    market: Mapped[str] = mapped_column(String(16), nullable=False)
    universe_scope: Mapped[str] = mapped_column(String(64), nullable=False, default="LOCAL_DATA")
    candidate_limit: Mapped[int] = mapped_column(Integer, nullable=False, default=30)
    criteria_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    model_instance_code: Mapped[str | None] = mapped_column(String(64))
    knowledge_base_ids_json: Mapped[list[int]] = mapped_column(JSON, nullable=False, default=list)
    graph_ids_json: Mapped[list[int]] = mapped_column(JSON, nullable=False, default=list)
    data_mode: Mapped[str] = mapped_column(String(16), nullable=False, default="REAL")
    analysis_mode: Mapped[str] = mapped_column(String(32), nullable=False, default="LOCAL_EVIDENCE")
    status: Mapped[str] = mapped_column(String(24), nullable=False, default="REVIEW")
    candidate_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    reviewed_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    tracking_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    missing_data_json: Mapped[list[str]] = mapped_column(JSON, nullable=False, default=list)
    error_message: Mapped[str | None] = mapped_column(Text)
    as_of: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, nullable=False)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class SelectionCandidate(TimestampMixin, Base):
    __tablename__ = "selection_candidate"
    __table_args__ = (
        UniqueConstraint("run_id", "market", "symbol", name="uq_selection_candidate_run_symbol"),
        Index("ix_selection_candidate_decision", "decision"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    run_id: Mapped[int] = mapped_column(ForeignKey("selection_run.id", ondelete="CASCADE"), nullable=False, index=True)
    market: Mapped[str] = mapped_column(String(16), nullable=False)
    symbol: Mapped[str] = mapped_column(String(32), nullable=False)
    name: Mapped[str | None] = mapped_column(String(128))
    entry_price: Mapped[float | None] = mapped_column()
    entry_date: Mapped[str | None] = mapped_column(String(16))
    hard_score: Mapped[float] = mapped_column(nullable=False, default=0)
    hard_rules_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    evidence_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    analysis_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    decision: Mapped[str] = mapped_column(String(16), nullable=False, default="PENDING")
    review_notes: Mapped[str | None] = mapped_column(Text)
    target_price: Mapped[float | None] = mapped_column()
    stop_price: Mapped[float | None] = mapped_column()
    target_return_pct: Mapped[float | None] = mapped_column()
    confidence: Mapped[float | None] = mapped_column()
    prediction_id: Mapped[int | None] = mapped_column(ForeignKey("prediction_ledger.id", ondelete="SET NULL"))
    reviewed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class SelectionTracking(TimestampMixin, Base):
    __tablename__ = "selection_tracking"
    __table_args__ = (Index("ix_selection_tracking_status", "status"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    candidate_id: Mapped[int] = mapped_column(ForeignKey("selection_candidate.id", ondelete="CASCADE"), nullable=False, unique=True)
    market: Mapped[str] = mapped_column(String(16), nullable=False)
    symbol: Mapped[str] = mapped_column(String(32), nullable=False)
    entry_price: Mapped[float] = mapped_column(nullable=False)
    entry_date: Mapped[str] = mapped_column(String(16), nullable=False)
    confirmation_date: Mapped[str] = mapped_column(String(16), nullable=False)
    adjust: Mapped[str] = mapped_column(String(16), nullable=False, default="")
    target_price: Mapped[float | None] = mapped_column()
    stop_price: Mapped[float | None] = mapped_column()
    target_return_pct: Mapped[float | None] = mapped_column()
    confidence: Mapped[float | None] = mapped_column()
    required_sessions: Mapped[int] = mapped_column(Integer, nullable=False, default=10)
    observed_sessions: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    status: Mapped[str] = mapped_column(String(24), nullable=False, default="PENDING_DATA")
    latest_date: Mapped[str | None] = mapped_column(String(16))
    latest_price: Mapped[float | None] = mapped_column()
    cumulative_return_pct: Mapped[float | None] = mapped_column()
    max_drawdown_pct: Mapped[float | None] = mapped_column()
    target_hit: Mapped[bool | None] = mapped_column(Boolean)
    stop_hit: Mapped[bool | None] = mapped_column(Boolean)
    direction_hit: Mapped[bool | None] = mapped_column(Boolean)
    data_source: Mapped[str] = mapped_column(String(64), nullable=False, default="LOCAL_STOCK_KLINE")
    data_mode: Mapped[str] = mapped_column(String(16), nullable=False, default="REAL")
    review_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, nullable=False)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class SelectionSnapshot(Base):
    __tablename__ = "selection_snapshot"
    __table_args__ = (
        UniqueConstraint("tracking_id", "trade_date", name="uq_selection_snapshot_tracking_date"),
        Index("ix_selection_snapshot_tracking_date", "tracking_id", "trade_date"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    tracking_id: Mapped[int] = mapped_column(ForeignKey("selection_tracking.id", ondelete="CASCADE"), nullable=False)
    trade_date: Mapped[str] = mapped_column(String(16), nullable=False)
    close_price: Mapped[float] = mapped_column(nullable=False)
    return_pct: Mapped[float] = mapped_column(nullable=False)
    drawdown_pct: Mapped[float] = mapped_column(nullable=False, default=0)
    sequence: Mapped[int] = mapped_column(Integer, nullable=False)
    source: Mapped[str] = mapped_column(String(64), nullable=False, default="LOCAL_STOCK_KLINE")
    data_mode: Mapped[str] = mapped_column(String(16), nullable=False, default="REAL")


class SelectionDimensionDefinition(TimestampMixin, Base):
    """Version-independent meaning and display contract for one score axis."""

    __tablename__ = "selection_dimension_definition"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    dimension_code: Mapped[str] = mapped_column(String(64), nullable=False, unique=True, index=True)
    dimension_name: Mapped[str] = mapped_column(String(128), nullable=False)
    description: Mapped[str] = mapped_column(Text, nullable=False)
    score_interpretation: Mapped[str] = mapped_column(Text, nullable=False)
    default_method: Mapped[str] = mapped_column(String(32), nullable=False, default="HYBRID")
    sort_order: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    enabled: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)


class SelectionStrategyProfile(TimestampMixin, Base):
    """Immutable versioned weighting profile; only lifecycle status changes."""

    __tablename__ = "selection_strategy_profile"
    __table_args__ = (
        UniqueConstraint("profile_code", "version", name="uq_selection_strategy_profile_version"),
        Index("ix_selection_strategy_profile_status", "status", "enabled"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    profile_code: Mapped[str] = mapped_column(String(64), nullable=False)
    profile_name: Mapped[str] = mapped_column(String(128), nullable=False)
    version: Mapped[str] = mapped_column(String(32), nullable=False)
    description: Mapped[str] = mapped_column(Text, nullable=False, default="")
    horizon_code: Mapped[str] = mapped_column(String(32), nullable=False, default="T3_T5_T7")
    market_scope_json: Mapped[list[str]] = mapped_column(JSON, nullable=False, default=list)
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="DRAFT")
    enabled: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    published_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    parent_profile_id: Mapped[int | None] = mapped_column(ForeignKey("selection_strategy_profile.id", ondelete="SET NULL"))


class SelectionStrategyProfileItem(Base):
    __tablename__ = "selection_strategy_profile_item"
    __table_args__ = (
        UniqueConstraint("profile_id", "dimension_code", name="uq_selection_profile_dimension"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    profile_id: Mapped[int] = mapped_column(ForeignKey("selection_strategy_profile.id", ondelete="CASCADE"), nullable=False, index=True)
    dimension_code: Mapped[str] = mapped_column(ForeignKey("selection_dimension_definition.dimension_code", ondelete="RESTRICT"), nullable=False)
    weight: Mapped[float] = mapped_column(Float, nullable=False)
    minimum_coverage: Mapped[float] = mapped_column(Float, nullable=False, default=0.4)
    minimum_confidence: Mapped[float] = mapped_column(Float, nullable=False, default=0.4)
    skill_code: Mapped[str] = mapped_column(String(128), nullable=False)
    skill_version: Mapped[str] = mapped_column(String(32), nullable=False, default="1.0.0")


class SelectionDimensionResult(TimestampMixin, Base):
    __tablename__ = "selection_dimension_result"
    __table_args__ = (
        UniqueConstraint("candidate_id", "profile_id", "dimension_code", name="uq_selection_candidate_profile_dimension"),
        Index("ix_selection_dimension_result_code", "dimension_code", "status"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    candidate_id: Mapped[int] = mapped_column(ForeignKey("selection_candidate.id", ondelete="CASCADE"), nullable=False, index=True)
    profile_id: Mapped[int] = mapped_column(ForeignKey("selection_strategy_profile.id", ondelete="RESTRICT"), nullable=False)
    dimension_code: Mapped[str] = mapped_column(String(64), nullable=False)
    status: Mapped[str] = mapped_column(String(24), nullable=False)
    direction: Mapped[str] = mapped_column(String(16), nullable=False, default="UNKNOWN")
    score: Mapped[float | None] = mapped_column(Float)
    coverage: Mapped[float] = mapped_column(Float, nullable=False, default=0)
    evidence_confidence: Mapped[float] = mapped_column(Float, nullable=False, default=0)
    weight: Mapped[float] = mapped_column(Float, nullable=False)
    weighted_contribution: Mapped[float | None] = mapped_column(Float)
    method: Mapped[str] = mapped_column(String(32), nullable=False)
    summary: Mapped[str] = mapped_column(Text, nullable=False)
    metrics_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    evidence_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    warnings_json: Mapped[list[str]] = mapped_column(JSON, nullable=False, default=list)
    as_of: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)


class SelectionEvaluationMetric(TimestampMixin, Base):
    __tablename__ = "selection_evaluation_metric"
    __table_args__ = (
        UniqueConstraint("candidate_id", "horizon_sessions", name="uq_selection_candidate_horizon_metric"),
        Index("ix_selection_evaluation_profile_horizon", "profile_id", "horizon_sessions"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    candidate_id: Mapped[int] = mapped_column(ForeignKey("selection_candidate.id", ondelete="CASCADE"), nullable=False, index=True)
    profile_id: Mapped[int] = mapped_column(ForeignKey("selection_strategy_profile.id", ondelete="RESTRICT"), nullable=False)
    horizon_sessions: Mapped[int] = mapped_column(Integer, nullable=False)
    status: Mapped[str] = mapped_column(String(24), nullable=False, default="PENDING_DATA")
    return_pct: Mapped[float | None] = mapped_column(Float)
    benchmark_return_pct: Mapped[float | None] = mapped_column(Float)
    excess_return_pct: Mapped[float | None] = mapped_column(Float)
    max_drawdown_pct: Mapped[float | None] = mapped_column(Float)
    direction_hit: Mapped[bool | None] = mapped_column(Boolean)
    observed_date: Mapped[str | None] = mapped_column(String(16))
    details_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
