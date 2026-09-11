from __future__ import annotations

import json
from dataclasses import dataclass, field
from typing import Any, Iterator

from pydantic import BaseModel, Field
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.prompts.fundamental import FUNDAMENTAL_SYSTEM_PROMPT
from app.prompts.orchestrator import ORCHESTRATOR_SYSTEM_PROMPT
from app.prompts.technical import TECHNICAL_SYSTEM_PROMPT
from app.services.model_hub import ModelHubService
from app.services.stock_on_demand import StockOnDemandService
from app.models.market_data import DataSource
from app.skills.capital_tech import CapitalAndTechOutput, get_capital_and_tech_skill
from app.skills.financial import FinancialAnalysisOutput, get_financial_analysis_skill
from app.skills.news_rag import RecentNewsRagOutput, get_recent_news_rag_skill


class FundamentalAgentResult(BaseModel):
    agent: str = "FundamentalAgent"
    score: int = Field(ge=0, le=100)
    rating: str
    commentary: str
    positive_factors: list[str] = Field(default_factory=list)
    negative_factors: list[str] = Field(default_factory=list)
    data_gaps: list[str] = Field(default_factory=list)
    evidence: FinancialAnalysisOutput


class TechnicalCapitalAgentResult(BaseModel):
    agent: str = "TechnicalCapitalAgent"
    score: int = Field(ge=0, le=100)
    trend: str
    capital_intent: str
    commentary: str
    key_levels: dict[str, float | None] = Field(default_factory=dict)
    positive_factors: list[str] = Field(default_factory=list)
    negative_factors: list[str] = Field(default_factory=list)
    data_gaps: list[str] = Field(default_factory=list)
    evidence: CapitalAndTechOutput


class ResearchReport(BaseModel):
    symbol: str
    market: str
    name: str
    report_markdown: str
    fundamental: FundamentalAgentResult
    technical: TechnicalCapitalAgentResult
    news: RecentNewsRagOutput
    model_provider: str | None = None
    model_instance: str | None = None
    warnings: list[str] = Field(default_factory=list)


@dataclass
class ResearchContext:
    symbol: str
    market: str | None
    financial: FinancialAnalysisOutput | None = None
    technical: CapitalAndTechOutput | None = None
    news: RecentNewsRagOutput | None = None
    fundamental: FundamentalAgentResult | None = None
    technical_agent: TechnicalCapitalAgentResult | None = None
    report: ResearchReport | None = None
    warnings: list[str] = field(default_factory=list)


def _rating(score: int) -> str:
    if score >= 75:
        return "A"
    if score >= 60:
        return "B"
    if score >= 45:
        return "C"
    return "D"


def _fmt(value: Any) -> str:
    if value is None or value == "":
        return "暂无"
    if isinstance(value, float):
        return f"{value:.2f}"
    return str(value)


def _model_json(response_text: str | None) -> dict[str, Any]:
    """Parse a model JSON response, including common Markdown code fences."""

    text = str(response_text or "").strip()
    if text.startswith("[MOCK:"):
        return {}
    if "```" in text:
        text = text.replace("```json", "").replace("```JSON", "").replace("```", "").strip()
    try:
        value = json.loads(text)
    except (TypeError, ValueError, json.JSONDecodeError):
        return {}
    return value if isinstance(value, dict) else {}


