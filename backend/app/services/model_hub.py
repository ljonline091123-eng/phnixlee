from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
import json
import os
import time
from typing import Any

import httpx
from jsonschema import Draft202012Validator, ValidationError

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.connectors.model_registry import get_model_adapter
from app.core.config import get_settings
from app.models.ai_hub import ModelCallLog, ModelInstance, ModelProvider, ModelRouteRule, ModelSkill
from app.services.core_skill_defs import core_skill_rows
from app.services.skill_registry import save_skill_content, sync_skill_from_file
from app.services.model_credentials import decrypt_api_key, encrypt_api_key
from app.services.model_errors import describe_model_error


def _normalize_bounded_json(value: Any, schema: dict[str, Any], root: dict[str, Any], path: str = "$") -> tuple[Any, list[str]]:
    """Apply only declared JSON-schema array bounds before strict validation.

    Some OpenAI-compatible providers return one extra item in a bounded list.
    Trimming that extra item is deterministic and auditable; it never invents
    fields or citations, and the repair paths are persisted with the call log.
    """
    if not isinstance(schema, dict):
        return value, []
    if "$ref" in schema:
        reference = str(schema["$ref"])
        if reference.startswith("#/$defs/"):
            schema = (root.get("$defs") or {}).get(reference.rsplit("/", 1)[-1], {})
    alternatives = schema.get("anyOf") or schema.get("oneOf")
    if alternatives:
        for alternative in alternatives:
            if alternative.get("type") == "null" and value is None:
                return value, []
            if alternative.get("type") == "object" and isinstance(value, dict):
                return _normalize_bounded_json(value, alternative, root, path)
            if alternative.get("type") == "array" and isinstance(value, list):
                return _normalize_bounded_json(value, alternative, root, path)
        return value, []
    if schema.get("type") == "object" and isinstance(value, dict):
        repairs: list[str] = []
        properties = schema.get("properties") or {}
        normalized = dict(value)
        for key, child in properties.items():
            if key in normalized:
                normalized[key], child_repairs = _normalize_bounded_json(
                    normalized[key], child, root, f"{path}.{key}"
                )
                repairs.extend(child_repairs)
        return normalized, repairs
    if schema.get("type") == "array" and isinstance(value, list):
        repairs: list[str] = []
        values = value
        maximum = schema.get("maxItems")
        if isinstance(maximum, int) and len(values) > maximum:
            values = values[:maximum]
            repairs.append(f"{path}: truncated {len(value)} items to maxItems={maximum}")
        item_schema = schema.get("items") or {}
        normalized_items: list[Any] = []
        for index, item in enumerate(values):
            normalized_item, child_repairs = _normalize_bounded_json(
                item, item_schema, root, f"{path}[{index}]"
            )
            normalized_items.append(normalized_item)
            repairs.extend(child_repairs)
        return normalized_items, repairs
    return value, []


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
        "description": "DeepSeek 付费模型，兼容 OpenAI Chat Completions API，适合作为股票研究主力模型。",
        "config_json": {
            "api_base_url": "https://api.deepseek.com",
            "billing_type": "PAID",
            "billing_label": "付费",
            "api_key_label": "DeepSeek API Key / SK",
            "api_key_hint": "在 DeepSeek 控制台创建 API Key 后填入模型实例的 SK 字段。",
        },
    },
    {
        "provider_code": "QWEN",
        "provider_name": "通义千问 / 阿里云百炼",
        "provider_type": "OPENAI_COMPAT",
        "enabled": True,
        "description": "千问模型，使用阿里云百炼 OpenAI 兼容接口；按所选地域和业务空间配置 Base URL 与 SK。",
        "config_json": {
            "api_base_url": "https://dashscope.aliyuncs.com/compatible-mode/v1",
            "billing_type": "FREE_TIER_OR_PAID",
            "billing_label": "免费额度/付费",
            "api_key_label": "DASHSCOPE_API_KEY / SK",
            "api_key_hint": "填入阿里云百炼 API Key；如使用新版地域域名，请把实例 API Base URL 改为对应 WorkspaceId 地址。",
        },
    },
    {
        "provider_code": "OPENAI",
        "provider_name": "ChatGPT / OpenAI",
        "provider_type": "OPENAI_COMPAT",
        "enabled": True,
        "description": "OpenAI / ChatGPT API 付费模型，使用 OpenAI 兼容 Chat Completions 接口。",
        "config_json": {
            "api_base_url": "https://api.openai.com/v1",
            "billing_type": "PAID",
            "billing_label": "付费",
            "api_key_label": "OPENAI_API_KEY / SK",
            "api_key_hint": "填入 OpenAI API Key，格式通常以 sk- 开头。",
        },
    },
    {
        "provider_code": "GEMINI",
        "provider_name": "Google Gemini",
        "provider_type": "GEMINI_REST",
        "enabled": True,
        "description": "Gemini API 模型，使用 Google Gemini generateContent REST 接口。",
        "config_json": {
            "api_base_url": "https://generativelanguage.googleapis.com/v1beta",
            "billing_type": "FREE_TIER_OR_PAID",
            "billing_label": "免费额度/付费",
            "api_key_label": "GEMINI_API_KEY / SK",
            "api_key_hint": "填入 Google AI Studio 或 Google Cloud 的 Gemini API Key。",
        },
    },
    {
        "provider_code": "ANTHROPIC",
        "provider_name": "Claude / Anthropic",
        "provider_type": "ANTHROPIC",
        "enabled": True,
        "description": "Claude Messages API for multi-step Agent coordination and review.",
        "config_json": {
            "api_base_url": "https://api.anthropic.com/v1",
            "billing_type": "PAID",
            "billing_label": "付费",
            "api_key_label": "Anthropic API Key",
        },
    },
    {
        "provider_code": "MOCK",
        "provider_name": "Local Mock Provider",
        "provider_type": "MOCK",
        "enabled": True,
        "description": "本地免费模拟模型，用于未配置外部 SK 时的离线验证和兜底。",
        "config_json": {
            "billing_type": "FREE",
            "billing_label": "免费",
            "api_key_label": "无需 SK",
        },
    },
)

