import { useEffect, useRef, useState } from "react";
import { SelectionWorkbench as AdvancedWorkbench } from "./SelectionWatch";
import { api, type ModelInstance, type ModelProvider } from "./api";
import { selectionApi, type BatchAcceptance, type SelectionCandidate, type SelectionRecord, type SelectionRun, type SelectionTracking } from "./selectionApi";
import "./SelectionWatch.css";
import { SelectionAnalysis, type AnalysisDimension } from "./SelectionAnalysis";

const message = (error: unknown) => error instanceof Error ? error.message : "请求失败，请重试";
const modelConnectivityLabel = (item: ModelInstance) => item.availability_label || ({
  AVAILABLE: "可用", DEGRADED: "暂时不可用，可重试", BILLING: "欠费/额度不足", UNAVAILABLE: "不可用", NOT_CONFIGURED: "未配置",
  DISABLED: "已停用", CHECKING: "检查中", UNKNOWN: "未检查",
} as Record<string, string>)[item.availability_status || "UNKNOWN"] || "未检查";
const modelStatusLabel = (item: ModelInstance) => {
  const connectivity = modelConnectivityLabel(item);
  if (item.availability_status !== "AVAILABLE") return connectivity;
  return `${connectivity} · ${item.selection_contract_label || "未验证选股契约"}`;
};
const modelStatusReason = (item: ModelInstance) => item.selection_contract_status === "FAILED" || item.selection_contract_status === "PARTIAL"
  ? item.selection_contract_reason || item.availability_reason
  : item.availability_reason;
const modelIsSelectable = (item: ModelInstance) => item.enabled && item.api_key_configured
  && ["AVAILABLE", "DEGRADED", "UNKNOWN", "CHECKING"].includes(item.availability_status || "UNKNOWN");
const number = (value: number | null | undefined) => value == null ? "—" : value.toFixed(2);
const text = (value: unknown) => typeof value === "string" || typeof value === "number" ? String(value) : "";
const rows = (value: unknown): SelectionRecord[] => Array.isArray(value) ? value.filter(item => item && typeof item === "object") : [];
const attemptsNewestFirst = (value: unknown) => [...rows(value)].sort((left, right) => {
  const toTime = (record: SelectionRecord) => {
    const raw = text(record.at).trim();
    if (!raw) return Number.NEGATIVE_INFINITY;
    const normalized = /(?:Z|[+-]\d{2}:?\d{2})$/.test(raw) ? raw : `${raw}Z`;
    const parsed = Date.parse(normalized);
    return Number.isFinite(parsed) ? parsed : Number.NEGATIVE_INFINITY;
  };
  return toTime(right) - toTime(left);
});
const timestamp = (value: string) => new Date(/(?:Z|[+-]\d{2}:?\d{2})$/.test(value) ? value : `${value}Z`).toLocaleString("zh-CN", { timeZone: "Asia/Shanghai" });
const elapsed = (value: unknown) => {
  const seconds = Math.max(0, Math.floor(Number(value) || 0));
  if (seconds < 60) return `${seconds} 秒`;
  return `${Math.floor(seconds / 60)} 分 ${seconds % 60} 秒`;
};
const liveElapsed = (progress: SelectionRecord, _tick: number) => {
  const stored = Math.max(0, Math.floor(Number(progress.elapsed_seconds) || 0));
  const started = progress.activity_started_at || progress.started_at;
  if (!started) return stored;
  const timestampMs = new Date(String(started)).getTime();
  if (!Number.isFinite(timestampMs)) return stored;
  return Math.max(stored, Math.floor((Date.now() - timestampMs) / 1000));
};
const heartbeatAge = (value: unknown) => {
  if (!value) return null;
  const date = new Date(String(value));
  return Number.isFinite(date.getTime()) ? Math.max(0, Math.floor((Date.now() - date.getTime()) / 1000)) : null;
};
const phaseLabels: Record<string, string> = {
  DATA: "数据检查及同步", MODEL_PREFLIGHT: "模型测试", ANALYSIS: "分析与结果校验",
};
const phaseStatusLabel: Record<string, string> = {
  PENDING: "未开始", NOT_RECORDED: "历史未记录", RUNNING: "进行中", SUCCESS: "已完成", PARTIAL: "部分完成", FAILED: "失败", SKIPPED: "已跳过",
};
const phaseTime = (value: unknown) => value ? timestamp(String(value)) : "—";
const phaseElapsed = (phase: SelectionRecord, running: boolean) => {
  const stored = Math.max(0, Number(phase.elapsed_seconds) || 0);
  const started = phase.started_at;
  if (!running || !started || phase.status !== "RUNNING") return elapsed(stored);
  const parsed = Date.parse(/(?:Z|[+-]\d{2}:\d{2})$/.test(String(started)) ? String(started) : `${String(started)}Z`);
  return Number.isFinite(parsed) ? elapsed(Math.max(stored, Math.floor((Date.now() - parsed) / 1000))) : elapsed(stored);
};

