from copy import deepcopy
import pytest
from sqlalchemy import create_engine, func, select
from sqlalchemy.orm import Session
import app.models
from app.db.base import Base
from app.connectors.company_master_sources import HKEX_SECURITIES_URL
from app.models.foundation import FoundationEntity, FoundationEvidence, FoundationFact, FoundationListing, FoundationSecurity
from app.models.market_data import DataSource, StockSymbol
from app.services.hk_company_coverage import apply_completion, completion_plan, mainland_clearing_reference, official_equities


@pytest.fixture
def db():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(engine)
    with Session(engine) as session:
        source = DataSource(source_code="FIXTURE", source_name="Fixture", adapter_type="MOCK")
        session.add(source)
        session.flush()
        session.add(StockSymbol(source_id=source.id, market="HK", symbol="00001", name="Original name", exchange="HKEX"))
        session.commit()
        yield session
    engine.dispose()


def snapshot():
    hkex = {"source_name": "HKEX", "source_url": HKEX_SECURITIES_URL, "retrieved_at": "2026-01-01T00:00:00Z",
        "source_date_label": "Updated22/09/2026", "records": [
            {"Stock Code": "00001", "Name of Securities": "EXAMPLE", "Category": "Equity", "Sub-Category": "Equity Securities (Main Board)"},
            {"Stock Code": "02917", "Name of Securities": "EXAMPLE RTS", "Category": "Equity", "Sub-Category": "Equity Securities (Main Board)"},
            {"Stock Code": "02800", "Name of Securities": "FUND", "Category": "Exchange Traded Products", "Sub-Category": "ETF"}]}
    pages = [{"source_url": "https://example.test/profile", "report_name": "RPT_HKF10_INFO_ORGPROFILE",
        "retrieved_at": "2026-01-01T00:00:00Z", "raw": {"result": {"data": [
            {"SECUCODE": f"{symbol}.HK", "SECURITY_CODE": symbol, "SECURITY_NAME_ABBR": f"Name {symbol}",
                "ORG_NAME": "Example Company", "ORG_CODE": "ORG1", "REG_PLACE": "中国"}
            for symbol in ("00001", "02917", "02800")]}}}]
    return hkex, pages


def count(db, model):
    return db.scalar(select(func.count()).select_from(model))


def test_current_official_universe_completes_without_changing_existing_master_and_is_idempotent(db):
    hkex, pages = snapshot()
    plan = completion_plan(db, hkex, pages)
    assert plan["before"]["official_equity_total"] == 2
    assert plan["add_master_codes"] == ["02917"]
    assert count(db, StockSymbol) == 1 and count(db, FoundationListing) == 0
    report = apply_completion(db, hkex, pages, snapshot_name="fixture")
    db.commit()
    assert report["after"]["official_equity_mapping_complete"]
    assert count(db, StockSymbol) == 2 and count(db, FoundationEntity) == 1
    assert count(db, FoundationFact) == 0
    assert db.scalar(select(StockSymbol).where(StockSymbol.symbol == "00001")).name == "Original name"
    added = db.scalar(select(StockSymbol).where(StockSymbol.symbol == "02917"))
    assert added.asset_type == "EQUITY"
    assert added.ext_json["official_security_name"] == "EXAMPLE RTS"
    assert all(row.share_class == "EQUITY_UNSPECIFIED" for row in db.scalars(select(FoundationSecurity)))
    before = [(row.id, row.security_id, row.evidence_id) for row in db.scalars(select(FoundationListing))]
    evidence_count = count(db, FoundationEvidence)
    repeated = apply_completion(db, hkex, pages, snapshot_name="fixture")
    assert repeated["imported"] == [] and repeated["added_master_codes"] == []
    assert count(db, FoundationEvidence) == evidence_count
    assert before == [(row.id, row.security_id, row.evidence_id) for row in db.scalars(select(FoundationListing))]


