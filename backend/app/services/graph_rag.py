"""Bounded, versioned context for model-assisted stock analysis.

The online tables and the legacy graph remain the source of truth.  This
module only projects records that were available at ``as_of`` into a compact
context contract.  It deliberately does not call a model and it never writes
entities or facts.  Callers can therefore use the result as an auditable
GraphRAG boundary for LLA/agent prompts.
"""

from __future__ import annotations

import hashlib
import json
from datetime import date, datetime, timezone
from typing import Any, Iterable

from sqlalchemy import or_, select
from sqlalchemy.orm import Session

from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, KnowledgeEntity, KnowledgeGraph, KnowledgeRelation
from app.models.foundation import (
    FoundationEntity,
    FoundationFact,
    FoundationFactEvidence,
    FoundationEvidence,
    FoundationListing,
    FoundationSecurity,
)
from app.models.lakehouse import DocumentChunkVersion, LakeDataset, LakeDatasetVersion, LakeLineageEvent, LakeObject
from app.models.market_data import (
    StockF10Cache,
    StockFinancialReport,
    StockKline,
    StockRealtimeQuote,
    StockSymbol,
)
from app.services.graph_identity import canonical_company_id, canonical_security_id, resolve_many
from app.services.foundation import facts_query


CONTEXT_VERSION = "GRAPH_RAG_CONTEXT_V3"


def _iso(value: Any) -> str | None:
    if value is None:
        return None
    if isinstance(value, (datetime, date)):
        return value.isoformat()
    return str(value)


def _aware(value: datetime | None) -> datetime | None:
    if value is None:
        return None
    return value.replace(tzinfo=timezone.utc) if value.tzinfo is None else value


def _known(value: Any, cutoff: datetime | None) -> bool:
    """Apply a conservative availability gate without assuming DB tz support."""
    if cutoff is None or value is None:
        return True
    if isinstance(value, str):
        try:
            value = datetime.fromisoformat(value.replace("Z", "+00:00"))
        except ValueError:
            return True
    if isinstance(value, date) and not isinstance(value, datetime):
        value = datetime.combine(value, datetime.min.time(), tzinfo=timezone.utc)
    if not isinstance(value, datetime):
        return True
    left, right = _aware(value), _aware(cutoff)
    return bool(left and right and left <= right)


def _date_known(value: Any, cutoff: datetime | None) -> bool:
    """Apply a calendar-date cutoff for exchange observations."""
    if cutoff is None or value in (None, ""):
        return True
    try:
        observed = date.fromisoformat(str(value)[:10])
    except ValueError:
        return True
    return observed <= cutoff.date()


def _hash(value: Any) -> str:
    payload = json.dumps(value, ensure_ascii=False, sort_keys=True, default=str, separators=(",", ":"))
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def _unique(values: Iterable[Any]) -> list[Any]:
    return list(dict.fromkeys(value for value in values if value not in (None, "")))


def _compact(value: Any, *, max_items: int = 24, max_chars: int = 1600) -> Any:
    """Keep source payloads useful to an LLA without copying whole raw rows."""
    if isinstance(value, str):
        return value if len(value) <= max_chars else value[:max_chars] + "..."
    if isinstance(value, dict):
        return {str(key): _compact(item, max_items=max_items, max_chars=max_chars)
                for key, item in list(value.items())[:max_items]}
    if isinstance(value, (list, tuple)):
        values = [_compact(item, max_items=max_items, max_chars=max_chars) for item in value[:max_items]]
        return values + ([f"... 共 {len(value)} 项"] if len(value) > max_items else [])
    return value


def _doc_payload(row: KnowledgeDocument, *, include_content: bool = True) -> dict[str, Any]:
    metadata = row.metadata_json or {}
    content = row.content or ""
    return {
        "id": row.id,
        "title": row.title,
        "source_table": row.source_table,
        "source_record_id": row.source_record_id,
        "market": row.market,
        "symbol": row.symbol,
        "graph_id": row.graph_id,
        "content": content[:1200] if include_content else None,
        "content_hash": _hash(content),
        "metadata": metadata,
        "updated_at": _iso(row.updated_at),
    }


