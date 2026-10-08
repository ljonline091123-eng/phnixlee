"""Check live Q&A APIs, existing IDs, evidence, and ingestion compatibility."""
import json
from pathlib import Path
import sqlite3

import httpx


ROOT = Path(__file__).resolve().parents[1]
BACKUP = ROOT / "backups" / "quant-20261008-185856-pre-hk-investor-qa.db"
OUTPUT = ROOT / "validation" / "hk-investor-qa-20261008"
API = "http://127.0.0.1:8000/api/v1"


def canonical_qa(row):
    result = dict(row)
    # Background refreshes may update ingestion timestamps; never ignore
    # source dates, answers, source identity, hashes or other evidence.
    for key in ("fetched_at", "updated_at"):
        result.pop(key, None)
    raw = json.loads(result["raw_payload"])
    raw.pop("fetched_at", None)
    result["raw_payload"] = raw
    return result


def main():
    with httpx.Client(timeout=45) as client:
        response = client.get(API + "/data-sources")
        response.raise_for_status()
        source = next(s for s in response.json() if s["source_code"] == "HK_ISSUER_IR")
        health = client.post(API + f"/data-sources/{source['id']}/test")
        health.raise_for_status()
        samples = []
        for symbol in ("09868", "09866", "02015", "09888", "02533"):
            response = client.get(API + f"/stocks/HK/{symbol}/research/qa")
            response.raise_for_status()
            section = response.json()
            local = client.get(API + f"/stocks/HK/{symbol}/f10", params={"local_only": "true"})
            local.raise_for_status()
            projected = next(s for s in local.json()["research_sections"]["sections"] if s["key"] == "qa")
            rows = section["rows"]
            assert len(projected["rows"]) == len(rows)
            assert projected["title"] == section["title"] == "投资者问答"
            if symbol == "02533":
                assert section["status"] == section["sync"]["status"] == "MISSING" and not rows
                assert section["source_urls"] and not section["sync"]["supported"]
            else:
                assert section["status"] == "AVAILABLE" and section["sync"]["status"] == "COMPLETE" and rows
                for row in rows:
                    assert row["source_code"] == "HK_ISSUER_IR" and row["qa_kind"] == "OFFICIAL_FAQ"
                    assert row["answer"] and row["question"] and row["source_url"].startswith("https://ir.")
                    assert len(row["content_hash"]) == len(row["source_content_hash"]) == 64
                    assert all(row[key] is None for key in ("asked_at", "answered_at", "updated_at"))
                    assert row["date_status"] == "UNDISCLOSED" and row["fetched_at"]
            samples.append({"symbol": symbol, "count": len(rows), "status": section["status"],
                            "sync": section["sync"], "sources": sorted({r["source_code"] for r in rows})})
    with sqlite3.connect(BACKUP) as old, sqlite3.connect(ROOT / "quant.db") as current:
        preservation = []
        for (table,) in old.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"):
            if not any(c[1] == "id" for c in old.execute(f'PRAGMA table_info("{table}")')):
                continue
            previous = {r[0] for r in old.execute(f'SELECT id FROM "{table}"')}
            now = {r[0] for r in current.execute(f'SELECT id FROM "{table}"')}
            preservation.append({"table": table, "before": len(previous), "after": len(now), "missing_ids": len(previous - now)})
        old.row_factory = current.row_factory = sqlite3.Row
        previous = {r["id"]: canonical_qa(r) for r in old.execute("SELECT * FROM stock_investor_qa WHERE market='CN_A'")}
        now = {r["id"]: canonical_qa(r) for r in current.execute("SELECT * FROM stock_investor_qa WHERE market='CN_A'")}
        qa_content_unchanged = all(now.get(key) == value for key, value in previous.items())
        duplicates = [tuple(row) for row in current.execute("""SELECT market,symbol,source_code,external_id,count(*)
            FROM stock_investor_qa GROUP BY market,symbol,source_code,external_id HAVING count(*)>1""")]
        integrity = current.execute("PRAGMA integrity_check").fetchone()[0]
        assert not any(t["missing_ids"] for t in preservation)
        assert qa_content_unchanged and not duplicates and integrity == "ok"
    result = {"passed": True, "sources": samples, "source_health": health.json(),
              "database_preservation": preservation, "old_a_share_qa_content_unchanged": qa_content_unchanged,
              "compatibility_basis": "Compare all values except ingestion/update timestamps and raw_payload.fetched_at",
              "duplicates": duplicates, "integrity": integrity}
    OUTPUT.mkdir(parents=True, exist_ok=True)
    (OUTPUT / "api-database-result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"passed": True, "samples": [{k: r[k] for k in ("symbol", "count", "status")} for r in samples],
                      "preserved_tables": len(preservation), "a_share_content_unchanged": qa_content_unchanged,
                      "integrity": integrity}, ensure_ascii=False))


if __name__ == "__main__":
    main()
