import { useEffect, useRef, useState } from "react";
import { BrainCircuit, Search, ArrowUpRight, Clock3, History, FileText, RefreshCw, X, MessageCircle, Maximize2, Minimize2 } from "lucide-react";
import { api, type StockSymbol, type ModelInstance } from "./api";
import { analystApi, type AnalystRun, type AnalystCoverage, type AnalystEvidence, type AnalystRequest, type AnalystRevision, type AnalystDailyReview, type AnalystDiscussionContext } from "./stockAnalystApi";
import { AnalystFollowup } from "./AnalystFollowup";
import { useAnalystFullscreen } from "./AnalystFullscreen";
import { StockDetailDrawer } from "./StockDetailDrawer";
import "./AiStockAnalyst.css";

const categories: Record<string, string> = { MARKET: "量价行情", FINANCIAL: "财务数据", F10: "主营与公司资料", NEWS: "新闻", NOTICE: "公告", RESEARCH: "机构研究", QA: "投资者问答", COMPANY: "公司事实与分类", KNOWLEDGE: "知识库文档" };
const directions: Record<string, string> = { BULLISH: "偏向上行", BEARISH: "偏向下行", SIDEWAYS: "区间震荡", UNCERTAIN: "方向分歧" };
const statuses: Record<string, string> = { QUEUED: "已提交", RUNNING: "分析中", COMPLETED: "已完成", FAILED: "分析失败", CANCELLED: "已取消" };
const running = (row: AnalystRun | null) => !!row && ["QUEUED", "RUNNING"].includes(row.status);
function dateValue(value: string) { return new Date(/Z$|[+-]\d\d:\d\d$/.test(value) ? value : `${value}Z`); }
function when(value?: string | null) { return value ? dateValue(value).toLocaleString("zh-CN", { hour12: false }) : "—"; }
function saved(key: string) { try { return localStorage.getItem(key) || ""; } catch { return ""; } }
function remember(key: string, value: string) { try { localStorage.setItem(key, value); } catch { /* optional preference */ } }
function sourceUrl(value: unknown) { return typeof value === "string" && /^https?:\/\//i.test(value) ? value : undefined; }
function Points({ title, items }: { title: string; items?: string[] }) {
  return <section className="analyst-box"><h3>{title}</h3>{items?.length ? <ul>{items.map((item, i) => <li key={i}>{item}</li>)}</ul> : <p className="analyst-muted">本次未形成独立判断</p>}</section>;
}

export function AiStockAnalyst() {
  const [market, setMarket] = useState("ALL");
  const [query, setQuery] = useState("");
  const [matches, setMatches] = useState<StockSymbol[]>([]);
  const [stock, setStock] = useState<Pick<StockSymbol, "market" | "symbol" | "name"> | null>(null);
  const [months, setMonths] = useState(12);
  const [models, setModels] = useState<ModelInstance[]>([]);
  const [model, setModel] = useState(saved("ai-analyst-model"));
  const [coverage, setCoverage] = useState<AnalystCoverage | null>(null);
  const [previewing, setPreviewing] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [testing, setTesting] = useState(false);
  const [active, setActive] = useState<AnalystRun | null>(null);
  const [confirmedRevision, setConfirmedRevision] = useState<AnalystRevision | null>(null);
  const [latestReview, setLatestReview] = useState<AnalystDailyReview | null>(null);
  const [history, setHistory] = useState<AnalystRun[]>([]);
  const [evidence, setEvidence] = useState<AnalystEvidence[]>([]);
  const [tab, setTab] = useState<"result" | "evidence" | "process" | "stock" | "timeline" | "tracking">("result");
  const [timelineTarget, setTimelineTarget] = useState<HTMLDivElement | null>(null);
  const [trackingTarget, setTrackingTarget] = useState<HTMLDivElement | null>(null);
  const readerFullscreen = useAnalystFullscreen();
  const [notice, setNotice] = useState("");
  const [now, setNow] = useState(Date.now());
  const [dataOpened, setDataOpened] = useState(false);
  const [showHistory, setShowHistory] = useState(false);
  const [configure, setConfigure] = useState(false);
  const [mobilePane, setMobilePane] = useState("reader");
  const [discussionContext, setDiscussionContext] = useState<AnalystDiscussionContext | null>(null);
  const previewGeneration = useRef(0);
  const workspace = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const element = workspace.current;
    if (!element) return;
    let frame = 0;
    const resize = () => {
      window.cancelAnimationFrame(frame);
      frame = window.requestAnimationFrame(() => {
        // Measure the actual header/configuration height so the composer stays within the viewport.
        const available = window.innerHeight - Math.max(16, element.getBoundingClientRect().top) - 24;
        element.style.setProperty("--analyst-workspace-height", `${Math.floor(Math.min(1100, Math.max(440, available)))}px`);
      });
    };
    const observer = new ResizeObserver(resize);
    if (element.parentElement) observer.observe(element.parentElement);
    window.addEventListener("resize", resize);
    resize();
    return () => { observer.disconnect(); window.removeEventListener("resize", resize); window.cancelAnimationFrame(frame); };
  }, []);

  async function loadModels() {
    const [rows, providers] = await Promise.all([api.listModelInstances(), api.listModelProviders()]);
    const allowed = new Set(providers.filter(p => p.enabled && p.provider_type !== "MOCK").map(p => p.id));
    const real = rows.filter(row => row.enabled && allowed.has(row.provider_id));
    setModels(real);
    setModel(current => real.some(row => row.instance_code === current) ? current : (real.find(row => row.availability_status === "AVAILABLE") || real[0])?.instance_code || "");
  }
  async function loadHistory() { setHistory(await analystApi.list()); }
  useEffect(() => {
    void loadModels().catch(e => setNotice(String(e.message)));
    void loadHistory().catch(e => setNotice(String(e.message)));
    const id = Number(saved("ai-analyst-active"));
    if (id) void analystApi.get(id).then(restoreRun).catch(() => remember("ai-analyst-active", ""));
  }, []);
  useEffect(() => { if (model) remember("ai-analyst-model", model); }, [model]);
  useEffect(() => {
    if (query.trim().length < 1 || (stock && query === stock.name)) { setMatches([]); return; }
    const controller = new AbortController();
    const timer = window.setTimeout(() => {
      void api.searchStocks(market, query.trim(), 12, { signal: controller.signal }).then(setMatches).catch(e => { if (e.name !== "AbortError") setNotice(e.message); });
    }, 250);
    return () => { window.clearTimeout(timer); controller.abort(); };
  }, [market, query, stock]);
  useEffect(() => {
    const generation = ++previewGeneration.current;
    setCoverage(null);
    if (!stock) { setPreviewing(false); return; }
    const controller = new AbortController();
    setPreviewing(true);
    void analystApi.preview({ market: stock.market, symbol: stock.symbol, period_months: months }, controller.signal)
      .then(row => { if (generation === previewGeneration.current) setCoverage(row); })
      .catch(e => { if (e.name !== "AbortError") setNotice(e.message); })
      .finally(() => { if (generation === previewGeneration.current) setPreviewing(false); });
    return () => controller.abort();
  }, [stock, months]);
  const isRunning = running(active);
  useEffect(() => {
    if (!active || !isRunning) return;
    let closed = false;
    const controller = new AbortController();
    const interval = window.setInterval(() => {
      void analystApi.get(active.id, controller.signal).then(row => {
        if (closed) return;
        setActive(row);
        if (!running(row)) {
          void loadHistory().catch(e => setNotice(e.message));
          if (row.status === "COMPLETED") setTab(current => current === "process" ? "result" : current);
        }
      }).catch(e => { if (!closed && e.name !== "AbortError") setNotice(`状态刷新失败：${e.message}；后台任务会继续执行。`); });
    }, 2000);
    return () => { closed = true; controller.abort(); window.clearInterval(interval); };
  }, [active?.id, isRunning]);
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);
  useEffect(() => {
    setEvidence([]);
    setConfirmedRevision(null);
    setLatestReview(null);
    setDataOpened(false); setDiscussionContext(null);
    if (!active) return;
    let closed = false;
    void analystApi.evidence(active.id).then(row => { if (!closed) setEvidence(row.items); }).catch(e => { if (!closed) setNotice(e.message); });
    return () => { closed = true; };
  }, [active?.id]);
  useEffect(() => {
    if (!showHistory) return;
    const close = (e: KeyboardEvent) => { if (e.key === "Escape") setShowHistory(false); };
    window.addEventListener("keydown", close);
    return () => window.removeEventListener("keydown", close);
  }, [showHistory]);

  function choose(row: StockSymbol) { setStock(row); setQuery(row.name); setMatches([]); setNotice(""); }
  function restoreRun(row: AnalystRun) {
    setActive(row);
    setStock({ market: row.market, symbol: row.symbol, name: row.name });
    setMarket(row.market);
    setQuery(row.name);
    setMonths(row.period_months);
    setMatches([]);
    setShowHistory(false); setMobilePane("reader"); setConfigure(false);
    remember("ai-analyst-active", String(row.id));
  }
  function discuss(text: string, evidenceId?: string) {
    void readerFullscreen.exit();
    setDiscussionContext({ text, evidenceId, nonce: Date.now() }); setMobilePane("chat");
  }
  async function start() {
    if (!stock || !model || submitting || isRunning) return;
    setSubmitting(true); setNotice("");
    const payload: AnalystRequest = { market: stock.market, symbol: stock.symbol, period_months: months, model_instance_code: model };
    try {
      const record = await analystApi.create(payload);
      setActive(record); remember("ai-analyst-active", String(record.id)); setTab("process");
      await loadHistory();
    } catch (e) { setNotice(e instanceof Error ? e.message : "提交分析失败"); }
    finally { setSubmitting(false); }
  }
  async function testModel() {
    const selected = models.find(row => row.instance_code === model);
    if (!selected) return;
    setTesting(true); setNotice("");
    try { const result = await api.testModelInstance(selected.id); await loadModels(); setNotice(result.status === "SUCCESS" ? "模型连通测试通过，状态已刷新。完整分析还需检查输出是否完整，以及分析结构和证据引用是否有效。" : `模型测试未通过：${result.message || "服务未返回有效结果"}`); }
    catch (e) { setNotice(e instanceof Error ? e.message : "模型测试失败"); }
    finally { setTesting(false); }
  }
  async function cancel() {
    if (!active) return;
    try { setActive(await analystApi.cancel(active.id)); await loadHistory(); }
    catch (e) { setNotice(e instanceof Error ? e.message : "取消失败"); }
  }
  const result = active?.result_json;
  const currentCoverage = active?.coverage_json;
  const selectedModel = models.find(row => row.instance_code === model);
  const modelName = models.find(row => row.instance_code === active?.model_instance_code)?.model_name || active?.model_instance_code;
  const elapsed = active ? Math.max(0, Math.floor(((active.completed_at ? dateValue(active.completed_at).getTime() : now) - dateValue(active.created_at).getTime()) / 1000)) : 0;
  const phase = active && ["FAILED", "CANCELLED"].includes(active.status) ? active.progress_json.last_stage : active?.progress_json.stage;
  const phases = [{ code: "PREPARED", label: "读取本地资料" }, { code: "MODEL", label: "AI综合研判" }, { code: "VALIDATING", label: "核验证据与保存" }];
  const phaseIndex = active?.status === "COMPLETED" ? 3 : Math.max(0, phases.findIndex(row => row.code === phase));
  const references = result?.evidence_chain || [];

  return <div className="ai-stock-analyst">
    <header className="analyst-intro"><span className="analyst-logo"><BrainCircuit size={25} /></span><div><h2>AI证券分析师</h2><p>阅读报告、查阅资料，与AI一起深入研究。</p></div><button type="button" className="analyst-secondary analyst-history-toggle" onClick={() => setShowHistory(true)}><History size={16} />分析历史</button></header>
    <details className="analyst-config analyst-box" open={!active || configure} onToggle={e => setConfigure(e.currentTarget.open)}>
      <summary>股票与模型设置 <span>{active ? `${active.name} · ${selectedModel?.model_name || model} · 更换股票或模型` : "选择股票，开始研究"}</span></summary>
      <div className="analyst-fields">
        <label>市场<select aria-label="分析市场" value={market} onChange={e => { setMarket(e.target.value); setStock(null); setQuery(""); }}><option value="ALL">全部市场</option><option value="CN_A">A股</option><option value="HK">港股</option><option value="NEEQ">新三板</option><option value="NEEQ_INNOVATION">创新层</option></select></label>
        <label className="analyst-search">分析哪只股票<div className="analyst-search-input"><Search size={17} /><input aria-label="搜索分析股票" value={query} onChange={e => { setStock(null); setQuery(e.target.value); }} placeholder="名称 / 代码 / 拼音 / 首字母" autoComplete="off" /></div>
          {!!matches.length && <div className="analyst-matches">{matches.map(row => <button type="button" key={`${row.market}:${row.symbol}`} onClick={() => choose(row)}><strong>{row.name}</strong><span>{row.market} · {row.symbol}</span></button>)}</div>}
        </label>
        <label>回看资料周期<select aria-label="回看资料周期" value={months} onChange={e => setMonths(Number(e.target.value))}>{[3, 6, 12, 24, 36, 60].map(value => <option key={value} value={value}>近{value}个月</option>)}</select></label>
        <label className="analyst-model">分析模型<select aria-label="证券分析模型" value={model} onChange={e => setModel(e.target.value)}><option value="" disabled>请选择真实AI模型</option>{models.map(row => <option key={row.id} value={row.instance_code}>{row.model_name} · {row.availability_label || "待测试"}</option>)}</select></label>
        <button type="button" className="analyst-secondary" disabled={!model || testing || isRunning} onClick={() => void testModel()}>{testing ? "测试中…" : "测试模型"}</button>
        <button type="button" className="analyst-primary" disabled={!stock || !model || !coverage || coverage.evidence_count < 3 || previewing || submitting || isRunning} onClick={() => void start()}><BrainCircuit size={17} />{submitting ? "正在提交…" : isRunning ? "后台分析中" : active?.status === "FAILED" ? "重新分析" : "开始AI分析"}</button>
      </div>
      <div className="analyst-config-note">{stock ? `${stock.name}（${stock.symbol}）` : "支持中文、代码、英文及拼音查找股票。"}<span>{selectedModel?.availability_reason || "基于本地已有数据；不会自动重复采集。"}</span></div>
      {previewing && <p role="status">正在读取本地资料覆盖情况…</p>}
      {coverage && <div className="analyst-preview"><div><strong>本次可读 {coverage.evidence_count} 条资料摘要</strong><span>按类别控制输入长度，行情为区间汇总；财务和公司背景可追溯更早报告。</span></div><div className="analyst-coverage-pills">{Object.entries(coverage.counts).map(([key, count]) => <span className={count ? "has-data" : "no-data"} key={key} title={`本次读取${count}条；本地范围内${coverage.total_counts[key] ?? count}条`}>{categories[key] || key} <b>{count}</b></span>)}</div></div>}
    </details>
    {notice && <div className="analyst-notice" role="status">{notice}<button type="button" aria-label="关闭提示" onClick={() => setNotice("")}>×</button></div>}
    {active?.status === "COMPLETED" && <div className="analyst-mobile-switch"><button className={mobilePane === "reader" ? "selected" : ""} onClick={() => setMobilePane("reader")}><FileText size={15} />报告与资料</button><button className={mobilePane === "chat" ? "selected" : ""} onClick={() => setMobilePane("chat")}><MessageCircle size={15} />AI讨论与跟踪</button></div>}
    <div ref={workspace} className={`analyst-layout mobile-${mobilePane}`}>
      <main ref={readerFullscreen.ref} className={`analyst-main analyst-box ${readerFullscreen.className}`}>
        {!active ? <div className="analyst-empty"><BrainCircuit size={44} /><h3>从一只股票开始</h3><p>选择股票与模型，AI将综合主营、财务、新闻、公告及市场表现，给出短、中、长期可能趋势和依据。</p><p>分析历史会单独保存，随时可以回来查看。</p></div> : <>
          <header className="analyst-run-header"><div><button className="analyst-stock-link" type="button" onClick={() => { setTab("stock"); setDataOpened(true); }}>{active.name} <small>{active.symbol}</small><ArrowUpRight size={16} /></button><p>{modelName} · 近{active.period_months}个月资料 · {when(active.created_at)}</p></div><div className="analyst-actions"><span className={`analyst-status ${active.status.toLowerCase()}`}>{statuses[active.status] || active.status}</span><button type="button" className="analyst-icon-button" aria-label={readerFullscreen.fullscreen ? "退出全屏分析结论" : "全屏分析结论"} title={readerFullscreen.fullscreen ? "退出全屏（Esc）" : "全屏阅读"} onClick={() => void readerFullscreen.toggle()}>{readerFullscreen.fullscreen ? <Minimize2 size={18} /> : <Maximize2 size={18} />}</button></div></header>
          <div className="analyst-tabs" role="tablist" aria-label="分析内容"><button role="tab" aria-selected={tab === "result"} onClick={() => setTab("result")}>分析结论</button><button role="tab" aria-selected={tab === "stock"} onClick={() => { setTab("stock"); setDataOpened(true); }}>股票资料</button><button role="tab" aria-selected={tab === "evidence"} onClick={() => setTab("evidence")}>依据与数据 <small>{currentCoverage?.evidence_count || 0}</small></button><button role="tab" aria-selected={tab === "process"} onClick={() => setTab("process")}>执行记录</button><button role="tab" aria-selected={tab === "timeline"} onClick={() => setTab("timeline")}>时间轴</button><button role="tab" aria-selected={tab === "tracking"} onClick={() => setTab("tracking")}>跟踪复盘</button></div>
          <div className="analyst-reader-scroll" hidden={tab !== "result"} aria-label="分析报告阅读区">
          {isRunning && <div className="analyst-live" role="status"><RefreshCw size={17} className="analyst-spin" /><strong>{active.progress_json.message}</strong><span>已用时 {elapsed} 秒</span><button type="button" onClick={() => void cancel()}>取消分析</button></div>}
          {active.status === "FAILED" && <div className="analyst-error"><h3>本次模型分析未成功</h3><p>{active.error_message}</p><p>证据快照已保留。可测试或更换上方模型，然后点击“重新分析”；失败记录不会生成替代结论。</p></div>}
          {active.status === "CANCELLED" && <p className="analyst-notice">已取消分析。已发出的模型请求可能仍计费，返回结果不再发布。</p>}
          {(active.status === "COMPLETED" && result?.executive_summary ? <div className="analyst-result">
            {latestReview?.result_json.judgment && <section className="analyst-confirmed-report analyst-daily-current"><header><h3>最新跟踪判断 · {latestReview.business_date}</h3><small>{latestReview.status === "PARTIAL" ? "部分证据可用" : "每日复核已完成"}</small></header><p>{latestReview.result_json.judgment}</p><small>每日复核意见单独留存，可在下方时间轴查看变化与依据。</small></section>}
            {confirmedRevision && <section className="analyst-confirmed-report"><header><h3>您确认的判断与关注点</h3><small>第{confirmedRevision.version}版 · {when(confirmedRevision.created_at)}</small></header><p>{confirmedRevision.judgment}</p><ul>{confirmedRevision.focus_points_json.map((point, i) => <li key={i}>{point}</li>)}</ul><small>后续每日跟踪将以此为依据，原始AI研判保留在下方。</small></section>}
            <section className="analyst-verdict"><div><span>整体研判</span><strong className={`direction-${result.overall_direction}`}>{directions[result.overall_direction || ""] || "方向待定"}</strong><small>证据支持度 {Math.round((result.overall_confidence || 0) * 100)}% · 不代表涨跌概率</small></div><p>{result.executive_summary}</p><button type="button" className="analyst-text-button" onClick={() => discuss(`请结合报告的整体研判，进一步解释：${result.executive_summary}`)}><MessageCircle size={14} />讨论这条结论</button></section>
            <section className="analyst-narrative"><h3>公司现状与投资逻辑</h3><p>{result.current_assessment}</p></section>
            <div className="analyst-scenarios">{[...(result.scenarios || [])].sort((a, b) => ["SHORT", "MEDIUM", "LONG"].indexOf(a.horizon) - ["SHORT", "MEDIUM", "LONG"].indexOf(b.horizon)).map(scenario => <section key={scenario.horizon} className="analyst-scenario"><div><h3>{scenario.horizon_label}</h3><span className={`direction-${scenario.direction}`}>{directions[scenario.direction]}</span></div><p>{scenario.thesis}</p><small>证据支持度 {Math.round(scenario.confidence * 100)}%</small><h4>成立条件</h4><ul>{scenario.conditions.map((value, i) => <li key={i}>{value}</li>)}</ul><h4>需要改变判断的信号</h4><ul>{scenario.invalidation_conditions.map((value, i) => <li key={i}>{value}</li>)}</ul><button type="button" className="analyst-text-button" onClick={() => setTab("evidence")}>查看依据 · {scenario.evidence.length}条</button></section>)}</div>
            <div className="analyst-two-cols"><Points title="核心驱动与近期催化" items={[...(result.core_drivers || []), ...(result.catalysts || [])]} /><Points title="主要风险与竞争性解释" items={result.key_risks} /></div><Points title="接下来重点观察" items={result.watch_items} /><p className="analyst-disclaimer">{result.disclaimer}</p>
          </div> : <div className="analyst-empty"><Clock3 size={32} /><p>{isRunning ? "AI正在综合阅读资料。完成后，这里将展示整体判断和三个时间窗口的趋势。" : "本次没有可发布的分析结论。"}</p></div>)}
          </div>
          <div className="analyst-reader-scroll" hidden={tab !== "stock"}>{dataOpened && <><button type="button" className="analyst-text-button" onClick={() => discuss(`请结合${active.name}（${active.symbol}）最新本地股票资料和原分析报告，讨论资料中的变化及其对判断的影响。`)}><MessageCircle size={14} />结合股票资料与AI讨论</button><StockDetailDrawer key={active.id} embedded stock={{ market: active.market, symbol: active.symbol, name: active.name }} onClose={() => setTab("result")} /></>}</div>
          <div className="analyst-reader-scroll" hidden={tab !== "evidence"}><div className="analyst-evidence"><h3>本次实际分析输入</h3><p className="analyst-muted">{evidence.length}条资料摘要，保留分析当时的快照。摘要不等同于全文阅读，候选公司关系不代表已验证事实。</p><div className="analyst-coverage-grid">{Object.entries(currentCoverage?.counts || {}).map(([key, count]) => <div key={key}><span>{categories[key] || key}</span><strong>{count}<small> / {currentCoverage?.total_counts?.[key] ?? count}</small></strong><small>本次摘要 / 本地范围内记录</small></div>)}</div>
            {!!result?.data_limitations?.length && <details open><summary>资料范围与分析限制</summary><ul>{result.data_limitations.map((text, i) => <li key={i}>{text}</li>)}</ul></details>}
            {evidence.map(row => <details key={row.evidence_id} className="analyst-evidence-row"><summary><span className="analyst-tag">{row.category_name}</span><strong>{row.title}</strong><small>{row.observed_at?.slice(0, 10) || "日期未注明"}</small></summary><div><p className="analyst-muted">来源：{row.source || "本地资料"} · 证据编号：{row.evidence_id}</p>{references.filter(ref => ref.evidence_id === row.evidence_id).map((ref, i) => <p className="analyst-citation" key={i}>AI引用：{ref.claim}</p>)}{row.excerpt && <p>{row.excerpt}</p>}<button type="button" className="analyst-text-button" onClick={() => discuss(`请解读这条资料，并说明它是否改变报告判断：${row.title}`, row.evidence_id)}>请AI解读这条资料</button>{sourceUrl(row.data?.url) && <a href={sourceUrl(row.data?.url)} target="_blank" rel="noreferrer">查看来源原文 ↗</a>}{row.data && <details><summary>查看原始数据摘录</summary><pre>{JSON.stringify(row.data, null, 2)}</pre></details>}</div></details>)}
          </div></div>
          <div className="analyst-reader-scroll" hidden={tab !== "process"}><div className="analyst-process"><div className="analyst-steps">{phases.map((row, i) => <div key={row.code} className={`${i < phaseIndex ? "done" : i === phaseIndex ? "current" : ""} ${active.status === "FAILED" && i === phaseIndex ? "failed" : ""}`}><span>{i + 1}</span><strong>{row.label}</strong></div>)}</div><p>开始：{when(active.created_at)} · 结束：{when(active.completed_at)} · 总耗时：{elapsed}秒</p><p className="analyst-muted">模型调用阶段显示真实等待时间，不用模拟百分比代表模型内部进度。</p><ol>{(active.progress_json.events || []).map((event, i) => <li key={i}><time>{when(event.at)}</time><span>{event.message}</span></li>)}</ol><details><summary>分析来源与版本</summary><p>模型：{modelName} · 调用记录：{active.model_call_log_id || "尚未返回"}</p><p>分析技能：资深证券分析师·趋势研判 · 版本 {active.skill_version}</p></details></div></div>
          <div ref={setTimelineTarget} className="analyst-reader-scroll analyst-tracking-pane" hidden={tab !== "timeline"} aria-label="每日观察时间轴">{active.status !== "COMPLETED" && <p className="analyst-muted">完成有效分析后即可设置每日观察。</p>}</div>
          <div ref={setTrackingTarget} className="analyst-reader-scroll analyst-tracking-pane" hidden={tab !== "tracking"} aria-label="目标价与跟踪复盘">{active.status !== "COMPLETED" && <p className="analyst-muted">完成有效分析后即可开展跟踪复盘。</p>}</div>
        </>}
      </main>
      {active?.status === "COMPLETED" ? <AnalystFollowup key={active.id} analysis={active} model={model} onRevision={setConfirmedRevision} onReview={setLatestReview} context={discussionContext} onClearContext={() => setDiscussionContext(null)} timelineTarget={timelineTarget} trackingTarget={trackingTarget} onNavigate={view => { setTab(view); setMobilePane("reader"); }} /> : <aside className="analyst-chat-placeholder analyst-box"><MessageCircle size={28} /><h3>AI研究伙伴</h3><p>分析完成后，可在这里持续讨论。报告和股票资料将在左侧独立展示。</p></aside>}
    </div>
    {showHistory && <div className="analyst-modal-backdrop" onClick={() => setShowHistory(false)}><aside className="analyst-history analyst-modal" role="dialog" aria-modal="true" aria-label="分析历史" onClick={e => e.stopPropagation()}><header><h3><History size={17} />分析历史</h3><div className="analyst-actions"><button type="button" aria-label="刷新分析历史" onClick={() => void loadHistory().catch(e => setNotice(e.message))}><RefreshCw size={16} /></button><button type="button" aria-label="关闭分析历史" onClick={() => setShowHistory(false)}><X size={18} /></button></div></header><p className="analyst-muted">最近50次 · 每份报告与对话独立保存</p>{!history.length && <p className="analyst-muted">暂无历史记录</p>}{history.map(row => <button type="button" disabled={isRunning && row.id !== active?.id} className={`analyst-history-row ${active?.id === row.id ? "selected" : ""}`} key={row.id} onClick={() => { restoreRun(row); setTab("result"); }}><div><strong>{row.name}</strong><span className={`analyst-status ${row.status.toLowerCase()}`}>{statuses[row.status]}</span></div><small>{row.market} · {row.symbol} · {when(row.created_at)}</small><p>{row.result_json.executive_summary || row.progress_json.message || "等待分析"}</p></button>)}</aside></div>}
  </div>;
}
