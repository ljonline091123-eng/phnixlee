from __future__ import annotations

from datetime import datetime, timezone
from typing import Any

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.markets import MARKET_CN_A, MARKET_HK, MARKET_NEEQ, MARKET_NEEQ_INNOVATION, MASTER_MARKETS
from app.connectors.base import SymbolRecord
from app.connectors.registry import get_adapter
from app.models.market_data import DataSource, DataSyncLog, StockSymbol

MARKET_INTERFACE_CODES = {
    MARKET_CN_A: "CN_A_SYMBOLS",
    MARKET_HK: "HK_SYMBOLS",
    MARKET_NEEQ: "NEEQ_SYMBOLS",
    MARKET_NEEQ_INNOVATION: "NEEQ_INNOVATION_SYMBOLS",
}

SYMBOL_REMOTE_TIME_KEYS = (
    "last_synced_at",
    "updated_at",
    "update_time",
    "last_update",
    "更新时间",
    "更新日期",
    "UPDATE_DATE",
)


def _parse_symbol_timestamp(value: Any) -> datetime | None:
    if isinstance(value, datetime):
        parsed = value
    else:
        text = str(value or "").strip()
        if not text:
            return None
        text = text.replace("Z", "+00:00").replace("/", "-")
        parsed = None
        for candidate in (text, text[:19], text[:10]):
            try:
                parsed = datetime.fromisoformat(candidate)
                break
            except ValueError:
                continue
        if parsed is None:
            for fmt in ("%Y%m%d", "%Y-%m-%d %H:%M:%S", "%Y/%m/%d %H:%M:%S"):
                try:
                    parsed = datetime.strptime(text, fmt)
                    break
                except ValueError:
                    continue
        if parsed is None:
            return None
    return parsed.replace(tzinfo=parsed.tzinfo or timezone.utc)


def _symbol_remote_timestamp(record: SymbolRecord, fallback: datetime) -> datetime:
    for payload in (record.ext_json, record.raw_payload):
        if not isinstance(payload, dict):
            continue
        lowered = {str(key).lower(): value for key, value in payload.items()}
        for key in SYMBOL_REMOTE_TIME_KEYS:
            value = payload.get(key)
            if value is None:
                value = lowered.get(key.lower())
            parsed = _parse_symbol_timestamp(value)
            if parsed:
                return parsed
    return fallback


