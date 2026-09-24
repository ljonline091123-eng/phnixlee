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

from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, KnowledgeGraph, KnowledgeRelation
from app.models.foundation import FoundationListing
from app.models.market_data import StockSymbol
from app.models.pipeline import PipelineRun, PipelineStageRun
from app.models.lakehouse import LakeLineageEvent
from app.services import lakehouse
from app.services.graph_identity import resolve_many
from app.services.resource_hub import build_knowledge_graph


COMPANY_TABLES = (
    "foundation_entity", "foundation_security", "foundation_listing", "foundation_evidence",
    "foundation_fact", "foundation_fact_evidence", "foundation_fact_review",
    "foundation_security_classification", "foundation_source_identity",
    "foundation_company_mapping_state", "classification_definition",
)


class PipelineConflictError(ValueError):
    """The same idempotency key was reused for a different canonical request."""


def _json_identities(identities: dict[tuple[str, str], dict[str, Any]]) -> dict[str, dict[str, Any]]:
    """Convert the resolver's tuple keys to a stable JSON/API contract.

    ``resolve_many`` intentionally uses a tuple key internally so market and
    symbol cannot collide.  Pipeline stage output is persisted as JSON, where
    tuple keys are invalid on SQLite and in the HTTP response.  Keep the
    canonical pair explicit in the key while retaining the resolver payload.
    """
    return {
        f"{market}:{symbol}": {"market": market, "symbol": symbol, **(payload or {})}
        for (market, symbol), payload in identities.items()
    }


def _stage(db: Session, run: PipelineRun, code: str, output: dict[str, Any], status: str = "SUCCESS") -> None:
    row = db.scalar(select(PipelineStageRun).where(
        PipelineStageRun.pipeline_run_id == run.id, PipelineStageRun.stage_code == code,
    ))
    if row is None:
        row = PipelineStageRun(pipeline_run_id=run.id, stage_code=code, attempt=1)
        db.add(row)
    row.status, row.output_json, row.completed_at = status, output, datetime.now(timezone.utc)
    db.flush()


def _document_rank(document: KnowledgeDocument) -> tuple[float, int]:
    """Return a comparable recency key for SQLite naive/aware timestamps."""
    value = document.updated_at
    if isinstance(value, datetime):
        if value.tzinfo is None:
            value = value.replace(tzinfo=timezone.utc)
        return value.timestamp(), int(document.id or 0)
    return 0.0, int(document.id or 0)


