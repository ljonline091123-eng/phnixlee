"""Small, repeatable data-to-knowledge pipeline for bounded stock scopes.

This is intentionally synchronous for the local MVP.  It uses the existing
source adapters and governance services, records each stage in ``pipeline_*``
tables, and can be moved behind the worker without changing the contracts.
"""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Any
from uuid import uuid4

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, KnowledgeGraph
from app.models.foundation import FoundationListing
from app.models.market_data import StockSymbol
from app.models.pipeline import PipelineRun, PipelineStageRun
from app.models.lakehouse import LakeLineageEvent
from app.services import lakehouse
from app.services.graph_identity import resolve_many
from app.services.resource_hub import build_knowledge_graph


COMPANY_TABLES = (
    "foundation_entity", "foundation_security", "foundation_listing", "foundation_evidence",
    "foundation_fact", "foundation_security_classification",
)


def _stage(db: Session, run: PipelineRun, code: str, output: dict[str, Any], status: str = "SUCCESS") -> None:
    row = db.scalar(select(PipelineStageRun).where(
        PipelineStageRun.pipeline_run_id == run.id, PipelineStageRun.stage_code == code,
    ))
    if row is None:
        row = PipelineStageRun(pipeline_run_id=run.id, stage_code=code, attempt=1)
        db.add(row)
    row.status, row.output_json, row.completed_at = status, output, datetime.now(timezone.utc)
    db.flush()


