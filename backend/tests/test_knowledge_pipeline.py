from __future__ import annotations

from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session

from app.db.base import Base
from app.models.ai_hub import KnowledgeBase, KnowledgeGraph
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
        monkeypatch.setattr(knowledge_pipeline.lakehouse, "assess_dataset_source",
                            lambda *args, **kwargs: {"passed": True, "level": "PASS"})
        exported: list[str] = []

        def export(_db, **kwargs):
            exported.append(kwargs["source_table"])
            return {"status": "PUBLISHED", "source_table": kwargs["source_table"], "row_count": 1}

        monkeypatch.setattr(knowledge_pipeline.lakehouse, "export_dataset", export)
        result = knowledge_pipeline.run_stock_pipeline(
            db, market="CN_A", symbols=["000001"], export_lakehouse=True,
            archive_chunks=False, run_graph=False,
        )
        assert result["status"] == "COMPLETED"
        assert result["identity"]["identities"]["CN_A:000001"]["market"] == "CN_A"
        assert "foundation_entity" in exported
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

        def build(_db, target, **kwargs):
            seen.append(target.id)
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
        assert db.get(KnowledgeGraph, graph.id).graph_code == "STOCK_GRAPH"
        assert db.scalar(select(KnowledgeGraph).where(KnowledgeGraph.id == result["graph_id"])) is not None
    engine.dispose()
