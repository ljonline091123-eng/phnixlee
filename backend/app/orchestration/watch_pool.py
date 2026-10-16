"""Nightly watch-pool data refresh workflow.

For every symbol on the watchlist (the L0 scan pool) refresh:

* daily K-line bars — incremental window derived from the latest stored bar
  (small overlap so late corrections replace stale rows via upsert),
* recent announcements — trailing window covering the L0 notice-cluster rule,
* recent news — the adapter's own recent window (upsert keeps re-runs idempotent).

One failing symbol never stops the batch: failures stay visible in the
``data_fetch_log`` rows written by ``StockOnDemandService`` and in the returned
summary.  Date arguments use the ``YYYYMMDD`` format expected by the market
adapters, matching ``services/stock_batch.py`` and ``services/selection.py``.
"""

from __future__ import annotations

import time
from dataclasses import dataclass
from datetime import date, datetime, timedelta
from typing import Any
from zoneinfo import ZoneInfo

from sqlalchemy import func, select

from app.db.session import SessionLocal
from app.models.market_data import StockKline, WatchlistItem
from app.services.catalog import select_data_source


SHANGHAI = ZoneInfo("Asia/Shanghai")

# first fill for a brand-new pool member: enough history for the 20-bar windows
FIRST_FILL_DAYS = 90
# re-fetch overlap so late corrections replace stale rows (upsert is idempotent)
KLINE_OVERLAP_DAYS = 3
# upper bound for a single incremental window
KLINE_MAX_WINDOW_DAYS = 370
# announcement lookback covers the 3-day cluster window plus late arrivals
NOTICE_LOOKBACK_DAYS = 10
# politeness delay between symbols to avoid upstream throttling
DEFAULT_DELAY_SECONDS = 0.5
MAX_ERROR_SAMPLES = 10


def _resolve_end_date(raw: str | None) -> date:
    if raw:
        try:
            return date.fromisoformat(str(raw)[:10])
        except ValueError:
            pass
    return datetime.now(SHANGHAI).date()


def compute_kline_window(latest_trade_date: str | None, end: date) -> tuple[str, str]:
    """Return ``(start, end)`` in ``YYYYMMDD`` covering the incremental window.

    ``latest_trade_date`` is the newest stored daily bar for the symbol; the
    window restarts a few days before it so late corrections upsert cleanly.
    """
    floor = end - timedelta(days=KLINE_MAX_WINDOW_DAYS)
    if latest_trade_date:
        try:
            start = date.fromisoformat(str(latest_trade_date)[:10]) - timedelta(days=KLINE_OVERLAP_DAYS)
        except ValueError:
            start = end - timedelta(days=FIRST_FILL_DAYS)
        if start < floor:
            start = floor
    else:
        start = end - timedelta(days=FIRST_FILL_DAYS)
    return start.strftime("%Y%m%d"), end.strftime("%Y%m%d")


@dataclass(slots=True)
class _Totals:
    kline: int = 0
    notice: int = 0
    news: int = 0


