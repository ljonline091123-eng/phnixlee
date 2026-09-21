"""Read-only HTTP acceptance for the fixed real-data security/company sample."""

import argparse
from collections import Counter
from datetime import date, timedelta
import hashlib
import json
from pathlib import Path
import sys

import requests

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from app.services.company_sample import SAMPLE_SECURITIES


def verify(base_url):
    checks = []
    session = requests.Session()
    session.trust_env = False

    def get(path, **params):
        response = session.get(base_url.rstrip("/") + path, params=params, timeout=60)
        response.raise_for_status()
        return response.json()

    def check(label, condition):
        checks.append({"check": label, "passed": bool(condition)})

    samples = get("/company-graph/samples", limit=30)
    expected = {(row["market"], row["symbol"]) for row in SAMPLE_SECURITIES}
    check("fixed_25_sample_denominator", samples["total"] == 25 and
        {(row["market"], row["symbol"]) for row in samples["items"]} == expected)
    check("all_25_mapped", all(row["mapping_status"] == "MAPPED" for row in samples["items"]))
    profiles = {}
    report_samples = []
    evidence = {}
    for item in SAMPLE_SECURITIES:
        profile = get("/company-graph/by-symbol", **item)
        profiles[item["market"], item["symbol"]] = profile
        issuer = profile["company"]["id"]
        reverse = get("/company-graph/companies/" + issuer)
        selected = profile["selected_security"]
        check(item["market"] + ":" + item["symbol"] + "_dual_identity",
            reverse["company"]["id"] == issuer and any(row["stock_symbol_id"] == selected["stock_symbol_id"] for row in reverse["securities"]))
        all_facts = [fact for rows in profile["facts"].values() for fact in rows]
        size_rows = [row for row in profile["classifications"] if row["dimension"] == "SIZE" and row["security_id"] == selected["security_id"]]
        check(item["symbol"] + "_size_listing_scope", len(size_rows) == (1 if item["market"] == "CN_A" else 0))
        for row in size_rows:
            check(item["symbol"] + "_size_snapshot_only", row["method"] == "COMPUTED_SOURCE_RULE" and
                row["definition_version"] == "CNY_MARKET_CAP_ABSOLUTE_V1" and row["valid_from"] is not None and
                row["valid_to"] == (date.fromisoformat(row["valid_from"]) + timedelta(days=1)).isoformat())
        check(item["symbol"] + "_facts_have_evidence", all(row["evidence_ids"] for row in all_facts))
        for row in profile["evidence"]:
            evidence[row["id"]] = row
        for fact in profile["facts"]["ownership"]:
            props = fact["properties_json"]
            if props.get("temporal_scope") == "AS_OF_OBSERVATION":
                check(item["symbol"] + "_holder_" + fact["id"], fact["valid_from"] == props["report_date"] and
                    fact["valid_to"] == (date.fromisoformat(props["report_date"]) + timedelta(days=1)).isoformat())
            if fact["fact_type"] == "CONTROLS":
                check(item["symbol"] + "_control_not_auto_accepted", fact["status"] == "PENDING")
        report_samples.append({**item, "stock_symbol_id": selected["stock_symbol_id"], "name": selected["name"],
            "company_id": issuer, "company_name": profile["company"]["name"],
            "source_count": len(profile["sources"]), "evidence_count": len(profile["evidence"]),
            "legal_disclosure_count": len(profile["legal_disclosures"]),
            "business_disclosure_count": len(profile["business_disclosures"]),
            "coverage_missing": profile["coverage"]["missing"]})
    check("20_issuer_companies", len({row["company_id"] for row in report_samples}) == 20)
    for ashare, hshare in [("601318", "02318"), ("600036", "03968"), ("002594", "01211"), ("601899", "02899"), ("601398", "01398")]:
        check("AH_" + ashare + "_" + hshare, profiles["CN_A", ashare]["company"]["id"] == profiles["HK", hshare]["company"]["id"])
    bank, insurer = profiles["CN_A", "000001"], profiles["CN_A", "601318"]
    graph = get("/company-graph/graph", stock_symbol_id=bank["selected_security"]["stock_symbol_id"], max_nodes=200, max_edges=400)
    ownership = [row for row in graph["edges"] if row["type"] == "HOLDS_EQUITY" and
        row["source"] == insurer["company"]["id"] and row["target"] == bank["company"]["id"]]
    check("pingan_holding_direction", len(ownership) == 1 and bool(ownership[0]["evidence_ids"]))
    check("pingan_AH_nodes", all(any(node["id"] == profiles[market, symbol]["selected_security"]["security_id"]
        for node in graph["nodes"]) for market, symbol in [("CN_A", "000001"), ("CN_A", "601318"), ("HK", "02318")]))
    check("no_pending_control_in_default_graph", not any(row["type"] == "CONTROLS" for row in graph["edges"]))
    for row in evidence.values():
        detail = get("/foundation/evidence/" + row["id"])
        check("content_hash_" + row["id"], hashlib.sha256(detail["content"].encode()).hexdigest() == row["content_hash"])
    legal = profiles["CN_A", "600221"]["legal_disclosures"]
    check("HNA_real_legal_pdf_pages", bool(legal) and any(row["metadata_json"].get("pages") for row in legal))
    check("legal_disclosure_not_issuer_party", all(row["association_role"] == "DISCLOSING_COMPANY_NOT_CONFIRMED_CASE_PARTY" for row in legal))
    business = profiles["CN_A", "601766"]["business_disclosures"]
    check("CRRC_real_business_pdf_pages", bool(business) and any(row["metadata_json"].get("pages") for row in business))
    check("business_disclosure_not_invented_transaction", all(row["association_role"] == "DISCLOSING_COMPANY_NOT_VERIFIED_TRANSACTION_PARTY" for row in business))
    governance = get("/company-governance/status")
    restricted = {row["code"]: row for row in governance["sources"]}
    check("official_sources_require_authorization", all(not restricted[code]["enabled"] and
        restricted[code]["authorization_status"] == "AUTH_REQUIRED" for code in ("GSXT_REGISTRATION", "COURT_JUDICIAL")))
    status = get("/company-graph/status")
    return {"passed": all(row["passed"] for row in checks), "checks": checks, "samples": report_samples,
        "issuer_companies": len({row["company_id"] for row in report_samples}), "status": status,
        "unique_evidence": len(evidence), "evidence_by_source": dict(Counter(row["source_name"] for row in evidence.values())),
        "missing_dimensions_are_unknown": True, "graph": {"nodes": len(graph["nodes"]), "edges": len(graph["edges"]), "truncated": graph["truncated"]}}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--report")
    args = parser.parse_args()
    result = verify(args.base_url)
    if args.report:
        Path(args.report).write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({key: value for key, value in result.items() if key not in {"samples", "checks"}}, ensure_ascii=True))
    print(json.dumps({"check_count": len(result["checks"]), "failed": [row for row in result["checks"] if not row["passed"]]}))
    raise SystemExit(0 if result["passed"] else 1)


if __name__ == "__main__":
    main()
