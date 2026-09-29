import json
from datetime import datetime, timedelta, timezone
from types import SimpleNamespace
from typing import Any

from fastapi import APIRouter, Depends, HTTPException, Response
from sqlalchemy import func, or_, select
from sqlalchemy.orm import Session

from app.core.markets import (
    LIVE_REFRESH_MARKETS,
    MARKET_HK,
    MARKET_NEEQ,
    MARKET_NEEQ_INNOVATION,
    MASTER_MARKETS,
    digit_length_for_market,
    normalize_market,
)
from app.db.session import get_db
from app.orchestration.f10 import (
    F10ValidationError,
    F10Workflow,
    GetStockF10Command,
    StockNotFoundError,
)
from app.orchestration.stock_sync import (
    StockMasterSyncWorkflow,
    StockSyncCommand,
    SyncSourceDisabledError,
    SyncSourceNotFoundError,
)
from app.services.f10 import (
    _fetch_and_cache_f10_extended_data,
    _hk_published_reports_need_refresh,
    _needs_f10_refresh,
    NOTICE_CATEGORIES,
    classify_notice,
)
from app.models.market_data import (
    DataFetchLog,
    DataSource,
    DataSyncLog,
    StockFinancialReport,
    StockF10Cache,
    StockKline,
    StockNews,
    StockRealtimeQuote,
    StockNotice,
    StockSymbol,
    WatchlistItem,
)
from app.models.ai_hub import KnowledgeDocument, KnowledgeEntity, KnowledgeGraph, KnowledgeRelation
from app.models.pipeline import PipelineRun
from app.schemas.market_data import (
    DataFetchLogRead,
    DataSyncLogRead,
    FinancialFetchRequest,
    IpoCalendarResponse,
    KlineFetchRequest,
    NewsFetchRequest,
    NoticeFetchRequest,
    OnDemandFetchResponse,
    QuoteFetchRequest,
    StockFinancialReportRead,
    StockKlineRead,
    StockNewsRead,
    StockNewsPage,
    StockQuoteRead,
    StockNoticeRead,
    StockNoticePage,
    StockF10Read,
    StockPipelineStatusDetailRead,
    StockSymbolPage,
    StockSymbolRead,
    StockSyncRequest,
    StockSyncResponse,
    WatchlistItemCreate,
    WatchlistItemRead,
)
from app.schemas.stock_batch import (
    BUSINESS_DATA_CONTINUATION,
    DOCUMENT_CHUNKS_CONTINUATION,
    MASTER_BUSINESS_TYPES,
    SCOPED_GRAPH_CONTINUATION,
    StockBatchGovernanceRequest,
    StockBatchGovernanceResponse,
    StockPipelineContinuationRequest,
)
from app.api.stock_batch import (
    find_stock_batch_job_by_client_key,
    stock_batch_job_view,
    submit_stock_batch_governance,
)
from app.services.ipo_calendar import IpoCalendarService
from app.services.lakehouse import current_knowledge_document_chunk_counts
from app.services.stock_on_demand import OnDemandFetchError, StockOnDemandService

router = APIRouter(prefix="/stocks", tags=["Stock Master Data"])


def _watch_symbol(value: str, market: str) -> str:
    normalized_market = normalize_market(market)
    text = value.strip().upper()
    return text.zfill(digit_length_for_market(normalized_market)) if text.isdigit() else text


def _notice_read_payload(row: StockNotice, latest_date: str | None = None) -> dict[str, Any]:
    return {
        "id": row.id,
        "market": row.market,
        "symbol": row.symbol,
        "notice_date": row.notice_date,
        "title": row.title,
        "notice_type": row.notice_type,
        "raw_notice_type": row.notice_type,
        "category": classify_notice(row.title, row.notice_type),
        "is_latest": bool(latest_date and row.notice_date == latest_date),
        "url": row.url,
        "content_json": row.content_json,
        "source_id": row.source_id,
        "fetched_at": row.fetched_at,
    }


def _rank_stock_candidate(item: StockSymbol, query: str, market: str) -> tuple[int, int, str]:
    symbol = (item.symbol or "").upper()
    name = item.name or ""
    upper_query = query.upper()
    normalized_query = _watch_symbol(query, market) if query.isdigit() else upper_query
    if symbol == normalized_query or symbol == upper_query:
        match_rank = 0
    elif name == query:
        match_rank = 1
    elif symbol.startswith(upper_query):
        match_rank = 2
    elif symbol.endswith(upper_query):
        match_rank = 3
    elif upper_query in symbol:
        match_rank = 4
    else:
        match_rank = 5
    status_rank = 0 if item.status == "LISTED" else 1
    return (match_rank, status_rank, symbol)


def re_fullmatch(pattern: str, value: str) -> bool:
    import re

    return re.fullmatch(pattern, value) is not None


def _validate_watch_symbol(value: str, market: str) -> str:
    """Validate/normalize a code after fuzzy lookup has not found a local row."""
    normalized_market = normalize_market(market)
    if normalized_market != "ALL" and normalized_market not in MASTER_MARKETS:
        raise HTTPException(
            status_code=422,
            detail=f"market must be one of: ALL, {', '.join(MASTER_MARKETS)}",
        )
    raw = value.strip().upper()
    if not raw.isdigit():
        raise HTTPException(status_code=422, detail="股票代码只支持数字格式")
    if normalized_market == MARKET_HK:
        if not re_fullmatch(r"\d{1,5}", raw):
            raise HTTPException(status_code=422, detail="港股代码应为 1-5 位数字")
    elif not re_fullmatch(r"\d{6}", raw):
        raise HTTPException(status_code=422, detail="该市场股票代码应为 6 位数字")
    return _watch_symbol(raw, normalized_market)


def _resolve_watch_symbol(db: Session, value: str, market: str) -> str:
    """Resolve a fuzzy code/name against the locally synchronized master data."""
    normalized_market = normalize_market(market)
    if normalized_market not in MASTER_MARKETS:
        raise HTTPException(status_code=422, detail=f"market must be one of: {', '.join(MASTER_MARKETS)}")
    query = value.strip()
    if not query:
        raise HTTPException(status_code=422, detail="请输入股票代码或名称")

    if query.isdigit():
        expected_digits = digit_length_for_market(normalized_market)
        if len(query) > expected_digits:
            raise HTTPException(status_code=422, detail=f"{normalized_market} 股票代码最多 {expected_digits} 位")
        if normalized_market == MARKET_HK or len(query) == expected_digits:
            normalized = _watch_symbol(query, normalized_market)
            exact = db.scalar(
                select(StockSymbol).where(
                    StockSymbol.market == normalized_market,
                    StockSymbol.symbol == normalized,
                    StockSymbol.status.in_(["LISTED", "IPO"]),
                )
            )
            if exact:
                return exact.symbol

    pattern = f"%{query}%"
    candidates = sorted(
        list(
            db.scalars(
                select(StockSymbol).where(
                    StockSymbol.market == normalized_market,
                    StockSymbol.status.in_(["LISTED", "IPO"]),
                    or_(StockSymbol.symbol.ilike(pattern), StockSymbol.name.ilike(pattern)),
                )
            ).all()
        ),
        key=lambda item: _rank_stock_candidate(item, query, normalized_market),
    )[:20]
    if len(candidates) == 1:
        return candidates[0].symbol
    if len(candidates) > 1:
        raise HTTPException(
            status_code=409,
            detail={
                "message": "匹配到多个股票，请从候选列表中选择",
                "candidates": [
                    {"market": item.market, "symbol": item.symbol, "name": _display_stock_name(item)}
                    for item in candidates
                ],
            },
        )
    if query.isdigit():
        return _validate_watch_symbol(query, normalized_market)
    raise HTTPException(status_code=404, detail=f"未找到匹配的 {normalized_market} 股票")


def _normalize_symbol(market: str, symbol: str) -> str:
    normalized_market = normalize_market(market)
    stripped = symbol.strip().upper()
    return stripped.zfill(digit_length_for_market(normalized_market)) if stripped.isdigit() else stripped


def _first_text(payload: dict[str, Any], keys: tuple[str, ...]) -> str | None:
    for key in keys:
        value = payload.get(key)
        if value is None:
            continue
        text = str(value).strip()
        if text:
            return text
    return None


def _has_cjk(text: str) -> bool:
    return any("\u4e00" <= char <= "\u9fff" for char in text)


def _quote_payload_parts(quote: Any | None) -> list[str]:
    if not quote or not isinstance(quote.raw_payload, dict):
        return []
    parts = quote.raw_payload.get("parts")
    if not isinstance(parts, list):
        return []
    return [str(part).strip() for part in parts]


def _hk_short_name_from_quote(quote: Any | None) -> tuple[str | None, str | None]:
    parts = _quote_payload_parts(quote)
    if len(parts) < 3:
        return None, None

    cn_short = parts[1] if len(parts) > 1 and _has_cjk(parts[1]) else None
    en_short: str | None = None
    for candidate in parts[40:]:
        if candidate and not _has_cjk(candidate) and any(ch.isalpha() for ch in candidate):
            en_short = candidate
            break
    return cn_short, en_short


def _shorten_hk_chinese_name(name: str | None) -> str | None:
    text = str(name or "").strip()
    if not text:
        return None
    for suffix in (
        "国际控股有限公司",
        "集团控股有限公司",
        "控股有限公司",
        "集团有限公司",
        "股份有限公司",
        "有限公司",
    ):
        if text.endswith(suffix) and len(text) > len(suffix):
            return text[: -len(suffix)].strip() or text
    return text


def _shorten_hk_english_name(name: str | None) -> str | None:
    text = str(name or "").strip()
    if not text:
        return None
    upper = " ".join(text.replace(".", " ").split()).upper()
    for suffix in (
        " INTERNATIONAL HOLDING LIMITED",
        " GROUP HOLDING LIMITED",
        " HOLDINGS LIMITED",
        " HOLDING LIMITED",
        " GROUP LIMITED",
        " LIMITED",
        " LTD",
        " INC",
        " CORPORATION",
        " CORP",
    ):
        if upper.endswith(suffix) and len(upper) > len(suffix):
            return upper[: -len(suffix)].strip() or upper
    return upper


