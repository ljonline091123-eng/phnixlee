"""Task registry kept independent from HTTP routes."""

from __future__ import annotations

from collections.abc import Callable
from typing import Any

from app.orchestration.background import MasterDataSyncWorkflow, PredictionReviewWorkflow


TaskHandler = Callable[[dict[str, Any]], dict[str, Any]]


TASK_HANDLERS: dict[str, TaskHandler] = {
    "daily_master_sync": MasterDataSyncWorkflow().execute,
    "daily_prediction_review": PredictionReviewWorkflow().execute,
}


def execute_task(task_type: str, payload: dict[str, Any]) -> dict[str, Any]:
    try:
        handler = TASK_HANDLERS[task_type]
    except KeyError as exc:
        raise LookupError(f"No background task registered for {task_type}") from exc
    return handler(payload)
