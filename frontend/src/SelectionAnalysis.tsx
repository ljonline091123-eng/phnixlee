import type { SelectionCandidate, SelectionRecord, SelectionScorecard, SelectionScoreDimensionResult } from "./selectionApi";
import { SelectionEvidenceCards } from "./SelectionEvidenceCards";

const names: Record<string, string> = {NEWS: "新闻事件", NOTICE: "公告披露", FINANCIAL: "财务业绩", PRICE_VOLUME: "量价趋势", FUND_FLOW: "资金进出", COMPANY: "主营经营、行业分类与股东", GRAPH: "公司关联与图谱", POSITIVE: "偏利好", NEGATIVE: "偏利空", MIXED: "影响交织", NEUTRAL: "中性", UNKNOWN: "待判断", WATCH: "值得关注", PASS: "暂不关注", FOLLOW_UP: "后续进展", VALIDATES: "相互验证", CONTRADICTS: "信息冲突", REALIZES: "已兑现", FAILS_TO_REALIZE: "未兑现", SAME_TOPIC: "同一事项", EXECUTIVE_UNLOCKING_RISK: "高管与解禁线索", SHAREHOLDER_CONFIDENCE_SIGNAL: "股东信心信号", RELATED_SECURITY_IMPACT: "关联证券影响", UNVERIFIED: "待验证", PARTIALLY_REALIZED: "部分兑现", REALIZED: "已兑现", NOT_REALIZED: "未兑现", CONFLICTED: "存在冲突", INSUFFICIENT: "证据不足"};
const relationshipNames: Record<string, string> = {SUPPLIER: "供应商", CUSTOMER: "客户", PARTNER: "合作伙伴", COMPETITOR: "竞争对手", EQUITY_CONTROL: "股权/控制", TECHNOLOGY_SUBSTITUTION: "技术替代", SAME_INDUSTRY: "同行业", SAME_THEME: "同主题", OTHER: "其他关系"};
const horizonNames: Record<string, string> = {SHORT: "短期", MEDIUM: "中期", LONG: "长期", MULTI_PERIOD: "跨周期", UNKNOWN: "周期待判断"};
const technologyNames: Record<string, string> = {UNKNOWN: "证据不足", LOW: "较低", MEDIUM: "中等", HIGH: "较高", LEADING: "领先（有证据）"};
const thesisStatusNames: Record<string, string> = {SUPPORTED: "逻辑成立", PARTIAL: "部分成立", INSUFFICIENT: "证据不足", CONTRADICTED: "存在反证"};
const classificationRoleNames: Record<string, string> = {PRIMARY_INDUSTRY: "核心行业", SECONDARY_INDUSTRY: "次要行业", CORE_THEME: "核心主题", OPTIONAL_THEME: "观察主题", NOISE_TAG: "弱相关标签"};
const assessmentNames: Record<string, string> = {BUSINESS_OUTLOOK: "主营赛道与盈利前景", INDUSTRY_OUTLOOK: "行业与赛道趋势", COMPETITIVE_POSITION: "公司竞争位置", VALUE_CHAIN_RESILIENCE: "产业链与外部韧性", VALUATION: "估值匹配度", INSTITUTION_EXPECTATION: "机构预期", MARKET_AND_CAPITAL: "一年量价与资金状态", GOVERNANCE_AND_SHAREHOLDER: "股东回报与治理信号", SECOND_CURVE: "第二曲线"};
const array = (value: unknown): SelectionRecord[] => Array.isArray(value) ? value : [];
const strings = (value: unknown): string[] => Array.isArray(value) ? value.filter(item => typeof item === "string") : [];
export type AnalysisDimension = "NEWS" | "NOTICE" | "FINANCIAL" | "PRICE_VOLUME" | "FUND_FLOW" | "COMPANY" | "RESEARCH" | "GRAPH";

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

