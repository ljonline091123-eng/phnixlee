from __future__ import annotations

import json
from collections.abc import Iterator

from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import StreamingResponse
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.agents.workflow import ResearchWorkflow
from app.db.session import get_db
from app.models.ai_hub import ResearchReportRecord
from app.schemas.research import ResearchAnalyzeRequest, ResearchReportDetail, ResearchReportSummary

router = APIRouter(prefix="/research", tags=["AI Research"])


def _sse(event: str, data: dict) -> str:
    return f"event: {event}\ndata: {json.dumps(data, ensure_ascii=False, default=str)}\n\n"


@router.post("/analyze")
def analyze_stock(payload: ResearchAnalyzeRequest, db: Session = Depends(get_db)) -> StreamingResponse:
    """Stream agent stages, score snapshots and the generated Markdown report."""

    def stream() -> Iterator[str]:
        workflow = ResearchWorkflow(db)
        for item in workflow.stream(
            payload.symbol,
            payload.market,
            payload.top_k,
            payload.refresh,
            data_source_codes=payload.data_source_codes,
            knowledge_base_ids=payload.knowledge_base_ids,
        ):
            yield _sse(str(item.get("event") or "message"), item.get("data") or {})

    return StreamingResponse(
        stream(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )


def _summary(row: ResearchReportRecord) -> ResearchReportSummary:
    return ResearchReportSummary(
        id=row.id,
        symbol=row.symbol,
        market=row.market,
        name=row.name,
        title=row.title,
        rating=row.rating,
        score=row.score,
        conclusion=row.conclusion,
        model_provider=row.model_provider,
        model_instance=row.model_instance,
        created_at=row.created_at,
        updated_at=row.updated_at,
    )


@router.get("/reports", response_model=list[ResearchReportSummary])
def list_reports(
    symbol: str | None = None,
    market: str | None = None,
    limit: int = 100,
    db: Session = Depends(get_db),
) -> list[ResearchReportSummary]:
    if limit < 1 or limit > 500:
        raise HTTPException(status_code=422, detail="limit must be between 1 and 500")
    statement = select(ResearchReportRecord).order_by(ResearchReportRecord.created_at.desc()).limit(limit)
    if symbol:
        statement = statement.where(ResearchReportRecord.symbol == symbol.strip())
    if market:
        statement = statement.where(ResearchReportRecord.market == market.strip())
    return [_summary(item) for item in db.scalars(statement).all()]


@router.get("/reports/{report_id}", response_model=ResearchReportDetail)
def get_report(report_id: int, db: Session = Depends(get_db)) -> ResearchReportDetail:
    row = db.get(ResearchReportRecord, report_id)
    if row is None:
        raise HTTPException(status_code=404, detail="Research report not found")
    return ResearchReportDetail(
        id=row.id,
        symbol=row.symbol,
        market=row.market,
        name=row.name,
        title=row.title,
        rating=row.rating,
        score=row.score,
        conclusion=row.conclusion,
        model_provider=row.model_provider,
        model_instance=row.model_instance,
        created_at=row.created_at,
        updated_at=row.updated_at,
        report_markdown=row.report_markdown,
        data_sources_json=row.data_sources_json,
        knowledge_base_ids_json=row.knowledge_base_ids_json,
        agent_snapshot_json=row.agent_snapshot_json,
        history_evaluation_json=row.history_evaluation_json,
        warnings_json=row.warnings_json,
    )
