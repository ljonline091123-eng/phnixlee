from __future__ import annotations

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, Query, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.stock_analyst import AiStockAnalysisRecord
from app.schemas.stock_analyst import (
    AiStockAnalysisCreate, AiStockAnalysisRead, AiStockContextPreview,
)
from app.services.stock_analyst import (
    queue_stock_analysis, preview_stock_analyst_context, run_stock_analysis_background, cancel_stock_analysis,
)


router = APIRouter(prefix="/ai-stock-analyst", tags=["AI Stock Analyst"])


@router.post("/context-preview", response_model=AiStockContextPreview)
def preview_context(
    payload: AiStockAnalysisCreate,
    db: Session = Depends(get_db),
) -> AiStockContextPreview:
    try:
        return preview_stock_analyst_context(db, payload)
    except LookupError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc


@router.post("/analyses", response_model=AiStockAnalysisRead, status_code=status.HTTP_202_ACCEPTED)
def create_analysis(
    payload: AiStockAnalysisCreate,
    background_tasks: BackgroundTasks,
    db: Session = Depends(get_db),
) -> AiStockAnalysisRecord:
    try:
        record = queue_stock_analysis(db, payload)
        background_tasks.add_task(run_stock_analysis_background, record.id)
        return record
    except LookupError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=409, detail=str(exc)) from exc


@router.get("/analyses", response_model=list[AiStockAnalysisRead])
def list_analyses(
    market: str | None = Query(default=None, max_length=16),
    symbol: str | None = Query(default=None, max_length=32),
    limit: int = Query(default=30, ge=1, le=200),
    db: Session = Depends(get_db),
) -> list[AiStockAnalysisRecord]:
    statement = select(AiStockAnalysisRecord).order_by(
        AiStockAnalysisRecord.created_at.desc(), AiStockAnalysisRecord.id.desc(),
    ).limit(limit)
    if market:
        statement = statement.where(AiStockAnalysisRecord.market == market.strip().upper())
    if symbol:
        statement = statement.where(AiStockAnalysisRecord.symbol == symbol.strip())
    return list(db.scalars(statement).all())


@router.get("/analyses/{analysis_id}", response_model=AiStockAnalysisRead)
def get_analysis(analysis_id: int, db: Session = Depends(get_db)) -> AiStockAnalysisRecord:
    row = db.get(AiStockAnalysisRecord, analysis_id)
    if row is None:
        raise HTTPException(status_code=404, detail="AI证券分析记录不存在")
    return row


@router.get("/analyses/{analysis_id}/evidence")
def get_evidence(
    analysis_id: int, offset: int = Query(default=0, ge=0), limit: int = Query(default=20, ge=1, le=100),
    db: Session = Depends(get_db),
) -> dict:
    record = get_analysis(analysis_id, db)
    rows = record.evidence_snapshot_json.get("evidence", [])
    return {"items": rows[offset:offset + limit], "total": len(rows), "offset": offset, "limit": limit}


@router.post("/analyses/{analysis_id}/cancel", response_model=AiStockAnalysisRead)
def cancel_analysis(analysis_id: int, db: Session = Depends(get_db)) -> AiStockAnalysisRecord:
    return cancel_stock_analysis(db, get_analysis(analysis_id, db))
