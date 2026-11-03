# V8 研究问题驱动分析

V8 不替换现有湖仓、知识库、知识图谱、GraphRAG 或八维评分。八维评分继续作为结果展示和质量校验层，研究过程先围绕四个问题组织证据，再形成整体结论：

1. **公司赚钱逻辑（BUSINESS_MODEL）**：主营产品、收入结构、利润质量、现金流和持续性。
2. **重大管理层变化（MANAGEMENT_CHANGE）**：职务变化、生效日期、官方原因、职责交接、经营连续性和竞争性解释。
3. **并购与第二曲线（ACQUISITION_SECOND_CURVE）**：标的、对价、交割、并表、协同、整合风险和收入利润兑现。
4. **资本行为动因（CAPITAL_ACTION_INTENT）**：回购、注销、增减持、解禁和锁定安排的多种可能动因及后续验证。

每个问题都必须区分事实、推断、假设和未知，保存支持证据、反证、缺口、补证计划和跟踪指标。重大事件适用时至少提出两条竞争性假设；没有事件时使用 `NOT_APPLICABLE`，不得为了填满字段虚构事件。

外部互联网信息不能直接进入结论。系统先给出优先官方来源、检索词和缺口影响，补证资料入湖仓并生成可追溯文档后，才允许重新分析。`RESEARCH_QUESTION_PLANNER`、`EVIDENCE_GAP_DETECTOR`、`MANAGEMENT_CHANGE_DEEP_DIVE`、`ACQUISITION_IMPACT_ANALYST`、`CAPITAL_ACTION_INTENT_ANALYST`、`PEER_BENCHMARK_ANALYST`、`COUNTER_EVIDENCE_REVIEWER` 和 `CONCLUSION_QUALITY_GATE` 均为只读 Skill，不执行任意 SQL 或数据库写入。

历史 V7 结果保持只读兼容，新建分析和失败重试使用 `FUNDAMENTAL_RESEARCH_V8`。研究未完成时发布状态为 `RESEARCH_INCOMPLETE`，前端可以查看阶段性研判和缺口，但不能确认跟踪或伪装成完整结论。
