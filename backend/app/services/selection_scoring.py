"""Versioned, evidence-aware scoring for the selection workbench.

The service deliberately separates deterministic calculations from LLM
interpretation.  Missing evidence stays missing; it is never converted into a
positive or negative score.  Historical candidates retain an immutable profile
snapshot in ``analysis_json`` while normalized rows support evaluation queries.
"""

from __future__ import annotations

from datetime import datetime, timedelta, timezone
from math import sqrt
from statistics import median
from typing import Any, Iterable

from sqlalchemy import delete, select
from sqlalchemy.orm import Session

from app.models.company_graph import SecurityClassification
from app.models.foundation import FoundationListing
from app.models.market_data import StockKline, StockSymbol
from app.models.selection import (
    SelectionCandidate,
    SelectionDimensionDefinition,
    SelectionDimensionResult,
    SelectionEvaluationMetric,
    SelectionStrategyProfile,
    SelectionStrategyProfileItem,
    SelectionRun,
)
from app.schemas.selection import SelectionProfileCreate, SelectionProfileUpdate


DIMENSIONS: tuple[dict[str, Any], ...] = (
    {"code": "FUNDAMENTAL_QUALITY", "name": "基本面与盈利质量", "method": "HYBRID", "order": 10,
     "description": "衡量收入、利润、现金流、资本回报和财务稳定性。",
     "interpretation": "高分表示盈利质量和持续性相对较好；缺少财务期次时不评分。"},
    {"code": "VALUATION_EXPECTATION", "name": "估值与盈利预期", "method": "RULE", "order": 20,
     "description": "衡量估值水平、股息率、盈利预测及预期变化。",
     "interpretation": "高分表示估值与现有盈利预期更匹配；不同行业不可只比较绝对倍数。"},
    {"code": "INDUSTRY_MOMENTUM", "name": "行业与板块景气", "method": "RULE", "order": 30,
     "description": "衡量标准主行业的相对强弱、上涨广度和个股相对行业表现。",
     "interpretation": "高分表示行业及个股相对行业表现较强；主题标签不自动替代标准行业。"},
    {"code": "PRICE_RISK", "name": "价格趋势与风险收益", "method": "RULE", "order": 40,
     "description": "衡量区间趋势、波动率、最大回撤和量价确认。",
     "interpretation": "高分表示趋势与回撤结构更有利，不代表未来必然上涨。"},
    {"code": "CAPITAL_BEHAVIOR", "name": "资金与交易行为", "method": "HYBRID", "order": 50,
     "description": "衡量成交、换手、融资和来源口径明确的资金行为信号。",
     "interpretation": "高分表示持续承接或交易活跃特征较强，不等于确认主力或游资身份。"},
    {"code": "EVENT_DISCLOSURE", "name": "事件催化与信披可信度", "method": "LLM", "order": 60,
     "description": "综合新闻、公告、事件联动、后续兑现和信息披露可靠性。",
     "interpretation": "高分表示有较可靠的正向催化及兑现证据；必须引用原文。"},
    {"code": "GOVERNANCE_STABILITY", "name": "治理与股东稳定性", "method": "HYBRID", "order": 70,
     "description": "衡量控制权、增减持、回购、解禁、质押及高管变化。",
     "interpretation": "高分表示治理和股东行为相对稳定；时点接近不自动证明行为动机。"},
    {"code": "SUPPLY_CHAIN_RESILIENCE", "name": "产业链与外部韧性", "method": "HYBRID", "order": 80,
     "description": "衡量上下游、经营往来、客户依赖、司法和外部事件风险。",
     "interpretation": "高分表示已知外部关系与风险相对可控；候选图谱关系不会直接计为事实。"},
)

SHORT_TERM_WEIGHTS = {
    "FUNDAMENTAL_QUALITY": 12.0,
    "VALUATION_EXPECTATION": 6.0,
    "INDUSTRY_MOMENTUM": 14.0,
    "PRICE_RISK": 20.0,
    "CAPITAL_BEHAVIOR": 17.0,
    "EVENT_DISCLOSURE": 17.0,
    "GOVERNANCE_STABILITY": 6.0,
    "SUPPLY_CHAIN_RESILIENCE": 8.0,
}
GENERAL_WEIGHTS = {
    "FUNDAMENTAL_QUALITY": 20.0,
    "VALUATION_EXPECTATION": 10.0,
    "INDUSTRY_MOMENTUM": 12.0,
    "PRICE_RISK": 14.0,
    "CAPITAL_BEHAVIOR": 12.0,
    "EVENT_DISCLOSURE": 13.0,
    "GOVERNANCE_STABILITY": 8.0,
    "SUPPLY_CHAIN_RESILIENCE": 11.0,
}

SKILLS = {
    "FUNDAMENTAL_QUALITY": "FINANCIAL_QUALITY_SCORER",
    "VALUATION_EXPECTATION": "VALUATION_EXPECTATION_SCORER",
    "INDUSTRY_MOMENTUM": "INDUSTRY_RELATIVE_STRENGTH_ANALYST",
    "PRICE_RISK": "PRICE_RISK_ANALYST",
    "CAPITAL_BEHAVIOR": "CAPITAL_BEHAVIOR_ANALYST",
    "EVENT_DISCLOSURE": "STOCK_EVENT_LINKAGE_ANALYST",
    "GOVERNANCE_STABILITY": "GOVERNANCE_SHAREHOLDER_ANALYST",
    "SUPPLY_CHAIN_RESILIENCE": "RELATION_RISK_ANALYST",
}


