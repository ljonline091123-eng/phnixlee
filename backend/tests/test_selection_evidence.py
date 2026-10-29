from sqlalchemy import create_engine
from sqlalchemy.orm import Session
from types import SimpleNamespace
from unittest.mock import patch
from datetime import datetime, timedelta, timezone
import json

from app.db.base import Base
from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, KnowledgeEntity, KnowledgeGraph, KnowledgeRelation
from app.models.market_data import StockSymbol, DataSource
from app.services.graph_rag import build_selection_context
from app.services.selection import _apply_model_analysis, _evidence


def test_evidence_balances_sources_and_honors_explicit_knowledge_scope():
    engine = create_engine("sqlite://")
    Base.metadata.create_all(engine)
    with Session(engine) as db:
        base = KnowledgeBase(kb_code="DEMO_KB", kb_name="Demo")
        db.add(base)
        db.flush()
        graph = KnowledgeGraph(knowledge_base_id=base.id, graph_code="DEMO_GRAPH", graph_name="Demo")
        other = KnowledgeGraph(knowledge_base_id=base.id, graph_code="DEMO_OTHER", graph_name="Other")
        db.add_all([graph, other])
        source = DataSource(source_code="DEMO", source_name="Demo", adapter_type="MOCK")
        db.add(source)
        db.flush()
        db.add(StockSymbol(market="CN_A", symbol="DEMO001", name="Demo", exchange="SZSE", source_id=source.id, status="LISTED"))
        db.flush()

        def document(source, title, graph_id):
            row = KnowledgeDocument(knowledge_base_id=base.id, graph_id=graph_id,
                source_table=source, market="CN_A", symbol="DEMO001", title=title, content="Demo evidence")
            db.add(row)
            return row

        news = document("stock_news", "Old but relevant news", graph.id)
        policy = document("stock_context_event", "Policy evidence", graph.id)
        for index in range(100):
            document("stock_kline", f"Price {index}", graph.id)
        excluded = document("stock_news", "Other graph", other.id)
        head = KnowledgeEntity(knowledge_base_id=base.id, graph_id=graph.id, entity_type="STOCK",
            entity_key=f"{graph.id}:CN_A:DEMO001", entity_name="Demo company")
        tail = KnowledgeEntity(knowledge_base_id=base.id, graph_id=graph.id, entity_type="POLICY",
            entity_key=f"{graph.id}:POLICY:1", entity_name="Demo policy")
        db.add_all([head, tail])
        db.flush()
        db.add(KnowledgeRelation(knowledge_base_id=base.id, graph_id=graph.id,
            subject_entity_id=head.id, object_entity_id=tail.id, predicate="MENTIONS_POLICY",
            evidence_document_id=policy.id))
        db.commit()

        evidence = _evidence(db, "CN_A", "DEMO001", [base.id], [graph.id])
        assert news.id in evidence["document_ids"]
        assert policy.id in evidence["document_ids"]
        assert excluded.id not in evidence["document_ids"]
        assert len(evidence["documents"]) == 4  # two price rows, news and policy
        assert evidence["relations"][0]["evidence_text"] == "Demo evidence"
        assert evidence["relations"][0]["head"] == "Demo company"
        assert evidence["chunk_coverage"]["scope"] == "BOUNDED_SELECTION_CONTEXT"
        assert {row["source_table"] for row in evidence["evidence_context"]["documents"]} >= {
            "stock_kline", "stock_news", "stock_context_event",
        }
        cutoff = datetime.now(timezone.utc) + timedelta(seconds=1)
        future = document("stock_news", "Future evidence", graph.id)
        future.created_at = cutoff + timedelta(days=1)
        future.updated_at = cutoff + timedelta(days=1)
        db.commit()
        historical = _evidence(
            db, "CN_A", "DEMO001", [base.id], [graph.id], as_of=cutoff,
        )
        assert future.id not in historical["document_ids"]
        assert historical["data_cutoff"] == cutoff.date().isoformat()
        no_scope = _evidence(db, "CN_A", "DEMO001", [], [])
        assert no_scope["documents"] == [] and no_scope["relations"] == []
        kb_only = _evidence(db, "CN_A", "DEMO001", [base.id], [])
        assert kb_only["documents"] and kb_only["relations"] == []
    engine.dispose()


