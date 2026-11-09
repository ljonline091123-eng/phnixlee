"""Persistent, evidence-linked results for the standalone AI stock analyst."""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Any

from sqlalchemy import Boolean, DateTime, ForeignKey, Index, Integer, JSON, String, Text, UniqueConstraint, text
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


class AiStockAnalysisRecord(Base):
    """One immutable-input, versioned AI analyst run for a single security.

    This table is intentionally separate from selection candidates and their
    eight-dimension scorecards.  It stores the exact evidence snapshot and the
    model-authored conditional trend view without promoting either to facts.
    """

    __tablename__ = "ai_stock_analysis"
    __table_args__ = (
        Index("ix_ai_stock_analysis_identity_created", "market", "symbol", "created_at"),
        Index("ix_ai_stock_analysis_status_created", "status", "created_at"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    market: Mapped[str] = mapped_column(String(16), nullable=False, index=True)
    symbol: Mapped[str] = mapped_column(String(32), nullable=False, index=True)
    name: Mapped[str] = mapped_column(String(128), nullable=False, default="")
    period_months: Mapped[int] = mapped_column(Integer, nullable=False, default=12)
    status: Mapped[str] = mapped_column(String(24), nullable=False, default="RUNNING")
    model_instance_code: Mapped[str | None] = mapped_column(String(64))
    model_call_log_id: Mapped[int | None] = mapped_column(
        ForeignKey("model_call_log.id", ondelete="SET NULL"), index=True
    )
    skill_code: Mapped[str] = mapped_column(String(64), nullable=False, default="SENIOR_STOCK_TREND_ANALYST")
    skill_version: Mapped[str] = mapped_column(String(32), nullable=False, default="1.0.0")
    evidence_snapshot_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    coverage_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    result_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    error_message: Mapped[str | None] = mapped_column(Text)
    progress_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class AiAnalystDialogue(Base):
    __tablename__ = "ai_analyst_dialogue"
    __table_args__ = (Index("uq_ai_analyst_active_dialogue", "analysis_id", unique=True,
        sqlite_where=text("status IN ('QUEUED', 'RUNNING')"),
        postgresql_where=text("status IN ('QUEUED', 'RUNNING')")),)
    id: Mapped[int] = mapped_column(primary_key=True)
    analysis_id: Mapped[int] = mapped_column(ForeignKey("ai_stock_analysis.id"), index=True)
    status: Mapped[str] = mapped_column(String(24), default="QUEUED")
    question: Mapped[str] = mapped_column(Text)
    reply_json: Mapped[dict] = mapped_column(JSON, default=dict)
    context_snapshot_json: Mapped[dict] = mapped_column(JSON, default=dict)
    model_instance_code: Mapped[str | None] = mapped_column(String(64))
    model_call_log_id: Mapped[int | None] = mapped_column(ForeignKey("model_call_log.id"))
    skill_version: Mapped[str] = mapped_column(String(32), default="1.0.0")
    base_revision_id: Mapped[int | None] = mapped_column(Integer)
    error_message: Mapped[str | None] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class AiAnalystReportRevision(Base):
    __tablename__ = "ai_analyst_report_revision"
    __table_args__ = (UniqueConstraint("analysis_id", "version", name="uq_analyst_report_version"),)
    id: Mapped[int] = mapped_column(primary_key=True)
    analysis_id: Mapped[int] = mapped_column(ForeignKey("ai_stock_analysis.id"), index=True)
    version: Mapped[int] = mapped_column(Integer)
    source_type: Mapped[str] = mapped_column(String(32))
    dialogue_id: Mapped[int | None] = mapped_column(ForeignKey("ai_analyst_dialogue.id"))
    judgment: Mapped[str] = mapped_column(Text)
    focus_points_json: Mapped[list] = mapped_column(JSON, default=list)
    report_json: Mapped[dict] = mapped_column(JSON, default=dict)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now)


class AiAnalystWatchSuggestion(Base):
    """AI authored draft only; never enables a watch or overwrites saved conditions."""
    __tablename__ = "ai_analyst_watch_suggestion"
    __table_args__ = (Index("uq_ai_analyst_active_suggestion", "analysis_id", unique=True,
        sqlite_where=text("status IN ('QUEUED', 'RUNNING')"),
        postgresql_where=text("status IN ('QUEUED', 'RUNNING')")),)
    id: Mapped[int] = mapped_column(primary_key=True)
    analysis_id: Mapped[int] = mapped_column(ForeignKey("ai_stock_analysis.id"), index=True)
    status: Mapped[str] = mapped_column(String(24), default="QUEUED")
    result_json: Mapped[dict] = mapped_column(JSON, default=dict)
    evidence_snapshot_json: Mapped[dict] = mapped_column(JSON, default=dict)
    model_instance_code: Mapped[str | None] = mapped_column(String(64))
    model_call_log_id: Mapped[int | None] = mapped_column(ForeignKey("model_call_log.id"))
    skill_version: Mapped[str] = mapped_column(String(32), default="1.0.0")
    error_message: Mapped[str | None] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class AiAnalystWatch(Base):
    __tablename__ = "ai_analyst_watch"
    id: Mapped[int] = mapped_column(primary_key=True)
    analysis_id: Mapped[int] = mapped_column(ForeignKey("ai_stock_analysis.id"), unique=True)
    enabled: Mapped[bool] = mapped_column(Boolean, default=True)
    config_json: Mapped[dict] = mapped_column(JSON, default=dict)
    model_instance_code: Mapped[str | None] = mapped_column(String(64))
    config_version: Mapped[int] = mapped_column(Integer, default=1)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, onupdate=utc_now)


