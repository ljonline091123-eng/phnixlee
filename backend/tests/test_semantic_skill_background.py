"""Queue recovery, multi-document acceptance and stale-evidence safeguards."""
from copy import deepcopy
import json

import pytest
from sqlalchemy import select, func

from app.models.ai_hub import ModelCallLog, ModelSkill
from app.models.decision_review import SkillEvaluationRun, SkillEvaluationResult
from app.models.foundation import FoundationFact
from app.models.market_data import StockNews
from app.models.pipeline import ScheduledJob
from app.jobs.worker import JobWorker
from app.services import document_semantic_skills as specialist
from app.services import document_semantic_planning as planning
from tests.test_document_semantics import database
from tests.test_interactive_semantic_workflow import ready, model_fixture
from tests.test_semantic_batch_execution import api_client


def draft_for(db, news_id):
    row = db.get(StockNews, news_id)
    plan = planning.generate_plan(db, row)
    content = deepcopy(row.content_json)
    content["semantic_governance_plan"]["capability_gaps"] = [{"type_name": "税务证据",
        "suggested_skill_name": "税务事项治理", "reason": "需要区分预计计提与实际支付"}]
    row.content_json = content
    db.commit()
    draft = specialist.create_draft(db, row, gap_index=0, expected_plan_id=plan["plan_id"], reviewer="隔离验收员")
    return db.get(ModelSkill, draft["id"]), row


def test_async_duplicate_submission_worker_and_reopened_progress(database, monkeypatch):
    _, news_id = ready(database, monkeypatch)
    with database() as db:
        skill, _ = draft_for(db, news_id)
        skill_id = skill.id
    worker = JobWorker(task_types=(specialist.TEST_TASK_TYPE,), session_factory=database,
        task_executor=lambda task, payload: specialist.execute_test_job(payload))
    monkeypatch.setattr("app.db.session.SessionLocal", database)
    with api_client(database) as client:
        path = f"/document-governance/skill-drafts/{skill_id}/test-jobs"
        first = client.post(path, json={"reviewer": "隔离验收员"})
        assert first.status_code == 202, first.text
        job_id = first.json()["job_id"]
        assert client.post(path, json={"reviewer": "隔离验收员"}).json()["job_id"] == job_id
        reopened = client.get(f"/document-governance/documents/stock_news/{news_id}/skill-drafts").json()
        assert reopened["0"]["test_job_id"] == job_id
        assert reopened["0"]["test_progress"]["status"] == "PENDING"
        assert worker.run_once()
        finished = client.get(f"/document-governance/skill-test-jobs/{job_id}").json()
        assert finished["status"] == "COMPLETED", finished
        assert finished["draft"]["test_result"]["passed"]
        assert finished["draft"]["test_progress"]["status"] == "COMPLETED"
    with database() as db:
        assert db.scalar(select(func.count()).select_from(FoundationFact)) == 0


@pytest.mark.parametrize("mutation", ["body", "scope", "sources"])
def test_enable_rejects_changed_evidence_or_acceptance_scope(database, monkeypatch, mutation):
    _, news_id = ready(database, monkeypatch)
    with database() as db:
        skill, row = draft_for(db, news_id)
        assert specialist.test_draft(db, skill, row, reviewer="隔离验收员")["test_result"]["passed"]
        if mutation == "body":
            row.content += "\n新披露数据改变了原文。"
        elif mutation == "scope":
            content = deepcopy(row.content_json)
            content["semantic_governance_plan"]["target_scopes"] = ["MARKET"]
            row.content_json = content
        else:
            skill.config_json = {**skill.config_json, "validation_sources": []}
        db.commit()
        with pytest.raises(planning.WorkflowConflict, match="重新测试"):
            specialist.enable_draft(db, skill, reviewer="隔离验收员")
        assert not skill.enabled


