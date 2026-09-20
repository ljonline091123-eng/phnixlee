"""Task registry kept independent from HTTP routes."""

from __future__ import annotations

from collections.abc import Callable
from typing import Any

from app.orchestration.background import MasterDataSyncWorkflow, PredictionReviewWorkflow
from app.orchestration.foundation import LegacyEvidenceArchiveWorkflow
from app.services.selection import refresh_tracking
from app.db.session import SessionLocal


TaskHandler = Callable[[dict[str, Any]], dict[str, Any]]


TASK_HANDLERS: dict[str, TaskHandler] = {
    'foundation_archive_evidence': LegacyEvidenceArchiveWorkflow().execute,
    "daily_master_sync": MasterDataSyncWorkflow().execute,
    "daily_prediction_review": PredictionReviewWorkflow().execute,
    "daily_selection_tracking_refresh": lambda payload: _refresh_selection_tracking(payload),
}


def _refresh_selection_tracking(payload: dict[str, Any]) -> dict[str, Any]:
    with SessionLocal() as db:
        return {"workflow": "daily_selection_tracking_refresh", **refresh_tracking(db, list(payload.get("tracking_ids") or []))}


def execute_task(task_type: str, payload: dict[str, Any]) -> dict[str, Any]:
    try:
        handler = TASK_HANDLERS[task_type]
    except KeyError as exc:
        raise LookupError(f"No background task registered for {task_type}") from exc
    return handler(payload)
