import type { SelectionCandidate, SelectionRecord } from "./selectionApi";

const names: Record<string, string> = {NEWS: "新闻事件", NOTICE: "公告披露", FINANCIAL: "财务业绩", PRICE_VOLUME: "量价趋势", FUND_FLOW: "资金进出", COMPANY: "经营与股东", GRAPH: "公司关联与图谱", POSITIVE: "偏利好", NEGATIVE: "偏利空", MIXED: "影响交织", NEUTRAL: "中性", UNKNOWN: "待判断", WATCH: "值得关注", PASS: "暂不关注"};
const array = (value: unknown): SelectionRecord[] => Array.isArray(value) ? value : [];
const strings = (value: unknown): string[] => Array.isArray(value) ? value.filter(item => typeof item === "string") : [];

export function SelectionAnalysis({candidate, onDocument}: {candidate: SelectionCandidate; onDocument?: (id: number) => void}) {
  const analysis = candidate.analysis_json;
  const model = (analysis.model || analysis.local_multi_source || {}) as SelectionRecord;
  const localFallback = analysis.model_output_status === "FAILED" && !!analysis.local_multi_source;
  const usage = (analysis.input_usage || {}) as SelectionRecord;
  const dimensions = array(model.dimensions), events = array(model.events);
  const documents = [...array(candidate.evidence_json.documents), ...array(candidate.evidence_json.evidence_context?.documents)];
  const document = (id: number) => documents.find(item => item.id === id);
  const reference = (id: number) => <button type="button" className="analysis-reference" key={id} title={document(id)?.title || `文档${id}`} onClick={() => onDocument?.(id)}>{document(id)?.source_table === "stock_news" ? "新闻" : document(id)?.source_table === "stock_notice" ? "公告" : "证据"} · {document(id)?.title || `#${id}`}</button>;
  const money = (value: unknown) => typeof value === "number" ? `${value.toFixed(2)} 元` : "证据不足";
  if (analysis.model_output_status !== "SUCCESS" && !localFallback) return <p className="batch-alert error">模型分析尚未成功完成，请查看错误后重试。</p>;
  return <section className="selection-analysis">
    {localFallback && <p className="batch-alert warning">模型服务本次未返回有效结果。以下为本地知识库、图谱和结构化数据的证据盘点，不是模型投资结论；模型恢复后请重新分析。</p>}
    <div className="analysis-verdict"><span className={`analysis-signal ${model.decision === "WATCH" ? "watch" : ""}`}>{names[model.decision] || "待判断"}</span><h3>{model.headline || "历史分析摘要"}</h3><small>模型置信度 {typeof model.confidence === "number" ? `${Math.round(model.confidence * 100)}%` : "—"} · 置信度不等于收益概率</small></div>
    <div className="analysis-metrics"><div><small>推断目标价</small><strong>{money(model.target_price)}</strong></div><div><small>观察止损价</small><strong>{money(model.stop_price)}</strong></div><div><small>观察周期</small><strong>10 个交易日</strong></div></div>
    {dimensions.length ? <><h3>逐项研判</h3><div className="analysis-dimensions">{dimensions.map(item => <article key={item.dimension} className={`analysis-dimension ${item.status === "INSUFFICIENT" ? "missing" : ""}`}><div className="analysis-card-heading"><h4>{names[item.dimension]}</h4><span className={`analysis-direction ${String(item.direction).toLowerCase()}`}>{item.status === "INSUFFICIENT" ? "证据不足" : names[item.direction]}</span></div><p>{item.summary}</p><div className="analysis-references">{(item.evidence_document_ids || []).map(reference)}</div>{!!item.evidence_fact_ids?.length && <small>{item.evidence_fact_ids.length} 条数值/事实依据</small>}{!!item.evidence_relation_ids?.length && <small>{item.evidence_relation_ids.length} 条图谱关系依据</small>}</article>)}</div></> : <><p className="batch-missing">此历史批次尚未生成分项分析；重新分析可获得新闻、公告等七项研判。</p><p>{String(analysis.summary || "")}</p></>}
    {!!events.length && <><h3>新闻与公告如何影响判断</h3><div className="analysis-events">{events.map((item, index) => <article key={index} className="analysis-event"><div className="analysis-card-heading"><h4>{item.title}</h4><span className={`analysis-direction ${String(item.direction).toLowerCase()}`}>{names[item.direction]}</span></div><p>{item.mechanism}</p><small>{item.timing}</small><blockquote>{item.quote}</blockquote>{reference(item.document_id)}</article>)}</div></>}
    <div className="analysis-checks">{[["支持因素", model.positives], ["主要风险", model.risks], ["接下来观察", model.watch_conditions]].map(([title, value]) => strings(value).length > 0 && <div key={String(title)}><h4>{String(title)}</h4><ul>{strings(value).map((item, index) => <li key={index}>{item}</li>)}</ul></div>)}</div>
    <details className="analysis-input"><summary>本次实际读取与引用</summary><p>已送入模型 {array(usage.documents).length || documents.length} 篇文档 · 模型引用 {model.evidence_document_ids?.length || 0} 篇 · {usage.relations ?? 0} 条图谱关系。引用表示模型使用了该材料，不等于事实已核验。</p>{array(usage.selected_scope?.knowledge_bases).map(row => <small key={row.id}>知识库 #{row.id} · {row.code} · {row.version}</small>)}{array(usage.selected_scope?.graphs).map(row => <small key={row.id}>图谱 #{row.id} · {row.code} · {row.version}</small>)}</details>
  </section>;
}
