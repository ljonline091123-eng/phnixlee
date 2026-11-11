from __future__ import annotations

from dataclasses import dataclass
from typing import Any

import httpx

from app.models.ai_hub import ModelInstance


# Provider connections can fail before a model starts generating. Keep that
# failure short so one unreachable provider cannot hold the durable job queue
# for several minutes, while allowing long evidence-analysis responses enough
# time to arrive once the connection is established.
MODEL_HTTP_TIMEOUT = httpx.Timeout(connect=8.0, read=90.0, write=30.0, pool=8.0)


def text_content(content) -> str:
    if isinstance(content, str):
        return content
    return "\n".join(part.get("text", "") for part in content if part.get("type") == "text")


def native_parts(content, provider: str):
    if isinstance(content, str):
        return [{"text": content}] if provider == "GEMINI_REST" else content
    parts = []
    for part in content:
        if part.get("type") == "text":
            parts.append({"text": part["text"]} if provider == "GEMINI_REST" else {"type": "text", "text": part["text"]})
        elif part.get("type") == "image_url":
            url = part["image_url"]["url"]
            if not url.startswith("data:image/") or ";base64," not in url:
                raise ValueError("图片请求仅支持已验证的内嵌图片")
            header, data = url.split(",", 1)
            mime = header[5:].removesuffix(";base64")
            parts.append({"inlineData": {"mimeType": mime, "data": data}} if provider == "GEMINI_REST"
                         else {"type": "image", "source": {"type": "base64", "media_type": mime, "data": data}})
        else:
            raise ValueError("不支持的模型输入内容类型")
    return parts


@dataclass(frozen=True)
class ModelExecutionResult:
    response_text: str
    response_json: dict[str, Any]


class ModelAdapter:
    adapter_type: str

    def health_check(self, instance: ModelInstance) -> tuple[str, list[str]]:
        raise NotImplementedError

    def chat(
        self,
        instance: ModelInstance,
        messages: list[dict[str, str]],
        temperature: float | None = None,
        max_tokens: int | None = None,
        metadata_json: dict[str, Any] | None = None,
    ) -> ModelExecutionResult:
        raise NotImplementedError


class MockModelAdapter(ModelAdapter):
    adapter_type = "MOCK"

    def health_check(self, instance: ModelInstance) -> tuple[str, list[str]]:
        return ("Mock model is ready.", ["chat"])

    def chat(
        self,
        instance: ModelInstance,
        messages: list[dict[str, str]],
        temperature: float | None = None,
        max_tokens: int | None = None,
        metadata_json: dict[str, Any] | None = None,
    ) -> ModelExecutionResult:
        user_text = next((item["content"] for item in reversed(messages) if item["role"] == "user"), "")
        response_text = (
            f"[MOCK:{instance.instance_code}] 已收到 {len(messages)} 条消息。"
            f"最后一条用户输入：{user_text[:120]}"
        )
        return ModelExecutionResult(
            response_text=response_text,
            response_json={
                "provider_type": self.adapter_type,
                "instance_code": instance.instance_code,
                "model_code": instance.model_code,
                "metadata_json": metadata_json or {},
                "echo": user_text,
            },
        )


