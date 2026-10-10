"""HTTP adapter for stock batch collection and knowledge governance jobs."""

from __future__ import annotations

from hashlib import sha256
import json
from uuid import uuid4

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import String, func, select
from sqlalchemy.orm import Session

from app.core.markets import MASTER_MARKETS, digit_length_for_market
from app.db.session import get_db
from app.jobs.dispatcher import DatabaseJobDispatcher
from app.models.ai_hub import KnowledgeBase, KnowledgeGraph
from app.models.market_data import StockSymbol
from app.models.governance import AgentExecutionRun, SkillExecutionRun, StockGovernanceDetail
from app.models.pipeline import PipelineRun, ScheduledJob
from app.schemas.stock_batch import (
    DOCUMENT_CHUNKS_CONTINUATION,
    SCOPED_GRAPH_CONTINUATION,
    StockBatchGovernanceRequest,
    StockBatchGovernanceResponse,
)
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
    if payload.run_graph and selected_graph is not None:
        payload.graph_id = selected_graph.id
        payload.knowledge_base_id = selected_graph.knowledge_base_id


def _canonical_request(payload: StockBatchGovernanceRequest) -> dict:
    return payload.model_dump(exclude={"idempotency_key"})


def _job_key(payload: StockBatchGovernanceRequest) -> str:
    if not payload.idempotency_key:
        return f"{TASK_TYPE}:{uuid4()}"
    digest = sha256(payload.idempotency_key.encode("utf-8")).hexdigest()
    return f"{TASK_TYPE}:{digest}"


def find_stock_batch_job_by_client_key(
    db: Session,
    client_key: str,
) -> ScheduledJob | None:
    """Resolve the durable job behind a client idempotency key."""
    digest = sha256(client_key.encode("utf-8")).hexdigest()
    return db.scalar(select(ScheduledJob).where(
        ScheduledJob.idempotency_key == f"{TASK_TYPE}:{digest}",
    ))


def stock_batch_job_view(db: Session, job: ScheduledJob) -> dict:
    """Expose the stable response contract to continuation endpoints."""
    return _job_view(db, job, details=False)


_BUSINESS_LABELS = {
    "QUOTE": "实时行情",
    "KLINE": "历史量价",
    "FINANCIAL": "财务数据",
    "NEWS": "新闻资讯",
    "NOTICE": "公司公告",
    "F10": "F10资料",
}
_BUSINESS_VERIFY_SUFFIX = {
    "QUOTE": "pipeline-status-detail",
    "KLINE": "kline",
    "FINANCIAL": "financials",
    "NEWS": "news/page",
    "NOTICE": "notices/page",
    "F10": "f10?local_only=true",
}


def _stage_item(
    code: str,
    label: str,
    status: str,
    *,
    reason: str,
    record_count: int | None = None,
    details: dict | None = None,
    verify_target: str | None = None,
) -> dict:
    return {
        "code": code,
        "label": label,
        "status": status,
        "record_count": record_count,
        "reason": reason,
        "details": details or {},
        "verification_path": verify_target,
    }


