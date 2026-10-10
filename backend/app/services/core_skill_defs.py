"""Built-in SOPs and their deterministic function contracts."""

from __future__ import annotations

from pathlib import Path


DOC_ROOT = Path(__file__).resolve().parents[2] / "skill_docs"

CORE_SKILLS = (
    {
        "skill_code": "NEWS_NOTICE_EVENT_GOVERNOR",
        "skill_name": "新闻公告结构化事件治理",
        "description": "有界读取原文、补取官方公告正文、调用真实模型抽取事件并精确核验证据，写入既有事实与证据体系。",
        "task_type": "stock_data_governance",
        "skill_type": "EXECUTABLE_TOOL",
        "side_effect_level": "CONTROLLED_WRITE",
        "idempotency_policy": "REQUIRED",
        "permission_policy_json": {
            "arbitrary_sql": False, "database_write": True, "unrestricted_database_write": False,
            "allowed_operations": ["EXTRACT_EVIDENCED_EVENTS"],
            "allowed_services": ["govern_stock_documents", "create_fact", "create_evidence", "review_fact"],
            "write_scope": {"tables": ["stock_news", "stock_notice", "foundation_fact", "foundation_evidence", "foundation_fact_evidence", "foundation_fact_review"], "operations": ["INSERT", "UPDATE"]},
        },
        "function_spec": {
            "name": "govern_stock_documents", "internal_service_only": True,
            "parameters": {"type": "object", "required": ["market", "symbol", "governance_batch_id"],
                "properties": {"market": {"type": "string"}, "symbol": {"type": "string"}, "governance_batch_id": {"type": "string"}},
                "additionalProperties": True},
        },
    },
    {
        "skill_code": "ONDEMAND_DATA_DISTILLER",
        "skill_name": "按需数据双轨蒸馏 SOP",
        "description": "单文档有界提取问答块与有证据的图谱三元组。",
        "task_type": "data_distillation",
        "skill_type": "PROMPT_SOP",
        "function_spec": {
            "name": "trigger_ondemand_extraction",
            "description": "Read one provenance-linked stock document before bounded LLM extraction.",
            "endpoint": "/api/v1/model-hub/skill-tools/trigger-ondemand-extraction",
            "parameters": {
                "type": "object",
                "required": ["stock_code", "doc_id"],
                "properties": {
                    "stock_code": {"type": "string", "minLength": 1, "maxLength": 32},
                    "doc_id": {"type": "integer", "minimum": 1},
                },
                "additionalProperties": False,
            },
        },
    },
    {
        "skill_code": "ONDEMAND_DATA_DISTILLER_TOOL",
        "skill_name": "按需业务原文读取工具",
        "description": "确定性读取单条来源原文，核对证券代码及文档来源。",
        "task_type": "data_distillation",
        "skill_type": "EXECUTABLE_TOOL",
        "function_spec": {
            "name": "trigger_ondemand_extraction",
            "description": "Read one provenance-linked stock document; no model or graph writes.",
            "endpoint": "/api/v1/model-hub/skill-tools/trigger-ondemand-extraction",
            "parameters": {
                "type": "object",
                "required": ["stock_code", "doc_id"],
                "properties": {
                    "stock_code": {"type": "string", "minLength": 1, "maxLength": 32},
                    "doc_id": {"type": "integer", "minimum": 1},
                },
                "additionalProperties": False,
            },
        },
    },
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
    {
        "skill_code": "PREDICTION_LEDGER_WRITER",
        "skill_name": "预测账本受控写入",
        "description": "仅将已校验的候选预测写入预测账本，限定服务、表、操作和幂等键，并保留完整执行审计。",
        "task_type": "prediction_persistence",
        "skill_type": "EXECUTABLE_TOOL",
        "side_effect_level": "CONTROLLED_WRITE",
        "idempotency_policy": "REQUIRED",
        "retry_policy_json": {"max_attempts": 1, "retry_on": []},
        "permission_policy_json": {
            "arbitrary_sql": False,
            "database_write": True,
            "unrestricted_database_write": False,
            "allowed_operations": ["CREATE_PREDICTION_LEDGER"],
            "allowed_services": ["prediction_ledger_service"],
            "write_scope": {
                "tables": ["prediction_ledger"],
                "operations": ["INSERT"],
            },
        },
        "function_spec": {
            "name": "record_agent_predictions",
            "description": "Persist validated prediction candidates through the governed prediction-ledger service.",
            "internal_service_only": True,
            "parameters": {
                "type": "object",
                "required": ["candidates", "idempotency_key"],
                "properties": {
                    "candidates": {"type": "array", "minItems": 1, "maxItems": 50},
                    "idempotency_key": {"type": "string", "minLength": 1, "maxLength": 192},
                    "evidence_ids": {"type": "array", "maxItems": 500, "items": {"type": "string"}},
                    "model_instance_code": {"type": ["string", "null"]},
                    "model_call_log_id": {"type": ["integer", "null"]},
                },
                "additionalProperties": False,
            },
        },
    },
    {
        "skill_code": "SECURITY_BOARD_IDENTITY_GOVERNOR",
        "skill_name": "证券与板块身份核验",
        "description": "按主数据和上市板块契约核验市场、证券代码、证券类型及板块，不按名称猜测或跨市场合并。",
        "task_type": "stock_data_governance",
        "skill_type": "EXECUTABLE_TOOL",
        "function_spec": {
            "name": "validate_stock_governance_targets",
            "description": "Validate a bounded stock list against governed security master and board taxonomy.",
            "internal_service_only": True,
            "parameters": {
                "type": "object", "required": ["governance_batch_id", "stocks"],
                "properties": {
                    "governance_batch_id": {"type": "string", "minLength": 1, "maxLength": 128},
                    "stocks": {"type": "array", "minItems": 1, "maxItems": 30},
                }, "additionalProperties": False,
            },
        },
    },
    {
        "skill_code": "STOCK_SOURCE_COLLECTION",
        "skill_name": "股票来源数据受控采集",
        "description": "按已配置市场路由调用白名单采集服务，保存原始响应、来源、时间、哈希和采集日志。",
        "task_type": "stock_data_governance",
        "skill_type": "EXECUTABLE_TOOL",
        "side_effect_level": "CONTROLLED_WRITE",
        "idempotency_policy": "REQUIRED",
        "permission_policy_json": {
            "arbitrary_sql": False, "database_write": True, "unrestricted_database_write": False,
            "allowed_operations": ["FETCH_AND_UPSERT_STOCK_DATA", "WRITE_FETCH_AUDIT"],
            "allowed_services": ["StockOnDemandService", "f10_governance_service"],
            "write_scope": {"tables": ["data_fetch_log", "stock_realtime_quote", "stock_kline", "stock_financial_report", "stock_news", "stock_notice", "stock_f10_cache", "stock_investor_qa", "stock_earnings_consensus", "stock_institution_forecast", "stock_broker_research_report"], "operations": ["INSERT", "UPDATE"]},
        },
        "function_spec": {
            "name": "collect_stock_source_data",
            "description": "Collect one stock through configured source routes and existing idempotent services.",
            "internal_service_only": True,
            "parameters": {
                "type": "object", "required": ["governance_batch_id", "market", "symbol", "business_types"],
                "properties": {
                    "governance_batch_id": {"type": "string", "minLength": 1, "maxLength": 128},
                    "market": {"type": "string"}, "symbol": {"type": "string"},
                    "business_types": {"type": "array", "minItems": 1, "maxItems": 6},
                }, "additionalProperties": False,
            },
        },
    },
    {
        "skill_code": "STOCK_DATA_QUALITY_GATE",
        "skill_name": "股票数据质量门禁",
        "description": "以确定性规则区分通过、部分、缺失、来源失败和公开来源不可得，不允许用空响应冒充完成。",
        "task_type": "stock_data_governance",
        "skill_type": "EXECUTABLE_TOOL",
        "function_spec": {
            "name": "evaluate_stock_governance_quality",
            "description": "Evaluate collection and persistence evidence with deterministic quality states.",
            "internal_service_only": True,
            "parameters": {
                "type": "object", "required": ["governance_batch_id", "stock_results"],
                "properties": {
                    "governance_batch_id": {"type": "string", "minLength": 1, "maxLength": 128},
                    "stock_results": {"type": "array", "minItems": 1, "maxItems": 30},
                }, "additionalProperties": False,
            },
        },
    },
    {
        "skill_code": "LAKEHOUSE_KNOWLEDGE_PUBLISHER",
        "skill_name": "湖仓与知识链受控发布",
        "description": "复用现有湖仓、切片、知识库和图谱服务发布版本并保留完整血缘；不直接执行 SQL。",
        "task_type": "stock_data_governance",
        "skill_type": "EXECUTABLE_TOOL",
        "side_effect_level": "CONTROLLED_WRITE",
        "idempotency_policy": "REQUIRED",
        "permission_policy_json": {
            "arbitrary_sql": False, "database_write": True, "unrestricted_database_write": False,
            "allowed_operations": ["PUBLISH_LAKE_DATASET", "CREATE_DOCUMENT_CHUNKS", "BUILD_SCOPED_GRAPH"],
            "allowed_services": ["run_stock_pipeline", "lakehouse", "knowledge_pipeline"],
            "write_scope": {"tables": ["lake_object", "lake_dataset", "lake_dataset_version", "lake_lineage_event", "document_chunk_version", "knowledge_document", "knowledge_entity", "knowledge_relation"], "operations": ["INSERT", "UPDATE"]},
        },
        "function_spec": {
            "name": "publish_stock_knowledge_chain",
            "description": "Publish bounded stock data into lakehouse, chunks and evidence-linked graph.",
            "internal_service_only": True,
            "parameters": {
                "type": "object", "required": ["governance_batch_id", "market", "symbols"],
                "properties": {
                    "governance_batch_id": {"type": "string", "minLength": 1, "maxLength": 128},
                    "market": {"type": "string"},
                    "symbols": {"type": "array", "minItems": 1, "maxItems": 30},
                }, "additionalProperties": True,
            },
        },
    },
    {
        "skill_code": "STOCK_GOVERNANCE_ACCEPTANCE",
        "skill_name": "股票治理结果验收",
        "description": "按同一验收口径汇总逐股数据、来源、湖仓、切片、知识图谱和血缘状态，并与系统治理基线比较。",
        "task_type": "stock_data_governance",
        "skill_type": "EXECUTABLE_TOOL",
        "function_spec": {
            "name": "accept_stock_governance_run",
            "description": "Produce evidence-linked per-stock acceptance without changing business facts.",
            "internal_service_only": True,
            "parameters": {
                "type": "object", "required": ["governance_batch_id", "result_status", "stage_results"],
                "properties": {
                    "governance_batch_id": {"type": "string", "minLength": 1, "maxLength": 128},
                    "result_status": {"type": "string"}, "stage_results": {"type": "object"},
                }, "additionalProperties": True,
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
        "skill_type": item.get("skill_type", "PROMPT_SOP"),
        "enabled": True,
        "lifecycle_status": "ENABLED",
        "input_contract_json": item["function_spec"].get("parameters") or {},
        "output_contract_json": {"type": "object"},
        "permission_policy_json": item.get("permission_policy_json") or {
            "arbitrary_sql": False,
            "database_write": False,
            "unrestricted_database_write": False,
            "allowed_operations": ["READ_GOVERNED_DATA", "RETURN_BOUNDED_RESULT"],
            "allowed_services": [item["function_spec"]["name"]],
        },
        "side_effect_level": item.get("side_effect_level", "READ_ONLY"),
        "idempotency_policy": item.get("idempotency_policy", "OPTIONAL"),
        "retry_policy_json": item.get("retry_policy_json") or {
            "max_attempts": 2, "retry_on": ["TimeoutError", "RuntimeError"]
        },
        "error_policy_json": {"on_error": "FAIL_CLOSED", "retain_audit": True},
        "config_json": {"task_type": item["task_type"], "function_spec": item["function_spec"]},
    } for item in CORE_SKILLS)
