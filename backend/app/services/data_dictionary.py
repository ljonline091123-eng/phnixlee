"""Chinese preview metadata shared by asset APIs, without rewriting source values."""

from __future__ import annotations

from typing import Any

from app.db.table_comments import (
    COMMON_COLUMN_DESCRIPTIONS,
    TABLE_COLUMN_OVERRIDES,
    TABLE_DESCRIPTIONS,
)


# Labels are concise table headings. Longer business meanings come from the
# existing schema dictionary so UI explanations and database comments agree.
COLUMN_LABELS: dict[str, str] = {
    "id": "记录标识", "market": "证券市场", "symbol": "股票代码", "stock_code": "股票代码",
    "name": "名称", "entity_id": "主体标识", "entity_type": "主体类型", "jurisdiction": "注册法域",
    "identifier_scheme": "标识体系", "identifier_value": "外部标识", "properties_json": "扩展属性",
    "security_id": "证券标识", "share_class": "股份类别", "stock_symbol_id": "股票记录标识",
    "evidence_id": "证据标识", "fact_id": "事实标识", "source_name": "来源名称", "source_key": "来源文档键",
    "source": "数据来源", "source_id": "数据源标识", "source_code": "数据源代码",
    "source_url": "来源链接", "url": "原文链接", "title": "标题", "content": "正文",
    "published_at": "发布时间", "available_at": "可获知时间", "observed_at": "观察时间",
    "content_hash": "内容校验值", "fingerprint": "版本指纹", "version": "版本",
    "metadata_json": "来源元数据", "fact_type": "事实类型", "subject_entity_id": "关系主体标识",
    "object_entity_id": "关系客体标识", "status": "状态", "valid_from": "有效期开始",
    "valid_to": "有效期结束", "created_at": "创建时间", "updated_at": "更新时间",
    "namespace": "来源命名空间", "external_id": "来源稳定标识", "dimension": "分类维度",
    "code": "分类代码", "label": "分类名称", "definition_version": "定义版本", "method": "分类方法",
    "reviews_json": "审核记录", "reason": "结果原因", "source_snapshot": "来源快照",
    "attempt_count": "尝试次数", "details_json": "详细结果", "attempted_at": "最近尝试时间",
    "taxonomy": "分类体系", "definition": "名词释义", "criteria": "纳入标准", "parent_code": "上级分类代码",
    "exchange": "交易所", "asset_type": "证券类型", "industry": "行业", "list_date": "上市日期",
    "ext_json": "基础资料扩展", "last_synced_at": "最近同步时间", "currency": "币种",
    "current_price": "最新价格", "change_pct": "涨跌幅", "change_percent": "涨跌幅",
    "change_amount": "涨跌额", "open_price": "开盘价", "high_price": "最高价", "low_price": "最低价",
    "close_price": "收盘价", "previous_close_price": "昨收价", "volume": "成交量", "amount": "成交额",
    "turnover_rate": "换手率", "amplitude": "振幅", "quote_time": "行情时间", "fetched_at": "获取时间",
    "raw_payload": "来源原始数据", "period": "行情周期", "adjust": "复权口径", "trade_date": "交易日期",
    "indicator": "财务指标", "report_period": "报告期", "data_json": "指标数据",
    "notice_date": "公告日期", "notice_type": "公告类型", "content_json": "结构化正文",
    "news_time": "新闻时间", "summary": "摘要", "sentiment": "情绪标注", "section": "资料栏目",
    "payload_json": "栏目原始数据", "event_type": "事件类别", "related_entity": "相关主体",
    "report_markdown": "研报正文", "conclusion": "研究结论", "rating": "评级", "score": "评分",
    "model_provider": "模型供应商", "model_instance": "模型实例", "model_instance_code": "模型实例代码",
    "data_sources_json": "引用数据源", "knowledge_base_ids_json": "引用知识库",
    "history_evaluation_json": "历史研报评估", "warnings_json": "风险提示", "note": "备注",
    "agent_snapshot_json": "研究智能体配置快照",
    "description": "业务说明", "enabled": "是否启用", "interface_code": "接口代码",
    "request_mode": "获取方式", "request_json": "请求参数", "record_count": "记录数量",
    "persisted_count": "入库数量", "inserted_count": "新增数量", "updated_count": "更新数量",
    "deleted_count": "删除数量", "failed_count": "失败数量", "total_count": "总数",
    "started_at": "开始时间", "completed_at": "完成时间", "finished_at": "结束时间",
    "error_message": "异常说明", "detail_json": "结果详情",
    "object_uri": "对象存储地址", "layer": "数据分层", "bucket": "存储桶", "object_key": "对象键",
    "content_type": "内容类型", "byte_size": "文件大小", "source_table": "来源数据表",
    "source_record_id": "来源记录编号", "dataset_version": "数据集版本", "dataset_code": "数据集编码",
    "dataset_name": "数据集名称", "format": "存储格式", "partition_spec": "分区规范",
    "current_version": "当前版本", "batch_id": "处理批次", "upstream_type": "上游对象类型",
    "upstream_id": "上游对象编号", "downstream_type": "下游对象类型", "downstream_id": "下游对象编号",
    "transformation": "转换步骤", "parser_version": "解析器版本", "chunk_index": "切片序号",
    "chunk_version": "切片版本", "chunk_text": "切片正文", "start_offset": "原文起始位置",
    "end_offset": "原文结束位置", "embedding_model": "向量模型版本", "document_key": "文档稳定键",
    "document_id": "文档编号", "row_count": "数据行数", "schema_json": "字段结构",
    "quality_json": "质量检查结果",

    # 湖仓、知识图谱与治理执行表的扩展字段。 这些字段在新增治理模型中
    # 会出现在数据资产预览里，必须和 COMMON_COLUMN_DESCRIPTIONS 共用同一
    # 契约，不能回退为英文物理列名。
    "dataset_id": "数据集标识", "object_id": "湖仓对象标识",
    "knowledge_base_id": "知识库标识", "graph_id": "知识图谱标识",
    "section_title": "章节标题", "section_path_json": "章节路径",
    "section_status": "章节识别状态", "embedding_status": "向量状态",
    "embedding_kind": "向量类型", "source_object_id": "原文对象标识",
    "lineage_batch_id": "血缘批次标识",
    "entity_key": "实体唯一键", "entity_name": "实体名称",
    "predicate": "关系谓词", "evidence_document_id": "证据文档标识",
    "rule_code": "规则编码", "rule_name": "规则名称", "asset_scope": "资产范围",
    "target_code": "目标编码", "rule_type": "规则类型", "severity": "严重程度",
    "definition_json": "规则定义", "threshold_json": "门禁阈值",
    "lifecycle_status": "生命周期状态",
    "run_key": "运行唯一键", "target_type": "目标类型", "target_id": "目标标识",
    "target_version": "目标版本", "pipeline_run_id": "工作流运行标识",
    "total_rules": "规则总数", "passed_rules": "通过规则数",
    "failed_rules": "失败规则数", "warning_rules": "警告规则数",
    "summary_json": "运行结果摘要",
    "quality_run_id": "质量运行标识", "rule_id": "规则标识", "issue_code": "问题编码",
    "message": "问题说明", "observed_value_json": "实际观测值",
    "expected_value_json": "期望值", "evidence_ids_json": "证据标识列表",
    "first_detected_at": "首次发现时间", "resolved_at": "解决时间",
    "resolution": "解决说明",
    "parent_run_id": "父级运行标识", "pipeline_stage_run_id": "工作流阶段标识",
    "agent_id": "智能体标识", "agent_code": "智能体编码", "agent_version": "智能体版本",
    "model_version": "模型版本", "model_call_log_id": "模型调用日志标识",
    "trigger_type": "触发类型", "correlation_id": "关联请求标识",
    "idempotency_key": "幂等键", "input_hash": "输入哈希", "output_hash": "输出哈希",
    "input_json": "输入参数", "output_json": "输出结果",
    "accessible_assets_json": "可访问资产列表", "knowledge_versions_json": "知识版本快照",
    "skill_versions_json": "技能版本快照", "audit_json": "审计信息",
    "error_code": "错误编码", "agent_execution_run_id": "智能体执行标识",
    "skill_id": "技能标识", "skill_code": "技能编码", "skill_version": "技能版本",
    "skill_name": "技能名称", "instructions": "技能指令正文", "config_json": "技能扩展配置",
    "file_path": "技能文档路径", "is_builtin": "是否系统内置", "skill_type": "技能类型",
    "input_contract_json": "输入契约", "output_contract_json": "输出契约",
    "permission_policy_json": "权限策略", "idempotency_policy": "幂等策略",
    "retry_policy_json": "重试策略", "error_policy_json": "错误处理策略",
    "attempt": "当前重试次数", "max_attempts": "最大重试次数",
    "side_effect_level": "副作用等级", "write_scope_json": "写入范围",
}

