from fastapi import APIRouter, Depends, HTTPException, Response, status
from sqlalchemy import func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.ai_hub import AgentSkillLink, ModelCallLog, ModelInstance, ModelProvider, ModelRouteRule, ModelSkill
from app.schemas.model_hub import (
    ModelCallLogRead,
    ModelChatRequest,
    ModelChatResponse,
    ModelInstanceCreate,
    ModelInstanceRead,
    ModelInstanceUpdate,
    ModelProviderCreate,
    ModelProviderRead,
    ModelProviderUpdate,
    ModelRouteRuleCreate,
    ModelRouteRuleRead,
    ModelRouteRuleUpdate,
    ModelSkillCreate,
    ModelSkillRead,
    ModelSkillUpdate,
    ModelTestResponse,
)
from app.services.model_hub import ModelHubService
from app.connectors.model_registry import get_model_adapter
from app.services.skill_files import delete_skill_file, write_skill_file

router = APIRouter(prefix="/model-hub", tags=["Model Hub"])


_SECRET_CONFIG_KEYS = {"api_key", "apikey", "api-key", "access_token", "token", "secret", "password"}


def _redact_config(value):
    if isinstance(value, dict):
        return {
            key: ("***" if key.lower() in _SECRET_CONFIG_KEYS else _redact_config(item))
            for key, item in value.items()
        }
    if isinstance(value, list):
        return [_redact_config(item) for item in value]
    return value


def _provider_read(provider: ModelProvider) -> ModelProviderRead:
    data = ModelProviderRead.model_validate(provider)
    data.config_json = _redact_config(provider.config_json or {})
    return data


def _provider_or_404(db: Session, provider_id: int) -> ModelProvider:
    provider = db.get(ModelProvider, provider_id)
    if not provider:
        raise HTTPException(status_code=404, detail="Model provider not found")
    return provider


def _instance_or_404(db: Session, instance_id: int) -> ModelInstance:
    instance = db.get(ModelInstance, instance_id)
    if not instance:
        raise HTTPException(status_code=404, detail="Model instance not found")
    return instance


def _instance_read(instance: ModelInstance) -> ModelInstanceRead:
    data = ModelInstanceRead.model_validate(instance)
    data.config_json = _redact_config(instance.config_json or {})
    data.api_key_configured = bool(instance.api_key)
    return data


def _skill_or_404(db: Session, skill_id: int) -> ModelSkill:
    skill = db.get(ModelSkill, skill_id)
    if not skill:
        raise HTTPException(status_code=404, detail="Model skill not found")
    return skill


@router.get("/providers", response_model=list[ModelProviderRead])
def list_providers(db: Session = Depends(get_db)) -> list[ModelProviderRead]:
    return [_provider_read(item) for item in db.scalars(select(ModelProvider).order_by(ModelProvider.provider_code)).all()]


@router.post("/providers", response_model=ModelProviderRead, status_code=status.HTTP_201_CREATED)
def create_provider(payload: ModelProviderCreate, db: Session = Depends(get_db)) -> ModelProvider:
    provider = ModelProvider(**payload.model_dump())
    db.add(provider)
    try:
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        raise HTTPException(status_code=409, detail="Provider code already exists") from exc
    db.refresh(provider)
    return _provider_read(provider)


@router.put("/providers/{provider_id}", response_model=ModelProviderRead)
def update_provider(
    provider_id: int,
    payload: ModelProviderUpdate,
    db: Session = Depends(get_db),
) -> ModelProvider:
    provider = _provider_or_404(db, provider_id)
    for field_name, value in payload.model_dump(exclude_unset=True).items():
        setattr(provider, field_name, value)
    db.commit()
    db.refresh(provider)
    return _provider_read(provider)


@router.delete("/providers/{provider_id}", status_code=204)
def delete_provider(provider_id: int, db: Session = Depends(get_db)) -> Response:
    provider = _provider_or_404(db, provider_id)
    instance_count = db.scalar(select(func.count(ModelInstance.id)).where(ModelInstance.provider_id == provider.id)) or 0
    if instance_count:
        raise HTTPException(
            status_code=409,
            detail={"message": "Provider still has model instances; disable it or remove instances first.", "dependent_count": int(instance_count)},
        )
    db.delete(provider)
    db.commit()
    return Response(status_code=204)


