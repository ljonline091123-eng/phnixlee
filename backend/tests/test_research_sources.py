from __future__ import annotations

import json
from datetime import date
from unittest.mock import patch

import pandas as pd

from app.connectors.akshare_adapter import AkshareAdapter
from app.services.f10 import normalize_f10_sections


def test_ths_html_fallback_maps_yjycdata_to_eps_and_forward_pe() -> None:
    """The THS HTML third column is PE, never net profit."""
    current_year = date.today().year
    rows = [
        [str(current_year - 2), "0.16", "4.34", "SJ"],
        [str(current_year - 1), None, "5.25", "SJ"],
        [str(current_year), None, None, "SJ"],
        [str(current_year + 1), "9.99", "10.0", "SJ"],
    ]
    response = type("Response", (), {
        "content": (
            f'<div id="yjycData">{json.dumps(rows, ensure_ascii=False)}</div>'
        ).encode("utf-8"),
        "raise_for_status": lambda self: None,
    })()
    client = type("Client", (), {
        "__enter__": lambda self: self,
        "__exit__": lambda self, *args: None,
        "get": lambda self, url: response,
    })()
    with patch("app.connectors.akshare_adapter.httpx.Client", return_value=client):
        parsed = AkshareAdapter._safe_ths_historical_forecast_records("000008")

    assert {(row["forecast_year"], row["metric_code"]) for row in parsed} == {
        (str(current_year - 2), "EPS"),
        (str(current_year - 2), "FORWARD_PE"),
        (str(current_year - 1), "FORWARD_PE"),
    }
    assert next(row for row in parsed if row["metric_code"] == "FORWARD_PE")["value"] == 4.34
    assert all(row["metric_code"] != "NET_PROFIT" for row in parsed)


def test_historical_only_earnings_are_visible_as_actuals_and_partial() -> None:
    current_year = date.today().year
    payload = normalize_f10_sections({
        "profile": {"fields": {}, "concepts": []},
        "holders": {},
        "financial_summary": {},
        "financial_statements": {},
        "business_composition": {},
        "research_sections": {
            "earnings_forecast": [
                {
                    "forecast_year": str(current_year - 1),
                    "metric": "每股收益",
                    "metric_code": "EPS",
                    "value": 0.16,
                    "actual": True,
                    "is_forecast": False,
                    "value_scope": "ACTUAL",
                },
                {
                    "forecast_year": str(current_year - 1),
                    "metric": "预测市盈率",
                    "metric_code": "FORWARD_PE",
                    "value": 4.34,
                    "actual": True,
                    "is_forecast": False,
                    "value_scope": "ACTUAL",
                },
            ],
            "reports": [],
        },
    })
    contract = payload["research_sections"]["earnings_forecast"]
    assert contract["actual_years"] == [str(current_year - 1)]
    metrics = {row["metric_code"]: row for row in contract["metrics"]}
    assert metrics["EPS"]["actual_values"][str(current_year - 1)]["value"] == 0.16
    assert metrics["FORWARD_PE"]["actual_values"][str(current_year - 1)]["value"] == 4.34
    assert contract["completeness"]["status"] == "PARTIAL"
    assert all(year != str(current_year) for year in contract["actual_years"])


def test_historical_reports_fallback_keeps_rating_statistics_and_exclusive_lists() -> None:
    current_year = date.today().year
    payload = normalize_f10_sections({
        "profile": {"fields": {}, "concepts": []},
        "holders": {},
        "financial_summary": {},
        "financial_statements": {},
        "business_composition": {},
        "research_sections": {
            "reports": [
                {
                    "source_code": "EASTMONEY",
                    "external_id": "old-1",
                    "title": "历史研报一",
                    "report_date": f"{current_year - 2}-09-01",
                    "rating": "买入",
                },
                {
                    "source_code": "EASTMONEY",
                    "external_id": "old-2",
                    "title": "历史研报二",
                    "report_date": f"{current_year - 2}-08-01",
                    "rating": "增持",
                },
            ],
            "institution_forecast": [],
        },
    })
    research = payload["research_sections"]
    assert research["latest_reports_window_status"] == "NO_REPORT_IN_LAST_YEAR"
    assert {row["external_id"] for row in research["latest_reports"]} == {"old-1", "old-2"}
    assert research["reports"] == []
    institution = next(row for row in research["sections"] if row["key"] == "institution_forecast")
    assert institution["rows"] == []
    assert any(row["total"] > 0 for row in institution["rating_statistics"])


