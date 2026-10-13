"""Draft/test/enable specialist prompts using existing Skill and evaluation stores."""
from copy import deepcopy
from datetime import datetime, timezone
from hashlib import sha256
import json
import re
from uuid import uuid4

from pydantic import BaseModel, ConfigDict, Field
from sqlalchemy import select, update

from app.models.ai_hub import AgentDefinition, AgentSkillLink, ModelCallLog, ModelSkill
from app.models.market_data import StockNews, StockNotice, StockSymbol
from app.models.decision_review import SkillEvaluationCase, SkillEvaluationRun, SkillEvaluationResult
from app.services.document_semantic_planning import WorkflowConflict, _body_for_plan, _load_json, _lock_document
from app.services.document_semantics import Analysis, SemanticModelUnavailable, semantic_analysis
from app.services.model_hub import ModelHubService, save_skill_content

TEST_TASK_TYPE = "document_specialist_skill_test"


class DraftInstructions(BaseModel):
    model_config = ConfigDict(extra="forbid")
    instructions: str = Field(min_length=40, max_length=12000)


class DraftEvaluation(BaseModel):
    model_config = ConfigDict(extra="forbid")
    passed: bool
    score: int = Field(ge=0, le=100)
    summary: str = Field(min_length=1, max_length=1200)
    issues: list[str] = Field(default_factory=list, max_length=12)
    criteria_results: list[str] = Field(default_factory=list, max_length=20)
    improved_instructions: str = Field(default="", max_length=12000)


def _semantic_model_instance(db):
    """Keep draft generation, execution, and review on the configured semantic agent model."""
    agent = db.scalar(select(AgentDefinition).where(
        AgentDefinition.agent_code == "DOCUMENT_SEMANTIC_GOVERNANCE_AGENT",
        AgentDefinition.enabled.is_(True),
    ))
    return agent.model_instance_code if agent else None


def _specialist_family(plan, gap):
    """Return a stable reuse family without letting the model choose an identity."""
    label = " ".join(str(value or "") for value in (
        gap.get("suggested_skill_name"), gap.get("type_name"), gap.get("failure_error"),
        plan.get("primary_type"),
    )).upper()
    types = {plan.get("primary_type"), *(plan.get("secondary_types") or [])}
    if types & {"MARKET_STATISTICS", "SECTOR_MARKET", "SECURITY_MARKET"} and any(
            term in label for term in ("TABLE", "表格", "LIST SHOULD HAVE AT MOST")):
        return "DOCUMENT_MARKET_TABLE_EXTRACTION"
    safe = "_".join(part for part in "".join(
        character if character.isascii() and character.isalnum() else " " for character in label
    ).split() if part)
    identity = json.dumps({"types": sorted(str(value) for value in types if value),
                           "name": gap.get("suggested_skill_name") or gap.get("type_name")}, ensure_ascii=False)
    return ((safe or "DOCUMENT_EVIDENCE_SPECIALIST")[:100] + "_" + sha256(identity.encode()).hexdigest()[:16])


def _find_reusable_draft(db, family):
    candidates = []
    for skill in db.scalars(select(ModelSkill)).all():
        config = skill.config_json or {}
        if not config.get("semantic_generated_draft"):
            continue
        inferred = config.get("specialist_family")
        if not inferred and "TABLE" in f"{skill.skill_code} {skill.skill_name}".upper():
            inferred = "DOCUMENT_MARKET_TABLE_EXTRACTION"
        # An enabled Skill is immutable. A new document must first be tested
        # against its version, rather than inherit another document's pass.
        if inferred == family and not skill.enabled and skill.lifecycle_status != "DEPRECATED":
            candidates.append(skill)
    return max(candidates, key=lambda item: (
        bool(item.enabled),
        bool(((item.config_json or {}).get("semantic_draft_test") or {}).get("passed")),
        item.id,
    ), default=None)


