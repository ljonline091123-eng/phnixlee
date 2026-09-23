# Windows 原生 MinIO

本机已安装原生 Windows AMD64 MinIO，运行路径为 `C:\MinIO\minio.exe`，数据目录为 `C:\MinIO\data`。

## 当前实例

- API：`http://127.0.0.1:9000`
- 控制台：`http://127.0.0.1:9001`
- 存储桶：`quant-lake`
- 账号：`minioadmin`
- 密码：保存在被 Git 忽略的 `backend/.env`，不要提交到仓库
- 自动启动：Windows 计划任务 `GeminiQuantMinIO`，用户登录时启动且无运行时限

启动命令：

```powershell
$env:MINIO_ROOT_USER="minioadmin"
$env:MINIO_ROOT_PASSWORD="<与 backend/.env 一致的本机密码>"
C:\MinIO\minio.exe server C:\MinIO\data --address "127.0.0.1:9000" --console-address "127.0.0.1:9001"
```

项目后端从 `backend/.env` 读取：

```text
LAKE_STORAGE_BACKEND=minio
LAKE_S3_ENDPOINT=127.0.0.1:9000
LAKE_S3_BUCKET=quant-lake
LAKE_S3_SECURE=false
```

## 版本与来源

官方 MinIO 下载站已返回 `410 Gone`，不再提供开源社区二进制。当前文件由 MinIO 官方 GitHub 标签 `RELEASE.2025-10-15T17-29-55Z` 源码构建，源码包 SHA256 为：

```text
C0FA910BC7B546291FD0460E26CC965DF8D1EF22DD039CB40A257E14F619B26F
```

该版本是 AGPLv3 社区版，已停止后续社区维护，仅建议用于本机开发验证，不应作为长期生产安全基线。

本机构建的 `C:\MinIO\minio.exe` SHA256：

```text
C15388FDC9C5D21858658B6B97D181E97B4FA8B7DA74782FE095DA46E5A06CE8
```

服务仅监听 `127.0.0.1`，不会暴露到局域网。启动脚本为 `C:\MinIO\start-minio.ps1`，ACL 仅允许 Administrator 和 SYSTEM 读取。

## 启停

```powershell
schtasks /Run /TN GeminiQuantMinIO
schtasks /End /TN GeminiQuantMinIO
schtasks /Query /TN GeminiQuantMinIO /V /FO LIST
```

## 验证

```powershell
Invoke-WebRequest http://127.0.0.1:9000/minio/health/live
Invoke-WebRequest http://127.0.0.1:8000/api/v1/lakehouse/status
```

湖仓状态应显示 `storage_backend=minio` 且 `storage_health.healthy=true`。
