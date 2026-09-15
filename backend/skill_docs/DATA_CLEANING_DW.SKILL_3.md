# DATA_CLEANING_DW

类型：`PROMPT_SOP`。任务路由：`data_governance`。
配套执行函数：`validate_dw_records(records)`。函数调用地址：`/api/v1/model-hub/skill-tools/validate-dw-records`。原始批量数据必须先由 Python/SQL 完成字段校验、去重、异常值检测和分类；单次输入最多 5000 条记录，更大数据集由调度方分批处理。模型只接收函数返回的质量摘要、最多 50 条问题样本与已核验字段说明，不直接读取原始整表或完整行情序列。

## 目标

对股票数据批次进行可追溯的质量分析和 DW 入湖建议，将记录归类为基本资料、新闻、公告、财报、股价、交易量、股东、F10 或研报，并明确目标表、业务主键、问题记录和待审核事项。模型只解释确定性工具结果，不修改事实值，不执行数据库写入，不编造缺失字段、市场数据、来源、时间或业务关系。无法核验的记录必须进入 `pending_review`。

## 执行顺序

1. 调用 `validate_dw_records`，输入最多 5000 条记录；更大数据集由调度方分批处理。每条记录至少包含市场、股票代码、类别、来源、来源记录 ID 和观测时间。调度方不得把全市场原始记录直接传给模型。
2. 校验函数结果中的批次时间、类别计数、业务键、缺失字段、重复记录、未来日期及不合理价格或成交量。问题样本以来源记录 ID 追溯；函数未验证的内容不得被模型标记为已通过。
3. 将已验证记录映射到基本资料、新闻、公告、财报、股价、交易量、股东、F10 或研报目标表，说明业务主键和去重规则。仅把可验证的原始事实建议写入事实层，推断留在分析层；工具输出和模型建议均不代表已经执行入库。
4. 若函数失败、必要字段缺失、映射冲突或数据无法核验，返回 `PENDING_REVIEW`，列出 `quality_issues`、`pending_review` 和 `missing_data`；只有批次不存在未解决问题且全部目标映射可核验时才返回 `COMPLETED`。不得自动填造事实或省略异常记录。

## 严格输出契约

返回且只返回严格的 JSON，不带 Markdown 说明文字或额外字段。顶层只能包含 `as_of`、`status`、`category_counts`、`quality_issues`、`dedupe_keys`、`target_tables`、`pending_review`、`missing_data`、`confidence`、`evidence_text`。

`status` 仅允许 `COMPLETED`、`PENDING_REVIEW`。`confidence` 必须是 0 到 1 的数值。`as_of` 必须是带时区的 ISO 8601 时间。`category_counts` 的值必须来自 `validate_dw_records`，不得由模型估算。`dedupe_keys` 必须使用已核验字段。`target_tables` 只是待执行的映射建议，不得表示已经写库。`pending_review` 必须包含所有未解决问题对应的来源记录 ID；`missing_data` 必须列出阻止完成治理的缺失数据。

`quality_issues` 数组中的每个元素必须且只能包含 `record_id`、`field_name`、`issue_type`、`severity`、`evidence_text`。`severity` 仅允许 `LOW`、`MEDIUM`、`HIGH`。`record_id` 使用来源记录 ID；如果来源记录 ID 本身缺失，则使用调度方提供的批次内临时 ID，并在 `issue_type` 中标记 `MISSING_SOURCE_RECORD_ID`。每条问题必须有可追溯的 `evidence_text`。没有质量问题时返回空数组。

后端必须以 Pydantic Schema 拒绝未知字段、非法状态、非法严重程度、超出范围的置信度及缺失证据。任何校验失败都应保持 `PENDING_REVIEW`，不得进入自动入湖流程。

```json
{
  "as_of": "2026-01-31T10:30:00+08:00",
  "status": "PENDING_REVIEW",
  "category_counts": {
    "NEWS": 1,
    "FINANCIAL_REPORT": 1
  },
  "quality_issues": [
    {
      "record_id": "DEMO001-NEWS-001",
      "field_name": "source",
      "issue_type": "MISSING_REQUIRED_FIELD",
      "severity": "HIGH",
      "evidence_text": "validate_dw_records.quality_issues[0]：source 为空"
    },
    {
      "record_id": "DEMO001-FIN-001",
      "field_name": "observed_at",
      "issue_type": "FUTURE_OBSERVED_AT",
      "severity": "MEDIUM",
      "evidence_text": "validate_dw_records.quality_issues[1]：observed_at 晚于批次 as_of"
    }
  ],
  "dedupe_keys": [
    "market",
    "stock_code",
    "category",
    "source",
    "source_record_id"
  ],
  "target_tables": [
    {
      "category": "NEWS",
      "table_name": "stock_news",
      "business_key": ["market", "symbol", "news_time", "title", "source_id"],
      "evidence_text": "已核验的 NEWS 类别与 stock_news 表结构映射"
    },
    {
      "category": "FINANCIAL_REPORT",
      "table_name": "stock_financial_report",
      "business_key": ["market", "symbol", "indicator", "report_period", "source_id"],
      "evidence_text": "已核验的 FINANCIAL_REPORT 类别与 stock_financial_report 表结构映射"
    }
  ],
  "pending_review": [
    {
      "record_id": "DEMO001-NEWS-001",
      "reason": "缺少可追溯来源，不能进入自动入湖流程",
      "evidence_text": "validate_dw_records.quality_issues[0]"
    },
    {
      "record_id": "DEMO001-FIN-001",
      "reason": "观测时间晚于批次时间，需要核对数据时区或来源时间",
      "evidence_text": "validate_dw_records.quality_issues[1]"
    }
  ],
  "missing_data": [
    "DEMO001-NEWS-001.source",
    "DEMO001-FIN-001 的可信 observed_at"
  ],
  "confidence": 0.92,
  "evidence_text": "validate_dw_records：共检查 2 条虚构记录，发现 2 条待审核质量问题"
}
```

`DEMO001`、记录 ID、时间和问题内容均为结构示例，不依赖真实证券、行情或数据库记录。
