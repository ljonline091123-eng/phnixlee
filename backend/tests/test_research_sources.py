from __future__ import annotations

from unittest.mock import patch

import pandas as pd

from app.connectors.akshare_adapter import AkshareAdapter
from app.services.f10 import normalize_f10_sections


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