def draft_read(skill, *, db=None):
    result = deepcopy((skill.config_json or {}).get("semantic_draft_test"))
    progress = deepcopy((skill.config_json or {}).get("semantic_test_progress"))
    # Older evaluations hid provider failures behind a generic extraction error.
    # Resolve the referenced audit log for presentation, retaining stored history.
    if db is not None and result and not result.get("passed") and not result.get("failure_kind"):
        error = str(result.get("error") or "")
        if "未取得真实模型" in error:
            identity = re.search(r"模型调用#(\d+)", error)
            call = db.get(ModelCallLog, int(identity[1])) if identity else None
            if call and call.status == "FAILED":
                result.update(failure_kind="MODEL_UNAVAILABLE",
                    error=f"{call.error_message or error}（模型调用#{call.id}）",
                    summary="历史验收因模型服务不可用而未完成，原规则与治理历程已保留；恢复模型后可继续测试。",
                    processed_document_count=len(result.get("document_results") or []))
                if progress and progress.get("status") == "FAILED":
                    progress["message"] = result["summary"]
    if result and (result.get("instruction_hash") != sha256(skill.instructions.encode()).hexdigest()
                   or result.get("skill_version") != skill.version):
        result.update(passed=False, pending_version_test=True)
    if db is not None and result and result.get("passed") and not skill.enabled:
        try:
            require_test(skill, db=db)
        except WorkflowConflict as exc:
            result.update(passed=False, pending_version_test=True, summary=str(exc))
    return {"id": skill.id, "skill_code": skill.skill_code, "skill_name": skill.skill_name,
        "instructions": skill.instructions, "version": skill.version,
        "enabled": skill.enabled, "lifecycle_status": skill.lifecycle_status,
        "test_result": result, "test_progress": progress,
        "test_job_id": (skill.config_json or {}).get("semantic_test_job_id"),
        "iteration_history": (skill.config_json or {}).get("semantic_iteration_history") or []}


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
        return draft_read(existing, db=db)
    body = _body_for_plan(row)
    if plan.get("content_hash") != sha256(body.encode()).hexdigest():
        raise WorkflowConflict("原文已更新，请重新诊断或生成方案后创建专项Skill")
    base = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == "GENERIC_EVIDENCE_FACT_GOVERNOR"))
    if not base:
        raise ValueError("通用证据Skill尚未安装")
    gap = gaps[gap_index]
    family = _specialist_family(plan, gap)
    reusable = _find_reusable_draft(db, family)
    if reusable:
        if ((reusable.config_json or {}).get("semantic_test_progress") or {}).get("status") in {"PENDING", "RUNNING"}:
            raise WorkflowConflict("同类专项Skill正在测试，请待本轮结束后再增加验收文档")
        _lock_document(db, row)
        latest = deepcopy((row.content_json or {}).get("semantic_governance_plan") or {})
        if latest.get("plan_id") != expected_plan_id or latest.get("revision") != plan.get("revision"):
            raise WorkflowConflict("复用专项Skill期间方案发生变化，请刷新后重试")
        updated = deepcopy(latest)
        updated["draft_skill_codes"] = {
            **(latest.get("draft_skill_codes") or {}), str(gap_index): reusable.skill_code,
        }
        row.content_json = {**(row.content_json or {}), "semantic_governance_plan": updated}
        sources = (reusable.config_json or {}).get("validation_sources") or [{
            "source_table": reusable.config_json["draft_source_table"], "record_id": reusable.config_json["draft_record_id"]}]
        source = {"source_table": row.__tablename__, "record_id": row.id}
        if source not in sources:
            reusable.config_json = {**reusable.config_json, "validation_sources": [*sources, source],
                "semantic_draft_test": {**(reusable.config_json.get("semantic_draft_test") or {}), "passed": False,
                    "note": "增加了待治理文档，当前版本必须对全部关联文档重新验收。"}}
        db.commit()
        return {**draft_read(reusable, db=db), "reused": True}
    log = ModelHubService(db).chat(task_type="data_governance",
        instance_code=_semantic_model_instance(db), temperature=0, max_tokens=4000,
        messages=[{"role": "system", "content": "为文档治理能力缺口编写中文专项Skill规则。只输出提示词与操作规范，禁止生成可执行代码、SQL或自动发布指令。"
            "规则必须兼容所给结构化契约，不能用Markdown表格替代JSON，不能猜年份或给净流出自行加负号。"
            "表格按0开始编号，行0为表头，数据行从1开始，列从0开始；指标名称逐字对应表头去掉括号单位后的名称，引用完整原始数据行。"
            "仅治理目标证券及相关市场/板块，不能把全榜单股票当成目标公司。所有抽取均需逐字证据、主体边界、数值单位核验，区分事实/观点/推断，不确定项请求用户确认。只返回契约JSON：" + json.dumps(DraftInstructions.model_json_schema(), ensure_ascii=False)},
            {"role": "user", "content": json.dumps({"gap": gap, "expected_knowledge": plan.get("expected_knowledge"),
                "target_scopes": plan.get("target_scopes"), "title": row.title, "body": body[:12000],
                "extraction_schema": Analysis.model_json_schema()}, ensure_ascii=False)}],
        metadata_json={"purpose": "DOCUMENT_SPECIALIST_DRAFT", "require_real_model": True,
            "json_schema_output": DraftInstructions.model_json_schema(),
            "json_schema_name": "document_specialist_instructions",
            "native_structured_output": False,
            "governance_plan_id": expected_plan_id, "reviewer": reviewer})
    if log.status != "SUCCESS" or str(log.provider_code).upper() == "MOCK":
        raise SemanticModelUnavailable(f"Skill草稿生成：{getattr(log, 'error_message', None) or '未取得真实模型输出'}（模型调用#{log.id}）")
    if any(choice.get("finish_reason") == "length" for choice in (getattr(log, "response_json", None) or {}).get("choices", [])):
        raise ValueError(f"专项Skill草稿输出被截断，请重试（模型调用#{log.id}）")
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
            "specialist_family": family,
            "draft_source_table": row.__tablename__, "draft_record_id": row.id,
            "validation_sources": [{"source_table": row.__tablename__, "record_id": row.id}],
            "created_by": reviewer, "draft_model_call_id": log.id,
            "acceptance_criteria": gap.get("acceptance_criteria") or [],
            "source_failure_error": gap.get("failure_error"),
            "diagnosis_model_call_id": gap.get("diagnosis_model_call_id")})
    skill.input_contract_json["properties"]["skill_code"] = {"const": code}
    skill.config_json["function_spec"]["parameters"] = deepcopy(skill.input_contract_json)
    db.add(skill); db.flush()
    save_skill_content(db, skill, skill.instructions)
    updated = deepcopy(latest)
    updated["draft_skill_codes"] = {**(latest.get("draft_skill_codes") or {}), str(gap_index): code}
    row.content_json = {**(row.content_json or {}), "semantic_governance_plan": updated}
    db.commit()
    return draft_read(skill)


