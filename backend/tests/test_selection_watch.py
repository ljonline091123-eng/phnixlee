from datetime import datetime, timedelta, timezone
from types import SimpleNamespace
from unittest.mock import patch
import json

import pytest
from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session

from app.db.base import Base
from app.models.ai_hub import PredictionLedger
from app.models.market_data import DataSource, StockKline, StockSymbol
from app.models.selection import SelectionCandidate, SelectionSnapshot, SelectionTracking
from app.schemas.selection import SelectionReviewRequest, SelectionRunCreate
from app.services.selection import _candidate_payload, _last_completed_trade_date, create_selection_run, refresh_tracking, review_candidate
from app.models.selection import SelectionRun
from app.services.prediction_review import review_predictions


# 18:00 local Shanghai time: the 6 Jan CN_A session is complete.
NOW = datetime(2025, 1, 6, 10, tzinfo=timezone.utc)


def test_market_cutoff_uses_local_date_and_market_close():
    # Shanghai midnight is still the previous completed CN_A session; a same-day
    # bar cannot enter a T+10 window before the close.
    assert _last_completed_trade_date("CN_A", datetime(2025, 1, 6, 16, 20, tzinfo=timezone.utc)) == "2025-01-06"
    assert _last_completed_trade_date("CN_A", datetime(2025, 1, 6, 7, 0, tzinfo=timezone.utc)) == "2025-01-05"
    # Hong Kong's later close keeps a 15:30 local bar incomplete.
    assert _last_completed_trade_date("HK", datetime(2025, 1, 6, 7, 30, tzinfo=timezone.utc)) == "2025-01-05"
    assert _last_completed_trade_date("HK", datetime(2025, 1, 6, 8, 30, tzinfo=timezone.utc)) == "2025-01-06"


def test_screening_never_divides_quote_shares_by_history_lots():
    run = SelectionRun(market="CN_A", criteria_json={"min_abs_change_pct": 0}, analysis_mode="LOCAL_EVIDENCE")
    quote = SimpleNamespace(symbol="000006", current_price=10, previous_close_price=10,
        change_pct=0, volume=1000000, quote_time="2026-10-15", fetched_at=NOW,
        raw_payload={"derived": {"volume_ratio": 0.72}})
    bars = [StockKline(source_id=1, trade_date="2026-10-08", volume=12000, close_price=10,
                       raw_payload={"source_method": "tx", "volume_unit": "unknown"}),
            StockKline(source_id=1, trade_date="2026-10-07", volume=10000, close_price=10,
                       raw_payload={"source_method": "tx", "volume_unit": "unknown"})]
    item, _ = _candidate_payload(None, run, quote, bars)
    assert item["hard_rules_json"]["volume_ratio"] == 0.72
    assert item["hard_rules_json"]["volume_ratio_date"] == "2026-10-15"
    assert item["hard_rules_json"]["volume_ratio_basis"] == "SOURCE_QUOTE_RATIO"
    quote.raw_payload = {}
    bars.append(StockKline(source_id=2, trade_date="2026-10-06", volume=1000000, close_price=10,
                          raw_payload={"source_method": "another", "volume_unit": "shares"}))
    item, _ = _candidate_payload(None, run, quote, bars)
    assert item["hard_rules_json"]["volume_ratio"] == 1.2
    assert item["hard_rules_json"]["volume_ratio_date"] == "2026-10-08"
    assert item["hard_rules_json"]["price_observed_date"] == "2026-10-15"


@pytest.fixture
def db():
    engine = create_engine("sqlite://")
    Base.metadata.create_all(engine)
    with Session(engine) as session:
        source = DataSource(source_code="DEMO", source_name="Demo", adapter_type="MOCK")
        session.add(source)
        session.flush()
        session.info["source_id"] = source.id
        yield session
    engine.dispose()


def stock(db, symbol="DEMO001"):
    db.add(StockSymbol(market="CN_A", symbol=symbol, name=symbol, exchange="DEMO", status="LISTED", source_id=db.info["source_id"]))


def bar(db, day, close, symbol="DEMO001", adjust="", volume=100, source_id=None):
    db.add(StockKline(market="CN_A", symbol=symbol, period="daily", adjust=adjust,
        trade_date=day, close_price=close, open_price=close, volume=volume,
        source_id=source_id or db.info["source_id"], fetched_at=NOW))


def candidate(db, use_model=False):
    stock(db)
    bar(db, "2025-01-03", 10)
    bar(db, "2025-01-06", 11)
    db.commit()
    with patch("app.services.selection._now", return_value=NOW):
        run = create_selection_run(db, SelectionRunCreate(symbols=["DEMO001"], use_model=use_model, data_mode="MOCK"))
    return run, db.scalar(select(SelectionCandidate).where(SelectionCandidate.run_id == run.id))