def test_unmatched_news_is_excluded_from_selection_and_graph_rag_without_deleting_it():
    engine = create_engine("sqlite://")
    Base.metadata.create_all(engine)
    with Session(engine) as db:
        base = KnowledgeBase(kb_code="IDENTITY_KB", kb_name="主体核验")
        db.add(base)
        db.flush()
        graph = KnowledgeGraph(knowledge_base_id=base.id, graph_code="IDENTITY_GRAPH", graph_name="主体核验")
        db.add(graph)
        source = DataSource(source_code="DEMO", source_name="Demo", adapter_type="MOCK")
        db.add(source)
        db.flush()
        db.add(StockSymbol(market="CN_A", symbol="000001", name="平安银行", exchange="SZSE", source_id=source.id, status="LISTED"))
        db.flush()
        matched = KnowledgeDocument(knowledge_base_id=base.id, graph_id=graph.id, source_table="stock_news",
            market="CN_A", symbol="000001", title="平安银行公布中报", content="主体已匹配的原文")
        candidate = KnowledgeDocument(knowledge_base_id=base.id, graph_id=graph.id, source_table="stock_news",
            market="CN_A", symbol="000001", title="000001上证指数ETF上涨", content="未提及发行主体")
        head = KnowledgeEntity(knowledge_base_id=base.id, graph_id=graph.id, entity_type="STOCK",
            entity_key=f"{graph.id}:CN_A:000001", entity_name="平安银行")
        tail = KnowledgeEntity(knowledge_base_id=base.id, graph_id=graph.id, entity_type="NEWS",
            entity_key=f"{graph.id}:NEWS:1", entity_name="新闻")
        db.add_all([matched, candidate, head, tail])
        db.flush()
        db.add(KnowledgeRelation(knowledge_base_id=base.id, graph_id=graph.id, subject_entity_id=head.id,
            object_entity_id=tail.id, predicate="HAS_NEWS", evidence_document_id=candidate.id))
        db.commit()
        evidence = _evidence(db, "CN_A", "000001", [base.id], [graph.id])
        context = build_selection_context(db, "CN_A", "000001", knowledge_base_ids=[base.id], graph_ids=[graph.id])
        assert matched.id in evidence["document_ids"]
        assert candidate.id not in evidence["document_ids"]
        assert not evidence["relations"]
        assert candidate.id not in context["evidence_document_ids"]
        assert not context["graph_paths"]
        assert db.get(KnowledgeDocument, candidate.id) is not None
        db.delete(db.query(StockSymbol).filter_by(market="CN_A", symbol="000001").one())
        db.commit()
        assert not _evidence(db, "CN_A", "000001", [base.id], [graph.id])["documents"]
    engine.dispose()


def test_model_calls_batch_large_evidence_and_use_workflow_schema():
    run = SimpleNamespace(model_instance_code=None, analysis_mode="MODEL_REQUESTED", error_message=None)
    pool = [({"symbol": f"DEMO{index:03}", "analysis_json": {}, "evidence_json": {
        "document_ids": [index], "documents": [{"id": index, "excerpt": "x" * 120_000}],
        "chunks": [{"id": f"chunk-{index}", "text": "y" * 100_000}],
        "lineage": [{"metadata": {"oversized": "z" * 100_000}}],
    }}, 1.0) for index in range(20)]
    requests = []

    def chat(**kwargs):
        requests.append(kwargs)
        assert sum(len(message["content"]) for message in kwargs["messages"]) < 60_000
        assert kwargs["messages"][0]["role"] == "system"
        text = kwargs["messages"][-1]["content"]
        batch = json.loads(text[text.index("["):])
        assert len(batch) <= 5
        assert all(len(json.dumps(row, ensure_ascii=False)) < 32_000 for row in batch)
        assert len(batch[0]["evidence_json"]["documents"][0]["excerpt"]) <= 360
        output = {"candidates": [{"symbol": row["symbol"], "decision": "WATCH", "confidence": .5,
            "reasoning": "Demo inference", "target_price": None, "stop_price": None,
            "horizon_sessions": 10, "evidence_document_ids": row["evidence_json"]["document_ids"]} for row in batch]}
        return SimpleNamespace(id=len(requests), status="SUCCESS", instance_code="DEMO_MODEL", response_text=json.dumps(output))

    with patch("app.services.model_hub.ModelHubService.chat", side_effect=chat):
        _apply_model_analysis(None, run, pool)
    assert len(requests) > 1
    assert run.analysis_mode == "MODEL_COMPLETED"
    assert all(row["analysis_json"]["model_output_status"] == "SUCCESS" for row, _ in pool)
    assert len(pool[0][0]["evidence_json"]["documents"][0]["excerpt"]) == 120_000
    assert all(request["instance_code"] is None for request in requests)


