import { useEffect, useRef, useState } from "react";
import { SelectionWorkbench as AdvancedWorkbench } from "./SelectionWatch";
import { selectionApi, type BatchAcceptance, type SelectionBatchAcceptance, type SelectionCandidate, type SelectionRecord, type SelectionRun, type SelectionTracking } from "./selectionApi";
import "./SelectionWatch.css";
import { SelectionAnalysis, type AnalysisDimension } from "./SelectionAnalysis";

const message = (error: unknown) => error instanceof Error ? error.message : "请求失败，请重试";
const number = (value: number | null | undefined) => value == null ? "—" : value.toFixed(2);
const text = (value: unknown) => typeof value === "string" || typeof value === "number" ? String(value) : "";
const rows = (value: unknown): SelectionRecord[] => Array.isArray(value) ? value.filter(item => item && typeof item === "object") : [];
const timestamp = (value: string) => new Date(/(?:Z|[+-]\d{2}:?\d{2})$/.test(value) ? value : `${value}Z`).toLocaleString("zh-CN", { timeZone: "Asia/Shanghai" });
const sourceUrl = (value: unknown) => { try { const url = new URL(text(value)); return ["http:", "https:"].includes(url.protocol) ? url.href : null; } catch { return null; } };
const labels: Record<string, string> = {
  REVIEW: "待复核", TRACKING: "跟踪中", COMPLETED: "已完成", PARTIAL: "部分完成", APPROVED: "已确认",
  REJECTED: "已排除", PENDING: "待复核", PENDING_DATA: "等待行情", FAILED: "执行失败", QUEUED: "排队中",
  CANCEL_REQUESTED: "正在停止", CANCELLED: "已取消", SCREENING: "筛选股票与行情", EVIDENCE: "整理知识证据", MODEL: "模型分析与校验", DATA_BACKFILL: "补齐数据并更新知识图谱", RETRY_QUEUED: "失败项重新分析",
  selection_analysis: "选股分析任务", selection_retry_failed: "失败项重试任务", selection_data_backfill: "数据补齐任务",
  RUNNING: "执行中", WATCH: "值得关注", PASS: "暂不关注", SUCCESS: "已完成", NOT_CALLED: "未调用模型",
  PENDING_MODEL_CALL: "等待模型", SOURCE: "来源记录", OBSERVED: "来源观测", CANDIDATE: "候选待核实",
  ACCEPTED: "已审核", VERIFIED: "已验证", MISSING: "未生成", HASH_ONLY: "哈希向量", AVAILABLE: "语义向量可用",
  stock_news: "新闻", stock_notice: "公告", stock_symbol: "股票主数据", stock_kline: "日线行情",
  stock_financial_report: "财报", stock_realtime_quote: "行情快照", stock_f10_cache: "F10资料",
  trade_date: "交易日期", report_period: "报告期", indicator: "指标", value: "数值", unit: "单位",
  close_price: "收盘价", open_price: "开盘价", high_price: "最高价", low_price: "最低价", volume: "成交量",
  amount: "成交额", change_pct: "涨跌幅（%）", turnover: "换手率", current_price: "现价", revenue: "营业收入",
  net_profit: "净利润", eps: "每股收益", pe: "市盈率", pb: "市净率", currency: "币种", observed_at: "观测时间",
  quote_time: "行情时间", previous_close_price: "昨收价", change_amount: "涨跌额", turnover_rate: "换手率（来源原值）",
  period: "周期", daily: "日线", adjust: "复权方式", window_size: "行情窗口（交易日）", CNY: "人民币",
  ISSUED_BY: "发行主体", HAS_NEWS: "相关新闻", HAS_NOTICE: "相关公告", HAS_FINANCIAL_REPORT: "财务报告",
  HAS_PRICE_AND_VOLUME_SERIES: "量价序列", HAS_FINANCIAL_METRIC: "财务指标", HAS_F10_SECTION: "F10资料",
  HAS_REALTIME_QUOTE: "行情快照", SUPPORTED_BY: "证据支持", EVIDENCED_BY: "证据来源", DESCRIBED_BY: "资料描述",
};
const label = (value: string) => labels[value] || value;
const isRunning = (run: SelectionRun | null) => !!run && ["QUEUED", "RUNNING", "CANCEL_REQUESTED"].includes(run.status);
const financialFields: Array<[string, string, "money" | "yuan" | "percent"]> = [
  ["TOTALOPERATEREVE", "营业收入", "money"], ["PARENTNETPROFIT", "归母净利润", "money"],
  ["KCFJCXSYJLR", "扣非净利润", "money"], ["MLR", "毛利", "money"],
  ["EPSJB", "基本每股收益", "yuan"], ["EPSKCJB", "扣非每股收益", "yuan"],
  ["BPS", "每股净资产", "yuan"], ["MGJYXJJE", "每股经营现金流", "yuan"],
  ["TOTALOPERATEREVETZ", "营收同比", "percent"], ["PARENTNETPROFITTZ", "归母净利润同比", "percent"],
  ["ROEJQ", "加权净资产收益率", "percent"], ["XSMLL", "毛利率", "percent"], ["XSJLL", "净利率", "percent"],
];

