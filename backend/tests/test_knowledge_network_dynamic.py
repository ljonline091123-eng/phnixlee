from datetime import datetime, timezone

import pytest
from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session

from app.db.base import Base
from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, KnowledgeGraph, ResearchReportRecord
from app.models.context_event import StockContextEvent
from app.models.market_data import (
    DataSource, StockF10Cache, StockFinancialReport, StockKline, StockNews,
    StockNotice, StockRealtimeQuote,
)
from app.services.knowledge_network_dynamic import build_dynamic_projection, financial_metrics


NOW = datetime(2026, 9, 20, tzinfo=timezone.utc)


@pytest.fixture
def context():
    engine = create_engine("sqlite://")
    Base.metadata.create_all(engine)
    with Session(engine) as db:
        source = DataSource(source_code="PROJECTION_TEST", source_name="测试来源", adapter_type="MOCK")
        db.add(source)
        db.commit()
        yield db, source
    engine.dispose()


def row_fields(source, market="CN_A", symbol="000001"):
    return {"market": market, "symbol": symbol, "source_id": source.id, "fetched_at": NOW}


def by_type(result, kind):
    return [node for node in result["nodes"] if node["type"] == kind]


def test_financial_real_values_currency_and_unknown_publication_are_preserved(context):
    db, source = context
    financial = StockFinancialReport(**row_fields(source, "HK", "00700"), indicator="报告期", report_period="2026-06-30",
        currency="HKD", data_json={"OPERATE_INCOME": 401243000000, "HOLDER_PROFIT": 114115000000,
            "GROSS_PROFIT_RATIO": "57.25%", "BASIC_EPS": 12.639, "ORG_CODE": 10009066,
            "REPORT_DATE": "2026-06-30", "EPS_TTM": None})
    db.add(financial)
    db.commit()
    result = build_dynamic_projection(db, "HK", "00700", company_id="issuer")
    metrics = {node["properties"]["metric_code"]: node for node in by_type(result, "FINANCIAL_OBSERVATION")}
    assert metrics["revenue"]["properties"]["value"] == 401243000000
    assert metrics["revenue"]["properties"]["unit"] == "港元"
    assert metrics["holder_profit"]["properties"]["value"] == 114115000000
    assert metrics["gross_margin"]["properties"]["value"] == 57.25
    assert metrics["basic_eps"]["properties"]["unit"] == "港元/股"
    assert metrics["revenue"]["properties"]["published_at"] is None
    assert metrics["revenue"]["properties"]["canonical_company_id"] == "company:issuer"
    assert len(metrics) == 4
    assert len(result["evidence"]) == 1
    assert result["evidence"][0]["observed_at"].startswith("2026-09-20")
    assert all(edge["properties"]["confidence"] is None for edge in result["edges"])
    assert not db.dirty and not db.new and not db.deleted


def test_invalid_financial_values_are_not_zero_and_explicit_units_take_precedence():
    row = StockFinancialReport(market="CN_A", symbol="000001", source_id=1, indicator="x", report_period="2025",
        data_json={"revenue": "1,234.5", "parent_net_profit": "--", "net_profit": float("nan"),
            "gross_margin": True, "NOTICE_DATE": "2026-03-01", "units": {"revenue": "万元"}, "restated_flag": True})
    metrics = financial_metrics(row)
    assert len(metrics) == 1
    assert metrics[0]["value"] == 1234.5 and metrics[0]["unit"] == "万元"
    assert metrics[0]["currency"] is None and metrics[0]["restated_flag"] is True
    assert metrics[0]["published_at"] == "2026-03-01"