def _display_stock_name(stock: StockSymbol, quote: Any | None = None) -> str:
    if stock.market == MARKET_HK:
        cn_short, en_short = _hk_short_name_from_quote(quote)
        if cn_short and en_short:
            return f"{cn_short} / {en_short}"
        if cn_short:
            return cn_short
    """Use Chinese / English display names for HK stocks when F10 has both."""
    base_name = str(stock.name or stock.symbol).strip()
    if stock.market != MARKET_HK:
        return base_name

    ext_json = stock.ext_json or {}
    raw_payload = stock.raw_payload or {}
    profile_fields = ext_json.get("f10_profile_fields")
    if not isinstance(profile_fields, dict):
        profile_fields = {}

    chinese_name = _first_text(
        profile_fields,
        (
            "中文名称",
            "公司名称",
            "证券简称",
            "股份简称",
            "简称",
            "SECURITY_NAME_ABBR",
        ),
    )
    english_name = _first_text(
        profile_fields,
        (
            "英文名称",
            "英文名",
            "公司英文名称",
            "英文简称",
            "english_name",
            "ENGLISH_NAME",
        ),
    )
    if not english_name:
        english_name = _first_text(raw_payload, ("en", "english_name", "n")) or base_name

    if chinese_name and not _has_cjk(chinese_name):
        chinese_name = None
    if english_name and chinese_name and english_name.strip().casefold() == chinese_name.strip().casefold():
        english_name = None
    if chinese_name and english_name:
        return f"{_shorten_hk_chinese_name(chinese_name)} / {_shorten_hk_english_name(english_name)}"
    return _shorten_hk_chinese_name(chinese_name) or _shorten_hk_english_name(english_name) or base_name


def _pipeline_coverage_status(completed: int, expected: int, *, last_at: Any = None,
                              detail: dict[str, Any] | None = None) -> dict[str, Any]:
    """Return the small, presentation-ready coverage contract for one stock.

    Coverage is deliberately derived from persisted rows, rather than from a
    job's last message.  A retry, restart, or manually imported record thus
    produces the same status.  The endpoint remains truthful when only some
    source types have been collected.
    """
    completed = max(0, int(completed))
    expected = max(0, int(expected))
    if completed <= 0:
        status, label, color = "NOT_STARTED", "未开始", "gray"
    elif expected > 0 and completed >= expected:
        status, label, color = "COMPLETED", "已完成", "green"
    else:
        status, label, color = "PARTIAL", "部分完成", "yellow"
    return {
        "status": status,
        "label": label,
        "color": color,
        "color_code": color,
        "completed_count": completed,
        "expected_count": expected,
        "last_at": last_at,
        "detail": detail or {},
    }


def _stock_pipeline_statuses(db: Session, stocks: list[StockSymbol]) -> dict[tuple[str, str], dict[str, Any]]:
    """Build pipeline status for a page of securities in bounded queries.

    The stock-master page renders a status badge for every row.  The original
    implementation called the single-stock implementation once per row,
    which meant roughly a dozen SQL statements per stock (and made a page of
    30 stocks noticeably slow).  This function deliberately keeps the exact
    same persisted-row semantics, but aggregates each stage by ``market`` and
    ``symbol`` and performs one query per source/table.  It is also used by
    the single-stock endpoint through :func:`_stock_pipeline_status`, so the
    two views cannot drift apart.
    """
    seen: set[tuple[str, str]] = set()
    for stock in stocks:
        key = (stock.market, stock.symbol)
        if key not in seen:
            seen.add(key)
    keys = list(seen)
    if not keys:
        return {}

    def pair_filter(model: Any):
        # Group symbols by market instead of using a broad
        # ``market IN (...) AND symbol IN (...)`` predicate (which includes a
        # Cartesian product).  Grouping also uses roughly one bind variable
        # per stock, so the documented page_size=500 remains below SQLite's
        # default 999-variable limit.
        by_market: dict[str, set[str]] = {}
        for market, symbol in keys:
            by_market.setdefault(market, set()).add(symbol)
        return or_(*[
            (model.market == market) & model.symbol.in_(sorted(symbols))
            for market, symbols in by_market.items()
        ])

    business_models: dict[str, Any] = {
        "QUOTE": StockRealtimeQuote,
        "KLINE": StockKline,
        "FINANCIAL": StockFinancialReport,
        "NEWS": StockNews,
        "NOTICE": StockNotice,
        "F10": StockF10Cache,
    }
    business_counts: dict[tuple[str, str], dict[str, int]] = {
        key: {name: 0 for name in business_models} for key in keys
    }
    business_last: dict[tuple[str, str], list[Any]] = {key: [] for key in keys}
    for name, model in business_models.items():
        if name == "F10":
            # F10 cache rows can exist with an empty payload when an upstream
            # section is unavailable.  Preserve the single-stock behaviour by
            # counting only rows carrying a payload.
            rows = db.execute(
                select(model.market, model.symbol, model.payload_json, model.fetched_at)
                .where(pair_filter(model))
            ).all()
            for market, symbol, payload, fetched_at in rows:
                key = (market, symbol)
                if payload:
                    business_counts[key][name] += 1
                    if fetched_at is not None:
                        business_last[key].append(fetched_at)
            continue

        rows = db.execute(
            select(
                model.market,
                model.symbol,
                func.count().label("row_count"),
                func.max(model.fetched_at).label("last_at"),
            )
            .where(pair_filter(model))
            .group_by(model.market, model.symbol)
        ).all()
        for market, symbol, row_count, last_at in rows:
            key = (market, symbol)
            business_counts[key][name] = int(row_count or 0)
            if last_at is not None:
                business_last[key].append(last_at)

    # Documents and chunks are queried in bulk.  ``document_id`` is shared by
    # several document domains, so the canonical knowledge-document key must
    # participate in the match to avoid treating e.g. ``notice:1`` as chunks
    # for ``knowledge_document:1``.
    document_rows = list(db.scalars(
        select(KnowledgeDocument).where(pair_filter(KnowledgeDocument))
    ).all())
    documents_by_key: dict[tuple[str, str], list[Any]] = {key: [] for key in keys}
    for document in document_rows:
        key = (document.market, document.symbol)
        row = (str(document.id), document.updated_at, document.graph_id)
        documents_by_key.setdefault(key, []).append(row)
    chunk_counts = current_knowledge_document_chunk_counts(db, document_rows)

    graph_ids_by_key: dict[tuple[str, str], set[int]] = {key: set() for key in keys}
    kb_parts: dict[tuple[str, str], dict[str, Any]] = {}
    for key in keys:
        rows = documents_by_key.get(key, [])
        ids = [document_id for document_id, _updated_at, _graph_id in rows]
        documents_with_chunks = sum(1 for document_id in ids if chunk_counts.get(document_id, 0) > 0)
        kb_parts[key] = {
            "document_count": len(rows),
            "chunk_count": sum(chunk_counts.get(document_id, 0) for document_id in ids),
            "documents_with_chunks": documents_with_chunks,
            "last_at": max((updated_at for _document_id, updated_at, _graph_id in rows if updated_at is not None), default=None),
        }
        graph_ids_by_key[key] = {
            int(graph_id) for _document_id, _updated_at, graph_id in rows if graph_id is not None
        }

    all_graph_ids = sorted({graph_id for graph_ids in graph_ids_by_key.values() for graph_id in graph_ids})
    graph_rows: list[Any] = []
    if all_graph_ids:
        for offset in range(0, len(all_graph_ids), 500):
            graph_rows.extend(db.execute(
                select(
                    KnowledgeGraph.id,
                    KnowledgeGraph.governance_status,
                    KnowledgeGraph.created_at,
                    KnowledgeGraph.last_governed_at,
                ).where(KnowledgeGraph.id.in_(all_graph_ids[offset : offset + 500]))
            ).all())
    graph_info = {
        int(graph_id): (governance_status, created_at, last_governed_at)
        for graph_id, governance_status, created_at, last_governed_at in graph_rows
    }

    # Existing semantics use ``entity_key LIKE '%:<market>:<symbol>'``.  A
    # suffix check is the equivalent operation once the relevant graph rows
    # have been restricted above, and avoids one entity query per stock.
    stock_entity_ids_by_key: dict[tuple[str, str], set[int]] = {key: set() for key in keys}
    entity_stock_key: dict[int, tuple[str, str]] = {}
    if all_graph_ids:
        entity_rows: list[Any] = []
        for offset in range(0, len(all_graph_ids), 500):
            entity_rows.extend(db.execute(
                select(KnowledgeEntity.id, KnowledgeEntity.entity_key, KnowledgeEntity.graph_id)
                .where(
                    KnowledgeEntity.graph_id.in_(all_graph_ids[offset : offset + 500]),
                    KnowledgeEntity.entity_type == "STOCK",
                )
            ).all())
        for entity_id, entity_key, graph_id in entity_rows:
            text_key = str(entity_key or "")
            # Entity keys are namespaced strings ending in ``:<market>:<code>``.
            # Parse that suffix once rather than scanning every page symbol for
            # every entity (the previous implementation's LIKE predicate had
            # the same suffix semantics).
            suffix_parts = text_key.rsplit(":", 2)
            matched_key = (
                (suffix_parts[1], suffix_parts[2])
                if len(suffix_parts) == 3
                else None
            )
            if matched_key not in stock_entity_ids_by_key:
                matched_key = None
            if matched_key is None or int(graph_id) not in graph_ids_by_key[matched_key]:
                continue
            stock_entity_ids_by_key[matched_key].add(int(entity_id))
            entity_stock_key[int(entity_id)] = matched_key

    relation_counts: dict[tuple[str, str], int] = {key: 0 for key in keys}
    all_entity_ids = sorted(entity_stock_key)
    if all_graph_ids and all_entity_ids:
        relation_rows_by_id: dict[int, tuple[int, int, int]] = {}
        for graph_offset in range(0, len(all_graph_ids), 500):
            graph_batch = all_graph_ids[graph_offset : graph_offset + 500]
            for entity_offset in range(0, len(all_entity_ids), 500):
                entity_batch = all_entity_ids[entity_offset : entity_offset + 500]
                for relation_id, subject_id, object_id, relation_graph_id in db.execute(
                    select(
                        KnowledgeRelation.id,
                        KnowledgeRelation.subject_entity_id,
                        KnowledgeRelation.object_entity_id,
                        KnowledgeRelation.graph_id,
                    )
                    .where(
                        KnowledgeRelation.graph_id.in_(graph_batch),
                        or_(
                            KnowledgeRelation.subject_entity_id.in_(entity_batch),
                            KnowledgeRelation.object_entity_id.in_(entity_batch),
                        ),
                    )
                ).all():
                    # A relation whose endpoints fall in two entity batches
                    # is returned by both queries; deduplicate by its primary
                    # key before applying per-stock counts.
                    relation_rows_by_id[int(relation_id)] = (
                        int(subject_id), int(object_id), int(relation_graph_id)
                    )
        for subject_id, object_id, relation_graph_id in relation_rows_by_id.values():
            # A self-loop should count once for that stock, matching SQL
            # COUNT(*) rather than incrementing twice for subject/object.
            touched = {
                key
                for entity_id in (int(subject_id), int(object_id))
                for key in (entity_stock_key.get(entity_id),)
                if key is not None and relation_graph_id in graph_ids_by_key[key]
            }
            touched.discard(None)
            for key in touched:
                relation_counts[key] += 1

    statuses: dict[tuple[str, str], dict[str, Any]] = {}
    for key in keys:
        counts = business_counts[key]
        statuses[key] = {
            "data_collection": _pipeline_coverage_status(
                sum(1 for count in counts.values() if count > 0),
                len(business_models),
                last_at=max(business_last[key]) if business_last[key] else None,
                detail=counts,
            ),
            "knowledge_base": _pipeline_coverage_status(
                (1 if kb_parts[key]["document_count"] else 0)
                + (1 if kb_parts[key]["document_count"] > 0 and kb_parts[key]["documents_with_chunks"] == kb_parts[key]["document_count"] else 0),
                2,
                last_at=kb_parts[key]["last_at"],
                detail={
                    "document_count": kb_parts[key]["document_count"],
                    "chunk_count": kb_parts[key]["chunk_count"],
                    "documents_with_chunks": kb_parts[key]["documents_with_chunks"],
                },
            ),
        }
        graph_ids = graph_ids_by_key[key]
        entity_count = len(stock_entity_ids_by_key[key])
        relation_count = relation_counts[key]
        governed_graph_count = sum(
            1 for graph_id in graph_ids
            if graph_info.get(graph_id, (None, None, None))[0] in ("GOVERNED", "LOCKED")
        )
        graph_last = max(
            (
                value
                for graph_id in graph_ids
                for value in graph_info.get(graph_id, (None, None, None))[1:]
                if value is not None
            ),
            default=None,
        )
        statuses[key]["knowledge_graph"] = _pipeline_coverage_status(
            (1 if graph_ids else 0) + (1 if entity_count > 0 else 0) + (1 if relation_count > 0 else 0),
            3,
            last_at=graph_last,
            detail={
                "graph_count": len(graph_ids),
                "entity_count": entity_count,
                "relation_count": relation_count,
                "governed_graph_count": governed_graph_count,
            },
        )
    return statuses


