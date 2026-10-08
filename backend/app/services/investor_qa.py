"""Durable, page-checkpointed Q&A sync using the existing research store/queue."""

from __future__ import annotations

from datetime import datetime, timedelta, timezone
from hashlib import sha256
from typing import Any
from uuid import uuid4

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.connectors.investor_qa import OfficialQAClient, qa_source
from app.db.session import SessionLocal
from app.jobs.dispatcher import DatabaseJobDispatcher
from app.jobs.errors import DurableRetryError
from app.models.market_data import DataFetchLog, DataInterface, DataSource, StockF10Cache
from app.models.pipeline import ScheduledJob
from app.models.research_data import StockInvestorQA
from app.services.research_store import persist_research_sections
from app.services.catalog import select_data_source


TASK_TYPE = "investor_qa_sync"
SYNC_VERSION = 2
ACTIVE_STATUSES = ("PENDING", "RETRY", "RUNNING")


def ensure_qa_sync(db: Session, market: str, symbol: str, *, force: bool = False) -> dict[str, Any]:
    if market != "CN_A":
        return {"status": "UNSUPPORTED"}
    source = select_data_source(db, market, "QA", fallback_code="AKSHARE")
    if source is None:
        return {"status": "MISSING", "message": "未配置问答数据源"}
    return enqueue_qa_sync(db, market, symbol, source.id, force=force)


def _cache(db: Session, market: str, symbol: str) -> StockF10Cache | None:
    return db.scalar(select(StockF10Cache).where(
        StockF10Cache.market == market, StockF10Cache.symbol == symbol,
        StockF10Cache.section == "research_sections",
    ).execution_options(populate_existing=True))


def _state(db: Session, market: str, symbol: str) -> dict[str, Any]:
    cache = _cache(db, market, symbol)
    return dict((cache.payload_json or {}).get("qa_sync") or {}) if cache else {}


def _save_state(db: Session, market: str, symbol: str, source_id: int, state: dict[str, Any]) -> None:
    cache = _cache(db, market, symbol)
    if cache is None:
        cache = StockF10Cache(market=market, symbol=symbol, section="research_sections", source_id=source_id, payload_json={})
        db.add(cache)
    # Assign a fresh JSON object, preserving all unrelated research sections.
    cache.payload_json = {
        **(cache.payload_json or {}), "qa_sync": state,
        "qa_source": state["source_name"], "qa_message": state.get("message") or "",
    }


def _job_prefix(market: str, symbol: str) -> str:
    return f"{TASK_TYPE}:{market}:{symbol}:"


def _active_job(db: Session, market: str, symbol: str) -> ScheduledJob | None:
    return db.scalar(select(ScheduledJob).where(
        ScheduledJob.task_type == TASK_TYPE,
        ScheduledJob.idempotency_key.startswith(_job_prefix(market, symbol)),
        ScheduledJob.status.in_(ACTIVE_STATUSES),
    ).order_by(ScheduledJob.id.desc()))


def qa_sync_status(db: Session, market: str, symbol: str) -> dict[str, Any]:
    state = _state(db, market, symbol)
    job = _active_job(db, market, symbol)
    count = db.scalar(select(func.count()).select_from(StockInvestorQA).where(
        StockInvestorQA.market == market, StockInvestorQA.symbol == symbol,
    )) or 0
    return {
        "status": "MISSING", **state, "market": market, "symbol": symbol, "stored_count": count,
        "job_id": job.id if job else None, "job_status": job.status if job else None,
        "attempts": job.attempts if job else None,
        "next_retry_at": job.run_after.isoformat() if job and job.status == "RETRY" else None,
        "error": job.error_message if job else state.get("error"),
    }


