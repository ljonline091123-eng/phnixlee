"""Stock-scoped references and actual multimodal transport; no live model calls."""
import base64
from io import BytesIO
import json
from unittest.mock import patch

import httpx
from fastapi.testclient import TestClient
from PIL import Image
import pytest
from pydantic import ValidationError
from sqlalchemy import select

from tests.test_stock_analyst import db
from tests.test_analyst_followup import analysis, followup_log, model_output
from app.main import app
from app.db.session import get_db
from app.models.ai_hub import ModelInstance, ModelProvider
from app.models.market_data import StockNews, StockNotice, DataSource
from app.schemas.stock_analyst import AnalystDialogueCreate
from app.services import analyst_followup as service
from app.services.analyst_inputs import validate_image, supports_images
from app.connectors.model_adapters import OpenAICompatAdapter, GeminiRestAdapter, AnthropicMessagesAdapter
from app.services.model_hub import ModelHubService


def png():
    stream = BytesIO()
    Image.new("RGB", (16, 12), "white").save(stream, format="PNG")
    return "data:image/png;base64," + base64.b64encode(stream.getvalue()).decode()


def vision_model(db):
    provider = ModelProvider(provider_code="VISION_TEST", provider_name="图片测试", provider_type="GEMINI_REST")
    db.add(provider)
    db.flush()
    row = ModelInstance(provider_id=provider.id, instance_code="REAL_TEST", model_name="图片测试",
                        model_code="gemini-3.6-flash", config_json={})
    db.add(row)
    db.commit()
    return row


def notice(db, **kwargs):
    row = StockNotice(market=kwargs.pop("market", "HK"), symbol=kwargs.pop("symbol", "02533"),
        source_id=db.scalar(select(DataSource.id)), notice_date="2026-11-10", title=kwargs.pop("title", "联合创始人职务调整"),
        content_json=kwargs.pop("content_json", {"content": "执行董事调整为非执行董事，总裁由CEO兼任。"}), **kwargs)
    db.add(row)
    db.commit()
    return row


@pytest.mark.parametrize("value", ["data:image/png;base64,aGVsbG8=", "data:image/png;base64,!!",
    "data:text/html;base64,PGgxPmhlbGxvPC9oMT4=", "https://image.test/a.png"])
def test_rejects_forged_images(value):
    with pytest.raises(ValidationError):
        AnalystDialogueCreate(question="请分析图片", images=[{"name": "截图", "data_url": value}])


def test_image_format_bytes_and_limits():
    mime, binary, width, height = validate_image(png())
    assert (mime, width, height) == ("image/png", 16, 12) and binary.startswith(b"\x89PNG")
    with pytest.raises(ValueError):
        validate_image(png().replace("image/png", "image/jpeg"))
    with pytest.raises(ValueError, match="4MB"):
        validate_image("data:image/png;base64," + base64.b64encode(b"x" * (4 * 1024 * 1024 + 1)).decode())
    with pytest.raises(ValidationError):
        AnalystDialogueCreate(question="请分析图片", images=[{"data_url": png()}] * 5)


def test_model_capability_checks_and_explicit_override(db, analysis):
    assert supports_images("gemini-3.6-flash") and supports_images("qwen3-vl-plus")
    assert not supports_images("deepseek-v4-flash")
    assert not supports_images("gemini-3.6-flash", {"supports_images": False})
    assert supports_images("custom", {"supports_vision": True})
    with pytest.raises(ValueError, match="不支持图片"):
        service.queue_dialogue(db, analysis.id, AnalystDialogueCreate(question="分析截图", images=[{"data_url": png()}]))


def test_new_local_reference_is_frozen_and_priority_input(db, analysis):
    doc = notice(db)
    ref = f"notice:{doc.id}"
    assert ref not in analysis.evidence_snapshot_json["evidence_allowlist"]
    row = service.queue_dialogue(db, analysis.id, AnalystDialogueCreate(question="人事变化如何影响公司？", evidence_ids=[ref]))
    original = row.context_snapshot_json["selected_evidence"][0]
    doc.content_json = {"content": "发送之后被更新的文本不应取代快照"}
    db.commit()
    result = model_output(analysis)
    result["evidence"] = [{"evidence_id": ref, "claim": "管理层角色调整需要结合主营业务验证"}]
    with patch.object(service.ModelHubService, "chat", return_value=followup_log(db, result)) as call:
        saved = service.execute_dialogue(db, row.id)
    assert saved.status == "COMPLETED"
    prompt = call.call_args.kwargs["messages"][0]["content"]
    assert original["excerpt"] in prompt
    assert "发送之后被更新" not in prompt
    assert original["data"]["content_sha256"]
    assert saved.context_snapshot_json["evidence"][0]["evidence_id"] == ref
    assert saved.context_snapshot_json["selected_evidence"] == [original]


@pytest.mark.parametrize("market,symbol", [("CN_A", "02533"), ("HK", "00700")])
def test_cross_market_or_stock_reference_rejected(db, analysis, market, symbol):
    doc = notice(db, market=market, symbol=symbol)
    with pytest.raises(ValueError, match="不属于当前股票"):
        service.queue_dialogue(db, analysis.id, AnalystDialogueCreate(question="不能串股", evidence_ids=[f"notice:{doc.id}"]))


