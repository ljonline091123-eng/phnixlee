from __future__ import annotations

from datetime import datetime, timedelta, timezone

import pytest
from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session

from app.db.base import Base
from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, KnowledgeGraph
from app.models.market_data import DataSource, StockSymbol
from app.models.pipeline import PipelineStageRun
from app.services import knowledge_pipeline


def _db():
    engine = create_engine("sqlite://")
    Base.metadata.create_all(engine)
    return engine


def _seed(db: Session) -> tuple[KnowledgeBase, KnowledgeGraph]:
    source = DataSource(source_code="PIPE_TEST", source_name="管道测试源", adapter_type="TEST")
    db.add(source)
    db.flush()
    db.add(StockSymbol(market="CN_A", symbol="000001", exchange="SZ", name="测试银行", source_id=source.id))
    kb = KnowledgeBase(kb_code="STOCK_FULL_KG", kb_name="股票知识库", source_tables=["stock_symbol"])
    db.add(kb)
    db.flush()
    graph = KnowledgeGraph(knowledge_base_id=kb.id, graph_code="STOCK_GRAPH", graph_name="股票图谱",
                           source_tables=["stock_symbol"], governance_status="GOVERNED")
    db.add(graph)
    db.commit()
    return kb, graph


def test_pipeline_serializes_tuple_identity_keys_and_exports_master_tables(monkeypatch):
    engine = _db()
    with Session(engine) as db:
        _seed(db)
        assessed: list[dict] = []

        def assess(*args, **kwargs):
            assessed.append(kwargs)
            return {"passed": True, "level": "PASS"}

        monkeypatch.setattr(knowledge_pipeline.lakehouse, "assess_dataset_source", assess)
        exported: list[str] = []

        def export(_db, **kwargs):
            exported.append(kwargs["source_table"])
            return {"status": "PUBLISHED", "source_table": kwargs["source_table"], "row_count": 1}

        monkeypatch.setattr(knowledge_pipeline.lakehouse, "export_dataset", export)
        result = knowledge_pipeline.run_stock_pipeline(
            db, market="CN_A", symbols=["000001"], export_lakehouse=True,
            archive_chunks=False, run_graph=False,
        )
        assert result["status"] == "PARTIAL"
        assert {item["code"] for item in result["completion"]["issues"]} == {"IDENTITY_UNMAPPED"}
        assert result["completion"]["model_selection_ready"] is False
        assert result["identity"]["identities"]["CN_A:000001"]["market"] == "CN_A"
        assert "foundation_entity" in exported
        assert assessed
        assert all(item["scope_pairs"] == [("CN_A", "000001")] for item in assessed)
        stage = db.scalar(select(PipelineStageRun).where(
            PipelineStageRun.pipeline_run_id == result["pipeline_run_id"],
            PipelineStageRun.stage_code == "IDENTITY_RESOLUTION",
        ))
        assert stage is not None
        assert stage.output_json["identities"]["CN_A:000001"]["symbol"] == "000001"
    engine.dispose()


def test_scoped_graph_build_creates_projection_without_replacing_base_graph(monkeypatch):
    engine = _db()
    with Session(engine) as db:
        _kb, graph = _seed(db)
        monkeypatch.setattr(knowledge_pipeline.lakehouse, "assess_dataset_source",
                            lambda *args, **kwargs: {"passed": True, "level": "PASS"})
        monkeypatch.setattr(knowledge_pipeline.lakehouse, "export_dataset",
                            lambda _db, **kwargs: {"status": "PUBLISHED", "row_count": 1})
        seen: list[int] = []
        seen_limits: list[int | None] = []

        def build(_db, target, **kwargs):
            seen.append(target.id)
            seen_limits.append(kwargs.get("max_documents_per_stock"))
            return {"documents": 1, "entities": 1, "relations": 0}

        monkeypatch.setattr(knowledge_pipeline, "build_knowledge_graph", build)
        result = knowledge_pipeline.run_stock_pipeline(
            db, market="CN_A", symbols=["000001"], export_lakehouse=False,
            archive_chunks=False, run_graph=True,
        )
        assert result["graph"]["status"] == "BUILT"
        assert result["graph"]["base_graph_id"] == graph.id
        assert result["graph_id"] != graph.id
        assert seen == [result["graph_id"]]
        assert seen_limits == [50]
        assert result["status"] == "PARTIAL"
        assert "GRAPH_PENDING_GOVERNANCE" in {
            item["code"] for item in result["completion"]["issues"]
        }
        assert result["completion"]["model_selection_ready"] is False
        assert db.get(KnowledgeGraph, graph.id).graph_code == "STOCK_GRAPH"
        assert db.scalar(select(KnowledgeGraph).where(KnowledgeGraph.id == result["graph_id"])) is not None
    engine.dispose()


