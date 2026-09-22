"""Bounded, read-only projections of existing security data into the unified graph.

Source rows remain the source of truth.  A document mention is a candidate, not a
verified event or a causal company relationship.  No ORM objects are mutated.
"""
from __future__ import annotations

import hashlib
import json
import math
import re
from datetime import date, datetime
from statistics import median
from typing import Any

from sqlalchemy import func, select, tuple_
from sqlalchemy.orm import Session

from app.models.ai_hub import KnowledgeDocument, ResearchReportRecord
from app.models.context_event import StockContextEvent
from app.models.market_data import (
    DataSource, StockF10Cache, StockFinancialReport, StockKline, StockNews,
    StockNotice, StockRealtimeQuote,
)
from app.services.graph_identity import canonical_company_id, canonical_security_id


SUPPORTED_SOURCE_TABLES = {
    "stock_financial_report": StockFinancialReport,
    "stock_kline": StockKline,
    "stock_realtime_quote": StockRealtimeQuote,
    "stock_news": StockNews,
    "stock_notice": StockNotice,
    "stock_f10_cache": StockF10Cache,
    "research_report": ResearchReportRecord,
    "stock_context_event": StockContextEvent,
}

# Field aliases are explicit: arbitrary numeric fields (codes, years, etc.) are
# never silently converted into financial observations.
METRICS = (
    ("revenue", "营业收入", "money", ("TOTALOPERATEREVE", "TOTAL_OPERATE_INCOME", "OPERATE_INCOME", "营业总收入", "营业收入", "revenue")),
    ("parent_net_profit", "归母净利润", "money", ("PARENTNETPROFIT", "PARENT_NETPROFIT", "归母净利润", "归属于母公司股东的净利润", "parent_net_profit")),
    ("holder_profit", "股东应占利润", "money", ("HOLDER_PROFIT", "股东应占利润")),
    ("net_profit", "净利润", "money", ("NETPROFIT", "NET_PROFIT", "净利润", "net_profit")),
    ("gross_margin", "毛利率", "%", ("GROSS_PROFIT_RATIO", "销售毛利率", "毛利率", "gross_margin")),
    ("operating_cash_flow", "经营现金流净额", "money", ("NETCASH_OPERATE", "NETCASH_OPERATE_ACT", "经营活动产生的现金流量净额", "operating_cash_flow")),
    ("contract_liabilities", "合同负债", "money", ("CONTRACT_LIAB", "CONTRACT_LIABILITIES", "合同负债", "contract_liabilities")),
    ("research_expense", "研发费用", "money", ("RESEARCH_EXPENSE", "研发费用", "research_expense")),
    ("basic_eps", "基本每股收益", "per_share", ("EPSJB", "BASIC_EPS", "基本每股收益", "基本每股收益(元)", "eps")),
    ("roe", "净资产收益率", "%", ("ROEJQ", "ROE_AVG", "净资产收益率", "roe")),
    ("net_margin", "净利率", "%", ("NET_PROFIT_RATIO", "销售净利率", "净利率", "net_margin")),
    ("total_assets", "总资产", "money", ("TOTAL_ASSETS", "总资产", "total_assets")),
    ("pe", "市盈率", "倍", ("PE_TTM", "PE", "市盈率", "市盈率(TTM)", "pe")),
    ("pb", "市净率", "倍", ("PB", "市净率", "pb")),
)
TOPICS = (
    ("POLICY", "政策", r"政策|监管|法规|国务院|财政|货币"),
    ("RAW_MATERIAL", "原材料", r"原材料|锂|铜|钢|石油|大宗|商品价格"),
    ("SUPPLY_CHAIN", "供应链", r"供应链|上游|下游|供应商|客户"),
    ("SHAREHOLDER", "股东行为", r"回购|增持|减持|股东|大股东"),
    ("CONTRACT", "合同", r"合同|订单|签署|中标|协议"),
    ("LEGAL", "司法事项", r"诉讼|仲裁|判决|执行案件|立案"),
)
F10_LABELS = {
    "profile": "公司资料", "company_profile": "公司资料", "financial": "财务资料",
    "financial_summary": "财务摘要", "financial_reports": "财务报告", "shareholders": "股东资料",
    "shareholder": "股东资料", "business": "经营资料", "business_analysis": "经营分析",
    "dividends": "分红资料", "capital": "股本资料", "industry": "行业资料",
}


