from __future__ import annotations

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, Query, status
from fastapi.responses import Response
from typing import Literal
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.stock_analyst import AiStockAnalysisRecord
from app.schemas.stock_analyst import (
    AiStockAnalysisCreate, AiStockAnalysisRead, AiStockContextPreview,
)
from app.services.stock_analyst import (
    queue_stock_analysis, preview_stock_analyst_context, run_stock_analysis_background, cancel_stock_analysis,
)
from app.models.stock_analyst import AiAnalystDialogue, AiAnalystReportRevision, AiAnalystWatch, AiAnalystDailyReview, AiAnalystWatchSuggestion
from app.schemas.stock_analyst import AnalystDialogueCreate, AnalystReportConfirm, AnalystWatchConfig, AnalystSuggestionCreate
from app.services import analyst_followup as followup


router = APIRouter(prefix="/ai-stock-analyst", tags=["AI Stock Analyst"])


@router.post("/context-preview", response_model=AiStockContextPreview)
def preview_context(
    payload: AiStockAnalysisCreate,
    db: Session = Depends(get_db),
) -> AiStockContextPreview:
    try:
        return preview_stock_analyst_context(db, payload)
    except LookupError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc


@router.post("/analyses", response_model=AiStockAnalysisRead, status_code=status.HTTP_202_ACCEPTED)
def create_analysis(
    payload: AiStockAnalysisCreate,
    background_tasks: BackgroundTasks,
    db: Session = Depends(get_db),
) -> AiStockAnalysisRecord:
    try:
        record = queue_stock_analysis(db, payload)
        background_tasks.add_task(run_stock_analysis_background, record.id)
        return record
    except LookupError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=409, detail=str(exc)) from exc


@router.get("/analyses", response_model=list[AiStockAnalysisRead])
def list_analyses(
    market: str | None = Query(default=None, max_length=16),
    symbol: str | None = Query(default=None, max_length=32),
    limit: int = Query(default=30, ge=1, le=200),
    db: Session = Depends(get_db),
) -> list[AiStockAnalysisRecord]:
    statement = select(AiStockAnalysisRecord).order_by(
        AiStockAnalysisRecord.created_at.desc(), AiStockAnalysisRecord.id.desc(),
    ).limit(limit)
    if market:
        statement = statement.where(AiStockAnalysisRecord.market == market.strip().upper())
    if symbol:
        statement = statement.where(AiStockAnalysisRecord.symbol == symbol.strip())
    return list(db.scalars(statement).all())


@router.get("/analyses/{analysis_id}", response_model=AiStockAnalysisRead)
def get_analysis(analysis_id: int, db: Session = Depends(get_db)) -> AiStockAnalysisRecord:
    row = db.get(AiStockAnalysisRecord, analysis_id)
    if row is None:
        raise HTTPException(status_code=404, detail="AI证券分析记录不存在")
    return row


@router.get("/analyses/{analysis_id}/evidence")
def get_evidence(
    analysis_id: int, offset: int = Query(default=0, ge=0), limit: int = Query(default=20, ge=1, le=100),
    db: Session = Depends(get_db),
) -> dict:
    record = get_analysis(analysis_id, db)
    rows = record.evidence_snapshot_json.get("evidence", [])
    return {"items": rows[offset:offset + limit], "total": len(rows), "offset": offset, "limit": limit}


@router.post("/analyses/{analysis_id}/cancel", response_model=AiStockAnalysisRead)
def cancel_analysis(analysis_id: int, db: Session = Depends(get_db)) -> AiStockAnalysisRecord:
    return cancel_stock_analysis(db, get_analysis(analysis_id, db))


def _public(row, *, omit=()):
    return {c.name: getattr(row, c.name) for c in row.__table__.columns if c.name not in omit} if row else None


