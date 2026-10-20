import { useEffect, useState } from "react";
import { SelectionWorkbench as AdvancedWorkbench } from "./SelectionWatch";
import { selectionApi, type BatchAcceptance, type SelectionCandidate, type SelectionRecord, type SelectionRun, type SelectionTracking } from "./selectionApi";
import "./SelectionWatch.css";

const message = (error: unknown) => error instanceof Error ? error.message : "请求失败，请重试";
const number = (value: number | null | undefined) => value == null ? "—" : value.toFixed(2);
const text = (value: unknown) => typeof value === "string" || typeof value === "number" ? String(value) : "";
const rows = (value: unknown): SelectionRecord[] => Array.isArray(value) ? value.filter(item => item && typeof item === "object") : [];
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
  ISSUED_BY: "发行主体", HAS_NEWS: "相关新闻", HAS_NOTICE: "相关公告", HAS_FINANCIAL_REPORT: "财务报告",
  HAS_PRICE_AND_VOLUME_SERIES: "量价序列", HAS_FINANCIAL_METRIC: "财务指标", HAS_F10_SECTION: "F10资料",
  HAS_REALTIME_QUOTE: "行情快照", SUPPORTED_BY: "证据支持", EVIDENCED_BY: "证据来源", DESCRIBED_BY: "资料描述",
};
const label = (value: string) => labels[value] || value;
const isRunning = (run: SelectionRun | null) => !!run && ["QUEUED", "RUNNING"].includes(run.status);

function Fact({ fact }: { fact: SelectionRecord }) {
  const values = Object.entries(fact.properties || {}).filter(([, value]) => value != null && typeof value !== "object");
  return <article className="batch-fact"><strong>{text(fact.title || fact.fact_type)}</strong><small>{label(text(fact.source_table))} · {label(text(fact.status))}</small>
    <dl>{values.slice(0, 12).map(([key, value]) => <div key={key}><dt>{label(key)}</dt><dd>{text(value)}</dd></div>)}</dl></article>;
}

