"""Incremental remediation, immutable reports and scoped result previews."""
from __future__ import annotations

from datetime import datetime, timezone
from hashlib import sha256
import json
import math
from uuid import uuid4

from fastapi import HTTPException
from fastapi.encoders import jsonable_encoder
from jsonschema import Draft202012Validator
from sqlalchemy import func, select

from app.models.ai_hub import (KnowledgeDocument, KnowledgeEntity, KnowledgeGraph, KnowledgeRelation,
                               ModelSkill, ModelSkillRevision, SkillOptimizationDraft)
from app.models.foundation import FoundationFact
from app.models.market_data import (StockSymbol, StockNews, StockNotice, StockKline, StockFinancialReport,
                                   StockRealtimeQuote, StockF10Cache)
from app.models.pipeline import PipelineRun, ScheduledJob
from app.schemas.stock_batch import StockBatchGovernanceRequest
from app.services.document_event_governance import SKILL_CODE, _parse_events
from app.services.foundation import fact_read


def require_job(db, job_id):
    row = db.get(ScheduledJob, job_id)
    if not row or row.task_type != "stock_batch_governance":
        raise HTTPException(404, "股票治理任务不存在")
    return row


def _request(job):
    return dict((job.payload_json or {}).get("request") or {})


def history(db, job):
    root = _request(job).get("root_job_id") or job.id
    # JSON equality, not a global task history or substring search.
    rows = db.scalars(select(ScheduledJob).where(
        ScheduledJob.task_type == job.task_type,
        (ScheduledJob.id == root) | (ScheduledJob.payload_json["request"]["root_job_id"].as_integer() == root),
    ).order_by(ScheduledJob.id)).all()
    result = []
    for row in rows:
        run = db.get(PipelineRun, row.pipeline_run_id) if row.pipeline_run_id else None
        result.append({"job_id": row.id, "parent_job_id": _request(row).get("parent_job_id"),
                       "created_at": row.created_at, "status": row.status,
                       "result_status": (run.output_json or {}).get("result_status") if run else None})
    return result


def report_actions(db, job):
    run = db.get(PipelineRun, job.pipeline_run_id)
    audit = (run.output_json or {}).get("governance_actions") or {} if run else {}
    drafts = []
    for draft_id in audit.get("draft_ids") or []:
        draft = db.get(SkillOptimizationDraft, draft_id)
        if not draft:
            continue
        skill = db.get(ModelSkill, draft.skill_id)
        if not skill:
            continue
        revision = db.scalar(select(ModelSkillRevision).where(
            ModelSkillRevision.skill_id == skill.id, ModelSkillRevision.version == draft.base_skill_version))
        drafts.append({"id": draft.id, "skill_code": skill.skill_code, "skill_name": skill.skill_name,
            "base_skill_version": draft.base_skill_version, "current_version": skill.version,
            "status": draft.status, "rationale": draft.rationale,
            "proposed_instructions": draft.proposed_instructions,
            "current_instructions": revision.instructions if revision else skill.instructions,
            "created_at": draft.created_at, "reviewed_at": draft.reviewed_at,
            "validation": (audit.get("validations") or {}).get(str(draft.id))})
    return {"history": history(db, job), "drafts": drafts, "continuations": audit.get("continuations") or []}


def freeze_report(db, job):
    from app.api.stock_batch import _job_view
    run = db.get(PipelineRun, job.pipeline_run_id)
    if run and not (run.output_json or {}).get("report_snapshot_v1"):
        snapshot = jsonable_encoder(_job_view(db, job, details=True))
        snapshot["governance_report"].pop("actions", None)
        run.output_json = {**(run.output_json or {}), "report_snapshot_v1": snapshot,
                           "report_frozen_at": datetime.now(timezone.utc).isoformat()}
        db.commit()


def _audit(db, job, **updates):
    run = db.get(PipelineRun, job.pipeline_run_id)
    run.output_json = {**(run.output_json or {}), "governance_actions": {
        **((run.output_json or {}).get("governance_actions") or {}), **updates,
    }}
    db.commit()


