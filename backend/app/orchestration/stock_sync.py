"""Stock master-data synchronization workflow."""

from __future__ import annotations

from dataclasses import dataclass

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.markets import MASTER_MARKETS
from app.models.market_data import DataSource
from app.schemas.market_data import StockSyncResponse
from app.services.stock_sync import StockSyncService


class SyncSourceNotFoundError(LookupError):
    pass


class SyncSourceDisabledError(RuntimeError):
    pass


@dataclass(frozen=True, slots=True)
class StockSyncCommand:
    source_code: str
    market: str
    enable_fallback: bool = True


class StockMasterSyncWorkflow:
    def __init__(self, db: Session):
        self.db = db

    def execute(self, command: StockSyncCommand) -> StockSyncResponse:
        source = self.db.scalar(
            select(DataSource).where(DataSource.source_code == command.source_code)
        )
        if source is None:
            raise SyncSourceNotFoundError(f"Data source not found: {command.source_code}")
        if not source.enabled:
            raise SyncSourceDisabledError(f"Data source is disabled: {command.source_code}")

        markets = list(MASTER_MARKETS) if command.market == "ALL" else [command.market]
        logs = [
            StockSyncService(self.db).synchronize(
                source, market, enable_fallback=command.enable_fallback
            )
            for market in markets
        ]
        attempted_source_codes: list[str] = []
        for log in logs:
            route_trace = (log.detail_json or {}).get("route_trace") or []
            if isinstance(route_trace, list) and route_trace:
                attempted_source_codes.extend(
                    str(item.get("source_code"))
                    for item in route_trace
                    if isinstance(item, dict) and item.get("source_code")
                )
            else:
                attempted_source_codes.append(source.source_code)

        has_failure = any(item.status == "FAILED" for item in logs)
        has_fallback_failure = any(
            isinstance(trace_item, dict) and trace_item.get("status") == "FAILED"
            for log in logs
            for trace_item in ((log.detail_json or {}).get("route_trace") or [])
        )
        has_success = any(item.status == "SUCCESS" for item in logs)
        return StockSyncResponse(
            sync_log_ids=[item.id for item in logs],
            attempted_source_codes=attempted_source_codes,
            status=(
                "SUCCESS_WITH_FALLBACK"
                if has_success and (has_failure or has_fallback_failure)
                else "FAILED"
                if not has_success
                else "SUCCESS"
            ),
            message=(
                "Synchronization finished. Check data-sync logs for provider failures "
                "and fallback attempts."
            ),
        )
