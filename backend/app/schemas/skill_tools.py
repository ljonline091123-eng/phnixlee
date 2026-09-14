from __future__ import annotations

from datetime import datetime
from typing import Any, Literal

from pydantic import BaseModel, Field


class DwValidationRequest(BaseModel):
    records: list[dict[str, Any]] = Field(min_length=1, max_length=5000)


class WatchMetric(BaseModel):
    market: str
    stock_code: str
    price: float
    previous_close: float
    volume: float
    avg_volume_20: float
    as_of: datetime


class WatchFilterRequest(BaseModel):
    metrics: list[WatchMetric] = Field(min_length=1, max_length=5000)
    min_abs_change_pct: float = Field(default=3.0, ge=0)
    min_volume_ratio: float = Field(default=2.0, ge=0)
    max_candidates: int = Field(default=50, ge=1, le=50)


class PredictionOutcomeInput(BaseModel):
    ledger_id: int = Field(gt=0)
    action_type: Literal["BUY", "SELL", "HOLD"]
    entry_price: float = Field(gt=0)
    actual_price: float | None = Field(default=None, gt=0)
    target_return_pct: float | None = None
    due_at: datetime
    price_as_of: datetime | None = None
    source: str | None = None


class PredictionScoreRequest(BaseModel):
    outcomes: list[PredictionOutcomeInput] = Field(min_length=1, max_length=50)