def _stock_pipeline_status(db: Session, stock: StockSymbol) -> dict[str, Any]:
    """Build data/KB/graph status for one security.

    Kept as a compatibility wrapper for detail endpoints and existing callers;
    the paged master-data endpoint uses the batch implementation directly.
    """
    return _stock_pipeline_statuses(db, [stock])[(stock.market, stock.symbol)]


_BUSINESS_STAGE_ITEMS: dict[str, tuple[str, Any, str]] = {
    # There is no standalone GET /quote endpoint; use the auditable pipeline
    # detail route so the UI's verification link always resolves and still
    # exposes the persisted quote sample together with its source metadata.
    "QUOTE": ("实时行情", StockRealtimeQuote, "pipeline-status-detail?sample_limit=5"),
    "KLINE": ("历史量价", StockKline, "kline"),
    "FINANCIAL": ("财务数据", StockFinancialReport, "financials"),
    "NEWS": ("新闻资讯", StockNews, "news/page"),
    "NOTICE": ("公司公告", StockNotice, "notices/page"),
    "F10": ("F10资料", StockF10Cache, "f10?local_only=true"),
}


def _pipeline_text(value: Any) -> str | None:
    if value is None:
        return None
    if hasattr(value, "isoformat"):
        return value.isoformat()
    return str(value)


def _pipeline_preview(value: Any, limit: int = 260) -> str:
    if value in (None, "", {}, []):
        return ""
    if isinstance(value, str):
        text = value
    else:
        text = json.dumps(value, ensure_ascii=False, default=str, separators=(",", ":"))
    return text if len(text) <= limit else f"{text[:limit]}…"


def _pipeline_source_names(db: Session, source_ids: set[int]) -> dict[int, str]:
    if not source_ids:
        return {}
    return {
        int(source_id): str(source_name)
        for source_id, source_name in db.execute(
            select(DataSource.id, DataSource.source_name).where(DataSource.id.in_(source_ids))
        ).all()
    }


def _pipeline_record(
    *,
    record_id: Any,
    title: str,
    observed_at: Any = None,
    source_name: str | None = None,
    source_url: str | None = None,
    verification_path: str | None = None,
    fields: dict[str, Any] | None = None,
) -> dict[str, Any]:
    return {
        "record_id": str(record_id),
        "title": title,
        "observed_at": _pipeline_text(observed_at),
        "source_name": source_name,
        "source_url": source_url,
        "verification_path": verification_path,
        "fields": fields or {},
    }


def _business_sample_rows(
    db: Session,
    stock: StockSymbol,
    *,
    sample_limit: int,
) -> dict[str, list[Any]]:
    pair = lambda model: (model.market == stock.market) & (model.symbol == stock.symbol)
    return {
        "QUOTE": list(db.scalars(
            select(StockRealtimeQuote).where(pair(StockRealtimeQuote))
            .order_by(StockRealtimeQuote.fetched_at.desc()).limit(sample_limit)
        ).all()),
        "KLINE": list(db.scalars(
            select(StockKline).where(pair(StockKline))
            .order_by(StockKline.trade_date.desc(), StockKline.id.desc()).limit(sample_limit)
        ).all()),
        "FINANCIAL": list(db.scalars(
            select(StockFinancialReport).where(pair(StockFinancialReport))
            .order_by(StockFinancialReport.report_period.desc(), StockFinancialReport.id.desc()).limit(sample_limit)
        ).all()),
        "NEWS": list(db.scalars(
            select(StockNews).where(pair(StockNews))
            .order_by(StockNews.news_time.desc(), StockNews.id.desc()).limit(sample_limit)
        ).all()),
        "NOTICE": list(db.scalars(
            select(StockNotice).where(pair(StockNotice))
            .order_by(StockNotice.notice_date.desc(), StockNotice.id.desc()).limit(sample_limit)
        ).all()),
        "F10": [
            row for row in db.scalars(
                select(StockF10Cache).where(pair(StockF10Cache))
                .order_by(StockF10Cache.fetched_at.desc(), StockF10Cache.id.desc())
                .limit(max(sample_limit * 4, sample_limit))
            ).all()
            if row.payload_json
        ][:sample_limit],
    }


def _business_record(
    code: str,
    row: Any,
    *,
    source_names: dict[int, str],
    verification_path: str,
) -> dict[str, Any]:
    source_name = source_names.get(int(row.source_id)) if getattr(row, "source_id", None) else None
    if code == "QUOTE":
        return _pipeline_record(
            record_id=row.id, title=f"{row.quote_time or '最新'} 实时行情",
            observed_at=row.quote_time or row.fetched_at, source_name=source_name,
            verification_path=verification_path,
            fields={"当前价": row.current_price, "涨跌幅": row.change_pct, "成交量": row.volume,
                    "成交额": row.amount, "换手率": row.turnover_rate},
        )
    if code == "KLINE":
        return _pipeline_record(
            record_id=row.id, title=f"{row.trade_date} 日线",
            observed_at=row.trade_date, source_name=source_name, verification_path=verification_path,
            fields={"开盘价": row.open_price, "最高价": row.high_price, "最低价": row.low_price,
                    "收盘价": row.close_price, "成交量": row.volume, "成交额": row.amount,
                    "换手率": row.turnover_rate},
        )
    if code == "FINANCIAL":
        return _pipeline_record(
            record_id=row.id, title=f"{row.report_period} · {row.indicator}",
            observed_at=row.report_period, source_name=source_name, source_url=row.url,
            verification_path=verification_path,
            fields={"报告期": row.report_period, "指标类型": row.indicator, "币种": row.currency,
                    "数据摘要": _pipeline_preview(row.data_json)},
        )
    if code == "NEWS":
        return _pipeline_record(
            record_id=row.id, title=row.title, observed_at=row.news_time,
            source_name=row.source_name or source_name, source_url=row.url,
            verification_path=verification_path,
            fields={"正文摘要": _pipeline_preview(row.content or row.content_json)},
        )
    if code == "NOTICE":
        return _pipeline_record(
            record_id=row.id, title=row.title, observed_at=row.notice_date,
            source_name=source_name, source_url=row.url, verification_path=verification_path,
            fields={"公告类型": row.notice_type, "内容摘要": _pipeline_preview(row.content_json)},
        )
    return _pipeline_record(
        record_id=row.id, title=f"F10 · {row.section}", observed_at=row.fetched_at,
        source_name=source_name, verification_path=verification_path,
        fields={"资料分区": row.section, "内容摘要": _pipeline_preview(row.payload_json)},
    )


def _fetch_log_category(interface_code: str) -> str | None:
    upper = str(interface_code or "").upper()
    for code in ("FINANCIAL", "NOTICE", "KLINE", "QUOTE", "NEWS", "F10"):
        if code in upper:
            return code
    return None


def _latest_business_attempts(db: Session, stock: StockSymbol) -> dict[str, dict[str, Any]]:
    attempts: dict[str, dict[str, Any]] = {}
    logs = db.scalars(
        select(DataFetchLog)
        .where(DataFetchLog.market == stock.market, DataFetchLog.symbol == stock.symbol)
        .order_by(DataFetchLog.started_at.desc(), DataFetchLog.id.desc())
        .limit(100)
    ).all()
    source_names = _pipeline_source_names(db, {int(row.source_id) for row in logs})
    stale_before = datetime.now(timezone.utc) - timedelta(hours=2)
    for row in logs:
        code = _fetch_log_category(row.interface_code)
        if not code or code in attempts:
            continue
        started_at = row.started_at
        comparable_started_at = started_at
        if comparable_started_at is not None and comparable_started_at.tzinfo is None:
            comparable_started_at = comparable_started_at.replace(tzinfo=timezone.utc)
        stale_running = bool(
            str(row.status or "").upper() == "RUNNING"
            and comparable_started_at is not None
            and comparable_started_at < stale_before
        )
        attempts[code] = {
            "fetch_log_id": row.id,
            "interface_code": row.interface_code,
            "status": "STALE" if stale_running else row.status,
            "total_count": int(row.total_count or 0),
            "persisted_count": int(row.persisted_count or 0),
            "source_name": source_names.get(int(row.source_id)),
            "error_message": (
                "采集执行超过2小时且未写入终态，可安全重新提交续作。"
                if stale_running else row.error_message
            ),
            "started_at": row.started_at,
            "completed_at": row.completed_at,
        }
    return attempts


