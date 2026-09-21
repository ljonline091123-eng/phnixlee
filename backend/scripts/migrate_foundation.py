"""Inspect or apply foundation-only migrations without bootstrapping the app."""

from __future__ import annotations

import argparse
import io
import json
import os
from pathlib import Path
import sys
from typing import Any

from alembic import command
from alembic.autogenerate import compare_metadata
from alembic.config import Config
from alembic.migration import MigrationContext
from alembic.script import ScriptDirectory
from sqlalchemy import MetaData, create_engine, inspect, text
from sqlalchemy.engine import Connection, Engine, make_url


BACKEND_ROOT = Path(__file__).resolve().parents[1]
if str(BACKEND_ROOT) not in sys.path:
    sys.path.insert(0, str(BACKEND_ROOT))

HEAD = "foundation_0003"
VERSION_TABLE = "foundation_schema_revision"
DATABASE_ENV = "FOUNDATION_MIGRATION_DATABASE_URL"


class MigrationSafetyError(RuntimeError):
    """A schema or scope mismatch that must be resolved before applying DDL."""


def migration_config(*, output_buffer: io.StringIO | None = None) -> Config:
    return Config(str(BACKEND_ROOT / "alembic.ini"), output_buffer=output_buffer)


def revision_metadata(revision_id: str = HEAD) -> MetaData:
    revision = ScriptDirectory.from_config(migration_config()).get_revision(revision_id)
    if revision is None:
        raise MigrationSafetyError("The foundation revision is missing.")
    return revision.module.build_metadata()


def supported_revisions() -> set[str]:
    """Only accept revisions on the checked foundation upgrade history."""
    return {revision.revision for revision in ScriptDirectory.from_config(
        migration_config()).iterate_revisions(HEAD, "base")}


def managed_table_names(metadata: MetaData) -> set[str]:
    return {name for name in metadata.tables if name.startswith("foundation_")}


def _sql_tokens(value: str | None) -> str:
    return "".join((value or "").split())


def inspect_foundation(connection: Connection, metadata: MetaData | None = None) -> dict[str, Any]:
    """Read schema state without creating tables or invoking application startup."""
    metadata = metadata if metadata is not None else revision_metadata()
    expected = managed_table_names(metadata)
    inspector = inspect(connection)
    existing = set(inspector.get_table_names())
    missing = sorted(expected - existing)
    present = sorted(expected & existing)
    problems: list[str] = []
    unknown = sorted(name for name in existing if name.startswith("foundation_") and name not in expected | {VERSION_TABLE})
    if unknown:
        problems.append("Unrecognized foundation tables: " + ", ".join(unknown))

    if "stock_symbol" not in existing:
        problems.append("Required legacy table stock_symbol is missing; initialize the application schema separately.")
    else:
        stock_columns = {column["name"]: column for column in inspector.get_columns("stock_symbol")}
        stock_pk = inspector.get_pk_constraint("stock_symbol").get("constrained_columns") or []
        if "id" not in stock_columns or stock_pk != ["id"] or "INT" not in str(stock_columns["id"]["type"]).upper():
            problems.append("Required legacy key stock_symbol.id must be an integer primary key.")

    def include_name(name: str | None, type_: str, parent_names: dict[str, Any]) -> bool:
        return type_ != "table" or name in expected

    def include_object(obj: Any, name: str | None, type_: str, reflected: bool, compare_to: Any) -> bool:
        return type_ != "table" or name in expected

    migration_context = MigrationContext.configure(connection, opts={
        "include_name": include_name,
        "include_object": include_object,
        "compare_type": True,
        "compare_server_default": True,
        "version_table": VERSION_TABLE,
    })
    differences = compare_metadata(migration_context, metadata)
    for difference in differences:
        if isinstance(difference, tuple) and difference[0] == "add_table" and difference[1].name in missing:
            continue
        if isinstance(difference, tuple) and difference[0] == "add_index" and difference[1].table.name in missing:
            continue
        problems.append(f"Schema differs from frozen {HEAD}: " + repr(difference))

    # Alembic does not compare CHECK constraints or primary keys automatically.
    for name in present:
        table = metadata.tables[name]
        expected_pk = [column.name for column in table.primary_key.columns]
        actual_pk = inspector.get_pk_constraint(name).get("constrained_columns") or []
        if expected_pk != actual_pk:
            problems.append(f"{name}: primary key differs from the frozen revision.")
        expected_checks = sorted(_sql_tokens(str(item.sqltext)) for item in table.constraints if hasattr(item, "sqltext"))
        actual_checks = sorted(_sql_tokens(item["sqltext"]) for item in inspector.get_check_constraints(name))
        if expected_checks != actual_checks:
            problems.append(f"{name}: CHECK constraints differ from the frozen revision.")

    current_revision: str | None = None
    if VERSION_TABLE in existing:
        columns = inspector.get_columns(VERSION_TABLE)
        pk = inspector.get_pk_constraint(VERSION_TABLE).get("constrained_columns") or []
        if (
            len(columns) != 1
            or columns[0]["name"] != "version_num"
            or str(columns[0]["type"]).upper() != "VARCHAR(32)"
            or columns[0]["nullable"]
            or pk != ["version_num"]
        ):
            problems.append("The foundation version table has an unexpected schema.")
        else:
            versions = list(connection.execute(text(f'SELECT version_num FROM "{VERSION_TABLE}"')).scalars())
            if len(versions) != 1 or versions[0] not in supported_revisions():
                problems.append("The foundation version table is empty or has an unsupported revision.")
            else:
                current_revision = versions[0]
                recorded_tables = managed_table_names(revision_metadata(current_revision))
                if recorded_tables - existing:
                    problems.append("Some tables required by the recorded revision are missing.")

    status = ("BLOCKED" if problems else "CURRENT" if current_revision == HEAD else
              "UPGRADE_REQUIRED" if current_revision else "UNVERSIONED" if present else "PENDING")
    return {
        "status": status,
        "target_revision": HEAD,
        "current_revision": current_revision,
        "managed_tables": sorted(expected),
        "present_tables": present,
        "tables_to_create": missing,
        "verified_tables_to_adopt": present if not current_revision and not problems else [],
        "problems": problems,
    }


