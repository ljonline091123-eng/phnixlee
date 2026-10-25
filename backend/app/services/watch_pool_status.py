"""Read model for watch-pool data freshness.

Shared by the HTTP API (``GET /watch-pool/status``) and the CLI
(``scripts/check_watch_pool.py``) so both surfaces always agree.
"""

from __future__ import annotations

from datetime import date, datetime, timedelta, timezone
from zoneinfo import ZoneInfo

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models.market_data import DataFetchLog, StockKline, StockNews, StockNotice, StockSymbol, WatchlistItem


SHANGHAI = ZoneInfo("Asia/Shanghai")
FRESH_LIMIT_DAYS = 4
BAR_LOOKBACK_DAYS = 30
TEXT_LOOKBACK_DAYS = 7


def _count(db: Session, model, market: str, symbol: str, date_column, from_date: str) -> int:
    return int(
        db.scalar(
            select(func.count())
            .select_from(model)
            .where(model.market == market, model.symbol == symbol, date_column >= from_date)
        )
        or 0
    )


def build_watch_pool_status(db: Session, *, market: str | None = None) -> dict:
    """Return per-symbol freshness rows plus a summary.

    ``status`` is ``FRESH`` when the latest daily bar is within
    ``FRESH_LIMIT_DAYS`` calendar days, ``STALE`` otherwise, ``NO_DATA`` when
    the symbol has never been fetched.
    """
    today = datetime.now(SHANGHAI).date()
    bar_from = (today - timedelta(days=BAR_LOOKBACK_DAYS)).isoformat()
    text_from = (today - timedelta(days=TEXT_LOOKBACK_DAYS)).isoformat()
    since_24h = datetime.now(timezone.utc) - timedelta(hours=24)

    stmt = select(WatchlistItem).order_by(WatchlistItem.market, WatchlistItem.symbol)
    if market:
        stmt = stmt.where(WatchlistItem.market == market)
    pool = list(db.scalars(stmt).all())

    rows: list[dict] = []
    fresh_count = stale_count = missing_count = 0
    for item in pool:
        name = (
            db.scalar(
                select(StockSymbol.name).where(
                    StockSymbol.market == item.market, StockSymbol.symbol == item.symbol
                )
            )
            or ""
        )
        latest_bar = db.scalar(
            select(func.max(StockKline.trade_date)).where(
                StockKline.market == item.market,
                StockKline.symbol == item.symbol,
                StockKline.period == "daily",
                StockKline.adjust == "",
            )
        )
        bars_30d = _count(db, StockKline, item.market, item.symbol, StockKline.trade_date, bar_from)
        latest_notice = db.scalar(
            select(func.max(StockNotice.notice_date)).where(
                StockNotice.market == item.market, StockNotice.symbol == item.symbol
            )
        )
        notices_7d = _count(db, StockNotice, item.market, item.symbol, StockNotice.notice_date, text_from)
        latest_news = db.scalar(
            select(func.max(StockNews.news_time)).where(
                StockNews.market == item.market, StockNews.symbol == item.symbol
            )
        )
        news_7d = _count(db, StockNews, item.market, item.symbol, StockNews.news_time, text_from)
        failed_24h = int(
            db.scalar(
                select(func.count())
                .select_from(DataFetchLog)
                .where(
                    DataFetchLog.market == item.market,
                    DataFetchLog.symbol == item.symbol,
                    DataFetchLog.status == "FAILED",
                    DataFetchLog.started_at >= since_24h,
                )
            )
            or 0
        )

        lag_days = None
        if latest_bar:
            try:
                lag_days = (today - date.fromisoformat(str(latest_bar)[:10])).days
            except ValueError:
                lag_days = None
        if latest_bar is None:
            status = "NO_DATA"
            missing_count += 1
        elif lag_days is not None and lag_days <= FRESH_LIMIT_DAYS:
            status = "FRESH"
            fresh_count += 1
        else:
            status = "STALE"
            stale_count += 1

        rows.append(
            {
                "market": item.market,
                "symbol": item.symbol,
                "name": name,
                "latest_bar": str(latest_bar)[:10] if latest_bar else None,
                "lag_days": lag_days,
                "bars_30d": bars_30d,
                "latest_notice": str(latest_notice)[:10] if latest_notice else None,
                "notices_7d": notices_7d,
                "latest_news": str(latest_news)[:10] if latest_news else None,
                "news_7d": news_7d,
                "failed_fetch_24h": failed_24h,
                "status": status,
            }
        )

    return {
        "as_of": today.isoformat(),
        "fresh_limit_days": FRESH_LIMIT_DAYS,
        "summary": {
            "pool_size": len(rows),
            "fresh": fresh_count,
            "stale": stale_count,
            "no_data": missing_count,
        },
        "rows": rows,
    }
