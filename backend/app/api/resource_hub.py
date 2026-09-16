from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException, Response, status
from fastapi.encoders import jsonable_encoder
from sqlalchemy import delete, func, select
from sqlalchemy.orm import Session
from jsonschema import Draft202012Validator
from jsonschema.exceptions import SchemaError

from app.db.session import get_db
from app.models.ai_hub import (
    AgentDataAsset,
    AgentDataAssetLink,
    AgentDefinition,
    AgentKnowledgeBaseLink,
    AgentSkillLink,
    KnowledgeBase,
    KnowledgeGraph,
    GovernanceRun,
    KnowledgeDocument,
    KnowledgeEntity,
    KnowledgeRelation,
    ModelInstance,
    ModelProvider,
    ModelSkill,
)
from app.schemas.model_hub import ModelChatResponse, ModelSkillRead
from app.schemas.resource_hub import (
    AgentCreate,
    AgentRead,
    AgentRunRequest,
    AgentSummary,
    AgentUpdate,
    DataAssetRead,
    DataAssetCreate,
    DataAssetUpdate,
    KnowledgeBaseCreate,
    KnowledgeBaseRead,
    KnowledgeBaseUpdate,
    KnowledgeBuildResponse,
    KnowledgeSearchResult,
    KnowledgeGraphCreate,
    KnowledgeGraphRead,
    KnowledgeGraphUpdate,
    GovernanceRequest,
    GovernanceBatchRequest,
    GovernanceStateUpdate,
    GovernanceRunRead,
)
from app.services.governance import available_source_tables, govern_asset, govern_graph, preview_table, GOVERNANCE_STATES
from app.services.resource_hub import (
    build_knowledge_base,
    inspect_data_asset,
    seed_default_data_assets,
    set_agent_links,
    table_columns,
)
from app.services.skill_files import delete_skill_file, skill_file_path, write_skill_file
from app.orchestration.model_hub import (
    AgentExecutionCommand,
    AgentExecutionWorkflow,
    AgentOutputError,
    AgentUnavailableError,
)

router = APIRouter(prefix="/resources", tags=["AI Resource Hub"])


def _agent_read(agent: AgentDefinition) -> AgentRead:
    return AgentRead(
        id=agent.id,
        agent_code=agent.agent_code,
        display_name=agent.display_name,
        system_prompt=agent.system_prompt,
        model_instance_code=agent.model_instance_code,
        max_iterations=agent.max_iterations,
        context_window_limit=agent.context_window_limit,
        json_schema_output=agent.json_schema_output or {},
        enabled=agent.enabled,
        description=agent.description,
        version=agent.version,
        child_agent_ids=[link.child_agent_id for link in sorted(agent.child_links, key=lambda item: item.order_index)],
        skill_ids=[link.skill_id for link in agent.skill_links],
        knowledge_base_ids=[link.knowledge_base_id for link in agent.knowledge_links],
        data_asset_ids=[link.data_asset_id for link in agent.data_asset_links],
        data_source_ids=[link.data_source_id for link in agent.data_source_links],
        created_at=agent.created_at,
        updated_at=agent.updated_at,
    )


def _asset_read(asset: AgentDataAsset, db: Session) -> DataAssetRead:
    return DataAssetRead(
        id=asset.id,
        asset_code=asset.asset_code,
        table_name=asset.table_name,
        display_name=asset.display_name,
        description=asset.description,
        allowed_columns=asset.allowed_columns or [],
        columns=table_columns(asset.table_name, db.get_bind()),
        governance_status=asset.governance_status,
        source_health=asset.source_health,
        last_governed_at=asset.last_governed_at,
        governance_report_json=asset.governance_report_json or {},
        row_count=asset.row_count,
        enabled=asset.enabled,
        last_inspected_at=asset.last_inspected_at,
        created_at=asset.created_at,
        updated_at=asset.updated_at,
    )


