from datetime import datetime, timezone
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import Session

from app.db.base import Base
from app.db.session import get_db
from app.main import app
from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, KnowledgeGraph
from app.models.context_event import StockContextEvent
from app.models.market_data import DataSource, StockFinancialReport, StockKline, StockSymbol
from app.services.resource_hub import build_knowledge_graph


def test_graph_coverage_evidence_dedupe_metrics_anomaly_and_document_boundary():
    engine = create_engine("sqlite://", connect_args={"check_same_thread": False})
    Base.metadata.create_all(engine)
    try:
        with Session(engine) as db:
            source = DataSource(source_code="DEMO_GRAPH", source_name="Demo", adapter_type="MOCK")
            db.add(source)
            db.flush()
            db.add(StockSymbol(market="CN_A", symbol="DEMO001", name="Demo", exchange="DEMO", source_id=source.id))
            db.add_all([
                StockFinancialReport(market="CN_A", symbol="DEMO001", indicator="revenue", report_period="2024", data_json={"value": 100}, source_id=source.id),
                StockFinancialReport(market="CN_A", symbol="DEMO001", indicator="revenue", report_period="2025", data_json={"value": 120}, source_id=source.id),
            ])
            db.add_all([
                StockKline(market="CN_A", symbol="DEMO001", period="daily", adjust="", trade_date="2025-01-01", open_price=10, high_price=10, low_price=9, close_price=10, volume=100, source_id=source.id),
                StockKline(market="CN_A", symbol="DEMO001", period="daily", adjust="", trade_date="2025-01-02", open_price=10, high_price=10, low_price=9, close_price=10, volume=100, source_id=source.id),
                StockKline(market="CN_A", symbol="DEMO001", period="daily", adjust="", trade_date="2025-01-03", open_price=10, high_price=10, low_price=9, close_price=10, volume=100, source_id=source.id),
                StockKline(market="CN_A", symbol="DEMO001", period="daily", adjust="", trade_date="2025-01-04", open_price=10, high_price=10, low_price=9, close_price=10.9, volume=1000, source_id=source.id),
            ])
            long_text = "evidence" * 200
            db.add_all([
                StockContextEvent(market="CN_A", symbol="DEMO001", event_type="POLICY", title="Policy", content=long_text, source_name="Demo", url="https://example.invalid/1", published_at=datetime(2025, 1, 1, tzinfo=timezone.utc), content_hash="a" * 64),
                StockContextEvent(market="CN_A", symbol="DEMO001", event_type="POLICY", title="Policy", content=long_text, source_name="Demo", url="https://example.invalid/2", published_at=datetime(2025, 1, 1, tzinfo=timezone.utc), content_hash="b" * 64),
            ])
            kb = KnowledgeBase(kb_code="DEMO_KB", kb_name="Demo", source_tables=["stock_context_event", "stock_financial_report", "stock_kline", "stock_news"])
            db.add(kb)
            db.flush()
            graph = KnowledgeGraph(knowledge_base_id=kb.id, graph_code="DEMO_GRAPH", graph_name="Demo", source_tables=list(kb.source_tables))
            other = KnowledgeGraph(knowledge_base_id=kb.id, graph_code="DEMO_OTHER", graph_name="Other", source_tables=list(kb.source_tables))
            db.add_all([graph, other])
            db.commit()
            result = build_knowledge_graph(db, graph)
            assert result["coverage"]["complete"] is False
            assert "news_notices" in result["coverage"]["missing_categories"]
            assert result["coverage"]["stock_dimension_coverage"]["total_stock_count"] == 1
            assert result["coverage"]["duplicate_records_removed"] == 1
            assert result["entities_created"] > 0
            db.commit()
            client = TestClient(app)
            app.dependency_overrides[get_db] = lambda: db
            try:
                explored = client.get(f"/api/v1/resources/knowledge-graphs/{graph.id}/explore?q=DEMO001").json()
                assert any(item["type"] == "FINANCIAL_METRIC" for item in explored["nodes"])
                assert any(item["type"] == "MARKET_ANOMALY" for item in explored["nodes"])
                event_doc = next(doc for doc in db.query(KnowledgeDocument).filter_by(graph_id=graph.id).all() if doc.source_table == "stock_context_event")
                full = client.get(f"/api/v1/resources/knowledge-graphs/{graph.id}/documents/{event_doc.id}")
                assert full.status_code == 200
                assert full.json()["content"] == long_text
                wrong = client.get(f"/api/v1/resources/knowledge-graphs/{other.id}/documents/{event_doc.id}")
                assert wrong.status_code == 404
            finally:
                app.dependency_overrides.clear()
                client.close()
    finally:
        engine.dispose()
