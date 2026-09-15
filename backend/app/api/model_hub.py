from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, Response, status
from sqlalchemy import delete, func, inspect, select, text, update
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.ai_hub import AgentSkillLink, ModelCallLog, ModelInstance, ModelProvider, ModelRouteRule, ModelSkill, ModelSkillRevision, PredictionLedger, SkillOptimizationDraft, ResearchReportRecord
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
    ModelSkillRevisionRead,
    ModelSkillUpdate,
    ModelTestResponse,
)
from app.services.model_hub import ModelHubService
from app.services.model_credentials import decrypt_api_key, encrypt_api_key
from app.connectors.model_registry import get_model_adapter
from app.services.skill_files import delete_skill_file, write_skill_file
from app.services.skill_registry import rollback_skill, save_skill_content, sync_skill_from_file
from app.schemas.skill_tools import DwValidationRequest, PredictionScoreRequest, WatchFilterRequest
from app.schemas.distillation import ExtractionRequest, ExtractionSource
from app.services.skill_tools import filter_watch_candidates, score_prediction_outcomes, validate_dw_records
from app.services.ondemand_distillation import trigger_ondemand_extraction
from app.schemas.predictions import PredictionCreate, PredictionRead, SkillDraftRead
from app.services.prediction_review import propose_failed_skill_revisions, review_predictions

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
    data.api_key_configured = bool(provider.api_key_encrypted)
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
    data.api_key_configured = bool(instance.api_key or (instance.provider and instance.provider.api_key_encrypted))
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
    values = payload.model_dump()
    api_key = values.pop("api_key", None)
    try:
        provider = ModelProvider(**values, api_key_encrypted=encrypt_api_key(api_key) if api_key else None)
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
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
    values = payload.model_dump(exclude_unset=True)
    api_key = values.pop("api_key", None)
    if api_key:
        try:
            provider.api_key_encrypted = encrypt_api_key(api_key)
            db.execute(update(ModelInstance).where(ModelInstance.provider_id == provider.id).values(api_key=None))
        except ValueError as exc:
            raise HTTPException(status_code=422, detail=str(exc)) from exc
    for field_name, value in values.items():
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
                    api_key=decrypt_api_key(provider.api_key_encrypted) if provider.api_key_encrypted else None,
                    api_base_url=provider.api_base_url,
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
    succeeded = log.status == "SUCCESS"
    return ModelTestResponse(
        call_log_id=log.id,
        provider_code=log.provider_code,
        instance_code=log.instance_code,
        model_code=log.model_code,
        status=log.status,
        message="供应商测试成功，已收到模型响应。" if succeeded else "供应商测试失败，请检查 API Base URL、SK/API Key、模型编码和网络连通性。",
        response_text=log.response_text or log.error_message,
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
    provider = _provider_or_404(db, payload.provider_id)
    values = payload.model_dump()
    api_key = values.pop("api_key", None)
    if api_key:
        try:
            provider.api_key_encrypted = encrypt_api_key(api_key)
            db.execute(update(ModelInstance).where(ModelInstance.provider_id == provider.id).values(api_key=None))
        except ValueError as exc:
            raise HTTPException(status_code=422, detail=str(exc)) from exc
    instance = ModelInstance(**values)
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
    api_key = values.pop("api_key", None)
    if api_key:
        provider = _provider_or_404(db, values.get("provider_id", instance.provider_id))
        try:
            provider.api_key_encrypted = encrypt_api_key(api_key)
            db.execute(update(ModelInstance).where(ModelInstance.provider_id == provider.id).values(api_key=None))
        except ValueError as exc:
            raise HTTPException(status_code=422, detail=str(exc)) from exc
        instance.api_key = None
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
    try:
        log = ModelHubService(db).test_instance(instance.instance_code)
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    succeeded = log.status == "SUCCESS"
    return ModelTestResponse(
        call_log_id=log.id,
        provider_code=log.provider_code,
        instance_code=log.instance_code,
        model_code=log.model_code,
        status=log.status,
        message="模型实例测试成功，已收到模型响应。" if succeeded else "模型实例测试失败，请检查 API Base URL、SK/API Key、模型编码和网络连通性。",
        response_text=log.response_text or log.error_message,
    )


@router.get("/routes", response_model=list[ModelRouteRuleRead])
def list_routes(db: Session = Depends(get_db)) -> list[ModelRouteRule]:
    return list(db.scalars(select(ModelRouteRule).order_by(ModelRouteRule.task_type)).all())


def _validate_route_chain(db: Session, primary: str, fallback_chain: list[str]) -> None:
    if not fallback_chain:
        raise HTTPException(status_code=422, detail="A task route requires at least one fallback instance")
    codes = [primary, *fallback_chain]
    if len(codes) != len(set(codes)):
        raise HTTPException(status_code=422, detail="Route instances must be distinct")
    found = set(db.scalars(select(ModelInstance.instance_code).where(ModelInstance.instance_code.in_(codes))).all())
    if found != set(codes):
        raise HTTPException(status_code=422, detail=f"Unknown model instances: {', '.join(sorted(set(codes) - found))}")


@router.post("/routes", response_model=ModelRouteRuleRead, status_code=status.HTTP_201_CREATED)
def create_route(payload: ModelRouteRuleCreate, db: Session = Depends(get_db)) -> ModelRouteRule:
    _validate_route_chain(db, payload.preferred_instance_code, payload.fallback_chain_json)
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
    if values.get("fallback_chain_json", route.fallback_chain_json) is None or values.get("preferred_instance_code", route.preferred_instance_code) is None:
        raise HTTPException(status_code=422, detail="Primary and fallback model instances are required")
    _validate_route_chain(
        db,
        values.get("preferred_instance_code", route.preferred_instance_code),
        values.get("fallback_chain_json", route.fallback_chain_json or []),
    )
    for field_name, value in values.items():
        setattr(route, field_name, value)
    db.commit()
    db.refresh(route)
    return route


@router.delete("/routes/{route_id}", status_code=204)
def delete_route(route_id: int, db: Session = Depends(get_db)) -> Response:
    route = db.get(ModelRouteRule, route_id)
    if not route:
        raise HTTPException(status_code=404, detail="Model route not found")
    db.delete(route)
    db.commit()
    return Response(status_code=204)


@router.get("/skills", response_model=list[ModelSkillRead])
def list_skills(enabled: bool | None = None, db: Session = Depends(get_db)) -> list[ModelSkill]:
    statement = select(ModelSkill).order_by(ModelSkill.skill_code)
    if enabled is not None:
        statement = statement.where(ModelSkill.enabled == enabled)
    skills = list(db.scalars(statement).all())
    try:
        for skill in skills:
            sync_skill_from_file(db, skill)
        db.commit()
    except ValueError as exc:
        db.rollback()
        raise HTTPException(status_code=409, detail=str(exc)) from exc
    return skills


def _validate_skill_type(skill_type: str, config_json: dict) -> None:
    if skill_type == "EXECUTABLE_TOOL":
        spec = config_json.get("function_spec")
        if not isinstance(spec, dict) or not isinstance(spec.get("name"), str) or not isinstance(spec.get("parameters"), dict):
            raise HTTPException(status_code=422, detail="Executable tool requires config_json.function_spec with name and parameters")


@router.post("/skills", response_model=ModelSkillRead, status_code=status.HTTP_201_CREATED)
def create_skill(payload: ModelSkillCreate, db: Session = Depends(get_db)) -> ModelSkill:
    values = payload.model_dump()
    values["skill_code"] = values.get("skill_code") or _next_skill_code(db)
    _validate_skill_type(values["skill_type"], values["config_json"])
    skill = ModelSkill(**values)
    db.add(skill)
    try:
        db.flush()
        save_skill_content(db, skill, skill.instructions)
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
    try:
        sync_skill_from_file(db, skill)
    except ValueError as exc:
        db.rollback()
        raise HTTPException(status_code=409, detail=str(exc)) from exc
    previous_skill_code = skill.skill_code
    values = payload.model_dump(exclude_unset=True)
    expected_hash = values.pop("expected_content_hash", None)
    if expected_hash and expected_hash != skill.content_hash:
        db.rollback()
        raise HTTPException(status_code=409, detail="Skill file changed; reload before editing")
    values.pop("version", None)
    if any(values.get(name) is None for name in ("skill_code", "skill_name", "instructions", "skill_type", "config_json") if name in values):
        db.rollback()
        raise HTTPException(status_code=422, detail="Skill required fields cannot be null")
    instructions = values.pop("instructions", skill.instructions)
    _validate_skill_type(values.get("skill_type", skill.skill_type), values.get("config_json", skill.config_json or {}))
    for field_name, value in values.items():
        setattr(skill, field_name, value)
    try:
        db.flush()
        save_skill_content(db, skill, instructions)
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        raise HTTPException(status_code=409, detail="Skill code already exists") from exc
    if "skill_code" in values and values["skill_code"] != previous_skill_code:
        delete_skill_file(previous_skill_code)
    db.refresh(skill)
    return skill


@router.get("/skills/{skill_id}/revisions", response_model=list[ModelSkillRevisionRead])
def list_skill_revisions(skill_id: int, db: Session = Depends(get_db)) -> list[ModelSkillRevision]:
    skill = _skill_or_404(db, skill_id)
    sync_skill_from_file(db, skill)
    db.commit()
    return list(db.scalars(select(ModelSkillRevision).where(ModelSkillRevision.skill_id == skill_id).order_by(ModelSkillRevision.id.desc())).all())


@router.post("/skills/{skill_id}/revisions/{revision_id}/rollback", response_model=ModelSkillRead)
def rollback_skill_revision(skill_id: int, revision_id: int, db: Session = Depends(get_db)) -> ModelSkill:
    skill = _skill_or_404(db, skill_id)
    sync_skill_from_file(db, skill)
    try:
        rollback_skill(db, skill, revision_id)
        db.commit()
    except LookupError as exc:
        db.rollback()
        raise HTTPException(status_code=404, detail=str(exc)) from exc
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
    db.execute(delete(ModelSkillRevision).where(ModelSkillRevision.skill_id == skill.id))
    db.delete(skill)
    db.commit()
    delete_skill_file(skill_code)
    return Response(status_code=204)


@router.post("/predictions", response_model=PredictionRead, status_code=201)
def create_prediction(payload: PredictionCreate, db: Session = Depends(get_db)) -> PredictionLedger:
    skill = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == payload.skill_code, ModelSkill.enabled.is_(True)))
    if skill is None:
        raise HTTPException(status_code=404, detail="Enabled Skill not found")
    sync_skill_from_file(db, skill)
    if payload.research_report_id and db.get(ResearchReportRecord, payload.research_report_id) is None:
        raise HTTPException(status_code=404, detail="Research report not found")
    if payload.model_instance_code and not db.scalar(select(ModelInstance.id).where(ModelInstance.instance_code == payload.model_instance_code)):
        raise HTTPException(status_code=404, detail="Model instance not found")
    row = PredictionLedger(**payload.model_dump(), skill_version_used=skill.version, status="PENDING")
    db.add(row)
    db.commit()
    db.refresh(row)
    return row


