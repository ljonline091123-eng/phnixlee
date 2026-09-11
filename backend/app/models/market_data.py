from __future__ import annotations

from datetime import datetime, timezone
from typing import Any

from sqlalchemy import Boolean, DateTime, ForeignKey, Index, Integer, JSON, String, Text, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


class TimestampMixin:
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=utc_now,
        onupdate=utc_now,
        nullable=False,
    )


class DataSource(TimestampMixin, Base):
    __tablename__ = "data_source"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    source_code: Mapped[str] = mapped_column(String(64), unique=True, nullable=False, index=True)
    source_name: Mapped[str] = mapped_column(String(128), nullable=False)
    source_type: Mapped[str] = mapped_column(String(32), nullable=False, default="MARKET_DATA")
    adapter_type: Mapped[str] = mapped_column(String(64), nullable=False)
    priority: Mapped[int] = mapped_column(Integer, nullable=False, default=100)
    enabled: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    config_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    description: Mapped[str | None] = mapped_column(Text)

    interfaces: Mapped[list[DataInterface]] = relationship(
        back_populates="source",
        cascade="all, delete-orphan",
    )
    sync_logs: Mapped[list[DataSyncLog]] = relationship(back_populates="source")


class DataInterface(TimestampMixin, Base):
    __tablename__ = "data_interface"
    __table_args__ = (UniqueConstraint("source_id", "interface_code", name="uq_data_interface_source_code"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    source_id: Mapped[int] = mapped_column(ForeignKey("data_source.id", ondelete="CASCADE"), nullable=False)
    interface_code: Mapped[str] = mapped_column(String(64), nullable=False)
    interface_name: Mapped[str] = mapped_column(String(128), nullable=False)
    data_category: Mapped[str] = mapped_column(String(32), nullable=False)
    request_mode: Mapped[str] = mapped_column(String(32), nullable=False, default="SYNC")
    adapter_method: Mapped[str] = mapped_column(String(128), nullable=False)
    supported_markets: Mapped[list[str]] = mapped_column(JSON, nullable=False, default=list)
    input_schema: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    output_schema: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    enabled: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    description: Mapped[str | None] = mapped_column(Text)

    source: Mapped[DataSource] = relationship(back_populates="interfaces")


class DataSyncLog(Base):
    __tablename__ = "data_sync_log"
    __table_args__ = (Index("ix_data_sync_log_source_market_started", "source_id", "market", "started_at"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    source_id: Mapped[int] = mapped_column(ForeignKey("data_source.id", ondelete="RESTRICT"), nullable=False)
    interface_code: Mapped[str] = mapped_column(String(64), nullable=False)
    market: Mapped[str] = mapped_column(String(16), nullable=False)
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="RUNNING")
    total_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    inserted_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    updated_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    failed_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    error_message: Mapped[str | None] = mapped_column(Text)
    detail_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, nullable=False)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))

    source: Mapped[DataSource] = relationship(back_populates="sync_logs")


