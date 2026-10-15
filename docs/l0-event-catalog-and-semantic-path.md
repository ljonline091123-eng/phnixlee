# L0 事件清单与语义事件实现路径

> 制定时间：2026-10-15
> 关联文档：[target-architecture-alignment.md](target-architecture-alignment.md) · [phase1-implementation-plan.md](phase1-implementation-plan.md) · [phase1-console-prototype.html](phase1-console-prototype.html)
> 本文回答两个问题：**确定性事件到底有哪些（v1 完整清单）**；**语义事件如何基于现有代码基础实现**。

---

## 一、确定性事件清单（L0_PROFILE_V1）

### 设计原则

1. **纯函数实现**——无 LLM、无网络调用，可单测、可重放（同输入必同输出）；
2. **阈值集中进版本化配置**（`L0_PROFILE_V1`），触发时的全部数值写入 `signal_json`（可审计、可归因）；
3. **去重键** `dedup_key = {market}:{symbol}:{event_type}:{event_date}`，保证同日同类型不重复；
4. **无未来函数**——只使用检测时刻已可得的数据（收盘后跑当日检测）。

### A 组 · 量价类（数据源：`stock_kline`、`stock_realtime_quote`）

| # | 事件代码 | 定义（公式） | 默认阈值 | 数据字段 | 方向 | 优先级 |
|---|---|---|---|---|---|---|
| A1 | `PRICE_MOVE` | 单日涨跌幅 = close/前收−1 | \|涨跌幅\| ≥ 3%（与现有 `filter_watch_candidates` 默认一致；L0 为召回层） | close_price | ± | P0 |
| A2 | `VOLUME_SPIKE` | 量比 = 当日成交量 ÷ 20 日均量 | ≥ 2.0 | volume | ± | P0 |
| A3 | `BREAKOUT_20D` | 收盘创 20 日新高 / 新低 | close > max(前20日high) 或 < min(前20日low) | close/high/low | 上/下 | P0 |
| A4 | `MA_CROSS_5_20` | MA5 与 MA20 交叉 | 上穿或下穿 | close 序列 | 上/下 | P0 |
| A5 | `TURNOVER_SURGE` | 换手率异动（相对自身历史分位） | turnover ≥ 自身60日90分位 且 ≥ 5% | turnover_rate | ± | P1 |
| A6 | `GAP_OPEN` | 跳空缺口 = open/前收−1 | \|缺口\| ≥ 3% | open/前收 | 上/下 | P1 |
| A7 | `CONSECUTIVE_MOVE` | 连续同向且累计幅度 | 连续 3 日同向 且累计 ≥ 8% | close 序列 | ± | P1 |
| A8 | `LIMIT_APPROACH` | 触及/接近涨跌停 | \|涨跌幅\| ≥ 9.5%（主板口径，可按板块配置） | close/前收 | ± | P2 |

### B 组 · 文本存在性类（数据源：`stock_notice`、`stock_news`）

| # | 事件代码 | 定义 | 默认阈值 | 依赖工具 | 方向 | 优先级 |
|---|---|---|---|---|---|---|
| B1 | `NOTICE_NEW` | 当日有新公告 | ≥ 1 条 | `notice_read_model.deduplicate_notice_rows` 去重 | 中性 | P0 |
| B2 | `NOTICE_CLUSTER` | 公告簇（密集披露） | 3 日内 ≥ 3 条（去重后） | 同上 | 中性 | P0 |
| B3 | `NEWS_CLUSTER` | 新闻簇 | 3 日内 ≥ 5 条（身份过滤后） | `news_identity.partition_news` | 中性 | P0 |
| B4 | `NOTICE_TYPE_FLAG` | 命中结构化公告类型清单（业绩预告/停复牌/问询函/诉讼/减持计划…） | 命中即触发 | `stock_notice.notice_type` 字段 | ± | P2 |

> B4 说明：依赖数据源是否提供结构化 `notice_type`；若字段为空，则由语义层（第二部分）承担同等职责，不重复实现。

### C 组 · 基本面低频类（数据源：`stock_financial_report`）

| # | 事件代码 | 定义 | 默认阈值 | 方向 | 优先级 |
|---|---|---|---|---|---|
| C1 | `FINANCIAL_REPORT_NEW` | 新报告期财务数据入库 | 出现新 report_period | 中性 | P1 |
| C2 | `FINANCIAL_SURPRISE` | 业绩超预期 | 营收或净利同比 \|Δ\| ≥ 30% | ± | P2 |

### D 组 · 可选（依赖数据可得性，暂列 P2）

