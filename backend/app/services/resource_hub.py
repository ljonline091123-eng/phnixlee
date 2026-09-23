from __future__ import annotations

import hashlib
import json
import re
from datetime import datetime, timezone
from typing import Any

from sqlalchemy import delete, func, inspect, select, text, tuple_
from sqlalchemy.engine import Engine
from sqlalchemy.orm import Session, load_only

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
from app.models.foundation import FoundationEntity
from app.schemas.distillation import DistillationOutput
from app.services.graph_identity import normalize_pair, resolve_many


DEFAULT_DATA_ASSETS = (
    ("STOCK_SYMBOL", "stock_symbol", "DW基础数据：股票主数据", "股票代码、名称、市场、交易所、资产类型和上市状态，是单股知识图谱的主实体来源。"),
    ("STOCK_QUOTE", "stock_realtime_quote", "DW股价：实时行情", "股票最新价格、涨跌幅、开高低收、成交量、成交额和换手率，用于短线预警。"),
    ("STOCK_KLINE", "stock_kline", "DW股价/交易量：历史K线", "股票日线 OHLC、成交量、成交额、换手率和均线计算基础，用于量价研判。"),
    ("STOCK_FINANCIAL", "stock_financial_report", "DW财报：财务报告", "F10 财务指标、报告期、币种和原始财报字段，用于盈利质量与经营现状分析。"),
    ("STOCK_NOTICE", "stock_notice", "DW公告：公司公告", "上市公司及挂牌公司公告、公告类型、发布时间和来源链接，用于事件驱动和风险识别。"),
    ("STOCK_NEWS", "stock_news", "DW新闻：新闻资讯", "股票相关新闻、来源、情绪、摘要和发布时间，用于舆情与外部催化分析。"),
    ("STOCK_CONTEXT_EVENT", "stock_context_event", "External contextual evidence", "Policy, raw materials, supply chain, contracts and shareholder events with provenance."),
    ("STOCK_F10", "stock_f10_cache", "DW股东/业务：F10资料", "公司简介、股东、基金流、业务构成、财务摘要和财务报表等 F10 分区缓存。"),
    ("RESEARCH_REPORT", "research_report", "DW研究：AI研报", "AI 生成研报、历史结论、评分、模型信息、知识库引用和复盘结果。"),
    ("WATCHLIST", "watchlist_item", "DW用户：自选股", "用户自选股票清单和研究关注标记。"),
    ("DATA_SYNC_LOG", "data_sync_log", "治理日志：批量同步", "批量数据同步状态、统计和异常信息。"),
    ("DATA_FETCH_LOG", "data_fetch_log", "治理日志：按需获取", "单股票按需数据获取记录、来源接口和入库统计。"),
    ("FOUNDATION_ENTITY", "foundation_entity", "公司主数据：公司及其他主体", "公司、机构、股东账户及行业等主体身份，保留注册法域和外部标识；同名不自动合并。"),
    ("FOUNDATION_SECURITY", "foundation_security", "公司主数据：证券与发行公司", "独立证券身份、发行公司及股份类别，支持同一公司发行多只证券。"),
    ("FOUNDATION_LISTING", "foundation_listing", "公司主数据：股票上市映射", "连接股票代码、市场、证券及发行公司，并保存映射依据。"),
    ("FOUNDATION_FACT", "foundation_fact", "公司关系：股权、往来及司法事实", "股权、控制、供应链、经济往来和司法参与事实；审核状态、比例、金额及有效期以事实记录为准。"),
    ("FOUNDATION_EVIDENCE", "foundation_evidence", "公司关系：持久证据", "关系及主数据的来源原文、版本、发布时间和可获知时间，不随图谱重建删除。"),
    ("FOUNDATION_FACT_EVIDENCE", "foundation_fact_evidence", "公司关系：事实证据关联", "关联公司事实与对应的证据版本，支持一项事实使用多份证据。"),
    ("FOUNDATION_SOURCE_IDENTITY", "foundation_source_identity", "公司主数据：来源身份映射", "来源内稳定标识到公司及主体的映射，用于可追溯的跨来源身份统一。"),
    ("FOUNDATION_SECURITY_CLASSIFICATION", "foundation_security_classification", "分类主数据：证券分类事实", "证券的行业、主题、风格及规模分类，保留定义版本、证据和审核状态。"),
    ("FOUNDATION_COMPANY_MAPPING_STATE", "foundation_company_mapping_state", "公司主数据：股票公司映射覆盖", "记录各股票发行公司映射的结果和未覆盖原因，便于检查全市场覆盖情况。"),
    ("CLASSIFICATION_DEFINITION", "classification_definition", "分类主数据：行业与类型释义", "行业、主题、类型和规模标签的真实释义、纳入标准、来源和定义版本。"),
    ("LAKE_OBJECT", "lake_object", "湖仓：原始与标准化对象", "Raw、Normalized、Serving 对象的 URI、内容哈希、来源记录和数据集版本。"),
    ("LAKE_DATASET", "lake_dataset", "湖仓：数据集目录", "Parquet 数据集、分层、分区规范和当前版本。"),
    ("LAKE_LINEAGE", "lake_lineage_event", "湖仓：数据血缘", "来源表到数据集、文档、切片和服务层结果的转换批次与版本链路。"),
    ("DOCUMENT_CHUNKS", "document_chunk_version", "知识库：文档切片版本", "切片文本、原文偏移、内容哈希、解析器版本和 Embedding 模型版本。"),
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
            "stock_context_event",
            "research_report",
        ],
        "version": "1.0.0",
        "enabled": True,
    },
)