@router.get("/overview")
def resource_overview(db: Session = Depends(get_db)) -> dict[str, int]:
    seed_default_data_assets(db)
    return {
        "providers": int(db.scalar(select(func.count(ModelProvider.id))) or 0),
        "instances": int(db.scalar(select(func.count(ModelInstance.id))) or 0),
        "skills": int(db.scalar(select(func.count(ModelSkill.id))) or 0),
        "agents": int(db.scalar(select(func.count(AgentDefinition.id))) or 0),
        "data_assets": int(db.scalar(select(func.count(AgentDataAsset.id))) or 0),
        "knowledge_bases": int(db.scalar(select(func.count(KnowledgeBase.id))) or 0),
    }


@router.get("/agents", response_model=list[AgentRead])
def list_agents(db: Session = Depends(get_db)) -> list[AgentRead]:
    return [_agent_read(item) for item in db.scalars(select(AgentDefinition).order_by(AgentDefinition.agent_code)).all()]


@router.get("/agents/options", response_model=list[AgentSummary])
def list_agent_options(db: Session = Depends(get_db)) -> list[AgentSummary]:
    return [
        AgentSummary(
            id=item.id,
            agent_code=item.agent_code,
            display_name=item.display_name,
            enabled=item.enabled,
            model_instance_code=item.model_instance_code,
        )
        for item in db.scalars(select(AgentDefinition).order_by(AgentDefinition.agent_code)).all()
    ]


def _validate_agent_model(db: Session, model_instance_code: str | None) -> None:
    if model_instance_code and not db.scalar(
        select(ModelInstance).where(ModelInstance.instance_code == model_instance_code)
    ):
        raise HTTPException(status_code=404, detail="Model instance not found")


def _validate_output_schema(schema: dict) -> None:
    if schema:
        try:
            Draft202012Validator.check_schema(schema)
        except SchemaError as exc:
            raise HTTPException(status_code=422, detail=f"Invalid JSON output schema: {exc.message}") from exc


@router.post("/agents", response_model=AgentRead, status_code=status.HTTP_201_CREATED)
def create_agent(payload: AgentCreate, db: Session = Depends(get_db)) -> AgentRead:
    _validate_agent_model(db, payload.model_instance_code)
    _validate_output_schema(payload.json_schema_output)
    agent = AgentDefinition(
            agent_code=payload.agent_code or _next_agent_code(db),
        display_name=payload.display_name,
        system_prompt=payload.system_prompt,
        model_instance_code=payload.model_instance_code,
        max_iterations=payload.max_iterations,
        context_window_limit=payload.context_window_limit,
        json_schema_output=payload.json_schema_output,
        enabled=payload.enabled,
        description=payload.description,
        version=payload.version,
    )
    db.add(agent)
    try:
        db.flush()
        set_agent_links(
            db,
            agent,
            payload.child_agent_ids,
            payload.skill_ids,
            payload.knowledge_base_ids,
            payload.data_asset_ids,
            payload.data_source_ids,
        )
        db.commit()
    except ValueError as exc:
        db.rollback()
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except Exception as exc:
        db.rollback()
        if "UNIQUE" in str(exc).upper():
            raise HTTPException(status_code=409, detail="Agent code already exists") from exc
        raise
    db.refresh(agent)
    return _agent_read(agent)