class OpenAICompatAdapter(ModelAdapter):
    adapter_type = "OPENAI_COMPAT"

    def health_check(self, instance: ModelInstance) -> tuple[str, list[str]]:
        if not instance.api_key:
            raise ValueError("OPENAI_COMPAT instance requires api_key")
        if not instance.api_base_url:
            raise ValueError("OPENAI_COMPAT instance requires api_base_url")
        return ("OpenAI-compatible endpoint is configured.", ["chat"])

    def chat(
        self,
        instance: ModelInstance,
        messages: list[dict[str, str]],
        temperature: float | None = None,
        max_tokens: int | None = None,
        metadata_json: dict[str, Any] | None = None,
    ) -> ModelExecutionResult:
        self.health_check(instance)
        url = self._build_url(instance)
        payload: dict[str, Any] = {
            "model": instance.model_code,
            "messages": messages,
            "temperature": temperature if temperature is not None else instance.temperature,
            "max_tokens": max_tokens if max_tokens is not None else instance.max_tokens,
            "top_p": instance.top_p,
        }
        payload.update(instance.config_json.get("extra_body_json", {}))
        metadata = metadata_json or {}
        # Evidence extraction needs bounded JSON, rather than a long reasoning
        # stream consuming the entire budget before any answer is returned.
        # Scope this provider option to controlled JSON workflows.
        controlled_json = (
            metadata.get("purpose") in {
                "NEWS_NOTICE_EVENT_EXTRACTION", "NEWS_NOTICE_SEMANTIC_ANALYSIS",
                "DOCUMENT_GOVERNANCE_PLANNING", "DOCUMENT_SPECIALIST_DRAFT",
                "DOCUMENT_FAILURE_DIAGNOSIS", "DOCUMENT_SPECIALIST_EVALUATION",
                "DOCUMENT_SPECIALIST_REVISION", "STOCK_SELECTION_EVIDENCE_SEGMENT",
                "STOCK_SELECTION_ANALYSIS",
                "STOCK_SELECTION_SINGLE_STOCK_RECOVERY",
                "STOCK_SELECTION_SINGLE_STOCK_CONTRACT_RETRY",
                "STOCK_SELECTION_PER_STOCK_RETRY",
                "STANDALONE_STOCK_ANALYST",
                "STANDALONE_STOCK_ANALYST_FOLLOWUP",
            }
            and metadata.get("require_real_model")
        )
        selection_preflight = (
            metadata.get("purpose") == "SELECTION_MODEL_SWITCH_PREFLIGHT"
            and metadata.get("require_real_model")
        )
        if controlled_json:
            payload["response_format"] = {"type": "json_object"}
        # DeepSeek's reasoning stream consumes the same completion budget as
        # the visible answer.  A tiny connectivity probe can therefore end
        # with ``finish_reason=length`` and an empty ``content`` even though
        # the account and endpoint are healthy.  Disable reasoning for both
        # bounded JSON workflows and the explicit selection preflight, while
        # leaving normal conversational calls unchanged.
        if instance.model_code.lower().startswith("deepseek") and (controlled_json or selection_preflight):
            payload["thinking"] = {"type": "disabled"}
        output_schema = metadata.get("json_schema_output")
        if output_schema and metadata.get("native_structured_output"):
            payload["response_format"] = {
                "type": "json_schema",
                "json_schema": {
                    "name": str(metadata.get("json_schema_name") or "structured_output"),
                    "strict": True,
                    "schema": output_schema,
                },
            }
        headers = {
            "Authorization": f"Bearer {instance.api_key}",
            "Content-Type": "application/json",
        }
        with httpx.Client(timeout=MODEL_HTTP_TIMEOUT) as client:
            response = client.post(url, json=payload, headers=headers)
            response.raise_for_status()
            response_json = response.json()
        response_text = (
            response_json["choices"][0]["message"]["content"]
            if response_json.get("choices")
            else str(response_json)
        )
        return ModelExecutionResult(response_text=response_text, response_json=response_json)

    def _build_url(self, instance: ModelInstance) -> str:
        base_url = instance.api_base_url.rstrip("/")
        api_path = (instance.api_path or "chat/completions").lstrip("/")
        return f"{base_url}/{api_path}"


