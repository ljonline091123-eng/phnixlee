"""Lakehouse-compatible catalog, object and lineage records.

The catalog is deliberately small: SQLite/PostgreSQL remains the transaction
store while raw bytes and analytical snapshots can live in filesystem/MinIO.
"""
from datetime import datetime, timezone
from typing import Any
from uuid import uuid4

from sqlalchemy import DateTime, Integer, JSON, String, Text, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


class LakeObject(Base):
    __tablename__ = "lake_object"
    __table_args__ = (UniqueConstraint("content_hash", name="uq_lake_object_content_hash"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid4()))
    object_uri: Mapped[str] = mapped_column(String(2048), nullable=False, index=True)
    layer: Mapped[str] = mapped_column(String(16), nullable=False, index=True)
    bucket: Mapped[str | None] = mapped_column(String(128))
    object_key: Mapped[str | None] = mapped_column(String(1800))
    content_hash: Mapped[str] = mapped_column(String(64), nullable=False)
    content_type: Mapped[str] = mapped_column(String(256), nullable=False, default="application/octet-stream")
    byte_size: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    source_code: Mapped[str | None] = mapped_column(String(64), index=True)
    source_table: Mapped[str | None] = mapped_column(String(128), index=True)
    source_record_id: Mapped[str | None] = mapped_column(String(128), index=True)
    dataset_version: Mapped[str | None] = mapped_column(String(64), index=True)
    metadata_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)


class LakeDataset(Base):
    __tablename__ = "lake_dataset"
    __table_args__ = (UniqueConstraint("dataset_code", name="uq_lake_dataset_code"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    dataset_code: Mapped[str] = mapped_column(String(128), nullable=False)
    dataset_name: Mapped[str] = mapped_column(String(256), nullable=False)
    layer: Mapped[str] = mapped_column(String(16), nullable=False)
    format: Mapped[str] = mapped_column(String(32), nullable=False, default="PARQUET")
    partition_spec: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    description: Mapped[str | None] = mapped_column(Text)
    current_version: Mapped[str | None] = mapped_column(String(64))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now, onupdate=utc_now)


class LakeDatasetVersion(Base):
    __tablename__ = "lake_dataset_version"
    __table_args__ = (UniqueConstraint("dataset_id", "version", name="uq_lake_dataset_version"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    dataset_id: Mapped[int] = mapped_column(Integer, nullable=False, index=True)
    version: Mapped[str] = mapped_column(String(64), nullable=False)
    object_id: Mapped[str | None] = mapped_column(String(36), index=True)
    row_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    schema_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    quality_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    status: Mapped[str] = mapped_column(String(24), nullable=False, default="PUBLISHED")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)


class LakeLineageEvent(Base):
    __tablename__ = "lake_lineage_event"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    batch_id: Mapped[str] = mapped_column(String(64), nullable=False, index=True)
    upstream_type: Mapped[str] = mapped_column(String(64), nullable=False)
    upstream_id: Mapped[str] = mapped_column(String(256), nullable=False)
    downstream_type: Mapped[str] = mapped_column(String(64), nullable=False)
    downstream_id: Mapped[str] = mapped_column(String(256), nullable=False)
    transformation: Mapped[str] = mapped_column(String(128), nullable=False)
    parser_version: Mapped[str | None] = mapped_column(String(64))
    dataset_version: Mapped[str | None] = mapped_column(String(64))
    metadata_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)


class DocumentChunkVersion(Base):
    __tablename__ = "document_chunk_version"
    __table_args__ = (UniqueConstraint("document_key", "content_hash", "chunk_index", "chunk_version", name="uq_document_chunk_version"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid4()))
    document_key: Mapped[str] = mapped_column(String(512), nullable=False, index=True)
    document_id: Mapped[str | None] = mapped_column(String(128), index=True)
    chunk_index: Mapped[int] = mapped_column(Integer, nullable=False)
    chunk_version: Mapped[str] = mapped_column(String(64), nullable=False)
    content_hash: Mapped[str] = mapped_column(String(64), nullable=False)
    chunk_text: Mapped[str] = mapped_column(Text, nullable=False)
    start_offset: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    end_offset: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    parser_version: Mapped[str] = mapped_column(String(64), nullable=False)
    embedding_model: Mapped[str | None] = mapped_column(String(128))
    status: Mapped[str] = mapped_column(String(24), nullable=False, default="READY")
    metadata_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)