@router.put("/agents/{agent_id}", response_model=AgentRead)
def update_agent(agent_id: int, payload: AgentUpdate, db: Session = Depends(get_db)) -> AgentRead:
    agent = db.get(AgentDefinition, agent_id)
    if not agent:
        raise HTTPException(status_code=404, detail="Agent not found")
    values = payload.model_dump(exclude_unset=True)
    _validate_agent_model(db, values.get("model_instance_code", agent.model_instance_code))
    _validate_output_schema(values.get("json_schema_output", agent.json_schema_output or {}))
    for field in ("agent_code", "display_name", "system_prompt", "model_instance_code", "max_iterations", "context_window_limit", "json_schema_output", "enabled", "description", "version"):
        if field in values:
            setattr(agent, field, values[field])
    try:
        set_agent_links(
            db,
            agent,
            values.get("child_agent_ids", [link.child_agent_id for link in agent.child_links]),
            values.get("skill_ids", [link.skill_id for link in agent.skill_links]),
            values.get("knowledge_base_ids", [link.knowledge_base_id for link in agent.knowledge_links]),
            values.get("data_asset_ids", [link.data_asset_id for link in agent.data_asset_links]),
            values.get("data_source_ids", [link.data_source_id for link in agent.data_source_links]),
        )
        db.commit()
    except ValueError as exc:
        db.rollback()
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    db.refresh(agent)
    return _agent_read(agent)


@router.post("/agents/{agent_id}/run", response_model=ModelChatResponse)
def run_agent(agent_id: int, payload: AgentRunRequest, db: Session = Depends(get_db)) -> ModelChatResponse:
    try:
        return AgentExecutionWorkflow(db).execute(
            AgentExecutionCommand(
                agent_id=agent_id,
                task_type=payload.task_type,
                messages=[item.model_dump() for item in payload.messages],
            )
        )
    except AgentUnavailableError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except AgentOutputError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc


@router.delete("/agents/{agent_id}", status_code=204)
def delete_agent(agent_id: int, db: Session = Depends(get_db)) -> Response:
    agent = db.get(AgentDefinition, agent_id)
    if not agent:
        raise HTTPException(status_code=404, detail="Agent not found")
    from app.models.ai_hub import AgentChildLink

    incoming = db.scalar(select(func.count(AgentChildLink.id)).where(AgentChildLink.child_agent_id == agent_id)) or 0
    if incoming:
        raise HTTPException(status_code=409, detail={"message": "Agent is referenced as a child agent", "dependent_count": incoming})
    db.delete(agent)
    db.commit()
    return Response(status_code=204)


@router.get("/data-assets", response_model=list[DataAssetRead])
def list_data_assets(db: Session = Depends(get_db)) -> list[DataAssetRead]:
    seed_default_data_assets(db)
    rows = db.scalars(select(AgentDataAsset).order_by(AgentDataAsset.asset_code)).all()
    changed = False
    for row in rows:
        before = (row.row_count, row.source_health, row.allowed_columns)
        inspect_data_asset(row, db.get_bind())
        after = (row.row_count, row.source_health, row.allowed_columns)
        changed = changed or before != after
    if changed:
        db.commit()
    return [_asset_read(item, db) for item in rows]


@router.get("/data-assets/source-tables", response_model=list[str])
def list_source_tables(db: Session = Depends(get_db)) -> list[str]:
    return available_source_tables(db.get_bind())


@router.post("/data-assets", response_model=DataAssetRead, status_code=201)
def create_data_asset(payload: DataAssetCreate, db: Session = Depends(get_db)) -> DataAssetRead:
    if payload.table_name not in available_source_tables(db.get_bind()):
        raise HTTPException(status_code=422, detail="Choose an existing business data source")
    if db.scalar(select(AgentDataAsset.id).where(
        (AgentDataAsset.asset_code == payload.asset_code) | (AgentDataAsset.table_name == payload.table_name)
    )):
        raise HTTPException(status_code=409, detail="Data asset code or source table already exists")
    asset = AgentDataAsset(**payload.model_dump(), governance_status="PENDING")
    db.add(asset)
    inspect_data_asset(asset, db.get_bind())
    db.commit()
    db.refresh(asset)
    return _asset_read(asset, db)