def _identity(db: Session, market: str, symbol: str, cutoff: datetime | None) -> dict[str, Any]:
    pair = resolve_many(db, [(market, symbol)], known_at=cutoff).get((market.upper(), symbol.upper()), {})
    result = {
        "canonical_security_id": canonical_security_id(market, symbol),
        "canonical_company_id": pair.get("canonical_company_id"),
        "mapping_status": pair.get("mapping_status", "UNMAPPED"),
        "stock_symbol_id": pair.get("stock_symbol_id"),
        "security_id": pair.get("security_id"),
        "company_id": pair.get("company_id"),
        "listing_id": pair.get("listing_id"),
        "identity_version": pair.get("identity_version"),
    }
    stock = db.scalar(select(StockSymbol).where(StockSymbol.market == market, StockSymbol.symbol == symbol))
    if stock is not None:
        result["security"] = {
            "id": stock.id,
            "market": stock.market,
            "symbol": stock.symbol,
            "name": stock.name,
            "exchange": stock.exchange,
            "asset_type": stock.asset_type,
            "status": stock.status,
            "list_date": stock.list_date,
            "last_synced_at": _iso(stock.last_synced_at),
        }
    company_id = pair.get("company_id")
    if company_id:
        company = db.get(FoundationEntity, company_id)
        if company is not None and _known(company.created_at, cutoff):
            result["company"] = {
                "id": company.id,
                "canonical_id": canonical_company_id(company.id),
                "name": company.name,
                "entity_type": company.entity_type,
                "jurisdiction": company.jurisdiction,
                "identifier_scheme": company.identifier_scheme,
                "identifier_value": company.identifier_value,
                "properties": company.properties_json or {},
            }
    return result


def _structured_observations(
    db: Session,
    market: str,
    symbol: str,
    cutoff: datetime | None,
    limit: int,
) -> dict[str, Any]:
    """Project bounded source-table observations into the GraphRAG contract.

    These are facts backed by the normalized market tables, not model
    inferences.  Keeping a stable ``source_table:source_record_id`` reference
    lets a reviewer jump from an LLA claim back to the source row even when no
    legacy KnowledgeDocument was generated for that row.
    """
    limit = max(1, min(int(limit or 1), 50))
    facts: list[dict[str, Any]] = []
    by_category: dict[str, list[dict[str, Any]]] = {
        "financial": [], "market": [], "realtime": [], "shareholder": [],
    }
    source_record_refs: list[str] = []
    source_tables: set[str] = set()

    def ref(table: str, record_id: Any) -> str:
        return f"{table}:{record_id}"

    def add(category: str, row: Any, fact_type: str, title: str, properties: dict[str, Any], observed: Any) -> None:
        table = row.__tablename__
        record_ref = ref(table, row.id)
        item = {
            "id": record_ref,
            "fact_type": fact_type,
            "title": title,
            "source_table": table,
            "source_record_id": row.id,
            "source_id": getattr(row, "source_id", None),
            "observed_at": _iso(observed),
            "status": "SOURCE",
            "properties": _compact(properties),
            "evidence": [{
                "kind": "STRUCTURED_RECORD",
                "source_table": table,
                "source_record_id": row.id,
                "source_id": getattr(row, "source_id", None),
            }],
        }
        facts.append(item)
        by_category[category].append(item)
        source_record_refs.append(record_ref)
        source_tables.add(table)

    financial_rows = list(db.scalars(select(StockFinancialReport).where(
        StockFinancialReport.market == market,
        StockFinancialReport.symbol == symbol,
    ).order_by(StockFinancialReport.report_period.desc(), StockFinancialReport.fetched_at.desc()).limit(limit * 2)).all())
    for row in financial_rows:
        if not _known(row.fetched_at, cutoff):
            continue
        data = row.data_json or {}
        add("financial", row, "FINANCIAL_OBSERVATION", f"{symbol} {row.indicator} · {row.report_period}", {
            "indicator": row.indicator,
            "report_period": row.report_period,
            "currency": row.currency,
            "data": data,
            "url": row.url,
            "fact_only": True,
        }, row.fetched_at)

    kline_rows = list(db.scalars(select(StockKline).where(
        StockKline.market == market,
        StockKline.symbol == symbol,
        StockKline.period == "daily",
    ).order_by(StockKline.trade_date.desc(), StockKline.fetched_at.desc()).limit(max(40, limit * 2))).all())
    kline_rows = [row for row in kline_rows if _known(row.fetched_at, cutoff) and _date_known(row.trade_date, cutoff)]
    if kline_rows:
        latest = kline_rows[0]
        bars = [{key: getattr(row, key) for key in (
            "trade_date", "open_price", "high_price", "low_price", "close_price",
            "volume", "amount", "turnover_rate", "adjust",
        )} for row in kline_rows[:20]]
        add("market", latest, "MARKET_OBSERVATION", f"{symbol} 日线量价 · {latest.trade_date}", {
            "period": latest.period,
            "adjust": latest.adjust,
            "latest": bars[0],
            "recent_bars": bars,
            "window_size": len(bars),
            "fact_only": True,
        }, latest.fetched_at)

    quote_rows = list(db.scalars(select(StockRealtimeQuote).where(
        StockRealtimeQuote.market == market,
        StockRealtimeQuote.symbol == symbol,
    ).order_by(StockRealtimeQuote.fetched_at.desc()).limit(limit)).all())
    quote = next((row for row in quote_rows if _known(row.fetched_at, cutoff)), None)
    if quote is not None:
        add("realtime", quote, "REALTIME_QUOTE", f"{symbol} 实时行情 · {quote.quote_time or '时间未说明'}", {
            key: getattr(quote, key) for key in (
                "quote_time", "current_price", "previous_close_price", "open_price", "high_price",
                "low_price", "volume", "amount", "change_amount", "change_pct", "turnover_rate",
            )
        } | {"fact_only": True}, quote.fetched_at)

    # F10 section names are the available structured shareholder signal.  The
    # section payload remains source data; no ownership percentage is inferred
    # when the source does not provide one.
    shareholder_terms = ("股东", "回购", "增持", "减持", "股本", "shareholder", "holder")
    f10_rows = list(db.scalars(select(StockF10Cache).where(
        StockF10Cache.market == market,
        StockF10Cache.symbol == symbol,
        or_(*[StockF10Cache.section.ilike(f"%{term}%") for term in shareholder_terms]),
    ).order_by(StockF10Cache.fetched_at.desc()).limit(limit * 2)).all())
    for row in f10_rows:
        if not _known(row.fetched_at, cutoff):
            continue
        add("shareholder", row, "SHAREHOLDER_OBSERVATION", f"{symbol} F10 · {row.section}", {
            "section": row.section,
            "payload": row.payload_json or {},
            "fact_only": True,
            "note": "F10 原始分区资料；仅在原文有明确数值时解释持股比例，不作名称推断。",
        }, row.fetched_at)

    return {
        "facts": facts[: max(limit * 3, 1)],
        "by_category": by_category,
        "source_record_refs": _unique(source_record_refs),
        "source_tables": source_tables,
        "counts": {key: len(value) for key, value in by_category.items()},
    }


