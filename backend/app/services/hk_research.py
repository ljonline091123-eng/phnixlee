"""Independent, local-first Hong Kong research collection and cache update."""
from datetime import datetime, timedelta, timezone
from threading import Lock
from typing import Any

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.connectors.hk_research import fetch_hk_research_sections, hk_symbol
from app.db.session import SessionLocal
from app.models.market_data import DataFetchLog, DataInterface, DataSource, StockF10Cache
from app.services.research_store import load_research_sections, persist_research_sections
from app.services.stock_on_demand import StockOnDemandService

SYNC_INTERFACE = "HK_RESEARCH_BACKGROUND"
_SYNC_LOCK = Lock()
_SYNC_KEYS: set[str] = set()


def _utc(value: datetime) -> datetime:
    return value.replace(tzinfo=timezone.utc) if value.tzinfo is None else value.astimezone(timezone.utc)


def _enabled_interfaces(db: Session) -> dict[str, set[str]]:
    rows = db.execute(select(DataSource.source_code, DataInterface.interface_code).join(
        DataInterface, DataInterface.source_id == DataSource.id,
    ).where(DataSource.source_code.in_(["ETNET_HK", "AASTOCKS_HK"]),
            DataSource.enabled.is_(True), DataInterface.enabled.is_(True))).all()
    result: dict[str, set[str]] = {}
    for source, interface in rows:
        result.setdefault(source, set()).add(interface)
    return result


def hk_research_needs_refresh(db: Session, symbol: str) -> bool:
    cached = db.scalar(select(StockF10Cache).where(StockF10Cache.market == "HK",
        StockF10Cache.symbol == hk_symbol(symbol), StockF10Cache.section == "research_sections"))
    payload = cached.payload_json if cached else {}
    if not payload or not payload.get("fetched_at"):
        return True
    try:
        observed = _utc(datetime.fromisoformat(payload["fetched_at"]))
    except (TypeError, ValueError):
        return True
    failed = any(row.get("status") == "FAILED" for row in payload.get("source_metadata", []) if isinstance(row, dict))
    interval = timedelta(minutes=15) if failed else timedelta(hours=24)
    return datetime.now(timezone.utc) - observed >= interval


def hk_research_sync_status(db: Session, symbol: str) -> dict[str, Any]:
    symbol = hk_symbol(symbol)
    cached = db.scalar(select(StockF10Cache).where(StockF10Cache.market == "HK",
        StockF10Cache.symbol == symbol, StockF10Cache.section == "research_sections"))
    payload = cached.payload_json if cached else {}
    log = db.scalar(select(DataFetchLog).where(DataFetchLog.market == "HK",
        DataFetchLog.symbol == symbol, DataFetchLog.interface_code == SYNC_INTERFACE,
    ).order_by(DataFetchLog.id.desc()).limit(1))
    active = bool(log and log.status == "RUNNING" and
                  datetime.now(timezone.utc) - _utc(log.started_at) < timedelta(minutes=3))
    status = "RUNNING" if active else payload.get("status") or "MISSING"
    if not _enabled_interfaces(db):
        status = "DISABLED"
    elif log and log.status == "FAILED" and (not cached or _utc(log.started_at) > _utc(cached.fetched_at)):
        status = "FAILED"
    return {"market": "HK", "symbol": symbol, "status": status,
            "refresh_needed": hk_research_needs_refresh(db, symbol),
            "fetched_at": payload.get("fetched_at"), "message": payload.get("message"),
            "sources": [{k: v for k, v in row.items() if k != "raw_html"}
                        for row in payload.get("source_metadata", []) if isinstance(row, dict)],
            "counts": payload.get("persisted_counts") or {}, "log_id": log.id if log else None,
            "error": log.error_message if log else None}


def queue_hk_research_sync(db: Session, symbol: str, background_tasks: Any, force: bool = False) -> dict[str, Any]:
    symbol = hk_symbol(symbol)
    status = hk_research_sync_status(db, symbol)
    if status["status"] in {"RUNNING", "DISABLED"} or (not force and not status["refresh_needed"]):
        return {**status, "queued": False}
    with _SYNC_LOCK:
        if symbol in _SYNC_KEYS:
            return {**status, "status": "RUNNING", "queued": False}
        _SYNC_KEYS.add(symbol)
    try:
        source = db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE"))
        source = source or db.scalar(select(DataSource).where(DataSource.source_code.in_(list(_enabled_interfaces(db)))))
        if source is None:
            raise ValueError("未配置港股研究数据源")
        log = DataFetchLog(source_id=source.id, interface_code=SYNC_INTERFACE, market="HK", symbol=symbol,
                           request_json={"mode": "BACKGROUND_RESEARCH_ONLY"}, status="RUNNING")
        db.add(log)
        db.commit()
        background_tasks.add_task(_refresh_hk_research_background, symbol, log.id)
        return {**status, "status": "RUNNING", "queued": True, "log_id": log.id}
    except Exception:
        with _SYNC_LOCK:
            _SYNC_KEYS.discard(symbol)
        raise