def continuation_plan(db, job):
    from app.api.stock_batch import _job_view
    report = _job_view(db, job)["governance_report"]
    original = _request(job)
    stages = report.get("stage_results") or {}
    failures = (stages.get("BUSINESS_DATA") or {}).get("incomplete_items") or []
    business_types = list(dict.fromkeys(str(item.get("code", "")).split(":")[-1] for item in failures))
    business_types = [code for code in business_types if code in {"QUOTE", "KLINE", "FINANCIAL", "NEWS", "NOTICE", "F10"}]
    if job.status == "FAILED" and not business_types:
        business_types = list(original.get("business_types") or [])
    knowledge_incomplete = any((stages.get(stage) or {}).get("status") not in {None, "SUCCESS", "SKIPPED"}
                               for stage in ("LAKEHOUSE_EXPORT", "DOCUMENT_CHUNKS", "KNOWLEDGE_GRAPH"))
    event_incomplete = (stages.get("STRUCTURED_EVENTS") or {}).get("status") != "SUCCESS"
    reasons = [{"stage": stage, "reason": item.get("reason"), "code": item.get("code")}
               for stage, value in stages.items() if isinstance(value, dict)
               for item in value.get("incomplete_items") or []]
    if event_incomplete and not stages.get("STRUCTURED_EVENTS"):
        reasons.append({"stage": "STRUCTURED_EVENTS", "reason": "历史任务未执行新闻公告正文结构化，补充候选事实和证据链"})
    root = original.get("root_job_id") or job.id
    options = {**original, "stocks": original.get("stocks") or [], "parent_job_id": job.id, "root_job_id": root,
        "collect_business_data": bool(business_types), "business_types": business_types,
        "structure_documents": event_incomplete, "structured_document_limit": 20,
        "export_lakehouse": knowledge_incomplete, "archive_chunks": knowledge_incomplete,
        "run_graph": knowledge_incomplete, "run_agent_governance": False,
        "continuation_action": None, "continuation_stage": None, "continuation_item_codes": []}
    return {"parent_job_id": job.id, "root_job_id": root, "reasons": reasons,
        "request": options, "can_continue": bool(business_types or knowledge_incomplete or event_incomplete),
        "notes": ["按未通过业务类型定向采集，成功业务类型不重复采集。", "结构化每轮有界处理20份正文，未完成部分保留PARTIAL。",
                  "知识阶段按依赖重发范围版本；待审核事实及缺少来源不会自动变为已验证。", "创建新的治理任务及批次，保留此前报告。"]}


def continue_job(db, job, client_key: str):
    from app.api.stock_batch import submit_stock_batch_governance, find_stock_batch_job_by_client_key, _job_view
    if not client_key or len(client_key) > 128:
        raise HTTPException(422, "必须提供1-128字符幂等键")
    key = f"continue:{job.id}:{client_key}"
    existing = find_stock_batch_job_by_client_key(db, key)
    if existing:
        return _job_view(db, existing, details=False)
    if job.status in {"PENDING", "RUNNING", "RETRY"}:
        raise HTTPException(409, "当前任务尚未结束，请先查看执行进度")
    for item in history(db, job):
        if item["job_id"] != job.id and item["status"] in {"PENDING", "RUNNING", "RETRY"}:
            raise HTTPException(409, f"治理历程中任务#{item['job_id']}正在执行，无需重复续作")
    plan = continuation_plan(db, job)
    if not plan["can_continue"]:
        raise HTTPException(409, "当前没有需要继续执行的阶段")
    freeze_report(db, job)
    payload = {**plan["request"], "idempotency_key": key,
               "governance_batch_id": f"continuation:{job.id}:{sha256(key.encode()).hexdigest()[:20]}"}
    result = submit_stock_batch_governance(StockBatchGovernanceRequest.model_validate(payload), db)
    audit = (db.get(PipelineRun, job.pipeline_run_id).output_json or {}).get("governance_actions") or {}
    _audit(db, job, continuations=[*(audit.get("continuations") or []), {
        "job_id": result["job_id"], "created_at": datetime.now(timezone.utc).isoformat(),
        "reasons": plan["reasons"], "request": payload,
    }])
    return result


