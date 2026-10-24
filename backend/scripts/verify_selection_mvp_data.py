"""Read-only acceptance of a persisted real-model selection run.

Does not collect data, call models, confirm candidates or create business rows.
Only the optional JSON report is written; SQLite connections use mode=ro.
"""
from __future__ import annotations

import argparse
from collections import Counter
from copy import deepcopy
from datetime import datetime, timezone
import json
from pathlib import Path
import sqlite3
import sys

import httpx

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.db.session import engine
from app.services.selection import _model_candidate_context, _validate_multi_source_output
from app.services.selection_audit import payload_hash


def main() -> None:
    parser = argparse.ArgumentParser(description="只读核验真实选股批次、引用、模型日志和原有数据保留")
    parser.add_argument("--api", default="http://127.0.0.1:8000/api/v1")
    parser.add_argument("--run-id", type=int, required=True)
    parser.add_argument("--expected-count", type=int, default=20)
    parser.add_argument("--backup", type=Path)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    assert engine.url.get_backend_name() == "sqlite", "此验收脚本仅支持本机 SQLite"
    database = Path(engine.url.database).resolve()
    dimensions = {"NEWS", "NOTICE", "FINANCIAL", "PRICE_VOLUME", "FUND_FLOW", "COMPANY", "GRAPH"}
    db = sqlite3.connect(f"{database.as_uri()}?mode=ro", uri=True)
    db.row_factory = sqlite3.Row
    db.execute("PRAGMA query_only=ON")
    rows, missing, documents, model_codes = [], Counter(), set(), set()
    with httpx.Client(base_url=args.api, timeout=30) as client:
        response = client.get(f"/selection/runs/{args.run_id}")
        response.raise_for_status()
        run = response.json()
        assert run["data_mode"] == "REAL" and run["analysis_mode"] == "MODEL_COMPLETED"
        assert run["candidate_count"] == len(run["candidates"]) == args.expected_count
        assert len({(row["market"], row["symbol"]) for row in run["candidates"]}) == args.expected_count
        for candidate in run["candidates"]:
            analysis, evidence = candidate["analysis_json"], candidate["evidence_json"]
            assert analysis["model_output_status"] == "SUCCESS", candidate["symbol"]
            assert analysis["analysis_validation_status"] == "PASSED", candidate["symbol"]
            model = analysis["model"]
            assert model["symbol"] == candidate["symbol"]
            assert {row["dimension"] for row in model["dimensions"]} == dimensions
            context = _model_candidate_context(candidate)
            _validate_multi_source_output(deepcopy(model), context, required=True)
            visible = context["evidence_json"]
            assert set(model.get("evidence_fact_ids") or []).issubset(visible["evidence_fact_ids"])
            assert set(model.get("evidence_chunk_ids") or []).issubset(row["id"] for row in visible["chunks"])
            assert evidence["identity"]["company"], candidate["symbol"]
            assert evidence["graph_paths"] and evidence["lineage"] and evidence["context_version"]
            assert analysis["skill_code"] and analysis["skill_version"]
            for dimension in model["dimensions"]:
                if dimension["status"] == "INSUFFICIENT":
                    missing[dimension["dimension"]] += 1
                    assert dimension["direction"] == "UNKNOWN", candidate["symbol"]
            log = db.execute("SELECT id,status,instance_code,provider_code,model_code,request_json "
                             "FROM model_call_log WHERE id=?", (analysis["model_call_log_id"],)).fetchone()
            assert log and log["status"] == "SUCCESS" and log["provider_code"] != "MOCK"
            assert log["instance_code"] == analysis["actual_instance_code"]
            metadata = json.loads(log["request_json"])["metadata_json"]
            assert metadata["require_real_model"] is True
            model_codes.add(log["model_code"])
            snapshots = db.execute("SELECT payload_json,evidence_json,payload_hash FROM selection_decision_snapshot "
                                   "WHERE candidate_id=?", (candidate["id"],)).fetchall()
            assert snapshots, candidate["symbol"]
            for snapshot in snapshots:
                frozen = {"payload": json.loads(snapshot["payload_json"]), "evidence": json.loads(snapshot["evidence_json"])}
                assert payload_hash(frozen) == snapshot["payload_hash"], candidate["symbol"]
            for document_id in model["evidence_document_ids"]:
                original = client.get(f"/selection/candidates/{candidate['id']}/documents/{document_id}")
                original.raise_for_status()
                proof = original.json()
                assert proof["snapshot"] is True and proof["content"].strip(), (candidate["symbol"], document_id)
            documents.update(int(row["id"]) for row in evidence["documents"])
            rows.append({"symbol": candidate["symbol"], "name": candidate["name"], "status": "PASSED",
                         "entry_date": candidate["entry_date"], "model_call_log_id": log["id"],
                         "model_code": log["model_code"], "skill_version": analysis["skill_version"],
                         "document_count": len(evidence["documents"]), "event_count": len(model["events"]),
                         "relation_count": len(evidence["graph_paths"]), "lineage_count": len(evidence["lineage"]),
                         "insufficient_dimensions": [row["dimension"] for row in model["dimensions"] if row["status"] == "INSUFFICIENT"]})

    preservation = {}
    if args.backup:
        backup = args.backup.resolve()
        assert backup.is_file(), backup
        db.execute("ATTACH DATABASE ? AS baseline", (f"{backup.as_uri()}?mode=ro",))
        tables = [row[0] for row in db.execute("SELECT name FROM baseline.sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")]
        for table in tables:
            quoted = '"' + table.replace('"', '""') + '"'
            before = db.execute(f"SELECT count(*) FROM baseline.{quoted}").fetchone()[0]
            after = db.execute(f"SELECT count(*) FROM main.{quoted}").fetchone()[0]
            assert after >= before, (table, before, after)
            primary_keys = [row[1] for row in db.execute(f"PRAGMA baseline.table_info({quoted})") if row[5]]
            absent = 0
            if primary_keys:
                match = " AND ".join('new."' + key.replace('"', '""') + '" = old."' + key.replace('"', '""') + '"' for key in primary_keys)
                absent = db.execute(f"SELECT count(*) FROM baseline.{quoted} old "
                                    f"WHERE NOT EXISTS (SELECT 1 FROM main.{quoted} new WHERE {match})").fetchone()[0]
                assert absent == 0, (table, absent)
            preservation[table] = {"before": before, "after": after, "missing_primary_keys": absent}
    db.close()
    result = {"verified_at": datetime.now(timezone.utc).isoformat(), "run_id": run["id"], "status": "PASSED",
              "candidate_count": len(rows), "passed_count": len(rows), "document_count": len(documents),
              "event_count": sum(row["event_count"] for row in rows), "model_codes": sorted(model_codes),
              "insufficient_dimensions": dict(missing), "rows": rows, "preservation": preservation,
              "scope": "引用与证据链核验；不验证全部原始数据正确性、实时性或未来收益。历史决策快照独立保留，当前分析结果以本批候选为准。"}
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({key: value for key, value in result.items() if key not in {"rows", "preservation"}}
                     | {"preserved_tables": len(preservation)}, ensure_ascii=True))


if __name__ == "__main__":
    main()
