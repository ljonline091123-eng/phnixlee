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
    # Natural calendar months are used for the evidence window.  The run's
    # ``as_of`` remains the availability cutoff, so historical runs stay
    # reproducible even when they are opened later.
    period_months: int = Field(default=3, ge=1, le=60)
    model_instance_code: str | None = Field(default=None, max_length=64)
    knowledge_base_ids: list[int] = Field(default_factory=list, max_length=20)
    graph_ids: list[int] = Field(default_factory=list, max_length=50)
    use_model: bool = True
    data_mode: Literal["REAL", "MOCK"] = "REAL"

    @model_validator(mode="after")
    def explicit_symbols_fit_candidate_limit(self) -> "SelectionRunCreate":
        symbols = [str(symbol).strip() for symbol in self.symbols if str(symbol).strip()]
        if len(symbols) != len(set(symbols)):
            raise ValueError("手选股票中存在重复代码，请先去重")
        if symbols and len(symbols) > self.candidate_limit:
            raise ValueError("手选股票数量超过候选上限，请提高候选数量上限")
        return self


class SelectionScopePreview(BaseModel):
    market: str = Field(default="CN_A", max_length=16)
    symbols: list[str] = Field(default_factory=list, max_length=500)
    knowledge_base_ids: list[int] = Field(default_factory=list, max_length=20)
    graph_ids: list[int] = Field(default_factory=list, max_length=50)
    period_months: int = Field(default=3, ge=1, le=60)


class SelectionDimension(BaseModel):
    model_config = ConfigDict(extra="forbid")
    dimension: Literal["NEWS", "NOTICE", "FINANCIAL", "PRICE_VOLUME", "FUND_FLOW", "COMPANY", "GRAPH"]
    status: Literal["SUPPORTED", "INSUFFICIENT"]
    direction: Literal["POSITIVE", "NEGATIVE", "MIXED", "NEUTRAL", "UNKNOWN"]
    summary: str = Field(min_length=1, max_length=400)
    # A single NEWS or NOTICE dimension can legitimately cover every document
    # loaded for the period analysis. Keep this aligned with the bounded
    # 80-document context and the 100-item analyzed-document contract.
    evidence_document_ids: list[int] = Field(default_factory=list, max_length=100)
    evidence_fact_ids: list[str] = Field(default_factory=list, max_length=12)
    evidence_relation_ids: list[int] = Field(default_factory=list, max_length=12)


class SelectionEvent(BaseModel):
    model_config = ConfigDict(extra="forbid")
    title: str = Field(min_length=1, max_length=120)
    source_type: Literal["NEWS", "NOTICE"]
    document_id: int
    quote: str = Field(min_length=8, max_length=400)
    direction: Literal["POSITIVE", "NEGATIVE", "MIXED", "NEUTRAL", "UNKNOWN"]
    mechanism: str = Field(min_length=1, max_length=400)
    timing: str = Field(min_length=1, max_length=160)
    confidence: float | None = Field(default=None, ge=0, le=1)
    validation_status: Literal["UNVERIFIED", "PARTIALLY_REALIZED", "REALIZED", "NOT_REALIZED", "CONFLICTED", "INSUFFICIENT"] = "UNVERIFIED"


class SelectionEventLink(BaseModel):
    model_config = ConfigDict(extra="forbid")
    relation_type: Literal[
        "FOLLOW_UP", "VALIDATES", "CONTRADICTS", "REALIZES", "FAILS_TO_REALIZE",
        "SAME_TOPIC", "EXECUTIVE_UNLOCKING_RISK", "SHAREHOLDER_CONFIDENCE_SIGNAL",
        "RELATED_SECURITY_IMPACT",
    ]
    source_document_ids: list[int] = Field(default_factory=list, min_length=1, max_length=12)
    target_document_ids: list[int] = Field(default_factory=list, min_length=1, max_length=12)
    relation_scope: Literal["CURRENT_SECURITY", "RELATED_SECURITY"] = "CURRENT_SECURITY"
    evidence_fact_ids: list[str] = Field(default_factory=list, max_length=12)
    evidence_relation_ids: list[int] = Field(default_factory=list, max_length=12)
    summary: str = Field(min_length=1, max_length=800)
    evidence_quotes: list[str] = Field(default_factory=list, max_length=8)
    direction: Literal["POSITIVE", "NEGATIVE", "MIXED", "NEUTRAL", "UNKNOWN"]
    confidence: float = Field(ge=0, le=1)