TABLE_LABEL_OVERRIDES: dict[str, dict[str, str]] = {
    "foundation_entity": {"id": "主体标识", "name": "主体名称"},
    "foundation_security": {"id": "证券标识", "entity_id": "发行公司标识"},
    "foundation_listing": {"id": "上市映射标识", "name": "证券名称"},
    "foundation_evidence": {"id": "证据版本标识"},
    "foundation_fact": {"id": "事实版本标识", "status": "事实审核状态"},
    "foundation_security_classification": {"id": "分类事实标识", "status": "分类审核状态"},
    "classification_definition": {"id": "分类定义标识", "status": "定义状态"},
    "stock_symbol": {"name": "证券名称", "status": "上市状态"},
}


def table_metadata(table_name: str) -> dict[str, str]:
    description = TABLE_DESCRIPTIONS.get(table_name)
    return {
        "display_name": description.display_name if description else table_name,
        "description": description.description if description else "",
        "domain": description.domain if description else "业务数据",
    }


def column_metadata(table_name: str, column: dict[str, Any]) -> dict[str, Any]:
    """Use real reflected columns; custom tables retain their original names."""
    name = str(column["name"])
    label = TABLE_LABEL_OVERRIDES.get(table_name, {}).get(name, COLUMN_LABELS.get(name, name))
    description = TABLE_COLUMN_OVERRIDES.get(table_name, {}).get(
        name, str(column.get("comment") or COMMON_COLUMN_DESCRIPTIONS.get(name, ""))
    )
    return {
        "name": name,
        "label": label,
        "description": description,
        "type": str(column["type"]),
        "nullable": bool(column.get("nullable", True)),
    }
