from __future__ import annotations

from collections import defaultdict
from datetime import datetime, timezone
from typing import Any

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, KnowledgeEntity, KnowledgeGraph, KnowledgeRelation
from app.models.foundation import FoundationEntity, FoundationListing, FoundationSecurity
from app.models.market_data import (
    StockF10Cache, StockFinancialReport, StockKline, StockNews, StockNotice,
    StockRealtimeQuote, StockSymbol,
)
from app.services import lakehouse
from app.services.resource_hub import build_knowledge_graph


VALIDATION_KB_CODE = "LAKEHOUSE_20_STOCK_KB"
VALIDATION_GRAPH_CODE = "LAKEHOUSE_20_STOCK_KG"
GRAPH_SOURCES = [
    "stock_symbol", "stock_kline", "stock_realtime_quote", "stock_financial_report",
    "stock_news", "stock_notice", "stock_f10_cache", "stock_context_event", "research_report",
]


def select_scope(db: Session, count: int = 20) -> list[dict[str, Any]]:
    mapped = db.execute(select(
        StockSymbol.id, StockSymbol.market, StockSymbol.symbol, StockSymbol.name,
        FoundationEntity.id.label("company_id"), FoundationEntity.name.label("company_name"),
    ).join(FoundationListing, FoundationListing.stock_symbol_id == StockSymbol.id)
     .join(FoundationSecurity, FoundationSecurity.id == FoundationListing.security_id)
     .join(FoundationEntity, FoundationEntity.id == FoundationSecurity.entity_id)
     .where(StockSymbol.status == "LISTED", FoundationEntity.entity_type == "COMPANY")).all()
    records = {(row.market, row.symbol): row for row in mapped}
    coverage: dict[tuple[str, str], dict[str, int]] = defaultdict(dict)
    for model in (StockKline, StockFinancialReport, StockNews, StockNotice, StockF10Cache, StockRealtimeQuote):
        for market, symbol, total in db.execute(select(model.market, model.symbol, func.count(model.id))
                                                .group_by(model.market, model.symbol)):
            if (market, symbol) in records:
                coverage[(market, symbol)][model.__tablename__] = int(total)
    ranked = sorted(records, key=lambda pair: (len(coverage[pair]), sum(coverage[pair].values()), pair), reverse=True)
    result = []
    for pair in ranked[:count]:
        row = records[pair]
        result.append({"market": row.market, "symbol": row.symbol, "stock_name": row.name,
                       "stock_symbol_id": row.id, "company_id": row.company_id,
                       "company_name": row.company_name, "source_counts": coverage[pair],
                       "covered_source_count": len(coverage[pair])})
    if len(result) != count:
        raise ValueError(f"只有 {len(result)} 只股票具备公司映射，无法完成 {count} 只验证")
    return result


def _ensure_graph(db: Session, scope: list[dict[str, Any]]) -> KnowledgeGraph:
    kb = db.scalar(select(KnowledgeBase).where(KnowledgeBase.kb_code == VALIDATION_KB_CODE))
    if kb is None:
        kb = KnowledgeBase(kb_code=VALIDATION_KB_CODE, kb_name="20只股票湖仓与公司知识验证库",
            description="用于结构化、非结构化、股票和公司双对象端到端验收。",
            status="DRAFT", version="1.0.0", source_tables=GRAPH_SOURCES, enabled=True)
        db.add(kb)
        db.flush()
    graph = db.scalar(select(KnowledgeGraph).where(KnowledgeGraph.graph_code == VALIDATION_GRAPH_CODE))
    if graph is None:
        graph = KnowledgeGraph(knowledge_base_id=kb.id, graph_code=VALIDATION_GRAPH_CODE,
            graph_name="20只股票与公司湖仓验证图谱", description="独立验收图谱，不覆盖历史图谱。",
            source_tables=GRAPH_SOURCES, version="1.0.0", governance_status="PENDING", enabled=True)
        db.add(graph)
        db.flush()
    graph.source_tables = GRAPH_SOURCES
    graph.governance_report_json = {"scope": [{"market": item["market"], "symbol": item["symbol"]} for item in scope]}
    return graph


