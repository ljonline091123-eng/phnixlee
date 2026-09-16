"""F10 application workflow.

The route owns HTTP concerns; this workflow owns ordering, fallback and
persistence boundaries across repository, connector and domain services.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Callable

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.markets import (
    LIVE_REFRESH_MARKETS,
    MASTER_MARKETS,
    digit_length_for_market,
    normalize_market,
)
from app.models.market_data import DataSource, StockSymbol
from app.repositories.stock_f10 import StockF10Repository, StockF10Snapshot
from app.schemas.market_data import StockF10Read
from app.services.catalog import select_data_source
from app.services.f10 import (
    F10_EXTENDED_SECTIONS,
    _ensure_neeq_f10_from_core,
    _hydrate_symbol_from_f10,
    _load_f10_extended_data,
    _merge_f10_extended_data,
    _merge_local_report_notices,
    _needs_f10_refresh,
)
from app.services.stock_on_demand import StockOnDemandService


class F10WorkflowError(Exception):
    """Base exception translated by the HTTP adapter."""


class F10ValidationError(F10WorkflowError):
    pass


class StockNotFoundError(F10WorkflowError):
    pass


@dataclass(frozen=True, slots=True)
class GetStockF10Command:
    market: str
    symbol: str
    kline_limit: int = 12
    financial_limit: int = 8
    notice_limit: int = 8
    notice_page: int = 1
    news_limit: int = 8
    news_page: int = 1
    refresh: bool = False
    local_only: bool = False


def _normalize_symbol(market: str, symbol: str) -> str:
    normalized_market = normalize_market(market)
    value = symbol.strip().upper()
    return value.zfill(digit_length_for_market(normalized_market)) if value.isdigit() else value


class F10Workflow:
    def __init__(
        self,
        db: Session,
        *,
        fetch_extended_data: Callable[[Session, DataSource, str, str], dict[str, dict]],
        symbol_reader: Callable[[StockSymbol, Any | None], dict[str, Any]],
    ):
        self.db = db
        self.repository = StockF10Repository(db)
        self.fetch_extended_data = fetch_extended_data
        self.symbol_reader = symbol_reader

    def execute(self, command: GetStockF10Command) -> StockF10Read:
        market = command.market.upper()
        symbol = _normalize_symbol(market, command.symbol)
        self._validate(command, market)
        snapshot = self._load(command, market, symbol)
        if snapshot.stock is None:
            raise StockNotFoundError("Stock symbol not found")

        extended_data = _load_f10_extended_data(self.db, market, symbol)
        source = select_data_source(self.db, market, "F10", fallback_code="AKSHARE")
        core_source = self.db.scalar(
            select(DataSource).where(
                DataSource.source_code == "AKSHARE",
                DataSource.enabled.is_(True),
            )
        )

        if command.refresh and core_source and market in LIVE_REFRESH_MARKETS:
            extended_data = self._explicit_refresh(
                command, snapshot, market, symbol, source, core_source, extended_data
            )
            snapshot = self._load(command, market, symbol)
            if snapshot.stock is None:
                raise StockNotFoundError("Stock symbol not found")
        elif not command.local_only and _needs_f10_refresh(extended_data, market) and source:
            try:
                fresh = self.fetch_extended_data(self.db, source, market, symbol)
                extended_data = _merge_f10_extended_data(
                    _load_f10_extended_data(self.db, market, symbol), fresh
                )
            except Exception as exc:
                self._set_fallback_message(
                    extended_data,
                    f"Remote F10 refresh failed; local cache is used: {str(exc)[:180]}",
                )

        merged_reports = _merge_local_report_notices(
            extended_data.get("published_reports"),
            snapshot.report_notices,
            market,
            symbol,
        )
        extended_data["published_reports"] = merged_reports
        if not command.local_only and merged_reports.get("reports") and source:
            StockOnDemandService(self.db).upsert_f10_cache(
                source, market, symbol, "published_reports", merged_reports
            )

        if command.refresh:
            # Persistence may have changed the report cache after the snapshot.
            extended_data = _load_f10_extended_data(self.db, market, symbol)
            extended_data["published_reports"] = _merge_local_report_notices(
                extended_data.get("published_reports"),
                snapshot.report_notices,
                market,
                symbol,
            )

        extended_data = _ensure_neeq_f10_from_core(
            db=self.db,
            source=source,
            market=market,
            symbol=symbol,
            quote=snapshot.quote,
            financials=snapshot.financials,
            notices=snapshot.report_notices,
            extended_data=extended_data,
        )
        if _hydrate_symbol_from_f10(snapshot.stock, extended_data):
            self.db.commit()

        return StockF10Read(
            symbol=self.symbol_reader(snapshot.stock, snapshot.quote),
            realtime_quote=snapshot.quote,
            recent_klines=snapshot.klines,
            financial_reports=snapshot.financials,
            notices=snapshot.notices,
            news=snapshot.news,
            notice_total=snapshot.notice_total,
            notice_page=command.notice_page,
            news_total=snapshot.news_total,
            news_page=command.news_page,
            published_reports=extended_data.get("published_reports") or {},
            profile=extended_data.get("profile") or {},
            holders=extended_data.get("holders") or {},
            fund_flow=extended_data.get("fund_flow") or {},
            financial_summary=extended_data.get("financial_summary") or {},
            financial_statements=extended_data.get("financial_statements") or {},
            business_composition=extended_data.get("business_composition") or {},
        )

    @staticmethod
    def _validate(command: GetStockF10Command, market: str) -> None:
        if market not in MASTER_MARKETS:
            raise F10ValidationError(f"market must be one of: {', '.join(MASTER_MARKETS)}")
        limits = (command.financial_limit, command.notice_limit, command.news_limit)
        if min(limits) < 1 or max(limits) > 100:
            raise F10ValidationError("F10 detail limits must be between 1 and 100")
        if command.notice_page < 1 or command.news_page < 1:
            raise F10ValidationError("notice_page and news_page must be >= 1")
        if command.kline_limit < 1 or command.kline_limit > 5000:
            raise F10ValidationError("kline_limit must be between 1 and 5000")

    def _load(self, command: GetStockF10Command, market: str, symbol: str) -> StockF10Snapshot:
        return self.repository.load(
            market=market,
            symbol=symbol,
            kline_limit=command.kline_limit,
            financial_limit=command.financial_limit,
            notice_limit=command.notice_limit,
            notice_page=command.notice_page,
            news_limit=command.news_limit,
            news_page=command.news_page,
        )

    def _explicit_refresh(
        self,
        command: GetStockF10Command,
        snapshot: StockF10Snapshot,
        market: str,
        symbol: str,
        source: DataSource | None,
        core_source: DataSource,
        extended_data: dict[str, dict],
    ) -> dict[str, dict]:
        refresh_errors = StockOnDemandService(self.db).refresh_stock_data(
            source=core_source,
            market=market,
            symbol=symbol,
            list_date=snapshot.stock.list_date if snapshot.stock else None,
        )
        if refresh_errors:
            self._set_fallback_message(
                extended_data,
                f"Core refresh partially failed; local data is used: {'; '.join(refresh_errors)[:180]}",
            )
        if source:
            try:
                self.fetch_extended_data(self.db, source, market, symbol)
            except Exception as exc:
                refresh_errors.append(f"f10: {str(exc)[:240]}")
        return _load_f10_extended_data(self.db, market, symbol)

    @staticmethod
    def _set_fallback_message(extended_data: dict[str, dict], message: str) -> None:
        for section in F10_EXTENDED_SECTIONS:
            payload = extended_data.get(section)
            if isinstance(payload, dict):
                payload["message"] = payload.get("message") or message