function Fact({ fact }: { fact: SelectionRecord }) {
  const properties = fact.properties || {};
  const financial = properties.data || {};
  const scalar = { ...properties, ...(properties.latest || {}) };
  const values = Object.entries(scalar).filter(([key, value]) => key !== "fact_only" && key !== "url" && value != null && typeof value !== "object");
  const metrics = financialFields.filter(([key]) => typeof financial[key] === "number");
  const holders = rows(properties.payload?.major).slice(0, 5);
  return <article className="batch-fact"><strong>{text(fact.title || fact.fact_type)}</strong><small>{label(text(fact.source_table))} · {label(text(fact.status))}</small>
    <dl>{values.slice(0, 12).map(([key, value]) => <div key={key}><dt>{label(key)}</dt><dd>{typeof value === "number" ? value.toLocaleString("zh-CN", { maximumFractionDigits: 2 }) : label(text(value))}</dd></div>)}
      {metrics.map(([key, name, unit]) => <div key={key}><dt>{name}</dt><dd>{(financial[key] / (unit === "money" ? 1e8 : 1)).toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}{unit === "percent" ? "%" : unit === "money" ? (properties.currency === "CNY" ? " 亿元" : " 亿（原币）") : (properties.currency === "CNY" ? " 元" : "（原币）")}</dd></div>)}</dl>
    {!!holders.length && <><small>主要股东（前5名，持股比例为来源披露值）</small><dl>{holders.map((holder, index) => <div key={index}><dt>{text(holder["股东名称"])}</dt><dd>{typeof holder["持股比例"] === "number" ? `${number(holder["持股比例"])}%` : "未披露"}</dd></div>)}</dl></>}
    {fact.fact_type === "FINANCIAL_OBSERVATION" && !metrics.length && !values.some(([key]) => key === "value") && <small>该来源未提供可识别的标准数值，请展开原文核验。</small>}
  </article>;
}

function SourceDocument({ doc, candidateId, cited, selected = false }: { doc: SelectionRecord; candidateId: number; cited: boolean; selected?: boolean }) {
  const [full, setFull] = useState<SelectionRecord | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  async function load() {
    if (loading || full) return;
    setLoading(true); setError("");
    try { setFull(await selectionApi.candidateDocument(candidateId, Number(doc.id))); }
    catch (err) { setError(message(err)); } finally { setLoading(false); }
  }
  const metadata = full?.metadata_json || doc.metadata || {};
  const url = sourceUrl(metadata.url || doc.url);
  return <details open={selected || undefined} className={cited ? "batch-source cited" : "batch-source"} onToggle={event => { if (event.currentTarget.open) void load(); }}>
    <summary>{cited && <span className="batch-badge">模型引用</span>} {text(doc.title) || `证据文档 #${doc.id}`}</summary>
    <small>{label(text(full?.source_table || doc.source_table))} · 文档 #{doc.id} · {text(doc.published_at || metadata.published_at) || "来源未提供发布时间"}</small>
    {loading && <p role="status">正在读取已存原文…</p>}
    {error && <p className="batch-alert error">原文读取失败：{error} <button onClick={() => void load()}>重试</button></p>}
    <p className="batch-source-content">{text(full?.content || doc.content || doc.excerpt) || "暂无正文"}</p>
    <p className="batch-note">{full?.content_state === "GRAPH_REFERENCE_ONLY" ? "本轮保存的图谱关系证据摘录；未保存完整原文。" : full?.content_state === "CHUNK_ONLY" ? "本轮保存的文档切片；未保存完整原文。" : full ? (full.content_truncated ? "已存来源内容有截断，请结合原文链接核验。" : "展示数据库中保存的来源内容。") : "当前为证据快照摘录。"}</p>
    {url && <a href={url} target="_blank" rel="noreferrer">打开来源链接</a>}
  </details>;
}