def _dataset_versions(
    db: Session,
    source_tables: set[str],
    *,
    market: str,
    symbol: str,
    cutoff: datetime | None,
) -> list[dict[str, Any]]:
    """Resolve the latest published version matching this security and cutoff."""
    rows = list(db.scalars(select(LakeDataset).order_by(LakeDataset.dataset_code)).all())
    normalized_sources = {str(item).lower() for item in source_tables}
    result: list[dict[str, Any]] = []
    for dataset in rows:
        marker = f"{dataset.dataset_code} {dataset.description or ''}".lower()
        selected: tuple[LakeDatasetVersion, LakeObject | None, set[str], dict[str, Any], list[Any], bool] | None = None
        versions = list(db.scalars(select(LakeDatasetVersion).where(
            LakeDatasetVersion.dataset_id == dataset.id,
            LakeDatasetVersion.status == "PUBLISHED",
        ).order_by(LakeDatasetVersion.created_at.desc(), LakeDatasetVersion.id.desc())).all())
        for version_row in versions:
            if not _known(version_row.created_at, cutoff):
                continue
            obj = db.get(LakeObject, version_row.object_id) if version_row.object_id else None
            if obj is not None and not _known(obj.created_at, cutoff):
                continue
            quality = version_row.quality_json if isinstance(version_row.quality_json, dict) else {}
            metadata = obj.metadata_json if obj is not None and isinstance(obj.metadata_json, dict) else {}
            declared_sources = {
                str(value).lower() for value in (
                    quality.get("source_table"),
                    obj.source_table if obj is not None else None,
                    metadata.get("source_table"),
                ) if value
            }
            text_match = any(table in marker for table in normalized_sources)
            object_match = bool(declared_sources & normalized_sources)
            if normalized_sources and not (object_match or text_match):
                continue
            scope_pairs = quality.get("scope_pairs")
            if not isinstance(scope_pairs, list):
                scope_pairs = metadata.get("scope_pairs") if isinstance(metadata.get("scope_pairs"), list) else []
            scope_applied = quality.get("scope_applied")
            if scope_applied is None:
                scope_applied = metadata.get("scope_applied")
            if scope_applied is True and not any(
                isinstance(pair, (list, tuple)) and len(pair) >= 2
                and str(pair[0]).upper() == market and str(pair[1]).upper() == symbol
                for pair in scope_pairs
            ):
                continue
            selected = (version_row, obj, declared_sources, quality, scope_pairs, bool(scope_applied))
            break
        if selected is None:
            continue
        row, obj, declared_sources, quality, scope_pairs, scope_applied = selected
        result.append({
            "dataset_id": dataset.id,
            "dataset_code": dataset.dataset_code,
            "layer": dataset.layer,
            "format": dataset.format,
            "version": row.version,
            "version_id": row.id,
            "object_id": row.object_id,
            "object_uri": obj.object_uri if obj is not None else None,
            "source_tables": sorted(declared_sources) or sorted(
                table for table in source_tables if table.lower() in marker
            ),
            "scope_pairs": scope_pairs,
            "scope_applied": scope_applied,
            "quality": quality,
            "row_count": row.row_count,
        })
    return result