class AiAnalystDailyReview(Base):
    __tablename__ = "ai_analyst_daily_review"
    __table_args__ = (UniqueConstraint("watch_id", "business_date", "attempt", name="uq_analyst_daily_attempt"),)
    id: Mapped[int] = mapped_column(primary_key=True)
    watch_id: Mapped[int] = mapped_column(ForeignKey("ai_analyst_watch.id"), index=True)
    business_date: Mapped[str] = mapped_column(String(10))
    attempt: Mapped[int] = mapped_column(Integer, default=1)
    status: Mapped[str] = mapped_column(String(24), default="QUEUED")
    progress_json: Mapped[dict] = mapped_column(JSON, default=dict)
    config_snapshot_json: Mapped[dict] = mapped_column(JSON, default=dict)
    report_revision_id: Mapped[int | None] = mapped_column(ForeignKey("ai_analyst_report_revision.id"))
    evidence_snapshot_json: Mapped[dict] = mapped_column(JSON, default=dict)
    conditions_json: Mapped[list] = mapped_column(JSON, default=list)
    result_json: Mapped[dict] = mapped_column(JSON, default=dict)
    model_call_log_id: Mapped[int | None] = mapped_column(ForeignKey("model_call_log.id"))
    model_instance_code: Mapped[str | None] = mapped_column(String(64))
    skill_version: Mapped[str] = mapped_column(String(32), default="1.0.0")
    error_message: Mapped[str | None] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class AiAnalystForecast(Base):
    """Immutable analyst-specific contract; canonical prices/outcomes live in PredictionLedger."""
    __tablename__ = "ai_analyst_forecast"
    __table_args__ = (UniqueConstraint("review_id", "ordinal", name="uq_analyst_forecast_origin"),)
    id: Mapped[int] = mapped_column(primary_key=True)
    analysis_id: Mapped[int] = mapped_column(ForeignKey("ai_stock_analysis.id"), index=True)
    review_id: Mapped[int] = mapped_column(ForeignKey("ai_analyst_daily_review.id"), index=True)
    ledger_id: Mapped[int] = mapped_column(ForeignKey("prediction_ledger.id"), unique=True)
    ordinal: Mapped[int] = mapped_column(Integer)
    contract_json: Mapped[dict] = mapped_column(JSON, default=dict)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now)


class AiAnalystForecastCheck(Base):
    """Append-only validation snapshots, including unresolved and insufficient-data checks."""
    __tablename__ = "ai_analyst_forecast_check"
    __table_args__ = (UniqueConstraint("forecast_id", "review_id", name="uq_analyst_forecast_check"),)
    id: Mapped[int] = mapped_column(primary_key=True)
    forecast_id: Mapped[int] = mapped_column(ForeignKey("ai_analyst_forecast.id"), index=True)
    review_id: Mapped[int] = mapped_column(ForeignKey("ai_analyst_daily_review.id"), index=True)
    status: Mapped[str] = mapped_column(String(32))
    result_json: Mapped[dict] = mapped_column(JSON, default=dict)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now)
