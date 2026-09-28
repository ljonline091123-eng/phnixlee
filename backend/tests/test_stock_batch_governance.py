from __future__ import annotations

from pathlib import Path
from tempfile import TemporaryDirectory

import pytest
from fastapi import FastAPI, HTTPException
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session, sessionmaker

from app.api.stock_batch import (
    _enrich_stage_results,
    get_stock_batch_governance_job,
    list_stock_batch_governance_jobs,
    router as stock_batch_router,
    submit_stock_batch_governance,
)
from app.db.base import Base
from app.db.session import get_db
from app.jobs.dispatcher import DatabaseJobDispatcher
from app.jobs.tasks import TASK_HANDLERS
from app.models.market_data import DataSource, StockSymbol
from app.models.pipeline import PipelineRun, ScheduledJob
from app.schemas.stock_batch import StockBatchGovernanceRequest
from app.services import stock_batch


def _database():
    temporary = TemporaryDirectory()
    path = Path(temporary.name) / "stock-batch.db"
    engine = create_engine(f"sqlite:///{path}")
    Base.metadata.create_all(engine)
    sessions = sessionmaker(bind=engine)
    with sessions() as db:
        source = DataSource(
            source_code="AKSHARE", source_name="测试数据源", adapter_type="AKSHARE",
            config_json={
                "market_scope": ["CN_A", "HK"],
                "capabilities": ["QUOTE", "KLINE", "FINANCIAL", "NEWS", "NOTICE", "F10"],
            },
        )
        db.add(source)
        db.flush()
        db.add_all([
            StockSymbol(
                market="CN_A", symbol="000001", exchange="SZSE", name="平安银行",
                source_id=source.id,
            ),
            StockSymbol(
                market="HK", symbol="00700", exchange="HKEX", name="腾讯控股",
                source_id=source.id,
            ),
        ])
        db.commit()
    return temporary, engine, sessions


def test_schema_deduplicates_targets_and_expands_graph_dependencies() -> None:
    request = StockBatchGovernanceRequest.model_validate({
        "stocks": [
            {"market": "cn_a", "symbol": "000001"},
            {"market": "CN_A", "symbol": "000001"},
        ],
        "collect_business_data": False,
        "export_lakehouse": False,
        "archive_chunks": False,
        "run_graph": True,
    })
    assert [(item.market, item.symbol) for item in request.stocks] == [("CN_A", "000001")]
    assert request.export_lakehouse is True
    assert request.archive_chunks is True

    with pytest.raises(ValueError):
        StockBatchGovernanceRequest.model_validate({
            "stocks": [{"market": "CN_A", "symbol": "000001"}],
            "collect_business_data": False,
            "export_lakehouse": False,
            "archive_chunks": False,
            "run_graph": False,
            "run_agent_governance": False,
        })


def test_submit_is_idempotent_and_exposes_worker_contract() -> None:
    temporary, engine, sessions = _database()
    try:
        with sessions() as db:
            request = StockBatchGovernanceRequest.model_validate({
                "stocks": [{"market": "CN_A", "symbol": "1"}],
                "idempotency_key": "screen-selection-1",
                "run_graph": False,
                "archive_chunks": False,
                "export_lakehouse": False,
            })
            first = submit_stock_batch_governance(request, db)
            second = submit_stock_batch_governance(request, db)
            assert first["job_id"] == second["job_id"]
            assert first["stock_count"] == 1
            assert first["current_stage"] == "QUEUED"
            assert db.scalar(select(ScheduledJob)).task_type == stock_batch.TASK_TYPE
            assert stock_batch.TASK_TYPE in TASK_HANDLERS

            detail = get_stock_batch_governance_job(first["job_id"], db)
            assert detail["worker_required"] is True
            assert detail["worker_task_type"] == "stock_batch_governance"
            assert "--task-type stock_batch_governance" in detail["worker_command"]
            listing = list_stock_batch_governance_jobs(limit=20, db=db)
            assert listing["total"] == 1
            assert listing["items"][0]["job_id"] == first["job_id"]

            job = db.get(ScheduledJob, first["job_id"])
            run = db.get(PipelineRun, job.pipeline_run_id)
            job.status = "COMPLETED"
            run.output_json = {"result_status": "PARTIAL", "progress": 100}
            db.commit()
            completed = get_stock_batch_governance_job(first["job_id"], db)
            assert completed["status"] == "PARTIAL"
            assert completed["job_status"] == "COMPLETED"

            different = StockBatchGovernanceRequest.model_validate({
                "stocks": [{"market": "HK", "symbol": "700"}],
                "idempotency_key": "screen-selection-1",
                "run_graph": False,
                "archive_chunks": False,
                "export_lakehouse": False,
            })
            with pytest.raises(HTTPException) as exc:
                submit_stock_batch_governance(different, db)
            assert exc.value.status_code == 409
    finally:
        engine.dispose()
        temporary.cleanup()