def test_compact_document_whitelist_matches_visible_text_and_chunk_proofs():
    from app.services.selection import _model_candidate_context
    item = {"evidence_json": {"model_document_ids": list(range(1, 50)), "document_ids": list(range(1, 50)),
        "documents": [{"id": 49, "excerpt": "实际可见原文"}],
        "chunks": [{"id": "chunk", "document_id": "48", "text": "可见切片"}],
        "graph_paths": [{"id": 1, "evidence_document_id": 47, "evidence_excerpt": "关系证据"}]}}
    evidence = _model_candidate_context(item)["evidence_json"]
    assert evidence["model_document_ids"] == [47, 48, 49]
    assert evidence["document_ids"] == [47, 48, 49]


def test_source_whitelists_and_repair_feedback_distinguish_news_about_an_announcement():
    import pytest
    from app.services.selection import _model_candidate_context, _validate_multi_source_output
    item = {"symbol": "DEMO001", "evidence_json": {"documents": [
        {"id": 11, "source_table": "stock_news", "title": "公司公告回购",
         "content_state": "TEXT_AVAILABLE", "content": "新闻报道公司计划回购股份。"},
        {"id": 12, "source_table": "stock_notice", "content_state": "TEXT_AVAILABLE",
         "content": "关于为子公司担保的进展公告。"},
        {"id": 13, "source_table": "stock_notice", "content_state": "METADATA_ONLY", "title": "仅标题"}]}}
    context = _model_candidate_context(item)
    allowlist = context["evidence_json"]["citation_allowlist"]
    assert allowlist["news_document_ids"] == [11]
    assert allowlist["notice_document_ids"] == [12]
    output = {"evidence_document_ids": [11], "dimensions": [
        {"dimension": "NOTICE", "status": "SUPPORTED", "evidence_document_ids": [11]}], "events": []}
    with pytest.raises(ValueError, match=r"NOTICE.*当前引用\[11\].*只能引用\[12\]"):
        _validate_multi_source_output(output, context, required=False)


def test_multi_source_validator_accepts_visible_graph_and_chunk_document_refs():
    from app.services.selection import _validate_multi_source_output
    evidence = {"documents": [{"id": 49, "source_table": "stock_news", "content_state": "TEXT_AVAILABLE",
        "content": "公司披露新增订单，预计改善收入。"}],
        "chunks": [{"id": "chunk-1", "document_id": "48", "text": "业务资料"}],
        "graph_paths": [{"id": "path-1", "evidence_document_id": 47, "evidence_excerpt": "供应关系"}],
        "facts": [], "evidence_fact_ids": []}
    output = {"evidence_document_ids": [49], "dimensions": [
        {"dimension": "NEWS", "status": "SUPPORTED", "evidence_document_ids": [49],
         "evidence_fact_ids": [], "evidence_relation_ids": []},
        {"dimension": "GRAPH", "status": "SUPPORTED", "evidence_document_ids": [47],
         "evidence_fact_ids": [], "evidence_relation_ids": ["path-1"]}],
        "events": [{"document_id": 49, "source_type": "NEWS", "quote": "公司披露新增订单",
            "title": "订单披露", "direction": "POSITIVE", "mechanism": "收入改善", "timing": "中期"}]}
    _validate_multi_source_output(output, {"evidence_json": evidence}, required=False)
    assert output["evidence_document_ids"] == [47, 49]


def test_multi_source_validator_rejects_unseen_citation_and_metadata_only_news():
    import pytest
    from app.services.selection import _validate_multi_source_output
    base = {"evidence_json": {"documents": [{"id": 1, "source_table": "stock_news",
        "content_state": "TEXT_AVAILABLE", "content": "公司发布新增订单"}], "facts": [],
        "evidence_fact_ids": [], "chunks": [], "graph_paths": []}}
    output = {"evidence_document_ids": [2], "dimensions": [{"dimension": "NEWS", "status": "INSUFFICIENT",
        "evidence_document_ids": [2], "evidence_fact_ids": [], "evidence_relation_ids": []}], "events": []}
    with pytest.raises(ValueError):
        _validate_multi_source_output(output, base, required=False)
    base["evidence_json"]["documents"][0]["content_state"] = "METADATA_ONLY"
    output["evidence_document_ids"] = [1]
    output["dimensions"][0]["status"] = "SUPPORTED"
    output["dimensions"][0]["evidence_document_ids"] = [1]
    with pytest.raises(ValueError):
        _validate_multi_source_output(output, base, required=False)


