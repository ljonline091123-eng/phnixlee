from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.context_event import StockContextEvent
from app.schemas.context_events import ContextEventCreate, ContextEventRead
from app.services.context_events import import_context_event

router = APIRouter(prefix="/resources/context-events", tags=["External Evidence"])


@router.post("", response_model=ContextEventRead)
def create_event(payload: ContextEventCreate, db: Session = Depends(get_db)):
    try:
        return import_context_event(db, payload)
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc


@router.get("", response_model=list[ContextEventRead])
def list_events(market: str | None = None, symbol: str | None = None, limit: int = Query(20, ge=1, le=100), db: Session = Depends(get_db)):
    query = select(StockContextEvent)
    if market:
        query = query.where(StockContextEvent.market == market)
    if symbol:
        query = query.where(StockContextEvent.symbol == symbol)
    return list(db.scalars(query.order_by(StockContextEvent.published_at.desc()).limit(limit)))
