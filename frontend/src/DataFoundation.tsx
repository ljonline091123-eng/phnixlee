import { useEffect, useRef, useState, type FormEvent, type ReactNode } from "react";
import { ArrowLeft, ArrowRight, Check, ExternalLink, FileText, Plus, RefreshCw, Search, Unlink, Upload, X } from "lucide-react";
import { foundationApi, type FoundationCatalog, type FoundationEntity, type FoundationEntityDetail, type FoundationEvidence, type FoundationFact, type FoundationFactDetail, type FoundationPage, type FoundationStatus, type FoundationStockOption } from "./foundationApi";
import "./DataFoundation.css";

type Tab = "entities" | "evidence" | "facts" | "legal";
type Row = FoundationEntity | FoundationEvidence | FoundationFact;
type DialogState = { kind: "entity-create" | "evidence-create" | "import" | "fact-create" } | { kind: "entity" | "evidence" | "fact"; id: string } | null;
const entityNames: Record<string, string> = { COMPANY: "公司", PERSON: "自然人", ORGANIZATION: "机构", HOLDER_ACCOUNT: "披露股东账户（身份待解析）", PROJECT: "项目", INDUSTRY: "行业", THEME: "板块主题", CLASSIFICATION: "类型标签", PRODUCT: "产品", MATERIAL: "原材料" };
const factNames: Record<string, string> = { HOLDS_EQUITY: "直接持股", CONTROLS: "控制关系", SUPPLIES_TO: "供应关系", PARTNERS_WITH: "合作关系", GUARANTEES: "担保", LENDS_TO: "借贷", COMPETES_WITH: "竞争关系", IN_INDUSTRY: "行业归属", MEMBER_OF_THEME: "板块归属", HAS_CLASSIFICATION: "类型归属", CONTRACT: "合同", LEGAL_CASE: "司法事项" };
const statusNames: Record<string, string> = { ALL: "全部状态", PENDING: "待审核", ACCEPTED: "已接受", REJECTED: "已拒绝" };
const valueNames: Record<string, string> = { INTENT: "意向", SIGNED: "已签署", ACTIVE: "履行中", COMPLETED: "已完成", TERMINATED: "已终止", CIVIL: "民事", CRIMINAL: "刑事", ADMINISTRATIVE: "行政", ARBITRATION: "仲裁", ENFORCEMENT: "执行", BANKRUPTCY: "破产", OTHER: "其他", PLAINTIFF: "原告", DEFENDANT: "被告", THIRD_PARTY: "第三人", APPLICANT: "申请人", RESPONDENT: "被申请人", DEBTOR: "债务人", CREDITOR: "债权人", FILED: "已立案", PENDING: "审理中", JUDGMENT: "已裁判", APPEAL: "上诉中", SETTLED: "已和解", WITHDRAWN: "已撤回", CLOSED: "已结案", UNKNOWN: "未知", CLAIMED: "诉请金额", SETTLEMENT: "和解金额", PROVISION: "预计负债" };
const propertyNames: Record<string, string> = { ratio: "持股比例 (%)", ratio_basis: "比例分母与口径", shares: "持股数量", shares_unit: "数量单位", control_basis: "控制依据", product: "产品或服务", business_scope: "业务范围", amount: "金额", currency: "币种", taxonomy: "行业分类体系", taxonomy_version: "分类版本", classification_method: "分类方法", contract_status: "合同状态", case_number: "案号", court: "法院或机构", jurisdiction: "司法辖区", procedure_type: "程序类型", party_role: "主体诉讼角色", case_status: "程序状态", amount_type: "金额性质", registered_address: "注册地址", legal_form: "法律形式", registration_status: "登记状态", description: "备注" };
const amountTypeNames: Record<string, string> = { CLAIMED: "诉请金额", JUDGMENT: "判决义务金额", ENFORCEMENT: "执行金额", SETTLEMENT: "和解金额", PROVISION: "预计负债" };
const propertyValueName = (key: string, value: string) => value === "SOURCE_SNAPSHOT_UNKNOWN_BUSINESS_DATE" ? "持仓基准日未知" : (key === "amount_type" ? amountTypeNames[value] : valueNames[value]) || value;
const tabNames: Record<Tab, string> = { entities: "主体主数据", evidence: "证据资料", facts: "关系与事项", legal: "司法事项" };
const text = (value: unknown): string => value == null || value === "" ? "未提供" : typeof value === "object" ? JSON.stringify(value) : String(value);
const errorText = (error: unknown) => error instanceof Error ? error.message : "请求失败";
const dateText = (value: string | null | undefined) => value ? /^\d{4}-\d{2}-\d{2}$/.test(value) ? value : new Date(value).toLocaleString("zh-CN") : "未提供";
const iso = (value: FormDataEntryValue | null) => value ? new Date(String(value)).toISOString() : null;
const fieldText = (data: FormData, key: string) => String(data.get(key) || "").trim();
const emptyPage = (): FoundationPage<Row> => ({ items: [], total: 0, limit: 20, offset: 0 });

