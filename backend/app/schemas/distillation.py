"""Contracts for on-demand source loading and two-track LLM extraction."""

from __future__ import annotations

from datetime import date as Date, datetime
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, model_validator


SourceTable = Literal["stock_news", "stock_notice", "stock_financial_report"]
TrackStatus = Literal["PENDING", "DONE", "FAILED"]
FetchStatus = Literal["PENDING", "FETCHING", "DONE", "FAILED"]
EntityKind = Literal["Company", "Industry", "Event"]
RelationKind = Literal["BELONGS_TO", "SUPPLIES_TO", "IMPACTED_BY"]


class StrictModel(BaseModel):
    model_config = ConfigDict(extra="forbid")


class ExtractionRequest(StrictModel):
    stock_code: str = Field(min_length=1, max_length=32)
    doc_id: int = Field(gt=0, description="knowledge_document.id")


class BusinessDataDistillationState(StrictModel):
    """New control fields shared by news, notices, and financial reports."""

    fetch_status: FetchStatus = "PENDING"
    content_raw: str | None = None
    is_distilled: bool = False


class ExtractionSource(StrictModel):
    knowledge_document_id: int
    source_table: SourceTable
    source_record_id: int
    stock_code: str
    title: str
    content_raw: str = Field(min_length=1)
    content_sha256: str = Field(pattern=r"^[0-9a-f]{64}$")
    source_url: str | None = None
    fetched_at: datetime | None = None


class ChunkMetadata(StrictModel):
    knowledge_document_id: int = Field(gt=0)
    source_table: SourceTable
    source_record_id: int = Field(gt=0)
    stock_code: str = Field(min_length=1)
    question: str = Field(min_length=1, max_length=500)
    answer: str = Field(min_length=1, max_length=4000)
    source_url: str | None = None


class KbChunk(StrictModel):
    chunk_text: str = Field(min_length=1, max_length=6000)
    metadata: ChunkMetadata


class EntityRef(StrictModel):
    kind: EntityKind
    key: str = Field(min_length=1, max_length=128)
    name: str = Field(min_length=1, max_length=256)
    date: Date | None = None

    @model_validator(mode="after")
    def validate_identity(self) -> "EntityRef":
        if self.kind == "Event" and self.date is None:
            raise ValueError("Event requires an ISO date")
        if self.kind != "Event" and self.date is not None:
            raise ValueError("Only Event may have a date")
        if self.kind == "Industry" and self.key != self.name:
            raise ValueError("Industry key must equal its canonical name")
        if self.kind == "Event" and self.key != f"{self.name}|{self.date.isoformat()}":
            raise ValueError("Event key must be '<title>|<ISO date>'")
        return self


class KgTriplet(StrictModel):
    head: EntityRef
    relation: RelationKind
    tail: EntityRef
    confidence: float = Field(ge=0, le=1)
    evidence_text: str = Field(min_length=1, max_length=1000)

    @model_validator(mode="after")
    def validate_endpoints(self) -> "KgTriplet":
        endpoints = {
            "BELONGS_TO": ("Company", "Industry"),
            "SUPPLIES_TO": ("Company", "Company"),
            "IMPACTED_BY": ("Company", "Event"),
        }
        if (self.head.kind, self.tail.kind) != endpoints[self.relation]:
            raise ValueError(f"Invalid endpoints for {self.relation}")
        return self


class DistillationOutput(StrictModel):
    kb_chunks: list[KbChunk] = Field(max_length=100)
    kg_triplets: list[KgTriplet] = Field(max_length=100)


class DistillationTaskCreate(StrictModel):
    source_table: SourceTable
    source_doc_id: int = Field(gt=0)
    knowledge_document_id: int | None = None
    knowledge_base_id: int = Field(gt=0)
    graph_id: int = Field(gt=0)
    content_sha256: str = Field(pattern=r"^[0-9a-f]{64}$")
    governance_run_id: int | None = None


class DistillationTaskRead(DistillationTaskCreate):
    task_id: UUID
    kb_status: TrackStatus = "PENDING"
    kg_status: TrackStatus = "PENDING"
    kb_attempts: int = Field(default=0, ge=0)
    kg_attempts: int = Field(default=0, ge=0)
    error_log: dict[str, str] = Field(default_factory=dict)
    next_retry_at: datetime | None = None
    lease_until: datetime | None = None
    created_at: datetime
    updated_at: datetime