def _now() -> datetime:
    return datetime.now(timezone.utc)


def _clamp(value: float, low: float = 0, high: float = 100) -> float:
    return round(max(low, min(high, value)), 2)


def seed_selection_scoring(db: Session) -> None:
    existing = {row.dimension_code: row for row in db.scalars(select(SelectionDimensionDefinition)).all()}
    for spec in DIMENSIONS:
        if spec["code"] not in existing:
            db.add(SelectionDimensionDefinition(
                dimension_code=spec["code"], dimension_name=spec["name"],
                description=spec["description"], score_interpretation=spec["interpretation"],
                default_method=spec["method"], sort_order=spec["order"], enabled=True,
            ))
    db.flush()
    if db.scalar(select(SelectionStrategyProfile.id).where(
        SelectionStrategyProfile.profile_code == "SHORT_TERM_COMPOSITE",
        SelectionStrategyProfile.version == "1.0.0",
    )) is None:
        profile = SelectionStrategyProfile(
            profile_code="SHORT_TERM_COMPOSITE", profile_name="短周期综合",
            version="1.0.0", horizon_code="T3_T5_T7", status="ENABLED", enabled=True,
            published_at=_now(), market_scope_json=["CN_A", "HK", "NEEQ", "NEEQ_INNOVATION"],
            description="用于T+3/T+5/T+7跟踪的首版可解释权重；雷达图展示原始分，权重仅影响综合分。",
        )
        db.add(profile)
        db.flush()
        for code, weight in SHORT_TERM_WEIGHTS.items():
            db.add(SelectionStrategyProfileItem(
                profile_id=profile.id, dimension_code=code, weight=weight,
                minimum_coverage=0.4, minimum_confidence=0.35,
                skill_code=SKILLS[code], skill_version="1.0.0",
            ))
    if db.scalar(select(SelectionStrategyProfile.id).where(
        SelectionStrategyProfile.profile_code == "GENERAL_RESEARCH",
        SelectionStrategyProfile.version == "1.0.0",
    )) is None:
        profile = SelectionStrategyProfile(
            profile_code="GENERAL_RESEARCH", profile_name="综合研究",
            version="1.0.0", horizon_code="RESEARCH", status="DRAFT", enabled=False,
            market_scope_json=["CN_A", "HK", "NEEQ", "NEEQ_INNOVATION"],
            description="中期综合研究草稿；完成样本外验证前不用于生产排序。",
        )
        db.add(profile)
        db.flush()
        for code, weight in GENERAL_WEIGHTS.items():
            db.add(SelectionStrategyProfileItem(
                profile_id=profile.id, dimension_code=code, weight=weight,
                minimum_coverage=0.4, minimum_confidence=0.35,
                skill_code=SKILLS[code], skill_version="1.0.0",
            ))


def _profile_items(db: Session, profile_id: int) -> list[SelectionStrategyProfileItem]:
    return list(db.scalars(select(SelectionStrategyProfileItem).where(
        SelectionStrategyProfileItem.profile_id == profile_id,
    ).order_by(SelectionStrategyProfileItem.id)).all())


def serialize_profile(db: Session, profile: SelectionStrategyProfile) -> dict[str, Any]:
    definitions = {row.dimension_code: row for row in db.scalars(select(SelectionDimensionDefinition)).all()}
    items = []
    for row in _profile_items(db, profile.id):
        definition = definitions.get(row.dimension_code)
        items.append({
            "dimension_code": row.dimension_code,
            "dimension_name": definition.dimension_name if definition else row.dimension_code,
            "description": definition.description if definition else "",
            "score_interpretation": definition.score_interpretation if definition else "",
            "method": definition.default_method if definition else "HYBRID",
            "sort_order": definition.sort_order if definition else 999,
            "weight": row.weight,
            "minimum_coverage": row.minimum_coverage,
            "minimum_confidence": row.minimum_confidence,
            "skill_code": row.skill_code,
            "skill_version": row.skill_version,
        })
    items.sort(key=lambda item: item["sort_order"])
    return {
        "id": profile.id, "profile_code": profile.profile_code,
        "profile_name": profile.profile_name, "version": profile.version,
        "description": profile.description, "horizon_code": profile.horizon_code,
        "market_scope": profile.market_scope_json or [], "status": profile.status,
        "enabled": profile.enabled, "published_at": profile.published_at.isoformat() if profile.published_at else None,
        "parent_profile_id": profile.parent_profile_id,
        "weight_total": round(sum(item["weight"] for item in items), 4),
        "items": items,
        "created_at": profile.created_at.isoformat() if profile.created_at else None,
        "updated_at": profile.updated_at.isoformat() if profile.updated_at else None,
    }


def list_profiles(db: Session) -> list[dict[str, Any]]:
    rows = list(db.scalars(select(SelectionStrategyProfile).order_by(
        SelectionStrategyProfile.created_at.desc(), SelectionStrategyProfile.id.desc(),
    )).all())
    return [serialize_profile(db, row) for row in rows]


def resolve_profile(db: Session, profile_id: int | None = None, market: str | None = None) -> SelectionStrategyProfile:
    if profile_id is not None:
        profile = db.get(SelectionStrategyProfile, profile_id)
        if profile is None:
            raise LookupError("评价权重方案不存在")
        if not profile.enabled or profile.status != "ENABLED":
            raise ValueError("所选评价权重方案尚未发布或已停用")
    else:
        profile = db.scalar(select(SelectionStrategyProfile).where(
            SelectionStrategyProfile.enabled.is_(True),
            SelectionStrategyProfile.status == "ENABLED",
        ).order_by(SelectionStrategyProfile.published_at.desc(), SelectionStrategyProfile.id.desc()))
        if profile is None:
            raise LookupError("没有已发布的评价权重方案")
    if market and profile.market_scope_json and market.upper() not in profile.market_scope_json:
        raise ValueError(f"评价权重方案不适用于{market.upper()}市场")
    return profile