function IconButton({ label, children, onClick, disabled = false }: { label: string; children: ReactNode; onClick: () => void; disabled?: boolean }) {
  return <button className="foundation-icon" type="button" title={label} aria-label={label} onClick={onClick} disabled={disabled}>{children}</button>;
}
function Status({ value }: { value: string }) { return <span className={"foundation-status " + value.toLowerCase()}>{statusNames[value] || value}</span>; }
function Alert({ error }: { error: string }) { return error ? <div className="foundation-error" role="alert">{error}</div> : null; }
function Empty({ children = "暂无记录" }: { children?: ReactNode }) { return <div className="foundation-empty">{children}</div>; }
function Properties({ values }: { values: Record<string, unknown> }) {
  const entries = Object.entries(values).filter(([, value]) => value != null && value !== "");
  return entries.length ? <dl className="foundation-properties">{entries.map(([key, value]) => <div key={key}><dt>{propertyNames[key] || key}</dt><dd>{typeof value === "string" ? propertyValueName(key, value) : text(value)}</dd></div>)}</dl> : <Empty>暂无补充属性</Empty>;
}
function Modal({ title, close, children, busy = false }: { title: string; close: () => void; children: ReactNode; busy?: boolean }) {
  const ref = useRef<HTMLElement>(null);
  const closeRef = useRef(close); closeRef.current = close;
  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    const overflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    ref.current?.focus();
    return () => { document.body.style.overflow = overflow; previous?.focus(); };
  }, []);
  return <div className="foundation-backdrop" onMouseDown={event => { if (event.target === event.currentTarget && !busy) closeRef.current(); }}>
    <section ref={ref} className="foundation-dialog" role="dialog" aria-modal="true" aria-label={title} tabIndex={-1} onKeyDown={event => {
      if (event.key === "Escape" && !busy) { event.stopPropagation(); closeRef.current(); }
      if (event.key !== "Tab") return;
      const elements = Array.from(ref.current?.querySelectorAll<HTMLElement>('button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), a[href], summary, [tabindex="0"]') || []);
      const first = elements[0], last = elements[elements.length - 1];
      if (!first) { event.preventDefault(); return; }
      if (event.shiftKey && (document.activeElement === first || document.activeElement === ref.current)) { event.preventDefault(); last.focus(); }
      else if (!event.shiftKey && (document.activeElement === last || document.activeElement === ref.current)) { event.preventDefault(); first.focus(); }
    }}>
      <header><h2>{title}</h2><IconButton label="关闭" onClick={close} disabled={busy}><X size={18} /></IconButton></header>
      <div className="foundation-dialog-body">{children}</div>
    </section>
  </div>;
}

function EntityPicker({ label, value, change, required = false }: { label: string; value: FoundationEntity | null; change: (item: FoundationEntity | null) => void; required?: boolean }) {
  const [query, setQuery] = useState("");
  const [items, setItems] = useState<FoundationEntity[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => {
    if (value) return;
    let active = true;
    const timer = window.setTimeout(() => {
      setLoading(true); setError("");
      void foundationApi.entities({ q: query, limit: 8 }).then(result => { if (active) setItems(result.items); }).catch(err => { if (active) setError(errorText(err)); }).finally(() => { if (active) setLoading(false); });
    }, 200);
    return () => { active = false; window.clearTimeout(timer); };
  }, [query, value]);
  return <div className="foundation-picker"><span className="foundation-label">{label}{required ? " *" : ""}</span>
    {value ? <div className="foundation-picked"><span><strong>{value.name}</strong><small>{entityNames[value.entity_type] || value.entity_type} · {value.identifier_value || value.jurisdiction || "未提供标识"}</small></span><IconButton label={"清除" + label} onClick={() => change(null)}><X size={16} /></IconButton></div> : <>
      <input aria-label={label + "检索"} value={query} onChange={event => setQuery(event.target.value)} placeholder="名称或登记标识" />
      <div className="foundation-options">{loading ? <span>加载中...</span> : items.map(item => <button key={item.id} type="button" onClick={() => change(item)}><strong>{item.name}</strong><small>{entityNames[item.entity_type] || item.entity_type} · {item.identifier_value || item.jurisdiction || "未提供标识"}</small></button>)}{!loading && !items.length && !error && <span>未找到主体</span>}</div>
    </>}
    <Alert error={error} />
  </div>;
}

function EvidencePicker({ selected, change, single = false }: { selected: FoundationEvidence[]; change: (items: FoundationEvidence[]) => void; single?: boolean }) {
  const [query, setQuery] = useState("");
  const [items, setItems] = useState<FoundationEvidence[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => {
    let active = true;
    const timer = window.setTimeout(() => {
      setLoading(true); setError("");
      void foundationApi.evidence({ q: query, limit: 10 }).then(result => { if (active) setItems(result.items); }).catch(err => { if (active) setError(errorText(err)); }).finally(() => { if (active) setLoading(false); });
    }, 200);
    return () => { active = false; window.clearTimeout(timer); };
  }, [query]);
  return <div className="foundation-picker"><span className="foundation-label">证据资料 *</span>
    <input aria-label="检索证据" value={query} onChange={event => setQuery(event.target.value)} placeholder="标题或来源" />
    {selected.length > 0 && <div className="foundation-selected">{selected.map(item => <div key={item.id}><FileText size={15} /><span>{item.title}</span><IconButton label={"移除证据 " + item.title} onClick={() => change(selected.filter(row => row.id !== item.id))}><X size={14} /></IconButton></div>)}</div>}
    <div className="foundation-options">{loading ? <span>加载中...</span> : items.map(item => <label key={item.id}><input type={single ? "radio" : "checkbox"} name={single ? "foundation-evidence" : undefined} checked={selected.some(row => row.id === item.id)} onChange={event => change(event.target.checked ? single ? [item] : [...selected, item] : selected.filter(row => row.id !== item.id))} /><span><strong>{item.title}</strong><small>{item.source_name} · 版本 {item.version}</small></span></label>)}{!loading && !items.length && !error && <span>未找到证据</span>}</div>
    <Alert error={error} />
  </div>;
}

