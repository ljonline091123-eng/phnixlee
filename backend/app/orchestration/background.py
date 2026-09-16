"""Background workflows shared by the standalone worker and direct callers."""

from __future__ import annotations

from typing import Any

from sqlalchemy import select

from app.core.markets import MASTER_MARKETS
from app.db.session import SessionLocal
from app.models.market_data import DataSource
from app.services.catalog import seed_default_catalog
from app.services.prediction_review import propose_failed_skill_revisions, review_predictions
from app.services.stock_sync import StockSyncService


class MasterDataSyncWorkflow:
    workflow_code = "daily_master_sync"

    def execute(self, context_data: dict[str, Any] | None = None) -> dict[str, Any]:
        succeeded: list[str] = []
        failed: dict[str, str] = {}
        with SessionLocal() as db:
            seed_default_catalog(db)
            source = db.scalar(
                select(DataSource).where(
                    DataSource.source_code == "AKSHARE",
                    DataSource.enabled.is_(True),
                )
            )
            if source is None:
                return {
                    "status": "SKIPPED",
                    "workflow": self.workflow_code,
                    "reason": "AKSHARE source is unavailable",
                }
            for market in MASTER_MARKETS:
                try:
                    StockSyncService(db).synchronize(source, market, enable_fallback=True)
                    succeeded.append(market)
                except Exception as exc:
                    db.rollback()
                    failed[market] = str(exc)[:500]
        return {
            "status": "COMPLETED" if not failed else "PARTIAL",
            "workflow": self.workflow_code,
            "succeeded_markets": succeeded,
            "failed_markets": failed,
        }


class PredictionReviewWorkflow:
    workflow_code = "daily_prediction_review"

    def execute(self, context_data: dict[str, Any] | None = None) -> dict[str, Any]:
        with SessionLocal() as db:
            review_summary = review_predictions(db, fetch_missing=True)
            proposed_revisions = propose_failed_skill_revisions(db)
        return {
            "status": "COMPLETED",
            "workflow": self.workflow_code,
            "review": review_summary,
            "proposed_revisions": proposed_revisions,
        }
