"""Typed, auditable evidence cards and one conclusion publication contract.

This module is pure: projecting historical results never rewrites their audit
records, invents an acquisition date, or assigns a score to an unassessed item.
"""
from __future__ import annotations

from datetime import datetime, timezone
from math import isfinite
from typing import Any


SECTION_NAMES = {"NEWS": "新闻事件", "NOTICE": "公告披露", "FINANCIAL": "财务业绩",
                 "PRICE_VOLUME": "量价趋势", "FUND_FLOW": "资金进出", "COMPANY": "主营经营、壁垒与股东",
                 "GRAPH": "公司关联与图谱"}
DOCUMENT_SOURCES = {"NEWS": {"stock_news"}, "NOTICE": {"stock_notice"},
                    "FINANCIAL": {"stock_financial_report"},
                    "PRICE_VOLUME": {"stock_kline", "stock_realtime_quote"},
                    "FUND_FLOW": set(), "COMPANY": {"stock_symbol", "stock_f10_cache"}, "GRAPH": set()}
FACT_KINDS = {"FINANCIAL": {"FINANCIAL_OBSERVATION", "FINANCIAL_METRIC"},
              "PRICE_VOLUME": {"MARKET_OBSERVATION", "REALTIME_QUOTE", "MARKET_EVENT", "MARKET_ANOMALY"},
              "FUND_FLOW": {"FUND_FLOW_OBSERVATION"},
              "COMPANY": {"SHAREHOLDER_OBSERVATION", "SHAREHOLDER_EVENT", "COMPANY_PROFILE",
                          "BUSINESS_PROFILE", "BUSINESS_COMPOSITION", "BUSINESS_POSITION", "TECHNOLOGY_CLAIM",
                          "OWNERSHIP", "CONTROL", "BUSINESS", "SUPPLY_CHAIN", "LEGAL_PARTICIPATION"}}
CATEGORY_KINDS = {"financial": "FINANCIAL_OBSERVATION", "market": "MARKET_OBSERVATION",
                  "realtime": "REALTIME_QUOTE", "fund_flow": "FUND_FLOW_OBSERVATION",
                  "shareholder": "SHAREHOLDER_OBSERVATION", "business": "BUSINESS_PROFILE"}
SCORE_DIMENSIONS = {"EVENT_DISCLOSURE", "FUNDAMENTAL_QUALITY", "VALUATION_EXPECTATION", "PRICE_RISK",
                    "CAPITAL_BEHAVIOR", "GOVERNANCE_STABILITY", "SUPPLY_CHAIN_RESILIENCE", "INDUSTRY_MOMENTUM"}


def _finite_number(value: Any) -> bool:
    return isinstance(value, (int, float)) and not isinstance(value, bool) and isfinite(value)


def _document_matches(code: str, row: dict) -> bool:
    if row.get("source_table") not in DOCUMENT_SOURCES.get(code, set()):
        return False
    if code == "COMPANY" and row.get("source_table") == "stock_f10_cache":
        # A cached finance/fund-flow F10 section is not a company/holder fact.
        metadata = row.get("source_metadata") or {}
        section = str(metadata.get("section") or metadata.get("section_code") or row.get("title") or "").lower()
        return any(word in section for word in (
            "holder", "shareholder", "profile", "overview", "business_composition", "business",
            "股东", "公司概况", "公司资料", "公司简介", "主营构成", "主营业务",
        ))
    return True


def evidence_indexes(evidence: dict) -> tuple[dict[str, dict], dict[str, dict], dict[str, dict]]:
    documents: dict[str, dict] = {}
    for scope in (evidence.get("evidence_context") or {}, evidence):
        for key in ("documents", "related_security_documents"):
            for row in scope.get(key) or []:
                if isinstance(row, dict) and row.get("id") is not None:
                    documents[str(row["id"])] = row
    facts = {}
    for category, rows in (evidence.get("structured_observations") or {}).items():
        for row in rows if isinstance(rows, list) else []:
            if isinstance(row, dict) and row.get("id") is not None:
                facts[str(row["id"])] = {**row, "fact_type": row.get("fact_type") or CATEGORY_KINDS.get(category)}
    for row in evidence.get("facts") or []:
        if isinstance(row, dict) and row.get("id") is not None:
            facts[str(row["id"])] = row
    relations = {str(row["id"]): row for row in evidence.get("graph_paths") or []
                 if isinstance(row, dict) and row.get("id") is not None}
    return documents, facts, relations