def test_independent_review_receives_rejected_output_and_saves_last_improvement(database, monkeypatch):
    _, news_id = ready(database, monkeypatch)
    rejected = {"events": [{"summary": "错误输出", "metrics": [{"raw_value": "999"}]}]}
    def extraction(*args, **kwargs):
        exc = ValueError("指标数值或单位与原文不符")
        exc.semantic_invalid_output = rejected
        exc.semantic_reading = {"overview": "实际阅读"}
        raise exc
    seen = []
    def review(self, **kwargs):
        if kwargs.get("metadata_json", {}).get("purpose") != "DOCUMENT_SPECIALIST_EVALUATION":
            return model_fixture(self, **kwargs)
        seen.append(json.loads(kwargs["messages"][-1]["content"])["extraction"])
        result = model_fixture(self, **kwargs)
        result.response_text = json.dumps({"passed": True, "score": 99, "summary": "需要修正表格数字",
            "improved_instructions": "必须逐字对照原文的数值、单位与表格行列，不得增加符号或猜测年份。保留全部关键指标并按契约JSON输出。"}, ensure_ascii=False)
        return result
    monkeypatch.setattr(specialist.ModelHubService, "chat", review)
    monkeypatch.setattr(specialist, "semantic_analysis", extraction)
    with database() as db:
        skill, row = draft_for(db, news_id)
        result = specialist.test_draft(db, skill, row, reviewer="隔离验收员", max_iterations=1)
        assert seen[0]["rejected_output"] == rejected
        assert seen[0]["reading"]["overview"] == "实际阅读"
        assert not result["test_result"]["passed"]
        assert result["test_result"]["pending_version_test"]
        assert result["version"] != result["test_result"]["skill_version"]
        with pytest.raises(planning.WorkflowConflict):
            specialist.enable_draft(db, skill, reviewer="隔离验收员")


def test_multiple_documents_all_required_and_do_not_write_facts(database, monkeypatch):
    _, news_id = ready(database, monkeypatch)
    original = specialist.semantic_analysis
    def extraction(db, doc, **kwargs):
        if doc["title"] == "失败验收样本":
            raise ValueError("第二篇文档证据缺失")
        return original(db, doc, **kwargs)
    monkeypatch.setattr(specialist, "semantic_analysis", extraction)
    with database() as db:
        skill, row = draft_for(db, news_id)
        second = StockNews(symbol=row.symbol, market=row.market, source_id=row.source_id,
            title="失败验收样本", content=row.content, content_json={}, news_time=row.news_time)
        db.add(second); db.commit()
        planning.generate_plan(db, second)
        skill.config_json = {**skill.config_json, "validation_sources": [
            {"source_table": "stock_news", "record_id": row.id}, {"source_table": "stock_news", "record_id": second.id}]}
        db.commit()
        result = specialist.test_draft(db, skill, row, reviewer="隔离验收员", max_iterations=1)
        assert result["test_result"]["document_count"] == 2
        assert not result["test_result"]["passed"]
        assert len(result["test_result"]["document_results"]) == 2
        assert db.scalar(select(func.count()).select_from(FoundationFact)) == 0


def test_async_unexpected_failure_closes_evaluation_and_keeps_retry_available(database, monkeypatch):
    _, news_id = ready(database, monkeypatch)
    monkeypatch.setattr("app.db.session.SessionLocal", database)
    with database() as db:
        skill, _ = draft_for(db, news_id)
        job = specialist.submit_test_job(db, skill, reviewer="隔离验收员")
        skill_id, job_id = skill.id, job["job_id"]
    def crash(db, skill, row, **kwargs):
        run = SkillEvaluationRun(skill_id=skill.id, skill_code=skill.skill_code, skill_version=skill.version,
            task_type="data_governance", evaluator="ISOLATED_FAILURE", status="RUNNING")
        db.add(run); db.flush()
        skill.config_json = {**skill.config_json, "semantic_test_progress": {"status": "RUNNING", "run_id": run.id}}
        db.commit()
        raise RuntimeError("隔离中断")
    monkeypatch.setattr(specialist, "test_draft", crash)
    worker = JobWorker(task_types=(specialist.TEST_TASK_TYPE,), session_factory=database,
        task_executor=lambda task, payload: specialist.execute_test_job(payload))
    assert worker.run_once()
    with database() as db:
        skill = db.get(ModelSkill, skill_id)
        assert skill.config_json["semantic_test_progress"]["status"] == "FAILED"
        assert db.get(ScheduledJob, job_id).status == "FAILED"
        assert db.scalar(select(SkillEvaluationRun)).status == "FAILED"
        assert specialist.submit_test_job(db, skill, reviewer="隔离验收员")["job_id"] != job_id


