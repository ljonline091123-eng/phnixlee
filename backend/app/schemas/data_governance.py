from __future__ import annotations

from datetime import datetime
from enum import Enum

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator


class GovernanceStatus(str, Enum):
    COMPLETED = "COMPLETED"
    PENDING_REVIEW = "PENDING_REVIEW"


class IssueSeverity(str, Enum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"


class StrictGovernanceModel(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)


class QualityIssue(StrictGovernanceModel):
    record_id: str = Field(min_length=1)
    field_name: str = Field(min_length=1)
    issue_type: str = Field(min_length=1)
    severity: IssueSeverity
    evidence_text: str = Field(min_length=1)


class TargetTable(StrictGovernanceModel):
    category: str = Field(min_length=1)
    table_name: str = Field(min_length=1)
    business_key: list[str] = Field(min_length=1)
    evidence_text: str = Field(min_length=1)


class PendingReview(StrictGovernanceModel):
    record_id: str = Field(min_length=1)
    reason: str = Field(min_length=1)
    evidence_text: str = Field(min_length=1)


class DataCleaningOutput(StrictGovernanceModel):
    as_of: datetime
    status: GovernanceStatus
    category_counts: dict[str, int]
    quality_issues: list[QualityIssue]
    dedupe_keys: list[str] = Field(min_length=1)
    target_tables: list[TargetTable]
    pending_review: list[PendingReview]
    missing_data: list[str]
    confidence: float = Field(ge=0.0, le=1.0)
    evidence_text: str = Field(min_length=1)

    @field_validator("as_of")
    @classmethod
    def require_timezone(cls, value: datetime) -> datetime:
        if value.tzinfo is None or value.utcoffset() is None:
            raise ValueError("as_of must include a timezone")
        return value

    @field_validator("category_counts")
    @classmethod
    def validate_category_counts(cls, value: dict[str, int]) -> dict[str, int]:
        if any(not key.strip() for key in value):
            raise ValueError("category_counts keys cannot be empty")
        if any(count < 0 for count in value.values()):
            raise ValueError("category_counts values cannot be negative")
        return value

    @field_validator("dedupe_keys")
    @classmethod
    def validate_dedupe_keys(cls, value: list[str]) -> list[str]:
        if any(not item.strip() for item in value):
            raise ValueError("dedupe_keys cannot contain empty values")
        if len(set(value)) != len(value):
            raise ValueError("dedupe_keys cannot contain duplicates")
        return value

    @model_validator(mode="after")
    def enforce_status_consistency(self) -> DataCleaningOutput:
        has_unresolved_work = bool(self.quality_issues or self.pending_review or self.missing_data)
        if self.status == GovernanceStatus.COMPLETED and has_unresolved_work:
            raise ValueError("COMPLETED output cannot contain unresolved governance work")
        if has_unresolved_work and self.status != GovernanceStatus.PENDING_REVIEW:
            raise ValueError("unresolved governance work requires PENDING_REVIEW status")
        return self
