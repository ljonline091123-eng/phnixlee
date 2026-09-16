"""Atomic F10 cache, merge and normalization policies.

This module deliberately contains no FastAPI types.  It can be reused by an
HTTP request, a background job, or a command-line import without depending on
the transport layer.
"""

from __future__ import annotations

from datetime import date
from typing import Any

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
from app.services.stock_on_demand import StockOnDemandService, _merge_f10_payload


F10_EXTENDED_SECTIONS = (
    "published_reports",
    "profile",
    "holders",
    "fund_flow",
    "financial_summary",
    "financial_statements",
    "business_composition",
)

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
        "published_reports": {
            "source": "暂无",
            "reports": [],
            "message": message,
        },
    }

def _load_f10_extended_data(db: Session, market: str, symbol: str) -> dict[str, dict]:
    cached = StockOnDemandService(db).load_f10_cache(market, symbol)
    extended_data = _empty_extended_data("本地暂无缓存，可点击“拉取最新”获取。")
    for section in F10_EXTENDED_SECTIONS:
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
        return bool(payload.get("major")) or bool(payload.get("circulating")) or bool(payload.get("official_links"))
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
        return bool(payload.get("sections")) or str(payload.get("source") or "") not in {"", "暂无"}
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
    for section in F10_EXTENDED_SECTIONS:
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
    for section in F10_EXTENDED_SECTIONS:
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
