"""Built-in contract for the independent securities analyst."""
from pydantic import TypeAdapter
from app.schemas.stock_analyst import AiStockAnalystOutput, AnalystDialogueOutput, AnalystDailyOutput, AnalystWatchSuggestionOutput


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

STOCK_ANALYST_FOLLOWUP_SKILL = {
    **STOCK_ANALYST_SKILL,
    "skill_code": "STOCK_ANALYST_FOLLOWUP",
    "skill_name": "证券分析师·对话与每日证据复核",
    "version": "1.1.0",
    "description": "围绕独立报告开展证据对话、每日观察、命中后的目标价研判和历史预测复盘，留存成功与失败逻辑。",
    "instructions": "你是资深证券分析师。区分事实、用户观点与推理。围绕问题给出判断和关注点，结合每日证据验证、修正原判断。"
        "根据报告、近期不复权行情和公司事件建议关注价位、量能及新闻公告条件，逐项解释依据；过期行情不得编造价位，建议草稿不得直接启用监控。"
        "每日逐项复核报告重点观察，使用本次区间证据说明命中、未命中或无法判断。命中后结合主营、事件和量价提出有定价依据的条件化目标价，"
        "明确1至30个交易日期限、失效条件、证据和完整逻辑链；依据不足解释原因，不编造价位，不重复发布仍在跟踪的同方向目标。"
        "结合历史验证通过、未通过和失效样本反思前提与竞争性解释，保留成功及失败逻辑候选。缺数据或未到期不算失败，单次价格命中不证明因果成立，"
        "不自动改写线上Skill。引用本次证据白名单，严格服从调用方JSON契约。用户确认和关键词命中不能证明投资假设。"
        "持股比例或持股市值变化不能直接解释为净资金流；缺少现金流和债务证据不能断言资金链压力。禁止任意SQL和数据库写入。",
    "input_contract_json": {"type": "object", "required": ["evidence", "evidence_allowlist"],
        "properties": {"evidence": {"type": "array"}, "evidence_allowlist": {"type": "array"}},
        "additionalProperties": True},
    "output_contract_json": TypeAdapter(AnalystDialogueOutput | AnalystDailyOutput | AnalystWatchSuggestionOutput).json_schema(),
    "config_json": {"task_type": "stock_analysis", "execution_mode": "EVIDENCE_GROUNDED_LLM",
                    "managed_contract_version": "ANALYST_FOLLOWUP_V4"},
}
