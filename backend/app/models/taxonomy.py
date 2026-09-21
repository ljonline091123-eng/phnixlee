"""Versioned master definitions for industry, theme and security classifications.

These definitions are deliberately separate from ``SecurityClassification`` facts:
the latter records that a provider/rule assigned a label to one security, while
this table explains what the label means and how it is defined.
"""

from datetime import date, datetime
from typing import Any

from sqlalchemy import JSON, Date, DateTime, Index, String, Text, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base
from app.models.foundation import new_id, utc_now


class ClassificationDefinition(Base):
    # Kept outside the isolated foundation migration history so older
    # deployments can receive it through the normal additive create_all path.
    __tablename__ = "classification_definition"
    __table_args__ = (
        UniqueConstraint(
            "taxonomy", "dimension", "code", "definition_version",
            name="uq_classification_definition_version",
        ),
        Index("ix_classification_definition_lookup", "dimension", "status", "label"),
    )

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    taxonomy: Mapped[str] = mapped_column(String(128), nullable=False)
    dimension: Mapped[str] = mapped_column(String(64), nullable=False)
    code: Mapped[str] = mapped_column(String(128), nullable=False)
    label: Mapped[str] = mapped_column(String(256), nullable=False)
    definition: Mapped[str] = mapped_column(Text, nullable=False)
    criteria: Mapped[str | None] = mapped_column(Text)
    parent_code: Mapped[str | None] = mapped_column(String(128))
    jurisdiction: Mapped[str] = mapped_column(String(64), nullable=False, default="GLOBAL")
    source_name: Mapped[str] = mapped_column(String(256), nullable=False)
    source_url: Mapped[str | None] = mapped_column(String(2048))
    definition_version: Mapped[str] = mapped_column(String(128), nullable=False)
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="ACTIVE")
    valid_from: Mapped[date | None] = mapped_column(Date)
    valid_to: Mapped[date | None] = mapped_column(Date)
    properties_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)
