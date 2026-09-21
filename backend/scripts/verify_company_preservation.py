"""Read-only row preservation audit; never prints credentials or row values."""

import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import sqlite3


def quoted(value):
    return '"' + value.replace('"', '""') + '"'


def fingerprint(row):
    values = [("bytes", item.hex()) if isinstance(item, bytes) else item for item in row]
    return hashlib.sha256(json.dumps(values, ensure_ascii=True, separators=(",", ":")).encode()).hexdigest()


def audit(baseline, current):
    results = []
    with sqlite3.connect(Path(baseline).resolve().as_uri() + "?mode=ro", uri=True) as old:
        with sqlite3.connect(Path(current).resolve().as_uri() + "?mode=ro", uri=True) as new:
            tables = old.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'").fetchall()
            current_tables = {row[0] for row in new.execute("SELECT name FROM sqlite_master WHERE type='table'")}
            for (table,) in tables:
                if table == "alembic_version":
                    continue
                columns = [row[1] for row in old.execute("PRAGMA table_info(" + quoted(table) + ")")]
                query = "SELECT " + ",".join(map(quoted, columns)) + " FROM " + quoted(table)
                before = Counter(map(fingerprint, old.execute(query)))
                after = Counter(map(fingerprint, new.execute(query))) if table in current_tables else Counter()
                missing = sum((before - after).values())
                results.append({"table": table, "baseline_rows": sum(before.values()),
                    "current_rows": sum(after.values()), "missing_or_changed_rows": missing,
                    "additional_rows": sum((after - before).values())})
            integrity = new.execute("PRAGMA integrity_check").fetchone()[0]
    return {"baseline": str(Path(baseline).resolve()), "current": str(Path(current).resolve()),
        "integrity": integrity, "preserved": integrity == "ok" and not any(row["missing_or_changed_rows"] for row in results),
        "tables": results}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--baseline", required=True)
    parser.add_argument("--current", required=True)
    parser.add_argument("--report")
    args = parser.parse_args()
    result = audit(args.baseline, args.current)
    if args.report:
        Path(args.report).write_text(json.dumps(result, indent=2, ensure_ascii=False), encoding="utf-8")
    print(json.dumps(result, ensure_ascii=True))
    raise SystemExit(0 if result["preserved"] else 1)


if __name__ == "__main__":
    main()