class AnthropicMessagesAdapter(ModelAdapter):
    adapter_type = "ANTHROPIC"

    def health_check(self, instance: ModelInstance) -> tuple[str, list[str]]:
        if not instance.api_key:
            raise ValueError("ANTHROPIC instance requires api_key")
        if not instance.api_base_url:
            raise ValueError("ANTHROPIC instance requires api_base_url")
        return ("Claude Messages endpoint is configured.", ["chat"])

    def chat(
        self,
        instance: ModelInstance,
        messages: list[dict[str, str]],
        temperature: float | None = None,
        max_tokens: int | None = None,
        metadata_json: dict[str, Any] | None = None,
    ) -> ModelExecutionResult:
        self.health_check(instance)
        url = f"{instance.api_base_url.rstrip('/')}/{(instance.api_path or 'messages').lstrip('/')}"
        system = "\n".join(text_content(item["content"]) for item in messages if item["role"] == "system").strip()
        conversation = [
            {"role": "assistant" if item["role"] == "assistant" else "user", "content": native_parts(item["content"], "ANTHROPIC")}
            for item in messages if item["role"] != "system"
        ]
        payload: dict[str, Any] = {
            "model": instance.model_code,
            "max_tokens": max_tokens if max_tokens is not None else instance.max_tokens,
            "messages": conversation,
        }
        if system:
            payload["system"] = system
        # Current Claude Sonnet models reject non-default sampling parameters;
        # opt in for models that explicitly support them.
        if (instance.config_json or {}).get("send_sampling_parameters"):
            payload["temperature"] = temperature if temperature is not None else instance.temperature
            payload["top_p"] = instance.top_p
        payload.update((instance.config_json or {}).get("extra_body_json", {}))
        headers = {
            "Authorization": f"Bearer {instance.api_key}",
            "anthropic-version": "2023-06-01",
            "Content-Type": "application/json",
        }
        workspace_id = (instance.config_json or {}).get("workspace_id")
        if workspace_id:
            headers["anthropic-workspace-id"] = str(workspace_id)
        with httpx.Client(timeout=MODEL_HTTP_TIMEOUT) as client:
            response = client.post(url, json=payload, headers=headers)
            response.raise_for_status()
            response_json = response.json()
        response_text = "\n".join(
            item.get("text", "") for item in response_json.get("content", []) if item.get("type") == "text"
        )
        return ModelExecutionResult(response_text=response_text, response_json=response_json)