def create_optimization_drafts(db, job):
    from app.api.stock_batch import _job_view
    if job.status in {"PENDING", "RUNNING", "RETRY"}:
        raise HTTPException(409, "任务尚未结束，请在本轮报告生成后分析失败原因并优化Skill")
    freeze_report(db, job)
    report = _job_view(db, job)["governance_report"]
    mappings = {"BUSINESS_DATA": "STOCK_SOURCE_COLLECTION", "STRUCTURED_EVENTS": SKILL_CODE,
                "LAKEHOUSE_EXPORT": "LAKEHOUSE_KNOWLEDGE_PUBLISHER", "DOCUMENT_CHUNKS": "LAKEHOUSE_KNOWLEDGE_PUBLISHER",
                "KNOWLEDGE_GRAPH": "LAKEHOUSE_KNOWLEDGE_PUBLISHER", "AI_QUALITY_GATE": "STOCK_DATA_QUALITY_GATE"}
    grouped = {}
    for stage, skill_code in mappings.items():
        result = report["stage_results"].get(stage) or {}
        issues = result.get("incomplete_items") or []
        if issues:
            grouped.setdefault(skill_code, []).extend({"stage": stage, "code": item.get("code"), "reason": item.get("reason")} for item in issues)
    if "STRUCTURED_EVENTS" not in report["stage_results"]:
        grouped.setdefault(SKILL_CODE, []).append({"stage": "STRUCTURED_EVENTS", "reason": "旧流程缺少正文事件抽取与事实存储"})
    draft_ids = []
    for skill_code, reasons in grouped.items():
        skill = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == skill_code))
        if not skill:
            raise HTTPException(409, f"Skill {skill_code} 尚未登记")
        signature = "GOVERNANCE:" + sha256(json.dumps([job.id, skill_code, skill.version, reasons], ensure_ascii=False, sort_keys=True).encode()).hexdigest()
        draft = db.scalar(select(SkillOptimizationDraft).where(SkillOptimizationDraft.failure_signature == signature))
        if not draft:
            guidance = {
                SKILL_CODE: "缺正文先补官方原文；引文必须逐字存在于正文；分开标记全文分析、摘录候选、扫描件与来源失败；按正文哈希和Skill版本幂等续作，保留旧事实。",
                "STOCK_SOURCE_COLLECTION": "按市场板块选择已有有效接口；将网络超时、公开数据缺失及异步问答排队分别记录；仅重试未通过类型；本地已有记录不能证明本轮远程采集成功。",
                "LAKEHOUSE_KNOWLEDGE_PUBLISHER": "逐项核验缺少的来源和质量规则；区分空源、数值质量问题与图谱待审；每次只发布范围新版本；保持原图谱历史，不把PENDING改为已治理。",
                "STOCK_DATA_QUALITY_GATE": "按证据核验本次持久化数量与任务状态，异步排队不能算同步完成；禁止模型提升硬规则状态。",
            }[skill_code]
            rationale = json.dumps({"治理任务": job.id, "失败原因": reasons, "优化建议": guidance,
                "能力边界": "此草稿完善SOP和验收约束，不改变底层采集器、权限或数据源。网络、OCR、无公开来源及待人工审核需要对应工具或数据修复。"}, ensure_ascii=False, indent=2)
            draft = SkillOptimizationDraft(skill_id=skill.id, base_skill_version=skill.version,
                failure_signature=signature, prediction_ids=[], proposed_instructions=skill.instructions + "\n\n## 治理失败复核与定向续作\n" + guidance + "\n",
                rationale=rationale, status="PENDING_REVIEW")
            db.add(draft)
            db.flush()
        draft_ids.append(draft.id)
    audit = (db.get(PipelineRun, job.pipeline_run_id).output_json or {}).get("governance_actions") or {}
    _audit(db, job, draft_ids=list(dict.fromkeys([*(audit.get("draft_ids") or []), *draft_ids])))
    return report_actions(db, job)