class FundamentalAgent:
    """Deterministic fundamental scorer with an LLM-friendly structured result."""

    def run(self, symbol: str, market: str | None, db: Session) -> FundamentalAgentResult:
        evidence = get_financial_analysis_skill(symbol, market, db=db)
        metrics = evidence.metrics
        yoy = evidence.yoy_growth
        score = 50
        positive: list[str] = []
        negative: list[str] = []

        revenue_yoy = yoy.get("revenue_yoy")
        profit_yoy = yoy.get("net_profit_yoy")
        roe = metrics.get("roe")
        net_margin = metrics.get("net_margin")
        debt_ratio = metrics.get("debt_asset_ratio")
        for value, label in ((revenue_yoy, "收入同比"), (profit_yoy, "净利润同比")):
            if isinstance(value, (int, float)):
                if value > 0:
                    score += 8
                    positive.append(f"{label}为 {_fmt(value)}，当前期同比为正。")
                elif value < 0:
                    score -= 8
                    negative.append(f"{label}为 {_fmt(value)}，当前期同比承压。")
        if isinstance(roe, (int, float)):
            if roe >= 10:
                score += 8
                positive.append(f"ROE 为 {_fmt(roe)}，盈利效率相对较好。")
            elif roe < 5:
                score -= 5
                negative.append(f"ROE 为 {_fmt(roe)}，盈利效率偏弱。")
        if isinstance(net_margin, (int, float)) and net_margin > 0:
            score += 4
            positive.append(f"净利率为 {_fmt(net_margin)}。")
        if isinstance(debt_ratio, (int, float)) and debt_ratio > 70:
            score -= 8
            negative.append(f"资产负债率为 {_fmt(debt_ratio)}，杠杆水平偏高。")
        score = max(0, min(100, score))
        gaps = list(evidence.warnings)
        if not positive and not negative:
            gaps.append("没有识别到足够的可比盈利指标。")

        result = FundamentalAgentResult(
            score=score,
            rating=_rating(score),
            commentary=(
                f"基本面初评分为 {score}/100。"
                + (" 当前数据支持盈利质量进一步核验。" if positive else " 当前数据不足，结论置信度较低。")
            ),
            positive_factors=positive,
            negative_factors=negative,
            data_gaps=gaps,
            evidence=evidence,
        )
        # When DeepSeek (or another configured route) is available, let it
        # refine the structured commentary. The deterministic score remains a
        # safe fallback for offline mode and malformed model output.
        try:
            model_response = ModelHubService(db).chat(
                task_type="stock_analysis",
                messages=[
                    {"role": "system", "content": FUNDAMENTAL_SYSTEM_PROMPT},
                    {
                        "role": "user",
                        "content": json.dumps(evidence.model_dump(), ensure_ascii=False, default=str),
                    },
                ],
                temperature=0.1,
                max_tokens=1200,
                metadata_json={"skill_code": "STOCK_TREND_ADVISOR", "agent": "fundamental"},
            )
            model_data = _model_json(model_response.response_text)
            if model_data:
                result.score = max(0, min(100, int(model_data.get("score", result.score))))
                result.rating = str(model_data.get("rating") or _rating(result.score))
                result.commentary = str(model_data.get("commentary") or result.commentary)
                result.positive_factors = [str(item) for item in model_data.get("positive_factors", result.positive_factors)]
                result.negative_factors = [str(item) for item in model_data.get("negative_factors", result.negative_factors)]
                result.data_gaps = [str(item) for item in model_data.get("data_gaps", result.data_gaps)]
        except Exception:
            # Agent-level model enhancement must never make local research
            # unavailable when a provider is down.
            pass
        return result


class TechnicalCapitalAgent:
    """Deterministic technical/capital scorer over locally stored daily data."""

    def run(self, symbol: str, market: str | None, db: Session) -> TechnicalCapitalAgentResult:
        evidence = get_capital_and_tech_skill(symbol, market, db=db)
        score = 50
        positive: list[str] = []
        negative: list[str] = []
        trend = evidence.trend
        if trend == "UP":
            score += 18
            positive.append("收盘价位于 20 日均线之上，短中期均线结构偏强。")
        elif trend == "DOWN":
            score -= 18
            negative.append("收盘价位于 20 日均线之下，短中期趋势偏弱。")
        elif trend == "RANGE":
            positive.append("价格处于区间震荡状态，等待方向选择。")
        net_inflow = evidence.capital_summary.get("net_inflow_20d")
        if isinstance(net_inflow, (int, float)):
            if net_inflow > 0:
                score += 12
                positive.append(f"近 20 日可识别资金净流入为 {_fmt(net_inflow)}。")
            elif net_inflow < 0:
                score -= 12
                negative.append(f"近 20 日可识别资金净流入为 {_fmt(net_inflow)}。")
        score = max(0, min(100, score))
        if net_inflow is None:
            intent = "UNKNOWN"
        elif net_inflow > 0:
            intent = "ACCUMULATION"
        elif net_inflow < 0:
            intent = "DISTRIBUTION"
        else:
            intent = "NEUTRAL"
        gaps = list(evidence.warnings)
        result = TechnicalCapitalAgentResult(
            score=score,
            trend=trend,
            capital_intent=intent,
            commentary=f"技术资金面初评分为 {score}/100，趋势状态为 {trend}。",
            key_levels=evidence.support_resistance,
            positive_factors=positive,
            negative_factors=negative,
            data_gaps=gaps,
            evidence=evidence,
        )
        try:
            model_response = ModelHubService(db).chat(
                task_type="stock_analysis",
                messages=[
                    {"role": "system", "content": TECHNICAL_SYSTEM_PROMPT},
                    {
                        "role": "user",
                        "content": json.dumps(evidence.model_dump(), ensure_ascii=False, default=str),
                    },
                ],
                temperature=0.1,
                max_tokens=1200,
                metadata_json={"skill_code": "STOCK_TREND_ADVISOR", "agent": "technical_capital"},
            )
            model_data = _model_json(model_response.response_text)
            if model_data:
                result.score = max(0, min(100, int(model_data.get("score", result.score))))
                result.trend = str(model_data.get("trend") or result.trend)
                result.capital_intent = str(model_data.get("capital_intent") or result.capital_intent)
                result.commentary = str(model_data.get("commentary") or result.commentary)
                result.key_levels = dict(model_data.get("key_levels") or result.key_levels)
                result.positive_factors = [str(item) for item in model_data.get("positive_factors", result.positive_factors)]
                result.negative_factors = [str(item) for item in model_data.get("negative_factors", result.negative_factors)]
                result.data_gaps = [str(item) for item in model_data.get("data_gaps", result.data_gaps)]
        except Exception:
            pass
        return result