function SelectionPhaseTimeline({ progress, running }: { progress: SelectionRecord; running: boolean }) {
  const raw = progress.phases && typeof progress.phases === "object" ? progress.phases as SelectionRecord : {};
  const phases: Array<SelectionRecord & { code: string }> = ["DATA", "MODEL_PREFLIGHT", "ANALYSIS"].map(code => {
    const phase = (raw[code] && typeof raw[code] === "object") ? raw[code] as SelectionRecord : {};
    return { ...phase, code };
  });
   return <section className="selection-phase-timeline" aria-label="后台处理三阶段">
    <div className="selection-phase-track">{phases.map((phase, index) => <div key={phase.code} className={`selection-phase ${String(phase.status || "PENDING").toLowerCase()}`}>
      <div className="selection-phase-title"><strong>{phaseLabels[phase.code]}</strong><span>{phaseStatusLabel[String(phase.status || "PENDING")] || "未开始"}</span></div>
       <div className="selection-phase-bar"><i style={{ width: `${Math.max(0, Math.min(100, Number(phase.percent || (phase.status === "SUCCESS" || phase.status === "FAILED" ? 100 : 0))))}%` }} /></div>
      <div className="selection-phase-times"><span>开始 {phaseTime(phase.started_at)}</span><span>结束 {phaseTime(phase.completed_at)}</span><b>耗时 {phaseElapsed(phase, running)}</b></div>
      {rows(phase.activities).slice(-3).reverse().map((activity, activityIndex) => <p key={`${text(activity.at)}-${activityIndex}`}>{text(activity.message) || "处理中"}</p>)}
      {rows(phase.activities).length > 3 && <details><summary>其余 {rows(phase.activities).length - 3} 项</summary>{rows(phase.activities).slice(0, -3).reverse().map((activity, activityIndex) => <p key={`${text(activity.at)}-old-${activityIndex}`}>{text(activity.message)} · {phaseTime(activity.at)}</p>)}</details>}
      {index < phases.length - 1 && <span className="selection-phase-connector" aria-hidden="true" />}
    </div>)}</div>
  </section>;
}
const sourceUrl = (value: unknown) => { try { const url = new URL(text(value)); return ["http:", "https:"].includes(url.protocol) ? url.href : null; } catch { return null; } };
const labels: Record<string, string> = {
  REVIEW: "待复核", TRACKING: "跟踪中", COMPLETED: "已完成", PARTIAL: "部分完成", APPROVED: "已确认",
  REJECTED: "已排除", PENDING: "待复核", PENDING_DATA: "等待行情", FAILED: "执行失败", QUEUED: "排队中",
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
const isRunning = (run: SelectionRun | null) => !!run && ["QUEUED", "RUNNING"].includes(run.status);
const financialFields: Array<[string, string, "money" | "yuan" | "percent"]> = [
  ["TOTALOPERATEREVE", "营业收入", "money"], ["PARENTNETPROFIT", "归母净利润", "money"],
  ["KCFJCXSYJLR", "扣非净利润", "money"], ["MLR", "毛利", "money"],
  ["EPSJB", "基本每股收益", "yuan"], ["EPSKCJB", "扣非每股收益", "yuan"],
  ["BPS", "每股净资产", "yuan"], ["MGJYXJJE", "每股经营现金流", "yuan"],
  ["TOTALOPERATEREVETZ", "营收同比", "percent"], ["PARENTNETPROFITTZ", "归母净利润同比", "percent"],
  ["ROEJQ", "加权净资产收益率", "percent"], ["XSMLL", "毛利率", "percent"], ["XSJLL", "净利率", "percent"],
];

function ModelMultiPicker({
  models, providers, selected, disabled, testingCode, onTest, onChange,
}: { models: ModelInstance[]; providers: ModelProvider[]; selected: string[]; disabled?: boolean; testingCode?: string | null; onTest: (item: ModelInstance) => void; onChange: (codes: string[]) => void }) {
  const options = models.filter(item => item.enabled && providers.some(provider => provider.id === item.provider_id && provider.enabled && provider.provider_type !== "MOCK"));
  const toggle = (code: string) => onChange(selected.includes(code) ? selected.filter(item => item !== code) : [...selected, code]);
  return <section className="model-multi-picker" aria-label="分析模型多选">
    <div className="model-multi-heading"><strong>分析模型（可多选）</strong><small>按勾选顺序作为首选与重试顺序；每小时后台测试只用于展示信号。</small></div>
    <div className="model-multi-list">{options.map(item => { const provider = providers.find(row => row.id === item.provider_id); const selectable = modelIsSelectable(item); const bars = Math.max(0, Math.min(4, Number(item.signal_strength || 0))); const testable = item.availability_status !== "AVAILABLE" && item.availability_status !== "CHECKING"; return <div key={item.instance_code} className={`model-option ${!selectable ? "unavailable" : ""}`}><label><input type="checkbox" checked={selected.includes(item.instance_code)} disabled={disabled || !selectable} onChange={() => toggle(item.instance_code)} /><span className="model-multi-name"><b>{item.model_name || item.model_code}</b><small>{provider?.provider_name || provider?.provider_code || "模型服务"} · {modelStatusLabel(item)}</small></span><span className={`model-signal strength-${bars}`} title={`信号强度：${item.signal_strength_label || "未知"}`}>{[1,2,3,4].map(level => <i key={level} className={level <= bars ? "on" : ""} />)}</span></label>{testable && <button type="button" className="model-test-button" disabled={disabled || testingCode === item.instance_code} onClick={() => onTest(item)}>{testingCode === item.instance_code ? "测试中…" : "测试"}</button>}</div>; })}</div>
    {!options.length && <p className="batch-note">暂无已配置的可用模型，请先在模型管理中配置。</p>}
  </section>;
}

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
  const [showRunScope, setShowRunScope] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [detail, setDetail] = useState<SelectionCandidate | null>(null);
  const [detailTab, setDetailTab] = useState("conclusion");
  const [llmEvaluation, setLlmEvaluation] = useState<SelectionRecord | null>(null);
  const [llmEvaluationBusy, setLlmEvaluationBusy] = useState(false);
  const [focusDocument, setFocusDocument] = useState<number | null>(null);
  const [targetPrice, setTargetPrice] = useState("");
  const [resultPage, setResultPage] = useState(1);
  const [modelInstances, setModelInstances] = useState<ModelInstance[]>([]);
  const [modelProviders, setModelProviders] = useState<ModelProvider[]>([]);
  const [analysisModelCode, setAnalysisModelCode] = useState("");
  const [analysisModelCodes, setAnalysisModelCodes] = useState<string[]>(() => {
    try { const raw = window.localStorage.getItem("selection.analysisModelCodes"); const parsed = raw ? JSON.parse(raw) : []; return Array.isArray(parsed) ? parsed.filter(item => typeof item === "string") : []; } catch { return []; }
  });
  const [modelsLoaded, setModelsLoaded] = useState(false);
  const [testingModelCode, setTestingModelCode] = useState<string | null>(null);
  const [preflight, setPreflight] = useState<SelectionRecord | null>(null);
  const [preflightBusy, setPreflightBusy] = useState(false);
  const [activityTick, setActivityTick] = useState(0);
  const dialogRef = useRef<HTMLElement>(null);
  const retryProgress = (run?.criteria_json.retry_progress || {}) as SelectionRecord;
  const retryActive = ["QUEUED", "RUNNING"].includes(text(retryProgress.status));
  const backfillProgress = (run?.criteria_json.backfill_progress || {}) as SelectionRecord;
  const backfillActive = ["QUEUED", "RUNNING"].includes(text(backfillProgress.status));
  const running = isRunning(run) || retryActive || backfillActive;

  useEffect(() => {
    let valid = true;
    void Promise.all([selectionApi.acceptance(), selectionApi.listRuns(), selectionApi.listTracking(), api.listModelInstances(), api.listModelProviders()])
      .then(async ([data, nextRuns, nextTracking, nextInstances, nextProviders]) => {
        if (!valid) return;
        setAcceptance(data); setRuns(nextRuns); setTracking(nextTracking);
        setModelInstances(nextInstances); setModelProviders(nextProviders); setModelsLoaded(true);
        const latest = nextRuns[0];
        if (latest) { const full = await selectionApi.getRun(latest.id); if (valid) setRun(full); }
      }).catch(err => { if (valid) setError(message(err)); }).finally(() => { if (valid) setLoading(false); });
    return () => { valid = false; };
  }, []);

  useEffect(() => {
    if (!running) return;
    const timer = window.setInterval(() => setActivityTick(value => value + 1), 1000);
    return () => window.clearInterval(timer);
  }, [running]);

  useEffect(() => {
    if (!modelsLoaded) return;
    const valid = analysisModelCodes.filter(code => modelInstances.some(item => item.instance_code === code));
    if (valid.length !== analysisModelCodes.length) setAnalysisModelCodes(valid);
    if (valid[0] && valid[0] !== analysisModelCode) setAnalysisModelCode(valid[0]);
    try { window.localStorage.setItem("selection.analysisModelCodes", JSON.stringify(valid)); } catch { /* browser storage may be disabled */ }
  }, [analysisModelCodes, analysisModelCode, modelInstances, modelsLoaded]);

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
        if (isRunning(next) || ["QUEUED", "RUNNING"].includes(text(nextRetry.status)) || ["QUEUED", "RUNNING"].includes(text(nextBackfill.status))) timer = setTimeout(() => void poll(), 1000);
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
    setBusy(true); setPreflightBusy(true); setError(""); setNotice(""); setPreflight(null);
    try {
      const gate = await selectionApi.preflight({
        market: acceptance.market, symbols: acceptance.symbols,
        knowledge_base_ids: [acceptance.knowledge_base_id], graph_ids: [acceptance.graph_id],
        period_months: 3, model_instance_code: analysisModelCode || null, model_instance_codes: analysisModelCodes, use_model: true, data_mode: "REAL",
      });
      setPreflight(gate);
      if (!gate.model_ready) {
        const model = gate.model || {};
        throw new Error(`${text(model.title) || "模型不可用"}：${text(model.summary) || "无法完成连通性检查"} ${text(model.suggestion)}`);
      }
      if (!gate.data_ready) {
        const missing = Number((gate.data as SelectionRecord)?.missing_symbols?.length || 0);
        const proceed = window.confirm(`分析前检查发现 ${missing} 只股票存在数据缺口。建议先更新数据；如果继续，将按部分数据分析并在结果中标记缺口。是否继续？`);
        if (!proceed) { setNotice("已取消分析，请先补齐数据后再开始。"); return; }
      }
      const actualModel = text((gate.model || {}).instance_code) || analysisModelCode || null;
      const created = await selectionApi.startRun({ market: acceptance.market, symbols: acceptance.symbols,
        universe_scope: "FIXED20_ACCEPTANCE", candidate_limit: 20, min_abs_change_pct: 0, min_volume_ratio: 0,
        period_months: 3,
        model_instance_code: actualModel, model_instance_codes: analysisModelCodes, knowledge_base_ids: [acceptance.knowledge_base_id], graph_ids: [acceptance.graph_id], use_model: true, data_mode: "REAL" });
      setRun(created); setRuns(current => [created, ...current]); setNotice("已提交后台，刷新页面后仍可查看执行进度。");
    } catch (err) { setError(message(err)); } finally { setBusy(false); setPreflightBusy(false); }
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
  async function retryWithModel() {
    if (!run || running || !analysisModelCodes.length || !unresolvedCandidates.length) return;
    const sourceRunId = run.id;
    const retryCodes = analysisModelCodes.filter(code => availableSwitchModels.some(item => item.instance_code === code));
    if (!retryCodes.length) { setError("当前勾选的分析模型均不可用，请先点击模型后的“测试”并通过后再重试。"); return; }
    const selected = availableSwitchModels.find(item => item.instance_code === retryCodes[0]);
    setBusy(true); setError(""); setNotice("");
    try {
      const next = await selectionApi.retryFailedWithModel(sourceRunId, {
        model_instance_code: retryCodes[0], model_instance_codes: retryCodes,
        candidate_ids: unresolvedCandidates.map(item => item.id),
      });
      setRun(next); setRuns(await selectionApi.listRuns());
      setNotice(`已按分析模型勾选顺序通过${selected?.model_name || retryCodes[0]}预检，创建新批次 #${next.id}；原批次 #${sourceRunId} 保留。`);
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
  async function rescore(item: SelectionCandidate) {
    setBusy(true); setError("");
    try {
      const scoring = await selectionApi.rescoreCandidate(item.id);
      const next = {...item, analysis_json: {...item.analysis_json, scoring}};
      setDetail(next);
      setRun(current => current ? {...current, candidates: current.candidates.map(row => row.id === next.id ? next : row)} : current);
      setNotice("八维评分已生成，原分析证据和历史结论未被覆盖。");
    } catch (err) { setError(message(err)); } finally { setBusy(false); }
  }

  async function loadLlmEvaluation(item: SelectionCandidate) {
    setLlmEvaluationBusy(true); setError("");
    try { setLlmEvaluation(await selectionApi.llmEvaluation(item.id)); }
    catch (err) { setError(message(err)); }
    finally { setLlmEvaluationBusy(false); }
  }

  async function generateLlmEvaluation(item: SelectionCandidate) {
    setLlmEvaluationBusy(true); setError("");
    try {
      const result = await selectionApi.createLlmEvaluation(item.id, text(item.analysis_json.actual_instance_code || analysisModelCode));
      setLlmEvaluation(result);
      setDetail(current => current && current.id === item.id ? {...current, analysis_json: {...current.analysis_json, llm_evaluation: result}} : current);
      setRun(current => current ? {...current, candidates: current.candidates.map(row => row.id === item.id ? {...row, analysis_json: {...row.analysis_json, llm_evaluation: result}} : row)} : current);
    } catch (err) { setError(message(err)); }
    finally { setLlmEvaluationBusy(false); }
  }

  async function confirmFollowFromEvaluation(item: SelectionCandidate) {
    setBusy(true); setError("");
    try { const next = await selectionApi.follow(item.id, { target_price: Number(item.analysis_json.model && (item.analysis_json.model as SelectionRecord).target_price) || undefined }); setDetail(null); setRun(current => current ? {...current, candidates: current.candidates.map(row => row.id === item.id ? next : row)} : current); setTracking(await selectionApi.listTracking()); setNotice("已确认跟踪，后续结果会进入跟踪与复盘。"); }
    catch (err) { setError(message(err)); }
    finally { setBusy(false); }
  }

  async function refreshTracking() {
    setBusy(true); setError("");
    try { const result = await selectionApi.refresh(); setTracking(await selectionApi.listTracking()); setNotice(`刷新完成：${result.completed_count}只完成，${result.pending_data_count}只等待行情。`); }
    catch (err) { setError(message(err)); } finally { setBusy(false); }
  }

  const modelCount = run?.candidates.filter(item => item.analysis_json.model_output_status === "SUCCESS" && item.analysis_json.analysis_validation_status !== "PARTIAL").length || 0;
  const dataBlockedCandidates = run?.candidates.filter(item => item.analysis_json.data_gate_status === "MISSING_DATA" || item.analysis_json.model_output_status === "NOT_RUN") || [];
  const partialCount = run?.candidates.filter(item => item.analysis_json.model_output_status === "SUCCESS" && item.analysis_json.data_gate_status !== "MISSING_DATA" && item.analysis_json.analysis_validation_status === "PARTIAL").length || 0;
  const failedCandidates = run?.candidates.filter(item => item.decision === "PENDING" && item.analysis_json.model_output_status === "FAILED") || [];
  const unresolvedCandidates = run?.candidates.filter(item => item.decision === "PENDING" && (item.analysis_json.model_output_status === "FAILED" || item.analysis_json.model_output_status === "NOT_RUN" || item.analysis_json.data_gate_status === "MISSING_DATA" || item.analysis_json.analysis_validation_status === "PARTIAL")) || [];
  const modelPickerOptions = modelInstances.filter(item => item.enabled && modelProviders.some(provider => provider.id === item.provider_id && provider.enabled && provider.provider_type !== "MOCK"));
  const availableSwitchModels = modelPickerOptions.filter(modelIsSelectable);
  async function testUnavailableModel(item: ModelInstance) {
    setTestingModelCode(item.instance_code); setError(""); setNotice("");
    try {
      const result = await api.testModelInstance(item.id);
      const refreshed = await api.listModelInstances();
      setModelInstances(refreshed);
      if (result.status === "SUCCESS") setNotice(`${item.model_name || item.instance_code} 测试通过，已转为可用。`);
      else setError(`${item.model_name || item.instance_code} 测试未通过：${result.response_text || result.message || "模型未返回成功"}`);
    } catch (err) { setError(`${item.model_name || item.instance_code} 测试失败：${message(err)}`); }
    finally { setTestingModelCode(null); }
  }
  const progress = (run?.criteria_json.progress || {}) as SelectionRecord;
  const progressHeartbeat = text(progress.heartbeat_at || progress.updated_at);
  const progressHeartbeatAge = heartbeatAge(progress.heartbeat_at);
  const progressStale = running && progressHeartbeatAge != null && progressHeartbeatAge > 15;
  const actualModelCode = text(progress.model_instance_code)
    || text(run?.model_instance_code)
    || text(run?.candidates.find(item => text(item.analysis_json.actual_instance_code))?.analysis_json.actual_instance_code);
  const actualModel = modelInstances.find(item => item.instance_code === actualModelCode);
  const actualProvider = actualModel ? modelProviders.find(item => item.id === actualModel.provider_id) : undefined;
  const liveElapsedSeconds = liveElapsed(progress, activityTick);
  const fixedRuns = runs;
  const pageSize = 10;
  const pageCount = Math.max(1, Math.ceil((run?.candidates.length || 0) / pageSize));
  const visibleCandidates = run?.candidates.slice((Math.min(resultPage, pageCount) - 1) * pageSize, Math.min(resultPage, pageCount) * pageSize) || [];
  const companyCount = run?.candidates.filter(item => item.evidence_json.identity?.company).length || 0;
  const documentCount = new Set(run?.candidates.flatMap(item => rows(item.evidence_json.documents).map(doc => doc.id)) || []).size;
  const runTracking = tracking.filter(item => run?.candidates.some(candidate => candidate.id === item.candidate_id));
  const requestSymbols = (run?.criteria_json.request as SelectionRecord | undefined)?.symbols;
  const requestModelCodes = Array.isArray((run?.criteria_json.request as SelectionRecord | undefined)?.model_instance_codes)
    ? ((run?.criteria_json.request as SelectionRecord).model_instance_codes as unknown[]).map(text).filter(Boolean)
    : run?.model_instance_code ? [run.model_instance_code] : [];
  const requestModelNames = requestModelCodes.map(code => modelInstances.find(item => item.instance_code === code)?.model_name || code);
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

  if (advanced) return <><button className="batch-return" onClick={() => setAdvanced(false)}>← 返回选股工作台</button><AdvancedWorkbench mode={mode} initialCreate={newSelection} initialModelCode={analysisModelCode || run?.model_instance_code || ""} onCreated={created => {setRun(created); setRuns(current => [created, ...current]); setAdvanced(false); setNewSelection(false);}} /></>;
  return <main className="batch-workbench">
    {run && <button type="button" className="batch-scope-button" onClick={() => setShowRunScope(true)}>查看本次任务股票与参数</button>}
    {mode === "selection" && <ModelMultiPicker models={modelInstances} providers={modelProviders} selected={analysisModelCodes} disabled={busy || running} testingCode={testingModelCode} onTest={item => void testUnavailableModel(item)} onChange={codes => { setAnalysisModelCodes(codes); setAnalysisModelCode(codes[0] || ""); }} />}
    <header className="batch-heading"><div><p className="eyebrow">多源证据 · 可追溯分析</p><h1>选股分析工作台</h1><p>选择股票与知识范围 → 后台分析 → 核对结论与原文</p><small className="batch-model-picker-hint">分析模型请在上方多选；勾选顺序同时用于首选模型和失败重试顺序。</small></div><div className="batch-actions"><button onClick={() => {setNewSelection(false); setAdvanced(true);}}>高级工作台</button>
      {mode === "selection" ? <><button disabled={busy || loading || running || !acceptance?.prepared} onClick={() => void start()}>{preflightBusy ? "正在检查数据与模型…" : busy ? "提交中…" : "验证20股样本"}</button><button className="primary-button" onClick={() => {setNewSelection(true); setAdvanced(true);}}>新建选股</button></> : <button className="primary-button" disabled={busy || loading} onClick={() => void refreshTracking()}>刷新跟踪</button>}</div></header>
    {error && <p className="batch-alert error" role="alert">{error}</p>}{notice && <p className="batch-alert" role="status">{notice}</p>}
    {preflight && !run && <section className="batch-preflight" aria-label="分析前检查结果"><strong>分析前检查</strong><span>模型：{text((preflight.model || {}).title) || (preflight.model_ready ? "可用" : "不可用")}</span><span>数据：{preflight.data_ready ? "齐备" : `部分缺失（${Number((preflight.data as SelectionRecord)?.missing_symbols?.length || 0)} 只）`}</span>{(Array.isArray(preflight.blockers) ? preflight.blockers : []).map((item, index) => <p className="batch-alert error" key={index}>{text(item)}</p>)}{(Array.isArray(preflight.warnings) ? preflight.warnings : []).slice(0, 3).map((item, index) => <p className="batch-note" key={index}>{text(item)}</p>)}</section>}
    <section className="batch-summary" aria-label="全链路进度">
      <div><small>本批股票</small><strong>{selectedCount} 只</strong><small>公司已关联 {companyCount} 只</small></div>
      <div><small>本次载入证据</small><strong>{documentCount} 篇文档</strong><small>按证据文档编号去重</small></div>
      <div><small>已通过引用校验</small><strong>{modelCount} / {selectedCount}</strong><small>{run?.analysis_mode === "RULE_ONLY" ? "本批为规则候选，未调用模型" : "模型研判与来源引用"}</small></div>
      <div><small>需要处理</small><strong>{failedCandidates.length} 失败 · {partialCount} 待核对 · {dataBlockedCandidates.length} 数据不足</strong><small>{running ? "正在后台执行" : "重试会先同步数据并测试模型"}</small></div>
      <div><small>本批确认后跟踪</small><strong>{runTracking.length} 只</strong><small>{runTracking.filter(item => item.status === "COMPLETED").length} 只已复盘</small></div>
    </section>
    <div className="batch-scope"><p className="batch-note">{run ? `当前批次 #${run.id} · ${Number(run.criteria_json.period_months || 3)} 个月（${text(run.criteria_json.period_start)} 至 ${text(run.criteria_json.period_end)}）· ${run.knowledge_base_ids_json.length} 个知识库 · ${run.graph_ids_json.length} 张图谱。` : "点击“新建选股”选择股票，或运行20股基准样本。"}本次分析使用已入库数据，行情日期逐股显示；引用校验通过不等于投资结论已经验证。</p><button onClick={() => setShowData(!showData)} aria-expanded={showData}>{showData ? "收起基准样本验收" : "查看20股基准样本验收"}</button></div>
    {showData && <section className="batch-card"><div className="batch-card-title"><h2>20股数据验收</h2><span>实际读取结果 · 图谱 #{acceptance?.graph_id}</span></div><div className="batch-table-wrap"><table><thead><tr><th>股票</th><th>公司映射</th><th>证据文档</th><th>载入切片</th><th>事实与观测</th><th>图谱路径</th><th>血缘</th><th>状态</th></tr></thead><tbody>{acceptance?.rows.map(item => <tr key={item.symbol}><td><strong>{item.name}</strong><small>{item.symbol}</small></td><td>{item.company_linked ? "已关联" : "缺失"}</td><td>{item.documents}</td><td>{item.selected_chunks}</td><td>{item.facts}</td><td>{item.relations}</td><td>{item.lineage}</td><td><span className={`batch-badge ${item.passed ? "" : "warning"}`}>{item.passed ? "链路通过" : "待处理"}</span></td></tr>)}</tbody></table></div><p className="batch-note">切片按上下文预算选取，未载入全部切片不等于切片失败。本轮复用已有采集数据，未重新采集全量远程数据。</p></section>}
    {loading && <p className="batch-empty" role="status">正在读取20股数据与历史分析…</p>}
    {!loading && !run && <p className="batch-empty">{acceptance?.prepared ? "数据准备已通过，请点击“新建选股”或“验证20股样本”。" : "数据准备尚未完成，请查看验收状态。"}</p>}
    {run && <section className="batch-card"><div className="batch-card-title"><div><h2>{running ? "正在分析" : "本轮结果"} <span className="batch-badge">{label(run.status)}</span></h2><span>分析时间 {timestamp(run.as_of)}（北京时间）</span><div className="batch-model-status"><strong>实际执行模型：{actualModel?.model_name || actualModelCode || "尚未开始调用"}</strong><small>{actualModelCode ? `${actualModelCode} · ${actualProvider?.provider_name || actualProvider?.provider_code || "模型服务"}` : "预检后才会确定实际模型"}</small></div></div><select aria-label="历史分析批次" disabled={busy} value={run.id} onChange={event => { setError(""); void selectionApi.getRun(Number(event.target.value)).then(setRun).catch(err => setError(message(err))); }}>{fixedRuns.map(item => <option key={item.id} value={item.id}>第{item.id}轮 · {label(item.status)}</option>)}</select></div>
      <SelectionPhaseTimeline progress={progress} running={running} />
  {(isRunning(run) || retryActive) && <div className={`batch-progress${progressStale ? " stale" : ""}`} role="status"><div><strong>当前阶段：{text(progress.stage) || "准备中"}</strong><span>{Number(progress.percent || 0)}%</span></div><progress aria-label="分析进度" max="100" value={Number(progress.percent || 0)} /><div className="batch-progress-meta"><span className="batch-progress-activity">当前动作：{text(progress.activity || progress.message) || "等待工作线程启动"}</span><span>当前动作耗时：{elapsed(liveElapsedSeconds)}</span><span>实际模型：{text(progress.model_instance_code) || "预检后确定"}</span><span>当前股票：{Array.isArray(progress.candidate_symbols) ? progress.candidate_symbols.join("、") : "--"}</span>{Number(progress.attempt || 0) > 0 && <span>尝试：{Number(progress.attempt)} / 3</span>}{progress.active_model_call_id ? <span>调用记录：#{text(progress.active_model_call_id)}</span> : null}{Number(progress.total_stock_count || 0) > 0 && <span>已核验：{Number(progress.completed_stock_count || 0)} / {Number(progress.total_stock_count)} 只</span>}{Number(progress.prompt_chars || 0) > 0 && <span>模型上下文：{Number(progress.prompt_chars).toLocaleString()} / {Number(progress.prompt_limit_chars || 60000).toLocaleString()} 字符</span>}{Number(progress.retained_document_count || 0) > 0 && <span>载入文档：{Number(progress.retained_document_count)} 篇{Number(progress.omitted_document_count || 0) > 0 ? ` · 预算外 ${Number(progress.omitted_document_count)} 篇` : ""}</span>}<span>最近心跳：{progressHeartbeat ? `${heartbeatAge(progressHeartbeat) ?? 0} 秒前` : "尚未记录"}</span></div>{(progress.last_validation_error || progress.last_call_error) ? <p className="batch-missing">最近一次未通过：{text(progress.last_validation_error || progress.last_call_error)}</p> : null}{progressStale && <strong className="batch-progress-warning">最近 {progressHeartbeatAge} 秒没有心跳更新，后台可能异常；系统不会把等待时间当作进度，模型调用超时后会按重试策略处理。</strong>}<small>进度按已完成工作记录；等待模型时百分比保持不变，耗时和心跳持续更新。</small></div>}
      {backfillActive && <div className="batch-progress" role="status"><div><strong>{text(backfillProgress.message) || "正在补齐缺失数据"}</strong><span>{Number(backfillProgress.percent || 0)}%</span></div><progress aria-label="数据补齐进度" max="100" value={Number(backfillProgress.percent || 0)} /><small>逐股票、逐板块记录成功、空数据和失败原因；旧分析保持不变。</small></div>}
      {run.error_message && <p className="batch-alert error">{run.error_message}</p>}
      {(unresolvedCandidates.length > 0 || retryActive || run.status === "FAILED") && <section className="selection-recovery" aria-label="分析失败处理">
        <div className="selection-scope-heading"><strong>校验通过 {modelCount} 只 · 待核对 {partialCount} 只 · 失败 {failedCandidates.length} 只 · 数据不足 {dataBlockedCandidates.length} 只</strong><button disabled={busy || running} onClick={() => void retryAnalysis()}>{retryActive ? "正在重新同步并分析…" : run.status === "FAILED" ? "重新执行此批次" : `同步并重试 ${unresolvedCandidates.length} 只未通过股票`}</button></div>
        {unresolvedCandidates.length > 0 && run.status !== "FAILED" && <div className="selection-model-switch" role="group" aria-label="使用分析模型重试"><strong>重试模型</strong><span>使用上方已勾选的分析模型，按顺序进行预检和重试：{analysisModelCodes.length ? analysisModelCodes.join(" → ") : "尚未勾选"}</span><button type="button" disabled={busy || running || !analysisModelCodes.length || !availableSwitchModels.length} onClick={() => void retryWithModel()}>{busy ? "正在检查模型…" : "使用已选模型重试"}</button><small>重试不再单独选择模型，避免上下两处配置不一致；原批次、证据和调用记录保持不变。</small></div>}
        <p className="batch-note">逐股重试保留成功结果、本轮证据及历史尝试；整批异常会创建新批次。</p>
        {retryProgress.message && <p role="status">{text(retryProgress.message)}</p>}
        {failedCandidates.length > 0 && <details><summary>查看逐股失败原因与尝试记录（{failedCandidates.length}）</summary><div className="selection-failure-list">{failedCandidates.map(item => { const detail = (item.analysis_json.model_failure_detail || {}) as SelectionRecord; const actions = rows(detail.recovery_actions); return <article key={item.id}><strong>{item.name || item.symbol} · {item.symbol}</strong><p><b>{text(detail.title) || "执行未完成"}</b>：{text(detail.summary) || text(item.analysis_json.model_failure_reason) || run.error_message || "历史批次未记录逐股错误，请重试。"}</p><p className="batch-note">建议：{text(detail.suggestion) || "请查看调用记录或更换模型后重试。"}</p>{actions.length > 0 && <div className="batch-recovery-actions"><strong>针对性处理步骤</strong><ol>{actions.map((action, index) => <li key={index}>{text(action)}</li>)}</ol></div>}<details><summary>查看技术详情</summary><pre>{text(detail.raw_error) || text(item.analysis_json.model_failure_reason) || "暂无原始错误"}</pre></details>{attemptsNewestFirst(item.analysis_json.model_attempts).map((attempt, index) => <small key={index}>{attempt.at ? timestamp(text(attempt.at)) : ""} · {attempt.status === "SUCCESS" ? "调用完成" : "未通过"}{attempt.model_call_log_id ? ` · 调用 #${attempt.model_call_log_id}` : ""} · {text(attempt.reason) || "已返回结果并经过引用校验"}</small>)}</article>; })}</div></details>}
      </section>}
      {run.missing_data_json.length > 0 && <details className="batch-missing"><summary>数据缺口与限制（{run.missing_data_json.length}）</summary><ul>{run.missing_data_json.map((item, index) => <li key={index}>{item}</li>)}</ul><button disabled={busy || running} onClick={() => void backfill()}>补齐全部缺失并重新分析</button></details>}
      <div className="batch-table-wrap"><table><thead><tr><th>股票</th><th>行情基准</th><th>模型判断</th><th>证据</th><th>确认状态</th><th>操作</th></tr></thead><tbody>{visibleCandidates.map(item => {
         const dataBlocked = item.analysis_json.model_output_status === "NOT_RUN" || item.analysis_json.data_gate_status === "MISSING_DATA";
         const success = item.analysis_json.model_output_status === "SUCCESS" && !dataBlocked;
        const partial = item.analysis_json.analysis_validation_status === "PARTIAL";
        const local = !!item.analysis_json.local_multi_source;
         return <tr key={item.id}><td><strong>{item.name || item.symbol}</strong><small>{item.symbol}</small></td><td>{number(item.entry_price)} 元<small>{item.entry_date || "缺少日期"}</small></td><td><span className={`batch-badge ${success && !partial ? "" : "warning"}`}>{dataBlocked ? "数据不足 · 未分析" : partial ? "证据不足 · 待核对" : success ? label(text(item.analysis_json.recommendation)) : local ? "模型失败 · 本地证据盘点" : label(text(item.analysis_json.model_output_status))}</span><small className="batch-clamp">{dataBlocked ? text(item.analysis_json.model_failure_reason) || "未达到模型分析数据门槛" : success ? text((item.analysis_json.model as SelectionRecord)?.headline || item.analysis_json.summary) : local ? "模型未返回成功结果；仅保存证据盘点，不是模型结论" : "尚无有效模型结论"}</small></td><td>{rows(item.evidence_json.documents).length} 篇文档<small>{rows(item.evidence_json.facts).length} 条事实与观测</small></td><td>{label(item.decision)}</td><td><button onClick={() => { setDetail(item); setFocusDocument(null); setDetailTab("conclusion"); setTargetPrice(text(item.analysis_json.target_price)); }}>分析与证据</button></td></tr>;
      })}</tbody></table>{!run.candidates.length && !running && <p className="batch-empty">本轮无可分析结果，请检查数据与失败原因。</p>}</div>{run.candidates.length > pageSize && <nav className="batch-pagination" aria-label="分析结果分页"><span>共 {run.candidates.length} 只 · 第 {Math.min(resultPage, pageCount)} / {pageCount} 页</span><button disabled={resultPage <= 1} onClick={() => setResultPage(page => page - 1)}>上一页</button><button disabled={resultPage >= pageCount} onClick={() => setResultPage(page => page + 1)}>下一页</button></nav>}</section>}
    {mode === "tracking" && <section className="batch-card"><div className="batch-card-title"><div><h2>跟踪与复盘</h2><span>从人工确认后的有效交易日开始，当前为10日规则。</span></div><button onClick={() => setAdvanced(true)}>查看详细复盘与Skill回归</button></div><div className="batch-table-wrap"><table><thead><tr><th>股票</th><th>目标 / 最新价</th><th>有效交易日</th><th>累计收益</th><th>最大回撤</th><th>状态</th></tr></thead><tbody>{tracking.map(item => <tr key={item.id}><td>{item.symbol}</td><td>{number(item.target_price)} / {number(item.latest_price)}</td><td>{item.observed_sessions} / {item.required_sessions}</td><td>{number(item.cumulative_return_pct)}%</td><td>{number(item.max_drawdown_pct)}%</td><td>{label(item.status)}</td></tr>)}</tbody></table>{!tracking.length && <p className="batch-empty">确认入选后自动建立跟踪。尚未发生的交易日不会生成收益结果。</p>}</div></section>}
    {detail && <div className="batch-overlay" onMouseDown={event => { if (event.target === event.currentTarget && !busy) setDetail(null); }}><section ref={dialogRef} className="batch-detail" role="dialog" aria-modal="true" aria-label={`${detail.name || detail.symbol}分析与证据`}>
      <header><div><h2>{detail.name || detail.symbol}</h2><span>{detail.symbol} · 行情 {detail.entry_date} · {number(detail.entry_price)} 元</span></div><button disabled={busy} onClick={() => setDetail(null)}>关闭</button></header>
      <nav className="batch-tabs" aria-label="研判详情分类">{[["conclusion", "分析结论"], ["facts", "数据事实"], ["documents", "原文与引用"], ["trace", "关系与血缘"], ["llm", "LLM评价"]].map(([key, title]) => <button key={key} className={detailTab === key ? "active" : ""} aria-pressed={detailTab === key} onClick={() => { setDetailTab(key); if (key === "llm") void loadLlmEvaluation(detail); }}>{title}</button>)}</nav>
      {detailTab === "conclusion" && <><SelectionAnalysis candidate={detail} onRescore={() => void rescore(detail)} onDocument={id => {setFocusDocument(id); setDetailTab("documents");}} onBackfillDimension={dimension => void backfill([detail.id], [dimension])} /><details className="batch-missing"><summary>数据缺口与分析版本</summary><ul>{(Array.isArray(evidence?.missing_data) ? evidence.missing_data : []).map((item, index) => <li key={index}>{String(item)}</li>)}</ul>{Array.isArray(evidence?.missing_data) && evidence.missing_data.length > 0 && <button disabled={busy || running} onClick={() => void backfill([detail.id])}>补齐该股票并重新分析</button>}<p className="batch-note">Skill：{text(detail.analysis_json.skill_code)} / {text(detail.analysis_json.skill_version)}<br />模型：{text(detail.analysis_json.actual_instance_code || detail.analysis_json.model_version)}</p></details></>}
      {detailTab === "facts" && <><h3>股票与公司</h3><p>{text(evidence?.identity?.company?.name) || "公司名称未提供"}</p><p className="batch-note">{text(evidence?.identity?.canonical_security_id)} · {text(evidence?.identity?.canonical_company_id)}</p><h3>数值与事实</h3><div className="batch-facts">{factList.slice(0, 35).map((fact, index) => <Fact key={`${fact.id}-${index}`} fact={fact} />)}</div></>}
      {detailTab === "documents" && <><p className="batch-note">模型引用 {citedIds.size} 篇文档 · 事实 {model.evidence_fact_ids?.length || 0} 条 · 切片 {model.evidence_chunk_ids?.length || 0} 条。展开文档可读取本次保存的原文快照。</p>{[...documentList].sort((a,b) => Number(Number(b.id) === focusDocument) - Number(Number(a.id) === focusDocument)).map(doc => <SourceDocument key={`${detail.id}-${doc.id}`} doc={doc} candidateId={detail.id} cited={citedIds.has(Number(doc.id))} selected={Number(doc.id) === focusDocument} />)}<h3>本次载入的切片</h3>{rows(evidence?.chunks).map((chunk, index) => <details key={index}><summary>{text(chunk.section_title) || `片段${index + 1}`} · 向量{label(text(chunk.embedding_status))}</summary><p>{text(chunk.text)}</p></details>)}</>}
      {detailTab === "trace" && <><h3>图谱关系</h3>{rows(evidence?.graph_paths).slice(0, 12).map((path, index) => <p key={index}>{text(path.subject_name)} → {label(text(path.predicate))} → {text(path.object_name)}<br /><small>证据文档 #{path.evidence_document_id} · {label(text(path.verification_status))}</small></p>)}<h3>血缘追踪</h3><p className="batch-note">{rows(evidence?.lineage).length} 条血缘记录 · {rows(evidence?.lakehouse_datasets).length} 个湖仓版本</p>{rows(evidence?.lineage).slice(0, 10).map((item, index) => <details key={index}><summary>{text(item.transformation)}</summary><p>{text(item.upstream_type)}:{text(item.upstream_id)} → {text(item.downstream_type)}:{text(item.downstream_id)}</p></details>)}<p className="batch-note">上下文版本：{text(evidence?.context_version)}<br />图谱版本：{text(evidence?.graph_version)}</p></>}
      {detailTab === "llm" && <section className="llm-evaluation-panel"><div className="analysis-card-heading"><h3>LLM评价</h3><small>评价在分析流程的第三阶段自动生成并保存；此处仅查看已保存结果。</small><button type="button" disabled={llmEvaluationBusy || detail.analysis_json.model_output_status !== "SUCCESS" || detail.analysis_json.analysis_validation_status === "PARTIAL"} onClick={() => void generateLlmEvaluation(detail)}>{llmEvaluationBusy ? "正在复核…" : "重新生成评价"}</button></div>{detail.analysis_json.model_output_status !== "SUCCESS" ? <p className="batch-alert warning">模型分析未成功，暂无可评价的有效结论。</p> : llmEvaluation?.status === "SUCCESS" ? <><p className="llm-evaluation-text">{text(llmEvaluation.text)}</p><p className="batch-note">模型：{text(llmEvaluation.model_instance_code)} · 生成时间：{text(llmEvaluation.generated_at)}</p><button type="button" className="primary-button" disabled={busy || detail.decision !== "PENDING"} onClick={() => void confirmFollowFromEvaluation(detail)}>确认跟踪</button></> : <p className="batch-note">本轮分析未生成评价或评价模型调用失败：{text(llmEvaluation?.message) || "请查看第三阶段记录后重试。"}</p>}</section>}
      {detail.decision === "PENDING" && <footer><label>确认目标价<input aria-label="确认目标价" type="number" min="0.000001" step="any" value={targetPrice} onChange={event => setTargetPrice(event.target.value)} /></label><button disabled={busy} onClick={() => void review("REJECTED")}>排除</button><button className="primary-button" disabled={busy || !(Number(targetPrice) > 0) || detail.analysis_json.model_output_status !== "SUCCESS" || detail.analysis_json.analysis_validation_status === "PARTIAL"} onClick={() => void review("APPROVED")}>确认并跟踪</button></footer>}
    </section></div>}
    {showRunScope && run && <div className="batch-overlay" onMouseDown={event => { if (event.target === event.currentTarget) setShowRunScope(false); }}><section className="batch-detail batch-run-scope-dialog" role="dialog" aria-modal="true" aria-label="本次任务范围与参数">
      <header><div><h2>本次任务范围与参数</h2><span>批次 #{run.id} · 创建于 {timestamp(run.created_at)}</span></div><button type="button" onClick={() => setShowRunScope(false)}>关闭</button></header>
      <div className="batch-scope-grid"><div><strong>股票</strong><p>{Array.isArray(requestSymbols) && requestSymbols.length ? requestSymbols.join("、") : "按筛选条件生成候选"}</p></div><div><strong>分析周期</strong><p>{Number(run.criteria_json.period_months || 3)} 个月 · {text(run.criteria_json.period_start) || "--"} 至 {text(run.criteria_json.period_end) || "--"}</p></div><div><strong>用户勾选模型</strong><p>{requestModelNames.length ? requestModelNames.join(" → ") : "历史任务未记录多模型顺序"}</p></div><div><strong>实际执行模型</strong><p>{actualModel?.model_name || actualModelCode || "尚未开始执行"}</p></div><div><strong>知识库 / 图谱</strong><p>{run.knowledge_base_ids_json.join(", ") || "未指定"} / {run.graph_ids_json.join(", ") || "未指定"}</p></div><div><strong>筛选参数</strong><p>候选上限 {run.candidate_limit} · 涨跌幅阈值 {text((run.criteria_json.request as SelectionRecord | undefined)?.min_abs_change_pct) || "0"}% · 成交量比 {text((run.criteria_json.request as SelectionRecord | undefined)?.min_volume_ratio) || "0"}</p></div><div><strong>数据模式</strong><p>{run.data_mode} · 截止 {timestamp(run.as_of)}</p></div></div>
    </section></div>}
  </main>;
}
