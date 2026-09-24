from __future__ import annotations

from datetime import datetime
from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field, model_validator


class SelectionRunCreate(BaseModel):
    market: str = Field(default="CN_A", min_length=2, max_length=16)
    universe_scope: str = Field(default="LOCAL_DATA", max_length=64)
    symbols: list[str] = Field(default_factory=list, max_length=500)
    candidate_limit: int = Field(default=30, ge=1, le=50)
    min_abs_change_pct: float = Field(default=2.0, ge=0, le=50)
    min_volume_ratio: float = Field(default=1.2, ge=0, le=100)
    model_instance_code: str | None = Field(default=None, max_length=64)
    knowledge_base_ids: list[int] = Field(default_factory=list, max_length=20)
    graph_ids: list[int] = Field(default_factory=list, max_length=50)
    use_model: bool = True
    data_mode: Literal["REAL", "MOCK"] = "REAL"


class SelectionModelRecommendation(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)
    symbol: str
    decision: Literal["WATCH", "PASS"]
    confidence: float = Field(ge=0, le=1)
    reasoning: str = Field(min_length=1, max_length=2000)
    target_price: float | None = Field(gt=0)
    stop_price: float | None = Field(gt=0)
    horizon_sessions: Literal[10]
    evidence_document_ids: list[int]
    # Optional typed references let the model point to structured facts and
    # document chunks without breaking older model routes that only return
    # document IDs.  The service validates these IDs against the bounded
    # GraphRAG context before persisting the analysis.
    evidence_fact_ids: list[str] = Field(default_factory=list)
    evidence_chunk_ids: list[str] = Field(default_factory=list)


class SelectionModelOutput(BaseModel):
    model_config = ConfigDict(extra="forbid")
    candidates: list[SelectionModelRecommendation] = Field(min_length=1, max_length=50)


class SelectionReviewRequest(BaseModel):
    decision: Literal["APPROVED", "REJECTED"]
    target_price: float | None = Field(default=None, gt=0)
    stop_price: float | None = Field(default=None, gt=0)
    target_return_pct: float | None = Field(default=None, ge=-100, le=1000)
    confidence: float | None = Field(default=None, ge=0, le=1)
    notes: str | None = Field(default=None, max_length=4000)

    @model_validator(mode="after")
    def target_required_for_approval(self) -> "SelectionReviewRequest":
        if self.decision == "APPROVED" and self.target_price is None and self.target_return_pct is None:
            raise ValueError("approved candidate requires target_price or target_return_pct")
        return self


class SelectionSnapshotRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    trade_date: str
    close_price: float
    return_pct: float
    drawdown_pct: float
    sequence: int
    source: str
    data_mode: str


class SelectionCandidateRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    run_id: int
    market: str
    symbol: str
    name: str | None
    entry_price: float | None
    entry_date: str | None
    hard_score: float
    hard_rules_json: dict[str, Any]
    evidence_json: dict[str, Any]
    analysis_json: dict[str, Any]
    decision: str
    review_notes: str | None
    target_price: float | None
    stop_price: float | None
    target_return_pct: float | None
    confidence: float | None
    prediction_id: int | None
    reviewed_at: datetime | None
    created_at: datetime
    tracking_id: int | None = None


class SelectionRunRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    market: str
    universe_scope: str
    candidate_limit: int
    criteria_json: dict[str, Any]
    model_instance_code: str | None
    knowledge_base_ids_json: list[int]
    graph_ids_json: list[int]
    data_mode: str
    analysis_mode: str
    status: str
    candidate_count: int
    reviewed_count: int
    tracking_count: int
    missing_data_json: list[str]
    error_message: str | None
    as_of: datetime
    completed_at: datetime | None
    created_at: datetime
    updated_at: datetime
    candidates: list[SelectionCandidateRead] = Field(default_factory=list)


class SelectionTrackingRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    candidate_id: int
    market: str
    symbol: str
    entry_price: float
    entry_date: str
    confirmation_date: str
    adjust: str
    target_price: float | None
    stop_price: float | None
    target_return_pct: float | None
    confidence: float | None
    required_sessions: int
    observed_sessions: int
    status: str
    latest_date: str | None
    latest_price: float | None
    cumulative_return_pct: float | None
    max_drawdown_pct: float | None
    target_hit: bool | None
    stop_hit: bool | None
    direction_hit: bool | None
    data_source: str
    data_mode: str
    review_json: dict[str, Any]
    started_at: datetime
    completed_at: datetime | None
    snapshots: list[SelectionSnapshotRead] = Field(default_factory=list)


class SelectionRefreshRequest(BaseModel):
    tracking_ids: list[int] = Field(default_factory=list, max_length=500)