def _time(value: Any) -> str | None:
    if value in (None, ""):
        return None
    return value.isoformat() if isinstance(value, (datetime, date)) else str(value)


def _number(value: Any) -> float | None:
    if isinstance(value, bool) or value in (None, "", "--", "-", "不适用"):
        return None
    try:
        parsed = float(str(value).replace(",", "").replace("，", "").rstrip("%"))
    except (ValueError, TypeError):
        return None
    return parsed if math.isfinite(parsed) else None


def _text(value: Any) -> str:
    return value if isinstance(value, str) else json.dumps(value, ensure_ascii=False, default=str)


def _first(data: dict[str, Any], keys: tuple[str, ...]) -> Any:
    return next((data[key] for key in keys if data.get(key) not in (None, "")), None)


def _publication(row: Any) -> Any:
    if isinstance(row, StockFinancialReport):
        return _first(row.data_json or {}, ("NOTICE_DATE", "PUBLISH_DATE", "PUBLISHED_AT", "公告日期", "published_at"))
    if isinstance(row, StockNews):
        return row.news_time
    if isinstance(row, StockNotice):
        return row.notice_date
    if isinstance(row, StockContextEvent):
        return row.published_at
    if isinstance(row, ResearchReportRecord):
        return row.created_at
    # Collection time, quote time and trading date are not publication time.
    return None


def financial_metrics(row: StockFinancialReport) -> list[dict[str, Any]]:
    data = row.data_json or {}
    currency = row.currency or _first(data, ("CURRENCY", "币种", "货币"))
    currency_unit = {"CNY": "人民币元", "RMB": "人民币元", "HKD": "港元", "USD": "美元", "EUR": "欧元"}.get(str(currency).upper(), str(currency) if currency else "原始货币单位（币种未说明）")
    result = []
    for code, name, kind, aliases in METRICS:
        field = next((field for field in aliases if _number(data.get(field)) is not None), None)
        if not field:
            continue
        unit = currency_unit if kind == "money" else f"{currency_unit}/股" if kind == "per_share" else kind
        # An explicit source unit takes precedence; values are not rescaled or
        # converted across currencies in a read projection.
        explicit_unit = (data.get("units") or {}).get(field) if isinstance(data.get("units"), dict) else None
        unit = explicit_unit or unit
        result.append({
            "metric_code": code, "metric_name": name, "value": _number(data[field]),
            "unit": unit, "currency": currency, "report_period": row.report_period,
            "published_at": _time(_publication(row)), "raw_field": field, "raw_value": data[field],
            "restated_flag": _first(data, ("restated_flag", "RESTATED_FLAG", "是否重述")),
            "fact_only": True,
        })
    return result[:10]


