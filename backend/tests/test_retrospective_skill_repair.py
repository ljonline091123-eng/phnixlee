"""Regression coverage for retrospective-to-Skill repair orchestration."""

from __future__ import annotations

from datetime import datetime, timezone
from types import SimpleNamespace
from unittest.mock import patch

from fastapi.testclient import TestClient
from sqlalchemy import create_engine, func, select
from sqlalchemy.orm import Session
from sqlalchemy.pool import StaticPool

from app.db.base import Base
from app.db.session import get_db
from app.main import app
from app.models.ai_hub import ModelCallLog, ModelSkill, SkillOptimizationDraft
from app.models.decision_review import (
    SelectionDecisionSnapshot,
    SelectionRetrospective,
    SkillEvaluationCase,
    SkillEvaluationResult,
    SkillEvaluationRun,
)
from app.models.selection import SelectionCandidate, SelectionRun
from app.services.skill_evaluation import run_retrospective_repair_flow


def _seed_failed_retrospective(db: Session) -> tuple[ModelSkill, SelectionRetrospective]:
    now = datetime(2026, 9, 25, tzinfo=timezone.utc)
    skill = ModelSkill(
        skill_code="STOCK_SELECTION_ANALYST",
        skill_name="Selection analyst",
        instructions="Use only grounded evidence and state invalidation conditions.",
        version="1.2.3",
        enabled=True,
        config_json={"task_type": "stock_screening"},
    )
    run = SelectionRun(market="CN_A", as_of=now, candidate_count=1)
    db.add_all([skill, run])
    db.flush()
    candidate = SelectionCandidate(
        run_id=run.id,
        market="CN_A",
        symbol="000001",
        name="Test Security",
        decision="APPROVED",
        analysis_json={},
        evidence_json={},
    )
    db.add(candidate)
    db.flush()
    snapshot = SelectionDecisionSnapshot(
        candidate_id=candidate.id,
        run_id=run.id,
        snapshot_kind="REVIEW",
        event_key="review:test-retrospective",
        market="CN_A",
        symbol="000001",
        decision="APPROVED",
        as_of=now,
        data_version="lake-v7",
        graph_version="graph-v3",
        skill_code=skill.skill_code,
        skill_version=skill.version,
        model_instance_code="LIVE_SELECTION_MODEL",
        payload_json={
            "analysis": {
                "model": {
                    "decision": "WATCH",
                    "confidence": 0.8,
                    "reasoning": "The cited evidence supports the thesis.",
                    "horizon_sessions": 10,
                    "evidence_document_ids": [11],
                }
            }
        },
        evidence_json={"document_ids": [11], "context_document_ids": [12]},
        payload_hash="snapshot-payload-hash",
    )
    db.add(snapshot)
    db.flush()
    retrospective = SelectionRetrospective(
        snapshot_id=snapshot.id,
        revision=1,
        market="CN_A",
        symbol="000001",
        status="COMPLETED",
        evaluated_at=now,
        observed_sessions=10,
        entry_price=10.0,
        final_price=9.0,
        return_pct=-10.0,
        max_drawdown_pct=-12.0,
        target_hit=False,
        stop_hit=True,
        direction_hit=False,
        attribution_json={"evidence_document_ids": [11, 13]},
        error_tags_json=["DIRECTION_MISS", "TARGET_NOT_HIT", "STOP_HIT"],
        summary="The expected direction was not observed within ten sessions.",
        state_hash="retrospective-state-hash-0001",
    )
    db.add(retrospective)
    db.commit()
    return skill, retrospective


