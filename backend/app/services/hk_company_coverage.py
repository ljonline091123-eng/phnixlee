"""Complete the current HKEX equity universe without changing existing identities.

The official broad category is preserved. An equity listing is not assumed to
be an ordinary share: rights, temporary counters and preference instruments may
also appear in that category. Historical codes absent from the list remain gaps.
"""
from collections import Counter
from datetime import datetime, timezone
import re

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.connectors.company_master_sources import (
    HKEX_SECURITIES_URL, HKEX_SDW_URL, HK_EQUITY_SUBCATEGORIES, build_master_result,
)
from app.models.company_mapping import CompanyMappingState
from app.models.foundation import FoundationListing
from app.models.market_data import DataSource, StockSymbol
from app.services.company_master import _normalized
from app.services.company_graph import import_batch


def official_equities(hkex: dict) -> dict[str, dict]:
    if hkex.get("source_name") != "HKEX" or hkex.get("source_url") != HKEX_SECURITIES_URL:
        raise ValueError("Expected the archived official HKEX securities list")
    datetime.fromisoformat(hkex["retrieved_at"].replace("Z", "+00:00"))
    rows = hkex.get("records") or []
    codes = [row.get("Stock Code") for row in rows]
    if not rows or any(not re.fullmatch(r"\d{5}", str(code)) for code in codes) or len(codes) != len(set(codes)):
        raise ValueError("Official security codes must be nonempty, unique and five-digit")
    return {row["Stock Code"]: row for row in rows
        if row.get("Category") == "Equity" and row.get("Sub-Category") in HK_EQUITY_SUBCATEGORIES}


def coverage_report(db: Session, hkex: dict) -> dict:
    current = official_equities(hkex)
    rows = list(db.execute(select(StockSymbol, FoundationListing.id)
        .outerjoin(FoundationListing, FoundationListing.stock_symbol_id == StockSymbol.id)
        .where(StockSymbol.market == "HK")))
    indexed = {stock.symbol: (stock, listing) for stock, listing in rows}
    missing_master = sorted(set(current) - set(indexed))
    unmapped = sorted(code for code in current if code in indexed and not indexed[code][1])
    states = {row.stock_symbol_id: row for row in db.scalars(select(CompanyMappingState)
        .join(StockSymbol, StockSymbol.id == CompanyMappingState.stock_symbol_id).where(StockSymbol.market == "HK"))}
    issues = [{"symbol": stock.symbol, "name": stock.name, "status": state.status if state else "SOURCE_MISSING",
        "reason": state.reason if state else "尚无公司映射"} for stock, listing in rows if not listing
        and ((state := states.get(stock.id)) is None or state.status != "NOT_APPLICABLE")]
    mapped = len(current) - len(missing_master) - len(unmapped)
    return {"basis": "HKEX_CURRENT_EQUITY_LIST_NOT_ALL_HK_MASTER_ROWS", "source_url": HKEX_SECURITIES_URL,
        "source_date_label": hkex.get("source_date_label"), "retrieved_at": hkex["retrieved_at"],
        "official_equity_total": len(current), "official_equity_mapped": mapped,
        "official_equity_mapping_complete": not missing_master and not unmapped,
        "official_subcategories": dict(Counter(row["Sub-Category"] for row in current.values())),
        "missing_master_codes": missing_master, "unmapped_current_codes": unmapped,
        "hk_master_total": len(rows), "hk_master_mapped": sum(bool(listing) for _, listing in rows),
        "historical_or_unclassified_pending": issues,
        "instrument_note": "港交所权益类含供股权、临时柜台和优先股；不等同于普通股或独立发行公司数量"}


def mainland_clearing_reference(stock: StockSymbol, all_official_codes: set[str], cn_codes: set[str]) -> dict | None:
    """Accept an explicit CCASS mainland code reference, including old missing-A labels."""
    raw, meta = stock.raw_payload or {}, stock.ext_json or {}
    if (stock.market != "HK" or stock.symbol in all_official_codes
            or meta.get("source_endpoint") != "HKEX_SDW"
            or str(raw.get("c") or "").zfill(5) != stock.symbol):
        return None
    reference = re.search(r"#\s*(\d{6})\)\s*$", str(raw.get("n") or ""))
    if reference is None or reference.group(1) not in cn_codes:
        return None
    return {"source_name": "HKEX_SDW", "source_url": HKEX_SDW_URL,
        "source_record": raw, "referenced_market": "CN_A", "referenced_symbol": reference.group(1),
        "observed_at": stock.last_synced_at.isoformat() if stock.last_synced_at else None,
        "rule": "EXACT_SDW_CODE_EXPLICIT_MAINLAND_REFERENCE_PRESENT_IN_CN_MASTER_ABSENT_HKEX_CURRENT_LIST"}


