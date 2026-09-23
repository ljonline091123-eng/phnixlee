# 湖仓兼容层实施说明

本轮实现的是第一阶段轻量湖仓兼容层，不引入 Iceberg、Spark 或 Trino。

## 已实现

- `LakeObject`：原始对象、Normalized 对象和 Serving 对象的 URI、哈希、来源记录和数据集版本。
- `LakeDataset`/`LakeDatasetVersion`：Parquet 数据集目录和版本快照。
- `LakeLineageEvent`：来源表、转换步骤、数据集版本和批次之间的统一血缘。
- `DocumentChunkVersion`：文档切片内容哈希、切片偏移、解析器版本和 Embedding 模型版本。
- 文件系统对象存储：本机默认模式，无额外服务依赖。
- MinIO 对象存储：通过 `LAKE_STORAGE_BACKEND=minio` 切换，使用 S3 兼容 API。
- `/api/v1/lakehouse/status`：查看后端、对象、数据集、切片和血缘数量。
- `/api/v1/lakehouse/datasets`：查看数据集目录。
- `/api/v1/lakehouse/lineage`：查看转换血缘。
- `/api/v1/lakehouse/datasets/export`：将受支持的业务表导出为 Parquet。
- `/api/v1/lakehouse/documents/chunks`：创建可重复的文档切片版本。

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

当前可导出的业务表为：`stock_symbol`、`stock_kline`、`stock_news`、`stock_notice`、`stock_financial_report`、`foundation_entity` 和 `foundation_fact`。导出采用有限行数保护，不扫描全库，不影响在线接口。

## 尚未启用的能力

- Iceberg/Delta Lake 表格式
- Spark/Trino 分布式计算
- 本地 Embedding 模型和独立向量数据库
- 自动全量回填历史原文

这些能力需要达到容量、并发或恢复要求后再增加。
