"""Dedicated news/notice semantic workbench over existing audited job/fact stores."""
from __future__ import annotations

from typing import Literal
import re

from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel, ConfigDict, Field
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.ai_hub import ModelCallLog, ModelSkill
from app.models.decision_review import SkillEvaluationResult, SkillEvaluationRun
from app.models.foundation import FoundationFact
from app.models.market_data import StockNews, StockNotice, StockSymbol
from app.schemas.document_governance import (
    DocumentPlanDecisionRequest, DocumentPlanGenerateRequest, DocumentResultReviewRequest,
    DocumentReviewRequest, DocumentSkillDraftRequest,
)
from app.schemas.stock_batch import DocumentTarget, StockBatchGovernanceRequest, StockBatchTarget
from app.services.document_event_governance import (
    SKILL_CODE, _done, _search_excerpt, _select_pending, document_body, scope_rows,
)
from app.services.document_semantic_review import apply_review, compare_versions, review_status, version_key, versions_for
from app.services.document_semantic_planning import (
    decide_plan, generate_plan, public_type_catalog, result_report, review_result, workflow_status,
    validate_confirmed_plan, WorkflowConflict,
)
from app.services.foundation import fact_read

router = APIRouter(prefix="/document-governance", tags=["新闻公告语义治理"])
MODELS = {"stock_news": StockNews, "stock_notice": StockNotice}


class SkillDraftAction(BaseModel):
    model_config = ConfigDict(extra="forbid")
    reviewer: str = Field(min_length=1, max_length=128)


class SemanticSkillRetryRequest(BaseModel):
    """Request for retrying an immutable specialist Skill acceptance batch."""

    model_config = ConfigDict(extra="forbid")
    reviewer: str = Field(min_length=1, max_length=128)


class FailureDiagnosisRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    reviewer: str = Field(min_length=1, max_length=128)
    force: bool = False


@router.post("/documents/{source_table}/{record_id}/failure-diagnosis")
def diagnose_document_failure(source_table: Literal["stock_news", "stock_notice"], record_id: int,
                              payload: FailureDiagnosisRequest, db: Session = Depends(get_db)):
    from app.services.document_semantic_planning import diagnose_failure
    row = required_document(db, source_table, record_id)
    try:
        return diagnose_failure(db, row, reviewer=payload.reviewer, force=payload.force)
    except (ValueError, OSError) as exc:
        db.rollback()
        raise HTTPException(409 if isinstance(exc, WorkflowConflict) else 422, str(exc)) from exc


@router.post("/documents/{source_table}/{record_id}/skill-draft")
def propose_skill_draft(source_table: Literal["stock_news", "stock_notice"], record_id: int,
                       payload: DocumentSkillDraftRequest, db: Session = Depends(get_db)):
    from app.services.document_semantic_skills import create_draft
    row = required_document(db, source_table, record_id)
    try:
        return create_draft(db, row, **payload.model_dump())
    except ValueError as exc:
        db.rollback()
        raise HTTPException(409 if isinstance(exc, WorkflowConflict) else 422, str(exc)) from exc


@router.get("/documents/{source_table}/{record_id}/skill-drafts")
def document_skill_drafts(source_table: Literal["stock_news", "stock_notice"], record_id: int,
                         db: Session = Depends(get_db)):
    from app.services.document_semantic_skills import draft_read
    row = required_document(db, source_table, record_id)
    codes = ((row.content_json or {}).get("semantic_governance_plan") or {}).get("draft_skill_codes") or {}
    return {index: draft_read(skill, db=db) for index, code in codes.items()
        if (skill := db.scalar(select(ModelSkill).where(ModelSkill.skill_code == code))) is not None}


@router.post("/skill-drafts/{skill_id}/test-jobs", status_code=202)
def queue_skill_draft_test(skill_id: int, payload: SkillDraftAction, db: Session = Depends(get_db)):
    from app.services.document_semantic_skills import submit_test_job
    skill = db.get(ModelSkill, skill_id)
    if not skill or not (skill.config_json or {}).get("semantic_generated_draft"):
        raise HTTPException(404, "专项Skill草稿不存在")
    try:
        return submit_test_job(db, skill, reviewer=payload.reviewer)
    except ValueError as exc:
        db.rollback()
        raise HTTPException(409 if isinstance(exc, WorkflowConflict) else 422, str(exc)) from exc


@router.get("/skill-test-jobs/{job_id}")
def skill_draft_test_job(job_id: int, db: Session = Depends(get_db)):
    from app.models.pipeline import ScheduledJob
    from app.services.document_semantic_skills import TEST_TASK_TYPE, test_job_read
    job = db.get(ScheduledJob, job_id)
    if not job or job.task_type != TEST_TASK_TYPE:
        raise HTTPException(404, "专项Skill测试任务不存在")
    return test_job_read(db, job)


