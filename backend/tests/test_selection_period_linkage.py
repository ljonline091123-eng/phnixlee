from datetime import datetime, timedelta, timezone
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import patch

import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import Session

from app.db.base import Base
from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, KnowledgeGraph, ModelSkill
from app.models.foundation import (
    FoundationEntity, FoundationEvidence, FoundationFact, FoundationFactReview,
    FoundationListing, FoundationSecurity,
)
from app.models.market_data import DataSource, StockSymbol
from app.schemas.selection import SelectionDimension, SelectionModelOutput
from app.services.graph_rag import build_selection_context, selection_period_bounds
from app.services.model_hub import ModelHubService, seed_default_models
from app.services.selection import _model_candidate_context, _validate_multi_source_output


def test_period_uses_natural_month_boundary_and_clamps_month_end():
    start, end = selection_period_bounds(datetime(2026, 5, 31, 12, tzinfo=timezone.utc), 3)
    assert start.isoformat() == "2026-02-28"
    assert end.isoformat() == "2026-05-31"
    local_start, local_end = selection_period_bounds(
        datetime(2026, 10, 24, 17, tzinfo=timezone.utc), 3, "CN_A",
    )
    assert local_start.isoformat() == "2026-07-25"
    assert local_end.isoformat() == "2026-10-25"


def test_related_security_context_requires_reviewed_company_fact():
    engine = create_engine("sqlite://")
    Base.metadata.create_all(engine)
    now = datetime.now(timezone.utc) + timedelta(days=1)
    with Session(engine) as db:
        source = DataSource(source_code="RELATED_TEST", source_name="关联测试", adapter_type="MOCK")
        base = KnowledgeBase(kb_code="RELATED_KB", kb_name="关联知识库")
        db.add_all([source, base])
        db.flush()
        graph = KnowledgeGraph(knowledge_base_id=base.id, graph_code="RELATED_GRAPH", graph_name="关联图谱")
        db.add(graph)
        db.flush()

        stocks = [
            StockSymbol(market="CN_A", symbol="000001", name="甲公司", exchange="SZSE", source_id=source.id, status="LISTED"),
            StockSymbol(market="CN_A", symbol="000002", name="乙公司", exchange="SZSE", source_id=source.id, status="LISTED"),
            StockSymbol(market="CN_A", symbol="000003", name="丙公司", exchange="SZSE", source_id=source.id, status="LISTED"),
        ]
        companies = [FoundationEntity(name=name, entity_type="COMPANY", jurisdiction="CN")
                     for name in ("甲公司", "乙公司", "丙公司")]
        db.add_all([*stocks, *companies])
        db.flush()
        for index, (stock, company) in enumerate(zip(stocks, companies), start=1):
            proof = FoundationEvidence(
                entity_id=company.id, source_name="MASTER", source_key=f"company:{index}",
                title=company.name, content=company.name, available_at=now,
                content_hash=f"hash-{index}", fingerprint=f"fingerprint-{index}", version=1,
            )
            security = FoundationSecurity(entity_id=company.id)
            db.add_all([proof, security])
            db.flush()
            db.add(FoundationListing(
                security_id=security.id, stock_symbol_id=stock.id, market=stock.market,
                symbol=stock.symbol, name=stock.name, evidence_id=proof.id,
            ))

        accepted = FoundationFact(
            fact_type="SUPPLIES_TO", title="甲向乙供货", subject_entity_id=companies[0].id,
            object_entity_id=companies[1].id, status="ACCEPTED",
        )
        pending = FoundationFact(
            fact_type="SUPPLIES_TO", title="甲向丙供货候选", subject_entity_id=companies[0].id,
            object_entity_id=companies[2].id, status="PENDING",
        )
        db.add_all([accepted, pending])
        db.flush()
        db.add(FoundationFactReview(
            fact_id=accepted.id, previous_status="PENDING", decision="ACCEPTED",
            reason="测试审核", reviewer="TEST", created_at=now,
        ))
        db.add_all([
            KnowledgeDocument(
                knowledge_base_id=base.id, graph_id=graph.id, source_table="stock_news",
                market="CN_A", symbol="000001", title="甲公司发布经营进展",
                content="甲公司发布经营进展，项目进入交付阶段。",
                metadata_json={"published_at": "2026-09-01", "source_name": "测试媒体"},
            ),
            KnowledgeDocument(
                knowledge_base_id=base.id, graph_id=graph.id, source_table="stock_news",
                market="CN_A", symbol="000002", title="乙公司确认收到首批产品",
                content="乙公司确认收到首批产品，后续验收仍在进行。",
                metadata_json={"published_at": "2026-09-20", "source_name": "测试媒体"},
            ),
            KnowledgeDocument(
                knowledge_base_id=base.id, graph_id=graph.id, source_table="stock_news",
                market="CN_A", symbol="000003", title="丙公司发布经营进展",
                content="丙公司发布经营进展，但与甲公司的关系尚未审核。",
                metadata_json={"published_at": "2026-09-25", "source_name": "测试媒体"},
            ),
        ])
        db.commit()

        context = build_selection_context(
            db, "CN_A", "000001", knowledge_base_ids=[base.id], graph_ids=[graph.id],
            as_of=now, period_start=datetime(2026, 7, 24, tzinfo=timezone.utc).date(),
            period_end=now.date(),
        )
        assert {(row["market"], row["symbol"]) for row in context["related_securities"]} == {("CN_A", "000002")}
        assert {row["symbol"] for row in context["related_security_documents"]} == {"000002"}
        assert context["related_security_documents"][0]["related_via_fact_ids"] == [accepted.id]
        assert all(row["symbol"] != "000003" for row in context["documents"])
    engine.dispose()