def run_stock_pipeline(
    db: Session,
    *,
    market: str,
    symbols: list[str],
    knowledge_base_id: int | None = None,
    graph_id: int | None = None,
    export_lakehouse: bool = True,
    archive_chunks: bool = True,
    run_graph: bool = False,
    include_company_tables: bool = True,
    dataset_limit: int = 10000,
    chunk_size: int = 1800,
    overlap: int = 180,
    idempotency_key: str | None = None,
) -> dict[str, Any]:
    market = market.strip().upper()
    normalized = list(dict.fromkeys(str(item).strip().upper() for item in symbols if str(item).strip()))
    if not normalized:
        raise ValueError("至少提供一只股票")
    if len(normalized) > 200:
        raise ValueError("单次管道最多处理200只股票")
    if graph_id:
        graph = db.get(KnowledgeGraph, graph_id)
        if graph is None or not graph.enabled:
            raise ValueError("知识图谱不存在或已停用")
        kb = db.get(KnowledgeBase, graph.knowledge_base_id)
    else:
        kb = db.get(KnowledgeBase, knowledge_base_id) if knowledge_base_id else db.scalar(
            select(KnowledgeBase).where(KnowledgeBase.kb_code == "STOCK_FULL_KG")
        )
        graph = db.get(KnowledgeGraph, graph_id) if graph_id else (db.scalar(
            select(KnowledgeGraph).where(KnowledgeGraph.knowledge_base_id == kb.id).order_by(KnowledgeGraph.id)
        ) if kb else None)
    if kb is None:
        raise ValueError("知识库不存在")
    source_tables = list(dict.fromkeys(kb.source_tables or (graph.source_tables if graph else []) or []))
    if include_company_tables:
        source_tables.extend(table for table in COMPANY_TABLES if table not in source_tables)
    run = None
    if idempotency_key:
        run = db.scalar(select(PipelineRun).where(PipelineRun.idempotency_key == idempotency_key))
    if run is not None:
        return {"pipeline_run_id": run.id, "status": run.status, **(run.output_json or {})}
    run = PipelineRun(pipeline_type="STOCK_KNOWLEDGE_LAKEHOUSE", trigger_type="API",
                      status="RUNNING", correlation_id=str(uuid4()), idempotency_key=idempotency_key,
                      input_json={"market": market, "symbols": normalized, "knowledge_base_id": kb.id,
                                  "graph_id": graph.id if graph else None})
    db.add(run)
    db.flush()
    pairs = [(market, symbol) for symbol in normalized]
    identities = resolve_many(db, pairs)
    mapped = sum(item.get("mapping_status") == "MAPPED" for item in identities.values())
    _stage(db, run, "IDENTITY_RESOLUTION", {"requested": len(pairs), "mapped": mapped,
                                             "unmapped": len(pairs) - mapped, "identities": identities})
    quality: dict[str, Any] = {}
    for table in source_tables:
        try:
            quality[table] = lakehouse.assess_dataset_source(db, source_table=table, layer="NORMALIZED", limit=dataset_limit)
        except Exception as exc:
            quality[table] = {"level": "UNAVAILABLE", "passed": False, "error": str(exc)[:300]}
    _stage(db, run, "QUALITY_GATE", {"tables": quality})
    exports: dict[str, Any] = {}
    if export_lakehouse:
        for table in source_tables:
            try:
                # Empty optional company feeds are recorded, not fatal to the
                # stock pipeline.  Serving publication remains quality-gated.
                exports[table] = lakehouse.export_dataset(
                    db, dataset_code=f"pipeline_{table}_normalized", dataset_name=table,
                    layer="NORMALIZED", source_table=table, limit=dataset_limit, scope_pairs=pairs,
                )
            except Exception as exc:
                exports[table] = {"status": "SKIPPED", "error": str(exc)[:300]}
    _stage(db, run, "LAKEHOUSE_EXPORT", {"exports": exports})
    chunks: dict[str, Any] = {}
    if archive_chunks:
        doc_query = select(KnowledgeDocument).where(KnowledgeDocument.market == market,
                                                    KnowledgeDocument.symbol.in_(normalized))
        if graph:
            doc_query = doc_query.where(KnowledgeDocument.graph_id == graph.id)
        elif kb:
            doc_query = doc_query.where(KnowledgeDocument.knowledge_base_id == kb.id)
        docs = list(db.scalars(doc_query.order_by(KnowledgeDocument.id)).all())
        for document in docs[: min(len(docs), 2000)]:
            try:
                chunks[str(document.id)] = lakehouse.create_chunks(
                    db, document_key=f"knowledge_document:{document.id}", document_id=str(document.id),
                    text=document.content or document.title, chunk_size=chunk_size, overlap=overlap,
                    parser_version="PIPELINE_STRUCTURE_V1", embedding_model="HASH_EMBED_V1",
                    archive_original=True,
                )
            except Exception as exc:
                chunks[str(document.id)] = {"status": "FAILED", "error": str(exc)[:300]}
    _stage(db, run, "DOCUMENT_CHUNKS", {"document_count": len(chunks), "chunks": chunks})
    graph_result: dict[str, Any] = {"status": "NOT_REQUESTED", "graph_id": graph.id if graph else None}
    if run_graph and graph is not None:
        try:
            graph.symbol = None
            counts = build_knowledge_graph(db, graph, scope_pairs=pairs)
            graph_result = {"status": "BUILT", "counts": counts}
        except Exception as exc:
            graph_result = {"status": "FAILED", "error": str(exc)[:300]}
    _stage(db, run, "KNOWLEDGE_GRAPH", graph_result, "SUCCESS" if graph_result.get("status") != "FAILED" else "FAILED")
    batch_id = str(uuid4())
    db.add(LakeLineageEvent(batch_id=batch_id, upstream_type="PIPELINE_RUN", upstream_id=str(run.id),
        downstream_type="KNOWLEDGE_GRAPH", downstream_id=str(graph.id if graph else kb.id),
        transformation="DATA_TO_MASTER_KB_KG_LAKEHOUSE", dataset_version=None,
        metadata_json={"market": market, "symbols": normalized, "stages": ["IDENTITY_RESOLUTION", "QUALITY_GATE", "LAKEHOUSE_EXPORT", "DOCUMENT_CHUNKS", "KNOWLEDGE_GRAPH"]}))
    output = {"market": market, "symbols": normalized, "knowledge_base_id": kb.id,
              "graph_id": graph.id if graph else None, "identity": {"requested": len(pairs), "mapped": mapped},
              "quality": quality, "exports": exports, "chunks": {"documents": len(chunks)},
              "graph": graph_result, "lineage_batch_id": batch_id}
    run.status = "COMPLETED" if graph_result.get("status") != "FAILED" else "PARTIAL"
    run.current_stage = "COMPLETE"
    run.output_json = output
    run.completed_at = datetime.now(timezone.utc)
    db.commit()
    return {"pipeline_run_id": run.id, "status": run.status, **output}

