import { useEffect, useRef, useState, type FormEvent, type ReactNode } from "react";
import { Wrench } from "lucide-react";
import { api, type KnowledgeBase, type KnowledgeGraph, type ModelInstance, type ModelProvider, type StockSymbol } from "./api";
import { SelectionAnalysis, type AnalysisDimension } from "./SelectionAnalysis";
import { selectionApi, type RetrospectiveRepairFlow, type SelectionAudit, type SelectionCandidate, type SelectionEvidence, type SelectionRecord, type SelectionRetrospective, type SelectionRun, type SelectionScoringProfile, type SelectionTracking } from "./selectionApi";
import "./SelectionWatch.css";

const labels: Record<string, string> = {
  PENDING: "待复选",
  PENDING_REVIEW: "待人工审核",
  REVIEW: "待人工复选",
  PARTIAL: "部分完成 · 请核对",
  MODEL_PARTIAL: "模型部分完成",
  QUEUED: "已排队",
  PENDING_DATA: "等待行情",
  LOCAL_EVIDENCE: "本地规则预选",
  MODEL_REQUESTED: "模型分析中",
  MODEL_COMPLETED: "模型综合分析",
  MODEL_FAILED_FALLBACK_LOCAL: "模型失败 · 本地多源证据",
  DATA_INSUFFICIENT: "数据不足 · 未调用模型",
  MODEL_UNAVAILABLE: "真实模型不可用",
  NOT_RUN: "未调用模型",
  NOT_AVAILABLE: "暂不可用",
  NOT_REQUESTED: "未申请",
  NOT_ELIGIBLE: "不满足修复条件",
  NOT_NEEDED: "无需修复",
  SKILL_VERSION_MISMATCH: "Skill 版本不一致",
  PASSED: "评测通过",
  APPROVED: "已入选",
  REJECTED: "已排除",
  COMPLETED: "已完成",
  FAILED: "执行失败",
  RUNNING: "生成中",
  TRACKING: "跟踪中",
  WAITING_DATA: "等待行情",
  REVIEWED: "已复盘",
  REAL: "真实数据",
  MOCK: "模拟数据",
  RULES_ONLY: "规则预选",
  MODEL: "模型综合分析",
  LLM: "模型综合分析",
  CN_A: "A 股",
  HK: "港股",
  NEEQ: "新三板",
  NEEQ_INNOVATION: "创新层",
  SOURCE: "来源记录",
  OBSERVED: "来源记录",
  NORMALIZED: "已标准化",
  CANDIDATE: "候选待核实",
  VERIFIED: "已验证",
  ACCEPTED: "已审核",
  SYSTEM: "系统规则",
  RULE: "确定性规则",
  KEYWORD_MENTION: "关键词提及",
  ISSUED_BY: "发行主体",
  DESCRIBED_BY: "资料描述",
  HAS_RECORD: "包含记录",
  HAS_DOCUMENT: "关联文档",
  HAS_NEWS: "相关新闻",
  HAS_NEWS_CANDIDATE: "新闻候选关联（待核实）",
  HAS_NOTICE: "相关公告",
  HAS_FINANCIAL_REPORT: "相关财报",
  HAS_FINANCIAL_METRIC: "财务指标",
  HAS_PRICE_AND_VOLUME_SERIES: "量价序列",
  HAS_REALTIME_QUOTE: "实时行情",
  HAS_F10_SECTION: "F10 资料",
  HAS_RESEARCH_REPORT: "研究报告",
  HAS_MARKET_ANOMALY: "量价异常",
  HAS_RECORDED_FACT: "关联事实",
  REFERS_TO: "指向对象",
  MENTIONS_POLICY: "提及政策",
  MENTIONS_RAW_MATERIAL: "提及原材料",
  MENTIONS_SUPPLY_CHAIN: "提及供应链",
  MENTIONS_SHAREHOLDER: "提及股东事项",
  MENTIONS_CONTRACT: "提及合同",
  SUPPORTED_BY: "证据支持",
  EVIDENCED_BY: "证据来源",
};
const nameOf = (value: string) => labels[value] || value;
const numberOf = (value: unknown, digits = 2) => typeof value === "number" && Number.isFinite(value) ? value.toFixed(digits) : "—";
const pct = (value: unknown) => value == null ? "—" : `${numberOf(value)}%`;
const dateOf = (value: string | null) => value ? new Date(value).toLocaleString("zh-CN") : "—";
const elapsed = (value: unknown) => {
  const seconds = Math.max(0, Math.floor(Number(value) || 0));
  return seconds < 60 ? `${seconds} 秒` : `${Math.floor(seconds / 60)} 分 ${seconds % 60} 秒`;
};
const liveElapsed = (progress: SelectionRecord, _tick: number) => {
  const stored = Math.max(0, Math.floor(Number(progress.elapsed_seconds) || 0));
  const started = progress.activity_started_at || progress.started_at;
  const stamp = started ? new Date(String(started)).getTime() : NaN;
  return Number.isFinite(stamp) ? Math.max(stored, Math.floor((Date.now() - stamp) / 1000)) : stored;
};
const heartbeatAge = (value: unknown) => {
  if (!value) return null;
  const date = new Date(String(value));
  return Number.isFinite(date.getTime()) ? Math.max(0, Math.floor((Date.now() - date.getTime()) / 1000)) : null;
};
const strings = (value: unknown): string[] => Array.isArray(value) ? value.filter((item): item is string => typeof item === "string") : [];
const records = (value: unknown): SelectionRecord[] => Array.isArray(value) ? value.filter((item): item is SelectionRecord => !!item && typeof item === "object") : [];
const attemptsNewestFirst = (value: unknown) => [...records(value)].sort((left, right) => {
  const toTime = (record: SelectionRecord) => {
    const raw = textOf(record.at).trim();
    if (!raw) return Number.NEGATIVE_INFINITY;
    const normalized = /(?:Z|[+-]\d{2}:?\d{2})$/.test(raw) ? raw : `${raw}Z`;
    const parsed = Date.parse(normalized);
    return Number.isFinite(parsed) ? parsed : Number.NEGATIVE_INFINITY;
  };
  return toTime(right) - toTime(left);
});
const sourceLabelFallback: Record<string, string> = {
  stock_news: "新闻", stock_notice: "公告", stock_context_event: "外部事件线索", stock_financial_report: "财务报告",
  stock_kline: "历史量价", stock_realtime_quote: "实时行情", stock_f10_cache: "F10资料", research_report: "研究报告",
};
function sourceCountSummary(value: unknown, labelsValue: unknown): string {
  if (!value || typeof value !== "object") return "暂无来源明细";
  const sourceLabels = labelsValue && typeof labelsValue === "object" ? labelsValue as Record<string, string> : {};
  const entries = Object.entries(value as Record<string, unknown>).filter(([, count]) => Number(count) > 0);
  return entries.length ? entries.map(([source, count]) => `${sourceLabels[source] || sourceLabelFallback[source] || source} ${Number(count).toLocaleString()}`).join(" · ") : "暂无命中来源";
}
function relationCountSummary(value: unknown): string {
  if (!value || typeof value !== "object") return "暂无直接关系";
  const entries = Object.entries(value as Record<string, unknown>).filter(([, count]) => Number(count) > 0);
  return entries.length ? entries.map(([type, count]) => `${nameOf(type)} ${Number(count).toLocaleString()}`).join(" · ") : "暂无直接关系";
}
const textOf = (value: unknown) => typeof value === "string" || typeof value === "number" ? String(value) : "";
const recordLabel = (record: SelectionRecord, keys: string[], fallback: string) => keys.map(key => textOf(record[key])).find(Boolean) || fallback;
const recordDetail = (record: SelectionRecord, keys: string[]) => keys.map(key => textOf(record[key])).filter(Boolean).join(" · ");
const boolOf = (value: unknown) => value == null ? "--" : value === true ? "是" : value === false ? "否" : textOf(value);
const jsonPreview = (value: unknown, limit = 1200) => {
  if (value == null) return "";
  const text: string = typeof value === "string" ? value : JSON.stringify(value, null, 2) ?? String(value);
  return text.length > limit ? `${text.slice(0, limit)}…` : text;
};
const errorOf = (error: unknown) => error instanceof Error ? error.message : "请求失败，请重试";
const modelConnectivityLabel = (item: ModelInstance) => item.availability_label || ({
  AVAILABLE: "可用", DEGRADED: "暂时不可用，可重试", BILLING: "欠费/额度不足", UNAVAILABLE: "不可用", NOT_CONFIGURED: "未配置",
  DISABLED: "已停用", CHECKING: "检查中", UNKNOWN: "未检查",
} as Record<string, string>)[item.availability_status || "UNKNOWN"] || "未检查";
const modelStatusLabel = (item: ModelInstance) => item.availability_status === "AVAILABLE"
  ? `${modelConnectivityLabel(item)} · ${item.selection_contract_label || "未验证选股契约"}`
  : modelConnectivityLabel(item);
