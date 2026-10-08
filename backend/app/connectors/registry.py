from app.connectors.akshare_adapter import AkshareAdapter
from app.connectors.akshare_hk_sina_adapter import AkshareHkSinaAdapter
from app.connectors.base import MarketDataAdapter
from app.connectors.hkex_sdw_adapter import HkexSdwAdapter
from app.connectors.official_reference_adapter import OfficialReferenceAdapter
from app.connectors.pytdx_adapter import PytdxAdapter
from app.connectors.listing_master import SseMasterAdapter, SzseMasterAdapter, BseMasterAdapter, HkexSecuritiesAdapter
from app.connectors.company_registry_adapter import CompanyPublicAdapter, CompanyDisclosureAdapter, CompanyMarketCapAdapter
from app.connectors.hk_research import EtNetResearchAdapter, AastocksResearchAdapter
from app.connectors.hk_investor_qa import HongKongQAAdapter

_ADAPTERS: dict[str, type[MarketDataAdapter]] = {
    "AKSHARE": AkshareAdapter,
    "AKSHARE_HK_SINA": AkshareHkSinaAdapter,
    "HKEX_SDW": HkexSdwAdapter,
    "SSE_MASTER": SseMasterAdapter,
    "SZSE_MASTER": SzseMasterAdapter,
    "BSE_MASTER": BseMasterAdapter,
    "HKEX_SECURITIES": HkexSecuritiesAdapter,
    "OFFICIAL_REFERENCE": OfficialReferenceAdapter,
    "PYTDX": PytdxAdapter,
    "COMPANY_PUBLIC": CompanyPublicAdapter,
    "COMPANY_DISCLOSURE": CompanyDisclosureAdapter,
    "COMPANY_MARKET_CAP": CompanyMarketCapAdapter,
    "ETNET_HK": EtNetResearchAdapter,
    "AASTOCKS_HK": AastocksResearchAdapter,
    "HK_ISSUER_IR": HongKongQAAdapter,
}


def get_adapter(adapter_type: str) -> MarketDataAdapter:
    adapter_class = _ADAPTERS.get(adapter_type.upper())
    if not adapter_class:
        supported = ", ".join(sorted(_ADAPTERS))
        raise ValueError(f"Unsupported adapter type '{adapter_type}'. Supported adapters: {supported}")
    return adapter_class()
