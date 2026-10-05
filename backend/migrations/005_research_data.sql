CREATE TABLE IF NOT EXISTS stock_broker_research_report (
    id INTEGER PRIMARY KEY,
    market VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    source_code VARCHAR(32) NOT NULL,
    external_id VARCHAR(128) NOT NULL,
    title VARCHAR(1024) NOT NULL,
    report_date VARCHAR(32) NOT NULL DEFAULT '',
    institution VARCHAR(256),
    analysts_json JSON NOT NULL DEFAULT '[]',
    rating VARCHAR(64),
    summary TEXT,
    content_text TEXT,
    content_hash VARCHAR(64),
    content_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    fetch_error TEXT,
    source_url VARCHAR(2048),
    detail_url VARCHAR(2048),
    pdf_url VARCHAR(2048),
    source_updated_at VARCHAR(64),
    fetched_at DATETIME NOT NULL,
    raw_payload JSON NOT NULL DEFAULT '{}',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uq_stock_broker_report_source_external UNIQUE (market, symbol, source_code, external_id)
);
CREATE INDEX IF NOT EXISTS ix_stock_broker_report_symbol_date
    ON stock_broker_research_report (market, symbol, report_date);

CREATE TABLE IF NOT EXISTS stock_earnings_consensus (
    id INTEGER PRIMARY KEY,
    market VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    source_code VARCHAR(32) NOT NULL,
    forecast_year VARCHAR(8) NOT NULL,
    metric_code VARCHAR(64) NOT NULL,
    metric_name VARCHAR(128) NOT NULL,
    unit VARCHAR(32),
    prediction_count INTEGER,
    minimum_value VARCHAR(64),
    mean_value VARCHAR(64),
    maximum_value VARCHAR(64),
    industry_average VARCHAR(64),
    source_url VARCHAR(2048),
    source_updated_at VARCHAR(64),
    fetched_at DATETIME NOT NULL,
    raw_payload JSON NOT NULL DEFAULT '{}',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uq_stock_earnings_consensus_metric UNIQUE (market, symbol, source_code, forecast_year, metric_code)
);
CREATE INDEX IF NOT EXISTS ix_stock_earnings_consensus_symbol_year
    ON stock_earnings_consensus (market, symbol, forecast_year);

CREATE TABLE IF NOT EXISTS stock_institution_forecast (
    id INTEGER PRIMARY KEY,
    market VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    source_code VARCHAR(32) NOT NULL,
    external_key VARCHAR(160) NOT NULL,
    institution VARCHAR(256) NOT NULL DEFAULT '',
    analysts_json JSON NOT NULL DEFAULT '[]',
    report_date VARCHAR(32) NOT NULL DEFAULT '',
    rating VARCHAR(64),
    forecast_json JSON NOT NULL DEFAULT '{}',
    report_external_id VARCHAR(128),
    source_url VARCHAR(2048),
    detail_url VARCHAR(2048),
    source_updated_at VARCHAR(64),
    fetched_at DATETIME NOT NULL,
    raw_payload JSON NOT NULL DEFAULT '{}',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uq_stock_institution_forecast_external UNIQUE (market, symbol, source_code, external_key)
);
CREATE INDEX IF NOT EXISTS ix_stock_institution_forecast_symbol_date
    ON stock_institution_forecast (market, symbol, report_date);

CREATE TABLE IF NOT EXISTS stock_investor_qa (
    id INTEGER PRIMARY KEY,
    market VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    source_code VARCHAR(32) NOT NULL,
    external_id VARCHAR(160) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT,
    questioner VARCHAR(256),
    answerer VARCHAR(256),
    asked_time VARCHAR(64),
    answered_time VARCHAR(64),
    updated_time VARCHAR(64),
    source_url VARCHAR(2048),
    source_updated_at VARCHAR(64),
    fetched_at DATETIME NOT NULL,
    raw_payload JSON NOT NULL DEFAULT '{}',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uq_stock_investor_qa_external UNIQUE (market, symbol, source_code, external_id)
);
CREATE INDEX IF NOT EXISTS ix_stock_investor_qa_symbol_time
    ON stock_investor_qa (market, symbol, updated_time);
