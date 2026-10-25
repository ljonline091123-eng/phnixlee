import unittest
import json
import os
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import Mock, patch

from sqlalchemy import create_engine, select
from sqlalchemy.orm import sessionmaker

from app.db.base import Base
from app.db.session import get_db
from app.core.config import get_settings
from app.connectors.model_adapters import ModelExecutionResult
from app.models.ai_hub import ModelCallLog, ModelInstance, ModelProvider, ModelRouteRule, ModelSkill
from app.services.model_hub import ModelHubService, _normalize_bounded_json, seed_default_models


class ModelHubFoundationTest(unittest.TestCase):
    temp_dir: TemporaryDirectory[str] | None = None

    @classmethod
    def setUpClass(cls) -> None:
        cls.temp_dir = TemporaryDirectory()
        cls.db_path = Path(cls.temp_dir.name) / "test_quant_models.db"
        cls.engine = create_engine(f"sqlite:///{cls.db_path}")
        Base.metadata.create_all(cls.engine)
        cls.session_factory = sessionmaker(bind=cls.engine)

    @classmethod
    def tearDownClass(cls) -> None:
        cls.engine.dispose()
        if cls.temp_dir:
            cls.temp_dir.cleanup()

    def setUp(self) -> None:
        self.db = self.session_factory()
        seed_default_models(self.db)

    def tearDown(self) -> None:
        self.db.close()

    def test_default_model_catalog_seeded(self) -> None:
        provider = self.db.scalar(select(ModelProvider).where(ModelProvider.provider_code == "MOCK"))
        deepseek = self.db.scalar(select(ModelProvider).where(ModelProvider.provider_code == "DEEPSEEK"))
        deepseek_instance = self.db.scalar(select(ModelInstance).where(ModelInstance.instance_code == "DEEPSEEK_CHAT"))
        instance = self.db.scalar(select(ModelInstance).where(ModelInstance.instance_code == "MOCK_GENERAL"))
        route = self.db.scalar(select(ModelRouteRule).where(ModelRouteRule.task_type == "general_chat"))
        self.assertIsNotNone(provider)
        self.assertEqual(provider.provider_type, "MOCK")
        self.assertIsNotNone(deepseek)
        self.assertEqual(deepseek.provider_type, "DEEPSEEK")
        self.assertIsNotNone(deepseek_instance)
        self.assertEqual(deepseek_instance.model_code, "deepseek-v4-pro")
        self.assertIsNotNone(instance)
        self.assertIsNotNone(route)
        self.assertEqual(route.preferred_instance_code, "DEEPSEEK_V4_FLASH")
        self.assertEqual(route.fallback_chain_json, ["GEMINI_FLASH", "MOCK_GENERAL"])

    def test_legacy_route_upgrades_without_overwriting_custom_route(self) -> None:
        with TemporaryDirectory() as folder:
            engine = create_engine(f"sqlite:///{Path(folder) / 'routes.db'}")
            Base.metadata.create_all(engine)
            with sessionmaker(bind=engine)() as db:
                seed_default_models(db)
                route = db.scalar(select(ModelRouteRule).where(ModelRouteRule.task_type == "general_chat"))
                deepseek = db.scalar(select(ModelInstance).where(ModelInstance.instance_code == "DEEPSEEK_CHAT"))
                deepseek.fallback_instance_code = "QWEN_PLUS"
                route.preferred_instance_code = "QWEN_PLUS"
                route.fallback_chain_json = ["DEEPSEEK_V4_FLASH", "GEMINI_FLASH", "OPENAI_CHATGPT", "MOCK_GENERAL"]
                db.commit()
                seed_default_models(db)
                self.assertEqual(route.preferred_instance_code, "DEEPSEEK_V4_FLASH")
                self.assertEqual(route.fallback_chain_json, ["GEMINI_FLASH", "MOCK_GENERAL"])
                self.assertEqual(deepseek.fallback_instance_code, "GEMINI_PRO")
                route.preferred_instance_code = "GEMINI_PRO"
                route.fallback_chain_json = ["MOCK_GENERAL"]
                db.commit()
                seed_default_models(db)
                self.assertEqual(route.preferred_instance_code, "GEMINI_PRO")
                self.assertEqual(route.fallback_chain_json, ["MOCK_GENERAL"])
            engine.dispose()

    def test_mock_chat_works(self) -> None:
        service = ModelHubService(self.db)
        log = service.chat(
            task_type="general_chat",
            messages=[{"role": "user", "content": "请总结一下当前市场"}],
        )
        self.assertEqual(log.status, "SUCCESS")
        self.assertIn("已收到", log.response_text)
        self.assertIsNotNone(self.db.scalar(select(ModelCallLog).where(ModelCallLog.id == log.id)))

    def test_bounded_schema_repair_is_audited_and_does_not_change_other_fields(self) -> None:
        value, repairs = _normalize_bounded_json(
            {"items": ["a", "b"], "label": "keep"},
            {"type": "object", "properties": {
                "items": {"type": "array", "maxItems": 1, "items": {"type": "string"}},
                "label": {"type": "string"},
            }},
            {"type": "object", "properties": {
                "items": {"type": "array", "maxItems": 1, "items": {"type": "string"}},
                "label": {"type": "string"},
            }},
        )
        self.assertEqual(value, {"items": ["a"], "label": "keep"})
        self.assertEqual(repairs, ["$.items: truncated 2 items to maxItems=1"])

    def test_bounded_schema_repair_keeps_provider_call_successful(self) -> None:
        service = ModelHubService(self.db)
        adapter = Mock()
        adapter.chat.return_value = ModelExecutionResult(
            response_text='{"items":["a","b"],"label":"keep"}', response_json={"provider": "test"}
        )
        schema = {"type": "object", "additionalProperties": False,
                  "properties": {"items": {"type": "array", "maxItems": 1, "items": {"type": "string"}},
                                  "label": {"type": "string"}},
                  "required": ["items", "label"]}
        with patch("app.services.model_hub.get_model_adapter", return_value=adapter):
            log = service.chat(
                task_type="general_chat", instance_code="MOCK_GENERAL",
                messages=[{"role": "user", "content": "test"}],
                metadata_json={"json_schema_output": schema, "bounded_schema_repair": True,
                               "strict_instance": True},
            )
        self.assertEqual(log.status, "SUCCESS")
        self.assertEqual(json.loads(log.response_text or "{}"), {"items": ["a"], "label": "keep"})
        self.assertEqual(log.response_json["schema_repairs"], ["$.items: truncated 2 items to maxItems=1"])

    def test_soft_analysis_limit_includes_skill_instructions(self) -> None:
        self.db.add(ModelSkill(
            skill_code="TEST_LONG_PROMPT", skill_name="Long prompt", instructions="s" * 2_000,
            enabled=True, skill_type="PROMPT_SOP",
        ))
        self.db.commit()
        with TemporaryDirectory() as temporary, patch("app.services.skill_files.SKILL_ROOT", Path(temporary) / "skills"), self.assertRaisesRegex(ValueError, "after skill instructions"):
            ModelHubService(self.db).chat(
                task_type="stock_screening",
                messages=[{"role": "user", "content": "u" * 59_000}],
                metadata_json={"skill_code": "TEST_LONG_PROMPT"},
            )

    def test_gemini_runtime_key_is_used_without_persisting_it(self) -> None:
        instance = self.db.scalar(select(ModelInstance).where(ModelInstance.instance_code == "GEMINI_FLASH"))
        self.assertIsNotNone(instance)
        with patch.dict(os.environ, {"GEMINI_API_KEY": "runtime-secret"}, clear=False):
            get_settings.cache_clear()
            routed = ModelHubService(self.db)._to_routed_model(instance)
            self.assertEqual(routed.instance.api_key, "runtime-secret")
            self.assertIsNone(instance.api_key)
        get_settings.cache_clear()

    def test_route_falls_back_to_mock_when_preferred_fails(self) -> None:
        provider = ModelProvider(
            provider_code="BROKEN_PROVIDER",
            provider_name="Broken Provider",
            provider_type="OPENAI_COMPAT",
            enabled=True,
        )
        self.db.add(provider)
        self.db.flush()
        backup = ModelInstance(
            provider_id=provider.id,
            instance_code="BROKEN_INSTANCE",
            model_code="broken-model",
            model_name="Broken Model",
            enabled=True,
        )
        self.db.add(backup)
        self.db.add(
            ModelRouteRule(
                task_type="risk_review",
                preferred_instance_code="BROKEN_INSTANCE",
                fallback_chain_json=["MOCK_GENERAL"],
                route_policy="PREFERRED_THEN_FALLBACK",
                enabled=True,
            )
        )
        self.db.commit()
        log = ModelHubService(self.db).chat(
            task_type="risk_review",
            messages=[{"role": "user", "content": "测试回退"}],
        )
        self.assertEqual(log.status, "SUCCESS")
        self.assertEqual(log.instance_code, "MOCK_GENERAL")
        failed_attempt = self.db.scalar(
            select(ModelCallLog).where(
                ModelCallLog.task_type == "risk_review",
                ModelCallLog.instance_code == "BROKEN_INSTANCE",
            )
        )
        self.assertIsNotNone(failed_attempt)
        self.assertEqual(failed_attempt.status, "FAILED")

    def test_empty_real_response_is_failed_and_uses_real_fallback(self) -> None:
        service = ModelHubService(self.db)
        codes = ["DEEPSEEK_CHAT", "DEEPSEEK_V4_FLASH"]
        routed = [service._to_routed_model(self.db.scalar(select(ModelInstance).where(
            ModelInstance.instance_code == code))) for code in codes]
        adapter = Mock()
        adapter.chat.side_effect = [
            ModelExecutionResult(response_text=" ", response_json={}),
            ModelExecutionResult(response_text='{"overview":"有效方案"}', response_json={}),
        ]
        with patch.object(service, "list_route_options", return_value=routed), patch(
                "app.services.model_hub.get_model_adapter", return_value=adapter):
            result = service.chat(task_type="empty_real_document_test",
                messages=[{"role": "user", "content": "读取原文并返回方案"}],
                metadata_json={"require_real_model": True, "purpose": "DOCUMENT_GOVERNANCE_PLANNING"})
        self.assertEqual(result.status, "SUCCESS")
        self.assertEqual(result.instance_code, "DEEPSEEK_V4_FLASH")
        failed = self.db.scalar(select(ModelCallLog).where(
            ModelCallLog.task_type == "empty_real_document_test", ModelCallLog.instance_code == "DEEPSEEK_CHAT"))
        self.assertEqual(failed.status, "FAILED")
        self.assertIn("空响应", failed.error_message)
        self.assertEqual(adapter.chat.call_count, 2)

    def test_strict_requested_instance_never_uses_route_fallback(self) -> None:
        service = ModelHubService(self.db)
        requested = service._to_routed_model(self.db.scalar(select(ModelInstance).where(
            ModelInstance.instance_code == "DEEPSEEK_CHAT")))
        fallback = service._to_routed_model(self.db.scalar(select(ModelInstance).where(
            ModelInstance.instance_code == "MOCK_GENERAL")))
        adapter = Mock()
        adapter.chat.side_effect = RuntimeError("requested model billing failure")
        with patch.object(service, "list_route_options", return_value=[requested]) as options, patch(
                "app.services.model_hub.get_model_adapter", return_value=adapter):
            log = service.chat(
                task_type="stock_screening",
                instance_code="DEEPSEEK_CHAT",
                messages=[{"role": "user", "content": "strict"}],
                metadata_json={"strict_instance": True},
            )
        self.assertEqual(log.status, "FAILED")
        self.assertEqual(log.instance_code, "DEEPSEEK_CHAT")
        self.assertEqual(adapter.chat.call_count, 1)
        options.assert_called_once_with(
            task_type="stock_screening", requested_instance_code="DEEPSEEK_CHAT", strict_instance=True,
        )
        self.assertNotEqual(log.instance_code, fallback.instance.instance_code)

    def test_route_crud_api(self) -> None:
        from fastapi.testclient import TestClient
        from app.main import app

        def override_get_db():
            yield self.db

        app.dependency_overrides[get_db] = override_get_db
        try:
            with TestClient(app) as client:
                response = client.post(
                    "/api/v1/model-hub/routes",
                    json={
                        "task_type": "ui_test_route",
                        "preferred_instance_code": "MOCK_GENERAL",
                        "fallback_chain_json": ["DEEPSEEK_CHAT"],
                        "route_policy": "PREFERRED_THEN_FALLBACK",
                        "enabled": True,
                        "description": "前端路由管理测试",
                    },
                )
                self.assertEqual(response.status_code, 201)
                route_id = response.json()["id"]

                response = client.put(
                    f"/api/v1/model-hub/routes/{route_id}",
                    json={
                        "preferred_instance_code": "DEEPSEEK_CHAT",
                        "fallback_chain_json": ["MOCK_GENERAL"],
                        "enabled": False,
                    },
                )
                self.assertEqual(response.status_code, 200)
                self.assertEqual(response.json()["preferred_instance_code"], "DEEPSEEK_CHAT")
                self.assertEqual(response.json()["fallback_chain_json"], ["MOCK_GENERAL"])
                self.assertFalse(response.json()["enabled"])

                response = client.delete(f"/api/v1/model-hub/routes/{route_id}")
                self.assertEqual(response.status_code, 204)

                response = client.get("/api/v1/model-hub/routes")
                self.assertEqual(response.status_code, 200)
                task_types = {item["task_type"] for item in response.json()}
                self.assertNotIn("ui_test_route", task_types)
        finally:
            app.dependency_overrides.clear()

    def test_skill_crud_api(self) -> None:
        from fastapi.testclient import TestClient
        from app.main import app

        def override_get_db():
            yield self.db

        app.dependency_overrides[get_db] = override_get_db
        try:
            with TestClient(app) as client:
                response = client.post(
                    "/api/v1/model-hub/skills",
                    json={
                        "skill_code": "STOCK_RESEARCH",
                        "skill_name": "股票研究",
                        "description": "生成股票研究摘要",
                        "instructions": "先总结公司基本面，再列出风险。",
                        "enabled": True,
                        "config_json": {},
                    },
                )
                self.assertEqual(response.status_code, 201)
                skill_id = response.json()["id"]

                response = client.put(
                    f"/api/v1/model-hub/skills/{skill_id}",
                    json={"skill_name": "股票研究助手", "enabled": False},
                )
                self.assertEqual(response.status_code, 200)
                self.assertEqual(response.json()["skill_name"], "股票研究助手")
                self.assertFalse(response.json()["enabled"])

                response = client.get("/api/v1/model-hub/skills")
                self.assertEqual(response.status_code, 200)
                self.assertEqual(len(response.json()), 1)

                response = client.delete(f"/api/v1/model-hub/skills/{skill_id}")
                self.assertEqual(response.status_code, 204)

                response = client.get("/api/v1/model-hub/skills")
                self.assertEqual(response.status_code, 200)
                self.assertEqual(response.json(), [])
        finally:
            app.dependency_overrides.clear()


if __name__ == "__main__":
    unittest.main()
