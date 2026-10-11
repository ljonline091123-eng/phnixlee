-- Append-only human review trail for versioned news/notice semantic candidates.
-- Facts and evidence remain in the existing Foundation tables.
CREATE TABLE IF NOT EXISTS document_semantic_review (
    id VARCHAR(36) PRIMARY KEY,
    source_table VARCHAR(32) NOT NULL,
    source_record_id INTEGER NOT NULL,
    version_index INTEGER NOT NULL,
    version_key VARCHAR(192) NOT NULL,
    review_round INTEGER NOT NULL DEFAULT 1,
    action VARCHAR(32) NOT NULL,
    target_type VARCHAR(32) NOT NULL DEFAULT 'DOCUMENT',
    target_key VARCHAR(128),
    event_key VARCHAR(128),
    item_index INTEGER,
    field_name VARCHAR(64),
    old_value_json JSON,
    new_value_json JSON,
    reason VARCHAR(4000) NOT NULL,
    reviewer VARCHAR(128) NOT NULL,
    resulting_status VARCHAR(32) NOT NULL,
    fact_ids_json JSON NOT NULL DEFAULT '[]',
    corrected_fact_ids_json JSON NOT NULL DEFAULT '[]',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_document_semantic_review_document_time
    ON document_semantic_review (source_table, source_record_id, created_at);
CREATE INDEX IF NOT EXISTS ix_document_semantic_review_version
    ON document_semantic_review (version_key, created_at);
CREATE INDEX IF NOT EXISTS ix_document_semantic_review_source_table
    ON document_semantic_review (source_table);
CREATE INDEX IF NOT EXISTS ix_document_semantic_review_source_record_id
    ON document_semantic_review (source_record_id);