def completion_plan(db: Session, hkex: dict, pages: list[dict]) -> dict:
    current = official_equities(hkex)
    snapshot = build_master_result([{"market": "HK", "symbol": code} for code in current], pages, hkex)
    stocks = {row.symbol: row for row in db.scalars(select(StockSymbol).where(StockSymbol.market == "HK"))}
    mapped = set(db.scalars(select(FoundationListing.stock_symbol_id)))
    pending = [record for record in snapshot["records"]
        if record["symbol"] not in stocks or stocks[record["symbol"]].id not in mapped]
    all_codes = {row["Stock Code"] for row in hkex["records"]}
    cn_codes = set(db.scalars(select(StockSymbol.symbol).where(StockSymbol.market == "CN_A")))
    states = {row.stock_symbol_id: row for row in db.scalars(select(CompanyMappingState)
        .join(StockSymbol, StockSymbol.id == CompanyMappingState.stock_symbol_id).where(StockSymbol.market == "HK"))}
    exclusions = []
    for stock in stocks.values():
        state = states.get(stock.id)
        if stock.id in mapped or (state is not None and state.status == "NOT_APPLICABLE"):
            continue
        if proof := mainland_clearing_reference(stock, all_codes, cn_codes):
            exclusions.append({"stock_symbol_id": stock.id, "symbol": stock.symbol, **proof})
    return {"before": coverage_report(db, hkex), "records": pending,
        "add_master_codes": [row["symbol"] for row in pending if row["symbol"] not in stocks],
        "unresolved_source_records": snapshot["unresolved"], "clearing_exclusions": exclusions}


def apply_completion(db: Session, hkex: dict, pages: list[dict], *, snapshot_name: str, max_additions: int = 100) -> dict:
    """Flush only. The caller commits the entire small supplementation atomically."""
    plan = completion_plan(db, hkex, pages)
    if len(plan["records"]) > max_additions:
        raise ValueError("Planned mappings exceed the requested bounded import limit")
    observed = datetime.fromisoformat(hkex["retrieved_at"].replace("Z", "+00:00"))
    official = official_equities(hkex)
    source = db.scalar(select(DataSource).where(DataSource.source_code == "HKEX_SECURITIES_SNAPSHOT"))
    if plan["add_master_codes"] and source is None:
        source = DataSource(source_code="HKEX_SECURITIES_SNAPSHOT", source_name="港交所官方证券清单快照",
            source_type="MASTER_DATA", adapter_type="ARCHIVED_SNAPSHOT", enabled=False,
            config_json={"source_url": HKEX_SECURITIES_URL, "refresh_script": "scripts/collect_company_master.py"},
            description="官方权益证券清单；通过采集与补齐脚本更新，未配置自动同步接口。")
        db.add(source)
        db.flush()
    imported = []
    for record in plan["records"]:
        symbol = record["symbol"]
        stock = db.scalar(select(StockSymbol).where(StockSymbol.market == "HK", StockSymbol.symbol == symbol))
        row = official[symbol]
        if stock is None:
            raw = record["source_record"]
            stock = StockSymbol(market="HK", symbol=symbol, exchange="HKEX",
                name=str(raw.get("SECURITY_NAME_ABBR") or row["Name of Securities"]),
                asset_type="EQUITY", status="LISTED", source_id=source.id,
                list_date=str(raw.get("LISTING_DATE") or "")[:10] or None,
                raw_payload=row, last_synced_at=observed,
                ext_json={"source_endpoint": "HKEX_SECURITIES_LIST", "source_url": HKEX_SECURITIES_URL,
                    "source_date_label": hkex.get("source_date_label"), "official_category": row["Category"],
                    "official_subcategory": row["Sub-Category"], "official_security_name": row["Name of Securities"],
                    "instrument_classification": "EQUITY_UNSPECIFIED", "currency": row.get("Trading Currency"),
                    "isin": row.get("ISIN") or None})
            db.add(stock)
            db.flush()
        normalized = _normalized(stock, record, {"retrieved_at": observed.isoformat()})
        # HKEX's general equity category alone does not distinguish common
        # shares from nil-paid rights, temporary counters or preference shares.
        normalized.security.share_class = ("DEPOSITARY_RECEIPT"
            if row["Sub-Category"] == "Depositary Receipts" else "EQUITY_UNSPECIFIED")
        result = import_batch(db, {"records": [normalized]})["records"][0]
        state = db.get(CompanyMappingState, stock.id)
        if state is None:
            state = CompanyMappingState(stock_symbol_id=stock.id, attempt_count=0)
            db.add(state)
        state.status, state.reason = "MAPPED", "依据港交所权益清单及精确来源主体编号建立映射；证券细分类以来源证据为准"
        state.source_name, state.source_snapshot = "HKEX+EASTMONEY", snapshot_name[:512]
        state.attempt_count += 1
        state.attempted_at = datetime.now(timezone.utc)
        state.details_json = {**result, "official_security": row, "source_url": HKEX_SECURITIES_URL}
        imported.append({"symbol": symbol, **result})
    for exclusion in plan["clearing_exclusions"]:
        stock = db.get(StockSymbol, exclusion["stock_symbol_id"])
        state = db.get(CompanyMappingState, stock.id)
        if state is None:
            state = CompanyMappingState(stock_symbol_id=stock.id, attempt_count=0)
            db.add(state)
        state.status, state.reason = "NOT_APPLICABLE", "港交所结算系统明确引用内地证券代码，不是同号港股上市证券"
        state.source_name, state.source_snapshot = "HKEX_SDW", snapshot_name[:512]
        state.attempt_count += 1
        state.attempted_at = datetime.now(timezone.utc)
        state.details_json = exclusion
    db.flush()
    return {"before": plan["before"], "after": coverage_report(db, hkex), "imported": imported,
        "added_master_codes": plan["add_master_codes"], "clearing_exclusions": plan["clearing_exclusions"],
        "unresolved_source_records": plan["unresolved_source_records"]}
