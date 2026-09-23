from sqlalchemy import create_engine
from sqlalchemy.orm import Session
from sqlalchemy.pool import StaticPool

from app.core.config import get_settings
from app.db.base import Base
from app.models.ai_hub import KnowledgeBase, KnowledgeDocument
from app.models.market_data import DataSource, StockRealtimeQuote, StockSymbol
from app.services import lakehouse


def _engine():
    engine = create_engine("sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool)
    Base.metadata.create_all(engine)
    return engine


def _filesystem(monkeypatch, tmp_path):
    monkeypatch.setenv("LAKE_STORAGE_BACKEND", "filesystem")
    monkeypatch.setenv("LAKE_FILESYSTEM_ROOT", str(tmp_path))
    get_settings.cache_clear()


def test_filesystem_object_store_and_idempotent_versioned_chunks(tmp_path, monkeypatch):
    _filesystem(monkeypatch, tmp_path)
    with Session(_engine()) as db:
        result = lakehouse.put_object(b"raw document", layer="RAW", content_type="text/plain", db=db)
        assert result["object_uri"].startswith("file://")
        assert lakehouse.read_object(db.get(lakehouse.LakeObject, result["object_id"])) == b"raw document"
        first = lakehouse.create_chunks(db, document_key="notice:1", document_id="1",
            text="甲公司发布公告。" * 500, chunk_size=300, overlap=20, parser_version="TEXT_V2")
        second = lakehouse.create_chunks(db, document_key="notice:1", document_id="1",
            text="甲公司发布公告。" * 500, chunk_size=300, overlap=20, parser_version="TEXT_V2")
        assert first["created_count"] > 1
        assert second["created_count"] == 0
        assert second["reused_count"] == first["chunk_count"]
        assert db.query(lakehouse.LakeLineageEvent).count() == first["created_count"] + 2
        assert lakehouse.storage_health()["healthy"] is True


def test_export_quality_version_and_preview(tmp_path, monkeypatch):
    _filesystem(monkeypatch, tmp_path)
    with Session(_engine()) as db:
        source = DataSource(source_code="TEST", source_name="测试", source_type="TEST", adapter_type="TEST")
        db.add(source)
        db.flush()
        db.add(StockSymbol(market="CN_A", symbol="000001", exchange="SZSE", name="平安银行",
            asset_type="STOCK", status="LISTED", source_id=source.id,
            ext_json={"industry": "银行"}, raw_payload={"source_symbol": "000001"}))
        db.commit()
        exported = lakehouse.export_dataset(db, dataset_code="stock_master", dataset_name="股票主数据",
            layer="NORMALIZED", source_table="stock_symbol", limit=100)
        assert exported["quality"]["passed"] is True
        assert exported["batch_id"]
        preview = lakehouse.preview_dataset(db, exported["dataset_id"])
        assert preview["rows"][0]["symbol"] == "000001"
        assert preview["quality"]["checks"]["non_empty"] is True
        raw = lakehouse.export_dataset(db, dataset_code="stock_master_raw", dataset_name="股票主数据原始快照",
            layer="RAW", source_table="stock_symbol", limit=100)
        raw_preview = lakehouse.preview_dataset(db, raw["dataset_id"])
        assert raw_preview["rows"][0]["name"] == "平安银行"


def test_empty_dataset_is_not_published(tmp_path, monkeypatch):
    _filesystem(monkeypatch, tmp_path)
    with Session(_engine()) as db:
        try:
            lakehouse.export_dataset(db, dataset_code="empty", dataset_name="空数据",
                layer="NORMALIZED", source_table="stock_symbol")
        except ValueError as exc:
            assert "没有可发布的数据" in str(exc)
        else:
            raise AssertionError("empty dataset must fail quality gate")
        assert db.query(lakehouse.LakeDatasetVersion).count() == 0


def test_serving_layer_rejects_unreviewed_source(tmp_path, monkeypatch):
    _filesystem(monkeypatch, tmp_path)
    with Session(_engine()) as db:
        try:
            lakehouse.export_dataset(db, dataset_code="news_serving", dataset_name="新闻服务层",
                layer="SERVING", source_table="stock_news")
        except ValueError as exc:
            assert "发布白名单" in str(exc)
        else:
            raise AssertionError("unreviewed source must not enter serving layer")


def test_archive_knowledge_documents_builds_raw_objects_and_lineage(tmp_path, monkeypatch):
    _filesystem(monkeypatch, tmp_path)
    with Session(_engine()) as db:
        kb = KnowledgeBase(kb_code="TEST_KB", kb_name="测试知识库", source_tables=["stock_news"])
        db.add(kb)
        db.flush()
        db.add(KnowledgeDocument(knowledge_base_id=kb.id, source_table="stock_news",
            source_record_id=1, title="测试新闻", content="测试正文" * 200))
        db.commit()
        result = lakehouse.archive_knowledge_documents(db, limit=10, chunk_size=300, overlap=20)
        assert result["document_count"] == 1
        assert result["created_chunk_count"] > 1
        assert db.query(lakehouse.LakeObject).filter_by(layer="RAW").count() == 1
        assert db.query(lakehouse.LakeLineageEvent).count() == result["created_chunk_count"] + 1


def test_quality_contract_warns_in_raw_and_blocks_invalid_normalized_data(tmp_path, monkeypatch):
    _filesystem(monkeypatch, tmp_path)
    with Session(_engine()) as db:
        source = DataSource(source_code="TEST", source_name="Test", source_type="TEST", adapter_type="TEST")
        db.add(source)
        db.flush()
        db.add(StockRealtimeQuote(market="HK", symbol="00001", source_id=source.id,
            current_price=40, open_price=39, high_price=41, low_price=38,
            turnover_rate=-4.3, raw_payload={"source": "test"}))
        db.commit()
        raw = lakehouse.assess_dataset_source(db, source_table="stock_realtime_quote", layer="RAW")
        assert raw["passed"] is True and raw["level"] == "WARNING"
        assert raw["invalid_numeric_count"] == 1
        normalized = lakehouse.assess_dataset_source(db, source_table="stock_realtime_quote", layer="NORMALIZED")
        assert normalized["passed"] is False and normalized["level"] == "FAILED"
        assert normalized["issue_samples"][0]["issues"] == ["INVALID_NUMERIC:turnover_rate"]
        try:
            lakehouse.export_dataset(db, dataset_code="bad_quote", dataset_name="Bad quote",
                layer="NORMALIZED", source_table="stock_realtime_quote")
        except ValueError as exc:
            assert "quality" in str(exc).lower() or "质量" in str(exc)
        else:
            raise AssertionError("invalid normalized data must not be published")


def test_quality_assessment_reports_non_numeric_source_values_without_crashing(tmp_path, monkeypatch):
    _filesystem(monkeypatch, tmp_path)
    record = {name: None for name in lakehouse._source_schema("stock_realtime_quote")}
    record.update({
        "id": 1, "market": "HK", "symbol": "00001", "source_id": 1,
        "current_price": "-", "previous_close_price": 40, "open_price": 39,
        "high_price": 41, "low_price": 38,
        "volume": 100, "amount": 4000, "turnover_rate": 1.2,
    })
    records = [record]
    monkeypatch.setattr(lakehouse, "_records_for_table", lambda *args, **kwargs: records)
    with Session(_engine()) as db:
        raw = lakehouse.assess_dataset_source(db, source_table="stock_realtime_quote", layer="RAW")
        assert raw["passed"] is True and raw["level"] == "WARNING"
        assert raw["issue_samples"][0]["issues"] == ["INVALID_NUMERIC:current_price"]
        normalized = lakehouse.assess_dataset_source(
            db, source_table="stock_realtime_quote", layer="NORMALIZED"
        )
        assert normalized["passed"] is False and normalized["level"] == "FAILED"


def test_structure_aware_chunks_prefer_paragraph_boundaries_and_keep_offsets(tmp_path, monkeypatch):
    _filesystem(monkeypatch, tmp_path)
    text = "# 第一章\n" + "甲" * 170 + "。\n\n# 第二章\n" + "乙" * 170 + "。"
    with Session(_engine()) as db:
        result = lakehouse.create_chunks(db, document_key="report:1", document_id="1", text=text,
            chunk_size=300, overlap=20, parser_version="STRUCTURE_V1", archive_original=False)
        rows = db.query(lakehouse.DocumentChunkVersion).order_by(lakehouse.DocumentChunkVersion.chunk_index).all()
        assert result["chunk_method"] == "STRUCTURE_AWARE_V1"
        assert len(rows) == 2
        assert rows[0].metadata_json["boundary_type"] == "PARAGRAPH"
        assert rows[1].metadata_json["section_title"] == "第二章"
        assert all(text[row.start_offset:row.end_offset] == row.chunk_text for row in rows)
