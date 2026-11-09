from __future__ import annotations

from datetime import datetime
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, model_validator


class AiStockAnalysisCreate(BaseModel):
    model_config = ConfigDict(extra="forbid")
    market: Literal["CN_A", "HK", "NEEQ", "NEEQ_INNOVATION"]
    symbol: str = Field(min_length=1, max_length=32)
    period_months: int = Field(default=12, ge=1, le=60)
    model_instance_code: str | None = Field(default=None, max_length=64)


class AiAnalystEvidenceReference(BaseModel):
    model_config = ConfigDict(extra="forbid")
    evidence_id: str = Field(min_length=3, max_length=160)
    claim: str = Field(min_length=2, max_length=600)


class AiAnalystScenario(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)
    horizon: Literal["SHORT", "MEDIUM", "LONG"]
    horizon_label: str = Field(min_length=2, max_length=80)
    direction: Literal["BULLISH", "BEARISH", "SIDEWAYS", "UNCERTAIN"]
    confidence: float = Field(ge=0, le=1)
    thesis: str = Field(min_length=2, max_length=1200)
    conditions: list[str] = Field(default_factory=list, max_length=8)
    invalidation_conditions: list[str] = Field(default_factory=list, max_length=8)
    evidence: list[AiAnalystEvidenceReference] = Field(default_factory=list, max_length=10)


class AiStockAnalystOutput(BaseModel):
    """Strict model contract for the standalone analyst page."""

    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)
    analysis_mode: Literal["SENIOR_AI_ANALYST_V1"] = "SENIOR_AI_ANALYST_V1"
    executive_summary: str = Field(min_length=10, max_length=1800)
    current_assessment: str = Field(min_length=10, max_length=1800)
    overall_direction: Literal["BULLISH", "BEARISH", "SIDEWAYS", "UNCERTAIN"]
    overall_confidence: float = Field(ge=0, le=1)
    scenarios: list[AiAnalystScenario] = Field(min_length=3, max_length=3)
    core_drivers: list[str] = Field(default_factory=list, max_length=10)
    catalysts: list[str] = Field(default_factory=list, max_length=10)
    key_risks: list[str] = Field(default_factory=list, max_length=10)
    evidence_chain: list[AiAnalystEvidenceReference] = Field(default_factory=list, max_length=16)
    watch_items: list[str] = Field(default_factory=list, max_length=12)
    data_limitations: list[str] = Field(default_factory=list, max_length=12)
    disclaimer: str = Field(min_length=8, max_length=300)

    @model_validator(mode="after")
    def exactly_one_horizon(self) -> "AiStockAnalystOutput":
        horizons = [row.horizon for row in self.scenarios]
        if set(horizons) != {"SHORT", "MEDIUM", "LONG"}:
            raise ValueError("短期、中期、长期情景必须各返回一项")
        return self


class AiStockContextPreview(BaseModel):
    market: str
    symbol: str
    name: str
    period_months: int
    as_of: str
    counts: dict[str, int] = Field(default_factory=dict)
    total_counts: dict[str, int] = Field(default_factory=dict)
    latest_dates: dict[str, str | None] = Field(default_factory=dict)
    available_categories: list[str] = Field(default_factory=list)
    missing_categories: list[str] = Field(default_factory=list)
    warnings: list[str] = Field(default_factory=list)
    evidence_count: int = 0


class AiStockAnalysisRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    market: str
    symbol: str
    name: str
    period_months: int
    status: str
    model_instance_code: str | None = None
    model_call_log_id: int | None = None
    skill_code: str
    skill_version: str
    coverage_json: dict = Field(default_factory=dict)
    result_json: dict = Field(default_factory=dict)
    error_message: str | None = None
    progress_json: dict = Field(default_factory=dict)
    created_at: datetime
    completed_at: datetime | None = None
