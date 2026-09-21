"""Collect full-universe issuer identities; opens SQLite only in read-only mode."""
import argparse
import json
from pathlib import Path
import sqlite3
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from app.connectors.company_master_sources import CompanyMasterSourcesClient


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--database", required=True, help="Existing SQLite stock universe, read only")
    parser.add_argument("--cache-dir", required=True, help="Snapshot directory; use a new directory for a fresh snapshot")
    parser.add_argument("--markets", default="CN_A,HK,NEEQ,NEEQ_INNOVATION")
    parser.add_argument("--page-size", type=int, default=500)
    parser.add_argument("--delay", type=float, default=0.25)
    parser.add_argument("--no-resume", action="store_true")
    args = parser.parse_args()
    markets = set(args.markets.split(","))
    if not markets or markets - {"CN_A", "HK", "NEEQ", "NEEQ_INNOVATION"}:
        parser.error("Unsupported markets")
    database = Path(args.database).resolve(strict=True)
    with sqlite3.connect(database.as_uri() + "?mode=ro", uri=True) as connection:
        connection.row_factory = sqlite3.Row
        targets = [dict(row) for row in connection.execute("SELECT market, symbol, raw_payload, ext_json, last_synced_at FROM stock_symbol ORDER BY market, symbol") if row["market"] in markets]
    def progress(value):
        print(json.dumps(value, ensure_ascii=True), flush=True)
    with CompanyMasterSourcesClient(Path(args.cache_dir).resolve(), page_size=args.page_size, delay=args.delay, progress=progress) as client:
        result = client.collect(targets, resume=not args.no_resume)
    progress({"status": result["status"], "targets": len(targets), "records": len(result["records"]),
        "exclusions": len(result["exclusions"]), "unresolved": len(result["unresolved"]), "failures": result["failures"]})
    return 1 if result["failures"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
