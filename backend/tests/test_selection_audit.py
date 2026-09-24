"""Regression tests for immutable selection and Skill evaluation metadata."""

from datetime import datetime, timezone

from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session

from app.db.base import Base
from app.db.table_comments import TABLE_DESCRIPTIONS, column_descriptions_for
from app.models import (
    SelectionCandidate,
    SelectionDecisionSnapshot,
    SelectionRetrospective,
    SelectionRun,
    SelectionSnapshot,
    SelectionTracking,
)
from app.services.selection_audit import capture_decision_snapshot, capture_retrospective


def test_audit_tables_are_registered_and_documented():
    expected = {
        "selection_decision_snapshot",
        "selection_retrospective",
        "skill_evaluation_case",
        "skill_evaluation_run",
        "skill_evaluation_result",
    }
    assert expected.issubset(Base.metadata.tables)
    assert expected.issubset(TABLE_DESCRIPTIONS)
    for name in expected:
        table = Base.metadata.tables[name]
        descriptions = column_descriptions_for(table)
        assert set(descriptions) == {column.name for column in table.columns}
        assert all(descriptions.values())


def test_decision_snapshot_and_retrospective_capture_are_idempotent():
    engine = create_engine("sqlite://")
    Base.metadata.create_all(engine)
    now = datetime(2026, 9, 25, tzinfo=timezone.utc)
    with Session(engine) as db:
        run = SelectionRun(market="CN_A", as_of=now, candidate_count=1)
        db.add(run)
        db.flush()
        candidate = SelectionCandidate(
            run_id=run.id,
            market="CN_A",
            symbol="000001",
            name="测试证券",
            decision="APPROVED",
            entry_price=10.0,
            entry_date="2026-09-25",
            evidence_json={"context_hash": "ctx-1", "graph_version": "graph-1"},
        )
        db.add(candidate)
        db.flush()
        first = capture_decision_snapshot(
            db, candidate, snapshot_kind="REVIEW", decision="APPROVED", as_of=now,
            evidence={"context_hash": "ctx-1", "graph_version": "graph-1"},
        )
        second = capture_decision_snapshot(
            db, candidate, snapshot_kind="REVIEW", decision="APPROVED", as_of=now,
            evidence={"context_hash": "ctx-1", "graph_version": "graph-1"},
        )
        assert first.id == second.id

        tracking = SelectionTracking(
            candidate_id=candidate.id,
            market="CN_A",
            symbol="000001",
            entry_price=10.0,
            entry_date="2026-09-25",
            confirmation_date="2026-09-25",
            status="PENDING_DATA",
        )
        db.add(tracking)
        db.flush()
        db.add(SelectionSnapshot(
            tracking_id=tracking.id,
            trade_date="2026-09-25",
            close_price=10.2,
            return_pct=2.0,
            drawdown_pct=0.0,
            sequence=1,
        ))
        db.flush()
        review_one = capture_retrospective(db, tracking)
        review_two = capture_retrospective(db, tracking)
        db.commit()
        assert review_one is not None
        assert review_two is not None
        assert review_one.id == review_two.id
        assert db.scalar(select(SelectionDecisionSnapshot).where(
            SelectionDecisionSnapshot.candidate_id == candidate.id,
        )) is not None
        assert db.scalar(select(SelectionRetrospective).where(
            SelectionRetrospective.tracking_id == tracking.id,
        )) is not None
    engine.dispose()
