from __future__ import annotations

import json
import unittest
from unittest.mock import patch

import httpx
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, select
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.main import app
from app.db.base import Base
from app.db.session import get_db
from app.models.ai_hub import ModelCallLog, ModelInstance, ModelInstanceDeletion, ModelProvider, ModelRouteRule
from app.connectors.model_adapters import GeminiRestAdapter
from app.services.model_hub import seed_default_models, sync_gemini_model_catalog
from app.services.model_errors import classify_model_error, describe_model_error


class GeminiCatalogTest(unittest.TestCase):
    def setUp(self):
        self.engine = create_engine("sqlite://", poolclass=StaticPool, connect_args={"check_same_thread": False})
        Base.metadata.create_all(self.engine)
        self.db = sessionmaker(bind=self.engine)()
        seed_default_models(self.db)

    def tearDown(self):
        app.dependency_overrides.clear()
        self.db.close()
        self.engine.dispose()

    def test_catalog_preserves_ids_custom_settings_routes_and_deletions(self):
        provider = self.db.scalar(select(ModelProvider).where(ModelProvider.provider_code == "GEMINI"))
        pro = self.db.scalar(select(ModelInstance).where(ModelInstance.instance_code == "GEMINI_PRO_25"))
        pro.model_code, pro.model_name = "gemini-3.1-pro", "Gemini 3.1 Pro（深度研究）"
        pro.max_tokens = 9123
        carbon = self.db.scalar(select(ModelInstance).where(ModelInstance.instance_code == "GEMINI_CARBON"))
        carbon.model_code = "custom-model"
        carbon.model_name = "自定义研究模型"
        deleted = self.db.scalar(select(ModelInstance).where(ModelInstance.instance_code == "GEMINI_FLASH_35"))
        self.db.delete(deleted)
        self.db.add(ModelInstanceDeletion(instance_code="GEMINI_FLASH_35"))
        route = self.db.scalar(select(ModelRouteRule).where(ModelRouteRule.task_type == "stock_analysis"))
        route.preferred_instance_code = "GEMINI_FLASH"
        route.fallback_chain_json = []
        self.db.commit()
        ids = {r.instance_code: r.id for r in self.db.scalars(select(ModelInstance))}
        for _ in range(2):
            sync_gemini_model_catalog(self.db, provider, {"GEMINI_FLASH_35"})
            self.db.commit()
        self.assertEqual(pro.model_code, "gemini-3.1-pro-preview")
        self.assertEqual(pro.max_tokens, 9123)
        self.assertEqual(pro.config_json["extra_body_json"]["generationConfig"]["thinkingConfig"], {"thinkingLevel": "LOW"})
        self.assertIsNone(pro.config_json["thinking_level"])
        self.assertEqual(carbon.model_code, "custom-model")
        self.assertEqual({r.instance_code: r.id for r in self.db.scalars(select(ModelInstance))}, ids)
        seed_default_models(self.db)
        self.assertEqual(route.preferred_instance_code, "GEMINI_FLASH")
        self.assertEqual(route.fallback_chain_json, [])
        self.assertIsNone(self.db.scalar(select(ModelInstance).where(ModelInstance.instance_code == "GEMINI_FLASH_35")))

    def test_model_health_uses_current_api_target(self):
        instance = self.db.scalar(select(ModelInstance).where(ModelInstance.instance_code == "GEMINI_FLASH_LITE"))
        instance.api_key = "not-a-real-key"
        self.db.add(ModelCallLog(instance_code=instance.instance_code, model_code="gemini-2.5-flash-lite", status="SUCCESS", task_type="model_test"))
        self.db.commit()
        app.dependency_overrides[get_db] = lambda: self.db
        client = TestClient(app)
        try:
            def read():
                response = client.get("/api/v1/model-hub/instances")
                self.assertEqual(response.status_code, 200, response.text)
                return next(row for row in response.json() if row["id"] == instance.id)

            self.assertEqual(read()["availability_status"], "UNKNOWN")
            self.db.add(ModelCallLog(instance_code=instance.instance_code, model_code=instance.model_code, status="FAILED", error_message="quota exhausted", task_type="model_test"))
            self.db.commit()
            self.assertEqual(read()["availability_status"], "BILLING")
        finally:
            client.close()


