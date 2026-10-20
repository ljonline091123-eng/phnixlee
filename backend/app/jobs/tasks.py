"""Task registry kept independent from HTTP routes."""

from __future__ import annotations

from collections.abc import Callable
from typing import Any

from app.orchestration.background import MasterDataSyncWorkflow, PredictionReviewWorkflow
from app.orchestration.watch_pool import WatchPoolRefreshWorkflow
from app.orchestration.foundation import LegacyEvidenceArchiveWorkflow
from app.orchestration.company_governance import CompanyGovernanceWorkflow
from app.services.selection import refresh_tracking, execute_selection_job
from app.services.change_detection import run_change_detection
from app.services.stock_batch import TASK_TYPE as STOCK_BATCH_TASK, execute_stock_batch_governance
from app.services.investor_qa import TASK_TYPE as QA_TASK, execute_qa_sync
from app.services.document_semantic_skills import TEST_TASK_TYPE, execute_test_job
from app.db.session import SessionLocal


TaskHandler = Callable[[dict[str, Any]], dict[str, Any]]


TASK_HANDLERS: dict[str, TaskHandler] = {
    "selection_analysis": execute_selection_job,
    "company_graph_governance": CompanyGovernanceWorkflow().execute,
    'foundation_archive_evidence': LegacyEvidenceArchiveWorkflow().execute,
    "daily_master_sync": MasterDataSyncWorkflow().execute,
    "daily_prediction_review": PredictionReviewWorkflow().execute,
    "daily_selection_tracking_refresh": lambda payload: _refresh_selection_tracking(payload),
    "watch_pool_data_refresh": WatchPoolRefreshWorkflow().execute,
    "l0_change_detection": lambda payload: _run_l0_change_detection(payload),
    STOCK_BATCH_TASK: execute_stock_batch_governance,
    QA_TASK: execute_qa_sync,
    TEST_TASK_TYPE: execute_test_job,
}


def _refresh_selection_tracking(payload: dict[str, Any]) -> dict[str, Any]:
    with SessionLocal() as db:
        return {"workflow": "daily_selection_tracking_refresh", **refresh_tracking(db, list(payload.get("tracking_ids") or []))}


def _run_l0_change_detection(payload: dict[str, Any]) -> dict[str, Any]:
    with SessionLocal() as db:
        return run_change_detection(db, as_of=payload.get("business_date"))


def execute_task(task_type: str, payload: dict[str, Any]) -> dict[str, Any]:
    try:
        handler = TASK_HANDLERS[task_type]
    except KeyError as exc:
        raise LookupError(f"No background task registered for {task_type}") from exc
    return handler(payload)