DEFAULT_MODEL_INSTANCES = (
    {
        "provider_code": "DEEPSEEK",
        "instance_code": "DEEPSEEK_CHAT",
        "model_code": "deepseek-v4-pro",
        "model_name": "DeepSeek V4 Pro",
        "purpose": "GENERAL,RESEARCH,STOCK_ANALYSIS",
        "api_key": None,
        "api_base_url": "https://api.deepseek.com",
        "api_path": "chat/completions",
        "max_tokens": 6000,
        "temperature": 0.2,
        "top_p": 0.95,
        "enabled": True,
        "fallback_instance_code": "GEMINI_PRO",
        "config_json": {"billing_label": "付费", "billing_type": "PAID"},
        "description": "股票研究主力实例；保留 DEEPSEEK_CHAT 实例代码以兼容既有路由。",
    },
    {
        "provider_code": "DEEPSEEK",
        "instance_code": "DEEPSEEK_V4_FLASH",
        "model_code": "deepseek-v4-flash",
        "model_name": "DeepSeek V4 Flash",
        "purpose": "GENERAL,QA_QUERY",
        "api_key": None,
        "api_base_url": "https://api.deepseek.com",
        "api_path": "chat/completions",
        "max_tokens": 4096,
        "temperature": 0.2,
        "top_p": 0.95,
        "enabled": True,
        "fallback_instance_code": "GEMINI_FLASH",
        "config_json": {"billing_label": "付费", "billing_type": "PAID"},
        "description": "快速问答、问数和常规分析实例。",
    },
    {
        "provider_code": "QWEN",
        "instance_code": "QWEN_PLUS",
        "model_code": "qwen-plus",
        "model_name": "通义千问 Plus",
        "purpose": "GENERAL,QA_QUERY,DATA_GOVERNANCE",
        "api_key": None,
        "api_base_url": "https://dashscope.aliyuncs.com/compatible-mode/v1",
        "api_path": "chat/completions",
        "max_tokens": 6000,
        "temperature": 0.2,
        "top_p": 0.9,
        "enabled": True,
        "fallback_instance_code": "GEMINI_FLASH",
        "config_json": {"billing_label": "免费额度/付费", "billing_type": "FREE_TIER_OR_PAID"},
        "description": "千问 OpenAI 兼容接口实例；适合中文数据治理、问答问数和低成本研判。",
    },
    {
        "provider_code": "OPENAI",
        "instance_code": "OPENAI_CHATGPT",
        "model_code": "gpt-5.6-luna",
        "model_name": "ChatGPT / OpenAI GPT-5.6 Luna",
        "purpose": "GENERAL,RESEARCH,STOCK_ANALYSIS",
        "api_key": None,
        "api_base_url": "https://api.openai.com/v1",
        "api_path": "chat/completions",
        "max_tokens": 6000,
        "temperature": 0.2,
        "top_p": 0.9,
        "enabled": True,
        "fallback_instance_code": "MOCK_GENERAL",
        "config_json": {"billing_label": "付费", "billing_type": "PAID"},
        "description": "ChatGPT / OpenAI 通用研究实例，可用于选股分析、报告润色和复杂问答。",
    },
    {
        "provider_code": "GEMINI",
        "instance_code": "GEMINI_FLASH",
        "model_code": "gemini-3.6-flash",
        "model_name": "Gemini 3.6 Flash",
        "purpose": "GENERAL,RESEARCH,KNOWLEDGE_GRAPH",
        "api_key": None,
        "api_base_url": "https://generativelanguage.googleapis.com/v1beta",
        "api_path": None,
        "max_tokens": 6000,
        "temperature": 0.2,
        "top_p": 0.9,
        "enabled": True,
        "fallback_instance_code": "MOCK_GENERAL",
        "config_json": {"billing_label": "免费额度/付费", "billing_type": "FREE_TIER_OR_PAID"},
        "description": "Gemini REST 实例，适合长文本治理、知识图谱摘要和多源信息归纳。",
    },
    {
        "provider_code": "GEMINI",
        "instance_code": "GEMINI_PRO",
        "model_code": "gemini-2.5-pro",
        "model_name": "Gemini 2.5 Pro",
        "purpose": "DEEP_RESEARCH,LONG_REPORT,MULTIMODAL",
        "api_key": None,
        "api_base_url": "https://generativelanguage.googleapis.com/v1beta",
        "api_path": None,
        "max_tokens": 8192,
        "temperature": 0.2,
        "top_p": 0.9,
        "enabled": True,
        "fallback_instance_code": "GEMINI_FLASH",
        "config_json": {"billing_label": "免费额度/付费", "billing_type": "FREE_TIER_OR_PAID"},
        "description": "Long financial reports and chart or document interpretation.",
    },
    {
        "provider_code": "ANTHROPIC",
        "instance_code": "CLAUDE_SONNET",
        "model_code": "claude-sonnet-5",
        "model_name": "Claude Sonnet 5",
        "purpose": "MULTI_STEP_AGENT,META_REVIEW",
        "api_key": None,
        "api_base_url": "https://api.anthropic.com/v1",
        "api_path": "messages",
        "max_tokens": 6000,
        "temperature": 1.0,
        "top_p": 1.0,
        "enabled": True,
        "fallback_instance_code": "OPENAI_CHATGPT",
        "config_json": {"billing_label": "付费", "billing_type": "PAID"},
        "description": "Claude Messages instance for multi-step reasoning.",
    },
    {
        "provider_code": "MOCK",
        "instance_code": "MOCK_GENERAL",
        "model_code": "mock-general",
        "model_name": "Mock General Model",
        "purpose": "GENERAL,OFFLINE_FALLBACK",
        "api_key": None,
        "api_base_url": None,
        "api_path": None,
        "max_tokens": 4096,
        "temperature": 0.2,
        "top_p": 0.95,
        "enabled": True,
        "fallback_instance_code": None,
        "config_json": {"billing_label": "免费", "billing_type": "FREE"},
        "description": "本地免费兜底模型，确保无外部 API Key 时系统可运行和测试。",
    },
)

