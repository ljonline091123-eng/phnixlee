import { useCallback, useEffect, useMemo, useState } from "react";
import { FileText, Play, RefreshCw, Search, X } from "lucide-react";
import {
  batchGovernanceApi,
  type GovernanceMode,
  type GovernanceRecord,
  type GovernanceReport,
} from "./batchGovernanceApi";

const statusNames: Record<string, string> = {
  PENDING: "等待执行",
  RUNNING: "执行中",
  RETRY: "等待重试",
  COMPLETED: "已完成",
  SUCCESS: "已完成",
  PARTIAL: "部分完成",
  FAILED: "执行失败",
  EMPTY: "无有效数据",
};

const modeNames: Record<string, string> = {
  AI_AGENT_SKILL_GOVERNANCE: "AI + Agent + Skill 治理",
  SYSTEM_GOVERNANCE: "系统治理",
};

function statusText(value?: string | null) {
  const key = String(value || "PENDING").toUpperCase();
  return statusNames[key] || key;
}

function statusClass(value?: string | null) {
  const key = String(value || "PENDING").toUpperCase();
  if (["SUCCESS", "COMPLETED"].includes(key)) return "success";
  if (["FAILED", "CANCELLED"].includes(key)) return "failed";
  if (["PARTIAL", "EMPTY"].includes(key)) return "partial";
  if (key === "RUNNING") return "running";
  return "pending";
}

function dateText(value?: string | null) {
  return value ? new Date(value).toLocaleString("zh-CN") : "--";
}

function jsonText(value: unknown) {
  if (value == null) return "--";
  if (typeof value === "string") return value;
  try { return JSON.stringify(value, null, 2); } catch { return String(value); }
}

function reportSummary(report: GovernanceReport | undefined) {
  if (!report) return { stocks: 0, completed: 0, errors: 0, skills: 0 };
  if (report.summary) {
    return {
      stocks: report.summary.stock_count ?? report.stock_results?.length ?? 0,
      completed: report.summary.completed_stocks ?? 0,
      errors: report.summary.error_count ?? report.errors?.length ?? 0,
      skills: report.summary.skill_execution_count ?? report.skills?.length ?? 0,
    };
  }
  const stocks = report.stock_results || [];
  const completed = stocks.filter((row) => ["COMPLETED", "SUCCESS"].includes(String(row.status || "").toUpperCase())).length;
  return { stocks: stocks.length, completed, errors: report.errors?.length || 0, skills: report.skills?.length || 0 };
}