def _lineage_rows(
    db: Session,
    *,
    market: str,
    symbol: str,
    source_tables: set[str],
    document_ids: list[int],
    chunk_ids: list[str],
    structured_refs: list[str],
    datasets: list[dict[str, Any]],
    cutoff: datetime | None,
) -> list[dict[str, Any]]:
    """Return lineage touching this security's docs, records or lake objects."""
    document_id_set = {str(item) for item in document_ids if item not in (None, "")}
    chunk_id_set = {str(item) for item in chunk_ids if item not in (None, "")}
    structured_id_set = {str(item) for item in structured_refs if item not in (None, "")}
    dataset_versions = {
        f"{item.get('dataset_code')}:{item.get('version')}" for item in datasets
        if item.get("dataset_code") and item.get("version")
    }
    object_ids = {str(item.get("object_id")) for item in datasets if item.get("object_id")}
    metadata_ids = document_id_set | chunk_id_set | structured_id_set | object_ids
    source_names = {str(item) for item in source_tables if item}
    candidates = list(db.scalars(select(LakeLineageEvent).order_by(
        LakeLineageEvent.created_at.desc(), LakeLineageEvent.id.desc()).limit(1000)).all())
    result: list[dict[str, Any]] = []
    for row in candidates:
        if not _known(row.created_at, cutoff):
            continue
        metadata = row.metadata_json if isinstance(row.metadata_json, dict) else {}
        scope_pairs = metadata.get("scope_pairs") or []
        pair_matches = not scope_pairs or any(
            isinstance(pair, (list, tuple)) and len(pair) >= 2 and
            str(pair[0]).upper() == market and str(pair[1]).upper() == symbol
            for pair in scope_pairs
        )
        metadata_matches = (
            str(metadata.get("market", "")).upper() == market and
            str(metadata.get("symbol", "")).upper() == symbol
        )
        scoped_source_match = bool(scope_pairs) and (
            row.upstream_type == "SOURCE_TABLE" and str(row.upstream_id) in source_names
        )
        metadata_id_match = any(
            str(metadata.get(key)) in metadata_ids
            for key in ("document_id", "source_record_id", "object_id")
            if metadata.get(key) not in (None, "")
        )
        touched = (
            (row.upstream_type == "KNOWLEDGE_DOCUMENT" and str(row.upstream_id) in document_id_set) or
            (row.downstream_type == "DOCUMENT_CHUNK" and str(row.downstream_id) in chunk_id_set) or
            (row.upstream_type == "LAKE_OBJECT" and str(row.upstream_id) in object_ids) or
            (row.downstream_type == "LAKE_OBJECT" and str(row.downstream_id) in object_ids) or
            (row.downstream_type == "DATASET_VERSION" and str(row.downstream_id) in dataset_versions) or
            scoped_source_match or metadata_matches or metadata_id_match
        )
        if touched and pair_matches:
            result.append({
                "id": row.id,
                "batch_id": row.batch_id,
                "upstream_type": row.upstream_type,
                "upstream_id": row.upstream_id,
                "downstream_type": row.downstream_type,
                "downstream_id": row.downstream_id,
                "transformation": row.transformation,
                "parser_version": row.parser_version,
                "dataset_version": row.dataset_version,
                "metadata": _compact(metadata),
                "created_at": _iso(row.created_at),
            })
        if len(result) >= 120:
            break
    return result


def _document_source_hash(document: KnowledgeDocument) -> str:
    text = document.content or document.title or ""
    return hashlib.sha256(text.encode("utf-8")).hexdigest()[:12]