DEFAULT_MODEL_ROUTES = (
    {
        "task_type": "data_distillation",
        "preferred_instance_code": "DEEPSEEK_CHAT",
        "fallback_chain_json": ["GEMINI_FLASH"],
        "route_policy": "PREFERRED_THEN_FALLBACK",
        "enabled": False,
        "description": "单文档双轨蒸馏；完成任务执行器及真实模型连通验证后启用。",
    },
    {
        "task_type": "deep_research",
        "preferred_instance_code": "GEMINI_PRO",
        "fallback_chain_json": ["DEEPSEEK_CHAT", "GEMINI_FLASH", "MOCK_GENERAL"],
        "route_policy": "PREFERRED_THEN_FALLBACK",
        "enabled": True,
        "description": "Long report reading and research synthesis after deterministic screening.",
    },
    {
        "task_type": "meta_review",
        "preferred_instance_code": "GEMINI_PRO",
        "fallback_chain_json": ["DEEPSEEK_CHAT", "GEMINI_FLASH", "MOCK_GENERAL"],
        "route_policy": "PREFERRED_THEN_FALLBACK",
        "enabled": True,
        "description": "Review repeated failed predictions and propose a Skill revision for approval.",
    },
    {
        "task_type": "general_chat",
        "preferred_instance_code": "DEEPSEEK_V4_FLASH",
        "fallback_chain_json": ["GEMINI_FLASH", "MOCK_GENERAL"],
        "route_policy": "PREFERRED_THEN_FALLBACK",
        "enabled": True,
        "description": "通用对话优先使用 DeepSeek V4 Flash，失败时依次降级到 Gemini Flash、本地 Mock。",
    },
    {
        "task_type": "model_lab_chat",
        "preferred_instance_code": "DEEPSEEK_V4_FLASH",
        "fallback_chain_json": ["GEMINI_FLASH", "MOCK_GENERAL"],
        "route_policy": "PREFERRED_THEN_FALLBACK",
        "enabled": True,
        "description": "模型实验室手工问答和连通性验证路由。",
    },
    {
        "task_type": "data_governance",
        "preferred_instance_code": "DEEPSEEK_V4_FLASH",
        # Document governance must have more than one real provider.  The
        # local mock remains in the catalog for offline development, but is
        # filtered out whenever a caller requires a real model.
        "fallback_chain_json": ["QWEN_PLUS", "GEMINI_FLASH", "DEEPSEEK_CHAT", "OPENAI_CHATGPT", "MOCK_GENERAL"],
        "route_policy": "PREFERRED_THEN_FALLBACK",
        "enabled": True,
        "description": "数据治理、分类入湖、DW 层质量检查与字段解释路由。",
    },
    {
        "task_type": "knowledge_graph",
        "preferred_instance_code": "DEEPSEEK_V4_FLASH",
        "fallback_chain_json": ["GEMINI_PRO", "MOCK_GENERAL"],
        "route_policy": "PREFERRED_THEN_FALLBACK",
        "enabled": True,
        "description": "单股知识图谱构建、实体关系抽取与证据归档路由。",
    },
    {
        "task_type": "qa_query",
        "preferred_instance_code": "DEEPSEEK_V4_FLASH",
        "fallback_chain_json": ["GEMINI_FLASH", "MOCK_GENERAL"],
        "route_policy": "PREFERRED_THEN_FALLBACK",
        "enabled": True,
        "description": "研究中心对话页签的问答、问数、治理和分析路由。",
    },
    {
        "task_type": "stock_screening",
        "preferred_instance_code": "DEEPSEEK_CHAT",
        "fallback_chain_json": ["GEMINI_PRO", "MOCK_GENERAL"],
        "route_policy": "PREFERRED_THEN_FALLBACK",
        "enabled": True,
        "description": "分析选股、基本面和量价策略研判路由。",
    },
    {
        "task_type": "stock_analysis",
        "preferred_instance_code": "DEEPSEEK_CHAT",
        "fallback_chain_json": ["GEMINI_PRO", "MOCK_GENERAL"],
        "route_policy": "PREFERRED_THEN_FALLBACK",
        "enabled": True,
        "description": "股票分析优先使用 DeepSeek V4 Pro，失败时依次降级到 Gemini 2.5 Pro、本地 Mock。",
    },
    {
        "task_type": "research_report",
        "preferred_instance_code": "DEEPSEEK_CHAT",
        "fallback_chain_json": ["GEMINI_PRO", "MOCK_GENERAL"],
        "route_policy": "PREFERRED_THEN_FALLBACK",
        "enabled": True,
        "description": "历史研报对比、结论复盘和报告生成路由。",
    },
    {
        "task_type": "risk_warning",
        "preferred_instance_code": "DEEPSEEK_V4_FLASH",
        "fallback_chain_json": ["GEMINI_FLASH", "MOCK_GENERAL"],
        "route_policy": "PREFERRED_THEN_FALLBACK",
        "enabled": True,
        "description": "预警判断、负面公告识别、量价失效条件和风险提醒路由。",
    },
)


def _runtime_api_key(provider: ModelProvider) -> str | None:
    """Resolve an optional process-level key without writing it to the DB."""
    if provider.provider_type != "GEMINI_REST" and provider.provider_code != "GEMINI":
        return None
    settings = get_settings()
    return settings.gemini_api_key.strip() or settings.google_api_key.strip() or None

# Upgrade only routes that still match the previous built-in profile. Operators'
# customized routes must remain untouched across application restarts.
LEGACY_DEFAULT_ROUTE_OPTIONS = {
    # The first tuple is the previous built-in route.  Only this known
    # profile is upgraded; an operator-customized route is left untouched.
    "data_governance": ("DEEPSEEK_V4_FLASH", ("GEMINI_FLASH", "MOCK_GENERAL")),
    "deep_research": ("GEMINI_FLASH", ("OPENAI_CHATGPT", "DEEPSEEK_CHAT", "MOCK_GENERAL")),
    "general_chat": ("QWEN_PLUS", ("DEEPSEEK_V4_FLASH", "GEMINI_FLASH", "OPENAI_CHATGPT", "MOCK_GENERAL")),
    "knowledge_graph": ("GEMINI_FLASH", ("QWEN_PLUS", "DEEPSEEK_CHAT", "OPENAI_CHATGPT", "MOCK_GENERAL")),
    "meta_review": ("OPENAI_CHATGPT", ("DEEPSEEK_CHAT", "QWEN_PLUS", "MOCK_GENERAL")),
    "model_lab_chat": ("QWEN_PLUS", ("DEEPSEEK_V4_FLASH", "GEMINI_FLASH", "OPENAI_CHATGPT", "MOCK_GENERAL")),
    "qa_query": ("QWEN_PLUS", ("DEEPSEEK_V4_FLASH", "GEMINI_FLASH", "OPENAI_CHATGPT", "MOCK_GENERAL")),
    "research_report": ("DEEPSEEK_CHAT", ("QWEN_PLUS", "GEMINI_FLASH", "OPENAI_CHATGPT", "MOCK_GENERAL")),
    "risk_warning": ("DEEPSEEK_CHAT", ("QWEN_PLUS", "GEMINI_FLASH", "OPENAI_CHATGPT", "MOCK_GENERAL")),
    "stock_analysis": ("DEEPSEEK_CHAT", ("QWEN_PLUS", "GEMINI_FLASH", "OPENAI_CHATGPT", "MOCK_GENERAL")),
    "stock_screening": ("DEEPSEEK_CHAT", ("QWEN_PLUS", "GEMINI_FLASH", "OPENAI_CHATGPT", "MOCK_GENERAL")),
}

