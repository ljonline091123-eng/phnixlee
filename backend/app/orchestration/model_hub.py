"""Reusable Model Hub and Agent execution workflows."""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
import hashlib
import json
from typing import Any
from uuid import uuid4

from fastapi.encoders import jsonable_encoder
from jsonschema import Draft202012Validator, ValidationError
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.ai_hub import AgentDefinition, ModelCallLog, ModelSkill
from app.models.governance import AgentExecutionRun, SkillExecutionRun
from app.schemas.model_hub import ModelChatResponse
from app.schemas.skill_tools import PredictionScoreRequest, WatchFilterRequest
from app.services.model_hub import ModelHubService
from app.services.prediction_ledger import PREDICTION_WRITER_SKILL_CODE, record_agent_predictions
from app.services.skill_registry import sync_skill_from_file
from app.services.skill_tools import (
    filter_watch_candidates,
    score_prediction_outcomes,
    validate_dw_records,
)
from app.services.ondemand_distillation import trigger_ondemand_extraction


class AgentUnavailableError(LookupError):
    pass


class AgentOutputError(ValueError):
    pass


@dataclass(frozen=True, slots=True)
class ModelInvocationCommand:
    task_type: str
    messages: list[dict[str, str]]
    instance_code: str | None = None
    temperature: float | None = None
    max_tokens: int | None = None
    metadata_json: dict[str, Any] = field(default_factory=dict)


