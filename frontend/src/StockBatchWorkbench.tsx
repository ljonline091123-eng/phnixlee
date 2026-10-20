import { useEffect, useMemo, useState } from "react";
import { api, type KnowledgeBase, type KnowledgeGraph } from "./api";
import { selectionApi, type SelectionCandidate, type SelectionRun, type SelectionTracking } from "./selectionApi";
import "./SelectionWatch.css";

type Mode = "selection" | "tracking";
const message = (error: unknown) => error instanceof Error ? error.message : "请求失败，请重试";
const pct = (value: number | null | undefined) => value == null ? "—" : `${value.toFixed(2)}%`;
const price = (value: number | null | undefined) => value == null ? "—" : value.toFixed(2);
const statusText: Record<string, string> = {
  REVIEW: "待复核", TRACKING: "跟踪中", COMPLETED: "已完成", PARTIAL: "数据不足", APPROVED: "已确认",
  REJECTED: "已排除", PENDING: "待复核", PENDING_DATA: "等待行情", FAILED: "失败",
};

export function StockBatchWorkbench({ mode }: { mode: Mode }) {
  const [market, setMarket] = useState("CN_A");
  const [runs, setRuns] = useState<SelectionRun[]>([]);
  const [run, setRun] = useState<SelectionRun | null>(null);
  const [tracking, setTracking] = useState<SelectionTracking[]>([]);
  const [bases, setBases] = useState<KnowledgeBase[]>([]);
  const [graphs, setGraphs] = useState<KnowledgeGraph[]>([]);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [detail, setDetail] = useState<SelectionCandidate | null>(null);
  const [targetPrice, setTargetPrice] = useState("");

  async function reload() {
    const [nextRuns, nextTracking] = await Promise.all([selectionApi.listRuns(), selectionApi.listTracking()]);
    setRuns(nextRuns);
    setTracking(nextTracking);
    if (run) setRun(await selectionApi.getRun(run.id));
    else if (nextRuns.length) setRun(await selectionApi.getRun(nextRuns[0].id));
  }
  useEffect(() => {
    let active = true;
    void Promise.all([selectionApi.listRuns(), selectionApi.listTracking(), api.listKnowledgeBases(), api.listKnowledgeGraphs()])
      .then(async ([nextRuns, nextTracking, nextBases, nextGraphs]) => {
        if (!active) return;
        setRuns(nextRuns); setTracking(nextTracking); setBases(nextBases); setGraphs(nextGraphs);
        if (nextRuns.length) setRun(await selectionApi.getRun(nextRuns[0].id));
      })
      .catch(err => { if (active) setError(message(err)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  const governedGraph = useMemo(() => graphs.find(item => item.enabled && ["GOVERNED", "LOCKED"].includes(item.governance_status)), [graphs]);
  const governedBase = bases.find(item => item.enabled && item.id === governedGraph?.knowledge_base_id);
  const pending = run?.candidates.filter(item => item.decision === "PENDING").length ?? 0;
  const completed = tracking.filter(item => item.status === "COMPLETED").length;

  async function startRun() {
    if (!governedBase || !governedGraph) {
      setError("没有已治理的知识图谱，无法启动真实模型分析。请先完成知识治理。");
      return;
    }
    setBusy(true); setError(""); setNotice("");
    try {
      const created = await selectionApi.createRun({
        market, symbols: [], candidate_limit: 20, min_abs_change_pct: 0, min_volume_ratio: 0,
        model_instance_code: null, knowledge_base_ids: [governedBase.id], graph_ids: [governedGraph.id], use_model: true, data_mode: "REAL",
      });
      setRun(created); setRuns(current => [created, ...current]);
      setNotice(created.analysis_mode === "MODEL_COMPLETED"
        ? `本轮已完成：实际研判 ${created.candidate_count} 只股票；未达 20 只会由行情覆盖或筛选结果说明。`
        : `本轮已结束：${created.analysis_mode === "MODEL_UNAVAILABLE" ? "真实模型不可用" : "模型未完成分析"}，请查看结果状态与失败原因。`);
    } catch (err) { setError(message(err)); }
    finally { setBusy(false); }
  }
  async function refreshTracking() {
    setBusy(true); setError(""); setNotice("");
    try {
      const result = await selectionApi.refresh();
      await reload();
      setNotice(`跟踪已刷新：${String(result.completed_count ?? 0)} 只完成，${String(result.pending_data_count ?? 0)} 只等待行情。未满 10 个有效交易日不会提前判定。`);
    } catch (err) { setError(message(err)); }
    finally { setBusy(false); }
  }
  async function review(candidate: SelectionCandidate, decision: "APPROVED" | "REJECTED") {
    setBusy(true); setError("");
    try {
      await selectionApi.review(candidate.id, { decision, target_price: decision === "APPROVED" ? Number(targetPrice) : undefined });
      setRun(await selectionApi.getRun(candidate.run_id));
      await reload(); setDetail(null); setTargetPrice("");
      setNotice(decision === "APPROVED" ? `${candidate.symbol} 已确认，进入后续行情跟踪。` : `${candidate.symbol} 已排除。`);
    } catch (err) { setError(message(err)); }
    finally { setBusy(false); }
  }

  return <main className="batch-workbench">
    <header className="batch-heading"><div><p className="eyebrow">20 STOCK RESEARCH</p><h1>20 股全链路研判</h1><p>行情筛选 → 真实模型研判 → 人工确认 → 交易日跟踪与复盘</p></div>
      {mode === "selection" ? <button className="primary-button" disabled={busy || loading} onClick={() => void startRun()}>{busy ? "正在分析…" : "开始分析 20 股"}</button> : <button className="primary-button" disabled={busy || loading} onClick={() => void refreshTracking()}>{busy ? "更新中…" : "刷新跟踪"}</button>}
    </header>
    {error && <p className="batch-alert error" role="alert">{error}</p>}{notice && <p className="batch-alert" role="status">{notice}</p>}
    <section className="batch-summary" aria-label="治理进度">
      <label>市场<select value={market} onChange={event => setMarket(event.target.value)}><option value="CN_A">A 股</option><option value="HK">港股</option></select></label>
      <div><small>最近一轮</small><strong>{run ? `#${run.id} · ${statusText[run.status] || run.status}` : "尚未运行"}</strong></div>
      <div><small>模型研判</small><strong>{run?.candidate_count ?? 0} / 20 只</strong></div>
      <div><small>待人工确认</small><strong>{pending} 只</strong></div>
      <div><small>跟踪已完成</small><strong>{completed} / {tracking.length} 只</strong></div>
    </section>
    <p className="batch-note">自动从本地有行情的股票中筛选最多 20 只；只使用已治理知识图谱和真实模型。若覆盖不足或模型不可用，页面会明确显示，不会以规则结果冒充模型研判。</p>
    {run?.error_message && <p className="batch-alert error">{run.error_message}</p>}
    {run && <section className="batch-card"><div className="batch-card-title"><div><h2>本轮结果</h2><span>数据截至 {new Date(run.as_of).toLocaleString("zh-CN")} · {run.model_instance_code || "自动模型路由"}</span></div><select aria-label="历史分析批次" value={run.id} onChange={event => { const selected = runs.find(item => item.id === Number(event.target.value)); if (selected) void selectionApi.getRun(selected.id).then(setRun).catch(err => setError(message(err))); }}>{runs.map(item => <option key={item.id} value={item.id}>第 {item.id} 轮 · {item.candidate_count} 只</option>)}</select></div>
      {run.missing_data_json.length > 0 && <details className="batch-missing"><summary>数据缺口 / 处理限制（{run.missing_data_json.length}）</summary><ul>{run.missing_data_json.slice(0, 12).map((item, index) => <li key={index}>{item}</li>)}</ul></details>}
      <div className="batch-table-wrap"><table><thead><tr><th>股票</th><th>行情基准</th><th>模型判断</th><th>证据</th><th>确认状态</th><th></th></tr></thead><tbody>{run.candidates.map(item => {
        const analysis = item.analysis_json;
        const evidence = item.evidence_json;
        const modelStatus = String(analysis.model_output_status || "NOT_CALLED");
        return <tr key={item.id}><td><strong>{item.name || item.symbol}</strong><small>{item.symbol}</small></td><td>{price(item.entry_price)}<small>{item.entry_date || "缺少行情"}</small></td><td><strong>{String(analysis.recommendation || statusText[modelStatus] || modelStatus)}</strong><small>{String(analysis.summary || analysis.reasoning || "暂无模型结论")}</small></td><td>{Array.isArray(evidence.documents) ? evidence.documents.length : 0} 篇文档 · {Array.isArray(evidence.facts) ? evidence.facts.length : 0} 条事实</td><td>{statusText[item.decision] || item.decision}</td><td><button type="button" onClick={() => { setDetail(item); setTargetPrice(String(item.analysis_json.target_price || "")); }}>查看</button></td></tr>;
      })}</tbody></table>{run.candidates.length === 0 && <p className="batch-empty">本轮没有可研判股票。请检查行情覆盖、知识图谱状态及失败原因。</p>}</div>
    </section>}
    {mode === "tracking" && <section className="batch-card"><div className="batch-card-title"><div><h2>预测跟踪与复盘</h2><span>仅按有效交易日计数，不足 10 日不会提前定论</span></div></div><div className="batch-table-wrap"><table><thead><tr><th>股票</th><th>目标 / 最新价</th><th>进度</th><th>累计收益</th><th>最大回撤</th><th>状态</th></tr></thead><tbody>{tracking.map(item => <tr key={item.id}><td>{item.symbol}</td><td>{price(item.target_price)} / {price(item.latest_price)}</td><td>{item.observed_sessions} / {item.required_sessions} 日</td><td>{pct(item.cumulative_return_pct)}</td><td>{pct(item.max_drawdown_pct)}</td><td>{statusText[item.status] || item.status}</td></tr>)}</tbody></table>{tracking.length === 0 && <p className="batch-empty">确认入选股票后，跟踪记录会显示在这里。</p>}</div></section>}
    {detail && <div className="batch-overlay" onMouseDown={event => { if (event.target === event.currentTarget && !busy) setDetail(null); }}><section className="batch-detail" role="dialog" aria-modal="true" aria-label={`${detail.name || detail.symbol} 研判详情`}><header><div><h2>{detail.name || detail.symbol}</h2><span>{detail.symbol} · {price(detail.entry_price)} · {detail.entry_date || "无行情日期"}</span></div><button onClick={() => setDetail(null)}>关闭</button></header><h3>模型研判</h3><p>{String(detail.analysis_json.summary || detail.analysis_json.reasoning || "尚无模型结论")}</p><h3>风险与缺口</h3><ul>{(Array.isArray(detail.analysis_json.risks) ? detail.analysis_json.risks : []).map((item, i) => <li key={i}>{String(item)}</li>)}{(Array.isArray(detail.evidence_json.missing_data) ? detail.evidence_json.missing_data : []).map((item, i) => <li key={`m${i}`}>{String(item)}</li>)}</ul><h3>原始证据</h3>{(Array.isArray(detail.evidence_json.documents) ? detail.evidence_json.documents : []).slice(0, 8).map((doc, i) => <details key={i}><summary>{String(doc.title || "证据文档")}</summary><p>{String(doc.excerpt || doc.content || "")}</p>{doc.url && <a href={String(doc.url)} target="_blank" rel="noreferrer">打开来源</a>}</details>)}{detail.decision === "PENDING" && <footer><label>目标价<input type="number" min="0.000001" step="any" value={targetPrice} onChange={event => setTargetPrice(event.target.value)} /></label><button disabled={busy} onClick={() => void review(detail, "REJECTED")}>排除</button><button className="primary-button" disabled={busy || !Number(targetPrice)} onClick={() => void review(detail, "APPROVED")}>确认并开始跟踪</button></footer>}</section></div>}
  </main>;
}
