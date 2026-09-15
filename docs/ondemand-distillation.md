# 按需加载与双轨蒸馏增量设计

本设计沿用当前的数据资产、知识库、图谱、治理运行、模型路由和 Skill 注册结构。当前开发环境默认 SQLite；[PostgreSQL 增量迁移](../backend/sql/postgresql/20260915_ondemand_distillation.sql)只供目标 PostgreSQL 16 环境审核、备份后执行，未对本地 SQLite 运行。Neo4j 与向量库属于外部写入目标，当前仓库没有连接配置或可验证的部署；新增调度 Agent 和路由默认停用。

## 现有表映射

| 阶段 | 表与字段 | 责任 |
| --- | --- | --- |
| 主数据锚点 | `stock_symbol.market/symbol/name` | 证券代码、名称和市场由 SQL 全量维护，先建立可信公司骨架。 |
| 按需业务原文 | `stock_news.content/content_json`、`stock_notice.content_json`、`stock_financial_report.data_json`，新增 `content_raw/fetch_status/is_distilled` | 仅把真实原文保存为 `content_raw`；数值型财报 JSON 不等于研报全文。`fetch_status` 管抓取，不管蒸馏。 |
| 统一文档资产 | `knowledge_document.knowledge_base_id/graph_id/source_table/source_record_id/market/symbol/title/content/metadata_json` | 每个业务记录映射一份可追溯知识文档；`content` 是清理后的文本，`metadata_json` 记录 URL、来源时间、SHA-256、版本。`graph_id` 可为空，知识库可以包含多张独立图谱。 |
| 双轨执行 | 新增 `asset_distillation_task` | 同一来源、知识库、图谱、内容哈希唯一；`knowledge_document_id` 是统一文档 ID，`source_doc_id` 是业务表行 ID。KB/KG 各自有 PENDING/DONE/FAILED 状态和重试计数。 |
| 治理审计 | `governance_run` 与任务的 `governance_run_id` | 汇总一次治理运行，不替代每条文档、每条轨道的详细状态。 |
| 现有 SQL 图谱 | `knowledge_entity/knowledge_relation` | 保留原功能，不能把其全量重建函数用于增量任务。Neo4j 是新增图目标；若要双写，需单独定义一致性和回放策略。 |

业务记录成功抓取并落库后，由异步队列创建/定位 `knowledge_document`，计算原文 SHA-256，并 `INSERT ... ON CONFLICT` 入队。用户查看某只股票或盯盘预警只触发该股票缺失或过期数据的抓取，不扫描整市场。工作者按 `next_retry_at` 领取任务，分别完成：语义分块、Embedding/向量 upsert；实体校验、人工审核分流、Neo4j MERGE。向量 ID 建议使用 `SHA-256(kb_id|knowledge_document_id|content_sha256|chunk_index)`，保证重试幂等。只有两轨都 DONE 才把相应版本视作完成；业务表 `is_distilled` 仅作派生快捷标志，具体进度以任务为准。新版本原文入队时将其重置为 false。

已有新闻正文可回填 `content_raw`；公告和财报只从明确的 `content/body/text` 字段回填。只有标题、URL 或纯财务数字时保留 PENDING，先抓全文。当前的 `trigger_ondemand_extraction(stock_code, doc_id)` 从白名单业务表读取单条原文并校验股票代码，不调用模型；调度方必须把长文本有界分块后再调用模型。Pydantic 的 [DistillationOutput](../backend/app/schemas/distillation.py) 拒绝未知字段、关系类型错配及缺失日期，并可用 `model_json_schema()` 填入 Agent 的 `json_schema_output`。

## 图模型与审核

[Cypher 文件](../backend/cypher/ondemand_graph.cypher)创建按 `graph_code` 隔离的 Company(stock_code)、Industry(name)、Event(title,date) 复合唯一约束，并分别给 BELONGS_TO、SUPPLIES_TO、IMPACTED_BY 提供固定模板。调度方只把验证通过且审核通过的三元组按关系类型分组传入参数，不拼接模型输出的标签或关系名。节点和关系都携带 `graph_code`，使同一知识库下的多张图谱相互独立。导入前应从 `stock_symbol` 和人工确认的行业字典创建公司/行业骨架，否则初次导入会因“新节点需审核”产生大量草稿。

调度 Agent 编码 `DISTILLATION_GOVERNANCE_AGENT`，绑定两条 Skill：`ONDEMAND_DATA_DISTILLER`（PROMPT_SOP）与 `ONDEMAND_DATA_DISTILLER_TOOL`（EXECUTABLE_TOOL）。路由 `data_distillation` 预设 DeepSeek 主模型、Gemini Flash 降级，默认停用；需要先配置有效 API Key 并验证结构化输出。任一三元组置信度低于 0.8、实体不在已审核骨架或与已有事实冲突时，写入 `skill_optimization_draft`，`draft_type='DISTILLATION_REVIEW'`，`failure_signature` 以 `DISTILLATION_REVIEW:` 开头，`review_context_json` 保存 `task_id`、三元组、证据、冲突项；`proposed_instructions` 仅保留现有 Skill 原文以满足旧字段非空约束，不能被当成 Prompt 修订。现有 Skill 草稿批准 API 已拒绝此前缀。正式启用前还需实现专门的实体审核动作；批准实体时才允许入图，拒绝后保留审计记录。

任务获取建议使用 PostgreSQL `FOR UPDATE SKIP LOCKED`，每条轨道单独事务和指数退避。达到上限转 FAILED 并保留 `error_log`；人工审核期间不标为 DONE。对同源同版本使用任务唯一约束避免并发重复入队。迁移前检查 `knowledge_document` 同一来源/目标是否已有重复记录，再决定是否增加文档级唯一索引，不应在未知历史数据上直接加约束。

## 部署顺序

1. 备份目标 PostgreSQL，核对历史数据和目标知识库/图谱 ID；执行增量 SQL。
2. 在 Neo4j 5 执行约束，再以已验证的主数据创建公司/行业节点。
3. 配置真实模型、向量库与 Neo4j 连接，补齐任务领取/写入器和实体人工审核动作；验证一条新闻、公告、财报的两轨成功、单轨失败重试、重复入队及新节点审核。
4. 最后启用 `data_distillation` 路由和调度 Agent；不要让 Mock 模型输出进入事实库。