def test_selection_retries_missing_dimensions_and_only_marks_success_after_validation():
    required = ["NEWS", "NOTICE", "FINANCIAL", "PRICE_VOLUME", "FUND_FLOW", "COMPANY", "GRAPH"]

    def recommendation(with_dimensions):
        row = {"symbol": "DEMO001", "decision": "WATCH", "confidence": .5, "reasoning": "证据不足，等待人工复核",
            "target_price": None, "stop_price": None, "horizon_sessions": 10, "evidence_document_ids": []}
        if with_dimensions:
            row["dimensions"] = [{"dimension": dimension, "status": "INSUFFICIENT", "direction": "UNKNOWN",
                "summary": "当前压缩上下文没有可验证证据", "evidence_document_ids": [], "evidence_fact_ids": [],
                "evidence_relation_ids": []} for dimension in required]
        return json.dumps({"candidates": [row]}, ensure_ascii=False)

    run = SimpleNamespace(model_instance_code=None, analysis_mode="MODEL_REQUESTED", error_message=None,
        criteria_json={"analysis_contract": "MULTI_SOURCE_V2"}, data_mode="MOCK")
    item = {"symbol": "DEMO001", "market": "CN_A", "name": "Demo", "analysis_json": {},
        "evidence_json": {"document_ids": [], "documents": [], "facts": [], "evidence_fact_ids": [],
            "chunks": [], "relations": [], "graph_paths": [], "missing_data": [], "evidence_context": {}}}
    calls = []

    def chat(**kwargs):
        calls.append(kwargs)
        response = recommendation(with_dimensions=len(calls) > 1)
        return SimpleNamespace(id=len(calls), status="SUCCESS", instance_code="DEMO_MODEL", response_text=response)

    with patch("app.services.model_hub.ModelHubService.chat", side_effect=chat):
        _apply_model_analysis(None, run, [(item, 1.0)])

    assert len(calls) == 2
    assert calls[1]["metadata_json"]["purpose"] == "STOCK_SELECTION_SINGLE_STOCK_CONTRACT_RETRY"
    assert run.analysis_mode == "MODEL_COMPLETED"
    assert item["analysis_json"]["model_output_status"] == "SUCCESS"
    assert item["analysis_json"]["analysis_contract"] == "MULTI_SOURCE_V2"
    assert {row["dimension"] for row in item["analysis_json"]["model"]["dimensions"]} == set(required)


def test_local_fallback_reports_all_source_dimensions_without_claiming_semantics():
    from app.services.selection import _build_local_multi_source_analysis
    report = _build_local_multi_source_analysis({"evidence_json": {
        "documents": [{"id": 1, "source_table": "stock_news", "content_state": "TEXT_AVAILABLE"},
                      {"id": 2, "source_table": "stock_notice", "content_state": "TEXT_AVAILABLE"}],
        "facts": [{"id": "finance:1", "fact_type": "FINANCIAL_OBSERVATION"}],
        "structured_observations": {"market": [{"id": "market:1"}], "shareholder": [{"id": "holder:1"}]},
        "graph_paths": [{"id": "path:1"}],
    }})
    assert {row["dimension"] for row in report["dimensions"]} == {"NEWS", "NOTICE", "FINANCIAL", "PRICE_VOLUME", "FUND_FLOW", "COMPANY", "GRAPH"}
    assert report["report_type"] == "LOCAL_MULTI_SOURCE_EVIDENCE"
    assert all(row["direction"] == "UNKNOWN" for row in report["dimensions"])
    assert report["evidence_document_ids"] == [1, 2]
def test_compact_financial_evidence_retains_revenue_and_profit_after_source_metadata():
    from app.services.selection import _model_candidate_context
    raw = {f"SOURCE_METADATA_{index}": str(index) for index in range(25)}
    raw.update(TOTALOPERATEREVE=1205169675.91, PARENTNETPROFIT=-68175381.59,
               KCFJCXSYJLR=-72603322.09, EPSJB=-0.1144,
               REPORT_DATE="2026-06-30", CURRENCY="CNY")
    item = {"evidence_json": {"facts": [{"id": "finance:1", "fact_type": "FINANCIAL_OBSERVATION",
        "properties": {"report_period": "2026-06-30", "currency": "CNY", "data": raw}}]}}
    view = _model_candidate_context(item)["evidence_json"]["facts"][0]["properties"]
    assert view["data"]["TOTALOPERATEREVE"] == 1205169675.91
    assert view["data"]["PARENTNETPROFIT"] == -68175381.59
    assert view["data"]["KCFJCXSYJLR"] == -72603322.09
    assert view["report_period"] == "2026-06-30"
    assert len(view["data"]) <= 16
    assert len(raw) == 31  # bounded view must not mutate historical evidence