def test_http_submit_response_keeps_worker_requirements() -> None:
    temporary, engine, sessions = _database()
    try:
        app = FastAPI()
        app.include_router(stock_batch_router, prefix="/api/v1")

        def override_db():
            with sessions() as db:
                yield db

        app.dependency_overrides[get_db] = override_db
        with TestClient(app) as client:
            response = client.post("/api/v1/stocks/batch-governance/jobs", json={
                "stocks": [{"market": "CN_A", "symbol": "1"}],
                "collect_business_data": True,
                "business_types": ["QUOTE"],
                "export_lakehouse": False,
                "archive_chunks": False,
                "run_graph": False,
            })
        assert response.status_code == 202
        body = response.json()
        assert body["worker_required"] is True
        assert body["worker_task_type"] == "stock_batch_governance"
        assert body["job_status"] == "PENDING"
        assert body["status"] == "PENDING"
    finally:
        engine.dispose()
        temporary.cleanup()


def test_worker_groups_cross_market_knowledge_runs_and_persists_progress(monkeypatch) -> None:
    temporary, engine, sessions = _database()
    try:
        request = StockBatchGovernanceRequest.model_validate({
            "stocks": [
                {"market": "CN_A", "symbol": "000001"},
                {"market": "HK", "symbol": "00700"},
            ],
            "business_types": ["QUOTE"],
        }).model_dump(exclude={"idempotency_key"})
        with sessions() as db:
            job = DatabaseJobDispatcher(db).enqueue(
                task_type=stock_batch.TASK_TYPE,
                idempotency_key="stock-batch-test",
                payload={"request": request},
            )
            job.payload_json = {
                "request": request,
                "orchestration_job_id": job.id,
                "orchestration_pipeline_run_id": job.pipeline_run_id,
            }
            payload = dict(job.payload_json)
            run_id = job.pipeline_run_id
            db.commit()

        monkeypatch.setattr(stock_batch, "SessionLocal", sessions)
        monkeypatch.setattr(stock_batch, "_collect_stock", lambda _db, stock, **_kwargs: {
            "market": stock.market,
            "symbol": stock.symbol,
            "name": stock.name,
            "status": "SUCCESS",
            "operations": {"QUOTE": {"status": "SUCCESS"}},
            "errors": [],
        })
        calls: list[tuple[str, tuple[str, ...], str, bool]] = []

        def run_pipeline(_db, **kwargs):
            calls.append((
                kwargs["market"], tuple(kwargs["symbols"]), kwargs["idempotency_key"],
                kwargs["run_agent_governance"],
            ))
            number = 101 if kwargs["market"] == "CN_A" else 102
            return {
                "pipeline_run_id": number,
                "status": "COMPLETED",
                "graph_id": number + 100,
                "base_graph_id": 1,
                "completion": {"status": "COMPLETED"},
                "lineage_batch_id": f"lineage-{number}",
                "exports": {"stock_symbol": {"status": "PUBLISHED"}},
                "chunks": {
                    "documents_total": 1, "documents_with_chunks": 1,
                    "failed_documents": 0, "truncated": False, "coverage_status": "COMPLETE",
                },
                "graph": {
                    "status": "BUILT", "projection": True, "graph_id": number + 100,
                },
            }

        monkeypatch.setattr(stock_batch, "run_stock_pipeline", run_pipeline)
        result = stock_batch.execute_stock_batch_governance(payload)
        assert result["result_status"] == "PARTIAL"
        assert result["progress"] == 100
        assert result["pipeline_run_ids"] == [101, 102]
        assert result["graph_ids"] == [201, 202]
        assert {item[0] for item in calls} == {"CN_A", "HK"}
        assert len({item[2] for item in calls}) == 2
        assert sum(item[3] for item in calls) == 0
        assert result["stage_results"]["LAKEHOUSE_EXPORT"]["status"] == "SUCCESS"
        assert result["stage_results"]["DOCUMENT_CHUNKS"]["status"] == "SUCCESS"
        assert result["stage_results"]["KNOWLEDGE_GRAPH"]["status"] == "PARTIAL"
        assert result["stage_results"]["KNOWLEDGE_GRAPH"]["market_runs"][0]["build_status"] == "BUILT"

        with sessions() as db:
            run = db.get(PipelineRun, run_id)
            assert run.current_stage == "COMPLETE"
            assert run.output_json["progress"] == 100
    finally:
        engine.dispose()
        temporary.cleanup()