def require_test(skill, *, instructions=None, config=None, db=None):
    if not (skill.config_json or {}).get("semantic_generated_draft"):
        return
    result = (skill.config_json or {}).get("semantic_draft_test") or {}
    signature = sha256((instructions if instructions is not None else skill.instructions).encode()).hexdigest()
    if not result.get("passed") or result.get("instruction_hash") != signature or result.get("skill_version") != skill.version:
        raise WorkflowConflict("生成的专项Skill必须通过当前版本的真实模型与证据契约测试后才能启用")
    if config is not None and not config.get("semantic_generated_draft"):
        raise WorkflowConflict("生成Skill的测试门禁标记不能移除")
    if ((skill.config_json or {}).get("semantic_test_progress") or {}).get("status") in {"PENDING", "RUNNING"}:
        raise WorkflowConflict("专项Skill测试仍在执行，不能启用")
    if db is not None:
        from app.services.document_event_governance import document_body
        sources = (config if config is not None else skill.config_json).get("validation_sources") or []
        signatures = result.get("source_signatures") or []
        if {(s["source_table"], s["record_id"]) for s in sources} != {
                (s["source_table"], s["record_id"]) for s in signatures} or not signatures:
            raise WorkflowConflict("验收文档范围已改变，必须重新测试专项Skill")
        for signature in signatures:
            model = {"stock_news": StockNews, "stock_notice": StockNotice}.get(signature["source_table"])
            row = db.get(model, signature["record_id"]) if model else None
            plan = (row.content_json or {}).get("semantic_governance_plan") or {} if row else {}
            if not row or signature != _sample_signature(row, document_body(row), plan):
                raise WorkflowConflict("验收文档正文或治理方案已改变，必须重新测试专项Skill")


def _sample_signature(row, body, plan):
    return {"source_table": row.__tablename__, "record_id": row.id,
        "content_hash": sha256(body.encode()).hexdigest(), "plan_id": plan.get("plan_id"),
        "revision": plan.get("revision"), "target_scopes": plan.get("target_scopes") or [],
        "expected_knowledge": plan.get("expected_knowledge") or []}


