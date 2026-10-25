"""Durable selection tasks: HTTP acknowledgement, recovery and decision gates."""
from datetime import date, datetime, timezone
from types import SimpleNamespace
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, func, select
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.db.base import Base
from app.db.session import get_db
from app.main import app
from app.models.ai_hub import KnowledgeBase, KnowledgeGraph
from app.models.decision_review import SelectionDecisionSnapshot
from app.models.market_data import DataSource, StockKline, StockSymbol
from app.models.pipeline import ScheduledJob
from app.models.selection import SelectionCandidate, SelectionRun, SelectionTracking
from app.schemas.selection import SelectionReviewRequest, SelectionRunCreate
from app.services.selection import (
    SelectionCancelled, _apply_model_analysis, _date_window_filter, _raise_if_selection_cancelled, cancel_selection_run,
    execute_failed_selection_reanalysis, execute_selection_job, queue_selection_run,
    review_candidate, selection_preflight,
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


def test_preflight_reports_missing_security_without_creating_a_job(isolated):
    with isolated() as db:
        result = selection_preflight(db, SelectionRunCreate(
            symbols=["DOES_NOT_EXIST"], use_model=False, data_mode="MOCK",
        ))
        assert result["ready"] is False
        assert any("DOES_NOT_EXIST" in item for item in result["blockers"])
        assert db.scalar(select(func.count(SelectionRun.id))) == 0
        assert db.scalar(select(func.count(ScheduledJob.id))) == 0


def test_real_model_preflight_uses_a_short_probe_before_queueing(isolated):
    with isolated() as db:
        base = KnowledgeBase(kb_code="PREFLIGHT_KB", kb_name="預檢知識庫")
        db.add(base)
        db.flush()
        graph = KnowledgeGraph(knowledge_base_id=base.id, graph_code="PREFLIGHT_GRAPH",
            graph_name="預檢圖譜", governance_status="GOVERNED")
        db.add(graph)
        db.commit()
        route = SimpleNamespace(
            provider=SimpleNamespace(provider_type="OPENAI"),
            instance=SimpleNamespace(api_key="test-key", instance_code="test-model"),
        )
        with patch("app.services.model_hub.ModelHubService.list_route_options", return_value=[route]), \
             patch("app.services.model_hub.ModelHubService.chat", return_value=SimpleNamespace(
                 status="SUCCESS", instance_code="test-model")) as chat:
            result = selection_preflight(db, SelectionRunCreate(
                symbols=["DEMO001"], use_model=True, model_instance_code="test-model",
                knowledge_base_ids=[base.id], graph_ids=[graph.id], data_mode="REAL",
            ), probe_model=True)
        assert result["ready"] is True
        assert result["model_status"] == "READY"
        assert result["model_calls_estimate"] == 1
        assert chat.call_args.kwargs["max_tokens"] == 16
        assert chat.call_args.kwargs["metadata_json"]["preflight_only"] is True


def test_failed_real_model_preflight_blocks_analysis_without_creating_a_run(isolated):
    with isolated() as db:
        base = KnowledgeBase(kb_code="PREFLIGHT_FAIL_KB", kb_name="預檢失敗知識庫")
        db.add(base)
        db.flush()
        graph = KnowledgeGraph(knowledge_base_id=base.id, graph_code="PREFLIGHT_FAIL_GRAPH",
            graph_name="預檢失敗圖譜", governance_status="GOVERNED")
        db.add(graph)
        db.commit()
        route = SimpleNamespace(
            provider=SimpleNamespace(provider_type="OPENAI"),
            instance=SimpleNamespace(api_key="test-key", instance_code="test-model"),
        )
        with patch("app.services.model_hub.ModelHubService.list_route_options", return_value=[route]), \
             patch("app.services.model_hub.ModelHubService.chat", return_value=SimpleNamespace(
                 status="FAILED", error_message="service quota exhausted")):
            result = selection_preflight(db, SelectionRunCreate(
                symbols=["DEMO001"], use_model=True, model_instance_code="test-model",
                knowledge_base_ids=[base.id], graph_ids=[graph.id], data_mode="REAL",
            ), probe_model=True)
        assert result["ready"] is False
        assert result["model_status"] == "BLOCKED"
        assert any("service quota exhausted" in item for item in result["blockers"])
        assert db.scalar(select(func.count(SelectionRun.id))) == 0
        assert db.scalar(select(func.count(ScheduledJob.id))) == 0


def test_date_window_filter_keeps_iso_and_compact_dates_but_excludes_out_of_period_rows(isolated):
    with isolated() as db:
        source_id = db.scalar(select(DataSource.id).where(DataSource.source_code == "QUEUE_TEST"))
        db.add_all([
            StockSymbol(market="CN_A", symbol="DATE001", name="2025-01-05T09:30:00", exchange="DEMO", source_id=source_id),
            StockSymbol(market="CN_A", symbol="DATE002", name="20250106", exchange="DEMO", source_id=source_id),
            StockSymbol(market="CN_A", symbol="DATE003", name="2025-01-07", exchange="DEMO", source_id=source_id),
        ])
        db.commit()
        observed = set(db.scalars(select(StockSymbol.name).where(
            _date_window_filter(StockSymbol.name, date(2025, 1, 5), date(2025, 1, 6)),
        )).all())
        assert observed == {"2025-01-05T09:30:00", "20250106"}


def test_queued_selection_can_be_cancelled_without_removing_history(isolated):
    with isolated() as db:
        run = queue_selection_run(db, request())
        job = db.scalar(select(ScheduledJob).where(ScheduledJob.task_type == "selection_analysis"))
        cancelled = cancel_selection_run(db, run.id)
        assert cancelled.status == "CANCELLED"
        assert job.status == "CANCELLED"
        assert db.get(SelectionRun, run.id) is not None
        assert "cancel_requested_at" in cancelled.criteria_json


def test_running_selection_cancellation_is_observed_by_stale_worker_session(isolated):
    worker_factory = isolated
    with worker_factory() as worker_db:
        run = SelectionRun(market="CN_A", status="RUNNING", criteria_json={
            "progress": {"percent": 40},
            "backfill_progress": {"status": "RUNNING", "percent": 25},
            "retry_progress": {"status": "RUNNING", "percent": 50},
        })
        worker_db.add(run)
        worker_db.commit()
        run_id = run.id
        with worker_factory() as api_db:
            cancel_selection_run(api_db, run_id)
        with pytest.raises(SelectionCancelled):
            _raise_if_selection_cancelled(worker_db, run)
        worker_db.expire_all()
        cancelled = worker_db.get(SelectionRun, run_id)
        assert cancelled.status == "CANCELLED"
        assert cancelled.criteria_json["backfill_progress"]["status"] == "CANCELLED"
        assert cancelled.criteria_json["retry_progress"]["status"] == "CANCELLED"


def test_cancel_after_model_response_does_not_start_single_stock_recovery_calls(isolated):
    with isolated() as db:
        run = SelectionRun(market="CN_A", status="RUNNING", analysis_mode="MODEL_REQUESTED",
            data_mode="REAL", model_instance_code="test-model", criteria_json={})
        db.add(run)
        db.commit()

        def candidate(symbol):
            return ({"market": "CN_A", "symbol": symbol, "name": symbol,
                "entry_price": 10.0, "entry_date": "2025-01-06", "hard_score": 1.0,
                "hard_rules_json": {}, "evidence_json": {"documents": [], "facts": [],
                    "chunks": [], "graph_paths": [], "relations": [], "missing_data": []},
                "analysis_json": {}}, 1.0)

        with patch("app.services.selection._raise_if_selection_cancelled",
                   side_effect=[None, SelectionCancelled("cancelled")]), \
             patch("app.services.model_hub.ModelHubService.chat", return_value=SimpleNamespace(
                 status="SUCCESS", response_text="{}", id=1, instance_code="test-model")) as chat:
            with pytest.raises(SelectionCancelled):
                _apply_model_analysis(db, run, [candidate("DEMO001"), candidate("DEMO002")])
        chat.assert_called_once()


def test_batch_acceptance_is_scoped_to_current_runs_candidates(isolated):
    with isolated() as db:
        run = SelectionRun(market="CN_A", status="PARTIAL", analysis_mode="MODEL_COMPLETED")
        db.add(run)
        db.flush()
        first = SelectionCandidate(run_id=run.id, market="CN_A", symbol="DEMO001", name="Demo",
            entry_price=11, entry_date="2025-01-06",
            evidence_json={"documents": [{"id": 1}], "facts": [{"id": "F1"}], "chunks": [],
                           "graph_paths": [{"id": 3}], "lineage": [{"id": 4}]},
            analysis_json={"model_output_status": "SUCCESS", "analysis_validation_status": "PASSED"})
        second = SelectionCandidate(run_id=run.id, market="CN_A", symbol="DEMO002", name="Demo 2",
            evidence_json={"documents": [], "facts": []},
            analysis_json={"model_output_status": "FAILED"})
        db.add_all([first, second])
        db.commit()
        app.dependency_overrides[get_db] = lambda: db
        client = TestClient(app)
        try:
            result = client.get(f"/api/v1/selection/runs/{run.id}/acceptance").json()
            assert result["candidate_count"] == 2
            assert result["accepted_count"] == 1
            assert result["needs_attention_count"] == 1
            assert {row["symbol"] for row in result["rows"]} == {"DEMO001", "DEMO002"}
        finally:
            client.close()
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
            assert original.status == "FAILED" and original.error_message == "原始失败原因"
            assert client.post(f"/api/v1/selection/runs/{retried['id']}/retry").status_code == 409
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
        good = SelectionCandidate(run_id=run.id, market="CN_A", symbol="DEMO002",
            analysis_json={"model_output_status": "SUCCESS", "reasoning": "保留已成功结果"},
            evidence_json={"documents": [{"id": 456}]})
        db.add_all([failed, good])
        db.commit()
        app.dependency_overrides[get_db] = lambda: db
        client = TestClient(app)
        calls = []

        def analyze(_db, _run, candidates, **kwargs):
            calls.extend(item["symbol"] for item, _score in candidates)
            for item, _score in candidates:
                assert item["evidence_json"] == {"documents": [{"id": 123, "content": "原始证据"}]}
                item["analysis_json"] = {**item["analysis_json"], "model_output_status": "SUCCESS",
                                         "analysis_validation_status": "PARTIAL"}

        try:
            response = client.post(f"/api/v1/selection/runs/{run.id}/retry-failed")
            assert response.status_code == 202, response.text
            assert response.json()["status"] == "RUNNING"
            assert client.post(f"/api/v1/selection/runs/{run.id}/retry-failed").status_code == 409
            job = db.scalar(select(ScheduledJob).where(ScheduledJob.task_type == "selection_retry_failed"))
            assert job.payload_json["candidate_ids"] == [failed.id]
            with patch("app.services.selection._apply_model_analysis", side_effect=analyze):
                result = execute_failed_selection_reanalysis(job.payload_json)
            assert calls == ["DEMO001"] and result["failed_count"] == 0
            db.expire_all()
            assert good.analysis_json == {"model_output_status": "SUCCESS", "reasoning": "保留已成功结果"}
            assert good.evidence_json == {"documents": [{"id": 456}]}
            assert failed.evidence_json == {"documents": [{"id": 123, "content": "原始证据"}]}
            assert run.status == "PARTIAL" and run.analysis_mode == "MODEL_PARTIAL"
            assert result["passed_count"] == 1 and result["citation_partial_count"] == 1
            assert run.criteria_json["retry_progress"]["passed_count"] == 1
            assert "引用校验通过 1 只、待核对 1 只、执行失败 0 只" in run.criteria_json["progress"]["message"]
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
