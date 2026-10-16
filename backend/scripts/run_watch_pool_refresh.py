"""Run the watch-pool data refresh workflow (K-line / notices / news).

Examples (run from the ``backend`` directory)::

    python -m scripts.run_watch_pool_refresh
    python -m scripts.run_watch_pool_refresh --market HK
    python -m scripts.run_watch_pool_refresh --enqueue

Direct mode refreshes every watchlist symbol and prints a per-symbol summary.
``--enqueue`` submits a MANUAL job instead (the worker executes it; when it
finishes it chains the L0 detection job automatically).
"""

from __future__ import annotations

import argparse
import json
import uuid
from datetime import datetime
from zoneinfo import ZoneInfo

import app.models  # noqa: F401 - register every ORM model in Base.metadata

from app.db.session import SessionLocal
from app.orchestration.watch_pool import WatchPoolRefreshWorkflow


SHANGHAI = ZoneInfo("Asia/Shanghai")


def _enqueue(business_date: str | None, market: str | None) -> int:
    from app.jobs.dispatcher import DatabaseJobDispatcher

    with SessionLocal() as db:
        job = DatabaseJobDispatcher(db).enqueue(
            task_type="watch_pool_data_refresh",
            pipeline_type="watch_pool_data_refresh",
            idempotency_key=f"watch_pool_data_refresh:manual:{uuid.uuid4().hex[:12]}",
            payload={
                "business_date": business_date or datetime.now(SHANGHAI).date().isoformat(),
                "market": market,
                "chain_detection": False,
            },
            trigger_type="MANUAL",
        )
        db.commit()
        return job.id


def main() -> None:
    parser = argparse.ArgumentParser(description="Run the watch-pool data refresh workflow.")
    parser.add_argument("--market", default=None, help="Only refresh this market (CN_A / HK / NEEQ / NEEQ_INNOVATION).")
    parser.add_argument(
        "--as-of",
        dest="as_of",
        default=None,
        help="Business date (YYYY-MM-DD) used as the refresh end date; defaults to today.",
    )
    parser.add_argument(
        "--delay-seconds",
        dest="delay_seconds",
        type=float,
        default=0.5,
        help="Politeness delay between symbols (default 0.5).",
    )
    parser.add_argument(
        "--enqueue",
        action="store_true",
        help="Submit a MANUAL job into the queue instead of running the refresh directly.",
    )
    args = parser.parse_args()

    if args.enqueue:
        job_id = _enqueue(args.as_of, args.market)
        print(f"enqueued job id={job_id} (trigger=MANUAL)")
        print("run it with: python -m app.jobs.worker --task-type watch_pool_data_refresh --once")
        return

    result = WatchPoolRefreshWorkflow().execute(
        {
            "business_date": args.as_of or datetime.now(SHANGHAI).date().isoformat(),
            "market": args.market,
            "delay_seconds": args.delay_seconds,
            "chain_detection": False,  # manual runs never consume the daily detection key
        }
    )
    symbols = result.pop("symbols", [])
    print(json.dumps(result, ensure_ascii=False, indent=2))
    if symbols:
        print("\nper-symbol detail:")
        for entry in symbols:
            errors = "; ".join(entry.get("errors") or [])
            skipped = "; ".join(entry.get("skipped") or [])
            note = errors or skipped or "ok"
            print(
                f"  {entry['market']}:{entry['symbol']:<12} "
                f"kline={entry.get('kline_persisted', 0):<3} notice={entry.get('notice_persisted', 0):<3} "
                f"news={entry.get('news_persisted', 0):<3} {note}"
            )


if __name__ == "__main__":
    main()
