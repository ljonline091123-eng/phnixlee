from datetime import datetime, timedelta, timezone
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, func, select
from sqlalchemy.orm import Session
from sqlalchemy.pool import StaticPool

from app.db.base import Base
from app.db.session import get_db
from app.main import app
from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, KnowledgeGraph
from app.models.foundation import FoundationEntity, FoundationEvidence, FoundationFact, FoundationFactReview
from app.models.market_data import DataSource, StockSymbol
from app.services import foundation as service
from app.services.resource_hub import build_knowledge_graph


@pytest.fixture
def api():
    engine = create_engine("sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool)
    Base.metadata.create_all(engine)
    with Session(engine) as db:
        source = DataSource(source_code="FOUNDATION_TEST", source_name="Test", adapter_type="MOCK")
        db.add(source)
        db.flush()
        stocks = [StockSymbol(market=market, symbol="000001", name=f"Demo {market}", exchange=market, source_id=source.id)
                  for market in ("CN_A", "HK")]
        db.add_all(stocks)
        db.commit()
        app.dependency_overrides[get_db] = lambda: db
        client = TestClient(app)
        try:
            yield client, db, stocks
        finally:
            client.close()
            app.dependency_overrides.pop(get_db, None)
    engine.dispose()


def post(client, path, body, status=201):
    response = client.post("/api/v1/foundation" + path, json=body)
    assert response.status_code == status, response.text
    return response.json()


def entity(client, name="Demo Company", **changes):
    return post(client, "/entities", {"name": name, "entity_type": "COMPANY", "jurisdiction": "CN", **changes})


def evidence(client, **changes):
    return post(client, "/evidence", {"source_name": "Demo filing", "source_key": "filing-1", "title": "Example evidence",
        "content": "  Original evidence text.\n\n", "published_at": "2025-01-01T00:00:00Z", **changes})


def fact(client, subject, obj, evidence_id, **changes):
    return post(client, "/facts", {"fact_type": "HOLDS_EQUITY", "title": "Demo holding", "subject_entity_id": subject,
        "object_entity_id": obj, "properties_json": {"ratio": 30, "ratio_basis": "TOTAL_ISSUED_SHARES"},
        "evidence_ids": [evidence_id], **changes})


def review(client, fact_id, decision="ACCEPTED", expected="PENDING", status=200):
    return post(client, f"/facts/{fact_id}/review", {"decision": decision, "expected_status": expected,
        "reason": "Verified against supplied document", "reviewer": "local-operator"}, status=status)


def test_identity_same_names_identifier_conflict_and_cross_market_mapping(api):
    client, db, stocks = api
    first = entity(client, identifier_scheme="REGISTRATION", identifier_value="unique-1")
    second = entity(client)
    assert first["id"] != second["id"]
    post(client, "/entities", {"name": "Different name", "entity_type": "COMPANY", "jurisdiction": "cn",
        "identifier_scheme": "registration", "identifier_value": "unique-1"}, status=409)
    proof = evidence(client)
    mappings = []
    for company, stock in zip([first, second], stocks):
        payload = {"entity_id": company["id"], "stock_symbol_id": stock.id, "evidence_id": proof["id"]}
        result = post(client, "/security-mappings", payload)
        assert post(client, "/security-mappings", payload)["id"] == result["id"]
        mappings.append(result)
    assert mappings[0]["security_id"] != mappings[1]["security_id"]
    for company, stock in zip([first, second], stocks):
        response = client.get("/api/v1/foundation/stock-context", params={"market": stock.market, "symbol": stock.symbol}).json()
        assert response["entity"]["id"] == company["id"]
    post(client, "/security-mappings", {"entity_id": second["id"], "stock_symbol_id": stocks[0].id,
        "evidence_id": proof["id"]}, status=409)
    person = entity(client, entity_type="PERSON")
    post(client, "/security-mappings", {"entity_id": person["id"], "stock_symbol_id": stocks[0].id,
        "evidence_id": proof["id"]}, status=422)
    assert client.get(f"/api/v1/foundation/entities/{first['id']}").json()["listings"][0]["market"] == "CN_A"
    missing = client.get("/api/v1/foundation/stock-context?market=HK&symbol=unknown").json()
    assert missing["mapping"] is None and missing["missing_data"] == ["COMPANY_MAPPING_MISSING"]


def test_evidence_verbatim_idempotency_and_revision(api):
    client, db, _ = api
    original = evidence(client)
    repeated = evidence(client)
    assert original["content"] == "  Original evidence text.\n\n"
    assert repeated["id"] == original["id"]
    revised = evidence(client, content="Updated disclosure", title="Corrected disclosure")
    assert revised["id"] != original["id"] and revised["version"] == 2
    old = client.get(f"/api/v1/foundation/evidence/{original['id']}").json()
    assert old["content"] == original["content"]
    assert old["metadata_json"]["availability_basis"] == "RECORDED_AT"
    assert old["available_at"] == old["created_at"]
    assert old["created_at"].endswith("+00:00") or old["created_at"].endswith("Z")
    listed = client.get("/api/v1/foundation/evidence").json()
    assert listed["total"] == 2 and "content" not in listed["items"][0]
    assert client.put(f"/api/v1/foundation/evidence/{original['id']}", json={"content": "overwrite"}).status_code == 405


def test_candidate_default_visibility_review_and_conflict(api):
    client, db, _ = api
    a, b = entity(client), entity(client, name="Customer")
    proof = evidence(client)
    item = fact(client, a["id"], b["id"], proof["id"])
    assert item["status"] == "PENDING"
    assert client.get("/api/v1/foundation/facts").json()["total"] == 0
    assert client.get("/api/v1/foundation/facts?status=PENDING").json()["total"] == 1
    accepted = review(client, item["id"])
    assert accepted["status"] == "ACCEPTED" and len(accepted["reviews"]) == 1
    review(client, item["id"], expected="PENDING", status=409)
    review(client, item["id"], expected="ACCEPTED", status=409)
    rejected = review(client, item["id"], decision="REJECTED", expected="ACCEPTED")
    assert rejected["status"] == "REJECTED" and len(rejected["reviews"]) == 2
    review(client, item["id"], expected="REJECTED", status=409)
    assert client.get("/api/v1/foundation/facts").json()["total"] == 0


def test_business_time_and_known_time_replay_without_future_information(api):
    client, db, _ = api
    first_time = datetime(2025, 1, 1, tzinfo=timezone.utc)
    with patch.object(service, "utc_now", return_value=first_time):
        a, b = entity(client), entity(client, name="Customer")
        proof = evidence(client)
        item = fact(client, a["id"], b["id"], proof["id"], valid_from="2025-01-01", valid_to="2025-03-01")
    with patch.object(service, "utc_now", return_value=first_time + timedelta(days=1)):
        review(client, item["id"])
    with patch.object(service, "utc_now", return_value=first_time + timedelta(days=3)):
        review(client, item["id"], decision="REJECTED", expected="ACCEPTED")
    base = "/api/v1/foundation/facts"
    assert client.get(base, params={"known_at": "2025-01-01T12:00:00Z"}).json()["total"] == 0
    past = client.get(base, params={"known_at": "2025-01-03T00:00:00Z", "as_of": "2025-02-28"}).json()
    assert past["total"] == 1 and past["items"][0]["status"] == "ACCEPTED"
    assert client.get(base, params={"known_at": "2025-01-03T00:00:00Z", "as_of": "2025-03-01"}).json()["total"] == 0
    detail = client.get(f"{base}/{item['id']}", params={"known_at": "2025-01-03T00:00:00Z"}).json()
    assert len(detail["reviews"]) == 1 and detail["status"] == "ACCEPTED"
    assert client.get(f"{base}/{item['id']}", params={"known_at": "2024-12-31T00:00:00Z"}).status_code == 404
    assert client.get(base, params={"known_at": "2025-01-03T00:00:00"}).status_code == 422
    assert client.get(base, params={"known_at": "2099-01-03T00:00:00Z"}).status_code == 422


def test_evidence_and_entity_recorded_times_limit_known_at(api):
    client, db, _ = api
    a, b = entity(client), entity(client, name="Another")
    proof = evidence(client, available_at="2025-01-01T00:00:00Z")
    item = fact(client, a["id"], b["id"], proof["id"])
    review(client, item["id"])
    assert client.get("/api/v1/foundation/facts?known_at=2025-01-02T00:00:00Z&status=ALL").json()["total"] == 0
    assert client.get("/api/v1/foundation/facts").json()["total"] == 1


def test_review_order_is_stable_when_wall_clock_does_not_advance(api):
    client, db, _ = api
    fixed = datetime(2025, 1, 2, tzinfo=timezone.utc)
    with patch.object(service, "utc_now", return_value=fixed):
        a, b, proof = entity(client), entity(client, name="Other"), evidence(client)
        item = fact(client, a["id"], b["id"], proof["id"])
        accepted = review(client, item["id"])
        rejected = review(client, item["id"], decision="REJECTED", expected="ACCEPTED")
    assert accepted["reviews"][0]["created_at"] < rejected["reviews"][1]["created_at"]
    known = accepted["reviews"][0]["created_at"]
    past = client.get(f"/api/v1/foundation/facts/{item['id']}", params={"known_at": known}).json()
    assert past["status"] == "ACCEPTED" and len(past["reviews"]) == 1


@pytest.mark.parametrize("properties", [{"ratio": 10}, {"ratio": 101, "ratio_basis": "TOTAL"},
                                         {"ratio": -1, "ratio_basis": "TOTAL"}, {"shares": 10}])
def test_equity_requires_measurement_basis(api, properties):
    client, _, _ = api
    a, b, proof = entity(client), entity(client, name="Other"), evidence(client)
    post(client, "/facts", {"fact_type": "HOLDS_EQUITY", "title": "Invalid holding", "subject_entity_id": a["id"],
        "object_entity_id": b["id"], "properties_json": properties, "evidence_ids": [proof["id"]]}, status=422)


def test_legal_procedure_is_distinct_from_review_and_amounts_have_scope(api):
    client, _, _ = api
    company, proof = entity(client), evidence(client)
    properties = {"case_number": "TEST-2025-001", "court": "Demo court", "jurisdiction": "CN", "procedure_type": "CIVIL",
                  "party_role": "DEFENDANT", "case_status": "PENDING", "amount": 100, "currency": "CNY", "amount_type": "CLAIMED"}
    item = fact(client, company["id"], None, proof["id"], fact_type="LEGAL_CASE", title="Demo pending case", properties_json=properties)
    accepted = review(client, item["id"])
    assert accepted["properties_json"]["case_status"] == "PENDING"
    assert accepted["properties_json"]["amount_type"] == "CLAIMED"
    bad = dict(properties)
    bad.pop("amount_type")
    post(client, "/facts", {"fact_type": "LEGAL_CASE", "title": "No amount scope", "subject_entity_id": company["id"],
        "properties_json": bad, "evidence_ids": [proof["id"]]}, status=422)
    assert "is_dishonest" not in accepted["properties_json"]


def test_missing_references_dates_and_pagination_rejected(api):
    client, _, stocks = api
    company, proof = entity(client), evidence(client)
    post(client, "/security-mappings", {"entity_id": company["id"], "stock_symbol_id": stocks[0].id,
        "evidence_id": "missing"}, status=404)
    post(client, "/evidence", {"entity_id": "missing", "source_name": "Demo", "source_key": "x",
        "title": "Test", "content": "text"}, status=404)
    post(client, "/facts", {"fact_type": "HOLDS_EQUITY", "title": "Missing object", "subject_entity_id": company["id"],
        "object_entity_id": "missing", "properties_json": {"ratio": 1, "ratio_basis": "TOTAL"}, "evidence_ids": [proof["id"]]}, status=404)
    other = entity(client, name="Other")
    post(client, "/facts", {"fact_type": "HOLDS_EQUITY", "title": "Empty interval", "subject_entity_id": company["id"],
        "object_entity_id": other["id"], "properties_json": {"ratio": 1, "ratio_basis": "TOTAL"}, "evidence_ids": [proof["id"]],
        "valid_from": "2025-01-01", "valid_to": "2025-01-01"}, status=422)
    post(client, "/facts", {"fact_type": "HOLDS_EQUITY", "title": "Missing proof", "subject_entity_id": company["id"],
        "object_entity_id": other["id"], "properties_json": {"ratio": 1, "ratio_basis": "TOTAL"},
        "evidence_ids": ["missing"]}, status=404)
    assert client.get("/api/v1/foundation/entities?limit=1000").status_code == 422
    assert client.get("/api/v1/foundation/facts?status=INVALID").status_code == 422
    for timestamp in ("2099-01-01T00:00:00Z", "2025-01-01T00:00:00"):
        post(client, "/evidence", {"source_name": "Demo", "source_key": "x", "title": "Test", "content": "text",
             "available_at": timestamp}, status=422)


def test_graph_rebuild_does_not_delete_imported_evidence(api):
    client, db, stocks = api
    kb = KnowledgeBase(kb_code="FOUNDATION_TEST", kb_name="Test", source_tables=["stock_symbol"])
    db.add(kb)
    db.flush()
    graph = KnowledgeGraph(knowledge_base_id=kb.id, graph_code="FOUNDATION_TEST", graph_name="Test", source_tables=["stock_symbol"])
    db.add(graph)
    db.flush()
    legacy = KnowledgeDocument(knowledge_base_id=kb.id, graph_id=graph.id, source_table="stock_symbol",
        source_record_id=stocks[0].id, market="CN_A", symbol=stocks[0].symbol, title="Legacy excerpt", content="Partial original text",
        metadata_json={"content_truncated": True, "original_length": 5000})
    db.add(legacy)
    db.commit()
    saved = post(client, "/evidence/import-legacy", {"document_id": legacy.id})
    assert saved["metadata_json"]["content_truncated"] is True
    assert saved["metadata_json"]["content_scope"] == "LEGACY_KNOWLEDGE_DOCUMENT"
    db.expunge(legacy)
    build_knowledge_graph(db, graph)
    db.commit()
    assert client.get(f"/api/v1/foundation/evidence/{saved['id']}").json()["content"] == "Partial original text"
    assert db.scalar(select(func.count()).select_from(FoundationEvidence)) == 1


def test_same_legacy_source_in_different_graphs_is_one_evidence_version(api):
    client, db, stocks = api
    kb = KnowledgeBase(kb_code="FOUNDATION_COPY", kb_name="Test", source_tables=["stock_symbol"])
    db.add(kb)
    db.flush()
    documents = []
    for index in range(2):
        graph = KnowledgeGraph(knowledge_base_id=kb.id, graph_code=f"COPY_{index}", graph_name="Copy", source_tables=["stock_symbol"])
        db.add(graph)
        db.flush()
        doc = KnowledgeDocument(knowledge_base_id=kb.id, graph_id=graph.id, source_table="stock_symbol",
            source_record_id=stocks[0].id, market="CN_A", symbol=stocks[0].symbol,
            title="Same source", content="Identical source text", metadata_json={"content_truncated": False})
        db.add(doc)
        db.flush()
        documents.append(doc)
    db.commit()
    first = post(client, "/evidence/import-legacy", {"document_id": documents[0].id})
    second = post(client, "/evidence/import-legacy", {"document_id": documents[1].id})
    assert first["id"] == second["id"] and second["version"] == 1


def test_disabled_status_does_not_query_new_tables_and_api_stops(api):
    client, db, _ = api
    with patch("app.api.foundation.get_settings") as settings:
        settings.return_value.data_foundation_enabled = False
        with patch.object(db, "scalar", side_effect=AssertionError("disabled status queried DB")):
            response = client.get("/api/v1/foundation/status")
            assert response.status_code == 200 and response.json()["enabled"] is False
        assert client.get("/api/v1/foundation/entities").status_code == 503
