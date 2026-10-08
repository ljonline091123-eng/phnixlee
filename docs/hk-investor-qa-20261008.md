# 港股投资者问答接入与验收记录

日期：2026-10-08。本轮接入公司官网公开投资者常见问答（FAQ），将采集、存储、后台更新和研究页面串联起来。FAQ 不是实时问董秘记录，也不是业绩会问答全文；当前来源仅覆盖已核验的四家发行主体，不代表港股全市场覆盖。

## 修改前备份

- 独立备份分支：`backup/20261008-185856-pre-hk-investor-qa`，已推送至配置的 GitHub 远程仓库 `ljonline091123-eng/phnixlee`。
- 快照提交：`01aab8fe0e5ee4a1f8956357239f28479d5cb994`；修改前主分支提交：`abc60cb9b1389a7a452b0fc7f6a46636e884121c`。
- 一致性数据库备份：`backend/backups/quant-20261008-185856-pre-hk-investor-qa.db`，完整性检查为 `ok`。
- 原 `main` 分支、原暂存区及此前未提交成果保留。没有清空数据库、删除旧表或重建原有图谱。
- 推送成功后，重复查询远程分支曾遇到 `Empty reply from server`，该次网络查询失败不影响此前已完成的推送。

## 来源与真实采集结果

| 港股代码 | 公司 | 实际入库问答 | 来源语言 | 官网原文 |
| --- | --- | ---: | --- | --- |
| 09868 | 小鹏汽车 | 9 | 中文 | https://ir.xiaopeng.com/zh-hans/resources/investor-faqs |
| 09866 | 蔚来 | 8 | 中文 | https://ir.nio.com/zh-hans/resources/investor-faqs |
| 02015 | 理想汽车 | 9 | 中文 | https://ir.lixiang.com/zh-hans/touzizhechangjianwenti |
| 09888 | 百度 | 22 | 英文原文 | https://ir.baidu.com/shareholder-services/investor-faqs |
| 02533 | 黑芝麻智能 | 0 | 尚未找到公开问答文字原文 | 见下方核验范围 |

四家已接入公司合计 48 条；各自问答同步状态为 `COMPLETE`，页面问答状态为 `AVAILABLE`。来源总体覆盖仍为 `PARTIAL`，同步完成仅表示本轮已配置官网 FAQ 采集完成。

黑芝麻智能已检查以下公司公开页面：

- https://ir.blacksesame.com/?lang=zh-cn
- https://ir.blacksesame.com/report.html?lang=zh-cn
- https://www.blacksesame.com/zh/list_8/

这些页面有报告、公告及投资者联系等内容，尚未找到可采集的公开问答文字原文。系统显示 `MISSING` 并列出核验页面，不虚构问答，不将来源缺失记为同步完成。这不代表公司没有举行投资者交流，也不代表已经穷尽全部网络及付费来源。

## 数据契约与更新机制

- 新增数据源 `HK_ISSUER_IR`（中文名称“港股公司官网投资者问答”）和接口 `HK_INVESTOR_QA_ON_DEMAND`（“港股公司投资者常见问答”）；支持来源及接口启停、中文连接测试与覆盖说明。
- 复用 `StockInvestorQA`、`StockF10Cache`、现有 `investor_qa_sync` 持久任务队列及采集日志。不新增业务表，不需要 DDL 迁移。
- 问答保留证券及公司身份、问题、完整回答、问答类型、语言、官方 URL、原 HTML 片段、内容哈希、页面哈希及采集时间。来源没有披露发布日期时保持空值，页面显示“发布日期未披露”，不以采集日期代替。
- 稳定问答 ID 基于证券代码和问题；同一问题的回答更新就地保存，重复更新不重复插入。只允许已核验的证券—官网映射及同域 HTTPS 重定向。
- 本地数据优先展示。进入研究页面后，未采集或超过六小时的已支持问答可提交后台更新；“更新问答”按钮可立即提交。此刷新由访问或按钮触发，不是全市场定时采集。
- 后台任务保存断点、执行状态、重试次数及错误；网络失败使用现有持久队列退避重试，单个任务最多尝试三次，终态失败后可再次手动提交。重试期间保留已有问答，解析失败、身份不匹配或访问限制不能当成“无问答”。
- 研究及完整 F10 的较慢更新保留独立问答状态。港股批量采集及后台阶段日志纳入问答状态，来源缺失或未完成时显示部分完成，不伪报全部完成。

实际验收发现原研究来源归一会将未知的 `HK_ISSUER_IR` 回退为 `CNINFO`。已修正来源归一，并限定本轮 48 条已核验港股官网 FAQ 就地校正来源标识及缓存，保留原主键；新增回归测试检查数据库来源字段和页面投影。

## 用户验证步骤

1. 在股票主数据页面选择港股，搜索 `09868`（小鹏汽车），打开个股详情的“研究”。
2. 查看“投资者问答”中的官网常见问答标签、来源及采集状态；展开更多后可查看全部 9 条。
3. 点击问题，查看完整回答、未披露日期说明及官网原文链接。
4. 点击“更新问答”，观察排队、后台采集及完成状态；再次查看仍为 9 条，不应新增重复记录。
5. 分别验证 `09866`、`02015` 和 `09888`；百度保留英文原文并标注语言。
6. 查询 `02533`，确认页面显示已核验来源及缺口说明，问答数量为 0，不应显示全部完成。
7. 在数据源管理查看“港股公司官网投资者问答”，测试连接；在接口目录查看对应中文接口及启停状态。

