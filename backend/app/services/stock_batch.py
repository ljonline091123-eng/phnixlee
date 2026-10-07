"""Bounded stock business-data and knowledge governance orchestration.

The orchestration is intentionally implemented as one durable worker task.
All writes reuse the existing upsert-based market-data services and the
versioned knowledge pipeline; no legacy graph or knowledge asset is reset.
"""

from __future__ import annotations

from datetime import date, datetime, timedelta, timezone
from typing import Any

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.markets import MARKET_CN_A, digit_length_for_market
from app.db.session import SessionLocal
from app.models.ai_hub import KnowledgeDocument
from app.models.market_data import DataFetchLog, DataSource, StockSymbol
from app.models.pipeline import PipelineRun
from app.schemas.stock_batch import (
    DOCUMENT_CHUNKS_CONTINUATION,
    MASTER_BUSINESS_TYPES,
    SCOPED_GRAPH_CONTINUATION,
    StockBatchGovernanceRequest,
)
from app.services import lakehouse
from app.services.lakehouse import current_knowledge_document_chunk_counts
from app.services.catalog import select_data_source
from app.services.f10 import _fetch_and_cache_f10_extended_data, _section_has_payload
from app.services.knowledge_pipeline import run_stock_pipeline
from app.services.stock_on_demand import StockOnDemandService


TASK_TYPE = "stock_batch_governance"


def _normalize_symbol(market: str, symbol: str) -> str:
    value = str(symbol or "").strip().upper()
    return value.zfill(digit_length_for_market(market)) if value.isdigit() else value


def _source_for(db: Session, market: str, capability: str) -> DataSource:
    source = select_data_source(db, market, capability, fallback_code="AKSHARE")
    if source is None or not source.enabled:
        raise LookupError(f"{market} 没有可用的 {capability} 数据源")
    return source


def _fetch_log_result(log: DataFetchLog, source_code: str | None = None) -> dict[str, Any]:
    return {
        "status": "SUCCESS" if log.status == "SUCCESS" else "FAILED",
        "source_code": source_code,
        "fetch_log_id": log.id,
        "total_count": int(log.total_count or 0),
        "persisted_count": int(log.persisted_count or 0),
        "error": log.error_message,
    }


def _fetch_one(
    db: Session,
    *,
    market: str,
    symbol: str,
    data_type: str,
    kline_days: int,
    disclosure_days: int,
) -> dict[str, Any]:
    source = _source_for(db, market, data_type)
    service = StockOnDemandService(db)
    today = date.today()
    if data_type == "QUOTE":
        log, _ = service.fetch_quote(source, market, symbol, persist=True)
    elif data_type == "KLINE":
        log, _ = service.fetch_kline(
            source, market, symbol, period="daily", adjust="",
            start_date=(today - timedelta(days=kline_days)).strftime("%Y%m%d"),
            end_date=today.strftime("%Y%m%d"), persist=True,
        )
    elif data_type == "FINANCIAL":
        indicator = "按报告期" if market == MARKET_CN_A else "报告期"
        log, _ = service.fetch_financials(source, market, symbol, indicator=indicator, persist=True)
    elif data_type == "NEWS":
        log, _ = service.fetch_news(source, market, symbol, persist=True)
    elif data_type == "NOTICE":
        log, _ = service.fetch_notices(
            source, market, symbol,
            start_date=(today - timedelta(days=disclosure_days)).strftime("%Y%m%d"),
            end_date=today.strftime("%Y%m%d"), persist=True,
        )
    else:
        raise ValueError(f"Unsupported business data type: {data_type}")
    return _fetch_log_result(log, source.source_code)


def _run_f10_refresh(db: Session, market: str, symbol: str) -> dict[str, Any]:
    """Refresh only F10 extensions, without silently fetching ten years of core data."""
    source = _source_for(db, market, "F10")
    sections = _fetch_and_cache_f10_extended_data(db, source, market, symbol)
    available_sections = [
        name for name, value in sections.items()
        if _section_has_payload(name, value, market)
    ]
    non_empty = len(available_sections)
    return {
        "status": "SUCCESS" if non_empty else "PARTIAL",
        "source_code": source.source_code,
        "section_count": non_empty,
        "sections": available_sections,
        "message": "仅刷新F10扩展资料；行情、K线、财务、新闻和公告按勾选项分别采集",
    }


