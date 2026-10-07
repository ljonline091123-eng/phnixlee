-- Task 1: additive data-quality and controlled Agent/Skill execution schema.
-- PostgreSQL migration. Existing tables and rows are preserved.

BEGIN;

ALTER TABLE model_skill ADD COLUMN IF NOT EXISTS lifecycle_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT';
ALTER TABLE model_skill ADD COLUMN IF NOT EXISTS input_contract_json JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE model_skill ADD COLUMN IF NOT EXISTS output_contract_json JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE model_skill ADD COLUMN IF NOT EXISTS permission_policy_json JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE model_skill ADD COLUMN IF NOT EXISTS side_effect_level VARCHAR(24) NOT NULL DEFAULT 'READ_ONLY';
ALTER TABLE model_skill ADD COLUMN IF NOT EXISTS idempotency_policy VARCHAR(32) NOT NULL DEFAULT 'NONE';
ALTER TABLE model_skill ADD COLUMN IF NOT EXISTS retry_policy_json JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE model_skill ADD COLUMN IF NOT EXISTS error_policy_json JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE model_skill_revision ADD COLUMN IF NOT EXISTS governance_json JSONB NOT NULL DEFAULT '{}'::jsonb;
UPDATE model_skill SET lifecycle_status = CASE WHEN enabled THEN 'ENABLED' ELSE 'DISABLED' END
WHERE lifecycle_status IS NULL OR lifecycle_status = 'DRAFT';

ALTER TABLE agent_definition ADD COLUMN IF NOT EXISTS lifecycle_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT';
ALTER TABLE agent_definition ADD COLUMN IF NOT EXISTS policy_json JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE agent_definition ADD COLUMN IF NOT EXISTS knowledge_version_policy_json JSONB NOT NULL DEFAULT '{}'::jsonb;
UPDATE agent_definition SET lifecycle_status = CASE WHEN enabled THEN 'ENABLED' ELSE 'DISABLED' END
WHERE lifecycle_status IS NULL OR lifecycle_status = 'DRAFT';

ALTER TABLE agent_data_asset ADD COLUMN IF NOT EXISTS asset_type VARCHAR(32) NOT NULL DEFAULT 'DATABASE_TABLE';
ALTER TABLE agent_data_asset ADD COLUMN IF NOT EXISTS canonical_identity VARCHAR(256);
ALTER TABLE agent_data_asset ADD COLUMN IF NOT EXISTS metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb;
CREATE INDEX IF NOT EXISTS ix_agent_data_asset_canonical_identity ON agent_data_asset (canonical_identity);

ALTER TABLE pipeline_stage_run ADD COLUMN IF NOT EXISTS agent_id INTEGER REFERENCES agent_definition(id) ON DELETE SET NULL;
ALTER TABLE pipeline_stage_run ADD COLUMN IF NOT EXISTS agent_version VARCHAR(32);
ALTER TABLE pipeline_stage_run ADD COLUMN IF NOT EXISTS skill_id INTEGER REFERENCES model_skill(id) ON DELETE SET NULL;
ALTER TABLE pipeline_stage_run ADD COLUMN IF NOT EXISTS skill_code VARCHAR(64);
ALTER TABLE pipeline_stage_run ADD COLUMN IF NOT EXISTS skill_version VARCHAR(32);
ALTER TABLE pipeline_stage_run ADD COLUMN IF NOT EXISTS input_hash VARCHAR(64);
ALTER TABLE pipeline_stage_run ADD COLUMN IF NOT EXISTS output_hash VARCHAR(64);
ALTER TABLE pipeline_stage_run ADD COLUMN IF NOT EXISTS evidence_ids_json JSONB NOT NULL DEFAULT '[]'::jsonb;
CREATE INDEX IF NOT EXISTS ix_pipeline_stage_run_agent_id ON pipeline_stage_run (agent_id);
CREATE INDEX IF NOT EXISTS ix_pipeline_stage_run_skill_id ON pipeline_stage_run (skill_id);

ALTER TABLE document_chunk_version ADD COLUMN IF NOT EXISTS section_title VARCHAR(512);
ALTER TABLE document_chunk_version ADD COLUMN IF NOT EXISTS section_path_json JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE document_chunk_version ADD COLUMN IF NOT EXISTS section_status VARCHAR(24) NOT NULL DEFAULT 'UNRESOLVED';
ALTER TABLE document_chunk_version ADD COLUMN IF NOT EXISTS embedding_status VARCHAR(24) NOT NULL DEFAULT 'MISSING';
ALTER TABLE document_chunk_version ADD COLUMN IF NOT EXISTS embedding_kind VARCHAR(24) NOT NULL DEFAULT 'NONE';
ALTER TABLE document_chunk_version ADD COLUMN IF NOT EXISTS source_object_id VARCHAR(36);
ALTER TABLE document_chunk_version ADD COLUMN IF NOT EXISTS lineage_batch_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS ix_document_chunk_version_source_object_id ON document_chunk_version (source_object_id);
CREATE INDEX IF NOT EXISTS ix_document_chunk_version_lineage_batch_id ON document_chunk_version (lineage_batch_id);
CREATE INDEX IF NOT EXISTS ix_lake_lineage_event_downstream_lookup
    ON lake_lineage_event (downstream_type, downstream_id, id);