class StockSyncService:
    def __init__(self, db: Session):
        self.db = db

    def synchronize(self, source: DataSource, market: str, enable_fallback: bool = True) -> DataSyncLog:
        if not source.enabled:
            raise ValueError(f"Data source {source.source_code} is disabled")
        if market not in MASTER_MARKETS:
            raise ValueError(f"Unsupported symbol market: {market}")

        sync_log = DataSyncLog(
            source_id=source.id,
            interface_code=MARKET_INTERFACE_CODES[market],
            market=market,
            status="RUNNING",
        )
        self.db.add(sync_log)
        self.db.commit()
        self.db.refresh(sync_log)

        try:
            adapter = get_adapter(source.adapter_type)
            records, used_source, route_trace = self._fetch_with_fallback(
                adapter,
                source,
                market,
                enable_fallback=enable_fallback,
            )
            inserted_count, updated_count = self._upsert_records(used_source.id, records)
            sync_log.status = "SUCCESS"
            sync_log.total_count = len(records)
            sync_log.inserted_count = inserted_count
            sync_log.updated_count = updated_count
            sync_log.detail_json = {
                "adapter_type": source.adapter_type,
                "used_source_code": used_source.source_code,
                "used_adapter_type": used_source.adapter_type,
                "route_trace": route_trace,
            }
            sync_log.completed_at = datetime.now(timezone.utc)
            self.db.commit()
            self.db.refresh(sync_log)
            return sync_log
        except Exception as exc:
            self.db.rollback()
            failed_log = self.db.get(DataSyncLog, sync_log.id)
            if not failed_log:
                raise
            failed_log.status = "FAILED"
            failed_log.failed_count = 1
            failed_log.error_message = str(exc)[:4000]
            failed_log.detail_json = {"adapter_type": source.adapter_type, "market": market, "error_type": type(exc).__name__}
            failed_log.completed_at = datetime.now(timezone.utc)
            self.db.commit()
            self.db.refresh(failed_log)
            return failed_log

    def _fetch_with_fallback(
        self,
        adapter,
        source: DataSource,
        market: str,
        enable_fallback: bool,
    ) -> tuple[list[SymbolRecord], DataSource, list[dict[str, str]]]:
        route_trace: list[dict[str, str]] = []
        candidate_codes = [source.source_code]
        fallback_source_code = (source.config_json or {}).get("fallback_source_code")
        if enable_fallback and market == MARKET_HK and fallback_source_code:
            fallback_source = self.db.scalar(select(DataSource).where(DataSource.source_code == fallback_source_code))
            if fallback_source and fallback_source.enabled:
                candidate_codes.append(fallback_source.source_code)
                nested_fallback = (fallback_source.config_json or {}).get("fallback_source_code")
                if nested_fallback and nested_fallback not in candidate_codes:
                    nested_source = self.db.scalar(select(DataSource).where(DataSource.source_code == nested_fallback))
                    if nested_source and nested_source.enabled:
                        candidate_codes.append(nested_source.source_code)

        last_error: Exception | None = None
        for source_code in candidate_codes:
            current_source = self.db.scalar(select(DataSource).where(DataSource.source_code == source_code))
            if not current_source or not current_source.enabled:
                continue
            route_trace.append({"source_code": source_code, "adapter_type": current_source.adapter_type})
            current_adapter = get_adapter(current_source.adapter_type)
            try:
                records = current_adapter.fetch_symbol_master(market)
                if records:
                    route_trace[-1]["status"] = "SUCCESS"
                    return records, current_source, route_trace
            except Exception as exc:
                last_error = exc
                route_trace[-1]["status"] = "FAILED"
                route_trace[-1]["error"] = f"{type(exc).__name__}: {exc}"[:400]
                continue

        if last_error:
            raise last_error
        raise RuntimeError(f"No available records for market {market}")

    def _upsert_records(self, source_id: int, records: list[SymbolRecord]) -> tuple[int, int]:
        if not records:
            return 0, 0

        market = records[0].market
        existing_rows = self.db.scalars(
            select(StockSymbol).where(
                StockSymbol.market == market,
                StockSymbol.symbol.in_([item.symbol for item in records]),
            )
        ).all()
        existing_by_symbol = {item.symbol: item for item in existing_rows}

        inserted_count = 0
        updated_count = 0
        sync_time = datetime.now(timezone.utc)
        for item in records:
            remote_synced_at = _symbol_remote_timestamp(item, sync_time)
            current = existing_by_symbol.get(item.symbol)
            if current:
                local_synced_at = _parse_symbol_timestamp(current.last_synced_at)
                if remote_synced_at and local_synced_at and remote_synced_at < local_synced_at:
                    continue
                current.exchange = item.exchange
                current.name = item.name
                current.asset_type = item.asset_type
                current.status = item.status
                current.list_date = item.list_date
                current.source_id = source_id
                current.ext_json = item.ext_json
                current.raw_payload = item.raw_payload
                current.last_synced_at = remote_synced_at
                updated_count += 1
                continue

            self.db.add(
                StockSymbol(
                    market=item.market,
                    symbol=item.symbol,
                    exchange=item.exchange,
                    name=item.name,
                    asset_type=item.asset_type,
                    status=item.status,
                    list_date=item.list_date,
                    source_id=source_id,
                    ext_json=item.ext_json,
                    raw_payload=item.raw_payload,
                    last_synced_at=remote_synced_at,
                )
            )
            inserted_count += 1

        # NEEQ code-table responses represent the complete active universe.
        # Reconcile rows that disappeared from the remote snapshot so
        # delisted/transferred companies remain auditable but are not returned
        # by normal LISTED/IPO searches.
        if market in (MARKET_NEEQ, MARKET_NEEQ_INNOVATION):
            active_symbols = {item.symbol for item in records}
            stale_rows = self.db.scalars(
                select(StockSymbol).where(
                    StockSymbol.market == market,
                    StockSymbol.status.in_(["LISTED", "IPO"]),
                    StockSymbol.symbol.not_in(active_symbols),
                )
            ).all()
            for stale in stale_rows:
                stale.status = "DELISTED"

        self.db.flush()
        return inserted_count, updated_count