function EvidenceContent({ item }: { item: FoundationEvidence }) {
  let safeUrl: string | null = null;
  try { if (item.url && ["https:", "http:"].includes(new URL(item.url).protocol)) safeUrl = item.url; } catch { /* Invalid source URLs remain plain text. */ }
  return <article className="foundation-evidence-detail"><h3>{item.title}</h3><dl className="foundation-properties"><div><dt>来源</dt><dd>{item.source_name}</dd></div><div><dt>来源记录</dt><dd>{item.source_key}</dd></div><div><dt>版本</dt><dd>{item.version}</dd></div><div><dt>发布时间</dt><dd>{dateText(item.published_at)}</dd></div><div><dt>可获知时间</dt><dd>{dateText(item.available_at)}</dd></div><div><dt>收录时间</dt><dd>{dateText(item.created_at)}</dd></div></dl>
    {safeUrl && <a className="foundation-source-link" href={safeUrl} target="_blank" rel="noreferrer">查看来源 <ExternalLink size={14} /></a>}
    <div className="foundation-original">{item.content || "未提供正文"}</div><details><summary>证据标识</summary><p className="foundation-code">{item.id}</p><p className="foundation-code">SHA-256: {item.content_hash}</p></details>
  </article>;
}
function EvidenceDetail({ id, close }: { id: string; close: () => void }) {
  const [item, setItem] = useState<FoundationEvidence | null>(null), [error, setError] = useState("");
  useEffect(() => { let active = true; void foundationApi.evidenceDetail(id).then(value => { if (active) setItem(value); }).catch(err => { if (active) setError(errorText(err)); }); return () => { active = false; }; }, [id]);
  return <Modal title="证据详情" close={close}><Alert error={error} />{item ? <EvidenceContent item={item} /> : !error && <Empty>加载中...</Empty>}</Modal>;
}

function SecurityMapping({ entityId, saved }: { entityId: string; saved: () => void }) {
  const [query, setQuery] = useState(""), [stocks, setStocks] = useState<FoundationStockOption[]>([]), [stock, setStock] = useState<FoundationStockOption | null>(null);
  const [evidence, setEvidence] = useState<FoundationEvidence[]>([]), [busy, setBusy] = useState(false), [error, setError] = useState(""), [loading, setLoading] = useState(false);
  useEffect(() => {
    if (stock) return;
    let active = true;
    const timer = window.setTimeout(() => { setLoading(true); setError(""); void foundationApi.stocks({ q: query, limit: 8 }).then(result => { if (active) setStocks(result.items); }).catch(err => { if (active) setError(errorText(err)); }).finally(() => { if (active) setLoading(false); }); }, 250);
    return () => { active = false; window.clearTimeout(timer); };
  }, [query, stock]);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(""); if (!stock || !evidence.length) { setError("请选择证券并关联发行主体证据。"); return; }
    const data = new FormData(event.currentTarget); setBusy(true);
    try { await foundationApi.mapSecurity({ entity_id: entityId, stock_symbol_id: stock.id, evidence_id: evidence[0].id, share_class: fieldText(data, "share_class") || undefined }); setStock(null); setEvidence([]); setQuery(""); saved(); }
    catch (err) { setError(errorText(err)); } finally { setBusy(false); }
  }
  return <form className="foundation-form" onSubmit={submit}><fieldset disabled={busy}>
    <label>证券检索<input aria-label="证券检索" value={query} onChange={event => { setQuery(event.target.value); setStock(null); }} placeholder="代码或名称" /></label>
    {stock ? <div className="foundation-picked"><strong>{stock.market} · {stock.symbol} · {stock.name}</strong><IconButton label="清除证券" onClick={() => setStock(null)}><X size={16} /></IconButton></div> : <div className="foundation-options">{loading ? <span>加载中...</span> : stocks.map(item => <button key={item.id} type="button" disabled={!!item.mapped_entity_id} onClick={() => setStock(item)}><strong>{item.market} · {item.symbol} · {item.name}</strong><small>{item.mapped_entity_id ? "已有主体映射" : item.exchange}</small></button>)}{!loading && !stocks.length && <span>未找到证券</span>}</div>}
    <label>股份类别<input name="share_class" placeholder="如 A_SHARE / H_SHARE" /></label><EvidencePicker single selected={evidence} change={setEvidence} /><Alert error={error} /><footer><button className="primary-button" type="submit">{busy ? "关联中..." : "确认证券映射"}</button></footer>
  </fieldset></form>;
}

