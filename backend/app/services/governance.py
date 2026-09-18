from __future__ import annotations

import asyncio
from datetime import datetime, timezone
from typing import Any

from sqlalchemy import inspect, select, text
from sqlalchemy.engine import Engine
from sqlalchemy.orm import Session, sessionmaker

from app.core.agent.data_cleaning_runner import run_data_cleaning_agent
from app.db.session import engine
from app.models.ai_hub import AgentDataAsset, AgentDefinition, GovernanceRun, KnowledgeBase, KnowledgeGraph
from app.services.model_hub import ModelHubService
from app.services.resource_hub import build_knowledge_graph, inspect_data_asset, is_business_table


GOVERNANCE_STATES = {"PENDING", "GOVERNED", "LOCKED"}
MAX_GOVERNANCE_RECORDS = 5000

ASSET_CATEGORIES = {
    "STOCK_SYMBOL": "BASIC",
    "STOCK_QUOTE": "PRICE",
    "STOCK_KLINE": "KLINE",
    "STOCK_FINANCIAL": "FINANCIAL_REPORT",
    "STOCK_NOTICE": "NOTICE",
    "STOCK_NEWS": "NEWS",
    "STOCK_F10": "F10",
    "RESEARCH_REPORT": "RESEARCH_REPORT",
    "WATCHLIST": "WATCHLIST",
    "DATA_SYNC_LOG": "DATA_SYNC_LOG",
    "DATA_FETCH_LOG": "DATA_FETCH_LOG",
}

GOVERNANCE_COLUMNS = {
    "id", "market", "stock_code", "symbol", "source", "source_name", "source_id",
    "model_provider", "interface_code", "observed_at", "news_time", "notice_date",
    "report_period", "trade_date", "quote_time", "fetched_at", "updated_at", "created_at",
    "last_synced_at", "started_at", "price", "current_price", "close", "close_price",
    "open", "open_price", "volume",
}

TIME_COLUMNS = (
    "observed_at", "news_time", "notice_date", "trade_date", "quote_time", "report_period",
    "fetched_at", "last_synced_at", "updated_at", "created_at", "started_at",
)


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


def _asset_category(asset: AgentDataAsset) -> str:
    if asset.asset_code in ASSET_CATEGORIES:
        return ASSET_CATEGORIES[asset.asset_code]
    name = f"{asset.asset_code}_{asset.table_name}".upper()
    for token, category in (
        ("SHAREHOLDER", "SHAREHOLDER"), ("FINANCIAL", "FINANCIAL_REPORT"),
        ("NOTICE", "NOTICE"), ("NEWS", "NEWS"), ("KLINE", "KLINE"),
        ("QUOTE", "PRICE"), ("RESEARCH", "RESEARCH_REPORT"), ("SYMBOL", "BASIC"),
    ):
        if token in name:
            return category
    return asset.asset_code.upper()


def _business_key(table_name: str, db_engine: Engine) -> list[str]:
    details = inspect(db_engine)
    unique_keys = [
        list(item.get("column_names") or [])
        for item in details.get_unique_constraints(table_name)
        if item.get("column_names")
    ]
    if unique_keys:
        return min(unique_keys, key=len)
    primary_key = list(details.get_pk_constraint(table_name).get("constrained_columns") or [])
    return primary_key or [str(details.get_columns(table_name)[0]["name"])]


