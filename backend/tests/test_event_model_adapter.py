"""Ensure event extraction produces bounded JSON without changing other model uses."""
import json

import httpx
import pytest

from app.connectors.model_adapters import OpenAICompatAdapter
from app.models.ai_hub import ModelInstance


@pytest.mark.parametrize("model,purpose,real,scoped", [
    ("deepseek-v4-flash", "NEWS_NOTICE_EVENT_EXTRACTION", True, True),
    ("deepseek-v4-flash", "ADVISORY_QUALITY_REVIEW", True, False),
    ("deepseek-v4-flash", "NEWS_NOTICE_EVENT_EXTRACTION", False, False),
    ("other-model", "NEWS_NOTICE_EVENT_EXTRACTION", True, False),
])
def test_event_json_options_are_scoped(monkeypatch, model, purpose, real, scoped):
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
    if scoped:
        assert payloads[0]["thinking"] == {"type": "disabled"}
        assert payloads[0]["response_format"] == {"type": "json_object"}
    else:
        assert "thinking" not in payloads[0] and "response_format" not in payloads[0]