def test_chunk_only_run_is_reported_as_existing_documents_only(monkeypatch) -> None:
    temporary, engine, sessions = _database()
    try:
        request = StockBatchGovernanceRequest.model_validate({
            "stocks": [{"market": "CN_A", "symbol": "000001"}],
            "collect_business_data": False,
            "export_lakehouse": True,
            "archive_chunks": True,
            "run_graph": False,
        }).model_dump(exclude={"idempotency_key"})
        monkeypatch.setattr(stock_batch, "SessionLocal", sessions)
        monkeypatch.setattr(stock_batch, "run_stock_pipeline", lambda _db, **_kwargs: {
            "pipeline_run_id": 7,
            "status": "COMPLETED",
            "graph_id": 8,
            "completion": {"status": "COMPLETED"},
        })
        result = stock_batch.execute_stock_batch_governance({"request": request})
        stage = result["stage_results"]["KNOWLEDGE_PIPELINE"]
        assert result["result_status"] == "PARTIAL"
        assert stage["status"] == "PARTIAL"
        assert stage["knowledge_base_mode"] == "EXISTING_DOCUMENTS_ONLY"
        assert "既有知识文档" in stage["message"]
        assert result["graph_ids"] == []
    finally:
        engine.dispose()
        temporary.cleanup()


def test_f10_refresh_does_not_implicitly_fetch_core_business_types(monkeypatch) -> None:
    temporary, engine, sessions = _database()
    try:
        with sessions() as db:
            stock = db.scalar(select(StockSymbol).where(StockSymbol.market == "CN_A"))
            f10_calls: list[tuple[str, str]] = []
            core_calls: list[str] = []
            monkeypatch.setattr(stock_batch, "_fetch_and_cache_f10_extended_data", lambda _db, _source, market, symbol: (
                f10_calls.append((market, symbol)) or {
                    "profile": {"fields": {"上市日期": "1991-04-03"}},
                    "holders": {}, "fund_flow": {}, "financial_summary": {},
                    "financial_statements": {}, "business_composition": {},
                    "published_reports": {"reports": []},
                }
            ))
            monkeypatch.setattr(stock_batch, "_fetch_one", lambda _db, **kwargs: (
                core_calls.append(kwargs["data_type"]) or {"status": "SUCCESS"}
            ))
            result = stock_batch._collect_stock(
                db,
                stock,
                business_types=["QUOTE", "KLINE", "FINANCIAL", "NEWS", "NOTICE", "F10"],
                kline_days=30,
                disclosure_days=90,
            )
            assert result["status"] == "SUCCESS"
            assert f10_calls == [("CN_A", "000001")]
            assert core_calls == ["QUOTE", "KLINE", "FINANCIAL", "NEWS", "NOTICE"]
    finally:
        engine.dispose()
        temporary.cleanup()