def test_missing_or_conflicting_profile_never_manufactures_a_company_or_security(db):
    hkex, pages = snapshot()
    pages[0]["raw"]["result"]["data"] = [pages[0]["raw"]["result"]["data"][0]]
    report = apply_completion(db, hkex, pages, snapshot_name="fixture")
    assert report["after"]["missing_master_codes"] == ["02917"]
    assert report["unresolved_source_records"] == [{"market": "HK", "symbol": "02917", "reason": "COMPANY_PROFILE_MISSING"}]
    assert count(db, StockSymbol) == 1


def test_conflicting_issuers_for_one_code_remain_unresolved(db):
    hkex, pages = snapshot()
    conflicting = deepcopy(pages[0]["raw"]["result"]["data"][1])
    conflicting.update(ORG_CODE="OTHER_ISSUER", ORG_NAME="Other Company")
    pages[0]["raw"]["result"]["data"].append(conflicting)
    result = apply_completion(db, hkex, pages, snapshot_name="fixture")
    assert result["unresolved_source_records"] == [
        {"market": "HK", "symbol": "02917", "reason": "CONFLICTING_PROVIDER_IDENTITIES"}]
    assert count(db, StockSymbol) == 1 and count(db, FoundationEntity) == 1


def test_bounded_import_refuses_before_any_mutation(db):
    hkex, pages = snapshot()
    with pytest.raises(ValueError, match="bounded"):
        apply_completion(db, hkex, pages, snapshot_name="fixture", max_additions=1)
    assert count(db, StockSymbol) == 1 and count(db, DataSource) == 1
    assert count(db, FoundationEntity) == 0


def test_duplicate_or_untrusted_official_list_fails_closed():
    hkex, _ = snapshot()
    duplicate = deepcopy(hkex)
    duplicate["records"].append(duplicate["records"][0])
    with pytest.raises(ValueError, match="unique"):
        official_equities(duplicate)
    hkex["source_name"] = "NAME_GUESS"
    with pytest.raises(ValueError, match="official"):
        official_equities(hkex)


@pytest.mark.parametrize("name", ["EXAMPLE (#601900)", "EXAMPLE #601900)", "EXAMPLE (A #601900)"])
def test_sdw_clearing_reference_requires_exact_official_origin_code_and_known_mainland_target(name):
    stock = StockSymbol(market="HK", symbol="91900", raw_payload={"c": "91900", "n": name},
        ext_json={"source_endpoint": "HKEX_SDW"})
    proof = mainland_clearing_reference(stock, set(), {"601900"})
    assert proof["referenced_market"] == "CN_A" and proof["referenced_symbol"] == "601900"
    assert mainland_clearing_reference(stock, {"91900"}, {"601900"}) is None
    assert mainland_clearing_reference(stock, set(), set()) is None
    stock.raw_payload = {"c": "91901", "n": name}
    assert mainland_clearing_reference(stock, set(), {"601900"}) is None
    stock.raw_payload = {"c": "91900", "n": name}
    stock.ext_json = {"source_endpoint": "UNKNOWN"}
    assert mainland_clearing_reference(stock, set(), {"601900"}) is None


def test_apply_clearing_exclusion_preserves_original_security(db):
    hkex, pages = snapshot()
    source = db.scalar(select(DataSource))
    raw = {"c": "91900", "n": "EXAMPLE (#601900)"}
    old = StockSymbol(source_id=source.id, market="HK", symbol="91900", name="Old raw name", exchange="HKEX",
        raw_payload=raw, ext_json={"source_endpoint": "HKEX_SDW"})
    db.add_all([old, StockSymbol(source_id=source.id, market="CN_A", symbol="601900", name="A share", exchange="SH")])
    db.commit()
    report = apply_completion(db, hkex, pages, snapshot_name="fixture")
    assert [r["symbol"] for r in report["clearing_exclusions"]] == ["91900"]
    assert old.market == "HK" and old.symbol == "91900" and old.raw_payload == raw
    assert db.scalar(select(FoundationListing).where(FoundationListing.stock_symbol_id == old.id)) is None
    assert apply_completion(db, hkex, pages, snapshot_name="fixture")["clearing_exclusions"] == []