@router.put("/data-assets/{asset_id}", response_model=DataAssetRead)
def update_data_asset(asset_id: int, payload: DataAssetUpdate, db: Session = Depends(get_db)) -> DataAssetRead:
    asset = db.get(AgentDataAsset, asset_id)
    if not asset:
        raise HTTPException(status_code=404, detail="Data asset not found")
    values = payload.model_dump(exclude_unset=True)
    if asset.governance_status == "LOCKED" and "allowed_columns" in values and values["allowed_columns"] != asset.allowed_columns:
        raise HTTPException(status_code=409, detail="Unlock the data asset before changing governed columns")
    if "allowed_columns" in values and values["allowed_columns"] != asset.allowed_columns:
        asset.governance_status = "PENDING"
    for field, value in values.items():
        setattr(asset, field, value)
    inspect_data_asset(asset, db.get_bind())
    db.commit()
    db.refresh(asset)
    return _asset_read(asset, db)


@router.post("/data-assets/{asset_id}/inspect", response_model=DataAssetRead)
def inspect_asset(asset_id: int, db: Session = Depends(get_db)) -> DataAssetRead:
    asset = db.get(AgentDataAsset, asset_id)
    if not asset:
        raise HTTPException(status_code=404, detail="Data asset not found")
    inspect_data_asset(asset, db.get_bind())
    db.commit()
    db.refresh(asset)
    return _asset_read(asset, db)


@router.get("/data-assets/{asset_id}/preview")
def preview_data_asset(asset_id: int, limit: int = 20, db: Session = Depends(get_db)) -> dict:
    asset = db.get(AgentDataAsset, asset_id)
    if not asset:
        raise HTTPException(status_code=404, detail="Data asset not found")
    if not 1 <= limit <= 100:
        raise HTTPException(status_code=422, detail="limit must be between 1 and 100")
    try:
        return jsonable_encoder(preview_table(asset.table_name, asset.allowed_columns, limit, db.get_bind()))
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc


@router.put("/data-assets/{asset_id}/governance-state", response_model=DataAssetRead)
def set_asset_governance_state(asset_id: int, payload: GovernanceStateUpdate,
                               db: Session = Depends(get_db)) -> DataAssetRead:
    asset = db.get(AgentDataAsset, asset_id)
    if not asset:
        raise HTTPException(status_code=404, detail="Data asset not found")
    if payload.governance_status not in GOVERNANCE_STATES:
        raise HTTPException(status_code=422, detail="Invalid governance state")
    if payload.governance_status == "GOVERNED" and not asset.last_governed_at:
        raise HTTPException(status_code=409, detail="Govern the data asset before marking it governed")
    if payload.governance_status == "PENDING" and asset.governance_status == "LOCKED":
        asset.governance_status = "GOVERNED" if asset.last_governed_at else "PENDING"
    elif payload.governance_status == "GOVERNED" and asset.governance_status == "LOCKED":
        asset.governance_status = "GOVERNED"
    else:
        asset.governance_status = payload.governance_status
    db.commit()
    return _asset_read(asset, db)


@router.post("/data-assets/{asset_id}/govern", response_model=GovernanceRunRead)
async def govern_one_asset(
    asset_id: int, payload: GovernanceRequest, db: Session = Depends(get_db)
) -> GovernanceRun:
    asset = db.get(AgentDataAsset, asset_id)
    if not asset:
        raise HTTPException(status_code=404, detail="Data asset not found")
    try:
        return await govern_asset(
            db, asset, payload.source_asset_ids, payload.agent_id, payload.record_limit
        )
    except ValueError as exc:
        raise HTTPException(status_code=409, detail=str(exc)) from exc


@router.post("/data-assets/govern/batch")
async def govern_assets_batch(
    payload: GovernanceBatchRequest, db: Session = Depends(get_db)
) -> dict:
    results = []
    for asset_id in dict.fromkeys(payload.target_ids):
        asset = db.get(AgentDataAsset, asset_id)
        if not asset or asset.governance_status == "LOCKED":
            results.append({"target_id": asset_id, "status": "SKIPPED", "message": "Not found or locked"})
            continue
        try:
            run = await govern_asset(
                db, asset, payload.source_asset_ids, payload.agent_id, payload.record_limit
            )
            results.append({"target_id": asset_id, "status": run.status, "run_id": run.id})
        except Exception as exc:
            results.append({"target_id": asset_id, "status": "FAILED", "message": str(exc)})
    return {"results": results}