def _evaluation(db, skill, row, *, reviewer, extraction, extraction_error, iteration, body=None):
    """Use a second real-model call to judge the Skill against frozen evidence."""
    body = body if body is not None else _body_for_plan(row)
    criteria = (skill.config_json or {}).get("acceptance_criteria") or [
        "输出符合结构契约", "所有引文可在原文逐字定位", "数值、符号和单位与原文一致",
        "主体边界正确", "事实、观点和模型推断严格区分",
    ]
    prompt = (
        "你是独立的证券文档Skill质量评估智能体。根据冻结原文、执行结果或错误、当前Skill规则和验收标准，"
        "判断该Skill是否足以稳定治理同类文档。不能因为输出JSON可解析就判定通过。"
        "数值单位、表格列、主体或证据门禁任一失败必须判定不通过。"
        "extraction包含实际抽取内容或被契约拒绝的真实原始输出，不能只凭报错臆测。对照全部目标预期知识逐项检查缺失，"
        "关键数值被删除或全体事件为空以绕过门禁时不得通过。未给完整年月日时event_date必须为空；"
        "原文净流出没有负号时raw_value不能添加负号。表格的逐字数据行和程序绑定的表头证据一起核验，不能因其分开保存就判定无证据。"
        "特别注意：‘9月30日’不含年份，不是完整日期；即使发布日期含2026年也不得要求event_date补写2026-09-30。"
        "‘净流出10.68亿元’的raw_value必须是10.68，净流出通过指标名称表达；只有原文数字带负号才保留负号。"
        "成功extraction已通过程序校验并补充normalized_value、normalized_unit、direction、证据位置等派生字段，"
        "这些不是模型凭空新增字段，不能据此否决。schema用于模型输入，extraction是经过程序扩展的成果，勿将两者混为一谈。"
        "允许提取明确标记MODEL_INFERENCE的分析影响和关系，但不得冒充REPORTED_FACT或把个股方向推为未来涨跌。"
        "不要要求目标之外全部榜单股票都形成事件；只核验目标股票和其相关板块、市场，并按expected_knowledge检查实际覆盖。"
        "如不通过，improved_instructions返回可直接替换专项规则的完整中文提示词；不得生成代码、SQL、任意写库或自动发布。"
        "改进规则必须兼容extraction_schema，仍输出契约JSON，不可改成Markdown表格。指标名称须对应原始表头，不能猜单位和日期。"
        "如通过，improved_instructions留空。只返回契约JSON。\n契约：" +
        json.dumps(DraftEvaluation.model_json_schema(), ensure_ascii=False)
    )
    log = ModelHubService(db).chat(
        task_type="data_governance", instance_code=_semantic_model_instance(db),
        temperature=0, max_tokens=5000,
        messages=[{"role": "system", "content": prompt}, {"role": "user", "content": json.dumps({
            "title": row.title, "body": body, "skill_code": skill.skill_code,
            "skill_version": skill.version, "instructions": skill.instructions,
            "acceptance_criteria": criteria, "extraction": extraction,
            "governance_plan": (row.content_json or {}).get("semantic_governance_plan"),
            "extraction_schema": Analysis.model_json_schema(),
            "extraction_error": extraction_error, "iteration": iteration,
        }, ensure_ascii=False)}],
        metadata_json={"purpose": "DOCUMENT_SPECIALIST_EVALUATION", "phase": "SKILL_QUALITY_REVIEW",
                       "require_real_model": True, "source_table": row.__tablename__,
                       "source_record_id": row.id, "skill_code": skill.skill_code,
                       "skill_version": skill.version, "reviewer": reviewer, "iteration": iteration,
                       "json_schema_output": DraftEvaluation.model_json_schema(),
                       "json_schema_name": "document_specialist_evaluation",
                       "native_structured_output": False},
    )
    if log.status != "SUCCESS" or str(log.provider_code).upper() == "MOCK" or not str(log.response_text or "").strip():
        raise SemanticModelUnavailable(f"Skill质量复核：{getattr(log, 'error_message', None) or '未取得真实模型输出'}（模型调用#{log.id}）")
    if any(choice.get("finish_reason") == "length" for choice in (getattr(log, "response_json", None) or {}).get("choices", [])):
        raise ValueError(f"专项Skill复核输出被截断，请重试（模型调用#{log.id}）")
    result = DraftEvaluation.model_validate(_load_json(log.response_text)).model_dump()
    if extraction_error:
        result["passed"] = False
        result["score"] = min(result["score"], 59)
    result["model_call_id"] = log.id
    return result