def read_qa_section(db: Session, market: str, symbol: str) -> dict[str, Any]:
    sync = qa_sync_status(db, market, symbol)
    records = db.scalars(select(StockInvestorQA).where(
        StockInvestorQA.market == market, StockInvestorQA.symbol == symbol,
    ).order_by(StockInvestorQA.updated_time.desc(), StockInvestorQA.id.desc())).all()
    rows = [{
        "question_id": row.external_id, "source_code": row.source_code,
        "question": row.question, "answer": row.answer,
        "questioner": row.questioner, "answerer": row.answerer,
        "asked_at": row.asked_time, "answered_at": row.answered_time,
        "updated_at": row.updated_time, "source_url": row.source_url,
        "source_name": (row.raw_payload or {}).get("source_name"),
        "content_hash": (row.raw_payload or {}).get("content_hash"),
    } for row in records]
    return {
        "key": "qa", "title": "问董秘", "rows": rows, "sync": sync,
        "source": sync.get("source_name") or qa_source(symbol)[1],
        "as_of": sync.get("updated_at"), "message": sync.get("message") or "",
        "status": "AVAILABLE" if rows and sync["status"] == "COMPLETE" else "PARTIAL" if rows else "UNAVAILABLE" if sync["status"] == "EMPTY" else "PENDING",
    }


def enqueue_qa_sync(db: Session, market: str, symbol: str, source_id: int, *, force: bool = False) -> dict[str, Any]:
    if market != "CN_A":
        return {"status": "UNSUPPORTED", "message": "当前官方问答接口覆盖沪深 A 股"}
    active = _active_job(db, market, symbol)
    if active:
        return qa_sync_status(db, market, symbol)
    source = db.get(DataSource, source_id)
    interface_code = "CN_A_SSE_QA_ON_DEMAND" if symbol.startswith("6") else "CN_A_IRM_QA_ON_DEMAND"
    interface = db.scalar(select(DataInterface).where(DataInterface.source_id == source_id, DataInterface.interface_code == interface_code))
    if source is None or not source.enabled or (interface is not None and not interface.enabled):
        return {"status": "DISABLED", "message": "问答数据源或接口已停用，保留已有本地数据"}
    state = _state(db, market, symbol)
    now = datetime.now(timezone.utc)
    if not force and state.get("version") == SYNC_VERSION and state.get("completed_at"):
        completed = datetime.fromisoformat(state["completed_at"])
        if completed > now - timedelta(hours=6):
            return qa_sync_status(db, market, symbol)
    # Recover a checkpoint even if the process stopped before submitting its
    # continuation job. A completed historical pass starts again at page one.
    if state.get("status") not in ACTIVE_STATUSES + ("PARTIAL", "DISABLED") or not state.get("run_id"):
        code, name = qa_source(symbol)
        state = {
            "version": SYNC_VERSION, "run_id": uuid4().hex, "status": "PENDING",
            "source_code": code, "source_name": name, "cursor": {"page": 1},
            "pages_saved": 0, "records_seen": 0, "page_signatures": [],
            "started_at": now.isoformat(), "message": "官方问答已排队，后台分页采集后自动保存",
        }
    elif state.get("status") == "DISABLED":
        state.update(run_id=uuid4().hex, status="PENDING", message="问答接口已恢复，后台将从已保存断点继续采集")
    _save_state(db, market, symbol, source_id, state)
    db.commit()
    _enqueue_page(db, market, symbol, source_id, state)
    return qa_sync_status(db, market, symbol)


def _enqueue_page(db: Session, market: str, symbol: str, source_id: int, state: dict[str, Any]) -> ScheduledJob:
    key = f"{_job_prefix(market, symbol)}{state['run_id']}:{state['cursor'].get('feed_type', 11)}:{state['cursor'].get('page', 1)}"
    existing = db.scalar(select(ScheduledJob).where(ScheduledJob.idempotency_key == key))
    if existing and existing.status in {"FAILED", "COMPLETED"}:
        # A stopped interface or exhausted infrastructure failure can leave a
        # terminal job with an unfinished checkpoint. Keep its audit record
        # and submit a fresh recovery job instead of returning a dead job.
        key += f":recovery:{uuid4().hex}"
    return DatabaseJobDispatcher(db).enqueue(
        task_type=TASK_TYPE,
        idempotency_key=key,
        payload={"market": market, "symbol": symbol, "source_id": source_id, "run_id": state["run_id"]},
        max_attempts=3, trigger_type="ON_DEMAND", pipeline_type="INVESTOR_QA_SYNC",
    )