@router.get("/governance-runs", response_model=list[GovernanceRunRead])
def list_governance_runs(target_type: str, target_id: int, limit: int = 20,
                         db: Session = Depends(get_db)) -> list[GovernanceRun]:
    if target_type not in {"ASSET", "GRAPH"} or not 1 <= limit <= 100:
        raise HTTPException(status_code=422, detail="Invalid governance run query")
    return list(db.scalars(select(GovernanceRun).where(
        GovernanceRun.target_type == target_type, GovernanceRun.target_id == target_id
    ).order_by(GovernanceRun.created_at.desc()).limit(limit)).all())


@router.get("/knowledge-bases", response_model=list[KnowledgeBaseRead])
def list_knowledge_bases(db: Session = Depends(get_db)) -> list[KnowledgeBase]:
    return list(db.scalars(select(KnowledgeBase).order_by(KnowledgeBase.kb_code)).all())


@router.post("/knowledge-bases", response_model=KnowledgeBaseRead, status_code=status.HTTP_201_CREATED)
def create_knowledge_base(payload: KnowledgeBaseCreate, db: Session = Depends(get_db)) -> KnowledgeBase:
    kb = KnowledgeBase(**payload.model_dump(), status="DRAFT")
    db.add(kb)
    try:
        db.commit()
    except Exception as exc:
        db.rollback()
        if "UNIQUE" in str(exc).upper():
            raise HTTPException(status_code=409, detail="Knowledge base code already exists") from exc
        raise
    db.refresh(kb)
    return kb


@router.put("/knowledge-bases/{kb_id}", response_model=KnowledgeBaseRead)
def update_knowledge_base(kb_id: int, payload: KnowledgeBaseUpdate, db: Session = Depends(get_db)) -> KnowledgeBase:
    kb = db.get(KnowledgeBase, kb_id)
    if not kb:
        raise HTTPException(status_code=404, detail="Knowledge base not found")
    values = payload.model_dump(exclude_unset=True)
    if "source_tables" in values and values["source_tables"] != kb.source_tables:
        graphs = list(db.scalars(select(KnowledgeGraph).where(KnowledgeGraph.knowledge_base_id == kb_id)).all())
        if any(graph.governance_status == "LOCKED" for graph in graphs):
            raise HTTPException(status_code=409, detail="Unlock the knowledge base's graphs before changing sources")
        for graph in graphs:
            graph.source_tables = [table for table in graph.source_tables if table in values["source_tables"]]
            graph.governance_status = "PENDING"
    for field, value in values.items():
        setattr(kb, field, value)
    db.commit()
    db.refresh(kb)
    return kb


@router.delete("/knowledge-bases/{kb_id}", status_code=204)
def delete_knowledge_base(kb_id: int, db: Session = Depends(get_db)) -> Response:
    kb = db.get(KnowledgeBase, kb_id)
    if not kb:
        raise HTTPException(status_code=404, detail="Knowledge base not found")
    dependent_count = db.scalar(
        select(func.count(AgentKnowledgeBaseLink.id)).where(AgentKnowledgeBaseLink.knowledge_base_id == kb_id)
    ) or 0
    if dependent_count:
        raise HTTPException(
            status_code=409,
            detail={"message": "Knowledge base is referenced by one or more agents.", "dependent_count": int(dependent_count)},
        )
    if db.scalar(select(func.count(KnowledgeGraph.id)).where(KnowledgeGraph.knowledge_base_id == kb_id)):
        raise HTTPException(status_code=409, detail="Delete the knowledge base's graphs first")
    db.execute(delete(KnowledgeRelation).where(KnowledgeRelation.knowledge_base_id == kb_id))
    db.execute(delete(KnowledgeEntity).where(KnowledgeEntity.knowledge_base_id == kb_id))
    db.execute(delete(KnowledgeDocument).where(KnowledgeDocument.knowledge_base_id == kb_id))
    db.delete(kb)
    db.commit()
    return Response(status_code=204)


