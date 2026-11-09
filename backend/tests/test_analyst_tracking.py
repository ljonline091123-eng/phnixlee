"""Forward-only forecast contracts; isolated SQLite and no paid model calls."""
from copy import deepcopy
from datetime import datetime, timedelta, timezone
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import func, select

from tests.test_stock_analyst import db, payload
from tests.test_analyst_followup import analysis, followup_log
from app.db.session import get_db
from app.main import app
from app.models.ai_hub import PredictionLedger
from app.models.market_data import StockKline
from app.models.stock_analyst import AiAnalystWatch, AiAnalystDailyReview, AiAnalystForecastCheck
from app.schemas.stock_analyst import AnalystWatchConfig
from app.services import analyst_tracking as tracking, analyst_followup as followup
from app.services.stock_analyst import queue_stock_analysis


def origin(db, analysis):
    watch = AiAnalystWatch(analysis_id=analysis.id, config_json=AnalystWatchConfig().model_dump())
    db.add(watch)
    db.flush()
    day = datetime.now(tracking.ZONE).date().isoformat()
    row = AiAnalystDailyReview(watch_id=watch.id, business_date=day, status="COMPLETED",
        model_instance_code="REAL_TEST", skill_version="2.3.4", config_snapshot_json={"config_version": 3},
        evidence_snapshot_json={"tracked_observations": [{"observation_id": "O1", "text": "观察订单兑现"}],
            "evidence": [{"evidence_id": "market-watch:1", "data": {"fresh": True, "close_price": 100, "trade_date": day}}]},
        conditions_json=[{"kind": "event", "label": "订单兑现", "status": "HIT", "evidence_ids": ["notice:1"]}])
    db.add(row)
    db.commit()
    return row


def proposal(**updates):
    return {"direction": "BULLISH", "target_price": 110, "invalidation_price": 90, "horizon_sessions": 3,
        "confidence": .6, "trigger_labels": ["订单兑现"], "thesis": "订单兑现可能改善收入预期，仍需持续验证。",
        "pricing_basis": "以近期原币收盘价与阻力区间推导目标，成交量不足时可能失效。",
        "logic_chain": ["新订单兑现改善收入可见度", "收入改善可能带动价格趋势"],
        "invalidation_conditions": ["订单取消或收盘价触及失效价"],
        "evidence": [{"evidence_id": "notice:1", "claim": "订单已披露"}, {"evidence_id": "market-watch:1", "claim": "近期收盘价"}], **updates}


def register(db, analysis, row, **updates):
    ids, warnings = tracking.persist_forecasts(db, analysis, row, {"forecasts": [proposal(**updates)]})
    db.commit()
    return ids, warnings


def history(db, row, prices, *, publish_day="2026-01-05", direction="BULLISH"):
    ledger = db.scalar(select(PredictionLedger).where(PredictionLedger.skill_code == "STOCK_ANALYST_FOLLOWUP"))
    ledger.predicted_at = datetime.fromisoformat(publish_day + "T04:00:00+00:00")
    # Publishing-day, adjusted, other-market, and future bars must never pass this forecast.
    bars = [(publish_day, 200 if direction == "BULLISH" else 1, "", "HK")]
    for i, price in enumerate(prices, 1):
        day = (datetime.fromisoformat(publish_day) + timedelta(days=i)).date().isoformat()
        bars.append((day, price, "", "HK"))
        bars.append((day, 200 if direction == "BULLISH" else 1, "qfq", "HK"))
        bars.append((day, 200 if direction == "BULLISH" else 1, "", "CN_A"))
    source_id = db.scalar(select(StockKline.source_id))
    for day, price, adjust, market in bars:
        db.add(StockKline(market=market, symbol="02533", trade_date=day, adjust=adjust,
                         close_price=price, source_id=source_id))
    review = AiAnalystDailyReview(watch_id=row.watch_id, business_date="2026-01-09", attempt=1)
    db.add(review)
    db.commit()
    return ledger, review


@pytest.mark.parametrize("updates,reason", [
    ({"target_price": 95}, "方向"), ({"invalidation_price": 101}, "方向"),
    ({"trigger_labels": ["不存在的信号"]}, "命中"),
    ({"evidence": [{"evidence_id": "notice:1", "claim": "缺少行情"}]}, "行情"),
    ({"evidence": [{"evidence_id": "market-watch:1", "claim": "缺少事件"}]}, "命中证据"),
])
def test_forecast_evidence_and_direction_gates(db, analysis, updates, reason):
    ids, warnings = register(db, analysis, origin(db, analysis), **updates)
    assert not ids and reason in warnings[0]
    assert db.scalar(select(func.count()).select_from(PredictionLedger)) == 0


@pytest.mark.parametrize("fault", ["stale", "no_hit", "future"])
def test_forecast_not_published_without_current_signal_and_price(db, analysis, fault):
    row = origin(db, analysis)
    if fault == "stale":
        row.evidence_snapshot_json["evidence"][0]["data"]["fresh"] = False
    elif fault == "no_hit":
        row.conditions_json = []
    else:
        row.business_date = "2099-01-01"
    ids, warnings = register(db, analysis, row)
    assert not ids and warnings


