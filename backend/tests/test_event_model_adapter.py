"""Ensure event extraction produces bounded JSON without changing other model uses."""
import json

import httpx
import pytest

from app.connectors.model_adapters import GeminiRestAdapter, OpenAICompatAdapter
from app.models.ai_hub import ModelInstance


@pytest.mark.parametrize("model,purpose,real,json_scoped,thinking_disabled", [
    ("deepseek-v4-flash", "NEWS_NOTICE_EVENT_EXTRACTION", True, True, True),
    ("deepseek-v4-flash", "NEWS_NOTICE_SEMANTIC_ANALYSIS", True, True, True),
    ("deepseek-v4-flash", "DOCUMENT_GOVERNANCE_PLANNING", True, True, True),
    ("deepseek-v4-flash", "DOCUMENT_SPECIALIST_DRAFT", True, True, True),
    ("deepseek-v4-pro", "STOCK_SELECTION_ANALYSIS", True, True, True),
    ("deepseek-v4-pro", "STOCK_SELECTION_SINGLE_STOCK_RECOVERY", True, True, True),
    ("deepseek-v4-pro", "STOCK_SELECTION_SINGLE_STOCK_CONTRACT_RETRY", True, True, True),
    ("deepseek-v4-flash", "SELECTION_MODEL_SWITCH_PREFLIGHT", True, False, True),
    ("deepseek-v4-pro", "STOCK_SELECTION_SINGLE_STOCK_CONTRACT_RETRY", False, False, False),
    ("deepseek-v4-pro", "STOCK_SELECTION_ANALYSIS", False, False, False),
    ("deepseek-v4-flash", "ADVISORY_QUALITY_REVIEW", True, False, False),
    ("deepseek-v4-flash", "NEWS_NOTICE_EVENT_EXTRACTION", False, False, False),
    ("other-model", "NEWS_NOTICE_EVENT_EXTRACTION", True, True, False),
    ("glm-4-flash", "STOCK_SELECTION_ANALYSIS", True, True, False),
    ("deepseek-v4-flash", "STANDALONE_STOCK_ANALYST", True, True, True),
])
def test_event_json_options_are_scoped(monkeypatch, model, purpose, real, json_scoped, thinking_disabled):
    payloads = []

    def handle(request):
        assert str(request.url) == "https://model.test/v1/chat/completions"
        payloads.append(json.loads(request.content))
        return httpx.Response(200, json={"choices": [{"message": {"content": '{"documents": []}'}}]})

    client = httpx.Client(transport=httpx.MockTransport(handle))
    monkeypatch.setattr("app.connectors.model_adapters.httpx.Client", lambda **kwargs: client)
    instance = ModelInstance(instance_code="test", model_code=model, api_key="test-secret", api_base_url="https://model.test/v1",
                             max_tokens=6000, temperature=0.2, top_p=0.95, config_json={})
    result = OpenAICompatAdapter().chat(instance, [{"role": "user", "content": "JSON"}],
        metadata_json={"purpose": purpose, "require_real_model": real})
    assert result.response_text == '{"documents": []}'
    if json_scoped:
        assert payloads[0]["response_format"] == {"type": "json_object"}
    else:
        assert "response_format" not in payloads[0]
    if thinking_disabled:
        assert payloads[0]["thinking"] == {"type": "disabled"}
    else:
        assert "thinking" not in payloads[0]


@pytest.mark.parametrize("purpose,real,controlled", [
    ("STANDALONE_STOCK_ANALYST", True, True),
    ("STANDALONE_STOCK_ANALYST", False, False),
    ("GENERAL", True, False),
])
def test_gemini_analyst_reserves_answer_budget_and_excludes_thought_parts(monkeypatch, purpose, real, controlled):
    payloads = []

    def handle(request):
        payloads.append(json.loads(request.content))
        return httpx.Response(200, json={"candidates": [{"finishReason": "STOP", "content": {"parts": [
            {"thought": True, "text": "内部推理不是JSON正文"},
            {"text": '{"executive_summary":"完整正文"}'},
        ]}}]})

    client = httpx.Client(transport=httpx.MockTransport(handle))
    monkeypatch.setattr("app.connectors.model_adapters.httpx.Client", lambda **kwargs: client)
    instance = ModelInstance(instance_code="test", model_code="gemini-3.6-flash", api_key="test-secret",
                             api_base_url="https://model.test/v1beta", max_tokens=6000,
                             temperature=0.2, top_p=0.95, config_json={})
    result = GeminiRestAdapter().chat(instance, [{"role": "user", "content": "分析"}], max_tokens=8192,
                                     metadata_json={"purpose": purpose, "require_real_model": real})
    config = payloads[0]["generationConfig"]
    assert config["maxOutputTokens"] == 8192
    assert result.response_text == '{"executive_summary":"完整正文"}'
    assert result.response_json["candidates"][0]["content"]["parts"][0]["thought"] is True
    if controlled:
        assert config["responseMimeType"] == "application/json"
        assert config["thinkingConfig"] == {"thinkingBudget": 1024}
    else:
        assert "responseMimeType" not in config
        assert "thinkingConfig" not in config
