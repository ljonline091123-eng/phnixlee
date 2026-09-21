"""Company-source health probes in the existing source registry interface."""

from datetime import datetime, timezone

from app.connectors.base import MarketDataAdapter
from app.connectors.company_sources import (SourceResult, fetch_business_disclosures, fetch_company_profile,
    fetch_legal_disclosures, fetch_market_cap, fetch_shareholders, fetch_supply_chain_disclosures)


class CompanyPublicAdapter(MarketDataAdapter):
    adapter_type = "COMPANY_PUBLIC"

    def fetch_company_profile(self, market: str, symbol: str, **kwargs) -> dict:
        return fetch_company_profile(market, symbol, **kwargs).to_dict()

    def fetch_shareholders(self, market: str, symbol: str, **kwargs) -> dict:
        return fetch_shareholders(market, symbol, **kwargs).to_dict()

    def health_check(self):
        result = fetch_company_profile("CN_A", "000001")
        if result.status != "SUCCESS":
            raise RuntimeError("Company profile probe: " + result.status)
        return "公开公司资料实测可读取；工商字段为提供商转录，非登记机关核验。", ["COMPANY_PROFILE", "INDUSTRY", "DISCLOSED_HOLDERS"]

    def fetch_symbol_master(self, market):
        raise NotImplementedError("Use company governance for company records; this source does not replace stock master synchronization")


class CompanyDisclosureAdapter(MarketDataAdapter):
    adapter_type = "COMPANY_DISCLOSURE"

    @staticmethod
    def _unsupported(market: str, symbol: str, source_code: str) -> dict:
        return SourceResult(source_code=source_code, source_name="CNINFO", source_kind="OFFICIAL_DISCLOSURE",
            source_url="https://www.cninfo.com.cn/", retrieved_at=datetime.now(timezone.utc).isoformat(),
            status="UNSUPPORTED", request={"market": market, "symbol": symbol},
            warnings=["This CNINFO disclosure adapter supports CN_A only; no A-share request was made."]).to_dict()

    def fetch_legal_disclosures(self, market: str, symbol: str, **kwargs) -> dict:
        if market.upper() != "CN_A":
            return self._unsupported(market, symbol, "CNINFO_LEGAL_DISCLOSURES")
        return fetch_legal_disclosures(symbol, **kwargs).to_dict()

    def fetch_business_disclosures(self, market: str, symbol: str, **kwargs) -> dict:
        if market.upper() != "CN_A":
            return self._unsupported(market, symbol, "CNINFO_BUSINESS_DISCLOSURES")
        return fetch_business_disclosures(symbol, **kwargs).to_dict()

    def fetch_supply_chain_disclosures(self, market: str, symbol: str, **kwargs) -> dict:
        if market.upper() != "CN_A":
            return self._unsupported(market, symbol, "CNINFO_SUPPLY_CHAIN_DISCLOSURES")
        return fetch_supply_chain_disclosures(symbol, **kwargs).to_dict()

    def health_check(self):
        result = fetch_legal_disclosures("600221", max_documents=1)
        if result.status not in {"SUCCESS", "PARTIAL"}:
            raise RuntimeError("Disclosure probe: " + result.status)
        return "巨潮司法披露实测可读取；公告完整度 " + result.status + "，不代表法院案件库完整覆盖。", ["LEGAL_DISCLOSURE", "BUSINESS_DISCLOSURE", "SUPPLY_CHAIN_DISCLOSURE", "PDF_TEXT"]

    def fetch_symbol_master(self, market):
        raise NotImplementedError("Disclosure source does not replace stock master synchronization")


class CompanyMarketCapAdapter(MarketDataAdapter):
    adapter_type = "COMPANY_MARKET_CAP"

    def fetch_market_cap(self, market: str, symbol: str, **kwargs) -> dict:
        return fetch_market_cap(market, symbol, **kwargs).to_dict()

    def health_check(self):
        result = fetch_market_cap("CN_A", "000001")
        if result.status != "SUCCESS" or not result.records:
            raise RuntimeError("Tencent market-cap probe: " + result.status)
        return "腾讯 A 股市值快照实测可读取；供应商发行主体总股本口径，A/H 不相加，港股暂不支持。", ["MARKET_CAP", "SIZE_CLASSIFICATION"]

    def fetch_symbol_master(self, market):
        raise NotImplementedError("Market-cap snapshots do not replace stock master synchronization")
