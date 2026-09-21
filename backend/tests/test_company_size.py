"""Offline market-cap classification boundaries and provenance checks."""

from datetime import date
import json
from pathlib import Path
from unittest.mock import patch

import pytest
from sqlalchemy import create_engine, func, select
from sqlalchemy.orm import Session

from app.db.base import Base
from app.models.company_graph import SecurityClassification
from app.models.foundation import FoundationEntity, FoundationEvidence
from app.models.market_data import DataInterface, DataSource, StockSymbol
from app.services import company_governance as governance
from app.services import company_graph
from app.services.company_registry import register_company_resources


def company():
    return {"name": "Fixture Issuer", "jurisdiction": "CN", "identifier_scheme": "TEST_REGISTER",
        "identifier_value": "FIXTURE-REGISTER-1", "source_issuer_id": "EASTMONEY-FIXTURE-1"}


def profile():
    return {"status": "SUCCESS", "source_name": "EASTMONEY", "source_code": "EASTMONEY_COMPANY_PROFILE",
        "source_url": "https://example.test/company", "retrieved_at": "2026-01-02T00:00:00Z", "warnings": [],
        "records": [{"market": "CN_A", "symbol": "000001", "company": company(),
            "evidence": {"source_key": "company:fixture", "title": "Fixture company identity",
                "content": "Fixture identity record", "available_at": "2026-01-02T00:00:00Z"}}]}


def marketcap(value=100_000_000_000):
    return {"status": "SUCCESS", "source_name": "TENCENT", "source_code": "TENCENT_MARKET_CAP",
        "source_url": "https://qt.gtimg.cn/", "retrieved_at": "2026-01-02T00:00:00Z", "warnings": [],
        "request": {"method": "GET", "params": {"q": "sz000001"}}, "raw": {"text": "fixture quote"},
        "records": [{"market": "CN_A", "symbol": "000001", "name": "Fixture Issuer",
            "total_market_cap": value, "currency": "CNY", "unit": "CNY",
            "source_value_100m": str(value / 100_000_000), "source_unit": "CNY_100M",
            "valuation_scope": "PROVIDER_ISSUER_TOTAL_SHARES_AT_A_SHARE_PRICE",
            "snapshot_at": "2026-01-01T16:00:00+08:00", "as_of": "2026-01-01", "cross_listing_additive": False}]}


@pytest.fixture
def db():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(engine)
    with Session(engine) as session:
        source = DataSource(source_code="SIZE_TEST", source_name="Fixture", adapter_type="MOCK")
        session.add(source)
        session.flush()
        session.add(StockSymbol(market="CN_A", symbol="000001", name="Fixture Issuer", exchange="SZ", source_id=source.id))
        session.commit()
        yield session
    engine.dispose()


def normalize(source):
    return governance.marketcap_record(source, company(), 1, market="CN_A", symbol="000001")


@pytest.mark.parametrize("value,expected", [(100_000_000_000, "LARGE_CAP"), (99_999_999_999, "MID_CAP"),
    (20_000_000_000, "MID_CAP"), (19_999_999_999, "SMALL_CAP")])
def test_absolute_size_boundaries(value, expected):
    normalized = normalize(marketcap(value))
    classification = normalized["classifications"][0]
    assert classification["code"] == expected
    assert classification["definition_version"] == governance.MARKET_CAP_RULE_VERSION
    assert classification["method"] == "COMPUTED_SOURCE_RULE"
    assert classification["properties_json"]["classification_basis"] == "ABSOLUTE_CNY_THRESHOLDS_NOT_PERCENTILES"
    assert classification["valid_from"] == "2026-01-01" and classification["valid_to"] == "2026-01-02"
    assert classification["properties_json"]["cross_listing_additive"] is False
    assert "source_issuer_id" not in normalized["company"]
    assert json.loads(normalized["evidence"]["content"])["raw"]["text"] == "fixture quote"


@pytest.mark.parametrize("updates", [{"total_market_cap": 0}, {"total_market_cap": -1},
    {"total_market_cap": True}, {"total_market_cap": float("inf")}, {"total_market_cap": float("nan")},
    {"total_market_cap": "100000000000"}, {"currency": "HKD"}, {"unit": "CNY_100M"},
    {"market": "HK"}, {"symbol": "601398"}, {"valuation_scope": "UNKNOWN"}, {"cross_listing_additive": True},
    {"snapshot_at": "2026-01-01T16:00:00"}, {"snapshot_at": "2026-01-03T16:00:00+08:00"},
    {"as_of": "2026-01-02"}])
