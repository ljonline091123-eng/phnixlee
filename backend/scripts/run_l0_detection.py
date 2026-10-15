"""Run L0 deterministic change detection over the watch pool.

Examples (run from the ``backend`` directory):

    python -m scripts.run_l0_detection
    python -m scripts.run_l0_detection --market CN_A --show-events 20
    python -m scripts.run_l0_detection --as-of 2026-10-08
    python -m scripts.run_l0_detection --show-pool        # per-symbol scan detail
    python -m scripts.run_l0_detection --enqueue     # submit to the job queue instead

The script is idempotent: re-running it never duplicates events because each
event carries a ``market:symbol:event_type:event_date`` dedup key.
"""

from __future__ import annotations

import argparse
import json
import uuid
from datetime import date

import app.models  # noqa: F401 - register every ORM model in Base.metadata
from sqlalchemy import func, select

from app.db.session import SessionLocal
from app.models.change_event import StockChangeEvent
from app.services.change_detection import run_change_detection


def _enqueue(as_of: str | None) -> int:
    """Submit a MANUAL l0_change_detection job into the pipeline queue."""
    from app.jobs.dispatcher import DatabaseJobDispatcher

    with SessionLocal() as db:
        job = DatabaseJobDispatcher(db).enqueue(
            task_type="l0_change_detection",
            pipeline_type="l0_change_detection",
            idempotency_key=f"l0_change_detection:manual:{uuid.uuid4().hex[:12]}",
            payload={"business_date": as_of or date.today().isoformat()},
            trigger_type="MANUAL",
        )
        db.commit()
        return job.id


def main() -> None:
    parser = argparse.ArgumentParser(description="Run L0 change detection over the watch pool.")
    parser.add_argument("--market", default=None, help="Only scan this market (CN_A / HK / NEEQ / NEEQ_INNOVATION).")
    parser.add_argument(
        "--as-of",
        dest="as_of",
        default=None,
        help="Limit bars to trade dates <= YYYY-MM-DD for deterministic replay.",
    )
    parser.add_argument(
        "--show-events",
        type=int,
        default=10,
        help="Show the most recent N stored events after the run (0 disables).",
    )
    parser.add_argument(
        "--show-pool",
        action="store_true",
        help="Also print every pool symbol's scan result (EVENTS / NO_EVENTS / SKIPPED_STALE / SKIPPED_NO_KLINE / ERROR).",
    )
    parser.add_argument(
        "--enqueue",
        action="store_true",
        help="Submit a MANUAL job into the queue instead of running detection directly.",
    )
    args = parser.parse_args()

    if args.enqueue:
        job_id = _enqueue(args.as_of)
        print(f"enqueued job id={job_id} (trigger=MANUAL)")
        print("run it with: python -m app.jobs.worker --task-type l0_change_detection --once")
        return

    with SessionLocal() as db:
        result = run_change_detection(db, as_of=args.as_of, market=args.market, include_details=args.show_pool)
        symbols = result.pop("symbols", None)
        errors = result.pop("errors", [])
        print(json.dumps(result, ensure_ascii=False, indent=2))
        if symbols:
            print("\npool scan detail (per symbol):")
            for entry in symbols:
                events = ",".join(entry.get("event_types") or [])
                counts = ""
                if entry["status"] == "EVENTS":
                    counts = f" created={entry.get('created', 0)} dup={entry.get('duplicates', 0)}"
                print(
                    f"  {entry['market']}:{entry['symbol']:<12} {entry['status']:<18} "
                    f"{entry.get('event_date') or '-':<12} {events}{counts}"
                )
        if errors:
            print("error samples:")
            for item in errors:
                print("  -", item)

        if args.show_events > 0:
            rows = list(
                db.scalars(
                    select(StockChangeEvent)
                    .order_by(StockChangeEvent.event_date.desc(), StockChangeEvent.id.desc())
                    .limit(args.show_events)
                ).all()
            )
            total = db.scalar(select(func.count(StockChangeEvent.id))) or 0
            print(f"\nlatest {len(rows)} stored events (total {total}):")
            for row in rows:
                signal = json.dumps(row.signal_json, ensure_ascii=False)
                print(
                    f"  [{row.event_date}] {row.market}:{row.symbol} "
                    f"{row.event_type} {row.direction} :: {signal[:120]}"
                )


if __name__ == "__main__":
    main()