@router.post("/skill-drafts/{skill_id}/{action}")
def operate_skill_draft(skill_id: int, action: Literal["test", "enable"], payload: SkillDraftAction,
                        db: Session = Depends(get_db)):
    from app.services.document_semantic_skills import test_draft, enable_draft
    skill = db.get(ModelSkill, skill_id)
    if not skill or not (skill.config_json or {}).get("semantic_generated_draft"):
        raise HTTPException(404, "专项Skill草稿不存在")
    try:
        if action == "enable":
            return enable_draft(db, skill, reviewer=payload.reviewer)
        config = skill.config_json
        row = required_document(db, config["draft_source_table"], config["draft_record_id"])
        return test_draft(db, skill, row, reviewer=payload.reviewer)
    except ValueError as exc:
        db.rollback()
        raise HTTPException(409 if isinstance(exc, WorkflowConflict) else 422, str(exc)) from exc


def current_version(db):
    skill = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == SKILL_CODE))
    return skill.version if skill else "未登记"


def _skill_test_status(value: object, *, error: str | None = None) -> str:
    """Normalize stored evaluation details into a small UI-safe status set."""
    text = " ".join(str(item or "") for item in (value, error)).upper()
    if any(token in text for token in (
        "MODEL_UNAVAILABLE", "HTTP 401", "HTTP 402", "HTTP 403", "HTTP 408", "HTTP 429",
        "INSUFFICIENT BALANCE", "INSUFFICIENT QUOTA", "余额不足", "额度不足", "模型服务不可用",
    )):
        return "MODEL_UNAVAILABLE"
    if value is True or str(value).upper() in {"PASSED", "PASS", "SUCCESS"}:
        return "PASSED"
    if value is False or str(value).upper() in {"FAILED", "FAIL", "ERROR"}:
        return "FAILED"
    return "PENDING"


def _error_call_ids(text: object) -> list[int]:
    return [int(item) for item in re.findall(r"(?:模型调用|model call)[#：:\s]*(\d+)", str(text or ""), flags=re.IGNORECASE)]


def _error_call_ids(text: object) -> list[int]:
    # Historical reports contain both UTF-8 Chinese and the old mojibake
    # spelling. Keep the parser permissive so audit logs remain traceable.
    pattern = r"(?:model\s*call|\u6a21\u578b\u8c03\u7528|妯″瀷璋冪敤)\s*[#:#：锛?]*\s*(\d+)"
    return [int(item) for item in re.findall(pattern, str(text or ""), flags=re.IGNORECASE)]


def _model_attempts(calls: list[ModelCallLog]) -> list[dict]:
    """Flatten durable model call traces into a UI-safe attempt chain."""
    attempts: list[dict] = []
    seen: set[tuple] = set()
    for call in sorted(calls, key=lambda item: (item.started_at.timestamp() if item.started_at else float("-inf"), item.id), reverse=True):
        request = call.request_json if isinstance(call.request_json, dict) else {}
        trace = request.get("attempted_models") if isinstance(request.get("attempted_models"), list) else []
        if trace:
            for item in trace:
                if not isinstance(item, dict):
                    continue
                row = {
                    "provider": item.get("provider") or call.provider_code,
                    "instance": item.get("instance") or call.instance_code,
                    "model": item.get("model") or call.model_code,
                    "status": item.get("status") or call.status,
                    "error": item.get("error") or (call.error_message if call.status == "FAILED" else None),
                    "retryable": bool(item.get("retryable")),
                    "call_log_id": item.get("call_log_id") or call.id,
                    "attempt": item.get("attempt"),
                    "at": item.get("at") or (call.started_at.isoformat() if call.started_at else None),
                }
                key = (row["call_log_id"], row["attempt"], row["provider"], row["status"])
                if key not in seen:
                    attempts.append(row)
                    seen.add(key)
            continue
        row = {"provider": call.provider_code, "instance": call.instance_code,
               "model": call.model_code, "status": call.status,
               "error": call.error_message, "retryable": False,
               "call_log_id": call.id, "attempt": 1,
               "at": call.started_at.isoformat() if call.started_at else None}
        key = (call.id, 1, call.provider_code, call.status)
        if key not in seen:
            attempts.append(row)
            seen.add(key)
    return sorted(attempts, key=lambda item: str(item.get("at") or ""), reverse=True)


def _model_failure_summary(attempts: list[dict]) -> str:
    failures = []
    for item in attempts:
        if str(item.get("status") or "").upper() == "SUCCESS":
            continue
        name = item.get("provider") or item.get("instance") or "模型"
        error = str(item.get("error") or "未知错误").replace("\n", " ").strip()
        if item.get("retryable"):
            error = f"{error}（可重试）"
        failures.append(f"{name}：{error[:300]}")
    return "；".join(dict.fromkeys(failures))


def _iso(value):
    return value.isoformat() if value is not None and hasattr(value, "isoformat") else value


