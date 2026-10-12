import { useEffect, useMemo, useState } from "react";
import { request } from "./api";
import type { BatchGovernanceJob } from "./batchGovernanceApi";

export type SemanticPlan = { plan_id: string; plan_version: number; revision: number; status: string; overview: string; primary_type: string;
  secondary_types: string[]; relevance: string; target_scopes: string[]; rationale: string;
  expected_knowledge: string[]; uncertainties: string[]; user_confirmation_questions: string[];
  recommended_skill_codes: string[]; capability_gaps: { type_name: string; suggested_skill_name: string; reason: string }[];
  confirmed_by?: string; confirmed_at?: string };
type Point = { point_type: "EVENT" | "RELATION"; key: string; knowledge_type: string; summary: string;
  subjects: string[]; event_time?: string; metrics: string[]; direction: string; assertion_type: string;
  evidence_quotes: string[]; source_url?: string; confirmation_level: string; confirmation_reason?: string;
  review_status: string };
export type SemanticReport = { version_key: string; plan_id: string; plan_revision: number; version_index: number; title: string; source_name: string; source_url?: string; overview: string; knowledge_points: Point[];
  uncertainties: string[]; workflow_status: string; publication_status: string; note: string };
type Catalog = { types: { code: string; name: string; skill: string; description: string; skill_status: string }[];
  skills: { skill_code: string; skill_name: string; description: string; version?: string; enabled: boolean; lifecycle_status: string }[]; note: string };
type Detail = { source_table: string; record_id: number; title: string; selected_version: number; workflow_status?: string;
  governance_plan?: SemanticPlan; result_report?: SemanticReport; analysis?: unknown; url?: string };
type Draft = { id: number; skill_code: string; skill_name: string; instructions: string; lifecycle_status: string;
  test_result?: { passed: boolean; event_count: number; relation_count: number; error?: string; note: string } };

const typeNames: Record<string, string> = {
  FINANCIAL_DISCLOSURE: "财务披露与业绩", CAPITAL_OPERATION: "资本运作", SHAREHOLDER_PERSONNEL: "股东与人事",
  JUDICIAL_RISK: "司法与风险", CORPORATE_OPERATION: "公司经营事项", SUPPLY_CHAIN: "供应链与经营往来",
  POLICY_INDUSTRY: "政策与产业事件", SECURITY_MARKET: "证券市场观测", SECTOR_MARKET: "板块市场观测",
  MARKET_STATISTICS: "榜单与市场统计", RESEARCH_OPINION: "研究观点", GENERAL_EVIDENCE: "通用证据事实",
  NO_RELEVANT_FACT: "无直接相关事实",
};
const scopeNames: Record<string, string> = { COMPANY: "公司", SECURITY: "证券", SECTOR: "板块", INDUSTRY: "行业", MARKET: "市场", PERSON: "人物", OTHER: "其他" };
const directionNames: Record<string, string> = { POSITIVE: "正向", NEGATIVE: "负向", NEUTRAL: "中性", MIXED: "正负并存", UNKNOWN: "不确定" };
const assertionNames: Record<string, string> = { REPORTED_FACT: "文中转述事实", AUTHOR_OPINION: "作者／专家观点", MODEL_INFERENCE: "模型推断" };
const workflowNames: Record<string, string> = { NEEDS_PLAN: "待制定方案", NEEDS_PLAN_CONFIRMATION: "待确认方案", READY_TO_EXECUTE: "待执行已确认方案", NEEDS_RESULT_CONFIRMATION: "待确认知识点", NEEDS_REANALYSIS: "等待定向重分析", PUBLISHED: "最终成果已发布", SKIPPED: "已选择不治理" };
const safeUrl = (value?: string) => /^https?:\/\//i.test(value || "") ? value : undefined;