class _Projection:
    def __init__(self, db: Session, market: str, symbol: str, include_evidence: bool, company_id: str | None, graph_id: int | None):
        self.db, self.market, self.symbol = db, market, symbol
        self.security_id = canonical_security_id(market, symbol)
        self.company_id = canonical_company_id(company_id) if company_id and not company_id.startswith("company:") else company_id
        self.include_evidence, self.graph_id = include_evidence, graph_id
        self.nodes: dict[str, dict[str, Any]] = {}
        self.edges: dict[str, dict[str, Any]] = {}
        self.evidence: dict[str, dict[str, Any]] = {}
        self.sources = {row.id: row for row in db.scalars(select(DataSource)).all()}

    def node(self, identifier: str, kind: str, label: str, type_label: str, properties: dict[str, Any], *, layer: str = "FACT", status: str = "NORMALIZED", evidence_ids: list[str] | None = None):
        self.nodes[identifier] = {"id": identifier, "type": kind, "type_label": type_label, "label": label,
            "layer": layer, "status": status, "properties": properties, "evidence_ids": evidence_ids or []}

    def edge(self, source: str, target: str, predicate: str, label: str, evidence_id: str, *, status: str = "NORMALIZED", method: str = "SOURCE", extra: dict[str, Any] | None = None, layer: str = "FACT"):
        evidence = self.evidence[evidence_id]
        identifier = "dynamic-edge:" + hashlib.sha256(f"{source}|{predicate}|{target}|{evidence_id}".encode()).hexdigest()[:24]
        properties = {"relation_id": identifier, "predicate": predicate, "subject_id": source, "object_id": target,
            "market": self.market, "valid_from": None, "valid_to": None, "as_of": None,
            "observed_at": evidence["observed_at"], "published_at": evidence["published_at"],
            "source_id": evidence["source_id"], "source_name": evidence["source_name"], "source_url": evidence["source_url"],
            "source_table": evidence["source_table"], "source_record_id": evidence["source_record_id"],
            "evidence_document_id": evidence_id, "extraction_method": method, "confidence": None,
            "source_reliability": None, "extraction_confidence": None, "verification_status": status,
            "version": "DYNAMIC_PROJECTION_V1", **(extra or {})}
        self.edges[identifier] = {"id": identifier, "source": source, "target": target, "type": predicate,
            "predicate": predicate, "label": label, "layer": layer, "status": status,
            "properties": properties, "evidence_ids": [evidence_id]}

    def document(self, row: Any, title: str, content: Any, document_type: str) -> str:
        table = row.__tablename__
        identifier = f"evidence:{table}:{row.id}"
        source = self.sources.get(getattr(row, "source_id", None))
        source_name = getattr(row, "source_name", None) or (source.source_name if source else None)
        if isinstance(row, ResearchReportRecord):
            source_name = "系统生成研究报告"
        source_name = source_name or "来源未说明"
        source_id = f"datasource:{source.id}" if source else "datasource:name:" + hashlib.sha256(source_name.encode()).hexdigest()[:16]
        content_text = _text(content)
        evidence = {"id": identifier, "title": title, "document_type": document_type,
            "source_name": source_name, "source_id": source_id, "source_url": getattr(row, "url", None),
            "published_at": _time(_publication(row)),
            "observed_at": _time(getattr(row, "fetched_at", None) or getattr(row, "created_at", None)),
            "excerpt": content_text[:6000], "content_truncated": len(content_text) > 6000,
            "source_table": table, "source_record_id": row.id, "market": self.market, "symbol": self.symbol,
            "legacy_document_ids": [], "canonical_security_id": self.security_id,
            "canonical_company_id": self.company_id}
        self.evidence[identifier] = evidence
        if self.include_evidence:
            self.node(identifier, "EVIDENCE_DOCUMENT", title, "证据文档", dict(evidence), layer="EVIDENCE", evidence_ids=[identifier])
            self.node(source_id, "DATA_SOURCE", source_name, "数据源", {"source_code": source.source_code if source else None}, layer="EVIDENCE")
            self.edge(identifier, source_id, "FROM_SOURCE", "来源于", identifier, layer="EVIDENCE")
        return identifier

    def fact(self, row: Any, suffix: str, kind: str, label: str, type_label: str, predicate: str, edge_label: str, properties: dict[str, Any], evidence_id: str, *, status: str = "NORMALIZED", method: str = "SOURCE") -> str:
        identifier = f"dynamic:{row.__tablename__}:{row.id}:{suffix}"
        evidence = self.evidence[evidence_id]
        props = {"market": self.market, "symbol": self.symbol, "canonical_security_id": self.security_id,
            "canonical_company_id": self.company_id, "source_table": row.__tablename__, "source_record_id": row.id,
            "source_name": evidence["source_name"], "source_url": evidence["source_url"],
            "observed_at": evidence["observed_at"], "published_at": evidence["published_at"],
            "extraction_method": method, "verification_status": status, **properties}
        self.node(identifier, kind, label, type_label, props, status=status, evidence_ids=[evidence_id])
        self.edge(self.security_id, identifier, predicate, edge_label, evidence_id, status=status, method=method)
        if self.include_evidence:
            self.edge(identifier, evidence_id, "SUPPORTED_BY", "证据支持", evidence_id, status=status, method=method, layer="EVIDENCE")
        return identifier

    def mentions(self, row: Any, text: str, evidence_id: str):
        for topic, name, pattern in TOPICS:
            match = re.search(pattern, text, re.IGNORECASE)
            if not match:
                continue
            identifier = self.fact(row, f"mention:{topic}", "CANDIDATE_EVENT", f"{name}提及（待核实）", "候选事件",
                "HAS_EVENT_MENTION", "关联候选线索", {"event_type": topic, "matched_keyword": match.group(),
                    "excerpt": text[max(0, match.start() - 50):match.end() + 100],
                    "note": "仅关键词提及，尚未核实主体、行为、时间或影响方向。"}, evidence_id,
                status="CANDIDATE", method="RULE")
            if self.include_evidence:
                self.edge(evidence_id, identifier, "MENTIONS", "提及", evidence_id, status="CANDIDATE", method="RULE", layer="FACT")

    def finish(self):
        keys = [(item["source_table"], item["source_record_id"]) for item in self.evidence.values()]
        if keys:
            query = select(KnowledgeDocument.id, KnowledgeDocument.graph_id, KnowledgeDocument.source_table, KnowledgeDocument.source_record_id).where(
                KnowledgeDocument.market == self.market, KnowledgeDocument.symbol == self.symbol,
                tuple_(KnowledgeDocument.source_table, KnowledgeDocument.source_record_id).in_(keys))
            if self.graph_id is not None:
                query = query.where(KnowledgeDocument.graph_id == self.graph_id)
            for identifier, graph_id, table, record_id in self.db.execute(query):
                self.evidence[f"evidence:{table}:{record_id}"]["legacy_document_ids"].append(identifier)
            # List references above are intentionally shared with node properties.
        return {"nodes": list(self.nodes.values()), "edges": list(self.edges.values()), "evidence": list(self.evidence.values())}