def semantic_skill_status(db: Session, skill: ModelSkill) -> dict:
    """Return an operational, non-verbose acceptance summary for one draft Skill.

    The source list is frozen on the Skill configuration.  Evaluation runs and
    results are append-only, so this endpoint deliberately reports both the
    latest batch and its history rather than deriving a single mutable status.
    """
    config = skill.config_json or {}
    sources = list(config.get("validation_sources") or [])
    runs = list(db.scalars(select(SkillEvaluationRun).where(
        SkillEvaluationRun.skill_id == skill.id,
    ).order_by(SkillEvaluationRun.started_at.desc(), SkillEvaluationRun.id.desc())).all())
    latest = runs[0] if runs else None
    result_rows = list(db.scalars(select(SkillEvaluationResult).where(
        SkillEvaluationResult.run_id == latest.id,
    ).order_by(SkillEvaluationResult.id)).all()) if latest else []
    by_source = {}
    for result in result_rows:
        metrics = result.metrics_json or {}
        key = (metrics.get("source_table"), metrics.get("record_id"))
        if key[0] and key[1] is not None:
            by_source[key] = result

    stored = (config.get("semantic_draft_test") or {})
    stored_items = stored.get("document_results") or []
    stored_by_source = {(item.get("source_table"), item.get("record_id")): item for item in stored_items}
    # Resolve model-call details before classifying documents.  The semantic
    # worker stores a compact "model call #id" marker in each result; the
    # provider's HTTP status and quota explanation live in ModelCallLog.
    candidate_call_ids = set(_error_call_ids((latest.summary_json or {}).get("error") if latest else None))
    for item in stored_items:
        candidate_call_ids.update(_error_call_ids(item.get("extraction_error") or item.get("error")))
    for result in result_rows:
        metrics = result.metrics_json or {}
        candidate_call_ids.update(_error_call_ids(metrics.get("extraction_error") or (metrics.get("evaluation") or {}).get("summary")))
    latest_summary = (latest.summary_json or {}) if latest else {}
    for attempt in latest_summary.get("attempted_models") or []:
        if isinstance(attempt, dict) and attempt.get("call_log_id"):
            candidate_call_ids.add(int(attempt["call_log_id"]))
    call_rows = list(db.scalars(select(ModelCallLog).where(ModelCallLog.id.in_(candidate_call_ids))).all()) if candidate_call_ids else []
    call_by_id = {call.id: call for call in call_rows}
    latest_marker_error = (latest.summary_json or {}).get("error") if latest else None
    latest_call_errors = [call.error_message for call_id in _error_call_ids(latest_marker_error)
                          if (call := call_by_id.get(call_id)) and call.error_message]
    documents = []
    counts = {key: 0 for key in ("total", "processed", "passed", "failed", "model_unavailable", "pending")}
    for source in sources:
        table, record_id = source.get("source_table"), source.get("record_id")
        result = by_source.get((table, record_id))
        item = stored_by_source.get((table, record_id), {})
        metrics = result.metrics_json if result else item
        evaluation = metrics.get("evaluation") if isinstance(metrics, dict) else {}
        marker_error = (metrics or {}).get("extraction_error") or (evaluation or {}).get("summary")
        linked_call_errors = [call.error_message for call_id in _error_call_ids(marker_error) if (call := call_by_id.get(call_id)) and call.error_message]
        error = "\n".join([str(value) for value in [marker_error, *linked_call_errors] if value])
        status = _skill_test_status(result.passed if result else None, error=error)
        if result is None and latest is not None and (latest.status in {"FAILED", "INTERRUPTED"}):
            # A provider failure can stop a batch before later documents run.
            status = "PENDING"
        counts["total"] += 1
        counts["processed"] += status != "PENDING"
        counts["passed"] += status == "PASSED"
        counts["failed"] += status == "FAILED"
        counts["model_unavailable"] += status == "MODEL_UNAVAILABLE"
        counts["pending"] += status == "PENDING"
        documents.append({
            "source_table": table, "record_id": record_id,
            "status": status, "passed": status == "PASSED",
            "title": (metrics or {}).get("title"),
            "error": error, "score": (evaluation or {}).get("score"),
            "event_count": (metrics or {}).get("event_count", 0),
            "relation_count": (metrics or {}).get("relation_count", 0),
            "run_id": latest.id if latest else None,
        })

    summary = latest_summary if latest else stored
    call_ids = set(_error_call_ids(summary.get("error")))
    for attempt in summary.get("attempted_models") or []:
        if isinstance(attempt, dict) and attempt.get("call_log_id"):
            call_ids.add(int(attempt["call_log_id"]))
    for document in documents:
        call_ids.update(_error_call_ids(document.get("error")))
    calls = list(db.scalars(select(ModelCallLog).where(ModelCallLog.id.in_(call_ids))).all()) if call_ids else []
    latest_errors = [{"id": call.id, "provider": call.provider_code, "instance": call.instance_code,
                      "status": call.status, "error": call.error_message, "created_at": _iso(call.started_at)}
                     for call in sorted(calls, key=lambda item: item.id, reverse=True)]
    attempts = _model_attempts(calls)
    failure_summary = _model_failure_summary(attempts)
    last_successful = next((item for item in reversed(attempts)
                            if str(item.get("status") or "").upper() == "SUCCESS"), None)
    progress = config.get("semantic_test_progress") or {}
    test_result = stored or {}
    can_enable = bool(test_result.get("passed")) and skill.lifecycle_status != "DEPRECATED" and not skill.enabled
    if test_result.get("pending_version_test"):
        can_enable = False
    if skill.enabled:
        next_action = "已启用，可用于正式治理"
    elif counts["model_unavailable"]:
        next_action = "模型恢复后重新测试全部关联文档"
    elif counts["failed"] or counts["pending"] or not test_result.get("passed"):
        next_action = "继续测试并根据LLM复核结果迭代Skill"
    else:
        next_action = "人工确认后启用专项Skill"
    history = []
    for run in runs[:20]:
        run_summary = run.summary_json or {}
        history.append({
            "run_id": run.id, "skill_version": run.skill_version,
            "status": run.status, "started_at": _iso(run.started_at), "completed_at": _iso(run.completed_at),
            "document_count": run_summary.get("document_count", 0),
            "processed_document_count": run_summary.get("processed_document_count", run_summary.get("document_count", 0)),
            "passed": bool(run_summary.get("passed")),
            "failure_kind": run_summary.get("failure_kind"), "summary": run_summary.get("summary"),
            "error": run_summary.get("error"),
        })
    return {
        "skill": {"id": skill.id, "skill_code": skill.skill_code, "skill_name": skill.skill_name,
                  "version": skill.version, "enabled": skill.enabled, "lifecycle_status": skill.lifecycle_status},
        "summary": {**counts, "latest_run_id": latest.id if latest else None,
                     "latest_run_status": latest.status if latest else None,
                     "current_progress": progress.get("status", "NOT_STARTED"),
                     "current_phase": progress.get("phase"), "current_message": progress.get("message")},
        "documents": documents, "current_test": {"passed": bool(test_result.get("passed")),
            "score": test_result.get("score"), "failure_kind": test_result.get("failure_kind"),
            "error": test_result.get("error"), "summary": test_result.get("summary"),
            "skill_version": test_result.get("skill_version"), "pending_version_test": test_result.get("pending_version_test", False)},
        "history": history, "latest_model_errors": latest_errors,
        "attempted_models": attempts,
        "model_failure_summary": failure_summary or None,
        "retryable": any(bool(item.get("retryable")) for item in attempts),
        "last_successful_model": last_successful,
        "can_enable": can_enable, "next_action": next_action,
        "validation_sources": sources,
    }


