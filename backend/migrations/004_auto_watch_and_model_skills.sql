-- Auto watch and model skill management additions.

CREATE TABLE IF NOT EXISTS watchlist_item (
    id SERIAL PRIMARY KEY,
    market VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    note VARCHAR(256),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_watchlist_market_symbol UNIQUE (market, symbol)
);

CREATE TABLE IF NOT EXISTS model_skill (
    id SERIAL PRIMARY KEY,
    skill_code VARCHAR(64) NOT NULL UNIQUE,
    skill_name VARCHAR(128) NOT NULL,
    description TEXT,
    instructions TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    config_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_model_skill_skill_code
    ON model_skill (skill_code);