def test_large_disclosure_and_shareholder_context_stays_within_call_budget():
    from app.services.selection import _model_candidate_context
    documents = [{
        "id": index + 1,
        "source_table": "stock_notice" if index % 2 else "stock_news",
        "title": f"第{index + 1}篇披露文件",
        "published_at": f"2026-10-{index % 28 + 1:02d}",
        "content_state": "TEXT_AVAILABLE",
        "content": "经营事项、财务影响和后续验证条件。" * 80,
        "url": f"https://example.test/{index + 1}",
        "market": "CN_A",
        "symbol": "DEMO001",
    } for index in range(84)]
    shareholder_payload = {
        f"section_{section}": [
            {f"field_{field}": f"value-{section}-{row}-{field}" * 20 for field in range(18)}
            for row in range(8)
        ]
        for section in range(18)
    }
    item = {"symbol": "DEMO001", "market": "CN_A", "evidence_json": {
        "documents": documents,
        "facts": [{"id": "holder:1", "fact_type": "SHAREHOLDER_OBSERVATION",
                   "properties": {"section": "holders", "payload": shareholder_payload}}],
    }}
    compact = _model_candidate_context(item)
    evidence = compact["evidence_json"]
    assert len(evidence["documents"]) == 84
    assert evidence["prompt_compaction"] == {
        "source_document_count": 84,
        "retained_document_count": 84,
        "omitted_document_count": 0,
        "disclosure_excerpt_limit": 72,
        "full_text_preserved_in_evidence": True,
    }
    assert len(json.dumps(compact, ensure_ascii=False, separators=(",", ":"))) < 32_000
    assert len(documents[0]["content"]) > 1_000


def test_twenty_stock_batch_isolates_citations_and_withdraws_unverifiable_conclusions():
    required = ["NEWS", "NOTICE", "FINANCIAL", "PRICE_VOLUME", "FUND_FLOW", "COMPANY", "GRAPH"]
    pool = [({"symbol": f"DEMO{index:03}", "market": "CN_A", "analysis_json": {}, "evidence_json": {
        "documents": [{"id": index + 100, "source_table": "stock_news", "content_state": "TEXT_AVAILABLE",
                       "excerpt": "公司披露新增订单，预计改善收入。"}],
        "document_ids": [index + 100], "facts": [], "evidence_fact_ids": [], "chunks": [], "graph_paths": [],
    }}, 1.0) for index in range(20)]
    run = SimpleNamespace(model_instance_code=None, analysis_mode="MODEL_REQUESTED", error_message=None,
                          criteria_json={"analysis_contract": "MULTI_SOURCE_V2"}, data_mode="MOCK")
    calls = []

    def chat(**kwargs):
        calls.append(kwargs)
        text = kwargs["messages"][-1]["content"]
        batch = json.loads(text[text.index("["):])
        assert len(batch) <= 5
        rows = []
        for item in batch:
            allowed = item["evidence_json"]["citation_allowlist"]
            assert allowed["symbol"] == item["symbol"]
            assert allowed["document_ids"] == item["evidence_json"]["document_ids"]
            reference = allowed["document_ids"][0]
            # Persistently cite a peer's evidence on one stock, even after the
            # directed retry. Its unsupported thesis must never leak through.
            if item["symbol"] == "DEMO000":
                reference = 101
            rows.append({"symbol": item["symbol"], "decision": "WATCH", "confidence": .8,
                "reasoning": "订单增长支持关注", "target_price": 12, "stop_price": 9,
                "horizon_sessions": 10, "evidence_document_ids": [reference],
                "dimensions": [{"dimension": dimension,
                    "status": "SUPPORTED" if dimension == "NEWS" else "INSUFFICIENT",
                    "direction": "POSITIVE" if dimension == "NEWS" else "UNKNOWN",
                    "summary": "新增订单" if dimension == "NEWS" else "本轮证据不足",
                    "evidence_document_ids": [reference] if dimension == "NEWS" else [],
                    "evidence_fact_ids": [], "evidence_relation_ids": []} for dimension in required],
                "events": [], "positives": ["订单增长"], "risks": [], "watch_conditions": []})
        return SimpleNamespace(id=len(calls), status="SUCCESS", instance_code="TEST_MODEL",
                               response_text=json.dumps({"candidates": rows}, ensure_ascii=False))

    with patch("app.services.model_hub.ModelHubService.chat", side_effect=chat):
        _apply_model_analysis(None, run, pool)

    assert len(calls) == 5  # four batches plus one directed repair
    assert run.analysis_mode == "MODEL_PARTIAL"
    repaired = pool[0][0]["analysis_json"]
    assert repaired["model_output_status"] == "SUCCESS"
    assert repaired["analysis_validation_status"] == "PARTIAL"
    assert repaired["model"]["evidence_document_ids"] == []
    assert repaired["model"]["target_price"] is None
    assert repaired["model"]["confidence"] is None
    assert repaired["model"]["positives"] == []
    assert repaired["model"]["dimensions"][0]["status"] == "INSUFFICIENT"
    assert repaired["evidence_validation_warnings"]
    assert [attempt["status"] for attempt in repaired["model_attempts"]] == ["INVALID", "SUCCESS"]
    assert all(item["analysis_json"]["model_output_status"] == "SUCCESS" for item, _ in pool[1:])
    assert all(item["analysis_json"]["model"]["evidence_document_ids"] == [index + 100]
               for index, (item, _) in enumerate(pool[1:], start=1))


