"""Reparse preserved Tencent quotes without replacing them with older quotes.

Only fixed acceptance samples and proved legacy unit values are corrected.
The source timestamp and fetched_at are preserved; no remote fetch is claimed.
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
from app.connectors.akshare_adapter import AkshareAdapter
from app.db.session import SessionLocal
from app.models.lakehouse import LakeLineageEvent
from app.models.market_data import StockRealtimeQuote

OUT = ROOT / "validation" / "all-boards-20261008"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    samples = json.loads((OUT / "samples.json").read_text(encoding="utf-8"))
    pairs = {(s["market"], s["symbol"]) for s in samples}
    changes, batch = [], str(uuid4())
    checked_at = datetime.now(timezone.utc).isoformat()
    adapter = AkshareAdapter()
    with SessionLocal() as db:
        for row in db.scalars(select(StockRealtimeQuote)).all():
            raw = dict(row.raw_payload or {})
            if (row.market, row.symbol) not in pairs or not raw.get("parts") or not raw.get("raw"):
                continue
            parsed = adapter._parse_tencent_quote(raw["raw"], row.market, row.symbol)
            if parsed.quote_time != row.quote_time or parsed.current_price != row.current_price:
                continue
            # Require a provable original value, rather than guessing a factor
            # for another provider or replacing a newer observation.
            source_volume = float(raw["parts"][6])
            if row.volume == parsed.volume or row.volume not in (source_volume, source_volume * 100):
                continue
            change = {"id": row.id, "market": row.market, "symbol": row.symbol,
                      "quote_time": row.quote_time, "before": row.volume, "after": parsed.volume,
                      "raw_sha256": hashlib.sha256(raw["raw"].encode()).hexdigest(),
                      "original_units": raw.get("units"), "checked_at": checked_at,
                      "remote_refetched": False}
            changes.append(change)
            if args.apply:
                row.volume = parsed.volume
                row.raw_payload = {**raw, "source_method": "TENCENT_QUOTE",
                                   "units": parsed.raw_payload["units"], "local_correction": change}
                db.add(LakeLineageEvent(batch_id=batch, upstream_type="SOURCE_RECORD",
                    upstream_id=f"stock_realtime_quote:{row.id}", downstream_type="SOURCE_RECORD",
                    downstream_id=f"stock_realtime_quote:{row.id}", transformation="VOLUME_UNIT_CORRECTION",
                    parser_version="TENCENT_BOARD_VOLUME_V2", metadata_json=change))
        if args.apply:
            db.commit()
    result = {"applied": args.apply, "checked_at": checked_at, "batch_id": batch,
              "scope": "FIXED_160_AUDIT_SAMPLES", "remote_refetched": False,
              "count": len(changes), "changes": changes}
    path = OUT / ("quote-unit-correction.json" if args.apply else "quote-unit-correction-preview.json")
    path.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
