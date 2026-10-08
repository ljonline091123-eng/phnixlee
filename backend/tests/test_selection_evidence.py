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
        "document_ids": [index], "documents": [{"id": index, "excerpt": "x" * 12_000}],
    }}, 1.0) for index in range(20)]
    requests = []

    def chat(**kwargs):
        requests.append(kwargs)
        assert sum(len(message["content"]) for message in kwargs["messages"]) < 60_000
        assert kwargs["messages"][0]["role"] == "system"
        text = kwargs["messages"][-1]["content"]
        batch = json.loads(text[text.index("["):])
        assert len(batch) <= 5
        output = {"candidates": [{"symbol": row["symbol"], "decision": "WATCH", "confidence": .5,
            "reasoning": "Demo inference", "target_price": None, "stop_price": None,
            "horizon_sessions": 10, "evidence_document_ids": row["evidence_json"]["document_ids"]} for row in batch]}
        return SimpleNamespace(id=len(requests), status="SUCCESS", instance_code="DEMO_MODEL", response_text=json.dumps(output))

    with patch("app.services.model_hub.ModelHubService.chat", side_effect=chat):
        _apply_model_analysis(None, run, pool)
    assert len(requests) > 1
    assert run.analysis_mode == "MODEL_COMPLETED"
    assert all(row["analysis_json"]["model_output_status"] == "SUCCESS" for row, _ in pool)
    assert all(request["instance_code"] is None for request in requests)
