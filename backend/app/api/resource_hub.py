from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException, Response, status
from sqlalchemy import delete, func, select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.ai_hub import (
    AgentDataAsset,
    AgentDataAssetLink,
    AgentDefinition,
    AgentKnowledgeBaseLink,
    AgentSkillLink,
    KnowledgeBase,
    KnowledgeDocument,
    KnowledgeEntity,
    KnowledgeRelation,
    ModelInstance,
    ModelProvider,
    ModelSkill,
)
from app.schemas.model_hub import ModelSkillRead
from app.schemas.resource_hub import (
    AgentCreate,
    AgentRead,
    AgentSummary,
    AgentUpdate,
    DataAssetRead,
    DataAssetUpdate,
    KnowledgeBaseCreate,
    KnowledgeBaseRead,
    KnowledgeBaseUpdate,
    KnowledgeBuildResponse,
    KnowledgeSearchResult,
)
from app.services.resource_hub import (
    build_knowledge_base,
    inspect_data_asset,
    seed_default_data_assets,
    set_agent_links,
    table_columns,
)
from app.services.skill_files import delete_skill_file, skill_file_path, write_skill_file

router = APIRouter(prefix="/resources", tags=["AI Resource Hub"])


def _agent_read(agent: AgentDefinition) -> AgentRead:
    return AgentRead(
        id=agent.id,
        agent_code=agent.agent_code,
        display_name=agent.display_name,
        system_prompt=agent.system_prompt,
        model_instance_code=agent.model_instance_code,
        max_iterations=agent.max_iterations,
        enabled=agent.enabled,
        description=agent.description,
        version=agent.version,
        child_agent_ids=[link.child_agent_id for link in sorted(agent.child_links, key=lambda item: item.order_index)],
        skill_ids=[link.skill_id for link in agent.skill_links],
        knowledge_base_ids=[link.knowledge_base_id for link in agent.knowledge_links],
        data_asset_ids=[link.data_asset_id for link in agent.data_asset_links],
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
        columns=table_columns(asset.table_name),
        governance_status=asset.governance_status,
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


@router.post("/agents", response_model=AgentRead, status_code=status.HTTP_201_CREATED)
def create_agent(payload: AgentCreate, db: Session = Depends(get_db)) -> AgentRead:
    _validate_agent_model(db, payload.model_instance_code)
    agent = AgentDefinition(
        agent_code=payload.agent_code,
        display_name=payload.display_name,
        system_prompt=payload.system_prompt,
        model_instance_code=payload.model_instance_code,
        max_iterations=payload.max_iterations,
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
    for field in ("agent_code", "display_name", "system_prompt", "model_instance_code", "max_iterations", "enabled", "description", "version"):
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
        )
        db.commit()
    except ValueError as exc:
        db.rollback()
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    db.refresh(agent)
    return _agent_read(agent)


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
        before = (row.row_count, row.governance_status, row.allowed_columns, row.last_inspected_at)
        inspect_data_asset(row)
        after = (row.row_count, row.governance_status, row.allowed_columns, row.last_inspected_at)
        changed = changed or before != after
    if changed:
        db.commit()
    return [_asset_read(item, db) for item in rows]


@router.put("/data-assets/{asset_id}", response_model=DataAssetRead)
def update_data_asset(asset_id: int, payload: DataAssetUpdate, db: Session = Depends(get_db)) -> DataAssetRead:
    asset = db.get(AgentDataAsset, asset_id)
    if not asset:
        raise HTTPException(status_code=404, detail="Data asset not found")
    for field, value in payload.model_dump(exclude_unset=True).items():
        setattr(asset, field, value)
    inspect_data_asset(asset)
    db.commit()
    db.refresh(asset)
    return _asset_read(asset, db)


@router.post("/data-assets/{asset_id}/inspect", response_model=DataAssetRead)
def inspect_asset(asset_id: int, db: Session = Depends(get_db)) -> DataAssetRead:
    asset = db.get(AgentDataAsset, asset_id)
    if not asset:
        raise HTTPException(status_code=404, detail="Data asset not found")
    inspect_data_asset(asset)
    db.commit()
    db.refresh(asset)
    return _asset_read(asset, db)


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
    for field, value in payload.model_dump(exclude_unset=True).items():
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
def search_kb(kb_id: int, q: str, limit: int = 20, db: Session = Depends(get_db)) -> list[KnowledgeSearchResult]:
    if limit < 1 or limit > 100:
        raise HTTPException(status_code=422, detail="limit must be between 1 and 100")
    kb = db.get(KnowledgeBase, kb_id)
    if not kb:
        raise HTTPException(status_code=404, detail="Knowledge base not found")
    query = q.strip()
    if not query:
        return []
    rows = db.scalars(
        select(KnowledgeDocument)
        .where(
            KnowledgeDocument.knowledge_base_id == kb_id,
            (KnowledgeDocument.title.contains(query) | KnowledgeDocument.content.contains(query) | KnowledgeDocument.symbol.contains(query)),
        )
        .order_by(KnowledgeDocument.created_at.desc())
        .limit(limit)
    ).all()
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


@router.get("/skills/{skill_id}/file", response_model=ModelSkillRead)
def get_skill_file(skill_id: int, db: Session = Depends(get_db)) -> ModelSkill:
    skill = db.get(ModelSkill, skill_id)
    if not skill:
        raise HTTPException(status_code=404, detail="Skill not found")
    path = skill_file_path(skill.skill_code)
    if path.exists():
        skill.instructions = path.read_text(encoding="utf-8")
    return skill