def _data_collection_stage_detail(
    db: Session,
    stock: StockSymbol,
    summary: dict[str, Any],
    *,
    sample_limit: int,
) -> dict[str, Any]:
    counts = {code: int((summary.get("detail") or {}).get(code) or 0) for code in _BUSINESS_STAGE_ITEMS}
    rows_by_code = _business_sample_rows(db, stock, sample_limit=sample_limit)
    source_ids = {
        int(row.source_id)
        for rows in rows_by_code.values() for row in rows
        if getattr(row, "source_id", None) is not None
    }
    source_names = _pipeline_source_names(db, source_ids)
    attempts = _latest_business_attempts(db, stock)
    completed_items: list[dict[str, Any]] = []
    incomplete_items: list[dict[str, Any]] = []
    verification_records: list[dict[str, Any]] = []
    for code, (label, _model, path_suffix) in _BUSINESS_STAGE_ITEMS.items():
        count = counts[code]
        api_path = f"/api/v1/stocks/{stock.market}/{stock.symbol}/{path_suffix}"
        records = [
            _business_record(
                code, row, source_names=source_names, verification_path=api_path,
            )
            for row in rows_by_code[code]
        ]
        verification_records.extend(records)
        latest_attempt = attempts.get(code)
        if count > 0:
            reason = f"已形成 {count} 条可追溯的持久化记录。"
            action_hint = None
            if latest_attempt and latest_attempt.get("status") == "FAILED":
                reason += f" 最近一次刷新失败：{latest_attempt.get('error_message') or '数据源调用失败'}"
                action_hint = "现有记录仍可核验；建议稍后重新采集以恢复数据新鲜度。"
            item_status = "COMPLETED"
            target = completed_items
        elif latest_attempt and latest_attempt.get("status") == "FAILED":
            item_status = "FAILED"
            reason = f"最近一次采集失败：{latest_attempt.get('error_message') or '数据源调用失败'}"
            action_hint = "检查数据源可用性、接口限流或参数后重新采集。"
            target = incomplete_items
        elif latest_attempt and latest_attempt.get("status") == "RUNNING":
            item_status = "PROCESSING"
            reason = "采集任务正在执行，尚未产生持久化记录。"
            action_hint = "等待当前任务完成后刷新状态。"
            target = incomplete_items
        elif latest_attempt and latest_attempt.get("status") == "STALE":
            item_status = "STALE"
            reason = "最近一次采集执行超过2小时且未写入终态，已视为超时任务。"
            action_hint = "可使用继续完成功能重新提交；新任务会按幂等规则执行。"
            target = incomplete_items
        elif latest_attempt and latest_attempt.get("status") == "SUCCESS":
            item_status = "EMPTY"
            reason = "最近一次接口调用成功，但未返回或未持久化有效记录。"
            action_hint = "核对上游覆盖范围与返回内容；必要时切换备用数据源。"
            target = incomplete_items
        else:
            item_status = "NOT_STARTED"
            reason = "尚未发现该类业务数据，也没有可关联的采集日志。"
            action_hint = "在股票主数据页勾选该股票并执行业务数据采集。"
            target = incomplete_items
        target.append({
            "code": code,
            "label": label,
            "status": item_status,
            "record_count": count,
            "expected_count": 1,
            "last_at": max(
                (getattr(row, "fetched_at", None) for row in rows_by_code[code] if getattr(row, "fetched_at", None)),
                default=None,
            ),
            "reason": reason,
            "action_hint": action_hint,
            "verification_path": api_path,
            "latest_attempt": latest_attempt,
            "records": records,
        })
    return {
        "stage": "data_collection",
        "label": "业务数据采集",
        "summary": summary,
        "completed_items": completed_items,
        "incomplete_items": incomplete_items,
        "verification": {
            "total_count": sum(counts.values()),
            "sample_count": len(verification_records),
            "records": verification_records,
        },
        "notes": ["完成状态按数据库中的实际持久化记录判定；最近采集失败不会抹除已有记录。"],
    }


def _document_chunk_counts(
    db: Session,
    documents: list[KnowledgeDocument],
) -> dict[str, int]:
    return current_knowledge_document_chunk_counts(db, documents)


def _knowledge_base_stage_detail(
    db: Session,
    stock: StockSymbol,
    summary: dict[str, Any],
    *,
    sample_limit: int,
) -> dict[str, Any]:
    documents = list(db.scalars(
        select(KnowledgeDocument)
        .where(KnowledgeDocument.market == stock.market, KnowledgeDocument.symbol == stock.symbol)
        .order_by(KnowledgeDocument.updated_at.desc(), KnowledgeDocument.id.desc())
    ).all())
    chunk_counts = _document_chunk_counts(db, documents)
    chunked = [row for row in documents if chunk_counts.get(str(row.id), 0) > 0]
    unchunked = [row for row in documents if chunk_counts.get(str(row.id), 0) <= 0]

    def document_record(row: KnowledgeDocument, *, missing_reason: str | None = None) -> dict[str, Any]:
        path = (
            f"/api/v1/resources/knowledge-graphs/{row.graph_id}/documents/{row.id}"
            if row.graph_id else None
        )
        fields: dict[str, Any] = {
            "来源表": row.source_table,
            "来源记录ID": row.source_record_id,
            "切片数": chunk_counts.get(str(row.id), 0),
            "正文长度": len(row.content or ""),
            "正文摘要": _pipeline_preview(row.content),
        }
        if missing_reason:
            fields["未完成原因"] = missing_reason
        return _pipeline_record(
            record_id=row.id, title=row.title, observed_at=row.updated_at,
            source_name=row.source_table, verification_path=path, fields=fields,
        )

    document_records = [document_record(row) for row in documents[:sample_limit]]
    chunked_records = [document_record(row) for row in chunked[:sample_limit]]
    unchunked_records = [
        document_record(
            row,
            missing_reason=(
                "文档正文为空，无法生成有效切片。"
                if not (row.content or "").strip()
                else "该文档尚未被切片任务覆盖，或切片写入未成功。"
            ),
        )
        for row in unchunked[:sample_limit]
    ]
    completed_items: list[dict[str, Any]] = []
    incomplete_items: list[dict[str, Any]] = []

    if documents:
        completed_items.append({
            "code": "KNOWLEDGE_DOCUMENTS", "label": "知识文档",
            "status": "COMPLETED", "record_count": len(documents), "expected_count": 1,
            "last_at": max((row.updated_at for row in documents), default=None),
            "reason": f"已生成 {len(documents)} 份可追溯知识文档。",
            "action_hint": None, "verification_path": None, "latest_attempt": None,
            "records": document_records,
        })
    else:
        incomplete_items.append({
            "code": "KNOWLEDGE_DOCUMENTS", "label": "知识文档",
            "status": "NOT_STARTED", "record_count": 0, "expected_count": 1,
            "last_at": None,
            "reason": "尚未找到与该股票关联的知识文档。",
            "action_hint": "执行知识库加工；若业务数据已采集，确认所选知识库覆盖对应来源表。",
            "verification_path": None, "latest_attempt": None, "records": [],
        })

    if documents and not unchunked:
        completed_items.append({
            "code": "DOCUMENT_CHUNKS", "label": "文档切片",
            "status": "COMPLETED", "record_count": len(chunked), "expected_count": len(documents),
            "last_at": max((row.updated_at for row in chunked), default=None),
            "reason": f"{len(documents)} 份文档均已生成切片，共 {sum(chunk_counts.values())} 个切片。",
            "action_hint": None, "verification_path": "/api/v1/lakehouse/chunks",
            "latest_attempt": None, "records": chunked_records,
        })
    else:
        chunk_status = "PARTIAL" if chunked else "NOT_STARTED"
        reason = (
            f"已切片 {len(chunked)}/{len(documents)} 份文档，仍有 {len(unchunked)} 份未完成。"
            if documents else "没有可供切片的知识文档。"
        )
        incomplete_items.append({
            "code": "DOCUMENT_CHUNKS", "label": "文档切片",
            "status": chunk_status, "record_count": len(chunked), "expected_count": len(documents),
            "last_at": max((row.updated_at for row in chunked), default=None),
            "reason": reason,
            "action_hint": "重新执行知识库与切片阶段，并检查空正文、范围截断和切片失败记录。",
            "verification_path": "/api/v1/lakehouse/chunks",
            "latest_attempt": None, "records": unchunked_records,
        })

    return {
        "stage": "knowledge_base",
        "label": "知识库与切片",
        "summary": summary,
        "completed_items": completed_items,
        "incomplete_items": incomplete_items,
        "verification": {
            "document_count": len(documents),
            "documents_with_chunks": len(chunked),
            "documents_without_chunks": len(unchunked),
            "chunk_count": sum(chunk_counts.values()),
            "records": document_records,
            "incomplete_records": unchunked_records,
        },
        "notes": ["知识文档与切片分别核验；有文档不等于切片已完整覆盖。"],
    }