def _related_link_output(include_relation_fact: bool = True):
    return {
        "evidence_document_ids": [1, 2],
        "evidence_fact_ids": ["fact-1"] if include_relation_fact else [],
        "dimensions": [],
        "events": [],
        "event_timeline": [],
        "validation_findings": [],
        "event_links": [{
            "relation_type": "RELATED_SECURITY_IMPACT",
            "relation_scope": "RELATED_SECURITY",
            "source_document_ids": [1],
            "target_document_ids": [2],
            "evidence_fact_ids": ["fact-1"] if include_relation_fact else [],
            "evidence_relation_ids": [],
            "summary": "关联公司后续披露验证了交付进度",
            "evidence_quotes": ["项目进入交付阶段", "确认收到首批产品"],
            "direction": "POSITIVE",
            "confidence": 0.8,
        }],
    }


def test_related_security_impact_requires_current_related_documents_and_reviewed_relation():
    item = {"evidence_json": {
        "documents": [
            {"id": 1, "market": "CN_A", "symbol": "000001", "source_table": "stock_news",
             "published_at": "2026-09-01", "content_state": "TEXT_AVAILABLE",
             "content": "甲公司项目进入交付阶段。", "source_name": "同一媒体"},
            {"id": 2, "market": "CN_A", "symbol": "000002", "source_table": "stock_news",
             "published_at": "2026-09-20", "content_state": "TEXT_AVAILABLE",
             "content": "乙公司确认收到首批产品。", "source_name": "同一媒体",
             "relation_scope": "RELATED_SECURITY", "related_via_fact_ids": ["fact-1"]},
        ],
        "facts": [{"id": "fact-1", "status": "ACCEPTED"}],
        "evidence_fact_ids": ["fact-1"], "chunks": [], "graph_paths": [],
    }}
    output = _related_link_output()
    _validate_multi_source_output(output, item, required=False)
    assert output["confidence_breakdown"]["cross_document_support"] < 1
    with pytest.raises(ValueError, match="已审核关系事实"):
        _validate_multi_source_output(_related_link_output(False), item, required=False)


def test_period_document_compaction_keeps_all_mvp_disclosures_within_prompt_budget():
    documents = [{
        "id": index, "market": "CN_A", "symbol": "000027", "source_table": "stock_notice",
        "published_at": f"2026-09-{(index % 28) + 1:02}", "content_state": "TEXT_AVAILABLE",
        "title": f"第{index}份公告", "content": "公告正文" * 2_000,
    } for index in range(1, 76)]
    view = _model_candidate_context({"symbol": "000027", "evidence_json": {"documents": documents}})
    compact = view["evidence_json"]
    assert len(compact["documents"]) == 75
    assert set(compact["citation_allowlist"]["notice_document_ids"]) == set(range(1, 76))
    assert len(json.dumps(view, ensure_ascii=False)) < 32_000


