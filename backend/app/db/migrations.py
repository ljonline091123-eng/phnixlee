from __future__ import annotations

from sqlalchemy import inspect, text
from sqlalchemy.engine import Engine


def ensure_compat_columns(engine: Engine) -> None:
    """Apply small, idempotent SQLite-compatible additions for existing databases.

    The project currently bootstraps with SQLAlchemy ``create_all`` rather than Alembic.
    New tables are created normally; columns added to long-lived tables are added here
    without rewriting or replacing user data.
    """

    # Keep schema inspection on the same connection as the migration writes.
    # ``inspect(engine)`` checks out a second SQLite connection.  Once an
    # earlier ALTER/UPDATE in this transaction has taken the schema/write
    # lock, that second connection can block on a later PRAGMA (and eventually
    # raise ``database is locked``).  A connection-bound inspector avoids the
    # self-deadlock while retaining the existing idempotent migration logic.
    with engine.begin() as connection:
        inspector = inspect(connection)
        tables = set(inspector.get_table_names())
        for table_name, additions in (
            ("model_provider", (("api_base_url", "VARCHAR(512)"), ("api_key_encrypted", "TEXT"))),
            ("model_instance", (("usage_type", "VARCHAR(16) NOT NULL DEFAULT 'EXACT'"),)),
            ("agent_definition", (
                ("context_window_limit", "INTEGER NOT NULL DEFAULT 12"),
                ("json_schema_output", "JSON NOT NULL DEFAULT '{}'"),
                ("lifecycle_status", "VARCHAR(24) NOT NULL DEFAULT 'DRAFT'"),
                ("policy_json", "JSON NOT NULL DEFAULT '{}'"),
                ("knowledge_version_policy_json", "JSON NOT NULL DEFAULT '{}'"),
            )),
        ):
            if table_name in tables:
                existing = {column["name"] for column in inspector.get_columns(table_name)}
                for name, definition in additions:
                    if name not in existing:
                        connection.execute(text(f'ALTER TABLE "{table_name}" ADD COLUMN "{name}" {definition}'))
        if "model_skill" in tables:
            existing = {column["name"] for column in inspector.get_columns("model_skill")}
            lifecycle_added = "lifecycle_status" not in existing
            additions = (
                ("file_path", "VARCHAR(512)"),
                ("content_hash", "VARCHAR(128)"),
                ("version", "VARCHAR(32) NOT NULL DEFAULT '1.0.0'"),
                ("is_builtin", "BOOLEAN NOT NULL DEFAULT 0"),
                ("format", "VARCHAR(16) NOT NULL DEFAULT 'MD'"),
                ("skill_type", "VARCHAR(32) NOT NULL DEFAULT 'PROMPT_SOP'"),
                ("lifecycle_status", "VARCHAR(24) NOT NULL DEFAULT 'DRAFT'"),
                ("input_contract_json", "JSON NOT NULL DEFAULT '{}'"),
                ("output_contract_json", "JSON NOT NULL DEFAULT '{}'"),
                ("permission_policy_json", "JSON NOT NULL DEFAULT '{}'"),
                ("side_effect_level", "VARCHAR(24) NOT NULL DEFAULT 'READ_ONLY'"),
                ("idempotency_policy", "VARCHAR(32) NOT NULL DEFAULT 'NONE'"),
                ("retry_policy_json", "JSON NOT NULL DEFAULT '{}'"),
                ("error_policy_json", "JSON NOT NULL DEFAULT '{}'"),
            )
            for name, definition in additions:
                if name not in existing:
                    connection.execute(text(f'ALTER TABLE model_skill ADD COLUMN "{name}" {definition}'))
            if lifecycle_added and "enabled" in existing:
                connection.execute(text(
                    "UPDATE model_skill SET lifecycle_status = "
                    "CASE WHEN enabled THEN 'ENABLED' ELSE 'DISABLED' END "
                    "WHERE lifecycle_status IS NULL OR lifecycle_status = 'DRAFT'"
                ))
        if "model_skill_revision" in tables:
            existing = {column["name"] for column in inspector.get_columns("model_skill_revision")}
            if "governance_json" not in existing:
                connection.execute(text(
                    "ALTER TABLE model_skill_revision ADD COLUMN governance_json JSON NOT NULL DEFAULT '{}'"
                ))
        if "agent_definition" in tables:
            existing = {column["name"] for column in inspector.get_columns("agent_definition")}
            lifecycle_added = "lifecycle_status" not in existing
            if lifecycle_added and "enabled" in existing:
                connection.execute(text(
                    "UPDATE agent_definition SET lifecycle_status = "
                    "CASE WHEN enabled THEN 'ENABLED' ELSE 'DISABLED' END "
                    "WHERE lifecycle_status IS NULL OR lifecycle_status = 'DRAFT'"
                ))
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
                ("asset_type", "VARCHAR(32) NOT NULL DEFAULT 'DATABASE_TABLE'"),
                ("canonical_identity", "VARCHAR(256)"),
                ("metadata_json", "JSON NOT NULL DEFAULT '{}'"),
            ):
                if name not in existing:
                    connection.execute(text(f'ALTER TABLE agent_data_asset ADD COLUMN "{name}" {definition}'))
            connection.execute(text(
                "CREATE INDEX IF NOT EXISTS ix_agent_data_asset_canonical_identity "
                "ON agent_data_asset (canonical_identity)"
            ))
        if "pipeline_stage_run" in tables:
            existing = {column["name"] for column in inspector.get_columns("pipeline_stage_run")}
            for name, definition in (
                ("agent_id", "INTEGER"),
                ("agent_version", "VARCHAR(32)"),
                ("skill_id", "INTEGER"),
                ("skill_code", "VARCHAR(64)"),
                ("skill_version", "VARCHAR(32)"),
                ("input_hash", "VARCHAR(64)"),
                ("output_hash", "VARCHAR(64)"),
                ("evidence_ids_json", "JSON NOT NULL DEFAULT '[]'"),
            ):
                if name not in existing:
                    connection.execute(text(f'ALTER TABLE pipeline_stage_run ADD COLUMN "{name}" {definition}'))
            connection.execute(text(
                "CREATE INDEX IF NOT EXISTS ix_pipeline_stage_run_agent_id ON pipeline_stage_run (agent_id)"
            ))
            connection.execute(text(
                "CREATE INDEX IF NOT EXISTS ix_pipeline_stage_run_skill_id ON pipeline_stage_run (skill_id)"
            ))
        if "document_chunk_version" in tables:
            existing = {column["name"] for column in inspector.get_columns("document_chunk_version")}
            for name, definition in (
                ("section_title", "VARCHAR(512)"),
                ("section_path_json", "JSON NOT NULL DEFAULT '[]'"),
                ("section_status", "VARCHAR(24) NOT NULL DEFAULT 'UNRESOLVED'"),
                ("embedding_status", "VARCHAR(24) NOT NULL DEFAULT 'MISSING'"),
                ("embedding_kind", "VARCHAR(24) NOT NULL DEFAULT 'NONE'"),
                ("source_object_id", "VARCHAR(36)"),
                ("lineage_batch_id", "VARCHAR(64)"),
            ):
                if name not in existing:
                    connection.execute(text(f'ALTER TABLE document_chunk_version ADD COLUMN "{name}" {definition}'))
            connection.execute(text(
                "CREATE INDEX IF NOT EXISTS ix_document_chunk_version_source_object_id "
                "ON document_chunk_version (source_object_id)"
            ))
            connection.execute(text(
                "CREATE INDEX IF NOT EXISTS ix_document_chunk_version_lineage_batch_id "
                "ON document_chunk_version (lineage_batch_id)"
            ))
            # Correct stale defaults even when a previous partial migration
            # already added the columns. Hash fingerprints are never semantic
            # vectors; non-hash model names remain PENDING until a provider has
            # written a verifiable vector payload.
            connection.execute(text(
                "UPDATE document_chunk_version SET embedding_kind = 'HASH', embedding_status = 'HASH_ONLY' "
                "WHERE UPPER(COALESCE(embedding_model, '')) LIKE 'HASH%' "
                "AND (embedding_kind <> 'HASH' OR embedding_status <> 'HASH_ONLY')"
            ))
            connection.execute(text(
                "UPDATE document_chunk_version SET embedding_kind = 'SEMANTIC', embedding_status = 'PENDING' "
                "WHERE COALESCE(embedding_model, '') <> '' "
                "AND UPPER(embedding_model) NOT LIKE 'HASH%' "
                "AND (embedding_kind IS NULL OR embedding_kind = 'NONE' OR embedding_status = 'MISSING')"
            ))
            if "lake_lineage_event" in tables:
                # Existing databases only indexed batch_id.  The compatibility
                # backfill joins by downstream type and identifier, so without
                # this index every startup performs a correlated full scan.
                connection.execute(text(
                    "CREATE INDEX IF NOT EXISTS ix_lake_lineage_event_downstream_lookup "
                    "ON lake_lineage_event (downstream_type, downstream_id, id)"
                ))
                connection.execute(text(
                    "UPDATE document_chunk_version SET "
                    "source_object_id = COALESCE(source_object_id, ("
                    "SELECT upstream_id FROM lake_lineage_event "
                    "WHERE downstream_type = 'DOCUMENT_CHUNK' "
                    "AND downstream_id = document_chunk_version.id "
                    "ORDER BY id DESC LIMIT 1)), "
                    "lineage_batch_id = COALESCE(lineage_batch_id, ("
                    "SELECT batch_id FROM lake_lineage_event "
                    "WHERE downstream_type = 'DOCUMENT_CHUNK' "
                    "AND downstream_id = document_chunk_version.id "
                    "ORDER BY id DESC LIMIT 1)) "
                    "WHERE (source_object_id IS NULL OR lineage_batch_id IS NULL) "
                    "AND EXISTS (SELECT 1 FROM lake_lineage_event "
                    "WHERE downstream_type = 'DOCUMENT_CHUNK' "
                    "AND downstream_id = document_chunk_version.id)"
                ))
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
        if "database_table_comment" in tables:
            existing = {column["name"] for column in inspector.get_columns("database_table_comment")}
            if "column_comments_json" not in existing:
                connection.execute(text(
                    "ALTER TABLE database_table_comment ADD COLUMN "
                    "column_comments_json JSON NOT NULL DEFAULT '{}'"
                ))
        if "selection_tracking" in tables:
            existing = {column["name"] for column in inspector.get_columns("selection_tracking")}
            if "confirmation_date" not in existing:
                connection.execute(text(
                    "ALTER TABLE selection_tracking ADD COLUMN confirmation_date VARCHAR(16) NOT NULL DEFAULT '1970-01-01'"
                ))
            if "adjust" not in existing:
                connection.execute(text("ALTER TABLE selection_tracking ADD COLUMN adjust VARCHAR(16) NOT NULL DEFAULT ''"))
            if "target_hit" not in existing:
                connection.execute(text("ALTER TABLE selection_tracking ADD COLUMN target_hit BOOLEAN"))
            if "stop_hit" not in existing:
                connection.execute(text("ALTER TABLE selection_tracking ADD COLUMN stop_hit BOOLEAN"))
