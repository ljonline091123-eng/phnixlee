import { useEffect, useState, type ReactNode } from "react";
import { Building2, ExternalLink, FileText, GitBranch, Search, X } from "lucide-react";
import { createPortal } from "react-dom";
import { companyGraphApi, type ClassificationDefinition, type CompanyClassification, type CompanyCoverage, type CompanyGraphData, type CompanyProfile, type CompanySearchItem, type CompanySecurity } from "./companyGraphApi";
import { foundationApi, type FoundationEvidence, type FoundationFact } from "./foundationApi";
import { EnvironmentBanner } from "./EnvironmentBanner";
import { CompanyGovernancePanel } from "./CompanyGovernancePanel";
import "./CompanyGraphWorkbench.css";

type Mode = "security" | "company";
type View = "overview" | "ownership" | "supply_chain" | "business" | "legal" | "classification" | "paths" | "evidence";
type Inspect = { kind: "evidence" | "fact"; id: string } | null;
const views: Array<[View, string]> = [["overview", "主体概览"], ["ownership", "股权与控制"], ["supply_chain", "供应链"], ["business", "经营往来"], ["legal", "司法事项"], ["classification", "行业与类型"], ["paths", "关联路径"], ["evidence", "来源证据"]];
const factNames: Record<string, string> = { HOLDS_EQUITY: "直接持股", CONTROLS: "控制关系", SUPPLIES_TO: "供应关系", PARTNERS_WITH: "合作关系", GUARANTEES: "担保", LENDS_TO: "借贷", COMPETES_WITH: "竞争关系", IN_INDUSTRY: "行业归属", MEMBER_OF_THEME: "板块归属", HAS_CLASSIFICATION: "类型归属", CONTRACT: "合同", LEGAL_CASE: "司法事项" };
factNames.ISSUED_BY = "发行公司";
const dimensions: Record<string, string> = { INDUSTRY: "行业", THEME: "板块主题", SIZE: "市值规模", STYLE: "投资风格", LISTING: "上市属性", identity: "主体身份", security: "证券映射", ownership: "股权与控制", business: "经营往来", legal: "司法事项", classification: "类型分类" };
Object.assign(dimensions, { securities: "证券映射", industry: "行业", theme: "板块主题", supply_chain: "供应链", evidence: "证据", classification: "类型分类" });
const states: Record<string, string> = { ACCEPTED: "已接受", PENDING: "待审核", REJECTED: "已拒绝", PRESENT: "已有记录", NOT_COLLECTED: "尚未采集", MAPPED: "已映射", DISCLOSURES_ONLY: "仅有披露线索", EVIDENCE_ONLY: "仅有披露线索", TEXT_AVAILABLE: "已提取正文", TITLE_ONLY: "仅有公告信息", OCR_REQUIRED: "需文字识别", UNKNOWN: "未提供正文状态" };
const entityNames: Record<string, string> = { COMPANY: "公司", PERSON: "自然人", ORGANIZATION: "机构", HOLDER_ACCOUNT: "股东账户", SECURITY: "证券", INDUSTRY: "行业", THEME: "板块主题", CLASSIFICATION: "类型分类" };
const marketNames: Record<string, string> = { CN_A: "中国A股", HK: "香港", NEEQ: "新三板", NEEQ_INNOVATION: "新三板创新层" };
const jurisdictionNames: Record<string, string> = { CN: "中国", HK: "中国香港", KY: "开曼群岛", BM: "百慕大", VG: "英属维尔京群岛", UNKNOWN: "未确认" };
const mappingNames: Record<string, string> = { MAPPED: "已映射", UNMAPPED: "待映射", SOURCE_MISSING: "缺少主体资料", NOT_APPLICABLE: "非普通股", CONFLICT: "主体资料待核实" };
const shareClassNames: Record<string, string> = { A_SHARE: "A股", H_SHARE: "H股", RED_CHIP: "红筹股", ADR: "存托凭证", ORDINARY: "普通股", NEEQ_SHARE: "新三板股份", DEPOSITARY_RECEIPT: "存托凭证" };
const propertyNames: Record<string, string> = { registered_name: "注册名称", english_name: "英文名称", company_full_name: "公司全称", registered_address: "注册地址", legal_form: "法律形式", registration_status: "登记状态", identifier_scheme: "标识体系", identifier_value: "登记标识", jurisdiction: "辖区", description: "说明", business: "主要业务", source_name: "来源名称", source_url: "来源网址", ratio: "持股比例", ratio_basis: "比例口径", shares: "持股数量", shares_unit: "数量单位", report_date: "报告日期", share_class: "股份类别", holder_rank: "股东排名", temporal_scope: "时间口径", semantic_scope: "事实口径", control_basis: "控制依据", product: "产品或服务", business_scope: "业务范围", amount: "金额", currency: "币种", case_number: "案号", court: "法院或机构", procedure_type: "程序类型", party_role: "当事人角色", case_status: "程序状态", amount_type: "金额性质", taxonomy: "行业体系", taxonomy_version: "行业版本", classification_method: "分类方法", contract_status: "合同状态", as_of: "基准日期", market_cap: "市值", threshold: "分类阈值" };
Object.assign(propertyNames, { registration_number_raw: "登记号码（来源原始值）", registration_verified: "是否完成登记核验", registration_source_kind: "登记资料来源类型", legal_representative: "法定代表人", incorporated_on: "成立日期", main_business: "主营业务", website: "公司网站", source_company_id: "来源公司编号", primary_market: "主要上市市场", industry: "所属行业", source_issuer_id: "来源主体编号", source_namespace: "数据提供方", registration_checksum_valid: "登记号码校验位是否通过", industry_label_raw: "来源行业名称", identity_basis: "主体识别依据", identity_source: "主体资料来源" });
const state = (value: string) => <span className={"cg-state " + value.toLowerCase()}>{states[value] || value}</span>;
const text = (value: unknown) => value == null || value === "" ? "未提供" : typeof value === "object" ? JSON.stringify(value) : String(value);
const errorText = (e: unknown) => e instanceof Error ? e.message : "数据加载失败";
const defKey = (dimension: string, code: string) => `${dimension.toUpperCase()}:${code}`;
const sourceNames: Record<string, string> = { CNINFO: "巨潮资讯", EASTMONEY: "东方财富", TENCENT: "腾讯财经", HKEXnews: "港交所披露易", CNINFO_SUPPLY_CHAIN_DISCLOSURES: "巨潮供应链披露", CNINFO_BUSINESS_DISCLOSURES: "巨潮经营披露", CNINFO_LEGAL_DISCLOSURES: "巨潮司法披露" };
const sourceName = (name: string) => sourceNames[name] || name;
const timeText = (value: string | null | undefined) => {
  if (!value) return "未提供";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("zh-CN", { hour12: false });
};
const records = (value: unknown): Array<Record<string, unknown>> => Array.isArray(value) ? value.filter((item): item is Record<string, unknown> => item !== null && typeof item === "object" && !Array.isArray(item)) : [];
const safeSourceUrl = (value: string | null | undefined, page?: unknown) => {
  try {
    const url = new URL(value || "");
    if (!["http:", "https:"].includes(url.protocol)) return null;
    if (typeof page === "number" && Number.isInteger(page) && page > 0) url.hash = `page=${page}`;
    return url.toString();
  } catch { return null; }
};
const disclosureState = (item: FoundationEvidence) => typeof item.metadata_json?.completeness === "string" ? item.metadata_json.completeness : "UNKNOWN";
const disclosureNotes: Record<string, string> = {
  supply_chain: "公告和年报提供供应商、客户及供销线索；实际主体、产品、期间和履约情况仍需逐条核实，匿名客户不会被推断为某家公司。",
  business: "经营公告可包含合同、中标、合作或担保线索；中标、签约与实际履约分别核实，公告金额不等于已确认收入。",
  legal: "司法披露用于核对案件和程序线索；披露公司不必然是当事人，子公司案件不直接归给母公司，未查到记录不表示没有诉讼。",
};