@pytest.mark.parametrize("success", [True, False])
def test_images_reach_model_and_history_hides_base64(db, analysis, success):
    vision_model(db)
    row = service.queue_dialogue(db, analysis.id, AnalystDialogueCreate(question="请解释截图中的判断", images=[{"name": "用户截图", "data_url": png()}]))
    image_id = row.context_snapshot_json["images"][0]["evidence_id"]
    result = model_output(analysis)
    result["evidence"] = [{"evidence_id": image_id, "claim": "用户材料有待独立核验"}]
    log = followup_log(db, result, status="SUCCESS" if success else "FAILED")
    with patch.object(service.ModelHubService, "chat", return_value=log) as call:
        saved = service.execute_dialogue(db, row.id)
    assert saved.status == ("COMPLETED" if success else "FAILED")
    parts = call.call_args.kwargs["messages"][0]["content"]
    assert parts[1] == {"type": "image_url", "image_url": {"url": png()}}
    assert image_id in saved.context_snapshot_json["evidence_allowlist"]
    assert saved.context_snapshot_json["images"][0]["data_url"] == png()
    app.dependency_overrides[get_db] = lambda: db
    try:
        client = TestClient(app)
        state = client.get(f"/api/v1/ai-stock-analyst/analyses/{analysis.id}/followup").json()
        assert "base64" not in json.dumps(state)
        image_url = state["dialogues"][0]["images"][0]["url"]
        response = client.get(image_url)
        assert response.status_code == 200 and response.headers["content-type"] == "image/png"
        assert response.content == validate_image(png())[1]
        assert client.get(image_url.replace(f"/analyses/{analysis.id}/", "/analyses/999999/")).status_code == 404
        assert client.get(image_url.rsplit("/", 1)[0] + "/4").status_code == 404
        evidence = client.get(f"/api/v1/ai-stock-analyst/analyses/{analysis.id}/dialogues/{row.id}/evidence").json()
        assert "images" not in evidence and "base64" not in json.dumps(evidence)
    finally:
        app.dependency_overrides.clear()


def test_local_document_api_filters_paginates_and_does_not_invent_text(db, analysis):
    notice(db, title="人事调整公告")
    missing = notice(db, title="只有标题公告", content_json={})
    notice(db, market="CN_A", title="跨市场人事公告")
    app.dependency_overrides[get_db] = lambda: db
    try:
        client = TestClient(app)
        url = f"/api/v1/ai-stock-analyst/analyses/{analysis.id}/local-documents"
        first = client.get(url, params={"category": "NOTICE", "limit": 1}).json()
        second = client.get(url, params={"category": "NOTICE", "limit": 1, "offset": 1}).json()
        assert first["total"] == 2 and first["items"][0]["evidence_id"] != second["items"][0]["evidence_id"]
        found = client.get(url, params={"keyword": "人事", "category": "NOTICE"}).json()
        assert found["total"] == 1 and found["items"][0]["excerpt"]
        only_title = client.get(url, params={"keyword": "只有标题"}).json()["items"][0]
        assert only_title["evidence_id"] == f"notice:{missing.id}"
        assert only_title["excerpt"] is None and only_title["data"]["content_status"] == "TITLE_ONLY"
        assert client.get(url, params={"keyword": "%"}).json()["total"] == 0
        assert client.get(url, params={"category": "UNKNOWN"}).status_code == 422
        assert client.get(url.replace(f"/{analysis.id}/", "/999999/")).status_code == 404
    finally:
        app.dependency_overrides.clear()


@pytest.mark.parametrize("adapter", [OpenAICompatAdapter, GeminiRestAdapter, AnthropicMessagesAdapter])
def test_native_provider_image_payload(monkeypatch, adapter):
    captured = []
    def handle(request):
        captured.append(json.loads(request.content))
        if adapter is GeminiRestAdapter:
            return httpx.Response(200, json={"candidates": [{"content": {"parts": [{"text": "正确"}]}}]})
        if adapter is AnthropicMessagesAdapter:
            return httpx.Response(200, json={"content": [{"type": "text", "text": "正确"}]})
        return httpx.Response(200, json={"choices": [{"message": {"content": "正确"}}]})
    client = httpx.Client(transport=httpx.MockTransport(handle))
    monkeypatch.setattr("app.connectors.model_adapters.httpx.Client", lambda **kwargs: client)
    instance = ModelInstance(instance_code="test", model_code="test-vision", api_key="test", api_base_url="https://model.test",
                             max_tokens=100, temperature=.2, top_p=.95, config_json={})
    content = [{"type": "text", "text": "分析图片"}, {"type": "image_url", "image_url": {"url": png()}}]
    result = adapter().chat(instance, [{"role": "system", "content": "规则"}, {"role": "user", "content": content}])
    assert result.response_text == "正确"
    if adapter is GeminiRestAdapter:
        part = captured[0]["contents"][0]["parts"][1]["inlineData"]
        assert part == {"mimeType": "image/png", "data": png().split(",")[1]}
    elif adapter is AnthropicMessagesAdapter:
        part = captured[0]["messages"][0]["content"][1]["source"]
        assert part == {"type": "base64", "media_type": "image/png", "data": png().split(",")[1]}
    else:
        assert captured[0]["messages"][1]["content"] == content


def test_multimodal_text_cannot_bypass_character_budget(db):
    with patch.object(ModelHubService, "list_route_options", return_value=[]) as route:
        with pytest.raises(ValueError, match="60,000"):
            ModelHubService(db).chat(task_type="stock_analysis", messages=[{"role": "user", "content": [
                {"type": "text", "text": "字" * 60001}, {"type": "image_url", "image_url": {"url": png()}}]}])
        route.assert_not_called()
        # Image bytes do not consume the character budget, but routing still runs normally.
        with pytest.raises(RuntimeError, match="No available model"):
            ModelHubService(db).chat(task_type="stock_analysis", messages=[{"role": "user", "content": [
                {"type": "text", "text": "图片分析"}, {"type": "image_url", "image_url": {"url": "data:image/png;base64," + "A" * 70000}}]}])
        route.assert_called_once()
