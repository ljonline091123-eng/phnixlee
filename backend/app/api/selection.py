from __future__ import annotations

from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import and_, func, or_, select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.selection import SelectionCandidate, SelectionRun, SelectionSnapshot, SelectionTracking
from app.models.decision_review import SelectionDecisionSnapshot, SelectionRetrospective
from app.schemas.selection import (
    SelectionBackfillRequest,
    SelectionCandidateRead,
    SelectionRefreshRequest,
    SelectionReviewRequest,
    SelectionRunCreate,
    SelectionPreflightRequest,
    SelectionModelSwitchRequest,
    SelectionRunRead,
    SelectionSnapshotRead,
    SelectionTrackingRead,
)
from app.services.selection import (
    create_selection_run, queue_failed_selection_reanalysis, queue_selection_run,
    queue_failed_selection_reanalysis_with_model, queue_selection_data_backfill,
    probe_selection_model, refresh_tracking, review_candidate,
)
from app.models.ai_hub import (
    KnowledgeBase, KnowledgeDocument, KnowledgeEntity, KnowledgeGraph,
    KnowledgeRelation, ModelCallLog,
)
from app.schemas.selection import SelectionScopePreview
from app.services.graph_rag import build_selection_context
from app.services.model_errors import classify_model_error
from app.services.selection_audit import retrospective_read, snapshot_read

router = APIRouter(prefix="/selection", tags=["Selection Watch"])


def _candidate_read(db: Session, candidate: SelectionCandidate) -> SelectionCandidateRead:
    tracking = db.scalar(select(SelectionTracking).where(SelectionTracking.candidate_id == candidate.id))
    return SelectionCandidateRead(
        **{key: getattr(candidate, key) for key in (
            "id", "run_id", "market", "symbol", "name", "entry_price", "entry_date", "hard_score",
            "hard_rules_json", "evidence_json", "analysis_json", "decision", "review_notes", "target_price",
            "stop_price", "target_return_pct", "confidence", "prediction_id", "reviewed_at", "created_at",
        )},
        tracking_id=tracking.id if tracking else None,
    )


