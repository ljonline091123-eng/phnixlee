from datetime import datetime, timezone
from unittest.mock import patch

from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import Session
from sqlalchemy.pool import StaticPool

from app.db.base import Base
from app.db.session import get_db
from app.main import app
from app.models.market_data import DataSource, StockKline, StockSymbol
from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, KnowledgeEntity, KnowledgeGraph, KnowledgeRelation
from app.models.selection import SelectionCandidate


def test_http_selection_review_tracking_contract():
    engine = create_engine("sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool)
    Base.metadata.create_all(engine)
    with Session(engine) as db:
        source = DataSource(source_code="DEMO", source_name="Demo", adapter_type="MOCK")
        db.add(source)
        db.flush()
        db.add(StockSymbol(market="CN_A", symbol="DEMO001", name="Demo", exchange="DEMO", source_id=source.id))
        for day, close in [("2025-01-03", 10), ("2025-01-06", 11)]:
            db.add(StockKline(market="CN_A", symbol="DEMO001", period="daily", adjust="",
                trade_date=day, close_price=close, volume=100, source_id=source.id))
        db.commit()
        app.dependency_overrides[get_db] = lambda: db
        client = TestClient(app)
        try:
            with patch("app.services.selection._now", return_value=datetime(2025, 1, 6, 10, tzinfo=timezone.utc)):
                response = client.post("/api/v1/selection/runs", json={"symbols": ["DEMO001"], "use_model": False, "data_mode": "MOCK"})
                assert response.status_code == 201, response.text
                run = response.json()
                item = run["candidates"][0]
                assert item["decision"] == "PENDING"
                stored = db.get(SelectionCandidate, item["id"])
                stored.evidence_json = {
                    **stored.evidence_json,
                    "documents": [{"id": "7", "title": "公告原文", "source_table": "stock_notice", "excerpt": "来源摘录"}],
                    "analysis_originals": {"7": {"content": "本轮保存的完整原文"}},
                    "graph_paths": [{"id": 11, "evidence_document_id": "9", "evidence_excerpt": "本轮图谱证据摘录"}],
                    "chunks": [{"id": "chunk-1", "document_id": "10", "text": "本轮切片内容"}],
                }
                db.commit()
                document_url = f"/api/v1/selection/candidates/{item['id']}/documents"
                proof = client.get(f"{document_url}/7")
                assert proof.status_code == 200, proof.text
                assert proof.json()["content"] == "本轮保存的完整原文"
                assert proof.json()["snapshot"] is True
                assert client.get(f"{document_url}/8").status_code == 404
                graph_proof = client.get(f"{document_url}/9")
                assert graph_proof.status_code == 200, graph_proof.text
                assert graph_proof.json()["content"] == "本轮图谱证据摘录"
                assert graph_proof.json()["content_state"] == "GRAPH_REFERENCE_ONLY"
                assert graph_proof.json()["content_truncated"] is True
                assert graph_proof.json()["snapshot"] is True
                chunk_proof = client.get(f"{document_url}/10")
                assert chunk_proof.status_code == 200, chunk_proof.text
                assert chunk_proof.json()["content"] == "本轮切片内容"
                assert chunk_proof.json()["content_state"] == "CHUNK_ONLY"
                assert client.post(f"/api/v1/selection/candidates/{item['id']}/review", json={"decision": "APPROVED"}).status_code == 422
                reviewed = client.post(f"/api/v1/selection/candidates/{item['id']}/review", json={"decision": "APPROVED", "target_price": 12})
                assert reviewed.status_code == 200, reviewed.text
                tracking_id = reviewed.json()["tracking_id"]
                details = client.get(f"/api/v1/selection/tracking/{tracking_id}").json()
                assert details["required_sessions"] >= 10 and details["snapshots"] == []
                assert details["confirmation_date"] == "2025-01-06"
                assert client.post("/api/v1/selection/refresh", json={"tracking_ids": [tracking_id]}).status_code == 200
                assert client.get(f"/api/v1/selection/runs/{run['id']}").json()["status"] == "TRACKING"
        finally:
            client.close()
            app.dependency_overrides.pop(get_db, None)
    engine.dispose()


def test_scope_catalog_compares_unselected_options_without_contaminating_selected_totals():
    engine = create_engine("sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool)
    Base.metadata.create_all(engine)
    with Session(engine) as db:
        base = KnowledgeBase(kb_code="SELECTED", kb_name="公司公告", source_tables=["stock_notice"])
        other = KnowledgeBase(kb_code="OTHER", kb_name="外部线索", source_tables=["stock_context_event"])
        db.add_all([base, other])
        db.flush()
        graph = KnowledgeGraph(knowledge_base_id=base.id, graph_code="SELECTED", graph_name="已选图谱", governance_status="GOVERNED")
        other_graph = KnowledgeGraph(knowledge_base_id=other.id, graph_code="OTHER", graph_name="待治理图谱", governance_status="PENDING")
        db.add_all([graph, other_graph])
        db.flush()
        db.add_all([
            KnowledgeDocument(knowledge_base_id=base.id, graph_id=graph.id, market="CN_A", symbol="000001",
                              source_table="stock_notice", title="公司公告", content="法定披露"),
            KnowledgeDocument(knowledge_base_id=other.id, graph_id=other_graph.id, market="CN_A", symbol="000001",
                              source_table="stock_context_event", title="政策事件", content="外部线索"),
        ])
        stock = KnowledgeEntity(knowledge_base_id=base.id, graph_id=graph.id, entity_type="STOCK",
                                entity_key=f"{graph.id}:CN_A:000001", entity_name="A股公司")
        hong_kong = KnowledgeEntity(knowledge_base_id=base.id, graph_id=graph.id, entity_type="STOCK",
                                    entity_key=f"{graph.id}:HK:000001", entity_name="港股公司")
        tail = KnowledgeEntity(knowledge_base_id=base.id, graph_id=graph.id, entity_type="COMPANY",
                               entity_key="COMPANY:1", entity_name="发行主体")
        db.add_all([stock, hong_kong, tail])
        db.flush()
        for entity in [stock, hong_kong]:
            db.add(KnowledgeRelation(knowledge_base_id=base.id, graph_id=graph.id,
                                    subject_entity_id=entity.id, object_entity_id=tail.id, predicate="ISSUED_BY"))
        db.commit()
        app.dependency_overrides[get_db] = lambda: db
        client = TestClient(app)
        try:
            with patch("app.jobs.governance_runner.wake_governance_runner"):
                response = client.post("/api/v1/selection/scope-preview", json={"market": "CN_A", "symbols": ["000001"],
                                       "knowledge_base_ids": [base.id], "graph_ids": [graph.id]})
                assert response.status_code == 200, response.text
                result = response.json()
                assert result["source_counts"] == {"stock_notice": 1}
                assert result["rows"][0]["knowledge_source_counts"] == {"stock_notice": 1}
                assert result["rows"][0]["relation_type_counts"] == {"ISSUED_BY": 1}
                assert len(result["knowledge_bases"]) == 2
                other_stats = next(row for row in result["knowledge_bases"] if row["id"] == other.id)
                assert other_stats["source_counts"] == {"stock_context_event": 1}
                assert other_stats["document_count"] == 1
                assert other_stats["source_explanations"]
                assert next(row for row in result["graphs"] if row["id"] == other_graph.id)["analysis_eligible"] is False
                assert result["ready"] is True
        finally:
            client.close()
            app.dependency_overrides.pop(get_db, None)
    engine.dispose()


def test_selection_preflight_is_read_only_and_reports_data_gate():
    engine = create_engine("sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool)
    Base.metadata.create_all(engine)
    with Session(engine) as db:
        source = DataSource(source_code="PREFLIGHT_DEMO", source_name="预检演示源", adapter_type="MOCK")
        db.add(source)
        db.flush()
        base = KnowledgeBase(kb_code="PREFLIGHT", kb_name="预检知识库", source_tables=["stock_notice"])
        db.add(base)
        db.flush()
        graph = KnowledgeGraph(knowledge_base_id=base.id, graph_code="PREFLIGHT", graph_name="预检图谱", governance_status="GOVERNED")
        db.add(graph)
        db.flush()
        db.add(StockSymbol(market="CN_A", symbol="PREF001", name="预检股票", exchange="DEMO", source_id=source.id))
        db.commit()
        app.dependency_overrides[get_db] = lambda: db
        client = TestClient(app)
        try:
            response = client.post("/api/v1/selection/preflight", json={
                "market": "CN_A", "symbols": ["PREF001"],
                "knowledge_base_ids": [base.id], "graph_ids": [graph.id],
                "period_months": 3, "use_model": False, "data_mode": "REAL",
            })
            assert response.status_code == 200, response.text
            result = response.json()
            assert result["model"]["status"] == "SKIPPED"
            assert result["data_ready"] is False
            assert result["data"]["missing_symbols"] == ["PREF001"]
            assert db.query(SelectionCandidate).count() == 0
            unavailable = client.post("/api/v1/selection/preflight", json={
                "market": "CN_A", "symbols": ["PREF001"],
                "knowledge_base_ids": [base.id], "graph_ids": [graph.id],
                "model_instance_code": "DOES_NOT_EXIST", "use_model": True, "data_mode": "REAL",
            })
            assert unavailable.status_code == 200, unavailable.text
            failed_model = unavailable.json()
            assert failed_model["model_ready"] is False
            assert failed_model["model"]["category"] == "MODEL_UNKNOWN"
            assert "更换模型" in failed_model["model"]["suggestion"]
            assert db.query(SelectionCandidate).count() == 0
        finally:
            client.close()
            app.dependency_overrides.pop(get_db, None)
    engine.dispose()
