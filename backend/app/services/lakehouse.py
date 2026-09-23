from __future__ import annotations

import hashlib
import io
import json
import re
from datetime import date, datetime, timezone
from pathlib import Path
from typing import Any
from uuid import uuid4

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.models.lakehouse import DocumentChunkVersion, LakeDataset, LakeDatasetVersion, LakeLineageEvent, LakeObject


SUPPORTED_EXPORT_TABLES = {
    "stock_symbol", "stock_realtime_quote", "stock_kline", "stock_news", "stock_notice",
    "stock_financial_report", "stock_f10_cache", "stock_context_event", "research_report",
    "knowledge_document", "foundation_entity", "foundation_security", "foundation_listing",
    "foundation_evidence", "foundation_fact", "foundation_security_classification",
}
SERVING_EXPORT_TABLES = {
    "stock_symbol", "foundation_entity", "foundation_security", "foundation_listing",
    "foundation_fact", "foundation_security_classification",
}


def _sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def _safe(value: str) -> str:
    return re.sub(r"[^A-Za-z0-9._=-]+", "_", value)


def _json_value(value: Any) -> Any:
    if isinstance(value, (datetime, date)):
        return value.isoformat()
    if isinstance(value, bytes):
        return value.hex()
    return value


def storage_mode() -> str:
    backend = get_settings().lake_storage_backend.lower().strip()
    if backend not in {"filesystem", "minio"}:
        raise RuntimeError(f"不支持的湖仓存储后端: {backend}")
    return backend


def _minio_client():
    from minio import Minio
    settings = get_settings()
    return Minio(settings.lake_s3_endpoint, access_key=settings.lake_s3_access_key,
                 secret_key=settings.lake_s3_secret_key, secure=settings.lake_s3_secure)


def storage_health() -> dict[str, Any]:
    backend = storage_mode()
    settings = get_settings()
    try:
        if backend == "minio":
            client = _minio_client()
            bucket_exists = client.bucket_exists(settings.lake_s3_bucket)
            healthy = True
            detail = "存储桶已连接" if bucket_exists else "MinIO 可连接，存储桶将在首次写入时创建"
        else:
            root = Path(settings.lake_filesystem_root)
            root.mkdir(parents=True, exist_ok=True)
            probe = root / ".lakehouse-write-probe"
            probe.write_bytes(b"ok")
            probe.unlink(missing_ok=True)
            healthy, detail = True, f"本地目录可写: {root.resolve()}"
    except Exception as exc:
        return {"healthy": False, "detail": str(exc)}
    return {"healthy": healthy, "detail": detail}


def put_object(data: bytes, *, layer: str, content_type: str, source_code: str | None = None,
               source_table: str | None = None, source_record_id: str | None = None,
               dataset_version: str | None = None, metadata: dict[str, Any] | None = None,
               db: Session | None = None) -> dict[str, Any]:
    layer = layer.upper()
    if layer not in {"RAW", "NORMALIZED", "SERVING"}:
        raise ValueError("数据分层必须是 RAW、NORMALIZED 或 SERVING")
    digest = _sha256(data)
    settings = get_settings()
    key = f"{layer.lower()}/{_safe(source_code or source_table or 'internal')}/{digest[:2]}/{digest}"
    backend = storage_mode()
    if backend == "minio":
        client = _minio_client()
        bucket = settings.lake_s3_bucket
        if not client.bucket_exists(bucket):
            client.make_bucket(bucket)
        try:
            client.stat_object(bucket, key)
        except Exception:
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


def read_object(row: LakeObject) -> bytes:
    if row.object_uri.startswith("s3://"):
        response = _minio_client().get_object(row.bucket or get_settings().lake_s3_bucket, row.object_key or "")
        try:
            return response.read()
        finally:
            response.close()
            response.release_conn()
    if not row.object_uri.startswith("file://"):
        raise RuntimeError("对象地址不是受支持的存储 URI")
    path = Path(row.object_uri[7:]).resolve()
    root = Path(get_settings().lake_filesystem_root).resolve()
    if root != path and root not in path.parents:
        raise RuntimeError("对象地址超出湖仓存储目录")
    return path.read_bytes()


