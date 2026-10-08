"""Listing taxonomy without changing historical market/symbol identities."""
from __future__ import annotations

from datetime import datetime, timezone
from typing import Any

from sqlalchemy import and_, case, select
from sqlalchemy.orm import Session

from app.models.market_data import StockSymbol

BOARD_DEFINITIONS = (
    {"code": "MAIN", "market": "CN_A", "name": "主板", "exchanges": ["SSE", "SZSE"], "definition": "上海、深圳证券交易所主板 A 股；包含原中小板并入深市主板的股票。"},
    {"code": "STAR", "market": "CN_A", "name": "科创板", "exchanges": ["SSE"], "definition": "上海证券交易所科创板，主要服务符合定位的科技创新企业；包含科创板存托凭证。"},
    {"code": "CHINEXT", "market": "CN_A", "name": "创业板", "exchanges": ["SZSE"], "definition": "深圳证券交易所创业板，主要服务成长型创新创业企业。"},
    {"code": "BSE", "market": "CN_A", "name": "北交所", "exchanges": ["BSE"], "definition": "北京证券交易所上市股票，属于 A 股；不是新三板挂牌层级，支持 920 新代码和历史 4、8 开头代码。"},
    {"code": "HK_MAIN", "market": "HK", "name": "港股主板", "exchanges": ["HKEX"], "definition": "按港交所官方证券名单认定的主板权益证券，不能仅凭证券代码推断。"},
    {"code": "HK_GEM", "market": "HK", "name": "港股 GEM", "exchanges": ["HKEX"], "definition": "香港交易所 GEM（创业板）权益证券，以港交所官方分类为准。"},
    {"code": "NEEQ_BASE", "market": "NEEQ", "name": "新三板基础层", "exchanges": ["NEEQ"], "definition": "全国股转系统基础层挂牌股票，属于非上市公众公司证券，保留已有 NEEQ 身份。"},
    {"code": "NEEQ_INNOVATION", "market": "NEEQ_INNOVATION", "name": "新三板创新层", "exchanges": ["NEEQ"], "definition": "全国股转系统创新层挂牌股票，是新三板的分层，不是独立证券交易所；保留历史市场键。"},
)
BOARDS = {item["code"]: item for item in BOARD_DEFINITIONS}
EXCHANGES = {"SSE": "上海证券交易所", "SZSE": "深圳证券交易所", "BSE": "北京证券交易所", "HKEX": "香港交易所", "NEEQ": "全国股转系统"}
SECURITY_TYPES = {"STOCK": "股票", "DEPOSITARY_RECEIPT": "存托凭证", "ETF": "交易型基金", "FUND": "基金", "REIT": "房地产投资信托", "WARRANT": "权证", "CBBC": "牛熊证", "BOND": "债券", "OTHER": "其他证券", "UNKNOWN": "待核实证券类型"}
VERSION = "LISTING_TAXONOMY_V1"


def infer_exchange(market: str, symbol: str) -> str:
    if market == "HK":
        return "HKEX"
    if market in {"NEEQ", "NEEQ_INNOVATION"}:
        return "NEEQ"
    if symbol.startswith(("4", "8", "92")):
        return "BSE"
    if symbol.startswith("6"):
        return "SSE"
    if symbol.startswith(("0", "3")):
        return "SZSE"
    return market


def hk_classification(row: dict[str, Any]) -> tuple[str, str]:
    category, sub = str(row.get("Category", "")).lower(), str(row.get("Sub-Category", "")).lower()
    board = "HK_GEM" if sub == "equity securities (gem)" else "HK_MAIN" if sub == "equity securities (main board)" else "UNCLASSIFIED"
    if category == "equity":
        return board, "DEPOSITARY_RECEIPT" if sub == "depositary receipts" else "STOCK"
    if "cbbc" in category or "callable bull" in category:
        return board, "CBBC"
    if "warrant" in category:
        return board, "WARRANT"
    if "debt" in category or "bond" in category:
        return board, "BOND"
    if "real estate" in sub or "reit" in sub or "real estate" in category:
        return board, "REIT"
    if "exchange traded" in sub or "exchange traded" in category or sub == "etfs":
        return board, "ETF"
    if "fund" in category or "unit trust" in category:
        return board, "FUND"
    return board, "OTHER"


