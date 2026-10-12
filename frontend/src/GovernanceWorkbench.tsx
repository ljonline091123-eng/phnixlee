import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Activity, ChevronRight, FileText, Play, RefreshCw, X } from "lucide-react";
import { GovernanceProgress } from "./GovernanceProgress";
import { useGovernanceDialog } from "./useGovernanceDialog";
import { batchGovernanceApi, type BatchGovernanceJob, type GovernanceActions, type GovernanceContinuationPlan, type GovernanceRecord } from "./batchGovernanceApi";
import "./GovernanceWorkbench.css";

const statusNames: Record<string, string> = {
  PENDING: "等待执行", RUNNING: "执行中", RETRY: "等待重试", COMPLETED: "流程结束",
  SUCCESS: "已完成", PARTIAL: "部分完成", FAILED: "执行失败", SKIPPED: "本轮未执行",
  EMPTY: "无有效数据", PUBLISHED: "已发布", BUILT: "已构建", GOVERNED: "已治理",
  DRAFT: "待测试草稿", APPROVED: "审核通过", REJECTED: "已驳回", PASSED: "测试通过",
  UNKNOWN: "尚未记录", CANCELLED: "已取消", MISSING: "数据缺失", CANDIDATE: "待审核候选",
  PENDING_REVIEW: "待测试与审核",
};
const stageNames: Record<string, string> = {
  QUEUED: "等待后台领取", VALIDATION: "主数据校验", BUSINESS_DATA: "业务数据采集",
  STRUCTURED_EVENTS: "新闻公告知识结构化", AI_QUALITY_GATE: "AI治理质量门禁",
  QUALITY_GATE: "数据质量门禁", LAKEHOUSE_EXPORT: "湖仓数据发布", DOCUMENT_CHUNKS: "知识库与文档切片",
  KNOWLEDGE_GRAPH: "知识图谱投影", AGENT_SKILL_GOVERNANCE: "Agent与Skill治理", COMPLETE: "本轮处理结束",
};
const stageOrder = ["VALIDATION", "BUSINESS_DATA", "STRUCTURED_EVENTS", "AI_QUALITY_GATE", "QUALITY_GATE",
  "LAKEHOUSE_EXPORT", "DOCUMENT_CHUNKS", "KNOWLEDGE_GRAPH", "AGENT_SKILL_GOVERNANCE"];
const businessNames: Record<string, string> = { QUOTE: "实时行情", KLINE: "历史量价", FINANCIAL: "财务数据", NEWS: "新闻", NOTICE: "公告", F10: "F10资料",
  stock_news: "新闻", stock_notice: "公告", stock_kline: "历史量价", stock_financial_report: "财务报告", stock_f10_cache: "F10资料" };
const phaseNames: Record<string, string> = { PREPARING: "准备文档", BODY_FETCH: "读取与补取原文", ANALYZING: "语义分析", PLANNING: "阅读全文并制定方案",
  PLANNED: "待用户确认方案", SEMANTIC_READING: "理解原文", STRUCTURED_EXTRACTION: "抽取事件与关系", STRUCTURED_EXTRACTION_REPAIR: "核验证据", FINISHED: "文档阶段结束" };

