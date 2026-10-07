from __future__ import annotations

from unittest.mock import patch

from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session

from app.db.base import Base
import app.models  # noqa: F401
from app.models.research_data import (
    StockBrokerResearchReport,
    StockEarningsConsensus,
    StockInstitutionForecast,
    StockInvestorQA,
)
from app.services.f10 import build_research_contract
from app.services.research_store import (
    get_or_fetch_report_detail,
    load_research_sections,
    persist_research_sections,
)


def test_research_sections_are_dual_written_and_loaded_from_normalized_tables() -> None:
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(engine)
    with Session(engine) as db:
        persist_research_sections(db, "CN_A", "000002", {
            "reports": [{
                "source_code": "SINA_FINANCE", "external_id": "842236924395",
                "title": "万科A半年报点评", "report_date": "2026-09-08",
                "institution": "东方财富证券", "rating": "中性",
                "content": "公开研报正文", "detail_url": "https://example.test/report",
            }],
            "earnings_forecast": [{
                "forecast_year": "2026", "metric": "每股收益（元）",
                "prediction_count": 6, "mean": -2.26, "source_name": "同花顺盈利预测",
            }],
            "institution_forecast": [{
                "机构": "测试证券", "研究员": "甲", "报告日期": "2026-09-01",
                "预测年报每股收益2026预测": -2.2, "source_name": "同花顺业绩预测详表-机构",
            }],
            "qa": [{
                "question_id": "q-1", "question": "经营情况如何？", "answer": "请见定期报告。",
                "updated_at": "2026-09-10 10:00:00", "source_name": "巨潮资讯互动易",
            }],
        })
        db.commit()

        assert db.scalar(select(StockBrokerResearchReport)) is not None
        assert db.scalar(select(StockEarningsConsensus)) is not None
        assert db.scalar(select(StockInstitutionForecast)) is not None
        assert db.scalar(select(StockInvestorQA)) is not None
        loaded = load_research_sections(db, "CN_A", "000002")
        assert loaded["reports"][0]["content"] == "公开研报正文"
        assert loaded["earnings_forecast"][0]["forecast_year"] == "2026"
        assert loaded["institution_forecast"][0]["institution"] == "测试证券"
        assert loaded["qa"][0]["question_id"] == "q-1"


def test_research_persistence_deduplicates_rows_without_collapsing_distinct_metrics() -> None:
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(engine)
    research = {
        "reports": [{
            "source_code": "EASTMONEY", "external_id": "report-1", "title": "机构研报",
            "report_date": "2026-09-08", "institution": "测试机构",
        }],
        "latest_reports": [{
            "source_code": "EASTMONEY", "external_id": "report-1", "title": "机构研报",
            "report_date": "2026-09-08", "institution": "测试机构",
            "content": "已获取的完整研报正文", "pdf_url": "https://example.test/report.pdf",
        }],
        "earnings_forecast": {"rows": [
            {
                "forecast_year": "2026", "metric": "归母净利润", "metric_code": "NET_PROFIT",
                "prediction_count": 12, "mean": 437.42, "source_code": "TONGHUASHUN",
            },
            {
                "forecast_year": "2026", "metric": "归母净利润同比", "metric_code": "NET_PROFIT",
                "prediction_count": 12, "mean": 18.5, "source_code": "TONGHUASHUN",
            },
            {
                "forecast_year": "2026", "metric": "归母净利润", "metric_code": "NET_PROFIT",
                "prediction_count": 12, "mean": 438.0, "source_code": "TONGHUASHUN",
                "source_updated_at": "2026-09-08",
            },
        ]},
        "institution_forecast": {"rows": [
            {
                "机构": "摩根大通", "研究员": "分析师甲", "报告日期": "2026-09-08",
                "预测年报每股收益2026预测": 2.25, "source_name": "同花顺机构预测",
            },
            {
                "机构": "摩根大通", "研究员": "分析师甲", "报告日期": "2026-09-08",
                "预测年报每股收益2026预测": 2.25, "source_name": "同花顺机构预测",
            },
            {
                "机构": "摩根大通", "研究员": "分析师乙", "报告日期": "2026-09-08",
                "预测年报每股收益2026预测": 2.3, "source_name": "同花顺机构预测",
            },
        ]},
        "qa": {"rows": [
            {"question_id": "q-2", "question": "经营情况？", "source_name": "巨潮资讯互动易"},
            {"question_id": "q-2", "question": "经营情况？", "answer": "以公告披露为准。", "source_name": "巨潮资讯互动易"},
        ]},
    }
    with Session(engine) as db:
        persist_research_sections(db, "CN_A", "688627", research)
        db.commit()
        persist_research_sections(db, "CN_A", "688627", research)
        db.commit()

        reports = db.scalars(select(StockBrokerResearchReport)).all()
        earnings = db.scalars(select(StockEarningsConsensus).order_by(StockEarningsConsensus.metric_code)).all()
        institutions = db.scalars(select(StockInstitutionForecast)).all()
        qa = db.scalars(select(StockInvestorQA)).all()

        assert len(reports) == 1
        assert reports[0].content_text == "已获取的完整研报正文"
        assert len(earnings) == 2
        by_metric = {row.metric_code: row for row in earnings}
        assert by_metric["NET_PROFIT"].mean_value == "438.0"
        assert by_metric["NET_PROFIT_YOY"].mean_value == "18.5"
        assert by_metric["NET_PROFIT_YOY"].unit == "%"
        assert len(institutions) == 2
        assert {tuple(row.analysts_json) for row in institutions} == {("分析师甲",), ("分析师乙",)}
        assert len(qa) == 1
        assert qa[0].answer == "以公告披露为准。"
    engine.dispose()


