import json
from datetime import datetime, timezone

import pytest
from sqlalchemy import create_engine, delete, event, func, select, text
from sqlalchemy.orm import Session

import app.models  # noqa: F401
from app.db.base import Base
from app.models.ai_hub import AgentDataAsset, AgentDataAssetLink, AgentDefinition, GovernanceRun, KnowledgeBase, KnowledgeDocument, KnowledgeEntity, KnowledgeGraph, KnowledgeRelation, ModelCallLog, ModelSkill
from app.models.foundation import FoundationEntity, FoundationEvidence, FoundationFact, FoundationListing, FoundationSecurity
from app.models.market_data import DataSource, StockFinancialReport, StockSymbol
from app.schemas.knowledge_governance import KnowledgeGovernanceRequest
from app.services import knowledge_governance as governance


@pytest.fixture
def db(tmp_path):
    # Legacy adapters inspect a separate engine connection: mirror production's
    # file-backed SQLite instead of sharing one in-memory connection.
    engine = create_engine("sqlite:///" + (tmp_path / "governance.db").as_posix())
    Base.metadata.create_all(engine)
    with Session(engine, autoflush=False) as session:
        session.add(DataSource(source_code="TEST", source_name="Test fixture", adapter_type="MOCK"))
        session.flush()
        source = session.scalar(select(DataSource))
        session.add_all([StockSymbol(source_id=source.id, market="CN_A", symbol=symbol, name="样例公司", exchange="SZSE")
                         for symbol in ("000001", "000002", "000003")])
        session.add(StockFinancialReport(source_id=source.id, market="CN_A", symbol="000001", report_period="2025-12-31",
                                        indicator="利润表", data_json={"revenue": 100}))
        for name in governance.SOURCE_TABLES:
            session.add(AgentDataAsset(asset_code=name.upper(), table_name=name, display_name=name, enabled=True))
        legacy = KnowledgeBase(kb_code="STOCK_FULL_KG", kb_name="历史新闻", source_tables=["stock_news"])
        session.add(legacy)
        session.flush()
        graph = KnowledgeGraph(knowledge_base_id=legacy.id, graph_code="STOCK_FULL_KG", graph_name="历史新闻", source_tables=["stock_news"])
        session.add(graph)
        session.flush()
        session.add(KnowledgeDocument(knowledge_base_id=legacy.id, graph_id=graph.id, source_table="stock_news", title="保留新闻", content="原始证据"))
        session.commit()
        yield session
    engine.dispose()


def mock_model(monkeypatch, *, malformed=False, status="SUCCESS", provider_code="REAL_TEST_RESPONSE", response_mode="json", truncated=False, finish_reason="stop"):
    seen = []

    def chat(self, **kwargs):
        seen.append(kwargs)
        evidence = json.loads(kwargs["messages"][-1]["content"])
        review = {"summary": "仅审核提供的来源样本，空源仍需补齐。", "sources": [
            {"source_table": source["source_table"],
             "decision": "READY" if source["status"] == "AVAILABLE" else "INSUFFICIENT_DATA",
             "issues": [] if source["status"] == "AVAILABLE" else ["没有可审核记录"],
             "evidence_refs": [item["evidence_ref"] for item in source.get("samples", [])]}
            for source in evidence["sources"]]}
        if malformed:
            review["sources"][0]["evidence_refs"] = ["invented#99"]
        if response_mode == "invalid_shape":
            review["unknown_field"] = "not allowed"
        response = json.dumps(review)
        if response_mode != "json":
            response = "```json\n" + response + "\n```"
        if response_mode == "mixed_prefix":
            response = "这里是分析结果：\n" + response
        elif response_mode == "mixed_suffix":
            response += "\n以上是结果。"
        log = ModelCallLog(task_type="knowledge_graph", provider_code=provider_code, instance_code="TEST_RESPONSE",
            status=status, response_text="" if truncated else response,
            response_json={"choices": [{"finish_reason": "length" if truncated else finish_reason}]},
            error_message="fixture model failed" if status != "SUCCESS" else None,
            request_json=kwargs)
        self.db.add(log)
        self.db.commit()
        return log

    monkeypatch.setattr(governance.ModelHubService, "chat", chat)
    return seen