def _read_governance_rows(
    asset: AgentDataAsset, limit: int, db_engine: Engine
) -> tuple[list[dict[str, Any]], list[str]]:
    if asset.table_name not in available_source_tables(db_engine):
        raise ValueError(f"Data source is unavailable: {asset.table_name}")
    all_columns = [str(item["name"]) for item in inspect(db_engine).get_columns(asset.table_name)]
    permitted = [name for name in (asset.allowed_columns or all_columns) if name in all_columns]
    chosen = [name for name in permitted if name in GOVERNANCE_COLUMNS]
    if not chosen and permitted:
        chosen = permitted[:1]
    if not chosen:
        raise ValueError(f"Data asset has no readable governance columns: {asset.asset_code}")
    quote = db_engine.dialect.identifier_preparer.quote
    statement = text(
        f'SELECT {", ".join(quote(name) for name in chosen)} '
        f'FROM {quote(asset.table_name)} LIMIT :limit'
    )
    with db_engine.connect() as connection:
        rows = [
            dict(row._mapping)
            for row in connection.execute(statement, {"limit": min(MAX_GOVERNANCE_RECORDS, limit)})
        ]
    return rows, _business_key(asset.table_name, db_engine)


def _first_value(row: dict[str, Any], names: tuple[str, ...]) -> Any:
    return next((row[name] for name in names if name in row and row[name] not in (None, "")), None)


def _canonical_records(
    asset: AgentDataAsset, rows: list[dict[str, Any]], business_key: list[str]
) -> list[dict[str, Any]]:
    category = _asset_category(asset)
    source_columns = tuple(
        name for name in ("source", "source_name", "source_id", "model_provider", "interface_code")
        if name in (rows[0] if rows else {})
    )
    records: list[dict[str, Any]] = []
    for row in rows:
        key_values = [row.get(name) for name in business_key]
        source_record_id = (
            "|".join(f"{name}={value}" for name, value in zip(business_key, key_values))
            if key_values and all(value not in (None, "") for value in key_values)
            else row.get("id")
        )
        source = _first_value(row, source_columns) if source_columns else asset.table_name
        market = _first_value(row, ("market",))
        stock_code = _first_value(row, ("stock_code", "symbol"))
        if category in {"DATA_SYNC_LOG", "DATA_FETCH_LOG"}:
            market = market or "SYSTEM"
            stock_code = stock_code or _first_value(row, ("interface_code",)) or asset.asset_code
        payload_json: dict[str, Any] = {}
        for target, candidates in {
            "price": ("price", "current_price"),
            "close": ("close", "close_price"),
            "open": ("open", "open_price"),
            "volume": ("volume",),
        }.items():
            source_name = next((name for name in candidates if name in row), None)
            if source_name:
                payload_json[target] = row[source_name]
        records.append({
            "market": market or "",
            "stock_code": stock_code or "",
            "category": category,
            "source": source if source not in (None, "") else "",
            "source_record_id": source_record_id or "",
            "observed_at": _first_value(row, TIME_COLUMNS) or "",
            "payload_json": payload_json,
        })
    return records