def _enrich_stage_results(output: dict) -> dict:
    """Add explainable completion/verification sections to old and new jobs.

    Batch output has intentionally stayed backward compatible and historically
    stored only status words and per-market payloads.  This adapter derives the
    richer UI contract at read time, so opening an old task never requires a
    destructive migration or a rerun.
    """
    stage_results = output.get("stage_results") or {}
    enriched = dict(stage_results)
    completion = (output.get("completion") or {}) if isinstance(output.get("completion"), dict) else {}
    completion_issues = list(completion.get("issues") or [])
    scoped_graph_continuation = (
        str((output.get("effective_options") or {}).get("continuation_action") or "").upper()
        == SCOPED_GRAPH_CONTINUATION
    )
    completion_by_market: dict[str, dict] = {}
    knowledge_market_runs = (
        (stage_results.get("KNOWLEDGE_PIPELINE") or {}).get("market_runs") or []
    )
    for market_run in knowledge_market_runs:
        market_completion = market_run.get("completion") or {}
        market_key = str(market_run.get("market") or "ALL")
        completion_by_market[market_key] = market_completion
        completion_issues.extend(market_completion.get("issues") or [])

    def issue_for(stage_code: str) -> list[dict]:
        return [item for item in completion_issues if isinstance(item, dict) and item.get("stage") == stage_code]

    # The business stage is represented by one stock result per selected stock.
    business = dict(enriched.get("BUSINESS_DATA") or {})
    completed: list[dict] = []
    incomplete: list[dict] = []
    business_records = output.get("stock_results") or []
    requested_business_types = [
        str(code).upper()
        for code in ((output.get("effective_options") or {}).get("business_types") or [])
        if str(code).upper() in _BUSINESS_LABELS
    ]
    for stock_result in business_records:
        market = stock_result.get("market")
        symbol = stock_result.get("symbol")
        operations = stock_result.get("operations") or {}
        operation_codes = requested_business_types or [
            str(code).upper() for code in operations if str(code).upper() in _BUSINESS_LABELS
        ]
        for code in operation_codes:
            operation = operations.get(code) or {}
            status = str(operation.get("status") or "UNKNOWN").upper()
            count = operation.get("persisted_count")
            if count is None and code == "F10":
                count = operation.get("section_count")
            total_count = operation.get("total_count")
            effective_count = (
                operation.get("section_count") if code == "F10"
                else total_count if total_count is not None else count
            )
            has_persisted_result = status == "SUCCESS" and (effective_count or 0) > 0
            detail = {
                "market": market, "symbol": symbol,
                "source_code": operation.get("source_code"),
                "fetch_log_id": operation.get("fetch_log_id"),
                "total_count": operation.get("total_count"),
                "persisted_count": operation.get("persisted_count"),
                "sections": operation.get("sections"),
                "error": operation.get("error"),
            }
            reason = (
                f"{market}:{symbol} 已返回 {effective_count or 0} 条{_BUSINESS_LABELS[code]}记录，"
                f"持久化 {count or 0} 条（幂等更新也视为已完成）。"
                if has_persisted_result
                else operation.get("error") or operation.get("message") or (
                    f"{market}:{symbol} 的{_BUSINESS_LABELS[code]}接口调用成功，"
                    "但未返回或未持久化有效记录。"
                    if status == "SUCCESS"
                    else
                    f"{market}:{symbol} 的{_BUSINESS_LABELS[code]}状态为 {status}，未形成完整可核验记录。"
                )
            )
            item = _stage_item(
                f"{market}:{symbol}:{code}", _BUSINESS_LABELS[code],
                "COMPLETED" if has_persisted_result else ("EMPTY" if status == "SUCCESS" else status),
                reason=reason, record_count=effective_count, details=detail,
                verify_target=f"/api/v1/stocks/{market}/{symbol}/{_BUSINESS_VERIFY_SUFFIX[code]}",
            )
            (completed if item["status"] == "COMPLETED" else incomplete).append(item)
    business["completed_items"] = completed
    business["incomplete_items"] = incomplete
    business["pending_items"] = incomplete
    business["reasons"] = [
        {"code": item["code"], "reason": item["reason"]}
        for item in incomplete
    ]
    business["verification"] = {
        "stock_count": len(business_records),
        "completed_count": len(completed),
        "incomplete_count": len(incomplete),
        "records": business_records,
    }
    business["verify_target"] = "/api/v1/stocks/{market}/{symbol}/pipeline-status-detail"
    enriched["BUSINESS_DATA"] = business

    # Knowledge stages are emitted per market.  Retain the raw market payloads
    # and expose concise completion items plus the exact child evidence.
    stage_specs = (
        ("LAKEHOUSE_EXPORT", "湖仓数据发布", "/api/v1/lakehouse/datasets"),
        ("DOCUMENT_CHUNKS", "知识文档切片", "/api/v1/lakehouse/chunks"),
        ("KNOWLEDGE_GRAPH", "知识图谱投影", "/api/v1/knowledge-network/explore"),
    )
    for stage_code, stage_label, verify_target in stage_specs:
        stage = dict(enriched.get(stage_code) or {})
        market_runs = stage.get("market_runs") or []
        stage_completed: list[dict] = []
        stage_incomplete: list[dict] = []
        reasons: list[dict] = []
        for run_index, raw_run in enumerate(list(market_runs)):
            run = dict(raw_run)
            market_runs[run_index] = run
            market = run.get("market") or run.get("market_trigger") or "ALL"
            status = str(run.get("status") or "UNKNOWN").upper()
            details = dict(run)
            if stage_code == "LAKEHOUSE_EXPORT":
                datasets = run.get("datasets") or {}
                if not datasets and run.get("dataset_statuses"):
                    datasets = {
                        name: {"status": value}
                        for name, value in (run.get("dataset_statuses") or {}).items()
                    }
                run["datasets"] = datasets
                for dataset_code, dataset in datasets.items():
                    dataset_status = str((dataset or {}).get("status") or "UNKNOWN").upper()
                    item = _stage_item(
                        f"{market}:{dataset_code}", dataset_code,
                        "COMPLETED" if dataset_status == "PUBLISHED" else dataset_status,
                        reason=(
                            f"{market} 数据集 {dataset_code} 已发布，可查看版本、质量和行数。"
                            if dataset_status == "PUBLISHED"
                            else (dataset or {}).get("error") or f"数据集 {dataset_code} 发布状态为 {dataset_status}。"
                        ),
                        record_count=(dataset or {}).get("row_count"), details=dataset,
                        verify_target=(
                            f"/api/v1/lakehouse/datasets/{(dataset or {}).get('dataset_id')}/preview"
                            if (dataset or {}).get("dataset_id") else "/api/v1/lakehouse/datasets"
                        ),
                    )
                    (stage_completed if item["status"] == "COMPLETED" else stage_incomplete).append(item)
            elif stage_code == "DOCUMENT_CHUNKS":
                market_completion = completion_by_market.get(str(market), {})
                chunk_issues = [
                    issue for issue in (market_completion.get("issues") or [])
                    if issue.get("stage") == "DOCUMENT_CHUNKS"
                ]
                for issue in chunk_issues:
                    if issue.get("code") == "CHUNK_SELECTION_TRUNCATED" and isinstance(issue.get("detail"), dict):
                        run.setdefault("documents_selected", issue["detail"].get("selected"))
                        run.setdefault("documents_total", issue["detail"].get("total"))
                        run.setdefault("truncated", True)
                    elif issue.get("code") == "CHUNK_FAILURES":
                        run.setdefault("failed_documents", issue.get("detail"))
                run["completion_issues"] = chunk_issues
                run["next_action"] = run.get("next_action") or market_completion.get("next_action")
                total = int(run.get("documents_total") or 0)
                with_chunks = int(run.get("documents_with_chunks") or 0)
                failed = int(run.get("failed_documents") or 0)
                selected_value = run.get("documents_selected")
                # Historical runs predate the selection metric. Infer it from
                # actual chunk coverage so a successful old task is not shown
                # as "0 documents selected" when reopened for verification.
                selected = (
                    int(selected_value or 0)
                    if selected_value is not None
                    else min(total, with_chunks)
                )
                continuation_mode = (
                    str(run.get("continuation_action") or "").upper()
                    == DOCUMENT_CHUNKS_CONTINUATION
                )
                selection_item = _stage_item(
                    f"{market}:DOCUMENT_SELECTION",
                    f"{market} · 实际缺口识别" if continuation_mode else f"{market} · 知识文档选择",
                    "COMPLETED" if (selected > 0 or continuation_mode) else status,
                    reason=(
                        f"跨 {run.get('graph_count', 0)} 个图谱版本识别到 {selected} 份未切片文档。"
                        if continuation_mode else
                        f"已从 {total} 份知识文档中选择 {selected} 份进入切片。"
                        if selected > 0 else "该范围没有选出可供切片的知识文档。"
                    ), record_count=selected, details=run, verify_target=verify_target,
                )
                (stage_completed if selection_item["status"] == "COMPLETED" else stage_incomplete).append(selection_item)
                coverage_complete = bool(total and with_chunks == total and not failed)
                coverage_item = _stage_item(
                    f"{market}:CHUNK_COVERAGE", f"{market} · 切片覆盖",
                    "COMPLETED" if coverage_complete else ("PARTIAL" if with_chunks else status),
                    reason=(
                        f"{market} 已完成 {with_chunks}/{total} 份文档切片。"
                        if coverage_complete else (
                            f"{market} 已切片 {with_chunks}/{total} 份，仍有 "
                            f"{run.get('missing_documents_after', max(total - with_chunks, 0))} 份未覆盖。"
                            if continuation_mode and total else
                            f"{market} 已切片 {with_chunks}/{total} 份；选择 {selected} 份，"
                            f"仍有 {max(total - with_chunks, 0)} 份未覆盖，失败 {failed} 份。"
                            if total else "该范围没有可切片文档。"
                        )
                    ), record_count=with_chunks, details=run, verify_target=verify_target,
                )
                (stage_completed if coverage_item["status"] == "COMPLETED" else stage_incomplete).append(coverage_item)
                knowledge_mode = str(
                    run.get("knowledge_base_mode") or stage.get("knowledge_base_mode") or ""
                ).upper()
                if knowledge_mode == "EXISTING_DOCUMENTS_ONLY":
                    stage_incomplete.append(_stage_item(
                        f"{market}:NEW_DATA_MATERIALIZATION",
                        f"{market} · 新业务数据知识物化",
                        "PARTIAL",
                        reason="本次只加工既有知识文档；新采集业务记录尚未全部物化为新知识文档。",
                        details={"knowledge_base_mode": knowledge_mode},
                        verify_target=verify_target,
                    ))
            else:
                build_status = str(run.get("build_status") or status).upper()
                governance = str(run.get("governance_status") or "").upper()
                market_completion = completion_by_market.get(str(market), {})
                graph_issues = run.get("completion_issues") or [
                    issue for issue in (market_completion.get("issues") or [])
                    if issue.get("stage") == "KNOWLEDGE_GRAPH"
                ]
                next_action = run.get("next_action") or market_completion.get("next_action")
                run["completion_issues"] = graph_issues
                run["next_action"] = next_action
                build_complete = build_status == "BUILT"
                build_item = _stage_item(
                    f"{market}:GRAPH_BUILD", f"{market} · 图谱投影构建",
                    "COMPLETED" if build_complete else build_status,
                    reason=(
                        f"{market} 图谱投影已构建，包含 {(run.get('counts') or {}).get('entities', 0)} 个实体、"
                        f"{(run.get('counts') or {}).get('relations', 0)} 条关系。"
                        if build_complete else run.get("error") or f"图谱构建状态为 {build_status}。"
                    ), record_count=(run.get("counts") or {}).get("relations"),
                    details={**run, "completion_issues": graph_issues, "next_action": next_action},
                    verify_target=(
                        f"/api/v1/resources/knowledge-graphs/{run.get('graph_id')}/explore"
                        if run.get("graph_id") else verify_target
                    ),
                )
                (stage_completed if build_item["status"] == "COMPLETED" else stage_incomplete).append(build_item)
                if not scoped_graph_continuation:
                    governance_complete = governance in {"GOVERNED", "LOCKED"}
                    governance_item = _stage_item(
                        f"{market}:GRAPH_GOVERNANCE", f"{market} · 图谱治理发布",
                        "COMPLETED" if governance_complete else (governance or "PENDING"),
                        reason=(
                            f"图谱已完成治理，当前状态为 {governance}。"
                            if governance_complete else next_action or f"图谱治理状态为 {governance or 'PENDING'}，尚未发布为已治理事实。"
                        ),
                        details={**run, "completion_issues": graph_issues, "next_action": next_action},
                        verify_target=(
                            f"/api/v1/resources/knowledge-graphs/{run.get('graph_id')}/explore"
                            if run.get("graph_id") else verify_target
                        ),
                    )
                    (stage_completed if governance_item["status"] == "COMPLETED" else stage_incomplete).append(governance_item)
        reasons.extend({"code": item["code"], "reason": item["reason"]} for item in stage_incomplete)
        # Older output may have no market_runs but does have completion issues.
        for issue in issue_for(stage_code):
            if scoped_graph_continuation and issue.get("code") == "GRAPH_PENDING_GOVERNANCE":
                continue
            if not any(reason.get("code") == issue.get("code") for reason in reasons):
                reasons.append({"code": issue.get("code"), "reason": issue.get("detail")})
        stage["completed_items"] = stage_completed
        stage["incomplete_items"] = stage_incomplete
        stage["pending_items"] = stage_incomplete
        stage["reasons"] = reasons
        stage["verification"] = {
            "market_runs": market_runs,
            "completed_count": len(stage_completed),
            "incomplete_count": len(stage_incomplete),
            "governance_follow_up": scoped_graph_continuation and stage_code == "KNOWLEDGE_GRAPH",
        }
        stage["market_runs"] = market_runs
        stage["verify_target"] = verify_target
        enriched[stage_code] = stage
    return enriched


