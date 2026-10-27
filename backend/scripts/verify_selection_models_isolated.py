"""Send real model requests while keeping the production database read-only.

Provider credentials are copied only into a temporary local database and are
never printed or included in the retained validation report.
"""
from __future__ import annotations

import argparse
from copy import deepcopy
from datetime import datetime, timezone
import json
from pathlib import Path
import sys
import tempfile
import time

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session, sessionmaker

from app.db.base import Base
import app.db.session as session_module
from app.models.ai_hub import ModelCallLog, ModelInstance, ModelProvider, ModelSkill
from app.models.selection import SelectionCandidate, SelectionRun
from app.services.model_errors import classify_model_error
from app.services.selection import _apply_model_analysis, _evidence_data_gate, probe_selection_model


def clone(row):
    return type(row)(**{column.name: deepcopy(getattr(row, column.name)) for column in row.__table__.columns})


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--database", type=Path, default=Path("quant.db"))
    parser.add_argument("--models", nargs="+", default=["ZHIPU_FLASH", "GEMINI_FLASH"])
    parser.add_argument("--run-id", type=int, default=38)
    parser.add_argument("--symbol", default="688627")
    parser.add_argument("--analyze", action="store_true")
    parser.add_argument("--report", type=Path, default=Path("validation/selection-reliability-20261027/live-model-report.json"))
    args = parser.parse_args()
    report = {"production_database_mode": "READ_ONLY", "transport": "REAL_HTTP",
              "started_at": datetime.now(timezone.utc).isoformat(), "probes": [], "analysis": None}
    readonly_engine = create_engine(f"sqlite:///file:{args.database.resolve().as_posix()}?mode=ro&uri=true")
    with Session(readonly_engine) as source:
        models = list(source.scalars(select(ModelInstance).where(ModelInstance.instance_code.in_(args.models))))
        providers = [clone(row) for row in source.scalars(select(ModelProvider).where(
            ModelProvider.id.in_({model.provider_id for model in models}))) ]
        models = [clone(row) for row in models]
        skills = [clone(row) for row in source.scalars(select(ModelSkill).where(
            ModelSkill.skill_code.in_(["STOCK_SELECTION_ANALYST", "STOCK_EVENT_LINKAGE_ANALYST"]))) ]
        old_run = source.get(SelectionRun, args.run_id)
        candidate = source.scalar(select(SelectionCandidate).where(
            SelectionCandidate.run_id == args.run_id, SelectionCandidate.symbol == args.symbol))
        if not old_run or not candidate:
            raise ValueError("找不到只读验证样本")
        item = {key: deepcopy(getattr(candidate, key)) for key in (
            "market", "symbol", "name", "entry_price", "entry_date", "hard_score", "hard_rules_json", "evidence_json")}
        item["analysis_json"] = {}
        contract = (old_run.criteria_json or {}).get("analysis_contract", "MULTI_SOURCE_V2")
        assert not source.dirty and not source.new and not source.deleted
    readonly_engine.dispose()
    with tempfile.TemporaryDirectory(prefix="selection-model-validation-") as directory:
        engine = create_engine(f"sqlite:///{Path(directory).as_posix()}/validation.db", connect_args={"check_same_thread": False})
        Base.metadata.create_all(engine)
        factory = sessionmaker(bind=engine, expire_on_commit=False)
        original_factory = session_module.SessionLocal
        session_module.SessionLocal = factory
        try:
            with factory() as db:
                db.add_all([*providers, *models, *skills])
                db.commit()
                available = []
                for code in args.models:
                    start = time.monotonic()
                    print(f"正在实际测试 {code}（生产库只读）", flush=True)
                    try:
                        result = probe_selection_model(db, code)
                        available.append(code)
                        row = {"instance_code": code, "status": "SUCCESS", "call_log_id": result["call_log_id"]}
                    except Exception as exc:
                        detail = classify_model_error(exc)
                        row = {"instance_code": code, "status": "FAILED", "category": detail["category"],
                               "summary": detail["summary"], "suggestion": detail["suggestion"]}
                    row["elapsed_seconds"] = round(time.monotonic() - start, 2)
                    report["probes"].append(row)
                    print(json.dumps(row, ensure_ascii=True), flush=True)
                if args.analyze and available:
                    gate = _evidence_data_gate(item["evidence_json"])
                    assert gate["status"] == "READY", gate
                    run = SelectionRun(market=item["market"], status="RUNNING", data_mode="REAL",
                        model_instance_code=available[0], analysis_mode="MODEL_REQUESTED",
                        criteria_json={"analysis_contract": contract, "request": {"model_instance_codes": available}})
                    db.add(run)
                    db.commit()
                    print(f"正在实际分析 {args.symbol}，最多三次，日志仅写入隔离验证库", flush=True)
                    start = time.monotonic()
                    _apply_model_analysis(db, run, [(item, item["hard_score"])])
                    analysis = item["analysis_json"]
                    report["analysis"] = {"symbol": args.symbol, "data_gate_status": gate["status"],
                        "status": analysis.get("model_output_status"), "validation_status": analysis.get("analysis_validation_status"),
                        "actual_instance_code": analysis.get("actual_instance_code"), "attempts": analysis.get("model_attempts"),
                        "dimensions": [{key: value for key, value in dimension.items()
                                        if key in {"dimension", "status", "evidence_document_ids", "evidence_fact_ids"}}
                                       for dimension in (analysis.get("model") or {}).get("dimensions", [])],
                        "validation_warnings": analysis.get("evidence_validation_warnings"),
                        "failure_detail": {key: value for key, value in (analysis.get("model_failure_detail") or {}).items() if key != "raw_error"},
                        "elapsed_seconds": round(time.monotonic() - start, 2)}
                    print(json.dumps(report["analysis"], ensure_ascii=True), flush=True)
                report["calls"] = [{"id": log.id, "task_type": log.task_type, "instance_code": log.instance_code,
                    "status": log.status, "latency_ms": log.latency_ms,
                    "transport_attempts": len((log.request_json or {}).get("attempted_models") or [])}
                    for log in db.scalars(select(ModelCallLog).order_by(ModelCallLog.id))]
        finally:
            session_module.SessionLocal = original_factory
            engine.dispose()
    report["completed_at"] = datetime.now(timezone.utc).isoformat()
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"报告：{args.report}", flush=True)


if __name__ == "__main__":
    main()
