from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.selection import SelectionCandidate, SelectionRun, SelectionSnapshot, SelectionTracking
from app.models.decision_review import SelectionDecisionSnapshot, SelectionRetrospective
from app.schemas.selection import (
    SelectionCandidateRead,
    SelectionRefreshRequest,
    SelectionReviewRequest,
    SelectionRunCreate,
    SelectionRunRead,
    SelectionSnapshotRead,
    SelectionTrackingRead,
)
from app.services.selection import create_selection_run, queue_selection_run, refresh_tracking, review_candidate
from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, KnowledgeGraph
from app.schemas.selection import SelectionScopePreview
from app.services.graph_rag import build_selection_context
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
    payload = {key: getattr(run, key) for key in (
        "id", "market", "universe_scope", "candidate_limit", "criteria_json", "model_instance_code",
        "knowledge_base_ids_json", "graph_ids_json", "data_mode", "analysis_mode", "status", "candidate_count",
        "reviewed_count", "tracking_count", "missing_data_json", "error_message", "as_of", "completed_at",
        "created_at", "updated_at",
    )}
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
    query = select(KnowledgeDocument.symbol, KnowledgeDocument.source_table, func.count(KnowledgeDocument.id)).where(
        KnowledgeDocument.market == payload.market.upper(),
        KnowledgeDocument.knowledge_base_id.in_(payload.knowledge_base_ids),
        KnowledgeDocument.graph_id.in_(payload.graph_ids),
    )
    if payload.symbols:
        query = query.where(KnowledgeDocument.symbol.in_(payload.symbols))
    counts = db.execute(query.group_by(KnowledgeDocument.symbol, KnowledgeDocument.source_table)).all()
    symbols, totals = {}, {}
    for symbol, source, count in counts:
        symbols.setdefault(symbol, {})[source] = count
        totals[source] = totals.get(source, 0) + count
    return {"market": payload.market.upper(), "source_counts": totals,
            "covered_stocks": len(symbols), "requested_stocks": len(payload.symbols),
            "rows": [{"symbol": symbol, "source_counts": symbols.get(symbol, {}),
                      "status": "AVAILABLE" if symbols.get(symbol) else "MISSING"}
                     for symbol in payload.symbols],
            "ready": bool(bases and graphs and counts and all(row.governance_status in {"GOVERNED", "LOCKED"} for row in graphs)),
            "scope_note": "仅读取所选知识库与图谱的交集；记录数不代表正文完整或语义核验通过。实际模型引用在分析结果中逐项显示。"}


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
    doc = next((row for row in [*(evidence.get("documents") or []),
        *((evidence.get("evidence_context") or {}).get("documents") or [])] if row.get("id") == document_id), None)
    if doc is None:
        raise HTTPException(status_code=404, detail="文档不在本次候选证据范围内")
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
