from datetime import datetime, timedelta, timezone

import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from app.db.base import Base
from app.jobs import worker as worker_module
from app.jobs.dispatcher import DatabaseJobDispatcher
from app.jobs.worker import JobWorker
from app.models.pipeline import ScheduledJob


@pytest.fixture
def sessions(tmp_path):
    engine = create_engine(f"sqlite:///{tmp_path / 'worker.db'}")
    Base.metadata.create_all(engine)
    yield sessionmaker(bind=engine)
    engine.dispose()


@pytest.mark.parametrize("other_status", ["PENDING", "RETRY", "RUNNING"])
def test_worker_scope_never_claims_other_task_types(sessions, other_status):
    with sessions() as db:
        dispatcher = DatabaseJobDispatcher(db)
        other = dispatcher.enqueue(task_type="daily_master_sync", idempotency_key="other")
        target = dispatcher.enqueue(task_type="company_graph_governance", idempotency_key="target")
        other_id, target_id = other.id, target.id
        other.status = other_status
        other.run_after = datetime.now(timezone.utc) - timedelta(minutes=2)
        other.lease_until = datetime.now(timezone.utc) - timedelta(minutes=1)
        db.commit()

    executed = []

    def execute(task_type, payload):
        executed.append(task_type)
        return {"ok": True}

    worker = JobWorker(task_types=("company_graph_governance",), lease_seconds=3600,
                       session_factory=sessions, task_executor=execute)
    assert worker.run_once() is True
    assert worker.run_once() is False
    assert executed == ["company_graph_governance"]
    with sessions() as db:
        other = db.get(ScheduledJob, other_id)
        assert other.status == other_status
        assert other.attempts == 0
        assert other.worker_id is None
        assert db.get(ScheduledJob, target_id).status == "COMPLETED"


@pytest.mark.parametrize("task_types", [(), ("misspelled_task",)])
def test_worker_rejects_invalid_explicit_scope(task_types):
    with pytest.raises(ValueError):
        JobWorker(task_types=task_types)


def test_worker_rejects_nonpositive_lease():
    with pytest.raises(ValueError, match="positive"):
        JobWorker(lease_seconds=0)


@pytest.mark.parametrize("has_job", [True, False])
def test_cli_once_claims_at_most_one_job(monkeypatch, has_job):
    calls = []
    monkeypatch.setattr(worker_module, "initialize_database", lambda **kwargs: calls.append(("init", kwargs)))

    class StubWorker:
        def __init__(self, **kwargs):
            calls.append(("worker", kwargs))

        def run_once(self):
            calls.append(("run", {}))
            return has_job

    monkeypatch.setattr(worker_module, "JobWorker", StubWorker)
    worker_module.main(["--task-type", "company_graph_governance", "--task-type",
                        "foundation_archive_evidence", "--lease-seconds", "3600", "--once"])
    assert calls == [
        ("init", {"seed_defaults": False}),
        ("worker", {"task_types": ("company_graph_governance", "foundation_archive_evidence"),
                    "lease_seconds": 3600}),
        ("run", {}),
    ]


@pytest.mark.parametrize("arguments", [["--task-type", "unknown"], ["--lease-seconds", "0"]])
def test_cli_rejects_invalid_options_before_initialization(monkeypatch, arguments):
    initialized = []
    monkeypatch.setattr(worker_module, "initialize_database", lambda **kwargs: initialized.append(kwargs))
    with pytest.raises(SystemExit) as error:
        worker_module.main(arguments)
    assert error.value.code == 2
    assert initialized == []


def test_cli_without_flags_keeps_continuous_global_worker(monkeypatch):
    calls = []
    monkeypatch.setattr(worker_module, "initialize_database", lambda **kwargs: calls.append(("init", kwargs)))
    monkeypatch.setattr(worker_module.time, "sleep", lambda seconds: calls.append(("sleep", seconds)))

    class StopPolling(Exception):
        pass

    class StubWorker:
        def __init__(self, **kwargs):
            calls.append(("worker", kwargs))
            self.polled = False

        def run_once(self):
            if self.polled:
                raise StopPolling
            self.polled = True
            return False

    monkeypatch.setattr(worker_module, "JobWorker", StubWorker)
    with pytest.raises(StopPolling):
        worker_module.main([])
    assert calls == [("init", {"seed_defaults": False}),
                     ("worker", {"task_types": None, "lease_seconds": 900}), ("sleep", 2)]
