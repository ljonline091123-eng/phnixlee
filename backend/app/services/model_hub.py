from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.connectors.model_registry import get_model_adapter
from app.models.ai_hub import ModelCallLog, ModelInstance, ModelProvider, ModelRouteRule, ModelSkill
from app.services.skill_files import ensure_skill_file


@dataclass(frozen=True)
class RoutedModel:
    provider: ModelProvider
    instance: ModelInstance


class ModelHubError(RuntimeError):
    def __init__(self, message: str, call_log_id: int):
        super().__init__(message)
        self.call_log_id = call_log_id


DEFAULT_MODEL_PROVIDERS = (
    {
        "provider_code": "DEEPSEEK",
        "provider_name": "DeepSeek",
        "provider_type": "DEEPSEEK",
        "enabled": True,
        "description": "DeepSeek 最新模型，兼容 OpenAI Chat Completions API。",
        "config_json": {"api_base_url": "https://api.deepseek.com"},
    },
    {
        "provider_code": "MOCK",
        "provider_name": "Local Mock Provider",
        "provider_type": "MOCK",
        "enabled": True,
        "description": "Built-in fallback for local validation and offline development.",
        "config_json": {},
    },
)

DEFAULT_DEEPSEEK_INSTANCES = (
    {
        "instance_code": "DEEPSEEK_CHAT",
        "model_code": "deepseek-v4-pro",
        "model_name": "DeepSeek V4 Pro",
        "purpose": "GENERAL",
        "api_key": None,
        "api_base_url": "https://api.deepseek.com",
        "api_path": "chat/completions",
        "max_tokens": 4096,
        "temperature": 0.2,
        "top_p": 0.95,
        "enabled": True,
        "fallback_instance_code": None,
        "config_json": {},
        "description": "DeepSeek V4 Pro 通用对话模型，保留 DEEPSEEK_CHAT 实例代码以兼容既有配置。",
    },
    {
        "instance_code": "DEEPSEEK_V4_FLASH",
        "model_code": "deepseek-v4-flash",
        "model_name": "DeepSeek V4 Flash",
        "purpose": "GENERAL",
        "api_key": None,
        "api_base_url": "https://api.deepseek.com",
        "api_path": "chat/completions",
        "max_tokens": 4096,
        "temperature": 0.2,
        "top_p": 0.95,
        "enabled": True,
        "fallback_instance_code": None,
        "config_json": {},
        "description": "DeepSeek V4 Flash 快速通用模型。",
    },
)

DEFAULT_MODEL_INSTANCES = (
    {
        "instance_code": "MOCK_GENERAL",
        "model_code": "mock-general",
        "model_name": "Mock General Model",
        "purpose": "GENERAL",
        "api_key": None,
        "api_base_url": None,
        "api_path": None,
        "max_tokens": 4096,
        "temperature": 0.2,
        "top_p": 0.95,
        "enabled": True,
        "fallback_instance_code": None,
        "config_json": {},
        "description": "Fallback model instance that returns deterministic structured output.",
    },
)

DEFAULT_MODEL_ROUTES = (
    {
        "task_type": "general_chat",
        "preferred_instance_code": "MOCK_GENERAL",
        "fallback_chain_json": [],
        "route_policy": "PREFERRED_THEN_FALLBACK",
        "enabled": True,
        "description": "Default route for model-center smoke tests.",
    },
    {
        "task_type": "stock_analysis",
        "preferred_instance_code": "DEEPSEEK_CHAT",
        "fallback_chain_json": ["MOCK_GENERAL"],
        "route_policy": "PREFERRED_THEN_FALLBACK",
        "enabled": True,
        "description": "Stock analysis uses DeepSeek when configured and falls back to the local mock model offline.",
    },
)

