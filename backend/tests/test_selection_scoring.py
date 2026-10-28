from datetime import datetime, timezone

import pytest
from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session

from app.db.base import Base
from app.models.ai_hub import ModelSkill
from app.models.market_data import DataSource, StockKline, StockSymbol
from app.models.selection import SelectionCandidate, SelectionDimensionResult, SelectionRun
from app.schemas.selection import SelectionProfileUpdate
from app.services.model_hub import seed_default_skills
from app.services.selection_scoring import (
    clone_profile,
    publish_profile,
    refresh_candidate_evaluations,
    score_candidate,
    seed_selection_scoring,
    serialize_profile,
    update_profile,
)


@pytest.fixture
def db():
    engine = create_engine("sqlite://")
    Base.metadata.create_all(engine)
    with Session(engine) as session:
        seed_selection_scoring(session)
        source = DataSource(source_code="SCORE_TEST", source_name="Score Test", adapter_type="MOCK")
        session.add(source)
        session.flush()
        session.add(StockSymbol(market="CN_A", symbol="000001", name="评分样本", exchange="SZSE", source_id=source.id))
        run = SelectionRun(market="CN_A", candidate_limit=1, criteria_json={}, data_mode="REAL",
                           analysis_mode="LOCAL_EVIDENCE", as_of=datetime(2026, 1, 2, tzinfo=timezone.utc))
        session.add(run)
        session.flush()
        candidate = SelectionCandidate(
            run_id=run.id, market="CN_A", symbol="000001", name="评分样本",
            entry_price=10, entry_date="2026-01-02", hard_score=0,
            hard_rules_json={}, evidence_json={"structured_observations": {}},
            analysis_json={"model_output_status": "NOT_CALLED"},
        )
        session.add(candidate)
        session.commit()
        session.info.update(source_id=source.id, candidate_id=candidate.id)
        yield session
    engine.dispose()


def enabled_profile(db: Session):
    from app.models.selection import SelectionStrategyProfile
    return db.scalar(select(SelectionStrategyProfile).where(SelectionStrategyProfile.enabled.is_(True)))


def test_missing_dimensions_stay_missing_and_do_not_become_zero(db):
    candidate = db.get(SelectionCandidate, db.info["candidate_id"])
    result = score_candidate(db, candidate, enabled_profile(db))
    assert result["status"] == "PARTIAL"
    assert result["base_score"] is None
    assert result["trusted_score"] is None
    assert result["as_of"].startswith("2026-01-02")
    assert all(row["status"] == "INSUFFICIENT" and row["score"] is None for row in result["dimensions"])
    assert db.scalar(select(SelectionDimensionResult).where(
        SelectionDimensionResult.candidate_id == candidate.id,
    )) is not None


def test_profile_clone_requires_complete_weights_and_publishing_retires_previous(db):
    current = enabled_profile(db)
    draft = clone_profile(db, current.id, version="1.1.0-test")
    snapshot = serialize_profile(db, draft)
    items = snapshot["items"]
    items[0]["weight"] += 1
    with pytest.raises(ValueError, match="100%"):
        update_profile(db, draft.id, SelectionProfileUpdate(items=[{
            "dimension_code": row["dimension_code"], "weight": row["weight"],
            "minimum_coverage": row["minimum_coverage"], "minimum_confidence": row["minimum_confidence"],
            "skill_code": row["skill_code"], "skill_version": row["skill_version"],
        } for row in items]))
    published = publish_profile(db, draft.id)
    assert published.enabled is True and published.status == "ENABLED"
    db.refresh(current)
    assert current.enabled is False and current.status == "RETIRED"


def test_trusted_score_contracts_toward_neutral(db):
    candidate = db.get(SelectionCandidate, db.info["candidate_id"])
    candidate.evidence_json = {
        "structured_observations": {"market": [{"id": "m1", "properties": {"recent_bars": [
            {"trade_date": f"2025-12-{day:02d}", "close_price": 10 + day * .1, "volume": 100 + day}
            for day in range(1, 21)
        ]}}]},
    }
    result = score_candidate(db, candidate, enabled_profile(db))
    assert result["base_score"] is not None
    assert result["trusted_score"] is not None
    assert abs(result["trusted_score"] - 50) <= abs(result["base_score"] - 50)
    price = next(row for row in result["dimensions"] if row["dimension_code"] == "PRICE_RISK")
    assert price["status"] == "SUPPORTED" and price["coverage"] == 1


@pytest.mark.parametrize("analysis", [
    {"model_output_status": "FAILED"},
    {"model_output_status": "NOT_RUN", "data_gate_status": "MISSING_DATA"},
    {"model_output_status": "SUCCESS", "analysis_validation_status": "PARTIAL"},
])
def test_unverified_model_result_cannot_produce_a_score(db, analysis):
    candidate = db.get(SelectionCandidate, db.info["candidate_id"])
    candidate.analysis_json = {**analysis, "scoring": {"trusted_score": 88}}
    result = score_candidate(db, candidate, enabled_profile(db))
    assert result["status"] == "BLOCKED" and result["dimensions"] == []
    assert "scoring" not in candidate.analysis_json
    assert candidate.analysis_json["scoring_available"] is False


