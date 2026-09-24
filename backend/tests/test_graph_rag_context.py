import hashlib
from datetime import datetime, timedelta, timezone

from sqlalchemy import create_engine
from sqlalchemy.orm import Session

from app.db.base import Base
from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, KnowledgeEntity, KnowledgeGraph, KnowledgeRelation
from app.models.foundation import (
    FoundationEntity, FoundationEvidence, FoundationFact, FoundationFactEvidence,
    FoundationFactReview, FoundationListing, FoundationSecurity,
)
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
            chunk_version=f"TEST:{hashlib.sha256(document.content.encode('utf-8')).hexdigest()[:12]}",
            content_hash="a" * 64, chunk_text="测试新闻切片",
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
            quality_json={"passed": True, "source_table": "stock_kline",
                          "scope_pairs": [["CN_A", "000001"]], "scope_applied": True},
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
        hk_object = LakeObject(
            object_uri="file:///tmp/test-hk-stock-kline.parquet", layer="NORMALIZED",
            object_key="stock_kline/v2.parquet", content_hash="c" * 64,
            content_type="application/vnd.apache.parquet", source_table="stock_kline",
            metadata_json={"source_table": "stock_kline", "scope_pairs": [["HK", "00001"]]},
            created_at=now + timedelta(hours=1),
        )
        db.add(hk_object)
        db.flush()
        dataset.current_version = "v2"
        db.add(LakeDatasetVersion(
            dataset_id=dataset.id, version="v2", object_id=hk_object.id, row_count=1,
            quality_json={"passed": True, "source_table": "stock_kline",
                          "scope_pairs": [["HK", "00001"]], "scope_applied": True},
            created_at=now + timedelta(hours=1),
        ))
        db.add(LakeLineageEvent(
            batch_id="batch-hk", upstream_type="SOURCE_TABLE", upstream_id="stock_kline",
            downstream_type="DATASET_VERSION", downstream_id="pipeline_stock_kline_normalized:v2",
            transformation="PARQUET_EXPORT", dataset_version="v2",
            metadata_json={"source_table": "stock_kline", "scope_pairs": [["HK", "00001"]],
                           "scope_applied": True, "object_id": hk_object.id},
            created_at=now + timedelta(hours=1),
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
        assert context["lakehouse_datasets"][0]["version"] == "v1"
        assert {row["batch_id"] for row in context["lineage"]} == {"batch-1", "batch-2"}
        assert context["chunks"][0]["section_title"] == "测试章节"
        assert context["chunk_coverage"]["coverage_ratio"] == 1.0
        assert context["chunk_coverage"]["context_inclusion_ratio"] == 1.0
        assert context["context_version"].endswith("V3")

        empty = build_selection_context(db, "CN_A", "999999", as_of=now + timedelta(days=1))
        assert empty["chunk_coverage"]["coverage_status"] == "NO_DOCUMENTS"
        assert empty["chunk_coverage"]["coverage_ratio"] is None
    engine.dispose()


def test_context_excludes_future_graph_relations_and_reports_prompt_chunk_budget():
    engine = create_engine("sqlite://")
    Base.metadata.create_all(engine)
    cutoff = datetime(2026, 9, 25, tzinfo=timezone.utc)
    past = cutoff - timedelta(days=1)
    future = cutoff + timedelta(days=1)
    with Session(engine) as db:
        kb = KnowledgeBase(kb_code="POINT_IN_TIME_KB", kb_name="时点测试库", source_tables=["stock_news"])
        db.add(kb)
        db.flush()
        graph = KnowledgeGraph(
            knowledge_base_id=kb.id, graph_code="POINT_IN_TIME_GRAPH", graph_name="时点测试图谱",
            source_tables=["stock_news"], governance_status="GOVERNED",
        )
        graph.created_at = past
        graph.updated_at = past
        db.add(graph)
        db.flush()
        stock = KnowledgeEntity(
            knowledge_base_id=kb.id, graph_id=graph.id, entity_type="STOCK",
            entity_key=f"{graph.id}:stock:CN_A:000001", entity_name="测试股票",
        )
        company = KnowledgeEntity(
            knowledge_base_id=kb.id, graph_id=graph.id, entity_type="COMPANY",
            entity_key=f"{graph.id}:company:CN_A:000001:issuer", entity_name="测试公司",
        )
        stock.created_at = company.created_at = past
        stock.updated_at = company.updated_at = past
        db.add_all([stock, company])
        db.flush()
        known_relation = KnowledgeRelation(
            knowledge_base_id=kb.id, graph_id=graph.id, subject_entity_id=stock.id,
            predicate="ISSUED_BY", object_entity_id=company.id,
        )
        known_relation.created_at = past
        known_relation.updated_at = past
        future_relation = KnowledgeRelation(
            knowledge_base_id=kb.id, graph_id=graph.id, subject_entity_id=stock.id,
            predicate="FUTURE_RELATION", object_entity_id=company.id,
        )
        future_relation.created_at = future
        future_relation.updated_at = future
        db.add_all([known_relation, future_relation])

        for index in range(2):
            content = f"第 {index + 1} 篇可追溯文档"
            document = KnowledgeDocument(
                knowledge_base_id=kb.id, graph_id=graph.id, source_table="stock_news",
                source_record_id=index + 1, market="CN_A", symbol="000001",
                title=f"测试新闻 {index + 1}", content=content,
            )
            document.created_at = past
            document.updated_at = past
            db.add(document)
            db.flush()
            chunk = DocumentChunkVersion(
                document_key=f"knowledge_document:{document.id}", document_id=str(document.id),
                chunk_index=0,
                chunk_version=f"TEST:{hashlib.sha256(content.encode('utf-8')).hexdigest()[:12]}",
                content_hash=hashlib.sha256(content.encode("utf-8")).hexdigest(),
                chunk_text=content, start_offset=0, end_offset=len(content), parser_version="TEST",
            )
            chunk.created_at = past
            db.add(chunk)
        db.commit()

        context = build_selection_context(
            db, "CN_A", "000001", knowledge_base_ids=[kb.id], graph_ids=[graph.id],
            as_of=cutoff, max_documents=2, max_chunks=1,
        )

        assert {row["predicate"] for row in context["graph_paths"]} == {"ISSUED_BY"}
        assert context["chunk_coverage"]["available_chunk_coverage_ratio"] == 1.0
        assert context["chunk_coverage"]["context_inclusion_ratio"] == 0.5
        assert any("上下文预算" in item for item in context["missing_data"])
    engine.dispose()


def test_context_uses_fact_review_state_at_the_historical_cutoff():
    engine = create_engine("sqlite://")
    Base.metadata.create_all(engine)
    cutoff = datetime(2026, 9, 25, tzinfo=timezone.utc)
    past = cutoff - timedelta(days=2)
    future = cutoff + timedelta(days=1)
    with Session(engine) as db:
        source = DataSource(source_code="POINT_IN_TIME", source_name="时点来源", adapter_type="TEST")
        source.created_at = source.updated_at = past
        db.add(source)
        db.flush()
        stock = StockSymbol(
            market="CN_A", symbol="000001", exchange="SZ", name="测试银行", source_id=source.id,
        )
        stock.created_at = stock.updated_at = past
        db.add(stock)
        db.flush()
        company = FoundationEntity(
            name="测试银行股份有限公司", entity_type="COMPANY", jurisdiction="CN",
            identifier_scheme="TEST", identifier_value="company-1", created_at=past,
        )
        db.add(company)
        db.flush()
        evidence = FoundationEvidence(
            entity_id=company.id, source_name="TEST", source_key="issuer-1", title="发行主体证据",
            content="可追溯原文", available_at=past, content_hash="d" * 64,
            fingerprint="e" * 64, version=1, created_at=past,
        )
        db.add(evidence)
        db.flush()
        security = FoundationSecurity(entity_id=company.id, created_at=past)
        db.add(security)
        db.flush()
        db.add(FoundationListing(
            security_id=security.id, stock_symbol_id=stock.id, market="CN_A", symbol="000001",
            name="测试银行", evidence_id=evidence.id, created_at=past,
        ))
        early = FoundationFact(
            fact_type="IN_INDUSTRY", title="截止日前已审核事实", subject_entity_id=company.id,
            properties_json={"taxonomy": "TEST", "taxonomy_version": "1"},
            status="ACCEPTED", created_at=past, updated_at=past,
        )
        late = FoundationFact(
            fact_type="IN_INDUSTRY", title="截止日后才审核事实", subject_entity_id=company.id,
            properties_json={"taxonomy": "TEST", "taxonomy_version": "1"},
            status="ACCEPTED", created_at=past, updated_at=future,
        )
        db.add_all([early, late])
        db.flush()
        db.add_all([
            FoundationFactEvidence(fact_id=early.id, evidence_id=evidence.id),
            FoundationFactEvidence(fact_id=late.id, evidence_id=evidence.id),
            FoundationFactReview(
                fact_id=early.id, previous_status="PENDING", decision="ACCEPTED",
                reason="截止日前审核", reviewer="TEST", created_at=past + timedelta(hours=1),
            ),
            FoundationFactReview(
                fact_id=late.id, previous_status="PENDING", decision="ACCEPTED",
                reason="截止日后审核", reviewer="TEST", created_at=future,
            ),
        ])
        db.commit()

        context = build_selection_context(db, "CN_A", "000001", as_of=cutoff, max_facts=10)
        titles = {row["title"] for row in context["facts"]}
        assert "截止日前已审核事实" in titles
        assert "截止日后才审核事实" not in titles
    engine.dispose()