def test_report_detail_is_local_first_and_does_not_repeat_remote_fetch() -> None:
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(engine)
    with Session(engine) as db:
        persist_research_sections(db, "CN_A", "000002", {"reports": [{
            "source_code": "SINA_FINANCE", "external_id": "local-1", "title": "本地研报",
            "report_date": "2026-09-08", "content": "已经落库的正文",
        }]})
        db.commit()
        with patch("app.services.research_store.fetch_sina_report_detail") as remote:
            result = get_or_fetch_report_detail(db, "CN_A", "000002", "SINA_FINANCE", "local-1")
        assert result is not None
        assert result["content"] == "已经落库的正文"
        remote.assert_not_called()


def test_fixed_research_contract_has_three_years_five_rating_windows_and_exclusive_reports() -> None:
    payload = build_research_contract({
        "earnings_forecast": [
            {"forecast_year": "2026", "metric": "每股收益（元）", "mean": 1.0},
            {"forecast_year": "2027", "metric": "每股收益（元）", "mean": 1.2},
            {"forecast_year": "2028", "metric": "每股收益（元）", "mean": 1.4},
        ],
        "institution_forecast": [{
            "机构": "测试证券", "报告日期": "2026-09-01",
            "预测年报每股收益2026预测": 1.0,
            "预测年报每股收益2027预测": 1.2,
            "预测年报每股收益2028预测": 1.4,
        }],
        "reports": [
            {"source_code": "SINA_FINANCE", "external_id": "new", "title": "新研报", "report_date": "2026-09-08", "rating": "持有"},
            {"source_code": "EASTMONEY", "external_id": "old", "title": "旧研报", "report_date": "2025-01-01", "rating": "买入"},
        ],
    })
    assert payload["earnings_forecast"]["forecast_years"] == ["2026", "2027", "2028"]
    assert payload["institution_forecast"]["forecast_years"] == ["2026", "2027", "2028"]
    assert len(payload["institution_forecast"]["rating_statistics"]) == 5
    assert payload["institution_forecast"]["rating_statistics"][0]["hold"] == 1
    assert {row["external_id"] for row in payload["latest_reports"]} == {"new"}
    assert {row["external_id"] for row in payload["reports"]} == {"old"}


def test_research_contract_keeps_provider_consensus_and_report_level_estimates_separate() -> None:
    payload = build_research_contract({
        "earnings_forecast": [{
            "forecast_year": "2026", "metric": "每股收益（元）", "mean": 2.17,
            "source_code": "TONGHUASHUN", "source_name": "同花顺盈利预测",
        }, {
            "forecast_year": "2026", "metric": "归母净利润", "mean": 437.80,
            "source_code": "TONGHUASHUN", "source_name": "同花顺盈利预测",
        }],
        "reports": [{
            "source_code": "EASTMONEY", "external_id": "em-1", "title": "机构盈利预测",
            "report_date": "2026-09-01", "institution": "测试机构", "rating": "增持",
            "2026-盈利预测-收益": 2.25, "2026-盈利预测-市盈率": 5.23,
            "source_url": "https://example.test/report",
        }],
        "provider_rating_statistics": {
            "buy": 2, "add": 3, "neutral": 1, "reduce": 0, "sell": 0,
            "total": 6, "reference_period": "近六个月", "source_code": "EASTMONEY",
            "source_name": "东方财富盈利预测评级汇总",
        },
    })
    metrics = {metric["metric_code"]: metric for metric in payload["earnings_forecast"]["metrics"]}
    eps_2026 = metrics["EPS"]["values"]["2026"]
    assert eps_2026["mean"] == 2.17
    assert eps_2026["source_code"] == "TONGHUASHUN"
    assert eps_2026["report_observations"][0]["value"] == 2.25
    assert eps_2026["report_observations"][0]["value_scope"] == "REPORT_LEVEL"
    assert "FORWARD_PE" in metrics
    assert payload["institution_forecast"]["rating_statistics"][3]["buy"] == 2
    assert payload["institution_forecast"]["rating_statistics"][3]["source_code"] == "EASTMONEY"
    assert payload["earnings_forecast"]["completeness"]["status"] == "PARTIAL"
