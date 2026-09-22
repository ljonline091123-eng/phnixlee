import pytest
from sqlalchemy import create_engine, func, select
from sqlalchemy.orm import Session
import app.models
from app.db.base import Base
from app.models.company_mapping import CompanyMappingState
from app.models.foundation import FoundationEntity, FoundationEvidence, FoundationFact, FoundationListing, FoundationSecurity
from app.models.market_data import DataSource, StockSymbol
from app.services import company_graph
from app.services.company_master import import_master_batch, mapping_status


@pytest.fixture
def db():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(engine)
    with Session(engine) as session:
        source = DataSource(source_code="FIXTURE", source_name="Fixture", adapter_type="MOCK")
        session.add(source)
        session.flush()
        session.add_all([StockSymbol(source_id=source.id, market=market, symbol=symbol, name=name, exchange=market)
            for market, symbol, name in [("CN_A", "000001", "Example"), ("HK", "00001", "Example H"),
                ("HK", "02800", "Example Fund"), ("NEEQ", "430001", "Example NQ")]])
        session.commit()
        yield session
    engine.dispose()


def record(market="CN_A", symbol="000001", issuer="ORG1"):
    suffix = {"CN_A": "SZ", "HK": "HK", "NEEQ": "NQ"}[market]
    result = {"market": market, "symbol": symbol, "source_name": "EASTMONEY", "source_url": "https://example.test/profile",
        "observed_at": "2026-01-01T00:00:00Z", "company": {"name": "Example Company", "jurisdiction": "CN", "source_issuer_id": issuer},
        "source_record": {"SECUCODE": f"{symbol}.{suffix}", "ORG_CODE": issuer, "ORG_NAME": "Example Company",
            "SECURITY_TYPE": "三板股" if market == "NEEQ" else "A股"}}
    if market == "HK":
        result["security_classification_evidence"] = {"source_name": "HKEX", "source_record": {
            "Stock Code": symbol, "Category": "Equity", "Sub-Category": "Equity Securities (Main Board)"}}
    return result


def stocks(db):
    return list(db.scalars(select(StockSymbol).order_by(StockSymbol.id)))


def count(db, model):
    return db.scalar(select(func.count()).select_from(model))


def test_bulk_identity_maps_ah_to_one_company_without_creating_relationships(db):
    snapshot = {"records": [record(), record("HK", "00001"), record("NEEQ", "430001", "ORG2")]}
    first = import_master_batch(db, stocks(db), snapshot)
    db.commit()
    assert first["counts"] == {"MAPPED": 3, "SOURCE_MISSING": 1}
    assert count(db, FoundationEntity) == 2 and count(db, FoundationListing) == 3
    assert count(db, FoundationFact) == 0
    mappings = list(db.scalars(select(FoundationListing)))
    prior = [(r.id, r.security_id, r.evidence_id) for r in mappings]
    proofs = count(db, FoundationEvidence)
    import_master_batch(db, stocks(db), snapshot)
    db.commit()
    assert prior == [(r.id, r.security_id, r.evidence_id) for r in db.scalars(select(FoundationListing))]
    assert count(db, FoundationEvidence) == proofs
    assert mapping_status(db)["mapped"] == 3
    page = company_graph.search(db, q="00001", market="HK")
    assert page["total"] == 1 and page["items"][0]["mapping_status"] == "MAPPED"
    assert "coverage" not in page["items"][0]
    assert page["items"][0]["canonical_security_id"] == "security:HK:00001"


def test_non_equity_needs_actual_security_classification_and_stays_unmapped(db):
    exclusion = {"market": "HK", "symbol": "02800", "source_name": "HKEX", "reason": "NON_EQUITY",
        "source_record": {"Stock Code": "02800", "Category": "Exchange Traded Products", "Sub-Category": "ETF"}}
    result = import_master_batch(db, stocks(db), {"exclusions": [exclusion]})
    assert result["counts"]["NOT_APPLICABLE"] == 1
    assert count(db, FoundationEntity) == 0
    page = company_graph.search(db, q="02800", market="HK")
    assert page["items"][0]["mapping_status"] == "NOT_APPLICABLE"
    assert "非普通股" in page["items"][0]["mapping_reason"]
    with pytest.raises(Exception, match="非普通股"):
        company_graph.security_profile(db, page["items"][0]["stock_symbol_id"])


@pytest.mark.parametrize("error", ["wrong_code", "missing_id", "wrong_name", "hk_no_classification", "multiple_issuers"])
def test_conflicts_never_create_partial_issuers_or_fake_mappings(db, error):
    value = record("HK", "00001") if error == "hk_no_classification" else record()
    if error == "wrong_code":
        value["source_record"]["SECUCODE"] = "600001.SH"
    elif error == "missing_id":
        value["source_record"].pop("ORG_CODE")
    elif error == "wrong_name":
        value["company"]["name"] = "Different company"
    elif error == "hk_no_classification":
        value.pop("security_classification_evidence")
    values = [value, record(issuer="OTHER")] if error == "multiple_issuers" else [value]
    result = import_master_batch(db, stocks(db), {"records": values})
    assert result["counts"]["CONFLICT"] == 1
    assert count(db, FoundationEntity) == 0 and count(db, FoundationListing) == 0
    assert count(db, CompanyMappingState) == 4


