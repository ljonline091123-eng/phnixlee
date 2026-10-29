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
    model_instance_codes: list[str] = Field(default_factory=list, max_length=10)
    strategy_profile_id: int | None = Field(default=None, gt=0)
    knowledge_base_ids: list[int] = Field(default_factory=list, max_length=20)
    graph_ids: list[int] = Field(default_factory=list, max_length=50)
    use_model: bool = True
    data_mode: Literal["REAL", "MOCK"] = "REAL"
    # UPDATE preserves the normal data-first workflow. CONTINUE_PARTIAL is an
    # explicit user decision to analyse the current local snapshot as-is.
    # REUSE_EXISTING is reserved for auditable retries: evidence-ready stocks
    # reuse local data while only genuine evidence gaps are refreshed.
    preflight_data_action: Literal["UPDATE", "CONTINUE_PARTIAL", "REUSE_EXISTING"] = "UPDATE"

    @model_validator(mode="after")
    def explicit_symbols_fit_candidate_limit(self) -> "SelectionRunCreate":
        symbols = [str(symbol).strip() for symbol in self.symbols if str(symbol).strip()]
        if len(symbols) != len(set(symbols)):
            raise ValueError("手选股票中存在重复代码，请先去重")
        if symbols and len(symbols) > self.candidate_limit:
            raise ValueError("手选股票数量超过候选上限，请提高候选数量上限")
        self.model_instance_codes = list(dict.fromkeys(
            str(item).strip() for item in self.model_instance_codes if str(item).strip()
        ))
        if self.model_instance_code and self.model_instance_code not in self.model_instance_codes:
            self.model_instance_codes.insert(0, self.model_instance_code)
        if not self.model_instance_code and self.model_instance_codes:
            self.model_instance_code = self.model_instance_codes[0]
        return self


class SelectionScopePreview(BaseModel):
    market: str = Field(default="CN_A", max_length=16)
    symbols: list[str] = Field(default_factory=list, max_length=500)
    knowledge_base_ids: list[int] = Field(default_factory=list, max_length=20)
    graph_ids: list[int] = Field(default_factory=list, max_length=50)
    period_months: int = Field(default=3, ge=1, le=60)


class SelectionPreflightRequest(SelectionScopePreview):
    """Read-only gate checked immediately before creating an analysis run."""

    model_instance_code: str | None = Field(default=None, max_length=64)
    model_instance_codes: list[str] = Field(default_factory=list, max_length=10)
    use_model: bool = True
    data_mode: Literal["REAL", "MOCK"] = "REAL"

    @model_validator(mode="after")
    def normalize_models(self) -> "SelectionPreflightRequest":
        self.model_instance_codes = list(dict.fromkeys(
            str(item).strip() for item in self.model_instance_codes if str(item).strip()
        ))
        if self.model_instance_code and self.model_instance_code not in self.model_instance_codes:
            self.model_instance_codes.insert(0, self.model_instance_code)
        if not self.model_instance_code and self.model_instance_codes:
            self.model_instance_code = self.model_instance_codes[0]
        return self


class SelectionEvidenceAssessment(BaseModel):
    model_config = ConfigDict(extra="forbid")
    reference_type: Literal["DOCUMENT", "FACT", "RELATION"]
    reference_id: str
    direction: Literal["POSITIVE", "NEGATIVE", "MIXED", "NEUTRAL", "UNKNOWN"]
    score: float | None = Field(default=None, ge=-100, le=100)
    rationale: str = Field(min_length=1, max_length=160)


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
    evidence_assessments: list[SelectionEvidenceAssessment] = Field(default_factory=list, max_length=124)


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


class SelectionBusinessSegment(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)
    name: str = Field(min_length=1, max_length=160)
    report_date: str | None = Field(default=None, max_length=32)
    revenue: float | None = None
    revenue_ratio: float | None = Field(default=None, ge=0, le=1)
    profit: float | None = None
    profit_ratio: float | None = Field(default=None, ge=-1, le=1)
    gross_margin: float | None = Field(default=None, ge=-1, le=1)
    evidence_fact_id: str = Field(min_length=1, max_length=128)


class SelectionMarketShareEvidence(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)
    verification_status: Literal["SOURCE_VERIFIED", "UNVERIFIED", "MISSING"]
    value: float | None = Field(default=None, ge=0)
    unit: str | None = Field(default=None, max_length=32)
    scope: str | None = Field(default=None, max_length=240)
    as_of: str | None = Field(default=None, max_length=32)
    source: str | None = Field(default=None, max_length=240)
    explanation: str = Field(min_length=1, max_length=600)
    evidence_document_ids: list[int] = Field(default_factory=list, max_length=12)
    evidence_fact_ids: list[str] = Field(default_factory=list, max_length=12)


