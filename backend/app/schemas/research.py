from __future__ import annotations

from datetime import datetime
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
    data_source_codes: list[str] = Field(default_factory=list)
    knowledge_base_ids: list[int] = Field(default_factory=list)


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
    score: int | None = None
    rating: str | None = None
    conclusion: str | None = None
    report_id: int | None = None
    history_evaluation: dict[str, Any] = Field(default_factory=dict)


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
        score=report.score,
        rating=report.rating,
        conclusion=report.conclusion,
        report_id=report.report_id,
        history_evaluation=report.history_evaluation,
    )


class ResearchReportSummary(BaseModel):
    id: int
    symbol: str
    market: str
    name: str
    title: str
    rating: str | None = None
    score: int | None = None
    conclusion: str | None = None
    model_provider: str | None = None
    model_instance: str | None = None
    created_at: datetime
    updated_at: datetime


class ResearchReportDetail(ResearchReportSummary):
    report_markdown: str
    data_sources_json: list[str] = Field(default_factory=list)
    knowledge_base_ids_json: list[int] = Field(default_factory=list)
    agent_snapshot_json: dict[str, Any] = Field(default_factory=dict)
    history_evaluation_json: dict[str, Any] = Field(default_factory=dict)
    warnings_json: list[str] = Field(default_factory=list)
