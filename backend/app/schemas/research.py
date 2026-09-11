from __future__ import annotations

from typing import Any

from pydantic import BaseModel, Field

from app.agents.workflow import (
    FundamentalAgentResult,
    ResearchReport,
    TechnicalCapitalAgentResult,
)
from app.skills.news_rag import RecentNewsRagOutput


class ResearchAnalyzeRequest(BaseModel):
    symbol: str = Field(min_length=1, max_length=64)
    market: str | None = Field(default=None, max_length=32)
    top_k: int = Field(default=5, ge=1, le=20)
    refresh: bool = False


class ResearchAgentSnapshot(BaseModel):
    fundamental: FundamentalAgentResult | None = None
    technical: TechnicalCapitalAgentResult | None = None
    news: RecentNewsRagOutput | None = None


class ResearchAnalyzeResponse(BaseModel):
    symbol: str
    market: str
    name: str
    report_markdown: str
    agents: ResearchAgentSnapshot
    model_provider: str | None = None
    model_instance: str | None = None
    warnings: list[str] = Field(default_factory=list)


def report_to_response(report: ResearchReport) -> ResearchAnalyzeResponse:
    return ResearchAnalyzeResponse(
        symbol=report.symbol,
        market=report.market,
        name=report.name,
        report_markdown=report.report_markdown,
        agents=ResearchAgentSnapshot(
            fundamental=report.fundamental,
            technical=report.technical,
            news=report.news,
        ),
        model_provider=report.model_provider,
        model_instance=report.model_instance,
        warnings=report.warnings,
    )
