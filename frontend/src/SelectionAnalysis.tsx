import type { SelectionCandidate, SelectionRecord, SelectionScorecard, SelectionScoreDimensionResult } from "./selectionApi";

const names: Record<string, string> = {NEWS: "新闻事件", NOTICE: "公告披露", FINANCIAL: "财务业绩", PRICE_VOLUME: "量价趋势", FUND_FLOW: "资金进出", COMPANY: "经营与股东", GRAPH: "公司关联与图谱", POSITIVE: "偏利好", NEGATIVE: "偏利空", MIXED: "影响交织", NEUTRAL: "中性", UNKNOWN: "待判断", WATCH: "值得关注", PASS: "暂不关注", FOLLOW_UP: "后续进展", VALIDATES: "相互验证", CONTRADICTS: "信息冲突", REALIZES: "已兑现", FAILS_TO_REALIZE: "未兑现", SAME_TOPIC: "同一事项", EXECUTIVE_UNLOCKING_RISK: "高管与解禁线索", SHAREHOLDER_CONFIDENCE_SIGNAL: "股东信心信号", RELATED_SECURITY_IMPACT: "关联证券影响", UNVERIFIED: "待验证", PARTIALLY_REALIZED: "部分兑现", REALIZED: "已兑现", NOT_REALIZED: "未兑现", CONFLICTED: "存在冲突", INSUFFICIENT: "证据不足"};
const array = (value: unknown): SelectionRecord[] => Array.isArray(value) ? value : [];
const strings = (value: unknown): string[] => Array.isArray(value) ? value.filter(item => typeof item === "string") : [];
export type AnalysisDimension = "NEWS" | "NOTICE" | "FINANCIAL" | "PRICE_VOLUME" | "FUND_FLOW" | "COMPANY" | "GRAPH";

function ScoreRadar({ rows }: { rows: SelectionScoreDimensionResult[] }) {
  const size = 280, center = size / 2, radius = 84;
  const point = (index: number, value: number) => {
    const angle = -Math.PI / 2 + index * Math.PI * 2 / rows.length;
    const length = radius * Math.max(0, Math.min(100, value)) / 100;
    return [center + Math.cos(angle) * length, center + Math.sin(angle) * length];
  };
  const ring = (value: number) => rows.map((_, index) => point(index, value).join(",")).join(" ");
  const complete = rows.length === 8 && rows.every(row => typeof (row.observed_score ?? row.score) === "number");
  const values = rows.map(row => typeof (row.observed_score ?? row.score) === "number" ? Number(row.observed_score ?? row.score) : null);
  const segments = rows.flatMap((_, index) => {
    const next = (index + 1) % rows.length;
    return values[index] != null && values[next] != null ? [[point(index, values[index]!), point(next, values[next]!)]] : [];
  });
  return <figure className="score-radar">
    <svg viewBox={`0 0 ${size} ${size}`} role="img" aria-label="八维原始评分雷达图，缺失维度留空">
      {[25, 50, 75, 100].map(value => <polygon key={value} points={ring(value)} className="score-radar-ring" />)}
      {rows.map((row, index) => { const end = point(index, 100); const label = point(index, 126); return <g key={row.dimension_code}><line x1={center} y1={center} x2={end[0]} y2={end[1]} className="score-radar-axis"/><text x={label[0]} y={label[1]} textAnchor="middle" dominantBaseline="middle">{row.dimension_name.replace("与", "/")}</text></g>; })}
      {complete && <polygon points={values.map((value, index) => point(index, value!).join(",")).join(" ")} className="score-radar-area" />}
      {!complete && segments.map((segment, index) => <line key={index} x1={segment[0][0]} y1={segment[0][1]} x2={segment[1][0]} y2={segment[1][1]} className="score-radar-segment" />)}
      {values.map((value, index) => value == null ? null : <g key={rows[index].dimension_code}><circle cx={point(index, value)[0]} cy={point(index, value)[1]} r="4" className="score-radar-point"/><text x={point(index, value)[0]} y={point(index, value)[1] - 8} textAnchor="middle" className="score-radar-value">{value.toFixed(0)}</text></g>)}
    </svg>
    <figcaption>原始维度分 · 缺失项留空</figcaption>
  </figure>;
}

