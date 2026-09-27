"""HTTP adapter for stock batch collection and knowledge governance jobs."""

from __future__ import annotations

from hashlib import sha256
from uuid import uuid4

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.core.markets import MASTER_MARKETS, digit_length_for_market
from app.db.session import get_db
from app.jobs.dispatcher import DatabaseJobDispatcher
from app.models.ai_hub import KnowledgeBase, KnowledgeGraph
from app.models.market_data import StockSymbol
from app.models.pipeline import PipelineRun, ScheduledJob
from app.schemas.stock_batch import StockBatchGovernanceRequest, StockBatchGovernanceResponse
from app.services.stock_batch import TASK_TYPE


router = APIRouter(prefix="/stocks/batch-governance/jobs", tags=["Stock batch governance"])


def _normalize_request(payload: StockBatchGovernanceRequest) -> StockBatchGovernanceRequest:
    normalized: list[dict[str, str]] = []
    for target in payload.stocks:
        market = target.market.strip().upper()
        if market not in MASTER_MARKETS:
            raise HTTPException(
                status_code=422,
                detail=f"market must be one of: {', '.join(MASTER_MARKETS)}",
            )
        symbol = target.symbol.strip().upper()
        if symbol.isdigit():
            symbol = symbol.zfill(digit_length_for_market(market))
        normalized.append({"market": market, "symbol": symbol})
    return StockBatchGovernanceRequest.model_validate({
        **payload.model_dump(),
        "stocks": normalized,
    })


def _validate_targets(db: Session, payload: StockBatchGovernanceRequest) -> None:
    missing: list[dict[str, str]] = []
    for target in payload.stocks:
        exists = db.scalar(select(StockSymbol.id).where(
            StockSymbol.market == target.market,
            StockSymbol.symbol == target.symbol,
        ))
        if exists is None:
            missing.append({"market": target.market, "symbol": target.symbol})
    if missing:
        raise HTTPException(status_code=422, detail={
            "message": "所选股票尚未进入股票主数据，请先同步主数据",
            "missing_stocks": missing,
        })

    knowledge_requested = any((
        payload.export_lakehouse,
        payload.archive_chunks,
        payload.run_graph,
        payload.run_agent_governance,
    ))
    if not knowledge_requested:
        return
    selected_kb: KnowledgeBase | None = None
    selected_graph: KnowledgeGraph | None = None
    if payload.graph_id is not None:
        graph = db.get(KnowledgeGraph, payload.graph_id)
        if graph is None or not graph.enabled:
            raise HTTPException(status_code=422, detail="知识图谱不存在或已停用")
        selected_graph = graph
        selected_kb = db.get(KnowledgeBase, graph.knowledge_base_id)
        if payload.knowledge_base_id is not None and graph.knowledge_base_id != payload.knowledge_base_id:
            raise HTTPException(status_code=422, detail="知识库与知识图谱不属于同一范围")
    elif payload.knowledge_base_id is not None:
        kb = db.get(KnowledgeBase, payload.knowledge_base_id)
        if kb is None or not kb.enabled:
            raise HTTPException(status_code=422, detail="知识库不存在或已停用")
        selected_kb = kb
    else:
        selected_kb = db.scalar(select(KnowledgeBase).where(
            KnowledgeBase.kb_code == "STOCK_FULL_KG",
            KnowledgeBase.enabled.is_(True),
        ))
        if selected_kb is None:
            raise HTTPException(status_code=422, detail="默认股票知识库不存在或已停用")
    if payload.run_graph and selected_graph is None and selected_kb is not None:
        selected_graph = db.scalar(select(KnowledgeGraph).where(
            KnowledgeGraph.knowledge_base_id == selected_kb.id,
            KnowledgeGraph.enabled.is_(True),
        ).order_by(KnowledgeGraph.id))
        if selected_graph is None:
            raise HTTPException(status_code=422, detail="所选知识库没有可用的基础知识图谱")


def _canonical_request(payload: StockBatchGovernanceRequest) -> dict:
    return payload.model_dump(exclude={"idempotency_key"})


def _job_key(payload: StockBatchGovernanceRequest) -> str:
    if not payload.idempotency_key:
        return f"{TASK_TYPE}:{uuid4()}"
    digest = sha256(payload.idempotency_key.encode("utf-8")).hexdigest()
    return f"{TASK_TYPE}:{digest}"


