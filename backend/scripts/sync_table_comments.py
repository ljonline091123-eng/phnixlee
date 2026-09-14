"""Apply the table-description catalog to an existing database."""

from __future__ import annotations

import argparse
import sqlite3
from contextlib import closing
from datetime import datetime
from pathlib import Path

from sqlalchemy import select

from app.db.migrations import ensure_compat_columns
from app.db.session import SessionLocal, engine
from app.db.table_comments import TABLE_DESCRIPTIONS, column_descriptions_for, seed_table_comments
from app.db.base import Base
from app.models import DatabaseTableComment


def backup_sqlite_database() -> Path | None:
    if engine.dialect.name != "sqlite" or not engine.url.database:
        return None
    database_file = Path(engine.url.database).resolve()
    if not database_file.is_file():
        return None
    backup_dir = database_file.parent / "backups"
    backup_dir.mkdir(exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S_%f")
    backup_file = backup_dir / f"{database_file.stem}_before_table_comments_{timestamp}.db"
    with closing(sqlite3.connect(database_file.as_uri() + "?mode=ro", uri=True)) as source:
        with closing(sqlite3.connect(backup_file)) as target:
            source.backup(target)
    return backup_file


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--backup", action="store_true", help="Back up SQLite before writing comments")
    parser.add_argument("--check", action="store_true", help="Verify comments without writing")
    args = parser.parse_args()

    if args.check:
        with SessionLocal() as session:
            actual = {
                row.table_name: (row.description, row.column_comments_json)
                for row in session.scalars(select(DatabaseTableComment))
            }
        mismatched = sorted(
            name for name, description in TABLE_DESCRIPTIONS.items()
            if actual.get(name) != (
                description.description,
                column_descriptions_for(Base.metadata.tables[name]),
            )
        )
        field_count = sum(len(columns) for _, columns in actual.values())
        print(
            f"Table descriptions found: {len(actual)}; field descriptions: {field_count}; "
            f"missing or different tables: {len(mismatched)}"
        )
        if mismatched:
            print("Missing or different: " + ", ".join(mismatched))
            raise SystemExit(1)
        return

    if args.backup:
        backup_path = backup_sqlite_database()
        if backup_path:
            print(f"SQLite backup: {backup_path}")
    ensure_compat_columns(engine)
    DatabaseTableComment.__table__.create(bind=engine, checkfirst=True)
    with SessionLocal() as session:
        seed_table_comments(session)
    field_count = sum(len(table.columns) for table in Base.metadata.tables.values())
    print(f"Table descriptions synchronized: {len(TABLE_DESCRIPTIONS)}; fields: {field_count}")


if __name__ == "__main__":
    main()
