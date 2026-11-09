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


class AnalystDialogueCreate(BaseModel):
    model_config = ConfigDict(extra="forbid")
    question: str = Field(min_length=2, max_length=6000)
    model_instance_code: str | None = Field(default=None, max_length=64)


class AnalystDialogueOutput(BaseModel):
    model_config = ConfigDict(extra="forbid")
    reply: str = Field(min_length=10, max_length=6000)
    suggested_judgment: str = Field(min_length=10, max_length=2500)
    focus_points: list[str] = Field(min_length=1, max_length=12)
    evidence: list[AiAnalystEvidenceReference] = Field(default_factory=list, max_length=12)


class AnalystReportConfirm(BaseModel):
    model_config = ConfigDict(extra="forbid")
    dialogue_id: int = Field(gt=0)
    expected_revision_id: int | None = None
    judgment: str = Field(min_length=10, max_length=2500)
    focus_points: list[str] = Field(min_length=1, max_length=12)


class AnalystEventCondition(BaseModel):
    model_config = ConfigDict(extra="forbid")
    category: Literal["NEWS", "NOTICE", "ANY"] = "ANY"
    keywords: list[str] = Field(min_length=1, max_length=12)

    @model_validator(mode="after")
    def clean_keywords(self):
        self.keywords = list(dict.fromkeys(word.strip() for word in self.keywords if word.strip()))
        if not self.keywords or any(len(word) > 80 for word in self.keywords):
            raise ValueError("请填写有效关键词，每个关键词不超过80字")
        return self


class AnalystWatchConfig(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)
    enabled: bool = True
    price_lower: float | None = Field(default=None, gt=0)
    price_upper: float | None = Field(default=None, gt=0)
    volume_mode: Literal["ANY", "EXPAND", "SHRINK"] = "ANY"
    volume_ratio: float = Field(default=1.5, gt=0, le=100)
    volume_window: int = Field(default=20, ge=5, le=120)
    event_conditions: list[AnalystEventCondition] = Field(default_factory=list, max_length=12)
    review_focus: str = Field(default="", max_length=2500)
    schedule_hour: int = Field(default=18, ge=0, le=23)
    schedule_minute: int = Field(default=0, ge=0, le=59)
    model_instance_code: str | None = Field(default=None, max_length=64)

    @model_validator(mode="after")
    def valid_bounds(self):
        if self.price_lower is not None and self.price_upper is not None and self.price_lower >= self.price_upper:
            raise ValueError("关注下限必须小于上限")
        if self.volume_mode == "EXPAND" and self.volume_ratio <= 1:
            raise ValueError("放量倍数必须大于1")
        if self.volume_mode == "SHRINK" and self.volume_ratio >= 1:
            raise ValueError("缩量倍数必须小于1")
        return self


class AnalystDailyOutput(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)
    summary: str = Field(min_length=10, max_length=2500)
    judgment: str = Field(min_length=10, max_length=2500)
    verdict: Literal["SUPPORTED", "CHALLENGED", "UNCHANGED", "INCONCLUSIVE"]
    confidence: float = Field(ge=0, le=1)
    changes: list[str] = Field(default_factory=list, max_length=10)
    next_focus: list[str] = Field(default_factory=list, max_length=12)
    evidence: list[AiAnalystEvidenceReference] = Field(default_factory=list, max_length=12)