@router.post("/providers/{provider_id}/test", response_model=ModelTestResponse)
def test_provider(provider_id: int, db: Session = Depends(get_db)) -> ModelTestResponse:
    provider = _provider_or_404(db, provider_id)
    instance = db.scalar(
        select(ModelInstance)
        .where(ModelInstance.provider_id == provider.id, ModelInstance.enabled.is_(True))
        .order_by(ModelInstance.id)
    )
    if not instance:
        try:
            message, _ = get_model_adapter(provider.provider_type).health_check(
                ModelInstance(
                    provider_id=provider.id,
                    instance_code="PROVIDER_TEST",
                    model_code="provider-test",
                    model_name="Provider Test",
                )
            )
        except Exception as exc:
            raise HTTPException(status_code=400, detail=str(exc)) from exc
        return ModelTestResponse(
            call_log_id=0,
            provider_code=provider.provider_code,
            instance_code=None,
            model_code=None,
            status="CONFIGURED",
            message=message,
        )
    log = ModelHubService(db).test_instance(instance.instance_code)
    return ModelTestResponse(
        call_log_id=log.id,
        provider_code=log.provider_code,
        instance_code=log.instance_code,
        model_code=log.model_code,
        status=log.status,
        message="Model provider test completed.",
        response_text=log.response_text,
    )


@router.get("/instances", response_model=list[ModelInstanceRead])
def list_instances(
    provider_id: int | None = None,
    enabled: bool | None = None,
    db: Session = Depends(get_db),
) -> list[ModelInstanceRead]:
    statement = select(ModelInstance).order_by(ModelInstance.instance_code)
    if provider_id is not None:
        statement = statement.where(ModelInstance.provider_id == provider_id)
    if enabled is not None:
        statement = statement.where(ModelInstance.enabled == enabled)
    return [_instance_read(item) for item in db.scalars(statement).all()]


@router.post("/instances", response_model=ModelInstanceRead, status_code=status.HTTP_201_CREATED)
def create_instance(payload: ModelInstanceCreate, db: Session = Depends(get_db)) -> ModelInstanceRead:
    _provider_or_404(db, payload.provider_id)
    instance = ModelInstance(**payload.model_dump())
    db.add(instance)
    try:
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        raise HTTPException(status_code=409, detail="Model instance code already exists") from exc
    db.refresh(instance)
    return _instance_read(instance)


@router.put("/instances/{instance_id}", response_model=ModelInstanceRead)
def update_instance(
    instance_id: int,
    payload: ModelInstanceUpdate,
    db: Session = Depends(get_db),
) -> ModelInstanceRead:
    instance = _instance_or_404(db, instance_id)
    values = payload.model_dump(exclude_unset=True)
    if "provider_id" in values:
        _provider_or_404(db, values["provider_id"])
    for field_name, value in values.items():
        setattr(instance, field_name, value)
    db.commit()
    db.refresh(instance)
    return _instance_read(instance)


@router.delete("/instances/{instance_id}", status_code=204)
def delete_instance(instance_id: int, db: Session = Depends(get_db)) -> Response:
    instance = _instance_or_404(db, instance_id)
    routes = list(db.scalars(select(ModelRouteRule)).all())
    route_count = sum(
        1
        for route in routes
        if route.preferred_instance_code == instance.instance_code
        or instance.instance_code in (route.fallback_chain_json or [])
    )
    if route_count:
        raise HTTPException(
            status_code=409,
            detail={"message": "Model instance is referenced by one or more routes.", "dependent_count": int(route_count)},
        )
    db.delete(instance)
    db.commit()
    return Response(status_code=204)


@router.post("/instances/{instance_id}/test", response_model=ModelTestResponse)
def test_instance(instance_id: int, db: Session = Depends(get_db)) -> ModelTestResponse:
    instance = _instance_or_404(db, instance_id)
    log = ModelHubService(db).test_instance(instance.instance_code)
    return ModelTestResponse(
        call_log_id=log.id,
        provider_code=log.provider_code,
        instance_code=log.instance_code,
        model_code=log.model_code,
        status=log.status,
        message="Model instance test completed.",
        response_text=log.response_text or log.error_message,
    )


@router.get("/routes", response_model=list[ModelRouteRuleRead])
def list_routes(db: Session = Depends(get_db)) -> list[ModelRouteRule]:
    return list(db.scalars(select(ModelRouteRule).order_by(ModelRouteRule.task_type)).all())


@router.post("/routes", response_model=ModelRouteRuleRead, status_code=status.HTTP_201_CREATED)
def create_route(payload: ModelRouteRuleCreate, db: Session = Depends(get_db)) -> ModelRouteRule:
    if not db.scalar(select(ModelInstance).where(ModelInstance.instance_code == payload.preferred_instance_code)):
        raise HTTPException(status_code=404, detail="Preferred model instance not found")
    route = ModelRouteRule(**payload.model_dump())
    db.add(route)
    try:
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        raise HTTPException(status_code=409, detail="Task route already exists") from exc
    db.refresh(route)
    return route


