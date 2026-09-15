from __future__ import annotations

import json
import re
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
    AgentDataSourceLink,
    AgentDefinition,
    AgentKnowledgeBaseLink,
    AgentSkillLink,
    KnowledgeBase,
    KnowledgeGraph,
    KnowledgeDocument,
    KnowledgeEntity,
    KnowledgeRelation,
    ModelSkill,
    ResearchReportRecord,
)
from app.models.market_data import (
    DataSource,
    StockF10Cache,
    StockFinancialReport,
    StockKline,
    StockNews,
    StockNotice,
    StockRealtimeQuote,
    StockSymbol,
)
from app.schemas.distillation import DistillationOutput


DEFAULT_DATA_ASSETS = (
    ("STOCK_SYMBOL", "stock_symbol", "DW基础数据：股票主数据", "股票代码、名称、市场、交易所、资产类型和上市状态，是单股知识图谱的主实体来源。"),
    ("STOCK_QUOTE", "stock_realtime_quote", "DW股价：实时行情", "股票最新价格、涨跌幅、开高低收、成交量、成交额和换手率，用于短线预警。"),
    ("STOCK_KLINE", "stock_kline", "DW股价/交易量：历史K线", "股票日线 OHLC、成交量、成交额、换手率和均线计算基础，用于量价研判。"),
    ("STOCK_FINANCIAL", "stock_financial_report", "DW财报：财务报告", "F10 财务指标、报告期、币种和原始财报字段，用于盈利质量与经营现状分析。"),
    ("STOCK_NOTICE", "stock_notice", "DW公告：公司公告", "上市公司及挂牌公司公告、公告类型、发布时间和来源链接，用于事件驱动和风险识别。"),
    ("STOCK_NEWS", "stock_news", "DW新闻：新闻资讯", "股票相关新闻、来源、情绪、摘要和发布时间，用于舆情与外部催化分析。"),
    ("STOCK_F10", "stock_f10_cache", "DW股东/业务：F10资料", "公司简介、股东、基金流、业务构成、财务摘要和财务报表等 F10 分区缓存。"),
    ("RESEARCH_REPORT", "research_report", "DW研究：AI研报", "AI 生成研报、历史结论、评分、模型信息、知识库引用和复盘结果。"),
    ("WATCHLIST", "watchlist_item", "DW用户：自选股", "用户自选股票清单和研究关注标记。"),
    ("DATA_SYNC_LOG", "data_sync_log", "治理日志：批量同步", "批量数据同步状态、统计和异常信息。"),
    ("DATA_FETCH_LOG", "data_fetch_log", "治理日志：按需获取", "单股票按需数据获取记录、来源接口和入库统计。"),
)

DEFAULT_KNOWLEDGE_BASES = (
    {
        "kb_code": "STOCK_FULL_KG",
        "kb_name": "单股全覆盖知识图谱",
        "description": "围绕单只股票整合基础数据、新闻、公告、财报、股价、交易量、股东/F10 和 AI 研报，形成可追溯实体关系网络。",
        "source_tables": [
            "stock_symbol",
            "stock_news",
            "stock_notice",
            "stock_financial_report",
            "stock_kline",
            "stock_realtime_quote",
            "stock_f10_cache",
            "research_report",
        ],
        "version": "1.0.0",
        "enabled": True,
    },
)


def is_business_table(table_name: str) -> bool:
    """Allow new local business tables while excluding credentials and control metadata."""
    return bool(re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*", table_name)) and not table_name.startswith(
        ("agent_", "model_", "knowledge_", "governance_", "sqlite_")
    ) and table_name not in {"data_source", "data_interface"}


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
                governance_status="PENDING",
                enabled=True,
            )
            db.add(asset)
        else:
            asset.display_name = asset.display_name or display_name
            asset.description = asset.description or description
            if asset.governance_status in {"READY", "MISSING"}:
                asset.governance_status = "PENDING"
    db.commit()


def seed_default_knowledge_bases(db: Session) -> None:
    """Seed the stock knowledge graph catalog without rebuilding user data."""

    for kb_config in DEFAULT_KNOWLEDGE_BASES:
        kb = db.scalar(select(KnowledgeBase).where(KnowledgeBase.kb_code == kb_config["kb_code"]))
        if kb is None:
            kb = KnowledgeBase(
                kb_code=str(kb_config["kb_code"]),
                kb_name=str(kb_config["kb_name"]),
                description=str(kb_config["description"]),
                source_tables=list(kb_config["source_tables"]),
                version=str(kb_config["version"]),
                enabled=bool(kb_config["enabled"]),
                status="DRAFT",
            )
            db.add(kb)
        else:
            kb.kb_name = kb.kb_name or str(kb_config["kb_name"])
            kb.description = kb.description or str(kb_config["description"])
            kb.version = kb.version or str(kb_config["version"])
    db.commit()


