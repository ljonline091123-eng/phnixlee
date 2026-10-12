"""Draft/test/enable specialist prompts using existing Skill and evaluation stores."""
from copy import deepcopy
from datetime import datetime, timezone
from hashlib import sha256
import json
from uuid import uuid4

from pydantic import BaseModel, ConfigDict, Field
from sqlalchemy import select

from app.models.ai_hub import AgentDefinition, AgentSkillLink, ModelSkill
from app.models.decision_review import SkillEvaluationCase, SkillEvaluationRun, SkillEvaluationResult
from app.services.document_semantic_planning import WorkflowConflict, _body_for_plan, _load_json, _lock_document
from app.services.document_semantics import semantic_analysis
from app.services.model_hub import ModelHubService, save_skill_content


class DraftInstructions(BaseModel):
    model_config = ConfigDict(extra="forbid")
    instructions: str = Field(min_length=40, max_length=12000)


def draft_read(skill):
    return {"id": skill.id, "skill_code": skill.skill_code, "skill_name": skill.skill_name,
        "instructions": skill.instructions, "lifecycle_status": skill.lifecycle_status,
        "test_result": (skill.config_json or {}).get("semantic_draft_test")}


def create_draft(db, row, *, gap_index, expected_plan_id, reviewer):
    plan = (row.content_json or {}).get("semantic_governance_plan") or {}
    if plan.get("plan_id") != expected_plan_id or plan.get("status") != "PROPOSED":
        raise WorkflowConflict("请在当前待确认方案中创建专项Skill")
    gaps = plan.get("capability_gaps") or []
    if gap_index >= len(gaps):
        raise ValueError("能力缺口不存在")
    existing = db.scalar(select(ModelSkill).where(ModelSkill.skill_code ==
        ((plan.get("draft_skill_codes") or {}).get(str(gap_index)) or "")))
    if existing:
        return draft_read(existing)
    base = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == "GENERIC_EVIDENCE_FACT_GOVERNOR"))
    if not base:
        raise ValueError("通用证据Skill尚未安装")
    gap = gaps[gap_index]
    log = ModelHubService(db).chat(task_type="data_governance", temperature=0, max_tokens=4000,
        messages=[{"role": "system", "content": "为文档治理能力缺口编写中文专项Skill规则。只输出提示词与操作规范，禁止生成可执行代码、SQL或自动发布指令。"
            "所有抽取均需逐字证据、主体边界、数值单位核验，区分事实/观点/推断，不确定项请求用户确认。只返回契约JSON：" + json.dumps(DraftInstructions.model_json_schema(), ensure_ascii=False)},
            {"role": "user", "content": json.dumps({"gap": gap, "expected_knowledge": plan.get("expected_knowledge")}, ensure_ascii=False)}],
        metadata_json={"purpose": "DOCUMENT_SPECIALIST_DRAFT", "require_real_model": True,
            "governance_plan_id": expected_plan_id, "reviewer": reviewer})
    if log.status != "SUCCESS" or str(log.provider_code).upper() == "MOCK":
        raise ValueError("Skill草稿未取得真实模型输出")
    generated = DraftInstructions.model_validate(_load_json(log.response_text)).instructions
    db.refresh(row)
    latest = (row.content_json or {}).get("semantic_governance_plan") or {}
    if latest.get("plan_id") != expected_plan_id or latest.get("revision") != plan.get("revision"):
        raise WorkflowConflict("草稿生成期间方案发生变化，请刷新")
    _lock_document(db, row)
    existing = db.scalar(select(ModelSkill).where(ModelSkill.skill_code ==
        ((latest.get("draft_skill_codes") or {}).get(str(gap_index)) or "")))
    if existing:
        db.commit()
        return draft_read(existing)
    code = "DOC_SPECIALIST_" + uuid4().hex[:16].upper()
    skill = ModelSkill(skill_code=code, skill_name=(gap["type_name"] + "专项治理")[:128],
        description=gap["reason"], instructions=base.instructions + "\n\n## 专项语义规则\n" + generated,
        enabled=False, lifecycle_status="DRAFT", skill_type=base.skill_type, version="1.0.0",
        input_contract_json=deepcopy(base.input_contract_json), output_contract_json=deepcopy(base.output_contract_json),
        permission_policy_json=deepcopy(base.permission_policy_json), side_effect_level=base.side_effect_level,
        idempotency_policy=base.idempotency_policy, retry_policy_json=deepcopy(base.retry_policy_json),
        error_policy_json=deepcopy(base.error_policy_json),
        config_json={**deepcopy(base.config_json or {}), "document_semantic_specialist": True,
            "semantic_generated_draft": True, "draft_plan_id": expected_plan_id,
            "draft_source_table": row.__tablename__, "draft_record_id": row.id,
            "created_by": reviewer, "draft_model_call_id": log.id})
    skill.input_contract_json["properties"]["skill_code"] = {"const": code}
    skill.config_json["function_spec"]["parameters"] = deepcopy(skill.input_contract_json)
    db.add(skill); db.flush()
    save_skill_content(db, skill, skill.instructions)
    updated = deepcopy(latest)
    updated["draft_skill_codes"] = {**(latest.get("draft_skill_codes") or {}), str(gap_index): code}
    row.content_json = {**(row.content_json or {}), "semantic_governance_plan": updated}
    db.commit()
    return draft_read(skill)


