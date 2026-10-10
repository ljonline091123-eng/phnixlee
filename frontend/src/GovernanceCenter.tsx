import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Activity,
  AlertTriangle,
  Bot,
  CheckCircle2,
  FileCheck2,
  RefreshCw,
  ShieldCheck,
  Wrench,
  XCircle,
} from "lucide-react";
import {
  api,
  type AgentDefinition,
  type AgentExecutionRun,
  type DataQualityIssue,
  type DataQualityRule,
  type DataQualityRun,
  type ModelSkill,
  type SkillExecutionRun,
} from "./api";
import "./GovernanceCenter.css";
import "./GovernanceCenterExtra.css";

export type GovernanceCenterView = "quality" | "executions" | "contracts";

type LoadState = {
  loading: boolean;
  failures: string[];
  loadedAt?: Date;
};

const lifecycleNames: Record<string, string> = {
  DRAFT: "草稿",
  TESTING: "测试中",
  ENABLED: "已启用",
  DISABLED: "已停用",
  DEPRECATED: "已废弃",
};

const statusNames: Record<string, string> = {
  SUCCESS: "成功",
  SUCCEEDED: "成功",
  COMPLETED: "已完成",
  PASS: "通过",
  PASSED: "通过",
  RUNNING: "执行中",
  PENDING: "待处理",
  OPEN: "待处理",
  PARTIAL: "部分完成",
  WARNING: "警告",
  FAILED: "失败",
  ERROR: "错误",
  MISSING: "缺失",
  RESOLVED: "已解决",
};

function rows<T>(value: T[] | { items: T[] }): T[] {
  return Array.isArray(value) ? value : value.items || [];
}

function errorText(reason: unknown) {
  return reason instanceof Error ? reason.message : "接口读取失败";
}

function dateText(value?: string | null) {
  if (!value) return "--";
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? value : parsed.toLocaleString("zh-CN", { hour12: false });
}

function statusTone(status?: string | null) {
  const value = String(status || "PENDING").toUpperCase();
  if (["SUCCESS", "SUCCEEDED", "COMPLETED", "PASS", "PASSED", "RESOLVED", "ENABLED"].includes(value)) return "success";
  if (["FAILED", "ERROR", "MISSING", "REJECTED"].includes(value)) return "failed";
  if (["RUNNING", "PROCESSING", "TESTING"].includes(value)) return "running";
  if (["PARTIAL", "WARNING", "OPEN"].includes(value)) return "warning";
  return "pending";
}

function statusText(status?: string | null) {
  const value = String(status || "PENDING").toUpperCase();
  return statusNames[value] || lifecycleNames[value] || value;
}

function hashText(value?: string | null) {
  if (!value) return "--";
  return value.length > 14 ? `${value.slice(0, 8)}…${value.slice(-4)}` : value;
}

function asObject(value: unknown): Record<string, unknown> {
  return value && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : {};
}

function compactJson(value: unknown) {
  const object = asObject(value);
  const keys = Object.keys(object);
  if (!keys.length) return "未配置";
  return keys.slice(0, 4).map((key) => `${key}: ${String(object[key])}`).join("；");
}

function Summary({ label, value, detail, tone = "neutral" }: { label: string; value: number; detail: string; tone?: string }) {
  return <article className={`gov-summary ${tone}`}><span>{label}</span><strong>{value.toLocaleString("zh-CN")}</strong><p>{detail}</p></article>;
}

function Header({ title, detail, state, onRefresh }: { title: string; detail: string; state: LoadState; onRefresh: () => void }) {
  return (
    <header className="gov-header">
      <div><p>GOVERNANCE CONTROL · 治理控制台</p><h1>{title}</h1><span>{detail}</span></div>
      <button type="button" onClick={onRefresh} disabled={state.loading}><RefreshCw size={14} className={state.loading ? "spin" : ""} />{state.loading ? "加载中" : "刷新"}</button>
    </header>
  );
}

function FailureNotice({ failures }: { failures: string[] }) {
  if (!failures.length) return null;
  return <div className="gov-warning"><AlertTriangle size={16} /><span>{failures.join("；")}。当前仅展示成功读取的数据，未读取到的项目不计为已完成。</span></div>;
}

