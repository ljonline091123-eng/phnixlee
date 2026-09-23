from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.lakehouse import DocumentChunkVersion, LakeDataset, LakeDatasetVersion, LakeLineageEvent, LakeObject
from app.schemas.lakehouse import DatasetExportRequest, DatasetQualityRequest, DocumentArchiveRequest, DocumentChunkRequest
from app.services import lakehouse

router = APIRouter(prefix="/lakehouse", tags=["Lakehouse"])

@router.get("/status")
def status(db: Session = Depends(get_db)):
    return {"storage_backend": lakehouse.storage_mode(), "filesystem_root": str(lakehouse.get_settings().lake_filesystem_root),
            "storage_health": lakehouse.storage_health(),
            "object_count": db.query(LakeObject).count(), "dataset_count": db.query(LakeDataset).count(),
            "chunk_count": db.query(DocumentChunkVersion).count(), "lineage_count": db.query(LakeLineageEvent).count()}

@router.get("/datasets")
def datasets(db: Session = Depends(get_db)):
    return [{"id": row.id, "dataset_code": row.dataset_code, "dataset_name": row.dataset_name, "layer": row.layer,
             "format": row.format, "current_version": row.current_version, "description": row.description}
            for row in db.scalars(select(LakeDataset).order_by(LakeDataset.layer, LakeDataset.dataset_code)).all()]

@router.get("/datasets/{dataset_id}/versions")
def dataset_versions(dataset_id: int, db: Session = Depends(get_db)):
    rows = db.scalars(select(LakeDatasetVersion).where(LakeDatasetVersion.dataset_id == dataset_id)
        .order_by(LakeDatasetVersion.created_at.desc())).all()
    return [{"id": row.id, "version": row.version, "object_id": row.object_id, "row_count": row.row_count,
             "schema": row.schema_json, "quality": row.quality_json, "status": row.status,
             "created_at": row.created_at} for row in rows]

@router.get("/datasets/{dataset_id}/preview")
def dataset_preview(dataset_id: int, version: str | None = None, limit: int = 50, db: Session = Depends(get_db)):
    try:
        return lakehouse.preview_dataset(db, dataset_id, version=version, limit=limit)
    except (ValueError, RuntimeError, OSError) as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc

@router.get("/objects")
def objects(layer: str | None = None, limit: int = 100, db: Session = Depends(get_db)):
    query = select(LakeObject).order_by(LakeObject.created_at.desc()).limit(min(limit, 500))
    if layer:
        query = query.where(LakeObject.layer == layer.upper())
    return [{"id": row.id, "object_uri": row.object_uri, "layer": row.layer, "bucket": row.bucket,
             "object_key": row.object_key, "content_hash": row.content_hash,
             "content_type": row.content_type, "byte_size": row.byte_size, "source_table": row.source_table,
             "source_record_id": row.source_record_id, "dataset_version": row.dataset_version,
             "created_at": row.created_at} for row in db.scalars(query).all()]

@router.get("/chunks")
def chunks_catalog(document_key: str | None = None, limit: int = 100, db: Session = Depends(get_db)):
    query = select(DocumentChunkVersion).order_by(DocumentChunkVersion.created_at.desc()).limit(min(limit, 500))
    if document_key:
        query = query.where(DocumentChunkVersion.document_key == document_key)
    return [{"id": row.id, "document_key": row.document_key, "document_id": row.document_id,
             "chunk_index": row.chunk_index, "chunk_version": row.chunk_version,
             "content_hash": row.content_hash, "text_preview": row.chunk_text[:300],
             "start_offset": row.start_offset, "end_offset": row.end_offset,
             "parser_version": row.parser_version, "embedding_model": row.embedding_model,
             "status": row.status, "section_title": (row.metadata_json or {}).get("section_title"),
             "boundary_type": (row.metadata_json or {}).get("boundary_type"),
             "chunk_method": (row.metadata_json or {}).get("chunk_method"),
             "created_at": row.created_at}
            for row in db.scalars(query).all()]

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

@router.post("/quality/assess")
def assess_quality(payload: DatasetQualityRequest, db: Session = Depends(get_db)):
    try:
        return lakehouse.assess_dataset_source(db, **payload.model_dump())
    except (ValueError, RuntimeError) as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc

@router.post("/documents/chunks")
def chunks(payload: DocumentChunkRequest, db: Session = Depends(get_db)):
    try:
        return lakehouse.create_chunks(db, **payload.model_dump())
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc

@router.post("/documents/archive")
def archive_documents(payload: DocumentArchiveRequest, db: Session = Depends(get_db)):
    try:
        return lakehouse.archive_knowledge_documents(db, **payload.model_dump())
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
