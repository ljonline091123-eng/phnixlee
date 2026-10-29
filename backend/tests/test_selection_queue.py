"""Durable selection tasks: HTTP acknowledgement, recovery and decision gates."""
from datetime import datetime, timezone
import time
from types import SimpleNamespace
from unittest.mock import patch
from tests.selection_samples import valid_scorecard

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, func, select
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.db.base import Base
from app.db.session import get_db
from app.main import app
from app.models.ai_hub import KnowledgeBase, KnowledgeGraph, ModelCallLog, ModelInstance, ModelProvider
from app.models.decision_review import SelectionDecisionSnapshot
from app.models.market_data import DataSource, StockKline, StockSymbol
from app.models.pipeline import ScheduledJob
from app.models.selection import SelectionCandidate, SelectionRun, SelectionTracking
from app.schemas.selection import SelectionReviewRequest, SelectionRunCreate
from app.services.selection import (
    _SelectionProgressHeartbeat,
    _progress,
    _sanitize_model_evidence_references,
    execute_failed_selection_reanalysis,
    execute_selection_job,
    queue_selection_run,
    review_candidate,
)


NOW = datetime(2025, 1, 6, 10, tzinfo=timezone.utc)


@pytest.fixture
def isolated():
    engine = create_engine("sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool)
    Base.metadata.create_all(engine)
    factory = sessionmaker(bind=engine, expire_on_commit=False)
    with factory() as db:
        source = DataSource(source_code="QUEUE_TEST", source_name="隔离测试来源", adapter_type="MOCK")
        db.add(source)
        db.flush()
        db.add(StockSymbol(market="CN_A", symbol="DEMO001", name="隔离样本", exchange="DEMO", source_id=source.id))
        for day, close in [("2025-01-03", 10), ("2025-01-06", 11)]:
            db.add(StockKline(market="CN_A", symbol="DEMO001", period="daily", adjust="",
                             trade_date=day, close_price=close, volume=100, source_id=source.id, fetched_at=NOW))
        db.commit()
    with patch("app.db.session.SessionLocal", factory), patch("app.jobs.governance_runner.wake_governance_runner"):
        yield factory
    engine.dispose()


def request():
    return SelectionRunCreate(symbols=["DEMO001"], use_model=False, data_mode="MOCK")


def test_selection_request_defaults_to_updating_data_before_analysis():
    assert request().preflight_data_action == "UPDATE"
    assert SelectionRunCreate(
        symbols=["DEMO001"], use_model=False, data_mode="MOCK",
        preflight_data_action="CONTINUE_PARTIAL",
    ).preflight_data_action == "CONTINUE_PARTIAL"
    assert SelectionRunCreate(
        symbols=["DEMO001"], use_model=False, data_mode="MOCK",
        preflight_data_action="REUSE_EXISTING",
    ).preflight_data_action == "REUSE_EXISTING"


def test_continue_partial_records_decision_and_skips_remote_update(isolated):
    with isolated() as db:
        payload = request().model_copy(update={"preflight_data_action": "CONTINUE_PARTIAL"})
        run = queue_selection_run(db, payload)
        with patch("app.services.selection._prepare_selection_data") as prepare:
            result = execute_selection_job({"selection_run_id": run.id})
        assert result["candidate_count"] == 1
        prepare.assert_not_called()
        db.expire_all()
        saved = db.get(SelectionRun, run.id)
        assert saved.criteria_json["request"]["preflight_data_action"] == "CONTINUE_PARTIAL"
        steps = saved.criteria_json["progress"]["process_steps"]
        partial_step = next(item for item in steps if item["stage"] == "DATA_PARTIAL_CONFIRMED")
        assert "部分数据" in partial_step["message"]


def test_graph_dimension_without_loaded_relation_is_downgraded_not_stock_failure():
    output = {
        "dimensions": [{
            "dimension": "GRAPH", "status": "SUPPORTED", "direction": "POSITIVE",
            "summary": "graph", "evidence_document_ids": [],
            "evidence_fact_ids": [], "evidence_relation_ids": [],
        }],
        "events": [], "event_timeline": [], "event_links": [], "validation_findings": [],
        "evidence_document_ids": [], "evidence_fact_ids": [], "evidence_chunk_ids": [],
    }
    _sanitize_model_evidence_references(output, {"evidence_json": {"documents": [], "chunks": [], "graph_paths": [], "evidence_fact_ids": []}})
    graph = output["dimensions"][0]
    assert graph["status"] == "INSUFFICIENT"
    assert graph["direction"] == "UNKNOWN"
    assert output["validation_status"] == "PARTIAL"


def test_model_request_heartbeat_persists_activity_and_elapsed_time(isolated):
    with isolated() as db:
        run = SelectionRun(market="CN_A", status="RUNNING", criteria_json={"progress": {"stage": "MODEL", "percent": 40}})
        db.add(run)
        db.commit()
        heartbeat = _SelectionProgressHeartbeat(
            run.id, activity="等待测试模型返回", model_instance_code="ZHIPU_FLASH",
            batch_index=1, batch_total=1, candidate_symbols=["DEMO001"],
            percent=40, purpose="TEST", interval_seconds=0.01,
        )
        heartbeat.start()
        time.sleep(0.06)
        heartbeat.stop()
        db.expire_all()
        progress = (db.get(SelectionRun, run.id).criteria_json or {}).get("progress") or {}
        assert progress["activity"] == "等待测试模型返回"
        assert progress["model_instance_code"] == "ZHIPU_FLASH"
        assert progress["heartbeat_at"]
        assert progress["elapsed_seconds"] >= 0


def test_progress_keeps_compact_backend_activity_history(isolated):
    with isolated() as db:
        run = SelectionRun(market="CN_A", status="RUNNING", criteria_json={})
        db.add(run)
        db.commit()
        _progress(db, run, "EVIDENCE", 25, "已加载新闻公告证据", activity="读取证据")
        _progress(db, run, "MODEL", 40, "等待模型返回", activity="调用模型")
        db.expire_all()
        steps = (db.get(SelectionRun, run.id).criteria_json or {}).get("progress", {}).get("process_steps")
        assert [item["stage"] for item in steps] == ["EVIDENCE", "MODEL"]
        assert steps[-1]["message"] == "等待模型返回"


def test_progress_persists_three_phase_timeline_and_timing(isolated):
    with isolated() as db:
        run = SelectionRun(market="CN_A", status="RUNNING", criteria_json={})
        db.add(run)
        db.commit()
        _progress(db, run, "DATA_SYNC", 10, "同步行情", phase="DATA_SYNC")
        _progress(db, run, "EVIDENCE", 35, "加载证据")
        _progress(db, run, "MODEL_PREFLIGHT", 42, "测试模型")
        _progress(db, run, "MODEL", 70, "执行分析")
        _progress(db, run, "COMPLETED", 100, "分析完成")
        db.expire_all()
        progress = (db.get(SelectionRun, run.id).criteria_json or {})["progress"]
        assert list(progress["phases"]) == ["DATA", "MODEL_PREFLIGHT", "ANALYSIS"]
        assert [progress["phases"][key]["status"] for key in ("DATA", "MODEL_PREFLIGHT", "ANALYSIS")] == ["SUCCESS", "SUCCESS", "SUCCESS"]
        assert progress["phases"]["DATA"]["activities"][-1]["message"] == "加载证据"
        assert progress["phases"]["MODEL_PREFLIGHT"]["completed_at"]
        assert progress["phases"]["ANALYSIS"]["completed_at"]
        assert progress["total_elapsed_seconds"] >= 0


def test_run_read_exposes_active_model_call_when_heartbeat_is_delayed(isolated):
    with isolated() as db:
        run = SelectionRun(
            market="CN_A", status="RUNNING", model_instance_code="ZHIPU_FLASH",
            criteria_json={"progress": {"stage": "MODEL", "percent": 40}},
        )
        db.add(run)
        db.flush()
        db.add(ModelCallLog(
            task_type="stock_screening", provider_code="ZHIPU", instance_code="ZHIPU_FLASH",
            model_code="glm-4-flash", status="RUNNING",
            request_json={"metadata_json": {"selection_run_id": run.id,
                                             "purpose": "STOCK_SELECTION_ANALYSIS",
                                             "candidate_symbols": ["DEMO001"]}},
        ))
        db.commit()
        app.dependency_overrides[get_db] = lambda: db
        try:
            client = TestClient(app)
            response = client.get(f"/api/v1/selection/runs/{run.id}")
            assert response.status_code == 200, response.text
            progress = response.json()["criteria_json"]["progress"]
            assert progress["activity"].startswith("模型调用中")
            assert progress["active_model_call_id"]
            assert progress["model_instance_code"] == "ZHIPU_FLASH"
            assert progress["candidate_symbols"] == ["DEMO001"]
        finally:
            app.dependency_overrides.pop(get_db, None)


def test_start_acknowledges_before_execution_and_completed_job_is_idempotent(isolated):
    with isolated() as db:
        app.dependency_overrides[get_db] = lambda: db
        try:
            client = TestClient(app, raise_server_exceptions=True)
            with patch("app.services.selection._now", return_value=NOW):
                response = client.post("/api/v1/selection/runs/start", json=request().model_dump())
                assert response.status_code == 202, response.text
                run_id = response.json()["id"]
                assert response.json()["status"] == "QUEUED"
                assert response.json()["candidates"] == []
                job = db.scalar(select(ScheduledJob))
                assert job.task_type == "selection_analysis" and job.payload_json == {"selection_run_id": run_id}
                first = execute_selection_job(job.payload_json)
                second = execute_selection_job(job.payload_json)
                assert first == second and first["candidate_count"] == 1
                db.expire_all()
                assert client.get(f"/api/v1/selection/runs/{run_id}").json()["criteria_json"]["progress"]["percent"] == 100
                assert db.scalar(select(func.count(SelectionCandidate.id))) == 1
                assert db.scalar(select(func.count(SelectionDecisionSnapshot.id))) == 1
        finally:
            client.close()
            app.dependency_overrides.pop(get_db, None)


def test_interrupted_job_recovers_without_duplicate_candidates(isolated):
    with isolated() as db, patch("app.services.selection._now", return_value=NOW):
        run = queue_selection_run(db, request())
        with patch("app.services.selection._evidence", side_effect=RuntimeError("temporary evidence error")):
            with pytest.raises(RuntimeError, match="temporary evidence error"):
                execute_selection_job({"selection_run_id": run.id})
        db.expire_all()
        failed = db.get(SelectionRun, run.id)
        assert failed.status == "FAILED" and "temporary evidence error" in failed.error_message
        result = execute_selection_job({"selection_run_id": run.id})
        assert result["candidate_count"] == 1
        assert db.scalar(select(func.count(SelectionCandidate.id))) == 1
        db.expire_all()
        assert db.get(SelectionRun, run.id).error_message is None


def test_queued_job_rechecks_disabled_graph_before_model_call(isolated):
    with isolated() as db:
        base = KnowledgeBase(kb_code="QUEUE_KB", kb_name="隔离知识库")
        db.add(base)
        db.flush()
        graph = KnowledgeGraph(knowledge_base_id=base.id, graph_code="QUEUE_GRAPH", graph_name="隔离图谱", governance_status="GOVERNED")
        db.add(graph)
        db.commit()
        run = queue_selection_run(db, SelectionRunCreate(symbols=["DEMO001"], use_model=True,
            knowledge_base_ids=[base.id], graph_ids=[graph.id], data_mode="REAL"))
        graph.enabled = False
        db.commit()
        with patch("app.services.model_hub.ModelHubService.chat") as chat:
            with pytest.raises(ValueError, match="已停用"):
                execute_selection_job({"selection_run_id": run.id})
            chat.assert_not_called()


def test_failed_real_model_cannot_be_approved(isolated):
    with isolated() as db:
        run = SelectionRun(market="CN_A", status="PARTIAL", analysis_mode="MODEL_FAILED_FALLBACK_LOCAL", data_mode="REAL")
        db.add(run)
        db.flush()
        candidate = SelectionCandidate(run_id=run.id, market="CN_A", symbol="DEMO001", entry_price=11,
            entry_date="2025-01-06", analysis_json={"model_output_status": "FAILED"})
        db.add(candidate)
        db.commit()
        with pytest.raises(ValueError, match="真实模型尚未完成"):
            review_candidate(db, candidate.id, SelectionReviewRequest(decision="APPROVED", target_price=12))
        assert candidate.decision == "PENDING"
        assert db.scalar(select(SelectionTracking)) is None


def test_partial_candidate_read_keeps_review_context_but_hides_unverified_score(isolated):
    with isolated() as db:
        run = SelectionRun(market="CN_A", status="PARTIAL", analysis_mode="MODEL_PARTIAL", data_mode="REAL")
        db.add(run)
        db.flush()
        candidate = SelectionCandidate(
            run_id=run.id, market="CN_A", symbol="DEMO001", decision="PENDING",
            evidence_json={"documents": [{"id": 123, "title": "待核验公告", "content": "公司发布经营进展公告，项目仍在推进，部分事项有待后续核实。"}]},
            analysis_json={
                "model_output_status": "SUCCESS", "analysis_validation_status": "PARTIAL",
                "model": {"headline": "初步研判，仅供核对"},
                "scoring": {"trusted_score": 70, "dimensions": [{"dimension_code": "NEWS"}]},
            },
        )
        db.add(candidate)
        db.commit()
        app.dependency_overrides[get_db] = lambda: db
        client = TestClient(app)
        try:
            item = client.get(f"/api/v1/selection/runs/{run.id}").json()["candidates"][0]
            analysis = item["analysis_json"]
            assert analysis["conclusion_available"] is False
            assert analysis["provisional_conclusion_available"] is True
            assert analysis["conclusion_source"] == "LLM_EVIDENCE_UNVERIFIED"
            assert analysis["scoring_available"] is False
            assert "scoring" not in analysis
            assert analysis["model"]["headline"] == "初步研判，仅供核对"
        finally:
            client.close()
            app.dependency_overrides.pop(get_db, None)


def test_valid_candidate_llm_evaluation_is_generated_and_persisted(isolated):
    with isolated() as db:
        run = SelectionRun(
            market="CN_A", status="REVIEW", analysis_mode="MODEL_COMPLETED", data_mode="REAL",
            model_instance_code="TEST_MODEL",
        )
        db.add(run)
        db.flush()
        candidate = SelectionCandidate(
            run_id=run.id, market="CN_A", symbol="DEMO001", name="隔离样本", decision="PENDING",
            analysis_json={
                "model_output_status": "SUCCESS", "analysis_validation_status": "PASSED",
                "actual_instance_code": "TEST_MODEL",
                "scoring": valid_scorecard(),
                "model": {"decision": "WATCH", "confidence": .72, "headline": "证据已核验",
                          "target_price": 12, "reasoning": "基于已引用事实"},
            },
            evidence_json={"documents": [{"id": 123, "title": "已核验公告", "content": "公司发布经营进展公告，项目已通过验收，收入确认仍以财务披露为准。"}]},
        )
        db.add(candidate)
        db.commit()
        app.dependency_overrides[get_db] = lambda: db
        client = TestClient(app)
        evaluation_text = "评价" * 400
        log = SimpleNamespace(id=801, status="SUCCESS", response_text=evaluation_text, error_message=None)
        try:
            with patch("app.services.model_hub.ModelHubService.chat", return_value=log) as chat:
                response = client.post(f"/api/v1/selection/candidates/{candidate.id}/llm-evaluation", json={})
            assert response.status_code == 200, response.text
            result = response.json()
            assert result["text"] == evaluation_text
            assert result["model_instance_code"] == "TEST_MODEL"
            assert result["model_call_log_id"] == 801
            assert result["evidence_document_ids"] == [123]
            assert chat.call_args.kwargs["metadata_json"]["evidence_document_ids"] == [123]
            persisted = client.get(f"/api/v1/selection/candidates/{candidate.id}/llm-evaluation").json()
            assert persisted == result
        finally:
            client.close()
            app.dependency_overrides.pop(get_db, None)


def test_unprepared_acceptance_has_stable_empty_collections(isolated):
    with isolated() as db:
        app.dependency_overrides[get_db] = lambda: db
        try:
            client = TestClient(app)
            result = client.get("/api/v1/selection/acceptance").json()
            assert result["prepared"] is False and result["rows"] == []
            assert result["symbols"] == [] and result["datasets"] == {}
            client.close()
        finally:
            app.dependency_overrides.pop(get_db, None)


def test_whole_run_retry_preserves_failed_attempt_and_queues_new_run(isolated):
    with isolated() as db:
        original = queue_selection_run(db, request())
        original.status = "FAILED"
        original.error_message = "原始失败原因"
        db.commit()
        app.dependency_overrides[get_db] = lambda: db
        client = TestClient(app)
        try:
            response = client.post(f"/api/v1/selection/runs/{original.id}/retry")
            assert response.status_code == 202, response.text
            retried = response.json()
            assert retried["id"] != original.id and retried["status"] == "QUEUED"
            assert retried["criteria_json"]["retry_of_run_id"] == original.id
            assert retried["criteria_json"]["request"]["preflight_data_action"] == "REUSE_EXISTING"
            assert original.status == "FAILED" and original.error_message == "原始失败原因"
            assert client.post(f"/api/v1/selection/runs/{retried['id']}/retry").status_code == 409
        finally:
            client.close()
            app.dependency_overrides.pop(get_db, None)


def test_whole_run_retry_reuses_ready_candidate_data_without_remote_collection(isolated):
    with isolated() as db:
        original = queue_selection_run(db, request())
        original.status = "FAILED"
        original.error_message = "模型上下文超限"
        db.add(SelectionCandidate(
            run_id=original.id, market="CN_A", symbol="DEMO001",
            analysis_json={"model_output_status": "FAILED"},
            evidence_json={"documents": [{
                "id": 123, "title": "项目进展",
                "content": "公司披露项目已进入交付阶段，收入确认仍以实际验收和履约进度为准。",
                "content_state": "TEXT_AVAILABLE",
            }]},
        ))
        db.commit()
        app.dependency_overrides[get_db] = lambda: db
        client = TestClient(app)
        try:
            response = client.post(f"/api/v1/selection/runs/{original.id}/retry")
            assert response.status_code == 202, response.text
            retry_id = response.json()["id"]
            with patch("app.services.selection._prepare_selection_data") as prepare:
                execute_selection_job({"selection_run_id": retry_id})
            assert prepare.call_args.kwargs == {
                "symbols": ["DEMO001"], "force_refresh": False,
            }
        finally:
            client.close()
            app.dependency_overrides.pop(get_db, None)


def test_failed_stock_retry_keeps_successful_stock_evidence_and_rejects_duplicate(isolated):
    with isolated() as db:
        run = SelectionRun(market="CN_A", status="PARTIAL", analysis_mode="MODEL_PARTIAL", data_mode="REAL")
        db.add(run)
        db.flush()
        failed = SelectionCandidate(run_id=run.id, market="CN_A", symbol="DEMO001",
            analysis_json={"model_output_status": "FAILED", "model_attempts": [{"status": "FAILED"}]},
            evidence_json={"documents": [{"id": 123, "content": "原始证据"}]})
        good_evidence = {"documents": [{"id": 456, "source_table": "stock_news",
            "content": "公司已披露项目进展及交付计划，相关订单收入仍需依据实际履约进度确认。"}]}
        good = SelectionCandidate(run_id=run.id, market="CN_A", symbol="DEMO002",
            analysis_json={"model_output_status": "SUCCESS", "reasoning": "保留已成功结果",
                "model": {"reasoning": "保留已成功结果", "confidence": .7}, "scoring": valid_scorecard()},
            evidence_json=good_evidence)
        db.add_all([failed, good])
        db.commit()
        app.dependency_overrides[get_db] = lambda: db
        client = TestClient(app)
        calls = []
        stages = []
        refreshed_evidence = {"documents": [{"id": 124, "source_table": "stock_news",
            "content": "公司新披露订单签约及项目交付安排，收入影响以最终履约进度为准。"}]}

        def sync(_db, _run, **kwargs):
            stages.append("DATA")
            assert kwargs["symbols"] == ["DEMO001"]
            _progress(_db, _run, "DATA_SYNC", 25, "隔离采集完成")

        def probe(_db, _run):
            stages.append("MODEL_PREFLIGHT")
            _progress(_db, _run, "MODEL_PREFLIGHT", 42, "隔离模型测试完成")
            return None

        def analyze(_db, _run, candidates, **kwargs):
            stages.append("ANALYSIS")
            calls.extend(item["symbol"] for item, _score in candidates)
            for item, _score in candidates:
                assert item["evidence_json"]["documents"] == refreshed_evidence["documents"]
                item["analysis_json"] = {**item["analysis_json"], "model_output_status": "SUCCESS",
                                         "analysis_validation_status": "PARTIAL"}

        try:
            response = client.post(f"/api/v1/selection/runs/{run.id}/retry-failed")
            assert response.status_code == 202, response.text
            assert response.json()["status"] == "RUNNING"
            assert client.post(f"/api/v1/selection/runs/{run.id}/retry-failed").status_code == 409
            job = db.scalar(select(ScheduledJob).where(ScheduledJob.task_type == "selection_retry_failed"))
            assert job.payload_json["candidate_ids"] == [failed.id]
            with patch("app.services.selection._apply_model_analysis", side_effect=analyze), \
                    patch("app.services.selection._prepare_selection_data", side_effect=sync), \
                    patch("app.services.selection._preflight_selection_models", side_effect=probe), \
                    patch("app.services.selection._evidence", return_value=refreshed_evidence), \
                    patch("app.services.selection._load_disclosure_bodies"):
                result = execute_failed_selection_reanalysis(job.payload_json)
            assert calls == ["DEMO001"] and result["failed_count"] == 1
            assert stages == ["DATA", "MODEL_PREFLIGHT", "ANALYSIS"]
            db.expire_all()
            assert good.analysis_json == {"model_output_status": "SUCCESS", "reasoning": "保留已成功结果",
                "model": {"reasoning": "保留已成功结果", "confidence": .7}, "scoring": valid_scorecard()}
            assert good.evidence_json == good_evidence
            assert failed.evidence_json["documents"] == refreshed_evidence["documents"]
            prior = db.scalar(select(SelectionDecisionSnapshot).where(
                SelectionDecisionSnapshot.candidate_id == failed.id,
                SelectionDecisionSnapshot.snapshot_kind == "BEFORE_REANALYSIS"))
            assert prior is not None
            assert "原始证据" in str(prior.evidence_json)
            assert "scoring" not in failed.analysis_json
            assert failed.analysis_json["scoring_available"] is False
            assert run.status == "PARTIAL" and run.analysis_mode == "MODEL_PARTIAL"
            assert result["passed_count"] == 1 and result["citation_partial_count"] == 0
            assert run.criteria_json["retry_progress"]["passed_count"] == 1
            assert "有效结论 1 只、模型/结论/评分失败 1 只" in run.criteria_json["progress"]["message"]
            # The worker normally marks the completed job successful. Simulate
            # that lifecycle before retrying the remaining unverified result.
            job.status = "SUCCESS"
            db.commit()
            next_response = client.post(f"/api/v1/selection/runs/{run.id}/retry-failed")
            assert next_response.status_code == 202, next_response.text
            retry_jobs = list(db.scalars(select(ScheduledJob).where(ScheduledJob.task_type == "selection_retry_failed").order_by(ScheduledJob.id)))
            assert retry_jobs[-1].payload_json["candidate_ids"] == [failed.id]
        finally:
            client.close()
            app.dependency_overrides.pop(get_db, None)


def _model_switch_fixture(db):
    provider = ModelProvider(provider_code="SWITCH_PROVIDER", provider_name="切换测试服务", provider_type="OPENAI_COMPAT", enabled=True)
    db.add(provider)
    db.flush()
    db.add_all([
        ModelInstance(provider_id=provider.id, instance_code="ORIGINAL_MODEL", model_code="original", model_name="原模型", api_key="original-key"),
        ModelInstance(provider_id=provider.id, instance_code="BACKUP_MODEL", model_code="backup", model_name="备用模型", api_key="backup-key"),
    ])
    run = SelectionRun(market="CN_A", status="PARTIAL", analysis_mode="MODEL_PARTIAL", data_mode="REAL",
                       model_instance_code="ORIGINAL_MODEL", knowledge_base_ids_json=[], graph_ids_json=[],
                       criteria_json={"request": {"model_instance_code": "ORIGINAL_MODEL",
                                                   "model_instance_codes": ["ORIGINAL_MODEL", "BACKUP_MODEL"]}})
    db.add(run)
    db.flush()
    candidate = SelectionCandidate(run_id=run.id, market="CN_A", symbol="DEMO001", decision="PENDING",
        analysis_json={"model_output_status": "FAILED", "model_failure_reason": "HTTP 402：余额不足"},
        evidence_json={"documents": [{"id": 123, "content": "原始证据"}]})
    db.add(candidate)
    db.commit()
    return run, candidate


def test_model_switch_probe_failure_does_not_create_retry_job(isolated):
    with isolated() as db:
        run, _candidate = _model_switch_fixture(db)
        app.dependency_overrides[get_db] = lambda: db
        client = TestClient(app)
        failed_probe = SimpleNamespace(status="FAILED", error_message="HTTP 402：API 账户余额不足或付费额度不可用")
        try:
            with patch("app.services.model_hub.ModelHubService.chat", return_value=failed_probe):
                response = client.post(f"/api/v1/selection/runs/{run.id}/retry-failed-with-model",
                    json={"model_instance_code": "BACKUP_MODEL"})
            assert response.status_code == 422
            assert "余额不足" in response.json()["detail"]
            assert db.scalar(select(func.count(SelectionRun.id))) == 1
            assert db.scalar(select(func.count(ScheduledJob.id))) == 0
        finally:
            client.close()
            app.dependency_overrides.pop(get_db, None)


def test_model_switch_probe_success_creates_isolated_retry_batch(isolated):
    with isolated() as db:
        run, candidate = _model_switch_fixture(db)
        app.dependency_overrides[get_db] = lambda: db
        client = TestClient(app)
        successful_probe = SimpleNamespace(status="SUCCESS", id=701, error_message=None)
        try:
            with patch("app.services.model_hub.ModelHubService.chat", return_value=successful_probe):
                response = client.post(f"/api/v1/selection/runs/{run.id}/retry-failed-with-model",
                    json={"model_instance_code": "BACKUP_MODEL", "candidate_ids": [candidate.id]})
            assert response.status_code == 202, response.text
            switched = response.json()
            assert switched["id"] != run.id
            assert switched["model_instance_code"] == "BACKUP_MODEL"
            assert switched["criteria_json"]["retry_of_run_id"] == run.id
            assert switched["criteria_json"]["model_switch"]["preflight"]["call_log_id"] == 701
            assert switched["criteria_json"]["request"]["model_instance_code"] == "BACKUP_MODEL"
            assert switched["criteria_json"]["request"]["model_instance_codes"] == ["BACKUP_MODEL"]
            assert db.get(SelectionRun, run.id).model_instance_code == "ORIGINAL_MODEL"
            clone = db.scalar(select(SelectionCandidate).where(SelectionCandidate.run_id == switched["id"]))
            assert clone is not None and clone.symbol == "DEMO001"
            assert clone.evidence_json == candidate.evidence_json
            job = db.scalar(select(ScheduledJob).where(ScheduledJob.task_type == "selection_retry_failed"))
            assert job.payload_json["selection_run_id"] == switched["id"]
            assert job.payload_json["model_instance_code"] == "BACKUP_MODEL"
            assert job.payload_json["force_model_retry"] is True
        finally:
            client.close()
            app.dependency_overrides.pop(get_db, None)


def test_model_switch_reuses_analysis_model_order_and_selects_first_healthy(isolated):
    with isolated() as db:
        run, candidate = _model_switch_fixture(db)
        app.dependency_overrides[get_db] = lambda: db
        client = TestClient(app)
        try:
            with patch("app.api.selection.probe_selection_model") as probe:
                probe.side_effect = [ValueError("原模型不可用"), {"status": "SUCCESS", "call_log_id": 704}]
                response = client.post(
                    f"/api/v1/selection/runs/{run.id}/retry-failed-with-model",
                    json={"model_instance_codes": ["ORIGINAL_MODEL", "BACKUP_MODEL"],
                          "candidate_ids": [candidate.id]},
                )
            assert response.status_code == 202, response.text
            retried = response.json()
            assert retried["model_instance_code"] == "BACKUP_MODEL"
            assert retried["criteria_json"]["request"]["model_instance_codes"] == ["BACKUP_MODEL", "ORIGINAL_MODEL"]
            assert retried["criteria_json"]["model_switch"]["model_instance_codes"] == ["ORIGINAL_MODEL", "BACKUP_MODEL"]
            assert [call.args[1] for call in probe.call_args_list] == ["ORIGINAL_MODEL", "BACKUP_MODEL"]
        finally:
            client.close()
            app.dependency_overrides.pop(get_db, None)


def test_current_model_can_be_retried_after_context_repair(isolated):
    with isolated() as db:
        run, candidate = _model_switch_fixture(db)
        app.dependency_overrides[get_db] = lambda: db
        client = TestClient(app)
        successful_probe = SimpleNamespace(status="SUCCESS", id=702, error_message=None)
        try:
            with patch("app.services.model_hub.ModelHubService.chat", return_value=successful_probe):
                response = client.post(
                    f"/api/v1/selection/runs/{run.id}/retry-failed-with-model",
                    json={"model_instance_code": "ORIGINAL_MODEL", "candidate_ids": [candidate.id]},
                )
            assert response.status_code == 202, response.text
            retried = response.json()
            assert retried["id"] != run.id
            assert retried["model_instance_code"] == "ORIGINAL_MODEL"
            clone = db.scalar(select(SelectionCandidate).where(SelectionCandidate.run_id == retried["id"]))
            assert clone is not None
            assert clone.analysis_json["model_switch_from"] == "ORIGINAL_MODEL"
            assert clone.analysis_json["model_switch_to"] == "ORIGINAL_MODEL"
            assert "actual_instance_code" not in clone.analysis_json
            assert clone.analysis_json["model_attempts"] == []
        finally:
            client.close()
            app.dependency_overrides.pop(get_db, None)


def test_model_probe_rejects_truncated_connectivity_response(isolated):
    from app.services.selection import probe_selection_model

    with isolated() as db:
        _run, _candidate = _model_switch_fixture(db)
        truncated = SimpleNamespace(
            status="SUCCESS", id=703, error_message=None,
            response_json={"candidates": [{"finishReason": "MAX_TOKENS"}]},
        )
        with patch("app.services.model_hub.ModelHubService.chat", return_value=truncated), pytest.raises(
            ValueError, match="探测输出被截断"
        ):
            probe_selection_model(db, "ORIGINAL_MODEL")