def typed_references(dimension: dict, evidence: dict) -> dict[str, list]:
    documents, facts, relations = evidence_indexes(evidence)
    code = dimension.get("dimension")
    # A mention of financial performance in news is still a NEWS document.
    # Cross-source reasoning belongs in event linkage, not in every source card.
    return {
        "evidence_document_ids": list(dict.fromkeys(int(value) for value in dimension.get("evidence_document_ids") or []
            if str(value) in documents and _document_matches(code, documents[str(value)])
            and documents[str(value)].get("relation_scope") != "RELATED_SECURITY"
            and documents[str(value)].get("content_state") != "METADATA_ONLY")),
        "evidence_fact_ids": list(dict.fromkeys(str(value) for value in dimension.get("evidence_fact_ids") or []
            if str(value) in facts and str(facts[str(value)].get("fact_type") or "").upper() in FACT_KINDS.get(code, set()))),
        "evidence_relation_ids": list(dict.fromkeys(int(value) for value in dimension.get("evidence_relation_ids") or []
            if code == "GRAPH" and str(value) in relations)),
    }


def validate_assessments(output: dict, evidence: dict, *, required: bool = False) -> None:
    for dimension in output.get("dimensions") or []:
        references = typed_references(dimension, evidence)
        expected = {(kind, str(value)) for key, kind in (("evidence_document_ids", "DOCUMENT"),
                    ("evidence_fact_ids", "FACT"), ("evidence_relation_ids", "RELATION")) for value in references[key]}
        seen = set()
        for row in dimension.get("evidence_assessments") or []:
            identity = (row.get("reference_type"), str(row.get("reference_id")))
            if identity not in expected or identity in seen:
                raise ValueError(f"{dimension['dimension']}逐条评分存在重复、越界或类型不符的引用：{identity}")
            seen.add(identity)
            score, direction = row.get("score"), row.get("direction")
            if direction == "UNKNOWN":
                valid = score is None
            else:
                valid = isinstance(score, (float, int)) and not isinstance(score, bool) and isfinite(score) and -100 <= score <= 100
                if valid:
                    valid = (direction == "POSITIVE" and score > 0 or direction == "NEGATIVE" and score < 0
                             or direction in {"NEUTRAL", "MIXED"} and score == 0)
            if not valid:
                raise ValueError(f"{dimension['dimension']}引用{identity}逐条评分不合法：direction={direction},score={score}；利好正分、利空负分、中性/交织0分、未知null")
        if required and dimension.get("status") == "SUPPORTED" and (not expected or expected != seen):
            raise ValueError(f"{dimension['dimension']}必须对每条引用数据评分，尚缺 {len(expected - seen)} 条")


def _time_key(value: Any) -> float:
    try:
        parsed = datetime.fromisoformat(str(value).replace("Z", "+00:00"))
        return parsed.replace(tzinfo=parsed.tzinfo or timezone.utc).timestamp()
    except (ValueError, TypeError):
        return float("-inf")


def section_weights(scoring: dict) -> dict[str, float | None]:
    """Conserve the published eight-dimension weight budget; do not add weights."""
    weights = {row["dimension_code"]: row.get("weight", 0) for row in scoring.get("dimensions") or []}
    mapping = {"NEWS": {"EVENT_DISCLOSURE": .5}, "NOTICE": {"EVENT_DISCLOSURE": .5},
               "FINANCIAL": {"FUNDAMENTAL_QUALITY": .65, "VALUATION_EXPECTATION": 1},
               "PRICE_VOLUME": {"PRICE_RISK": 1}, "FUND_FLOW": {"CAPITAL_BEHAVIOR": 1},
               "COMPANY": {"GOVERNANCE_STABILITY": 1, "FUNDAMENTAL_QUALITY": .35},
               "GRAPH": {"SUPPLY_CHAIN_RESILIENCE": 1, "INDUSTRY_MOMENTUM": 1}}
    return {code: round(sum(float(weights.get(key) or 0) * ratio for key, ratio in spec.items()), 4)
            if weights else None for code, spec in mapping.items()}


