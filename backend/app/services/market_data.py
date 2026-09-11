"""Small read facade for the market-data foundation.

Phase 2 skills use this module as their data boundary.  Keeping the SQLAlchemy
queries here means a future remote/cache implementation can be swapped in
without changing Agent prompts or the HTTP API.
"""

from __future__ import annotations

from typing import Any

from sqlalchemy import desc, select
from sqlalchemy.orm import Session

from app.models.market_data import (
    StockF10Cache,
    StockFinancialReport,
    StockKline,
    StockNews,
    StockNotice,
    StockRealtimeQuote,
)


def get_financial_reports(db: Session, market: str, symbol: str, limit: int = 12) -> list[StockFinancialReport]:
    return list(
        db.scalars(
            select(StockFinancialReport)
            .where(
                StockFinancialReport.market == market,
                StockFinancialReport.symbol == symbol,
            )
            .order_by(desc(StockFinancialReport.report_period), desc(StockFinancialReport.fetched_at))
            .limit(max(1, min(limit, 200)))
        ).all()
    )


def get_recent_klines(db: Session, market: str, symbol: str, limit: int = 60) -> list[StockKline]:
    return list(
        db.scalars(
            select(StockKline)
            .where(StockKline.market == market, StockKline.symbol == symbol)
            .order_by(desc(StockKline.trade_date))
            .limit(max(1, min(limit, 500)))
        ).all()
    )


def get_latest_quote(db: Session, market: str, symbol: str) -> StockRealtimeQuote | None:
    return db.scalar(
        select(StockRealtimeQuote)
        .where(StockRealtimeQuote.market == market, StockRealtimeQuote.symbol == symbol)
        .order_by(desc(StockRealtimeQuote.fetched_at))
    )


def get_fund_flow_cache(db: Session, market: str, symbol: str) -> dict[str, Any]:
    cache = db.scalar(
        select(StockF10Cache).where(
            StockF10Cache.market == market,
            StockF10Cache.symbol == symbol,
            StockF10Cache.section == "fund_flow",
        )
    )
    return dict(cache.payload_json or {}) if cache else {}


def get_recent_news_and_notices(
    db: Session,
    market: str,
    symbol: str,
    limit: int = 15,
) -> tuple[list[StockNews], list[StockNotice]]:
    bounded_limit = max(1, min(limit, 200))
    news = list(
        db.scalars(
            select(StockNews)
            .where(StockNews.market == market, StockNews.symbol == symbol)
            .order_by(desc(StockNews.news_time))
            .limit(bounded_limit)
        ).all()
    )
    notices = list(
        db.scalars(
            select(StockNotice)
            .where(StockNotice.market == market, StockNotice.symbol == symbol)
            .order_by(desc(StockNotice.notice_date))
            .limit(bounded_limit)
        ).all()
    )
    return news, notices

