"""Bounded, source-identified imports for the company and security graph."""

from datetime import date, datetime
from typing import Annotated, Any, Literal

from pydantic import Field, HttpUrl, StringConstraints, field_validator, model_validator

from app.schemas.foundation import EntityType, EvidenceCreate, FactType, StrictInput


class SourceEntity(StrictInput):
    name: str = Field(min_length=1, max_length=256)
    jurisdiction: str = Field(min_length=1, max_length=64)
    entity_type: EntityType = "COMPANY"
    identifier_scheme: str | None = Field(default=None, min_length=1, max_length=64)
    identifier_value: str | None = Field(default=None, min_length=1, max_length=128)
    source_issuer_id: str | None = Field(default=None, min_length=1, max_length=128)
    properties_json: dict[str, Any] = Field(default_factory=dict)

    @model_validator(mode="after")
    def identity_required(self):
        if bool(self.identifier_scheme) != bool(self.identifier_value):
            raise ValueError("external identifier scheme and value must be paired")
        if not self.identifier_value and not self.source_issuer_id:
            raise ValueError("a registration identifier or a source-scoped issuer identifier is required; names alone cannot merge entities")
        return self


class SourceSecurity(StrictInput):
    stock_symbol_id: int = Field(gt=0)
    share_class: str | None = Field(default=None, min_length=1, max_length=64)


class SourceEvidence(StrictInput):
    title: str = Field(min_length=1, max_length=512)
    content: Annotated[str, StringConstraints(strip_whitespace=False)] = Field(min_length=1, max_length=2000000)
    published_at: datetime | None = None
    available_at: datetime | None = None

    _timestamps = field_validator("published_at", "available_at")(EvidenceCreate.known_timestamp.__func__)
    _content = field_validator("content", mode="before")(EvidenceCreate.preserve_content.__func__)


class SourceFact(StrictInput):
    source_key: str = Field(min_length=1, max_length=512)
    fact_type: FactType
    title: str = Field(min_length=1, max_length=512)
    direction: Literal["OUTGOING", "INCOMING"] = "OUTGOING"
    object: SourceEntity | None = None
    properties_json: dict[str, Any] = Field(default_factory=dict)
    valid_from: date | None = None
    valid_to: date | None = None

    @model_validator(mode="after")
    def legal_direction(self):
        if self.fact_type == "LEGAL_CASE" and self.direction != "OUTGOING":
            raise ValueError("legal participation uses the disclosed subject, not an incoming relation")
        if self.fact_type == "LEGAL_CASE" and self.object is not None:
            raise ValueError("legal participation has no object entity")
        if self.fact_type != "LEGAL_CASE" and self.object is None:
            raise ValueError("a relationship requires an identified counterparty")
        if self.valid_from and self.valid_to and self.valid_to <= self.valid_from:
            raise ValueError("valid_to is exclusive and must be later than valid_from")
        return self


class ClassificationInput(StrictInput):
    dimension: Literal["SIZE", "STYLE", "QUALITY", "LEGAL_LISTING_CLASS", "LIQUIDITY", "RISK", "THEME", "INDUSTRY"]
    code: str = Field(min_length=1, max_length=128)
    label: str = Field(min_length=1, max_length=256)
    definition_version: str = Field(min_length=1, max_length=128)
    method: str = Field(min_length=1, max_length=256)
    properties_json: dict[str, Any] = Field(default_factory=dict)
    valid_from: date | None = None
    valid_to: date | None = None

    @model_validator(mode="after")
    def valid_interval(self):
        if self.valid_from and self.valid_to and self.valid_to <= self.valid_from:
            raise ValueError("valid_to is exclusive and must be later than valid_from")
        return self


class SecurityClassificationCreate(ClassificationInput):
    security_id: str = Field(min_length=1, max_length=36)
    evidence_id: str = Field(min_length=1, max_length=36)


class SourceRecord(StrictInput):
    source_name: str = Field(min_length=1, max_length=256)
    source_key: str = Field(min_length=1, max_length=512)
    source_url: HttpUrl | None = None
    source_kind: Literal["OFFICIAL", "AGGREGATOR", "AUTHORIZED", "MANUAL"] = "AGGREGATOR"
    observed_at: datetime
    company: SourceEntity
    security: SourceSecurity
    evidence: SourceEvidence
    facts: list[SourceFact] = Field(default_factory=list, max_length=100)
    classifications: list[ClassificationInput] = Field(default_factory=list, max_length=100)

    _observed_at = field_validator("observed_at")(EvidenceCreate.known_timestamp.__func__)

    @model_validator(mode="after")
    def company_endpoint(self):
        if self.company.entity_type != "COMPANY":
            raise ValueError("a security issuer must be a company")
        return self


class SourceBatch(StrictInput):
    records: list[SourceRecord] = Field(min_length=1, max_length=100)
