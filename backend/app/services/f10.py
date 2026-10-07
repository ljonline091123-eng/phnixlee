"""Atomic F10 cache, merge and normalization policies.

This module deliberately contains no FastAPI types.  It can be reused by an
HTTP request, a background job, or a command-line import without depending on
the transport layer.
"""

from __future__ import annotations

from datetime import date, datetime, timedelta
import re
from typing import Any

from sqlalchemy import select
from sqlalchemy.exc import SQLAlchemyError
from sqlalchemy.orm import Session

from app.connectors.akshare_adapter import AkshareAdapter
from app.connectors.registry import get_adapter
from app.core.markets import MARKET_NEEQ, MARKET_NEEQ_INNOVATION
from app.models.market_data import (
    DataSource,
    StockFinancialReport,
    StockNotice,
    StockRealtimeQuote,
    StockSymbol,
)
from app.models.foundation import (
    FoundationEntity,
    FoundationEvidence,
    FoundationFact,
    FoundationFactEvidence,
    FoundationListing,
    FoundationSecurity,
)
from app.services.stock_on_demand import StockOnDemandService, _merge_f10_payload
from app.services.stock_classification import _looks_like_index
from app.services.taxonomy import LABEL_DEFINITION_OVERRIDES, label_definition
from app.services.research_store import load_research_sections, persist_research_sections
from app.services.research_quality import audit_research_quality


F10_EXTENDED_SECTIONS = (
    "published_reports",
    "profile",
    "holders",
    "fund_flow",
    "financial_summary",
    "financial_statements",
    "business_composition",
)
F10_OPTIONAL_SECTIONS = ("research_sections",)
F10_CACHE_SECTIONS = F10_EXTENDED_SECTIONS + F10_OPTIONAL_SECTIONS

NOTICE_CATEGORIES = (
    "全部",
    "财务业绩",
    "重大事项",
    "风险提示",
    "抵押担保",
    "增持回购",
    "对外投资",
    "其他公告",
)

# Keep the historical import name for callers while making taxonomy.py the
# single source of truth for label-level explanations.
CONCEPT_GLOSSARY: dict[str, str] = {
    item["label"]: item["definition"] for item in LABEL_DEFINITION_OVERRIDES
}

# Frequently returned provider labels that are more specific than the generic
# dimension glossary.  Keep these local read-model definitions so older caches
# become useful immediately; the source label/code remains attached to each
# row and can later be promoted into the versioned taxonomy table.
PROVIDER_CONCEPT_DEFINITIONS: dict[tuple[str, str], str] = {
    ("INDUSTRY", "货币金融服务"): "以货币金融中介、银行及相关金融服务为主的来源行业分类。",
    ("INDUSTRY", "银行"): "以吸收存款、发放贷款、支付结算及其他银行金融服务为主的来源行业分类。",
    ("INDUSTRY", "银行Ⅱ"): "银行行业下用于细分银行业成分证券的第二级来源行业分类。",
    ("INDUSTRY", "股份制银行Ⅲ"): "银行行业下按股份制商业银行口径归集的第三级来源行业分类。",
    ("INDUSTRY", "房地产开发"): "按住宅、商业或综合不动产开发、销售及配套运营成分证券归集的来源行业分类。",
    ("THEME", "大盘价值"): "按来源将较大市值规模与相对价值特征组合识别的投资风格主题，随市值和估值时点变化。",
    ("THEME", "大盘股"): "按来源口径以总市值或流通市值区间划分的较大规模证券主题，必须结合采集日期和口径解释。",
    ("THEME", "低市净率"): "按来源采集时点市净率处于较低区间的估值风格主题，不代表未来收益。",
}


def _concept_definition(name: str, dimension: str | None = None) -> str:
    text = str(name or "").strip()
    normalized_dimension = str(dimension or "THEME").upper()
    master = label_definition(text, dimension)
    if master and str(master.get("dimension") or "").upper() == normalized_dimension:
        return str(master.get("definition") or "")
    concrete = PROVIDER_CONCEPT_DEFINITIONS.get((normalized_dimension, text))
    if concrete:
        return concrete
    if text in CONCEPT_GLOSSARY:
        return CONCEPT_GLOSSARY[text]
    prefix = "行业归属" if normalized_dimension == "INDUSTRY" else "投资主题"
    # Do not present a broad dimension sentence as if it were a real
    # definition for an unknown label.  The explicit unresolved wording is
    # useful to users and downstream quality checks, while preserving the
    # observed label for later taxonomy enrichment.
    return (
        f"“{text}”当前仅是来源主数据登记的{prefix}标签，系统尚未建立该标签的专属释义；"
        "请结合来源代码、成分名单和原始披露核验，不代表公司全部收入或投资建议。"
    )


def _normalize_concept_rows(rows: list[Any]) -> list[dict[str, Any]]:
    normalized: list[dict[str, Any]] = []
    seen: set[tuple[str, str]] = set()
    for raw in rows:
        if not isinstance(raw, dict):
            continue
        name = str(raw.get("name") or raw.get("label") or "").strip()
        if not name or _looks_like_index(name, raw.get("code")):
            continue
        dimension = str(raw.get("dimension") or raw.get("classification_dimension") or "THEME").upper()
        key = (dimension, name)
        if key in seen:
            continue
        seen.add(key)
        item = dict(raw)
        item["name"] = name
        item.setdefault("label", name)
        item["dimension"] = dimension
        item["relevance_label"] = item.get("relevance_label") or "最相关"
        existing_definition = str(item.get("definition") or "").strip()
        master = label_definition(name, dimension)
        if master and str(master.get("dimension") or "").upper() != dimension:
            master = None
        glossary_definition = str(master.get("definition") or "") if master else CONCEPT_GLOSSARY.get(name)
        generic_theme_definition = (
            "数据提供方根据公司业务、产品、公告或市场约定整理的投资主题集合" in existing_definition
            or "数据提供方标注的行业名称" in existing_definition
            or existing_definition == str(item.get("master_definition") or "").strip()
            and bool(item.get("master_dimension"))
        )
        if master and glossary_definition:
            if existing_definition and existing_definition != glossary_definition:
                item.setdefault("fact_definition", existing_definition)
            item["definition"] = glossary_definition
            if not item.get("criteria") or generic_theme_definition:
                item["criteria"] = master.get("criteria")
            item["definition_source"] = master.get("source_name") or "系统分类标签释义"
            item["definition_version"] = master.get("definition_version") or "LABEL_GLOSSARY_V1"
            item["taxonomy"] = master.get("taxonomy") or "CN_SECURITIES"
            item["master_definition"] = glossary_definition
            item["master_criteria"] = master.get("criteria")
            item["master_definition_version"] = item["definition_version"]
        elif glossary_definition and (not existing_definition or "需结合原文核验" in existing_definition
                                     or "版本化主数据释义" in existing_definition or generic_theme_definition):
            if existing_definition and existing_definition != glossary_definition:
                item.setdefault("fact_definition", existing_definition)
            item["definition"] = glossary_definition
            item["definition_source"] = "系统概念释义词典"
            item["definition_version"] = "CONCEPT_GLOSSARY_V1"
        elif not existing_definition or "需结合原文核验" in existing_definition or "版本化主数据释义" in existing_definition:
            item["definition"] = _concept_definition(name, dimension)
        else:
            item["definition"] = existing_definition
        item["definition_source"] = item.get("definition_source") or item.get("source_name") or item.get("source") or "证券分类主数据"
        item["related_stocks"] = item.get("related_stocks") if isinstance(item.get("related_stocks"), list) else []
        normalized.append(item)
    return normalized


def _holder_value(row: dict[str, Any], aliases: tuple[str, ...]) -> Any:
    """Read an exact holder field without fuzzy substitution."""
    for alias in aliases:
        if row.get(alias) not in (None, ""):
            return row.get(alias)
    normalized = {str(key).lower().replace("_", "").replace("-", ""): value for key, value in row.items()}
    for alias in aliases:
        key = alias.lower().replace("_", "").replace("-", "")
        if normalized.get(key) not in (None, ""):
            return normalized[key]
    return None


def _holder_period(row: dict[str, Any]) -> str:
    value = _holder_value(row, ("报告期", "截止日期", "截至日期", "股东户数统计截止日", "持股日期", "date", "report_date"))
    return str(value or "").strip()[:19]


def _holder_name(row: dict[str, Any]) -> str:
    return str(_holder_value(row, ("股东名称", "股东", "名称", "股东全称", "name")) or "").strip()


def _holder_shares(row: dict[str, Any]) -> float | None:
    value = _holder_value(row, ("持股数量", "持股数", "股份数", "数量", "shares", "holdings"))
    try:
        return float(value) if value not in (None, "") else None
    except (TypeError, ValueError):
        return None


def _normalize_holder_read_rows(rows: Any) -> list[dict[str, Any]]:
    """Normalize legacy holder caches and add period-over-period deltas.

    Fresh adapter responses already carry these fields, but old F10 snapshots
    must receive the same contract when read.  A delta is emitted only when a
    named holder appears in two actual reporting periods; missing history is
    deliberately left unknown rather than labelled as a new holder.
    """
    if not isinstance(rows, list):
        return rows if isinstance(rows, list) else []
    normalized = [AkshareAdapter._normalize_holder_payload(row) if isinstance(row, dict) else row for row in rows]
    dict_rows = [row for row in normalized if isinstance(row, dict)]
    periods = sorted({period for row in dict_rows if (period := _holder_period(row))}, reverse=True)
    if len(periods) < 2:
        return normalized
    current_period, previous_period = periods[0], periods[1]
    previous_by_name = {
        _holder_name(row): row
        for row in dict_rows
        if _holder_period(row) == previous_period and _holder_name(row)
    }
    for row in dict_rows:
        if _holder_period(row) != current_period:
            continue
        name = _holder_name(row)
        if not name:
            continue
        current = _holder_shares(row)
        previous = _holder_shares(previous_by_name.get(name, {})) if name in previous_by_name else None
        if current is not None and previous is not None:
            delta = round(current - previous, 2)
            row.setdefault("holding_delta", delta)
            row.setdefault("变动值", delta)
            row.setdefault("holding_change", "不变" if abs(delta) < 1e-9 else ("↑ 增持" if delta > 0 else "↓ 减持"))
            row.setdefault("变动", row["holding_change"])
        elif name not in previous_by_name:
            row.setdefault("holding_delta", None)
            row.setdefault("holding_change", "新进")
            row.setdefault("变动", "新进")
    return normalized


