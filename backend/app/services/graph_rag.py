"""Bounded, versioned context for model-assisted stock analysis.

The online tables and the legacy graph remain the source of truth.  This
module only projects records that were available at ``as_of`` into a compact
context contract.  It deliberately does not call a model and it never writes
entities or facts.  Callers can therefore use the result as an auditable
GraphRAG boundary for LLA/agent prompts.
"""

from __future__ import annotations

import hashlib
import json
from datetime import date, datetime, timezone
from typing import Any, Iterable

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, KnowledgeEntity, KnowledgeGraph, KnowledgeRelation
from app.models.foundation import (
    FoundationEntity,
    FoundationFact,
    FoundationFactEvidence,
    FoundationEvidence,
    FoundationListing,
    FoundationSecurity,
)
from app.models.lakehouse import DocumentChunkVersion, LakeDataset, LakeDatasetVersion, LakeLineageEvent
from app.models.market_data import StockSymbol
from app.services.graph_identity import canonical_company_id, canonical_security_id, resolve_many


CONTEXT_VERSION = "GRAPH_RAG_CONTEXT_V1"


def _iso(value: Any) -> str | None:
    if value is None:
        return None
    if isinstance(value, (datetime, date)):
        return value.isoformat()
    return str(value)


def _aware(value: datetime | None) -> datetime | None:
    if value is None:
        return None
    return value.replace(tzinfo=timezone.utc) if value.tzinfo is None else value


def _known(value: Any, cutoff: datetime | None) -> bool:
    """Apply a conservative availability gate without assuming DB tz support."""
    if cutoff is None or value is None:
        return True
    if isinstance(value, str):
        try:
            value = datetime.fromisoformat(value.replace("Z", "+00:00"))
        except ValueError:
            return True
    if isinstance(value, date) and not isinstance(value, datetime):
        value = datetime.combine(value, datetime.min.time(), tzinfo=timezone.utc)
    if not isinstance(value, datetime):
        return True
    left, right = _aware(value), _aware(cutoff)
    return bool(left and right and left <= right)


def _hash(value: Any) -> str:
    payload = json.dumps(value, ensure_ascii=False, sort_keys=True, default=str, separators=(",", ":"))
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def _unique(values: Iterable[Any]) -> list[Any]:
    return list(dict.fromkeys(value for value in values if value not in (None, "")))


def _doc_payload(row: KnowledgeDocument, *, include_content: bool = True) -> dict[str, Any]:
    metadata = row.metadata_json or {}
    content = row.content or ""
    return {
        "id": row.id,
        "title": row.title,
        "source_table": row.source_table,
        "source_record_id": row.source_record_id,
        "market": row.market,
        "symbol": row.symbol,
        "graph_id": row.graph_id,
        "content": content[:1200] if include_content else None,
        "content_hash": _hash(content),
        "metadata": metadata,
        "updated_at": _iso(row.updated_at),
    }


def _identity(db: Session, market: str, symbol: str, cutoff: datetime | None) -> dict[str, Any]:
    pair = resolve_many(db, [(market, symbol)], known_at=cutoff).get((market.upper(), symbol.upper()), {})
    result = {
        "canonical_security_id": canonical_security_id(market, symbol),
        "canonical_company_id": pair.get("canonical_company_id"),
        "mapping_status": pair.get("mapping_status", "UNMAPPED"),
        "stock_symbol_id": pair.get("stock_symbol_id"),
        "security_id": pair.get("security_id"),
        "company_id": pair.get("company_id"),
        "listing_id": pair.get("listing_id"),
        "identity_version": pair.get("identity_version"),
    }
    stock = db.scalar(select(StockSymbol).where(StockSymbol.market == market, StockSymbol.symbol == symbol))
    if stock is not None:
        result["security"] = {
            "id": stock.id,
            "market": stock.market,
            "symbol": stock.symbol,
            "name": stock.name,
            "exchange": stock.exchange,
            "asset_type": stock.asset_type,
            "status": stock.status,
            "list_date": stock.list_date,
            "last_synced_at": _iso(stock.last_synced_at),
        }
    company_id = pair.get("company_id")
    if company_id:
        company = db.get(FoundationEntity, company_id)
        if company is not None and _known(company.created_at, cutoff):
            result["company"] = {
                "id": company.id,
                "canonical_id": canonical_company_id(company.id),
                "name": company.name,
                "entity_type": company.entity_type,
                "jurisdiction": company.jurisdiction,
                "identifier_scheme": company.identifier_scheme,
                "identifier_value": company.identifier_value,
                "properties": company.properties_json or {},
            }
    return result


