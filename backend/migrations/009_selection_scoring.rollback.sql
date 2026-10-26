-- Roll back only the additive scoring subsystem. Existing selection data is preserved.
PRAGMA foreign_keys = OFF;
DROP TABLE IF EXISTS selection_evaluation_metric;
DROP TABLE IF EXISTS selection_dimension_result;
DROP TABLE IF EXISTS selection_strategy_profile_item;
DROP TABLE IF EXISTS selection_strategy_profile;
DROP TABLE IF EXISTS selection_dimension_definition;
PRAGMA foreign_keys = ON;
