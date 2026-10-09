-- Roll back only the additive detail table and indexes. PostgreSQL column drops
-- are explicit because this migration never rewrites business fact tables.
DROP TABLE IF EXISTS stock_governance_detail;
DROP INDEX IF EXISTS ix_skill_execution_run_governance_batch_id;
DROP INDEX IF EXISTS ix_skill_execution_run_governance_mode;
DROP INDEX IF EXISTS ix_agent_execution_run_governance_batch_id;
DROP INDEX IF EXISTS ix_agent_execution_run_governance_mode;
DROP INDEX IF EXISTS ix_data_fetch_log_skill_execution_run_id;
DROP INDEX IF EXISTS ix_data_fetch_log_agent_execution_run_id;
DROP INDEX IF EXISTS ix_data_fetch_log_governance_batch_id;
DROP INDEX IF EXISTS ix_data_fetch_log_governance_mode;
DROP INDEX IF EXISTS ix_pipeline_run_governance_batch_id;
DROP INDEX IF EXISTS ix_pipeline_run_governance_mode;
ALTER TABLE skill_execution_run DROP COLUMN IF EXISTS governance_batch_id;
ALTER TABLE skill_execution_run DROP COLUMN IF EXISTS governance_mode;
ALTER TABLE agent_execution_run DROP COLUMN IF EXISTS governance_batch_id;
ALTER TABLE agent_execution_run DROP COLUMN IF EXISTS governance_mode;
ALTER TABLE data_fetch_log DROP COLUMN IF EXISTS skill_execution_run_id;
ALTER TABLE data_fetch_log DROP COLUMN IF EXISTS agent_execution_run_id;
ALTER TABLE data_fetch_log DROP COLUMN IF EXISTS governance_batch_id;
ALTER TABLE data_fetch_log DROP COLUMN IF EXISTS governance_mode;
ALTER TABLE pipeline_run DROP COLUMN IF EXISTS governance_batch_id;
ALTER TABLE pipeline_run DROP COLUMN IF EXISTS governance_mode;