RATING_STATISTIC_PERIODS: tuple[tuple[str, int], ...] = (
    ("1个月内", 31),
    ("2个月内", 62),
    ("3个月内", 93),
    ("6个月内", 186),
    ("1年内", 366),
)


def _parse_research_date(value: Any) -> date | None:
    if isinstance(value, datetime):
        return value.date()
    if isinstance(value, date):
        return value
    text = str(value or "").strip()
    if not text:
        return None
    text = re.sub(r"年|月", "-", text.replace("日", ""))
    text = text.replace("/", "-").replace(".", "-")
    if re.fullmatch(r"\d{8}", text):
        text = f"{text[:4]}-{text[4:6]}-{text[6:]}"
    text = text[:10]
    try:
        return datetime.strptime(text, "%Y-%m-%d").date()
    except ValueError:
        return None


def _research_rating(value: Any) -> str:
    text = str(value or "").strip()
    if re.search(r"强烈?买入|买入|推荐", text):
        return "买入"
    if re.search(r"强烈?增持|增持", text):
        return "增持"
    if re.search(r"持有", text):
        return "持有"
    if re.search(r"中性", text):
        return "中性"
    if re.search(r"减持", text):
        return "减持"
    if re.search(r"卖出|回避", text):
        return "卖出"
    return ""


def build_rating_statistics(rows: list[Any]) -> dict[str, Any]:
    """Build rating counts against the latest available report date.

    The provider may be stale or may return no reports for the current
    calendar year.  Using ``date.today()`` would make every historical bucket
    appear empty, so this read model uses the newest report date as its
    explicit observation anchor.  Counts represent institution/rating rows,
    matching the UI's “家数” wording rather than the number of reports.
    """
    dated_rows: list[tuple[dict[str, Any], date]] = []
    for raw in rows or []:
        if not isinstance(raw, dict):
            continue
        report_date = _parse_research_date(
            raw.get("最新报告日期") or raw.get("日期") or raw.get("报告日期") or raw.get("report_date")
        )
        if report_date is not None:
            dated_rows.append((raw, report_date))
    anchor = max((item[1] for item in dated_rows), default=None)
    buckets: list[dict[str, Any]] = []
    for period, days in RATING_STATISTIC_PERIODS:
        counts = {"buy": 0, "add": 0, "neutral": 0, "hold": 0, "reduce": 0, "sell": 0}
        if anchor is not None:
            cutoff = anchor - timedelta(days=days)
            for row, report_date in dated_rows:
                if report_date < cutoff or report_date > anchor:
                    continue
                rating = _research_rating(row.get("评级") or row.get("东财评级") or row.get("rating"))
                if rating == "买入":
                    counts["buy"] += 1
                elif rating == "增持":
                    counts["add"] += 1
                elif rating == "中性":
                    counts["neutral"] += 1
                elif rating == "持有":
                    counts["hold"] += 1
                elif rating == "减持":
                    counts["reduce"] += 1
                elif rating == "卖出":
                    counts["sell"] += 1
        buckets.append({
            "period": period,
            **counts,
            "total": sum(counts.values()),
        })
    return {
        "reference_date": anchor.isoformat() if anchor else None,
        "reference_basis": "最新报告日期" if anchor else "无可解析报告日期",
        "buckets": buckets,
    }


def project_company_control_facts(db: Session, stock: StockSymbol) -> dict[str, Any]:
    """Project source-backed company control facts into the F10 read model.

    F10 providers do not expose a stable controlling-holder endpoint for every
    security.  The company graph already stores explicit ``CONTROLS`` facts;
    this read-only projection makes those facts visible without inferring
    control from a percentage.  Pending facts remain visibly pending and every
    row carries its evidence provenance.  Missing foundation tables/data are a
    normal unavailable state, not an exception from the stock detail route.
    """
    empty = {"rows": [], "source": "公司关系图谱", "message": "暂无已登记的控股股东或实际控制人事实"}
    try:
        listing = db.scalar(select(FoundationListing).where(FoundationListing.stock_symbol_id == stock.id))
        if listing is None:
            return empty
        security = db.get(FoundationSecurity, listing.security_id)
        if security is None or not security.entity_id:
            return empty
        company_id = security.entity_id
        facts = list(db.scalars(select(FoundationFact).where(
            FoundationFact.fact_type == "CONTROLS",
            FoundationFact.object_entity_id == company_id,
            FoundationFact.status.in_(("ACCEPTED", "PENDING")),
        ).order_by(FoundationFact.updated_at.desc(), FoundationFact.id)).all())
        if not facts:
            return empty
        rows: list[dict[str, Any]] = []
        for fact in facts:
            subject = db.get(FoundationEntity, fact.subject_entity_id)
            if subject is None:
                continue
            properties = fact.properties_json if isinstance(fact.properties_json, dict) else {}
            control_role = str(properties.get("control_role") or "CONTROLLING_HOLDER").upper()
            if control_role not in {"CONTROLLING_HOLDER", "ACTUAL_CONTROLLER"}:
                control_role = "CONTROLLING_HOLDER"
            role_label = "实际控制人" if control_role == "ACTUAL_CONTROLLER" else "控股股东"
            evidence_links = list(db.scalars(select(FoundationEvidence).join(
                FoundationFactEvidence, FoundationFactEvidence.evidence_id == FoundationEvidence.id
            ).where(FoundationFactEvidence.fact_id == fact.id).order_by(FoundationEvidence.created_at.desc())).all())
            evidence = evidence_links[0] if evidence_links else None
            row: dict[str, Any] = {
                "主体名称": subject.name,
                "关系": role_label,
                "control_role": control_role,
                "事实状态": fact.status,
                "status": fact.status,
                "control_basis": properties.get("control_basis") or "来源明确披露，未根据持股比例推断",
                "fact_id": fact.id,
                "evidence_id": evidence.id if evidence else None,
                "source_name": evidence.source_name if evidence else "公司关系图谱",
                "source_url": evidence.url if evidence else None,
                "source_title": evidence.title if evidence else fact.title,
                "observed_at": evidence.available_at.isoformat() if evidence else None,
            }
            # Ratios are copied only when explicitly disclosed on the related
            # fact; no ratio means no fabricated percentage.
            for key in ("ratio", "direct_ratio", "indirect_ratio", "ratio_basis"):
                if properties.get(key) not in (None, ""):
                    row[key] = properties[key]
            rows.append(row)
        return {"rows": rows, "source": "公司关系图谱（来源证据）", "message": "" if rows else empty["message"]}
    except SQLAlchemyError:
        # Optional foundation tables may be absent in a legacy installation.
        return empty


def classify_notice(title: str | None, notice_type: str | None = None) -> str:
    """Normalize source-specific notice labels to the stock-page taxonomy.

    The original source label is retained by the caller.  This classifier is
    intentionally conservative: a keyword match creates a display category,
    not a business fact or an investment signal.
    """
    text = "".join(str(value or "") for value in (title, notice_type)).replace(" ", "")
    if any(word in text for word in (
        "年度报告", "年报", "半年度报告", "半年报", "中期报告", "季度报告", "一季报", "三季报",
        "业绩预告", "业绩快报", "财务报表", "财务报告", "审计报告", "利润分配",
    )):
        return "财务业绩"
    if any(word in text for word in ("担保", "抵押", "质押", "借款", "授信", "保证")):
        return "抵押担保"
    if any(word in text for word in ("增持", "减持", "回购", "股份变动", "持股变动")):
        return "增持回购"
    if any(word in text for word in ("对外投资", "投资设立", "投资项目", "设立子公司", "收购", "并购")):
        return "对外投资"
    if any(word in text for word in (
        "风险", "退市", "警示", "立案", "处罚", "诉讼", "仲裁", "无法表示意见", "延期披露",
    )):
        return "风险提示"
    if any(word in text for word in (
        "重大事项", "重大合同", "重大资产", "重组", "关联交易", "股权转让", "停牌", "复牌",
        "中标", "合同", "订单", "董事会", "股东大会", "人事任免",
    )):
        return "重大事项"
    return "其他公告"

def _empty_extended_data(message: str) -> dict[str, dict]:
    """Keep optional F10 modules available even when an upstream source is unavailable."""
    return {
        "profile": {"source": "暂无", "fields": {}, "message": message},
        "holders": {"source": "暂无", "major": [], "circulating": [], "message": message},
        "fund_flow": {"source": "暂无", "rows": [], "message": message},
        "financial_summary": {"source": "暂无", "periods": [], "rows": [], "message": message},
        "financial_statements": {
            "source": "暂无",
            "balance_sheet": {"label": "资产负债表", "periods": []},
            "income_statement": {"label": "利润表", "periods": []},
            "cash_flow": {"label": "现金流量表", "periods": []},
            "message": message,
        },
        "business_composition": {
            "source": "暂无",
            "report_date": None,
            "sections": [],
            "message": message,
        },
        "research_sections": {
            "source": "暂无",
            "sections": [],
            "message": message,
        },
        "published_reports": {
            "source": "暂无",
            "reports": [],
            "message": message,
        },
    }


def _display_section(
    key: str,
    title: str,
    *,
    rows: list[dict[str, Any]] | None = None,
    source: str | None = None,
    as_of: str | None = None,
    message: str | None = None,
) -> dict[str, Any]:
    rows = rows or []
    normalized_status = "AVAILABLE" if rows else "UNAVAILABLE"
    if any(
        isinstance(row, dict)
        and str(row.get("status") or row.get("事实状态") or row.get("verification_status") or "").upper()
        in {"PENDING", "待核验", "待审核"}
        for row in rows
    ):
        normalized_status = "PENDING"
    elif rows and message and message not in {"", "当前数据源未返回该分区数据"}:
        normalized_status = "PARTIAL"
    elif not rows and message and any(token in message for token in ("未接入", "待核验", "需授权")):
        normalized_status = "PENDING"
    return {
        "key": key,
        "title": title,
        "status": normalized_status,
        "rows": rows,
        "source": source or "暂无",
        "as_of": as_of,
        "message": message or ("" if rows else "当前数据源未返回该分区数据"),
    }


def _research_rows(value: Any) -> list[dict[str, Any]]:
    if isinstance(value, dict):
        value = value.get("rows")
    return [row for row in value if isinstance(row, dict)] if isinstance(value, list) else []


