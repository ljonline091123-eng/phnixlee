"""Dedicated news/notice semantic workbench over existing audited job/fact stores."""
from __future__ import annotations

from typing import Literal

from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel, Field
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.ai_hub import ModelSkill
from app.models.foundation import FoundationFact
from app.models.market_data import StockNews, StockNotice, StockSymbol
from app.schemas.stock_batch import DocumentTarget, StockBatchGovernanceRequest
from app.services.document_event_governance import SKILL_CODE, document_body, _done, _search_excerpt
from app.services.foundation import fact_read

router = APIRouter(prefix="/document-governance", tags=["新闻公告语义治理"])
MODELS = {"stock_news": StockNews, "stock_notice": StockNotice}


def current_version(db):
    skill = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == SKILL_CODE))
    return skill.version if skill else "未登记"


def required_document(db, table, identity):
    row = db.get(MODELS[table], identity)
    if not row:
        raise HTTPException(404, "新闻公告不存在")
    return row


def summary(row, version):
    state = (row.content_json or {}).get("event_governance") or {}
    analysis = state.get("semantic_analysis") or {}
    return {"source_table": row.__tablename__, "record_id": row.id, "market": row.market, "symbol": row.symbol,
        "title": row.title, "published_at": getattr(row, "news_time", None) or row.notice_date,
        "source_name": getattr(row, "source_name", None) or f"数据源#{row.source_id}",
        "status": state.get("status", "NOT_PROCESSED"), "error": state.get("error"),
        "body_characters": len(document_body(row)), "body_available": bool(document_body(row)) and not _search_excerpt(row),
        "semantic_complete": _done(row, version, True), "skill_version": state.get("skill_version"),
        "current_skill_version": version, "schema_version": state.get("semantic_schema_version"),
        "event_count": len(analysis.get("events") or []), "relation_count": len(analysis.get("relations") or []),
        "version_count": len((row.content_json or {}).get("event_governance_history") or []) + bool(state)}


@router.get("/documents")
def documents(market: str = "CN_A", symbol: str = "000159", kind: Literal["ALL", "NEWS", "NOTICE"] = "ALL",
              q: str = Query("", max_length=256), limit: int = Query(20, ge=1, le=50), offset: int = Query(0, ge=0),
              db: Session = Depends(get_db)):
    # Read only metadata pages, not 76k full JSON bodies on every navigation.
    choices = {"NEWS": [StockNews], "NOTICE": [StockNotice], "ALL": [StockNews, StockNotice]}[kind]
    rows = []
    from sqlalchemy import func
    total = 0
    for model in choices:
        time_column = model.news_time if model is StockNews else model.notice_date
        query = select(model).where(model.market == market, model.symbol == symbol)
        if q:
            query = query.where(model.title.contains(q, autoescape=True))
        total += db.scalar(select(func.count()).select_from(query.subquery())) or 0
        rows += list(db.scalars(query.order_by(time_column.desc(), model.id.desc()).limit(offset + limit)).all())
    rows.sort(key=lambda row: (str(getattr(row, "news_time", None) or row.notice_date), row.id), reverse=True)
    stock = db.scalar(select(StockSymbol).where(StockSymbol.market == market, StockSymbol.symbol == symbol))
    version = current_version(db)
    return {"items": [summary(row, version) for row in rows[offset:offset + limit]], "total": total,
            "offset": offset, "limit": limit, "stock_name": stock.name if stock else None,
            "current_skill_version": version}


@router.get("/documents/{source_table}/{record_id}")
def detail(source_table: Literal["stock_news", "stock_notice"], record_id: int, version_index: int | None = Query(None, ge=0),
           db: Session = Depends(get_db)):
    row = required_document(db, source_table, record_id)
    payload = row.content_json or {}
    versions = [*(payload.get("event_governance_history") or []), *(([payload["event_governance"]]) if payload.get("event_governance") else [])]
    if version_index is not None and version_index >= len(versions):
        raise HTTPException(404, "治理版本不存在")
    index = len(versions) - 1 if version_index is None else version_index
    state = versions[index] if versions else {}
    ids = state.get("fact_ids") or []
    facts = [db.get(FoundationFact, identity) for identity in ids]
    # The original evidence associated with the selected version takes precedence
    # over the current body to avoid misleading historical comparisons.
    from app.models.foundation import FoundationEvidence
    evidence = db.get(FoundationEvidence, state.get("evidence_id")) if state.get("evidence_id") else None
    return {**summary(row, current_version(db)), "body": evidence.content if evidence else document_body(row),
        "url": row.url, "selected_version": index, "state": state,
        "versions": [{"index": i, **{k: st.get(k) for k in ("skill_version", "semantic_schema_version", "observed_at", "status", "content_scope", "error")}} for i, st in enumerate(versions)],
        "facts": [fact_read(db, fact, full=False) for fact in facts if fact],
        "analysis": state.get("semantic_analysis"), "note": "新闻转述、作者观点及模型推断均为待审核候选；仅核验原文引用，不代表官方交叉核验。"}


class AnalyzeRequest(BaseModel):
    documents: list[DocumentTarget] = Field(min_length=1, max_length=10)
    idempotency_key: str = Field(min_length=1, max_length=128)


@router.post("/analyze", status_code=202)
def analyze(payload: AnalyzeRequest, db: Session = Depends(get_db)):
    from app.api.stock_batch import submit_stock_batch_governance
    rows = [required_document(db, item.source_table, item.record_id) for item in payload.documents]
    stocks = list(dict.fromkeys((row.market, row.symbol) for row in rows))
    request = StockBatchGovernanceRequest(stocks=[{"market": market, "symbol": symbol} for market, symbol in stocks],
        governance_mode="AI_AGENT_SKILL_GOVERNANCE", structure_documents=True, semantic_analysis=True,
        document_targets=payload.documents, structured_document_limit=len(rows), collect_business_data=False,
        export_lakehouse=False, archive_chunks=False, run_graph=False, business_types=[],
        idempotency_key="semantic:" + payload.idempotency_key)
    return submit_stock_batch_governance(request, db)
