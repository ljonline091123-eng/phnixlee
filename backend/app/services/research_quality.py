"""Read-only quality assessment for one security's research data.

The research page combines observations from several public providers.  A
non-empty list is not evidence that a provider returned the complete concept,
institution or report universe, so this module deliberately separates:

* bounded contract coverage, such as the required metric/year matrix; and
* observed-source coverage, where no authoritative public universe exists.

The assessment never fills source data or promotes a missing value to a fact.
It is a derived read model and therefore requires no schema migration.
"""

from __future__ import annotations

from datetime import date, datetime
import re
from typing import Any, Iterable


EXPECTED_EARNINGS_METRICS: tuple[tuple[str, str], ...] = (
    ("TOTAL_REVENUE", "营业总收入"),
    ("REVENUE_YOY", "营业总收入同比"),
    ("NET_PROFIT", "归母净利润"),
    ("NET_PROFIT_YOY", "归母净利润同比"),
    ("EPS", "每股收益"),
    ("FORWARD_PE", "预测市盈率"),
    ("DIVIDEND_YIELD", "预测股息率"),
)

EXPECTED_RATING_PERIODS: tuple[str, ...] = (
    "1个月内",
    "2个月内",
    "3个月内",
    "6个月内",
    "1年内",
)

_SOURCE_FIELDS = ("source_code", "source_name", "source", "数据来源")
_DATE_FIELDS = ("report_date", "报告日期", "日期", "最新报告日期", "source_updated_at")
_RATING_FIELDS = ("rating", "评级", "东财评级")


def _rows(value: Any) -> list[dict[str, Any]]:
    if isinstance(value, dict):
        value = value.get("rows")
    return [row for row in value if isinstance(row, dict)] if isinstance(value, list) else []


def _present(value: Any) -> bool:
    if value is None or value == "":
        return False
    if isinstance(value, float) and value != value:  # NaN
        return False
    if isinstance(value, dict):
        return any(_present(item) for item in value.values())
    if isinstance(value, (list, tuple, set)):
        return any(_present(item) for item in value)
    return True


def _first(row: dict[str, Any], fields: Iterable[str]) -> Any:
    return next((row.get(field) for field in fields if _present(row.get(field))), None)


def _ratio(numerator: int, denominator: int) -> float | None:
    return round(numerator / denominator, 6) if denominator else None


def _source_values(rows: list[dict[str, Any]], *declared: Any) -> list[str]:
    values: set[str] = set()
    for value in declared:
        if isinstance(value, (list, tuple, set)):
            candidates = value
        else:
            candidates = (value,)
        for candidate in candidates:
            text = str(candidate or "").strip()
            if text and text not in {"暂无", "None"}:
                values.add(text)
    for row in rows:
        value = _first(row, _SOURCE_FIELDS)
        text = str(value or "").strip()
        if text and text not in {"暂无", "None"}:
            values.add(text)
    return sorted(values)


def _has_provenance(row: dict[str, Any]) -> bool:
    return _first(row, _SOURCE_FIELDS) is not None or _first(
        row, ("source_url", "detail_url", "pdf_url", "报告PDF链接")
    ) is not None


def _parse_date(value: Any) -> date | None:
    if isinstance(value, datetime):
        return value.date()
    if isinstance(value, date):
        return value
    text = str(value or "").strip()
    if not text:
        return None
    text = text.replace("年", "-").replace("月", "-").replace("日", "")
    text = text.replace("/", "-").replace(".", "-")
    if re.fullmatch(r"\d{8}", text):
        text = f"{text[:4]}-{text[4:6]}-{text[6:]}"
    try:
        return datetime.strptime(text[:10], "%Y-%m-%d").date()
    except ValueError:
        return None


def _reason(
    code: str,
    message: str,
    action: str,
    *,
    severity: str = "WARNING",
    missing_items: list[str] | None = None,
) -> dict[str, Any]:
    result: dict[str, Any] = {
        "code": code,
        "severity": severity,
        "message": message,
        "action": action,
    }
    if missing_items:
        result["missing_items"] = missing_items
    return result


