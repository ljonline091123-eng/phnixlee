"""Additive, local single-user foundation API."""

from datetime import date, datetime, timezone
from typing import Literal

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import or_, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.db.session import get_db
from app.models.foundation import FoundationEntity, FoundationEvidence, FoundationFact, FoundationListing, FoundationSecurity
from app.models.market_data import StockSymbol
from app.schemas.foundation import EntityCreate, EntityType, EvidenceCreate, FactCreate, FactReviewCreate, FactType, LegacyEvidenceImport, SecurityMappingCreate
from app.services import foundation as service


router = APIRouter(prefix="/foundation", tags=["Data Foundation"])


def require_enabled() -> None:
    if not get_settings().data_foundation_enabled:
        raise HTTPException(status_code=503, detail="Data foundation is disabled")


def _read(operation):
    try:
        return operation()
    except service.FoundationError as exc:
        raise HTTPException(status_code=exc.status_code, detail=str(exc)) from exc


def _write(db: Session, operation, serialize):
    try:
        row = operation()
        db.commit()
        return serialize(row)
    except service.FoundationError as exc:
        db.rollback()
        raise HTTPException(status_code=exc.status_code, detail=str(exc)) from exc
    except IntegrityError as exc:
        db.rollback()
        raise HTTPException(status_code=409, detail="Conflicting identifier or concurrent write; reload and retry") from exc


def _known_at(value: datetime | None) -> datetime | None:
    if value is None:
        return None
    if value.tzinfo is None or value.utcoffset() is None:
        raise HTTPException(status_code=422, detail="known_at must include a timezone")
    if value > datetime.now(timezone.utc):
        raise HTTPException(status_code=422, detail="known_at must not be in the future")
    return value.astimezone(timezone.utc)


@router.get("/status")
def status(db: Session = Depends(get_db)):
    return service.foundation_status(db, get_settings().data_foundation_enabled)


@router.get("/catalog", dependencies=[Depends(require_enabled)])
def catalog():
    return service.catalog()


@router.get("/entities", dependencies=[Depends(require_enabled)])
def entities(q: str = Query("", max_length=256), entity_type: EntityType | None = None,
             limit: int = Query(20, ge=1, le=100), offset: int = Query(0, ge=0, le=100000), db: Session = Depends(get_db)):
    query = select(FoundationEntity)
    if q:
        query = query.where(or_(FoundationEntity.name.contains(q, autoescape=True), FoundationEntity.identifier_value.contains(q, autoescape=True)))
    if entity_type:
        query = query.where(FoundationEntity.entity_type == entity_type)
    return service.page(db, query.order_by(FoundationEntity.created_at.desc(), FoundationEntity.id), limit, offset, service.entity_read)


@router.post("/entities", status_code=201, dependencies=[Depends(require_enabled)])
def create_entity(payload: EntityCreate, db: Session = Depends(get_db)):
    return _write(db, lambda: service.create_entity(db, payload), service.entity_read)


@router.get("/entities/{entity_id}", dependencies=[Depends(require_enabled)])
def entity_detail(entity_id: str, db: Session = Depends(get_db)):
    row = _read(lambda: service.required(db, FoundationEntity, entity_id))
    listings = db.scalars(select(FoundationListing).join(FoundationSecurity,
        FoundationListing.security_id == FoundationSecurity.id).where(FoundationSecurity.entity_id == entity_id)
        .order_by(FoundationListing.market, FoundationListing.symbol)).all()
    return {**service.entity_read(row), "listings": [service.listing_read(db, listing) for listing in listings]}


@router.get("/stock-options", dependencies=[Depends(require_enabled)])
def stock_options(q: str = Query("", max_length=256), limit: int = Query(20, ge=1, le=100),
                  offset: int = Query(0, ge=0, le=100000), db: Session = Depends(get_db)):
    query = select(StockSymbol)
    if q:
        query = query.where(or_(StockSymbol.name.contains(q, autoescape=True), StockSymbol.symbol.contains(q, autoescape=True)))

    def serialize(row):
        mapping = db.scalar(select(FoundationListing).where(FoundationListing.stock_symbol_id == row.id))
        mapped_entity_id = service.required(db, FoundationSecurity, mapping.security_id).entity_id if mapping else None
        return {"id": row.id, "market": row.market, "symbol": row.symbol, "name": row.name,
                "exchange": row.exchange, "mapped_entity_id": mapped_entity_id}

    return service.page(db, query.order_by(StockSymbol.market, StockSymbol.symbol), limit, offset, serialize)


@router.post("/security-mappings", status_code=201, dependencies=[Depends(require_enabled)])
def create_mapping(payload: SecurityMappingCreate, db: Session = Depends(get_db)):
    return _write(db, lambda: service.create_mapping(db, payload), lambda row: service.listing_read(db, row))


