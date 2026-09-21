"""Collect bounded public acceptance samples without opening any database."""

import argparse
import json
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.connectors.company_sources import CompanySourcesClient
from app.services.company_sample import SAMPLE_SECURITIES


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--cache-dir", required=True)
    parser.add_argument("--section", choices=["profiles", "holders", "legal", "business", "supply_chain", "marketcap", "all"], default="profiles")
    parser.add_argument("--symbols", default="")
    parser.add_argument("--max-documents", type=int, choices=range(1, 4), default=2)
    args = parser.parse_args()
    cache = Path(args.cache_dir).resolve()
    wanted = set(filter(None, args.symbols.split(",")))
    samples = [row for row in SAMPLE_SECURITIES if not wanted or row["symbol"] in wanted]
    if wanted - {row["symbol"] for row in samples}:
        parser.error("symbols must belong to the explicit 25-security sample set")
    with CompanySourcesClient(cache_dir=cache) as client:
        for row in samples:
            market, symbol = row["market"], row["symbol"]
            calls = []
            if args.section in {"profiles", "all"}:
                calls.append(("profile", lambda: client.fetch_company_profile(market, symbol)))
            if args.section in {"holders", "all"}:
                calls.append(("holders", lambda: client.fetch_shareholders(market, symbol)))
            if market == "CN_A" and args.section in {"legal", "all"}:
                calls.append(("legal", lambda: client.fetch_legal_disclosures(symbol, max_documents=args.max_documents)))
            if market == "CN_A" and args.section in {"business", "all"}:
                calls.append(("business", lambda: client.fetch_business_disclosures(symbol, max_documents=args.max_documents)))
            if market == "CN_A" and args.section in {"supply_chain", "all"}:
                calls.append(("supply_chain", lambda: client.fetch_supply_chain_disclosures(symbol, max_documents=args.max_documents)))
            if args.section in {"marketcap", "all"}:
                calls.append(("marketcap", lambda: client.fetch_market_cap(market, symbol)))
            for stage, call in calls:
                result = call()
                print(json.dumps({"market": market, "symbol": symbol, "stage": stage,
                    "status": result.status, "records": len(result.records), "warnings": result.warnings},
                    ensure_ascii=True), flush=True)


if __name__ == "__main__":
    main()