class SelectionBusinessQualityAnalysis(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)
    status: Literal["SUPPORTED", "INSUFFICIENT"]
    main_business_summary: str = Field(min_length=1, max_length=800)
    products_services: list[str] = Field(default_factory=list, max_length=10)
    revenue_mix: list[SelectionBusinessSegment] = Field(default_factory=list, max_length=12)
    technology_level: Literal["UNKNOWN", "LOW", "MEDIUM", "HIGH", "LEADING"] = "UNKNOWN"
    technology_assessment: str = Field(min_length=1, max_length=800)
    competitive_moat: str = Field(min_length=1, max_length=800)
    commercialization: str = Field(min_length=1, max_length=800)
    market_position: str = Field(min_length=1, max_length=800)
    market_share: SelectionMarketShareEvidence
    direction: Literal["POSITIVE", "NEGATIVE", "MIXED", "NEUTRAL", "UNKNOWN"]
    impact_score: float | None = Field(default=None, ge=-100, le=100)
    confidence: float = Field(ge=0, le=1)
    evidence_document_ids: list[int] = Field(default_factory=list, max_length=24)
    evidence_fact_ids: list[str] = Field(default_factory=list, max_length=24)
    risks: list[str] = Field(default_factory=list, max_length=6)
    watch_conditions: list[str] = Field(default_factory=list, max_length=6)


class SelectionValueChainImpact(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)
    related_company: str = Field(min_length=1, max_length=240)
    related_market: str | None = Field(default=None, max_length=16)
    related_symbol: str | None = Field(default=None, max_length=32)
    relationship_type: Literal[
        "SUPPLIER", "CUSTOMER", "PARTNER", "COMPETITOR", "EQUITY_CONTROL",
        "TECHNOLOGY_SUBSTITUTION", "SAME_INDUSTRY", "SAME_THEME", "OTHER",
    ]
    relation_status: Literal["VERIFIED", "CANDIDATE", "UNVERIFIED"]
    relation_fact_ids: list[str] = Field(default_factory=list, max_length=12)
    current_security_document_ids: list[int] = Field(default_factory=list, max_length=12)
    related_security_document_ids: list[int] = Field(default_factory=list, max_length=12)
    event_summary: str = Field(min_length=1, max_length=600)
    transmission_mechanism: str = Field(min_length=1, max_length=800)
    direction: Literal["POSITIVE", "NEGATIVE", "MIXED", "NEUTRAL", "UNKNOWN"]
    impact_score: float | None = Field(default=None, ge=-100, le=100)
    horizon: Literal["SHORT", "MEDIUM", "LONG", "MULTI_PERIOD", "UNKNOWN"]
    confidence: float = Field(ge=0, le=1)
    assumptions: list[str] = Field(default_factory=list, max_length=6)
    invalidation_conditions: list[str] = Field(default_factory=list, max_length=6)


class SelectionValueChainAnalysis(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)
    status: Literal["SUPPORTED", "INSUFFICIENT"]
    summary: str = Field(min_length=1, max_length=1000)
    impacts: list[SelectionValueChainImpact] = Field(default_factory=list, max_length=8)
    risks: list[str] = Field(default_factory=list, max_length=6)
    watch_conditions: list[str] = Field(default_factory=list, max_length=6)


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
    business_quality: SelectionBusinessQualityAnalysis | None = None
    value_chain_analysis: SelectionValueChainAnalysis | None = None
    # Event linkage is an optional semantic enrichment.  A linkage failure
    # must not invalidate independently verified dimensions.
    semantic_linkage_status: Literal["PASSED", "PARTIAL", "UNAVAILABLE"] = "UNAVAILABLE"
    semantic_linkage_warnings: list[str] = Field(default_factory=list, max_length=8)
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


class SelectionEvaluationRequest(BaseModel):
    model_instance_code: str | None = Field(default=None, max_length=64)


class SelectionFollowRequest(BaseModel):
    target_price: float | None = Field(default=None, gt=0)
    stop_price: float | None = Field(default=None, gt=0)
    confidence: float | None = Field(default=None, ge=0, le=1)
    notes: str | None = Field(default=None, max_length=4000)


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
    name: str | None = None
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
    dimensions: list[Literal["NEWS", "NOTICE", "FINANCIAL", "PRICE_VOLUME", "FUND_FLOW", "COMPANY", "RESEARCH", "GRAPH"]] = Field(default_factory=list, max_length=8)
    period_months: int | None = Field(default=None, ge=1, le=60)
    force_refresh: bool = False
    rerun_analysis: bool = False

    @model_validator(mode="after")
    def unique_backfill_scope(self) -> "SelectionBackfillRequest":
        self.candidate_ids = list(dict.fromkeys(self.candidate_ids))
        self.dimensions = list(dict.fromkeys(self.dimensions))
        return self


