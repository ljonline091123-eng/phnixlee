import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import { api, type KnowledgeBase, type KnowledgeGraph, type ModelInstance, type ModelProvider } from "./api";
import { selectionApi, type SelectionCandidate, type SelectionRun, type SelectionTracking } from "./selectionApi";
import "./SelectionWatch.css";

const labels: Record<string, string> = { PENDING: "待复选", PENDING_REVIEW: "待人工复选", REVIEW: "待人工复选", PARTIAL: "数据不足", PENDING_DATA: "等待行情", LOCAL_EVIDENCE: "本地规则预选", MODEL_REQUESTED: "模型分析中", MODEL_COMPLETED: "模型综合分析", MODEL_FAILED_FALLBACK_LOCAL: "模型失败 · 规则预选", APPROVED: "已入选", REJECTED: "已排除", COMPLETED: "已完成", FAILED: "执行失败", RUNNING: "生成中", TRACKING: "跟踪中", WAITING_DATA: "等待行情", REVIEWED: "已复盘", REAL: "真实数据", MOCK: "模拟数据", RULES_ONLY: "规则预选", MODEL: "模型综合分析", LLM: "模型综合分析", CN_A: "A 股", HK: "港股", NEEQ: "新三板", NEEQ_INNOVATION: "创新层" };
const nameOf = (value: string) => labels[value] || value;
const numberOf = (value: unknown, digits = 2) => typeof value === "number" && Number.isFinite(value) ? value.toFixed(digits) : "—";
const pct = (value: unknown) => value == null ? "—" : `${numberOf(value)}%`;
const dateOf = (value: string | null) => value ? new Date(value).toLocaleString("zh-CN") : "—";
const strings = (value: unknown): string[] => Array.isArray(value) ? value.filter((item): item is string => typeof item === "string") : [];
const records = (value: unknown): Record<string, unknown>[] => Array.isArray(value) ? value.filter((item): item is Record<string, unknown> => !!item && typeof item === "object") : [];
const textOf = (value: unknown) => typeof value === "string" || typeof value === "number" ? String(value) : "";
const errorOf = (error: unknown) => error instanceof Error ? error.message : "请求失败，请重试";
function Badge({ value }: { value: string }) { return <span className={`selection-badge ${["PENDING_REVIEW", "WAITING_DATA", "FAILED", "MOCK", "RULES_ONLY"].includes(value) ? "warning" : ""}`}>{nameOf(value)}</span>; }

function Dialog({ title, close, children, busy = false }: { title: string; close: () => void; children: ReactNode; busy?: boolean }) {
  useEffect(() => { const key = (event: KeyboardEvent) => { if (event.key === "Escape" && !busy) close(); }; window.addEventListener("keydown", key); return () => window.removeEventListener("keydown", key); }, [close, busy]);
  return <div className="selection-backdrop" onMouseDown={event => { if (event.currentTarget === event.target && !busy) close(); }}><section className="selection-dialog" role="dialog" aria-modal="true" aria-label={title}><header><h2>{title}</h2><button type="button" disabled={busy} onClick={close}>关闭</button></header>{children}</section></div>;
}

function ReturnChart({ item }: { item: SelectionTracking }) {
  if (!item.snapshots.length) return <p className="selection-help">人工确认后的有效交易日行情到齐后显示收益曲线。</p>;
  const values = [0, ...item.snapshots.map(row => row.return_pct)];
  const low = Math.min(...values, item.target_return_pct ?? 0), high = Math.max(...values, item.target_return_pct ?? 0);
  const span = Math.max(high - low, 1);
  const x = (index: number) => 52 + index * 610 / Math.max(values.length - 1, 1);
  const y = (value: number) => 174 - (value - low) * 130 / span;
  return <svg viewBox="0 0 710 220" role="img" aria-label="确认预测后的累计收益率曲线" style={{ width: "100%", background: "#f4f8f5", borderRadius: 10 }}>
    <line x1="52" x2="662" y1={y(0)} y2={y(0)} stroke="#b5c7ba" strokeDasharray="4 4" />
    <text x="8" y={y(0) + 4} fontSize="11" fill="#607568">0%</text>
    {item.target_return_pct != null && <><line x1="52" x2="662" y1={y(item.target_return_pct)} y2={y(item.target_return_pct)} stroke="#b17c29" strokeDasharray="6 4" /><text x="52" y={y(item.target_return_pct) - 8} fontSize="11" fill="#916620">目标 {pct(item.target_return_pct)}</text></>}
    <polyline points={values.map((value, index) => `${x(index)},${y(value)}`).join(" ")} fill="none" stroke="#28674b" strokeWidth="3" />
    {values.map((value, index) => <circle key={index} cx={x(index)} cy={y(value)} r="3" fill="#28674b"><title>{index === 0 ? "基准" : item.snapshots[index - 1].trade_date}：{pct(value)}</title></circle>)}
    <text x="52" y="206" fontSize="11" fill="#607568">确认基准</text><text x="550" y="206" fontSize="11" fill="#607568">第 {item.observed_sessions} 个交易日</text>
  </svg>;
}

