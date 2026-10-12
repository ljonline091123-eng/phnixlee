"""Managed local governance executor; the durable database queue remains canonical."""

import logging
import threading

from app.jobs.worker import JobWorker


class GovernanceRunner:
    def __init__(self, worker=None):
        self.worker = worker or JobWorker(task_types=("stock_batch_governance",), lease_seconds=120)
        self.stop_event = threading.Event()
        self.wakeup = threading.Event()
        self.thread = threading.Thread(target=self._run, name="stock-governance-runner", daemon=True)

    def start(self):
        self.thread.start()

    def stop(self):
        self.stop_event.set()
        self.wakeup.set()
        # Finish the current document operation normally; interrupted processes
        # are recovered through the expiring lease and document checkpoints.
        self.thread.join(timeout=5)

    def _run(self):
        while not self.stop_event.is_set():
            self.wakeup.clear()
            try:
                if self.worker.run_once():
                    continue
            except Exception:
                logging.getLogger(__name__).exception("后台治理执行器异常，将继续检查持久化队列")
            self.wakeup.wait(2)


_runner: GovernanceRunner | None = None


def start_governance_runner():
    global _runner
    if _runner is None or not _runner.thread.is_alive():
        _runner = GovernanceRunner()
        _runner.start()
    return _runner


def wake_governance_runner():
    if _runner is not None:
        _runner.wakeup.set()


def governance_runner_status():
    return {"mode": "MANAGED" if _runner else "EXTERNAL",
            "available": bool(_runner and _runner.thread.is_alive()),
            "message": "后台治理执行器已启动，按队列顺序执行" if _runner and _runner.thread.is_alive()
            else "使用独立后台执行器，请确认治理 worker 已启动"}
