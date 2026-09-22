# 多来源知识库治理

现有 `STOCK_FULL_KG` 和新闻文档保留。新的 `KB_MULTISOURCE_GOVERNOR`
智能体绑定启用的 `KB_SOURCE_AUDITOR` Skill；Skill Markdown 同步到数据库并保留版本，
实际调用模型时加载完整指令，日志保留 Skill 版本/哈希、模型、输入样本与审核结果。
模型只作质量审核，固定 Python 映射归档证据，不把模型建议写为已确认公司关系。

每批创建 6 个独立领域知识库及对应图谱：证券与公司主数据、公告与外部事件、财务报告、
行情与交易量、股东与经营资料、研究报告。名称以 `KB2_领域_批次键` 标识版本。
公司主体、股票发行关系和公司关系事实保留基础数据 ID、审核状态与证据文档。
新闻继续保留在原知识库中。

## 执行

修改/治理前按项目约定先完成当天 Git 与数据库备份。在 backend 目录执行：

```powershell
python scripts/govern_knowledge_sources.py --database-url sqlite:///./quant.db --limit 50 --run-key DAILY20260922 --report backups/knowledge-governance-20260922.json --apply
```

不带 `--apply` 时仅输出来源登记、表是否存在和行数。`--instance-code` 可指定启用模型，
不指定时使用知识图谱任务的实际模型路由。拒绝 MOCK 模型冒充真实验证。
同一 `run_key` 与相同参数再次调用会返回原批次（包括失败批次），不会重新生成知识库；
重试失败任务须使用新批次键。

接口：`GET /api/v1/resources/knowledge-governance/catalog`；
`POST /api/v1/resources/knowledge-governance/run`，请求包含
`record_limit_per_source`（1..500，默认50）、可选 `instance_code` 和 `run_key`。
后者同步执行并返回 `GovernanceRunRead`；模型失败也以 `status=FAILED` 返回持久化报告。

## 范围与验收

- `source_coverage` 按来源列出登记、可用状态、真实总量、归档文档数量、抽样截断与模型审核。
- Python 每来源最多归档指定数量；模型仅审核每来源一条有界摘要，不能称逐记录或全市场治理。
- 空源为 `EMPTY`，未登记/禁用/不存在/列权限受限分别报告，不造数据补齐。
- `PENDING_REVIEW` 表示结果可查看，但仍有空源、抽样或质量待审；不等于模型调用失败。
- `complete` 的口径是配置来源的归档覆盖，不是模型对全部记录的质量认证。
- 任一模型错误、格式错误、遗漏来源或伪造引用会使整批失败；新知识库写入回滚，旧版本不变。
- 模型调用前提交配置及运行日志，避免网络等待期间持有 SQLite 写锁；新知识库作为一个事务发布。
- 禁用智能体、Skill 或移除二者绑定均阻止执行，不自动重新启用用户关闭的配置。
  已有智能体解除的资产绑定不会补回，对应来源显示 `UNBOUND_ASSET`。
  只读取资产允许的列；财务适配器不要求可选 URL 列，其他必需列缺失则保持权限受限状态。

治理报告含 `model_call_log_id`，可在模型调用日志核验真实调用。回归测试使用独立临时数据库和
受控模型响应验证失败边界、旧图谱保留与幂等性，不将测试替身的通过当成线上模型调用成功。
