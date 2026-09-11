from __future__ import annotations

from datetime import datetime, timezone
from typing import Any

from sqlalchemy import delete, func, inspect, select, text
from sqlalchemy.engine import Engine
from sqlalchemy.orm import Session

from app.db.session import engine
from app.models.ai_hub import (
    AgentChildLink,
    AgentDataAsset,
    AgentDataAssetLink,
    AgentDefinition,
    AgentKnowledgeBaseLink,
    AgentSkillLink,
    KnowledgeBase,
    KnowledgeDocument,
    KnowledgeEntity,
    KnowledgeRelation,
)
from app.models.market_data import (
    StockFinancialReport,
    StockNews,
    StockNotice,
    StockSymbol,
)


DEFAULT_DATA_ASSETS = (
    ("STOCK_SYMBOL", "stock_symbol", "股票主数据", "A股、港股、新三板和创新层股票代码、名称及市场属性。"),
    ("STOCK_QUOTE", "stock_realtime_quote", "实时行情", "股票最新价格、涨跌幅、成交量和成交额。"),
    ("STOCK_KLINE", "stock_kline", "历史K线", "股票日线 OHLC、成交量和成交额。"),
    ("STOCK_FINANCIAL", "stock_financial_report", "财务报告", "F10 财务指标和报告期数据。"),
    ("STOCK_NOTICE", "stock_notice", "公司公告", "上市公司及挂牌公司公告。"),
    ("STOCK_NEWS", "stock_news", "新闻资讯", "股票相关新闻和资讯。"),
    ("STOCK_F10", "stock_f10_cache", "F10资料缓存", "公司简介、股东、业务构成等 F10 分区缓存。"),
    ("WATCHLIST", "watchlist_item", "自选股", "用户自选股票清单。"),
    ("DATA_SYNC_LOG", "data_sync_log", "数据同步日志", "批量数据同步状态与统计。"),
    ("DATA_FETCH_LOG", "data_fetch_log", "按需获取日志", "单股票按需数据获取记录。"),
)


def seed_default_data_assets(db: Session) -> None:
    """Register the project's local tables as read-only Agent data assets."""
    for asset_code, table_name, display_name, description in DEFAULT_DATA_ASSETS:
        asset = db.scalar(select(AgentDataAsset).where(AgentDataAsset.asset_code == asset_code))
        if asset is None:
            asset = AgentDataAsset(
                asset_code=asset_code,
                table_name=table_name,
                display_name=display_name,
                description=description,
                governance_status="READY",
                enabled=True,
            )
            db.add(asset)
        else:
            asset.table_name = table_name
            asset.display_name = display_name
            asset.description = description
    db.commit()


def table_columns(table_name: str, db_engine: Engine = engine) -> list[str]:
    inspector = inspect(db_engine)
    if table_name not in set(inspector.get_table_names()):
        return []
    return [str(item["name"]) for item in inspector.get_columns(table_name)]


def inspect_data_asset(asset: AgentDataAsset, db_engine: Engine = engine) -> AgentDataAsset:
    columns = table_columns(asset.table_name, db_engine)
    if not columns:
        asset.row_count = 0
        asset.governance_status = "MISSING"
        asset.last_inspected_at = datetime.now(timezone.utc)
        return asset
    with db_engine.connect() as connection:
        asset.row_count = int(connection.execute(text(f'SELECT COUNT(*) FROM "{asset.table_name}"')).scalar_one())
    if not asset.allowed_columns:
        asset.allowed_columns = columns
    else:
        asset.allowed_columns = [column for column in asset.allowed_columns if column in columns]
    asset.governance_status = "READY"
    asset.last_inspected_at = datetime.now(timezone.utc)
    return asset