def test_invalid_or_ambiguous_marketcap_is_not_classified(updates):
    source = marketcap()
    source["records"][0].update(updates)
    with pytest.raises(ValueError):
        normalize(source)


def test_unsupported_source_and_unresolved_identity_rejected():
    source = marketcap()
    source["status"] = "UNSUPPORTED"
    with pytest.raises(ValueError):
        normalize(source)
    with pytest.raises(ValueError, match="resolved company"):
        governance.marketcap_record(marketcap(), {"name": "Unresolved"}, 1, market="CN_A", symbol="000001")


def test_cached_size_import_reuses_company_and_is_idempotent(db):
    profile_source, cap_source = profile(), marketcap()

    def cached(cache_dir, prefix, *args, **kwargs):
        return profile_source if prefix == "profile" else cap_source if prefix == "marketcap" else None

    with patch.object(governance, "latest_cache", side_effect=cached):
        first = governance.import_cached_sources(db, Path("unused-fixture-cache"), accept_structured=True,
            samples=[{"market": "CN_A", "symbol": "000001"}])
        db.commit()
        assert first["items"][0]["accepted_marketcap"] == {"facts": 0, "classifications": 1}
        assert db.scalar(select(func.count()).select_from(FoundationEntity).where(FoundationEntity.entity_type == "COMPANY")) == 1
        size = db.scalar(select(SecurityClassification).where(SecurityClassification.dimension == "SIZE"))
        assert size.status == "ACCEPTED"
        assert size.reviews_json[-1]["reviewer"] == "RULE:" + governance.MARKET_CAP_RULE_VERSION
        assert size.valid_from == date(2026, 1, 1) and size.valid_to == date(2026, 1, 2)
        assert db.get(FoundationEvidence, size.evidence_id).source_name == "TENCENT"
        evidence_count = db.scalar(select(func.count()).select_from(FoundationEvidence))
        cap_source["retrieved_at"] = "2026-01-03T00:00:00Z"
        second = governance.import_cached_sources(db, Path("unused-fixture-cache"), accept_structured=True,
            samples=[{"market": "CN_A", "symbol": "000001"}])
        assert first["items"][0]["evidence_ids"] == second["items"][0]["evidence_ids"]
        assert db.scalar(select(func.count()).select_from(FoundationEvidence)) == evidence_count
        assert second["items"][0]["accepted_marketcap"] == {"facts": 0, "classifications": 0}
    selected = company_graph.security_profile(db, 1, as_of=date(2026, 1, 2))
    assert all(item["dimension"] != "SIZE" for item in selected["classifications"])


def test_unverified_size_is_not_accepted(db):
    normalized = normalize(marketcap())
    normalized["classifications"][0]["method"] = "UNVERIFIED"
    imported = company_graph.import_batch(db, {"records": [normalized]})
    assert governance.accept_source_observations(db, imported) == {"facts": 0, "classifications": 0}
    assert db.get(SecurityClassification, imported["records"][0]["classification_ids"][0]).status == "PENDING"


def test_invalid_cache_is_reported_without_writing_size(db):
    invalid = marketcap()
    invalid["records"][0]["currency"] = "HKD"
    with patch.object(governance, "latest_cache", side_effect=lambda cache, prefix, *args, **kwargs:
        profile() if prefix == "profile" else invalid if prefix == "marketcap" else None):
        result = governance.import_cached_sources(db, Path("unused-fixture-cache"), accept_structured=True,
            samples=[{"market": "CN_A", "symbol": "000001"}])
    assert result["items"][0]["sources"]["marketcap"]["status"] == "INVALID"
    assert db.scalar(select(func.count()).select_from(SecurityClassification).where(SecurityClassification.dimension == "SIZE")) == 0


def test_marketcap_source_registration_and_request_default(db):
    from app.orchestration.company_governance import GovernanceRequest

    register_company_resources(db)
    source = db.scalar(select(DataSource).where(DataSource.source_code == "TENCENT_MARKET_CAP"))
    interface = db.scalar(select(DataInterface).where(DataInterface.source_id == source.id))
    assert source.enabled and source.adapter_type == "COMPANY_MARKET_CAP" and interface.adapter_method == "fetch_market_cap"
    assert interface.supported_markets == ["CN_A"]
    assert GovernanceRequest(stock_symbol_ids=[1], request_key="fixture").include_marketcap is True