function propertyValue(key: string, value: unknown) {
  if (typeof value === "boolean") return value ? "是" : "否";
  if (typeof value === "string") {
    if (key === "registration_source_kind" && value === "AGGREGATOR") return "聚合数据来源";
    if (key === "identity_basis" && value === "REGISTERED_IDENTIFIER") return "依据登记标识识别主体，尚需工商核验";
    if (key === "identity_basis" && value === "SOURCE_SCOPED_IDENTIFIER") return "依据数据提供方的主体编号识别";
    if (["source_namespace", "identity_source", "source_name"].includes(key)) return sourceName(value);
    if (key === "jurisdiction") return jurisdictionNames[value] || value;
    if (key === "share_class") return shareClassNames[value] || value;
  }
  return text(value);
}
function PropertyList({ values }: { values: Record<string, unknown> }) {
  const entries = Object.entries(values || {}).filter(([, value]) => value !== null && value !== undefined && value !== "");
  return entries.length ? <dl className="cg-properties">{entries.map(([key, value]) => <div key={key}><dt>{propertyNames[key] || key}</dt><dd>{propertyValue(key, value)}</dd></div>)}</dl> : null;
}
function Coverage({ value }: { value: CompanyCoverage }) {
  return <div className="cg-coverage">{value.dimensions.map(item => <div key={item.key}><strong>{dimensions[item.key] || item.key}</strong>{state(item.status === "NOT_COLLECTED" && (item.disclosure_count || 0) > 0 ? "EVIDENCE_ONLY" : item.status)}<small>已接受 {item.count} · 待审 {item.pending_count} · 事实证据 {item.evidence_count}{item.disclosure_count !== undefined && <> · 披露 {item.disclosure_count} 份</>}</small></div>)}</div>;
}
function Securities({ rows, select }: { rows: CompanySecurity[]; select: (row: CompanySecurity) => void }) {
  return <div className="cg-table-scroll"><table><thead><tr><th>市场 / 代码</th><th>证券名称</th><th>类别</th><th>查看</th></tr></thead><tbody>{rows.map(row => <tr key={row.stock_symbol_id || row.symbol}><td><strong>{row.symbol}</strong><small>{marketNames[row.market] || row.market}</small></td><td>{row.name}</td><td>{shareClassNames[row.share_class || ""] || row.share_class || "未提供"}</td><td><button className="cg-link" type="button" onClick={() => select(row)}>打开</button></td></tr>)}</tbody></table>{!rows.length && <div className="cg-empty">尚未建立证券映射</div>}</div>;
}
function Facts({ rows, inspect }: { rows: FoundationFact[]; inspect: (value: Inspect) => void }) {
  return <div className="cg-table-scroll"><table><thead><tr><th>事实</th><th>主体与对方</th><th>状态</th><th>有效期间</th><th>证据</th></tr></thead><tbody>{rows.map(row => <tr key={row.id}><td><button className="cg-link" type="button" onClick={() => inspect({ kind: "fact", id: row.id })}>{row.title}</button><small>{factNames[row.fact_type] || row.fact_type}</small></td><td>{row.subject_name}{row.object_name && <small>关联 {row.object_name}</small>}</td><td>{state(row.status)}</td><td>{row.valid_from || "未提供"}{row.valid_to ? ` 至 ${row.valid_to}` : ""}</td><td><button className="cg-link" type="button" onClick={() => inspect({ kind: "fact", id: row.id })}>{row.evidence_ids.length} 件</button></td></tr>)}</tbody></table>{!rows.length && <div className="cg-empty">当前范围暂无记录</div>}</div>;
}
function EvidenceRows({ rows, inspect }: { rows: FoundationEvidence[]; inspect: (value: Inspect) => void }) {
  return <div className="cg-table-scroll"><table><thead><tr><th>标题</th><th>来源</th><th>发布时间</th><th>版本</th></tr></thead><tbody>{rows.map(row => <tr key={row.id}><td><button className="cg-link" type="button" onClick={() => inspect({ kind: "evidence", id: row.id })}>{row.title}</button></td><td>{sourceName(row.source_name)}{safeSourceUrl(row.url) && <a href={safeSourceUrl(row.url)!} target="_blank" rel="noreferrer" aria-label="打开来源网页"><ExternalLink size={12} /></a>}</td><td>{timeText(row.published_at)}</td><td>{row.version}</td></tr>)}</tbody></table>{!rows.length && <div className="cg-empty">尚未收录来源证据</div>}</div>;
}
function DisclosureRows({ rows, inspect }: { rows: FoundationEvidence[]; inspect: (value: Inspect) => void }) {
  return <div className="cg-table-scroll"><table><thead><tr><th>公告标题</th><th>来源 / 发布时间</th><th>正文状态</th><th>原文</th></tr></thead><tbody>{rows.map(row => {
    const url = safeSourceUrl(row.url);
    return <tr key={row.id}><td><button className="cg-link" type="button" onClick={() => inspect({ kind: "evidence", id: row.id })}>{row.title}</button><small>查看正文与线索</small></td><td>{sourceName(row.source_name)}<small>{timeText(row.published_at)}</small></td><td>{state(disclosureState(row))}{row.metadata_json?.text_truncated === true && <small>正文提取有截断</small>}</td><td>{url ? <a href={url} target="_blank" rel="noreferrer">查看原文 <ExternalLink size={12} /></a> : "来源未提供链接"}</td></tr>;
  })}</tbody></table>{!rows.length && <div className="cg-empty">本次范围尚未收录相关披露；可在“来源与治理任务”中勾选此类数据后采集。</div>}</div>;
}
function DisclosureSection({ category, facts, disclosures, inspect }: { category: "supply_chain" | "business" | "legal"; facts: FoundationFact[]; disclosures: FoundationEvidence[]; inspect: (value: Inspect) => void }) {
  return <section className="cg-section"><h3>{dimensions[category]}</h3><p className="cg-gap">{disclosureNotes[category]}披露线索不等于已确认关系。</p><h4>关系事实 <span>{facts.length}</span></h4><Facts rows={facts} inspect={inspect} /><h4>披露原文与线索 <span>{disclosures.length} 份</span></h4><DisclosureRows rows={disclosures} inspect={inspect} /></section>;
}
function Classifications({ rows, definitions, inspect }: { rows: CompanyClassification[]; definitions: Record<string, ClassificationDefinition>; inspect: (value: Inspect) => void }) {
  return <div className="cg-classifications">{rows.map(item => { const definition = item.definition || definitions[defKey(item.dimension, item.code)] || definitions[item.code]; const meaning = definition?.definition || "来源标签，术语定义尚未登记，不能仅凭名称推断含义。"; return <details key={item.id}><summary title={meaning}><span><strong>{item.label}</strong><small>{dimensions[item.dimension] || item.dimension} · {item.code}</small></span>{state(item.status)}</summary><p className="cg-term-definition">{meaning}</p>{definition && <p className="cg-meta">判定口径：{definition.criteria || "主数据未提供专门阈值"} · 来源：{definition.source_name || "未提供"} · 版本：{definition.definition_version || item.definition_version}</p>}<PropertyList values={item.properties_json} /><button type="button" onClick={() => inspect({ kind: "evidence", id: item.evidence_id })}><FileText size={14} /> 查看分类证据</button></details>; })}{!rows.length && <div className="cg-empty">当前状态下暂无分类记录</div>}</div>;
}
function EvidenceContent({ item }: { item: FoundationEvidence }) {
  const metadata = item.metadata_json || {}, pages = records(metadata.pages);
  const caseMentions = records(metadata.case_mentions), mentions = records(metadata.disclosure_mentions);
  const kinds = Array.isArray(metadata.document_kinds) ? metadata.document_kinds : [metadata.document_kind];
  const disclosure = kinds.some(kind => ["SUPPLY_CHAIN_DISCLOSURE", "BUSINESS_DISCLOSURE", "LEGAL_DISCLOSURE"].includes(String(kind)));
  const url = safeSourceUrl(item.url), completeness = disclosureState(item);
  const categoryNames: Record<string, string> = { SUPPLY_CHAIN: "供应链线索", BUSINESS: "经营线索", LEGAL: "司法线索", supply_chain: "供应链线索", business: "经营线索", legal: "司法线索" };
  return <>
    <h4>{url ? <a className="cg-source-title" href={url} target="_blank" rel="noreferrer">{item.title} <ExternalLink size={14} /></a> : item.title}</h4>
    <p className="cg-meta">{sourceName(item.source_name)} · 发布：{timeText(item.published_at)} · 收录：{timeText(item.created_at)} · 版本 {item.version}</p>
    {disclosure ? <>
      <p className="cg-meta">正文采集状态：{state(completeness)}{typeof metadata.page_count === "number" && <> · 原文 {metadata.page_count} 页</>}{pages.length > 0 && <> · 已提取 {pages.length} 页</>}</p>
      {metadata.text_truncated === true && <p className="cg-gap">正文提取达到页数或字数上限，当前内容不完整；请打开原始文件核对其余部分。</p>}
      {completeness === "OCR_REQUIRED" && <p className="cg-gap">原文可能是扫描件，尚未取得可检索文字；需文字识别后才能核对正文线索。</p>}
      {completeness === "TITLE_ONLY" && <p className="cg-gap">目前只取得公告标题和链接，尚未取得正文，不能据此确认交易对方或诉讼当事人。</p>}
      {completeness === "UNKNOWN" && <p className="cg-gap">该历史记录未登记正文提取状态，请结合原始文件核对完整性。</p>}
      {(caseMentions.length > 0 || mentions.length > 0) && <section className="cg-case-mentions"><h4>原文线索 · 待核实</h4><p className="cg-meta">以下为原文提及，尚未确认关系、当事人角色或案件结论。</p>
        {caseMentions.map((mention, index) => <article key={`case-${index}`}><div><strong>案号：{text(mention.case_number)}</strong>{state("PENDING")}{mention.page != null && <small>第 {text(mention.page)} 页</small>}{safeSourceUrl(item.url, mention.page) && <a href={safeSourceUrl(item.url, mention.page)!} target="_blank" rel="noreferrer">查看原文 <ExternalLink size={12} /></a>}</div><p>{text(mention.excerpt)}</p></article>)}
        {mentions.map((mention, index) => <article key={`mention-${index}`}><div><strong>{typeof mention.topic === "string" ? mention.topic : categoryNames[String(mention.category)] || "披露线索"}{mention.keyword != null ? `：${text(mention.keyword)}` : ""}</strong>{state("PENDING")}{mention.page != null && <small>第 {text(mention.page)} 页</small>}{safeSourceUrl(item.url, mention.page) && <a href={safeSourceUrl(item.url, mention.page)!} target="_blank" rel="noreferrer">查看原文 <ExternalLink size={12} /></a>}</div><p>{text(mention.excerpt)}</p></article>)}
      </section>}
      {pages.length > 0 ? <section className="cg-evidence-content"><h4>提取正文 · 按页查看</h4>{pages.map((page, index) => <details key={`${page.page}-${index}`}><summary>第 {text(page.page ?? index + 1)} 页</summary><pre>{typeof page.text === "string" && page.text.trim() ? page.text : "本页未提取到文字，请查看原始文件。"}</pre></details>)}</section> : completeness === "TEXT_AVAILABLE" && item.content ? <section className="cg-evidence-content"><h4>提取正文</h4><pre>{item.content}</pre></section> : null}
    </> : <><PropertyList values={Object.fromEntries(Object.entries(metadata).filter(([key]) => !["pages", "case_mentions", "disclosure_mentions"].includes(key)))} />{item.content && <section className="cg-evidence-content"><pre>{item.content}</pre></section>}</>}
  </>;
}
function Inspector({ value, close }: { value: NonNullable<Inspect>; close: () => void }) {
  const [item, setItem] = useState<FoundationEvidence | FoundationFact | null>(null); const [error, setError] = useState("");
  useEffect(() => { let active = true; setItem(null); setError(""); const call = value.kind === "evidence" ? foundationApi.evidenceDetail(value.id) : foundationApi.fact(value.id); void call.then(result => { if (active) setItem(result); }).catch(e => { if (active) setError(errorText(e)); }); return () => { active = false; }; }, [value]);
  return <section className="cg-inspector"><header><h3>{value.kind === "evidence" ? "来源证据" : "关系事实"}</h3><button type="button" onClick={close}>关闭</button></header>{error && <p className="cg-error">{error}</p>}{!item && !error && <p className="cg-meta">正在加载详情...</p>}{item && (value.kind === "evidence" ? <EvidenceContent item={item as FoundationEvidence} /> : <><h4>{item.title}</h4><p className="cg-meta">{factNames[(item as FoundationFact).fact_type] || (item as FoundationFact).fact_type}</p><PropertyList values={(item as FoundationFact).properties_json || {}} /></>)}</section>;
}
function Paths({ companyId }: { companyId: string }) {
  const [graph, setGraph] = useState<CompanyGraphData | null>(null); const [error, setError] = useState("");
  useEffect(() => { void companyGraphApi.graph({ company_id: companyId, depth: 2, limit: 100 }).then(setGraph).catch(e => setError(errorText(e))); }, [companyId]);
  return <section className="cg-section"><h3><GitBranch size={16} /> 关联路径</h3>{error && <p className="cg-error">{error}</p>}{graph && <><p className="cg-meta">节点 {graph.nodes.length} · 关系 {graph.edges.length} · 路径 {graph.paths.length}</p><ul className="cg-path-list">{graph.paths.map((path, index) => <li key={index}><div className="cg-path-track">{path.node_ids.map((id, step) => { const node = graph.nodes.find(item => item.id === id); const edge = step > 0 ? graph.edges.find(item => item.id === path.edge_ids[step - 1]) : null; return <div className="cg-path-step" key={`${id}-${step}`}>{edge && <div className={"cg-path-edge " + edge.status.toLowerCase()}><small>{factNames[edge.type] || edge.type}</small><span className="cg-edge-detail">{edge.properties_json?.ratio != null ? `持股 ${edge.properties_json.ratio}%` : edge.evidence_ids.length ? `证据 ${edge.evidence_ids.length} 件` : ""}</span></div>}<div className="cg-path-node"><strong>{node?.label || id}</strong><small>{entityNames[node?.type || ""] || node?.type || "主体"}</small></div></div>; })}</div></li>)}</ul></>}</section>;
}
function CompanyDetail({ profile, selectSecurity, inspect, definitions }: { profile: CompanyProfile; selectSecurity: (item: CompanySecurity) => void; inspect: (value: Inspect) => void; definitions: Record<string, ClassificationDefinition> }) {
  const [view, setView] = useState<View>("overview"); const allFacts = [...profile.facts.ownership, ...profile.facts.industry, ...profile.facts.business, ...profile.facts.legal, ...profile.facts.other];
  return <><div className="cg-company-heading"><div><div className="cg-identity-line"><Building2 size={16} />{entityNames[profile.company.entity_type] || profile.company.entity_type} · {jurisdictionNames[profile.company.jurisdiction] || profile.company.jurisdiction}</div><h2>{profile.company.name}</h2><p className="cg-selected-security">{profile.selected_security ? `${marketNames[profile.selected_security.market] || profile.selected_security.market}:${profile.selected_security.symbol} ${profile.selected_security.name}` : "公司主体"}</p></div><div className="cg-fact-counts"><span><strong>{allFacts.length}</strong>事实</span><span><strong>{profile.evidence.length}</strong>证据</span><span><strong>{profile.classifications.length}</strong>分类</span></div></div><Coverage value={profile.coverage} /><nav className="cg-detail-tabs">{views.map(([key, label]) => <button key={key} className={view === key ? "active" : ""} type="button" onClick={() => setView(key)}>{label}</button>)}</nav>
    {view === "overview" && <><section className="cg-section"><h3>证券映射</h3><Securities rows={profile.securities} select={selectSecurity} /></section><section className="cg-section"><h3>主体属性</h3><PropertyList values={profile.company.properties_json} /></section></>}
    {view === "ownership" && <section className="cg-section"><h3>股权与控制 <span>{profile.facts.ownership.length}</span></h3><Facts rows={profile.facts.ownership} inspect={inspect} /></section>}
    {view === "supply_chain" && <DisclosureSection category="supply_chain" facts={profile.facts.business.filter(item => item.fact_type === "SUPPLIES_TO")} disclosures={profile.supply_chain_disclosures || []} inspect={inspect} />}
    {view === "business" && <DisclosureSection category="business" facts={profile.facts.business.filter(item => item.fact_type !== "SUPPLIES_TO")} disclosures={profile.business_disclosures || []} inspect={inspect} />}
    {view === "legal" && <DisclosureSection category="legal" facts={profile.facts.legal} disclosures={profile.legal_disclosures || []} inspect={inspect} />}
    {view === "classification" && <section className="cg-section"><h3>行业与类型</h3><Classifications rows={profile.classifications} definitions={definitions} inspect={inspect} /></section>}
    {view === "paths" && <Paths companyId={profile.company.id} />}
    {view === "evidence" && <section className="cg-section"><h3>来源证据</h3><EvidenceRows rows={profile.evidence} inspect={inspect} /></section>}
  </>;
}
type CompanySelection = { kind: "security"; market: string; symbol: string; stock_symbol_id?: number | null; name?: string | null } | { kind: "company"; id: string };
const companyPageSize = 30;

