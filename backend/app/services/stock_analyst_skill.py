"""Built-in contract for the independent securities analyst."""
from app.schemas.stock_analyst import AiStockAnalystOutput


STOCK_ANALYST_SKILL = {
    "skill_code": "SENIOR_STOCK_TREND_ANALYST",
    "skill_name": "资深证券分析师·趋势研判",
    "description": "直接阅读单只股票的本地证据，综合理解主营与重大事项，形成短中长期条件化趋势判断。",
    "instructions": "你是资深证券分析师。先综合理解公司，再分析未来可能趋势。区分事实与推理，以本次证据白名单引用，严格按照调用方JSON契约输出。",
    "enabled": True,
    "lifecycle_status": "ENABLED",
    "skill_type": "PROMPT_SOP",
    "version": "1.0.0",
    "input_contract_json": {
        "type": "object", "required": ["identity", "as_of", "evidence", "evidence_allowlist"],
        "properties": {"identity": {"type": "object"}, "as_of": {"type": "string"},
                       "evidence": {"type": "array"}, "evidence_allowlist": {"type": "array"}},
        "additionalProperties": True,
    },
    "output_contract_json": AiStockAnalystOutput.model_json_schema(),
    "permission_policy_json": {
        "arbitrary_sql": False, "database_write": False, "unrestricted_database_write": False,
        "allowed_operations": ["READ_LOCAL_STOCK_EVIDENCE", "RETURN_CONDITIONAL_TREND_ANALYSIS"],
        "allowed_services": ["stock_analyst"],
    },
    "side_effect_level": "READ_ONLY", "idempotency_policy": "SNAPSHOT",
    "retry_policy_json": {"max_attempts": 1, "retry_on": [], "backoff": "NONE"},
    "error_policy_json": {"on_error": "FAIL_CLOSED", "retain_audit": True, "fabricate_fallback": False},
    "config_json": {"task_type": "stock_analysis", "execution_mode": "EVIDENCE_GROUNDED_LLM",
                    "managed_contract_version": "SENIOR_AI_ANALYST_V1"},
}