def _forecast_years(*row_sets: list[dict[str, Any]]) -> list[str]:
    years: set[int] = set()
    for rows in row_sets:
        for row in rows:
            for key, value in row.items():
                if value in (None, ""):
                    continue
                match = re.search(r"20\d{2}", str(key))
                if match:
                    years.add(int(match.group(0)))
            direct = str(row.get("forecast_year") or row.get("预测年度") or "")[:4]
            if re.fullmatch(r"20\d{2}", direct):
                years.add(int(direct))
            forecast = row.get("forecast")
            if isinstance(forecast, dict):
                for values in forecast.values():
                    if isinstance(values, dict):
                        years.update(int(year) for year in values if re.fullmatch(r"20\d{2}", str(year)))
    future = sorted(year for year in years if year >= date.today().year)
    start = future[0] if future else date.today().year
    return [str(year) for year in range(start, start + 3)]


def _is_dedicated_institution_forecast(row: dict[str, Any]) -> bool:
    """Reject legacy brokerage-report rows once mixed into this section.

    Older caches appended the complete Eastmoney research-report archive to
    ``institution_forecast``.  Those rows often carry EPS estimates, so merely
    checking for forecast values is insufficient.  Keep provider rows that are
    explicitly sourced from the institution-forecast feed (currently THS), and
    retain title-less legacy forecast rows as a compatibility fallback.
    """
    source_code = str(row.get("source_code") or "").upper()
    source_name = str(row.get("source_name") or row.get("source") or "")
    if source_code:
        return source_code == "TONGHUASHUN"
    if "同花顺" in source_name and ("预测" in source_name or "机构" in source_name):
        return True
    if any(token in source_name.upper() for token in ("EASTMONEY", "东方财富", "研究报告", "研报")):
        return False
    return not bool(row.get("title") or row.get("报告名称"))


FORECAST_METRIC_CATALOG: tuple[tuple[str, str, str], ...] = (
    ("TOTAL_REVENUE", "营业总收入", "亿元"),
    ("REVENUE_YOY", "营业总收入同比", "%"),
    ("NET_PROFIT", "归母净利润", "亿元"),
    ("NET_PROFIT_YOY", "归母净利润同比", "%"),
    ("EPS", "每股收益", "元/股"),
    ("FORWARD_PE", "预测市盈率", "倍"),
    ("DIVIDEND_YIELD", "预测股息率", "%"),
    ("PRETAX_PROFIT", "利润总额", "亿元"),
    ("OCF_PER_SHARE", "每股现金流", "元/股"),
    ("BVPS", "每股净资产", "元/股"),
    ("ROE", "净资产收益率", "%"),
)
_FORECAST_METRIC_BY_CODE = {
    code: {"metric_code": code, "metric_name": name, "unit": unit}
    for code, name, unit in FORECAST_METRIC_CATALOG
}


def _first_forecast_value(row: dict[str, Any], *keys: str) -> Any:
    for key in keys:
        value = row.get(key)
        if value not in (None, ""):
            return value
    return None


def _forecast_metric_definition(row: dict[str, Any]) -> dict[str, str] | None:
    metric_name = str(row.get("metric") or row.get("预测指标") or row.get("metric_name") or "").strip()
    explicit_code = str(row.get("metric_code") or "").strip().upper()
    if explicit_code in _FORECAST_METRIC_BY_CODE:
        definition = dict(_FORECAST_METRIC_BY_CODE[explicit_code])
        definition["metric_name"] = metric_name or definition["metric_name"]
        definition["unit"] = str(row.get("unit") or definition["unit"])
        return definition
    if not metric_name:
        return None
    if "同比" in metric_name and any(token in metric_name for token in ("营业收入", "营业总收入", "营收")):
        code = "REVENUE_YOY"
    elif "同比" in metric_name and any(token in metric_name for token in ("净利润", "归母净利")):
        code = "NET_PROFIT_YOY"
    elif "利润总额" in metric_name:
        code = "PRETAX_PROFIT"
    elif "每股现金流" in metric_name:
        code = "OCF_PER_SHARE"
    elif "每股净资产" in metric_name:
        code = "BVPS"
    elif "净资产收益率" in metric_name or metric_name.upper() == "ROE":
        code = "ROE"
    elif any(token in metric_name for token in ("营业收入", "营业总收入", "营收")):
        code = "TOTAL_REVENUE"
    elif "每股收益" in metric_name or re.search(r"\bEPS\b", metric_name, re.I):
        code = "EPS"
    elif any(token in metric_name for token in ("净利润", "归母净利")):
        code = "NET_PROFIT"
    elif "股息率" in metric_name:
        code = "DIVIDEND_YIELD"
    elif "市盈率" in metric_name or re.search(r"(?:预测)?\s*PE", metric_name, re.I):
        code = "FORWARD_PE"
    else:
        code = explicit_code or metric_name
        return {
            "metric_code": code,
            "metric_name": metric_name,
            "unit": str(row.get("unit") or ""),
        }
    definition = dict(_FORECAST_METRIC_BY_CODE[code])
    definition["metric_name"] = metric_name or definition["metric_name"]
    definition["unit"] = str(row.get("unit") or definition["unit"])
    return definition


def _forecast_source_code(row: dict[str, Any], fallback: str = "UNKNOWN") -> str:
    code = str(row.get("source_code") or "").strip().upper()
    if code:
        return code
    name = str(row.get("source_name") or row.get("source") or "").upper()
    if "同花顺" in name or "THS" in name:
        return "TONGHUASHUN"
    if "东方财富" in name or "东财" in name or "EAST" in name:
        return "EASTMONEY"
    if "新浪" in name or "SINA" in name:
        return "SINA_FINANCE"
    return fallback


