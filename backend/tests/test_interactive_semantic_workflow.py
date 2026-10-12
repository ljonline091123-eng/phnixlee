"""Isolated workflow integration tests. Model outputs here are deterministic fixtures."""
from copy import deepcopy
import json
from types import SimpleNamespace

import pytest
from sqlalchemy import select, func

from app.models.ai_hub import ModelSkill, AgentDefinition, AgentSkillLink
from app.models.foundation import FoundationFact
from app.models.governance import SkillExecutionRun
from app.models.market_data import StockNews
from app.models.pipeline import ScheduledJob
from app.services import document_semantics as semantics, document_event_governance as governor
from app.services import document_semantic_planning as planning, document_semantic_skills as specialist
from app.services import stock_batch
from app.services.resource_hub import seed_default_agents, seed_default_data_assets, seed_default_knowledge_bases
from app.jobs.worker import JobWorker
from tests.test_document_semantics import BODY, database, payload, setup_document
from tests.test_semantic_batch_execution import api_client


def routing():
    return {"overview": "计提预计负债及终止定增，需要核对作者观点。", "primary_type": "FINANCIAL_DISCLOSURE",
        "secondary_types": ["CAPITAL_OPERATION", "RESEARCH_OPINION"], "relevance": "DIRECT",
        "target_scopes": ["COMPANY"], "rationale": "原文披露金额及公司事项，同时包含作者评价。",
        "expected_knowledge": ["负债计提与利润影响", "终止定增", "关联认购方"],
        "uncertainties": [], "user_confirmation_questions": [], "capability_gaps": [],
        "recommended_skill_codes": ["FINANCIAL_DISCLOSURE_GOVERNOR", "CAPITAL_OPERATION_GOVERNOR"]}


def interactive_payload():
    reading, extraction = payload()
    for event in extraction["events"]:
        event["subject_name"] = "测试公司"
        event["evidence_quotes"].append("测试公司基于税务争议计提预计负债")
    return reading, extraction


def model_fixture(self, **kwargs):
    phase = kwargs.get("metadata_json", {}).get("phase")
    reading, extraction = interactive_payload()
    user = json.loads(kwargs["messages"][-1]["content"]) if phase else {}
    if phase == "TYPE_AND_SKILL_ROUTING":
        value = routing()
    elif kwargs.get("metadata_json", {}).get("purpose") == "DOCUMENT_SPECIALIST_DRAFT":
        value = {"instructions": "逐字阅读原文，识别预计负债的原因、金额、时间和利润影响。区分真实已发生事实、作者观点与模型推断，所有字段附逐字原文证据；不能推断股票必然上涨下跌。"}
    elif phase == "SEMANTIC_READING":
        # A feedback round must still read the full original before patching.
        # Mixing patch instructions into this phase caused live models to
        # return events instead of the required reading contract.
        assert "review_instruction" not in user
        assert "previous_analysis" not in user and "correction_feedback" not in user
        if user.get("human_review_feedback"):
            assert "reading_instruction" in user
        assert "阅读契约" in kwargs["messages"][0]["content"]
        value = reading
    elif phase == "STRUCTURED_EXTRACTION" and user.get("correction_feedback"):
        assert {item["key"] for item in user["previous_analysis"]["events"]} == {
            item["key"] for item in user["correction_feedback"] if item["point_type"] == "EVENT"}
        assert all("fact_id" not in item for item in user["previous_analysis"]["events"])
        assert "protected_knowledge" in user and "JSON补丁" in kwargs["messages"][0]["content"]
        value = {"participants": [], "events": [], "relations": [], "removed_event_keys": [], "removed_relation_keys": [], "uncertainties": []}
        for issue in user["correction_feedback"]:
            if issue["point_type"] == "EVENT":
                event = next(e for e in user["previous_analysis"]["events"] if e["key"] == issue["key"])
                clean = {key: deepcopy(event[key]) for key in semantics.Event.model_fields if key in event}
                clean["metrics"] = [{key: m[key] for key in semantics.Metric.model_fields if key in m} for m in event["metrics"]]
                clean["impacts"] = [{key: m[key] for key in semantics.Impact.model_fields if key in m} for m in event["impacts"]]
                clean["summary"] += "（本轮已按意见核对预计性质）"
                value["events"].append(clean)
    else:
        value = extraction
    return SimpleNamespace(id=100, status="SUCCESS", provider_code="FIXTURE", instance_code="fixture",
        model_code="fixture-model", response_text=json.dumps(value, ensure_ascii=False) if phase or isinstance(value, dict) else str(value))