export function SelectionAnalysis({candidate, onDocument, onBackfillDimension, onRescore}: {candidate: SelectionCandidate; onDocument?: (id: number) => void; onBackfillDimension?: (dimension: AnalysisDimension) => void; onRescore?: () => void}) {
  const analysis = candidate.analysis_json;
  const model = (analysis.model || {}) as SelectionRecord;
  const modelFailed = analysis.model_output_status === "FAILED";
  const usage = (analysis.input_usage || {}) as SelectionRecord;
  const dimensions = array(model.dimensions), events = array(model.events);
  const timeline = array(model.event_timeline), links = array(model.event_links), findings = array(model.validation_findings);
  const confidence = (model.confidence_breakdown || {}) as SelectionRecord;
  const period = (candidate.evidence_json.analysis_period || usage.analysis_period || {}) as SelectionRecord;
  const scoring = (analysis.scoring || null) as SelectionScorecard | null;
  const documents = [
    ...array(candidate.evidence_json.documents),
    ...array(candidate.evidence_json.related_security_documents),
    ...array(candidate.evidence_json.evidence_context?.documents),
    ...array(candidate.evidence_json.evidence_context?.related_security_documents),
  ];
  const document = (id: number) => documents.find(item => Number(item.id) === Number(id));
  const reference = (id: number) => {
    const row = document(id);
    const scope = row?.relation_scope === "RELATED_SECURITY" ? `关联 ${row.symbol || "证券"} · ` : "";
    return <button type="button" className="analysis-reference" key={id} title={row?.title || `文档${id}`} onClick={() => onDocument?.(id)}>{scope}{row?.source_table === "stock_news" ? "新闻" : row?.source_table === "stock_notice" ? "公告" : "证据"} · {row?.title || `#${id}`}</button>;
  };
  const money = (value: unknown) => typeof value === "number" ? `${value.toFixed(2)} 元` : "证据不足";
  if (modelFailed) return <section className="selection-analysis">
    <p className="batch-alert error"><strong>模型分析失败，不能显示模型结论、方向、目标价或评分。</strong><br />请查看失败原因并重试。以下仅为本地证据盘点，不是模型分析结论，也不可用于确认跟踪。</p>
    {!!analysis.model_failure_reason && <p className="batch-note">失败原因：{String(analysis.model_failure_reason)}</p>}
    {!!analysis.local_multi_source && <div className="analysis-local-inventory"><h3>本地证据盘点</h3><p>系统已保存原始证据和结构化数据，可在“数据事实”“原文与引用”页签核对；模型恢复后重新分析。</p></div>}
  </section>;
  return <section className="selection-analysis">
    {strings(analysis.evidence_validation_warnings).length > 0 && <div className="batch-alert warning"><strong>部分完成 · 引用核验后已撤销部分判断</strong><ul>{strings(analysis.evidence_validation_warnings).map((warning, index) => <li key={index}>{warning}</li>)}</ul><p>相关分项显示为证据不足，综合方向、目标价和置信度已撤销；请核对原文后再人工复选。</p></div>}
    <div className="analysis-verdict-layout"><div className="analysis-verdict"><span className={`analysis-signal ${model.decision === "WATCH" && analysis.analysis_validation_status !== "PARTIAL" ? "watch" : ""}`}>{analysis.analysis_validation_status === "PARTIAL" ? "待核对" : names[model.decision] || "待判断"}</span><h3>{model.headline || "历史分析摘要"}</h3><small>模型综合置信度 {typeof model.confidence === "number" ? `${Math.round(model.confidence * 100)}%` : "—"} · 事件证据置信度 {typeof confidence.final_confidence === "number" ? `${Math.round(confidence.final_confidence * 100)}%` : "—"} · 均不等于收益概率</small>{scoring && <div className="score-summary"><strong>可信综合分 {scoring.trusted_score == null ? "证据不足" : scoring.trusted_score.toFixed(1)}</strong><span>基础分 {scoring.base_score == null ? "--" : scoring.base_score.toFixed(1)} · 覆盖 {Math.round(scoring.coverage * 100)}% · 证据置信 {Math.round(scoring.evidence_confidence * 100)}%</span><small>{String(scoring.profile?.profile_name || "评价方案")} v{String(scoring.profile?.version || "--")} · 分数不是收益概率</small></div>}</div>{scoring?.dimensions?.length ? <ScoreRadar rows={scoring.dimensions} /> : <div className="score-radar-empty"><strong>该历史候选尚无八维评分</strong><p>原证据和原分析不会被覆盖，可按当前已发布方案补算。</p>{onRescore && <button type="button" onClick={onRescore}>生成八维评分</button>}</div>}</div>
    <div className="analysis-metrics"><div><small>分析时间范围</small><strong>{period.start && period.end ? `${period.start} 至 ${period.end}` : "历史批次未记录"}</strong></div><div><small>周期新闻公告</small><strong>{Number(usage.period_disclosures ?? usage.period_documents ?? documents.length)} 篇</strong><small>模型载入 {Number(usage.period_disclosures_included ?? usage.period_documents_included ?? (array(usage.documents).length || documents.length))} 篇</small></div><div><small>事件联动</small><strong>{links.length} 条</strong><small>{findings.length} 条兑现/冲突验证</small></div></div>
    {scoring?.dimensions?.length ? <details className="scorecard-details"><summary>查看八维评分依据、覆盖与权重</summary><div className="scorecard-grid">{scoring.dimensions.map(row => <article key={row.dimension_code} className={row.status === "SUPPORTED" ? "" : "missing"}><div><strong>{row.dimension_name}</strong><span>{row.status === "SUPPORTED" && row.score != null ? `${row.score.toFixed(1)} 分` : "证据不足"}</span></div><p>{row.summary}</p><small>权重 {row.weight}% · 覆盖 {Math.round(row.coverage * 100)}% · 置信 {Math.round(row.evidence_confidence * 100)}% · {row.method}</small>{row.warnings?.map((warning, index) => <em key={index}>{warning}</em>)}</article>)}</div></details> : null}
    {dimensions.length ? <><h3>逐项研判</h3><div className="analysis-dimensions">{dimensions.map(item => <article key={item.dimension} className={`analysis-dimension ${item.status === "INSUFFICIENT" ? "missing" : ""}`}><div className="analysis-card-heading"><h4>{names[item.dimension]}</h4><span className={`analysis-direction ${String(item.direction).toLowerCase()}`}>{item.status === "INSUFFICIENT" ? "证据不足" : names[item.direction]}</span></div><p>{item.summary}</p><div className="analysis-references">{(item.evidence_document_ids || []).map(reference)}</div>{!!item.evidence_fact_ids?.length && <small>{item.evidence_fact_ids.length} 条数值/事实依据</small>}{!!item.evidence_relation_ids?.length && <small>{item.evidence_relation_ids.length} 条图谱关系依据</small>}{item.status === "INSUFFICIENT" && onBackfillDimension && <button type="button" className="analysis-repair" onClick={() => onBackfillDimension(item.dimension as AnalysisDimension)}>补齐{names[item.dimension] || "该板块"}数据</button>}</article>)}</div></> : <><p className="batch-missing">此历史批次尚未生成分项分析；重新分析可获得新闻、公告等七项研判。</p><p>{String(analysis.summary || "")}</p></>}
    {!!events.length && <><h3>新闻与公告如何影响判断</h3><div className="analysis-events">{events.map((item, index) => <article key={index} className="analysis-event"><div className="analysis-card-heading"><h4>{item.title}</h4><span className={`analysis-direction ${String(item.direction).toLowerCase()}`}>{names[item.direction]}</span></div><p>{item.mechanism}</p><small>{item.timing}</small><blockquote>{item.quote}</blockquote>{reference(item.document_id)}</article>)}</div></>}
    {!!timeline.length && <><h3>周期事件时间线</h3><div className="analysis-timeline">{timeline.map((item, index) => <article key={item.event_id || index}><time>{item.event_date}</time><div><strong>{item.topic}</strong><p>{item.claim}</p><small>{names[item.source_type]} · {names[item.direction]} · 置信度 {Math.round(Number(item.confidence || 0) * 100)}%</small><div className="analysis-references">{array(item.document_ids).map(id => reference(Number(id)))}</div></div></article>)}</div></>}
    {!!links.length && <><h3>前后联动与验证</h3><div className="analysis-linkage">{links.map((item, index) => <article key={index}><div className="analysis-card-heading"><h4>{names[item.relation_type] || item.relation_type}</h4><span className={`analysis-direction ${String(item.direction).toLowerCase()}`}>{Math.round(Number(item.confidence || 0) * 100)}%</span></div><p>{item.summary}</p>{strings(item.evidence_quotes).map((quote, quoteIndex) => <blockquote key={quoteIndex}>{quote}</blockquote>)}<div className="analysis-references">{[...array(item.source_document_ids), ...array(item.target_document_ids)].map(id => reference(Number(id)))}</div></article>)}</div></>}
    {array(candidate.evidence_json.related_securities).length > 0 && <><h3>已审核关联证券线索</h3><div className="analysis-related-securities">{array(candidate.evidence_json.related_securities).map((item, index) => <article key={`${item.market}-${item.symbol}-${index}`}><strong>{item.name || item.symbol}</strong><span>{item.market}:{item.symbol}</span><small>关联事实 {array(item.related_via_fact_ids).length} 条；仅用于关联证券影响，不代表当前股票因果结论。</small></article>)}</div><p className="analysis-help">关联证券新闻/公告只有在已审核公司关系存在时才会进入模型上下文；模型必须同时引用当前股票和关联证券原文。</p></>}
    {!!findings.length && <><h3>历史信息兑现情况</h3><div className="analysis-findings">{findings.map((item, index) => <article key={index}><span>{names[item.validation_status] || item.validation_status}</span><strong>{item.claim}</strong><p>{item.explanation}</p><small>{item.validation_window} · 置信度 {Math.round(Number(item.confidence || 0) * 100)}%</small><div className="analysis-references">{array(item.supporting_document_ids).map(id => reference(Number(id)))}</div></article>)}</div></>}
    {typeof confidence.final_confidence === "number" && <details className="analysis-confidence"><summary>事件证据置信度如何计算 · {Math.round(confidence.final_confidence * 100)}%</summary><div>{[["来源可靠性", confidence.source_reliability], ["时间一致性", confidence.temporal_consistency], ["跨文档支持", confidence.cross_document_support], ["结构化事实支持", confidence.structured_fact_support], ["语义抽取置信度", confidence.model_extraction_confidence], ["冲突惩罚", confidence.conflict_penalty]].map(([title, value]) => <p key={String(title)}><span>{String(title)}</span><progress max="1" value={Number(value || 0)} /><strong>{Math.round(Number(value || 0) * 100)}%</strong></p>)}</div><small>来源可靠性与模型判断分开计算；同源转载不增加独立证据，时间相邻不自动构成因果。</small></details>}
    <div className="analysis-checks">{[["支持因素", model.positives], ["主要风险", model.risks], ["接下来观察", model.watch_conditions]].map(([title, value]) => strings(value).length > 0 && <div key={String(title)}><h4>{String(title)}</h4><ul>{strings(value).map((item, index) => <li key={index}>{item}</li>)}</ul></div>)}</div>
    <details className="analysis-input"><summary>本次实际读取与引用</summary><p>已送入模型 {array(usage.documents).length || documents.length} 篇文档 · 模型引用 {model.evidence_document_ids?.length || 0} 篇 · {usage.relations ?? 0} 条图谱关系 · {usage.related_security_documents ?? 0} 篇关联证券资料。引用表示模型使用了该材料，不等于事实已核验。</p>{array(analysis.skill_chain).map(row => <small key={String(row.skill_code)}>Skill {row.skill_code} · {row.skill_version}</small>)}{array(usage.selected_scope?.knowledge_bases).map(row => <small key={row.id}>知识库 #{row.id} · {row.code} · {row.version}</small>)}{array(usage.selected_scope?.graphs).map(row => <small key={row.id}>图谱 #{row.id} · {row.code} · {row.version}</small>)}</details>
  </section>;
}
