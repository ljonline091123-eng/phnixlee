"""Registry interface contracts without public network calls."""

from unittest.mock import patch

import pytest

from app.connectors import company_registry_adapter as adapters
from app.connectors.company_sources import SourceResult
from app.connectors.registry import get_adapter


def result(status="SUCCESS"):
    return SourceResult(source_code="FIXTURE", source_name="FIXTURE", source_kind="AGGREGATOR",
        source_url="https://example.test/", retrieved_at="2026-01-01T00:00:00Z", status=status,
        records=[{"fixture": True}] if status == "SUCCESS" else [], warnings=["Fixture warning"])


@pytest.mark.parametrize("adapter_type,method,kwargs", [
    ("COMPANY_PUBLIC", "fetch_company_profile", {}),
    ("COMPANY_PUBLIC", "fetch_shareholders", {"report_date": "2025-12-31"}),
    ("COMPANY_MARKET_CAP", "fetch_market_cap", {}),
])
def test_registered_company_methods_return_source_result_objects(adapter_type, method, kwargs):
    expected = result()
    with patch.object(adapters, method, return_value=expected) as fetch:
        payload = getattr(get_adapter(adapter_type), method)(market="CN_A", symbol="000001", **kwargs)
    assert payload == expected.to_dict()
    fetch.assert_called_once_with("CN_A", "000001", **kwargs)
    assert {"status", "records", "warnings"} <= payload.keys()


@pytest.mark.parametrize("method", ["fetch_legal_disclosures", "fetch_business_disclosures", "fetch_supply_chain_disclosures"])
def test_disclosure_wrappers_accept_registered_market_symbol_contract(method):
    expected = result()
    with patch.object(adapters, method, return_value=expected) as fetch:
        payload = getattr(get_adapter("COMPANY_DISCLOSURE"), method)(market="CN_A", symbol="600221", max_documents=1)
    assert payload == expected.to_dict()
    fetch.assert_called_once_with("600221", max_documents=1)


@pytest.mark.parametrize("method,source_code", [
    ("fetch_legal_disclosures", "CNINFO_LEGAL_DISCLOSURES"),
    ("fetch_business_disclosures", "CNINFO_BUSINESS_DISCLOSURES"),
    ("fetch_supply_chain_disclosures", "CNINFO_SUPPLY_CHAIN_DISCLOSURES"),
])
def test_hk_disclosures_never_send_a_share_requests(method, source_code):
    with patch.object(adapters, method, side_effect=AssertionError("HK was sent to an A-share source")) as fetch:
        payload = getattr(get_adapter("COMPANY_DISCLOSURE"), method)(market="HK", symbol="01398")
    fetch.assert_not_called()
    assert payload["status"] == "UNSUPPORTED" and payload["records"] == []
    assert payload["source_code"] == source_code
    assert payload["request"] == {"market": "HK", "symbol": "01398"}


def test_marketcap_health_probes_only_tencent():
    with patch.object(adapters, "fetch_market_cap", return_value=result()) as cap_fetch, \
         patch.object(adapters, "fetch_company_profile", side_effect=AssertionError("Wrong provider health probe")) as profile_fetch:
        message, capabilities = get_adapter("COMPANY_MARKET_CAP").health_check()
    cap_fetch.assert_called_once_with("CN_A", "000001")
    profile_fetch.assert_not_called()
    assert "MARKET_CAP" in capabilities


@pytest.mark.parametrize("status", ["UNAVAILABLE", "UNSUPPORTED", "EMPTY"])
def test_marketcap_health_does_not_hide_source_failure(status):
    with patch.object(adapters, "fetch_market_cap", return_value=result(status)), \
         patch.object(adapters, "fetch_company_profile", return_value=result()):
        with pytest.raises(RuntimeError, match="Tencent market-cap probe"):
            get_adapter("COMPANY_MARKET_CAP").health_check()


def test_marketcap_success_without_records_is_not_healthy():
    empty = result()
    empty.records = []
    with patch.object(adapters, "fetch_market_cap", return_value=empty):
        with pytest.raises(RuntimeError, match="Tencent market-cap probe"):
            get_adapter("COMPANY_MARKET_CAP").health_check()
