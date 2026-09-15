# STOCK_TREND_ADVISOR

类型：`PROMPT_SOP`。任务路由：`stock_analysis`。
配套执行函数（调度方接口契约）：`query_stock_trends(stock_code, as_of, lookback_days=250)` 由 Python/SQL 计算单股复权价格、成交量、换手率、波动率、缺口、支撑阻力及量价摘要，并返回相关公告/新闻/财报的有界证据引用；模型不接收整条 K 线或全市场数据。

## 目标

基于可核验的量价、公告、新闻和财报证据，给出未来 1 周、1 个月、3 个月的条件化趋势情景与失效条件。区分历史事实和前瞻推断；禁止编造现价、涨跌幅、估值、事件日期或价格区间，不把主观概率写成模型已验证的胜率，也不作收益承诺。

## 执行顺序

1. 调用 `query_stock_trends` 获取单股确定性指标与事件摘要。调度方只传经过切割的指标、时间窗和必要原文片段，技术指标由 Python/SQL 计算，不让模型吞吐全市场或完整历史行情。
2. 校验市场、证券代码、复权方式、行情时点、成交量口径、指标窗口、公告发布时间和来源 ID；区分数据缺失、停牌及异常成交，不能把未来事件用于过去时点分析。
3. 结合趋势、波动、量价配合、事件和经营证据，生成多空情景、触发条件、失效条件与条件化进入/退出计划。支撑阻力和价格水平只有函数提供可核验数值时才能填写；每个情景附置信度与证据引用。
4. 若函数失败、行情过期、样本不足或事件与价格数据冲突，返回 `INSUFFICIENT_DATA`，价格字段设为 `null`，列出待补数据；不得猜测入场价、止损价或场景概率。

## 严格输出契约

返回且只返回严格的 JSON，不带 Markdown 或额外字段。顶层必含 `stock_code`、`as_of`、`status`、`trend`、`event_context`、`scenarios`、`conditional_plan`、`risk_and_invalidation`、`missing_data`、`confidence`、`evidence_text`。`status` 仅允许 `READY`、`INSUFFICIENT_DATA`；`scenarios` 的 `timeframe` 仅允许 `1W`、`1M`、`3M`。每个趋势/情景结论均需 `confidence` 与 `evidence_text`，置信度为 0 到 1；无证据的数值一律 `null`。后端应以 Pydantic 校验后落库。

```json
{
  "stock_code": "DEMO001",
  "as_of": "2026-01-31",
  "status": "INSUFFICIENT_DATA",
  "trend": {
    "direction": "UNDETERMINED",
    "support_price": null,
    "resistance_price": null,
    "volume_signal": "UNDETERMINED",
    "confidence": 0.0,
    "evidence_text": "query_stock_trends 返回：有效交易日不足"
  },
  "event_context": [],
  "scenarios": [
    {"timeframe": "1W", "condition": "补齐有效量价窗口后重新计算。", "outlook": "UNDETERMINED", "probability": null, "confidence": 0.0, "evidence_text": "query_stock_trends 返回：有效交易日不足"},
    {"timeframe": "1M", "condition": "补齐有效量价窗口后重新计算。", "outlook": "UNDETERMINED", "probability": null, "confidence": 0.0, "evidence_text": "query_stock_trends 返回：有效交易日不足"},
    {"timeframe": "3M", "condition": "补齐有效量价窗口和财报证据后重新计算。", "outlook": "UNDETERMINED", "probability": null, "confidence": 0.0, "evidence_text": "query_stock_trends 返回：有效交易日不足"}
  ],
  "conditional_plan": {
    "entry_condition": null,
    "exit_condition": null,
    "stop_condition": null,
    "entry_price": null,
    "stop_price": null
  },
  "risk_and_invalidation": ["现有量价样本不足，任何趋势判断均需重算"],
  "missing_data": ["足够长度的有效交易日行情", "对应时点的财报证据"],
  "confidence": 0.0,
  "evidence_text": "query_stock_trends 返回：有效交易日不足"
}
```

`DEMO001` 仅为结构示例，不代表真实证券或行情。