def _run_read(db: Session, run: SelectionRun, include_candidates: bool = True) -> SelectionRunRead:
    criteria_json = dict(run.criteria_json or {})
    # The worker writes a durable model-call log before it sends the request.
    # Use that log as a read-side fallback when a separate heartbeat thread is
    # delayed or SQLite briefly rejects its write. This keeps the UI honest
    # about the active provider and current purpose instead of leaving it at a
    # static 40% stage.
    if run.status in {"QUEUED", "RUNNING"}:
        progress = dict(criteria_json.get("progress") or {})
        logs = list(db.scalars(
            select(ModelCallLog).where(
                ModelCallLog.task_type == "stock_screening",
                ModelCallLog.completed_at.is_(None),
            ).order_by(ModelCallLog.started_at.desc()).limit(20)
        ).all())
        active = None
        for log in logs:
            request = log.request_json if isinstance(log.request_json, dict) else {}
            metadata = request.get("metadata_json") if isinstance(request.get("metadata_json"), dict) else {}
            if str(metadata.get("selection_run_id") or "") == str(run.id):
                active = (log, metadata)
                break
            # Calls created before the run id was included are only eligible
            # when they started after this run and use the selected instance.
            started_at = log.started_at
            created_at = run.created_at
            if started_at and created_at:
                if started_at.tzinfo is None and created_at.tzinfo is not None:
                    started_at = started_at.replace(tzinfo=created_at.tzinfo)
                elif started_at.tzinfo is not None and created_at.tzinfo is None:
                    created_at = created_at.replace(tzinfo=started_at.tzinfo)
            if started_at and created_at and started_at >= created_at and (
                not run.model_instance_code or log.instance_code == run.model_instance_code
            ):
                active = (log, metadata)
                break
        if active:
            log, metadata = active
            started_at = log.started_at
            if started_at and started_at.tzinfo is None:
                started_at = started_at.replace(tzinfo=timezone.utc)
            elapsed_seconds = max(0, int((datetime.now(timezone.utc) - started_at).total_seconds())) if started_at else 0
            batch_index = int(progress.get("batch_index") or metadata.get("batch_index") or 1)
            batch_total = max(1, int(progress.get("batch_total") or 1))
            stage_start = 40 + round(50 * max(0, batch_index - 1) / batch_total)
            stage_end = 40 + round(50 * batch_index / batch_total) - 1
            progress.update({
                "activity": progress.get("activity") or f"正在调用 {log.provider_code or '模型服务'} 的 {log.instance_code or '自动路由'}",
                "model_instance_code": log.instance_code or progress.get("model_instance_code") or run.model_instance_code,
                "active_model_call_id": log.id,
                "provider_code": log.provider_code,
                "model_code": log.model_code,
                "purpose": metadata.get("purpose") or progress.get("purpose") or "STOCK_SELECTION_ANALYSIS",
                "elapsed_seconds": max(int(progress.get("elapsed_seconds") or 0), elapsed_seconds),
                "activity_started_at": progress.get("activity_started_at") or started_at.isoformat() if started_at else progress.get("activity_started_at"),
                "last_activity_at": datetime.now(timezone.utc).isoformat(),
                "heartbeat_at": datetime.now(timezone.utc).isoformat(),
                "percent": min(stage_end, max(int(progress.get("percent") or stage_start), stage_start + elapsed_seconds // 5)),
                "batch_index": batch_index,
                "batch_total": batch_total,
                "candidate_symbols": progress.get("candidate_symbols") or metadata.get("candidate_symbols") or [],
                "updated_at": datetime.now(timezone.utc).isoformat(),
            })
            criteria_json["progress"] = progress
    payload = {key: getattr(run, key) for key in (
        "id", "market", "universe_scope", "candidate_limit", "criteria_json", "model_instance_code",
        "knowledge_base_ids_json", "graph_ids_json", "data_mode", "analysis_mode", "status", "candidate_count",
        "reviewed_count", "tracking_count", "missing_data_json", "error_message", "as_of", "completed_at",
        "created_at", "updated_at",
    )}
    payload["criteria_json"] = criteria_json
    if include_candidates:
        candidates = list(db.scalars(
            select(SelectionCandidate).where(SelectionCandidate.run_id == run.id).order_by(SelectionCandidate.hard_score.desc())
        ).all())
        candidates.sort(key=lambda row: (
            row.analysis_json.get("recommendation") == "WATCH",
            row.analysis_json.get("recommendation") != "PASS",
            row.analysis_json.get("confidence") or 0,
            row.hard_score,
        ), reverse=True)
        payload["candidates"] = [_candidate_read(db, item) for item in candidates]
    return SelectionRunRead(**payload)


def _tracking_read(db: Session, row: SelectionTracking, include_snapshots: bool = True) -> SelectionTrackingRead:
    payload = {key: getattr(row, key) for key in (
        "id", "candidate_id", "market", "symbol", "entry_price", "entry_date", "confirmation_date", "adjust", "target_price", "stop_price",
        "target_return_pct", "confidence", "required_sessions", "observed_sessions", "status", "latest_date",
        "latest_price", "cumulative_return_pct", "max_drawdown_pct", "target_hit", "stop_hit", "direction_hit", "data_source", "data_mode",
        "review_json", "started_at", "completed_at",
    )}
    payload["snapshots"] = []
    if include_snapshots:
        payload["snapshots"] = [SelectionSnapshotRead.model_validate(item) for item in db.scalars(
            select(SelectionSnapshot).where(SelectionSnapshot.tracking_id == row.id).order_by(SelectionSnapshot.sequence)
        ).all()]
    return SelectionTrackingRead(**payload)


@router.post("/runs", response_model=SelectionRunRead, status_code=201)
def create_run(payload: SelectionRunCreate, db: Session = Depends(get_db)) -> SelectionRunRead:
    try:
        return _run_read(db, create_selection_run(db, payload))
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc


@router.post("/runs/start", response_model=SelectionRunRead, status_code=202)
def start_run(payload: SelectionRunCreate, db: Session = Depends(get_db)) -> SelectionRunRead:
    try:
        return _run_read(db, queue_selection_run(db, payload))
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc


@router.post("/runs/{run_id}/retry", response_model=SelectionRunRead, status_code=202)
def retry_run(run_id: int, db: Session = Depends(get_db)) -> SelectionRunRead:
    """Create a new auditable attempt after a whole selection job failed."""
    original = db.get(SelectionRun, run_id)
    if original is None:
        raise HTTPException(status_code=404, detail="选股任务不存在")
    if original.status != "FAILED":
        raise HTTPException(status_code=409, detail="只有整批执行失败的任务可以重新执行；模型失败项请使用逐股重试")
    request_data = (original.criteria_json or {}).get("request")
    if not isinstance(request_data, dict):
        raise HTTPException(status_code=409, detail="原任务没有可复用的请求快照，无法安全重试")
    try:
        payload = SelectionRunCreate.model_validate(request_data)
        return _run_read(db, queue_selection_run(db, payload, retry_of_run_id=original.id))
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc


@router.get("/acceptance")
def acceptance(db: Session = Depends(get_db)) -> dict:
    graph = db.scalar(select(KnowledgeGraph).where(KnowledgeGraph.graph_code == "FIXED20_ACCEPTANCE_20261021"))
    if graph is None:
        return {"prepared": False, "message": "20股验证数据尚未准备完成", "graph_id": None,
                "knowledge_base_id": None, "graph_status": "MISSING", "market": "CN_A",
                "symbols": [], "rows": [], "scope_notes": [], "prepared_at": None,
                "datasets": {}, "coverage": {}}
    report = graph.governance_report_json or {}
    return {"prepared": bool(report.get("acceptance_passed")), "graph_id": graph.id,
            "knowledge_base_id": graph.knowledge_base_id, "graph_status": graph.governance_status,
            "market": report.get("market", "CN_A"), "symbols": report.get("symbols", []),
            "rows": report.get("acceptance_rows", []), "scope_notes": report.get("scope_notes", []),
            "prepared_at": report.get("prepared_at"), "datasets": report.get("datasets", {}),
            "coverage": report.get("coverage", {})}


@router.post("/scope-preview")
def scope_preview(payload: SelectionScopePreview, db: Session = Depends(get_db)) -> dict:
    """Read-only visibility into the exact selected KB/graph intersection."""
    bases = list(db.scalars(select(KnowledgeBase).where(KnowledgeBase.id.in_(payload.knowledge_base_ids))).all())
    graphs = list(db.scalars(select(KnowledgeGraph).where(KnowledgeGraph.id.in_(payload.graph_ids))).all())
    if (len(bases) != len(set(payload.knowledge_base_ids)) or any(not row.enabled for row in bases)
            or len(graphs) != len(set(payload.graph_ids)) or any(not row.enabled or row.knowledge_base_id not in payload.knowledge_base_ids for row in graphs)):
        raise HTTPException(status_code=422, detail="知识库或图谱不可用，或图谱不属于所选知识库")
    selected_bases, selected_graphs = bases, graphs
    usable_graphs = [row for row in selected_graphs if row.governance_status in {"GOVERNED", "LOCKED"}]
    usable_graph_ids = {row.id for row in usable_graphs}
    # Include unselected options in the catalog so users can compare coverage
    # before choosing. Only the selected, governed scope contributes to totals.
    bases = list(db.scalars(select(KnowledgeBase).where(KnowledgeBase.enabled.is_(True))).all())
    base_ids = [row.id for row in bases]
    graphs = list(db.scalars(select(KnowledgeGraph).where(
        KnowledgeGraph.enabled.is_(True), KnowledgeGraph.knowledge_base_id.in_(base_ids),
    )).all())
    graph_ids = [row.id for row in graphs]
    from app.services.graph_rag import selection_period_bounds
    period_start, period_end = selection_period_bounds(
        datetime.now(timezone.utc), payload.period_months, payload.market,
    )
    published = func.coalesce(*(KnowledgeDocument.metadata_json[key].as_string()
        for key in ("published_at", "news_time", "notice_date", "report_period", "trade_date")))
    base_query = select(KnowledgeDocument.knowledge_base_id, KnowledgeDocument.symbol,
                        KnowledgeDocument.source_table, func.count(KnowledgeDocument.id)).where(
        KnowledgeDocument.market == payload.market.upper(),
        KnowledgeDocument.knowledge_base_id.in_(base_ids),
        or_(published.is_(None), and_(published >= period_start.isoformat(), published <= period_end.isoformat() + "T23:59:59")),
    )
    if payload.symbols:
        base_query = base_query.where(KnowledgeDocument.symbol.in_(payload.symbols))
    base_counts = db.execute(base_query.group_by(KnowledgeDocument.knowledge_base_id,
        KnowledgeDocument.symbol, KnowledgeDocument.source_table)).all()

    effective_query = select(KnowledgeDocument.knowledge_base_id, KnowledgeDocument.graph_id,
                             KnowledgeDocument.symbol, KnowledgeDocument.source_table,
                             func.count(KnowledgeDocument.id)).where(
        KnowledgeDocument.market == payload.market.upper(),
        KnowledgeDocument.knowledge_base_id.in_(base_ids),
        KnowledgeDocument.graph_id.in_(graph_ids),
        or_(published.is_(None), and_(published >= period_start.isoformat(), published <= period_end.isoformat() + "T23:59:59")),
    )
    if payload.symbols:
        effective_query = effective_query.where(KnowledgeDocument.symbol.in_(payload.symbols))
    counts = db.execute(effective_query.group_by(KnowledgeDocument.knowledge_base_id,
        KnowledgeDocument.graph_id, KnowledgeDocument.symbol, KnowledgeDocument.source_table)).all() if graph_ids else []
    symbols, base_symbols, totals, kb_stats, graph_stats = {}, {}, {}, {}, {}
    for kb_id, symbol, source, count in base_counts:
        kb = kb_stats.setdefault(kb_id, {"document_count": 0, "symbols": set(), "source_counts": {}})
        kb["document_count"] += count
        if symbol:
            kb["symbols"].add(symbol)
        if symbol and kb_id in payload.knowledge_base_ids:
            base_symbols.setdefault(symbol, {})[source] = base_symbols.setdefault(symbol, {}).get(source, 0) + count
        kb["source_counts"][source] = kb["source_counts"].get(source, 0) + count
    for kb_id, graph_id, symbol, source, count in counts:
        if graph_id in usable_graph_ids:
            if symbol:
                symbols.setdefault(symbol, {})[source] = symbols.setdefault(symbol, {}).get(source, 0) + count
            totals[source] = totals.get(source, 0) + count
        kb = kb_stats.setdefault(kb_id, {"document_count": 0, "symbols": set(), "source_counts": {}})
        kb["effective_document_count"] = kb.get("effective_document_count", 0) + count
        graph = graph_stats.setdefault(graph_id, {"document_count": 0, "symbols": set(), "source_counts": {}})
        graph["document_count"] += count
        if symbol:
            graph["symbols"].add(symbol)
        graph["source_counts"][source] = graph["source_counts"].get(source, 0) + count

    stock_entity_query = select(KnowledgeEntity).where(
        KnowledgeEntity.graph_id.in_(graph_ids), KnowledgeEntity.entity_type.in_(["STOCK", "SECURITY"]),
        or_(KnowledgeEntity.entity_key.startswith(f"{payload.market.upper()}:"),
            KnowledgeEntity.entity_key.contains(f":{payload.market.upper()}:")),
    )
    if payload.symbols:
        stock_keys = [f"{payload.market.upper()}:{symbol}" for symbol in payload.symbols]
        namespaced_keys = [KnowledgeEntity.entity_key.endswith(f":{payload.market.upper()}:{symbol}") for symbol in payload.symbols]
        stock_entity_query = stock_entity_query.where(or_(KnowledgeEntity.entity_key.in_(stock_keys), *namespaced_keys))
    stock_entities = list(db.scalars(stock_entity_query).all()) if graph_ids else []
    stock_by_entity_id = {row.id: row for row in stock_entities}
    stock_relation_counts: dict[str, int] = {}
    stock_relation_types: dict[str, dict[str, int]] = {}
    for entity in stock_entities:
        properties = entity.properties_json or {}
        symbol = str(properties.get("symbol") or entity.entity_key.rsplit(":", 1)[-1])
        stock_relation_counts.setdefault(symbol, 0)
        stock_relation_types.setdefault(symbol, {})
    relations = list(db.execute(select(
        KnowledgeRelation.graph_id, KnowledgeRelation.subject_entity_id, KnowledgeRelation.predicate,
        KnowledgeRelation.object_entity_id,
    ).where(
        KnowledgeRelation.graph_id.in_(graph_ids),
        or_(KnowledgeRelation.subject_entity_id.in_(stock_by_entity_id),
            KnowledgeRelation.object_entity_id.in_(stock_by_entity_id)),
    )).all()) if stock_by_entity_id else []
    for graph_id, subject_id, predicate, object_id in relations:
        graph = graph_stats.setdefault(graph_id, {"document_count": 0, "symbols": set(), "source_counts": {}, "direct_relation_count": 0, "relation_type_counts": {}})
        graph["direct_relation_count"] = graph.get("direct_relation_count", 0) + 1
        relation_types = graph.setdefault("relation_type_counts", {})
        relation_types[predicate] = relation_types.get(predicate, 0) + 1
        for entity_id in {subject_id, object_id}:
            entity = stock_by_entity_id.get(entity_id)
            if entity is not None:
                properties = entity.properties_json or {}
                symbol = str(properties.get("symbol") or entity.entity_key.rsplit(":", 1)[-1])
                graph["symbols"].add(symbol)
                if graph_id not in usable_graph_ids:
                    continue
                stock_relation_counts[symbol] = stock_relation_counts.get(symbol, 0) + 1
                types = stock_relation_types.setdefault(symbol, {})
                types[predicate] = types.get(predicate, 0) + 1
    source_names = {
        "stock_news": "新闻文档", "stock_notice": "上市公司公告/法定披露文档",
        "stock_context_event": "政策、产业链等外部事件线索（非公司公告）",
        "stock_financial_report": "财务报告", "stock_kline": "历史量价", "stock_realtime_quote": "实时行情",
        "stock_f10_cache": "F10资料", "research_report": "研究报告",
    }
    source_explanations = {
        "stock_notice": "上市公司自身的法定披露，包含公告及依法披露的财务报告。",
        "stock_context_event": "公司之外的政策、原材料、产业链等外部线索，不是上市公司公告。",
    }
    kb_rows = [{"id": row.id, "name": row.kb_name, "description": row.description,
                "source_tables": row.source_tables or [], "source_names": [source_names.get(source, source) for source in row.source_tables or []],
                "source_explanations": [source_explanations[source] for source in row.source_tables or [] if source in source_explanations],
                "document_count": kb_stats.get(row.id, {}).get("document_count", 0),
                "covered_stocks": len(kb_stats.get(row.id, {}).get("symbols", set())),
                "source_counts": kb_stats.get(row.id, {}).get("source_counts", {}),
                "effective_document_count": kb_stats.get(row.id, {}).get("effective_document_count", 0)} for row in bases]
    graph_rows = [{"id": row.id, "knowledge_base_id": row.knowledge_base_id, "name": row.graph_name,
                   "description": row.description, "status": row.governance_status, "version": row.version,
                   "entity_count": row.entity_count, "relation_count": row.relation_count,
                   "document_count": graph_stats.get(row.id, {}).get("document_count", 0),
                   "covered_stocks": len(graph_stats.get(row.id, {}).get("symbols", set())),
                   "direct_relation_count": graph_stats.get(row.id, {}).get("direct_relation_count", 0),
                   "relation_type_counts": graph_stats.get(row.id, {}).get("relation_type_counts", {}),
                   "source_counts": graph_stats.get(row.id, {}).get("source_counts", {}),
                   "analysis_eligible": row.governance_status in {"GOVERNED", "LOCKED"}} for row in graphs]
    blockers = []
    if not selected_bases:
        blockers.append("至少选择一个启用的知识库")
    if not selected_graphs:
        blockers.append("至少选择一张图谱")
    if selected_graphs and not usable_graphs:
        blockers.append("所选图谱均未治理完成；请选一张已治理图谱，待治理图谱不能作为模型依据")
    stock_rows = []
    for symbol in payload.symbols:
        knowledge_sources = base_symbols.get(symbol, {})
        graph_sources = symbols.get(symbol, {})
        knowledge_document_count = sum(knowledge_sources.values())
        graph_document_count = sum(graph_sources.values())
        direct_relation_count = stock_relation_counts.get(symbol, 0)
        if knowledge_document_count and direct_relation_count:
            coverage_label = "文档与直接关系均有"
        elif knowledge_document_count:
            coverage_label = "有文档，暂无直接关系"
        elif direct_relation_count:
            coverage_label = "有直接关系，暂无文档"
        else:
            coverage_label = "暂无关联资料"
        source_diversity = len({source for source, count in knowledge_sources.items() if count > 0})
        coverage_level = (
            "RICH" if knowledge_document_count >= 3 and source_diversity >= 2 and direct_relation_count >= 1
            else "SOME" if knowledge_document_count or direct_relation_count
            else "NONE"
        )
        stock_rows.append({
            "symbol": symbol,
            "source_counts": graph_sources,
            "knowledge_document_count": knowledge_document_count,
            "knowledge_source_counts": knowledge_sources,
            "graph_document_count": graph_document_count,
            "graph_source_counts": graph_sources,
            "document_count": knowledge_document_count,
            "direct_relation_count": direct_relation_count,
            "relation_type_counts": stock_relation_types.get(symbol, {}),
            "coverage_level": coverage_level,
            "coverage_label": coverage_label,
            "status": "AVAILABLE" if knowledge_document_count or graph_document_count or direct_relation_count else "MISSING",
        })
    warnings = []
    if not totals:
        warnings.append("所选范围没有图谱投影文档；仍可按已选证券的结构化观测运行，文档类结论将标为缺失。")
    if not totals.get("stock_news"):
        warnings.append("本次图谱范围没有新闻文档；文档数量也不代表正文已完整获取。")
    if not totals.get("stock_notice"):
        warnings.append("本次图谱范围没有公司公告文档。")
    if not any(stock_relation_counts.values()):
        warnings.append("本次所选股票没有直接图谱关系；图谱不会为本次分析提供实体连接线索。")
    if any(row not in usable_graphs for row in selected_graphs) and usable_graphs:
        warnings.append("混选了待治理图谱，模型只读取已治理图谱。")
    return {"market": payload.market.upper(), "source_counts": totals,
            "analysis_period": {"months": payload.period_months, "start": period_start.isoformat(), "end": period_end.isoformat()},
            "covered_stocks": len(symbols), "requested_stocks": len(payload.symbols),
            "rows": stock_rows,
            "knowledge_bases": kb_rows, "graphs": graph_rows,
            "ready": bool(selected_bases and usable_graphs), "blockers": blockers,
            "warnings": warnings,
            "source_labels": source_names,
            "scope_note": "覆盖层级只按本次选择范围内的文档数量、来源种类和直接关系数量计算，不是语义相关度、来源可靠度或投资价值评分。模型只引用本次证据；图谱关系仅提供实体连接线索，不直接证明因果。上市公司公告是公司法定披露；外部事件是政策、原材料、产业链等外部线索，不是公告。"}


@router.post("/preflight")
def selection_preflight(payload: SelectionPreflightRequest, db: Session = Depends(get_db)) -> dict:
    """Check evidence coverage and model connectivity before creating a run.

    No SelectionRun or ScheduledJob is created here.  The model probe is a
    small auditable connectivity call; all stock evidence checks are read-only.
    """
    scope = scope_preview(SelectionScopePreview.model_validate(payload.model_dump()), db)
    from app.services.graph_rag import selection_period_bounds
    from app.services.model_hub import ModelHubService

    model_info: dict[str, object] = {"status": "SKIPPED", "instance_code": payload.model_instance_code}
    model_ready = not payload.use_model
    if payload.use_model:
        requested_code = payload.model_instance_code
        try:
            if not requested_code:
                options = ModelHubService(db).list_route_options("stock_screening")
                if not options:
                    raise ValueError("当前没有可用的选股模型实例")
                requested_code = options[0].instance.instance_code
            probe = probe_selection_model(db, requested_code, data_mode=payload.data_mode)
            model_info = {**probe, "status": "READY", "instance_code": requested_code}
            model_ready = True
        except Exception as exc:
            detail = classify_model_error(exc)
            model_info = {"status": "FAILED", "instance_code": requested_code, **detail}
            model_ready = False

    period_start, period_end = selection_period_bounds(datetime.now(timezone.utc), payload.period_months, payload.market)
    data_rows: list[dict[str, object]] = []
    for symbol in payload.symbols:
        try:
            context = build_selection_context(
                db, payload.market, symbol,
                knowledge_base_ids=payload.knowledge_base_ids,
                graph_ids=payload.graph_ids,
                as_of=datetime.now(timezone.utc),
                period_start=period_start, period_end=period_end,
                max_documents=8, max_relations=8, max_facts=8, max_chunks=8,
            )
            counts = context.get("counts") or {}
            missing = list(context.get("missing_data") or [])
            data_rows.append({
                "symbol": symbol,
                "document_count": int(counts.get("documents") or 0),
                "fact_count": int(counts.get("facts") or 0),
                "relation_count": int(counts.get("graph_paths") or 0),
                "chunk_count": int(counts.get("chunks") or 0),
                "source_counts": counts.get("period_source_counts") or {},
                "missing": missing,
                "status": "READY" if not missing else "PARTIAL",
            })
        except Exception as exc:
            data_rows.append({"symbol": symbol, "status": "FAILED", "missing": [f"数据检查失败：{str(exc)[:180]}"]})
    missing_symbols = [row["symbol"] for row in data_rows if row.get("status") != "READY"]
    data_complete = bool(data_rows) and not missing_symbols
    blockers: list[str] = []
    if payload.use_model and not model_ready:
        blockers.append("当前模型不可用，请更换模型后再开始分析")
    if not scope.get("ready"):
        blockers.extend(scope.get("blockers") or [])
    warnings: list[str] = list(scope.get("warnings") or [])
    if missing_symbols:
        warnings.append(f"{len(missing_symbols)} 只股票存在数据缺口；可先更新数据，或确认以部分数据继续分析")
    return {
        "ready": bool(model_ready and scope.get("ready") and data_complete),
        "model_ready": model_ready,
        "data_ready": data_complete,
        "model": model_info,
        "data": {"period_months": payload.period_months, "start": period_start.isoformat(),
                  "end": period_end.isoformat(), "rows": data_rows,
                  "missing_symbols": missing_symbols, "requested_count": len(payload.symbols)},
        "blockers": blockers,
        "warnings": warnings,
        "scope": scope,
    }


@router.get("/runs", response_model=list[SelectionRunRead])
def list_runs(limit: int = 50, db: Session = Depends(get_db)) -> list[SelectionRunRead]:
    if not 1 <= limit <= 200:
        raise HTTPException(status_code=422, detail="limit must be between 1 and 200")
    return [_run_read(db, item, include_candidates=False) for item in db.scalars(
        select(SelectionRun).order_by(SelectionRun.created_at.desc()).limit(limit)
    ).all()]


@router.get("/runs/{run_id}", response_model=SelectionRunRead)
def get_run(run_id: int, db: Session = Depends(get_db)) -> SelectionRunRead:
    run = db.get(SelectionRun, run_id)
    if run is None:
        raise HTTPException(status_code=404, detail="selection run not found")
    return _run_read(db, run)


@router.get("/runs/{run_id}/backfill-status")
def backfill_status(run_id: int, db: Session = Depends(get_db)) -> dict:
    run = db.get(SelectionRun, run_id)
    if run is None:
        raise HTTPException(status_code=404, detail="选股任务不存在")
    return {"selection_run_id": run.id, "status": (run.criteria_json or {}).get("backfill_progress", {}).get("status", "NOT_RUN"),
            "progress": (run.criteria_json or {}).get("backfill_progress", {})}


@router.post("/runs/{run_id}/retry-failed", response_model=SelectionRunRead, status_code=202)
def retry_failed(run_id: int, db: Session = Depends(get_db)) -> SelectionRunRead:
    try:
        return _run_read(db, queue_failed_selection_reanalysis(db, run_id))
    except LookupError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=409, detail=str(exc)) from exc


@router.post("/runs/{run_id}/retry-failed-with-model", response_model=SelectionRunRead, status_code=202)
def retry_failed_with_model(
    run_id: int,
    payload: SelectionModelSwitchRequest,
    db: Session = Depends(get_db),
) -> SelectionRunRead:
    """Probe a user-selected model, then queue an isolated retry batch."""
    original = db.get(SelectionRun, run_id)
    if original is None:
        raise HTTPException(status_code=404, detail="选股任务不存在")
    if original.model_instance_code == payload.model_instance_code:
        raise HTTPException(status_code=422, detail="请选择与原模型不同的模型实例")
    try:
        probe = probe_selection_model(db, payload.model_instance_code, data_mode=original.data_mode)
        run = queue_failed_selection_reanalysis_with_model(
            db, run_id, payload.model_instance_code, payload.candidate_ids,
        )
        criteria = dict(run.criteria_json or {})
        criteria["model_switch"] = {
            **(criteria.get("model_switch") or {}),
            "preflight": probe,
            "status": "QUEUED",
        }
        run.criteria_json = criteria
        db.commit()
        return _run_read(db, run)
    except LookupError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except ValueError as exc:
        # A failed probe is intentionally returned before a job is created.
        db.rollback()
        raise HTTPException(status_code=422, detail=str(exc)) from exc


@router.post("/runs/{run_id}/backfill", response_model=SelectionRunRead, status_code=202)
def backfill_run(run_id: int, payload: SelectionBackfillRequest, db: Session = Depends(get_db)) -> SelectionRunRead:
    try:
        return _run_read(db, queue_selection_data_backfill(db, run_id, payload))
    except LookupError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=409, detail=str(exc)) from exc