def _revise_for_samples(db, skill, samples, reviews, *, reviewer, iteration):
    """Reconcile all sample reviews against evidence before changing shared rules."""
    log = ModelHubService(db).chat(
        task_type="data_governance", instance_code=_semantic_model_instance(db),
        temperature=0, max_tokens=5000,
        messages=[{"role": "system", "content":
            "你是证券文档专项Skill改进智能体。综合全部样本的冻结原文、治理范围、验收结果和当前规则，"
            "生成可替换专项规则的完整中文instructions。评审也可能错误，必须对照原文和契约，不能盲目照抄第一篇建议。"
            "保持JSON抽取契约，禁止生成SQL、可执行代码、任意写库或自动发布。"
            "只抽取目标证券及相关市场板块；不要求无关榜单公司逐一形成事件，每个事件只有一个主体。"
            "表格使用从0开始的表号、行号和列号，行0为表头，引用完整数据行，指标名对应表头去除括号单位。"
            "数值、符号、单位逐字保留：净流出10.68亿元的raw_value为10.68，不能自行加负号，表格已有负号必须保留。"
            "9月30日没有年份，event_date必须为空，不得从发布日期补年份。"
            "程序补充的normalized_value、normalized_unit和证据位置不是模型幻觉；不得要求模型输出程序派生字段。"
            "区分转述事实、作者观点、标注的MODEL_INFERENCE；不能删关键指标或返回空事件规避证据门禁。"
            "保留成功样本已满足的规则，修复失败样本。只返回契约JSON：" +
            json.dumps(DraftInstructions.model_json_schema(), ensure_ascii=False)},
            {"role": "user", "content": json.dumps({
                "skill_code": skill.skill_code, "skill_version": skill.version,
                "instructions": skill.instructions, "extraction_schema": Analysis.model_json_schema(),
                "acceptance_criteria": (skill.config_json or {}).get("acceptance_criteria") or [],
                "reviews": reviews,
                "samples": [{"source_table": target.__tablename__, "record_id": target.id,
                    "title": target.title, "body": body, "target_scopes": plan.get("target_scopes"),
                    "expected_knowledge": plan.get("expected_knowledge")}
                    for target, body, plan, stock_name, case in samples],
            }, ensure_ascii=False)}],
        metadata_json={"purpose": "DOCUMENT_SPECIALIST_REVISION", "require_real_model": True,
            "json_schema_output": DraftInstructions.model_json_schema(),
            "json_schema_name": "document_specialist_revision", "native_structured_output": False,
            "skill_code": skill.skill_code, "skill_version": skill.version,
            "reviewer": reviewer, "iteration": iteration})
    if log.status != "SUCCESS" or str(log.provider_code).upper() == "MOCK" or not str(log.response_text or "").strip():
        raise SemanticModelUnavailable(f"Skill综合改进：{getattr(log, 'error_message', None) or '未取得真实模型输出'}（模型调用#{log.id}）")
    if any(choice.get("finish_reason") == "length" for choice in (getattr(log, "response_json", None) or {}).get("choices", [])):
        raise ValueError(f"专项Skill改进输出被截断，原规则已保留（模型调用#{log.id}）")
    return DraftInstructions.model_validate(_load_json(log.response_text)).instructions, log.id


