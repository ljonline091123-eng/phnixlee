import { useEffect, useRef, useState, type FormEvent } from "react";
import type { GovernanceRun } from "./api";
import { knowledgeGovernanceApi, type KnowledgeGovernanceCatalog, type KnowledgeGovernanceSummary } from "./knowledgeGovernanceApi";
import "./ResourcePreview.css";

const statusNames: Record<string, string> = {
  UNREGISTERED: "未登记资产", DISABLED: "资产已停用", UNAVAILABLE: "来源不可用", EMPTY: "来源暂无数据",
  SAMPLED: "已生成限定样本", MATERIALIZED: "已生成知识文档", PENDING_REVIEW: "待审核或补齐", READY: "可治理", RESTRICTED_COLUMNS: "读取字段权限不完整",
  AVAILABLE: "来源可用", PENDING: "尚未治理", PARTIAL: "部分覆盖", GOVERNED: "已治理", NOT_CALLED: "尚未调用模型", SUCCESS: "本次范围治理完成", COMPLETED: "本次范围治理完成",
  FAILED: "执行失败", RUNNING: "正在治理", ACCEPT: "通过", ACCEPTED: "通过", REVIEW: "需要复核",
  REJECT: "未通过", REJECTED: "未通过", MOCK: "模拟模型", SKIPPED: "未执行",
  UNBOUND_ASSET: "智能体未绑定资产", INSUFFICIENT_DATA: "证据不足", MOCK_REJECTED: "模拟结果未采纳",
  OUTPUT_LIMIT_REACHED: "模型输出额度不足",
};
const statusName = (value: string) => statusNames[value] || "待核实";
const sourceStatusName = (source: { status: string; materialization_status?: string }) => source.materialization_status === "NOT_PUBLISHED" ? "未发布（本次已回滚）" : statusName(source.status);
const explanation = (value: unknown) => typeof value === "string" ? value : value && typeof value === "object" ? Object.values(value as Record<string, unknown>).filter(item => typeof item === "string").join("；") : String(value ?? "");

