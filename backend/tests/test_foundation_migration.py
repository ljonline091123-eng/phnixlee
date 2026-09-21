from __future__ import annotations

from contextlib import redirect_stderr, redirect_stdout
import io
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from sqlalchemy import CheckConstraint, String, create_engine, inspect, text

from scripts.migrate_foundation import (
    HEAD,
    VERSION_TABLE,
    MigrationSafetyError,
    apply_foundation,
    inspect_foundation,
    main,
    managed_table_names,
    preview_sql,
    revision_metadata,
    target_engine,
)


class FoundationMigrationTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary = tempfile.TemporaryDirectory(prefix="foundation-migration-")
        self.database_path = Path(self.temporary.name) / "isolated.sqlite"
        self.url = "sqlite:///" + self.database_path.as_posix()
        self.engine = create_engine(self.url)
        with self.engine.begin() as connection:
            connection.execute(text("CREATE TABLE stock_symbol (id INTEGER NOT NULL PRIMARY KEY, symbol TEXT NOT NULL)"))
            connection.execute(text("INSERT INTO stock_symbol VALUES (1, 'LEGACY')"))
            connection.execute(text("CREATE TABLE legacy_evidence (id INTEGER PRIMARY KEY, body TEXT NOT NULL)"))
            connection.execute(text("INSERT INTO legacy_evidence VALUES (4, 'keep this evidence')"))
        self.metadata = revision_metadata()

    def tearDown(self) -> None:
        self.engine.dispose()
        self.temporary.cleanup()

    def legacy_snapshot(self) -> list[tuple]:
        with self.engine.connect() as connection:
            schema = connection.execute(text(
                "SELECT type, name, sql FROM sqlite_master "
                "WHERE name IN ('stock_symbol', 'legacy_evidence') ORDER BY name"
            )).all()
            data = connection.execute(text("SELECT * FROM stock_symbol")).all()
            evidence = connection.execute(text("SELECT * FROM legacy_evidence")).all()
            return [tuple(row) for row in schema + data + evidence]

    def create_foundation_tables(self, metadata=None, names=None) -> None:
        metadata = self.metadata if metadata is None else metadata
        selected = managed_table_names(metadata) if names is None else set(names)
        metadata.create_all(self.engine, tables=[table for table in metadata.sorted_tables if table.name in selected])

    def test_check_is_read_only_and_upgrade_only_adds_managed_tables(self) -> None:
        legacy_before = self.legacy_snapshot()
        tables_before = set(inspect(self.engine).get_table_names())
        with self.engine.connect() as connection:
            report = inspect_foundation(connection)
        self.assertEqual(report["status"], "PENDING")
        self.assertEqual(set(inspect(self.engine).get_table_names()), tables_before)

        result = apply_foundation(self.engine)
        self.assertEqual(result["status"], "CURRENT")
        self.assertEqual(set(inspect(self.engine).get_table_names()) - tables_before, managed_table_names(self.metadata) | {VERSION_TABLE})
        self.assertEqual(self.legacy_snapshot(), legacy_before)
        with self.engine.connect() as connection:
            self.assertEqual(connection.execute(text(f"SELECT version_num FROM {VERSION_TABLE}")).scalar_one(), HEAD)

    def test_repeated_upgrade_is_idempotent_and_preserves_rows(self) -> None:
        apply_foundation(self.engine)
        with self.engine.begin() as connection:
            connection.execute(text(
                "INSERT INTO foundation_entity (id, name, entity_type, jurisdiction, properties_json, created_at) "
                "VALUES ('company-1', 'Existing Company', 'COMPANY', 'CN', '{}', '2026-09-20 00:00:00')"
            ))
        self.assertEqual(apply_foundation(self.engine)["status"], "CURRENT")
        with self.engine.connect() as connection:
            self.assertEqual(connection.execute(text("SELECT name FROM foundation_entity WHERE id = 'company-1'")).scalar_one(), "Existing Company")

    def test_existing_create_all_schema_is_verified_before_adoption(self) -> None:
        import app.models.foundation  # noqa: F401
        from app.db.base import Base

        self.create_foundation_tables(Base.metadata)
        legacy_before = self.legacy_snapshot()
        with self.engine.connect() as connection:
            report = inspect_foundation(connection)
        self.assertEqual(report["status"], "UNVERSIONED")
        self.assertEqual(set(report["verified_tables_to_adopt"]), managed_table_names(self.metadata))
        self.assertNotIn(VERSION_TABLE, inspect(self.engine).get_table_names())
        self.assertEqual(apply_foundation(self.engine)["status"], "CURRENT")
        self.assertEqual(self.legacy_snapshot(), legacy_before)

    def test_partial_matching_schema_can_be_completed(self) -> None:
        self.create_foundation_tables(names={"foundation_entity"})
        self.assertEqual(apply_foundation(self.engine)["status"], "CURRENT")

    def test_previous_revision_upgrades_without_recreating_its_tables(self) -> None:
        previous = revision_metadata("foundation_0001")
        self.create_foundation_tables(previous)
        with self.engine.begin() as connection:
            connection.execute(text(f"CREATE TABLE {VERSION_TABLE} (version_num VARCHAR(32) NOT NULL PRIMARY KEY)"))
            connection.execute(text(f"INSERT INTO {VERSION_TABLE} VALUES ('foundation_0001')"))
            connection.execute(text(
                "INSERT INTO foundation_entity (id, name, entity_type, jurisdiction, properties_json, created_at) "
                "VALUES ('before-v2', 'Existing Company', 'COMPANY', 'CN', '{}', '2026-09-20 00:00:00')"
            ))
        legacy_before = self.legacy_snapshot()
        with self.engine.connect() as connection:
            report = inspect_foundation(connection)
        self.assertEqual(report["status"], "UPGRADE_REQUIRED")
        self.assertEqual(set(report["tables_to_create"]), {
            "foundation_source_identity", "foundation_security_classification", "foundation_company_mapping_state"})
        self.assertEqual(apply_foundation(self.engine)["status"], "CURRENT")
        with self.engine.connect() as connection:
            self.assertEqual(connection.execute(text("SELECT name FROM foundation_entity WHERE id = 'before-v2'")).scalar_one(), "Existing Company")
        self.assertEqual(self.legacy_snapshot(), legacy_before)

    def test_second_revision_adds_mapping_state_without_recreating_previous_schema(self) -> None:
        previous = revision_metadata("foundation_0002")
        self.create_foundation_tables(previous)
        with self.engine.begin() as connection:
            connection.execute(text(f"CREATE TABLE {VERSION_TABLE} (version_num VARCHAR(32) NOT NULL PRIMARY KEY)"))
            connection.execute(text(f"INSERT INTO {VERSION_TABLE} VALUES ('foundation_0002')"))
            connection.execute(text(
                "INSERT INTO foundation_entity (id, name, entity_type, jurisdiction, properties_json, created_at) "
                "VALUES ('before-v3', 'Keep Existing Company', 'COMPANY', 'CN', '{}', '2026-09-20 00:00:00')"
            ))
            connection.execute(text(
                "INSERT INTO foundation_source_identity (id, entity_id, namespace, external_id, entity_type, jurisdiction, created_at) "
                "VALUES ('source-before-v3', 'before-v3', 'SOURCE', 'ORG-1', 'COMPANY', 'CN', '2026-09-20 00:00:00')"
            ))
            old_schema = connection.execute(text(
                "SELECT name, sql FROM sqlite_master WHERE name LIKE 'foundation_%' "
                "AND name != :version_table ORDER BY name"
            ), {"version_table": VERSION_TABLE}).all()
        legacy_before = self.legacy_snapshot()
        tables_before = set(inspect(self.engine).get_table_names())
        with self.engine.connect() as connection:
            report = inspect_foundation(connection)
        self.assertEqual(report["status"], "UPGRADE_REQUIRED")
        self.assertEqual(report["current_revision"], "foundation_0002")
        self.assertEqual(report["tables_to_create"], ["foundation_company_mapping_state"])
        self.assertEqual(apply_foundation(self.engine)["status"], "CURRENT")
        self.assertEqual(set(inspect(self.engine).get_table_names()) - tables_before, {"foundation_company_mapping_state"})
        with self.engine.connect() as connection:
            self.assertEqual(connection.execute(text("SELECT name FROM foundation_entity WHERE id = 'before-v3'")).scalar_one(), "Keep Existing Company")
            self.assertEqual(connection.execute(text("SELECT entity_id FROM foundation_source_identity WHERE id = 'source-before-v3'")).scalar_one(), "before-v3")
            new_schema = connection.execute(text(
                "SELECT name, sql FROM sqlite_master WHERE name LIKE 'foundation_%' "
                "AND name NOT IN (:version_table, 'foundation_company_mapping_state') ORDER BY name"
            ), {"version_table": VERSION_TABLE}).all()
            self.assertEqual(new_schema, old_schema)
        self.assertEqual(self.legacy_snapshot(), legacy_before)

    def test_mapping_outcomes_created_before_migration_are_adopted_without_data_loss(self) -> None:
        import app.models.company_mapping  # noqa: F401
        from app.db.base import Base

        self.create_foundation_tables(Base.metadata)
        with self.engine.begin() as connection:
            connection.execute(text(
                "INSERT INTO foundation_company_mapping_state "
                "(stock_symbol_id, status, reason, source_name, source_snapshot, attempt_count, details_json, attempted_at) "
                "VALUES (1, 'SOURCE_NOT_FOUND', 'No matching issuer in source', 'PUBLIC_SOURCE', 'snapshot-1', "
                "2, :details, '2026-09-20 00:00:00')"
            ), {"details": '{"retained":true}'})
            before = connection.execute(text("SELECT * FROM foundation_company_mapping_state")).all()
        self.assertEqual(apply_foundation(self.engine)["status"], "CURRENT")
        self.assertEqual(apply_foundation(self.engine)["status"], "CURRENT")
        with self.engine.connect() as connection:
            self.assertEqual(connection.execute(text("SELECT * FROM foundation_company_mapping_state")).all(), before)

    def test_mapping_state_schema_drift_blocks_adoption(self) -> None:
        self.metadata.tables["foundation_company_mapping_state"].c.reason.type = String(128)
        self.metadata.tables["foundation_company_mapping_state"].indexes.clear()
        self.create_foundation_tables()
        with self.engine.connect() as connection:
            report = inspect_foundation(connection)
        self.assertEqual(report["status"], "BLOCKED")
        self.assertTrue(any("modify_type" in problem for problem in report["problems"]))
        self.assertTrue(any("add_index" in problem for problem in report["problems"]))
        with self.assertRaises(MigrationSafetyError):
            apply_foundation(self.engine)
        self.assertNotIn(VERSION_TABLE, inspect(self.engine).get_table_names())

    def test_previous_revision_with_missing_old_tables_is_blocked(self) -> None:
        with self.engine.begin() as connection:
            connection.execute(text(f"CREATE TABLE {VERSION_TABLE} (version_num VARCHAR(32) NOT NULL PRIMARY KEY)"))
            connection.execute(text(f"INSERT INTO {VERSION_TABLE} VALUES ('foundation_0001')"))
        with self.assertRaisesRegex(MigrationSafetyError, "required by the recorded revision"):
            apply_foundation(self.engine)

    def test_column_and_index_drift_prevent_version_adoption(self) -> None:
        self.metadata.tables["foundation_entity"].c.name.type = String(80)
        self.metadata.tables["foundation_entity"].c.name.nullable = True
        self.metadata.tables["foundation_entity"].indexes.clear()
        self.create_foundation_tables()
        with self.engine.connect() as connection:
            report = inspect_foundation(connection)
        self.assertEqual(report["status"], "BLOCKED")
        self.assertTrue(any("modify_type" in problem for problem in report["problems"]))
        self.assertTrue(any("modify_nullable" in problem for problem in report["problems"]))
        self.assertTrue(any("add_index" in problem for problem in report["problems"]))
        with self.assertRaises(MigrationSafetyError):
            apply_foundation(self.engine)
        self.assertNotIn(VERSION_TABLE, inspect(self.engine).get_table_names())

    def test_foreign_key_unique_and_check_drift_are_rejected(self) -> None:
        entity = self.metadata.tables["foundation_entity"]
        entity.append_constraint(CheckConstraint("length(name) > 3", name="extra_name_check"))
        for constraint in list(entity.constraints):
            if constraint.name == "uq_foundation_entity_identifier":
                entity.constraints.remove(constraint)
        for foreign_key in self.metadata.tables["foundation_security"].foreign_key_constraints:
            foreign_key.ondelete = "CASCADE"
        self.create_foundation_tables()
        with self.engine.connect() as connection:
            report = inspect_foundation(connection)
        self.assertEqual(report["status"], "BLOCKED")
        self.assertTrue(any("add_constraint" in problem for problem in report["problems"]))
        self.assertTrue(any("remove_fk" in problem for problem in report["problems"]))
        self.assertTrue(any("CHECK constraints" in problem for problem in report["problems"]))
        with self.assertRaises(MigrationSafetyError):
            apply_foundation(self.engine)

    def test_current_revision_does_not_hide_missing_tables(self) -> None:
        apply_foundation(self.engine)
        self.metadata.tables["foundation_fact_review"].drop(self.engine)
        with self.assertRaises(MigrationSafetyError):
            apply_foundation(self.engine)
        self.assertNotIn("foundation_fact_review", inspect(self.engine).get_table_names())

    def test_unknown_revision_is_not_stamped_over(self) -> None:
        apply_foundation(self.engine)
        with self.engine.begin() as connection:
            connection.execute(text(f"UPDATE {VERSION_TABLE} SET version_num = 'unknown_revision'"))
        with self.assertRaises(MigrationSafetyError):
            apply_foundation(self.engine)
        with self.engine.connect() as connection:
            self.assertEqual(connection.execute(text(f"SELECT version_num FROM {VERSION_TABLE}")).scalar_one(), "unknown_revision")

    def test_missing_legacy_prerequisite_is_not_created_by_migration(self) -> None:
        empty_engine = create_engine("sqlite:///:memory:")
        try:
            with self.assertRaisesRegex(MigrationSafetyError, "stock_symbol is missing"):
                apply_foundation(empty_engine)
            self.assertEqual(inspect(empty_engine).get_table_names(), [])
        finally:
            empty_engine.dispose()

    def test_unrecognized_foundation_table_requires_review(self) -> None:
        with self.engine.begin() as connection:
            connection.execute(text("CREATE TABLE foundation_unregistered (id INTEGER PRIMARY KEY)"))
        with self.assertRaisesRegex(MigrationSafetyError, "Unrecognized foundation tables"):
            apply_foundation(self.engine)
        self.assertNotIn(VERSION_TABLE, inspect(self.engine).get_table_names())

    def test_failure_rolls_back_sqlite_ddl_without_touching_legacy(self) -> None:
        legacy_before = self.legacy_snapshot()

        def fail_upgrade(config, revision):
            connection = config.attributes["connection"]
            self.metadata.tables["foundation_entity"].create(connection)
            raise RuntimeError("simulated interrupted migration")

        with patch("scripts.migrate_foundation.command.upgrade", side_effect=fail_upgrade):
            with self.assertRaisesRegex(RuntimeError, "simulated"):
                apply_foundation(self.engine)
        self.assertNotIn("foundation_entity", inspect(self.engine).get_table_names())
        self.assertEqual(self.legacy_snapshot(), legacy_before)

    def test_cli_check_uses_read_only_file_and_does_not_initialize_missing_file(self) -> None:
        read_engine = target_engine(self.url, read_only=True)
        try:
            with read_engine.connect() as connection:
                self.assertEqual(inspect_foundation(connection)["status"], "PENDING")
                with self.assertRaises(Exception):
                    connection.execute(text("CREATE TABLE forbidden_write (id INTEGER)"))
        finally:
            read_engine.dispose()
        missing = Path(self.temporary.name) / "must-not-create.sqlite"
        with self.assertRaises(MigrationSafetyError):
            target_engine("sqlite:///" + missing.as_posix(), read_only=True)
        self.assertFalse(missing.exists())

    def test_offline_preview_contains_only_foundation_ddl(self) -> None:
        for dialect in ("sqlite", "postgresql"):
            with self.subTest(dialect=dialect):
                sql = preview_sql(dialect)
                self.assertIn("CREATE TABLE foundation_entity", sql)
                self.assertIn("CREATE TABLE foundation_schema_revision", sql)
                self.assertIn("CREATE TABLE foundation_company_mapping_state", sql)
                self.assertNotIn("CREATE TABLE stock_symbol", sql)
                self.assertNotIn("ALTER TABLE stock_symbol", sql)
                self.assertNotIn("DROP TABLE", sql)

    def test_cli_requires_explicit_migration_target(self) -> None:
        with patch.dict("os.environ", {"DATABASE_URL": self.url}, clear=True):
            with redirect_stderr(io.StringIO()), self.assertRaises(SystemExit) as failure:
                main(["check"])
        self.assertEqual(failure.exception.code, 2)
        with redirect_stdout(io.StringIO()) as output:
            self.assertEqual(main(["check", "--database-url", self.url]), 0)
        self.assertIn('"status": "PENDING"', output.getvalue())


if __name__ == "__main__":
    unittest.main()
