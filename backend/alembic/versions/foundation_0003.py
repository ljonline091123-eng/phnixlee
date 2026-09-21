"""Add auditable issuer mapping outcomes without changing existing mappings."""

import importlib.util
from pathlib import Path

from alembic import context, op
import sqlalchemy as sa

revision = "foundation_0003"
down_revision = "foundation_0002"
branch_labels = None
depends_on = None


def build_metadata() -> sa.MetaData:
    """Frozen complete schema for checked adoption; never import app models."""
    spec = importlib.util.spec_from_file_location("foundation_schema_0002", Path(__file__).with_name("foundation_0002.py"))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    metadata = module.build_metadata()
    sa.Table("foundation_company_mapping_state", metadata,
        sa.Column("stock_symbol_id", sa.Integer, sa.ForeignKey("stock_symbol.id", ondelete="RESTRICT"), primary_key=True),
        sa.Column("status", sa.String(32), nullable=False),
        sa.Column("reason", sa.String(1024), nullable=False),
        sa.Column("source_name", sa.String(256)),
        sa.Column("source_snapshot", sa.String(512)),
        sa.Column("attempt_count", sa.Integer, nullable=False),
        sa.Column("details_json", sa.JSON, nullable=False),
        sa.Column("attempted_at", sa.DateTime(timezone=True), nullable=False),
        sa.Index("ix_foundation_company_mapping_state_status", "status"))
    return metadata


def upgrade():
    if not context.is_offline_mode() and "foundation_company_mapping_state" in sa.inspect(op.get_bind()).get_table_names():
        return
    build_metadata().tables["foundation_company_mapping_state"].create(op.get_bind(), checkfirst=False)


def downgrade():
    raise RuntimeError("Destructive downgrade is disabled; retain the additive mapping state table.")