def create_profile(db: Session, payload: SelectionProfileCreate) -> SelectionStrategyProfile:
    if db.scalar(select(SelectionStrategyProfile.id).where(
        SelectionStrategyProfile.profile_code == payload.profile_code,
        SelectionStrategyProfile.version == payload.version,
    )) is not None:
        raise ValueError("评价方案编码和版本已存在")
    if payload.parent_profile_id and db.get(SelectionStrategyProfile, payload.parent_profile_id) is None:
        raise LookupError("父版本不存在")
    profile = SelectionStrategyProfile(
        profile_code=payload.profile_code, profile_name=payload.profile_name,
        version=payload.version, description=payload.description,
        horizon_code=payload.horizon_code, market_scope_json=payload.market_scope,
        status="DRAFT", enabled=False, parent_profile_id=payload.parent_profile_id,
    )
    db.add(profile)
    db.flush()
    for item in payload.items:
        db.add(SelectionStrategyProfileItem(profile_id=profile.id, **item.model_dump()))
    db.commit()
    db.refresh(profile)
    return profile


def update_profile(db: Session, profile_id: int, payload: SelectionProfileUpdate) -> SelectionStrategyProfile:
    profile = db.get(SelectionStrategyProfile, profile_id)
    if profile is None:
        raise LookupError("评价方案不存在")
    if profile.status != "DRAFT":
        raise ValueError("仅草稿版本可编辑；请复制已发布版本后再调整")
    changes = payload.model_dump(exclude_unset=True, exclude={"items", "market_scope"})
    for key, value in changes.items():
        setattr(profile, key, value)
    if payload.market_scope is not None:
        profile.market_scope_json = payload.market_scope
    if payload.items is not None:
        db.execute(delete(SelectionStrategyProfileItem).where(SelectionStrategyProfileItem.profile_id == profile.id))
        for item in payload.items:
            db.add(SelectionStrategyProfileItem(profile_id=profile.id, **item.model_dump()))
    db.commit()
    db.refresh(profile)
    return profile


def publish_profile(db: Session, profile_id: int) -> SelectionStrategyProfile:
    profile = db.get(SelectionStrategyProfile, profile_id)
    if profile is None:
        raise LookupError("评价方案不存在")
    items = _profile_items(db, profile.id)
    if len(items) != 8 or abs(sum(row.weight for row in items) - 100) > 0.001:
        raise ValueError("发布前必须配置完整八维，且权重合计为100%")
    for row in db.scalars(select(SelectionStrategyProfile).where(
        SelectionStrategyProfile.profile_code == profile.profile_code,
        SelectionStrategyProfile.enabled.is_(True),
    )).all():
        row.enabled = False
        if row.status == "ENABLED":
            row.status = "RETIRED"
    profile.status = "ENABLED"
    profile.enabled = True
    profile.published_at = _now()
    db.commit()
    db.refresh(profile)
    return profile


def retire_profile(db: Session, profile_id: int) -> SelectionStrategyProfile:
    profile = db.get(SelectionStrategyProfile, profile_id)
    if profile is None:
        raise LookupError("评价方案不存在")
    if profile.enabled and db.scalar(select(SelectionStrategyProfile.id).where(
        SelectionStrategyProfile.id != profile.id,
        SelectionStrategyProfile.enabled.is_(True),
        SelectionStrategyProfile.status == "ENABLED",
    )) is None:
        raise ValueError("不能停用当前唯一已发布方案；请先发布另一个版本")
    profile.enabled = False
    profile.status = "RETIRED"
    db.commit()
    db.refresh(profile)
    return profile


def clone_profile(db: Session, profile_id: int, *, version: str) -> SelectionStrategyProfile:
    source = db.get(SelectionStrategyProfile, profile_id)
    if source is None:
        raise LookupError("评价方案不存在")
    payload = SelectionProfileCreate(
        profile_code=source.profile_code, profile_name=source.profile_name,
        version=version, description=source.description, horizon_code=source.horizon_code,
        market_scope=source.market_scope_json or [], parent_profile_id=source.id,
        items=[{
            "dimension_code": row.dimension_code, "weight": row.weight,
            "minimum_coverage": row.minimum_coverage,
            "minimum_confidence": row.minimum_confidence,
            "skill_code": row.skill_code, "skill_version": row.skill_version,
        } for row in _profile_items(db, source.id)],
    )
    return create_profile(db, payload)


def _records(value: Any) -> list[dict[str, Any]]:
    return [row for row in value if isinstance(row, dict)] if isinstance(value, list) else []


def _numbers(value: Any, keys: Iterable[str]) -> list[float]:
    wanted = {str(key).lower().replace("_", "") for key in keys}
    result: list[float] = []
    def walk(node: Any) -> None:
        if isinstance(node, dict):
            for key, child in node.items():
                normalized = str(key).lower().replace("_", "")
                if normalized in wanted and isinstance(child, (int, float)) and not isinstance(child, bool):
                    result.append(float(child))
                elif isinstance(child, (dict, list)):
                    walk(child)
        elif isinstance(node, list):
            for child in node:
                walk(child)
    walk(value)
    return result