@router.get("/predictions", response_model=list[PredictionRead])
def list_predictions(stock_code: str | None = None, status_filter: str | None = None, limit: int = 100,
                     db: Session = Depends(get_db)) -> list[PredictionLedger]:
    if not 1 <= limit <= 500:
        raise HTTPException(status_code=422, detail="limit must be between 1 and 500")
    query = select(PredictionLedger).order_by(PredictionLedger.predicted_at.desc()).limit(limit)
    if stock_code:
        query = query.where(PredictionLedger.stock_code == stock_code)
    if status_filter:
        query = query.where(PredictionLedger.status == status_filter)
    return list(db.scalars(query).all())


@router.post("/predictions/review")
def review_prediction_ledger(fetch_missing: bool = False, db: Session = Depends(get_db)) -> dict[str, int]:
    result = review_predictions(db, fetch_missing=fetch_missing)
    result["drafts_created"] = propose_failed_skill_revisions(db)
    return result


@router.get("/skill-drafts", response_model=list[SkillDraftRead])
def list_skill_drafts(status_filter: str | None = None, db: Session = Depends(get_db)) -> list[SkillOptimizationDraft]:
    query = select(SkillOptimizationDraft).order_by(SkillOptimizationDraft.created_at.desc())
    if status_filter:
        query = query.where(SkillOptimizationDraft.status == status_filter)
    return list(db.scalars(query.limit(100)).all())