DEFAULT_MODEL_SKILLS = (
    {
        "skill_code": "STOCK_TREND_ADVISOR",
        "skill_name": "股票趋势研判（DeepSeek）",
        "description": "综合新闻、公告、量价、成交量和财报，生成带风险边界的趋势研判。",
        "instructions": (
            "你是一名严谨的股票研究助理，优先使用提供的最新数据，不得编造不存在的新闻、公告、价格或财报。"
            "请按以下顺序分析：1）新闻与公告的事实、来源、时间和潜在影响；"
            "2）股价、趋势、支撑阻力、波动和成交量变化；3）财报中的收入、利润、现金流、杠杆和估值线索；"
            "4）多空情景与未来1周、1个月、3个月的趋势概率。"
            "最后给出条件化的进入/退出计划：参考入场区间、止损位、分批止盈区间、观察时间窗口和触发条件。"
            "如果数据不足，明确列出缺失项并降低结论置信度；不要把分析写成保证收益的承诺。"
            "输出必须包含：结论摘要、证据、趋势判断、进入方案、退出方案、风险与失效条件。"
        ),
        "enabled": True,
        "config_json": {
            "preferred_instance_code": "DEEPSEEK_CHAT",
            "task_type": "stock_analysis",
            "temperature": 0.2,
            "risk_disclaimer": "仅供研究参考，不构成投资建议。",
        },
    },
)


def seed_default_models(db: Session) -> None:
    provider_by_code: dict[str, ModelProvider] = {}
    for provider_config in DEFAULT_MODEL_PROVIDERS:
        provider = db.scalar(select(ModelProvider).where(ModelProvider.provider_code == provider_config["provider_code"]))
        if not provider:
            provider = ModelProvider(**provider_config)
            db.add(provider)
            db.flush()
        provider_by_code[provider.provider_code] = provider

    deepseek = provider_by_code["DEEPSEEK"]
    for instance_config in DEFAULT_DEEPSEEK_INSTANCES:
        instance = db.scalar(select(ModelInstance).where(ModelInstance.instance_code == instance_config["instance_code"]))
        if not instance:
            db.add(ModelInstance(provider_id=deepseek.id, **instance_config))
            db.flush()
            continue
        if instance.instance_code == "DEEPSEEK_CHAT" and instance.model_code in {"deepseek-chat", "deepseek-reasoner"}:
            instance.model_code = str(instance_config["model_code"])
            instance.model_name = str(instance_config["model_name"])
            instance.api_base_url = instance.api_base_url or str(instance_config["api_base_url"])
            instance.api_path = instance.api_path or str(instance_config["api_path"])
            instance.description = instance.description or str(instance_config["description"])

    provider = provider_by_code["MOCK"]

    instance = db.scalar(select(ModelInstance).where(ModelInstance.instance_code == "MOCK_GENERAL"))
    if not instance:
        db.add(
            ModelInstance(
                provider_id=provider.id,
                **DEFAULT_MODEL_INSTANCES[0],
            )
        )
        db.flush()

    existing_route_types = set(db.scalars(select(ModelRouteRule.task_type)).all())
    for route_config in DEFAULT_MODEL_ROUTES:
        route = db.scalar(select(ModelRouteRule).where(ModelRouteRule.task_type == route_config["task_type"]))
        if route is None:
            db.add(ModelRouteRule(**route_config))
            continue
        if route_config["task_type"] == "stock_analysis":
            route.preferred_instance_code = route_config["preferred_instance_code"]
            route.fallback_chain_json = route_config["fallback_chain_json"]
            route.route_policy = route_config["route_policy"]
            route.description = route_config["description"]

    db.commit()


