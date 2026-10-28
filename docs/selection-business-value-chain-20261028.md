# 选股主营价值与产业链事件传导迭代记录

日期：2026-10-28。

## 备份基线

- 本地 Git 备份分支：`backup/selection-value-chain-20261028-160851`
- 备份提交：`ad21f03ff73934783ee68bbdaeb7af3af4aff6c0`
- 一致性数据库备份：`backend/backups/quant-selection-value-chain-20261028-160851-consistent.db`
- 数据库 `PRAGMA quick_check = ok`，大小 2,106,347,520 字节。
- 远程推送因 GitHub 443 连接失败，当前仅为本地备份；网络恢复后重试。

## 交付内容

1. 进度阶段统一颜色：已完成绿色、进行中蓝色、待开始灰色、失败红色；部分完成橙色、跳过紫灰色。
2. GraphRAG 升级到 `GRAPH_RAG_CONTEXT_V5`，从最新 F10 `profile` 和 `business_composition` 生成可追溯主营事实。
3. 模型契约升级为 `MULTI_SOURCE_VALUE_CHAIN_V5`，新增 `business_quality` 和 `value_chain_analysis`。
4. 新增只读 Skill：`BUSINESS_MOAT_VALUE_ANALYST`、`VALUE_CHAIN_TRANSMISSION_ANALYST`。
5. 页面新增“主营业务与竞争壁垒”和“产业链关联公司重大事件影响”两块重点卡片。
6. 公司板块补齐会实际刷新 F10，并同时检查 `profile` 与 `business_composition`；缺项保持 `MISSING`。
7. 八维权重和历史结果不改写。主营专项进入“基本面与盈利质量”，已核验传导进入“产业链与外部韧性”；候选关系不计分。

## 关键质量门禁

- 市场占有率有具体数值时，必须有单位、统计范围、日期、来源和证据 ID。
- 技术领先、填补空白、国产替代、客户验证不能只从产品名称推断。
- `VERIFIED` 产业链关系必须引用状态为 `ACCEPTED` 的 `FoundationFact`。
- 关联公司事件必须与关系事实属于同一传导路径。
- 候选或未验证关系只能展示为线索，方向为 `UNKNOWN`、影响分为空。
- 影响分为 -100 至 100 的研究影响强度，不是股价涨跌幅预测。

## 三轮验证

1. 静态与前端：Python 编译通过；TypeScript 与 Vite 生产构建通过。
2. 专项规则：GraphRAG 主营事实、主营构成数值、市场份额来源门禁、已审核关系门禁、候选关系隔离等 41 项测试通过。
3. 选股链路：证据、队列、评分、观察页、GraphRAG 等 129 项回归通过；新增评分专项测试通过。

只读生产库验证精智达 `688627` 能载入：

- `stock_f10_cache:51` / `BUSINESS_PROFILE`
- `stock_f10_cache:56` / `BUSINESS_COMPOSITION`
- 最新主营构成报告期 `2025-12-31`
- 半导体存储器件测试收入 624,516,904.27 元、占比 55.362%、毛利率 28.2552%
- 新型显示器件检测收入 500,612,727.07 元、占比 44.3782%、毛利率 47.2487%

当前真实库尚无精智达与长鑫之间状态为 `ACCEPTED` 的供应链事实，也没有基于该事实载入的关联证券事件。因此系统会显示产业链证据不足，不会把用户案例或关键词印象自动写成已验证关系。后续应从公司公告、年报、招股书、合同或可靠访谈建立候选事实，再经现有审核流程升级。
