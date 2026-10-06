from datetime import date

from app.services.research_quality import audit_research_quality


def _dimensions(result: dict) -> dict[str, dict]:
    return {item["key"]: item for item in result["dimensions"]}


def test_empty_research_quality_contract_reports_explicit_unavailable_reasons() -> None:
    result = audit_research_quality({}, concepts=[], as_of=date(2026, 10, 6))

    assert result["status"] == "UNAVAILABLE"
    assert result["summary"]["unavailable"] == 5
    dimensions = _dimensions(result)
    assert dimensions["concepts"]["missing_reasons"][0]["code"] == "CONCEPT_ROWS_UNAVAILABLE"
    assert dimensions["earnings_metrics"]["missing_reasons"][0]["code"] == "EARNINGS_FORECAST_UNAVAILABLE"
    assert dimensions["institution_forecasts"]["missing_reasons"][0]["code"] == "INSTITUTION_FORECAST_UNAVAILABLE"
    assert dimensions["ratings"]["missing_reasons"][0]["code"] == "RATING_DATA_UNAVAILABLE"
    assert dimensions["research_reports"]["missing_reasons"][0]["code"] == "RESEARCH_REPORTS_UNAVAILABLE"


def test_quality_contract_separates_bounded_metric_gaps_from_unbounded_source_coverage() -> None:
    periods = ["1个月内", "2个月内", "3个月内", "6个月内", "1年内"]
    research = {
        "earnings_forecast": {
            "forecast_years": ["2026", "2027"],
            "source": "同花顺盈利预测",
            "metrics": [
                {
                    "metric_code": "EPS",
                    "metric_name": "每股收益",
                    "values": {"2026": {"mean": 2.1}, "2027": {"mean": 2.2}},
                },
                {
                    "metric_code": "NET_PROFIT",
                    "metric_name": "归母净利润",
                    "values": {"2026": {"mean": 430.0}, "2027": {"mean": 450.0}},
                },
            ],
            "rows": [
                {
                    "forecast_year": year,
                    "metric_code": code,
                    "mean": value,
                    "source_code": "TONGHUASHUN",
                }
                for year, code, value in (
                    ("2026", "EPS", 2.1),
                    ("2027", "EPS", 2.2),
                    ("2026", "NET_PROFIT", 430.0),
                    ("2027", "NET_PROFIT", 450.0),
                )
            ],
        },
        "institution_forecast": {
            "forecast_years": ["2026", "2027"],
            "source": "同花顺业绩预测详表-机构",
            "rows": [{
                "institution": "测试证券",
                "report_date": "2026-09-30",
                "title": "半年报点评",
                "eps": {"2026": 2.1, "2027": 2.2},
                "net_profit": {"2026": "430亿元"},
                "source_code": "TONGHUASHUN",
                "detail_url": "https://example.test/forecast",
            }],
            "rating_statistics": [{"period": period, "buy": 1, "total": 1} for period in periods],
            "rating_reference_date": "2026-09-30",
            "rating_reference_basis": "最新报告日期",
        },
        "provider_rating_statistics": {
            "source_name": "东方财富盈利预测评级汇总",
            "source_url": "https://example.test/ratings",
            "reference_period": "近六个月",
            "as_of": "2026-09-30",
            "buy": 1,
            "total": 1,
        },
        "latest_reports": [{
            "source_code": "EASTMONEY",
            "external_id": "report-1",
            "title": "半年报点评",
            "report_date": "2026-09-30",
            "institution": "测试证券",
            "rating": "买入",
            "detail_url": "https://example.test/report-1",
        }],
        "reports": [],
        "report_source": "东方财富研究报告",
    }
    concepts = [
        {
            "name": "银行",
            "dimension": "INDUSTRY",
            "definition": "银行业分类。",
            "source_name": "行业分类主数据",
        },
        {
            "name": "跨境支付",
            "dimension": "THEME",
            "definition": "跨境支付主题。",
            "source_name": "概念分类主数据",
        },
    ]

    result = audit_research_quality(research, concepts=concepts, as_of=date(2026, 10, 6))
    dimensions = _dimensions(result)

    earnings = dimensions["earnings_metrics"]
    assert earnings["status"] == "PARTIAL"
    assert earnings["coverage_ratio"] == round(4 / 14, 6)
    assert {item["metric_code"] for item in earnings["missing_metrics"]} == {
        "TOTAL_REVENUE",
        "REVENUE_YOY",
        "NET_PROFIT_YOY",
        "FORWARD_PE",
        "DIVIDEND_YIELD",
    }

    institutions = dimensions["institution_forecasts"]
    assert institutions["field_coverage_ratio"] == 0.75
    assert institutions["completeness_claim"] == "UNBOUNDED"
    assert "INSTITUTION_NET_PROFIT_PARTIAL" in {
        item["code"] for item in institutions["missing_reasons"]
    }
    assert dimensions["ratings"]["status"] == "AVAILABLE"
    assert dimensions["research_reports"]["status"] == "AVAILABLE"
    assert dimensions["concepts"]["status"] == "AVAILABLE"
    assert "不能据此断言不存在漏项" in dimensions["concepts"]["coverage_note"]


def test_complete_metric_matrix_is_complete_only_within_declared_contract() -> None:
    metric_codes = (
        "TOTAL_REVENUE",
        "REVENUE_YOY",
        "NET_PROFIT",
        "NET_PROFIT_YOY",
        "EPS",
        "FORWARD_PE",
        "DIVIDEND_YIELD",
    )
    research = {
        "earnings_forecast": {
            "forecast_years": ["2026"],
            "source": "测试来源",
            "metrics": [
                {
                    "metric_code": code,
                    "metric_name": code,
                    "values": {"2026": {"mean": index + 1}},
                }
                for index, code in enumerate(metric_codes)
            ],
            "rows": [
                {
                    "forecast_year": "2026",
                    "metric_code": code,
                    "mean": index + 1,
                    "source_code": "TEST",
                }
                for index, code in enumerate(metric_codes)
            ],
        }
    }

    dimension = _dimensions(
        audit_research_quality(research, concepts=[], as_of=date(2026, 10, 6))
    )["earnings_metrics"]

    assert dimension["status"] == "COMPLETE"
    assert dimension["coverage_ratio"] == 1.0
    assert dimension["completeness_claim"] == "BOUNDED_CONTRACT"
    assert dimension["missing_metrics"] == []
