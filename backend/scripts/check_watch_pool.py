"""Check watch-pool data freshness (K-line / notice / news coverage).

Run from the ``backend`` directory::

    python -m scripts.check_watch_pool
    python -m scripts.check_watch_pool --market CN_A
    python -m scripts.check_watch_pool --json

A symbol is ``FRESH`` when its latest daily bar is within
``FRESH_LIMIT_DAYS`` calendar days (covers a normal weekend); otherwise it is
``STALE`` (old data) or ``NO_DATA`` (never fetched).
"""

from __future__ import annotations

import argparse
import json
from datetime import date, datetime, timedelta, timezone
from zoneinfo import ZoneInfo

import app.models  # noqa: F401 - register every ORM model in Base.metadata
from sqlalchemy import func, select

from app.db.session import SessionLocal
from app.models.market_data import DataFetchLog, StockKline, StockNews, StockNotice, StockSymbol, WatchlistItem


SHANGHAI = ZoneInfo("Asia/Shanghai")
FRESH_LIMIT_DAYS = 4
BAR_LOOKBACK_DAYS = 30
TEXT_LOOKBACK_DAYS = 7


def _count(db, model, market: str, symbol: str, date_column, from_date: str) -> int:
    return int(
        db.scalar(
            select(func.count())
            .select_from(model)
            .where(model.market == market, model.symbol == symbol, date_column >= from_date)
        )
        or 0
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="Check watch-pool data freshness.")
    parser.add_argument("--market", default=None, help="Only check this market (CN_A / HK / NEEQ / NEEQ_INNOVATION).")
    parser.add_argument("--json", action="store_true", help="Print raw JSON instead of the table.")
    args = parser.parse_args()

    today = datetime.now(SHANGHAI).date()
    bar_from = (today - timedelta(days=BAR_LOOKBACK_DAYS)).isoformat()
    text_from = (today - timedelta(days=TEXT_LOOKBACK_DAYS)).isoformat()
    since_24h = datetime.now(timezone.utc) - timedelta(hours=24)

    with SessionLocal() as db:
        stmt = select(WatchlistItem).order_by(WatchlistItem.market, WatchlistItem.symbol)
        if args.market:
            stmt = stmt.where(WatchlistItem.market == args.market)
        pool = list(db.scalars(stmt).all())

        rows = []
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

    if args.json:
        print(json.dumps(rows, ensure_ascii=False, indent=2))
    else:
        print(f"watch pool: {len(rows)} symbols | FRESH {fresh_count} / STALE {stale_count} / NO_DATA {missing_count}")
        print()
        header = f"{'代码':<24} {'名称':<12} {'最新K线':<11} {'滞后':<5} {'30日K':<6} {'7日公告':<7} {'7日新闻':<7} {'24h失败':<7} 状态"
        print(header)
        print("-" * 110)
        for row in rows:
            key = f"{row['market']}:{row['symbol']}"
            lag = f"{row['lag_days']}d" if row["lag_days"] is not None else "-"
            print(
                f"{key:<24} {(row['name'] or '-')[:10]:<12} {(row['latest_bar'] or '-'):<11} {lag:<5} "
                f"{row['bars_30d']:<6} {row['notices_7d']:<7} {row['news_7d']:<7} {row['failed_fetch_24h']:<7} {row['status']}"
            )
        print()
        print("提示：STALE/NO_DATA 说明该股票需要刷新；可运行 python -m scripts.run_watch_pool_refresh 补数据。")


if __name__ == "__main__":
    main()
