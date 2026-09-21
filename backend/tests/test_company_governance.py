"""Isolated fixtures for literal-source governance, without external requests."""

from copy import deepcopy
from datetime import date, datetime, timedelta, timezone
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch

import pytest
from sqlalchemy import create_engine, func, select
from sqlalchemy.orm import Session

from app.db.base import Base
from app.models.ai_hub import AgentDefinition, AgentSkillLink, AgentDataSourceLink, ModelSkill
from app.models.company_graph import SecurityClassification
from app.models.foundation import FoundationEntity, FoundationEvidence, FoundationFact
from app.models.market_data import DataInterface, DataSource, StockSymbol
from app.schemas.foundation import FactCreate
from app.services import company_governance as governance
from app.services import company_graph, company_research, foundation
from app.services.company_registry import register_company_resources


@pytest.fixture
def db():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(engine)
    with Session(engine) as session:
        source = DataSource(source_code="GOVERNANCE_TEST", source_name="Fixture", adapter_type="MOCK")
        session.add(source)
        session.flush()
        session.add(StockSymbol(market="CN_A", symbol="000001", name="Fixture Issuer", exchange="SZ", source_id=source.id))
        session.commit()
        yield session
    engine.dispose()


def source_profile():
    return {
        "status": "SUCCESS", "source_name": "EASTMONEY", "source_code": "EASTMONEY_COMPANY_PROFILE",
        "source_url": "https://example.test/company", "retrieved_at": "2026-01-01T00:00:00Z",
        "warnings": ["Provider aggregation, not official registry verification."],
        "records": [{"market": "CN_A", "symbol": "000001",
            "company": {"name": "Fixture Issuer", "jurisdiction": "CN", "source_issuer_id": "FIXTURE-ORG-1",
                "identifier_scheme": "TEST_REGISTER", "identifier_value": "FIXTURE-REGISTER-1", "properties_json": {"registered_capital": 100}},
            "industries": [{"code": "BANK", "name": "Banking", "source_issuer_id": "industry:BANK",
                "taxonomy": "PROVIDER", "taxonomy_version": "snapshot-v1", "level": 1}],
            "themes": [{"code": "PROVIDER_THEME", "name": "Provider theme", "source_issuer_id": "theme:1", "taxonomy": "PROVIDER", "taxonomy_version": "snapshot-v1"}],
            "controller_mentions": [{"source_issuer_id": "FIXTURE-PARENT", "name": "Fixture Parent", "mention_type": "CONTROLLING_HOLDER",
                "direct_ratio": 60.0, "ratio_basis": "PROVIDER_DIRECT_EQUITY_RATIO"}],
            "evidence": {"source_key": "company:FIXTURE-ORG-1:CN_A:000001", "title": "Fixture structured profile",
                "content": '{"ORG_CODE":"FIXTURE-ORG-1","ORG_NAME":"Fixture Issuer"}', "published_at": None,
                "available_at": "2026-01-01T00:00:00Z"}}],
    }


def parent_identity():
    return {"name": "Fixture Parent", "jurisdiction": "CN", "source_issuer_id": "FIXTURE-PARENT"}


def source_holders():
    return {"status": "SUCCESS", "source_name": "EASTMONEY", "source_url": "https://example.test/holders",
        "retrieved_at": "2026-01-01T00:00:00Z", "warnings": ["Top ten only; beneficial ownership unresolved"],
        "records": [{"holder_name": "Fixture Custody Account", "report_date": "2025-12-31", "ratio": 10,
            "shares": 1000, "ratio_basis": "TOTAL_ISSUED_SHARES", "shares_unit": "SHARES", "share_class": "A_SHARE",
            "identity_status": "UNRESOLVED_REGISTER_HOLDER", "beneficial_owner_verified": False, "holder_rank": 1,
            "source_record": {"HOLDER_NAME": "Fixture Custody Account", "HOLD_RATIO": 10, "END_DATE": "2025-12-31"}}]}


def count(db, model):
    return int(db.scalar(select(func.count()).select_from(model)) or 0)


def stock_id(db):
    return db.scalar(select(StockSymbol.id).where(StockSymbol.symbol == "000001"))


