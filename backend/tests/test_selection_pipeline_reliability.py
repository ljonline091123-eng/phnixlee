"""Regression checks for the live selection gates, retries and historical views."""
from copy import deepcopy
from datetime import datetime, timedelta, timezone
import json
from types import SimpleNamespace
from unittest.mock import patch
from tests.selection_samples import valid_scorecard

import pytest
from sqlalchemy import create_engine, select
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.db.base import Base
from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, KnowledgeGraph, ModelCallLog, ModelSkill
from app.models.market_data import DataSource, StockNews, StockSymbol
from app.models.selection import SelectionCandidate, SelectionRun
from app.schemas.selection import SelectionRunCreate
from app.services.selection import (
    _apply_model_analysis, _candidate_needs_data_refresh, _compact_reduce_batch_context,
    _evidence_data_gate, _model_candidate_context, _prepare_selection_data, _split_model_candidate_context,
    _preflight_selection_models, _selection_result_counts, create_selection_run, execute_failed_selection_reanalysis,
)


BODY = "公司披露新增订单及项目交付安排，收入确认与利润影响仍以最终履约进度为准。"


def evidence():
    return {"documents": [{"id": 123, "title": "项目进展", "source_table": "stock_news",
                            "content_state": "TEXT_AVAILABLE", "content": BODY}],
            "document_ids": [123], "facts": [], "chunks": [], "graph_paths": [],
            "evidence_fact_ids": [], "missing_data": [], "evidence_text": BODY}