class SelectionTimelineEvent(BaseModel):
    model_config = ConfigDict(extra="forbid")
    event_id: str = Field(min_length=1, max_length=80)
    event_date: str = Field(min_length=4, max_length=32)
    source_type: Literal["NEWS", "NOTICE"]
    document_ids: list[int] = Field(default_factory=list, min_length=1, max_length=12)
    topic: str = Field(min_length=1, max_length=120)
    claim: str = Field(min_length=1, max_length=600)
    direction: Literal["POSITIVE", "NEGATIVE", "MIXED", "NEUTRAL", "UNKNOWN"]
    confidence: float = Field(ge=0, le=1)


class SelectionValidationFinding(BaseModel):
    model_config = ConfigDict(extra="forbid")
    claim: str = Field(min_length=1, max_length=400)
    validation_status: Literal["UNVERIFIED", "PARTIALLY_REALIZED", "REALIZED", "NOT_REALIZED", "CONFLICTED", "INSUFFICIENT"]
    validation_window: str = Field(min_length=1, max_length=120)
    supporting_fact_ids: list[str] = Field(default_factory=list, max_length=12)
    supporting_document_ids: list[int] = Field(default_factory=list, max_length=12)
    confidence: float = Field(ge=0, le=1)
    explanation: str = Field(min_length=1, max_length=800)


class SelectionConfidenceBreakdown(BaseModel):
    model_config = ConfigDict(extra="forbid")
    source_reliability: float = Field(ge=0, le=1)
    temporal_consistency: float = Field(ge=0, le=1)
    cross_document_support: float = Field(ge=0, le=1)
    structured_fact_support: float = Field(ge=0, le=1)
    model_extraction_confidence: float = Field(ge=0, le=1)
    conflict_penalty: float = Field(ge=0, le=1)
    final_confidence: float = Field(ge=0, le=1)


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
    headline: str | None = Field(default=None, max_length=160)
    dimensions: list[SelectionDimension] = Field(default_factory=list, max_length=7)
    events: list[SelectionEvent] = Field(default_factory=list, max_length=6)
    # These are decision-oriented summaries, not one row per source document.
    # Full reading coverage is recorded separately in analyzed_document_ids.
    event_timeline: list[SelectionTimelineEvent] = Field(default_factory=list, max_length=4)
    event_links: list[SelectionEventLink] = Field(default_factory=list, max_length=4)
    validation_findings: list[SelectionValidationFinding] = Field(default_factory=list, max_length=4)
    analyzed_document_ids: list[int] = Field(default_factory=list, max_length=100)
    confidence_breakdown: SelectionConfidenceBreakdown | None = None
    positives: list[str] = Field(default_factory=list, max_length=4)
    risks: list[str] = Field(default_factory=list, max_length=4)
    watch_conditions: list[str] = Field(default_factory=list, max_length=4)


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


class SelectionBackfillRequest(BaseModel):
    candidate_ids: list[int] = Field(default_factory=list, max_length=500)
    dimensions: list[Literal["NEWS", "NOTICE", "FINANCIAL", "PRICE_VOLUME", "FUND_FLOW", "COMPANY", "GRAPH"]] = Field(default_factory=list, max_length=7)
    period_months: int | None = Field(default=None, ge=1, le=60)
    force_refresh: bool = False
    rerun_analysis: bool = False

    @model_validator(mode="after")
    def unique_backfill_scope(self) -> "SelectionBackfillRequest":
        self.candidate_ids = list(dict.fromkeys(self.candidate_ids))
        self.dimensions = list(dict.fromkeys(self.dimensions))
        return self