def test_registration_is_additive_and_preserves_user_configuration(db):
    source = DataSource(source_code="EASTMONEY_COMPANY_PROFILE", source_name="User Source", adapter_type="USER_ADAPTER",
        enabled=False, priority=7, config_json={"user_setting": "keep"})
    agent = AgentDefinition(agent_code="COMPANY_GRAPH_GOVERNOR", display_name="User Agent", system_prompt="User prompt",
        enabled=False, version="9.0.0")
    skill = ModelSkill(skill_code="COMPANY_MASTER_RESOLVER", skill_name="User Skill", instructions="User instructions",
        enabled=False, version="8.0.0", config_json={"custom": True})
    db.add_all([source, agent, skill])
    db.flush()
    interface = DataInterface(source_id=source.id, interface_code="COMPANY_PROFILE", interface_name="User Interface",
        data_category="USER", adapter_method="custom_fetch", enabled=False, supported_markets=["CN_A"], input_schema={}, output_schema={})
    db.add(interface)
    db.commit()
    first = register_company_resources(db)
    db.commit()
    counts = [count(db, model) for model in (DataSource, DataInterface, AgentDefinition, ModelSkill, AgentSkillLink, AgentDataSourceLink)]
    second = register_company_resources(db)
    db.commit()
    assert first == second
    assert counts == [count(db, model) for model in (DataSource, DataInterface, AgentDefinition, ModelSkill, AgentSkillLink, AgentDataSourceLink)]
    assert source.source_name == "User Source" and source.enabled is False and source.priority == 7
    assert source.adapter_type == "USER_ADAPTER" and source.config_json == {"user_setting": "keep"}
    assert agent.system_prompt == "User prompt" and agent.version == "9.0.0" and agent.enabled is False
    assert skill.instructions == "User instructions" and skill.version == "8.0.0" and skill.enabled is False
    assert interface.adapter_method == "custom_fetch" and interface.enabled is False


def test_profile_normalization_preserves_identity_direction_and_source_scope(db):
    source = source_profile()
    result = governance.profile_record(source, stock_id(db), {"FIXTURE-PARENT": parent_identity()})
    assert result["source_kind"] == "AGGREGATOR"
    assert result["company"]["source_issuer_id"] == "FIXTURE-ORG-1"
    assert result["company"]["identifier_value"] == "FIXTURE-REGISTER-1"
    assert all(row["direction"] == "INCOMING" for row in result["facts"] if row["fact_type"] in {"HOLDS_EQUITY", "CONTROLS"})
    assert all(row["properties_json"]["temporal_scope"] == "SOURCE_SNAPSHOT_UNKNOWN_BUSINESS_DATE"
        for row in result["facts"] if row["fact_type"] == "HOLDS_EQUITY")
    assert {row["dimension"] for row in result["classifications"]} == {"THEME", "LEGAL_LISTING_CLASS"}
    assert not any(row["fact_type"] == "HAS_CLASSIFICATION" for row in result["facts"])
    unknown = governance.profile_record(source, stock_id(db), {})
    assert all(row["fact_type"] == "IN_INDUSTRY" for row in unknown["facts"])


def test_acceptance_rule_excludes_control_legal_and_size_inferences(db):
    normalized = governance.profile_record(source_profile(), stock_id(db), {"FIXTURE-PARENT": parent_identity()})
    normalized["facts"].append({"source_key": "fixture-case", "fact_type": "LEGAL_CASE", "title": "Fixture case mention",
        "properties_json": {"case_number": "FIXTURE-1", "court": "Fixture Court", "jurisdiction": "CN",
            "procedure_type": "CIVIL", "party_role": "DEFENDANT", "case_status": "PENDING", "acceptance_rule": governance.RULE_VERSION}})
    normalized["classifications"].append({"dimension": "SIZE", "code": "LARGE_CAP", "label": "Unverified size",
        "definition_version": "fixture", "method": "UNVERIFIED", "properties_json": {"acceptance_rule": governance.RULE_VERSION}})
    imported = company_graph.import_batch(db, {"records": [normalized]})
    governance.accept_source_observations(db, imported)
    db.commit()
    facts = list(db.scalars(select(FoundationFact)))
    assert all(row.status == "ACCEPTED" for row in facts if row.fact_type in {"IN_INDUSTRY", "HOLDS_EQUITY"})
    assert all(row.status == "PENDING" for row in facts if row.fact_type in {"CONTROLS", "LEGAL_CASE"})
    classifications = list(db.scalars(select(SecurityClassification)))
    assert all(row.status == "PENDING" for row in classifications if row.dimension == "SIZE")
    assert governance.accept_source_observations(db, imported) == {"facts": 0, "classifications": 0}