def test_draft(db, skill, row, *, reviewer, max_iterations: int = 3):
    if not (skill.config_json or {}).get("semantic_generated_draft"):
        raise ValueError("该Skill不是专项治理草稿")
    if skill.enabled:
        raise WorkflowConflict("请先将Skill转为测试状态")
    config = deepcopy(skill.config_json or {})
    active = config.get("semantic_test_progress") or {}
    if active.get("status") == "RUNNING":
        raise WorkflowConflict("该Skill已经在测试，请查看当前进度，不要重复启动")
    sources = list(config.get("validation_sources") or [])
    current_source = {"source_table": row.__tablename__, "record_id": row.id}
    if current_source not in sources:
        sources.append(current_source)
    samples, frozen = [], []
    for source in sources:
        model = {"stock_news": StockNews, "stock_notice": StockNotice}.get(source["source_table"])
        target = db.get(model, source["record_id"]) if model else None
        if not target:
            raise ValueError("专项Skill关联的验收文档不存在，不能标记测试通过")
        body = _body_for_plan(target)
        plan = deepcopy((target.content_json or {}).get("semantic_governance_plan") or {})
        if plan.get("content_hash") != sha256(body.encode()).hexdigest():
            raise WorkflowConflict(f"文档《{target.title}》原文已更新，请重新诊断或生成方案后再测试")
        frozen.append((target, body, plan))
    # Full-text retrieval happens before taking the database write lock.
    changed = db.execute(update(ModelSkill).where(ModelSkill.id == skill.id,
        ModelSkill.config_json == config).values(config_json=config).execution_options(synchronize_session=False))
    if changed.rowcount != 1:
        raise WorkflowConflict("专项Skill状态已变化，请刷新后重试")
    source_signatures = [_sample_signature(target, body, plan) for target, body, plan in frozen]
    for target, body, plan in frozen:
        # The draft replaces the selected rules only inside this offline test.
        # The confirmed production plan and its signatures stay unchanged.
        plan["recommended_skill_codes"] = []
        stock = db.scalar(select(StockSymbol).where(StockSymbol.market == target.market, StockSymbol.symbol == target.symbol))
        case = SkillEvaluationCase(case_code="DOC_DRAFT_" + uuid4().hex, task_type="data_governance",
            description="冻结原文的专项Skill证据与契约测试",
            input_json={"source_table": target.__tablename__, "record_id": target.id, "body": body, "title": target.title,
                "content_hash": sha256(body.encode()).hexdigest(), "plan_id": plan.get("plan_id")},
            expected_json={"must_pass_evidence_contract": True, "expected_knowledge": plan.get("expected_knowledge")}, source="SYSTEM", enabled=True)
        db.add(case); db.flush()
        samples.append((target, body, plan, stock.name if stock else target.symbol, case))
    original_version = skill.version
    run = SkillEvaluationRun(skill_id=skill.id, skill_code=skill.skill_code, skill_version=skill.version,
        task_type="data_governance", evaluator="REAL_MODEL_SKILL_LOOP", status="RUNNING")
    db.add(run); db.flush()
    skill.lifecycle_status = "TESTING"
    skill.config_json = {**config, "validation_sources": sources,
        "semantic_test_progress": {"status": "RUNNING", "run_id": run.id, "phase": "STARTING", "message": "准备冻结原文验收"},
        "semantic_draft_test": {**(config.get("semantic_draft_test") or {}), "passed": False}}
    db.commit()
    def progress(phase, iteration, sample_index):
        labels = {"SEMANTIC_READING": "LLM阅读全文", "STRUCTURED_EXTRACTION": "专项Skill抽取知识",
            "STRUCTURED_EXTRACTION_REPAIR": "按证据报错修正", "SKILL_QUALITY_REVIEW": "独立LLM复核质量",
            "SKILL_RULE_REVISION": "综合全部文档改进Skill规则"}
        skill.config_json = {**(skill.config_json or {}), "semantic_test_progress": {
            "status": "RUNNING", "run_id": run.id, "phase": phase, "iteration": iteration,
            "sample_index": sample_index + 1, "sample_count": len(samples),
            "message": f"第{iteration}轮 · 文档{sample_index + 1}/{len(samples)} · {labels.get(phase, phase)}"}}
        db.commit()
    history, result, outcome = [], {}, {"passed": False, "event_count": 0, "relation_count": 0,
        "note": "Skill执行后由独立LLM按冻结原文和证据契约复核；测试不写入正式事实，启用仍需人工审核。"}
    iteration_limit = max(1, min(max_iterations, 5))
    for iteration in range(1, iteration_limit + 1):
        tested_version = skill.version
        tested_hash = sha256(skill.instructions.encode()).hexdigest()
        document_results, unavailable = [], False
        for sample_index, (target, body, plan, stock_name, case) in enumerate(samples):
            extraction, extraction_error = None, None
            try:
                extraction = semantic_analysis(db, {"key": f"draft-case:{case.id}:{iteration}", "title": target.title, "body": body,
                    "market": target.market, "symbol": target.symbol, "stock_name": stock_name, "governance_plan": plan,
                    "_progress_callback": lambda phase: progress(phase, iteration, sample_index),
                    "state": {"evidence_id": f"evaluation-case:{case.id}"}},
                    context={"skill_code": skill.skill_code, "skill_version": tested_version,
                        "evaluation_run_id": run.id, "reviewer": reviewer, "iteration": iteration},
                    model_instance=_semantic_model_instance(db), instructions=skill.instructions)
            except Exception as exc:
                extraction_error = str(exc)[:1600]
                unavailable = isinstance(exc, SemanticModelUnavailable)
                extraction = {"model_calls": getattr(exc, "semantic_calls", []),
                    "rejected_output": getattr(exc, "semantic_invalid_output", None),
                    "reading": getattr(exc, "semantic_reading", None)}
            progress("SKILL_QUALITY_REVIEW", iteration, sample_index)
            try:
                if unavailable:
                    raise SemanticModelUnavailable(extraction_error)
                review = _evaluation(db, skill, target, reviewer=reviewer, extraction=extraction,
                                     extraction_error=extraction_error, iteration=iteration, body=body)
            except Exception as exc:
                unavailable = isinstance(exc, SemanticModelUnavailable)
                review = {"passed": False, "score": 0, "summary": "独立LLM质量复核失败",
                          "issues": [str(exc)[:1600]], "criteria_results": [],
                          "improved_instructions": "", "model_call_id": getattr(exc, "call_log_id", None)}
            item = {"iteration": iteration, "run_id": run.id, "source_table": target.__tablename__, "record_id": target.id,
                "title": target.title, "content_hash": case.input_json["content_hash"],
                "skill_version": tested_version, "instruction_hash": tested_hash,
                "extraction_error": extraction_error, "evaluation": review,
                "event_count": len((extraction or {}).get("events") or []),
                "relation_count": len((extraction or {}).get("relations") or [])}
            history.append(item); document_results.append(item)
            result[str(case.id)] = extraction or {}
            skill.config_json = {**skill.config_json, "semantic_iteration_history": [
                *((config.get("semantic_iteration_history") or [])), *history]}
            db.commit()
            if unavailable:
                break
        failed = [item for item in document_results if item["extraction_error"] or not item["evaluation"]["passed"]]
        outcome.update(score=min(item["evaluation"]["score"] for item in document_results),
            event_count=sum(item["event_count"] for item in document_results), relation_count=sum(item["relation_count"] for item in document_results),
            document_results=document_results, document_count=len(samples), iterations=iteration,
            instruction_hash=tested_hash, skill_version=tested_version)
        if not failed:
            outcome.update(passed=True, summary=f"全部{len(samples)}篇验收文档通过独立LLM复核和证据契约。")
            break
        first = failed[0]
        outcome.update(error=first["extraction_error"] or first["evaluation"]["summary"],
            summary=f"{len(failed)}/{len(samples)}篇文档未通过；" + first["evaluation"]["summary"])
        if unavailable:
            outcome.update(failure_kind="MODEL_UNAVAILABLE", error=review["issues"][0],
                processed_document_count=len(document_results),
                summary="模型服务不可用，已停止本轮并保留进度和草稿；恢复模型后可继续测试，未处理文档没有标记完成。")
            break
        if len(samples) > 1:
            progress("SKILL_RULE_REVISION", iteration, len(samples) - 1)
            try:
                improved, call_id = _revise_for_samples(db, skill, samples, document_results,
                    reviewer=reviewer, iteration=iteration)
                outcome["revision_model_call_ids"] = [*(outcome.get("revision_model_call_ids") or []), call_id]
            except Exception as exc:
                outcome.update(error=str(exc)[:1600],
                    failure_kind="MODEL_UNAVAILABLE" if isinstance(exc, SemanticModelUnavailable) else "REVISION_FAILED",
                    summary="Skill综合改进未完成，原规则与全部样本结果已保留，可恢复后继续测试。")
                break
        else:
            improved = next((str(item["evaluation"].get("improved_instructions") or "").strip() for item in failed
                             if len(str(item["evaluation"].get("improved_instructions") or "").strip()) >= 40), "")
        if not improved:
            break
        next_instructions = skill.instructions.split("\n\n## 专项语义规则\n", 1)[0] + "\n\n## 专项语义规则\n" + improved
        save_skill_content(db, skill, next_instructions, source="LLM_EVALUATION")
        skill.lifecycle_status = "TESTING"
        db.commit(); db.refresh(skill)
        if iteration >= iteration_limit:
            outcome["pending_version_test"] = skill.version != tested_version
            break
    outcome.update(current_skill_version=skill.version, original_skill_version=original_version,
        iteration_history=history, source_signatures=source_signatures)
    run.status = "COMPLETED" if outcome["passed"] else "FAILED"
    run.summary_json = outcome; run.completed_at = datetime.now(timezone.utc)
    run.skill_version = outcome["skill_version"]
    for target, body, plan, stock_name, case in samples:
        item = next((item for item in outcome["document_results"] if item["record_id"] == target.id and item["source_table"] == target.__tablename__), None)
        if not item:
            continue
        db.add(SkillEvaluationResult(run_id=run.id, case_id=case.id, output_json=result[str(case.id)],
            metrics_json=item, passed=not item["extraction_error"] and item["evaluation"]["passed"], evidence_refs_json=[{"case_id": case.id}]))
    skill.config_json = {**(skill.config_json or {}),
        "semantic_draft_test": {**outcome, "evaluation_run_id": run.id},
        "semantic_test_progress": {"status": run.status, "run_id": run.id, "message": outcome["summary"]},
        "semantic_iteration_history": [*(config.get("semantic_iteration_history") or []), *history]}
    db.commit()
    return draft_read(skill)


