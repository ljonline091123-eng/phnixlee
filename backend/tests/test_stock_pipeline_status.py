import unittest
from datetime import datetime, timezone

from sqlalchemy import create_engine
from sqlalchemy.orm import Session

from app.api.stocks import _stock_pipeline_statuses
from app.db.base import Base
from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, KnowledgeEntity, KnowledgeGraph, KnowledgeRelation
from app.models.lakehouse import DocumentChunkVersion
from app.models.market_data import (
    DataSource,
    StockF10Cache,
    StockFinancialReport,
    StockKline,
    StockNews,
    StockNotice,
    StockRealtimeQuote,
    StockSymbol,
)


class StockPipelineStatusTest(unittest.TestCase):
    def setUp(self) -> None:
        self.engine = create_engine("sqlite://")
        Base.metadata.create_all(self.engine)
        self.db = Session(self.engine)
        now = datetime.now(timezone.utc)
        source = DataSource(
            source_code="TEST_STATUS",
            source_name="状态测试来源",
            source_type="MARKET_DATA",
            adapter_type="TEST",
        )
        self.db.add(source)
        self.db.flush()
        self.stock = StockSymbol(
            market="CN_A",
            symbol="000001",
            exchange="SZSE",
            name="平安银行",
            asset_type="STOCK",
            status="LISTED",
            source_id=source.id,
            ext_json={},
            raw_payload={},
            last_synced_at=now,
        )
        self.db.add(self.stock)
        self.db.commit()
        self.source = source

    def tearDown(self) -> None:
        self.db.close()
        self.engine.dispose()

    def test_empty_security_is_not_started(self) -> None:
        summary = _stock_pipeline_statuses(self.db, [self.stock])[("CN_A", "000001")]
        self.assertEqual(summary["data_collection"]["status"], "NOT_STARTED")
        self.assertEqual(summary["knowledge_base"]["status"], "NOT_STARTED")
        self.assertEqual(summary["knowledge_graph"]["status"], "NOT_STARTED")
        self.assertEqual(summary["data_collection"]["expected_count"], 6)

    def test_status_uses_persisted_rows_and_chunks(self) -> None:
        now = datetime.now(timezone.utc)
        self.db.add_all([
            StockRealtimeQuote(market="CN_A", symbol="000001", source_id=self.source.id, fetched_at=now),
            StockKline(market="CN_A", symbol="000001", source_id=self.source.id, trade_date="2026-09-01", fetched_at=now),
            StockFinancialReport(market="CN_A", symbol="000001", source_id=self.source.id, indicator="revenue", report_period="2026Q2", fetched_at=now),
            StockNews(market="CN_A", symbol="000001", source_id=self.source.id, news_time="2026-09-01", title="测试新闻", fetched_at=now),
            StockNotice(market="CN_A", symbol="000001", source_id=self.source.id, notice_date="2026-09-01", title="测试公告", fetched_at=now),
            StockF10Cache(market="CN_A", symbol="000001", source_id=self.source.id, section="profile", payload_json={"name": "平安银行"}, fetched_at=now),
        ])
        kb = KnowledgeBase(kb_code="STATUS_KB", kb_name="状态测试知识库", source_tables=["stock_news"])
        self.db.add(kb)
        self.db.flush()
        graph = KnowledgeGraph(
            knowledge_base_id=kb.id,
            graph_code="STATUS_GRAPH",
            graph_name="状态测试图谱",
            source_tables=["stock_news"],
            governance_status="PENDING",
        )
        self.db.add(graph)
        self.db.flush()
        document = KnowledgeDocument(
            knowledge_base_id=kb.id,
            graph_id=graph.id,
            source_table="stock_news",
            source_record_id=1,
            market="CN_A",
            symbol="000001",
            title="测试新闻",
            content="测试正文",
        )
        self.db.add(document)
        self.db.flush()
        self.db.add(DocumentChunkVersion(
            document_key="status:document:1",
            document_id=str(document.id),
            chunk_index=0,
            chunk_version="v1",
            content_hash="a" * 64,
            chunk_text="测试正文",
            parser_version="test",
        ))
        entity = KnowledgeEntity(
            knowledge_base_id=kb.id,
            graph_id=graph.id,
            entity_type="STOCK",
            entity_key="1:CN_A:000001",
            entity_name="000001",
        )
        self.db.add(entity)
        self.db.flush()
        self.db.add(KnowledgeRelation(
            knowledge_base_id=kb.id,
            graph_id=graph.id,
            subject_entity_id=entity.id,
            object_entity_id=entity.id,
            predicate="SELF_TEST",
        ))
        self.db.commit()

        summary = _stock_pipeline_statuses(self.db, [self.stock])[("CN_A", "000001")]
        self.assertEqual(summary["data_collection"]["status"], "COMPLETED")
        self.assertEqual(summary["knowledge_base"]["status"], "COMPLETED")
        self.assertEqual(summary["knowledge_graph"]["status"], "COMPLETED")
        self.assertEqual(summary["knowledge_base"]["detail"]["documents_with_chunks"], 1)
        self.assertEqual(summary["knowledge_graph"]["detail"]["relation_count"], 1)


if __name__ == "__main__":
    unittest.main()
