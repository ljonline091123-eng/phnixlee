"""Independent master identities, immutable evidence and reviewed facts."""

from datetime import date, datetime, timezone
from typing import Any
from uuid import uuid4

from sqlalchemy import JSON, Date, DateTime, ForeignKey, Index, Integer, String, Text, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


def new_id() -> str:
    return str(uuid4())


class FoundationEntity(Base):
    __tablename__ = "foundation_entity"
    __table_args__ = (UniqueConstraint("jurisdiction", "identifier_scheme", "identifier_value", name="uq_foundation_entity_identifier"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    name: Mapped[str] = mapped_column(String(256), nullable=False, index=True)
    entity_type: Mapped[str] = mapped_column(String(32), nullable=False, index=True)
    jurisdiction: Mapped[str] = mapped_column(String(64), nullable=False)
    identifier_scheme: Mapped[str | None] = mapped_column(String(64))
    identifier_value: Mapped[str | None] = mapped_column(String(128))
    properties_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)


class FoundationSecurity(Base):
    __tablename__ = "foundation_security"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    entity_id: Mapped[str] = mapped_column(ForeignKey("foundation_entity.id", ondelete="RESTRICT"), nullable=False, index=True)
    share_class: Mapped[str | None] = mapped_column(String(64))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)


class FoundationListing(Base):
    __tablename__ = "foundation_listing"
    __table_args__ = (UniqueConstraint("stock_symbol_id", name="uq_foundation_listing_stock"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    security_id: Mapped[str] = mapped_column(ForeignKey("foundation_security.id", ondelete="RESTRICT"), nullable=False, index=True)
    stock_symbol_id: Mapped[int] = mapped_column(ForeignKey("stock_symbol.id", ondelete="RESTRICT"), nullable=False)
    market: Mapped[str] = mapped_column(String(16), nullable=False)
    symbol: Mapped[str] = mapped_column(String(32), nullable=False)
    name: Mapped[str] = mapped_column(String(128), nullable=False)
    evidence_id: Mapped[str] = mapped_column(ForeignKey("foundation_evidence.id", ondelete="RESTRICT"), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)


class FoundationEvidence(Base):
    __tablename__ = "foundation_evidence"
    __table_args__ = (
        UniqueConstraint("source_name", "source_key", "version", name="uq_foundation_evidence_version"),
        UniqueConstraint("source_name", "source_key", "fingerprint", name="uq_foundation_evidence_fingerprint"),
    )

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    entity_id: Mapped[str | None] = mapped_column(ForeignKey("foundation_entity.id", ondelete="RESTRICT"), index=True)
    source_name: Mapped[str] = mapped_column(String(256), nullable=False)
    source_key: Mapped[str] = mapped_column(String(512), nullable=False)
    title: Mapped[str] = mapped_column(String(512), nullable=False)
    content: Mapped[str] = mapped_column(Text, nullable=False)
    url: Mapped[str | None] = mapped_column(String(2048))
    published_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    available_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    content_hash: Mapped[str] = mapped_column(String(64), nullable=False)
    fingerprint: Mapped[str] = mapped_column(String(64), nullable=False)
    version: Mapped[int] = mapped_column(Integer, nullable=False)
    metadata_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)


class FoundationFact(Base):
    __tablename__ = "foundation_fact"
    __table_args__ = (Index("ix_foundation_fact_status_type", "status", "fact_type"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    fact_type: Mapped[str] = mapped_column(String(32), nullable=False)
    title: Mapped[str] = mapped_column(String(512), nullable=False)
    subject_entity_id: Mapped[str] = mapped_column(ForeignKey("foundation_entity.id", ondelete="RESTRICT"), nullable=False, index=True)
    object_entity_id: Mapped[str | None] = mapped_column(ForeignKey("foundation_entity.id", ondelete="RESTRICT"), index=True)
    properties_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="PENDING")
    valid_from: Mapped[date | None] = mapped_column(Date)
    valid_to: Mapped[date | None] = mapped_column(Date)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)


class FoundationFactEvidence(Base):
    __tablename__ = "foundation_fact_evidence"

    fact_id: Mapped[str] = mapped_column(ForeignKey("foundation_fact.id", ondelete="RESTRICT"), primary_key=True)
    evidence_id: Mapped[str] = mapped_column(ForeignKey("foundation_evidence.id", ondelete="RESTRICT"), primary_key=True)


class FoundationFactReview(Base):
    __tablename__ = "foundation_fact_review"
    __table_args__ = (Index("ix_foundation_review_fact_time", "fact_id", "created_at"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    fact_id: Mapped[str] = mapped_column(ForeignKey("foundation_fact.id", ondelete="RESTRICT"), nullable=False)
    previous_status: Mapped[str] = mapped_column(String(16), nullable=False)
    decision: Mapped[str] = mapped_column(String(16), nullable=False)
    reason: Mapped[str] = mapped_column(String(4000), nullable=False)
    reviewer: Mapped[str] = mapped_column(String(128), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)
