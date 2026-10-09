"""Read-only checks of preserved IDs, live-ingestion RAW objects and lineage."""
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import sqlite3
import os
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from app.db.session import SessionLocal
from app.models.lakehouse import LakeObject
from app.services.lakehouse import read_object

OUT = Path(os.environ.get(
    "QUANT_AUDIT_OUT",
    ROOT / "validation" / "all-boards-20261008",
))
BACKUP = Path(os.environ.get(
    "QUANT_AUDIT_BACKUP",
    ROOT / "backups" / "quant-20261008-200837-pre-all-board-data-audit.db",
))


def quote_identifier(value):
    return '"' + value.replace('"', '""') + '"'


def main():
    report = {"checked_at": datetime.now(timezone.utc).isoformat(), "backup": str(BACKUP),
              "tables": [], "ingestion": [], "objects": []}
    samples = json.loads((OUT / "samples.json").read_text(encoding="utf-8"))
    with sqlite3.connect(f"file:{ROOT / 'quant.db'}?mode=ro", uri=True, timeout=30) as current, \
         sqlite3.connect(f"file:{BACKUP}?mode=ro", uri=True) as before:
        current.execute("BEGIN")
        report["integrity_check"] = [r[0] for r in current.execute("PRAGMA integrity_check")]
        report["foreign_key_check"] = current.execute("PRAGMA foreign_key_check").fetchall()
        report["backup_foreign_key_check"] = before.execute("PRAGMA foreign_key_check").fetchall()
        tables = [r[0] for r in before.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")]
        current_tables = {r[0] for r in current.execute("SELECT name FROM sqlite_master WHERE type='table'")}
        for table in tables:
            item = {"table": table, "before_count": before.execute(f"SELECT count(*) FROM {quote_identifier(table)}").fetchone()[0]}
            if table not in current_tables:
                item["status"] = "MISSING_TABLE"
            else:
                item["current_count"] = current.execute(f"SELECT count(*) FROM {quote_identifier(table)}").fetchone()[0]
                primary = [r[1] for r in sorted(before.execute(f"PRAGMA table_info({quote_identifier(table)})"), key=lambda r: r[5]) if r[5]]
                if primary:
                    columns = ",".join(quote_identifier(p) for p in primary)
                    old = set(before.execute(f"SELECT {columns} FROM {quote_identifier(table)}"))
                    now = set(current.execute(f"SELECT {columns} FROM {quote_identifier(table)}"))
                    missing = old - now
                    item.update(primary_key=primary, missing_ids=sorted(missing, key=str),
                                status="PRESERVED" if not missing else "MISSING_ORIGINAL_IDS")
                else:
                    item["status"] = "NO_PRIMARY_KEY_REQUIRES_REVIEW"
            report["tables"].append(item)
        for sample in samples:
            data = json.loads((OUT / "stocks" / f"{sample['board']}-{sample['symbol']}.json").read_text(encoding="utf-8"))
            for stage, attempts in data["attempts"].items():
                last = attempts[-1]
                item = {"board": sample["board"], "market": sample["market"], "symbol": sample["symbol"],
                        "stage": stage, "collection_status": last["status"], "objects": []}
                if last.get("log_id"):
                    refs = [("DATA_FETCH_LOG", str(last["log_id"]))]
                elif stage in {"extended", "research"} and last["status"] == "FETCHED":
                    rows = current.execute("SELECT id,section FROM stock_f10_cache WHERE market=? AND symbol=?",
                                           (sample["market"], sample["symbol"])).fetchall()
                    sections = set(last.get("sections", {})) if stage == "extended" else {"research_sections"}
                    refs = [("STOCK_F10_CACHE", str(r[0])) for r in rows if r[1] in sections]
                else:
                    refs = []
                for kind, key in refs:
                    lineage = current.execute("SELECT id,downstream_id FROM lake_lineage_event WHERE upstream_type=? AND upstream_id=? "
                        "AND downstream_type='LAKE_OBJECT' AND transformation='RAW_ARCHIVE' ORDER BY id DESC LIMIT 1", (kind, key)).fetchone()
                    item["objects"].append({"upstream_type": kind, "upstream_id": key,
                        "object_id": lineage[1] if lineage else None, "lineage_id": lineage[0] if lineage else None})
                if last["status"] != "FETCHED":
                    item["status"] = "COLLECTION_NOT_VERIFIED"
                elif not refs or any(not r["object_id"] for r in item["objects"]):
                    item["status"] = "MISSING_RAW_LINEAGE"
                else:
                    item["status"] = "RAW_LINEAGE_PRESENT"
                report["ingestion"].append(item)
        report["historical_archive_errors"] = [{"log_id": r[0], "market": r[1], "symbol": r[2],
            "interface_code": r[3], "started_at": r[4], "error": json.loads(r[5]).get("lake_archive_error")}
            for r in current.execute("SELECT id,market,symbol,interface_code,started_at,request_json FROM data_fetch_log "
                "WHERE id>1987 AND request_json LIKE '%lake_archive_error%'")]
        current.rollback()
    ids = {r["object_id"] for item in report["ingestion"] for r in item["objects"] if r["object_id"]}
    with SessionLocal() as db:
        objects = [db.get(LakeObject, key) for key in sorted(ids)]
        def verify(row):
            if row is None:
                return {"status": "OBJECT_CATALOG_MISSING"}
            result = {"id": row.id, "layer": row.layer, "source_table": row.source_table, "byte_size": row.byte_size}
            try:
                raw = read_object(row)
                result.update(actual_bytes=len(raw), hash_match=hashlib.sha256(raw).hexdigest() == row.content_hash)
                result["status"] = "PASS" if result["hash_match"] and len(raw) == row.byte_size else "HASH_OR_SIZE_MISMATCH"
                # Serialised connector records are RAW archives; the transport
                # response is only proven if the payload actually retains it.
                result["original_response_field_present"] = any(marker in raw for marker in (
                    b'"original_source_responses"', b'"original_source_response"', b'"original_response"', b'"raw_response"'))
            except Exception as exc:
                result.update(status="OBJECT_READ_FAILED", error=str(exc)[:500])
            return result
        with ThreadPoolExecutor(max_workers=4) as pool:
            report["objects"] = list(pool.map(verify, objects))
    report["status"] = "PASS" if report["integrity_check"] == ["ok"] and not report["foreign_key_check"] and all(
        r["status"] == "PRESERVED" for r in report["tables"]) and all(r["status"] == "PASS" for r in report["objects"]) and all(
        r["status"] == "RAW_LINEAGE_PRESENT" for r in report["ingestion"]) else "PARTIAL"
    (OUT / "storage-result.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"status": report["status"], "tables": len(report["tables"]), "objects": len(report["objects"]),
        "table_issues": [r for r in report["tables"] if r["status"] != "PRESERVED"],
        "object_issues": [r for r in report["objects"] if r["status"] != "PASS"],
        "missing_raw_count": sum(r["status"] == "MISSING_RAW_LINEAGE" for r in report["ingestion"]),
        "missing_raw_examples": [r for r in report["ingestion"] if r["status"] == "MISSING_RAW_LINEAGE"][:5]}, ensure_ascii=False), flush=True)


if __name__ == "__main__":
    main()