def ready(database, monkeypatch):
    monkeypatch.setattr(semantics.ModelHubService, "chat", model_fixture)
    monkeypatch.setattr("app.jobs.governance_runner.wake_governance_runner", lambda: None)
    monkeypatch.setattr(stock_batch, "SessionLocal", database)
    with database() as db:
        seed_default_data_assets(db); seed_default_knowledge_bases(db); seed_default_agents(db)
        stock, news = setup_document(db)
        return stock.id, news.id


def confirm(db, row):
    plan = planning.generate_plan(db, row)
    return planning.decide_plan(db, row, decision="CONFIRM", primary_type=plan["primary_type"],
        secondary_types=plan["secondary_types"], skill_codes=plan["recommended_skill_codes"],
        target_scopes=plan["target_scopes"], reviewer="隔离验收员", reason="核对类型和归属",
        expected_plan_id=plan["plan_id"], expected_revision=plan["revision"])


def extracted(database, monkeypatch):
    stock_id, news_id = ready(database, monkeypatch)
    with database() as db:
        from app.models.market_data import StockSymbol
        news = db.get(StockNews, news_id); confirm(db, news)
        run = governor.govern_stock_documents(db, db.get(StockSymbol, stock_id), semantic=True,
            context={"skill_version": "9.0.0", "interactive_plan_required": True},
            document_targets=[{"source_table": "stock_news", "record_id": news_id}])
        assert run["completed"] == 1, run
    return news_id


def review_payload(detail, **extra):
    return {"version_index": detail["selected_version"], "expected_version_key": detail["result_report"]["version_key"],
        "expected_plan_id": detail["governance_plan"]["plan_id"], "reviewer": "隔离验收员", **extra}


@pytest.mark.parametrize("metric, expected", [
    ({"name": "经营现金流", "raw_value": "-2.65", "unit": "亿元", "qualifier": "EXACT", "period": "2026年中期"},
     "经营现金流：-2.65亿元（2026年中期）"),
    ({"name": "质押借款", "raw_value": "2.4", "unit": "亿元", "qualifier": "UPPER_BOUND", "period": "认购资金计划"},
     "质押借款：不超过2.4亿元（认购资金计划）"),
    ({"name": "滞纳金比例", "raw_value": "万分之五", "unit": "%", "normalized_value": "0.0500", "normalized_unit": "%", "period": "每日"},
     "滞纳金比例：万分之五（0.05%）（每日）"),
    ({"name": "金额", "raw_value": "1.07", "unit": "亿元", "qualifier": "ESTIMATE"}, "金额：预计1.07亿元"),
    ({"name": "金额", "raw_value": "1.07", "unit": "亿元", "qualifier": "APPROXIMATE"}, "金额：约1.07亿元"),
    ({"name": "金额", "raw_value": "1.07", "unit": "亿元", "qualifier": "LOWER_BOUND"}, "金额：不少于1.07亿元"),
])
def test_report_metrics_preserve_sign_scope_and_original_fraction(metric, expected):
    assert planning._report_metric(metric) == expected


def test_subject_evidence_candidates_are_original_and_do_not_invent_names():
    body = "首段介绍测试公司。\n随后公司终止定增，原因需要结合本段和首段核对。"
    candidates = semantics._subject_evidence_candidates(body, ["测试公司", "测试公司", "原文没有的主体"])
    assert len(candidates) == 1
    assert candidates[0]["subject_name"] == "测试公司"
    assert candidates[0]["evidence_quote"] in body


