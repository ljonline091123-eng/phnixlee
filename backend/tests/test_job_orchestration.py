from __future__ import annotations

from pathlib import Path
from tempfile import TemporaryDirectory

from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session, sessionmaker

from app.db.base import Base
from app.jobs.dispatcher import DatabaseJobDispatcher
from app.jobs.worker import JobWorker
from app.models.pipeline import PipelineRun, PipelineStageRun, ScheduledJob


def _database():
    temporary = TemporaryDirectory()
    path = Path(temporary.name) / "jobs.db"
    engine = create_engine(f"sqlite:///{path}")
    Base.metadata.create_all(engine)
    return temporary, engine, sessionmaker(bind=engine)


def test_dispatch_is_idempotent_and_worker_completes_pipeline() -> None:
    temporary, engine, sessions = _database()
    try:
        with sessions() as db:
            first = DatabaseJobDispatcher(db).enqueue(
                task_type="demo",
                idempotency_key="demo:2026-09-16",
                payload={"value": 7},
            )
            second = DatabaseJobDispatcher(db).enqueue(
                task_type="demo",
                idempotency_key="demo:2026-09-16",
                payload={"value": 99},
            )
            assert first.id == second.id
            assert db.query(ScheduledJob).count() == 1
            assert db.query(PipelineRun).count() == 1

        worker = JobWorker(
            worker_id="test-worker",
            session_factory=sessions,
            task_executor=lambda task_type, payload: {
                "task_type": task_type,
                "result": payload["value"] * 2,
            },
        )
        assert worker.run_once() is True
        assert worker.run_once() is False

        with sessions() as db:
            job = db.scalar(select(ScheduledJob))
            pipeline = db.scalar(select(PipelineRun))
            stage = db.scalar(select(PipelineStageRun))
            assert job.status == "COMPLETED"
            assert job.worker_id == "test-worker"
            assert pipeline.status == "COMPLETED"
            assert pipeline.output_json["result"] == 14
            assert stage.status == "COMPLETED"
            assert stage.attempt == 1
    finally:
        engine.dispose()
        temporary.cleanup()


def test_worker_records_retry_without_holding_claim_transaction() -> None:
    temporary, engine, sessions = _database()
    try:
        with sessions() as db:
            DatabaseJobDispatcher(db).enqueue(
                task_type="always_fails",
                idempotency_key="always_fails:1",
                max_attempts=2,
            )

        def fail(_task_type: str, _payload: dict) -> dict:
            # A separate session can write while the handler runs, proving the
            # SQLite claim transaction was committed before execution.
            with sessions() as independent:
                independent.get(ScheduledJob, 1)
            raise RuntimeError("provider unavailable")

        worker = JobWorker(session_factory=sessions, task_executor=fail)
        assert worker.run_once() is True
        with sessions() as db:
            job = db.scalar(select(ScheduledJob))
            pipeline = db.scalar(select(PipelineRun))
            stage = db.scalar(select(PipelineStageRun))
            assert job.status == "RETRY"
            assert job.attempts == 1
            assert "provider unavailable" in job.error_message
            assert pipeline.status == "RETRY"
            assert stage.status == "RETRY"
            assert stage.next_retry_at is not None
    finally:
        engine.dispose()
        temporary.cleanup()


def test_worker_clears_previous_attempt_error_when_retry_starts() -> None:
    temporary, engine, sessions = _database()
    try:
        with sessions() as db:
            job = DatabaseJobDispatcher(db).enqueue(
                task_type="retry_succeeds",
                idempotency_key="retry_succeeds:1",
                max_attempts=2,
            )
            pipeline = db.get(PipelineRun, job.pipeline_run_id)
            stage = db.scalar(select(PipelineStageRun).where(
                PipelineStageRun.pipeline_run_id == job.pipeline_run_id,
            ))
            job.status = "RETRY"
            job.error_message = "previous attempt failed"
            pipeline.status = "RETRY"
            pipeline.error_message = "previous attempt failed"
            stage.status = "RETRY"
            stage.error_code = "RuntimeError"
            stage.error_message = "previous attempt failed"
            db.commit()

        observed: dict[str, object] = {}

        def inspect_running_attempt(_task_type: str, _payload: dict) -> dict:
            with sessions() as db:
                job = db.scalar(select(ScheduledJob))
                pipeline = db.scalar(select(PipelineRun))
                stage = db.scalar(select(PipelineStageRun))
                observed.update({
                    "job_status": job.status,
                    "job_error": job.error_message,
                    "pipeline_error": pipeline.error_message,
                    "stage_error_code": stage.error_code,
                    "stage_error": stage.error_message,
                    "next_retry_at": stage.next_retry_at,
                })
            return {"ok": True}

        worker = JobWorker(session_factory=sessions, task_executor=inspect_running_attempt)
        assert worker.run_once() is True
        assert observed == {
            "job_status": "RUNNING",
            "job_error": None,
            "pipeline_error": None,
            "stage_error_code": None,
            "stage_error": None,
            "next_retry_at": None,
        }
    finally:
        engine.dispose()
        temporary.cleanup()
