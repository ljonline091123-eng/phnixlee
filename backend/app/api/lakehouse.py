from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import or_, select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.lakehouse import DocumentChunkVersion, LakeDataset, LakeDatasetVersion, LakeLineageEvent, LakeObject
from app.schemas.lakehouse import DatasetExportRequest, DatasetQualityRequest, DocumentArchiveRequest, DocumentChunkRequest
from app.services import lakehouse

router = APIRouter(prefix="/lakehouse", tags=["Lakehouse"])


def _bounded_limit(limit: int) -> int:
    if limit < 1 or limit > 500:
        raise HTTPException(status_code=422, detail="limit must be between 1 and 500")
    return limit


def _object_read(row: LakeObject) -> dict:
    return {
        "id": row.id,
        "object_uri": row.object_uri,
        "layer": row.layer,
        "bucket": row.bucket,
        "object_key": row.object_key,
        "content_hash": row.content_hash,
        "content_type": row.content_type,
        "byte_size": row.byte_size,
        "source_code": row.source_code,
        "source_table": row.source_table,
        "source_record_id": row.source_record_id,
        "dataset_version": row.dataset_version,
        "metadata": row.metadata_json or {},
        "created_at": row.created_at,
    }


def _chunk_read(row: DocumentChunkVersion, *, include_text: bool = False) -> dict:
    metadata = row.metadata_json or {}
    embedding_metadata = metadata.get("embedding") if isinstance(metadata.get("embedding"), dict) else {}
    embedding_kind = getattr(row, "embedding_kind", None) or embedding_metadata.get("kind")
    # HASH_EMBED_V1 is a deterministic fingerprint for retrieval fallback. It is
    # deliberately exposed as HASH rather than a semantic embedding.
    if row.embedding_model == "HASH_EMBED_V1" and not embedding_kind:
        embedding_kind = "HASH"
    payload = {
        "id": row.id,
        "document_key": row.document_key,
        "document_id": row.document_id,
        "chunk_index": row.chunk_index,
        "chunk_version": row.chunk_version,
        "content_hash": row.content_hash,
        "text_preview": row.chunk_text[:300],
        "start_offset": row.start_offset,
        "end_offset": row.end_offset,
        "parser_version": row.parser_version,
        "embedding_model": row.embedding_model,
        "embedding_kind": embedding_kind or "NONE",
        "embedding_status": getattr(row, "embedding_status", None) or ("NOT_REQUESTED" if not row.embedding_model else "READY"),
        "status": row.status,
        "section_title": getattr(row, "section_title", None) or metadata.get("section_title"),
        "section_path": getattr(row, "section_path_json", None) or metadata.get("section_path") or [],
        "section_status": getattr(row, "section_status", None) or ("IDENTIFIED" if metadata.get("section_title") else "UNIDENTIFIED"),
        "source_object_id": getattr(row, "source_object_id", None),
        "lineage_batch_id": getattr(row, "lineage_batch_id", None),
        "boundary_type": metadata.get("boundary_type"),
        "chunk_method": metadata.get("chunk_method"),
        "metadata": metadata,
        "created_at": row.created_at,
    }
    if include_text:
        payload["chunk_text"] = row.chunk_text
    return payload


def _lineage_read(row: LakeLineageEvent) -> dict:
    return {
        "id": row.id,
        "batch_id": row.batch_id,
        "upstream_type": row.upstream_type,
        "upstream_id": row.upstream_id,
        "downstream_type": row.downstream_type,
        "downstream_id": row.downstream_id,
        "transformation": row.transformation,
        "parser_version": row.parser_version,
        "dataset_version": row.dataset_version,
        "metadata": row.metadata_json or {},
        "created_at": row.created_at,
    }

@router.get("/status")
def status(db: Session = Depends(get_db)):
    return {"storage_backend": lakehouse.storage_mode(), "filesystem_root": str(lakehouse.get_settings().lake_filesystem_root),
            "storage_health": lakehouse.storage_health(),
            "object_count": db.query(LakeObject).count(), "dataset_count": db.query(LakeDataset).count(),
            "chunk_count": db.query(DocumentChunkVersion).count(), "lineage_count": db.query(LakeLineageEvent).count()}

@router.get("/datasets")
def datasets(layer: str | None = None, db: Session = Depends(get_db)):
    query = select(LakeDataset).order_by(LakeDataset.layer, LakeDataset.dataset_code)
    if layer:
        query = query.where(LakeDataset.layer == layer.upper())
    return [{"id": row.id, "dataset_code": row.dataset_code, "dataset_name": row.dataset_name, "layer": row.layer,
             "format": row.format, "partition_spec": row.partition_spec or {},
             "current_version": row.current_version, "description": row.description,
             "created_at": row.created_at, "updated_at": row.updated_at}
            for row in db.scalars(query).all()]


