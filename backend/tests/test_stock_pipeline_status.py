import unittest
from datetime import datetime, timezone

from fastapi import HTTPException
from sqlalchemy import create_engine
from sqlalchemy.orm import Session

from app.api.stocks import _stock_pipeline_statuses, get_stock_pipeline_status_detail
from app.db.base import Base
from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, KnowledgeEntity, KnowledgeGraph, KnowledgeRelation
from app.models.lakehouse import DocumentChunkVersion
from app.models.market_data import (
    DataFetchLog,
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

    def test_detail_explains_partial_business_data_and_unchunked_documents(self) -> None:
        now = datetime.now(timezone.utc)
        self.db.add_all([
            StockRealtimeQuote(
                market="CN_A", symbol="000001", source_id=self.source.id,
                quote_time="2026-09-28 15:00:00", current_price=12.34,
                volume=123456, change_pct=1.25, fetched_at=now,
            ),
            StockNews(
                market="CN_A", symbol="000001", source_id=self.source.id,
                news_time="2026-09-28 10:00:00", title="测试新闻详情",
                content="用于核验阶段详情的新闻正文", source_name="状态测试来源",
                url="https://example.test/news/1", fetched_at=now,
            ),
            DataFetchLog(
                source_id=self.source.id, interface_code="STOCK_NOTICE_FETCH",
                market="CN_A", symbol="000001", status="FAILED",
                total_count=0, persisted_count=0, error_message="公告接口限流",
                started_at=now, completed_at=now,
            ),
        ])
        kb = KnowledgeBase(
            kb_code="DETAIL_KB", kb_name="详情测试知识库", source_tables=["stock_news"],
        )
        self.db.add(kb)
        self.db.flush()
        graph = KnowledgeGraph(
            knowledge_base_id=kb.id,
            graph_code="DETAIL_GRAPH",
            graph_name="详情测试图谱",
            source_tables=["stock_news"],
            governance_status="PENDING",
        )
        self.db.add(graph)
        self.db.flush()
        self.db.add(KnowledgeDocument(
            knowledge_base_id=kb.id,
            graph_id=graph.id,
            source_table="stock_news",
            source_record_id=1,
            market="CN_A",
            symbol="000001",
            title="尚未切片的新闻文档",
            content="该文档故意不创建切片，用于验证未完成原因。",
        ))
        self.db.commit()

        detail = get_stock_pipeline_status_detail(
            market="CN_A", symbol="000001", sample_limit=5, db=self.db,
        )
        data_stage = detail["stages"]["data_collection"]
        completed = {item["code"]: item for item in data_stage["completed_items"]}
        incomplete = {item["code"]: item for item in data_stage["incomplete_items"]}

        self.assertEqual(set(completed) | set(incomplete), {
            "QUOTE", "KLINE", "FINANCIAL", "NEWS", "NOTICE", "F10",
        })
        self.assertEqual(completed["QUOTE"]["record_count"], 1)
        self.assertIn("pipeline-status-detail", completed["QUOTE"]["verification_path"])
        self.assertEqual(completed["QUOTE"]["records"][0]["fields"]["当前价"], 12.34)
        self.assertEqual(completed["NEWS"]["records"][0]["source_url"], "https://example.test/news/1")
        self.assertEqual(incomplete["NOTICE"]["status"], "FAILED")
        self.assertIn("公告接口限流", incomplete["NOTICE"]["reason"])
        self.assertTrue(incomplete["NOTICE"]["action_hint"])

        kb_stage = detail["stages"]["knowledge_base"]
        kb_incomplete = {item["code"]: item for item in kb_stage["incomplete_items"]}
        self.assertIn("DOCUMENT_CHUNKS", kb_incomplete)
        self.assertEqual(kb_incomplete["DOCUMENT_CHUNKS"]["record_count"], 0)
        self.assertEqual(kb_incomplete["DOCUMENT_CHUNKS"]["expected_count"], 1)
        self.assertIn("未完成", kb_incomplete["DOCUMENT_CHUNKS"]["reason"])
        self.assertEqual(kb_stage["verification"]["documents_without_chunks"], 1)
        self.assertEqual(
            kb_stage["verification"]["incomplete_records"][0]["title"],
            "尚未切片的新闻文档",
        )

    def test_detail_returns_graph_verification_records_for_a_completed_graph(self) -> None:
        kb = KnowledgeBase(
            kb_code="GRAPH_DETAIL_KB", kb_name="图谱详情测试知识库", source_tables=["stock_symbol"],
        )
        self.db.add(kb)
        self.db.flush()
        graph = KnowledgeGraph(
            knowledge_base_id=kb.id,
            graph_code="GRAPH_DETAIL",
            graph_name="已治理详情图谱",
            source_tables=["stock_symbol"],
            governance_status="GOVERNED",
        )
        self.db.add(graph)
        self.db.flush()
        document = KnowledgeDocument(
            knowledge_base_id=kb.id,
            graph_id=graph.id,
            source_table="stock_symbol",
            source_record_id=self.stock.id,
            market="CN_A",
            symbol="000001",
            title="平安银行主数据",
            content="证券主数据证据",
        )
        self.db.add(document)
        stock_entity = KnowledgeEntity(
            knowledge_base_id=kb.id,
            graph_id=graph.id,
            entity_type="STOCK",
            entity_key="security:CN_A:000001",
            entity_name="平安银行",
        )
        company_entity = KnowledgeEntity(
            knowledge_base_id=kb.id,
            graph_id=graph.id,
            entity_type="COMPANY",
            entity_key="company:test-pab",
            entity_name="平安银行股份有限公司",
        )
        self.db.add_all([stock_entity, company_entity])
        self.db.flush()
        self.db.add(KnowledgeRelation(
            knowledge_base_id=kb.id,
            graph_id=graph.id,
            subject_entity_id=stock_entity.id,
            object_entity_id=company_entity.id,
            predicate="ISSUED_BY",
            evidence_document_id=document.id,
        ))
        self.db.commit()

        detail = get_stock_pipeline_status_detail(
            market="CN_A", symbol="000001", sample_limit=5, db=self.db,
        )
        graph_stage = detail["stages"]["knowledge_graph"]
        completed = {item["code"]: item for item in graph_stage["completed_items"]}
        self.assertEqual(set(completed), {"GRAPH_PROJECTION", "STOCK_ENTITY", "GRAPH_RELATIONS"})
        self.assertEqual(graph_stage["incomplete_items"], [])
        self.assertEqual(graph_stage["verification"]["governed_graph_count"], 1)
        self.assertEqual(graph_stage["verification"]["graph_count"], 1)
        self.assertGreaterEqual(len(graph_stage["verification"]["records"]), 3)
        self.assertEqual(
            graph_stage["verification"]["relations"][0]["fields"]["关系类型"],
            "ISSUED_BY",
        )
        self.assertIn(
            f"/api/v1/resources/knowledge-graphs/{graph.id}/explore",
            completed["GRAPH_PROJECTION"]["verification_path"],
        )

    def test_detail_rejects_invalid_scope_and_missing_stock(self) -> None:
        with self.assertRaises(HTTPException) as invalid_limit:
            get_stock_pipeline_status_detail(
                market="CN_A", symbol="000001", sample_limit=0, db=self.db,
            )
        self.assertEqual(invalid_limit.exception.status_code, 422)
        self.assertTrue(invalid_limit.exception.detail)

        with self.assertRaises(HTTPException) as missing_stock:
            get_stock_pipeline_status_detail(
                market="CN_A", symbol="999999", sample_limit=5, db=self.db,
            )
        self.assertEqual(missing_stock.exception.status_code, 404)
        self.assertTrue(missing_stock.exception.detail)


if __name__ == "__main__":
    unittest.main()