function lifecycle(job: BatchGovernanceJob) { return String(job.job_status || job.status || "PENDING").toUpperCase(); }
function displayStatus(job: BatchGovernanceJob) {
  return lifecycle(job) === "COMPLETED" ? String(job.result_status || job.status || "COMPLETED").toUpperCase() : lifecycle(job);
}
function statusText(value?: string | null) { const key = String(value || "PENDING").toUpperCase(); return statusNames[key] || key; }
function stageText(value?: string | null) { const key = String(value || "").toUpperCase(); return stageNames[key] || value || "等待执行"; }
function statusClass(value?: string | null) {
  const key = String(value || "PENDING").toUpperCase();
  if (["SUCCESS", "COMPLETED", "PUBLISHED", "BUILT", "GOVERNED"].includes(key)) return "success";
  if (key === "RUNNING") return "running";
  if (["FAILED", "CANCELLED"].includes(key)) return "failed";
  if (["PARTIAL", "EMPTY"].includes(key)) return "partial";
  return "pending";
}
function dateText(value?: string | null) { return value ? new Date(value).toLocaleString("zh-CN") : "--"; }
function errorText(value: string | Record<string, unknown>) {
  if (typeof value === "string") return value;
  return String(value.message || value.error || JSON.stringify(value));
}
function itemText(value: unknown) {
  const text = String(value || "未记录说明");
  return Object.entries(businessNames).reduce((result, [key, label]) => result.replaceAll(key, label), text);
}
function stageSummary(stage?: Record<string, unknown>) {
  if (!stage) return "等待执行";
  if (stage.message) return String(stage.message);
  const completed = Array.isArray(stage.completed_items) ? stage.completed_items.length : Number(stage.succeeded || stage.completed || 0);
  const incomplete = Array.isArray(stage.incomplete_items) ? stage.incomplete_items.length : Number(stage.failed || stage.incomplete || 0);
  return completed || incomplete ? `通过 ${completed} 项 · 待处理 ${incomplete} 项` : "阶段结果已记录";
}
function requestedStage(job: BatchGovernanceJob, code: string) {
  const options = job.effective_options || {};
  if (code === "BUSINESS_DATA") return !!options.collect_business_data;
  if (code === "STRUCTURED_EVENTS") return !!options.structure_documents;
  if (code === "LAKEHOUSE_EXPORT") return !!options.export_lakehouse || !!options.run_graph;
  if (code === "DOCUMENT_CHUNKS") return !!options.archive_chunks || !!options.run_graph;
  if (code === "KNOWLEDGE_GRAPH") return !!options.run_graph;
  if (code === "AGENT_SKILL_GOVERNANCE") return options.governance_mode === "AI_AGENT_SKILL_GOVERNANCE" || !!options.run_agent_governance;
  return true;
}
function semanticTaskLink(job: BatchGovernanceJob) {
  const target = job.document_progress?.current_document || job.effective_options?.stocks?.[0] || job.stock_results?.[0];
  const params = new URLSearchParams({ job: String(job.job_id) });
  if (target?.market && target.symbol) {
    params.set("market", target.market); params.set("symbol", target.symbol);
  }
  return `#/operations/documents?${params}`;
}

type StageEntry = { code: string; label: string; status: string; summary: string; value?: Record<string, unknown> };