def _collect_stock(
    db: Session,
    stock: StockSymbol,
    *,
    business_types: list[str],
    kline_days: int,
    disclosure_days: int,
) -> dict[str, Any]:
    requested = list(dict.fromkeys(item for item in business_types if item in MASTER_BUSINESS_TYPES))
    operations: dict[str, Any] = {}
    errors: list[dict[str, Any]] = []
    if "F10" in requested:
        try:
            operations["F10"] = _run_f10_refresh(db, stock.market, stock.symbol)
        except Exception as exc:
            # An adapter or F10 normalizer can fail after SQLAlchemy has
            # entered a failed transaction state (for example, a provider
            # payload violating a uniqueness constraint).  The remaining
            # business types must still be attempted for this stock; clear
            # only the uncommitted transaction state and preserve committed
            # rows from earlier stages.
            db.rollback()
            message = f"{type(exc).__name__}: {str(exc)[:400]}"
            operations["F10"] = {"status": "FAILED", "error": message}
            errors.append({"data_type": "F10", "message": message})

    for data_type in requested:
        if data_type == "F10":
            continue
        try:
            operations[data_type] = _fetch_one(
                db, market=stock.market, symbol=stock.symbol, data_type=data_type,
                kline_days=kline_days, disclosure_days=disclosure_days,
            )
        except Exception as exc:
            # Keep one remote/provider failure isolated to this data type.
            # StockOnDemandService normally commits its fetch log, but an
            # adapter or custom source may raise before its failure handler;
            # without rollback the next type would fail with
            # PendingRollbackError and the batch would report a cascade of
            # misleading errors.
            db.rollback()
            message = f"{type(exc).__name__}: {str(exc)[:400]}"
            fetch_log_id = getattr(exc, "fetch_log_id", None)
            operations[data_type] = {
                "status": "FAILED", "error": message, "fetch_log_id": fetch_log_id,
            }
            errors.append({"data_type": data_type, "message": message, "fetch_log_id": fetch_log_id})

    failed = sum(result.get("status") == "FAILED" for result in operations.values())
    partial = sum(result.get("status") == "PARTIAL" for result in operations.values())
    return {
        "market": stock.market,
        "symbol": stock.symbol,
        "name": stock.name,
        "status": "FAILED" if operations and failed == len(operations) else (
            "PARTIAL" if failed or partial else "SUCCESS"
        ),
        "operations": operations,
        "errors": errors,
    }


def _save_progress(
    db: Session,
    pipeline_run_id: int | None,
    output: dict[str, Any],
    *,
    stage: str,
    progress: int,
) -> None:
    output["current_stage"] = stage
    output["progress"] = max(0, min(100, int(progress)))
    if not pipeline_run_id:
        return
    row = db.get(PipelineRun, pipeline_run_id)
    if row is None:
        return
    row.current_stage = stage
    row.output_json = dict(output)
    db.commit()


def _chunked_document_ids(db: Session, documents: list[KnowledgeDocument]) -> set[str]:
    counts = current_knowledge_document_chunk_counts(db, documents)
    return {document_id for document_id, count in counts.items() if count > 0}


def _complete_missing_document_chunks(db: Session, stock: StockSymbol) -> dict[str, Any]:
    """Fill the actual chunk gaps for one stock across every graph version."""
    documents = list(db.scalars(
        select(KnowledgeDocument)
        .where(
            KnowledgeDocument.market == stock.market,
            KnowledgeDocument.symbol == stock.symbol,
        )
        .order_by(KnowledgeDocument.id)
    ).all())
    before_chunked = _chunked_document_ids(db, documents)
    missing = [document for document in documents if str(document.id) not in before_chunked]
    failures: list[dict[str, Any]] = []
    result_samples: list[dict[str, Any]] = []
    created_chunks = 0
    reused_chunks = 0
    for document in missing:
        try:
            result = lakehouse.create_chunks(
                db,
                document_key=f"knowledge_document:{document.id}",
                document_id=str(document.id),
                text=document.content or document.title,
                chunk_size=1800,
                overlap=180,
                parser_version="PIPELINE_STRUCTURE_V1",
                embedding_model="HASH_EMBED_V1",
                archive_original=True,
            )
            created_chunks += int(result.get("created_count") or 0)
            reused_chunks += int(result.get("reused_count") or 0)
            if int(result.get("chunk_count") or 0) <= 0:
                failures.append({
                    "document_id": document.id,
                    "title": document.title,
                    "error": "文档没有可生成切片的正文或标题",
                })
            elif len(result_samples) < 50:
                result_samples.append({
                    "document_id": document.id,
                    "graph_id": document.graph_id,
                    "title": document.title,
                    "created_count": int(result.get("created_count") or 0),
                    "reused_count": int(result.get("reused_count") or 0),
                })
        except Exception as exc:
            db.rollback()
            failures.append({
                "document_id": document.id,
                "graph_id": document.graph_id,
                "title": document.title,
                "error": f"{type(exc).__name__}: {str(exc)[:300]}",
            })

    after_chunked = _chunked_document_ids(db, documents)
    remaining_ids = [
        int(document.id) for document in documents if str(document.id) not in after_chunked
    ]
    graph_ids = sorted({int(document.graph_id) for document in documents if document.graph_id is not None})
    return {
        "market": stock.market,
        "symbol": stock.symbol,
        "status": "SUCCESS" if documents and not remaining_ids else "PARTIAL",
        "continuation_action": DOCUMENT_CHUNKS_CONTINUATION,
        "knowledge_base_mode": "ALL_GRAPH_VERSIONS_MISSING_ONLY",
        "documents_total": len(documents),
        "documents_selected": len(missing),
        "documents_with_chunks": len(after_chunked),
        "missing_documents_before": len(missing),
        "missing_documents_after": len(remaining_ids),
        "remaining_document_ids": remaining_ids[:50],
        "failed_documents": len(remaining_ids),
        "failed_document_samples": failures[:50],
        "created_chunk_count": created_chunks,
        "reused_chunk_count": reused_chunks,
        "graph_count": len(graph_ids),
        "graph_ids": graph_ids,
        "result_samples": result_samples,
        "coverage_ratio": round(len(after_chunked) / len(documents), 6) if documents else None,
        "coverage_status": (
            "NO_DOCUMENTS" if not documents else "COMPLETE" if not remaining_ids else "PARTIAL"
        ),
        "truncated": False,
    }