def test_chunk_selection_prioritizes_latest_security_documents_and_reports_coverage():
    base = datetime(2026, 1, 1, tzinfo=timezone.utc)
    documents = []
    next_id = 1
    for symbol in ("000001", "000002", "00700"):
        for index in range(20):
            row = KnowledgeDocument(
                id=next_id, knowledge_base_id=1, graph_id=1, source_table="stock_news" if index % 2 else "stock_notice",
                market="CN_A" if symbol != "00700" else "HK", symbol=symbol,
                title=f"{symbol}-{index}", content="正文",
            )
            row.updated_at = base + timedelta(minutes=next_id)
            documents.append(row)
            next_id += 1

    selected, summary = knowledge_pipeline._select_chunk_documents(documents, limit=12)
    selected_keys = {(row.symbol, row.title) for row in selected}
    assert len(selected) == 12
    assert summary["documents_total"] == 60
    assert summary["documents_selected"] == 12
    assert summary["truncated"] is True
    assert summary["coverage_ratio"] == 0.2
    assert summary["document_selection_ratio"] == 0.2
    assert summary["security_selection_ratio"] == 1.0
    assert summary["coverage_status"] == "PARTIAL"
    for symbol in ("000001", "000002", "00700"):
        assert any(item[0] == symbol for item in selected_keys)
        assert {f"{symbol}-{index}" for index in range(16, 20)} <= {
            title for selected_symbol, title in selected_keys if selected_symbol == symbol
        }

    # A large relation-evidence set from one security must not crowd the
    # latest documents for the other securities out of a bounded run.
    balanced, _ = knowledge_pipeline._select_chunk_documents(
        documents, limit=12, priority_document_ids=set(range(1, 21)),
    )
    assert {row.symbol for row in balanced} == {"000001", "000002", "00700"}

    bounded, bounded_summary = knowledge_pipeline._select_chunk_documents(documents, limit=2)
    assert {row.symbol for row in bounded} == {"000002", "00700"}
    assert bounded_summary["securities_selected"] == 2
    assert bounded_summary["securities_total"] == 3
    assert bounded_summary["security_selection_ratio"] == round(2 / 3, 6)

    empty, empty_summary = knowledge_pipeline._select_chunk_documents([], limit=10)
    assert empty == []
    assert empty_summary["coverage_status"] == "NO_DOCUMENTS"
    assert empty_summary["coverage_ratio"] is None


def test_completion_requires_all_requested_gates_for_model_readiness():
    graph = KnowledgeGraph(
        knowledge_base_id=1, graph_code="READY", graph_name="已治理图谱",
        governance_status="GOVERNED",
    )
    complete = knowledge_pipeline._completion_summary(
        requested=1, mapped=1,
        quality={"stock_symbol": {"passed": True}},
        exports={"stock_symbol": {"status": "PUBLISHED"}},
        export_lakehouse=True,
        agent_governance={"status": "NOT_REQUESTED"},
        run_agent_governance=False,
        chunks={"documents_total": 0, "failed_documents": 0, "truncated": False},
        archive_chunks=False,
        graph_result={"status": "NOT_REQUESTED"},
        run_graph=False,
        graph=graph,
    )
    assert complete["status"] == "COMPLETED"
    assert complete["model_selection_ready"] is True

    incomplete = knowledge_pipeline._completion_summary(
        requested=1, mapped=0,
        quality={"stock_symbol": {"passed": True}},
        exports={"stock_symbol": {"status": "PUBLISHED"}},
        export_lakehouse=True,
        agent_governance={"status": "NOT_REQUESTED"},
        run_agent_governance=False,
        chunks={"documents_total": 0, "failed_documents": 0, "truncated": False},
        archive_chunks=False,
        graph_result={"status": "NOT_REQUESTED"},
        run_graph=False,
        graph=graph,
    )
    assert incomplete["status"] == "PARTIAL"
    assert incomplete["model_selection_ready"] is False


def test_pipeline_idempotency_key_rejects_a_different_request(monkeypatch):
    engine = _db()
    with Session(engine) as db:
        _seed(db)
        monkeypatch.setattr(
            knowledge_pipeline.lakehouse,
            "assess_dataset_source",
            lambda *args, **kwargs: {"passed": True, "level": "PASS"},
        )
        first = knowledge_pipeline.run_stock_pipeline(
            db, market="CN_A", symbols=["000001"], export_lakehouse=False,
            archive_chunks=False, run_graph=False, include_company_tables=False,
            idempotency_key="same-key",
        )
        repeated = knowledge_pipeline.run_stock_pipeline(
            db, market="CN_A", symbols=["000001"], export_lakehouse=False,
            archive_chunks=False, run_graph=False, include_company_tables=False,
            idempotency_key="same-key",
        )
        assert repeated["pipeline_run_id"] == first["pipeline_run_id"]
        with pytest.raises(knowledge_pipeline.PipelineConflictError):
            knowledge_pipeline.run_stock_pipeline(
                db, market="CN_A", symbols=["000002"], export_lakehouse=False,
                archive_chunks=False, run_graph=False, include_company_tables=False,
                idempotency_key="same-key",
            )
    engine.dispose()
