"""Explicit-target company-source import with an auditable persistent run."""

import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--database-url", required=True)
    parser.add_argument("--cache-dir", required=True)
    parser.add_argument("--accept-structured", action="store_true")
    parser.add_argument("--report")
    args = parser.parse_args()
    os.environ["DATABASE_URL"] = args.database_url
    from sqlalchemy import create_engine
    from sqlalchemy.orm import Session
    import app.models
    from app.models.pipeline import PipelineRun
    from app.services.company_governance import import_cached_sources
    from app.services.company_registry import register_company_resources
    engine = create_engine(args.database_url)
    with Session(engine) as db:
        run = PipelineRun(pipeline_type="company_graph_governance", trigger_type="MANUAL",
            status="RUNNING", input_json={"cache_dir": str(Path(args.cache_dir).resolve()),
            "accept_structured": args.accept_structured}, started_at=datetime.now(timezone.utc))
        db.add(run)
        db.commit()
        run_id = run.id
        try:
            resources = register_company_resources(db)
            result = import_cached_sources(db, Path(args.cache_dir), accept_structured=args.accept_structured)
            result["resources"] = resources
            run.status = "COMPLETED" if result["mapped"] == result["requested"] else "PARTIAL"
            run.output_json = result
            run.completed_at = datetime.now(timezone.utc)
            db.commit()
        except Exception as error:
            db.rollback()
            run = db.get(PipelineRun, run_id)
            run.status = "FAILED"
            run.error_message = str(error)[:4000]
            run.completed_at = datetime.now(timezone.utc)
            db.commit()
            raise
        output = json.dumps({"run_id": run_id, **result}, ensure_ascii=False, indent=2, default=str)
        if args.report:
            Path(args.report).write_text(output, encoding="utf-8")
        print(json.dumps({"run_id": run_id, "requested": result["requested"], "mapped": result["mapped"],
            "sources": sum(len(item["evidence_ids"]) for item in result["items"])}, ensure_ascii=True))
    engine.dispose()


if __name__ == "__main__":
    main()