def _legacy_dimensions(analysis: dict[str, Any]) -> dict[str, dict[str, Any]]:
    model = analysis.get("model") or analysis.get("local_multi_source") or {}
    return {str(row.get("dimension")): row for row in _records(model.get("dimensions")) if row.get("dimension")}


def _direction_score(*rows: dict[str, Any] | None) -> float | None:
    values = []
    mapping = {"POSITIVE": 68.0, "NEGATIVE": 32.0, "MIXED": 50.0, "NEUTRAL": 50.0}
    for row in rows:
        if row and str(row.get("status")) != "INSUFFICIENT" and row.get("direction") in mapping:
            values.append(mapping[str(row["direction"])])
    return sum(values) / len(values) if values else None


def _summary(*rows: dict[str, Any] | None, fallback: str) -> str:
    values = [str(row.get("summary")) for row in rows if row and row.get("summary")]
    return " ".join(values)[:800] if values else fallback


def _bars(evidence: dict[str, Any]) -> list[dict[str, Any]]:
    observations = evidence.get("structured_observations") or {}
    market = _records(observations.get("market"))
    if not market:
        return []
    properties = market[0].get("properties") or {}
    return _records(properties.get("recent_bars"))


def _bar_metrics(bars: list[dict[str, Any]]) -> dict[str, float | None]:
    ordered = sorted(bars, key=lambda row: str(row.get("trade_date") or ""))
    closes = [float(row["close_price"]) for row in ordered if isinstance(row.get("close_price"), (int, float)) and row["close_price"] > 0]
    volumes = [float(row["volume"]) for row in ordered if isinstance(row.get("volume"), (int, float)) and row["volume"] >= 0]
    if not closes:
        return {"return_5d": None, "return_20d": None, "volatility": None, "max_drawdown": None, "volume_ratio": None}
    returns = [(closes[index] / closes[index - 1] - 1) * 100 for index in range(1, len(closes)) if closes[index - 1] > 0]
    volatility = sqrt(sum((value - sum(returns) / len(returns)) ** 2 for value in returns) / len(returns)) if returns else None
    peak = closes[0]
    drawdown = 0.0
    for close in closes:
        peak = max(peak, close)
        drawdown = min(drawdown, (close / peak - 1) * 100)
    recent_volume = sum(volumes[-5:]) / min(5, len(volumes)) if volumes else None
    older = volumes[-20:-5]
    older_volume = sum(older) / len(older) if older else None
    return {
        "return_5d": (closes[-1] / closes[-6] - 1) * 100 if len(closes) >= 6 else None,
        "return_20d": (closes[-1] / closes[0] - 1) * 100 if len(closes) >= 2 else None,
        "volatility": volatility,
        "max_drawdown": drawdown,
        "volume_ratio": recent_volume / older_volume if recent_volume is not None and older_volume and older_volume > 0 else None,
        "bar_count": float(len(closes)),
    }


def _industry_context(db: Session, market: str, symbol: str, as_of: datetime) -> dict[str, Any]:
    stock = db.scalar(select(StockSymbol).where(StockSymbol.market == market, StockSymbol.symbol == symbol))
    if stock is None:
        return {"status": "INSUFFICIENT", "warning": "股票主数据不存在"}
    listing = db.scalar(select(FoundationListing).where(FoundationListing.stock_symbol_id == stock.id))
    classification = None
    if listing is not None:
        classification = db.scalar(select(SecurityClassification).where(
            SecurityClassification.security_id == listing.security_id,
            SecurityClassification.dimension == "INDUSTRY",
            SecurityClassification.status.in_(("ACCEPTED", "VERIFIED", "SOURCE")),
        ).order_by(SecurityClassification.updated_at.desc()))
    def source_industry(row: StockSymbol) -> str | None:
        for payload in (row.ext_json or {}, row.raw_payload or {}):
            for key in ("industry", "industry_name", "industryName", "所属行业", "行业"):
                value = payload.get(key)
                if value not in (None, ""):
                    return str(value).strip()
        return None
    industry = classification.label if classification else source_industry(stock)
    if not industry:
        return {"status": "INSUFFICIENT", "warning": "缺少标准行业归属"}
    peers: list[tuple[str, str]] = []
    if classification is not None:
        peer_rows = list(db.execute(select(FoundationListing.market, FoundationListing.symbol).join(
            SecurityClassification, SecurityClassification.security_id == FoundationListing.security_id,
        ).where(
            FoundationListing.market == market,
            SecurityClassification.dimension == "INDUSTRY",
            SecurityClassification.label == classification.label,
            SecurityClassification.status.in_(("ACCEPTED", "VERIFIED", "SOURCE")),
        ).limit(40)).all())
        peers = [(row.market, row.symbol) for row in peer_rows]
    if not peers:
        market_stocks = list(db.scalars(select(StockSymbol).where(StockSymbol.market == market).limit(2000)).all())
        peers = [(row.market, row.symbol) for row in market_stocks if source_industry(row) == industry][:40]
    if (market, symbol) not in peers:
        peers.append((market, symbol))
    peer_symbols = [peer_symbol for peer_market, peer_symbol in peers[:40] if peer_market == market]
    period_start = (as_of.date() - timedelta(days=75)).isoformat()
    market_rows = list(db.scalars(select(StockKline).where(
        StockKline.market == market, StockKline.symbol.in_(peer_symbols),
        StockKline.period == "daily", StockKline.trade_date >= period_start,
        StockKline.trade_date <= as_of.date().isoformat(),
    ).order_by(StockKline.symbol, StockKline.trade_date.desc())).all()) if peer_symbols else []
    grouped: dict[str, list[StockKline]] = {}
    for row in market_rows:
        if len(grouped.setdefault(row.symbol, [])) < 20:
            grouped[row.symbol].append(row)
    returns: dict[str, float] = {}
    for peer_symbol, rows in grouped.items():
        closes = [float(row.close_price) for row in reversed(rows) if row.close_price and row.close_price > 0]
        if len(closes) >= 5:
            returns[peer_symbol] = (closes[-1] / closes[0] - 1) * 100
    own = returns.get(symbol)
    peer_values = [value for key, value in returns.items() if key != symbol]
    peer_median = median(peer_values) if peer_values else None
    breadth = sum(value > 0 for value in peer_values) / len(peer_values) if peer_values else None
    excess = own - peer_median if own is not None and peer_median is not None else None
    return {
        "status": "SUPPORTED" if own is not None and peer_values else "INSUFFICIENT",
        "industry": industry, "taxonomy": (classification.properties_json or {}).get("taxonomy") if classification else "STOCK_MASTER",
        "member_count": len(peers), "priced_peer_count": len(peer_values),
        "stock_return_20d": own, "peer_median_return_20d": peer_median,
        "excess_return_20d": excess, "positive_breadth": breadth,
        "classification_id": classification.id if classification else None,
    }


