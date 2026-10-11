import { useEffect, useState } from "react";
import { request } from "./api";
import { batchGovernanceApi, type BatchGovernanceJob } from "./batchGovernanceApi";
import "./documentSemantics.css";

type Doc = { source_table: string; record_id: number; market: string; symbol: string; title: string; published_at: string;
  source_name: string; status: string; error?: string; semantic_complete: boolean; body_characters: number;
  body_available: boolean; skill_version?: string; current_skill_version: string; event_count: number; relation_count: number; version_count: number };
type Impact = { dimension: string; direction: string; rationale: string; assertion_type: string; attribution: string; evidence_quote: string; uncertainty: string };
type Metric = { name: string; raw_value: string; unit: string; qualifier: string; period: string; normalized_value: string; normalized_unit: string; evidence_quote: string };
type Event = { key: string; event_type: string; summary: string; assertion_type: string; attribution: string; event_time_raw?: string;
  event_date?: string; direction: string; evidence_quotes: string[]; participant_keys: string[]; metrics: Metric[]; impacts: Impact[]; fact_id?: string };
type Participant = { key: string; name: string; role: string; aliases: string[]; evidence_quote: string; entity_id?: string; resolution_status: string };
type Relation = { source_key: string; target_key: string; predicate: string; explanation: string; assertion_type: string; attribution: string; evidence_quote: string; fact_id?: string };
type Analysis = { reading: { overview: string; sections: { topic: string; summary: string; evidence_quotes: string[] }[]; uncertainties: string[] };
  participants: Participant[]; events: Event[]; relations: Relation[]; uncertainties: string[];
  model_calls: { phase: string; id: number; model_version: string }[]; schema_version: string };
type Detail = Doc & { body: string; url?: string; analysis?: Analysis; note: string; selected_version: number;
  state: { content_scope?: string; skill_version?: string; observed_at?: string; error?: string; pipeline_run_id?: number;
    agent_execution_run_id?: string; skill_execution_run_id?: string; evidence_id?: string; content_hash?: string };
  versions: { index: number; skill_version: string; semantic_schema_version?: string; observed_at: string; status: string }[] };
const labels: Record<string, string> = {
  REPORTED_FACT: "文中转述事实 · 待核验", AUTHOR_OPINION: "作者／专家观点", MODEL_INFERENCE: "模型推断",
  POSITIVE: "正向", NEGATIVE: "负向", NEUTRAL: "中性", MIXED: "正负并存", UNKNOWN: "证据不足",
  PROFIT: "利润影响", CASH_FLOW: "现金流影响", FINANCING: "融资影响", GOVERNANCE: "治理风险", REGULATORY: "监管风险", OPERATION: "经营影响", STOCK_PRICE: "股价影响",
  INVOLVES: "涉及", CAUSES: "导致", CONTRIBUTES_TO: "促成", PRECEDES: "先于", FINANCES: "融资安排", SUBSCRIBES: "认购安排",
  RENAMED_TO: "更名", CONTROLS: "控制关系", RELATED_PARTY: "关联方", EXPLAINS: "解释依据", DISPUTES: "争议",
  ANALYZED: "已分析 · 候选待审", NO_EVENT: "未发现相关事件", MISSING_BODY: "正文缺失", NOT_PROCESSED: "尚未分析",
  EXTRACTION_FAILED: "分析失败", PARTIAL_BODY: "正文部分分析", BODY_DOWNLOAD_DEFERRED: "待补正文",
  RESOLVED_EXISTING: "已关联现有主体", DOCUMENT_MENTION: "文档提及 · 待消歧", AMBIGUOUS: "身份存在歧义",
  EXACT: "明确数值", APPROXIMATE: "约数", UPPER_BOUND: "上限", LOWER_BOUND: "下限", ESTIMATE: "预计值",
  PENDING: "等待执行", RUNNING: "执行中", COMPLETED: "本轮结束", PARTIAL: "部分完成", FAILED: "执行失败", RETRY: "等待重试",
};
const name = (value: string) => labels[value] || value;
const prefix = "/document-governance";
const docKey = (doc: Pick<Doc, "source_table" | "record_id">) => `${doc.source_table}:${doc.record_id}`;
function Quote({ text }: { text: string }) { return <details className="ds-quote"><summary>查看原文证据</summary><blockquote>{text}</blockquote></details>; }
function Badge({ type }: { type: string }) { return <span className={`ds-badge ds-${type.toLowerCase()}`}>{name(type)}</span>; }

