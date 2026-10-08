"""Run only the fixed-sample durable Q&A jobs, preserving checkpoints and logs."""
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone, timedelta
import argparse
import json
from pathlib import Path
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from sqlalchemy import select
from app.db.session import SessionLocal
from app.jobs.worker import JobWorker
from app.models.pipeline import ScheduledJob, PipelineRun, PipelineStageRun
from app.services.investor_qa import ensure_qa_sync, qa_sync_status, TASK_TYPE

OUT = ROOT / "validation" / "all-boards-20261008"


class SampleWorker(JobWorker):
    def __init__(self, market, symbol):
        super().__init__(task_types=(TASK_TYPE,))
        self.market, self.symbol = market, symbol

    def _claim(self, db):
        # Reuse the worker's transaction/log handling while limiting this audit
        # to one issuer. Never execute another user's queued task.
        job = db.scalar(select(ScheduledJob).where(
            ScheduledJob.task_type == TASK_TYPE,
            ScheduledJob.payload_json["market"].as_string() == self.market,
            ScheduledJob.payload_json["symbol"].as_string() == self.symbol,
            ScheduledJob.status.in_(("PENDING", "RETRY")),
            ScheduledJob.run_after <= datetime.now(timezone.utc),
        ).order_by(ScheduledJob.id).limit(1))
        if job is None:
            return None
        job.status = "RUNNING"
        job.worker_id = self.worker_id
        job.attempts += 1
        job.started_at = job.started_at or datetime.now(timezone.utc)
        job.lease_until = datetime.now(timezone.utc) + timedelta(seconds=self.lease_seconds)
        pipeline = db.get(PipelineRun, job.pipeline_run_id)
        if pipeline:
            pipeline.status = "RUNNING"
            pipeline.started_at = pipeline.started_at or job.started_at
            pipeline.current_stage = job.task_type
            pipeline.completed_at = None
            pipeline.error_message = None
        stage = db.scalar(select(PipelineStageRun).where(
            PipelineStageRun.pipeline_run_id == job.pipeline_run_id,
            PipelineStageRun.stage_code == job.task_type,
        ))
        if stage:
            stage.status = "RUNNING"
            stage.attempt = job.attempts
            stage.started_at = stage.started_at or job.started_at
            stage.lease_until = job.lease_until
            stage.next_retry_at = None
            stage.completed_at = None
            stage.error_code = None
            stage.error_message = None
        db.commit()
        return job


def run(sample, budget):
    worker = SampleWorker(sample["market"], sample["symbol"])
    with SessionLocal() as db:
        state = ensure_qa_sync(db, sample["market"], sample["symbol"], force=True)
    deadline = time.monotonic() + budget
    calls = 0
    while state.get("job_id") and time.monotonic() < deadline:
        if not worker.run_once():
            break
        calls += 1
        with SessionLocal() as db:
            state = qa_sync_status(db, sample["market"], sample["symbol"])
        if state["status"] in {"COMPLETE", "EMPTY", "RETRY", "DISABLED", "MISSING"}:
            break
    result = {**sample, "checked_at": datetime.now(timezone.utc).isoformat(), "calls": calls,
              "sync": state, "status": "SOURCE_DATA_COLLECTED" if state.get("status") == "COMPLETE"
              else "SOURCE_EMPTY_ONLY" if state.get("status") == "EMPTY" else "PARTIAL"}
    path = OUT / "qa" / f"{sample['board']}-{sample['symbol']}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(result, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--boards", default="BSE,NEEQ_BASE,NEEQ_INNOVATION")
    parser.add_argument("--workers", type=int, default=4)
    parser.add_argument("--budget", type=int, default=150)
    args = parser.parse_args()
    samples = [s for s in json.loads((OUT / "samples.json").read_text(encoding="utf-8")) if s["board"] in args.boards.split(",")]
    results = []
    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        jobs = {pool.submit(run, s, args.budget): s for s in samples}
        for future in as_completed(jobs):
            try:
                result = future.result()
            except Exception as exc:
                result = {**jobs[future], "status": "FAILED", "error": str(exc)}
            results.append(result)
            print(json.dumps({"done": len(results), "total": len(samples), "board": result["board"],
                "symbol": result["symbol"], "status": result["status"], "sync_status": result.get("sync", {}).get("status"),
                "rows": result.get("sync", {}).get("stored_count"), "error": result.get("error")}, ensure_ascii=False), flush=True)
    (OUT / "qa-summary.json").write_text(json.dumps(results, ensure_ascii=False, indent=2, default=str), encoding="utf-8")


if __name__ == "__main__":
    main()
