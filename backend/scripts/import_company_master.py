"""Import a collected issuer snapshot with bounded commits; never replaces mappings."""

import argparse
from collections import Counter
from datetime import datetime, timezone
import json
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import app.models
from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session
from app.models.company_mapping import CompanyMappingState
from app.models.market_data import StockSymbol
from app.models.pipeline import PipelineRun
from app.services.company_master import import_master_batch, mapping_status, snapshot_index


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--database-url", required=True)
    parser.add_argument("--snapshot", required=True)
    parser.add_argument("--report", required=True)
    parser.add_argument("--batch-size", type=int, default=100)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    if not 1 <= args.batch_size <= 500:
        parser.error("batch-size must be 1..500")
    snapshot_path = Path(args.snapshot).resolve(strict=True)
    snapshot = json.loads(snapshot_path.read_text(encoding="utf-8"))
    if snapshot.get("source_code") != "EASTMONEY_COMPANY_MASTER":
        parser.error("Expected a company master collector snapshot")
    records, exclusions = snapshot_index(snapshot)
    engine = create_engine(args.database_url)
    if args.dry_run:
        print(json.dumps({"source_records": len(records), "exclusions": len(exclusions), "unresolved": len(snapshot.get("unresolved", [])),
            "source_failures": snapshot.get("failures", [])}))
        return
    CompanyMappingState.__table__.create(engine, checkfirst=True)
    now = datetime.now(timezone.utc)
    with Session(engine) as db:
        ids = list(db.scalars(select(StockSymbol.id).order_by(StockSymbol.id)))
        run = PipelineRun(pipeline_type="company_master_mapping", trigger_type="MANUAL", status="RUNNING", started_at=now,
            input_json={"snapshot": str(snapshot_path), "requested": len(ids), "batch_size": args.batch_size})
        db.add(run)
        db.commit()
        run_id = run.id
    totals, results = Counter(), []
    try:
        for start in range(0, len(ids), args.batch_size):
            with Session(engine) as db:
                stocks = list(db.scalars(select(StockSymbol).where(StockSymbol.id.in_(ids[start:start + args.batch_size])).order_by(StockSymbol.id)))
                result = import_master_batch(db, stocks, snapshot, records=records, exclusions=exclusions, snapshot_name=str(snapshot_path))
                totals.update(result["counts"])
                results.extend(result["items"])
                run = db.get(PipelineRun, run_id)
                run.output_json = {"processed": len(results), "requested": len(ids), "counts": dict(totals)}
                db.commit()
            print(json.dumps({"processed": len(results), "requested": len(ids), "counts": dict(totals)}), flush=True)
        with Session(engine) as db:
            summary = mapping_status(db)
            run = db.get(PipelineRun, run_id)
            run.status = "COMPLETED"
            run.completed_at = datetime.now(timezone.utc)
            run.output_json = {**summary, "counts": dict(totals), "source_failures": snapshot.get("failures", []),
                "source_status": "PARTIAL" if totals.get("SOURCE_MISSING") or totals.get("CONFLICT") else "COMPLETE"}
            db.commit()
            report = {"run_id": run_id, **run.output_json, "items": results}
    except Exception as error:
        with Session(engine) as db:
            run = db.get(PipelineRun, run_id)
            run.status, run.error_message = "FAILED", str(error)[:4000]
            run.completed_at = datetime.now(timezone.utc)
            db.commit()
        raise
    finally:
        engine.dispose()
    Path(args.report).write_text(json.dumps(report, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
    print(json.dumps({key: value for key, value in report.items() if key != "items"}, ensure_ascii=True))


if __name__ == "__main__":
    main()
