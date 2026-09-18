import hashlib

from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.models.ai_hub import AgentDataAsset, KnowledgeBase, KnowledgeGraph
from app.models.context_event import StockContextEvent
from app.models.market_data import StockSymbol
from app.schemas.context_events import ContextEventCreate


def import_context_event(db: Session, payload: ContextEventCreate) -> StockContextEvent:
    stock = db.scalar(select(StockSymbol).where(StockSymbol.market == payload.market, StockSymbol.symbol == payload.symbol))
    if not stock:
        raise ValueError("股票不在主数据中，请先同步股票主数据并核对市场与代码")
    values = payload.model_dump(mode="python")
    values["url"] = str(payload.url)
    digest = hashlib.sha256((payload.source_name + "\n" + str(payload.url) + "\n" + payload.title + "\n" + payload.content).encode()).hexdigest()
    statement = select(StockContextEvent).where(StockContextEvent.market == payload.market, StockContextEvent.symbol == payload.symbol, StockContextEvent.content_hash == digest)
    existing = db.scalar(statement)
    if existing:
        return existing
    row = StockContextEvent(**values, content_hash=digest)
    try:
        with db.begin_nested():
            db.add(row)
            db.flush()
    except IntegrityError:
        existing = db.scalar(statement)
        if existing:
            return existing
        raise
    asset = db.scalar(select(AgentDataAsset).where(AgentDataAsset.table_name == "stock_context_event"))
    if asset and asset.governance_status != "LOCKED":
        asset.governance_status = "PENDING"
    for graph in db.scalars(select(KnowledgeGraph).where(KnowledgeGraph.governance_status != "LOCKED")):
        kb = db.get(KnowledgeBase, graph.knowledge_base_id)
        tables = graph.source_tables or (kb.source_tables if kb else [])
        if "stock_context_event" in tables and (not graph.symbol or graph.symbol == payload.symbol):
            graph.governance_status = "PENDING"
    db.commit()
    db.refresh(row)
    return row