def _report_level_forecast_observations(report_rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """Return source-preserving report estimates without promoting them to consensus."""
    projected = AkshareAdapter._derive_research_earnings_forecast(report_rows, "")
    observations: list[dict[str, Any]] = []
    for row in projected:
        year = str(row.get("forecast_year") or row.get("预测年度") or "")[:4]
        if not re.fullmatch(r"20\d{2}", year):
            continue
        common = {
            "forecast_year": year,
            "institution": row.get("institution") or row.get("机构"),
            "report_date": row.get("report_date") or row.get("日期"),
            "report_title": row.get("title") or row.get("报告名称"),
            "rating": row.get("rating") or row.get("东财评级"),
            "source_code": _forecast_source_code(row, "EASTMONEY"),
            "source_name": row.get("source_name") or "东方财富研究报告",
            "source_url": row.get("source_url"),
            "detail_url": row.get("detail_url"),
            "pdf_url": row.get("pdf_url") or row.get("报告PDF链接"),
            "external_id": row.get("external_id") or row.get("report_id"),
            "value_scope": "REPORT_LEVEL",
            "methodology": "SOURCE_REPORTED_ESTIMATE",
        }
        for code, aliases in (
            ("EPS", ("eps", "预测每股收益")),
            ("FORWARD_PE", ("pe", "预测市盈率")),
            ("NET_PROFIT", ("net_profit", "预测净利润")),
        ):
            value = _first_forecast_value(row, *aliases)
            if value in (None, ""):
                continue
            observation = {**common, **_FORECAST_METRIC_BY_CODE[code], "value": value}
            observations.append({key: item for key, item in observation.items() if item not in (None, "")})
    deduped: dict[tuple[str, str, str, str, str], dict[str, Any]] = {}
    for item in observations:
        key = (
            str(item.get("metric_code") or ""),
            str(item.get("forecast_year") or ""),
            str(item.get("source_code") or ""),
            str(item.get("external_id") or ""),
            str(item.get("institution") or ""),
        )
        deduped[key] = item
    return sorted(
        deduped.values(),
        key=lambda item: (
            str(item.get("forecast_year") or ""),
            str(item.get("report_date") or ""),
            str(item.get("external_id") or ""),
        ),
        reverse=True,
    )


def _provider_rating_bucket(provider: Any) -> dict[str, Any] | None:
    if not isinstance(provider, dict):
        return None
    count_keys = ("buy", "add", "neutral", "hold", "reduce", "sell")
    if not any(key in provider and provider.get(key) not in (None, "") for key in (*count_keys, "total")):
        return None

    def count(key: str) -> int:
        try:
            return int(float(provider.get(key) or 0))
        except (TypeError, ValueError):
            return 0

    counts = {key: count(key) for key in count_keys}
    try:
        total = int(float(provider["total"])) if provider.get("total") not in (None, "") else sum(counts.values())
    except (TypeError, ValueError):
        total = sum(counts.values())
    return {
        "period": "6个月内",
        **counts,
        "total": total,
        "source_code": _forecast_source_code(provider, "EASTMONEY"),
        "source_name": provider.get("source_name") or "供应商原生评级统计",
        "source_url": provider.get("source_url"),
        "reference_date": provider.get("as_of") or provider.get("source_updated_at"),
        "reference_basis": "供应商原生近六个月评级快照",
        "calculation_method": "PROVIDER_NATIVE",
    }


def build_research_contract(research: dict[str, Any]) -> dict[str, Any]:
    """Project persisted/raw research rows into one fixed frontend contract."""
    result = dict(research or {})
    earnings_rows = _research_rows(result.get("earnings_forecast"))
    institution_rows = [
        row for row in _research_rows(result.get("institution_forecast"))
        if _is_dedicated_institution_forecast(row)
    ]
    report_rows = _research_rows(result.get("reports"))
    report_rows.sort(
        key=lambda row: str(row.get("report_date") or row.get("报告日期") or row.get("日期") or ""),
        reverse=True,
    )
    report_observations = _report_level_forecast_observations(report_rows)
    years = _forecast_years(earnings_rows, institution_rows, report_observations)
    metric_map: dict[str, dict[str, Any]] = {}
    source_metadata: dict[tuple[str, str, str], dict[str, Any]] = {}

    def register_source(row: dict[str, Any], *, role: str, methodology: str) -> dict[str, Any]:
        source_code = _forecast_source_code(row)
        source_name = str(row.get("source_name") or row.get("source") or source_code).strip()
        source_url = row.get("source_url") or row.get("detail_url")
        key = (role, source_code, str(source_url or ""))
        metadata = source_metadata.setdefault(key, {
            "role": role,
            "source_code": source_code,
            "source_name": source_name,
            "source_url": source_url,
            "methodology": methodology,
        })
        observed_at = row.get("source_updated_at") or row.get("as_of") or row.get("report_date") or row.get("日期")
        if observed_at and str(observed_at) > str(metadata.get("observed_at") or ""):
            metadata["observed_at"] = observed_at
        return metadata

    for row in earnings_rows:
        definition = _forecast_metric_definition(row)
        year = str(row.get("forecast_year") or row.get("预测年度") or "")[:4]
        if definition is None or year not in years:
            continue
        # Actual historical observations remain in ``rows`` for auditability,
        # but only forecast observations should populate the forward matrix.
        if row.get("actual") is True or row.get("is_forecast") is False:
            continue
        metric_code = definition["metric_code"]
        metric = metric_map.setdefault(metric_code, {
            "metric_code": metric_code,
            "metric_name": definition["metric_name"],
            "unit": definition["unit"],
            "value_scope": "CONSENSUS",
            "values": {},
        })
        source = register_source(row, role="CONSENSUS", methodology="PROVIDER_CONSENSUS")
        observation = {
            "prediction_count": _first_forecast_value(row, "prediction_count", "预测机构数"),
            "min": _first_forecast_value(row, "minimum", "最小值"),
            "mean": _first_forecast_value(row, "mean", "均值", "value", "预测值"),
            "max": _first_forecast_value(row, "maximum", "最大值"),
            "industry_average": _first_forecast_value(row, "industry_average", "行业平均数"),
            "source_code": source["source_code"],
            "source_name": source["source_name"],
            "source_url": source.get("source_url"),
            "source_updated_at": row.get("source_updated_at") or row.get("as_of"),
            "value_scope": "CONSENSUS",
            "methodology": "PROVIDER_CONSENSUS",
        }
        observation = {key: value for key, value in observation.items() if value not in (None, "")}
        value_record = metric["values"].setdefault(year, {"consensus_observations": []})
        value_record["consensus_observations"].append(observation)
        incoming_priority = 100 if source["source_code"] == "TONGHUASHUN" else 10
        if incoming_priority > int(value_record.get("_source_priority") or -1):
            retained_reports = value_record.get("report_observations")
            retained_consensus = value_record["consensus_observations"]
            value_record.clear()
            value_record.update(observation)
            value_record["consensus_observations"] = retained_consensus
            if retained_reports:
                value_record["report_observations"] = retained_reports
            value_record["_source_priority"] = incoming_priority

    for observation in report_observations:
        code = str(observation["metric_code"])
        year = str(observation["forecast_year"])
        definition = _FORECAST_METRIC_BY_CODE[code]
        metric = metric_map.setdefault(code, {
            **definition,
            "value_scope": "REPORT_LEVEL",
            "values": {},
        })
        register_source(observation, role="REPORT_OBSERVATION", methodology="SOURCE_REPORTED_ESTIMATE")
        value_record = metric["values"].setdefault(year, {})
        value_record.setdefault("report_observations", []).append(observation)
        # A provider consensus remains the primary cell value.  When no
        # consensus exists, use the latest dated report observation and mark
        # its narrower scope explicitly instead of presenting it as consensus.
        if value_record.get("value_scope") != "CONSENSUS" and "mean" not in value_record:
            current_date = str(value_record.get("report_date") or "")
            incoming_date = str(observation.get("report_date") or "")
            if "value" not in value_record or incoming_date >= current_date:
                value_record.update({
                    "value": observation["value"],
                    "source_code": observation.get("source_code"),
                    "source_name": observation.get("source_name"),
                    "source_url": observation.get("source_url"),
                    "report_date": observation.get("report_date"),
                    "institution": observation.get("institution"),
                    "external_id": observation.get("external_id"),
                    "value_scope": "REPORT_LEVEL",
                    "methodology": "LATEST_SOURCE_REPORT_OBSERVATION",
                })

    for metric in metric_map.values():
        for value in metric["values"].values():
            value.pop("_source_priority", None)
        scopes = {
            str(value.get("value_scope") or "")
            for value in metric["values"].values()
            if isinstance(value, dict)
        }
        metric["value_scope"] = "MIXED" if len(scopes) > 1 else (next(iter(scopes)) if scopes else metric["value_scope"])

    available_codes = sorted(
        code for code, metric in metric_map.items()
        if any(
            any(value.get(key) not in (None, "") for key in ("value", "mean", "min", "max"))
            for value in metric["values"].values()
        )
    )
    expected_codes = [code for code, _, _ in FORECAST_METRIC_CATALOG]
    completeness_by_metric = []
    for code, name, _unit in FORECAST_METRIC_CATALOG:
        metric = metric_map.get(code)
        available_years = sorted(
            year for year, value in (metric.get("values", {}) if metric else {}).items()
            if any(value.get(key) not in (None, "") for key in ("value", "mean", "min", "max"))
        )
        completeness_by_metric.append({
            "metric_code": code,
            "metric_name": name,
            "status": "AVAILABLE" if available_years else "MISSING",
            "available_years": available_years,
            "missing_years": [year for year in years if year not in available_years],
        })
    completeness_ratio = round(len(available_codes) / len(expected_codes), 4) if expected_codes else 0.0
    result["earnings_forecast"] = {
        "forecast_years": years,
        "metrics": list(metric_map.values()),
        "rows": earnings_rows,
        "report_level_observations": report_observations,
        "source": result.get("earnings_forecast_source") or result.get("source"),
        "source_metadata": list(source_metadata.values()),
        "completeness": {
            "status": "COMPLETE" if completeness_ratio == 1 else "PARTIAL" if available_codes else "UNAVAILABLE",
            "coverage_ratio": completeness_ratio,
            "expected_metric_codes": expected_codes,
            "available_metric_codes": available_codes,
            "missing_metric_codes": [code for code in expected_codes if code not in available_codes],
            "by_metric": completeness_by_metric,
        },
    }

    normalized_institutions: list[dict[str, Any]] = []
    for raw in institution_rows:
        row = dict(raw)
        forecast = row.get("forecast") if isinstance(row.get("forecast"), dict) else {}
        eps = dict(forecast.get("eps") or {}) if isinstance(forecast, dict) else {}
        net_profit = dict(forecast.get("net_profit") or {}) if isinstance(forecast, dict) else {}
        for key, value in row.items():
            match = re.search(r"(20\d{2})", str(key))
            if not match or value in (None, ""):
                continue
            if re.search(r"每股收益|EPS|盈利预测.*收益", str(key), re.I):
                eps.setdefault(match.group(1), value)
            elif re.search(r"净利润|归母净利", str(key)):
                net_profit.setdefault(match.group(1), value)
        row.update({
            "institution": row.get("institution") or row.get("机构") or row.get("机构名称"),
            "report_date": row.get("report_date") or row.get("报告日期"),
            "rating": row.get("rating") or row.get("评级") or row.get("东财评级"),
            "eps": {year: eps.get(year) for year in years if year in eps},
            "net_profit": {year: net_profit.get(year) for year in years if year in net_profit},
        })
        normalized_institutions.append(row)
    rating = build_rating_statistics(report_rows)
    provider_rating = _provider_rating_bucket(result.get("provider_rating_statistics"))
    rating_buckets = list(rating["buckets"])
    rating_basis = rating["reference_basis"]
    if provider_rating is not None:
        rating_buckets = [
            provider_rating if row.get("period") == "6个月内" else row
            for row in rating_buckets
        ]
        rating_basis = "供应商原生近六个月评级快照优先；其他窗口按最新研报日期滚动统计"
    result["institution_forecast"] = {
        "forecast_years": years,
        "rows": normalized_institutions,
        "rating_statistics": rating_buckets,
        "rating_reference_date": rating["reference_date"],
        "rating_reference_basis": rating_basis,
        "rating_provider_reference_date": provider_rating.get("reference_date") if provider_rating else None,
        "rating_source_metadata": ([{
            "period": "6个月内",
            "source_code": provider_rating.get("source_code"),
            "source_name": provider_rating.get("source_name"),
            "source_url": provider_rating.get("source_url"),
            "calculation_method": provider_rating.get("calculation_method"),
            "reference_date": provider_rating.get("reference_date"),
        }] if provider_rating else []) + [{
            "periods": [row[0] for row in RATING_STATISTIC_PERIODS if row[0] != "6个月内" or provider_rating is None],
            "source_code": "LOCAL_REPORT_AGGREGATION",
            "source_name": "本地研报评级滚动统计",
            "calculation_method": "LOCAL_ROLLING_WINDOWS",
            "reference_date": rating["reference_date"],
        }],
        "provider_rating_statistics": result.get("provider_rating_statistics") or {},
        "source": result.get("institution_forecast_source") or result.get("source"),
    }

    cutoff = date.today() - timedelta(days=365)
    latest: list[dict[str, Any]] = []
    archive: list[dict[str, Any]] = []
    for raw in report_rows:
        row = dict(raw)
        source_code = str(row.get("source_code") or "EASTMONEY").upper()
        external_id = str(row.get("external_id") or row.get("report_id") or "")
        row["source_code"] = source_code
        row["external_id"] = external_id
        row["detail_api_url"] = (
            f"/api/v1/stocks/{{market}}/{{symbol}}/research/reports/{source_code}/{external_id}"
        )
        parsed = _parse_research_date(row.get("report_date") or row.get("报告日期") or row.get("日期"))
        (latest if parsed and parsed >= cutoff else archive).append(row)
    if not latest and report_rows and not any(
        _parse_research_date(row.get("report_date") or row.get("报告日期") or row.get("日期"))
        for row in report_rows
    ):
        # Legacy provider snapshots sometimes omitted every report date while
        # retaining newest-first order. Keep a bounded compatibility preview
        # without inventing dates; all remaining rows stay in the archive.
        latest = archive[:10]
        archive = archive[10:]
        for row in latest:
            row.setdefault("date_status", "UNKNOWN_LEGACY_ORDER")
    result["latest_reports"] = latest
    result["reports"] = archive
    return result


def _research_industry_performance_rows(concepts: list[Any]) -> list[dict[str, Any]]:
    """Project observed member-stock changes for the research tab.

    Classification labels without source-backed members deliberately stay out
    of this section.  A missing quote is unknown and is never converted into a
    zero return, so the page can distinguish an unavailable trend from a flat
    one.
    """
    result: list[dict[str, Any]] = []
    for raw in concepts or []:
        if not isinstance(raw, dict):
            continue
        members = raw.get("related_stocks")
        if not isinstance(members, list) or not members:
            continue
        changes: list[float] = []
        rise = fall = 0
        for member in members:
            if not isinstance(member, dict):
                continue
            value = member.get("change_pct")
            try:
                numeric = float(value)
            except (TypeError, ValueError):
                continue
            changes.append(numeric)
            if numeric > 0:
                rise += 1
            elif numeric < 0:
                fall += 1
        if not changes:
            continue
        name = str(raw.get("name") or raw.get("label") or "").strip()
        if not name:
            continue
        result.append({
            "name": name,
            "label": name,
            "dimension": raw.get("dimension") or raw.get("classification_dimension") or "THEME",
            "classification_dimension": raw.get("classification_dimension") or raw.get("dimension") or "THEME",
            "code": raw.get("code"),
            "average_change_pct": round(sum(changes) / len(changes), 2),
            "trend_pct": round(sum(changes) / len(changes), 2),
            "sample_size": len(changes),
            "member_count": len(members),
            "rise_count": rise,
            "fall_count": fall,
            "related_stocks": members,
            "source_name": raw.get("source_name") or raw.get("source") or "分类成分股行情快照",
            "source_url": raw.get("source_url"),
        })
    result.sort(key=lambda row: str(row.get("name") or ""))
    return result


def normalize_f10_sections(extended_data: dict[str, dict[str, Any]]) -> dict[str, dict[str, Any]]:
    """Attach stable section contracts to the existing F10 cache payloads.

    The cache keeps the original provider payloads. These derived sections are
    read-model metadata, so old data remains compatible while the UI gets a
    fixed seven/five/six-section layout and explicit unavailable states.
    """
    profile = extended_data.setdefault("profile", {})
    fields = profile.get("fields") if isinstance(profile.get("fields"), dict) else {}
    summary = extended_data.setdefault("financial_summary", {})
    statements = extended_data.setdefault("financial_statements", {})
    holders = extended_data.setdefault("holders", {})
    composition = extended_data.setdefault("business_composition", {})

    # Bring historical caches to the same holder contract as newly fetched
    # rows.  This is read-model normalization only; raw provider payloads are
    # retained in the cache for auditability.
    for holder_key in (
        "major", "circulating", "institutional", "holder_count",
        "restricted_release", "capital_structure", "control",
    ):
        if isinstance(holders.get(holder_key), list):
            holders[holder_key] = _normalize_holder_read_rows(holders[holder_key])

    def sort_rows_latest(payload: dict[str, Any], field_names: tuple[str, ...]) -> None:
        rows = payload.get("rows")
        if not isinstance(rows, list):
            return
        payload["rows"] = sorted(
            rows,
            key=lambda row: max(
                (str(row.get(name) or "")[:19] for name in field_names if isinstance(row, dict)),
                default="",
            ),
            reverse=True,
        )

    fund_payload = extended_data.get("fund_flow")
    if isinstance(fund_payload, dict):
        sort_rows_latest(fund_payload, ("日期", "交易日期", "持股日期", "date", "trade_date"))
    dividend_rows = profile.get("dividends")
    if isinstance(dividend_rows, list):
        profile["dividends"] = sorted(
            dividend_rows,
            key=lambda row: max(
                (
                    str(row.get(name) or "")[:19]
                    for name in ("公告日期", "实施方案公告日期", "除权日", "股权登记日", "最新公告日期", "date")
                    if isinstance(row, dict)
                ),
                default="",
            ),
            reverse=True,
        )

    def payload_as_of(payload: dict[str, Any] | None) -> str | None:
        if not isinstance(payload, dict):
            return None
        return payload.get("as_of") or payload.get("report_date") or (payload.get("_meta") or {}).get("fetched_at")

    concepts = profile.get("concepts") if isinstance(profile.get("concepts"), list) else []
    # Older caches were built before index membership was split from themes;
    # remove only the recognisable index labels from that derived read model.
    concepts = [
        item for item in concepts
        if isinstance(item, dict)
        and not _looks_like_index(item.get("name") or item.get("label"), item.get("code"))
    ]
    # The typed stock-classification projection is the authoritative glossary
    # for the concept panel.  Older caches may already contain provider labels
    # with a generic placeholder definition; merge the typed item into that
    # fact instead of dropping the provider row.  Index membership remains a
    # separate dimension and is never promoted to an industry/theme card.
    groups = profile.get("classification_groups")
    typed_concepts: dict[tuple[str, str], dict[str, Any]] = {}
    if isinstance(groups, list):
        for group in groups:
            if not isinstance(group, dict) or str(group.get("key") or "").lower() not in {"industry", "theme", "board"}:
                continue
            dimension = {"industry": "INDUSTRY", "theme": "THEME", "board": "BOARD"}[str(group.get("key") or "").lower()]
            for item in group.get("items") or []:
                if not isinstance(item, dict):
                    continue
                name = item.get("name") or item.get("label")
                if not name or _looks_like_index(name, item.get("code")):
                    continue
                typed = dict(item)
                typed.update({
                    "name": str(name),
                    "label": str(item.get("label") or name),
                    "dimension": item.get("dimension") or dimension,
                })
                typed_key = (str(typed.get("dimension") or dimension).upper(), str(name).strip().casefold())
                typed_concepts.setdefault(typed_key, typed)

    merged_concepts: list[dict[str, Any]] = []
    seen_concepts: set[tuple[str, str]] = set()
    placeholder_definitions = {
        "来源披露的行业/概念标签，需结合原文核验。",
        "来源披露的行业/概念标签，需结合原文核验",
    }
    for raw_item in concepts:
        if not isinstance(raw_item, dict):
            continue
        name = raw_item.get("name") or raw_item.get("label")
        if not name or _looks_like_index(name, raw_item.get("code")):
            continue
        key = (str(raw_item.get("dimension") or "THEME").upper(), str(name).strip().casefold())
        typed = typed_concepts.get(key)
        item = dict(raw_item)
        if typed:
            # Preserve concrete provider wording as fact_definition, while
            # using the versioned classification glossary for display.
            concrete_definition = item.get("definition")
            typed_definition = typed.get("definition")
            if typed_definition and (not concrete_definition or concrete_definition in placeholder_definitions):
                item["definition"] = typed_definition
            elif concrete_definition and typed_definition and concrete_definition != typed_definition:
                item.setdefault("fact_definition", concrete_definition)
            for field in ("label", "code", "dimension", "criteria", "source", "source_name",
                          "source_url", "definition_version", "master_definition", "master_criteria",
                          "master_source_name", "master_source_url", "master_definition_version", "taxonomy"):
                if item.get(field) in (None, "") and typed.get(field) not in (None, ""):
                    item[field] = typed[field]
        item.setdefault("name", str(name))
        item.setdefault("label", str(name))
        item.setdefault("definition_status", "MASTER_DATA" if item.get("definition_version") else "UNRESOLVED")
        merged_concepts.append(item)
        seen_concepts.add(key)

    for key, typed in typed_concepts.items():
        if key not in seen_concepts:
            merged_concepts.append(typed)
    concepts = _normalize_concept_rows(merged_concepts)
    if not concepts:
        # Index membership is a separate dimension and must not be presented
        # as an investment concept.  Legacy caches may have no concept field;
        # leave that section empty rather than inventing a theme from indexes.
        raw_concepts = fields.get("所属概念") or fields.get("概念")
        if raw_concepts:
            concepts = _normalize_concept_rows([
                {
                    "name": item.strip(),
                    "dimension": "THEME",
                    "definition_status": "UNRESOLVED",
                }
                for item in str(raw_concepts).replace("，", ",").split(",")
                if item.strip()
            ])
    profile["concepts"] = concepts
    anomaly_section = _display_section(
        "anomaly",
        "异动揭秘",
        rows=profile.get("margin_history"),
        source=profile.get("margin_source"),
        message=("" if profile.get("margin_history") else "融资融券明细暂未从公开接口返回"),
    )
    anomaly_section["as_of"] = payload_as_of(profile)
    anomaly_section["action_label"] = "融资融券近一个月"
    anomaly_section["detail_title"] = "融资融券近一个月"
    profile["overview_sections"] = [
        _display_section("basic", "基本情况", rows=[
            {"label": key, "value": fields.get(key)}
            for key in ("公司名称", "所属行业", "所属市场", "所属板块", "上市日期", "法人代表")
            if fields.get(key) not in (None, "")
        ], source=profile.get("source"), as_of=payload_as_of(profile)),
        _display_section("indicators", "主要指标", rows=summary.get("rows"), source=summary.get("source"), as_of=payload_as_of(summary)),
        anomaly_section,
        _display_section("insight", "道破天机", rows=holders.get("insight_rows"), source=holders.get("insight_source") or holders.get("source"), as_of=payload_as_of(holders)),
        _display_section("dividend", "分红配送", rows=profile.get("dividends"), source=profile.get("dividend_source"), as_of=payload_as_of(profile), message="" if profile.get("dividends") else "暂无分红配送明细"),
        _display_section("company", "公司相关", rows=[
            {"label": key, "value": fields.get(key)}
            for key in ("公司名称", "注册地址", "办公地址", "主营业务", "经营范围", "公司网址")
            if fields.get(key) not in (None, "")
        ], source=profile.get("source"), as_of=payload_as_of(profile)),
        _display_section("issuance", "发行相关", rows=[
            {"label": key, "value": fields.get(key)}
            for key in ("发行价", "发行数量", "发行市盈率", "上市日期", "每手股数")
            if fields.get(key) not in (None, "")
        ], source=profile.get("source"), as_of=payload_as_of(profile)),
    ]
    # Always rebuild the canonical seven-part holder read model. Older cache
    # rows may contain only the original ``major``/``circulating`` payloads or
    # a partial section list; rebuilding keeps the page shape stable without
    # deleting any raw provider fields from the cache.
    holders["sections"] = [
        _display_section("capital_structure", "股本结构", rows=holders.get("capital_structure"), source=holders.get("capital_structure_source") or holders.get("source"), as_of=payload_as_of(holders)),
        _display_section("restricted_release", "限售解禁", rows=holders.get("restricted_release"), source=holders.get("restricted_release_source") or holders.get("source"), as_of=payload_as_of(holders)),
        _display_section("institutional", "机构持股", rows=holders.get("institutional"), source=holders.get("institutional_source") or holders.get("source"), as_of=payload_as_of(holders)),
        _display_section("holder_count", "股东户数", rows=holders.get("holder_count"), source=holders.get("holder_count_source") or holders.get("source"), as_of=payload_as_of(holders)),
        _display_section("top_ten_circulating", "十大流通股东", rows=holders.get("circulating"), source=holders.get("circulating_source") or holders.get("source"), as_of=payload_as_of(holders)),
        _display_section("top_ten", "十大股东", rows=holders.get("major"), source=holders.get("major_source") or holders.get("source"), as_of=payload_as_of(holders)),
        _display_section(
            "control",
            "控股股东与实际控制人",
            rows=holders.get("control"),
            source=holders.get("control_source") or holders.get("source"),
            as_of=payload_as_of(holders),
            message=holders.get("control_message"),
        ),
    ]
    composition_rows: list[dict[str, Any]] = []
    for section in composition.get("sections") or []:
        if not isinstance(section, dict):
            continue
        category = section.get("category") or section.get("name") or "主营业务"
        items = section.get("items") if isinstance(section.get("items"), list) else []
        if not items:
            composition_rows.append({"label": category, "value": section.get("summary") or section})
            continue
        for item in items:
            item_record = item if isinstance(item, dict) else {"value": item}
            composition_rows.append({
                "label": item_record.get("name") or item_record.get("项目") or category,
                "value": item_record.get("value") or item_record.get("amount") or item_record.get("data") or item_record.get("summary") or item_record,
                "category": category,
            })
    summary["financial_sections"] = [
        _display_section("composition", "主营构成", rows=composition_rows, source=composition.get("source"), as_of=payload_as_of(composition)),
        _display_section("indicators", "主要指标", rows=summary.get("rows"), source=summary.get("source"), as_of=payload_as_of(summary)),
        _display_section("income", "利润表", rows=(statements.get("income_statement") or {}).get("rows"), source=statements.get("source"), as_of=payload_as_of(statements)),
        _display_section("balance", "资产负债表", rows=(statements.get("balance_sheet") or {}).get("rows"), source=statements.get("source"), as_of=payload_as_of(statements)),
        _display_section("cash_flow", "现金流量表", rows=(statements.get("cash_flow") or {}).get("rows"), source=statements.get("source"), as_of=payload_as_of(statements)),
    ]
    research = extended_data.setdefault("research_sections", {})
    reports = _research_rows(research.get("reports"))
    # Legacy cache rows may contain only report-derived earnings estimates.
    # Preserve that fallback for the earnings panel, but never manufacture an
    # institution forecast from ordinary研报 rows.
    if reports:
        report_symbol = next(
            (
                str(item.get("股票代码") or item.get("证券代码") or item.get("symbol") or "")
                for item in reports
                if isinstance(item, dict)
                and (item.get("股票代码") or item.get("证券代码") or item.get("symbol"))
            ),
            "",
        )
        if not _research_rows(research.get("earnings_forecast")):
            research["earnings_forecast"] = AkshareAdapter._derive_research_earnings_forecast(reports, report_symbol)
    research = build_research_contract(research)
    research["data_quality"] = audit_research_quality(research, concepts=concepts)
    extended_data["research_sections"] = research
    earnings_contract = research.get("earnings_forecast") or {}
    earnings_section = _display_section(
        "earnings_forecast",
        "盈利预测",
        rows=earnings_contract.get("rows"),
        source=earnings_contract.get("source") or research.get("earnings_forecast_source") or research.get("source"),
        as_of=payload_as_of(research),
    )
    for key in (
        "forecast_years", "metrics", "report_level_observations",
        "source_metadata", "completeness",
    ):
        earnings_section[key] = earnings_contract.get(key) or ([] if key != "completeness" else {})
    institution_section = _display_section(
        "institution_forecast",
        "机构预测（评级统计）",
        rows=(research.get("institution_forecast") or {}).get("rows"),
        source=research.get("institution_forecast_source") or research.get("source"),
        as_of=payload_as_of(research),
    )
    institution_contract = research.get("institution_forecast") or {}
    institution_section["forecast_years"] = institution_contract.get("forecast_years") or []
    institution_section["rating_statistics"] = institution_contract.get("rating_statistics") or []
    institution_section["rating_statistics_reference_date"] = institution_contract.get("rating_reference_date")
    institution_section["rating_statistics_basis"] = institution_contract.get("rating_reference_basis")
    institution_section["rating_provider_reference_date"] = institution_contract.get("rating_provider_reference_date")
    institution_section["rating_source_metadata"] = institution_contract.get("rating_source_metadata") or []
    provider_rating = institution_contract.get("provider_rating_statistics") or research.get("provider_rating_statistics")
    if isinstance(provider_rating, dict) and provider_rating:
        institution_section["provider_rating_statistics"] = provider_rating
    research_sections = [
        _display_section("industry_concepts", "行业概念", rows=concepts, source=profile.get("source"), as_of=payload_as_of(profile)),
    ]
    # 成分股数量、涨跌家数和整体走势已经随行业/概念行返回，并在分类
    # 详情中展示。研究页契约保持截图定义的六个一级板块，避免重复生成
    # 单独的“行业表现”第七板块。
    research_sections.extend([
        _display_section(
            "qa",
            "问董秘",
            rows=research.get("qa"),
            source=research.get("qa_source") or research.get("source"),
            as_of=payload_as_of(research),
            message="" if research.get("qa") else (research.get("qa_message") or "当前未接入问董秘公开接口"),
        ),
        earnings_section,
        institution_section,
        _display_section("latest_reports", "最新研报", rows=research.get("latest_reports"), source=research.get("report_source") or research.get("source"), as_of=payload_as_of(research)),
        _display_section("reports", "研报", rows=research.get("reports"), source=research.get("report_source") or research.get("source"), as_of=payload_as_of(research)),
    ])
    research["sections"] = research_sections
    return extended_data

def _load_f10_extended_data(db: Session, market: str, symbol: str) -> dict[str, dict]:
    cached = StockOnDemandService(db).load_f10_cache(market, symbol)
    extended_data = _empty_extended_data("本地暂无缓存，可点击“拉取最新”获取。")
    for section in F10_CACHE_SECTIONS:
        payload = cached.get(section)
        if isinstance(payload, dict):
            extended_data[section] = payload
    normalized_research = load_research_sections(db, market, symbol)
    if any(normalized_research.get(key) for key in ("reports", "earnings_forecast", "institution_forecast", "qa")):
        legacy = extended_data.get("research_sections")
        merged_research = dict(legacy) if isinstance(legacy, dict) else {}
        for key, rows in normalized_research.items():
            if rows:
                merged_research[key] = rows
        merged_research["source"] = "本地研究规范表 / stock_f10_cache"
        extended_data["research_sections"] = merged_research
    if cached:
        extended_data["profile"]["message"] = extended_data["profile"].get("message") or "本地缓存已加载。"
    return extended_data

def _section_has_payload(section: str, payload: dict | None, market: str | None = None) -> bool:
    if not isinstance(payload, dict):
        return False
    if section == "profile":
        fields = payload.get("fields")
        if not isinstance(fields, dict) or not fields:
            return False
        if market == "HK" and not any(key in fields for key in ("上市日期", "市盈率", "总市值(港元)", "港股市值(港元)")):
            return False
        return True
    if section == "holders":
        return any(
            bool(payload.get(key))
            for key in (
                "major", "circulating", "capital_structure", "restricted_release",
                "institutional", "holder_count", "control", "official_links",
            )
        )
    if section == "fund_flow":
        return bool(payload.get("rows"))
    if section == "financial_summary":
        return bool(payload.get("rows"))
    if section == "financial_statements":
        for key in ("balance_sheet", "income_statement", "cash_flow"):
            statement = payload.get(key)
            if isinstance(statement, dict) and bool(statement.get("periods") or statement.get("rows")):
                return True
        return False
    if section == "business_composition":
        return bool(payload.get("sections"))
    if section == "research_sections":
        return any(
            bool(payload.get(key))
            for key in ("sections", "reports", "earnings_forecast", "institution_forecast", "qa")
        )
    if section == "published_reports":
        reports = payload.get("reports")
        if not isinstance(reports, list):
            return False
        return any(
            isinstance(item, dict)
            and str(item.get("report_type") or "").lower()
            not in {"financial_indicators", "announcement"}
            for item in reports
        )
    return False

def _needs_f10_refresh(extended_data: dict[str, dict], market: str | None = None) -> bool:
    if market in (MARKET_NEEQ, MARKET_NEEQ_INNOVATION):
        return any(
            not _section_has_payload(section, extended_data.get(section), market)
            for section in ("profile", "financial_summary", "published_reports")
        )
    if any(not _section_has_payload(section, extended_data.get(section), market) for section in F10_EXTENDED_SECTIONS):
        return True
    # Research aggregates are an optional CN A enhancement.  Existing HK and
    # NEEQ snapshots should remain usable without repeatedly retrying a source
    # that does not publish the same endpoints.
    if market == "CN_A" and not _section_has_payload("research_sections", extended_data.get("research_sections"), market):
        return True
    if market in {"CN_A", "HK"}:
        if _fund_flow_is_stale(extended_data.get("fund_flow"), market=market):
            return True
    if market == "HK":
        if _hk_published_reports_need_refresh(extended_data.get("published_reports")):
            return True
    return False


def _fund_flow_is_stale(payload: dict | None, *, market: str | None = None, max_age_days: int = 5) -> bool:
    """Check freshness from the latest business date, not cache fetch time.

    A cache can be fetched successfully while the provider still returns an
    older trading day.  Both A-share and HK pages use the same read-model
    contract, with a small weekend/holiday tolerance so opening a page does
    not trigger a refresh loop when exchanges are closed.
    """
    if not isinstance(payload, dict):
        return True
    rows = payload.get("rows")
    if not isinstance(rows, list) or not rows:
        return True
    candidates: list[date] = []
    for row in rows:
        if not isinstance(row, dict):
            continue
        for key in ("日期", "交易日期", "持股日期", "date", "trade_date"):
            value = row.get(key)
            if not value:
                continue
            text = str(value).strip().replace("/", "-").split(" ", 1)[0]
            try:
                candidates.append(date.fromisoformat(text[:10]))
            except ValueError:
                continue
            break
    if not candidates:
        return True
    latest = max(candidates)
    # ``market`` is intentionally accepted for future exchange-specific
    # holidays; the current tolerance is valid for both CN_A and HK.
    _ = market
    return (date.today() - latest).days > max_age_days

def _hk_fund_flow_is_stale(payload: dict | None) -> bool:
    if not isinstance(payload, dict):
        return True
    rows = payload.get("rows")
    if not isinstance(rows, list) or not rows:
        return True
    latest_text = ""
    for row in rows:
        if not isinstance(row, dict):
            continue
        value = row.get("持股日期") or row.get("日期") or row.get("date")
        if value:
            latest_text = max(latest_text, str(value).split(" ", 1)[0])
    if not latest_text:
        return True
    try:
        latest_date = date.fromisoformat(latest_text[:10])
    except ValueError:
        return False
    return (date.today() - latest_date).days > 14

def _hk_published_reports_need_refresh(
    payload: dict | None,
    financial_summary: dict | None = None,
    financial_statements: dict | None = None,
    notices: list[StockNotice] | None = None,
) -> bool:
    if not isinstance(payload, dict):
        return True
    reports = payload.get("reports")
    if not isinstance(reports, list) or not reports:
        return True
    valid_reports = [item for item in reports if isinstance(item, dict)]
    if not valid_reports:
        return True
    latest = max(
        valid_reports,
        key=lambda item: (
            str(item.get("report_date") or ""),
            str(item.get("notice_date") or ""),
        ),
    )
    latest_report_date = str(latest.get("report_date") or "").strip()
    if not bool(str(latest.get("url") or "").strip()):
        return True

    latest_known_period = ""
    if isinstance(financial_summary, dict):
        periods = financial_summary.get("periods")
        if isinstance(periods, list):
            latest_known_period = max((str(item or "").strip() for item in periods if str(item or "").strip()), default="")
    if not latest_known_period and isinstance(financial_statements, dict):
        for key in ("balance_sheet", "income_statement", "cash_flow"):
            statement = financial_statements.get(key)
            if not isinstance(statement, dict):
                continue
            periods = statement.get("periods")
            if isinstance(periods, list):
                candidate = max((str(item.get("报告日期") or item.get("report_date") or "").strip() for item in periods if isinstance(item, dict)), default="")
                if candidate > latest_known_period:
                    latest_known_period = candidate

    if notices:
        for item in notices:
            if not isinstance(item, StockNotice):
                continue
            combined_text = f"{item.title or ''} {item.notice_type or ''}".strip()
            if not combined_text or "月报" in combined_text or "月報" in combined_text:
                continue
            if AkshareAdapter._is_hk_financial_report_title(combined_text):
                report_type = AkshareAdapter._normalize_hk_report_type(AkshareAdapter._guess_hk_report_type(combined_text))
                report_date = AkshareAdapter._guess_hk_report_date(combined_text, report_type, item.notice_date)
                if report_date and report_date[:10] > latest_known_period:
                    return True

    return bool(latest_known_period and latest_report_date and latest_report_date < latest_known_period)

def _merge_f10_extended_data(base: dict[str, dict], fresh: dict[str, dict]) -> dict[str, dict]:
    merged = dict(base)
    for section in F10_CACHE_SECTIONS:
        payload = fresh.get(section)
        if isinstance(payload, dict) and payload:
            existing = merged.get(section)
            merged[section] = _merge_f10_payload(section, existing if isinstance(existing, dict) else None, payload)
    return merged

def _hk_report_notice_priority(text: str) -> int:
    normalized = text.replace(" ", "")
    if "月报" in normalized or "月報" in normalized:
        return -1000
    score = 0
    if "财务报表" in normalized or "財務報表" in normalized:
        score += 120
    if any(keyword in normalized for keyword in ("年度报告", "年度報告", "中期报告", "中期報告", "半年度报告", "半年度報告", "年报", "年報", "半年报", "半年報")):
        score += 100
    if "业绩" in normalized or "業績" in normalized:
        score += 60
    if "股息" in normalized or "分派" in normalized:
        score -= 20
    return score

def _prefer_hk_report_notice(candidate: dict, current: dict | None) -> bool:
    if not current:
        return True
    candidate_priority = int(candidate.get("_priority") or _hk_report_notice_priority(str(candidate.get("title") or "")))
    current_priority = int(current.get("_priority") or _hk_report_notice_priority(str(current.get("title") or "")))
    if candidate_priority != current_priority:
        return candidate_priority > current_priority
    candidate_has_link = bool(str(candidate.get("url") or "").strip())
    current_has_link = bool(str(current.get("url") or "").strip())
    if candidate_has_link != current_has_link:
        return candidate_has_link
    return str(candidate.get("notice_date") or "") > str(current.get("notice_date") or "")

def _notice_to_hk_report_entry(notice: StockNotice, symbol: str) -> dict[str, Any] | None:
    combined_text = f"{notice.title or ''} {notice.notice_type or ''}".strip()
    if not combined_text or _hk_report_notice_priority(combined_text) < 0:
        return None
    if not AkshareAdapter._is_hk_financial_report_title(combined_text):
        return None
    report_type = AkshareAdapter._normalize_hk_report_type(AkshareAdapter._guess_hk_report_type(combined_text))
    report_date = AkshareAdapter._guess_hk_report_date(combined_text, report_type, notice.notice_date)
    if not report_date:
        return None
    entry = AkshareAdapter._normalize_hk_financial_report_entry(
        symbol=symbol,
        report_type=report_type,
        report_date=report_date[:10],
        notice_date=notice.notice_date,
        url=notice.url,
        source_name="HKEXnews",
        title=notice.title,
    )
    entry["_priority"] = _hk_report_notice_priority(combined_text)
    return entry

def _merge_local_hk_report_notices(payload: dict | None, notices: list[StockNotice], symbol: str) -> dict[str, Any]:
    merged_payload = dict(payload or {})
    reports = merged_payload.get("reports")
    reports_by_key: dict[tuple[str, str], dict[str, Any]] = {}
    if isinstance(reports, list):
        for item in reports:
            if not isinstance(item, dict):
                continue
            report_date = str(item.get("report_date") or item.get("notice_date") or "").strip()[:10]
            report_type = AkshareAdapter._normalize_hk_report_type(item.get("report_type"))
            if report_date:
                reports_by_key[(report_date, report_type)] = dict(item)

    for notice in notices:
        entry = _notice_to_hk_report_entry(notice, symbol)
        if not entry:
            continue
        key = (str(entry.get("report_date") or "")[:10], AkshareAdapter._normalize_hk_report_type(entry.get("report_type")))
        current = reports_by_key.get(key)
        if not _prefer_hk_report_notice(entry, current):
            continue
        merged = entry if current is None else AkshareAdapter._merge_hk_financial_report_entry(current, entry)
        merged["_priority"] = entry.get("_priority")
        reports_by_key[key] = merged

    merged_reports = list(reports_by_key.values())
    for item in merged_reports:
        item.pop("_priority", None)
    merged_reports.sort(key=lambda row: (row.get("report_date") or "", row.get("notice_date") or ""), reverse=True)
    merged_payload["reports"] = merged_reports
    if merged_reports:
        merged_payload["source"] = "东方财富港股财报 / HKEXnews"
        merged_payload["message"] = ""
    return merged_payload

def _is_cn_report_notice(notice: StockNotice) -> bool:
    text = f"{notice.title or ''} {notice.notice_type or ''}".replace(" ", "")
    if not text:
        return False
    excluded = (
        "问询函",
        "问询回复",
        "回复函",
        "回复公告",
        "更正公告",
        "更正后",
        "取消公告",
        "延期披露",
        "提示性公告",
        "摘要说明",
        "月报",
    )
    if any(keyword in text for keyword in excluded):
        return False
    return any(
        keyword in text
        for keyword in (
            "年度报告",
            "年报",
            "半年度报告",
            "半年报",
            "中期报告",
            "季度报告",
            "第一季度报告",
            "第三季度报告",
            "一季报",
            "三季报",
            "财务报告",
            "财务报表",
            "审计报告",
        )
    )

def _cn_report_type(title: str) -> str:
    compact = title.replace(" ", "")
    if any(keyword in compact for keyword in ("第一季度报告", "第三季度报告", "季度报告", "一季报", "三季报")):
        return "quarterly_report"
    if any(keyword in compact for keyword in ("半年度报告", "半年报", "中期报告")):
        return "semiannual_report"
    if any(keyword in compact for keyword in ("年度报告", "年报")):
        return "annual_report"
    if "审计报告" in compact:
        return "audit_report"
    return "financial_report"

def _notice_to_financial_report_entry(notice: StockNotice) -> dict[str, Any] | None:
    if notice.market == "HK":
        return _notice_to_hk_report_entry(notice, notice.symbol)
    if notice.market in (MARKET_NEEQ, MARKET_NEEQ_INNOVATION):
        checker = AkshareAdapter._is_neeq_report_notice
        if not checker(
            # The helper only reads title/type, so the ORM object is compatible
            # with the connector dataclass shape for this lightweight check.
            notice  # type: ignore[arg-type]
        ):
            return None
    elif not _is_cn_report_notice(notice):
        return None

    title = str(notice.title or "").strip()
    if not title:
        return None
    return {
        "report_name": title,
        "report_type": _cn_report_type(title),
        "report_date": notice.notice_date,
        "notice_date": notice.notice_date,
        "url": notice.url,
        "source_name": "Eastmoney NEEQ announcements"
        if notice.market in (MARKET_NEEQ, MARKET_NEEQ_INNOVATION)
        else "CNINFO",
        "title": title,
    }

def _merge_local_report_notices(
    payload: dict | None,
    notices: list[StockNotice],
    market: str,
    symbol: str,
) -> dict[str, Any]:
    if market == "HK":
        return _merge_local_hk_report_notices(payload, notices, symbol)

    merged_payload = dict(payload or {})
    reports_by_key: dict[tuple[str, str, str], dict[str, Any]] = {}
    existing_reports = merged_payload.get("reports")
    if isinstance(existing_reports, list):
        for item in existing_reports:
            if not isinstance(item, dict):
                continue
            report_date = str(item.get("report_date") or item.get("notice_date") or "").strip()[:10]
            title = str(item.get("title") or item.get("report_name") or "").strip()
            report_type = str(item.get("report_type") or "financial_report").strip()
            # Drop pseudo indicator rows from the formal report tab.
            if not title or report_type == "financial_indicators":
                continue
            reports_by_key[(report_date, report_type, title)] = dict(item)

    for notice in notices:
        entry = _notice_to_financial_report_entry(notice)
        if not entry:
            continue
        entry_title = str(entry.get("title") or entry.get("report_name") or "").strip()
        title_match = next(
            (
                key
                for key, value in reports_by_key.items()
                if entry_title
                and entry_title
                == str(value.get("title") or value.get("report_name") or "").strip()
            ),
            None,
        )
        if title_match:
            current = reports_by_key[title_match]
            if entry.get("url") and not current.get("url"):
                reports_by_key[title_match] = {**current, **entry}
            elif entry.get("url") and str(current.get("url") or "").find("cninfo.com.cn") < 0:
                reports_by_key[title_match] = {**current, "url": entry["url"], "notice_date": entry.get("notice_date")}
            continue
        key = (
            str(entry.get("report_date") or entry.get("notice_date") or "")[:10],
            str(entry.get("report_type") or "financial_report"),
            str(entry.get("title") or entry.get("report_name") or ""),
        )
        existing = reports_by_key.get(key)
        if not existing or (entry.get("url") and not existing.get("url")):
            reports_by_key[key] = entry if not existing else {**existing, **entry}

    reports = list(reports_by_key.values())
    reports.sort(
        key=lambda row: (
            str(row.get("report_date") or ""),
            str(row.get("notice_date") or ""),
        ),
        reverse=True,
    )
    merged_payload["reports"] = reports[:200]
    if reports:
        if market in (MARKET_NEEQ, MARKET_NEEQ_INNOVATION):
            merged_payload["source"] = "Eastmoney NEEQ announcements"
        else:
            merged_payload["source"] = "CNINFO official disclosures"
        merged_payload["message"] = ""
    else:
        merged_payload["message"] = merged_payload.get("message") or "暂无正式财报披露文件。财务指标请查看“财务”页签。"
    return merged_payload

LIST_DATE_KEYS = (
    "上市日期",
    "上市时间",
    "上市日",
    "A股上市日期",
    "首发上市日期",
    "挂牌日期",
    "LIST_DATE",
    "list_date",
    "listing_date",
    "Listing Date",
    "IPO_DATE",
    "ipo_date",
    "f26",
)

def _normalize_stock_list_date(value: Any) -> str | None:
    if value in (None, ""):
        return None
    if isinstance(value, date):
        return value.isoformat()
    text = str(value).strip()
    if not text or text.lower() in {"none", "nan", "nat", "--", "-"}:
        return None
    text = text.replace("/", "-").replace(".", "-")
    digits = "".join(ch for ch in text if ch.isdigit())
    if len(digits) >= 8:
        candidate = f"{digits[:4]}-{digits[4:6]}-{digits[6:8]}"
        try:
            return date.fromisoformat(candidate).isoformat()
        except ValueError:
            pass
    if len(text) >= 10:
        candidate = text[:10]
        try:
            return date.fromisoformat(candidate).isoformat()
        except ValueError:
            return candidate
    return None

def _extract_stock_list_date(stock: StockSymbol, extended_data: dict[str, dict]) -> str | None:
    payloads: list[dict[str, Any]] = []
    profile = extended_data.get("profile")
    if isinstance(profile, dict) and isinstance(profile.get("fields"), dict):
        payloads.append(profile["fields"])
    if isinstance(stock.ext_json, dict):
        payloads.append(stock.ext_json)
        profile_fields = stock.ext_json.get("f10_profile_fields")
        if isinstance(profile_fields, dict):
            payloads.append(profile_fields)
    if isinstance(stock.raw_payload, dict):
        payloads.append(stock.raw_payload)

    lowered_keys = {key.lower() for key in LIST_DATE_KEYS}
    for payload in payloads:
        for key in LIST_DATE_KEYS:
            parsed = _normalize_stock_list_date(payload.get(key))
            if parsed:
                return parsed
        for key, value in payload.items():
            key_text = str(key).strip()
            normalized_key = key_text.lower()
            looks_like_list_date = (
                normalized_key in lowered_keys
                or ("上市" in key_text and ("日期" in key_text or "时间" in key_text or "日" in key_text))
                or ("挂牌" in key_text and ("日期" in key_text or "时间" in key_text or "日" in key_text))
            )
            if looks_like_list_date:
                parsed = _normalize_stock_list_date(value)
                if parsed:
                    return parsed
    return None

def _hydrate_symbol_from_f10(stock: StockSymbol, extended_data: dict[str, dict]) -> bool:
    updated = False
    list_date = _extract_stock_list_date(stock, extended_data)
    if list_date and stock.list_date != list_date:
        stock.list_date = list_date
        updated = True
    profile = extended_data.get("profile")
    if isinstance(profile, dict) and isinstance(profile.get("fields"), dict):
        ext_json = dict(stock.ext_json or {})
        ext_json["f10_profile_fields"] = profile["fields"]
        ext_json["f10_profile_source"] = profile.get("source")
        stock.ext_json = ext_json
        updated = True
    return updated

def _fetch_and_cache_f10_extended_data(
    db: Session,
    source: DataSource,
    market: str,
    symbol: str,
    persistence_stats: dict[str, Any] | None = None,
) -> dict[str, dict]:
    extended_data = get_adapter(source.adapter_type).fetch_extended_data(market, symbol)
    research = extended_data.get("research_sections")
    if isinstance(research, dict):
        research_counts = persist_research_sections(db, market, symbol, research)
        db.commit()
        if persistence_stats is not None:
            persistence_stats["research_records"] = research_counts
        persisted = load_research_sections(db, market, symbol)
        research.update({key: rows for key, rows in persisted.items() if rows})
    service = StockOnDemandService(db)
    updated_cache_sections = 0
    for section in F10_CACHE_SECTIONS:
        payload = extended_data.get(section)
        if isinstance(payload, dict):
            updated_cache_sections += service.upsert_f10_cache(source, market, symbol, section, payload)
    if persistence_stats is not None:
        persistence_stats["updated_cache_sections"] = updated_cache_sections
    return {**_empty_extended_data("本地暂无缓存，可点击“拉取最新”获取。"), **extended_data}

def _ensure_neeq_f10_from_core(
    db: Session,
    source: DataSource | None,
    market: str,
    symbol: str,
    quote: StockRealtimeQuote | None,
    financials: list[StockFinancialReport],
    notices: list[StockNotice],
    extended_data: dict[str, dict],
) -> dict[str, dict]:
    """Fill NEEQ F10 sections from core rows when optional APIs are rate-limited."""
    if market not in (MARKET_NEEQ, MARKET_NEEQ_INNOVATION):
        return extended_data

    merged = dict(extended_data)
    service = StockOnDemandService(db)

    if quote:
        raw_payload = quote.raw_payload if isinstance(quote.raw_payload, dict) else {}
        raw_quote = raw_payload.get("quote") if isinstance(raw_payload.get("quote"), dict) else {}
        derived = raw_payload.get("derived") if isinstance(raw_payload.get("derived"), dict) else {}
        fields = {
            "symbol": symbol,
            "market": market,
            "current_price": quote.current_price,
            "previous_close": quote.previous_close_price,
            "open_price": quote.open_price,
            "high_price": quote.high_price,
            "low_price": quote.low_price,
            "volume": quote.volume,
            "amount": quote.amount,
            "change_amount": quote.change_amount,
            "change_pct": quote.change_pct,
            "turnover_rate": quote.turnover_rate,
            "quote_date": quote.quote_time,
            "total_market_value": raw_quote.get("MarketValue") or derived.get("total_market_cap_yi"),
            "float_market_value": raw_quote.get("FlowCapitalValue") or derived.get("float_market_cap_yi"),
            "pe_ratio": raw_quote.get("PERation") or derived.get("pe_ratio"),
            "pb_ratio": raw_quote.get("PB") or derived.get("pb_ratio"),
            "latest_price_source": raw_payload.get("latest_price_source"),
            "no_trade_today": raw_payload.get("no_trade_today"),
        }
        fields = {key: value for key, value in fields.items() if value not in (None, "")}
        if fields:
            profile_payload = {
                "source": "Eastmoney NEEQ quote fallback",
                "fields": fields,
                "message": (
                    "当日无成交时，最新价使用上一交易日收盘价。"
                    if raw_payload.get("no_trade_today")
                    else ""
                ),
            }
            if source:
                service.upsert_f10_cache(source, market, symbol, "profile", profile_payload)
            merged["profile"] = _merge_f10_payload(
                "profile",
                merged.get("profile") if isinstance(merged.get("profile"), dict) else None,
                profile_payload,
            )
            merged["profile"]["message"] = profile_payload.get("message") or ""

    if financials:
        metric_fields = (
            ("revenue", "INCOME"),
            ("net_profit", "PROFIT"),
            ("eps", "EPSJB"),
            ("book_value_per_share", "BPS"),
            ("gross_margin_pct", "XSMLL"),
            ("net_margin_pct", "XSJLL"),
            ("debt_asset_ratio_pct", "ZCFZL"),
            ("roe_pct", "ROEKCJQ"),
            ("revenue_yoy_pct", "YSTZ"),
            ("profit_yoy_pct", "SJLTZ"),
            ("pe_ratio", "PELYR"),
            ("pb_ratio", "PBMRQ"),
        )
        latest = max(
            financials,
            key=lambda item: (
                sum(
                    1
                    for _, field in metric_fields
                    if isinstance(item.data_json, dict) and item.data_json.get(field) not in (None, "")
                ),
                str(item.report_period or ""),
            ),
        )
        raw = latest.data_json if isinstance(latest.data_json, dict) else {}
        rows = [
            {"metric": metric, "data": {latest.report_period: raw[field]}}
            for metric, field in metric_fields
            if raw.get(field) not in (None, "")
        ]
        if rows:
            financial_payload = {
                "source": "Eastmoney NEEQ financial indicators",
                "periods": [latest.report_period],
                "rows": rows,
                "latest": {
                    "report_date": latest.report_period,
                    "report_name": "NEEQ financial indicators",
                },
                "message": "",
            }
            if source:
                service.upsert_f10_cache(source, market, symbol, "financial_summary", financial_payload)
            existing_summary = merged.get("financial_summary") if isinstance(merged.get("financial_summary"), dict) else None
            existing_rows = existing_summary.get("rows") if isinstance(existing_summary, dict) else []
            if not isinstance(existing_rows, list) or len(rows) > len(existing_rows):
                # A quote-indicator fallback can have today's date but only
                # two valuation fields. Prefer a richer actual financial
                # report even when its disclosure date is earlier.
                merged["financial_summary"] = financial_payload
            else:
                merged["financial_summary"] = _merge_f10_payload(
                    "financial_summary",
                    existing_summary,
                    financial_payload,
                )
            merged["financial_summary"]["message"] = ""

    reports_payload = _merge_local_report_notices(
        merged.get("published_reports"),
        notices,
        market,
        symbol,
    )
    if "reports" in reports_payload:
        if source:
            service.upsert_f10_cache(source, market, symbol, "published_reports", reports_payload)
        merged["published_reports"] = _merge_f10_payload(
            "published_reports",
            merged.get("published_reports") if isinstance(merged.get("published_reports"), dict) else None,
            reports_payload,
        )
        merged["published_reports"]["message"] = ""

    return merged
