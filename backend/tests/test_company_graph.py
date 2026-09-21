from copy import deepcopy
from datetime import datetime, timedelta, timezone
from unittest.mock import patch

from fastapi import FastAPI
from fastapi.testclient import TestClient
import pytest
from sqlalchemy import create_engine, func, select
from sqlalchemy.orm import Session
from sqlalchemy.pool import StaticPool

from app.api.company_graph import router
from app.api.foundation import router as foundation_router
from app.db.base import Base
from app.db.session import get_db
from app.models.company_graph import SecurityClassification, SourceIdentity
from app.models.foundation import FoundationEntity, FoundationEvidence, FoundationFact, FoundationListing
from app.models.market_data import DataSource, StockSymbol
from app.schemas.foundation import EvidenceCreate, FactCreate, FactReviewCreate
from app.services import company_graph as service
from app.services import foundation as base


@pytest.fixture
def context():
    engine = create_engine("sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool)
    Base.metadata.create_all(engine)
    with Session(engine) as db:
        source = DataSource(source_code="COMPANY_GRAPH_TEST", source_name="Fixture", adapter_type="MOCK")
        db.add(source)
        db.flush()
        stocks = [StockSymbol(market=market, symbol=symbol, name=name, exchange=market, source_id=source.id)
            for market, symbol, name in [("CN_A", "000001", "Issuer A"), ("HK", "000001", "Issuer A H"),
                                        ("CN_A", "000002", "Other Issuer")]]
        db.add_all(stocks)
        db.commit()
        app = FastAPI()
        app.include_router(router, prefix="/api/v1")
        app.include_router(foundation_router, prefix="/api/v1")
        app.dependency_overrides[get_db] = lambda: db
        with TestClient(app) as client:
            yield db, client, stocks
    engine.dispose()


def record(stock, **changes):
    value = {
        "source_name": "TEST_PROVIDER", "source_key": f"issuer:ORG-1:{stock.market}:{stock.symbol}",
        "source_kind": "AGGREGATOR", "source_url": "https://example.test/disclosure",
        "observed_at": "2026-01-01T00:00:00Z",
        "company": {"name": "Same Legal Name", "jurisdiction": "CN", "source_issuer_id": "ORG-1",
                    "identifier_scheme": "REGISTRATION", "identifier_value": "REGISTER-1", "properties_json": {"registered_capital": 100}},
        "security": {"stock_symbol_id": stock.id, "share_class": "H" if stock.market == "HK" else "A"},
        "evidence": {"title": "Issuer profile", "content": "  Exact source JSON\n", "available_at": "2026-01-01T00:00:00Z"},
        "facts": [], "classifications": [],
    }
    value.update(changes)
    return value


def import_record(db, stock, **changes):
    result = service.import_batch(db, {"records": [record(stock, **changes)]})["records"][0]
    db.commit()
    return result


def accept(db, fact_id):
    base.review_fact(db, fact_id, FactReviewCreate(decision="ACCEPTED", expected_status="PENDING", reviewer="test-reviewer", reason="Verified test fixture"))
    db.commit()


def test_ah_source_issuer_identity_and_same_name_separation(context):
    db, client, stocks = context
    first = import_record(db, stocks[0])
    hk = record(stocks[1])
    hk["company"].pop("identifier_scheme")
    hk["company"].pop("identifier_value")
    second = service.import_batch(db, {"records": [hk]})["records"][0]
    db.commit()
    assert first["company_id"] == second["company_id"]
    assert first["security_id"] != second["security_id"]
    other = import_record(db, stocks[2], company={"name": "Same Legal Name", "jurisdiction": "CN", "source_issuer_id": "ORG-OTHER"})
    assert other["company_id"] != first["company_id"]
    profile = client.get(f"/api/v1/company-graph/companies/{first['company_id']}").json()
    assert {row["market"] for row in profile["securities"]} == {"CN_A", "HK"}
    found = client.get("/api/v1/company-graph/search", params={"mode": "company", "q": "Same Legal Name"}).json()
    assert found["total"] == 2
    selected = client.get("/api/v1/company-graph/by-symbol", params={"market": "HK", "symbol": "000001"}).json()
    assert selected["selected_security"]["stock_symbol_id"] == stocks[1].id


def test_h_first_then_a_identifier_alias_keeps_one_company(context):
    db, _, stocks = context
    first = import_record(db, stocks[1], company={"name": "Same Legal Name", "jurisdiction": "CN", "source_issuer_id": "ORG-1"})
    second = import_record(db, stocks[0])
    assert first["company_id"] == second["company_id"]
    assert db.scalar(select(func.count()).select_from(FoundationEntity)) == 1
    assert db.scalar(select(func.count()).select_from(SourceIdentity)) == 2


def test_repeated_fetch_is_idempotent_even_when_retrieval_time_changes(context):
    db, _, stocks = context
    incoming = record(stocks[0])
    incoming["facts"] = [{"source_key": "sector", "fact_type": "IN_INDUSTRY", "title": "Source industry",
        "object": {"name": "Banking", "entity_type": "INDUSTRY", "jurisdiction": "CN", "source_issuer_id": "industry:bank"},
        "properties_json": {"taxonomy": "TEST", "taxonomy_version": "v1"}}]
    first = service.import_batch(db, {"records": [incoming]})
    db.commit()
    repeated = deepcopy(incoming)
    repeated["observed_at"] = "2026-02-01T00:00:00Z"
    repeated["evidence"]["available_at"] = "2026-02-01T00:00:00Z"
    second = service.import_batch(db, {"records": [repeated]})
    db.commit()
    assert first["records"][0]["evidence_id"] == second["records"][0]["evidence_id"]
    assert first["records"][0]["fact_ids"] == second["records"][0]["fact_ids"]
    assert db.scalar(select(func.count()).select_from(FoundationEvidence)) == 1
    assert db.scalar(select(func.count()).select_from(FoundationFact)) == 1
    proof = db.get(FoundationEvidence, first["records"][0]["evidence_id"])
    assert proof.content == "  Exact source JSON\n"
    assert base.aware(proof.available_at) == datetime(2026, 1, 1, tzinfo=timezone.utc)


def test_registry_conflict_rejects_without_overwriting_source_identity(context):
    db, client, stocks = context
    first = import_record(db, stocks[0])
    wrong = record(stocks[1])
    wrong["company"]["identifier_value"] = "WRONG-REGISTER"
    response = client.post("/api/v1/company-graph/imports", json={"records": [wrong]})
    assert response.status_code == 409
    assert db.scalar(select(func.count()).select_from(FoundationListing)) == 1
    assert db.get(FoundationEntity, first["company_id"]).identifier_value == "REGISTER-1"


def test_name_without_identity_rejected_and_unmapped_security_visible(context):
    db, client, stocks = context
    wrong = record(stocks[0], company={"name": "Name alone", "jurisdiction": "CN"})
    assert client.post("/api/v1/company-graph/imports", json={"records": [wrong]}).status_code == 422
    data = client.get("/api/v1/company-graph/search", params={"q": "000001", "market": "HK"}).json()
    assert data["total"] == 1 and data["items"][0]["mapping_status"] == "UNMAPPED"
    assert "identity" in data["items"][0]["coverage"]["missing"]
    sample = client.get("/api/v1/company-graph/samples").json()
    assert sample["total"] == 25
    assert any(row["mapping_status"] == "STOCK_MISSING" for row in sample["items"])
    assert any(row["symbol"] == "000001" and row["mapping_status"] == "UNMAPPED" for row in sample["items"])


def test_holder_account_is_incoming_holding_not_issuer_ownership(context):
    db, _, stocks = context
    holding = {"source_key": "holders:2025Q4:account-A", "direction": "INCOMING", "fact_type": "HOLDS_EQUITY",
        "title": "Disclosed registered holding", "object": {"name": "Registered account", "entity_type": "HOLDER_ACCOUNT",
            "jurisdiction": "CN", "source_issuer_id": "issuer:ORG-1:account:A",
            "properties_json": {"identity_status": "UNRESOLVED_REGISTER_HOLDER"}},
        "properties_json": {"ratio": 20, "ratio_basis": "TOTAL_ISSUED_SHARES"}}
    imported = import_record(db, stocks[0], facts=[holding])
    fact = db.get(FoundationFact, imported["fact_ids"][0])
    assert fact.object_entity_id == imported["company_id"]
    assert db.get(FoundationEntity, fact.subject_entity_id).entity_type == "HOLDER_ACCOUNT"
    assert fact.status == "PENDING"
    graph = service.graph(db, stock_symbol_id=stocks[0].id, include_pending=True)
    holding_edges = [edge for edge in graph["edges"] if edge["type"] == "HOLDS_EQUITY"]
    assert holding_edges and holding_edges[0]["properties_json"]["ratio"] == 20
    assert holding_edges[0]["properties_json"]["ratio_basis"] == "TOTAL_ISSUED_SHARES"
    with pytest.raises(base.FoundationError):
        base.create_fact(db, FactCreate(fact_type="CONTROLS", title="Not a control assertion",
            subject_entity_id=fact.subject_entity_id, object_entity_id=fact.object_entity_id,
            properties_json={"control_basis": "none"}, evidence_ids=[imported["evidence_id"]]))


def test_security_classification_does_not_spill_across_ah(context):
    db, client, stocks = context
    first = import_record(db, stocks[0], classifications=[{"dimension": "SIZE", "code": "LARGE_CAP", "label": "Large cap",
        "definition_version": "test-v1", "method": "TEST_RULE", "properties_json": {"universe": "CN_A"}}])
    second = import_record(db, stocks[1])
    classification_id = first["classification_ids"][0]
    profile = service.security_profile(db, stocks[1].id)
    assert "classification" in profile["coverage"]["missing"]
    assert all(item["security_id"] != second["security_id"] for item in profile["classifications"])
    assert db.scalar(select(func.count()).select_from(FoundationFact)) == 0
    review = client.post(f"/api/v1/company-graph/classifications/{classification_id}/review", json={
        "decision": "ACCEPTED", "expected_status": "PENDING", "reason": "Verified calculation", "reviewer": "test"})
    assert review.status_code == 200
    assert "classification" not in service.security_profile(db, stocks[0].id)["coverage"]["missing"]
    assert "classification" in service.security_profile(db, stocks[1].id)["coverage"]["missing"]


def test_bounded_graph_defaults_to_accepted_and_respects_business_dates(context):
    db, _, stocks = context
    first = import_record(db, stocks[0])
    second = import_record(db, stocks[2], company={"name": "Partner", "jurisdiction": "CN", "source_issuer_id": "ORG-2"})
    item = base.create_fact(db, FactCreate(fact_type="SUPPLIES_TO", title="Observed supply",
        subject_entity_id=first["company_id"], object_entity_id=second["company_id"], evidence_ids=[first["evidence_id"]],
        properties_json={"product": "Reported goods"}, valid_from="2026-01-01", valid_to="2026-02-01"))
    db.commit()
    assert not any(edge["type"] == "SUPPLIES_TO" for edge in service.graph(db, company_id=first["company_id"])["edges"])
    candidate = service.graph(db, stock_symbol_id=stocks[0].id, include_pending=True)
    assert any(edge["status"] == "PENDING" for edge in candidate["edges"])
    assert candidate["paths"][0]["node_ids"][0] == first["security_id"]
    accept(db, item.id)
    assert any(edge["type"] == "SUPPLIES_TO" for edge in service.graph(db, company_id=first["company_id"], as_of=datetime(2026, 1, 31).date())["edges"])
    assert not any(edge["type"] == "SUPPLIES_TO" for edge in service.graph(db, company_id=first["company_id"], as_of=datetime(2026, 2, 1).date())["edges"])
    limited = service.graph(db, stock_symbol_id=stocks[0].id, max_nodes=3, max_edges=1, include_pending=True)
    assert len(limited["nodes"]) <= 3 and len(limited["edges"]) <= 1 and limited["truncated"]


def test_legal_disclosure_is_not_a_verified_case(context):
    db, _, stocks = context
    imported = import_record(db, stocks[0])
    base.create_evidence(db, EvidenceCreate(entity_id=imported["company_id"], source_name="TEST_DISCLOSURE", source_key="notice-1",
        title="Subsidiary litigation update", content="Document does not resolve the parent as a case party."),
        metadata={"document_kind": "LEGAL_DISCLOSURE", "party_resolution": "UNRESOLVED"})
    db.commit()
    profile = service.company_profile(db, imported["company_id"])
    assert len(profile["legal_disclosures"]) == 1
    assert profile["legal_disclosures"][0]["association_role"] == "DISCLOSING_COMPANY_NOT_CONFIRMED_CASE_PARTY"
    assert profile["facts"]["legal"] == []
    assert "legal" in profile["coverage"]["missing"]


def test_classification_review_replay_and_recorded_time_filter(context):
    db, _, stocks = context
    day = datetime(2026, 1, 2, tzinfo=timezone.utc)
    with patch.object(base, "utc_now", return_value=day):
        imported = import_record(db, stocks[0], classifications=[{"dimension": "STYLE", "code": "VALUE", "label": "Value",
            "definition_version": "test-v1", "method": "TEST_RULE"}])
    row_id = imported["classification_ids"][0]
    with patch.object(base, "utc_now", return_value=day + timedelta(days=1)):
        service.review_classification(db, row_id, FactReviewCreate(decision="ACCEPTED", expected_status="PENDING", reason="Accepted", reviewer="test"))
    with patch.object(base, "utc_now", return_value=day + timedelta(days=3)):
        service.review_classification(db, row_id, FactReviewCreate(decision="REJECTED", expected_status="ACCEPTED", reason="Revoked", reviewer="test"))
    db.commit()
    history = service.company_profile(db, imported["company_id"], known_at=day + timedelta(days=2))
    assert history["classifications"][0]["status"] == "ACCEPTED"
    assert len(history["classifications"][0]["reviews"]) == 1
    assert "classification" not in history["coverage"]["missing"]
    with pytest.raises(base.FoundationError):
        service.company_profile(db, imported["company_id"], known_at=day - timedelta(days=1))


def test_api_caps_graph_and_rejects_ambiguous_entry(context):
    _, client, _ = context
    assert client.get("/api/v1/company-graph/graph", params={"depth": 20}).status_code == 422
    assert client.get("/api/v1/company-graph/graph").status_code == 422
    assert client.get("/api/v1/company-graph/search?limit=1000").status_code == 422


def test_import_rejects_invalid_content_and_relationship_before_writes(context):
    db, client, stocks = context
    value = record(stocks[0])
    value["evidence"]["content"] = "  \n"
    assert client.post("/api/v1/company-graph/imports", json={"records": [value]}).status_code == 422
    value = record(stocks[0], facts=[{"source_key": "invalid", "fact_type": "SUPPLIES_TO", "title": "Missing counterparty",
        "properties_json": {"product": "goods"}}])
    assert client.post("/api/v1/company-graph/imports", json={"records": [value]}).status_code == 422
    assert db.scalar(select(func.count()).select_from(FoundationEntity)) == 0
