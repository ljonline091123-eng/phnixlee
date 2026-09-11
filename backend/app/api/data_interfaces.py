from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.market_data import DataInterface, DataSource
from app.schemas.market_data import DataInterfaceCreate, DataInterfaceRead, DataInterfaceUpdate

router = APIRouter(prefix="/data-interfaces", tags=["Data Interfaces"])


@router.get("", response_model=list[DataInterfaceRead])
def list_data_interfaces(
    source_id: int | None = None,
    enabled: bool | None = None,
    db: Session = Depends(get_db),
) -> list[DataInterface]:
    statement = select(DataInterface).order_by(DataInterface.source_id, DataInterface.interface_code)
    if source_id is not None:
        statement = statement.where(DataInterface.source_id == source_id)
    if enabled is not None:
        statement = statement.where(DataInterface.enabled == enabled)
    return list(db.scalars(statement).all())


@router.post("", response_model=DataInterfaceRead, status_code=status.HTTP_201_CREATED)
def create_data_interface(payload: DataInterfaceCreate, db: Session = Depends(get_db)) -> DataInterface:
    if not db.get(DataSource, payload.source_id):
        raise HTTPException(status_code=404, detail="Data source not found")
    item = DataInterface(**payload.model_dump())
    db.add(item)
    try:
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        raise HTTPException(status_code=409, detail="Interface code already exists for this source") from exc
    db.refresh(item)
    return item


@router.put("/{interface_id}", response_model=DataInterfaceRead)
def update_data_interface(
    interface_id: int,
    payload: DataInterfaceUpdate,
    db: Session = Depends(get_db),
) -> DataInterface:
    item = db.get(DataInterface, interface_id)
    if not item:
        raise HTTPException(status_code=404, detail="Data interface not found")
    for field_name, value in payload.model_dump(exclude_unset=True).items():
        setattr(item, field_name, value)
    db.commit()
    db.refresh(item)
    return item

