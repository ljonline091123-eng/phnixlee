from __future__ import annotations

from sqlalchemy import inspect, text
from sqlalchemy.engine import Engine


def ensure_compat_columns(engine: Engine) -> None:
    """Apply small, idempotent SQLite-compatible additions for existing databases.

    The project currently bootstraps with SQLAlchemy ``create_all`` rather than Alembic.
    New tables are created normally; columns added to long-lived tables are added here
    without rewriting or replacing user data.
    """

    inspector = inspect(engine)
    tables = set(inspector.get_table_names())
    with engine.begin() as connection:
        for table_name, additions in (
            ("model_provider", (("api_base_url", "VARCHAR(512)"), ("api_key_encrypted", "TEXT"))),
            ("model_instance", (("usage_type", "VARCHAR(16) NOT NULL DEFAULT 'EXACT'"),)),
            ("agent_definition", (("context_window_limit", "INTEGER NOT NULL DEFAULT 12"), ("json_schema_output", "JSON NOT NULL DEFAULT '{}'"))),
        ):
            if table_name in tables:
                existing = {column["name"] for column in inspector.get_columns(table_name)}
                for name, definition in additions:
                    if name not in existing:
                        connection.execute(text(f'ALTER TABLE "{table_name}" ADD COLUMN "{name}" {definition}'))
        if "model_skill" in tables:
            existing = {column["name"] for column in inspector.get_columns("model_skill")}
            additions = (
                ("file_path", "VARCHAR(512)"),
                ("content_hash", "VARCHAR(128)"),
                ("version", "VARCHAR(32) NOT NULL DEFAULT '1.0.0'"),
                ("is_builtin", "BOOLEAN NOT NULL DEFAULT 0"),
                ("format", "VARCHAR(16) NOT NULL DEFAULT 'MD'"),
                ("skill_type", "VARCHAR(32) NOT NULL DEFAULT 'PROMPT_SOP'"),
            )
            for name, definition in additions:
                if name not in existing:
                    connection.execute(text(f'ALTER TABLE model_skill ADD COLUMN "{name}" {definition}'))
        if "stock_financial_report" in tables:
            existing = {column["name"] for column in inspector.get_columns("stock_financial_report")}
            if "url" not in existing:
                connection.execute(text('ALTER TABLE stock_financial_report ADD COLUMN "url" VARCHAR(2048)'))
        if "agent_data_asset" in tables:
            existing = {column["name"] for column in inspector.get_columns("agent_data_asset")}
            for name, definition in (
                ("source_health", "VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN'"),
                ("last_governed_at", "DATETIME"),
                ("governance_report_json", "JSON NOT NULL DEFAULT '{}'"),
            ):
                if name not in existing:
                    connection.execute(text(f'ALTER TABLE agent_data_asset ADD COLUMN "{name}" {definition}'))
        for table_name in ("knowledge_document", "knowledge_entity", "knowledge_relation"):
            if table_name in tables:
                existing = {column["name"] for column in inspector.get_columns(table_name)}
                if "graph_id" not in existing:
                    connection.execute(text(f'ALTER TABLE "{table_name}" ADD COLUMN graph_id INTEGER'))
        if "skill_optimization_draft" in tables:
            existing = {column["name"] for column in inspector.get_columns("skill_optimization_draft")}
            if "base_skill_version" not in existing:
                connection.execute(text(
                    "ALTER TABLE skill_optimization_draft ADD COLUMN "
                    "base_skill_version VARCHAR(32) NOT NULL DEFAULT '1.0.0'"
                ))
                if "skill_id" in existing and "model_skill" in tables:
                    connection.execute(text(
                        "UPDATE skill_optimization_draft SET base_skill_version = "
                        "COALESCE((SELECT version FROM model_skill "
                        "WHERE model_skill.id = skill_optimization_draft.skill_id), '1.0.0')"
                    ))
