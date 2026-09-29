"""Atomic F10 cache, merge and normalization policies.

This module deliberately contains no FastAPI types.  It can be reused by an
HTTP request, a background job, or a command-line import without depending on
the transport layer.
"""

from __future__ import annotations

from datetime import date
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
    # The typed stock-classification projection is the authoritative source
    # for the concept panel when an older cache only contained index labels.
    # Keep industry and theme observations, but never promote index
    # membership into a concept.  This also carries the versioned glossary
    # fields through to the concept-detail dialog and hover text.
    if not concepts:
        groups = profile.get("classification_groups")
        if isinstance(groups, list):
            for group in groups:
                if not isinstance(group, dict) or str(group.get("key") or "").lower() not in {"industry", "theme"}:
                    continue
                dimension = "INDUSTRY" if str(group.get("key") or "").lower() == "industry" else "THEME"
                for item in group.get("items") or []:
                    if not isinstance(item, dict):
                        continue
                    name = item.get("name") or item.get("label")
                    if name and not _looks_like_index(name, item.get("code")):
                        concepts.append({
                            "name": str(name),
                            "label": str(item.get("label") or name),
                            "code": item.get("code"),
                            "dimension": dimension,
                            "definition": item.get("definition"),
                            "criteria": item.get("criteria"),
                            "source": item.get("source"),
                            "source_name": item.get("source_name"),
                            "definition_version": item.get("definition_version"),
                        })
    if not concepts:
        # Index membership is a separate dimension and must not be presented
        # as an investment concept.  Legacy caches may have no concept field;
        # leave that section empty rather than inventing a theme from indexes.
        raw_concepts = fields.get("所属概念") or fields.get("概念")
        if raw_concepts:
            concepts = [
                {"name": item.strip(), "definition": "来源披露的行业/概念标签，需结合原文核验。"}
                for item in str(raw_concepts).replace("，", ",").split(",")
                if item.strip()
            ]
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
    # ``latest_reports`` used to be populated with the complete report list.
    # Normalize old snapshots as a read-model projection so the latest panel
    # is a strict subset of the full report panel and never duplicates it.
    reports = research.get("reports")
    if isinstance(reports, list):
        research["latest_reports"] = reports[:10]
    elif isinstance(research.get("latest_reports"), list):
        research["latest_reports"] = research["latest_reports"][:10]
    else:
        research["latest_reports"] = []
    # Rebuild optional research projections for legacy cache rows.  The raw
    # report list is already isolated by the cache's market/symbol key; the
    # adapter helpers only derive facts from those rows and never call a
    # market-wide endpoint here.
    if isinstance(reports, list) and reports:
        report_symbol = next(
            (
                str(item.get("股票代码") or item.get("证券代码") or item.get("symbol") or "")
                for item in reports
                if isinstance(item, dict)
                and (item.get("股票代码") or item.get("证券代码") or item.get("symbol"))
            ),
            "",
        )
        if not isinstance(research.get("earnings_forecast"), list) or not research.get("earnings_forecast"):
            research["earnings_forecast"] = AkshareAdapter._derive_research_earnings_forecast(reports, report_symbol)
        if not isinstance(research.get("institution_forecast"), list) or not research.get("institution_forecast"):
            research["institution_forecast"] = AkshareAdapter._derive_institution_forecast(reports, report_symbol)
    # Rebuild the fixed six-part research layout for both new and legacy
    # caches. Raw ``qa``/forecast/report arrays remain untouched above.
    research["sections"] = [
        _display_section("industry_concepts", "行业概念", rows=concepts, source=profile.get("source"), as_of=payload_as_of(profile)),
        _display_section("qa", "问董秘", rows=research.get("qa"), source=research.get("source"), as_of=payload_as_of(research), message="当前未接入问董秘公开接口"),
        _display_section("earnings_forecast", "盈利预测", rows=research.get("earnings_forecast"), source=research.get("source"), as_of=payload_as_of(research)),
        _display_section("institution_forecast", "机构预测（评级统计）", rows=research.get("institution_forecast"), source=research.get("source"), as_of=payload_as_of(research)),
        _display_section("latest_reports", "最新研报", rows=research.get("latest_reports"), source=research.get("source"), as_of=payload_as_of(research)),
        _display_section("reports", "研报", rows=research.get("reports"), source=research.get("source"), as_of=payload_as_of(research)),
    ]
    return extended_data

def _load_f10_extended_data(db: Session, market: str, symbol: str) -> dict[str, dict]:
    cached = StockOnDemandService(db).load_f10_cache(market, symbol)
    extended_data = _empty_extended_data("本地暂无缓存，可点击“拉取最新”获取。")
    for section in F10_CACHE_SECTIONS:
        payload = cached.get(section)
        if isinstance(payload, dict):
            extended_data[section] = payload
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
    if market == "HK":
        if _hk_fund_flow_is_stale(extended_data.get("fund_flow")):
            return True
        if _hk_published_reports_need_refresh(extended_data.get("published_reports")):
            return True
    return False

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
) -> dict[str, dict]:
    extended_data = get_adapter(source.adapter_type).fetch_extended_data(market, symbol)
    service = StockOnDemandService(db)
    for section in F10_CACHE_SECTIONS:
        payload = extended_data.get(section)
        if isinstance(payload, dict):
            service.upsert_f10_cache(source, market, symbol, section, payload)
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
