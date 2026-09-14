from __future__ import annotations

from datetime import datetime
from typing import Any

from pydantic import BaseModel, ConfigDict, Field


class AgentCreate(BaseModel):
    agent_code: str | None = Field(default=None, min_length=2, max_length=64, pattern=r"^[A-Z0-9_\-]+$")
    display_name: str = Field(min_length=1, max_length=128)
    system_prompt: str = ""
    model_instance_code: str | None = Field(default=None, max_length=64)
    max_iterations: int = Field(default=8, ge=1, le=100)
    enabled: bool = True
    description: str | None = None
    version: str = Field(default="1.0.0", max_length=32)
    child_agent_ids: list[int] = Field(default_factory=list)
    skill_ids: list[int] = Field(default_factory=list)
    knowledge_base_ids: list[int] = Field(default_factory=list)
    data_asset_ids: list[int] = Field(default_factory=list)


class AgentUpdate(BaseModel):
    agent_code: str | None = Field(default=None, min_length=2, max_length=64, pattern=r"^[A-Z0-9_\-]+$")
    display_name: str | None = Field(default=None, min_length=1, max_length=128)
    system_prompt: str | None = None
    model_instance_code: str | None = Field(default=None, max_length=64)
    max_iterations: int | None = Field(default=None, ge=1, le=100)
    enabled: bool | None = None
    description: str | None = None
    version: str | None = Field(default=None, max_length=32)
    child_agent_ids: list[int] | None = None
    skill_ids: list[int] | None = None
    knowledge_base_ids: list[int] | None = None
    data_asset_ids: list[int] | None = None


class AgentRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    agent_code: str
    display_name: str
    system_prompt: str
    model_instance_code: str | None
    max_iterations: int
    enabled: bool
    description: str | None
    version: str
    child_agent_ids: list[int] = Field(default_factory=list)
    skill_ids: list[int] = Field(default_factory=list)
    knowledge_base_ids: list[int] = Field(default_factory=list)
    data_asset_ids: list[int] = Field(default_factory=list)
    created_at: datetime
    updated_at: datetime


class AgentSummary(BaseModel):
    id: int
    agent_code: str
    display_name: str
    enabled: bool
    model_instance_code: str | None = None


class DataAssetCreate(BaseModel):
    asset_code: str = Field(min_length=2, max_length=64, pattern=r"^[A-Z0-9_\-]+$")
    table_name: str = Field(min_length=1, max_length=128, pattern=r"^[A-Za-z_][A-Za-z0-9_]*$")
    display_name: str = Field(min_length=1, max_length=128)
    description: str | None = None
    allowed_columns: list[str] = Field(default_factory=list)
    enabled: bool = True


class DataAssetUpdate(BaseModel):
    display_name: str | None = Field(default=None, min_length=1, max_length=128)
    description: str | None = None
    allowed_columns: list[str] | None = None
    enabled: bool | None = None


class DataAssetRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    asset_code: str
    table_name: str
    display_name: str
    description: str | None
    allowed_columns: list[str]
    columns: list[str] = Field(default_factory=list)
    governance_status: str
    source_health: str = "UNKNOWN"
    last_governed_at: datetime | None = None
    governance_report_json: dict[str, Any] = Field(default_factory=dict)
    row_count: int
    enabled: bool
    last_inspected_at: datetime | None
    created_at: datetime
    updated_at: datetime


class KnowledgeBaseCreate(BaseModel):
    kb_code: str = Field(min_length=2, max_length=64, pattern=r"^[A-Z0-9_\-]+$")
    kb_name: str = Field(min_length=1, max_length=128)
    description: str | None = None
    source_tables: list[str] = Field(default_factory=list)
    version: str = Field(default="1.0.0", max_length=32)
    enabled: bool = True


class KnowledgeBaseUpdate(BaseModel):
    kb_name: str | None = Field(default=None, min_length=1, max_length=128)
    description: str | None = None
    source_tables: list[str] | None = None
    version: str | None = Field(default=None, max_length=32)
    status: str | None = Field(default=None, max_length=32)
    enabled: bool | None = None


class KnowledgeBaseRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    kb_code: str
    kb_name: str
    description: str | None
    status: str
    version: str
    source_tables: list[str]
    entity_count: int
    relation_count: int
    enabled: bool
    created_at: datetime
    updated_at: datetime


class KnowledgeBuildResponse(BaseModel):
    knowledge_base_id: int
    status: str
    documents_created: int
    entities_created: int
    relations_created: int
    message: str


class KnowledgeSearchResult(BaseModel):
    document_id: int
    source_table: str
    symbol: str | None
    title: str
    content: str
    metadata_json: dict[str, Any]


class KnowledgeGraphCreate(BaseModel):
    knowledge_base_id: int
    graph_code: str = Field(min_length=2, max_length=64, pattern=r"^[A-Z0-9_\-]+$")
    graph_name: str = Field(min_length=1, max_length=128)
    description: str | None = None
    symbol: str | None = Field(default=None, max_length=32)
    source_tables: list[str] = Field(default_factory=list)
    version: str = "1.0.0"
    enabled: bool = True


class KnowledgeGraphUpdate(BaseModel):
    graph_name: str | None = Field(default=None, min_length=1, max_length=128)
    description: str | None = None
    symbol: str | None = Field(default=None, max_length=32)
    source_tables: list[str] | None = None
    version: str | None = None
    enabled: bool | None = None


class KnowledgeGraphRead(KnowledgeGraphCreate):
    model_config = ConfigDict(from_attributes=True)
    id: int
    governance_status: str
    entity_count: int
    relation_count: int
    last_governed_at: datetime | None
    governance_report_json: dict[str, Any]
    created_at: datetime
    updated_at: datetime


class GovernanceRequest(BaseModel):
    source_asset_ids: list[int] = Field(default_factory=list)
    agent_id: int | None = None


class GovernanceBatchRequest(GovernanceRequest):
    target_ids: list[int] = Field(min_length=1)


class GovernanceStateUpdate(BaseModel):
    governance_status: str


class GovernanceRunRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    target_type: str
    target_id: int
    agent_id: int | None
    model_call_log_id: int | None
    source_asset_ids: list[int]
    status: str
    summary_json: dict[str, Any]
    created_at: datetime
    updated_at: datetime