@router.post("/skill-drafts/{draft_id}/approve", response_model=ModelSkillRead)
def approve_skill_draft(draft_id: int, db: Session = Depends(get_db)) -> ModelSkill:
    draft = db.get(SkillOptimizationDraft, draft_id)
    if draft is None:
        raise HTTPException(status_code=404, detail="Skill draft not found")
    if draft.status != "PENDING_REVIEW":
        raise HTTPException(status_code=409, detail="Skill draft has already been reviewed")
    if draft.failure_signature.startswith("DISTILLATION_REVIEW:"):
        raise HTTPException(status_code=409, detail="Entity review cannot be approved as a Skill prompt revision")
    draft_columns = {column["name"] for column in inspect(db.connection()).get_columns("skill_optimization_draft")}
    if "draft_type" in draft_columns and db.execute(
        text("SELECT draft_type FROM skill_optimization_draft WHERE id = :draft_id"),
        {"draft_id": draft_id},
    ).scalar_one() == "DISTILLATION_REVIEW":
        raise HTTPException(status_code=409, detail="Entity review cannot be approved as a Skill prompt revision")
    skill = _skill_or_404(db, draft.skill_id)
    sync_skill_from_file(db, skill)
    if skill.version != draft.base_skill_version:
        db.rollback()
        raise HTTPException(status_code=409, detail="Skill changed since draft creation; review it again")
    save_skill_content(db, skill, draft.proposed_instructions, source="META_APPROVED")
    draft.status = "APPROVED"
    draft.reviewed_at = datetime.now(timezone.utc)
    db.commit()
    db.refresh(skill)
    return skill