def ensure_migratable(connection: Connection, metadata: MetaData | None = None) -> dict[str, Any]:
    report = inspect_foundation(connection, metadata)
    if report["problems"]:
        raise MigrationSafetyError("\n".join(report["problems"]))
    return report


def apply_foundation(engine: Engine) -> dict[str, Any]:
    """Create missing managed tables or adopt a fully checked create_all schema."""
    if engine.dialect.name not in {"sqlite", "postgresql"}:
        raise MigrationSafetyError("Only SQLite and PostgreSQL are supported by this migration runner.")
    with engine.connect() as connection, connection.begin():
        if connection.dialect.name == "sqlite":
            connection.exec_driver_sql("BEGIN IMMEDIATE")
        else:
            connection.execute(text("SELECT pg_advisory_xact_lock(710204001)"))
        before = ensure_migratable(connection)
        if before["status"] == "CURRENT":
            return before
        config = migration_config()
        config.attributes["connection"] = connection
        config.attributes["checked_foundation_apply"] = True
        command.upgrade(config, HEAD)
        after = ensure_migratable(connection)
        if after["status"] != "CURRENT":
            raise MigrationSafetyError("The migration did not produce the expected schema and revision.")
        return after


def preview_sql(dialect: str = "sqlite") -> str:
    output = io.StringIO()
    config = migration_config(output_buffer=output)
    config.attributes["preview_dialect"] = dialect
    command.upgrade(config, HEAD, sql=True)
    return output.getvalue()


def target_engine(raw_url: str, *, read_only: bool) -> Engine:
    url = make_url(raw_url)
    if url.get_backend_name() not in {"sqlite", "postgresql"}:
        raise MigrationSafetyError("Only SQLite and PostgreSQL database URLs are supported.")
    if url.get_backend_name() == "sqlite":
        if not url.database or url.database == ":memory:" or url.query:
            raise MigrationSafetyError("Use an existing SQLite file path without URI/query parameters.")
        database_path = Path(url.database).resolve()
        if not database_path.is_file():
            raise MigrationSafetyError("The SQLite database file does not exist; this runner does not initialize application databases.")
        if read_only:
            url = url.set(database="file:" + database_path.as_posix(), query={"mode": "ro", "uri": "true"})
        else:
            url = url.set(database=str(database_path))
    return create_engine(url)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=("preview", "check", "apply"))
    parser.add_argument("--database-url", help=f"Explicit target URL; alternatively set {DATABASE_ENV}. No .env file is loaded.")
    parser.add_argument("--dialect", choices=("sqlite", "postgresql"), default="sqlite", help="Offline preview dialect.")
    args = parser.parse_args(argv)
    if args.action == "preview":
        print(preview_sql(args.dialect))
        return 0
    raw_url = args.database_url or os.environ.get(DATABASE_ENV)
    if not raw_url:
        parser.error(f"{args.action} requires --database-url or {DATABASE_ENV}")
    engine: Engine | None = None
    try:
        engine = target_engine(raw_url, read_only=args.action == "check")
        if args.action == "apply":
            report = apply_foundation(engine)
        else:
            with engine.connect() as connection:
                if engine.dialect.name == "postgresql":
                    connection.exec_driver_sql("SET TRANSACTION READ ONLY")
                report = inspect_foundation(connection)
        print(json.dumps(report, indent=2, ensure_ascii=True))
        return 2 if report["status"] == "BLOCKED" else 0
    except MigrationSafetyError as exc:
        print(str(exc), file=sys.stderr)
        return 2
    except Exception as exc:
        print(f"Migration failed ({type(exc).__name__}); check the explicit target, credentials and database availability.", file=sys.stderr)
        return 2
    finally:
        if engine is not None:
            engine.dispose()


if __name__ == "__main__":
    raise SystemExit(main())