def set_agent_links(
    db: Session,
    agent: AgentDefinition,
    child_agent_ids: list[int],
    skill_ids: list[int],
    knowledge_base_ids: list[int],
    data_asset_ids: list[int],
) -> None:
    unique_children = list(dict.fromkeys(child_agent_ids))
    unique_skills = list(dict.fromkeys(skill_ids))
    unique_kbs = list(dict.fromkeys(knowledge_base_ids))
    unique_assets = list(dict.fromkeys(data_asset_ids))
    if agent.id in unique_children:
        raise ValueError("An agent cannot reference itself as a child agent")

    children = list(db.scalars(select(AgentDefinition).where(AgentDefinition.id.in_(unique_children))).all()) if unique_children else []
    if len(children) != len(unique_children):
        raise ValueError("One or more child agents do not exist")
    if would_create_agent_cycle(db, agent.id, unique_children):
        raise ValueError("Child agent link would create a cycle")

    if unique_skills:
        from app.models.ai_hub import ModelSkill

        count = db.scalar(select(func.count(ModelSkill.id)).where(ModelSkill.id.in_(unique_skills))) or 0
        if int(count) != len(unique_skills):
            raise ValueError("One or more skills do not exist")
    if unique_kbs:
        count = db.scalar(select(func.count(KnowledgeBase.id)).where(KnowledgeBase.id.in_(unique_kbs))) or 0
        if int(count) != len(unique_kbs):
            raise ValueError("One or more knowledge bases do not exist")
    if unique_assets:
        count = db.scalar(select(func.count(AgentDataAsset.id)).where(AgentDataAsset.id.in_(unique_assets))) or 0
        if int(count) != len(unique_assets):
            raise ValueError("One or more data assets do not exist")

    db.execute(delete(AgentChildLink).where(AgentChildLink.agent_id == agent.id))
    db.execute(delete(AgentSkillLink).where(AgentSkillLink.agent_id == agent.id))
    db.execute(delete(AgentKnowledgeBaseLink).where(AgentKnowledgeBaseLink.agent_id == agent.id))
    db.execute(delete(AgentDataAssetLink).where(AgentDataAssetLink.agent_id == agent.id))
    db.flush()
    db.add_all(
        [AgentChildLink(agent_id=agent.id, child_agent_id=child_id, order_index=index) for index, child_id in enumerate(unique_children)]
    )
    db.add_all([AgentSkillLink(agent_id=agent.id, skill_id=skill_id) for skill_id in unique_skills])
    db.add_all([AgentKnowledgeBaseLink(agent_id=agent.id, knowledge_base_id=kb_id) for kb_id in unique_kbs])
    db.add_all([AgentDataAssetLink(agent_id=agent.id, data_asset_id=asset_id) for asset_id in unique_assets])


def would_create_agent_cycle(db: Session, agent_id: int, child_agent_ids: list[int]) -> bool:
    if not child_agent_ids:
        return False
    links = list(db.scalars(select(AgentChildLink)).all())
    graph: dict[int, list[int]] = {}
    for link in links:
        if link.agent_id != agent_id:
            graph.setdefault(link.agent_id, []).append(link.child_agent_id)
    graph[agent_id] = list(child_agent_ids)
    visiting: set[int] = set()
    visited: set[int] = set()

    def visit(node: int) -> bool:
        if node in visiting:
            return True
        if node in visited:
            return False
        visiting.add(node)
        if any(visit(child) for child in graph.get(node, [])):
            return True
        visiting.remove(node)
        visited.add(node)
        return False

    return visit(agent_id)