def test_correction_model_context_hides_storage_fields_and_protects_other_knowledge():
    reading, extraction = payload()
    previous = semantics.validate_analysis(semantics.Reading.model_validate(reading), semantics.Analysis.model_validate(extraction), BODY)
    previous["events"][0]["fact_id"] = "original-event-fact"
    previous["relations"][0]["fact_id"] = "original-relation-fact"
    before = deepcopy(previous)
    selected, protected = semantics._correction_model_context(previous, [
        {"point_type": "EVENT", "key": "provision"}, {"point_type": "RELATION", "key": "original-relation-fact"}])
    assert [item["key"] for item in selected["events"]] == ["provision"]
    assert "fact_id" not in selected["events"][0] and "direction" not in selected["events"][0]
    assert "normalized_value" not in selected["events"][0]["metrics"][0]
    assert selected["relations"][0]["correction_key"] == "original-relation-fact"
    assert "fact_id" not in selected["relations"][0]
    assert {item["key"] for item in protected["events"]} == {"offering", "opinion"}
    selected["events"][0]["summary"] = "模型上下文修改不得污染原始知识"
    assert previous == before


def test_durable_plan_skill_execution_three_corrections_and_publish(database, monkeypatch):
    _, news_id = ready(database, monkeypatch)
    worker = JobWorker(task_types=("stock_batch_governance",), session_factory=database,
        task_executor=lambda task, data: stock_batch.execute_stock_batch_governance(data))
    base = f"/document-governance/documents/stock_news/{news_id}"
    target = {"source_table": "stock_news", "record_id": news_id}
    with api_client(database) as client:
        response = client.post("/document-governance/plan-batch", json={"documents": [target], "idempotency_key": "plan"})
        assert response.status_code == 202, response.text
        assert worker.run_once()
        with database() as db:
            job = db.get(ScheduledJob, response.json()["job_id"])
            assert job.status == "COMPLETED", job.error_message
            assert db.scalar(select(func.count()).select_from(FoundationFact)) == 0
        detail = client.get(base).json(); plan = detail["governance_plan"]
        assert detail["workflow_status"] == "NEEDS_PLAN_CONFIRMATION"
        response = client.post(base + "/plan-decision", json={"decision": "CONFIRM", "primary_type": plan["primary_type"],
            "secondary_types": plan["secondary_types"], "skill_codes": plan["recommended_skill_codes"],
            "target_scopes": plan["target_scopes"], "expected_plan_id": plan["plan_id"], "expected_revision": plan["revision"],
            "reviewer": "隔离验收员"})
        assert response.status_code == 200, response.text
        response = client.post("/document-governance/analyze", json={"documents": [target], "idempotency_key": "extract"})
        assert response.status_code == 202, response.text
        assert worker.run_once()
        detail = client.get(base).json()
        assert detail["workflow_status"] == "NEEDS_RESULT_CONFIRMATION", detail.get("state")
        stable_event = next(e for e in detail["analysis"]["events"] if e["key"] == "offering")
        stable_link = detail["analysis"]["relations"][0]
        old_id = next(e["fact_id"] for e in detail["analysis"]["events"] if e["key"] == "provision")
        for iteration in range(3):
            response = client.post(base + "/result-review", json=review_payload(detail,
                nonconforming=[{"point_type": "EVENT", "key": "provision", "reason": f"第{iteration + 1}轮核对预计性质"}]))
            assert response.status_code == 200, response.text
            assert response.json()["job"]["job_id"]
            duplicate = client.post(base + "/result-review", json=review_payload(detail,
                nonconforming=[{"point_type": "EVENT", "key": "provision", "reason": "重复提交"}]))
            assert duplicate.status_code == 409
            assert worker.run_once()
            detail = client.get(base).json()
            assert detail["workflow_status"] == "NEEDS_RESULT_CONFIRMATION", detail.get("state")
            assert next(e for e in detail["analysis"]["events"] if e["key"] == "offering") == stable_event
            assert detail["analysis"]["relations"][0] == stable_link
            assert next(e["fact_id"] for e in detail["analysis"]["events"] if e["key"] == "provision") != old_id
            old_id = next(e["fact_id"] for e in detail["analysis"]["events"] if e["key"] == "provision")
        published = client.post(base + "/result-review", json=review_payload(detail))
        assert published.status_code == 200 and published.json()["status"] == "PUBLISHED", published.text
        assert client.post(base + "/result-review", json=review_payload(detail)).json()["idempotent"]
        final = client.get(base).json()
        assert final["result_report"]["publication_status"] == "PUBLISHED"
        assert len(final["result_report"]["knowledge_points"]) == 5 and len(final["versions"]) >= 4
        with database() as db:
            assert all(db.get(FoundationFact, f["id"]).status == "ACCEPTED" for f in final["facts"])
            codes = set(db.scalars(select(SkillExecutionRun.skill_code)))
            assert {"DOCUMENT_SEMANTIC_ROUTER", "FINANCIAL_DISCLOSURE_GOVERNOR", "CAPITAL_OPERATION_GOVERNOR"} <= codes


