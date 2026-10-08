"""Read model repository for the stock F10 page."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from sqlalchemy import func, select, or_
from sqlalchemy.orm import Session

from app.models.market_data import (
    StockFinancialReport,
    StockKline,
    StockNews,
    StockNotice,
    StockRealtimeQuote,
    StockSymbol,
)
from app.services.notice_read_model import deduplicate_notice_rows
from app.services.news_identity import partition_news


@dataclass(slots=True)
class StockF10Snapshot:
    stock: StockSymbol | None
    klines: list[StockKline]
    financials: list[StockFinancialReport]
    report_notices: list[StockNotice]
    notices: list[StockNotice]
    notice_total: int
    notice_metadata: dict[int, dict[str, Any]]
    quote: StockRealtimeQuote | None
    news: list[StockNews]
    news_total: int
    news_candidate_total: int = 0


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
        notice_category: str | None = None,
        notice_classifier: Any | None = None,
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
                    # Historical quote snapshots remain in storage for
                    # audit, but must never appear as filed financial data.
                    or_(StockFinancialReport.data_json["source"].as_string().is_(None),
                        StockFinancialReport.data_json["source"].as_string() != "Eastmoney push2 quote indicators"),
                )
                .order_by(StockFinancialReport.report_period.desc())
                .limit(financial_limit)
            ).all()
        )
        report_notices = list(self.db.scalars(
            select(StockNotice)
            .where(StockNotice.market == market, StockNotice.symbol == symbol)
            .order_by(StockNotice.notice_date.desc(), StockNotice.id.desc())
        ).all())
        classifier = notice_classifier or (lambda _title, _notice_type: None)
        canonical_notices = deduplicate_notice_rows(report_notices)
        notice_metadata = {item.row.id: item.metadata() for item in canonical_notices}
        filtered_notices = [
            item.row for item in canonical_notices
            if not notice_category or notice_category == "全部"
            or classifier(item.row.title, item.row.notice_type) == notice_category
        ]
        notice_start = (notice_page - 1) * notice_limit
        notices = filtered_notices[notice_start : notice_start + notice_limit]
        notice_total = len(filtered_notices)
        quote = self.db.scalar(
            select(StockRealtimeQuote)
            .where(
                StockRealtimeQuote.market == market,
                StockRealtimeQuote.symbol == symbol,
            )
            .order_by(StockRealtimeQuote.fetched_at.desc())
        )
        all_news = list(
            self.db.scalars(
                select(StockNews)
                .where(StockNews.market == market, StockNews.symbol == symbol)
                .order_by(StockNews.news_time.desc(), StockNews.id.desc())
            ).all()
        )
        matched_news, candidate_news = partition_news(all_news, stock)
        news_total = len(matched_news)
        news = matched_news[(news_page - 1) * news_limit : news_page * news_limit]
        return StockF10Snapshot(
            stock=stock,
            klines=klines,
            financials=financials,
            report_notices=report_notices,
            notices=notices,
            notice_total=notice_total,
            notice_metadata=notice_metadata,
            quote=quote,
            news=news,
            news_total=news_total,
            news_candidate_total=len(candidate_news),
        )
