# 湖仓兼容层实施说明

本轮实现的是第一阶段轻量湖仓兼容层，不引入 Iceberg、Spark 或 Trino。

## 已实现

- `LakeObject`：原始对象、Normalized 对象和 Serving 对象的 URI、哈希、来源记录和数据集版本。
- `LakeDataset`/`LakeDatasetVersion`：JSONL/Parquet 数据集目录、唯一版本、字段结构和质量结果。
- `LakeLineageEvent`：来源表、转换步骤、数据集版本和批次之间的统一血缘。
- `DocumentChunkVersion`：文档切片内容哈希、切片偏移、解析器版本和 Embedding 模型版本。
- 文件系统对象存储：本机默认模式，无额外服务依赖。
- MinIO 对象存储：通过 `LAKE_STORAGE_BACKEND=minio` 切换，使用 S3 兼容 API。
- `/api/v1/lakehouse/status`：查看存储健康状态及对象、数据集、切片和血缘数量。
- `/api/v1/lakehouse/datasets`：查看数据集目录。
- `/api/v1/lakehouse/lineage`：查看转换血缘。
- `/api/v1/lakehouse/datasets/export`：Raw 层发布 JSONL，Normalized/Serving 层发布 Parquet；发布前执行非空、字段结构和行结构质量门禁。
- `/api/v1/lakehouse/datasets/{id}/versions`：查询数据集版本、对象、字段结构和质量结果。
- `/api/v1/lakehouse/datasets/{id}/preview`：从对象存储回读指定数据集版本并预览，验证文件并非只写不读。
- `/api/v1/lakehouse/objects`：查询内容寻址对象目录。
- `/api/v1/lakehouse/documents/chunks`：归档原文并创建幂等的文档切片版本和切片血缘。
- `/api/v1/lakehouse/documents/archive`：批量回填已有知识文档的 Raw 原文、切片和血缘。
- 数据中台“湖仓工作台”：可选择来源表和目标分层发布快照、归档文档、检查存储状态、质量结果与样例数据。
- 股票主数据同步和按需行情抓取成功后自动归档连接器返回值，并建立同步/抓取日志到 Raw 对象的血缘；归档异常写入任务日志，不回滚已经成功的在线入库。

## 分层发布规则

- Raw：JSONL 或原文对象，尽量保留来源记录形态。
- Normalized：使用 Zstandard 压缩的 Parquet 标准快照。
- Serving：只允许证券/公司主数据和已审核事实；公司关系事实及证券分类仅发布 `ACCEPTED` 状态，新闻等候选线索禁止直接进入 Serving。
- 所有发布版本使用 UTC 微秒时间和随机后缀，记录批次 ID、内容哈希、来源表、行数、字段结构和质量结果。
- 文档切片记录原文对象 ID、原文偏移、解析器版本、向量模型版本；相同原文和解析版本重复执行时复用切片。

## MinIO 验证

安装 Docker Desktop 后，在项目根目录执行：

```powershell
docker compose up -d minio
```

MinIO 控制台：`http://127.0.0.1:9001`，默认账号为 `minioadmin`，仅用于本地验证。

API/Worker 容器通过以下配置访问：

```text
LAKE_STORAGE_BACKEND=minio
LAKE_S3_ENDPOINT=minio:9000
LAKE_S3_BUCKET=quant-lake
```

本机直接运行 API 时，使用 `127.0.0.1:9000`；容器内部运行时必须使用服务名 `minio:9000`。

## 数据范围

当前覆盖股票主数据、实时行情、K 线、新闻、公告、财务、F10、外部事件、研报、知识文档，以及公司主体、证券、上市映射、证据、事实和证券分类。单次导出最多十万行，工作台默认一万行，避免阻塞在线接口。

## 尚未启用的能力

- Iceberg/Delta Lake 表格式
- Spark/Trino 分布式计算
- 本地 Embedding 模型和独立向量数据库
- 自动调度全量历史回填（当前提供可控的手动批量回填）

这些能力需要达到容量、并发或恢复要求后再增加。
