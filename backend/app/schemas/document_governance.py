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


class DocumentPlanGenerateRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    force: bool = False


class DocumentPlanDecisionRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    decision: Literal["CONFIRM", "SKIP"] = "CONFIRM"
    expected_plan_id: str = Field(min_length=1, max_length=64)
    expected_revision: int = Field(ge=1)
    primary_type: str = Field(min_length=1, max_length=64)
    secondary_types: list[str] = Field(default_factory=list, max_length=8)
    skill_codes: list[str] = Field(default_factory=list, max_length=12)
    target_scopes: list[str] = Field(default_factory=list, max_length=8)
    reviewer: str = Field(min_length=1, max_length=128)
    reason: str = Field(default="", max_length=2000)


class KnowledgePointFeedback(BaseModel):
    model_config = ConfigDict(extra="forbid")
    point_type: Literal["EVENT", "RELATION"]
    key: str = Field(min_length=1, max_length=256)
    reason: str = Field(min_length=1, max_length=2000)


class DocumentResultReviewRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    version_index: int = Field(ge=0)
    expected_version_key: str = Field(min_length=1, max_length=512)
    expected_plan_id: str = Field(min_length=1, max_length=64)
    decision: Literal["CONFIRM", "SKIP"] = "CONFIRM"
    reviewer: str = Field(min_length=1, max_length=128)
    reason: str = Field(default="", max_length=2000)
    nonconforming: list[KnowledgePointFeedback] = Field(default_factory=list, max_length=128)
    explicit_confirmations: list[str] = Field(default_factory=list, max_length=128)


class DocumentSkillDraftRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    gap_index: int = Field(ge=0, le=32)
    expected_plan_id: str = Field(min_length=1, max_length=64)
    reviewer: str = Field(min_length=1, max_length=128)