def _score_dimensions(db: Session, candidate: SelectionCandidate, profile: SelectionStrategyProfile) -> list[dict[str, Any]]:
    evidence = candidate.evidence_json or {}
    analysis = candidate.analysis_json or {}
    legacy = _legacy_dimensions(analysis)
    observations = evidence.get("structured_observations") or {}
    financial = _records(observations.get("financial"))
    shareholder = _records(observations.get("shareholder"))
    fund_flow = _records(observations.get("fund_flow"))
    bars = _bars(evidence)
    bar_metrics = _bar_metrics(bars)
    model = analysis.get("model") or analysis.get("local_multi_source") or {}
    model_confidence = model.get("confidence_breakdown") or {}
    event_confidence = float(model_confidence.get("final_confidence") or 0)
    result: dict[str, dict[str, Any]] = {}

    financial_payload = [row.get("properties") or {} for row in financial]
    revenue_growth = (_numbers(financial_payload, ("TOTALOPERATEREVETZ", "revenue_growth", "营业收入同比")) or [None])[0]
    profit_growth = (_numbers(financial_payload, ("PARENTNETPROFITTZ", "net_profit_growth", "归母净利润同比")) or [None])[0]
    roe = (_numbers(financial_payload, ("ROEJQ", "roe", "净资产收益率")) or [None])[0]
    financial_adjustments = []
    if revenue_growth is not None: financial_adjustments.append(max(-18, min(18, revenue_growth * 0.35)))
    if profit_growth is not None: financial_adjustments.append(max(-22, min(22, profit_growth * 0.4)))
    if roe is not None: financial_adjustments.append(max(-10, min(15, (roe - 8) * 1.2)))
    financial_direction = _direction_score(legacy.get("FINANCIAL"))
    financial_score = 50 + sum(financial_adjustments) / max(1, len(financial_adjustments))
    if financial_direction is not None:
        financial_score = 0.7 * financial_score + 0.3 * financial_direction
    result["FUNDAMENTAL_QUALITY"] = {
        "score": _clamp(financial_score) if financial or financial_direction is not None else None,
        "coverage": min(1.0, 0.25 * len(financial) + 0.15 * len(financial_adjustments)),
        "confidence": 0.8 if financial_adjustments else 0.5 if financial_direction is not None else 0,
        "direction": legacy.get("FINANCIAL", {}).get("direction", "UNKNOWN"), "method": "HYBRID",
        "summary": _summary(legacy.get("FINANCIAL"), fallback="根据结构化财务指标计算盈利质量；缺失指标不会补写。"),
        "metrics": {"revenue_growth": revenue_growth, "profit_growth": profit_growth, "roe": roe, "records": len(financial)},
        "evidence": {"fact_ids": [row.get("id") for row in financial]}, "warnings": [],
    }

    pe = (_numbers(financial_payload, ("PE", "PETTM", "pe_ratio", "市盈率")) or [None])[0]
    pb = (_numbers(financial_payload, ("PB", "pb_ratio", "市净率")) or [None])[0]
    dividend = (_numbers(financial_payload, ("DIVIDEND_YIELD", "dividend_yield", "股息率")) or [None])[0]
    valuation_parts = []
    if pe is not None:
        valuation_parts.append(25 if pe <= 0 else 68 if pe <= 15 else 58 if pe <= 30 else 42 if pe <= 50 else 30)
    if pb is not None:
        valuation_parts.append(65 if 0 < pb <= 2 else 55 if pb <= 4 else 35 if pb > 4 else 25)
    if dividend is not None:
        valuation_parts.append(_clamp(45 + dividend * 4))
    result["VALUATION_EXPECTATION"] = {
        "score": round(sum(valuation_parts) / len(valuation_parts), 2) if valuation_parts else None,
        "coverage": min(1.0, len(valuation_parts) / 3), "confidence": 0.75 if valuation_parts else 0,
        "direction": "NEUTRAL" if valuation_parts else "UNKNOWN", "method": "RULE",
        "summary": "按已入库估值和股息指标计算；未做跨行业绝对估值比较。" if valuation_parts else "缺少可识别的估值与股息指标。",
        "metrics": {"pe": pe, "pb": pb, "dividend_yield": dividend},
        "evidence": {"fact_ids": [row.get("id") for row in financial]}, "warnings": ([] if valuation_parts else ["盈利预测和估值指标覆盖不足"]),
    }

    run = db.get(SelectionRun, candidate.run_id)
    scoring_as_of = run.as_of if run is not None else candidate.created_at or _now()
    industry = _industry_context(db, candidate.market, candidate.symbol, scoring_as_of)
    excess = industry.get("excess_return_20d")
    breadth = industry.get("positive_breadth")
    industry_score = _clamp(50 + (float(excess) * 1.3 if excess is not None else 0) + ((float(breadth) - 0.5) * 24 if breadth is not None else 0)) if industry.get("status") == "SUPPORTED" else None
    result["INDUSTRY_MOMENTUM"] = {
        "score": industry_score, "coverage": min(1.0, float(industry.get("priced_peer_count") or 0) / 10),
        "confidence": min(0.85, 0.45 + float(industry.get("priced_peer_count") or 0) * 0.025) if industry_score is not None else 0,
        "direction": "POSITIVE" if industry_score is not None and industry_score >= 60 else "NEGATIVE" if industry_score is not None and industry_score < 40 else "NEUTRAL" if industry_score is not None else "UNKNOWN",
        "method": "RULE", "summary": f"{industry.get('industry') or '行业未识别'}：比较个股与同行20日表现及上涨广度。",
        "metrics": industry, "evidence": {"classification_id": industry.get("classification_id")},
        "warnings": [industry["warning"]] if industry.get("warning") else [],
    }

    price_score = None
    if bar_metrics.get("return_20d") is not None:
        price_score = 50 + float(bar_metrics["return_20d"]) * 0.8 + float(bar_metrics.get("return_5d") or 0) * 0.5
        price_score += max(-12, float(bar_metrics.get("max_drawdown") or 0) * 0.6)
        price_score -= max(0, float(bar_metrics.get("volatility") or 0) - 3) * 1.5
        price_score = _clamp(price_score)
    result["PRICE_RISK"] = {
        "score": price_score, "coverage": min(1.0, float(bar_metrics.get("bar_count") or 0) / 20),
        "confidence": 0.9 if price_score is not None else 0,
        "direction": "POSITIVE" if price_score is not None and price_score >= 60 else "NEGATIVE" if price_score is not None and price_score < 40 else "NEUTRAL" if price_score is not None else "UNKNOWN",
        "method": "RULE", "summary": "按同源日线计算区间收益、波动和回撤，不跨来源比较成交量。",
        "metrics": bar_metrics, "evidence": {"fact_ids": [row.get("id") for row in _records(observations.get("market"))]},
        "warnings": [] if price_score is not None else ["日线窗口不足，无法形成可复现价格评分"],
    }

    flow_values = _numbers([row.get("properties") or {} for row in fund_flow], (
        "net_inflow", "main_net_inflow", "主力净流入", "主力净流入净额", "主力净额",
    ))
    positive_ratio = sum(value > 0 for value in flow_values) / len(flow_values) if flow_values else None
    volume_ratio = bar_metrics.get("volume_ratio")
    capital_score = None
    if flow_values or volume_ratio is not None:
        capital_score = 50 + ((positive_ratio - 0.5) * 36 if positive_ratio is not None else 0)
        if volume_ratio is not None:
            trend = float(bar_metrics.get("return_5d") or 0)
            capital_score += min(12, max(-12, (float(volume_ratio) - 1) * 8 * (1 if trend >= 0 else -1)))
        capital_score = _clamp(capital_score)
    result["CAPITAL_BEHAVIOR"] = {
        "score": capital_score,
        "coverage": min(1.0, (0.55 if flow_values else 0) + (0.35 if volume_ratio is not None else 0)),
        "confidence": 0.58 if flow_values else 0.45 if volume_ratio is not None else 0,
        "direction": "POSITIVE" if capital_score is not None and capital_score >= 60 else "NEGATIVE" if capital_score is not None and capital_score < 40 else "NEUTRAL" if capital_score is not None else "UNKNOWN",
        "method": "HYBRID", "summary": "仅使用已识别资金字段和同源量价特征；该结果不确认主力或游资身份。",
        "metrics": {"recognized_flow_values": len(flow_values), "positive_flow_ratio": positive_ratio, "volume_ratio": volume_ratio},
        "evidence": {"fact_ids": [row.get("id") for row in fund_flow]},
        "warnings": ([] if flow_values else ["缺少口径明确的资金流字段，仅保留量价行为线索"]),
    }

    news, notice = legacy.get("NEWS"), legacy.get("NOTICE")
    event_score = _direction_score(news, notice)
    documents = _records(evidence.get("documents"))
    disclosure_docs = [row for row in documents if row.get("source_table") in {"stock_news", "stock_notice"} and row.get("content_state") != "METADATA_ONLY"]
    analyzed = {int(value) for value in model.get("analyzed_document_ids") or [] if str(value).isdigit()}
    event_coverage = min(1.0, len(analyzed) / len(disclosure_docs)) if disclosure_docs else 0
    result["EVENT_DISCLOSURE"] = {
        "score": _clamp(event_score) if event_score is not None else None,
        "coverage": event_coverage, "confidence": event_confidence,
        "direction": "MIXED" if news and notice and news.get("direction") != notice.get("direction") else str((notice or news or {}).get("direction") or "UNKNOWN"),
        "method": "LLM", "summary": _summary(news, notice, fallback="缺少已通过引用校验的新闻公告语义结果。"),
        "metrics": {"period_documents": len(disclosure_docs), "analyzed_documents": len(analyzed), "event_links": len(_records(model.get("event_links"))), "validation_findings": len(_records(model.get("validation_findings")))},
        "evidence": {"document_ids": sorted(analyzed)},
        "warnings": [] if event_score is not None else ["LLM语义研判未完成或引用未通过校验"],
    }

    company = legacy.get("COMPANY")
    governance_score = _direction_score(company)
    if governance_score is None and shareholder:
        governance_score = 50
    result["GOVERNANCE_STABILITY"] = {
        "score": _clamp(governance_score) if governance_score is not None else None,
        "coverage": min(1.0, len(shareholder) * 0.25 + (0.35 if company and company.get("status") != "INSUFFICIENT" else 0)),
        "confidence": 0.72 if shareholder and company else 0.55 if shareholder or company else 0,
        "direction": str((company or {}).get("direction") or ("NEUTRAL" if shareholder else "UNKNOWN")),
        "method": "HYBRID", "summary": _summary(company, fallback="仅检测到股东/F10来源记录，尚无足够语义证据判断治理方向。"),
        "metrics": {"shareholder_records": len(shareholder)},
        "evidence": {"fact_ids": [row.get("id") for row in shareholder]}, "warnings": [],
    }

    graph = legacy.get("GRAPH")
    paths = _records(evidence.get("graph_paths"))
    business_paths = [row for row in paths if str(row.get("predicate")) not in {"HAS_RECORD", "SUPPORTED_BY", "EVIDENCED_BY", "DESCRIBED_BY"}]
    verified_paths = [row for row in business_paths if str(row.get("verification_status") or "").upper() in {"VERIFIED", "SOURCE", "ACCEPTED"}]
    relation_score = _direction_score(graph) if verified_paths else None
    if relation_score is None and verified_paths:
        relation_score = 50
    risk_terms = ("LITIGATION", "PLEDGE", "DEFAULT", "SANCTION", "诉讼", "质押", "违约", "处罚")
    risk_count = sum(any(term in str(row.get("predicate") or "").upper() for term in risk_terms) for row in verified_paths)
    if relation_score is not None:
        relation_score = _clamp(relation_score - min(20, risk_count * 5))
    result["SUPPLY_CHAIN_RESILIENCE"] = {
        "score": relation_score, "coverage": min(1.0, len(verified_paths) / 5),
        "confidence": 0.72 if verified_paths else 0,
        "direction": str((graph or {}).get("direction") or ("NEUTRAL" if verified_paths else "UNKNOWN")),
        "method": "HYBRID", "summary": _summary(graph, fallback="图谱存在关系线索，但仅已核验且具业务含义的关系参与覆盖计算。"),
        "metrics": {"verified_business_relations": len(verified_paths), "candidate_business_relations": len(business_paths) - len(verified_paths), "risk_relations": risk_count},
        "evidence": {"relation_ids": [row.get("id") for row in verified_paths]},
        "warnings": [] if verified_paths else ["缺少已核验的产业链、经营往来或外部风险关系；候选关系不参与评分"],
    }

    profile_items = {row.dimension_code: row for row in _profile_items(db, profile.id)}
    rows: list[dict[str, Any]] = []
    for definition in DIMENSIONS:
        code = definition["code"]
        value = result[code]
        item = profile_items[code]
        supported = value["score"] is not None and value["coverage"] >= item.minimum_coverage and value["confidence"] >= item.minimum_confidence
        rows.append({
            "dimension_code": code, "dimension_name": definition["name"],
            "description": definition["description"], "score_interpretation": definition["interpretation"],
            "status": "SUPPORTED" if supported else "INSUFFICIENT", "direction": value["direction"],
            "score": value["score"] if supported else None, "observed_score": value["score"],
            "coverage": round(value["coverage"], 4), "evidence_confidence": round(value["confidence"], 4),
            "weight": item.weight,
            "weighted_contribution": round(item.weight * value["score"] / 100, 4) if supported else None,
            "method": value["method"], "summary": value["summary"], "metrics": value["metrics"],
            "evidence": value["evidence"], "warnings": value["warnings"],
            "minimum_coverage": item.minimum_coverage, "minimum_confidence": item.minimum_confidence,
            "skill_code": item.skill_code, "skill_version": item.skill_version,
        })
    return rows