UPDATE document_chunk_version SET embedding_kind = 'HASH', embedding_status = 'HASH_ONLY'
WHERE UPPER(COALESCE(embedding_model, '')) LIKE 'HASH%'
  AND (embedding_kind IS NULL OR embedding_kind IN ('NONE', 'SEMANTIC'));
-- A model name alone does not prove that a semantic vector was generated.
-- Keep non-hash historical rows pending until a provider writes a verifiable
-- vector payload and explicitly promotes the status.
UPDATE document_chunk_version SET embedding_kind = 'SEMANTIC', embedding_status = 'PENDING'
WHERE COALESCE(embedding_model, '') <> '' AND UPPER(embedding_model) NOT LIKE 'HASH%'
  AND (embedding_kind IS NULL OR embedding_kind = 'NONE' OR embedding_status = 'MISSING');

CREATE TABLE IF NOT EXISTS data_quality_rule (
    id SERIAL PRIMARY KEY,
    rule_code VARCHAR(128) NOT NULL,
    rule_name VARCHAR(256) NOT NULL,
    asset_scope VARCHAR(64) NOT NULL,
    target_code VARCHAR(256),
    rule_type VARCHAR(64) NOT NULL,
    severity VARCHAR(16) NOT NULL DEFAULT 'ERROR',
    version VARCHAR(32) NOT NULL DEFAULT '1.0.0',
    definition_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    threshold_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    lifecycle_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_data_quality_rule_version UNIQUE (rule_code, version)
);
CREATE INDEX IF NOT EXISTS ix_data_quality_rule_rule_code ON data_quality_rule (rule_code);
CREATE INDEX IF NOT EXISTS ix_data_quality_rule_target_code ON data_quality_rule (target_code);
CREATE INDEX IF NOT EXISTS ix_data_quality_rule_scope_enabled ON data_quality_rule (asset_scope, enabled);

