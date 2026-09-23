from datetime import datetime, timezone

from sqlalchemy import create_engine
from sqlalchemy.orm import Session
from sqlalchemy.pool import StaticPool

from app.db.base import Base
from app.models.market_data import DataSource, StockSymbol
from app.services import lakehouse


def test_filesystem_object_store_and_versioned_chunks(tmp_path, monkeypatch):
    engine = create_engine("sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool)
    Base.metadata.create_all(engine)
    monkeypatch.setenv("LAKE_STORAGE_BACKEND", "filesystem")
    monkeypatch.setenv("LAKE_FILESYSTEM_ROOT", str(tmp_path))
    from app.core.config import get_settings
    get_settings.cache_clear()
    with Session(engine) as db:
        result = lakehouse.put_object(b"raw document", layer="RAW", content_type="text/plain", db=db)
        assert result["object_uri"].startswith("file://")
        assert result["content_hash"]
        chunks = lakehouse.create_chunks(db, document_key="notice:1", document_id="1",
            text="甲公司发布公告。" * 500, chunk_size=300, overlap=20, parser_version="TEXT_V2")
        assert chunks["chunk_count"] > 1
        assert db.query(lakehouse.DocumentChunkVersion).count() == chunks["chunk_count"]