function EntityDetail({ id, close, changed, openFacts }: { id: string; close: () => void; changed: () => void; openFacts: (entity: FoundationEntity) => void }) {
  const [item, setItem] = useState<FoundationEntityDetail | null>(null), [error, setError] = useState(""), [revision, setRevision] = useState(0);
  useEffect(() => { let active = true; setError(""); void foundationApi.entity(id).then(value => { if (active) setItem(value); }).catch(err => { if (active) setError(errorText(err)); }); return () => { active = false; }; }, [id, revision]);
  return <Modal title="主体详情" close={close}><Alert error={error} />{item ? <>
    <div className="foundation-detail-heading"><div><h3>{item.name}</h3><span>{entityNames[item.entity_type] || item.entity_type}</span></div><button type="button" onClick={() => openFacts(item)}>查看关联事项 <ArrowRight size={15} /></button></div>
    <dl className="foundation-properties"><div><dt>辖区</dt><dd>{text(item.jurisdiction)}</dd></div><div><dt>标识体系</dt><dd>{text(item.identifier_scheme)}</dd></div><div><dt>登记标识</dt><dd>{text(item.identifier_value)}</dd></div><div><dt>创建时间</dt><dd>{dateText(item.created_at)}</dd></div></dl><Properties values={item.properties_json} />
    <section className="foundation-detail-section"><h3>证券映射</h3>{item.listings.length ? <div className="foundation-table-scroll"><table><thead><tr><th>市场</th><th>证券代码</th><th>名称</th><th>股份类别</th></tr></thead><tbody>{item.listings.map(row => <tr key={row.id}><td>{row.market}</td><td>{row.symbol}</td><td>{row.name}</td><td>{text(row.share_class)}</td></tr>)}</tbody></table></div> : <Empty>暂无证券映射</Empty>}</section>
    {item.entity_type === "COMPANY" && <details className="foundation-mapping"><summary>新增证券映射</summary><SecurityMapping entityId={item.id} saved={() => { setRevision(value => value + 1); changed(); }} /></details>}
    <details><summary>主体标识</summary><p className="foundation-code">{item.id}</p></details>
  </> : !error && <Empty>加载中...</Empty>}</Modal>;
}

function FactDetail({ id, close, changed }: { id: string; close: () => void; changed: () => void }) {
  const [item, setItem] = useState<FoundationFactDetail | null>(null), [error, setError] = useState(""), [busy, setBusy] = useState(false), [revision, setRevision] = useState(0);
  const [decision, setDecision] = useState<"ACCEPTED" | "REJECTED">("ACCEPTED");
  useEffect(() => { let active = true; setError(""); void foundationApi.fact(id).then(value => { if (active) setItem(value); }).catch(err => { if (active) setError(errorText(err)); }); return () => { active = false; }; }, [id, revision]);
  async function review(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!item) return; const data = new FormData(event.currentTarget); setBusy(true); setError("");
    try { const updated = await foundationApi.review(item.id, { decision: item.status === "ACCEPTED" ? "REJECTED" : decision, reason: fieldText(data, "reason"), reviewer: fieldText(data, "reviewer"), expected_status: item.status }); setItem(current => current ? { ...current, ...updated } : current); setRevision(value => value + 1); changed(); }
    catch (err) { setError(errorText(err)); } finally { setBusy(false); }
  }
  return <Modal title="事实与审核" close={close} busy={busy}>{item ? <>
    <div className="foundation-detail-heading"><div><h3>{item.title}</h3><span>{factNames[item.fact_type] || item.fact_type}</span></div><Status value={item.status} /></div>
    <div className="foundation-relation"><span>{item.subject_name}</span>{item.object_entity_id && <><ArrowRight size={18} /><span>{item.object_name || item.object_entity_id}</span></>}</div>
    {item.properties_json.temporal_scope === "SOURCE_SNAPSHOT_UNKNOWN_BUSINESS_DATE" && <p className="foundation-notice">持仓基准日未知</p>}
    <Properties values={item.properties_json} /><dl className="foundation-properties"><div><dt>生效开始</dt><dd>{item.valid_from || "未提供"}</dd></div><div><dt>生效截止（不含）</dt><dd>{item.valid_to || "未提供"}</dd></div></dl>
    <section className="foundation-detail-section"><h3>关联证据 · {item.evidence.length}</h3>{item.evidence.map(evidence => <details key={evidence.id} className="foundation-evidence-disclosure"><summary>{evidence.title}<small>{evidence.source_name} · 版本 {evidence.version}</small></summary><EvidenceContent item={evidence} /></details>)}{!item.evidence.length && <Empty>暂无关联证据</Empty>}</section>
    {(item.status === "PENDING" || item.status === "ACCEPTED") && <form key={item.status} className="foundation-form foundation-review" onSubmit={review}>
      <h3>{item.status === "ACCEPTED" ? "撤销接受" : "人工审核"}</h3>
      <fieldset disabled={busy}><div className="foundation-form-grid">
        {item.status === "ACCEPTED" ? <label>撤销后状态<input value="已拒绝" readOnly /></label> : <label>审核结论<select value={decision} onChange={event => setDecision(event.target.value as "ACCEPTED" | "REJECTED")}><option value="ACCEPTED">接受</option><option value="REJECTED">拒绝</option></select></label>}
        <label>{item.status === "ACCEPTED" ? "撤销记录人 *" : "审核人 *"}<input name="reviewer" required maxLength={128} /></label>
      </div><label>{item.status === "ACCEPTED" ? "撤销原因 *" : "审核依据 *"}<textarea name="reason" required minLength={2} maxLength={4000} rows={3} /></label>
      <footer><button className="primary-button" type="submit">{item.status === "PENDING" && decision === "ACCEPTED" ? <Check size={16} /> : <X size={16} />}{busy ? "提交中..." : item.status === "ACCEPTED" ? "撤销接受" : "提交审核"}</button></footer></fieldset>
    </form>}
    <Alert error={error} /><section className="foundation-detail-section"><h3>审核记录</h3>{item.reviews.length ? <ol className="foundation-review-history">{item.reviews.map(reviewItem => <li key={reviewItem.id}><div><Status value={reviewItem.decision} /><strong>{reviewItem.reviewer}</strong><time>{dateText(reviewItem.created_at)}</time></div><p>{reviewItem.reason}</p></li>)}</ol> : <Empty>暂无审核记录</Empty>}</section>
  </> : <><Alert error={error} />{!error && <Empty>加载中...</Empty>}</>}</Modal>;
}

