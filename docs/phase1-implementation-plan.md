# 第一期启动实施方案（Phase 1：跑通端到端基线）

> 制定时间：2026-10-14
> 前置文档：[target-architecture-alignment.md](target-architecture-alignment.md)、[接管报告.md](../接管报告.md)
> 一句话目标：**四周内，让"变化检测 → 初筛 → 语义研判 → 落账 → 基准相对复盘"每晚无人值守自动跑通，产出第一份可归因的基线报告。**

---

## 一、第一期目标与退出标准

### 目标
在**圈定的股票池**（不是全市场）上，每晚自动执行：
1. 刷新池内股票的 K 线 / 公告 / 新闻数据；
2. 检测确定性"变化事件"（放量、突破、新公告等）；
3. 用中档模型给新公告/新闻打语义事件标签；
4. 刚性规则初筛出 ≤50 只候选；
5. LLM 研判 Skill 基于证据输出结构化预测（T+20），经受控写入落预测账本；
6. T+N 复盘时计算**相对行业基准的超额收益**；
7. 生成月度基线报告（命中率 / 超额 / 按事件类型 / 按 Skill 版本）。

### 退出标准（满足才算第一期完成）
| # | 标准 | 度量 |
|---|---|---|
| 1 | 管道连续 5 个交易日无人值守运行成功 | scheduler/worker 日任务全绿 |
| 2 | 累计登记 ≥30 条预测 | PredictionLedger 计数 |
| 3 | ≥20 条完成 T+5 部分复盘、首批 T+20 完整复盘 | status=EVALUATED |
| 4 | 基线报告脚本可重复产出 | phase1_baseline_report.py |
| 5 | 所有关键参数版本化（初筛参数、检测阈值） | 配置文件带版本号 |
| 6 | **基线出来前，禁止调任何阈值/规则** | 防过拟合纪律 |

### 明确不做（第一期边界）
- ❌ 不做知识图谱关联（L2 完整版留第二期）
- ❌ 不做全市场扫描（只跑圈定池，全市场留给二期）
- ❌ 不做 Meta-Agent 自动优化（只积累复盘数据，不改规则）
- ❌ 不做新前端界面（用现有控制台 + 报告脚本）
- ❌ 不碰冻结模块（图谱可视化/湖仓校验/治理工作台）

---

## 二、Day 1 启动清单（今天就做的事）

| # | 事项 | 产出 | 依赖 |
|---|---|---|---|
| 1 | **确认模型 Key**：模型实验室里 DeepSeek 或 Gemini 至少一个 provider 有效（测试按钮通过） | 可用 instance_code | 已有 Model Hub |
| 2 | **圈定股票池**：向现有 `watchlist_item` 表灌入 50–100 只（建议：自选股 + 2–3 个重点行业的成分股） | WatchlistItem 行数 | 已有表结构 |
| 3 | **初始化池数据**：对池内每只股票执行一次按需抓取（K 线 + 公告 + 新闻，persist=true），把存量数据补齐 | StockKline/StockNotice/StockNews 有数据 | 已有 StockOnDemandService |
| 4 | **确认 worker/scheduler 在跑**（本地双终端或 docker compose） | 三表队列可见日任务 | 已有 |

> 股票池建议先小后大：**50–100 只**足够在 4 周内积累 ≥30 条事件驱动的预测；池越大，单票数据与研判成本越高，但基线样本并不会成比例变多（因为触发事件的是少数）。

---

## 三、工程步骤（按依赖顺序，每步含验收）

### Step 1：池数据夜刷新任务（新任务类型 `watch_pool_data_refresh`）

- **新增**：`app/jobs` 注册新 task_type；orchestration 加 `WatchPoolRefreshWorkflow`。
- **复用**：`StockOnDemandService.fetch_kline/fetch_notices/fetch_news`（persist=true）、`DataFetchLog`、三表队列幂等键 `watch_pool_data_refresh:{day}`。
- **逻辑**：遍历 `WatchlistItem`，逐只增量刷新最近 N 日 K 线 + 近 7 日公告/新闻；单只失败记日志不中断整批（与现有同步日志纪律一致）。
- **验收**：连续两晚运行后，池内每只股票 `StockKline.trade_date` 覆盖最近交易日，公告/新闻有新行。

### Step 2：L0 确定性变化检测（核心新增，纯代码无 LLM）

