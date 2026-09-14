# 数据库表说明

当前本地数据库是 `backend/quant.db`。SQLite 没有原生的 `COMMENT ON TABLE` 表属性，因此项目把中文表名、业务领域、用途及字段释义保存在 `database_table_comment` 表中。现有 34 张业务表及说明表自身均有记录；`column_comments_json` 以“字段名 → 中文释义”的 JSON 对象保存每张表的全部字段。SQLite 客户端不会自动把这些说明显示为表名旁的悬浮注释；在 VS Code Database Client 中刷新表列表，打开 `database_table_comment` 即可查看。

也可在 SQL 编辑器运行：

```sql
SELECT table_name, display_name, domain, description, column_comments_json
FROM database_table_comment
ORDER BY domain, table_name;
```

查单张表的用途，例如：

```sql
SELECT display_name, description
FROM database_table_comment
WHERE table_name = 'agent_child_link';
```

逐行查看某张表的字段释义：

```sql
SELECT t.table_name, j.key AS column_name, j.value AS column_description
FROM database_table_comment AS t,
     json_each(t.column_comments_json) AS j
WHERE t.table_name = 'stock_kline'
ORDER BY j.key;
```

说明内容维护在 `backend/app/db/table_comments.py`。服务启动时会同步到数据库；已存在的本地数据库也可以手动运行 `cd backend` 后执行 `python -m scripts.sync_table_comments --backup`。该命令先用 SQLite 在线备份保存一份快照，再迁移并同步说明表。SQLAlchemy 元数据也使用相同的表和字段说明，供支持原生注释的数据库在新建表时使用。