function Evidence({ candidate }: { candidate: SelectionCandidate }) {
  const evidence = candidate.evidence_json;
  const documents = records(evidence.documents);
  const relations = records(evidence.relations);
  const analysis = candidate.analysis_json;
  const gaps = [...new Set([...strings(evidence.missing_data), ...strings(analysis.missing_data)])];
  return <>
    <h3>综合判断</h3><p className="selection-help">{textOf(analysis.summary) || textOf(analysis.reasoning) || textOf(analysis.reasoning_logic) || textOf(analysis.rationale) || "尚无模型结论，请核验下方证据后复选。"}</p>
    {strings(analysis.risks).length > 0 && <ul>{strings(analysis.risks).map((risk, i) => <li key={i}>{risk}</li>)}</ul>}
    <h3>知识库证据摘录 · {documents.length}</h3><p className="selection-help">以下为选股时保存的证据片段，每篇最多 500 字；完整入库原文可在知识图谱中按文档核查。</p>
    {documents.length ? documents.map((doc, i) => <details className="selection-evidence" key={i}><summary>{textOf(doc.title) || "资料证据"}</summary><small>来源：{textOf(doc.source_table) || textOf(doc.source)} · 文档 {textOf(doc.id) || textOf(doc.document_id)}</small><p>{textOf(doc.content) || textOf(doc.excerpt) || textOf(doc.evidence_text) || "原文未提供"}</p></details>) : <p className="selection-help">当前范围没有可用知识文档。</p>}
    <h3>图谱关系证据 · {relations.length}</h3>
    {relations.slice(0, 20).map((relation, i) => <div className="selection-evidence" key={i}><strong>{textOf(relation.head) || textOf(relation.from_name)} → {textOf(relation.type) || textOf(relation.relation_type) || textOf(relation.relation)} → {textOf(relation.tail) || textOf(relation.to_name)}</strong><p>{textOf(relation.evidence_text) || textOf(relation.evidence) || "请在知识图谱页查看关联原文。"}</p></div>)}
    {gaps.length > 0 && <><h3>仍待补充的数据</h3><ul className="selection-help">{gaps.map((gap, i) => <li key={i}>{gap}</li>)}</ul></>}
  </>;
}

