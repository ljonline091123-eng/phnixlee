"""Create the independent foundation schema without altering legacy tables.

Revision ID: foundation_0001
Revises: None
"""

from alembic import context, op
import sqlalchemy as sa


revision = "foundation_0001"
down_revision = None
branch_labels = ("foundation",)
depends_on = None


def build_metadata() -> sa.MetaData:
    """Frozen database schema; never import mutable application model metadata."""
    metadata = sa.MetaData()
    sa.Table("stock_symbol", metadata, sa.Column("id", sa.Integer, primary_key=True))

    def identity() -> sa.Column:
        return sa.Column("id", sa.String(36), primary_key=True)

    def timestamp(name: str = "created_at") -> sa.Column:
        return sa.Column(name, sa.DateTime(timezone=True), nullable=False)

    def reference(name: str, target: str, *, nullable: bool = False, index: bool = False) -> sa.Column:
        return sa.Column(name, sa.String(36), sa.ForeignKey(target, ondelete="RESTRICT"), nullable=nullable, index=index)

    sa.Table(
        "foundation_entity", metadata,
        identity(),
        sa.Column("name", sa.String(256), nullable=False, index=True),
        sa.Column("entity_type", sa.String(32), nullable=False, index=True),
        sa.Column("jurisdiction", sa.String(64), nullable=False),
        sa.Column("identifier_scheme", sa.String(64)),
        sa.Column("identifier_value", sa.String(128)),
        sa.Column("properties_json", sa.JSON, nullable=False),
        timestamp(),
        sa.UniqueConstraint("jurisdiction", "identifier_scheme", "identifier_value", name="uq_foundation_entity_identifier"),
    )
    sa.Table(
        "foundation_security", metadata,
        identity(),
        reference("entity_id", "foundation_entity.id", index=True),
        sa.Column("share_class", sa.String(64)),
        timestamp(),
    )
    sa.Table(
        "foundation_evidence", metadata,
        identity(),
        reference("entity_id", "foundation_entity.id", nullable=True, index=True),
        sa.Column("source_name", sa.String(256), nullable=False),
        sa.Column("source_key", sa.String(512), nullable=False),
        sa.Column("title", sa.String(512), nullable=False),
        sa.Column("content", sa.Text, nullable=False),
        sa.Column("url", sa.String(2048)),
        sa.Column("published_at", sa.DateTime(timezone=True)),
        sa.Column("available_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("content_hash", sa.String(64), nullable=False),
        sa.Column("fingerprint", sa.String(64), nullable=False),
        sa.Column("version", sa.Integer, nullable=False),
        sa.Column("metadata_json", sa.JSON, nullable=False),
        timestamp(),
        sa.UniqueConstraint("source_name", "source_key", "version", name="uq_foundation_evidence_version"),
        sa.UniqueConstraint("source_name", "source_key", "fingerprint", name="uq_foundation_evidence_fingerprint"),
    )
    sa.Table(
        "foundation_listing", metadata,
        identity(),
        reference("security_id", "foundation_security.id", index=True),
        sa.Column("stock_symbol_id", sa.Integer, sa.ForeignKey("stock_symbol.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("market", sa.String(16), nullable=False),
        sa.Column("symbol", sa.String(32), nullable=False),
        sa.Column("name", sa.String(128), nullable=False),
        reference("evidence_id", "foundation_evidence.id"),
        timestamp(),
        sa.UniqueConstraint("stock_symbol_id", name="uq_foundation_listing_stock"),
    )
    sa.Table(
        "foundation_fact", metadata,
        identity(),
        sa.Column("fact_type", sa.String(32), nullable=False),
        sa.Column("title", sa.String(512), nullable=False),
        reference("subject_entity_id", "foundation_entity.id", index=True),
        reference("object_entity_id", "foundation_entity.id", nullable=True, index=True),
        sa.Column("properties_json", sa.JSON, nullable=False),
        sa.Column("status", sa.String(16), nullable=False),
        sa.Column("valid_from", sa.Date),
        sa.Column("valid_to", sa.Date),
        timestamp(),
        timestamp("updated_at"),
        sa.Index("ix_foundation_fact_status_type", "status", "fact_type"),
    )
    sa.Table(
        "foundation_fact_evidence", metadata,
        sa.Column("fact_id", sa.String(36), sa.ForeignKey("foundation_fact.id", ondelete="RESTRICT"), primary_key=True),
        sa.Column("evidence_id", sa.String(36), sa.ForeignKey("foundation_evidence.id", ondelete="RESTRICT"), primary_key=True),
    )
    sa.Table(
        "foundation_fact_review", metadata,
        identity(),
        reference("fact_id", "foundation_fact.id"),
        sa.Column("previous_status", sa.String(16), nullable=False),
        sa.Column("decision", sa.String(16), nullable=False),
        sa.Column("reason", sa.String(4000), nullable=False),
        sa.Column("reviewer", sa.String(128), nullable=False),
        timestamp(),
        sa.Index("ix_foundation_review_fact_time", "fact_id", "created_at"),
    )
    return metadata


def upgrade() -> None:
    metadata = build_metadata()
    existing = set() if context.is_offline_mode() else set(sa.inspect(op.get_bind()).get_table_names())
    for table in metadata.sorted_tables:
        if table.name.startswith("foundation_") and table.name not in existing:
            table.create(op.get_bind(), checkfirst=False)


def downgrade() -> None:
    raise RuntimeError("Destructive foundation downgrade is disabled. Restore an explicitly selected backup instead.")
