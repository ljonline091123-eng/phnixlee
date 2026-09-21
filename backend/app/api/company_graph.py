"""Company and security views added alongside existing research APIs."""

from datetime import date, datetime
from typing import Literal

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.foundation import _known_at, _read, _write, require_enabled
from app.db.session import get_db
from app.models.market_data import StockSymbol
from app.schemas.company_graph import SecurityClassificationCreate, SourceBatch
from app.schemas.foundation import FactReviewCreate
from app.services import company_graph as service


router = APIRouter(prefix="/company-graph", tags=["Company Graph"], dependencies=[Depends(require_enabled)])


@router.get("/status")
def status(db: Session = Depends(get_db)):
    return service.status(db)


@router.get("/search")
def search(mode: Literal["security", "company"] = "security", q: str = Query("", max_length=256),
           market: str | None = Query(None, max_length=16), limit: int = Query(20, ge=1, le=50),
           offset: int = Query(0, ge=0, le=100000), db: Session = Depends(get_db)):
    return service.search(db, mode=mode, q=q, market=market, limit=limit, offset=offset)


@router.get("/samples")
def samples(q: str = Query("", max_length=256), market: str | None = Query(None, max_length=16),
            limit: int = Query(30, ge=1, le=50), offset: int = Query(0, ge=0, le=100000), db: Session = Depends(get_db)):
    return service.samples(db, q=q, market=market, limit=limit, offset=offset)


@router.get("/mapping-status")
def mapping_status(db: Session = Depends(get_db)):
    from app.services.company_master import mapping_status as read_status
    return read_status(db)


@router.get("/companies/{company_id}")
def company(company_id: str, as_of: date | None = None, known_at: datetime | None = None, db: Session = Depends(get_db)):
    return _read(lambda: service.company_profile(db, company_id, as_of=as_of, known_at=_known_at(known_at)))


@router.get("/securities/{stock_symbol_id}")
def security(stock_symbol_id: int, as_of: date | None = None, known_at: datetime | None = None, db: Session = Depends(get_db)):
    return _read(lambda: service.security_profile(db, stock_symbol_id, as_of=as_of, known_at=_known_at(known_at)))


@router.get("/by-symbol")
def by_symbol(market: str = Query(min_length=1, max_length=16), symbol: str = Query(min_length=1, max_length=32),
              as_of: date | None = None, known_at: datetime | None = None, db: Session = Depends(get_db)):
    stock = db.scalar(select(StockSymbol).where(StockSymbol.market == market, StockSymbol.symbol == symbol))
    if stock is None:
        raise HTTPException(status_code=404, detail="Security not found in the selected market")
    return _read(lambda: service.security_profile(db, stock.id, as_of=as_of, known_at=_known_at(known_at)))


@router.get("/graph")
def graph(company_id: str | None = None, stock_symbol_id: int | None = Query(None, gt=0),
          depth: int = Query(2, ge=1, le=3), max_nodes: int = Query(80, ge=3, le=200),
          max_edges: int = Query(150, ge=1, le=400), include_pending: bool = False,
          as_of: date | None = None, known_at: datetime | None = None, db: Session = Depends(get_db)):
    return _read(lambda: service.graph(db, company_id=company_id, stock_symbol_id=stock_symbol_id, depth=depth,
        max_nodes=max_nodes, max_edges=max_edges, include_pending=include_pending, as_of=as_of, known_at=_known_at(known_at)))


@router.post("/imports", status_code=201)
def import_records(payload: SourceBatch, db: Session = Depends(get_db)):
    return _write(db, lambda: service.import_batch(db, payload), lambda result: result)


@router.post("/classifications", status_code=201)
def create_classification(payload: SecurityClassificationCreate, db: Session = Depends(get_db)):
    return _write(db, lambda: service.create_classification(db, payload), service.classification_read)


@router.post("/classifications/{classification_id}/review")
def review_classification(classification_id: str, payload: FactReviewCreate, db: Session = Depends(get_db)):
    return _write(db, lambda: service.review_classification(db, classification_id, payload), service.classification_read)
