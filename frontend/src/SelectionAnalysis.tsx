import { useEffect, useState } from "react";
import { selectionApi } from "./selectionApi";
import type {
  SelectionAiResearchConfirmRequest, SelectionCandidate, SelectionHypothesisDirectionRequest, SelectionRecord,
  SelectionResearchLearning, SelectionResearchReviewRequest,
  SelectionScorecard, SelectionScoreDimensionResult,
} from "./selectionApi";
import { SelectionEvidenceCards } from "./SelectionEvidenceCards";

const names: Record<string, string> = {NEWS: "新闻事件", NOTICE: "公告披露", FINANCIAL: "财务业绩", PRICE_VOLUME: "量价趋势", FUND_FLOW: "资金进出", COMPANY: "主营经营、行业分类与股东", GRAPH: "公司关联与图谱", POSITIVE: "偏利好", NEGATIVE: "偏利空", MIXED: "影响交织", NEUTRAL: "中性", UNKNOWN: "待判断", WATCH: "值得关注", PASS: "暂不关注", FOLLOW_UP: "后续进展", VALIDATES: "相互验证", CONTRADICTS: "信息冲突", REALIZES: "已兑现", FAILS_TO_REALIZE: "未兑现", SAME_TOPIC: "同一事项", EXECUTIVE_UNLOCKING_RISK: "高管与解禁线索", SHAREHOLDER_CONFIDENCE_SIGNAL: "股东信心信号", RELATED_SECURITY_IMPACT: "关联证券影响", UNVERIFIED: "待验证", PARTIALLY_REALIZED: "部分兑现", REALIZED: "已兑现", NOT_REALIZED: "未兑现", CONFLICTED: "存在冲突", INSUFFICIENT: "证据不足"};
const relationshipNames: Record<string, string> = {SUPPLIER: "供应商", CUSTOMER: "客户", PARTNER: "合作伙伴", COMPETITOR: "竞争对手", EQUITY_CONTROL: "股权/控制", TECHNOLOGY_SUBSTITUTION: "技术替代", SAME_INDUSTRY: "同行业", SAME_THEME: "同主题", OTHER: "其他关系"};
const horizonNames: Record<string, string> = {SHORT: "短期", MEDIUM: "中期", LONG: "长期", MULTI_PERIOD: "跨周期", UNKNOWN: "周期待判断"};
const technologyNames: Record<string, string> = {UNKNOWN: "证据不足", LOW: "较低", MEDIUM: "中等", HIGH: "较高", LEADING: "领先（有证据）"};
const thesisStatusNames: Record<string, string> = {SUPPORTED: "逻辑成立", PARTIAL: "部分成立", INSUFFICIENT: "证据不足", CONTRADICTED: "存在反证"};
const classificationRoleNames: Record<string, string> = {PRIMARY_INDUSTRY: "核心行业", SECONDARY_INDUSTRY: "次要行业", CORE_THEME: "核心主题", OPTIONAL_THEME: "观察主题", NOISE_TAG: "弱相关标签"};
const assessmentNames: Record<string, string> = {BUSINESS_OUTLOOK: "主营赛道与盈利前景", INDUSTRY_OUTLOOK: "行业与赛道趋势", COMPETITIVE_POSITION: "公司竞争位置", VALUE_CHAIN_RESILIENCE: "产业链与外部韧性", VALUATION: "估值匹配度", INSTITUTION_EXPECTATION: "机构预期", MARKET_AND_CAPITAL: "一年量价与资金状态", GOVERNANCE_AND_SHAREHOLDER: "股东回报与治理信号", SECOND_CURVE: "第二曲线"};
const researchNames: Record<string, string> = {BUSINESS_MODEL: "公司赚钱逻辑", MANAGEMENT_CHANGE: "重大管理层变化", ACQUISITION_SECOND_CURVE: "并购与第二曲线", CAPITAL_ACTION_INTENT: "回购、注销与锁定行为"};
const researchStatusNames: Record<string, string> = {COMPLETE: "已完成", PARTIAL: "部分完成", INSUFFICIENT: "证据不足", NOT_APPLICABLE: "不适用"};
const researchResolutionNames: Record<string, string> = {LOCAL_SOURCE_PENDING: "等待本地数据补齐", EXTERNAL_RESEARCH_REQUIRED: "需要外部补证", USER_INPUT_REQUIRED: "需要用户补充", UNAVAILABLE: "当前不可得"};
const researchLoopNames: Record<string, string> = {WAITING_USER_DIRECTION: "等待确认研究方向", READY_FOR_RESEARCH: "方向已确认，等待执行", WAITING_EVIDENCE_REVIEW: "证据冲突待复核", WAITING_CONCLUSION_REVIEW: "阶段结论待确认", USER_CONFIRMED: "用户已确认", STOPPED_BY_USER: "用户已停止", QUEUED: "等待执行", RUNNING: "正在补数验证", COMPLETED: "已完成", SOURCE_EXHAUSTED: "现有来源已查无新增证据", MAX_ROUNDS: "达到本轮验证上限", SOURCE_FAILED: "数据源调用失败，可重试", MODEL_FAILED: "模型重验失败，可重试"};
const researchRoundNames: Record<string, string> = {VERIFIED_WITH_INCREMENT: "发现新增证据并完成重验", NO_NEW_EVIDENCE: "来源查询成功，但未发现新增证据", SOURCE_FAILED: "数据源调用失败，本轮不降权", MODEL_FAILED: "模型重验失败，本轮不降权"};
const array = (value: unknown): SelectionRecord[] => Array.isArray(value) ? value : [];
const strings = (value: unknown): string[] => Array.isArray(value) ? value.filter(item => typeof item === "string") : [];
export type AnalysisDimension = "NEWS" | "NOTICE" | "FINANCIAL" | "PRICE_VOLUME" | "FUND_FLOW" | "COMPANY" | "RESEARCH" | "GRAPH";

