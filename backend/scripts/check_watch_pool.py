"""Check watch-pool data freshness (K-line / notice / news coverage).

Run from the ``backend`` directory::

    python -m scripts.check_watch_pool
    python -m scripts.check_watch_pool --market CN_A
    python -m scripts.check_watch_pool --json

The same data is available over HTTP at ``GET /api/v1/watch-pool/status``.
A symbol is ``FRESH`` when its latest daily bar is within
``FRESH_LIMIT_DAYS`` calendar days (covers a normal weekend); otherwise it is
``STALE`` (old data) or ``NO_DATA`` (never fetched).
"""

from __future__ import annotations

import argparse
import json

import app.models  # noqa: F401 - register every ORM model in Base.metadata

from app.db.session import SessionLocal
from app.services.watch_pool_status import build_watch_pool_status


def main() -> None:
    parser = argparse.ArgumentParser(description="Check watch-pool data freshness.")
    parser.add_argument("--market", default=None, help="Only check this market (CN_A / HK / NEEQ / NEEQ_INNOVATION).")
    parser.add_argument("--json", action="store_true", help="Print raw JSON instead of the table.")
    args = parser.parse_args()

    with SessionLocal() as db:
        payload = build_watch_pool_status(db, market=args.market)

    if args.json:
        print(json.dumps(payload, ensure_ascii=False, indent=2))
        return

    rows = payload["rows"]
    summary = payload["summary"]
    print(
        f"watch pool: {summary['pool_size']} symbols | FRESH {summary['fresh']} / "
        f"STALE {summary['stale']} / NO_DATA {summary['no_data']} | as_of {payload['as_of']}"
    )
    print()
    header = f"{'代码':<24} {'名称':<12} {'最新K线':<11} {'滞后':<5} {'30日K':<6} {'7日公告':<7} {'7日新闻':<7} {'24h失败':<7} 状态"
    print(header)
    print("-" * 110)
    for row in rows:
        key = f"{row['market']}:{row['symbol']}"
        lag = f"{row['lag_days']}d" if row["lag_days"] is not None else "-"
        print(
            f"{key:<24} {(row['name'] or '-')[:10]:<12} {(row['latest_bar'] or '-'):<11} {lag:<5} "
            f"{row['bars_30d']:<6} {row['notices_7d']:<7} {row['news_7d']:<7} {row['failed_fetch_24h']:<7} {row['status']}"
        )
    print()
    print("提示：STALE/NO_DATA 说明该股票需要刷新；可运行 python -m scripts.run_watch_pool_refresh 补数据。")


if __name__ == "__main__":
    main()
