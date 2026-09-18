from __future__ import annotations

import unittest

from sqlalchemy import create_engine, inspect, select, text
from sqlalchemy.orm import Session

from app.db.base import Base
from app.db.migrations import ensure_compat_columns
from app.db.table_comments import TABLE_DESCRIPTIONS, column_descriptions_for, seed_table_comments
from app.models import DatabaseTableComment


class TableCommentTests(unittest.TestCase):
    def test_every_model_table_has_a_description(self) -> None:
        self.assertEqual(set(Base.metadata.tables), set(TABLE_DESCRIPTIONS))
        self.assertTrue({"selection_run", "selection_candidate", "selection_tracking", "selection_snapshot", "stock_context_event"}.issubset(TABLE_DESCRIPTIONS))
        for name, table in Base.metadata.tables.items():
            self.assertEqual(table.comment, TABLE_DESCRIPTIONS[name].description)
            self.assertEqual(
                {column.name: column.comment for column in table.columns},
                column_descriptions_for(table),
            )

    def test_catalog_is_queryable_and_seed_is_idempotent(self) -> None:
        engine = create_engine("sqlite:///:memory:")
        DatabaseTableComment.__table__.create(engine)
        with Session(engine) as session:
            seed_table_comments(session)
            rows = session.scalars(select(DatabaseTableComment)).all()
            self.assertEqual(len(rows), len(Base.metadata.tables))
            self.assertEqual(
                session.get(DatabaseTableComment, "stock_kline").display_name,
                "股票历史 K 线",
            )
            kline_columns = session.get(DatabaseTableComment, "stock_kline").column_comments_json
            self.assertEqual(len(kline_columns), len(Base.metadata.tables["stock_kline"].columns))
            self.assertIn("收盘价", kline_columns["close_price"])
            seed_table_comments(session)
            self.assertEqual(len(session.scalars(select(DatabaseTableComment)).all()), len(Base.metadata.tables))
        with engine.connect() as connection:
            flattened = connection.execute(text(
                "SELECT j.key, j.value FROM database_table_comment AS t, "
                "json_each(t.column_comments_json) AS j WHERE t.table_name = 'stock_kline'"
            )).all()
            self.assertEqual(len(flattened), len(Base.metadata.tables["stock_kline"].columns))
        engine.dispose()

    def test_existing_catalog_gains_column_comments_without_losing_rows(self) -> None:
        engine = create_engine("sqlite:///:memory:")
        with engine.begin() as connection:
            connection.execute(text(
                "CREATE TABLE database_table_comment ("
                "table_name VARCHAR(128) PRIMARY KEY, display_name VARCHAR(128) NOT NULL, "
                "domain VARCHAR(64) NOT NULL, description TEXT NOT NULL)"
            ))
            connection.execute(text(
                "INSERT INTO database_table_comment VALUES "
                "('stock_kline', '原有名称', '股票数据', '原有说明')"
            ))
        ensure_compat_columns(engine)
        self.assertIn(
            "column_comments_json",
            {column["name"] for column in inspect(engine).get_columns("database_table_comment")},
        )
        with Session(engine) as session:
            self.assertEqual(session.get(DatabaseTableComment, "stock_kline").description, "原有说明")
            seed_table_comments(session)
            self.assertEqual(
                session.get(DatabaseTableComment, "stock_kline").column_comments_json["trade_date"],
                "K 线对应的交易日期。",
            )
        engine.dispose()


if __name__ == "__main__":
    unittest.main()