def test_cninfo_qa_rows_are_scoped_sorted_and_traceable() -> None:
    adapter = AkshareAdapter()
    frame = pd.DataFrame([
        {
            "股票代码": "000002",
            "问题": "较早问题",
            "回答内容": "较早回答",
            "回答者": "万科A",
            "提问时间": "2026-09-01 09:00:00",
            "更新时间": "2026-09-02 09:00:00",
            "问题编号": "q-old",
        },
        {
            "股票代码": "000001",
            "问题": "其他股票不可泄漏",
            "提问时间": "2026-09-30 09:00:00",
            "问题编号": "q-other",
        },
        {
            "股票代码": "000002",
            "问题": "最新问题",
            "提问时间": "2026-09-30 09:00:00",
            "更新时间": "2026-09-30 10:00:00",
            "问题编号": "q-new",
        },
    ])
    with patch("app.connectors.akshare_adapter.ak.stock_irm_cninfo", return_value=frame):
        rows = adapter._safe_qa_records("000002")

    assert [row["question_id"] for row in rows] == ["q-new", "q-old"]
    assert all(row["股票代码"] == "000002" for row in rows)
    assert rows[0]["source_name"] == "巨潮资讯互动易"
    assert rows[0]["detail_url"].endswith("questionId=q-new")


def test_ths_forecast_rows_keep_consensus_statistics_and_institution_date_order() -> None:
    adapter = AkshareAdapter()

    def forecast(*, symbol: str, indicator: str) -> pd.DataFrame:
        assert symbol == "000002"
        if indicator == "预测年报每股收益":
            return pd.DataFrame([{
                "年度": "2026", "预测机构数": 6, "最小值": -3.43,
                "均值": -2.26, "最大值": -1.4, "行业平均数": 0.15,
            }])
        if indicator == "预测年报净利润":
            return pd.DataFrame([{
                "年度": "2026", "预测机构数": 6, "最小值": -409.26,
                "均值": -269.5, "最大值": -167.03, "行业平均数": -3.2,
            }])
        if indicator == "业绩预测详表-机构":
            return pd.DataFrame([
                {"机构名称": "乙机构", "研究员": "乙分析师", "报告日期": "2026-09-03", "预测年报每股收益2026预测": 1.0},
                {"机构名称": "甲机构", "研究员": "甲分析师", "报告日期": "2026-09-10", "预测年报每股收益2026预测": 2.0},
            ])
        return pd.DataFrame()

    with patch("app.connectors.akshare_adapter.ak.stock_profit_forecast_ths", side_effect=forecast):
        earnings = adapter._safe_profit_forecast_records("000002")
        institutions = adapter._safe_institution_forecast_records("000002", [])

    assert {row["预测指标"] for row in earnings} == {"每股收益（元）", "归母净利润"}
    eps = next(row for row in earnings if row["预测指标"] == "每股收益（元）")
    assert eps["均值"] == -2.26
    assert eps["2026E"] == -2.26
    assert [row["机构"] for row in institutions] == ["甲机构", "乙机构"]
    assert all(row["source_name"] == "同花顺业绩预测详表-机构" for row in institutions)