def test_valid_chunk_document_reference_is_not_lost_when_stored_as_string():
    from app.services.selection import _sanitize_model_evidence_references, _validate_multi_source_output
    item = {"evidence_json": {"documents": [{"id": 48, "source_table": "stock_symbol", "excerpt": "经营资料"}],
                              "chunks": [{"id": "chunk-1", "document_id": "48", "text": "经营资料"}],
                              "facts": [], "evidence_fact_ids": [], "graph_paths": []}}
    output = {"dimensions": [{"dimension": "COMPANY", "status": "SUPPORTED", "direction": "NEUTRAL",
                              "summary": "仅有切片线索", "evidence_document_ids": [48]}],
              "events": [], "evidence_document_ids": [48]}
    _sanitize_model_evidence_references(output, item)
    _validate_multi_source_output(output, item, required=False)
    assert output["validation_status"] == "PASSED"
    assert output["evidence_document_ids"] == [48]


def test_untyped_chunk_cannot_be_promoted_to_company_evidence():
    from app.services.selection import _sanitize_model_evidence_references, _validate_multi_source_output
    item = {"evidence_json": {"documents": [], "chunks": [{"id": "chunk-1", "document_id": "48", "text": "经营资料"}],
                              "facts": [], "evidence_fact_ids": [], "graph_paths": []}}
    output = {"dimensions": [{"dimension": "COMPANY", "status": "SUPPORTED", "direction": "NEUTRAL",
                              "summary": "仅有无来源分类切片", "evidence_document_ids": [48]}],
              "events": [], "evidence_document_ids": [48]}
    _sanitize_model_evidence_references(output, item)
    _validate_multi_source_output(output, item, required=False)
    assert output["validation_status"] == "PARTIAL"
    assert output["dimensions"][0]["status"] == "INSUFFICIENT"
    assert output["dimensions"][0]["evidence_document_ids"] == []


def test_optional_event_quote_failure_does_not_fail_core_evidence_validation():
    from app.services.selection import _sanitize_model_evidence_references
    item = {"evidence_json": {"documents": [{
        "id": 49, "source_table": "stock_news", "content_state": "TEXT_AVAILABLE",
        "content": "公司披露新增订单，预计改善收入。",
    }], "facts": [], "evidence_fact_ids": [], "chunks": [], "graph_paths": []}}
    output = {
        "events": [{"document_id": 49, "source_type": "NEWS", "quote": "模型自行改写的句子",
                     "title": "订单", "direction": "POSITIVE", "mechanism": "收入改善",
                     "timing": "短期", "confidence": 0.6}],
        "event_timeline": [], "event_links": [], "validation_findings": [],
    }
    _sanitize_model_evidence_references(output, item)
    assert output["events"] == []
    assert output["validation_status"] == "PASSED"
    assert output["semantic_linkage_status"] == "PARTIAL"
    assert output["semantic_linkage_warnings"]
    assert not output.get("evidence_validation_warnings")