export function KnowledgeGovernancePanel({ reload, notify }: { reload: () => Promise<void>; notify: (text: string) => void }) {
  const [catalog, setCatalog] = useState<KnowledgeGovernanceCatalog | null>(null);
  const [result, setResult] = useState<GovernanceRun | null>(null);
  const [recordLimit, setRecordLimit] = useState(50);
  const [loading, setLoading] = useState(true), [busy, setBusy] = useState(false), [error, setError] = useState("");
  const [revision, setRevision] = useState(0);
  const runningRef = useRef(false);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    void knowledgeGovernanceApi.catalog(controller.signal).then(data => {
      if (!controller.signal.aborted) { setCatalog(data); setResult(data.latest_run); setError(""); }
    }).catch(error => { if (!controller.signal.aborted) setError(error instanceof Error ? error.message : "读取治理来源失败"); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [revision]);
  const running = busy || result?.status === "RUNNING";
  useEffect(() => {
    if (!running) return;
    const interval = window.setInterval(() => setRevision(old => old + 1), 15000);
    return () => window.clearInterval(interval);
  }, [running]);
  async function govern(event: FormEvent) {
    event.preventDefault();
    if (runningRef.current || running || !catalog) return;
    runningRef.current = true; setBusy(true); setError("");
    const runKey = `ui-${Date.now()}-${crypto.randomUUID()}`;
    try {
      const next = await knowledgeGovernanceApi.run(recordLimit, runKey);
      setResult(next);
      const summary = next.summary_json as KnowledgeGovernanceSummary;
      if (next.status === "FAILED") setError(summary.error || "治理执行失败，请查看运行记录。");
      else notify(next.status === "PENDING_REVIEW" ? "知识库治理已生成新版本，仍有来源或审核事项待补齐。" : "本次知识库治理完成，请查看逐来源覆盖报告。");
      await reload();
      const updated = await knowledgeGovernanceApi.catalog(); setCatalog(updated);
    } catch (error) {
      setError(`${error instanceof Error ? error.message : "治理请求失败"}。可刷新来源检查任务最终状态。`);
    } finally { runningRef.current = false; setBusy(false); setRevision(old => old + 1); }
  }
  const summary = result?.summary_json as KnowledgeGovernanceSummary | undefined;
  const sources = summary?.source_coverage || catalog?.source_coverage || [];
  const created = summary?.created_knowledge_bases || [];
  return <section className="panel resource-management-panel knowledge-governance-panel" aria-label="智能体与技能知识库治理">
    <div className="panel-heading"><div><p className="eyebrow">知识治理</p><h2>智能体 + Skill 治理</h2><p>审核多类数据来源，生成分领域知识库及图谱版本，并记录实际覆盖范围。</p></div><button type="button" disabled={loading} onClick={() => setRevision(old => old + 1)}>刷新来源与结果</button></div>
    <div className="knowledge-governance-content">
      {error && <p className="form-error" role="alert">{error}</p>}
      {loading && !catalog && <p role="status">正在读取可治理的数据来源…</p>}
      <div className="knowledge-governance-groups">{catalog?.source_groups.map(group => <span key={group.code} title={group.source_tables.join("、")}>{group.name}</span>)}</div>
      <form className="knowledge-governance-toolbar" onSubmit={event => void govern(event)}><label>每个来源最多处理记录<input aria-label="每个来源处理上限" type="number" min={1} max={500} step={1} required value={recordLimit} disabled={running} onChange={event => setRecordLimit(Number(event.target.value))} /></label><button type="submit" className="primary-button" disabled={!catalog || running || recordLimit < 1 || recordLimit > 500}>{running ? "智能体正在治理…" : "运行智能体与 Skill 治理"}</button></form>
      <p className="knowledge-governance-note">智能体按 Skill 对每个来源最多 1 条摘要进行审核；程序按所选记录上限生成知识文档和关系。模型未逐条审核全部归档记录。报告区分原始总量、实际生成量和缺口；限定样本不代表全市场覆盖。</p>
      {running && <p className="knowledge-governance-note" role="status">正在审核来源并生成新版本。页面会定期更新执行状态。</p>}
      <div className="knowledge-governance-report">
        {summary?.error && error !== summary.error && <p className="form-error" role="alert">{summary.error}</p>}
        {result && <><h3>最近治理结果 · #{result.id}</h3><span className={`knowledge-governance-status${summary?.complete ? "" : " pending"}`}>{statusName(result.status)} · {new Date(result.created_at).toLocaleString("zh-CN")}</span><p className="knowledge-governance-note">{summary?.complete ? "本次配置的来源已完成治理，详情见下表。" : "本次结果仍有来源缺口、样本限制或待审核事项。"}{summary?.record_limit_per_source ? ` 每来源处理上限：${summary.record_limit_per_source} 条。` : ""}</p></>}
        {!!sources.length && <div className="table-wrap"><table><thead><tr><th>数据来源</th><th>原始记录</th><th>生成知识文档</th><th>覆盖状态</th><th>智能体审核</th></tr></thead><tbody>{sources.map(source => <tr key={source.source_table}><td><strong>{source.display_name || source.source_table}</strong><code>{source.source_table}</code></td><td>{source.row_count_known === false ? "未统计" : source.total_records.toLocaleString()}</td><td>{source.documents_created == null ? "尚未治理" : source.documents_created.toLocaleString()}</td><td>{sourceStatusName(source)}{source.limited && <><br /><small>仅处理限定样本</small></>}</td><td>{source.model_review ? <>{statusName(source.model_review.decision || "REVIEW")}{!!source.model_review.issues?.length && <details><summary>查看审核说明（{source.model_review.issues.length}）</summary>{source.model_review.issues.map((issue, index) => <p key={index}>{explanation(issue)}</p>)}</details>}{!!source.model_review.evidence_refs?.length && <details><summary>审核依据</summary>{source.model_review.evidence_refs.map((evidence, index) => <p key={index}>{explanation(evidence)}</p>)}</details>}</> : "尚无审核结果"}</td></tr>)}</tbody></table></div>}
        {!!created.length && <><h3>本次生成的知识库版本</h3><div className="table-wrap"><table><thead><tr><th>知识库</th><th>文档</th><th>实体</th><th>关系</th><th>状态</th></tr></thead><tbody>{created.map(base => <tr key={base.id}><td><strong>{base.kb_name}</strong><code>{base.kb_code}</code></td><td>{base.documents_created.toLocaleString()}</td><td>{base.entities_created.toLocaleString()}</td><td>{base.relations_created.toLocaleString()}</td><td>{statusName(base.status)}</td></tr>)}</tbody></table></div></>}
        {summary?.skill_snapshot && <details><summary>查看本次技能与模型执行记录</summary><p>技能：{summary.skill_snapshot.skill_code} · 版本 {summary.skill_snapshot.version}</p><p>内容校验：<code>{summary.skill_snapshot.content_hash}</code></p><p>模型调用记录：{summary.model_call_log_id ? `#${summary.model_call_log_id}` : "未生成"} · {summary.model_status === "SUCCESS" ? "调用成功" : summary.model_status ? statusName(summary.model_status) : "尚未执行"}</p>{summary.run_key && <p>运行标识：<code>{summary.run_key}</code></p>}</details>}
      </div>
    </div>
  </section>;
}
