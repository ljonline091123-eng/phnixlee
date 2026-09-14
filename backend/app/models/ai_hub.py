from __future__ import annotations

from datetime import datetime, timezone
from typing import Any

from sqlalchemy import Boolean, DateTime, ForeignKey, Index, Integer, JSON, String, Text, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


class TimestampMixin:
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=utc_now,
        onupdate=utc_now,
        nullable=False,
    )


class ModelProvider(TimestampMixin, Base):
    __tablename__ = "model_provider"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    provider_code: Mapped[str] = mapped_column(String(64), unique=True, nullable=False, index=True)
    provider_name: Mapped[str] = mapped_column(String(128), nullable=False)
    provider_type: Mapped[str] = mapped_column(String(32), nullable=False)
    api_base_url: Mapped[str | None] = mapped_column(String(512))
    api_key_encrypted: Mapped[str | None] = mapped_column(Text)
    enabled: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    description: Mapped[str | None] = mapped_column(Text)
    config_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)

    instances: Mapped[list[ModelInstance]] = relationship(
        back_populates="provider",
        cascade="all, delete-orphan",
    )


class ModelInstance(TimestampMixin, Base):
    __tablename__ = "model_instance"
    __table_args__ = (
        UniqueConstraint("provider_id", "instance_code", name="uq_model_instance_provider_code"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    provider_id: Mapped[int] = mapped_column(ForeignKey("model_provider.id", ondelete="CASCADE"), nullable=False)
    instance_code: Mapped[str] = mapped_column(String(64), unique=True, nullable=False, index=True)
    model_code: Mapped[str] = mapped_column(String(128), nullable=False)
    model_name: Mapped[str] = mapped_column(String(128), nullable=False)
    purpose: Mapped[str] = mapped_column(String(64), nullable=False, default="GENERAL")
    usage_type: Mapped[str] = mapped_column(String(16), nullable=False, default="EXACT")
    api_key: Mapped[str | None] = mapped_column(Text)
    api_base_url: Mapped[str | None] = mapped_column(String(512))
    api_path: Mapped[str | None] = mapped_column(String(256))
    max_tokens: Mapped[int] = mapped_column(Integer, nullable=False, default=2048)
    temperature: Mapped[float] = mapped_column(nullable=False, default=0.2)
    top_p: Mapped[float] = mapped_column(nullable=False, default=0.95)
    enabled: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    fallback_instance_code: Mapped[str | None] = mapped_column(String(64))
    config_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    description: Mapped[str | None] = mapped_column(Text)

    provider: Mapped[ModelProvider] = relationship(back_populates="instances")


class ModelRouteRule(TimestampMixin, Base):
    __tablename__ = "model_route_rule"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    task_type: Mapped[str] = mapped_column(String(64), unique=True, nullable=False)
    preferred_instance_code: Mapped[str] = mapped_column(String(64), nullable=False)
    fallback_chain_json: Mapped[list[str]] = mapped_column(JSON, nullable=False, default=list)
    route_policy: Mapped[str] = mapped_column(String(32), nullable=False, default="PREFERRED_THEN_FALLBACK")
    enabled: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    description: Mapped[str | None] = mapped_column(Text)


class ModelSkill(TimestampMixin, Base):
    __tablename__ = "model_skill"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    skill_code: Mapped[str] = mapped_column(String(64), unique=True, nullable=False, index=True)
    skill_name: Mapped[str] = mapped_column(String(128), nullable=False)
    description: Mapped[str | None] = mapped_column(Text)
    instructions: Mapped[str] = mapped_column(Text, nullable=False)
    enabled: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    config_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    file_path: Mapped[str | None] = mapped_column(String(512))
    content_hash: Mapped[str | None] = mapped_column(String(128))
    version: Mapped[str] = mapped_column(String(32), nullable=False, default="1.0.0")
    is_builtin: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    format: Mapped[str] = mapped_column(String(16), nullable=False, default="MD")
    skill_type: Mapped[str] = mapped_column(String(32), nullable=False, default="PROMPT_SOP")


class ModelSkillRevision(Base):
    __tablename__ = "model_skill_revision"
    __table_args__ = (UniqueConstraint("skill_id", "version", name="uq_model_skill_revision_version"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    skill_id: Mapped[int] = mapped_column(ForeignKey("model_skill.id", ondelete="CASCADE"), nullable=False, index=True)
    version: Mapped[str] = mapped_column(String(32), nullable=False)
    instructions: Mapped[str] = mapped_column(Text, nullable=False)
    content_hash: Mapped[str] = mapped_column(String(128), nullable=False)
    source: Mapped[str] = mapped_column(String(16), nullable=False, default="EDITOR")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, nullable=False)


class AgentDefinition(TimestampMixin, Base):
    __tablename__ = "agent_definition"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    agent_code: Mapped[str] = mapped_column(String(64), unique=True, nullable=False, index=True)
    display_name: Mapped[str] = mapped_column(String(128), nullable=False)
    system_prompt: Mapped[str] = mapped_column(Text, nullable=False, default="")
    model_instance_code: Mapped[str | None] = mapped_column(String(64), index=True)
    max_iterations: Mapped[int] = mapped_column(Integer, nullable=False, default=8)
    context_window_limit: Mapped[int] = mapped_column(Integer, nullable=False, default=12)
    json_schema_output: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    enabled: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    description: Mapped[str | None] = mapped_column(Text)
    version: Mapped[str] = mapped_column(String(32), nullable=False, default="1.0.0")

    child_links: Mapped[list["AgentChildLink"]] = relationship(
        foreign_keys="AgentChildLink.agent_id",
        back_populates="agent",
        cascade="all, delete-orphan",
    )
    skill_links: Mapped[list["AgentSkillLink"]] = relationship(
        back_populates="agent",
        cascade="all, delete-orphan",
    )
    knowledge_links: Mapped[list["AgentKnowledgeBaseLink"]] = relationship(
        back_populates="agent",
        cascade="all, delete-orphan",
    )
    data_asset_links: Mapped[list["AgentDataAssetLink"]] = relationship(
        back_populates="agent",
        cascade="all, delete-orphan",
    )
    data_source_links: Mapped[list["AgentDataSourceLink"]] = relationship(
        back_populates="agent",
        cascade="all, delete-orphan",
    )


class AgentChildLink(Base):
    __tablename__ = "agent_child_link"
    __table_args__ = (UniqueConstraint("agent_id", "child_agent_id", name="uq_agent_child_link"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    agent_id: Mapped[int] = mapped_column(ForeignKey("agent_definition.id", ondelete="CASCADE"), nullable=False)
    child_agent_id: Mapped[int] = mapped_column(ForeignKey("agent_definition.id", ondelete="RESTRICT"), nullable=False)
    order_index: Mapped[int] = mapped_column(Integer, nullable=False, default=0)

    agent: Mapped[AgentDefinition] = relationship(
        foreign_keys=[agent_id],
        back_populates="child_links",
    )
    child_agent: Mapped[AgentDefinition] = relationship(foreign_keys=[child_agent_id])


class AgentSkillLink(Base):
    __tablename__ = "agent_skill_link"
    __table_args__ = (UniqueConstraint("agent_id", "skill_id", name="uq_agent_skill_link"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    agent_id: Mapped[int] = mapped_column(ForeignKey("agent_definition.id", ondelete="CASCADE"), nullable=False)
    skill_id: Mapped[int] = mapped_column(ForeignKey("model_skill.id", ondelete="RESTRICT"), nullable=False)

    agent: Mapped[AgentDefinition] = relationship(back_populates="skill_links")
    skill: Mapped[ModelSkill] = relationship()


class AgentDataAssetLink(Base):
    __tablename__ = "agent_data_asset_link"
    __table_args__ = (UniqueConstraint("agent_id", "data_asset_id", name="uq_agent_data_asset_link"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    agent_id: Mapped[int] = mapped_column(ForeignKey("agent_definition.id", ondelete="CASCADE"), nullable=False)
    data_asset_id: Mapped[int] = mapped_column(ForeignKey("agent_data_asset.id", ondelete="RESTRICT"), nullable=False)

    agent: Mapped[AgentDefinition] = relationship(back_populates="data_asset_links")
    data_asset: Mapped["AgentDataAsset"] = relationship()


class AgentDataSourceLink(Base):
    __tablename__ = "agent_data_source_link"
    __table_args__ = (UniqueConstraint("agent_id", "data_source_id", name="uq_agent_data_source_link"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    agent_id: Mapped[int] = mapped_column(ForeignKey("agent_definition.id", ondelete="CASCADE"), nullable=False)
    data_source_id: Mapped[int] = mapped_column(ForeignKey("data_source.id", ondelete="RESTRICT"), nullable=False)

    agent: Mapped[AgentDefinition] = relationship(back_populates="data_source_links")


class AgentKnowledgeBaseLink(Base):
    __tablename__ = "agent_knowledge_base_link"
    __table_args__ = (UniqueConstraint("agent_id", "knowledge_base_id", name="uq_agent_knowledge_base_link"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    agent_id: Mapped[int] = mapped_column(ForeignKey("agent_definition.id", ondelete="CASCADE"), nullable=False)
    knowledge_base_id: Mapped[int] = mapped_column(ForeignKey("knowledge_base.id", ondelete="RESTRICT"), nullable=False)

    agent: Mapped[AgentDefinition] = relationship(back_populates="knowledge_links")
    knowledge_base: Mapped["KnowledgeBase"] = relationship()


class AgentDataAsset(TimestampMixin, Base):
    __tablename__ = "agent_data_asset"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    asset_code: Mapped[str] = mapped_column(String(64), unique=True, nullable=False, index=True)
    table_name: Mapped[str] = mapped_column(String(128), unique=True, nullable=False)
    display_name: Mapped[str] = mapped_column(String(128), nullable=False)
    description: Mapped[str | None] = mapped_column(Text)
    allowed_columns: Mapped[list[str]] = mapped_column(JSON, nullable=False, default=list)
    governance_status: Mapped[str] = mapped_column(String(32), nullable=False, default="PENDING")
    source_health: Mapped[str] = mapped_column(String(32), nullable=False, default="UNKNOWN")
    last_governed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    governance_report_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    row_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    enabled: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    last_inspected_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class KnowledgeBase(TimestampMixin, Base):
    __tablename__ = "knowledge_base"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    kb_code: Mapped[str] = mapped_column(String(64), unique=True, nullable=False, index=True)
    kb_name: Mapped[str] = mapped_column(String(128), nullable=False)
    description: Mapped[str | None] = mapped_column(Text)
    status: Mapped[str] = mapped_column(String(32), nullable=False, default="DRAFT")
    version: Mapped[str] = mapped_column(String(32), nullable=False, default="1.0.0")
    source_tables: Mapped[list[str]] = mapped_column(JSON, nullable=False, default=list)
    entity_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    relation_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    enabled: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)


class KnowledgeGraph(TimestampMixin, Base):
    __tablename__ = "knowledge_graph"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    knowledge_base_id: Mapped[int] = mapped_column(ForeignKey("knowledge_base.id", ondelete="RESTRICT"), nullable=False, index=True)
    graph_code: Mapped[str] = mapped_column(String(64), unique=True, nullable=False, index=True)
    graph_name: Mapped[str] = mapped_column(String(128), nullable=False)
    description: Mapped[str | None] = mapped_column(Text)
    symbol: Mapped[str | None] = mapped_column(String(32))
    source_tables: Mapped[list[str]] = mapped_column(JSON, nullable=False, default=list)
    version: Mapped[str] = mapped_column(String(32), nullable=False, default="1.0.0")
    governance_status: Mapped[str] = mapped_column(String(32), nullable=False, default="PENDING")
    enabled: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    entity_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    relation_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    last_governed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    governance_report_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)


class GovernanceRun(TimestampMixin, Base):
    __tablename__ = "governance_run"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    target_type: Mapped[str] = mapped_column(String(32), nullable=False, index=True)
    target_id: Mapped[int] = mapped_column(Integer, nullable=False, index=True)
    agent_id: Mapped[int | None] = mapped_column(ForeignKey("agent_definition.id", ondelete="SET NULL"))
    model_call_log_id: Mapped[int | None] = mapped_column(ForeignKey("model_call_log.id", ondelete="SET NULL"))
    source_asset_ids: Mapped[list[int]] = mapped_column(JSON, nullable=False, default=list)
    status: Mapped[str] = mapped_column(String(32), nullable=False)
    summary_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)


class KnowledgeDocument(TimestampMixin, Base):
    __tablename__ = "knowledge_document"
    __table_args__ = (Index("ix_knowledge_document_kb_symbol", "knowledge_base_id", "symbol"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    knowledge_base_id: Mapped[int] = mapped_column(ForeignKey("knowledge_base.id", ondelete="CASCADE"), nullable=False)
    graph_id: Mapped[int | None] = mapped_column(ForeignKey("knowledge_graph.id", ondelete="CASCADE"), index=True)
    source_table: Mapped[str] = mapped_column(String(128), nullable=False)
    source_record_id: Mapped[int | None] = mapped_column(Integer)
    market: Mapped[str | None] = mapped_column(String(16))
    symbol: Mapped[str | None] = mapped_column(String(32), index=True)
    title: Mapped[str] = mapped_column(String(512), nullable=False)
    content: Mapped[str] = mapped_column(Text, nullable=False, default="")
    metadata_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)


class KnowledgeEntity(TimestampMixin, Base):
    __tablename__ = "knowledge_entity"
    __table_args__ = (
        UniqueConstraint("knowledge_base_id", "entity_type", "entity_key", name="uq_knowledge_entity_key"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    knowledge_base_id: Mapped[int] = mapped_column(ForeignKey("knowledge_base.id", ondelete="CASCADE"), nullable=False)
    graph_id: Mapped[int | None] = mapped_column(ForeignKey("knowledge_graph.id", ondelete="CASCADE"), index=True)
    entity_type: Mapped[str] = mapped_column(String(64), nullable=False)
    entity_key: Mapped[str] = mapped_column(String(256), nullable=False)
    entity_name: Mapped[str] = mapped_column(String(256), nullable=False)
    properties_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)


class KnowledgeRelation(TimestampMixin, Base):
    __tablename__ = "knowledge_relation"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    knowledge_base_id: Mapped[int] = mapped_column(ForeignKey("knowledge_base.id", ondelete="CASCADE"), nullable=False)
    graph_id: Mapped[int | None] = mapped_column(ForeignKey("knowledge_graph.id", ondelete="CASCADE"), index=True)
    subject_entity_id: Mapped[int] = mapped_column(ForeignKey("knowledge_entity.id", ondelete="CASCADE"), nullable=False)
    predicate: Mapped[str] = mapped_column(String(128), nullable=False)
    object_entity_id: Mapped[int] = mapped_column(ForeignKey("knowledge_entity.id", ondelete="CASCADE"), nullable=False)
    evidence_document_id: Mapped[int | None] = mapped_column(ForeignKey("knowledge_document.id", ondelete="SET NULL"))


class ResearchReportRecord(TimestampMixin, Base):
    __tablename__ = "research_report"
    __table_args__ = (Index("ix_research_report_symbol_created", "market", "symbol", "created_at"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    market: Mapped[str] = mapped_column(String(16), nullable=False, index=True)
    symbol: Mapped[str] = mapped_column(String(32), nullable=False, index=True)
    name: Mapped[str] = mapped_column(String(128), nullable=False, default="")
    title: Mapped[str] = mapped_column(String(256), nullable=False, default="AI 深度研究报告")
    report_markdown: Mapped[str] = mapped_column(Text, nullable=False, default="")
    conclusion: Mapped[str | None] = mapped_column(Text)
    rating: Mapped[str | None] = mapped_column(String(32))
    score: Mapped[int | None] = mapped_column(Integer)
    model_provider: Mapped[str | None] = mapped_column(String(64))
    model_instance: Mapped[str | None] = mapped_column(String(64))
    data_sources_json: Mapped[list[str]] = mapped_column(JSON, nullable=False, default=list)
    knowledge_base_ids_json: Mapped[list[int]] = mapped_column(JSON, nullable=False, default=list)
    agent_snapshot_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    history_evaluation_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    warnings_json: Mapped[list[str]] = mapped_column(JSON, nullable=False, default=list)


class PredictionLedger(TimestampMixin, Base):
    __tablename__ = "prediction_ledger"
    __table_args__ = (Index("ix_prediction_ledger_stock_time", "market", "stock_code", "predicted_at"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    market: Mapped[str] = mapped_column(String(16), nullable=False)
    stock_code: Mapped[str] = mapped_column(String(32), nullable=False)
    action_type: Mapped[str] = mapped_column(String(16), nullable=False)
    predicted_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, nullable=False)
    target_timeframe: Mapped[str] = mapped_column(String(16), nullable=False)
    reasoning_logic: Mapped[str] = mapped_column(Text, nullable=False)
    skill_code: Mapped[str] = mapped_column(String(64), nullable=False)
    skill_version_used: Mapped[str] = mapped_column(String(32), nullable=False)
    model_instance_code: Mapped[str | None] = mapped_column(String(64))
    research_report_id: Mapped[int | None] = mapped_column(ForeignKey("research_report.id", ondelete="SET NULL"))
    entry_price: Mapped[float] = mapped_column(nullable=False)
    target_return_pct: Mapped[float | None] = mapped_column()
    status: Mapped[str] = mapped_column(String(24), nullable=False, default="PENDING")
    actual_price: Mapped[float | None] = mapped_column()
    actual_return_pct: Mapped[float | None] = mapped_column()
    direction_hit: Mapped[bool | None] = mapped_column(Boolean)
    price_as_of: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    evaluation_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    evaluated_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class SkillOptimizationDraft(Base):
    __tablename__ = "skill_optimization_draft"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    skill_id: Mapped[int] = mapped_column(ForeignKey("model_skill.id", ondelete="CASCADE"), nullable=False)
    base_skill_version: Mapped[str] = mapped_column(String(32), nullable=False)
    failure_signature: Mapped[str] = mapped_column(String(128), unique=True, nullable=False)
    prediction_ids: Mapped[list[int]] = mapped_column(JSON, nullable=False, default=list)
    proposed_instructions: Mapped[str] = mapped_column(Text, nullable=False)
    rationale: Mapped[str] = mapped_column(Text, nullable=False)
    status: Mapped[str] = mapped_column(String(24), nullable=False, default="PENDING_REVIEW")
    model_call_log_id: Mapped[int | None] = mapped_column(ForeignKey("model_call_log.id", ondelete="SET NULL"))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, nullable=False)
    reviewed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class ModelCallLog(Base):
    __tablename__ = "model_call_log"
    __table_args__ = (Index("ix_model_call_log_task_started", "task_type", "started_at"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    task_type: Mapped[str] = mapped_column(String(64), nullable=False)
    provider_code: Mapped[str | None] = mapped_column(String(64))
    instance_code: Mapped[str | None] = mapped_column(String(64))
    model_code: Mapped[str | None] = mapped_column(String(128))
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="RUNNING")
    request_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    response_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    response_text: Mapped[str | None] = mapped_column(Text)
    error_message: Mapped[str | None] = mapped_column(Text)
    latency_ms: Mapped[int | None] = mapped_column(Integer)
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, nullable=False)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
