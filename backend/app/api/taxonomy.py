"""Read-only classification and terminology master-data API."""

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.foundation import require_enabled, _read
from app.db.session import get_db
from app.services import taxonomy as service


router = APIRouter(prefix="/company-graph", tags=["Taxonomy"], dependencies=[Depends(require_enabled)])


@router.get("/taxonomies")
def list_taxonomy_definitions(
    dimension: str | None = Query(None, max_length=64),
    taxonomy: str | None = Query(None, max_length=128),
    code: str | None = Query(None, max_length=128),
    q: str | None = Query(None, max_length=256),
    db: Session = Depends(get_db),
):
    """Return the versioned definitions used to explain industry/type labels."""
    return service.list_definitions(db, dimension=dimension, taxonomy=taxonomy, code=code, q=q)


@router.get("/taxonomies/{definition_id}")
def taxonomy_definition(definition_id: str, db: Session = Depends(get_db)):
    return _read(lambda: service.get_definition(db, definition_id))
