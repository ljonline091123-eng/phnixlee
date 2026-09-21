from datetime import datetime, timezone

import pytest
from sqlalchemy import create_engine, event, select
from sqlalchemy.orm import Session

from app.api.resource_hub import explore_knowledge_graph
from app.db.base import Base
from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, KnowledgeEntity, KnowledgeGraph, KnowledgeRelation
from app.models.foundation import FoundationEntity, FoundationEvidence, FoundationListing, FoundationSecurity
from app.models.market_data import DataSource, StockSymbol
from app.services.graph_identity import (
    IDENTITY_VERSION, backfill_legacy_identity, canonical_company_id,
    canonical_security_id, enrich_legacy_nodes, resolve_many,
)
from app.services.resource_hub import build_knowledge_graph


EARLY = datetime(2026, 1, 1, tzinfo=timezone.utc)
MAPPED_AT = datetime(2026, 2, 1, tzinfo=timezone.utc)


@pytest.fixture
def context():
    engine = create_engine("sqlite://")
    Base.metadata.create_all(engine)
    with Session(engine) as db:
        source = DataSource(source_code="IDENTITY_TEST", source_name="Fixture", adapter_type="MOCK")
        db.add(source)
        db.flush()
        stocks = [StockSymbol(market=market, symbol=symbol, name=name, source_id=source.id,
            exchange=market, created_at=EARLY) for market, symbol, name in [
                ("CN_A", "000001", "Issuer A"), ("HK", "000001", "Issuer A H"),
                ("NEEQ", "832327", "NEEQ Issuer"), ("NEEQ_INNOVATION", "832327", "NEEQ Issuer"),
            ]]
        db.add_all(stocks)
        kb = KnowledgeBase(kb_code="IDENTITY_TEST", kb_name="Original knowledge base", source_tables=["stock_symbol"])
        db.add(kb)
        db.flush()
        graph = KnowledgeGraph(knowledge_base_id=kb.id, graph_code="IDENTITY_TEST", graph_name="Original graph",
            source_tables=["stock_symbol"])
        db.add(graph)
        db.flush()
        node = KnowledgeEntity(knowledge_base_id=kb.id, graph_id=graph.id, entity_type="STOCK",
            entity_key=f"{graph.id}:CN_A:000001", entity_name="Old security label",
            properties_json={"market": "CN_A", "symbol": "000001", "existing_annotation": "Keep this"})
        document = KnowledgeDocument(knowledge_base_id=kb.id, graph_id=graph.id, source_table="stock_symbol",
            source_record_id=stocks[0].id, market="CN_A", symbol="000001", title="Original source", content="Original evidence")
        db.add_all([node, document])
        db.flush()
        other = KnowledgeEntity(knowledge_base_id=kb.id, graph_id=graph.id, entity_type="COMPANY_PROFILE",
            entity_key=f"{graph.id}:stock_symbol:{stocks[0].id}:000001", entity_name="Profile document",
            properties_json={"document_id": document.id})
        db.add(other)
        db.flush()
        relation = KnowledgeRelation(knowledge_base_id=kb.id, graph_id=graph.id,
            subject_entity_id=node.id, object_entity_id=other.id, predicate="DESCRIBED_BY", evidence_document_id=document.id)
        db.add(relation)
        db.commit()
        yield db, stocks, graph, node, document, relation
    engine.dispose()


def map_stocks(db, stocks):
    company = FoundationEntity(id="11111111-1111-1111-1111-111111111111", name="Verified issuer",
        entity_type="COMPANY", jurisdiction="CN", created_at=EARLY)
    evidence = FoundationEvidence(entity_id=company.id, source_name="Fixture", source_key="issuer",
        title="Issuer mapping", content="Original identified issuer", available_at=EARLY,
        content_hash="a" * 64, fingerprint="b" * 64, version=1, created_at=EARLY)
    db.add_all([company, evidence])
    db.flush()
    listings = []
    for stock in stocks:
        security = FoundationSecurity(entity_id=company.id, share_class="A" if stock.market == "CN_A" else "H",
            created_at=MAPPED_AT)
        db.add(security)
        db.flush()
        listing = FoundationListing(security_id=security.id, stock_symbol_id=stock.id,
            market=stock.market, symbol=stock.symbol, name=stock.name, evidence_id=evidence.id, created_at=MAPPED_AT)
        db.add(listing)
        listings.append(listing)
    db.commit()
    return company, listings, evidence


