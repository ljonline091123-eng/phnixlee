from __future__ import annotations

from datetime import datetime, timezone
from typing import Any

from sqlalchemy import inspect, select, text
from sqlalchemy.engine import Engine
from sqlalchemy.orm import Session

from app.db.session import engine
from app.models.ai_hub import AgentDataAsset, AgentDefinition, GovernanceRun, KnowledgeBase, KnowledgeGraph
from app.services.model_hub import ModelHubService
from app.services.resource_hub import build_knowledge_graph, inspect_data_asset, is_business_table


GOVERNANCE_STATES = {"PENDING", "GOVERNED", "LOCKED"}


def available_source_tables(db_engine: Engine = engine) -> list[str]:
    """Only expose local business data; never expose credentials or control tables."""
    return sorted(name for name in inspect(db_engine).get_table_names() if is_business_table(name))


def preview_table(table_name: str, columns: list[str] | None = None, limit: int = 20,
                  db_engine: Engine = engine) -> dict[str, Any]:
    if table_name not in available_source_tables(db_engine):
        raise ValueError("Data source is unavailable")
    all_columns = [str(item["name"]) for item in inspect(db_engine).get_columns(table_name)]
    chosen = [name for name in (columns or all_columns) if name in all_columns]
    if not chosen:
        raise ValueError("No readable columns")
    quote = db_engine.dialect.identifier_preparer.quote
    statement = text(f'SELECT {", ".join(quote(name) for name in chosen)} FROM {quote(table_name)} LIMIT :limit')
    with db_engine.connect() as connection:
        rows = [dict(row._mapping) for row in connection.execute(statement, {"limit": min(100, max(1, limit))})]
        count = int(connection.execute(text(f'SELECT COUNT(*) FROM {quote(table_name)}')).scalar_one())
    return {"table_name": table_name, "columns": chosen, "row_count": count, "rows": rows}


def quality_profile(table_name: str, columns: list[str], db_engine: Engine) -> dict[str, Any]:
    details = inspect(db_engine)
    all_columns = [str(item["name"]) for item in details.get_columns(table_name)]
    chosen = [name for name in columns if name in all_columns]
    quote = db_engine.dialect.identifier_preparer.quote
    counts = ", ".join(
        f'SUM(CASE WHEN {quote(name)} IS NULL THEN 1 ELSE 0 END) AS {quote(name)}'
        for name in chosen
    )
    if not counts:
        raise ValueError("Data source has no allowed columns")
    with db_engine.connect() as connection:
        result = connection.execute(text(f'SELECT {counts} FROM {quote(table_name)}')).mappings().one()
    return {
        "primary_key": details.get_pk_constraint(table_name).get("constrained_columns") or [],
        "null_counts": {name: int(result[name] or 0) for name in chosen},
        "checked_columns": chosen,
    }


def _source_assets(db: Session, asset_ids: list[int]) -> list[AgentDataAsset]:
    ids = list(dict.fromkeys(asset_ids))
    assets = list(db.scalars(select(AgentDataAsset).where(AgentDataAsset.id.in_(ids))).all()) if ids else []
    if len(assets) != len(ids):
        raise ValueError("One or more data assets do not exist")
    if any(asset.table_name not in available_source_tables(db.get_bind()) for asset in assets):
        raise ValueError("One or more source tables are unavailable")
    return assets


def _agent(db: Session, agent_id: int | None, code: str) -> AgentDefinition | None:
    agent = db.get(AgentDefinition, agent_id) if agent_id else db.scalar(
        select(AgentDefinition).where(AgentDefinition.agent_code == code)
    )
    if agent_id and agent is None:
        raise ValueError("Selected agent does not exist")
    if agent and not agent.enabled:
        raise ValueError("Selected agent is disabled")
    return agent


def _agent_review(db: Session, agent: AgentDefinition | None, task_type: str, skill_code: str, evidence: dict[str, Any]):
    import json

    messages = []
    if agent:
        messages.append({"role": "system", "content": agent.system_prompt})
    messages.append({
        "role": "user",
        "content": "请基于以下真实数据源检查字段含义、缺失值、去重键、时间口径和可追溯证据；不要编造事实。\n"
        + json.dumps(evidence, ensure_ascii=False, default=str)[:12000],
    })
    log = ModelHubService(db).chat(
        task_type=task_type,
        instance_code=agent.model_instance_code if agent else None,
        messages=messages,
        metadata_json={"skill_code": skill_code, "governance_target": evidence.get("target")},
    )
    if log.status != "SUCCESS":
        raise ValueError(log.error_message or "Agent governance failed")
    return log