export function GovernanceWorkbench({ onStartTask }: { onStartTask?: () => void } = {}) {
  const [records, setRecords] = useState<GovernanceRecord[]>([]);
  const [total, setTotal] = useState(0), [offset, setOffset] = useState(0);
  const [query, setQuery] = useState(""), [keyword, setKeyword] = useState(""), [status, setStatus] = useState("");
  const [selected, setSelected] = useState<BatchGovernanceJob | null>(null);
  const [stageDetailCode, setStageDetailCode] = useState<string | null>(null);
  const [detailOffset, setDetailOffset] = useState(0);
  const [skillActions, setSkillActions] = useState<GovernanceActions | null>(null);
  const [plan, setPlan] = useState<GovernanceContinuationPlan | null>(null);
  const [loading, setLoading] = useState(false), [busy, setBusy] = useState(false);
  const [error, setError] = useState(""), [notice, setNotice] = useState("");
  const [updatedAt, setUpdatedAt] = useState<string | null>(null);
  const loadSequence = useRef(0), openSequence = useRef(0);
  const continuationKey = useRef("");
  const pageSize = 20;
  useGovernanceDialog(plan ? ".gw-confirm" : stageDetailCode ? ".gw-detail-dialog" : selected ? ".gw-dialog" : null, () => {
    if (busy) return;
    if (plan) setPlan(null); else if (stageDetailCode) setStageDetailCode(null); else setSelected(null);
  });

  const load = useCallback(async () => {
    const sequence = ++loadSequence.current;
    setLoading(true);
    try {
      const result = await batchGovernanceApi.listRecords({ limit: pageSize, offset, keyword, status });
      if (sequence !== loadSequence.current) return;
      setRecords(result.items || []); setTotal(result.total || 0);
    } catch (cause) { if (sequence === loadSequence.current) setError(cause instanceof Error ? cause.message : "治理任务加载失败"); }
    finally { if (sequence === loadSequence.current) setLoading(false); }
  }, [offset, keyword, status]);
  useEffect(() => { void load(); }, [load]);
  useEffect(() => {
    const timer = window.setTimeout(() => { setKeyword(query.trim()); setOffset(0); }, 300);
    return () => window.clearTimeout(timer);
  }, [query]);
  useEffect(() => {
    const readLink = () => {
      const jobId = Number(new URLSearchParams(window.location.hash.split("?")[1] || "").get("job"));
      if (jobId > 0) void openTask({ job_id: jobId } as GovernanceRecord);
    };
    readLink(); window.addEventListener("hashchange", readLink);
    return () => window.removeEventListener("hashchange", readLink);
  }, []);
  const hasActiveRecords = records.some(item => ["PENDING", "RUNNING", "RETRY"].includes(lifecycle(item)));
  useEffect(() => {
    if (!hasActiveRecords) return;
    const timer = window.setInterval(() => void load(), 5000);
    return () => window.clearInterval(timer);
  }, [hasActiveRecords, load]);
  useEffect(() => {
    if (!selected || !["PENDING", "RUNNING", "RETRY"].includes(lifecycle(selected))) return;
    let stopped = false, inFlight = false;
    const controller = new AbortController();
    const poll = async () => {
      if (inFlight) return;
      inFlight = true;
      try {
        const value = await batchGovernanceApi.get(selected.job_id, controller.signal);
        if (stopped) return;
        setSelected(value);
        setUpdatedAt(new Date().toLocaleTimeString("zh-CN")); setError("");
        setRecords(current => current.map(item => item.job_id === value.job_id ? { ...item, ...value } : item));
        if (!["PENDING", "RUNNING", "RETRY"].includes(lifecycle(value))) {
          setNotice(`任务 #${value.job_id} 本轮处理结束：${statusText(displayStatus(value))}。可查看节点明细或前往治理报告核验成果。`);
          void load();
        }
      } catch (cause) { if (!stopped) setError(`进度暂未更新，正在自动重试：${cause instanceof Error ? cause.message : "网络连接失败"}`); }
      finally { inFlight = false; }
    };
    void poll(); const timer = window.setInterval(() => void poll(), 2500);
    return () => { stopped = true; controller.abort(); window.clearInterval(timer); };
  }, [selected?.job_id, selected ? lifecycle(selected) : "", load]);

  const stageEntries = useMemo<StageEntry[]>(() => {
    if (!selected) return [];
    const results = selected.stage_results || {};
    const summaryStats = (selected.governance_report as Record<string, unknown> | undefined)?.summary as Record<string, unknown> | undefined;
    const stats = (summaryStats?.stage_stats || {}) as Record<string, Record<string, unknown>>;
    const keys = [...new Set([...stageOrder, ...Object.keys(results), ...Object.keys(stats)])]
      .filter(code => requestedStage(selected, code) || results[code] || stats[code]);
    return keys.map(code => {
      const value = results[code] as Record<string, unknown> | undefined;
      const stat = stats[code];
      const isCurrent = String(selected.current_stage || "").toUpperCase().includes(code);
      const terminal = ["COMPLETED", "FAILED", "CANCELLED"].includes(lifecycle(selected));
      const status = String(value?.status || stat?.status || (isCurrent && lifecycle(selected) === "RUNNING" ? "RUNNING" : isCurrent && lifecycle(selected) === "FAILED" ? "FAILED" : terminal ? "SKIPPED" : "PENDING"));
      const summary = value || stat ? stageSummary(value || stat) : status === "SKIPPED" ? "本轮未执行，未形成阶段成果" : status === "RUNNING" ? "后台执行中，等待阶段成果" : status === "FAILED" ? "执行中断，尚未形成阶段成果" : "等待执行";
      return { code, label: String(stat?.label || stageNames[code] || code), status, summary, value };
    });
  }, [selected]);
  const stageDetail = stageEntries.find(item => item.code === stageDetailCode);
  const detailItems = [...(Array.isArray(stageDetail?.value?.incomplete_items) ? stageDetail.value.incomplete_items : []),
    ...(Array.isArray(stageDetail?.value?.completed_items) ? stageDetail.value.completed_items : [])] as Record<string, unknown>[];
  const runningCount = records.filter(item => ["PENDING", "RUNNING", "RETRY"].includes(lifecycle(item))).length;
  const actionableCount = records.filter(item => ["PARTIAL", "FAILED"].includes(displayStatus(item))).length;

  async function openTask(job: GovernanceRecord | BatchGovernanceJob) {
    const sequence = ++openSequence.current;
    setBusy(true); setError(""); setNotice(""); setPlan(null); setStageDetailCode(null); setSkillActions(null);
    try {
      const value = await batchGovernanceApi.get(job.job_id);
      if (sequence !== openSequence.current) return;
      setSelected(value); setUpdatedAt(new Date().toLocaleTimeString("zh-CN"));
    }
    catch (cause) { setError(cause instanceof Error ? cause.message : "治理任务读取失败"); }
    finally { if (sequence === openSequence.current) setBusy(false); }
  }
  async function startTask() {
    if (!selected) return;
    setBusy(true); setError("");
    try {
      const value = lifecycle(selected) === "FAILED" ? await batchGovernanceApi.retry(selected.job_id) : await batchGovernanceApi.start(selected.job_id);
      setSelected(value); setNotice(`任务 #${value.job_id} 已唤醒后台执行器，窗口将自动刷新节点和当前处理对象。`);
    } catch (cause) { setError(cause instanceof Error ? cause.message : "任务启动失败"); }
    finally { setBusy(false); }
  }
  async function skillAction(action: () => Promise<unknown>) {
    if (!selected) return;
    setBusy(true); setError("");
    try {
      await action();
      const value = await batchGovernanceApi.get(selected.job_id);
      setSelected(value); setSkillActions((value.governance_report as { actions?: GovernanceActions } | undefined)?.actions || null);
      setNotice("Skill优化操作已完成，测试和审核结果已更新。");
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Skill优化操作失败"); }
    finally { setBusy(false); }
  }
  async function prepareContinuation() {
    if (!selected) return;
    setBusy(true); setError("");
    try { continuationKey.current = crypto.randomUUID(); setPlan(await batchGovernanceApi.continuationPlan(selected.job_id)); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "续作计划生成失败"); }
    finally { setBusy(false); }
  }
  async function executeContinuation() {
    if (!selected || !plan?.can_continue) return;
    setBusy(true); setError("");
    try {
      const next = await batchGovernanceApi.continueJob(selected.job_id, continuationKey.current);
      setPlan(null); setSelected(next);
      setNotice(`续作任务 #${next.job_id} 已创建并唤醒后台执行器；原任务 #${selected.job_id} 报告保持只读。`);
      await load();
    } catch (cause) { setError(cause instanceof Error ? cause.message : "续作任务创建失败"); }
    finally { setBusy(false); }
  }
  function openReport(jobId: number) { window.location.hash = `#/operations/records?job=${jobId}`; }

  return <section className="gw-root">
    <header className="gw-header"><div><p className="eyebrow">GOVERNANCE WORKBENCH</p><h1>治理工作台</h1><p>在这里执行治理、查看节点进度并处理待办；治理报告只负责查询已经形成的成果。</p></div><div className="gw-actions"><button onClick={() => onStartTask?.()}><Play size={15} />发起新任务</button><button disabled={loading} onClick={() => void load()}><RefreshCw size={15} />刷新</button></div></header>
    {error && <div className="gw-message error" role="alert">{error}</div>}
    {notice && <div className="gw-message" role="status">{notice}</div>}
    <div className="gw-summary"><article><span>本页队列与执行中</span><strong>{runningCount}</strong><small>自动更新治理进度</small></article><article><span>本页需要处理</span><strong>{actionableCount}</strong><small>部分完成或失败</small></article><article><span>筛选范围任务总数</span><strong>{total}</strong><small>搜索、筛选后分批查看</small></article></div>
    <section className="gw-list"><header><div><h2>治理任务</h2><p>主页面只展示整体状态；进入治理窗口后再按节点下钻。</p></div></header>
      <div className="gw-toolbar"><label>搜索任务<input aria-label="搜索治理任务" placeholder="股票代码、名称或批次号" value={query} onChange={event => setQuery(event.target.value)} /></label><label>任务状态<select aria-label="治理任务状态" value={status} onChange={event => { setStatus(event.target.value); setOffset(0); }}><option value="">全部状态</option><option value="PENDING">等待执行</option><option value="RUNNING">执行中</option><option value="PARTIAL">部分完成</option><option value="FAILED">执行失败</option><option value="COMPLETED">流程结束</option></select></label></div>
      <div className="gw-table-wrap"><table><thead><tr><th>任务</th><th>治理范围</th><th>当前节点</th><th>整体进度</th><th>状态</th><th>操作</th></tr></thead><tbody>
        {records.map(record => <tr key={record.job_id}><td><strong>任务 #{record.job_id}</strong><small>{dateText(record.created_at)}</small></td><td>{record.stock_count}只股票<small>{(record.stock_names || record.stock_symbols || record.effective_options?.stocks?.map(item => `${item.market}:${item.symbol}`) || []).slice(0, 3).join("、")}</small><small>{record.governance_mode === "AI_AGENT_SKILL_GOVERNANCE" ? "AI + Agent + Skill" : "系统治理"}</small></td><td>{stageText(record.current_stage)}{record.document_progress?.current_document && <small>{record.document_progress.current_document.title}</small>}</td><td><GovernanceProgress job={record} /></td><td><span className={`gw-status ${statusClass(displayStatus(record))}`}>{statusText(displayStatus(record))}</span></td><td><button disabled={busy} className="gw-primary" onClick={() => void openTask(record)}>进入治理窗口<ChevronRight size={14} /></button></td></tr>)}
        {!records.length && <tr><td colSpan={6} className="gw-empty">{loading ? "正在加载…" : "暂无治理任务"}</td></tr>}
      </tbody></table></div>
      <footer className="gw-pagination"><span>共 {total} 条任务{loading ? " · 加载中…" : ""}</span><div className="gw-actions"><button disabled={loading || offset === 0} onClick={() => setOffset(Math.max(0, offset - pageSize))}>上一页</button><span>第 {Math.floor(offset / pageSize) + 1} / {Math.max(1, Math.ceil(total / pageSize))} 页</span><button disabled={loading || offset + pageSize >= total} onClick={() => setOffset(offset + pageSize)}>下一页</button></div></footer>
    </section>
    {selected && <div className="gw-backdrop" onMouseDown={event => { if (event.target === event.currentTarget && !busy) setSelected(null); }}><section className="gw-dialog" role="dialog" tabIndex={-1} aria-modal="true" aria-labelledby="gw-dialog-title">
      <header><div><p className="eyebrow">GOVERNANCE PROCESS</p><h2 id="gw-dialog-title">治理窗口 · 任务 #{selected.job_id}</h2><p>{selected.stock_count}只股票 · {selected.governance_mode === "AI_AGENT_SKILL_GOVERNANCE" ? "AI + Agent + Skill治理" : "系统治理"} · {dateText(selected.created_at)}</p></div><button aria-label="关闭治理窗口" disabled={busy} onClick={() => setSelected(null)}><X size={20} /></button></header>
      <div className="gw-dialog-body">
        {notice && <div className="gw-message" role="status">{notice}</div>}{error && <div className="gw-message error">{error}</div>}
        <section className="gw-overall"><div><span>整体进度</span><strong>{Math.round(Number(selected.progress || 0))}%</strong><small>{stageText(selected.current_stage)}</small></div><GovernanceProgress job={selected} /><span className={`gw-status ${statusClass(displayStatus(selected))}`}>{statusText(displayStatus(selected))}</span></section>
        <p className="gw-refresh-note">{["PENDING", "RUNNING", "RETRY"].includes(lifecycle(selected)) ? "正在自动刷新 · " : "本轮已结束 · "}最近读取：{updatedAt || "正在读取"}{selected.executor?.available === false ? ` · ${selected.executor.message}` : ""}</p>
        {selected.document_progress?.current_document && <section className="gw-current"><Activity size={18} /><div><strong>{stageText(selected.current_stage)} · {phaseNames[selected.document_progress.phase || ""] || "处理文档"}</strong><p>当前：{selected.document_progress.current_document.title}</p><small>已处理 {selected.document_progress.processed || 0}/{selected.document_progress.total || 0}篇 · 成功 {selected.document_progress.succeeded || 0} · 未完成 {selected.document_progress.partial || 0} · 失败 {selected.document_progress.failed || 0}</small></div></section>}
        <section className="gw-node-section"><header><div><h3>治理节点</h3><p>点击节点在明细窗口中查看本阶段范围、结果和失败原因。</p></div></header><ol className="gw-nodes">{stageEntries.map((stage, index) => <li className={statusClass(stage.status)} key={stage.code}><button onClick={() => { setStageDetailCode(stage.code); setDetailOffset(0); }}><b>{["SUCCESS", "COMPLETED"].includes(stage.status.toUpperCase()) ? "✓" : index + 1}</b><span><strong>{stage.label}</strong><small>{stage.summary}</small></span><em>{statusText(stage.status)}</em><ChevronRight size={16} /></button></li>)}</ol></section>
        {!!selected.errors?.length && <details className="gw-errors"><summary>当前错误与待处理项（{selected.errors.length}项）</summary><ul>{selected.errors.slice(0, 10).map((item, index) => <li key={index}>{errorText(item)}</li>)}</ul>{selected.errors.length > 10 && <p>其余问题请打开相关节点明细或治理报告查看。</p>}</details>}
        <footer className="gw-footer"><div><small>后台任务不会因关闭窗口而中断；再次进入可继续查看。</small><small>流程100%仅表示本轮结束，不代表所有数据质量检查通过。</small></div><div className="gw-actions">
          {["PENDING", "RETRY", "FAILED"].includes(lifecycle(selected)) && <button disabled={busy} onClick={() => void startTask()}><Play size={14} />{busy ? "正在提交…" : lifecycle(selected) === "FAILED" ? "重试执行" : "立即执行"}</button>}
          {["PARTIAL", "FAILED"].includes(displayStatus(selected)) && lifecycle(selected) === "COMPLETED" && <button className="gw-primary" disabled={busy} onClick={() => void prepareContinuation()}>{busy ? "正在准备续作…" : "继续治理"}</button>}
          <button onClick={() => openReport(selected.job_id)}><FileText size={14} />查看只读报告</button>
        </div></footer>
      </div>
    </section></div>}
    {stageDetail && selected && <div className="gw-detail-backdrop" onMouseDown={event => { if (event.target === event.currentTarget && !busy) setStageDetailCode(null); }}><section className="gw-detail-dialog" role="dialog" tabIndex={-1} aria-modal="true" aria-labelledby="gw-stage-title"><header><div><p className="eyebrow">NODE DETAIL</p><h2 id="gw-stage-title">{stageDetail.label}</h2><p>{stageDetail.summary}</p></div><button disabled={busy} aria-label="关闭节点明细" onClick={() => setStageDetailCode(null)}><X size={18} /></button></header><div className="gw-detail-body">
        {error && <p role="alert" className="gw-message error">{error}</p>}
        <div className={`gw-stage-banner ${statusClass(stageDetail.status)}`}><strong>{statusText(stageDetail.status)}</strong><span>任务 #{selected.job_id}</span></div>
        {stageDetailCode === "STRUCTURED_EVENTS" && <div className="gw-actions"><a href={semanticTaskLink(selected)}>打开新闻公告交互治理窗口</a><p>继续查看本任务进度与文档待办；逐篇确认类型与Skill、核对知识点，针对不符合项再次分析。</p></div>}
        {detailItems.length > 0 && <section><h3>检查项明细 · {detailItems.length}项</h3><ul>{detailItems.slice(detailOffset, detailOffset + pageSize).map((item, i) => <li key={i}><strong>{itemText(item.label || item.code || `检查项${detailOffset + i + 1}`)}</strong><span>{statusText(String(item.status || "UNKNOWN"))} · {itemText(item.reason)}</span></li>)}</ul>{detailItems.length > pageSize && <div className="gw-actions"><button disabled={detailOffset === 0} onClick={() => setDetailOffset(Math.max(0, detailOffset - pageSize))}>上一页检查项</button><span>第 {Math.floor(detailOffset / pageSize) + 1} / {Math.ceil(detailItems.length / pageSize)} 页</span><button disabled={detailOffset + pageSize >= detailItems.length} onClick={() => setDetailOffset(detailOffset + pageSize)}>下一页检查项</button></div>}</section>}
{stageDetailCode === "AGENT_SKILL_GOVERNANCE" && <section><h3>Skill优化与审核</h3><p>从本轮失败原因生成优化草稿，先测试再审核启用；历史版本保留。</p><button disabled={busy} onClick={() => void skillAction(() => batchGovernanceApi.optimize(selected.job_id))}>生成Skill优化草稿</button>{(skillActions?.drafts || (selected.governance_report as { actions?: GovernanceActions } | undefined)?.actions?.drafts || []).map(draft => <article key={draft.id}><h4>{draft.skill_name} · {statusText(draft.status)}</h4><p>{draft.rationale}</p><details><summary>查看建议规则</summary><pre>{draft.proposed_instructions}</pre></details>{draft.validation && <p>测试结果：{statusText(draft.validation.status)} · {draft.validation.scope}</p>}<div className="gw-actions"><button disabled={busy || !["DRAFT", "PENDING_REVIEW"].includes(draft.status)} onClick={() => void skillAction(() => batchGovernanceApi.testDraft(selected.job_id, draft.id))}>测试优化草稿</button><button disabled={busy || !["DRAFT", "PENDING_REVIEW"].includes(draft.status) || draft.validation?.status !== "PASSED"} onClick={() => void skillAction(() => batchGovernanceApi.reviewDraft(selected.job_id, draft.id, "APPROVE"))}>审核通过并启用</button><button disabled={busy || !["DRAFT", "PENDING_REVIEW"].includes(draft.status)} onClick={() => void skillAction(() => batchGovernanceApi.reviewDraft(selected.job_id, draft.id, "REJECT"))}>驳回草稿</button></div></article>)}</section>}
        {stageDetail.value ? <details><summary>查看阶段诊断数据（高级）</summary><pre>{JSON.stringify(stageDetail.value, null, 2)}</pre></details> : <p>该节点尚未产生阶段结果；窗口会在后台执行后自动更新。</p>}
        <button onClick={() => openReport(selected.job_id)}><FileText size={14} />查询阶段成果与证据</button>
      </div></section></div>}
    {plan && selected && <div className="gw-confirm-backdrop"><section className="gw-confirm" role="dialog" tabIndex={-1} aria-modal="true" aria-labelledby="gw-confirm-title"><header><div><p className="eyebrow">CONTINUATION PLAN</p><h2 id="gw-confirm-title">确认定向续作计划</h2><p>原任务 #{selected.job_id} 报告不会被覆盖。</p></div><button disabled={busy} aria-label="关闭续作计划" onClick={() => setPlan(null)}><X size={18} /></button></header><div>
        {error && <p role="alert" className="gw-message error">{error}</p>}
        <h3>本轮只处理以下缺口</h3>{plan.reasons.length ? <ul>{plan.reasons.map((item, index) => <li key={index}><strong>{stageText(item.stage)}</strong><span>{itemText(item.reason || item.code || "待处理")}</span></li>)}</ul> : <p>当前没有可识别的未完成项。</p>}<div className="gw-plan-grid"><span>业务数据：{plan.request.business_types?.map(code => businessNames[code] || code).join("、") || "不重复采集"}</span><span>新闻公告结构化：{plan.request.structure_documents ? "执行" : "不执行"}</span><span>湖仓／知识库／图谱：{plan.request.run_graph ? "按依赖更新" : "不更新图谱"}</span></div>{plan.notes.length > 0 && <details><summary>续作范围与处理规则</summary><ul>{plan.notes.map((item, i) => <li key={i}>{item}</li>)}</ul></details>}<p className="gw-plan-note">创建后会立即切换到新任务治理窗口并显示执行过程；旧报告保持只读并保留在治理历程中。</p><footer><button disabled={busy} onClick={() => setPlan(null)}>取消</button><button className="gw-primary" disabled={busy || !plan.can_continue} onClick={() => void executeContinuation()}>{busy ? "正在创建续作任务…" : "确认并开始续作"}</button></footer></div></section></div>}
  </section>;
}

export default GovernanceWorkbench;
