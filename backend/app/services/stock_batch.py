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
from app.models.market_data import DataFetchLog, DataSource, StockSymbol
from app.models.pipeline import PipelineRun
from app.schemas.stock_batch import MASTER_BUSINESS_TYPES, StockBatchGovernanceRequest
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
                    )
                    child_status = str(result.get("status") or "UNKNOWN")
                    child_statuses.append(child_status)
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
                    })

                    chunks = result.get("chunks") or {}
                    if not request.archive_chunks:
                        chunk_status = "SKIPPED"
                    elif existing_documents_only:
                        chunk_status = "PARTIAL"
                    elif (
                        int(chunks.get("documents_total") or 0) > 0
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
                        "documents_with_chunks": int(chunks.get("documents_with_chunks") or 0),
                        "coverage_status": chunks.get("coverage_status"),
                        "knowledge_base_mode": (
                            "EXISTING_DOCUMENTS_ONLY" if existing_documents_only
                            else "SCOPED_GRAPH_DOCUMENTS_AND_CHUNKS"
                        ),
                    })

                    raw_graph_status = str(graph_payload.get("status") or "NOT_REQUESTED").upper()
                    graph_governance_status = str(
                        (result.get("completion") or {}).get("graph_governance_status") or ""
                    ).upper()
                    graph_status = (
                        "SKIPPED" if not request.run_graph
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
                    market_results.append({
                        "market": market,
                        "symbols": symbols,
                        "status": child_status,
                        "pipeline_run_id": child_run_id,
                        "graph_id": new_graph_id,
                        "selected_graph_id": result.get("graph_id"),
                        "base_graph_id": result.get("base_graph_id"),
                        "completion": result.get("completion") or {},
                        "lineage_batch_id": result.get("lineage_batch_id"),
                        "knowledge_base_mode": (
                            "EXISTING_DOCUMENTS_ONLY" if existing_documents_only
                            else "SCOPED_GRAPH_DOCUMENTS_AND_CHUNKS"
                        ),
                    })
                except Exception as exc:
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
                "message": "仅返回本批新建的范围投影图；PENDING投影保留为PARTIAL，基础图谱不会计入新生成图谱",
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