LEGACY_DEFAULT_INSTANCE_FALLBACKS = {
    "DEEPSEEK_CHAT": "QWEN_PLUS",
    "DEEPSEEK_V4_FLASH": "MOCK_GENERAL",
}


def _damaged_text(value: object) -> bool:
    return isinstance(value, str) and ("\ufffd" in value or "??" in value)


def _repair_default_text(current: dict[str, Any], defaults: dict[str, Any]) -> dict[str, Any]:
    repaired = dict(current)
    for key, default in defaults.items():
        value = repaired.get(key)
        if _damaged_text(value) and isinstance(default, str):
            repaired[key] = default
    return repaired


def seed_default_models(db: Session) -> None:
    provider_by_code: dict[str, ModelProvider] = {}
    for provider_config in DEFAULT_MODEL_PROVIDERS:
        provider = db.scalar(select(ModelProvider).where(ModelProvider.provider_code == provider_config["provider_code"]))
        if not provider:
            provider = ModelProvider(**provider_config)
            db.add(provider)
            db.flush()
        else:
            if not provider.provider_name or _damaged_text(provider.provider_name):
                provider.provider_name = str(provider_config["provider_name"])
            provider.provider_type = str(provider_config["provider_type"])
            if not provider.description or _damaged_text(provider.description):
                provider.description = str(provider_config.get("description") or "")
            provider.config_json = _repair_default_text(
                {**dict(provider_config.get("config_json") or {}), **dict(provider.config_json or {})},
                dict(provider_config.get("config_json") or {}),
            )
        provider_by_code[provider.provider_code] = provider
        if not provider.api_base_url:
            provider.api_base_url = (provider.config_json or {}).get("api_base_url")

    for raw_config in DEFAULT_MODEL_INSTANCES:
        instance_config = dict(raw_config)
        provider_code = str(instance_config.pop("provider_code"))
        provider = provider_by_code[provider_code]
        instance = db.scalar(select(ModelInstance).where(ModelInstance.instance_code == instance_config["instance_code"]))
        if not instance:
            db.add(ModelInstance(provider_id=provider.id, **instance_config))
            db.flush()
            continue
        instance.provider_id = provider.id
        if instance.fallback_instance_code == LEGACY_DEFAULT_INSTANCE_FALLBACKS.get(instance.instance_code):
            instance.fallback_instance_code = instance_config["fallback_instance_code"]
        if instance.instance_code == "DEEPSEEK_CHAT" and instance.model_code in {"deepseek-chat", "deepseek-reasoner"}:
            instance.model_code = str(instance_config["model_code"])
            instance.model_name = str(instance_config["model_name"])
        for field_name in ("api_base_url", "api_path", "fallback_instance_code", "description"):
            if getattr(instance, field_name) in (None, "") or _damaged_text(getattr(instance, field_name)):
                setattr(instance, field_name, instance_config.get(field_name))
        instance.purpose = instance.purpose or str(instance_config.get("purpose") or "GENERAL")
        instance.max_tokens = instance.max_tokens or int(instance_config.get("max_tokens") or 4096)
        instance.temperature = instance.temperature if instance.temperature is not None else float(instance_config.get("temperature") or 0.2)
        instance.top_p = instance.top_p if instance.top_p is not None else float(instance_config.get("top_p") or 0.95)
        instance.config_json = _repair_default_text(
            {**dict(instance_config.get("config_json") or {}), **dict(instance.config_json or {})},
            dict(instance_config.get("config_json") or {}),
        )

    if os.environ.get("MODEL_CREDENTIAL_KEY") or get_settings().model_credential_key:
        for provider in provider_by_code.values():
            instances = list(db.scalars(select(ModelInstance).where(ModelInstance.provider_id == provider.id)).all())
            legacy_keys = {item.api_key for item in instances if item.api_key}
            if not provider.api_key_encrypted and len(legacy_keys) == 1:
                provider.api_key_encrypted = encrypt_api_key(next(iter(legacy_keys)))
            if provider.api_key_encrypted:
                shared_key = decrypt_api_key(provider.api_key_encrypted)
                for item in instances:
                    if item.api_key == shared_key:
                        item.api_key = None

    for route_config in DEFAULT_MODEL_ROUTES:
        route = db.scalar(select(ModelRouteRule).where(ModelRouteRule.task_type == route_config["task_type"]))
        if route is None:
            db.add(ModelRouteRule(**route_config))
            continue
        if (route.preferred_instance_code, tuple(route.fallback_chain_json or ())) == LEGACY_DEFAULT_ROUTE_OPTIONS.get(route.task_type):
            route.preferred_instance_code = route_config["preferred_instance_code"]
            route.fallback_chain_json = route_config["fallback_chain_json"]
            route.description = route_config["description"]
        if not route.preferred_instance_code:
            route.preferred_instance_code = route_config["preferred_instance_code"]
        if route.fallback_chain_json is None:
            route.fallback_chain_json = route_config["fallback_chain_json"]
        if not route.route_policy:
            route.route_policy = route_config["route_policy"]
        if not route.description or _damaged_text(route.description):
            route.description = route_config["description"]

    db.commit()


