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
        if "model_skill" in tables:
            existing = {column["name"] for column in inspector.get_columns("model_skill")}
            additions = (
                ("file_path", "VARCHAR(512)"),
                ("content_hash", "VARCHAR(128)"),
                ("version", "VARCHAR(32) NOT NULL DEFAULT '1.0.0'"),
                ("is_builtin", "BOOLEAN NOT NULL DEFAULT 0"),
                ("format", "VARCHAR(16) NOT NULL DEFAULT 'MD'"),
            )
            for name, definition in additions:
                if name not in existing:
                    connection.execute(text(f'ALTER TABLE model_skill ADD COLUMN "{name}" {definition}'))
        if "stock_financial_report" in tables:
            existing = {column["name"] for column in inspector.get_columns("stock_financial_report")}
            if "url" not in existing:
                connection.execute(text('ALTER TABLE stock_financial_report ADD COLUMN "url" VARCHAR(2048)'))