def approve(db):
    run, item = candidate(db)
    with patch("app.services.selection._now", return_value=NOW):
        review_candidate(db, item.id, SelectionReviewRequest(decision="APPROVED", target_price=12, stop_price=9, notes="Demo evidence only"))
    return item, db.scalar(select(SelectionTracking).where(SelectionTracking.candidate_id == item.id))


def test_universe_scan_is_not_silently_limited_to_first_500(db):
    for index in range(510):
        symbol = f"DEMO{index:04}"
        stock(db, symbol)
        bar(db, "2025-01-03", 10, symbol)
        bar(db, "2025-01-06", 12 if index == 509 else 10, symbol)
    db.commit()
    with patch("app.services.selection._now", return_value=NOW):
        run = create_selection_run(db, SelectionRunCreate(use_model=False, candidate_limit=1, min_volume_ratio=100))
    items = db.scalars(select(SelectionCandidate)).all()
    assert [item.symbol for item in items] == ["DEMO0509"]
    assert run.criteria_json["local_universe_count"] == 510


def test_auto_route_and_model_prediction_are_recorded(db):
    # Routing success requires enough actual observations for a valid scorecard.
    # Keep the short-history fixtures used by the other tracking tests unchanged.
    day = datetime(2025, 1, 2).date()
    history = []
    while len(history) < 20:
        if day.weekday() < 5:
            history.append(day.isoformat())
        day -= timedelta(days=1)
    for index, trade_date in enumerate(reversed(history)):
        bar(db, trade_date, 9 + index / 20)
    result = {"candidates": [{"symbol": "DEMO001", "decision": "WATCH", "confidence": .7,
        "reasoning": "Demo inference; limited evidence", "target_price": 12, "stop_price": 9,
        "horizon_sessions": 10, "evidence_document_ids": [], "dimensions": [
            {"dimension": key, "status": "INSUFFICIENT", "direction": "UNKNOWN", "summary": "Evidence unavailable"}
            for key in ("NEWS", "NOTICE", "FINANCIAL", "PRICE_VOLUME", "FUND_FLOW", "COMPANY", "GRAPH")],
        "business_quality": {"status": "INSUFFICIENT", "main_business_summary": "No business evidence",
            "products_services": [], "revenue_mix": [], "technology_level": "UNKNOWN",
            "technology_assessment": "No evidence", "competitive_moat": "No evidence",
            "commercialization": "No evidence", "market_position": "No evidence",
            "market_share": {"verification_status": "MISSING", "explanation": "No evidence",
                             "evidence_document_ids": [], "evidence_fact_ids": []},
            "direction": "UNKNOWN", "impact_score": None, "confidence": 0,
            "evidence_document_ids": [], "evidence_fact_ids": [], "risks": [], "watch_conditions": []},
        "value_chain_analysis": {"status": "INSUFFICIENT", "summary": "No verified relation",
                                 "impacts": [], "risks": [], "watch_conditions": []}}]}
    log = SimpleNamespace(id=99, status="SUCCESS", instance_code="DEMO_MODEL", response_text=json.dumps(result))

    def respond(**kwargs):
        content = kwargs["messages"][-1]["content"]
        context = json.loads(content[content.index("["):])[0]
        fact_id = context["evidence_json"]["citation_allowlist"]["market_fact_ids"][0]
        recommendation = result["candidates"][0]
        recommendation["evidence_fact_ids"] = [fact_id]
        price = next(row for row in recommendation["dimensions"] if row["dimension"] == "PRICE_VOLUME")
        price.update(status="SUPPORTED", direction="POSITIVE", summary="2025-01-06收盘11元，前收10元",
            evidence_fact_ids=[fact_id], evidence_assessments=[{"reference_type": "FACT", "reference_id": fact_id,
                "direction": "POSITIVE", "score": 25, "rationale": "实际收盘价格较前一交易日上涨"}])
        log.response_text = json.dumps(result)
        return log

    with patch("app.services.model_hub.ModelHubService.chat", side_effect=respond) as chat:
        run, item = candidate(db, use_model=True)
    assert chat.call_args.kwargs["instance_code"] is None
    assert run.analysis_mode == "MODEL_COMPLETED", (run.error_message, item.analysis_json)
    assert item.analysis_json["target_price"] == 12
    assert item.analysis_json["summary"] == "Demo inference; limited evidence"
    assert run.model_instance_code == "DEMO_MODEL"


@pytest.mark.parametrize("response", ['{}', '{"candidates": []}', 'not json'])
def test_invalid_model_output_falls_back_without_fabricated_prediction(db, response):
    log = SimpleNamespace(id=99, status="SUCCESS", instance_code="DEMO_MODEL", response_text=response)
    with patch("app.services.model_hub.ModelHubService.chat", return_value=log):
        run, item = candidate(db, use_model=True)
    assert run.analysis_mode == "MODEL_FAILED_FALLBACK_LOCAL"
    assert item.analysis_json["model_output_status"] == "FAILED"
    assert "target_price" not in item.analysis_json