def validate_draft(db, job, draft_id):
    actions = report_actions(db, job)
    selected = next((row for row in actions["drafts"] if row["id"] == draft_id), None)
    if not selected:
        raise HTTPException(404, "此草稿不属于当前治理任务")
    draft = db.get(SkillOptimizationDraft, draft_id)
    skill = db.get(ModelSkill, draft.skill_id)
    policy = skill.permission_policy_json or {}
    checks = [{"name": "基础版本与当前Skill一致", "passed": skill.version == draft.base_skill_version},
              {"name": "不允许任意SQL及无限制数据库写入", "passed": not policy.get("arbitrary_sql") and not policy.get("unrestricted_database_write")},
              {"name": "保留原SOP并增加缺口约束", "passed": draft.proposed_instructions.startswith(skill.instructions) and len(draft.proposed_instructions) > len(skill.instructions)}]
    try:
        Draft202012Validator.check_schema(skill.input_contract_json or {})
        Draft202012Validator.check_schema(skill.output_contract_json or {})
        checks.append({"name": "输入输出契约有效", "passed": True})
    except Exception as exc:
        checks.append({"name": "输入输出契约有效", "passed": False, "reason": str(exc)})
    body = "国际实业计提预计负债，业绩可能转亏，原文不能证明未来股价涨跌。"
    docs = [{"key": "test", "body": body}]
    good = {"documents": [{"key": "test", "events": [{"event_type": "预计负债", "summary": "计提预计负债", "direction": "UNKNOWN", "evidence_quote": "国际实业计提预计负债"}]}]}
    _parse_events(json.dumps(good, ensure_ascii=False), docs)
    checks.append({"name": "原文真实引用可通过结构化门禁", "passed": True})
    good["documents"][0]["events"][0]["evidence_quote"] = "此引用是原文中不存在的伪造金额"
    try:
        _parse_events(json.dumps(good, ensure_ascii=False), docs)
        rejected = False
    except ValueError:
        rejected = True
    checks.append({"name": "不存在于原文的引用被拒绝", "passed": rejected})
    result = {"status": "PASSED" if all(check["passed"] for check in checks) else "FAILED", "checks": checks,
              "base_skill_version": skill.version, "proposed_hash": sha256(draft.proposed_instructions.encode()).hexdigest(),
              "tested_at": datetime.now(timezone.utc).isoformat(),
              "scope": "契约、权限和事件证据门禁离线回归；未执行远程采集或模型全量效果评估，批准后需续作验收"}
    run = db.get(PipelineRun, job.pipeline_run_id)
    audit = (run.output_json or {}).get("governance_actions") or {}
    _audit(db, job, validations={**(audit.get("validations") or {}), str(draft.id): result})
    return result


def review_draft(db, job, draft_id, decision):
    from app.api.model_hub import approve_skill_draft, reject_skill_draft
    selected = next((row for row in report_actions(db, job)["drafts"] if row["id"] == draft_id), None)
    if not selected:
        raise HTTPException(404, "此草稿不属于当前治理任务")
    if decision == "APPROVE":
        validation = validate_draft(db, job, draft_id)
        if validation["status"] != "PASSED":
            raise HTTPException(409, "草稿离线检查未通过，不能启用")
        approve_skill_draft(draft_id, db)
    elif decision == "REJECT":
        reject_skill_draft(draft_id, db)
    else:
        raise HTTPException(422, "审核决定必须为 APPROVE 或 REJECT")
    return report_actions(db, job)


def _read(row):
    return {column.name: getattr(row, column.name) for column in row.__table__.columns}


def _preview_json(value, invalid_paths, path=""):
    if isinstance(value, float) and not math.isfinite(value):
        invalid_paths.append(path)
        return None
    if isinstance(value, dict):
        return {key: _preview_json(item, invalid_paths, f"{path}.{key}" if path else str(key)) for key, item in value.items()}
    if isinstance(value, (list, tuple)):
        return [_preview_json(item, invalid_paths, f"{path}[{index}]") for index, item in enumerate(value)]
    return value