def build_knowledge_base(db: Session, knowledge_base: KnowledgeBase, max_documents: int = 2000) -> dict[str, int]:
    allowed_tables = set(knowledge_base.source_tables or ["stock_symbol", "stock_news", "stock_notice", "stock_financial_report"])
    documents: list[KnowledgeDocument] = []

    if "stock_symbol" in allowed_tables:
        rows = list(db.scalars(select(StockSymbol).order_by(StockSymbol.market, StockSymbol.symbol).limit(max_documents)).all())
        documents.extend(
            KnowledgeDocument(
                knowledge_base_id=knowledge_base.id,
                source_table="stock_symbol",
                source_record_id=row.id,
                market=row.market,
                symbol=row.symbol,
                title=f"{row.symbol} {row.name}",
                content=f"股票代码：{row.symbol}\n股票名称：{row.name}\n市场：{row.market}\n交易所：{row.exchange}\n资产类型：{row.asset_type}\n状态：{row.status}",
                metadata_json={"name": row.name, "exchange": row.exchange, "asset_type": row.asset_type},
            )
            for row in rows
        )
    if "stock_news" in allowed_tables:
        rows = list(db.scalars(select(StockNews).order_by(StockNews.news_time.desc()).limit(max_documents)).all())
        documents.extend(
            KnowledgeDocument(
                knowledge_base_id=knowledge_base.id,
                source_table="stock_news",
                source_record_id=row.id,
                market=row.market,
                symbol=row.symbol,
                title=row.title,
                content=row.content or row.title,
                metadata_json={"news_time": row.news_time, "source_name": row.source_name, "url": row.url},
            )
            for row in rows
        )
    if "stock_notice" in allowed_tables:
        rows = list(db.scalars(select(StockNotice).order_by(StockNotice.notice_date.desc()).limit(max_documents)).all())
        documents.extend(
            KnowledgeDocument(
                knowledge_base_id=knowledge_base.id,
                source_table="stock_notice",
                source_record_id=row.id,
                market=row.market,
                symbol=row.symbol,
                title=row.title,
                content=f"公告日期：{row.notice_date}\n公告类型：{row.notice_type or '--'}\n{row.content_json}",
                metadata_json={"notice_date": row.notice_date, "notice_type": row.notice_type, "url": row.url},
            )
            for row in rows
        )
    if "stock_financial_report" in allowed_tables:
        rows = list(
            db.scalars(
                select(StockFinancialReport)
                .order_by(StockFinancialReport.report_period.desc())
                .limit(max_documents)
            ).all()
        )
        documents.extend(
            KnowledgeDocument(
                knowledge_base_id=knowledge_base.id,
                source_table="stock_financial_report",
                source_record_id=row.id,
                market=row.market,
                symbol=row.symbol,
                title=f"{row.symbol} {row.report_period} {row.indicator}",
                content=f"报告期：{row.report_period}\n指标：{row.indicator}\n币种：{row.currency or '--'}\n数据：{row.data_json}",
                metadata_json={"report_period": row.report_period, "indicator": row.indicator, "currency": row.currency},
            )
            for row in rows
        )

    db.execute(delete(KnowledgeRelation).where(KnowledgeRelation.knowledge_base_id == knowledge_base.id))
    db.execute(delete(KnowledgeEntity).where(KnowledgeEntity.knowledge_base_id == knowledge_base.id))
    db.execute(delete(KnowledgeDocument).where(KnowledgeDocument.knowledge_base_id == knowledge_base.id))
    db.flush()
    db.add_all(documents)
    db.flush()

    entities: dict[tuple[str, str], KnowledgeEntity] = {}
    for document in documents:
        if not document.symbol:
            continue
        key = ("STOCK", f"{document.market}:{document.symbol}")
        entity = entities.get(key)
        if entity is None:
            entity = KnowledgeEntity(
                knowledge_base_id=knowledge_base.id,
                entity_type="STOCK",
                entity_key=key[1],
                entity_name=document.symbol,
                properties_json={"market": document.market, "symbol": document.symbol},
            )
            entities[key] = entity
            db.add(entity)
    db.flush()

    relations: list[KnowledgeRelation] = []
    for document in documents:
        if not document.symbol:
            continue
        entity = entities.get(("STOCK", f"{document.market}:{document.symbol}"))
        if entity is None:
            continue
        relations.append(
            KnowledgeRelation(
                knowledge_base_id=knowledge_base.id,
                subject_entity_id=entity.id,
                predicate={
                    "stock_news": "HAS_NEWS",
                    "stock_notice": "HAS_NOTICE",
                    "stock_financial_report": "HAS_FINANCIAL_REPORT",
                    "stock_symbol": "DESCRIBED_BY",
                }.get(document.source_table, "HAS_DOCUMENT"),
                object_entity_id=entity.id,
                evidence_document_id=document.id,
            )
        )
    db.add_all(relations)
    knowledge_base.entity_count = len(entities)
    knowledge_base.relation_count = len(relations)
    knowledge_base.status = "READY"
    db.commit()
    return {
        "documents_created": len(documents),
        "entities_created": len(entities),
        "relations_created": len(relations),
    }