def listing_metadata(market: str, symbol: str, ext: dict | None = None, raw: dict | None = None) -> dict[str, Any]:
    ext, raw = ext or {}, raw or {}
    proof = ext.get("listing_classification") or {}
    board, kind, method = "UNCLASSIFIED", "UNKNOWN", "MISSING"
    if market == "CN_A":
        kind, method = "STOCK", "RULE"
        if symbol.startswith(("688", "689")):
            board = "STAR"
            if symbol.startswith("689"):
                kind = "DEPOSITARY_RECEIPT"
        elif symbol.startswith(("300", "301", "302")):
            board = "CHINEXT"
        elif symbol.startswith(("4", "8", "92")):
            board = "BSE"
        elif symbol.startswith(("600", "601", "603", "605", "000", "001", "002", "003")):
            board = "MAIN"
        else:
            method = "MISSING"
        official = proof.get("source_record") or {}
        source_code = proof.get("source_code")
        official_code = str(official.get("证券代码") or official.get("A股代码") or "").zfill(6)
        if source_code in {"SSE_MASTER", "SZSE_MASTER", "BSE_MASTER"} and official_code == symbol:
            source_board = official.get("板块") or proof.get("source_board")
            official_board = {"主板": "MAIN", "主板A股": "MAIN", "科创板": "STAR", "创业板": "CHINEXT", "北交所": "BSE"}.get(source_board)
            if official_board:
                board, method = official_board, "SOURCE"
    elif market in {"NEEQ", "NEEQ_INNOVATION"}:
        board, kind, method = "NEEQ_BASE" if market == "NEEQ" else "NEEQ_INNOVATION", "STOCK", "SOURCE_LAYER"
    elif market == "HK":
        # Accept exact official code matches only. Never classify an 08xxx
        # code as GEM or an unmatched legacy warrant as an ordinary stock.
        official = proof.get("source_record") or {}
        if proof.get("source_code") == "HKEX_SECURITIES" and str(official.get("Stock Code", "")).zfill(5) == symbol:
            board, kind = hk_classification(official)
            method = "SOURCE"
    return {
        "listing_board": board, "listing_board_name": BOARDS.get(board, {}).get("name", "未分类"),
        "market_family": "NEEQ" if market.startswith("NEEQ") else market,
        "market_family_name": {"CN_A": "A股", "HK": "港股", "NEEQ": "新三板", "NEEQ_INNOVATION": "新三板"}.get(market, market),
        "exchange": infer_exchange(market, symbol), "exchange_name": EXCHANGES.get(infer_exchange(market, symbol), "待核实交易所"),
        "security_type": kind, "security_type_name": SECURITY_TYPES[kind],
        "classification_status": "MISSING" if method == "MISSING" else "NORMALIZED",
        "classification_method": method, "classification_version": VERSION,
        "classification_evidence": proof if method == "SOURCE" else {"method": method, "market": market, "symbol": symbol, "source_method": ext.get("_source_method") or raw.get("_source_method"), "source": ext.get("source")},
    }


def normalized_ext(market: str, symbol: str, ext: dict | None = None, raw: dict | None = None) -> dict:
    return {**(ext or {}), "listing": listing_metadata(market, symbol, ext, raw)}


def board_expression():
    s = StockSymbol
    return case(
        (s.market == "NEEQ", "NEEQ_BASE"), (s.market == "NEEQ_INNOVATION", "NEEQ_INNOVATION"),
        (and_(s.market == "CN_A", s.ext_json["listing"]["classification_method"].as_string() == "SOURCE"), s.ext_json["listing"]["listing_board"].as_string()),
        (and_(s.market == "CN_A", s.symbol.startswith("688")), "STAR"),
        (and_(s.market == "CN_A", s.symbol.startswith("689")), "STAR"),
        (and_(s.market == "CN_A", s.symbol.regexp_match(r"^(300|301|302)")), "CHINEXT"),
        (and_(s.market == "CN_A", s.symbol.regexp_match(r"^(4|8|92)")), "BSE"),
        (and_(s.market == "CN_A", s.symbol.regexp_match(r"^(600|601|603|605|000|001|002|003)")), "MAIN"),
        else_=s.ext_json["listing"]["listing_board"].as_string(),
    )


def type_expression():
    return case(
        (and_(StockSymbol.market == "CN_A", StockSymbol.symbol.startswith("689")), "DEPOSITARY_RECEIPT"),
        (StockSymbol.market.in_(["CN_A", "NEEQ", "NEEQ_INNOVATION"]), "STOCK"),
        else_=StockSymbol.ext_json["listing"]["security_type"].as_string(),
    )


def reconcile_master(db: Session, hk_snapshot: dict | None = None) -> dict:
    official = {row["Stock Code"]: row for row in (hk_snapshot or {}).get("records", [])}
    counts: dict[str, int] = {}
    changed = 0
    for stock in db.scalars(select(StockSymbol)).all():
        ext = dict(stock.ext_json or {})
        if stock.market == "HK" and stock.symbol in official:
            ext["listing_classification"] = {
                "source_code": "HKEX_SECURITIES", "source_url": hk_snapshot["source_url"],
                "observed_at": hk_snapshot["retrieved_at"], "source_date_label": hk_snapshot.get("source_date_label"),
                "content_hash": hk_snapshot.get("content_sha256"), "source_record": official[stock.symbol],
            }
        updated = normalized_ext(stock.market, stock.symbol, ext, stock.raw_payload)
        exchange = updated["listing"]["exchange"]
        if updated != stock.ext_json or stock.exchange != exchange:
            stock.ext_json, stock.exchange = updated, exchange
            changed += 1
        board = updated["listing"]["listing_board"]
        counts[board] = counts.get(board, 0) + 1
    db.flush()
    return {"changed": changed, "boards": counts, "verified_at": datetime.now(timezone.utc).isoformat(), "identity_preserved": True}
