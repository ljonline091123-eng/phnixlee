# PREDICTION_LEDGER_WRITER

## 目标

把已经通过结构校验、Skill 绑定校验和证据校验的候选预测写入预测账本。

## 强制约束

- 只能由后端 `prediction_ledger_service` 调用，不接受 Agent 或 LLM 生成的 SQL。
- 只允许向 `prediction_ledger` 执行 `INSERT`，不得更新或删除其他数据。
- 每次写入必须提供幂等键；相同键和相同输入复用原结果，不得重复落库。
- 候选预测必须引用当前 Agent 已绑定且启用的分析 Skill。
- 写入审计必须保留 Agent 运行、模型调用、Skill 版本、证据 ID、输入输出哈希和写入范围。
- 任一权限、范围或幂等条件不满足时，只返回候选结果，不写数据库。

## 输入

- `candidates`：最多 50 条已校验候选预测。
- `idempotency_key`：调用方显式提供的幂等键。
- `evidence_ids`：支持预测结论的证据标识。

## 输出

- 成功时返回新增的预测账本 ID。
- 条件不满足时返回候选状态和未落库原因。
