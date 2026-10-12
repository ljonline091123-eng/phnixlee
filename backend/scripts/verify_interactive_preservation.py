"""Read-only integrity and baseline identity checks against this task's backup."""
import json
import argparse
from pathlib import Path
import sqlite3

from app.db.session import engine


def main():
    root = Path(__file__).resolve().parents[1]
    parser = argparse.ArgumentParser(description="只读核验历史表结构及记录身份是否保留")
    parser.add_argument("--backup", type=Path, default=root / "backups" / "quant-20261012-092443-pre-interactive-semantic-governance.db")
    parser.add_argument("--output", type=Path, default=root / "validation" / "interactive-semantic" / "preservation-verification.json")
    args = parser.parse_args()
    backup = args.backup.resolve()
    current = Path(engine.url.database).resolve()
    result = {"backup": str(backup), "current": str(current), "tables": [], "schema_changes": []}
    with sqlite3.connect(current.as_uri() + "?mode=ro", uri=True, timeout=30) as db:
        db.execute("PRAGMA query_only=ON")
        db.execute("ATTACH DATABASE ? AS baseline", (backup.as_uri() + "?mode=ro",))
        db.execute("BEGIN")
        result["quick_check"] = [row[0] for row in db.execute("PRAGMA quick_check")]
        violations = list(db.execute("PRAGMA foreign_key_check"))
        result["foreign_key_violation_count"] = len(violations)
        # Report identifiers only, no source texts or credentials.
        result["foreign_key_violations"] = violations[:20]
        tables = list(db.execute("SELECT name, sql FROM baseline.sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"))
        for name, original_schema in tables:
            safe_name = '"' + name.replace('"', '""') + '"'
            row = db.execute("SELECT sql FROM main.sqlite_master WHERE type='table' AND name=?", (name,)).fetchone()
            if not row:
                result["tables"].append({"table": name, "missing_table": True}); continue
            if original_schema != row[0]:
                result["schema_changes"].append(name)
            before = db.execute(f"SELECT count(*) FROM baseline.{safe_name}").fetchone()[0]
            after = db.execute(f"SELECT count(*) FROM main.{safe_name}").fetchone()[0]
            keys = [row[1] for row in db.execute(f"PRAGMA baseline.table_info({safe_name})") if row[5]]
            predicates = [f'now."{key}" IS old."{key}"' for key in keys]
            missing = db.execute(f"SELECT count(*) FROM baseline.{safe_name} old WHERE NOT EXISTS "
                                 f"(SELECT 1 FROM main.{safe_name} now WHERE {' AND '.join(predicates)})").fetchone()[0] if keys else None
            result["tables"].append({"table": name, "before": before, "after": after, "missing_baseline_identities": missing})
        db.rollback()
    result["passed"] = result["quick_check"] == ["ok"] and not violations and not result["schema_changes"] and all(
        not item.get("missing_table") and not item.get("missing_baseline_identities") for item in result["tables"])
    output = args.output.resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"passed": result["passed"], "table_count": len(tables), "quick_check": result["quick_check"],
                      "foreign_key_violations": len(violations), "schema_changes": result["schema_changes"],
                      "identity_losses": [r for r in result["tables"] if r.get("missing_baseline_identities") or r.get("missing_table")]}, ensure_ascii=False))
    assert result["passed"], f"Check {output}"


if __name__ == "__main__":
    main()
