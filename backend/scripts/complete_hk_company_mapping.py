"""Preview or atomically complete the archived HKEX current equity universe."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import app.models
from sqlalchemy import create_engine
from sqlalchemy.orm import Session
from app.models.pipeline import PipelineRun
from app.services.hk_company_coverage import apply_completion, completion_plan


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--database-url", required=True)
    parser.add_argument("--snapshot-dir", required=True)
    parser.add_argument("--report", required=True)
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--max-additions", type=int, default=100)
    args = parser.parse_args()
    if not 1 <= args.max_additions <= 5000:
        parser.error("max-additions must be 1..5000")
    folder = Path(args.snapshot_dir).resolve(strict=True)
    hkex = json.loads((folder / "hkex_security_classification.json").read_text(encoding="utf-8"))
    digest = hashlib.sha256((folder / "hkex_list_of_securities.xlsx").read_bytes()).hexdigest()
    if digest != hkex.get("content_sha256"):
        parser.error("Official XLSX checksum does not match the archived classification")
    pages = [json.loads(path.read_text(encoding="utf-8")) for path in sorted((folder / "pages").glob("HK_*.json"))]
    if not pages or len({page["raw"]["result"]["count"] for page in pages}) != 1 or sum(
            len(page["raw"]["result"]["data"]) for page in pages) != pages[0]["raw"]["result"]["count"]:
        parser.error("Company profile archive is not a complete stable snapshot")
    engine = create_engine(args.database_url)
    try:
        with Session(engine) as db:
            if args.apply:
                report = apply_completion(db, hkex, pages, snapshot_name=str(folder), max_additions=args.max_additions)
                now = datetime.now(timezone.utc)
                run = PipelineRun(pipeline_type="hk_company_mapping_completion", trigger_type="MANUAL", status="COMPLETED",
                    started_at=now, completed_at=now, input_json={"snapshot": str(folder), "hkex_sha256": digest},
                    output_json={key: value for key, value in report.items() if key != "imported"})
                db.add(run)
                db.commit()
                report["run_id"] = run.id
            else:
                report = completion_plan(db, hkex, pages)
                db.rollback()
    finally:
        engine.dispose()
    report["applied"] = args.apply
    target = Path(args.report)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(report, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
    summary = {key: value for key, value in report.items() if key not in {"records", "imported"}}
    print(json.dumps(summary, ensure_ascii=True, default=str))


if __name__ == "__main__":
    main()