def test_empty_f10_placeholder_is_not_reported_as_success(monkeypatch) -> None:
    temporary, engine, sessions = _database()
    try:
        with sessions() as db:
            monkeypatch.setattr(stock_batch, "_fetch_and_cache_f10_extended_data", lambda *_args: {
                "profile": {"fields": {}, "message": "暂无缓存"},
                "holders": {"message": "暂无缓存"},
                "fund_flow": {"rows": [], "message": "暂无缓存"},
                "financial_summary": {"rows": [], "message": "暂无缓存"},
                "financial_statements": {"message": "暂无缓存"},
                "business_composition": {"message": "暂无缓存"},
                "published_reports": {"reports": [], "message": "暂无缓存"},
            })
            result = stock_batch._run_f10_refresh(db, "CN_A", "000001")
            assert result["status"] == "PARTIAL"
            assert result["section_count"] == 0
            assert result["sections"] == []
    finally:
        engine.dispose()
        temporary.cleanup()


def test_global_agent_governance_runs_once_for_cross_market_batch(monkeypatch) -> None:
    temporary, engine, sessions = _database()
    try:
        request = StockBatchGovernanceRequest.model_validate({
            "stocks": [
                {"market": "CN_A", "symbol": "000001"},
                {"market": "HK", "symbol": "00700"},
            ],
            "collect_business_data": False,
            "export_lakehouse": True,
            "archive_chunks": False,
            "run_graph": False,
            "run_agent_governance": True,
        }).model_dump(exclude={"idempotency_key"})
        monkeypatch.setattr(stock_batch, "SessionLocal", sessions)
        agent_flags: list[bool] = []

        def pipeline(_db, **kwargs):
            agent_flags.append(kwargs["run_agent_governance"])
            return {
                "pipeline_run_id": len(agent_flags),
                "status": "COMPLETED",
                "exports": {"stock_symbol": {"status": "PUBLISHED"}},
                "chunks": {},
                "graph": {"status": "NOT_REQUESTED"},
                "agent_governance": {
                    "status": "COMPLETED" if kwargs["run_agent_governance"] else "NOT_REQUESTED",
                    "run_id": 88 if kwargs["run_agent_governance"] else None,
                },
                "completion": {"status": "COMPLETED"},
            }

        monkeypatch.setattr(stock_batch, "run_stock_pipeline", pipeline)
        result = stock_batch.execute_stock_batch_governance({"request": request})
        assert agent_flags.count(True) == 1
        assert agent_flags.count(False) == 1
        agent_stage = result["stage_results"]["AGENT_SKILL_GOVERNANCE"]
        assert agent_stage["status"] == "SUCCESS"
        assert agent_stage["scope"] == "GLOBAL_BOUNDED_SOURCE_AUDIT"
        assert len(agent_stage["runs"]) == 1
    finally:
        engine.dispose()
        temporary.cleanup()


def test_historical_chunk_detail_infers_selection_and_explains_chunk_only_mode() -> None:
    historical = _enrich_stage_results({
        "stage_results": {
            "DOCUMENT_CHUNKS": {
                "status": "SUCCESS",
                "market_runs": [{
                    "market": "CN_A",
                    "status": "SUCCESS",
                    "documents_total": 32,
                    "documents_with_chunks": 32,
                    "coverage_status": "COMPLETE",
                    "knowledge_base_mode": "SCOPED_GRAPH_DOCUMENTS_AND_CHUNKS",
                }],
            },
        },
    })["DOCUMENT_CHUNKS"]
    assert {item["code"] for item in historical["completed_items"]} == {
        "CN_A:DOCUMENT_SELECTION", "CN_A:CHUNK_COVERAGE",
    }
    assert historical["incomplete_items"] == []

    chunk_only = _enrich_stage_results({
        "stage_results": {
            "DOCUMENT_CHUNKS": {
                "status": "PARTIAL",
                "knowledge_base_mode": "EXISTING_DOCUMENTS_ONLY",
                "market_runs": [{
                    "market": "CN_A",
                    "status": "PARTIAL",
                    "documents_total": 2,
                    "documents_with_chunks": 2,
                    "knowledge_base_mode": "EXISTING_DOCUMENTS_ONLY",
                }],
            },
        },
    })["DOCUMENT_CHUNKS"]
    assert {item["code"] for item in chunk_only["completed_items"]} == {
        "CN_A:DOCUMENT_SELECTION", "CN_A:CHUNK_COVERAGE",
    }
    assert [item["code"] for item in chunk_only["incomplete_items"]] == [
        "CN_A:NEW_DATA_MATERIALIZATION",
    ]


