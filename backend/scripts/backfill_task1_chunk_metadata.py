"""Backfill truthful Task-1 chunk governance metadata without replacing chunks."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

if __package__ in {None, ""}:
    sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.db.migrations import ensure_compat_columns
from app.models.lakehouse import DocumentChunkVersion, LakeLineageEvent
from app.services.lakehouse import _section_title


def backfill(db: Session) -> dict[str, int]:
    lineage = {
        row.downstream_id: row
        for row in db.scalars(
            select(LakeLineageEvent)
            .where(LakeLineageEvent.downstream_type == "DOCUMENT_CHUNK")
            .order_by(LakeLineageEvent.id)
        ).all()
    }
    counts = {
        "scanned": 0,
        "embedding_status_updated": 0,
        "lineage_fields_updated": 0,
        "section_identified": 0,
        "section_not_detected": 0,
        "missing_source_marked": 0,
        "demo_orphans_quarantined": 0,
    }
    for chunk in db.scalars(select(DocumentChunkVersion)).yield_per(500):
        counts["scanned"] += 1
        model = str(chunk.embedding_model or "").strip()
        expected_kind = "HASH" if model.upper().startswith("HASH") else "SEMANTIC" if model else "NONE"
        expected_status = "HASH_ONLY" if expected_kind == "HASH" else "PENDING" if expected_kind == "SEMANTIC" else "MISSING"
        if (chunk.embedding_kind, chunk.embedding_status) != (expected_kind, expected_status):
            chunk.embedding_kind = expected_kind
            chunk.embedding_status = expected_status
            counts["embedding_status_updated"] += 1

        event = lineage.get(chunk.id)
        if event is not None and (not chunk.source_object_id or not chunk.lineage_batch_id):
            chunk.source_object_id = chunk.source_object_id or event.upstream_id
            chunk.lineage_batch_id = chunk.lineage_batch_id or event.batch_id
            counts["lineage_fields_updated"] += 1

        if not chunk.section_title:
            title = _section_title(chunk.chunk_text, 0, len(chunk.chunk_text))
            if title:
                chunk.section_title = title
                chunk.section_path_json = [title]
                chunk.section_status = "IDENTIFIED"
                counts["section_identified"] += 1
            elif event is None and not chunk.source_object_id:
                if chunk.section_status != "MISSING_SOURCE":
                    counts["missing_source_marked"] += 1
                chunk.section_status = "MISSING_SOURCE"
                if str(chunk.document_key or "").startswith("demo:"):
                    if chunk.status != "QUARANTINED":
                        counts["demo_orphans_quarantined"] += 1
                    chunk.status = "QUARANTINED"
            elif chunk.section_status in {None, "", "UNRESOLVED"}:
                chunk.section_status = "NOT_DETECTED"
                counts["section_not_detected"] += 1

        raw_metadata = chunk.metadata_json
        metadata = dict(raw_metadata) if isinstance(raw_metadata, dict) else {}
        raw_embedding = metadata.get("embedding")
        embedding_metadata = dict(raw_embedding) if isinstance(raw_embedding, dict) else {}
        if isinstance(raw_embedding, list):
            # Historical hash fingerprints were sometimes stored directly as
            # an array. Preserve them for traceability without presenting them
            # as a real semantic vector.
            embedding_metadata["legacy_hash_fingerprint"] = raw_embedding
        metadata["section_title"] = chunk.section_title
        metadata["section_path"] = list(chunk.section_path_json or [])
        metadata["section_status"] = chunk.section_status
        metadata["source_object_id"] = chunk.source_object_id
        metadata["lineage_batch_id"] = chunk.lineage_batch_id
        metadata["embedding"] = {
            **embedding_metadata,
            "model": chunk.embedding_model,
            "kind": chunk.embedding_kind,
            "status": chunk.embedding_status,
            "model_quality": "DETERMINISTIC_HASH" if chunk.embedding_kind == "HASH" else "NOT_GENERATED",
        }
        if chunk.status == "QUARANTINED":
            metadata["quality_status"] = "MISSING"
            metadata["quarantine_reason"] = "MISSING_SOURCE_LINEAGE"
        chunk.metadata_json = metadata
    db.commit()
    return counts


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--database-url", default=get_settings().database_url)
    args = parser.parse_args()
    engine = create_engine(args.database_url)
    ensure_compat_columns(engine)
    with Session(engine) as db:
        print(json.dumps(backfill(db), ensure_ascii=False))


if __name__ == "__main__":
    main()