def _action(fn, *args, **kwargs):
    try:
        return fn(*args, **kwargs)
    except LookupError as exc:
        raise HTTPException(404, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(409, detail=str(exc)) from exc


def _dialogue_public(row):
    value = _public(row, omit=("context_snapshot_json",))
    snapshot = row.context_snapshot_json or {}
    value["references"] = snapshot.get("selected_evidence", [])
    value["images"] = [{key: image.get(key) for key in ("name", "evidence_id", "width", "height")}
                       | {"url": f"/api/v1/ai-stock-analyst/analyses/{row.analysis_id}/dialogues/{row.id}/images/{index}"}
                       for index, image in enumerate(snapshot.get("images", []))]
    return value


@router.get("/analyses/{analysis_id}/local-documents")
def list_local_documents(analysis_id: int, category: Literal["ANY", "NEWS", "NOTICE"] = "ANY",
                         keyword: str = Query("", max_length=100), offset: int = Query(0, ge=0),
                         limit: int = Query(12, ge=1, le=50), db: Session = Depends(get_db)):
    from app.services.analyst_inputs import local_documents
    return local_documents(db, get_analysis(analysis_id, db), category, keyword, offset, limit)


@router.get("/analyses/{analysis_id}/followup")
def get_followup(analysis_id: int, db: Session = Depends(get_db)):
    get_analysis(analysis_id, db)
    dialogues = db.scalars(select(AiAnalystDialogue).where(AiAnalystDialogue.analysis_id == analysis_id)
                           .order_by(AiAnalystDialogue.id.desc()).limit(100)).all()
    revisions = db.scalars(select(AiAnalystReportRevision).where(AiAnalystReportRevision.analysis_id == analysis_id)
                           .order_by(AiAnalystReportRevision.version.desc()).limit(50)).all()
    watch = db.scalar(select(AiAnalystWatch).where(AiAnalystWatch.analysis_id == analysis_id))
    daily = db.scalar(select(AiAnalystDailyReview).where(AiAnalystDailyReview.watch_id == watch.id,
        AiAnalystDailyReview.status.in_(("COMPLETED", "PARTIAL")))
        .order_by(AiAnalystDailyReview.business_date.desc(), AiAnalystDailyReview.id.desc()).limit(1)) if watch else None
    from app.jobs.governance_runner import governance_runner_status
    suggestion = db.scalar(select(AiAnalystWatchSuggestion).where(AiAnalystWatchSuggestion.analysis_id == analysis_id)
        .order_by(AiAnalystWatchSuggestion.id.desc()).limit(1))
    return {"dialogues": [_dialogue_public(r) for r in reversed(dialogues)], "revisions": [_public(r) for r in revisions],
            "watch_suggestion": _public(suggestion, omit=("evidence_snapshot_json",)),
            "watch": _public(watch), "latest_review": _public(daily, omit=("evidence_snapshot_json",)),
            "runner": governance_runner_status(), "timezone": "Asia/Shanghai"}


@router.post("/analyses/{analysis_id}/dialogues", status_code=202)
def create_dialogue(analysis_id: int, payload: AnalystDialogueCreate, background_tasks: BackgroundTasks, db: Session = Depends(get_db)):
    row = _action(followup.queue_dialogue, db, analysis_id, payload)
    background_tasks.add_task(followup.run_dialogue_background, row.id)
    return _dialogue_public(row)


@router.get("/analyses/{analysis_id}/dialogues/{dialogue_id}/evidence")
def get_dialogue_evidence(analysis_id: int, dialogue_id: int, db: Session = Depends(get_db)):
    row = db.get(AiAnalystDialogue, dialogue_id)
    if row is None or row.analysis_id != analysis_id:
        raise HTTPException(404, detail="对话记录不存在")
    snapshot = dict(row.context_snapshot_json or {})
    snapshot.pop("images", None)
    review_state = snapshot.pop("full_local_review_state", None)
    if review_state:
        snapshot["full_local_review_coverage"] = review_state.get("coverage") or snapshot.get("full_local_review_coverage")
    return snapshot


@router.get("/analyses/{analysis_id}/dialogues/{dialogue_id}/images/{image_index}")
def get_dialogue_image(analysis_id: int, dialogue_id: int, image_index: int, db: Session = Depends(get_db)):
    row = db.get(AiAnalystDialogue, dialogue_id)
    if row is None or row.analysis_id != analysis_id or not 0 <= image_index < len((row.context_snapshot_json or {}).get("images", [])):
        raise HTTPException(404, detail="对话图片不存在")
    from app.services.analyst_inputs import validate_image
    mime, binary, _, _ = validate_image(row.context_snapshot_json["images"][image_index]["data_url"])
    return Response(binary, media_type=mime, headers={"Cache-Control": "private, max-age=3600", "X-Content-Type-Options": "nosniff"})


@router.post("/analyses/{analysis_id}/watch/suggestions", status_code=202)
def create_watch_suggestion(analysis_id: int, payload: AnalystSuggestionCreate,
                            background_tasks: BackgroundTasks, db: Session = Depends(get_db)):
    row = _action(followup.queue_watch_suggestion, db, analysis_id, payload)
    background_tasks.add_task(followup.run_suggestion_background, row.id)
    return _public(row, omit=("evidence_snapshot_json",))


@router.get("/analyses/{analysis_id}/watch/suggestions/{suggestion_id}/evidence")
def get_suggestion_evidence(analysis_id: int, suggestion_id: int, db: Session = Depends(get_db)):
    row = db.get(AiAnalystWatchSuggestion, suggestion_id)
    if row is None or row.analysis_id != analysis_id:
        raise HTTPException(404, detail="关注建议不存在")
    return row.evidence_snapshot_json


@router.post("/analyses/{analysis_id}/report-confirmations", status_code=201)
def confirm_report(analysis_id: int, payload: AnalystReportConfirm, db: Session = Depends(get_db)):
    return _public(_action(followup.confirm_report, db, analysis_id, payload))


@router.put("/analyses/{analysis_id}/watch")
def configure_watch(analysis_id: int, payload: AnalystWatchConfig, db: Session = Depends(get_db)):
    return _public(_action(followup.save_watch, db, analysis_id, payload))


@router.post("/analyses/{analysis_id}/watch/check", status_code=202)
def check_watch(analysis_id: int, db: Session = Depends(get_db)):
    get_analysis(analysis_id, db)
    watch = db.scalar(select(AiAnalystWatch).where(AiAnalystWatch.analysis_id == analysis_id))
    if watch is None:
        raise HTTPException(409, detail="请先保存关注条件")
    return _public(followup.enqueue_review(db, watch, force=True), omit=("evidence_snapshot_json",))


@router.get("/analyses/{analysis_id}/timeline")
def get_timeline(analysis_id: int, offset: int = Query(0, ge=0), limit: int = Query(15, ge=1, le=100), db: Session = Depends(get_db)):
    get_analysis(analysis_id, db)
    from sqlalchemy import func
    stmt = select(AiAnalystDailyReview).join(AiAnalystWatch).where(AiAnalystWatch.analysis_id == analysis_id)
    total = db.scalar(select(func.count()).select_from(stmt.subquery())) or 0
    rows = db.scalars(stmt.order_by(AiAnalystDailyReview.business_date.desc(), AiAnalystDailyReview.id.desc())
                      .offset(offset).limit(limit)).all()
    return {"items": [_public(r, omit=("evidence_snapshot_json",)) for r in rows], "total": total, "offset": offset, "limit": limit}


@router.get("/analyses/{analysis_id}/tracking")
def get_tracking(analysis_id: int, offset: int = Query(0, ge=0), limit: int = Query(10, ge=1, le=50), db: Session = Depends(get_db)):
    get_analysis(analysis_id, db)
    from app.services.analyst_tracking import tracking_page
    return tracking_page(db, analysis_id, offset, limit)


@router.get("/analyses/{analysis_id}/timeline/{review_id}/evidence")
def get_review_evidence(analysis_id: int, review_id: int, db: Session = Depends(get_db)):
    row = db.get(AiAnalystDailyReview, review_id)
    watch = db.get(AiAnalystWatch, row.watch_id) if row else None
    if watch is None or watch.analysis_id != analysis_id:
        raise HTTPException(404, detail="跟踪记录不存在")
    return row.evidence_snapshot_json