const friendlyDiagnostic = (value: string) => {
  if (/METADATA_ONLY|仅有标题|无正文/i.test(value)) return "部分公告目前只有标题和链接，尚未取得可阅读全文，因此这些公告不能参与内容核验。";
  if (/graph_paths|verified_relation_fact_ids|图谱.*关系|关系.*缺少|公司关系事实/i.test(value)) return "尚无已审核的客户、供应商或合作关系。系统可以分析主营赛道和外部依赖风险，但不能据此确认具体公司间的传导方向；补齐并审核关系事实后再计入产业链评分。";
  if (/embedding|向量/i.test(value)) return "部分文档尚未完成语义向量处理；原文仍可阅读，但相似内容召回可能不完整。";
  if (/period.*document|周期文档|上下文预算/i.test(value)) return "一年分析期内的资料超过单次模型输入容量，系统已按文档分段阅读并汇总，原文仍完整保留。";
  if (/fund.?flow|资金流/i.test(value)) return "当前缺少口径一致的资金流数据，资金判断仅使用可核验的成交量、换手率等交易数据。";
  if (/source_document_ids|target_document_ids|前后顺序|日期顺序/i.test(value)) return "部分前后事件的发生顺序或引用范围未通过核验，系统已撤销这些事件间的联动判断。";
  if (/evidence_quotes|quote|引句|原句/i.test(value)) return "部分联动结论没有找到可逐字核对的原文引句，暂不作为确定性证据。";
  if (/source_type|NEWS|NOTICE|来源类型/i.test(value)) return "部分事件的新闻/公告来源归类不一致，系统已保留原文，但未发布该联动结论。";
  if (/analyzed_document_ids|文档.*覆盖|正文.*覆盖/i.test(value)) return "本轮实际阅读记录与一年期资料清单未完全对应，需要重新执行分段阅读后再核验联动。";
  if (/timeline|时间线|联动/i.test(value)) return "部分事件尚未找到可相互核验的前后文，相关联动只作为待核线索，不计入确定性结论。";
  return value.replace(/_/g, " ");
};

const friendlyValueChainSummary = (value: unknown) => {
  const summary = String(value || "暂无可核验产业链传导");
  if (/verified_relation_fact_ids|graph_paths|已审核.*关系.*为空|关系事实.*为空/i.test(summary)) {
    return "本轮没有已审核的客户、供应商或合作关系事实，因此暂时无法判断具体关联公司事件如何传导；当前只分析主营赛道、客户生态和外部依赖风险。";
  }
  return summary;
};

function EvidenceCoverage({ analysis }: { analysis: SelectionRecord }) {
  const coverage = (analysis.evidence_coverage || {}) as SelectionRecord;
  const acquisition = strings(coverage.acquisition_gaps), sourceFailures = strings(coverage.source_failures);
  const enrichment = strings(coverage.optional_enrichment_gaps), contextLimits = strings(coverage.context_limits);
  const warnings = strings(coverage.quality_warnings);
  const ready = coverage.minimum_analysis_ready === true;
  if (!Object.keys(coverage).length) return null;
  return <section className="analysis-input" aria-label="数据覆盖与分析输入状态">
    <div className="analysis-card-heading"><div><strong>数据覆盖与分析输入</strong><small>{ready ? "最低分析门槛已通过，不等于所有数据源均完整" : "未达到最低分析门槛"}</small></div><span className={`batch-badge ${ready ? "" : "warning"}`}>{ready ? "可进入分析" : "需要补数"}</span></div>
    {Number(coverage.period_document_count || 0) > 0 && <p>周期内发现 {Number(coverage.period_document_count)} 篇文档，本轮模型输入 {Number(coverage.included_document_count || 0)} 篇。上下文预算外资料仍保存在数据库，不属于采集失败。</p>}
    {coverage.model_validation_error ? <p className="batch-alert error"><strong>本轮失败环节：模型输出与证据引用校验</strong><br />{friendlyDiagnostic(String(coverage.model_validation_error))}</p> : null}
    {(acquisition.length > 0 || sourceFailures.length > 0) && <details open><summary>需要补采或重试来源（{acquisition.length + sourceFailures.length}）</summary><ul>{[...acquisition, ...sourceFailures].map((item, index) => <li key={index}>{friendlyDiagnostic(item)}</li>)}</ul></details>}
    {contextLimits.length > 0 && <details><summary>模型输入容量处理（{contextLimits.length}）</summary><ul>{contextLimits.map((item, index) => <li key={index}>{friendlyDiagnostic(item)}</li>)}</ul></details>}
    {enrichment.length > 0 && <details><summary>尚未覆盖的增强数据（{enrichment.length}）</summary><ul>{enrichment.map((item, index) => <li key={index}>{friendlyDiagnostic(item)}</li>)}</ul><p>这些项目会影响对应维度覆盖率，但在最低门槛已通过时，不代表数据同步任务失败。</p></details>}
    {warnings.length > 0 && <details><summary>数据质量提醒（{warnings.length}）</summary><ul>{warnings.map((item, index) => <li key={index}>{friendlyDiagnostic(item)}</li>)}</ul></details>}
  </section>;
}

