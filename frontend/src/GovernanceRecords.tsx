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
  SKIPPED: "未执行",
  UNKNOWN: "未知",
  SUCCEEDED: "执行成功",
  PASSED: "已通过",
  PASS: "已通过",
  PUBLISHED: "已发布",
  BUILT: "已构建",
  GOVERNED: "已治理",
  LOCKED: "已锁定",
  PENDING_REVIEW: "待审核",
  MISSING: "数据缺失",
  CANCELLED: "已取消",
};

const stageNames: Record<string, string> = {
  VALIDATION: "主数据校验",
  BUSINESS_DATA: "业务数据采集",
  AI_QUALITY_GATE: "AI治理质量门禁",
  QUALITY_GATE: "数据质量门禁",
  LAKEHOUSE_EXPORT: "湖仓发布",
  DOCUMENT_CHUNKS: "文档切片",
  KNOWLEDGE_GRAPH: "知识图谱",
  AGENT_SKILL_GOVERNANCE: "Agent + Skill 治理",
  QUEUED: "等待后台领取",
  COMPLETE: "流程结束",
  KNOWLEDGE_PIPELINE: "知识流水线",
  stock_batch_governance: "批量股票治理",
};

const businessNames: Record<string, string> = {
  QUOTE: "实时行情", KLINE: "历史量价", FINANCIAL: "财务数据",
  NEWS: "新闻资讯", NOTICE: "公司公告", F10: "F10资料",
};

const dataNames: Record<string, string> = {
  stock_symbol: "股票主数据", stock_kline: "历史量价", stock_realtime_quote: "实时行情",
  stock_financial_report: "财务数据", stock_news: "新闻", stock_notice: "公告",
  stock_f10_cache: "F10资料", stock_context_event: "政策与外部事件", research_report: "研究报告",
  foundation_entity: "主体主数据", foundation_security: "证券主数据", foundation_listing: "上市关系",
  foundation_evidence: "事实证据", foundation_fact: "统一事实", foundation_fact_evidence: "事实证据关系",
  foundation_fact_review: "事实审核", foundation_security_classification: "证券分类",
  foundation_source_identity: "来源身份映射", foundation_company_mapping_state: "股票公司映射状态",
  classification_definition: "分类与标签定义",
};

const skillNames: Record<string, string> = {
  SECURITY_BOARD_IDENTITY_GOVERNOR: "证券与板块身份治理",
  STOCK_SOURCE_COLLECTION: "股票数据采集",
  STOCK_DATA_QUALITY_GATE: "股票数据质量门禁",
  LAKEHOUSE_KNOWLEDGE_PUBLISHER: "湖仓与知识发布",
  STOCK_GOVERNANCE_ACCEPTANCE: "股票治理验收",
};

const marketNames: Record<string, string> = { CN_A: "A股", HK: "港股", NEEQ: "新三板", NEEQ_INNOVATION: "新三板创新层" };
const exchangeNames: Record<string, string> = { SSE: "上交所", SZSE: "深交所", BSE: "北交所", HKEX: "港交所", NEEQ: "全国股转系统" };