def test_event_trigger_cannot_fall_back_to_unrelated_market_evidence(db, analysis):
    row = origin(db, analysis)
    row.conditions_json = [{"kind": "event", "label": "订单兑现", "status": "HIT"}]
    ids, warnings = register(db, analysis, row,
        evidence=[{"evidence_id": "market-watch:1", "claim": "只有行情，缺少命中事件"}])
    assert not ids and "命中证据" in warnings[0]


@pytest.mark.parametrize("direction,prices,expected,updates", [
    ("BULLISH", [102, 111, 90], "PASSED", {}),
    ("BULLISH", [89, 111, 112], "INVALIDATED", {}),
    ("BULLISH", [101, 105, 109], "FAILED", {}),
    ("BEARISH", [98, 89, 105], "PASSED", {"target_price": 90, "invalidation_price": 110}),
    ("BEARISH", [111, 89, 85], "INVALIDATED", {"target_price": 90, "invalidation_price": 110}),
    ("BULLISH", [None, 111, 112], "MISSING", {}),
])
def test_forward_only_prices_first_hit_expiry_and_missing(db, analysis, direction, prices, expected, updates):
    row = origin(db, analysis)
    ids, warnings = register(db, analysis, row, direction=direction, **updates)
    assert len(ids) == 1 and not warnings
    ledger, review = history(db, row, prices, direction=direction)
    result = tracking.validate_forecasts(db, analysis, review)
    assert result[0]["state"] == expected
    assert all(r["date"] > "2026-01-05" for r in result[0]["bar_evidence"])
    assert ledger.status == ("ANALYST_PENDING" if expected == "MISSING" else "ANALYST_" + expected)
    assert result[0]["learning_status"] == ("PENDING" if expected == "MISSING" else "CANDIDATE")
    tracking.validate_forecasts(db, analysis, review)
    assert db.scalar(select(func.count()).select_from(AiAnalystForecastCheck)) == 1


def test_persistence_and_validation_are_idempotent_and_preserve_contract(db, analysis):
    row = origin(db, analysis)
    ids, _ = register(db, analysis, row)
    assert register(db, analysis, row)[0] == ids
    ledger, review = history(db, row, [105, 111, 115])
    snapshot = deepcopy(tracking.tracking_page(db, analysis.id)["items"][0]["contract"])
    tracking.validate_forecasts(db, analysis, review)
    other = AiAnalystDailyReview(watch_id=row.watch_id, business_date="2026-01-10")
    db.add(other); db.commit()
    assert tracking.validate_forecasts(db, analysis, other) == []
    page = tracking.tracking_page(db, analysis.id, 0, 1)
    assert page["items"][0]["contract"] == snapshot
    assert page["stats"] == {"PASSED": 1} and page["total"] == 1
    assert ledger.evaluation_json["review_id"] == review.id


def test_unfinished_daily_candle_cannot_validate_target(db, analysis):
    row = origin(db, analysis)
    register(db, analysis, row)
    ledger, review = history(db, row, [111])
    class BeforeClose(datetime):
        @classmethod
        def now(cls, tz=None):
            instant = datetime(2026, 1, 6, 10, tzinfo=tracking.ZONE)
            return instant.astimezone(tz) if tz else instant.replace(tzinfo=None)
    with patch.object(tracking, "datetime", BeforeClose):
        result = tracking.validate_forecasts(db, analysis, review)
    assert result[0]["state"] == "MISSING" and result[0]["sessions_observed"] == 0
    assert ledger.status == "ANALYST_PENDING"


def test_learning_reused_across_reports_and_isolated_by_market(db, analysis):
    row = origin(db, analysis)
    register(db, analysis, row)
    ledger, review = history(db, row, [101, 102, 103])
    tracking.validate_forecasts(db, analysis, review)
    assert ledger.status == "ANALYST_FAILED"
    later = queue_stock_analysis(db, payload())
    memory = later.evidence_snapshot_json["validated_learning"]
    assert memory[0]["status"] == "FAILED" and memory[0]["analysis_id"] == analysis.id
    assert memory[0]["learning_status"] == "CANDIDATE"
    assert tracking.learning_context(db, market="CN_A", symbol="02533") == []


