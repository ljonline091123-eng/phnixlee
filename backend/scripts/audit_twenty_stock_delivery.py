"""Read-only acceptance against saved objects, chunks, graph and real model logs."""
from __future__ import annotations

import argparse
from collections import Counter
from datetime import datetime, timezone
import hashlib
import io
import json

import pyarrow.parquet as pq
from sqlalchemy import select

from app.db.session import SessionLocal
from app.models.ai_hub import KnowledgeDocument, KnowledgeEntity, KnowledgeGraph, KnowledgeRelation, ModelCallLog
from app.models.decision_review import SelectionDecisionSnapshot
from app.models.foundation import FoundationListing
from app.models.lakehouse import DocumentChunkVersion, LakeDataset, LakeDatasetVersion, LakeLineageEvent, LakeObject
from app.models.market_data import StockSymbol
from app.models.selection import SelectionCandidate, SelectionRun
from app.services import lakehouse
from app.services.selection import _evidence, _model_candidate_context
from app.services.selection_audit import payload_hash
from scripts.verify_twenty_stock_chain import GRAPH_CODE, OUTPUT, SYMBOLS


def audit(db, run_id):
    graph = db.scalar(select(KnowledgeGraph).where(KnowledgeGraph.graph_code == GRAPH_CODE))
    assert graph is not None
    expected = set(SYMBOLS)
    datasets = []
    for dataset in db.scalars(select(LakeDataset).where(LakeDataset.dataset_code.like("FIXED20_%"))).all():
        version = db.scalar(select(LakeDatasetVersion).where(
            LakeDatasetVersion.dataset_id == dataset.id, LakeDatasetVersion.version == dataset.current_version))
        assert version is not None and version.status == "PUBLISHED" and version.quality_json["passed"]
        obj = db.get(LakeObject, version.object_id)
        assert obj is not None
        content = lakehouse.read_object(obj)
        assert len(content) == obj.byte_size and hashlib.sha256(content).hexdigest() == obj.content_hash
        records = [json.loads(line) for line in content.splitlines()] if dataset.format == "JSONL" else pq.read_table(io.BytesIO(content)).to_pylist()
        assert len(records) == version.row_count
        assert {row["symbol"] for row in records} == expected
        assert {row["market"] for row in records} == {"CN_A"}
        lineage = db.scalar(select(LakeLineageEvent).where(
            LakeLineageEvent.downstream_type == "DATASET_VERSION",
            LakeLineageEvent.downstream_id == f"{dataset.dataset_code}:{version.version}",
            LakeLineageEvent.upstream_type == "SOURCE_TABLE"))
        assert lineage is not None
        datasets.append({"code": dataset.dataset_code, "layer": dataset.layer, "format": dataset.format,
            "version": version.version, "rows": len(records), "hash_verified": True,
            "quality_level": version.quality_json.get("level"), "lineage_id": lineage.id})
    assert len(datasets) == 15
    docs = {row.id: row for row in db.scalars(select(KnowledgeDocument).where(KnowledgeDocument.graph_id == graph.id)).all()}
    entity_ids = set(db.scalars(select(KnowledgeEntity.id).where(KnowledgeEntity.graph_id == graph.id)).all())
    relations = db.scalars(select(KnowledgeRelation).where(KnowledgeRelation.graph_id == graph.id)).all()
    for row in relations:
        assert row.subject_entity_id in entity_ids and row.object_entity_id in entity_ids
        assert row.evidence_document_id is None or row.evidence_document_id in docs
    chunks = db.scalars(select(DocumentChunkVersion).where(DocumentChunkVersion.document_key.in_(
        [f"knowledge_document:{key}" for key in docs]))).all()
    object_cache = {}
    chunk_lineage = set(db.scalars(select(LakeLineageEvent.downstream_id).where(
        LakeLineageEvent.downstream_type == "DOCUMENT_CHUNK")).all())
    archive_edges = {(row.upstream_id, row.downstream_id) for row in db.scalars(select(LakeLineageEvent).where(
        LakeLineageEvent.upstream_type == "KNOWLEDGE_DOCUMENT", LakeLineageEvent.downstream_type == "LAKE_OBJECT")).all()}
    for chunk in chunks:
        assert chunk.source_object_id and chunk.id in chunk_lineage
        if chunk.source_object_id not in object_cache:
            obj = db.get(LakeObject, chunk.source_object_id)
            content = lakehouse.read_object(obj)
            assert hashlib.sha256(content).hexdigest() == obj.content_hash
            object_cache[chunk.source_object_id] = content.decode("utf-8")
        original = object_cache[chunk.source_object_id]
        assert (chunk.document_id, chunk.source_object_id) in archive_edges
        assert original == (docs[int(chunk.document_id)].content or docs[int(chunk.document_id)].title)
        assert original[chunk.start_offset:chunk.end_offset] == chunk.chunk_text
        assert hashlib.sha256(chunk.chunk_text.encode("utf-8")).hexdigest() == chunk.content_hash
        assert chunk.embedding_kind != "HASH" or chunk.embedding_status != "AVAILABLE"
    assert chunks
    stocks = []
    for symbol in SYMBOLS:
        stock = db.scalar(select(StockSymbol).where(StockSymbol.market == "CN_A", StockSymbol.symbol == symbol))
        listing = db.scalar(select(FoundationListing).where(FoundationListing.stock_symbol_id == stock.id))
        evidence = _evidence(db, "CN_A", symbol, [graph.knowledge_base_id], [graph.id])
        assert listing is not None and not evidence.get("context_error")
        assert all(evidence.get(key) for key in ("chunks", "facts", "graph_paths", "lineage"))
        stocks.append({"symbol": symbol, "name": stock.name, "company_linked": True,
            "chunks": len(evidence["chunks"]), "facts": len(evidence["facts"]),
            "relations": len(evidence["graph_paths"]), "lineage": len(evidence["lineage"])})
    run = db.get(SelectionRun, run_id)
    assert run and run.analysis_mode == "MODEL_COMPLETED" and run.data_mode == "REAL"
    candidates = db.scalars(select(SelectionCandidate).where(SelectionCandidate.run_id == run_id)).all()
    assert {row.symbol for row in candidates} == expected
    logs = set()
    for candidate in candidates:
        assert candidate.analysis_json["model_output_status"] == "SUCCESS"
        model = candidate.analysis_json["model"]
        compact = _model_candidate_context({"evidence_json": candidate.evidence_json})["evidence_json"]
        assert set(model["evidence_document_ids"]).issubset(compact["model_document_ids"])
        assert set(model.get("evidence_fact_ids", [])).issubset(compact["evidence_fact_ids"])
        assert set(model.get("evidence_chunk_ids", [])).issubset({row["id"] for row in compact["chunks"]})
        log = db.get(ModelCallLog, candidate.analysis_json["model_call_log_id"])
        assert log and log.status == "SUCCESS" and "MOCK" not in str(log.provider_code).upper()
        logs.add(log.id)
        assert candidate.hard_rules_json.get("volume_ratio_basis") in {"SOURCE_QUOTE_RATIO", "SAME_SOURCE_DAILY_BARS", "MISSING"}
    snapshots = db.scalars(select(SelectionDecisionSnapshot).where(SelectionDecisionSnapshot.run_id == run_id)).all()
    assert len(snapshots) >= 20
    for snapshot in snapshots:
        assert snapshot.payload_hash == payload_hash({"payload": snapshot.payload_json, "evidence": snapshot.evidence_json})
    return {"passed": True, "checked_at": datetime.now(timezone.utc).isoformat(), "run_id": run.id,
        "model_instance": run.model_instance_code, "real_model_calls": len(logs), "candidate_count": len(candidates),
        "recommendations": dict(Counter(row.analysis_json["model"]["decision"] for row in candidates)),
        "graph_id": graph.id, "knowledge_base_id": graph.knowledge_base_id, "documents": len(docs),
        "entities": len(entity_ids), "relations": len(relations), "archived_documents": len(object_cache),
        "checked_chunks": len(chunks), "section_states": dict(Counter(row.section_status for row in chunks)),
        "embedding_states": dict(Counter(row.embedding_status for row in chunks)),
        "decision_snapshots": len(snapshots), "datasets": datasets, "stocks": stocks,
        "limits": ["使用已有本地采集数据，非全量实时重新采集", "质量规则通过不代表已完成语义与投资效果验证",
                   "语义向量尚未生成", "历史日线成交量口径未全面复核，不进行跨来源成交量相除",
                   "实际未来跟踪须等待确认后的交易日"]}


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--run-id", type=int, required=True)
    args = parser.parse_args()
    with SessionLocal() as db:
        report = audit(db, args.run_id)
    OUTPUT.mkdir(parents=True, exist_ok=True)
    (OUTPUT / "delivery-audit.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({key: value for key, value in report.items() if key not in {"datasets", "stocks"}}, ensure_ascii=True))
