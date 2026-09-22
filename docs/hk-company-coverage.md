# 港股公司映射的覆盖口径与补齐

本次以港交所官方 [List of Securities](https://www.hkex.com.hk/eng/services/trading/securities/securitieslists/ListOfSecurities.xlsx) 中的权益证券作为可核查分母，来源快照标记为 `Updated as at 22/09/2026`。抓取时间另存为 `2026-09-21T15:42:48.815500+00:00`；不把来源标签当成信息首次可得时间。

官方权益记录共 2,818 条，包含主板 2,480 条、GEM 308 条、投资公司 22 条、仅供交易证券 6 条及存托凭证 2 条。这里统计的是证券代码，不是独立公司数量，也不全部是普通股：同一公司可能有多个交易柜台、供股权或优先股。

原系统港股主表有 7,710 条，其中与官方当前权益清单重合的 2,807 条均已有公司映射。另有 11 个官方当前代码缺少主表记录：02917、02927、02928、02929、02930、02932、04621、06727、08567、08581、09856。新增服务只追加这些证券及来源有明确公司编号的映射，不覆盖既有证券、公司、股权、证据或关联。

新增代码必须同时满足：

1. 官方原文件 SHA-256 与归档校验值一致，官方清单中的五位代码唯一，类别为支持的权益证券类别。
2. 完整归档的东方财富公司资料存在精确 `SECUCODE=代码.HK`、非空公司全称及 `ORG_CODE`，同一代码没有冲突主体。
3. 公司通过来源命名空间及机构编号解析，法律注册地冲突时停止，不通过证券简称合并。
4. 保留官方证券名称、类别、子类别、ISIN、交易币种和完整来源记录。来源大类不能区分普通股、临时柜台或供股权时，标为 `EQUITY_UNSPECIFIED`，不会依据简称补造细分类。

旧主表中的 30796、91211、91900、93466 具有原始 HKEX SDW 记录，明确引用 688796、601211、601900、603466 内地证券代码。只有原始结算代码精确匹配、来源端点为 HKEX SDW、目标存在于 A 股主表且该结算代码未出现在官方现行清单中时，才更新为“不适用港股发行公司映射”。保留原证券记录及其市场、代码，不把结算代码变成香港公司。

当前清单之外的 13 个历史或未分类港股代码继续显示来源缺口。历史临时柜台须补充对应时期的证券类别与发行主体证据，历史 ETF 也不能把基金管理人直接作为股票发行公司。这些代码不在当前官方权益分母内。

运行方式（项目 `backend` 目录）：

```powershell
# 只读预览：业务数据库不写入；输出可审查候选及来源证据
python scripts/complete_hk_company_mapping.py --database-url sqlite:///quant.db --snapshot-dir tmp_company_master_sources --report validation/hk-company-completion-preview.json

# 已审查、备份后的实际补齐；全部追加在一个有限事务内提交
python scripts/complete_hk_company_mapping.py --database-url sqlite:///quant.db --snapshot-dir tmp_company_master_sources --report validation/hk-company-completion.json --apply
```

默认最多 100 条新增映射，超出时在写入前停止。重复运行保留原映射及证据，不重复插入。成功运行保存 `hk_company_mapping_completion` 流水记录与前后覆盖报告。官方清单快照在数据源中登记为“港交所官方证券清单快照”，自动同步关闭，更新使用现有采集脚本后再次预览补齐。

预期完成后：官方当前权益 2,818/2,818 关联公司；港股主表共 7,721 条、公司映射 2,819 条，另含一个不在当前官方清单内但已有明确来源映射的历史代码。实际结果以 `backend/validation/hk-company-completion.json` 的提交记录为准。
