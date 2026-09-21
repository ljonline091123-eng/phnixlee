from __future__ import annotations

import json
from dataclasses import dataclass, field
from typing import Any, Iterator

from pydantic import BaseModel, Field
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, KnowledgeGraph, ResearchReportRecord
from app.models.market_data import DataSource, StockKline, StockRealtimeQuote
from app.prompts.fundamental import FUNDAMENTAL_SYSTEM_PROMPT
from app.prompts.orchestrator import ORCHESTRATOR_SYSTEM_PROMPT
from app.prompts.technical import TECHNICAL_SYSTEM_PROMPT
from app.services.model_hub import ModelHubService
from app.services.company_research import load_company_context, company_context_markdown
from app.services.prediction_ledger import record_report_rating
from app.services.stock_on_demand import StockOnDemandService
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
    score: int | None = None
    rating: str | None = None
    conclusion: str | None = None
    report_id: int | None = None
    data_source_codes: list[str] = Field(default_factory=list)
    knowledge_base_ids: list[int] = Field(default_factory=list)
    knowledge_documents: list[dict[str, Any]] = Field(default_factory=list)
    history_evaluation: dict[str, Any] = Field(default_factory=dict)


@dataclass
class ResearchContext:
    symbol: str
    market: str | None
    data_source_codes: list[str] = field(default_factory=list)
    knowledge_base_ids: list[int] = field(default_factory=list)
    model_instance_code: str | None = None
    knowledge_documents: list[dict[str, Any]] = field(default_factory=list)
    history_evaluation: dict[str, Any] = field(default_factory=dict)
    price_snapshot: dict[str, Any] = field(default_factory=dict)
    company_context: dict[str, Any] = field(default_factory=dict)
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


def _overall_score(context: ResearchContext) -> int:
    if context.fundamental is None or context.technical_agent is None:
        return 0
    return round((context.fundamental.score + context.technical_agent.score) / 2)


def _first_number(*values: Any) -> float | None:
    for value in values:
        if isinstance(value, (int, float)):
            return float(value)
    return None


def _latest_price_snapshot(db: Session, symbol: str, market: str | None) -> dict[str, Any]:
    quote_statement = select(StockRealtimeQuote).where(StockRealtimeQuote.symbol == symbol)
    kline_statement = select(StockKline).where(StockKline.symbol == symbol)
    if market:
        quote_statement = quote_statement.where(StockRealtimeQuote.market == market)
        kline_statement = kline_statement.where(StockKline.market == market)
    quote = db.scalar(quote_statement.order_by(StockRealtimeQuote.fetched_at.desc()).limit(1))
    kline = db.scalar(kline_statement.order_by(StockKline.trade_date.desc()).limit(1))
    latest_price = _first_number(
        quote.current_price if quote else None,
        kline.close_price if kline else None,
    )
    previous_close = _first_number(
        quote.previous_close_price if quote else None,
        kline.open_price if kline else None,
    )
    return {
        "latest_price": latest_price,
        "previous_close": previous_close,
        "quote_time": quote.quote_time if quote else None,
        "trade_date": kline.trade_date if kline else None,
        "volume": _first_number(quote.volume if quote else None, kline.volume if kline else None),
        "amount": _first_number(quote.amount if quote else None, kline.amount if kline else None),
        "change_pct": quote.change_pct if quote else None,
    }


def _knowledge_scope_text(context: ResearchContext) -> str:
    if not context.knowledge_documents:
        return "- 本次未选中知识库，或知识库中暂未检索到该股票证据。"
    lines = []
    for item in context.knowledge_documents[:8]:
        title = str(item.get("title") or "未命名证据")
        source_table = str(item.get("source_table") or "unknown")
        lines.append(f"- {title}（{source_table}）")
    return "\n".join(lines)


def _history_evaluation_text(context: ResearchContext) -> str:
    data = context.history_evaluation or {}
    if data.get("status") == "NO_HISTORY":
        return "- 暂无历史研报，本次报告将作为后续复盘基准。"
    if data.get("status") == "NO_PRICE":
        return f"- 已找到历史研报 #{data.get('previous_report_id')}，但缺少可比较的当前或历史价格，暂以评分和风险项变化做定性复盘。"
    return (
        f"- 上一份研报 #{data.get('previous_report_id')} 评级 {data.get('previous_rating') or '--'}、"
        f"评分 {data.get('previous_score') or '--'}；参考价 {_fmt(data.get('previous_price'))}，"
        f"当前价 {_fmt(data.get('current_price'))}，区间变化 {_fmt(data.get('price_change_pct'))}%。"
    )


