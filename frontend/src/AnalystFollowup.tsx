import { useEffect, useRef, useState } from "react";
import { BellRing, MessageCircle, Send, RefreshCw, X, Check, Clock3 } from "lucide-react";
import { analystFollowupApi, type AnalystDialogue, type AnalystFollowupState, type AnalystRun, type AnalystWatchConfig, type AnalystDailyReview, type AnalystEvidence, type AnalystRevision } from "./stockAnalystApi";

const statusNames: Record<string, string> = { QUEUED: "等待执行", RUNNING: "正在执行", COMPLETED: "已完成", PARTIAL: "部分完成", FAILED: "失败", SKIPPED: "已跳过" };
const verdicts: Record<string, string> = { SUPPORTED: "原判断获得支持", CHALLENGED: "原判断受到挑战", UNCHANGED: "判断暂未改变", INCONCLUSIVE: "暂时无法验证" };
const hits: Record<string, string> = { HIT: "已触发", NOT_HIT: "未触发", UNKNOWN: "暂无法判断" };
const categoryNames: Record<string, string> = { NEWS: "新闻", NOTICE: "公告", ANY: "新闻与公告", QUOTE: "实时行情", KLINE: "日线与成交量" };
function date(value?: string) { return value ? new Date(/Z$|[+-]\d\d:\d\d$/.test(value) ? value : value + "Z").toLocaleString("zh-CN", { hour12: false }) : "—"; }
const pending = (status: string) => ["QUEUED", "RUNNING"].includes(status);
const defaults = (): AnalystWatchConfig => ({ enabled: true, price_lower: null, price_upper: null, volume_mode: "ANY", volume_ratio: 1.5, volume_window: 20, event_conditions: [], review_focus: "", schedule_hour: 18, schedule_minute: 0 });

