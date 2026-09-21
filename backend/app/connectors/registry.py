from app.connectors.akshare_adapter import AkshareAdapter
from app.connectors.akshare_hk_sina_adapter import AkshareHkSinaAdapter
from app.connectors.base import MarketDataAdapter
from app.connectors.hkex_sdw_adapter import HkexSdwAdapter
from app.connectors.official_reference_adapter import OfficialReferenceAdapter
from app.connectors.pytdx_adapter import PytdxAdapter
from app.connectors.company_registry_adapter import CompanyPublicAdapter, CompanyDisclosureAdapter, CompanyMarketCapAdapter

_ADAPTERS: dict[str, type[MarketDataAdapter]] = {
    "AKSHARE": AkshareAdapter,
    "AKSHARE_HK_SINA": AkshareHkSinaAdapter,
    "HKEX_SDW": HkexSdwAdapter,
    "OFFICIAL_REFERENCE": OfficialReferenceAdapter,
    "PYTDX": PytdxAdapter,
    "COMPANY_PUBLIC": CompanyPublicAdapter,
    "COMPANY_DISCLOSURE": CompanyDisclosureAdapter,
    "COMPANY_MARKET_CAP": CompanyMarketCapAdapter,
}


def get_adapter(adapter_type: str) -> MarketDataAdapter:
    adapter_class = _ADAPTERS.get(adapter_type.upper())
    if not adapter_class:
        supported = ", ".join(sorted(_ADAPTERS))
        raise ValueError(f"Unsupported adapter type '{adapter_type}'. Supported adapters: {supported}")
    return adapter_class()