def test_holder_observation_is_scoped_account_and_one_day_snapshot(db):
    profile = source_profile()
    company_graph.import_batch(db, {"records": [governance.profile_record(profile, stock_id(db), {})]})
    normalized = governance.holder_record(source_holders(), profile, stock_id(db))
    imported = company_graph.import_batch(db, {"records": [normalized]})
    governance.accept_source_observations(db, imported)
    db.commit()
    fact = db.get(FoundationFact, imported["records"][0]["fact_ids"][0])
    assert fact.valid_from == date(2025, 12, 31) and fact.valid_to == date(2026, 1, 1)
    assert fact.properties_json["temporal_scope"] == "AS_OF_OBSERVATION"
    holder = db.get(FoundationEntity, fact.subject_entity_id)
    assert holder.entity_type == "HOLDER_ACCOUNT"
    assert holder.properties_json["beneficial_owner_verified"] is False
    assert fact.object_entity_id == imported["records"][0]["company_id"]
    assert db.scalars(foundation.facts_query(as_of=date(2026, 1, 1)).where(FoundationFact.id == fact.id)).first() is None


def test_cached_source_import_is_idempotent_across_retrieval_times(db):
    profile, holders = source_profile(), source_holders()
    samples = [{"market": "CN_A", "symbol": "000001"}]

    def compatible_cache(*args, **kwargs):
        prefix = args[1]
        return profile if prefix == "profile" else holders if prefix == "holders" else None

    with patch.object(governance, "latest_cache", side_effect=compatible_cache):
        first = governance.import_cached_sources(db, Path("unused-fixture-cache"), accept_structured=True, samples=samples)
        db.commit()
        counts = [count(db, model) for model in (FoundationEntity, FoundationEvidence, FoundationFact, SecurityClassification)]
        profile["retrieved_at"] = "2026-02-01T00:00:00Z"
        profile["records"][0]["evidence"]["available_at"] = profile["retrieved_at"]
        holders["retrieved_at"] = "2026-02-01T00:00:00Z"
        second = governance.import_cached_sources(db, Path("unused-fixture-cache"), accept_structured=True, samples=samples)
        db.commit()
    assert first["mapped"] == second["mapped"] == 1
    assert counts == [count(db, model) for model in (FoundationEntity, FoundationEvidence, FoundationFact, SecurityClassification)]
    assert first["items"][0]["evidence_ids"] == second["items"][0]["evidence_ids"]


def test_research_bridge_uses_only_accepted_available_facts(db):
    normalized = governance.profile_record(source_profile(), stock_id(db), {"FIXTURE-PARENT": parent_identity()})
    imported = company_graph.import_batch(db, {"records": [normalized]})
    governance.accept_source_observations(db, imported)
    db.commit()
    with patch.object(company_research, "get_settings", return_value=SimpleNamespace(
        data_foundation_enabled=True, company_research_context_enabled=True)):
        result = company_research.load_company_context(db, "CN_A", "000001")
    assert result["status"] == "AVAILABLE"
    assert result["facts"] and all(row["status"] == "ACCEPTED" for row in result["facts"])
    assert all(row["fact_type"] != "CONTROLS" for row in result["facts"])
    assert all(row["fact_type"] != "HOLDS_EQUITY" for row in result["facts"])
    assert all(row["evidence"] for row in result["facts"])
    assert "Company" not in company_research.company_context_markdown({"status": "UNMAPPED"})


def test_research_disabled_is_read_only_no_database_queries(db):
    with patch.object(company_research, "get_settings", return_value=SimpleNamespace(
        data_foundation_enabled=False, company_research_context_enabled=True)):
        with patch.object(db, "connection", side_effect=AssertionError("disabled research queried database")):
            assert company_research.load_company_context(db, "CN_A", "000001") == {}