- **新表** `stock_change_event`：`market, stock_code, event_date, event_type, signal_json, source_ids_json, dedup_key(唯一约束), detected_at, status`。
- **新增** `app/services/change_detection.py`：纯函数检测器，输入从 `StockKline`/`StockNotice`/`StockNews` 查询，输出事件行：
  - `PRICE_MOVE`：|涨跌幅| ≥ 阈值（复用 `filter_watch_candidates` 现有逻辑与阈值口径）
  - `VOLUME_SPIKE`：量比 = volume / 20日均量 ≥ 阈值
  - `BREAKOUT_20D`：收盘价创 20 日新高/新低
  - `MA_CROSS`：MA5 上穿/下穿 MA20
  - `NOTICE_CLUSTER`：近 3 日新增公告 ≥ N 条（携带 notice_ids）
  - `NEWS_CLUSTER`：近 3 日新增新闻 ≥ N 条（携带 news_ids）
- **新任务** `l0_change_detection`（夜任务，排在 Step 1 之后）。
- **关键纪律**：检测器全部是**可单测的纯 Python**（每个检测器一组正反例单测）；阈值先取 `filter_watch_candidates` 现有默认值，参数进版本化配置。
- **验收**：给定两个交易日的池数据，事件表行数与手工核算一致；dedup_key 保证同日不重复。

### Step 3：L0b 语义事件打标（批量、中档模型、限定输入）

- **新 Skill**：`CHANGE_EVENT_SEMANTIC_TAGGER`（文档放 `backend/skill_docs/`，注册进 `core_skill_defs.py`，模式抄 `NEWS_NOTICE_EVENT_GOVERNOR`）。
- **输入约束**：只喂"当日新增公告/新闻的标题 + 摘要"（限长），逐条输出严格 JSON：`{event_type, sentiment, urgency, summary, confidence}`。
- **事件类型字典（首版 12 类）**：并购重组 / 产能扩张 / 业绩预增 / 业绩预减 / 回购增持 / 减持 / 股权变动 / 风险警示 / 订单合同 / 政策行业利好 / 政策行业利空 / 其他（含"无法判断"显式拒答）。
- **路由**：task_type `event_semantic_tagging` → 便宜实例（DeepSeek/Gemini Flash 级），fallback MOCK；失败不阻塞确定性事件入库。
- **落库**：标签写进对应 `stock_change_event.signal_json`，保留 notice_id/news_id 作为证据。
- **验收**：抽查 20 条打标结果，类型准确率人工评估 ≥80%；拒答路径可触发。

### Step 4：L1 刚性初筛（确定性、参数版本化）

- **新增** `app/services/screening.py` + 配置 `SCREENING_PROFILE_V1`（单一 JSON，含全部阈值与版本号）。
- **输入**：当日事件表 + 池；**输出**：`screening_result` 表（date, stock, event_ids, passed_rules_json, blocked_reasons_json, score, profile_version）。
- **规则（首版）**：非 ST；20 日日均成交额 ≥ 阈值；价格区间过滤；事件加权评分排序；**候选 ≤50**。
- **关键纪律**：每只股票**记录被哪条规则放行/拦截**（为第二期分层归因备料）；LLM 零参与。
- **验收**：同输入重跑结果完全一致（确定性）；候选数 ≤50。

### Step 5：L3 最简研判 Skill（证据锚定 + 受控落账）

- **新 Skill**：`CHANGE_SIGNAL_ANALYST.SKILL.md`（或直接扩展现有 `STOCK_TREND_ADVISOR`，建议新建保持隔离，便于版本对比）。
- **输入 bundle（服务端组装，不给模型自由检索权）**：
  1. 该股当日全部事件（确定性 + 语义标签，含 id）
  2. 20 日 K 线摘要（收盘/量/均线位置，服务端算好）
  3. 触发公告/新闻的标题摘要（带 notice_id/news_id）
- **输出**：符合 `PredictionCreate` 的 JSON——每条含 `market, stock_code, action_type, target_timeframe=T+20, entry_price(取最新收盘价), reasoning_logic, confidence, evidence_ids`；**允许显式拒绝**（`{"predictions":[]}` + 拒绝理由）。
- **落账**：走现有 `record_agent_predictions` + `PREDICTION_LEDGER_WRITER` 受控写入（幂等键 `phase1:{date}:{stock}`）。**注意走通用账本路径，不要用 SelectionCandidate 十日跟踪那条线**（两条复盘线会互相排除，选通用线）。
- **服务端硬校验**：evidence_ids 必须真实存在（查事件/公告/新闻表），引用不存在直接拒绝该条预测。
- **成本护栏**：每日研判上限 20 条预测；并发限速；路由 task_type `stock_analysis`，主用可用实例、fallback MOCK（MOCK 结果会照常落账但标记 model_instance_code，报告时单独统计）。
- **验收**：连续 3 晚产出预测；evidence_ids 抽查全部可回溯到真实行；拒绝路径有记录。