export function DocumentSemanticWorkbench() {
  const [market, setMarket] = useState("CN_A"), [symbol, setSymbol] = useState("000159"), [keyword, setKeyword] = useState("");
  const [kind, setKind] = useState("NEWS"), [offset, setOffset] = useState(0), [rows, setRows] = useState<Doc[]>([]), [total, setTotal] = useState(0);
  const [stockName, setStockName] = useState(""), [selected, setSelected] = useState<Doc[]>([]), [detail, setDetail] = useState<Detail | null>(null);
  const [tab, setTab] = useState("reading"), [error, setError] = useState(""), [busy, setBusy] = useState(false), [loading, setLoading] = useState(false);
  const [job, setJob] = useState<BatchGovernanceJob | null>(null);
  const active = !!job && ["PENDING", "RUNNING", "RETRY"].includes(String(job.job_status || job.status));
  async function load(nextOffset = offset) {
    setLoading(true);
    try {
      const params = new URLSearchParams({ market, symbol: symbol.trim(), kind, q: keyword, offset: String(nextOffset), limit: "20" });
      const result = await request<{ items: Doc[]; total: number; stock_name?: string }>(`${prefix}/documents?${params}`);
      setRows(result.items); setTotal(result.total); setStockName(result.stock_name || "未找到股票");
    } catch (e) { setError(e instanceof Error ? e.message : "文档读取失败"); } finally { setLoading(false); }
  }
  async function open(doc: Pick<Doc, "source_table" | "record_id">, version?: number) {
    try {
      const result = await request<Detail>(`${prefix}/documents/${doc.source_table}/${doc.record_id}${version === undefined ? "" : `?version_index=${version}`}`);
      setDetail(result); setTab("reading");
    } catch (e) { setError(e instanceof Error ? e.message : "成果读取失败"); }
  }
  useEffect(() => { void load(); }, [kind, offset]);
  useEffect(() => {
    const params = new URLSearchParams(window.location.hash.split("?")[1] || "");
    if (params.get("id") && ["stock_news", "stock_notice"].includes(params.get("table") || "")) {
      void open({ source_table: params.get("table")!, record_id: Number(params.get("id")) });
    }
  }, []);
  useEffect(() => {
    if (!active || !job) return;
    let cancelled = false;
    const tick = async () => {
      try {
        const result = await batchGovernanceApi.get(job.job_id);
        if (cancelled) return;
        setJob(result);
        if (!["PENDING", "RUNNING", "RETRY"].includes(String(result.job_status || result.status))) { await load(); if (detail) await open(detail); }
      } catch (e) { if (!cancelled) setError(e instanceof Error ? e.message : "任务状态读取失败"); }
    };
    const timer = window.setInterval(() => void tick(), 3000);
    void tick();
    return () => { cancelled = true; window.clearInterval(timer); };
  }, [active, job?.job_id]);
  async function analyze(docs: Doc[]) {
    if (!docs.length) return;
    setBusy(true); setError("");
    try {
      const result = await request<BatchGovernanceJob>(`${prefix}/analyze`, { method: "POST", body: JSON.stringify({
        documents: docs.map(d => ({ source_table: d.source_table, record_id: d.record_id })), idempotency_key: crypto.randomUUID(),
      }) });
      setJob(result); setSelected([]);
    } catch (e) { setError(e instanceof Error ? e.message : "创建语义治理任务失败"); } finally { setBusy(false); }
  }
  const analysis = detail?.analysis;
  const titles = Object.fromEntries([...(analysis?.participants || []).map(p => [p.key, p.name]), ...(analysis?.events || []).map(e => [e.key, e.event_type])]);
  const impacts = analysis?.events.flatMap(event => event.impacts.map(impact => ({ event: event.event_type, impact }))) || [];
  return <div className="ds">
    <header className="ds-heading"><div><p className="eyebrow">DOCUMENT SEMANTIC GOVERNANCE</p><h1>新闻公告语义治理</h1><p>全文理解 → 信息抽取 → 证据校验 → 候选事实与关系入库</p></div><a href="#/operations/records">查看治理记录与报告</a></header>
    <p className="ds-note">区分文中转述事实、作者观点与模型推断；业务影响按维度解释。原文引文核验通过仍需事实审核，不能视为已确认的股价判断。</p>
    {error && <p role="alert" className="ds-error">{error}</p>}
    <section className="ds-panel">
      <form className="ds-toolbar" onSubmit={e => { e.preventDefault(); setOffset(0); setSelected([]); void load(0); }}>
        <label>市场<select value={market} onChange={e => setMarket(e.target.value)}><option value="CN_A">A股</option><option value="HK">港股</option><option value="NEEQ">新三板</option><option value="NEEQ_INNOVATION">创新层</option></select></label>
        <label>股票代码<input aria-label="股票代码" value={symbol} onChange={e => setSymbol(e.target.value)} /></label>
        <label>标题关键词<input value={keyword} onChange={e => setKeyword(e.target.value)} placeholder="例如：计提、定增" /></label>
        <label>文档类型<select value={kind} onChange={e => { setKind(e.target.value); setOffset(0); setSelected([]); }}><option value="NEWS">新闻</option><option value="NOTICE">公告</option><option value="ALL">新闻与公告</option></select></label>
        <button disabled={loading} type="submit">查询</button>
        <button disabled={busy || active || !selected.length} type="button" onClick={() => void analyze(selected)}>分析选中文档（{selected.length} / 10）</button>
      </form>
      <p>{stockName} · {market} / {symbol} · 共 {total} 份文档。逐篇分析保存独立证据与历史版本。</p>
      {job && <div className="ds-job" role="status"><strong>专项任务 #{job.job_id} · {name(String(job.job_status || job.status))}</strong><progress max={100} value={job.progress || 0} /><span>流程进度 {job.progress || 0}% · {job.current_stage || "等待执行"}</span><a href="#/operations/records">查看阶段报告</a>{!active && <span>本轮结果：{name(job.result_status || "UNKNOWN")}；请检查成果与缺口。</span>}</div>}
      <div className="ds-table-wrap"><table><thead><tr><th>选择</th><th>新闻／公告</th><th>来源与时间</th><th>正文与治理状态</th><th>语义成果</th><th>操作</th></tr></thead><tbody>
        {rows.map(doc => <tr key={docKey(doc)}><td><input type="checkbox" aria-label={`选择${doc.title}`} checked={selected.some(x => docKey(x) === docKey(doc))} onChange={e => { if (e.target.checked && selected.length < 10) setSelected([...selected, doc]); else setSelected(selected.filter(x => docKey(x) !== docKey(doc))); }} disabled={active || (selected.length >= 10 && !selected.some(x => docKey(x) === docKey(doc)))} /></td>
          <td><button className="ds-link" onClick={() => void open(doc)}>{doc.title}</button><small>{doc.source_table === "stock_news" ? "新闻" : "公告"} · 记录 #{doc.record_id}</small></td>
          <td>{doc.source_name}<small>{doc.published_at}</small></td>
          <td><Badge type={doc.status} /><small>{doc.body_available ? `${doc.body_characters} 字正文` : "正文需补取"} · {doc.semantic_complete ? "当前版本已分析" : "待语义分析/重试"}</small>{doc.error && <small className="ds-error">{doc.error}</small>}</td>
          <td>{doc.event_count} 条事件 · {doc.relation_count} 条关系<small>{doc.version_count} 轮治理 · Skill {doc.skill_version || "未执行"}</small></td>
          <td><button onClick={() => void open(doc)}>查看成果</button><button disabled={busy || active} onClick={() => void analyze([doc])}>{doc.semantic_complete ? "复核（幂等）" : "语义分析"}</button></td></tr>)}
        {!rows.length && <tr><td colSpan={6}>{loading ? "读取中…" : "当前范围没有新闻公告，请先采集业务数据。"}</td></tr>}
      </tbody></table></div>
      <div className="ds-toolbar"><span>共 {total} 条 · 第 {Math.floor(offset / 20) + 1} / {Math.max(1, Math.ceil(total / 20))} 页</span><button disabled={loading || offset === 0} onClick={() => setOffset(Math.max(0, offset - 20))}>上一页</button><button disabled={loading || offset + 20 >= total} onClick={() => setOffset(offset + 20)}>下一页</button></div>
    </section>
    {detail && <section className="ds-panel ds-result" aria-label="文档语义成果">
      <header className="ds-heading"><div><h2>{detail.title}</h2><p>{detail.source_name} · {detail.published_at} · Skill {detail.state.skill_version || "未执行"}</p></div><button onClick={() => setDetail(null)}>关闭成果</button></header>
      <div className="ds-toolbar"><label>治理版本<select aria-label="治理版本" value={detail.selected_version} onChange={e => void open(detail, Number(e.target.value))}>{detail.versions.map(v => <option value={v.index} key={v.index}>第 {v.index + 1} 轮 · {v.semantic_schema_version || "旧事件摘要"} · {name(v.status)} · v{v.skill_version}</option>)}</select></label><button disabled={busy || active} onClick={() => void analyze([detail])}>执行当前版本语义治理</button><span>{detail.state.content_scope === "BODY_EXCERPT" ? "仅正文摘录，全文未完成" : "所选版本正文与证据可核验"}</span></div>
      {detail.state.error && <p className="ds-error">{detail.state.error}</p>}
      <p className="ds-note">{detail.note}</p>
      <div className="ds-tabs" role="tablist" aria-label="语义成果分类">{[["reading", "全文理解与影响"], ["events", "事件与数字"], ["relations", "主体与关键联系"], ["timeline", "事件时间线"], ["source", "原文与证据"], ["audit", "执行与版本"]].map(([key, text]) => <button key={key} role="tab" aria-selected={tab === key} className={tab === key ? "active" : ""} onClick={() => setTab(key)}>{text}</button>)}</div>
      {!analysis && tab !== "source" && tab !== "audit" && <p>所选版本尚无完整语义成果。旧事件摘要与报告仍保留；点击“执行当前版本语义治理”补充分析，失败可查看原因并重试。</p>}
      {analysis && tab === "reading" && <>
        <article className="ds-overview"><h3>全文概览</h3><p>{analysis.reading.overview}</p></article>
        <div className="ds-topic-grid">{analysis.reading.sections.map((section, i) => <article className="ds-card" key={i}><h3>{section.topic}</h3><p>{section.summary}</p>{section.evidence_quotes.map((quote, q) => <Quote text={quote} key={q} />)}</article>)}</div>
        <h3>分维度影响与依据</h3><div className="ds-impact-grid">{impacts.map(({event, impact}, i) => <article className={`ds-card ds-impact-${impact.direction.toLowerCase()}`} key={i}><h4>{name(impact.dimension)} · <Badge type={impact.direction} /></h4><small>{event}</small><p>{impact.rationale}</p><Badge type={impact.assertion_type} /><p>归属：{impact.attribution}</p><p className="ds-muted">边界：{impact.uncertainty}</p><Quote text={impact.evidence_quote} /></article>)}</div>
        <h3>不确定性与待核验事项</h3><ul>{[...new Set([...analysis.reading.uncertainties, ...analysis.uncertainties])].map((item, i) => <li key={i}>{item}</li>)}</ul>
      </>}
      {analysis && tab === "events" && <div className="ds-event-list">{analysis.events.map(event => <article className="ds-card" key={event.key}><header><h3>{event.event_type}</h3><Badge type={event.assertion_type} /></header><p>{event.summary}</p><p className="ds-muted">归属：{event.attribution} · 原文时间：{event.event_time_raw || "未明确"} · {event.fact_id ? "已存入候选事实" : "待存储"}</p>
        {event.metrics.length > 0 && <div className="ds-table-wrap"><table><thead><tr><th>指标</th><th>原文值</th><th>性质／期间</th><th>标准值</th><th>证据</th></tr></thead><tbody>{event.metrics.map((metric, i) => <tr key={i}><td>{metric.name}</td><td>{metric.raw_value} {metric.unit}</td><td>{name(metric.qualifier)}<small>{metric.period}</small></td><td>{metric.normalized_value} {metric.normalized_unit}</td><td><Quote text={metric.evidence_quote} /></td></tr>)}</tbody></table></div>}
        {event.impacts.map((impact, i) => <p key={i}>{name(impact.dimension)}：<Badge type={impact.direction} /> {impact.rationale}</p>)}
        {event.evidence_quotes.map((quote, i) => <Quote key={i} text={quote} />)}
      </article>)}</div>}
      {analysis && tab === "relations" && <><h3>参与主体与身份</h3><div className="ds-topic-grid">{analysis.participants.map(p => <article className="ds-card" key={p.key}><h3>{p.name}</h3><p>{p.role}</p><p>别名：{p.aliases.join("、") || "无"}</p><Badge type={p.resolution_status} />{p.entity_id && <small>主体编号：{p.entity_id}</small>}<Quote text={p.evidence_quote} /></article>)}</div><h3>关键联系 · 均为有证据候选</h3><div className="ds-table-wrap"><table><thead><tr><th>起点</th><th>联系</th><th>终点</th><th>缘由与归属</th><th>原文证据</th></tr></thead><tbody>{analysis.relations.map((link, i) => <tr key={i}><td>{titles[link.source_key]}</td><td>{name(link.predicate)}</td><td>{titles[link.target_key]}</td><td>{link.explanation}<p><Badge type={link.assertion_type} /> {link.attribution}</p></td><td><Quote text={link.evidence_quote} /></td></tr>)}</tbody></table></div></>}
      {analysis && tab === "timeline" && <ol className="ds-timeline">{[...analysis.events].sort((a,b) => (a.event_date || "9999").localeCompare(b.event_date || "9999")).map(event => <li key={event.key}><time>{event.event_time_raw || "原文未明确日期"}</time><div><h3>{event.event_type}</h3><Badge type={event.assertion_type} /><p>{event.summary}</p>{event.evidence_quotes.map((q,i) => <Quote text={q} key={i} />)}</div></li>)}</ol>}
      {tab === "source" && <><h3>所选版本原文</h3><p>{detail.body.length} 字 · 证据编号：{detail.state.evidence_id || "未生成"}</p>{detail.url && <a href={detail.url} target="_blank" rel="noreferrer">打开原始来源</a>}<div className="ds-source">{detail.body || "正文未获取，分析时将尝试补取已登记来源。"}</div></>}
      {tab === "audit" && <><h3>执行与来源追溯</h3><dl className="ds-audit"><dt>治理时间</dt><dd>{detail.state.observed_at || "未执行"}</dd><dt>流水线编号</dt><dd>{detail.state.pipeline_run_id || "未执行"}</dd><dt>智能体执行编号</dt><dd>{detail.state.agent_execution_run_id || "未执行"}</dd><dt>Skill执行编号</dt><dd>{detail.state.skill_execution_run_id || "未执行"}</dd><dt>正文哈希</dt><dd>{detail.state.content_hash || "未生成"}</dd></dl>{analysis?.model_calls.map(call => <p key={call.id}>{call.phase === "SEMANTIC_READING" ? "全文语义理解" : "结构化抽取"} · 真实模型调用 #{call.id} · {call.model_version}</p>)}<details><summary>查看完整治理契约与原始审计</summary><pre>{JSON.stringify(detail.state, null, 2)}</pre></details></>}
    </section>}
  </div>;
}
