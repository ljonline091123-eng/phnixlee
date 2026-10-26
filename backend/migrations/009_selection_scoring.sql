-- Additive migration for versioned, evidence-aware selection scoring.
-- Existing selection runs, candidates, evidence and decisions are untouched.
PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS selection_dimension_definition (
  id INTEGER PRIMARY KEY,
  dimension_code VARCHAR(64) NOT NULL UNIQUE,
  dimension_name VARCHAR(128) NOT NULL,
  description TEXT NOT NULL,
  score_interpretation TEXT NOT NULL,
  default_method VARCHAR(32) NOT NULL DEFAULT 'HYBRID',
  sort_order INTEGER NOT NULL DEFAULT 0,
  enabled BOOLEAN NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_selection_dimension_definition_dimension_code ON selection_dimension_definition(dimension_code);

CREATE TABLE IF NOT EXISTS selection_strategy_profile (
  id INTEGER PRIMARY KEY,
  profile_code VARCHAR(64) NOT NULL,
  profile_name VARCHAR(128) NOT NULL,
  version VARCHAR(32) NOT NULL,
  description TEXT NOT NULL DEFAULT '',
  horizon_code VARCHAR(32) NOT NULL DEFAULT 'T3_T5_T7',
  market_scope_json JSON NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  enabled BOOLEAN NOT NULL DEFAULT 0,
  published_at DATETIME,
  parent_profile_id INTEGER REFERENCES selection_strategy_profile(id) ON DELETE SET NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT uq_selection_strategy_profile_version UNIQUE(profile_code, version)
);
CREATE INDEX IF NOT EXISTS ix_selection_strategy_profile_status ON selection_strategy_profile(status, enabled);

CREATE TABLE IF NOT EXISTS selection_strategy_profile_item (
  id INTEGER PRIMARY KEY,
  profile_id INTEGER NOT NULL REFERENCES selection_strategy_profile(id) ON DELETE CASCADE,
  dimension_code VARCHAR(64) NOT NULL REFERENCES selection_dimension_definition(dimension_code) ON DELETE RESTRICT,
  weight FLOAT NOT NULL,
  minimum_coverage FLOAT NOT NULL DEFAULT 0.4,
  minimum_confidence FLOAT NOT NULL DEFAULT 0.4,
  skill_code VARCHAR(128) NOT NULL,
  skill_version VARCHAR(32) NOT NULL DEFAULT '1.0.0',
  CONSTRAINT uq_selection_profile_dimension UNIQUE(profile_id, dimension_code)
);
CREATE INDEX IF NOT EXISTS ix_selection_strategy_profile_item_profile_id ON selection_strategy_profile_item(profile_id);

CREATE TABLE IF NOT EXISTS selection_dimension_result (
  id INTEGER PRIMARY KEY,
  candidate_id INTEGER NOT NULL REFERENCES selection_candidate(id) ON DELETE CASCADE,
  profile_id INTEGER NOT NULL REFERENCES selection_strategy_profile(id) ON DELETE RESTRICT,
  dimension_code VARCHAR(64) NOT NULL,
  status VARCHAR(24) NOT NULL,
  direction VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
  score FLOAT,
  coverage FLOAT NOT NULL DEFAULT 0,
  evidence_confidence FLOAT NOT NULL DEFAULT 0,
  weight FLOAT NOT NULL,
  weighted_contribution FLOAT,
  method VARCHAR(32) NOT NULL,
  summary TEXT NOT NULL,
  metrics_json JSON NOT NULL,
  evidence_json JSON NOT NULL,
  warnings_json JSON NOT NULL,
  as_of DATETIME NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT uq_selection_candidate_profile_dimension UNIQUE(candidate_id, profile_id, dimension_code)
);
CREATE INDEX IF NOT EXISTS ix_selection_dimension_result_candidate_id ON selection_dimension_result(candidate_id);
CREATE INDEX IF NOT EXISTS ix_selection_dimension_result_code ON selection_dimension_result(dimension_code, status);

CREATE TABLE IF NOT EXISTS selection_evaluation_metric (
  id INTEGER PRIMARY KEY,
  candidate_id INTEGER NOT NULL REFERENCES selection_candidate(id) ON DELETE CASCADE,
  profile_id INTEGER NOT NULL REFERENCES selection_strategy_profile(id) ON DELETE RESTRICT,
  horizon_sessions INTEGER NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'PENDING_DATA',
  return_pct FLOAT,
  benchmark_return_pct FLOAT,
  excess_return_pct FLOAT,
  max_drawdown_pct FLOAT,
  direction_hit BOOLEAN,
  observed_date VARCHAR(16),
  details_json JSON NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT uq_selection_candidate_horizon_metric UNIQUE(candidate_id, horizon_sessions)
);
CREATE INDEX IF NOT EXISTS ix_selection_evaluation_metric_candidate_id ON selection_evaluation_metric(candidate_id);
CREATE INDEX IF NOT EXISTS ix_selection_evaluation_profile_horizon ON selection_evaluation_metric(profile_id, horizon_sessions);
