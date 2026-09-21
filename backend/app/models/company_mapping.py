"""Per-stock issuer resolution outcomes, separate from original security master data."""

from datetime import datetime
from typing import Any
from sqlalchemy import DateTime, ForeignKey, Integer, JSON, String
from sqlalchemy.orm import Mapped, mapped_column
from app.db.base import Base
from app.models.foundation import utc_now


class CompanyMappingState(Base):
    __tablename__ = "foundation_company_mapping_state"

    stock_symbol_id: Mapped[int] = mapped_column(ForeignKey("stock_symbol.id", ondelete="RESTRICT"), primary_key=True)
    status: Mapped[str] = mapped_column(String(32), nullable=False, index=True)
    reason: Mapped[str] = mapped_column(String(1024), nullable=False)
    source_name: Mapped[str | None] = mapped_column(String(256))
    source_snapshot: Mapped[str | None] = mapped_column(String(512))
    attempt_count: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    details_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    attempted_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)