def _knowledge_graph_stage_detail(
    db: Session,
    stock: StockSymbol,
    summary: dict[str, Any],
    *,
    sample_limit: int,
) -> dict[str, Any]:
    graph_ids = {
        int(graph_id)
        for graph_id in db.scalars(
            select(KnowledgeDocument.graph_id).where(
                KnowledgeDocument.market == stock.market,
                KnowledgeDocument.symbol == stock.symbol,
                KnowledgeDocument.graph_id.is_not(None),
            )
        ).all()
        if graph_id is not None
    }
    graphs = list(db.scalars(
        select(KnowledgeGraph).where(KnowledgeGraph.id.in_(sorted(graph_ids)))
        .order_by(KnowledgeGraph.updated_at.desc(), KnowledgeGraph.id.desc())
    ).all()) if graph_ids else []
    document_scopes: dict[int, list[dict[str, str]]] = {}
    if graph_ids:
        for graph_id, market, symbol in db.execute(
            select(
                KnowledgeDocument.graph_id,
                KnowledgeDocument.market,
                KnowledgeDocument.symbol,
            )
            .where(
                KnowledgeDocument.graph_id.in_(sorted(graph_ids)),
                KnowledgeDocument.market.is_not(None),
                KnowledgeDocument.symbol.is_not(None),
            )
            .distinct()
            .order_by(
                KnowledgeDocument.graph_id,
                KnowledgeDocument.market,
                KnowledgeDocument.symbol,
            )
        ).all():
            document_scopes.setdefault(int(graph_id), []).append({
                "market": str(market),
                "symbol": str(symbol),
            })

    def positive_int(value: Any) -> int | None:
        try:
            result = int(value)
        except (TypeError, ValueError):
            return None
        return result if result > 0 else None

    def projection_run_id(row: KnowledgeGraph) -> int | None:
        report = row.governance_report_json or {}
        metadata = report.get("projection_metadata") or {}
        explicit = positive_int(metadata.get("pipeline_run_id"))
        if explicit:
            return explicit
        # Historical projections predate projection_metadata. Their stable
        # graph-code suffix still provides an auditable link to PipelineRun.
        marker = "_PIPE_"
        code = str(row.graph_code or "")
        if marker not in code:
            return None
        suffix = code.rsplit(marker, 1)[-1]
        return positive_int(suffix) if suffix.isdigit() else None

    projection_run_ids = {
        run_id for row in graphs if (run_id := projection_run_id(row)) is not None
    }
    pipeline_runs = {
        row.id: row
        for row in db.scalars(
            select(PipelineRun).where(PipelineRun.id.in_(sorted(projection_run_ids)))
        ).all()
    } if projection_run_ids else {}

    projection_kind_labels = {
        "PIPELINE_SCOPE_SNAPSHOT": "管道范围快照",
        "PIPELINE_PROJECTION": "管道投影",
        "LAKEHOUSE_VALIDATION": "湖仓验收图谱",
        "BASE_GRAPH": "基础图谱",
    }

    def normalized_scope(value: Any) -> list[dict[str, str]]:
        if not isinstance(value, list):
            return []
        result: list[dict[str, str]] = []
        seen: set[tuple[str, str]] = set()
        for item in value:
            if not isinstance(item, dict):
                continue
            market = str(item.get("market") or "").strip().upper()
            symbol = str(item.get("symbol") or "").strip().upper()
            if not market or not symbol or (market, symbol) in seen:
                continue
            seen.add((market, symbol))
            result.append({"market": market, "symbol": symbol})
        return result

    def projection_fields(row: KnowledgeGraph) -> dict[str, Any]:
        report = row.governance_report_json or {}
        metadata = report.get("projection_metadata") or {}
        run_id = projection_run_id(row)
        pipeline_run = pipeline_runs.get(run_id) if run_id else None
        run_input = (pipeline_run.input_json or {}) if pipeline_run else {}
        run_output = (pipeline_run.output_json or {}) if pipeline_run else {}

        scope = normalized_scope(metadata.get("scope"))
        if not scope:
            scope = normalized_scope(report.get("scope"))
        if not scope and run_input.get("market") and isinstance(run_input.get("symbols"), list):
            scope = normalized_scope([
                {"market": run_input.get("market"), "symbol": symbol}
                for symbol in run_input["symbols"]
            ])
        if not scope:
            scope = document_scopes.get(row.id, [])

        scope_size = positive_int(metadata.get("scope_size")) or len(scope)
        scope_preview = "、".join(
            f"{item['market']}:{item['symbol']}" for item in scope[:5]
        )
        if scope_size > 5:
            scope_preview = f"{scope_preview} 等 {scope_size} 只"
        elif not scope_preview:
            scope_preview = "未记录"

        kind_code = str(metadata.get("projection_kind") or "").strip().upper()
        if not kind_code:
            if run_id:
                kind_code = "PIPELINE_PROJECTION"
            elif "LAKEHOUSE" in str(row.graph_code or "").upper() or report.get("validation"):
                kind_code = "LAKEHOUSE_VALIDATION"
            else:
                kind_code = "BASE_GRAPH"

        base_graph_id = positive_int(metadata.get("base_graph_id"))
        if base_graph_id is None and run_id:
            base_graph_id = (
                positive_int(run_output.get("base_graph_id"))
                or positive_int(run_input.get("graph_id"))
            )
        return {
            "投影类型": projection_kind_labels.get(kind_code, kind_code or "基础图谱"),
            "来源管道ID": run_id,
            "覆盖范围": scope_preview,
            "覆盖股票数": scope_size,
            "基础图谱ID": base_graph_id,
        }
    entities: list[KnowledgeEntity] = []
    if graph_ids:
        suffix = f":{stock.market}:{stock.symbol}"
        entities = [
            row for row in db.scalars(
                select(KnowledgeEntity).where(
                    KnowledgeEntity.graph_id.in_(sorted(graph_ids)),
                    KnowledgeEntity.entity_type == "STOCK",
                )
            ).all()
            if str(row.entity_key or "").endswith(suffix)
        ]
    entity_ids = [row.id for row in entities]
    relations: list[KnowledgeRelation] = []
    if entity_ids:
        relations = list(db.scalars(
            select(KnowledgeRelation).where(
                KnowledgeRelation.graph_id.in_(sorted(graph_ids)),
                or_(
                    KnowledgeRelation.subject_entity_id.in_(entity_ids),
                    KnowledgeRelation.object_entity_id.in_(entity_ids),
                ),
            ).order_by(KnowledgeRelation.id.desc()).limit(max(sample_limit, 1))
        ).all())
    relation_entity_ids = {
        int(entity_id)
        for row in relations
        for entity_id in (row.subject_entity_id, row.object_entity_id)
    }
    entity_names = {
        int(entity_id): {"name": entity_name, "type": entity_type}
        for entity_id, entity_name, entity_type in db.execute(
            select(KnowledgeEntity.id, KnowledgeEntity.entity_name, KnowledgeEntity.entity_type)
            .where(KnowledgeEntity.id.in_(sorted(relation_entity_ids)))
        ).all()
    } if relation_entity_ids else {}

    graph_records = [
        _pipeline_record(
            record_id=row.id, title=row.graph_name,
            observed_at=row.last_governed_at or row.updated_at,
            source_name="知识图谱",
            verification_path=f"/api/v1/resources/knowledge-graphs/{row.id}/explore",
            fields={"图谱代码": row.graph_code, "版本": row.version,
                    "治理状态": row.governance_status, "实体数": row.entity_count,
                    "关系数": row.relation_count, "来源表": row.source_tables},
        )
        # Unlike generic data samples, graph projections form a complete list
        # that the client paginates. Do not silently hide records after five.
        for row in graphs
    ]
    for row, record in zip(graphs, graph_records):
        record["fields"].update(projection_fields(row))
    entity_records = [
        _pipeline_record(
            record_id=row.id, title=row.entity_name, observed_at=row.updated_at,
            source_name="知识实体",
            verification_path=f"/api/v1/resources/knowledge-graphs/{row.graph_id}/explore",
            fields={"实体类型": row.entity_type, "实体标识": row.entity_key,
                    "图谱ID": row.graph_id, "属性": _pipeline_preview(row.properties_json)},
        )
        for row in entities[:sample_limit]
    ]
    relation_records = [
        _pipeline_record(
            record_id=row.id,
            title=(
                f"{entity_names.get(row.subject_entity_id, {}).get('name', row.subject_entity_id)} "
                f"—{row.predicate}→ "
                f"{entity_names.get(row.object_entity_id, {}).get('name', row.object_entity_id)}"
            ),
            source_name="知识关系",
            verification_path=f"/api/v1/resources/knowledge-graphs/{row.graph_id}/explore",
            fields={
                "关系类型": row.predicate,
                "主体类型": entity_names.get(row.subject_entity_id, {}).get("type"),
                "客体类型": entity_names.get(row.object_entity_id, {}).get("type"),
                "证据文档ID": row.evidence_document_id,
                "图谱ID": row.graph_id,
            },
        )
        for row in relations
    ]
    governed_count = sum(row.governance_status in {"GOVERNED", "LOCKED"} for row in graphs)
    criteria = [
        ("GRAPH_PROJECTION", "图谱投影", len(graphs), graph_records,
         "已找到与该股票知识文档绑定的图谱投影。",
         "尚未找到与该股票知识文档绑定的图谱投影。",
         "执行知识图谱投影；确认知识文档已绑定 graph_id。"),
        ("STOCK_ENTITY", "股票实体", len(entities), entity_records,
         "图谱中已建立统一股票实体。",
         "图谱存在，但未找到该市场与代码对应的股票实体。",
         "检查统一身份标识与 STOCK 实体键后重新投影。"),
        ("GRAPH_RELATIONS", "关联关系", int((summary.get("detail") or {}).get("relation_count") or 0), relation_records,
         "股票实体已与其他知识节点建立关系。",
         "已有股票实体，但尚未形成可核验关联关系。",
         "检查来源文档是否产生实体，并重新执行关系构建。"),
    ]
    completed_items: list[dict[str, Any]] = []
    incomplete_items: list[dict[str, Any]] = []
    for code, label, count, records, ok_reason, missing_reason, action_hint in criteria:
        completed = count > 0
        target = completed_items if completed else incomplete_items
        target.append({
            "code": code, "label": label,
            "status": "COMPLETED" if completed else "NOT_STARTED",
            "record_count": count, "expected_count": 1,
            "last_at": max(
                (row.last_governed_at or row.updated_at for row in graphs), default=None,
            ) if graphs else None,
            "reason": ok_reason if completed else missing_reason,
            "action_hint": None if completed else action_hint,
            "verification_path": (
                f"/api/v1/resources/knowledge-graphs/{graphs[0].id}/explore" if graphs else None
            ),
            "latest_attempt": None, "records": records,
        })
    notes = ["图谱完成按投影、股票实体和关联关系三项持久化结果判定。"]
    if graphs and governed_count < len(graphs):
        notes.append(f"{len(graphs) - governed_count} 个图谱投影尚未达到 GOVERNED/LOCKED，仍需治理复核。")
    return {
        "stage": "knowledge_graph",
        "label": "知识图谱",
        "summary": summary,
        "completed_items": completed_items,
        "incomplete_items": incomplete_items,
        "verification": {
            "graph_count": len(graphs),
            "entity_count": len(entities),
            "relation_count": int((summary.get("detail") or {}).get("relation_count") or 0),
            "governed_graph_count": governed_count,
            "graphs": graph_records,
            "entities": entity_records,
            "relations": relation_records,
            "records": graph_records + entity_records + relation_records,
        },
        "notes": notes,
    }