def run_validation(db: Session, count: int = 20) -> dict[str, Any]:
    scope = select_scope(db, count)
    pairs = [(item["market"], item["symbol"]) for item in scope]
    graph = _ensure_graph(db, scope)
    graph_result = build_knowledge_graph(db, graph, max_documents=100000, scope_pairs=pairs,
                                         max_documents_per_stock=50)
    fully_covered_stocks = sum(item["covered_source_count"] == 6 for item in scope)
    validation_status = "COMPLETE" if fully_covered_stocks == len(scope) else "PARTIAL"
    graph.governance_status = "GOVERNED" if validation_status == "COMPLETE" else "PENDING"
    graph.last_governed_at = datetime.now(timezone.utc)

    dataset_specs = [
        ("validation20_stock_master", "20股股票主数据", "NORMALIZED", "stock_symbol"),
        ("validation20_market", "20股历史量价", "NORMALIZED", "stock_kline"),
        ("validation20_financial", "20股财务数据", "NORMALIZED", "stock_financial_report"),
        ("validation20_f10", "20股F10数据", "NORMALIZED", "stock_f10_cache"),
        ("validation20_news_raw", "20股新闻原始数据", "RAW", "stock_news"),
        ("validation20_notice_raw", "20股公告原始数据", "RAW", "stock_notice"),
        ("validation20_company_serving", "20股公司映射服务数据", "SERVING", "foundation_listing"),
    ]
    datasets = []
    for code, name, layer, table in dataset_specs:
        try:
            datasets.append(lakehouse.export_dataset(db, dataset_code=code, dataset_name=name,
                layer=layer, source_table=table, limit=100000, scope_pairs=pairs))
        except ValueError as exc:
            datasets.append({"dataset_code": code, "source_table": table, "status": "EMPTY", "detail": str(exc)})

    archived_documents = 0
    created_chunks = 0
    for market, symbol in pairs:
        for source_table in ("stock_news", "stock_notice", "stock_context_event", "research_report"):
            documents = db.scalars(select(KnowledgeDocument).where(
                KnowledgeDocument.graph_id == graph.id, KnowledgeDocument.market == market,
                KnowledgeDocument.symbol == symbol, KnowledgeDocument.source_table == source_table)
                .order_by(KnowledgeDocument.id.desc()).limit(20)).all()
            for document in documents:
                result = lakehouse.create_chunks(db, document_key=f"knowledge_document:{document.id}",
                    document_id=str(document.id), text=document.content or document.title,
                    parser_version="STRUCTURE_V2", archive_original=True, section_title=document.title)
                archived_documents += 1
                created_chunks += result["created_count"]

    stock_nodes = db.scalar(select(func.count(KnowledgeEntity.id)).where(
        KnowledgeEntity.graph_id == graph.id, KnowledgeEntity.entity_type == "STOCK")) or 0
    company_nodes = db.scalar(select(func.count(KnowledgeEntity.id)).where(
        KnowledgeEntity.graph_id == graph.id, KnowledgeEntity.entity_type == "COMPANY")) or 0
    issued_by = db.scalar(select(func.count(KnowledgeRelation.id)).where(
        KnowledgeRelation.graph_id == graph.id, KnowledgeRelation.predicate == "ISSUED_BY")) or 0
    evidence_relations = db.scalar(select(func.count(KnowledgeRelation.id)).where(
        KnowledgeRelation.graph_id == graph.id, KnowledgeRelation.evidence_document_id.is_not(None))) or 0
    report = {
        "validated_at": datetime.now(timezone.utc).isoformat(), "validation_status": validation_status,
        "fully_covered_stock_count": fully_covered_stocks, "scope": scope,
        "graph": {"id": graph.id, "code": graph.graph_code, **graph_result,
                  "stock_nodes": int(stock_nodes), "company_nodes": int(company_nodes),
                  "issued_by_relations": int(issued_by), "evidence_relations": int(evidence_relations)},
        "datasets": datasets, "archived_documents": archived_documents,
        "created_chunks": created_chunks,
    }
    graph.governance_report_json = {**(graph.governance_report_json or {}), "validation": report}
    db.commit()
    return report
