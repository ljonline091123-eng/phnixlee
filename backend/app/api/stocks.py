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
from app.models.lakehouse import DocumentChunkVersion
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
    StockSymbolPage,
    StockSymbolRead,
    StockSyncRequest,
    StockSyncResponse,
    WatchlistItemCreate,
    WatchlistItemRead,
)
from app.services.ipo_calendar import IpoCalendarService
from app.services.stock_on_demand import OnDemandFetchError, StockOnDemandService

router = APIRouter(prefix="/stocks", tags=["Stock Master Data"])


def _watch_symbol(value: str, market: str) -> str:
    normalized_market = normalize_market(market)
    text = value.strip().upper()
    return text.zfill(digit_length_for_market(normalized_market)) if text.isdigit() else text


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

    # Documents and chunks are queried in bulk.  ``document_id`` is a string
    # in the lakehouse table while KnowledgeDocument.id is an integer, hence
    # the explicit conversion retained from the original implementation.
    document_rows = db.execute(
        select(
            KnowledgeDocument.id,
            KnowledgeDocument.market,
            KnowledgeDocument.symbol,
            KnowledgeDocument.updated_at,
            KnowledgeDocument.graph_id,
        ).where(pair_filter(KnowledgeDocument))
    ).all()
    documents_by_key: dict[tuple[str, str], list[Any]] = {key: [] for key in keys}
    document_ids: list[str] = []
    for document_id, market, symbol, updated_at, graph_id in document_rows:
        key = (market, symbol)
        row = (str(document_id), updated_at, graph_id)
        documents_by_key.setdefault(key, []).append(row)
        document_ids.append(str(document_id))

    chunk_counts: dict[str, int] = {}
    if document_ids:
        # A source may produce many documents per stock.  Chunk this IN list
        # as well, otherwise a large page could exceed SQLite's bind limit.
        for offset in range(0, len(document_ids), 500):
            document_batch = document_ids[offset : offset + 500]
            chunk_rows = db.execute(
                select(DocumentChunkVersion.document_id, func.count().label("chunk_count"))
                .where(DocumentChunkVersion.document_id.in_(document_batch))
                .group_by(DocumentChunkVersion.document_id)
            ).all()
            chunk_counts.update({str(document_id): int(count or 0) for document_id, count in chunk_rows})

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
    db: Session = Depends(get_db),
) -> list[StockNotice]:
    if limit < 1 or limit > 500:
        raise HTTPException(status_code=422, detail="limit must be between 1 and 500")
    normalized_market = market.upper()
    normalized_symbol = _normalize_symbol(normalized_market, symbol)
    return list(
        db.scalars(
            select(StockNotice)
            .where(StockNotice.market == normalized_market, StockNotice.symbol == normalized_symbol)
            .order_by(StockNotice.notice_date.desc())
            .limit(limit)
        ).all()
    )


@router.get("/{market}/{symbol}/notices/page", response_model=StockNoticePage)
def list_notices_page(
    market: str,
    symbol: str,
    page: int = 1,
    page_size: int = 8,
    db: Session = Depends(get_db),
) -> StockNoticePage:
    if page < 1 or page_size < 1 or page_size > 100:
        raise HTTPException(status_code=422, detail="page must be >= 1 and page_size must be between 1 and 100")
    normalized_market = market.upper()
    normalized_symbol = _normalize_symbol(normalized_market, symbol)
    filters = [
        StockNotice.market == normalized_market,
        StockNotice.symbol == normalized_symbol,
    ]
    total = db.scalar(select(func.count()).select_from(StockNotice).where(*filters)) or 0
    items = db.scalars(
        select(StockNotice)
        .where(*filters)
        .order_by(StockNotice.notice_date.desc(), StockNotice.id.desc())
        .offset((page - 1) * page_size)
        .limit(page_size)
    ).all()
    return StockNoticePage(items=list(items), total=total, page=page, page_size=page_size)


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
        raise HTTPException(status_code=404, detail="Stock symbol not found")
    quote = db.scalar(
        select(StockRealtimeQuote)
        .where(StockRealtimeQuote.market == normalized_market, StockRealtimeQuote.symbol == normalized_symbol)
        .order_by(StockRealtimeQuote.fetched_at.desc())
    )
    return _stock_symbol_read(item, quote, db)
