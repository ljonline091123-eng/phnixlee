"""Regression checks for the live selection gates, retries and historical views."""
from copy import deepcopy
from datetime import datetime, timedelta, timezone
import json
from types import SimpleNamespace
from unittest.mock import patch

import pytest
from sqlalchemy import create_engine, select
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.db.base import Base
from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, KnowledgeGraph, ModelCallLog
from app.models.market_data import DataSource, StockNews, StockSymbol
from app.models.selection import SelectionCandidate, SelectionRun
from app.schemas.selection import SelectionRunCreate
from app.services.selection import (
    _apply_model_analysis, _evidence_data_gate, _prepare_selection_data,
    _preflight_selection_models, create_selection_run, execute_failed_selection_reanalysis,
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


def test_invalid_dimension_citations_are_retried_before_partial_is_accepted():
    partial = output()
    partial["dimensions"][1].update(status="SUPPORTED", direction="POSITIVE", evidence_document_ids=[123])
    _run, analysis, calls = analyze([partial, output()])
    assert len(calls) == 2
    assert analysis["analysis_validation_status"] == "PASSED"
    assert "公告披露" in analysis["model_attempts"][0]["reason"]
    _run, analysis, calls = analyze([partial])
    assert len(calls) == 3
    assert analysis["analysis_validation_status"] == "PARTIAL"
    assert analysis["confidence"] is None and analysis["target_price"] is None
    assert analysis["model"]["dimensions"][1]["status"] == "INSUFFICIENT"


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
def test_failed_retry_enforces_data_model_analysis_order(isolated, has_evidence):
    db = isolated
    run = SelectionRun(market="CN_A", data_mode="REAL", status="PARTIAL", model_instance_code="ZHIPU",
                       as_of=datetime.now(timezone.utc), criteria_json={"analysis_contract": "MULTI_SOURCE_V2"})
    db.add(run)
    db.flush()
    candidate = SelectionCandidate(run_id=run.id, market="CN_A", symbol="688627", name="精智达",
        decision="PENDING", evidence_json=evidence(),
        analysis_json={"model_output_status": "FAILED", "model": {"headline": "旧结论"}})
    db.add(candidate)
    db.commit()
    stages = []

    def sync(*args, **kwargs):
        stages.append("DATA")

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
    assert stages == (["DATA", "MODEL_PREFLIGHT", "ANALYSIS"] if has_evidence else ["DATA"])
    assert actual.analysis_json["model_output_status"] == ("FAILED" if has_evidence else "NOT_RUN")
    assert not actual.analysis_json.get("model")
    assert result["data_blocked_count"] == (0 if has_evidence else 1)
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
    assert "模型失败 1 只、数据不足 1 只" in result.error_message
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
