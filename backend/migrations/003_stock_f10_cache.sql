-- Local F10 cache for on-demand stock detail data.
-- The API also creates this table automatically on startup.

CREATE TABLE IF NOT EXISTS stock_f10_cache (
    id SERIAL PRIMARY KEY,
    market VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    section VARCHAR(64) NOT NULL,
    source_id INTEGER NOT NULL REFERENCES data_source(id) ON DELETE RESTRICT,
    payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    fetched_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_stock_f10_cache_market_symbol_section
        UNIQUE (market, symbol, section)
);

CREATE INDEX IF NOT EXISTS ix_stock_f10_cache_market_symbol
    ON stock_f10_cache (market, symbol);
