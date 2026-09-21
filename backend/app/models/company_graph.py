"""Security-level classifications, separate from company relationship facts."""

from datetime import date, datetime
from typing import Any

from sqlalchemy import JSON, Date, DateTime, ForeignKey, Index, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base
from app.models.foundation import new_id, utc_now


class SourceIdentity(Base):
    __tablename__ = "foundation_source_identity"
    __table_args__ = (UniqueConstraint("namespace", "external_id", "entity_type", name="uq_foundation_source_identity"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    entity_id: Mapped[str] = mapped_column(ForeignKey("foundation_entity.id", ondelete="RESTRICT"), nullable=False, index=True)
    namespace: Mapped[str] = mapped_column(String(256), nullable=False)
    external_id: Mapped[str] = mapped_column(String(128), nullable=False)
    entity_type: Mapped[str] = mapped_column(String(32), nullable=False)
    jurisdiction: Mapped[str] = mapped_column(String(64), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)


class SecurityClassification(Base):
    __tablename__ = "foundation_security_classification"
    __table_args__ = (Index("ix_foundation_classification_security_status", "security_id", "status"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    security_id: Mapped[str] = mapped_column(ForeignKey("foundation_security.id", ondelete="RESTRICT"), nullable=False)
    evidence_id: Mapped[str] = mapped_column(ForeignKey("foundation_evidence.id", ondelete="RESTRICT"), nullable=False)
    dimension: Mapped[str] = mapped_column(String(64), nullable=False)
    code: Mapped[str] = mapped_column(String(128), nullable=False)
    label: Mapped[str] = mapped_column(String(256), nullable=False)
    definition_version: Mapped[str] = mapped_column(String(128), nullable=False)
    method: Mapped[str] = mapped_column(String(256), nullable=False)
    properties_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="PENDING")
    valid_from: Mapped[date | None] = mapped_column(Date)
    valid_to: Mapped[date | None] = mapped_column(Date)
    reviews_json: Mapped[list[dict[str, Any]]] = mapped_column(JSON, nullable=False, default=list)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)
