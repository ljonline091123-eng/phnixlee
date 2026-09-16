import json
import unittest
from datetime import datetime, timezone
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import patch

from fastapi.testclient import TestClient
from sqlalchemy import create_engine, select, func, text
from sqlalchemy.orm import sessionmaker

from app.db.base import Base
from app.db.session import get_db
from app.main import app
from app.connectors.model_adapters import ModelExecutionResult
from app.models.ai_hub import AgentDataAsset, GovernanceRun, KnowledgeBase, KnowledgeDocument, KnowledgeEntity, KnowledgeGraph, ModelProvider, ModelSkill, ResearchReportRecord
from app.services.model_hub import seed_default_models, seed_default_skills
from app.services.resource_hub import seed_default_data_assets, seed_default_graphs, seed_default_knowledge_bases


class ResourceGovernanceTest(unittest.TestCase):
    def setUp(self):
        self.directory = TemporaryDirectory()
        self.engine = create_engine(f"sqlite:///{Path(self.directory.name) / 'governance.db'}", connect_args={"check_same_thread": False})
        Base.metadata.create_all(self.engine)
        self.sessions = sessionmaker(bind=self.engine, expire_on_commit=False)
        self.db = self.sessions()
        seed_default_models(self.db)
        seed_default_data_assets(self.db)
        seed_default_knowledge_bases(self.db)
        seed_default_graphs(self.db)
        for symbol in ("000001", "000002"):
            self.db.add(ResearchReportRecord(market="CN_A", symbol=symbol, name=symbol,
                                             report_markdown=f"{symbol} 研究证据", title=f"{symbol} 研报"))
        self.db.commit()
        app.dependency_overrides[get_db] = lambda: self.db
        self.client = TestClient(app)

    def tearDown(self):
        self.client.close()
        app.dependency_overrides.clear()
        self.db.close()
        self.engine.dispose()
        self.directory.cleanup()

    def test_asset_preview_governance_lock_and_batch_skip(self):
        assets = self.client.get("/api/v1/resources/data-assets").json()
        report = next(item for item in assets if item["asset_code"] == "RESEARCH_REPORT")
        invalid_limit = self.client.post(
            f"/api/v1/resources/data-assets/{report['id']}/govern", json={"record_limit": 5001}
        )
        self.assertEqual(invalid_limit.status_code, 422)
        self.assertEqual(report["governance_status"], "PENDING")
        self.assertEqual(report["source_health"], "READY")
        self.client.put(f"/api/v1/resources/data-assets/{report['id']}", json={"enabled": False})
        preview = self.client.get(f"/api/v1/resources/data-assets/{report['id']}/preview").json()
        self.assertEqual(preview["row_count"], 2)
        self.assertEqual(len(preview["rows"]), 2)
        governed = self.client.post(f"/api/v1/resources/data-assets/{report['id']}/govern", json={}).json()
        self.assertEqual(governed["status"], "PENDING_REVIEW")
        self.assertFalse(self.db.get(AgentDataAsset, report["id"]).enabled)
        self.assertEqual(len(governed["summary_json"]["quality_issues"]), 2)
        self.assertEqual(
            {item["field_name"] for item in governed["summary_json"]["quality_issues"]},
            {"source"},
        )
        self.assertEqual(self.db.get(AgentDataAsset, report["id"]).governance_status, "PENDING")
        self.client.put(f"/api/v1/resources/data-assets/{report['id']}/governance-state", json={"governance_status": "LOCKED"})
        skipped = self.client.post("/api/v1/resources/data-assets/govern/batch", json={"target_ids": [report["id"]]}).json()
        self.assertEqual(skipped["results"][0]["status"], "SKIPPED")
        self.assertEqual(self.db.scalar(select(func.count(GovernanceRun.id))), 1)

    def test_asset_governance_marks_completed_only_after_valid_structured_output(self):
        for report in self.db.scalars(select(ResearchReportRecord)).all():
            report.model_provider = "TEST_PROVIDER"
        self.db.commit()
        asset = self.db.scalar(select(AgentDataAsset).where(AgentDataAsset.asset_code == "RESEARCH_REPORT"))
        captured_messages = []

        class StructuredAdapter:
            def chat(self, **kwargs):
                captured_messages.extend(kwargs["messages"])
                response = {
                    "as_of": datetime.now(timezone.utc).isoformat(),
                    "status": "COMPLETED",
                    "category_counts": {"RESEARCH_REPORT": 2},
                    "quality_issues": [],
                    "dedupe_keys": ["source_record_id"],
                    "target_tables": [],
                    "pending_review": [],
                    "missing_data": [],
                    "confidence": 0.94,
                    "evidence_text": "Validated bounded hard-rule summary.",
                }
                return ModelExecutionResult(response_text=json.dumps(response), response_json=response)

        with patch("app.services.model_hub.get_model_adapter", return_value=StructuredAdapter()):
            response = self.client.post(
                f"/api/v1/resources/data-assets/{asset.id}/govern",
                json={"record_limit": 5000},
            )
        self.assertEqual(response.status_code, 200, response.text)
        result = response.json()
        self.assertEqual(result["status"], "COMPLETED")
        self.assertEqual(result["summary_json"]["quality_issues"], [])
        self.assertEqual(result["summary_json"]["target_tables"][0]["table_name"], "research_report")
        self.assertIsNotNone(result["model_call_log_id"])
        self.db.refresh(asset)
        self.assertEqual(asset.governance_status, "GOVERNED")
        user_prompt = next(item["content"] for item in captured_messages if item["role"] == "user")
        self.assertNotIn("研究证据", user_prompt)

    def test_two_graphs_from_one_base_have_independent_entities(self):
        kb = self.client.get("/api/v1/resources/knowledge-bases").json()[0]
        assets = self.client.get("/api/v1/resources/data-assets").json()
        report = next(item for item in assets if item["asset_code"] == "RESEARCH_REPORT")
        ids = []
        for symbol in ("000001", "000002"):
            response = self.client.post("/api/v1/resources/knowledge-graphs", json={
                "knowledge_base_id": kb["id"], "graph_code": f"GRAPH_{symbol}",
                "graph_name": f"{symbol} 图谱", "symbol": symbol,
                "source_tables": ["research_report"], "enabled": True,
            })
            self.assertEqual(response.status_code, 201, response.text)
            ids.append(response.json()["id"])
        for graph_id in ids:
            if graph_id == ids[0]:
                self.client.put(f"/api/v1/resources/knowledge-graphs/{graph_id}", json={"enabled": False})
            response = self.client.post(f"/api/v1/resources/knowledge-graphs/{graph_id}/govern",
                                        json={"source_asset_ids": [report["id"]]})
            self.assertEqual(response.status_code, 200, response.text)
        first = self.client.get(f"/api/v1/resources/knowledge-graphs/{ids[0]}/explore?q=000001").json()
        self.assertTrue(first["relations"])
        self.assertTrue(first["relations"][0]["evidence"])
        first_docs = self.db.scalars(select(KnowledgeDocument).where(KnowledgeDocument.graph_id == ids[0])).all()
        second_docs = self.db.scalars(select(KnowledgeDocument).where(KnowledgeDocument.graph_id == ids[1])).all()
        self.assertEqual([item.symbol for item in first_docs], ["000001"])
        self.assertEqual([item.symbol for item in second_docs], ["000002"])
        self.assertTrue(self.db.scalars(select(KnowledgeEntity).where(KnowledgeEntity.graph_id == ids[1])).all())
        self.assertEqual(self.db.get(KnowledgeGraph, ids[0]).governance_status, "GOVERNED")
        self.assertFalse(self.db.get(KnowledgeGraph, ids[0]).enabled)
        self.client.put(f"/api/v1/resources/knowledge-graphs/{ids[0]}/governance-state",
                        json={"governance_status": "LOCKED"})
        skipped = self.client.post("/api/v1/resources/knowledge-graphs/govern/batch",
                                   json={"target_ids": [ids[0]]}).json()
        self.assertEqual(skipped["results"][0]["status"], "SKIPPED")

    def test_legacy_knowledge_rows_and_builtin_text_are_restored(self):
        kb = KnowledgeBase(kb_code="LEGACY_KB", kb_name="旧知识库", source_tables=["research_report"], status="READY")
        self.db.add(kb)
        self.db.flush()
        document = KnowledgeDocument(knowledge_base_id=kb.id, source_table="research_report",
                                     symbol="000001", title="旧证据", content="保留内容")
        self.db.add(document)
        self.db.commit()
        seed_default_graphs(self.db)
        migrated = self.db.get(KnowledgeDocument, document.id)
        graph = self.db.scalar(select(KnowledgeGraph).where(KnowledgeGraph.graph_code == "LEGACY_KB"))
        self.assertEqual(migrated.graph_id, graph.id)
        self.assertEqual(migrated.content, "保留内容")

        seed_default_skills(self.db)
        skill = self.db.scalar(select(ModelSkill).where(ModelSkill.skill_code == "DATA_GOVERNANCE_DW"))
        provider = self.db.scalar(select(ModelProvider).where(ModelProvider.provider_code == "MOCK"))
        skill.skill_name = "???????"
        skill.instructions = "???????"
        skill.description = "用户自定义说明"
        provider.config_json = {**provider.config_json, "billing_label": "??"}
        self.db.commit()
        seed_default_skills(self.db)
        seed_default_models(self.db)
        self.assertEqual(skill.skill_name, "股票数据治理与 DW 分层")
        self.assertIn("数据治理", skill.instructions)
        self.assertEqual(skill.description, "用户自定义说明")
        self.assertEqual(provider.config_json["billing_label"], "免费")

    def test_new_local_asset_can_feed_a_new_graph(self):
        with self.engine.begin() as connection:
            connection.execute(text("CREATE TABLE shareholder_info (id INTEGER PRIMARY KEY, market TEXT, symbol TEXT, name TEXT)"))
            connection.execute(text("INSERT INTO shareholder_info (id, market, symbol, name) VALUES (1, 'CN_A', '000003', '新增来源')"))
        asset_response = self.client.post("/api/v1/resources/data-assets", json={
            "asset_code": "SHAREHOLDER_INFO", "table_name": "shareholder_info",
            "display_name": "新增股票资料", "enabled": True,
        })
        self.assertEqual(asset_response.status_code, 201, asset_response.text)
        self.assertEqual(self.client.get(f"/api/v1/resources/data-assets/{asset_response.json()['id']}/preview").json()["row_count"], 1)
        kb = self.client.get("/api/v1/resources/knowledge-bases").json()[0]
        update = self.client.put(f"/api/v1/resources/knowledge-bases/{kb['id']}",
                                 json={"source_tables": [*kb["source_tables"], "shareholder_info"]})
        self.assertEqual(update.status_code, 200, update.text)
        graph_response = self.client.post("/api/v1/resources/knowledge-graphs", json={
            "knowledge_base_id": kb["id"], "graph_code": "CUSTOM_GRAPH",
            "graph_name": "新增来源图谱", "source_tables": ["shareholder_info"],
            "symbol": "000003", "enabled": True,
        })
        self.assertEqual(graph_response.status_code, 201, graph_response.text)
        graph_id = graph_response.json()["id"]
        governed = self.client.post(f"/api/v1/resources/knowledge-graphs/{graph_id}/govern",
                                    json={"source_asset_ids": [asset_response.json()["id"]]})
        self.assertEqual(governed.status_code, 200, governed.text)
        result = self.client.get(f"/api/v1/resources/knowledge-graphs/{graph_id}/explore?q=000003").json()
        self.assertTrue(result["relations"])
        self.assertEqual(result["relations"][0]["evidence"]["source_table"], "shareholder_info")


if __name__ == "__main__":
    unittest.main()
