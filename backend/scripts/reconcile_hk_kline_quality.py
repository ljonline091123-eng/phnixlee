"""Cross-check invalid HK OHLC rows against the independent Sina daily feed."""
from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from sqlalchemy import select

from app.connectors.registry import get_adapter
from app.db.session import SessionLocal
from app.models.market_data import DataSource, StockKline


def invalid(row: StockKline) -> bool:
    values = (row.open_price, row.high_price, row.low_price, row.close_price)
    if any(value is None for value in values):
        return False
    open_price, high, low, close = map(float, values)
    return high < max(open_price, close, low) or low > min(open_price, close, high)


def invalid_values(open_price, high, low, close) -> bool:
    if any(value is None for value in (open_price, high, low, close)):
        return True
    return float(high) < max(map(float, (open_price, close, low))) or float(low) > min(map(float, (open_price, close, high)))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default="validation/hk-kline-reconciliation-latest.json")
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    output = Path(args.output)
    if not output.is_absolute():
        output = Path(__file__).resolve().parents[1] / output
    output.parent.mkdir(parents=True, exist_ok=True)
    report = {"applied": args.apply, "checked_at": datetime.now(timezone.utc).isoformat(), "rows": []}
    with SessionLocal() as db:
        fallback = db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE_HK_SINA"))
        rows = [row for row in db.scalars(select(StockKline).where(StockKline.market == "HK")).all() if invalid(row)]
        by_symbol: dict[str, list[StockKline]] = {}
        for row in rows:
            by_symbol.setdefault(row.symbol, []).append(row)
        adapter = get_adapter("AKSHARE_HK_SINA")
        for symbol, bad_rows in by_symbol.items():
            dates = sorted(row.trade_date.replace("-", "") for row in bad_rows)
            try:
                reference = adapter.fetch_kline("HK", symbol, "daily", dates[0], dates[-1], "")
                by_date = {row.trade_date: row for row in reference}
            except Exception as exc:
                by_date = {}
                fetch_error = str(exc)[:500]
            else:
                fetch_error = None
            for row in bad_rows:
                item = {"id": row.id, "market": row.market, "symbol": row.symbol,
                        "trade_date": row.trade_date, "original": {"open": row.open_price,
                        "high": row.high_price, "low": row.low_price, "close": row.close_price}}
                candidate = by_date.get(row.trade_date)
                if candidate and not invalid_values(candidate.open_price, candidate.high_price,
                                                     candidate.low_price, candidate.close_price):
                    item["reference"] = {"open": candidate.open_price, "high": candidate.high_price,
                        "low": candidate.low_price, "close": candidate.close_price}
                    item["status"] = "CONFIRMED_REFERENCE"
                    if args.apply:
                        original = dict(item["original"])
                        raw = dict(row.raw_payload or {})
                        raw["quality_reconciliation"] = {"method": "HK_SINA_CROSS_SOURCE",
                            "original": original, "reference": item["reference"],
                            "checked_at": report["checked_at"], "source_code": "AKSHARE_HK_SINA"}
                        row.open_price, row.high_price = candidate.open_price, candidate.high_price
                        row.low_price, row.close_price = candidate.low_price, candidate.close_price
                        row.volume, row.amount = candidate.volume, candidate.amount
                        row.raw_payload = raw
                        if fallback is not None:
                            row.source_id = fallback.id
                        item["status"] = "REPAIRED_FROM_REFERENCE"
                else:
                    item["status"] = "UNRESOLVED"
                    item["error"] = fetch_error or ("Reference row also violates OHLC constraints" if candidate else "Reference source has no matching date")
                    if args.apply:
                        raw = dict(row.raw_payload or {})
                        prior = (raw.get("quality_reconciliation") or {}).get("original")
                        if isinstance(prior, dict):
                            row.open_price, row.high_price = prior.get("open"), prior.get("high")
                            row.low_price, row.close_price = prior.get("low"), prior.get("close")
                        raw["quality_status"] = "QUARANTINED"
                        raw["quality_issue"] = "OHLC_RANGE_INCONSISTENT"
                        raw["quality_reconciliation"] = {"method": "HK_SINA_CROSS_SOURCE",
                            "checked_at": report["checked_at"], "status": "NO_MATCHING_REFERENCE"}
                        row.raw_payload = raw
                report["rows"].append(item)
        if args.apply:
            db.commit()
    report["summary"] = {status: sum(row["status"] == status for row in report["rows"])
                         for status in ("REPAIRED_FROM_REFERENCE", "CONFIRMED_REFERENCE", "UNRESOLVED")}
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"output": str(output), **report["summary"]}, ensure_ascii=False))


if __name__ == "__main__":
    main()
