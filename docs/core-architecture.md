# GEMINI QUANT 核心模型、Skill 与复盘链路

## 四层应用架构

后端按以下依赖方向组织，外层可以调用内层，内层不依赖 FastAPI：

| 层级 | 路径 | 当前职责 |
| --- | --- | --- |
| 表示层 | `backend/app/api/` | Request/Response Schema、HTTP 状态码、响应头和异常映射。 |
| 编排层 | `backend/app/orchestration/` | 串联仓储、数据源、领域服务和模型路由；F10、Agent/Skill、每日同步与复盘均通过 Workflow 执行。 |
| 领域服务层 | `backend/app/services/` | F10 合并规则、行情抓取、模型路由、Skill 硬规则、治理与复盘等原子能力。 |
| 数据与适配器层 | `backend/app/repositories/`、`backend/app/db/`、`backend/app/connectors/` | 查询模型、ORM 会话、第三方行情和模型供应商适配。 |

`GET /stocks/{market}/{symbol}/f10` 已迁移到 `F10Workflow`。路由只处理 HTTP 缓存头和把应用异常映射成 404/422；`StockF10Repository` 集中查询，`services/f10.py` 保存无 HTTP 依赖的缓存、合并和标准化规则。模型对话和 Agent 执行分别使用 `ModelInvocationWorkflow`、`AgentExecutionWorkflow`；确定性 Skill 工具统一经 `SkillToolWorkflow` 调用，因此 REST、后台任务和未来的批处理入口可以复用同一条业务链。

## 独立任务调度与状态机

FastAPI `lifespan` 只初始化数据库和默认目录，不再创建常驻定时协程。Docker 部署包含三个独立后端角色：

- `api`：接收 HTTP 请求；
- `scheduler`：按上海时区生成每日主数据同步和 T+N 复盘任务；
- `worker`：领取任务并执行 `orchestration/background.py` 中的 Workflow。

任务使用 PostgreSQL/SQLite 内的三张表形成轻量可靠队列：

| 表 | 用途 |
| --- | --- |
| `scheduled_job` | 幂等键、计划时间、Worker 租约、最大尝试次数、重试时间和最终状态。 |
| `pipeline_run` | 一次完整业务工作流的输入、输出、触发来源、当前阶段和总体状态。 |
| `pipeline_stage_run` | 每个阶段的尝试、租约、错误和结构化结果，后续可扩展数据采集、清洗、蒸馏、入图等多阶段 Pipeline。 |

PostgreSQL Worker 使用 `FOR UPDATE SKIP LOCKED` 竞争任务。领取后立即提交事务，再执行外部行情或模型调用；成功或失败由新事务落库。Worker 崩溃后，其他 Worker 可以回收租约已经过期的 `RUNNING` 任务。`idempotency_key` 防止多实例 Scheduler 重复创建同一业务日任务。

## 两层筛选

Python/SQL 对全市场量价、财务和数据质量做批量计算。`filter-watch-candidates` 最多接收 5,000 条已聚合指标，每次最多返回 50 只候选股。Agent 只接收候选池、证据摘要、研报片段和知识图谱关系；`stock_screening`、`stock_analysis`、`risk_warning`、`data_governance` 调用还受 60,000 字符及 50 只候选股的服务层边界约束。不要通过普通 `general_chat` 端点传输全市场原始表。

## 数据模型与配置