def test_agent_loads_skill_and_materializes_nonnews_without_replacing_legacy(db, monkeypatch):
    seen = mock_model(monkeypatch)
    run = governance.govern_knowledge_sources(db, KnowledgeGovernanceRequest(record_limit_per_source=2, run_key="FIRST"))
    assert run.status == "PENDING_REVIEW", run.summary_json.get("error")
    assert len(seen) == 1
    assert "证券主数据检查市场+代码" in seen[0]["messages"][0]["content"]
    assert run.model_call_log_id is not None, run.summary_json.get("error")
    assert len(run.summary_json["created_knowledge_bases"]) == 6
    sources = {item["source_table"]: item for item in run.summary_json["source_coverage"]}
    assert sources["stock_symbol"]["total_records"] == 3
    assert sources["stock_symbol"]["documents_created"] == 2
    assert sources["stock_symbol"]["status"] == "SAMPLED"
    assert sources["stock_financial_report"]["documents_created"] == 1
    assert sources["stock_notice"]["status"] == "EMPTY"
    legacy = db.scalar(select(KnowledgeBase).where(KnowledgeBase.kb_code == "STOCK_FULL_KG"))
    assert legacy.source_tables == ["stock_news"]
    assert db.scalar(select(KnowledgeDocument).where(KnowledgeDocument.knowledge_base_id == legacy.id)).content == "原始证据"
    assert run.summary_json["complete"] is False
    assert run.summary_json["skill_snapshot"]["content_hash"]
    repeated = governance.govern_knowledge_sources(db, KnowledgeGovernanceRequest(record_limit_per_source=2, run_key="FIRST"))
    assert repeated.id == run.id
    assert len(seen) == 1
    with pytest.raises(ValueError, match="不同参数"):
        governance.govern_knowledge_sources(db, KnowledgeGovernanceRequest(record_limit_per_source=3, run_key="FIRST"))


@pytest.mark.parametrize("options", [{"status": "FAILED"}, {"malformed": True}, {"provider_code": "MOCK"}, {"truncated": True}])
def test_failed_or_fabricated_or_mock_review_never_publishes_new_versions(db, monkeypatch, options):
    mock_model(monkeypatch, **options)
    run = governance.govern_knowledge_sources(db, KnowledgeGovernanceRequest(run_key="FAILURE"))
    assert run.status == "FAILED"
    assert run.summary_json["created_knowledge_bases"] == []
    assert run.model_call_log_id is not None, run.summary_json.get("error")
    assert db.scalar(select(func.count()).select_from(KnowledgeBase)) == 1
    assert db.scalar(select(func.count()).select_from(KnowledgeDocument)) == 1


def test_single_complete_json_fence_is_validated_and_raw_response_is_retained(db, monkeypatch):
    seen = mock_model(monkeypatch, response_mode="fenced")
    run = governance.govern_knowledge_sources(db, KnowledgeGovernanceRequest(run_key="FENCED"))
    assert run.status == "PENDING_REVIEW", run.summary_json.get("error")
    assert len(run.summary_json["created_knowledge_bases"]) == 6
    assert run.summary_json["response_format"] == "JSON_FENCED"
    assert seen[0]["metadata_json"]["defer_schema_validation"] is True
    assert seen[0]["metadata_json"]["json_schema_output"]
    assert db.get(ModelCallLog, run.model_call_log_id).response_text.startswith("```json\n")


def test_output_limit_with_nonempty_json_is_not_published_as_complete_review(db, monkeypatch):
    mock_model(monkeypatch, finish_reason="length")
    run = governance.govern_knowledge_sources(db, KnowledgeGovernanceRequest(run_key="OUTPUT_LIMIT"))
    assert run.status == "FAILED"
    assert run.summary_json["model_status"] == "OUTPUT_LIMIT_REACHED"
    assert run.summary_json["created_knowledge_bases"] == []
    assert db.scalar(select(func.count()).select_from(KnowledgeBase)) == 1
    assert db.get(ModelCallLog, run.model_call_log_id).response_text


@pytest.mark.parametrize("options", [
    {"response_mode": "mixed_prefix"}, {"response_mode": "mixed_suffix"},
    {"response_mode": "invalid_shape"}, {"response_mode": "fenced", "malformed": True},
])
def test_fence_does_not_allow_mixed_text_invalid_schema_or_fabricated_refs(db, monkeypatch, options):
    mock_model(monkeypatch, **options)
    run = governance.govern_knowledge_sources(db, KnowledgeGovernanceRequest(run_key="INVALID_FENCE"))
    assert run.status == "FAILED"
    assert run.summary_json["created_knowledge_bases"] == []
    assert db.scalar(select(func.count()).select_from(KnowledgeBase)) == 1
    assert db.scalar(select(func.count()).select_from(KnowledgeDocument)) == 1
    assert db.get(ModelCallLog, run.model_call_log_id).response_text


def test_disabled_skill_blocks_execution_and_other_running_batch_blocks_start(db, monkeypatch):
    seen = mock_model(monkeypatch)
    db.add(ModelSkill(skill_code=governance.SKILL_CODE, skill_name="用户已禁用", instructions="disabled", enabled=False))
    db.commit()
    run = governance.govern_knowledge_sources(db, KnowledgeGovernanceRequest(run_key="DISABLED"))
    assert run.status == "FAILED"
    assert not seen
    db.add(GovernanceRun(target_type=governance.TARGET_TYPE, target_id=0, status="RUNNING", summary_json={"run_key": "OTHER"}))
    db.commit()
    with pytest.raises(ValueError, match="正在运行"):
        governance.govern_knowledge_sources(db, KnowledgeGovernanceRequest(run_key="ANOTHER"))


