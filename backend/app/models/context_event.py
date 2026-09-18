"""Traceable external evidence supplied for a specific security."""
from datetime import datetime

from sqlalchemy import DateTime, Integer, String, Text, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base
from app.models.market_data import TimestampMixin


class StockContextEvent(TimestampMixin, Base):
    __tablename__ = "stock_context_event"
    __table_args__ = (UniqueConstraint("market", "symbol", "content_hash", name="uq_context_event_content"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    market: Mapped[str] = mapped_column(String(16), index=True)
    symbol: Mapped[str] = mapped_column(String(32), index=True)
    event_type: Mapped[str] = mapped_column(String(32))
    title: Mapped[str] = mapped_column(String(512))
    content: Mapped[str] = mapped_column(Text)
    source_name: Mapped[str] = mapped_column(String(256))
    url: Mapped[str] = mapped_column(String(2048))
    published_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    related_entity: Mapped[str | None] = mapped_column(String(256))
    content_hash: Mapped[str] = mapped_column(String(64))
