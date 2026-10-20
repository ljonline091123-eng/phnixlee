"""Functional replay with 20 actual price series, in an isolated in-memory DB.

This is NOT an investment backtest: current model outputs are only fixtures for
the operational workflow. No production approval, live Skill change or model
call is made. MOCK on the isolated run disables external market fetching; the
prices themselves are copied unchanged from the saved source bars.
"""
from __future__ import annotations

import argparse
from copy import deepcopy
from datetime import datetime, timezone
import json
from unittest.mock import patch

from sqlalchemy import create_engine, func, select
from sqlalchemy.orm import Session

import app.models  # Register the existing schema, without a production migration.
from app.db.base import Base
from app.db.session import SessionLocal
from app.models.ai_hub import ModelSkill, PredictionLedger, SkillOptimizationDraft
from app.models.decision_review import SelectionDecisionSnapshot, SelectionRetrospective, SkillEvaluationCase
from app.models.market_data import DataSource, StockKline
from app.models.selection import SelectionCandidate, SelectionRun, SelectionSnapshot, SelectionTracking
from app.schemas.selection import SelectionReviewRequest
from app.services.selection import refresh_tracking, review_candidate
from app.services.selection_audit import payload_hash
from app.services.skill_evaluation import run_retrospective_repair_flow
from scripts.verify_twenty_stock_chain import OUTPUT, SYMBOLS


def local_close(day):
    # 16:00 Asia/Shanghai; that day's A-share session is completed.
    return datetime.fromisoformat(day).replace(hour=8, tzinfo=timezone.utc)