def _job_view(db: Session, job: ScheduledJob, *, details: bool = True) -> dict:
    run = db.get(PipelineRun, job.pipeline_run_id)
    output = dict((run.output_json if run else {}) or {})
    stock_count = len(((job.payload_json or {}).get("request") or {}).get("stocks") or [])
    # PipelineRun keeps the last progress snapshot on worker retry/failure. Its
    # RUNNING result is not the job's current lifecycle status.
    result_status = output.get("result_status") if job.status == "COMPLETED" else None
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
        "governance_mode": output.get("governance_mode") or (
            getattr(run, "governance_mode", None) if run else None
        ) or ((job.payload_json or {}).get("request") or {}).get("governance_mode") or "SYSTEM_GOVERNANCE",
        "governance_batch_id": output.get("governance_batch_id") or (
            getattr(run, "governance_batch_id", None) if run else None
        ) or ((job.payload_json or {}).get("request") or {}).get("governance_batch_id"),
        "agent_execution_run_id": output.get("agent_execution_run_id"),
        "effective_options": output.get("effective_options") or (
            (job.payload_json or {}).get("request") or {}
        ),
        "attempts": job.attempts,
        "max_attempts": job.max_attempts,
        "run_after": job.run_after if job.status in {"PENDING", "RETRY"} else None,
        "error_message": job.error_message or (run.error_message if run else None),
        "created_at": job.created_at,
        "started_at": job.started_at,
        "completed_at": job.completed_at,
        "worker_required": job.status in {"PENDING", "RETRY"},
        "worker_task_type": TASK_TYPE,
        "worker_command": (
            f"python -m app.jobs.worker --task-type {TASK_TYPE} "
            "--lease-seconds 7200 --reload"
        ),
    }
    if details:
        batch_id = status.get("governance_batch_id")
        stock_statement = select(StockGovernanceDetail).where(
            StockGovernanceDetail.pipeline_run_id == job.pipeline_run_id
        )
        if batch_id:
            stock_statement = stock_statement.where(
                StockGovernanceDetail.governance_batch_id == batch_id
            )
        stock_details = list(db.scalars(
            stock_statement.order_by(StockGovernanceDetail.created_at, StockGovernanceDetail.symbol)
        ).all())
        agent_run_id = status.get("agent_execution_run_id") or next(
            (row.agent_execution_run_id for row in stock_details if row.agent_execution_run_id),
            None,
        )
        agent_run = db.get(AgentExecutionRun, agent_run_id) if agent_run_id else None
        skill_predicates = []
        if agent_run_id:
            skill_predicates.append(SkillExecutionRun.agent_execution_run_id == agent_run_id)
        if batch_id:
            skill_predicates.append(SkillExecutionRun.governance_batch_id == batch_id)
        skill_query = select(SkillExecutionRun).order_by(SkillExecutionRun.started_at)
        if skill_predicates:
            from sqlalchemy import or_
            skill_query = skill_query.where(or_(*skill_predicates))
        skill_runs = list(db.scalars(skill_query).all()) if skill_predicates else []
        stock_report = [{
            "id": row.id,
            "market": row.market,
            "symbol": row.symbol,
            "stock_name": row.stock_name,
            "board_code": row.board_code,
            "status": row.status,
            "stage_status": row.stage_status_json or {},
            "source_ids": row.source_ids_json or [],
            "evidence_ids": row.evidence_ids_json or [],
            "quality_summary": row.quality_summary_json or {},
            "started_at": row.started_at,
            "completed_at": row.completed_at,
        } for row in stock_details]
        if not stock_report:
            # Older jobs may predate StockGovernanceDetail materialization. Do
            # not show a false empty report when their durable pipeline output
            # already contains the per-stock result and evidence information.
            stock_report = [
                item for item in (output.get("stock_results") or [])
                if isinstance(item, dict)
            ]
        report = {
            "job": {
                "job_id": job.id,
                "pipeline_run_id": job.pipeline_run_id,
                "governance_mode": status.get("governance_mode"),
                "governance_batch_id": batch_id,
                "status": status.get("status"),
                "job_status": status.get("job_status"),
                "result_status": status.get("result_status"),
                "current_stage": status.get("current_stage"),
                "progress": status.get("progress"),
                "created_at": status.get("created_at"),
                "started_at": status.get("started_at"),
                "completed_at": status.get("completed_at"),
            },
            "stock_results": stock_report,
            "stage_results": _enrich_stage_results(output),
            "errors": output.get("errors") or [],
            "agent": ({
                "id": agent_run.id,
                "agent_code": agent_run.agent_code,
                "agent_version": agent_run.agent_version,
                "model_instance_code": agent_run.model_instance_code,
                "model_version": agent_run.model_version,
                "status": agent_run.status,
                "input_hash": agent_run.input_hash,
                "output_hash": agent_run.output_hash,
                "evidence_ids": agent_run.evidence_ids_json or [],
                "audit": agent_run.audit_json or {},
                "error_message": agent_run.error_message,
                "started_at": agent_run.started_at,
                "completed_at": agent_run.completed_at,
            } if agent_run else None),
            "skills": [{
                "id": item.id,
                "skill_code": item.skill_code,
                "skill_version": item.skill_version,
                "status": item.status,
                "attempt": item.attempt,
                "max_attempts": item.max_attempts,
                "side_effect_level": item.side_effect_level,
                "write_scope": item.write_scope_json or {},
                "input_hash": item.input_hash,
                "output_hash": item.output_hash,
                "evidence_ids": item.evidence_ids_json or [],
                "error_message": item.error_message,
                "started_at": item.started_at,
                "completed_at": item.completed_at,
            } for item in skill_runs],
        }
        status.update({
            "stage_results": _enrich_stage_results(output),
            "stock_results": output.get("stock_results") or [],
            "errors": output.get("errors") or [],
            "pipeline_run_ids": output.get("pipeline_run_ids") or [],
            "graph_ids": output.get("graph_ids") or [],
            "governance_report": report,
        })
    return status


