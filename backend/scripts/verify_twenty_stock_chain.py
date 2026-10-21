"""Repeatable real-data acceptance. Never rebuilds an existing graph.

Run from backend: python -m scripts.verify_twenty_stock_chain --prepare
Then: python -m scripts.verify_twenty_stock_chain --analyze
Preparation archives local source snapshots; it does not claim a fresh crawl.
"""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import json
from pathlib import Path

from sqlalchemy import func, select

from app.db.session import SessionLocal
from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, KnowledgeGraph, KnowledgeRelation
from app.models.market_data import StockSymbol
from app.models.foundation import FoundationListing
from app.schemas.selection import SelectionRunCreate
from app.services import lakehouse
from app.services.resource_hub import build_knowledge_graph
from app.services.selection import _evidence, create_selection_run


SYMBOLS = ["000006", "000007", "000008", "000009", "000011", "000012", "000014", "000017", "000019", "000020",
           "000021", "000025", "000026", "000027", "000028", "000029", "000030", "000031", "000032", "000034"]
SOURCES = ["stock_symbol", "stock_news", "stock_notice", "stock_financial_report", "stock_kline", "stock_realtime_quote", "stock_f10_cache"]
GRAPH_CODE = "FIXED20_ACCEPTANCE_20261021"
OUTPUT = Path(__file__).resolve().parents[1] / "validation" / "twenty-stock-chain"


def emit(value):
    print(json.dumps(value, ensure_ascii=False, default=str), flush=True)


