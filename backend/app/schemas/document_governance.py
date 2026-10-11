"""Contracts for bounded human review of news/notice semantic candidates."""
from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field


class DocumentReviewRequest(BaseModel):
    version_index: int = Field(default=0, ge=0)
    action: Literal["COMMENT", "CONFIRM", "REJECT", "REQUEST_CORRECTION", "MODIFY", "REANALYZE"]
    target_type: Literal["DOCUMENT", "EVENT", "METRIC", "IMPACT", "PARTICIPANT", "RELATION"] = "DOCUMENT"
    target_key: str | None = Field(default=None, min_length=1, max_length=128)
    event_key: str | None = Field(default=None, min_length=1, max_length=128)
    item_index: int | None = Field(default=None, ge=0, le=32)
    field: str | None = Field(default=None, min_length=1, max_length=64)
    value: Any | None = None
    reason: str = Field(min_length=1, max_length=4000)
    reviewer: str = Field(min_length=1, max_length=128)


class DocumentAnalyzeRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    documents: list[dict[str, Any]] = Field(min_length=1, max_length=10)
    idempotency_key: str = Field(min_length=1, max_length=128)
