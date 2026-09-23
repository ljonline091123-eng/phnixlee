from __future__ import annotations

import hashlib
import io
import json
import os
import re
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from uuid import uuid4

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.models.lakehouse import DocumentChunkVersion, LakeDataset, LakeDatasetVersion, LakeLineageEvent, LakeObject


def _sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def _safe(value: str) -> str:
    return re.sub(r"[^A-Za-z0-9._=-]+", "_", value)


def storage_mode() -> str:
    return get_settings().lake_storage_backend.lower()


def put_object(data: bytes, *, layer: str, content_type: str, source_code: str | None = None,
               source_table: str | None = None, source_record_id: str | None = None,
               dataset_version: str | None = None, metadata: dict[str, Any] | None = None,
               db: Session | None = None) -> dict[str, Any]:
    digest = _sha256(data)
    settings = get_settings()
    key = f"{layer.lower()}/{source_code or 'internal'}/{digest[:2]}/{digest}"
    backend = storage_mode()
    if backend == "minio":
        from minio import Minio
        client = Minio(settings.lake_s3_endpoint, access_key=settings.lake_s3_access_key,
                       secret_key=settings.lake_s3_secret_key, secure=settings.lake_s3_secure)
        bucket = settings.lake_s3_bucket
        if not client.bucket_exists(bucket):
            client.make_bucket(bucket)
        client.put_object(bucket, key, io.BytesIO(data), len(data), content_type=content_type)
        uri = f"s3://{bucket}/{key}"
    else:
        root = Path(settings.lake_filesystem_root)
        path = root / key
        path.parent.mkdir(parents=True, exist_ok=True)
        if not path.exists():
            path.write_bytes(data)
        bucket, uri = None, f"file://{path.resolve()}"
    result = {"object_uri": uri, "content_hash": digest, "byte_size": len(data), "layer": layer,
              "bucket": bucket, "object_key": key, "content_type": content_type}
    if db is not None:
        existing = db.scalar(select(LakeObject).where(LakeObject.content_hash == digest))
        if existing is None:
            existing = LakeObject(**result, source_code=source_code, source_table=source_table,
                source_record_id=str(source_record_id) if source_record_id is not None else None,
                dataset_version=dataset_version, metadata_json=metadata or {})
            db.add(existing)
            db.flush()
        result["object_id"] = existing.id
    return result


def _records_for_table(db: Session, table_name: str, limit: int) -> list[dict[str, Any]]:
    from app.models import market_data, foundation
    models = {"stock_symbol": market_data.StockSymbol, "stock_kline": market_data.StockKline,
              "stock_news": market_data.StockNews, "stock_notice": market_data.StockNotice,
              "stock_financial_report": market_data.StockFinancialReport,
              "foundation_entity": foundation.FoundationEntity,
              "foundation_fact": foundation.FoundationFact}
    model = models.get(table_name)
    if model is None:
        raise ValueError(f"不支持导出数据表: {table_name}")
    rows = db.scalars(select(model).limit(limit)).all()
    return [{column.name: getattr(row, column.name) for column in model.__table__.columns} for row in rows]


def export_dataset(db: Session, *, dataset_code: str, dataset_name: str, layer: str,
                   source_table: str, limit: int = 10000) -> dict[str, Any]:
    records = _records_for_table(db, source_table, min(limit, 100000))
    try:
        import pyarrow as pa
        import pyarrow.parquet as pq
    except ImportError as exc:
        raise RuntimeError("Parquet 导出需要安装 pyarrow") from exc
    normalized = [{key: (value.isoformat() if isinstance(value, datetime) else value) for key, value in row.items()} for row in records]
    table = pa.Table.from_pylist(normalized)
    buffer = io.BytesIO(); pq.write_table(table, buffer, compression="zstd")
    version = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    obj = put_object(buffer.getvalue(), layer=layer, content_type="application/vnd.apache.parquet",
                     source_table=source_table, dataset_version=version, db=db,
                     metadata={"dataset_code": dataset_code, "source_table": source_table})
    dataset = db.scalar(select(LakeDataset).where(LakeDataset.dataset_code == dataset_code))
    if dataset is None:
        dataset = LakeDataset(dataset_code=dataset_code, dataset_name=dataset_name, layer=layer, format="PARQUET")
        db.add(dataset); db.flush()
    dataset.current_version = version
    db.add(LakeDatasetVersion(dataset_id=dataset.id, version=version, object_id=obj.get("object_id"),
        row_count=len(normalized), schema_json={field.name: str(field.type) for field in table.schema},
        quality_json={"source_table": source_table, "content_hash": obj["content_hash"]}))
    batch_id = str(uuid4())
    db.add(LakeLineageEvent(batch_id=batch_id, upstream_type="SOURCE_TABLE", upstream_id=source_table,
        downstream_type="DATASET_VERSION", downstream_id=f"{dataset_code}:{version}",
        transformation="PARQUET_EXPORT", dataset_version=version))
    db.commit()
    return {"dataset_code": dataset_code, "version": version, "row_count": len(normalized), **obj}


def create_chunks(db: Session, *, document_key: str, text: str, document_id: str | None = None,
                  chunk_size: int = 1800, overlap: int = 180, parser_version: str = "TEXT_V1",
                  embedding_model: str | None = None) -> dict[str, Any]:
    text = text or ""
    step = max(1, chunk_size - overlap)
    chunks = []
    for index, start in enumerate(range(0, len(text), step)):
        end = min(len(text), start + chunk_size)
        chunk = text[start:end].strip()
        if not chunk: continue
        digest = _sha256(chunk.encode("utf-8"))
        item = DocumentChunkVersion(document_key=document_key, document_id=document_id, chunk_index=index,
            chunk_version=f"{parser_version}:{digest[:12]}", content_hash=digest, chunk_text=chunk,
            start_offset=start, end_offset=end, parser_version=parser_version,
            embedding_model=embedding_model, metadata_json={"overlap": overlap})
        db.add(item); chunks.append(item)
    db.commit()
    return {"document_key": document_key, "chunk_count": len(chunks), "parser_version": parser_version,
            "content_hash": _sha256(text.encode("utf-8"))}