def test_one_announcement_retains_both_uses_without_changing_evidence_version(db):
    imported = company_graph.import_batch(db, {"records": [governance.profile_record(source_profile(), stock_id(db), {})]})
    company_id = imported["records"][0]["company_id"]
    source = {"source_name": "CNINFO", "retrieved_at": "2026-01-01T00:00:00Z", "request": {}, "warnings": [],
        "records": [{"announcement_id": "fixture-announcement", "title": "Contract and lawsuit disclosure",
            "content": "Exact immutable announcement text", "published_at": "2025-12-30T00:00:00Z",
            "url": "https://example.test/notice.pdf", "pages": [{"page": 1, "text": "Exact immutable announcement text"}],
            "extraction_status": "TEXT_AVAILABLE", "content_scope": "PDF_EXTRACTED_TEXT", "case_mentions": []}]}
    first = governance.archive_disclosures(db, source, company_id, "LEGAL_DISCLOSURE")[0]
    db.commit()
    evidence = db.get(FoundationEvidence, first)
    original = (evidence.content, evidence.version, evidence.content_hash, evidence.fingerprint)
    second = governance.archive_disclosures(db, source, company_id, "BUSINESS_DISCLOSURE")[0]
    db.commit()
    assert first == second
    assert (evidence.content, evidence.version, evidence.content_hash, evidence.fingerprint) == original
    assert set(evidence.metadata_json["document_kinds"]) == {"LEGAL_DISCLOSURE", "BUSINESS_DISCLOSURE"}
    profile = company_graph.company_profile(db, company_id)
    assert first in {item["id"] for item in profile["legal_disclosures"]}
    assert first in {item["id"] for item in profile["business_disclosures"]}
    assignments = evidence.metadata_json["document_kind_assignments"]
    governance.archive_disclosures(db, source, company_id, "BUSINESS_DISCLOSURE")
    assert evidence.metadata_json["document_kind_assignments"] == assignments
    new_use_at = datetime.fromisoformat(assignments[-1]["recorded_at"])
    serialized = foundation.evidence_read(evidence)
    assert not company_graph._document_has_kind(serialized, "BUSINESS_DISCLOSURE", new_use_at - timedelta(microseconds=1))
    assert company_graph._document_has_kind(serialized, "BUSINESS_DISCLOSURE", new_use_at)


def test_profile_failure_remains_explicit_in_batch_coverage(db):
    source = source_profile()
    source.update(status="UNAVAILABLE", records=[], warnings=["Fixture source unavailable"])
    with patch.object(governance, "latest_cache", return_value=source):
        result = governance.import_cached_sources(db, Path("unused-fixture-cache"), samples=[{"market": "CN_A", "symbol": "000001"}])
    assert result["mapped"] == 0 and result["requested"] == 1
    assert result["items"][0]["status"] == "PROFILE_UNAVAILABLE"
    assert result["items"][0]["sources"]["profile"]["status"] == "UNAVAILABLE"
    assert count(db, FoundationEntity) == 0


def test_supply_disclosure_ingestion_preserves_partial_text_and_does_not_create_relationships(db):
    profile_source = source_profile()
    disclosure = {"source_name": "CNINFO", "status": "PARTIAL", "retrieved_at": "2026-01-01T00:00:00Z",
        "request": {"max_documents": 2}, "warnings": ["Truncated PDF"], "records": [{
            "announcement_id": "supply-fixture", "title": "2025 annual report", "content": "Supplier 1 is anonymous.",
            "pages": [{"page": 32, "text": "Supplier 1 is anonymous."}], "page_count": 150, "text_truncated": True,
            "matched_search_terms": ["年度报告"], "extraction_status": "TEXT_AVAILABLE"}]}
    def cached(*args, **kwargs):
        return {"profile": profile_source, "supply_chain": disclosure}.get(args[1])
    with patch.object(governance, "latest_cache", side_effect=cached):
        result = governance.import_cached_sources(db, Path("unused"), samples=[{"market": "CN_A", "symbol": "000001"}])
    company_id = result["items"][0]["company_id"]
    profile = company_graph.company_profile(db, company_id)
    proof = profile["supply_chain_disclosures"][0]
    assert proof["metadata_json"]["text_truncated"] is True
    assert proof["metadata_json"]["page_count"] == 150
    assert proof["metadata_json"]["pages"][0]["page"] == 32
    assert profile["facts"]["business"] == []
    dimension = next(row for row in profile["coverage"]["dimensions"] if row["key"] == "supply_chain")
    assert dimension["count"] == 0 and dimension["disclosure_count"] == 1
    assert dimension["disclosure_status"] == "EVIDENCE_ONLY"
    assert "supply_chain" in profile["coverage"]["missing"]
    assert result["source_status"] == "PARTIAL"
    count_before = count(db, FoundationEvidence)
    governance.archive_disclosures(db, disclosure, company_id, "SUPPLY_CHAIN_DISCLOSURE")
    assert count(db, FoundationEvidence) == count_before


def test_governance_ids_reject_boolean_and_string():
    from pydantic import ValidationError
    from app.orchestration.company_governance import GovernanceRequest

    for value in (True, "1"):
        with pytest.raises(ValidationError):
            GovernanceRequest(stock_symbol_ids=[value], request_key="fixture")