def seed_default_graphs(db: Session) -> None:
    """Give each legacy knowledge base an independent graph and retain its existing rows."""
    for kb in db.scalars(select(KnowledgeBase)).all():
        graph = db.scalar(select(KnowledgeGraph).where(KnowledgeGraph.graph_code == kb.kb_code))
        if graph is None:
            has_legacy_rows = db.scalar(select(KnowledgeDocument.id).where(
                KnowledgeDocument.knowledge_base_id == kb.id, KnowledgeDocument.graph_id.is_(None)
            ).limit(1)) is not None
            if kb.kb_code != "STOCK_FULL_KG" and not has_legacy_rows:
                continue
            graph = KnowledgeGraph(
                knowledge_base_id=kb.id, graph_code=kb.kb_code,
                graph_name=kb.kb_name, description=kb.description,
                source_tables=list(kb.source_tables or []), version=kb.version,
                enabled=kb.enabled, entity_count=kb.entity_count,
                relation_count=kb.relation_count,
                governance_status="GOVERNED" if kb.status == "READY" else "PENDING",
                last_governed_at=kb.updated_at if kb.status == "READY" else None,
            )
            db.add(graph)
            db.flush()
        for model in (KnowledgeDocument, KnowledgeEntity, KnowledgeRelation):
            db.query(model).filter(model.knowledge_base_id == kb.id, model.graph_id.is_(None)).update(
                {model.graph_id: graph.id}, synchronize_session=False
            )
    db.commit()
    db.expire_all()