def test_observation_hit_cannot_reuse_old_news_or_unknown_ids():
    result = {"observation_checks": [{"observation_id": "O1", "status": "HIT", "evidence": [{"evidence_id": "news:1"}]}]}
    ctx = {"evidence_allowlist": ["news:1"], "monitor_window": {"since": "2026-01-05", "through": "2026-01-09"},
           "evidence": [{"evidence_id": "news:1", "category": "NEWS", "observed_at": "2026-01-01"}]}
    items = [{"observation_id": "O1", "text": "订单兑现"}]
    with pytest.raises(ValueError, match="旧资料"):
        tracking.validate_daily_contract(result, ctx, items)
    ctx["evidence"][0]["observed_at"] = "2026-01-08"
    assert not tracking.validate_daily_contract(result, ctx, items)
    assert result["observation_checks"][0]["observation_text"] == "订单兑现"
    result["observation_checks"].append(deepcopy(result["observation_checks"][0]))
    with pytest.raises(ValueError, match="重复"):
        tracking.validate_daily_contract(result, ctx, items)


@pytest.mark.parametrize("repaired", [True, False])
def test_daily_review_repairs_stale_hit_before_publishing_forecast(db, analysis, repaired):
    from tests.test_analyst_followup import daily_output
    analysis.result_json = {**analysis.result_json, "watch_items": ["观察订单兑现"]}
    row = origin(db, analysis)
    row.status, row.conditions_json = "QUEUED", []
    row.config_snapshot_json = {**AnalystWatchConfig().model_dump(), "config_version": 3}
    db.commit()
    day = datetime.now(tracking.ZONE).date().isoformat()
    fresh = [{"evidence_id": "market-watch:1", "data": {"fresh": True, "close_price": 500, "trade_date": day}},
             {"evidence_id": "news:1", "category": "NEWS", "observed_at": day},
             {"evidence_id": "notice:old", "category": "NOTICE", "observed_at": "2000-01-01"}]
    first = {**daily_output(analysis), "observation_checks": [{"observation_id": "O1", "status": "HIT",
        "explanation": "旧合同仍然有效，但不应当作本次新命中。",
        "evidence": [{"evidence_id": "notice:old", "claim": "早期合同"}]}]}
    good = {**daily_output(analysis), "observation_checks": [{"observation_id": "O1", "status": "HIT",
        "explanation": "本次新披露订单进展，可以继续核验收入兑现。",
        "evidence": [{"evidence_id": "news:1", "claim": "当前订单进展"}]}],
        "forecasts": [proposal(target_price=550, invalidation_price=450, trigger_labels=["观察订单兑现"],
            evidence=[{"evidence_id": "news:1", "claim": "当前订单进展"},
                      {"evidence_id": "market-watch:1", "claim": "最新原币收盘价"}])]}
    first_log, next_log = followup_log(db, first), followup_log(db, good if repaired else first)
    original = deepcopy(analysis.result_json)
    with patch.object(followup, "refresh_monitor_data", return_value=[]), \
         patch.object(followup, "evaluate_conditions", return_value=([], fresh)), \
         patch.object(followup.ModelHubService, "chat", side_effect=[first_log, next_log]) as call:
        outcome = followup.run_daily_review(db, row.id)
    assert call.call_count == 2
    assert "旧资料" in call.call_args.kwargs["metadata_json"]["previous_output_error"]
    assert "monitor_window" in call.call_args.kwargs["messages"][0]["content"]
    assert row.model_call_log_id == next_log.id and analysis.result_json == original
    assert outcome["status"] == ("COMPLETED" if repaired else "FAILED")
    forecasts = tracking.tracking_page(db, analysis.id)
    assert forecasts["total"] == (1 if repaired else 0)
    if repaired:
        assert row.result_json["forecast_ids"] == [forecasts["items"][0]["id"]]
        assert forecasts["items"][0]["contract"]["pricing_basis"] == proposal()["pricing_basis"]
    else:
        assert not row.result_json and "证据校验" in row.error_message


def test_price_validation_survives_failed_model_and_keeps_original_report(db, analysis):
    original = deepcopy(analysis.result_json)
    row = origin(db, analysis)
    register(db, analysis, row)
    ledger, review = history(db, row, [111, 112, 113])
    review.config_snapshot_json = {**AnalystWatchConfig().model_dump(), "config_version": 3}
    review.status = "QUEUED"
    db.commit()
    with patch.object(followup, "refresh_monitor_data", return_value=[]), patch.object(followup.ModelHubService, "chat", return_value=followup_log(db, {}, status="FAILED")):
        outcome = followup.run_daily_review(db, review.id)
    assert outcome["status"] == "FAILED"
    assert ledger.status == "ANALYST_PASSED"
    assert review.progress_json["forecast_validations"][0]["state"] == "PASSED"
    assert analysis.result_json == original and review.result_json == {}


def test_tracking_api_empty_pagination_and_ownership(db, analysis):
    app.dependency_overrides[get_db] = lambda: db
    try:
        client = TestClient(app)
        assert client.get(f"/api/v1/ai-stock-analyst/analyses/{analysis.id}/tracking").json()["total"] == 0
        assert client.get(f"/api/v1/ai-stock-analyst/analyses/{analysis.id}/tracking?offset=-1").status_code == 422
        assert client.get("/api/v1/ai-stock-analyst/analyses/999999/tracking").status_code == 404
    finally:
        app.dependency_overrides.clear()
