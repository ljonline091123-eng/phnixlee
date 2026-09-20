"""Validated contracts for the additive data foundation."""

from datetime import date, datetime, timezone
from typing import Annotated, Any, Literal

from pydantic import BaseModel, ConfigDict, Field, HttpUrl, StringConstraints, field_validator, model_validator

EntityType = Literal["COMPANY", "PERSON", "ORGANIZATION", "PROJECT", "INDUSTRY", "THEME", "CLASSIFICATION", "PRODUCT", "MATERIAL"]
FactType = Literal["HOLDS_EQUITY", "CONTROLS", "SUPPLIES_TO", "PARTNERS_WITH", "GUARANTEES", "LENDS_TO", "COMPETES_WITH", "IN_INDUSTRY", "MEMBER_OF_THEME", "HAS_CLASSIFICATION", "CONTRACT", "LEGAL_CASE"]
FactStatus = Literal["PENDING", "ACCEPTED", "REJECTED"]


class StrictInput(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True, extra="forbid", allow_inf_nan=False)


class EntityCreate(StrictInput):
    name: str = Field(min_length=1, max_length=256)
    entity_type: EntityType
    jurisdiction: str = Field(min_length=1, max_length=64)
    identifier_scheme: str | None = Field(default=None, min_length=1, max_length=64)
    identifier_value: str | None = Field(default=None, min_length=1, max_length=128)
    properties_json: dict[str, Any] = Field(default_factory=dict)

    @model_validator(mode="after")
    def identifier_pair(self):
        if bool(self.identifier_scheme) != bool(self.identifier_value):
            raise ValueError("identifier_scheme and identifier_value must be supplied together")
        return self


class EvidenceCreate(StrictInput):
    entity_id: str | None = Field(default=None, min_length=1, max_length=36)
    source_name: str = Field(min_length=1, max_length=256)
    source_key: str = Field(min_length=1, max_length=512)
    title: str = Field(min_length=1, max_length=512)
    content: Annotated[str, StringConstraints(strip_whitespace=False)] = Field(min_length=1, max_length=2000000)
    url: HttpUrl | None = Field(default=None, max_length=2048)
    published_at: datetime | None = None
    available_at: datetime | None = None

    @field_validator("content", mode="before")
    @classmethod
    def preserve_content(cls, value):
        if isinstance(value, str) and not value.strip():
            raise ValueError("content must not be blank")
        return value

    @field_validator("published_at", "available_at")
    @classmethod
    def known_timestamp(cls, value):
        if value is None:
            return value
        if value.tzinfo is None or value.utcoffset() is None:
            raise ValueError("timestamps must include a timezone")
        if value > datetime.now(timezone.utc):
            raise ValueError("timestamps must not be in the future")
        return value.astimezone(timezone.utc)


class LegacyEvidenceImport(StrictInput):
    document_id: int = Field(gt=0)
    entity_id: str | None = Field(default=None, min_length=1, max_length=36)


class SecurityMappingCreate(StrictInput):
    entity_id: str = Field(min_length=1, max_length=36)
    stock_symbol_id: int = Field(gt=0)
    evidence_id: str = Field(min_length=1, max_length=36)
    share_class: str | None = Field(default=None, min_length=1, max_length=64)


class FactCreate(StrictInput):
    fact_type: FactType
    title: str = Field(min_length=1, max_length=512)
    subject_entity_id: str = Field(min_length=1, max_length=36)
    object_entity_id: str | None = Field(default=None, min_length=1, max_length=36)
    properties_json: dict[str, Any] = Field(default_factory=dict)
    evidence_ids: list[str] = Field(min_length=1, max_length=50)
    valid_from: date | None = None
    valid_to: date | None = None

    @model_validator(mode="after")
    def endpoints_and_dates(self):
        if self.fact_type == "LEGAL_CASE":
            if self.object_entity_id:
                raise ValueError("LEGAL_CASE uses subject participation and has no object entity")
        elif not self.object_entity_id:
            raise ValueError("object_entity_id is required for this relation")
        if self.object_entity_id == self.subject_entity_id:
            raise ValueError("subject and object must be different entities")
        if self.valid_from and self.valid_to and self.valid_to <= self.valid_from:
            raise ValueError("valid_to must be later than valid_from; end date is exclusive")
        if any(not value or len(value) > 36 for value in self.evidence_ids):
            raise ValueError("invalid evidence id")
        return self


class FactReviewCreate(StrictInput):
    decision: Literal["ACCEPTED", "REJECTED"]
    reason: str = Field(min_length=1, max_length=4000)
    reviewer: str = Field(min_length=1, max_length=128)
    expected_status: FactStatus