const modelStatusReason = (item: ModelInstance) => item.selection_contract_status === "FAILED" || item.selection_contract_status === "PARTIAL"
  ? item.selection_contract_reason || item.availability_reason
  : item.availability_reason;
const modelIsSelectable = (item: ModelInstance) => item.enabled && item.api_key_configured
  && ["AVAILABLE", "DEGRADED", "UNKNOWN", "CHECKING"].includes(item.availability_status || "UNKNOWN");
const modelStatusClass = (item: ModelInstance) => modelIsSelectable(item) ? "model-option-available" : "model-option-unavailable";
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

function LegacyEvidence({ candidate }: { candidate: SelectionCandidate }) {
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

function EvidenceVersionStrip({ evidence }: { evidence: SelectionEvidence }) {
  const items = ([
    ["证据时点", evidence.as_of],
    ["数据截止", evidence.data_cutoff],
    ["上下文版本", evidence.context_version],
    ["图谱版本", evidence.graph_version],
    ["湖仓版本", evidence.lakehouse_version],
    ["文档版本", evidence.document_version],
    ["Skill 版本", evidence.skill_version],
    ["模型版本", evidence.model_version],
    ["上下文哈希", evidence.context_hash],
  ] as Array<[string, unknown]>).filter((item): item is [string, string] => typeof item[1] === "string" && item[1].length > 0);
  if (!items.length) return null;
  return <section className="selection-evidence-snapshot"><h3>证据与版本快照</h3><div className="selection-evidence-meta">{items.map(([label, value]) => <span key={label}><small>{label}</small><strong title={value}>{value}</strong></span>)}</div></section>;
}

function IdentityEvidence({ identity }: { identity: SelectionRecord | undefined }) {
  if (!identity) return null;
  const security = identity.security as SelectionRecord | undefined;
  const company = identity.company as SelectionRecord | undefined;
  const fields: Array<[string, unknown]> = [
    ["证券规范标识", identity.canonical_security_id],
    ["公司规范标识", identity.canonical_company_id],
    ["映射状态", identity.mapping_status],
    ["身份版本", identity.identity_version],
    ["证券主数据 ID", identity.security_id || identity.stock_symbol_id],
    ["公司主数据 ID", identity.company_id],
    ["上市关系 ID", identity.listing_id],
  ].filter((item): item is [string, string | number] => item[1] !== null && item[1] !== undefined && String(item[1]).length > 0);
  return <section className="selection-evidence-block"><h3>股票与公司身份</h3><div className="selection-identity-grid">{fields.map(([label, value]) => <div key={label}><small>{label}</small><strong>{String(value)}</strong></div>)}</div>{security && <details className="selection-evidence"><summary>证券主数据</summary><p>{recordDetail(security, ["name", "market", "symbol", "exchange", "asset_type", "status", "list_date"])}</p></details>}{company && <details className="selection-evidence"><summary>公司主数据</summary><p>{recordDetail(company, ["name", "entity_type", "jurisdiction", "identifier_scheme", "identifier_value", "id"])}</p>{company.properties && <pre>{jsonPreview(company.properties)}</pre>}</details>}</section>;
}

function StructuredEvidence({ evidence }: { evidence: SelectionEvidence }) {
  const facts = records(evidence.facts);
  const grouped: SelectionRecord[] = Object.entries(evidence.structured_observations || {}).flatMap(([category, values]) => records(values).map(item => ({ ...item, _category: category })));
  const rows: SelectionRecord[] = [...facts, ...grouped.filter(item => !facts.some(fact => String(fact.id || "") === String(item.id || "")))];
  if (!rows.length) return null;
  return <section className="selection-evidence-block"><h3>结构化事实与观测 · {rows.length}</h3><div className="selection-fact-grid">{rows.slice(0, 60).map((fact, index) => <article className="selection-evidence selection-fact" key={`${textOf(fact.id)}-${index}`}><strong>{recordLabel(fact, ["title", "fact_type", "_category", "indicator", "name"], "结构化观测")}</strong><small>{recordDetail(fact, ["source_table", "source_record_id", "observed_at", "report_period", "status"])}</small>{fact.properties && <pre>{jsonPreview(fact.properties)}</pre>}{fact.evidence && <small>证据引用：{jsonPreview(fact.evidence, 360)}</small>}</article>)}</div></section>;
}

function ChunkEvidence({ chunks, coverage }: { chunks: SelectionRecord[] | undefined; coverage: SelectionRecord | undefined }) {
  const rows = chunks || [];
  const requested = textOf(coverage?.documents_requested);
  const ready = textOf(coverage?.documents_with_chunks);
  const included = textOf(coverage?.documents_included);
  const coverageText = coverage
    ? Number(coverage.documents_requested || 0) > 0
      ? `${ready}/${requested} 篇文档有有效切片，${included}/${requested} 篇已载入本次上下文`
      : "当前范围没有知识文档"
    : "";
  if (!rows.length && !coverage) return null;
  return <section className="selection-evidence-block"><h3>知识切片 · {rows.length}{coverageText ? ` · ${coverageText}` : ""}</h3>{rows.slice(0, 40).map((chunk, index) => <details className="selection-evidence" key={`${textOf(chunk.id)}-${index}`}><summary>{recordLabel(chunk, ["section_title", "title", "chunk_id"], `切片 ${index + 1}`)} · {recordLabel(chunk, ["embedding_status"], "未声明向量")}</summary><small>{recordDetail(chunk, ["document_id", "chunk_version", "parser_version", "embedding_model", "embedding_quality", "start_offset", "end_offset"])}</small><p>{recordLabel(chunk, ["text", "content", "text_preview", "excerpt"], "暂无切片文本")}</p></details>)}</section>;
}

function GraphPathEvidence({ paths }: { paths: SelectionRecord[] | undefined }) {
  if (!paths?.length) return null;
  return <section className="selection-evidence-block"><h3>图谱路径 · {paths.length}</h3>{paths.slice(0, 40).map((path, index) => {
    const predicate = recordLabel(path, ["predicate", "relation", "type"], "关联");
    const details = [
      path.evidence_document_id != null ? `证据文档：${textOf(path.evidence_document_id)}` : "",
      textOf(path.verification_status) ? `核验状态：${nameOf(textOf(path.verification_status))}` : "",
      path.confidence != null ? `置信度：${textOf(path.confidence)}` : "",
      textOf(path.extraction_method) ? `形成方式：${nameOf(textOf(path.extraction_method))}` : "",
    ].filter(Boolean).join(" · ");
    return <article className="selection-evidence" key={`${textOf(path.id)}-${index}`}><strong>{recordLabel(path, ["subject_name", "subject", "source"], `节点 ${index + 1}`)} → {nameOf(predicate)} → {recordLabel(path, ["object_name", "object", "target"], "目标节点")}</strong><small>{details || "当前关系未声明独立证据元数据"}</small><p>{recordLabel(path, ["evidence_excerpt", "evidence_text", "excerpt"], "暂无路径证据摘录")}</p></article>;
  })}</section>;
}

function LineageEvidence({ lineage, datasets }: { lineage: SelectionRecord[] | undefined; datasets: SelectionRecord[] | undefined }) {
  if ((!lineage || !lineage.length) && (!datasets || !datasets.length)) return null;
  return <section className="selection-evidence-block"><h3>湖仓版本与数据血缘</h3>{datasets?.slice(0, 30).map((dataset, index) => <article className="selection-evidence" key={`dataset-${textOf(dataset.dataset_id)}-${index}`}><strong>{recordLabel(dataset, ["dataset_code", "dataset_name"], "数据集")}</strong><small>{recordDetail(dataset, ["layer", "format", "version", "version_id", "object_id", "row_count"])}</small>{dataset.quality && <p>质量：{jsonPreview(dataset.quality, 500)}</p>}</article>)}{lineage?.slice(0, 60).map((row, index) => <article className="selection-evidence" key={`lineage-${textOf(row.id)}-${index}`}><strong>{recordLabel(row, ["upstream_type"], "上游")}:{recordLabel(row, ["upstream_id"], "--")} → {recordLabel(row, ["transformation"], "转换")} → {recordLabel(row, ["downstream_type"], "下游")}:{recordLabel(row, ["downstream_id"], "--")}</strong><small>{recordDetail(row, ["dataset_version", "parser_version", "batch_id", "created_at"])}</small>{row.metadata && <p>{jsonPreview(row.metadata, 500)}</p>}</article>)}</section>;
}

function Evidence({ candidate }: { candidate: SelectionCandidate }) {
  const evidence = candidate.evidence_json;
  return <><EvidenceVersionStrip evidence={evidence} /><IdentityEvidence identity={evidence.identity} /><StructuredEvidence evidence={evidence} /><ChunkEvidence chunks={evidence.chunks} coverage={evidence.chunk_coverage} /><GraphPathEvidence paths={evidence.graph_paths} /><LineageEvidence lineage={evidence.lineage} datasets={evidence.lakehouse_datasets} /><LegacyEvidence candidate={candidate} /></>;
}

function SkillRepairAction({ retrospective }: { retrospective: SelectionRetrospective }) {
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState<RetrospectiveRepairFlow | null>(null);
  const [error, setError] = useState("");
  if (!retrospective.error_tags_json?.length) return null;
  async function runRepair() {
    setBusy(true);
    setError("");
    try {
      setResult(await selectionApi.repairRetrospective(retrospective.id));
    } catch (nextError) {
      setError(errorOf(nextError));
    } finally {
      setBusy(false);
    }
  }
  return <div className="selection-repair-action">
    <button type="button" disabled={busy} onClick={() => void runRepair()} title="将本次复盘加入 Skill 回归评测">
      <Wrench size={16} aria-hidden="true" />{busy ? "正在评测…" : result ? "重新运行 Skill 回归" : "运行 Skill 回归"}
    </button>
    {result && <small>
      用例 #{result.case_id} · 评测 {nameOf(result.evaluation_status)} · 修复 {nameOf(result.repair_status)}
      {result.repair_draft_id ? ` · 草案 #${result.repair_draft_id} 待人工审核` : ""}
      {result.message ? ` · ${result.message}` : ""}
    </small>}
    {error && <small className="form-error">{error}</small>}
  </div>;
}

function AuditTrail({ audit, busy, error }: { audit: SelectionAudit | null; busy: boolean; error: string }) {
  if (busy) return <p className="selection-help">正在读取不可变决策快照与复盘版本…</p>;
  if (error) return <p className="form-error">审计记录读取失败：{error}</p>;
  if (!audit) return <p className="selection-help">尚未读取决策审计记录。</p>;
  return <section className="selection-evidence-block"><h3>决策快照与复盘审计</h3>{audit.snapshots.length > 0 ? audit.snapshots.map(snapshot => <details className="selection-evidence" key={snapshot.id}><summary>{snapshot.snapshot_kind} · {nameOf(snapshot.decision)} · {dateOf(snapshot.as_of)}</summary><div className="selection-audit-meta"><span>图谱：{snapshot.graph_version || "--"}</span><span>数据：{snapshot.data_version || "--"}</span><span>Skill：{snapshot.skill_code || "--"} / {snapshot.skill_version || "--"}</span><span>模型：{snapshot.model_instance_code || "--"}</span><span>哈希：{snapshot.payload_hash || "--"}</span></div>{snapshot.evidence_json && <pre>{jsonPreview({ counts: snapshot.evidence_json.counts, context_hash: snapshot.evidence_json.context_hash, evidence_fact_ids: snapshot.evidence_json.evidence_fact_ids, document_ids: snapshot.evidence_json.document_ids }, 1800)}</pre>}</details>) : <p className="selection-help">暂无不可变决策快照。</p>}{audit.retrospectives.length > 0 && <><h4>复盘修订 · {audit.retrospectives.length}</h4>{audit.retrospectives.map(row => <article className="selection-evidence" key={row.id}><strong>第 {row.revision} 版 · {nameOf(row.status)}</strong><small>{dateOf(row.evaluated_at)} · {row.observed_sessions} 个交易日 · 结果 {pct(row.return_pct)} · 回撤 {pct(row.max_drawdown_pct)}</small><p>{row.summary || "暂无复盘摘要"}</p>{row.error_tags_json?.length ? <small>误差标签：{row.error_tags_json.join("、")}</small> : null}<SkillRepairAction retrospective={row} /></article>)}</>}</section>;
}

function RetrospectiveTrail({ rows, error }: { rows: SelectionRetrospective[]; error: string }) {
  return <section className="selection-evidence-block"><h3>复盘修订记录 · {rows.length}</h3>{error && <p className="form-error">复盘审计读取失败：{error}</p>}{rows.length ? rows.map(row => <article className="selection-evidence" key={row.id}><strong>第 {row.revision} 版 · {nameOf(row.status)}</strong><small>{dateOf(row.evaluated_at)} · {row.observed_sessions} 个交易日 · 收益 {pct(row.return_pct)} · 回撤 {pct(row.max_drawdown_pct)}</small><p>{row.summary || "暂无复盘摘要"}</p>{row.attribution_json && <details><summary>归因与版本</summary><pre>{jsonPreview(row.attribution_json, 1800)}</pre></details>}{row.error_tags_json?.length ? <small>误差标签：{row.error_tags_json.join("、")}</small> : null}<SkillRepairAction retrospective={row} /></article>) : !error && <p className="selection-help">当前还没有可展示的复盘修订。</p>}</section>;
}

export function SelectionWorkbench({ mode, initialCreate = false, initialModelCode = "", onCreated }: { mode: "selection" | "tracking"; initialCreate?: boolean; initialModelCode?: string; onCreated?: (run: SelectionRun) => void }) {
  const [runs, setRuns] = useState<SelectionRun[]>([]);
  const [run, setRun] = useState<SelectionRun | null>(null);
  const [tracking, setTracking] = useState<SelectionTracking[]>([]);
  const [bases, setBases] = useState<KnowledgeBase[]>([]);
  const [graphs, setGraphs] = useState<KnowledgeGraph[]>([]);
  const [instances, setInstances] = useState<ModelInstance[]>([]);
  const [providers, setProviders] = useState<ModelProvider[]>([]);
  const [scoringProfiles, setScoringProfiles] = useState<SelectionScoringProfile[]>([]);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadVersion, setLoadVersion] = useState(0);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [createOpen, setCreateOpen] = useState(initialCreate);
  const [stockQuery, setStockQuery] = useState("");
  const [stockMatches, setStockMatches] = useState<StockSymbol[]>([]);
  const [chosenStocks, setChosenStocks] = useState<StockSymbol[]>([]);
  const [searchBusy, setSearchBusy] = useState(false);
  const [searchError, setSearchError] = useState("");
  const [scopePreview, setScopePreview] = useState<SelectionRecord | null>(null);
  const [scopeBusy, setScopeBusy] = useState(false);
  const [scopeError, setScopeError] = useState("");
  const [proofDocument, setProofDocument] = useState<SelectionRecord | null>(null);
  const [candidate, setCandidate] = useState<SelectionCandidate | null>(null);
  const [candidateAudit, setCandidateAudit] = useState<SelectionAudit | null>(null);
  const [auditBusy, setAuditBusy] = useState(false);
  const [auditError, setAuditError] = useState("");
  const [detail, setDetail] = useState<SelectionTracking | null>(null);
  const [detailRetrospectives, setDetailRetrospectives] = useState<SelectionRetrospective[]>([]);
  const [detailAuditError, setDetailAuditError] = useState("");
  const [activityTick, setActivityTick] = useState(0);
  const [form, setForm] = useState({ market: "CN_A", symbols: "", candidate_limit: 30, min_abs_change_pct: 2, min_volume_ratio: 1.2, period_months: 3, strategy_profile_id: null as number | null, model_instance_code: initialModelCode, use_model: false, knowledge_base_ids: [] as number[], graph_ids: [] as number[] });
  const selectAllMatchesRef = useRef<HTMLInputElement>(null);
  const [review, setReview] = useState({ target_price: "", stop_price: "", confidence: "", notes: "" });
  async function reload() {
    const [nextRuns, nextTracking] = await Promise.all([selectionApi.listRuns(), selectionApi.listTracking()]);
    setRuns(nextRuns); setTracking(nextTracking);
    return nextRuns;
  }
  useEffect(() => {
    let valid = true;
    setLoading(true); setError("");
    void Promise.all([selectionApi.listRuns(), selectionApi.listTracking(), api.listKnowledgeBases(), api.listKnowledgeGraphs(), api.listModelInstances(), api.listModelProviders(), selectionApi.scoringProfiles()]).then(async ([nextRuns, nextTracking, nextBases, nextGraphs, nextInstances, nextProviders, nextProfiles]) => {
      if (!valid) return;
      setRuns(nextRuns); setTracking(nextTracking); setBases(nextBases); setGraphs(nextGraphs); setInstances(nextInstances); setProviders(nextProviders); setScoringProfiles(nextProfiles);
      const enabledBases = nextBases.filter(item => item.enabled);
      const preferred = nextGraphs.find(item => item.graph_code === "FIXED20_ACCEPTANCE_20261021" && item.enabled && ["GOVERNED", "LOCKED"].includes(item.governance_status))
        || nextGraphs.find(item => item.enabled && ["GOVERNED", "LOCKED"].includes(item.governance_status));
      setForm(current => ({ ...current, strategy_profile_id: current.strategy_profile_id || nextProfiles.find(item => item.enabled)?.id || null, knowledge_base_ids: preferred ? [preferred.knowledge_base_id] : enabledBases.slice(0, 1).map(item => item.id), graph_ids: preferred ? [preferred.id] : [], use_model: Boolean(preferred) }));
      if (nextRuns.length) { const first = await selectionApi.getRun(nextRuns[0].id); if (valid) setRun(first); }
    }).catch(err => { if (valid) setError(errorOf(err)); }).finally(() => { if (valid) setLoading(false); });
    return () => { valid = false; };
  }, [loadVersion]);
  useEffect(() => {
    if (!createOpen || !stockQuery.trim()) {setStockMatches([]); setSearchBusy(false); return;}
    let valid = true;
    setSearchBusy(true); setSearchError("");
    const timer = window.setTimeout(() => { void api.searchStocks(form.market, stockQuery.trim(), 50).then(items => {if (valid) setStockMatches(items);}).catch(err => {if (valid) setSearchError(errorOf(err));}).finally(() => {if (valid) setSearchBusy(false);}); }, 250);
    return () => {valid = false; clearTimeout(timer);};
  }, [createOpen, stockQuery, form.market]);
  useEffect(() => {
    if (!createOpen) return;
    let valid = true;
    setScopeBusy(true); setScopeError(""); setScopePreview(null);
    const timer = window.setTimeout(() => { void selectionApi.scopePreview({market: form.market, symbols: chosenStocks.map(item => item.symbol), knowledge_base_ids: form.knowledge_base_ids, graph_ids: form.graph_ids, period_months: form.period_months}).then(result => {if(valid) setScopePreview(result);}).catch(err => {if(valid) setScopeError(errorOf(err));}).finally(() => {if(valid) setScopeBusy(false);}); }, 300);
    return () => {valid = false; clearTimeout(timer);};
  }, [createOpen, form.market, form.knowledge_base_ids, form.graph_ids, form.period_months, chosenStocks]);
  useEffect(() => {
    const backfillStatus = String(((run?.criteria_json.backfill_progress as SelectionRecord | undefined)?.status) || "");
    if (!run || (!["QUEUED", "RUNNING"].includes(run.status) && !["QUEUED", "RUNNING"].includes(backfillStatus))) return;
    let valid = true;
    const timer = window.setInterval(() => {void selectionApi.getRun(run.id).then(async next => {
      if (!valid) return;
      const progress = (next.criteria_json.backfill_progress || {}) as SelectionRecord;
      const rerunId = Number(progress.rerun_id || 0);
      if (rerunId > 0 && !["QUEUED", "RUNNING"].includes(textOf(progress.status))) {
        const rerun = await selectionApi.getRun(rerunId);
        if (!valid) return;
        setRun(rerun); await reload();
        setNotice(`缺失数据补齐完成，已自动进入新分析批次 #${rerunId}；原批次 #${next.id} 保留。`);
        return;
      }
      setRun(next);
      if (!["QUEUED", "RUNNING"].includes(next.status)) void reload();
    }).catch(err => {if(valid) setError(errorOf(err));});}, 1000);
    return () => {valid = false; window.clearInterval(timer);};
  }, [run?.id, run?.status, (run?.criteria_json.backfill_progress as SelectionRecord | undefined)?.status]);
  useEffect(() => {
    const active = !!run && (["QUEUED", "RUNNING"].includes(run.status) || ["QUEUED", "RUNNING"].includes(String((run.criteria_json.retry_progress as SelectionRecord | undefined)?.status || "")));
    if (!active) return;
    const timer = window.setInterval(() => setActivityTick(value => value + 1), 1000);
    return () => window.clearInterval(timer);
  }, [run?.id, run?.status, run?.criteria_json.progress, run?.criteria_json.retry_progress]);
  async function selectRun(id: number) { setBusy(true); setError(""); try { setRun(await selectionApi.getRun(id)); } catch (err) { setError(errorOf(err)); } finally { setBusy(false); } }
  async function create(event: FormEvent) {
    event.preventDefault(); setError(""); setNotice(""); setBusy(true);
    try {
      if (stockQuery.trim() && !chosenStocks.length) throw new Error("请勾选搜索结果中的股票，或清空搜索框后按本地市场筛选。");
      const modelReady = Boolean(form.use_model && form.knowledge_base_ids.length && governedGraphs.some(item => form.graph_ids.includes(item.id)));
      const symbols = chosenStocks.map(item => item.symbol);
      let actualModelCode = form.model_instance_code || null;
      if (modelReady) {
        const gate = await selectionApi.preflight({
          market: form.market, symbols, knowledge_base_ids: form.knowledge_base_ids,
          graph_ids: form.graph_ids, period_months: form.period_months,
          model_instance_code: actualModelCode, use_model: true, data_mode: "REAL",
        });
        if (!gate.model_ready) {
          const model = gate.model || {};
          throw new Error(`${textOf(model.title) || "模型不可用"}：${textOf(model.summary) || "无法完成连通性检查"} ${textOf(model.suggestion)}`);
        }
        actualModelCode = textOf((gate.model || {}).instance_code) || actualModelCode;
        if (!gate.data_ready) {
          const missing = Number((gate.data || {}).missing_symbols?.length || 0);
          if (!window.confirm(`分析前检查发现 ${missing} 只股票存在数据缺口。建议先更新数据；是否继续以部分数据分析？`)) {
            setNotice("已取消分析，请先补齐数据后再开始。");
            return;
          }
        }
      }
      const result = await selectionApi.startRun({ ...form, use_model: modelReady, symbols, model_instance_code: actualModelCode, data_mode: "REAL" });
      setRun(result); await reload(); setCreateOpen(false);
      setNotice(`选股批次 #${result.id} 已提交后台${form.use_model && !modelReady ? "；所选知识库/图谱暂不可用，本次按规则生成候选，未调用模型" : ""}。`); onCreated?.(result);
    } catch (err) { setError(errorOf(err)); } finally { setBusy(false); }
  }
  function openReview(item: SelectionCandidate) {
    setCandidate(item); setCandidateAudit(null); setAuditError(""); setError("");
    setReview({ target_price: item.target_price?.toString() || textOf(item.analysis_json.target_price), stop_price: item.stop_price?.toString() || textOf(item.analysis_json.stop_price), confidence: item.confidence?.toString() || textOf(item.analysis_json.confidence), notes: item.review_notes || "" });
    setAuditBusy(true);
    void selectionApi.candidateAudit(item.id).then(setCandidateAudit).catch(err => setAuditError(errorOf(err))).finally(() => setAuditBusy(false));
  }
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
  async function openTracking(id: number) { setBusy(true); setDetailRetrospectives([]); setDetailAuditError(""); setError(""); try { const [nextDetail, nextRetrospectives] = await Promise.all([selectionApi.getTracking(id), selectionApi.trackingRetrospectives(id)]); setDetail(nextDetail); setDetailRetrospectives(nextRetrospectives); } catch (err) { setDetailAuditError(errorOf(err)); try { setDetail(await selectionApi.getTracking(id)); } catch (fallbackError) { setError(errorOf(fallbackError)); } } finally { setBusy(false); } }
  const pending = run?.candidates.filter(item => !["APPROVED", "REJECTED"].includes(item.decision)).length ?? 0;
  const availableGraphs = graphs.filter(item => item.enabled && form.knowledge_base_ids.includes(item.knowledge_base_id));
  const governedGraphs = availableGraphs.filter(item => ["GOVERNED", "LOCKED"].includes(item.governance_status));
  const scopeKnowledgeBases = records(scopePreview?.knowledge_bases);
  const scopeGraphs = records(scopePreview?.graphs);
  const selectableMatches = stockMatches.filter(item => !chosenStocks.some(stock => stock.market === item.market && stock.symbol === item.symbol));
  const allMatchesSelected = stockMatches.length > 0 && selectableMatches.length === 0;
  const selectedMatchCount = stockMatches.length - selectableMatches.length;
  const failedModelCandidates = run?.candidates.filter(item => item.decision === "PENDING" && (["FAILED", "NOT_RUN"].includes(String(item.analysis_json.model_output_status)) || item.analysis_json.data_gate_status === "MISSING_DATA" || item.analysis_json.analysis_validation_status === "PARTIAL")) ?? [];
  const retryProgress = (run?.criteria_json.retry_progress as SelectionRecord | undefined) ?? {};
  const retryActive = ["QUEUED", "RUNNING"].includes(String(retryProgress.status || ""));
  const submitBlockReason = busy ? "正在提交选股任务。" : "";
  const modelScopeSelected = Boolean(form.knowledge_base_ids.length && governedGraphs.some(item => form.graph_ids.includes(item.id)));
  const successfulModelCount = run?.candidates.filter(item => item.analysis_json.model_output_status === "SUCCESS" && item.analysis_json.data_gate_status !== "MISSING_DATA" && item.analysis_json.analysis_validation_status !== "PARTIAL").length ?? 0;
  const modelPickerOptions = instances.filter(item => item.enabled && providers.some(provider => provider.id === item.provider_id && provider.enabled && provider.provider_type !== "MOCK"));
  const availableSwitchModels = modelPickerOptions.filter(modelIsSelectable);
  const updateChosenStocks = (next: StockSymbol[]) => {
    setChosenStocks(next);
    setForm(current => ({ ...current, candidate_limit: Math.max(current.candidate_limit, next.length) }));
  };
  const toggleStock = (stock: StockSymbol) => {
    const exists = chosenStocks.some(row => row.market === stock.market && row.symbol === stock.symbol);
    if (!exists && chosenStocks.length >= 50) { setSearchError("单次最多手选 50 只股票，请先移除部分已选股票。也可以使用本地全市场规则筛选。"); return; }
    updateChosenStocks(exists
      ? chosenStocks.filter(row => row.market !== stock.market || row.symbol !== stock.symbol)
      : [...chosenStocks, stock]);
  };
  const toggleVisibleMatches = (checked: boolean) => {
    if (!checked) {
      updateChosenStocks(chosenStocks.filter(stock => !stockMatches.some(match => match.market === stock.market && match.symbol === stock.symbol)));
      return;
    }
    const availableSlots = Math.max(0, 50 - chosenStocks.length);
    const next = [...chosenStocks, ...selectableMatches.slice(0, availableSlots)];
    updateChosenStocks(next);
    if (selectableMatches.length > availableSlots) setSearchError("单次最多手选 50 只，已选入剩余可选名额。可移除股票后继续添加。");
    else setSearchError("");
  };
  const clearChosenStocks = () => updateChosenStocks([]);
  useEffect(() => { if (selectAllMatchesRef.current) selectAllMatchesRef.current.indeterminate = selectedMatchCount > 0 && !allMatchesSelected; }, [selectedMatchCount, allMatchesSelected]);
  const selectedModelCode = availableSwitchModels.some(item => item.instance_code === form.model_instance_code)
    ? form.model_instance_code
    : availableSwitchModels.some(item => item.instance_code === run?.model_instance_code)
      ? run?.model_instance_code || ""
      : availableSwitchModels[0]?.instance_code || "";
  async function retryFailed() {
    if (!run || retryActive) return;
    setBusy(true); setError(""); setNotice("");
    try {
      setRun(await selectionApi.retryFailed(run.id));
      await reload();
      setNotice(`已提交 ${failedModelCandidates.length} 只失败股票的单独重试，继续保留原有证据快照。`);
    } catch (err) { setError(errorOf(err)); }
    finally { setBusy(false); }
  }
  async function retryFailedWithModel() {
    if (!run || retryActive || !selectedModelCode || !failedModelCandidates.length) return;
    const sourceRunId = run.id;
    const selectedModel = availableSwitchModels.find(item => item.instance_code === selectedModelCode);
    setBusy(true); setError(""); setNotice("");
    try {
      const next = await selectionApi.retryFailedWithModel(sourceRunId, {
        model_instance_code: selectedModelCode,
        model_instance_codes: [selectedModelCode],
        candidate_ids: failedModelCandidates.map(item => item.id),
      });
      setRun(next); await reload();
      setNotice(`已通过${selectedModel?.model_name || selectedModelCode}预检，创建新批次 #${next.id} 重试 ${failedModelCandidates.length} 只失败股票；原批次 #${sourceRunId} 保留。`);
    } catch (err) { setError(errorOf(err)); }
    finally { setBusy(false); }
  }
  async function retryWholeRun() {
    if (!run || run.status !== "FAILED") return;
    setBusy(true); setError(""); setNotice("");
    try {
      const next = await selectionApi.retryRun(run.id);
      setRun(next); await reload();
      setNotice(`已创建新批次 #${next.id} 重新执行；原失败批次 #${run.id} 保留，可随时回看。`);
    } catch (err) { setError(errorOf(err)); }
    finally { setBusy(false); }
  }
  async function backfill(candidateIds: number[] = [], dimensions: AnalysisDimension[] = []) {
    if (!run) return;
    setBusy(true); setError(""); setNotice("");
    try {
      const next = await selectionApi.backfill(run.id, { candidate_ids: candidateIds, dimensions, period_months: Number(run.criteria_json.period_months || 3), rerun_analysis: true });
      setRun(next);
      setNotice(candidateIds.length ? "已提交该股票的数据补齐；完成后会创建新的分析批次，旧结论继续保留。" : "已提交本批次缺失数据补齐；完成后会创建新的分析批次，旧结论继续保留。");
    } catch (err) { setError(errorOf(err)); }
    finally { setBusy(false); }
  }
  async function rescore(item: SelectionCandidate) {
    setBusy(true); setError("");
    try {
      const scoring = await selectionApi.rescoreCandidate(item.id);
      const next = {...item, analysis_json: {...item.analysis_json, scoring}};
      setCandidate(next);
      setRun(current => current ? {...current, candidates: current.candidates.map(row => row.id === next.id ? next : row)} : current);
      setNotice("八维评分已生成，原分析证据和历史结论未被覆盖。");
    } catch (err) { setError(errorOf(err)); }
    finally { setBusy(false); }
  }
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
          <div className="selection-kpis"><div className="selection-kpi"><span>规则候选</span><strong>{run.candidate_count}</strong></div><div className="selection-kpi"><span>待人工复选</span><strong>{pending}</strong></div><div className="selection-kpi"><span>分析周期</span><strong>{Number(run.criteria_json.period_months || 3)} 个月</strong><small>{textOf(run.criteria_json.period_start)} 至 {textOf(run.criteria_json.period_end)}</small></div><div className="selection-kpi"><span>数据截止时间</span><strong style={{ fontSize: 14 }}>{dateOf(run.as_of)}</strong></div></div>
          {["QUEUED", "RUNNING"].includes(run.status) && (() => { const progress = (run.criteria_json.progress || {}) as SelectionRecord; const heartbeat = textOf(progress.heartbeat_at || progress.updated_at); const age = heartbeatAge(progress.heartbeat_at); const stale = age != null && age > 15; return <div className={`batch-progress${stale ? " stale" : ""}`} role="status"><div><strong>当前阶段：{textOf(progress.stage) || "准备中"}</strong><span>{Number(progress.percent || 0)}%</span></div><progress aria-label="分析进度" max="100" value={Number(progress.percent || 0)} /><div className="batch-progress-meta"><span className="batch-progress-activity">当前动作：{textOf(progress.activity || progress.message) || "等待工作线程启动"}</span><span>已运行：{elapsed(liveElapsed(progress, activityTick))}</span><span>实际模型：{textOf(progress.model_instance_code) || run.model_instance_code || "预检后确定"}</span><span>当前股票：{Array.isArray(progress.candidate_symbols) ? progress.candidate_symbols.join("、") : "--"}</span>{Number(progress.prompt_chars || 0) > 0 && <span>模型上下文：{Number(progress.prompt_chars).toLocaleString()} / {Number(progress.prompt_limit_chars || 60000).toLocaleString()} 字符</span>}{Number(progress.retained_document_count || 0) > 0 && <span>载入文档：{Number(progress.retained_document_count)} 篇{Number(progress.omitted_document_count || 0) > 0 ? ` · 预算外 ${Number(progress.omitted_document_count)} 篇` : ""}</span>}<span>最近心跳：{heartbeat ? `${age ?? 0} 秒前` : "尚未记录"}</span></div>{stale && <strong className="batch-progress-warning">最近 {age} 秒没有心跳更新，后台可能异常；请稍候刷新查看状态。</strong>}<small>进度百分比表示阶段；系统会持续写入后台活动和心跳。</small>{records(progress.process_steps).length > 0 && <div className="batch-process-steps"><strong>后台处理记录</strong><ol>{records(progress.process_steps).slice(-8).reverse().map((step, index) => <li key={`${textOf(step.at)}-${index}`}><span>{textOf(step.message) || "处理中"}</span><small>{textOf(step.stage)} · {Number(step.percent || 0)}% · {textOf(step.at) ? dateOf(textOf(step.at)) : "刚刚"}</small></li>)}</ol></div>}</div>; })()}
          {["QUEUED", "RUNNING"].includes(textOf((run.criteria_json.backfill_progress as SelectionRecord)?.status)) && <div className="batch-progress"><strong>{textOf((run.criteria_json.backfill_progress as SelectionRecord)?.message) || "正在补齐缺失数据"}</strong><progress aria-label="数据补齐进度" max="100" value={Number((run.criteria_json.backfill_progress as SelectionRecord)?.percent || 0)} /></div>}
          <p className="selection-help">模型：{run.model_instance_code || "自动路由 / 规则预选"} · {run.knowledge_base_ids_json.length} 个知识库 · {run.graph_ids_json.length} 张图谱。未确认的候选不会进入预测跟踪。</p>
          {run.error_message && <p className="form-error">{run.error_message}</p>}
          {(failedModelCandidates.length > 0 || retryActive || run.status === "FAILED") && <section className="selection-recovery" aria-label="分析失败处理">
            <div className="selection-scope-heading"><strong>引用校验通过 {successfulModelCount} / {run.candidates.length} 只 · 待处理 {failedModelCandidates.length} 只</strong>
              {run.status === "FAILED" ? <button type="button" disabled={busy} onClick={() => void retryWholeRun()}>重新执行此批次</button> : <button type="button" disabled={busy || retryActive || !failedModelCandidates.length} onClick={() => void retryFailed()}>{retryActive ? "正在重新分析…" : `重新分析 ${failedModelCandidates.length} 只失败或待核对股票`}</button>}
            </div>
            {failedModelCandidates.length > 0 && run.status !== "FAILED" && <div className="selection-model-switch" role="group" aria-label="更换模型并重试">
              <strong>重试模型</strong><span>{selectedModelCode ? `沿用分析模型：${selectedModelCode}` : "尚未选择可用分析模型"}</span>
              <button type="button" disabled={busy || retryActive || !selectedModelCode || !availableSwitchModels.length} onClick={() => void retryFailedWithModel()}>
                {busy ? "正在检查模型…" : "使用分析模型重试"}
              </button>
              <small>重试不再单独选择模型；系统沿用新建分析时的模型，并先发送最小探测请求，探测失败不会创建任务。</small>
            </div>}
            <p className="selection-help">重试只处理失败且尚未复选的股票，保留本轮证据和历史调用；整批异常则创建新批次，原记录保留。</p>
            {!!retryProgress.status && <p role="status">{textOf(retryProgress.message)}{retryActive ? "，下方进度会自动更新。" : ""}</p>}
            {failedModelCandidates.length > 0 && <details><summary>查看逐股失败原因与尝试记录（{failedModelCandidates.length}）</summary><div className="selection-failure-list">{failedModelCandidates.map(item => <article key={item.id}>
              <strong>{item.name || item.symbol} · {item.symbol}</strong><p>{textOf(item.analysis_json.model_failure_reason) || run.error_message || "历史批次未记录逐股错误；请重试生成可追溯分析。"}</p>{records((item.analysis_json.model_failure_detail as SelectionRecord | undefined)?.recovery_actions).length > 0 && <div className="batch-recovery-actions"><strong>针对性处理步骤</strong><ol>{records((item.analysis_json.model_failure_detail as SelectionRecord | undefined)?.recovery_actions).map((action, index) => <li key={index}>{textOf(action)}</li>)}</ol></div>}
              {attemptsNewestFirst(item.analysis_json.model_attempts).map((attempt, index) => <small key={index}>{dateOf(textOf(attempt.at) || null)} · {attempt.status === "SUCCESS" ? "调用完成" : "未通过"}{attempt.model_call_log_id ? ` · 调用 #${attempt.model_call_log_id}` : ""} · {textOf(attempt.reason) || "结果已返回并经过证据校验"}</small>)}
            </article>)}</div></details>}
          </section>}
          {run.missing_data_json.length > 0 && <details className="selection-evidence"><summary>数据与分析限制（{run.missing_data_json.length}）</summary><ul>{run.missing_data_json.map((item, i) => <li key={i}>{item}</li>)}</ul><button type="button" disabled={busy || ["QUEUED", "RUNNING"].includes(textOf((run.criteria_json.backfill_progress as SelectionRecord)?.status))} onClick={() => void backfill()}>补齐全部缺失并重新分析</button></details>}
          <div className="table-wrap"><table><thead><tr><th>股票</th><th>行情基准</th><th>规则评分</th><th>分析状态 / 结论</th><th>人工复选</th><th>操作</th></tr></thead><tbody>{run.candidates.map(item => <tr key={item.id}><td><strong>{item.name || item.symbol}</strong><br /><code>{item.symbol}</code> <small>{nameOf(item.market)}</small></td><td>{numberOf(item.entry_price)}<br /><small>{item.entry_date || "缺行情"}</small></td><td>{numberOf(item.hard_score, 1)}</td><td style={{ maxWidth: 340 }}><span className={`selection-analysis-state ${item.analysis_json.model_output_status === "FAILED" || item.analysis_json.data_gate_status === "MISSING_DATA" || item.analysis_json.analysis_validation_status === "PARTIAL" ? "warning" : ""}`}>{item.analysis_json.data_gate_status === "MISSING_DATA" || item.analysis_json.model_output_status === "NOT_RUN" ? "数据不足 · 未进行模型分析" : item.analysis_json.model_output_status === "SUCCESS" ? item.analysis_json.analysis_validation_status === "PARTIAL" ? "部分证据不足 · 待核对" : "模型分析已完成" : item.analysis_json.model_output_status === "FAILED" ? "模型分析失败 · 可重试" : "规则预选 · 未形成模型结论"}</span>{textOf(item.analysis_json.summary) || textOf(item.analysis_json.reasoning) || "查看资料后复选"}{strings(item.evidence_json.missing_data).length > 0 && <p className="selection-help">有 {strings(item.evidence_json.missing_data).length} 项数据待补充</p>}</td><td><Badge value={item.decision} /></td><td><button type="button" disabled={busy} onClick={() => openReview(item)}>{["APPROVED", "REJECTED"].includes(item.decision) ? "查看决策" : "证据与复选"}</button></td></tr>)}</tbody></table>{!run.candidates.length && <p className="selection-empty">当前条件没有候选。请检查本地行情覆盖、数据时效与筛选阈值。</p>}</div>
        </> : <p className="selection-empty">新建选股，从已有数据中筛出候选，再结合知识证据进行人工复选。</p>}
      </> : <>
        <p className="selection-help">仅计算人工确认后、同一复权口径的有效交易日行情。周末、节假日或缺行情不会充当跟踪天数。后台 Scheduler 与 Worker 运行时会定期更新，也可手动更新。</p>
        <div className="table-wrap"><table><thead><tr><th>股票</th><th>确认基准 / 目标价</th><th>跟踪进度</th><th>最新收盘 / 日期</th><th>累计收益</th><th>最大回撤</th><th>状态</th><th>操作</th></tr></thead><tbody>{tracking.map(item => <tr key={item.id}><td><code>{item.symbol}</code><br /><small>{nameOf(item.market)}</small></td><td>{numberOf(item.entry_price)} / {numberOf(item.target_price)}</td><td>{item.observed_sessions} / {item.required_sessions} 交易日</td><td>{numberOf(item.latest_price)}<br /><small>{item.latest_date || "等待行情"}</small></td><td>{pct(item.cumulative_return_pct)}</td><td>{pct(item.max_drawdown_pct)}</td><td><Badge value={item.status} /></td><td><button type="button" disabled={busy} onClick={() => void openTracking(item.id)}>查看跟踪与复盘</button></td></tr>)}</tbody></table>{!tracking.length && <p className="selection-empty">尚无跟踪股票。请在候选池中完成人工复选并确认预测目标。</p>}</div>
      </>}
    </section>
    {createOpen && <Dialog title="新建选股" busy={busy} close={() => setCreateOpen(false)}><form className="selection-form" onSubmit={create}>
      <div className="selection-form-grid"><label>市场<select value={form.market} onChange={event => {setForm({ ...form, market: event.target.value }); setChosenStocks([]); setStockQuery("");}}>{["CN_A", "HK", "NEEQ", "NEEQ_INNOVATION"].map(market => <option key={market} value={market}>{nameOf(market)}</option>)}</select></label><label>候选数量上限<input type="number" required min={1} max={50} value={form.candidate_limit} onChange={event => setForm({ ...form, candidate_limit: Number(event.target.value) })} /></label><label>分析周期（月）<input type="number" required min={1} max={60} step={1} value={form.period_months} onChange={event => setForm({ ...form, period_months: Math.max(1, Math.floor(Number(event.target.value) || 1)) })} /><small>默认读取最近 3 个自然月，可填写 1–60 的自然数。</small></label></div>
      <div className="selection-stock-picker">
        <label>1. 选择股票<input aria-label="选股股票搜索" role="combobox" aria-expanded={!!stockMatches.length} aria-controls="selection-stock-options" value={stockQuery} placeholder="代码、中文名称、英文名、拼音或首字母" onChange={event => setStockQuery(event.target.value)} /></label>
        {stockMatches.length > 0 && <div className="selection-select-all"><label><input ref={selectAllMatchesRef} type="checkbox" checked={allMatchesSelected} aria-checked={allMatchesSelected ? true : selectedMatchCount ? "mixed" : false} onChange={event => toggleVisibleMatches(event.target.checked)} />全选当前 {stockMatches.length} 条结果</label><span>本页已选 {selectedMatchCount} 只 · 搜索最多显示 50 条</span></div>}
        <div className="selection-stock-results" id="selection-stock-options">{stockMatches.map(item => { const checked = chosenStocks.some(stock => stock.market === item.market && stock.symbol === item.symbol); return <label key={`${item.market}:${item.symbol}`}><input type="checkbox" checked={checked} onChange={() => toggleStock(item)} /><span><strong>{item.name}</strong><small>{item.symbol} · {nameOf(item.market)}</small></span></label>; })}</div>
        {searchBusy && stockQuery && <small role="status">正在搜索…</small>}{searchError && <p role="alert">{searchError}</p>}{stockQuery && !searchBusy && !stockMatches.length && !searchError && <small>未找到匹配股票，请调整关键词。</small>}
        <div className="selection-scope-heading"><strong>已选 {chosenStocks.length} / 50 只</strong><button type="button" disabled={!chosenStocks.length} onClick={clearChosenStocks}>清空已选</button></div>
        <div className="selection-stock-selected">{chosenStocks.map(item => <button type="button" key={`${item.market}:${item.symbol}`} aria-label={`移除${item.name}`} onClick={() => toggleStock(item)}>{item.name} · {item.symbol} ×</button>)}</div>
        <small className="selection-help">手选股票逐只分析，不因量价阈值被静默剔除；留空则按本地已有行情和下方阈值筛选。</small>
      </div>
      <div className="selection-form-grid"><label>绝对涨跌幅阈值（%）<input type="number" required min={0} max={50} step="0.1" value={form.min_abs_change_pct} onChange={event => setForm({ ...form, min_abs_change_pct: Number(event.target.value) })} /></label><label>成交量比阈值<input type="number" required min={0} max={100} step="0.1" value={form.min_volume_ratio} onChange={event => setForm({ ...form, min_volume_ratio: Number(event.target.value) })} /></label></div>
      <div className="selection-scope-heading"><strong>2. 选择证据 · 知识库</strong><button type="button" disabled={!bases.some(item => item.enabled)} onClick={() => { const ids = bases.filter(item => item.enabled).map(item => item.id); setForm(current => ({ ...current, knowledge_base_ids: ids, graph_ids: graphs.filter(graph => graph.enabled && ids.includes(graph.knowledge_base_id) && ["GOVERNED", "LOCKED"].includes(graph.governance_status)).map(graph => graph.id) })); }}>全选可用范围</button></div>
      <p className="selection-help">知识库提供原文和事实，图谱提供公司、行业与事件关系。同名的“公告与外部事件知识库”来自不同治理批次，业务范围相同，保存的数据可能不同；请比较创建时间与本次覆盖。公告是法定披露，外部事件是政策、原材料、产业链等线索。</p>
      <div className="selection-choices">{bases.filter(item => item.enabled).map(item => { const stats = scopeKnowledgeBases.find(row => Number(row.id) === item.id); return <label key={item.id} className={form.knowledge_base_ids.includes(item.id) ? "selected" : ""}>
        <input type="checkbox" checked={form.knowledge_base_ids.includes(item.id)} onChange={() => { const ids = form.knowledge_base_ids.includes(item.id) ? form.knowledge_base_ids.filter(id => id !== item.id) : [...form.knowledge_base_ids, item.id]; setForm({ ...form, knowledge_base_ids: ids, graph_ids: form.graph_ids.filter(id => graphs.some(graph => graph.id === id && ids.includes(graph.knowledge_base_id))) }); }} />
        <span className="selection-choice-name"><strong>{item.kb_name}</strong><small>#{item.id} · {item.version} · {item.source_tables.map(source => sourceLabelFallback[source] || nameOf(source)).join("、") || "未登记来源"}</small>
          <small>创建于 {dateOf(item.created_at)}{bases.filter(other => other.enabled && other.kb_name === item.kb_name).length > 1 ? " · 同名历史批次，请按覆盖选择" : ""}</small><span className="selection-choice-count">{scopeBusy ? "正在统计覆盖…" : stats ? `${Number(stats.document_count || 0).toLocaleString()} 篇文档 · 覆盖 ${Number(stats.covered_stocks || 0)} 只${chosenStocks.length ? ` / 已选 ${chosenStocks.length} 只` : "（当前市场）"}` : "覆盖统计暂不可用"}</span>
          <small>{sourceCountSummary(stats?.source_counts, scopePreview?.source_labels)}</small>
          <small>{scopeBusy || !stats ? "" : !Number(stats.document_count) ? "本次无关联文档，对本轮文档分析无补充" : chosenStocks.length && Number(stats.covered_stocks) < chosenStocks.length ? "部分股票有资料，未覆盖股票将提示证据不足" : "当前范围均有资料，可提供原文依据"}</small>
          <details onClick={event => event.stopPropagation()}><summary>范围与批次说明</summary><small>{item.description || "用于检索已归集的文档证据及其来源记录。"}</small><small>批次编码：{item.kb_code}</small>{strings(stats?.source_explanations).map((explanation, index) => <small key={index}>{explanation}</small>)}</details>
        </span></label>; })}</div>
      <div className="selection-scope-heading"><strong>独立图谱 · 关系证据</strong><span>已治理表示完成治理流程，不代表每条关系已人工核实</span></div>
      <div className="selection-choices">{availableGraphs.map(item => { const stats = scopeGraphs.find(row => Number(row.id) === item.id); const eligible = ["GOVERNED", "LOCKED"].includes(item.governance_status); return <label key={item.id} className={!eligible ? "selection-choice-disabled" : form.graph_ids.includes(item.id) ? "selected" : ""}>
        <input type="checkbox" checked={form.graph_ids.includes(item.id)} disabled={!eligible} onChange={() => setForm({ ...form, graph_ids: form.graph_ids.includes(item.id) ? form.graph_ids.filter(id => id !== item.id) : [...form.graph_ids, item.id] })} /><span className="selection-choice-name"><strong>{item.graph_name}</strong><small>#{item.id} · {item.version} · {eligible ? "已治理，可用于模型分析" : "待治理，不纳入模型证据"}</small>
          <span className="selection-choice-count">{scopeBusy ? "正在统计覆盖…" : stats ? `交集 ${Number(stats.document_count || 0).toLocaleString()} 篇文档 · ${Number(stats.direct_relation_count || 0).toLocaleString()} 条直接关系 · 覆盖 ${Number(stats.covered_stocks || 0)} 只` : "覆盖统计暂不可用"}</span>
          <small>{relationCountSummary(stats?.relation_type_counts)}</small><details onClick={event => event.stopPropagation()}><summary>图谱范围说明</summary><small>{item.description || "用于提供股票、公司、行业及事件间可追溯关系。"}</small><small>全图总量：{item.entity_count.toLocaleString()} 个实体 / {item.relation_count.toLocaleString()} 条关系（包含本轮以外的股票）</small></details>
        </span></label>; })}{!availableGraphs.length && <span className="selection-help">请先勾选知识库；该知识库暂无启用图谱。</span>}</div>
      <section className="selection-scope-preview" aria-label="知识范围覆盖预检"><strong>本轮证据预览</strong>
        {scopeBusy ? <p role="status">正在统计交集；您仍可提交任务，后台会核验实际范围。</p> : scopeError ? <p role="alert">覆盖统计暂不可用：{scopeError}。仍可提交，后台执行时核验。</p> : scopePreview && <>
          <p>选中图谱含 {Object.values(scopePreview.source_counts || {}).reduce<number>((sum, value) => sum + Number(value || 0), 0).toLocaleString()} 篇投影文档，覆盖 {scopePreview.covered_stocks} 只股票{chosenStocks.length ? ` / 本次选 ${chosenStocks.length} 只` : "（当前市场）"}。文档数量含图谱投影记录，不代表不重复的原文数量。</p>
          <p>{sourceCountSummary(scopePreview.source_counts, scopePreview.source_labels)}</p>
          {records(scopePreview.rows).length > 0 && <details className="selection-coverage-details"><summary>逐股查看资料覆盖与关系类型（{records(scopePreview.rows).length} 只）</summary><div className="selection-coverage-list">{records(scopePreview.rows).map(row => <article key={String(row.symbol)} className={row.status === "AVAILABLE" ? "coverage-ready" : "coverage-missing"}>
            <strong>{chosenStocks.find(stock => stock.symbol === row.symbol)?.name || row.symbol} · {String(row.coverage_label || "暂无关联资料")}</strong><span>知识库 {Number(row.knowledge_document_count || 0)} 篇 · 选中图谱 {Number(row.graph_document_count || 0)} 篇 · 直接关系 {Number(row.direct_relation_count || 0)} 条</span>
            <small>{sourceCountSummary(row.graph_source_counts, scopePreview.source_labels)}</small><small>{relationCountSummary(row.relation_type_counts)}</small>
          </article>)}</div></details>}
          {strings(scopePreview.warnings).map((warning, index) => <p className="selection-help" key={index}>提示：{warning}</p>)}{scopePreview.ready !== true && <p className="selection-help">{strings(scopePreview.blockers).join("；")}；当前可先生成规则候选。</p>}
          <small>覆盖数量与关系种类可核查，但不等于语义相关度、来源可靠度或投资价值；缺少资料会标为证据不足，关联关系不直接证明因果。</small>
        </>}
      </section>
      <label style={{ display: "flex", flexDirection: "row", alignItems: "center" }}><input type="checkbox" checked={form.use_model} onChange={event => setForm({ ...form, use_model: event.target.checked })} />调用模型综合分析候选证据</label>
      <label>评价权重方案<select required value={form.strategy_profile_id || ""} onChange={event => setForm({ ...form, strategy_profile_id: Number(event.target.value) || null })}>{scoringProfiles.filter(item => item.enabled).map(item => <option key={item.id} value={item.id}>{item.profile_name} v{item.version} · {item.horizon_code}</option>)}</select><small>雷达图展示八维原始分；该版本权重只影响可信综合分，并随批次冻结。</small></label>
      <label>分析模型<select disabled={!form.use_model} value={form.model_instance_code} onChange={event => setForm({ ...form, model_instance_code: event.target.value })}><option value="">按选股任务自动路由</option>{instances.filter(item => item.enabled && providers.some(provider => provider.id === item.provider_id && provider.enabled && provider.provider_type !== "MOCK")).map(item => { const reason = modelStatusReason(item); return <option className={modelStatusClass(item)} key={item.id} value={item.instance_code} disabled={!modelIsSelectable(item)}>{item.model_name} · {item.instance_code} · {modelStatusLabel(item)}{reason ? `（${reason}）` : ""}</option>; })}</select></label>
      <p className="selection-help">只向模型发送筛选后的候选和有限证据。取消模型分析时保存为规则预选，供人工核验；数据不足或模型失败会明确记录。</p>
      <p className="selection-submit-mode" role="status">本次将：{form.use_model && modelScopeSelected ? `生成候选池并进行多源模型分析${scopeBusy ? "（覆盖统计进行中，不影响提交）" : ""}` : "生成规则候选，不调用模型"}{form.use_model && !modelScopeSelected ? "。若需模型分析，请勾选知识库及其已治理图谱。" : ""}</p>
      {error && <p className="form-error" role="alert">{error}</p>}<footer><span className="selection-submit-reason" role="status">{submitBlockReason}</span><button type="button" disabled={busy} onClick={() => setCreateOpen(false)}>取消</button><button className="primary-button" disabled={!!submitBlockReason}>{busy ? "正在提交…" : "生成候选池"}</button></footer>
    </form></Dialog>}
    {candidate && <Dialog title={`${candidate.symbol} ${candidate.name || ""} · 人工复选`} busy={busy} close={() => setCandidate(null)}>
      <p className="selection-help">行情基准 {numberOf(candidate.entry_price)}（{candidate.entry_date || "缺行情"}）。确认后记录预测与当时证据，后续复盘以确认时快照为准。</p><SelectionAnalysis candidate={candidate} onRescore={() => void rescore(candidate)} onBackfillDimension={dimension => void backfill([candidate.id], [dimension])} onDocument={id => {const doc = [...records(candidate.evidence_json.documents), ...records(candidate.evidence_json.related_security_documents), ...records(candidate.evidence_json.evidence_context?.documents), ...records(candidate.evidence_json.evidence_context?.related_security_documents)].find(row => row.id === id); setProofDocument({id, title: doc?.title, loading: true}); void selectionApi.candidateDocument(candidate.id, id).then(setProofDocument).catch(err => setProofDocument({id, error: errorOf(err)}));}} />{strings(candidate.evidence_json.missing_data).length > 0 && <section className="selection-recovery"><div className="selection-scope-heading"><strong>该股票有 {strings(candidate.evidence_json.missing_data).length} 项数据待补齐</strong><button type="button" disabled={busy} onClick={() => void backfill([candidate.id])}>补齐该股票并重新分析</button></div><p className="selection-help">补齐会读取已登记数据源，并创建新分析批次；当前证据快照和结论不会被覆盖。</p></section>}{proofDocument && <article className="selection-evidence"><h3>{textOf(proofDocument.title) || `原文 #${proofDocument.id}`}</h3><p>{proofDocument.loading ? "正在读取原文…" : proofDocument.error || proofDocument.content}</p><button type="button" onClick={() => setProofDocument(null)}>收起原文</button></article>}<details><summary>查看原文、事实及血缘明细</summary><Evidence candidate={candidate} /><AuditTrail audit={candidateAudit} busy={auditBusy} error={auditError} /></details>
      {!["APPROVED", "REJECTED"].includes(candidate.decision) ? <form className="selection-form" onSubmit={event => { event.preventDefault(); void saveReview("APPROVED"); }}><h3>确认预测</h3><div className="selection-form-grid"><label>预测目标价<input type="number" required min="0.000001" step="any" value={review.target_price} onChange={event => setReview({ ...review, target_price: event.target.value })} /></label><label>观察止损价（选填）<input type="number" min="0.000001" step="any" value={review.stop_price} onChange={event => setReview({ ...review, stop_price: event.target.value })} /></label><label>主观置信度（0–1，选填）<input type="number" min={0} max={1} step="0.01" value={review.confidence} onChange={event => setReview({ ...review, confidence: event.target.value })} /></label></div><label>复选理由 / 调整说明<textarea rows={3} maxLength={4000} value={review.notes} onChange={event => setReview({ ...review, notes: event.target.value })} /></label>{error && <p className="form-error" role="alert">{error}</p>}<footer><button type="button" disabled={busy} onClick={() => void saveReview("REJECTED")}>排除此候选</button><button className="primary-button" disabled={busy || !candidate.entry_price || candidate.analysis_json.analysis_validation_status === "PARTIAL" || ["FAILED", "NOT_RUN"].includes(String(candidate.analysis_json.model_output_status)) || candidate.analysis_json.data_gate_status === "MISSING_DATA"}>{busy ? "保存中…" : "确认入选并开始跟踪"}</button></footer></form> : <div className="selection-evidence"><Badge value={candidate.decision} /><p>目标价：{numberOf(candidate.target_price)} · 确认时间：{dateOf(candidate.reviewed_at)}</p><p>{candidate.review_notes || "未填写人工复选说明"}</p></div>}
    </Dialog>}
    {detail && <Dialog title={`${detail.symbol} · 跟踪与复盘`} close={() => setDetail(null)}><p className="selection-help">起始 {dateOf(detail.started_at)} · {detail.observed_sessions} / {detail.required_sessions} 个交易日 · {nameOf(detail.status)}</p><ReturnChart item={detail} /><div className="selection-kpis"><div className="selection-kpi"><span>累计收益</span><strong>{pct(detail.cumulative_return_pct)}</strong></div><div className="selection-kpi"><span>最大回撤</span><strong>{pct(detail.max_drawdown_pct)}</strong></div><div className="selection-kpi"><span>预测收益</span><strong>{pct(detail.target_return_pct)}</strong></div><div className="selection-kpi"><span>方向判断</span><strong>{detail.direction_hit == null ? "待复盘" : detail.direction_hit ? "正确" : "偏离"}</strong></div></div><h3>复盘结论</h3><p className="selection-help">{textOf(detail.review_json.summary) || textOf(detail.review_json.conclusion) || textOf(detail.review_json.reason) || (detail.observed_sessions < detail.required_sessions ? "有效跟踪日尚未满 10 个交易日，继续等待行情，不提前判定预测准确。" : "请更新跟踪以生成复盘结果。")}</p>{strings(detail.review_json.adjustments).length > 0 && <ul>{strings(detail.review_json.adjustments).map((item, i) => <li key={i}>{item}</li>)}</ul>}<RetrospectiveTrail rows={detailRetrospectives} error={detailAuditError} /><div className="table-wrap"><table style={{ minWidth: 540 }}><thead><tr><th>交易日序号</th><th>日期</th><th>收盘价</th><th>累计收益</th><th>回撤</th></tr></thead><tbody>{detail.snapshots.map(item => <tr key={item.id}><td>{item.sequence}</td><td>{item.trade_date}</td><td>{numberOf(item.close_price)}</td><td>{pct(item.return_pct)}</td><td>{pct(item.drawdown_pct)}</td></tr>)}</tbody></table></div></Dialog>}
  </div>;
}