def test_required_confirmation_atomic_failure_stale_version_and_skip(database, monkeypatch):
    news_id = extracted(database, monkeypatch)
    base = f"/document-governance/documents/stock_news/{news_id}"
    with database() as db:
        row = db.get(StockNews, news_id); content = deepcopy(row.content_json)
        content["event_governance"]["semantic_analysis"]["events"][0]["review_reason"] = "预计结果尚未实现，请重点核对"
        row.content_json = content; db.commit()
    with api_client(database) as client:
        detail = client.get(base).json()
        denied = client.post(base + "/result-review", json=review_payload(detail))
        assert denied.status_code == 422 and "重点确认" in denied.text
        assert client.get(base + "/reviews").json()["review_count"] == 0
        stale = client.post(base + "/result-review", json={**review_payload(detail), "expected_version_key": "stale"})
        assert stale.status_code == 409
        ids = [fact["id"] for fact in detail["facts"]]
        with database() as db:
            db.get(FoundationFact, ids[-1]).status = "REJECTED"; db.commit()
        denied = client.post(base + "/result-review", json=review_payload(detail, explicit_confirmations=["EVENT:provision"]))
        assert denied.status_code == 422
        with database() as db:
            assert db.get(FoundationFact, ids[0]).status == "PENDING"
        assert client.post(base + "/result-review", json=review_payload(detail, decision="SKIP")).status_code == 200
        assert client.get(base).json()["workflow_status"] == "SKIPPED"


@pytest.mark.parametrize("mutation", ["body", "skill", "disabled", "revision"])
def test_confirmed_execution_contract_rejects_changes(database, monkeypatch, mutation):
    _, news_id = ready(database, monkeypatch)
    with database() as db:
        row = db.get(StockNews, news_id); plan = confirm(db, row)
        if mutation == "body":
            row.content += "\n新的正文"; db.commit()
        elif mutation == "revision":
            with pytest.raises(planning.WorkflowConflict, match="变更"):
                planning.decide_plan(db, row, decision="CONFIRM", primary_type=plan["primary_type"], secondary_types=[],
                    skill_codes=plan["recommended_skill_codes"], target_scopes=["COMPANY"], reviewer="隔离验收员", reason="旧页提交",
                    expected_plan_id=plan["plan_id"], expected_revision=1)
            return
        else:
            skill = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == plan["recommended_skill_codes"][0]))
            if mutation == "disabled": skill.enabled = False
            else: skill.instructions += "\n新规则"
            db.commit()
        with pytest.raises(planning.WorkflowConflict):
            planning.validate_confirmed_plan(db, row)