@router.post("/candidates/{candidate_id}/review", response_model=SelectionCandidateRead)
def review(candidate_id: int, payload: SelectionReviewRequest, db: Session = Depends(get_db)) -> SelectionCandidateRead:
    try:
        return _candidate_read(db, review_candidate(db, candidate_id, payload))
    except LookupError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=409, detail=str(exc)) from exc


@router.get("/tracking", response_model=list[SelectionTrackingRead])
def list_tracking(status_filter: str | None = None, limit: int = 200, db: Session = Depends(get_db)) -> list[SelectionTrackingRead]:
    if not 1 <= limit <= 500:
        raise HTTPException(status_code=422, detail="limit must be between 1 and 500")
    query = select(SelectionTracking).order_by(SelectionTracking.started_at.desc()).limit(limit)
    if status_filter:
        query = query.where(SelectionTracking.status == status_filter)
    return [_tracking_read(db, item) for item in db.scalars(query).all()]


@router.get("/tracking/{tracking_id}", response_model=SelectionTrackingRead)
def get_tracking(tracking_id: int, db: Session = Depends(get_db)) -> SelectionTrackingRead:
    row = db.get(SelectionTracking, tracking_id)
    if row is None:
        raise HTTPException(status_code=404, detail="selection tracking not found")
    return _tracking_read(db, row)