def _refresh_hk_research_background(symbol: str, log_id: int) -> None:
    try:
        with SessionLocal() as db:
            try:
                result = sync_hk_research(db, symbol)
                log = db.get(DataFetchLog, log_id)
                log.status = "PARTIAL" if result["status"] == "PARTIAL" else "FAILED" if any(
                    row.get("status") == "FAILED" for row in result["source_metadata"]) else "MISSING"
                log.persisted_count = sum(result["persisted_counts"].values())
                log.total_count = log.persisted_count
                log.request_json = {**log.request_json, "counts": result["persisted_counts"],
                                    "sources": [{k: v for k, v in row.items() if k != "raw_html"} for row in result["source_metadata"]]}
                log.error_message = "; ".join(f"{row['source_code']}: {row['error']}" for row in result["source_metadata"] if row.get("error")) or None
                log.completed_at = datetime.now(timezone.utc)
                db.commit()
            except Exception as exc:
                db.rollback()
                log = db.get(DataFetchLog, log_id)
                if log is not None:
                    log.status, log.error_message = "FAILED", str(exc)[:2000]
                    log.completed_at = datetime.now(timezone.utc)
                    db.commit()
    finally:
        with _SYNC_LOCK:
            _SYNC_KEYS.discard(symbol)


def sync_hk_research(db: Session, symbol: str, cache_source: DataSource | None = None) -> dict[str, Any]:
    symbol = hk_symbol(symbol)
    interfaces = _enabled_interfaces(db)
    enabled = {code for code, codes in interfaces.items() if codes & {
        "HK_PROFIT_FORECAST_ON_DEMAND", "HK_INSTITUTION_RATING_ON_DEMAND", "HK_RESEARCH_REPORT_ON_DEMAND"}}
    # Release the read transaction before bounded remote I/O.
    db.commit()
    research = fetch_hk_research_sections(symbol, enabled_sources=enabled)
    if "HK_PROFIT_FORECAST_ON_DEMAND" not in interfaces.get("ETNET_HK", set()):
        research["earnings_forecast"], research["institution_forecast"] = [], []
    if "HK_INSTITUTION_RATING_ON_DEMAND" not in interfaces.get("ETNET_HK", set()):
        research.pop("provider_rating_statistics", None)
    research["persisted_counts"] = persist_research_sections(db, "HK", symbol, research)
    # Never discard a prior successful snapshot when either remote feed fails.
    existing = db.scalar(select(StockF10Cache).where(StockF10Cache.market == "HK",
        StockF10Cache.symbol == symbol, StockF10Cache.section == "research_sections"))
    previous = existing.payload_json if existing else {}
    if not research.get("provider_rating_statistics") and previous.get("provider_rating_statistics"):
        research["provider_rating_statistics"] = previous["provider_rating_statistics"]
    db.flush()
    normalized = load_research_sections(db, "HK", symbol)
    research.update({key: rows for key, rows in normalized.items() if rows})
    research["status"] = "PARTIAL" if any(research.get(key) for key in (
        "reports", "earnings_forecast", "institution_forecast")) else "MISSING"
    if not any(research.get(key) for key in ("reports", "earnings_forecast", "institution_forecast")):
        research["message"] = "公开港股研究源暂无该证券数据或采集失败；请查看来源状态后重试。"
    elif any(row.get("status") == "FAILED" for row in research["source_metadata"]):
        research["message"] = "部分来源采集失败，已保留本地历史数据；失败来源可继续重试。"
    cache_source = cache_source or db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE"))
    cache_source = cache_source or db.scalar(select(DataSource).where(DataSource.source_code.in_(["ETNET_HK", "AASTOCKS_HK"])))
    if cache_source is None:
        raise ValueError("未找到港股研究缓存关联数据源")
    StockOnDemandService(db).upsert_f10_cache(cache_source, "HK", symbol, "research_sections", research)
    db.commit()
    return research