def _attach_verifiable_stage_results(output: dict[str, Any]) -> None:
    """Persist a compact explain/verify contract alongside raw stage facts.

    The HTTP adapter can derive the same contract for historical runs.  New
    runs store it directly as well, so PipelineRun consumers outside the UI do
    not need to reverse-engineer status words.
    """
    stages = output.get("stage_results") or {}

    def finish(stage: dict[str, Any], completed: list[dict[str, Any]], incomplete: list[dict[str, Any]],
               verification: dict[str, Any], verify_target: str) -> None:
        stage["completed_items"] = completed
        stage["incomplete_items"] = incomplete
        stage["pending_items"] = incomplete
        stage["reasons"] = [
            {"code": item.get("code"), "reason": item.get("reason")}
            for item in incomplete
        ]
        stage["verification"] = verification
        stage["verify_target"] = verify_target

    business = stages.get("BUSINESS_DATA") or {}
    business_completed: list[dict[str, Any]] = []
    business_incomplete: list[dict[str, Any]] = []
    labels = {
        "QUOTE": "实时行情", "KLINE": "历史量价", "FINANCIAL": "财务数据",
        "NEWS": "新闻资讯", "NOTICE": "公司公告", "F10": "F10资料",
    }
    for stock in output.get("stock_results") or []:
        for code, operation in (stock.get("operations") or {}).items():
            status = str((operation or {}).get("status") or "UNKNOWN").upper()
            total = (
                (operation or {}).get("section_count") if code == "F10"
                else (operation or {}).get("total_count")
            )
            if total is None:
                total = (operation or {}).get("persisted_count")
            done = status == "SUCCESS" and int(total or 0) > 0
            item = {
                "code": f"{stock.get('market')}:{stock.get('symbol')}:{code}",
                "label": labels.get(code, code),
                "status": "COMPLETED" if done else ("EMPTY" if status == "SUCCESS" else status),
                "record_count": int(total or 0),
                "reason": (
                    f"接口返回 {int(total or 0)} 条记录；完整采集事实见 details。"
                    if done else (operation or {}).get("error") or (operation or {}).get("message") or (
                        "接口调用成功，但未返回或未持久化有效记录。"
                        if status == "SUCCESS" else f"采集状态为 {status}。"
                    )
                ),
                "details": operation or {},
            }
            (business_completed if done else business_incomplete).append(item)
    finish(
        business, business_completed, business_incomplete,
        {"records": output.get("stock_results") or []},
        "/api/v1/stocks/{market}/{symbol}/pipeline-status-detail",
    )
    stages["BUSINESS_DATA"] = business

    lake = stages.get("LAKEHOUSE_EXPORT") or {}
    lake_completed: list[dict[str, Any]] = []
    lake_incomplete: list[dict[str, Any]] = []
    for run in lake.get("market_runs") or []:
        for dataset_code, dataset in (run.get("datasets") or {}).items():
            status = str((dataset or {}).get("status") or "UNKNOWN").upper()
            done = status == "PUBLISHED"
            item = {
                "code": f"{run.get('market')}:{dataset_code}", "label": dataset_code,
                "status": "COMPLETED" if done else status,
                "record_count": (dataset or {}).get("row_count"),
                "reason": (
                    "数据集已发布。" if done else (dataset or {}).get("error") or f"发布状态为 {status}。"
                ),
                "details": dataset or {},
            }
            (lake_completed if done else lake_incomplete).append(item)
    finish(
        lake, lake_completed, lake_incomplete,
        {"market_runs": lake.get("market_runs") or []}, "/api/v1/lakehouse/datasets",
    )
    stages["LAKEHOUSE_EXPORT"] = lake

    chunks = stages.get("DOCUMENT_CHUNKS") or {}
    chunk_completed: list[dict[str, Any]] = []
    chunk_incomplete: list[dict[str, Any]] = []
    for run in chunks.get("market_runs") or []:
        market = run.get("market") or "ALL"
        total = int(run.get("documents_total") or 0)
        selected = int(run.get("documents_selected") or 0)
        covered = int(run.get("documents_with_chunks") or 0)
        failed = int(run.get("failed_documents") or 0)
        continuation_mode = (
            str(run.get("continuation_action") or "").upper() == DOCUMENT_CHUNKS_CONTINUATION
        )
        selection = {
            "code": f"{market}:DOCUMENT_SELECTION",
            "label": f"{market} · 实际缺口识别" if continuation_mode else f"{market} · 知识文档选择",
            "status": "COMPLETED" if (selected or continuation_mode) else "NOT_STARTED",
            "record_count": selected,
            "reason": (
                f"跨 {run.get('graph_count', 0)} 个图谱版本识别到 {selected} 份未切片文档。"
                if continuation_mode else (
                    f"从 {total} 份知识文档中选择 {selected} 份进入切片。"
                    if selected else "没有选出可切片文档。"
                )
            ),
            "details": run,
        }
        (chunk_completed if (selected or continuation_mode) else chunk_incomplete).append(selection)
        fully_covered = bool(total and covered == total and not failed)
        coverage = {
            "code": f"{market}:CHUNK_COVERAGE", "label": f"{market} · 切片覆盖",
            "status": "COMPLETED" if fully_covered else ("PARTIAL" if covered else "NOT_STARTED"),
            "record_count": covered,
            "reason": (
                f"已切片 {covered}/{total} 份，仍有 {run.get('missing_documents_after', failed)} 份未覆盖。"
                if continuation_mode and total else
                f"已切片 {covered}/{total} 份，失败 {failed} 份。"
                if total else "该范围没有可切片知识文档。"
            ),
            "details": run,
        }
        (chunk_completed if fully_covered else chunk_incomplete).append(coverage)
        if str(run.get("knowledge_base_mode") or chunks.get("knowledge_base_mode") or "").upper() == "EXISTING_DOCUMENTS_ONLY":
            chunk_incomplete.append({
                "code": f"{market}:NEW_DATA_MATERIALIZATION",
                "label": f"{market} · 新业务数据知识物化",
                "status": "PARTIAL",
                "record_count": None,
                "reason": "本次只加工既有知识文档；新采集业务记录尚未全部物化为新知识文档。",
                "details": {"knowledge_base_mode": "EXISTING_DOCUMENTS_ONLY"},
            })
    finish(
        chunks, chunk_completed, chunk_incomplete,
        {"market_runs": chunks.get("market_runs") or []}, "/api/v1/lakehouse/chunks",
    )
    stages["DOCUMENT_CHUNKS"] = chunks

    graphs = stages.get("KNOWLEDGE_GRAPH") or {}
    graph_completed: list[dict[str, Any]] = []
    graph_incomplete: list[dict[str, Any]] = []
    scoped_graph_continuation = (
        str((output.get("effective_options") or {}).get("continuation_action") or "").upper()
        == SCOPED_GRAPH_CONTINUATION
    )
    for run in graphs.get("market_runs") or []:
        market = run.get("market") or "ALL"
        built = str(run.get("build_status") or "").upper() == "BUILT"
        governance = str(run.get("governance_status") or "PENDING").upper()
        governed = governance in {"GOVERNED", "LOCKED"}
        build_item = {
            "code": f"{market}:GRAPH_BUILD", "label": f"{market} · 图谱投影构建",
            "status": "COMPLETED" if built else str(run.get("build_status") or "NOT_STARTED"),
            "record_count": (run.get("counts") or {}).get("relations"),
            "reason": "范围图谱投影已构建。" if built else run.get("error") or "图谱投影尚未构建。",
            "details": run,
        }
        (graph_completed if built else graph_incomplete).append(build_item)
        if not scoped_graph_continuation:
            governance_item = {
                "code": f"{market}:GRAPH_GOVERNANCE", "label": f"{market} · 图谱治理发布",
                "status": "COMPLETED" if governed else governance,
                "record_count": None,
                "reason": (
                    f"图谱治理状态为 {governance}。" if governed
                    else run.get("next_action") or f"图谱治理状态为 {governance}，尚未发布。"
                ),
                "details": run,
            }
            (graph_completed if governed else graph_incomplete).append(governance_item)
    finish(
        graphs, graph_completed, graph_incomplete,
        {
            "market_runs": graphs.get("market_runs") or [],
            "governance_follow_up": scoped_graph_continuation,
        },
        "/api/v1/knowledge-network/explore",
    )
    stages["KNOWLEDGE_GRAPH"] = graphs
    output["stage_results"] = stages


