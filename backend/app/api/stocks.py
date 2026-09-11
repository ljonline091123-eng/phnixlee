from datetime import date
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
from app.connectors.akshare_adapter import AkshareAdapter
from app.connectors.registry import get_adapter
from app.db.session import get_db
from app.models.market_data import (
    DataFetchLog,
    DataSource,
    DataSyncLog,
    StockFinancialReport,
    StockKline,
    StockNews,
    StockRealtimeQuote,
    StockNotice,
    StockSymbol,
    WatchlistItem,
)
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
from app.services.catalog import select_data_source
from app.services.stock_on_demand import OnDemandFetchError, StockOnDemandService, _merge_f10_payload
from app.services.stock_sync import StockSyncService

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


def _stock_symbol_read(stock: StockSymbol, quote: Any | None = None) -> dict[str, Any]:
    return {
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


def _stock_symbol_view(stock: StockSymbol, quote: Any | None = None) -> SimpleNamespace:
    return SimpleNamespace(**_stock_symbol_read(stock, quote))


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

F10_EXTENDED_SECTIONS = (
    "published_reports",
    "profile",
    "holders",
    "fund_flow",
    "financial_summary",
    "financial_statements",
    "business_composition",
)


def _get_source(db: Session, source_code: str) -> DataSource:
    source = db.scalar(select(DataSource).where(DataSource.source_code == source_code))
    if not source:
        raise HTTPException(status_code=404, detail=f"Data source not found: {source_code}")
    if not source.enabled:
        raise HTTPException(status_code=409, detail=f"Data source is disabled: {source_code}")
    return source


def _empty_extended_data(message: str) -> dict[str, dict]:
    """Keep optional F10 modules available even when an upstream source is unavailable."""
    return {
        "profile": {"source": "暂无", "fields": {}, "message": message},
        "holders": {"source": "暂无", "major": [], "circulating": [], "message": message},
        "fund_flow": {"source": "暂无", "rows": [], "message": message},
        "financial_summary": {"source": "暂无", "periods": [], "rows": [], "message": message},
        "financial_statements": {
            "source": "暂无",
            "balance_sheet": {"label": "资产负债表", "periods": []},
            "income_statement": {"label": "利润表", "periods": []},
            "cash_flow": {"label": "现金流量表", "periods": []},
            "message": message,
        },
        "business_composition": {
            "source": "暂无",
            "report_date": None,
            "sections": [],
            "message": message,
        },
        "published_reports": {
            "source": "暂无",
            "reports": [],
            "message": message,
        },
    }


def _load_f10_extended_data(db: Session, market: str, symbol: str) -> dict[str, dict]:
    cached = StockOnDemandService(db).load_f10_cache(market, symbol)
    extended_data = _empty_extended_data("本地暂无缓存，可点击“拉取最新”获取。")
    for section in F10_EXTENDED_SECTIONS:
        payload = cached.get(section)
        if isinstance(payload, dict):
            extended_data[section] = payload
    if cached:
        extended_data["profile"]["message"] = extended_data["profile"].get("message") or "本地缓存已加载。"
    return extended_data


def _section_has_payload(section: str, payload: dict | None, market: str | None = None) -> bool:
    if not isinstance(payload, dict):
        return False
    if section == "profile":
        fields = payload.get("fields")
        if not isinstance(fields, dict) or not fields:
            return False
        if market == "HK" and not any(key in fields for key in ("上市日期", "市盈率", "总市值(港元)", "港股市值(港元)")):
            return False
        return True
    if section == "holders":
        return bool(payload.get("major")) or bool(payload.get("circulating")) or bool(payload.get("official_links"))
    if section == "fund_flow":
        return bool(payload.get("rows"))
    if section == "financial_summary":
        return bool(payload.get("rows"))
    if section == "financial_statements":
        for key in ("balance_sheet", "income_statement", "cash_flow"):
            statement = payload.get(key)
            if isinstance(statement, dict) and bool(statement.get("periods") or statement.get("rows")):
                return True
        return False
    if section == "business_composition":
        return bool(payload.get("sections")) or str(payload.get("source") or "") not in {"", "暂无"}
    if section == "published_reports":
        reports = payload.get("reports")
        if not isinstance(reports, list):
            return False
        return any(
            isinstance(item, dict)
            and str(item.get("report_type") or "").lower()
            not in {"financial_indicators", "announcement"}
            for item in reports
        )
    return False


def _needs_f10_refresh(extended_data: dict[str, dict], market: str | None = None) -> bool:
    if market in (MARKET_NEEQ, MARKET_NEEQ_INNOVATION):
        return any(
            not _section_has_payload(section, extended_data.get(section), market)
            for section in ("profile", "financial_summary", "published_reports")
        )
    if any(not _section_has_payload(section, extended_data.get(section), market) for section in F10_EXTENDED_SECTIONS):
        return True
    if market == "HK":
        if _hk_fund_flow_is_stale(extended_data.get("fund_flow")):
            return True
        if _hk_published_reports_need_refresh(extended_data.get("published_reports")):
            return True
    return False


def _hk_fund_flow_is_stale(payload: dict | None) -> bool:
    if not isinstance(payload, dict):
        return True
    rows = payload.get("rows")
    if not isinstance(rows, list) or not rows:
        return True
    latest_text = ""
    for row in rows:
        if not isinstance(row, dict):
            continue
        value = row.get("持股日期") or row.get("日期") or row.get("date")
        if value:
            latest_text = max(latest_text, str(value).split(" ", 1)[0])
    if not latest_text:
        return True
    try:
        latest_date = date.fromisoformat(latest_text[:10])
    except ValueError:
        return False
    return (date.today() - latest_date).days > 14


def _hk_published_reports_need_refresh(
    payload: dict | None,
    financial_summary: dict | None = None,
    financial_statements: dict | None = None,
    notices: list[StockNotice] | None = None,
) -> bool:
    if not isinstance(payload, dict):
        return True
    reports = payload.get("reports")
    if not isinstance(reports, list) or not reports:
        return True
    valid_reports = [item for item in reports if isinstance(item, dict)]
    if not valid_reports:
        return True
    latest = max(
        valid_reports,
        key=lambda item: (
            str(item.get("report_date") or ""),
            str(item.get("notice_date") or ""),
        ),
    )
    latest_report_date = str(latest.get("report_date") or "").strip()
    if not bool(str(latest.get("url") or "").strip()):
        return True

    latest_known_period = ""
    if isinstance(financial_summary, dict):
        periods = financial_summary.get("periods")
        if isinstance(periods, list):
            latest_known_period = max((str(item or "").strip() for item in periods if str(item or "").strip()), default="")
    if not latest_known_period and isinstance(financial_statements, dict):
        for key in ("balance_sheet", "income_statement", "cash_flow"):
            statement = financial_statements.get(key)
            if not isinstance(statement, dict):
                continue
            periods = statement.get("periods")
            if isinstance(periods, list):
                candidate = max((str(item.get("报告日期") or item.get("report_date") or "").strip() for item in periods if isinstance(item, dict)), default="")
                if candidate > latest_known_period:
                    latest_known_period = candidate

    if notices:
        for item in notices:
            if not isinstance(item, StockNotice):
                continue
            combined_text = f"{item.title or ''} {item.notice_type or ''}".strip()
            if not combined_text or "月报" in combined_text or "月報" in combined_text:
                continue
            if AkshareAdapter._is_hk_financial_report_title(combined_text):
                report_type = AkshareAdapter._normalize_hk_report_type(AkshareAdapter._guess_hk_report_type(combined_text))
                report_date = AkshareAdapter._guess_hk_report_date(combined_text, report_type, item.notice_date)
                if report_date and report_date[:10] > latest_known_period:
                    return True

    return bool(latest_known_period and latest_report_date and latest_report_date < latest_known_period)


def _merge_f10_extended_data(base: dict[str, dict], fresh: dict[str, dict]) -> dict[str, dict]:
    merged = dict(base)
    for section in F10_EXTENDED_SECTIONS:
        payload = fresh.get(section)
        if isinstance(payload, dict) and payload:
            existing = merged.get(section)
            merged[section] = _merge_f10_payload(section, existing if isinstance(existing, dict) else None, payload)
    return merged


def _hk_report_notice_priority(text: str) -> int:
    normalized = text.replace(" ", "")
    if "月报" in normalized or "月報" in normalized:
        return -1000
    score = 0
    if "财务报表" in normalized or "財務報表" in normalized:
        score += 120
    if any(keyword in normalized for keyword in ("年度报告", "年度報告", "中期报告", "中期報告", "半年度报告", "半年度報告", "年报", "年報", "半年报", "半年報")):
        score += 100
    if "业绩" in normalized or "業績" in normalized:
        score += 60
    if "股息" in normalized or "分派" in normalized:
        score -= 20
    return score


def _prefer_hk_report_notice(candidate: dict, current: dict | None) -> bool:
    if not current:
        return True
    candidate_priority = int(candidate.get("_priority") or _hk_report_notice_priority(str(candidate.get("title") or "")))
    current_priority = int(current.get("_priority") or _hk_report_notice_priority(str(current.get("title") or "")))
    if candidate_priority != current_priority:
        return candidate_priority > current_priority
    candidate_has_link = bool(str(candidate.get("url") or "").strip())
    current_has_link = bool(str(current.get("url") or "").strip())
    if candidate_has_link != current_has_link:
        return candidate_has_link
    return str(candidate.get("notice_date") or "") > str(current.get("notice_date") or "")


def _notice_to_hk_report_entry(notice: StockNotice, symbol: str) -> dict[str, Any] | None:
    combined_text = f"{notice.title or ''} {notice.notice_type or ''}".strip()
    if not combined_text or _hk_report_notice_priority(combined_text) < 0:
        return None
    if not AkshareAdapter._is_hk_financial_report_title(combined_text):
        return None
    report_type = AkshareAdapter._normalize_hk_report_type(AkshareAdapter._guess_hk_report_type(combined_text))
    report_date = AkshareAdapter._guess_hk_report_date(combined_text, report_type, notice.notice_date)
    if not report_date:
        return None
    entry = AkshareAdapter._normalize_hk_financial_report_entry(
        symbol=symbol,
        report_type=report_type,
        report_date=report_date[:10],
        notice_date=notice.notice_date,
        url=notice.url,
        source_name="HKEXnews",
        title=notice.title,
    )
    entry["_priority"] = _hk_report_notice_priority(combined_text)
    return entry


def _merge_local_hk_report_notices(payload: dict | None, notices: list[StockNotice], symbol: str) -> dict[str, Any]:
    merged_payload = dict(payload or {})
    reports = merged_payload.get("reports")
    reports_by_key: dict[tuple[str, str], dict[str, Any]] = {}
    if isinstance(reports, list):
        for item in reports:
            if not isinstance(item, dict):
                continue
            report_date = str(item.get("report_date") or item.get("notice_date") or "").strip()[:10]
            report_type = AkshareAdapter._normalize_hk_report_type(item.get("report_type"))
            if report_date:
                reports_by_key[(report_date, report_type)] = dict(item)

    for notice in notices:
        entry = _notice_to_hk_report_entry(notice, symbol)
        if not entry:
            continue
        key = (str(entry.get("report_date") or "")[:10], AkshareAdapter._normalize_hk_report_type(entry.get("report_type")))
        current = reports_by_key.get(key)
        if not _prefer_hk_report_notice(entry, current):
            continue
        merged = entry if current is None else AkshareAdapter._merge_hk_financial_report_entry(current, entry)
        merged["_priority"] = entry.get("_priority")
        reports_by_key[key] = merged

    merged_reports = list(reports_by_key.values())
    for item in merged_reports:
        item.pop("_priority", None)
    merged_reports.sort(key=lambda row: (row.get("report_date") or "", row.get("notice_date") or ""), reverse=True)
    merged_payload["reports"] = merged_reports
    if merged_reports:
        merged_payload["source"] = "东方财富港股财报 / HKEXnews"
        merged_payload["message"] = ""
    return merged_payload


def _is_cn_report_notice(notice: StockNotice) -> bool:
    text = f"{notice.title or ''} {notice.notice_type or ''}".replace(" ", "")
    if not text:
        return False
    excluded = (
        "问询函",
        "问询回复",
        "回复函",
        "回复公告",
        "更正公告",
        "更正后",
        "取消公告",
        "延期披露",
        "提示性公告",
        "摘要说明",
        "月报",
    )
    if any(keyword in text for keyword in excluded):
        return False
    return any(
        keyword in text
        for keyword in (
            "年度报告",
            "年报",
            "半年度报告",
            "半年报",
            "中期报告",
            "季度报告",
            "第一季度报告",
            "第三季度报告",
            "一季报",
            "三季报",
            "财务报告",
            "财务报表",
            "审计报告",
        )
    )


def _cn_report_type(title: str) -> str:
    compact = title.replace(" ", "")
    if any(keyword in compact for keyword in ("第一季度报告", "第三季度报告", "季度报告", "一季报", "三季报")):
        return "quarterly_report"
    if any(keyword in compact for keyword in ("半年度报告", "半年报", "中期报告")):
        return "semiannual_report"
    if any(keyword in compact for keyword in ("年度报告", "年报")):
        return "annual_report"
    if "审计报告" in compact:
        return "audit_report"
    return "financial_report"


def _notice_to_financial_report_entry(notice: StockNotice) -> dict[str, Any] | None:
    if notice.market == "HK":
        return _notice_to_hk_report_entry(notice, notice.symbol)
    if notice.market in (MARKET_NEEQ, MARKET_NEEQ_INNOVATION):
        checker = AkshareAdapter._is_neeq_report_notice
        if not checker(
            # The helper only reads title/type, so the ORM object is compatible
            # with the connector dataclass shape for this lightweight check.
            notice  # type: ignore[arg-type]
        ):
            return None
    elif not _is_cn_report_notice(notice):
        return None

    title = str(notice.title or "").strip()
    if not title:
        return None
    return {
        "report_name": title,
        "report_type": _cn_report_type(title),
        "report_date": notice.notice_date,
        "notice_date": notice.notice_date,
        "url": notice.url,
        "source_name": "Eastmoney NEEQ announcements"
        if notice.market in (MARKET_NEEQ, MARKET_NEEQ_INNOVATION)
        else "CNINFO",
        "title": title,
    }


def _merge_local_report_notices(
    payload: dict | None,
    notices: list[StockNotice],
    market: str,
    symbol: str,
) -> dict[str, Any]:
    if market == "HK":
        return _merge_local_hk_report_notices(payload, notices, symbol)

    merged_payload = dict(payload or {})
    reports_by_key: dict[tuple[str, str, str], dict[str, Any]] = {}
    existing_reports = merged_payload.get("reports")
    if isinstance(existing_reports, list):
        for item in existing_reports:
            if not isinstance(item, dict):
                continue
            report_date = str(item.get("report_date") or item.get("notice_date") or "").strip()[:10]
            title = str(item.get("title") or item.get("report_name") or "").strip()
            report_type = str(item.get("report_type") or "financial_report").strip()
            # Drop pseudo indicator rows from the formal report tab.
            if not title or report_type == "financial_indicators":
                continue
            reports_by_key[(report_date, report_type, title)] = dict(item)

    for notice in notices:
        entry = _notice_to_financial_report_entry(notice)
        if not entry:
            continue
        entry_title = str(entry.get("title") or entry.get("report_name") or "").strip()
        title_match = next(
            (
                key
                for key, value in reports_by_key.items()
                if entry_title
                and entry_title
                == str(value.get("title") or value.get("report_name") or "").strip()
            ),
            None,
        )
        if title_match:
            current = reports_by_key[title_match]
            if entry.get("url") and not current.get("url"):
                reports_by_key[title_match] = {**current, **entry}
            elif entry.get("url") and str(current.get("url") or "").find("cninfo.com.cn") < 0:
                reports_by_key[title_match] = {**current, "url": entry["url"], "notice_date": entry.get("notice_date")}
            continue
        key = (
            str(entry.get("report_date") or entry.get("notice_date") or "")[:10],
            str(entry.get("report_type") or "financial_report"),
            str(entry.get("title") or entry.get("report_name") or ""),
        )
        existing = reports_by_key.get(key)
        if not existing or (entry.get("url") and not existing.get("url")):
            reports_by_key[key] = entry if not existing else {**existing, **entry}

    reports = list(reports_by_key.values())
    reports.sort(
        key=lambda row: (
            str(row.get("report_date") or ""),
            str(row.get("notice_date") or ""),
        ),
        reverse=True,
    )
    merged_payload["reports"] = reports[:200]
    if reports:
        if market in (MARKET_NEEQ, MARKET_NEEQ_INNOVATION):
            merged_payload["source"] = "Eastmoney NEEQ announcements"
        else:
            merged_payload["source"] = "CNINFO official disclosures"
        merged_payload["message"] = ""
    else:
        merged_payload["message"] = merged_payload.get("message") or "暂无正式财报披露文件。财务指标请查看“财务”页签。"
    return merged_payload


LIST_DATE_KEYS = (
    "上市日期",
    "上市时间",
    "上市日",
    "A股上市日期",
    "首发上市日期",
    "挂牌日期",
    "LIST_DATE",
    "list_date",
    "listing_date",
    "Listing Date",
    "IPO_DATE",
    "ipo_date",
    "f26",
)


def _normalize_stock_list_date(value: Any) -> str | None:
    if value in (None, ""):
        return None
    if isinstance(value, date):
        return value.isoformat()
    text = str(value).strip()
    if not text or text.lower() in {"none", "nan", "nat", "--", "-"}:
        return None
    text = text.replace("/", "-").replace(".", "-")
    digits = "".join(ch for ch in text if ch.isdigit())
    if len(digits) >= 8:
        candidate = f"{digits[:4]}-{digits[4:6]}-{digits[6:8]}"
        try:
            return date.fromisoformat(candidate).isoformat()
        except ValueError:
            pass
    if len(text) >= 10:
        candidate = text[:10]
        try:
            return date.fromisoformat(candidate).isoformat()
        except ValueError:
            return candidate
    return None


def _extract_stock_list_date(stock: StockSymbol, extended_data: dict[str, dict]) -> str | None:
    payloads: list[dict[str, Any]] = []
    profile = extended_data.get("profile")
    if isinstance(profile, dict) and isinstance(profile.get("fields"), dict):
        payloads.append(profile["fields"])
    if isinstance(stock.ext_json, dict):
        payloads.append(stock.ext_json)
        profile_fields = stock.ext_json.get("f10_profile_fields")
        if isinstance(profile_fields, dict):
            payloads.append(profile_fields)
    if isinstance(stock.raw_payload, dict):
        payloads.append(stock.raw_payload)

    lowered_keys = {key.lower() for key in LIST_DATE_KEYS}
    for payload in payloads:
        for key in LIST_DATE_KEYS:
            parsed = _normalize_stock_list_date(payload.get(key))
            if parsed:
                return parsed
        for key, value in payload.items():
            key_text = str(key).strip()
            normalized_key = key_text.lower()
            looks_like_list_date = (
                normalized_key in lowered_keys
                or ("上市" in key_text and ("日期" in key_text or "时间" in key_text or "日" in key_text))
                or ("挂牌" in key_text and ("日期" in key_text or "时间" in key_text or "日" in key_text))
            )
            if looks_like_list_date:
                parsed = _normalize_stock_list_date(value)
                if parsed:
                    return parsed
    return None


def _hydrate_symbol_from_f10(stock: StockSymbol, extended_data: dict[str, dict]) -> bool:
    updated = False
    list_date = _extract_stock_list_date(stock, extended_data)
    if list_date and stock.list_date != list_date:
        stock.list_date = list_date
        updated = True
    profile = extended_data.get("profile")
    if isinstance(profile, dict) and isinstance(profile.get("fields"), dict):
        ext_json = dict(stock.ext_json or {})
        ext_json["f10_profile_fields"] = profile["fields"]
        ext_json["f10_profile_source"] = profile.get("source")
        stock.ext_json = ext_json
        updated = True
    return updated


def _fetch_and_cache_f10_extended_data(
    db: Session,
    source: DataSource,
    market: str,
    symbol: str,
) -> dict[str, dict]:
    extended_data = get_adapter(source.adapter_type).fetch_extended_data(market, symbol)
    service = StockOnDemandService(db)
    for section in F10_EXTENDED_SECTIONS:
        payload = extended_data.get(section)
        if isinstance(payload, dict):
            service.upsert_f10_cache(source, market, symbol, section, payload)
    return {**_empty_extended_data("本地暂无缓存，可点击“拉取最新”获取。"), **extended_data}


def _ensure_neeq_f10_from_core(
    db: Session,
    source: DataSource | None,
    market: str,
    symbol: str,
    quote: StockRealtimeQuote | None,
    financials: list[StockFinancialReport],
    notices: list[StockNotice],
    extended_data: dict[str, dict],
) -> dict[str, dict]:
    """Fill NEEQ F10 sections from core rows when optional APIs are rate-limited."""
    if market not in (MARKET_NEEQ, MARKET_NEEQ_INNOVATION):
        return extended_data

    merged = dict(extended_data)
    service = StockOnDemandService(db)

    if quote:
        raw_payload = quote.raw_payload if isinstance(quote.raw_payload, dict) else {}
        raw_quote = raw_payload.get("quote") if isinstance(raw_payload.get("quote"), dict) else {}
        derived = raw_payload.get("derived") if isinstance(raw_payload.get("derived"), dict) else {}
        fields = {
            "symbol": symbol,
            "market": market,
            "current_price": quote.current_price,
            "previous_close": quote.previous_close_price,
            "open_price": quote.open_price,
            "high_price": quote.high_price,
            "low_price": quote.low_price,
            "volume": quote.volume,
            "amount": quote.amount,
            "change_amount": quote.change_amount,
            "change_pct": quote.change_pct,
            "turnover_rate": quote.turnover_rate,
            "quote_date": quote.quote_time,
            "total_market_value": raw_quote.get("MarketValue") or derived.get("total_market_cap_yi"),
            "float_market_value": raw_quote.get("FlowCapitalValue") or derived.get("float_market_cap_yi"),
            "pe_ratio": raw_quote.get("PERation") or derived.get("pe_ratio"),
            "pb_ratio": raw_quote.get("PB") or derived.get("pb_ratio"),
            "latest_price_source": raw_payload.get("latest_price_source"),
            "no_trade_today": raw_payload.get("no_trade_today"),
        }
        fields = {key: value for key, value in fields.items() if value not in (None, "")}
        if fields:
            profile_payload = {
                "source": "Eastmoney NEEQ quote fallback",
                "fields": fields,
                "message": (
                    "当日无成交时，最新价使用上一交易日收盘价。"
                    if raw_payload.get("no_trade_today")
                    else ""
                ),
            }
            if source:
                service.upsert_f10_cache(source, market, symbol, "profile", profile_payload)
            merged["profile"] = _merge_f10_payload(
                "profile",
                merged.get("profile") if isinstance(merged.get("profile"), dict) else None,
                profile_payload,
            )
            merged["profile"]["message"] = profile_payload.get("message") or ""

    if financials:
        metric_fields = (
            ("revenue", "INCOME"),
            ("net_profit", "PROFIT"),
            ("eps", "EPSJB"),
            ("book_value_per_share", "BPS"),
            ("gross_margin_pct", "XSMLL"),
            ("net_margin_pct", "XSJLL"),
            ("debt_asset_ratio_pct", "ZCFZL"),
            ("roe_pct", "ROEKCJQ"),
            ("revenue_yoy_pct", "YSTZ"),
            ("profit_yoy_pct", "SJLTZ"),
            ("pe_ratio", "PELYR"),
            ("pb_ratio", "PBMRQ"),
        )
        latest = max(
            financials,
            key=lambda item: (
                sum(
                    1
                    for _, field in metric_fields
                    if isinstance(item.data_json, dict) and item.data_json.get(field) not in (None, "")
                ),
                str(item.report_period or ""),
            ),
        )
        raw = latest.data_json if isinstance(latest.data_json, dict) else {}
        rows = [
            {"metric": metric, "data": {latest.report_period: raw[field]}}
            for metric, field in metric_fields
            if raw.get(field) not in (None, "")
        ]
        if rows:
            financial_payload = {
                "source": "Eastmoney NEEQ financial indicators",
                "periods": [latest.report_period],
                "rows": rows,
                "latest": {
                    "report_date": latest.report_period,
                    "report_name": "NEEQ financial indicators",
                },
                "message": "",
            }
            if source:
                service.upsert_f10_cache(source, market, symbol, "financial_summary", financial_payload)
            existing_summary = merged.get("financial_summary") if isinstance(merged.get("financial_summary"), dict) else None
            existing_rows = existing_summary.get("rows") if isinstance(existing_summary, dict) else []
            if not isinstance(existing_rows, list) or len(rows) > len(existing_rows):
                # A quote-indicator fallback can have today's date but only
                # two valuation fields. Prefer a richer actual financial
                # report even when its disclosure date is earlier.
                merged["financial_summary"] = financial_payload
            else:
                merged["financial_summary"] = _merge_f10_payload(
                    "financial_summary",
                    existing_summary,
                    financial_payload,
                )
            merged["financial_summary"]["message"] = ""

    reports_payload = _merge_local_report_notices(
        merged.get("published_reports"),
        notices,
        market,
        symbol,
    )
    if "reports" in reports_payload:
        if source:
            service.upsert_f10_cache(source, market, symbol, "published_reports", reports_payload)
        merged["published_reports"] = _merge_f10_payload(
            "published_reports",
            merged.get("published_reports") if isinstance(merged.get("published_reports"), dict) else None,
            reports_payload,
        )
        merged["published_reports"]["message"] = ""

    return merged


@router.post("/sync", response_model=StockSyncResponse)
def sync_stock_symbols(payload: StockSyncRequest, db: Session = Depends(get_db)) -> StockSyncResponse:
    source = _get_source(db, payload.source_code)

    markets = list(MASTER_MARKETS) if payload.market == "ALL" else [payload.market]
    logs = []
    attempted_source_codes: list[str] = []
    for market in markets:
        sync_log = StockSyncService(db).synchronize(source, market, enable_fallback=payload.enable_fallback)
        logs.append(sync_log)
        route_trace = (sync_log.detail_json or {}).get("route_trace") or []
        if isinstance(route_trace, list) and route_trace:
            attempted_source_codes.extend(
                str(item.get("source_code")) for item in route_trace if isinstance(item, dict) and item.get("source_code")
            )
        else:
            attempted_source_codes.append(source.source_code)

    has_failure = any(item.status == "FAILED" for item in logs)
    has_fallback_failure = any(
        isinstance(trace_item, dict) and trace_item.get("status") == "FAILED"
        for log in logs
        for trace_item in ((log.detail_json or {}).get("route_trace") or [])
    )
    has_success = any(item.status == "SUCCESS" for item in logs)
    return StockSyncResponse(
        sync_log_ids=[item.id for item in logs],
        attempted_source_codes=attempted_source_codes,
        status="SUCCESS_WITH_FALLBACK" if has_success and (has_failure or has_fallback_failure) else ("FAILED" if not has_success else "SUCCESS"),
        message="Synchronization finished. Check data-sync logs for provider failures and fallback attempts.",
    )


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
    return StockSymbolPage(
        items=[
            _stock_symbol_read(item, latest_quotes.get((item.market, item.symbol)))
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
        _stock_symbol_view(item, latest_quotes.get((item.market, item.symbol)))
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
    normalized_market = market.upper()
    normalized_symbol = _normalize_symbol(normalized_market, symbol)
    if normalized_market not in MASTER_MARKETS:
        raise HTTPException(status_code=422, detail=f"market must be one of: {', '.join(MASTER_MARKETS)}")
    if min(financial_limit, notice_limit, news_limit) < 1 or max(financial_limit, notice_limit, news_limit) > 100:
        raise HTTPException(status_code=422, detail="F10 detail limits must be between 1 and 100")
    if notice_page < 1 or news_page < 1:
        raise HTTPException(status_code=422, detail="notice_page and news_page must be >= 1")
    if kline_limit < 1 or kline_limit > 5000:
        raise HTTPException(status_code=422, detail="kline_limit must be between 1 and 5000")

    stock = db.scalar(
        select(StockSymbol).where(
            StockSymbol.market == normalized_market,
            StockSymbol.symbol == normalized_symbol,
        )
    )
    if not stock:
        raise HTTPException(status_code=404, detail="Stock symbol not found")

    klines = db.scalars(
        select(StockKline)
        .where(
            StockKline.market == normalized_market,
            StockKline.symbol == normalized_symbol,
        )
        .order_by(StockKline.trade_date.desc())
        .limit(kline_limit)
    ).all()
    financials = db.scalars(
        select(StockFinancialReport)
        .where(
            StockFinancialReport.market == normalized_market,
            StockFinancialReport.symbol == normalized_symbol,
        )
        .order_by(StockFinancialReport.report_period.desc())
        .limit(financial_limit)
    ).all()
    stale_report_notices = db.scalars(
        select(StockNotice)
        .where(
            StockNotice.market == normalized_market,
            StockNotice.symbol == normalized_symbol,
        )
        .order_by(StockNotice.notice_date.desc(), StockNotice.id.desc())
        .limit(500)
    ).all()
    notices = list(stale_report_notices[(notice_page - 1) * notice_limit : notice_page * notice_limit])
    notice_total = db.scalar(
        select(func.count()).select_from(StockNotice).where(
            StockNotice.market == normalized_market,
            StockNotice.symbol == normalized_symbol,
        )
    ) or 0
    quote = db.scalar(
        select(StockRealtimeQuote)
        .where(StockRealtimeQuote.market == normalized_market, StockRealtimeQuote.symbol == normalized_symbol)
        .order_by(StockRealtimeQuote.fetched_at.desc())
    )
    news = db.scalars(
        select(StockNews)
        .where(StockNews.market == normalized_market, StockNews.symbol == normalized_symbol)
        .order_by(StockNews.news_time.desc())
        .offset((news_page - 1) * news_limit)
        .limit(news_limit)
    ).all()
    news_total = db.scalar(
        select(func.count()).select_from(StockNews).where(
            StockNews.market == normalized_market,
            StockNews.symbol == normalized_symbol,
        )
    ) or 0
    extended_data = _load_f10_extended_data(db, normalized_market, normalized_symbol)

    # A manual refresh must update the core tables as well as the extended F10
    # cache. The frontend can then use one response that is guaranteed to come
    # from the data just persisted in the local database.
    core_source = db.scalar(
        select(DataSource).where(
            DataSource.source_code == "AKSHARE",
            DataSource.enabled.is_(True),
        )
    )
    source = select_data_source(db, normalized_market, "F10", fallback_code="AKSHARE")
    if refresh and core_source and normalized_market in LIVE_REFRESH_MARKETS:
        refresh_errors = StockOnDemandService(db).refresh_stock_data(
            source=core_source,
            market=normalized_market,
            symbol=normalized_symbol,
            list_date=stock.list_date,
        )
        if refresh_errors:
            fallback_message = f"部分行情数据刷新失败，已使用本地已保存数据：{'; '.join(refresh_errors)[:180]}"
            for section in F10_EXTENDED_SECTIONS:
                if isinstance(extended_data.get(section), dict):
                    extended_data[section]["message"] = (
                        extended_data[section].get("message") or fallback_message
                    )

        # Refresh the extended F10 sections in the same explicit refresh
        # request. Each section is persisted before it is read back below.
        try:
            _fetch_and_cache_f10_extended_data(
                db,
                source,
                normalized_market,
                normalized_symbol,
            )
        except Exception as exc:
            refresh_errors.append(f"f10: {str(exc)[:240]}")

        # Core and F10 refreshes commit independently. Always load the local
        # cache again so the response is built from persisted data.
        extended_data = _load_f10_extended_data(db, normalized_market, normalized_symbol)
        stale_report_notices = db.scalars(
            select(StockNotice)
            .where(
                StockNotice.market == normalized_market,
                StockNotice.symbol == normalized_symbol,
            )
            .order_by(StockNotice.notice_date.desc(), StockNotice.id.desc())
            .limit(500)
        ).all()

    # Normal detail opens must prefer the local database. Only an explicit
    # refresh, or a genuinely missing section, is allowed to call upstream
    # providers. This prevents an older provider response from overwriting a
    # newer snapshot that was just persisted by the user.
    should_refresh = (not refresh) and (not local_only) and _needs_f10_refresh(extended_data, normalized_market)
    if should_refresh:
        if source:
            try:
                fresh_extended_data = _fetch_and_cache_f10_extended_data(db, source, normalized_market, normalized_symbol)
                extended_data = _merge_f10_extended_data(
                    _load_f10_extended_data(db, normalized_market, normalized_symbol),
                    fresh_extended_data,
                )
            except Exception as exc:
                fallback_message = f"远端扩展资料刷新失败，已读取本地缓存：{str(exc)[:180]}"
                for section in F10_EXTENDED_SECTIONS:
                    if isinstance(extended_data.get(section), dict):
                        extended_data[section]["message"] = extended_data[section].get("message") or fallback_message

    merged_reports = _merge_local_report_notices(
        extended_data.get("published_reports"),
        list(stale_report_notices),
        normalized_market,
        normalized_symbol,
    )
    extended_data["published_reports"] = merged_reports
    if not local_only:
        if merged_reports.get("reports"):
            extended_data["published_reports"] = merged_reports
            if source:
                StockOnDemandService(db).upsert_f10_cache(
                    source,
                    normalized_market,
                    normalized_symbol,
                    "published_reports",
                    merged_reports,
                )

    # Refreshes and report merges may have changed cached values. Always read
    # the response objects after persistence so this request cannot return a
    # stale ORM snapshot captured before the refresh.
    if refresh:
        stock = db.scalar(
            select(StockSymbol).where(
                StockSymbol.market == normalized_market,
                StockSymbol.symbol == normalized_symbol,
            )
        ) or stock
        klines = db.scalars(
            select(StockKline)
            .where(
                StockKline.market == normalized_market,
                StockKline.symbol == normalized_symbol,
            )
            .order_by(StockKline.trade_date.desc())
            .limit(kline_limit)
        ).all()
        financials = db.scalars(
            select(StockFinancialReport)
            .where(
                StockFinancialReport.market == normalized_market,
                StockFinancialReport.symbol == normalized_symbol,
            )
            .order_by(StockFinancialReport.report_period.desc())
            .limit(financial_limit)
        ).all()
        notices = db.scalars(
            select(StockNotice)
            .where(
                StockNotice.market == normalized_market,
                StockNotice.symbol == normalized_symbol,
            )
            .order_by(StockNotice.notice_date.desc(), StockNotice.id.desc())
            .offset((notice_page - 1) * notice_limit)
            .limit(notice_limit)
        ).all()
        notice_total = db.scalar(
            select(func.count()).select_from(StockNotice).where(
                StockNotice.market == normalized_market,
                StockNotice.symbol == normalized_symbol,
            )
        ) or 0
        quote = db.scalar(
            select(StockRealtimeQuote)
            .where(
                StockRealtimeQuote.market == normalized_market,
                StockRealtimeQuote.symbol == normalized_symbol,
            )
            .order_by(StockRealtimeQuote.fetched_at.desc())
        )
        news = db.scalars(
            select(StockNews)
            .where(
                StockNews.market == normalized_market,
                StockNews.symbol == normalized_symbol,
            )
            .order_by(StockNews.news_time.desc(), StockNews.id.desc())
            .offset((news_page - 1) * news_limit)
            .limit(news_limit)
        ).all()
        news_total = db.scalar(
            select(func.count()).select_from(StockNews).where(
                StockNews.market == normalized_market,
                StockNews.symbol == normalized_symbol,
            )
        ) or 0
        extended_data = _load_f10_extended_data(db, normalized_market, normalized_symbol)

        extended_data["published_reports"] = _merge_local_report_notices(
            extended_data.get("published_reports"),
            list(stale_report_notices),
            normalized_market,
            normalized_symbol,
        )

    extended_data = _ensure_neeq_f10_from_core(
        db=db,
        source=source,
        market=normalized_market,
        symbol=normalized_symbol,
        quote=quote,
        financials=list(financials),
        notices=list(stale_report_notices),
        extended_data=extended_data,
    )

    if _hydrate_symbol_from_f10(stock, extended_data):
        db.commit()

    return StockF10Read(
        symbol=_stock_symbol_read(stock, quote),
        realtime_quote=quote,
        recent_klines=list(klines),
        financial_reports=list(financials),
        notices=list(notices),
        news=list(news),
        notice_total=notice_total,
        notice_page=notice_page,
        news_total=news_total,
        news_page=news_page,
        published_reports=extended_data.get("published_reports") or {},
        profile=extended_data.get("profile") or {},
        holders=extended_data.get("holders") or {},
        fund_flow=extended_data.get("fund_flow") or {},
        financial_summary=extended_data.get("financial_summary") or {},
        financial_statements=extended_data.get("financial_statements") or {},
        business_composition=extended_data.get("business_composition") or {},
    )


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
    return _stock_symbol_read(item, quote)