def _canonical_metric(value: Any) -> str | None:
    text = str(value or "").strip().upper().replace("-", "_").replace(" ", "_")
    aliases = {
        "TOTAL_OPERATE_INCOME": "TOTAL_REVENUE",
        "OPERATING_REVENUE": "TOTAL_REVENUE",
        "REVENUE": "TOTAL_REVENUE",
        "TOTAL_REVENUE": "TOTAL_REVENUE",
        "REVENUE_YOY": "REVENUE_YOY",
        "TOTAL_REVENUE_YOY": "REVENUE_YOY",
        "NET_PROFIT": "NET_PROFIT",
        "PARENT_NET_PROFIT": "NET_PROFIT",
        "NET_PROFIT_YOY": "NET_PROFIT_YOY",
        "PARENT_NET_PROFIT_YOY": "NET_PROFIT_YOY",
        "EPS": "EPS",
        "FORWARD_PE": "FORWARD_PE",
        "PE": "FORWARD_PE",
        "PREDICTED_PE": "FORWARD_PE",
        "DIVIDEND_YIELD": "DIVIDEND_YIELD",
        "PREDICTED_DIVIDEND_YIELD": "DIVIDEND_YIELD",
    }
    if text in aliases:
        return aliases[text]
    original = str(value or "").strip()
    if re.search(r"营业总收入|营业收入", original) and re.search(r"同比|YOY", original, re.I):
        return "REVENUE_YOY"
    if re.search(r"归母净利润|净利润", original) and re.search(r"同比|YOY", original, re.I):
        return "NET_PROFIT_YOY"
    if re.search(r"营业总收入|营业收入", original):
        return "TOTAL_REVENUE"
    if re.search(r"归母净利润|净利润", original):
        return "NET_PROFIT"
    if re.search(r"每股收益|EPS", original, re.I):
        return "EPS"
    if re.search(r"股息率", original):
        return "DIVIDEND_YIELD"
    if re.search(r"预测市盈率|市盈率|FORWARD.?PE|\bPE\b", original, re.I):
        return "FORWARD_PE"
    return None


def _metric_pairs(earnings: dict[str, Any]) -> set[tuple[str, str]]:
    pairs: set[tuple[str, str]] = set()
    for metric in earnings.get("metrics") or []:
        if not isinstance(metric, dict):
            continue
        code = _canonical_metric(metric.get("metric_code") or metric.get("metric_name"))
        values = metric.get("values")
        if not code or not isinstance(values, dict):
            continue
        for year, value in values.items():
            if re.fullmatch(r"20\d{2}", str(year)) and _present(value):
                pairs.add((code, str(year)))

    direct_fields = {
        "TOTAL_REVENUE": ("total_revenue", "预测营业总收入", "营业总收入"),
        "REVENUE_YOY": ("revenue_yoy", "营业总收入同比", "营业收入同比"),
        "NET_PROFIT": ("net_profit", "预测净利润", "归母净利润"),
        "NET_PROFIT_YOY": ("net_profit_yoy", "归母净利润同比", "净利润同比"),
        "EPS": ("eps", "预测每股收益", "每股收益"),
        "FORWARD_PE": ("forward_pe", "pe", "预测市盈率"),
        "DIVIDEND_YIELD": ("dividend_yield", "预测股息率", "股息率"),
    }
    for row in _rows(earnings):
        year = str(row.get("forecast_year") or row.get("预测年度") or "")[:4]
        code = _canonical_metric(
            row.get("metric_code") or row.get("metric") or row.get("预测指标")
        )
        if re.fullmatch(r"20\d{2}", year) and code and _present(
            _first(row, ("mean", "均值", "value", "预测值", f"{year}E"))
        ):
            pairs.add((code, year))
        if not re.fullmatch(r"20\d{2}", year):
            continue
        for direct_code, fields in direct_fields.items():
            value = _first(row, fields)
            if isinstance(value, dict):
                value = value.get(year)
            if _present(value):
                pairs.add((direct_code, year))
    return pairs