const issueNames: Record<string, string> = {
  QUALITY_GATE_NOT_PASSED: "数据质量门禁未通过",
  LAKEHOUSE_EXPORT_INCOMPLETE: "湖仓数据集未全部发布",
  AGENT_GOVERNANCE_NOT_PASSED: "智能体审查未通过",
  GRAPH_PENDING_GOVERNANCE: "知识图谱待治理审核",
  CHUNK_SELECTION_TRUNCATED: "部分文档未纳入切片",
  CHUNK_COVERAGE_INCOMPLETE: "文档切片覆盖不完整",
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
  if (["SUCCESS", "COMPLETED", "SUCCEEDED", "PASSED", "PASS", "PUBLISHED", "GOVERNED", "LOCKED"].includes(key)) return "success";
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

function stageText(value?: string | null) {
  const key = String(value || "").toUpperCase();
  return stageNames[key] || stageNames[value || ""] || value || "--";
}

function issueText(code?: string) {
  const key = code || "";
  const suffix = key.split(":").at(-1) || key;
  return issueNames[key] || businessNames[suffix] || dataNames[suffix]
    || (suffix === "GRAPH_GOVERNANCE" ? "图谱治理审核" : key || "未分类");
}

function reasonText(reason?: string) {
  if (reason === "PENDING") return "知识图谱已构建，尚待治理审核";
  if (reason?.includes("'numeric_values_valid': False")) return "数值有效性检查未通过，湖仓发布被质量门禁拦截；需核对缺失值、单位或非法数值。";
  let text = reason || "未记录具体原因";
  for (const [code, label] of Object.entries(dataNames)) text = text.replaceAll(code, label);
  return text;
}

function stageFacts(code: string, report: GovernanceReport) {
  const stage = report.stage_results[code];
  if (!stage || stage.status === "SKIPPED") return stage?.message || "本次未执行";
  const runs = Array.isArray(stage.market_runs) ? stage.market_runs as Array<Record<string, unknown>> : [];
  if (code === "DOCUMENT_CHUNKS") return runs.map((run) => `文档覆盖 ${run.documents_with_chunks ?? 0}/${run.documents_total ?? 0} 份；新增 ${run.created_chunk_count ?? 0} 条切片；失败 ${run.failed_documents ?? 0} 份`).join("；");
  if (code === "KNOWLEDGE_GRAPH") return runs.map((run) => {
    const counts = (run.counts || {}) as Record<string, unknown>;
    const coverage = (counts.coverage || {}) as Record<string, unknown>;
    const sources = Array.isArray(coverage.configured_sources) ? coverage.configured_sources as string[] : [];
    const limited = Array.isArray(coverage.limited_sources) ? coverage.limited_sources as string[] : [];
    const governanceStatus = run.governance_status === "PENDING" ? "待治理审核" : statusText(String(run.governance_status || "UNKNOWN"));
    return `${counts.entities ?? counts.entities_created ?? 0} 个实体、${counts.relations ?? counts.relations_created ?? 0} 条关系；${governanceStatus}${sources.length ? `；配置范围：${sources.map((source) => dataNames[source] || source).join("、")}` : ""}${limited.length ? `；${limited.map((source) => dataNames[source] || source).join("、")}达到读取上限` : ""}`;
  }).join("；");
  return stage.message || "请展开成果明细查看";
}

function qualityText(value: unknown) {
  if (!value || typeof value !== "object" || Array.isArray(value)) return "--";
  const quality = value as Record<string, unknown>;
  const completion = quality.completion && typeof quality.completion === "object" && !Array.isArray(quality.completion)
    ? quality.completion as Record<string, unknown> : {};
  const issues = Array.isArray(completion.issues) ? completion.issues as Array<Record<string, unknown>> : [];
  const issueCodes = issues.map((item) => { const code = String(item.code || ""); return issueNames[code] || code; }).filter(Boolean).slice(0, 3).join("、");
  const status = statusText(String(quality.result_status || completion.status || "UNKNOWN"));
  return issueCodes ? `${status}；缺口：${issueCodes}` : status;
}

function operationsText(row: Record<string, unknown>, report: GovernanceReport) {
  if (report.stage_results.BUSINESS_DATA?.status === "SKIPPED") return "本次未执行；保留已有数据";
  const stages = row.stage_status as Record<string, unknown> | undefined;
  const operations = (stages?.operations || row.operations || {}) as Record<string, unknown>;
  const items = Object.entries(operations).map(([code, value]) => {
    const operationStatus = value && typeof value === "object" ? (value as Record<string, unknown>).status : value;
    return `${businessNames[code] || code}：${statusText(String(operationStatus || "UNKNOWN"))}`;
  });
  return items.join("；") || "尚无执行结果";
}

function interpretationText(report: GovernanceReport) {
  const summary = reportSummary(report);
  const lifecycle = String(report.job.job_status || report.job.status || "").toUpperCase();
  if (["PENDING", "RETRY"].includes(lifecycle)) return "任务已入队，等待后台执行；当前没有新的治理成果。";
  if (lifecycle === "RUNNING") return `任务正在执行，流程进度 ${report.job.progress ?? 0}%，阶段与成果将自动更新。`;
  if (lifecycle === "FAILED") return "任务执行失败，请先查看失败原因；已有成功阶段的成果保留。";
  if (summary.completed < summary.stocks) return `流程已结束，完全通过 ${summary.completed}/${summary.stocks} 只股票；仍有数据或治理缺口。流程进度 100% 不代表数据全部完成。`;
  return "本次请求的阶段已完成，请继续核验证据、数据新鲜度和图谱覆盖范围。";
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
  const completed = stocks.filter((row) => ["COMPLETED", "SUCCESS", "PASS", "PASSED", "SUCCEEDED"].includes(String(row.status || "").toUpperCase())).length;
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
  const [actionNotice, setActionNotice] = useState("");
  const [actionJobId, setActionJobId] = useState<number | null>(null);
  const [actionQueuedAt, setActionQueuedAt] = useState(0);
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
  useEffect(() => {
    if (!records.some((record) => ["PENDING", "RUNNING", "RETRY"].includes(String(record.status).toUpperCase()))) return;
    const timer = window.setInterval(() => { void load(); }, 5000);
    return () => window.clearInterval(timer);
  }, [load, records]);
  useEffect(() => {
    if (!selected || !["PENDING", "RUNNING", "RETRY"].includes(String(selected.job_status || selected.status).toUpperCase())) return;
    let cancelled = false;
    const timer = window.setInterval(() => {
      void batchGovernanceApi.report(selected.job_id).then((updated) => {
        if (!cancelled) setSelected((current) => current?.job_id === updated.job_id ? updated : current);
      }).catch((cause) => { if (!cancelled) setError(cause instanceof Error ? cause.message : "报告刷新失败"); });
    }, 5000);
    return () => { cancelled = true; window.clearInterval(timer); };
  }, [selected]);
  useEffect(() => {
    const record = records.find((item) => item.job_id === actionJobId);
    if (!record) return;
    const lifecycle = String(record.job_status || record.status).toUpperCase();
    if (["PENDING", "RETRY"].includes(lifecycle) && Date.now() - actionQueuedAt >= 15000) setActionNotice(`任务 #${record.job_id} 仍在等待后台执行，尚未开始采集。请检查治理执行服务是否已启动；页面会继续更新状态。`);
    if (lifecycle === "RUNNING") setActionNotice(`任务 #${record.job_id} 正在执行，进度 ${record.progress ?? 0}%。`);
    if (lifecycle === "COMPLETED") setActionNotice(`任务 #${record.job_id} 流程已结束，结果为“${statusText(record.result_status || record.status)}”，请查看报告与成果。`);
    if (lifecycle === "FAILED") setActionNotice(`任务 #${record.job_id} 执行失败，请查看报告中的失败原因。`);
  }, [records, actionJobId, actionQueuedAt]);

  async function openRecord(record: GovernanceRecord) {
    setError("");
    try { setSelected(await batchGovernanceApi.report(record.job_id)); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "治理报告加载失败"); }
  }

  async function startRecord(record: GovernanceRecord) {
    setStartingId(record.job_id);
    setError("");
    setActionNotice("");
    try {
      await batchGovernanceApi.start(record.job_id);
      setActionJobId(record.job_id);
      setActionQueuedAt(Date.now());
      setActionNotice(`任务 #${record.job_id} 已提交后台队列，等待执行。页面每 5 秒自动更新状态。`);
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
      .gr-notice,.gr-interpretation{padding:12px 14px;border:1px solid #c9dbce;border-radius:9px;background:#f1f8f2;font-size:13px;line-height:1.7}.gr-detail-table{overflow-wrap:anywhere}.gr-section>p{margin:0;font-size:12px;color:#68796f;line-height:1.6}.gr-link:disabled{opacity:.5;cursor:wait}.gr-modal-head small{display:block;margin-top:6px;color:#708077}.gr-table-wrap .gr-detail-table{min-width:850px}.gr-modal .gr-section ul{padding-left:22px;margin:0;font-size:12px;line-height:1.8}
      .gr-stage-table{table-layout:fixed}.gr-stage-table th:first-child{width:15%}.gr-stage-table th:nth-child(2){width:9%}.gr-stage-table th:nth-child(3),.gr-stage-table th:nth-child(4){width:8%}.gr-stage-table th:nth-child(5){width:9%}.gr-stage-table td:nth-child(5){white-space:nowrap}.gr-stage-table td{font-size:12px;line-height:1.6}.gr-section details{font-size:12px;line-height:1.7}.gr-section summary{cursor:pointer;padding:4px 0}
    `}</style>
    <header className="gr-head"><div><p className="eyebrow">GOVERNANCE HISTORY</p><h1>治理记录</h1><p>查询股票数据采集、湖仓发布、知识库切片、知识图谱和 AI + Agent + Skill 执行历史；打开记录可查看治理报告与成果。</p></div><div className="gr-actions"><button className="gr-button primary" type="button" onClick={() => onStartTask?.()}><Play size={14} />发起治理任务</button><button className="gr-button" type="button" disabled={loading} onClick={() => void load()}><RefreshCw size={14} />刷新</button></div></header>
    {error && <div className="gr-error">{error}</div>}
    {actionNotice && <div className="gr-notice" role="status">{actionNotice}</div>}
    <section className="gr-panel"><div className="gr-toolbar"><label className="gr-search"><Search size={14} /><input className="gr-input" value={keyword} onChange={(event) => { setKeyword(event.target.value); setOffset(0); }} placeholder="搜索股票、批次号或市场" /></label><select className="gr-select" value={status} onChange={(event) => { setStatus(event.target.value); setOffset(0); }}><option value="">全部状态</option><option value="PENDING">等待执行</option><option value="RUNNING">执行中</option><option value="COMPLETED">已完成</option><option value="PARTIAL">部分完成</option><option value="FAILED">执行失败</option></select><select className="gr-select" value={mode} onChange={(event) => { setMode(event.target.value as GovernanceMode | ""); setOffset(0); }}><option value="">全部治理方式</option><option value="AI_AGENT_SKILL_GOVERNANCE">AI + Agent + Skill 治理</option><option value="SYSTEM_GOVERNANCE">系统治理</option></select><button className="gr-button primary" type="button" onClick={() => { setOffset(0); void load(); }}>查询</button></div>
      <div className="gr-table-wrap"><table className="gr-table">
        <thead><tr><th>治理批次</th><th>股票范围</th><th>治理方式</th><th>状态</th><th>进度</th><th>创建时间</th><th>操作</th></tr></thead>
        <tbody>{records.length ? records.map((record) => {
          const displayStatus = record.result_status || record.status;
          const lifecycle = String(record.job_status || record.status).toUpperCase();
          const canStart = ["PENDING", "RETRY", "FAILED"].includes(lifecycle);
          return <tr key={record.job_id}>
            <td><strong>任务 #{record.job_id}</strong><span className="gr-code">{record.governance_batch_id || "--"}</span><small>Pipeline #{record.pipeline_run_id || "--"}</small></td>
            <td><strong>{record.stock_count} 只股票</strong><small>{record.effective_options?.stocks ? record.effective_options.stocks.slice(0, 3).map((item) => `${marketNames[item.market] || item.market}:${item.symbol}`).join("、") : "范围记录见详情"}</small></td>
            <td>{modeNames[record.governance_mode || ""] || record.governance_mode || "--"}<small>Agent 执行：{record.agent_execution_run_id ? "已记录" : "未记录"}</small></td>
            <td><span className={`gr-status ${statusClass(displayStatus)}`}>{statusText(displayStatus)}</span><small>{stageText(record.current_stage)}</small></td>
            <td>{record.progress ?? 0}%</td><td>{dateText(record.created_at)}</td>
            <td><div className="gr-actions">
              {canStart && <button className="gr-link" type="button" disabled={startingId === record.job_id} onClick={() => void startRecord(record)}><Play size={13} /> {startingId === record.job_id ? "启动中" : "立即执行"}</button>}
              {["PARTIAL", "FAILED"].includes(String(displayStatus).toUpperCase()) && <button className="gr-link" type="button" onClick={() => void batchGovernanceApi.retry(record.job_id).then(load).catch((cause) => setError(cause instanceof Error ? cause.message : "重试失败"))}>重试</button>}
              <button className="gr-link" type="button" onClick={() => void openRecord(record)}><FileText size={13} /> 查看报告与成果</button>
            </div></td>
          </tr>;
        }) : <tr><td className="gr-empty" colSpan={7}>{loading ? "正在加载治理记录…" : "暂无治理记录"}</td></tr>}</tbody>
      </table></div>
      <footer className="gr-footer"><span>共 {total} 条记录{loading ? " · 加载中…" : ""}</span><span className="gr-page"><button className="gr-button" type="button" disabled={offset === 0 || loading} onClick={() => setOffset(Math.max(0, offset - pageSize))}>上一页</button><span>第 {currentPage} / {totalPages} 页</span><button className="gr-button" type="button" disabled={offset + pageSize >= total || loading} onClick={() => setOffset(offset + pageSize)}>下一页</button></span></footer>
    </section>
    {selected && selectedReport && <div className="gr-modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setSelected(null); }}>
      <section className="gr-modal" role="dialog" aria-modal="true" aria-labelledby="governance-report-title">
        <header className="gr-modal-head">
          <div>
            <h2 id="governance-report-title">治理报告 · 任务 #{selected.job_id}</h2>
            <p>{modeNames[selected.governance_mode || ""] || selected.governance_mode} · {selected.governance_batch_id || "无批次号"} · {dateText(selected.created_at)}</p>
            <small>流程进度 {selectedReport.job.progress ?? 0}% · {stageText(selectedReport.job.current_stage)}</small>
          </div>
          <button className="gr-close" type="button" aria-label="关闭治理报告" onClick={() => setSelected(null)}><X size={18} /></button>
        </header>
        <div className="gr-modal-body">
          <div className="gr-summary">
            <div><small>股票数</small><strong>{summary.stocks}</strong></div>
            <div><small>完全通过股票</small><strong>{summary.completed}</strong></div>
            <div><small>{selectedReport.summary?.skill_execution_scope === "BATCH_HISTORY" ? "批次历史 Skill 执行" : "本任务 Skill 执行"}</small><strong>{summary.skills}</strong></div>
            <div><small>错误/缺口记录</small><strong>{summary.errors}</strong></div>
          </div>
          <div className="gr-interpretation"><strong>报告解读：</strong>{interpretationText(selectedReport)}</div>
          <section className="gr-section">
            <h3>阶段进度</h3>
            <p>统计单位为各阶段的验收检查项，分母因阶段而异；未请求的阶段不计算完成率。切片通过不代表所有类型的股票信息齐全。</p>
            <div className="gr-table-wrap"><table className="gr-detail-table gr-stage-table">
              <thead><tr><th>阶段</th><th>状态</th><th>通过检查项</th><th>未通过检查项</th><th>完成率</th><th>成果与范围</th></tr></thead>
              <tbody>{Object.entries(selectedReport.summary?.stage_stats || {}).map(([code, item]) => <tr key={code}>
                <td>{item.label || stageText(code)}</td>
                <td>{statusText(item.status || "UNKNOWN")}</td>
                <td>{item.completed ?? 0}</td><td>{item.incomplete ?? 0}</td>
                <td>{item.completion_rate == null ? "不适用" : `${item.completion_rate}%`}</td>
                <td>{stageFacts(code, selectedReport)}</td>
              </tr>)}</tbody>
            </table></div>
          </section>
          <section className="gr-section">
            <h3>阶段成果明细</h3>
            {["BUSINESS_DATA", "LAKEHOUSE_EXPORT", "DOCUMENT_CHUNKS", "KNOWLEDGE_GRAPH"].map((code) => {
              const stage = selectedReport.stage_results[code];
              if (!stage || stage.status === "SKIPPED") return null;
              const items = [...(Array.isArray(stage.completed_items) ? stage.completed_items : []), ...(Array.isArray(stage.incomplete_items) ? stage.incomplete_items : [])] as Array<Record<string, unknown>>;
              if (!items.length) return null;
              return <details key={code}>
                <summary>{stageText(code)} · {items.length} 个检查项</summary>
                <div className="gr-table-wrap"><table className="gr-detail-table">
                  <thead><tr><th>检查内容</th><th>状态</th><th>记录量</th><th>说明</th></tr></thead>
                  <tbody>{items.map((item, index) => <tr key={`${String(item.code)}-${index}`}>
                    <td>{dataNames[String(item.label)] || String(item.label || item.code || "--")}</td>
                    <td>{String(item.code).endsWith(":GRAPH_GOVERNANCE") && item.status === "PENDING" ? "待治理审核" : statusText(String(item.status || "UNKNOWN"))}</td><td>{item.record_count == null ? "--" : String(item.record_count)}</td>
                    <td>{reasonText(String(item.reason || ""))}</td>
                  </tr>)}</tbody>
                </table></div>
              </details>;
            })}
          </section>
          <section className="gr-section">
            <h3>股票基本信息与治理成果</h3>
            <div className="gr-table-wrap"><table className="gr-detail-table">
              <thead><tr><th>市场</th><th>代码</th><th>股票名称</th><th>交易所/板块</th><th>治理状态</th><th>业务数据</th><th>质量摘要</th><th>证据数</th></tr></thead>
              <tbody>{selectedReport.stock_results.map((row, index) => <tr key={`${String(row.market)}-${String(row.symbol)}-${index}`}>
                <td>{marketNames[String(row.market)] || String(row.market || "--")}</td><td>{String(row.symbol || "--")}</td><td>{String(row.stock_name || "--")}</td>
                <td>{exchangeNames[String(row.exchange)] || String(row.exchange || "--")} / {String(row.listing_board || "--")}</td>
                <td><span className={`gr-status ${statusClass(String(row.status || ""))}`}>{statusText(String(row.status || ""))}</span></td>
                <td>{operationsText(row, selectedReport)}</td><td>{qualityText(row.quality_summary)}</td><td>{Array.isArray(row.evidence_ids) ? row.evidence_ids.length : 0}</td>
              </tr>)}</tbody>
            </table></div>
          </section>
          <section className="gr-section">
            <h3>失败原因与优化建议</h3>
            <p>缺口记录可能来自同一原因在多个阶段的反馈，数量不等于独立故障数。来源无记录也不能据此判断市场不存在该数据。</p>
            {selectedReport.analysis?.failure_reasons?.length ? <ul>{selectedReport.analysis.failure_reasons.map((item, index) => <li key={`${item.code}-${index}`}>
              {issueText(item.code)}：{reasonText(item.reason)}
            </li>)}</ul> : <p>当前没有记录失败原因。</p>}
            {selectedReport.analysis?.recommendations?.length ? <ul>{selectedReport.analysis.recommendations.map((item, index) => <li key={index}>{item}</li>)}</ul> : null}
            <div className="gr-actions">
              <button className="gr-button" type="button" onClick={() => { setSelected(null); onStartTask?.(); }}>继续治理</button>
              <button className="gr-button" type="button" onClick={() => { setSelected(null); onOptimizeSkill?.(); }}>进入 Skill 优化审核</button>
            </div>
          </section>
          <section className="gr-section">
            <h3>Agent / Skill 执行链路</h3>
            <p>{selectedReport.agent ? `${selectedReport.agent.agent_code === "STOCK_DATA_GOVERNANCE_AGENT" ? "股票数据治理智能体" : String(selectedReport.agent.agent_code || "Agent")} · v${String(selectedReport.agent.agent_version || "--")} · ${statusText(String(selectedReport.agent.status || ""))}` : "未关联 Agent 执行记录"}</p>
            {selectedReport.skills?.length ? <div className="gr-table-wrap"><table className="gr-detail-table">
              <thead><tr><th>Skill</th><th>版本</th><th>状态</th><th>副作用级别</th><th>证据数</th></tr></thead>
              <tbody>{selectedReport.skills.map((skill, index) => <tr key={`${String(skill.skill_code)}-${index}`}>
                <td title={String(skill.skill_code || "")}>{skillNames[String(skill.skill_code)] || String(skill.skill_code || "--")}</td><td>{String(skill.skill_version || "--")}</td><td>{statusText(String(skill.status || ""))}</td><td>{({ READ_ONLY: "只读", DATABASE_WRITE: "受控数据库写入", DB_WRITE: "受控数据库写入", OBJECT_WRITE: "对象写入", EXTERNAL_WRITE: "外部写入" } as Record<string, string>)[String(skill.side_effect_level)] || String(skill.side_effect_level || "--")}</td><td>{Array.isArray(skill.evidence_ids) ? skill.evidence_ids.length : 0}</td>
              </tr>)}</tbody>
            </table></div> : <p>未记录 Skill 执行明细。</p>}
          </section>
          <section className="gr-section">
            <h3>原始审计数据</h3>
            <details><summary>展开阶段原始记录</summary><pre className="gr-pre">{jsonText(selectedReport.stage_results)}</pre></details>
            {selectedReport.errors?.length ? <details><summary>展开错误原文</summary><pre className="gr-pre">{jsonText(selectedReport.errors)}</pre></details> : null}
          </section>
        </div>
      </section>
    </div>}
  </section>;
}

export default GovernanceRecords;