class WatchPoolRefreshWorkflow:
    """Refresh K-line / announcement / news data for every watchlist symbol."""

    workflow_code = "watch_pool_data_refresh"

    def execute(self, context_data: dict[str, Any] | None = None) -> dict[str, Any]:
        context = context_data or {}
        end = _resolve_end_date(context.get("business_date"))
        market_filter = context.get("market")
        try:
            delay_seconds = float(context.get("delay_seconds", DEFAULT_DELAY_SECONDS))
        except (TypeError, ValueError):
            delay_seconds = DEFAULT_DELAY_SECONDS

        started_at = time.monotonic()
        with SessionLocal() as db:
            stmt = select(WatchlistItem).order_by(WatchlistItem.market, WatchlistItem.symbol)
            if market_filter:
                stmt = stmt.where(WatchlistItem.market == market_filter)
            pool = list(db.scalars(stmt).all())
            if not pool:
                return {
                    "status": "SKIPPED",
                    "workflow": self.workflow_code,
                    "reason": "watchlist is empty; add pool symbols first",
                }

            totals = _Totals()
            symbols: list[dict[str, Any]] = []
            error_samples: list[str] = []
            error_count = 0

            for index, item in enumerate(pool):
                entry: dict[str, Any] = {"market": item.market, "symbol": item.symbol}
                try:
                    detail = self._refresh_symbol(db, item.market, item.symbol, end)
                except Exception as exc:  # one bad symbol must never stop the batch
                    db.rollback()
                    detail = {
                        "kline_persisted": 0,
                        "notice_persisted": 0,
                        "news_persisted": 0,
                        "skipped": [],
                        "errors": [f"{exc.__class__.__name__}: {str(exc)[:300]}"],
                    }
                entry.update(detail)
                totals.kline += int(detail.get("kline_persisted") or 0)
                totals.notice += int(detail.get("notice_persisted") or 0)
                totals.news += int(detail.get("news_persisted") or 0)
                if detail.get("errors"):
                    error_count += len(detail["errors"])
                    if len(error_samples) < MAX_ERROR_SAMPLES:
                        error_samples.append(f"{item.market}:{item.symbol}: {detail['errors'][0]}")
                symbols.append(entry)
                if delay_seconds > 0 and index < len(pool) - 1:
                    time.sleep(delay_seconds)

        summary = {
            "status": "COMPLETED" if error_count == 0 else "PARTIAL",
            "workflow": self.workflow_code,
            "business_date": end.isoformat(),
            "pool_size": len(pool),
            "kline_persisted": totals.kline,
            "notice_persisted": totals.notice,
            "news_persisted": totals.news,
            "symbols_with_errors": sum(1 for item in symbols if item.get("errors")),
            "error_count": error_count,
            "error_samples": error_samples,
            "duration_seconds": round(time.monotonic() - started_at, 1),
            "symbols": symbols,
        }
        chain_requested = bool(context.get("chain_detection", True))
        summary["detection_chained"] = self._enqueue_detection(end.isoformat()) if chain_requested else False
        return summary

    def _enqueue_detection(self, business_date: str) -> bool:
        """Chain the L0 detection job right after the refresh finishes.

        Shares the scheduler's idempotency key, so whichever enqueue runs
        first wins and the other becomes a no-op.  A failure here only means
        the scheduler fallback will submit the detection later.

        Manual callers (``scripts/run_watch_pool_refresh.py``) pass
        ``chain_detection=False`` so a daytime manual refresh never consumes
        the daily key that the automated post-close run relies on.
        """
        from app.jobs.dispatcher import DatabaseJobDispatcher

        try:
            with SessionLocal() as db:
                DatabaseJobDispatcher(db).enqueue(
                    task_type="l0_change_detection",
                    pipeline_type="l0_change_detection",
                    idempotency_key=f"l0_change_detection:{business_date}",
                    payload={"business_date": business_date},
                    trigger_type="PIPELINE",
                )
                db.commit()
            return True
        except Exception:
            return False

    def _refresh_symbol(self, db, market: str, symbol: str, end: date) -> dict[str, Any]:
        from app.services.stock_on_demand import StockOnDemandService

        service = StockOnDemandService(db)
        result: dict[str, Any] = {
            "kline_persisted": 0,
            "notice_persisted": 0,
            "news_persisted": 0,
            "skipped": [],
            "errors": [],
        }

        latest = db.scalar(
            select(func.max(StockKline.trade_date)).where(
                StockKline.market == market,
                StockKline.symbol == symbol,
                StockKline.period == "daily",
                StockKline.adjust == "",
            )
        )
        start_str, end_str = compute_kline_window(latest, end)

        kline_source = select_data_source(db, market, "KLINE")
        if kline_source is None:
            result["skipped"].append("KLINE source unavailable")
        else:
            try:
                log, _ = service.fetch_kline(
                    source=kline_source,
                    market=market,
                    symbol=symbol,
                    period="daily",
                    adjust="",
                    start_date=start_str,
                    end_date=end_str,
                    persist=True,
                )
                result["kline_persisted"] = int(log.persisted_count or 0)
            except Exception as exc:
                db.rollback()
                result["errors"].append(f"kline: {exc.__class__.__name__}: {str(exc)[:200]}")

        notice_source = select_data_source(db, market, "NOTICE")
        if notice_source is None:
            result["skipped"].append("NOTICE source unavailable")
        else:
            try:
                log, _ = service.fetch_notices(
                    source=notice_source,
                    market=market,
                    symbol=symbol,
                    start_date=(end - timedelta(days=NOTICE_LOOKBACK_DAYS)).strftime("%Y%m%d"),
                    end_date=end_str,
                    persist=True,
                )
                result["notice_persisted"] = int(log.persisted_count or 0)
            except Exception as exc:
                db.rollback()
                result["errors"].append(f"notices: {exc.__class__.__name__}: {str(exc)[:200]}")

        news_source = select_data_source(db, market, "NEWS")
        if news_source is None:
            result["skipped"].append("NEWS source unavailable")
        else:
            try:
                log, _ = service.fetch_news(
                    source=news_source,
                    market=market,
                    symbol=symbol,
                    persist=True,
                )
                result["news_persisted"] = int(log.persisted_count or 0)
            except Exception as exc:
                db.rollback()
                result["errors"].append(f"news: {exc.__class__.__name__}: {str(exc)[:200]}")

        return result
