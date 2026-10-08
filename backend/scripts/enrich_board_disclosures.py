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
from app.connectors.neeq_f10 import financial_table, fetch_neeq_f10
from app.db.session import SessionLocal
from app.services.catalog import select_data_source
from app.services.stock_on_demand import StockOnDemandService

OUT = ROOT / "validation" / "all-boards-20261008"


def repair(sample, section="holders"):
    # No DB connection is held across remote requests.
    if section == "financial_statements":
        adapter = AkshareAdapter()
        if sample["market"] == "HK":
            payload = adapter._safe_hk_financial_statements(sample["symbol"])
        elif sample["market"] in {"NEEQ", "NEEQ_INNOVATION"}:
            payload = {"source": "东方财富新三板财务报表（元）", "original_source_responses": []}
            for key, endpoint in (("income_statement", "lrb"), ("balance_sheet", "zcfzb"), ("cash_flow", "xjllb")):
                params = {"MSECUCODE": sample["symbol"], "dateType": 0}
                original = adapter._fetch_neeq_json("/api/F10/Finance/" + endpoint, params)
                if not isinstance(original, dict) or original.get("IsSuccess") not in (True, 1):
                    raise ValueError("新三板财报响应失败，不能标记完成")
                for row in original.get("result") or []:
                    for identity_key in ("MSECUCODE", "SECURITYCODE"):
                        if row.get(identity_key) and str(row[identity_key]).split(".")[0] != sample["symbol"]:
                            raise ValueError("新三板财报证券身份不符")
                url = "https://xinsanban.eastmoney.com/api/F10/Finance/" + endpoint
                payload[key] = financial_table(original, url)
                payload["original_source_responses"].append({"url": url, "params": params, "payload": original,
                    "fetched_at": datetime.now(timezone.utc).isoformat()})
        else:
            raise ValueError("本次财报补采只支持港股和新三板")
        statement_counts = {key: len((payload.get(key) or {}).get("rows") or [])
                            for key in ("income_statement", "balance_sheet", "cash_flow")}
        count = sum(statement_counts.values())
        status = "SOURCE_DATA_COLLECTED" if all(statement_counts.values()) else "PARTIAL" if count else "MISSING_REQUIRES_SOURCE_REVIEW"
        payload["collection_status"] = status
        payload["statement_counts"] = statement_counts
        payload["missing_statements"] = [key for key, value in statement_counts.items() if not value]
        if not count:
            raise ValueError("报表源未返回有效报表，不写入空完成状态")
    elif sample["market"] in {"NEEQ", "NEEQ_INNOVATION"}:
        all_sections = fetch_neeq_f10(sample["symbol"], AkshareAdapter()._fetch_neeq_json)
        payload = all_sections[section]
        # Originals are shared by the full connector response; a standalone
        # repair must carry the originals for its own archive and hashes.
        originals = all_sections.get("profile", {}).get("original_source_responses", [])
        urls = {r["url"] for r in payload.get("source_evidence", [])}
        payload["original_source_responses"] = [r for r in originals if r["url"] in urls]
        count = sum(len(payload.get(key) or []) for key in ("major", "holder_count", "capital_structure", "control"))
        status = "PARTIAL" if payload.get("source_errors") else "SOURCE_DATA_COLLECTED" if count else "MISSING_REQUIRES_SOURCE_REVIEW"
        if not count:
            raise ValueError("股东来源没有可验证记录；保留旧数据及缺失状态")
    elif sample["market"] == "CN_A":
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
            source, sample["market"], sample["symbol"], section, payload, archive_payload=payload)
    return {**sample, "status": status, "rows": count, "updated": changed,
            "checked_at": datetime.now(timezone.utc).isoformat()}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--boards", required=True)
    parser.add_argument("--workers", type=int, default=4)
    parser.add_argument("--section", choices=("holders", "financial_statements"), default="holders")
    parser.add_argument("--symbols", default="", help="仅复验指定代码，逗号分隔")
    args = parser.parse_args()
    samples = json.loads((OUT / "samples.json").read_text(encoding="utf-8"))
    samples = [s for s in samples if s["board"] in args.boards.split(",")]
    if args.symbols:
        samples = [s for s in samples if s["symbol"] in args.symbols.split(",")]
    results = []
    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        jobs = {pool.submit(repair, s, args.section): s for s in samples}
        for future in as_completed(jobs):
            try:
                result = future.result()
            except Exception as exc:
                result = {**jobs[future], "status": "FAILED", "error": str(exc)}
            results.append(result)
            print(json.dumps(result, ensure_ascii=False), flush=True)
    path = OUT / ("disclosure-repair-" + args.boards.replace(",", "-") + "-" + args.section +
                  "-" + datetime.now().strftime("%Y%m%d-%H%M%S") + ".json")
    path.write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