def execute_qa_sync(payload: dict[str, Any]) -> dict[str, Any]:
    with SessionLocal() as db:
        return sync_qa_pages(db, payload)


def sync_qa_pages(db: Session, payload: dict[str, Any], *, page_budget: int = 5) -> dict[str, Any]:
    market, symbol, source_id = payload["market"], payload["symbol"], int(payload["source_id"])
    state = _state(db, market, symbol)
    if state.get("run_id") != payload["run_id"] or state.get("status") in {"COMPLETE", "EMPTY"}:
        return qa_sync_status(db, market, symbol)
    source = db.get(DataSource, source_id)
    interface_code = "CN_A_SSE_QA_ON_DEMAND" if symbol.startswith("6") else "CN_A_IRM_QA_ON_DEMAND"
    interface = db.scalar(select(DataInterface).where(DataInterface.source_id == source_id, DataInterface.interface_code == interface_code))
    if not source or not source.enabled or (interface and not interface.enabled):
        state.update(status="DISABLED", message="问答数据源或接口已停用，保留采集断点和已有数据")
        _save_state(db, market, symbol, source_id, state)
        db.commit()
        return state
    log = DataFetchLog(source_id=source_id, market=market, symbol=symbol,
                       interface_code=interface_code, request_json={"run_id": state["run_id"], "cursor": state["cursor"]})
    db.add(log)
    db.commit()
    log_id = log.id
    try:
        with OfficialQAClient() as client:
            for _ in range(page_budget):
                # Release the read transaction before touching the network.
                cursor = dict(state["cursor"])
                db.rollback()
                page = client.fetch_page(symbol, cursor)
                db.expire_all()
                signature = sha256((str(cursor.get("feed_type", 11)) + "|" + "|".join(row["question_id"] for row in page.rows)).encode()).hexdigest()
                if page.rows and signature in state.get("page_signatures", []):
                    raise RuntimeError("官方返回重复分页，已保留断点，未将历史采集标记为完成")
                stats = persist_research_sections(db, market, symbol, {"qa": page.rows})
                state.update({
                    "cursor": page.cursor, "status": "PARTIAL",
                    "pages_saved": state.get("pages_saved", 0) + 1,
                    "records_seen": state.get("records_seen", 0) + len(page.rows),
                    "updated_at": datetime.now(timezone.utc).isoformat(),
                    "last_response_hash": page.response_hash, "error": None,
                    "message": "已保存部分官方问答，正在继续采集历史分页",
                })
                if page.rows:
                    state["page_signatures"] = [*state.get("page_signatures", []), signature]
                if page.complete:
                    state.update(status="COMPLETE" if state["records_seen"] else "EMPTY",
                                 completed_at=datetime.now(timezone.utc).isoformat(),
                                 message="" if state["records_seen"] else "官方问答来源当前未返回记录；保留已有本地历史问答")
                _save_state(db, market, symbol, source_id, state)
                log = db.get(DataFetchLog, log_id)
                log.total_count += len(page.rows)
                log.persisted_count += stats["qa"]
                log.request_json = {**log.request_json, "cursor": page.cursor, "response_hash": page.response_hash}
                # Rows and cursor commit together: replay after a crash is safe.
                db.commit()
                if page.complete:
                    break
        if state["status"] == "PARTIAL":
            _enqueue_page(db, market, symbol, source_id, state)
        log = db.get(DataFetchLog, log_id)
        log.status = "SUCCESS" if state["status"] in {"COMPLETE", "EMPTY"} else "PARTIAL"
        log.completed_at = datetime.now(timezone.utc)
        db.commit()
        return state
    except Exception as exc:
        db.rollback()
        state = _state(db, market, symbol)  # Last committed cursor, never the failing page.
        state.update(status="RETRY", error=str(exc)[:1500],
                     message="官方问答采集暂未完成；已保存数据可查看，后台将自动退避重试并从断点续采")
        _save_state(db, market, symbol, source_id, state)
        log = db.get(DataFetchLog, log_id)
        log.status, log.error_message = "PARTIAL", str(exc)[:3000]
        log.completed_at = datetime.now(timezone.utc)
        db.commit()
        raise DurableRetryError(str(exc)) from exc
