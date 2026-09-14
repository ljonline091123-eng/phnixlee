from __future__ import annotations

from datetime import datetime
from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field


class PredictionCreate(BaseModel):
    market: str = Field(min_length=2, max_length=16)
    stock_code: str = Field(min_length=1, max_length=32)
    action_type: Literal["BUY", "SELL", "HOLD"]
    target_timeframe: str = Field(pattern=r"^T\+([1-9]|[12][0-9]|30)$")
    reasoning_logic: str = Field(min_length=1, max_length=20000)
    skill_code: str = Field(min_length=2, max_length=64)
    model_instance_code: str | None = None
    research_report_id: int | None = None
    entry_price: float = Field(gt=0)
    target_return_pct: float | None = None


class PredictionRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    market: str
    stock_code: str
    action_type: str
    predicted_at: datetime
    target_timeframe: str
    reasoning_logic: str
    skill_code: str
    skill_version_used: str
    model_instance_code: str | None
    research_report_id: int | None
    entry_price: float
    target_return_pct: float | None
    status: str
    actual_price: float | None
    actual_return_pct: float | None
    direction_hit: bool | None
    price_as_of: datetime | None
    evaluation_json: dict[str, Any]
    evaluated_at: datetime | None
    created_at: datetime
    updated_at: datetime


class SkillDraftRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    skill_id: int
    base_skill_version: str
    prediction_ids: list[int]
    proposed_instructions: str
    rationale: str
    status: str
    model_call_log_id: int | None
    created_at: datetime
    reviewed_at: datetime | None
