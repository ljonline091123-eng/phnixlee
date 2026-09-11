-- PostgreSQL schema for Phase 1 market-data management.
-- The API also creates these tables automatically on startup.

CREATE TABLE IF NOT EXISTS data_source (
    id SERIAL PRIMARY KEY,
    source_code VARCHAR(64) NOT NULL UNIQUE,
    source_name VARCHAR(128) NOT NULL,
    source_type VARCHAR(32) NOT NULL DEFAULT 'MARKET_DATA',
    adapter_type VARCHAR(64) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 100,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    config_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS data_interface (
    id SERIAL PRIMARY KEY,
    source_id INTEGER NOT NULL REFERENCES data_source(id) ON DELETE CASCADE,
    interface_code VARCHAR(64) NOT NULL,
    interface_name VARCHAR(128) NOT NULL,
    data_category VARCHAR(32) NOT NULL,
    request_mode VARCHAR(32) NOT NULL DEFAULT 'SYNC',
    adapter_method VARCHAR(128) NOT NULL,
    supported_markets JSONB NOT NULL DEFAULT '[]'::jsonb,
    input_schema JSONB NOT NULL DEFAULT '{}'::jsonb,
    output_schema JSONB NOT NULL DEFAULT '{}'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_data_interface_source_code UNIQUE (source_id, interface_code)
);

CREATE TABLE IF NOT EXISTS data_sync_log (
    id SERIAL PRIMARY KEY,
    source_id INTEGER NOT NULL REFERENCES data_source(id) ON DELETE RESTRICT,
    interface_code VARCHAR(64) NOT NULL,
    market VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'RUNNING',
    total_count INTEGER NOT NULL DEFAULT 0,
    inserted_count INTEGER NOT NULL DEFAULT 0,
    updated_count INTEGER NOT NULL DEFAULT 0,
    failed_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    detail_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS ix_data_sync_log_source_market_started
    ON data_sync_log (source_id, market, started_at);

CREATE TABLE IF NOT EXISTS stock_symbol (
    id SERIAL PRIMARY KEY,
    market VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    exchange VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    asset_type VARCHAR(32) NOT NULL DEFAULT 'STOCK',
    status VARCHAR(32) NOT NULL DEFAULT 'LISTED',
    list_date VARCHAR(16),
    source_id INTEGER NOT NULL REFERENCES data_source(id) ON DELETE RESTRICT,
    ext_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    raw_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    last_synced_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_stock_symbol_market_symbol UNIQUE (market, symbol)
);

CREATE INDEX IF NOT EXISTS ix_stock_symbol_market_status
    ON stock_symbol (market, status);
CREATE INDEX IF NOT EXISTS ix_stock_symbol_name
    ON stock_symbol (name);

CREATE TABLE IF NOT EXISTS stock_kline (
    id SERIAL PRIMARY KEY,
    market VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    period VARCHAR(16) NOT NULL DEFAULT 'daily',
    adjust VARCHAR(16) NOT NULL DEFAULT '',
    trade_date VARCHAR(16) NOT NULL,
    open_price DOUBLE PRECISION,
    high_price DOUBLE PRECISION,
    low_price DOUBLE PRECISION,
    close_price DOUBLE PRECISION,
    volume DOUBLE PRECISION,
    amount DOUBLE PRECISION,
    turnover_rate DOUBLE PRECISION,
    source_id INTEGER NOT NULL REFERENCES data_source(id) ON DELETE RESTRICT,
    raw_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    fetched_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_stock_kline_market_symbol_period_adjust_date
        UNIQUE (market, symbol, period, adjust, trade_date)
);

CREATE INDEX IF NOT EXISTS ix_stock_kline_market_symbol_date
    ON stock_kline (market, symbol, trade_date);

CREATE TABLE IF NOT EXISTS stock_financial_report (
    id SERIAL PRIMARY KEY,
    market VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    indicator VARCHAR(64) NOT NULL,
    report_period VARCHAR(32) NOT NULL,
    currency VARCHAR(16),
    data_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    url VARCHAR(2048),
    source_id INTEGER NOT NULL REFERENCES data_source(id) ON DELETE RESTRICT,
    fetched_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_stock_financial_market_symbol_indicator_period_source
        UNIQUE (market, symbol, indicator, report_period, source_id)
);

CREATE INDEX IF NOT EXISTS ix_stock_financial_market_symbol_period
    ON stock_financial_report (market, symbol, report_period);

CREATE TABLE IF NOT EXISTS stock_notice (
    id SERIAL PRIMARY KEY,
    market VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    notice_date VARCHAR(32) NOT NULL,
    title VARCHAR(512) NOT NULL,
    notice_type VARCHAR(128),
    url VARCHAR(2048),
    content_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    source_id INTEGER NOT NULL REFERENCES data_source(id) ON DELETE RESTRICT,
    fetched_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_stock_notice_market_symbol_date_title_source
        UNIQUE (market, symbol, notice_date, title, source_id)
);

CREATE INDEX IF NOT EXISTS ix_stock_notice_market_symbol_date
    ON stock_notice (market, symbol, notice_date);

CREATE TABLE IF NOT EXISTS stock_realtime_quote (
    id SERIAL PRIMARY KEY,
    market VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    quote_time VARCHAR(32),
    current_price DOUBLE PRECISION,
    previous_close_price DOUBLE PRECISION,
    open_price DOUBLE PRECISION,
    high_price DOUBLE PRECISION,
    low_price DOUBLE PRECISION,
    volume DOUBLE PRECISION,
    amount DOUBLE PRECISION,
    change_amount DOUBLE PRECISION,
    change_pct DOUBLE PRECISION,
    turnover_rate DOUBLE PRECISION,
    source_id INTEGER NOT NULL REFERENCES data_source(id) ON DELETE RESTRICT,
    raw_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    fetched_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_stock_realtime_quote_market_symbol UNIQUE (market, symbol)
);

CREATE INDEX IF NOT EXISTS ix_stock_realtime_quote_market_updated
    ON stock_realtime_quote (market, updated_at);

CREATE TABLE IF NOT EXISTS stock_news (
    id SERIAL PRIMARY KEY,
    market VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    news_time VARCHAR(32) NOT NULL,
    title VARCHAR(512) NOT NULL,
    content TEXT,
    source_name VARCHAR(128),
    url VARCHAR(2048),
    content_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    source_id INTEGER NOT NULL REFERENCES data_source(id) ON DELETE RESTRICT,
    fetched_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_stock_news_market_symbol_time_title_source
        UNIQUE (market, symbol, news_time, title, source_id)
);

CREATE INDEX IF NOT EXISTS ix_stock_news_market_symbol_time
    ON stock_news (market, symbol, news_time);

CREATE TABLE IF NOT EXISTS data_fetch_log (
    id SERIAL PRIMARY KEY,
    source_id INTEGER NOT NULL REFERENCES data_source(id) ON DELETE RESTRICT,
    interface_code VARCHAR(64) NOT NULL,
    market VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    request_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(16) NOT NULL DEFAULT 'RUNNING',
    total_count INTEGER NOT NULL DEFAULT 0,
    persisted_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS ix_data_fetch_log_symbol_started
    ON data_fetch_log (market, symbol, started_at);