DEFAULT_AGENTS = (
    {
        "agent_code": "DISTILLATION_GOVERNANCE_AGENT",
        "display_name": "双轨蒸馏调度智能体",
        "system_prompt": "你是数据蒸馏调度官。仅处理工具按单只股票和知识文档读取的业务原文。先由 Python 分块，再调用 ONDEMAND_DATA_DISTILLER；严格验证 JSON、实体类型、来源证据和知识图谱关系白名单。KB 与 KG 各自记录成功或失败。置信度低于 0.8、无法核验的新节点或冲突事实写入隔离的 DISTILLATION_REVIEW 草稿，等待人工审核，不得自动写图，也不得作为 Skill Prompt 修订批准。",
        "model_instance_code": "DEEPSEEK_CHAT",
        "max_iterations": 6,
        "context_window_limit": 4,
        "json_schema_output": DistillationOutput.model_json_schema(),
        "description": "按需业务数据蒸馏的待启用编排配置；启用前需部署任务执行器、向量库与 Neo4j。",
        "enabled": False,
        "skill_codes": ["ONDEMAND_DATA_DISTILLER", "ONDEMAND_DATA_DISTILLER_TOOL"],
        "kb_codes": ["STOCK_FULL_KG"],
        "asset_codes": ["STOCK_NEWS", "STOCK_NOTICE", "STOCK_FINANCIAL"],
    },
    {
        "agent_code": "DATA_GOVERNANCE_AGENT",
        "display_name": "数据治理智能体",
        "system_prompt": "负责把股票基础数据、新闻、公告、财报、股价、交易量、股东和 F10 数据治理成 DW 数据层，输出分类、质量、入库和待补数据建议。",
        "model_instance_code": "DEEPSEEK_V4_FLASH",
        "max_iterations": 8,
        "description": "用于研究中心对话页签的数据治理、数据分类、字段解释和质量检查。",
        "skill_codes": ["DATA_GOVERNANCE_DW", "DATA_CLEANING_DW"],
        "kb_codes": ["STOCK_FULL_KG"],
        "asset_codes": ["STOCK_SYMBOL", "STOCK_QUOTE", "STOCK_KLINE", "STOCK_FINANCIAL", "STOCK_NOTICE", "STOCK_NEWS", "STOCK_F10", "RESEARCH_REPORT"],
    },
    {
        "agent_code": "KNOWLEDGE_GRAPH_AGENT",
        "display_name": "知识图谱智能体",
        "system_prompt": "以单只股票为对象建立全覆盖知识图谱，抽取公司、股东、业务、财报、公告、新闻、价格、交易量和研报之间的可追溯关系。",
        "model_instance_code": "GEMINI_FLASH",
        "max_iterations": 8,
        "description": "用于知识库构建、实体关系抽取和证据链归档。",
        "skill_codes": ["STOCK_KNOWLEDGE_GRAPH_BUILDER"],
        "kb_codes": ["STOCK_FULL_KG"],
        "asset_codes": ["STOCK_SYMBOL", "STOCK_QUOTE", "STOCK_KLINE", "STOCK_FINANCIAL", "STOCK_NOTICE", "STOCK_NEWS", "STOCK_F10", "RESEARCH_REPORT"],
    },
    {
        "agent_code": "QA_QUERY_AGENT",
        "display_name": "问答问数智能体",
        "system_prompt": "负责研究中心对话页签的问答问数、治理解释和分析问答，优先使用内部数据源和知识图谱，输出统计口径和证据。",
        "model_instance_code": "DEEPSEEK_V4_FLASH",
        "max_iterations": 6,
        "description": "用于数据治理、分析、问数、问答等日常交互操作。",
        "skill_codes": ["STOCK_QA_QUERY", "DATA_GOVERNANCE_DW"],
        "kb_codes": ["STOCK_FULL_KG"],
        "asset_codes": ["STOCK_SYMBOL", "STOCK_QUOTE", "STOCK_KLINE", "STOCK_FINANCIAL", "STOCK_NOTICE", "STOCK_NEWS", "STOCK_F10", "RESEARCH_REPORT"],
    },
    {
        "agent_code": "STOCK_SELECTION_AGENT",
        "display_name": "分析选股智能体",
        "system_prompt": "读取内部数据源、知识图谱和可验证外部信息，分析基本面、业务布局、发展前景、经营现状、量价操作和交易量，给出短中长线结论。",
        "model_instance_code": "DEEPSEEK_CHAT",
        "max_iterations": 10,
        "description": "用于分析选股、趋势研判、短中长线操作结论和风险条件。",
        "skill_codes": ["STOCK_SELECTION_ANALYST", "STOCK_TREND_ADVISOR", "RESEARCH_REPORT_REVIEW", "RESEARCH_REVIEW_CORRECTION"],
        "kb_codes": ["STOCK_FULL_KG"],
        "asset_codes": ["STOCK_SYMBOL", "STOCK_QUOTE", "STOCK_KLINE", "STOCK_FINANCIAL", "STOCK_NOTICE", "STOCK_NEWS", "STOCK_F10", "RESEARCH_REPORT"],
    },
    {
        "agent_code": "RESEARCH_REPORT_AGENT",
        "display_name": "研报生成智能体",
        "system_prompt": "对指定股票、数据源和知识库进行证据归纳，生成 AI 研究报告，并对历史研报进行预测准确性复盘和结论修正。",
        "model_instance_code": "DEEPSEEK_CHAT",
        "max_iterations": 10,
        "description": "用于研究中心研究页签，生成并保存研报、比对历史研报。",
        "skill_codes": ["STOCK_SELECTION_ANALYST", "STOCK_TREND_ADVISOR", "RESEARCH_REPORT_REVIEW", "RESEARCH_REVIEW_CORRECTION"],
        "kb_codes": ["STOCK_FULL_KG"],
        "asset_codes": ["STOCK_SYMBOL", "STOCK_QUOTE", "STOCK_KLINE", "STOCK_FINANCIAL", "STOCK_NOTICE", "STOCK_NEWS", "STOCK_F10", "RESEARCH_REPORT"],
    },
    {
        "agent_code": "RISK_WARNING_AGENT",
        "display_name": "预警智能体",
        "system_prompt": "监控公告、新闻、财报、股价、成交量、股东和研报结论变化，识别量价失效、负面事件、经营恶化和历史判断偏差。",
        "model_instance_code": "DEEPSEEK_CHAT",
        "max_iterations": 6,
        "description": "用于预警判断、历史研报偏差提示和风险解释。",
        "skill_codes": ["STOCK_TREND_ADVISOR", "RESEARCH_REPORT_REVIEW", "WATCH_ALERT"],
        "kb_codes": ["STOCK_FULL_KG"],
        "asset_codes": ["STOCK_QUOTE", "STOCK_KLINE", "STOCK_NOTICE", "STOCK_NEWS", "STOCK_FINANCIAL", "STOCK_F10", "RESEARCH_REPORT"],
    },
)