@router.put("/routes/{route_id}", response_model=ModelRouteRuleRead)
def update_route(
    route_id: int,
    payload: ModelRouteRuleUpdate,
    db: Session = Depends(get_db),
) -> ModelRouteRule:
    route = db.get(ModelRouteRule, route_id)
    if not route:
        raise HTTPException(status_code=404, detail="Model route not found")
    values = payload.model_dump(exclude_unset=True)
    if "preferred_instance_code" in values and not db.scalar(
        select(ModelInstance).where(ModelInstance.instance_code == values["preferred_instance_code"])
    ):
        raise HTTPException(status_code=404, detail="Preferred model instance not found")
    for field_name, value in values.items():
        setattr(route, field_name, value)
    db.commit()
    db.refresh(route)
    return route


@router.get("/skills", response_model=list[ModelSkillRead])
def list_skills(enabled: bool | None = None, db: Session = Depends(get_db)) -> list[ModelSkill]:
    statement = select(ModelSkill).order_by(ModelSkill.skill_code)
    if enabled is not None:
        statement = statement.where(ModelSkill.enabled == enabled)
    return list(db.scalars(statement).all())


@router.post("/skills", response_model=ModelSkillRead, status_code=status.HTTP_201_CREATED)
def create_skill(payload: ModelSkillCreate, db: Session = Depends(get_db)) -> ModelSkill:
    skill = ModelSkill(**payload.model_dump())
    db.add(skill)
    try:
        db.flush()
        file_path, content_hash = write_skill_file(skill.skill_code, skill.instructions)
        skill.file_path = file_path
        skill.content_hash = content_hash
        skill.format = "MD"
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        raise HTTPException(status_code=409, detail="Skill code already exists") from exc
    db.refresh(skill)
    return skill


@router.put("/skills/{skill_id}", response_model=ModelSkillRead)
def update_skill(skill_id: int, payload: ModelSkillUpdate, db: Session = Depends(get_db)) -> ModelSkill:
    skill = _skill_or_404(db, skill_id)
    previous_skill_code = skill.skill_code
    values = payload.model_dump(exclude_unset=True)
    for field_name, value in values.items():
        setattr(skill, field_name, value)
    try:
        if "instructions" in values:
            file_path, content_hash = write_skill_file(skill.skill_code, str(values["instructions"]))
            skill.file_path = file_path
            skill.content_hash = content_hash
            skill.format = "MD"
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        raise HTTPException(status_code=409, detail="Skill code already exists") from exc
    if "skill_code" in values and values["skill_code"] != previous_skill_code:
        delete_skill_file(previous_skill_code)
    db.refresh(skill)
    return skill


@router.delete("/skills/{skill_id}", status_code=204)
def delete_skill(skill_id: int, db: Session = Depends(get_db)) -> Response:
    skill = _skill_or_404(db, skill_id)
    dependent_count = db.scalar(select(func.count(AgentSkillLink.id)).where(AgentSkillLink.skill_id == skill.id)) or 0
    if dependent_count:
        raise HTTPException(
            status_code=409,
            detail={"message": "Skill is referenced by one or more agents.", "dependent_count": int(dependent_count)},
        )
    skill_code = skill.skill_code
    db.delete(skill)
    db.commit()
    delete_skill_file(skill_code)
    return Response(status_code=204)


@router.post("/chat", response_model=ModelChatResponse)
def chat(payload: ModelChatRequest, db: Session = Depends(get_db)) -> ModelChatResponse:
    log = ModelHubService(db).chat(
        task_type=payload.task_type,
        instance_code=payload.instance_code,
        messages=[item.model_dump() for item in payload.messages],
        temperature=payload.temperature,
        max_tokens=payload.max_tokens,
        metadata_json=payload.metadata_json,
    )
    return ModelChatResponse(
        call_log_id=log.id,
        task_type=log.task_type,
        provider_code=log.provider_code,
        instance_code=log.instance_code,
        model_code=log.model_code,
        status=log.status,
        response_text=log.response_text or log.error_message or "",
        response_json=log.response_json,
    )


@router.get("/call-logs", response_model=list[ModelCallLogRead])
def list_call_logs(limit: int = 50, db: Session = Depends(get_db)) -> list[ModelCallLog]:
    if limit < 1 or limit > 500:
        raise HTTPException(status_code=422, detail="limit must be between 1 and 500")
    return list(db.scalars(select(ModelCallLog).order_by(ModelCallLog.started_at.desc()).limit(limit)).all())
