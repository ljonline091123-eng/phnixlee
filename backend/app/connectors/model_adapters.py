from __future__ import annotations

from dataclasses import dataclass
from typing import Any

import httpx

from app.models.ai_hub import ModelInstance


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
        if not instance.api_key or not instance.api_base_url:
            raise ValueError("OPENAI_COMPAT instance requires api_key and api_base_url")
        return ("OpenAI-compatible endpoint is configured.", ["chat"])

    def chat(
        self,
        instance: ModelInstance,
        messages: list[dict[str, str]],
        temperature: float | None = None,
        max_tokens: int | None = None,
        metadata_json: dict[str, Any] | None = None,
    ) -> ModelExecutionResult:
        if not instance.api_key or not instance.api_base_url:
            raise ValueError("OPENAI_COMPAT instance requires api_key and api_base_url")
        url = self._build_url(instance)
        payload: dict[str, Any] = {
            "model": instance.model_code,
            "messages": messages,
            "temperature": temperature if temperature is not None else instance.temperature,
            "max_tokens": max_tokens if max_tokens is not None else instance.max_tokens,
            "top_p": instance.top_p,
        }
        payload.update(instance.config_json.get("extra_body_json", {}))
        headers = {
            "Authorization": f"Bearer {instance.api_key}",
            "Content-Type": "application/json",
        }
        with httpx.Client(timeout=httpx.Timeout(60.0)) as client:
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
        if not instance.api_key or not instance.api_base_url:
            raise ValueError("ANTHROPIC instance requires api_key and api_base_url")
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
        system = "\n".join(item["content"] for item in messages if item["role"] == "system").strip()
        conversation = [
            {"role": "assistant" if item["role"] == "assistant" else "user", "content": item["content"]}
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
        with httpx.Client(timeout=httpx.Timeout(60.0)) as client:
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
        return ("Gemini REST endpoint is configured.", ["chat"])

    def chat(
        self,
        instance: ModelInstance,
        messages: list[dict[str, str]],
        temperature: float | None = None,
        max_tokens: int | None = None,
        metadata_json: dict[str, Any] | None = None,
    ) -> ModelExecutionResult:
        if not instance.api_key:
            raise ValueError("GEMINI_REST instance requires api_key")
        base_url = (instance.api_base_url or "https://generativelanguage.googleapis.com/v1beta").rstrip("/")
        url = f"{base_url}/models/{instance.model_code}:generateContent"
        system_instruction = "\n".join(item["content"] for item in messages if item["role"] == "system").strip()
        contents = [
            {"role": "model" if item["role"] == "assistant" else "user", "parts": [{"text": item["content"]}]}
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
        payload["generationConfig"] = generation_config
        payload.update(instance.config_json.get("extra_body_json", {}))
        with httpx.Client(timeout=httpx.Timeout(60.0)) as client:
            response = client.post(url, json=payload, headers={"x-goog-api-key": instance.api_key})
            response.raise_for_status()
            response_json = response.json()
        text = ""
        candidates = response_json.get("candidates") or []
        if candidates:
            content = candidates[0].get("content") or {}
            parts = content.get("parts") or []
            text = "\n".join(part.get("text", "") for part in parts if part.get("text"))
        return ModelExecutionResult(response_text=text or str(response_json), response_json=response_json)
