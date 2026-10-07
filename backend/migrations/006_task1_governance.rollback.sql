-- Explicit rollback for migration 006. Do not execute after production data has
-- been written without first restoring/validating the selected database backup.

BEGIN;

DROP TABLE IF EXISTS skill_execution_run;
DROP TABLE IF EXISTS agent_execution_run;
DROP TABLE IF EXISTS data_quality_issue;
DROP TABLE IF EXISTS data_quality_run;
DROP TABLE IF EXISTS data_quality_rule;

ALTER TABLE document_chunk_version DROP COLUMN IF EXISTS lineage_batch_id;
ALTER TABLE document_chunk_version DROP COLUMN IF EXISTS source_object_id;
ALTER TABLE document_chunk_version DROP COLUMN IF EXISTS embedding_kind;
ALTER TABLE document_chunk_version DROP COLUMN IF EXISTS embedding_status;
ALTER TABLE document_chunk_version DROP COLUMN IF EXISTS section_status;
ALTER TABLE document_chunk_version DROP COLUMN IF EXISTS section_path_json;
ALTER TABLE document_chunk_version DROP COLUMN IF EXISTS section_title;

ALTER TABLE pipeline_stage_run DROP COLUMN IF EXISTS evidence_ids_json;
ALTER TABLE pipeline_stage_run DROP COLUMN IF EXISTS output_hash;
ALTER TABLE pipeline_stage_run DROP COLUMN IF EXISTS input_hash;
ALTER TABLE pipeline_stage_run DROP COLUMN IF EXISTS skill_version;
ALTER TABLE pipeline_stage_run DROP COLUMN IF EXISTS skill_code;
ALTER TABLE pipeline_stage_run DROP COLUMN IF EXISTS skill_id;
ALTER TABLE pipeline_stage_run DROP COLUMN IF EXISTS agent_version;
ALTER TABLE pipeline_stage_run DROP COLUMN IF EXISTS agent_id;

ALTER TABLE agent_data_asset DROP COLUMN IF EXISTS metadata_json;
ALTER TABLE agent_data_asset DROP COLUMN IF EXISTS canonical_identity;
ALTER TABLE agent_data_asset DROP COLUMN IF EXISTS asset_type;

ALTER TABLE agent_definition DROP COLUMN IF EXISTS knowledge_version_policy_json;
ALTER TABLE agent_definition DROP COLUMN IF EXISTS policy_json;
ALTER TABLE agent_definition DROP COLUMN IF EXISTS lifecycle_status;

ALTER TABLE model_skill DROP COLUMN IF EXISTS error_policy_json;
ALTER TABLE model_skill DROP COLUMN IF EXISTS retry_policy_json;
ALTER TABLE model_skill DROP COLUMN IF EXISTS idempotency_policy;
ALTER TABLE model_skill DROP COLUMN IF EXISTS side_effect_level;
ALTER TABLE model_skill DROP COLUMN IF EXISTS permission_policy_json;
ALTER TABLE model_skill DROP COLUMN IF EXISTS output_contract_json;
ALTER TABLE model_skill DROP COLUMN IF EXISTS input_contract_json;
ALTER TABLE model_skill DROP COLUMN IF EXISTS lifecycle_status;

COMMIT;