def _specialist_skill_or_404(db: Session, skill_id: int) -> ModelSkill:
    skill = db.get(ModelSkill, skill_id)
    if not skill or not (skill.config_json or {}).get("semantic_generated_draft"):
        raise HTTPException(404, "专项 Skill 草稿不存在")
    return skill


@router.get("/semantic-skill-status/{skill_id}")
def get_semantic_skill_status(skill_id: int, db: Session = Depends(get_db)):
    """Return the append-only acceptance summary for every linked document."""
    skill = _specialist_skill_or_404(db, skill_id)
    return semantic_skill_status(db, skill)


@router.post("/semantic-skill-status/{skill_id}/retry", status_code=202)
def retry_semantic_skill_status(skill_id: int, payload: SemanticSkillRetryRequest,
                                db: Session = Depends(get_db)):
    """Queue a fresh acceptance batch without replacing historical runs."""
    from app.services.document_semantic_skills import submit_test_job

    skill = _specialist_skill_or_404(db, skill_id)
    try:
        result = submit_test_job(db, skill, reviewer=payload.reviewer)
        # Include the immediately readable aggregate so the UI can update
        # without making a second request when the queue accepts the job.
        return {**result, "semantic_skill_status": semantic_skill_status(db, skill)}
    except ValueError as exc:
        db.rollback()
        raise HTTPException(409 if isinstance(exc, WorkflowConflict) else 422, str(exc)) from exc


def required_document(db, table, identity):
    row = db.get(MODELS[table], identity)
    if not row:
        raise HTTPException(404, "新闻公告不存在")
    return row


def summary(row, version, db: Session | None = None):
    state = (row.content_json or {}).get("event_governance") or {}
    analysis = state.get("semantic_analysis") or {}
    versions = versions_for(row)
    selected_key = version_key(versions[-1], len(versions) - 1) if versions else None
    result = {"source_table": row.__tablename__, "record_id": row.id, "market": row.market, "symbol": row.symbol,
        "title": row.title, "published_at": getattr(row, "news_time", None) or row.notice_date,
        "source_name": getattr(row, "source_name", None) or f"数据源#{row.source_id}",
        "status": state.get("status", "NOT_PROCESSED"), "error": state.get("error"),
        "body_characters": len(document_body(row)), "body_available": bool(document_body(row)) and not _search_excerpt(row),
        "semantic_complete": _done(row, version, True), "skill_version": state.get("skill_version"),
        "current_skill_version": version, "schema_version": state.get("semantic_schema_version"),
        "event_count": len(analysis.get("events") or []), "relation_count": len(analysis.get("relations") or []),
        "version_count": len(versions), "governance_plan": (row.content_json or {}).get("semantic_governance_plan"),
        "workflow_status": workflow_status(row, state)}
    if db is not None:
        result["human_review"] = review_status(db, row.__tablename__, row.id, selected_key)
    return result


