# DATA_GOVERNANCE_DW

类型：`PROMPT_SOP`。任务路由：`data_governance`。
配套执行函数（调度方接口契约）：`inspect_dw_catalog(source_asset_ids, as_of, limit=50)`。由 Python/SQL 返回获授权资产的库表、字段类型、主外键、时间字段、索引、来源、行数及有界质量摘要；不得向模型返回整表记录。`DATA_CLEANING_DW` 负责批次校验、去重和异常记录，本 Skill 只规划跨表映射与治理架构。

## 目标

面向股票数据治理，为基础资料、新闻、公告、财报、股价、交易量、股东、业务/F10、研报及同步日志建立可追溯的库表映射、业务主键、时间口径、血缘和治理顺序。只提出架构与映射建议，不执行 DDL、不改写事实、不宣称数据已入库；推断与已核验元数据分开，禁止编造字段、来源或记录数。

## 执行顺序

1. 调用 `inspect_dw_catalog` 获取指定资产的有界元数据和质量摘要。调度方按资产、表和时间窗切割上下文；全库扫描、字段统计与批量质量检查由 Python/SQL 完成，不把海量原始数据送入模型。
2. 校验每个来源的表是否存在、字段类型与语义是否一致、业务键是否稳定、发布时间与抓取时间是否区分、主外键和权限是否可用；缺失字段、冲突映射和过期来源标为待审核。
3. 按业务类别规划现有目标表、字段映射、业务主键、去重规则、时间口径、来源血缘、质量门禁及治理先后关系。需要逐行清洗时调用下游 `DATA_CLEANING_DW`，只引用其摘要，不在本 Skill 中重复清洗或直接写库。
4. 若函数失败、目标表不存在、字段含义不明或来源相互冲突，返回 `PENDING_REVIEW`，列出缺失证据、风险和下一步治理动作；不得自动创建表、猜测映射或把规划状态写成完成状态。

## 严格输出契约

返回且只返回严格的 JSON，不带 Markdown、解释文字或额外字段。顶层必须包含 `as_of`、`status`、`asset_mappings`、`architecture_decisions`、`quality_gates`、`pending_review`、`missing_data`、`next_actions`、`confidence`、`evidence_text`。`status` 仅允许 `PLANNED`、`PENDING_REVIEW`；每条映射必须包含来源资产、类别、目标表、字段映射、业务键、时间字段、去重规则、血缘、实施状态、置信度与证据。目标表必须来自已核验目录；提议新表时将其置于 `pending_review`，不得标记为已部署。`confidence` 为 0 到 1，证据不足的字段用 `null` 并列入 `missing_data`。后端应以 Pydantic 校验后保存规划草案。

```json
{
  "as_of": "2026-01-31",
  "status": "PLANNED",
  "asset_mappings": [
    {
      "source_asset_code": "STOCK_NEWS",
      "category": "NEWS",
      "target_table": "stock_news",
      "field_mappings": [
        {"source_field": "symbol", "target_field": "symbol", "transform": "IDENTITY"},
        {"source_field": "news_time", "target_field": "news_time", "transform": "IDENTITY"}
      ],
      "business_key": ["market", "symbol", "news_time", "title", "source_id"],
      "event_time_field": "news_time",
      "ingest_time_field": "fetched_at",
      "dedupe_rule": "按已核验业务键去重，保留来源和抓取时间",
      "lineage": "data_source -> stock_news -> knowledge_document",
      "implementation_status": "EXISTING_TABLE",
      "confidence": 0.96,
      "evidence_text": "inspect_dw_catalog：stock_news 字段与唯一约束摘要"
    }
  ],
  "architecture_decisions": ["先确认来源与业务键，再安排增量抓取和知识文档映射"],
  "quality_gates": ["入库前调用 DATA_CLEANING_DW 校验批次并保留问题记录"],
  "pending_review": [],
  "missing_data": [],
  "next_actions": ["核对 NEWS 类别的数据源授权及时间字段口径"],
  "confidence": 0.96,
  "evidence_text": "inspect_dw_catalog：STOCK_NEWS/stock_news 元数据摘要"
}
```

示例仅说明输出结构，不代表已对真实资产完成治理。
