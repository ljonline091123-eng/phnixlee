from __future__ import annotations

import hashlib
import io
import json
import math
import re
from datetime import date, datetime, timezone
from pathlib import Path
from typing import Any
from uuid import uuid4

from sqlalchemy import select, tuple_
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.models.lakehouse import DocumentChunkVersion, LakeDataset, LakeDatasetVersion, LakeLineageEvent, LakeObject


SUPPORTED_EXPORT_TABLES = {
    "stock_symbol", "stock_realtime_quote", "stock_kline", "stock_news", "stock_notice",
    "stock_financial_report", "stock_f10_cache", "stock_context_event", "research_report",
    "knowledge_document", "foundation_entity", "foundation_security", "foundation_listing",
    "foundation_evidence", "foundation_fact", "foundation_fact_evidence", "foundation_fact_review",
    "foundation_security_classification", "foundation_source_identity",
    "foundation_company_mapping_state", "classification_definition",
}
SERVING_EXPORT_TABLES = {
    "stock_symbol", "foundation_entity", "foundation_security", "foundation_listing",
    "foundation_fact", "foundation_fact_evidence", "foundation_security_classification",
    "classification_definition",
}

QUALITY_CONTRACT_VERSION = "LAKE_QUALITY_V2"
QUALITY_CONTRACTS: dict[str, dict[str, tuple[str, ...]]] = {
    "stock_symbol": {"required": ("market", "symbol", "exchange", "name", "source_id"),
                     "business_key": ("market", "symbol")},
    "stock_realtime_quote": {"required": ("market", "symbol", "source_id"),
                              "business_key": ("market", "symbol"),
                              "non_negative": ("current_price", "previous_close_price", "open_price", "high_price", "low_price", "volume", "amount", "turnover_rate")},
    "stock_kline": {"required": ("market", "symbol", "period", "trade_date", "source_id"),
                    "business_key": ("market", "symbol", "period", "adjust", "trade_date"),
                    "non_negative": ("open_price", "high_price", "low_price", "close_price", "volume", "amount", "turnover_rate")},
    "stock_financial_report": {"required": ("market", "symbol", "indicator", "report_period", "source_id", "data_json"),
                               "business_key": ("market", "symbol", "indicator", "report_period", "source_id")},
    "stock_notice": {"required": ("market", "symbol", "notice_date", "title", "source_id"),
                     "business_key": ("market", "symbol", "notice_date", "title", "source_id"),
                     "content_any": ("content_json", "url")},
    "stock_news": {"required": ("market", "symbol", "news_time", "title", "source_id"),
                   "business_key": ("market", "symbol", "news_time", "title", "source_id"),
                   "content_any": ("content", "content_json", "url")},
    "stock_f10_cache": {"required": ("market", "symbol", "section", "source_id", "payload_json"),
                        "business_key": ("market", "symbol", "section")},
    "stock_context_event": {"required": ("market", "symbol", "event_type", "title", "content"),
                            "business_key": ("market", "symbol", "event_type", "content_hash")},
    "research_report": {"required": ("market", "symbol", "title", "report_markdown"),
                        "business_key": ("id",), "content_any": ("report_markdown",)},
    "knowledge_document": {"required": ("knowledge_base_id", "source_table", "title", "content"),
                           "business_key": ("id",), "content_any": ("content",)},
    "foundation_entity": {"required": ("id", "entity_type", "name", "jurisdiction"), "business_key": ("id",)},
    "foundation_security": {"required": ("id", "entity_id", "share_class"), "business_key": ("id",)},
    "foundation_listing": {"required": ("id", "security_id", "market", "symbol"),
                           "business_key": ("market", "symbol")},
    "foundation_evidence": {"required": ("id", "entity_id", "source_name", "source_key", "title", "content", "content_hash"),
                            "business_key": ("id",), "content_any": ("content", "url")},
    "foundation_fact": {"required": ("id", "subject_entity_id", "fact_type", "status"), "business_key": ("id",)},
    "foundation_security_classification": {"required": ("id", "security_id", "dimension", "code", "status"), "business_key": ("id",)},
    "foundation_fact_evidence": {"required": ("fact_id", "evidence_id"), "business_key": ("fact_id", "evidence_id")},
    "foundation_fact_review": {"required": ("id", "fact_id", "decision", "reviewer"), "business_key": ("id",)},
    "foundation_source_identity": {"required": ("id", "entity_id", "namespace", "external_id", "entity_type"), "business_key": ("namespace", "external_id", "entity_type")},
    "foundation_company_mapping_state": {"required": ("stock_symbol_id", "status", "reason"), "business_key": ("stock_symbol_id",)},
    "classification_definition": {"required": ("id", "taxonomy", "dimension", "code", "label", "definition", "definition_version", "status"), "business_key": ("taxonomy", "dimension", "code", "definition_version")},
}


