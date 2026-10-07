from __future__ import annotations

from datetime import datetime
from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field, model_validator


class DataQualityRuleCreate(BaseModel):
    rule_code: str = Field(min_length=2, max_length=128, pattern=r"^[A-Z0-9_\-\.]+$")
    rule_name: str = Field(min_length=1, max_length=256)
    asset_scope: str = Field(min_length=1, max_length=64)
    target_code: str | None = Field(default=None, max_length=256)
    rule_type: str = Field(min_length=1, max_length=64)
    severity: Literal["INFO", "WARNING", "ERROR", "CRITICAL"] = "ERROR"
    version: str = Field(default="1.0.0", max_length=32)
    definition_json: dict[str, Any] = Field(default_factory=dict)
    threshold_json: dict[str, Any] = Field(default_factory=dict)
    lifecycle_status: Literal["DRAFT", "TESTING", "ENABLED", "DISABLED", "DEPRECATED"] = "DRAFT"
    enabled: bool = False
    description: str | None = None


class DataQualityRuleUpdate(BaseModel):
    rule_name: str | None = Field(default=None, min_length=1, max_length=256)
    asset_scope: str | None = Field(default=None, min_length=1, max_length=64)
    target_code: str | None = Field(default=None, max_length=256)
    rule_type: str | None = Field(default=None, min_length=1, max_length=64)
    severity: Literal["INFO", "WARNING", "ERROR", "CRITICAL"] | None = None
    definition_json: dict[str, Any] | None = None
    threshold_json: dict[str, Any] | None = None
    lifecycle_status: Literal["DRAFT", "TESTING", "ENABLED", "DISABLED", "DEPRECATED"] | None = None
    enabled: bool | None = None
    description: str | None = None


class DataQualityRuleRead(DataQualityRuleCreate):
    model_config = ConfigDict(from_attributes=True)

    id: int
    created_at: datetime
    updated_at: datetime


class DataQualityRunRequest(BaseModel):
    target_type: Literal[
        "SOURCE_TABLE",
        "LAKE_DATASET_VERSION",
        "LAKE_OBJECT",
        "DOCUMENT_CHUNK",
        "MODEL_SKILL",
    ] = "SOURCE_TABLE"
    target_id: str = Field(min_length=1, max_length=256)
    target_version: str | None = Field(default=None, max_length=128)
    layer: Literal["RAW", "NORMALIZED", "SERVING"] = "NORMALIZED"
    limit: int = Field(default=10000, ge=1, le=100000)
    idempotency_key: str | None = Field(default=None, max_length=128)

    @model_validator(mode="after")
    def validate_target_version(self) -> "DataQualityRunRequest":
        if self.target_type == "LAKE_DATASET_VERSION" and not self.target_version:
            raise ValueError("LAKE_DATASET_VERSION requires target_version")
        return self


class DataQualityIssueResolve(BaseModel):
    status: Literal["OPEN", "ACKNOWLEDGED", "RESOLVED", "IGNORED"]
    resolution: str | None = Field(default=None, max_length=4000)


class SkillLifecycleUpdate(BaseModel):
    lifecycle_status: Literal["DRAFT", "TESTING", "ENABLED", "DISABLED", "DEPRECATED"]
    reason: str | None = Field(default=None, max_length=2000)

