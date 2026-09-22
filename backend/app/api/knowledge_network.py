"""Unified current-data knowledge network, added alongside legacy graph APIs."""

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.foundation import _read, require_enabled
from app.db.session import get_db
from app.services import knowledge_network as service


router = APIRouter(prefix="/knowledge-network", tags=["Knowledge Network"], dependencies=[Depends(require_enabled)])


@router.get("/explore")
def explore(market: str | None = Query(None, max_length=16), symbol: str | None = Query(None, max_length=32),
            company_id: str | None = Query(None, max_length=36), center_id: str | None = Query(None, max_length=128),
            graph_id: int | None = Query(None, gt=0), depth: int = Query(1, ge=1, le=3),
            max_nodes: int = Query(160, ge=10, le=400), max_edges: int = Query(300, ge=10, le=800),
            limit: int | None = Query(None, ge=10, le=400), per_category: int = Query(3, ge=1, le=10),
            include_candidates: bool = False, include_evidence: bool = True, db: Session = Depends(get_db)):
    return _read(lambda: service.explore(db, market=market, symbol=symbol, company_id=company_id,
        center_id=center_id, graph_id=graph_id, depth=depth, max_nodes=limit or max_nodes, max_edges=max_edges,
        include_candidates=include_candidates, include_evidence=include_evidence, per_category=per_category))


@router.get("/evidence")
def evidence(source_table: str = Query(min_length=1, max_length=64), record_id: str = Query(min_length=1, max_length=64),
             db: Session = Depends(get_db)):
    return _read(lambda: service.evidence_detail(db, source_table, record_id))