def _stock_pipeline_status_detail(
    db: Session,
    stock: StockSymbol,
    *,
    sample_limit: int = 5,
) -> dict[str, Any]:
    summary = _stock_pipeline_status(db, stock)
    detail = {
        "market": stock.market,
        "symbol": stock.symbol,
        "name": stock.name,
        "stages": {
            "data_collection": _data_collection_stage_detail(
                db, stock, summary["data_collection"], sample_limit=sample_limit,
            ),
            "knowledge_base": _knowledge_base_stage_detail(
                db, stock, summary["knowledge_base"], sample_limit=sample_limit,
            ),
            "knowledge_graph": _knowledge_graph_stage_detail(
                db, stock, summary["knowledge_graph"], sample_limit=sample_limit,
            ),
        },
    }
    for stage_code, stage in detail["stages"].items():
        for item in stage["completed_items"]:
            item.update({
                "can_continue": False,
                "continuation_action": None,
                "blocked_reason": "该项已经完成，无需继续执行。",
            })
        for item in stage["incomplete_items"]:
            item_code = str(item.get("code") or "").upper()
            can_continue = False
            action = None
            blocked_reason = None
            if stage_code == "data_collection" and item_code in MASTER_BUSINESS_TYPES:
                if str(item.get("status") or "").upper() == "PROCESSING":
                    blocked_reason = "该数据采集任务正在执行，请等待当前任务完成后刷新状态。"
                else:
                    can_continue = True
                    action = BUSINESS_DATA_CONTINUATION
            elif stage_code == "knowledge_base" and item_code == "DOCUMENT_CHUNKS":
                if int(stage["verification"].get("document_count") or 0) > 0:
                    can_continue = True
                    action = DOCUMENT_CHUNKS_CONTINUATION
                else:
                    can_continue = True
                    action = SCOPED_GRAPH_CONTINUATION
            elif stage_code == "knowledge_base":
                can_continue = True
                action = SCOPED_GRAPH_CONTINUATION
            elif item_code in {"GRAPH_PROJECTION", "STOCK_ENTITY", "GRAPH_RELATIONS"}:
                can_continue = True
                action = SCOPED_GRAPH_CONTINUATION
            else:
                blocked_reason = "为避免无范围重建或覆盖历史图谱，请先明确目标图谱后再治理。"
            item.update({
                "can_continue": can_continue,
                "continuation_action": action,
                "blocked_reason": blocked_reason,
            })
    return detail


def _stock_symbol_read(stock: StockSymbol, quote: Any | None = None, db: Session | None = None) -> dict[str, Any]:
    result = {
        "id": stock.id,
        "market": stock.market,
        "symbol": stock.symbol,
        "exchange": stock.exchange,
        "name": _display_stock_name(stock, quote),
        "asset_type": stock.asset_type,
        "status": stock.status,
        "list_date": stock.list_date,
        "source_id": stock.source_id,
        "ext_json": stock.ext_json or {},
        "raw_payload": stock.raw_payload or {},
        "last_synced_at": stock.last_synced_at,
    }
    if db is not None:
        summary = _stock_pipeline_status(db, stock)
        result["status_summary"] = summary
        result["data_collection_status"] = summary["data_collection"]["status"]
        result["knowledge_base_status"] = summary["knowledge_base"]["status"]
        result["knowledge_graph_status"] = summary["knowledge_graph"]["status"]
    return result


def _stock_symbol_view(stock: StockSymbol, quote: Any | None = None, db: Session | None = None) -> SimpleNamespace:
    return SimpleNamespace(**_stock_symbol_read(stock, quote, db))


def _latest_quotes_for_stocks(db: Session, stocks: list[StockSymbol]) -> dict[tuple[str, str], StockRealtimeQuote]:
    grouped_symbols: dict[str, set[str]] = {}
    for stock in stocks:
        grouped_symbols.setdefault(stock.market, set()).add(stock.symbol)

    latest: dict[tuple[str, str], StockRealtimeQuote] = {}
    for market, symbols in grouped_symbols.items():
        rows = db.scalars(
            select(StockRealtimeQuote)
            .where(StockRealtimeQuote.market == market, StockRealtimeQuote.symbol.in_(symbols))
            .order_by(StockRealtimeQuote.fetched_at.desc())
        ).all()
        for quote in rows:
            latest.setdefault((quote.market, quote.symbol), quote)
    return latest


def _watchlist_item_read(db: Session, item: WatchlistItem) -> dict[str, Any]:
    stock = db.scalar(
        select(StockSymbol).where(
            StockSymbol.market == item.market,
            StockSymbol.symbol == item.symbol,
        )
    )
    quote = db.scalar(
        select(StockRealtimeQuote)
        .where(StockRealtimeQuote.market == item.market, StockRealtimeQuote.symbol == item.symbol)
        .order_by(StockRealtimeQuote.fetched_at.desc())
    )
    return {
        "id": item.id,
        "market": item.market,
        "symbol": item.symbol,
        "note": item.note,
        "name": _display_stock_name(stock, quote) if stock else None,
        "current_price": quote.current_price if quote else None,
        "change_pct": quote.change_pct if quote else None,
        "created_at": item.created_at,
    }


@router.get("/watchlist", response_model=list[WatchlistItemRead], tags=["Auto Watch"])
def list_watchlist(db: Session = Depends(get_db)):
    items = db.scalars(select(WatchlistItem).order_by(WatchlistItem.created_at.desc())).all()
    return [_watchlist_item_read(db, item) for item in items]


@router.post("/watchlist", response_model=WatchlistItemRead, tags=["Auto Watch"])
def add_watchlist(payload: WatchlistItemCreate, db: Session = Depends(get_db)):
    normalized_market = payload.market.upper()
    symbol = _resolve_watch_symbol(db, payload.symbol, normalized_market)
    existing = db.scalar(select(WatchlistItem).where(WatchlistItem.market == normalized_market, WatchlistItem.symbol == symbol))
    if existing:
        return _watchlist_item_read(db, existing)
    item = WatchlistItem(market=normalized_market, symbol=symbol, note=payload.note)
    db.add(item)
    db.commit()
    db.refresh(item)
    return _watchlist_item_read(db, item)


@router.delete("/watchlist/{market}/{symbol}", status_code=204, tags=["Auto Watch"])
def remove_watchlist(market: str, symbol: str, db: Session = Depends(get_db)):
    normalized_market = market.upper()
    if normalized_market not in MASTER_MARKETS:
        raise HTTPException(status_code=422, detail=f"market must be one of: {', '.join(MASTER_MARKETS)}")
    item = db.scalar(
        select(WatchlistItem).where(
            WatchlistItem.market == normalized_market,
            WatchlistItem.symbol == _validate_watch_symbol(symbol, normalized_market),
        )
    )
    if item:
        db.delete(item)
        db.commit()
    return Response(status_code=204)


@router.get("/ipo-calendar", response_model=IpoCalendarResponse, tags=["Auto Watch"])
def ipo_calendar(days: int = 7, db: Session = Depends(get_db)) -> dict[str, Any]:
    if days < 1 or days > 30:
        raise HTTPException(status_code=422, detail="days must be between 1 and 30")
    return IpoCalendarService(db).load(days=days)


def _get_source(db: Session, source_code: str) -> DataSource:
    source = db.scalar(select(DataSource).where(DataSource.source_code == source_code))
    if not source:
        raise HTTPException(status_code=404, detail=f"Data source not found: {source_code}")
    if not source.enabled:
        raise HTTPException(status_code=409, detail=f"Data source is disabled: {source_code}")
    return source


@router.post("/sync", response_model=StockSyncResponse)
def sync_stock_symbols(payload: StockSyncRequest, db: Session = Depends(get_db)) -> StockSyncResponse:
    try:
        return StockMasterSyncWorkflow(db).execute(
            StockSyncCommand(
                source_code=payload.source_code,
                market=payload.market,
                enable_fallback=payload.enable_fallback,
            )
        )
    except SyncSourceNotFoundError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except SyncSourceDisabledError as exc:
        raise HTTPException(status_code=409, detail=str(exc)) from exc


@router.get("", response_model=StockSymbolPage)
def list_stock_symbols(
    market: str | None = None,
    keyword: str | None = None,
    status: str | None = "LISTED",
    page: int = 1,
    page_size: int = 50,
    db: Session = Depends(get_db),
) -> StockSymbolPage:
    if page < 1 or page_size < 1 or page_size > 500:
        raise HTTPException(status_code=422, detail="page must be >= 1 and page_size must be between 1 and 500")

    filters = []
    # ``ALL`` is a first-class filter used by the data-console selector.  It
    # deliberately means "do not constrain by market" rather than looking for
    # a literal market named ALL (which would always return an empty page).
    normalized_market = str(market or "").strip().upper()
    if normalized_market and normalized_market != "ALL":
        if normalized_market not in MASTER_MARKETS:
            raise HTTPException(
                status_code=422,
                detail=f"market must be one of: ALL, {', '.join(MASTER_MARKETS)}",
            )
        filters.append(StockSymbol.market == normalized_market)
    if status:
        filters.append(StockSymbol.status == status)
    if keyword:
        pattern = f"%{keyword.strip()}%"
        filters.append(or_(StockSymbol.symbol.ilike(pattern), StockSymbol.name.ilike(pattern)))

    statement = select(StockSymbol).where(*filters).order_by(StockSymbol.market, StockSymbol.symbol)
    count_statement = select(func.count()).select_from(StockSymbol).where(*filters)
    total = db.scalar(count_statement) or 0
    items = db.scalars(statement.offset((page - 1) * page_size).limit(page_size)).all()
    latest_quotes = _latest_quotes_for_stocks(db, list(items))
    pipeline_statuses = _stock_pipeline_statuses(db, list(items))
    return StockSymbolPage(
        items=[
            {
                **_stock_symbol_read(item, latest_quotes.get((item.market, item.symbol))),
                "status_summary": pipeline_statuses.get((item.market, item.symbol)),
                "data_collection_status": pipeline_statuses.get((item.market, item.symbol), {}).get("data_collection", {}).get("status"),
                "knowledge_base_status": pipeline_statuses.get((item.market, item.symbol), {}).get("knowledge_base", {}).get("status"),
                "knowledge_graph_status": pipeline_statuses.get((item.market, item.symbol), {}).get("knowledge_graph", {}).get("status"),
            }
            for item in items
        ],
        total=total,
        page=page,
        page_size=page_size,
    )


@router.get("/search", response_model=list[StockSymbolRead])
def search_stock_symbols(
    market: str,
    keyword: str,
    limit: int = 12,
    db: Session = Depends(get_db),
) -> list[Any]:
    normalized_market = str(market or "").strip().upper()
    query = keyword.strip()
    if normalized_market != "ALL" and normalized_market not in MASTER_MARKETS:
        raise HTTPException(
            status_code=422,
            detail=f"market must be one of: ALL, {', '.join(MASTER_MARKETS)}",
        )
    if not query:
        return []
    if limit < 1 or limit > 50:
        raise HTTPException(status_code=422, detail="limit must be between 1 and 50")
    pattern = f"%{query}%"
    search_markets = (
        MASTER_MARKETS
        if normalized_market == "ALL"
        else (
            (MARKET_NEEQ, MARKET_NEEQ_INNOVATION)
            if normalized_market == MARKET_NEEQ
            else (normalized_market,)
        )
    )
    candidates = list(
        db.scalars(
            select(StockSymbol).where(
                StockSymbol.market.in_(search_markets),
                StockSymbol.status.in_(["LISTED", "IPO"]),
                or_(StockSymbol.symbol.ilike(pattern), StockSymbol.name.ilike(pattern)),
            )
        ).all()
    )
    ranked = sorted(
        candidates,
        key=lambda item: _rank_stock_candidate(
            item,
            query,
            item.market if normalized_market == "ALL" else normalized_market,
        ),
    )[:limit]
    latest_quotes = _latest_quotes_for_stocks(db, list(ranked))
    return [
        _stock_symbol_view(item, latest_quotes.get((item.market, item.symbol)), db)
        for item in ranked
    ]