@router.post("/skill-drafts/{draft_id}/reject", response_model=SkillDraftRead)
def reject_skill_draft(draft_id: int, db: Session = Depends(get_db)) -> SkillOptimizationDraft:
    draft = db.get(SkillOptimizationDraft, draft_id)
    if draft is None:
        raise HTTPException(status_code=404, detail="Skill draft not found")
    if draft.status != "PENDING_REVIEW":
        raise HTTPException(status_code=409, detail="Skill draft has already been reviewed")
    draft.status = "REJECTED"
    draft.reviewed_at = datetime.now(timezone.utc)
    db.commit()
    db.refresh(draft)
    return draft


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


@router.post("/skill-tools/validate-dw-records")
def run_dw_validation(payload: DwValidationRequest) -> dict:
    return validate_dw_records(payload.records)


@router.post("/skill-tools/filter-watch-candidates")
def run_watch_filter(payload: WatchFilterRequest) -> dict:
    return filter_watch_candidates(payload)


@router.post("/skill-tools/score-prediction-outcomes")
def run_prediction_scoring(payload: PredictionScoreRequest) -> dict:
    return score_prediction_outcomes(payload)


@router.post("/skill-tools/trigger-ondemand-extraction", response_model=ExtractionSource)
def run_ondemand_extraction(
    payload: ExtractionRequest, db: Session = Depends(get_db)
) -> ExtractionSource:
    try:
        return trigger_ondemand_extraction(payload.stock_code, payload.doc_id, db)
    except LookupError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc


def _next_skill_code(db: "Session") -> str:
    """Return the next stable, human-readable Skill identifier."""
    index = int(db.scalar(select(func.count(ModelSkill.id))) or 0) + 1
    while True:
        code = f"SKILL_{index:04d}"
        if not db.scalar(select(ModelSkill.id).where(ModelSkill.skill_code == code)):
            return code
        index += 1