### Step 6：L4 基准相对复盘（增强现有复盘，不重建）

- **新增日任务** `benchmark_kline_sync`：只同步少数基准指数日 K（所属行业指数 + 沪深300，经由 AKSHARE 指数接口，池小所以成本低）。
- **扩展** `prediction_review.py` 的评估输出：在 `evaluation_json` 中追加 `benchmark_code, benchmark_return_pct, excess_return_pct(=actual−benchmark), benchmark_direction_hit`；**不动已有字段**（向后兼容）。
- **映射**：用现有 `stock_classification`/taxonomy 的行业归属给每只股票找基准指数；找不到行业基准的回退沪深300。
- **验收**：历史已 EVALUATED 的样本重算可得超额字段；T+20 到期样本自动带基准对比。

### Step 7：基线报告脚本

- **新增** `backend/scripts/phase1_baseline_report.py`（这是**运维报告脚本**，不是自验脚本，长期保留）：
  - 总体：方向命中率、平均超额、中位超额、拒答率
  - 分组：按 event_type、按语义标签、按 skill_version、按 model_instance（MOCK 单列）
  - 数据质量：每晚管道各环节成功率（从三表队列取）
- **验收**：可重复执行；输出 Markdown 报告到 `docs/baselines/`。

---

## 四、四周排期

| 周 | 交付 | 验收 |
|---|---|---|
| **W1** | Day1 清单 + Step1 池刷新 + Step2 确定性检测（含单测） | 两晚数据齐、事件表有真实输出 |
| **W2** | Step3 语义打标 + Step4 初筛 | 打标准确率抽查 ≥80%；候选 ≤50 确定性可重放 |
| **W3** | Step5 L3 研判落账 + Step6 基准复盘 | 连续 3 晚预测落账；到期样本带超额字段 |
| **W4** | Step7 基线报告 + 5 晚无人值守试运行 | 退出标准全达成，出首份基线报告 |

> W2 起每天产出的小样本预测**照常入账但不动任何规则**——基线前的调参=对着噪声拟合。

---

## 五、风险与对策

| 风险 | 对策 |
|---|---|
| 上游数据源不稳定（AkShare 接口变动） | 已有多源 fallback 链；失败写同步日志不污染存量数据；单只失败不阻塞整批 |
| 模型额度/费用超预期 | 池 ≤100、日预测 ≤20、打标只用 Flash 级、拒答免费；MOCK 兜底保证管道不断 |
| 事件太少，4 周攒不够 30 条 | 扩池到 200 或放宽 NOTICE_CLUSTER 阈值（**这是唯一允许的"调参"**，且要记录版本） |
| MOCK 预测污染基线 | 报告按 model_instance 分组，MOCK 单列不计入主指标 |
| 两条复盘线打架（Selection vs 通用） | 第一期全部走通用 PredictionLedger 线（代码里已互相排除，选一条） |

---

## 六、与现有代码的映射（改动面预估）

| 改动 | 文件 | 性质 |
|---|---|---|
| 新表 ×2 | `stock_change_event`, `screening_result`（+ models 注册） | 新增 |
| 新服务 ×3 | `change_detection.py`, `screening.py`, `event_bundle.py`(证据组装) | 新增 |
| 新任务 ×4 | 池刷新 / L0 检测 / 语义打标 / 基准同步（注册进 TASK_HANDLERS + scheduler） | 新增 |
| 新 Skill ×2 | `CHANGE_EVENT_SEMANTIC_TAGGER`, `CHANGE_SIGNAL_ANALYST`（skill_docs + core_skill_defs） | 新增 |
| 扩展 ×1 | `prediction_review.py` 加基准字段（向后兼容） | 修改 |
| 报告 ×1 | `scripts/phase1_baseline_report.py` | 新增 |
| **不动的** | Model Hub / 受控写入 / 三表队列 / F10 / 前端 | 复用 |

改动面集中在"新增"，对存量代码几乎零侵入——这正好符合接管报告"增量扩展、不重置"的协作约定。