def _sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def _hash_embedding(text: str, dimensions: int = 32) -> list[float]:
    """Build a small deterministic local vector for the lightweight MVP.

    This is deliberately not presented as a semantic model embedding.  It is
    dependency-free, reproducible across machines, and sufficient to support
    smoke tests and deterministic lexical retrieval until a licensed embedding
    service is configured.
    """
    tokens = re.findall(r"[A-Za-z0-9_]+|[\u4e00-\u9fff]", (text or "").lower())
    vector = [0.0] * dimensions
    for token in tokens:
        digest = hashlib.sha256(token.encode("utf-8")).digest()
        index = int.from_bytes(digest[:4], "big") % dimensions
        sign = 1.0 if digest[4] & 1 else -1.0
        vector[index] += sign
    norm = math.sqrt(sum(value * value for value in vector))
    if norm:
        vector = [round(value / norm, 8) for value in vector]
    return vector


def _safe(value: str) -> str:
    return re.sub(r"[^A-Za-z0-9._=-]+", "_", value)


def _json_value(value: Any, *, serialize_nested: bool = False) -> Any:
    if isinstance(value, (datetime, date)):
        return value.isoformat()
    if isinstance(value, bytes):
        return value.hex()
    if serialize_nested and isinstance(value, (dict, list)):
        return json.dumps(value, ensure_ascii=False, sort_keys=True, default=str)
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
        logical_binding = {
            "layer": layer,
            "source_code": source_code,
            "source_table": source_table,
            "source_record_id": str(source_record_id) if source_record_id is not None else None,
            "dataset_version": dataset_version,
            "metadata": metadata or {},
        }
        existing = db.scalar(select(LakeObject).where(LakeObject.content_hash == digest))
        if existing is None:
            existing = LakeObject(**result, source_code=source_code, source_table=source_table,
                source_record_id=str(source_record_id) if source_record_id is not None else None,
                dataset_version=dataset_version,
                metadata_json={**(metadata or {}), "logical_bindings": [logical_binding]})
            db.add(existing)
            db.flush()
            result["catalog_reused"] = False
        elif existing.object_uri != uri and existing.object_uri.split(":", 1)[0] != uri.split(":", 1)[0]:
            # Content addressing makes this a safe storage migration: every
            # historical version still resolves to identical bytes.
            existing.object_uri = uri
            existing.bucket = bucket
            existing.object_key = key
            existing.content_type = content_type
            existing.byte_size = len(data)
            bindings = list((existing.metadata_json or {}).get("logical_bindings") or [])
            if not bindings:
                bindings.append({
                    "layer": existing.layer, "source_code": existing.source_code,
                    "source_table": existing.source_table, "source_record_id": existing.source_record_id,
                    "dataset_version": existing.dataset_version,
                    "metadata": existing.metadata_json or {},
                })
            bindings.append(logical_binding)
            existing.metadata_json = {
                **(existing.metadata_json or {}), "storage_migrated": True,
                "logical_bindings": bindings[-200:],
            }
            db.flush()
            result["catalog_reused"] = True
        else:
            # LakeObject is the physical content-addressed object.  A byte-for-
            # byte duplicate can legitimately belong to another layer/source;
            # retain that logical ownership instead of relabeling the original
            # catalog row or pretending the first source owns every reuse.
            bindings = list((existing.metadata_json or {}).get("logical_bindings") or [])
            if not bindings:
                bindings.append({
                    "layer": existing.layer, "source_code": existing.source_code,
                    "source_table": existing.source_table, "source_record_id": existing.source_record_id,
                    "dataset_version": existing.dataset_version,
                    "metadata": existing.metadata_json or {},
                })
            binding_key = json.dumps(logical_binding, ensure_ascii=False, sort_keys=True, default=str)
            if all(json.dumps(item, ensure_ascii=False, sort_keys=True, default=str) != binding_key for item in bindings):
                bindings.append(logical_binding)
                existing.metadata_json = {**(existing.metadata_json or {}), "logical_bindings": bindings[-200:]}
                db.flush()
            result.update({
                "object_uri": existing.object_uri,
                "bucket": existing.bucket,
                "object_key": existing.object_key,
                "content_type": existing.content_type,
                "catalog_reused": True,
            })
        result["object_id"] = existing.id
        result["logical_binding"] = logical_binding
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
    from app.models import ai_hub, company_graph, company_mapping, context_event, foundation, market_data, taxonomy
    models = {
        "stock_symbol": market_data.StockSymbol, "stock_realtime_quote": market_data.StockRealtimeQuote,
        "stock_kline": market_data.StockKline, "stock_news": market_data.StockNews,
        "stock_notice": market_data.StockNotice, "stock_financial_report": market_data.StockFinancialReport,
        "stock_f10_cache": market_data.StockF10Cache, "stock_context_event": context_event.StockContextEvent,
        "research_report": ai_hub.ResearchReportRecord, "knowledge_document": ai_hub.KnowledgeDocument,
        "foundation_entity": foundation.FoundationEntity, "foundation_security": foundation.FoundationSecurity,
        "foundation_listing": foundation.FoundationListing, "foundation_evidence": foundation.FoundationEvidence,
        "foundation_fact": foundation.FoundationFact, "foundation_fact_evidence": foundation.FoundationFactEvidence,
        "foundation_fact_review": foundation.FoundationFactReview,
        "foundation_security_classification": company_graph.SecurityClassification,
        "foundation_source_identity": company_graph.SourceIdentity,
        "foundation_company_mapping_state": company_mapping.CompanyMappingState,
        "classification_definition": taxonomy.ClassificationDefinition,
    }
    if table_name not in SUPPORTED_EXPORT_TABLES or table_name not in models:
        raise ValueError(f"不支持导出的数据表: {table_name}")
    return models[table_name]


