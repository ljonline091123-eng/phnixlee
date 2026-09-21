# 全量证券与公司主体来源

`backend/scripts/collect_company_master.py` 只读取证券主表作为采集范围，原始响应和规范化候选写入本地快照目录；它不会写业务数据库、删除证券或替换既有映射。

| 来源 | 可提供的身份信息 | 限制 |
| --- | --- | --- |
| 东方财富 `RPT_F10_ORG_BASICINFO` | A 股、新三板的证券代码、公司全称、机构编号、转录的登记号码 | 包含历史证券；登记信息未经过工商登记库核验 |
| 东方财富 `RPT_HKF10_INFO_ORGPROFILE` | 港股对应机构编号及公司资料 | ETF 的资料可能指向基金管理人，不能仅凭这张表创建股票发行公司 |
| 港交所 [List of Securities](https://www.hkex.com.hk/eng/services/trading/securities/securitieslists/ListOfSecurities.xlsx) | 证券类别、子类别、ISIN | 主要是现有上市证券清单，不覆盖所有历史代码；来源更新日期与实际抓取时间分别保留 |
| 既有 [HKEX SDW](https://www.hkexnews.hk/sdw/search/stocklist.aspx) 原始记录 | CCASS 内部证券编号及明确的大陆证券引用 | 标有 `(A #六位代码)` 的记录不是同号香港上市股票，不能据此创建虚假的港股公司映射 |

国内及香港公司表均使用稳定排序、分页批量采集，不为每一只股票单独请求公司资料。主体保持 `EASTMONEY + ORG_CODE` 来源标识；有效的统一社会信用代码另作为身份依据。新三板基础层和创新层采用同一来源机构编号，不因为层级不同重复建立公司。

国内存托凭证保留 `DEPOSITARY_RECEIPT` 类型。公司注册地缺乏明确依据时标为未知，不把上市市场直接当作公司注册地。港交所明确的投资公司、仅供交易证券和存托凭证归入权益证券；基金、信托、权证等不会自动作为普通股公司。

从项目的 `backend` 目录运行：

```powershell
python scripts/collect_company_master.py --database quant.db --cache-dir tmp_company_master_sources --page-size 500
```

`--database` 使用 SQLite 只读模式。默认断点续采，重用请求参数相同的已归档页；刷新时使用新的快照目录。网络失败最多重试三次，遇到鉴权或限流响应不重试。分页总量改变、提前空页或重复页会明确报告失败，不把不完整结果当全量数据。

快照文件：

- `pages/DOMESTIC_*.json`、`pages/HK_*.json`：源响应、请求参数、抓取时间。
- `hkex_list_of_securities.xlsx`、`hkex_security_classification.json`：官方类别原文件及解析记录。
- `result.json`：可导入的 `records`、不适用的 `exclusions`、待补来源的 `unresolved` 以及 `failures`。
- `manifest.json`：数量、状态、来源边界和结果路径。

每个候选包含 `market`、`symbol`、`company`、`source_record`、`evidence`、`source_name`、`source_url`、`observed_at` 和证券类别。香港候选另保存 `security_classification_evidence`。`records` 是有证据的身份输入，并不代表工商核验已经完成；写库仍须检查既有主体、映射和冲突。

`NON_EQUITY` 表示有官方类别证明不属于本次公司股票映射范围；`NOT_HK_LISTED_SECURITY` 表示原始 SDW 记录明确引用大陆 A 股。无证据、旧代码、市场冲突等进入 `unresolved`，不能通过简称猜测或随意去掉市场后缀来补齐。

快照是抓取时所见的数据。来源没有明确公布时间的资料不填写 `published_at`，更不能直接用于历史回测中的已知信息判断。