def _select_chunk_documents(
    documents: list[KnowledgeDocument],
    *,
    limit: int | None,
    priority_document_ids: set[int] | None = None,
    latest_per_security: int = 16,
) -> tuple[list[KnowledgeDocument], dict[str, Any]]:
    """Choose a transparent, source-balanced document slice for chunking.

    GraphRAG reads the newest documents for each security. Selecting by
    ascending database ID could therefore create a graph with documents but
    no retrievable chunks. Reserve the newest documents per security and
    explicit relation-evidence documents first, then fill the remaining
    budget round-robin across source tables.
    """
    ranked = sorted(documents, key=_document_rank, reverse=True)
    total = len(ranked)
    if limit is None:
        selected = ranked
    else:
        cap = max(1, int(limit))
        selected_by_id: dict[int, KnowledgeDocument] = {}
        by_security: dict[tuple[str, str], list[KnowledgeDocument]] = {}
        for document in ranked:
            key = (str(document.market or ""), str(document.symbol or ""))
            by_security.setdefault(key, []).append(document)
        # When the caller deliberately supplies a budget smaller than the
        # number of securities, retain the securities with the newest source
        # observations first and report the unavoidable partial coverage.
        security_keys = sorted(
            by_security,
            key=lambda key: _document_rank(by_security[key][0]),
            reverse=True,
        )
        for offset in range(max(1, latest_per_security)):
            for key in security_keys:
                if len(selected_by_id) >= cap:
                    break
                rows = by_security[key]
                if offset < len(rows):
                    selected_by_id.setdefault(rows[offset].id, rows[offset])
            if len(selected_by_id) >= cap:
                break

        # Relation proof documents may be older than the latest source rows;
        # include them after the per-security reserve so they cannot starve a
        # security that GraphRAG is expected to serve.
        priority_ids = {int(item) for item in (priority_document_ids or set()) if str(item).isdigit()}
        for document in ranked:
            if len(selected_by_id) >= cap:
                break
            if document.id in priority_ids:
                selected_by_id.setdefault(document.id, document)

        # Fill the budget evenly by source table so one high-volume feed does
        # not crowd all news/notice/F10 evidence out of the chunk layer.
        by_source: dict[str, list[KnowledgeDocument]] = {}
        for document in ranked:
            by_source.setdefault(str(document.source_table or "UNKNOWN"), []).append(document)
        source_keys = sorted(by_source)
        cursors = {source: 0 for source in source_keys}
        while len(selected_by_id) < cap and source_keys:
            added = False
            for source in source_keys:
                rows = by_source[source]
                cursor = cursors[source]
                while cursor < len(rows) and rows[cursor].id in selected_by_id:
                    cursor += 1
                cursors[source] = cursor
                if cursor < len(rows):
                    selected_by_id[rows[cursor].id] = rows[cursor]
                    cursors[source] = cursor + 1
                    added = True
                    if len(selected_by_id) >= cap:
                        break
            if not added:
                break
        selected = sorted(selected_by_id.values(), key=_document_rank, reverse=True)

    selected_ids = {document.id for document in selected}
    priority_ids = {
        int(item) for item in (priority_document_ids or set()) if str(item).isdigit()
    }
    security_keys = {
        (str(document.market or ""), str(document.symbol or "")) for document in ranked
    }
    selected_security_keys = {
        (str(document.market or ""), str(document.symbol or "")) for document in selected
    }
    document_coverage = round(len(selected) / total, 6) if total else None
    security_coverage = round(len(selected_security_keys) / len(security_keys), 6) if security_keys else None
    return selected, {
        "documents_total": total,
        "documents_selected": len(selected),
        # ``coverage_ratio`` remains as a compatibility alias.  The explicit
        # field name prevents consumers from confusing selection coverage
        # with successful chunk creation or model-context inclusion.
        "coverage_ratio": document_coverage,
        "document_selection_ratio": document_coverage,
        "coverage_status": "NO_DOCUMENTS" if not total else ("COMPLETE" if len(selected) == total else "PARTIAL"),
        "securities_total": len(security_keys),
        "securities_selected": len(selected_security_keys),
        "security_selection_ratio": security_coverage,
        "truncated": len(selected) < total,
        "selection_policy": "LATEST_PER_SECURITY_THEN_PRIORITY_EVIDENCE_THEN_SOURCE_ROUND_ROBIN",
        "priority_documents_selected": len(selected_ids & priority_ids),
    }


def _archive_document_chunks(
    db: Session,
    documents: list[KnowledgeDocument],
    *,
    chunk_document_limit: int | None,
    priority_document_ids: set[int] | None,
    chunk_size: int,
    overlap: int,
) -> tuple[dict[str, Any], dict[str, Any]]:
    selected, summary = _select_chunk_documents(
        documents, limit=chunk_document_limit, priority_document_ids=priority_document_ids,
    )
    results: dict[str, Any] = {}
    failed: list[dict[str, Any]] = []
    created_chunks = 0
    reused_chunks = 0
    for document in selected:
        try:
            result = lakehouse.create_chunks(
                db, document_key=f"knowledge_document:{document.id}", document_id=str(document.id),
                text=document.content or document.title, chunk_size=chunk_size, overlap=overlap,
                parser_version="PIPELINE_STRUCTURE_V1", embedding_model="HASH_EMBED_V1",
                archive_original=True,
            )
            results[str(document.id)] = result
            created_chunks += int(result.get("created_count", 0))
            reused_chunks += int(result.get("reused_count", 0))
        except Exception as exc:
            error = {"status": "FAILED", "error": str(exc)[:300]}
            results[str(document.id)] = error
            failed.append({"document_id": document.id, **error})
    summary.update({
        "documents_with_chunks": len(selected) - len(failed),
        "successful_chunk_ratio": (
            round((len(selected) - len(failed)) / len(selected), 6) if selected else None
        ),
        "failed_documents": len(failed),
        "failed_document_samples": failed[:50],
        "created_chunk_count": created_chunks,
        "reused_chunk_count": reused_chunks,
    })
    return results, summary