class StockSymbol(TimestampMixin, Base):
    __tablename__ = "stock_symbol"
    __table_args__ = (
        UniqueConstraint("market", "symbol", name="uq_stock_symbol_market_symbol"),
        Index("ix_stock_symbol_market_status", "market", "status"),
        Index("ix_stock_symbol_name", "name"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    market: Mapped[str] = mapped_column(String(16), nullable=False)
    symbol: Mapped[str] = mapped_column(String(32), nullable=False)
    exchange: Mapped[str] = mapped_column(String(32), nullable=False)
    name: Mapped[str] = mapped_column(String(128), nullable=False)
    asset_type: Mapped[str] = mapped_column(String(32), nullable=False, default="STOCK")
    status: Mapped[str] = mapped_column(String(32), nullable=False, default="LISTED")
    list_date: Mapped[str | None] = mapped_column(String(16))
    source_id: Mapped[int] = mapped_column(ForeignKey("data_source.id", ondelete="RESTRICT"), nullable=False)
    ext_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    raw_payload: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    last_synced_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, nullable=False)


class WatchlistItem(TimestampMixin, Base):
    __tablename__ = "watchlist_item"
    __table_args__ = (UniqueConstraint("market", "symbol", name="uq_watchlist_market_symbol"),)
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    market: Mapped[str] = mapped_column(String(16), nullable=False)
    symbol: Mapped[str] = mapped_column(String(32), nullable=False)
    note: Mapped[str | None] = mapped_column(String(256))


class StockKline(TimestampMixin, Base):
    __tablename__ = "stock_kline"
    __table_args__ = (
        UniqueConstraint(
            "market",
            "symbol",
            "period",
            "adjust",
            "trade_date",
            name="uq_stock_kline_market_symbol_period_adjust_date",
        ),
        Index("ix_stock_kline_market_symbol_date", "market", "symbol", "trade_date"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    market: Mapped[str] = mapped_column(String(16), nullable=False)
    symbol: Mapped[str] = mapped_column(String(32), nullable=False)
    period: Mapped[str] = mapped_column(String(16), nullable=False, default="daily")
    adjust: Mapped[str] = mapped_column(String(16), nullable=False, default="")
    trade_date: Mapped[str] = mapped_column(String(16), nullable=False)
    open_price: Mapped[float | None] = mapped_column()
    high_price: Mapped[float | None] = mapped_column()
    low_price: Mapped[float | None] = mapped_column()
    close_price: Mapped[float | None] = mapped_column()
    volume: Mapped[float | None] = mapped_column()
    amount: Mapped[float | None] = mapped_column()
    turnover_rate: Mapped[float | None] = mapped_column()
    source_id: Mapped[int] = mapped_column(ForeignKey("data_source.id", ondelete="RESTRICT"), nullable=False)
    raw_payload: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    fetched_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, nullable=False)


class StockFinancialReport(TimestampMixin, Base):
    __tablename__ = "stock_financial_report"
    __table_args__ = (
        UniqueConstraint(
            "market",
            "symbol",
            "indicator",
            "report_period",
            "source_id",
            name="uq_stock_financial_market_symbol_indicator_period_source",
        ),
        Index("ix_stock_financial_market_symbol_period", "market", "symbol", "report_period"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    market: Mapped[str] = mapped_column(String(16), nullable=False)
    symbol: Mapped[str] = mapped_column(String(32), nullable=False)
    indicator: Mapped[str] = mapped_column(String(64), nullable=False)
    report_period: Mapped[str] = mapped_column(String(32), nullable=False)
    currency: Mapped[str | None] = mapped_column(String(16))
    data_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    url: Mapped[str | None] = mapped_column(String(2048))
    source_id: Mapped[int] = mapped_column(ForeignKey("data_source.id", ondelete="RESTRICT"), nullable=False)
    fetched_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, nullable=False)


class StockNotice(TimestampMixin, Base):
    __tablename__ = "stock_notice"
    __table_args__ = (
        UniqueConstraint(
            "market",
            "symbol",
            "notice_date",
            "title",
            "source_id",
            name="uq_stock_notice_market_symbol_date_title_source",
        ),
        Index("ix_stock_notice_market_symbol_date", "market", "symbol", "notice_date"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    market: Mapped[str] = mapped_column(String(16), nullable=False)
    symbol: Mapped[str] = mapped_column(String(32), nullable=False)
    notice_date: Mapped[str] = mapped_column(String(32), nullable=False)
    title: Mapped[str] = mapped_column(String(512), nullable=False)
    notice_type: Mapped[str | None] = mapped_column(String(128))
    url: Mapped[str | None] = mapped_column(String(2048))
    content_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    source_id: Mapped[int] = mapped_column(ForeignKey("data_source.id", ondelete="RESTRICT"), nullable=False)
    fetched_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, nullable=False)


class StockRealtimeQuote(TimestampMixin, Base):
    __tablename__ = "stock_realtime_quote"
    __table_args__ = (
        UniqueConstraint("market", "symbol", name="uq_stock_realtime_quote_market_symbol"),
        Index("ix_stock_realtime_quote_market_updated", "market", "updated_at"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    market: Mapped[str] = mapped_column(String(16), nullable=False)
    symbol: Mapped[str] = mapped_column(String(32), nullable=False)
    quote_time: Mapped[str | None] = mapped_column(String(32))
    current_price: Mapped[float | None] = mapped_column()
    previous_close_price: Mapped[float | None] = mapped_column()
    open_price: Mapped[float | None] = mapped_column()
    high_price: Mapped[float | None] = mapped_column()
    low_price: Mapped[float | None] = mapped_column()
    volume: Mapped[float | None] = mapped_column()
    amount: Mapped[float | None] = mapped_column()
    change_amount: Mapped[float | None] = mapped_column()
    change_pct: Mapped[float | None] = mapped_column()
    turnover_rate: Mapped[float | None] = mapped_column()
    source_id: Mapped[int] = mapped_column(ForeignKey("data_source.id", ondelete="RESTRICT"), nullable=False)
    raw_payload: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    fetched_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, nullable=False)


class StockNews(TimestampMixin, Base):
    __tablename__ = "stock_news"
    __table_args__ = (
        UniqueConstraint(
            "market",
            "symbol",
            "news_time",
            "title",
            "source_id",
            name="uq_stock_news_market_symbol_time_title_source",
        ),
        Index("ix_stock_news_market_symbol_time", "market", "symbol", "news_time"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    market: Mapped[str] = mapped_column(String(16), nullable=False)
    symbol: Mapped[str] = mapped_column(String(32), nullable=False)
    news_time: Mapped[str] = mapped_column(String(32), nullable=False)
    title: Mapped[str] = mapped_column(String(512), nullable=False)
    content: Mapped[str | None] = mapped_column(Text)
    source_name: Mapped[str | None] = mapped_column(String(128))
    url: Mapped[str | None] = mapped_column(String(2048))
    content_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    source_id: Mapped[int] = mapped_column(ForeignKey("data_source.id", ondelete="RESTRICT"), nullable=False)
    fetched_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, nullable=False)


class StockF10Cache(TimestampMixin, Base):
    __tablename__ = "stock_f10_cache"
    __table_args__ = (
        UniqueConstraint("market", "symbol", "section", name="uq_stock_f10_cache_market_symbol_section"),
        Index("ix_stock_f10_cache_market_symbol", "market", "symbol"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    market: Mapped[str] = mapped_column(String(16), nullable=False)
    symbol: Mapped[str] = mapped_column(String(32), nullable=False)
    section: Mapped[str] = mapped_column(String(64), nullable=False)
    source_id: Mapped[int] = mapped_column(ForeignKey("data_source.id", ondelete="RESTRICT"), nullable=False)
    payload_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    fetched_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, nullable=False)


class DataFetchLog(Base):
    __tablename__ = "data_fetch_log"
    __table_args__ = (Index("ix_data_fetch_log_symbol_started", "market", "symbol", "started_at"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    source_id: Mapped[int] = mapped_column(ForeignKey("data_source.id", ondelete="RESTRICT"), nullable=False)
    interface_code: Mapped[str] = mapped_column(String(64), nullable=False)
    market: Mapped[str] = mapped_column(String(16), nullable=False)
    symbol: Mapped[str] = mapped_column(String(32), nullable=False)
    request_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="RUNNING")
    total_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    persisted_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    error_message: Mapped[str | None] = mapped_column(Text)
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, nullable=False)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
