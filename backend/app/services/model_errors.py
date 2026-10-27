"""Actionable model errors, safe to persist in logs and show in the UI."""

from __future__ import annotations

import re
from urllib.parse import quote, quote_plus

import httpx


def classify_model_error(error: Exception | str | None) -> dict[str, object]:
    """Return a stable, Chinese, user-facing error contract.

    The raw provider/schema message is retained separately by callers.  This
    classifier prevents JSON Schema/Pydantic internals from becoming the
    primary message in the selection UI while still keeping enough detail for
    technical troubleshooting.
    """
    raw = str(error or "").strip()
    lowered = raw.lower()
    category = "MODEL_UNKNOWN"
    title = "模型调用失败"
    summary = "模型没有返回可用的分析结果。"
    suggestion = "请查看技术详情，或更换模型后重试。优先选择已配置 API Key 且已通过连通性检查的模型。"
    retryable = True
    recovery_actions: list[str] = [
        "先查看模型状态和最近一次调用记录，再决定是否重试。",
        "不要连续重试同一失败模型；必要时先做连通性预检。",
    ]
    if any(token in lowered for token in ("60,000", "60000", "input exceeds", "上下文", "context length")):
        category, title = "MODEL_CONTEXT_LIMIT", "分析上下文超过安全上限"
        summary = "完整证据、输出契约和 Skill 指令合计超过模型调用前的安全预算。"
        suggestion = "系统应先压缩证据上下文；仍超限时请缩小单批股票范围后重试，可继续使用当前模型。"
        recovery_actions = [
            "保留文档 ID、标题、来源和关键原文，对重复元数据与大型嵌套载荷执行有界压缩。",
            "确认进度记录中的压缩后字符数低于 60,000，再使用当前模型重试。",
            "仍超限时减少单批候选数量，不需要因该错误更换模型。",
        ]
    elif any(token in lowered for token in ("余额不足", "欠费", "insufficient_quota", "quota", "billing", "payment required", "http 402")):
        category, title = "MODEL_BILLING", "模型额度或账单不可用"
        summary = "当前模型账户余额不足、额度耗尽或未开通调用权限。"
        suggestion = "请在模型服务商控制台检查余额和配额，或切换到其他可用模型。"
        recovery_actions = [
            "在供应商控制台检查余额、套餐和剩余调用配额。",
            "不要继续重试当前模型；选择状态为“可用”的模型并先做预检。",
            "确认预检成功后，再对失败股票执行定向重试。",
        ]
    elif any(token in lowered for token in ("api key", "apikey", "身份验证", "unauthorized", "http 401", "forbidden", "http 403")):
        category, title = "MODEL_CREDENTIAL", "模型身份验证失败"
        summary = "当前模型的 API Key 无效、缺失或没有调用权限。"
        suggestion = "请检查供应商、API Base URL 和 API Key，或切换到已配置密钥的模型。"
        recovery_actions = [
            "检查模型供应商、API Base URL、模型编码和 API Key。",
            "在模型管理页执行连通性测试，确认返回“可用”后再分析。",
            "如果密钥属于其他供应商，请切换到对应供应商的模型实例。",
        ]
    elif any(token in lowered for token in ("timeout", "超时", "无法连接", "connection", "http 5", "temporarily unavailable")):
        category, title = "MODEL_UNAVAILABLE", "模型服务暂时不可用"
        summary = "模型服务连接失败或响应超时，本次分析未完成。"
        suggestion = "请检查网络后重试；若持续失败，请切换到其他模型。"
        recovery_actions = [
            "检查网络、代理、API Base URL 和服务商状态。",
            "等待服务恢复后重新执行预检；预检失败不会创建新的分析任务。",
            "仍不可用时切换到状态为“可用”的其他模型。",
        ]
    elif any(token in lowered for token in ("json schema", "validationerror", "jsondecodeerror", "expecting property name", "有效 json", "model results", "不符合契约", "not one of", "required property", "additional properties", "缺少 candidates")):
        category, title = "MODEL_SCHEMA", "模型输出格式不符合分析契约"
        summary = "模型返回内容无法按选股分析契约解析，部分字段缺失或取值不受支持。"
        match = re.search(r"['\"]([^'\"]+)['\"]\s+is not one of", raw, re.IGNORECASE)
        if match:
            summary = f"模型返回了不支持的字段取值：{match.group(1)}。"
        suggestion = "系统已保留原始调用记录；可更换模型或重新分析该股票。"
        recovery_actions = [
            "优先对失败股票执行单股定向重试，避免整批响应再次超出契约。",
            "选择支持结构化 JSON 输出且状态为“可用”的模型。",
            "如果仍失败，查看 Skill 输出契约和调用记录中的原始响应。",
        ]
    elif any(token in lowered for token in ("空响应", "empty response", "未返回可用正文", "空的模型响应", "未返回可用结果")):
        category, title = "MODEL_EMPTY_RESPONSE", "模型未返回有效内容"
        summary = "模型返回了空响应，无法生成选股分析结果。"
        suggestion = "请稍后重试，或更换一个可用的模型。"
        recovery_actions = [
            "先执行模型连通性预检，确认服务确实返回正文。",
            "降低单次股票数量或请求长度后重试单股分析。",
            "检查 Token、超时和供应商限流设置。",
        ]
    elif any(token in lowered for token in ("证据", "引用", "白名单", "evidence", "citation")):
        category, title = "MODEL_EVIDENCE", "模型引用的证据未通过校验"
        summary = "模型引用了本次分析范围之外的文档、事实或切片。"
        suggestion = "请重新分析或更换模型；系统不会把未核验结论标记为有效。"
        recovery_actions = [
            "先补齐该股票的新闻、公告正文和知识切片。",
            "重新生成本批次证据包，确保引用 ID 在当前股票白名单内。",
            "再执行单股定向重试；未通过校验的结论不会进入跟踪。",
        ]
    elif any(token in lowered for token in ("数据不足", "insufficient", "缺少知识库", "缺少")):
        category, title = "DATA_INSUFFICIENT", "分析数据不足"
        summary = "当前股票在分析周期内缺少必要的数据或证据。"
        suggestion = "请先补齐数据，或确认以部分数据继续分析。"
        recovery_actions = [
            "查看数据缺口清单，先补齐缺失的数据源或正文。",
            "重新生成知识库、切片和图谱证据后再分析。",
            "如果数据源确实没有数据，保留“部分数据”标记，不要伪造结论。",
        ]
    return {
        "category": category,
        "title": title,
        "summary": summary,
        "suggestion": suggestion,
        "recovery_actions": recovery_actions,
        "retryable": retryable,
        "raw_error": raw[:4000],
    }


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
        # jsonschema/pydantic exceptions are useful in logs but too verbose
        # and English-heavy for the main product message.
        classified = classify_model_error(exc)
        message = (
            f"{classified['title']}：{classified['summary']} 建议：{classified['suggestion']}"
            if classified["category"] != "MODEL_UNKNOWN"
            else str(exc)
        )
    if api_key:
        for secret in (api_key, quote(api_key, safe=""), quote_plus(api_key)):
            message = message.replace(secret, "***")
    message = re.sub(r"(?i)([?&](?:key|api_key|api-key|access_token)=)[^\s&\"'<>]+", r"\1***", message)
    return message[:4000]