export function GovernanceRecords({ onStartTask, onOptimizeSkill }: { onStartTask?: () => void; onOptimizeSkill?: () => void } = {}) {
  const [records, setRecords] = useState<GovernanceRecord[]>([]);
  const [total, setTotal] = useState(0);
  const [offset, setOffset] = useState(0);
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState("");
  const [mode, setMode] = useState<GovernanceMode | "">("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [selected, setSelected] = useState<(GovernanceRecord & { governance_report?: GovernanceReport }) | null>(null);
  const [startingId, setStartingId] = useState<number | null>(null);
  const pageSize = 20;

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const result = await batchGovernanceApi.listRecords({ limit: pageSize, offset, keyword, status, governanceMode: mode });
      setRecords(result.items || []);
      setTotal(result.total || 0);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "治理记录加载失败");
    } finally { setLoading(false); }
  }, [keyword, mode, offset, status]);

  useEffect(() => { void load(); }, [load]);

  async function openRecord(record: GovernanceRecord) {
    setError("");
    try { setSelected(await batchGovernanceApi.report(record.job_id)); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "治理报告加载失败"); }
  }

  async function startRecord(record: GovernanceRecord) {
    setStartingId(record.job_id);
    setError("");
    try {
      await batchGovernanceApi.start(record.job_id);
      await load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "任务启动失败");
    } finally {
      setStartingId(null);
    }
  }

  const selectedReport = selected?.governance_report;
  const summary = useMemo(() => reportSummary(selectedReport), [selectedReport]);
  const totalPages = Math.max(1, Math.ceil(total / pageSize));
  const currentPage = Math.floor(offset / pageSize) + 1;

  return <section className="governance-records-root">
    <style>{`
      .governance-records-root{display:grid;gap:16px;color:#20372d}
      .gr-head{display:flex;justify-content:space-between;align-items:flex-start;gap:16px;padding:22px 24px;border:1px solid #d8e1d7;border-radius:14px;background:linear-gradient(135deg,#fffdf6,#f1f7f0)}
      .gr-head h1{margin:0;font-size:26px}.gr-head p{margin:7px 0 0;color:#68796f;font-size:13px;line-height:1.6}.gr-actions{display:flex;gap:8px;flex-wrap:wrap}
      .gr-button{display:inline-flex;align-items:center;gap:6px;min-height:35px;padding:7px 12px;border:1px solid #c8d7c9;border-radius:8px;background:#fff;color:#315844;font:inherit;font-size:12px;cursor:pointer}.gr-button.primary{color:#fff;background:#2f6246;border-color:#2f6246}.gr-button:disabled{opacity:.55;cursor:wait}
      .gr-panel{border:1px solid #d9e0d7;border-radius:12px;background:#fff;overflow:hidden}.gr-toolbar{display:grid;grid-template-columns:minmax(220px,1fr) 150px 210px auto;gap:9px;padding:13px;border-bottom:1px solid #e4e9e2;background:#f7faf6}.gr-input,.gr-select{height:36px;padding:7px 10px;border:1px solid #cbd8cd;border-radius:7px;background:#fff;color:#294238;font:inherit;font-size:12px}.gr-search{position:relative}.gr-search svg{position:absolute;left:10px;top:11px;color:#829388}.gr-search .gr-input{width:100%;padding-left:31px}
      .gr-table-wrap{overflow:auto}.gr-table{width:100%;min-width:930px;border-collapse:collapse}.gr-table th{padding:10px 12px;background:#f5f8f3;color:#68796f;font-size:11px;text-align:left;white-space:nowrap}.gr-table td{padding:12px;border-top:1px solid #edf0eb;font-size:12px;vertical-align:top}.gr-table strong{display:block;color:#244535}.gr-table small{display:block;margin-top:4px;color:#819087}.gr-code{font-family:ui-monospace,SFMono-Regular,Consolas,monospace;color:#516a5b;font-size:11px}.gr-status{display:inline-flex;padding:4px 8px;border-radius:999px;font-size:11px;font-weight:700}.gr-status.success{color:#28734b;background:#e2f2e6}.gr-status.failed{color:#a43c35;background:#fbe5e1}.gr-status.partial{color:#8b671d;background:#fff2cc}.gr-status.running{color:#2f6388;background:#e4eff7}.gr-status.pending{color:#68796f;background:#edf1ed}.gr-link{border:0;background:none;color:#2f6a4b;text-decoration:underline;cursor:pointer;font:inherit;font-size:12px}.gr-empty{padding:38px;text-align:center;color:#77877d}.gr-error{padding:11px 13px;border:1px solid #e2b3ac;border-radius:8px;color:#9b3f35;background:#fff3f1;font-size:12px}.gr-footer{display:flex;justify-content:space-between;align-items:center;padding:12px 14px;color:#718077;font-size:12px}.gr-page{display:flex;gap:7px;align-items:center}.gr-modal-backdrop{position:fixed;inset:0;z-index:80;display:grid;place-items:center;padding:24px;background:rgba(10,30,22,.5)}.gr-modal{width:min(1120px,100%);max-height:calc(100vh - 48px);overflow:auto;border:1px solid #d2ddd2;border-radius:15px;background:#fff;box-shadow:0 24px 80px rgba(0,0,0,.22)}.gr-modal-head{display:flex;justify-content:space-between;gap:16px;align-items:flex-start;padding:18px 22px;border-bottom:1px solid #e3e9e1}.gr-modal-head h2{margin:0;font-size:20px}.gr-modal-head p{margin:6px 0 0;color:#708077;font-size:12px}.gr-close{border:0;background:none;color:#66776d;cursor:pointer}.gr-modal-body{display:grid;gap:16px;padding:20px 22px}.gr-summary{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:10px}.gr-summary div{padding:12px;border:1px solid #dbe6da;border-radius:9px;background:#f7fbf6}.gr-summary small{display:block;color:#76867c;font-size:11px}.gr-summary strong{display:block;margin-top:5px;font-size:20px}.gr-section{display:grid;gap:8px}.gr-section h3{margin:0;font-size:14px}.gr-detail-table{width:100%;border-collapse:collapse}.gr-detail-table th,.gr-detail-table td{padding:8px;border:1px solid #e1e8df;text-align:left;font-size:11px;vertical-align:top}.gr-detail-table th{background:#f4f8f2}.gr-pre{max-height:300px;overflow:auto;margin:0;padding:12px;border-radius:8px;background:#f5f7f4;white-space:pre-wrap;overflow-wrap:anywhere;font:11px/1.55 ui-monospace,SFMono-Regular,Consolas,monospace}
      @media(max-width:800px){.gr-toolbar{grid-template-columns:1fr}.gr-summary{grid-template-columns:repeat(2,1fr)}.gr-head{display:block}.gr-actions{margin-top:12px}}
    `}</style>
    <header className="gr-head"><div><p className="eyebrow">GOVERNANCE HISTORY</p><h1>治理记录</h1><p>查询股票数据采集、湖仓发布、知识库切片、知识图谱和 AI + Agent + Skill 执行历史；打开记录可查看治理报告与成果。</p></div><div className="gr-actions"><button className="gr-button primary" type="button" onClick={() => onStartTask?.()}><Play size={14} />发起治理任务</button><button className="gr-button" type="button" disabled={loading} onClick={() => void load()}><RefreshCw size={14} />刷新</button></div></header>
    {error && <div className="gr-error">{error}</div>}
    <section className="gr-panel"><div className="gr-toolbar"><label className="gr-search"><Search size={14} /><input className="gr-input" value={keyword} onChange={(event) => { setKeyword(event.target.value); setOffset(0); }} placeholder="搜索股票、批次号或市场" /></label><select className="gr-select" value={status} onChange={(event) => { setStatus(event.target.value); setOffset(0); }}><option value="">全部状态</option><option value="PENDING">等待执行</option><option value="RUNNING">执行中</option><option value="COMPLETED">已完成</option><option value="PARTIAL">部分完成</option><option value="FAILED">执行失败</option></select><select className="gr-select" value={mode} onChange={(event) => { setMode(event.target.value as GovernanceMode | ""); setOffset(0); }}><option value="">全部治理方式</option><option value="AI_AGENT_SKILL_GOVERNANCE">AI + Agent + Skill 治理</option><option value="SYSTEM_GOVERNANCE">系统治理</option></select><button className="gr-button primary" type="button" onClick={() => { setOffset(0); void load(); }}>查询</button></div>
      <div className="gr-table-wrap"><table className="gr-table"><thead><tr><th>治理批次</th><th>股票范围</th><th>治理方式</th><th>状态</th><th>进度</th><th>创建时间</th><th>操作</th></tr></thead><tbody>{records.length ? records.map((record) => { const displayStatus = record.result_status || record.status; const canStart = ["PENDING", "RETRY", "FAILED"].includes(String(displayStatus).toUpperCase()); return <tr key={record.job_id}><td><strong>任务 #{record.job_id}</strong><span className="gr-code">{record.governance_batch_id || "--"}</span><small>Pipeline #{record.pipeline_run_id || "--"}</small></td><td><strong>{record.stock_count} 只股票</strong><small>{record.effective_options?.stocks ? (record.effective_options.stocks as Array<{market:string;symbol:string}>).slice(0,3).map((item) => `${item.market}:${item.symbol}`).join("、") : "范围记录见详情"}</small></td><td>{modeNames[record.governance_mode || ""] || record.governance_mode || "--"}<small>Agent 执行：{record.agent_execution_run_id ? "已记录" : "未记录"}</small></td><td><span className={`gr-status ${statusClass(displayStatus)}`}>{statusText(displayStatus)}</span><small>{record.current_stage || "--"}</small></td><td>{record.progress ?? 0}%</td><td>{dateText(record.created_at)}</td><td><div className="gr-actions">{canStart && <button className="gr-link" type="button" disabled={startingId === record.job_id} onClick={() => void startRecord(record)}><Play size={13} /> {startingId === record.job_id ? "启动中" : "立即执行"}</button>}{["PARTIAL", "FAILED"].includes(String(displayStatus).toUpperCase()) && <button className="gr-link" type="button" onClick={() => void batchGovernanceApi.retry(record.job_id).then(load).catch((cause) => setError(cause instanceof Error ? cause.message : "重试失败"))}>重试</button>}<button className="gr-link" type="button" onClick={() => void openRecord(record)}><FileText size={13} /> 查看报告与成果</button></div></td></tr>; }) : <tr><td className="gr-empty" colSpan={7}>{loading ? "正在加载治理记录…" : "暂无治理记录"}</td></tr>}</tbody></table></div>
      <footer className="gr-footer"><span>共 {total} 条记录{loading ? " · 加载中…" : ""}</span><span className="gr-page"><button className="gr-button" type="button" disabled={offset === 0 || loading} onClick={() => setOffset(Math.max(0, offset - pageSize))}>上一页</button><span>第 {currentPage} / {totalPages} 页</span><button className="gr-button" type="button" disabled={offset + pageSize >= total || loading} onClick={() => setOffset(offset + pageSize)}>下一页</button></span></footer>
    </section>
    {selected && selectedReport && <div className="gr-modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setSelected(null); }}><section className="gr-modal" role="dialog" aria-modal="true"><header className="gr-modal-head"><div><h2>治理报告 · 任务 #{selected.job_id}</h2><p>{modeNames[selected.governance_mode || ""] || selected.governance_mode} · {selected.governance_batch_id || "无批次号"} · {dateText(selected.created_at)}</p></div><button className="gr-close" type="button" onClick={() => setSelected(null)}><X size={18} /></button></header><div className="gr-modal-body"><div className="gr-summary"><div><small>股票数</small><strong>{summary.stocks}</strong></div><div><small>已完成股票</small><strong>{summary.completed}</strong></div><div><small>Skill 执行</small><strong>{summary.skills}</strong></div><div><small>错误/缺口</small><strong>{summary.errors}</strong></div></div><section className="gr-section"><h3>阶段进度</h3><div className="gr-detail-table"><table><thead><tr><th>阶段</th><th>完成</th><th>未完成</th><th>完成率</th></tr></thead><tbody>{Object.entries(selectedReport.summary?.stage_stats || {}).map(([code, item]) => <tr key={code}><td>{item.label || code}</td><td>{item.completed ?? 0}</td><td>{item.incomplete ?? 0}</td><td>{item.completion_rate ?? 0}%</td></tr>)}</tbody></table></div></section><section className="gr-section"><h3>股票基本信息与治理成果</h3><table className="gr-detail-table"><thead><tr><th>市场</th><th>代码</th><th>股票名称</th><th>交易所/板块</th><th>治理状态</th><th>当前阶段</th><th>质量摘要</th><th>证据数</th></tr></thead><tbody>{(selectedReport.stock_results || []).map((row, index) => <tr key={`${String(row.market)}-${String(row.symbol)}-${index}`}><td>{String(row.market || "--")}</td><td>{String(row.symbol || "--")}</td><td>{String(row.stock_name || "--")}</td><td>{String(row.exchange || "--")} / {String(row.listing_board || "--")}</td><td><span className={`gr-status ${statusClass(String(row.status || ""))}`}>{statusText(String(row.status || ""))}</span></td><td>{String(row.current_stage || "--")}</td><td>{jsonText(row.quality_summary)}</td><td>{Array.isArray(row.evidence_ids) ? row.evidence_ids.length : 0}</td></tr>)}</tbody></table></section><section className="gr-section"><h3>失败原因与优化建议</h3>{selectedReport.analysis?.failure_reasons?.length ? <ul>{selectedReport.analysis.failure_reasons.map((item, index) => <li key={`${item.code}-${index}`}>{item.code || "未分类"}：{item.reason || `出现 ${item.count || 0} 次`}</li>)}</ul> : <p>当前没有记录失败原因。</p>}{selectedReport.analysis?.recommendations?.length ? <ul>{selectedReport.analysis.recommendations.map((item, index) => <li key={index}>{item}</li>)}</ul> : null}<div className="gr-actions"><button className="gr-button" type="button" onClick={() => { setSelected(null); onStartTask?.(); }}>继续治理</button><button className="gr-button" type="button" onClick={() => { setSelected(null); onOptimizeSkill?.(); }}>进入 Skill 优化审核</button></div></section><section className="gr-section"><h3>Agent / Skill 执行链路</h3><p>{selectedReport.agent ? `${String(selectedReport.agent.agent_code || "Agent")} · v${String(selectedReport.agent.agent_version || "--")} · ${statusText(String(selectedReport.agent.status || ""))}` : "未关联 Agent 执行记录"}</p>{selectedReport.skills?.length ? <table className="gr-detail-table"><thead><tr><th>Skill</th><th>版本</th><th>状态</th><th>副作用级别</th><th>证据数</th></tr></thead><tbody>{selectedReport.skills.map((skill, index) => <tr key={`${String(skill.skill_code)}-${index}`}><td>{String(skill.skill_code || "--")}</td><td>{String(skill.skill_version || "--")}</td><td>{statusText(String(skill.status || ""))}</td><td>{String(skill.side_effect_level || "--")}</td><td>{Array.isArray(skill.evidence_ids) ? skill.evidence_ids.length : 0}</td></tr>)}</tbody></table> : <p>未记录 Skill 执行明细。</p>}</section><section className="gr-section"><h3>原始审计数据</h3><details><summary>展开阶段原始记录</summary><pre className="gr-pre">{jsonText(selectedReport.stage_results)}</pre></details>{selectedReport.errors?.length ? <details><summary>展开错误原文</summary><pre className="gr-pre">{jsonText(selectedReport.errors)}</pre></details> : null}</section></div></section></div>}
  </section>;
}

export default GovernanceRecords;
