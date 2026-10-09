"""Resumable production Agent/Skill governance for eight disjoint 20-stock boards."""
from __future__ import annotations

import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
OUT = ROOT / "validation" / "ai-boards-20261009"
BASELINE = ROOT / "validation" / "all-boards-20261008"
BATCH = "ai-boards-20261009"


def save(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps(data, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
    temporary.replace(path)


def prepare() -> list[dict]:
    from sqlalchemy import select
    from app.db.bootstrap import initialize_database
    from app.db.session import SessionLocal
    from app.models.governance import StockGovernanceDetail
    from app.models.market_data import StockSymbol
    from app.services.security_master import BOARD_DEFINITIONS, board_expression, type_expression
    from app.services.stock_governance_agent import ensure_stock_governance_details

    initialize_database()
    old_samples = json.loads((BASELINE / "samples.json").read_text(encoding="utf-8"))
    old_keys = {(s["market"], s["symbol"]) for s in old_samples}
    manifest = OUT / "samples.json"
    with SessionLocal() as db:
        if manifest.exists():
            samples = json.loads(manifest.read_text(encoding="utf-8"))
        else:
            samples = []
            old_names = {(s["market"], s["name"]) for s in old_samples}
            for board in BOARD_DEFINITIONS:
                stocks = list(db.scalars(select(StockSymbol).where(
                    board_expression() == board["code"],
                    type_expression() == "STOCK",
                    StockSymbol.status == "LISTED",
                )).all())
                stocks = [
                    s for s in stocks
                    if (s.market, s.symbol) not in old_keys
                    and (s.market, s.name) not in old_names
                ]
                if board["code"] == "BSE":
                    stocks = [
                        s for s in stocks
                        if s.symbol.startswith("920")
                        and (s.ext_json or {}).get("listing", {}).get("classification_method") == "SOURCE"
                    ]
                # Sampling is fixed before any source outcome, never replacing
                # a failed sample with one that is easier to collect.
                stocks.sort(key=lambda s: hashlib.sha256(
                    f"AI-20261009:{board['code']}:{s.market}:{s.symbol}".encode()
                ).hexdigest())
                if len(stocks) < 20:
                    raise RuntimeError(f"{board['code']} 非重复可用样本不足20只")
                for stock in stocks[:20]:
                    samples.append({
                        "board": board["code"], "board_name": board["name"],
                        "market": stock.market, "symbol": stock.symbol,
                        "name": stock.name, "master_id": stock.id,
                        "listing_evidence": (stock.ext_json or {}).get("listing", {}),
                        "sampling": "deterministic disjoint sample before ingestion",
                        "governance_mode": "AI_AGENT_SKILL_GOVERNANCE",
                        "governance_batch_id": BATCH,
                    })
            save(manifest, samples)
        if len(samples) != 160 or old_keys.intersection((s["market"], s["symbol"]) for s in samples):
            raise RuntimeError("样本数量或排除上一轮样本检查失败")
        report = json.loads((BASELINE / "acceptance-matrix.json").read_text(encoding="utf-8"))
        baseline_results = {(r["market"], r["symbol"]): r for r in report["stocks"]}
        for sample in old_samples:
            exists = db.scalar(select(StockGovernanceDetail).where(
                StockGovernanceDetail.governance_batch_id == BASELINE.name,
                StockGovernanceDetail.market == sample["market"],
                StockGovernanceDetail.symbol == sample["symbol"],
            ))
            if exists:
                continue
            result = baseline_results[(sample["market"], sample["symbol"])]
            db.add(StockGovernanceDetail(
                governance_batch_id=BASELINE.name, governance_mode="SYSTEM_GOVERNANCE",
                board_code=sample["board"], market=sample["market"],
                symbol=sample["symbol"], stock_name=sample["name"],
                status=result["verification_status"],
                stage_status_json={k: v["status"] for k, v in result["stages"].items()},
                evidence_ids_json=[f"validation:{BASELINE.name}/acceptance-matrix.json"],
                quality_summary_json={
                    "coverage_status": result["coverage_status"],
                    "quality": result["quality"],
                    "lineage": result["lineage"],
                    "historical_baseline": True,
                },
                completed_at=datetime.fromisoformat(report["generated_at"]),
            ))
        targets = [db.get(StockSymbol, s["master_id"]) for s in samples]
        ensure_stock_governance_details(
            db, governance_batch_id=BATCH,
            governance_mode="AI_AGENT_SKILL_GOVERNANCE", stocks=targets,
            pipeline_run_id=None, agent_execution_run_id=None,
        )
        for row in db.scalars(select(StockGovernanceDetail).where(
            StockGovernanceDetail.governance_batch_id == BATCH,
            StockGovernanceDetail.agent_execution_run_id.is_(None),
        )).all():
            row.status = "PENDING"
        db.commit()
    save(OUT / "sampling-check.json", {
        "total": len(samples), "per_board": {
            b: sum(s["board"] == b for s in samples) for b in dict.fromkeys(s["board"] for s in samples)
        }, "overlap": 0, "baseline": BASELINE.name,
        "generated_at": datetime.now(timezone.utc).isoformat(),
    })
    return samples


def worker(
    sample: dict | list[dict],
    types: list[str],
    *,
    knowledge_only: bool = False,
) -> dict:
    from scripts.audit_all_boards import install_bounded_http
    from app.api.stock_batch import submit_stock_batch_governance
    from app.db.session import SessionLocal
    from app.models.pipeline import PipelineRun, ScheduledJob
    from app.schemas.stock_batch import StockBatchGovernanceRequest
    from app.services import stock_batch

    targets = sample if isinstance(sample, list) else [sample]
    first = targets[0]
    trace, clock = [], [time.monotonic() + 60]
    install_bounded_http(trace, clock)
    original_fetch = stock_batch._fetch_one
    original_f10 = stock_batch._run_f10_refresh

    def fetch(*args, **kwargs):
        clock[0] = time.monotonic() + (100 if kwargs["data_type"] == "NEWS" else 60)
        return original_fetch(*args, **kwargs)

    def f10(*args, **kwargs):
        clock[0] = time.monotonic() + 220
        return original_f10(*args, **kwargs)

    stock_batch._fetch_one, stock_batch._run_f10_refresh = fetch, f10
    # Model review is separate from the data-source budget.
    from app.services.stock_governance_agent import StockGovernanceAgentSession
    original_review = StockGovernanceAgentSession.review_quality_with_model

    def review(self, summary):
        clock[0] = time.monotonic() + 130
        return original_review(self, summary)

    StockGovernanceAgentSession.review_quality_with_model = review
    request = StockBatchGovernanceRequest(
        stocks=[{"market": selected["market"], "symbol": selected["symbol"]} for selected in targets],
        governance_mode="AI_AGENT_SKILL_GOVERNANCE",
        governance_batch_id=BATCH,
        collect_business_data=not knowledge_only,
        business_types=[] if knowledge_only else types,
        kline_days=3650 if first["market"].startswith("NEEQ") else 400,
        disclosure_days=730,
        export_lakehouse=knowledge_only,
        archive_chunks=knowledge_only,
        run_graph=knowledge_only,
    )
    with SessionLocal() as db:
        job_view = submit_stock_batch_governance(request, db)
        job = db.get(ScheduledJob, job_view["job_id"])
        payload = dict(job.payload_json)
        job.status = "RUNNING"
        job.attempts += 1
        job.started_at = datetime.now(timezone.utc)
        run = db.get(PipelineRun, job.pipeline_run_id)
        run.status = "RUNNING"
        db.commit()
    original_progress = stock_batch._save_progress

    def progress(db, run_id, current, **kwargs):
        original_progress(db, run_id, current, **kwargs)
        save(OUT / "progress" / f"{first['board']}-{job_view['job_id']}.json", {
            "job_id": job_view["job_id"], "board": first["board"],
            "stock_count": len(targets),
            "collected": len(current.get("stock_results") or []),
            "stage": current.get("current_stage"),
            "progress": current.get("progress"),
            "at": datetime.now(timezone.utc).isoformat(),
        })

    stock_batch._save_progress = progress
    output = stock_batch.execute_stock_batch_governance(payload)
    with SessionLocal() as db:
        job = db.get(ScheduledJob, job_view["job_id"])
        run = db.get(PipelineRun, job.pipeline_run_id)
        job.status = "COMPLETED"
        job.completed_at = datetime.now(timezone.utc)
        run.status = "COMPLETED"
        run.output_json = output
        run.completed_at = job.completed_at
        db.commit()
    results = {(row["market"], row["symbol"]): row for row in output.get("stock_results") or []}
    mapping = {"QUOTE": "quote", "KLINE": "kline", "FINANCIAL": "financials", "NOTICE": "notices", "NEWS": "news", "F10": "extended"}
    trace_file = f"traces/{first['board']}-{job_view['job_id']}.json"
    save(OUT / trace_file, trace)
    for selected in targets:
        path = OUT / "stocks" / f"{selected['board']}-{selected['symbol']}.json"
        result = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {**selected, "attempts": {}}
        operations = results.get((selected["market"], selected["symbol"]), {}).get("operations") or {}
        for code, operation in operations.items():
            attempt = {
                **operation, "at": datetime.now(timezone.utc).isoformat(),
                "status": "FETCHED" if operation.get("status") == "SUCCESS" or (
                    code == "F10" and int(operation.get("section_count") or 0) > 0
                ) else (
                    "EMPTY_UNVERIFIED" if operation.get("status") == "MISSING" else "FAILED"
                ),
                "log_id": operation.get("fetch_log_id"),
                "job_id": job_view["job_id"],
                "agent_execution_run_id": output.get("agent_execution_run_id"),
                "trace_file": trace_file,
            }
            result["attempts"].setdefault(mapping[code], []).append(attempt)
            if code == "F10":
                result["attempts"].setdefault("research", []).append(dict(attempt))
        result["agent_output"] = {
            key: output.get(key) for key in (
                "result_status", "governance_mode", "governance_batch_id",
                "agent_execution_run_id", "agent_skill_governance", "errors",
            )
        }
        save(path, result)
    return {"agent_output": result["agent_output"]}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--prepare", action="store_true")
    parser.add_argument("--workers", type=int, default=4)
    parser.add_argument("--boards", default="")
    parser.add_argument("--limit", type=int, default=160)
    parser.add_argument("--types", default="QUOTE,KLINE,FINANCIAL,NOTICE,NEWS,F10")
    parser.add_argument("--retry", action="store_true")
    parser.add_argument("--child", default="")
    parser.add_argument("--board-batches", action="store_true")
    parser.add_argument(
        "--knowledge-only",
        action="store_true",
        help="仅按固定板块运行湖仓、文档切片和知识图谱发布，不重复采集业务数据",
    )
    args = parser.parse_args()
    if args.prepare:
        samples = prepare()
        print(json.dumps({"prepared": len(samples), "manifest": str(OUT / "samples.json")}, ensure_ascii=False))
        return
    samples = json.loads((OUT / "samples.json").read_text(encoding="utf-8"))
    if args.child:
        sample = (
            [s for s in samples if s["board"] == args.child.split(":")[0]]
            if args.child.endswith(":ALL")
            else next(s for s in samples if f"{s['board']}:{s['symbol']}" == args.child)
        )
        result = worker(
            sample,
            args.types.split(","),
            knowledge_only=args.knowledge_only,
        )
        print(json.dumps(result["agent_output"], ensure_ascii=False))
        return
    selected = [s for s in samples if not args.boards or s["board"] in args.boards.split(",")][:args.limit]
    if args.board_batches:
        selected = [
            {"board": board, "symbol": "ALL", "targets": [s for s in selected if s["board"] == board]}
            for board in dict.fromkeys(s["board"] for s in selected)
        ]

    def execute(sample):
        path = OUT / "stocks" / f"{sample['board']}-{sample['symbol']}.json"
        if path.exists() and not args.retry:
            try:
                previous = json.loads(path.read_text(encoding="utf-8"))
                review = ((previous.get("agent_output") or {}).get("agent_skill_governance") or {}).get("model_quality_review") or {}
                if review.get("status") == "SUCCESS" and review.get("real_model") is True:
                    return {"board": sample["board"], "symbol": sample["symbol"], "status": "RESUMED"}
            except (OSError, ValueError, TypeError):
                pass
        command = [
            sys.executable, "-u", str(Path(__file__).resolve()),
            "--child", f"{sample['board']}:{sample['symbol']}", "--types", args.types,
        ]
        if args.knowledge_only:
            command.append("--knowledge-only")
        try:
            process = subprocess.run(
                command, cwd=ROOT, capture_output=True, text=True,
                encoding="utf-8", errors="replace", timeout=7200 if args.board_batches else 720,
            )
            return {
                "board": sample["board"], "symbol": sample["symbol"],
                "status": "FINISHED" if process.returncode == 0 else "FAILED",
                "error": process.stderr[-1800:] if process.returncode else None,
            }
        except subprocess.TimeoutExpired:
            return {"board": sample["board"], "symbol": sample["symbol"], "status": "TIMEOUT", "error": "采集进程超时，已提交的数据保留"}

    results = []
    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        futures = [pool.submit(execute, s) for s in selected]
        for future in as_completed(futures):
            result = future.result()
            results.append(result)
            save(OUT / "worker-results.json", results)
            print(json.dumps({"done": len(results), "total": len(selected), **result}, ensure_ascii=False), flush=True)


if __name__ == "__main__":
    main()