def test_keyword_and_external_mentions_never_become_verified_facts(context):
    db, source = context
    news = StockNews(**row_fields(source), news_time="2026-09-20", title="供应商合同公告提及股东增持", content="这是政策讨论，未披露交易对手。", url="https://example.test/news")
    event = StockContextEvent(market="CN_A", symbol="000001", event_type="CONTRACT", title="一项外部线索",
        content="未经审核的事件", source_name="外部来源", url="https://example.test/event", published_at=NOW,
        related_entity="未解析主体", content_hash="a" * 64)
    db.add_all([news, event])
    db.commit()
    result = build_dynamic_projection(db, "CN_A", "000001")
    candidates = by_type(result, "CANDIDATE_EVENT")
    assert len(candidates) == 5
    assert all(node["status"] == "CANDIDATE" for node in candidates)
    assert not any(edge["type"] in {"AFFECTED_BY", "SUPPLIES_TO", "HOLDS_EQUITY_IN"} for edge in result["edges"])
    assert all(edge["status"] == "CANDIDATE" for edge in result["edges"] if edge["target"] in {node["id"] for node in candidates})
    assert by_type(result, "EVIDENCE_DOCUMENT")[0]["properties"]["source_url"] == "https://example.test/news"


def test_daily_anomalies_use_only_same_source_adjustment_and_prior_dates(context):
    db, source = context
    fields = row_fields(source)
    rows = [StockKline(**fields, trade_date=f"2026-09-{day:02d}", period="daily", adjust="qfq", volume=100,
        open_price=10, close_price=10, turnover_rate=1) for day in range(1, 6)]
    latest = StockKline(**fields, trade_date="2026-09-06", period="daily", adjust="qfq", volume=400,
        open_price=10, close_price=11, turnover_rate=21)
    # The same day with a different adjustment must not pollute the 100-volume baseline.
    rows.extend([latest, StockKline(**fields, trade_date="2026-09-05", period="daily", adjust="hfq", volume=100000),
        StockKline(**fields, trade_date="2026-09-30", period="weekly", adjust="qfq", volume=100000)])
    db.add_all(rows)
    db.commit()
    result = build_dynamic_projection(db, "CN_A", "000001")
    anomalies = {node["properties"]["event_type"]: node for node in by_type(result, "MARKET_EVENT")}
    assert set(anomalies) == {"VOLUME_SPIKE", "INTRADAY_PRICE_MOVE", "TURNOVER_SPIKE"}
    volume = anomalies["VOLUME_SPIKE"]["properties"]
    assert volume["baseline_volume"] == 100 and volume["baseline_window"] == 5
    assert latest.id not in volume["baseline_record_ids"]
    assert all(item["volume"] == 100 for item in volume["baseline_observations"])
    assert all(node["status"] == "DERIVED" for node in anomalies.values())
    assert len(by_type(result, "MARKET_OBSERVATION")) == 1
    assert by_type(result, "MARKET_OBSERVATION")[0]["properties"]["trade_date"] == "2026-09-06"
    assert len(result["evidence"]) == 1


def test_insufficient_volume_history_does_not_generate_volume_anomaly(context):
    db, source = context
    db.add(StockKline(**row_fields(source), trade_date="2026-09-20", volume=100000, open_price=10, close_price=10))
    db.commit()
    result = build_dynamic_projection(db, "CN_A", "000001")
    assert not by_type(result, "MARKET_EVENT")
    assert any("不足3日" in warning for warning in result["warnings"])


def test_exact_seven_percent_price_threshold_is_not_lost_to_float_roundoff(context):
    db, source = context
    db.add(StockKline(**row_fields(source), trade_date="2026-09-20", volume=100, open_price=10, close_price=10.7))
    db.commit()
    result = build_dynamic_projection(db, "CN_A", "000001")
    assert [node["properties"]["event_type"] for node in by_type(result, "MARKET_EVENT")] == ["INTRADAY_PRICE_MOVE"]