def test_linkage_contract_rejects_unreported_period_document_coverage():
    item = {"evidence_json": {"documents": [
        {"id": 1, "source_table": "stock_news", "content_state": "TEXT_AVAILABLE", "content": "第一篇新闻正文"},
        {"id": 2, "source_table": "stock_notice", "content_state": "TEXT_AVAILABLE", "content": "第二篇公告正文"},
    ], "facts": [], "evidence_fact_ids": [], "chunks": [], "graph_paths": []}}
    output = {"evidence_document_ids": [], "dimensions": [], "events": [], "event_timeline": [],
              "event_links": [], "validation_findings": [], "analyzed_document_ids": [1]}
    with pytest.raises(ValueError, match="周期文档覆盖不完整"):
        _validate_multi_source_output(output, item, required=False, enforce_document_coverage=True)
    output["analyzed_document_ids"] = [1, 2]
    _validate_multi_source_output(output, item, required=False, enforce_document_coverage=True)


def test_dimension_accepts_full_period_document_set_but_rejects_out_of_scope_id():
    schema = SelectionModelOutput.model_json_schema()
    recommendation = schema["$defs"]["SelectionModelRecommendation"]["properties"]
    assert recommendation["event_timeline"]["maxItems"] == 4
    assert recommendation["event_links"]["maxItems"] == 4
    assert recommendation["validation_findings"]["maxItems"] == 4

    document_ids = list(range(1, 81))
    dimension = SelectionDimension.model_validate({
        "dimension": "NOTICE", "status": "SUPPORTED", "direction": "MIXED",
        "summary": "分析周期内公告已全部纳入联动分析。",
        "evidence_document_ids": document_ids,
        "evidence_fact_ids": [], "evidence_relation_ids": [],
    })
    assert dimension.evidence_document_ids == document_ids

    item = {"evidence_json": {
        "documents": [{
            "id": document_id, "source_table": "stock_notice", "content_state": "TEXT_AVAILABLE",
            "content": f"第{document_id}份公告正文",
        } for document_id in document_ids],
        "facts": [], "evidence_fact_ids": [], "chunks": [], "graph_paths": [],
    }}
    output = {
        "evidence_document_ids": document_ids,
        "dimensions": [dimension.model_dump()],
        "events": [], "event_timeline": [], "event_links": [], "validation_findings": [],
        "analyzed_document_ids": document_ids,
    }
    _validate_multi_source_output(output, item, required=False, enforce_document_coverage=True)
    assert output["dimensions"][0]["evidence_document_ids"] == document_ids

    output["dimensions"][0]["evidence_document_ids"] = [*document_ids, 999]
    with pytest.raises(ValueError):
        _validate_multi_source_output(output, item, required=False, enforce_document_coverage=True)


def test_model_hub_injects_ordered_skill_chain_and_records_versions():
    with TemporaryDirectory() as folder:
        engine = create_engine(f"sqlite:///{Path(folder) / 'skills.db'}")
        Base.metadata.create_all(engine)
        with Session(engine) as db, patch("app.services.model_hub.sync_skill_from_file", return_value=False):
            seed_default_models(db)
            db.add_all([
                ModelSkill(skill_code="CHAIN_A", skill_name="链路A", instructions="先执行A", enabled=True,
                           skill_type="PROMPT_SOP", version="1.2.0"),
                ModelSkill(skill_code="CHAIN_B", skill_name="链路B", instructions="再执行B", enabled=True,
                           skill_type="PROMPT_SOP", version="2.1.0"),
            ])
            db.commit()
            log = ModelHubService(db).chat(
                task_type="general_chat", messages=[{"role": "user", "content": "测试"}],
                metadata_json={"skill_codes": ["CHAIN_A", "CHAIN_B"]},
            )
            metadata = log.request_json["metadata_json"]
            assert [row["skill_code"] for row in metadata["skill_chain"]] == ["CHAIN_A", "CHAIN_B"]
            assert [row["skill_version"] for row in metadata["skill_chain"]] == ["1.2.0", "2.1.0"]
            assert [row["content"] for row in log.request_json["messages"][:2]] == ["先执行A", "再执行B"]
        engine.dispose()
