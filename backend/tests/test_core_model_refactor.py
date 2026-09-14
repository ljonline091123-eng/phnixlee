from __future__ import annotations

import os
import unittest
from datetime import datetime, timezone
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import patch

import httpx
from cryptography.fernet import Fernet
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, inspect, select, text
from sqlalchemy.orm import sessionmaker

from app.db.base import Base
from app.db.session import get_db
from app.main import app
from app.models.ai_hub import AgentDefinition, AgentSkillLink, ModelCallLog, ModelInstance, ModelProvider, ModelRouteRule, ModelSkill, PredictionLedger, ResearchReportRecord, SkillOptimizationDraft
from app.models.market_data import DataSource, StockKline
from app.services.model_hub import ModelHubService, seed_default_models, seed_default_skills
from app.services.prediction_review import propose_failed_skill_revisions, review_predictions
from app.services.prediction_ledger import record_report_rating
from app.services.resource_hub import seed_default_agents, seed_default_data_assets, seed_default_knowledge_bases
from app.services.skill_files import skill_file_path
from app.connectors.model_adapters import AnthropicMessagesAdapter, GeminiRestAdapter, MockModelAdapter, ModelExecutionResult
from app.db.migrations import ensure_compat_columns


class CoreModelRefactorTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = TemporaryDirectory()
        self.engine = create_engine(f"sqlite:///{Path(self.temp.name) / 'test.db'}", connect_args={"check_same_thread": False})
        Base.metadata.create_all(self.engine)
        self.db = sessionmaker(bind=self.engine)()
        self.skill_root_patch = patch("app.services.skill_files.SKILL_ROOT", Path(self.temp.name) / "skill_docs")
        self.skill_root_patch.start()
        seed_default_models(self.db)
        app.dependency_overrides[get_db] = lambda: self.db
        # TestClient without the lifespan context leaves the real local database untouched.
        self.client = TestClient(app)

    def tearDown(self) -> None:
        self.client.close()
        app.dependency_overrides.clear()
        self.skill_root_patch.stop()
        self.db.close()
        self.engine.dispose()
        self.temp.cleanup()

    def test_provider_key_is_encrypted_and_shared_by_instances(self) -> None:
        key = Fernet.generate_key().decode()
        provider = self.db.scalar(select(ModelProvider).where(ModelProvider.provider_code == "DEEPSEEK"))
        with patch.dict(os.environ, {"MODEL_CREDENTIAL_KEY": key}):
            response = self.client.put(f"/api/v1/model-hub/providers/{provider.id}", json={
                "api_base_url": "https://example.invalid/v1", "api_key": "test-secret-123",
            })
            self.assertEqual(response.status_code, 200, response.text)
            self.assertTrue(response.json()["api_key_configured"])
            self.assertNotIn("api_key_encrypted", response.json())
            self.assertNotIn("test-secret-123", response.text)
            self.db.refresh(provider)
            self.assertNotEqual(provider.api_key_encrypted, "test-secret-123")
            for code in ("DEEPSEEK_CHAT", "DEEPSEEK_V4_FLASH"):
                routed = ModelHubService(self.db)._to_routed_model(
                    self.db.scalar(select(ModelInstance).where(ModelInstance.instance_code == code))
                )
                self.assertEqual(routed.instance.api_key, "test-secret-123")
                self.assertEqual(routed.instance.api_base_url, "https://example.invalid/v1")

    def test_legacy_key_migration_keeps_conflicting_instance_credentials(self) -> None:
        provider = ModelProvider(provider_code="LEGACY_KEYS", provider_name="Legacy keys", provider_type="OPENAI_COMPAT")
        self.db.add(provider)
        self.db.flush()
        first = ModelInstance(provider_id=provider.id, instance_code="LEGACY_A", model_code="a", model_name="A", api_key="shared-old-key")
        second = ModelInstance(provider_id=provider.id, instance_code="LEGACY_B", model_code="b", model_name="B", api_key="different-old-key")
        self.db.add_all([first, second])
        self.db.commit()
        with patch.dict(os.environ, {"MODEL_CREDENTIAL_KEY": Fernet.generate_key().decode()}):
            seed_default_models(self.db)
        self.db.refresh(provider)
        self.db.refresh(first)
        self.db.refresh(second)
        self.assertIsNone(provider.api_key_encrypted)
        self.assertEqual({first.api_key, second.api_key}, {"shared-old-key", "different-old-key"})

    def test_claude_and_gemini_adapters_use_provider_message_formats(self) -> None:
        claude = ModelInstance(provider_id=1, instance_code="CLAUDE_TEST", model_code="claude-sonnet-5",
                               model_name="Claude", api_key="private-key", api_base_url="https://api.anthropic.com/v1",
                               api_path="messages", max_tokens=1024, temperature=1, top_p=1, config_json={})
        with patch("app.connectors.model_adapters.httpx.Client") as client_type:
            post = client_type.return_value.__enter__.return_value.post
            post.return_value.json.return_value = {"content": [{"type": "text", "text": "reviewed"}]}
            result = AnthropicMessagesAdapter().chat(claude, [
                {"role": "system", "content": "Analyze evidence"}, {"role": "user", "content": "A"},
                {"role": "assistant", "content": "B"},
            ])
            self.assertEqual(result.response_text, "reviewed")
            self.assertEqual(post.call_args.args[0], "https://api.anthropic.com/v1/messages")
            self.assertEqual(post.call_args.kwargs["headers"]["Authorization"], "Bearer private-key")
            self.assertEqual(post.call_args.kwargs["json"]["system"], "Analyze evidence")
            self.assertEqual(post.call_args.kwargs["json"]["messages"][1]["role"], "assistant")
            self.assertNotIn("temperature", post.call_args.kwargs["json"])

        gemini = ModelInstance(provider_id=1, instance_code="GEMINI_TEST", model_code="gemini-2.5-pro",
                               model_name="Gemini", api_key="private-key", api_base_url="https://generativelanguage.googleapis.com/v1beta",
                               max_tokens=1024, temperature=0.2, top_p=0.9, config_json={})
        with patch("app.connectors.model_adapters.httpx.Client") as client_type:
            post = client_type.return_value.__enter__.return_value.post
            post.return_value.json.return_value = {"candidates": [{"content": {"parts": [{"text": "one"}, {"text": "two"}]}}]}
            result = GeminiRestAdapter().chat(gemini, [
                {"role": "user", "content": "A"}, {"role": "assistant", "content": "B"},
            ])
            self.assertEqual(result.response_text, "one\ntwo")
            self.assertEqual(post.call_args.kwargs["headers"]["x-goog-api-key"], "private-key")
            self.assertNotIn("private-key", post.call_args.args[0])
            self.assertEqual(post.call_args.kwargs["json"]["contents"][1]["role"], "model")

    def test_route_rejects_missing_or_duplicate_fallbacks(self) -> None:
        base = {"task_type": "test_route", "preferred_instance_code": "MOCK_GENERAL", "enabled": True}
        for fallback in ([], ["MOCK_GENERAL"], ["UNKNOWN_INSTANCE"]):
            response = self.client.post("/api/v1/model-hub/routes", json={**base, "fallback_chain_json": fallback})
            self.assertEqual(response.status_code, 422, response.text)

    def test_legacy_schema_receives_new_columns(self) -> None:
        engine = create_engine(f"sqlite:///{Path(self.temp.name) / 'legacy.db'}")
        with engine.begin() as connection:
            for name in ("model_provider", "model_instance", "model_skill", "agent_definition"):
                connection.execute(text(f"CREATE TABLE {name} (id INTEGER PRIMARY KEY)"))
        ensure_compat_columns(engine)
        inspector = inspect(engine)
        for table_name, column in (("model_provider", "api_key_encrypted"), ("model_instance", "usage_type"),
                                   ("model_skill", "skill_type"), ("agent_definition", "json_schema_output")):
            self.assertIn(column, {item["name"] for item in inspector.get_columns(table_name)})
        engine.dispose()

    def test_transient_http_errors_fall_back_but_bad_request_does_not(self) -> None:
        provider = ModelProvider(provider_code="TEST_REMOTE", provider_name="Test", provider_type="OPENAI_COMPAT")
        self.db.add(provider)
        self.db.flush()
        self.db.add(ModelInstance(provider_id=provider.id, instance_code="TEST_PRIMARY", model_code="test", model_name="Test"))
        self.db.add(ModelRouteRule(task_type="test_failover", preferred_instance_code="TEST_PRIMARY", fallback_chain_json=["MOCK_GENERAL"]))
        self.db.commit()

        class FailingAdapter:
            def __init__(self, status_code: int):
                self.status_code = status_code

            def chat(self, **_kwargs):
                request = httpx.Request("POST", "https://example.invalid/chat")
                response = httpx.Response(self.status_code, request=request)
                raise httpx.HTTPStatusError("remote failure", request=request, response=response)

        for code, expected in ((429, "MOCK_GENERAL"), (500, "MOCK_GENERAL"), (503, "MOCK_GENERAL"), (400, "TEST_PRIMARY")):
            with patch("app.services.model_hub.get_model_adapter", side_effect=lambda kind: FailingAdapter(code) if kind == "OPENAI_COMPAT" else MockModelAdapter()):
                log = ModelHubService(self.db).chat("test_failover", [{"role": "user", "content": "test"}], instance_code="TEST_PRIMARY")
            self.assertEqual(log.instance_code, expected)
            self.assertEqual(log.status, "SUCCESS" if code != 400 else "FAILED")
        class TimeoutAdapter:
            def chat(self, **_kwargs):
                raise httpx.ReadTimeout("upstream timed out")

        with patch("app.services.model_hub.get_model_adapter", side_effect=lambda kind: TimeoutAdapter() if kind == "OPENAI_COMPAT" else MockModelAdapter()):
            timeout_log = ModelHubService(self.db).chat("test_failover", [{"role": "user", "content": "test"}], instance_code="TEST_PRIMARY")
        self.assertEqual(timeout_log.instance_code, "MOCK_GENERAL")
        failures = list(self.db.scalars(select(ModelCallLog).where(ModelCallLog.instance_code == "TEST_PRIMARY")).all())
        self.assertEqual(len(failures), 5)

    def test_skill_sync_versions_and_rollback(self) -> None:
        created = self.client.post("/api/v1/model-hub/skills", json={
            "skill_code": "TEST_REVISION", "skill_name": "Test revision",
            "instructions": "First prompt", "skill_type": "PROMPT_SOP",
        })
        self.assertEqual(created.status_code, 201, created.text)
        skill_id = created.json()["id"]
        updated = self.client.put(f"/api/v1/model-hub/skills/{skill_id}", json={
            "instructions": "Second prompt", "expected_content_hash": created.json()["content_hash"],
        })
        self.assertEqual(updated.status_code, 200, updated.text)
        self.assertEqual(updated.json()["version"], "1.0.1")
        path = skill_file_path("TEST_REVISION")
        self.assertEqual(path.read_text(encoding="utf-8"), "Second prompt")
        path.write_text("External file edit", encoding="utf-8")
        listed = self.client.get("/api/v1/model-hub/skills")
        self.assertEqual(listed.status_code, 200, listed.text)
        current = next(item for item in listed.json() if item["id"] == skill_id)
        self.assertEqual(current["version"], "1.0.2")
        revisions = self.client.get(f"/api/v1/model-hub/skills/{skill_id}/revisions").json()
        self.assertEqual(len(revisions), 3)
        original_id = next(item["id"] for item in revisions if item["version"] == "1.0.0")
        rolled = self.client.post(f"/api/v1/model-hub/skills/{skill_id}/revisions/{original_id}/rollback")
        self.assertEqual(rolled.status_code, 200, rolled.text)
        self.assertEqual(rolled.json()["version"], "1.0.3")
        self.assertEqual(path.read_text(encoding="utf-8"), "First prompt")
        invalid_tool = self.client.post("/api/v1/model-hub/skills", json={
            "skill_code": "BAD_TOOL", "skill_name": "Bad", "instructions": "No function",
            "skill_type": "EXECUTABLE_TOOL", "config_json": {},
        })
        self.assertEqual(invalid_tool.status_code, 422)

    def test_core_skills_tools_and_t_plus_n_review(self) -> None:
        seed_default_skills(self.db)
        self.assertIsNotNone(self.db.scalar(select(ModelSkill).where(ModelSkill.skill_code == "WATCH_ALERT")))
        metrics = [{"market": "CN_A", "stock_code": str(i), "price": 110, "previous_close": 100,
                    "volume": 300, "avg_volume_20": 100, "as_of": "2026-09-14T10:00:00+08:00"} for i in range(80)]
        filtered = self.client.post("/api/v1/model-hub/skill-tools/filter-watch-candidates", json={"metrics": metrics, "max_candidates": 30})
        self.assertEqual(filtered.status_code, 200, filtered.text)
        self.assertEqual(filtered.json()["matched_count"], 80)
        self.assertEqual(len(filtered.json()["candidates"]), 30)
        self.assertTrue(filtered.json()["truncated"])

        source = DataSource(source_code="TEST_PRICE", source_name="Test price", adapter_type="AKSHARE")
        self.db.add(source)
        self.db.flush()
        created = self.client.post("/api/v1/model-hub/predictions", json={
            "market": "CN_A", "stock_code": "000001", "action_type": "BUY",
            "target_timeframe": "T+2", "reasoning_logic": "Testable thesis",
            "skill_code": "RESEARCH_REVIEW_CORRECTION", "entry_price": 10.0,
        })
        self.assertEqual(created.status_code, 201, created.text)
        ledger = self.db.get(PredictionLedger, created.json()["id"])
        ledger.predicted_at = datetime(2020, 1, 1, tzinfo=timezone.utc)
        for date, close in (("2020-01-02", 10.5), ("2020-01-03", 11.0)):
            self.db.add(StockKline(market="CN_A", symbol="000001", period="daily", adjust="", trade_date=date,
                                   close_price=close, source_id=source.id))
        self.db.commit()
        result = review_predictions(self.db)
        self.assertEqual(result["evaluated"], 1)
        self.db.refresh(ledger)
        self.assertTrue(ledger.direction_hit)
        self.assertEqual(ledger.actual_return_pct, 10.0)
        self.assertEqual(ledger.status, "EVALUATED")

    def test_meta_proposal_stays_pending_until_approved(self) -> None:
        seed_default_skills(self.db)
        skill = self.db.scalar(select(ModelSkill).where(ModelSkill.skill_code == "RESEARCH_REVIEW_CORRECTION"))
        provider = ModelProvider(provider_code="TEST_META", provider_name="Test meta", provider_type="OPENAI_COMPAT")
        self.db.add(provider)
        self.db.flush()
        self.db.add(ModelInstance(provider_id=provider.id, instance_code="TEST_META_INSTANCE", model_code="test", model_name="Test", api_key="legacy-test-key"))
        route = self.db.scalar(select(ModelRouteRule).where(ModelRouteRule.task_type == "meta_review"))
        route.preferred_instance_code = "TEST_META_INSTANCE"
        route.fallback_chain_json = ["MOCK_GENERAL"]
        for index in range(3):
            self.db.add(PredictionLedger(market="CN_A", stock_code=f"00000{index}", action_type="BUY",
                predicted_at=datetime(2020, 1, index + 1, tzinfo=timezone.utc), target_timeframe="T+5",
                reasoning_logic="Outdated growth assumption", skill_code=skill.skill_code,
                skill_version_used=skill.version, entry_price=10, actual_price=9,
                actual_return_pct=-10, direction_hit=False, status="EVALUATED"))
        self.db.commit()

        class MetaAdapter:
            def chat(self, **_kwargs):
                content = '{"proposed_instructions":"Revised evidence-based prompt with explicit invalidation rules.","rationale":"Three false BUY calls relied on stale evidence."}'
                return ModelExecutionResult(response_text=content, response_json={})

        with patch("app.services.model_hub.get_model_adapter", side_effect=lambda kind: MetaAdapter() if kind == "OPENAI_COMPAT" else MockModelAdapter()):
            self.assertEqual(propose_failed_skill_revisions(self.db), 1)
        draft = self.db.scalar(select(SkillOptimizationDraft))
        self.assertEqual(draft.status, "PENDING_REVIEW")
        self.db.refresh(skill)
        self.assertEqual(skill.version, "1.0.0")
        approved = self.client.post(f"/api/v1/model-hub/skill-drafts/{draft.id}/approve")
        self.assertEqual(approved.status_code, 200, approved.text)
        self.assertEqual(approved.json()["version"], "1.0.1")
        self.db.refresh(draft)
        self.assertEqual(draft.status, "APPROVED")

    def test_agent_structured_advice_and_report_rating_enter_ledger(self) -> None:
        seed_default_skills(self.db)
        skill = self.db.scalar(select(ModelSkill).where(ModelSkill.skill_code == "STOCK_TREND_ADVISOR"))
        provider = ModelProvider(provider_code="TEST_ADVICE", provider_name="Test advice", provider_type="OPENAI_COMPAT")
        self.db.add(provider)
        self.db.flush()
        self.db.add(ModelInstance(provider_id=provider.id, instance_code="TEST_ADVICE_MODEL", model_code="test", model_name="Test", api_key="legacy-test-key"))
        agent = AgentDefinition(agent_code="TEST_ADVISOR", display_name="Test advisor", system_prompt="Analyze candidates",
                                model_instance_code="TEST_ADVICE_MODEL", json_schema_output={"type": "object"})
        self.db.add(agent)
        self.db.flush()
        self.db.add(AgentSkillLink(agent_id=agent.id, skill_id=skill.id))
        self.db.commit()

        class AdviceAdapter:
            def chat(self, **_kwargs):
                content = '{"prediction":{"market":"CN_A","stock_code":"000001","action_type":"BUY","target_timeframe":"T+5","reasoning_logic":"Revenue growth and volume confirmation","skill_code":"STOCK_TREND_ADVISOR","entry_price":10.0}}'
                return ModelExecutionResult(response_text=content, response_json={})

        with patch("app.services.model_hub.get_model_adapter", return_value=AdviceAdapter()):
            response = self.client.post(f"/api/v1/resources/agents/{agent.id}/run", json={
                "task_type": "stock_analysis", "messages": [{"role": "user", "content": "Analyze 000001"}],
            })
        self.assertEqual(response.status_code, 200, response.text)
        self.assertEqual(len(response.json()["prediction_ids"]), 1)
        advice = self.db.get(PredictionLedger, response.json()["prediction_ids"][0])
        self.assertEqual(advice.skill_version_used, skill.version)
        self.assertEqual(advice.model_instance_code, "TEST_ADVICE_MODEL")

        report = ResearchReportRecord(market="CN_A", symbol="000001", rating="B", score=68,
                                      conclusion="Positive trend", report_markdown="# Report")
        self.db.add(report)
        self.db.flush()
        report_prediction = record_report_rating(self.db, report, entry_price=10.5)
        self.db.commit()
        self.assertIsNotNone(report_prediction)
        self.assertEqual(report_prediction.action_type, "BUY")
        self.assertEqual(report_prediction.research_report_id, report.id)

    def test_startup_seed_keeps_agent_edits(self) -> None:
        seed_default_skills(self.db)
        seed_default_data_assets(self.db)
        seed_default_knowledge_bases(self.db)
        seed_default_agents(self.db)
        agent = self.db.scalar(select(AgentDefinition).where(AgentDefinition.agent_code == "RISK_WARNING_AGENT"))
        self.assertIn("WATCH_ALERT", {link.skill.skill_code for link in agent.skill_links})
        agent.system_prompt = "Operator-maintained risk rules"
        self.db.commit()
        seed_default_agents(self.db)
        self.db.refresh(agent)
        self.assertEqual(agent.system_prompt, "Operator-maintained risk rules")


if __name__ == "__main__":
    unittest.main()
