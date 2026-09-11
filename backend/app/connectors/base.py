from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True)
class SymbolRecord:
    market: str
    symbol: str
    exchange: str
    name: str
    asset_type: str
    status: str
    list_date: str | None
    ext_json: dict[str, Any]
    raw_payload: dict[str, Any]


@dataclass(frozen=True)
class KlineRecord:
    market: str
    symbol: str
    period: str
    adjust: str
    trade_date: str
    open_price: float | None
    high_price: float | None
    low_price: float | None
    close_price: float | None
    volume: float | None
    amount: float | None
    turnover_rate: float | None
    raw_payload: dict[str, Any]


@dataclass(frozen=True)
class FinancialRecord:
    market: str
    symbol: str
    indicator: str
    report_period: str
    currency: str | None
    data_json: dict[str, Any]
    url: str | None = None


@dataclass(frozen=True)
class NoticeRecord:
    market: str
    symbol: str
    notice_date: str
    title: str
    notice_type: str | None
    url: str | None
    content_json: dict[str, Any]


@dataclass(frozen=True)
class QuoteRecord:
    market: str
    symbol: str
    quote_time: str | None
    current_price: float | None
    previous_close_price: float | None
    open_price: float | None
    high_price: float | None
    low_price: float | None
    volume: float | None
    amount: float | None
    change_amount: float | None
    change_pct: float | None
    turnover_rate: float | None
    raw_payload: dict[str, Any]


@dataclass(frozen=True)
class NewsRecord:
    market: str
    symbol: str
    news_time: str
    title: str
    content: str | None
    source_name: str | None
    url: str | None
    content_json: dict[str, Any]


class MarketDataAdapter(ABC):
    adapter_type: str

    @abstractmethod
    def health_check(self) -> tuple[str, list[str]]:
        """Return a status message and supported capability names."""

    @abstractmethod
    def fetch_symbol_master(self, market: str) -> list[SymbolRecord]:
        """Fetch and standardize stock master data for one market."""

    def fetch_kline(
        self,
        market: str,
        symbol: str,
        period: str,
        start_date: str,
        end_date: str,
        adjust: str,
    ) -> list[KlineRecord]:
        raise NotImplementedError(f"{self.adapter_type} does not support K-line retrieval")

    def fetch_financial_indicators(
        self,
        market: str,
        symbol: str,
        indicator: str,
    ) -> list[FinancialRecord]:
        raise NotImplementedError(f"{self.adapter_type} does not support financial indicator retrieval")

    def fetch_notices(
        self,
        market: str,
        symbol: str,
        start_date: str,
        end_date: str,
    ) -> list[NoticeRecord]:
        raise NotImplementedError(f"{self.adapter_type} does not support announcement retrieval")

    def fetch_realtime_quote(self, market: str, symbol: str) -> QuoteRecord:
        raise NotImplementedError(f"{self.adapter_type} does not support realtime quote retrieval")

    def fetch_news(self, market: str, symbol: str) -> list[NewsRecord]:
        raise NotImplementedError(f"{self.adapter_type} does not support news retrieval")

    def fetch_extended_data(self, market: str, symbol: str) -> dict[str, Any]:
        """Return optional F10-style profile, holder, fund-flow and report data."""
        return {}