def test_failed_correction_retains_targets_and_retry_endpoint(database, monkeypatch):
    news_id = extracted(database, monkeypatch)
    base = f"/document-governance/documents/stock_news/{news_id}"
    def queued(request, db):
        return {"job_id": 77, "job_status": "PENDING", "status": "PENDING"}
    monkeypatch.setattr("app.api.stock_batch.submit_stock_batch_governance", queued)
    with api_client(database) as client:
        detail = client.get(base).json()
        saved = client.post(base + "/result-review", json=review_payload(detail,
            nonconforming=[{"point_type": "EVENT", "key": "provision", "reason": "明确预计性质"}])).json()
        feedback, review_id = saved["semantic_feedback"], saved["semantic_review_id"]
        monkeypatch.setattr(semantics.ModelHubService, "chat", lambda *a, **k: (_ for _ in ()).throw(ValueError("网络故障")))
        with database() as db:
            from app.models.market_data import StockSymbol
            row = db.get(StockNews, news_id); stock = db.scalar(select(StockSymbol).where(StockSymbol.symbol == "000001"))
            result = governor.govern_stock_documents(db, stock, semantic=True, semantic_feedback=feedback,
                context={"skill_version": "9.0.0", "interactive_plan_required": True, "semantic_review_id": review_id},
                document_targets=[{"source_table": "stock_news", "record_id": news_id}])
            assert result["failed"] == 1
        failed = client.get(base).json()
        assert failed["workflow_status"] == "NEEDS_REANALYSIS"
        assert failed["analysis"] == detail["analysis"]
        assert failed["state"]["correction_feedback"][0]["key"] == "provision"
        assert client.post(base + "/retry-correction").status_code == 202


def test_real_worker_correction_failure_retry_preserves_confirmed_ids(database, monkeypatch):
    news_id = extracted(database, monkeypatch)
    worker = JobWorker(task_types=("stock_batch_governance",), session_factory=database,
        task_executor=lambda task, data: stock_batch.execute_stock_batch_governance(data))
    base = f"/document-governance/documents/stock_news/{news_id}"
    with api_client(database) as client:
        original = client.get(base).json()
        response = client.post(base + "/result-review", json=review_payload(original,
            nonconforming=[{"point_type": "EVENT", "key": "provision", "reason": "预计性质需核对"}]))
        assert response.status_code == 200, response.text
        first_job = response.json()["job"]["job_id"]
        def failure(self, **kwargs):
            if kwargs.get("metadata_json", {}).get("phase") == "SEMANTIC_READING":
                raise ValueError("隔离网络故障")
            return model_fixture(self, **kwargs)
        monkeypatch.setattr(semantics.ModelHubService, "chat", failure)
        assert worker.run_once()
        failed = client.get(base).json()
        assert failed["analysis"] == original["analysis"]
        assert failed["workflow_status"] == "NEEDS_REANALYSIS"
        monkeypatch.setattr(semantics.ModelHubService, "chat", model_fixture)
        retry = client.post(base + "/retry-correction")
        assert retry.status_code == 202, retry.text
        assert retry.json()["job_id"] != first_job
        assert worker.run_once()
        fixed = client.get(base).json()
        assert fixed["workflow_status"] == "NEEDS_RESULT_CONFIRMATION", fixed.get("state")
        for key in ("offering", "opinion"):
            assert next(e for e in fixed["analysis"]["events"] if e["key"] == key) == next(e for e in original["analysis"]["events"] if e["key"] == key)
        assert fixed["analysis"]["relations"] == original["analysis"]["relations"]