export function SelectionDiagnostics({ candidate }: { candidate: SelectionCandidate }) {
  const analysis = candidate.analysis_json;
  const model = (analysis.model || {}) as SelectionRecord;
  const publication = (analysis.publication || {}) as SelectionRecord;
  const inputUsage = (analysis.input_usage || {}) as SelectionRecord;
  const llmEvaluation = (analysis.llm_evaluation || {}) as SelectionRecord;
  const period = (candidate.evidence_json.analysis_period || inputUsage.analysis_period || {}) as SelectionRecord;
  const usage = (analysis.input_usage || {}) as SelectionRecord;
  return <section className="selection-diagnostics" aria-label="分析诊断与数据覆盖">
    <h3>分析诊断与数据覆盖</h3>
    <p className="batch-note">分析范围：{period.start && period.end ? `${period.start} 至 ${period.end}` : "历史批次未记录"} · 周期资料 {Number(usage.period_disclosures ?? usage.period_documents ?? 0)} 篇 · 模型载入 {Number(usage.period_disclosures_included ?? usage.period_documents_included ?? 0)} 篇</p>
    <EvidenceCoverage analysis={analysis} />
    {analysis.model_output_status === "FAILED" && <p className="batch-alert error"><strong>模型分析未通过</strong><br />{friendlyDiagnostic(String(analysis.model_failure_reason || "模型服务没有返回可校验结果"))}</p>}
    {["CONCLUSION_FAILED", "SCORE_FAILED"].includes(String(publication.status)) && <p className="batch-alert error"><strong>{publication.status === "CONCLUSION_FAILED" ? "结论生成未通过" : "评分生成未通过"}</strong><br />{friendlyDiagnostic(String(publication.reason || "请重新分析"))}</p>}
    {strings(analysis.evidence_validation_warnings).length > 0 && <details open><summary>证据引用核验提醒（{strings(analysis.evidence_validation_warnings).length}）</summary><ul>{strings(analysis.evidence_validation_warnings).map((item, index) => <li key={index}>{friendlyDiagnostic(item)}</li>)}</ul></details>}
    {model.semantic_linkage_status === "PARTIAL" && <details open><summary>事件前后联动尚未完全核验</summary><ul>{strings(model.semantic_linkage_warnings).map((item, index) => <li key={index}>{friendlyDiagnostic(item)}</li>)}</ul><p>已通过原文核验的单项分析仍然有效；未核验联动不会参与确定性结论。</p></details>}
    {llmEvaluation.status === "FAILED" && <details><summary>附加 LLM 文字评价未生成</summary><p>{friendlyDiagnostic(String(llmEvaluation.message || "模型服务本次未返回有效评价"))}</p><p>核心投资结论、证据核验和八维评分已经独立完成，不受本项失败影响；可在“LLM评价”页签单独重试。</p></details>}
  </section>;
}