class ModelInvocationWorkflow:
    """One model invocation entry point for REST, workers and workflows."""

    def __init__(self, db: Session):
        self.db = db

    def execute(self, command: ModelInvocationCommand) -> ModelChatResponse:
        log = ModelHubService(self.db).chat(
            task_type=command.task_type,
            instance_code=command.instance_code,
            messages=command.messages,
            temperature=command.temperature,
            max_tokens=command.max_tokens,
            metadata_json=command.metadata_json,
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


@dataclass(frozen=True, slots=True)
class AgentExecutionCommand:
    agent_id: int
    task_type: str
    messages: list[dict[str, str]]
    evidence_ids: list[str] = field(default_factory=list)
    correlation_id: str | None = None
    idempotency_key: str | None = None
    request_source: str = "API"


class AgentExecutionWorkflow:
    """Compose Agent, Skill, routing and prediction-ledger capabilities."""

    PREDICTION_TASKS = {"stock_screening", "stock_analysis", "research_report"}

    def __init__(self, db: Session):
        self.db = db

    def execute(self, command: AgentExecutionCommand) -> ModelChatResponse:
        agent = self.db.get(AgentDefinition, command.agent_id)
        if agent is None or not agent.enabled or agent.lifecycle_status != "ENABLED":
            raise AgentUnavailableError("Enabled agent not found")

        input_payload = {
            "task_type": command.task_type,
            "messages": command.messages,
            "evidence_ids": [str(item) for item in command.evidence_ids],
            "request_source": command.request_source,
        }
        input_hash = _payload_hash(input_payload)
        if command.idempotency_key:
            existing = self.db.scalar(select(AgentExecutionRun).where(
                AgentExecutionRun.idempotency_key == command.idempotency_key,
            ))
            if existing is not None:
                if existing.agent_id != agent.id or existing.input_hash != input_hash:
                    raise ValueError("Agent idempotency key was reused with different input")
                if existing.status != "SUCCESS" or existing.model_call_log_id is None:
                    raise ValueError("Agent idempotency key belongs to a non-reusable execution")
                call_log = self.db.get(ModelCallLog, existing.model_call_log_id)
                if call_log is None:
                    raise ValueError("Agent execution model call log is missing")
                output = existing.output_json or {}
                return ModelChatResponse(
                    call_log_id=call_log.id,
                    task_type=call_log.task_type,
                    provider_code=call_log.provider_code,
                    instance_code=call_log.instance_code,
                    model_code=call_log.model_code,
                    status=existing.status,
                    response_text=str(output.get("response_text") or ""),
                    response_json=output.get("response_json") or {},
                    prediction_ids=list(output.get("prediction_ids") or []),
                )
        agent_run = AgentExecutionRun(
            run_key=f"agent:{agent.agent_code}:{command.idempotency_key or uuid4().hex}",
            agent_id=agent.id,
            agent_code=agent.agent_code,
            agent_version=agent.version,
            model_instance_code=agent.model_instance_code,
            trigger_type="API",
            status="RUNNING",
            correlation_id=command.correlation_id,
            idempotency_key=command.idempotency_key,
            input_hash=input_hash,
            input_json=input_payload,
            accessible_assets_json=[link.data_asset.asset_code for link in agent.data_asset_links],
            knowledge_versions_json={
                link.knowledge_base.kb_code: link.knowledge_base.version for link in agent.knowledge_links
            },
            skill_versions_json={
                link.skill.skill_code: link.skill.version for link in agent.skill_links if link.skill.enabled
            },
            evidence_ids_json=[str(item) for item in command.evidence_ids],
            audit_json={
                "arbitrary_sql_allowed": False,
                "direct_database_write_allowed": False,
                "request_source": command.request_source,
            },
        )
        self.db.add(agent_run)
        self.db.flush()

        system_messages = [{"role": "system", "content": agent.system_prompt}]
        if command.task_type in self.PREDICTION_TASKS:
            system_messages.append(
                {
                    "role": "system",
                    "content": (
                        "If you give an explicit BUY, SELL, or HOLD recommendation, return a JSON "
                        "object with a prediction field. Include market, stock_code, action_type, "
                        "target_timeframe (T+1 to T+30), reasoning_logic, skill_code from a bound "
                        "Skill, and an observed positive entry_price. Do not invent prices. The "
                        "recommendation is first returned as a candidate. It enters the T+N review "
                        "ledger only when this Agent has the controlled prediction-writer Skill and "
                        "the request supplies an idempotency key."
                    ),
                }
            )
        for link in agent.skill_links:
            if (link.skill.enabled and link.skill.lifecycle_status == "ENABLED"
                    and link.skill.skill_type == "PROMPT_SOP"):
                sync_skill_from_file(self.db, link.skill)
                system_messages.append({"role": "system", "content": link.skill.instructions})
                self.db.add(SkillExecutionRun(
                    agent_execution_run_id=agent_run.id,
                    skill_id=link.skill.id,
                    skill_code=link.skill.skill_code,
                    skill_version=link.skill.version,
                    status="SUCCESS",
                    input_hash=input_hash,
                    output_hash=hashlib.sha256(link.skill.instructions.encode("utf-8")).hexdigest(),
                    input_json={"task_type": command.task_type},
                    output_json={"prompt_injected": True},
                    side_effect_level=link.skill.side_effect_level,
                    write_scope_json={},
                    evidence_ids_json=[str(item) for item in command.evidence_ids],
                    completed_at=datetime.now(timezone.utc),
                ))

        history = command.messages[-agent.context_window_limit * 2 :]
        metadata = {
            "agent_code": agent.agent_code,
            "json_schema_output": agent.json_schema_output or {},
            "knowledge_base_ids": [link.knowledge_base_id for link in agent.knowledge_links],
            "data_source_ids": [link.data_source_id for link in agent.data_source_links],
        }
        allowed_skill_codes = {
            link.skill.skill_code for link in agent.skill_links
            if link.skill.enabled
            and link.skill.lifecycle_status == "ENABLED"
            and link.skill.skill_code != PREDICTION_WRITER_SKILL_CODE
        }
        instance_code = agent.model_instance_code
        self.db.commit()

        try:
            response = ModelInvocationWorkflow(self.db).execute(
                ModelInvocationCommand(
                    task_type=command.task_type,
                    instance_code=instance_code,
                    messages=[*system_messages, *history],
                    metadata_json={**metadata, "agent_execution_run_id": agent_run.id},
                )
            )
        except Exception as exc:
            agent_run.status = "FAILED"
            agent_run.error_code = exc.__class__.__name__
            agent_run.error_message = str(exc)[:4000]
            agent_run.completed_at = datetime.now(timezone.utc)
            self.db.commit()
            raise
        prediction_capture = None
        if response.status == "SUCCESS" and response.provider_code != "MOCK":
            try:
                writer_skill = next((
                    link.skill for link in sorted(agent.skill_links, key=lambda item: item.skill.id)
                    if link.skill.skill_code == PREDICTION_WRITER_SKILL_CODE
                ), None)
                prediction_capture = record_agent_predictions(
                    self.db,
                    response.response_text,
                    allowed_skill_codes=allowed_skill_codes,
                    model_instance_code=response.instance_code,
                    writer_skill=writer_skill,
                    idempotency_key=command.idempotency_key,
                    agent_execution_run_id=agent_run.id,
                    model_call_log_id=response.call_log_id,
                    evidence_ids=agent_run.evidence_ids_json,
                )
                response.prediction_ids = prediction_capture.prediction_ids
                response.response_json = {
                    **(response.response_json or {}),
                    "prediction_capture": prediction_capture.as_audit_json(),
                }
            except ValueError as exc:
                self.db.rollback()
                agent_run.status = "FAILED"
                agent_run.error_code = "AGENT_OUTPUT_INVALID"
                agent_run.error_message = str(exc)[:4000]
                agent_run.completed_at = datetime.now(timezone.utc)
                self.db.commit()
                raise AgentOutputError(str(exc)) from exc
        output_payload = {
            "status": response.status,
            "response_text": response.response_text,
            "response_json": response.response_json,
            "prediction_ids": response.prediction_ids,
        }
        agent_run.status = response.status
        agent_run.model_call_log_id = response.call_log_id
        agent_run.model_instance_code = response.instance_code
        agent_run.model_version = response.model_code
        agent_run.output_json = output_payload
        agent_run.output_hash = _payload_hash(output_payload)
        if isinstance(response.response_json, dict):
            evidence = response.response_json.get("evidence_ids")
            if isinstance(evidence, list):
                agent_run.evidence_ids_json = list(dict.fromkeys([
                    *agent_run.evidence_ids_json,
                    *(str(item) for item in evidence),
                ]))
        if prediction_capture is not None:
            agent_run.audit_json = {
                **(agent_run.audit_json or {}),
                "prediction_capture_status": prediction_capture.status,
                "prediction_capture_reason": prediction_capture.reason,
                "prediction_write_skill": prediction_capture.writer_skill_code,
                "prediction_write_run_id": prediction_capture.skill_execution_run_id,
            }
        agent_run.completed_at = datetime.now(timezone.utc)
        self.db.commit()
        return response


class SkillToolWorkflow:
    """Deterministic Skill tools exposed equally to REST and worker code."""

    def __init__(self, db: Session | None = None):
        self.db = db

    def execute(self, tool_code: str, context_data: Any, *, idempotency_key: str | None = None,
                agent_execution_run_id: str | None = None, evidence_ids: list[str] | None = None) -> Any:
        skill = self._registered_tool(tool_code) if self.db is not None else None
        if self.db is not None and skill is None:
            raise LookupError(f"Registered Skill tool not found: {tool_code}")
        input_payload = jsonable_encoder(context_data)
        execution = None
        if skill is not None:
            if skill.lifecycle_status != "ENABLED" or not skill.enabled:
                raise LookupError(f"Skill tool is not enabled: {tool_code}")
            if skill.input_contract_json:
                try:
                    Draft202012Validator(skill.input_contract_json).validate(input_payload)
                except ValidationError as exc:
                    raise ValueError(f"Skill input contract rejected the request: {exc.message}") from exc
            permissions = skill.permission_policy_json or {}
            if permissions.get("arbitrary_sql") or permissions.get("unrestricted_database_write"):
                raise PermissionError("Skill contract cannot grant arbitrary SQL or unrestricted database writes")
            if skill.side_effect_level != "READ_ONLY" and not idempotency_key:
                raise PermissionError("Controlled write Skills require an idempotency key")
            input_hash = _payload_hash(input_payload)
            if idempotency_key:
                existing = self.db.scalar(select(SkillExecutionRun).where(
                    SkillExecutionRun.idempotency_key == idempotency_key,
                ))
                if existing is not None:
                    if existing.skill_id != skill.id or existing.input_hash != input_hash:
                        raise ValueError("Idempotency key was reused with a different Skill input")
                    if existing.status == "SUCCESS":
                        return existing.output_json
            execution = SkillExecutionRun(
                agent_execution_run_id=agent_execution_run_id,
                skill_id=skill.id,
                skill_code=skill.skill_code,
                skill_version=skill.version,
                status="RUNNING",
                idempotency_key=idempotency_key,
                input_hash=input_hash,
                input_json=input_payload,
                side_effect_level=skill.side_effect_level,
                write_scope_json={
                    "allowed_operations": permissions.get("allowed_operations", []),
                    "allowed_services": permissions.get("allowed_services", []),
                    "direct_sql": False,
                },
                evidence_ids_json=list(evidence_ids or []),
                max_attempts=max(1, min(int((skill.retry_policy_json or {}).get("max_attempts", 1)), 3)),
            )
            self.db.add(execution)
            self.db.commit()
        max_attempts = execution.max_attempts if execution is not None else 1
        retry_on = set((skill.retry_policy_json or {}).get("retry_on", [])) if skill is not None else set()
        for attempt in range(1, max_attempts + 1):
            try:
                result = self._dispatch(tool_code, context_data)
                output_payload = jsonable_encoder(result)
                if skill is not None and skill.output_contract_json:
                    Draft202012Validator(skill.output_contract_json).validate(output_payload)
                if execution is not None:
                    execution.attempt = attempt
                    execution.status = "SUCCESS"
                    execution.output_json = output_payload
                    execution.output_hash = _payload_hash(output_payload)
                    execution.completed_at = datetime.now(timezone.utc)
                    self.db.commit()
                return result
            except Exception as exc:
                retryable = attempt < max_attempts and exc.__class__.__name__ in retry_on
                if execution is not None:
                    execution.attempt = attempt
                    execution.status = "RETRYING" if retryable else "FAILED"
                    execution.error_code = exc.__class__.__name__
                    execution.error_message = str(exc)[:4000]
                    execution.completed_at = None if retryable else datetime.now(timezone.utc)
                    self.db.commit()
                if not retryable:
                    raise
        raise RuntimeError("Skill execution exhausted without a terminal result")

    def _registered_tool(self, tool_code: str) -> ModelSkill | None:
        assert self.db is not None
        skills = list(self.db.scalars(select(ModelSkill).where(ModelSkill.enabled.is_(True))).all())
        matches = [item for item in skills
                   if (item.config_json or {}).get("function_spec", {}).get("name") == tool_code]
        matches.sort(key=lambda item: item.skill_type != "EXECUTABLE_TOOL")
        return matches[0] if matches else None

    def _dispatch(self, tool_code: str, context_data: Any) -> Any:
        if tool_code == "validate_dw_records":
            records = context_data.get("records", []) if isinstance(context_data, dict) else context_data
            return validate_dw_records(records)
        if tool_code == "filter_watch_candidates":
            request = (
                context_data
                if isinstance(context_data, WatchFilterRequest)
                else WatchFilterRequest.model_validate(context_data)
            )
            return filter_watch_candidates(request)
        if tool_code == "score_prediction_outcomes":
            request = (
                context_data
                if isinstance(context_data, PredictionScoreRequest)
                else PredictionScoreRequest.model_validate(context_data)
            )
            return score_prediction_outcomes(request)
        if tool_code == "trigger_ondemand_extraction":
            if self.db is None:
                raise RuntimeError("A database session is required for on-demand extraction")
            return trigger_ondemand_extraction(
                context_data["stock_code"], context_data.get("doc_id"), self.db
            )
        raise LookupError(f"Unknown Skill tool: {tool_code}")


def _payload_hash(payload: Any) -> str:
    encoded = json.dumps(jsonable_encoder(payload), ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(encoded.encode("utf-8")).hexdigest()
