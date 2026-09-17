"""Actionable model errors, safe to persist in logs and show in the UI."""

from __future__ import annotations

import re
from urllib.parse import quote, quote_plus

import httpx


def describe_model_error(exc: Exception, api_key: str | None = None) -> str:
    if isinstance(exc, httpx.HTTPStatusError):
        code = exc.response.status_code
        hints = {
            400: "请求参数不被接受，请检查模型参数和接口配置。",
            401: "身份验证失败，请检查 API Key 是否有效。",
            402: "API 账户余额不足或付费额度不可用，请到服务商开放平台检查账单并充值后重试。",
            403: "访问被拒绝，请检查 API Key 权限、项目授权或地区限制。",
            404: "模型或接口不可用，请检查模型编码、当前账户的模型使用权限及 API 路径。密钥输入框留空不表示密钥丢失。",
            429: "请求频率或可用配额受限，请检查额度并稍后重试。",
        }
        hint = hints.get(code, "模型服务暂时异常，请稍后重试。" if code >= 500 else "模型服务拒绝了请求。")
        message = f"HTTP {code}：{hint}"
        # Only show the provider's error message, never dump headers or full bodies.
        try:
            payload = exc.response.json()
        except (ValueError, httpx.DecodingError):
            payload = None
        if isinstance(payload, dict):
            error = payload.get("error")
            detail = error.get("message") if isinstance(error, dict) else error
            if isinstance(detail, str) and detail.strip():
                if code == 404 and "no longer available to new users" in detail.lower():
                    message = "HTTP 404：当前模型已不再向新用户开放，请将模型实例的模型编码改为服务商支持的新型号。"
                message += f"\n服务商详情：{detail.strip()}"
    elif isinstance(exc, httpx.TimeoutException):
        message = "模型服务连接或响应超时，请检查网络连接并稍后重试。"
    elif isinstance(exc, httpx.RequestError):
        message = "无法连接模型服务，请检查 API Base URL、网络和代理设置。"
    else:
        message = str(exc)
    if api_key:
        for secret in (api_key, quote(api_key, safe=""), quote_plus(api_key)):
            message = message.replace(secret, "***")
    message = re.sub(r"(?i)([?&](?:key|api_key|api-key|access_token)=)[^\s&\"'<>]+", r"\1***", message)
    return message[:4000]