def seed_default_agents(db: Session) -> None:
    """Configure the built-in stock research agents and bind their resources."""
    created_codes: set[str] = set()
    for agent_config in DEFAULT_AGENTS:
        agent = db.scalar(select(AgentDefinition).where(AgentDefinition.agent_code == agent_config["agent_code"]))
        if agent is None:
            created_codes.add(str(agent_config["agent_code"]))
            agent = AgentDefinition(
                agent_code=str(agent_config["agent_code"]),
                display_name=str(agent_config["display_name"]),
                system_prompt=str(agent_config["system_prompt"]),
                model_instance_code=str(agent_config["model_instance_code"]),
                max_iterations=int(agent_config["max_iterations"]),
                context_window_limit=int(agent_config.get("context_window_limit", 12)),
                json_schema_output=dict(agent_config.get("json_schema_output") or {}),
                enabled=bool(agent_config.get("enabled", True)),
                description=str(agent_config["description"]),
                version="1.0.1" if agent_config["agent_code"] in {"DATA_GOVERNANCE_AGENT", "QA_QUERY_AGENT"} else "1.0.0",
            )
            db.add(agent)
        elif agent_config["agent_code"] in {"DATA_GOVERNANCE_AGENT", "QA_QUERY_AGENT"} and agent.version == "1.0.0":
            if agent.model_instance_code == "QWEN_PLUS":
                agent.model_instance_code = "DEEPSEEK_V4_FLASH"
            if agent.model_instance_code == "DEEPSEEK_V4_FLASH":
                agent.version = "1.0.1"
        db.flush()

    skill_by_code = {
        item.skill_code: item.id for item in db.scalars(select(ModelSkill).where(ModelSkill.enabled.is_(True))).all()
    }
    kb_by_code = {
        item.kb_code: item.id for item in db.scalars(select(KnowledgeBase).where(KnowledgeBase.enabled.is_(True))).all()
    }
    asset_by_code = {
        item.asset_code: item.id for item in db.scalars(select(AgentDataAsset).where(AgentDataAsset.enabled.is_(True))).all()
    }

    for agent_config in DEFAULT_AGENTS:
        if agent_config["agent_code"] not in created_codes:
            continue
        agent = db.scalar(select(AgentDefinition).where(AgentDefinition.agent_code == agent_config["agent_code"]))
        if agent is None:
            continue
        skill_ids = [skill_by_code[code] for code in agent_config["skill_codes"] if code in skill_by_code]
        kb_ids = [kb_by_code[code] for code in agent_config["kb_codes"] if code in kb_by_code]
        asset_ids = [asset_by_code[code] for code in agent_config["asset_codes"] if code in asset_by_code]
        set_agent_links(db, agent, [], skill_ids, kb_ids, asset_ids)
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
        asset.source_health = "MISSING"
        asset.last_inspected_at = datetime.now(timezone.utc)
        return asset
    with db_engine.connect() as connection:
        asset.row_count = int(connection.execute(text(f'SELECT COUNT(*) FROM "{asset.table_name}"')).scalar_one())
    if not asset.allowed_columns:
        asset.allowed_columns = columns
    asset.source_health = "SCHEMA_CHANGED" if any(column not in columns for column in asset.allowed_columns) else "READY"
    asset.last_inspected_at = datetime.now(timezone.utc)
    return asset


