"""Real queue/concurrency tests with isolated databases; no provider calls."""
import threading
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone

from app.jobs.dispatcher import DatabaseJobDispatcher
from app.jobs.governance_runner import GovernanceRunner
from app.jobs.worker import JobWorker
from app.models.pipeline import ScheduledJob
from tests.test_worker_scope import sessions


def test_managed_runner_executes_new_jobs_and_stops(sessions):
    executed = threading.Event()
    worker = JobWorker(task_types=("stock_batch_governance",), session_factory=sessions,
        task_executor=lambda task, payload: (executed.set() or {"progress": 100}))
    runner = GovernanceRunner(worker)
    runner.start()
    try:
        with sessions() as db:
            job = DatabaseJobDispatcher(db).enqueue(task_type="stock_batch_governance", idempotency_key="auto")
            identity = job.id
        runner.wakeup.set()
        assert executed.wait(5), "Queued job must execute without a separate CLI process"
    finally:
        runner.stop()
    assert not runner.thread.is_alive()
    with sessions() as db:
        assert db.get(ScheduledJob, identity).status == "COMPLETED"


def test_two_workers_claim_one_job_once(sessions):
    with sessions() as db:
        DatabaseJobDispatcher(db).enqueue(task_type="stock_batch_governance", idempotency_key="concurrent")
    barrier = threading.Barrier(2)
    calls = []
    def execute(task, payload):
        calls.append(task)
        return {"progress": 100}
    workers = [JobWorker(task_types=("stock_batch_governance",), session_factory=sessions,
                         task_executor=execute) for _ in range(2)]
    def run(worker):
        barrier.wait(timeout=5)
        return worker.run_once()
    with ThreadPoolExecutor(max_workers=2) as executor:
        results = list(executor.map(run, workers))
    assert sum(results) == 1 and calls == ["stock_batch_governance"]


def test_long_job_renews_lease_and_other_worker_cannot_reclaim(sessions):
    with sessions() as db:
        job = DatabaseJobDispatcher(db).enqueue(task_type="stock_batch_governance", idempotency_key="lease")
        identity = job.id
    started, finish = threading.Event(), threading.Event()
    def execute(task, payload):
        started.set()
        assert finish.wait(5)
        return {"progress": 100}
    first = JobWorker(task_types=("stock_batch_governance",), lease_seconds=1,
                      session_factory=sessions, task_executor=execute)
    second = JobWorker(task_types=("stock_batch_governance",), session_factory=sessions,
                       task_executor=lambda *args: {"unexpected": True})
    thread = threading.Thread(target=first.run_once)
    thread.start()
    try:
        assert started.wait(5)
        threading.Event().wait(1.4)
        with sessions() as db:
            job = db.get(ScheduledJob, identity)
            assert job.lease_until > datetime.now(timezone.utc).replace(tzinfo=None)
        assert second.run_once() is False
    finally:
        finish.set()
        thread.join(5)
    with sessions() as db:
        assert db.get(ScheduledJob, identity).attempts == 1