def _audit_concepts(concepts: list[dict[str, Any]]) -> dict[str, Any]:
    reasons: list[dict[str, Any]] = []
    dimensions = [str(row.get("dimension") or row.get("classification_dimension") or "THEME").upper() for row in concepts]
    industry_count = sum(value == "INDUSTRY" for value in dimensions)
    theme_count = sum(value in {"THEME", "BOARD"} for value in dimensions)
    provenance_count = sum(_has_provenance(row) for row in concepts)
    definition_count = sum(_present(row.get("definition")) for row in concepts)
    if not concepts:
        reasons.append(_reason(
            "CONCEPT_ROWS_UNAVAILABLE",
            "当前来源没有返回该证券的行业或概念标签。",
            "刷新分类主数据，并接入至少一个可追溯的行业来源和概念来源。",
            severity="ERROR",
        ))
        status = "UNAVAILABLE"
    else:
        if not industry_count:
            reasons.append(_reason(
                "INDUSTRY_CLASSIFICATION_MISSING",
                "已取得概念数据，但缺少独立的行业归属。",
                "从带分类版本的行业主数据补齐行业代码、层级和有效期。",
            ))
        if not theme_count:
            reasons.append(_reason(
                "THEME_CLASSIFICATION_MISSING",
                "已取得行业数据，但当前来源没有返回投资主题或概念板块。",
                "增加第二个概念来源并按来源、代码和观察日期增量合并。",
            ))
        if provenance_count < len(concepts):
            reasons.append(_reason(
                "CONCEPT_PROVENANCE_PARTIAL",
                f"{len(concepts) - provenance_count} 条标签缺少来源名称、代码或原始链接。",
                "保留标签来源、来源代码、观察日期和原始页面，避免无法复核。",
            ))
        if definition_count < len(concepts):
            reasons.append(_reason(
                "CONCEPT_DEFINITION_PARTIAL",
                f"{len(concepts) - definition_count} 条标签尚无专属释义。",
                "在分类主数据中补充标签级定义和定义版本。",
            ))
        status = "PARTIAL" if reasons else "AVAILABLE"
    return {
        "key": "concepts",
        "label": "行业概念",
        "status": status,
        "coverage_scope": "OBSERVED_SOURCE_ROWS",
        "completeness_claim": "UNBOUNDED",
        "record_count": len(concepts),
        "industry_count": industry_count,
        "theme_count": theme_count,
        "provenance_coverage_ratio": _ratio(provenance_count, len(concepts)),
        "definition_coverage_ratio": _ratio(definition_count, len(concepts)),
        "sources": _source_values(concepts),
        "missing_reasons": reasons,
        "coverage_note": "公开来源没有统一的概念全集；未配置外部基准时，只能证明已观察到的标签，不能据此断言不存在漏项。",
    }