@router.post("/{market}/{symbol}/kline/fetch", response_model=OnDemandFetchResponse)
def fetch_kline(
    market: str,
    symbol: str,
    payload: KlineFetchRequest,
    db: Session = Depends(get_db),
) -> OnDemandFetchResponse:
    normalized_market = market.upper()
    if normalized_market not in MASTER_MARKETS:
        raise HTTPException(status_code=422, detail=f"market must be one of: {', '.join(MASTER_MARKETS)}")
    if payload.start_date > payload.end_date:
        raise HTTPException(status_code=422, detail="start_date cannot be after end_date")
    source = _get_source(db, payload.source_code)
    normalized_symbol = _normalize_symbol(normalized_market, symbol)
    try:
        log, items = StockOnDemandService(db).fetch_kline(
            source=source,
            market=normalized_market,
            symbol=normalized_symbol,
            period=payload.period,
            adjust=payload.adjust,
            start_date=payload.start_date.strftime("%Y%m%d"),
            end_date=payload.end_date.strftime("%Y%m%d"),
            persist=payload.persist,
        )
    except OnDemandFetchError as exc:
        raise HTTPException(
            status_code=502,
            detail={"message": str(exc), "fetch_log_id": exc.fetch_log_id},
        ) from exc
    return OnDemandFetchResponse(
        fetch_log_id=log.id,
        source_code=source.source_code,
        status=log.status,
        total_count=log.total_count,
        persisted_count=log.persisted_count,
        items=items,
    )


@router.get("/{market}/{symbol}/kline", response_model=list[StockKlineRead])
def list_klines(
    market: str,
    symbol: str,
    period: str = "daily",
    adjust: str = "",
    limit: int = 500,
    db: Session = Depends(get_db),
) -> list[StockKline]:
    if limit < 1 or limit > 5000:
        raise HTTPException(status_code=422, detail="limit must be between 1 and 5000")
    normalized_market = market.upper()
    normalized_symbol = _normalize_symbol(normalized_market, symbol)
    statement = (
        select(StockKline)
        .where(
            StockKline.market == normalized_market,
            StockKline.symbol == normalized_symbol,
            StockKline.period == period,
            StockKline.adjust == adjust,
        )
        .order_by(StockKline.trade_date.desc())
        .limit(limit)
    )
    return list(db.scalars(statement).all())


@router.post("/{market}/{symbol}/financials/fetch", response_model=OnDemandFetchResponse)
def fetch_financials(
    market: str,
    symbol: str,
    payload: FinancialFetchRequest,
    db: Session = Depends(get_db),
) -> OnDemandFetchResponse:
    normalized_market = market.upper()
    if normalized_market not in MASTER_MARKETS:
        raise HTTPException(status_code=422, detail=f"market must be one of: {', '.join(MASTER_MARKETS)}")
    source = _get_source(db, payload.source_code)
    normalized_symbol = _normalize_symbol(normalized_market, symbol)
    indicator = payload.indicator or ("报告期" if normalized_market == MARKET_HK else "按报告期")
    try:
        log, items = StockOnDemandService(db).fetch_financials(
            source=source,
            market=normalized_market,
            symbol=normalized_symbol,
            indicator=indicator,
            persist=payload.persist,
        )
    except OnDemandFetchError as exc:
        raise HTTPException(
            status_code=502,
            detail={"message": str(exc), "fetch_log_id": exc.fetch_log_id},
        ) from exc
    return OnDemandFetchResponse(
        fetch_log_id=log.id,
        source_code=source.source_code,
        status=log.status,
        total_count=log.total_count,
        persisted_count=log.persisted_count,
        items=items,
    )


@router.get("/{market}/{symbol}/financials", response_model=list[StockFinancialReportRead])
def list_financials(
    market: str,
    symbol: str,
    indicator: str | None = None,
    limit: int = 100,
    db: Session = Depends(get_db),
) -> list[StockFinancialReport]:
    if limit < 1 or limit > 500:
        raise HTTPException(status_code=422, detail="limit must be between 1 and 500")
    normalized_market = market.upper()
    normalized_symbol = _normalize_symbol(normalized_market, symbol)
    statement = select(StockFinancialReport).where(
        StockFinancialReport.market == normalized_market,
        StockFinancialReport.symbol == normalized_symbol,
    )
    if indicator:
        statement = statement.where(StockFinancialReport.indicator == indicator)
    statement = statement.order_by(StockFinancialReport.report_period.desc()).limit(limit)
    return list(db.scalars(statement).all())


@router.post("/{market}/{symbol}/notices/fetch", response_model=OnDemandFetchResponse)
def fetch_notices(
    market: str,
    symbol: str,
    payload: NoticeFetchRequest,
    db: Session = Depends(get_db),
) -> OnDemandFetchResponse:
    normalized_market = market.upper()
    if normalized_market not in MASTER_MARKETS:
        raise HTTPException(status_code=422, detail=f"market must be one of: {', '.join(MASTER_MARKETS)}")
    if payload.start_date > payload.end_date:
        raise HTTPException(status_code=422, detail="start_date cannot be after end_date")
    source = _get_source(db, payload.source_code)
    normalized_symbol = _normalize_symbol(normalized_market, symbol)
    try:
        log, items = StockOnDemandService(db).fetch_notices(
            source=source,
            market=normalized_market,
            symbol=normalized_symbol,
            start_date=payload.start_date.strftime("%Y%m%d"),
            end_date=payload.end_date.strftime("%Y%m%d"),
            persist=payload.persist,
        )
    except OnDemandFetchError as exc:
        raise HTTPException(
            status_code=502,
            detail={"message": str(exc), "fetch_log_id": exc.fetch_log_id},
        ) from exc
    return OnDemandFetchResponse(
        fetch_log_id=log.id,
        source_code=source.source_code,
        status=log.status,
        total_count=log.total_count,
        persisted_count=log.persisted_count,
        items=items,
    )


@router.post("/{market}/{symbol}/quote/fetch", response_model=OnDemandFetchResponse)
def fetch_quote(
    market: str,
    symbol: str,
    payload: QuoteFetchRequest,
    db: Session = Depends(get_db),
) -> OnDemandFetchResponse:
    normalized_market = market.upper()
    if normalized_market not in MASTER_MARKETS:
        raise HTTPException(status_code=422, detail=f"market must be one of: {', '.join(MASTER_MARKETS)}")
    source = _get_source(db, payload.source_code)
    normalized_symbol = _normalize_symbol(normalized_market, symbol)
    try:
        log, items = StockOnDemandService(db).fetch_quote(
            source=source,
            market=normalized_market,
            symbol=normalized_symbol,
            persist=payload.persist,
        )
    except OnDemandFetchError as exc:
        raise HTTPException(
            status_code=502,
            detail={"message": str(exc), "fetch_log_id": exc.fetch_log_id},
        ) from exc
    return OnDemandFetchResponse(
        fetch_log_id=log.id,
        source_code=source.source_code,
        status=log.status,
        total_count=log.total_count,
        persisted_count=log.persisted_count,
        items=items,
    )


@router.post("/{market}/{symbol}/news/fetch", response_model=OnDemandFetchResponse)
def fetch_news(
    market: str,
    symbol: str,
    payload: NewsFetchRequest,
    db: Session = Depends(get_db),
) -> OnDemandFetchResponse:
    normalized_market = market.upper()
    if normalized_market not in MASTER_MARKETS:
        raise HTTPException(status_code=422, detail=f"market must be one of: {', '.join(MASTER_MARKETS)}")
    source = _get_source(db, payload.source_code)
    normalized_symbol = _normalize_symbol(normalized_market, symbol)
    try:
        log, items = StockOnDemandService(db).fetch_news(
            source=source,
            market=normalized_market,
            symbol=normalized_symbol,
            persist=payload.persist,
        )
    except OnDemandFetchError as exc:
        raise HTTPException(
            status_code=502,
            detail={"message": str(exc), "fetch_log_id": exc.fetch_log_id},
        ) from exc
    return OnDemandFetchResponse(
        fetch_log_id=log.id,
        source_code=source.source_code,
        status=log.status,
        total_count=log.total_count,
        persisted_count=log.persisted_count,
        items=items,
    )
@router.get("/{market}/{symbol}/notices", response_model=list[StockNoticeRead])
def list_notices(
    market: str,
    symbol: str,
    limit: int = 100,
    category: str | None = None,
    db: Session = Depends(get_db),
) -> list[dict[str, Any]]:
    if limit < 1 or limit > 500:
        raise HTTPException(status_code=422, detail="limit must be between 1 and 500")
    normalized_market = market.upper()
    normalized_symbol = _normalize_symbol(normalized_market, symbol)
    if category and category not in NOTICE_CATEGORIES:
        raise HTTPException(status_code=422, detail=f"公告分类必须是以下之一：{', '.join(NOTICE_CATEGORIES)}")
    rows = list(
        db.scalars(
            select(StockNotice)
            .where(StockNotice.market == normalized_market, StockNotice.symbol == normalized_symbol)
            .order_by(StockNotice.notice_date.desc())
            .limit(limit)
        ).all()
    )
    filtered = [row for row in rows if not category or category == "全部" or classify_notice(row.title, row.notice_type) == category]
    latest = max((row.notice_date for row in filtered), default=None)
    return [_notice_read_payload(row, latest) for row in filtered]


@router.get("/{market}/{symbol}/notices/page", response_model=StockNoticePage)
def list_notices_page(
    market: str,
    symbol: str,
    page: int = 1,
    page_size: int = 8,
    category: str | None = None,
    db: Session = Depends(get_db),
) -> StockNoticePage:
    if page < 1 or page_size < 1 or page_size > 100:
        raise HTTPException(status_code=422, detail="page must be >= 1 and page_size must be between 1 and 100")
    normalized_market = market.upper()
    normalized_symbol = _normalize_symbol(normalized_market, symbol)
    if category and category not in NOTICE_CATEGORIES:
        raise HTTPException(status_code=422, detail=f"公告分类必须是以下之一：{', '.join(NOTICE_CATEGORIES)}")
    rows = list(db.scalars(
        select(StockNotice)
        .where(StockNotice.market == normalized_market, StockNotice.symbol == normalized_symbol)
        .order_by(StockNotice.notice_date.desc(), StockNotice.id.desc())
    ).all())
    filtered = [row for row in rows if not category or category == "全部" or classify_notice(row.title, row.notice_type) == category]
    latest = max((row.notice_date for row in filtered), default=None)
    start = (page - 1) * page_size
    return StockNoticePage(
        items=[_notice_read_payload(row, latest) for row in filtered[start : start + page_size]],
        total=len(filtered), page=page, page_size=page_size,
    )