@router.post("/knowledge-bases/{kb_id}/build", response_model=KnowledgeBuildResponse)
def build_kb(kb_id: int, db: Session = Depends(get_db)) -> KnowledgeBuildResponse:
    kb = db.get(KnowledgeBase, kb_id)
    if not kb:
        raise HTTPException(status_code=404, detail="Knowledge base not found")
    graph = db.scalar(select(KnowledgeGraph).where(KnowledgeGraph.graph_code == kb.kb_code))
    if graph and graph.governance_status == "LOCKED":
        raise HTTPException(status_code=409, detail="Knowledge graph is locked")
    try:
        stats = build_knowledge_base(db, kb)
    except Exception as exc:
        db.rollback()
        raise HTTPException(status_code=500, detail=f"Knowledge base build failed: {exc}") from exc
    return KnowledgeBuildResponse(
        knowledge_base_id=kb.id,
        status=kb.status,
        message="Knowledge base rebuilt from local data assets.",
        **stats,
    )


@router.get("/knowledge-bases/{kb_id}/search", response_model=list[KnowledgeSearchResult])
def search_kb(kb_id: int, q: str = "", limit: int = 20, db: Session = Depends(get_db)) -> list[KnowledgeSearchResult]:
    if limit < 1 or limit > 100:
        raise HTTPException(status_code=422, detail="limit must be between 1 and 100")
    kb = db.get(KnowledgeBase, kb_id)
    if not kb:
        raise HTTPException(status_code=404, detail="Knowledge base not found")
    query = q.strip()
    statement = select(KnowledgeDocument).where(KnowledgeDocument.knowledge_base_id == kb_id)
    if query:
        statement = statement.where(
            KnowledgeDocument.title.contains(query) | KnowledgeDocument.content.contains(query) | KnowledgeDocument.symbol.contains(query)
        )
    rows = db.scalars(statement.order_by(KnowledgeDocument.created_at.desc()).limit(limit)).all()
    return [
        KnowledgeSearchResult(
            document_id=row.id,
            source_table=row.source_table,
            symbol=row.symbol,
            title=row.title,
            content=row.content,
            metadata_json=row.metadata_json,
        )
        for row in rows
    ]


def _graph_or_404(db: Session, graph_id: int) -> KnowledgeGraph:
    graph = db.get(KnowledgeGraph, graph_id)
    if graph is None:
        raise HTTPException(status_code=404, detail="Knowledge graph not found")
    return graph


def _validate_graph_sources(kb: KnowledgeBase, source_tables: list[str]) -> None:
    if any(table not in kb.source_tables for table in source_tables):
        raise HTTPException(status_code=422, detail="Graph sources must be registered in the knowledge base")


@router.get("/knowledge-graphs", response_model=list[KnowledgeGraphRead])
def list_knowledge_graphs(db: Session = Depends(get_db)) -> list[KnowledgeGraph]:
    return list(db.scalars(select(KnowledgeGraph).order_by(KnowledgeGraph.graph_code)).all())


@router.post("/knowledge-graphs", response_model=KnowledgeGraphRead, status_code=201)
def create_knowledge_graph(payload: KnowledgeGraphCreate, db: Session = Depends(get_db)) -> KnowledgeGraph:
    kb = db.get(KnowledgeBase, payload.knowledge_base_id)
    if not kb:
        raise HTTPException(status_code=404, detail="Knowledge base not found")
    _validate_graph_sources(kb, payload.source_tables)
    if db.scalar(select(KnowledgeGraph.id).where(KnowledgeGraph.graph_code == payload.graph_code)):
        raise HTTPException(status_code=409, detail="Graph code already exists")
    graph = KnowledgeGraph(**payload.model_dump(), governance_status="PENDING")
    db.add(graph)
    db.commit()
    db.refresh(graph)
    return graph


