"""Explicit, bounded archive jobs; no automatic rewrite of legacy graphs."""

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.db.session import get_db
from app.models.pipeline import PipelineRun, ScheduledJob
from app.orchestration.foundation import ARCHIVE_TASK, ArchiveRequest, enqueue_archive


router = APIRouter(prefix='/foundation/evidence/archive-jobs', tags=['Data Foundation'])


def _enabled():
    if not get_settings().data_foundation_enabled:
        raise HTTPException(status_code=503, detail='Data foundation is disabled')


def _read_job(db: Session, job: ScheduledJob) -> dict:
    run = db.get(PipelineRun, job.pipeline_run_id)
    return {
        'id': job.id, 'status': job.status, 'attempts': job.attempts,
        'document_count': len((job.payload_json or {}).get('document_ids', [])),
        'error_message': job.error_message,
        'result': run.output_json if run else {},
    }


@router.post('', status_code=202, dependencies=[Depends(_enabled)])
def submit_archive(payload: ArchiveRequest, db: Session = Depends(get_db)) -> dict:
    try:
        job = enqueue_archive(db, payload)
    except LookupError as exc:
        db.rollback()
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except ValueError as exc:
        db.rollback()
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    return _read_job(db, job)


@router.get('/{job_id}', dependencies=[Depends(_enabled)])
def archive_status(job_id: int, db: Session = Depends(get_db)) -> dict:
    job = db.get(ScheduledJob, job_id)
    if job is None or job.task_type != ARCHIVE_TASK:
        raise HTTPException(status_code=404, detail='Archive job not found')
    return _read_job(db, job)