@router.get("/{market}/{symbol}/news/page", response_model=StockNewsPage)
def list_news_page(
    market: str,
    symbol: str,
    page: int = 1,
    page_size: int = 8,
    db: Session = Depends(get_db),
) -> StockNewsPage:
    if page < 1 or page_size < 1 or page_size > 100:
        raise HTTPException(status_code=422, detail="page must be >= 1 and page_size must be between 1 and 100")
    normalized_market = market.upper()
    normalized_symbol = _normalize_symbol(normalized_market, symbol)
    filters = [
        StockNews.market == normalized_market,
        StockNews.symbol == normalized_symbol,
    ]
    total = db.scalar(select(func.count()).select_from(StockNews).where(*filters)) or 0
    items = db.scalars(
        select(StockNews)
        .where(*filters)
        .order_by(StockNews.news_time.desc(), StockNews.id.desc())
        .offset((page - 1) * page_size)
        .limit(page_size)
    ).all()
    return StockNewsPage(items=list(items), total=total, page=page, page_size=page_size)


@router.get("/{market}/{symbol}/f10", response_model=StockF10Read)
def get_stock_f10(
    market: str,
    symbol: str,
    kline_limit: int = 12,
    financial_limit: int = 8,
    notice_limit: int = 8,
    notice_page: int = 1,
    notice_category: str | None = None,
    news_limit: int = 8,
    news_page: int = 1,
    refresh: bool = False,
    local_only: bool = False,
    response: Response = None,
    db: Session = Depends(get_db),
) -> StockF10Read:
    if response is not None:
        response.headers["Cache-Control"] = "no-store, no-cache, must-revalidate, max-age=0"
        response.headers["Pragma"] = "no-cache"
        response.headers["Expires"] = "0"
    command = GetStockF10Command(
        market=market,
        symbol=symbol,
        kline_limit=kline_limit,
        financial_limit=financial_limit,
        notice_limit=notice_limit,
        notice_page=notice_page,
        notice_category=notice_category,
        news_limit=news_limit,
        news_page=news_page,
        refresh=refresh,
        local_only=local_only,
    )
    try:
        return F10Workflow(
            db,
            fetch_extended_data=_fetch_and_cache_f10_extended_data,
            symbol_reader=_stock_symbol_read,
        ).execute(command)
    except F10ValidationError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except StockNotFoundError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc


@router.get("/sync-logs/list", response_model=list[DataSyncLogRead])
def list_sync_logs(
    source_code: str | None = None,
    market: str | None = None,
    limit: int = 50,
    db: Session = Depends(get_db),
) -> list[DataSyncLog]:
    if limit < 1 or limit > 500:
        raise HTTPException(status_code=422, detail="limit must be between 1 and 500")
    statement = select(DataSyncLog).join(DataSource).order_by(DataSyncLog.started_at.desc()).limit(limit)
    if source_code:
        statement = statement.where(DataSource.source_code == source_code)
    if market:
        statement = statement.where(DataSyncLog.market == market)
    return list(db.scalars(statement).all())


@router.get("/fetch-logs/list", response_model=list[DataFetchLogRead])
def list_fetch_logs(
    market: str | None = None,
    symbol: str | None = None,
    limit: int = 50,
    db: Session = Depends(get_db),
) -> list[DataFetchLog]:
    if limit < 1 or limit > 500:
        raise HTTPException(status_code=422, detail="limit must be between 1 and 500")
    statement = select(DataFetchLog).order_by(DataFetchLog.started_at.desc()).limit(limit)
    if market:
        statement = statement.where(DataFetchLog.market == market.upper())
    if symbol:
        statement = statement.where(DataFetchLog.symbol == symbol.upper())
    return list(db.scalars(statement).all())


@router.get(
    "/{market}/{symbol}/pipeline-status-detail",
    response_model=StockPipelineStatusDetailRead,
)
def get_stock_pipeline_status_detail(
    market: str,
    symbol: str,
    sample_limit: int = 5,
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    """Explain and verify each persisted data-to-knowledge stage for one stock."""
    if sample_limit < 1 or sample_limit > 20:
        raise HTTPException(status_code=422, detail="样本数量必须在 1 到 20 之间")
    normalized_market = market.upper()
    if normalized_market not in MASTER_MARKETS:
        raise HTTPException(status_code=422, detail=f"市场必须是以下之一：{', '.join(MASTER_MARKETS)}")
    normalized_symbol = _normalize_symbol(normalized_market, symbol)
    stock = db.scalar(select(StockSymbol).where(
        StockSymbol.market == normalized_market,
        StockSymbol.symbol == normalized_symbol,
    ))
    if stock is None:
        raise HTTPException(status_code=404, detail="股票主数据不存在")
    return _stock_pipeline_status_detail(db, stock, sample_limit=sample_limit)


@router.post(
    "/{market}/{symbol}/pipeline-status/continue",
    status_code=202,
    response_model=StockBatchGovernanceResponse,
)
def continue_stock_pipeline(
    market: str,
    symbol: str,
    payload: StockPipelineContinuationRequest,
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    """Enqueue only requested criteria that remain incomplete for one stock."""
    normalized_market = market.upper()
    if normalized_market not in MASTER_MARKETS:
        raise HTTPException(status_code=422, detail=f"市场必须是以下之一：{', '.join(MASTER_MARKETS)}")
    normalized_symbol = _normalize_symbol(normalized_market, symbol)
    stock = db.scalar(select(StockSymbol).where(
        StockSymbol.market == normalized_market,
        StockSymbol.symbol == normalized_symbol,
    ))
    if stock is None:
        raise HTTPException(status_code=404, detail="股票主数据不存在")

    continuation_client_key = (
        f"pipeline-continuation:{normalized_market}:{normalized_symbol}:{payload.idempotency_key}"
        if payload.idempotency_key else None
    )
    if continuation_client_key:
        existing_job = find_stock_batch_job_by_client_key(db, continuation_client_key)
        if existing_job is not None:
            existing_request = ((existing_job.payload_json or {}).get("request") or {})
            existing_targets = existing_request.get("stocks") or []
            same_request = (
                existing_targets == [{"market": normalized_market, "symbol": normalized_symbol}]
                and existing_request.get("continuation_stage") == payload.stage
                and sorted(existing_request.get("continuation_item_codes") or []) == sorted(payload.item_codes)
                and int(existing_request.get("kline_days") or 365) == payload.kline_days
                and int(existing_request.get("disclosure_days") or 730) == payload.disclosure_days
            )
            if not same_request:
                raise HTTPException(
                    status_code=409,
                    detail="相同幂等键已用于不同的续作请求，请更换幂等键",
                )
            return stock_batch_job_view(db, existing_job)

    status_detail = _stock_pipeline_status_detail(db, stock, sample_limit=1)
    stage = status_detail["stages"].get(payload.stage)
    if stage is None:
        raise HTTPException(status_code=422, detail="续作阶段不存在")
    completed = {str(item.get("code") or "").upper(): item for item in stage["completed_items"]}
    incomplete = {str(item.get("code") or "").upper(): item for item in stage["incomplete_items"]}
    known_codes = set(completed) | set(incomplete)
    unknown_codes = [code for code in payload.item_codes if code not in known_codes]
    if unknown_codes:
        raise HTTPException(status_code=422, detail={
            "message": "续作项目不存在或不属于所选阶段",
            "item_codes": unknown_codes,
        })
    completed_codes = [code for code in payload.item_codes if code in completed]
    if completed_codes:
        raise HTTPException(status_code=409, detail={
            "message": "所选项目已经完成，请刷新状态详情",
            "item_codes": completed_codes,
        })
    blocked = [incomplete[code] for code in payload.item_codes if not incomplete[code].get("can_continue")]
    if blocked:
        raise HTTPException(status_code=409, detail={
            "message": "所选项目当前不能自动续作",
            "items": [{
                "code": item.get("code"),
                "label": item.get("label"),
                "blocked_reason": item.get("blocked_reason"),
            } for item in blocked],
        })
    actions = {incomplete[code].get("continuation_action") for code in payload.item_codes}
    if len(actions) != 1:
        raise HTTPException(status_code=422, detail="一次续作请求只能包含同一种执行动作")
    action = actions.pop()
    request_values: dict[str, Any] = {
        "stocks": [{"market": normalized_market, "symbol": normalized_symbol}],
        "collect_business_data": False,
        "export_lakehouse": False,
        "archive_chunks": False,
        "run_graph": False,
        "run_agent_governance": False,
        "business_types": [],
        "continuation_stage": payload.stage,
        "continuation_item_codes": payload.item_codes,
        "kline_days": payload.kline_days,
        "disclosure_days": payload.disclosure_days,
        "idempotency_key": continuation_client_key,
    }
    if action == BUSINESS_DATA_CONTINUATION:
        request_values.update({
            "collect_business_data": True,
            "business_types": payload.item_codes,
        })
    elif action == DOCUMENT_CHUNKS_CONTINUATION:
        request_values["continuation_action"] = DOCUMENT_CHUNKS_CONTINUATION
    elif action == SCOPED_GRAPH_CONTINUATION:
        request_values["continuation_action"] = SCOPED_GRAPH_CONTINUATION
    else:
        raise HTTPException(status_code=409, detail="所选项目没有安全的自动续作动作")
    batch_request = StockBatchGovernanceRequest.model_validate(request_values)
    return submit_stock_batch_governance(batch_request, db)


@router.get("/{market}/{symbol}", response_model=StockSymbolRead)
def get_stock_symbol(market: str, symbol: str, db: Session = Depends(get_db)) -> dict[str, Any]:
    normalized_market = market.upper()
    normalized_symbol = _normalize_symbol(normalized_market, symbol)
    item = db.scalar(
        select(StockSymbol).where(
            StockSymbol.market == normalized_market,
            StockSymbol.symbol == normalized_symbol,
        )
    )
    if not item:
        raise HTTPException(status_code=404, detail="股票主数据不存在")
    quote = db.scalar(
        select(StockRealtimeQuote)
        .where(StockRealtimeQuote.market == normalized_market, StockRealtimeQuote.symbol == normalized_symbol)
        .order_by(StockRealtimeQuote.fetched_at.desc())
    )
    return _stock_symbol_read(item, quote, db)