def test_identity_stays_stable_as_mapping_arrives_and_keeps_markets_distinct(context):
    db, stocks, *_ = context
    pairs = [(stock.market, stock.symbol) for stock in stocks]
    before = resolve_many(db, pairs)
    company, listings, _ = map_stocks(db, stocks[:2])
    after = resolve_many(db, pairs)
    for pair in pairs:
        assert before[pair]["canonical_security_id"] == after[pair]["canonical_security_id"]
    for stock, listing in zip(stocks[:2], listings):
        item = after[stock.market, stock.symbol]
        assert item["security_id"] == listing.security_id
        assert item["company_id"] == company.id
        assert item["canonical_company_id"] == canonical_company_id(company.id)
        assert item["stock_symbol_id"] == stock.id
        assert item["identity_version"] == IDENTITY_VERSION
    assert after[pairs[0]]["security_id"] != after[pairs[1]]["security_id"]
    assert after[pairs[2]]["canonical_security_id"] != after[pairs[3]]["canonical_security_id"]
    assert after[pairs[2]]["company_id"] is None
    assert canonical_security_id(" hk ", "00001") == "security:HK:00001"
    assert not db.dirty


def test_historical_read_does_not_leak_backfilled_future_mapping(context):
    db, stocks, _, node, *_ = context
    company, _, evidence = map_stocks(db, stocks[:1])
    backfill_legacy_identity(db, apply=True)
    db.commit()
    stored = dict(node.properties_json)
    historical = enrich_legacy_nodes(db, [node], known_at=EARLY)[0]
    assert historical["company_id"] is None
    assert historical["properties"]["canonical_company_id"] is None
    assert historical["canonical_security_id"] == "security:CN_A:000001"
    assert historical["mapping_status"] == "UNMAPPED"
    assert node.properties_json == stored
    evidence.available_at = datetime(2026, 4, 1, tzinfo=timezone.utc)
    db.commit()
    march = resolve_many(db, [("CN_A", "000001")], known_at=datetime(2026, 3, 1, tzinfo=timezone.utc))
    assert march["CN_A", "000001"]["company_id"] is None
    assert resolve_many(db, [("CN_A", "000001")])["CN_A", "000001"]["company_id"] == company.id


def test_backfill_is_additive_idempotent_and_explore_uses_live_identity(context):
    db, stocks, graph, node, document, relation = context
    original = (node.id, node.entity_key, node.entity_name, document.id, document.content,
        relation.id, relation.subject_entity_id, relation.object_entity_id, relation.evidence_document_id)
    preview = backfill_legacy_identity(db)
    assert preview["updated"] == 1
    assert "canonical_security_id" not in node.properties_json
    assert not db.dirty
    backfill_legacy_identity(db, apply=True)
    db.commit()
    # Mapping can arrive after the last backfill; read-time aliases must refresh.
    company, _, _ = map_stocks(db, stocks[:1])
    explored = explore_knowledge_graph(graph.id, q="000001", db=db)
    stock_node = next(item for item in explored["nodes"] if item["type"] == "STOCK")
    assert stock_node["id"] == node.id
    assert stock_node["company_id"] == company.id
    assert stock_node["canonical_company_id"] == canonical_company_id(company.id)
    assert node.properties_json["company_id"] is None
    assert next(item for item in explored["nodes"] if item["type"] == "COMPANY_PROFILE")["properties"] == {"document_id": document.id}
    assert explored["relations"][0]["evidence"]["document_id"] == document.id
    changed = backfill_legacy_identity(db, apply=True)
    db.commit()
    assert changed["updated"] == 1 and changed["mapped"] == 1
    assert backfill_legacy_identity(db, apply=True)["updated"] == 0
    assert node.properties_json["existing_annotation"] == "Keep this"
    assert original == (node.id, node.entity_key, node.entity_name, document.id, document.content,
        relation.id, relation.subject_entity_id, relation.object_entity_id, relation.evidence_document_id)


