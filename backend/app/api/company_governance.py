"""Bounded governance jobs and honest company-source capability status."""

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.foundation import require_enabled
from app.connectors.company_sources import authorized_source_capabilities
from app.db.session import get_db
from app.models.market_data import DataSource
from app.models.pipeline import PipelineRun, ScheduledJob
from app.orchestration.company_governance import TASK_TYPE, GovernanceRequest, enqueue_company_governance


router = APIRouter(prefix="/company-governance", tags=["Company Governance"], dependencies=[Depends(require_enabled)])


def read_job(db: Session, job: ScheduledJob):
    run = db.get(PipelineRun, job.pipeline_run_id)
    return {"id": job.id, "status": job.status, "attempts": job.attempts,
        "error_message": job.error_message, "result": run.output_json if run else {},
        "created_at": job.created_at, "completed_at": job.completed_at}


@router.get("/status")
def status(db: Session = Depends(get_db)):
    source_codes = ["EASTMONEY_COMPANY_PROFILE", "CNINFO_LEGAL_DISCLOSURES", "CNINFO_BUSINESS_DISCLOSURES",
                    "CNINFO_SUPPLY_CHAIN_DISCLOSURES", "GSXT_REGISTRATION", "COURT_JUDICIAL", "TENCENT_MARKET_CAP"]
    sources = list(db.scalars(select(DataSource).where(DataSource.source_code.in_(source_codes))))
    run = db.scalar(select(PipelineRun).where(PipelineRun.pipeline_type == TASK_TYPE).order_by(PipelineRun.id.desc()).limit(1))
    return {"sources": [{"code": row.source_code, "name": row.source_name, "enabled": row.enabled,
        "capabilities": (row.config_json or {}).get("capabilities", []),
        "authorization_status": (row.config_json or {}).get("authorization_status", "UNKNOWN"),
        "description": row.description} for row in sources],
        "restricted_sources": authorized_source_capabilities(),
        "latest_run": {"id": run.id, "status": run.status, "created_at": run.created_at,
            "result": run.output_json, "error_message": run.error_message} if run else None,
        "max_stocks_per_job": 30, "default_accept_structured": False}


@router.post("/jobs", status_code=202)
def submit(payload: GovernanceRequest, db: Session = Depends(get_db)):
    try:
        return read_job(db, enqueue_company_governance(db, payload))
    except LookupError as error:
        db.rollback()
        raise HTTPException(404, str(error)) from error
    except ValueError as error:
        db.rollback()
        raise HTTPException(422, str(error)) from error


@router.get("/jobs/{job_id}")
def job(job_id: int, db: Session = Depends(get_db)):
    row = db.get(ScheduledJob, job_id)
    if row is None or row.task_type != TASK_TYPE:
        raise HTTPException(404, "Company governance job not found")
    return read_job(db, row)