export function SelectionWorkbench({ mode }: { mode: "selection" | "tracking" }) {
  const [runs, setRuns] = useState<SelectionRun[]>([]);
  const [run, setRun] = useState<SelectionRun | null>(null);
  const [tracking, setTracking] = useState<SelectionTracking[]>([]);
  const [bases, setBases] = useState<KnowledgeBase[]>([]);
  const [graphs, setGraphs] = useState<KnowledgeGraph[]>([]);
  const [instances, setInstances] = useState<ModelInstance[]>([]);
  const [providers, setProviders] = useState<ModelProvider[]>([]);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadVersion, setLoadVersion] = useState(0);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [createOpen, setCreateOpen] = useState(false);
  const [candidate, setCandidate] = useState<SelectionCandidate | null>(null);
  const [detail, setDetail] = useState<SelectionTracking | null>(null);
  const [form, setForm] = useState({ market: "CN_A", symbols: "", candidate_limit: 30, min_abs_change_pct: 2, min_volume_ratio: 1.2, model_instance_code: "", use_model: true, knowledge_base_ids: [] as number[], graph_ids: [] as number[] });
  const [review, setReview] = useState({ target_price: "", stop_price: "", confidence: "", notes: "" });
  async function reload() {
    const [nextRuns, nextTracking] = await Promise.all([selectionApi.listRuns(), selectionApi.listTracking()]);
    setRuns(nextRuns); setTracking(nextTracking);
    return nextRuns;
  }
  useEffect(() => {
    let valid = true;
    setLoading(true); setError("");
    void Promise.all([selectionApi.listRuns(), selectionApi.listTracking(), api.listKnowledgeBases(), api.listKnowledgeGraphs(), api.listModelInstances(), api.listModelProviders()]).then(async ([nextRuns, nextTracking, nextBases, nextGraphs, nextInstances, nextProviders]) => {
      if (!valid) return;
      setRuns(nextRuns); setTracking(nextTracking); setBases(nextBases); setGraphs(nextGraphs); setInstances(nextInstances); setProviders(nextProviders);
      const enabledBases = nextBases.filter(item => item.enabled);
      setForm(current => ({ ...current, knowledge_base_ids: enabledBases.map(item => item.id), graph_ids: nextGraphs.filter(item => item.enabled && enabledBases.some(base => base.id === item.knowledge_base_id)).map(item => item.id) }));
      if (nextRuns.length) { const first = await selectionApi.getRun(nextRuns[0].id); if (valid) setRun(first); }
    }).catch(err => { if (valid) setError(errorOf(err)); }).finally(() => { if (valid) setLoading(false); });
    return () => { valid = false; };
  }, [loadVersion]);
  async function selectRun(id: number) { setBusy(true); setError(""); try { setRun(await selectionApi.getRun(id)); } catch (err) { setError(errorOf(err)); } finally { setBusy(false); } }
  async function create(event: FormEvent) {
    event.preventDefault(); setError(""); setNotice(""); setBusy(true);
    try {
      if (!form.knowledge_base_ids.length || !form.graph_ids.length) throw new Error("请至少选择一个知识库和一张独立图谱作为选股依据。");
      const result = await selectionApi.createRun({ ...form, symbols: form.symbols.split(/[\s,，;；]+/).filter(Boolean), model_instance_code: form.model_instance_code || null, data_mode: "REAL" });
      setRun(result); await reload(); setCreateOpen(false); setNotice(`选股批次 #${result.id} 已保存，${result.candidate_count} 只候选待人工复选。`);
    } catch (err) { setError(errorOf(err)); } finally { setBusy(false); }
  }
  function openReview(item: SelectionCandidate) { setCandidate(item); setError(""); setReview({ target_price: item.target_price?.toString() || textOf(item.analysis_json.target_price), stop_price: item.stop_price?.toString() || textOf(item.analysis_json.stop_price), confidence: item.confidence?.toString() || textOf(item.analysis_json.confidence), notes: item.review_notes || "" }); }
  async function saveReview(decision: "APPROVED" | "REJECTED") {
    if (!candidate) return;
    setError(""); setBusy(true);
    try {
      const target = review.target_price.trim() ? Number(review.target_price) : undefined;
      if (decision === "APPROVED" && (!target || !Number.isFinite(target))) throw new Error("请输入有效的预测目标价后再确认入选。");
      await selectionApi.review(candidate.id, { decision, target_price: decision === "APPROVED" ? target : undefined, stop_price: decision === "APPROVED" && review.stop_price ? Number(review.stop_price) : undefined, confidence: review.confidence ? Number(review.confidence) : undefined, notes: review.notes });
      setRun(await selectionApi.getRun(candidate.run_id)); await reload(); setCandidate(null);
      setNotice(decision === "APPROVED" ? "已确认入选并固化预测，进入至少 10 个交易日的跟踪。" : "已保存排除意见。");
    } catch (err) { setError(errorOf(err)); } finally { setBusy(false); }
  }
  async function refresh() { setBusy(true); setError(""); try { await selectionApi.refresh(); await reload(); setNotice("已更新跟踪记录。有效行情不足 10 个交易日的股票将继续等待；完成后可查看复盘。"); } catch (err) { setError(errorOf(err)); } finally { setBusy(false); } }
  async function openTracking(id: number) { setBusy(true); setError(""); try { setDetail(await selectionApi.getTracking(id)); } catch (err) { setError(errorOf(err)); } finally { setBusy(false); } }
  const pending = run?.candidates.filter(item => !["APPROVED", "REJECTED"].includes(item.decision)).length ?? 0;
  const availableGraphs = graphs.filter(item => item.enabled && form.knowledge_base_ids.includes(item.knowledge_base_id));
  return <div className="selection-workbench">
    {error && !createOpen && !candidate && <div className="form-error" role="alert">{error} <button type="button" disabled={loading} onClick={() => setLoadVersion(value => value + 1)}>重新加载</button></div>}{notice && <p className="selection-notice" role="status">{notice}</p>}
    <section className="panel">
      <div className="selection-toolbar"><div><p className="eyebrow">{mode === "selection" ? "EVIDENCE BASED SELECTION" : "PREDICTION REVIEW"}</p><h2>{mode === "selection" ? "候选池与人工复选" : "预测跟踪与复盘"}</h2></div>
        {mode === "selection" ? <button className="primary-button" type="button" disabled={busy || loading} onClick={() => { setError(""); setCreateOpen(true); }}>{loading ? "加载选股配置…" : "新建选股"}</button> : <button className="primary-button" type="button" disabled={busy || loading} onClick={() => void refresh()}>{busy ? "更新中…" : "更新跟踪与复盘"}</button>}
      </div>
      <p className="selection-steps">规则筛选 → 知识库与图谱综合分析 → 人工复选并确认预测 → 至少 10 个交易日跟踪 → 检验偏差与复盘</p>
      {mode === "selection" ? <>
        <div className="selection-toolbar"><label>历史选股批次 <select aria-label="选股批次" value={run?.id ?? ""} disabled={busy} onChange={event => void selectRun(Number(event.target.value))}><option value="" disabled>暂无选股批次</option>{runs.map(item => <option key={item.id} value={item.id}>#{item.id} · {nameOf(item.market)} · {dateOf(item.created_at)} · {item.candidate_count} 只</option>)}</select></label>{run && <div><Badge value={run.analysis_mode} /> <Badge value={run.status} /></div>}</div>
        {run ? <>
          <div className="selection-kpis"><div className="selection-kpi"><span>规则候选</span><strong>{run.candidate_count}</strong></div><div className="selection-kpi"><span>待人工复选</span><strong>{pending}</strong></div><div className="selection-kpi"><span>已进入跟踪</span><strong>{run.tracking_count}</strong></div><div className="selection-kpi"><span>数据截止时间</span><strong style={{ fontSize: 14 }}>{dateOf(run.as_of)}</strong></div></div>
          <p className="selection-help">模型：{run.model_instance_code || "自动路由 / 规则预选"} · {run.knowledge_base_ids_json.length} 个知识库 · {run.graph_ids_json.length} 张图谱。未确认的候选不会进入预测跟踪。</p>
          {run.error_message && <p className="form-error">{run.error_message}</p>}
          {run.missing_data_json.length > 0 && <details className="selection-evidence"><summary>数据与分析限制（{run.missing_data_json.length}）</summary><ul>{run.missing_data_json.map((item, i) => <li key={i}>{item}</li>)}</ul></details>}
          <div className="table-wrap"><table><thead><tr><th>股票</th><th>行情基准</th><th>规则评分</th><th>结论 / 数据缺口</th><th>人工复选</th><th>操作</th></tr></thead><tbody>{run.candidates.map(item => <tr key={item.id}><td><strong>{item.name || item.symbol}</strong><br /><code>{item.symbol}</code> <small>{nameOf(item.market)}</small></td><td>{numberOf(item.entry_price)}<br /><small>{item.entry_date || "缺行情"}</small></td><td>{numberOf(item.hard_score, 1)}</td><td style={{ maxWidth: 340 }}>{textOf(item.analysis_json.summary) || textOf(item.analysis_json.reasoning) || "查看资料后复选"}{strings(item.evidence_json.missing_data).length > 0 && <p className="selection-help">有 {strings(item.evidence_json.missing_data).length} 项数据待补充</p>}</td><td><Badge value={item.decision} /></td><td><button type="button" disabled={busy} onClick={() => openReview(item)}>{["APPROVED", "REJECTED"].includes(item.decision) ? "查看决策" : "证据与复选"}</button></td></tr>)}</tbody></table>{!run.candidates.length && <p className="selection-empty">当前条件没有候选。请检查本地行情覆盖、数据时效与筛选阈值。</p>}</div>
        </> : <p className="selection-empty">新建选股，从已有数据中筛出候选，再结合知识证据进行人工复选。</p>}
      </> : <>
        <p className="selection-help">仅计算人工确认后、同一复权口径的有效交易日行情。周末、节假日或缺行情不会充当跟踪天数。后台 Scheduler 与 Worker 运行时会定期更新，也可手动更新。</p>
        <div className="table-wrap"><table><thead><tr><th>股票</th><th>确认基准 / 目标价</th><th>跟踪进度</th><th>最新收盘 / 日期</th><th>累计收益</th><th>最大回撤</th><th>状态</th><th>操作</th></tr></thead><tbody>{tracking.map(item => <tr key={item.id}><td><code>{item.symbol}</code><br /><small>{nameOf(item.market)}</small></td><td>{numberOf(item.entry_price)} / {numberOf(item.target_price)}</td><td>{item.observed_sessions} / {item.required_sessions} 交易日</td><td>{numberOf(item.latest_price)}<br /><small>{item.latest_date || "等待行情"}</small></td><td>{pct(item.cumulative_return_pct)}</td><td>{pct(item.max_drawdown_pct)}</td><td><Badge value={item.status} /></td><td><button type="button" disabled={busy} onClick={() => void openTracking(item.id)}>查看跟踪与复盘</button></td></tr>)}</tbody></table>{!tracking.length && <p className="selection-empty">尚无跟踪股票。请在候选池中完成人工复选并确认预测目标。</p>}</div>
      </>}
    </section>
    {createOpen && <Dialog title="新建选股" busy={busy} close={() => setCreateOpen(false)}><form className="selection-form" onSubmit={create}>
      <div className="selection-form-grid"><label>市场<select value={form.market} onChange={event => setForm({ ...form, market: event.target.value })}>{["CN_A", "HK", "NEEQ", "NEEQ_INNOVATION"].map(market => <option key={market} value={market}>{nameOf(market)}</option>)}</select></label><label>候选数量上限<input type="number" required min={1} max={50} value={form.candidate_limit} onChange={event => setForm({ ...form, candidate_limit: Number(event.target.value) })} /></label></div>
      <label>股票范围（选填）<textarea rows={2} value={form.symbols} placeholder="输入代码，用逗号或空格分隔；留空按本地已有行情进行硬规则筛选" onChange={event => setForm({ ...form, symbols: event.target.value })} /></label>
      <div className="selection-form-grid"><label>绝对涨跌幅阈值（%）<input type="number" required min={0} max={50} step="0.1" value={form.min_abs_change_pct} onChange={event => setForm({ ...form, min_abs_change_pct: Number(event.target.value) })} /></label><label>成交量比阈值<input type="number" required min={0} max={100} step="0.1" value={form.min_volume_ratio} onChange={event => setForm({ ...form, min_volume_ratio: Number(event.target.value) })} /></label></div>
      <label>知识库</label><div className="selection-choices">{bases.filter(item => item.enabled).map(item => <label key={item.id}><input type="checkbox" checked={form.knowledge_base_ids.includes(item.id)} onChange={() => { const ids = form.knowledge_base_ids.includes(item.id) ? form.knowledge_base_ids.filter(id => id !== item.id) : [...form.knowledge_base_ids, item.id]; setForm({ ...form, knowledge_base_ids: ids, graph_ids: form.graph_ids.filter(id => graphs.some(graph => graph.id === id && ids.includes(graph.knowledge_base_id))) }); }} />{item.kb_name}</label>)}</div>
      <label>独立图谱</label><div className="selection-choices">{availableGraphs.map(item => <label key={item.id}><input type="checkbox" checked={form.graph_ids.includes(item.id)} onChange={() => setForm({ ...form, graph_ids: form.graph_ids.includes(item.id) ? form.graph_ids.filter(id => id !== item.id) : [...form.graph_ids, item.id] })} />{item.graph_name} <small>（{item.governance_status === "GOVERNED" ? "已治理" : item.governance_status === "LOCKED" ? "已锁定" : "待治理"}）</small></label>)}{!availableGraphs.length && <span className="selection-help">所选知识库暂无启用图谱，请先建立并治理。</span>}</div>
      <label style={{ display: "flex", flexDirection: "row", alignItems: "center" }}><input type="checkbox" checked={form.use_model} onChange={event => setForm({ ...form, use_model: event.target.checked })} />调用模型综合分析候选证据</label>
      <label>分析模型<select disabled={!form.use_model} value={form.model_instance_code} onChange={event => setForm({ ...form, model_instance_code: event.target.value })}><option value="">按选股任务自动路由</option>{instances.filter(item => item.enabled && providers.some(provider => provider.id === item.provider_id && provider.enabled && provider.provider_type !== "MOCK")).map(item => <option key={item.id} value={item.instance_code} disabled={!item.api_key_configured}>{item.model_name} · {item.instance_code}{item.api_key_configured ? "" : "（未配置密钥）"}</option>)}</select></label>
      <p className="selection-help">只向模型发送筛选后的候选和有限证据。取消模型分析时保存为规则预选，供人工核验；数据不足或模型失败会明确记录。</p>
      {error && <p className="form-error" role="alert">{error}</p>}<footer><button type="button" disabled={busy} onClick={() => setCreateOpen(false)}>取消</button><button className="primary-button" disabled={busy}>{busy ? "正在筛选与分析…" : "生成候选池"}</button></footer>
    </form></Dialog>}
    {candidate && <Dialog title={`${candidate.symbol} ${candidate.name || ""} · 人工复选`} busy={busy} close={() => setCandidate(null)}>
      <p className="selection-help">行情基准 {numberOf(candidate.entry_price)}（{candidate.entry_date || "缺行情"}）。确认后记录预测与当时证据，后续复盘以确认时快照为准。</p><Evidence candidate={candidate} />
      {!["APPROVED", "REJECTED"].includes(candidate.decision) ? <form className="selection-form" onSubmit={event => { event.preventDefault(); void saveReview("APPROVED"); }}><h3>确认预测</h3><div className="selection-form-grid"><label>预测目标价<input type="number" required min="0.000001" step="any" value={review.target_price} onChange={event => setReview({ ...review, target_price: event.target.value })} /></label><label>观察止损价（选填）<input type="number" min="0.000001" step="any" value={review.stop_price} onChange={event => setReview({ ...review, stop_price: event.target.value })} /></label><label>主观置信度（0–1，选填）<input type="number" min={0} max={1} step="0.01" value={review.confidence} onChange={event => setReview({ ...review, confidence: event.target.value })} /></label></div><label>复选理由 / 调整说明<textarea rows={3} maxLength={4000} value={review.notes} onChange={event => setReview({ ...review, notes: event.target.value })} /></label>{error && <p className="form-error" role="alert">{error}</p>}<footer><button type="button" disabled={busy} onClick={() => void saveReview("REJECTED")}>排除此候选</button><button className="primary-button" disabled={busy || !candidate.entry_price}>{busy ? "保存中…" : "确认入选并开始跟踪"}</button></footer></form> : <div className="selection-evidence"><Badge value={candidate.decision} /><p>目标价：{numberOf(candidate.target_price)} · 确认时间：{dateOf(candidate.reviewed_at)}</p><p>{candidate.review_notes || "未填写人工复选说明"}</p></div>}
    </Dialog>}
    {detail && <Dialog title={`${detail.symbol} · 跟踪与复盘`} close={() => setDetail(null)}><p className="selection-help">起始 {dateOf(detail.started_at)} · {detail.observed_sessions} / {detail.required_sessions} 个交易日 · {nameOf(detail.status)}</p><ReturnChart item={detail} /><div className="selection-kpis"><div className="selection-kpi"><span>累计收益</span><strong>{pct(detail.cumulative_return_pct)}</strong></div><div className="selection-kpi"><span>最大回撤</span><strong>{pct(detail.max_drawdown_pct)}</strong></div><div className="selection-kpi"><span>预测收益</span><strong>{pct(detail.target_return_pct)}</strong></div><div className="selection-kpi"><span>方向判断</span><strong>{detail.direction_hit == null ? "待复盘" : detail.direction_hit ? "正确" : "偏离"}</strong></div></div><h3>复盘结论</h3><p className="selection-help">{textOf(detail.review_json.summary) || textOf(detail.review_json.conclusion) || textOf(detail.review_json.reason) || (detail.observed_sessions < detail.required_sessions ? "有效跟踪日尚未满 10 个交易日，继续等待行情，不提前判定预测准确。" : "请更新跟踪以生成复盘结果。")}</p>{strings(detail.review_json.adjustments).length > 0 && <ul>{strings(detail.review_json.adjustments).map((item, i) => <li key={i}>{item}</li>)}</ul>}<div className="table-wrap"><table style={{ minWidth: 540 }}><thead><tr><th>交易日序号</th><th>日期</th><th>收盘价</th><th>累计收益</th><th>回撤</th></tr></thead><tbody>{detail.snapshots.map(item => <tr key={item.id}><td>{item.sequence}</td><td>{item.trade_date}</td><td>{numberOf(item.close_price)}</td><td>{pct(item.return_pct)}</td><td>{pct(item.drawdown_pct)}</td></tr>)}</tbody></table></div></Dialog>}
  </div>;
}
