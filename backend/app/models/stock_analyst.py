"""Persistent, evidence-linked results for the standalone AI stock analyst."""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Any

from sqlalchemy import DateTime, ForeignKey, Index, Integer, JSON, String, Text
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
