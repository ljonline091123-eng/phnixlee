"""Deterministic L0 change-detection events.

One row per ``(market, symbol, event_type, event_date)``.  Rows are produced
only by :mod:`app.services.change_detection`; thresholds live in
:mod:`app.services.l0_profile`.  ``profile_version`` keeps every detection
attributable to the exact rule set that produced it.
"""

from __future__ import annotations

from datetime import datetime
from typing import Any

from sqlalchemy import DateTime, Index, Integer, JSON, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base
from app.models.market_data import TimestampMixin, utc_now


class StockChangeEvent(TimestampMixin, Base):
    __tablename__ = "stock_change_event"
    __table_args__ = (
        UniqueConstraint("dedup_key", name="uq_stock_change_event_dedup_key"),
        Index("ix_stock_change_event_market_symbol_date", "market", "symbol", "event_date"),
        Index("ix_stock_change_event_date_type", "event_date", "event_type"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    market: Mapped[str] = mapped_column(String(16), nullable=False)
    symbol: Mapped[str] = mapped_column(String(32), nullable=False)
    event_date: Mapped[str] = mapped_column(String(16), nullable=False)
    event_type: Mapped[str] = mapped_column(String(48), nullable=False)
    direction: Mapped[str] = mapped_column(String(16), nullable=False, default="NEUTRAL")
    signal_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    source_ids_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    dedup_key: Mapped[str] = mapped_column(String(160), nullable=False)
    profile_version: Mapped[str] = mapped_column(String(32), nullable=False)
    status: Mapped[str] = mapped_column(String(24), nullable=False, default="DETECTED")
    detected_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, nullable=False)