def _eligible_chunk_rows(
    db: Session,
    documents: list[KnowledgeDocument],
    cutoff: datetime | None,
) -> list[DocumentChunkVersion]:
    """Return current, ready chunks that existed at the point-in-time cutoff."""
    if not documents:
        return []
    expected = {str(row.id): _document_source_hash(row) for row in documents}
    rows = list(db.scalars(select(DocumentChunkVersion).where(
        DocumentChunkVersion.document_id.in_(sorted(expected)),
        DocumentChunkVersion.status == "READY",
    ).order_by(DocumentChunkVersion.created_at.desc())).all())
    current: dict[tuple[str, int], DocumentChunkVersion] = {}
    for row in rows:
        document_id = str(row.document_id or "")
        if not _known(row.created_at, cutoff):
            continue
        # create_chunks encodes the source-document hash in chunk_version.
        # Excluding an older source hash prevents stale chunks from being
        # presented after a KnowledgeDocument has changed.
        if not str(row.chunk_version or "").endswith(f":{expected.get(document_id, '')}"):
            continue
        current.setdefault((document_id, int(row.chunk_index)), row)
    return list(current.values())


def _chunk_payload(row: DocumentChunkVersion) -> dict[str, Any]:
    return {
        "id": row.id,
        "document_id": row.document_id,
        "chunk_index": row.chunk_index,
        "chunk_version": row.chunk_version,
        "content_hash": row.content_hash,
        "text": row.chunk_text[:1800],
        "start_offset": row.start_offset,
        "end_offset": row.end_offset,
        "parser_version": row.parser_version,
        "embedding_model": row.embedding_model,
        "embedding_status": "AVAILABLE" if (row.metadata_json or {}).get("embedding", {}).get("vector") else
                            ("DECLARED_ONLY" if row.embedding_model else "NOT_GENERATED"),
        "embedding_quality": (row.metadata_json or {}).get("embedding", {}).get("model_quality"),
        "section_title": (row.metadata_json or {}).get("section_title"),
        "boundary_type": (row.metadata_json or {}).get("boundary_type"),
        "metadata": row.metadata_json or {},
    }


def _chunks(
    rows: list[DocumentChunkVersion],
    document_ids: list[int],
    limit: int,
) -> list[dict[str, Any]]:
    """Bound context chunks fairly so one long document cannot crowd out all others."""
    if not rows or not document_ids or limit <= 0:
        return []
    grouped: dict[str, list[DocumentChunkVersion]] = {}
    for row in rows:
        grouped.setdefault(str(row.document_id), []).append(row)
    for items in grouped.values():
        items.sort(key=lambda row: row.chunk_index)
    ordered_ids = [str(item) for item in document_ids]
    selected: list[DocumentChunkVersion] = []
    offset = 0
    while len(selected) < limit:
        added = False
        for document_id in ordered_ids:
            items = grouped.get(document_id, [])
            if offset < len(items):
                selected.append(items[offset])
                added = True
                if len(selected) >= limit:
                    break
        if not added:
            break
        offset += 1
    return [_chunk_payload(row) for row in selected]


def _chunk_coverage(
    document_ids: list[int],
    eligible_rows: list[DocumentChunkVersion],
    included_chunks: list[dict[str, Any]],
) -> dict[str, Any]:
    """Report both ready-chunk availability and actual prompt inclusion."""
    requested = {str(item) for item in document_ids if item not in (None, "")}
    if not requested:
        return {
            "scope": "BOUNDED_SELECTION_CONTEXT",
            "coverage_status": "NO_DOCUMENTS",
            "documents_requested": 0,
            "documents_with_chunks": 0,
            "documents_included": 0,
            "coverage_ratio": None,
            "available_chunk_coverage_ratio": None,
            "context_inclusion_ratio": None,
            "missing_document_ids": [],
            "omitted_from_context_document_ids": [],
        }
    covered = {str(row.document_id) for row in eligible_rows if row.document_id not in (None, "")}
    included = {
        str(row.get("document_id")) for row in included_chunks if row.get("document_id") not in (None, "")
    }
    available_ratio = round(len(requested & covered) / len(requested), 6)
    included_ratio = round(len(requested & included) / len(requested), 6)
    return {
        "scope": "BOUNDED_SELECTION_CONTEXT",
        "coverage_status": "COMPLETE" if requested <= covered else "PARTIAL",
        "documents_requested": len(requested),
        "documents_with_chunks": len(requested & covered),
        "documents_included": len(requested & included),
        # Compatibility alias for clients already reading coverage_ratio.
        "coverage_ratio": available_ratio,
        "available_chunk_coverage_ratio": available_ratio,
        "context_inclusion_ratio": included_ratio,
        "missing_document_ids": sorted(requested - covered)[:50],
        "omitted_from_context_document_ids": sorted((requested & covered) - included)[:50],
    }