DEFAULT_MODEL_SKILLS_V2 = (
    {
        "skill_code": "STOCK_TREND_ADVISOR",
        "skill_name": "股票趋势综合研判",
        "description": "综合新闻、公告、股价、成交量和财报，给出带证据、置信度和风险边界的趋势与交易计划。",
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
    {
        "skill_code": "DATA_GOVERNANCE_DW",
        "skill_name": "股票数据治理与 DW 分层",
        "description": "把股票基础资料、新闻、公告、财报、股价、交易量、股东和 F10 数据归类成可追溯 DW 数据层。",
        "instructions": (
            "你是股票数据治理智能体。输入可能包含来自接口、数据库表、新闻、公告、财报、行情和 F10 的混合数据。"
            "必须先识别数据类别：基础数据、新闻、公告、财报、股价、交易量、股东、业务/F10、研究报告、同步日志。"
            "为每类数据输出目标 DW 层、主键、时间字段、来源、质量问题、去重规则、缺失字段和后续治理动作。"
            "不得改变事实值，不得把猜测写入 DW 层。无法确认的数据标为 PENDING_REVIEW。"
            "输出格式固定为：分类结果、字段映射、质量校验、入库建议、风险与待补数据。"
        ),
        "enabled": True,
        "config_json": {"task_type": "data_governance", "preferred_instance_code": "QWEN_PLUS"},
    },
    {
        "skill_code": "STOCK_KNOWLEDGE_GRAPH_BUILDER",
        "skill_name": "单股全覆盖知识图谱构建",
        "description": "以单只股票为对象，抽取公司、行业、股东、财报、公告、新闻、价格、成交量和研报结论的实体关系。",
        "instructions": (
            "你是股票知识图谱构建智能体。围绕一只股票建立全覆盖知识图谱，所有实体和关系都必须能追溯到输入证据。"
            "实体类型至少包括：STOCK、COMPANY、INDUSTRY、SHAREHOLDER、FINANCIAL_REPORT、NOTICE、NEWS、PRICE_SERIES、VOLUME_SERIES、BUSINESS_SEGMENT、RESEARCH_REPORT。"
            "关系类型至少包括：HAS_BASIC_PROFILE、HAS_SHAREHOLDER、HAS_FINANCIAL_REPORT、HAS_NOTICE、HAS_NEWS、HAS_PRICE_SERIES、HAS_VOLUME_SERIES、HAS_BUSINESS_SEGMENT、HAS_RESEARCH_REPORT、IMPACTS、SUPPORTS、CONFLICTS_WITH。"
            "输出时给出实体清单、关系清单、证据来源、冲突事实和缺失项，不得生成无证据关系。"
        ),
        "enabled": True,
        "config_json": {"task_type": "knowledge_graph", "preferred_instance_code": "GEMINI_FLASH"},
    },
    {
        "skill_code": "STOCK_QA_QUERY",
        "skill_name": "股票问答问数",
        "description": "面向研究中心对话页签，读取内部数据源和知识图谱，回答治理、问数、分析和问答请求。",
        "instructions": (
            "你是股票问答问数智能体。回答前先判断用户问题属于数据治理、统计问数、事实问答、研判分析还是预警解释。"
            "优先引用内部数据源和知识图谱中的事实；如果缺少数据，明确说明缺失表、缺失字段或缺失时间段。"
            "问数类问题要给出统计口径、过滤条件和结果含义；分析类问题要区分事实、推断和需要验证的假设。"
            "输出应简洁、可执行，必要时给出下一步数据治理或分析动作。"
        ),
        "enabled": True,
        "config_json": {"task_type": "qa_query", "preferred_instance_code": "QWEN_PLUS"},
    },
    {
        "skill_code": "STOCK_SELECTION_ANALYST",
        "skill_name": "分析选股与短中长线研判",
        "description": "结合内部数据源、知识图谱和外部信息，分析基本面、业务布局、发展前景、经营现状和量价操作，给出短中长线结论。",
        "instructions": (
            "你是资深股票研究分析师。必须从基本面、业务布局、发展前景、经营现状、量价结构、成交量、资金行为、新闻公告和股东变化进行综合判断。"
            "结论要覆盖短线（1周）、中线（1个月）、长线（3个月以上）三个周期，并说明适用条件、失效条件和跟踪指标。"
            "读取内部数据源和知识图谱时，优先使用带日期、来源和可追溯证据的数据。外部信息只能作为补充，并要标明来源或说明尚未接入。"
            "输出必须包括：核心结论、证据链、看多/看空因素、短中长线策略、预警信号、需要补充的数据。"
        ),
        "enabled": True,
        "config_json": {"task_type": "stock_screening", "preferred_instance_code": "DEEPSEEK_CHAT"},
    },
    {
        "skill_code": "STOCK_EVENT_LINKAGE_ANALYST",
        "skill_name": "新闻公告事件联动与兑现验证",
        "description": "在指定自然月窗口内联合分析新闻和公告，建立事件时间线、前后关系、兑现验证及可解释置信度。",
        "instructions": (
            "你是证券事件联动分析智能体。仅使用本次白名单内、处于分析周期内的新闻公告正文和结构化事实。"
            "先按主体、事项和时间归一化事件，再识别后续跟进、相互验证、冲突、利好兑现或未兑现、股东高管与解禁时间关联、关联证券影响。"
            "高管辞职与解禁临近只能形成待验证的时间关联线索，除非原文明确说明，不得推断减持动机。"
            "同源转载不得当作多份独立证据；来源可靠性、模型抽取置信度和最终置信度必须分开。"
            "所有事件与联动必须引用文档ID及连续原文引句；无法核实则返回UNVERIFIED或INSUFFICIENT。"
            "输出包括EVENT_TIMELINE、EVENT_LINKS、VALIDATION_FINDINGS和CONFIDENCE_BREAKDOWN。"
        ),
        "enabled": True,
        "config_json": {"task_type": "stock_screening", "preferred_instance_code": "DEEPSEEK_CHAT",
                        "managed_contract_version": "EVENT_LINKAGE_V1"},
    },
    {
        "skill_code": "RESEARCH_REPORT_REVIEW",
        "skill_name": "研报历史复盘与修正",
        "description": "生成新研报时对比历史研报，评估历史预测是否有效，并提出需要调整的判断。",
        "instructions": (
            "你是研报复盘智能体。输入包含当前研报证据和历史研报摘要时，必须评估历史结论是否被后续价格、成交量、公告、财报或新闻验证。"
            "复盘要输出：历史结论、当前验证结果、预测偏差、偏差原因、需要上调或下调的判断、下一次跟踪指标。"
            "不得因为单一短期价格波动直接判定全部预测正确或错误，要结合历史研报的时间、评级、分数、支撑阻力和风险提示。"
        ),
        "enabled": True,
        "config_json": {"task_type": "research_report", "preferred_instance_code": "DEEPSEEK_CHAT"},
    },
)


GENERIC_PROMPT_INPUT_CONTRACT: dict[str, Any] = {
    "type": "object",
    "properties": {
        "task_type": {"type": "string", "maxLength": 64},
        "messages": {
            "type": "array",
            "maxItems": 200,
            "items": {
                "type": "object",
                "required": ["role", "content"],
                "properties": {
                    "role": {"type": "string"},
                    "content": {"type": "string"},
                },
            },
        },
        "context": {"type": "object"},
        "evidence_ids": {
            "type": "array",
            "maxItems": 500,
            "items": {"type": "string", "maxLength": 256},
        },
    },
    "additionalProperties": True,
}

GENERIC_PROMPT_OUTPUT_CONTRACT: dict[str, Any] = {"type": ["object", "string"]}

GENERIC_READ_ONLY_PERMISSION_POLICY: dict[str, Any] = {
    "arbitrary_sql": False,
    "database_write": False,
    "unrestricted_database_write": False,
    "allowed_operations": ["READ_GOVERNED_DATA", "RETURN_EVIDENCE_LINKED_RESULT"],
    "allowed_services": [],
}

GENERIC_RETRY_POLICY: dict[str, Any] = {
    "max_attempts": 1,
    "retry_on": ["TimeoutError", "RuntimeError"],
}

GENERIC_ERROR_POLICY: dict[str, Any] = {
    "on_error": "FAIL_CLOSED",
    "retain_audit": True,
    "expose_internal_error": False,
}


def seed_default_skills(db: Session) -> None:
    """Seed built-in skills without overwriting user-maintained content."""
    for skill_config in (*DEFAULT_MODEL_SKILLS_V2, *core_skill_rows()):
        managed_contract_changed = False
        skill = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == skill_config["skill_code"]))
        if skill is None:
            skill = ModelSkill(**skill_config, is_builtin=True)
            db.add(skill)
            db.flush()
        elif not skill.file_path:
            skill.is_builtin = True
        if skill.is_builtin:
            for field_name in ("skill_name", "description", "instructions"):
                if _damaged_text(getattr(skill, field_name)):
                    setattr(skill, field_name, skill_config[field_name])
            skill.config_json = _repair_default_text(
                dict(skill.config_json or {}), dict(skill_config.get("config_json") or {})
            )
            for field_name, default_value in (
                ("input_contract_json", skill_config.get("input_contract_json") or GENERIC_PROMPT_INPUT_CONTRACT),
                ("output_contract_json", skill_config.get("output_contract_json") or GENERIC_PROMPT_OUTPUT_CONTRACT),
                ("permission_policy_json", skill_config.get("permission_policy_json") or GENERIC_READ_ONLY_PERMISSION_POLICY),
                ("retry_policy_json", skill_config.get("retry_policy_json") or GENERIC_RETRY_POLICY),
                ("error_policy_json", skill_config.get("error_policy_json") or GENERIC_ERROR_POLICY),
            ):
                if not getattr(skill, field_name, None):
                    setattr(skill, field_name, default_value)
            if getattr(skill, "lifecycle_status", "DRAFT") == "DRAFT" and skill.enabled:
                skill.lifecycle_status = "ENABLED"
            if not getattr(skill, "side_effect_level", None):
                skill.side_effect_level = skill_config.get("side_effect_level", "READ_ONLY")
            if not getattr(skill, "idempotency_policy", None):
                skill.idempotency_policy = skill_config.get("idempotency_policy", "OPTIONAL")
            # A versioned built-in contract is migrated once and then tracked in
            # the Skill revision.  Unversioned/user-maintained contracts retain
            # the existing non-overwrite behaviour above.
            desired_contract = (skill_config.get("config_json") or {}).get("managed_contract_version")
            current_contract = (skill.config_json or {}).get("managed_contract_version")
            if desired_contract and desired_contract != current_contract:
                managed_contract_changed = True
                for field_name in (
                    "input_contract_json", "output_contract_json", "permission_policy_json",
                    "retry_policy_json", "error_policy_json", "side_effect_level", "idempotency_policy",
                ):
                    setattr(skill, field_name, skill_config[field_name])
                skill.config_json = {**dict(skill.config_json or {}), **dict(skill_config.get("config_json") or {})}
        file_changed = sync_skill_from_file(db, skill)
        if managed_contract_changed and not file_changed:
            save_skill_content(db, skill, skill.instructions, source="CONTRACT", force_revision=True)
        skill.version = skill.version or "1.0.0"
        skill.format = "MD"

    # Historical and user-created Skills can predate the governance columns.
    # Fill only missing values so explicit user policies remain authoritative.
    for skill in db.scalars(select(ModelSkill)).all():
        if not skill.input_contract_json:
            skill.input_contract_json = dict(GENERIC_PROMPT_INPUT_CONTRACT)
        if not skill.output_contract_json:
            skill.output_contract_json = dict(GENERIC_PROMPT_OUTPUT_CONTRACT)
        if not skill.permission_policy_json:
            skill.permission_policy_json = dict(GENERIC_READ_ONLY_PERMISSION_POLICY)
        if not skill.retry_policy_json:
            skill.retry_policy_json = dict(GENERIC_RETRY_POLICY)
        if not skill.error_policy_json:
            skill.error_policy_json = dict(GENERIC_ERROR_POLICY)
        skill.version = skill.version or "1.0.0"
        skill.side_effect_level = skill.side_effect_level or "READ_ONLY"
        skill.idempotency_policy = skill.idempotency_policy or "NONE"
    db.commit()