class GeminiRestAdapter(ModelAdapter):
    adapter_type = "GEMINI_REST"

    def health_check(self, instance: ModelInstance) -> tuple[str, list[str]]:
        if not instance.api_key:
            raise ValueError("GEMINI_REST instance requires api_key")
        if not instance.api_base_url:
            raise ValueError("GEMINI_REST instance requires api_base_url")
        if not instance.model_code:
            raise ValueError("GEMINI_REST instance requires model_code")
        return ("Gemini REST endpoint is configured.", ["chat"])

    def chat(
        self,
        instance: ModelInstance,
        messages: list[dict[str, str]],
        temperature: float | None = None,
        max_tokens: int | None = None,
        metadata_json: dict[str, Any] | None = None,
    ) -> ModelExecutionResult:
        self.health_check(instance)
        base_url = instance.api_base_url.rstrip("/")
        model_code = instance.model_code.strip()
        if model_code.startswith("models/"):
            model_code = model_code.removeprefix("models/")
        url = f"{base_url}/models/{model_code}:generateContent"
        system_instruction = "\n".join(text_content(item["content"]) for item in messages if item["role"] == "system").strip()
        contents = [
            {"role": "model" if item["role"] == "assistant" else "user", "parts": native_parts(item["content"], "GEMINI_REST")}
            for item in messages
            if item["role"] != "system"
        ]
        payload: dict[str, Any] = {"contents": contents}
        if system_instruction:
            payload["systemInstruction"] = {"parts": [{"text": system_instruction}]}
        generation_config: dict[str, Any] = {
            "temperature": temperature if temperature is not None else instance.temperature,
            "maxOutputTokens": max_tokens if max_tokens is not None else instance.max_tokens,
            "topP": instance.top_p,
        }
        extra_body = dict((instance.config_json or {}).get("extra_body_json", {}))
        generation_config.update(extra_body.pop("generationConfig", {}))
        metadata = metadata_json or {}
        preflight = metadata.get("purpose") == "SELECTION_MODEL_SWITCH_PREFLIGHT" or metadata.get("mode") == "connectivity"
        modern = model_code.startswith(("gemini-3", "gemini-4"))
        supports_thinking = modern or model_code.startswith("gemini-2.5")
        if preflight and modern:
            # Gemini 3+ expects a level, and 64 tokens can be exhausted before
            # the probe emits any visible text. Keep a small, bounded budget.
            generation_config["maxOutputTokens"] = 1024
        output_schema = metadata.get("json_schema_output")
        controlled_json = (
            metadata.get("purpose") in {
                "STOCK_SELECTION_EVIDENCE_SEGMENT",
                "STOCK_SELECTION_ANALYSIS",
                "STOCK_SELECTION_SINGLE_STOCK_RECOVERY",
                "STOCK_SELECTION_SINGLE_STOCK_CONTRACT_RETRY",
                "STANDALONE_STOCK_ANALYST",
                "STANDALONE_STOCK_ANALYST_FOLLOWUP",
            }
            and metadata.get("require_real_model")
        )
        if controlled_json:
            generation_config.setdefault("responseMimeType", "application/json")
        thinking = dict(generation_config.get("thinkingConfig") or {})
        configured_level = (instance.config_json or {}).get("thinking_level")
        if preflight and supports_thinking:
            thinking.pop("thinkingBudget", None)
            thinking.pop("thinkingLevel", None)
            if modern:
                thinking["thinkingLevel"] = "LOW"
            else:
                thinking["thinkingBudget"] = 128 if model_code.startswith("gemini-2.5-pro") else 0
                if thinking["thinkingBudget"]:
                    generation_config["maxOutputTokens"] = 1024
        elif "thinkingLevel" not in thinking and "thinkingBudget" not in thinking:
            if modern:
                level = str(configured_level or ("low" if controlled_json else "")).lower()
                if level in {"off", "none", "minimal", "low", "medium", "high"}:
                    # LOW/HIGH also work on Pro models that do not expose
                    # MINIMAL or MEDIUM. LOW does not mean reasoning is off.
                    thinking["thinkingLevel"] = "HIGH" if level == "high" else "LOW"
            else:
                budgets = {"off": 0, "none": 0, "low": 1024, "medium": 4096, "high": 8192}
                budget = budgets.get(str(configured_level).lower(), configured_level)
                try:
                    if budget is not None or controlled_json:
                        thinking["thinkingBudget"] = int(budget if budget is not None else 1024)
                except (ValueError, TypeError):
                    pass
        if thinking:
            generation_config["thinkingConfig"] = thinking
        if output_schema and metadata.get("native_structured_output"):
            generation_config["responseMimeType"] = "application/json"
            generation_config["responseJsonSchema"] = output_schema
        payload.update(extra_body)
        payload["generationConfig"] = generation_config
        with httpx.Client(timeout=MODEL_HTTP_TIMEOUT) as client:
            response = client.post(url, json=payload, headers={"x-goog-api-key": instance.api_key})
            response.raise_for_status()
            response_json = response.json()
        text = ""
        candidates = response_json.get("candidates") or []
        if candidates:
            content = candidates[0].get("content") or {}
            parts = content.get("parts") or []
            # Thinking parts are not the final answer and can invalidate an
            # otherwise correct JSON document when includeThoughts is enabled.
            text = "\n".join(part.get("text", "") for part in parts if part.get("text") and not part.get("thought"))
        if not text.strip():
            reason = (candidates[0].get("finishReason") if candidates else None) or (
                response_json.get("promptFeedback") or {}
            ).get("blockReason")
            raise ValueError(f"Gemini 未返回可用正文（{reason or '无正文候选'}）")
        if preflight and candidates[0].get("finishReason") == "MAX_TOKENS":
            raise ValueError("Gemini 未返回可用正文：连通性测试输出被截断")
        return ModelExecutionResult(response_text=text, response_json=response_json)