CREATE TABLE IF NOT EXISTS data_quality_run (
    id VARCHAR(36) PRIMARY KEY,
    run_key VARCHAR(192) NOT NULL UNIQUE,
    target_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(256) NOT NULL,
    target_version VARCHAR(128),
    pipeline_run_id INTEGER REFERENCES pipeline_run(id) ON DELETE SET NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    score DOUBLE PRECISION,
    total_rules INTEGER NOT NULL DEFAULT 0,
    passed_rules INTEGER NOT NULL DEFAULT 0,
    failed_rules INTEGER NOT NULL DEFAULT 0,
    warning_rules INTEGER NOT NULL DEFAULT 0,
    summary_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS ix_data_quality_run_run_key ON data_quality_run (run_key);
CREATE INDEX IF NOT EXISTS ix_data_quality_run_pipeline_run_id ON data_quality_run (pipeline_run_id);
CREATE INDEX IF NOT EXISTS ix_data_quality_run_target_time ON data_quality_run (target_type, target_id, created_at);
CREATE INDEX IF NOT EXISTS ix_data_quality_run_status_time ON data_quality_run (status, created_at);

CREATE TABLE IF NOT EXISTS data_quality_issue (
    id VARCHAR(36) PRIMARY KEY,
    quality_run_id VARCHAR(36) NOT NULL REFERENCES data_quality_run(id) ON DELETE CASCADE,
    rule_id INTEGER REFERENCES data_quality_rule(id) ON DELETE SET NULL,
    rule_code VARCHAR(128) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(256) NOT NULL,
    issue_code VARCHAR(128) NOT NULL,
    severity VARCHAR(16) NOT NULL DEFAULT 'ERROR',
    status VARCHAR(24) NOT NULL DEFAULT 'OPEN',
    message TEXT NOT NULL,
    observed_value_json JSONB,
    expected_value_json JSONB,
    evidence_ids_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    details_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    first_detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,
    resolution TEXT,
    CONSTRAINT uq_data_quality_issue_run_rule_target UNIQUE (quality_run_id, rule_code, target_id)
);
CREATE INDEX IF NOT EXISTS ix_data_quality_issue_quality_run_id ON data_quality_issue (quality_run_id);
CREATE INDEX IF NOT EXISTS ix_data_quality_issue_rule_id ON data_quality_issue (rule_id);
CREATE INDEX IF NOT EXISTS ix_data_quality_issue_status_severity ON data_quality_issue (status, severity);

CREATE TABLE IF NOT EXISTS agent_execution_run (
    id VARCHAR(36) PRIMARY KEY,
    run_key VARCHAR(192) NOT NULL UNIQUE,
    parent_run_id VARCHAR(36) REFERENCES agent_execution_run(id) ON DELETE SET NULL,
    pipeline_run_id INTEGER REFERENCES pipeline_run(id) ON DELETE SET NULL,
    pipeline_stage_run_id INTEGER REFERENCES pipeline_stage_run(id) ON DELETE SET NULL,
    agent_id INTEGER NOT NULL REFERENCES agent_definition(id) ON DELETE RESTRICT,
    agent_code VARCHAR(64) NOT NULL,
    agent_version VARCHAR(32) NOT NULL,
    model_instance_code VARCHAR(64),
    model_version VARCHAR(128),
    model_call_log_id INTEGER REFERENCES model_call_log(id) ON DELETE SET NULL,
    trigger_type VARCHAR(32) NOT NULL DEFAULT 'API',
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    correlation_id VARCHAR(128),
    idempotency_key VARCHAR(192) UNIQUE,
    input_hash VARCHAR(64),
    output_hash VARCHAR(64),
    input_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    output_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    accessible_assets_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    knowledge_versions_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    skill_versions_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    evidence_ids_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    audit_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    error_code VARCHAR(64),
    error_message TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS ix_agent_execution_run_run_key ON agent_execution_run (run_key);
CREATE INDEX IF NOT EXISTS ix_agent_execution_run_parent_run_id ON agent_execution_run (parent_run_id);
CREATE INDEX IF NOT EXISTS ix_agent_execution_run_pipeline_run_id ON agent_execution_run (pipeline_run_id);
CREATE INDEX IF NOT EXISTS ix_agent_execution_run_pipeline_stage_run_id ON agent_execution_run (pipeline_stage_run_id);
CREATE INDEX IF NOT EXISTS ix_agent_execution_run_agent_id ON agent_execution_run (agent_id);
CREATE INDEX IF NOT EXISTS ix_agent_execution_run_model_call_log_id ON agent_execution_run (model_call_log_id);
CREATE INDEX IF NOT EXISTS ix_agent_execution_run_correlation_id ON agent_execution_run (correlation_id);
CREATE INDEX IF NOT EXISTS ix_agent_execution_run_idempotency_key ON agent_execution_run (idempotency_key);
CREATE INDEX IF NOT EXISTS ix_agent_execution_agent_time ON agent_execution_run (agent_id, started_at);
CREATE INDEX IF NOT EXISTS ix_agent_execution_status_time ON agent_execution_run (status, started_at);

CREATE TABLE IF NOT EXISTS skill_execution_run (
    id VARCHAR(36) PRIMARY KEY,
    agent_execution_run_id VARCHAR(36) REFERENCES agent_execution_run(id) ON DELETE SET NULL,
    pipeline_stage_run_id INTEGER REFERENCES pipeline_stage_run(id) ON DELETE SET NULL,
    skill_id INTEGER NOT NULL REFERENCES model_skill(id) ON DELETE RESTRICT,
    skill_code VARCHAR(64) NOT NULL,
    skill_version VARCHAR(32) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    attempt INTEGER NOT NULL DEFAULT 1,
    max_attempts INTEGER NOT NULL DEFAULT 1,
    idempotency_key VARCHAR(192) UNIQUE,
    input_hash VARCHAR(64),
    output_hash VARCHAR(64),
    input_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    output_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    side_effect_level VARCHAR(24) NOT NULL DEFAULT 'READ_ONLY',
    write_scope_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    evidence_ids_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    model_call_log_id INTEGER REFERENCES model_call_log(id) ON DELETE SET NULL,
    error_code VARCHAR(64),
    error_message TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS ix_skill_execution_run_agent_execution_run_id ON skill_execution_run (agent_execution_run_id);
CREATE INDEX IF NOT EXISTS ix_skill_execution_run_pipeline_stage_run_id ON skill_execution_run (pipeline_stage_run_id);
CREATE INDEX IF NOT EXISTS ix_skill_execution_run_skill_id ON skill_execution_run (skill_id);
CREATE INDEX IF NOT EXISTS ix_skill_execution_run_idempotency_key ON skill_execution_run (idempotency_key);
CREATE INDEX IF NOT EXISTS ix_skill_execution_run_model_call_log_id ON skill_execution_run (model_call_log_id);
CREATE INDEX IF NOT EXISTS ix_skill_execution_skill_time ON skill_execution_run (skill_id, started_at);
CREATE INDEX IF NOT EXISTS ix_skill_execution_status_time ON skill_execution_run (status, started_at);

COMMIT;
