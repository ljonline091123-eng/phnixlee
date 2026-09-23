from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.lakehouse import DocumentChunkVersion, LakeDataset, LakeDatasetVersion, LakeLineageEvent, LakeObject
from app.schemas.lakehouse import DatasetExportRequest, DocumentChunkRequest
from app.services import lakehouse

router = APIRouter(prefix="/lakehouse", tags=["Lakehouse"])

@router.get("/status")
def status(db: Session = Depends(get_db)):
    return {"storage_backend": lakehouse.storage_mode(), "filesystem_root": str(lakehouse.get_settings().lake_filesystem_root) if hasattr(lakehouse, "get_settings") else None,
            "object_count": db.query(LakeObject).count(), "dataset_count": db.query(LakeDataset).count(),
            "chunk_count": db.query(DocumentChunkVersion).count(), "lineage_count": db.query(LakeLineageEvent).count()}

@router.get("/datasets")
def datasets(db: Session = Depends(get_db)):
    return [{"id": row.id, "dataset_code": row.dataset_code, "dataset_name": row.dataset_name, "layer": row.layer,
             "format": row.format, "current_version": row.current_version, "description": row.description}
            for row in db.scalars(select(LakeDataset).order_by(LakeDataset.layer, LakeDataset.dataset_code)).all()]

@router.get("/lineage")
def lineage(batch_id: str | None = None, limit: int = 100, db: Session = Depends(get_db)):
    query = select(LakeLineageEvent).order_by(LakeLineageEvent.created_at.desc()).limit(min(limit, 500))
    if batch_id: query = query.where(LakeLineageEvent.batch_id == batch_id)
    return [{"id": row.id, "batch_id": row.batch_id, "upstream_type": row.upstream_type, "upstream_id": row.upstream_id,
             "downstream_type": row.downstream_type, "downstream_id": row.downstream_id, "transformation": row.transformation,
             "parser_version": row.parser_version, "dataset_version": row.dataset_version, "created_at": row.created_at}
            for row in db.scalars(query).all()]

@router.post("/datasets/export")
def export_dataset(payload: DatasetExportRequest, db: Session = Depends(get_db)):
    try: return lakehouse.export_dataset(db, **payload.model_dump())
    except (ValueError, RuntimeError) as exc: raise HTTPException(status_code=422, detail=str(exc)) from exc

@router.post("/documents/chunks")
def chunks(payload: DocumentChunkRequest, db: Session = Depends(get_db)):
    return lakehouse.create_chunks(db, **payload.model_dump())