def test_restricted_asset_is_not_read_into_prompt_or_materialized(db, monkeypatch):
    seen = mock_model(monkeypatch)
    asset = db.scalar(select(AgentDataAsset).where(AgentDataAsset.table_name == "stock_financial_report"))
    asset.allowed_columns = ["id"]
    db.commit()
    run = governance.govern_knowledge_sources(db, KnowledgeGovernanceRequest(run_key="RESTRICTED"))
    assert run.status == "PENDING_REVIEW", run.summary_json.get("error")
    source = next(item for item in run.summary_json["source_coverage"] if item["source_table"] == "stock_financial_report")
    assert source["status"] == "RESTRICTED_COLUMNS"
    assert source["row_count_known"] is True and source["total_records"] == 1
    assert source["documents_created"] == 0
    prompt_source = next(item for item in json.loads(seen[0]["messages"][-1]["content"])["sources"] if item["source_table"] == "stock_financial_report")
    assert "samples" not in prompt_source


def test_builder_failure_rolls_back_all_new_domains(db, monkeypatch):
    mock_model(monkeypatch)
    original = governance.build_knowledge_graph
    calls = []

    def fail_second(*args, **kwargs):
        calls.append(1)
        if len(calls) == 2:
            raise RuntimeError("fixture second domain failed")
        return original(*args, **kwargs)

    monkeypatch.setattr(governance, "build_knowledge_graph", fail_second)
    run = governance.govern_knowledge_sources(db, KnowledgeGovernanceRequest(run_key="ROLLBACK"))
    assert run.status == "FAILED"
    assert db.scalar(select(func.count()).select_from(KnowledgeBase)) == 1
    assert db.scalar(select(func.count()).select_from(KnowledgeDocument)) == 1
    assert all(item["documents_created"] == 0 for item in run.summary_json["source_coverage"])


def test_financial_adapter_uses_authorized_columns_without_reading_optional_url(db, monkeypatch):
    seen = mock_model(monkeypatch)
    asset = db.scalar(select(AgentDataAsset).where(AgentDataAsset.table_name == "stock_financial_report"))
    asset.allowed_columns = [column.name for column in StockFinancialReport.__table__.columns if column.name != "url"]
    db.commit()
    statements = []
    event.listen(db.get_bind(), "before_cursor_execute", lambda connection, cursor, statement, *args: statements.append(statement))
    run = governance.govern_knowledge_sources(db, KnowledgeGovernanceRequest(run_key="PERMITTED_FINANCE"))
    assert run.status == "PENDING_REVIEW", run.summary_json.get("error")
    source = next(item for item in run.summary_json["source_coverage"] if item["source_table"] == "stock_financial_report")
    assert source["documents_created"] == 1
    assert source["status"] == "MATERIALIZED"
    prompt = json.loads(seen[0]["messages"][-1]["content"])
    sample = next(item for item in prompt["sources"] if item["source_table"] == "stock_financial_report")
    assert "url" not in sample["samples"][0]["record"]
    selected = [statement for statement in statements if "FROM stock_financial_report" in statement]
    assert selected and all("stock_financial_report.url" not in statement for statement in selected)


def add_foundation(db):
    company = FoundationEntity(name="发行主体", entity_type="COMPANY", jurisdiction="CN")
    other = FoundationEntity(name="合作主体", entity_type="COMPANY", jurisdiction="CN")
    db.add_all([company, other])
    db.flush()
    evidence = FoundationEvidence(entity_id=company.id, source_name="FIXTURE", source_key="identity", title="主体证据",
        content="发行主体资料", available_at=datetime.now(timezone.utc), content_hash="fixture", fingerprint="fixture", version=1)
    security = FoundationSecurity(entity_id=company.id, share_class="A_SHARE")
    db.add_all([evidence, security])
    db.flush()
    stock = db.scalar(select(StockSymbol).where(StockSymbol.symbol == "000001"))
    db.add(FoundationListing(security_id=security.id, stock_symbol_id=stock.id, market=stock.market, symbol=stock.symbol,
                             name=stock.name, evidence_id=evidence.id))
    fact = FoundationFact(fact_type="SUPPLIES_TO", title="待审供应商线索", subject_entity_id=company.id, object_entity_id=other.id, status="PENDING")
    db.add(fact)
    db.commit()
    return company.id, fact.id


