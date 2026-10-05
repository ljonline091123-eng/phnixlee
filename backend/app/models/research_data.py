from __future__ import annotations

from datetime import datetime, timezone
from typing import Any

from sqlalchemy import DateTime, Index, Integer, JSON, String, Text, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


class TimestampMixin:
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=utc_now, onupdate=utc_now, nullable=False
    )


class StockBrokerResearchReport(TimestampMixin, Base):
    """Normalized public brokerage research report and locally cached content."""

    __tablename__ = "stock_broker_research_report"
    __table_args__ = (
        UniqueConstraint(
            "market", "symbol", "source_code", "external_id",
            name="uq_stock_broker_report_source_external",
        ),
        Index("ix_stock_broker_report_symbol_date", "market", "symbol", "report_date"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    market: Mapped[str] = mapped_column(String(16), nullable=False)
    symbol: Mapped[str] = mapped_column(String(32), nullable=False)
    source_code: Mapped[str] = mapped_column(String(32), nullable=False)
    external_id: Mapped[str] = mapped_column(String(128), nullable=False)
    title: Mapped[str] = mapped_column(String(1024), nullable=False)
    report_date: Mapped[str] = mapped_column(String(32), nullable=False, default="")
    institution: Mapped[str | None] = mapped_column(String(256))
    analysts_json: Mapped[list[str]] = mapped_column(JSON, nullable=False, default=list)
    rating: Mapped[str | None] = mapped_column(String(64))
    summary: Mapped[str | None] = mapped_column(Text)
    content_text: Mapped[str | None] = mapped_column(Text)
    content_hash: Mapped[str | None] = mapped_column(String(64))
    content_status: Mapped[str] = mapped_column(String(32), nullable=False, default="PENDING")
    fetch_error: Mapped[str | None] = mapped_column(Text)
    source_url: Mapped[str | None] = mapped_column(String(2048))
    detail_url: Mapped[str | None] = mapped_column(String(2048))
    pdf_url: Mapped[str | None] = mapped_column(String(2048))
    source_updated_at: Mapped[str | None] = mapped_column(String(64))
    fetched_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, nullable=False)
    raw_payload: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)


class StockEarningsConsensus(TimestampMixin, Base):
    """Per-stock, per-year consensus aggregate from a traceable public source."""

    __tablename__ = "stock_earnings_consensus"
    __table_args__ = (
        UniqueConstraint(
            "market", "symbol", "source_code", "forecast_year", "metric_code",
            name="uq_stock_earnings_consensus_metric",
        ),
        Index("ix_stock_earnings_consensus_symbol_year", "market", "symbol", "forecast_year"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    market: Mapped[str] = mapped_column(String(16), nullable=False)
    symbol: Mapped[str] = mapped_column(String(32), nullable=False)
    source_code: Mapped[str] = mapped_column(String(32), nullable=False)
    forecast_year: Mapped[str] = mapped_column(String(8), nullable=False)
    metric_code: Mapped[str] = mapped_column(String(64), nullable=False)
    metric_name: Mapped[str] = mapped_column(String(128), nullable=False)
    unit: Mapped[str | None] = mapped_column(String(32))
    prediction_count: Mapped[int | None] = mapped_column(Integer)
    minimum_value: Mapped[str | None] = mapped_column(String(64))
    mean_value: Mapped[str | None] = mapped_column(String(64))
    maximum_value: Mapped[str | None] = mapped_column(String(64))
    industry_average: Mapped[str | None] = mapped_column(String(64))
    source_url: Mapped[str | None] = mapped_column(String(2048))
    source_updated_at: Mapped[str | None] = mapped_column(String(64))
    fetched_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, nullable=False)
    raw_payload: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)


class StockInstitutionForecast(TimestampMixin, Base):
    """Dated institution forecast row, kept separate from brokerage reports."""

    __tablename__ = "stock_institution_forecast"
    __table_args__ = (
        UniqueConstraint(
            "market", "symbol", "source_code", "external_key",
            name="uq_stock_institution_forecast_external",
        ),
        Index("ix_stock_institution_forecast_symbol_date", "market", "symbol", "report_date"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    market: Mapped[str] = mapped_column(String(16), nullable=False)
    symbol: Mapped[str] = mapped_column(String(32), nullable=False)
    source_code: Mapped[str] = mapped_column(String(32), nullable=False)
    external_key: Mapped[str] = mapped_column(String(160), nullable=False)
    institution: Mapped[str] = mapped_column(String(256), nullable=False, default="")
    analysts_json: Mapped[list[str]] = mapped_column(JSON, nullable=False, default=list)
    report_date: Mapped[str] = mapped_column(String(32), nullable=False, default="")
    rating: Mapped[str | None] = mapped_column(String(64))
    forecast_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    report_external_id: Mapped[str | None] = mapped_column(String(128))
    source_url: Mapped[str | None] = mapped_column(String(2048))
    detail_url: Mapped[str | None] = mapped_column(String(2048))
    source_updated_at: Mapped[str | None] = mapped_column(String(64))
    fetched_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, nullable=False)
    raw_payload: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)


class StockInvestorQA(TimestampMixin, Base):
    """Normalized CNINFO investor Q&A, persisted independently of F10 cache."""

    __tablename__ = "stock_investor_qa"
    __table_args__ = (
        UniqueConstraint(
            "market", "symbol", "source_code", "external_id",
            name="uq_stock_investor_qa_external",
        ),
        Index("ix_stock_investor_qa_symbol_time", "market", "symbol", "updated_time"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    market: Mapped[str] = mapped_column(String(16), nullable=False)
    symbol: Mapped[str] = mapped_column(String(32), nullable=False)
    source_code: Mapped[str] = mapped_column(String(32), nullable=False)
    external_id: Mapped[str] = mapped_column(String(160), nullable=False)
    question: Mapped[str] = mapped_column(Text, nullable=False)
    answer: Mapped[str | None] = mapped_column(Text)
    questioner: Mapped[str | None] = mapped_column(String(256))
    answerer: Mapped[str | None] = mapped_column(String(256))
    asked_time: Mapped[str | None] = mapped_column(String(64))
    answered_time: Mapped[str | None] = mapped_column(String(64))
    updated_time: Mapped[str | None] = mapped_column(String(64))
    source_url: Mapped[str | None] = mapped_column(String(2048))
    source_updated_at: Mapped[str | None] = mapped_column(String(64))
    fetched_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, nullable=False)
    raw_payload: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
