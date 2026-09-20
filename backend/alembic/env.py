"""Run the isolated foundation history through its checked CLI."""

from alembic import context


config = context.config
version_table = "foundation_schema_revision"

if context.is_offline_mode():
    context.configure(
        dialect_name=config.attributes.get("preview_dialect", "sqlite"),
        version_table=version_table,
        literal_binds=True,
    )
    with context.begin_transaction():
        context.run_migrations()
else:
    connection = config.attributes.get("connection")
    if connection is None or not config.attributes.get("checked_foundation_apply"):
        raise RuntimeError(
            "Use python scripts/migrate_foundation.py check/apply; "
            "direct Alembic commands are disabled for this isolated history."
        )
    context.configure(connection=connection, version_table=version_table)
    with context.begin_transaction():
        context.run_migrations()