@router.get("/candidates/{candidate_id}/audit")
def candidate_audit(candidate_id: int, db: Session = Depends(get_db)) -> dict:
    candidate = db.get(SelectionCandidate, candidate_id)
    if candidate is None:
        raise HTTPException(status_code=404, detail="selection candidate not found")
    snapshots = list(db.scalars(select(SelectionDecisionSnapshot).where(
        SelectionDecisionSnapshot.candidate_id == candidate_id,
    ).order_by(SelectionDecisionSnapshot.created_at, SelectionDecisionSnapshot.id)).all())
    snapshot_ids = [row.id for row in snapshots]
    retrospectives = list(db.scalars(select(SelectionRetrospective).where(
        SelectionRetrospective.snapshot_id.in_(snapshot_ids)
    ).order_by(SelectionRetrospective.created_at, SelectionRetrospective.id)).all()) if snapshot_ids else []
    return {"candidate_id": candidate_id, "snapshots": [snapshot_read(row) for row in snapshots],
            "retrospectives": [retrospective_read(row) for row in retrospectives]}


@router.get("/candidates/{candidate_id}/documents/{document_id}")
def analysis_document(candidate_id: int, document_id: int, db: Session = Depends(get_db)) -> dict:
    candidate = db.get(SelectionCandidate, candidate_id)
    if candidate is None:
        raise HTTPException(status_code=404, detail="选股候选不存在")
    evidence = candidate.evidence_json or {}
    doc = next((row for row in [
        *(evidence.get("documents") or []),
        *(evidence.get("related_security_documents") or []),
        *((evidence.get("evidence_context") or {}).get("documents") or []),
        *((evidence.get("evidence_context") or {}).get("related_security_documents") or []),
    ]
        if str(row.get("id")) == str(document_id)), None)
    if doc is None:
        # Graph/chunk context can cite a document without including it in the
        # ranked document list. Serve only the persisted excerpt, never a fresh
        # database document that could differ from the model's evidence.
        paths = [row for row in evidence.get("graph_paths") or []
                 if str(row.get("evidence_document_id")) == str(document_id)]
        chunks = [row for row in evidence.get("chunks") or []
                  if str(row.get("document_id")) == str(document_id)]
        excerpts = list(dict.fromkeys(str(row.get("evidence_excerpt") or "") for row in paths))
        excerpts.extend(str(row.get("text") or row.get("content") or "") for row in chunks)
        content = "\n\n".join(dict.fromkeys(text for text in excerpts if text.strip()))
        if not content:
            raise HTTPException(status_code=404, detail="文档不在本次候选证据范围内，或本轮未保存可读内容")
        return {"id": document_id, "document_id": document_id,
                "title": f"图谱关系证据 #{document_id}" if paths else f"文档切片证据 #{document_id}",
                "content": content, "content_truncated": True,
                "content_state": "GRAPH_REFERENCE_ONLY" if paths else "CHUNK_ONLY",
                "relation_ids": [row["id"] for row in paths if row.get("id")],
                "chunk_ids": [row["id"] for row in chunks if row.get("id")],
                "metadata_json": {}, "snapshot": True}
    original = (evidence.get("analysis_originals") or {}).get(str(document_id))
    return {**doc, **(original or {}), "document_id": document_id,
            "content": (original or {}).get("content") or doc.get("content") or doc.get("excerpt") or "",
            "metadata_json": {**(doc.get("metadata") or {}), "url": doc.get("url"), "published_at": doc.get("published_at")},
            "snapshot": True}


@router.get("/tracking/{tracking_id}/retrospectives")
def tracking_retrospectives(tracking_id: int, db: Session = Depends(get_db)) -> list[dict]:
    tracking = db.get(SelectionTracking, tracking_id)
    if tracking is None:
        raise HTTPException(status_code=404, detail="selection tracking not found")
    rows = db.scalars(select(SelectionRetrospective).where(
        SelectionRetrospective.tracking_id == tracking_id,
    ).order_by(SelectionRetrospective.revision)).all()
    return [retrospective_read(row) for row in rows]


@router.get("/context/{market}/{symbol}")
def selection_context(market: str, symbol: str, knowledge_base_ids: list[int] = Query(default=[]),
                      graph_ids: list[int] = Query(default=[]), db: Session = Depends(get_db)) -> dict:
    """Expose the same bounded context contract used by the LLA prompt."""
    return build_selection_context(db, market, symbol, knowledge_base_ids=knowledge_base_ids, graph_ids=graph_ids)


@router.post("/refresh")
def refresh(payload: SelectionRefreshRequest | None = None, db: Session = Depends(get_db)) -> dict:
    return refresh_tracking(db, payload.tracking_ids if payload else None)