def _heuristic_markdown(context: ResearchContext) -> str:
    fundamental = context.fundamental
    technical = context.technical_agent
    news = context.news
    assert fundamental is not None and technical is not None and news is not None
    overall = round((fundamental.score + technical.score) / 2)
    rating = _rating(overall)
    news_direction = "偏正面" if news.positive_count > news.negative_count else "偏负面" if news.negative_count > news.positive_count else "中性/待确认"
    support = technical.key_levels.get("support_20d")
    resistance = technical.key_levels.get("resistance_20d")
    lines = [
        f"# {context.symbol} {context.news.name} AI 深度研究报告",
        "",
        f"> 数据范围：本地已存储的 F10、日线、资金流、新闻和公告；综合评级不代表收益承诺。",
        "",
        "## 一、综合评级",
        "",
        f"- **评级：{rating}（{overall}/100）**",
        f"- 基本面评分：{fundamental.score}/100；技术资金面评分：{technical.score}/100。",
        f"- 当前结论：{fundamental.commentary} {technical.commentary}",
        "",
        "## 二、基本面分析",
        "",
        *(f"- {item}" for item in (fundamental.positive_factors or ["暂无明确正面证据。"])),
        *(f"- {item}" for item in (fundamental.negative_factors or ["暂无明确负面证据。"])),
        "",
        "## 三、技术面与资金面",
        "",
        f"- 趋势：**{technical.trend}**；资金倾向：**{technical.capital_intent}**。",
        f"- 近 20 日支撑参考：{_fmt(support)}；压力参考：{_fmt(resistance)}。",
        *(f"- {item}" for item in (technical.positive_factors or ["暂无明确正面技术证据。"])),
        *(f"- {item}" for item in (technical.negative_factors or ["暂无明确负面技术证据。"])),
        "",
        "## 四、新闻与公告影响",
        "",
        f"- 最近证据共 {len(news.items)} 条，情绪统计：利好 {news.positive_count}、利空 {news.negative_count}、中性 {news.neutral_count}，整体{news_direction}。",
    ]
    for item in news.items[:5]:
        lines.append(f"- [{item.sentiment}] {item.title}（{item.published_at or '日期未知'}）")
    lines.extend(
        [
            "",
            "## 五、条件化观察与交易计划",
            "",
            f"- 观察区间：重点观察价格在 {_fmt(support)} 附近的承接，以及向上突破 {_fmt(resistance)} 时的成交量确认。",
            "- 进入条件：基本面最新报告未恶化，且价格重新站稳关键均线并出现量价配合；不满足条件时保持观望。",
            "- 退出条件：跌破支撑并伴随放量、公告风险落地、或后续财报显示盈利质量恶化。具体仓位和止损需结合个人风险承受能力。",
            "",
            "## 六、风险提示与待验证清单",
            "",
            *(f"- {item}" for item in (fundamental.data_gaps + technical.data_gaps + news.warnings or ["需继续跟踪下一期财报、公告和成交量变化。"])),
            "- 本报告仅供研究参考，不构成投资建议；历史数据不代表未来表现。",
        ]
    )
    return "\n".join(lines)


