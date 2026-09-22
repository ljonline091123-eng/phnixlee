# Foundation 增量迁移与运行说明

项目使用独立的 Alembic 历史，当前版本为 `foundation_0003`。首版 `foundation_0001` 的七张表保持原始快照，第二版仅新增来源标识映射和证券分类两张表，第三版仅新增全量股票公司映射状态表。该历史只管理以下十张表及自己的版本表 `foundation_schema_revision`：

- `foundation_entity`
- `foundation_security`
- `foundation_listing`
- `foundation_evidence`
- `foundation_fact`
- `foundation_fact_evidence`
- `foundation_fact_review`
- `foundation_source_identity`（0002 新增）
- `foundation_security_classification`（0002 新增）
- `foundation_company_mapping_state`（0003 新增，记录已映射、来源缺失、不适用及冲突的具体原因）

已有证券、图谱、研报、任务等业务表不在该历史的管理范围内。`stock_symbol.id` 是新上市记录表引用的前置主键；迁移仅核对其存在及基本类型，不创建或修改该旧表。

## 与现有启动的关系

现有 `initialize_database()` 仍执行旧兼容逻辑及 `Base.metadata.create_all()`；本轮没有把全项目改成由 Alembic 启动建库，也没有改变应用启动流程。

因此有两种正常状态：

1. 尚未启动新版本：新表不存在，迁移按 0001、0002、0003 顺序创建这些表并记录版本。
2. 新版本启动已通过 `create_all` 建好新表：迁移逐项核对已有结构，全部一致才记录版本。不会重建已有表，不删除数据，也不盲目执行 `stamp`。

部分表已经存在时，也必须先核对已存在部分，之后只补齐缺失表。发现字段、类型、可空性、数据库默认值、索引、唯一约束、外键、主键或 CHECK 不一致时，返回 `BLOCKED`，不尝试自动修复或掩盖差异。该核对不是全量业务数据审计，也不接管旧表的结构漂移。

## 命令入口

在项目 `backend` 目录运行。依赖列于 `requirements.txt`，需包含 Alembic。

首先离线查看首版完整 SQL；该命令不连接数据库：

```powershell
python scripts/migrate_foundation.py preview
python scripts/migrate_foundation.py preview --dialect postgresql
```

`preview` 展示从 0001 到 0003 的完整初始 DDL，不根据某个库的现状裁剪 SQL。实际待创建表和需核对采用的表由 `check` 给出。不要将完整预览 SQL 直接执行到已有表的数据库。

运行检查或迁移必须显式提供目标 URL，也可通过专用环境变量设置。脚本不会加载应用 `.env`，不会默认使用应用的 `DATABASE_URL`，也不会调用启动和种子逻辑。

以下路径为需替换的示例，先用已备份数据库的独立副本验证：

```powershell
$env:FOUNDATION_MIGRATION_DATABASE_URL = 'sqlite:///C:/chosen-backup-copy/quant.sqlite'
python scripts/migrate_foundation.py check
python scripts/migrate_foundation.py apply
python scripts/migrate_foundation.py check
```

也可使用 `--database-url` 参数。含密码的连接地址优先经专用环境变量传入，避免出现在终端命令历史中。真实库应先备份，停止使用该库的写入进程，并确认环境变量指向预期目标后再执行 `apply`。

SQLite CLI 仅接受已存在的数据库文件，不会因为路径拼错创建一个空库；不接受内存库或额外 URI 参数。`check` 以 SQLite 只读模式打开文件；PostgreSQL 检查事务使用只读模式。

## 检查结果

| 状态 | 含义 | 下一步 |
| --- | --- | --- |
| `PENDING` | 新表均未建立，旧证券主键符合前置条件 | 审阅待创建表后执行 `apply` |
| `UNVERSIONED` | 部分或全部新表已存在且结构一致，但没有迁移版本记录 | `apply` 补齐缺表并登记已验证版本 |
| `UPGRADE_REQUIRED` | 已登记 0001 或 0002，旧表结构正确，尚未登记 0003 | `apply` 仅补齐后续版本的新表，已由 `create_all` 建立的表先核对再采用 |
| `CURRENT` | 新表与冻结结构一致，版本为 `foundation_0003` | 重复 `apply` 不重复建表或写入版本 |
| `BLOCKED` | 前置条件、结构、未知新表或版本信息有差异 | 根据 `problems` 排查，保留差异，不直接 `stamp` |

`check` 成功退出码为 0，发现阻碍时为 2；`PENDING` 不等于已经迁移完成。输出同时包含 `tables_to_create`、`verified_tables_to_adopt` 和具体问题。

已登记当前版本但缺失表、未知版本、空版本记录或意外的 `foundation_*` 表均会阻止应用迁移。不能靠重新登记版本绕过问题。

## 事务与维护边界

- SQLite 写入使用显式 `BEGIN IMMEDIATE`，迁移失败时连 DDL 一并回滚；PostgreSQL 使用事务及事务级咨询锁串行化该迁移工具。
- 正常运行应用不参与上述锁协议，执行真实迁移仍应选择停写窗口。
- 原始 SQL 预览不包含在线结构核对与锁流程；正式执行使用 `apply`。
- 直接在线 Alembic 命令被隔离配置拒绝；不提供绕过检查的 `stamp` 入口。
- `downgrade` 主动拒绝删除新表。回退应用版本可以保留新表；数据库恢复应针对明确选择的备份和维护窗口处理。
- 目前包含 0001 与 0002 两个增量版本，没有自动生成后续全仓库迁移的能力。后续结构变更应新增经过评审的版本及对应检查和测试，不修改已发布版本的结构快照，不把旧业务表交给自动差异脚本。

冻结快照位于 `backend/alembic/versions/foundation_0001.py` 和 `foundation_0002.py`，不导入可变 ORM 模型。第二版基于首版冻结定义增加两张表。测试同时校验 ORM `create_all` 的结构与当前快照一致，并验证旧 0001 实例升级时原表和数据保持，避免应用和迁移悄然分叉。

## 本轮验证

```powershell
python -m pytest tests/test_foundation_migration.py -q
```

测试只使用独立临时 SQLite 文件，覆盖旧表和数据保留、重复升级、已有 `create_all` 表的核对采用、部分表补齐、结构漂移及未知版本拒绝、迁移失败回滚、只读检查和显式目标要求。SQLite 与 PostgreSQL 的离线预览均检查只输出预期的新表 DDL。

以上“未连接用户业务数据库、未执行真实库迁移”仅说明上一轮基础层初期验证，不代表后续部署状态。本轮公司图谱增量与正式库部署结果见 [公司图谱验收说明](company-graph-acceptance.md)。PostgreSQL 在线迁移尚未使用实际实例验证，投产前需在目标版本的独立测试库验证。