def _completion_summary(
    *,
    requested: int,
    mapped: int,
    quality: dict[str, Any],
    exports: dict[str, Any],
    export_lakehouse: bool,
    agent_governance: dict[str, Any],
    run_agent_governance: bool,
    chunks: dict[str, Any],
    archive_chunks: bool,
    graph_result: dict[str, Any],
    run_graph: bool,
    graph: KnowledgeGraph | None,
) -> dict[str, Any]:
    """Aggregate requested-stage outcomes without overstating completion."""
    issues: list[dict[str, Any]] = []

    def issue(code: str, stage: str, detail: Any) -> None:
        issues.append({"code": code, "stage": stage, "detail": detail})

    if mapped < requested:
        issue("IDENTITY_UNMAPPED", "IDENTITY_RESOLUTION", {"requested": requested, "mapped": mapped})
    failed_quality = sorted(name for name, result in quality.items() if not bool(result.get("passed")))
    if failed_quality:
        issue("QUALITY_GATE_NOT_PASSED", "QUALITY_GATE", failed_quality)
    if export_lakehouse:
        skipped_exports = sorted(
            name for name, result in exports.items() if str(result.get("status") or "").upper() != "PUBLISHED"
        )
        if skipped_exports:
            issue("LAKEHOUSE_EXPORT_INCOMPLETE", "LAKEHOUSE_EXPORT", skipped_exports)
    if run_agent_governance and agent_governance.get("status") not in {"SUCCESS", "COMPLETED", "PASSED"}:
        issue("AGENT_GOVERNANCE_NOT_PASSED", "AGENT_SKILL_GOVERNANCE", agent_governance.get("status"))
    if archive_chunks:
        if not chunks.get("documents_total"):
            issue("NO_KNOWLEDGE_DOCUMENTS", "DOCUMENT_CHUNKS", "当前范围没有可切片知识文档")
        if chunks.get("truncated"):
            issue("CHUNK_SELECTION_TRUNCATED", "DOCUMENT_CHUNKS", {
                "selected": chunks.get("documents_selected"), "total": chunks.get("documents_total"),
            })
        if chunks.get("failed_documents"):
            issue("CHUNK_FAILURES", "DOCUMENT_CHUNKS", chunks.get("failed_documents"))
    if run_graph and graph_result.get("status") != "BUILT":
        issue("GRAPH_BUILD_FAILED", "KNOWLEDGE_GRAPH", graph_result.get("status"))

    selected_graph_status = graph.governance_status if graph is not None else None
    graph_governance_ready = bool(
        graph is not None
        and selected_graph_status in {"GOVERNED", "LOCKED"}
        and graph_result.get("status") != "FAILED"
    )
    if run_graph and graph_result.get("status") == "BUILT" and not graph_governance_ready:
        issue("GRAPH_PENDING_GOVERNANCE", "KNOWLEDGE_GRAPH", selected_graph_status or "PENDING")
    model_selection_ready = graph_governance_ready and not issues
    if model_selection_ready:
        next_action = None
    elif not graph_governance_ready:
        next_action = "治理并锁定所选知识图谱后，方可调用真实模型选股"
    else:
        next_action = "先处理 completion.issues 中的数据身份、质量、湖仓或切片问题"
    return {
        "status": "COMPLETED" if not issues else "PARTIAL",
        "issues": issues,
        "requested_stages": {
            "lakehouse_export": export_lakehouse,
            "document_chunks": archive_chunks,
            "knowledge_graph": run_graph,
            "agent_skill_governance": run_agent_governance,
        },
        "model_selection_ready": model_selection_ready,
        "graph_governance_status": selected_graph_status,
        "next_action": next_action,
    }


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
    run_agent_governance: bool = False,
    governance_record_limit: int = 50,
    governance_run_key: str | None = None,
    include_company_tables: bool = True,
    dataset_limit: int = 10000,
    graph_documents_per_stock: int | None = 50,
    chunk_size: int = 1800,
    overlap: int = 180,
    chunk_document_limit: int | None = 10000,
    idempotency_key: str | None = None,
) -> dict[str, Any]:
    market = market.strip().upper()
    normalized = sorted(set(str(item).strip().upper() for item in symbols if str(item).strip()))
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
    request_input = {
        "market": market, "symbols": normalized, "knowledge_base_id": kb.id,
        "graph_id": graph.id if graph else None,
        "export_lakehouse": export_lakehouse, "archive_chunks": archive_chunks,
        "run_graph": run_graph, "run_agent_governance": run_agent_governance,
        "governance_record_limit": governance_record_limit,
        "governance_run_key": governance_run_key,
        "include_company_tables": include_company_tables,
        "dataset_limit": dataset_limit,
        "graph_documents_per_stock": graph_documents_per_stock,
        "chunk_size": chunk_size, "overlap": overlap,
        "chunk_document_limit": chunk_document_limit,
    }
    run = None
    if idempotency_key:
        run = db.scalar(select(PipelineRun).where(PipelineRun.idempotency_key == idempotency_key))
    if run is not None:
        if run.pipeline_type != "STOCK_KNOWLEDGE_LAKEHOUSE" or (run.input_json or {}) != request_input:
            raise PipelineConflictError("幂等键已被不同的知识管道请求使用，请更换幂等键")
        return {"pipeline_run_id": run.id, "status": run.status, **(run.output_json or {})}
    run = PipelineRun(pipeline_type="STOCK_KNOWLEDGE_LAKEHOUSE", trigger_type="API",
                      status="RUNNING", correlation_id=str(uuid4()), idempotency_key=idempotency_key,
                      input_json=request_input)
    db.add(run)
    db.flush()
    pairs = [(market, symbol) for symbol in normalized]
    identities = resolve_many(db, pairs)
    identity_payload = _json_identities(identities)
    mapped = sum(item.get("mapping_status") == "MAPPED" for item in identities.values())
    _stage(db, run, "IDENTITY_RESOLUTION", {"requested": len(pairs), "mapped": mapped,
                                             "unmapped": len(pairs) - mapped, "identities": identity_payload},
           "SUCCESS" if mapped == len(pairs) else "PARTIAL")
    quality: dict[str, Any] = {}
    for table in source_tables:
        try:
            quality[table] = lakehouse.assess_dataset_source(
                db,
                source_table=table,
                layer="NORMALIZED",
                limit=dataset_limit,
                scope_pairs=pairs,
            )
        except Exception as exc:
            quality[table] = {"level": "UNAVAILABLE", "passed": False, "error": str(exc)[:300]}
    _stage(db, run, "QUALITY_GATE", {"tables": quality},
           "SUCCESS" if all(bool(item.get("passed")) for item in quality.values()) else "PARTIAL")
    agent_governance: dict[str, Any] = {"status": "NOT_REQUESTED"}
    if run_agent_governance:
        try:
            from app.schemas.knowledge_governance import KnowledgeGovernanceRequest
            from app.services.knowledge_governance import govern_knowledge_sources

            governance_run = govern_knowledge_sources(db, KnowledgeGovernanceRequest(
                record_limit_per_source=governance_record_limit,
                run_key=governance_run_key,
            ))
            agent_governance = {
                "status": governance_run.status,
                "run_id": governance_run.id,
                "summary": governance_run.summary_json or {},
                "agent_skill": "KB_MULTISOURCE_GOVERNOR / KB_SOURCE_AUDITOR",
            }
        except Exception as exc:
            # Agent governance is an optional review stage.  Its failure must
            # remain visible and must not discard deterministic lakehouse data.
            agent_governance = {"status": "FAILED", "error": str(exc)[:500],
                                "agent_skill": "KB_MULTISOURCE_GOVERNOR / KB_SOURCE_AUDITOR"}
    agent_stage_status = "SKIPPED" if not run_agent_governance else (
        "SUCCESS" if agent_governance.get("status") in {"SUCCESS", "COMPLETED", "PASSED"}
        else "FAILED" if agent_governance.get("status") == "FAILED" else "PARTIAL"
    )
    _stage(db, run, "AGENT_SKILL_GOVERNANCE", agent_governance, agent_stage_status)
    exports: dict[str, Any] = {}
    if export_lakehouse:
        for table in source_tables:
            try:
                # Empty optional company feeds are recorded, not fatal to the
                # stock pipeline.  Serving publication remains quality-gated.
                # Company/master tables do not all carry a market/symbol pair.
                # The lakehouse exporter records the requested scope in object
                # metadata but safely exports the bounded master snapshot for
                # those tables instead of failing the whole stock run.
                exports[table] = lakehouse.export_dataset(
                    db, dataset_code=f"pipeline_{table}_normalized", dataset_name=table,
                    layer="NORMALIZED", source_table=table, limit=dataset_limit, scope_pairs=pairs,
                )
            except Exception as exc:
                exports[table] = {"status": "SKIPPED", "error": str(exc)[:300]}
    export_stage_status = "SKIPPED" if not export_lakehouse else (
        "SUCCESS" if exports and all(
            str(item.get("status") or "").upper() == "PUBLISHED" for item in exports.values()
        ) else "PARTIAL"
    )
    _stage(db, run, "LAKEHOUSE_EXPORT", {"exports": exports}, export_stage_status)
    chunks: dict[str, Any] = {}
    chunk_summary: dict[str, Any] = {
        "documents_total": 0,
        "documents_selected": 0,
        "documents_with_chunks": 0,
        "coverage_ratio": None,
        "document_selection_ratio": None,
        "successful_chunk_ratio": None,
        "coverage_status": "NOT_RUN" if not archive_chunks else "NO_DOCUMENTS",
        "securities_total": 0,
        "securities_selected": 0,
        "security_selection_ratio": None,
        "truncated": False,
    }
    if archive_chunks:
        doc_query = select(KnowledgeDocument).where(KnowledgeDocument.market == market,
                                                    KnowledgeDocument.symbol.in_(normalized))
        if graph:
            doc_query = doc_query.where(KnowledgeDocument.graph_id == graph.id)
        elif kb:
            doc_query = doc_query.where(KnowledgeDocument.knowledge_base_id == kb.id)
        docs = list(db.scalars(doc_query.order_by(KnowledgeDocument.id)).all())
        chunks, chunk_summary = _archive_document_chunks(
            db, docs, chunk_document_limit=chunk_document_limit, priority_document_ids=None,
            chunk_size=chunk_size, overlap=overlap,
        )
    graph_result: dict[str, Any] = {"status": "NOT_REQUESTED", "graph_id": graph.id if graph else None}
    if run_graph and graph is not None:
        try:
            # A scoped build must never delete the user's existing full graph.
            # Materialize a separate projection graph for this pipeline run;
            # the original graph remains the historical source of truth.
            projection = KnowledgeGraph(
                knowledge_base_id=graph.knowledge_base_id,
                graph_code=f"{graph.graph_code}_PIPE_{run.id}",
                graph_name=f"{graph.graph_name} · 管道投影 #{run.id}",
                description=f"{graph.description or ''}\n范围：{market}:{','.join(normalized)}",
                symbol=None,
                source_tables=list(graph.source_tables or kb.source_tables or []),
                version=graph.version,
                governance_status="PENDING",
                enabled=True,
            )
            db.add(projection)
            db.flush()
            counts = build_knowledge_graph(
                db, projection, scope_pairs=pairs,
                max_documents_per_stock=graph_documents_per_stock,
            )
            graph_result = {"status": "BUILT", "counts": counts,
                            "graph_id": projection.id, "base_graph_id": graph.id,
                            "projection": True}
        except Exception as exc:
            graph_result = {"status": "FAILED", "error": str(exc)[:300]}
    # A scoped projection creates fresh KnowledgeDocument IDs.  Archive those
    # documents as well so GraphRAG on the projection can resolve chunks and
    # their raw-object lineage; the base graph's chunks remain reusable.
    projection_id = graph_result.get("graph_id") if graph_result.get("projection") else None
    if archive_chunks and projection_id and projection_id != (graph.id if graph else None):
        projection_docs = list(db.scalars(select(KnowledgeDocument).where(
            KnowledgeDocument.graph_id == projection_id,
            KnowledgeDocument.market == market,
            KnowledgeDocument.symbol.in_(normalized),
        ).order_by(KnowledgeDocument.id)).all())
        priority_ids = set(db.scalars(select(KnowledgeRelation.evidence_document_id).where(
            KnowledgeRelation.graph_id == projection_id,
            KnowledgeRelation.evidence_document_id.is_not(None),
        )).all())
        chunks, chunk_summary = _archive_document_chunks(
            db, projection_docs, chunk_document_limit=chunk_document_limit,
            priority_document_ids={int(item) for item in priority_ids if item is not None},
            chunk_size=chunk_size, overlap=overlap,
        )
    chunk_stage_status = "SKIPPED" if not archive_chunks else (
        "SUCCESS" if chunk_summary.get("documents_total")
        and not chunk_summary.get("truncated") and not chunk_summary.get("failed_documents") else "PARTIAL"
    )
    chunk_result_limit = 200
    chunk_result_samples = dict(list(chunks.items())[:chunk_result_limit])
    _stage(db, run, "DOCUMENT_CHUNKS", {
        "document_count": len(chunks),
        "result_samples": chunk_result_samples,
        "results_truncated": len(chunks) > chunk_result_limit,
        "coverage": chunk_summary,
    }, chunk_stage_status)
    graph_stage_status = "SKIPPED" if not run_graph else (
        "SUCCESS" if graph_result.get("status") == "BUILT" else "FAILED"
    )
    _stage(db, run, "KNOWLEDGE_GRAPH", graph_result, graph_stage_status)
    result_graph_id = graph_result.get("graph_id") or (graph.id if graph else None)
    batch_id = str(uuid4())
    db.add(LakeLineageEvent(batch_id=batch_id, upstream_type="PIPELINE_RUN", upstream_id=str(run.id),
        downstream_type="KNOWLEDGE_GRAPH", downstream_id=str(result_graph_id or kb.id),
        transformation="DATA_TO_MASTER_KB_KG_LAKEHOUSE", dataset_version=None,
        metadata_json={"market": market, "symbols": normalized, "stages": ["IDENTITY_RESOLUTION", "QUALITY_GATE", "AGENT_SKILL_GOVERNANCE", "LAKEHOUSE_EXPORT", "DOCUMENT_CHUNKS", "KNOWLEDGE_GRAPH"]}))
    selected_graph = db.get(KnowledgeGraph, result_graph_id) if result_graph_id else None
    completion = _completion_summary(
        requested=len(pairs), mapped=mapped, quality=quality, exports=exports,
        export_lakehouse=export_lakehouse, agent_governance=agent_governance,
        run_agent_governance=run_agent_governance, chunks=chunk_summary,
        archive_chunks=archive_chunks, graph_result=graph_result, run_graph=run_graph,
        graph=selected_graph,
    )
    output = {"market": market, "symbols": normalized, "knowledge_base_id": kb.id,
              "graph_id": result_graph_id, "base_graph_id": graph.id if graph else None,
              "identity": {"requested": len(pairs), "mapped": mapped,
                                                                        "unmapped": len(pairs) - mapped,
                                                                        "identities": identity_payload},
              "quality": quality, "agent_governance": agent_governance,
              "exports": exports, "chunks": {"documents": len(chunks), **chunk_summary},
              "graph": graph_result, "lineage_batch_id": batch_id,
              "completion": completion}
    run.status = completion["status"]
    run.current_stage = "COMPLETE"
    run.output_json = output
    run.completed_at = datetime.now(timezone.utc)
    db.commit()
    return {"pipeline_run_id": run.id, "status": run.status, **output}