def _dataset_versions(db: Session, source_tables: set[str]) -> list[dict[str, Any]]:
    rows = list(db.scalars(select(LakeDataset).order_by(LakeDataset.dataset_code)).all())
    result: list[dict[str, Any]] = []
    for dataset in rows:
        description = (dataset.description or "")
        # A dataset may not have an explicit source-table column.  Keep all
        # current versions when the catalog metadata is opaque, and restrict
        # only when the source table is declared in the description/code.
        marker = f"{dataset.dataset_code} {description}".lower()
        if source_tables and not any(table.lower() in marker for table in source_tables):
            continue
        version = dataset.current_version
        row = None
        if version:
            row = db.scalar(select(LakeDatasetVersion).where(
                LakeDatasetVersion.dataset_id == dataset.id,
                LakeDatasetVersion.version == version,
            ))
        result.append({
            "dataset_id": dataset.id,
            "dataset_code": dataset.dataset_code,
            "layer": dataset.layer,
            "format": dataset.format,
            "version": version,
            "version_id": row.id if row else None,
            "quality": row.quality_json if row else {},
            "row_count": row.row_count if row else 0,
        })
    return result


def _chunks(db: Session, document_ids: list[int], limit: int) -> list[dict[str, Any]]:
    if not document_ids or limit <= 0:
        return []
    rows = list(db.scalars(select(DocumentChunkVersion).where(
        DocumentChunkVersion.document_id.in_([str(item) for item in document_ids]),
    ).order_by(DocumentChunkVersion.document_id, DocumentChunkVersion.chunk_index,
               DocumentChunkVersion.created_at.desc()).limit(limit)).all())
    return [{
        "id": row.id,
        "document_id": row.document_id,
        "chunk_index": row.chunk_index,
        "chunk_version": row.chunk_version,
        "content_hash": row.content_hash,
        "text": row.chunk_text[:1800],
        "start_offset": row.start_offset,
        "end_offset": row.end_offset,
        "parser_version": row.parser_version,
        "embedding_model": row.embedding_model,
        "embedding_status": "AVAILABLE" if row.embedding_model else "NOT_GENERATED",
        "section_title": (row.metadata_json or {}).get("section_title"),
        "boundary_type": (row.metadata_json or {}).get("boundary_type"),
        "metadata": row.metadata_json or {},
    } for row in rows]