@router.post("", status_code=202, response_model=StockBatchGovernanceResponse)
def submit_stock_batch_governance(
    payload: StockBatchGovernanceRequest,
    db: Session = Depends(get_db),
) -> dict:
    payload = _normalize_request(payload)
    if not payload.governance_batch_id:
        seed_payload = payload.model_dump(
            mode="json",
            exclude={"idempotency_key", "governance_batch_id"},
        )
        seed = json.dumps(seed_payload, ensure_ascii=False, sort_keys=True)
        payload.governance_batch_id = (
            f"{payload.governance_mode.lower()}:{sha256(seed.encode('utf-8')).hexdigest()[:20]}"
        )
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

    active_jobs = db.scalars(select(ScheduledJob).where(
        ScheduledJob.task_type == TASK_TYPE,
        ScheduledJob.status.in_(("PENDING", "RUNNING", "RETRY")),
    ).order_by(ScheduledJob.id.desc())).all()
    for active_job in active_jobs:
        if ((active_job.payload_json or {}).get("request") or {}) == canonical:
            return _job_view(db, active_job, details=False)

    job = DatabaseJobDispatcher(db).enqueue(
        task_type=TASK_TYPE,
        pipeline_type="STOCK_BATCH_DATA_KNOWLEDGE_GOVERNANCE",
        trigger_type="API",
        idempotency_key=idempotency_key,
        payload={"request": canonical},
        max_attempts=3,
    )
    enqueued_request = ((job.payload_json or {}).get("request") or {})
    if enqueued_request != canonical:
        raise HTTPException(
            status_code=409,
            detail="相同幂等键已用于不同的批量请求，请更换幂等键",
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
        run.governance_mode = payload.governance_mode
        run.governance_batch_id = payload.governance_batch_id
    db.commit()
    db.refresh(job)
    return _job_view(db, job, details=False)


@router.get("")
def list_stock_batch_governance_jobs(
    limit: int = Query(default=20, ge=1, le=100),
    offset: int = Query(default=0, ge=0),
    status_filter: str | None = Query(default=None),
    governance_mode: str | None = Query(default=None, pattern="^(SYSTEM_GOVERNANCE|AI_AGENT_SKILL_GOVERNANCE)$"),
    keyword: str | None = Query(default=None),
    db: Session = Depends(get_db),
) -> dict:
    # The function is also called directly by the service tests. FastAPI's
    # Query objects are only resolved by dependency injection, so normalize
    # those defaults when the adapter is invoked as a plain Python function.
    if not isinstance(limit, int):
        limit = 20
    if not isinstance(offset, int):
        offset = 0
    if not isinstance(status_filter, str):
        status_filter = None
    if not isinstance(governance_mode, str):
        governance_mode = None
    if not isinstance(keyword, str):
        keyword = None
    predicates = [ScheduledJob.task_type == TASK_TYPE]
    if status_filter:
        requested_status = status_filter.strip().upper()
        # Job status is the durable lifecycle status. Result status is kept in
        # PipelineRun.output_json, so filter both without changing old records.
        if requested_status in {"PENDING", "RUNNING", "RETRY", "FAILED"}:
            predicates.append(ScheduledJob.status == requested_status)
        else:
            predicates.append(ScheduledJob.status == "COMPLETED")
    if keyword:
        needle = f"%{keyword.strip()}%"
        matching_batch_ids = select(StockGovernanceDetail.governance_batch_id).where(
            StockGovernanceDetail.stock_name.ilike(needle)
            | StockGovernanceDetail.symbol.ilike(needle)
            | StockGovernanceDetail.market.ilike(needle)
        )
        predicates.append(
            (ScheduledJob.payload_json.cast(String).ilike(needle))
            | (PipelineRun.governance_batch_id.in_(matching_batch_ids))
        )
    statement = select(ScheduledJob).join(
        PipelineRun, PipelineRun.id == ScheduledJob.pipeline_run_id
    ).where(*predicates)
    if governance_mode:
        statement = statement.where(PipelineRun.governance_mode == governance_mode)
    result_status_filter = status_filter and status_filter.strip().upper() not in {
        "PENDING", "RUNNING", "RETRY", "FAILED"
    }
    if result_status_filter:
        # SQLite JSON operators differ from PostgreSQL and historical rows may
        # have no result_status. Filter the small, bounded history in Python so
        # pagination and the total count remain correct on both databases.
        all_jobs = list(db.scalars(statement.order_by(ScheduledJob.id.desc())).all())
        all_items = [_job_view(db, job, details=False) for job in all_jobs]
        requested_status = status_filter.strip().upper()
        filtered_items = [
            item for item in all_items
            if (
                str(item.get("result_status") or "").upper() in {"COMPLETED", "SUCCESS"}
                if requested_status in {"COMPLETED", "SUCCESS"}
                else str(item.get("result_status") or "").upper() == requested_status
            )
        ]
        total = len(filtered_items)
        items = filtered_items[offset:offset + limit]
    else:
        count_statement = select(func.count()).select_from(statement.subquery())
        total = int(db.scalar(count_statement) or 0)
        jobs = list(db.scalars(
            statement.order_by(ScheduledJob.id.desc()).offset(offset).limit(limit)
        ).all())
        items = [_job_view(db, job, details=False) for job in jobs]
    return {"items": items, "total": total, "limit": limit, "offset": offset}


@router.get("/details")
def list_stock_governance_details(
    governance_batch_id: str | None = None,
    governance_mode: str | None = Query(default=None, pattern="^(SYSTEM_GOVERNANCE|AI_AGENT_SKILL_GOVERNANCE)$"),
    board_code: str | None = None,
    market: str | None = None,
    symbol: str | None = None,
    limit: int = Query(default=200, ge=1, le=1000),
    offset: int = Query(default=0, ge=0),
    db: Session = Depends(get_db),
) -> dict:
    statement = select(StockGovernanceDetail)
    count_statement = select(func.count()).select_from(StockGovernanceDetail)
    predicates = []
    if governance_batch_id:
        predicates.append(StockGovernanceDetail.governance_batch_id == governance_batch_id)
    if governance_mode:
        predicates.append(StockGovernanceDetail.governance_mode == governance_mode)
    if board_code:
        predicates.append(StockGovernanceDetail.board_code == board_code.strip().upper())
    if market:
        predicates.append(StockGovernanceDetail.market == market.strip().upper())
    if symbol:
        predicates.append(StockGovernanceDetail.symbol == symbol.strip().upper())
    if predicates:
        statement = statement.where(*predicates)
        count_statement = count_statement.where(*predicates)
    rows = list(db.scalars(
        statement.order_by(StockGovernanceDetail.created_at.desc()).offset(offset).limit(limit)
    ).all())
    return {
        "items": [{
            "id": row.id,
            "governance_batch_id": row.governance_batch_id,
            "governance_mode": row.governance_mode,
            "governance_mode_name": (
                "AI+智能体+Skill治理"
                if row.governance_mode == "AI_AGENT_SKILL_GOVERNANCE"
                else "系统直接治理"
            ),
            "board_code": row.board_code,
            "market": row.market,
            "symbol": row.symbol,
            "stock_name": row.stock_name,
            "pipeline_run_id": row.pipeline_run_id,
            "agent_execution_run_id": row.agent_execution_run_id,
            "baseline_batch_id": row.baseline_batch_id,
            "status": row.status,
            "stage_status": row.stage_status_json,
            "source_ids": row.source_ids_json,
            "evidence_ids": row.evidence_ids_json,
            "quality_summary": row.quality_summary_json,
            "started_at": row.started_at,
            "completed_at": row.completed_at,
        } for row in rows],
        "total": int(db.scalar(count_statement) or 0),
        "limit": limit,
        "offset": offset,
    }


@router.get("/{job_id}")
def get_stock_batch_governance_job(job_id: int, db: Session = Depends(get_db)) -> dict:
    job = db.get(ScheduledJob, job_id)
    if job is None or job.task_type != TASK_TYPE:
        raise HTTPException(status_code=404, detail="批量采集治理任务不存在")
    return _job_view(db, job, details=True)


@router.post("/{job_id}/retry", status_code=202, response_model=StockBatchGovernanceResponse)
def retry_stock_batch_governance_job(
    job_id: int,
    db: Session = Depends(get_db),
) -> dict:
    """Retry a terminal/failed batch using its original, validated request.

    A retry is a new durable job and therefore receives a new idempotency key;
    the original job and its output remain immutable audit history.  Running
    jobs are deliberately rejected so the UI cannot create concurrent duplicate
    collection runs by repeatedly clicking a retry control.
    """
    job = db.get(ScheduledJob, job_id)
    if job is None or job.task_type != TASK_TYPE:
        raise HTTPException(status_code=404, detail="批量采集治理任务不存在")

    lifecycle = str(job.status or "").upper()
    run = db.get(PipelineRun, job.pipeline_run_id)
    output = dict((run.output_json if run else {}) or {})
    result_status = str(output.get("result_status") or "").upper()
    if lifecycle in {"PENDING", "RUNNING", "RETRY"}:
        raise HTTPException(status_code=409, detail="任务仍在队列或执行中，请等待当前任务结束")
    if lifecycle == "COMPLETED" and result_status not in {"PARTIAL", "FAILED"}:
        raise HTTPException(status_code=409, detail="只有部分完成或失败的任务可以重试")
    if lifecycle not in {"FAILED", "COMPLETED"}:
        raise HTTPException(status_code=409, detail=f"当前任务状态不支持重试：{lifecycle or 'UNKNOWN'}")

    original_request = dict(((job.payload_json or {}).get("request") or {}))
    if not original_request:
        raise HTTPException(status_code=409, detail="原任务没有可恢复的请求参数")
    original_request.pop("idempotency_key", None)
    original_request["idempotency_key"] = f"retry:{job_id}:{uuid4()}"
    try:
        retry_request = StockBatchGovernanceRequest.model_validate(original_request)
    except Exception as exc:
        raise HTTPException(status_code=409, detail={
            "message": "原任务请求已不再符合当前批量治理契约，无法安全重试",
            "error": str(exc)[:800],
        }) from exc
    return submit_stock_batch_governance(retry_request, db)