def _records_for_table(db: Session, table_name: str, limit: int, layer: str,
                       scope_pairs: list[tuple[str, str]] | None = None) -> list[dict[str, Any]]:
    model = _model_for_table(table_name)
    query = select(model)
    normalized_scope = sorted({(str(market).upper(), str(symbol).upper()) for market, symbol in (scope_pairs or [])})
    scope_supported = hasattr(model, "market") and hasattr(model, "symbol")
    if normalized_scope and scope_supported:
        query = query.where(tuple_(model.market, model.symbol).in_(normalized_scope))
    if layer == "SERVING" and table_name in {"foundation_fact", "foundation_security_classification"}:
        query = query.where(model.status == "ACCEPTED")
    rows = db.scalars(query.order_by(*model.__table__.primary_key.columns).limit(limit)).all()
    records = [{column.name: _json_value(getattr(row, column.name), serialize_nested=layer != "RAW")
                for column in model.__table__.columns} for row in rows]
    if layer != "RAW" and table_name == "stock_kline":
        records = [row for row in records if "\"quality_status\": \"QUARANTINED\"" not in str(row.get("raw_payload"))]
    return records


def _version() -> str:
    return f"{datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')}-{uuid4().hex[:8]}"


def _blank(value: Any) -> bool:
    return value is None or isinstance(value, str) and not value.strip() or isinstance(value, (dict, list)) and not value


def _source_schema(table_name: str) -> dict[str, str]:
    """Use the declared source schema so malformed values cannot break assessment."""
    model = _model_for_table(table_name)
    return {column.name: str(column.type) for column in model.__table__.columns}