export function SelectionAnalysis({candidate, onDocument, onBackfillDimension, onBackfillAll, backfillDisabled, onRescore}: {candidate: SelectionCandidate; onDocument?: (id: number) => void; onBackfillDimension?: (dimension: AnalysisDimension) => void; onBackfillAll?: () => void; backfillDisabled?: boolean; onRescore?: () => void}) {
  const analysis = candidate.analysis_json;
  const model = (analysis.model || {}) as SelectionRecord;
  const modelFailed = analysis.model_output_status === "FAILED";
  const dataBlocked = analysis.model_output_status === "NOT_RUN" || analysis.data_gate_status === "MISSING_DATA";
  const validationPartial = analysis.analysis_validation_status === "PARTIAL";
  const publication = (analysis.publication || {}) as SelectionRecord;
  const sections = array(analysis.evidence_sections);
  const usage = (analysis.input_usage || {}) as SelectionRecord;
  const dimensions = array(model.dimensions), events = array(model.events);
  const timeline = array(model.event_timeline), links = array(model.event_links), findings = array(model.validation_findings);
  const confidence = (model.confidence_breakdown || {}) as SelectionRecord;
  const business = (model.business_quality || null) as SelectionRecord | null;
  const valueChain = (model.value_chain_analysis || null) as SelectionRecord | null;
  const thesis = (model.investment_thesis || null) as SelectionRecord | null;
  const classifications = array(thesis?.classifications);
  const hypotheses = array(thesis?.hypotheses);
  const deepAssessments = array(thesis?.deep_assessments);
  const period = (candidate.evidence_json.analysis_period || usage.analysis_period || {}) as SelectionRecord;
  const scoring = (analysis.scoring || null) as SelectionScorecard | null;
  const documents = [...new Map([
    ...array(candidate.evidence_json.documents),
    ...array(candidate.evidence_json.related_security_documents),
    ...array(candidate.evidence_json.evidence_context?.documents),
    ...array(candidate.evidence_json.evidence_context?.related_security_documents),
  ].map(row => [String(row.id), row])).values()];
  const document = (id: number) => documents.find(item => Number(item.id) === Number(id));
  const reference = (id: number) => {
    const row = document(id);
    const scope = row?.relation_scope === "RELATED_SECURITY" ? `关联 ${row.symbol || "证券"} · ` : "";
    return <button type="button" className="analysis-reference" key={id} title={row?.title || `文档${id}`} onClick={() => onDocument?.(id)}>{scope}{row?.source_table === "stock_news" ? "新闻" : row?.source_table === "stock_notice" ? "公告" : "证据"} · {row?.title || `#${id}`}</button>;
  };
  const money = (value: unknown) => typeof value === "number" ? `${value.toFixed(2)} 元` : "证据不足";
  const largeMoney = (value: unknown) => typeof value === "number" ? `${(value / 100000000).toFixed(2)} 亿元` : "—";
  const ratio = (value: unknown) => typeof value === "number" ? `${(value * 100).toFixed(2)}%` : "—";
  if (dataBlocked) return <section className="selection-analysis">
    <div className="analysis-empty-conclusion"><strong>尚未形成可用投资结论</strong><p>本轮没有达到最低证据门槛。具体缺失项和补齐入口请查看“数据事实”页签。</p></div>
  </section>;
  if (modelFailed) return <section className="selection-analysis">
    <div className="analysis-empty-conclusion"><strong>本轮模型未形成可校验结论</strong><p>本地证据仍然保留。失败原因、模型调用和处理建议请查看“数据事实”页签。</p></div>
  </section>;
  return <section className="selection-analysis">
    <div className="analysis-verdict-layout"><div className="analysis-verdict"><span className={`analysis-signal ${publication.conclusion_available && model.decision === "WATCH" ? "watch" : ""}`}>{!publication.conclusion_available ? "未形成有效结论" : names[model.decision] || "待判断"}</span><h3>{model.headline || "历史分析摘要"}</h3><small>模型综合置信度 {typeof model.confidence === "number" ? `${Math.round(model.confidence * 100)}%` : "—"} · 事件证据置信度 {typeof confidence.final_confidence === "number" ? `${Math.round(confidence.final_confidence * 100)}%` : "—"} · 均不等于收益概率</small>{publication.scoring_available && scoring && <div className="score-summary"><strong>可信综合分 {scoring.trusted_score == null ? "证据不足" : scoring.trusted_score.toFixed(1)}</strong><span>基础分 {scoring.base_score == null ? "--" : scoring.base_score.toFixed(1)} · 覆盖 {Math.round(scoring.coverage * 100)}% · 证据置信 {Math.round(scoring.evidence_confidence * 100)}%</span><small>{String(scoring.profile?.profile_name || "评价方案")} v{String(scoring.profile?.version || "--")} · 分数不是收益概率</small></div>}</div>{validationPartial ? <div className="score-radar-empty"><strong>引用尚未通过，不生成有效评分</strong><p>保留初步研判用于核对，但不会将其计入有效结论或跟踪决策。</p></div> : publication.scoring_available && scoring?.dimensions?.length ? <ScoreRadar rows={scoring.dimensions} /> : <div className="score-radar-empty"><strong>尚未生成有效雷达图</strong><p>{publication.status === "SCORE_FAILED" ? "结论完整性已通过，可按当前已发布方案补算评分。" : "请先重新分析并通过结论校验，再生成评分。"}</p>{publication.status === "SCORE_FAILED" && onRescore && <button type="button" disabled={backfillDisabled} onClick={onRescore}>生成八维评分</button>}</div>}</div>
    {thesis ? <section className={`analysis-thesis ${String(thesis.status || "INSUFFICIENT").toLowerCase()}`}>
      <div className="analysis-thesis-heading"><div><span>基本面主线</span><h3>整体投资逻辑</h3><p>先判断公司如何赚钱，再用行业、公司行为、产业链、估值预期和交易状态交叉验证。</p></div><div><strong>{thesisStatusNames[String(thesis.status)] || "待核验"}</strong><small>置信度 {Math.round(Number(thesis.confidence || 0) * 100)}%</small></div></div>
      <div className="analysis-thesis-core"><article><small>公司靠什么赚钱</small><strong>{String(thesis.primary_business || "主营证据不足")}</strong><p>{String(thesis.earning_engine_summary || "尚未识别收入和利润发动机")}</p></article><article><small>盈利质量</small><strong>收入与利润是否可持续</strong><p>{String(thesis.profit_quality_summary || "盈利质量证据不足")}</p></article></div>
      {(thesis.primary_track_outlook || thesis.company_position_summary || thesis.second_curve_summary) && <div className="analysis-thesis-narrative">
        <article><small>第一步 · 主营赛道</small><strong>未来发展趋势</strong><p>{String(thesis.primary_track_outlook || "主营赛道趋势证据不足")}</p></article>
        <article><small>第二步 · 公司位置</small><strong>竞争力与收入承接能力</strong><p>{String(thesis.company_position_summary || "公司竞争位置证据不足")}</p></article>
        <article><small>第三步 · 第二曲线</small><strong>增量业务是否能够兑现</strong><p>{String(thesis.second_curve_summary || "尚未识别可验证的第二曲线")}</p></article>
      </div>}
      {classifications.length > 0 && <div className="analysis-classifications">{classifications.filter(item => item.role !== "NOISE_TAG").map((item, index) => <span key={`${String(item.role)}-${String(item.name)}-${index}`} className={`role-${String(item.role || "OPTIONAL_THEME").toLowerCase()}`}><small>{classificationRoleNames[String(item.role)] || "业务分类"}</small><strong>{String(item.name || "未命名")}</strong><em>相关度 {Math.round(Number(item.relevance_score || 0) * 100)}%</em><i>{String(item.rationale || "")}</i></span>)}</div>}
      <div className="analysis-thesis-chain" aria-label="整体分析逻辑链">
        <article><small>行业与产业链</small><p>{String(thesis.industry_chain_summary || "证据不足")}</p></article>
        <article><small>估值</small><p>{String(thesis.valuation_summary || "证据不足")}</p></article>
        <article><small>机构预期</small><p>{String(thesis.institution_view_summary || "证据不足")}</p></article>
        <article><small>量价与资金状态</small><p>{String(thesis.market_timing_summary || "证据不足")}</p></article>
      </div>
      {deepAssessments.length > 0 && <div className="analysis-deep-assessments"><h4>一年期深度判断</h4><div>{deepAssessments.map((item, index) => <article key={`${String(item.assessment_code)}-${index}`}>
        <header><strong>{assessmentNames[String(item.assessment_code)] || String(item.assessment_code || "分析维度")}</strong><span className={`analysis-direction ${String(item.direction || "UNKNOWN").toLowerCase()}`}>{names[String(item.direction)] || "待判断"}</span></header>
        <p>{String(item.conclusion || "证据不足")}</p>
        <small>{horizonNames[String(item.time_horizon)] || "周期待判断"} · 置信度 {Math.round(Number(item.confidence || 0) * 100)}%</small>
        {strings(item.supporting_points).length > 0 && <ul>{strings(item.supporting_points).map((point, pointIndex) => <li key={pointIndex}>{point}</li>)}</ul>}
        {(strings(item.risks).length > 0 || strings(item.watch_conditions).length > 0) && <details><summary>风险与后续验证</summary>{strings(item.risks).length > 0 && <p>风险：{strings(item.risks).join("；")}</p>}{strings(item.watch_conditions).length > 0 && <p>观察：{strings(item.watch_conditions).join("；")}</p>}</details>}
        {array(item.evidence_document_ids).length > 0 && <div className="analysis-references">{array(item.evidence_document_ids).map(id => reference(Number(id)))}</div>}
      </article>)}</div></div>}
      <div className="analysis-thesis-conclusion"><small>综合判断</small><p>{String(thesis.overall_logic || "尚未形成可验证的整体逻辑")}</p></div>
      {hypotheses.length > 0 ? <div className="analysis-hypotheses"><h4>需要持续验证的投资假设</h4>{hypotheses.map((item, index) => <article key={`${String(item.title)}-${index}`}>
        <header><span>{index + 1}</span><div><strong>{String(item.title || "未命名假设")}</strong><small>{horizonNames[String(item.horizon)] || "周期待判断"} · 置信度 {Math.round(Number(item.confidence || 0) * 100)}%</small></div><b className={`analysis-direction ${String(item.direction || "UNKNOWN").toLowerCase()}`}>{thesisStatusNames[String(item.status)] || names[String(item.direction)] || "待判断"}</b></header>
        <p>{String(item.thesis || "")}</p>
        <div className="analysis-hypothesis-evidence"><div><small>支持证据</small><strong>{array(item.supporting_document_ids).length} 篇原文 · {array(item.supporting_fact_ids).length} 条事实 · {array(item.supporting_relation_ids).length} 条关系</strong>{array(item.supporting_document_ids).length > 0 && <div className="analysis-references">{array(item.supporting_document_ids).map(id => reference(Number(id)))}</div>}</div><div className={array(item.counter_document_ids).length || array(item.counter_fact_ids).length || array(item.counter_relation_ids).length ? "has-counter" : ""}><small>反证与不确定性</small><strong>{array(item.counter_document_ids).length} 篇原文 · {array(item.counter_fact_ids).length} 条事实 · {array(item.counter_relation_ids).length} 条关系</strong>{array(item.counter_document_ids).length > 0 && <div className="analysis-references">{array(item.counter_document_ids).map(id => reference(Number(id)))}</div>}</div></div>
        {array(item.evidence_items).length > 0 && <div className="analysis-hypothesis-claims">{array(item.evidence_items).map((evidenceItem, evidenceIndex) => <article key={`${String(evidenceItem.reference_type)}-${String(evidenceItem.reference_id)}-${evidenceIndex}`} className={`stance-${String(evidenceItem.stance || "MIXED").toLowerCase()}`}>
          <header><strong>{evidenceItem.stance === "SUPPORT" ? "支持" : evidenceItem.stance === "COUNTER" ? "反证" : "正负交织"} · {String(evidenceItem.claim || "证据事实")}</strong><small>{String(evidenceItem.reference_type)} #{String(evidenceItem.reference_id)}</small></header>
          <p>{String(evidenceItem.company_specific_effect || "尚未说明对本公司的具体影响")}</p>
          {evidenceItem.quote && <blockquote>{String(evidenceItem.quote)}</blockquote>}
          <small>{String(evidenceItem.rationale || "")}</small>
        </article>)}</div>}
        <footer><div><small>逻辑失效条件</small><p>{strings(item.invalidation_conditions).join("；") || "尚未明确"}</p></div><div><small>后续观察</small><p>{strings(item.watch_conditions).join("；") || "尚未明确"}</p></div></footer>
      </article>)}</div> : <p className="batch-missing">整体证据尚不足以形成可验证的投资假设，不能用零值或孤立标签替代结论。</p>}
    </section> : <div className="analysis-legacy-notice"><strong>旧版分析结果</strong><p>该历史记录生成时尚未启用“一年期主营赛道 → 公司位置 → 收入利润 → 第二曲线 → 治理 → 估值预期 → 量价资金”的 V7 整体逻辑契约。以下保留原分析及证据，不改写历史结论；主动重新分析后才会生成新版整体逻辑。</p></div>}
    <details className="analysis-deep-dive" open={!thesis}>
      <summary><span><strong>详细分析与证据</strong><small>主营明细、产业链传导、八维评分、新闻公告联动、图谱关系和模型引用</small></span><b>{thesis ? "展开核对" : "旧版结果已展开"}</b></summary>
    <section className={`analysis-business-focus ${business?.status === "SUPPORTED" ? "supported" : "insufficient"}`}>
      <div className="analysis-focus-heading"><div><span>中长线重点</span><h3>主营业务与竞争壁垒</h3></div>{business ? <div className={`analysis-direction ${String(business.direction || "UNKNOWN").toLowerCase()}`}><strong>{names[String(business.direction)] || "待判断"}</strong><small>{typeof business.impact_score === "number" ? `${business.impact_score > 0 ? "+" : ""}${business.impact_score.toFixed(0)} 分` : "影响分待核"}</small></div> : <span className="analysis-evidence-state">历史结果未分析</span>}</div>
      {business ? <>
        <p className="analysis-focus-summary">{String(business.main_business_summary || "主营业务证据不足")}</p>
        {strings(business.products_services).length > 0 && <div className="analysis-product-tags">{strings(business.products_services).map((item, index) => <span key={`${item}-${index}`}>{item}</span>)}</div>}
        {array(business.revenue_mix).length > 0 && <div className="analysis-business-table-wrap"><table className="analysis-business-table"><thead><tr><th>主营产品/服务</th><th>报告期</th><th>收入</th><th>收入占比</th><th>毛利率</th></tr></thead><tbody>{array(business.revenue_mix).map((item, index) => <tr key={`${String(item.evidence_fact_id)}-${index}`}><td>{String(item.name || "—")}</td><td>{String(item.report_date || "—")}</td><td>{largeMoney(item.revenue)}</td><td>{ratio(item.revenue_ratio)}</td><td>{ratio(item.gross_margin)}</td></tr>)}</tbody></table></div>}
        <div className="analysis-business-grid">
          <article><small>技术含量</small><strong>{technologyNames[String(business.technology_level)] || "证据不足"}</strong><p>{String(business.technology_assessment || "缺少可核验技术资料")}</p></article>
          <article><small>竞争壁垒</small><strong>可持续性研判</strong><p>{String(business.competitive_moat || "证据不足")}</p></article>
          <article><small>商业化能力</small><strong>客户与收入验证</strong><p>{String(business.commercialization || "证据不足")}</p></article>
          <article><small>市场地位</small><strong>{business.market_share?.verification_status === "SOURCE_VERIFIED" ? `${business.market_share.value}${business.market_share.unit || ""}` : "市场份额未核实"}</strong><p>{String(business.market_position || business.market_share?.explanation || "证据不足")}</p>{business.market_share?.verification_status === "SOURCE_VERIFIED" && <em>{String(business.market_share.scope)} · {String(business.market_share.as_of)} · {String(business.market_share.source)}</em>}</article>
        </div>
        <div className="analysis-focus-foot"><span>证据置信度 {Math.round(Number(business.confidence || 0) * 100)}%</span><span>主营事实 {array(business.evidence_fact_ids).length} 条</span><small>技术领先、国产替代、客户关系和市场占有率只有通过来源核验才展示为事实。</small></div>
        {array(business.evidence_document_ids).length > 0 && <div className="analysis-references">{array(business.evidence_document_ids).map(id => reference(Number(id)))}</div>}
      </> : <p className="batch-missing">该历史结果尚未生成主营与竞争壁垒专项分析；重新分析后将按 V7 证据契约生成。</p>}
    </section>
    <section className={`analysis-value-chain ${valueChain?.status === "SUPPORTED" ? "supported" : "insufficient"}`}>
      <div className="analysis-focus-heading"><div><span>关系传导</span><h3>产业链关联公司重大事件影响</h3></div><small>影响分表示研究影响强弱，不是目标涨跌幅</small></div>
      {valueChain ? <><p className="analysis-focus-summary">{friendlyValueChainSummary(valueChain.summary)}</p>{array(valueChain.impacts).length > 0 ? <div className="analysis-chain-list">{array(valueChain.impacts).map((item, index) => <article key={`${String(item.related_symbol || item.related_company)}-${index}`} className={`relation-${String(item.relation_status || "UNVERIFIED").toLowerCase()}`}>
        <div className="analysis-card-heading"><div><strong>{String(item.related_company || "关联对象")}</strong><small>{[item.related_market, item.related_symbol].filter(Boolean).join(":") || "非上市关联对象"} · {relationshipNames[String(item.relationship_type)] || String(item.relationship_type || "关系待核")}</small></div><span className={`relation-status ${String(item.relation_status || "UNVERIFIED").toLowerCase()}`}>{item.relation_status === "VERIFIED" ? "关系已审核" : item.relation_status === "CANDIDATE" ? "候选关系" : "关系未核实"}</span></div>
        <h4>{String(item.event_summary || "关联事件待补充")}</h4><p>{String(item.transmission_mechanism || "传导机制证据不足")}</p>
        <div className="analysis-chain-metrics"><span className={`analysis-direction ${String(item.direction || "UNKNOWN").toLowerCase()}`}>{names[String(item.direction)] || "待判断"}</span><strong>{typeof item.impact_score === "number" ? `${item.impact_score > 0 ? "+" : ""}${item.impact_score.toFixed(0)} 分` : "不计分"}</strong><span>{horizonNames[String(item.horizon)] || "周期待判断"}</span><span>置信度 {Math.round(Number(item.confidence || 0) * 100)}%</span></div>
        {strings(item.assumptions).length > 0 && <details><summary>关键假设与失效条件</summary><p>假设：{strings(item.assumptions).join("；")}</p><p>失效：{strings(item.invalidation_conditions).join("；") || "尚未明确"}</p></details>}
        <div className="analysis-references">{[...array(item.current_security_document_ids), ...array(item.related_security_document_ids)].map(id => reference(Number(id)))}</div>
      </article>)}</div> : <p className="batch-missing">没有同时满足“已审核公司关系 + 关联对象重大事件原文”的传导链，不生成方向和影响分。</p>}</> : <p className="batch-missing">该历史结果尚未执行产业链事件传导专项分析；重新分析后生成。</p>}
    </section>
    {publication.scoring_available && scoring?.dimensions?.length ? <details className="scorecard-details"><summary>查看八维评分依据、覆盖与权重</summary><div className="scorecard-grid">{scoring.dimensions.map(row => <article key={row.dimension_code} className={row.status === "SUPPORTED" ? "" : "missing"}><div><strong>{row.dimension_name}</strong><span>{row.status === "SUPPORTED" && row.score != null ? `${row.score.toFixed(1)} 分` : "证据不足"}</span></div><p>{friendlyDiagnostic(row.summary)}</p><small>权重 {row.weight}% · 覆盖 {Math.round(row.coverage * 100)}% · 置信 {Math.round(row.evidence_confidence * 100)}% · {row.method}</small>{row.warnings?.map((warning, index) => <em key={index}>{friendlyDiagnostic(warning)}</em>)}</article>)}</div></details> : null}
    {sections.length ? <SelectionEvidenceCards sections={sections} onDocument={onDocument} disabled={backfillDisabled} onBackfill={onBackfillDimension ? dimension => onBackfillDimension(dimension as AnalysisDimension) : undefined} /> : <><p className="batch-missing">{dimensions.length ? "历史分项缺少可核验的证据分类，请重新分析。" : "此历史批次尚未生成分项分析。"}</p><p>{String(analysis.summary || "")}</p></>}
    {!!events.length && <><h3>新闻与公告如何影响判断</h3><div className="analysis-events">{events.map((item, index) => <article key={index} className="analysis-event"><div className="analysis-card-heading"><h4>{item.title}</h4><span className={`analysis-direction ${String(item.direction).toLowerCase()}`}>{names[item.direction]}</span></div><p>{item.mechanism}</p><small>{item.timing}</small><blockquote>{item.quote}</blockquote>{reference(item.document_id)}</article>)}</div></>}
    {!!timeline.length && <><h3>周期事件时间线</h3><div className="analysis-timeline">{timeline.map((item, index) => <article key={item.event_id || index}><time>{item.event_date}</time><div><strong>{item.topic}</strong><p>{item.claim}</p><small>{names[item.source_type]} · {names[item.direction]} · 置信度 {Math.round(Number(item.confidence || 0) * 100)}%</small><div className="analysis-references">{array(item.document_ids).map(id => reference(Number(id)))}</div></div></article>)}</div></>}
    {!!links.length && <><h3>前后联动与验证</h3><div className="analysis-linkage">{links.map((item, index) => <article key={index}><div className="analysis-card-heading"><h4>{names[item.relation_type] || item.relation_type}</h4><span className={`analysis-direction ${String(item.direction).toLowerCase()}`}>{Math.round(Number(item.confidence || 0) * 100)}%</span></div><p>{item.summary}</p>{strings(item.evidence_quotes).map((quote, quoteIndex) => <blockquote key={quoteIndex}>{quote}</blockquote>)}<div className="analysis-references">{[...array(item.source_document_ids), ...array(item.target_document_ids)].map(id => reference(Number(id)))}</div></article>)}</div></>}
    {array(candidate.evidence_json.related_securities).length > 0 && <><h3>已审核关联证券线索</h3><div className="analysis-related-securities">{array(candidate.evidence_json.related_securities).map((item, index) => <article key={`${item.market}-${item.symbol}-${index}`}><strong>{item.name || item.symbol}</strong><span>{item.market}:{item.symbol}</span><small>关联事实 {array(item.related_via_fact_ids).length} 条；仅用于关联证券影响，不代表当前股票因果结论。</small></article>)}</div><p className="analysis-help">关联证券新闻/公告只有在已审核公司关系存在时才会进入模型上下文；模型必须同时引用当前股票和关联证券原文。</p></>}
    {!!findings.length && <><h3>历史信息兑现情况</h3><div className="analysis-findings">{findings.map((item, index) => <article key={index}><span>{names[item.validation_status] || item.validation_status}</span><strong>{item.claim}</strong><p>{item.explanation}</p><small>{item.validation_window} · 置信度 {Math.round(Number(item.confidence || 0) * 100)}%</small><div className="analysis-references">{array(item.supporting_document_ids).map(id => reference(Number(id)))}</div></article>)}</div></>}
    {typeof confidence.final_confidence === "number" && <details className="analysis-confidence"><summary>事件证据置信度如何计算 · {Math.round(confidence.final_confidence * 100)}%</summary><div>{[["来源可靠性", confidence.source_reliability], ["时间一致性", confidence.temporal_consistency], ["跨文档支持", confidence.cross_document_support], ["结构化事实支持", confidence.structured_fact_support], ["语义抽取置信度", confidence.model_extraction_confidence], ["冲突惩罚", confidence.conflict_penalty]].map(([title, value]) => <p key={String(title)}><span>{String(title)}</span><progress max="1" value={Number(value || 0)} /><strong>{Math.round(Number(value || 0) * 100)}%</strong></p>)}</div><small>来源可靠性与模型判断分开计算；同源转载不增加独立证据，时间相邻不自动构成因果。</small></details>}
    <div className="analysis-checks">{[["支持因素", model.positives], ["主要风险", model.risks], ["接下来观察", model.watch_conditions]].map(([title, value]) => strings(value).length > 0 && <div key={String(title)}><h4>{String(title)}</h4><ul>{strings(value).map((item, index) => <li key={index}>{item}</li>)}</ul></div>)}</div>
    <details className="analysis-input"><summary>本次实际读取与引用</summary><p>已送入模型 {array(usage.documents).length || documents.length} 篇文档 · 模型引用 {model.evidence_document_ids?.length || 0} 篇 · {usage.relations ?? 0} 条图谱关系 · {usage.related_security_documents ?? 0} 篇关联证券资料。引用表示模型使用了该材料，不等于事实已核验。</p>{array(analysis.skill_chain).map(row => <small key={String(row.skill_code)}>Skill {row.skill_code} · {row.skill_version}</small>)}{array(usage.selected_scope?.knowledge_bases).map(row => <small key={row.id}>知识库 #{row.id} · {row.code} · {row.version}</small>)}{array(usage.selected_scope?.graphs).map(row => <small key={row.id}>图谱 #{row.id} · {row.code} · {row.version}</small>)}</details>
    </details>
  </section>;
}