def set_agent_links(
    db: Session,
    agent: AgentDefinition,
    child_agent_ids: list[int],
    skill_ids: list[int],
    knowledge_base_ids: list[int],
    data_asset_ids: list[int],
    data_source_ids: list[int] | None = None,
) -> None:
    unique_children = list(dict.fromkeys(child_agent_ids))
    unique_skills = list(dict.fromkeys(skill_ids))
    unique_kbs = list(dict.fromkeys(knowledge_base_ids))
    unique_assets = list(dict.fromkeys(data_asset_ids))
    unique_sources = list(dict.fromkeys(data_source_ids or []))
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
    if unique_sources:
        count = db.scalar(select(func.count(DataSource.id)).where(DataSource.id.in_(unique_sources))) or 0
        if int(count) != len(unique_sources):
            raise ValueError("One or more data sources do not exist")

    db.execute(delete(AgentChildLink).where(AgentChildLink.agent_id == agent.id))
    db.execute(delete(AgentSkillLink).where(AgentSkillLink.agent_id == agent.id))
    db.execute(delete(AgentKnowledgeBaseLink).where(AgentKnowledgeBaseLink.agent_id == agent.id))
    db.execute(delete(AgentDataAssetLink).where(AgentDataAssetLink.agent_id == agent.id))
    db.execute(delete(AgentDataSourceLink).where(AgentDataSourceLink.agent_id == agent.id))
    db.flush()
    db.add_all(
        [AgentChildLink(agent_id=agent.id, child_agent_id=child_id, order_index=index) for index, child_id in enumerate(unique_children)]
    )
    db.add_all([AgentSkillLink(agent_id=agent.id, skill_id=skill_id) for skill_id in unique_skills])
    db.add_all([AgentKnowledgeBaseLink(agent_id=agent.id, knowledge_base_id=kb_id) for kb_id in unique_kbs])
    db.add_all([AgentDataAssetLink(agent_id=agent.id, data_asset_id=asset_id) for asset_id in unique_assets])
    db.add_all([AgentDataSourceLink(agent_id=agent.id, data_source_id=source_id) for source_id in unique_sources])


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


