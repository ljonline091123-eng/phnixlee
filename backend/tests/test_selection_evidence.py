from sqlalchemy import create_engine
from sqlalchemy.orm import Session
from types import SimpleNamespace
from unittest.mock import patch
import json

from app.db.base import Base
from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, KnowledgeEntity, KnowledgeGraph, KnowledgeRelation
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
        no_scope = _evidence(db, "CN_A", "DEMO001", [], [])
        assert no_scope["documents"] == [] and no_scope["relations"] == []
        kb_only = _evidence(db, "CN_A", "DEMO001", [base.id], [])
        assert kb_only["documents"] and kb_only["relations"] == []
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