def _audit_earnings(research: dict[str, Any]) -> dict[str, Any]:
    earnings = research.get("earnings_forecast")
    earnings = earnings if isinstance(earnings, dict) else {"rows": earnings or []}
    rows = _rows(earnings)
    years = [
        str(year) for year in earnings.get("forecast_years") or []
        if re.fullmatch(r"20\d{2}", str(year))
    ]
    if not years:
        years = sorted({
            str(row.get("forecast_year") or row.get("预测年度") or "")[:4]
            for row in rows
            if re.fullmatch(r"20\d{2}", str(row.get("forecast_year") or row.get("预测年度") or "")[:4])
        })
    pairs = _metric_pairs(earnings)
    available_codes = sorted({code for code, _ in pairs})
    expected_labels = dict(EXPECTED_EARNINGS_METRICS)
    missing_codes = [code for code, _ in EXPECTED_EARNINGS_METRICS if code not in available_codes]
    missing_by_year = {
        year: [
            code for code, _ in EXPECTED_EARNINGS_METRICS
            if (code, year) not in pairs
        ]
        for year in years
    }
    expected_pairs = len(EXPECTED_EARNINGS_METRICS) * len(years)
    covered_pairs = sum(
        (code, year) in pairs
        for code, _ in EXPECTED_EARNINGS_METRICS
        for year in years
    )
    reasons: list[dict[str, Any]] = []
    if not rows and not pairs:
        reasons.append(_reason(
            "EARNINGS_FORECAST_UNAVAILABLE",
            "当前来源没有返回盈利预测。",
            "重试公开源，并将无数据、限流、接口异常和需授权分别记录。",
            severity="ERROR",
        ))
        status = "UNAVAILABLE"
    else:
        if not years:
            reasons.append(_reason(
                "FORECAST_YEAR_MISSING",
                "盈利预测记录缺少可识别的预测年度。",
                "在入库前标准化预测年度，并保留原始年度字段。",
                severity="ERROR",
            ))
        if missing_codes:
            missing_labels = [expected_labels[code] for code in missing_codes]
            reasons.append(_reason(
                "EARNINGS_METRICS_MISSING",
                "当前来源未覆盖完整的盈利预测指标契约。",
                "按来源分别补采缺失指标；不同来源或统计时点的值不得相互覆盖。",
                missing_items=missing_labels,
            ))
        incomplete_years = [year for year, codes in missing_by_year.items() if codes]
        if incomplete_years:
            reasons.append(_reason(
                "EARNINGS_YEAR_MATRIX_INCOMPLETE",
                "部分预测年度缺少一个或多个必需指标。",
                "按年度执行指标矩阵完整性校验，缺失值保持为空并显示来源原因。",
                missing_items=incomplete_years,
            ))
        provenance_count = sum(_has_provenance(row) for row in rows)
        if rows and provenance_count < len(rows):
            reasons.append(_reason(
                "EARNINGS_PROVENANCE_PARTIAL",
                f"{len(rows) - provenance_count} 条预测记录缺少可复核来源。",
                "保存 source_code、source_url、source_updated_at 和原始字段。",
            ))
        status = "COMPLETE" if expected_pairs and covered_pairs == expected_pairs and not reasons else "PARTIAL"
    return {
        "key": "earnings_metrics",
        "label": "盈利预测指标",
        "status": status,
        "coverage_scope": "EXPECTED_METRIC_YEAR_MATRIX",
        "completeness_claim": "BOUNDED_CONTRACT",
        "record_count": len(rows),
        "forecast_years": years,
        "required_metrics": [
            {"metric_code": code, "metric_name": label}
            for code, label in EXPECTED_EARNINGS_METRICS
        ],
        "available_metrics": [
            {"metric_code": code, "metric_name": expected_labels.get(code, code)}
            for code in available_codes
        ],
        "missing_metrics": [
            {"metric_code": code, "metric_name": expected_labels[code]}
            for code in missing_codes
        ],
        "missing_by_year": missing_by_year,
        "covered_metric_year_pairs": covered_pairs,
        "expected_metric_year_pairs": expected_pairs,
        "coverage_ratio": _ratio(covered_pairs, expected_pairs),
        "sources": _source_values(
            rows,
            earnings.get("source"),
            research.get("earnings_forecast_source"),
        ),
        "missing_reasons": reasons,
    }