def prepare(db):
    graph = db.scalar(select(KnowledgeGraph).where(KnowledgeGraph.graph_code == GRAPH_CODE))
    if graph is None:
        base = KnowledgeBase(kb_code=GRAPH_CODE, kb_name="20股全链路验证知识库", source_tables=SOURCES,
            description="固定样本；保留全部来源记录，仅为选股载入的有限证据创建切片。非全市场验收。")
        db.add(base)
        db.flush()
        graph = KnowledgeGraph(knowledge_base_id=base.id, graph_code=GRAPH_CODE, graph_name="20股全链路验证图谱",
            source_tables=SOURCES, governance_report_json={"symbols": SYMBOLS, "market": "CN_A", "acceptance_scope": "FIXED20"})
        db.add(graph)
        db.flush()
        result = build_knowledge_graph(db, graph, scope_pairs=[("CN_A", s) for s in SYMBOLS])
        graph.governance_status = "GOVERNED" if result["coverage"]["complete"] else "PENDING"
        graph.last_governed_at = datetime.now(timezone.utc)
        graph.governance_report_json = {**graph.governance_report_json, **result,
            "governance_method": "DETERMINISTIC_SOURCE_PROJECTION", "semantic_verification": "NOT_CLAIMED"}
        db.commit()
        emit({"stage": "图谱投影", "graph_id": graph.id, **result})
    else:
        emit({"stage": "复用既有验收图谱", "graph_id": graph.id})
    report = dict(graph.governance_report_json or {})
    if not report.get("coverage", {}).get("complete"):
        raise ValueError("固定样本来源覆盖门禁未通过；保留PENDING，不调用真实选股模型")
    datasets = dict(report.get("datasets") or {})
    for source in SOURCES:
        for layer in ("RAW", "NORMALIZED"):
            key = f"{source}:{layer}"
            if key in datasets:
                continue
            try:
                result = lakehouse.export_dataset(db, dataset_code=f"FIXED20_{source}_{layer}",
                    dataset_name=f"20股验证·{source}·{layer}", source_table=source, layer=layer,
                    limit=100000, scope_pairs=[("CN_A", s) for s in SYMBOLS])
                datasets[key] = {k: result[k] for k in ("dataset_id", "version", "row_count", "quality", "object_id")}
                emit({"stage": "湖仓发布", "source": source, "layer": layer, "rows": result["row_count"]})
            except Exception as exc:
                datasets[key] = {"status": "PARTIAL", "error": str(exc)}
                emit({"stage": "湖仓门禁未通过", "source": source, "layer": layer, "error": str(exc)})
        report["datasets"] = datasets
        graph.governance_report_json = dict(report)
        db.commit()
    if "stock_symbol:SERVING" not in datasets:
        result = lakehouse.export_dataset(db, dataset_code="FIXED20_stock_symbol_SERVING", dataset_name="20股验证·证券主数据·服务层",
            source_table="stock_symbol", layer="SERVING", limit=100000, scope_pairs=[("CN_A", s) for s in SYMBOLS])
        datasets["stock_symbol:SERVING"] = {k: result[k] for k in ("dataset_id", "version", "row_count", "quality", "object_id")}
    rows = []
    for symbol in SYMBOLS:
        evidence = _evidence(db, "CN_A", symbol, [graph.knowledge_base_id], [graph.id])
        doc_ids = set(evidence.get("model_document_ids", []))
        doc_ids.update(evidence.get("evidence_context", {}).get("evidence_document_ids", []))
        docs = db.scalars(select(KnowledgeDocument).where(KnowledgeDocument.id.in_(doc_ids))).all()
        chunk_count = 0
        for doc in docs:
            result = lakehouse.create_chunks(db, document_key=f"knowledge_document:{doc.id}", document_id=str(doc.id),
                text=doc.content or doc.title, embedding_model=None, archive_original=True)
            chunk_count += result["chunk_count"]
        evidence = _evidence(db, "CN_A", symbol, [graph.knowledge_base_id], [graph.id])
        stock = db.scalar(select(StockSymbol).where(StockSymbol.market == "CN_A", StockSymbol.symbol == symbol))
        listing = db.scalar(select(FoundationListing).where(FoundationListing.stock_symbol_id == stock.id))
        row = {"symbol": symbol, "name": stock.name, "company_linked": listing is not None,
            "documents": len(docs), "selected_chunks": len(evidence.get("chunks", [])), "archived_chunks": chunk_count,
            "facts": len(evidence.get("facts", [])), "relations": len(evidence.get("graph_paths", [])),
            "lineage": len(evidence.get("lineage", [])), "missing_data": evidence.get("missing_data", []),
            "context_error": evidence.get("context_error"), "embedding_status": "MISSING"}
        row["passed"] = bool(row["company_linked"] and row["documents"] and row["selected_chunks"] and row["facts"] and row["relations"] and row["lineage"] and not row["context_error"])
        rows.append(row)
        emit({"stage": "证据验收", **row})
    report.update({"datasets": datasets, "acceptance_rows": rows, "prepared_at": datetime.now(timezone.utc).isoformat(),
        "chunk_scope": "BOUNDED_SELECTION_EVIDENCE", "embedding_status": "MISSING",
        "scope_notes": ["本轮使用已有本地采集数据；行情日期在候选表逐股展示。", "结构化观测与原文投影不等同于LLM语义事件核验。",
                        "切片仅覆盖本轮选股证据范围，未宣称全库切片或语义向量完成。"]})
    report["acceptance_passed"] = all(row["passed"] for row in rows) and all("error" not in d for d in datasets.values())
    graph.governance_report_json = report
    db.commit()
    return graph, report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--prepare", action="store_true")
    parser.add_argument("--analyze", action="store_true")
    args = parser.parse_args()
    with SessionLocal() as db:
        if args.prepare:
            graph, report = prepare(db)
        else:
            graph = db.scalar(select(KnowledgeGraph).where(KnowledgeGraph.graph_code == GRAPH_CODE))
            report = dict(graph.governance_report_json or {}) if graph else {"prepared": False, "symbols": SYMBOLS}
        if args.analyze:
            if not graph or not report.get("acceptance_passed"):
                raise ValueError("请先运行 --prepare，并修复20股证据及湖仓门禁")
            run = create_selection_run(db, SelectionRunCreate(market="CN_A", symbols=SYMBOLS, candidate_limit=20,
                min_abs_change_pct=0, min_volume_ratio=0, knowledge_base_ids=[graph.knowledge_base_id], graph_ids=[graph.id],
                use_model=True, data_mode="REAL", universe_scope="FIXED20_ACCEPTANCE"))
            report["selection_run_id"] = run.id
            report["analysis_mode"] = run.analysis_mode
            report["model_error"] = run.error_message
            graph.governance_report_json = dict(report)
            db.commit()
        OUTPUT.mkdir(parents=True, exist_ok=True)
        stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
        (OUTPUT / f"acceptance-{stamp}.json").write_text(json.dumps(report, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
        emit({"acceptance_passed": report.get("acceptance_passed"), "rows": len(report.get("acceptance_rows", [])),
              "passed": sum(bool(row["passed"]) for row in report.get("acceptance_rows", [])),
              "selection_run_id": report.get("selection_run_id"), "analysis_mode": report.get("analysis_mode"),
              "error": report.get("model_error"), "report": str(OUTPUT / f"acceptance-{stamp}.json")})


if __name__ == "__main__":
    import sys
    sys.stdout.reconfigure(encoding="utf-8")
    main()