def build_dynamic_projection(db: Session, market: str, symbol: str, *, company_id: str | None = None,
    per_category: int = 3, include_evidence: bool = True, graph_id: int | None = None) -> dict[str, Any]:
    """Return current observations with bounded fan-out, provenance and coverage.

    ``per_category`` limits source rows, not the number of metrics in a report.
    The daily price series is limited to one source/adjustment regime and 21 bars.
    ``graph_id`` only restricts legacy document aliases, not source observations.
    """
    limit = max(1, min(int(per_category), 10))
    market, symbol = market.strip().upper(), symbol.strip()
    result = _Projection(db, market, symbol, include_evidence, company_id, graph_id)
    coverage: dict[str, Any] = {}
    warnings: list[str] = []

    def recent(model, order, category, count=limit):
        where = (model.market == market, model.symbol == symbol)
        total = db.scalar(select(func.count()).select_from(model).where(*where)) or 0
        rows = list(db.scalars(select(model).where(*where).order_by(order.desc(), model.id.desc()).limit(count)))
        coverage[category] = {"available": total > 0, "total_records": total, "shown_records": len(rows),
            "truncated": total > len(rows), "status": "AVAILABLE" if total else "MISSING"}
        return rows

    for row in recent(StockFinancialReport, StockFinancialReport.report_period, "financial"):
        evidence_id = result.document(row, f"{symbol} {row.report_period} 财务报告", row.data_json, "FINANCIAL_REPORT")
        metrics = financial_metrics(row)
        if not metrics:
            warnings.append(f"财务记录 {row.id} 暂无可按已知字段解析的数值，已保留原始证据。")
        if include_evidence:
            result.edge(result.security_id, evidence_id, "HAS_FINANCIAL_REPORT", "财报证据", evidence_id, layer="EVIDENCE")
        for metric in metrics:
            result.fact(row, metric["metric_code"], "FINANCIAL_OBSERVATION", f"{metric['metric_name']} · {row.report_period[:10]}",
                "财务观测", "HAS_FINANCIAL_OBSERVATION", "财务观测", metric, evidence_id)

    klines = recent(StockKline, StockKline.trade_date, "market", count=1)
    # Do not accidentally use a weekly bar as the basis for a daily anomaly.
    latest = db.scalar(select(StockKline).where(StockKline.market == market, StockKline.symbol == symbol,
        StockKline.period == "daily").order_by(StockKline.trade_date.desc(), StockKline.id.desc()).limit(1))
    if latest is not None:
        series = list(db.scalars(select(StockKline).where(StockKline.market == market, StockKline.symbol == symbol,
            StockKline.period == "daily", StockKline.adjust == latest.adjust, StockKline.source_id == latest.source_id,
            StockKline.trade_date <= latest.trade_date).order_by(StockKline.trade_date.desc(), StockKline.id.desc()).limit(21)))
        values = {key: getattr(latest, key) for key in ("trade_date", "period", "adjust", "open_price", "high_price", "low_price", "close_price", "volume", "amount", "turnover_rate")}
        evidence_id = result.document(latest, f"{symbol} {latest.trade_date} 日线行情", values, "PRICE_SERIES")
        values.update({"baseline_records": len(series) - 1, "series_start": series[-1].trade_date,
            "volume_unit": (latest.raw_payload or {}).get("volume_unit"),
            "amount_unit": (latest.raw_payload or {}).get("amount_unit"), "fact_only": True})
        result.fact(latest, "latest", "MARKET_OBSERVATION", f"日线量价 · {latest.trade_date}", "量价观测",
            "HAS_MARKET_OBSERVATION", "量价观测", values, evidence_id)
        prior = [row for row in series[1:] if _number(row.volume) is not None and row.volume > 0]
        baseline = median([row.volume for row in prior]) if len(prior) >= 3 else None
        anomalies = []
        if baseline and _number(latest.volume) is not None and latest.volume >= baseline * 3:
            anomalies.append(("VOLUME_SPIKE", "成交量异常放大", {"volume": latest.volume, "baseline_volume": baseline,
                "baseline_record_ids": [row.id for row in prior], "baseline_window": len(prior), "threshold_multiple": 3,
                "baseline_observations": [{"source_table": "stock_kline", "source_record_id": row.id,
                    "trade_date": row.trade_date, "volume": row.volume, "observed_at": _time(row.fetched_at)} for row in prior],
                "calculation": "当日成交量 ≥ 前最多20个同源同复权交易日有效成交量中位数的3倍（至少3日）"}))
        opening, closing = _number(latest.open_price), _number(latest.close_price)
        if opening and opening > 0 and closing is not None and abs((closing - opening) / opening) + 1e-12 >= 0.07:
            anomalies.append(("INTRADAY_PRICE_MOVE", "日内价格大幅变动", {"open_price": opening, "close_price": closing,
                "intraday_change_pct": (closing / opening - 1) * 100, "threshold_pct": 7,
                "calculation": "收盘相对开盘变动绝对值 ≥ 7%，不等同于昨收涨跌幅"}))
        if _number(latest.turnover_rate) is not None and latest.turnover_rate >= 20:
            anomalies.append(("TURNOVER_SPIKE", "高换手率", {"turnover_rate": latest.turnover_rate, "threshold_pct": 20}))
        for code, label, detail in anomalies:
            result.fact(latest, code, "MARKET_EVENT", f"{label} · {latest.trade_date}", "量价异常",
                "HAS_MARKET_EVENT", "观测异常", {"event_type": code, "trade_date": latest.trade_date,
                    "period": latest.period, "adjust": latest.adjust, "fact_only": True,
                    "note": "确定性检测结果，不代表上涨、下跌或资金意图。", **detail}, evidence_id, status="DERIVED", method="RULE")
        if baseline is None:
            warnings.append("同源同复权日线有效历史不足3日，未检测成交量倍数异常。")
    elif klines:
        warnings.append("存在非日线记录，但缺少日线；未混用周期生成日线摘要或异常。")
        coverage["market"]["shown_records"] = 0

    for row in recent(StockRealtimeQuote, StockRealtimeQuote.fetched_at, "realtime", count=1):
        values = {key: getattr(row, key) for key in ("quote_time", "current_price", "previous_close_price", "open_price", "high_price", "low_price", "volume", "amount", "change_amount", "change_pct", "turnover_rate")}
        evidence_id = result.document(row, f"{symbol} 实时行情快照", values, "REALTIME_QUOTE")
        result.fact(row, "quote", "MARKET_OBSERVATION", f"实时行情 · {row.quote_time or '时间未说明'}", "量价观测",
            "HAS_MARKET_OBSERVATION", "实时行情", {**values, "fact_only": True}, evidence_id)

    for model, order, category, kind, predicate, edge_label in (
        (StockNews, StockNews.news_time, "news", "NEWS", "HAS_NEWS", "相关新闻"),
        (StockNotice, StockNotice.notice_date, "notices", "NOTICE", "HAS_NOTICE", "相关公告"),
        (ResearchReportRecord, ResearchReportRecord.created_at, "research", "RESEARCH_REPORT", "HAS_RESEARCH_REPORT", "研究报告"),
    ):
        for row in recent(model, order, category):
            content = row.content if isinstance(row, StockNews) else row.content_json if isinstance(row, StockNotice) else row.report_markdown
            evidence_id = result.document(row, row.title, content or "", kind)
            if include_evidence:
                result.edge(result.security_id, evidence_id, predicate, edge_label, evidence_id, layer="EVIDENCE")
            if isinstance(row, ResearchReportRecord):
                result.evidence[evidence_id].update({"model_provider": row.model_provider, "model_instance": row.model_instance,
                    "rating": row.rating, "score": row.score, "report_sources": row.data_sources_json,
                    "note": "模型生成的研究意见，不能作为已核实事实或投资结果。"})
                if include_evidence:
                    result.nodes[evidence_id]["properties"].update(result.evidence[evidence_id])
            else:
                result.mentions(row, row.title + "\n" + _text(content or ""), evidence_id)

    for row in recent(StockF10Cache, StockF10Cache.fetched_at, "f10"):
        label = F10_LABELS.get(row.section.lower(), row.section if re.search(r"[\u4e00-\u9fff]", row.section) else "基本资料分区")
        evidence_id = result.document(row, f"{symbol} F10 {label}", row.payload_json, "F10_SECTION")
        if include_evidence:
            result.nodes[evidence_id]["properties"]["section"] = row.section
            result.edge(result.security_id, evidence_id, "HAS_F10_SECTION", "基本资料证据", evidence_id, layer="EVIDENCE")

    for row in recent(StockContextEvent, StockContextEvent.published_at, "external_events"):
        evidence_id = result.document(row, row.title, row.content, "EXTERNAL_EVENT_SOURCE")
        result.fact(row, "event", "CANDIDATE_EVENT", row.title, "外部候选事件", "HAS_EVENT_MENTION", "外部事件线索",
            {"event_type": row.event_type, "related_entity": row.related_entity, "content_hash": row.content_hash,
                "note": "来源声明的事件，当前数据表未记录审核结论；相关主体尚未解析为已核实关系。"},
            evidence_id, status="CANDIDATE")

    return {**result.finish(), "coverage": coverage, "warnings": warnings}
