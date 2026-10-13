import { useEffect, useRef, useState } from "react";
import { api, request, type StockSymbol } from "./api";
import { batchGovernanceApi, type BatchGovernanceJob } from "./batchGovernanceApi";
import "./documentSemantics.css";
import "./documentSemanticReview.css";
import { GovernanceProgress } from "./GovernanceProgress";
import { useGovernanceDialog } from "./useGovernanceDialog";
import { InteractiveSemanticFlow, type SemanticPlan, type SemanticReport } from "./InteractiveSemanticFlow";

type Doc = { source_table: string; record_id: number; market: string; symbol: string; title: string; published_at: string;
  source_name: string; status: string; error?: string; semantic_complete: boolean; body_characters: number;
  body_available: boolean; skill_version?: string; current_skill_version: string; event_count: number; relation_count: number; version_count: number;
  human_review?: ReviewStatus; workflow_status?: string; governance_plan?: { status: string; primary_type: string } };
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
  versions: { index: number; skill_version: string; semantic_schema_version?: string; observed_at: string; status: string }[];
  human_review: ReviewStatus; review_history: ReviewHistory[]; governance_plan?: SemanticPlan; governance_plan_history?: SemanticPlan[]; result_report?: SemanticReport };
type ReviewHistory = { id: string; version_index?: number; review_round: number; action: string; target_type: string; target_key?: string; event_key?: string;
  item_index?: number; field?: string; old_value?: unknown; new_value?: unknown; reason: string; reviewer: string; resulting_status: string; created_at: string };
type ReviewStatus = { overall: string; review_count: number; latest: Record<string, { action: string; status: string; reviewer: string; reason: string; created_at: string }>;
  history: ReviewHistory[] };
type ReviewTarget = { type: "DOCUMENT" | "EVENT" | "METRIC" | "IMPACT" | "PARTICIPANT" | "RELATION"; label: string; key?: string;
  eventKey?: string; itemIndex?: number; evidence?: string; values?: Record<string, unknown> };
type FieldChange = { field: string; from: unknown; to: unknown };
type CollectionDiff = { added: { key: string; value: Record<string, unknown> }[]; removed: { key: string; value: Record<string, unknown> }[];
  modified: { key: string; changes: FieldChange[] }[]; counts: { added: number; removed: number; modified: number } };
type VersionComparison = { from: { index: number; skill_version?: string; observed_at?: string }; to: { index: number; skill_version?: string; observed_at?: string };
  changed: boolean; summary: Record<string, unknown>; reading: { fields: FieldChange[]; sections: CollectionDiff };
  events: CollectionDiff; participants: CollectionDiff; relations: CollectionDiff; metrics: CollectionDiff; impacts: CollectionDiff };
type ScopePreview = { stocks: Array<{ market: string; symbol: string; stock_name?: string | null; status: string; total: number; completed: number; pending: number; planned: number; failed: number; missing_body: number }>;
  planned_documents: Doc[]; stock_count: number; planned_count: number; pending_count: number; completed_count: number; missing_stocks: unknown[];
  kind: string; disclosure_days: number; per_stock_limit: number | null; note: string };
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
  NOT_REVIEWED: "尚未人工审核", IN_REVIEW: "人工审核中", NEEDS_CORRECTION: "需要修正", HUMAN_CONFIRMED: "人工已确认结构",
  HUMAN_REVIEWED: "人工审核完成", REJECTED: "已驳回", CORRECTED_CANDIDATE: "人工修正候选", REANALYSIS_REQUESTED: "已请求重分析",
  COMMENT: "审核批注", CONFIRM: "确认结构", REQUEST_CORRECTION: "要求修正", MODIFY: "人工修改", REANALYZE: "模型定向重分析",
  DOCUMENT: "整篇文档", EVENT: "事件", METRIC: "指标", IMPACT: "影响", PARTICIPANT: "主体", RELATION: "关系",
  NEEDS_PLAN: "待制定方案", NEEDS_PLAN_CONFIRMATION: "待确认方案", READY_TO_EXECUTE: "方案已确认", NEEDS_RESULT_CONFIRMATION: "待确认知识点",
  NEEDS_REANALYSIS: "等待定向重分析", PUBLISHED: "已发布最终成果", SKIPPED: "用户选择不治理",
  FINANCIAL_DISCLOSURE: "财务披露与业绩", CAPITAL_OPERATION: "资本运作", SHAREHOLDER_PERSONNEL: "股东与人事",
  JUDICIAL_RISK: "司法与风险", CORPORATE_OPERATION: "公司经营事项", SUPPLY_CHAIN: "供应链与经营往来",
  POLICY_INDUSTRY: "政策与产业事件", SECURITY_MARKET: "证券市场观测", SECTOR_MARKET: "板块市场观测",
  MARKET_STATISTICS: "榜单与市场统计", RESEARCH_OPINION: "研究观点", GENERAL_EVIDENCE: "通用证据事实",
  NO_RELEVANT_FACT: "无直接相关事实", PROPOSED: "待确认方案", CONFIRMED: "方案已确认",
  QUEUED: "等待后台领取", STRUCTURED_EVENTS: "新闻公告知识结构化", COMPLETE: "后台本轮结束", SUCCESS: "已完成",
  PREPARING: "准备文档", BODY_FETCH: "读取与补取原文", PLANNING: "阅读全文并制定方案", PLANNED: "等待用户确认方案",
  SEMANTIC_READING: "理解原文", STRUCTURED_EXTRACTION: "抽取知识点与关系", STRUCTURED_EXTRACTION_REPAIR: "核验证据", FINISHED: "后台文档处理结束",
  PLAN_FAILED: "方案制定失败",
};
const name = (value: string) => labels[value] || value;
const prefix = "/document-governance";
const docKey = (doc: Pick<Doc, "source_table" | "record_id">) => `${doc.source_table}:${doc.record_id}`;
function Quote({ text }: { text: string }) { return <details className="ds-quote"><summary>查看原文证据</summary><blockquote>{text}</blockquote></details>; }
function Badge({ type }: { type: string }) { return <span className={`ds-badge ds-${type.toLowerCase()}`}>{name(type)}</span>; }
const reviewFields: Record<ReviewTarget["type"], string[]> = {
  DOCUMENT: [], EVENT: ["summary", "event_type", "assertion_type", "attribution", "event_time_raw", "direction", "evidence_quote"],
  METRIC: ["name", "raw_value", "unit", "qualifier", "period", "evidence_quote"],
  IMPACT: ["dimension", "direction", "rationale", "assertion_type", "attribution", "uncertainty", "evidence_quote"],
  PARTICIPANT: ["name", "role", "aliases", "evidence_quote"],
  RELATION: ["predicate", "explanation", "assertion_type", "attribution", "evidence_quote"],
};
const fieldNames: Record<string, string> = { summary: "摘要", event_type: "事件类型", assertion_type: "事实性质", attribution: "信息归属",
  event_time_raw: "原文时间", direction: "影响方向", evidence_quote: "原文证据", name: "名称", raw_value: "原文数值", unit: "单位",
  qualifier: "数值性质", period: "所属期间", dimension: "影响维度", rationale: "判断依据", uncertainty: "不确定性", role: "主体角色",
  aliases: "别名", predicate: "关系类型", explanation: "关系说明" };
