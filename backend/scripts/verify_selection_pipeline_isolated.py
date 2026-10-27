"""Exercise real selection I/O against a disposable consistent database copy.

Production is opened read-only; database writes and lake objects stay in the
temporary directory. No API lifespan, scheduler, mock adapter or worker starts.
The retained report deliberately excludes credentials and raw provider errors.
"""
from __future__ import annotations

import argparse
from contextlib import closing
from datetime import datetime, timezone
import json
from pathlib import Path
import sqlite3
import sys
import tempfile
import threading
import time

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from sqlalchemy import create_engine, select
from sqlalchemy.orm import sessionmaker

from app.core.config import get_settings
import app.db.session as session_module
from app.models.ai_hub import ModelCallLog
from app.models.selection import SelectionCandidate, SelectionRun
from app.services.model_errors import classify_model_error
from app.services.selection import execute_failed_selection_reanalysis


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--database", type=Path, default=Path("quant.db"))
    parser.add_argument("--run-id", type=int, default=38)
    parser.add_argument("--symbols", nargs="+", default=["688627", "300474"])
    parser.add_argument("--models", nargs="+", default=["ZHIPU_FLASH"])
    parser.add_argument("--report", type=Path, default=Path("validation/selection-reliability-20261027/live-pipeline-report.json"))
    args = parser.parse_args()
    report = {"production_database_mode": "READ_ONLY", "transport": "REAL_HTTP",
              "lake": "TEMPORARY_FILESYSTEM", "started_at": datetime.now(timezone.utc).isoformat()}
    with tempfile.TemporaryDirectory(prefix="selection-pipeline-validation-") as directory:
        copy_path = Path(directory) / "validation.db"
        print("Creating consistent isolated database copy", flush=True)
        with closing(sqlite3.connect(f"file:{args.database.resolve().as_posix()}?mode=ro", uri=True)) as source:
            with closing(sqlite3.connect(copy_path)) as target:
                source.backup(target)
                assert target.execute("PRAGMA quick_check").fetchone()[0] == "ok"
        engine = create_engine(f"sqlite:///{copy_path.as_posix()}", connect_args={"check_same_thread": False, "timeout": 30})
        factory = sessionmaker(bind=engine, expire_on_commit=False, autoflush=False)
        settings = get_settings()
        previous_factory = session_module.SessionLocal
        previous_lake = (settings.lake_storage_backend, settings.lake_filesystem_root)
        session_module.SessionLocal = factory
        settings.lake_storage_backend = "filesystem"
        settings.lake_filesystem_root = str(Path(directory) / "lake")
        stop = threading.Event()
        snapshots = []

        def observe() -> None:
            previous = None
            while not stop.wait(5):
                with factory() as db:
                    run = db.get(SelectionRun, args.run_id)
                    progress = (run.criteria_json or {}).get("progress") or {}
                    row = {key: progress.get(key) for key in (
                        "stage", "percent", "message", "heartbeat_at", "elapsed_seconds", "attempt")}
                    row["phases"] = {key: value.get("status") for key, value in (progress.get("phases") or {}).items()}
                    if row != previous:
                        snapshots.append(row)
                        print(json.dumps(row, ensure_ascii=True), flush=True)
                        previous = row

        observer = threading.Thread(target=observe, daemon=True)
        try:
            with factory() as db:
                run = db.get(SelectionRun, args.run_id)
                if run is None:
                    raise ValueError("Missing validation run")
                run.model_instance_code = args.models[0]
                run.criteria_json = {**(run.criteria_json or {}), "request": {
                    **((run.criteria_json or {}).get("request") or {}),
                    "model_instance_code": args.models[0], "model_instance_codes": args.models}}
                candidates = list(db.scalars(select(SelectionCandidate).where(
                    SelectionCandidate.run_id == run.id, SelectionCandidate.symbol.in_(args.symbols))))
                if len(candidates) != len(set(args.symbols)):
                    raise ValueError("Missing validation stocks")
                candidate_ids = [row.id for row in candidates]
                max_log_id = db.scalar(select(ModelCallLog.id).order_by(ModelCallLog.id.desc()).limit(1)) or 0
                db.commit()
            observer.start()
            started = time.monotonic()
            result = execute_failed_selection_reanalysis({"selection_run_id": args.run_id,
                "candidate_ids": candidate_ids, "force_model_retry": True})
            report["result"] = result
            report["elapsed_seconds"] = round(time.monotonic() - started, 2)
            with factory() as db:
                run = db.get(SelectionRun, args.run_id)
                criteria = run.criteria_json or {}
                report["data_sync"] = criteria.get("data_sync")
                report["model_preflight"] = {key: value for key, value in (criteria.get("model_preflight") or {}).items()
                    if key in {"status", "instance_code", "checked_at", "tested_models"}}
                report["progress"] = criteria.get("progress")
                report["stocks"] = []
                for row in db.scalars(select(SelectionCandidate).where(SelectionCandidate.id.in_(candidate_ids))):
                    analysis, evidence = row.analysis_json or {}, row.evidence_json or {}
                    report["stocks"].append({"symbol": row.symbol, "name": row.name,
                        "documents": len(evidence.get("documents") or []), "facts": len(evidence.get("facts") or []),
                        "gate": analysis.get("data_gate_status"), "status": analysis.get("model_output_status"),
                        "validation_status": analysis.get("analysis_validation_status"),
                        "attempts": [{**{key: value for key, value in attempt.items() if key != "reason"},
                                      "reason_summary": classify_model_error(attempt.get("reason"))["summary"]
                                      if attempt.get("reason") else None}
                                     for attempt in analysis.get("model_attempts") or []],
                        "current_round_attempts": [{key: value for key, value in attempt.items() if key != "reason"}
                            for attempt in analysis.get("model_attempts") or []
                            if int(attempt.get("model_call_log_id") or 0) > max_log_id],
                        "failure_category": (analysis.get("model_failure_detail") or {}).get("category"),
                        "validation_warnings": analysis.get("evidence_validation_warnings"),
                        "dimensions": [{key: value for key, value in dimension.items() if key in {"dimension", "status", "summary"}}
                                       for dimension in (analysis.get("model") or {}).get("dimensions") or []],
                        "evaluation_status": (analysis.get("llm_evaluation") or {}).get("status")})
                report["calls"] = [{"id": log.id, "task_type": log.task_type, "instance_code": log.instance_code,
                    "status": log.status, "latency_ms": log.latency_ms,
                    "purpose": (log.request_json or {}).get("metadata_json", {}).get("purpose"),
                    "transport_attempts": len((log.request_json or {}).get("attempted_models") or [])}
                    for log in db.scalars(select(ModelCallLog).where(ModelCallLog.id > max_log_id).order_by(ModelCallLog.id))]
                assert all(phase.get("status") != "RUNNING" for phase in report["progress"]["phases"].values())
                phases = report["progress"]["phases"]
                assert phases["DATA"]["completed_at"] <= phases["MODEL_PREFLIGHT"]["started_at"]
                if phases["ANALYSIS"].get("started_at"):
                    assert phases["MODEL_PREFLIGHT"]["completed_at"] <= phases["ANALYSIS"]["started_at"]
                assert not any(activity.get("stage") in {"MODEL", "MODEL_ANALYSIS", "RESULT_VALIDATION"}
                               for activity in phases["DATA"].get("activities") or [])
                for row in report["stocks"]:
                    assert len(row["current_round_attempts"]) <= 3
                    if row["gate"] == "MISSING_DATA":
                        assert row["status"] == "NOT_RUN" and not row["dimensions"]
        finally:
            stop.set()
            if observer.is_alive():
                observer.join(timeout=6)
            session_module.SessionLocal = previous_factory
            settings.lake_storage_backend, settings.lake_filesystem_root = previous_lake
            engine.dispose()
            report["observed_progress"] = snapshots
            report["completed_at"] = datetime.now(timezone.utc).isoformat()
            args.report.parent.mkdir(parents=True, exist_ok=True)
            args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
            print(f"Report: {args.report}", flush=True)


if __name__ == "__main__":
    main()
