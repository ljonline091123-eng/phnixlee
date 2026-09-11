from __future__ import annotations

import json
from collections.abc import Iterator

from fastapi import APIRouter, Depends
from fastapi.responses import StreamingResponse
from sqlalchemy.orm import Session

from app.agents.workflow import ResearchWorkflow
from app.db.session import get_db
from app.schemas.research import ResearchAnalyzeRequest

router = APIRouter(prefix="/research", tags=["AI Research"])


def _sse(event: str, data: dict) -> str:
    return f"event: {event}\ndata: {json.dumps(data, ensure_ascii=False, default=str)}\n\n"


@router.post("/analyze")
def analyze_stock(payload: ResearchAnalyzeRequest, db: Session = Depends(get_db)) -> StreamingResponse:
    """Stream agent stages, score snapshots and the generated Markdown report."""

    def stream() -> Iterator[str]:
        workflow = ResearchWorkflow(db)
        for item in workflow.stream(payload.symbol, payload.market, payload.top_k, payload.refresh):
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
