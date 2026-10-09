"""Persistent data-quality and controlled Agent/Skill execution records.

These tables are deliberately additive.  They reference the existing catalog,
pipeline, Agent, Skill and model-call records instead of introducing a second
asset or orchestration system.
"""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Any
from uuid import uuid4

from sqlalchemy import Boolean, DateTime, Float, ForeignKey, Index, Integer, JSON, String, Text, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


def new_id() -> str:
    return str(uuid4())


class DataQualityRule(Base):
    """Versioned rule applied to a governed asset, dataset or knowledge object."""

    __tablename__ = "data_quality_rule"
    __table_args__ = (
        UniqueConstraint("rule_code", "version", name="uq_data_quality_rule_version"),
        Index("ix_data_quality_rule_scope_enabled", "asset_scope", "enabled"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    rule_code: Mapped[str] = mapped_column(String(128), nullable=False, index=True)
    rule_name: Mapped[str] = mapped_column(String(256), nullable=False)
    asset_scope: Mapped[str] = mapped_column(String(64), nullable=False, index=True)
    target_code: Mapped[str | None] = mapped_column(String(256), index=True)
    rule_type: Mapped[str] = mapped_column(String(64), nullable=False)
    severity: Mapped[str] = mapped_column(String(16), nullable=False, default="ERROR")
    version: Mapped[str] = mapped_column(String(32), nullable=False, default="1.0.0")
    definition_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    threshold_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    lifecycle_status: Mapped[str] = mapped_column(String(24), nullable=False, default="DRAFT")
    enabled: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    description: Mapped[str | None] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=utc_now, onupdate=utc_now
    )


class DataQualityRun(Base):
    """One immutable quality evaluation batch for a specific target version."""

    __tablename__ = "data_quality_run"
    __table_args__ = (
        Index("ix_data_quality_run_target_time", "target_type", "target_id", "created_at"),
        Index("ix_data_quality_run_status_time", "status", "created_at"),
    )

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    run_key: Mapped[str] = mapped_column(String(192), unique=True, nullable=False, index=True)
    target_type: Mapped[str] = mapped_column(String(64), nullable=False)
    target_id: Mapped[str] = mapped_column(String(256), nullable=False)
    target_version: Mapped[str | None] = mapped_column(String(128))
    pipeline_run_id: Mapped[int | None] = mapped_column(
        ForeignKey("pipeline_run.id", ondelete="SET NULL"), index=True
    )
    status: Mapped[str] = mapped_column(String(24), nullable=False, default="PENDING")
    score: Mapped[float | None] = mapped_column(Float)
    total_rules: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    passed_rules: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    failed_rules: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    warning_rules: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    summary_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    started_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)


class DataQualityIssue(Base):
    """Rule-level quality failure or warning with auditable evidence."""

    __tablename__ = "data_quality_issue"
    __table_args__ = (
        UniqueConstraint("quality_run_id", "rule_code", "target_id", name="uq_data_quality_issue_run_rule_target"),
        Index("ix_data_quality_issue_status_severity", "status", "severity"),
    )

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    quality_run_id: Mapped[str] = mapped_column(
        ForeignKey("data_quality_run.id", ondelete="CASCADE"), nullable=False, index=True
    )
    rule_id: Mapped[int | None] = mapped_column(
        ForeignKey("data_quality_rule.id", ondelete="SET NULL"), index=True
    )
    rule_code: Mapped[str] = mapped_column(String(128), nullable=False)
    target_type: Mapped[str] = mapped_column(String(64), nullable=False)
    target_id: Mapped[str] = mapped_column(String(256), nullable=False)
    issue_code: Mapped[str] = mapped_column(String(128), nullable=False)
    severity: Mapped[str] = mapped_column(String(16), nullable=False, default="ERROR")
    status: Mapped[str] = mapped_column(String(24), nullable=False, default="OPEN")
    message: Mapped[str] = mapped_column(Text, nullable=False)
    observed_value_json: Mapped[Any | None] = mapped_column(JSON)
    expected_value_json: Mapped[Any | None] = mapped_column(JSON)
    evidence_ids_json: Mapped[list[str]] = mapped_column(JSON, nullable=False, default=list)
    details_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    first_detected_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)
    resolved_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    resolution: Mapped[str | None] = mapped_column(Text)


