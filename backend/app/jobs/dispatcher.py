"""Durable, idempotent job submission."""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Any

from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.models.pipeline import PipelineRun, PipelineStageRun, ScheduledJob


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


class DatabaseJobDispatcher:
    def __init__(self, db: Session):
        self.db = db

    def enqueue(
        self,
        *,
        task_type: str,
        idempotency_key: str,
        payload: dict[str, Any] | None = None,
        run_after: datetime | None = None,
        max_attempts: int = 3,
        pipeline_type: str | None = None,
        trigger_type: str = "SCHEDULE",
    ) -> ScheduledJob:
        existing = self.db.scalar(
            select(ScheduledJob).where(ScheduledJob.idempotency_key == idempotency_key)
        )
        if existing is not None:
            return existing

        pipeline = PipelineRun(
            pipeline_type=pipeline_type or task_type,
            trigger_type=trigger_type,
            status="PENDING",
            idempotency_key=idempotency_key,
            input_json=payload or {},
            current_stage=task_type,
        )
        self.db.add(pipeline)
        self.db.flush()
        self.db.add(
            PipelineStageRun(
                pipeline_run_id=pipeline.id,
                stage_code=task_type,
                status="PENDING",
                max_attempts=max_attempts,
                input_json=payload or {},
            )
        )
        job = ScheduledJob(
            task_type=task_type,
            status="PENDING",
            idempotency_key=idempotency_key,
            payload_json=payload or {},
            max_attempts=max_attempts,
            run_after=run_after or utc_now(),
            pipeline_run_id=pipeline.id,
        )
        self.db.add(job)
        try:
            self.db.commit()
        except IntegrityError:
            self.db.rollback()
            existing = self.db.scalar(
                select(ScheduledJob).where(ScheduledJob.idempotency_key == idempotency_key)
            )
            if existing is None:
                raise
            return existing
        self.db.refresh(job)
        return job
