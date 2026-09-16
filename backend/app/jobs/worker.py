"""Standalone database-backed worker process."""

from __future__ import annotations

import os
import socket
import time
from datetime import datetime, timedelta, timezone
from collections.abc import Callable
from typing import Any

from sqlalchemy import or_, select
from sqlalchemy.orm import Session

from app.db.bootstrap import initialize_database
from app.db.session import SessionLocal
from app.jobs.tasks import execute_task
from app.models.pipeline import PipelineRun, PipelineStageRun, ScheduledJob


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


class JobWorker:
    def __init__(
        self,
        *,
        worker_id: str | None = None,
        lease_seconds: int = 900,
        session_factory: Callable[[], Session] = SessionLocal,
        task_executor: Callable[[str, dict[str, Any]], dict[str, Any]] = execute_task,
    ):
        self.worker_id = worker_id or f"{socket.gethostname()}:{os.getpid()}"
        self.lease_seconds = lease_seconds
        self.session_factory = session_factory
        self.task_executor = task_executor

    def run_once(self) -> bool:
        with self.session_factory() as db:
            job = self._claim(db)
            if job is None:
                return False
            job_id = job.id
            task_type = job.task_type
            payload = dict(job.payload_json or {})

        # The claim transaction is committed before any network, market-data,
        # or model work starts. This avoids holding row/database locks while a
        # slow provider is running.
        try:
            output = self.task_executor(task_type, payload)
        except Exception as exc:
            self._finish_failure(job_id, exc)
        else:
            self._finish_success(job_id, output)
        return True

    def _claim(self, db: Session) -> ScheduledJob | None:
        now = utc_now()
        statement = (
            select(ScheduledJob)
            .where(
                ScheduledJob.run_after <= now,
                or_(
                    (
                        ScheduledJob.status.in_(("PENDING", "RETRY"))
                        & or_(ScheduledJob.lease_until.is_(None), ScheduledJob.lease_until < now)
                    ),
                    (
                        (ScheduledJob.status == "RUNNING")
                        & ScheduledJob.lease_until.is_not(None)
                        & (ScheduledJob.lease_until < now)
                    ),
                ),
            )
            .order_by(ScheduledJob.run_after, ScheduledJob.id)
            .limit(1)
        )
        if db.get_bind().dialect.name == "postgresql":
            statement = statement.with_for_update(skip_locked=True)
        job = db.scalar(statement)
        if job is None:
            return None
        job.status = "RUNNING"
        job.worker_id = self.worker_id
        job.attempts += 1
        job.started_at = job.started_at or now
        job.lease_until = now + timedelta(seconds=self.lease_seconds)
        pipeline = db.get(PipelineRun, job.pipeline_run_id)
        stage = db.scalar(
            select(PipelineStageRun).where(
                PipelineStageRun.pipeline_run_id == job.pipeline_run_id,
                PipelineStageRun.stage_code == job.task_type,
            )
        )
        if pipeline:
            pipeline.status = "RUNNING"
            pipeline.started_at = pipeline.started_at or now
            pipeline.current_stage = job.task_type
        if stage:
            stage.status = "RUNNING"
            stage.attempt = job.attempts
            stage.started_at = stage.started_at or now
            stage.lease_until = job.lease_until
        db.commit()
        db.refresh(job)
        return job

    def _finish_success(self, job_id: int, output: dict[str, Any]) -> None:
        now = utc_now()
        with self.session_factory() as db:
            job = db.get(ScheduledJob, job_id)
            if job is None:
                return
            job.status = "COMPLETED"
            job.lease_until = None
            job.completed_at = now
            job.error_message = None
            pipeline = db.get(PipelineRun, job.pipeline_run_id)
            stage = self._stage(db, job)
            if pipeline:
                pipeline.status = "COMPLETED"
                pipeline.output_json = output
                pipeline.completed_at = now
                pipeline.error_message = None
            if stage:
                stage.status = "COMPLETED"
                stage.output_json = output
                stage.completed_at = now
                stage.lease_until = None
                stage.error_code = None
                stage.error_message = None
            db.commit()

    def _finish_failure(self, job_id: int, exc: Exception) -> None:
        now = utc_now()
        with self.session_factory() as db:
            job = db.get(ScheduledJob, job_id)
            if job is None:
                return
            retry = job.attempts < job.max_attempts
            job.status = "RETRY" if retry else "FAILED"
            job.run_after = now + timedelta(seconds=min(300, 2 ** job.attempts * 5))
            job.lease_until = None
            job.error_message = str(exc)[:4000]
            if not retry:
                job.completed_at = now
            pipeline = db.get(PipelineRun, job.pipeline_run_id)
            stage = self._stage(db, job)
            if pipeline:
                pipeline.status = "RETRY" if retry else "FAILED"
                pipeline.error_message = job.error_message
                if not retry:
                    pipeline.completed_at = now
            if stage:
                stage.status = "RETRY" if retry else "FAILED"
                stage.next_retry_at = job.run_after if retry else None
                stage.lease_until = None
                stage.error_code = type(exc).__name__
                stage.error_message = job.error_message
                if not retry:
                    stage.completed_at = now
            db.commit()

    @staticmethod
    def _stage(db: Session, job: ScheduledJob) -> PipelineStageRun | None:
        return db.scalar(
            select(PipelineStageRun).where(
                PipelineStageRun.pipeline_run_id == job.pipeline_run_id,
                PipelineStageRun.stage_code == job.task_type,
            )
        )


def main() -> None:
    initialize_database(seed_defaults=False)
    worker = JobWorker()
    while True:
        if not worker.run_once():
            time.sleep(2)


if __name__ == "__main__":
    main()
