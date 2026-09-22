"""Audit real non-news sources with Agent + Skill and retain domain KB versions."""

import argparse
import json
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import app.models  # noqa: F401
from sqlalchemy import create_engine
from sqlalchemy.orm import Session

from app.schemas.knowledge_governance import KnowledgeGovernanceRequest
from app.schemas.resource_hub import GovernanceRunRead
from app.services.knowledge_governance import governance_catalog, govern_knowledge_sources


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--database-url", required=True)
    parser.add_argument("--limit", type=int, default=50, help="Maximum materialized rows per source (1..500)")
    parser.add_argument("--instance-code", default=None, help="Enabled real model; otherwise use knowledge_graph route")
    parser.add_argument("--run-key", default=None, help="Repeat the same key/parameters to retrieve, not duplicate, a run")
    parser.add_argument("--report", required=True)
    parser.add_argument("--apply", action="store_true", help="Invoke the model and write new KB versions; default only previews sources")
    args = parser.parse_args()
    request = KnowledgeGovernanceRequest(record_limit_per_source=args.limit, instance_code=args.instance_code, run_key=args.run_key)
    engine = create_engine(args.database_url)
    try:
        with Session(engine) as db:
            report = GovernanceRunRead.model_validate(govern_knowledge_sources(db, request)).model_dump(mode="json") if args.apply else governance_catalog(db)
    finally:
        engine.dispose()
    target = Path(args.report)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(report, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
    summary = report.get("summary_json", {})
    print(json.dumps({"report": str(target), "applied": args.apply, "run_id": report.get("id"),
        "status": report.get("status", "PREVIEW"), "model_call_log_id": report.get("model_call_log_id"),
        "knowledge_bases_created": len(summary.get("created_knowledge_bases", [])),
        "documents_created": sum(item.get("documents_created", 0) for item in summary.get("source_coverage", [])),
        "error": summary.get("error")}, ensure_ascii=True))
    return 1 if report.get("status") == "FAILED" else 0


if __name__ == "__main__":
    raise SystemExit(main())
