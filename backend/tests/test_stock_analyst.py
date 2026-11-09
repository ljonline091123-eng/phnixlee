"""Isolated checks for the independent analyst, with no provider charges."""
from datetime import datetime, timedelta, timezone
import json
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, func, select
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.db.base import Base
from app.db.session import get_db
from app.main import app
from app.models.ai_hub import ModelCallLog
from app.models.foundation import FoundationFact
from app.models.market_data import DataSource, StockF10Cache, StockKline, StockNews, StockSymbol
from app.models.selection import SelectionRun
from app.models.stock_analyst import AiStockAnalysisRecord
from app.schemas.stock_analyst import AiStockAnalysisCreate, AiStockAnalystOutput
from app.services.stock_analyst import (
    _diverse_documents, _validate_result_references, build_stock_analyst_context, cancel_stock_analysis,
    execute_stock_analysis, queue_stock_analysis, recover_interrupted_analyses,
)


@pytest.fixture
def db():
    engine = create_engine("sqlite://", poolclass=StaticPool, connect_args={"check_same_thread": False})
    Base.metadata.create_all(engine)
    with sessionmaker(bind=engine, expire_on_commit=False)() as session:
        source = DataSource(source_code="ANALYST_TEST", source_name="测试数据源", adapter_type="MOCK")
        session.add(source)
        session.flush()
        session.add_all([
            StockSymbol(market="HK", symbol="02533", name="黑芝麻智能", exchange="HKEX", source_id=source.id),
            StockSymbol(market="CN_A", symbol="02533", name="同代码隔离样本", exchange="SSE", source_id=source.id),
        ])
        today = datetime.now(timezone.utc).date()
        for index in range(3):
            day = (today - timedelta(days=index)).isoformat()
            session.add(StockKline(market="HK", symbol="02533", trade_date=day, close_price=10 + index,
                                   volume=100, adjust="qfq", source_id=source.id))
        session.add(StockKline(market="HK", symbol="02533", trade_date=today.isoformat(),
                               close_price=500, adjust="", source_id=source.id))
        session.add_all([
            StockNews(market="HK", symbol="02533", news_time=today.isoformat(), title="主营增长与新订单",
                      content="主营收入增长，新增订单尚需观察回款。", source_id=source.id),
            StockNews(market="HK", symbol="02533", news_time=today.isoformat() + " 09:00:00",
                      title="主营增长与新订单", content="重复标题", source_id=source.id),
            StockNews(market="HK", symbol="02533", news_time=(today - timedelta(days=1)).isoformat(),
                      title="投资者关注利润持续性", content="收入增长与亏损并存。", source_id=source.id),
            StockNews(market="CN_A", symbol="02533", news_time=today.isoformat(), title="不能串入港股分析",
                      content="其他市场数据", source_id=source.id),
            StockF10Cache(market="HK", symbol="02533", section="holders", source_id=source.id,
                          payload_json={"major": [{"name": "股东甲", "ratio": 0.2}],
                                        "original_source_response": {"html": "RAW_ARCHIVE" * 50000}}),
        ])
        session.commit()
        yield session
    engine.dispose()


def payload():
    return AiStockAnalysisCreate(market="HK", symbol="02533", model_instance_code="REAL_TEST")


def test_same_title_disclosures_on_different_days_remain_distinct():
    from types import SimpleNamespace
    rows = [SimpleNamespace(id=index, title="回购股份", notice_date=day)
            for index, day in [(1, "2026-11-06"), (2, "2026-11-06"), (3, "2026-11-05")]]
    assert [row.id for row in _diverse_documents(rows)] == [1, 3]


def output(evidence_id):
    return {
        "executive_summary": "主营收入增长但盈利尚未兑现，短期观察资金与订单兑现。",
        "current_assessment": "收入、利润及股价表现需要综合理解，不能仅凭单条利好判断。",
        "overall_direction": "SIDEWAYS", "overall_confidence": 0.6,
        "scenarios": [{
            "horizon": horizon, "horizon_label": label, "direction": "SIDEWAYS", "confidence": 0.6,
            "thesis": "若主营收入持续增长且亏损收窄，可能改善预期。", "conditions": ["订单回款"],
            "invalidation_conditions": ["亏损扩大"], "evidence": [{"evidence_id": evidence_id, "claim": "主营收入增长"}],
        } for horizon, label in [("SHORT", "短期"), ("MEDIUM", "中期"), ("LONG", "长期")]],
        "evidence_chain": [{"evidence_id": evidence_id, "claim": "主营收入增长"}],
        "disclaimer": "条件化研究判断，不构成收益保证。",
    }


def log_result(db, result=None, *, status="SUCCESS"):
    log = ModelCallLog(task_type="stock_analysis", instance_code="REAL_TEST", status=status,
                       response_text=json.dumps(result, ensure_ascii=False) if result else None,
                       error_message="余额不足，请充值或更换模型" if status != "SUCCESS" else None,
                       request_json={"metadata_json": {"skill_chain": [{
                           "skill_code": "SENIOR_STOCK_TREND_ANALYST", "skill_version": "1.2.3",
                       }]}})
    db.add(log)
    db.commit()
    return log