def test_workflow_reports_partial_when_profile_unavailable(db, tmp_path):
    from app.orchestration import company_governance as workflow
    from app.connectors import company_sources

    failed_result = {"mapped": 0, "requested": 1, "items": [{"status": "PROFILE_UNAVAILABLE", "sources": {}}]}
    with patch.object(workflow, "SessionLocal", side_effect=lambda: Session(db.get_bind())), \
         patch.object(workflow.tempfile, "mkdtemp", return_value=str(tmp_path)), \
         patch.object(company_sources, "CompanySourcesClient"), \
         patch.object(governance, "import_cached_sources", return_value=failed_result):
        result = workflow.CompanyGovernanceWorkflow().execute({"stock_symbol_ids": [stock_id(db)],
            "request_key": "fixture", "include_holders": False, "include_legal": False, "include_business": False})
    assert result["source_status"] == "PARTIAL"


def test_business_disclosure_is_not_labeled_as_case_participation(db):
    imported = company_graph.import_batch(db, {"records": [governance.profile_record(source_profile(), stock_id(db), {})]})
    company_id = imported["records"][0]["company_id"]
    source = {"source_name": "CNINFO", "retrieved_at": "2026-01-01T00:00:00Z", "request": {}, "warnings": [],
        "records": [{"announcement_id": "fixture-business", "title": "Business disclosure", "content": "Subsidiary contract terms."}]}
    proof_id = governance.archive_disclosures(db, source, company_id, "BUSINESS_DISCLOSURE")[0]
    assert db.get(FoundationEvidence, proof_id).metadata_json["association_role"] == "DISCLOSING_COMPANY_NOT_VERIFIED_TRANSACTION_PARTY"


@pytest.mark.parametrize("key,source_code,method", [
    ("legal", "CNINFO_LEGAL_DISCLOSURES", "fetch_legal_disclosures"),
    ("business", "CNINFO_BUSINESS_DISCLOSURES", "fetch_business_disclosures"),
    ("supply_chain", "CNINFO_SUPPLY_CHAIN_DISCLOSURES", "fetch_supply_chain_disclosures"),
    ("marketcap", "TENCENT_MARKET_CAP", "fetch_market_cap"),
])
@pytest.mark.parametrize("market,requested,enabled", [
    ("CN_A", True, False),
    ("HK", True, False),
    ("CN_A", False, False),
    ("HK", False, True),
    ("CN_A", True, True),
    ("HK", True, True),
])
def test_workflow_optional_source_status_matches_request_and_capability(
    db, tmp_path, key, source_code, method, market, requested, enabled,
):
    from app.orchestration import company_governance as workflow
    from app.connectors import company_sources

    register_company_resources(db)
    source = db.scalar(select(DataSource).where(DataSource.source_code == source_code))
    source.enabled = enabled
    stock = db.get(StockSymbol, stock_id(db))
    stock.market = market
    symbol_id = stock.id
    db.commit()

    collected = requested and enabled and (market == "CN_A" or key == "marketcap")
    cached_status = "UNSUPPORTED" if market == "HK" else "SUCCESS"
    sources = {name: {"status": "NOT_REQUESTED", "warnings": []}
               for name in ("holders", "legal", "business", "marketcap", "supply_chain")}
    sources["profile"] = {"status": "SUCCESS", "warnings": []}
    if collected:
        sources[key] = {"status": cached_status, "warnings": ["Original provider warning"]}
    imported = {"mapped": 1, "requested": 1, "items": [
        {"market": market, "symbol": stock.symbol, "status": "MAPPED", "sources": sources}]}
    payload = {"stock_symbol_ids": [symbol_id], "request_key": "source-status-fixture",
        "include_holders": False, "include_legal": False, "include_business": False, "include_marketcap": False}
    payload["include_" + key] = requested

    with patch.object(workflow, "SessionLocal", side_effect=lambda: Session(db.get_bind())), \
         patch.object(workflow.tempfile, "mkdtemp", return_value=str(tmp_path)), \
         patch.object(company_sources, "CompanySourcesClient") as client_factory, \
         patch.object(governance, "import_cached_sources", return_value=imported):
        result = workflow.CompanyGovernanceWorkflow().execute(payload)
        client = client_factory.return_value.__enter__.return_value
        assert getattr(client, method).call_count == int(collected)

    expected = ("NOT_REQUESTED" if not requested else "DISABLED" if not enabled
                else "UNSUPPORTED" if market == "HK" else "SUCCESS")
    actual = result["items"][0]["sources"][key]
    assert actual["status"] == expected
    if expected in {"DISABLED", "UNSUPPORTED"}:
        assert actual["warnings"]
    if collected:
        assert actual["warnings"] == ["Original provider warning"]
    assert result["source_status"] == (
        "PARTIAL" if expected in {"DISABLED", "UNSUPPORTED"} else "COMPLETE_WITH_COVERAGE_LIMITS")