def link_draft_agents(db, skill):
    if not (skill.config_json or {}).get("semantic_generated_draft"):
        return
    for agent in db.scalars(select(AgentDefinition).where(AgentDefinition.agent_code.in_(
            ["DOCUMENT_SEMANTIC_GOVERNANCE_AGENT", "STOCK_DATA_GOVERNANCE_AGENT"]))):
        if not db.scalar(select(AgentSkillLink).where(AgentSkillLink.agent_id == agent.id, AgentSkillLink.skill_id == skill.id)):
            db.add(AgentSkillLink(agent_id=agent.id, skill_id=skill.id))


def test_job_read(db, job):
    from app.models.pipeline import PipelineRun
    run = db.get(PipelineRun, job.pipeline_run_id)
    skill = db.get(ModelSkill, job.payload_json["skill_id"])
    draft = draft_read(skill, db=db) if skill else None
    # Legacy workers marked a finished test as COMPLETED even when acceptance
    # failed. Present that stored outcome accurately without rewriting history.
    result = (draft or {}).get("test_result") or {}
    legacy_failed = (job.status == "COMPLETED" and (skill.config_json or {}).get("semantic_test_job_id") == job.id
                     and bool(result) and not result.get("passed")) if skill else False
    return {"job_id": job.id, "status": "FAILED" if legacy_failed else job.status,
        "queue_status": job.status, "error": job.error_message or (result.get("error") if legacy_failed else None),
        "result": (run.output_json or {}).get("draft") if run else None,
        "draft": draft}