def evidence_values(source: dict) -> list[dict]:
    labels = {"TOTALOPERATEREVE": "营业总收入（元）", "PARENTNETPROFIT": "归母净利润（元）",
              "EPSJB": "每股收益（元）", "BPS": "每股净资产（元）", "REPORT_DATE": "报告期",
              "TOTALOPERATEREVETZ": "营收同比（%）", "PARENTNETPROFITTZ": "净利润同比（%）",
              "current_price": "现价（元）", "close_price": "收盘价（元）", "change_pct": "涨跌幅（%）",
              "trade_date": "交易日", "quote_time": "行情时间", "volume": "成交量（来源口径）",
              "amount": "成交额（元）", "turnover_rate": "换手率（%）", "report_period": "报告期",
              "holding_ratio": "持股比例", "holding_pct": "持股比例（%）", "holding_quantity": "持股数量（股）",
              "main_net_inflow": "主力净流入（元，供应商估算）", "net_inflow": "净流入（元，供应商估算）",
              "main_business": "主营业务", "industry": "所属行业", "report_date": "主营构成报告期",
              "revenue": "主营收入（元）", "revenue_ratio": "主营收入占比（小数）",
              "profit": "主营利润（元）", "profit_ratio": "主营利润占比（小数）",
              "gross_margin": "毛利率（小数）"}
    values = {}
    def visit(value: Any, depth: int = 0) -> None:
        if depth > 5 or len(values) >= 12:
            return
        if isinstance(value, dict):
            for key, child in value.items():
                if key in labels and isinstance(child, (str, int, float)) and not isinstance(child, bool):
                    values.setdefault(labels[key], round(child, 2) if isinstance(child, float) else child)
                elif isinstance(child, (list, dict)):
                    visit(child, depth + 1)
        elif isinstance(value, list) and value:
            visit(value[0], depth + 1)
    visit(source.get("properties") or source.get("properties_json") or {})
    return [{"name": key, "value": value} for key, value in list(values.items())[:12]]


