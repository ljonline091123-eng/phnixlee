"""HTTP surface for the L0 flywheel: pool status, refresh/detection triggers, jobs.

Endpoints are async by design: triggering returns ``202`` with a job view, and
the caller polls ``GET /watch-pool/jobs/{job_id}``.  Triggered jobs deliberately
do **not** chain the daily detection run, so an API call can never consume the
scheduler's idempotency key for the evening pipeline.
"""

from __future__ import annotations

from uuid import uuid4

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.jobs.dispatcher import DatabaseJobDispatcher
from app.models.pipeline import PipelineRun, ScheduledJob
from app.schemas.watch_pool import DetectRequest, RefreshRequest
from app.services.watch_pool_status import build_watch_pool_status


router = APIRouter(prefix="/watch-pool", tags=["Watch Pool"])

REFRESH_TASK = "watch_pool_data_refresh"
DETECT_TASK = "l0_change_detection"
WATCH_POOL_TASKS = (REFRESH_TASK, DETECT_TASK)


def _job_view(db: Session, job: ScheduledJob) -> dict:
    run = db.get(PipelineRun, job.pipeline_run_id) if job.pipeline_run_id else None
    return {
        "id": job.id,
        "task_type": job.task_type,
        "status": job.status,
        "attempts": job.attempts,
        "error_message": job.error_message,
        "created_at": job.created_at,
        "started_at": job.started_at,
        "completed_at": job.completed_at,
        "payload": job.payload_json,
        "result": run.output_json if run else {},
    }


@router.get("/status")
def watch_pool_status(market: str | None = None, db: Session = Depends(get_db)) -> dict:
    """池数据新鲜度：每只股票的最新行情/公告/新闻覆盖与 FRESH/STALE/NO_DATA 判定。"""
    return build_watch_pool_status(db, market=market)


@router.post("/refresh", status_code=202)
def submit_refresh(payload: RefreshRequest | None = None, db: Session = Depends(get_db)) -> dict:
    """投递一次池数据刷新任务（K线/公告/新闻），返回任务视图供轮询。"""
    body = payload or RefreshRequest()
    job = DatabaseJobDispatcher(db).enqueue(
        task_type=REFRESH_TASK,
        pipeline_type=REFRESH_TASK,
        idempotency_key=f"{REFRESH_TASK}:api:{uuid4().hex[:12]}",
        payload={
            "business_date": body.business_date,
            "market": body.market,
            "delay_seconds": body.delay_seconds,
            "chain_detection": False,
        },
        trigger_type="API",
    )
    return _job_view(db, job)


@router.post("/detect", status_code=202)
def submit_detection(payload: DetectRequest | None = None, db: Session = Depends(get_db)) -> dict:
    """投递一次 L0 变化检测任务（毫秒级扫描，worker 执行），返回任务视图。"""
    body = payload or DetectRequest()
    job = DatabaseJobDispatcher(db).enqueue(
        task_type=DETECT_TASK,
        pipeline_type=DETECT_TASK,
        idempotency_key=f"{DETECT_TASK}:api:{uuid4().hex[:12]}",
        payload={"business_date": body.business_date, "market": body.market},
        trigger_type="API",
    )
    return _job_view(db, job)


@router.get("/jobs")
def list_watch_pool_jobs(
    limit: int = Query(default=20, ge=1, le=100),
    db: Session = Depends(get_db),
) -> list[dict]:
    """最近的池刷新/检测任务，最新在前。"""
    jobs = list(
        db.scalars(
            select(ScheduledJob)
            .where(ScheduledJob.task_type.in_(WATCH_POOL_TASKS))
            .order_by(ScheduledJob.id.desc())
            .limit(limit)
        ).all()
    )
    return [_job_view(db, job) for job in jobs]


@router.get("/jobs/{job_id}")
def watch_pool_job(job_id: int, db: Session = Depends(get_db)) -> dict:
    """查询单个任务状态；完成后 result 中带本轮汇总。"""
    job = db.get(ScheduledJob, job_id)
    if job is None or job.task_type not in WATCH_POOL_TASKS:
        raise HTTPException(status_code=404, detail="Watch-pool job not found")
    return _job_view(db, job)