def test_job_detail_preserves_verifiable_results_for_every_stage(monkeypatch) -> None:
    """A terminal task must retain enough facts to explain and verify each stage.

    The dialog is not allowed to reduce a partial result to a colour and a
    generic sentence.  Business-data operations, dataset publication status,
    chunk coverage/failures and graph build/governance facts must survive the
    worker -> PipelineRun -> job-detail round trip.
    """
    temporary, engine, sessions = _database()
    try:
        request = StockBatchGovernanceRequest.model_validate({
            "stocks": [{"market": "CN_A", "symbol": "000001"}],
            "collect_business_data": True,
            "business_types": ["QUOTE", "KLINE", "FINANCIAL", "NEWS", "NOTICE", "F10"],
            "export_lakehouse": True,
            "archive_chunks": True,
            "run_graph": True,
        }).model_dump(exclude={"idempotency_key"})
        with sessions() as db:
            job = DatabaseJobDispatcher(db).enqueue(
                task_type=stock_batch.TASK_TYPE,
                idempotency_key="verifiable-stage-detail",
                payload={"request": request},
            )
            job.payload_json = {
                "request": request,
                "orchestration_job_id": job.id,
                "orchestration_pipeline_run_id": job.pipeline_run_id,
            }
            payload = dict(job.payload_json)
            job_id = job.id
            db.commit()

        operations = {
            "QUOTE": {
                "status": "SUCCESS", "source_code": "QUOTE_SOURCE",
                "fetch_log_id": 101, "total_count": 1, "persisted_count": 1,
            },
            "KLINE": {
                "status": "SUCCESS", "source_code": "KLINE_SOURCE",
                "fetch_log_id": 102, "total_count": 30, "persisted_count": 30,
            },
            "FINANCIAL": {
                "status": "SUCCESS", "source_code": "FIN_SOURCE",
                "fetch_log_id": 103, "total_count": 8, "persisted_count": 8,
            },
            "NEWS": {
                "status": "SUCCESS", "source_code": "NEWS_SOURCE",
                "fetch_log_id": 104, "total_count": 3, "persisted_count": 3,
            },
            "NOTICE": {
                "status": "FAILED", "source_code": "NOTICE_SOURCE",
                "fetch_log_id": 105, "total_count": 0, "persisted_count": 0,
                "error": "上游公告接口限流",
            },
            "F10": {
                "status": "PARTIAL", "source_code": "F10_SOURCE",
                "section_count": 2, "sections": ["profile", "holders"],
                "message": "部分F10分区暂未返回",
            },
        }

        monkeypatch.setattr(stock_batch, "SessionLocal", sessions)
        monkeypatch.setattr(stock_batch, "_collect_stock", lambda _db, stock, **_kwargs: {
            "market": stock.market,
            "symbol": stock.symbol,
            "name": stock.name,
            "status": "PARTIAL",
            "operations": operations,
            "errors": [{
                "data_type": "NOTICE", "message": "上游公告接口限流", "fetch_log_id": 105,
            }],
        })

        def run_pipeline(_db, **_kwargs):
            return {
                "pipeline_run_id": 201,
                "status": "PARTIAL",
                "graph_id": 301,
                "base_graph_id": 11,
                "lineage_batch_id": "lineage-verification-1",
                "exports": {
                    "stock_symbol": {
                        "status": "PUBLISHED", "dataset_id": 41,
                        "dataset_code": "pipeline_stock_symbol_normalized",
                        "version": "v7", "row_count": 1,
                    },
                    "stock_notice": {
                        "status": "SKIPPED", "error": "当前范围没有公告记录",
                    },
                },
                "chunks": {
                    "documents": 5,
                    "documents_total": 8,
                    "documents_selected": 5,
                    "documents_with_chunks": 4,
                    "coverage_status": "PARTIAL",
                    "coverage_ratio": 0.625,
                    "document_selection_ratio": 0.625,
                    "successful_chunk_ratio": 0.8,
                    "truncated": True,
                    "selection_policy": "LATEST_PER_SECURITY_THEN_PRIORITY_EVIDENCE_THEN_SOURCE_ROUND_ROBIN",
                    "failed_documents": 1,
                    "failed_document_samples": [{
                        "document_id": 88, "status": "FAILED", "error": "文档解析失败",
                    }],
                    "created_chunk_count": 12,
                    "reused_chunk_count": 3,
                },
                "graph": {
                    "status": "BUILT", "projection": True, "graph_id": 301,
                    "base_graph_id": 11,
                    "counts": {"documents": 8, "entities": 20, "relations": 19},
                },
                "completion": {
                    "status": "PARTIAL",
                    "graph_governance_status": "PENDING",
                    "issues": [
                        {
                            "code": "LAKEHOUSE_EXPORT_INCOMPLETE",
                            "stage": "LAKEHOUSE_EXPORT",
                            "detail": ["stock_notice"],
                        },
                        {
                            "code": "CHUNK_SELECTION_TRUNCATED",
                            "stage": "DOCUMENT_CHUNKS",
                            "detail": {"selected": 5, "total": 8},
                        },
                        {
                            "code": "GRAPH_PENDING_GOVERNANCE",
                            "stage": "KNOWLEDGE_GRAPH",
                            "detail": "PENDING",
                        },
                    ],
                    "next_action": "先补齐湖仓和切片，再治理范围图谱",
                },
            }

        monkeypatch.setattr(stock_batch, "run_stock_pipeline", run_pipeline)
        result = stock_batch.execute_stock_batch_governance(payload)
        assert result["result_status"] == "PARTIAL"

        with sessions() as db:
            job = db.get(ScheduledJob, job_id)
            job.status = "COMPLETED"
            db.commit()
            detail = get_stock_batch_governance_job(job_id, db)

        assert detail["status"] == "PARTIAL"
        stock_detail = detail["stock_results"][0]
        assert set(stock_detail["operations"]) == {
            "QUOTE", "KLINE", "FINANCIAL", "NEWS", "NOTICE", "F10",
        }
        assert stock_detail["operations"]["NOTICE"]["error"] == "上游公告接口限流"
        assert stock_detail["operations"]["F10"]["sections"] == ["profile", "holders"]

        stages = detail["stage_results"]
        for stage_code in ("BUSINESS_DATA", "LAKEHOUSE_EXPORT", "DOCUMENT_CHUNKS", "KNOWLEDGE_GRAPH"):
            stage = stages[stage_code]
            assert "completed_items" in stage
            assert "incomplete_items" in stage
            assert "pending_items" in stage  # compatibility alias for older UI clients
            assert "reasons" in stage
            assert "verification" in stage
            assert stage.get("verify_target")

        lakehouse_run = stages["LAKEHOUSE_EXPORT"]["market_runs"][0]
        assert lakehouse_run["dataset_statuses"] == {
            "stock_symbol": "PUBLISHED", "stock_notice": "SKIPPED",
        }
        assert lakehouse_run["datasets"]["stock_symbol"]["dataset_id"] == 41
        assert lakehouse_run["datasets"]["stock_notice"]["error"] == "当前范围没有公告记录"

        chunk_run = stages["DOCUMENT_CHUNKS"]["market_runs"][0]
        assert chunk_run["documents_selected"] == 5
        assert chunk_run["documents_with_chunks"] == 4
        assert chunk_run["coverage_ratio"] == 0.625
        assert chunk_run["truncated"] is True
        assert chunk_run["failed_documents"] == 1
        assert chunk_run["failed_document_samples"][0]["document_id"] == 88
        assert chunk_run["selection_policy"].startswith("LATEST_PER_SECURITY")

        graph_run = stages["KNOWLEDGE_GRAPH"]["market_runs"][0]
        assert graph_run["build_status"] == "BUILT"
        assert graph_run["governance_status"] == "PENDING"
        assert graph_run["counts"] == {"documents": 8, "entities": 20, "relations": 19}
        assert graph_run["completion_issues"][-1]["code"] == "GRAPH_PENDING_GOVERNANCE"
        assert graph_run["next_action"] == "先补齐湖仓和切片，再治理范围图谱"
    finally:
        engine.dispose()
        temporary.cleanup()