def test_context_market_isolation_adjustment_dedup_and_archive_budget(db):
    _, context, preview = build_stock_analyst_context(db, market="HK", symbol="02533", period_months=12)
    serialized = json.dumps(context, ensure_ascii=False)
    assert len(serialized) < 42000
    assert "不能串入港股分析" not in serialized
    assert "RAW_ARCHIVE" not in serialized
    assert preview["counts"]["NEWS"] == 2
    assert preview["total_counts"]["NEWS"] == 3
    market = next(row for row in context["evidence"] if row["category"] == "MARKET")
    assert market["data"]["adjustment"] == "qfq"
    assert market["data"]["latest_close"] == 10
    assert db.scalar(select(func.count()).select_from(AiStockAnalysisRecord)) == 0


def test_success_keeps_exact_snapshot_and_does_not_write_old_selection_or_facts(db):
    run = queue_stock_analysis(db, payload())
    snapshot = run.evidence_snapshot_json
    valid = output(snapshot["evidence_allowlist"][0])
    with patch("app.services.stock_analyst.ModelHubService.chat", return_value=log_result(db, valid)) as call:
        saved = execute_stock_analysis(db, run.id)
        assert saved.status == "COMPLETED"
        assert saved.skill_version == "1.2.3"
        assert saved.evidence_snapshot_json == snapshot
        assert {row["horizon"] for row in saved.result_json["scenarios"]} == {"SHORT", "MEDIUM", "LONG"}
        assert call.call_args.kwargs["metadata_json"]["require_real_model"] is True
        assert call.call_args.kwargs["metadata_json"]["strict_instance"] is True
        execute_stock_analysis(db, run.id)
        assert call.call_count == 1
    assert db.scalar(select(func.count()).select_from(SelectionRun)) == 0
    assert db.scalar(select(func.count()).select_from(FoundationFact)) == 0


@pytest.mark.parametrize("mode", ["provider_failure", "malformed", "missing_horizon"])
def test_failures_never_publish_a_fallback_conclusion(db, mode):
    run = queue_stock_analysis(db, payload())
    result = output(run.evidence_snapshot_json["evidence_allowlist"][0])
    if mode == "missing_horizon":
        result["scenarios"][2]["horizon"] = "SHORT"
    log = log_result(db, result, status="FAILED" if mode == "provider_failure" else "SUCCESS")
    if mode == "malformed":
        log.response_text = "模型没有遵守输出契约"
        db.commit()
    with patch("app.services.stock_analyst.ModelHubService.chat", return_value=log):
        saved = execute_stock_analysis(db, run.id)
    assert saved.status == "FAILED"
    assert saved.result_json == {}
    assert saved.model_call_log_id == log.id
    assert saved.error_message
    assert saved.progress_json["last_stage"] == ("MODEL" if mode == "provider_failure" else "VALIDATING")


def test_unknown_citations_removed_and_confidence_limited():
    result, warnings = _validate_result_references(output("unknown:123"), {"evidence_allowlist": ["news:1"]})
    assert warnings
    assert result["evidence_chain"] == []
    assert result["overall_confidence"] <= 0.3
    assert all(row["confidence"] <= 0.25 and not row["evidence"] for row in result["scenarios"])


def test_cancel_before_start_and_during_call_wins(db):
    run = queue_stock_analysis(db, payload())
    cancel_stock_analysis(db, run)
    with patch("app.services.stock_analyst.ModelHubService.chat") as chat:
        assert execute_stock_analysis(db, run.id).status == "CANCELLED"
        chat.assert_not_called()
    second = queue_stock_analysis(db, payload())
    def cancel_during_call(**kwargs):
        cancel_stock_analysis(db, second)
        return log_result(db, output(second.evidence_snapshot_json["evidence_allowlist"][0]))
    with patch("app.services.stock_analyst.ModelHubService.chat", side_effect=cancel_during_call):
        saved = execute_stock_analysis(db, second.id)
    assert saved.status == "CANCELLED"
    assert saved.result_json == {}
    assert saved.model_call_log_id


def test_restart_recovery_retains_snapshot_and_marks_interrupted(db):
    run = queue_stock_analysis(db, payload())
    snapshot = run.evidence_snapshot_json
    assert recover_interrupted_analyses(db) == 1
    db.refresh(run)
    assert run.status == "FAILED"
    assert "重启" in run.error_message
    assert run.evidence_snapshot_json == snapshot
    assert recover_interrupted_analyses(db) == 0


def test_api_acknowledges_before_model_and_evidence_is_paginated(db):
    app.dependency_overrides[get_db] = lambda: db
    try:
        with patch("app.api.stock_analyst.run_stock_analysis_background") as worker:
            client = TestClient(app)
            response = client.post("/api/v1/ai-stock-analyst/analyses", json=payload().model_dump())
            assert response.status_code == 202
            created = response.json()
            assert created["status"] == "QUEUED"
            worker.assert_called_once_with(created["id"])
        evidence = client.get(f"/api/v1/ai-stock-analyst/analyses/{created['id']}/evidence?offset=1&limit=1").json()
        assert len(evidence["items"]) == 1 and evidence["total"] >= 3
        assert client.get("/api/v1/ai-stock-analyst/analyses/999999").status_code == 404
        assert client.post("/api/v1/ai-stock-analyst/analyses", json={**payload().model_dump(), "period_months": 0}).status_code == 422
    finally:
        app.dependency_overrides.pop(get_db, None)