def _model_for_table(table_name: str):
    from app.models import ai_hub, company_graph, context_event, foundation, market_data
    models = {
        "stock_symbol": market_data.StockSymbol, "stock_realtime_quote": market_data.StockRealtimeQuote,
        "stock_kline": market_data.StockKline, "stock_news": market_data.StockNews,
        "stock_notice": market_data.StockNotice, "stock_financial_report": market_data.StockFinancialReport,
        "stock_f10_cache": market_data.StockF10Cache, "stock_context_event": context_event.StockContextEvent,
        "research_report": ai_hub.ResearchReportRecord, "knowledge_document": ai_hub.KnowledgeDocument,
        "foundation_entity": foundation.FoundationEntity, "foundation_security": foundation.FoundationSecurity,
        "foundation_listing": foundation.FoundationListing, "foundation_evidence": foundation.FoundationEvidence,
        "foundation_fact": foundation.FoundationFact,
        "foundation_security_classification": company_graph.SecurityClassification,
    }
    if table_name not in SUPPORTED_EXPORT_TABLES or table_name not in models:
        raise ValueError(f"不支持导出的数据表: {table_name}")
    return models[table_name]


def _records_for_table(db: Session, table_name: str, limit: int, layer: str) -> list[dict[str, Any]]:
    model = _model_for_table(table_name)
    query = select(model)
    if layer == "SERVING" and table_name in {"foundation_fact", "foundation_security_classification"}:
        query = query.where(model.status == "ACCEPTED")
    rows = db.scalars(query.order_by(*model.__table__.primary_key.columns).limit(limit)).all()
    return [{column.name: _json_value(getattr(row, column.name)) for column in model.__table__.columns} for row in rows]


def _version() -> str:
    return f"{datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')}-{uuid4().hex[:8]}"


def _quality(records: list[dict[str, Any]], schema: dict[str, str]) -> dict[str, Any]:
    null_counts = {name: sum(row.get(name) is None for row in records) for name in schema}
    checks = {"non_empty": len(records) > 0, "schema_present": bool(schema),
              "row_shape_consistent": all(set(row) == set(schema) for row in records)}
    return {"passed": all(checks.values()), "checks": checks, "null_counts": null_counts}


def archive_ingestion_payload(db: Session, *, source_code: str, log_type: str, log_id: int,
                              records: list[dict[str, Any]]) -> dict[str, Any]:
    """Archive a connector response without changing its online-table representation."""
    batch_id = str(uuid4())
    payload = json.dumps(records, ensure_ascii=False, sort_keys=True, default=str).encode("utf-8")
    obj = put_object(payload, layer="RAW", content_type="application/json", source_code=source_code,
        source_table=log_type, source_record_id=str(log_id), db=db,
        metadata={"batch_id": batch_id, "record_count": len(records)})
    db.add(LakeLineageEvent(batch_id=batch_id, upstream_type=log_type.upper(), upstream_id=str(log_id),
        downstream_type="LAKE_OBJECT", downstream_id=str(obj["object_id"]), transformation="RAW_ARCHIVE",
        metadata_json={"source_code": source_code, "record_count": len(records)}))
    db.commit()
    return {"batch_id": batch_id, "record_count": len(records), **obj}