class GeminiTransportTest(unittest.TestCase):
    def instance(self, model="gemini-3.5-flash-lite", **config):
        return ModelInstance(
            provider_id=1, instance_code="TEST_GEMINI", model_code=model, model_name=model,
            api_key="private-key", api_base_url="https://generativelanguage.googleapis.com/v1beta",
            max_tokens=4096, temperature=0.2, top_p=0.9, config_json=config,
        )

    def test_native_json_analysis_uses_level_for_modern_models_and_budget_for_25(self):
        schema = {"type": "object", "properties": {"ok": {"type": "boolean"}}, "required": ["ok"]}
        for model in ("gemini-3.5-flash-lite", "gemini-3.1-pro-preview", "gemini-4-carbon", "gemini-2.5-pro"):
            with self.subTest(model=model), patch("app.connectors.model_adapters.httpx.Client") as client:
                post = client.return_value.__enter__.return_value.post
                post.return_value.json.return_value = {"candidates": [{"content": {"parts": [{"thought": True, "text": "hidden"}, {"text": '{"ok":true}'}]}, "finishReason": "STOP"}]}
                result = GeminiRestAdapter().chat(self.instance(model, thinking_level="low"), [{"role": "user", "content": "Analyze"}], metadata_json={"purpose": "STANDALONE_STOCK_ANALYST", "require_real_model": True, "json_schema_output": schema, "native_structured_output": True})
                self.assertEqual(json.loads(result.response_text), {"ok": True})
                cfg = post.call_args.kwargs["json"]["generationConfig"]
                self.assertEqual(cfg["responseMimeType"], "application/json")
                self.assertEqual(cfg["responseJsonSchema"], schema)
                self.assertEqual(cfg["thinkingConfig"], {"thinkingBudget": 1024} if "2.5" in model else {"thinkingLevel": "LOW"})
                self.assertTrue(post.call_args.args[0].endswith(f"/{model}:generateContent"))

    def test_preflight_reserves_visible_output_and_does_not_reuse_high_reasoning(self):
        with patch("app.connectors.model_adapters.httpx.Client") as client:
            post = client.return_value.__enter__.return_value.post
            post.return_value.json.return_value = {"candidates": [{"content": {"parts": [{"text": "OK"}]}, "finishReason": "STOP"}]}
            GeminiRestAdapter().chat(self.instance(thinking_level="high"), [{"role": "user", "content": "OK"}], max_tokens=64, metadata_json={"purpose": "SELECTION_MODEL_SWITCH_PREFLIGHT"})
            cfg = post.call_args.kwargs["json"]["generationConfig"]
            self.assertEqual(cfg["maxOutputTokens"], 1024)
            self.assertEqual(cfg["thinkingConfig"], {"thinkingLevel": "LOW"})

    def test_empty_thought_only_and_truncated_probes_fail(self):
        responses = [
            {"promptFeedback": {"blockReason": "SAFETY"}},
            {"candidates": [{"content": {"parts": [{"text": "reasoning", "thought": True}]}, "finishReason": "MAX_TOKENS"}]},
            {"candidates": [{"content": {"parts": [{"text": "partial"}]}, "finishReason": "MAX_TOKENS"}]},
        ]
        for response in responses:
            with self.subTest(response=response), patch("app.connectors.model_adapters.httpx.Client") as client:
                client.return_value.__enter__.return_value.post.return_value.json.return_value = response
                with self.assertRaisesRegex(ValueError, "未返回可用正文"):
                    GeminiRestAdapter().chat(self.instance(), [{"role": "user", "content": "OK"}], metadata_json={"mode": "connectivity"})

    def test_free_tier_quota_explains_api_billing_without_leaking_key(self):
        response = httpx.Response(429, json={"error": {"message": "quota exceeded private-key", "details": [{"violations": [{"quotaId": "GenerateRequestsPerDayPerProjectPerModel-FreeTier"}]}]}}, request=httpx.Request("POST", "https://example.invalid"))
        message = describe_model_error(httpx.HTTPStatusError("quota", request=response.request, response=response), "private-key")
        self.assertNotIn("private-key", message)
        self.assertIn("网页 Pro 订阅与 API 项目额度独立", message)
        self.assertIn("配额不足或已用尽", message)
        self.assertNotIn("未取得", message)
        classified = classify_model_error(message)
        self.assertEqual(classified["category"], "MODEL_BILLING")
        self.assertIn("Google AI Studio", classified["suggestion"])