DEFAULT_MODEL_SKILLS_V2 = (
    {
        "skill_code": "STOCK_TREND_ADVISOR",
        "skill_name": "股票趋势综合研判（DeepSeek）",
        "description": "综合分析新闻、公告、股价、成交量和财报，给出带证据、带置信度和风险边界的趋势与交易计划。",
        "instructions": (
            "你是一名严谨的股票研究助手。只使用输入中提供或明确可验证的资料，不得编造新闻、公告、价格、成交量、财报或事件日期。"
            "请按以下顺序分析：1）新闻与公告的来源、发布时间、事实要点、潜在影响和是否已被市场定价；"
            "2）股价趋势、支撑阻力、波动率、缺口、量价配合、成交量与换手率变化；"
            "3）财报中的收入、利润、现金流、负债、盈利质量、估值和同比/环比变化；"
            "4）给出未来1周、1个月、3个月的多空情景、触发条件和主观概率。"
            "最后给出条件化的进入/退出计划：参考入场区间、止损位、分批止盈区间、观察时间窗、触发条件和失效条件。"
            "如果数据不足，明确列出缺失项并降低置信度；不要把分析写成保证收益的承诺，也不要替用户做无条件的买卖决定。"
            "输出必须包含：结论摘要、关键证据、趋势判断、情景与概率、进入方案、退出方案、风险与失效条件、需要继续验证的问题。"
        ),
        "enabled": True,
        "config_json": {
            "preferred_instance_code": "DEEPSEEK_CHAT",
            "task_type": "stock_analysis",
            "temperature": 0.2,
            "risk_disclaimer": "仅供研究参考，不构成投资建议；价格和时间区间均为条件化假设。",
        },
    },
)


def seed_default_skills(db: Session) -> None:
    """Seed built-in skills without overwriting user-maintained content."""
    for skill_config in DEFAULT_MODEL_SKILLS_V2:
        skill = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == skill_config["skill_code"]))
        if skill is None:
            skill = ModelSkill(**skill_config, is_builtin=True)
            db.add(skill)
            db.flush()
        elif not skill.file_path:
            skill.is_builtin = True
        if skill.file_path:
            continue
        file_path, content_hash = ensure_skill_file(skill.skill_code, skill.instructions)
        skill.file_path = file_path
        skill.content_hash = content_hash
        skill.version = skill.version or "1.0.0"
        skill.format = "MD"
    db.commit()