@router.put("/knowledge-graphs/{graph_id}", response_model=KnowledgeGraphRead)
def update_knowledge_graph(graph_id: int, payload: KnowledgeGraphUpdate,
                           db: Session = Depends(get_db)) -> KnowledgeGraph:
    graph = _graph_or_404(db, graph_id)
    values = payload.model_dump(exclude_unset=True)
    if graph.governance_status == "LOCKED" and any(
        key in values and values[key] != getattr(graph, key) for key in ("symbol", "source_tables")
    ):
        raise HTTPException(status_code=409, detail="Unlock the graph before editing its configuration")
    kb = db.get(KnowledgeBase, graph.knowledge_base_id)
    if "source_tables" in values and kb:
        _validate_graph_sources(kb, values["source_tables"] or [])
    if any(key in values and values[key] != getattr(graph, key) for key in ("symbol", "source_tables")):
        graph.governance_status = "PENDING"
    for key, value in values.items():
        setattr(graph, key, value)
    db.commit()
    db.refresh(graph)
    return graph


@router.delete("/knowledge-graphs/{graph_id}", status_code=204)
def delete_knowledge_graph(graph_id: int, db: Session = Depends(get_db)) -> Response:
    graph = _graph_or_404(db, graph_id)
    if graph.governance_status == "LOCKED":
        raise HTTPException(status_code=409, detail="Unlock the graph before deleting it")
    kb_id = graph.knowledge_base_id
    db.execute(delete(KnowledgeRelation).where(KnowledgeRelation.graph_id == graph_id))
    db.execute(delete(KnowledgeEntity).where(KnowledgeEntity.graph_id == graph_id))
    db.execute(delete(KnowledgeDocument).where(KnowledgeDocument.graph_id == graph_id))
    db.delete(graph)
    kb = db.get(KnowledgeBase, kb_id)
    if kb:
        kb.entity_count = int(db.scalar(select(func.count(KnowledgeEntity.id)).where(KnowledgeEntity.knowledge_base_id == kb_id)) or 0)
        kb.relation_count = int(db.scalar(select(func.count(KnowledgeRelation.id)).where(KnowledgeRelation.knowledge_base_id == kb_id)) or 0)
    db.commit()
    return Response(status_code=204)


@router.put("/knowledge-graphs/{graph_id}/governance-state", response_model=KnowledgeGraphRead)
def set_graph_governance_state(graph_id: int, payload: GovernanceStateUpdate,
                               db: Session = Depends(get_db)) -> KnowledgeGraph:
    graph = _graph_or_404(db, graph_id)
    if payload.governance_status not in GOVERNANCE_STATES:
        raise HTTPException(status_code=422, detail="Invalid governance state")
    if payload.governance_status == "GOVERNED" and not graph.last_governed_at:
        raise HTTPException(status_code=409, detail="Govern the graph before marking it governed")
    if graph.governance_status == "LOCKED" and payload.governance_status == "PENDING":
        graph.governance_status = "GOVERNED" if graph.last_governed_at else "PENDING"
    else:
        graph.governance_status = payload.governance_status
    db.commit()
    return graph


@router.post("/knowledge-graphs/{graph_id}/govern", response_model=GovernanceRunRead)
def govern_one_graph(graph_id: int, payload: GovernanceRequest, db: Session = Depends(get_db)) -> GovernanceRun:
    graph = _graph_or_404(db, graph_id)
    try:
        return govern_graph(db, graph, payload.source_asset_ids, payload.agent_id)
    except ValueError as exc:
        raise HTTPException(status_code=409, detail=str(exc)) from exc