@router.get("/datasets/{dataset_id}")
def dataset_detail(dataset_id: int, db: Session = Depends(get_db)):
    row = db.get(LakeDataset, dataset_id)
    if row is None:
        raise HTTPException(status_code=404, detail="Lake dataset not found")
    versions = db.scalars(
        select(LakeDatasetVersion)
        .where(LakeDatasetVersion.dataset_id == dataset_id)
        .order_by(LakeDatasetVersion.created_at.desc())
    ).all()
    return {
        "id": row.id, "dataset_code": row.dataset_code, "dataset_name": row.dataset_name,
        "layer": row.layer, "format": row.format, "partition_spec": row.partition_spec or {},
        "current_version": row.current_version, "description": row.description,
        "version_count": len(versions), "versions": [
            {"id": item.id, "version": item.version, "object_id": item.object_id,
             "row_count": item.row_count, "status": item.status, "quality": item.quality_json or {},
             "created_at": item.created_at}
            for item in versions[:20]
        ],
        "created_at": row.created_at, "updated_at": row.updated_at,
    }

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
def objects(layer: str | None = None, source_table: str | None = None, source_record_id: str | None = None,
            search: str | None = None, offset: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    query = select(LakeObject).order_by(LakeObject.created_at.desc()).offset(max(offset, 0)).limit(_bounded_limit(limit))
    if layer:
        query = query.where(LakeObject.layer == layer.upper())
    if source_table:
        query = query.where(LakeObject.source_table == source_table)
    if source_record_id:
        query = query.where(LakeObject.source_record_id == source_record_id)
    if search:
        pattern = f"%{search.strip()}%"
        query = query.where(or_(LakeObject.object_uri.ilike(pattern), LakeObject.object_key.ilike(pattern)))
    return [_object_read(row) for row in db.scalars(query).all()]


@router.get("/objects/{object_id}")
def object_detail(object_id: str, db: Session = Depends(get_db)):
    row = db.get(LakeObject, object_id)
    if row is None:
        raise HTTPException(status_code=404, detail="Lake object not found")
    payload = _object_read(row)
    payload["lineage"] = [
        _lineage_read(item) for item in db.scalars(
            select(LakeLineageEvent)
            .where(or_(
                LakeLineageEvent.upstream_id == object_id,
                LakeLineageEvent.downstream_id == object_id,
            ))
            .order_by(LakeLineageEvent.created_at.desc()).limit(100)
        ).all()
    ]
    return payload

@router.get("/chunks")
def chunks_catalog(document_key: str | None = None, document_id: str | None = None,
                   status_filter: str | None = None, offset: int = 0, limit: int = 100,
                   db: Session = Depends(get_db)):
    query = select(DocumentChunkVersion).order_by(
        DocumentChunkVersion.created_at.desc(), DocumentChunkVersion.chunk_index
    ).offset(max(offset, 0)).limit(_bounded_limit(limit))
    if document_key:
        query = query.where(DocumentChunkVersion.document_key == document_key)
    if document_id:
        query = query.where(DocumentChunkVersion.document_id == document_id)
    if status_filter:
        query = query.where(DocumentChunkVersion.status == status_filter.upper())
    return [_chunk_read(row) for row in db.scalars(query).all()]


@router.get("/chunks/{chunk_id}")
def chunk_detail(chunk_id: str, db: Session = Depends(get_db)):
    row = db.get(DocumentChunkVersion, chunk_id)
    if row is None:
        raise HTTPException(status_code=404, detail="Document chunk not found")
    return _chunk_read(row, include_text=True)

@router.get("/lineage")
def lineage(batch_id: str | None = None, object_type: str | None = None, object_id: str | None = None,
            direction: str = "BOTH", offset: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    query = select(LakeLineageEvent).order_by(LakeLineageEvent.created_at.desc()).offset(max(offset, 0)).limit(_bounded_limit(limit))
    if batch_id:
        query = query.where(LakeLineageEvent.batch_id == batch_id)
    if object_id:
        normalized_direction = direction.upper()
        if normalized_direction == "UPSTREAM":
            query = query.where(LakeLineageEvent.downstream_id == object_id)
            if object_type:
                query = query.where(LakeLineageEvent.downstream_type == object_type)
        elif normalized_direction == "DOWNSTREAM":
            query = query.where(LakeLineageEvent.upstream_id == object_id)
            if object_type:
                query = query.where(LakeLineageEvent.upstream_type == object_type)
        elif normalized_direction == "BOTH":
            conditions = [LakeLineageEvent.upstream_id == object_id, LakeLineageEvent.downstream_id == object_id]
            query = query.where(or_(*conditions))
            if object_type:
                query = query.where(or_(LakeLineageEvent.upstream_type == object_type,
                                        LakeLineageEvent.downstream_type == object_type))
        else:
            raise HTTPException(status_code=422, detail="direction must be UPSTREAM, DOWNSTREAM or BOTH")
    return [_lineage_read(row) for row in db.scalars(query).all()]

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