def _document_quality(text: str, metadata: dict[str, Any] | None = None) -> dict[str, Any]:
    metadata = metadata or {}
    stripped = (text or "").strip()
    replacement_count = stripped.count("\ufffd")
    checks = {
        "content_present": bool(stripped),
        "minimum_length": len(stripped) >= 40,
        "encoding_clean": replacement_count == 0,
        "not_declared_truncated": not bool(metadata.get("content_truncated")),
    }
    warnings = []
    if not checks["minimum_length"]:
        warnings.append("正文少于40个字符，可能只有标题或摘要")
    if not checks["encoding_clean"]:
        warnings.append(f"正文包含{replacement_count}个Unicode替换字符")
    if not checks["not_declared_truncated"]:
        warnings.append("来源正文已标记为截断")
    return {
        "passed": checks["content_present"] and checks["encoding_clean"],
        "checks": checks,
        "character_count": len(stripped),
        "replacement_character_count": replacement_count,
        "warnings": warnings,
    }


def _quality(records: list[dict[str, Any]], schema: dict[str, str], *, table_name: str,
             layer: str) -> dict[str, Any]:
    contract = QUALITY_CONTRACTS.get(table_name, {})
    required = contract.get("required", ())
    business_key = contract.get("business_key", ())
    non_negative = contract.get("non_negative", ())
    content_any = contract.get("content_any", ())
    null_counts = {name: sum(_blank(row.get(name)) for row in records) for name in schema}
    required_missing = {name: sum(_blank(row.get(name)) for row in records) for name in required}
    required_missing = {name: count for name, count in required_missing.items() if count}
    seen: set[tuple[str, ...]] = set()
    duplicate_keys = 0
    for row in records:
        if not business_key:
            break
        key = tuple(json.dumps(row.get(name), ensure_ascii=False, sort_keys=True, default=str) for name in business_key)
        if key in seen:
            duplicate_keys += 1
        seen.add(key)
    invalid_numeric = 0
    non_finite_numeric = 0
    for row in records:
        for name in non_negative:
            value = row.get(name)
            if value is None:
                continue
            try:
                number = float(value)
            except (TypeError, ValueError):
                invalid_numeric += 1
                continue
            if not math.isfinite(number):
                non_finite_numeric += 1
            elif number < 0:
                invalid_numeric += 1
    price_range_violations = 0
    if table_name in {"stock_kline", "stock_realtime_quote"}:
        for row in records:
            values = {name: row.get(name) for name in ("open_price", "high_price", "low_price", "close_price")}
            if any(value is None for value in values.values()):
                continue
            try:
                high, low = float(values["high_price"]), float(values["low_price"])
                if high < max(float(values["open_price"]), float(values["close_price"]), low) or low > min(float(values["open_price"]), float(values["close_price"]), high):
                    price_range_violations += 1
            except (TypeError, ValueError):
                price_range_violations += 1
    missing_content = sum(
        bool(content_any) and not any(not _blank(row.get(name)) for name in content_any)
        for row in records
    )
    traceable = sum(
        any(not _blank(row.get(name)) for name in ("source_id", "source_name", "source_table", "evidence_id", "url"))
        for row in records
    )
    checks = {
        "non_empty": len(records) > 0,
        "schema_present": bool(schema),
        "row_shape_consistent": all(set(row) == set(schema) for row in records),
        "required_fields_complete": not required_missing,
        "business_key_unique": duplicate_keys == 0,
        "numeric_values_valid": invalid_numeric == 0 and non_finite_numeric == 0,
        "price_range_consistent": price_range_violations == 0,
        "content_available": missing_content == 0,
    }
    hard_names = {"non_empty", "schema_present", "row_shape_consistent"}
    if layer != "RAW":
        hard_names.update({"required_fields_complete", "business_key_unique", "numeric_values_valid", "price_range_consistent"})
    if layer == "SERVING":
        hard_names.add("content_available")
    warnings = []
    if required_missing:
        warnings.append(f"必填字段缺失: {required_missing}")
    if duplicate_keys:
        warnings.append(f"发现{duplicate_keys}条重复业务键")
    if invalid_numeric or non_finite_numeric:
        warnings.append(f"发现{invalid_numeric + non_finite_numeric}个非法数值")
    if price_range_violations:
        warnings.append(f"发现{price_range_violations}条高低价区间不一致记录")
    if missing_content:
        warnings.append(f"发现{missing_content}条缺少正文或来源链接的记录")
    if records and traceable < len(records):
        warnings.append(f"有{len(records) - traceable}条记录缺少明确来源引用")
    issue_samples: list[dict[str, Any]] = []
    sample_seen: set[tuple[str, ...]] = set()
    for index, row in enumerate(records):
        issues = []
        missing = [name for name in required if _blank(row.get(name))]
        if missing:
            issues.append("REQUIRED_MISSING:" + ",".join(missing))
        if business_key:
            key = tuple(json.dumps(row.get(name), ensure_ascii=False, sort_keys=True, default=str) for name in business_key)
            if key in sample_seen:
                issues.append("DUPLICATE_BUSINESS_KEY")
            sample_seen.add(key)
        bad_numeric = []
        for name in non_negative:
            value = row.get(name)
            if value is None:
                continue
            try:
                if not math.isfinite(float(value)) or float(value) < 0:
                    bad_numeric.append(name)
            except (TypeError, ValueError):
                bad_numeric.append(name)
        if bad_numeric:
            issues.append("INVALID_NUMERIC:" + ",".join(bad_numeric))
        if content_any and not any(not _blank(row.get(name)) for name in content_any):
            issues.append("CONTENT_MISSING")
        if issues and len(issue_samples) < 20:
            issue_samples.append({
                "row_index": index,
                "identity": {name: row.get(name) for name in ("id", "market", "symbol", "title", "source_id") if name in row},
                "issues": issues,
            })
    passed = all(checks[name] for name in hard_names)
    return {
        "passed": passed,
        "level": "PASS" if passed and not warnings else "WARNING" if passed else "FAILED",
        "contract_version": QUALITY_CONTRACT_VERSION,
        "table_name": table_name,
        "layer": layer,
        "row_count": len(records),
        "checks": checks,
        "hard_checks": sorted(hard_names),
        "null_counts": null_counts,
        "required_missing": required_missing,
        "duplicate_business_key_count": duplicate_keys,
        "invalid_numeric_count": invalid_numeric + non_finite_numeric,
        "price_range_violation_count": price_range_violations,
        "missing_content_count": missing_content,
        "traceable_record_count": traceable,
        "warnings": warnings,
        "issue_samples": issue_samples,
    }


