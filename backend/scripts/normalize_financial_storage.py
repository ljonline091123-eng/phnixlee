"""Audit or normalize persisted financial observations to yuan cents.

The provider payload is kept as JSON evidence, but the financial fact table is
the serving read model.  This command applies the same conservative field
contract used by ``AkshareAdapter`` to historical rows so newly fetched and
existing observations have identical precision.  It is intentionally
idempotent and defaults to a read-only audit.
"""

from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path
import sys

BACKEND_ROOT = Path(__file__).resolve().parents[1]
if str(BACKEND_ROOT) not in sys.path:
    sys.path.insert(0, str(BACKEND_ROOT))

from sqlalchemy import select

from app.connectors.akshare_adapter import AkshareAdapter
from app.db.session import SessionLocal
from app.models.market_data import StockFinancialReport


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true", help="Write normalized JSON back to stock_financial_report")
    parser.add_argument("--batch-size", type=int, default=500, help="Commit after this many changed rows")
    args = parser.parse_args()
    if args.batch_size < 1:
        parser.error("--batch-size must be positive")

    changed = 0
    scanned = 0
    changed_fields: Counter[str] = Counter()
    with SessionLocal() as session:
        rows = session.scalars(select(StockFinancialReport).order_by(StockFinancialReport.id)).yield_per(args.batch_size)
        pending = 0
        for row in rows:
            scanned += 1
            old = row.data_json if isinstance(row.data_json, dict) else {}
            normalized = AkshareAdapter._normalize_financial_payload(old)
            if old == normalized:
                continue
            changed += 1
            for key, value in normalized.items():
                if old.get(key) != value:
                    changed_fields[str(key)] += 1
            if args.apply:
                row.data_json = normalized
                pending += 1
                if pending >= args.batch_size:
                    session.commit()
                    pending = 0
        if args.apply and pending:
            session.commit()

    print(json.dumps({
        "mode": "apply" if args.apply else "audit",
        "scanned": scanned,
        "changed": changed,
        "changed_fields": changed_fields.most_common(30),
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