def test_new_build_has_same_identity_and_does_not_retype_profile_evidence(context):
    db, stocks, graph, *_ = context
    company, _, _ = map_stocks(db, stocks[:1])
    company_id = company.id
    # Isolate the builder from stale ORM objects that its pre-existing rebuild
    # semantics replace; identity migration itself never invokes the builder.
    graph_id = graph.id
    db.expunge_all()
    graph = db.get(KnowledgeGraph, graph_id)
    build_knowledge_graph(db, graph)
    db.commit()
    nodes = list(db.scalars(select(KnowledgeEntity).where(KnowledgeEntity.graph_id == graph.id)))
    stock_node = next(node for node in nodes if node.entity_type == "STOCK" and node.properties_json["market"] == "CN_A")
    assert stock_node.properties_json["canonical_security_id"] == "security:CN_A:000001"
    assert stock_node.properties_json["canonical_company_id"] == canonical_company_id(company_id)
    assert any(node.entity_type == "COMPANY_PROFILE" for node in nodes)


def test_legacy_database_without_foundation_tables_still_resolves_security():
    engine = create_engine("sqlite://")
    Base.metadata.create_all(engine, tables=[DataSource.__table__, StockSymbol.__table__])
    with Session(engine) as db:
        source = DataSource(source_code="OLD", source_name="Old source", adapter_type="MOCK")
        db.add(source)
        db.flush()
        stock = StockSymbol(market="CN_A", symbol="000001", name="Old stock", exchange="SZ", source_id=source.id)
        db.add(stock)
        db.flush()
        item = resolve_many(db, [("CN_A", "000001")])["CN_A", "000001"]
        assert item["stock_symbol_id"] == stock.id
        assert item["canonical_security_id"] == "security:CN_A:000001"
        assert item["company_id"] is None
    engine.dispose()


def test_legacy_key_only_resolves_but_conflicting_keys_never_choose_an_issuer(context):
    db, stocks, _, node, *_ = context
    company, _, _ = map_stocks(db, stocks[:1])
    node.properties_json = {"existing_annotation": "Key-only source"}
    db.commit()
    assert enrich_legacy_nodes(db, [node])[0]["company_id"] == company.id
    node.properties_json = {"market": "HK", "symbol": "000001",
        "canonical_company_id": canonical_company_id(company.id)}
    db.commit()
    item = enrich_legacy_nodes(db, [node])[0]
    assert item["canonical_security_id"] is None
    assert item["canonical_company_id"] is None
    assert item["properties"]["canonical_company_id"] is None
    assert item["mapping_status"] == "UNRESOLVED"
    assert backfill_legacy_identity(db, apply=True)["unresolved"] == 1


def test_resolution_batches_large_stock_sets_without_per_node_queries(context):
    db, *_ = context
    statements = []

    def capture(conn, cursor, statement, parameters, ctx, executemany):
        statements.append(statement)

    event.listen(db.bind, "before_cursor_execute", capture)
    try:
        pairs = [("CN_A", f"{index:06}") for index in range(650)]
        result = resolve_many(db, pairs)
        assert len(result) == 650
        selects = [statement for statement in statements if statement.lstrip().upper().startswith("SELECT")]
        # 3 chunks * 2 reads, plus one schema inspection.
        assert len(selects) <= 7
    finally:
        event.remove(db.bind, "before_cursor_execute", capture)
