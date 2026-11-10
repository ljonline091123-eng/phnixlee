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


class AnalystImageInput(BaseModel):
    model_config = ConfigDict(extra="forbid")
    name: str = Field(default="", max_length=160)
    data_url: str = Field(max_length=5_600_000)

    @model_validator(mode="after")
    def valid_image(self):
        from app.services.analyst_inputs import validate_image
        validate_image(self.data_url)
        return self


class AnalystDialogueCreate(BaseModel):
    model_config = ConfigDict(extra="forbid")
    question: str = Field(min_length=2, max_length=6000)
    model_instance_code: str | None = Field(default=None, max_length=64)
    evidence_ids: list[str] = Field(default_factory=list, max_length=8)
    images: list[AnalystImageInput] = Field(default_factory=list, max_length=4)

    @model_validator(mode="after")
    def valid_attachments(self):
        from app.services.analyst_inputs import MAX_TOTAL_IMAGE_BYTES, validate_image
        if sum(len(validate_image(image.data_url)[1]) for image in self.images) > MAX_TOTAL_IMAGE_BYTES:
            raise ValueError("本次图片合计最大12MB，请减少图片后发送")
        self.evidence_ids = list(dict.fromkeys(self.evidence_ids))
        return self


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
    label: str = Field(default="", max_length=80)
    keywords: list[str] = Field(min_length=1, max_length=12)

    @model_validator(mode="after")
    def clean_keywords(self):
        self.keywords = list(dict.fromkeys(word.strip() for word in self.keywords if word.strip()))
        if not self.keywords or any(len(word) > 80 for word in self.keywords):
            raise ValueError("请填写有效关键词，每个关键词不超过80字")
        return self


class AnalystObservationRule(BaseModel):
    model_config = ConfigDict(extra="forbid")
    observation: str = Field(min_length=1, max_length=500)
    event_conditions: list[AnalystEventCondition] = Field(default_factory=list, max_length=8)

    @model_validator(mode="after")
    def clean_observation(self):
        self.observation = self.observation.strip()
        if not self.observation:
            raise ValueError("重点观察不能为空")
        return self


class AnalystWatchConfig(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)
    enabled: bool = True
    price_lower: float | None = Field(default=None, gt=0)
    price_upper: float | None = Field(default=None, gt=0)
    volume_mode: Literal["ANY", "EXPAND", "SHRINK"] = "ANY"
    volume_ratio: float = Field(default=1.5, gt=0, le=100)
    volume_window: int = Field(default=20, ge=5, le=120)
    # Retained for old saved configurations. New writes use observation_rules.
    event_conditions: list[AnalystEventCondition] = Field(default_factory=list, max_length=12)
    observation_rules: list[AnalystObservationRule] = Field(default_factory=list, max_length=12)
    selected_observations: list[str] = Field(default_factory=list, max_length=12)
    review_focus: str = Field(default="", max_length=2500)
    schedule_hour: int = Field(default=18, ge=0, le=23)
    schedule_minute: int = Field(default=0, ge=0, le=59)
    model_instance_code: str | None = Field(default=None, max_length=64)

    @model_validator(mode="after")
    def valid_bounds(self):
        self.selected_observations = list(dict.fromkeys(
            text.strip() for text in self.selected_observations if text.strip()
        ))
        if any(len(text) > 500 for text in self.selected_observations):
            raise ValueError("重点观察每项不超过500字")
        observations = [rule.observation for rule in self.observation_rules]
        if len(observations) != len(set(observations)):
            raise ValueError("每条重点观察只能配置一组触发规则")
        if any(text not in self.selected_observations for text in observations):
            raise ValueError("触发规则必须属于已选择的重点观察")
        if self.price_lower is not None and self.price_upper is not None and self.price_lower >= self.price_upper:
            raise ValueError("关注下限必须小于上限")
        if self.volume_mode == "EXPAND" and self.volume_ratio <= 1:
            raise ValueError("放量倍数必须大于1")
        if self.volume_mode == "SHRINK" and self.volume_ratio >= 1:
            raise ValueError("缩量倍数必须小于1")
        return self