def _audit_institutions(research: dict[str, Any]) -> dict[str, Any]:
    contract = research.get("institution_forecast")
    contract = contract if isinstance(contract, dict) else {"rows": contract or []}
    rows = _rows(contract)
    years = [
        str(year) for year in contract.get("forecast_years") or []
        if re.fullmatch(r"20\d{2}", str(year))
    ]
    expected_values = len(rows) * len(years) * 2
    covered_values = 0
    rows_missing_eps = 0
    rows_missing_profit = 0
    rows_missing_date = 0
    rows_missing_context = 0
    provenance_count = 0
    institutions: set[str] = set()
    for row in rows:
        name = str(row.get("institution") or row.get("机构") or row.get("机构名称") or "").strip()
        if name:
            institutions.add(name)
        eps = row.get("eps") if isinstance(row.get("eps"), dict) else {}
        profit = row.get("net_profit") if isinstance(row.get("net_profit"), dict) else {}
        forecast = row.get("forecast") if isinstance(row.get("forecast"), dict) else {}
        if not eps and isinstance(forecast.get("eps"), dict):
            eps = forecast["eps"]
        if not profit and isinstance(forecast.get("net_profit"), dict):
            profit = forecast["net_profit"]
        eps_covered = sum(_present(eps.get(year)) for year in years)
        profit_covered = sum(_present(profit.get(year)) for year in years)
        covered_values += eps_covered + profit_covered
        rows_missing_eps += int(bool(years) and eps_covered < len(years))
        rows_missing_profit += int(bool(years) and profit_covered < len(years))
        rows_missing_date += int(_first(row, _DATE_FIELDS) is None)
        rows_missing_context += int(_first(
            row,
            ("report_period", "报告期", "report_type", "报告类型", "report_external_id", "报告名称", "title"),
        ) is None)
        provenance_count += int(_has_provenance(row))

    reasons: list[dict[str, Any]] = []
    if not rows:
        reasons.append(_reason(
            "INSTITUTION_FORECAST_UNAVAILABLE",
            "当前来源没有返回机构级盈利预测明细。",
            "重试机构预测公开源；如目标机构仅在授权终端提供，应明确标记需授权。",
            severity="ERROR",
        ))
        status = "UNAVAILABLE"
    else:
        if not years:
            reasons.append(_reason(
                "INSTITUTION_FORECAST_YEAR_MISSING",
                "机构预测缺少统一预测年度。",
                "从字段名抽取年度并在入库时保存标准 forecast_year。",
                severity="ERROR",
            ))
        if rows_missing_eps:
            reasons.append(_reason(
                "INSTITUTION_EPS_PARTIAL",
                f"{rows_missing_eps} 条机构记录没有覆盖全部预测年度的每股收益。",
                "保留空值并补采同一机构、报告日期对应的 EPS 原始字段。",
            ))
        if rows_missing_profit:
            reasons.append(_reason(
                "INSTITUTION_NET_PROFIT_PARTIAL",
                f"{rows_missing_profit} 条机构记录没有覆盖全部预测年度的净利润。",
                "净利润与 EPS 分字段治理，禁止用 EPS 填充净利润。",
            ))
        if rows_missing_date:
            reasons.append(_reason(
                "INSTITUTION_REPORT_DATE_PARTIAL",
                f"{rows_missing_date} 条机构预测缺少报告日期。",
                "补齐报告日期和来源观察时间，以支持按时间倒序和历史复核。",
            ))
        if rows_missing_context:
            reasons.append(_reason(
                "INSTITUTION_REPORT_CONTEXT_PARTIAL",
                f"{rows_missing_context} 条机构预测未关联报告期、研报标题或研报标识。",
                "将预测记录关联到研报证据，并显式记录年报、中报等报告期间。",
            ))
        if provenance_count < len(rows):
            reasons.append(_reason(
                "INSTITUTION_PROVENANCE_PARTIAL",
                f"{len(rows) - provenance_count} 条机构预测缺少可复核来源。",
                "保存 source_code、来源链接、报告标识和采集时间。",
            ))
        status = "PARTIAL" if reasons or (expected_values and covered_values < expected_values) else "AVAILABLE"
    return {
        "key": "institution_forecasts",
        "label": "机构预测",
        "status": status,
        "coverage_scope": "OBSERVED_INSTITUTION_ROWS",
        "completeness_claim": "UNBOUNDED",
        "record_count": len(rows),
        "institution_count": len(institutions),
        "institutions": sorted(institutions),
        "forecast_years": years,
        "covered_forecast_values": covered_values,
        "expected_forecast_values": expected_values,
        "field_coverage_ratio": _ratio(covered_values, expected_values),
        "report_date_coverage_ratio": _ratio(len(rows) - rows_missing_date, len(rows)),
        "report_context_coverage_ratio": _ratio(len(rows) - rows_missing_context, len(rows)),
        "provenance_coverage_ratio": _ratio(provenance_count, len(rows)),
        "sources": _source_values(
            rows,
            contract.get("source"),
            research.get("institution_forecast_source"),
        ),
        "missing_reasons": reasons,
        "coverage_note": "当前机构清单只代表已接入来源返回的记录；未配置目标机构全集或授权源时，不能断言机构覆盖完整。",
    }