export function InteractiveSemanticFlow({ detail, isLatest, reviewer, onReviewer, busy, setBusy, setError, onReload, onExecute, onJob }:
  { detail: Detail; isLatest: boolean; reviewer: string; busy: boolean; setBusy: (value: boolean) => void; setError: (value: string) => void;
    onReviewer: (value: string) => void; onReload: () => Promise<void>; onExecute: () => Promise<void>; onJob: (job: BatchGovernanceJob) => void }) {
  const [catalog, setCatalog] = useState<Catalog | null>(null);
  const [primary, setPrimary] = useState("GENERAL_EVIDENCE"), [secondary, setSecondary] = useState<string[]>([]);
  const [skills, setSkills] = useState<string[]>([]), [scopes, setScopes] = useState<string[]>(["COMPANY"]);
  const [reason, setReason] = useState(""), [rejected, setRejected] = useState<Record<string, string>>({});
  const [confirmed, setConfirmed] = useState<string[]>([]), [drafts, setDrafts] = useState<Record<number, Draft>>({});
  const plan = detail.governance_plan;
  const report = detail.result_report ? { ...detail.result_report,
    knowledge_points: detail.result_report.knowledge_points.map(point => ({ ...point, source_url: safeUrl(point.source_url) })) } : undefined;
  const currentReport = !!report && report.plan_id === plan?.plan_id && report.plan_revision === plan?.revision;
  const canReview = isLatest && currentReport && plan?.status === "CONFIRMED" && detail.workflow_status === "NEEDS_RESULT_CONFIRMATION";
  const refreshCatalog = () => request<Catalog>("/document-governance/planning-catalog").then(setCatalog);
  useEffect(() => { void request<Catalog>("/document-governance/planning-catalog").then(setCatalog).catch(e => setError(e instanceof Error ? e.message : "治理目录读取失败")); }, []);
  useEffect(() => {
    setPrimary(plan?.primary_type || "GENERAL_EVIDENCE"); setSecondary(plan?.secondary_types || []);
    setSkills(plan?.recommended_skill_codes || []); setScopes(plan?.target_scopes || ["COMPANY"]); setReason(""); setRejected({}); setConfirmed([]); setDrafts({});
  }, [detail.source_table, detail.record_id, plan?.plan_id, plan?.revision, detail.selected_version]);
  useEffect(() => {
    let cancelled = false;
    if (plan?.status === "PROPOSED") void request<Record<number, Draft>>(`/document-governance/documents/${detail.source_table}/${detail.record_id}/skill-drafts`)
      .then(values => { if (!cancelled) { setDrafts(values); setSkills(current => [...new Set([...current, ...Object.values(values).filter(item => item.lifecycle_status === "ENABLED").map(item => item.skill_code)])]); } })
      .catch(e => { if (!cancelled) setError(e instanceof Error ? e.message : "专项Skill草稿读取失败"); });
    return () => { cancelled = true; };
  }, [detail.source_table, detail.record_id, plan?.plan_id, plan?.revision]);
  const steps = useMemo(() => [
    ["READ", "LLM阅读全文", !!plan], ["PLAN", "确认类型与Skill", ["CONFIRMED", "SKIPPED"].includes(plan?.status || "")],
    ["EXECUTE", "执行治理", currentReport], ["REVIEW", "确认知识点", currentReport && report?.publication_status === "PUBLISHED"],
    ["PUBLISH", "最终成果", detail.workflow_status === "PUBLISHED"],
  ] as const, [plan, detail.analysis, report, detail.workflow_status]);
  async function generate(force = false) {
    setBusy(true); setError("");
    try { const job = await request<BatchGovernanceJob>("/document-governance/plan-batch", { method: "POST", body: JSON.stringify({ force,
      documents: [{ source_table: detail.source_table, record_id: detail.record_id }], idempotency_key: crypto.randomUUID() }) }); onJob(job); }
    catch (e) { setError(e instanceof Error ? e.message : "治理方案生成失败"); } finally { setBusy(false); }
  }
  async function decide(decision: "CONFIRM" | "SKIP") {
    if (!reviewer.trim()) { setError("请先填写审核人"); return; }
    setBusy(true); setError("");
    try {
      await request(`/document-governance/documents/${detail.source_table}/${detail.record_id}/plan-decision`, { method: "POST", body: JSON.stringify({
        decision, primary_type: primary, secondary_types: secondary, skill_codes: skills, target_scopes: scopes,
        reviewer: reviewer.trim(), reason: reason.trim(), expected_plan_id: plan?.plan_id, expected_revision: plan?.revision,
      }) });
      await onReload();
    } catch (e) { setError(e instanceof Error ? e.message : "治理方案确认失败"); } finally { setBusy(false); }
  }
  async function confirmResult(decision: "CONFIRM" | "SKIP") {
    if (!reviewer.trim()) { setError("请先填写审核人"); return; }
    const nonconforming = Object.entries(rejected).filter(([, value]) => value.trim()).map(([identity, value]) => {
      const [point_type, ...rest] = identity.split(":"); return { point_type, key: rest.join(":"), reason: value.trim() };
    });
    const markedWithoutReason = Object.keys(rejected).some(key => !rejected[key].trim());
    if (markedWithoutReason && decision === "CONFIRM") { setError("请为每个不符合知识点填写修正意见"); return; }
    setBusy(true); setError("");
    try {
      const result = await request<{ job?: BatchGovernanceJob }>(`/document-governance/documents/${detail.source_table}/${detail.record_id}/result-review`, {
        method: "POST", body: JSON.stringify({ version_index: detail.selected_version, decision, reviewer: reviewer.trim(),
          expected_version_key: report?.version_key, expected_plan_id: plan?.plan_id, explicit_confirmations: confirmed,
          reason: reason.trim(), nonconforming: decision === "CONFIRM" ? nonconforming : [] }),
      });
      if (result.job) onJob(result.job);
      await onReload();
    } catch (e) { setError(e instanceof Error ? e.message : "知识点确认失败"); } finally { setBusy(false); }
  }
  async function retryCorrection() {
    setBusy(true); setError("");
    try {
      const job = await request<BatchGovernanceJob>(`/document-governance/documents/${detail.source_table}/${detail.record_id}/retry-correction`, { method: "POST" });
      onJob(job); await onReload();
    } catch (e) { setError(e instanceof Error ? e.message : "修正重试失败"); } finally { setBusy(false); }
  }
  function toggle(values: string[], value: string, setter: (next: string[]) => void) {
    setter(values.includes(value) ? values.filter(item => item !== value) : [...values, value]);
  }
  async function draftAction(index: number, action: "create" | "test" | "enable") {
    if (!reviewer.trim()) { setError("请填写审核人"); return; }
    setBusy(true); setError("");
    try {
      const path = action === "create" ? `/document-governance/documents/${detail.source_table}/${detail.record_id}/skill-draft` : `/document-governance/skill-drafts/${drafts[index].id}/${action}`;
      const result = await request<Draft>(path, { method: "POST", body: JSON.stringify(action === "create" ?
        { gap_index: index, expected_plan_id: plan?.plan_id, reviewer: reviewer.trim() } : { reviewer: reviewer.trim() }) });
      setDrafts(values => ({ ...values, [index]: result })); await refreshCatalog();
      if (action === "enable") setSkills(values => [...new Set([...values, result.skill_code])]);
    } catch (e) { setError(e instanceof Error ? e.message : "专项Skill操作失败"); } finally { setBusy(false); }
  }
  function knowledgeCard(point: Point, editable = false) {
    const id = `${point.point_type}:${point.key}`, marked = Object.prototype.hasOwnProperty.call(rejected, id);
    return <article key={id} className={`${marked ? "nonconforming" : ""} ${point.confirmation_level === "REQUIRED" ? "required-confirmation" : ""}`}>
      <header><div><h5>{point.knowledge_type}</h5><span className={`ds-direction ds-${point.direction.toLowerCase()}`}>{directionNames[point.direction] || point.direction}</span></div>
        {editable && point.confirmation_level !== "NORMAL" && <strong>{point.confirmation_level === "REQUIRED" ? "模型请求重点确认" : "建议确认"}</strong>}</header>
      <p>{point.summary}</p>{!!point.subjects.length && <p><b>涉及对象：</b>{point.subjects.join("、")}</p>}
      {point.event_time && <p><b>时间：</b>{point.event_time}</p>}{!!point.metrics.length && <p><b>关键指标：</b>{point.metrics.join("；")}</p>}
      <p><b>信息性质：</b>{assertionNames[point.assertion_type] || point.assertion_type}</p>
      {editable && point.confirmation_reason && <p className="ds-confirm-warning">{point.confirmation_reason}</p>}
      {point.evidence_quotes.map((quote, i) => <blockquote key={i}>{quote}</blockquote>)}
      {point.source_url && <a href={point.source_url} target="_blank" rel="noreferrer">查看原文</a>}
      {editable && <><label className="ds-nonconforming"><input type="checkbox" checked={marked} disabled={busy} onChange={e => setRejected(current => { const next = { ...current }; if (e.target.checked) next[id] = ""; else delete next[id]; return next; })} />该知识点不符合</label>
        {marked && <textarea rows={2} disabled={busy} value={rejected[id]} onChange={e => setRejected(current => ({ ...current, [id]: e.target.value }))} placeholder="说明主体、类型、数值、方向、证据或遗漏问题；模型将只针对问题项重分析" />}
        {point.confirmation_level === "REQUIRED" && <label className="ds-explicit-confirm"><input type="checkbox" checked={confirmed.includes(id) || point.review_status === "HUMAN_CONFIRMED"} disabled={busy || point.review_status === "HUMAN_CONFIRMED" || marked} onChange={() => toggle(confirmed, id, setConfirmed)} />我已核对并确认该重点项</label>}</>}
    </article>;
  }
  const missingRequired = report?.knowledge_points.filter(point => point.confirmation_level === "REQUIRED" && point.review_status !== "HUMAN_CONFIRMED" &&
    !confirmed.includes(`${point.point_type}:${point.key}`) && !Object.prototype.hasOwnProperty.call(rejected, `${point.point_type}:${point.key}`)).length || 0;
  return <section className="ds-interactive-flow" aria-label="交互式治理流程">
    <header><div><h3>交互式知识治理</h3><p>点击“确认本批结果”后，普通项未标记不符合即视为符合；重点项须单独确认。</p></div><span className="ds-flow-state">{workflowNames[detail.workflow_status || "NEEDS_PLAN"] || "待查看"}</span></header>
    <label className="ds-reviewer">审核人<input value={reviewer} onChange={e => onReviewer(e.target.value)} placeholder="填写审核人姓名" /></label>
    <ol className="ds-flow-steps">{steps.map(([key, label, complete], index) => <li key={key} className={complete ? "complete" : ""}><b>{complete ? "✓" : index + 1}</b><span>{label}</span></li>)}</ol>
    {!plan && <div className="ds-flow-action"><h4>第一步：LLM阅读全文并制定治理方案</h4><p>模型只提出文档类型、知识归属和Skill组合，本阶段不会生成或发布正式知识。</p><button className="ds-primary" disabled={busy} onClick={() => void generate()}>{busy ? "正在阅读全文…" : "生成治理方案"}</button></div>}
    {plan?.status === "PROPOSED" && catalog && <div className="ds-plan-editor">
      <h4>第二步：确认文档类型与治理Skill</h4><p className="ds-plan-overview">{plan.overview}</p><p><strong>模型判断依据：</strong>{plan.rationale}</p>
      <div className="ds-plan-grid"><label>主要类型<select aria-label="主要类型" value={primary} onChange={e => setPrimary(e.target.value)}>{catalog.types.map(item => <option value={item.code} key={item.code}>{item.name}</option>)}</select></label>
        <fieldset><legend>次要类型（可多选）</legend>{catalog.types.filter(item => item.code !== primary).map(item => <label key={item.code}><input type="checkbox" checked={secondary.includes(item.code)} onChange={() => toggle(secondary, item.code, setSecondary)} />{item.name}</label>)}</fieldset>
        <fieldset><legend>知识归属对象</legend>{Object.entries(scopeNames).map(([code, label]) => <label key={code}><input type="checkbox" checked={scopes.includes(code)} onChange={() => toggle(scopes, code, setScopes)} />{label}</label>)}</fieldset>
        <fieldset><legend>执行Skill（可修改）</legend>{catalog.skills.filter(item => item.skill_code !== "DOCUMENT_SEMANTIC_ROUTER").map(item => <label className={!item.enabled ? "disabled" : ""} key={item.skill_code}><input type="checkbox" disabled={!item.enabled || item.lifecycle_status !== "ENABLED"} checked={skills.includes(item.skill_code)} onChange={() => toggle(skills, item.skill_code, setSkills)} />{item.skill_name}<small>v{item.version || "未安装"}</small></label>)}</fieldset></div>
      {!!plan.expected_knowledge.length && <p><strong>预计提取：</strong>{plan.expected_knowledge.join("；")}</p>}
      {!!plan.uncertainties.length && <div className="ds-confirm-warning"><strong>模型不确定项</strong><ul>{plan.uncertainties.map((item, i) => <li key={i}>{item}</li>)}</ul></div>}
      {!!plan.user_confirmation_questions.length && <div className="ds-confirm-warning"><strong>请核对这些问题</strong><ul>{plan.user_confirmation_questions.map((item, i) => <li key={i}>{item}</li>)}</ul></div>}
      {!!plan.capability_gaps.length && <div className="ds-confirm-warning"><strong>能力缺口</strong>{plan.capability_gaps.map((item, i) => <div key={i}><p>{item.type_name}：{item.reason}</p>
        {!drafts[i] ? <button disabled={busy} onClick={() => void draftAction(i, "create")}>创建专项Skill草稿</button> : <div><p>{drafts[i].skill_name} · {drafts[i].lifecycle_status === "ENABLED" ? "已启用" : "待测试／确认"}</p><details><summary>查看草稿治理规则</summary><pre className="ds-skill-instructions">{drafts[i].instructions}</pre></details>
        {drafts[i].test_result && <p role="status">{drafts[i].test_result?.passed ? "证据与契约测试通过" : "测试未通过"} · {drafts[i].test_result?.note} {drafts[i].test_result?.error}</p>}
        <button disabled={busy || drafts[i].lifecycle_status === "ENABLED"} onClick={() => void draftAction(i, "test")}>{busy ? "测试中…" : "测试草稿（不入事实库）"}</button><button disabled={busy || !drafts[i].test_result?.passed || drafts[i].lifecycle_status === "ENABLED"} onClick={() => void draftAction(i, "enable")}>审核通过并启用</button></div>}
      </div>)}</div>}
      <label>确认说明（可选）<textarea rows={2} value={reason} onChange={e => setReason(e.target.value)} placeholder="可说明为何调整类型、Skill或归属对象" /></label>
      <div className="ds-review-actions"><button disabled={busy} onClick={() => void generate(true)}>重新阅读并生成</button><button disabled={busy} onClick={() => void decide("SKIP")}>本篇不治理</button><button className="ds-primary" disabled={busy || !skills.length || !scopes.length} onClick={() => void decide("CONFIRM")}>确认方案并进入执行</button></div>
    </div>}
    {plan?.status === "CONFIRMED" && detail.workflow_status === "READY_TO_EXECUTE" && <div className="ds-flow-action"><h4>第三步：按确认方案执行治理</h4><p>{typeNames[plan.primary_type]} · {plan.recommended_skill_codes.length}个Skill · 归属到{plan.target_scopes.map(item => scopeNames[item] || item).join("、")}</p><button className="ds-primary" disabled={busy} onClick={() => void onExecute()}>{busy ? "正在提交…" : "执行结构化治理"}</button></div>}
    {isLatest && detail.workflow_status === "NEEDS_REANALYSIS" && <div className="ds-flow-action"><p role="status">修正意见已保存，后台只重分析问题项并保留已确认项。任务失败或网络恢复后，可从原意见继续重试。</p><div className="ds-review-actions"><button disabled={busy} onClick={() => void retryCorrection()}>按原意见继续修正</button><button disabled={busy} onClick={() => void confirmResult("SKIP")}>本篇不治理</button></div></div>}
    {plan?.status === "SKIPPED" && <div className="ds-flow-action"><h4>本篇已选择不治理</h4><p>原文仍保留在知识库供检索，不生成或发布结构化事实。</p><button disabled={busy} onClick={() => void generate(true)}>重新制定方案</button></div>}
    {canReview && report && report.publication_status !== "PUBLISHED" && <div className="ds-result-review"><h4>第四步：确认结构化知识点</h4><p>{report.overview}</p><p className="ds-note">{report.note}</p>
      <div className="ds-knowledge-points">{report.knowledge_points.map(point => knowledgeCard(point, true))}</div>
      {!report.knowledge_points.length && <p className="ds-empty">模型未发现目标对象的直接结构化知识点。确认后将以“无相关事实”完成，不会制造事件。</p>}
      {!!report.uncertainties.length && <div className="ds-confirm-warning"><strong>全文待核对事项</strong><ul>{report.uncertainties.map((item, i) => <li key={i}>{item}</li>)}</ul></div>}
      {missingRequired > 0 && <p className="ds-confirm-warning">还有 {missingRequired} 个重点项需要明确确认或标记不符合。</p>}
      <label>本轮说明（可选）<textarea rows={2} value={reason} onChange={e => setReason(e.target.value)} /></label><div className="ds-review-actions"><button disabled={busy} onClick={() => void confirmResult("SKIP")}>本篇不治理</button><button className="ds-primary" disabled={busy || missingRequired > 0} onClick={() => void confirmResult("CONFIRM")}>{Object.keys(rejected).length ? `提交${Object.keys(rejected).length}项修正并重分析` : "确认本批结果并发布"}</button></div>
    </div>}
    {report?.publication_status === "PUBLISHED" && <div className="ds-final-report"><h4>{isLatest && currentReport ? "第五步：最终成果与知识结构化报告" : "历史已发布成果（只读）"}</h4><p>{report.overview}</p><p>已确认并发布 {report.knowledge_points.length} 个知识点。原文、来源和审核历史保留；人工确认表示抽取结构已核对。</p><div className="ds-knowledge-points">{report.knowledge_points.map(point => knowledgeCard(point))}</div></div>}
    {report && !canReview && report.publication_status !== "PUBLISHED" && <div className="ds-final-report"><h4>{isLatest && currentReport ? "修正前成果（意见已保存）" : "历史候选成果（只读）"}</h4><p>{report.overview}</p><div className="ds-knowledge-points">{report.knowledge_points.map(point => knowledgeCard(point))}</div></div>}
    {plan && plan.status !== "PROPOSED" && !busy && <button onClick={() => void generate(true)}>重新制定方案（保留历史成果）</button>}
  </section>;
}