def test_model_unavailable_stops_without_revising_or_claiming_remaining_samples(database, monkeypatch):
    _, news_id = ready(database, monkeypatch)
    with database() as db:
        skill, row = draft_for(db, news_id)
        second = StockNews(symbol=row.symbol, market=row.market, source_id=row.source_id,
            title="尚未处理的样本", content=row.content, content_json={}, news_time=row.news_time)
        db.add(second); db.commit()
        planning.generate_plan(db, second)
        skill.config_json = {**skill.config_json, "validation_sources": [
            {"source_table": "stock_news", "record_id": row.id}, {"source_table": "stock_news", "record_id": second.id}]}
        db.commit()
        calls = []
        def unavailable(self, **kwargs):
            calls.append(kwargs)
            result = model_fixture(self, **kwargs)
            result.status = "FAILED"; result.response_text = ""
            result.error_message = "HTTP 402：API账户余额不足"
            return result
        monkeypatch.setattr(specialist.ModelHubService, "chat", unavailable)
        original_version = skill.version
        result = specialist.test_draft(db, skill, row, reviewer="隔离验收员")["test_result"]
        assert len(calls) == 1
        assert not result["passed"] and result["failure_kind"] == "MODEL_UNAVAILABLE"
        assert "402" in result["error"]
        assert result["processed_document_count"] == 1 and result["document_count"] == 2
        assert skill.version == original_version
        assert db.scalar(select(func.count()).select_from(SkillEvaluationResult)) == 1
        assert db.scalar(select(func.count()).select_from(FoundationFact)) == 0


def test_multiple_sample_revisions_include_passed_and_failed_evidence(database, monkeypatch):
    _, news_id = ready(database, monkeypatch)
    with database() as db:
        skill, row = draft_for(db, news_id)
        second = StockNews(symbol=row.symbol, market=row.market, source_id=row.source_id,
            title="第二篇不同证据", content=row.content, content_json={}, news_time=row.news_time)
        db.add(second); db.commit()
        planning.generate_plan(db, second)
        skill.config_json = {**skill.config_json, "validation_sources": [
            {"source_table": "stock_news", "record_id": row.id}, {"source_table": "stock_news", "record_id": second.id}]}
        db.commit()
        seen = []
        def chat(self, **kwargs):
            result = model_fixture(self, **kwargs)
            purpose = kwargs.get("metadata_json", {}).get("purpose")
            data = json.loads(kwargs["messages"][-1]["content"])
            if purpose == "DOCUMENT_SPECIALIST_EVALUATION" and data["title"] == second.title:
                result.response_text = json.dumps({"passed": False, "score": 55, "summary": "需修订第二篇主体规则",
                    "improved_instructions": "不能仅仅使用本篇建议，需综合成功样本和失败样本，逐字对照冻结原文，并核验所有数值、主体、日期及证据。"})
            elif purpose == "DOCUMENT_SPECIALIST_REVISION":
                seen.append(data)
                result.response_text = json.dumps({"instructions": "综合保留第一篇通过的主体边界，新增第二篇证据核验。逐字对照原文数值、符号、单位及主体，区分计提和支付，不猜测日期，所有知识按契约JSON输出。"})
            return result
        monkeypatch.setattr(specialist.ModelHubService, "chat", chat)
        result = specialist.test_draft(db, skill, row, reviewer="隔离验收员", max_iterations=1)["test_result"]
        assert len(seen) == 1
        assert {s["record_id"] for s in seen[0]["samples"]} == {row.id, second.id}
        assert len(seen[0]["reviews"]) == 2
        assert {s["evaluation"]["passed"] for s in seen[0]["reviews"]} == {True, False}
        assert result["revision_model_call_ids"] == [100]
        assert result["pending_version_test"] and not result["passed"]


