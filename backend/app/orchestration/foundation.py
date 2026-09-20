"""Bounded evidence archiving through the existing durable worker."""

from __future__ import annotations

import hashlib
import json
from typing import Annotated, Any

from pydantic import BaseModel, ConfigDict, Field
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.db.session import SessionLocal
from app.jobs.dispatcher import DatabaseJobDispatcher
from app.models.ai_hub import KnowledgeDocument
from app.models.foundation import FoundationEntity
from app.services.foundation import import_legacy_document


ARCHIVE_TASK = 'foundation_archive_evidence'


class ArchiveRequest(BaseModel):
    model_config = ConfigDict(extra='forbid')
    document_ids: list[Annotated[int, Field(gt=0, strict=True)]] = Field(min_length=1, max_length=100)
    entity_id: str | None = Field(default=None, min_length=1, max_length=36)


def document_fingerprint(document: KnowledgeDocument) -> str:
    values = {
        'knowledge_base_id': document.knowledge_base_id,
        'graph_id': document.graph_id,
        'source_table': document.source_table,
        'source_record_id': document.source_record_id,
        'market': document.market,
        'symbol': document.symbol,
        'title': document.title,
        'content': document.content,
        'metadata': document.metadata_json,
    }
    raw = json.dumps(values, sort_keys=True, ensure_ascii=False, default=str)
    return hashlib.sha256(raw.encode('utf-8')).hexdigest()


def enqueue_archive(db: Session, request: ArchiveRequest):
    if not get_settings().data_foundation_enabled:
        raise ValueError('Data foundation is disabled')
    if request.entity_id and db.get(FoundationEntity, request.entity_id) is None:
        raise LookupError('Foundation entity not found')
    ids = sorted(set(request.document_ids))
    fingerprints = {}
    for document_id in ids:
        document = db.get(KnowledgeDocument, document_id)
        if document is None:
            raise LookupError(f'Knowledge document {document_id} not found')
        fingerprints[str(document_id)] = document_fingerprint(document)
    payload = {'document_ids': ids, 'entity_id': request.entity_id, 'expected_fingerprints': fingerprints}
    digest = hashlib.sha256(json.dumps(payload, sort_keys=True).encode('utf-8')).hexdigest()
    return DatabaseJobDispatcher(db).enqueue(
        task_type=ARCHIVE_TASK, idempotency_key=f'{ARCHIVE_TASK}:{digest}',
        payload=payload, trigger_type='MANUAL', pipeline_type='foundation_evidence_archive',
    )


def archive_batch(db: Session, payload: dict[str, Any]) -> dict[str, Any]:
    if not get_settings().data_foundation_enabled:
        raise ValueError('Data foundation is disabled')
    request = ArchiveRequest.model_validate({
        'document_ids': payload.get('document_ids'), 'entity_id': payload.get('entity_id'),
    })
    fingerprints = payload.get('expected_fingerprints')
    ids = sorted(set(request.document_ids))
    if not isinstance(fingerprints, dict) or set(fingerprints) != {str(item) for item in ids}:
        raise ValueError('Archive source fingerprints are required')
    # Legacy graph rebuilds can reuse numeric document IDs; verify before copying.
    for document_id in ids:
        document = db.get(KnowledgeDocument, document_id)
        if document is None or document_fingerprint(document) != fingerprints[str(document_id)]:
            raise ValueError(f'Knowledge document {document_id} changed; submit a new archive job')
    evidence_ids = [
        import_legacy_document(db, document_id, request.entity_id).id for document_id in ids
    ]
    db.flush()
    return {'workflow': ARCHIVE_TASK, 'archived_count': len(evidence_ids), 'evidence_ids': evidence_ids}


class LegacyEvidenceArchiveWorkflow:
    def execute(self, payload: dict[str, Any] | None = None) -> dict[str, Any]:
        with SessionLocal() as db:
            result = archive_batch(db, payload or {})
            db.commit()
            return result