def build_selection_context(
    db: Session,
    market: str,
    symbol: str,
    *,
    knowledge_base_ids: list[int] | None = None,
    graph_ids: list[int] | None = None,
    as_of: datetime | None = None,
    max_documents: int = 16,
    max_relations: int = 40,
    max_facts: int = 40,
    max_chunks: int = 32,
) -> dict[str, Any]:
    """Build a bounded, reproducible context contract for one security.

    ``as_of`` is the information cutoff.  The result includes identifiers and
    hashes for every versioned layer so a later review can prove what the LLA
    actually saw.  Empty source areas are represented explicitly as gaps.
    """
    market, symbol = market.strip().upper(), symbol.strip().upper()
    cutoff = as_of or datetime.now(timezone.utc)
    kb_ids = sorted(set(int(item) for item in (knowledge_base_ids or []) if item))
    graph_ids = sorted(set(int(item) for item in (graph_ids or []) if item))
    identity = _identity(db, market, symbol, cutoff)

    doc_query = select(KnowledgeDocument).where(
        KnowledgeDocument.market == market,
        KnowledgeDocument.symbol == symbol,
    )
    if kb_ids:
        doc_query = doc_query.where(KnowledgeDocument.knowledge_base_id.in_(kb_ids))
    if graph_ids:
        doc_query = doc_query.where(KnowledgeDocument.graph_id.in_(graph_ids))
    docs = [row for row in db.scalars(doc_query.order_by(KnowledgeDocument.updated_at.desc(), KnowledgeDocument.id.desc()).limit(max_documents * 3)).all()
            if _known(row.updated_at, cutoff)][:max_documents]
    document_ids = [row.id for row in docs]

    entity_query = select(KnowledgeEntity).where(
        KnowledgeEntity.knowledge_base_id.in_(kb_ids) if kb_ids else KnowledgeEntity.id > 0,
    )
    if graph_ids:
        entity_query = entity_query.where(KnowledgeEntity.graph_id.in_(graph_ids))
    suffixes = (f":{market}:{symbol}", f":{market}:{symbol}:%")
    from sqlalchemy import or_
    entity_query = entity_query.where(or_(
        KnowledgeEntity.entity_key.like(suffixes[0]),
        KnowledgeEntity.entity_key.like(suffixes[1]),
    ))
    entities = list(db.scalars(entity_query.limit(120)).all())
    entity_ids = [row.id for row in entities]
    relations: list[KnowledgeRelation] = []
    if entity_ids:
        relation_query = select(KnowledgeRelation).where(
            or_(KnowledgeRelation.subject_entity_id.in_(entity_ids), KnowledgeRelation.object_entity_id.in_(entity_ids)),
        )
        if kb_ids:
            relation_query = relation_query.where(KnowledgeRelation.knowledge_base_id.in_(kb_ids))
        if graph_ids:
            relation_query = relation_query.where(KnowledgeRelation.graph_id.in_(graph_ids))
        relations = list(db.scalars(relation_query.order_by(KnowledgeRelation.id.desc()).limit(max_relations)).all())
    all_relation_entity_ids = _unique(
        [value for relation in relations for value in (relation.subject_entity_id, relation.object_entity_id)]
    )
    relation_entities = {row.id: row for row in db.scalars(select(KnowledgeEntity).where(
        KnowledgeEntity.id.in_(all_relation_entity_ids)
    )).all()} if all_relation_entity_ids else {}
    relation_doc_ids = _unique([row.evidence_document_id for row in relations])
    relation_docs = {row.id: row for row in db.scalars(select(KnowledgeDocument).where(
        KnowledgeDocument.id.in_(relation_doc_ids)
    )).all()} if relation_doc_ids else {}
    graph_paths = [{
        "id": row.id,
        "subject_entity_id": row.subject_entity_id,
        "object_entity_id": row.object_entity_id,
        "subject_name": relation_entities.get(row.subject_entity_id).entity_name if row.subject_entity_id in relation_entities else None,
        "object_name": relation_entities.get(row.object_entity_id).entity_name if row.object_entity_id in relation_entities else None,
        "predicate": row.predicate,
        "evidence_document_id": row.evidence_document_id,
        "evidence_excerpt": (relation_docs[row.evidence_document_id].content or "")[:800]
        if row.evidence_document_id in relation_docs else None,
    } for row in relations]

    company_id = identity.get("company_id")
    facts: list[dict[str, Any]] = []
    fact_evidence_ids: list[str] = []
    if company_id:
        fact_query = select(FoundationFact).where(
            FoundationFact.status == "ACCEPTED",
            (FoundationFact.subject_entity_id == company_id) | (FoundationFact.object_entity_id == company_id),
        ).order_by(FoundationFact.created_at.desc()).limit(max_facts)
        for fact in db.scalars(fact_query).all():
            if not _known(fact.created_at, cutoff):
                continue
            links = list(db.scalars(select(FoundationFactEvidence).where(
                FoundationFactEvidence.fact_id == fact.id,
            )).all())
            proof_ids = [link.evidence_id for link in links]
            proofs = list(db.scalars(select(FoundationEvidence).where(
                FoundationEvidence.id.in_(proof_ids),
            )).all()) if proof_ids else []
            proofs = [proof for proof in proofs if _known(proof.available_at, cutoff)]
            fact_evidence_ids.extend(proof.id for proof in proofs)
            facts.append({
                "id": fact.id,
                "fact_type": fact.fact_type,
                "title": fact.title,
                "subject_entity_id": fact.subject_entity_id,
                "object_entity_id": fact.object_entity_id,
                "properties": fact.properties_json or {},
                "status": fact.status,
                "valid_from": _iso(fact.valid_from),
                "valid_to": _iso(fact.valid_to),
                "created_at": _iso(fact.created_at),
                "evidence": [{
                    "id": proof.id,
                    "title": proof.title,
                    "source_name": proof.source_name,
                    "source_url": proof.url,
                    "published_at": _iso(proof.published_at),
                    "available_at": _iso(proof.available_at),
                    "excerpt": proof.content[:900],
                    "content_hash": proof.content_hash,
                } for proof in proofs],
            })

    documents = [_doc_payload(row) for row in docs]
    # Include relation proof documents in the evidence set, but do not let them
    # displace the source-balanced primary documents above.
    for row in relation_docs.values():
        if row.id not in document_ids and len(documents) < max_documents:
            documents.append(_doc_payload(row))
            document_ids.append(row.id)
    chunks = _chunks(db, document_ids, max_chunks)
    source_tables = {row.source_table for row in docs}
    datasets = _dataset_versions(db, source_tables)
    dataset_versions = [row.get("version") for row in datasets if row.get("version")]
    lineage = list(db.scalars(select(LakeLineageEvent).where(
        (LakeLineageEvent.upstream_id.in_([str(item) for item in document_ids]) |
         LakeLineageEvent.downstream_id.in_([str(item) for item in document_ids])),
    ).order_by(LakeLineageEvent.created_at.desc()).limit(80)).all()) if document_ids else []
    lineage_rows = [{
        "id": row.id,
        "batch_id": row.batch_id,
        "upstream_type": row.upstream_type,
        "upstream_id": row.upstream_id,
        "downstream_type": row.downstream_type,
        "downstream_id": row.downstream_id,
        "transformation": row.transformation,
        "parser_version": row.parser_version,
        "dataset_version": row.dataset_version,
    } for row in lineage]

    graph_versions = []
    if graph_ids:
        graph_versions = [{"id": row.id, "code": row.graph_code, "version": row.version,
                           "status": row.governance_status} for row in db.scalars(select(KnowledgeGraph).where(KnowledgeGraph.id.in_(graph_ids))).all()]
    kb_versions = [{"id": row.id, "code": row.kb_code, "version": row.version, "status": row.status}
                   for row in db.scalars(select(KnowledgeBase).where(KnowledgeBase.id.in_(kb_ids))).all()] if kb_ids else []
    graph_version = _hash({"graphs": graph_versions, "kbs": kb_versions, "relations": [row.id for row in relations]})[:32]
    lakehouse_version = _hash({"datasets": datasets, "lineage": [row["id"] for row in lineage_rows]})[:32]
    document_version = _hash({"documents": [(row.id, row.updated_at, _hash(row.content or "")) for row in docs],
                              "chunks": [(row["id"], row["chunk_version"]) for row in chunks]})[:32]
    evidence_span = [{
        "document_id": row["id"],
        "start": 0,
        "end": min(len(row.get("content") or ""), 1200),
        "quote": row.get("content") or "",
        "content_hash": row.get("content_hash"),
    } for row in documents[:max_documents]]
    missing = []
    if not documents:
        missing.append("知识库文档")
    if not facts and not graph_paths:
        missing.append("公司关系事实/图谱关系")
    if not chunks:
        missing.append("文档切片")
    if any(item.get("embedding_status") != "AVAILABLE" for item in chunks):
        missing.append("部分切片尚未生成向量")
    context = {
        "context_version": CONTEXT_VERSION,
        "as_of": cutoff.isoformat(),
        "data_cutoff": cutoff.date().isoformat(),
        "identity": identity,
        "graph_version": graph_version,
        "lakehouse_version": lakehouse_version,
        "document_version": document_version,
        "knowledge_base_versions": kb_versions,
        "graph_versions": graph_versions,
        "lakehouse_datasets": datasets,
        "lineage": lineage_rows,
        "documents": documents,
        "facts": facts,
        "graph_paths": graph_paths,
        "chunks": chunks,
        "evidence_span": evidence_span,
        "evidence_document_ids": document_ids,
        "evidence_fact_ids": [item["id"] for item in facts],
        "evidence_source_ids": fact_evidence_ids,
        "missing_data": missing,
        "counts": {
            "documents": len(documents), "facts": len(facts), "graph_paths": len(graph_paths),
            "chunks": len(chunks), "lineage": len(lineage_rows), "datasets": len(datasets),
        },
    }
    context["context_hash"] = _hash({key: value for key, value in context.items() if key != "context_hash"})
    return context


def context_for_candidate(db: Session, candidate: Any, *, as_of: datetime | None = None) -> dict[str, Any]:
    """Convenience wrapper used by the selection workflow."""
    run = getattr(candidate, "run", None)
    if run is None:
        from app.models.selection import SelectionRun
        run = db.get(SelectionRun, candidate.run_id)
    return build_selection_context(
        db,
        candidate.market,
        candidate.symbol,
        knowledge_base_ids=list(getattr(run, "knowledge_base_ids_json", []) or []),
        graph_ids=list(getattr(run, "graph_ids_json", []) or []),
        as_of=as_of or getattr(run, "as_of", None),
    )