class AnalystObservationCheck(BaseModel):
    model_config = ConfigDict(extra="forbid")
    observation_id: str = Field(pattern=r"^O[1-9][0-9]*$")
    status: Literal["HIT", "NOT_HIT", "UNKNOWN"]
    explanation: str = Field(min_length=5, max_length=700)
    evidence: list[AiAnalystEvidenceReference] = Field(default_factory=list, max_length=5)


class AnalystForecastProposal(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)
    direction: Literal["BULLISH", "BEARISH"]
    target_price: float = Field(gt=0)
    invalidation_price: float | None = Field(default=None, gt=0)
    horizon_sessions: int = Field(ge=1, le=30)
    confidence: float = Field(ge=0, le=1)
    trigger_labels: list[str] = Field(min_length=1, max_length=6)
    thesis: str = Field(min_length=10, max_length=1200)
    pricing_basis: str = Field(min_length=10, max_length=1000)
    logic_chain: list[str] = Field(min_length=2, max_length=6)
    invalidation_conditions: list[str] = Field(min_length=1, max_length=6)
    evidence: list[AiAnalystEvidenceReference] = Field(min_length=1, max_length=8)


class AnalystDailyOutput(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)
    summary: str = Field(min_length=10, max_length=2500)
    judgment: str = Field(min_length=10, max_length=2500)
    verdict: Literal["SUPPORTED", "CHALLENGED", "UNCHANGED", "INCONCLUSIVE"]
    confidence: float = Field(ge=0, le=1)
    changes: list[str] = Field(default_factory=list, max_length=10)
    next_focus: list[str] = Field(default_factory=list, max_length=12)
    evidence: list[AiAnalystEvidenceReference] = Field(default_factory=list, max_length=12)
    observation_checks: list[AnalystObservationCheck] = Field(default_factory=list, max_length=24)
    forecasts: list[AnalystForecastProposal] = Field(default_factory=list, max_length=2)
    forecast_reason: str = Field(default="", max_length=800)
    learning_notes: list[str] = Field(default_factory=list, max_length=6)


class AnalystSuggestionCreate(BaseModel):
    model_config = ConfigDict(extra="forbid")
    model_instance_code: str | None = Field(default=None, max_length=64)
    selected_observations: list[str] = Field(default_factory=list, max_length=12)

    @model_validator(mode="after")
    def clean_observations(self):
        self.selected_observations = list(dict.fromkeys(
            text.strip() for text in self.selected_observations if text.strip()
        ))
        if any(len(text) > 500 for text in self.selected_observations):
            raise ValueError("重点观察每项不超过500字")
        return self


class AnalystSuggestedConditions(BaseModel):
    """The model can suggest investment observations, never enable scheduling."""
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)
    price_lower: float | None = Field(default=None, gt=0)
    price_upper: float | None = Field(default=None, gt=0)
    volume_mode: Literal["ANY", "EXPAND", "SHRINK"]
    volume_ratio: float = Field(gt=0, le=100)
    volume_window: int = Field(ge=5, le=120)
    event_conditions: list[AnalystEventCondition] = Field(default_factory=list, max_length=8)
    observation_rules: list[AnalystObservationRule] = Field(min_length=1, max_length=12)
    review_focus: str = Field(min_length=10, max_length=2500)

    @model_validator(mode="after")
    def valid_conditions(self):
        data = self.model_dump()
        data["selected_observations"] = [rule.observation for rule in self.observation_rules]
        AnalystWatchConfig(**data)
        return self


class AnalystWatchSuggestionOutput(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)
    summary: str = Field(min_length=10, max_length=1200)
    conditions: AnalystSuggestedConditions
    price_reason: str = Field(min_length=10, max_length=1500)
    volume_reason: str = Field(min_length=10, max_length=1000)
    event_reason: str = Field(min_length=10, max_length=1500)
    limitations: list[str] = Field(default_factory=list, max_length=8)
    evidence: list[AiAnalystEvidenceReference] = Field(min_length=1, max_length=12)