export function DataFoundation() {
  const [tab, setTab] = useState<Tab>("entities"), [catalog, setCatalog] = useState<FoundationCatalog | null>(null), [summary, setSummary] = useState<FoundationStatus | null>(null);
  const [query, setQuery] = useState(""), [search, setSearch] = useState(""), [typeFilter, setTypeFilter] = useState(""), [statusFilter, setStatusFilter] = useState("ALL"), [offset, setOffset] = useState(0);
  const [entityFilter, setEntityFilter] = useState<FoundationEntity | null>(null), [page, setPage] = useState<FoundationPage<Row>>(emptyPage), [loading, setLoading] = useState(true), [error, setError] = useState(""), [summaryError, setSummaryError] = useState("");
  const [revision, setRevision] = useState(0), [dialog, setDialog] = useState<DialogState>(null), [notice, setNotice] = useState("");
  const activeTab = useRef(tab);
  activeTab.current = tab;
  useEffect(() => { let active = true; void foundationApi.catalog().then(value => { if (active) setCatalog(value); }).catch(() => { /* Lists report unavailable services; local labels remain usable. */ }); return () => { active = false; }; }, []);
  useEffect(() => { let active = true; setSummaryError(""); void foundationApi.status().then(value => { if (active) setSummary(value); }).catch(err => { if (active) setSummaryError(errorText(err)); }); return () => { active = false; }; }, [revision]);
  useEffect(() => {
    let active = true; setLoading(true); setError("");
    const params = { q: search, limit: 20, offset };
    const action: Promise<FoundationPage<Row>> = tab === "entities" ? foundationApi.entities({ ...params, entity_type: typeFilter }) : tab === "evidence" ? foundationApi.evidence({ ...params, entity_id: entityFilter?.id }) : foundationApi.facts({ ...params, entity_id: entityFilter?.id, fact_type: tab === "legal" ? "LEGAL_CASE" : typeFilter, status: statusFilter });
    void action.then(value => { if (active && activeTab.current === tab) setPage(value); }).catch(err => { if (active && activeTab.current === tab) { setError(errorText(err)); setPage(emptyPage()); } }).finally(() => { if (active && activeTab.current === tab) setLoading(false); });
    return () => { active = false; };
  }, [tab, search, typeFilter, statusFilter, offset, revision, entityFilter]);
  function chooseTab(value: Tab) {
    if (value === tab) return;
    activeTab.current = value;
    setLoading(true);
    setPage(emptyPage());
    setError("");
    setTab(value); setTypeFilter(""); setStatusFilter("ALL"); setOffset(0); setQuery(""); setSearch(""); setEntityFilter(null); setNotice("");
  }
  function changed() { setRevision(value => value + 1); }
  function saved() { setDialog(null); setOffset(0); setNotice("已保存"); changed(); }
  function searchSubmit(event: FormEvent) { event.preventDefault(); setSearch(query.trim()); setOffset(0); }
  const countTiles = summary?.enabled ? [["主体", summary.counts.entities], ["证券映射", summary.counts.listings], ["证据", summary.counts.evidence], ["待审核", summary.counts.pending_facts], ["已接受", summary.counts.accepted_facts]] as const : [];
  if (summary && !summary.enabled) return <section className="foundation"><div className="foundation-heading"><h2>主体与关系</h2><IconButton label="刷新数据" onClick={changed}><RefreshCw size={18} /></IconButton></div><Empty>数据底座当前已停用</Empty></section>;
  return <section className="foundation" aria-label="主体与关系工作台">
    <div className="foundation-heading"><div><h2>主体与关系</h2><span>主数据 · 证据 · 事实审核</span></div><IconButton label="刷新数据" disabled={loading} onClick={changed}><RefreshCw size={18} /></IconButton></div>
    <div className="foundation-stats">{countTiles.map(([label, count]) => <div key={label}><span>{label}</span><strong>{count.toLocaleString()}</strong></div>)}</div>
    <Alert error={summaryError} />
    <div className="foundation-tabs" role="tablist" aria-label="数据底座分类">{(Object.keys(tabNames) as Tab[]).map(value => <button key={value} id={"foundation-tab-" + value} role="tab" aria-selected={tab === value} aria-controls="foundation-tabpanel" type="button" className={tab === value ? "active" : ""} onClick={() => chooseTab(value)}>{tabNames[value]}</button>)}</div>
    <div role="tabpanel" id="foundation-tabpanel" aria-labelledby={"foundation-tab-" + tab}>
      <div className="foundation-toolbar"><form onSubmit={searchSubmit}><div className="foundation-search"><Search size={16} /><input aria-label="检索当前列表" value={query} onChange={event => setQuery(event.target.value)} placeholder={tab === "entities" ? "名称或登记标识" : "标题或关键词"} /><button type="submit" aria-label="查询" title="查询"><ArrowRight size={17} /></button></div>
        {(tab === "entities" || tab === "facts") && <select aria-label="类型筛选" value={typeFilter} onChange={event => { setTypeFilter(event.target.value); setOffset(0); }}><option value="">全部类型</option>{(tab === "entities" ? catalog?.entity_types || Object.keys(entityNames) : catalog?.fact_types || Object.keys(factNames)).map(value => <option key={value} value={value}>{(tab === "entities" ? entityNames : factNames)[value] || value}</option>)}</select>}
        {(tab === "facts" || tab === "legal") && <select aria-label="审核状态筛选" value={statusFilter} onChange={event => { setStatusFilter(event.target.value); setOffset(0); }}>{Object.entries(statusNames).map(([value, name]) => <option key={value} value={value}>{name}</option>)}</select>}
      </form><div className="foundation-actions">{tab === "evidence" && <button type="button" onClick={() => setDialog({ kind: "import" })}><Upload size={16} />导入图谱文档</button>}<button className="primary-button" type="button" onClick={() => setDialog({ kind: tab === "entities" ? "entity-create" : tab === "evidence" ? "evidence-create" : "fact-create" })}><Plus size={16} />{tab === "entities" ? "新增主体" : tab === "evidence" ? "新增证据" : "新增候选"}</button></div></div>
      {entityFilter && <div className="foundation-filter-chip"><span>关联主体：{entityFilter.name}</span><IconButton label="清除主体筛选" onClick={() => { setEntityFilter(null); setOffset(0); }}><Unlink size={14} /></IconButton></div>}
      {notice && <div className="foundation-notice" role="status"><Check size={15} />{notice}</div>}<Alert error={error} />
      <div className="foundation-table-scroll" aria-busy={loading}>
        <table><thead>{tab === "entities" ? <tr><th>主体名称</th><th>类型</th><th>辖区</th><th>登记标识</th><th>创建时间</th><th>详情</th></tr> : tab === "evidence" ? <tr><th>标题</th><th>来源</th><th>版本</th><th>发布时间</th><th>可获知时间</th><th>原文</th></tr> : <tr><th>事实与事项</th><th>主体</th><th>关系对方</th><th>状态</th><th>证据</th><th>审核</th></tr>}</thead><tbody>
          {loading ? <tr><td colSpan={6}><Empty>加载中...</Empty></td></tr> : page.items.map(row => tab === "entities" ? (() => { const item = row as FoundationEntity; return <tr key={item.id}><td><button className="foundation-link" type="button" onClick={() => setDialog({ kind: "entity", id: item.id })}>{item.name}</button></td><td>{entityNames[item.entity_type] || item.entity_type}</td><td>{text(item.jurisdiction)}</td><td><span>{text(item.identifier_value)}</span><small>{item.identifier_scheme}</small></td><td>{dateText(item.created_at)}</td><td><IconButton label={"查看主体 " + item.name} onClick={() => setDialog({ kind: "entity", id: item.id })}><ArrowRight size={16} /></IconButton></td></tr>; })() : tab === "evidence" ? (() => { const item = row as FoundationEvidence; return <tr key={item.id}><td><button className="foundation-link" type="button" onClick={() => setDialog({ kind: "evidence", id: item.id })}>{item.title}</button></td><td>{item.source_name}</td><td>{item.version}</td><td>{dateText(item.published_at)}</td><td>{dateText(item.available_at)}</td><td><IconButton label={"查看证据 " + item.title} onClick={() => setDialog({ kind: "evidence", id: item.id })}><FileText size={16} /></IconButton></td></tr>; })() : (() => { const item = row as FoundationFact; return <tr key={item.id}><td><button className="foundation-link" type="button" onClick={() => setDialog({ kind: "fact", id: item.id })}>{item.title}</button><small>{factNames[item.fact_type] || item.fact_type}</small></td><td>{item.subject_name}</td><td>{item.object_name || "未指定"}</td><td><Status value={item.status} /></td><td>{item.evidence_ids.length} 份</td><td><IconButton label={"查看事实 " + item.title} onClick={() => setDialog({ kind: "fact", id: item.id })}><ArrowRight size={16} /></IconButton></td></tr>; })())}
          {!loading && !page.items.length && <tr><td colSpan={6}><Empty>{error ? "数据暂不可用" : "暂无匹配记录"}</Empty></td></tr>}
        </tbody></table>
      </div>
      <div className="foundation-pagination"><span>共 {page.total.toLocaleString()} 条{page.total > 0 ? " · 第 " + (Math.floor(offset / 20) + 1) + " / " + Math.ceil(page.total / 20) + " 页" : ""}</span><div><IconButton label="上一页" disabled={loading || offset === 0} onClick={() => setOffset(value => Math.max(0, value - 20))}><ArrowLeft size={17} /></IconButton><IconButton label="下一页" disabled={loading || offset + 20 >= page.total} onClick={() => setOffset(value => value + 20)}><ArrowRight size={17} /></IconButton></div></div>
    </div>
    {dialog?.kind === "entity-create" && <CreateEntity catalog={catalog} close={() => setDialog(null)} saved={saved} />}
    {dialog?.kind === "evidence-create" && <CreateEvidence close={() => setDialog(null)} saved={saved} />}
    {dialog?.kind === "import" && <CreateEvidence importing close={() => setDialog(null)} saved={saved} />}
    {dialog?.kind === "fact-create" && <CreateFact legal={tab === "legal"} catalog={catalog} close={() => setDialog(null)} saved={saved} />}
    {dialog?.kind === "entity" && <EntityDetail id={dialog.id} close={() => setDialog(null)} changed={changed} openFacts={entity => { chooseTab("facts"); setEntityFilter(entity); setDialog(null); }} />}
    {dialog?.kind === "evidence" && <EvidenceDetail id={dialog.id} close={() => setDialog(null)} />}
    {dialog?.kind === "fact" && <FactDetail id={dialog.id} close={() => setDialog(null)} changed={changed} />}
  </section>;
}