def _record(db: Session, target_type: str, target_id: int, asset_ids: list[int], agent_id: int | None,
            status: str, summary: dict[str, Any], model_call_log_id: int | None = None) -> GovernanceRun:
    run = GovernanceRun(target_type=target_type, target_id=target_id, source_asset_ids=asset_ids,
                        agent_id=agent_id, model_call_log_id=model_call_log_id, status=status,
                        summary_json=summary)
    db.add(run)
    db.commit()
    db.refresh(run)
    return run


def govern_asset(db: Session, asset: AgentDataAsset, source_asset_ids: list[int], agent_id: int | None) -> GovernanceRun:
    if asset.governance_status == "LOCKED":
        raise ValueError("Locked data assets cannot be governed")
    ids = list(dict.fromkeys([asset.id, *source_asset_ids]))
    sources = _source_assets(db, ids)
    agent = _agent(db, agent_id, "DATA_GOVERNANCE_AGENT")
    try:
        evidence = {"target": asset.asset_code, "sources": []}
        for source in sources:
            inspect_data_asset(source, db.get_bind())
            preview = preview_table(source.table_name, source.allowed_columns, 10, db.get_bind())
            evidence["sources"].append({
                "asset_code": source.asset_code, "table": source.table_name,
                "columns": preview["columns"], "row_count": preview["row_count"],
                "quality": quality_profile(source.table_name, preview["columns"], db.get_bind()),
                "sample": [{key: str(value)[:160] for key, value in row.items()} for row in preview["rows"][:3]],
            })
        log = _agent_review(db, agent, "data_governance", "DATA_GOVERNANCE_DW", evidence)
        report = {"sources": evidence["sources"], "agent_review": log.response_text,
                  "provider_code": log.provider_code, "model_call_log_id": log.id,
                  "checked_at": datetime.now(timezone.utc).isoformat()}
        asset.governance_status = "GOVERNED"
        asset.last_governed_at = datetime.now(timezone.utc)
        asset.governance_report_json = report
        return _record(db, "ASSET", asset.id, ids, agent.id if agent else None, "SUCCESS", report, log.id)
    except Exception as exc:
        db.rollback()
        _record(db, "ASSET", asset.id, ids, agent.id if agent else None, "FAILED", {"error": str(exc)})
        raise


def govern_graph(db: Session, graph: KnowledgeGraph, source_asset_ids: list[int], agent_id: int | None) -> GovernanceRun:
    if graph.governance_status == "LOCKED":
        raise ValueError("Locked knowledge graphs cannot be governed")
    kb = db.get(KnowledgeBase, graph.knowledge_base_id)
    if not kb:
        raise ValueError("Knowledge base is unavailable")
    table_names = graph.source_tables or kb.source_tables
    if source_asset_ids:
        sources = _source_assets(db, source_asset_ids)
        if any(source.table_name not in kb.source_tables for source in sources):
            raise ValueError("Graph sources must belong to its knowledge base")
        table_names = [source.table_name for source in sources]
    else:
        sources = list(db.scalars(select(AgentDataAsset).where(AgentDataAsset.table_name.in_(table_names))).all())
        if len(sources) != len(set(table_names)):
            raise ValueError("Register every configured graph source before governance")
    if not sources:
        raise ValueError("Choose at least one enabled registered data source")
    agent = _agent(db, agent_id, "KNOWLEDGE_GRAPH_AGENT")
    ids = [source.id for source in sources]
    try:
        evidence = {"target": graph.graph_code, "symbol": graph.symbol,
                    "sources": [{"asset_code": source.asset_code, "table": source.table_name,
                                 "row_count": preview_table(source.table_name, source.allowed_columns, 1, db.get_bind())["row_count"]}
                                for source in sources]}
        log = _agent_review(db, agent, "knowledge_graph", "STOCK_KNOWLEDGE_GRAPH_BUILDER", evidence)
        graph.source_tables = table_names
        counts = build_knowledge_graph(db, graph)
        report = {**counts, "sources": evidence["sources"], "agent_review": log.response_text,
                  "provider_code": log.provider_code, "model_call_log_id": log.id,
                  "checked_at": datetime.now(timezone.utc).isoformat()}
        graph.governance_status = "GOVERNED"
        graph.last_governed_at = datetime.now(timezone.utc)
        graph.governance_report_json = report
        return _record(db, "GRAPH", graph.id, ids, agent.id if agent else None, "SUCCESS", report, log.id)
    except Exception as exc:
        db.rollback()
        _record(db, "GRAPH", graph.id, ids, agent.id if agent else None, "FAILED", {"error": str(exc)})
        raise