def stage_preview(db, job, stage, code, limit=20, offset=0):
    from app.api.stock_batch import _job_view
    report = _job_view(db, job)["governance_report"]
    value = report["stage_results"].get(stage) or {}
    item = next((row for row in [*(value.get("completed_items") or []), *(value.get("incomplete_items") or [])] if row.get("code") == code), None)
    if not item:
        raise HTTPException(404, "此阶段检查项不属于本任务")
    details = item.get("details") or {}
    targets = {(row["market"], row["symbol"]) for row in _request(job).get("stocks") or []}
    items, total = [], 0
    note = "验收状态来自本轮报告；数据预览来自现存对应记录/版本，原文、证据及版本可展开核验。"
    if stage == "STRUCTURED_EVENTS":
        table, record_id = details.get("source_table"), details.get("source_record_id")
        model = {"stock_news": StockNews, "stock_notice": StockNotice}.get(table)
        row = db.get(model, record_id) if model and record_id else None
        if row and (row.market, row.symbol) in targets:
            facts = [db.get(FoundationFact, fact_id) for fact_id in details.get("fact_ids") or []]
            items = [fact_read(db, fact, full=True) for fact in facts if fact]
            if not items:
                items = [_read(row)]
            total = len(items)
            items = items[offset:offset + limit]
    elif stage == "BUSINESS_DATA":
        parts = code.split(":")
        if len(parts) != 3 or tuple(parts[:2]) not in targets:
            raise HTTPException(422, "股票身份不在任务范围内")
        model = {"QUOTE": StockRealtimeQuote, "KLINE": StockKline, "FINANCIAL": StockFinancialReport,
                 "NEWS": StockNews, "NOTICE": StockNotice, "F10": StockF10Cache}.get(parts[2])
        if model:
            query = select(model).where(model.market == parts[0], model.symbol == parts[1])
            total = int(db.scalar(select(func.count()).select_from(query.subquery())) or 0)
            items = [_read(row) for row in db.scalars(query.order_by(model.id.desc()).offset(offset).limit(limit)).all()]
    elif stage == "LAKEHOUSE_EXPORT" and details.get("dataset_id"):
        from app.services.lakehouse import preview_dataset
        preview = preview_dataset(db, int(details["dataset_id"]), version=details.get("version"), limit=limit, offset=offset)
        items = preview.get("rows") or preview.get("items") or []
        total = int(preview.get("row_count") or 0)
        details = {**details, "quality": preview.get("quality"), "schema": preview.get("schema"), "version": preview.get("version")}
        quality = preview.get("quality") or {}
        if quality.get("scope_pairs") and not quality.get("scope_applied"):
            note += "此项为共享公司主数据快照，记录总量不是本股票新增量；可按股票公司主体编号核对。"
    elif stage == "DOCUMENT_CHUNKS":
        from sqlalchemy import or_, and_
        doc_query = select(KnowledgeDocument).where(or_(*[
            and_(KnowledgeDocument.market == market, KnowledgeDocument.symbol == symbol) for market, symbol in targets
        ]))
        graph_ids = [row.get("graph_id") for row in (report["stage_results"].get("KNOWLEDGE_GRAPH") or {}).get("market_runs") or [] if row.get("graph_id")]
        if graph_ids:
            doc_query = doc_query.where(KnowledgeDocument.graph_id.in_(graph_ids))
        total = int(db.scalar(select(func.count()).select_from(doc_query.subquery())) or 0)
        from app.services.lakehouse import current_knowledge_document_chunks
        documents = list(db.scalars(doc_query.order_by(KnowledgeDocument.id.desc()).offset(offset).limit(limit)).all())
        chunks_by_document = current_knowledge_document_chunks(db, documents)
        for doc in documents:
            chunks = chunks_by_document.get(str(doc.id)) or []
            items.append({**_read(doc), "chunks": [_read(chunk) for chunk in chunks[:50]],
                          "current_chunk_count": len(chunks), "chunk_preview_limit": 50})
    elif stage == "KNOWLEDGE_GRAPH" and details.get("graph_id"):
        graph_id = int(details["graph_id"])
        graph = db.get(KnowledgeGraph, graph_id)
        query = select(KnowledgeRelation).where(KnowledgeRelation.graph_id == graph_id)
        total = int(db.scalar(select(func.count()).select_from(query.subquery())) or 0)
        for row in db.scalars(query.order_by(KnowledgeRelation.id).offset(offset).limit(limit)).all():
            subject, obj = db.get(KnowledgeEntity, row.subject_entity_id), db.get(KnowledgeEntity, row.object_entity_id)
            items.append({**_read(row), "subject": _read(subject) if subject else None, "object": _read(obj) if obj else None})
        details = {**details, "graph": _read(graph) if graph else None}
    result = {"title": item.get("label"), "status": item.get("status"), "reason": item.get("reason"),
            "report_snapshot": details, "items": items, "total": total, "limit": limit, "offset": offset,
            "note": note}
    invalid_paths = []
    result = _preview_json(result, invalid_paths)
    if invalid_paths:
        result["note"] += f" 原始记录中有{len(invalid_paths)}个非有限数值，本预览按缺失值展示；未修改原始数据，不能视为有效数值或零。"
        result["report_snapshot"] = {**(result["report_snapshot"] or {}), "invalid_numeric_count": len(invalid_paths),
                                     "invalid_numeric_paths": invalid_paths}
    return result