默认长文实例使用 `gemini-2.5-pro`，Meta-Agent 降级链包含 `claude-sonnet-5`。用户提出的 Gemini 1.5 Pro 已于 2025 年 9 月停用，因此未将它设为可调用的默认实例；模型代码仍可在模型实验室按供应商实际可用版本修改。参考 [Google 官方发布记录](https://ai.google.dev/gemini-api/docs/changelog) 和 [Claude 官方模型清单](https://platform.claude.com/docs/en/models/overview)。

| 实体 | 主要字段和关系 |
| --- | --- |
| `ModelProvider` | `provider_code`、`provider_name`、`provider_type`、`api_base_url`、`api_key_encrypted`、`enabled`；一个供应商可有多个实例。新增密钥需要进程环境变量 `MODEL_CREDENTIAL_KEY`，API 只返回 `api_key_configured`。 |
| `ModelInstance` | `provider_id`、`instance_code`、`model_code`、`temperature`、`top_p`、`max_tokens`、`usage_type` (`EXACT` / `CREATIVE`)；默认复用供应商密钥和 Base URL。 |
| `ModelRouteRule` | `task_type`、`preferred_instance_code`、非空 `fallback_chain_json`；主模型及降级实例必须存在且不能重复。遇到 429、5xx、网络错误或超时，按顺序重试；400 等请求错误不重试。显式指定 Agent 模型时，它先于任务路由的降级链。 |
| `AgentDefinition` | `agent_code`、`system_prompt`、`context_window_limit`、`json_schema_output`；可绑定子智能体、Skill、知识库、数据资产和外部数据源。Agent 运行时保留限定轮数，并校验 JSON Schema 输出。 |
| `ModelSkill` / `ModelSkillRevision` | `skill_type` 为 `PROMPT_SOP` 或 `EXECUTABLE_TOOL`；Markdown 正文位于 `backend/skill_docs/<CODE>.SKILL.md`。编辑器保存到文件，外部文件编辑在启动或读取时导入 DB 并生成不可变修订。回滚创建新版本，不覆盖历史。 |
| `PredictionLedger` / `SkillOptimizationDraft` | 预测方向、时间窗、理由、Skill 版本、入场价、实际价和复盘结果；连续三次方向失败时，Meta-Agent 只生成待审核 Prompt 草稿，由用户批准或驳回。 |

旧实例级明文密钥在设置 `MODEL_CREDENTIAL_KEY` 且供应商下密钥一致时，会在启动时迁移到供应商加密字段。若同一供应商下历史密钥不同，启动流程会保留它们，供管理员整理为共享供应商凭证；不会静默删除冲突密钥。设置新的供应商密钥会清除其实例的旧明文密钥。原来的配置文件或环境变量无需提交到 Git。

## 首批三个核心 Skill

| Skill | Python 函数/API | Agent 只接收 |
| --- | --- | --- |
| `DATA_CLEANING_DW` | `validate_dw_records` → `POST /api/v1/model-hub/skill-tools/validate-dw-records` | 分类计数、业务键重复/缺失/异常值摘要与最多 50 条问题样本；Prompt 指明待人工确认的 DW 入湖步骤。 |
| `WATCH_ALERT` | `filter_watch_candidates` → `POST /api/v1/model-hub/skill-tools/filter-watch-candidates` | 价量异动排名后的至多 50 只候选股及触发原因；Agent 再结合新闻、公告和知识图谱解释风险。 |
| `RESEARCH_REVIEW_CORRECTION` | `score_prediction_outcomes` → `POST /api/v1/model-hub/skill-tools/score-prediction-outcomes` | 已到期预测的方向命中、收益率差距和证据来源；Meta-Agent 只能提交修订建议。 |

每个 Markdown 文件内均包含输入边界、函数参数、推荐输出 JSON 结构和不得捏造行情的约束。函数端点由 Pydantic 校验并用 Python 确定性计算，不向大模型发送原始批量数据。`EXECUTABLE_TOOL` 类型的 Skill 必须在 `config_json.function_spec` 中声明名称和参数；新建它不会执行任意上传代码。

## API 与复盘

- `GET|POST|PUT|DELETE /api/v1/model-hub/providers|instances|routes|skills` 管理四类清单；供应商和实例分别有 `/test` 连通性测试。
- `GET /api/v1/model-hub/skills/{id}/revisions` 查看历史，`POST /skills/{id}/revisions/{revision_id}/rollback` 回滚；Skill 编辑弹窗显示版本历史。
- `POST /api/v1/resources/agents/{id}/run` 执行 Agent。明确的买入、卖出或持有建议需使用 JSON `prediction` 或 `predictions`，包含 `market`、`stock_code`、`action_type`、`target_timeframe` (`T+1` 至 `T+30`)、`reasoning_logic`、绑定的 `skill_code` 和来自行情证据的 `entry_price`。成功记录后响应返回 `prediction_ids`；缺字段或未绑定 Skill 的结构化建议会被拒绝。
- 研究中心保存研报时，若有有效行情价，将现有 A/B/C/D 评级映射到 `BUY` / `HOLD` / `SELL`，同时写入 T+5 预测账本，并关联研报与当时的 Skill 版本。没有有效价时研报仍保存，但不生成无法复盘的预测。
- `GET|POST /api/v1/model-hub/predictions` 查询或登记预测；`POST /predictions/review` 手动复盘。每天上海时间零点后 10 分钟自动尝试拉取到期日线并复盘；T+N 按第 N 个交易日的收盘价计算，缺少行情时保持 `PENDING`。
- `GET /api/v1/model-hub/skill-drafts` 查看 Meta-Agent 草稿；`POST /skill-drafts/{id}/approve|reject` 审核。模型实验室的 Skill 页也显示待审核建议。草稿不会自动覆盖当前 Prompt，且批准时会再次核对基础版本。

密钥与模型调用依赖实际配置和网络。默认 `MOCK_GENERAL` 仅用于离线验证；它的回复不会被当作 Meta-Agent 优化建议。预测账本的命中率只是按指定 T+N 窗口回测方向，并不代表交易后的真实可执行收益。