def export_dataset(db: Session, *, dataset_code: str, dataset_name: str, layer: str,
                   source_table: str, limit: int = 10000) -> dict[str, Any]:
    layer = layer.upper()
    if layer == "SERVING" and source_table not in SERVING_EXPORT_TABLES:
        raise ValueError(f"{source_table} 未通过 Serving 层发布白名单")
    records = _records_for_table(db, source_table, min(limit, 100000), layer)
    try:
        import pyarrow as pa
        import pyarrow.parquet as pq
    except ImportError as exc:
        raise RuntimeError("Parquet 导出需要安装 pyarrow") from exc
    if not records:
        raise ValueError(f"来源表 {source_table} 没有可发布的数据")
    table = pa.Table.from_pylist(records)
    schema = {field.name: str(field.type) for field in table.schema}
    quality = _quality(records, schema)
    if not quality["passed"]:
        raise ValueError(f"数据质量门禁未通过: {quality['checks']}")
    buffer = io.BytesIO()
    if layer == "RAW":
        payload = b"\n".join(json.dumps(row, ensure_ascii=False, sort_keys=True).encode("utf-8") for row in records)
        content_type, output_format = "application/x-ndjson", "JSONL"
    else:
        pq.write_table(table, buffer, compression="zstd")
        payload, content_type, output_format = buffer.getvalue(), "application/vnd.apache.parquet", "PARQUET"
    version, batch_id = _version(), str(uuid4())
    obj = put_object(payload, layer=layer, content_type=content_type,
                     source_table=source_table, dataset_version=version, db=db,
                     metadata={"dataset_code": dataset_code, "source_table": source_table, "batch_id": batch_id})
    dataset = db.scalar(select(LakeDataset).where(LakeDataset.dataset_code == dataset_code))
    if dataset is None:
        dataset = LakeDataset(dataset_code=dataset_code, dataset_name=dataset_name, layer=layer, format=output_format)
        db.add(dataset)
        db.flush()
    dataset.dataset_name, dataset.layer, dataset.format, dataset.current_version = dataset_name, layer, output_format, version
    quality.update({"source_table": source_table, "content_hash": obj["content_hash"], "batch_id": batch_id})
    version_row = LakeDatasetVersion(dataset_id=dataset.id, version=version, object_id=obj.get("object_id"),
        row_count=len(records), schema_json=schema, quality_json=quality, status="PUBLISHED")
    db.add(version_row)
    db.add(LakeLineageEvent(batch_id=batch_id, upstream_type="SOURCE_TABLE", upstream_id=source_table,
        downstream_type="DATASET_VERSION", downstream_id=f"{dataset_code}:{version}",
        transformation="PARQUET_EXPORT", dataset_version=version,
        metadata_json={"row_count": len(records), "object_id": obj.get("object_id")}))
    db.commit()
    return {"dataset_id": dataset.id, "dataset_code": dataset_code, "version": version,
            "batch_id": batch_id, "row_count": len(records), "quality": quality, **obj}


def preview_dataset(db: Session, dataset_id: int, version: str | None = None, limit: int = 50) -> dict[str, Any]:
    dataset = db.get(LakeDataset, dataset_id)
    if dataset is None:
        raise ValueError("数据集不存在")
    target = version or dataset.current_version
    version_row = db.scalar(select(LakeDatasetVersion).where(
        LakeDatasetVersion.dataset_id == dataset_id, LakeDatasetVersion.version == target))
    if version_row is None or not version_row.object_id:
        raise ValueError("数据集版本不存在或没有存储对象")
    obj = db.get(LakeObject, version_row.object_id)
    if obj is None:
        raise ValueError("数据集对象目录记录不存在")
    data = read_object(obj)
    if dataset.format == "JSONL":
        rows = [json.loads(line) for line in data.decode("utf-8").splitlines()[:min(limit, 200)]]
    else:
        import pyarrow.parquet as pq
        table = pq.read_table(io.BytesIO(data)).slice(0, min(limit, 200))
        rows = [{key: _json_value(value) for key, value in row.items()} for row in table.to_pylist()]
    return {"dataset": dataset.dataset_code, "version": version_row.version, "row_count": version_row.row_count,
            "schema": version_row.schema_json, "quality": version_row.quality_json, "rows": rows}