def test_no_event_can_be_confirmed_without_manufactured_facts(database, monkeypatch):
    stock_id, news_id = ready(database, monkeypatch)
    def no_events(self, **kwargs):
        if kwargs.get("metadata_json", {}).get("phase") != "STRUCTURED_EXTRACTION":
            return model_fixture(self, **kwargs)
        return SimpleNamespace(id=100, status="SUCCESS", provider_code="FIXTURE", instance_code="fixture",
            model_code="fixture", response_text=json.dumps({"participants": [], "events": [], "relations": [], "uncertainties": []}))
    monkeypatch.setattr(semantics.ModelHubService, "chat", no_events)
    with database() as db:
        from app.models.market_data import StockSymbol
        row = db.get(StockNews, news_id); confirm(db, row)
        result = governor.govern_stock_documents(db, db.get(StockSymbol, stock_id), semantic=True,
            context={"skill_version": "9.0.0", "interactive_plan_required": True},
            document_targets=[{"source_table": "stock_news", "record_id": news_id}])
        assert result["completed"] == 1, result
    with api_client(database) as client:
        base = f"/document-governance/documents/stock_news/{news_id}"
        detail = client.get(base).json()
        assert detail["result_report"]["knowledge_points"] == []
        response = client.post(base + "/result-review", json=review_payload(detail))
        assert response.status_code == 200 and response.json()["knowledge_point_count"] == 0, response.text
    with database() as db:
        assert db.scalar(select(func.count()).select_from(FoundationFact)) == 0


def test_child_rejection_blocks_publish_atomically(database, monkeypatch):
    news_id = extracted(database, monkeypatch)
    with api_client(database) as client:
        base = f"/document-governance/documents/stock_news/{news_id}"
        detail = client.get(base).json()
        response = client.post(base + "/reviews", json={"version_index": detail["selected_version"], "action": "REQUEST_CORRECTION",
            "target_type": "METRIC", "target_key": "provision", "event_key": "provision", "item_index": 0,
            "reason": "该金额待核对", "reviewer": "隔离验收员"})
        assert response.status_code == 200, response.text
        detail = client.get(base).json()
        response = client.post(base + "/result-review", json=review_payload(detail))
        assert response.status_code == 422, response.text
        with database() as db:
            assert all(db.get(FoundationFact, fact["id"]).status == "PENDING" for fact in detail["facts"])


def test_specialist_draft_test_enable_no_fact_writes_and_refresh(database, monkeypatch):
    _, news_id = ready(database, monkeypatch)
    with database() as db:
        row = db.get(StockNews, news_id); plan = planning.generate_plan(db, row)
        content = deepcopy(row.content_json)
        content["semantic_governance_plan"]["capability_gaps"] = [{"type_name": "税务争议", "suggested_skill_name": "税务事项治理", "reason": "需要分清计提与实际补缴"}]
        row.content_json = content; db.commit()
        draft = specialist.create_draft(db, row, gap_index=0, expected_plan_id=plan["plan_id"], reviewer="隔离验收员")
        skill = db.get(ModelSkill, draft["id"])
        with pytest.raises(planning.WorkflowConflict, match="测试"):
            specialist.enable_draft(db, skill, reviewer="隔离验收员")
        assert specialist.create_draft(db, row, gap_index=0, expected_plan_id=plan["plan_id"], reviewer="隔离验收员")["id"] == draft["id"]
        result = specialist.test_draft(db, skill, row, reviewer="隔离验收员")
        assert result["test_result"]["passed"], result
        assert db.scalar(select(func.count()).select_from(FoundationFact)) == 0
        enabled = specialist.enable_draft(db, skill, reviewer="隔离验收员")
        assert enabled["lifecycle_status"] == "ENABLED"
        agent = db.scalar(select(AgentDefinition).where(AgentDefinition.agent_code == "DOCUMENT_SEMANTIC_GOVERNANCE_AGENT"))
        assert db.scalar(select(AgentSkillLink).where(AgentSkillLink.agent_id == agent.id, AgentSkillLink.skill_id == skill.id))
        skill.instructions += "\n修改了规则"
        with pytest.raises(planning.WorkflowConflict): specialist.require_test(skill)
        db.rollback()
    with api_client(database) as client:
        drafts = client.get(f"/document-governance/documents/stock_news/{news_id}/skill-drafts").json()
        assert drafts["0"]["id"] == draft["id"]