def build_knowledge_graph(db: Session, graph: KnowledgeGraph, max_documents: int = 100000) -> dict[str, int]:
    knowledge_base = db.get(KnowledgeBase, graph.knowledge_base_id)
    if knowledge_base is None:
        raise ValueError("Knowledge base not found")
    allowed_tables = set(graph.source_tables or knowledge_base.source_tables or [])
    if not allowed_tables:
        raise ValueError("Choose source tables before building a graph")
    symbol_filter = graph.symbol.strip() if graph.symbol else None
    documents: list[KnowledgeDocument] = []

    if "stock_symbol" in allowed_tables:
        query = select(StockSymbol)
        if symbol_filter:
            query = query.where(StockSymbol.symbol == symbol_filter)
        rows = list(db.scalars(query.order_by(StockSymbol.market, StockSymbol.symbol).limit(max_documents)).all())
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
        query = select(StockNews)
        if symbol_filter:
            query = query.where(StockNews.symbol == symbol_filter)
        rows = list(db.scalars(query.order_by(StockNews.news_time.desc()).limit(max_documents)).all())
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
        query = select(StockNotice)
        if symbol_filter:
            query = query.where(StockNotice.symbol == symbol_filter)
        rows = list(db.scalars(query.order_by(StockNotice.notice_date.desc()).limit(max_documents)).all())
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
                (select(StockFinancialReport).where(StockFinancialReport.symbol == symbol_filter) if symbol_filter else select(StockFinancialReport))
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
    if "stock_kline" in allowed_tables:
        query = select(StockKline)
        if symbol_filter:
            query = query.where(StockKline.symbol == symbol_filter)
        rows = list(db.scalars(query.order_by(StockKline.trade_date.desc()).limit(max_documents)).all())
        documents.extend(
            KnowledgeDocument(
                knowledge_base_id=knowledge_base.id,
                source_table="stock_kline",
                source_record_id=row.id,
                market=row.market,
                symbol=row.symbol,
                title=f"{row.symbol} {row.trade_date} K线与交易量",
                content=(
                    f"交易日期：{row.trade_date}\n周期：{row.period}\n复权：{row.adjust or '--'}\n"
                    f"开盘：{row.open_price}，最高：{row.high_price}，最低：{row.low_price}，收盘：{row.close_price}\n"
                    f"成交量：{row.volume}，成交额：{row.amount}，换手率：{row.turnover_rate}"
                ),
                metadata_json={"trade_date": row.trade_date, "period": row.period, "adjust": row.adjust},
            )
            for row in rows
        )
    if "stock_realtime_quote" in allowed_tables:
        query = select(StockRealtimeQuote)
        if symbol_filter:
            query = query.where(StockRealtimeQuote.symbol == symbol_filter)
        rows = list(db.scalars(query.order_by(StockRealtimeQuote.fetched_at.desc()).limit(max_documents)).all())
        documents.extend(
            KnowledgeDocument(
                knowledge_base_id=knowledge_base.id,
                source_table="stock_realtime_quote",
                source_record_id=row.id,
                market=row.market,
                symbol=row.symbol,
                title=f"{row.symbol} 实时行情",
                content=(
                    f"行情时间：{row.quote_time or '--'}\n最新价：{row.current_price}\n涨跌额：{row.change_amount}\n"
                    f"涨跌幅：{row.change_pct}\n成交量：{row.volume}\n成交额：{row.amount}\n换手率：{row.turnover_rate}"
                ),
                metadata_json={"quote_time": row.quote_time, "fetched_at": row.fetched_at.isoformat() if row.fetched_at else None},
            )
            for row in rows
        )
    if "stock_f10_cache" in allowed_tables:
        query = select(StockF10Cache)
        if symbol_filter:
            query = query.where(StockF10Cache.symbol == symbol_filter)
        rows = list(db.scalars(query.order_by(StockF10Cache.fetched_at.desc()).limit(max_documents)).all())
        documents.extend(
            KnowledgeDocument(
                knowledge_base_id=knowledge_base.id,
                source_table="stock_f10_cache",
                source_record_id=row.id,
                market=row.market,
                symbol=row.symbol,
                title=f"{row.symbol} F10 {row.section}",
                content=f"F10 分区：{row.section}\n数据：{row.payload_json}",
                metadata_json={"section": row.section, "fetched_at": row.fetched_at.isoformat() if row.fetched_at else None},
            )
            for row in rows
        )
    if "research_report" in allowed_tables:
        query = select(ResearchReportRecord)
        if symbol_filter:
            query = query.where(ResearchReportRecord.symbol == symbol_filter)
        rows = list(db.scalars(query.order_by(ResearchReportRecord.created_at.desc()).limit(max_documents)).all())
        documents.extend(
            KnowledgeDocument(
                knowledge_base_id=knowledge_base.id,
                source_table="research_report",
                source_record_id=row.id,
                market=row.market,
                symbol=row.symbol,
                title=row.title,
                content=row.report_markdown,
                metadata_json={
                    "rating": row.rating,
                    "score": row.score,
                    "created_at": row.created_at.isoformat() if row.created_at else None,
                    "history_evaluation": row.history_evaluation_json,
                },
            )
            for row in rows
        )

    supported = {
        "stock_symbol", "stock_news", "stock_notice", "stock_financial_report",
        "stock_kline", "stock_realtime_quote", "stock_f10_cache", "research_report",
    }
    db_engine = db.get_bind()
    available_tables = set(inspect(db_engine).get_table_names())
    quote = db_engine.dialect.identifier_preparer.quote
    for table_name in sorted(allowed_tables - supported):
        if table_name not in available_tables or not is_business_table(table_name):
            raise ValueError(f"Unsupported graph source: {table_name}")
        columns = {str(item["name"]) for item in inspect(db_engine).get_columns(table_name)}
        where = " WHERE symbol = :symbol" if symbol_filter and "symbol" in columns else ""
        if symbol_filter and "symbol" not in columns:
            continue
        query = text(f'SELECT * FROM {quote(table_name)}{where} LIMIT :limit')
        parameters = {"limit": max_documents, "symbol": symbol_filter}
        rows = list(db.execute(query, parameters).mappings().all())
        for raw in rows:
            row = dict(raw)
            symbol = str(row.get("symbol") or "") or None
            record_id = row.get("id")
            documents.append(KnowledgeDocument(
                knowledge_base_id=knowledge_base.id,
                source_table=table_name,
                source_record_id=int(record_id) if isinstance(record_id, int) else None,
                market=str(row.get("market") or "") or None,
                symbol=symbol,
                title=str(row.get("title") or row.get("name") or f"{table_name} {symbol or record_id or ''}")[:512],
                content=json.dumps(row, ensure_ascii=False, default=str)[:6000],
                metadata_json={"source_table": table_name, "source_record_id": record_id},
            ))

    db.execute(delete(KnowledgeRelation).where(KnowledgeRelation.graph_id == graph.id))
    db.execute(delete(KnowledgeEntity).where(KnowledgeEntity.graph_id == graph.id))
    db.execute(delete(KnowledgeDocument).where(KnowledgeDocument.graph_id == graph.id))
    db.flush()
    for document in documents:
        document.graph_id = graph.id
    db.add_all(documents)
    db.flush()

    entities: dict[tuple[str, str], KnowledgeEntity] = {}
    document_entities: dict[int, KnowledgeEntity] = {}
    for document in documents:
        source_key = ("DATASET", document.source_table)
        if source_key not in entities:
            source_entity = KnowledgeEntity(
                knowledge_base_id=knowledge_base.id,
                graph_id=graph.id,
                entity_type="DATASET",
                entity_key=f"{graph.id}:dataset:{document.source_table}",
                entity_name=document.source_table,
                properties_json={"source_table": document.source_table},
            )
            entities[source_key] = source_entity
            db.add(source_entity)
        if document.symbol:
            key = ("STOCK", f"{document.market}:{document.symbol}")
            if key not in entities:
                entity = KnowledgeEntity(
                    knowledge_base_id=knowledge_base.id,
                    graph_id=graph.id,
                    entity_type="STOCK",
                    entity_key=f"{graph.id}:{key[1]}",
                    entity_name=document.symbol,
                    properties_json={"market": document.market, "symbol": document.symbol},
                )
                entities[key] = entity
                db.add(entity)
        document_key = f"{graph.id}:{document.source_table}:{document.source_record_id or document.id}:{document.symbol}"
        doc_entity = KnowledgeEntity(
            knowledge_base_id=knowledge_base.id,
            graph_id=graph.id,
            entity_type={
                "stock_symbol": "COMPANY_PROFILE",
                "stock_news": "NEWS",
                "stock_notice": "NOTICE",
                "stock_financial_report": "FINANCIAL_REPORT",
                "stock_kline": "PRICE_SERIES",
                "stock_realtime_quote": "REALTIME_QUOTE",
                "stock_f10_cache": "F10_SECTION",
                "research_report": "RESEARCH_REPORT",
            }.get(document.source_table, "DOCUMENT"),
            entity_key=document_key[:256],
            entity_name=document.title[:256],
            properties_json={
                "market": document.market,
                "symbol": document.symbol,
                "source_table": document.source_table,
                "source_record_id": document.source_record_id,
            },
        )
        document_entities[document.id] = doc_entity
        db.add(doc_entity)
    db.flush()

    relations: list[KnowledgeRelation] = []
    for document in documents:
        doc_entity = document_entities.get(document.id)
        source_entity = entities.get(("DATASET", document.source_table))
        if doc_entity is None or source_entity is None:
            continue
        relations.append(KnowledgeRelation(
            knowledge_base_id=knowledge_base.id, graph_id=graph.id,
            subject_entity_id=source_entity.id, predicate="HAS_RECORD",
            object_entity_id=doc_entity.id, evidence_document_id=document.id,
        ))
        if not document.symbol:
            continue
        entity = entities.get(("STOCK", f"{document.market}:{document.symbol}"))
        if entity is None:
            continue
        relations.append(
            KnowledgeRelation(
                knowledge_base_id=knowledge_base.id,
                graph_id=graph.id,
                subject_entity_id=entity.id,
                predicate={
                    "stock_news": "HAS_NEWS",
                    "stock_notice": "HAS_NOTICE",
                    "stock_financial_report": "HAS_FINANCIAL_REPORT",
                    "stock_kline": "HAS_PRICE_AND_VOLUME_SERIES",
                    "stock_realtime_quote": "HAS_REALTIME_QUOTE",
                    "stock_f10_cache": "HAS_F10_SECTION",
                    "research_report": "HAS_RESEARCH_REPORT",
                    "stock_symbol": "DESCRIBED_BY",
                }.get(document.source_table, "HAS_DOCUMENT"),
                object_entity_id=doc_entity.id,
                evidence_document_id=document.id,
            )
        )
    db.add_all(relations)
    db.flush()
    graph.entity_count = len(entities) + len(document_entities)
    graph.relation_count = len(relations)
    knowledge_base.entity_count = int(db.scalar(select(func.count(KnowledgeEntity.id)).where(KnowledgeEntity.knowledge_base_id == knowledge_base.id)) or 0)
    knowledge_base.relation_count = int(db.scalar(select(func.count(KnowledgeRelation.id)).where(KnowledgeRelation.knowledge_base_id == knowledge_base.id)) or 0)
    knowledge_base.status = "READY"
    db.flush()
    return {
        "documents_created": len(documents),
        "entities_created": len(entities),
        "relations_created": len(relations),
    }


def build_knowledge_base(db: Session, knowledge_base: KnowledgeBase, max_documents: int = 100000) -> dict[str, int]:
    graph = db.scalar(select(KnowledgeGraph).where(KnowledgeGraph.graph_code == knowledge_base.kb_code))
    if graph is None:
        raise ValueError("Default knowledge graph not found")
    result = build_knowledge_graph(db, graph, max_documents)
    db.commit()
    return result
