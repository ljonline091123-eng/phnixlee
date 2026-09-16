"""Read model repository for the stock F10 page."""

from __future__ import annotations

from dataclasses import dataclass

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models.market_data import (
    StockFinancialReport,
    StockKline,
    StockNews,
    StockNotice,
    StockRealtimeQuote,
    StockSymbol,
)


@dataclass(slots=True)
class StockF10Snapshot:
    stock: StockSymbol | None
    klines: list[StockKline]
    financials: list[StockFinancialReport]
    report_notices: list[StockNotice]
    notices: list[StockNotice]
    notice_total: int
    quote: StockRealtimeQuote | None
    news: list[StockNews]
    news_total: int


class StockF10Repository:
    """Own all SQL needed to assemble the F10 read model."""

    def __init__(self, db: Session):
        self.db = db

    def load(
        self,
        *,
        market: str,
        symbol: str,
        kline_limit: int,
        financial_limit: int,
        notice_limit: int,
        notice_page: int,
        news_limit: int,
        news_page: int,
    ) -> StockF10Snapshot:
        stock = self.db.scalar(
            select(StockSymbol).where(
                StockSymbol.market == market,
                StockSymbol.symbol == symbol,
            )
        )
        klines = list(
            self.db.scalars(
                select(StockKline)
                .where(StockKline.market == market, StockKline.symbol == symbol)
                .order_by(StockKline.trade_date.desc())
                .limit(kline_limit)
            ).all()
        )
        financials = list(
            self.db.scalars(
                select(StockFinancialReport)
                .where(
                    StockFinancialReport.market == market,
                    StockFinancialReport.symbol == symbol,
                )
                .order_by(StockFinancialReport.report_period.desc())
                .limit(financial_limit)
            ).all()
        )
        report_notices = list(
            self.db.scalars(
                select(StockNotice)
                .where(StockNotice.market == market, StockNotice.symbol == symbol)
                .order_by(StockNotice.notice_date.desc(), StockNotice.id.desc())
                .limit(500)
            ).all()
        )
        notice_start = (notice_page - 1) * notice_limit
        notices = report_notices[notice_start : notice_start + notice_limit]
        notice_total = int(
            self.db.scalar(
                select(func.count())
                .select_from(StockNotice)
                .where(StockNotice.market == market, StockNotice.symbol == symbol)
            )
            or 0
        )
        quote = self.db.scalar(
            select(StockRealtimeQuote)
            .where(
                StockRealtimeQuote.market == market,
                StockRealtimeQuote.symbol == symbol,
            )
            .order_by(StockRealtimeQuote.fetched_at.desc())
        )
        news = list(
            self.db.scalars(
                select(StockNews)
                .where(StockNews.market == market, StockNews.symbol == symbol)
                .order_by(StockNews.news_time.desc(), StockNews.id.desc())
                .offset((news_page - 1) * news_limit)
                .limit(news_limit)
            ).all()
        )
        news_total = int(
            self.db.scalar(
                select(func.count())
                .select_from(StockNews)
                .where(StockNews.market == market, StockNews.symbol == symbol)
            )
            or 0
        )
        return StockF10Snapshot(
            stock=stock,
            klines=klines,
            financials=financials,
            report_notices=report_notices,
            notices=notices,
            notice_total=notice_total,
            quote=quote,
            news=news,
            news_total=news_total,
        )