export function StockBatchWorkbench({ mode }: { mode: "selection" | "tracking" }) {
  const [acceptance, setAcceptance] = useState<BatchAcceptance | null>(null);
  const [runs, setRuns] = useState<SelectionRun[]>([]);
  const [run, setRun] = useState<SelectionRun | null>(null);
  const [tracking, setTracking] = useState<SelectionTracking[]>([]);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [advanced, setAdvanced] = useState(false);
  const [showData, setShowData] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [detail, setDetail] = useState<SelectionCandidate | null>(null);
  const [detailTab, setDetailTab] = useState("conclusion");
  const [targetPrice, setTargetPrice] = useState("");
  const running = isRunning(run);

  useEffect(() => {
    let valid = true;
    void Promise.all([selectionApi.acceptance(), selectionApi.listRuns(), selectionApi.listTracking()])
      .then(async ([data, nextRuns, nextTracking]) => {
        if (!valid) return;
        setAcceptance(data); setRuns(nextRuns); setTracking(nextTracking);
        const latest = nextRuns.find(item => item.universe_scope === "FIXED20_ACCEPTANCE");
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
        if (isRunning(next)) timer = setTimeout(() => void poll(), 2000);
        else { setRuns(await selectionApi.listRuns()); setNotice(next.analysis_mode === "MODEL_COMPLETED" ? `真实模型已完成${next.candidate_count}只股票的分析，请核对证据。` : "执行已结束，请查看失败原因。"); }
      } catch (err) { if (valid) { setError(`进度读取失败，将重试：${message(err)}`); timer = setTimeout(() => void poll(), 4000); } }
    };
    timer = setTimeout(() => void poll(), 1000);
    return () => { valid = false; clearTimeout(timer); };
  }, [running, run?.id]);

  useEffect(() => {
    if (!detail) return;
    const close = (event: KeyboardEvent) => { if (event.key === "Escape" && !busy) setDetail(null); };
    window.addEventListener("keydown", close);
    return () => window.removeEventListener("keydown", close);
  }, [!!detail, busy]);

  async function start() {
    if (!acceptance?.prepared || !acceptance.graph_id || !acceptance.knowledge_base_id) { setError("20股数据门禁尚未通过，请查看验收明细。"); return; }
    setBusy(true); setError(""); setNotice("");
    try {
      const created = await selectionApi.startRun({ market: acceptance.market, symbols: acceptance.symbols,
        universe_scope: "FIXED20_ACCEPTANCE", candidate_limit: 20, min_abs_change_pct: 0, min_volume_ratio: 0,
        model_instance_code: null, knowledge_base_ids: [acceptance.knowledge_base_id], graph_ids: [acceptance.graph_id], use_model: true, data_mode: "REAL" });
      setRun(created); setRuns(current => [created, ...current]); setNotice("已提交后台，刷新页面后仍可查看执行进度。");
    } catch (err) { setError(message(err)); } finally { setBusy(false); }
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

  async function refreshTracking() {
    setBusy(true); setError("");
    try { const result = await selectionApi.refresh(); setTracking(await selectionApi.listTracking()); setNotice(`刷新完成：${result.completed_count}只完成，${result.pending_data_count}只等待行情。`); }
    catch (err) { setError(message(err)); } finally { setBusy(false); }
  }

  const modelCount = run?.candidates.filter(item => item.analysis_json.model_output_status === "SUCCESS").length || 0;
  const progress = (run?.criteria_json.progress || {}) as SelectionRecord;
  const fixedRuns = runs.filter(item => item.universe_scope === "FIXED20_ACCEPTANCE");
  const evidence = detail?.evidence_json;
  const model = (detail?.analysis_json.model || {}) as SelectionRecord;
  const factList = evidence ? [...rows(evidence.facts), ...Object.values(evidence.structured_observations || {}).flatMap(rows)]
    .filter((item, index, all) => all.findIndex(other => other.id === item.id) === index) : [];

  if (advanced) return <><button className="batch-return" onClick={() => setAdvanced(false)}>← 返回20股简洁工作台</button><AdvancedWorkbench mode={mode} /></>;
  return <main className="batch-workbench">
    <header className="batch-heading"><div><p className="eyebrow">固定样本 · 可追溯分析</p><h1>20 股全链路工作台</h1><p>看数据 → 看分析与证据 → 确认跟踪 → 复盘改进</p></div><div className="batch-actions"><button onClick={() => setAdvanced(true)}>高级工作台</button>
      {mode === "selection" ? <button className="primary-button" disabled={busy || loading || running || !acceptance?.prepared} onClick={() => void start()}>{running ? "正在后台分析…" : busy ? "提交中…" : run ? "重新分析20股" : "开始分析20股"}</button> : <button className="primary-button" disabled={busy || loading} onClick={() => void refreshTracking()}>刷新跟踪</button>}</div></header>
    {error && <p className="batch-alert error" role="alert">{error}</p>}{notice && <p className="batch-alert" role="status">{notice}</p>}
    <section className="batch-summary" aria-label="全链路进度">
      <div><small>① 股票与公司</small><strong>{acceptance?.rows.filter(item => item.company_linked).length || 0} / 20</strong></div>
      <div><small>② 湖仓质量</small><strong>{acceptance ? Object.keys(acceptance.datasets).length : 0} 个数据集</strong></div>
      <div><small>③ 知识与证据</small><strong>{acceptance?.rows.filter(item => item.passed).length || 0} / 20 可读通</strong></div>
      <div><small>④ 真实模型分析</small><strong>{modelCount} / {run?.candidate_count || 20}</strong></div>
      <div><small>⑤ 确认后跟踪</small><strong>{tracking.length} 只 · {tracking.filter(item => item.status === "COMPLETED").length} 已复盘</strong></div>
    </section>
    <div className="batch-scope"><p className="batch-note">本轮为固定20只A股的本地数据验证。行情基准日期逐股显示；来源投影与切片可读通，不代表新闻事件已人工核验。语义向量尚未生成。</p><button onClick={() => setShowData(!showData)} aria-expanded={showData}>{showData ? "收起数据验收" : "查看20股数据验收"}</button></div>
    {showData && <section className="batch-card"><div className="batch-card-title"><h2>20股数据验收</h2><span>实际读取结果 · 图谱 #{acceptance?.graph_id}</span></div><div className="batch-table-wrap"><table><thead><tr><th>股票</th><th>公司映射</th><th>证据文档</th><th>载入切片</th><th>事实与观测</th><th>图谱路径</th><th>血缘</th><th>状态</th></tr></thead><tbody>{acceptance?.rows.map(item => <tr key={item.symbol}><td><strong>{item.name}</strong><small>{item.symbol}</small></td><td>{item.company_linked ? "已关联" : "缺失"}</td><td>{item.documents}</td><td>{item.selected_chunks}</td><td>{item.facts}</td><td>{item.relations}</td><td>{item.lineage}</td><td><span className={`batch-badge ${item.passed ? "" : "warning"}`}>{item.passed ? "链路通过" : "待处理"}</span></td></tr>)}</tbody></table></div><p className="batch-note">切片按上下文预算选取，未载入全部切片不等于切片失败。本轮复用已有采集数据，未重新采集全量远程数据。</p></section>}
    {loading && <p className="batch-empty" role="status">正在读取20股数据与历史分析…</p>}
    {!loading && !run && <p className="batch-empty">{acceptance?.prepared ? "数据准备已通过，请点击“开始分析20股”。" : "数据准备尚未完成，请查看验收状态。"}</p>}
    {run && <section className="batch-card"><div className="batch-card-title"><div><h2>{running ? "正在分析" : "本轮结果"} <span className="batch-badge">{label(run.status)}</span></h2><span>分析时间 {new Date(run.as_of).toLocaleString("zh-CN")} · {run.model_instance_code || "自动模型路由"}</span></div><select aria-label="历史分析批次" disabled={running} value={run.id} onChange={event => { setError(""); void selectionApi.getRun(Number(event.target.value)).then(setRun).catch(err => setError(message(err))); }}>{fixedRuns.map(item => <option key={item.id} value={item.id}>第{item.id}轮 · {label(item.status)}</option>)}</select></div>
      {running && <div className="batch-progress" role="status"><div><strong>{text(progress.message) || "等待后台执行"}</strong><span>{Number(progress.percent || 0)}%</span></div><progress aria-label="分析进度" max="100" value={Number(progress.percent || 0)} /><small>可离开页面，任务会在后台继续执行。</small></div>}
      {run.error_message && <p className="batch-alert error">{run.error_message} <button disabled={busy || running} onClick={() => void start()}>重试分析</button></p>}
      {run.missing_data_json.length > 0 && <details className="batch-missing"><summary>数据缺口与限制（{run.missing_data_json.length}）</summary><ul>{run.missing_data_json.map((item, index) => <li key={index}>{item}</li>)}</ul></details>}
      <div className="batch-table-wrap"><table><thead><tr><th>股票</th><th>行情基准</th><th>模型判断</th><th>证据</th><th>确认状态</th><th>操作</th></tr></thead><tbody>{run.candidates.map(item => {
        const success = item.analysis_json.model_output_status === "SUCCESS";
        return <tr key={item.id}><td><strong>{item.name || item.symbol}</strong><small>{item.symbol}</small></td><td>{number(item.entry_price)} 元<small>{item.entry_date || "缺少日期"}</small></td><td><span className={`batch-badge ${success ? "" : "warning"}`}>{success ? label(text(item.analysis_json.recommendation)) : label(text(item.analysis_json.model_output_status))}</span><small className="batch-clamp">{success ? text(item.analysis_json.summary) : "尚无有效模型结论"}</small></td><td>{rows(item.evidence_json.documents).length} 篇文档<small>{rows(item.evidence_json.facts).length} 条事实与观测</small></td><td>{label(item.decision)}</td><td><button onClick={() => { setDetail(item); setDetailTab("conclusion"); setTargetPrice(text(item.analysis_json.target_price)); }}>分析与证据</button></td></tr>;
      })}</tbody></table>{!run.candidates.length && !running && <p className="batch-empty">本轮无可分析结果，请检查数据与失败原因。</p>}</div></section>}
    {mode === "tracking" && <section className="batch-card"><div className="batch-card-title"><div><h2>跟踪与复盘</h2><span>从人工确认后的有效交易日开始，当前为10日规则。</span></div><button onClick={() => setAdvanced(true)}>查看详细复盘与Skill回归</button></div><div className="batch-table-wrap"><table><thead><tr><th>股票</th><th>目标 / 最新价</th><th>有效交易日</th><th>累计收益</th><th>最大回撤</th><th>状态</th></tr></thead><tbody>{tracking.map(item => <tr key={item.id}><td>{item.symbol}</td><td>{number(item.target_price)} / {number(item.latest_price)}</td><td>{item.observed_sessions} / {item.required_sessions}</td><td>{number(item.cumulative_return_pct)}%</td><td>{number(item.max_drawdown_pct)}%</td><td>{label(item.status)}</td></tr>)}</tbody></table>{!tracking.length && <p className="batch-empty">确认入选后自动建立跟踪。尚未发生的交易日不会生成收益结果。</p>}</div></section>}
    {detail && <div className="batch-overlay" onMouseDown={event => { if (event.target === event.currentTarget && !busy) setDetail(null); }}><section className="batch-detail" role="dialog" aria-modal="true" aria-label={`${detail.name || detail.symbol}分析与证据`}>
      <header><div><h2>{detail.name || detail.symbol}</h2><span>{detail.symbol} · 行情 {detail.entry_date} · {number(detail.entry_price)} 元</span></div><button disabled={busy} onClick={() => setDetail(null)}>关闭</button></header>
      <nav className="batch-tabs" aria-label="研判详情分类">{[["conclusion", "分析结论"], ["facts", "数据事实"], ["documents", "原文与引用"], ["trace", "关系与血缘"]].map(([key, title]) => <button key={key} className={detailTab === key ? "active" : ""} aria-pressed={detailTab === key} onClick={() => setDetailTab(key)}>{title}</button>)}</nav>
      {detailTab === "conclusion" && <><h3>模型分析</h3>{detail.analysis_json.model_output_status === "SUCCESS" ? <><p>{text(detail.analysis_json.summary)}</p><p>判断：{label(text(detail.analysis_json.recommendation))} · 置信度：{typeof detail.analysis_json.confidence === "number" ? `${Math.round(detail.analysis_json.confidence * 100)}%` : "—"}</p><p>模型推断目标价 {number(Number(model.target_price) || null)} 元 · 观察止损 {number(Number(model.stop_price) || null)} 元 · 10个交易日</p></> : <p className="batch-alert error">模型未完成，请重新分析后再确认入选。</p>}<h3>数据边界</h3><ul>{(Array.isArray(evidence?.missing_data) ? evidence.missing_data : []).map((item, index) => <li key={index}>{String(item)}</li>)}</ul><p className="batch-note">Skill：{text(detail.analysis_json.skill_code)} / {text(detail.analysis_json.skill_version)}<br />模型：{text(detail.analysis_json.actual_instance_code || detail.analysis_json.model_version)}</p></>}
      {detailTab === "facts" && <><h3>股票与公司</h3><p>{text(evidence?.identity?.company?.name) || "公司名称未提供"}</p><p className="batch-note">{text(evidence?.identity?.canonical_security_id)} · {text(evidence?.identity?.canonical_company_id)}</p><h3>数值与事实</h3><div className="batch-facts">{factList.slice(0, 35).map((fact, index) => <Fact key={`${fact.id}-${index}`} fact={fact} />)}</div></>}
      {detailTab === "documents" && <><p className="batch-note">模型引用文档：{(model.evidence_document_ids || []).join("、") || "无"} · 事实 {model.evidence_fact_ids?.length || 0} 条 · 切片 {model.evidence_chunk_ids?.length || 0} 条</p>{rows(evidence?.documents).map((doc, index) => <details key={index}><summary>{text(doc.title) || "证据文档"}</summary><small>{label(text(doc.source_table))} · 文档 #{doc.id} · {text(doc.published_at)}</small><p>{text(doc.excerpt || doc.content)}</p>{doc.url && <a href={text(doc.url)} target="_blank" rel="noreferrer">查看原文</a>}</details>)}<h3>本次载入的切片</h3>{rows(evidence?.chunks).map((chunk, index) => <details key={index}><summary>{text(chunk.section_title) || `片段${index + 1}`} · 向量{label(text(chunk.embedding_status))}</summary><p>{text(chunk.text)}</p></details>)}</>}
      {detailTab === "trace" && <><h3>图谱关系</h3>{rows(evidence?.graph_paths).slice(0, 12).map((path, index) => <p key={index}>{text(path.subject_name)} → {label(text(path.predicate))} → {text(path.object_name)}<br /><small>证据文档 #{path.evidence_document_id} · {label(text(path.verification_status))}</small></p>)}<h3>血缘追踪</h3><p className="batch-note">{rows(evidence?.lineage).length} 条血缘记录 · {rows(evidence?.lakehouse_datasets).length} 个湖仓版本</p>{rows(evidence?.lineage).slice(0, 10).map((item, index) => <details key={index}><summary>{text(item.transformation)}</summary><p>{text(item.upstream_type)}:{text(item.upstream_id)} → {text(item.downstream_type)}:{text(item.downstream_id)}</p></details>)}<p className="batch-note">上下文版本：{text(evidence?.context_version)}<br />图谱版本：{text(evidence?.graph_version)}</p></>}
      {detail.decision === "PENDING" && <footer><label>确认目标价<input aria-label="确认目标价" type="number" min="0.000001" step="any" value={targetPrice} onChange={event => setTargetPrice(event.target.value)} /></label><button disabled={busy} onClick={() => void review("REJECTED")}>排除</button><button className="primary-button" disabled={busy || !(Number(targetPrice) > 0) || detail.analysis_json.model_output_status !== "SUCCESS"} onClick={() => void review("APPROVED")}>确认并跟踪</button></footer>}
    </section></div>}
  </main>;
}