def build_evidence_sections(analysis: dict, evidence: dict) -> list[dict]:
    documents, facts, relations = evidence_indexes(evidence)
    weights = section_weights(analysis.get("scoring") or {})
    sections = []
    for dimension in (analysis.get("model") or {}).get("dimensions") or []:
        code = dimension.get("dimension")
        if code not in SECTION_NAMES:
            continue
        references = typed_references(dimension, evidence)
        contaminated = any({str(value) for value in dimension.get(key) or []} != {str(value) for value in references[key]}
                           for key in references)
        if contaminated:
            dimension = {**dimension, "status": "INSUFFICIENT", "direction": "UNKNOWN",
                         "summary": "历史分析混用了其他板块或未载入的证据，原研判不再有效，请重新分析。",
                         "evidence_assessments": []}
        assessments = {(row.get("reference_type"), str(row.get("reference_id"))): row
                       for row in dimension.get("evidence_assessments") or []}
        items = []
        for key, kind, index in (("evidence_document_ids", "DOCUMENT", documents),
                                 ("evidence_fact_ids", "FACT", facts), ("evidence_relation_ids", "RELATION", relations)):
            for reference in references[key]:
                source = index[str(reference)]
                metadata = source.get("source_metadata") or {}
                acquisition = (source.get("fetched_at") or source.get("acquired_at") or metadata.get("source_fetched_at")
                               or (source.get("metadata") or {}).get("fetched_at"))
                if kind == "FACT":
                    acquisition = acquisition or source.get("observed_at")
                assessment = assessments.get((kind, str(reference)), {})
                score = assessment.get("score")
                # Historical/invalid assessments must not acquire valid-looking scores on read.
                try:
                    validate_assessments({"dimensions": [{**dimension, **references, "evidence_assessments": [assessment]}]}, evidence)
                except (ValueError, TypeError):
                    score = None
                items.append({"reference_type": kind, "reference_id": reference,
                    "title": source.get("title") or source.get("relation_type") or source.get("predicate") or f"事实 {reference}",
                    "source_table": source.get("source_table"), "url": source.get("url"),
                    "acquired_at": acquisition, "published_at": source.get("published_at"),
                    "indexed_at": source.get("updated_at"), "direction": assessment.get("direction", "UNKNOWN"),
                    "score": score, "rationale": assessment.get("rationale") or "历史结果未逐条评分，请重新分析。",
                    "values": evidence_values(source),
                    "assessment_status": "SCORED" if score is not None else "UNSCORED"})
        items.sort(key=lambda row: (_time_key(row["acquired_at"]), _time_key(row["published_at"]), str(row["reference_id"])), reverse=True)
        scored = [row for row in items if row["score"] is not None]
        complete = bool(items) and len(scored) == len(items) and dimension.get("status") == "SUPPORTED"
        raw_score = round(sum(row["score"] for row in scored) / len(scored), 2) if complete else None
        weight = weights.get(code)
        direction = ("POSITIVE" if raw_score > 0 else "NEGATIVE" if raw_score < 0 else "NEUTRAL") if raw_score is not None else dimension.get("direction")
        sections.append({"dimension": code, "name": SECTION_NAMES[code], "status": dimension.get("status"),
            "direction": direction, "summary": dimension.get("summary"), "items": items,
            "weight": weight, "raw_score": raw_score,
            "weighted_score": round(raw_score * weight / 100, 2) if raw_score is not None and weight is not None else None,
            "scored_count": len(scored), "total_count": len(items), "score_status": "COMPLETE" if complete else "PARTIAL",
            "formula": "板块原始分=逐条影响分的算术平均；板块综合分=原始分×板块权重/100。未评分项不按0分处理。"})
    return sections


def conclusion_publication(analysis: dict) -> dict:
    """Model transport success is distinct from a publishable decision."""
    state, reason = "NOT_ANALYZED", "尚未生成模型分析"
    model = analysis.get("model") or {}
    scoring = analysis.get("scoring") or {}
    if analysis.get("model_output_status") == "NOT_RUN" or analysis.get("data_gate_status") == "MISSING_DATA":
        state, reason = "DATA_INSUFFICIENT", analysis.get("model_failure_reason") or "数据不足，未进入分析"
    elif analysis.get("model_output_status") == "FAILED":
        state, reason = "MODEL_FAILED", analysis.get("model_failure_reason") or "模型执行或结果校验失败"
    elif analysis.get("model_output_status") == "SUCCESS":
        if (analysis.get("analysis_validation_status") == "PARTIAL" or not str(model.get("reasoning") or "").strip()
                or not _finite_number(model.get("confidence")) or not 0 <= model["confidence"] <= 1):
            state, reason = "CONCLUSION_FAILED", "模型虽已返回，但引用或结论完整性校验未通过，未发布有效结论"
        elif (not _finite_number(scoring.get("trusted_score"))
              or not 0 <= scoring["trusted_score"] <= 100
              or len(scoring.get("dimensions") or []) != 8
              or {row.get("dimension_code") for row in scoring.get("dimensions") or []} != SCORE_DIMENSIONS
              or any(not _finite_number(row.get("weight"))
                     or not 0 <= row["weight"] <= 100
                     or (row.get("score") is not None and (not _finite_number(row["score"]) or not 0 <= row["score"] <= 100))
                     for row in scoring.get("dimensions") or [])
              or abs(sum(row["weight"] for row in scoring.get("dimensions") or []) - 100) > .01):
            state, reason = "SCORE_FAILED", "结论已有返回，但综合评分或雷达数据未生成，请重算评分或重新分析"
        else:
            state, reason = "READY_REVIEW", "结论与评分已生成，等待人工复核"
    return {"status": state, "reason": reason, "conclusion_available": state == "READY_REVIEW",
            "scoring_available": state == "READY_REVIEW", "retryable": state in {"MODEL_FAILED", "CONCLUSION_FAILED", "SCORE_FAILED"}}