def test_source_conflict_does_not_replace_existing_mapping(db):
    import_master_batch(db, stocks(db), {"records": [record()]})
    existing = db.scalar(select(FoundationListing))
    saved = (existing.id, existing.security_id, existing.evidence_id)
    import_master_batch(db, stocks(db), {"records": [record(issuer="OTHER")]})
    assert count(db, FoundationEntity) == 1
    assert (existing.id, existing.security_id, existing.evidence_id) == saved


def test_cdr_preserves_unknown_domicile_and_distinct_share_class(db):
    stock = stocks(db)[0]
    stock.symbol = "689009"
    value = record(symbol="689009")
    value["source_record"].update(SECUCODE="689009.SH", SECURITY_TYPE="中国存托凭证")
    value["company"]["jurisdiction"] = "UNKNOWN"
    result = import_master_batch(db, [stock], {"records": [value]})
    assert result["counts"] == {"MAPPED": 1}
    assert db.scalar(select(FoundationEntity)).jurisdiction == "UNKNOWN"
    assert db.scalar(select(FoundationSecurity)).share_class == "DEPOSITARY_RECEIPT"


@pytest.mark.parametrize("tampered", [False, True])
def test_sdw_exclusion_requires_exact_original_code_and_explicit_mainland_reference(db, tampered):
    stock = stocks(db)[1]
    stock.ext_json = {"source_endpoint": "HKEX_SDW"}
    stock.raw_payload = {"c": "00001", "n": "EXAMPLE (A# 600001)"}
    exclusion = {"market": "HK", "symbol": "00001", "source_name": "HKEX_SDW",
        "source_record": dict(stock.raw_payload), "referenced_market": "CN_A", "referenced_symbol": "600001"}
    if tampered:
        exclusion["source_record"]["n"] = "EXAMPLE (A #600002)"
    result = import_master_batch(db, [stock], {"exclusions": [exclusion]})
    assert result["counts"] == {"CONFLICT" if tampered else "NOT_APPLICABLE": 1}
    assert count(db, FoundationEntity) == 0


def test_normal_equity_cannot_be_excluded_as_non_equity(db):
    exclusion = {"market": "HK", "symbol": "00001", "source_name": "HKEX",
        "source_record": {"Stock Code": "00001", "Category": "Equity", "Sub-Category": "Equity Securities (Main Board)"}}
    result = import_master_batch(db, [stocks(db)[1]], {"exclusions": [exclusion]})
    assert result["counts"] == {"CONFLICT": 1}


def test_official_reit_category_does_not_require_a_nonexistent_subcategory(db):
    exclusion = {"market": "HK", "symbol": "02800", "source_name": "HKEX",
        "source_record": {"Stock Code": "02800", "Category": "Real Estate Investment Trusts", "Sub-Category": ""}}
    result = import_master_batch(db, [stocks(db)[2]], {"exclusions": [exclusion]})
    assert result["counts"] == {"NOT_APPLICABLE": 1}


def test_exact_provider_historical_equity_classification_can_resolve_delisted_code(db):
    value = record("HK", "00001")
    value["security_classification_evidence"] = {"source_name": "EASTMONEY_CODETABLE", "source_record": {
        "code": "00001", "market": 116, "smallType": 3, "securityType": [102], "status": 30}}
    result = import_master_batch(db, [stocks(db)[1]], {"records": [value]})
    assert result["counts"] == {"MAPPED": 1}
    proof = db.scalar(select(FoundationEvidence))
    assert 'EASTMONEY_CODETABLE' in proof.content


@pytest.mark.parametrize("small_type, extra_type, codes, expected", [(1, 10, [6], "NOT_APPLICABLE"),
    (1, 16, [6], "NOT_APPLICABLE"), (3, 10, [6, 102], "CONFLICT"), (1, 99, [6], "CONFLICT")])
def test_provider_fund_exclusion_requires_explicit_exact_classification(db, small_type, extra_type, codes, expected):
    exclusion = {"market": "HK", "symbol": "02800", "source_name": "EASTMONEY_CODETABLE", "source_record": {
        "code": "02800", "market": 116, "smallType": small_type, "extSmallType": extra_type, "securityType": codes}}
    result = import_master_batch(db, [stocks(db)[2]], {"exclusions": [exclusion]})
    assert result["counts"] == {expected: 1}


def test_cross_market_jurisdiction_proof_must_name_same_issuer(db):
    value = record()
    value["company"]["jurisdiction"] = "HK"
    value["registration_jurisdiction_evidence"] = {"source_record": {
        "ORG_CODE": "OTHER", "ORG_NAME": value["company"]["name"], "SECUCODE": "00001.HK", "REG_PLACE": "香港"}}
    result = import_master_batch(db, [stocks(db)[0]], {"records": [value]})
    assert result["counts"] == {"CONFLICT": 1}
    assert count(db, FoundationEntity) == 0
