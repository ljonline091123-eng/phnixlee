"""Reusable Model Hub and Agent execution workflows."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

from sqlalchemy.orm import Session

from app.models.ai_hub import AgentDefinition
from app.schemas.model_hub import ModelChatResponse
from app.schemas.skill_tools import PredictionScoreRequest, WatchFilterRequest
from app.services.model_hub import ModelHubService
from app.services.prediction_ledger import record_agent_predictions
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


class AgentExecutionWorkflow:
    """Compose Agent, Skill, routing and prediction-ledger capabilities."""

    PREDICTION_TASKS = {"stock_screening", "stock_analysis", "research_report"}

    def __init__(self, db: Session):
        self.db = db

    def execute(self, command: AgentExecutionCommand) -> ModelChatResponse:
        agent = self.db.get(AgentDefinition, command.agent_id)
        if agent is None or not agent.enabled:
            raise AgentUnavailableError("Enabled agent not found")

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
                        "recommendation is recorded for T+N review."
                    ),
                }
            )
        for link in agent.skill_links:
            if link.skill.enabled and link.skill.skill_type == "PROMPT_SOP":
                sync_skill_from_file(self.db, link.skill)
                system_messages.append({"role": "system", "content": link.skill.instructions})

        history = command.messages[-agent.context_window_limit * 2 :]
        metadata = {
            "agent_code": agent.agent_code,
            "json_schema_output": agent.json_schema_output or {},
            "knowledge_base_ids": [link.knowledge_base_id for link in agent.knowledge_links],
            "data_source_ids": [link.data_source_id for link in agent.data_source_links],
        }
        allowed_skill_codes = {
            link.skill.skill_code for link in agent.skill_links if link.skill.enabled
        }
        instance_code = agent.model_instance_code
        self.db.commit()

        response = ModelInvocationWorkflow(self.db).execute(
            ModelInvocationCommand(
                task_type=command.task_type,
                instance_code=instance_code,
                messages=[*system_messages, *history],
                metadata_json=metadata,
            )
        )
        if response.status == "SUCCESS" and response.provider_code != "MOCK":
            try:
                response.prediction_ids = record_agent_predictions(
                    self.db,
                    response.response_text,
                    allowed_skill_codes=allowed_skill_codes,
                    model_instance_code=response.instance_code,
                )
            except ValueError as exc:
                self.db.rollback()
                raise AgentOutputError(str(exc)) from exc
        return response


class SkillToolWorkflow:
    """Deterministic Skill tools exposed equally to REST and worker code."""

    def __init__(self, db: Session | None = None):
        self.db = db

    def execute(self, tool_code: str, context_data: Any) -> Any:
        if tool_code == "validate_dw_records":
            return validate_dw_records(context_data)
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