def replay(run_id):
    with SessionLocal() as source_db:
        original_run = source_db.get(SelectionRun, run_id)
        assert original_run and original_run.analysis_mode == "MODEL_COMPLETED"
        originals = source_db.scalars(select(SelectionCandidate).where(SelectionCandidate.run_id == run_id)).all()
        assert {row.symbol for row in originals} == set(SYMBOLS)
        skill = source_db.scalar(select(ModelSkill).where(ModelSkill.skill_code == "STOCK_SELECTION_ANALYST"))
        assert skill is not None
        skill_values = {key: deepcopy(getattr(skill, key)) for key in
                        ("skill_code", "skill_name", "version", "instructions", "config_json", "enabled")}
        fixtures = []
        for candidate in originals:
            saved = source_db.scalars(select(StockKline).where(
                StockKline.market == candidate.market, StockKline.symbol == candidate.symbol,
                StockKline.period == "daily", StockKline.adjust == "",
                StockKline.trade_date <= datetime.now(timezone.utc).date().isoformat(),
            ).order_by(StockKline.trade_date)).all()
            bars = [row for row in saved if row.close_price and row.close_price > 0
                    and (row.volume is None or row.volume > 0)
                    and datetime.fromisoformat(row.trade_date).weekday() < 5][-11:]
            assert len(bars) == 11, f"Not enough actual historical bars: {candidate.symbol}"
            fixtures.append({"symbol": candidate.symbol, "name": candidate.name,
                "analysis": deepcopy(candidate.analysis_json), "evidence": deepcopy(candidate.evidence_json),
                "bars": [{key: getattr(bar, key) for key in
                          ("trade_date", "close_price", "volume", "raw_payload", "fetched_at")} for bar in bars]})

    engine = create_engine("sqlite://")
    Base.metadata.create_all(engine)
    rows = []
    with Session(engine) as db:
        local_skill = ModelSkill(**skill_values)
        source = DataSource(source_code="SAVED_PRICE_REPLAY", source_name="已存真实行情功能回放", adapter_type="ISOLATED_REPLAY")
        run = SelectionRun(market="CN_A", universe_scope="ISOLATED_FUNCTIONAL_REPLAY",
            candidate_count=20, data_mode="MOCK", analysis_mode="MODEL_COMPLETED")
        db.add_all([local_skill, source, run])
        db.flush()
        for fixture in fixtures:
            first = fixture["bars"][0]
            candidate = SelectionCandidate(run_id=run.id, market="CN_A", symbol=fixture["symbol"],
                name=fixture["name"], entry_price=first["close_price"], entry_date=first["trade_date"],
                analysis_json=fixture["analysis"], evidence_json=fixture["evidence"])
            db.add(candidate)
            db.flush()
            for bar in fixture["bars"]:
                db.add(StockKline(market="CN_A", symbol=fixture["symbol"], source_id=source.id,
                    period="daily", adjust="", **bar))
            db.commit()
            with patch("app.services.selection._now", return_value=local_close(first["trade_date"])):
                review_candidate(db, candidate.id, SelectionReviewRequest(decision="APPROVED",
                    target_price=first["close_price"] * 1.05, stop_price=first["close_price"] * 0.95,
                    notes="隔离功能回放；目标价是测试参数，不代表历史预测。"))
            fixture["candidate_id"] = candidate.id

        for fixture in fixtures:
            tracking = db.scalar(select(SelectionTracking).where(SelectionTracking.candidate_id == fixture["candidate_id"]))
            # Preserve an in-progress revision, then complete the first 10
            # actual sessions and prove repeated refreshes do not duplicate it.
            with patch("app.services.selection._now", return_value=local_close(fixture["bars"][3]["trade_date"])):
                refresh_tracking(db, [tracking.id])
            assert tracking.observed_sessions == 3 and tracking.status == "TRACKING"
            with patch("app.services.selection._now", return_value=local_close(fixture["bars"][-1]["trade_date"])):
                refresh_tracking(db, [tracking.id])
                refresh_tracking(db, [tracking.id])
            assert tracking.observed_sessions == 10 and tracking.status == "COMPLETED"
            candidate = db.get(SelectionCandidate, fixture["candidate_id"])
            ledger = db.get(PredictionLedger, candidate.prediction_id)
            assert ledger and ledger.status == "EVALUATED"
            assert ledger.actual_price == fixture["bars"][-1]["close_price"]
            snapshots = db.scalars(select(SelectionSnapshot).where(SelectionSnapshot.tracking_id == tracking.id)
                                   .order_by(SelectionSnapshot.sequence)).all()
            assert [row.trade_date for row in snapshots] == [bar["trade_date"] for bar in fixture["bars"][1:]]
            retrospectives = db.scalars(select(SelectionRetrospective).where(SelectionRetrospective.tracking_id == tracking.id)
                                       .order_by(SelectionRetrospective.revision)).all()
            assert [row.observed_sessions for row in retrospectives] == [3, 10]
            first = run_retrospective_repair_flow(db, retrospectives[-1].id, request_repair_draft=False)
            second = run_retrospective_repair_flow(db, retrospectives[-1].id, request_repair_draft=False)
            assert first["case_created"] is True and second["case_created"] is False
            assert first["case_id"] == second["case_id"]
            assert first["evaluation_status"] in {"PASSED", "FAILED"}
            assert first["evaluation_source"] == "PERSISTED_MODEL_OUTPUT"
            assert first["repair_status"] == "NOT_REQUESTED"
            rows.append({"symbol": fixture["symbol"], "name": fixture["name"], "passed": True,
                "entry_date": fixture["bars"][0]["trade_date"], "last_date": fixture["bars"][-1]["trade_date"],
                "tracked_sessions": 10, "retrospective_revisions": 2, "regression_case_created": True,
                "regression_scoring": first["evaluation_status"]})
        for snapshot in db.scalars(select(SelectionDecisionSnapshot)):
            assert snapshot.payload_hash == payload_hash({"payload": snapshot.payload_json, "evidence": snapshot.evidence_json})
        assert run.status == "COMPLETED"
        assert db.scalar(select(func.count(SelectionSnapshot.id))) == 200
        assert db.scalar(select(func.count(SelectionRetrospective.id))) == 40
        assert db.scalar(select(func.count(SkillEvaluationCase.id))) == 20
        assert db.scalar(select(func.count(SkillOptimizationDraft.id))) == 0
        assert local_skill.version == skill_values["version"] and local_skill.instructions == skill_values["instructions"]
    engine.dispose()
    report = {"passed": True, "source_run_id": run_id, "scope": "ISOLATED_FUNCTIONAL_REPLAY",
        "not_an_investment_backtest": True, "production_writes": 0, "model_calls": 0,
        "completed_tracking": 20, "price_snapshots": 200, "retrospective_revisions": 40,
        "skill_regression_cases": 20, "live_skill_changes": 0,
        "limits": ["当前模型输出只用于测试流程，不能用于预测历史行情或计算策略有效性。",
                   "MOCK仅用于禁止回放时联网；行情取自正式库已存日线，未生成模拟价格。",
                   "回归PASSED/FAILED为规则评分结果，非工作流执行失败；自动优化未发布。"], "stocks": rows}
    OUTPUT.mkdir(parents=True, exist_ok=True)
    (OUTPUT / "tracking-replay.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=True), flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--run-id", type=int, required=True)
    replay(parser.parse_args().run_id)
