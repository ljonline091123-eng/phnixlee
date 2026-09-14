"""Built-in SOPs and their deterministic function contracts."""

from __future__ import annotations

from pathlib import Path


DOC_ROOT = Path(__file__).resolve().parents[2] / "skill_docs"

CORE_SKILLS = (
    {
        "skill_code": "DATA_CLEANING_DW",
        "skill_name": "数据清洗与 DW 入湖",
        "description": "Python 批量校验和去重，Agent 只解释质量摘要与入湖建议。",
        "task_type": "data_governance",
        "function_spec": {
            "name": "validate_dw_records",
            "description": "Validate and deduplicate a bounded batch of stock DW source records.",
            "endpoint": "/api/v1/model-hub/skill-tools/validate-dw-records",
            "parameters": {
                "type": "object",
                "required": ["records"],
                "properties": {
                    "records": {"type": "array", "maxItems": 5000, "items": {"type": "object", "required": ["market", "stock_code", "category", "source", "source_record_id", "observed_at"]}},
                },
            },
        },
    },
    {
        "skill_code": "WATCH_ALERT",
        "skill_name": "自动盯盘预警",
        "description": "Python/SQL 计算量价异常并压缩至最多 50 只候选，再交 Agent 解读。",
        "task_type": "risk_warning",
        "function_spec": {
            "name": "filter_watch_candidates",
            "description": "Rank price/volume anomalies without sending the whole market to an LLM.",
            "endpoint": "/api/v1/model-hub/skill-tools/filter-watch-candidates",
            "parameters": {
                "type": "object",
                "required": ["metrics"],
                "properties": {
                    "metrics": {"type": "array", "maxItems": 5000, "items": {"type": "object", "required": ["market", "stock_code", "price", "previous_close", "volume", "avg_volume_20", "as_of"]}},
                    "min_abs_change_pct": {"type": "number", "minimum": 0},
                    "min_volume_ratio": {"type": "number", "minimum": 0},
                    "max_candidates": {"type": "integer", "minimum": 1, "maximum": 50},
                },
            },
        },
    },
    {
        "skill_code": "RESEARCH_REVIEW_CORRECTION",
        "skill_name": "研报复盘修正",
        "description": "确定性计算预测方向与收益偏差，Agent 基于证据提出待审核修订。",
        "task_type": "research_report",
        "function_spec": {
            "name": "score_prediction_outcomes",
            "description": "Score matured prediction outcomes from observed prices.",
            "endpoint": "/api/v1/model-hub/skill-tools/score-prediction-outcomes",
            "parameters": {
                "type": "object",
                "required": ["outcomes"],
                "properties": {
                    "outcomes": {"type": "array", "maxItems": 50, "items": {"type": "object", "required": ["ledger_id", "action_type", "entry_price", "actual_price", "price_as_of", "source"]}},
                },
            },
        },
    },
)


def core_skill_rows() -> tuple[dict, ...]:
    return tuple({
        "skill_code": item["skill_code"],
        "skill_name": item["skill_name"],
        "description": item["description"],
        "instructions": (DOC_ROOT / f"{item['skill_code']}.SKILL.md").read_text(encoding="utf-8"),
        "skill_type": "PROMPT_SOP",
        "enabled": True,
        "config_json": {"task_type": item["task_type"], "function_spec": item["function_spec"]},
    } for item in CORE_SKILLS)
