from datetime import datetime, timedelta, timezone

from sqlalchemy import create_engine
from sqlalchemy.orm import Session

from app.db.base import Base
from app.models.ai_hub import KnowledgeBase, KnowledgeDocument
from app.models.lakehouse import DocumentChunkVersion, LakeDataset, LakeDatasetVersion, LakeLineageEvent, LakeObject
from app.models.market_data import DataSource, StockF10Cache, StockFinancialReport, StockKline, StockRealtimeQuote, StockSymbol
from app.services.graph_rag import build_selection_context


def test_context_contains_structured_observations_and_precise_lakehouse_lineage():
    engine = create_engine("sqlite://")
    Base.metadata.create_all(engine)
    now = datetime(2026, 9, 25, tzinfo=timezone.utc)
    with Session(engine) as db:
        source = DataSource(source_code="TEST_SOURCE", source_name="测试行情源", adapter_type="TEST")
        db.add(source)
        db.flush()
        db.add(StockSymbol(
            market="CN_A", symbol="000001", exchange="SZ", name="测试银行", source_id=source.id,
        ))
        db.add(StockFinancialReport(
            market="CN_A", symbol="000001", indicator="revenue", report_period="2026Q2",
            currency="CNY", data_json={"value": 123.4, "unit": "百万元"}, source_id=source.id,
            fetched_at=now,
        ))
        db.add(StockKline(
            market="CN_A", symbol="000001", period="daily", adjust="", trade_date="2026-09-24",
            open_price=10, high_price=11, low_price=9.5, close_price=10.8, volume=1000,
            amount=10800, turnover_rate=2.1, source_id=source.id, fetched_at=now,
        ))
        db.add(StockRealtimeQuote(
            market="CN_A", symbol="000001", quote_time="2026-09-25 10:00:00", current_price=10.9,
            previous_close_price=10.8, change_pct=0.93, volume=200, source_id=source.id, fetched_at=now,
        ))
        db.add(StockF10Cache(
            market="CN_A", symbol="000001", section="主要股东", source_id=source.id,
            payload_json={"第一大股东": "测试集团", "持股比例": 0.25}, fetched_at=now,
        ))

        kb = KnowledgeBase(kb_code="TEST_KB", kb_name="测试知识库", source_tables=["stock_news"])
        db.add(kb)
        db.flush()
        document = KnowledgeDocument(
            knowledge_base_id=kb.id, source_table="stock_news", source_record_id=1,
            market="CN_A", symbol="000001", title="测试新闻", content="测试新闻正文，包含可追溯证据。",
        )
        db.add(document)
        db.flush()
        chunk = DocumentChunkVersion(
            document_key=f"knowledge_document:{document.id}", document_id=str(document.id), chunk_index=0,
            chunk_version="TEST:1", content_hash="a" * 64, chunk_text="测试新闻切片",
            start_offset=0, end_offset=7, parser_version="TEST", embedding_model="TEST_EMBED",
            metadata_json={"section_title": "测试章节"},
        )
        db.add(chunk)
        db.flush()

        lake_object = LakeObject(
            object_uri="file:///tmp/test-stock-kline.parquet", layer="NORMALIZED",
            object_key="stock_kline/v1.parquet", content_hash="b" * 64,
            content_type="application/vnd.apache.parquet", source_table="stock_kline",
            metadata_json={"source_table": "stock_kline", "scope_pairs": [["CN_A", "000001"]]},
        )
        db.add(lake_object)
        db.flush()
        dataset = LakeDataset(
            dataset_code="pipeline_stock_kline_normalized", dataset_name="股票日线标准化数据",
            layer="NORMALIZED", format="PARQUET", current_version="v1",
        )
        db.add(dataset)
        db.flush()
        version = LakeDatasetVersion(
            dataset_id=dataset.id, version="v1", object_id=lake_object.id, row_count=1,
            quality_json={"passed": True},
        )
        db.add(version)
        db.add(LakeLineageEvent(
            batch_id="batch-1", upstream_type="SOURCE_TABLE", upstream_id="stock_kline",
            downstream_type="DATASET_VERSION", downstream_id="pipeline_stock_kline_normalized:v1",
            transformation="PARQUET_EXPORT", dataset_version="v1",
            metadata_json={"source_table": "stock_kline", "scope_pairs": [["CN_A", "000001"]]},
        ))
        db.add(LakeLineageEvent(
            batch_id="batch-2", upstream_type="LAKE_OBJECT", upstream_id=lake_object.id,
            downstream_type="DOCUMENT_CHUNK", downstream_id=chunk.id,
            transformation="DOCUMENT_CHUNKING", parser_version="TEST",
            metadata_json={"document_id": str(document.id)},
        ))
        db.commit()

        context = build_selection_context(
            db, "CN_A", "000001", as_of=now + timedelta(days=1), max_facts=10,
        )

        assert context["structured_observations"]["financial"]
        assert context["structured_observations"]["market"]
        assert context["structured_observations"]["realtime"]
        assert context["structured_observations"]["shareholder"]
        assert "stock_financial_report:1" in context["evidence_record_refs"]
        bounded = build_selection_context(db, "CN_A", "000001", as_of=now + timedelta(days=1), max_facts=1)
        assert "stock_financial_report:1" in bounded["evidence_fact_ids"]
        assert context["lakehouse_datasets"][0]["source_tables"] == ["stock_kline"]
        assert {row["batch_id"] for row in context["lineage"]} == {"batch-1", "batch-2"}
        assert context["chunks"][0]["section_title"] == "测试章节"
        assert context["context_version"].endswith("V2")
    engine.dispose()