def assess_dataset_source(db: Session, *, source_table: str, layer: str = "NORMALIZED",
                          limit: int = 10000,
                          scope_pairs: list[tuple[str, str]] | None = None) -> dict[str, Any]:
    layer = layer.upper()
    if layer == "SERVING" and source_table not in SERVING_EXPORT_TABLES:
        raise ValueError(f"{source_table} 未通过 Serving 层发布白名单")
    records = _records_for_table(
        db, source_table, min(limit, 100000), layer, scope_pairs,
    )
    if not records:
        return _quality([], {}, table_name=source_table, layer=layer)
    return _quality(records, _source_schema(source_table), table_name=source_table, layer=layer)


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
                   source_table: str, limit: int = 10000,
                   scope_pairs: list[tuple[str, str]] | None = None) -> dict[str, Any]:
    layer = layer.upper()
    if layer == "SERVING" and source_table not in SERVING_EXPORT_TABLES:
        raise ValueError(f"{source_table} 未通过 Serving 层发布白名单")
    records = _records_for_table(db, source_table, min(limit, 100000), layer, scope_pairs)
    try:
        import pyarrow as pa
        import pyarrow.parquet as pq
    except ImportError as exc:
        raise RuntimeError("Parquet 导出需要安装 pyarrow") from exc
    if not records:
        raise ValueError(f"来源表 {source_table} 没有可发布的数据")
    schema = _source_schema(source_table)
    quality = _quality(records, schema, table_name=source_table, layer=layer)
    if not quality["passed"]:
        raise ValueError(f"数据质量门禁未通过: {quality['checks']}")
    buffer = io.BytesIO()
    if layer == "RAW":
        payload = b"\n".join(json.dumps(row, ensure_ascii=False, sort_keys=True).encode("utf-8") for row in records)
        content_type, output_format = "application/x-ndjson", "JSONL"
    else:
        table = pa.Table.from_pylist(records)
        schema = {field.name: str(field.type) for field in table.schema}
        pq.write_table(table, buffer, compression="zstd")
        payload, content_type, output_format = buffer.getvalue(), "application/vnd.apache.parquet", "PARQUET"
    version, batch_id = _version(), str(uuid4())
    obj = put_object(payload, layer=layer, content_type=content_type,
                     source_table=source_table, dataset_version=version, db=db,
                     metadata={"dataset_code": dataset_code, "source_table": source_table, "batch_id": batch_id,
                               "scope_pairs": scope_pairs or []})
    dataset = db.scalar(select(LakeDataset).where(LakeDataset.dataset_code == dataset_code))
    if dataset is None:
        dataset = LakeDataset(dataset_code=dataset_code, dataset_name=dataset_name, layer=layer, format=output_format)
        db.add(dataset)
        db.flush()
    dataset.dataset_name, dataset.layer, dataset.format, dataset.current_version = dataset_name, layer, output_format, version
    scope_applied = bool(scope_pairs) and hasattr(_model_for_table(source_table), "market") and hasattr(_model_for_table(source_table), "symbol")
    quality.update({"source_table": source_table, "content_hash": obj["content_hash"], "batch_id": batch_id,
                    "scope_pairs": scope_pairs or [], "scope_applied": scope_applied,
                    "scope_note": "按证券范围过滤" if scope_applied else ("公司主数据按全量快照导出" if scope_pairs else "未指定证券范围")})
    version_row = LakeDatasetVersion(dataset_id=dataset.id, version=version, object_id=obj.get("object_id"),
        row_count=len(records), schema_json=schema, quality_json=quality, status="PUBLISHED")
    db.add(version_row)
    db.add(LakeLineageEvent(batch_id=batch_id, upstream_type="SOURCE_TABLE", upstream_id=source_table,
        downstream_type="DATASET_VERSION", downstream_id=f"{dataset_code}:{version}",
        transformation="PARQUET_EXPORT", dataset_version=version,
        metadata_json={"row_count": len(records), "object_id": obj.get("object_id"),
                       "source_table": source_table, "scope_pairs": scope_pairs or [],
                       "scope_applied": scope_applied}))
    db.commit()
    return {"dataset_id": dataset.id, "dataset_code": dataset_code, "version": version,
            "status": version_row.status,
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


def _section_title(text: str, offset: int, end: int | None = None) -> str | None:
    heading = None
    pattern = re.compile(r"(?m)^(?:#{1,6}\s+|第[一二三四五六七八九十百]+[章节]\s*|[一二三四五六七八九十]+、|\d+(?:\.\d+)*[、.\s]+)([^\n]{1,100})$")
    for match in pattern.finditer(text, 0, offset + 1):
        heading = match.group(0).strip().lstrip("#").strip()
    nearby_end = min(len(text), end or offset, offset + 240)
    nearby = pattern.search(text, offset, nearby_end)
    if nearby is not None:
        heading = nearby.group(0).strip().lstrip("#").strip()
    return heading


def _chunk_spans(text: str, chunk_size: int, overlap: int) -> list[tuple[int, int, str]]:
    """Prefer structural boundaries while retaining deterministic source offsets."""
    spans: list[tuple[int, int, str]] = []
    start = 0
    length = len(text)
    while start < length:
        hard_end = min(length, start + chunk_size)
        end, boundary = hard_end, "DOCUMENT_END" if hard_end == length else "HARD_LIMIT"
        if hard_end < length:
            minimum = start + max(1, int(chunk_size * 0.55))
            window = text[minimum:hard_end]
            candidates: list[tuple[int, int, str]] = []
            for priority, pattern, label in (
                (4, r"\n\s*\n", "PARAGRAPH"),
                (3, r"\n", "LINE"),
                (2, r"[。！？!?；;]\s*", "SENTENCE"),
                (1, r"[，,：:]\s*", "CLAUSE"),
            ):
                for match in re.finditer(pattern, window):
                    candidates.append((minimum + match.end(), priority, label))
            if candidates:
                end, _priority, boundary = max(candidates, key=lambda item: (item[1], item[0]))
        left, right = start, end
        while left < right and text[left].isspace():
            left += 1
        while right > left and text[right - 1].isspace():
            right -= 1
        if right > left:
            spans.append((left, right, boundary))
        if end >= length:
            break
        next_start = max(start + 1, end - overlap)
        start = next_start
    return spans


def create_chunks(db: Session, *, document_key: str, text: str, document_id: str | None = None,
                  chunk_size: int = 1800, overlap: int = 180, parser_version: str = "STRUCTURE_V1",
                  embedding_model: str | None = None, archive_original: bool = True) -> dict[str, Any]:
    text = text or ""
    if overlap >= chunk_size:
        raise ValueError("切片重叠长度必须小于切片长度")
    source_bytes = text.encode("utf-8")
    source_hash, batch_id = _sha256(source_bytes), str(uuid4())
    document_quality = _document_quality(text)
    source_obj = None
    if archive_original:
        source_obj = put_object(source_bytes, layer="RAW", content_type="text/plain; charset=utf-8",
            source_code="knowledge_document", source_record_id=document_id, db=db,
            metadata={"document_key": document_key, "parser_version": parser_version, "batch_id": batch_id,
                      "quality": document_quality})
        db.add(LakeLineageEvent(batch_id=batch_id, upstream_type="KNOWLEDGE_DOCUMENT",
            upstream_id=str(document_id or document_key), downstream_type="LAKE_OBJECT",
            downstream_id=str(source_obj["object_id"]), transformation="RAW_ARCHIVE",
            parser_version=parser_version, metadata_json={"document_key": document_key}))
    created, reused = 0, 0
    spans = _chunk_spans(text, chunk_size, overlap)
    for index, (start, end, boundary_type) in enumerate(spans):
        chunk = text[start:end]
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
        embedding = _hash_embedding(chunk) if embedding_model else None
        item = DocumentChunkVersion(document_key=document_key, document_id=document_id, chunk_index=index,
            chunk_version=chunk_version, content_hash=digest, chunk_text=chunk,
            start_offset=start, end_offset=end, parser_version=parser_version,
            embedding_model=embedding_model, metadata_json={"overlap": overlap,
                "source_object_id": (source_obj or {}).get("object_id"),
                "chunk_method": "STRUCTURE_AWARE_V1", "boundary_type": boundary_type,
                "section_title": _section_title(text, start, end), "document_quality": document_quality,
                "embedding": {"model": embedding_model, "dimension": len(embedding), "vector": embedding,
                               "model_quality": "DETERMINISTIC_HASH"}
                if embedding is not None else None})
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
            "object_id": (source_obj or {}).get("object_id"), "batch_id": batch_id,
            "quality": document_quality, "chunk_method": "STRUCTURE_AWARE_V1"}


def archive_knowledge_documents(db: Session, *, limit: int = 100, chunk_size: int = 1800,
                                overlap: int = 180, parser_version: str = "STRUCTURE_V1") -> dict[str, Any]:
    from app.models.ai_hub import KnowledgeDocument
    documents = db.scalars(select(KnowledgeDocument).order_by(KnowledgeDocument.id).limit(min(limit, 1000))).all()
    archived, chunks, reused, warning_documents = 0, 0, 0, 0
    for document in documents:
        result = create_chunks(db, document_key=f"knowledge_document:{document.id}", document_id=str(document.id),
            text=document.content or document.title, chunk_size=chunk_size, overlap=overlap,
            parser_version=parser_version, archive_original=True)
        archived += 1
        chunks += result["created_count"]
        reused += result["reused_count"]
        warning_documents += int(bool(result["quality"].get("warnings")))
    return {"document_count": archived, "created_chunk_count": chunks, "reused_chunk_count": reused,
            "warning_document_count": warning_documents, "parser_version": parser_version,
            "chunk_method": "STRUCTURE_AWARE_V1"}
