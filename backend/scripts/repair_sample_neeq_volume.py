"""Correct only fixed audit samples whose preserved Push2 K-line proves lots.

No remote collection is claimed. Original lines, before/after values and a
lineage event remain available. Run without --apply to inspect the exact scope.
"""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import sys
from uuid import uuid4

from sqlalchemy import select

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from app.db.session import SessionLocal
from app.models.market_data import StockKline
from app.models.lakehouse import LakeLineageEvent

OUT = ROOT / "validation" / "all-boards-20261008"


def correction(row):
    raw = dict(row.raw_payload or {})
    line = raw.get("line")
    if not line or (raw.get("units") or {}).get("volume") == "股":
        return None
    values = str(line).split(",")
    if len(values) < 7 or values[0] != row.trade_date:
        return None
    try:
        lots = float(values[5])
    except (ValueError, TypeError):
        return None
    # Correct a proved legacy representation only; do not guess a multiplier
    # for arbitrary provider rows, or transform already corrected data twice.
    if row.volume != lots:
        return None
    return {"id": row.id, "market": row.market, "symbol": row.symbol,
            "trade_date": row.trade_date, "before": row.volume, "after": lots * 100,
            "raw_line_sha256": hashlib.sha256(line.encode()).hexdigest()}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    samples = json.loads((OUT / "samples.json").read_text(encoding="utf-8"))
    pairs = {(s["market"], s["symbol"]) for s in samples if s["market"].startswith("NEEQ")}
    changes, batch = [], str(uuid4())
    checked_at = datetime.now(timezone.utc).isoformat()
    with SessionLocal() as db:
        rows = db.scalars(select(StockKline).where(StockKline.market.in_(["NEEQ", "NEEQ_INNOVATION"]))).all()
        for row in rows:
            if (row.market, row.symbol) not in pairs:
                continue
            change = correction(row)
            if not change:
                continue
            changes.append(change)
            if args.apply:
                row.volume = change["after"]
                row.raw_payload = {**row.raw_payload, "units": {"volume": "股", "source_volume": "手（100股）", "amount": "元"},
                                   "local_correction": {**change, "checked_at": checked_at,
                                                        "method": "PRESERVED_SOURCE_LINE", "remote_refetched": False}}
                db.add(LakeLineageEvent(batch_id=batch, upstream_type="SOURCE_RECORD",
                    upstream_id=f"stock_kline:{row.id}", downstream_type="SOURCE_RECORD",
                    downstream_id=f"stock_kline:{row.id}", transformation="VOLUME_UNIT_CORRECTION",
                    parser_version="NEEQ_LOTS_TO_SHARES_V1", metadata_json=change))
        if args.apply:
            db.commit()
    result = {"applied": args.apply, "checked_at": checked_at, "batch_id": batch,
              "scope": "FIXED_40_NEEQ_AUDIT_SAMPLES", "remote_refetched": False,
              "count": len(changes), "changes": changes}
    (OUT / ("volume-correction.json" if args.apply else "volume-correction-preview.json")).write_text(
        json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({key: value for key, value in result.items() if key != "changes"}, ensure_ascii=False))


if __name__ == "__main__":
    main()
