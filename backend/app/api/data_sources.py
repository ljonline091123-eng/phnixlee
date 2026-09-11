from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.connectors.registry import get_adapter
from app.db.session import get_db
from app.models.market_data import DataSource
from app.schemas.market_data import (
    DataSourceCreate,
    DataSourceRead,
    DataSourceTestResponse,
    DataSourceUpdate,
)

router = APIRouter(prefix="/data-sources", tags=["Data Sources"])


@router.get("", response_model=list[DataSourceRead])
def list_data_sources(db: Session = Depends(get_db)) -> list[DataSource]:
    return list(db.scalars(select(DataSource).order_by(DataSource.priority, DataSource.source_code)).all())


@router.post("", response_model=DataSourceRead, status_code=status.HTTP_201_CREATED)
def create_data_source(payload: DataSourceCreate, db: Session = Depends(get_db)) -> DataSource:
    item = DataSource(**payload.model_dump())
    db.add(item)
    try:
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        raise HTTPException(status_code=409, detail=f"Data source code already exists: {payload.source_code}") from exc
    db.refresh(item)
    return item


@router.put("/{source_id}", response_model=DataSourceRead)
def update_data_source(
    source_id: int,
    payload: DataSourceUpdate,
    db: Session = Depends(get_db),
) -> DataSource:
    item = db.get(DataSource, source_id)
    if not item:
        raise HTTPException(status_code=404, detail="Data source not found")
    for field_name, value in payload.model_dump(exclude_unset=True).items():
        setattr(item, field_name, value)
    db.commit()
    db.refresh(item)
    return item


@router.post("/{source_id}/test", response_model=DataSourceTestResponse)
def test_data_source(source_id: int, db: Session = Depends(get_db)) -> DataSourceTestResponse:
    source = db.get(DataSource, source_id)
    if not source:
        raise HTTPException(status_code=404, detail="Data source not found")
    try:
        message, capabilities = get_adapter(source.adapter_type).health_check()
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    provider_kind = str((source.config_json or {}).get("provider_kind") or "").upper()
    if provider_kind == "OFFICIAL_PENDING":
        test_status = "PENDING"
    elif provider_kind == "OPTIONAL_QUOTE" and not capabilities:
        test_status = "OPTIONAL_NOT_READY"
    else:
        test_status = "CONFIGURED"
    return DataSourceTestResponse(
        source_code=source.source_code,
        adapter_type=source.adapter_type,
        status=test_status,
        message=message,
        capabilities=capabilities,
    )