def test_rejection_does_not_create_tracking_and_review_is_single_use(db):
    run, item = candidate(db)
    review_candidate(db, item.id, SelectionReviewRequest(decision="REJECTED"))
    assert db.scalar(select(SelectionTracking)) is None
    assert db.scalar(select(PredictionLedger)) is None
    with pytest.raises(ValueError, match="already been reviewed"):
        review_candidate(db, item.id, SelectionReviewRequest(decision="APPROVED", target_price=12))


def test_unverified_analysis_cannot_be_confirmed_or_create_prediction(db):
    run, item = candidate(db)
    item.analysis_json = {**item.analysis_json, "model_output_status": "SUCCESS", "analysis_validation_status": "PARTIAL"}
    db.commit()
    with pytest.raises(ValueError, match="分析引用尚未通过核验"):
        review_candidate(db, item.id, SelectionReviewRequest(decision="APPROVED", target_price=12))
    assert item.decision == "PENDING"
    assert run.reviewed_count == 0
    assert db.scalar(select(SelectionTracking)) is None
    assert db.scalar(select(PredictionLedger)) is None
    review_candidate(db, item.id, SelectionReviewRequest(decision="REJECTED"))
    assert item.decision == "REJECTED"


def test_ten_session_review_excludes_earlier_adjusted_future_and_duplicate_bars(db):
    item, tracking = approve(db)
    assert item.prediction_id is not None
    days = []
    day = NOW.date() + timedelta(days=1)
    while len(days) < 11:
        if day.weekday() < 5:
            days.append(day.isoformat())
        day += timedelta(days=1)
    for index, day in enumerate(days):
        bar(db, day, 11 + index / 10)
    bar(db, "2025-01-04", 99)  # before human confirmation, weekend
    bar(db, "2025-01-11", 99)  # weekend
    bar(db, "2025-01-08", 999, adjust="qfq")
    bar(db, "2099-01-01", 999)
    db.commit()
    with patch("app.services.selection._now", return_value=datetime(2025, 1, 16, tzinfo=timezone.utc)):
        refresh_tracking(db)
    # 16 Jan 08:00 local is before the close; that day's bar is excluded.
    assert tracking.observed_sessions == 7
    assert tracking.status == "TRACKING" and tracking.direction_hit is None
    # The generic reviewer must leave this prediction to the selection workflow.
    assert review_predictions(db)["checked"] == 0
    with patch("app.services.selection._now", return_value=datetime(2025, 1, 22, tzinfo=timezone.utc)):
        refresh_tracking(db)
        refresh_tracking(db)
    assert tracking.observed_sessions == 10
    assert tracking.latest_date == days[9]
    assert tracking.status == "COMPLETED"
    assert len(db.scalars(select(SelectionSnapshot)).all()) == 10
    ledger = db.get(PredictionLedger, item.prediction_id)
    assert ledger.status == "EVALUATED" and ledger.actual_price == tracking.latest_price
    assert ledger.evaluation_json["workflow"] == "selection_tracking"
    assert tracking.entry_price == 11 and tracking.target_price == 12


def test_existing_partial_bars_do_not_prevent_market_refresh(db):
    item, tracking = approve(db)
    tracking.data_mode = "REAL"
    bar(db, "2025-01-07", 11.2)
    db.commit()
    source = db.get(DataSource, db.info["source_id"])
    with patch("app.services.selection._now", return_value=datetime(2025, 1, 10, tzinfo=timezone.utc)), \
         patch("app.services.catalog.select_data_source", return_value=source), \
         patch("app.services.stock_on_demand.StockOnDemandService.fetch_kline") as fetch:
        refresh_tracking(db)
    fetch.assert_called_once()
    assert tracking.status == "TRACKING"


def test_negative_target_and_parent_run_close_after_ten_sessions(db):
    run, item = candidate(db)
    with patch("app.services.selection._now", return_value=NOW):
        review_candidate(db, item.id, SelectionReviewRequest(decision="APPROVED", target_return_pct=-5))
    tracking = db.scalar(select(SelectionTracking).where(SelectionTracking.candidate_id == item.id))
    day = NOW.date() + timedelta(days=1)
    sessions = []
    while len(sessions) < 10:
        if day.weekday() < 5:
            sessions.append(day.isoformat())
        day += timedelta(days=1)
    for index, trade_date in enumerate(sessions):
        bar(db, trade_date, 10.5 - index * 0.05)
    db.commit()
    with patch("app.services.selection._now", return_value=datetime(2025, 1, 22, 8, tzinfo=timezone.utc)):
        refresh_tracking(db)
    assert tracking.status == "COMPLETED"
    assert tracking.target_hit is True
    assert run.status == "COMPLETED"