def test_company_identity_and_fact_state_are_preserved_with_evidence(db, monkeypatch):
    company_id, fact_id = add_foundation(db)
    mock_model(monkeypatch)
    run = governance.govern_knowledge_sources(db, KnowledgeGovernanceRequest(run_key="COMPANIES"))
    assert run.status == "PENDING_REVIEW", run.summary_json.get("error")
    company_nodes = list(db.scalars(select(KnowledgeEntity).where(KnowledgeEntity.entity_type == "COMPANY")))
    assert any(node.properties_json["canonical_company_id"] == "company:" + company_id for node in company_nodes)
    relations = list(db.scalars(select(KnowledgeRelation).where(KnowledgeRelation.predicate == "ISSUED_BY")))
    assert relations and all(relation.evidence_document_id for relation in relations)
    fact_node = db.scalar(select(KnowledgeEntity).where(KnowledgeEntity.entity_type == "RELATION_FACT"))
    assert fact_node.properties_json["verification_status"] == "PENDING"
    assert fact_node.properties_json["model_verified"] is False
    assert db.get(FoundationFact, fact_id).status == "PENDING"


def test_unbound_asset_is_not_regranted_and_identity_enrichment_cannot_bypass_it(db, monkeypatch):
    add_foundation(db)
    seen = mock_model(monkeypatch)
    agent, _ = governance._agent_skill(db)
    db.commit()
    asset = db.scalar(select(AgentDataAsset).where(AgentDataAsset.table_name == "foundation_listing"))
    db.execute(delete(AgentDataAssetLink).where(AgentDataAssetLink.agent_id == agent.id, AgentDataAssetLink.data_asset_id == asset.id))
    db.commit()
    run = governance.govern_knowledge_sources(db, KnowledgeGovernanceRequest(run_key="UNBOUND"))
    assert run.status == "PENDING_REVIEW", run.summary_json.get("error")
    source = next(item for item in run.summary_json["source_coverage"] if item["source_table"] == "foundation_listing")
    assert source["status"] == "UNBOUND_ASSET"
    assert not db.scalar(select(AgentDataAssetLink.id).where(AgentDataAssetLink.agent_id == agent.id, AgentDataAssetLink.data_asset_id == asset.id))
    stock_nodes = list(db.scalars(select(KnowledgeEntity).where(KnowledgeEntity.entity_type == "STOCK")))
    assert stock_nodes and all(node.properties_json.get("canonical_company_id") is None for node in stock_nodes)
    assert not db.scalar(select(KnowledgeRelation.id).where(KnowledgeRelation.predicate == "ISSUED_BY"))


def test_low_cache_size_atomic_domains_use_session_connection_for_schema_reads(db, monkeypatch):
    """A writer spill must not be blocked by a second pooled SQLite connection."""
    from app.services import graph_identity, resource_hub
    original_inspect = resource_hub.inspect

    def guarded_inspect(bind):
        # build_knowledge_graph's generic source adapter must inspect the active
        # transaction connection. An Engine inspection here would recreate the
        # database-locked failure observed during a real multi-domain publish.
        from sqlalchemy.engine import Engine
        if isinstance(bind, Engine):
            raise AssertionError("graph builder opened a second Engine schema connection")
        return original_inspect(bind)

    monkeypatch.setattr(resource_hub, "inspect", guarded_inspect)
    monkeypatch.setattr(graph_identity, "inspect", guarded_inspect)
    monkeypatch.setattr(governance, "inspect", guarded_inspect)
    add_foundation(db)
    mock_model(monkeypatch)
    db.execute(text("PRAGMA cache_size = 1"))
    run = governance.govern_knowledge_sources(db, KnowledgeGovernanceRequest(record_limit_per_source=2, run_key="LOW_CACHE"))
    assert run.status == "PENDING_REVIEW", run.summary_json.get("error")
    assert len(run.summary_json["created_knowledge_bases"]) == 6
    assert all(item["documents_created"] >= 0 for item in run.summary_json["source_coverage"])
    assert db.scalar(select(func.count()).select_from(KnowledgeBase)) == 7


def test_listing_without_security_permission_remains_source_document_without_issuer_edge(db, monkeypatch):
    add_foundation(db)
    mock_model(monkeypatch)
    agent, _ = governance._agent_skill(db)
    db.commit()
    security = db.scalar(select(AgentDataAsset).where(AgentDataAsset.table_name == "foundation_security"))
    db.execute(delete(AgentDataAssetLink).where(AgentDataAssetLink.agent_id == agent.id, AgentDataAssetLink.data_asset_id == security.id))
    db.commit()
    run = governance.govern_knowledge_sources(db, KnowledgeGovernanceRequest(run_key="NO_SECURITY"))
    assert run.status == "PENDING_REVIEW", run.summary_json.get("error")
    listing = next(item for item in run.summary_json["source_coverage"] if item["source_table"] == "foundation_listing")
    assert listing["documents_created"] == 1
    assert not db.scalar(select(KnowledgeRelation.id).where(KnowledgeRelation.predicate == "ISSUED_BY"))