def score_candidate(db: Session, candidate: SelectionCandidate, profile: SelectionStrategyProfile | None = None) -> dict[str, Any]:
    profile = profile or resolve_profile(db, market=candidate.market)
    rows = _score_dimensions(db, candidate, profile)
    run = db.get(SelectionRun, candidate.run_id)
    scoring_as_of = run.as_of if run is not None and run.as_of is not None else candidate.created_at or _now()
    supported = [row for row in rows if row["status"] == "SUPPORTED" and row["score"] is not None]
    supported_weight = sum(float(row["weight"]) for row in supported)
    base_score = sum(float(row["weight"]) * float(row["score"]) for row in supported) / supported_weight if supported_weight else None
    coverage = sum(float(row["weight"]) * float(row["coverage"]) for row in rows) / 100
    evidence_confidence = sum(float(row["weight"]) * float(row["coverage"]) * float(row["evidence_confidence"]) for row in rows) / 100
    trusted_score = 50 + (base_score - 50) * evidence_confidence if base_score is not None else None
    critical = {row["dimension_code"]: row["status"] for row in rows if row["dimension_code"] in {"PRICE_RISK", "EVENT_DISCLOSURE"}}
    status = "PASSED" if supported_weight >= 60 and critical.get("PRICE_RISK") == "SUPPORTED" else "PARTIAL"
    snapshot = serialize_profile(db, profile)
    result = {
        "contract_version": "SELECTION_SCORECARD_V1", "status": status,
        "profile": {key: snapshot[key] for key in ("id", "profile_code", "profile_name", "version", "horizon_code", "weight_total")},
        "base_score": round(base_score, 2) if base_score is not None else None,
        "trusted_score": round(_clamp(trusted_score), 2) if trusted_score is not None else None,
        "coverage": round(coverage, 4), "evidence_confidence": round(evidence_confidence, 4),
        "supported_weight": round(supported_weight, 2), "dimensions": rows,
        "profile_snapshot": snapshot, "as_of": scoring_as_of.isoformat(),
        "explanation": "可信评分会按覆盖率和证据置信度向中性50分收缩；它不是收益概率。",
    }
    candidate.analysis_json = {**(candidate.analysis_json or {}), "scoring": result}
    db.execute(delete(SelectionDimensionResult).where(
        SelectionDimensionResult.candidate_id == candidate.id,
        SelectionDimensionResult.profile_id == profile.id,
    ))
    for row in rows:
        db.add(SelectionDimensionResult(
            candidate_id=candidate.id, profile_id=profile.id,
            dimension_code=row["dimension_code"], status=row["status"], direction=row["direction"],
            score=row["score"], coverage=row["coverage"], evidence_confidence=row["evidence_confidence"],
            weight=row["weight"], weighted_contribution=row["weighted_contribution"],
            method=row["method"], summary=row["summary"], metrics_json=row["metrics"],
            evidence_json=row["evidence"], warnings_json=row["warnings"], as_of=scoring_as_of,
        ))
    db.flush()
    return result