class ModelHubService:
    # Retry only transient provider outages. Contract and authentication
    # failures should move to the next configured provider immediately.
    _MAX_TRANSIENT_ATTEMPTS = 2
    _TRANSIENT_BACKOFF_SECONDS = 0.25

    def __init__(self, db: Session):
        self.db = db

    def list_route_options(
        self,
        task_type: str,
        requested_instance_code: str | None = None,
        *,
        strict_instance: bool = False,
    ) -> list[RoutedModel]:
        statement = select(ModelRouteRule).where(ModelRouteRule.task_type == task_type, ModelRouteRule.enabled.is_(True))
        route = self.db.scalar(statement)
        if requested_instance_code:
            requested = self._get_instance_by_code(requested_instance_code)
            # An explicitly selected model is a user decision.  It must not
            # silently fall through to the task route after an expired key,
            # billing suspension, or exhausted quota.  The caller can choose
            # another instance through the model-switch workflow instead.
            if strict_instance:
                return [self._to_routed_model(requested)]
        if requested_instance_code and not route:
            return [self._to_routed_model(self._get_instance_by_code(requested_instance_code))]
        if not route and not requested_instance_code:
            return [self._load_default_mock_model()]
        candidates: list[str] = list(dict.fromkeys([
            *([requested_instance_code] if requested_instance_code else []),
            *([route.preferred_instance_code, *(route.fallback_chain_json or [])] if route else []),
        ]))
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
        if task_type in {"stock_screening", "stock_analysis", "risk_warning", "data_governance"}:
            if int(metadata.get("candidate_count") or 0) > 50:
                raise ValueError("Soft-analysis candidate pool cannot exceed 50 stocks")
        requested_skills = metadata.get("skill_codes") or [metadata.get("skill_code")]
        skill_codes = list(dict.fromkeys(
            str(code).strip() for code in requested_skills if str(code or "").strip()
        ))
        applied_skills: list[dict[str, str]] = []
        skill_prompts: list[dict[str, str]] = []
        for skill_code in skill_codes:
            skill = self.db.scalar(
                select(ModelSkill).where(
                    ModelSkill.skill_code == skill_code,
                    ModelSkill.enabled.is_(True),
                )
            )
            if skill is None:
                if metadata.get("require_skills"):
                    raise ValueError(f"Requested enabled Skill was not found: {skill_code}")
                continue
            if skill.skill_type == "PROMPT_SOP":
                sync_skill_from_file(self.db, skill)
                self.db.flush()
                skill_prompts.append({"role": "system", "content": skill.instructions})
            applied_skills.append({"skill_code": skill.skill_code, "skill_version": skill.version})
        if applied_skills:
            self.db.commit()
            metadata = {**metadata, "skill_codes": skill_codes, "skill_chain": applied_skills}
            messages = [*skill_prompts, *messages]
        # Enforce the limit on the exact prompt sent to the provider, including
        # any enabled Skill instructions added above.
        if task_type in {"stock_screening", "stock_analysis", "risk_warning", "data_governance"}:
            if sum(len(item.get("content", "")) for item in messages) > 60_000:
                raise ValueError("Soft-analysis input exceeds 60,000 characters after skill instructions; run Python/SQL filtering first")
        candidates = self.list_route_options(
            task_type=task_type,
            requested_instance_code=instance_code,
            strict_instance=bool(metadata.get("strict_instance")),
        )
        if metadata.get("require_real_model"):
            candidates = [
                candidate for candidate in candidates
                if candidate.provider.provider_type != "MOCK"
            ]
            if not candidates:
                raise ValueError("AI治理要求真实模型；请检查模型启用状态和 MODEL_CREDENTIAL_KEY，不能使用 Mock 替代。")
        last_log: ModelCallLog | None = None
        attempted_models: list[dict[str, Any]] = []
        stop_after_failure = False
        for routed in candidates:
            # Materialize provider values before _start_log commits. The model
            # instance is already a transient snapshot, so the outbound call
            # cannot implicitly open a database transaction by lazy-loading an
            # expired ORM row.
            provider_code = routed.provider.provider_code
            provider_type = routed.provider.provider_type
            log = self._start_log(
                task_type=task_type,
                provider_code=provider_code,
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
                result, retry_trace = self._chat_with_transient_retry(
                    provider_type=provider_type,
                    instance=routed.instance,
                    messages=messages,
                    temperature=temperature,
                    max_tokens=max_tokens,
                    metadata_json=metadata,
                )
                if metadata.get("require_real_model") and not str(result.response_text or "").strip():
                    raise ValueError("模型未返回可用正文，不能将空响应视为治理成功")
                output_schema = metadata.get("json_schema_output")
                response_json = result.response_json
                response_text = result.response_text
                if output_schema and not metadata.get("defer_schema_validation"):
                    parsed_response = json.loads(response_text)
                    repairs: list[str] = []
                    if metadata.get("bounded_schema_repair"):
                        parsed_response, repairs = _normalize_bounded_json(
                            parsed_response, output_schema, output_schema
                        )
                        if repairs:
                            response_text = json.dumps(parsed_response, ensure_ascii=False)
                            response_json = {
                                **(response_json if isinstance(response_json, dict) else {}),
                                "schema_repairs": repairs,
                            }
                    Draft202012Validator(output_schema).validate(parsed_response)
                log.status = "SUCCESS"
                log.response_text = response_text
                log.response_json = response_json
                attempted_models.extend({**item, "call_log_id": log.id} for item in retry_trace)
                log.request_json = {**(log.request_json or {}), "attempted_models": attempted_models}
                log.completed_at = datetime.now(timezone.utc)
                log.latency_ms = self._compute_latency_ms(log.started_at, log.completed_at)
                self.db.commit()
                self.db.refresh(log)
                return log
            except Exception as exc:
                self.db.rollback()
                last_log = self._mark_failed(log.id, exc, routed.instance.api_key)
                trace = getattr(exc, "model_attempts", None) or [{
                    "attempt": 1, "provider": provider_code,
                    "instance": routed.instance.instance_code,
                    "model": routed.instance.model_code, "status": "FAILED",
                    "error": last_log.error_message,
                    "retryable": self._is_transient_retryable(exc),
                }]
                attempted_models.extend({**item, "call_log_id": last_log.id} for item in trace)
                last_log.request_json = {**(last_log.request_json or {}), "attempted_models": attempted_models}
                if not self._should_fallback(exc):
                    stop_after_failure = True
                    break
            if stop_after_failure:
                break

        if last_log:
            summary = self._format_attempt_summary(attempted_models)
            last_log.request_json = {**(last_log.request_json or {}),
                                     "attempted_models": attempted_models,
                                     "failure_summary": summary,
                                     "retryable": any(item.get("retryable") for item in attempted_models)}
            last_log.error_message = f"{last_log.error_message or '模型调用失败'}；{summary}"
            self.db.commit()
            self.db.refresh(last_log)
            return last_log
        raise RuntimeError("No available model instance for this task")

    def test_instance(self, instance_code: str) -> ModelCallLog:
        instance = self._get_instance_by_code(instance_code)
        routed_model = self._to_routed_model(instance)
        prompt = "请只返回：模型连通性测试成功。"
        log = self._start_log(
            task_type="model_test",
            provider_code=routed_model.provider.provider_code,
            instance_code=routed_model.instance.instance_code,
            model_code=routed_model.instance.model_code,
            request_json={"mode": "connectivity", "prompt": prompt},
        )
        try:
            result = get_model_adapter(routed_model.provider.provider_type).chat(
                instance=routed_model.instance,
                messages=[{"role": "user", "content": prompt}],
                metadata_json={"mode": "connectivity"},
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
            return self._mark_failed(log.id, exc, routed_model.instance.api_key)

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

    def _mark_failed(self, log_id: int, exc: Exception, api_key: str | None = None) -> ModelCallLog:
        failed_log = self.db.get(ModelCallLog, log_id)
        if not failed_log:
            raise RuntimeError(f"Model call log not found: {log_id}") from exc
        failed_log.status = "FAILED"
        failed_log.error_message = describe_model_error(exc, api_key)
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
        api_key = (
            decrypt_api_key(provider.api_key_encrypted)
            if provider.api_key_encrypted
            else instance.api_key or _runtime_api_key(provider)
        )
        resolved = ModelInstance(
            provider_id=instance.provider_id,
            instance_code=instance.instance_code,
            model_code=instance.model_code,
            model_name=instance.model_name,
            purpose=instance.purpose,
            usage_type=instance.usage_type,
            api_key=api_key,
            api_base_url=provider.api_base_url or instance.api_base_url or (provider.config_json or {}).get("api_base_url"),
            api_path=instance.api_path,
            max_tokens=instance.max_tokens,
            temperature=instance.temperature,
            top_p=instance.top_p,
            enabled=instance.enabled,
            config_json=instance.config_json or {},
        )
        return RoutedModel(provider=provider, instance=resolved)

    def _chat_with_transient_retry(
        self,
        *,
        provider_type: str,
        instance: ModelInstance,
        messages: list[dict[str, str]],
        temperature: float | None,
        max_tokens: int | None,
        metadata_json: dict[str, Any],
    ):
        """Call one provider with a bounded retry for transient outages.

        The outer call keeps one durable ModelCallLog for the routed model and
        stores every transport attempt in ``request_json.attempted_models``.
        This preserves the existing audit contract while making a 503/timeout
        visible instead of silently replacing it with the last provider error.
        """
        attempts: list[dict[str, Any]] = []
        adapter = get_model_adapter(provider_type)
        for attempt in range(1, self._MAX_TRANSIENT_ATTEMPTS + 1):
            try:
                result = adapter.chat(
                    instance=instance,
                    messages=messages,
                    temperature=temperature,
                    max_tokens=max_tokens,
                    metadata_json=metadata_json,
                )
                attempts.append({"attempt": attempt, "provider": provider_type,
                                 "instance": instance.instance_code,
                                 "model": instance.model_code, "status": "SUCCESS",
                                 "retryable": False})
                return result, attempts
            except Exception as exc:
                retryable = self._is_transient_retryable(exc)
                attempts.append({"attempt": attempt, "provider": provider_type,
                                 "instance": instance.instance_code,
                                 "model": instance.model_code, "status": "FAILED",
                                 "error": describe_model_error(exc, instance.api_key),
                                 "retryable": retryable})
                if not retryable or attempt >= self._MAX_TRANSIENT_ATTEMPTS:
                    # Exception objects are mutable; attaching the trace keeps
                    # the outer audit path independent of provider exception
                    # classes and avoids losing the first 503 attempt.
                    exc.model_attempts = attempts
                    raise
                time.sleep(self._TRANSIENT_BACKOFF_SECONDS * attempt)
        raise RuntimeError("No available model attempt")

    @staticmethod
    def _is_transient_retryable(exc: Exception) -> bool:
        if isinstance(exc, httpx.HTTPStatusError):
            return exc.response.status_code in {408, 429, 500, 502, 503, 504}
        return isinstance(exc, (httpx.RequestError, TimeoutError))

    @staticmethod
    def _format_attempt_summary(attempts: list[dict[str, Any]]) -> str:
        if not attempts:
            return "没有可用模型调用记录"
        parts = []
        for item in attempts:
            identity = f"{item.get('provider') or 'UNKNOWN'}/{item.get('instance') or 'UNKNOWN'}"
            error = str(item.get("error") or "成功").replace("\n", " ")[:500]
            suffix = "（可重试）" if item.get("retryable") else ""
            parts.append(f"{identity}#{item.get('call_log_id')}: {error}{suffix}")
        return "模型调用链：" + "；".join(parts)

    @staticmethod
    def _should_fallback(exc: Exception) -> bool:
        if isinstance(exc, httpx.HTTPStatusError):
            # Authentication, billing, availability and rate-limit failures
            # are provider-specific.  A different configured provider may
            # still be healthy, so fail over for those classes.  Most 400
            # responses are prompt/contract errors and must remain terminal,
            # but some providers return billing/permission exhaustion as 400
            # (for example DashScope's overdue-payment response).  Detect
            # that provider-specific case from the response body instead of
            # stopping the whole governance workflow on the first provider.
            status = exc.response.status_code
            if status == 400:
                try:
                    body = exc.response.text.lower()
                except Exception:
                    body = str(exc).lower()
                billing_markers = (
                    "overdue-payment", "overdue payment", "insufficient balance",
                    "insufficient quota", "quota exceeded", "billing",
                    "account is in good standing", "access denied",
                    "余额", "额度", "欠费", "计费",
                )
                return any(marker in body for marker in billing_markers)
            return status in {401, 402, 403, 404, 408, 409, 429} or status >= 500
        return isinstance(exc, (httpx.RequestError, ValueError, TimeoutError, ValidationError, json.JSONDecodeError))

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