class MasterOrchestratorAgent:
    def run(self, context: ResearchContext, db: Session) -> ResearchReport:
        assert context.financial is not None
        assert context.technical is not None
        assert context.news is not None
        assert context.fundamental is not None
        assert context.technical_agent is not None
        user_payload = {
            "symbol": context.symbol,
            "market": context.market,
            "financial_agent": context.fundamental.model_dump(),
            "technical_agent": context.technical_agent.model_dump(),
            "news_rag": context.news.model_dump(),
        }
        response = ModelHubService(db).chat(
            task_type="stock_analysis",
            messages=[
                {"role": "system", "content": ORCHESTRATOR_SYSTEM_PROMPT},
                {
                    "role": "user",
                    "content": "请根据以下 JSON 证据生成结构化 Markdown 研报：\n"
                    + json.dumps(user_payload, ensure_ascii=False, default=str),
                },
            ],
            temperature=0.2,
            max_tokens=6000,
            metadata_json={"skill_code": "STOCK_TREND_ADVISOR", "phase": "research"},
        )
        generated = (response.response_text or "").strip()
        # A configured DeepSeek response is preferred. The local mock is useful
        # for offline smoke tests but only echoes the prompt, so use our safe
        # deterministic report in that case.
        if response.status == "SUCCESS" and generated and not generated.startswith("[MOCK:") and len(generated) > 200:
            markdown = generated
        else:
            markdown = _heuristic_markdown(context)
        context.report = ResearchReport(
            symbol=context.fundamental.evidence.symbol,
            market=context.fundamental.evidence.market,
            name=context.fundamental.evidence.name,
            report_markdown=markdown,
            fundamental=context.fundamental,
            technical=context.technical_agent,
            news=context.news,
            model_provider=response.provider_code,
            model_instance=response.instance_code,
            warnings=[
                *context.fundamental.data_gaps,
                *context.technical_agent.data_gaps,
                *context.news.warnings,
            ],
        )
        return context.report


class ResearchWorkflow:
    """A small LangGraph-compatible workflow with explicit state transitions.

    The project intentionally keeps the first implementation dependency-light:
    each node has a stable ``run`` contract and can be replaced by a LangGraph
    node later without changing the API or frontend.
    """

    def __init__(self, db: Session):
        self.db = db
        self.fundamental_agent = FundamentalAgent()
        self.technical_agent = TechnicalCapitalAgent()
        self.orchestrator = MasterOrchestratorAgent()

    def stream(
        self,
        symbol: str,
        market: str | None = None,
        top_k: int = 5,
        refresh: bool = False,
    ) -> Iterator[dict[str, Any]]:
        context = ResearchContext(symbol=symbol, market=market)
        yield {"event": "stage", "data": {"stage": "resolve", "message": "正在解析本地股票主数据"}}
        try:
            context.financial = get_financial_analysis_skill(symbol, market, db=self.db)
            context.symbol = context.financial.symbol
            context.market = context.financial.market
            if refresh:
                source = self.db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE"))
                if source and source.enabled:
                    yield {"event": "stage", "data": {"stage": "refresh", "message": "正在同步最新行情、K线、财报、新闻和公告"}}
                    errors = StockOnDemandService(self.db).refresh_stock_data(
                        source,
                        context.market,
                        context.symbol,
                    )
                    if errors:
                        context.warnings.extend(errors)
                    # The refresh may have added or replaced the financial rows.
                    context.financial = get_financial_analysis_skill(context.symbol, context.market, db=self.db)
            context.news = get_recent_news_rag_skill(context.symbol, top_k, context.market, db=self.db)
            context.technical = get_capital_and_tech_skill(context.symbol, context.market, db=self.db)
        except Exception as exc:
            yield {"event": "error", "data": {"message": str(exc)}}
            return

        yield {"event": "stage", "data": {"stage": "fundamental", "message": "FundamentalAgent 正在分析财务质量"}}
        context.fundamental = self.fundamental_agent.run(context.symbol, context.market, self.db)
        yield {"event": "agent", "data": context.fundamental.model_dump(mode="json")}

        yield {"event": "stage", "data": {"stage": "technical", "message": "TechnicalCapitalAgent 正在分析均线与资金流"}}
        context.technical_agent = self.technical_agent.run(context.symbol, context.market, self.db)
        yield {"event": "agent", "data": context.technical_agent.model_dump(mode="json")}

        yield {"event": "stage", "data": {"stage": "orchestrator", "message": "MasterOrchestratorAgent 正在汇总证据并生成研报"}}
        report = self.orchestrator.run(context, self.db)
        # Chunking makes the endpoint useful even when the selected provider is
        # not a streaming provider. The browser receives a genuine typewriter
        # stream and can render Markdown incrementally.
        for index in range(0, len(report.report_markdown), 80):
            yield {"event": "report", "data": {"delta": report.report_markdown[index : index + 80]}}
        yield {
            "event": "done",
            "data": {
                "symbol": report.symbol,
                "market": report.market,
                "name": report.name,
                "model_provider": report.model_provider,
                "model_instance": report.model_instance,
                "warnings": report.warnings,
            },
        }