def build_selection_context(
    db: Session,
    market: str,
    symbol: str,
    *,
    knowledge_base_ids: list[int] | None = None,
    graph_ids: list[int] | None = None,
    as_of: datetime | None = None,
    max_documents: int = 16,
    max_relations: int = 40,
    max_facts: int = 40,
    max_chunks: int = 32,
) -> dict[str, Any]:
    """Build a bounded, reproducible context contract for one security.

    ``as_of`` is the information cutoff.  The result includes identifiers and
    hashes for every versioned layer so a later review can prove what the LLA
    actually saw.  Empty source areas are represented explicitly as gaps.
    """
    market, symbol = market.strip().upper(), symbol.strip().upper()
    cutoff = as_of or datetime.now(timezone.utc)
    kb_ids = sorted(set(int(item) for item in (knowledge_base_ids or []) if item))
    graph_ids = sorted(set(int(item) for item in (graph_ids or []) if item))
    identity = _identity(db, market, symbol, cutoff)

    doc_query = select(KnowledgeDocument).where(
        KnowledgeDocument.market == market,
        KnowledgeDocument.symbol == symbol,
        KnowledgeDocument.created_at <= cutoff,
        KnowledgeDocument.updated_at <= cutoff,
    )
    if kb_ids:
        doc_query = doc_query.where(KnowledgeDocument.knowledge_base_id.in_(kb_ids))
    if graph_ids:
        doc_query = doc_query.where(KnowledgeDocument.graph_id.in_(graph_ids))
    doc_candidates = list(db.scalars(
        doc_query.order_by(KnowledgeDocument.updated_at.desc(), KnowledgeDocument.id.desc())
        .limit(max(max_documents * 20, max_documents))
    ).all())
    by_source: dict[str, list[KnowledgeDocument]] = {}
    for row in doc_candidates:
        by_source.setdefault(row.source_table, []).append(row)
    source_keys = sorted(
        by_source,
        key=lambda key: (_aware(by_source[key][0].updated_at) or datetime.min.replace(tzinfo=timezone.utc)),
        reverse=True,
    )
    docs: list[KnowledgeDocument] = []
    offset = 0
    while len(docs) < max_documents and source_keys:
        added = False
        for source in source_keys:
            rows = by_source[source]
            if offset < len(rows):
                docs.append(rows[offset])
                added = True
                if len(docs) >= max_documents:
                    break
        if not added:
            break
        offset += 1
    document_ids = [row.id for row in docs]

    entity_query = select(KnowledgeEntity).where(
        KnowledgeEntity.knowledge_base_id.in_(kb_ids) if kb_ids else KnowledgeEntity.id > 0,
    )
    if graph_ids:
        entity_query = entity_query.where(KnowledgeEntity.graph_id.in_(graph_ids))
    # Graph builder keys are namespaced (for example
    # ``{graph_id}:stock_symbol:{market}:{symbol}``).  Match the suffix rather
    # than assuming the namespace starts at position zero.
    suffixes = (f"%:{market}:{symbol}", f"%:{market}:{symbol}:%")
    entity_query = entity_query.where(or_(
        KnowledgeEntity.entity_key.like(suffixes[0]),
        KnowledgeEntity.entity_key.like(suffixes[1]),
    ))
    entities = [
        row for row in db.scalars(entity_query.limit(360)).all()
        if _known(row.created_at, cutoff)
    ][:120]
    entity_ids = [row.id for row in entities]
    relations: list[KnowledgeRelation] = []
    if entity_ids:
        relation_query = select(KnowledgeRelation).where(
            or_(KnowledgeRelation.subject_entity_id.in_(entity_ids), KnowledgeRelation.object_entity_id.in_(entity_ids)),
        )
        if kb_ids:
            relation_query = relation_query.where(KnowledgeRelation.knowledge_base_id.in_(kb_ids))
        if graph_ids:
            relation_query = relation_query.where(KnowledgeRelation.graph_id.in_(graph_ids))
        relations = [
            row for row in db.scalars(
                relation_query.order_by(KnowledgeRelation.id.desc()).limit(max_relations * 4)
            ).all()
            if _known(row.created_at, cutoff)
        ][:max_relations]
    all_relation_entity_ids = _unique(
        [value for relation in relations for value in (relation.subject_entity_id, relation.object_entity_id)]
    )
    relation_entities = {
        row.id: row for row in db.scalars(select(KnowledgeEntity).where(
            KnowledgeEntity.id.in_(all_relation_entity_ids)
        )).all() if _known(row.created_at, cutoff)
    } if all_relation_entity_ids else {}
    relation_doc_ids = _unique([row.evidence_document_id for row in relations])
    relation_docs = {
        row.id: row for row in db.scalars(select(KnowledgeDocument).where(
            KnowledgeDocument.id.in_(relation_doc_ids)
        )).all() if _known(row.created_at, cutoff) and _known(row.updated_at, cutoff)
    } if relation_doc_ids else {}
    relations = [
        row for row in relations
        if row.subject_entity_id in relation_entities
        and row.object_entity_id in relation_entities
        and (row.evidence_document_id is None or row.evidence_document_id in relation_docs)
    ]
    graph_paths = [{
        "id": row.id,
        "subject_entity_id": row.subject_entity_id,
        "object_entity_id": row.object_entity_id,
        "subject_name": relation_entities.get(row.subject_entity_id).entity_name if row.subject_entity_id in relation_entities else None,
        "object_name": relation_entities.get(row.object_entity_id).entity_name if row.object_entity_id in relation_entities else None,
        "predicate": row.predicate,
        "created_at": _iso(row.created_at),
        "evidence_document_id": row.evidence_document_id,
        "evidence_excerpt": (relation_docs[row.evidence_document_id].content or "")[:800]
        if row.evidence_document_id in relation_docs else None,
    } for row in relations]

    company_id = identity.get("company_id")
    facts: list[dict[str, Any]] = []
    fact_evidence_ids: list[str] = []
    if company_id:
        fact_query = facts_query(
            status="ACCEPTED", entity_id=company_id,
            as_of=cutoff.date(), known_at=cutoff,
        ).limit(max_facts)
        for fact in db.scalars(fact_query).all():
            links = list(db.scalars(select(FoundationFactEvidence).where(
                FoundationFactEvidence.fact_id == fact.id,
            )).all())
            proof_ids = [link.evidence_id for link in links]
            proofs = list(db.scalars(select(FoundationEvidence).where(
                FoundationEvidence.id.in_(proof_ids),
            )).all()) if proof_ids else []
            proofs = [proof for proof in proofs if _known(proof.available_at, cutoff)]
            fact_evidence_ids.extend(proof.id for proof in proofs)
            facts.append({
                "id": fact.id,
                "fact_type": fact.fact_type,
                "title": fact.title,
                "subject_entity_id": fact.subject_entity_id,
                "object_entity_id": fact.object_entity_id,
                "properties": fact.properties_json or {},
                "status": fact.status,
                "valid_from": _iso(fact.valid_from),
                "valid_to": _iso(fact.valid_to),
                "created_at": _iso(fact.created_at),
                "evidence": [{
                    "id": proof.id,
                    "title": proof.title,
                    "source_name": proof.source_name,
                    "source_url": proof.url,
                    "published_at": _iso(proof.published_at),
                    "available_at": _iso(proof.available_at),
                    "excerpt": proof.content[:900],
                    "content_hash": proof.content_hash,
                } for proof in proofs],
            })

    structured = _structured_observations(db, market, symbol, cutoff, max_facts)
    # Keep the original FoundationFact shape while adding normalized market
    # observations to the same bounded fact channel.  Consumers can use
    # ``fact_type`` to distinguish source observations from reviewed company
    # relationships.
    fact_limit = max(0, int(max_facts))
    facts = (facts + structured["facts"])[:fact_limit]

    context_documents = list(docs)
    documents = [_doc_payload(row) for row in context_documents]
    # Include relation proof documents in the evidence set, but do not let them
    # displace the source-balanced primary documents above.
    for row in relation_docs.values():
        if row.id not in document_ids and len(documents) < max_documents:
            context_documents.append(row)
            documents.append(_doc_payload(row))
            document_ids.append(row.id)
    eligible_chunks = _eligible_chunk_rows(db, context_documents, cutoff)
    chunks = _chunks(eligible_chunks, document_ids, max_chunks)
    chunk_coverage = _chunk_coverage(document_ids, eligible_chunks, chunks)
    source_tables = {row.source_table for row in context_documents} | set(structured["source_tables"])
    datasets = _dataset_versions(
        db, source_tables, market=market, symbol=symbol, cutoff=cutoff,
    )
    lineage_rows = _lineage_rows(
        db,
        market=market,
        symbol=symbol,
        source_tables=source_tables,
        document_ids=document_ids,
        chunk_ids=[str(item["id"]) for item in chunks],
        structured_refs=structured["source_record_refs"],
        datasets=datasets,
        cutoff=cutoff,
    )

    graph_versions = []
    if graph_ids:
        graph_versions = [{"id": row.id, "code": row.graph_code, "version": row.version,
                           "status": row.governance_status} for row in db.scalars(
                               select(KnowledgeGraph).where(KnowledgeGraph.id.in_(graph_ids))
                           ).all() if _known(row.created_at, cutoff)]
    kb_versions = [{"id": row.id, "code": row.kb_code, "version": row.version, "status": row.status}
                   for row in db.scalars(select(KnowledgeBase).where(KnowledgeBase.id.in_(kb_ids))).all()
                   if _known(row.created_at, cutoff)] if kb_ids else []
    graph_version = _hash({"graphs": graph_versions, "kbs": kb_versions, "relations": [row.id for row in relations]})[:32]
    lakehouse_version = _hash({
        "datasets": datasets,
        "lineage": [row["id"] for row in lineage_rows],
        "structured_records": structured["source_record_refs"],
    })[:32]
    document_version = _hash({"documents": [(row.id, row.updated_at, _hash(row.content or ""))
                                              for row in context_documents],
                              "chunks": [(row["id"], row["chunk_version"]) for row in chunks]})[:32]
    evidence_span = [{
        "document_id": row["id"],
        "start": 0,
        "end": min(len(row.get("content") or ""), 1200),
        "quote": row.get("content") or "",
        "content_hash": row.get("content_hash"),
    } for row in documents[:max_documents]]
    missing = []
    if not documents:
        missing.append("知识库文档")
    if not facts and not graph_paths:
        missing.append("公司关系事实/图谱关系")
    if not chunks:
        missing.append("文档切片")
    elif (chunk_coverage.get("available_chunk_coverage_ratio") or 0) < 1:
        missing.append(
            f"部分知识文档尚未切片（{chunk_coverage['documents_with_chunks']}/"
            f"{chunk_coverage['documents_requested']}）"
        )
    if chunks and (chunk_coverage.get("context_inclusion_ratio") or 0) < (
        chunk_coverage.get("available_chunk_coverage_ratio") or 0
    ):
        missing.append(
            f"部分可用切片因上下文预算未载入（{chunk_coverage['documents_included']}/"
            f"{chunk_coverage['documents_with_chunks']} 篇文档）"
        )
    if any(item.get("embedding_status") != "AVAILABLE" for item in chunks):
        missing.append("部分切片尚未生成向量")
    category_labels = {
        "financial": "结构化财务观测",
        "market": "结构化量价观测",
        "realtime": "实时行情观测",
        "shareholder": "股东/F10观测",
    }
    for category, label in category_labels.items():
        if not structured["by_category"].get(category):
            missing.append(label)
    context = {
        "context_version": CONTEXT_VERSION,
        "as_of": cutoff.isoformat(),
        "data_cutoff": cutoff.date().isoformat(),
        "identity": identity,
        "graph_version": graph_version,
        "lakehouse_version": lakehouse_version,
        "document_version": document_version,
        "knowledge_base_versions": kb_versions,
        "graph_versions": graph_versions,
        "lakehouse_datasets": datasets,
        "lineage": lineage_rows,
        "documents": documents,
        "facts": facts,
        "structured_observations": structured["by_category"],
        "graph_paths": graph_paths,
        "chunks": chunks,
        "chunk_coverage": chunk_coverage,
        "evidence_span": evidence_span,
        "evidence_document_ids": document_ids,
        # Structured source rows are valid fact references even when the
        # bounded ``facts`` array is full of reviewed company facts.  They are
        # also present under ``structured_observations`` for prompt context.
        "evidence_fact_ids": _unique([item["id"] for item in facts] + structured["source_record_refs"]),
        "evidence_source_ids": _unique(fact_evidence_ids + structured["source_record_refs"]),
        "evidence_record_refs": structured["source_record_refs"],
        "missing_data": missing,
        "counts": {
            "documents": len(documents), "facts": len(facts), "graph_paths": len(graph_paths),
            "chunks": len(chunks), "chunk_documents": chunk_coverage["documents_with_chunks"],
            "lineage": len(lineage_rows), "datasets": len(datasets),
            "structured": structured["counts"],
        },
    }
    context["context_hash"] = _hash({key: value for key, value in context.items() if key != "context_hash"})
    return context


def context_for_candidate(db: Session, candidate: Any, *, as_of: datetime | None = None) -> dict[str, Any]:
    """Convenience wrapper used by the selection workflow."""
    run = getattr(candidate, "run", None)
    if run is None:
        from app.models.selection import SelectionRun
        run = db.get(SelectionRun, candidate.run_id)
    return build_selection_context(
        db,
        candidate.market,
        candidate.symbol,
        knowledge_base_ids=list(getattr(run, "knowledge_base_ids_json", []) or []),
        graph_ids=list(getattr(run, "graph_ids_json", []) or []),
        as_of=as_of or getattr(run, "as_of", None),
    )