def refresh_candidate_evaluations(db: Session, candidate: SelectionCandidate) -> list[dict[str, Any]]:
    scoring = (candidate.analysis_json or {}).get("scoring") or {}
    profile_id = int((scoring.get("profile") or {}).get("id") or 0)
    if not profile_id or not candidate.entry_date or not candidate.entry_price or candidate.entry_price <= 0:
        return []
    bars = list(db.scalars(select(StockKline).where(
        StockKline.market == candidate.market, StockKline.symbol == candidate.symbol,
        StockKline.period == "daily", StockKline.trade_date > candidate.entry_date,
    ).order_by(StockKline.trade_date.asc()).limit(7)).all())
    results = []
    for horizon in (3, 5, 7):
        row = db.scalar(select(SelectionEvaluationMetric).where(
            SelectionEvaluationMetric.candidate_id == candidate.id,
            SelectionEvaluationMetric.horizon_sessions == horizon,
        ))
        if row is None:
            row = SelectionEvaluationMetric(candidate_id=candidate.id, profile_id=profile_id, horizon_sessions=horizon)
            db.add(row)
        else:
            row.profile_id = profile_id
        observed = bars[:horizon]
        if len(observed) < horizon:
            row.status = "PENDING_DATA"
        else:
            closes = [float(item.close_price) for item in observed if item.close_price is not None]
            if len(closes) == horizon:
                row.return_pct = round((closes[-1] / candidate.entry_price - 1) * 100, 4)
                peak = candidate.entry_price
                drawdown = 0.0
                for close in closes:
                    peak = max(peak, close)
                    drawdown = min(drawdown, (close / peak - 1) * 100)
                row.max_drawdown_pct = round(drawdown, 4)
                row.direction_hit = row.return_pct > 0
                row.observed_date = observed[-1].trade_date
                row.status = "COMPLETED"
                row.details_json = {"source": "LOCAL_STOCK_KLINE", "benchmark_status": "PENDING_INDUSTRY_SERIES"}
        results.append({
            "horizon_sessions": horizon, "status": row.status,
            "return_pct": row.return_pct, "benchmark_return_pct": row.benchmark_return_pct,
            "excess_return_pct": row.excess_return_pct, "max_drawdown_pct": row.max_drawdown_pct,
            "direction_hit": row.direction_hit, "observed_date": row.observed_date,
        })
    db.flush()
    return results


def evaluation_summary(db: Session, profile_id: int | None = None) -> dict[str, Any]:
    query = select(SelectionEvaluationMetric)
    if profile_id:
        query = query.where(SelectionEvaluationMetric.profile_id == profile_id)
    rows = list(db.scalars(query).all())
    horizons = []
    for horizon in (3, 5, 7):
        selected = [row for row in rows if row.horizon_sessions == horizon]
        completed = [row for row in selected if row.status == "COMPLETED" and row.return_pct is not None]
        horizons.append({
            "horizon_sessions": horizon, "total": len(selected), "completed": len(completed),
            "pending": len(selected) - len(completed),
            "direction_hit_rate": round(sum(bool(row.direction_hit) for row in completed) / len(completed), 4) if completed else None,
            "average_return_pct": round(sum(float(row.return_pct) for row in completed) / len(completed), 4) if completed else None,
            "average_max_drawdown_pct": round(sum(float(row.max_drawdown_pct or 0) for row in completed) / len(completed), 4) if completed else None,
        })
    return {"profile_id": profile_id, "horizons": horizons, "note": "仅统计已有真实交易日数据；缺失样本不计为命中。"}
