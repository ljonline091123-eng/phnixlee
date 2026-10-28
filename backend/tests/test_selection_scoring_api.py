from datetime import datetime, timezone

from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import Session
from sqlalchemy.pool import StaticPool

from app.db.base import Base
from app.db.session import get_db
from app.main import app
from app.models.market_data import DataSource, StockSymbol
from app.models.selection import SelectionCandidate, SelectionRun
from app.services.selection_scoring import seed_selection_scoring


def test_scoring_profile_lifecycle_and_candidate_publication_gate():
    engine = create_engine("sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool)
    Base.metadata.create_all(engine)
    with Session(engine) as db:
        seed_selection_scoring(db)
        source = DataSource(source_code="SCORE_API", source_name="Score API", adapter_type="MOCK")
        db.add(source)
        db.flush()
        db.add(StockSymbol(market="CN_A", symbol="000001", name="评分接口样本", exchange="SZSE", source_id=source.id))
        run = SelectionRun(market="CN_A", candidate_limit=1, criteria_json={}, data_mode="REAL",
                           analysis_mode="LOCAL_EVIDENCE", as_of=datetime(2026, 1, 2, tzinfo=timezone.utc))
        db.add(run)
        db.flush()
        candidate = SelectionCandidate(
            run_id=run.id, market="CN_A", symbol="000001", name="评分接口样本",
            entry_price=10, entry_date="2026-01-02", hard_score=0,
            hard_rules_json={}, evidence_json={"structured_observations": {}}, analysis_json={},
        )
        db.add(candidate)
        db.commit()
        app.dependency_overrides[get_db] = lambda: db
        client = TestClient(app)
        try:
            dimensions = client.get("/api/v1/selection/scoring/dimensions")
            assert dimensions.status_code == 200 and len(dimensions.json()) == 8
            profiles = client.get("/api/v1/selection/scoring/profiles")
            assert profiles.status_code == 200
            enabled = next(row for row in profiles.json() if row["enabled"])
            assert enabled["weight_total"] == 100

            clone = client.post(f"/api/v1/selection/scoring/profiles/{enabled['id']}/clone", json={"version": "api-test"})
            assert clone.status_code == 201 and clone.json()["status"] == "DRAFT"
            published = client.post(f"/api/v1/selection/scoring/profiles/{clone.json()['id']}/publish")
            assert published.status_code == 200 and published.json()["enabled"] is True

            scorecard = client.post(f"/api/v1/selection/candidates/{candidate.id}/scorecard")
            assert scorecard.status_code == 409
            assert candidate.analysis_json == {}
            assert client.get(f"/api/v1/selection/candidates/{candidate.id}/scorecard").status_code == 404

            # A complete model conclusion may repair a missing score, using
            # the actual observed price series, without fabricating coverage.
            candidate.evidence_json = {"structured_observations": {"market": [{
                "id": "market:1", "properties": {"recent_bars": [
                    {"trade_date": f"2025-12-{day:02d}", "close_price": 10 + day * .1, "volume": 100 + day}
                    for day in range(1, 21)
                ]},
            }]}}
            candidate.analysis_json = {"model_output_status": "SUCCESS", "analysis_validation_status": "PASSED",
                "model": {"reasoning": "依据已载入的量价序列进行研判", "confidence": .7}}
            db.commit()
            scorecard = client.post(f"/api/v1/selection/candidates/{candidate.id}/scorecard")
            assert scorecard.status_code == 200
            payload = scorecard.json()
            assert payload["contract_version"] == "SELECTION_SCORECARD_V1"
            assert len(payload["dimensions"]) == 8
            assert payload["trusted_score"] is not None

            read_back = client.get(f"/api/v1/selection/candidates/{candidate.id}/scorecard")
            assert read_back.status_code == 200 and len(read_back.json()["dimensions"]) == 8
            evaluation = client.get(f"/api/v1/selection/scoring/evaluations?profile_id={clone.json()['id']}")
            assert evaluation.status_code == 200 and [row["horizon_sessions"] for row in evaluation.json()["horizons"]] == [3, 5, 7]
        finally:
            app.dependency_overrides.pop(get_db, None)
    engine.dispose()