def test_read_projection_is_bounded_market_scoped_and_reuses_legacy_evidence(context):
    db, source = context
    financial = StockFinancialReport(**row_fields(source), indicator="报告期", report_period="2026-06-30", data_json={"revenue": 100})
    db.add(financial)
    db.flush()
    kb = KnowledgeBase(kb_code="dynamic", kb_name="旧知识库", source_tables=[])
    db.add(kb)
    db.flush()
    graphs = [KnowledgeGraph(knowledge_base_id=kb.id, graph_code=f"dynamic-{i}", graph_name="旧图谱", source_tables=[]) for i in range(2)]
    db.add_all(graphs)
    db.flush()
    docs = [KnowledgeDocument(knowledge_base_id=kb.id, graph_id=graph.id, market="CN_A", symbol="000001",
        source_table="stock_financial_report", source_record_id=financial.id, title="旧财务证据", content="原始内容") for graph in graphs]
    db.add_all(docs)
    db.add_all([StockNews(**row_fields(source), news_time=f"2026-09-{day:02d}", title=f"消息{day}", content="无关键词") for day in range(1, 9)])
    db.add(StockNews(**row_fields(source, "HK", "000001"), news_time="2026-09-20", title="另一个市场", content="合同"))
    db.commit()
    result = build_dynamic_projection(db, "CN_A", "000001", per_category=2, graph_id=graphs[0].id)
    assert result["coverage"]["news"] == {"available": True, "total_records": 8, "shown_records": 2, "truncated": True, "status": "AVAILABLE"}
    assert not any(item["title"] == "另一个市场" for item in result["evidence"])
    evidence = next(item for item in result["evidence"] if item["source_table"] == "stock_financial_report")
    assert evidence["legacy_document_ids"] == [docs[0].id]
    assert all(edge["source"] == "security:CN_A:000001" or edge["source"] in {node["id"] for node in result["nodes"]} for edge in result["edges"])
    assert all(edge["target"] in {node["id"] for node in result["nodes"]} for edge in result["edges"])
    assert [doc.content for doc in db.scalars(select(KnowledgeDocument).order_by(KnowledgeDocument.id))] == ["原始内容", "原始内容"]


def test_documents_research_and_realtime_keep_provenance_without_asserting_shareholder_actions(context):
    db, source = context
    db.add_all([
        StockNotice(**row_fields(source), notice_date="2026-09-19", title="年度报告", content_json={"body": "正文"}, url="https://example.test/report"),
        StockF10Cache(**row_fields(source), section="shareholders", payload_json={"major": [], "message": "未取得股东明细"}),
        StockRealtimeQuote(**row_fields(source), current_price=15.5, quote_time="2026-09-20 14:30:00", volume=500),
        ResearchReportRecord(market="CN_A", symbol="000001", title="模型研报", report_markdown="研究观点",
            model_provider="test", model_instance="test-model", rating="BUY", score=80, created_at=NOW),
    ])
    db.commit()
    result = build_dynamic_projection(db, "CN_A", "000001")
    assert not by_type(result, "SHAREHOLDER_ACTION")
    report = next(item for item in result["evidence"] if item["source_table"] == "research_report")
    assert report["model_instance"] == "test-model" and "研究意见" in report["note"]
    assert result["coverage"]["f10"]["available"]
    hidden = build_dynamic_projection(db, "CN_A", "000001", include_evidence=False)
    assert len(hidden["evidence"]) == 4
    assert not any(node["layer"] == "EVIDENCE" for node in hidden["nodes"])
    assert {node["type"] for node in hidden["nodes"]} == {"MARKET_OBSERVATION", "F10_SECTION"}
    f10 = next(node for node in hidden["nodes"] if node["type"] == "F10_SECTION")
    assert f10["properties"]["section_label"] == "股东资料"
    assert f10["properties"]["content_overview"] == "major 0 条"
    assert hidden["edges"][0]["source"] == "security:CN_A:000001"
    assert hidden["edges"][0]["target"] == hidden["nodes"][0]["id"]


def test_f10_items_replace_non_finite_values_without_changing_source_evidence(context):
    db, source = context
    db.add(StockF10Cache(**row_fields(source), section="business_analysis",
        payload_json={"items": [{"name": "测试业务", "gross_margin": float("nan")}]}))
    db.commit()
    result = build_dynamic_projection(db, "CN_A", "000001")
    item = next(node for node in result["nodes"] if node["type"] == "F10_ITEM")
    assert item["properties"]["gross_margin"] is None
    proof = next(entry for entry in result["evidence"] if entry["source_table"] == "stock_f10_cache")
    assert "NaN" in proof["excerpt"]


def test_empty_security_returns_explicit_gaps_not_fabricated_values(context):
    db, _ = context
    result = build_dynamic_projection(db, "HK", "99999")
    assert result["nodes"] == result["edges"] == result["evidence"] == []
    assert len(result["coverage"]) == 8
    assert all(not item["available"] for item in result["coverage"].values())