@pytest.fixture
def isolated():
    engine = create_engine("sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool)
    Base.metadata.create_all(engine)
    factory = sessionmaker(bind=engine, expire_on_commit=False)
    with patch("app.db.session.SessionLocal", factory):
        with factory() as db:
            yield db
    engine.dispose()


@pytest.mark.parametrize("payload", [
    {},
    {"facts": [{"id": 1, "fact_type": "REALTIME_QUOTE", "properties": {"price": 120}}]},
    {"documents": [{"id": 123, "title": BODY, "content": BODY}]},
    {"documents": [{"id": 123, "content_state": "METADATA_ONLY", "content": BODY}]},
    {"documents": [{"content": BODY}]},
    {"facts": [{"fact_type": "FINANCIAL_OBSERVATION", "properties": {"value": float("nan")}}]},
    {"facts": [{"fact_type": "FINANCIAL_OBSERVATION", "properties": {"source_id": 3, "year": 2026}}]},
])
def test_empty_or_identity_only_context_does_not_enter_analysis(payload):
    assert _evidence_data_gate(payload)["status"] == "MISSING_DATA"


@pytest.mark.parametrize("payload", [
    evidence(),
    {"facts": [{"id": 34, "fact_type": "FINANCIAL_OBSERVATION", "properties": {"net_profit": 0}}]},
    {"structured_observations": {"market": [{"id": 1, "properties": {"close": 120.5, "volume": 800}}]}},
])
def test_real_business_evidence_is_usable_without_semantic_vectors(payload):
    payload["chunks"] = [{"id": "pending-vector", "embedding_status": "PENDING"}]
    assert _evidence_data_gate(payload)["status"] == "READY"


def test_research_forecast_evidence_is_usable_for_model_gate():
    payload = {
        "structured_observations": {
            "research": [{
                "id": "stock_earnings_consensus:1",
                "fact_type": "EARNINGS_FORECAST",
                "properties": {"metric_code": "NET_PROFIT", "mean_value": "12.5"},
            }],
        },
    }
    assert _evidence_data_gate(payload)["status"] == "READY"


def test_failed_model_retry_refreshes_only_when_evidence_is_missing():
    ready = SelectionCandidate(evidence_json=evidence(), analysis_json={"model_output_status": "FAILED"})
    missing = SelectionCandidate(evidence_json={}, analysis_json={"model_output_status": "FAILED"})
    context_failure = SelectionCandidate(
        evidence_json=evidence(),
        analysis_json={"model_output_status": "FAILED", "model_failure_reason": "上下文超过安全上限"},
    )
    assert _candidate_needs_data_refresh(ready) is False
    assert _candidate_needs_data_refresh(context_failure) is False
    assert _candidate_needs_data_refresh(missing) is True


def test_oversized_candidate_context_splits_on_evidence_record_boundaries():
    item = {
        "market": "CN_A", "symbol": "688627", "name": "精智达",
        "evidence_json": {
            **evidence(),
            "documents": [
                {"id": index, "title": f"新闻 {index}", "source_table": "stock_news",
                 "published_at": "2026-10-01", "content_state": "TEXT_AVAILABLE", "content": BODY * 20}
                for index in range(1, 81)
            ],
            "document_ids": list(range(1, 81)),
            "facts": [
                {"id": f"fact:{index}", "fact_type": "FINANCIAL_OBSERVATION",
                 "properties": {"value": index, "detail": BODY * 20}}
                for index in range(12)
            ],
        },
        "analysis_json": {},
    }
    compact = _model_candidate_context(item)
    assert len(json.dumps(compact, ensure_ascii=False, separators=(",", ":"))) > 14_000
    segments = _split_model_candidate_context(compact, max_chars=14_000)
    assert len(segments) > 1
    assert all(len(json.dumps(segment, ensure_ascii=False, separators=(",", ":"))) <= 14_000 for segment in segments)
    document_ids = [row["id"] for segment in segments for row in segment["evidence_json"]["documents"]]
    fact_ids = [row["id"] for segment in segments for row in segment["evidence_json"]["facts"]]
    assert document_ids == list(range(1, 81))
    # The candidate context balances fact types before segmentation; every
    # retained fact must survive exactly once across the resulting segments.
    assert fact_ids == compact["evidence_json"]["evidence_fact_ids"]


def test_reduce_context_compaction_keeps_segment_coverage_and_fits_budget():
    item = {
        "symbol": "688627", "name": "精智达", "hard_rules_json": {"large": BODY * 100},
        "evidence_json": {
            "identity": {"large": BODY * 100},
            "documents": [
                {"id": index, "source_table": "stock_news", "published_at": "2026-10-01", "title": BODY * 20}
                for index in range(1, 30)
            ],
            "citation_allowlist": {"document_ids": list(range(1, 30)), "fact_ids": [f"fact:{i}" for i in range(30)]},
            "evidence_segment_digests": [{
                "segment": {"index": index, "total": 10},
                "analyzed_document_ids": [index],
                "findings": [{
                    "dimension": "NEWS", "direction": "POSITIVE", "summary": BODY * 20,
                    "evidence_document_ids": [index], "evidence_fact_ids": [f"fact:{index}"],
                    "evidence_relation_ids": [],
                } for _ in range(12)],
                "conflicts": [BODY * 20], "limitations": [BODY * 20],
            } for index in range(1, 11)],
        },
    }
    compacted = _compact_reduce_batch_context([item], 6_000)
    rendered = json.dumps(compacted, ensure_ascii=False, separators=(",", ":"))
    assert len(rendered) <= 6_000
    digests = compacted[0]["evidence_json"]["evidence_segment_digests"]
    assert [row["segment"]["index"] for row in digests] == list(range(1, 11))
    assert compacted[0]["evidence_json"]["citation_allowlist"]["document_ids"] == list(range(1, 30))


def output(*, empty=False):
    return {"symbol": "688627", "decision": "WATCH", "confidence": .7, "reasoning": "基于项目进展证据观察履约情况",
            "target_price": None, "stop_price": None, "horizon_sessions": 10,
            "evidence_document_ids": [] if empty else [123],
            "dimensions": [{"dimension": code,
                "status": "SUPPORTED" if code == "NEWS" and not empty else "INSUFFICIENT",
                "direction": "NEUTRAL" if code == "NEWS" and not empty else "UNKNOWN",
                "summary": "订单尚需履约" if code == "NEWS" and not empty else "本轮证据不足",
                "evidence_document_ids": [123] if code == "NEWS" and not empty else [],
                "evidence_fact_ids": [], "evidence_relation_ids": []}
                for code in ("NEWS", "NOTICE", "FINANCIAL", "PRICE_VOLUME", "FUND_FLOW", "COMPANY", "GRAPH")],
            "events": [], "positives": [], "risks": [], "watch_conditions": []}


def analyze(responses, models=None):
    run = SimpleNamespace(model_instance_code="ZHIPU", data_mode="REAL", error_message=None,
                          analysis_mode="MODEL_REQUESTED",
                          criteria_json={"analysis_contract": "MULTI_SOURCE_V2",
                                         "request": {"model_instance_codes": models or ["ZHIPU"]}})
    item = {"symbol": "688627", "market": "CN_A", "evidence_json": evidence(),
            "analysis_json": {"scoring": {"trusted_score": 88}, "summary": "旧结论",
                              "model": {"target_price": 200}}}
    calls = []

    def chat(**kwargs):
        calls.append(kwargs)
        response = responses[min(len(calls) - 1, len(responses) - 1)]
        return SimpleNamespace(id=len(calls), instance_code=kwargs["instance_code"],
                               status="FAILED" if response == "timeout" else "SUCCESS",
                               error_message="模型请求超时 TimeoutError" if response == "timeout" else None,
                               response_text="" if response == "timeout" else json.dumps({"candidates": [response]}, ensure_ascii=False))

    with patch("app.services.model_hub.ModelHubService.chat", side_effect=chat), patch("app.services.selection.clock.sleep"):
        _apply_model_analysis(None, run, [(item, 1.0)])
    return run, item["analysis_json"], calls


def test_oversized_analysis_maps_evidence_segments_before_final_reduce(isolated):
    db = isolated
    skill_codes = [
        "STOCK_SELECTION_ANALYST", "STOCK_EVENT_LINKAGE_ANALYST",
        "BUSINESS_MOAT_VALUE_ANALYST", "VALUE_CHAIN_TRANSMISSION_ANALYST",
    ]
    db.add_all([
        ModelSkill(skill_code=code, skill_name=code, instructions="治理约束。" * 800,
                   enabled=True, skill_type="PROMPT_SOP")
        for code in skill_codes
    ])
    db.commit()
    item = {
        "market": "CN_A", "symbol": "688627", "name": "精智达",
        "evidence_json": {
            **evidence(),
            "documents": [
                {"id": index, "title": f"新闻 {index}", "source_table": "stock_news",
                 "published_at": "2026-10-01", "content_state": "TEXT_AVAILABLE", "content": BODY * 20}
                for index in range(1, 81)
            ],
            "document_ids": list(range(1, 81)),
            "facts": [
                {"id": f"fact:{index}", "fact_type": f"CUSTOM_{index}",
                 "properties": {"value": index, "detail": BODY * 60}}
                for index in range(16)
            ],
        },
        "analysis_json": {},
    }
    run = SimpleNamespace(model_instance_code="TEST_MODEL", data_mode="REAL", error_message=None,
                          analysis_mode="MODEL_REQUESTED", criteria_json={
                              "analysis_contract": "LEGACY",
                              "request": {"model_instance_codes": ["TEST_MODEL"]},
                          })
    calls = []

    def chat(**kwargs):
        calls.append(kwargs)
        purpose = kwargs["metadata_json"]["purpose"]
        if purpose == "STOCK_SELECTION_EVIDENCE_SEGMENT":
            segment = json.loads(kwargs["messages"][-1]["content"].split("证据分段：", 1)[1])
            document_ids = segment["evidence_json"]["citation_allowlist"]["document_ids"]
            response = {"analyzed_document_ids": document_ids, "findings": [],
                        "conflicts": [], "limitations": []}
        elif purpose == "STOCK_SELECTION_ANALYSIS":
            # Force one contract failure. The bounded retry must reuse the
            # segment digests instead of rebuilding the original 60k+ prompt.
            response = {"unexpected": []}
        else:
            response = {"candidates": [output()]}
            response["candidates"][0]["evidence_document_ids"] = [1]
            news = next(row for row in response["candidates"][0]["dimensions"] if row["dimension"] == "NEWS")
            news["evidence_document_ids"] = [1]
        return SimpleNamespace(id=len(calls), instance_code="TEST_MODEL", status="SUCCESS",
                               error_message=None, response_text=json.dumps(response, ensure_ascii=False))

    with patch("app.services.model_hub.ModelHubService.chat", side_effect=chat):
        _apply_model_analysis(db, run, [(item, 1.0)])
    purposes = [call["metadata_json"]["purpose"] for call in calls]
    segment_calls = [call for call in calls if call["metadata_json"]["purpose"] == "STOCK_SELECTION_EVIDENCE_SEGMENT"]
    assert len(segment_calls) > 1
    assert all(call["metadata_json"]["skill_codes"] == [] for call in segment_calls)
    assert max(sum(len(message.get("content", "")) for message in call["messages"]) for call in calls) < 56_000
    analysis_call = next(call for call in calls if call["metadata_json"]["purpose"] == "STOCK_SELECTION_ANALYSIS")
    assert analysis_call["metadata_json"]["context_strategy"] == "EVIDENCE_RECORD_MAP_REDUCE"
    assert purposes[-1] == "STOCK_SELECTION_PER_STOCK_RETRY"
    assert "evidence_segment_digests" in calls[-1]["messages"][-1]["content"]
    assert item["analysis_json"]["model_output_status"] == "SUCCESS", item["analysis_json"]


def test_timeout_switches_only_to_selected_model_then_succeeds():
    run, analysis, calls = analyze(["timeout", output()], models=["ZHIPU", "BACKUP"])
    assert [call["instance_code"] for call in calls] == ["ZHIPU", "BACKUP"]
    assert all(call["metadata_json"]["transport_max_attempts"] == 1 for call in calls)
    assert analysis["model_output_status"] == "SUCCESS"
    assert analysis["actual_instance_code"] == "BACKUP"
    assert run.analysis_mode == "MODEL_COMPLETED"


@pytest.mark.parametrize("models, expected", [(["ZHIPU"], ["ZHIPU"] * 3),
                                             (["ZHIPU", "BACKUP"], ["ZHIPU", "BACKUP", "ZHIPU"])])
def test_exactly_three_failed_calls_then_no_old_conclusion(models, expected):
    run, analysis, calls = analyze(["timeout"], models=models)
    assert [call["instance_code"] for call in calls] == expected
    assert len(analysis["model_attempts"]) == 3
    assert analysis["model_output_status"] == "FAILED"
    assert analysis["conclusion_available"] is False
    assert not any(key in analysis for key in ("model", "scoring", "summary", "target_price"))
    assert run.analysis_mode == "MODEL_FAILED_FALLBACK_LOCAL"


@pytest.mark.parametrize("second_succeeds", [True, False])
def test_analysis_progress_counts_terminal_failures_without_timed_or_retry_advances(second_succeeds):
    run = SimpleNamespace(model_instance_code="ZHIPU", data_mode="REAL", error_message=None,
                          criteria_json={"analysis_contract": "MULTI_SOURCE_V2"})
    items = [{"symbol": symbol, "market": "CN_A", "evidence_json": evidence(), "analysis_json": {}}
             for symbol in ("688627", "300474")]
    progress = []
    call_progress = []

    def record(_db, _run, stage, percent, message, **extra):
        progress.append({"stage": stage, "percent": percent, "message": message, **extra})

    def chat(**kwargs):
        call_progress.append(deepcopy(progress[-1]))
        symbol = kwargs["metadata_json"]["candidate_symbols"][0]
        success = second_succeeds and symbol == "300474"
        result = {**output(), "symbol": symbol}
        return SimpleNamespace(id=len(call_progress), instance_code="ZHIPU",
            status="SUCCESS" if success else "FAILED", error_message=None if success else "模型超时",
            response_text=json.dumps({"candidates": [result]}, ensure_ascii=False) if success else "")

    with patch("app.services.selection._progress", side_effect=record), \
            patch("app.services.model_hub.ModelHubService.chat", side_effect=chat), \
            patch("app.services.selection.clock.sleep"):
        _apply_model_analysis(None, run, [(item, 1.0) for item in items], max_batch_candidates=1)

    # Calls hold the same checkpoint until a stock succeeds or exhausts all
    # three attempts. Starting the next stock or retry cannot move it back.
    expected = [45, 45, 65, 65] if second_succeeds else [45, 45, 45, 45, 65, 65]
    assert [row["percent"] for row in call_progress] == expected
    assert [row["percent"] for row in progress] == sorted(row["percent"] for row in progress)
    finished = [row for row in progress if row["stage"] == "RESULT_VALIDATION"]
    assert finished[-1]["completed_stock_count"] == 2
    assert finished[-1]["verified_stock_count"] == int(second_succeeds)
    assert finished[-1]["failed_stock_count"] == (1 if second_succeeds else 2)
    assert finished[-1]["percent"] == 85
    assert len(items[0]["analysis_json"]["model_attempts"]) == 3
    assert items[0]["analysis_json"]["model_output_status"] == "FAILED"


def test_result_counts_are_exclusive_and_do_not_rewrite_empty_historical_successes():
    rows = [SimpleNamespace(evidence_json=payload, analysis_json=analysis) for payload, analysis in [
        ({}, {"model_output_status": "SUCCESS", "analysis_validation_status": "PARTIAL"}),
        ({}, {"model_output_status": "FAILED", "analysis_validation_status": "PARTIAL"}),
        (evidence(), {"model_output_status": "FAILED", "analysis_validation_status": "PARTIAL"}),
        (evidence(), {"model_output_status": "SUCCESS", "analysis_validation_status": "PARTIAL"}),
        (evidence(), {"model_output_status": "SUCCESS", "analysis_validation_status": "PASSED",
                      "model": {"reasoning": "根据原始证据研判", "confidence": .7}, "scoring": valid_scorecard(),
                      "llm_evaluation": {"status": "FAILED"}}),
    ]]
    before = deepcopy([row.analysis_json for row in rows])
    counts = _selection_result_counts(rows)
    assert counts == {"passed_count": 1, "citation_partial_count": 0, "failed_count": 2,
                      "data_blocked_count": 2, "evaluation_failed_count": 1}
    assert sum(counts[key] for key in ("passed_count", "citation_partial_count", "failed_count", "data_blocked_count")) == len(rows)
    assert [row.analysis_json for row in rows] == before


def test_financial_event_type_is_repaired_instead_of_reported_as_provider_failure():
    malformed = output()
    malformed["events"] = [{"source_type": "FINANCIAL"}]
    _run, analysis, calls = analyze([malformed, output()])
    assert len(calls) == 2 and analysis["model_output_status"] == "SUCCESS"
    assert analysis["model_attempts"][0]["status"] == "INVALID"
    assert "source_type" in analysis["model_attempts"][0]["reason"]


def test_all_insufficient_response_is_failure_after_three_attempts():
    _run, analysis, calls = analyze([output(empty=True)])
    assert len(calls) == 3
    assert analysis["model_output_status"] == "FAILED"
    assert "部分完成" in analysis["model_attempts"][-1]["reason"]


def test_three_contract_failures_cannot_be_promoted_to_partial_success():
    missing_dimensions = output()
    missing_dimensions["dimensions"] = missing_dimensions["dimensions"][:1]
    malformed_retry = {"symbol": "688627", "headline": "模型遗漏了必填字段"}
    _run, analysis, calls = analyze([missing_dimensions, malformed_retry, malformed_retry])
    assert len(calls) == 3
    assert analysis["model_output_status"] == "FAILED"
    assert not analysis.get("analysis_validation_status")
    assert not analysis.get("model")
    assert analysis["conclusion_available"] is False
    assert analysis["model_failure_detail"]["category"] == "MODEL_SCHEMA"
    assert all("horizon_sessions" in call["messages"][1]["content"] for call in calls)


def test_invalid_dimension_citations_fail_after_three_unsuccessful_repairs():
    partial = output()
    partial["dimensions"][1].update(status="SUPPORTED", direction="POSITIVE", evidence_document_ids=[123])
    _run, analysis, calls = analyze([partial, output()])
    assert len(calls) == 2
    assert analysis["analysis_validation_status"] == "PASSED"
    assert "公告披露" in analysis["model_attempts"][0]["reason"]
    _run, analysis, calls = analyze([partial])
    assert len(calls) == 3
    assert analysis["model_output_status"] == "FAILED"
    assert not analysis.get("model") and not analysis.get("scoring")
    assert analysis["conclusion_available"] is False


@pytest.mark.parametrize("dimension, fact_type", [
    ("PRICE_VOLUME", "FINANCIAL_OBSERVATION"),
    ("FUND_FLOW", "FINANCIAL_OBSERVATION"),
    ("FINANCIAL", "MARKET_OBSERVATION"),
])
def test_valid_fact_id_cannot_support_the_wrong_business_dimension(dimension, fact_type):
    from app.services.selection import _sanitize_model_evidence_references, _validate_multi_source_output
    item = {"evidence_json": {**evidence(), "evidence_fact_ids": ["source:1"],
                              "facts": [{"id": "source:1", "fact_type": fact_type}]}}
    result = output()
    row = next(row for row in result["dimensions"] if row["dimension"] == dimension)
    row.update(status="SUPPORTED", direction="NEGATIVE", evidence_fact_ids=["source:1"])
    with pytest.raises(ValueError, match="证据类型不匹配"):
        _validate_multi_source_output(result, item, required=True)
    _sanitize_model_evidence_references(result, item)
    assert row["status"] == "INSUFFICIENT" and row["direction"] == "UNKNOWN"
    assert result["validation_status"] == "PARTIAL" and result["confidence"] is None
    _validate_multi_source_output(result, item, required=True)


@pytest.mark.parametrize("category, dimension, allowlist", [
    ("financial", "FINANCIAL", "financial_fact_ids"),
    ("market", "PRICE_VOLUME", "market_fact_ids"),
    ("fund_flow", "FUND_FLOW", "fund_flow_fact_ids"),
])
def test_structured_observation_survives_fact_cap_and_has_correct_citation_type(category, dimension, allowlist):
    from app.services.selection import _model_candidate_context, _validate_multi_source_output
    item = {"symbol": "688627", "evidence_json": {**evidence(), "structured_observations": {
        category: [{"id": "source:1", "properties": {"value": 15.2}}]}}}
    result = output()
    row = next(row for row in result["dimensions"] if row["dimension"] == dimension)
    row.update(status="SUPPORTED", direction="NEUTRAL", evidence_fact_ids=["source:1"])
    _validate_multi_source_output(result, item, required=True)
    compact = _model_candidate_context(item)
    assert compact["evidence_json"]["citation_allowlist"][allowlist] == ["source:1"]
    _validate_multi_source_output(result, compact, required=True)


def test_sync_appends_documents_once_and_keeps_single_stock_graph_scope(isolated):
    db = isolated
    source = DataSource(source_code="SYNC_TEST", source_name="隔离来源", adapter_type="MOCK")
    base = KnowledgeBase(kb_code="SYNC_KB", kb_name="同步证据", source_tables=["stock_news"])
    db.add_all([source, base])
    db.flush()
    graph = KnowledgeGraph(knowledge_base_id=base.id, graph_code="SYNC_GRAPH", graph_name="单股图谱",
                           symbol="688627", source_tables=["stock_news"], governance_status="GOVERNED")
    db.add(graph)
    db.flush()
    for symbol in ("688627", "300474"):
        db.add(StockNews(market="CN_A", symbol=symbol, title=f"{symbol}进展", content=BODY,
                         news_time=datetime.now().strftime("%Y-%m-%d %H:%M:%S"), source_id=source.id))
    cutoff = datetime.now(timezone.utc) - timedelta(hours=1)
    run = SelectionRun(market="CN_A", status="RUNNING", data_mode="REAL", as_of=cutoff,
                       knowledge_base_ids_json=[base.id], graph_ids_json=[graph.id],
                       criteria_json={"symbols": ["688627", "300474"]})
    db.add(run)
    db.commit()
    with patch("app.services.selection._sync_selection_data", return_value={"attempted": 2, "status": "SUCCESS"}):
        assert _prepare_selection_data(db, run)["added_source_documents"] == 1
        assert _prepare_selection_data(db, run)["added_source_documents"] == 0
    documents = list(db.scalars(select(KnowledgeDocument)))
    assert len(documents) == 1 and documents[0].symbol == "688627"
    assert documents[0].metadata_json["source_id"] == source.id
    assert run.as_of > cutoff
    assert run.criteria_json["queued_as_of"] == cutoff.isoformat()


def test_initial_run_executes_sync_preflight_analysis_in_order(isolated):
    db = isolated
    source = DataSource(source_code="ORDER_TEST", source_name="隔离来源", adapter_type="MOCK")
    base = KnowledgeBase(kb_code="ORDER_KB", kb_name="验证知识库")
    db.add_all([source, base])
    db.flush()
    graph = KnowledgeGraph(knowledge_base_id=base.id, graph_code="ORDER_GRAPH", graph_name="验证图谱",
                           governance_status="GOVERNED")
    db.add(graph)
    db.add(StockSymbol(market="CN_A", symbol="688627", name="精智达", exchange="SSE", source_id=source.id))
    db.commit()
    stages = []

    def sync(*args, **kwargs):
        stages.append("DATA")

    def preflight(*args, **kwargs):
        stages.append("MODEL_PREFLIGHT")

    def model(_db, _run, pool):
        stages.append("ANALYSIS")
        for item, _score in pool:
            item["analysis_json"]["model_output_status"] = "FAILED"

    with patch("app.services.selection._prepare_selection_data", side_effect=sync), \
            patch("app.services.selection._preflight_selection_models", side_effect=preflight), \
            patch("app.services.selection._apply_model_analysis", side_effect=model), \
            patch("app.services.selection._evidence", return_value=evidence()), \
            patch("app.services.selection._load_disclosure_bodies"):
        run = create_selection_run(db, SelectionRunCreate(symbols=["688627"], use_model=True, data_mode="REAL",
                                    knowledge_base_ids=[base.id], graph_ids=[graph.id]))
    assert stages == ["DATA", "MODEL_PREFLIGHT", "ANALYSIS"]
    assert run.completed_at is not None


@pytest.mark.parametrize("has_evidence", [True, False])
@pytest.mark.parametrize("has_untouched_empty_success", [True, False])
def test_failed_retry_enforces_data_model_analysis_order(isolated, has_evidence, has_untouched_empty_success):
    db = isolated
    run = SelectionRun(market="CN_A", data_mode="REAL", status="PARTIAL", model_instance_code="ZHIPU",
                       as_of=datetime.now(timezone.utc), criteria_json={"analysis_contract": "MULTI_SOURCE_V2"})
    db.add(run)
    db.flush()
    candidate = SelectionCandidate(run_id=run.id, market="CN_A", symbol="688627", name="精智达",
        decision="PENDING", evidence_json=evidence(),
        analysis_json={"model_output_status": "FAILED", "model": {"headline": "旧结论"}})
    db.add(candidate)
    untouched = None
    if has_untouched_empty_success:
        untouched = SelectionCandidate(run_id=run.id, market="CN_A", symbol="300474", evidence_json={},
            analysis_json={"model_output_status": "SUCCESS", "analysis_validation_status": "PARTIAL"})
        db.add(untouched)
    db.commit()
    stages = []

    sync_calls = []

    def sync(*args, **kwargs):
        stages.append("DATA")
        sync_calls.append(kwargs)

    def preflight(*args, **kwargs):
        stages.append("MODEL_PREFLIGHT")

    def model(_db, _run, pool, **kwargs):
        stages.append("ANALYSIS")
        for item, _score in pool:
            item["analysis_json"]["model_output_status"] = "FAILED"

    with patch("app.services.selection._prepare_selection_data", side_effect=sync), \
            patch("app.services.selection._preflight_selection_models", side_effect=preflight), \
            patch("app.services.selection._apply_model_analysis", side_effect=model), \
            patch("app.services.selection._latest_quotes", return_value=[]), \
            patch("app.services.selection._screening_bars", return_value={}), \
            patch("app.services.selection._evidence", return_value=evidence() if has_evidence else {}), \
            patch("app.services.selection._load_disclosure_bodies"), \
            patch("app.services.selection._generate_run_llm_evaluations", return_value={"failed": 0}):
        result = execute_failed_selection_reanalysis({"selection_run_id": run.id,
                                                      "candidate_ids": [candidate.id]})
    db.expire_all()
    actual = db.get(SelectionCandidate, candidate.id)
    actual_run = db.get(SelectionRun, run.id)
    assert actual_run.criteria_json["analysis_contract"] == "MULTI_SOURCE_VALUE_CHAIN_V5"
    assert stages == (["DATA", "MODEL_PREFLIGHT", "ANALYSIS"] if has_evidence else ["DATA"])
    # The retry decision uses the candidate's persisted evidence before the
    # new local read. Both parameterizations start with usable evidence, so a
    # model retry must not collect it again.
    assert sync_calls[0]["force_refresh"] is False
    assert actual.analysis_json["model_output_status"] == ("FAILED" if has_evidence else "NOT_RUN")
    assert not actual.analysis_json.get("model")
    assert result["data_blocked_count"] == (0 if has_evidence else 1) + int(has_untouched_empty_success)
    assert result["citation_partial_count"] == 0 and result["completed_count"] == 0
    from app.api.selection import _run_read
    displayed = _run_read(db, actual_run)
    assert displayed.criteria_json["result_counts"] == actual_run.criteria_json["result_counts"]
    if untouched is not None:
        assert db.get(SelectionCandidate, untouched.id).analysis_json["model_output_status"] == "SUCCESS"
    assert all(phase["status"] != "RUNNING" for phase in actual_run.criteria_json["progress"]["phases"].values())


def test_explicit_analysis_phase_never_reopens_data_phase(isolated):
    from app.services.selection import _initial_progress, _progress
    db = isolated
    run = SelectionRun(market="CN_A", status="RUNNING", criteria_json={"progress": _initial_progress("开始")})
    db.add(run)
    db.commit()
    _progress(db, run, "DATA_SYNC", 5, "同步数据", phase="DATA")
    _progress(db, run, "MODEL_PREFLIGHT", 42, "测试模型", phase="MODEL_PREFLIGHT")
    _progress(db, run, "MODEL_ANALYSIS", 45, "分析开始", phase="ANALYSIS")
    phases = run.criteria_json["progress"]["phases"]
    assert phases["DATA"]["status"] == "SUCCESS"
    assert phases["MODEL_PREFLIGHT"]["status"] == "SUCCESS"
    assert phases["ANALYSIS"]["status"] == "RUNNING"
    assert [activity["message"] for activity in phases["DATA"]["activities"]] == ["同步数据"]
    assert phases["MODEL_PREFLIGHT"]["completed_at"] == phases["ANALYSIS"]["started_at"]


def test_heartbeat_preserves_completed_work_instead_of_rewinding_or_inventing_progress(isolated):
    from app.services.selection import _initial_progress, _progress, _SelectionProgressHeartbeat
    db = isolated
    run = SelectionRun(market="CN_A", status="RUNNING", criteria_json={"progress": _initial_progress("开始")})
    db.add(run)
    db.commit()
    heartbeat = _SelectionProgressHeartbeat(run.id, activity="等待采集", model_instance_code=None,
        batch_index=1, batch_total=1, candidate_symbols=["688627"], percent=5, purpose="DATA_SYNC")
    _progress(db, run, "DATA_SYNC", 23, "日线已采集，正在读取公告", phase="DATA")
    heartbeat._beat()
    db.refresh(run)
    assert run.criteria_json["progress"]["percent"] == 23
    assert run.criteria_json["progress"]["activity"] == "日线已采集，正在读取公告"
    assert run.criteria_json["progress"]["heartbeat_at"]


def test_preflight_retries_three_times_with_selected_models(isolated):
    db = isolated
    run = SelectionRun(market="CN_A", status="RUNNING", data_mode="REAL", model_instance_code="ZHIPU",
                       criteria_json={"request": {"model_instance_codes": ["ZHIPU", "BACKUP"]}})
    db.add(run)
    db.commit()
    with patch("app.services.selection.probe_selection_model", side_effect=TimeoutError("模型超时")) as probe, \
            patch("app.services.selection.clock.sleep"), patch("app.services.selection._SelectionProgressHeartbeat"):
        result = _preflight_selection_models(db, run)
    assert [call.args[1] for call in probe.call_args_list] == ["ZHIPU", "BACKUP", "ZHIPU"]
    assert result is not None
    assert run.criteria_json["progress"]["phases"]["ANALYSIS"]["status"] == "SKIPPED"


def test_historical_empty_conclusion_is_hidden_without_changing_audit_row(isolated):
    from app.api.selection import _candidate_read
    db = isolated
    run = SelectionRun(market="CN_A", data_mode="REAL", status="PARTIAL", analysis_mode="MODEL_PARTIAL")
    db.add(run)
    db.flush()
    old_analysis = {"model_output_status": "SUCCESS", "analysis_validation_status": "PARTIAL",
                    "model": {"headline": "旧错误结论"}, "scoring": {"trusted_score": 0}}
    candidate = SelectionCandidate(run_id=run.id, market="CN_A", symbol="300474",
                                   evidence_json={}, analysis_json=deepcopy(old_analysis))
    db.add(candidate)
    db.commit()
    projected = _candidate_read(db, candidate).analysis_json
    assert projected["model_output_status"] == "NOT_RUN" and projected["conclusion_available"] is False
    assert "model" not in projected and "scoring" not in projected
    assert candidate.analysis_json == old_analysis


def test_read_does_not_attach_other_run_model_call_or_fabricate_heartbeat(isolated):
    from app.api.selection import _run_read
    db = isolated
    run = SelectionRun(market="CN_A", status="RUNNING", criteria_json={"progress": {"percent": 45, "stage": "MODEL"}})
    db.add(run)
    db.flush()
    db.add(ModelCallLog(task_type="stock_screening", status="RUNNING", instance_code="ZHIPU",
                        request_json={"metadata_json": {"selection_run_id": run.id + 1}}))
    db.commit()
    progress = _run_read(db, run).criteria_json["progress"]
    assert progress["percent"] == 45
    assert not progress.get("active_model_call_id") and not progress.get("heartbeat_at")


def test_terminal_summary_agrees_with_data_gate_without_rewriting_history(isolated):
    from app.api.selection import _run_read
    db = isolated
    old_criteria = {"progress": {"message": "旧待核对 1 只"},
                    "retry_progress": {"status": "PARTIAL", "message": "旧待核对 1 只"}}
    run = SelectionRun(market="CN_A", status="PARTIAL", data_mode="REAL", analysis_mode="MODEL_PARTIAL",
                       completed_at=datetime.now(timezone.utc), error_message="旧引用错误", criteria_json=deepcopy(old_criteria))
    db.add(run)
    db.flush()
    db.add_all([
        SelectionCandidate(run_id=run.id, market="CN_A", symbol="300474", evidence_json={},
            analysis_json={"model_output_status": "SUCCESS", "analysis_validation_status": "PARTIAL",
                           "model_failure_detail": {"category": "MODEL_UNKNOWN"}}),
        SelectionCandidate(run_id=run.id, market="CN_A", symbol="688627", evidence_json=evidence(),
            analysis_json={"model_output_status": "FAILED", "model_failure_reason": "模型输出不符合分析契约"}),
    ])
    db.commit()
    result = _run_read(db, run)
    assert result.criteria_json["result_counts"] == {
        "passed_count": 0, "citation_partial_count": 0, "failed_count": 1, "data_blocked_count": 1, "evaluation_failed_count": 0}
    assert "模型/结论/评分失败 1 只、数据不足 1 只" in result.error_message
    assert result.criteria_json["retry_progress"]["message"] == result.error_message
    assert result.criteria_json["progress"]["message"] == result.error_message
    empty = next(row for row in result.candidates if row.symbol == "300474")
    assert "model_failure_detail" not in empty.analysis_json
    assert empty.analysis_json["historical_model_failure_detail"]["category"] == "MODEL_UNKNOWN"
    assert run.criteria_json == old_criteria and run.error_message == "旧引用错误"


def test_terminal_summary_preserves_evaluation_failure(isolated):
    from app.api.selection import _run_read
    db = isolated
    run = SelectionRun(market="CN_A", status="PARTIAL", data_mode="REAL", analysis_mode="MODEL_PARTIAL",
                       completed_at=datetime.now(timezone.utc), error_message="LLM 评价生成失败")
    db.add(run)
    db.flush()
    db.add(SelectionCandidate(run_id=run.id, market="CN_A", symbol="688627", evidence_json=evidence(),
        analysis_json={"model_output_status": "SUCCESS", "analysis_validation_status": "PASSED",
                       "llm_evaluation": {"status": "FAILED"}}))
    db.commit()
    result = _run_read(db, run)
    assert result.criteria_json["result_counts"]["evaluation_failed_count"] == 1
    assert "LLM 评价生成失败" in result.error_message


def test_selection_transport_budget_disables_hidden_retry():
    from app.services.model_hub import ModelHubService
    with patch("app.services.model_hub.get_model_adapter") as adapter:
        adapter.return_value.chat.side_effect = TimeoutError("模型超时")
        with pytest.raises(TimeoutError):
            ModelHubService(None)._chat_with_transient_retry(provider_type="OPENAI_COMPAT",
                instance=SimpleNamespace(instance_code="ZHIPU", model_code="glm", api_key=""),
                messages=[], temperature=None, max_tokens=None, metadata_json={"transport_max_attempts": 1})
        assert adapter.return_value.chat.call_count == 1


@pytest.mark.parametrize("eventually_succeeds", [True, False])
def test_llm_evaluation_retries_raised_timeouts_and_preserves_attempts(isolated, eventually_succeeds):
    from app.services.selection import generate_candidate_llm_evaluation
    db = isolated
    run = SelectionRun(market="CN_A", status="RUNNING", data_mode="REAL", model_instance_code="ZHIPU",
                       criteria_json={"request": {"model_instance_codes": ["ZHIPU", "BACKUP"]}})
    db.add(run)
    db.flush()
    candidate = SelectionCandidate(run_id=run.id, market="CN_A", symbol="688627", evidence_json=evidence(),
        analysis_json={"model_output_status": "SUCCESS", "analysis_validation_status": "PASSED",
                       "model": {"decision": "WATCH"}})
    db.add(candidate)
    db.commit()
    success = SimpleNamespace(id=900, status="SUCCESS", response_text="基于已核验订单证据建议跟踪履约情况。", error_message=None)
    responses = [TimeoutError("模型超时"), success] if eventually_succeeds else [TimeoutError("模型超时")] * 3
    with patch("app.services.model_hub.ModelHubService.chat", side_effect=responses) as chat, \
            patch("app.services.selection.clock.sleep"), patch("app.services.selection._SelectionProgressHeartbeat"):
        result = generate_candidate_llm_evaluation(db, candidate)
    assert chat.call_count == (2 if eventually_succeeds else 3)
    assert [call.kwargs["instance_code"] for call in chat.call_args_list] == (["ZHIPU", "BACKUP"] if eventually_succeeds else ["ZHIPU", "BACKUP", "ZHIPU"])
    assert result["status"] == ("SUCCESS" if eventually_succeeds else "FAILED")
    assert len(result["attempts"]) == chat.call_count
    assert candidate.analysis_json["llm_evaluation"] == result
