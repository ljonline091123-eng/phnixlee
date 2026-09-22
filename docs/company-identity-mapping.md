# 股票、公司映射与新旧图谱身份

本轮在原系统上增量实现三个入口：股票详情的“公司关联”直接定位当前证券的发行主体；公司关系工作台按完整证券主数据搜索、筛选与分页；旧证据图谱和公司图谱返回共同的证券及公司标识。

## 身份契约

| 字段 | 含义 | 示例或规则 |
| --- | --- | --- |
| `canonical_security_id` | 跨图谱证券标识 | `security:CN_A:000001`，保留市场和前导零 |
| `canonical_company_id` | 跨图谱发行主体标识 | `company:<foundation_entity UUID>` |
| `stock_symbol_id` | 原股票主数据主键 | 保持原值 |
| `security_id` / `listing_id` | 公司图谱证券及上市映射 | 保持已有 UUID |
| `identity_version` | 身份契约版本 | `SECURITY_COMPANY_V1` |

同一公司多市场证券通过来源主体编号或一致的登记标识关联到同一公司。证券简称相似不能合并公司。NEEQ 与 NEEQ_INNOVATION 的市场记录仍各有证券标识，不凭代码相同合并证券。原图谱整数节点 ID、entity_key、边、证据及公司图谱 UUID 保留；共同标识作为附加字段提供。

旧图谱查询会实时解析新增映射，新建图谱也写入同样字段。`backfill_graph_identity.py` 只回填旧 STOCK 节点的身份属性，不重建图谱。历史 `known_at` 查询只使用当时已知的映射和证据。

## 来源及边界

公司资料来自东方财富结构化发行主体数据：国内 `RPT_F10_ORG_BASICINFO`、香港 `RPT_HKF10_INFO_ORGPROFILE`。核对精确市场、证券代码、`ORG_CODE` 及公司全称，保存原始记录、来源地址和采集时间。工商字段属于来源转录，不能等同工商登记的官方核验。

香港证券分类优先使用港交所 `ListOfSecurities.xlsx`，区分公司权益证券、存托凭证、基金、REIT、权证及其他产品。港交所结算编号中明确标注内地 A 股代码的记录，保留原主数据并标明不适用港股公司映射。历史代码只有取得精确代码及明确权益分类后才建立公司映射。

来源的更新日期文字与采集时间分开保存。当前资料不能自动作为过去时点的证据。A 股上市地不等于公司法律注册地；同一来源主体编号和公司全称对应的明确注册地证据，可用于跨市场统一公司主体。

`foundation_company_mapping_state` 为每条股票主数据记录保留本次处理状态：

| 状态 | 含义 |
| --- | --- |
| `MAPPED` | 已有可追溯的证券—公司映射 |
| `SOURCE_MISSING` | 缺少能够核对的发行主体或证券分类资料 |
| `NOT_APPLICABLE` | 来源明确为非普通股产品或非港股上市代码 |
| `CONFLICT` | 主体、注册标识或证券分类发生冲突，需要核对 |

映射完成仅表示发行主体已识别；股权、供应链、经营、司法和分类标签的覆盖情况仍分别显示。批量映射不会制造这些关系，也不会重写已有映射、持股比例、标签释义或事实审核状态。

## 重跑方式

在 `backend` 目录执行，先备份实际数据库。以下命令使用本地 `quant.db`，没有覆盖旧映射的参数。

```powershell
python scripts/migrate_foundation.py check --database-url sqlite:///quant.db
python scripts/migrate_foundation.py apply --database-url sqlite:///quant.db
python scripts/collect_company_master.py --database quant.db --cache-dir tmp_company_master_sources
python scripts/collect_company_master.py --database quant.db --cache-dir tmp_company_master_sources --supplement
python scripts/import_company_master.py --database-url sqlite:///quant.db --snapshot tmp_company_master_sources/combined_result.json --report validation/company-master-import.json --dry-run
python scripts/import_company_master.py --database-url sqlite:///quant.db --snapshot tmp_company_master_sources/combined_result.json --report validation/company-master-import.json
python scripts/backfill_graph_identity.py --database-url sqlite:///quant.db
python scripts/backfill_graph_identity.py --database-url sqlite:///quant.db --apply
```

采集器保留原始分页快照并支持续传。需要全新时间点的数据时使用新的缓存目录。导入按批提交，可重跑；已有映射跳过，新记录逐条保存点隔离。执行记录保存在 `pipeline_run`。`--dry-run` 给出快照规模，逐条身份校验在实际导入时执行。