| # | 事件代码 | 定义 | 数据前提 |
|---|---|---|---|
| D1 | `FUND_FLOW_ANOMALY` | 主力资金净流入/流出超阈值 | `stock_f10_cache` 内 fund_flow 结构化字段可用 |
| D2 | `HOLDER_CHANGE` | 股东户数/前十大股东变动 | 结构化股东数据可用，否则降级为语义事件（股东变动公告） |

### E 组 · 跨股联动类（数据源：行业指数 K 线 · 创业板数据来自第一期 Step 6 同步任务）

> 设计动机：单股事件只能看到"自己变了"，看不到"别人变了而自己没变"。跨股联动类是通往 L2 关联分析的桥梁，也是"潜在补动"信号的主要来源。

| # | 事件代码 | 定义 | 默认阈值 | 数据前提 | 方向 | 优先级 |
|---|---|---|---|---|---|---|
| E1 | `SECTOR_INDEX_MOVE` | 行业指数异动（代理"关联标的异动"） | 行业指数单日 \|Δ\| ≥ 3% | Step 6 已计划的行业指数 K 线同步（1 行业 1 序列，成本极低） | ± | P1（v1.1 首位） |
| E2 | `READ_THROUGH_LAG` | 领先-滞后：行业动了、个股未动 | E1 触发 且 个股同日 \|Δ\| < 1% | 同上 + 个股 K 线 | 潜在补动 | P1（v1.1 首位） |

> E2 的语义：行业指数大幅异动而个股未跟随 → 存在"补涨/补跌"预期。这是确定性可算的"关联信号"，后续由 L4 记分卡裁决其 lift。
> 个股级产业链联动（供应商↔客户）依赖知识图谱，留二期；v1.1 用行业指数代理是可落地的折中。

### 附：`LIMIT_APPROACH` 板块口径与精确判定（限价细节）

**核心问题**：A 股不同板块涨跌幅上限不同，写死 9.5% 会误判——创业板涨 15% 并非"接近涨停"（上限 20%），主板涨 9.8% 才是"贴板"。