def _job_view(db: Session, job: ScheduledJob, *, details: bool = True) -> dict:
    run = db.get(PipelineRun, job.pipeline_run_id)
    output = dict((run.output_json if run else {}) or {})
    stock_count = len(((job.payload_json or {}).get("request") or {}).get("stocks") or [])
    result_status = output.get("result_status")
    display_status = result_status if job.status == "COMPLETED" and result_status else job.status
    status = {
        "job_id": job.id,
        "pipeline_run_id": job.pipeline_run_id,
        "status": display_status,
        "job_status": job.status,
        "result_status": result_status,
        "progress": int(output.get("progress") or (100 if job.status == "COMPLETED" else 0)),
        "current_stage": output.get("current_stage") or (run.current_stage if run else None) or "QUEUED",
        "stock_count": stock_count,
        "effective_options": output.get("effective_options") or (
            (job.payload_json or {}).get("request") or {}
        ),
        "attempts": job.attempts,
        "max_attempts": job.max_attempts,
        "error_message": job.error_message or (run.error_message if run else None),
        "created_at": job.created_at,
        "started_at": job.started_at,
        "completed_at": job.completed_at,
        "worker_required": job.status in {"PENDING", "RETRY"},
        "worker_task_type": TASK_TYPE,
        "worker_command": f"python -m app.jobs.worker --task-type {TASK_TYPE} --lease-seconds 7200",
    }
    if details:
        status.update({
            "stage_results": output.get("stage_results") or {},
            "stock_results": output.get("stock_results") or [],
            "errors": output.get("errors") or [],
            "pipeline_run_ids": output.get("pipeline_run_ids") or [],
            "graph_ids": output.get("graph_ids") or [],
        })
    return status


@router.post("", status_code=202, response_model=StockBatchGovernanceResponse)
def submit_stock_batch_governance(
    payload: StockBatchGovernanceRequest,
    db: Session = Depends(get_db),
) -> dict:
    payload = _normalize_request(payload)
    _validate_targets(db, payload)
    canonical = _canonical_request(payload)
    idempotency_key = _job_key(payload)
    existing = db.scalar(select(ScheduledJob).where(
        ScheduledJob.idempotency_key == idempotency_key,
    ))
    if existing is not None:
        existing_request = ((existing.payload_json or {}).get("request") or {})
        if existing_request != canonical:
            raise HTTPException(
                status_code=409,
                detail="相同幂等键已用于不同的批量请求，请更换幂等键",
            )
        return _job_view(db, existing, details=False)

    job = DatabaseJobDispatcher(db).enqueue(
        task_type=TASK_TYPE,
        pipeline_type="STOCK_BATCH_DATA_KNOWLEDGE_GOVERNANCE",
        trigger_type="API",
        idempotency_key=idempotency_key,
        payload={"request": canonical},
        max_attempts=3,
    )
    job.payload_json = {
        "request": canonical,
        "orchestration_job_id": job.id,
        "orchestration_pipeline_run_id": job.pipeline_run_id,
    }
    run = db.get(PipelineRun, job.pipeline_run_id)
    if run is not None:
        run.input_json = dict(job.payload_json)
        run.current_stage = "QUEUED"
    db.commit()
    db.refresh(job)
    return _job_view(db, job, details=False)


@router.get("")
def list_stock_batch_governance_jobs(
    limit: int = Query(default=20, ge=1, le=100),
    db: Session = Depends(get_db),
) -> dict:
    total = db.scalar(select(func.count()).select_from(ScheduledJob).where(
        ScheduledJob.task_type == TASK_TYPE,
    )) or 0
    jobs = list(db.scalars(
        select(ScheduledJob)
        .where(ScheduledJob.task_type == TASK_TYPE)
        .order_by(ScheduledJob.id.desc())
        .limit(limit)
    ).all())
    return {"items": [_job_view(db, job, details=False) for job in jobs], "total": total}


@router.get("/{job_id}")
def get_stock_batch_governance_job(job_id: int, db: Session = Depends(get_db)) -> dict:
    job = db.get(ScheduledJob, job_id)
    if job is None or job.task_type != TASK_TYPE:
        raise HTTPException(status_code=404, detail="批量采集治理任务不存在")
    return _job_view(db, job, details=True)
