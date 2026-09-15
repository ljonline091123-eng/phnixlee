# ONDEMAND_DATA_DISTILLER

类型：`PROMPT_SOP`。配套可执行工具：`ONDEMAND_DATA_DISTILLER_TOOL`。
任务路由：`data_distillation`。输入仅限已按股票代码和文档 ID 定位、分块后的业务原文。禁止把全市场原始数据或整库记录发送给模型。

## 目标

针对同一份可溯源新闻、公告或财报文本，同时产出用于语义检索的问答式 `kb_chunks` 和用于图谱校验的 `kg_triplets`。输出是提取建议，不直接写入向量库或 Neo4j。仅陈述原文可核验的投资事实；推断、建议和事实必须分开。

## 执行顺序

1. 调用 `trigger_ondemand_extraction(stock_code, doc_id)` 读取原文及 SHA-256。`doc_id` 是 `knowledge_document.id`；工具验证其来源业务记录与股票代码一致。若没有正文，标记抓取待完成并停止。
2. 调度方在 Python 中将长文本按语义边界分块，保留文档 ID、业务表主键、股票代码、标题、来源 URL、抓取时间及块序号。每次只把一个有界文本块交给模型，不发送重复全文。
3. 为每个有投资意义的文本块生成可独立理解的问答对。`chunk_text` 保留支持答案的原文语境；`metadata.question` 和 `metadata.answer` 用于检索，不得编造未披露数字、日期或因果关系。
4. 仅抽取三种有明确原文证据的关系：`Company-BELONGS_TO-Industry`、`Company-SUPPLIES_TO-Company`、`Company-IMPACTED_BY-Event`。公司键为证券代码；行业键为规范名称；事件键为 `<标题>|<YYYY-MM-DD>`。证据不足时不要生成三元组。
5. 每条关系附 `confidence`（0 到 1）和短原文证据 `evidence_text`。低于 0.8、无法核验的新公司/行业/事件节点或来源冲突项进入人工审核，不得自动 MERGE。不得把“可能供应”写成确定的 `SUPPLIES_TO`。
6. 返回且只返回严格 JSON。调度方用 `DistillationOutput` Pydantic Schema 验证，校验失败则整块重试或记为失败。知识库分块与图谱写入是独立轨道，分别更新 `kb_status` 和 `kg_status`；一轨失败不得覆盖另一轨成功结果。

## 严格输出契约

顶层只能包含 `kb_chunks` 和 `kg_triplets` 两个数组，不使用 Markdown 代码块。每个 `kb_chunks` 元素只能包含 `chunk_text` 与 `metadata`；`metadata` 必须包含 `knowledge_document_id`、`source_table`、`source_record_id`、`stock_code`、`question`、`answer`，可选 `source_url`。每个 `kg_triplets` 元素必须包含 `head`、`relation`、`tail`、`confidence`、`evidence_text`。`head`/`tail` 必须包含 `kind`、`key`、`name`；`Event` 还必须包含 ISO 日期 `date`。类型与关系值仅允许 `backend/app/schemas/distillation.py` 中的枚举值，额外字段一律拒绝。

```json
{
  "kb_chunks": [
    {
      "chunk_text": "示例公司于示例日期公告其所在行业为汽车零部件。",
      "metadata": {
        "knowledge_document_id": 12,
        "source_table": "stock_notice",
        "source_record_id": 34,
        "stock_code": "000001",
        "question": "示例公司所属什么行业？",
        "answer": "公告列示为汽车零部件。"
      }
    }
  ],
  "kg_triplets": [
    {
      "head": {"kind": "Company", "key": "000001", "name": "示例公司"},
      "relation": "BELONGS_TO",
      "tail": {"kind": "Industry", "key": "汽车零部件", "name": "汽车零部件"},
      "confidence": 0.94,
      "evidence_text": "其所在行业为汽车零部件"
    }
  ]
}
```

示例仅解释格式，不代表真实证券事实。若没有可信内容，返回空数组；不得补造实体。模型输出中的股票代码、日期、证据及关系类型仍需由调度方对照来源与图谱白名单复核。