def _build_history_evaluation(db: Session, context: ResearchContext) -> dict[str, Any]:
    statement = (
        select(ResearchReportRecord)
        .where(
            ResearchReportRecord.symbol == context.symbol,
            ResearchReportRecord.market == str(context.market or ""),
        )
        .order_by(ResearchReportRecord.created_at.desc())
        .limit(1)
    )
    previous = db.scalar(statement)
    if previous is None:
        return {
            "status": "NO_HISTORY",
            "summary": "暂无历史研报，本次报告作为该股票的首份 AI 研报基准。",
            "adjustments": ["后续生成新研报时，将以本次评分、评级、价格快照和风险项进行复盘。"],
        }
    previous_snapshot = dict(previous.agent_snapshot_json or {}).get("price_snapshot") or {}
    previous_price = _first_number(previous_snapshot.get("latest_price"), previous_snapshot.get("previous_close"))
    current_price = _first_number(context.price_snapshot.get("latest_price"), context.price_snapshot.get("previous_close"))
    score = _overall_score(context)
    score_delta = score - previous.score if previous.score is not None else None
    result: dict[str, Any] = {
        "status": "EVALUATED",
        "previous_report_id": previous.id,
        "previous_created_at": previous.created_at.isoformat() if previous.created_at else None,
        "previous_rating": previous.rating,
        "previous_score": previous.score,
        "current_score": score,
        "score_delta": score_delta,
        "previous_price": previous_price,
        "current_price": current_price,
        "previous_conclusion": previous.conclusion,
    }
    if previous_price is None or current_price is None:
        result.update(
            {
                "status": "NO_PRICE",
                "summary": "已找到历史研报，但缺少可比较的历史或当前价格，当前只进行评分、评级和风险项变化复盘。",
                "adjustments": [
                    "补齐历史 K 线或实时行情后再评估价格方向准确性。",
                    "重点比较本次基本面、技术面和新闻公告风险项相对历史研报的变化。",
                ],
            }
        )
        return result
    price_change_pct = round((current_price - previous_price) / previous_price * 100, 2) if previous_price else None
    result["price_change_pct"] = price_change_pct
    bullish_previous = (previous.score or 0) >= 60 or str(previous.rating or "").upper() in {"A", "B"}
    bearish_previous = (previous.score or 0) <= 44 or str(previous.rating or "").upper() == "D"
    if price_change_pct is None:
        accuracy = "价格基准为 0，无法计算方向。"
    elif bullish_previous and price_change_pct >= 0:
        accuracy = "历史偏积极判断与后续价格方向基本一致。"
    elif bullish_previous and price_change_pct < 0:
        accuracy = "历史偏积极判断未被价格验证，需要下调假设或重新检查风险触发条件。"
    elif bearish_previous and price_change_pct <= 0:
        accuracy = "历史偏谨慎判断与后续价格方向基本一致。"
    elif bearish_previous and price_change_pct > 0:
        accuracy = "历史偏谨慎判断偏保守，需要复核错过的基本面或资金催化。"
    else:
        accuracy = "历史结论偏中性，本次重点评估是否出现新的方向信号。"
    adjustments = []
    if score_delta is not None:
        if score_delta >= 8:
            adjustments.append("本次综合评分明显上调，应核验改善来自基本面、量价还是新闻公告催化。")
        elif score_delta <= -8:
            adjustments.append("本次综合评分明显下调，应把风险项和失效条件前置到预警列表。")
        else:
            adjustments.append("本次评分相对历史变化不大，应继续跟踪原有支撑阻力和风险触发条件。")
    result.update({"summary": accuracy, "adjustments": adjustments or ["继续跟踪财报、公告、成交量和价格相对关键位的变化。"]})
    return result


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
        f"> 数据范围：本地已存储的 F10、日线、资金流、新闻、公告、知识图谱和历史研报；综合评级不代表收益承诺。",
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
            "## 五、数据源与知识图谱证据",
            "",
            f"- 指定数据源：{', '.join(context.data_source_codes) if context.data_source_codes else '默认内部数据源'}。",
            f"- 指定知识库：{', '.join(str(item) for item in context.knowledge_base_ids) if context.knowledge_base_ids else '未指定'}。",
            _knowledge_scope_text(context),
            "",
            "## 六、历史研报复盘",
            "",
            _history_evaluation_text(context),
            *(f"- 调整建议：{item}" for item in context.history_evaluation.get("adjustments", [])),
            "",
            "## 七、短中长线观察与交易计划",
            "",
            f"- 短线（1周）：重点观察价格在 {_fmt(support)} 附近的承接，以及放量突破 {_fmt(resistance)} 是否成立；若量价不配合，以观望为主。",
            "- 中线（1个月）：跟踪财报改善、公告催化和新闻情绪是否持续，若基本面评分与资金面同时改善，可提高关注优先级。",
            "- 长线（3个月以上）：以业务布局、盈利质量、股东结构和行业景气度为主，只有连续证据链改善时才提高长期评级。",
            f"- 观察区间：重点观察价格在 {_fmt(support)} 附近的承接，以及向上突破 {_fmt(resistance)} 时的成交量确认。",
            "- 进入条件：基本面最新报告未恶化，且价格重新站稳关键均线并出现量价配合；不满足条件时保持观望。",
            "- 退出条件：跌破支撑并伴随放量、公告风险落地、或后续财报显示盈利质量恶化。具体仓位和止损需结合个人风险承受能力。",
            "",
            "## 八、风险提示与待验证清单",
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
            "data_source_codes": context.data_source_codes,
            "knowledge_base_ids": context.knowledge_base_ids,
            "knowledge_documents": context.knowledge_documents,
            "history_evaluation": context.history_evaluation,
            "company_relationships": context.company_context,
            "financial_agent": context.fundamental.model_dump(),
            "technical_agent": context.technical_agent.model_dump(),
            "news_rag": context.news.model_dump(),
        }
        response = ModelHubService(db).chat(
            task_type="stock_analysis",
            instance_code=context.model_instance_code,
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
        markdown += company_context_markdown(context.company_context)
        score = _overall_score(context)
        rating = _rating(score)
        conclusion = (
            f"综合评级 {rating}（{score}/100）：{context.fundamental.commentary} "
            f"{context.technical_agent.commentary}"
        )
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
            score=score,
            rating=rating,
            conclusion=conclusion,
            data_source_codes=context.data_source_codes,
            knowledge_base_ids=context.knowledge_base_ids,
            knowledge_documents=context.knowledge_documents,
            history_evaluation=context.history_evaluation,
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

    def _load_knowledge_documents(self, context: ResearchContext, limit: int = 12) -> list[dict[str, Any]]:
        if not context.knowledge_base_ids:
            kb = self.db.scalar(
                select(KnowledgeBase).where(
                    KnowledgeBase.kb_code == "STOCK_FULL_KG",
                    KnowledgeBase.enabled.is_(True),
                )
            )
            context.knowledge_base_ids = [kb.id] if kb else []
        if not context.knowledge_base_ids:
            return []
        rows = list(
            self.db.scalars(
                select(KnowledgeDocument)
                .join(KnowledgeGraph, KnowledgeGraph.id == KnowledgeDocument.graph_id)
                .join(KnowledgeBase, KnowledgeBase.id == KnowledgeDocument.knowledge_base_id)
                .where(
                    KnowledgeDocument.knowledge_base_id.in_(context.knowledge_base_ids),
                    KnowledgeDocument.symbol == context.symbol,
                    KnowledgeBase.enabled.is_(True),
                    KnowledgeGraph.enabled.is_(True),
                    KnowledgeGraph.governance_status.in_(["GOVERNED", "LOCKED"]),
                )
                .order_by(KnowledgeDocument.updated_at.desc())
                .limit(limit * 4)
            ).all()
        )
        documents: list[dict[str, Any]] = []
        seen: set[tuple[str, int | None, str | None]] = set()
        for row in rows:
            key = (row.source_table, row.source_record_id, row.symbol)
            if key in seen:
                continue
            seen.add(key)
            documents.append({
                "id": row.id,
                "knowledge_base_id": row.knowledge_base_id,
                "source_table": row.source_table,
                "source_record_id": row.source_record_id,
                "market": row.market,
                "symbol": row.symbol,
                "title": row.title,
                "excerpt": row.content[:500],
                "metadata_json": row.metadata_json,
            })
            if len(documents) >= limit:
                break
        return documents

    def _save_report(self, context: ResearchContext, report: ResearchReport) -> ResearchReportRecord:
        snapshot = {
            "fundamental": context.fundamental.model_dump(mode="json") if context.fundamental else None,
            "technical": context.technical_agent.model_dump(mode="json") if context.technical_agent else None,
            "news": context.news.model_dump(mode="json") if context.news else None,
            "price_snapshot": context.price_snapshot,
            "knowledge_documents": context.knowledge_documents,
            "company_relationships": context.company_context,
        }
        record = ResearchReportRecord(
            market=report.market,
            symbol=report.symbol,
            name=report.name,
            title=f"{report.symbol} {report.name} AI 深度研究报告",
            report_markdown=report.report_markdown,
            conclusion=report.conclusion,
            rating=report.rating,
            score=report.score,
            model_provider=report.model_provider,
            model_instance=report.model_instance,
            data_sources_json=report.data_source_codes,
            knowledge_base_ids_json=report.knowledge_base_ids,
            agent_snapshot_json=snapshot,
            history_evaluation_json=report.history_evaluation,
            warnings_json=report.warnings,
        )
        self.db.add(record)
        self.db.flush()
        record_report_rating(
            self.db, record,
            entry_price=_first_number(context.price_snapshot.get("latest_price"), context.price_snapshot.get("previous_close")),
        )
        self.db.commit()
        self.db.refresh(record)
        return record

    def stream(
        self,
        symbol: str,
        market: str | None = None,
        top_k: int = 5,
        refresh: bool = False,
        data_source_codes: list[str] | None = None,
        knowledge_base_ids: list[int] | None = None,
        model_instance_code: str | None = None,
    ) -> Iterator[dict[str, Any]]:
        context = ResearchContext(
            symbol=symbol,
            market=market,
            data_source_codes=list(data_source_codes or []),
            knowledge_base_ids=list(knowledge_base_ids or []),
            model_instance_code=model_instance_code,
        )
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
            context.price_snapshot = _latest_price_snapshot(self.db, context.symbol, context.market)
            context.knowledge_documents = self._load_knowledge_documents(context)
            context.company_context = load_company_context(self.db, context.market, context.symbol)
        except Exception as exc:
            yield {"event": "error", "data": {"message": str(exc)}}
            return

        yield {"event": "stage", "data": {"stage": "fundamental", "message": "FundamentalAgent 正在分析财务质量"}}
        context.fundamental = self.fundamental_agent.run(context.symbol, context.market, self.db)
        yield {"event": "agent", "data": context.fundamental.model_dump(mode="json")}

        yield {"event": "stage", "data": {"stage": "technical", "message": "TechnicalCapitalAgent 正在分析均线与资金流"}}
        context.technical_agent = self.technical_agent.run(context.symbol, context.market, self.db)
        yield {"event": "agent", "data": context.technical_agent.model_dump(mode="json")}

        yield {"event": "stage", "data": {"stage": "history", "message": "ResearchReportReview 正在复盘历史研报与当前证据"}}
        context.history_evaluation = _build_history_evaluation(self.db, context)

        yield {"event": "stage", "data": {"stage": "orchestrator", "message": "MasterOrchestratorAgent 正在汇总证据并生成研报"}}
        report = self.orchestrator.run(context, self.db)
        yield {"event": "stage", "data": {"stage": "persist", "message": "正在保存研报并写入历史复盘库"}}
        saved_report = self._save_report(context, report)
        report.report_id = saved_report.id
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
                "score": report.score,
                "rating": report.rating,
                "conclusion": report.conclusion,
                "report_id": report.report_id,
                "history_evaluation": report.history_evaluation,
            },
        }
