# STOCK_KNOWLEDGE_GRAPH_BUILDER

类型：`PROMPT_SOP`。任务路由：`knowledge_graph`。
配套执行函数（调度方接口契约）：`load_stock_graph_evidence(stock_code, knowledge_base_id, graph_id, source_asset_ids, limit=50)`。由 Python/SQL 读取单股最多 50 份已治理文档摘要、既有实体/关系、来源 ID 与图谱白名单；长文先经 `ONDEMAND_DATA_DISTILLER` 分块和提取。函数不接受模型拼写的 SQL/Cypher。

## 目标

围绕单只股票形成可追溯的实体与关系建议，区分来源事实、模型推断和待审核节点。初始 Neo4j 图模型只允许 `Company`、`Industry`、`Event` 及 `BELONGS_TO`、`SUPPLIES_TO`、`IMPACTED_BY`；股东、财报、公告、新闻、价格、成交量、业务和研报等扩展实体需先注册图谱 Schema。禁止编造企业关系、因果链或未披露事件，模型不得自行写 Neo4j。

## 执行顺序

1. 调用 `load_stock_graph_evidence` 获取指定知识库和独立图谱的有界证据包。调度方按 `graph_id`、股票代码、时间和来源切割上下文，不把全市场数据或整库长文交给模型。
2. 校验业务记录与 `knowledge_document` 的来源映射、证券代码、事件日期、实体规范键、图谱白名单及证据文本；重复、过期或互相矛盾的来源须显式标记。
3. 仅根据直接证据提出实体和关系；每条关系写明头尾节点、置信度和来源引用。低于 0.8、图谱中不存在的新节点及冲突关系进入人工审核，审核通过后才由固定 Cypher 模板按 `graph_code` 执行 `MERGE`。
4. 若函数失败、正文缺失、关系超出注册本体或证据不足，返回 `PENDING_REVIEW` 或 `INSUFFICIENT_DATA`，列出冲突和缺失项；不得用猜测补齐“全覆盖”图谱。

## 严格输出契约

返回且只返回严格的 JSON，不带 Markdown 或额外字段。顶层必含 `stock_code`、`knowledge_base_id`、`graph_id`、`status`、`entities`、`relations`、`conflicts`、`pending_review`、`missing_data`、`confidence`、`evidence_text`。`status` 仅允许 `READY`、`PENDING_REVIEW`、`INSUFFICIENT_DATA`。每个实体、关系和待审项均须有 `confidence` 与 `evidence_text`；关系类型及节点类型必须符合当前图谱白名单。`confidence` 为 0 到 1；不得把 `PENDING_REVIEW` 项作为已落库事实。后端应以 Pydantic 校验后决定入库或送审。

```json
{
  "stock_code": "DEMO001",
  "knowledge_base_id": 1,
  "graph_id": 2,
  "status": "PENDING_REVIEW",
  "entities": [
    {"kind": "Company", "key": "DEMO001", "name": "样例公司", "confidence": 0.98, "evidence_text": "stock_symbol#11"},
    {"kind": "Industry", "key": "样例行业", "name": "样例行业", "confidence": 0.93, "evidence_text": "knowledge_document#21"}
  ],
  "relations": [
    {"head_key": "DEMO001", "relation": "BELONGS_TO", "tail_key": "样例行业", "confidence": 0.93, "evidence_text": "knowledge_document#21"}
  ],
  "conflicts": [],
  "pending_review": [{"reason": "NEW_NODE", "entity_key": "样例行业", "confidence": 0.93, "evidence_text": "knowledge_document#21"}],
  "missing_data": [],
  "confidence": 0.93,
  "evidence_text": "stock_symbol#11；knowledge_document#21"
}
```

`DEMO001` 及引用 ID 仅为结构示例，不代表真实证券或已存在的数据库记录。