@router.post("/knowledge-graphs/govern/batch")
def govern_graphs_batch(payload: GovernanceBatchRequest, db: Session = Depends(get_db)) -> dict:
    results = []
    for graph_id in dict.fromkeys(payload.target_ids):
        graph = db.get(KnowledgeGraph, graph_id)
        if not graph or graph.governance_status == "LOCKED":
            results.append({"target_id": graph_id, "status": "SKIPPED", "message": "Not found or locked"})
            continue
        try:
            run = govern_graph(db, graph, payload.source_asset_ids, payload.agent_id)
            results.append({"target_id": graph_id, "status": "SUCCESS", "run_id": run.id})
        except Exception as exc:
            results.append({"target_id": graph_id, "status": "FAILED", "message": str(exc)})
    return {"results": results}


@router.get("/knowledge-graphs/{graph_id}/explore")
def explore_knowledge_graph(graph_id: int, q: str = "", limit: int = 30,
                            db: Session = Depends(get_db)) -> dict:
    _graph_or_404(db, graph_id)
    if not 1 <= limit <= 100:
        raise HTTPException(status_code=422, detail="limit must be between 1 and 100")
    statement = select(KnowledgeEntity).where(KnowledgeEntity.graph_id == graph_id)
    if q.strip():
        statement = statement.where(KnowledgeEntity.entity_name.contains(q.strip()) |
                                    KnowledgeEntity.entity_key.contains(q.strip()))
    selected = list(db.scalars(statement.order_by(KnowledgeEntity.id).limit(limit)).all())
    ids = [item.id for item in selected]
    relations = list(db.scalars(select(KnowledgeRelation).where(
        KnowledgeRelation.graph_id == graph_id,
        (KnowledgeRelation.subject_entity_id.in_(ids) | KnowledgeRelation.object_entity_id.in_(ids))
    ).order_by(KnowledgeRelation.id).limit(limit * 3)).all()) if ids else []
    all_ids = set(ids)
    for relation in relations:
        all_ids.update((relation.subject_entity_id, relation.object_entity_id))
    nodes = list(db.scalars(select(KnowledgeEntity).where(KnowledgeEntity.id.in_(all_ids))).all()) if all_ids else []
    evidence_ids = [relation.evidence_document_id for relation in relations if relation.evidence_document_id]
    evidence = {item.id: item for item in db.scalars(select(KnowledgeDocument).where(
        KnowledgeDocument.id.in_(evidence_ids))).all()} if evidence_ids else {}
    return {"nodes": [{"id": node.id, "type": node.entity_type, "key": node.entity_key,
                        "name": node.entity_name, "properties": node.properties_json} for node in nodes],
            "relations": [{"id": relation.id, "from_id": relation.subject_entity_id,
                           "to_id": relation.object_entity_id, "type": relation.predicate,
                           "evidence": ({"document_id": evidence[relation.evidence_document_id].id,
                                         "title": evidence[relation.evidence_document_id].title,
                                         "source_table": evidence[relation.evidence_document_id].source_table,
                                         "excerpt": evidence[relation.evidence_document_id].content[:500]}
                                        if relation.evidence_document_id in evidence else None)}
                          for relation in relations]}


@router.get("/skills/{skill_id}/file", response_model=ModelSkillRead)
def get_skill_file(skill_id: int, db: Session = Depends(get_db)) -> ModelSkill:
    skill = db.get(ModelSkill, skill_id)
    if not skill:
        raise HTTPException(status_code=404, detail="Skill not found")
    path = skill_file_path(skill.skill_code)
    if path.exists():
        skill.instructions = path.read_text(encoding="utf-8")
    return skill
def _next_agent_code(db: "Session") -> str:
    """Return the next stable, human-readable agent identifier."""
    index = int(db.scalar(select(func.count(AgentDefinition.id))) or 0) + 1
    while True:
        code = f"AGENT_{index:04d}"
        if not db.scalar(select(AgentDefinition.id).where(AgentDefinition.agent_code == code)):
            return code
        index += 1