def create_chunks(db: Session, *, document_key: str, text: str, document_id: str | None = None,
                  chunk_size: int = 1800, overlap: int = 180, parser_version: str = "TEXT_V1",
                  embedding_model: str | None = None, archive_original: bool = True) -> dict[str, Any]:
    text = text or ""
    if overlap >= chunk_size:
        raise ValueError("切片重叠长度必须小于切片长度")
    source_bytes = text.encode("utf-8")
    source_hash, batch_id = _sha256(source_bytes), str(uuid4())
    source_obj = None
    if archive_original:
        source_obj = put_object(source_bytes, layer="RAW", content_type="text/plain; charset=utf-8",
            source_code="knowledge_document", source_record_id=document_id, db=db,
            metadata={"document_key": document_key, "parser_version": parser_version, "batch_id": batch_id})
        db.add(LakeLineageEvent(batch_id=batch_id, upstream_type="KNOWLEDGE_DOCUMENT",
            upstream_id=str(document_id or document_key), downstream_type="LAKE_OBJECT",
            downstream_id=str(source_obj["object_id"]), transformation="RAW_ARCHIVE",
            parser_version=parser_version, metadata_json={"document_key": document_key}))
    step, created, reused = chunk_size - overlap, 0, 0
    for index, start in enumerate(range(0, len(text), step)):
        end = min(len(text), start + chunk_size)
        chunk = text[start:end].strip()
        if not chunk:
            continue
        digest = _sha256(chunk.encode("utf-8"))
        chunk_version = f"{parser_version}:{source_hash[:12]}"
        existing = db.scalar(select(DocumentChunkVersion).where(
            DocumentChunkVersion.document_key == document_key,
            DocumentChunkVersion.content_hash == digest,
            DocumentChunkVersion.chunk_index == index,
            DocumentChunkVersion.chunk_version == chunk_version))
        if existing is not None:
            reused += 1
            continue
        item = DocumentChunkVersion(document_key=document_key, document_id=document_id, chunk_index=index,
            chunk_version=chunk_version, content_hash=digest, chunk_text=chunk,
            start_offset=start, end_offset=end, parser_version=parser_version,
            embedding_model=embedding_model, metadata_json={"overlap": overlap, "source_object_id": (source_obj or {}).get("object_id")})
        db.add(item)
        db.flush()
        db.add(LakeLineageEvent(batch_id=batch_id, upstream_type="LAKE_OBJECT",
            upstream_id=str((source_obj or {}).get("object_id") or document_key), downstream_type="DOCUMENT_CHUNK",
            downstream_id=item.id, transformation="DOCUMENT_CHUNKING", parser_version=parser_version,
            metadata_json={"document_id": document_id, "chunk_index": index}))
        created += 1
    db.commit()
    return {"document_key": document_key, "chunk_count": created + reused, "created_count": created,
            "reused_count": reused, "parser_version": parser_version, "content_hash": source_hash,
            "object_id": (source_obj or {}).get("object_id"), "batch_id": batch_id}


def archive_knowledge_documents(db: Session, *, limit: int = 100, chunk_size: int = 1800,
                                overlap: int = 180, parser_version: str = "TEXT_V1") -> dict[str, Any]:
    from app.models.ai_hub import KnowledgeDocument
    documents = db.scalars(select(KnowledgeDocument).order_by(KnowledgeDocument.id).limit(min(limit, 1000))).all()
    archived, chunks, reused = 0, 0, 0
    for document in documents:
        result = create_chunks(db, document_key=f"knowledge_document:{document.id}", document_id=str(document.id),
            text=document.content or document.title, chunk_size=chunk_size, overlap=overlap,
            parser_version=parser_version, archive_original=True)
        archived += 1
        chunks += result["created_count"]
        reused += result["reused_count"]
    return {"document_count": archived, "created_chunk_count": chunks, "reused_chunk_count": reused}