def test_queued_version_change_finishes_failed_and_allows_retry(database, monkeypatch):
    _, news_id = ready(database, monkeypatch)
    monkeypatch.setattr("app.db.session.SessionLocal", database)
    with database() as db:
        skill, _ = draft_for(db, news_id)
        job = specialist.submit_test_job(db, skill, reviewer="隔离验收员")
        skill.version = "1.0.99"
        db.commit()
        skill_id, job_id = skill.id, job["job_id"]
    worker = JobWorker(task_types=(specialist.TEST_TASK_TYPE,), session_factory=database,
        task_executor=lambda task, payload: specialist.execute_test_job(payload))
    assert worker.run_once()
    with database() as db:
        skill = db.get(ModelSkill, skill_id)
        assert skill.config_json["semantic_test_progress"]["status"] == "FAILED"
        assert "版本已变化" in skill.config_json["semantic_test_progress"]["message"]
        assert db.get(ScheduledJob, job_id).status == "FAILED"
        assert specialist.submit_test_job(db, skill, reviewer="隔离验收员")["job_id"] != job_id


def test_stale_draft_read_and_legacy_completed_queue_show_failed_acceptance(database, monkeypatch):
    _, news_id = ready(database, monkeypatch)
    with database() as db:
        skill, row = draft_for(db, news_id)
        assert specialist.test_draft(db, skill, row, reviewer="隔离验收员")["test_result"]["passed"]
        row.content += "新事实使验收快照过期。"
        db.commit()
        read = specialist.draft_read(skill, db=db)
        assert not read["test_result"]["passed"] and read["test_result"]["pending_version_test"]
        job = specialist.submit_test_job(db, skill, reviewer="隔离验收员")
        queued = db.get(ScheduledJob, job["job_id"])
        queued.status = "COMPLETED"
        skill.config_json = {**skill.config_json, "semantic_test_progress": {"status": "FAILED"},
            "semantic_draft_test": {"passed": False, "error": "验收失败"}}
        db.commit()
        read = specialist.test_job_read(db, queued)
        assert read["status"] == "FAILED" and read["queue_status"] == "COMPLETED"
        assert read["error"] == "验收失败"
        assert queued.status == "COMPLETED"  # Historical queue record stays intact.


def test_legacy_model_failure_uses_audit_reason_without_rewriting_history(database, monkeypatch):
    _, news_id = ready(database, monkeypatch)
    with database() as db:
        skill, _ = draft_for(db, news_id)
        call = ModelCallLog(task_type="data_governance", provider_code="DEEPSEEK", status="FAILED",
            error_message="HTTP 402：API账户余额不足")
        db.add(call); db.flush()
        original = {"passed": False, "error": f"SEMANTIC_READING未取得真实模型正文输出（模型调用#{call.id}）",
            "score": 0, "document_results": [{"record_id": news_id}]}
        skill.config_json = {**skill.config_json, "semantic_draft_test": original,
            "semantic_test_progress": {"status": "FAILED", "message": "4/4篇文档未通过"}}
        db.commit()
        read = specialist.draft_read(skill, db=db)
        assert read["test_result"]["failure_kind"] == "MODEL_UNAVAILABLE"
        assert "HTTP 402" in read["test_result"]["error"]
        assert "未完成" in read["test_progress"]["message"]
        assert skill.config_json["semantic_draft_test"] == original
        assert skill.config_json["semantic_test_progress"]["message"] == "4/4篇文档未通过"