def _audit_ratings(research: dict[str, Any], reports: list[dict[str, Any]]) -> dict[str, Any]:
    institution = research.get("institution_forecast")
    institution = institution if isinstance(institution, dict) else {}
    buckets = [row for row in institution.get("rating_statistics") or [] if isinstance(row, dict)]
    bucket_periods = {str(row.get("period") or "") for row in buckets}
    provider = research.get("provider_rating_statistics")
    provider = provider if isinstance(provider, dict) else {}
    rated_count = sum(_first(row, _RATING_FIELDS) is not None for row in reports)
    expected_period_count = len(EXPECTED_RATING_PERIODS)
    available_period_count = sum(period in bucket_periods for period in EXPECTED_RATING_PERIODS)
    reasons: list[dict[str, Any]] = []
    if not provider and not rated_count:
        reasons.append(_reason(
            "RATING_DATA_UNAVAILABLE",
            "既没有来源原生评级快照，也没有可聚合的带评级研报。",
            "接入来源原生评级统计；普通研报聚合只能作为回退口径。",
            severity="ERROR",
        ))
        status = "UNAVAILABLE"
    else:
        if not provider:
            reasons.append(_reason(
                "PROVIDER_RATING_SNAPSHOT_MISSING",
                "当前评级统计由研报记录回退聚合，缺少来源原生统计快照。",
                "优先保存提供方的原生统计、统计区间、参考日期和来源链接。",
            ))
        elif not _present(provider.get("reference_date") or provider.get("as_of") or provider.get("source_updated_at")):
            reasons.append(_reason(
                "RATING_OBSERVATION_DATE_MISSING",
                "来源原生评级快照没有明确观察日期。",
                "采集时保存提供方参考日期；若来源不披露，则保存抓取时间并标注口径。",
            ))
        missing_periods = [period for period in EXPECTED_RATING_PERIODS if period not in bucket_periods]
        if missing_periods:
            reasons.append(_reason(
                "RATING_WINDOWS_INCOMPLETE",
                "系统回退统计未覆盖全部标准时间窗口。",
                "按同一参考日期生成标准时间窗口，且不要与来源原生六个月口径混算。",
                missing_items=missing_periods,
            ))
        if reports and rated_count < len(reports):
            reasons.append(_reason(
                "REPORT_RATING_PARTIAL",
                f"{len(reports) - rated_count} 篇研报没有明确评级。",
                "缺失评级保持为空，不从标题或盈利方向推断评级。",
            ))
        status = "PARTIAL" if reasons else "AVAILABLE"
    provider_sources = _source_values([], provider.get("source_code"), provider.get("source_name"), provider.get("source_url"))
    return {
        "key": "ratings",
        "label": "评级统计",
        "status": status,
        "coverage_scope": "PROVIDER_SNAPSHOT_AND_DERIVED_WINDOWS",
        "completeness_claim": "SOURCE_METHOD_BOUND",
        "provider_snapshot_available": bool(provider),
        "provider_reference_period": provider.get("reference_period"),
        "provider_reference_date": provider.get("reference_date") or provider.get("as_of"),
        "derived_reference_date": institution.get("rating_reference_date"),
        "derived_reference_basis": institution.get("rating_reference_basis"),
        "available_periods": [period for period in EXPECTED_RATING_PERIODS if period in bucket_periods],
        "period_coverage_ratio": _ratio(available_period_count, expected_period_count),
        "rated_report_count": rated_count,
        "report_count": len(reports),
        "rated_report_coverage_ratio": _ratio(rated_count, len(reports)),
        "sources": provider_sources or _source_values(reports),
        "missing_reasons": reasons,
        "coverage_note": "来源原生快照与本地研报聚合的机构范围、评级映射和参考日期可能不同，两种口径分别展示，不互相覆盖。",
    }