class ModelHubService:
    def __init__(self, db: Session):
        self.db = db

    def list_route_options(self, task_type: str, requested_instance_code: str | None = None) -> list[RoutedModel]:
        statement = select(ModelRouteRule).where(ModelRouteRule.task_type == task_type, ModelRouteRule.enabled.is_(True))
        route = self.db.scalar(statement)
        if requested_instance_code:
            instance = self._get_instance_by_code(requested_instance_code)
            return [self._to_routed_model(instance)]
        if not route:
            return [self._load_default_mock_model()]

        candidates: list[str] = [route.preferred_instance_code, *route.fallback_chain_json]
        routed: list[RoutedModel] = []
        for code in candidates:
            try:
                instance = self._get_instance_by_code(code)
                routed.append(self._to_routed_model(instance))
            except ValueError:
                continue
        return routed or [self._load_default_mock_model()]

    def chat(
        self,
        task_type: str,
        messages: list[dict[str, str]],
        instance_code: str | None = None,
        temperature: float | None = None,
        max_tokens: int | None = None,
        metadata_json: dict[str, Any] | None = None,
    ) -> ModelCallLog:
        metadata = metadata_json or {}
        skill_code = str(metadata.get("skill_code") or "").strip()
        if skill_code:
            skill = self.db.scalar(
                select(ModelSkill).where(
                    ModelSkill.skill_code == skill_code,
                    ModelSkill.enabled.is_(True),
                )
            )
            if skill:
                messages = [
                    {"role": "system", "content": skill.instructions},
                    *messages,
                ]
        candidates = self.list_route_options(task_type=task_type, requested_instance_code=instance_code)
        last_log: ModelCallLog | None = None
        for routed in candidates:
            log = self._start_log(
                task_type=task_type,
                provider_code=routed.provider.provider_code,
                instance_code=routed.instance.instance_code,
                model_code=routed.instance.model_code,
                request_json={
                    "messages": messages,
                    "temperature": temperature,
                    "max_tokens": max_tokens,
                    "metadata_json": metadata,
                },
            )
            try:
                result = get_model_adapter(routed.provider.provider_type).chat(
                    instance=routed.instance,
                    messages=messages,
                    temperature=temperature,
                    max_tokens=max_tokens,
                    metadata_json=metadata,
                )
                log.status = "SUCCESS"
                log.response_text = result.response_text
                log.response_json = result.response_json
                log.completed_at = datetime.now(timezone.utc)
                log.latency_ms = self._compute_latency_ms(log.started_at, log.completed_at)
                self.db.commit()
                self.db.refresh(log)
                return log
            except Exception as exc:
                self.db.rollback()
                last_log = self._mark_failed(log.id, exc)

        if last_log:
            return last_log
        raise RuntimeError("No available model instance for this task")

    def test_instance(self, instance_code: str) -> ModelCallLog:
        instance = self._get_instance_by_code(instance_code)
        routed_model = self._to_routed_model(instance)
        log = self._start_log(
            task_type="model_test",
            provider_code=routed_model.provider.provider_code,
            instance_code=routed_model.instance.instance_code,
            model_code=routed_model.instance.model_code,
            request_json={"mode": "ping"},
        )
        try:
            result = get_model_adapter(routed_model.provider.provider_type).chat(
                instance=routed_model.instance,
                messages=[{"role": "user", "content": "ping"}],
                metadata_json={"mode": "ping"},
            )
            log.status = "SUCCESS"
            log.response_text = result.response_text
            log.response_json = result.response_json
            log.completed_at = datetime.now(timezone.utc)
            log.latency_ms = self._compute_latency_ms(log.started_at, log.completed_at)
            self.db.commit()
            self.db.refresh(log)
            return log
        except Exception as exc:
            return self._mark_failed(log.id, exc)

    def _start_log(
        self,
        task_type: str,
        provider_code: str | None,
        instance_code: str | None,
        model_code: str | None,
        request_json: dict[str, Any],
    ) -> ModelCallLog:
        log = ModelCallLog(
            task_type=task_type,
            provider_code=provider_code,
            instance_code=instance_code,
            model_code=model_code,
            request_json=request_json,
            status="RUNNING",
        )
        self.db.add(log)
        self.db.commit()
        self.db.refresh(log)
        return log

    def _get_instance_by_code(self, instance_code: str) -> ModelInstance:
        instance = self.db.scalar(select(ModelInstance).where(ModelInstance.instance_code == instance_code))
        if not instance:
            raise ValueError(f"Model instance not found: {instance_code}")
        if not instance.enabled:
            raise ValueError(f"Model instance is disabled: {instance_code}")
        return instance

    def _mark_failed(self, log_id: int, exc: Exception) -> ModelCallLog:
        failed_log = self.db.get(ModelCallLog, log_id)
        if not failed_log:
            raise RuntimeError(f"Model call log not found: {log_id}") from exc
        failed_log.status = "FAILED"
        failed_log.error_message = str(exc)[:4000]
        failed_log.completed_at = datetime.now(timezone.utc)
        failed_log.latency_ms = self._compute_latency_ms(failed_log.started_at, failed_log.completed_at)
        self.db.commit()
        self.db.refresh(failed_log)
        return failed_log

    def _to_routed_model(self, instance: ModelInstance) -> RoutedModel:
        provider = self.db.get(ModelProvider, instance.provider_id)
        if not provider:
            raise ValueError(f"Model provider not found for instance: {instance.instance_code}")
        if not provider.enabled:
            raise ValueError(f"Model provider is disabled: {provider.provider_code}")
        return RoutedModel(provider=provider, instance=instance)

    @staticmethod
    def _compute_latency_ms(started_at: datetime, completed_at: datetime | None) -> int | None:
        if not completed_at:
            return None
        if started_at.tzinfo is None:
            started_at = started_at.replace(tzinfo=timezone.utc)
        if completed_at.tzinfo is None:
            completed_at = completed_at.replace(tzinfo=timezone.utc)
        return max(0, int((completed_at - started_at).total_seconds() * 1000))

    def _load_default_mock_model(self) -> RoutedModel:
        instance = self._get_instance_by_code("MOCK_GENERAL")
        return self._to_routed_model(instance)