function CreateEntity({ catalog, close, saved }: { catalog: FoundationCatalog | null; close: () => void; saved: () => void }) {
  const [busy, setBusy] = useState(false), [error, setError] = useState("");
  const [type, setType] = useState("COMPANY");
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const data = new FormData(event.currentTarget); setError("");
    if (!!fieldText(data, "identifier_scheme") !== !!fieldText(data, "identifier_value")) { setError("标识体系与登记标识需要一起填写。"); return; }
    setBusy(true);
    try {
      const properties: Record<string, unknown> = {};
      for (const key of ["description", "legal_form", "registered_address", "registration_status"]) if (fieldText(data, key)) properties[key] = fieldText(data, key);
      await foundationApi.createEntity({ name: fieldText(data, "name"), entity_type: type, jurisdiction: fieldText(data, "jurisdiction"), identifier_scheme: fieldText(data, "identifier_scheme") || null, identifier_value: fieldText(data, "identifier_value") || null, properties_json: properties }); saved();
    } catch (err) { setError(errorText(err)); } finally { setBusy(false); }
  }
  return <Modal title="新增主体" close={close} busy={busy}><form className="foundation-form" onSubmit={submit}><fieldset disabled={busy}>
    <div className="foundation-form-grid"><label>主体类型<select value={type} onChange={event => setType(event.target.value)}>{(catalog?.entity_types || Object.keys(entityNames)).map(item => <option key={item} value={item}>{entityNames[item] || item}</option>)}</select></label><label>名称 *<input name="name" required maxLength={256} /></label><label>注册或所属辖区 *<input name="jurisdiction" required placeholder="CN / HK" maxLength={64} /></label><label>标识体系<input name="identifier_scheme" placeholder="如 USCC" maxLength={64} /></label><label>登记标识<input name="identifier_value" maxLength={128} /></label></div>
    {type === "COMPANY" && <div className="foundation-form-grid"><label>法律形式<input name="legal_form" /></label><label>登记状态<input name="registration_status" /></label><label className="foundation-span">注册地址<input name="registered_address" /></label></div>}
    <label>备注<textarea name="description" rows={3} /></label><Alert error={error} /><footer><button type="button" onClick={close}>取消</button><button className="primary-button" type="submit">{busy ? "保存中..." : "保存主体"}</button></footer>
  </fieldset></form></Modal>;
}

