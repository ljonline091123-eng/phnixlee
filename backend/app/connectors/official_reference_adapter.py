from __future__ import annotations

from app.connectors.base import MarketDataAdapter, SymbolRecord


class OfficialReferenceAdapter(MarketDataAdapter):
    """Catalog-only adapter for official sources pending structured crawling.

    It lets the data-source registry expose authoritative portals without
    pretending that all downstream interfaces have already been implemented.
    """

    adapter_type = "OFFICIAL_REFERENCE"

    def health_check(self) -> tuple[str, list[str]]:
        return (
            "官方数据入口已登记；结构化抓取接口仍需按页面/API 规则逐项接入。",
            ["REFERENCE"],
        )

    def fetch_symbol_master(self, market: str) -> list[SymbolRecord]:
        raise NotImplementedError("官方参考源当前不提供股票主数据同步")