export function CompanyGraphWorkbench({ initialStock, compact = false }: { initialStock?: { market: string; symbol: string; name?: string | null }; compact?: boolean }) {
  const direct = compact && !!initialStock;
  const [mode, setMode] = useState<Mode>("security"), [query, setQuery] = useState(""), [searchQuery, setSearchQuery] = useState(""), [market, setMarket] = useState("");
  const [offset, setOffset] = useState(0), [total, setTotal] = useState(0), [revision, setRevision] = useState(0), [listRevision, setListRevision] = useState(0);
  const [rows, setRows] = useState<Array<CompanySecurity | CompanySearchItem>>([]), [listLoading, setListLoading] = useState(!direct), [listError, setListError] = useState("");
  const [selection, setSelection] = useState<CompanySelection | null>(initialStock ? { kind: "security", ...initialStock } : null);
  const [profile, setProfile] = useState<CompanyProfile | null>(null), [profileLoading, setProfileLoading] = useState(!!initialStock), [error, setError] = useState("");
  const [definitions, setDefinitions] = useState<Record<string, ClassificationDefinition>>({}), [inspection, setInspection] = useState<Inspect>(null);
  useEffect(() => {
    let active = true;
    void companyGraphApi.taxonomyDefinitions().then(items => {
      if (!active) return;
      const map: Record<string, ClassificationDefinition> = {};
      items.forEach(item => { map[defKey(item.dimension, item.code)] = item; map[item.code] = item; }); setDefinitions(map);
    }).catch(() => undefined);
    return () => { active = false; };
  }, []);
  useEffect(() => {
    if (!initialStock) return;
    setProfile(null); setError(""); setProfileLoading(true); setInspection(null);
    setSelection({ kind: "security", ...initialStock });
  }, [initialStock?.market, initialStock?.symbol]);
  useEffect(() => {
    if (direct) return;
    const controller = new AbortController();
    setListLoading(true); setListError(""); setRows([]);
    const values = { q: searchQuery, market: market || undefined, limit: companyPageSize, offset };
    const call = mode === "security" ? companyGraphApi.searchSecurities(values, controller.signal) : companyGraphApi.searchCompanies(values, controller.signal);
    void call.then(result => { if (!controller.signal.aborted) { setRows(result.items); setTotal(result.total); } })
      .catch(e => { if (!controller.signal.aborted) { setListError(errorText(e)); setTotal(0); } })
      .finally(() => { if (!controller.signal.aborted) setListLoading(false); });
    return () => controller.abort();
  }, [direct, mode, searchQuery, market, offset, revision, listRevision]);
  useEffect(() => {
    if (!selection) return;
    const controller = new AbortController();
    setProfile(null); setError(""); setProfileLoading(true); setInspection(null);
    const call = selection.kind === "company" ? companyGraphApi.company(selection.id, controller.signal)
      : selection.stock_symbol_id != null ? companyGraphApi.security(selection.stock_symbol_id, controller.signal)
      : companyGraphApi.bySymbol(selection.market, selection.symbol, controller.signal);
    void call.then(value => { if (!controller.signal.aborted) setProfile(value); })
      .catch(e => { if (!controller.signal.aborted) setError(errorText(e)); })
      .finally(() => { if (!controller.signal.aborted) setProfileLoading(false); });
    return () => controller.abort();
  }, [selection, revision]);
  function chooseSecurity(row: CompanySecurity) {
    setProfile(null); setProfileLoading(true); setError(""); setInspection(null);
    setSelection({ kind: "security", market: row.market, symbol: row.symbol, stock_symbol_id: row.stock_symbol_id, name: row.name });
  }
  function choose(row: CompanySecurity | CompanySearchItem) {
    if (mode === "security") chooseSecurity(row as CompanySecurity);
    else { setProfile(null); setProfileLoading(true); setError(""); setInspection(null); setSelection({ kind: "company", id: (row as CompanySearchItem).company_id }); }
  }
  function changeMode(next: Mode) { if (next !== mode) setRows([]); setMode(next); setQuery(""); setSearchQuery(""); setMarket(""); setOffset(0); }
  const unmapped = /no company mapping|尚无公司映射|尚未.*映射/i.test(error);
  const mappingReason = unmapped ? error.replace(/^this security has no company mapping[:：]?\s*/i, "") : "";
  const selectedLabel = selection?.kind === "security" ? `${selection.name || ""}（${marketNames[selection.market] || selection.market} · ${selection.symbol}）` : "当前公司";
  const detail = <main className="cg-detail" aria-busy={profileLoading}>
    {profileLoading && <div className="cg-empty" role="status">正在加载{selectedLabel}的公司关联...</div>}
    {!profileLoading && error && <section className={unmapped ? "cg-gap" : "cg-error"} role="alert">
      <strong>{unmapped ? `${selectedLabel}尚未建立公司映射` : "公司关联加载失败"}</strong>
      <p>{unmapped ? mappingReason || "公司主体需要依据来源资料核实。可在公司关系工作台的“来源与治理任务”中选择该股票补充资料，完成后重试。" : error}</p>
      <button type="button" onClick={() => setRevision(value => value + 1)}>重新加载公司关联</button>
    </section>}
    {!profileLoading && profile && <CompanyDetail key={profile.company.id} profile={profile} selectSecurity={chooseSecurity} inspect={setInspection} definitions={definitions} />}
    {!profileLoading && !profile && !error && <div className="cg-empty">{direct ? "正在定位该股票的公司关联..." : "请选择股票或公司"}</div>}
    {inspection && <Inspector value={inspection} close={() => setInspection(null)} />}
  </main>;
  if (direct) return <section className="company-graph company-graph-direct">{detail}</section>;
  return <section className="company-graph"><EnvironmentBanner /><div className="cg-heading"><div><h2>公司关系工作台</h2><p>公司与证券双对象、关系事实和来源证据</p></div></div>
    {!compact && <CompanyGovernancePanel changed={() => setRevision(value => value + 1)} />}
    <div className="cg-layout"><aside className="cg-browser"><div className="cg-modes">
      <button className={mode === "security" ? "active" : ""} type="button" onClick={() => changeMode("security")}>股票</button>
      <button className={mode === "company" ? "active" : ""} type="button" onClick={() => changeMode("company")}>公司</button></div>
      <form className="cg-search" onSubmit={event => { event.preventDefault(); setSearchQuery(query.trim()); setOffset(0); setListRevision(value => value + 1); }}><Search size={15} />
        <input value={query} onChange={event => setQuery(event.target.value)} placeholder={mode === "security" ? "代码或证券名称" : "公司名称或登记标识"} aria-label={mode === "security" ? "搜索股票" : "搜索公司"} />
        <button type="submit" aria-label="搜索"><Search size={14} /></button></form>
      {mode === "security" && <label className="cg-market-filter">市场<select aria-label="股票市场" value={market} onChange={event => { setMarket(event.target.value); setOffset(0); }}><option value="">全部市场</option>{Object.entries(marketNames).map(([key, name]) => <option key={key} value={key}>{name}</option>)}</select></label>}
      <div className="cg-list-heading"><span>{mode === "security" ? "股票列表" : "公司列表"}</span><span>{listLoading ? "加载中" : `共 ${total.toLocaleString("zh-CN")} 条`}</span></div>
      {listError && <div className="cg-error" role="alert">{listError}<button type="button" onClick={() => setListRevision(value => value + 1)}>重试列表</button></div>}
      <div className="cg-results" aria-busy={listLoading}>{listLoading && <div className="cg-empty" role="status">正在加载{mode === "security" ? "股票" : "公司"}列表...</div>}
        {!listLoading && rows.map(row => {
          const security = row as CompanySecurity, company = row as CompanySearchItem;
          const selected = mode === "security" ? selection?.kind === "security" && selection.market === security.market && selection.symbol === security.symbol : selection?.kind === "company" && selection.id === company.company_id;
          return <button className={selected ? "active" : ""} aria-pressed={selected} title={mode === "security" ? security.mapping_reason || undefined : undefined} key={mode === "security" ? `${security.market}:${security.symbol}` : company.company_id} type="button" onClick={() => choose(row)}><span><strong>{row.name}</strong><small>{mode === "security" ? `${marketNames[security.market] || security.market} · ${security.symbol} · ${mappingNames[security.mapping_status || ""] || "待映射"}` : jurisdictionNames[company.jurisdiction] || company.jurisdiction}</small></span></button>;
        })}
        {!listLoading && !listError && !rows.length && <div className="cg-empty">未找到符合条件的{mode === "security" ? "股票" : "公司"}</div>}
      </div>
      <div className="cg-list-pages"><span>{total ? `${offset + 1}–${Math.min(offset + companyPageSize, total)}` : "0"} / {total}</span><button type="button" disabled={listLoading || offset === 0} onClick={() => setOffset(value => Math.max(0, value - companyPageSize))}>上一页</button><button type="button" disabled={listLoading || offset + companyPageSize >= total} onClick={() => setOffset(value => value + companyPageSize)}>下一页</button></div>
    </aside>{detail}</div>
  </section>;
}
export function CompanyGraphDialog({ stock, close }: { stock: { market: string; symbol: string; name?: string | null }; close: () => void }) {
  return createPortal(<div className="cg-modal-backdrop"><section className="cg-modal" role="dialog" aria-modal="true"><header><h2>{stock.name || stock.symbol} · 公司关联</h2><button type="button" onClick={close} aria-label="关闭"><X size={18} /></button></header><div className="cg-modal-body"><CompanyGraphWorkbench initialStock={stock} compact /></div></section></div>, document.body);
}
