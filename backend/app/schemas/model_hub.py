from __future__ import annotations

from datetime import datetime
from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field


class ModelProviderCreate(BaseModel):
    provider_code: str = Field(min_length=2, max_length=64, pattern=r"^[A-Z0-9_]+$")
    provider_name: str = Field(min_length=1, max_length=128)
    provider_type: str = Field(pattern=r"^(MOCK|OPENAI_COMPAT|GEMINI_REST|DEEPSEEK|ANTHROPIC)$")
    api_base_url: str | None = Field(default=None, max_length=512)
    api_key: str | None = Field(default=None, min_length=1)
    enabled: bool = True
    description: str | None = None
    config_json: dict[str, Any] = Field(default_factory=dict)


class ModelProviderUpdate(BaseModel):
    provider_name: str | None = Field(default=None, min_length=1, max_length=128)
    provider_type: str | None = Field(default=None, pattern=r"^(MOCK|OPENAI_COMPAT|GEMINI_REST|DEEPSEEK|ANTHROPIC)$")
    api_base_url: str | None = Field(default=None, max_length=512)
    api_key: str | None = Field(default=None, min_length=1)
    enabled: bool | None = None
    description: str | None = None
    config_json: dict[str, Any] | None = None


class ModelProviderRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    provider_code: str
    provider_name: str
    provider_type: str
    api_base_url: str | None
    api_key_configured: bool = False
    enabled: bool
    description: str | None
    config_json: dict[str, Any]
    created_at: datetime
    updated_at: datetime


class ModelInstanceCreate(BaseModel):
    provider_id: int
    instance_code: str = Field(min_length=2, max_length=64, pattern=r"^[A-Z0-9_\-]+$")
    model_code: str = Field(min_length=1, max_length=128)
    model_name: str = Field(min_length=1, max_length=128)
    purpose: str = Field(default="GENERAL", max_length=64)
    usage_type: Literal["EXACT", "CREATIVE"] = "EXACT"
    api_key: str | None = None
    api_base_url: str | None = None
    api_path: str | None = None
    max_tokens: int = Field(default=2048, ge=1, le=200000)
    temperature: float = Field(default=0.2, ge=0.0, le=2.0)
    top_p: float = Field(default=0.95, ge=0.0, le=1.0)
    enabled: bool = True
    fallback_instance_code: str | None = None
    config_json: dict[str, Any] = Field(default_factory=dict)
    description: str | None = None


class ModelInstanceUpdate(BaseModel):
    provider_id: int | None = None
    model_code: str | None = Field(default=None, max_length=128)
    model_name: str | None = Field(default=None, max_length=128)
    purpose: str | None = Field(default=None, max_length=64)
    usage_type: Literal["EXACT", "CREATIVE"] | None = None
    api_key: str | None = None
    api_base_url: str | None = None
    api_path: str | None = None
    max_tokens: int | None = Field(default=None, ge=1, le=200000)
    temperature: float | None = Field(default=None, ge=0.0, le=2.0)
    top_p: float | None = Field(default=None, ge=0.0, le=1.0)
    enabled: bool | None = None
    fallback_instance_code: str | None = None
    config_json: dict[str, Any] | None = None
    description: str | None = None


class ModelInstanceRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    provider_id: int
    instance_code: str
    model_code: str
    model_name: str
    purpose: str
    usage_type: Literal["EXACT", "CREATIVE"]
    api_base_url: str | None
    api_path: str | None
    max_tokens: int
    temperature: float
    top_p: float
    enabled: bool
    fallback_instance_code: str | None
    config_json: dict[str, Any]
    description: str | None
    api_key_configured: bool = False
    created_at: datetime
    updated_at: datetime


class ModelRouteRuleCreate(BaseModel):
    task_type: str = Field(min_length=1, max_length=64)
    preferred_instance_code: str = Field(min_length=1, max_length=64)
    fallback_chain_json: list[str] = Field(min_length=1)
    route_policy: str = Field(default="PREFERRED_THEN_FALLBACK", max_length=32)
    enabled: bool = True
    description: str | None = None


class ModelRouteRuleUpdate(BaseModel):
    preferred_instance_code: str | None = Field(default=None, max_length=64)
    fallback_chain_json: list[str] | None = Field(default=None, min_length=1)
    route_policy: str | None = Field(default=None, max_length=32)
    enabled: bool | None = None
    description: str | None = None


class ModelRouteRuleRead(ModelRouteRuleCreate):
    model_config = ConfigDict(from_attributes=True)

    id: int
    created_at: datetime
    updated_at: datetime


class ModelSkillCreate(BaseModel):
    skill_code: str | None = Field(default=None, min_length=2, max_length=64, pattern=r"^[A-Z0-9_\-]+$")
    skill_name: str = Field(min_length=1, max_length=128)
    description: str | None = None
    instructions: str = Field(min_length=1)
    skill_type: Literal["PROMPT_SOP", "EXECUTABLE_TOOL"] = "PROMPT_SOP"
    enabled: bool = True
    config_json: dict[str, Any] = Field(default_factory=dict)
    version: str = Field(default="1.0.0", max_length=32, pattern=r"^\d+\.\d+\.\d+$")
    is_builtin: bool = False


class ModelSkillUpdate(BaseModel):
    skill_code: str | None = Field(default=None, min_length=2, max_length=64, pattern=r"^[A-Z0-9_\-]+$")
    skill_name: str | None = Field(default=None, min_length=1, max_length=128)
    description: str | None = None
    instructions: str | None = Field(default=None, min_length=1)
    skill_type: Literal["PROMPT_SOP", "EXECUTABLE_TOOL"] | None = None
    enabled: bool | None = None
    config_json: dict[str, Any] | None = None
    version: str | None = Field(default=None, max_length=32)
    expected_content_hash: str | None = None


class ModelSkillRead(ModelSkillCreate):
    model_config = ConfigDict(from_attributes=True)

    id: int
    file_path: str | None = None
    content_hash: str | None = None
    format: str = "MD"
    created_at: datetime
    updated_at: datetime


class ModelSkillRevisionRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    skill_id: int
    version: str
    instructions: str
    content_hash: str
    source: str
    created_at: datetime


class ModelMessage(BaseModel):
    role: Literal["system", "user", "assistant", "tool"]
    content: str


class ModelChatRequest(BaseModel):
    task_type: str = Field(min_length=1, max_length=64)
    instance_code: str | None = None
    messages: list[ModelMessage]
    temperature: float | None = Field(default=None, ge=0.0, le=2.0)
    max_tokens: int | None = Field(default=None, ge=1, le=200000)
    metadata_json: dict[str, Any] = Field(default_factory=dict)


class ModelChatResponse(BaseModel):
    call_log_id: int
    task_type: str
    provider_code: str | None
    instance_code: str | None
    model_code: str | None
    status: str
    response_text: str
    response_json: dict[str, Any]
    prediction_ids: list[int] = Field(default_factory=list)


class ModelTestResponse(BaseModel):
    call_log_id: int
    provider_code: str | None
    instance_code: str | None
    model_code: str | None
    status: str
    message: str
    response_text: str | None = None


class ModelCallLogRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    task_type: str
    provider_code: str | None
    instance_code: str | None
    model_code: str | None
    status: str
    request_json: dict[str, Any]
    response_json: dict[str, Any]
    response_text: str | None
    error_message: str | None
    latency_ms: int | None
    started_at: datetime
    completed_at: datetime | None
