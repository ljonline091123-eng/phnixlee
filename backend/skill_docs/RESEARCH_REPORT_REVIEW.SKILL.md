# RESEARCH_REPORT_REVIEW

类型：`PROMPT_SOP`。任务路由：`research_report`。
配套执行函数（调度方接口契约）：`load_research_review_bundle(stock_code, report_id, as_of, max_history=5)`。由 Python/SQL 返回指定研报、最多 5 份历史研报、与各研报预测期限匹配的已实现价格和成交量摘要、公告/财报/新闻证据及来源 ID；不得返回全市场行情。函数只读取和计算，不由模型直接执行 SQL。

## 目标

复盘单只股票的历史研报，区分当时可得事实、后来发生的事实与当前推断，评估原结论在其目标期限内是否得到验证。禁止用事后信息改写历史判断，禁止编造价格、收益、公告或预测准确率；单一短期涨跌不足以判定整份研报正确或错误。

## 执行顺序

1. 调用 `load_research_review_bundle` 获取有界证据包。调度方按研报 ID、股票代码和时间窗切割上下文，只向模型传入相关摘要与必要原文片段，不传全量研报库或全市场价格序列。
2. 校验证券代码、报告发布时间、预测期限、复权口径、行情时间、来源 ID 和缺失项；只用报告发布之后且在目标期限内的结果验证预测，保留原报告中的评级、分数、支撑阻力和风险条件。
3. 逐项比较历史论点与已实现经营、量价及事件证据，分别说明得到支持、被证伪或尚无法验证的部分；给出偏差原因、需上调或下调的判断和下一次跟踪指标。每项判断附置信度与证据引用。
4. 若行情缺口、期限未到、来源冲突或函数失败，顶层返回 `INSUFFICIENT_DATA`，对应单项标为 `UNVERIFIABLE`，列出缺失数据和重试条件；不得猜测收益或输出伪精确胜率。

## 严格输出契约

返回且只返回严格的 JSON，不带 Markdown、解释性前后缀或额外字段。顶层必含 `stock_code`、`report_id`、`as_of`、`status`、`historical_reviews`、`overall_conclusion`、`next_tracking_indicators`、`missing_data`、`confidence`、`evidence_text`。`status` 仅允许 `REVIEWED`、`INSUFFICIENT_DATA`；单项 `verification` 仅允许 `SUPPORTED`、`PARTIALLY_SUPPORTED`、`CONTRADICTED`、`UNVERIFIABLE`。所有 `confidence` 为 0 到 1；每个可验证结论的 `evidence_text` 必须给出原文短摘录或来源记录 ID，缺证据时不得声称已验证。后端应以 Pydantic 校验后落库。

```json
{
  "stock_code": "DEMO001",
  "report_id": 102,
  "as_of": "2026-01-31",
  "status": "REVIEWED",
  "historical_reviews": [
    {
      "historical_report_id": 101,
      "published_at": "2025-12-01T09:00:00+08:00",
      "target_timeframe": "T+20",
      "original_conclusion": "样例研报认为业务收入增长需要后续财报验证。",
      "verification": "PARTIALLY_SUPPORTED",
      "actual_outcome": "后续公告确认业务进展，收入数据尚未披露。",
      "deviation_reason": "原预测中的收入条件尚未到验证时点。",
      "adjustment": "保留业务进展判断，延后收入结论。",
      "confidence": 0.68,
      "evidence_text": "research_report#101；stock_notice#201@2025-12-15"
    }
  ],
  "overall_conclusion": "已有事件证据仅支持部分论点，不能认定整份历史研报预测准确。",
  "next_tracking_indicators": ["下期收入披露", "公告所述业务进度"],
  "missing_data": ["目标期财报收入数据"],
  "confidence": 0.65,
  "evidence_text": "research_report#101；stock_notice#201@2025-12-15"
}
```

`DEMO001` 及引用 ID 仅为结构示例，不代表真实证券或已存在的数据库记录。
