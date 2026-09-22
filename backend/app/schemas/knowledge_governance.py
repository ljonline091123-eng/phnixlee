"""Bounded, evidence-aware knowledge governance contracts."""

from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


class KnowledgeGovernanceRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    record_limit_per_source: int = Field(default=50, ge=1, le=500)
    instance_code: str | None = Field(default=None, max_length=64)
    run_key: str | None = Field(default=None, min_length=1, max_length=32, pattern=r"^[A-Za-z0-9_-]+$")


class SourceReview(BaseModel):
    model_config = ConfigDict(extra="forbid")

    source_table: str
    decision: Literal["READY", "PENDING_REVIEW", "INSUFFICIENT_DATA"]
    issues: list[str]
    evidence_refs: list[str]


class KnowledgeSourceReview(BaseModel):
    model_config = ConfigDict(extra="forbid")

    sources: list[SourceReview]
    summary: str