function ReviewButton({ target, onOpen }: { target: ReviewTarget; onOpen: (target: ReviewTarget) => void }) {
  return <button type="button" className="ds-review-button" onClick={() => onOpen(target)}>审核/纠错</button>;
}
function SourceContext({ body, quote }: { body: string; quote?: string }) {
  if (!quote) return <p className="ds-muted">请选择带原文证据的对象，或打开“原文与证据”查看全文。</p>;
  const start = body.indexOf(quote);
  if (start < 0) return <blockquote>{quote}</blockquote>;
  const before = body.slice(Math.max(0, start - 180), start), after = body.slice(start + quote.length, start + quote.length + 180);
  return <div className="ds-source-context">{before}<mark>{quote}</mark>{after}</div>;
}
function EditControl({ field, value, onChange }: { field: string; value: string; onChange: (value: string) => void }) {
  const options: Record<string, string[]> = {
    assertion_type: ["REPORTED_FACT", "AUTHOR_OPINION", "MODEL_INFERENCE"], direction: ["POSITIVE", "NEGATIVE", "NEUTRAL", "MIXED", "UNKNOWN"],
    qualifier: ["EXACT", "APPROXIMATE", "UPPER_BOUND", "LOWER_BOUND", "ESTIMATE"], unit: ["元", "万元", "亿元", "%", "倍", "股", "万股", "亿股"],
    dimension: ["PROFIT", "CASH_FLOW", "FINANCING", "GOVERNANCE", "REGULATORY", "OPERATION", "STOCK_PRICE"],
    predicate: ["INVOLVES", "CAUSES", "CONTRIBUTES_TO", "PRECEDES", "FINANCES", "SUBSCRIBES", "RENAMED_TO", "CONTROLS", "RELATED_PARTY", "EXPLAINS", "DISPUTES"],
  };
  if (options[field]) return <select value={value} onChange={event => onChange(event.target.value)}>{options[field].map(item => <option key={item} value={item}>{name(item)}</option>)}</select>;
  return <textarea rows={field === "evidence_quote" || field === "summary" || field === "rationale" || field === "explanation" ? 5 : 2} value={value} onChange={event => onChange(event.target.value)} />;
}

function displayValue(value: unknown): string {
  if (value === null || value === undefined || value === "") return "未提供";
  if (typeof value === "string") return name(value);
  if (Array.isArray(value)) return value.map(displayValue).join("、");
  if (typeof value === "object") return JSON.stringify(value, null, 2);
  return String(value);
}

const diffNames: Record<string, string> = { events: "事件", participants: "主体", relations: "关系", metrics: "指标", impacts: "影响", sections: "章节" };
function DiffBlock({ title, diff }: { title: string; diff: CollectionDiff }) {
  if (!diff.counts.added && !diff.counts.removed && !diff.counts.modified) return null;
  return <article className="ds-diff-block"><h4>{title}<span>新增 {diff.counts.added} · 删除 {diff.counts.removed} · 修改 {diff.counts.modified}</span></h4>
    {diff.added.map(item => <div className="ds-diff-added" key={`add:${item.key}`}><strong>＋ {item.key}</strong><pre>{displayValue(item.value)}</pre></div>)}
    {diff.removed.map(item => <div className="ds-diff-removed" key={`remove:${item.key}`}><strong>－ {item.key}</strong><pre>{displayValue(item.value)}</pre></div>)}
    {diff.modified.map(item => <div className="ds-diff-modified" key={`modify:${item.key}`}><strong>△ {item.key}</strong>{item.changes.map(change => <p key={change.field}><b>{fieldNames[change.field] || change.field}</b><del>{displayValue(change.from)}</del><ins>{displayValue(change.to)}</ins></p>)}</div>)}
  </article>;
}