def test_patch_relation_endpoint_and_event_deletion_protect_confirmed_links():
    reading, output = interactive_payload()
    previous = semantics.validate_analysis(semantics.Reading.model_validate(reading), semantics.Analysis.model_validate(output), BODY)
    for index, link in enumerate(previous["relations"]): link["fact_id"] = f"relation:{index}"
    for event in previous["events"]: event["fact_id"] = "event:" + event["key"]
    link = deepcopy(output["relations"][0]); link.update(correction_key="relation:0", target_key="provision", predicate="INVOLVES")
    doc = {"body": BODY, "previous_analysis": previous, "correction_feedback": [{"point_type": "RELATION", "key": "relation:0", "reason": "核对端点"}]}
    fixed = semantics.merge_correction(semantics.Reading.model_validate(reading), semantics.AnalysisPatch(relations=[link]), doc)
    assert fixed["relations"][0]["target_key"] == "provision" and "fact_id" not in fixed["relations"][0]
    assert fixed["relations"][1] == previous["relations"][1]
    doc["correction_feedback"] = [{"point_type": "EVENT", "key": "provision", "reason": "证据不足"}]
    with pytest.raises(ValueError, match="已确认关系引用"):
        semantics.merge_correction(semantics.Reading.model_validate(reading), semantics.AnalysisPatch(removed_event_keys=["provision"]), doc)


def test_long_document_coverage_tables_and_failure_logs(database, monkeypatch):
    seen = []
    def model(self, **kwargs):
        user = json.loads(kwargs["messages"][-1]["content"]); seen.append(user["body"])
        value = {"overview": "读取本章节", "sections": [{"topic": "本部分", "summary": "逐段读取章节", "evidence_quotes": [user["body"][:30]]}], "uncertainties": []} if kwargs["metadata_json"]["phase"] == "SEMANTIC_READING" else {"participants": [], "events": [], "relations": [], "uncertainties": []}
        return SimpleNamespace(id=len(seen), status="SUCCESS", provider_code="FIXTURE", instance_code="fixture", model_code="fixture", response_text=json.dumps(value))
    monkeypatch.setattr(semantics.ModelHubService, "chat", model)
    body = "章节内容。\n" * 4000 + "[[TABLE]]\n名称\t金额（亿元）\n利润\t1.07\n[[/TABLE]]" + "尾部内容。\n" * 4000
    with database() as db:
        result = semantics.semantic_analysis(db, {"key": "isolated", "title": "长文", "body": body, "market": "CN_A",
            "symbol": "000001", "stock_name": "测试公司", "state": {"evidence_id": "fixture"}}, context={}, model_instance=None, instructions="")
        coverage = result["full_body_coverage"]
        assert sum(c["characters"] for c in coverage) == len(body)
        assert "".join(seen[::2]) == body
        assert all(s.count("[[TABLE]]") == s.count("[[/TABLE]]") for s in seen)
        assert len(result["model_calls"]) == 2 * len(coverage)


def test_table_metric_requires_matching_column_unit_and_value():
    body = "[[TABLE]]\n项目\t金额（亿元）\t占比（%）\n预计负债\t1.07\t2.40\n[[/TABLE]]"
    metric = {"name": "预计负债", "raw_value": "1.07", "unit": "亿元", "qualifier": "EXACT", "period": "本期",
        "evidence_quote": "预计负债\t1.07\t2.40", "table_index": 0, "table_row": 1, "table_column": 1}
    assert semantics.normalize_metric(metric, body)["normalized_value"] == "107000000.00"
    with pytest.raises(ValueError): semantics.normalize_metric({**metric, "unit": "%"}, body)
    with pytest.raises(ValueError): semantics.normalize_metric({**metric, "table_column": 2}, body)