def _engine():
    engine = create_engine(
        "sqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    Base.metadata.create_all(engine)
    return engine


def test_repair_flow_endpoint_reuses_case_and_reports_mock_model_unavailable():
    engine = _engine()
    with Session(engine) as db:
        skill, retrospective = _seed_failed_retrospective(db)
        original_instructions = skill.instructions
        app.dependency_overrides[get_db] = lambda: db
        client = TestClient(app)
        mock_log = SimpleNamespace(
            status="SUCCESS",
            response_text='{"proposed_instructions":"Do not use this mock proposal.","rationale":"mock"}',
            provider_code="MOCK",
        )
        try:
            with patch("app.services.skill_evaluation.ModelHubService.chat", return_value=mock_log):
                first = client.post(
                    f"/api/v1/skill-evaluations/retrospectives/{retrospective.id}/repair-flow",
                    json={},
                )
                second = client.post(
                    f"/api/v1/skill-evaluations/retrospectives/{retrospective.id}/repair-flow",
                    json={},
                )
            assert first.status_code == 200, first.text
            assert second.status_code == 200, second.text
            assert first.json()["case_created"] is True
            assert second.json()["case_created"] is False
            assert first.json()["evaluation_status"] == "FAILED"
            assert first.json()["evaluation_source"] == "PERSISTED_MODEL_OUTPUT"
            assert first.json()["repair_status"] == "MODEL_UNAVAILABLE"
            assert first.json()["repair_draft_id"] is None
            assert db.scalar(select(func.count(SkillEvaluationCase.id))) == 1
            case = db.scalar(select(SkillEvaluationCase))
            assert case is not None
            assert case.source == "RETROSPECTIVE"
            assert case.input_json["allowed_evidence_ids"] == [11, 12, 13]
            assert "outcome" not in case.input_json
            assert case.expected_json == {"decision": "PASS"}
            assert case.rubric_json["case_role"] == "HISTORICAL_BLIND_REGRESSION"
            result = db.scalar(select(SkillEvaluationResult).order_by(SkillEvaluationResult.id))
            assert result is not None
            assert result.error_type == "EXPECTED_MISMATCH"
            assert result.evidence_refs_json == [11]
            assert db.scalar(select(func.count(SkillOptimizationDraft.id))) == 0
            db.refresh(skill)
            assert skill.version == "1.2.3"
            assert skill.instructions == original_instructions
            assert skill.enabled is True
        finally:
            client.close()
            app.dependency_overrides.pop(get_db, None)
    engine.dispose()


def test_live_repair_proposal_stays_pending_and_is_deduplicated():
    engine = _engine()
    with Session(engine) as db:
        skill, retrospective = _seed_failed_retrospective(db)
        original_instructions = skill.instructions
        log = ModelCallLog(
            task_type="meta_review",
            provider_code="LIVE_TEST",
            instance_code="LIVE_META_MODEL",
            model_code="test-model",
            status="SUCCESS",
            request_json={},
            response_json={},
            response_text=(
                '{"proposed_instructions":"Require dated evidence and explicit invalidation rules.",'
                '"rationale":"The regression case missed direction and hit its stop."}'
            ),
        )
        db.add(log)
        db.commit()

        with patch("app.services.skill_evaluation.ModelHubService.chat", return_value=log) as chat:
            first = run_retrospective_repair_flow(db, retrospective.id)
            second = run_retrospective_repair_flow(db, retrospective.id)

        assert first["repair_status"] == "PENDING_REVIEW"
        assert first["repair_draft_status"] == "PENDING_REVIEW"
        assert second["repair_draft_id"] == first["repair_draft_id"]
        assert chat.call_count == 1
        assert db.scalar(select(func.count(SkillEvaluationCase.id))) == 1
        assert db.scalar(select(func.count(SkillEvaluationRun.id))) == 2
        assert db.scalar(select(func.count(SkillOptimizationDraft.id))) == 1
        draft = db.get(SkillOptimizationDraft, first["repair_draft_id"])
        assert draft is not None
        assert draft.status == "PENDING_REVIEW"
        assert draft.failure_signature.startswith("EVALUATION:")
        db.refresh(skill)
        assert skill.instructions == original_instructions
        assert skill.version == "1.2.3"
        assert skill.enabled is True
    engine.dispose()


def test_changed_skill_requires_explicit_current_version_output():
    engine = _engine()
    with Session(engine) as db:
        skill, retrospective = _seed_failed_retrospective(db)
        skill.version = "1.2.4"
        db.commit()

        result = run_retrospective_repair_flow(db, retrospective.id, request_repair_draft=False)
        assert result["evaluation_status"] == "SKILL_VERSION_MISMATCH"
        assert result["evaluation_run_id"] is None

        explicit = run_retrospective_repair_flow(
            db,
            retrospective.id,
            evaluation_output={"decision": "PASS", "evidence_document_ids": [11]},
            request_repair_draft=False,
        )
        assert explicit["evaluation_status"] == "PASSED"
        assert explicit["evaluation_source"] == "EXPLICIT_OUTPUT"
    engine.dispose()