const aiResearchStatusNames: Record<string, string> = {
  NOT_STARTED: "尚未启动", WAITING_USER_DIRECTION: "等待用户确认方向", MODEL_FAILED: "模型调用失败",
  READY_FOR_EVIDENCE_COLLECTION: "方向已确认，等待补证", QUEUED: "已排队，等待补证",
  RUNNING: "正在补证与AI复核", WAITING_CONCLUSION_REVIEW: "阶段结论待确认",
  COMPLETED: "本轮研究已确认", DIRECTION_DRAFT_SAVED: "已保存方向草稿",
  SOURCE_FAILED: "数据源补证失败", STOPPED_BY_USER: "用户已停止",
};

function AiDirectedResearchPanel({candidate, disabled, onUpdated}: {
  candidate: SelectionCandidate;
  disabled?: boolean;
  onUpdated?: (candidate: SelectionCandidate) => void;
}) {
  const initial = (candidate.analysis_json.ai_directed_research || {}) as SelectionRecord;
  const [state, setState] = useState<SelectionRecord>(initial);
  const [selected, setSelected] = useState<string[]>(Array.isArray(initial.selected_direction_ids) ? initial.selected_direction_ids.map(String) : []);
  const [comment, setComment] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => {
    const next = (candidate.analysis_json.ai_directed_research || {}) as SelectionRecord;
    setState(next);
    setSelected(Array.isArray(next.selected_direction_ids) ? next.selected_direction_ids.map(String) : []);
  }, [candidate.id, candidate.analysis_json.ai_directed_research]);
  useEffect(() => {
    if (!(["QUEUED", "RUNNING", "READY_FOR_EVIDENCE_COLLECTION"].includes(String(state.status)))) return;
    let stopped = false;
    const poll = async () => {
      try {
        const next = await selectionApi.getAiResearch(candidate.id);
        if (stopped) return;
        setState(next as SelectionRecord);
        const nextStatus = String((next as SelectionRecord).status || "");
        if (!["QUEUED", "RUNNING", "READY_FOR_EVIDENCE_COLLECTION"].includes(nextStatus)) {
          const refreshed = await selectionApi.candidate(candidate.id);
          if (!stopped) onUpdated?.(refreshed);
        }
      } catch { /* The next poll or the visible action will expose a durable error. */ }
    };
    void poll();
    const timer = window.setInterval(() => void poll(), 2200);
    return () => { stopped = true; window.clearInterval(timer); };
  }, [candidate.id, state.status, onUpdated]);
  async function prepare() {
    setBusy(true); setError("");
    try {
      const next = await selectionApi.prepareAiResearch(candidate.id);
      setState((next.analysis_json.ai_directed_research || {}) as SelectionRecord);
      onUpdated?.(next);
    } catch (err) { setError(err instanceof Error ? err.message : String(err)); }
    finally { setBusy(false); }
  }
  async function confirm(action: SelectionAiResearchConfirmRequest["action"]) {
    setBusy(true); setError("");
    try {
      const next = await selectionApi.confirmAiResearch(candidate.id, {
        action, selected_direction_ids: selected, reviewer: "USER", comment: comment.trim() || null,
      });
      setState((next.analysis_json.ai_directed_research || {}) as SelectionRecord);
      onUpdated?.(next);
    } catch (err) { setError(err instanceof Error ? err.message : String(err)); }
    finally { setBusy(false); }
  }
  const directions = array(state.directions);
  const status = String(state.status || "NOT_STARTED");
  return <section className="ai-directed-research-panel" aria-label="AI主动研究试验">
    <header className="ai-directed-research-heading"><div><span>新主流程 · AI主动研究试验</span><h3>先理解现有证据，再由你确认研究方向</h3></div><strong>{aiResearchStatusNames[status] || status}</strong></header>
    {status === "NOT_STARTED" && <div className="ai-directed-empty"><p>系统暂不把八维分数当作主结论。点击后，AI将读取黑芝麻智能已有的主营、财务、新闻、公告、公司行为、量价、研究和图谱证据，提出3至5个可验证方向。</p><button type="button" className="primary-button" disabled={disabled || busy} onClick={() => void prepare()}>{busy ? "正在读取证据并分析…" : "读取现有证据并提出研究方向"}</button></div>}
    {status === "MODEL_FAILED" && directions.length === 0 && <div className="ai-directed-empty error"><p>本次没有形成研究结论，系统没有把失败响应写入事实或结论。{String(state.error || "")}</p><button type="button" className="primary-button" disabled={disabled || busy} onClick={() => void prepare()}>{busy ? "正在重试…" : "重试AI主动研究"}</button></div>}
    {status !== "NOT_STARTED" && !(status === "MODEL_FAILED" && directions.length === 0) && <>
      <div className="ai-directed-summary"><div><small>AI当前公司画像</small><p>{String(state.business_profile || "尚未形成公司画像")}</p></div><div><small>初步判断（不等于投资建议）</small><p>{String(state.summary || state.preliminary_conclusion || "尚未形成初步判断")}</p></div></div>
      {array(state.key_findings).length > 0 && <details open className="ai-directed-evidence"><summary>关键事实（{array(state.key_findings).length}）</summary><ul>{array(state.key_findings).map((item, index) => <li key={index}><b>{String(item.stance || "FACT") === "FACT" ? "事实" : String(item.stance || "")}</b> {String(item.claim || "")}{item.quote && <blockquote>{String(item.quote)}</blockquote>}<small>{String(item.reference_type || "")} #{String(item.reference_id || "")}</small></li>)}</ul></details>}
      {array(state.major_events).length > 0 && <details className="ai-directed-evidence"><summary>AI识别的重大事项（{array(state.major_events).length}）</summary><ul>{array(state.major_events).map((item, index) => <li key={index}>{String(item.claim || "")}{item.quote && <blockquote>{String(item.quote)}</blockquote>}<small>{String(item.reference_type || "")} #{String(item.reference_id || "")}</small></li>)}</ul></details>}
      <div className="ai-directed-directions"><div className="ai-directed-directions-title"><div><small>用户确认节点</small><h4>请选择本轮要继续验证的方向</h4></div><span>{selected.length}/{directions.length} 已选</span></div>{directions.map((row, index) => {
        const id = String(row.direction_id || `direction_${index}`); const checked = selected.includes(id);
        return <article key={id} className={checked ? "selected" : ""}><label><input type="checkbox" checked={checked} disabled={busy || status === "READY_FOR_EVIDENCE_COLLECTION" || status === "STOPPED_BY_USER"} onChange={event => setSelected(current => event.target.checked ? [...current, id] : current.filter(value => value !== id))} /><strong>{String(row.title || "研究方向")}</strong><em>{Math.round(Number(row.confidence || 0) * 100)}% · {String(row.recommended_priority || "NORMAL") === "PRIORITY" ? "建议优先" : "建议常规"}</em></label><p><b>待验证假设：</b>{String(row.hypothesis || "")}</p><p><b>为什么重要：</b>{String(row.why_it_matters || "")}</p><p><b>AI当前判断：</b>{String(row.current_judgment || "")}</p>{array(row.supporting_evidence).length > 0 && <details><summary>支持证据（{array(row.supporting_evidence).length}）</summary><ul>{array(row.supporting_evidence).map((evidence, evidenceIndex) => <li key={evidenceIndex}>{String(evidence.claim || "")}<small>{String(evidence.reference_type || "")} #{String(evidence.reference_id || "")}</small></li>)}</ul></details>}{array(row.counter_evidence).length > 0 && <details><summary>反证与不确定性（{array(row.counter_evidence).length}）</summary><ul>{array(row.counter_evidence).map((evidence, evidenceIndex) => <li key={evidenceIndex}>{String(evidence.claim || "")}<small>{String(evidence.reference_type || "")} #{String(evidence.reference_id || "")}</small></li>)}</ul></details>}{strings(row.data_gaps).length > 0 && <p className="ai-directed-gap"><b>待补证：</b>{strings(row.data_gaps).join("；")}</p>}</article>;
      })}</div>
      {status === "WAITING_USER_DIRECTION" && <div className="ai-directed-actions"><label>给本轮研究的补充意见（选填）<textarea rows={2} value={comment} onChange={event => setComment(event.target.value)} placeholder="例如：先核验高管变动是否与业务转型有关。" /></label><div><button type="button" disabled={disabled || busy || selected.length === 0} onClick={() => void confirm("CONFIRM_AND_RESEARCH")} className="primary-button">确认方向并生成补证计划</button><button type="button" disabled={disabled || busy} onClick={() => void confirm("SAVE_DRAFT")}>保存草稿</button><button type="button" disabled={disabled || busy} onClick={() => void confirm("SKIP_RESEARCH")}>本轮不治理</button></div></div>}
      {status === "READY_FOR_EVIDENCE_COLLECTION" && <div className="ai-directed-next"><strong>方向已确认</strong><p>已生成受控补证计划，未将推断写入事实库。数据补齐完成后，再调用AI复核支持证据、反证和置信度。</p><p>维度：{strings(state.collection_plan?.dimensions).join("、") || "当前没有可自动调用的维度"}</p></div>}
      {(status === "QUEUED" || status === "RUNNING") && <div className="ai-directed-progress"><div className="ai-directed-progress-head"><strong>{String(state.progress?.message || "正在处理")}</strong><span>{Number(state.progress?.percent || 0)}%</span></div><div className="ai-directed-progress-track"><i style={{width: `${Math.max(4, Math.min(100, Number(state.progress?.percent || 0)))}%`}} /></div><small>当前阶段：数据补证 → 证据增量比较 → AI复核。该过程不会改写事实库中的原始事实。</small></div>}
      {(status === "SOURCE_FAILED" || status === "MODEL_FAILED") && <div className="ai-directed-empty error"><p>{status === "SOURCE_FAILED" ? "受控数据补证失败" : "模型复核失败"}：{String(state.last_error || "失败详情已记录，现有方向和历史结果均已保留。")}</p><button type="button" className="primary-button" disabled={disabled || busy} onClick={() => void confirm("REQUEST_MORE_EVIDENCE")}>{busy ? "正在重新排队…" : "保留现有证据并重试"}</button></div>}
      {status === "WAITING_CONCLUSION_REVIEW" && <section className="ai-directed-verification" aria-label="AI主动研究阶段结论">
        <header><div><small>补证完成 · 用户复核节点</small><h4>AI阶段复核结果</h4></div><strong>{String(state.latest_verification?.overall_status || "READY_FOR_REVIEW") === "READY_FOR_REVIEW" ? "可复核" : "证据仍不充分"}</strong></header>
        <p className="ai-directed-verification-summary">{String(state.latest_verification?.overall_summary || "尚未形成阶段复核摘要")}</p>
        {array(state.latest_verification?.direction_results).map((row, index) => <article key={`${String(row.direction_id)}-${index}`}><div><strong>{String(directions.find(item => String(item.direction_id) === String(row.direction_id))?.title || row.direction_id || "研究方向")}</strong><span className={`ai-verdict ai-verdict-${String(row.judgment || "INSUFFICIENT").toLowerCase()}`}>{String(row.judgment || "INSUFFICIENT") === "SUPPORTED" ? "支持" : String(row.judgment || "") === "CONTRADICTED" ? "反证" : String(row.judgment || "") === "MIXED" ? "交织" : "证据不足"} · {Math.round(Number(row.confidence || 0) * 100)}%</span></div><p>{String(row.conclusion || "尚未形成方向结论")}</p>{array(row.supporting_evidence).length > 0 && <small>支持证据：{array(row.supporting_evidence).map(item => String(item.claim || "")).join("；")}</small>}{array(row.counter_evidence).length > 0 && <small>反证：{array(row.counter_evidence).map(item => String(item.claim || "")).join("；")}</small>}{strings(row.remaining_gaps).length > 0 && <small>仍需验证：{strings(row.remaining_gaps).join("；")}</small>}</article>)}
        <div className="ai-directed-actions"><button type="button" disabled={disabled || busy} onClick={() => void confirm("REQUEST_MORE_EVIDENCE")}>继续补证一轮</button><button type="button" className="primary-button" disabled={disabled || busy} onClick={() => void confirm("ACCEPT_RESULT")}>确认本轮阶段结论</button></div>
      </section>}
      {status === "DIRECTION_DRAFT_SAVED" && <p className="ai-directed-next">方向草稿已保存。确认后才会进入补证阶段。</p>}
      {status === "COMPLETED" && <p className="ai-directed-next"><strong>本轮阶段结论已确认。</strong> 原始证据、模型调用和各轮结果已保留；该确认不会自动写成 FoundationFact，也不构成买卖建议。</p>}
      {status === "STOPPED_BY_USER" && <p className="ai-directed-next">用户已停止本轮主动研究，历史结果仍保留。</p>}
      {strings(state.data_quality_notes).length > 0 && <details className="ai-directed-notes"><summary>数据质量与引用提醒（{strings(state.data_quality_notes).length}）</summary><ul>{strings(state.data_quality_notes).map((item, index) => <li key={index}>{item}</li>)}</ul></details>}
    </>}
    {error && <p className="batch-alert error">{error}</p>}
  </section>;
}

function HypothesisDirectionReview({review, busy, onSubmit}: {
  review: SelectionRecord;
  busy?: boolean;
  onSubmit: (payload: SelectionHypothesisDirectionRequest) => void;
}) {
  const questions = array(review.questions);
  const rows: Array<SelectionRecord & {research_code: SelectionHypothesisDirectionRequest["directions"][number]["research_code"]}> = questions.flatMap(question => array(question.hypotheses).map(hypothesis => ({
    ...hypothesis,
    research_code: String(question.research_code || "") as SelectionHypothesisDirectionRequest["directions"][number]["research_code"],
  })));
  const revision = `${String(review.revision || 1)}:${rows.map(row => String(row.hypothesis_key)).join("|")}`;
  const [decisions, setDecisions] = useState<Record<string, "PRIORITY" | "NORMAL" | "PAUSED">>({});
  const [theses, setTheses] = useState<Record<string, string>>({});
  const [comment, setComment] = useState("");
  const [newHypothesis, setNewHypothesis] = useState({research_code: "MANAGEMENT_CHANGE", title: "", thesis: ""});
  useEffect(() => {
    setDecisions(Object.fromEntries(rows.map(row => [String(row.hypothesis_key), (String(row.decision || row.system_recommendation || "NORMAL") as "PRIORITY" | "NORMAL" | "PAUSED")])));
    setTheses(Object.fromEntries(rows.map(row => [String(row.hypothesis_key), String(row.thesis || "")])));
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [revision]);
  const selectedCount = rows.filter(row => decisions[String(row.hypothesis_key)] !== "PAUSED").length;
  const priorityCount = rows.filter(row => decisions[String(row.hypothesis_key)] === "PRIORITY").length;
  function toggleDirection(row: SelectionRecord) {
    const key = String(row.hypothesis_key);
    setDecisions(current => ({
      ...current,
      [key]: current[key] === "PAUSED" ? (String(row.system_recommendation) === "PRIORITY" ? "PRIORITY" : "NORMAL") : "PAUSED",
    }));
  }
  function submit(action: SelectionHypothesisDirectionRequest["action"]) {
    const additions = newHypothesis.title.trim() && newHypothesis.thesis.trim() ? [{
      research_code: newHypothesis.research_code as "BUSINESS_MODEL" | "MANAGEMENT_CHANGE" | "ACQUISITION_SECOND_CURVE" | "CAPITAL_ACTION_INTENT",
      title: newHypothesis.title.trim(), thesis: newHypothesis.thesis.trim(), direction: "UNKNOWN" as const, horizon: "UNKNOWN" as const,
    }] : [];
    onSubmit({
      action,
      directions: rows.map(row => ({
        research_code: row.research_code,
        hypothesis_key: String(row.hypothesis_key),
        decision: decisions[String(row.hypothesis_key)] || "NORMAL",
        revised_thesis: theses[String(row.hypothesis_key)] !== String(row.thesis || "") ? theses[String(row.hypothesis_key)] : null,
      })),
      new_hypotheses: additions,
      overall_comment: comment.trim() || null,
      max_rounds: Number(review.max_rounds || 3),
    });
  }
  return <section className="analysis-human-review" aria-label="研究方向确认">
    <header><div><span>AI 已提出研究方向</span><h4>先看判断依据，再选择本轮验证方向</h4></div><strong>{questions.length} 个问题 · {rows.length} 个方向</strong></header>
    <div className="human-priority-guide" aria-label="研究优先级说明">
      <div><strong>勾选方向</strong><span>表示本轮要求系统继续采集数据并验证，不代表你认同该结论。</span></div>
      <div><strong>优先补证</strong><span>首轮优先调用匹配数据源和研究预算，适合可能显著改变判断的方向。</span></div>
      <div><strong>常规补证</strong><span>仍进入研究循环，但按普通顺序验证；未勾选即本轮不验证。</span></div>
    </div>
    <div className="human-question-list">{questions.map((question, questionIndex) => {
      const code = String(question.research_code || "");
      const hypotheses = array(question.hypotheses);
      const facts = array(question.known_facts);
      const gaps = array(question.missing_evidence);
      const plan = strings(question.external_research_plan);
      const tracking = strings(question.tracking_metrics);
      return <article className="human-question-card" key={`${code}-${questionIndex}`}>
        <header><div><small>{questionIndex + 1} · {researchNames[code] || "专项研究"}</small><h5>{String(question.question || "待确认的研究问题")}</h5></div><span>{researchStatusNames[String(question.status)] || "待核验"}</span></header>
        <div className="human-ai-summary"><strong>AI 当前分析</strong><p>{String(question.conclusion || "AI 已识别到需要验证的问题，但尚未形成可发布结论。")}</p><small>{Number(question.confidence || 0) > 0 ? `当前问题置信度 ${Math.round(Number(question.confidence) * 100)}%，仍需补证` : "当前尚未形成可用置信度，以下均为待验证方向"}</small></div>
        {(facts.length > 0 || gaps.length > 0 || plan.length > 0) && <div className="human-question-context">
          {facts.length > 0 && <div><strong>AI 已看到的依据</strong><ul>{facts.slice(0, 4).map((fact, index) => <li key={index}><b>{fact.verification_status === "VERIFIED" ? "已核验" : "待核验"}</b>{String(fact.fact || "")}{fact.significance && <small>{String(fact.significance)}</small>}</li>)}</ul>{facts.length > 4 && <small>另有 {facts.length - 4} 条依据，提交后仍会完整保留。</small>}</div>}
          {(gaps.length > 0 || plan.length > 0) && <div><strong>还需要查什么</strong>{gaps.length > 0 && <ul>{gaps.slice(0, 4).map((gap, index) => <li key={index}>{String(gap.description || "证据缺口")}<small>{String(gap.impact_on_conclusion || "将影响该方向能否成立")}</small></li>)}</ul>}{plan.length > 0 && <p>补证计划：{plan.join("；")}</p>}</div>}
        </div>}
        <fieldset className="human-hypothesis-group"><legend>可多选的待验证方向</legend>{hypotheses.map(row => {
          const key = String(row.hypothesis_key);
          const selected = decisions[key] !== "PAUSED";
          const evidenceItems = array(row.evidence_items);
          return <div className={`human-hypothesis-option ${selected ? "selected" : ""}`} key={key}>
            <label className="human-hypothesis-select"><input type="checkbox" checked={selected} onChange={() => toggleDirection(row)} /><span><strong>{String(row.title || "未命名方向")}</strong><small>{String(row.thesis || "AI 尚未给出完整解释")}</small></span></label>
            <div className="human-hypothesis-meta"><span>支持 {Number(row.supporting_evidence_count || 0)}</span><span>反证 {Number(row.counter_evidence_count || 0)}</span><span>{Number(row.confidence || 0) > 0 ? `初始置信度 ${Math.round(Number(row.confidence) * 100)}%` : "置信度待补证"}</span></div>
            <p className="human-recommendation"><b>AI 建议：</b>{String(row.recommendation_reason || "建议保留为竞争性解释，等待证据验证")}</p>
            {selected && <div className="human-direction-actions" role="group" aria-label={`${String(row.title)}补证顺序`}><button type="button" className={decisions[key] === "PRIORITY" ? "active" : ""} onClick={() => setDecisions(current => ({...current, [key]: "PRIORITY"}))}>优先补证</button><button type="button" className={decisions[key] === "NORMAL" ? "active" : ""} onClick={() => setDecisions(current => ({...current, [key]: "NORMAL"}))}>常规补证</button></div>}
            {(evidenceItems.length > 0 || strings(row.invalidation_conditions).length > 0 || strings(row.watch_conditions).length > 0) && <details className="human-hypothesis-basis"><summary>查看AI的证据判断与失效条件</summary>{evidenceItems.length > 0 && <ul>{evidenceItems.slice(0, 4).map((item, index) => <li key={index}><b>{item.stance === "SUPPORT" ? "支持" : item.stance === "COUNTER" ? "反证" : "正反交织"}</b>{String(item.claim || item.company_specific_effect || "证据判断")}</li>)}</ul>}{strings(row.invalidation_conditions).length > 0 && <p><b>何时该方向不成立：</b>{strings(row.invalidation_conditions).join("；")}</p>}{strings(row.watch_conditions).length > 0 && <p><b>后续观察：</b>{strings(row.watch_conditions).join("；")}</p>}</details>}
            <details className="human-revise-thesis"><summary>调整AI提出的方向</summary><textarea aria-label={`${String(row.title)}假设内容`} rows={3} value={theses[key] ?? String(row.thesis || "")} onChange={event => setTheses(current => ({...current, [key]: event.target.value}))} /></details>
          </div>;
        })}</fieldset>
        {tracking.length > 0 && <p className="human-tracking"><b>后续持续观察：</b>{tracking.join("；")}</p>}
      </article>;
    })}</div>
    <details className="human-new-hypothesis"><summary>增加一个模型没有考虑的方向</summary><label>所属问题<select value={newHypothesis.research_code} onChange={event => setNewHypothesis(current => ({...current, research_code: event.target.value}))}>{Object.entries(researchNames).map(([code, name]) => <option key={code} value={code}>{name}</option>)}</select></label><label>假设标题<input value={newHypothesis.title} maxLength={120} onChange={event => setNewHypothesis(current => ({...current, title: event.target.value}))} /></label><label>为什么值得验证<textarea rows={3} maxLength={800} value={newHypothesis.thesis} onChange={event => setNewHypothesis(current => ({...current, thesis: event.target.value}))} /></label></details>
    <label>整体意见（选填）<textarea rows={2} maxLength={2000} value={comment} onChange={event => setComment(event.target.value)} /></label>
    <div className="human-selection-summary"><strong>已选 {selectedCount} 个方向</strong><span>其中 {priorityCount} 个优先补证 · 用户选择只决定研究顺序，不会写成事实或投资结论</span></div>
    <footer><button type="button" disabled={busy} onClick={() => submit("SAVE_DRAFT")}>保存稍后处理</button><button type="button" disabled={busy} onClick={() => submit("DELEGATE_TO_SYSTEM")}>由AI按建议执行</button><button type="button" className="primary-button" disabled={busy || !selectedCount} onClick={() => submit("CONFIRM_AND_RESEARCH")}>{busy ? "正在提交…" : `确认 ${selectedCount} 个方向并开始补证`}</button></footer>
  </section>;
}

function ResearchHumanReview({phase, busy, onSubmit}: {phase: "EVIDENCE" | "CONCLUSION"; busy?: boolean; onSubmit: (payload: SelectionResearchReviewRequest) => void}) {
  const [comment, setComment] = useState("");
  const [correction, setCorrection] = useState("");
  const evidencePhase = phase === "EVIDENCE";
  function submit(action: SelectionResearchReviewRequest["action"]) {
    const defaultComment = evidencePhase ? "已核对本轮支持证据与反证" : "已核对阶段结论与证据引用";
    onSubmit({phase, action, comment: comment.trim() || defaultComment, correction: correction.trim() || null, max_rounds: 5});
  }
  return <section className="analysis-human-review evidence-review">
    <header><div><span>关键复核节点</span><h4>{evidencePhase ? "支持证据与反证并存" : "确认阶段研究结论"}</h4></div></header>
    <p>{evidencePhase ? "请确认当前解释是否合理。接受后系统继续下一轮；若解释不准确，可填写修正意见重新补证。" : "你可以确认当前阶段结论、要求追加研究或指出解释错误。确认意见不会被当作事实，后续公告和财报仍会持续验证。"}</p>
    <label>复核意见<textarea rows={3} maxLength={2000} value={comment} onChange={event => setComment(event.target.value)} /></label>
    <label>纠正后的解释（仅需纠正时填写）<textarea rows={3} maxLength={1200} value={correction} onChange={event => setCorrection(event.target.value)} /></label>
    <footer><button type="button" disabled={busy} onClick={() => submit("STOP_TRACKING")}>停止该方向</button><button type="button" disabled={busy} onClick={() => submit("REQUEST_MORE_RESEARCH")}>继续补证</button><button type="button" disabled={busy || !correction.trim()} onClick={() => submit("CORRECT")}>按修正意见重验</button><button type="button" className="primary-button" disabled={busy} onClick={() => submit("ACCEPT")}>{evidencePhase ? "接受解释并继续" : "确认阶段结论"}</button></footer>
  </section>;
}

function ResearchLearningSummary({candidateId, version}: {candidateId: number; version: string}) {
  const [summary, setSummary] = useState<SelectionResearchLearning | null>(null);
  useEffect(() => { let valid = true; void selectionApi.researchLearning(candidateId).then(value => {if (valid) setSummary(value);}).catch(() => {if (valid) setSummary(null);}); return () => {valid = false;}; }, [candidateId, version]);
  if (!summary?.feedback_count) return null;
  const sourceRows = Object.entries(summary.source_metrics || {});
  return <details className="analysis-learning-summary"><summary>系统如何从本次协作中学习 · {summary.feedback_count} 条反馈</summary><p>用户选择先作为研究优先级；只有后续证据验证过的纠错才进入 Skill 优化样本。生产 Skill 当前未被自动修改。</p><div>{Object.entries(summary.outcomes || {}).map(([key, value]) => <span key={key}>{key} {value}</span>)}</div>{sourceRows.length > 0 && <table><thead><tr><th>数据环节</th><th>成功</th><th>无数据</th><th>失败</th></tr></thead><tbody>{sourceRows.map(([code, value]) => <tr key={code}><td>{names[code] || code}</td><td>{value.success}</td><td>{value.empty}</td><td>{value.failed}</td></tr>)}</tbody></table>}<small>Skill {summary.skill.code} v{summary.skill.version}{summary.skill.draft_id ? ` · 已生成待审核草案 #${summary.skill.draft_id}` : " · 尚未达到生成优化草案的验证阈值"}</small></details>;
}

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

export function SelectionAnalysis({candidate, onDocument, onBackfillDimension, onBackfillAll, onResearch, onPrepareDirection, onConfirmDirection, onReviewResearch, onAiResearchUpdated, backfillDisabled, onRescore}: {candidate: SelectionCandidate; onDocument?: (id: number) => void; onBackfillDimension?: (dimension: AnalysisDimension) => void; onBackfillAll?: () => void; onResearch?: () => void; onPrepareDirection?: () => void; onConfirmDirection?: (payload: SelectionHypothesisDirectionRequest) => void; onReviewResearch?: (payload: SelectionResearchReviewRequest) => void; onAiResearchUpdated?: (candidate: SelectionCandidate) => void; backfillDisabled?: boolean; onRescore?: () => void}) {
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
  const researchQuestions = array(thesis?.research_questions);
  const deepAssessments = array(thesis?.deep_assessments);
  const researchLoop = (analysis.hypothesis_research || {}) as SelectionRecord;
  const researchHistory = array(researchLoop.history);
  const directionReview = (researchLoop.direction_review || {}) as SelectionRecord;
  // Older pending reviews persisted only titles and counts. Enrich them from
  // the immutable research-question snapshot when available so an existing
  // candidate immediately gets the same explanatory UI as a new one.
  const directionSourceQuestions = [
    ...researchQuestions,
    ...array(researchLoop.research_question_snapshot),
  ];
  const directionReviewForDisplay = directionReview.questions ? {
    ...directionReview,
    questions: array(directionReview.questions).map(reviewQuestion => {
      const source = directionSourceQuestions.find(item => String(item.research_code || "") === String(reviewQuestion.research_code || "")) || {};
      const sourceHypotheses = array(source.hypotheses);
      return {
        ...source,
        ...reviewQuestion,
        known_facts: Array.isArray(reviewQuestion.known_facts) ? reviewQuestion.known_facts : source.known_facts,
        missing_evidence: Array.isArray(reviewQuestion.missing_evidence) ? reviewQuestion.missing_evidence : source.missing_evidence,
        external_research_plan: Array.isArray(reviewQuestion.external_research_plan) ? reviewQuestion.external_research_plan : source.external_research_plan,
        tracking_metrics: Array.isArray(reviewQuestion.tracking_metrics) ? reviewQuestion.tracking_metrics : source.tracking_metrics,
        hypotheses: array(reviewQuestion.hypotheses).map(reviewHypothesis => {
          const sourceHypothesis = sourceHypotheses.find(item => String(item.title || "") === String(reviewHypothesis.title || "")) || {};
          return {
            ...sourceHypothesis,
            ...reviewHypothesis,
            evidence_items: Array.isArray(reviewHypothesis.evidence_items) ? reviewHypothesis.evidence_items : sourceHypothesis.evidence_items,
            invalidation_conditions: Array.isArray(reviewHypothesis.invalidation_conditions) ? reviewHypothesis.invalidation_conditions : sourceHypothesis.invalidation_conditions,
            watch_conditions: Array.isArray(reviewHypothesis.watch_conditions) ? reviewHypothesis.watch_conditions : sourceHypothesis.watch_conditions,
          };
        }),
      };
    }),
  } : directionReview;
  const period = (candidate.evidence_json.analysis_period || usage.analysis_period || {}) as SelectionRecord;
  const scoring = (analysis.scoring || null) as SelectionScorecard | null;
  const aiDirectedTrial = (candidate.market === "HK" && candidate.symbol === "02533") || !!analysis.ai_directed_research;
  const waitingDirection = researchLoop.status === "WAITING_USER_DIRECTION";
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
    {aiDirectedTrial && <AiDirectedResearchPanel candidate={candidate} disabled={backfillDisabled} onUpdated={onAiResearchUpdated} />}
    <details className="analysis-legacy-scorecard"><summary>历史八维分析状态</summary><div className="analysis-empty-conclusion"><strong>尚未形成可用投资结论</strong><p>本轮没有达到最低证据门槛。具体缺失项和补齐入口请查看“数据事实”页签。</p></div></details>
  </section>;
  if (waitingDirection && directionReviewForDisplay.questions && onConfirmDirection) return <section className="selection-analysis">
    {aiDirectedTrial && <AiDirectedResearchPanel candidate={candidate} disabled={backfillDisabled} onUpdated={onAiResearchUpdated} />}
    <details className="analysis-legacy-scorecard"><summary>历史八维研究方向</summary><div className="analysis-empty-conclusion"><strong>已生成竞争性推理方向，等待你的确认</strong><p>这是旧八维流程保留的研究方向，不作为本次AI主动研究试验的主结论。</p>{researchLoop.direction_seed_model_call_log_id ? <small>方向来源：模型调用 #{String(researchLoop.direction_seed_model_call_log_id)} · 未写入事实库</small> : null}</div><HypothesisDirectionReview review={directionReviewForDisplay} busy={backfillDisabled} onSubmit={onConfirmDirection} /></details>
  </section>;
  if (modelFailed) return <section className="selection-analysis">
    {aiDirectedTrial && <AiDirectedResearchPanel candidate={candidate} disabled={backfillDisabled} onUpdated={onAiResearchUpdated} />}
    <details className="analysis-legacy-scorecard"><summary>历史八维分析状态</summary><div className="analysis-empty-conclusion"><strong>本轮模型未形成可校验结论</strong><p>本地证据仍然保留。失败原因和模型调用请查看“数据事实”页签。</p>{onPrepareDirection && <button type="button" className="primary-button" disabled={backfillDisabled} onClick={onPrepareDirection}>生成旧版方向确认方案</button>}</div></details>
  </section>;
  return <section className="selection-analysis">
    {aiDirectedTrial && <AiDirectedResearchPanel candidate={candidate} disabled={backfillDisabled} onUpdated={onAiResearchUpdated} />}
    <details className="analysis-legacy-scorecard" open={!aiDirectedTrial}><summary>历史八维分析（保留用于对照，不作为本次试验主流程）</summary>
    <div className="analysis-verdict-layout"><div className="analysis-verdict"><span className={`analysis-signal ${publication.conclusion_available && model.decision === "WATCH" ? "watch" : ""}`}>{publication.status === "RESEARCH_RUNNING" ? "正在主动研究" : publication.status === "RESEARCH_INCOMPLETE" ? "阶段性研究" : publication.status === "READY_REVIEW_WITH_UNCERTAINTY" ? "可复核 · 含低置信保留项" : !publication.conclusion_available ? "未形成有效结论" : names[model.decision] || "待判断"}</span><h3>{model.headline || "历史分析摘要"}</h3><small>模型综合置信度 {typeof model.confidence === "number" ? `${Math.round(model.confidence * 100)}%` : "—"} · 事件证据置信度 {typeof confidence.final_confidence === "number" ? `${Math.round(confidence.final_confidence * 100)}%` : "—"} · 均不等于收益概率</small>{publication.scoring_available && scoring && <div className="score-summary"><strong>可信综合分 {scoring.trusted_score == null ? "证据不足" : scoring.trusted_score.toFixed(1)}</strong><span>基础分 {scoring.base_score == null ? "--" : scoring.base_score.toFixed(1)} · 覆盖 {Math.round(scoring.coverage * 100)}% · 证据置信 {Math.round(scoring.evidence_confidence * 100)}%</span><small>{String(scoring.profile?.profile_name || "评价方案")} v{String(scoring.profile?.version || "--")} · 分数不是收益概率</small></div>}</div>{validationPartial ? <div className="score-radar-empty"><strong>引用尚未通过，不生成有效评分</strong><p>保留初步研判用于核对，但不会将其计入有效结论或跟踪决策。</p></div> : publication.scoring_available && scoring?.dimensions?.length ? <ScoreRadar rows={scoring.dimensions} /> : <div className="score-radar-empty"><strong>尚未生成有效雷达图</strong><p>{publication.status === "RESEARCH_RUNNING" ? "系统正在补数并重新验证假设，研究结束后再生成可跟踪评分。" : publication.status === "RESEARCH_INCOMPLETE" ? "研究问题尚未闭环，系统会继续补数验证。" : publication.status === "SCORE_FAILED" ? "结论完整性已通过，可按当前已发布方案补算评分。" : "请先重新分析并通过结论校验，再生成评分。"}</p>{publication.status === "SCORE_FAILED" && onRescore && <button type="button" disabled={backfillDisabled} onClick={onRescore}>生成八维评分</button>}</div>}</div>
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
      {researchLoop.status === "WAITING_USER_DIRECTION" && directionReviewForDisplay.questions && onConfirmDirection && <HypothesisDirectionReview review={directionReviewForDisplay} busy={backfillDisabled} onSubmit={onConfirmDirection} />}
      {!researchLoop.status && (modelFailed || researchQuestions.some(item => ["PARTIAL", "INSUFFICIENT"].includes(String(item.status)))) && onPrepareDirection && <section className="analysis-human-review"><header><div><span>协同研究</span><h4>先确认研究方向，再自动补数</h4></div></header><p>系统已经识别尚未闭环的重大事件。请先生成竞争性假设，确认哪些方向值得优先验证。</p><button type="button" className="primary-button" disabled={backfillDisabled} onClick={onPrepareDirection}>生成方向确认方案</button></section>}
      {researchLoop.status === "WAITING_EVIDENCE_REVIEW" && onReviewResearch && <ResearchHumanReview phase="EVIDENCE" busy={backfillDisabled} onSubmit={onReviewResearch} />}
      {researchLoop.status === "WAITING_CONCLUSION_REVIEW" && onReviewResearch && <ResearchHumanReview phase="CONCLUSION" busy={backfillDisabled} onSubmit={onReviewResearch} />}
      <ResearchLearningSummary candidateId={candidate.id} version={`${String(researchLoop.updated_at || "")}:${researchHistory.length}`} />
      {researchQuestions.length > 0 && <section className="analysis-research-questions" aria-label="研究问题闭环">
        <div className="analysis-research-heading"><div><span>研究问题驱动</span><h4>四项专项研究</h4></div><small>事实、假设、缺口与结论分开保存；阶段性结果不能直接确认跟踪</small></div>
        <div className="analysis-research-grid">{researchQuestions.map((question, index) => {
          const code = String(question.research_code || "");
          const facts = array(question.known_facts);
          const questionHypotheses = array(question.hypotheses);
          const gaps = array(question.missing_evidence);
          const plan = strings(question.external_research_plan);
          return <article key={`${code}-${index}`} className={`analysis-research-card research-status-${String(question.status || "INSUFFICIENT").toLowerCase()}`}>
            <header><div><small>{index + 1} · 专项研究</small><strong>{researchNames[code] || code}</strong></div><span>{researchStatusNames[String(question.status)] || "待核验"}</span></header>
            <p className="research-question-text">{String(question.question || "未定义研究问题")}</p>
            <p className="research-conclusion">{String(question.conclusion || "尚未形成结论")}</p>
            <div className="research-meta"><span>置信度 {Math.round(Number(question.confidence || 0) * 100)}%</span><span>已核验事实 {facts.filter(item => item.verification_status === "VERIFIED").length}/{facts.length}</span><span>竞争性假设 {questionHypotheses.length}</span></div>
            {facts.length > 0 && <details><summary>已确认事实（{facts.length}）</summary><ul>{facts.map((fact, factIndex) => <li key={factIndex}><b>{fact.verification_status === "VERIFIED" ? "已核验" : "待核验"}</b> {String(fact.fact || "")}<small>{String(fact.significance || "")}</small>{fact.quote && <blockquote>{String(fact.quote)}</blockquote>}</li>)}</ul></details>}
            {questionHypotheses.length > 0 && <details><summary>竞争性解释（{questionHypotheses.length}）</summary><ul>{questionHypotheses.map((hypothesis, hypothesisIndex) => <li key={hypothesisIndex}><b>{String(hypothesis.title || "未命名假设")}</b> · {thesisStatusNames[String(hypothesis.status)] || "待核验"}<br />{String(hypothesis.thesis || "")}<small>失效条件：{strings(hypothesis.invalidation_conditions).join("；") || "尚未明确"}</small></li>)}</ul></details>}
            {gaps.length > 0 && <details open><summary>待补证（{gaps.length}）</summary><ul>{gaps.map((gap, gapIndex) => <li key={gapIndex}><b>{String(gap.description || "证据缺口")}</b><small>影响：{String(gap.impact_on_conclusion || "未说明")}</small><em>{researchResolutionNames[String(gap.resolution_status)] || String(gap.resolution_status || "待处理")}</em></li>)}</ul>{plan.length > 0 && <p className="research-plan">补证计划：{plan.join("；")}</p>}</details>}
            {strings(question.tracking_metrics).length > 0 && <div className="research-tracking"><small>后续跟踪</small><p>{strings(question.tracking_metrics).join("；")}</p></div>}
            {array(question.evidence_document_ids).length > 0 && <div className="analysis-references">{array(question.evidence_document_ids).map(id => reference(Number(id)))}</div>}
          </article>;
        })}</div>
      </section>}
      {(researchLoop.status || researchHistory.length > 0) && <section className="analysis-research-loop" aria-label="重大事件假设研究历程">
        <div className="analysis-research-heading"><div><span>事件驱动研究闭环</span><h4>重大事件 → 竞争性假设 → 补数 → 重验</h4></div><small>第 {Number(researchLoop.round || 0)} / {Number(researchLoop.max_rounds || 3)} 轮 · {researchLoopNames[String(researchLoop.status)] || researchLoopNames[String(researchLoop.terminal_reason)] || "待执行"}</small></div>
        <p className="research-loop-explain">系统只对可能改变结论的重大事件假设主动补数。只有来源查询成功且连续两轮没有新增证据，才会降低次级假设置信度；数据源或模型失败不会被当作“查无证据”。</p>
        {["SOURCE_FAILED", "MODEL_FAILED"].includes(String(researchLoop.status)) && <p className="batch-alert error"><strong>{researchLoopNames[String(researchLoop.status)]}</strong><br />{String(researchLoop.last_error || "上一轮假设、已采集证据和结论均已保留，可继续重试。")}</p>}
        {researchHistory.length > 0 ? <ol className="research-rounds">{researchHistory.map((round, index) => {
          const questions = array(round.questions);
          const newDocuments = strings(round.new_document_ids);
          const newFacts = strings(round.new_fact_ids);
          const newRelations = strings(round.new_relation_ids);
          return <li key={`${Number(round.round || index + 1)}-${String(round.completed_at || "")}`}>
            <header><strong>第 {Number(round.round || index + 1)} 轮</strong><span className={String(round.status) === "VERIFIED_WITH_INCREMENT" ? "success" : "warning"}>{researchRoundNames[String(round.status)] || "研究状态待核"}</span></header>
            <p>研究事项：{questions.map(item => researchNames[String(item.research_code)] || String(item.question || item.research_code)).join("；") || "重大事件假设"}</p>
            <p>调用板块：{strings(round.dimensions).map(code => names[code] || code).join("、") || "按研究问题动态确定"}</p>
            <div><span>新增原文 {newDocuments.length}</span><span>新增事实 {newFacts.length}</span><span>新增关系 {newRelations.length}</span><span>结构化增量 {Number(round.structured_count_delta || 0)}</span></div>
            {["SOURCE_FAILED", "MODEL_FAILED"].includes(String(round.status)) ? <small>失败轮次不计入无新增证据次数，也不会触发假设置信度衰减。</small> : Number(round.consecutive_no_increment || 0) > 0 && <small>连续 {Number(round.consecutive_no_increment)} 轮无新增证据，相关假设已按确定性规则衰减置信度。</small>}
          </li>;
        })}</ol> : <p className="batch-missing">研究任务已识别，等待后台执行第一轮数据补全与验证。</p>}
        {["SOURCE_FAILED", "MODEL_FAILED", "READY_FOR_RESEARCH"].includes(String(researchLoop.status)) && publication.status === "RESEARCH_INCOMPLETE" && onResearch && <button type="button" disabled={backfillDisabled} onClick={onResearch}>继续执行假设研究</button>}
      </section>}
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
    </details>
  </section>;
}