| 接口 | 用途 |
| --- | --- |
| `GET /api/v1/stocks/HK/09868/research/qa` | 读取本地完整问答、来源证据及状态 |
| `GET /api/v1/stocks/HK/09868/research/qa/sync` | 查看持久同步状态、记录数、任务及错误 |
| `POST /api/v1/stocks/HK/09868/research/qa/sync` | 强制提交问答更新；同证券活动任务复用 |
| `GET /api/v1/stocks/HK/09868/f10?local_only=true` | 验证本地 F10 研究问答投影与问答表一致 |
| `POST /api/v1/data-sources/24/test` | 本机新增数据源的真实连接测试；其他环境以来源查询返回的 ID 为准 |

问答 worker 使用现有启动脚本中的命令；也可在后端目录运行 `python -m app.jobs.worker --task-type investor_qa_sync --lease-seconds 600 --reload`。

## 验证结果

- 后端相关回归：**157 个测试通过，9 个子测试通过**；一条现有 AkShare/Pandas 弃用提示。
- 前端 `npm run build` 通过；存在现有包体积提示。
- 浏览器实际验证问答列表、完整回答弹窗、来源链接、未披露日期、手动刷新及真实 worker 执行、黑芝麻缺口状态、百度英文原文；页面脚本错误为 0。
- 新来源连接测试返回 `CONFIGURED` 及中文信息，实际获取小鹏官网问答 9 条。
- 五个证券的本地问答 API 与 F10 问答投影数量一致；四家来源字段均为 `HK_ISSUER_IR`。
- 数据库完整性检查 `ok`，问答重复组为 0。
- 对备份中 **70 张有 ID 字段的原表**检查，原记录主键缺失为 0。原 194 条 A 股问答的问题、回答、来源日期、哈希及原文内容保留；后台原同步机制更新过部分采集时间，兼容比较仅忽略采集/更新时间戳及 `raw_payload.fetched_at`，不宣称数据库字节完全不变。

| 本轮重点统计 | 备份时 | 验收时 |
| --- | ---: | ---: |
| 数据源 | 23 | 24 |
| 接口 | 55 | 56 |
| 投资者问答 | 194 | 242 |
| 股票主数据 | 19559 | 19559 |
| 模型 Skill | 17 | 17 |
| Agent 定义 | 9 | 9 |
| 知识库 | 8 | 8 |
| 历史知识图谱 | 10 | 10 |

其他业务表在运行期间可能由既有后台同步自然更新，以详细验收 JSON 为准。

验收产物：

- `backend/validation/hk-investor-qa-20261008/api-database-result.json`
- `backend/validation/hk-investor-qa-20261008/browser-result.json`
- 同目录截图：`xiaopeng-faq-list.png`、`xiaopeng-faq-detail.png`、`black-sesame-source-gap.png`、`baidu-original-language.png`。

可复跑验证：后端目录下执行 `python scripts/validate_hk_investor_qa_data.py` 和 `python scripts/validate_hk_investor_qa_ui.py`；后者依赖本机已安装的 Playwright 和 Microsoft Edge，并会触发实际问答更新。

## 代码范围、兼容性与回滚

新增：

- `backend/app/connectors/hk_investor_qa.py`
- `backend/tests/test_hk_investor_qa.py`
- `backend/scripts/validate_hk_investor_qa_data.py`
- `backend/scripts/validate_hk_investor_qa_ui.py`

增量扩展：

- `backend/app/connectors/investor_qa.py`、`backend/app/connectors/registry.py`
- `backend/app/services/catalog.py`、`backend/app/services/source_coverage.py`
- `backend/app/services/investor_qa.py`、`backend/app/services/research_store.py`
- `backend/app/services/f10.py`、`backend/app/services/stock_on_demand.py`、`backend/app/services/stock_batch.py`
- `backend/app/api/stocks.py`
- `frontend/src/api.ts`、`frontend/src/StockDetailDrawer.tsx`

继续保留 A 股原问董秘客户端和数据；新增港股客户端与 A 股接口隔离。模型、Agent、Skill、基础身份、湖仓与历史知识图谱未因本轮功能删除或重建。

需要暂停采集时，停用新增数据源或接口即可，已有本地问答保留。需要回滚代码时，按本轮备份逐文件撤销港股问答增量，保留其他开发成果；本轮没有表结构迁移，禁止通过重置整个工作区或覆盖当前数据库回滚。旧港股研究验收报告记录的是当时状态，问答覆盖以本报告为准。

## 后续缺口

当前仅四家公司官网 FAQ 已完成接入；其他港股需要继续核验公司来源和解析结构。实时互动问答、业绩会问答全文及授权数据源尚未接入，黑芝麻智能问答原文仍缺失。现有 FAQ 接入不等于已将问答自动提取为已验证事实、完成知识图谱投影或实现选股分析。