def submit_test_job(db, skill, *, reviewer):
    from app.jobs.dispatcher import DatabaseJobDispatcher
    from app.models.pipeline import ScheduledJob
    if skill.enabled:
        raise WorkflowConflict("已启用Skill不能原位修改，请先转为测试状态")
    # Lock the Skill configuration before checking/enqueueing to prevent two
    # windows from launching parallel optimization of the same version.
    config = deepcopy(skill.config_json or {})
    changed = db.execute(update(ModelSkill).where(ModelSkill.id == skill.id, ModelSkill.config_json == config)
        .values(config_json=config).execution_options(synchronize_session=False))
    if changed.rowcount != 1:
        raise WorkflowConflict("Skill状态已变化，请刷新")
    active = db.get(ScheduledJob, config.get("semantic_test_job_id")) if config.get("semantic_test_job_id") else None
    if active and active.status in {"PENDING", "RUNNING", "RETRY"}:
        db.commit()
        return test_job_read(db, active)
    job = DatabaseJobDispatcher(db).enqueue(task_type=TEST_TASK_TYPE,
        pipeline_type="DOCUMENT_SPECIALIST_SKILL_EVALUATION", trigger_type="API",
        idempotency_key=f"{TEST_TASK_TYPE}:{uuid4().hex}", max_attempts=1,
        payload={"skill_id": skill.id, "skill_version": skill.version, "reviewer": reviewer})
    job.payload_json = {**job.payload_json, "job_id": job.id}
    skill.config_json = {**config, "semantic_test_job_id": job.id,
        "semantic_test_progress": {"status": "PENDING", "message": "测试已进入后台队列，关闭窗口后仍会执行"}}
    db.commit()
    from app.jobs.governance_runner import wake_governance_runner
    wake_governance_runner()
    return test_job_read(db, job)


def execute_test_job(payload):
    from app.db.session import SessionLocal
    with SessionLocal() as db:
        skill = db.get(ModelSkill, payload["skill_id"])
        if not skill or skill.enabled:
            raise WorkflowConflict("专项Skill已不存在或已启用，无法执行测试")
        config = deepcopy(skill.config_json or {})
        if payload.get("job_id") != config.get("semantic_test_job_id"):
            raise WorkflowConflict("该测试任务已被替代，不能覆盖新任务状态")
        try:
            if payload.get("skill_version") != skill.version:
                raise WorkflowConflict("排队期间Skill版本已变化，请重新提交测试")
            progress = config.get("semantic_test_progress") or {}
            if progress.get("status") == "RUNNING":
                # A recovered queue lease means the previous worker stopped;
                # retain its audit before starting a separate evaluation.
                run = db.get(SkillEvaluationRun, progress.get("run_id"))
                if run and run.status == "RUNNING":
                    run.status = "INTERRUPTED"
                    run.completed_at = datetime.now(timezone.utc)
                skill.config_json = {**config, "semantic_test_progress": {"status": "INTERRUPTED"}}
                db.commit()
            model = {"stock_news": StockNews, "stock_notice": StockNotice}[config["draft_source_table"]]
            row = db.get(model, config["draft_record_id"])
            if not row:
                raise ValueError("专项Skill关联文档不存在，不能执行验收")
            result = test_draft(db, skill, row, reviewer=payload["reviewer"])
            if not (result.get("test_result") or {}).get("passed"):
                raise ValueError((result.get("test_result") or {}).get("error") or "专项Skill未通过本轮全部文档验收，已保留优化版本与历史")
            return {"draft": result}
        except Exception as exc:
            db.rollback(); db.refresh(skill)
            progress = (skill.config_json or {}).get("semantic_test_progress") or {}
            run = db.get(SkillEvaluationRun, progress.get("run_id")) if progress.get("run_id") else None
            if run and run.status == "RUNNING":
                run.status = "FAILED"; run.completed_at = datetime.now(timezone.utc)
                run.summary_json = {"passed": False, "error": str(exc)[:1600]}
            skill.config_json = {**(skill.config_json or {}),
                "semantic_test_progress": {**progress, "status": "FAILED", "message": str(exc)[:1600]}}
            db.commit()
            raise


def enable_draft(db, skill, *, reviewer):
    require_test(skill, db=db)
    skill.enabled = True; skill.lifecycle_status = "ENABLED"
    skill.config_json = {**(skill.config_json or {}), "enabled_by": reviewer,
        "enabled_at": datetime.now(timezone.utc).isoformat()}
    link_draft_agents(db, skill)
    db.commit()
    return draft_read(skill)