def execute_stock_batch_governance(payload: dict[str, Any]) -> dict[str, Any]:
    """Worker entry point registered under :data:`TASK_TYPE`."""
    request = StockBatchGovernanceRequest.model_validate(payload.get("request") or payload)
    outer_run_id = payload.get("orchestration_pipeline_run_id")
    outer_job_id = payload.get("orchestration_job_id")
    effective_options = {
        "collect_business_data": request.collect_business_data,
        "export_lakehouse": request.export_lakehouse,
        "archive_chunks": request.archive_chunks,
        "run_graph": request.run_graph,
        "run_agent_governance": request.run_agent_governance,
        "continuation_action": request.continuation_action,
        "agent_governance_scope": (
            "GLOBAL_BOUNDED_SOURCE_AUDIT" if request.run_agent_governance else "NOT_REQUESTED"
        ),
        "business_types": request.business_types,
    }
    output: dict[str, Any] = {
        "result_status": "RUNNING",
        "progress": 0,
        "current_stage": "VALIDATION",
        "effective_options": effective_options,
        "stage_results": {},
        "stock_results": [],
        "errors": [],
        "pipeline_run_ids": [],
        "graph_ids": [],
    }

    with SessionLocal() as db:
        targets: list[StockSymbol] = []
        missing: list[dict[str, str]] = []
        for target in request.stocks:
            market = target.market.strip().upper()
            symbol = _normalize_symbol(market, target.symbol)
            stock = db.scalar(select(StockSymbol).where(
                StockSymbol.market == market, StockSymbol.symbol == symbol,
            ))
            if stock is None:
                missing.append({"market": market, "symbol": symbol})
            else:
                targets.append(stock)
        if missing:
            raise ValueError(f"以下股票不在主数据中: {missing}")

        output["stage_results"]["VALIDATION"] = {
            "status": "SUCCESS", "stock_count": len(targets), "message": "股票主数据校验通过",
        }
        if request.continuation_action == DOCUMENT_CHUNKS_CONTINUATION:
            _save_progress(db, outer_run_id, output, stage="DOCUMENT_CHUNKS", progress=10)
            chunk_run = _complete_missing_document_chunks(db, targets[0])
            chunk_status = str(chunk_run.get("status") or "PARTIAL").upper()
            output["stage_results"].update({
                "BUSINESS_DATA": {
                    "status": "SKIPPED", "message": "本次只补齐既有知识文档切片",
                },
                "LAKEHOUSE_EXPORT": {
                    "status": "SKIPPED", "message": "切片续作不重复发布湖仓数据集",
                },
                "DOCUMENT_CHUNKS": {
                    "status": chunk_status,
                    "market_runs": [chunk_run],
                    "continuation_action": DOCUMENT_CHUNKS_CONTINUATION,
                    "knowledge_base_mode": "ALL_GRAPH_VERSIONS_MISSING_ONLY",
                    "message": "已按股票范围补齐全部图谱版本中的实际未切片文档",
                },
                "KNOWLEDGE_GRAPH": {
                    "status": "SKIPPED", "message": "切片续作不会重建或治理知识图谱",
                },
                "AGENT_SKILL_GOVERNANCE": {
                    "status": "SKIPPED", "scope": "NOT_REQUESTED",
                    "message": "切片续作不执行智能体治理",
                },
                "KNOWLEDGE_PIPELINE": {
                    "status": chunk_status,
                    "market_runs": [{
                        "market": targets[0].market,
                        "symbols": [targets[0].symbol],
                        "status": chunk_status,
                        "chunks": chunk_run,
                        "continuation_action": DOCUMENT_CHUNKS_CONTINUATION,
                    }],
                    "message": "文档切片续作已完成实际缺口核验",
                },
            })
            output["errors"].extend({
                "stage": "DOCUMENT_CHUNKS",
                "market": targets[0].market,
                "symbol": targets[0].symbol,
                **item,
            } for item in chunk_run.get("failed_document_samples") or [])
            _attach_verifiable_stage_results(output)
            result_status = "COMPLETED" if chunk_status == "SUCCESS" else "PARTIAL"
            output["result_status"] = result_status
            output["stage_results"]["COMPLETE"] = {
                "status": result_status,
                "completed_at": datetime.now(timezone.utc).isoformat(),
                "message": "文档切片续作任务已结束",
            }
            _save_progress(db, outer_run_id, output, stage="COMPLETE", progress=100)
            return output

        _save_progress(db, outer_run_id, output, stage="BUSINESS_DATA", progress=5)

        business_issues = 0
        business_failed = 0
        if request.collect_business_data:
            total = max(1, len(targets))
            for index, stock in enumerate(targets, start=1):
                result = _collect_stock(
                    db, stock, business_types=request.business_types,
                    kline_days=request.kline_days, disclosure_days=request.disclosure_days,
                )
                output["stock_results"].append(result)
                if result["status"] != "SUCCESS":
                    business_issues += 1
                if result["status"] == "FAILED":
                    business_failed += 1
                for error in result["errors"]:
                    output["errors"].append({
                        "stage": "BUSINESS_DATA", "market": stock.market,
                        "symbol": stock.symbol, **error,
                    })
                _save_progress(
                    db, outer_run_id, output, stage="BUSINESS_DATA",
                    progress=5 + round(index / total * 50),
                )
            output["stage_results"]["BUSINESS_DATA"] = {
                "status": (
                    "SUCCESS" if not business_issues
                    else "FAILED" if business_failed == len(targets)
                    else "PARTIAL"
                ),
                "stock_count": len(targets),
                "success_count": len(targets) - business_issues,
                "partial_or_failed_count": business_issues,
                "failed_count": business_failed,
                "message": "业务数据采集完成；仅包含行情、K线、财务、新闻、公告和F10",
            }
        else:
            output["stage_results"]["BUSINESS_DATA"] = {
                "status": "SKIPPED", "message": "本次未选择业务数据采集",
            }

        knowledge_requested = any((
            request.export_lakehouse,
            request.archive_chunks,
            request.run_graph,
            request.run_agent_governance,
        ))
        child_statuses: list[str] = []
        existing_documents_only = bool(request.archive_chunks and not request.run_graph)
        if knowledge_requested:
            grouped: dict[str, list[str]] = {}
            for stock in targets:
                grouped.setdefault(stock.market, []).append(stock.symbol)
            market_results: list[dict[str, Any]] = []
            lakehouse_runs: list[dict[str, Any]] = []
            chunk_runs: list[dict[str, Any]] = []
            graph_runs: list[dict[str, Any]] = []
            agent_runs: list[dict[str, Any]] = []
            market_total = max(1, len(grouped))
            for index, (market, symbols) in enumerate(sorted(grouped.items()), start=1):
                run_global_agent_audit = bool(request.run_agent_governance and index == 1)
                try:
                    child_key = f"stock-batch:{outer_job_id or outer_run_id or 'manual'}:{market}"
                    result = run_stock_pipeline(
                        db,
                        market=market,
                        symbols=symbols,
                        knowledge_base_id=request.knowledge_base_id,
                        graph_id=request.graph_id,
                        export_lakehouse=request.export_lakehouse,
                        archive_chunks=request.archive_chunks,
                        run_graph=request.run_graph,
                        run_agent_governance=run_global_agent_audit,
                        include_company_tables=True,
                        idempotency_key=child_key,
                        resume_incomplete=True,
                    )
                    child_status = str(result.get("status") or "UNKNOWN")
                    child_run_id = result.get("pipeline_run_id")
                    graph_payload = result.get("graph") or {}
                    new_graph_id = (
                        graph_payload.get("graph_id") or result.get("graph_id")
                        if graph_payload.get("status") == "BUILT" and graph_payload.get("projection") is True
                        else None
                    )
                    if child_run_id:
                        output["pipeline_run_ids"].append(child_run_id)
                    if new_graph_id:
                        output["graph_ids"].append(new_graph_id)

                    exports = result.get("exports") or {}
                    export_statuses = [
                        str(item.get("status") or "UNKNOWN").upper()
                        for item in exports.values() if isinstance(item, dict)
                    ]
                    if not request.export_lakehouse:
                        lakehouse_status = "SKIPPED"
                    elif export_statuses and all(item == "PUBLISHED" for item in export_statuses):
                        lakehouse_status = "SUCCESS"
                    elif export_statuses and all(item in {"FAILED", "ERROR"} for item in export_statuses):
                        lakehouse_status = "FAILED"
                    else:
                        lakehouse_status = "PARTIAL"
                    lakehouse_runs.append({
                        "market": market,
                        "status": lakehouse_status,
                        "dataset_statuses": {
                            name: item.get("status") for name, item in exports.items()
                            if isinstance(item, dict)
                        },
                        # Keep the full child-pipeline result so a reviewer can
                        # verify dataset ids, versions, row counts, quality and
                        # errors instead of seeing only a derived status word.
                        "datasets": exports,
                    })

                    chunks = result.get("chunks") or {}
                    if not request.archive_chunks:
                        chunk_status = "SKIPPED"
                    elif existing_documents_only:
                        chunk_status = "PARTIAL"
                    elif (
                        int(chunks.get("documents_total") or 0) > 0
                        and int(chunks.get("documents_with_chunks") or 0)
                            == int(chunks.get("documents_total") or 0)
                        and not chunks.get("truncated")
                        and not int(chunks.get("failed_documents") or 0)
                    ):
                        chunk_status = "SUCCESS"
                    else:
                        chunk_status = "PARTIAL"
                    chunk_runs.append({
                        "market": market,
                        "status": chunk_status,
                        "documents_total": int(chunks.get("documents_total") or 0),
                        "documents_selected": int(chunks.get("documents_selected") or 0),
                        "documents_with_chunks": int(chunks.get("documents_with_chunks") or 0),
                        "failed_documents": int(chunks.get("failed_documents") or 0),
                        "failed_document_samples": chunks.get("failed_document_samples") or [],
                        "truncated": bool(chunks.get("truncated")),
                        "coverage_ratio": chunks.get("coverage_ratio"),
                        "document_selection_ratio": chunks.get("document_selection_ratio"),
                        "successful_chunk_ratio": chunks.get("successful_chunk_ratio"),
                        "selection_policy": chunks.get("selection_policy"),
                        "created_chunk_count": int(chunks.get("created_chunk_count") or 0),
                        "reused_chunk_count": int(chunks.get("reused_chunk_count") or 0),
                        "coverage_status": chunks.get("coverage_status"),
                        "details": chunks,
                        "knowledge_base_mode": (
                            "EXISTING_DOCUMENTS_ONLY" if existing_documents_only
                            else "SCOPED_GRAPH_DOCUMENTS_AND_CHUNKS"
                        ),
                    })

                    raw_graph_status = str(graph_payload.get("status") or "NOT_REQUESTED").upper()
                    graph_governance_status = str(
                        (result.get("completion") or {}).get("graph_governance_status") or ""
                    ).upper()
                    graph_counts = graph_payload.get("counts") or {}
                    scoped_projection_complete = bool(
                        request.continuation_action == SCOPED_GRAPH_CONTINUATION
                        and raw_graph_status == "BUILT"
                        and new_graph_id
                        and int(graph_counts.get("documents_created", graph_counts.get("documents", 0)) or 0) > 0
                        and int(graph_counts.get("entities_created", graph_counts.get("entities", 0)) or 0) > 0
                        and int(graph_counts.get("relations_created", graph_counts.get("relations", 0)) or 0) > 0
                    )
                    graph_status = (
                        "SKIPPED" if not request.run_graph
                        else "SUCCESS" if scoped_projection_complete
                        else "SUCCESS" if (
                            raw_graph_status == "BUILT"
                            and graph_governance_status in {"GOVERNED", "LOCKED"}
                        )
                        else "PARTIAL" if raw_graph_status == "BUILT"
                        else "FAILED" if raw_graph_status == "FAILED"
                        else "PARTIAL"
                    )
                    graph_runs.append({
                        "market": market,
                        "status": graph_status,
                        "build_status": raw_graph_status,
                        "graph_id": new_graph_id,
                        "base_graph_id": result.get("base_graph_id"),
                        "governance_status": graph_governance_status or None,
                        "counts": graph_counts,
                        "error": graph_payload.get("error"),
                        "details": graph_payload,
                        "completion_issues": [
                            issue for issue in ((result.get("completion") or {}).get("issues") or [])
                            if issue.get("stage") == "KNOWLEDGE_GRAPH"
                        ],
                        "next_action": (result.get("completion") or {}).get("next_action"),
                    })

                    if run_global_agent_audit:
                        agent_payload = result.get("agent_governance") or {}
                        raw_agent_status = str(agent_payload.get("status") or "UNKNOWN").upper()
                        agent_status = (
                            "SUCCESS" if raw_agent_status in {"SUCCESS", "COMPLETED", "PASSED"}
                            else "FAILED" if raw_agent_status == "FAILED"
                            else "PARTIAL"
                        )
                        agent_runs.append({
                            "market_trigger": market,
                            "status": agent_status,
                            "audit_status": raw_agent_status,
                            "scope": "GLOBAL_BOUNDED_SOURCE_AUDIT",
                            "run_id": agent_payload.get("run_id"),
                        })
                    effective_child_status = child_status
                    if request.continuation_action == SCOPED_GRAPH_CONTINUATION:
                        effective_child_status = (
                            "COMPLETED" if graph_status == "SUCCESS" and chunk_status == "SUCCESS"
                            else "FAILED" if graph_status == "FAILED" or chunk_status == "FAILED"
                            else "PARTIAL"
                        )
                    child_statuses.append(effective_child_status)
                    market_results.append({
                        "market": market,
                        "symbols": symbols,
                        "status": effective_child_status,
                        "pipeline_run_id": child_run_id,
                        "graph_id": new_graph_id,
                        "selected_graph_id": result.get("graph_id"),
                        "base_graph_id": result.get("base_graph_id"),
                        "completion": result.get("completion") or {},
                        "lineage_batch_id": result.get("lineage_batch_id"),
                        "exports": exports,
                        "chunks": chunks,
                        "graph": graph_payload,
                        "agent_governance": result.get("agent_governance") or {},
                        "knowledge_base_mode": (
                            "EXISTING_DOCUMENTS_ONLY" if existing_documents_only
                            else "SCOPED_GRAPH_DOCUMENTS_AND_CHUNKS"
                        ),
                    })
                except Exception as exc:
                    # A market pipeline is intentionally best-effort.  Clear
                    # any failed child transaction before recording the
                    # market-level diagnostic so the next market can run.
                    db.rollback()
                    message = f"{type(exc).__name__}: {str(exc)[:500]}"
                    child_statuses.append("FAILED")
                    market_results.append({
                        "market": market, "symbols": symbols, "status": "FAILED", "error": message,
                    })
                    if request.export_lakehouse:
                        lakehouse_runs.append({"market": market, "status": "FAILED", "error": message})
                    if request.archive_chunks:
                        chunk_runs.append({"market": market, "status": "FAILED", "error": message})
                    if request.run_graph:
                        graph_runs.append({"market": market, "status": "FAILED", "error": message})
                    if run_global_agent_audit:
                        agent_runs.append({
                            "market_trigger": market, "status": "FAILED", "error": message,
                            "scope": "GLOBAL_BOUNDED_SOURCE_AUDIT",
                        })
                    output["errors"].append({
                        "stage": "KNOWLEDGE_PIPELINE", "market": market, "message": message,
                    })
                _save_progress(
                    db, outer_run_id, output, stage="KNOWLEDGE_PIPELINE",
                    progress=55 + round(index / market_total * 40),
                )
            def aggregate_stage(runs: list[dict[str, Any]], requested: bool) -> str:
                if not requested:
                    return "SKIPPED"
                statuses = [str(item.get("status") or "PARTIAL") for item in runs]
                if statuses and all(item == "SUCCESS" for item in statuses):
                    return "SUCCESS"
                if statuses and all(item == "FAILED" for item in statuses):
                    return "FAILED"
                return "PARTIAL"

            lakehouse_stage_status = aggregate_stage(lakehouse_runs, request.export_lakehouse)
            chunk_stage_status = aggregate_stage(chunk_runs, request.archive_chunks)
            graph_stage_status = aggregate_stage(graph_runs, request.run_graph)
            agent_stage_status = aggregate_stage(agent_runs, request.run_agent_governance)
            output["stage_results"]["LAKEHOUSE_EXPORT"] = {
                "status": lakehouse_stage_status,
                "market_runs": lakehouse_runs,
                "message": "湖仓数据按所选股票范围导出；空源和质量门禁会保留为PARTIAL",
            }
            output["stage_results"]["DOCUMENT_CHUNKS"] = {
                "status": chunk_stage_status,
                "market_runs": chunk_runs,
                "knowledge_base_mode": (
                    "EXISTING_DOCUMENTS_ONLY" if existing_documents_only
                    else "SCOPED_GRAPH_DOCUMENTS_AND_CHUNKS"
                ),
                "message": (
                    "未生成知识图谱时只能切分既有知识文档，本批新业务数据尚未物化为新文档"
                    if existing_documents_only
                    else "范围图谱生成的知识文档已进入切片与血缘流程"
                ),
            }
            output["stage_results"]["KNOWLEDGE_GRAPH"] = {
                "status": graph_stage_status,
                "market_runs": graph_runs,
                "message": (
                    "单股续作仅核验新投影、实体和关系；PENDING治理状态作为后续发布提示，不覆盖历史图谱"
                    if request.continuation_action == SCOPED_GRAPH_CONTINUATION
                    else "仅返回本批新建的范围投影图；PENDING投影保留为PARTIAL，基础图谱不会计入新生成图谱"
                ),
            }
            output["stage_results"]["AGENT_SKILL_GOVERNANCE"] = {
                "status": agent_stage_status,
                "runs": agent_runs,
                "scope": (
                    "GLOBAL_BOUNDED_SOURCE_AUDIT" if request.run_agent_governance else "NOT_REQUESTED"
                ),
                "message": "该智能体审查是全局有界来源审计，不代表逐只股票治理",
            }
            requested_stage_statuses = [
                status for status, requested in (
                    (lakehouse_stage_status, request.export_lakehouse),
                    (chunk_stage_status, request.archive_chunks),
                    (graph_stage_status, request.run_graph),
                    (agent_stage_status, request.run_agent_governance),
                ) if requested
            ]
            knowledge_success = bool(requested_stage_statuses) and all(
                status == "SUCCESS" for status in requested_stage_statuses
            )
            output["stage_results"]["KNOWLEDGE_PIPELINE"] = {
                "status": "SUCCESS" if knowledge_success else "PARTIAL",
                "market_runs": market_results,
                "agent_governance_scope": (
                    "GLOBAL_BOUNDED_SOURCE_AUDIT" if request.run_agent_governance else "NOT_REQUESTED"
                ),
                "knowledge_base_mode": (
                    "EXISTING_DOCUMENTS_ONLY" if existing_documents_only
                    else "SCOPED_GRAPH_DOCUMENTS_AND_CHUNKS"
                ),
                "message": (
                    "未生成知识图谱时只能切分既有知识文档，本批新业务数据尚未物化为新文档"
                    if existing_documents_only
                    else "已按市场分组完成湖仓、知识文档切片和范围知识图谱管道"
                ),
            }
        else:
            for stage_code in (
                "LAKEHOUSE_EXPORT", "DOCUMENT_CHUNKS", "KNOWLEDGE_GRAPH", "AGENT_SKILL_GOVERNANCE",
            ):
                output["stage_results"][stage_code] = {
                    "status": "SKIPPED", "message": "本次未选择该阶段",
                }
            output["stage_results"]["KNOWLEDGE_PIPELINE"] = {
                "status": "SKIPPED", "message": "本次未选择湖仓或知识加工阶段",
            }

        # Persist the explainable stage contract for newly completed runs.
        # The API additionally derives it for historical runs written before
        # this field existed.
        _attach_verifiable_stage_results(output)

        has_failed_child = any(status == "FAILED" for status in child_statuses)
        has_partial_child = (
            any(status not in {"COMPLETED"} for status in child_statuses)
            or existing_documents_only
            or (
                knowledge_requested
                and (output["stage_results"].get("KNOWLEDGE_PIPELINE") or {}).get("status") != "SUCCESS"
            )
        )
        requested_unit_count = (
            (len(targets) if request.collect_business_data else 0)
            + (len(child_statuses) if knowledge_requested else 0)
        )
        failed_unit_count = business_failed + sum(status == "FAILED" for status in child_statuses)
        if requested_unit_count and failed_unit_count == requested_unit_count:
            result_status = "FAILED"
        elif business_issues or has_failed_child or has_partial_child or output["errors"]:
            result_status = "PARTIAL"
        else:
            result_status = "COMPLETED"
        output["result_status"] = result_status
        output["stage_results"]["COMPLETE"] = {
            "status": result_status,
            "completed_at": datetime.now(timezone.utc).isoformat(),
            "message": "批量采集与治理任务已结束",
        }
        _save_progress(db, outer_run_id, output, stage="COMPLETE", progress=100)
        return output