def test_long_correction_reads_full_text_and_remaps_whole_table(database, monkeypatch):
    prefix = "[[TABLE]]\n项目\t金额（亿元）\n营业收入\t2.4\n[[/TABLE]]\n" + "前章内容。\n" * 7000
    table = "[[TABLE]]\n项目\t金额（亿元）\n预计负债\t1.07\n[[/TABLE]]"
    body = prefix + "测试公司披露本期计提数据。\n" + table + "\n" + "尾章内容。\n" * 3000
    reading = {"overview": "表格披露预计负债。", "sections": [{"topic": "预计负债", "summary": "公司本期计提数据", "evidence_quotes": ["测试公司披露本期计提数据"]}], "uncertainties": []}
    metric = {"name": "预计负债", "raw_value": "1.07", "unit": "亿元", "evidence_quote": "预计负债\t1.07", "table_index": 1, "table_row": 1, "table_column": 1}
    event = {"key": "provision", "event_type": "计提预计负债", "summary": "公司计提预计负债1.07亿元。", "assertion_type": "REPORTED_FACT", "attribution": "文档披露",
        "subject_scope": "COMPANY", "subject_name": "测试公司", "evidence_quotes": ["测试公司披露本期计提数据", "预计负债\t1.07"], "metrics": [metric], "impacts": []}
    previous = semantics.validate_analysis(semantics.Reading.model_validate(reading), semantics.Analysis(events=[event]), body)
    previous["events"][0]["fact_id"] = "old-candidate"
    seen = []
    def model(self, **kwargs):
        user = json.loads(kwargs["messages"][-1]["content"]); seen.append((kwargs["metadata_json"]["phase"], user))
        if kwargs["metadata_json"]["phase"] == "SEMANTIC_READING":
            value = {"overview": "读取本章节", "sections": [{"topic": "本部分", "summary": "章节阅读", "evidence_quotes": [user["body"][:30]]}], "uncertainties": []}
        else:
            fixed = deepcopy(event); fixed["metrics"][0]["table_index"] = 0; fixed["summary"] += "已核对本期性质。"
            value = {"participants": [], "events": [fixed], "relations": [], "removed_event_keys": [], "removed_relation_keys": [], "uncertainties": []}
        return SimpleNamespace(id=len(seen), status="SUCCESS", provider_code="FIXTURE", instance_code="fixture", model_code="fixture", response_text=json.dumps(value))
    monkeypatch.setattr(semantics.ModelHubService, "chat", model)
    with database() as db:
        result = semantics.semantic_analysis(db, {"key": "isolated", "title": "长文表格", "body": body, "market": "CN_A", "symbol": "000001",
            "stock_name": "测试公司", "state": {"evidence_id": "fixture"}, "previous_analysis": previous,
            "review_feedback": "核对本期性质", "correction_feedback": [{"point_type": "EVENT", "key": "provision", "reason": "核对本期性质"}]},
            context={}, model_instance=None, instructions="")
    assert sum(c["characters"] for c in result["full_body_coverage"]) == len(body)
    patched_metric = result["events"][0]["metrics"][0]
    assert patched_metric["table_index"] == 1 and patched_metric["normalized_value"] == "107000000.00"
    window = next(user for phase, user in seen if phase == "STRUCTURED_EXTRACTION")
    assert table in window["body"] and window["body"].count("[[TABLE]]") == window["body"].count("[[/TABLE]]")
    assert window["previous_analysis"]["events"][0]["metrics"][0]["table_index"] == 0


def test_sector_evidence_cannot_be_forced_into_current_company(database, monkeypatch):
    stock_id, _ = ready(database, monkeypatch)
    with database() as db:
        from app.models.market_data import StockSymbol
        stock = db.get(StockSymbol, stock_id)
        with pytest.raises(ValueError, match="主体必须出现"):
            governor._semantic_subject(db, stock, "company", {"subject_scope": "COMPANY", "subject_name": "平安银行",
                "evidence_quotes": ["银行板块今日整体上涨百分之五"]}, {"target_scopes": ["COMPANY"]}, "银行板块今日整体上涨百分之五", "doc")
        with pytest.raises(ValueError, match="不在用户确认"):
            governor._semantic_subject(db, stock, "company", {"subject_scope": "SECTOR", "subject_name": "银行板块"},
                {"target_scopes": ["COMPANY"]}, "银行板块今日整体上涨百分之五", "doc")