def test_ths_detailed_forecast_rows_normalize_metric_scope_and_provenance() -> None:
    adapter = AkshareAdapter()
    with patch(
        "app.connectors.akshare_adapter.ak.stock_profit_forecast_ths",
        return_value=pd.DataFrame([
            {"预测指标": "营业收入(元)", "2025-实际值": "1314.42亿", "预测2026-平均": "1335.97亿"},
            {"预测指标": "营业收入增长率", "2025-实际值": "-10.40%", "预测2026-平均": "1.64%"},
            {"预测指标": "净利润(元)", "2025-实际值": "426.33亿", "预测2026-平均": "437.80亿"},
            {"预测指标": "净利润增长率", "2025-实际值": "-4.21%", "预测2026-平均": "2.86%"},
            {"预测指标": "市盈率(动态)", "2025-实际值": "5.48", "预测2026-平均": "5.23"},
        ]),
    ):
        rows = adapter._safe_detailed_forecast_records("000001")

    by_metric = {(row["metric_code"], row["forecast_year"]): row for row in rows}
    assert by_metric[("TOTAL_REVENUE", "2026")]["value"] == "1335.97亿"
    assert by_metric[("TOTAL_REVENUE", "2026")]["is_forecast"] is True
    assert by_metric[("TOTAL_REVENUE", "2025")]["actual"] is True
    assert by_metric[("REVENUE_YOY", "2026")]["unit"] == "%"
    assert by_metric[("NET_PROFIT", "2026")]["value"] == "437.80亿"
    assert by_metric[("NET_PROFIT_YOY", "2026")]["value"] == "2.86%"
    assert by_metric[("FORWARD_PE", "2026")]["value"] == "5.23"
    assert all(row["source_code"] == "TONGHUASHUN" for row in rows)
    assert all("worth.html" in row["source_url"] for row in rows)


def test_research_report_projection_is_newest_first_and_has_detail_provenance() -> None:
    adapter = AkshareAdapter()
    frame = pd.DataFrame([
        {
            "股票代码": "000002", "报告名称": "旧报告", "东财评级": "中性",
            "机构": "甲机构", "日期": "2025-01-01", "报告PDF链接": "https://example.test/old.pdf",
        },
        {
            "股票代码": "000002", "报告名称": "新报告", "东财评级": "增持",
            "机构": "乙机构", "日期": "2026-09-01", "报告PDF链接": "https://example.test/new.pdf",
        },
    ])
    with patch("app.connectors.akshare_adapter.ak.stock_research_report_em", return_value=frame):
        rows = adapter._safe_research_report_records("000002")

    assert [row["报告名称"] for row in rows] == ["新报告", "旧报告"]
    assert rows[0]["report_id"].startswith("EM-000002-")
    assert rows[0]["pdf_url"] == "https://example.test/new.pdf"
    assert rows[0]["source_url"] == "https://data.eastmoney.com/report/stock.jshtml"


def test_rating_windows_anchor_to_rated_reports_not_newer_unrated_consensus() -> None:
    payload = normalize_f10_sections({
        "profile": {"fields": {}, "concepts": []},
        "holders": {},
        "financial_summary": {},
        "financial_statements": {},
        "business_composition": {},
        "research_sections": {
            "institution_forecast": [{
                "机构": "预测机构",
                "报告日期": "2026-09-30",
                "预测年报每股收益2027预测": 1.0,
            }],
            "reports": [
                {"机构": "甲机构", "东财评级": "买入", "日期": "2025-08-27"},
                {"机构": "乙机构", "东财评级": "增持", "日期": "2025-08-20"},
            ],
        },
    })
    section = next(
        item for item in payload["research_sections"]["sections"]
        if item["key"] == "institution_forecast"
    )
    assert section["rating_statistics_reference_date"] == "2025-08-27"
    one_month = next(row for row in section["rating_statistics"] if row["period"] == "1个月内")
    assert one_month["buy"] == 1
    assert one_month["add"] == 1


def test_research_layout_keeps_exactly_six_sections_and_embeds_classification_trend() -> None:
    concept = {
        "name": "房地产开发",
        "classification_dimension": "INDUSTRY",
        "member_count": 2,
        "trend_pct": 1.25,
        "related_stocks": [
            {"symbol": "000002", "name": "万科A", "change_pct": 1.0},
            {"symbol": "001979", "name": "招商蛇口", "change_pct": 1.5},
        ],
    }
    payload = normalize_f10_sections({
        "profile": {"fields": {}, "concepts": [concept]},
        "holders": {},
        "financial_summary": {},
        "financial_statements": {},
        "business_composition": {},
        "research_sections": {},
    })

    sections = payload["research_sections"]["sections"]
    assert [section["key"] for section in sections] == [
        "industry_concepts",
        "qa",
        "earnings_forecast",
        "institution_forecast",
        "latest_reports",
        "reports",
    ]
    row = sections[0]["rows"][0]
    assert row["member_count"] == 2
    assert row["trend_pct"] == 1.25
    assert len(row["related_stocks"]) == 2
