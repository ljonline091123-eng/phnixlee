from unittest.mock import patch

import httpx
import pytest
from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session

from app.db.base import Base
from app.models.ai_hub import ModelInstance
from app.services.model_errors import describe_model_error
from app.services.model_hub import ModelHubService, seed_default_models


def http_error(code, *, message=None):
    request = httpx.Request("POST", "https://example.invalid/models/test?key=url-secret")
    response = httpx.Response(
        code, request=request,
        json={"error": {"message": message}} if message else None,
    )
    return httpx.HTTPStatusError("unhelpful generic HTTP error", request=request, response=response)


@pytest.mark.parametrize("code,hint", [
    (400, "参数"), (401, "身份验证"), (402, "余额不足"), (403, "权限"),
    (404, "模型或接口不可用"), (429, "配额"), (503, "暂时异常"),
])
def test_http_errors_explain_cause_without_exposing_request_url(code, hint):
    result = describe_model_error(http_error(code))
    assert f"HTTP {code}" in result
    assert hint in result
    assert "url-secret" not in result


def test_provider_detail_explains_model_restriction_and_redacts_secrets():
    result = describe_model_error(http_error(404, message=(
        "This model models/gemini-2.5-flash is no longer available to new users. "
        "Use models/gemini-3.6-flash. private-secret "
        "https://example.invalid/?key=another-secret&mode=test"
    )), "private-secret")
    assert "不再向新用户开放" in result
    assert "gemini-3.6-flash" in result
    assert "private-secret" not in result
    assert "another-secret" not in result


def test_insufficient_balance_explains_billing_action_and_redacts_secrets():
    result = describe_model_error(http_error(402, message=(
        "Insufficient Balance private-secret "
        "https://example.invalid/?key=another-secret"
    )), "private-secret")
    assert "HTTP 402" in result
    assert "余额不足" in result
    assert "账单" in result
    assert "充值" in result
    assert "Insufficient Balance" in result
    assert "private-secret" not in result
    assert "another-secret" not in result
    assert "url-secret" not in result


def test_non_json_error_does_not_dump_html_body():
    request = httpx.Request("POST", "https://example.invalid")
    response = httpx.Response(502, request=request, text="<html>internal proxy secret</html>")
    result = describe_model_error(httpx.HTTPStatusError("bad gateway", request=request, response=response))
    assert "HTTP 502" in result
    assert "internal proxy secret" not in result


def test_local_failures_and_timeouts_remain_safe():
    assert "超时" in describe_model_error(httpx.ReadTimeout("timed out"))
    assert "网络" in describe_model_error(httpx.ConnectError("connect failed"))
    assert describe_model_error(ValueError("invalid private/key"), "private/key") == "invalid ***"
    assert "private%2Fkey" not in describe_model_error(ValueError("invalid private%2Fkey"), "private/key")


def test_failure_log_keeps_upstream_reason_and_new_default_preserves_custom_model():
    engine = create_engine("sqlite://")
    Base.metadata.create_all(engine)
    try:
        with Session(engine) as db:
            seed_default_models(db)
            instance = db.scalar(select(ModelInstance).where(ModelInstance.instance_code == "GEMINI_FLASH"))
            assert instance.model_code == "gemini-3.6-flash"
            instance.api_key = "private-secret"
            db.commit()
            with patch("app.services.model_hub.get_model_adapter") as adapter:
                adapter.return_value.chat.side_effect = http_error(404, message="model unavailable private-secret")
                log = ModelHubService(db).test_instance("GEMINI_FLASH")
            db.expire_all()
            assert log.status == "FAILED"
            assert "model unavailable" in log.error_message
            assert "private-secret" not in log.error_message
            instance.model_code = "operator-custom-model"
            db.commit()
            seed_default_models(db)
            assert instance.model_code == "operator-custom-model"
    finally:
        engine.dispose()
