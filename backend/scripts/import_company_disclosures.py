"""Append cached disclosures to existing stock/company mappings; leave facts untouched."""

import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session
import app.models
from app.models.foundation import FoundationListing, FoundationSecurity
from app.models.market_data import DataSource, StockSymbol
from app.models.pipeline import PipelineRun
from app.services.company_governance import archive_disclosures, latest_cache
from app.services.company_registry import register_company_resources


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--database-url", required=True)
    parser.add_argument("--cache-dir", required=True)
    parser.add_argument("--symbols", required=True, help="Comma-separated A-share codes with existing company mappings")
    parser.add_argument("--report", required=True)
    args = parser.parse_args()
    symbols = list(dict.fromkeys(args.symbols.split(",")))
    if not 1 <= len(symbols) <= 30 or any(len(code) != 6 or not code.isascii() or not code.isdigit() for code in symbols):
        parser.error("Use 1..30 six-digit A-share codes")
    cache = Path(args.cache_dir).resolve()
    engine = create_engine(args.database_url)
    with Session(engine) as db:
        register_company_resources(db)
        enabled = {row.source_code: row.enabled for row in db.scalars(select(DataSource))}
        result = {"requested": len(symbols), "mapped": 0, "items": [], "mode": "DISCLOSURE_ARCHIVE_ONLY"}
        for symbol in symbols:
            mapping = db.scalar(select(FoundationListing).join(StockSymbol, FoundationListing.stock_symbol_id == StockSymbol.id)
                .where(StockSymbol.market == "CN_A", StockSymbol.symbol == symbol))
            if mapping is None:
                raise ValueError(f"CN_A:{symbol} has no existing company mapping; run identity governance first")
            company_id = db.get(FoundationSecurity, mapping.security_id).entity_id
            item = {"market": "CN_A", "symbol": symbol, "company_id": company_id, "status": "MAPPED", "sources": {}}
            result["items"].append(item)
            result["mapped"] += 1
            for prefix, kind, code in (
                ("supply_chain", "SUPPLY_CHAIN_DISCLOSURE", "CNINFO_SUPPLY_CHAIN_DISCLOSURES"),
                ("business", "BUSINESS_DISCLOSURE", "CNINFO_BUSINESS_DISCLOSURES"),
                ("legal", "LEGAL_DISCLOSURE", "CNINFO_LEGAL_DISCLOSURES"),
            ):
                source = latest_cache(cache, prefix, "CN_A", symbol) if enabled.get(code) else None
                summary = {"status": source["status"] if source else "NOT_REQUESTED" if enabled.get(code) else "DISABLED",
                    "warnings": source.get("warnings", []) if source else [], "disclosure_count": 0}
                if source:
                    if source.get("source_code") != code or source.get("request", {}).get("symbol") != symbol:
                        raise ValueError(f"Cached disclosure source does not match {code}:{symbol}")
                    if source["status"] in {"SUCCESS", "PARTIAL"}:
                        summary["evidence_ids"] = archive_disclosures(db, source, company_id, kind)
                        summary["disclosure_count"] = len(summary["evidence_ids"])
                    summary["retrieved_at"] = source.get("retrieved_at")
                item["sources"][prefix] = summary
        result["source_status"] = "PARTIAL" if any(source["status"] not in {"SUCCESS", "EMPTY", "NOT_REQUESTED"}
            for item in result["items"] for source in item["sources"].values()) else "COMPLETE_WITH_COVERAGE_LIMITS"
        now = datetime.now(timezone.utc)
        run = PipelineRun(pipeline_type="company_graph_governance", trigger_type="MANUAL", status="COMPLETED",
            input_json={"mode": result["mode"], "symbols": symbols, "cache_dir": str(cache)},
            output_json=result, started_at=now, completed_at=now)
        db.add(run)
        db.commit()
        report = {"run_id": run.id, **result}
        Path(args.report).write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
        print(json.dumps({"run_id": run.id, "mapped": result["mapped"], "source_status": result["source_status"],
            "disclosures": {kind: sum(item["sources"][kind]["disclosure_count"] for item in result["items"])
                for kind in ("supply_chain", "business", "legal")}}, ensure_ascii=True))
    engine.dispose()


if __name__ == "__main__":
    main()
