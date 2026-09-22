"""Asset discovery and preview preserve business rows and existing permissions."""

import json
import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

from sqlalchemy import create_engine, func, inspect, select, text
from sqlalchemy.orm import Session

from app.db.base import Base
import app.models  # noqa: F401 -- register the application's actual tables
from app.models.ai_hub import AgentDataAsset
from app.models.foundation import FoundationEntity
from app.services.data_dictionary import column_metadata
from app.services.governance import available_source_tables, preview_table
from app.services.resource_hub import DEFAULT_DATA_ASSETS, inspect_data_asset, seed_default_data_assets


class AssetCatalogPreviewTest(unittest.TestCase):
    def setUp(self):
        self.directory = TemporaryDirectory()
        self.engine = create_engine(f"sqlite:///{Path(self.directory.name) / 'preview.db'}")
        Base.metadata.create_all(self.engine)
        self.db = Session(self.engine)

    def tearDown(self):
        self.db.close()
        self.engine.dispose()
        self.directory.cleanup()

    def test_company_assets_reuse_existing_registration_without_replacing_user_settings(self):
        asset = AgentDataAsset(
            asset_code="MY_COMPANY", table_name="foundation_entity", display_name="用户公司目录",
            description="自定义说明", allowed_columns=["name"], enabled=False, governance_status="LOCKED",
        )
        company = FoundationEntity(name="示例公司", entity_type="COMPANY", jurisdiction="CN")
        self.db.add_all([asset, company])
        self.db.commit()
        seed_default_data_assets(self.db)
        seed_default_data_assets(self.db)
        self.db.refresh(asset)
        self.assertEqual(asset.display_name, "用户公司目录")
        self.assertEqual(asset.description, "自定义说明")
        self.assertEqual(asset.allowed_columns, ["name"])
        self.assertFalse(asset.enabled)
        self.assertEqual(asset.governance_status, "LOCKED")
        self.assertEqual(self.db.scalar(select(func.count(FoundationEntity.id))), 1)
        self.assertEqual(self.db.scalar(select(func.count(AgentDataAsset.id))), len(DEFAULT_DATA_ASSETS))
        listing = self.db.scalar(select(AgentDataAsset).where(AgentDataAsset.table_name == "foundation_listing"))
        inspect_data_asset(listing, self.engine)
        self.assertEqual(listing.source_health, "READY")
        self.assertIn("security_id", listing.allowed_columns)

    def test_preview_keeps_full_values_and_allowed_column_boundary(self):
        full_text = "公司原始业务资料。" * 1000
        self.db.add(FoundationEntity(
            name="示例公司", entity_type="COMPANY", jurisdiction="CN",
            properties_json={"business_scope": full_text},
        ))
        self.db.commit()
        result = preview_table("foundation_entity", ["name", "properties_json", "not_a_column"], db_engine=self.engine)
        self.assertEqual(result["columns"], ["name", "properties_json"])
        self.assertEqual(result["row_count"], 1)
        self.assertEqual(set(result["rows"][0]), {"name", "properties_json"})
        self.assertEqual(json.loads(result["rows"][0]["properties_json"])["business_scope"], full_text)
        self.assertEqual(result["column_meta"][0]["label"], "主体名称")
        self.assertIn("同名", result["column_meta"][0]["description"])
        self.assertEqual(result["column_meta"][1]["type"], "JSON")
        self.assertFalse(result["column_meta"][0]["nullable"])
        self.assertEqual(result["display_name"], "主数据主体")

    def test_control_tables_are_hidden_and_custom_business_tables_still_work(self):
        with self.engine.begin() as connection:
            connection.execute(text("CREATE TABLE customer_orders (id INTEGER PRIMARY KEY, custom_metric TEXT)"))
            connection.execute(text("INSERT INTO customer_orders VALUES (1, '原文')"))
            connection.execute(text("CREATE TABLE foundation_private_control (id INTEGER PRIMARY KEY)"))
        sources = available_source_tables(self.engine)
        for blocked in (
            "data_source", "model_provider", "scheduled_job", "pipeline_run", "pipeline_stage_run",
            "foundation_private_control", "foundation_fact_review", "database_table_comment",
        ):
            self.assertNotIn(blocked, sources)
            with self.assertRaises(ValueError):
                preview_table(blocked, db_engine=self.engine)
        result = preview_table("customer_orders", db_engine=self.engine)
        self.assertEqual(result["rows"][0]["custom_metric"], "原文")
        self.assertEqual(result["column_meta"][1]["name"], "custom_metric")
        self.assertEqual(result["column_meta"][1]["label"], "custom_metric")

    def test_all_default_business_asset_fields_have_chinese_metadata(self):
        schema = inspect(self.engine)
        for _, table_name, _, _ in DEFAULT_DATA_ASSETS:
            for column in schema.get_columns(table_name):
                with self.subTest(table=table_name, column=column["name"]):
                    item = column_metadata(table_name, column)
                    self.assertRegex(item["label"], "[\u4e00-\u9fff]")
                    self.assertRegex(item["description"], "[\u4e00-\u9fff]")


if __name__ == "__main__":
    unittest.main()
