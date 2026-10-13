import { useEffect, useMemo, useState } from "react";
import { request } from "./api";
import type { BatchGovernanceJob } from "./batchGovernanceApi";

export type SemanticPlan = { plan_id: string; plan_version: number; revision: number; status: string; overview: string; primary_type: string;
  secondary_types: string[]; relevance: string; target_scopes: string[]; rationale: string;
  expected_knowledge: string[]; uncertainties: string[]; user_confirmation_questions: string[];
  recommended_skill_codes: string[]; capability_gaps: { type_name: string; suggested_skill_name: string; reason: string }[];
  failure_diagnosis?: { failure_class: string; summary: string; root_causes: string[]; existing_skill_limitations: string[];
    requires_new_skill: boolean; suggested_skill_name: string; skill_goal: string; acceptance_criteria: string[];
    recommended_actions: string[]; model_call_id?: number; observed_error?: string };
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
  governance_plan?: SemanticPlan; result_report?: SemanticReport; analysis?: unknown; url?: string;
  state?: { status?: string; error?: string; attempts?: number } };
type Draft = { id: number; skill_code: string; skill_name: string; instructions: string; version?: string; enabled?: boolean;
  lifecycle_status: string; reused?: boolean;
  test_job_id?: number; test_progress?: { status: string; message: string; iteration?: number; sample_index?: number; sample_count?: number };
  test_result?: { passed: boolean; score?: number; summary?: string; event_count: number; relation_count: number; error?: string; note: string; iterations?: number; pending_version_test?: boolean; skill_version?: string;
    failure_kind?: string; document_count?: number; processed_document_count?: number };
  iteration_history?: { iteration: number; skill_version: string; extraction_error?: string; title?: string;
    evaluation?: { passed: boolean; score: number; summary: string; issues?: string[] } }[] };
type SkillTestJob = { job_id: number; status: string; error?: string; draft: Draft; result?: Draft };
type SemanticSkillStatus = {
  skill: { id: number; skill_code: string; skill_name: string; version: string; enabled: boolean; lifecycle_status: string };
  summary: { total: number; processed: number; passed: number; failed: number; model_unavailable: number; pending: number;
    latest_run_id?: number; latest_run_status?: string; current_progress?: string; current_phase?: string; current_message?: string };
  documents: { source_table: string; record_id: number; title?: string; status: string; error?: string; score?: number;
    event_count: number; relation_count: number }[];
  current_test: { passed: boolean; score?: number; failure_kind?: string; error?: string; summary?: string; pending_version_test?: boolean };
  history: { run_id: number; skill_version: string; status: string; started_at?: string; completed_at?: string; document_count: number;
    processed_document_count: number; passed: boolean; failure_kind?: string; summary?: string; error?: string }[];
  latest_model_errors: { id: number; provider?: string; instance?: string; status: string; error?: string; created_at?: string }[];
  can_enable: boolean; next_action: string;
};
type SemanticSkillRetryResponse = { job_id: number; status: string; draft?: Draft; result?: Draft; semantic_skill_status: SemanticSkillStatus };
const testing = (draft: Draft) => ["PENDING", "RUNNING", "RETRY"].includes(draft.test_progress?.status || "");
const skillStatusNames: Record<string, string> = {
  PASSED: "已通过", FAILED: "验收失败", MODEL_UNAVAILABLE: "模型不可用", PENDING: "待执行",
};

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
const workflowNames: Record<string, string> = { NEEDS_PLAN: "待制定方案", NEEDS_FAILURE_DIAGNOSIS: "执行失败，待LLM诊断", NEEDS_PLAN_CONFIRMATION: "待确认方案", READY_TO_EXECUTE: "待执行已确认方案", NEEDS_RESULT_CONFIRMATION: "待确认知识点", NEEDS_REANALYSIS: "等待定向重分析", PUBLISHED: "最终成果已发布", SKIPPED: "已选择不治理" };
const safeUrl = (value?: string) => /^https?:\/\//i.test(value || "") ? value : undefined;

