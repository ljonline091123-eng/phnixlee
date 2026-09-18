"""Standalone scheduler that submits idempotent daily jobs."""

from __future__ import annotations

import time
from datetime import datetime
from zoneinfo import ZoneInfo

from app.db.bootstrap import initialize_database
from app.db.session import SessionLocal
from app.jobs.dispatcher import DatabaseJobDispatcher


SHANGHAI = ZoneInfo("Asia/Shanghai")


def enqueue_due_jobs(now: datetime | None = None) -> int:
    local_now = now or datetime.now(SHANGHAI)
    day = local_now.date().isoformat()
    scheduled = 0
    with SessionLocal() as db:
        dispatcher = DatabaseJobDispatcher(db)
        dispatcher.enqueue(
            task_type="daily_master_sync",
            pipeline_type="master_data_sync",
            idempotency_key=f"daily_master_sync:{day}",
            payload={"business_date": day},
        )
        scheduled += 1
        if (local_now.hour, local_now.minute) >= (0, 10):
            dispatcher.enqueue(
                task_type="daily_prediction_review",
                pipeline_type="prediction_review",
                idempotency_key=f"daily_prediction_review:{day}",
                payload={"business_date": day},
            )
            scheduled += 1
        if (local_now.hour, local_now.minute) >= (0, 20):
            dispatcher.enqueue(
                task_type="daily_selection_tracking_refresh",
                pipeline_type="selection_tracking_refresh",
                idempotency_key=f"daily_selection_tracking_refresh:{day}",
                payload={"business_date": day},
            )
            scheduled += 1
    return scheduled


def main() -> None:
    initialize_database(seed_defaults=False)
    while True:
        enqueue_due_jobs()
        time.sleep(30)


if __name__ == "__main__":
    main()
