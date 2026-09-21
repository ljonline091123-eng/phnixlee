"""Add source identity aliases and security-level classifications only."""

import importlib.util
from pathlib import Path

from alembic import context, op
import sqlalchemy as sa


revision = "foundation_0002"
down_revision = "foundation_0001"
branch_labels = None
depends_on = None


def build_metadata() -> sa.MetaData:
    spec = importlib.util.spec_from_file_location("foundation_schema_0001", Path(__file__).with_name("foundation_0001.py"))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    metadata = module.build_metadata()
    sa.Table(
        "foundation_source_identity", metadata,
        sa.Column("id", sa.String(36), primary_key=True),
        sa.Column("entity_id", sa.String(36), sa.ForeignKey("foundation_entity.id", ondelete="RESTRICT"), nullable=False, index=True),
        sa.Column("namespace", sa.String(256), nullable=False),
        sa.Column("external_id", sa.String(128), nullable=False),
        sa.Column("entity_type", sa.String(32), nullable=False),
        sa.Column("jurisdiction", sa.String(64), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.UniqueConstraint("namespace", "external_id", "entity_type", name="uq_foundation_source_identity"),
    )
    sa.Table(
        "foundation_security_classification", metadata,
        sa.Column("id", sa.String(36), primary_key=True),
        sa.Column("security_id", sa.String(36), sa.ForeignKey("foundation_security.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("evidence_id", sa.String(36), sa.ForeignKey("foundation_evidence.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("dimension", sa.String(64), nullable=False),
        sa.Column("code", sa.String(128), nullable=False),
        sa.Column("label", sa.String(256), nullable=False),
        sa.Column("definition_version", sa.String(128), nullable=False),
        sa.Column("method", sa.String(256), nullable=False),
        sa.Column("properties_json", sa.JSON, nullable=False),
        sa.Column("status", sa.String(16), nullable=False),
        sa.Column("valid_from", sa.Date),
        sa.Column("valid_to", sa.Date),
        sa.Column("reviews_json", sa.JSON, nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
        sa.Index("ix_foundation_classification_security_status", "security_id", "status"),
    )
    return metadata


def upgrade() -> None:
    metadata = build_metadata()
    existing = set() if context.is_offline_mode() else set(sa.inspect(op.get_bind()).get_table_names())
    for name in ("foundation_source_identity", "foundation_security_classification"):
        if name not in existing:
            metadata.tables[name].create(op.get_bind(), checkfirst=False)


def downgrade() -> None:
    raise RuntimeError("Destructive downgrade is disabled; retain the additive tables or restore a selected backup.")
