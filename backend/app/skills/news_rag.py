from __future__ import annotations

from typing import Any

from pydantic import BaseModel, Field
from sqlalchemy.orm import Session

from app.services.market_data import get_recent_news_and_notices
from app.skills.common import compact_value, managed_session, resolve_stock


class RecentNewsRagInput(BaseModel):
    symbol: str = Field(min_length=1, max_length=32)
    market: str | None = None
    top_k: int = Field(default=5, ge=1, le=50)


class NewsEvidence(BaseModel):
    kind: str
    title: str
    published_at: str | None = None
    source: str | None = None
    url: str | None = None
    sentiment: str = "NEUTRAL"
    relevance: float = 0.0
    excerpt: str | None = None


class RecentNewsRagOutput(BaseModel):
    symbol: str
    market: str
    name: str
    items: list[NewsEvidence] = Field(default_factory=list)
    positive_count: int = 0
    negative_count: int = 0
    neutral_count: int = 0
    source: str = "LOCAL_DB"
    warnings: list[str] = Field(default_factory=list)


POSITIVE_TERMS = ("增长", "盈利", "中标", "回购", "增持", "分红", "突破", "利好", "获批", "订单", "growth", "profit")
NEGATIVE_TERMS = ("亏损", "减持", "诉讼", "处罚", "风险", "下滑", "预警", "违规", "减值", "利空", "loss", "warning")


def _sentiment(text: str) -> str:
    positive = sum(1 for term in POSITIVE_TERMS if term.lower() in text.lower())
    negative = sum(1 for term in NEGATIVE_TERMS if term.lower() in text.lower())
    if positive > negative:
        return "POSITIVE"
    if negative > positive:
        return "NEGATIVE"
    return "NEUTRAL"


def get_recent_news_rag_skill(
    symbol: str,
    top_k: int = 5,
    market: str | None = None,
    *,
    db: Session | None = None,
) -> RecentNewsRagOutput:
    """Retrieve recent news and announcements with lightweight relevance scoring.

    The current data foundation does not require an external vector database:
    exact symbol filtering plus recency/title relevance is deterministic and
    keeps the feature usable offline.  The returned evidence shape can later be
    passed directly to an embedding retriever.
    """

    parsed_input = RecentNewsRagInput(symbol=symbol, top_k=top_k, market=market)
    with managed_session(db) as session:
        stock = resolve_stock(session, parsed_input.symbol, parsed_input.market)
        news_rows, notice_rows = get_recent_news_and_notices(
            session,
            stock.market,
            stock.symbol,
            limit=parsed_input.top_k * 3,
        )

        combined: list[NewsEvidence] = []
        for row in news_rows:
            title = str(row.title or "")
            content = str(row.content or "")
            combined.append(
                NewsEvidence(
                    kind="NEWS",
                    title=title,
                    published_at=str(row.news_time or "") or None,
                    source=row.source_name,
                    url=row.url,
                    sentiment=_sentiment(f"{title} {content}"),
                    relevance=0.9,
                    excerpt=content[:300] if content else None,
                )
            )
        for row in notice_rows:
            title = str(row.title or "")
            combined.append(
                NewsEvidence(
                    kind="NOTICE",
                    title=title,
                    published_at=str(row.notice_date or "") or None,
                    source="公告",
                    url=row.url,
                    sentiment=_sentiment(title),
                    relevance=0.85,
                    excerpt=None,
                )
            )

        combined.sort(key=lambda item: (item.published_at or "", item.relevance), reverse=True)
        items = combined[: parsed_input.top_k]
        counts = {
            "POSITIVE": sum(item.sentiment == "POSITIVE" for item in items),
            "NEGATIVE": sum(item.sentiment == "NEGATIVE" for item in items),
            "NEUTRAL": sum(item.sentiment == "NEUTRAL" for item in items),
        }
        warnings = [] if items else ["本地新闻和公告暂无记录，请先在股票详情页刷新相关数据。"]
        return RecentNewsRagOutput(
            symbol=stock.symbol,
            market=stock.market,
            name=stock.name,
            items=items,
            positive_count=counts["POSITIVE"],
            negative_count=counts["NEGATIVE"],
            neutral_count=counts["NEUTRAL"],
            warnings=warnings,
        )
