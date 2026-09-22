from datetime import datetime, timezone

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, func, select
from sqlalchemy.orm import Session
from sqlalchemy.pool import StaticPool

from app.api.knowledge_network import router
from app.db.base import Base
from app.db.session import get_db
from app.models.company_graph import SecurityClassification
from app.models.foundation import FoundationEntity, FoundationEvidence, FoundationListing, FoundationSecurity
from app.models.market_data import DataSource, StockF10Cache, StockFinancialReport, StockNews, StockNotice, StockSymbol
from app.services.knowledge_network import explore


@pytest.fixture
def context():
    engine = create_engine("sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool)
    Base.metadata.create_all(engine)
    with Session(engine) as db:
        source = DataSource(source_code="CATEGORY_TEST", source_name="测试来源", adapter_type="MOCK")
        company = FoundationEntity(name="测试发行公司", entity_type="COMPANY", jurisdiction="CN")
        proof = FoundationEvidence(source_name="TEST", source_key="issuer", title="发行资料", content="原始发行公司资料",
            available_at=datetime.now(timezone.utc), content_hash="a" * 64, fingerprint="a" * 64, version=1)
        db.add_all([source, company, proof])
        db.flush()
        stock = StockSymbol(market="CN_A", symbol="000001", name="测试股票", exchange="SZSE", source_id=source.id)
        security = FoundationSecurity(entity_id=company.id, share_class="A")
        db.add_all([stock, security])
        db.flush()
        db.add(FoundationListing(security_id=security.id, stock_symbol_id=stock.id, market=stock.market,
            symbol=stock.symbol, name=stock.name, evidence_id=proof.id))
        for index in range(45):
            db.add(SecurityClassification(security_id=security.id, dimension="THEME", code=f"T{index}", label=f"主题{index}",
                method="SOURCE", definition_version="V1", status="ACCEPTED", evidence_id=proof.id, properties_json={}))
        fields = {"market": stock.market, "symbol": stock.symbol, "source_id": source.id}
        db.add(StockFinancialReport(**fields, indicator="报告期", report_period="2026-06-30", currency="CNY",
            data_json={"revenue": 1234, "net_profit": 567}))
        db.add_all([StockNews(**fields, title=f"公告提及诉讼{day}", content="尚待核实", news_time=f"2026-09-{day:02d}")
            for day in range(1, 8)])
        db.add(StockNotice(**fields, title="上市公司公告", notice_date="2026-09-10", content_json={"正文": "公告原文"}))
        db.add(StockF10Cache(**fields, section="profile", payload_json={"公司名称": company.name}))
        db.commit()
        yield db
    engine.dispose()


def test_domain_selected_before_node_budget_and_source_totals_remain_distinct(context):
    db = context
    baseline = explore(db, market="CN_A", symbol="000001", max_nodes=10, include_evidence=False)
    assert not any(node["type"] == "FINANCIAL_OBSERVATION" for node in baseline["nodes"])
    financial = explore(db, market="CN_A", symbol="000001", max_nodes=10, include_evidence=False, category="financial")
    assert sum(node["type"] == "FINANCIAL_OBSERVATION" for node in financial["nodes"]) == 2
    assert not any(node["type"] == "THEME" for node in financial["nodes"])
    assert {node["type"] for node in financial["nodes"]} == {"SECURITY", "COMPANY", "FINANCIAL_OBSERVATION"}
    coverage = {row["key"]: row["count"] for row in financial["coverage"]}
    assert coverage["theme"] == 45 and coverage["financial"] == 1 and coverage["news"] == 7
    assert financial["coverage_scope"]["financial"] == {
        "count_unit": "SOURCE_RECORDS", "count_scope": "SELECTED_SECURITIES", "sampled_records": 1}
    assert financial["coverage_scope"]["theme"]["count_scope"] == "BOUNDED_NEIGHBORHOOD"
    assert len(financial["category_node_ids"]) == 2


def test_disclosures_visible_without_auxiliary_evidence_and_preserve_real_edges(context):
    result = explore(context, market="CN_A", symbol="000001", max_nodes=10, include_evidence=False, category="disclosure", per_category=2)
    documents = [node for node in result["nodes"] if node["type"] == "EVIDENCE_DOCUMENT"]
    assert len(documents) == 3
    assert {node["properties"]["source_table"] for node in documents} == {"stock_news", "stock_notice"}
    assert any(node["type"] == "F10_SECTION" for node in result["nodes"])
    assert {edge["type"] for edge in result["edges"]} == {"ISSUED_BY", "HAS_NEWS", "HAS_NOTICE", "HAS_F10_SECTION"}
    assert not any(node["type"] in {"CANDIDATE_EVENT", "DATA_SOURCE", "THEME"} for node in result["nodes"])
    assert result["coverage_scope"]["news"]["sampled_records"] == 2
    assert result["truncated"]
    for document in documents:
        assert document["evidence_ids"]
        assert any(proof["id"] in document["evidence_ids"] for proof in result["evidence"])


def test_legal_mentions_remain_candidates_and_no_projection_writes(context):
    db = context
    count_before = db.scalar(select(func.count()).select_from(FoundationEvidence))
    hidden = explore(db, market="CN_A", symbol="000001", category="legal", include_evidence=False)
    assert not hidden["category_node_ids"]
    shown = explore(db, market="CN_A", symbol="000001", category="legal", include_candidates=True, include_evidence=False)
    events = [node for node in shown["nodes"] if node["type"] == "CANDIDATE_EVENT"]
    assert len(events) == 3 and all(node["status"] == "CANDIDATE" for node in events)
    assert all(edge["status"] == "CANDIDATE" for edge in shown["edges"] if edge["type"] == "HAS_EVENT_MENTION")
    assert not db.dirty and not db.new and not db.deleted
    assert db.scalar(select(func.count()).select_from(FoundationEvidence)) == count_before


def test_category_query_validated_and_forwarded(context):
    application = FastAPI()
    application.include_router(router)
    application.dependency_overrides[get_db] = lambda: context
    with TestClient(application) as client:
        invalid = client.get("/knowledge-network/explore", params={"category": "missing"})
        assert invalid.status_code == 422
        response = client.get("/knowledge-network/explore", params={"market": "CN_A", "symbol": "000001",
            "category": "financial", "max_nodes": 10, "include_evidence": False})
        assert response.status_code == 200, response.text
        assert sum(node["type"] == "FINANCIAL_OBSERVATION" for node in response.json()["nodes"]) == 2