def _dedupe_reports(rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    result: list[dict[str, Any]] = []
    seen: set[tuple[str, ...]] = set()
    for row in rows:
        source = str(row.get("source_code") or row.get("source_name") or row.get("source") or "").strip()
        external_id = str(row.get("external_id") or row.get("report_id") or "").strip()
        if source and external_id:
            key = (source.casefold(), external_id.casefold())
        else:
            key = tuple(str(row.get(field) or "").strip().casefold() for field in (
                "report_date", "institution", "title",
            ))
        if key in seen:
            continue
        seen.add(key)
        result.append(row)
    return result


def _audit_reports(research: dict[str, Any], *, as_of: date) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    reports = _dedupe_reports([
        *_rows(research.get("latest_reports")),
        *_rows(research.get("reports")),
    ])
    dated = [parsed for row in reports if (parsed := _parse_date(_first(row, _DATE_FIELDS))) is not None]
    latest_date = max(dated) if dated else None
    dated_count = len(dated)
    institution_count = sum(_present(row.get("institution") or row.get("机构")) for row in reports)
    provenance_count = sum(_has_provenance(row) for row in reports)
    detail_count = sum(_first(
        row,
        ("content", "content_text", "detail_api_url", "detail_url", "pdf_url", "报告PDF链接"),
    ) is not None for row in reports)
    source_distribution: dict[str, int] = {}
    for row in reports:
        source = str(_first(row, _SOURCE_FIELDS) or "未标注来源").strip()
        source_distribution[source] = source_distribution.get(source, 0) + 1

    reasons: list[dict[str, Any]] = []
    if not reports:
        reasons.append(_reason(
            "RESEARCH_REPORTS_UNAVAILABLE",
            "当前公开来源没有返回该证券的研报。",
            "重试公开源并记录无数据或接口失败；授权机构研报应通过合规数据源接入。",
            severity="ERROR",
        ))
        status = "UNAVAILABLE"
    else:
        if dated_count < len(reports):
            reasons.append(_reason(
                "REPORT_DATE_PARTIAL",
                f"{len(reports) - dated_count} 篇研报缺少可解析日期。",
                "补齐报告日期和来源观察时间，无法确认时不要用抓取日期替代报告日期。",
            ))
        if institution_count < len(reports):
            reasons.append(_reason(
                "REPORT_INSTITUTION_PARTIAL",
                f"{len(reports) - institution_count} 篇研报缺少发布机构。",
                "补齐机构原名并维护机构名称标准化映射。",
            ))
        if provenance_count < len(reports):
            reasons.append(_reason(
                "REPORT_PROVENANCE_PARTIAL",
                f"{len(reports) - provenance_count} 篇研报缺少来源或原始链接。",
                "保存来源代码、外部标识、原文/PDF 链接和采集时间。",
            ))
        if detail_count < len(reports):
            reasons.append(_reason(
                "REPORT_DETAIL_PARTIAL",
                f"{len(reports) - detail_count} 篇研报没有正文或可获取详情的引用。",
                "增量抓取公开正文或 PDF；受版权限制时仅保存合规摘要和来源链接。",
            ))
        freshness_days = (as_of - latest_date).days if latest_date else None
        if freshness_days is not None and freshness_days > 90:
            reasons.append(_reason(
                "REPORT_FEED_POSSIBLY_STALE",
                f"最新研报距质量评估日已有 {freshness_days} 天。",
                "先检查增量采集水位和接口失败记录，再区分确实无新研报与采集停滞。",
            ))
        status = "PARTIAL" if reasons else "AVAILABLE"
    freshness_days = (as_of - latest_date).days if latest_date else None
    return ({
        "key": "research_reports",
        "label": "研究报告",
        "status": status,
        "coverage_scope": "OBSERVED_PUBLIC_SOURCE_ROWS",
        "completeness_claim": "UNBOUNDED",
        "record_count": len(reports),
        "source_count": len([key for key in source_distribution if key != "未标注来源"]),
        "source_distribution": source_distribution,
        "latest_report_date": latest_date.isoformat() if latest_date else None,
        "freshness_days": freshness_days,
        "date_coverage_ratio": _ratio(dated_count, len(reports)),
        "institution_coverage_ratio": _ratio(institution_count, len(reports)),
        "provenance_coverage_ratio": _ratio(provenance_count, len(reports)),
        "detail_coverage_ratio": _ratio(detail_count, len(reports)),
        "sources": _source_values(reports, research.get("report_source")),
        "missing_reasons": reasons,
        "coverage_note": "公开接口通常不提供全部境内外机构研报全集；未配置授权源或目标机构基准时，不能据此断言特定机构没有发布研报。",
    }, reports)


def audit_research_quality(
    research: dict[str, Any] | None,
    *,
    concepts: list[dict[str, Any]] | None = None,
    as_of: date | None = None,
) -> dict[str, Any]:
    """Assess the normalized research payload for one security.

    ``research`` should preferably be the output of ``build_research_contract``.
    Legacy list-shaped sections are accepted so the function can also be used
    by diagnostics and tests without database access.
    """
    payload = research if isinstance(research, dict) else {}
    assessment_date = as_of or date.today()
    report_dimension, reports = _audit_reports(payload, as_of=assessment_date)
    dimensions = [
        _audit_concepts([row for row in concepts or [] if isinstance(row, dict)]),
        _audit_earnings(payload),
        _audit_institutions(payload),
        _audit_ratings(payload, reports),
        report_dimension,
    ]
    issues = [
        {"dimension": dimension["key"], **reason}
        for dimension in dimensions
        for reason in dimension.get("missing_reasons") or []
    ]
    counts = {
        status.lower(): sum(dimension["status"] == status for dimension in dimensions)
        for status in ("COMPLETE", "AVAILABLE", "PARTIAL", "UNAVAILABLE")
    }
    if counts["unavailable"] == len(dimensions):
        overall = "UNAVAILABLE"
    elif counts["partial"] or counts["unavailable"]:
        overall = "PARTIAL"
    else:
        # Concept, institution and report coverage is unbounded, so even a
        # clean bounded contract must not be described as globally complete.
        overall = "AVAILABLE"
    return {
        "status": overall,
        "scope": "CURRENT_SECURITY_RESEARCH_READ_MODEL",
        "assessment_date": assessment_date.isoformat(),
        "summary": {
            "dimension_count": len(dimensions),
            **counts,
            "issue_count": len(issues),
            "error_count": sum(issue.get("severity") == "ERROR" for issue in issues),
            "warning_count": sum(issue.get("severity") == "WARNING" for issue in issues),
        },
        "dimensions": dimensions,
        "issues": issues,
        "methodology": (
            "仅对当前股票已接入来源、标准字段和证据完整性进行审计；"
            "没有权威全集基准的概念、机构和研报不作全量完成声明。"
        ),
    }
