from __future__ import annotations

from datetime import date, timedelta
from typing import Any

import requests

from app.connectors.base import MarketDataAdapter, SymbolRecord


class HkexSdwAdapter(MarketDataAdapter):
    """HKEXnews SDW stock list adapter used as a stable HK master-data fallback."""

    adapter_type = "HKEX_SDW"
    stock_list_url = "https://www.hkexnews.hk/sdw/search/stocklist.aspx"

    def health_check(self) -> tuple[str, list[str]]:
        return (
            "HKEX SDW stock-list endpoint is configured.",
            ["HK_SYMBOLS"],
        )

    def fetch_symbol_master(self, market: str) -> list[SymbolRecord]:
        if market != "HK":
            raise ValueError("HKEX SDW adapter only supports the HK market")

        last_error: Exception | None = None
        for shareholding_date in self._recent_dates():
            try:
                rows = self._fetch_stock_list(shareholding_date)
                records = self._normalize_rows(rows)
                if records:
                    return records
            except Exception as exc:
                last_error = exc
        if last_error:
            raise last_error
        return []

    def _fetch_stock_list(self, shareholding_date: date) -> list[dict[str, Any]]:
        response = requests.get(
            self.stock_list_url,
            params={
                "sortby": "stockcode",
                "shareholdingdate": shareholding_date.strftime("%Y%m%d"),
            },
            headers={
                "Accept": "application/json,text/plain,*/*",
                "User-Agent": "Mozilla/5.0",
            },
            timeout=30,
        )
        response.raise_for_status()
        payload = response.json()
        if not isinstance(payload, list):
            raise ValueError("HKEX SDW stock-list response is not a JSON array")
        return payload

    def _normalize_rows(self, rows: list[dict[str, Any]]) -> list[SymbolRecord]:
        records: list[SymbolRecord] = []
        for row in rows:
            raw_code = row.get("c")
            raw_name = row.get("n")
            if raw_code is None or raw_name is None:
                continue
            symbol = str(raw_code).strip().zfill(5)
            name = str(raw_name).strip()
            if not symbol or not name:
                continue
            records.append(
                SymbolRecord(
                    market="HK",
                    symbol=symbol,
                    exchange="HKEX",
                    name=name,
                    asset_type="STOCK",
                    status="LISTED",
                    list_date=None,
                    ext_json={"source_endpoint": "HKEX_SDW"},
                    raw_payload=row,
                )
            )
        return records

    @staticmethod
    def _recent_dates() -> list[date]:
        today = date.today()
        return [today - timedelta(days=offset) for offset in range(0, 10)]
