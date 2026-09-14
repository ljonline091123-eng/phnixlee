import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

from sqlalchemy import create_engine, select
from sqlalchemy.orm import sessionmaker

from app.db.base import Base
from app.db.session import get_db
from app.models.ai_hub import ModelCallLog, ModelInstance, ModelProvider, ModelRouteRule
from app.services.model_hub import ModelHubService, seed_default_models


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