export function AnalystFollowup({ analysis, model, onRevision, onReview }: { analysis: AnalystRun; model: string; onRevision: (revision: AnalystRevision | null) => void; onReview: (review: AnalystDailyReview | null) => void }) {
  const [state, setState] = useState<AnalystFollowupState | null>(null);
  const [timeline, setTimeline] = useState<AnalystDailyReview[]>([]);
  const [total, setTotal] = useState(0);
  const [offset, setOffset] = useState(0);
  const [question, setQuestion] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [view, setView] = useState<"chat" | "timeline">("chat");
  const [editor, setEditor] = useState<AnalystDialogue | null>(null);
  const [judgment, setJudgment] = useState("");
  const [focus, setFocus] = useState("");
  const [config, setConfig] = useState<AnalystWatchConfig | null>(null);
  const [sources, setSources] = useState<{ review: AnalystDailyReview; rows: AnalystEvidence[] } | null>(null);
  const [seconds, setSeconds] = useState(0);
  const activeId = useRef(analysis.id);
  const loadGeneration = useRef(0);
  const confirmationBase = useRef<number | null>(null);
  const chatEnd = useRef<HTMLDivElement>(null);
  const shouldScroll = useRef(false);
  const inFlight = !!state?.dialogues.some(row => pending(row.status));
  const reviewRunning = timeline.some(row => pending(row.status));

  async function load() {
    const id = analysis.id;
    const generation = ++loadGeneration.current;
    const [next, page] = await Promise.all([analystFollowupApi.state(id), analystFollowupApi.timeline(id, offset)]);
    if (activeId.current !== id || loadGeneration.current !== generation) return;
    setState(next); setTimeline(page.items); setTotal(page.total); onRevision(next.revisions[0] || null); onReview(next.latest_review || null);
  }
  useEffect(() => {
    activeId.current = analysis.id;
    setState(null); setTimeline([]); setQuestion(""); setMessage(""); setOffset(0); setConfig(null); setEditor(null);
    return () => { activeId.current = -1; loadGeneration.current += 1; };
  }, [analysis.id]);
  useEffect(() => {
    let closed = false;
    void load().catch(e => { if (!closed) setMessage(e.message); });
    const timer = window.setInterval(() => { void load().catch(e => { if (!closed) setMessage(`刷新失败：${e.message}，后台任务仍会继续。`); }); }, inFlight || reviewRunning ? 2000 : 15000);
    return () => { closed = true; loadGeneration.current += 1; window.clearInterval(timer); };
  }, [analysis.id, offset, inFlight, reviewRunning]);
  useEffect(() => {
    if (!inFlight && !reviewRunning) return;
    const starts = [...(state?.dialogues || []), ...timeline].filter(row => pending(row.status)).map(row => new Date(/Z$|[+-]\d\d:\d\d$/.test(row.created_at) ? row.created_at : row.created_at + "Z").getTime());
    const started = Math.min(...starts);
    const tick = () => setSeconds(Math.max(0, Math.floor((Date.now() - started) / 1000)));
    tick();
    const timer = window.setInterval(tick, 1000);
    return () => window.clearInterval(timer);
  }, [inFlight, reviewRunning]);
  useEffect(() => { if (shouldScroll.current) { chatEnd.current?.scrollIntoView({ block: "nearest", behavior: "smooth" }); if (!inFlight) shouldScroll.current = false; } }, [state?.dialogues.length, inFlight]);
  useEffect(() => {
    if (!editor && !config && !sources) return;
    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const close = (event: KeyboardEvent) => { if (event.key === "Escape" && !busy) { setEditor(null); setConfig(null); setSources(null); } };
    window.addEventListener("keydown", close);
    return () => { document.body.style.overflow = previous; window.removeEventListener("keydown", close); };
  }, [!!editor, !!config, !!sources, busy]);

  async function action(work: () => Promise<void>) {
    if (busy) return;
    setBusy(true); setMessage("");
    try { await work(); } catch (e) { setMessage(e instanceof Error ? e.message : "操作失败，请重试"); }
    finally { setBusy(false); }
  }
  function openConditions() {
    setConfig({ ...(state?.watch?.config_json || defaults()), model_instance_code: model,
      review_focus: state?.watch?.config_json.review_focus || state?.revisions[0]?.focus_points_json.join("；") || (analysis.result_json.watch_items || []).join("；") });
  }
  async function send() {
    if (!question.trim() || !model || inFlight) return;
    await action(async () => {
      const row = await analystFollowupApi.send(analysis.id, question.trim(), model);
      shouldScroll.current = true;
      setState(prev => prev ? { ...prev, dialogues: [...prev.dialogues, row] } : prev);
      setQuestion(""); setSeconds(0); await load();
    });
  }
  function edit(row: AnalystDialogue) {
    confirmationBase.current = state?.revisions[0]?.id || null;
    setEditor(row); setJudgment(row.reply_json.suggested_judgment || ""); setFocus((row.reply_json.focus_points || []).join("\n"));
  }

  return <section className="analyst-followup" aria-label="分析对话与每日跟踪">
    <header className="analyst-followup-header"><div><h3><MessageCircle size={18} />与AI讨论，持续验证判断</h3><p>讨论后由您确认报告；每日复核保留新旧判断及证据。</p></div><div className="analyst-actions"><button type="button" className="analyst-secondary" disabled={!state} onClick={openConditions}><BellRing size={16} />关注价位 / 条件</button></div></header>
    <div className="analyst-tabs" role="tablist" aria-label="对话与跟踪"><button role="tab" aria-selected={view === "chat"} onClick={() => setView("chat")}>与AI对话</button><button role="tab" aria-selected={view === "timeline"} onClick={() => setView("timeline")}>事件跟踪时间轴 <small>{total}</small></button><button type="button" onClick={() => void load().catch(e => setMessage(e.message))} aria-label="刷新对话与跟踪"><RefreshCw size={15} /></button></div>
    {message && <p className="analyst-notice" role="status">{message}</p>}
    {view === "chat" && <>
      <div className="analyst-chat-log" aria-live="polite">
        {!state?.dialogues.length && <div className="analyst-chat-empty"><p>可以追问结论、比较不同解释，或提出希望继续验证的方向。</p><div className="analyst-actions">{["哪些新证据会改变当前判断？", "综合现有资料，最值得跟踪的三个方向是什么？"].map(text => <button type="button" key={text} onClick={() => setQuestion(text)}>{text}</button>)}</div></div>}
        {state?.dialogues.map(row => <article key={row.id} className="analyst-dialogue"><div className="analyst-user-bubble"><small>您 · {date(row.created_at)}</small><p>{row.question}</p></div><div className="analyst-ai-bubble"><small>AI证券分析师 · {row.model_instance_code || model}</small>{pending(row.status) ? <p role="status"><RefreshCw size={15} className="analyst-spin" /> 正在结合原报告和证据思考 · 等待 {seconds} 秒</p> : row.status === "FAILED" ? <div className="analyst-error"><p>{row.error_message}</p><button type="button" onClick={() => { setQuestion(row.question); setMessage("可更换上方分析模型后重新发送，原报告保持保留。"); }}>重新编辑发送</button></div> : <><p>{row.reply_json.reply}</p>{!!row.reply_json.evidence?.length && <details><summary>引用依据 · {row.reply_json.evidence.length}条</summary>{row.reply_json.evidence.map((ref, i) => <p key={i}>{ref.claim} <small>（{ref.evidence_id}）</small></p>)}</details>}<button type="button" className="analyst-text-button" onClick={() => edit(row)}><Check size={14} /> 编辑并确认更新报告</button></>}</div></article>)}
        <div ref={chatEnd} />
      </div>
      <form className="analyst-chat-compose" onSubmit={e => { e.preventDefault(); void send(); }}><label htmlFor={`analyst-question-${analysis.id}`}>针对这份报告继续提问</label><textarea id={`analyst-question-${analysis.id}`} value={question} maxLength={6000} onChange={e => setQuestion(e.target.value)} placeholder="例如：回购是否足以改变目前判断？还需要观察哪些后续信号？" rows={3} /><div><small>使用上方所选模型 · {model || "请先选择模型"} · {question.length}/6000</small><button className="analyst-primary" type="submit" disabled={busy || inFlight || !model || question.trim().length < 2}><Send size={15} />{busy ? "提交中…" : inFlight ? "AI正在回复" : "发送问题"}</button></div></form>
      {!!state?.revisions.length && <details className="analyst-revision-history"><summary>报告确认历史 · {state.revisions.length}个版本</summary>{state.revisions.map(row => <article key={row.id}><strong>第{row.version}版 · {date(row.created_at)}</strong><p>{row.judgment}</p><ul>{row.focus_points_json.map((s, i) => <li key={i}>{s}</li>)}</ul></article>)}</details>}
    </>}
    {view === "timeline" && <>
      <div className="analyst-watch-overview"><div><strong>{state?.watch ? state.watch.enabled ? "每日监控已开启" : "每日监控已暂停" : "尚未设置关注条件"}</strong><p>{state?.watch ? `每日 ${String(state.watch.config_json.schedule_hour).padStart(2, "0")}:${String(state.watch.config_json.schedule_minute).padStart(2, "0")}（北京时间），自动采集 → 条件比对 → AI复核 → 保存时间轴。` : "设置价位、量能和事件条件后，即可自动持续跟踪。"}</p>{state?.watch && <p className="analyst-muted">监控使用模型：{state.watch.model_instance_code} · 条件版本 {state.watch.config_version}</p>}</div><div className="analyst-actions"><button type="button" className="analyst-secondary" disabled={!state?.watch || busy || reviewRunning} onClick={() => void action(async () => { await analystFollowupApi.check(analysis.id); setSeconds(0); setOffset(0); await load(); })}>{reviewRunning ? "复核进行中" : "立即检查"}</button>{state?.watch && <button type="button" className="analyst-secondary" disabled={busy} onClick={() => void action(async () => { await analystFollowupApi.saveWatch(analysis.id, { ...state.watch!.config_json, enabled: !state.watch!.enabled }); await load(); })}>{state.watch.enabled ? "暂停监控" : "恢复监控"}</button>}</div></div>
      {state && !state.runner.available && <p className="analyst-notice">{state.runner.message}。服务需保持运行才能自动检查。</p>}
      {!timeline.length && <p className="analyst-muted">暂无跟踪记录，可点击“立即检查”验证全流程。</p>}
      <div className="analyst-timeline">{timeline.map(row => <article key={row.id} className={`analyst-timeline-item ${row.status.toLowerCase()}`}><header><strong>{row.business_date} <small>第{row.attempt}次检查</small></strong><span className={`analyst-status ${row.status.toLowerCase()}`}>{statusNames[row.status] || row.status}</span></header><p className="analyst-muted">{date(row.created_at)} {row.completed_at && `→ ${date(row.completed_at)}`} · {row.model_instance_code}</p>
        {pending(row.status) && <p role="status" className="analyst-live"><Clock3 size={16} /><strong>{row.progress_json.message}</strong>等待 {seconds} 秒</p>}
        {!!row.conditions_json.length && <div className="analyst-condition-pills">{row.conditions_json.map((c, i) => <span key={i} className={c.status.toLowerCase()} title={c.reason || ""}>{c.label}：{hits[c.status]}{c.value != null && `（${c.value}${c.kind === "volume" ? "倍" : c.currency || ""}）`}{c.matched_count != null && ` · ${c.matched_count}条`}</span>)}</div>}
        {row.result_json.judgment && <><h4>{verdicts[row.result_json.verdict || ""] || "每日判断"}{row.result_json.confidence != null && <small> · 证据支持度 {Math.round(row.result_json.confidence * 100)}%</small>}</h4><p>{row.result_json.judgment}</p><details><summary>本次变化与下一步关注</summary><p>{row.result_json.summary}</p><ul>{[...(row.result_json.changes || []), ...(row.result_json.next_focus || [])].map((s, i) => <li key={i}>{s}</li>)}</ul></details></>}
        {row.error_message && <p className="analyst-error">{row.error_message}</p>}
        <details><summary>采集与条件核验明细</summary><p className="analyst-muted">使用关注条件第{row.config_snapshot_json.config_version}版；价格为原币不复权日线收盘价，量能对比不含当日的前N个交易日均量。关键词匹配属于线索。</p>{row.progress_json.collection?.map((r, i) => <p key={i}>{categoryNames[r.category] || r.category}：{r.status === "SUCCESS" ? `已获取${r.count}条` : `未完成：${r.error || "来源不可用"}`}{r.source && ` · ${r.source}`}</p>)}{row.conditions_json.map((c, i) => <p key={i}>{c.label}：{hits[c.status]}{c.threshold != null && ` · 阈值 ${c.threshold}`}{c.trade_date && ` · 行情日期 ${c.trade_date}`}{c.scanned_count != null && ` · 扫描${c.scanned_count}篇`}{c.reason && ` · ${c.reason}`}</p>)}</details>
        <button type="button" className="analyst-text-button" disabled={busy || pending(row.status)} onClick={() => void action(async () => { const data = await analystFollowupApi.evidence(analysis.id, row.id); setSources({ review: row, rows: data.evidence || [] }); })}>查看本次证据</button>
      </article>)}</div>
      {total > 10 && <div className="analyst-pagination"><button disabled={!offset} onClick={() => setOffset(value => Math.max(0, value - 10))}>上一页</button><span>{Math.floor(offset / 10) + 1} / {Math.ceil(total / 10)}页 · {total}条</span><button disabled={offset + 10 >= total} onClick={() => setOffset(value => value + 10)}>下一页</button></div>}
    </>}
    {editor && <div className="analyst-modal-backdrop"><section className="analyst-modal" role="dialog" aria-modal="true" aria-labelledby="analyst-confirm-title"><header><h3 id="analyst-confirm-title">确认判断与关注点</h3><button type="button" aria-label="关闭确认窗口" onClick={() => setEditor(null)}><X size={19} /></button></header><p>下面是AI建议，可直接修改。确认后形成报告新版本，并作为后续每日复核的依据。</p><label>最终判断<textarea aria-label="最终判断" rows={6} maxLength={2500} value={judgment} onChange={e => setJudgment(e.target.value)} /></label><label>关注点（每行一项）<textarea aria-label="确认关注点" rows={5} value={focus} onChange={e => setFocus(e.target.value)} /></label>{message && <p className="analyst-error">{message}</p>}<footer><button type="button" className="analyst-secondary" onClick={() => setEditor(null)}>继续讨论</button><button type="button" className="analyst-primary" disabled={busy || judgment.trim().length < 10 || !focus.trim()} onClick={() => void action(async () => { await analystFollowupApi.confirm(analysis.id, { dialogue_id: editor.id, expected_revision_id: confirmationBase.current, judgment: judgment.trim(), focus_points: focus.split("\n").map(s => s.trim()).filter(Boolean) }); setEditor(null); await load(); setMessage("已更新报告，原始分析和历史确认版本均已保留。"); })}>确认并更新报告</button></footer></section></div>}
    {config && <div className="analyst-modal-backdrop"><section className="analyst-modal" role="dialog" aria-modal="true" aria-labelledby="analyst-watch-title"><header><h3 id="analyst-watch-title">关注价位与每日监控条件</h3><button type="button" aria-label="关闭关注条件" onClick={() => setConfig(null)}><X size={19} /></button></header><p>价格、量能、事件分别判断，任一命中会记录；每天均会复核判断。价格单位：{analysis.market === "HK" ? "港元" : "元"}。</p><div className="analyst-monitor-grid"><label>关注下限<input aria-label="关注下限" type="number" min="0.0001" step="any" value={config.price_lower ?? ""} onChange={e => setConfig({ ...config, price_lower: e.target.value ? Number(e.target.value) : null })} placeholder="跌至或低于" /></label><label>关注上限<input aria-label="关注上限" type="number" min="0.0001" step="any" value={config.price_upper ?? ""} onChange={e => setConfig({ ...config, price_upper: e.target.value ? Number(e.target.value) : null })} placeholder="涨至或高于" /></label><label>量能条件<select aria-label="量能条件" value={config.volume_mode} onChange={e => setConfig({ ...config, volume_mode: e.target.value as AnalystWatchConfig["volume_mode"], volume_ratio: e.target.value === "SHRINK" ? 0.7 : 1.5 })}><option value="ANY">不限定量能</option><option value="EXPAND">放量</option><option value="SHRINK">缩量</option></select></label>{config.volume_mode !== "ANY" && <><label>相对均量倍数<input aria-label="相对均量倍数" type="number" step="0.1" min="0.01" value={config.volume_ratio} onChange={e => setConfig({ ...config, volume_ratio: Number(e.target.value) })} /></label><label>此前交易日数<input type="number" min="5" max="120" value={config.volume_window} onChange={e => setConfig({ ...config, volume_window: Number(e.target.value) })} /></label></>}</div><h4>新闻与公告条件</h4><p className="analyst-muted">每组关键词任意一个命中即记录；可增加多组，扫描标题与已有正文。</p>{config.event_conditions.map((r, i) => <div className="analyst-event-rule" key={i}><select aria-label={`事件类型${i + 1}`} value={r.category} onChange={e => setConfig({ ...config, event_conditions: config.event_conditions.map((s, j) => i === j ? { ...s, category: e.target.value as typeof s.category } : s) })}><option value="ANY">新闻与公告</option><option value="NOTICE">公告</option><option value="NEWS">新闻</option></select><input aria-label={`事件关键词${i + 1}`} placeholder="回购、收购、订单（逗号分隔）" value={r.keywords.join("，")} onChange={e => setConfig({ ...config, event_conditions: config.event_conditions.map((s, j) => i === j ? { ...s, keywords: e.target.value.split(/[,，、；;]/) } : s) })} /><button type="button" aria-label={`删除事件条件${i + 1}`} onClick={() => setConfig({ ...config, event_conditions: config.event_conditions.filter((_, j) => i !== j) })}><X size={16} /></button></div>)}<button type="button" className="analyst-text-button" disabled={config.event_conditions.length >= 12} onClick={() => setConfig({ ...config, event_conditions: [...config.event_conditions, { category: "ANY", keywords: [""] }] })}>＋增加事件条件</button><label>持续复核的关注点<textarea aria-label="持续复核的关注点" rows={3} maxLength={2500} value={config.review_focus} onChange={e => setConfig({ ...config, review_focus: e.target.value })} /></label><div className="analyst-monitor-grid"><label>每日检查时间（北京时间）<input aria-label="每日检查时间" type="time" value={`${String(config.schedule_hour).padStart(2, "0")}:${String(config.schedule_minute).padStart(2, "0")}`} onChange={e => { const [h, m] = e.target.value.split(":").map(Number); setConfig({ ...config, schedule_hour: h, schedule_minute: m }); }} /></label><label className="analyst-checkbox"><input type="checkbox" checked={config.enabled} onChange={e => setConfig({ ...config, enabled: e.target.checked })} />启用每日监控</label></div><p className="analyst-muted">保存后使用上方当前模型：{model}。后台服务需持续运行；当天任务去重，失败记录保留并可手动再检查。</p>{message && <p className="analyst-error">{message}</p>}<footer><button type="button" className="analyst-secondary" onClick={() => setConfig(null)}>取消</button><button type="button" className="analyst-primary" disabled={busy || !model} onClick={() => void action(async () => { await analystFollowupApi.saveWatch(analysis.id, { ...config, model_instance_code: model }); setConfig(null); setView("timeline"); await load(); setMessage("关注条件已保存，已加入每日监控。可立即检查验证。"); })}>保存关注条件</button></footer></section></div>}
    {sources && <div className="analyst-modal-backdrop"><section className="analyst-modal" role="dialog" aria-modal="true" aria-label="每日复核证据"><header><h3>{sources.review.business_date} · 本次证据</h3><button aria-label="关闭每日证据" onClick={() => setSources(null)}><X size={18} /></button></header><p>{sources.rows.length}条实际分析摘要；保留当时快照。</p>{sources.rows.map(r => <details key={r.evidence_id}><summary>{r.title} · {r.observed_at?.slice(0, 10)}</summary><p>{r.excerpt || "结构化资料，见下方记录"}</p><small>{r.source} · {r.evidence_id}</small>{r.data && <pre>{JSON.stringify(r.data, null, 2)}</pre>}</details>)}</section></div>}
  </section>;
}