function QualityView() {
  const [rules, setRules] = useState<DataQualityRule[]>([]);
  const [runs, setRuns] = useState<DataQualityRun[]>([]);
  const [issues, setIssues] = useState<DataQualityIssue[]>([]);
  const [runTotal, setRunTotal] = useState(0);
  const [issueTotal, setIssueTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [tab, setTab] = useState<"runs" | "issues" | "rules">("runs");
  const [selectedRun, setSelectedRun] = useState<(DataQualityRun & { issues?: DataQualityIssue[] }) | null>(null);
  const [selectedIssue, setSelectedIssue] = useState<DataQualityIssue | null>(null);
  const [notice, setNotice] = useState("");
  const [state, setState] = useState<LoadState>({ loading: true, failures: [] });
  const pageSize = 20;

  const load = useCallback(async () => {
    setState((current) => ({ ...current, loading: true, failures: [] }));
    const result = await Promise.allSettled([
      api.listDataQualityRules(),
      api.listDataQualityRuns({ limit: pageSize, offset: page * pageSize }),
      api.listDataQualityIssues({ limit: pageSize, offset: page * pageSize }),
    ] as const);
    const failures: string[] = [];
    if (result[0].status === "fulfilled") setRules(rows(result[0].value)); else failures.push(`质量规则接口：${errorText(result[0].reason)}`);
    if (result[1].status === "fulfilled") { setRuns(result[1].value.items || []); setRunTotal(result[1].value.total || 0); } else failures.push(`质量运行接口：${errorText(result[1].reason)}`);
    if (result[2].status === "fulfilled") { setIssues(result[2].value.items || []); setIssueTotal(result[2].value.total || 0); } else failures.push(`质量问题接口：${errorText(result[2].reason)}`);
    setState({ loading: false, failures, loadedAt: new Date() });
  }, [page]);

  useEffect(() => { void load(); }, [load]);
  const openIssues = issues.filter((item) => !["RESOLVED", "CLOSED", "IGNORED"].includes(String(item.status).toUpperCase()));
  const failedRuns = runs.filter((item) => statusTone(item.status) === "failed" || item.passed === false);
  const totalPages = Math.max(1, Math.ceil((tab === "issues" ? issueTotal : runTotal) / pageSize));
  async function openRun(item: DataQualityRun) { try { setSelectedRun(await api.getDataQualityRun(String(item.id))); } catch (error) { setNotice(`质量运行详情读取失败：${errorText(error)}`); } }
  async function openIssue(item: DataQualityIssue) { try { setSelectedIssue(await api.getDataQualityIssue(String(item.id))); } catch (error) { setNotice(`质量问题详情读取失败：${errorText(error)}`); } }
  async function updateIssue(status: "ACKNOWLEDGED" | "RESOLVED" | "IGNORED") {
    if (!selectedIssue) return;
    try { await api.resolveDataQualityIssue(String(selectedIssue.id), { status, resolution: status === "RESOLVED" ? "已在治理控制台确认修复" : "由治理控制台处理" }); setSelectedIssue(null); setNotice(`质量问题已标记为${statusText(status)}`); await load(); } catch (error) { setNotice(`质量问题处理失败：${errorText(error)}`); }
  }

  return <section className="gov-root">
    <Header title="数据质量中心" detail="统一管理质量规则、运行结果、问题清单和质量门禁。" state={state} onRefresh={() => void load()} />
    <FailureNotice failures={state.failures} />
    <div className="gov-summaries">
      <Summary label="启用规则" value={rules.filter((item) => item.enabled).length} detail={`共登记 ${rules.length} 条规则`} tone="success" />
      <Summary label="质量运行" value={runs.length} detail="保留检查范围与结果摘要" />
      <Summary label="未解决问题" value={openIssues.length} detail="缺失、异常或待复核项目" tone={openIssues.length ? "warning" : "success"} />
      <Summary label="门禁未通过" value={failedRuns.length} detail="不得发布为完整或已治理" tone={failedRuns.length ? "failed" : "success"} />
    </div>
    {notice && <div className="gov-notice">{notice}</div>}
    <section className="gov-panel"><div className="gov-panel-title"><div><h2>质量治理工作台</h2><p>规则、运行和问题统一分页查看；点击行打开详情，问题可确认、解决或忽略。</p></div><ShieldCheck size={18} /></div><div className="gov-tabs"><button type="button" className={tab === "runs" ? "active" : ""} onClick={() => { setTab("runs"); setPage(0); }}>质量运行（{runTotal}）</button><button type="button" className={tab === "issues" ? "active" : ""} onClick={() => { setTab("issues"); setPage(0); }}>质量问题（{issueTotal}）</button><button type="button" className={tab === "rules" ? "active" : ""} onClick={() => setTab("rules")}>质量规则（{rules.length}）</button></div>
      {tab === "rules" && <div className="gov-table-wrap"><table><thead><tr><th>规则</th><th>检查对象</th><th>维度</th><th>级别</th><th>状态</th></tr></thead><tbody>{rules.length ? rules.map((item) => <tr key={item.id}><td><strong>{item.rule_name}</strong><small>{item.rule_code} · v{item.version || "1"}</small></td><td>{item.target_name || item.target_code || item.target_type || item.asset_scope || "全局"}</td><td>{item.dimension || item.rule_type || "通用"}</td><td>{item.severity || "WARNING"}</td><td><i className={`gov-status ${item.enabled ? "success" : "pending"}`}>{item.enabled ? "启用" : lifecycleNames[item.lifecycle_status || ""] || "停用"}</i></td></tr>) : <tr><td colSpan={5} className="gov-empty">暂无已登记质量规则</td></tr>}</tbody></table></div>}
      {tab === "runs" && <div className="gov-table-wrap"><table><thead><tr><th>运行</th><th>对象</th><th>检查量</th><th>问题</th><th>结果</th><th>操作</th></tr></thead><tbody>{runs.length ? runs.map((item) => <tr key={item.id}><td><strong>{item.run_code || item.run_key || `运行 #${item.id}`}</strong><small>{dateText(item.completed_at || item.started_at)}</small></td><td>{item.target_name || item.target_type || "--"} / {item.target_id || "--"}</td><td>{item.checked_count ?? item.total_rules ?? "--"}</td><td>{item.issue_count ?? ((item.failed_rules || 0) + (item.warning_rules || 0))}</td><td><i className={`gov-status ${statusTone(item.passed === false ? "FAILED" : item.status)}`}>{item.passed === false ? "未通过" : statusText(item.status)}</i></td><td><button type="button" className="gov-row-action" onClick={() => void openRun(item)}>查看详情</button></td></tr>) : <tr><td colSpan={6} className="gov-empty">暂无质量运行记录</td></tr>}</tbody></table></div>}
      {tab === "issues" && <div className="gov-table-wrap"><table><thead><tr><th>问题</th><th>对象</th><th>字段</th><th>级别</th><th>状态</th><th>发现时间</th><th>操作</th></tr></thead><tbody>{issues.length ? issues.map((item) => <tr key={item.id}><td><strong>{item.message}</strong><small>{item.issue_code || `问题 #${item.id}`} · 运行 #{item.quality_run_id || item.run_id || "--"}</small></td><td>{item.target_type || "--"} / {item.target_id || "--"}</td><td>{item.field_name || item.rule_code || "--"}</td><td>{item.severity || "WARNING"}</td><td><i className={`gov-status ${statusTone(item.status)}`}>{statusText(item.status)}</i></td><td>{dateText(item.first_detected_at || item.detected_at)}</td><td><button type="button" className="gov-row-action" onClick={() => void openIssue(item)}>查看/处理</button></td></tr>) : <tr><td colSpan={7} className="gov-empty">暂无质量问题记录</td></tr>}</tbody></table></div>}
      {tab !== "rules" && <footer className="gov-pagination"><button type="button" disabled={page === 0 || state.loading} onClick={() => setPage((value) => Math.max(0, value - 1))}>上一页</button><span>第 {page + 1} / {totalPages} 页</span><button type="button" disabled={page + 1 >= totalPages || state.loading} onClick={() => setPage((value) => value + 1)}>下一页</button></footer>}
    </section>
    {selectedRun && <div className="gov-modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setSelectedRun(null); }}><section className="gov-modal" role="dialog"><header><h2>质量运行详情</h2><button type="button" onClick={() => setSelectedRun(null)}>关闭</button></header><div className="gov-modal-body"><dl><div><dt>运行标识</dt><dd>{selectedRun.run_key || selectedRun.id}</dd></div><div><dt>检查对象</dt><dd>{selectedRun.target_type} / {selectedRun.target_id}</dd></div><div><dt>质量得分</dt><dd>{selectedRun.score ?? "--"}</dd></div><div><dt>门禁状态</dt><dd>{statusText(selectedRun.status)}</dd></div></dl><h3>质量摘要</h3><pre>{JSON.stringify(selectedRun.summary || selectedRun.summary_json || {}, null, 2)}</pre><h3>关联问题（{selectedRun.issues?.length || 0}）</h3>{selectedRun.issues?.map((issue) => <p key={issue.id}>{issue.message} · {statusText(issue.status)}</p>)}</div></section></div>}
    {selectedIssue && <div className="gov-modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setSelectedIssue(null); }}><section className="gov-modal" role="dialog"><header><h2>质量问题详情</h2><button type="button" onClick={() => setSelectedIssue(null)}>关闭</button></header><div className="gov-modal-body"><dl><div><dt>问题说明</dt><dd>{selectedIssue.message}</dd></div><div><dt>定位对象</dt><dd>{selectedIssue.target_type} / {selectedIssue.target_id}</dd></div><div><dt>规则与级别</dt><dd>{selectedIssue.rule_code || "--"} / {selectedIssue.severity || "--"}</dd></div><div><dt>证据</dt><dd>{JSON.stringify(selectedIssue.evidence_ids || [])}</dd></div></dl><pre>{JSON.stringify({ observed_value: selectedIssue.observed_value, expected_value: selectedIssue.expected_value, details: selectedIssue.details }, null, 2)}</pre><div className="gov-modal-actions"><button type="button" onClick={() => void updateIssue("ACKNOWLEDGED")}>确认问题</button><button type="button" onClick={() => void updateIssue("RESOLVED")}>标记已解决</button><button type="button" onClick={() => void updateIssue("IGNORED")}>忽略</button></div></div></section></div>}
    <footer className="gov-footer">最近刷新：{state.loadedAt?.toLocaleString("zh-CN") || "--"}</footer>
  </section>;
}

function ExecutionsView() {
  const [agentRuns, setAgentRuns] = useState<AgentExecutionRun[]>([]);
  const [skillRuns, setSkillRuns] = useState<SkillExecutionRun[]>([]);
  const [agentTotal, setAgentTotal] = useState(0);
  const [skillTotal, setSkillTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [tab, setTab] = useState<"agent" | "skill">("agent");
  const [statusFilter, setStatusFilter] = useState("");
  const [selected, setSelected] = useState<AgentExecutionRun | SkillExecutionRun | null>(null);
  const [state, setState] = useState<LoadState>({ loading: true, failures: [] });
  const pageSize = 20;
  const load = useCallback(async () => {
    setState((current) => ({ ...current, loading: true, failures: [] }));
    const result = await Promise.allSettled([api.listAgentExecutions({ limit: pageSize, offset: page * pageSize, status: statusFilter }), api.listSkillExecutions({ limit: pageSize, offset: page * pageSize, status: statusFilter })] as const);
    const failures: string[] = [];
    if (result[0].status === "fulfilled") { setAgentRuns(result[0].value.items || []); setAgentTotal(result[0].value.total || 0); } else failures.push(`Agent 执行接口：${errorText(result[0].reason)}`);
    if (result[1].status === "fulfilled") { setSkillRuns(result[1].value.items || []); setSkillTotal(result[1].value.total || 0); } else failures.push(`Skill 执行接口：${errorText(result[1].reason)}`);
    setState({ loading: false, failures, loadedAt: new Date() });
  }, [page, statusFilter]);
  useEffect(() => { void load(); }, [load]);
  const failed = [...agentRuns, ...skillRuns].filter((item) => statusTone(item.status) === "failed").length;
  const running = [...agentRuns, ...skillRuns].filter((item) => statusTone(item.status) === "running").length;
  const totalPages = Math.max(1, Math.ceil((tab === "agent" ? agentTotal : skillTotal) / pageSize));
  async function openExecution(id: string, kind: "agent" | "skill") {
    try { setSelected(kind === "agent" ? await api.getAgentExecution(id) : await api.getSkillExecution(id)); } catch (error) { /* detail is surfaced in the page refresh banner */ }
  }
  return <section className="gov-root">
    <Header title="Agent / Skill 执行审计" detail="统一查看执行状态、版本、输入输出哈希、证据和错误记录。" state={state} onRefresh={() => void load()} />
    <FailureNotice failures={state.failures} />
    <div className="gov-summaries"><Summary label="Agent 运行" value={agentRuns.length} detail="编排与模型调用批次" /><Summary label="Skill 运行" value={skillRuns.length} detail="受控读写和治理动作" /><Summary label="执行中" value={running} detail="当前尚未结束的运行" tone={running ? "running" : "success"} /><Summary label="失败" value={failed} detail="保留错误与重试信息" tone={failed ? "failed" : "success"} /></div>
    <section className="gov-panel"><div className="gov-panel-title"><div><h2>执行记录</h2><p>点击查看输入输出、模型/版本、Skill 链路、证据和错误。</p></div><Bot size={18} /></div><div className="gov-tabs"><button type="button" className={tab === "agent" ? "active" : ""} onClick={() => { setTab("agent"); setPage(0); }}>Agent 执行（{agentTotal}）</button><button type="button" className={tab === "skill" ? "active" : ""} onClick={() => { setTab("skill"); setPage(0); }}>Skill 执行（{skillTotal}）</button><select aria-label="按执行状态筛选" value={statusFilter} onChange={(event) => { setStatusFilter(event.target.value); setPage(0); }}><option value="">全部状态</option><option value="SUCCESS">成功</option><option value="RUNNING">执行中</option><option value="FAILED">失败</option><option value="PENDING">待处理</option></select></div>{tab === "agent" ? <div className="gov-table-wrap"><table><thead><tr><th>运行</th><th>Agent / 版本</th><th>模型</th><th>输入 / 输出哈希</th><th>状态</th><th>时间</th><th>操作</th></tr></thead><tbody>{agentRuns.length ? agentRuns.map((item) => <tr key={item.id}><td><strong>{item.run_id || item.run_key || `#${item.id}`}</strong><small>{item.error_message || "证据链已记录"}</small></td><td>{item.agent_code || `Agent #${item.agent_id || "--"}`} / {item.agent_version || "--"}</td><td>{item.model_instance_code || "--"}<small>{item.model_version || ""}</small></td><td><code>{hashText(item.input_hash)}</code><small>输出 {hashText(item.output_hash)}</small></td><td><i className={`gov-status ${statusTone(item.status)}`}>{statusText(item.status)}</i></td><td>{dateText(item.completed_at || item.started_at)}</td><td><button type="button" className="gov-row-action" onClick={() => void openExecution(item.id, "agent")}>查看</button></td></tr>) : <tr><td colSpan={7} className="gov-empty">暂无 Agent 执行记录</td></tr>}</tbody></table></div> : <div className="gov-table-wrap"><table><thead><tr><th>运行</th><th>Skill / 版本</th><th>幂等键</th><th>输入 / 输出哈希</th><th>重试</th><th>状态</th><th>时间</th><th>操作</th></tr></thead><tbody>{skillRuns.length ? skillRuns.map((item) => <tr key={item.id}><td><strong>{item.run_id || `#${item.id}`}</strong><small>{item.error_message || `Agent 执行 #${item.agent_execution_run_id || item.agent_execution_id || "--"}`}</small></td><td>{item.skill_code || `Skill #${item.skill_id || "--"}`} / {item.skill_version || "--"}<small>{item.side_effect_level || "READ_ONLY"}</small></td><td><code>{hashText(item.idempotency_key)}</code></td><td><code>{hashText(item.input_hash)}</code><small>输出 {hashText(item.output_hash)}</small></td><td>{item.attempt != null ? `${item.attempt}/${item.max_attempts || item.attempt}` : item.retry_count ?? 0}</td><td><i className={`gov-status ${statusTone(item.status)}`}>{statusText(item.status)}</i></td><td>{dateText(item.completed_at || item.started_at)}</td><td><button type="button" className="gov-row-action" onClick={() => void openExecution(item.id, "skill")}>查看</button></td></tr>) : <tr><td colSpan={8} className="gov-empty">暂无 Skill 执行记录</td></tr>}</tbody></table></div>}<footer className="gov-pagination"><button type="button" disabled={page === 0 || state.loading} onClick={() => setPage((value) => Math.max(0, value - 1))}>上一页</button><span>第 {page + 1} / {totalPages} 页</span><button type="button" disabled={page + 1 >= totalPages || state.loading} onClick={() => setPage((value) => value + 1)}>下一页</button></footer></section>
    {selected && <div className="gov-modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setSelected(null); }}><section className="gov-modal" role="dialog"><header><h2>执行详情</h2><button type="button" onClick={() => setSelected(null)}>关闭</button></header><div className="gov-modal-body"><dl><div><dt>运行标识</dt><dd>{selected.run_id || selected.id}</dd></div><div><dt>版本</dt><dd>{(selected as AgentExecutionRun).agent_code || (selected as SkillExecutionRun).skill_code || "--"} · v{(selected as AgentExecutionRun).agent_version || (selected as SkillExecutionRun).skill_version || "--"}</dd></div><div><dt>状态</dt><dd>{statusText(selected.status)}</dd></div><div><dt>输入/输出哈希</dt><dd>{selected.input_hash || "--"} / {selected.output_hash || "--"}</dd></div><div><dt>证据</dt><dd>{JSON.stringify(selected.evidence_ids_json || [])}</dd></div><div><dt>错误</dt><dd>{selected.error_code || "--"} {selected.error_message || ""}</dd></div></dl><h3>输入</h3><pre>{JSON.stringify(selected.input || {}, null, 2)}</pre><h3>输出</h3><pre>{JSON.stringify(selected.output || {}, null, 2)}</pre>{"skill_executions" in selected && <><h3>关联 Skill</h3><pre>{JSON.stringify((selected as AgentExecutionRun & { skill_executions?: SkillExecutionRun[] }).skill_executions || [], null, 2)}</pre></>}</div></section></div>}
    <footer className="gov-footer">最近刷新：{state.loadedAt?.toLocaleString("zh-CN") || "--"}</footer>
  </section>;
}

function ContractsView() {
  const [agents, setAgents] = useState<AgentDefinition[]>([]);
  const [skills, setSkills] = useState<ModelSkill[]>([]);
  const [state, setState] = useState<LoadState>({ loading: true, failures: [] });
  const [changingId, setChangingId] = useState<number | null>(null);
  const [notice, setNotice] = useState("");
  const load = useCallback(async () => {
    setState((current) => ({ ...current, loading: true, failures: [] }));
    const result = await Promise.allSettled([api.listAgents(), api.listModelSkills()] as const);
    const failures: string[] = [];
    if (result[0].status === "fulfilled") setAgents(result[0].value); else failures.push(`Agent 目录：${errorText(result[0].reason)}`);
    if (result[1].status === "fulfilled") setSkills(result[1].value); else failures.push(`Skill 目录：${errorText(result[1].reason)}`);
    setState({ loading: false, failures, loadedAt: new Date() });
  }, []);
  useEffect(() => { void load(); }, [load]);
  const contractComplete = useMemo(() => skills.filter((item) => {
    const config = asObject(item.config_json);
    return Boolean(item.input_contract_json || config.input_contract) && Boolean(item.output_contract_json || config.output_contract);
  }).length, [skills]);

  async function changeLifecycle(skill: ModelSkill, lifecycle_status: string) {
    setChangingId(skill.id);
    setNotice("");
    try {
      await api.updateSkillLifecycle(skill.id, lifecycle_status, "通过中文治理控制台变更生命周期");
      setNotice(`${skill.skill_name} 已切换为${lifecycleNames[lifecycle_status] || lifecycle_status}`);
      await load();
    } catch (error) {
      setNotice(`变更失败：${errorText(error)}`);
    } finally {
      setChangingId(null);
    }
  }

  return <section className="gov-root">
    <Header title="Agent / Skill 治理契约" detail="管理调用边界、资产权限、输入输出契约、生命周期和版本关系。" state={state} onRefresh={() => void load()} />
    <FailureNotice failures={state.failures} />
    {notice && <div className="gov-notice"><FileCheck2 size={15} />{notice}</div>}
    <div className="gov-summaries"><Summary label="Agent" value={agents.length} detail="已登记编排主体" /><Summary label="Skill" value={skills.length} detail="受控能力目录" /><Summary label="契约完整" value={contractComplete} detail={`输入输出契约均已登记，占 ${skills.length ? Math.round(contractComplete / skills.length * 100) : 0}%`} tone={contractComplete === skills.length && skills.length ? "success" : "warning"} /><Summary label="已启用" value={skills.filter((item) => (item.lifecycle_status || (item.enabled ? "ENABLED" : "DISABLED")) === "ENABLED").length} detail="可被 Agent 调用的技能" tone="success" /></div>
    <section className="gov-panel">
      <div className="gov-panel-title"><div><h2>Agent 权限与版本</h2><p>明确可调用 Skill、可访问资产、知识库、数据源和模型</p></div><Bot size={18} /></div>
      <div className="gov-table-wrap"><table><thead><tr><th>Agent</th><th>版本 / 模型</th><th>可调用 Skill</th><th>数据资产</th><th>知识库</th><th>数据源</th><th>状态</th></tr></thead><tbody>
        {agents.length ? agents.map((item) => <tr key={item.id}><td><strong>{item.display_name}</strong><small>{item.agent_code || `AGENT_${item.id}`}</small></td><td>v{item.version || "1"}<small>{item.model_instance_code || "默认模型路由"}</small></td><td>{item.skill_ids.length}</td><td>{item.data_asset_ids.length}</td><td>{item.knowledge_base_ids.length}</td><td>{item.data_source_ids.length}</td><td><i className={`gov-status ${statusTone(item.lifecycle_status || (item.enabled ? "ENABLED" : "DISABLED"))}`}>{lifecycleNames[item.lifecycle_status || ""] || (item.enabled ? "启用" : "停用")}</i></td></tr>) : <tr><td colSpan={7} className="gov-empty">暂无 Agent 定义</td></tr>}
      </tbody></table></div>
    </section>
    <section className="gov-panel">
      <div className="gov-panel-title"><div><h2>Skill 契约与生命周期</h2><p>禁止任意 SQL 和任意数据库写入；副作用必须声明并受权限策略约束</p></div><Wrench size={18} /></div>
      <div className="gov-contract-list">
        {skills.length ? skills.map((item) => {
          const config = asObject(item.config_json);
          const input = item.input_contract_json || asObject(config.input_contract);
          const output = item.output_contract_json || asObject(config.output_contract);
          const permission = item.permission_policy_json || asObject(config.permission_policy);
          const retry = item.retry_policy_json || asObject(config.retry_policy);
          const errorPolicy = item.error_policy_json || asObject(config.error_policy);
          const lifecycle = item.lifecycle_status || (item.enabled ? "ENABLED" : "DISABLED");
          return <article className="gov-contract" key={item.id}>
            <header><div><strong>{item.skill_name}</strong><code>{item.skill_code || `SKILL_${item.id}`} · v{item.version || "1"}</code></div><i className={`gov-status ${statusTone(lifecycle)}`}>{lifecycleNames[lifecycle] || lifecycle}</i></header>
            <dl><div><dt>输入契约</dt><dd>{compactJson(input)}</dd></div><div><dt>输出契约</dt><dd>{compactJson(output)}</dd></div><div><dt>权限策略</dt><dd>{compactJson(permission)}</dd></div><div><dt>副作用</dt><dd>{item.side_effect_level || String(config.side_effect_level || "未声明")}</dd></div><div><dt>幂等策略</dt><dd>{item.idempotency_policy || String(config.idempotency_policy || "未声明")}</dd></div><div><dt>重试策略</dt><dd>{compactJson(retry)}</dd></div><div><dt>错误策略</dt><dd>{compactJson(errorPolicy)}</dd></div></dl>
            <footer><label>生命周期<select aria-label={`${item.skill_name} 生命周期`} value={lifecycle} disabled={changingId === item.id} onChange={(event) => void changeLifecycle(item, event.target.value)}><option value="DRAFT">草稿</option><option value="TESTING">测试中</option><option value="ENABLED">启用</option><option value="DISABLED">停用</option><option value="DEPRECATED">废弃</option></select></label><span>{item.skill_type === "EXECUTABLE_TOOL" ? "执行型 Skill" : "Prompt SOP"}</span></footer>
          </article>;
        }) : <div className="gov-empty">暂无 Skill 定义</div>}
      </div>
    </section>
    <footer className="gov-footer">最近刷新：{state.loadedAt?.toLocaleString("zh-CN") || "--"}</footer>
  </section>;
}

export function GovernanceCenter({ view }: { view: GovernanceCenterView }) {
  if (view === "quality") return <QualityView />;
  if (view === "executions") return <ExecutionsView />;
  return <ContractsView />;
}

export default GovernanceCenter;