| 板块 | 涨跌幅上限 | "接近"判定 | 数据来源 |
|---|---|---|---|
| 主板普通股 | ±10% | \|Δ\| ≥ 9.7% | `listing_board` |
| 主板 ST/*ST | ±5% | \|Δ\| ≥ 4.85% | 名称前缀 ST/*ST |
| 创业板 | ±20% | \|Δ\| ≥ 19.4% | `listing_board` |
| 科创板 | ±20% | \|Δ\| ≥ 19.4% | `listing_board` |
| 北交所 | ±30% | \|Δ\| ≥ 29% | `listing_board` |
| 新三板（NEEQ） | 无标准日涨跌幅限制 | **v1 直接跳过** | — |

**实现要点：**

1. **精确涨停判定用"涨停价"公式，不用百分比**：
   - 涨停价 = `round_half_up(前收盘 × (1+比例), 2)`（四舍五入到分）；
   - **Python 内置 `round()` 是银行家舍入，必须用 `Decimal(ROUND_HALF_UP)`**（例：10.05 × 1.1 = 11.055 → 涨停价 11.06）；
   - `close == 涨停价` → 收盘封板（`CLOSE_LOCKED`）；`high == 涨停价` → 盘中触板（`TOUCHED`），两种模式写入 `signal_json.mode`，信息含量不同。
2. **排除规则**：注册制下新股上市前 5 个交易日无涨跌幅限制；退市整理期股票 → 按上市日期与 `status` 字段跳过本事件。
3. **板块判定**：复用 `StockSymbol.listing_board` / `security_type` 字段 + 名称前缀（ST/*ST），无需新增数据依赖。
4. **配置形态**（进 `L0_PROFILE_V1`）：
   ```json
   "LIMIT_APPROACH": {
     "board_limits": {"MAIN": 0.10, "MAIN_ST": 0.05, "CHINEXT": 0.20, "STAR": 0.20, "BSE": 0.30, "NEEQ": null},
     "near_ratio": 0.97,
     "exclude_first_n_days": 5
   }
   ```

### v1 首批交付范围

**A1–A8（8 个）+ B1–B3（3 个）+ C1（1 个）= 12 个**，其中 P0 五个（A1–A4 + B1–B3 核心）第一周先上。

**v1.1 扩展队列（随第一次记分卡评审上线，走影子流程）：** E1/E2（跨股联动，依赖 Step 6 行业指数数据，W3–W4 自然就绪）→ 语义字典新增「人事变动」「重大投资与扩产」（见 S1）。

### 落库规格（新增表 `stock_change_event`）

| 字段 | 说明 |
|---|---|
| `market / symbol` | 标的 |
| `event_date` | 事件归属交易日 |
| `event_type` | A/B/C 组代码 |
| `signal_json` | 触发数值明细（如 `{"change_pct":6.2,"volume_ratio":2.8}`）+ 证据引用 |
| `source_ids_json` | 关联的 kline_id / notice_ids / news_ids |
| `dedup_key` | 唯一约束 |
| `profile_version` | `L0_PROFILE_V1`（归因追溯） |
| `status` | DETECTED（后续由 L1 更新为 SCREENED / BLOCKED） |

> 边界说明：`stock_context_event`（已存在）保留为**外部证据事件层**（人工/导入），L0 机器检测事件不与之混表；L3 证据包可同时引用两者。`WatchlistItem`（已存在）直接作为股票池。

### 单测要求（每事件一组）

正例、反例、边界（停牌、零成交量、上市不足 20 日导致均线不足、重复运行幂等）。

---

## 二、语义事件实现路径（基于现有基础）

### 2.1 现有基础盘点（全部已验证存在）

| 现有资产 | 位置 | 现状用途 | 在 L0 语义路径中的角色 |
|---|---|---|---|
| `NEWS_NOTICE_EVENT_GOVERNOR` | `skill_docs/` + `services/document_semantics.py` 等 | **深度**文档治理：全文阅读→结构化事件/主体/数字指标/分维度影响→FoundationFact 候选；含原文引文精确校验、幂等、版本保留 | **Tier 2 复用**：对高价值文本做深度结构化，不重建 |
| `StockNotice` / `StockNews` | `models/market_data.py` | 公告/新闻存储（含 title/content/notice_type/url/content_json） | Tier 1 输入源 |
| 去重与身份工具 | `services/notice_read_model.py`、`services/news_identity.py` | 公告去重、新闻身份过滤 | 保证"当日新增文本"识别准确（已具备） |
| Model Hub | `services/model_hub.py` | 多模型路由 + `json_schema_output` 强约束 + `ModelCallLog` 审计 | Tier 1 打标的执行引擎 |
| Skill 体系 | `skill_docs/*.SKILL.md` + `services/core_skill_defs.py` + `services/skill_registry.py` | 文件即真源、版本化、可修订回滚 | 新增打标 Skill 的注册与迭代通道 |
| 任务队列 | `jobs/tasks.py`（TASK_HANDLERS）+ `DatabaseJobDispatcher` | 幂等后台任务 | 打标任务执行通道 |
| 语义 Skill 生命周期 | `services/document_semantic_skills.py` | 草稿→测试→评估→启用→人工评审的完整模式 | 打标 Skill 的迭代流程直接照搬 |
| taxonomy | `services/taxonomy.py` | 标签释义体系（label_definition） | 事件类型字典的登记处（与现有标签语言一致） |

### 2.2 关键设计：两级语义架构

```
Tier 1 · 快筛打标（新增）         Tier 2 · 深度治理（已有，复用）
标题 + 摘要（≤300字）             全文阅读 + 引文精确校验
Flash 级模型 · 全量覆盖            强模型 · 仅高价值文本
输出：枚举标签 + 置信度            输出：FoundationFact 候选（可审核）
目标：低成本广覆盖                 目标：高精度可追溯
                ↓ 触发                           ↑ 证据
        （urgency=HIGH 或与量价事件同现）
```

- **两级共享同一事件类型字典**——保证 L3 证据包、L4 归因、深度治理三者语言一致；
- Tier 1 只打**标签**，不写"事实"；`FoundationFact` 仍是唯一事实区（由 Tier 2 产生）；
- Tier 1 的成本量级：池 100 只 × 日均 5 条新文本 = 500 条/日，按 10 条/请求打包 ≈ **50 次 Flash 级调用/日**，成本可忽略。

### 2.3 实施步骤 S1–S6

**S1 · 事件类型字典（冷启动建立，v1 十二类 + v1.1 增补）**
- 方法：对池内股票近 2 年大涨/大跌样本的前置窗口文本做 LLM 归纳（与确定性事件的冷启动挖掘同一批样本），产出候选类型；人工审定后登记进 taxonomy；
- **v1 字典（12 类）**：并购重组 / 重大投资与扩产 / 业绩预增 / 业绩预减 / 回购增持 / 大股东减持 / 股权变动 / 监管与风险警示 / 订单合同 / 政策行业利好 / 政策行业利空 / 其他（含显式拒答 `UNCATEGORIZED`）；
- **v1.1 增补**：「**人事变动**」（董事长/总经理/财务总监/董秘变更或辞任，财务总监变动为风险指示）；
- **边界裁定**：「重大投资与扩产」= 自己建（建设项目/扩产/设立子公司/参股/产业基金）；「并购重组」= 买别人（收购股权资产/重大资产重组）——以"自己建 vs 买别人"划界，避免同一公告双标；
- **候选观察区（先记录不打标，由记分卡裁决）**：研报评级变化、分红送转、股权质押、股东户数变化；
- 每个类型带：定义、正向示例、反向示例（什么不算）。

**S2 · 打标 Skill 定义与注册**
- 新文件 `skill_docs/CHANGE_EVENT_SEMANTIC_TAGGER.SKILL.md`；注册进 `core_skill_defs.py`（照抄 `NEWS_NOTICE_EVENT_GOVERNOR` 注册模式）；
- 输入契约：每批 ≤10 条，每条 `{text_id, title, summary(≤300字)}`；
- 输出契约（JSON Schema 强约束）：
  ```json
  {"text_id": "...", "event_type": "订单合同", "direction": "POS",
   "urgency": "HIGH", "confidence": 0.88, "quote": "≤50字原文片段"}
  ```
- **显式允许拒答**（证据不足输出 `UNCATEGORIZED`，不计为失败）。

**S3 · 执行通道**
- `task_type = "event_semantic_tagging"` 注册进 `TASK_HANDLERS`，由 worker 执行；
- 每晚增量：当日新增 notice/news（经去重与身份过滤）→ 打包批次 → 打标；
- 幂等键：`content_hash + skill_version`（同文同版本不重复打标；Skill 升级后允许重标生成新版本）；
- 失败重试走三表队列；**打标失败不阻塞确定性事件入库**（两级解耦）；
- 模型路由：task_type 路由到 Flash 级实例，fallback `MOCK`（MOCK 结果标记存疑，不进 L3 证据包）。

**S4 · 落库（新增表 `stock_text_semantic_tag`）**
- 字段：`notice_id / news_id`（二选一）、`event_type`、`direction`、`urgency`、`confidence`、`quote`、`model_call_log_id`、`skill_version`、`content_hash`、`created_at`；
- 唯一约束 `(notice_id, skill_version)` / `(news_id, skill_version)`——支持重标新版本、保留历史；
- 与 `stock_change_event` 的关系：B 组事件（如 `NOTICE_CLUSTER`）的 `source_ids_json` 指向文本 id，L3 组装证据包时 join 标签表。

**S5 · 质量与迭代（对齐 L0 三速回路）**
- 快回路：每周抽样 30 条人工评估准确率，按 `event_type` 分组进记分卡；
- 慢回路：准确率 < 80% 的类型 → 走 `SkillOptimizationDraft` 修订（复用 `document_semantic_skills.py` 的草稿→测试→评估→启用流程）；漏报盲区 → 扩充字典（`L0_PROFILE` 版本递增）；
- 门禁：字典扩充与 Skill 修订都走"回测 + 影子期 + 审批"后才进线上。

**S6 · 与飞轮联动**
- 标签行的 `id` 直接进 L3 证据包（`evidence_ids`）；
- `urgency=HIGH` 且与 A 组量价事件同日出现 → 触发 Tier 2 深度治理；
- L4 复盘按 `event_type` 归因 → 反哺类型字典与打标 Skill（哪些类型有 α、哪些是噪声）。

---

## 三、与第一期计划的衔接

| 第一期计划步骤 | 本文对应内容 |
|---|---|
| Step 2 · L0 确定性检测 | 本文件「一、确定性事件清单」为完整实现范围（v1 12 个 + v1.1 扩展队列、落库规格、单测要求） |
| Step 3 · 语义事件打标 | 本文件「二、语义事件实现路径」为完整实现路径（S1–S6） |
| 新增表 | 由原 1 张（`stock_change_event`）增加为 2 张：+ `stock_text_semantic_tag` |

> 需要用户确认的三点：① v1 字典（12 类，含合并后的「重大投资与扩产」）与 v1.1 增补（人事变动）是否符合你的方向；② E 组跨股联动（行业指数代理方案）是否认可；③ `LIMIT_APPROACH` 板块口径表（主板 ST 5% / 主板 10% / 创业板科创板 20% / 北交所 30% / NEEQ 跳过）是否按此配置。
