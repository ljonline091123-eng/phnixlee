"""Backfill missing dataset-version to lake-object lineage without replacing data."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

if __package__ in {None, ""}:
    sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.db.migrations import ensure_compat_columns
from app.db.session import engine as default_engine
from app.models.lakehouse import LakeDataset, LakeDatasetVersion, LakeLineageEvent, LakeObject


def backfill(db: Session) -> dict[str, int]:
    existing = {
        (str(row.upstream_id), str(row.downstream_id))
        for row in db.scalars(select(LakeLineageEvent).where(
            LakeLineageEvent.upstream_type == "DATASET_VERSION",
            LakeLineageEvent.downstream_type == "LAKE_OBJECT",
        )).all()
    }
    counts = {
        "versions_scanned": 0,
        "lineage_created": 0,
        "missing_object": 0,
        "source_table_repaired": 0,
    }
    rows = db.execute(
        select(LakeDatasetVersion, LakeDataset)
        .join(LakeDataset, LakeDataset.id == LakeDatasetVersion.dataset_id)
        .order_by(LakeDatasetVersion.id)
    ).all()
    for version, dataset in rows:
        counts["versions_scanned"] += 1
        if not version.object_id:
            counts["missing_object"] += 1
            continue
        obj = db.get(LakeObject, version.object_id)
        if obj is None:
            counts["missing_object"] += 1
            continue
        quality = dict(version.quality_json or {})
        metadata = dict(obj.metadata_json or {})
        source_table = str(quality.get("source_table") or metadata.get("source_table") or obj.source_table or "")
        if source_table and not obj.source_table:
            obj.source_table = source_table
            counts["source_table_repaired"] += 1
        upstream_id = f"{dataset.dataset_code}:{version.version}"
        key = (upstream_id, str(obj.id))
        if key in existing:
            continue
        batch_id = str(quality.get("batch_id") or metadata.get("batch_id") or f"dataset-lineage-{version.id}")
        db.add(LakeLineageEvent(
            batch_id=batch_id,
            upstream_type="DATASET_VERSION",
            upstream_id=upstream_id,
            downstream_type="LAKE_OBJECT",
            downstream_id=str(obj.id),
            transformation="DATASET_OBJECT_WRITE",
            dataset_version=version.version,
            metadata_json={
                "dataset_code": dataset.dataset_code,
                "source_table": source_table,
                "backfill": "TASK1_DATASET_LINEAGE_V1",
            },
        ))
        existing.add(key)
        counts["lineage_created"] += 1
    db.commit()
    return counts


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--database-url", default=get_settings().database_url)
    args = parser.parse_args()
    if args.database_url == get_settings().database_url:
        engine = default_engine
    else:
        from sqlalchemy import create_engine
        engine = create_engine(args.database_url)
    ensure_compat_columns(engine)
    with Session(engine) as db:
        print(json.dumps(backfill(db), ensure_ascii=False))


if __name__ == "__main__":
    main()