export function DocumentSemanticWorkbench() {
  const link = new URLSearchParams(window.location.hash.split("?")[1] || "");
  const [market, setMarket] = useState(link.get("market") || "CN_A"), [symbol, setSymbol] = useState(link.get("symbol") || "000159"), [keyword, setKeyword] = useState("");
  const [stockQuery, setStockQuery] = useState(link.get("symbol") || "000159"), [stockMatches, setStockMatches] = useState<StockSymbol[]>([]);
  const [resolvedStock, setResolvedStock] = useState<StockSymbol | null>(null), [stockSearchFocused, setStockSearchFocused] = useState(false);
  const [kind, setKind] = useState("NEWS"), [offset, setOffset] = useState(0), [rows, setRows] = useState<Doc[]>([]), [total, setTotal] = useState(0);
  const [stockName, setStockName] = useState(""), [selected, setSelected] = useState<Pick<Doc, "source_table" | "record_id">[]>([]), [detail, setDetail] = useState<Detail | null>(null);
  const [loadedFilter, setLoadedFilter] = useState({ market, symbol, kind, q: keyword });
  const [tab, setTab] = useState("reading"), [error, setError] = useState(""), [busy, setBusy] = useState(false), [loading, setLoading] = useState(false);
  const [job, setJob] = useState<BatchGovernanceJob | null>(null);
  const [taskWindowOpen, setTaskWindowOpen] = useState(false);
  const [taskItemsOpen, setTaskItemsOpen] = useState(false), [taskOffset, setTaskOffset] = useState(0);
  const [reviewTarget, setReviewTarget] = useState<ReviewTarget | null>(null), [reviewReason, setReviewReason] = useState("");
  const [reviewer, setReviewer] = useState(() => window.localStorage.getItem("semantic-reviewer") || "数据审核员");
  const [editField, setEditField] = useState(""), [editValue, setEditValue] = useState("");
  const [compareFrom, setCompareFrom] = useState(0), [compareTo, setCompareTo] = useState(0);
  const [comparison, setComparison] = useState<VersionComparison | null>(null);
  const [scopeMode, setScopeMode] = useState<"DOCUMENTS" | "STOCKS">("DOCUMENTS");
  const [scopeInput, setScopeInput] = useState("CN_A:000159");
  const [scopeKind, setScopeKind] = useState<"ALL" | "NEWS" | "NOTICE">("ALL");
  const [scopeDays, setScopeDays] = useState(730), [scopeLimit, setScopeLimit] = useState(10);
  const [scopeAll, setScopeAll] = useState(true);
  const [scopePreview, setScopePreview] = useState<ScopePreview | null>(null);
  const detailRef = useRef(detail); detailRef.current = detail;
  useGovernanceDialog(detail ? ".ds-governance-dialog" : taskItemsOpen ? ".ds-task-items-dialog" : taskWindowOpen && job ? ".ds-task-dialog" : null, () => {
    if (busy) return;
    if (detail) setDetail(null); else if (taskItemsOpen) setTaskItemsOpen(false); else setTaskWindowOpen(false);
  });
  const active = !!job && ["PENDING", "RUNNING", "RETRY"].includes(String(job.job_status || job.status));
  async function load(nextOffset = offset, targetMarket = market, targetSymbol = symbol) {
    if (!targetSymbol || targetMarket === "ALL") return;
    setLoading(true);
    try {
      const params = new URLSearchParams({ market: targetMarket, symbol: targetSymbol.trim(), kind, q: keyword, offset: String(nextOffset), limit: "20" });
      const result = await request<{ items: Doc[]; total: number; stock_name?: string }>(`${prefix}/documents?${params}`);
      setRows(result.items); setTotal(result.total); setStockName(result.stock_name || "未找到股票"); setError("");
      setLoadedFilter({ market: targetMarket, symbol: targetSymbol.trim(), kind, q: keyword });
    } catch (e) { setError(e instanceof Error ? e.message : "文档读取失败"); } finally { setLoading(false); }
  }
  async function selectStock(item: StockSymbol) {
    setResolvedStock(item); setMarket(item.market); setSymbol(item.symbol); setStockQuery(`${item.name} ${item.symbol}`);
    setStockMatches([]); setStockSearchFocused(false); setOffset(0); setSelected([]); setDetail(null);
    await load(0, item.market, item.symbol);
  }
  async function resolveStockAndLoad() {
    const query = stockQuery.trim();
    if (!query) { setError("请输入股票代码、中文名称、英文名称、拼音或拼音首字母"); return; }
    if (resolvedStock) { setOffset(0); setSelected([]); await load(0, resolvedStock.market, resolvedStock.symbol); return; }
    setLoading(true); setError("");
    try {
      const matches = await api.searchStocks(market, query, 12);
      const compact = query.replace(/\s+/g, "").toLowerCase();
      const exact = matches.find(item => item.symbol.toLowerCase() === compact || item.name.replace(/\s+/g, "").toLowerCase() === compact);
      const selected = exact || (matches.length === 1 ? matches[0] : null);
      if (selected) await selectStock(selected);
      else if (matches.length) { setStockMatches(matches); setStockSearchFocused(true); setError(`匹配到 ${matches.length} 只股票，请从候选列表中选择。`); }
      else setError("未找到匹配股票，请检查市场范围或先同步股票主数据。");
    } catch (e) { setError(e instanceof Error ? e.message : "股票搜索失败"); } finally { setLoading(false); }
  }
  async function open(doc: Pick<Doc, "source_table" | "record_id">, version?: number) {
    try {
      const result = await request<Detail>(`${prefix}/documents/${doc.source_table}/${doc.record_id}${version === undefined ? "" : `?version_index=${version}`}`);
      setDetail(result); setTab("reading"); setReviewTarget(null); setReviewReason(""); setComparison(null);
      setCompareTo(result.selected_version); setCompareFrom(Math.max(0, result.selected_version - 1));
    } catch (e) { setError(e instanceof Error ? e.message : "成果读取失败"); }
  }
  useEffect(() => { void load(); }, [kind, offset]);
  useEffect(() => {
    const query = stockQuery.trim();
    if (scopeMode !== "DOCUMENTS" || !query || resolvedStock) { setStockMatches([]); return; }
    let valid = true;
    const timer = window.setTimeout(() => {
      void api.searchStocks(market, query, 12).then(items => { if (valid) setStockMatches(items); }).catch(() => { if (valid) setStockMatches([]); });
    }, 250);
    return () => { valid = false; window.clearTimeout(timer); };
  }, [market, stockQuery, resolvedStock, scopeMode]);
  useEffect(() => {
    let controller = new AbortController(), stopped = false, sequence = 0;
    const readLink = () => {
      if (!window.location.hash.startsWith("#/operations/documents")) return;
      controller.abort(); controller = new AbortController();
      const current = ++sequence;
      const params = new URLSearchParams(window.location.hash.split("?")[1] || "");
      const jobId = Number(params.get("job"));
      if (jobId > 0) {
        void batchGovernanceApi.get(jobId, controller.signal).then(value => {
          if (stopped || current !== sequence) return;
          setJob(value); setDetail(null); setTaskWindowOpen(true); setTaskItemsOpen(false); setTaskOffset(0);
          const target = value.document_progress?.current_document || value.effective_options?.stocks?.[0] || value.stock_results?.[0];
          if (target?.market && target.symbol) {
            setMarket(target.market); setSymbol(target.symbol); setStockQuery(target.symbol); setSelected([]);
            void load(0, target.market, target.symbol);
          }
          if (params.get("id") && ["stock_news", "stock_notice"].includes(params.get("table") || "")) {
            void open({ source_table: params.get("table")!, record_id: Number(params.get("id")) });
          }
        }).catch(cause => { if (!stopped && current === sequence) setError(cause instanceof Error ? cause.message : "治理任务读取失败"); });
      } else if (params.get("id") && ["stock_news", "stock_notice"].includes(params.get("table") || "")) {
        void open({ source_table: params.get("table")!, record_id: Number(params.get("id")) });
      }
    };
    readLink(); window.addEventListener("hashchange", readLink);
    return () => { stopped = true; controller.abort(); window.removeEventListener("hashchange", readLink); };
  }, []);
  useEffect(() => {
    if (!active || !job) return;
    let cancelled = false, inFlight = false;
    const controller = new AbortController();
    const tick = async () => {
      if (inFlight) return;
      inFlight = true;
      try {
        const result = await batchGovernanceApi.get(job.job_id, controller.signal);
        if (cancelled) return;
        setJob(result);
        if (!["PENDING", "RUNNING", "RETRY"].includes(String(result.job_status || result.status))) { await load(); if (detailRef.current) await open(detailRef.current); }
      } catch (e) { if (!cancelled) setError(`任务状态暂未更新，将自动重试：${e instanceof Error ? e.message : "网络连接失败"}`); }
      finally { inFlight = false; }
    };
    const timer = window.setInterval(() => void tick(), 3000);
    void tick();
    return () => { cancelled = true; controller.abort(); window.clearInterval(timer); };
  }, [active, job?.job_id]);
  async function selectAllDocuments() {
    setBusy(true); setError("");
    try {
      const result = await request<{ documents: Pick<Doc, "source_table" | "record_id">[]; total: number }>(
        `${prefix}/documents/selection?${new URLSearchParams(loadedFilter)}`);
      setSelected(result.documents);
    } catch (e) { setError(e instanceof Error ? e.message : "全选文档失败"); } finally { setBusy(false); }
  }
  async function analyze(docs: Pick<Doc, "source_table" | "record_id">[]) {
    if (!docs.length) return;
    setBusy(true); setError("");
    try {
      const result = await request<BatchGovernanceJob>(`${prefix}/analyze`, { method: "POST", body: JSON.stringify({
        documents: docs.map(d => ({ source_table: d.source_table, record_id: d.record_id })), idempotency_key: crypto.randomUUID(),
      }) });
      setJob(result); setTaskWindowOpen(!detail);
    } catch (e) { setError(e instanceof Error ? e.message : "创建语义治理任务失败"); } finally { setBusy(false); }
  }
  async function planSelected() {
    if (!selected.length) return;
    setBusy(true); setError("");
    try {
      const result = await request<BatchGovernanceJob>(`${prefix}/plan-batch`, { method: "POST", body: JSON.stringify({ documents: selected,
        idempotency_key: crypto.randomUUID() }) });
      setJob(result); setTaskWindowOpen(true);
    } catch (e) { setError(e instanceof Error ? e.message : "批量方案生成失败"); } finally { setBusy(false); }
  }
  function parseScopeStocks() {
    const parsed = scopeInput.split(/[\n,;，；\s]+/).map(value => value.trim()).filter(Boolean).map(value => {
      const [maybeMarket, maybeSymbol] = value.includes(":") ? value.split(":", 2) : [market, value];
      return { market: maybeMarket.trim().toUpperCase(), symbol: maybeSymbol.trim().toUpperCase() };
    }).filter(item => item.market && item.symbol);
    return [...new Map(parsed.map(item => [`${item.market}:${item.symbol}`, item])).values()];
  }
  async function previewScope() {
    const stocks = parseScopeStocks();
    if (!stocks.length) { setError("请输入至少一只股票，例如 CN_A:000159 或 000159"); return; }
    setBusy(true); setError("");
    try {
      setScopePreview(await request<ScopePreview>(`${prefix}/scope-preview`, { method: "POST", body: JSON.stringify({
        stocks, kind: scopeKind, disclosure_days: scopeDays, per_stock_limit: scopeAll ? null : scopeLimit,
      }) }));
    } catch (e) { setError(e instanceof Error ? e.message : "范围预览失败"); } finally { setBusy(false); }
  }
  async function analyzeScope(planOnly = true) {
    const stocks = parseScopeStocks();
    if (!stocks.length) { setError("请输入至少一只股票"); return; }
    setBusy(true); setError("");
    try {
      const result = await request<BatchGovernanceJob>(`${prefix}/${planOnly ? "plan-scope" : "analyze-scope"}`, { method: "POST", body: JSON.stringify({
        stocks, kind: scopeKind, disclosure_days: scopeDays, per_stock_limit: scopeAll ? null : scopeLimit, idempotency_key: crypto.randomUUID(),
      }) });
      setJob(result); setTaskWindowOpen(true); setScopePreview(null);
    } catch (e) { setError(e instanceof Error ? e.message : "创建批量语义治理任务失败"); } finally { setBusy(false); }
  }
  function chooseReview(target: ReviewTarget) {
    setReviewTarget(target); setReviewReason("");
    const first = reviewFields[target.type][0] || "";
    setEditField(first);
    const value = first ? target.values?.[first] : "";
    setEditValue(Array.isArray(value) ? value.join("、") : String(value ?? ""));
    window.setTimeout(() => document.querySelector<HTMLElement>(".ds-review-editor")?.scrollIntoView({ behavior: "smooth", block: "center" }), 10);
  }
  function changeField(field: string) {
    setEditField(field);
    const value = reviewTarget?.values?.[field];
    setEditValue(Array.isArray(value) ? value.join("、") : String(value ?? ""));
  }
  async function review(action: "COMMENT" | "CONFIRM" | "REJECT" | "REQUEST_CORRECTION" | "MODIFY" | "REANALYZE", target = reviewTarget) {
    if (!detail || !target || !reviewReason.trim() || !reviewer.trim()) return;
    setBusy(true); setError(""); window.localStorage.setItem("semantic-reviewer", reviewer.trim());
    try {
      let value: unknown = editValue;
      if (action === "MODIFY" && editField === "aliases") value = editValue.split(/[、,，]/).map(item => item.trim()).filter(Boolean);
      const result = await request<{ job?: BatchGovernanceJob }>(`${prefix}/documents/${detail.source_table}/${detail.record_id}/reviews`, {
        method: "POST", body: JSON.stringify({ version_index: detail.selected_version, action, target_type: target.type,
          target_key: target.key || null, event_key: target.eventKey || null, item_index: target.itemIndex ?? null,
          field: action === "MODIFY" ? editField : null, value: action === "MODIFY" ? value : null,
          reason: reviewReason.trim(), reviewer: reviewer.trim() }),
      });
      if (result.job) setJob(result.job);
      await load(); await open(detail);
      setReviewTarget(null); setReviewReason("");
      if (action !== "REANALYZE") setTab("review");
    } catch (e) { setError(e instanceof Error ? e.message : "人工审核提交失败"); } finally { setBusy(false); }
  }
  async function compareVersions(from = compareFrom, to = compareTo) {
    if (!detail) return;
    setBusy(true); setError("");
    try {
      const params = new URLSearchParams({ from_index: String(from), to_index: String(to) });
      setComparison(await request<VersionComparison>(`${prefix}/documents/${detail.source_table}/${detail.record_id}/compare?${params}`));
      setTab("review");
    } catch (e) { setError(e instanceof Error ? e.message : "版本对比失败"); } finally { setBusy(false); }
  }
  const analysis = detail?.analysis;
  const titles = Object.fromEntries([...(analysis?.participants || []).map(p => [p.key, p.name]), ...(analysis?.events || []).map(e => [e.key, e.event_type])]);
  const impacts = analysis?.events.flatMap(event => event.impacts.map(impact => ({ event: event.event_type, impact }))) || [];
  const isLatest = !!detail && detail.selected_version === detail.versions.length - 1;
  const documentTarget: ReviewTarget = { type: "DOCUMENT", label: detail?.title || "整篇文档", evidence: detail?.body, values: {} };
  const semanticStage = job?.stage_results?.STRUCTURED_EVENTS;
  const planOnly = !!((job?.effective_options as Record<string, unknown> | undefined)?.semantic_plan_only || semanticStage?.semantic_plan_only);
  const runs = (Array.isArray(semanticStage?.stock_runs) ? semanticStage.stock_runs : []) as Array<{ documents?: Array<{ source_table: string; source_record_id: number; title: string; status: string; error?: string }> }>;
  const taskDocuments = [...new Map([
    ...(job?.effective_options?.document_targets || []).map(doc => ({ ...doc, title: rows.find(row => docKey(row) === docKey(doc))?.title || `待处理文档 #${doc.record_id}`, status: "PENDING", error: "" })),
    ...runs.flatMap(run => (run.documents || []).map(doc => ({ ...doc, record_id: doc.source_record_id, error: doc.error || "" }))),
  ].map(doc => [`${doc.source_table}:${doc.record_id}`, doc])).values()];
  const openTaskItems = () => { setTaskOffset(0); setTaskItemsOpen(true); };
  return <div className="ds">
    <header className="ds-heading"><div><p className="eyebrow">DOCUMENT SEMANTIC GOVERNANCE</p><h1>新闻公告语义治理</h1><p>全文理解 → 信息抽取 → 证据校验 → 候选事实与关系入库</p></div><a href="#/operations/records">查看治理记录与报告</a></header>
    <p className="ds-note">区分文中转述事实、作者观点与模型推断；业务影响按维度解释。原文引文核验通过仍需事实审核，不能视为已确认的股价判断。</p>
    {error && <p role="alert" className="ds-error">{error}</p>}
    {taskWindowOpen && job && <div className="ds-governance-backdrop" onMouseDown={event => { if (event.target === event.currentTarget) setTaskWindowOpen(false); }}>
      <section className="ds-task-dialog" role="dialog" tabIndex={-1} aria-modal="true" aria-label="新闻公告治理过程">
        <header className="ds-task-dialog-header"><div><p className="eyebrow">GOVERNANCE PROCESS</p><h2>新闻公告知识结构化治理 · 任务 #{job.job_id}</h2><p>主窗口展示节点和进度；治理报告只读查询结果。</p></div><button type="button" onClick={() => setTaskWindowOpen(false)}>关闭</button></header>
        <div className="ds-task-dialog-body">{error && <p role="alert" className="ds-error">{error}</p>}{job.error_message && <p role="alert" className="ds-error">{job.error_message}</p>}<div className="ds-task-progress"><strong>后台进度 {job.progress || 0}%</strong><GovernanceProgress job={job} /><span>{name(String(job.job_status || job.status))} · {name(String(job.document_progress?.phase || job.current_stage || "QUEUED"))}</span></div>
          {job.document_progress && <section className="ds-current-document"><h3>当前处理文档</h3><p>{job.document_progress.current_document?.title || "正在准备文档"}</p><small>已处理 {job.document_progress.processed || 0} / {job.document_progress.total || 0} 篇 · 成功 {job.document_progress.succeeded || 0} · 部分完成 {job.document_progress.partial || 0} · 失败 {job.document_progress.failed || 0}</small></section>}
          <p className="ds-note">{active ? "正在后台处理，进度自动刷新。" : "后台本轮已结束，请逐篇核对方案或知识点。"}后台进度100%不代表用户已确认或最终成果已发布。</p>
          <ol className="ds-process-nodes"><li className={active && planOnly ? "active" : ""}>1. 读取原文与识别类型<small>{planOnly ? active ? "正在制定方案" : "查看各文档方案结果" : "依据已确认方案"}</small></li><li><button disabled={!taskDocuments.length} onClick={openTaskItems}>2. 用户确认类型与Skill</button><small>逐篇确认，允许修改类型与Skill</small></li><li className={active && !planOnly ? "active" : ""}>3. 执行抽取与证据核验<small>{planOnly ? "确认方案后再执行" : active ? "后台执行中" : "本轮结束，查看缺口"}</small></li><li><button disabled={!taskDocuments.length} onClick={openTaskItems}>4. 用户核对知识点并修正</button><small>不符合项再次分析，不自动确认</small></li><li><button disabled={!taskDocuments.length} onClick={openTaskItems}>5. 确认并发布最终成果</button><small>以各文档发布状态为准</small></li></ol>
          <button type="button" disabled={!taskDocuments.length} onClick={openTaskItems}>查看处理文档与交互待办（{taskDocuments.length}篇）</button>
          <button type="button" disabled={busy || (!job.document_progress?.current_document && !taskDocuments.length)} onClick={() => { const doc = job.document_progress?.current_document || taskDocuments[0]; if (doc) void open(doc); }}>打开文档交互明细</button>
          <div className="ds-task-dialog-actions"><button type="button" disabled={busy || !["PENDING", "RETRY"].includes(String(job.job_status || job.status))} onClick={() => { setBusy(true); void batchGovernanceApi.start(job.job_id).then(setJob).catch(e => setError(e instanceof Error ? e.message : "启动失败")).finally(() => setBusy(false)); }}>立即执行</button><button type="button" onClick={() => { setTaskWindowOpen(false); window.location.hash = `#/operations/workbench?job=${job.job_id}`; }}>进入治理工作台</button><a href={`#/operations/records?job=${job.job_id}`}>查看只读治理报告</a></div>
        </div>
      </section>
    </div>}
    {taskItemsOpen && job && <div className="ds-governance-backdrop ds-task-items-backdrop"><section className="ds-task-dialog ds-task-items-dialog" role="dialog" tabIndex={-1} aria-modal="true" aria-label="文档治理交互待办">
      <header className="ds-task-dialog-header"><div><h2>分项文档与交互待办</h2><p>任务 #{job.job_id} · 点击文档单独核对方案、知识点及发布状态。</p></div><button onClick={() => setTaskItemsOpen(false)}>关闭文档待办</button></header>
      <div className="ds-task-dialog-body">{error && <p role="alert" className="ds-error">{error}</p>}<div className="ds-table-wrap"><table><thead><tr><th>文档</th><th>本轮处理状态</th><th>操作</th></tr></thead><tbody>{taskDocuments.slice(taskOffset, taskOffset + 20).map(doc => <tr key={`${doc.source_table}:${doc.record_id}`}><td>{doc.title}<small>{doc.source_table === "stock_news" ? "新闻" : "公告"}</small></td><td>{name(doc.status)}{doc.error && <small className="ds-error">{doc.error}</small>}</td><td><button disabled={busy} onClick={() => void open(doc)}>打开交互治理</button></td></tr>)}</tbody></table></div><div className="ds-toolbar"><span>共 {taskDocuments.length}篇 · 第 {Math.floor(taskOffset / 20) + 1} / {Math.max(1, Math.ceil(taskDocuments.length / 20))} 页</span><button disabled={taskOffset === 0} onClick={() => setTaskOffset(Math.max(0, taskOffset - 20))}>上一页文档</button><button disabled={taskOffset + 20 >= taskDocuments.length} onClick={() => setTaskOffset(taskOffset + 20)}>下一页文档</button></div></div>
    </section></div>}
    <section className="ds-panel">
      <div className="ds-scope-tabs" role="tablist" aria-label="语义治理范围"><button type="button" className={scopeMode === "DOCUMENTS" ? "active" : ""} onClick={() => setScopeMode("DOCUMENTS")}>按文档勾选</button><button type="button" className={scopeMode === "STOCKS" ? "active" : ""} onClick={() => setScopeMode("STOCKS")}>按股票批量</button></div>
      {job && <div className="ds-job" role="status"><strong>专项任务 #{job.job_id} · {name(String(job.job_status || job.status))}</strong><GovernanceProgress job={job} /><a href={`#/operations/records?job=${job.job_id}`}>查看阶段报告</a>{["PENDING", "RETRY"].includes(String(job.job_status)) && <button type="button" disabled={busy} onClick={() => { setBusy(true); void batchGovernanceApi.start(job.job_id).then(setJob).catch(e => setError(e instanceof Error ? e.message : "启动失败")).finally(() => setBusy(false)); }}>立即执行</button>}{job.error_message && <span className="ds-error">{job.error_message}</span>}{!active && <span>本轮结果：{name(job.result_status || "UNKNOWN")}；请检查成果与缺口。</span>}</div>}
      {job && !taskWindowOpen && <button className="ds-open-task-window" type="button" onClick={() => setTaskWindowOpen(true)}>打开治理过程窗口</button>}
      {scopeMode === "STOCKS" && <section className="ds-batch-scope">
        <div><h3>批量结构化新闻与公告</h3><p className="ds-muted">股票数量不限，默认处理时间范围内全部未完成文档；后台分批执行，已完成文档自动跳过。格式：<code>CN_A:000159</code>，也可每行只填代码并使用当前市场。</p></div>
        <div className="ds-scope-form">
          <label>股票范围<textarea rows={3} value={scopeInput} onChange={event => { setScopeInput(event.target.value); setScopePreview(null); }} placeholder="CN_A:000159&#10;CN_A:000001&#10;HK:00700" /></label>
          <label>文档类型<select value={scopeKind} onChange={event => { setScopeKind(event.target.value as typeof scopeKind); setScopePreview(null); }}><option value="ALL">新闻 + 公告</option><option value="NEWS">仅新闻</option><option value="NOTICE">仅公告</option></select></label>
          <label>时间范围（天）<input type="number" min={1} max={3650} value={scopeDays} onChange={event => { setScopeDays(Number(event.target.value)); setScopePreview(null); }} /></label>
          <label><span>处理范围</span><span><input type="checkbox" checked={scopeAll} onChange={event => { setScopeAll(event.target.checked); setScopePreview(null); }} />全部未完成文档（不限篇数）</span></label>
          {!scopeAll && <label>每股处理篇数<input type="number" min={1} value={scopeLimit} onChange={event => { setScopeLimit(Number(event.target.value)); setScopePreview(null); }} /></label>}
          <button type="button" disabled={busy || active} onClick={() => void previewScope()}>{busy ? "处理中…" : "预览范围"}</button>
          <button type="button" className="ds-primary" disabled={busy || active || !scopePreview?.planned_count} onClick={() => void analyzeScope()}>{busy ? "正在提交…" : "批量阅读全文并制定方案"}</button>
          <button type="button" disabled={busy || active} onClick={() => void analyzeScope(false)}>执行范围内已确认方案</button>
        </div>
        {scopePreview && <div className="ds-scope-preview"><p><strong>范围预览：</strong>{scopePreview.stock_count} 只股票，待处理 {scopePreview.pending_count} 篇，本次计划 {scopePreview.planned_count} 篇，已完成 {scopePreview.completed_count} 篇。</p><small>{scopePreview.note}</small><div className="ds-table-wrap"><table><thead><tr><th>股票</th><th>范围文档</th><th>已完成</th><th>待处理</th><th>本次计划</th><th>失败／正文缺失</th></tr></thead><tbody>{scopePreview.stocks.map(item => <tr key={`${item.market}:${item.symbol}`}><td>{item.stock_name || "股票未找到"} · {item.market}:{item.symbol}</td><td>{item.total}</td><td>{item.completed}</td><td>{item.pending}</td><td>{item.planned}</td><td>{item.failed} ／ {item.missing_body}</td></tr>)}</tbody></table></div></div>}
      </section>}
      {scopeMode === "DOCUMENTS" && <>
      <form className="ds-toolbar" onSubmit={e => { e.preventDefault(); void resolveStockAndLoad(); }}>
        <label>市场<select value={market} onChange={e => { setMarket(e.target.value); setResolvedStock(null); setSymbol(""); setStockQuery(""); setRows([]); setTotal(0); }}><option value="ALL">全部市场</option><option value="CN_A">A股</option><option value="HK">港股</option><option value="NEEQ">新三板</option><option value="NEEQ_INNOVATION">创新层</option></select></label>
        <label className="ds-stock-search">股票搜索<input role="combobox" aria-label="股票搜索" aria-autocomplete="list" aria-expanded={stockSearchFocused && !!stockMatches.length} value={stockQuery} placeholder="代码、中文/英文名、拼音或首字母" onFocus={() => setStockSearchFocused(true)} onBlur={() => window.setTimeout(() => setStockSearchFocused(false), 120)} onChange={e => { setStockQuery(e.target.value); setResolvedStock(null); setSymbol(""); setStockSearchFocused(true); }} />{stockSearchFocused && stockMatches.length > 0 && <div className="ds-stock-matches" role="listbox">{stockMatches.map(item => <button type="button" role="option" key={`${item.market}:${item.symbol}`} onMouseDown={event => event.preventDefault()} onClick={() => void selectStock(item)}><strong>{item.name}</strong><span>{item.market} · {item.symbol} · {item.exchange_name || item.exchange}</span></button>)}</div>}</label>
        <label>标题关键词<input value={keyword} onChange={e => setKeyword(e.target.value)} placeholder="例如：计提、定增" /></label>
        <label>文档类型<select value={kind} onChange={e => { setKind(e.target.value); setOffset(0); setSelected([]); }}><option value="NEWS">新闻</option><option value="NOTICE">公告</option><option value="ALL">新闻与公告</option></select></label>
        <button disabled={loading} type="submit">查询</button>
        <button disabled={busy || active || !selected.length} type="button" onClick={() => void planSelected()}>{`批量制定治理方案（已选${selected.length}篇）`}</button>
        <button disabled={busy || active || !selected.length} type="button" onClick={() => void open(selected[0])}>逐篇确认方案与结果</button>
        <button disabled={busy || active || !selected.length} type="button" onClick={() => void analyze(selected)}>执行所选已确认方案</button>
      </form>
      <p>{stockName} · {market} / {symbol} · 共 {total} 份文档。逐篇分析保存独立证据与历史版本；可翻页后继续勾选，当前已选 {selected.length} 篇。</p>
      <div className="ds-toolbar"><button type="button" disabled={busy || active || loading || !rows.length} onClick={() => setSelected(current => [...new Map([...current, ...rows].map(doc => [docKey(doc), doc])).values()])}>全选本页</button><button type="button" disabled={busy || active || loading || !total} onClick={() => void selectAllDocuments()}>{busy ? "处理中…" : `全选当前筛选范围（${total} 篇）`}</button><button type="button" disabled={busy || active || !selected.length} onClick={() => setSelected([])}>清空选择</button><small>全选筛选范围包含所有分页；按当前已查询的股票、文档类型及标题关键词选择。</small></div>
      <div className="ds-table-wrap"><table><thead><tr><th>选择</th><th>新闻／公告</th><th>来源与时间</th><th>正文与治理状态</th><th>语义成果</th><th>操作</th></tr></thead><tbody>
        {rows.map(doc => <tr key={docKey(doc)}><td><input type="checkbox" aria-label={`选择${doc.title}`} checked={selected.some(x => docKey(x) === docKey(doc))} onChange={e => { if (e.target.checked) setSelected(current => [...current, doc]); else setSelected(current => current.filter(x => docKey(x) !== docKey(doc))); }} disabled={busy || active} /></td>
          <td><button className="ds-link" onClick={() => void open(doc)}>{doc.title}</button><small>{doc.source_table === "stock_news" ? "新闻" : "公告"} · 记录 #{doc.record_id}</small></td>
          <td>{doc.source_name}<small>{doc.published_at}</small></td>
          <td><Badge type={doc.status} /><small>{doc.body_available ? `${doc.body_characters} 字正文` : "正文需补取"} · {doc.semantic_complete ? "当前版本已分析" : "待语义分析/重试"}</small>{doc.error && <small className="ds-error">{doc.error}</small>}</td>
          <td>{doc.event_count} 条事件 · {doc.relation_count} 条关系<small>{doc.version_count} 轮治理 · {doc.governance_plan ? `类型：${name(doc.governance_plan.primary_type)}` : "尚未制定方案"}</small><Badge type={doc.workflow_status || "NEEDS_PLAN"} /></td>
          <td><button onClick={() => void open(doc)}>{doc.workflow_status === "NEEDS_PLAN" ? "制定方案" : "进入治理"}</button></td></tr>)}
        {!rows.length && <tr><td colSpan={6}>{loading ? "读取中…" : "当前范围没有新闻公告，请先采集业务数据。"}</td></tr>}
      </tbody></table></div>
      <div className="ds-toolbar"><span>共 {total} 条 · 第 {Math.floor(offset / 20) + 1} / {Math.max(1, Math.ceil(total / 20))} 页</span><button disabled={loading || offset === 0} onClick={() => setOffset(Math.max(0, offset - 20))}>上一页</button><button disabled={loading || offset + 20 >= total} onClick={() => setOffset(offset + 20)}>下一页</button></div>
      </>}
    </section>
    {detail && <div className="ds-governance-backdrop ds-result-backdrop" onMouseDown={event => { if (event.target === event.currentTarget && !busy) setDetail(null); }}><section className="ds-panel ds-result ds-governance-dialog" role="dialog" tabIndex={-1} aria-modal="true" aria-label="新闻公告知识结构化治理窗口">
      {error && <p role="alert" className="ds-error">{error}</p>}
      {job && <section className="ds-document-progress" aria-label="治理任务执行进度"><strong>后台任务 #{job.job_id} · {name(String(job.job_status || job.status))}</strong><GovernanceProgress job={job} /><p>{name(String(job.document_progress?.phase || job.current_stage || "QUEUED"))}{job.document_progress?.current_document ? ` · ${job.document_progress.current_document.title}` : ""}</p>{job.error_message && <p role="alert" className="ds-error">{job.error_message}</p>}</section>}
      <header className="ds-heading"><div><h2>{detail.title}</h2><p>{detail.source_name} · {detail.published_at}</p></div><button onClick={() => setDetail(null)}>关闭治理窗口</button></header>
      {selected.some(doc => docKey(doc) === docKey(detail)) && <nav className="ds-review-actions" aria-label="所选文档导航"><span>已选 {selected.length} 篇 · 当前第 {selected.findIndex(doc => docKey(doc) === docKey(detail)) + 1} 篇</span>
        <button disabled={busy || selected.findIndex(doc => docKey(doc) === docKey(detail)) <= 0} onClick={() => void open(selected[selected.findIndex(doc => docKey(doc) === docKey(detail)) - 1])}>上一篇</button>
        <button disabled={busy || selected.findIndex(doc => docKey(doc) === docKey(detail)) >= selected.length - 1} onClick={() => void open(selected[selected.findIndex(doc => docKey(doc) === docKey(detail)) + 1])}>下一篇</button></nav>}
      <InteractiveSemanticFlow detail={detail} isLatest={isLatest} reviewer={reviewer} onReviewer={value => { setReviewer(value); window.localStorage.setItem("semantic-reviewer", value); }} busy={busy || active} setBusy={setBusy} setError={setError}
        onReload={async () => { await open(detail); await load(); }} onExecute={() => analyze([detail])} onJob={value => { setJob(value); setTaskWindowOpen(false); }} />
      <details className="ds-advanced-review"><summary>展开专业核验、原文与历史版本</summary>
      {!!detail.governance_plan_history?.length && <details className="ds-plan-history"><summary>治理方案历程（{detail.governance_plan_history.length} 次）</summary>{detail.governance_plan_history.map((plan, index) => <article className="ds-card" key={`${plan.plan_id}:${plan.revision}:${index}`}><h4>第 {index + 1} 次方案 · {name(plan.status)}</h4><p>{name(plan.primary_type)} · {plan.overview}</p><p>判断依据：{plan.rationale}</p>{plan.confirmed_by && <p>确认人：{plan.confirmed_by} · {plan.confirmed_at}</p>}</article>)}</details>}
      <div className="ds-toolbar"><label>治理版本<select aria-label="治理版本" value={detail.selected_version} onChange={e => void open(detail, Number(e.target.value))}>{detail.versions.map(v => <option value={v.index} key={v.index}>第 {v.index + 1} 轮 · {v.semantic_schema_version || "旧事件摘要"} · {name(v.status)} · v{v.skill_version}</option>)}</select></label><Badge type={detail.human_review?.overall || "NOT_REVIEWED"} /><span>{detail.state.content_scope === "BODY_EXCERPT" ? "仅正文摘录，全文未完成" : "所选版本正文与证据可核验"}</span></div>
      <div className="ds-review-summary"><div><strong>人机审核门禁</strong><p>人工确认只确认当前抽取结构；候选事实仍需公告、监管文件等独立来源交叉核验后才能发布为已验证事实。</p></div><div className="ds-toolbar"><button disabled={busy || !analysis} onClick={() => chooseReview(documentTarget)}>审核整篇</button><button disabled={busy || !analysis} onClick={() => { chooseReview(documentTarget); setTab("review"); }}>查看审核历程（{detail.human_review?.review_count || 0}）</button></div></div>
      {detail.state.error && <p className="ds-error">{detail.state.error}</p>}
      <p className="ds-note">{detail.note}</p>
      <div className="ds-tabs" role="tablist" aria-label="语义成果分类">{[["reading", "全文理解与影响"], ["events", "事件与数字"], ["relations", "主体与关键联系"], ["timeline", "事件时间线"], ["source", "原文与证据"], ["review", "人工审核与版本对比"], ["audit", "执行与版本"]].map(([key, text]) => <button key={key} role="tab" aria-selected={tab === key} className={tab === key ? "active" : ""} onClick={() => setTab(key)}>{text}</button>)}</div>
      {reviewTarget && <section className="ds-review-editor" aria-label="人工审核编辑器">
        <header><div><h3>人工审核：{reviewTarget.label}</h3><p>目标：{name(reviewTarget.type)} · 当前版本第 {detail.selected_version + 1} 轮 · {isLatest ? "可修改当前版本" : "历史版本仅可批注、确认或驳回"}</p></div><button type="button" onClick={() => setReviewTarget(null)}>关闭审核面板</button></header>
        <div className="ds-review-columns"><div><h4>结构化内容</h4><p className="ds-review-value">{reviewTarget.values && Object.entries(reviewTarget.values).filter(([key]) => !["fact_id", "evidence_quotes", "impacts", "metrics", "participant_keys"].includes(key)).map(([key, value]) => <span key={key}><b>{fieldNames[key] || key}</b>：{Array.isArray(value) ? value.join("、") : String(value ?? "未提供")}<br /></span>)}</p></div><div><h4>原文对照</h4><SourceContext body={detail.body} quote={reviewTarget.evidence} /></div></div>
        <label className="ds-review-label">审核人<input value={reviewer} onChange={event => setReviewer(event.target.value)} /></label>
        {reviewTarget.type !== "DOCUMENT" && isLatest && <div className="ds-review-edit"><label>修改字段<select value={editField} onChange={event => changeField(event.target.value)}>{reviewFields[reviewTarget.type].map(field => <option key={field} value={field}>{fieldNames[field] || field}</option>)}</select></label><EditControl field={editField} value={editValue} onChange={setEditValue} /></div>}
        <label className="ds-review-label">审核意见<textarea rows={3} value={reviewReason} onChange={event => setReviewReason(event.target.value)} placeholder="请说明核验依据、错误原因或需要模型补充的内容" /></label>
        <div className="ds-review-actions"><button disabled={busy || !reviewReason.trim()} onClick={() => void review("COMMENT")}>保存批注</button><button disabled={busy || !reviewReason.trim()} onClick={() => void review("CONFIRM")}>确认当前结构</button><button disabled={busy || !reviewReason.trim()} onClick={() => void review("REQUEST_CORRECTION")}>标记需要修正</button><button disabled={busy || !reviewReason.trim()} onClick={() => void review("REJECT")}>驳回目标</button>{reviewTarget.type !== "DOCUMENT" && isLatest && <button className="ds-primary" disabled={busy || !reviewReason.trim() || !editField} onClick={() => void review("MODIFY")}>保存人工修改</button>}{reviewTarget.type === "DOCUMENT" && <button className="ds-primary" disabled={busy || !reviewReason.trim() || !isLatest} onClick={() => void review("REANALYZE")}>按意见重新分析（可持续修正）</button>}</div>
      </section>}
      {!analysis && tab !== "source" && tab !== "audit" && <p>所选版本尚无完整语义成果。旧事件摘要与报告仍保留；点击“执行当前版本语义治理”补充分析，失败可查看原因并重试。</p>}
      {analysis && tab === "reading" && <>
        <article className="ds-overview"><h3>全文概览</h3><p>{analysis.reading.overview}</p></article>
        <div className="ds-topic-grid">{analysis.reading.sections.map((section, i) => <article className="ds-card" key={i}><h3>{section.topic}</h3><p>{section.summary}</p>{section.evidence_quotes.map((quote, q) => <Quote text={quote} key={q} />)}</article>)}</div>
        <h3>分维度影响与依据</h3><div className="ds-impact-grid">{analysis.events.flatMap(event => event.impacts.map((impact, i) => <article className={`ds-card ds-impact-${impact.direction.toLowerCase()}`} key={`${event.key}:${i}`}><h4>{name(impact.dimension)} · <Badge type={impact.direction} /></h4><small>{event.event_type}</small><p>{impact.rationale}</p><Badge type={impact.assertion_type} /><p>归属：{impact.attribution}</p><p className="ds-muted">边界：{impact.uncertainty}</p><Quote text={impact.evidence_quote} /><ReviewButton onOpen={chooseReview} target={{ type: "IMPACT", label: `${event.event_type} / ${name(impact.dimension)}`, eventKey: event.key, itemIndex: i, evidence: impact.evidence_quote, values: impact as unknown as Record<string, unknown> }} /></article>))}</div>
        <h3>不确定性与待核验事项</h3><ul>{[...new Set([...analysis.reading.uncertainties, ...analysis.uncertainties])].map((item, i) => <li key={i}>{item}</li>)}</ul>
      </>}
      {analysis && tab === "events" && <div className="ds-event-list">{analysis.events.map(event => <article className="ds-card" key={event.key}><header><h3>{event.event_type}</h3><div><Badge type={event.assertion_type} /> <ReviewButton onOpen={chooseReview} target={{ type: "EVENT", label: event.event_type, key: event.key, evidence: event.evidence_quotes[0], values: { ...event, evidence_quote: event.evidence_quotes[0] } }} /></div></header><p>{event.summary}</p><p className="ds-muted">归属：{event.attribution} · 原文时间：{event.event_time_raw || "未明确"} · {event.fact_id ? "已存入候选事实" : "待存储"}</p>
        {event.metrics.length > 0 && <div className="ds-table-wrap"><table><thead><tr><th>指标</th><th>原文值</th><th>性质／期间</th><th>标准值</th><th>证据与审核</th></tr></thead><tbody>{event.metrics.map((metric, i) => <tr key={i}><td>{metric.name}</td><td>{metric.raw_value} {metric.unit === "%" && metric.raw_value.includes("分之") ? "" : metric.unit}</td><td>{name(metric.qualifier)}<small>{metric.period}</small></td><td>{metric.normalized_value} {metric.normalized_unit}</td><td><Quote text={metric.evidence_quote} /><ReviewButton onOpen={chooseReview} target={{ type: "METRIC", label: `${event.event_type} / ${metric.name}`, eventKey: event.key, itemIndex: i, evidence: metric.evidence_quote, values: metric as unknown as Record<string, unknown> }} /></td></tr>)}</tbody></table></div>}
        {event.impacts.map((impact, i) => <div className="ds-inline-review" key={i}><p>{name(impact.dimension)}：<Badge type={impact.direction} /> {impact.rationale}</p><ReviewButton onOpen={chooseReview} target={{ type: "IMPACT", label: `${event.event_type} / ${name(impact.dimension)}`, eventKey: event.key, itemIndex: i, evidence: impact.evidence_quote, values: impact as unknown as Record<string, unknown> }} /></div>)}
        {event.evidence_quotes.map((quote, i) => <Quote key={i} text={quote} />)}
      </article>)}</div>}
      {analysis && tab === "relations" && <><h3>参与主体与身份</h3><div className="ds-topic-grid">{analysis.participants.map(p => <article className="ds-card" key={p.key}><h3>{p.name}</h3><p>{p.role}</p><p>别名：{p.aliases.join("、") || "无"}</p><Badge type={p.resolution_status} />{p.entity_id && <small>主体编号：{p.entity_id}</small>}<Quote text={p.evidence_quote} /><ReviewButton onOpen={chooseReview} target={{ type: "PARTICIPANT", label: p.name, key: p.key, evidence: p.evidence_quote, values: p as unknown as Record<string, unknown> }} /></article>)}</div><h3>关键联系 · 均为有证据候选</h3><div className="ds-table-wrap"><table><thead><tr><th>起点</th><th>联系</th><th>终点</th><th>缘由与归属</th><th>原文证据与审核</th></tr></thead><tbody>{analysis.relations.map((link, i) => <tr key={i}><td>{titles[link.source_key]}</td><td>{name(link.predicate)}</td><td>{titles[link.target_key]}</td><td>{link.explanation}<p><Badge type={link.assertion_type} /> {link.attribution}</p></td><td><Quote text={link.evidence_quote} /><ReviewButton onOpen={chooseReview} target={{ type: "RELATION", label: `${titles[link.source_key]} → ${name(link.predicate)} → ${titles[link.target_key]}`, key: link.fact_id || `${link.source_key}->${link.target_key}`, evidence: link.evidence_quote, values: link as unknown as Record<string, unknown> }} /></td></tr>)}</tbody></table></div></>}
      {analysis && tab === "timeline" && <ol className="ds-timeline">{[...analysis.events].sort((a,b) => (a.event_date || "9999").localeCompare(b.event_date || "9999")).map(event => <li key={event.key}><time>{event.event_time_raw || "原文未明确日期"}</time><div><h3>{event.event_type}</h3><Badge type={event.assertion_type} /><p>{event.summary}</p>{event.evidence_quotes.map((q,i) => <Quote text={q} key={i} />)}</div></li>)}</ol>}
      {tab === "review" && <section className="ds-review-history"><h3>人工审核与版本对比</h3><p>审核状态：<Badge type={detail.human_review?.overall || "NOT_REVIEWED"} /> · 已记录 {detail.human_review?.review_count || 0} 次动作。人工确认仅代表结构审核，不等于外部事实核验。</p><div className="ds-version-grid">{detail.versions.map(version => <article key={version.index} className={version.index === detail.selected_version ? "active" : ""}><h4>第 {version.index + 1} 轮 {version.semantic_schema_version || "旧事件摘要"}</h4><p>{name(version.status)} · Skill {version.skill_version}</p><p>{version.observed_at}</p><div className="ds-version-actions"><button onClick={() => void open(detail, version.index)}>查看此版本</button>{version.index !== detail.selected_version && <button onClick={() => { setCompareFrom(version.index); setCompareTo(detail.selected_version); void compareVersions(version.index, detail.selected_version); }}>与当前所选版本比较</button>}</div></article>)}</div>
        {detail.versions.length > 1 && <div className="ds-compare-toolbar"><label>基准版本<select value={compareFrom} onChange={event => setCompareFrom(Number(event.target.value))}>{detail.versions.map(version => <option key={version.index} value={version.index}>第 {version.index + 1} 轮</option>)}</select></label><span>对比</span><label>目标版本<select value={compareTo} onChange={event => setCompareTo(Number(event.target.value))}>{detail.versions.map(version => <option key={version.index} value={version.index}>第 {version.index + 1} 轮</option>)}</select></label><button disabled={busy || compareFrom === compareTo} onClick={() => void compareVersions()}>生成字段级差异</button></div>}
        {comparison && <section className="ds-comparison"><header><div><h3>第 {comparison.from.index + 1} 轮 → 第 {comparison.to.index + 1} 轮</h3><p>{comparison.changed ? "以下仅显示发生变化的结构化字段；事实编号变化另计入血缘。" : "两个版本的语义结构没有差异。"}</p></div><Badge type={comparison.changed ? "NEEDS_CORRECTION" : "HUMAN_CONFIRMED"} /></header>{comparison.reading.fields.map(change => <div className="ds-diff-modified" key={change.field}><strong>全文概览</strong><p><del>{displayValue(change.from)}</del><ins>{displayValue(change.to)}</ins></p></div>)}<div className="ds-diff-grid">{Object.entries(diffNames).map(([key, label]) => <DiffBlock key={key} title={label} diff={key === "sections" ? comparison.reading.sections : comparison[key as keyof Pick<VersionComparison, "events" | "participants" | "relations" | "metrics" | "impacts">]} />)}</div></section>}
        <h3>全部版本审核历程</h3><div className="ds-table-wrap"><table><thead><tr><th>时间</th><th>版本／轮次</th><th>对象</th><th>动作</th><th>审核人</th><th>前值 → 新值</th><th>意见</th></tr></thead><tbody>{(detail.review_history || detail.human_review?.history || []).map(item => <tr key={item.id}><td>{item.created_at}</td><td>第 {(item.version_index ?? 0) + 1} 版／第 {item.review_round} 次</td><td>{name(item.target_type)} {item.target_key || item.event_key || "整篇"}</td><td><Badge type={item.action} /> · {name(item.resulting_status)}</td><td>{item.reviewer}</td><td>{item.field ? `${fieldNames[item.field] || item.field}：${displayValue(item.old_value)} → ${displayValue(item.new_value)}` : "—"}</td><td>{item.reason}</td></tr>)}</tbody></table></div></section>}
      {tab === "source" && <><h3>所选版本原文</h3><p>{detail.body.length} 字 · 证据编号：{detail.state.evidence_id || "未生成"}</p>{detail.url && <a href={detail.url} target="_blank" rel="noreferrer">打开原始来源</a>}<div className="ds-source">{detail.body || "正文未获取，分析时将尝试补取已登记来源。"}</div></>}
      {tab === "audit" && <><h3>执行与来源追溯</h3><dl className="ds-audit"><dt>治理时间</dt><dd>{detail.state.observed_at || "未执行"}</dd><dt>流水线编号</dt><dd>{detail.state.pipeline_run_id || "未执行"}</dd><dt>智能体执行编号</dt><dd>{detail.state.agent_execution_run_id || "未执行"}</dd><dt>Skill执行编号</dt><dd>{detail.state.skill_execution_run_id || "未执行"}</dd><dt>正文哈希</dt><dd>{detail.state.content_hash || "未生成"}</dd></dl>{analysis?.model_calls.map(call => <p key={call.id}>{call.phase === "SEMANTIC_READING" ? "全文语义理解" : "结构化抽取"} · 真实模型调用 #{call.id} · {call.model_version}</p>)}<details><summary>查看完整治理契约与原始审计</summary><pre>{JSON.stringify(detail.state, null, 2)}</pre></details></>}
      </details>
    </section></div>}
  </div>;
}