@router.post("/documents/{source_table}/{record_id}/retry-correction", status_code=202)
def retry_document_correction(source_table: Literal["stock_news", "stock_notice"], record_id: int,
                              db: Session = Depends(get_db)):
    """Retry persisted feedback after model/network/queue failure, without reapproval."""
    from app.api.stock_batch import submit_stock_batch_governance
    row = required_document(db, source_table, record_id)
    state = (row.content_json or {}).get("event_governance") or {}
    try:
        plan = validate_confirmed_plan(db, row)
        if state.get("result_review_status") != "NEEDS_CORRECTION" or not state.get("correction_feedback"):
            raise WorkflowConflict("当前文档没有待重试的修正意见")
        if (state.get("governance_plan_id"), state.get("governance_plan_revision")) != (plan["plan_id"], plan["revision"]):
            raise WorkflowConflict("修正意见不属于当前方案，请刷新")
    except ValueError as exc:
        raise HTTPException(409, str(exc)) from exc
    queued = submit_stock_batch_governance(StockBatchGovernanceRequest(
        stocks=[{"market": row.market, "symbol": row.symbol}], governance_mode="AI_AGENT_SKILL_GOVERNANCE",
        structure_documents=True, semantic_analysis=True, interactive_plan_required=True,
        document_targets=[DocumentTarget(source_table=source_table, record_id=record_id)],
        document_skill_codes=plan["recommended_skill_codes"], structured_document_limit=1,
        collect_business_data=False, export_lakehouse=False, archive_chunks=False, run_graph=False, business_types=[],
        semantic_feedback=state["semantic_feedback"], semantic_review_id=state["semantic_review_id"],
        idempotency_key=f"semantic-result-review:{state['semantic_review_id']}"), db)
    if queued.get("job_status") in {"FAILED", "COMPLETED"}:
        from app.api.stock_batch import retry_stock_batch_governance_job
        queued = retry_stock_batch_governance_job(queued["job_id"], db)
    return queued


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
    return {"items": [summary(row, version, db) for row in rows[offset:offset + limit]], "total": total,
            "offset": offset, "limit": limit, "stock_name": stock.name if stock else None,
            "current_skill_version": version}


@router.get("/documents/{source_table}/{record_id}")
def detail(source_table: Literal["stock_news", "stock_notice"], record_id: int, version_index: int | None = Query(None, ge=0),
           db: Session = Depends(get_db)):
    row = required_document(db, source_table, record_id)
    payload = row.content_json or {}
    versions = versions_for(row)
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
    selected_key = version_key(state, index) if versions else None
    return {**summary(row, current_version(db), db), "body": evidence.content if evidence else document_body(row),
        "url": row.url, "selected_version": index, "state": state,
        "versions": [{"index": i, **{k: st.get(k) for k in ("skill_version", "semantic_schema_version", "observed_at", "status", "content_scope", "error")}} for i, st in enumerate(versions)],
        "facts": [fact_read(db, fact, full=False) for fact in facts if fact],
        "analysis": state.get("semantic_analysis"), "human_review": review_status(db, source_table, record_id, selected_key),
        "governance_plan": payload.get("semantic_governance_plan"),
        "governance_plan_history": payload.get("semantic_governance_plan_history") or [],
        "result_report": result_report(db, row, state, index) if versions else None,
        "workflow_status": workflow_status(row, state),
        "review_history": review_status(db, source_table, record_id).get("history", []),
        "note": "新闻转述、作者观点及模型推断均为待审核候选；人工确认只确认抽取结构，不代表官方交叉核验。"}


@router.get("/documents/selection")
def select_documents(market: str, symbol: str, kind: Literal["ALL", "NEWS", "NOTICE"] = "ALL",
                     q: str = Query("", max_length=256), db: Session = Depends(get_db)):
    """Freeze all matching identities, across pages, without downloading bodies."""
    documents = []
    for table, model in (("stock_news", StockNews), ("stock_notice", StockNotice)):
        if kind != "ALL" and kind != ("NEWS" if table == "stock_news" else "NOTICE"):
            continue
        query = select(model.id).where(model.market == market, model.symbol == symbol)
        if q:
            query = query.where(model.title.contains(q, autoescape=True))
        documents.extend({"source_table": table, "record_id": identity} for identity in db.scalars(query.order_by(model.id)))
    return {"documents": documents, "total": len(documents),
            "scope": {"market": market, "symbol": symbol, "kind": kind, "q": q}}


class AnalyzeRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    documents: list[DocumentTarget] = Field(min_length=1)
    idempotency_key: str = Field(min_length=1, max_length=128)


class PlanBatchRequest(AnalyzeRequest):
    force: bool = False


class ScopeRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    stocks: list[StockBatchTarget] = Field(min_length=1)
    kind: Literal["ALL", "NEWS", "NOTICE"] = "ALL"
    disclosure_days: int = Field(default=730, ge=1, le=3650)
    per_stock_limit: int | None = Field(default=None, ge=1)


class ScopeAnalyzeRequest(ScopeRequest):
    idempotency_key: str = Field(min_length=1, max_length=128)


@router.get("/planning-catalog")
def planning_catalog(db: Session = Depends(get_db)):
    return public_type_catalog(db)


@router.post("/documents/{source_table}/{record_id}/plan")
def propose_document_plan(source_table: Literal["stock_news", "stock_notice"], record_id: int,
                          payload: DocumentPlanGenerateRequest, db: Session = Depends(get_db)):
    row = required_document(db, source_table, record_id)
    try:
        return generate_plan(db, row, force=payload.force)
    except (ValueError, OSError) as exc:
        db.rollback()
        raise HTTPException(409 if isinstance(exc, WorkflowConflict) else 422, str(exc)) from exc


@router.post("/documents/{source_table}/{record_id}/plan-decision")
def confirm_document_plan(source_table: Literal["stock_news", "stock_notice"], record_id: int,
                          payload: DocumentPlanDecisionRequest, db: Session = Depends(get_db)):
    row = required_document(db, source_table, record_id)
    try:
        return decide_plan(db, row, primary_type=payload.primary_type,
            secondary_types=payload.secondary_types, skill_codes=payload.skill_codes,
            target_scopes=payload.target_scopes, reviewer=payload.reviewer,
            reason=payload.reason, decision=payload.decision,
            expected_plan_id=payload.expected_plan_id, expected_revision=payload.expected_revision)
    except ValueError as exc:
        db.rollback()
        raise HTTPException(409 if isinstance(exc, WorkflowConflict) else 422, str(exc)) from exc


@router.post("/plan-batch", status_code=202)
def plan_batch(payload: PlanBatchRequest, db: Session = Depends(get_db)):
    from app.api.stock_batch import submit_stock_batch_governance
    rows = [required_document(db, item.source_table, item.record_id) for item in payload.documents]
    stocks = list(dict.fromkeys((row.market, row.symbol) for row in rows))
    return submit_stock_batch_governance(StockBatchGovernanceRequest(
        stocks=[{"market": market, "symbol": symbol} for market, symbol in stocks],
        governance_mode="AI_AGENT_SKILL_GOVERNANCE", structure_documents=True, semantic_analysis=True,
        semantic_plan_only=True, semantic_plan_force=payload.force, interactive_plan_required=True, document_targets=payload.documents,
        collect_business_data=False, export_lakehouse=False, archive_chunks=False, run_graph=False, business_types=[],
        idempotency_key="semantic-plan:" + payload.idempotency_key), db)


@router.post("/plan-scope", status_code=202)
def plan_scope(payload: ScopeAnalyzeRequest, db: Session = Depends(get_db)):
    from app.api.stock_batch import submit_stock_batch_governance
    return submit_stock_batch_governance(StockBatchGovernanceRequest(
        stocks=[{"market": market, "symbol": symbol} for market, symbol in _normalized_scope(payload)],
        governance_mode="AI_AGENT_SKILL_GOVERNANCE", structure_documents=True, semantic_analysis=True,
        semantic_plan_only=True, interactive_plan_required=True, structured_document_types=_scope_types(payload.kind),
        structured_document_limit=payload.per_stock_limit, disclosure_days=payload.disclosure_days,
        collect_business_data=False, export_lakehouse=False, archive_chunks=False, run_graph=False, business_types=[],
        idempotency_key="semantic-plan-scope:" + payload.idempotency_key), db)


def _scope_types(kind: str) -> list[str]:
    return ["NEWS", "NOTICE"] if kind == "ALL" else [kind]


def _normalized_scope(payload: ScopeRequest) -> list[tuple[str, str]]:
    stocks = list(dict.fromkeys(
        (item.market.strip().upper(), item.symbol.strip().upper()) for item in payload.stocks
    ))
    return stocks


