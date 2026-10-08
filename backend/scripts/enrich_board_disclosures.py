"""Incremental disclosure repair; archive new remote payload, preserve F10 history."""
import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from app.connectors.akshare_adapter import AkshareAdapter
from app.connectors.hk_company_disclosures import fetch_etnet_disclosures
from app.db.session import SessionLocal
from app.services.catalog import select_data_source
from app.services.stock_on_demand import StockOnDemandService

OUT = ROOT / "validation" / "all-boards-20261008"


def repair(sample):
    # No DB connection is held across remote requests.
    if sample["market"] == "CN_A":
        evidence = AkshareAdapter()._safe_cn_control_disclosures(sample["symbol"])
        payload = {"control": evidence["rows"], "control_source": evidence["source"],
                   "control_evidence": evidence, "control_message": evidence.get("message")}
        status, count = evidence["status"], len(evidence["rows"])
    elif sample["market"] == "HK":
        payload = fetch_etnet_disclosures(sample["symbol"])
        status, count = payload["collection_status"], len(payload["major"])
    else:
        raise ValueError("本脚本仅修复已适配的A股控制披露和港股主要股东披露")
    with SessionLocal() as db:
        source = select_data_source(db, sample["market"], "F10")
        if source is None:
            raise ValueError("无启用的匹配F10数据源")
        changed = StockOnDemandService(db).upsert_f10_cache(
            source, sample["market"], sample["symbol"], "holders", payload, archive_payload=payload)
    return {**sample, "status": status, "rows": count, "updated": changed,
            "checked_at": datetime.now(timezone.utc).isoformat()}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--boards", required=True)
    parser.add_argument("--workers", type=int, default=4)
    args = parser.parse_args()
    samples = json.loads((OUT / "samples.json").read_text(encoding="utf-8"))
    samples = [s for s in samples if s["board"] in args.boards.split(",")]
    results = []
    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        jobs = {pool.submit(repair, s): s for s in samples}
        for future in as_completed(jobs):
            try:
                result = future.result()
            except Exception as exc:
                result = {**jobs[future], "status": "FAILED", "error": str(exc)}
            results.append(result)
            print(json.dumps(result, ensure_ascii=False), flush=True)
    path = OUT / ("disclosure-repair-" + args.boards.replace(",", "-") + ".json")
    path.write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