def require_test(skill, *, instructions=None, config=None):
    if not (skill.config_json or {}).get("semantic_generated_draft"):
        return
    result = (skill.config_json or {}).get("semantic_draft_test") or {}
    signature = sha256((instructions if instructions is not None else skill.instructions).encode()).hexdigest()
    if not result.get("passed") or result.get("instruction_hash") != signature or result.get("skill_version") != skill.version:
        raise WorkflowConflict("生成的专项Skill必须通过当前版本的真实模型与证据契约测试后才能启用")
    if config is not None and not config.get("semantic_generated_draft"):
        raise WorkflowConflict("生成Skill的测试门禁标记不能移除")


def test_draft(db, skill, row, *, reviewer):
    if not (skill.config_json or {}).get("semantic_generated_draft"):
        raise ValueError("该Skill不是专项治理草稿")
    if skill.enabled:
        raise WorkflowConflict("请先将Skill转为测试状态")
    body = _body_for_plan(row)
    signature, version = sha256(skill.instructions.encode()).hexdigest(), skill.version
    case = SkillEvaluationCase(case_code="DOC_DRAFT_" + uuid4().hex, task_type="data_governance",
        description="冻结原文的专项Skill证据与契约测试",
        input_json={"source_table": row.__tablename__, "record_id": row.id, "body": body, "title": row.title,
            "content_hash": sha256(body.encode()).hexdigest()},
        expected_json={"must_pass_evidence_contract": True}, source="SYSTEM", enabled=True)
    db.add(case); db.flush()
    run = SkillEvaluationRun(skill_id=skill.id, skill_code=skill.skill_code, skill_version=version,
        task_type="data_governance", evaluator="REAL_MODEL_EVIDENCE_GATE", status="RUNNING")
    db.add(run); db.flush()
    skill.lifecycle_status = "TESTING"; db.commit()
    outcome = {"passed": False, "instruction_hash": signature, "skill_version": version,
        "event_count": 0, "relation_count": 0, "note": "测试只校验输出契约与原文证据；语义完整性仍需人工审核，未写入正式事实。"}
    try:
        result = semantic_analysis(db, {"key": f"draft-case:{case.id}", "title": row.title, "body": body,
            "market": row.market, "symbol": row.symbol, "stock_name": row.symbol,
            "state": {"evidence_id": f"evaluation-case:{case.id}"}},
            context={"skill_code": skill.skill_code, "skill_version": version, "evaluation_run_id": run.id,
                "reviewer": reviewer}, model_instance=None, instructions=skill.instructions)
        outcome.update(passed=True, event_count=len(result["events"]), relation_count=len(result["relations"]))
    except Exception as exc:
        result = {"model_calls": getattr(exc, "semantic_calls", [])}
        outcome["error"] = str(exc)[:1600]
    db.refresh(skill)
    if skill.version != version or sha256(skill.instructions.encode()).hexdigest() != signature:
        outcome.update(passed=False, error="测试期间Skill发生变化，必须重新测试")
    run.status = "COMPLETED" if outcome["passed"] else "FAILED"
    run.summary_json = outcome; run.completed_at = datetime.now(timezone.utc)
    db.add(SkillEvaluationResult(run_id=run.id, case_id=case.id, output_json=result,
        metrics_json=outcome, passed=outcome["passed"], evidence_refs_json=[{"case_id": case.id}]))
    skill.config_json = {**(skill.config_json or {}), "semantic_draft_test": {**outcome, "evaluation_run_id": run.id}}
    db.commit()
    return draft_read(skill)


def link_draft_agents(db, skill):
    if not (skill.config_json or {}).get("semantic_generated_draft"):
        return
    for agent in db.scalars(select(AgentDefinition).where(AgentDefinition.agent_code.in_(
            ["DOCUMENT_SEMANTIC_GOVERNANCE_AGENT", "STOCK_DATA_GOVERNANCE_AGENT"]))):
        if not db.scalar(select(AgentSkillLink).where(AgentSkillLink.agent_id == agent.id, AgentSkillLink.skill_id == skill.id)):
            db.add(AgentSkillLink(agent_id=agent.id, skill_id=skill.id))


def enable_draft(db, skill, *, reviewer):
    require_test(skill)
    skill.enabled = True; skill.lifecycle_status = "ENABLED"
    skill.config_json = {**(skill.config_json or {}), "enabled_by": reviewer,
        "enabled_at": datetime.now(timezone.utc).isoformat()}
    link_draft_agents(db, skill)
    db.commit()
    return draft_read(skill)