export function StockBatchWorkbench({ mode }: { mode: "selection" | "tracking" }) {
  const [acceptance, setAcceptance] = useState<BatchAcceptance | null>(null);
  const [runs, setRuns] = useState<SelectionRun[]>([]);
  const [run, setRun] = useState<SelectionRun | null>(null);
  const [tracking, setTracking] = useState<SelectionTracking[]>([]);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [advanced, setAdvanced] = useState(false);
  const [newSelection, setNewSelection] = useState(false);
  const [showData, setShowData] = useState(false);
  const [batchAcceptance, setBatchAcceptance] = useState<SelectionBatchAcceptance | null>(null);
  const [acceptanceBusy, setAcceptanceBusy] = useState(false);
  const [clock, setClock] = useState(Date.now());
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [detail, setDetail] = useState<SelectionCandidate | null>(null);
  const [detailTab, setDetailTab] = useState("conclusion");
  const [focusDocument, setFocusDocument] = useState<number | null>(null);
  const [targetPrice, setTargetPrice] = useState("");
  const [resultPage, setResultPage] = useState(1);
  const dialogRef = useRef<HTMLElement>(null);
  const retryProgress = (run?.criteria_json.retry_progress || {}) as SelectionRecord;
  const retryActive = ["QUEUED", "RUNNING"].includes(text(retryProgress.status));
  const backfillProgress = (run?.criteria_json.backfill_progress || {}) as SelectionRecord;
  const backfillActive = ["QUEUED", "RUNNING", "CANCEL_REQUESTED"].includes(text(backfillProgress.status));
  const running = isRunning(run) || retryActive || backfillActive;
  useEffect(() => {
    if (!running) return;
    const timer = window.setInterval(() => setClock(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [running]);

  useEffect(() => {
    let valid = true;
    void Promise.all([selectionApi.acceptance(), selectionApi.listRuns(), selectionApi.listTracking()])
      .then(async ([data, nextRuns, nextTracking]) => {
        if (!valid) return;
        setAcceptance(data); setRuns(nextRuns); setTracking(nextTracking);
        const latest = nextRuns[0];
        if (latest) { const full = await selectionApi.getRun(latest.id); if (valid) setRun(full); }
      }).catch(err => { if (valid) setError(message(err)); }).finally(() => { if (valid) setLoading(false); });
    return () => { valid = false; };
  }, []);

  useEffect(() => {
    if (!running || !run) return;
    let valid = true;
    let timer: ReturnType<typeof setTimeout>;
    const poll = async () => {
      try {
        const next = await selectionApi.getRun(run.id);
        if (!valid) return;
        setRun(next);
        const nextRetry = (next.criteria_json.retry_progress || {}) as SelectionRecord;
        const nextBackfill = (next.criteria_json.backfill_progress || {}) as SelectionRecord;
        const rerunId = Number(nextBackfill.rerun_id || 0);
        if (rerunId > 0 && !["QUEUED", "RUNNING"].includes(text(nextBackfill.status))) {
          const rerun = await selectionApi.getRun(rerunId);
          if (!valid) return;
          setRun(rerun);
          setRuns(await selectionApi.listRuns());
          setNotice(`缺失数据补齐完成，已自动进入新分析批次 #${rerunId}；原批次 #${next.id} 保留。`);
          return;
        }
        if (isRunning(next) || ["QUEUED", "RUNNING"].includes(text(nextRetry.status)) || ["QUEUED", "RUNNING"].includes(text(nextBackfill.status))) timer = setTimeout(() => void poll(), 2000);
        else { setRuns(await selectionApi.listRuns()); setNotice(next.analysis_mode === "MODEL_COMPLETED" ? `真实模型已完成${next.candidate_count}只股票的分析，请核对证据。` : next.analysis_mode === "RULE_ONLY" ? "候选池已生成，可查看规则与证据。" : "执行已结束，失败项与待核对项请查看下方明细。"); }
      } catch (err) { if (valid) { setError(`进度读取失败，将重试：${message(err)}`); timer = setTimeout(() => void poll(), 4000); } }
    };
    timer = setTimeout(() => void poll(), 1000);
    return () => { valid = false; clearTimeout(timer); };
  }, [running, run?.id]);

  useEffect(() => {
    if (!detail) return;
    const close = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !busy) setDetail(null);
      if (event.key !== "Tab") return;
      const elements = Array.from(dialogRef.current?.querySelectorAll<HTMLElement>('button:not(:disabled), input:not(:disabled), a[href], summary') || []).filter(item => item.getClientRects().length);
      const first = elements[0], last = elements[elements.length - 1];
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
    };
    window.addEventListener("keydown", close);
    return () => window.removeEventListener("keydown", close);
  }, [!!detail, busy]);

  useEffect(() => {
    if (!detail) return;
    const previousFocus = document.activeElement as HTMLElement | null;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    dialogRef.current?.querySelector<HTMLButtonElement>("button")?.focus();
    return () => { document.body.style.overflow = previousOverflow; previousFocus?.focus(); };
  }, [detail?.id]);

  async function start() {
    if (!acceptance?.prepared || !acceptance.graph_id || !acceptance.knowledge_base_id) { setError("20股数据门禁尚未通过，请查看验收明细。"); return; }
    setBusy(true); setError(""); setNotice("");
    try {
      const created = await selectionApi.startRun({ market: acceptance.market, symbols: acceptance.symbols,
        universe_scope: "FIXED20_ACCEPTANCE", candidate_limit: 20, min_abs_change_pct: 0, min_volume_ratio: 0,
        period_months: 3,
        model_instance_code: null, knowledge_base_ids: [acceptance.knowledge_base_id], graph_ids: [acceptance.graph_id], use_model: true, data_mode: "REAL" });
      setRun(created); setRuns(current => [created, ...current]); setNotice("已提交后台，刷新页面后仍可查看执行进度。");
    } catch (err) { setError(message(err)); } finally { setBusy(false); }
  }
  async function loadBatchAcceptance() {
    if (!run) return;
    setAcceptanceBusy(true); setError("");
    try { setBatchAcceptance(await selectionApi.batchAcceptance(run.id)); }
    catch (err) { setError(message(err)); }
    finally { setAcceptanceBusy(false); }
  }
  async function cancelRun() {
    if (!run || !running) return;
    setBusy(true); setError("");
    try { setRun(await selectionApi.cancelRun(run.id)); setNotice("已请求取消。模型网络请求正在途中时无法强制中断，返回后会停止后续步骤；未完成的本次分析结果不提交，历史数据和批次保持不变。"); }
    catch (err) { setError(message(err)); }
    finally { setBusy(false); }
  }

  async function review(decision: "APPROVED" | "REJECTED") {
    if (!detail) return;
    setBusy(true); setError("");
    try {
      await selectionApi.review(detail.id, { decision, target_price: decision === "APPROVED" ? Number(targetPrice) : undefined });
      setRun(await selectionApi.getRun(detail.run_id)); setTracking(await selectionApi.listTracking()); setDetail(null);
      setNotice(decision === "APPROVED" ? "已保存决策与证据快照，开始跟踪确认之后的有效交易日。" : "已排除并保存审核记录。");
    } catch (err) { setError(message(err)); } finally { setBusy(false); }
  }

  async function retryAnalysis() {
    if (!run || running) return;
    setBusy(true); setError(""); setNotice("");
    try {
      const next = run.status === "FAILED" ? await selectionApi.retryRun(run.id) : await selectionApi.retryFailed(run.id);
      setRun(next); setRuns(await selectionApi.listRuns());
      setNotice(run.status === "FAILED" ? `已创建重试批次 #${next.id}，原批次 #${run.id} 保留。` : "已提交失败股票重试，保留成功结果及原有证据快照。");
    } catch (err) { setError(message(err)); } finally { setBusy(false); }
  }

  async function backfill(candidateIds: number[] = [], dimensions: AnalysisDimension[] = []) {
    if (!run || running) return;
    setBusy(true); setError(""); setNotice("");
    try {
      const next = await selectionApi.backfill(run.id, { candidate_ids: candidateIds,
        dimensions,
        period_months: Number(run.criteria_json.period_months || 3), rerun_analysis: true });
      setRun(next);
      setNotice(candidateIds.length ? "已提交该股票的数据补齐，完成后自动创建新分析批次。" : "已提交本批次缺失数据补齐，完成后自动创建新分析批次。");
    } catch (err) { setError(message(err)); } finally { setBusy(false); }
  }

  async function refreshTracking() {
    setBusy(true); setError("");
    try { const result = await selectionApi.refresh(); setTracking(await selectionApi.listTracking()); setNotice(`刷新完成：${result.completed_count}只完成，${result.pending_data_count}只等待行情。`); }
    catch (err) { setError(message(err)); } finally { setBusy(false); }
  }

  const modelCount = run?.candidates.filter(item => item.analysis_json.model_output_status === "SUCCESS" && item.analysis_json.analysis_validation_status !== "PARTIAL").length || 0;
  const partialCount = run?.candidates.filter(item => item.analysis_json.analysis_validation_status === "PARTIAL").length || 0;
  const failedCandidates = run?.candidates.filter(item => item.decision === "PENDING" && item.analysis_json.model_output_status === "FAILED") || [];
  const unresolvedCandidates = run?.candidates.filter(item => item.decision === "PENDING" && (item.analysis_json.model_output_status === "FAILED" || item.analysis_json.analysis_validation_status === "PARTIAL")) || [];
  const progress = (run?.criteria_json.progress || {}) as SelectionRecord;
  const modelCall = (progress.model_call || {}) as SelectionRecord;
  const backend = (progress.backend || {}) as SelectionRecord;
  const runtimeProgress = backfillActive ? backfillProgress : progress;
  const progressStartedAt = Date.parse(text(runtimeProgress.started_at));
  const elapsedSeconds = Math.max(Number(runtimeProgress.elapsed_seconds || 0), Number.isFinite(progressStartedAt) ? Math.floor((clock - progressStartedAt) / 1000) : 0);
  const fixedRuns = runs;
  const pageSize = 10;
  const pageCount = Math.max(1, Math.ceil((run?.candidates.length || 0) / pageSize));
  const visibleCandidates = run?.candidates.slice((Math.min(resultPage, pageCount) - 1) * pageSize, Math.min(resultPage, pageCount) * pageSize) || [];
  const companyCount = run?.candidates.filter(item => item.evidence_json.identity?.company).length || 0;
  const documentCount = new Set(run?.candidates.flatMap(item => rows(item.evidence_json.documents).map(doc => doc.id)) || []).size;
  const runTracking = tracking.filter(item => run?.candidates.some(candidate => candidate.id === item.candidate_id));
  const requestSymbols = (run?.criteria_json.request as SelectionRecord | undefined)?.symbols;
  const selectedCount = Math.max(run?.candidate_count || 0, Array.isArray(requestSymbols) ? requestSymbols.length : 0);
  useEffect(() => { setResultPage(1); }, [run?.id]);
  const evidence = detail?.evidence_json;
  const model = (detail?.analysis_json.model || {}) as SelectionRecord;
  const factTypes: Record<string, number> = {};
  const factList = evidence ? [...Object.values(evidence.structured_observations || {}).flatMap(rows), ...rows(evidence.facts)]
    .filter((item, index, all) => all.findIndex(other => other.id === item.id) === index)
    .filter(item => { const kind = text(item.fact_type); factTypes[kind] = (factTypes[kind] || 0) + 1; return factTypes[kind] <= 2; }) : [];
  const citedIds = new Set<number>(Array.isArray(model.evidence_document_ids) ? model.evidence_document_ids.map(Number) : []);
  const documentList = [...rows(evidence?.documents), ...rows(evidence?.evidence_context?.documents)]
    .filter((item, index, all) => all.findIndex(other => Number(other.id) === Number(item.id)) === index);
  citedIds.forEach(id => { if (!documentList.some(doc => Number(doc.id) === id)) documentList.push({ id }); });
  documentList.sort((a, b) => Number(citedIds.has(Number(b.id))) - Number(citedIds.has(Number(a.id))));

  if (advanced) return <><button className="batch-return" onClick={() => setAdvanced(false)}>← 返回选股工作台</button><AdvancedWorkbench mode={mode} initialCreate={newSelection} onCreated={created => {setRun(created); setRuns(current => [created, ...current]); setAdvanced(false); setNewSelection(false);}} /></>;
  return <main className={`batch-workbench mode-${mode}`}>
    <header className="batch-heading"><div><p className="eyebrow">多源证据 · 可追溯分析</p><h1>选股分析工作台</h1><p>选择股票与知识范围 → 后台分析 → 核对结论与原文</p></div><div className="batch-actions"><button onClick={() => {setNewSelection(false); setAdvanced(true);}}>分析配置与历史</button>
      {mode === "selection" ? <><button disabled={busy || loading || running || !acceptance?.prepared} onClick={() => void start()}>{busy ? "提交中…" : "验证20股样本"}</button><button className="primary-button" onClick={() => {setNewSelection(true); setAdvanced(true);}}>新建选股</button></> : <button className="primary-button" disabled={busy || loading} onClick={() => void refreshTracking()}>刷新跟踪</button>}</div></header>
    {error && <p className="batch-alert error" role="alert">{error}</p>}{notice && <p className="batch-alert" role="status">{notice}</p>}
    {mode === "selection" && <p className="batch-workbench-note">本页用于查看当前批次、候选结果、证据和验收；“分析配置与历史”用于创建批次、设置筛选条件及模型知识范围，并查看全部历史运行、跟踪和复盘。两处操作的是同一份批次数据。</p>}
    <section className="batch-summary" aria-label="全链路进度">
      <div><small>本批股票</small><strong>{selectedCount} 只</strong><small>公司已关联 {companyCount} 只</small></div>
      <div><small>本次载入证据</small><strong>{documentCount} 篇文档</strong><small>按证据文档编号去重</small></div>
      <div><small>已通过引用校验</small><strong>{modelCount} / {selectedCount}</strong><small>{run?.analysis_mode === "RULE_ONLY" ? "本批为规则候选，未调用模型" : "模型研判与来源引用"}</small></div>
      <div><small>需要处理</small><strong>{failedCandidates.length} 失败 · {partialCount} 待核对</strong><small>{running ? "正在后台执行" : "只重试未通过项"}</small></div>
      <div><small>本批确认后跟踪</small><strong>{runTracking.length} 只</strong><small>{runTracking.filter(item => item.status === "COMPLETED").length} 只已复盘</small></div>
    </section>
    <div className="batch-scope"><p className="batch-note">{run ? `当前批次 #${run.id} · ${Number(run.criteria_json.period_months || 3)} 个月（${text(run.criteria_json.period_start)} 至 ${text(run.criteria_json.period_end)}）· ${run.knowledge_base_ids_json.length} 个知识库 · ${run.graph_ids_json.length} 张图谱。` : "点击“新建选股”选择股票，或运行20股基准样本。"}本次分析使用已入库数据，行情日期逐股显示；引用校验通过不等于投资结论已经验证。</p><button onClick={() => setShowData(!showData)} aria-expanded={showData}>{showData ? "收起基准样本验收" : "查看20股基准样本验收"}</button></div>
    {showData && <section className="batch-card"><div className="batch-card-title"><h2>20股数据验收</h2><span>实际读取结果 · 图谱 #{acceptance?.graph_id}</span></div><div className="batch-table-wrap"><table><thead><tr><th>股票</th><th>公司映射</th><th>证据文档</th><th>载入切片</th><th>事实与观测</th><th>图谱路径</th><th>血缘</th><th>状态</th></tr></thead><tbody>{acceptance?.rows.map(item => <tr key={item.symbol}><td><strong>{item.name}</strong><small>{item.symbol}</small></td><td>{item.company_linked ? "已关联" : "缺失"}</td><td>{item.documents}</td><td>{item.selected_chunks}</td><td>{item.facts}</td><td>{item.relations}</td><td>{item.lineage}</td><td><span className={`batch-badge ${item.passed ? "" : "warning"}`}>{item.passed ? "链路通过" : "待处理"}</span></td></tr>)}</tbody></table></div><p className="batch-note">切片按上下文预算选取，未载入全部切片不等于切片失败。本轮复用已有采集数据，未重新采集全量远程数据。</p></section>}
    {run && <section className="batch-card batch-acceptance"><div className="batch-card-title"><div><h2>本批次数据验收</h2><span>按当前批次全部股票检查行情、证据、来源引用与模型状态</span></div><button type="button" disabled={acceptanceBusy} onClick={() => void loadBatchAcceptance()}>{acceptanceBusy ? "正在验收…" : batchAcceptance?.run_id === run.id ? "重新验收本批次" : "查看本批次验收"}</button></div>{batchAcceptance?.run_id === run.id && <><p className="batch-note">通过 {batchAcceptance.accepted_count} / {batchAcceptance.candidate_count} 只 · 待处理 {batchAcceptance.needs_attention_count} 只。{batchAcceptance.scope_note}</p><div className="batch-table-wrap"><table><thead><tr><th>股票</th><th>行情基准</th><th>文档 / 切片</th><th>事实 / 图谱关系</th><th>模型 / 引用</th><th>缺口 / 验收</th></tr></thead><tbody>{batchAcceptance.rows.map(item => <tr key={item.candidate_id}><td><strong>{item.name || item.symbol}</strong><small>{item.symbol}</small></td><td>{item.has_market_baseline ? "已具备" : "缺失"}</td><td>{item.documents} / {item.chunks}</td><td>{item.facts} / {item.graph_relations}</td><td>{item.model_status} / {item.citation_status}</td><td><span className={`batch-badge ${item.passed ? "" : "warning"}`}>{item.passed ? "通过" : "待处理"}</span><small>{item.data_gaps.slice(0, 3).join("；") || `血缘 ${item.lineage} 条`}</small></td></tr>)}</tbody></table></div></>}</section>}
    {loading && <p className="batch-empty" role="status">正在读取20股数据与历史分析…</p>}
    {!loading && !run && <p className="batch-empty">{acceptance?.prepared ? "数据准备已通过，请点击“新建选股”或“验证20股样本”。" : "数据准备尚未完成，请查看验收状态。"}</p>}
    {run && <section className="batch-card"><div className="batch-card-title"><div><h2>{running ? "正在分析" : "本轮结果"} <span className="batch-badge">{label(run.status)}</span></h2><span>分析时间 {timestamp(run.as_of)}（北京时间） · {run.model_instance_code || "自动模型路由"}</span></div><select aria-label="历史分析批次" disabled={busy} value={run.id} onChange={event => { setError(""); void selectionApi.getRun(Number(event.target.value)).then(setRun).catch(err => setError(message(err))); }}>{fixedRuns.map(item => <option key={item.id} value={item.id}>第{item.id}轮 · {label(item.status)}</option>)}</select></div>
      {(isRunning(run) || retryActive) && <div className="batch-progress" role="status"><div><strong>{text(progress.message) || "等待后台执行"}</strong><span>{Number(progress.percent || 0)}%</span></div><progress aria-label="分析进度" max="100" value={Number(progress.percent || 0)} /><small>可离开页面，任务会在后台继续执行。</small></div>}
      {backfillActive && <div className="batch-progress" role="status"><div><strong>{text(backfillProgress.message) || "正在补齐缺失数据"}</strong><span>{Number(backfillProgress.percent || 0)}%</span></div><progress aria-label="数据补齐进度" max="100" value={Number(backfillProgress.percent || 0)} /><small>逐股票、逐板块记录成功、空数据和失败原因；旧分析保持不变。</small></div>}
    {running && <section className="batch-runtime" aria-live="polite"><div><strong>{text(runtimeProgress.operation) || text(runtimeProgress.message) || "后台正在处理"}</strong><span>已用时 {Math.floor(elapsedSeconds / 60)} 分 {elapsedSeconds % 60} 秒</span></div><small>阶段：{backfillActive ? "补齐数据并更新知识图谱" : label(text(progress.stage) || "QUEUED")} · 工作线程：{backend.status ? label(text(backend.status)) : "等待领取任务"}{backend.task_type ? ` · ${label(text(backend.task_type))}` : ""}{backend.attempt ? ` · 第 ${Number(backend.attempt)} 次执行` : ""}{backend.heartbeat_age_seconds != null ? ` · ${Number(backend.heartbeat_age_seconds) > 90 ? "心跳已超过90秒，可能停滞" : `最近心跳 ${Number(backend.heartbeat_age_seconds)} 秒前`}` : ""}</small>{backfillActive && <small>已补齐 {Number(backfillProgress.completed_count || 0)} / {Number(backfillProgress.candidate_count || 0)} 只股票</small>}{!backfillActive && progress.counts && <small>已处理 {Number(progress.counts.completed_stocks || 0)} / {Number(progress.counts.total_stocks || 0)} 只股票{progress.counts.total_batches ? ` · 模型批次 ${Number(progress.counts.completed_batches || 0)} / ${Number(progress.counts.total_batches)} 完成` : ""}</small>}{!backfillActive && modelCall.call_log_id && <small>模型 {text(modelCall.instance_code)} 正在处理调用 #{Number(modelCall.call_log_id)}，已等待 {Number(modelCall.elapsed_seconds || 0)} 秒；系统正在等待响应。</small>}{run.status === "CANCEL_REQUESTED" && <small>已收到取消请求，当前网络请求结束后会停止；在此期间不会新发起模型重试。</small>}<button type="button" disabled={busy || run.status === "CANCEL_REQUESTED"} onClick={() => void cancelRun()}>{run.status === "CANCEL_REQUESTED" ? "正在停止…" : "取消本次任务"}</button></section>}
      {run.error_message && <p className="batch-alert error">{run.error_message}</p>}
      {(unresolvedCandidates.length > 0 || retryActive || run.status === "FAILED") && <section className="selection-recovery" aria-label="分析失败处理">
        <div className="selection-scope-heading"><strong>已通过引用校验 {modelCount} 只 · 待核对 {partialCount} 只 · 失败 {failedCandidates.length} 只</strong><button disabled={busy || running} onClick={() => void retryAnalysis()}>{retryActive ? "正在重试失败项…" : run.status === "FAILED" ? "重新执行此批次" : partialCount ? `重新分析 ${unresolvedCandidates.length} 只失败或待核对股票` : `重试 ${failedCandidates.length} 只失败股票`}</button></div>
        <p className="batch-note">逐股重试保留成功结果、本轮证据及历史尝试；整批异常会创建新批次。</p>
        {retryProgress.message && <p role="status">{text(retryProgress.message)}</p>}
        {failedCandidates.length > 0 && <details><summary>查看逐股失败原因与尝试记录（{failedCandidates.length}）</summary><div className="selection-failure-list">{failedCandidates.map(item => <article key={item.id}><strong>{item.name || item.symbol} · {item.symbol}</strong><p>{text(item.analysis_json.model_failure_reason) || run.error_message || "历史批次未记录逐股错误，请重试。"}</p>{rows(item.analysis_json.model_attempts).map((attempt, index) => <small key={index}>{attempt.at ? timestamp(text(attempt.at)) : ""} · {attempt.status === "SUCCESS" ? "调用完成" : "未通过"}{attempt.model_call_log_id ? ` · 调用 #${attempt.model_call_log_id}` : ""} · {text(attempt.reason) || "已返回结果并经过引用校验"}</small>)}</article>)}</div></details>}
      </section>}
      {run.missing_data_json.length > 0 && <details className="batch-missing"><summary>数据缺口与限制（{run.missing_data_json.length}）</summary><ul>{run.missing_data_json.map((item, index) => <li key={index}>{item}</li>)}</ul><button disabled={busy || running} onClick={() => void backfill()}>补齐全部缺失并重新分析</button></details>}
      <div className="batch-table-wrap"><table><thead><tr><th>股票</th><th>行情基准</th><th>模型判断</th><th>证据</th><th>确认状态</th><th>操作</th></tr></thead><tbody>{visibleCandidates.map(item => {
        const success = item.analysis_json.model_output_status === "SUCCESS";
        const partial = item.analysis_json.analysis_validation_status === "PARTIAL";
        const local = !!item.analysis_json.local_multi_source;
        return <tr key={item.id}><td><strong>{item.name || item.symbol}</strong><small>{item.symbol}</small></td><td>{number(item.entry_price)} 元<small>{item.entry_date || "缺少日期"}</small></td><td><span className={`batch-badge ${success && !partial ? "" : "warning"}`}>{partial ? "证据不足 · 待核对" : success ? label(text(item.analysis_json.recommendation)) : local ? "本地多源证据" : label(text(item.analysis_json.model_output_status))}</span><small className="batch-clamp">{success ? text((item.analysis_json.model as SelectionRecord)?.headline || item.analysis_json.summary) : local ? "已盘点新闻、公告、财务、量价、资金、公司与图谱证据；等待模型语义研判" : "尚无有效模型结论"}</small></td><td>{rows(item.evidence_json.documents).length} 篇文档<small>{rows(item.evidence_json.facts).length} 条事实与观测</small></td><td>{label(item.decision)}</td><td><button onClick={() => { setDetail(item); setFocusDocument(null); setDetailTab("conclusion"); setTargetPrice(text(item.analysis_json.target_price)); }}>分析与证据</button></td></tr>;
      })}</tbody></table>{!run.candidates.length && !running && <p className="batch-empty">本轮无可分析结果，请检查数据与失败原因。</p>}</div>{run.candidates.length > pageSize && <nav className="batch-pagination" aria-label="分析结果分页"><span>共 {run.candidates.length} 只 · 第 {Math.min(resultPage, pageCount)} / {pageCount} 页</span><button disabled={resultPage <= 1} onClick={() => setResultPage(page => page - 1)}>上一页</button><button disabled={resultPage >= pageCount} onClick={() => setResultPage(page => page + 1)}>下一页</button></nav>}</section>}
    {mode === "tracking" && <section className="batch-card"><div className="batch-card-title"><div><h2>跟踪与复盘</h2><span>从人工确认后的有效交易日开始，当前为10日规则。</span></div><button onClick={() => setAdvanced(true)}>查看详细复盘与Skill回归</button></div><div className="batch-table-wrap"><table><thead><tr><th>股票</th><th>目标 / 最新价</th><th>有效交易日</th><th>累计收益</th><th>最大回撤</th><th>状态</th></tr></thead><tbody>{tracking.map(item => <tr key={item.id}><td>{item.symbol}</td><td>{number(item.target_price)} / {number(item.latest_price)}</td><td>{item.observed_sessions} / {item.required_sessions}</td><td>{number(item.cumulative_return_pct)}%</td><td>{number(item.max_drawdown_pct)}%</td><td>{label(item.status)}</td></tr>)}</tbody></table>{!tracking.length && <p className="batch-empty">确认入选后自动建立跟踪。尚未发生的交易日不会生成收益结果。</p>}</div></section>}
    {detail && <div className="batch-overlay" onMouseDown={event => { if (event.target === event.currentTarget && !busy) setDetail(null); }}><section ref={dialogRef} className="batch-detail" role="dialog" aria-modal="true" aria-label={`${detail.name || detail.symbol}分析与证据`}>
      <header><div><h2>{detail.name || detail.symbol}</h2><span>{detail.symbol} · 行情 {detail.entry_date} · {number(detail.entry_price)} 元</span></div><button disabled={busy} onClick={() => setDetail(null)}>关闭</button></header>
      <nav className="batch-tabs" aria-label="研判详情分类">{[["conclusion", "分析结论"], ["facts", "数据事实"], ["documents", "原文与引用"], ["trace", "关系与血缘"]].map(([key, title]) => <button key={key} className={detailTab === key ? "active" : ""} aria-pressed={detailTab === key} onClick={() => setDetailTab(key)}>{title}</button>)}</nav>
      {detailTab === "conclusion" && <><SelectionAnalysis candidate={detail} onDocument={id => {setFocusDocument(id); setDetailTab("documents");}} onBackfillDimension={dimension => void backfill([detail.id], [dimension])} /><details className="batch-missing"><summary>数据缺口与分析版本</summary><ul>{(Array.isArray(evidence?.missing_data) ? evidence.missing_data : []).map((item, index) => <li key={index}>{String(item)}</li>)}</ul>{Array.isArray(evidence?.missing_data) && evidence.missing_data.length > 0 && <button disabled={busy || running} onClick={() => void backfill([detail.id])}>补齐该股票并重新分析</button>}<p className="batch-note">Skill：{text(detail.analysis_json.skill_code)} / {text(detail.analysis_json.skill_version)}<br />模型：{text(detail.analysis_json.actual_instance_code || detail.analysis_json.model_version)}</p></details></>}
      {detailTab === "facts" && <><h3>股票与公司</h3><p>{text(evidence?.identity?.company?.name) || "公司名称未提供"}</p><p className="batch-note">{text(evidence?.identity?.canonical_security_id)} · {text(evidence?.identity?.canonical_company_id)}</p><h3>数值与事实</h3><div className="batch-facts">{factList.slice(0, 35).map((fact, index) => <Fact key={`${fact.id}-${index}`} fact={fact} />)}</div></>}
      {detailTab === "documents" && <><p className="batch-note">模型引用 {citedIds.size} 篇文档 · 事实 {model.evidence_fact_ids?.length || 0} 条 · 切片 {model.evidence_chunk_ids?.length || 0} 条。展开文档可读取本次保存的原文快照。</p>{[...documentList].sort((a,b) => Number(Number(b.id) === focusDocument) - Number(Number(a.id) === focusDocument)).map(doc => <SourceDocument key={`${detail.id}-${doc.id}`} doc={doc} candidateId={detail.id} cited={citedIds.has(Number(doc.id))} selected={Number(doc.id) === focusDocument} />)}<h3>本次载入的切片</h3>{rows(evidence?.chunks).map((chunk, index) => <details key={index}><summary>{text(chunk.section_title) || `片段${index + 1}`} · 向量{label(text(chunk.embedding_status))}</summary><p>{text(chunk.text)}</p></details>)}</>}
      {detailTab === "trace" && <><h3>图谱关系</h3>{rows(evidence?.graph_paths).slice(0, 12).map((path, index) => <p key={index}>{text(path.subject_name)} → {label(text(path.predicate))} → {text(path.object_name)}<br /><small>证据文档 #{path.evidence_document_id} · {label(text(path.verification_status))}</small></p>)}<h3>血缘追踪</h3><p className="batch-note">{rows(evidence?.lineage).length} 条血缘记录 · {rows(evidence?.lakehouse_datasets).length} 个湖仓版本</p>{rows(evidence?.lineage).slice(0, 10).map((item, index) => <details key={index}><summary>{text(item.transformation)}</summary><p>{text(item.upstream_type)}:{text(item.upstream_id)} → {text(item.downstream_type)}:{text(item.downstream_id)}</p></details>)}<p className="batch-note">上下文版本：{text(evidence?.context_version)}<br />图谱版本：{text(evidence?.graph_version)}</p></>}
      {detail.decision === "PENDING" && <footer><label>确认目标价<input aria-label="确认目标价" type="number" min="0.000001" step="any" value={targetPrice} onChange={event => setTargetPrice(event.target.value)} /></label><button disabled={busy} onClick={() => void review("REJECTED")}>排除</button><button className="primary-button" disabled={busy || !(Number(targetPrice) > 0) || detail.analysis_json.model_output_status !== "SUCCESS" || detail.analysis_json.analysis_validation_status === "PARTIAL"} onClick={() => void review("APPROVED")}>确认并跟踪</button></footer>}
    </section></div>}
  </main>;
}