function CreateEvidence({ close, saved, importing = false }: { close: () => void; saved: () => void; importing?: boolean }) {
  const [entity, setEntity] = useState<FoundationEntity | null>(null), [busy, setBusy] = useState(false), [error, setError] = useState("");
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const data = new FormData(event.currentTarget); setError("");
    const content = String(data.get("content") || "");
    if (!importing && !content.trim()) { setError("原文不能为空白。"); return; }
    if (!importing && ["published_at", "available_at"].some(key => data.get(key) && new Date(String(data.get(key))).getTime() > Date.now())) { setError("发布时间和可获知时间不能晚于当前时间。"); return; }
    setBusy(true);
    try {
      if (importing) await foundationApi.importEvidence({ document_id: Number(data.get("document_id")), ...(entity ? { entity_id: entity.id } : {}) });
      else await foundationApi.createEvidence({ entity_id: entity?.id || null, source_name: fieldText(data, "source_name"), source_key: fieldText(data, "source_key"), title: fieldText(data, "title"), content, url: fieldText(data, "url") || null, published_at: iso(data.get("published_at")), available_at: iso(data.get("available_at")) });
      saved();
    } catch (err) { setError(errorText(err)); } finally { setBusy(false); }
  }
  return <Modal title={importing ? "导入已有图谱文档" : "新增证据"} close={close} busy={busy}><form className="foundation-form" onSubmit={submit}><fieldset disabled={busy}>
    <EntityPicker label="关联主体" value={entity} change={setEntity} />
    {importing ? <label>原知识文档编号 *<input name="document_id" type="number" min={1} step={1} required /></label> : <>
      <div className="foundation-form-grid"><label>来源名称 *<input name="source_name" required maxLength={256} /></label><label>来源记录标识 *<input name="source_key" required maxLength={512} /></label><label className="foundation-span">标题 *<input name="title" required maxLength={512} /></label><label className="foundation-span">来源网址<input name="url" type="url" maxLength={2048} /></label><label>原文发布时间<input name="published_at" type="datetime-local" /></label><label>首次可获知时间<input name="available_at" type="datetime-local" /></label></div>
      <label>原文 *<textarea name="content" required rows={10} maxLength={200000} /></label>
    </>}
    <Alert error={error} /><footer><button type="button" onClick={close}>取消</button><button className="primary-button" type="submit">{busy ? "保存中..." : importing ? "导入文档" : "保存证据"}</button></footer>
  </fieldset></form></Modal>;
}