补源命令保存独立的 `supplemental_result.json` 及全量合并的 `combined_result.json`，原 `result.json` 不变。完整重跑应导入全量合并文件。精确的港股历史权益证券及基金分类使用东方财富证券码表补充，其聚合来源身份会保留，不标成港交所官方分类。详细来源说明见 [company-master-sources.md](company-master-sources.md)。

本轮修正六个新建发行主体的注册地使用独立的 `reconcile_company_master_jurisdiction.py`，仅接受指定快照、备份基线及明确主体集合，默认预览。它拒绝修改备份中已存在的公司；保留本轮新公司 UUID 及原证据，追加纠错证据和审计记录，不是常规全库批量改写入口。

## 验收入口

- 股票详情中点击“公司关联”，应直接显示该证券对应公司；连续切换股票不应显示上一只股票的公司。
- 公司关系工作台默认显示股票列表，可按市场、代码或名称搜索与翻页；“万科”能匹配含空格的旧证券名称。
- 搜索缺少资料或不适用的记录，列表及详情应展示具体原因。
- 比较 `/resources/knowledge-graphs/{id}/explore` 与 `/company-graph/by-symbol` 返回的两个 canonical 字段。
- `/company-graph/mapping-status` 查看当前全库覆盖和例外数量。

原股票抽屉的行情和 F10 自动刷新行为保留。新加取消信号会在关闭或切换股票时取消过期请求，防止它们占满浏览器连接。真实页面验收会触发原有的股票资料自动刷新；数据保留报告将这类有界的新增资料与身份回填单独列明。

## 本轮实库结果（2026-09-22）

全库 19,165 条原证券记录均已核对，无删除；其中 14,260 条关联到 14,008 家发行公司，4,885 条有证据判定不适用普通股公司映射，20 条保留资料缺口，身份冲突为 0。

| 市场 | 已映射 | 不适用 | 待补资料 | 原证券记录 |
| --- | ---: | ---: | ---: | ---: |
| A 股 | 5,566 | 0 | 0 | 5,566 |
| 香港 | 2,808 | 4,885 | 17 | 7,710 |
| 新三板 | 3,611 | 0 | 3 | 3,614 |
| 新三板创新层 | 2,275 | 0 | 0 | 2,275 |

待补资料的香港代码：02900、02901、02903、02905、02909、02910、02990、02991、02995、08577、08578、08580、30796、83168、91211、91900、93466。包括旧临时交易代码、尚未确定类别的产品及四条缺完整内地证券标记的结算记录。新三板 832317、833874、833994 的现存历史资料涉及转板及市场别名，尚未取得足够历史上市身份证据，保留原市场，不自动改市场或按名称合并。

验证结果：

- 132 项功能测试及 2 个迁移子测试通过，另 13 项数据保留校验测试通过；前端构建通过。
- 原 25 只证券样本的 454 项验收检查通过；股权方向、持股比例、分类、公告证据及旧公司 UUID 链保留。
- 旧图谱 16 个股票节点均与公司图谱标识一致，原 210 个节点、386 条关系及 165 份文档保留。重复回填显示 16 个节点全部无需修改。
- 浏览器连续打开万科、九号、长和、新松佳和均直达正确公司，搜索分页、缺失状态、重试及移动端验证通过；页面错误为 0。
- 31 张受保护表保留检查通过。页面原有自动刷新对 4 只股票产生正常变动：万科仅刷新时间戳，另 3 只补充原空上市日期及 F10 扩展资料；既有扩展键值保留。这些变动在验收报告中显式列为 `expected_refresh`。

实库导入报告：`backend/validation/company-master-final.json`。数据保留报告：`backend/validation/identity-preservation-production.json`。浏览器报告：`backend/validation/company-mapping-ui-report-20260922.json`。这些运行产物及原始来源快照使用现有忽略规则保留在本地，不作为源码提交。

只读数据保留校验脚本纳入源码，可在 `backend` 下复跑：

```powershell
python scripts/verify_identity_preservation.py --baseline backups/db-before-identity-20260921.db --current quant.db --require-identity --allow-additive-stock-refresh --report validation/identity-preservation-production.json
```

不传 `--allow-additive-stock-refresh` 时严格拒绝所有旧股票字段变化。该例外开关仅接受本轮实际核对的四只证券及限定新增字段，不接受其他旧值覆盖。