@router.post("/scope-preview")
def preview_scope(payload: ScopeRequest, db: Session = Depends(get_db)):
    """Preview the exact bounded stock/document scope before an AI task is queued."""
    version = current_version(db)
    document_types = _scope_types(payload.kind)
    stock_results, planned_documents, planned_count = [], [], 0
    for market, symbol in _normalized_scope(payload):
        stock = db.scalar(select(StockSymbol).where(
            StockSymbol.market == market, StockSymbol.symbol == symbol,
        ))
        if not stock:
            stock_results.append({"market": market, "symbol": symbol, "stock_name": None,
                                  "status": "MISSING", "total": 0, "completed": 0,
                                  "pending": 0, "planned": 0, "failed": 0, "missing_body": 0})
            continue
        rows = scope_rows(db, stock, payload.disclosure_days, document_types)
        pending = [row for row in rows if not _done(row, version, True)]
        planned = _select_pending(pending, payload.per_stock_limit)
        states = [((row.content_json or {}).get("event_governance") or {}) for row in rows]
        stock_results.append({
            "market": market, "symbol": symbol, "stock_name": stock.name, "status": "READY",
            "total": len(rows), "completed": sum(_done(row, version, True) for row in rows),
            "pending": len(pending), "planned": len(planned),
            "failed": sum(state.get("status") == "EXTRACTION_FAILED" for state in states),
            "missing_body": sum(not document_body(row) or _search_excerpt(row) for row in rows),
        })
        planned_count += len(planned)
        # Preview transport is bounded; execution scope and its count are not.
        planned_documents.extend({**summary(row, version), "planned": True} for row in planned[:max(0, 50 - len(planned_documents))])
    missing = [item for item in stock_results if item["status"] == "MISSING"]
    return {
        "stocks": stock_results, "planned_documents": planned_documents,
        "stock_count": len(stock_results), "planned_count": planned_count,
        "preview_truncated": planned_count > len(planned_documents),
        "pending_count": sum(item["pending"] for item in stock_results),
        "completed_count": sum(item["completed"] for item in stock_results),
        "missing_stocks": missing, "kind": payload.kind,
        "disclosure_days": payload.disclosure_days, "per_stock_limit": payload.per_stock_limit,
        "note": "仅规划当前Skill版本未完成的文档；已完成文档保持幂等，不重复消耗模型调用。",
    }


@router.post("/analyze-scope", status_code=202)
def analyze_scope(payload: ScopeAnalyzeRequest, db: Session = Depends(get_db)):
    """Queue semantic governance for multiple stocks without collecting unrelated stages."""
    targets = []
    for market, symbol in _normalized_scope(payload):
        stock = db.scalar(select(StockSymbol).where(StockSymbol.market == market, StockSymbol.symbol == symbol))
        if not stock:
            continue
        rows = scope_rows(db, stock, payload.disclosure_days, _scope_types(payload.kind))
        confirmed = [row for row in rows if ((row.content_json or {}).get("semantic_governance_plan") or {}).get("status") == "CONFIRMED"
            and not _done(row, current_version(db), True)]
        if payload.per_stock_limit is not None:
            confirmed = confirmed[:payload.per_stock_limit]
        targets.extend(DocumentTarget(source_table=row.__tablename__, record_id=row.id) for row in confirmed)
    if not targets:
        raise HTTPException(409, "范围内没有已确认且待执行的方案，请先生成并逐篇确认治理方案")
    return analyze(AnalyzeRequest(documents=targets, idempotency_key="scope:" + payload.idempotency_key), db)


@router.post("/analyze", status_code=202)
def analyze(payload: AnalyzeRequest, db: Session = Depends(get_db)):
    from app.api.stock_batch import submit_stock_batch_governance
    rows = [required_document(db, item.source_table, item.record_id) for item in payload.documents]
    skipped = [row for row in rows if ((row.content_json or {}).get("semantic_governance_plan") or {}).get("status") == "SKIPPED"]
    rows = [row for row in rows if row not in skipped]
    unconfirmed = [row for row in rows if ((row.content_json or {}).get("semantic_governance_plan") or {}).get("status") != "CONFIRMED"]
    if unconfirmed:
        raise HTTPException(409, detail={"message": "执行前必须逐篇确认文档类型与治理Skill方案",
            "documents": [{"source_table": row.__tablename__, "record_id": row.id, "title": row.title} for row in unconfirmed]})
    if not rows:
        return {"status": "SKIPPED", "message": "所选文档均已由用户选择不治理", "skipped": len(skipped)}
    try:
        for row in rows:
            validate_confirmed_plan(db, row)
    except WorkflowConflict as exc:
        raise HTTPException(409, str(exc)) from exc
    stocks = list(dict.fromkeys((row.market, row.symbol) for row in rows))
    skill_codes = list(dict.fromkeys(code for row in rows for code in
        (((row.content_json or {}).get("semantic_governance_plan") or {}).get("recommended_skill_codes") or [])))
    targets = [DocumentTarget(source_table=row.__tablename__, record_id=row.id) for row in rows]
    request = StockBatchGovernanceRequest(stocks=[{"market": market, "symbol": symbol} for market, symbol in stocks],
        governance_mode="AI_AGENT_SKILL_GOVERNANCE", structure_documents=True, semantic_analysis=True,
        interactive_plan_required=True,
        document_targets=targets, document_skill_codes=skill_codes, structured_document_limit=len(rows), collect_business_data=False,
        export_lakehouse=False, archive_chunks=False, run_graph=False, business_types=[],
        semantic_feedback=None,
        idempotency_key="semantic:" + payload.idempotency_key)
    return submit_stock_batch_governance(request, db)