type PropertyField = { key: string; required?: boolean; numeric?: boolean; options?: string[]; max?: number };
const propertyFields: Record<string, PropertyField[]> = {
  HOLDS_EQUITY: [{ key: "ratio", numeric: true, max: 100 }, { key: "ratio_basis" }, { key: "shares", numeric: true }, { key: "shares_unit" }],
  CONTROLS: [{ key: "control_basis", required: true }],
  SUPPLIES_TO: [{ key: "product", required: true }, { key: "business_scope" }],
  PARTNERS_WITH: [{ key: "business_scope", required: true }],
  GUARANTEES: [{ key: "amount", numeric: true, required: true }, { key: "currency", required: true }],
  LENDS_TO: [{ key: "amount", numeric: true, required: true }, { key: "currency", required: true }],
  COMPETES_WITH: [{ key: "business_scope", required: true }],
  IN_INDUSTRY: [{ key: "taxonomy", required: true }, { key: "taxonomy_version", required: true }],
  MEMBER_OF_THEME: [{ key: "classification_method", required: true }], HAS_CLASSIFICATION: [{ key: "classification_method", required: true }],
  CONTRACT: [{ key: "contract_status", required: true, options: ["INTENT", "SIGNED", "ACTIVE", "COMPLETED", "TERMINATED"] }, { key: "amount", numeric: true }, { key: "currency" }],
  LEGAL_CASE: [{ key: "case_number", required: true }, { key: "court", required: true }, { key: "jurisdiction", required: true }, { key: "procedure_type", required: true, options: ["CIVIL", "CRIMINAL", "ADMINISTRATIVE", "ARBITRATION", "ENFORCEMENT", "BANKRUPTCY", "OTHER"] }, { key: "party_role", required: true, options: ["PLAINTIFF", "DEFENDANT", "THIRD_PARTY", "APPLICANT", "RESPONDENT", "DEBTOR", "CREDITOR", "OTHER"] }, { key: "case_status", required: true, options: ["FILED", "PENDING", "JUDGMENT", "APPEAL", "SETTLED", "WITHDRAWN", "ENFORCEMENT", "CLOSED", "UNKNOWN"] }, { key: "amount_type", options: ["CLAIMED", "JUDGMENT", "ENFORCEMENT", "SETTLEMENT", "PROVISION"] }, { key: "amount", numeric: true }, { key: "currency" }],
};
function CreateFact({ close, saved, catalog, legal = false }: { close: () => void; saved: () => void; catalog: FoundationCatalog | null; legal?: boolean }) {
  const [type, setType] = useState(legal ? "LEGAL_CASE" : "HOLDS_EQUITY");
  const [subject, setSubject] = useState<FoundationEntity | null>(null), [object, setObject] = useState<FoundationEntity | null>(null);
  const [evidence, setEvidence] = useState<FoundationEvidence[]>([]), [busy, setBusy] = useState(false), [error, setError] = useState("");
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const data = new FormData(event.currentTarget); setError("");
    if (!subject || (type !== "LEGAL_CASE" && !object)) { setError("请选择关联主体和关系对方。"); return; }
    if (type !== "LEGAL_CASE" && subject.id === object?.id) { setError("关系主体与关系对方不能是同一主体。"); return; }
    if (!evidence.length) { setError("请关联至少一份证据。"); return; }
    const properties: Record<string, unknown> = {};
    for (const field of propertyFields[type] || []) { const value = fieldText(data, field.key); if (value) properties[field.key] = field.numeric ? Number(value) : value; }
    if (type === "HOLDS_EQUITY" && properties.ratio == null && properties.shares == null) { setError("持股比例或持股数量至少填写一项。"); return; }
    if (type === "HOLDS_EQUITY" && ((properties.ratio != null && !properties.ratio_basis) || (properties.shares != null && !properties.shares_unit))) { setError("请填写比例分母与口径，或持股数量对应的单位。"); return; }
    if (properties.amount != null && (!properties.currency || (type === "LEGAL_CASE" && !properties.amount_type))) { setError("金额需要币种；司法金额还需要明确金额性质。"); return; }
    const from = fieldText(data, "valid_from") || null, to = fieldText(data, "valid_to") || null;
    if (from && to && from >= to) { setError("生效截止日期必须晚于开始日期。"); return; }
    setBusy(true);
    try { await foundationApi.createFact({ fact_type: type, title: fieldText(data, "title"), subject_entity_id: subject.id, object_entity_id: type === "LEGAL_CASE" ? null : object!.id, properties_json: properties, evidence_ids: evidence.map(item => item.id), valid_from: from, valid_to: to }); saved(); }
    catch (err) { setError(errorText(err)); } finally { setBusy(false); }
  }
  return <Modal title={legal ? "新增司法候选" : "新增关系候选"} close={close} busy={busy}><form className="foundation-form" onSubmit={submit}><fieldset disabled={busy}>
    <div className="foundation-form-grid"><label>事实类型<select value={type} onChange={event => { setType(event.target.value); setError(""); }}>{(legal ? ["LEGAL_CASE"] : catalog?.fact_types || Object.keys(factNames)).map(item => <option key={item} value={item}>{factNames[item] || item}</option>)}</select></label><label>标题 *<input name="title" required maxLength={512} /></label></div>
    <div className="foundation-form-grid"><EntityPicker label={type === "LEGAL_CASE" ? "涉案主体" : "关系主体"} required value={subject} change={setSubject} />{type !== "LEGAL_CASE" && <EntityPicker label="关系对方" required value={object} change={setObject} />}</div>
    <div key={type} className="foundation-form-grid">{(propertyFields[type] || []).map(field => <label key={field.key}>{propertyNames[field.key]}{field.required ? " *" : ""}{field.options ? <select name={field.key} required={field.required} defaultValue=""><option value="">请选择</option>{field.options.map(value => <option key={value} value={value}>{propertyValueName(field.key, value)}</option>)}</select> : <input name={field.key} type={field.numeric ? "number" : "text"} min={field.numeric ? 0 : undefined} max={field.max} step={field.numeric ? "any" : undefined} required={field.required} />}</label>)}</div>
    <div className="foundation-form-grid"><label>生效开始日期<input name="valid_from" type="date" /></label><label>生效截止日期（不含）<input name="valid_to" type="date" /></label></div>
    <EvidencePicker selected={evidence} change={setEvidence} /><Alert error={error} /><footer><button type="button" onClick={close}>取消</button><button className="primary-button" type="submit">{busy ? "提交中..." : "提交待审候选"}</button></footer>
  </fieldset></form></Modal>;
}