def is_business_table(table_name: str) -> bool:
    """Allow new local business tables while excluding credentials and control metadata."""
    if table_name.startswith("foundation_"):
        return table_name in {item[1] for item in DEFAULT_DATA_ASSETS if item[1].startswith("foundation_")}
    return bool(re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*", table_name)) and not table_name.startswith(
        ("agent_", "model_", "knowledge_", "governance_", "sqlite_", "pipeline_")
    ) and table_name not in {
        "data_source", "data_interface", "scheduled_job", "database_table_comment", "skill_optimization_draft",
    }


def seed_default_data_assets(db: Session) -> None:
    """Register the project's local tables as read-only Agent data assets."""
    for asset_code, table_name, display_name, description in DEFAULT_DATA_ASSETS:
        # A user may already have registered the same business table under a
        # custom code. Reuse it instead of violating the unique table mapping.
        asset = db.scalar(select(AgentDataAsset).where(
            (AgentDataAsset.asset_code == asset_code) | (AgentDataAsset.table_name == table_name)
        ))
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
            # Upgrade only the built-in full graph.  User-created knowledge bases
            # keep their deliberate source boundary unchanged.
            if kb.kb_code == "STOCK_FULL_KG" and "stock_context_event" not in (kb.source_tables or []):
                kb.source_tables = [*(kb.source_tables or []), "stock_context_event"]
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
        elif kb.kb_code == "STOCK_FULL_KG" and "stock_context_event" not in (graph.source_tables or []):
            graph.source_tables = [*(graph.source_tables or []), "stock_context_event"]
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
        "asset_codes": ["STOCK_SYMBOL", "STOCK_QUOTE", "STOCK_KLINE", "STOCK_FINANCIAL", "STOCK_NOTICE", "STOCK_NEWS", "STOCK_F10", "STOCK_CONTEXT_EVENT", "RESEARCH_REPORT"],
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
        "asset_codes": ["STOCK_SYMBOL", "STOCK_QUOTE", "STOCK_KLINE", "STOCK_FINANCIAL", "STOCK_NOTICE", "STOCK_NEWS", "STOCK_F10", "STOCK_CONTEXT_EVENT", "RESEARCH_REPORT"],
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
        "asset_codes": ["STOCK_SYMBOL", "STOCK_QUOTE", "STOCK_KLINE", "STOCK_FINANCIAL", "STOCK_NOTICE", "STOCK_NEWS", "STOCK_F10", "STOCK_CONTEXT_EVENT", "RESEARCH_REPORT"],
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
        "asset_codes": ["STOCK_SYMBOL", "STOCK_QUOTE", "STOCK_KLINE", "STOCK_FINANCIAL", "STOCK_NOTICE", "STOCK_NEWS", "STOCK_F10", "STOCK_CONTEXT_EVENT", "RESEARCH_REPORT"],
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
        "asset_codes": ["STOCK_SYMBOL", "STOCK_QUOTE", "STOCK_KLINE", "STOCK_FINANCIAL", "STOCK_NOTICE", "STOCK_NEWS", "STOCK_F10", "STOCK_CONTEXT_EVENT", "RESEARCH_REPORT"],
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
        "asset_codes": ["STOCK_QUOTE", "STOCK_KLINE", "STOCK_NOTICE", "STOCK_NEWS", "STOCK_FINANCIAL", "STOCK_F10", "STOCK_CONTEXT_EVENT", "RESEARCH_REPORT"],
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


