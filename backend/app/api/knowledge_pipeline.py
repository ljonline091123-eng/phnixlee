from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.schemas.knowledge_pipeline import KnowledgePipelineRequest
from app.services.knowledge_pipeline import PipelineConflictError, run_stock_pipeline


router = APIRouter(prefix="/knowledge-pipeline", tags=["Knowledge pipeline"])


@router.post("/run")
def run(payload: KnowledgePipelineRequest, db: Session = Depends(get_db)):
    try:
        return run_stock_pipeline(db, **payload.model_dump())
    except PipelineConflictError as exc:
        raise HTTPException(status_code=409, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc


@router.get("/runs/{run_id}")
def get_run(run_id: int, db: Session = Depends(get_db)):
    from app.models.pipeline import PipelineRun, PipelineStageRun
    row = db.get(PipelineRun, run_id)
    if row is None:
        raise HTTPException(status_code=404, detail="pipeline run not found")
    return {"run": row, "stages": list(db.query(PipelineStageRun).filter(PipelineStageRun.pipeline_run_id == run_id).order_by(PipelineStageRun.id).all())}

