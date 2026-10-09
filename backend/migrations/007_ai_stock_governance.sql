-- Additive governance provenance for system-vs-Agent/Skill stock data runs.
ALTER TABLE pipeline_run ADD COLUMN IF NOT EXISTS governance_mode VARCHAR(40) NOT NULL DEFAULT 'SYSTEM_GOVERNANCE';
ALTER TABLE pipeline_run ADD COLUMN IF NOT EXISTS governance_batch_id VARCHAR(128);

ALTER TABLE data_fetch_log ADD COLUMN IF NOT EXISTS governance_mode VARCHAR(40) NOT NULL DEFAULT 'SYSTEM_GOVERNANCE';
ALTER TABLE data_fetch_log ADD COLUMN IF NOT EXISTS governance_batch_id VARCHAR(128);
ALTER TABLE data_fetch_log ADD COLUMN IF NOT EXISTS agent_execution_run_id VARCHAR(36) REFERENCES agent_execution_run(id) ON DELETE SET NULL;
ALTER TABLE data_fetch_log ADD COLUMN IF NOT EXISTS skill_execution_run_id VARCHAR(36) REFERENCES skill_execution_run(id) ON DELETE SET NULL;

ALTER TABLE agent_execution_run ADD COLUMN IF NOT EXISTS governance_mode VARCHAR(40) NOT NULL DEFAULT 'AI_AGENT_SKILL_GOVERNANCE';
ALTER TABLE agent_execution_run ADD COLUMN IF NOT EXISTS governance_batch_id VARCHAR(128);
ALTER TABLE skill_execution_run ADD COLUMN IF NOT EXISTS governance_mode VARCHAR(40) NOT NULL DEFAULT 'AI_AGENT_SKILL_GOVERNANCE';
ALTER TABLE skill_execution_run ADD COLUMN IF NOT EXISTS governance_batch_id VARCHAR(128);

CREATE TABLE IF NOT EXISTS stock_governance_detail (
    id VARCHAR(36) PRIMARY KEY,
    governance_batch_id VARCHAR(128) NOT NULL,
    governance_mode VARCHAR(40) NOT NULL DEFAULT 'SYSTEM_GOVERNANCE',
    board_code VARCHAR(32) NOT NULL,
    market VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    stock_name VARCHAR(128),
    pipeline_run_id INTEGER REFERENCES pipeline_run(id) ON DELETE SET NULL,
    agent_execution_run_id VARCHAR(36) REFERENCES agent_execution_run(id) ON DELETE SET NULL,
    baseline_batch_id VARCHAR(128),
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    stage_status_json JSON NOT NULL DEFAULT '{}',
    source_ids_json JSON NOT NULL DEFAULT '[]',
    evidence_ids_json JSON NOT NULL DEFAULT '[]',
    quality_summary_json JSON NOT NULL DEFAULT '{}',
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_stock_governance_detail_batch_stock
        UNIQUE (governance_batch_id, market, symbol)
);

CREATE INDEX IF NOT EXISTS ix_pipeline_run_governance_mode ON pipeline_run (governance_mode);
CREATE INDEX IF NOT EXISTS ix_pipeline_run_governance_batch_id ON pipeline_run (governance_batch_id);
CREATE INDEX IF NOT EXISTS ix_data_fetch_log_governance_mode ON data_fetch_log (governance_mode);
CREATE INDEX IF NOT EXISTS ix_data_fetch_log_governance_batch_id ON data_fetch_log (governance_batch_id);
CREATE INDEX IF NOT EXISTS ix_data_fetch_log_agent_execution_run_id ON data_fetch_log (agent_execution_run_id);
CREATE INDEX IF NOT EXISTS ix_data_fetch_log_skill_execution_run_id ON data_fetch_log (skill_execution_run_id);
CREATE INDEX IF NOT EXISTS ix_agent_execution_run_governance_mode ON agent_execution_run (governance_mode);
CREATE INDEX IF NOT EXISTS ix_agent_execution_run_governance_batch_id ON agent_execution_run (governance_batch_id);
CREATE INDEX IF NOT EXISTS ix_skill_execution_run_governance_mode ON skill_execution_run (governance_mode);
CREATE INDEX IF NOT EXISTS ix_skill_execution_run_governance_batch_id ON skill_execution_run (governance_batch_id);
CREATE INDEX IF NOT EXISTS ix_stock_governance_detail_governance_batch_id ON stock_governance_detail (governance_batch_id);
CREATE INDEX IF NOT EXISTS ix_stock_governance_detail_governance_mode ON stock_governance_detail (governance_mode);
CREATE INDEX IF NOT EXISTS ix_stock_governance_detail_board_code ON stock_governance_detail (board_code);
CREATE INDEX IF NOT EXISTS ix_stock_governance_detail_market ON stock_governance_detail (market);
CREATE INDEX IF NOT EXISTS ix_stock_governance_detail_symbol ON stock_governance_detail (symbol);
CREATE INDEX IF NOT EXISTS ix_stock_governance_detail_pipeline_run_id ON stock_governance_detail (pipeline_run_id);
CREATE INDEX IF NOT EXISTS ix_stock_governance_detail_agent_execution_run_id ON stock_governance_detail (agent_execution_run_id);
CREATE INDEX IF NOT EXISTS ix_stock_governance_detail_baseline_batch_id ON stock_governance_detail (baseline_batch_id);
CREATE INDEX IF NOT EXISTS ix_stock_governance_detail_status ON stock_governance_detail (status);
CREATE INDEX IF NOT EXISTS ix_stock_governance_detail_mode_board ON stock_governance_detail (governance_mode, board_code);
CREATE INDEX IF NOT EXISTS ix_stock_governance_detail_status_time ON stock_governance_detail (status, completed_at);