def build_knowledge_graph(db: Session, graph: KnowledgeGraph, max_documents: int = 100000, *,
                          source_columns: dict[str, list[str]] | None = None,
                          enrich_company_identity: bool = True,
                          scope_pairs: list[tuple[str, str]] | None = None,
                          max_documents_per_stock: int | None = None) -> dict[str, int]:
    knowledge_base = db.get(KnowledgeBase, graph.knowledge_base_id)
    if knowledge_base is None:
        raise ValueError("Knowledge base not found")
    allowed_tables = set(graph.source_tables or knowledge_base.source_tables or [])
    if not allowed_tables:
        raise ValueError("Choose source tables before building a graph")
    symbol_filter = graph.symbol.strip() if graph.symbol else None
    normalized_scope = sorted({pair for market, symbol in (scope_pairs or [])
                               if (pair := normalize_pair(market, symbol))})
    documents: list[KnowledgeDocument] = []
    # Keep per-source accounting alongside the materialized documents.  A graph is
    # only considered complete when every configured feed is available and non-empty.
    raw_counts: dict[str, int] = {name: 0 for name in sorted(allowed_tables)}
    duplicate_counts: dict[str, int] = {name: 0 for name in sorted(allowed_tables)}
    unavailable: dict[str, str] = {}
    limited_sources: set[str] = set()
    dedupe_keys: set[str] = set()

    def permitted(query, model):
        columns = (source_columns or {}).get(model.__tablename__)
        if columns:
            return query.options(load_only(*(getattr(model, name) for name in columns), raiseload=True))
        return query

    def scoped(query, model):
        query = permitted(query, model)
        if normalized_scope and hasattr(model, "market") and hasattr(model, "symbol"):
            return query.where(tuple_(model.market, model.symbol).in_(normalized_scope))
        if symbol_filter and hasattr(model, "symbol"):
            return query.where(model.symbol == symbol_filter)
        return query

    if "stock_symbol" in allowed_tables:
        query = scoped(select(StockSymbol), StockSymbol)
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
        query = scoped(select(StockNews), StockNews)
        rows = list(db.scalars(query.order_by(StockNews.news_time.desc()).limit(max_documents)).all())
        documents.extend(
            KnowledgeDocument(
                knowledge_base_id=knowledge_base.id,
                source_table="stock_news",
                source_record_id=row.id,
                market=row.market,
                symbol=row.symbol,
                title=row.title,
                content=row.content or (json.dumps(row.content_json, ensure_ascii=False, default=str) if row.content_json else row.title),
                metadata_json={"news_time": row.news_time, "source_name": row.source_name, "url": row.url,
                               "content_scope": "SOURCE_CONTENT_OR_PAYLOAD" if row.content else "SOURCE_PAYLOAD_OR_TITLE"},
            )
            for row in rows
        )
    if "stock_notice" in allowed_tables:
        query = scoped(select(StockNotice), StockNotice)
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
                scoped(select(StockFinancialReport), StockFinancialReport)
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
                metadata_json={
                    "report_period": row.report_period,
                    "indicator": row.indicator,
                    "currency": row.currency,
                    "data_json": row.data_json,
                    "content_scope": "SOURCE_PAYLOAD",
                },
            )
            for row in rows
        )
    if "stock_kline" in allowed_tables:
        query = scoped(select(StockKline), StockKline)
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
                metadata_json={"trade_date": row.trade_date, "period": row.period, "adjust": row.adjust,
                               "open_price": row.open_price, "high_price": row.high_price,
                               "low_price": row.low_price, "close_price": row.close_price,
                               "volume": row.volume, "amount": row.amount,
                               "turnover_rate": row.turnover_rate, "content_scope": "NORMALIZED_SOURCE_FIELDS"},
            )
            for row in rows
        )
    if "stock_realtime_quote" in allowed_tables:
        query = scoped(select(StockRealtimeQuote), StockRealtimeQuote)
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
                metadata_json={"quote_time": row.quote_time, "fetched_at": row.fetched_at.isoformat() if row.fetched_at else None,
                               "current_price": row.current_price, "change_pct": row.change_pct,
                               "volume": row.volume, "turnover_rate": row.turnover_rate,
                               "raw_payload": row.raw_payload, "content_scope": "SOURCE_PAYLOAD"},
            )
            for row in rows
        )
    if "stock_f10_cache" in allowed_tables:
        query = scoped(select(StockF10Cache), StockF10Cache)
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
                metadata_json={"section": row.section, "payload_json": row.payload_json,
                               "fetched_at": row.fetched_at.isoformat() if row.fetched_at else None,
                               "content_scope": "SOURCE_PAYLOAD"},
            )
            for row in rows
        )
    if "research_report" in allowed_tables:
        query = scoped(select(ResearchReportRecord), ResearchReportRecord)
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

    # The dedicated contextual evidence table is intentionally handled through the
    # generic path.  This keeps policy, raw-material, supply-chain, contract and
    # shareholder evidence extensible without making the graph builder depend on a
    # particular migration order.
    supported = {
        "stock_symbol", "stock_news", "stock_notice", "stock_financial_report",
        "stock_kline", "stock_realtime_quote", "stock_f10_cache", "research_report",
    }
    db_engine = db.get_bind()
    # Inspect within this transaction: a second SQLite connection cannot read
    # schema while a large multi-domain snapshot holds the writer's spill lock.
    source_inspector = inspect(db.connection())
    available_tables = set(source_inspector.get_table_names())
    quote = db_engine.dialect.identifier_preparer.quote
    for table_name in sorted(allowed_tables - supported):
        if table_name not in available_tables or not is_business_table(table_name):
            unavailable[table_name] = "TABLE_NOT_AVAILABLE_OR_NOT_ALLOWED"
            continue
        columns = {str(item["name"]) for item in source_inspector.get_columns(table_name)}
        parameters: dict[str, Any] = {"limit": max_documents}
        where = ""
        if normalized_scope and {"market", "symbol"}.issubset(columns):
            clauses = []
            for index, (market, symbol) in enumerate(normalized_scope):
                clauses.append(f"(market = :market_{index} AND symbol = :symbol_{index})")
                parameters[f"market_{index}"] = market
                parameters[f"symbol_{index}"] = symbol
            where = f" WHERE {' OR '.join(clauses)}"
        elif normalized_scope:
            unavailable[table_name] = "STOCK_SCOPE_NOT_SUPPORTED"
            continue
        elif symbol_filter and "symbol" in columns:
            where = " WHERE symbol = :symbol"
            parameters["symbol"] = symbol_filter
        if symbol_filter and not normalized_scope and "symbol" not in columns:
            unavailable[table_name] = "SYMBOL_FILTER_NOT_SUPPORTED"
            continue
        permitted_columns = (source_columns or {}).get(table_name)
        selection = ", ".join(quote(name) for name in permitted_columns if name in columns) if permitted_columns else "*"
        query = text(f'SELECT {selection} FROM {quote(table_name)}{where} LIMIT :limit')
        try:
            rows = list(db.execute(query, parameters).mappings().all())
        except Exception as exc:
            unavailable[table_name] = str(exc)[:240]
            continue
        raw_counts[table_name] = len(rows)
        for raw in rows:
            row = dict(raw)
            symbol = str(row.get("symbol") or "") or None
            record_id = row.get("id")
            event_type = str(row.get("event_type") or "").upper() or None
            content = row.get("content") or json.dumps(row, ensure_ascii=False, default=str)
            # Keep source payloads intact up to a generous bound and disclose any
            # truncation to callers; never present an excerpt as complete evidence.
            content_truncated = len(content) > 200000
            if content_truncated:
                content = content[:200000]
            title = str(row.get("title") or row.get("name") or f"{table_name} {symbol or record_id or ''}")[:512]
            dedupe_material = " ".join(str(content).split()).lower()
            dedupe_key = f"{table_name}|{symbol or ''}|{title.lower()}|{hashlib.sha1(dedupe_material.encode('utf-8')).hexdigest()}"
            if table_name in {"stock_news", "stock_notice", "stock_context_event"} and dedupe_key in dedupe_keys:
                duplicate_counts[table_name] += 1
                continue
            dedupe_keys.add(dedupe_key)
            documents.append(KnowledgeDocument(
                knowledge_base_id=knowledge_base.id,
                source_table=table_name,
                source_record_id=int(record_id) if isinstance(record_id, int) else None,
                market=str(row.get("market") or "") or None,
                symbol=symbol,
                title=title,
                content=content,
                metadata_json={"source_table": table_name, "source_record_id": record_id,
                               "source_name": row.get("source_name"), "url": row.get("url"),
                               "event_type": event_type, "published_at": row.get("published_at"),
                               "related_entity": row.get("related_entity"),
                               "content_scope": "SOURCE_CONTENT_OR_PAYLOAD",
                               "content_truncated": content_truncated, "dedupe_key": dedupe_key},
            ))
        # Known ORM sources above are counted from their materialized rows.  This
        # keeps coverage useful for both built-in and custom source tables.
    if max_documents_per_stock is not None:
        if max_documents_per_stock < 1:
            raise ValueError("max_documents_per_stock must be positive")
        bounded_documents: list[KnowledgeDocument] = []
        scope_counts: dict[tuple[str, str | None, str | None], int] = {}
        for document in documents:
            key = (document.source_table, document.market, document.symbol)
            count = scope_counts.get(key, 0)
            if count >= max_documents_per_stock:
                limited_sources.add(document.source_table)
                continue
            scope_counts[key] = count + 1
            bounded_documents.append(document)
        documents = bounded_documents
    for source_name in allowed_tables:
        if raw_counts.get(source_name, 0):
            continue
        raw_counts[source_name] = sum(1 for document in documents if document.source_table == source_name)

    # Built-in ORM sources are deduplicated here as well.  The source id remains in
    # evidence, while identical news/notice/event payloads yield one semantic edge.
    unique_documents: list[KnowledgeDocument] = []
    seen_builtin: dict[str, KnowledgeDocument] = {}
    for document in documents:
        if document.source_table not in {"stock_news", "stock_notice", "stock_context_event"}:
            unique_documents.append(document)
            continue
        metadata = dict(document.metadata_json or {})
        key = str(metadata.get("dedupe_key") or (
            f"{document.source_table}|{document.market or ''}|{document.symbol or ''}|"
            f"{document.title.strip().lower()}|{hashlib.sha1(' '.join(document.content.split()).lower().encode('utf-8')).hexdigest()}"
        ))
        if key in seen_builtin:
            duplicate_counts[document.source_table] = duplicate_counts.get(document.source_table, 0) + 1
            primary = seen_builtin[key]
            primary_metadata = dict(primary.metadata_json or {})
            duplicate_sources = list(primary_metadata.get("duplicate_source_records") or [])
            duplicate_sources.append({"source_record_id": document.source_record_id,
                                      "source_name": (metadata or {}).get("source_name"),
                                      "url": (metadata or {}).get("url")})
            primary_metadata["duplicate_source_records"] = duplicate_sources[:20]
            primary.metadata_json = primary_metadata
            continue
        seen_builtin[key] = document
        metadata["dedupe_key"] = key
        document.metadata_json = metadata
        unique_documents.append(document)
    documents = unique_documents
    # Keep bounded source content and disclose truncation uniformly for ORM and
    # generic tables.  A bounded document is still useful evidence but cannot make
    # a graph claim that the source was complete.
    for document in documents:
        if len(document.content or "") > 200000:
            metadata = dict(document.metadata_json or {})
            metadata["content_truncated"] = True
            document.metadata_json = metadata
            document.content = document.content[:200000]
    limited_sources.update(source for source, count in raw_counts.items() if count >= max_documents)

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
    stock_identities = resolve_many(db, ((document.market, document.symbol) for document in documents if document.symbol)) if enrich_company_identity else {
        pair: {"canonical_security_id": f"security:{pair[0]}:{pair[1]}", "canonical_company_id": None,
               "mapping_status": "NOT_AUTHORIZED", "identity_version": "SECURITY_COMPANY_V1"}
        for document in documents if document.symbol and (pair := normalize_pair(document.market, document.symbol))
    }
    company_ids = sorted({identity["company_id"] for identity in stock_identities.values()
                          if identity.get("mapping_status") == "MAPPED" and identity.get("company_id")})
    companies = {row.id: row for row in db.scalars(select(FoundationEntity).where(
        FoundationEntity.id.in_(company_ids))).all()} if company_ids else {}
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
            identity = stock_identities.get(normalize_pair(document.market, document.symbol), {})
            if key not in entities:
                entity = KnowledgeEntity(
                    knowledge_base_id=knowledge_base.id,
                    graph_id=graph.id,
                    entity_type="STOCK",
                    entity_key=f"{graph.id}:{key[1]}",
                    entity_name=document.symbol,
                    properties_json={"market": document.market, "symbol": document.symbol,
                        **identity},
                )
                entities[key] = entity
                db.add(entity)
            company = companies.get(identity.get("company_id"))
            if company is not None:
                company_key = ("COMPANY", str(company.id))
                if company_key not in entities:
                    company_entity = KnowledgeEntity(
                        knowledge_base_id=knowledge_base.id, graph_id=graph.id,
                        entity_type="COMPANY", entity_key=f"{graph.id}:company:{company.id}",
                        entity_name=company.name,
                        properties_json={"canonical_company_id": identity.get("canonical_company_id"),
                                         "company_id": company.id, "jurisdiction": company.jurisdiction,
                                         "identifier_scheme": company.identifier_scheme,
                                         "identifier_value": company.identifier_value,
                                         "identity_version": identity.get("identity_version")},
                    )
                    entities[company_key] = company_entity
                    db.add(company_entity)
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
                "stock_context_event": "EXTERNAL_EVENT",
            }.get(document.source_table, "DOCUMENT"),
            entity_key=document_key[:256],
            entity_name=document.title[:256],
            properties_json={
                "market": document.market,
                "symbol": document.symbol,
                "source_table": document.source_table,
                "source_record_id": document.source_record_id,
                "evidence": {
                    key: (document.metadata_json or {}).get(key)
                    for key in ("source_name", "url", "event_type", "published_at", "related_entity")
                    if (document.metadata_json or {}).get(key)
                },
                "content_scope": (document.metadata_json or {}).get("content_scope", "SOURCE_RECORD"),
                "content_truncated": bool((document.metadata_json or {}).get("content_truncated", False)),
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
        identity = stock_identities.get(normalize_pair(document.market, document.symbol), {})
        company_entity = entities.get(("COMPANY", str(identity.get("company_id"))))
        if document.source_table == "stock_symbol" and company_entity is not None:
            relations.append(KnowledgeRelation(
                knowledge_base_id=knowledge_base.id, graph_id=graph.id,
                subject_entity_id=entity.id, predicate="ISSUED_BY",
                object_entity_id=company_entity.id, evidence_document_id=document.id,
            ))
        metadata = document.metadata_json or {}
        if document.source_table == "stock_financial_report" and entity is not None:
            indicator = str(metadata.get("indicator") or document.title.rsplit(" ", 1)[-1])
            metric_key = ("FINANCIAL_METRIC", f"{document.market or ''}:{document.symbol}:{metadata.get('report_period') or ''}:{indicator}")
            metric_entity = entities.get(metric_key)
            if metric_entity is None:
                metric_entity = KnowledgeEntity(
                    knowledge_base_id=knowledge_base.id, graph_id=graph.id,
                    entity_type="FINANCIAL_METRIC", entity_key=f"{graph.id}:{metric_key[1]}",
                    entity_name=indicator[:256],
                    properties_json={"indicator": indicator, "report_period": metadata.get("report_period"),
                                     "currency": metadata.get("currency"), "data_json": metadata.get("data_json", {}),
                                     "fact_only": True},
                )
                entities[metric_key] = metric_entity
                db.add(metric_entity)
            # Metric nodes are attached below after all new entities receive ids.
        if document.source_table == "stock_f10_cache" and entity is not None:
            section = str(metadata.get("section") or "unknown")
            section_key = ("F10_SECTION", f"{document.market or ''}:{document.symbol}:{section}")
            section_entity = entities.get(section_key)
            if section_entity is None:
                section_entity = KnowledgeEntity(
                    knowledge_base_id=knowledge_base.id, graph_id=graph.id,
                    entity_type="F10_SECTION", entity_key=f"{graph.id}:{section_key[1]}",
                    entity_name=section[:256],
                    properties_json={"section": section, "payload": metadata.get("payload_json", {}), "fact_only": True},
                )
                entities[section_key] = section_entity
                db.add(section_entity)
        if document.source_table == "stock_context_event" and entity is not None:
            event_type = str(metadata.get("event_type") or "OTHER").upper()
            event_key = ("EXTERNAL_EVENT", f"{document.symbol}:{document.id}:{event_type}")
            event_entity = entities.get(event_key)
            if event_entity is None:
                event_entity = KnowledgeEntity(
                    knowledge_base_id=knowledge_base.id, graph_id=graph.id,
                    entity_type="EXTERNAL_EVENT", entity_key=f"{graph.id}:{event_key[1]}",
                    entity_name=document.title[:256],
                    properties_json={"event_type": event_type, "source_name": metadata.get("source_name"),
                                     "url": metadata.get("url"), "published_at": metadata.get("published_at"),
                                     "related_entity": metadata.get("related_entity"), "fact_only": True},
                )
                entities[event_key] = event_entity
                db.add(event_entity)
        if document.source_table in {"stock_news", "stock_notice"} and entity is not None:
            # A headline/content keyword only records that an item *mentions* a
            # topic.  It is deliberately represented as MENTIONED_EVENT and never
            # upgraded to a causal or verified external event.
            text_blob = f"{document.title}\n{document.content}"
            mentioned_types = []
            if re.search(r"政策|监管|法规|国务院|财政|货币", text_blob, re.IGNORECASE):
                mentioned_types.append("POLICY")
            if re.search(r"原材料|锂|铜|钢|石油|大宗|商品价格", text_blob, re.IGNORECASE):
                mentioned_types.append("RAW_MATERIAL")
            if re.search(r"供应链|上游|下游|供应商|客户", text_blob, re.IGNORECASE):
                mentioned_types.append("SUPPLY_CHAIN")
            if re.search(r"回购|增持|减持|股东|大股东", text_blob, re.IGNORECASE):
                mentioned_types.append("SHAREHOLDER")
            if re.search(r"合同|订单|签署|中标|协议", text_blob, re.IGNORECASE):
                mentioned_types.append("CONTRACT")
            for event_type in mentioned_types:
                event_key = ("MENTIONED_EVENT", f"{document.market or ''}:{document.symbol}:{document.id}:{event_type}")
                if event_key not in entities:
                    event_entity = KnowledgeEntity(
                        knowledge_base_id=knowledge_base.id, graph_id=graph.id,
                        entity_type="MENTIONED_EVENT", entity_key=f"{graph.id}:{event_key[1]}",
                        entity_name=f"{event_type} mention: {document.title}"[:256],
                        properties_json={"event_type": event_type, "extraction_method": "KEYWORD_MENTION",
                                         "fact_only": True, "source_table": document.source_table},
                    )
                    entities[event_key] = event_entity
                    db.add(event_entity)
    db.flush()

    # Add source-specific semantic edges after entity ids are available.  Event
    # predicates encode the declared event type only; they do not claim causality.
    semantic_seen: set[tuple[int, str, int, int]] = set()
    for document in documents:
        if not document.symbol:
            continue
        entity = entities.get(("STOCK", f"{document.market}:{document.symbol}"))
        doc_entity = document_entities.get(document.id)
        if entity is None or doc_entity is None:
            continue
        metadata = document.metadata_json or {}
        semantic: list[tuple[str, tuple[str, str]]] = []
        if document.source_table == "stock_financial_report":
            indicator = str(metadata.get("indicator") or document.title.rsplit(" ", 1)[-1])
            semantic.append(("HAS_FINANCIAL_METRIC", ("FINANCIAL_METRIC", f"{document.market or ''}:{document.symbol}:{metadata.get('report_period') or ''}:{indicator}")))
        elif document.source_table == "stock_f10_cache":
            section = str(metadata.get("section") or "unknown")
            semantic.append(("HAS_F10_SECTION", ("F10_SECTION", f"{document.market or ''}:{document.symbol}:{section}")))
            if re.search(r"share|holder|股东|回购|增持|减持|股本", section, re.IGNORECASE):
                semantic.append(("HAS_SHAREHOLDER_EVENT", ("F10_SECTION", f"{document.symbol}:{section}")))
        elif document.source_table == "stock_context_event":
            event_type = str(metadata.get("event_type") or "OTHER").upper()
            predicate = {"POLICY": "HAS_POLICY_EVENT", "RAW_MATERIAL": "HAS_RAW_MATERIAL_EVENT",
                         "SUPPLY_CHAIN": "HAS_SUPPLY_CHAIN_EVENT", "SHAREHOLDER": "HAS_SHAREHOLDER_EVENT",
                         "CONTRACT": "HAS_CONTRACT_EVENT"}.get(event_type, "HAS_EXTERNAL_EVENT")
            semantic.append((predicate, ("EXTERNAL_EVENT", f"{document.symbol}:{document.id}:{event_type}")))
        elif document.source_table in {"stock_news", "stock_notice"}:
            text_blob = f"{document.title}\n{document.content}"
            mentions = []
            if re.search(r"政策|监管|法规|国务院|财政|货币", text_blob, re.IGNORECASE):
                mentions.append(("POLICY", "MENTIONS_POLICY"))
            if re.search(r"原材料|锂|铜|钢|石油|大宗|商品价格", text_blob, re.IGNORECASE):
                mentions.append(("RAW_MATERIAL", "MENTIONS_RAW_MATERIAL"))
            if re.search(r"供应链|上游|下游|供应商|客户", text_blob, re.IGNORECASE):
                mentions.append(("SUPPLY_CHAIN", "MENTIONS_SUPPLY_CHAIN"))
            if re.search(r"回购|增持|减持|股东|大股东", text_blob, re.IGNORECASE):
                mentions.append(("SHAREHOLDER", "MENTIONS_SHAREHOLDER"))
            if re.search(r"合同|订单|签署|中标|协议", text_blob, re.IGNORECASE):
                mentions.append(("CONTRACT", "MENTIONS_CONTRACT"))
            semantic.extend((predicate, ("MENTIONED_EVENT", f"{document.market or ''}:{document.symbol}:{document.id}:{event_type}"))
                            for event_type, predicate in mentions)
        for predicate, target_key in semantic:
            target = entities.get(target_key)
            if target is None:
                continue
            key = (entity.id, predicate, target.id, document.id)
            if key in semantic_seen:
                continue
            semantic_seen.add(key)
            relations.append(KnowledgeRelation(
                knowledge_base_id=knowledge_base.id, graph_id=graph.id,
                subject_entity_id=entity.id, predicate=predicate,
                object_entity_id=target.id, evidence_document_id=document.id,
            ))
            relations.append(KnowledgeRelation(
                knowledge_base_id=knowledge_base.id, graph_id=graph.id,
                subject_entity_id=target.id, predicate="SUPPORTED_BY",
                object_entity_id=doc_entity.id, evidence_document_id=document.id,
            ))

    # Deterministic observable anomaly flags for price/volume, always linked to the
    # original K-line row.  They are facts about an observation rather than forecasts.
    from statistics import median
    kline_rows: dict[tuple[str, str, str, str], list[KnowledgeDocument]] = {}
    for document in documents:
        if document.source_table == "stock_kline" and document.symbol:
            metadata = document.metadata_json or {}
            key = (str(document.market or ""), document.symbol, str(metadata.get("period") or ""), str(metadata.get("adjust") or ""))
            kline_rows.setdefault(key, []).append(document)
    baseline_missing: list[str] = []
    for (market, symbol, period, adjust), rows in kline_rows.items():
        # Rows are sorted oldest first so the anomaly baseline never sees future
        # bars.  Separate markets and adjustment modes to avoid mixing scales.
        rows.sort(key=lambda item: str((item.metadata_json or {}).get("trade_date") or ""))
        prior_volumes: list[float] = []
        stock = entities.get(("STOCK", f"{market}:{symbol}"))
        if stock is None:
            continue
        for row in rows:
            metadata = row.metadata_json or {}
            anomalies: list[tuple[str, dict[str, Any]]] = []
            try:
                baseline = median(prior_volumes[-20:]) if len(prior_volumes) >= 3 else None
                if baseline is None and metadata.get("volume") not in (None, ""):
                    baseline_missing.append(f"{market}:{symbol}:{metadata.get('trade_date') or row.id}")
                if baseline and metadata.get("volume") is not None and float(metadata["volume"]) >= baseline * 3:
                    anomalies.append(("VOLUME_SPIKE", {"volume": metadata["volume"], "baseline_volume": baseline,
                                                        "baseline_window": min(len(prior_volumes), 20)}))
                if metadata.get("open_price") not in (None, 0, "") and metadata.get("close_price") not in (None, ""):
                    if abs(float(metadata["close_price"]) / float(metadata["open_price"]) - 1) >= 0.07:
                        anomalies.append(("PRICE_MOVE", {"open_price": metadata["open_price"], "close_price": metadata["close_price"]}))
                if metadata.get("turnover_rate") is not None and float(metadata["turnover_rate"]) >= 20:
                    anomalies.append(("TURNOVER_SPIKE", {"turnover_rate": metadata["turnover_rate"]}))
            except (TypeError, ValueError):
                continue
            try:
                if metadata.get("volume") not in (None, ""):
                    prior_volumes.append(float(metadata["volume"]))
            except (TypeError, ValueError):
                pass
            for anomaly_type, values in anomalies:
                key = ("MARKET_ANOMALY", f"{market}:{symbol}:{period}:{adjust}:{metadata.get('trade_date') or row.id}:{anomaly_type}")
                anomaly = entities.get(key)
                if anomaly is None:
                    anomaly = KnowledgeEntity(
                        knowledge_base_id=knowledge_base.id, graph_id=graph.id,
                        entity_type="MARKET_ANOMALY", entity_key=f"{graph.id}:{key[1]}",
                        entity_name=f"{symbol} {anomaly_type}",
                        properties_json={"anomaly_type": anomaly_type, **values,
                                         "trade_date": metadata.get("trade_date"), "fact_only": True},
                    )
                    entities[key] = anomaly
                    db.add(anomaly)
                    db.flush()
                relations.append(KnowledgeRelation(
                    knowledge_base_id=knowledge_base.id, graph_id=graph.id,
                    subject_entity_id=stock.id, predicate="HAS_MARKET_ANOMALY",
                    object_entity_id=anomaly.id, evidence_document_id=row.id,
                ))
                relations.append(KnowledgeRelation(
                    knowledge_base_id=knowledge_base.id, graph_id=graph.id,
                    subject_entity_id=anomaly.id, predicate="EVIDENCED_BY",
                    object_entity_id=document_entities[row.id].id, evidence_document_id=row.id,
                ))
    db.add_all(relations)
    db.flush()
    graph.entity_count = len(entities) + len(document_entities)
    graph.relation_count = len(relations)
    knowledge_base.entity_count = int(db.scalar(select(func.count(KnowledgeEntity.id)).where(KnowledgeEntity.knowledge_base_id == knowledge_base.id)) or 0)
    knowledge_base.relation_count = int(db.scalar(select(func.count(KnowledgeRelation.id)).where(KnowledgeRelation.knowledge_base_id == knowledge_base.id)) or 0)
    # Report source/category coverage explicitly.  Empty feeds are not silently
    # turned into a "governed" graph, while a graph intentionally configured with a
    # single source (for example a research-only graph) can still be complete.
    category_sources = {
        "company_profile": ["stock_symbol"],
        "market_price_volume": ["stock_kline", "stock_realtime_quote"],
        "financial_fundamentals": ["stock_financial_report"],
        "shareholders_corporate_actions": ["stock_f10_cache", "stock_context_event"],
        "news_notices": ["stock_news", "stock_notice"],
        "external_context": ["stock_context_event"],
        "research": ["research_report"],
    }
    categories: dict[str, Any] = {}
    missing_categories: list[str] = []
    shareholder_documents = [
        document for document in documents
        if (document.source_table == "stock_f10_cache" and re.search(
            r"share|holder|股东|回购|增持|减持|股本", str((document.metadata_json or {}).get("section") or ""), re.IGNORECASE
        ))
        or (document.source_table == "stock_context_event" and
            str((document.metadata_json or {}).get("event_type") or "").upper() == "SHAREHOLDER")
    ]
    for category, sources in category_sources.items():
        configured = [source for source in sources if source in allowed_tables]
        records = (len(shareholder_documents) if category == "shareholders_corporate_actions"
                   else sum(raw_counts.get(source, 0) for source in configured))
        categories[category] = {"configured_sources": configured, "records": records,
                                "covered": bool(records) if configured else False}
        if configured and not records:
            missing_categories.append(category)
    source_coverage = {
        source: {"status": ("READY" if raw_counts.get(source, 0) else
                             ("UNAVAILABLE" if source in unavailable else "EMPTY")),
                 "raw_records": raw_counts.get(source, 0),
                 "records_kept": sum(1 for document in documents if document.source_table == source),
                 "duplicates_removed": duplicate_counts.get(source, 0),
                 "limit_reached": source in limited_sources}
        for source in sorted(allowed_tables)
    }
    missing_data = [source if source not in unavailable else f"{source}:{unavailable[source]}"
                    for source in sorted(allowed_tables)
                    if not raw_counts.get(source, 0) or source in limited_sources]
    # Whole-graph counts can hide a single-stock gap (one financial row does not
    # cover thousands of symbols).  Summarize dimensions per symbol and include a
    # small sample for the governance UI and review workflow.
    configured_dimension_sources = {
        category: [source for source in sources if source in allowed_tables]
        for category, sources in category_sources.items()
    }
    stock_symbols = sorted({f"{document.market or ''}:{document.symbol}" for document in documents if document.symbol})
    stock_dimensions: dict[str, set[str]] = {symbol: set() for symbol in stock_symbols}
    source_to_categories = {
        source: [category for category, sources in configured_dimension_sources.items() if source in sources]
        for source in allowed_tables
    }
    for document in documents:
        if not document.symbol:
            continue
        stock_key = f"{document.market or ''}:{document.symbol}"
        dimensions = source_to_categories.get(document.source_table, [])
        if document.source_table == "stock_f10_cache" and not re.search(
            r"share|holder|股东|回购|增持|减持|股本", str((document.metadata_json or {}).get("section") or ""), re.IGNORECASE
        ):
            dimensions = [category for category in dimensions if category != "shareholders_corporate_actions"]
        if document.source_table == "stock_context_event" and str((document.metadata_json or {}).get("event_type") or "").upper() != "SHAREHOLDER":
            dimensions = [category for category in dimensions if category != "shareholders_corporate_actions"]
        stock_dimensions.setdefault(stock_key, set()).update(dimensions)
    required_dimensions = [category for category, sources in configured_dimension_sources.items() if sources]
    missing_dimension_counts = {
        category: sum(1 for dimensions in stock_dimensions.values() if category not in dimensions)
        for category in required_dimensions
    }
    uncovered_symbols = [symbol for symbol, dimensions in stock_dimensions.items()
                         if any(category not in dimensions for category in required_dimensions)]
    stock_dimension_coverage = {
        "total_stock_count": len(stock_dimensions),
        "covered_stock_count": len(stock_dimensions) - len(uncovered_symbols),
        "required_dimensions": required_dimensions,
        "missing_stock_count_by_dimension": missing_dimension_counts,
        "uncovered_symbol_sample": uncovered_symbols[:50],
    }
    stock_scope_missing = bool(required_dimensions and not stock_dimensions)
    coverage = {
        "complete": not missing_data and not missing_categories and not limited_sources and
        not uncovered_symbols and not stock_scope_missing,
        "configured_sources": sorted(allowed_tables),
        "source_coverage": source_coverage,
        "categories": categories,
        "missing_data": missing_data,
        "missing_categories": missing_categories,
        "limited_sources": sorted(limited_sources),
        "stock_dimension_coverage": stock_dimension_coverage,
        "stock_scope_missing": stock_scope_missing,
        "documents_before_dedupe": sum(raw_counts.values()),
        "documents_after_dedupe": len(documents),
        "duplicate_records_removed": sum(duplicate_counts.values()),
        "anomaly_baseline_missing_sample": baseline_missing[:50],
        "notes": ["事实关系均关联证据文档；未从新闻标题推断因果或补造缺失数据。"],
    }
    knowledge_base.status = "READY" if coverage["complete"] else "PARTIAL"
    db.flush()
    return {
        "documents_created": len(documents),
        "entities_created": len(entities) + len(document_entities),
        "relations_created": len(relations),
        "coverage": coverage,
    }


def build_knowledge_base(db: Session, knowledge_base: KnowledgeBase, max_documents: int = 100000) -> dict[str, Any]:
    graph = db.scalar(select(KnowledgeGraph).where(KnowledgeGraph.graph_code == knowledge_base.kb_code))
    if graph is None:
        raise ValueError("Default knowledge graph not found")
    result = build_knowledge_graph(db, graph, max_documents)
    db.commit()
    return result