def test_candidate_graph_relations_do_not_become_scored_facts(db):
    candidate = db.get(SelectionCandidate, db.info["candidate_id"])
    candidate.evidence_json = {
        "graph_paths": [
            {"id": index, "predicate": "SUPPLIES_TO", "verification_status": "CANDIDATE"}
            for index in range(1, 6)
        ],
    }
    candidate.analysis_json = {
        "local_multi_source": {
            "dimensions": [{"dimension": "GRAPH", "status": "SUPPORTED", "direction": "POSITIVE"}],
        },
    }
    result = score_candidate(db, candidate, enabled_profile(db))
    relation = next(row for row in result["dimensions"] if row["dimension_code"] == "SUPPLY_CHAIN_RESILIENCE")
    assert relation["status"] == "INSUFFICIENT" and relation["score"] is None
    assert relation["metrics"]["candidate_business_relations"] == 5
    assert relation["evidence"]["relation_ids"] == []


def test_verified_business_and_value_chain_analysis_feed_long_horizon_dimensions(db):
    candidate = db.get(SelectionCandidate, db.info["candidate_id"])
    candidate.evidence_json = {"structured_observations": {"business": [
        {"id": "business-profile", "fact_type": "BUSINESS_PROFILE", "properties": {"main_business": "半导体测试设备"}},
        {"id": "business-composition", "fact_type": "BUSINESS_COMPOSITION",
         "properties": {"report_date": "2025-12-31", "segments": [{"items": [
             {"name": "半导体测试设备", "revenue_ratio": .6, "gross_margin": .3},
         ]}]}},
    ]}}
    candidate.analysis_json = {"model_output_status": "SUCCESS", "analysis_validation_status": "PASSED", "model": {
        "business_quality": {"status": "SUPPORTED", "main_business_summary": "半导体测试设备已成为主要收入来源。",
                             "impact_score": 60, "direction": "POSITIVE", "confidence": .8,
                             "evidence_fact_ids": ["business-profile", "business-composition"]},
        "value_chain_analysis": {"status": "SUPPORTED", "summary": "已审核客户扩产形成中期需求传导。", "impacts": [{
            "relation_status": "VERIFIED", "impact_score": 40, "direction": "POSITIVE", "confidence": .8,
            "relation_fact_ids": ["accepted-supply"],
        }]},
    }}
    result = score_candidate(db, candidate, enabled_profile(db))
    fundamental = next(row for row in result["dimensions"] if row["dimension_code"] == "FUNDAMENTAL_QUALITY")
    relation = next(row for row in result["dimensions"] if row["dimension_code"] == "SUPPLY_CHAIN_RESILIENCE")
    assert fundamental["status"] == "SUPPORTED" and fundamental["score"] == 80
    assert fundamental["metrics"]["business_records"] == 2
    assert relation["status"] == "SUPPORTED" and relation["score"] == 70
    assert relation["metrics"]["verified_transmission_impacts"] == 1
    assert relation["evidence"]["fact_ids"] == ["accepted-supply"]


def test_scoring_skills_use_versioned_read_only_contract(db):
    seed_default_skills(db)
    skill = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == "RELATION_RISK_ANALYST"))
    assert skill is not None
    assert skill.config_json["managed_contract_version"] == "SELECTION_SCORING_V1"
    assert skill.input_contract_json["required"] == ["candidate_id", "market", "symbol", "as_of", "evidence"]
    assert skill.output_contract_json["properties"]["status"]["enum"] == ["SUPPORTED", "INSUFFICIENT"]
    assert skill.permission_policy_json["database_write"] is False
    assert skill.idempotency_policy == "REQUIRED"
    special = list(db.scalars(select(ModelSkill).where(ModelSkill.skill_code.in_([
        "BUSINESS_MOAT_VALUE_ANALYST", "VALUE_CHAIN_TRANSMISSION_ANALYST",
    ])).order_by(ModelSkill.skill_code)))
    assert len(special) == 2
    assert all(row.enabled and row.file_path and row.side_effect_level == "READ_ONLY" for row in special)
    assert {row.config_json["managed_contract_version"] for row in special} == {
        "BUSINESS_MOAT_V1", "VALUE_CHAIN_TRANSMISSION_V1",
    }


def test_t3_t5_t7_use_real_following_sessions(db):
    candidate = db.get(SelectionCandidate, db.info["candidate_id"])
    score_candidate(db, candidate, enabled_profile(db))
    for index in range(1, 8):
        db.add(StockKline(
            market="CN_A", symbol="000001", period="daily", adjust="",
            trade_date=f"2026-01-{index + 2:02d}", close_price=10 + index,
            volume=100, source_id=db.info["source_id"],
        ))
    db.flush()
    rows = refresh_candidate_evaluations(db, candidate)
    assert [row["status"] for row in rows] == ["COMPLETED", "COMPLETED", "COMPLETED"]
    assert [row["observed_date"] for row in rows] == ["2026-01-05", "2026-01-07", "2026-01-09"]
    assert rows[0]["return_pct"] == 30