export function InteractiveSemanticFlow({ detail, isLatest, reviewer, onReviewer, busy, setBusy, setError, onReload, onExecute, onJob }:
  { detail: Detail; isLatest: boolean; reviewer: string; busy: boolean; setBusy: (value: boolean) => void; setError: (value: string) => void;
    onReviewer: (value: string) => void; onReload: () => Promise<void>; onExecute: () => Promise<void>; onJob: (job: BatchGovernanceJob) => void }) {
  const [catalog, setCatalog] = useState<Catalog | null>(null);
  const [primary, setPrimary] = useState("GENERAL_EVIDENCE"), [secondary, setSecondary] = useState<string[]>([]);
  const [skills, setSkills] = useState<string[]>([]), [scopes, setScopes] = useState<string[]>(["COMPANY"]);
  const [reason, setReason] = useState(""), [rejected, setRejected] = useState<Record<string, string>>({});
  const [confirmed, setConfirmed] = useState<string[]>([]), [drafts, setDrafts] = useState<Record<number, Draft>>({});
  const [skillStatuses, setSkillStatuses] = useState<Record<number, SemanticSkillStatus>>({});
  const [recoveryStep, setRecoveryStep] = useState(""), [recoveryProgress, setRecoveryProgress] = useState(0);
  const [connectionWarning, setConnectionWarning] = useState("");
  const plan = detail.governance_plan;
  const report = detail.result_report ? { ...detail.result_report,
    knowledge_points: detail.result_report.knowledge_points.map(point => ({ ...point, source_url: safeUrl(point.source_url) })) } : undefined;
  const currentReport = !!report && report.plan_id === plan?.plan_id && report.plan_revision === plan?.revision;
  const canReview = isLatest && currentReport && plan?.status === "CONFIRMED" && detail.workflow_status === "NEEDS_RESULT_CONFIRMATION";
  const refreshCatalog = () => request<Catalog>("/document-governance/planning-catalog").then(setCatalog);
  useEffect(() => { void request<Catalog>("/document-governance/planning-catalog").then(setCatalog).catch(e => setError(e instanceof Error ? e.message : "治理目录读取失败")); }, []);
  useEffect(() => {
    setPrimary(plan?.primary_type || "GENERAL_EVIDENCE"); setSecondary(plan?.secondary_types || []);
    setSkills(plan?.recommended_skill_codes || []); setScopes(plan?.target_scopes || ["COMPANY"]); setReason(""); setRejected({}); setConfirmed([]); setDrafts({}); setSkillStatuses({});
    setRecoveryStep(""); setRecoveryProgress(0);
  }, [detail.source_table, detail.record_id, plan?.plan_id, plan?.revision, detail.selected_version]);
  useEffect(() => {
    let cancelled = false;
    if (plan && plan.status !== "SKIPPED") void request<Record<number, Draft>>(`/document-governance/documents/${detail.source_table}/${detail.record_id}/skill-drafts`)
      .then(values => { if (!cancelled) { setDrafts(values); setSkills(current => [...new Set([...current, ...Object.values(values).filter(item => item.lifecycle_status === "ENABLED").map(item => item.skill_code)])]); } })
      .catch(e => { if (!cancelled) setError(e instanceof Error ? e.message : "专项Skill草稿读取失败"); });
    return () => { cancelled = true; };
  }, [detail.source_table, detail.record_id, plan?.plan_id, plan?.revision]);
  const skillIds = useMemo(() => [...new Set(Object.values(drafts).map(item => item.id))], [drafts]);
  useEffect(() => {
    if (!skillIds.length) { setSkillStatuses({}); return; }
    let cancelled = false;
    const refresh = async () => {
      const responses = await Promise.allSettled(skillIds.map(async id => ({ id,
        status: await request<SemanticSkillStatus>(`/document-governance/semantic-skill-status/${id}`) })));
      if (cancelled) return;
      const next: Record<number, SemanticSkillStatus> = {};
      responses.forEach(item => { if (item.status === "fulfilled") next[item.value.id] = item.value.status; });
      setSkillStatuses(next);
    };
    void refresh();
    const timer = window.setInterval(() => void refresh(), 3000);
    return () => { cancelled = true; window.clearInterval(timer); };
  }, [skillIds.join(",")]);
  const testJobs = Object.entries(drafts).filter(([, draft]) => draft.test_job_id && testing(draft))
    .map(([index, draft]) => `${index}:${draft.test_job_id}`).join(",");
  useEffect(() => {
    if (!testJobs) { setConnectionWarning(""); return; }
    let cancelled = false, inFlight = false;
    const poll = async () => {
      if (inFlight) return;
      inFlight = true;
      const results = await Promise.allSettled(testJobs.split(",").map(async value => {
        const [index, jobId] = value.split(":");
        const job = await request<SkillTestJob>(`/document-governance/skill-test-jobs/${jobId}`);
        return { index: Number(index), job };
      }));
      inFlight = false;
      if (cancelled) return;
      setConnectionWarning(results.some(result => result.status === "rejected")
        ? "暂时无法读取进度，正在自动重连。后台任务仍会继续，请勿重复提交。" : "");
      results.forEach(result => {
        if (result.status !== "fulfilled") return;
        const { index, job } = result.value;
        const next = job.draft || job.result;
        if (next) setDrafts(values => ({ ...values, [index]: next }));
        if (job.status === "FAILED") setError(job.error || "专项Skill测试任务未完成，请查看原因后继续");
      });
    };
    void poll(); const timer = window.setInterval(() => void poll(), 2000);
    return () => { cancelled = true; window.clearInterval(timer); };
  }, [testJobs]);
  const skillsRunning = Object.values(drafts).some(testing);
  const gapsPending = (plan?.capability_gaps || []).some((_, index) => !drafts[index] || drafts[index].lifecycle_status !== "ENABLED");
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
  async function diagnoseFailure() {
    if (!reviewer.trim()) { setError("请先填写审核人"); return; }
    setBusy(true); setError("");
    try {
      setRecoveryProgress(8);
      setRecoveryStep("LLM正在阅读原文和失败证据，判断是数据、方案还是Skill问题…");
      const result = await request<{ plan: SemanticPlan }>(`/document-governance/documents/${detail.source_table}/${detail.record_id}/failure-diagnosis`, {
        method: "POST", body: JSON.stringify({ reviewer: reviewer.trim(), force: true }),
      });
      const generated: Record<number, Draft> = {};
      // Attach every gap/sample first; one shared Skill must not start testing
      // while another gap is still changing its acceptance scope.
      for (let index = 0; index < result.plan.capability_gaps.length; index += 1) {
        const base = 20 + Math.round(index / Math.max(result.plan.capability_gaps.length, 1) * 70);
        setRecoveryProgress(base);
        setRecoveryStep(`正在为第 ${index + 1}/${result.plan.capability_gaps.length} 个能力缺口生成专项Skill…`);
        const draft = await request<Draft>(`/document-governance/documents/${detail.source_table}/${detail.record_id}/skill-draft`, {
          method: "POST", body: JSON.stringify({ gap_index: index, expected_plan_id: result.plan.plan_id, reviewer: reviewer.trim() }),
        });
        setRecoveryProgress(base + 12);
        if (draft.lifecycle_status === "ENABLED") {
          generated[index] = draft;
          setSkills(values => [...new Set([...values, draft.skill_code])]);
          setRecoveryStep(`已复用启用中的“${draft.skill_name}”，无需重复创建。`);
        } else if (draft.test_result?.passed) {
          generated[index] = draft;
          setRecoveryStep(`已复用测试通过的“${draft.skill_name}”，等待人工审核启用。`);
        } else generated[index] = draft;
        setDrafts(values => ({ ...values, [index]: generated[index] }));
      }
      const jobs = new Map<number, Draft>();
      for (const [key, draft] of Object.entries(generated)) {
        if (draft.lifecycle_status === "ENABLED" || draft.test_result?.passed) continue;
        let updated = jobs.get(draft.id);
        if (!updated) {
          setRecoveryStep(`正在提交“${draft.skill_name}”的后台抽取、复核与规则优化…`);
          const job = await request<SkillTestJob>(`/document-governance/skill-drafts/${draft.id}/test-jobs`, {
            method: "POST", body: JSON.stringify({ reviewer: reviewer.trim() }),
          });
          updated = job.draft; jobs.set(draft.id, updated);
        }
        generated[Number(key)] = updated;
        setDrafts(values => ({ ...values, [Number(key)]: updated }));
      }
      const pending = Object.values(generated).filter(testing).length;
      const failed = Object.values(generated).filter(item => !testing(item) && item.test_result && !item.test_result.passed).length;
      setRecoveryProgress(100);
      setRecoveryStep(result.plan.capability_gaps.length
        ? pending ? `${pending}个专项Skill正在后台执行与复核，可关闭窗口后回来查看。` : failed ? `${failed}个专项Skill尚未达标，请查看真实原因并继续优化。` : "诊断和Skill验证已完成，请审核测试结果后决定是否启用。"
        : "诊断完成，请确认调整后的治理方案。");
    } catch (e) { setError(e instanceof Error ? e.message : "失败诊断或Skill验证未完成"); }
    finally { try { await onReload(); } catch { /* Preserve the original action error. */ } setBusy(false); }
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
    setBusy(true); setError(""); setRecoveryProgress(action === "test" ? 15 : 5);
    setRecoveryStep(action === "test" ? "正在用冻结原文继续测试，失败项将交给独立LLM生成下一版Skill规则…" :
      action === "enable" ? "正在执行人工启用门禁检查…" : "正在创建或复用专项Skill…");
    try {
      const path = action === "create" ? `/document-governance/documents/${detail.source_table}/${detail.record_id}/skill-draft` : `/document-governance/skill-drafts/${drafts[index].id}/${action === "test" ? "test-jobs" : action}`;
      const response = await request<Draft | SkillTestJob>(path, { method: "POST", body: JSON.stringify(action === "create" ?
        { gap_index: index, expected_plan_id: plan?.plan_id, reviewer: reviewer.trim() } : { reviewer: reviewer.trim() }) });
      const result = "job_id" in response ? response.draft : response;
      setDrafts(values => ({ ...values, [index]: result })); await refreshCatalog();
      if (action === "enable") setSkills(values => [...new Set([...values, result.skill_code])]);
      setRecoveryProgress(100);
      setRecoveryStep(action === "enable" ? "专项Skill已启用，请确认治理方案后重跑文档。" :
        testing(result) ? "测试已在后台启动，进度自动更新；关闭窗口不会中断。" : result.test_result?.passed ? "专项Skill已通过独立LLM复核，等待人工启用。" : "已保留草稿，可开始测试和自动优化。");
    } catch (e) { setError(e instanceof Error ? e.message : "专项Skill操作失败"); } finally { setBusy(false); }
  }
  async function retrySemanticSkill(skillId: number) {
    if (!reviewer.trim()) { setError("请先填写审核人"); return; }
    setBusy(true); setError(""); setRecoveryProgress(12);
    setRecoveryStep("正在重新提交全部关联文档，历史验收批次不会被覆盖…");
    try {
      const response = await request<SemanticSkillRetryResponse>(`/document-governance/semantic-skill-status/${skillId}/retry`, {
        method: "POST", body: JSON.stringify({ reviewer: reviewer.trim() }),
      });
      if (response.semantic_skill_status) setSkillStatuses(values => ({ ...values, [skillId]: response.semantic_skill_status }));
      const next = response.draft || response.result;
      if (next) setDrafts(values => Object.fromEntries(Object.entries(values).map(([index, draft]) => [index, draft.id === skillId ? next : draft])));
      setRecoveryProgress(100);
      setRecoveryStep("已创建新的验收批次。模型不可用时只会保留失败状态，不会伪造治理完成；模型恢复后会自动继续显示进度。");
    } catch (e) { setError(e instanceof Error ? e.message : "专项 Skill 重试提交失败"); }
    finally { setBusy(false); }
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
  function specialistStatusLabel(status: string) {
    return ({ PASSED: "已通过", FAILED: "验收失败", MODEL_UNAVAILABLE: "模型不可用", PENDING: "待处理" } as Record<string, string>)[status] || status;
  }
  function specialistOverview() {
    const entries = Object.entries(skillStatuses);
    if (!entries.length) return null;
    return <section className="ds-specialist-overview" aria-label="专项Skill验收总览">
      <header className="ds-specialist-summary">
        <div><span className="ds-section-kicker">专项Skill验收</span><h4>全部关联文档的回归状态</h4></div>
        <small>状态每3秒刷新；验收批次和失败记录均保留</small>
      </header>
      {entries.map(([key, status]) => {
        const summary = status.summary;
        const draft = Object.values(drafts).find(item => item.id === Number(key));
        const unavailable = summary.model_unavailable > 0 || status.current_test.failure_kind === "MODEL_UNAVAILABLE";
        return <article className="ds-specialist-card" key={key}>
          <div className="ds-specialist-card-header">
            <div><strong>{status.skill.skill_name}</strong><small>{status.skill.skill_code} · v{status.skill.version} · {status.skill.lifecycle_status === "TESTING" ? "测试中" : status.skill.lifecycle_status}</small></div>
            <span className={`ds-specialist-status status-${unavailable ? "model-unavailable" : summary.failed ? "failed" : summary.passed === summary.total && summary.total > 0 ? "passed" : "pending"}`}>
              {unavailable ? "模型不可用" : summary.failed ? "验收失败" : summary.passed === summary.total && summary.total > 0 ? "全部通过" : "待完成"}
            </span>
          </div>
          <div className="ds-specialist-counts"><span>关联 {summary.total} 篇</span><span>已处理 {summary.processed}/{summary.total}</span><span>通过 {summary.passed}</span><span>失败 {summary.failed}</span><span>模型不可用 {summary.model_unavailable}</span><span>待处理 {summary.pending}</span></div>
          {unavailable && <div className="ds-specialist-alert" role="alert"><strong>专项Skill已创建，但本轮真实模型验收未完成</strong><p>{status.latest_model_errors[0]?.error || status.current_test.error || "模型服务返回HTTP 402，账户余额或额度不可用。"}</p><small>这不是“未创建Skill”。恢复模型服务或额度后，点击“重新测试全部文档”；系统不会用Mock结果冒充真实验收。</small></div>}
          {summary.current_message && <p className="ds-specialist-progress">{summary.current_message}</p>}
          <details className="ds-specialist-documents"><summary>查看 {summary.total} 篇文档状态</summary><div>{status.documents.map(document => <div className="ds-specialist-document" key={`${document.source_table}:${document.record_id}`}><span>{document.title || `${document.source_table}:${document.record_id}`}</span><b className={`status-${document.status.toLowerCase()}`}>{specialistStatusLabel(document.status)}</b>{document.error && <small>{document.error}</small>}</div>)}</div></details>
          <details className="ds-specialist-history"><summary>查看历史验收批次（{status.history.length}）</summary><ol>{status.history.map(run => <li key={run.run_id}><span>批次 #{run.run_id} · v{run.skill_version} · {run.document_count}篇</span><b>{run.passed ? "通过" : run.failure_kind === "MODEL_UNAVAILABLE" ? "模型不可用" : run.status}</b>{run.error && <small>{run.error}</small>}</li>)}</ol></details>
          <div className="ds-specialist-actions"><span>{status.next_action}</span><button disabled={busy || !reviewer.trim() || Boolean(draft && testing(draft))} onClick={() => void retrySemanticSkill(Number(key))}>重新测试全部文档</button></div>
        </article>;
      })}
    </section>;
  }
  const missingRequired = report?.knowledge_points.filter(point => point.confirmation_level === "REQUIRED" && point.review_status !== "HUMAN_CONFIRMED" &&
    !confirmed.includes(`${point.point_type}:${point.key}`) && !Object.prototype.hasOwnProperty.call(rejected, `${point.point_type}:${point.key}`)).length || 0;
  return <section className="ds-interactive-flow" aria-label="交互式治理流程">
    <header><div><h3>交互式知识治理</h3><p>点击“确认本批结果”后，普通项未标记不符合即视为符合；重点项须单独确认。</p></div><span className="ds-flow-state">{workflowNames[detail.workflow_status || "NEEDS_PLAN"] || "待查看"}</span></header>
    <label className="ds-reviewer">审核人<input value={reviewer} onChange={e => onReviewer(e.target.value)} placeholder="填写审核人姓名" /></label>
    <ol className="ds-flow-steps">{steps.map(([key, label, complete], index) => <li key={key} className={complete ? "complete" : ""}><b>{complete ? "✓" : index + 1}</b><span>{label}</span></li>)}</ol>
    {connectionWarning && <p className="ds-confirm-warning" role="status">{connectionWarning}</p>}
    {recoveryStep && detail.workflow_status !== "NEEDS_FAILURE_DIAGNOSIS" && <div className="ds-loop-progress" role="status"><p>{recoveryStep}</p>{busy && <progress aria-label="专项Skill操作进度" />}</div>}
    {detail.workflow_status === "NEEDS_FAILURE_DIAGNOSIS" && <div className="ds-failure-recovery" role="alert">
      <div><span>本轮没有生成可确认的知识点</span><h4>先诊断失败，再决定是否创建或优化Skill</h4>
        <p>{detail.state?.error || "最近一次结构化治理未通过质量门禁。"}</p><small>系统不会继续用相同Skill盲目重试，也不会把失败结果发布为事实。</small></div>
      <div className="ds-recovery-action">{recoveryStep && <><p role="status">{recoveryStep}</p><progress max={100} value={recoveryProgress} aria-label="修复闭环进度" /></>}<button className="ds-primary" disabled={busy} onClick={() => void diagnoseFailure()}>{busy ? "修复闭环执行中…" : "LLM诊断并自动构建／验证Skill"}</button></div>
    </div>}
    {specialistOverview()}
    {!plan && detail.workflow_status !== "NEEDS_FAILURE_DIAGNOSIS" && <div className="ds-flow-action"><h4>第一步：LLM阅读全文并制定治理方案</h4><p>模型只提出文档类型、知识归属和Skill组合，本阶段不会生成或发布正式知识。</p><button className="ds-primary" disabled={busy} onClick={() => void generate()}>{busy ? "正在阅读全文…" : "生成治理方案"}</button></div>}
    {plan?.status === "PROPOSED" && catalog && <div className="ds-plan-editor">
      <h4>第二步：确认治理方案</h4><p className="ds-plan-overview">{plan.overview}</p>
      <div className="ds-plan-summary"><div><strong>{typeNames[primary] || primary}</strong><small>知识归属：{scopes.map(scope => scopeNames[scope] || scope).join("、")}</small></div><p>{skillsRunning ? "专项Skill正在后台测试，请等待抽取和独立复核完成。" : gapsPending ? "当前先完成下方专项Skill验证与启用，再确认方案。" : "方案已具备执行条件，请核对类型与范围后确认。"}</p></div>
      {plan.failure_diagnosis && <section className="ds-diagnosis"><header><strong>LLM失败诊断</strong><span>模型调用 #{plan.failure_diagnosis.model_call_id || "—"}</span></header>
        <p>{plan.failure_diagnosis.summary}</p><p><b>原执行错误：</b>{plan.failure_diagnosis.observed_error}</p>
        {!!plan.failure_diagnosis.root_causes.length && <details><summary>查看诊断依据与失败原因</summary><ul>{plan.failure_diagnosis.root_causes.map((item, i) => <li key={i}>{item}</li>)}</ul></details>}
        <strong>{plan.failure_diagnosis.requires_new_skill ? "结论：现有Skill不足，需要专项Skill并通过回归验证" : "结论：优先调整方案或重试，不强制创建Skill"}</strong>
      </section>}
      <details className="ds-plan-adjust"><summary>查看或调整类型、归属对象和现有Skill</summary><p><strong>模型判断依据：</strong>{plan.rationale}</p>
      <div className="ds-plan-grid"><label>主要类型<select aria-label="主要类型" value={primary} onChange={e => setPrimary(e.target.value)}>{catalog.types.map(item => <option value={item.code} key={item.code}>{item.name}</option>)}</select></label>
        <fieldset><legend>次要类型（可多选）</legend>{catalog.types.filter(item => item.code !== primary).map(item => <label key={item.code}><input type="checkbox" checked={secondary.includes(item.code)} onChange={() => toggle(secondary, item.code, setSecondary)} />{item.name}</label>)}</fieldset>
        <fieldset><legend>知识归属对象</legend>{Object.entries(scopeNames).map(([code, label]) => <label key={code}><input type="checkbox" checked={scopes.includes(code)} onChange={() => toggle(scopes, code, setScopes)} />{label}</label>)}</fieldset>
        <fieldset><legend>执行Skill（可修改）</legend>{catalog.skills.filter(item => item.skill_code !== "DOCUMENT_SEMANTIC_ROUTER").map(item => <label className={!item.enabled ? "disabled" : ""} key={item.skill_code}><input type="checkbox" disabled={!item.enabled || item.lifecycle_status !== "ENABLED"} checked={skills.includes(item.skill_code)} onChange={() => toggle(skills, item.skill_code, setSkills)} />{item.skill_name}<small>v{item.version || "未安装"}</small></label>)}</fieldset></div></details>
      {!!plan.expected_knowledge.length && <p><strong>预计提取：</strong>{plan.expected_knowledge.join("；")}</p>}
      {!!plan.uncertainties.length && <div className="ds-confirm-warning"><strong>模型不确定项</strong><ul>{plan.uncertainties.map((item, i) => <li key={i}>{item}</li>)}</ul></div>}
      {!!plan.user_confirmation_questions.length && <div className="ds-confirm-warning"><strong>请核对这些问题</strong><ul>{plan.user_confirmation_questions.map((item, i) => <li key={i}>{item}</li>)}</ul></div>}
      {!!plan.capability_gaps.length && <div className="ds-skill-loop"><header><div><span>专项技能持续优化</span><h4>能力缺口与Skill闭环</h4></div><small>创建 → 执行 → LLM复核 → 每次最多3轮优化，可继续迭代 → 人工启用</small></header>{plan.capability_gaps.map((item, i) => <article key={i}><p><b>{item.type_name}</b>：{item.reason}</p>
        {!drafts[i] ? <button disabled={busy} onClick={() => void draftAction(i, "create")}>创建专项Skill草稿</button> : <div><p>{drafts[i].skill_name} · {drafts[i].lifecycle_status === "ENABLED" ? "已启用" : testing(drafts[i]) ? "后台验证中" : drafts[i].test_result?.passed ? "测试已通过，等待启用" : drafts[i].test_result ? "验收未通过，待继续处理" : "草稿待测试"}</p><details><summary>查看草稿治理规则</summary><pre className="ds-skill-instructions">{drafts[i].instructions}</pre></details>
        {drafts[i].test_progress && <div className="ds-loop-progress" role="status"><p>{drafts[i].test_progress.message}</p>{testing(drafts[i]) && <><progress aria-label="专项Skill后台治理进度" /><small>进度按真实执行阶段更新，后台任务持续保存；关闭后可重新打开查看。</small></>}</div>}
        {drafts[i].test_result && !testing(drafts[i]) && <div className={`ds-skill-score ${drafts[i].test_result?.passed ? "passed" : "failed"}`} role="status"><b>{drafts[i].test_result?.passed ? "LLM复核通过" : drafts[i].test_result.failure_kind === "MODEL_UNAVAILABLE" ? "模型服务不可用，验收未完成" : "LLM复核未通过"}</b>{drafts[i].test_result.failure_kind !== "MODEL_UNAVAILABLE" && <span>{drafts[i].test_result.score ?? 0}分 · {drafts[i].test_result.iterations || 1}轮</span>}<p>{drafts[i].test_result.summary || drafts[i].test_result.note} {drafts[i].test_result.error}</p>{drafts[i].test_result.failure_kind === "MODEL_UNAVAILABLE" && <small>请在模型管理中恢复服务或选择可用模型，然后继续测试。原文、Skill版本与治理历程已保留。</small>}{drafts[i].test_result.document_count !== undefined && <small>验收范围：{drafts[i].test_result.document_count}篇文档{drafts[i].test_result.processed_document_count !== undefined ? `，本轮尝试${drafts[i].test_result.processed_document_count}篇` : "，须全部通过"}。</small>}{drafts[i].test_result.pending_version_test && <small>已保存改进规则 v{drafts[i].version}，需要测试新版后才能启用。</small>}</div>}
        {!!drafts[i].iteration_history?.length && <details><summary>查看每轮Skill优化与验证</summary><ol>{drafts[i].iteration_history.map((step, historyIndex) => <li key={`${historyIndex}:${step.iteration}:${step.skill_version}`}><b>第{step.iteration}轮 · v{step.skill_version} · {step.evaluation?.summary === "独立LLM质量复核失败" ? "复核未完成" : `${step.evaluation?.score ?? 0}分`}</b>{step.title && <small>{step.title}</small>}<p>{step.extraction_error || step.evaluation?.summary}</p></li>)}</ol></details>}
        <button disabled={busy || skillsRunning || drafts[i].lifecycle_status === "ENABLED"} onClick={() => void draftAction(i, "test")}>{testing(drafts[i]) ? "后台执行与复核中…" : drafts[i].test_result && !drafts[i].test_result.passed ? "继续自动优化并复核" : "执行测试并自动优化（每轮最多3次）"}</button><button className="ds-primary" disabled={busy || skillsRunning || !drafts[i].test_result?.passed || drafts[i].lifecycle_status === "ENABLED"} onClick={() => void draftAction(i, "enable")}>人工审核通过并启用</button></div>}
      </article>)}</div>}
      <label>确认说明（可选）<textarea rows={2} value={reason} onChange={e => setReason(e.target.value)} placeholder="可说明为何调整类型、Skill或归属对象" /></label>
      {gapsPending && <p className="ds-note">先完成专项Skill测试与人工启用，再确认方案执行；尚未通过的规则不能直接用于正式治理。</p>}
      <div className="ds-review-actions"><button disabled={busy || skillsRunning} onClick={() => void generate(true)}>重新阅读并生成</button><button disabled={busy || skillsRunning} onClick={() => void decide("SKIP")}>本篇不治理</button><button className="ds-primary" disabled={busy || skillsRunning || gapsPending || !skills.length || !scopes.length} onClick={() => void decide("CONFIRM")}>确认方案并进入执行</button></div>
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
