"""Read-only API for L0 deterministic change events.

Events are produced by ``app.services.change_detection`` (scheduled daily at
16:00 or run manually via ``scripts.run_l0_detection``).  This router only
reads; it never triggers detection.
"""

from fastapi import APIRouter, Depends, Query
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.change_event import StockChangeEvent
from app.schemas.change_events import ChangeEventRead

router = APIRouter(prefix="/change-events", tags=["Change Events"])


@router.get("", response_model=list[ChangeEventRead])
def list_change_events(
    market: str | None = None,
    symbol: str | None = None,
    event_type: str | None = None,
    date_from: str | None = Query(None, description="含当日，格式 YYYY-MM-DD"),
    date_to: str | None = Query(None, description="含当日，格式 YYYY-MM-DD"),
    limit: int = Query(50, ge=1, le=200),
    db: Session = Depends(get_db),
):
    """按市场 / 代码 / 类型 / 日期区间查询变化事件，最新在前。"""
    query = select(StockChangeEvent)
    if market:
        query = query.where(StockChangeEvent.market == market)
    if symbol:
        query = query.where(StockChangeEvent.symbol == symbol)
    if event_type:
        query = query.where(StockChangeEvent.event_type == event_type)
    if date_from:
        query = query.where(StockChangeEvent.event_date >= date_from)
    if date_to:
        query = query.where(StockChangeEvent.event_date <= date_to)
    return list(
        db.scalars(
            query.order_by(StockChangeEvent.id.desc()).limit(limit)
        ).all()
    )