@router.get("/documents/{source_table}/{record_id}/reviews")
def reviews(source_table: Literal["stock_news", "stock_notice"], record_id: int,
            version_index: int | None = Query(None, ge=0), db: Session = Depends(get_db)):
    row = required_document(db, source_table, record_id)
    versions = versions_for(row)
    if version_index is not None and version_index >= len(versions):
        raise HTTPException(404, "治理版本不存在")
    key = version_key(versions[version_index], version_index) if version_index is not None else None
    return review_status(db, source_table, record_id, key)


@router.get("/documents/{source_table}/{record_id}/compare")
def compare_document_versions(source_table: Literal["stock_news", "stock_notice"], record_id: int,
                              from_index: int | None = Query(None, ge=0),
                              to_index: int | None = Query(None, ge=0),
                              db: Session = Depends(get_db)):
    """Compare two immutable semantic snapshots at field level."""
    row = required_document(db, source_table, record_id)
    try:
        return compare_versions(row, from_index, to_index)
    except ValueError as exc:
        raise HTTPException(422, str(exc)) from exc


@router.post("/documents/{source_table}/{record_id}/reviews")
def review_document(source_table: Literal["stock_news", "stock_notice"], record_id: int,
                    payload: DocumentReviewRequest, db: Session = Depends(get_db)):
    row = required_document(db, source_table, record_id)
    versions = versions_for(row)
    if payload.version_index >= len(versions):
        raise HTTPException(404, "治理版本不存在")
    state = versions[payload.version_index]
    from app.models.foundation import FoundationEvidence
    evidence = db.get(FoundationEvidence, state.get("evidence_id")) if state.get("evidence_id") else None
    try:
        result = apply_review(db, row, payload, evidence.content if evidence else document_body(row))
    except ValueError as exc:
        db.rollback()
        raise HTTPException(422, str(exc)) from exc
    if payload.action == "REANALYZE":
        from app.api.stock_batch import submit_stock_batch_governance
        plan = (row.content_json or {}).get("semantic_governance_plan") or {}
        request = StockBatchGovernanceRequest(stocks=[{"market": row.market, "symbol": row.symbol}],
            governance_mode="AI_AGENT_SKILL_GOVERNANCE", structure_documents=True, semantic_analysis=True,
            interactive_plan_required=True,
            document_targets=[DocumentTarget(source_table=source_table, record_id=record_id)], structured_document_limit=1,
            document_skill_codes=plan.get("recommended_skill_codes") or [],
            collect_business_data=False, export_lakehouse=False, archive_chunks=False, run_graph=False,
            business_types=[], semantic_feedback=payload.reason, semantic_review_id=result["review_id"],
            idempotency_key=f"semantic-review:{result['review_id']}")
        result["job"] = submit_stock_batch_governance(request, db)
    return result


@router.post("/documents/{source_table}/{record_id}/result-review")
def confirm_document_result(source_table: Literal["stock_news", "stock_notice"], record_id: int,
                            payload: DocumentResultReviewRequest, db: Session = Depends(get_db)):
    row = required_document(db, source_table, record_id)
    try:
        result = review_result(db, row, version_index=payload.version_index,
            reviewer=payload.reviewer, nonconforming=[item.model_dump() for item in payload.nonconforming],
            decision=payload.decision, reason=payload.reason, explicit_confirmations=payload.explicit_confirmations,
            expected_version_key=payload.expected_version_key, expected_plan_id=payload.expected_plan_id)
    except ValueError as exc:
        db.rollback()
        raise HTTPException(409 if isinstance(exc, WorkflowConflict) else 422, str(exc)) from exc
    if result.get("status") == "NEEDS_CORRECTION":
        from app.api.stock_batch import submit_stock_batch_governance
        plan = (row.content_json or {}).get("semantic_governance_plan") or {}
        feedback = result["semantic_feedback"]
        request = StockBatchGovernanceRequest(stocks=[{"market": row.market, "symbol": row.symbol}],
            governance_mode="AI_AGENT_SKILL_GOVERNANCE", structure_documents=True, semantic_analysis=True,
            interactive_plan_required=True,
            document_targets=[DocumentTarget(source_table=source_table, record_id=record_id)],
            document_skill_codes=plan.get("recommended_skill_codes") or [], structured_document_limit=1,
            collect_business_data=False, export_lakehouse=False, archive_chunks=False, run_graph=False,
            business_types=[], semantic_feedback=feedback,
            semantic_review_id=result["semantic_review_id"],
            idempotency_key=f"semantic-result-review:{result['semantic_review_id']}")
        result["job"] = submit_stock_batch_governance(request, db)
    return result