class AgentExecutionRun(Base):
    """Auditable execution snapshot for an Agent invocation."""

    __tablename__ = "agent_execution_run"
    __table_args__ = (
        Index("ix_agent_execution_agent_time", "agent_id", "started_at"),
        Index("ix_agent_execution_status_time", "status", "started_at"),
    )

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    governance_mode: Mapped[str] = mapped_column(
        String(40), nullable=False, default="AI_AGENT_SKILL_GOVERNANCE", index=True
    )
    governance_batch_id: Mapped[str | None] = mapped_column(String(128), index=True)
    run_key: Mapped[str] = mapped_column(String(192), unique=True, nullable=False, index=True)
    parent_run_id: Mapped[str | None] = mapped_column(
        ForeignKey("agent_execution_run.id", ondelete="SET NULL"), index=True
    )
    pipeline_run_id: Mapped[int | None] = mapped_column(
        ForeignKey("pipeline_run.id", ondelete="SET NULL"), index=True
    )
    pipeline_stage_run_id: Mapped[int | None] = mapped_column(
        ForeignKey("pipeline_stage_run.id", ondelete="SET NULL"), index=True
    )
    agent_id: Mapped[int] = mapped_column(
        ForeignKey("agent_definition.id", ondelete="RESTRICT"), nullable=False, index=True
    )
    agent_code: Mapped[str] = mapped_column(String(64), nullable=False)
    agent_version: Mapped[str] = mapped_column(String(32), nullable=False)
    model_instance_code: Mapped[str | None] = mapped_column(String(64))
    model_version: Mapped[str | None] = mapped_column(String(128))
    model_call_log_id: Mapped[int | None] = mapped_column(
        ForeignKey("model_call_log.id", ondelete="SET NULL"), index=True
    )
    trigger_type: Mapped[str] = mapped_column(String(32), nullable=False, default="API")
    status: Mapped[str] = mapped_column(String(24), nullable=False, default="PENDING")
    correlation_id: Mapped[str | None] = mapped_column(String(128), index=True)
    idempotency_key: Mapped[str | None] = mapped_column(String(192), unique=True, index=True)
    input_hash: Mapped[str | None] = mapped_column(String(64))
    output_hash: Mapped[str | None] = mapped_column(String(64))
    input_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    output_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    accessible_assets_json: Mapped[list[str]] = mapped_column(JSON, nullable=False, default=list)
    knowledge_versions_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    skill_versions_json: Mapped[dict[str, str]] = mapped_column(JSON, nullable=False, default=dict)
    evidence_ids_json: Mapped[list[str]] = mapped_column(JSON, nullable=False, default=list)
    audit_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    error_code: Mapped[str | None] = mapped_column(String(64))
    error_message: Mapped[str | None] = mapped_column(Text)
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)


class SkillExecutionRun(Base):
    """Controlled Skill execution record including write scope and evidence."""

    __tablename__ = "skill_execution_run"
    __table_args__ = (
        Index("ix_skill_execution_skill_time", "skill_id", "started_at"),
        Index("ix_skill_execution_status_time", "status", "started_at"),
    )

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    governance_mode: Mapped[str] = mapped_column(
        String(40), nullable=False, default="AI_AGENT_SKILL_GOVERNANCE", index=True
    )
    governance_batch_id: Mapped[str | None] = mapped_column(String(128), index=True)
    agent_execution_run_id: Mapped[str | None] = mapped_column(
        ForeignKey("agent_execution_run.id", ondelete="SET NULL"), index=True
    )
    pipeline_stage_run_id: Mapped[int | None] = mapped_column(
        ForeignKey("pipeline_stage_run.id", ondelete="SET NULL"), index=True
    )
    skill_id: Mapped[int] = mapped_column(
        ForeignKey("model_skill.id", ondelete="RESTRICT"), nullable=False, index=True
    )
    skill_code: Mapped[str] = mapped_column(String(64), nullable=False)
    skill_version: Mapped[str] = mapped_column(String(32), nullable=False)
    status: Mapped[str] = mapped_column(String(24), nullable=False, default="PENDING")
    attempt: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    max_attempts: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    idempotency_key: Mapped[str | None] = mapped_column(String(192), unique=True, index=True)
    input_hash: Mapped[str | None] = mapped_column(String(64))
    output_hash: Mapped[str | None] = mapped_column(String(64))
    input_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    output_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    side_effect_level: Mapped[str] = mapped_column(String(24), nullable=False, default="READ_ONLY")
    write_scope_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    evidence_ids_json: Mapped[list[str]] = mapped_column(JSON, nullable=False, default=list)
    model_call_log_id: Mapped[int | None] = mapped_column(
        ForeignKey("model_call_log.id", ondelete="SET NULL"), index=True
    )
    error_code: Mapped[str | None] = mapped_column(String(64))
    error_message: Mapped[str | None] = mapped_column(Text)
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)


class StockGovernanceDetail(Base):
    """Per-stock governance outcome without duplicating governed business facts."""

    __tablename__ = "stock_governance_detail"
    __table_args__ = (
        UniqueConstraint(
            "governance_batch_id", "market", "symbol",
            name="uq_stock_governance_detail_batch_stock",
        ),
        Index("ix_stock_governance_detail_mode_board", "governance_mode", "board_code"),
        Index("ix_stock_governance_detail_status_time", "status", "completed_at"),
    )

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    governance_batch_id: Mapped[str] = mapped_column(String(128), nullable=False, index=True)
    governance_mode: Mapped[str] = mapped_column(
        String(40), nullable=False, default="SYSTEM_GOVERNANCE", index=True
    )
    board_code: Mapped[str] = mapped_column(String(32), nullable=False, index=True)
    market: Mapped[str] = mapped_column(String(16), nullable=False, index=True)
    symbol: Mapped[str] = mapped_column(String(32), nullable=False, index=True)
    stock_name: Mapped[str | None] = mapped_column(String(128))
    pipeline_run_id: Mapped[int | None] = mapped_column(
        ForeignKey("pipeline_run.id", ondelete="SET NULL"), index=True
    )
    agent_execution_run_id: Mapped[str | None] = mapped_column(
        ForeignKey("agent_execution_run.id", ondelete="SET NULL"), index=True
    )
    baseline_batch_id: Mapped[str | None] = mapped_column(String(128), index=True)
    status: Mapped[str] = mapped_column(String(24), nullable=False, default="PENDING", index=True)
    stage_status_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    source_ids_json: Mapped[list[str]] = mapped_column(JSON, nullable=False, default=list)
    evidence_ids_json: Mapped[list[str]] = mapped_column(JSON, nullable=False, default=list)
    quality_summary_json: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utc_now)