def _load_asset_batch(
    sources: list[AgentDataAsset], record_limit: int, db_engine: Engine
) -> tuple[list[dict[str, Any]], dict[str, tuple[str, list[str]]]]:
    records: list[dict[str, Any]] = []
    catalog: dict[str, tuple[str, list[str]]] = {}
    remaining = min(MAX_GOVERNANCE_RECORDS, max(1, record_limit))
    for index, source in enumerate(sources):
        if remaining <= 0:
            break
        sources_left = len(sources) - index
        source_limit = max(1, remaining // sources_left)
        rows, business_key = _read_governance_rows(source, source_limit, db_engine)
        category = _asset_category(source)
        catalog[category] = (source.table_name, business_key)
        records.extend(_canonical_records(source, rows, business_key))
        remaining -= len(rows)
    return records, catalog


def _data_cleaning_llm_call(
    db_engine: Engine,
    agent: AgentDefinition | None,
    runtime: dict[str, Any],
):
    local_sessions = sessionmaker(bind=db_engine, autoflush=False, autocommit=False)
    instance_code = agent.model_instance_code if agent else None
    agent_prompt = agent.system_prompt.strip() if agent else ""

    async def call(*, system_prompt: str, user_prompt: str, json_schema: dict[str, Any]) -> str:
        def invoke() -> str:
            with local_sessions() as local_db:
                combined_prompt = "\n\n".join(item for item in (agent_prompt, system_prompt) if item)
                log = ModelHubService(local_db).chat(
                    task_type="data_governance",
                    instance_code=instance_code,
                    messages=[
                        {"role": "system", "content": combined_prompt},
                        {"role": "user", "content": user_prompt},
                    ],
                    temperature=0.0,
                    max_tokens=4096,
                    metadata_json={
                        "json_schema_output": json_schema,
                        "json_schema_name": "data_cleaning_output",
                        "native_structured_output": True,
                        "defer_schema_validation": True,
                        "source": "data_asset_governance",
                    },
                )
                runtime.update({
                    "model_call_log_id": log.id,
                    "provider_code": log.provider_code,
                    "instance_code": log.instance_code,
                })
                if log.status != "SUCCESS" or not log.response_text:
                    raise RuntimeError(log.error_message or "All data-governance model routes failed")
                return log.response_text

        return await asyncio.to_thread(invoke)

    return call


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
        metadata_json={
            "skill_code": skill_code,
            "governance_target": evidence.get("target"),
            "json_schema_output": agent.json_schema_output if agent else {},
        },
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


async def govern_asset(
    db: Session,
    asset: AgentDataAsset,
    source_asset_ids: list[int],
    agent_id: int | None,
    record_limit: int = MAX_GOVERNANCE_RECORDS,
) -> GovernanceRun:
    if asset.governance_status == "LOCKED":
        raise ValueError("Locked data assets cannot be governed")
    ids = list(dict.fromkeys([asset.id, *source_asset_ids]))
    sources = _source_assets(db, ids)
    agent = _agent(db, agent_id, "DATA_GOVERNANCE_AGENT")
    run = _record(
        db,
        "ASSET",
        asset.id,
        ids,
        agent.id if agent else None,
        "RUNNING",
        {"phase": "HARD_RULE_VALIDATION", "record_limit": record_limit},
    )
    try:
        for source in sources:
            inspect_data_asset(source, db.get_bind())
        records, target_catalog = _load_asset_batch(sources, record_limit, db.get_bind())
        runtime: dict[str, Any] = {}
        output = await run_data_cleaning_agent(
            records,
            llm_call=_data_cleaning_llm_call(db.get_bind(), agent, runtime),
            target_table_catalog=target_catalog,
        )
        report = output.model_dump(mode="json")
        asset.governance_status = "GOVERNED" if output.status.value == "COMPLETED" else "PENDING"
        asset.last_governed_at = datetime.now(timezone.utc)
        asset.governance_report_json = report
        run.status = output.status.value
        run.summary_json = report
        run.model_call_log_id = runtime.get("model_call_log_id")
        db.commit()
        db.refresh(run)
        return run
    except Exception as exc:
        db.rollback()
        failed_run = db.get(GovernanceRun, run.id)
        if failed_run:
            failed_run.status = "FAILED"
            failed_run.summary_json = {"error": str(exc)}
            db.commit()
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
        coverage = counts.get("coverage", {}) if isinstance(counts, dict) else {}
        # A successful build with missing/empty configured feeds is a partial,
        # reviewable result.  It must not be presented as a complete graph.
        governance_status = "GOVERNED" if coverage.get("complete", True) else "PENDING"
        run_status = "SUCCESS" if governance_status == "GOVERNED" else "PENDING_REVIEW"
        report = {**counts, "coverage": coverage, "sources": evidence["sources"], "agent_review": log.response_text,
                  "provider_code": log.provider_code, "model_call_log_id": log.id,
                  "checked_at": datetime.now(timezone.utc).isoformat()}
        graph.governance_status = governance_status
        graph.last_governed_at = datetime.now(timezone.utc)
        graph.governance_report_json = report
        return _record(db, "GRAPH", graph.id, ids, agent.id if agent else None, run_status, report, log.id)
    except Exception as exc:
        db.rollback()
        _record(db, "GRAPH", graph.id, ids, agent.id if agent else None, "FAILED", {"error": str(exc)})
        raise