@router.get("/evidence", dependencies=[Depends(require_enabled)])
def evidence(q: str = Query("", max_length=256), entity_id: str | None = Query(None, max_length=36),
             limit: int = Query(20, ge=1, le=100), offset: int = Query(0, ge=0, le=100000), db: Session = Depends(get_db)):
    query = select(FoundationEvidence)
    if q:
        query = query.where(or_(FoundationEvidence.title.contains(q, autoescape=True), FoundationEvidence.source_name.contains(q, autoescape=True)))
    if entity_id:
        query = query.where(FoundationEvidence.entity_id == entity_id)
    return service.page(db, query.order_by(FoundationEvidence.created_at.desc(), FoundationEvidence.id), limit, offset, service.evidence_read)


@router.post("/evidence", status_code=201, dependencies=[Depends(require_enabled)])
def create_evidence(payload: EvidenceCreate, db: Session = Depends(get_db)):
    return _write(db, lambda: service.create_evidence(db, payload), lambda row: service.evidence_read(row, full=True))


@router.post("/evidence/import-legacy", status_code=201, dependencies=[Depends(require_enabled)])
def import_legacy(payload: LegacyEvidenceImport, db: Session = Depends(get_db)):
    return _write(db, lambda: service.import_legacy_document(db, payload.document_id, payload.entity_id),
                  lambda row: service.evidence_read(row, full=True))


@router.get("/evidence/{evidence_id}", dependencies=[Depends(require_enabled)])
def evidence_detail(evidence_id: str, db: Session = Depends(get_db)):
    return service.evidence_read(_read(lambda: service.required(db, FoundationEvidence, evidence_id)), full=True)


@router.get("/facts", dependencies=[Depends(require_enabled)])
def facts(status: Literal["ALL", "PENDING", "ACCEPTED", "REJECTED"] = "ACCEPTED",
          entity_id: str | None = Query(None, max_length=36), fact_type: FactType | None = None,
          q: str = Query("", max_length=256), as_of: date | None = None, known_at: datetime | None = None,
          limit: int = Query(20, ge=1, le=100), offset: int = Query(0, ge=0, le=100000), db: Session = Depends(get_db)):
    known = _known_at(known_at)
    query = service.facts_query(status=status, entity_id=entity_id, fact_type=fact_type, q=q, as_of=as_of, known_at=known)
    return service.page(db, query, limit, offset, lambda row: service.fact_read(db, row, known_at=known))


@router.post("/facts", status_code=201, dependencies=[Depends(require_enabled)])
def create_fact(payload: FactCreate, db: Session = Depends(get_db)):
    return _write(db, lambda: service.create_fact(db, payload), lambda row: service.fact_read(db, row, full=True))


@router.get("/facts/{fact_id}", dependencies=[Depends(require_enabled)])
def fact_detail(fact_id: str, known_at: datetime | None = None, db: Session = Depends(get_db)):
    known = _known_at(known_at)
    row = db.scalar(service.facts_query(status="ALL", known_at=known).where(FoundationFact.id == fact_id))
    if row is None:
        raise HTTPException(status_code=404, detail="Fact is not available at the requested time")
    return service.fact_read(db, row, full=True, known_at=known)


@router.post("/facts/{fact_id}/review", dependencies=[Depends(require_enabled)])
def review(fact_id: str, payload: FactReviewCreate, db: Session = Depends(get_db)):
    return _write(db, lambda: service.review_fact(db, fact_id, payload), lambda row: service.fact_read(db, row, full=True))


@router.get("/stock-context", dependencies=[Depends(require_enabled)])
def stock_context(market: str = Query(min_length=1, max_length=16), symbol: str = Query(min_length=1, max_length=32),
                  db: Session = Depends(get_db)):
    listing = db.scalar(select(FoundationListing).where(FoundationListing.market == market, FoundationListing.symbol == symbol))
    if not listing:
        return {"market": market, "symbol": symbol, "mapping": None, "entity": None,
                "facts": [], "total_facts": 0, "missing_data": ["COMPANY_MAPPING_MISSING"]}
    mapping = service.listing_read(db, listing)
    entity = service.required(db, FoundationEntity, mapping["entity_id"])
    results = service.page(db, service.facts_query(entity_id=entity.id, as_of=service.utc_now().date()),
                           50, 0, lambda row: service.fact_read(db, row))
    return {"market": market, "symbol": symbol, "mapping": mapping, "entity": service.entity_read(entity),
            "facts": results["items"], "total_facts": results["total"],
            "missing_data": [] if results["total"] else ["ACCEPTED_FACTS_MISSING"]}
