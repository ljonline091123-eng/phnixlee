-- Model hub schema: provider management, model instances, task routing, and audit logs.

CREATE TABLE IF NOT EXISTS model_provider (
    id SERIAL PRIMARY KEY,
    provider_code VARCHAR(64) NOT NULL UNIQUE,
    provider_name VARCHAR(128) NOT NULL,
    provider_type VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    config_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS model_instance (
    id SERIAL PRIMARY KEY,
    provider_id INTEGER NOT NULL REFERENCES model_provider(id) ON DELETE CASCADE,
    instance_code VARCHAR(64) NOT NULL UNIQUE,
    model_code VARCHAR(128) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    purpose VARCHAR(64) NOT NULL DEFAULT 'GENERAL',
    api_key TEXT,
    api_base_url VARCHAR(512),
    api_path VARCHAR(256),
    max_tokens INTEGER NOT NULL DEFAULT 2048,
    temperature DOUBLE PRECISION NOT NULL DEFAULT 0.2,
    top_p DOUBLE PRECISION NOT NULL DEFAULT 0.95,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    fallback_instance_code VARCHAR(64),
    config_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_model_instance_provider_code UNIQUE (provider_id, instance_code)
);

CREATE TABLE IF NOT EXISTS model_route_rule (
    id SERIAL PRIMARY KEY,
    task_type VARCHAR(64) NOT NULL UNIQUE,
    preferred_instance_code VARCHAR(64) NOT NULL,
    fallback_chain_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    route_policy VARCHAR(32) NOT NULL DEFAULT 'PREFERRED_THEN_FALLBACK',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS model_call_log (
    id SERIAL PRIMARY KEY,
    task_type VARCHAR(64) NOT NULL,
    provider_code VARCHAR(64),
    instance_code VARCHAR(64),
    model_code VARCHAR(128),
    status VARCHAR(16) NOT NULL DEFAULT 'RUNNING',
    request_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    response_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    response_text TEXT,
    error_message TEXT,
    latency_ms INTEGER,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS ix_model_call_log_task_started
    ON model_call_log (task_type, started_at);