class SelectionModelSwitchRequest(BaseModel):
    """Explicitly select another model for unresolved candidates.

    A model switch is intentionally separate from the normal retry endpoint so
    the selected provider is auditable and can never be chosen implicitly by a
    route fallback after a billing or quota failure.
    """

    # Reuse the analysis-model multi-select for retries.  The first code is
    # the preferred retry model and the remaining codes are retained in the
    # request snapshot for an auditable, deterministic choice.
    model_instance_code: str | None = Field(default=None, min_length=2, max_length=64)
    model_instance_codes: list[str] = Field(default_factory=list, max_length=10)
    candidate_ids: list[int] = Field(default_factory=list, max_length=500)

    @model_validator(mode="after")
    def unique_candidate_ids(self) -> "SelectionModelSwitchRequest":
        self.model_instance_codes = list(dict.fromkeys(
            str(item).strip() for item in self.model_instance_codes if str(item).strip()
        ))
        if self.model_instance_code and self.model_instance_code not in self.model_instance_codes:
            self.model_instance_codes.insert(0, self.model_instance_code)
        if not self.model_instance_code and self.model_instance_codes:
            self.model_instance_code = self.model_instance_codes[0]
        if not self.model_instance_codes:
            raise ValueError("请先在分析模型中至少勾选一个模型")
        self.candidate_ids = list(dict.fromkeys(self.candidate_ids))
        return self


class SelectionProfileItemInput(BaseModel):
    model_config = ConfigDict(extra="forbid")
    dimension_code: Literal[
        "FUNDAMENTAL_QUALITY", "VALUATION_EXPECTATION", "INDUSTRY_MOMENTUM",
        "PRICE_RISK", "CAPITAL_BEHAVIOR", "EVENT_DISCLOSURE",
        "GOVERNANCE_STABILITY", "SUPPLY_CHAIN_RESILIENCE",
    ]
    weight: float = Field(ge=0, le=100)
    minimum_coverage: float = Field(default=0.4, ge=0, le=1)
    minimum_confidence: float = Field(default=0.4, ge=0, le=1)
    skill_code: str = Field(min_length=2, max_length=128)
    skill_version: str = Field(default="1.0.0", min_length=1, max_length=32)


class SelectionProfileCreate(BaseModel):
    model_config = ConfigDict(extra="forbid")
    profile_code: str = Field(min_length=2, max_length=64, pattern=r"^[A-Z0-9_]+$")
    profile_name: str = Field(min_length=2, max_length=128)
    version: str = Field(min_length=1, max_length=32)
    description: str = Field(default="", max_length=4000)
    horizon_code: str = Field(default="T3_T5_T7", max_length=32)
    market_scope: list[str] = Field(default_factory=lambda: ["CN_A", "HK", "NEEQ", "NEEQ_INNOVATION"], max_length=16)
    items: list[SelectionProfileItemInput] = Field(min_length=8, max_length=8)
    parent_profile_id: int | None = Field(default=None, gt=0)

    @model_validator(mode="after")
    def valid_weights(self) -> "SelectionProfileCreate":
        codes = [item.dimension_code for item in self.items]
        if len(set(codes)) != 8:
            raise ValueError("八个评价维度必须各配置一次")
        if abs(sum(item.weight for item in self.items) - 100) > 0.001:
            raise ValueError("维度权重合计必须为100%")
        self.market_scope = list(dict.fromkeys(value.strip().upper() for value in self.market_scope if value.strip()))
        return self


class SelectionProfileUpdate(BaseModel):
    model_config = ConfigDict(extra="forbid")
    profile_name: str | None = Field(default=None, min_length=2, max_length=128)
    description: str | None = Field(default=None, max_length=4000)
    horizon_code: str | None = Field(default=None, max_length=32)
    market_scope: list[str] | None = Field(default=None, max_length=16)
    items: list[SelectionProfileItemInput] | None = Field(default=None, min_length=8, max_length=8)

    @model_validator(mode="after")
    def valid_optional_weights(self) -> "SelectionProfileUpdate":
        if self.items is not None:
            codes = [item.dimension_code for item in self.items]
            if len(set(codes)) != 8 or abs(sum(item.weight for item in self.items) - 100) > 0.001:
                raise ValueError("八个评价维度必须各配置一次，且权重合计为100%")
        if self.market_scope is not None:
            self.market_scope = list(dict.fromkeys(value.strip().upper() for value in self.market_scope if value.strip()))
        return self


class SelectionProfileCloneRequest(BaseModel):
    version: str = Field(min_length=1, max_length=32)
