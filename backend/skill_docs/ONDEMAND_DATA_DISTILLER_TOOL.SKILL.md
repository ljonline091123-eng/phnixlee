# ONDEMAND_DATA_DISTILLER_TOOL

类型：`EXECUTABLE_TOOL`。函数：`trigger_ondemand_extraction(stock_code, doc_id)`。

只从 `knowledge_document.id` 所锚定的 `stock_news`、`stock_notice`、`stock_financial_report` 单条业务记录读取真实原文；验证股票代码、来源表白名单和来源主键。返回原文、来源 URL、抓取时间及 SHA-256。无正文时返回错误并等待按需抓取，不得用标题填充正文。工具不调用 LLM，也不写知识库或图谱。对外 API 为 `POST /api/v1/model-hub/skill-tools/trigger-ondemand-extraction`。
