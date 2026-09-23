import { useEffect, useId, useMemo, useRef, useState, type FormEvent, type PointerEvent as ReactPointerEvent } from "react";
import { createPortal } from "react-dom";
import { ArrowLeft, ChevronDown, ChevronLeft, ChevronRight, ChevronUp, Database, Download, Expand, Maximize2, Minimize2, Network, PanelRightClose, PanelRightOpen, RefreshCw, Search, X, ZoomIn, ZoomOut } from "lucide-react";
import { companyGraphApi, type CompanySearchItem, type CompanySecurity } from "./companyGraphApi";
import { knowledgeNetworkApi, type KnowledgeNetwork, type NetworkEdge, type NetworkEvidence, type NetworkLayer, type NetworkNode, type NetworkQuery } from "./knowledgeNetworkApi";
import { buildAllInformationGraph, buildDomainGraph, buildNavigationGroups, categoriesForCenter, effectiveNodeType, graphCategories, identityGraph, stableDomainLayout, type GraphCategory } from "./knowledgeGraphNavigation";
import "./KnowledgeGraphExplorer.css";

type Point = { x: number; y: number };
type Camera = Point & { scale: number };
type Selection = { kind: "node" | "edge"; id: string } | null;
type ExplorerProps = { initialStock?: { market: string; symbol: string; name?: string | null }; initialCompanyId?: string; initialGraphId?: number; compact?: boolean };
const marketNames: Record<string, string> = { CN_A: "A股", HK: "港股", NEEQ: "新三板", NEEQ_INNOVATION: "创新层" };
const layerNames: Record<NetworkLayer, string> = { MASTER: "主数据", FACT: "动态事实", EVIDENCE: "来源证据" };
const typeNames: Record<string, string> = {
  SECURITY: "证券", STOCK: "证券", COMPANY: "公司", LEGAL_ENTITY: "公司", ORGANIZATION: "机构", PERSON: "自然人", HOLDER_ACCOUNT: "股东账户",
  INDUSTRY: "行业", SECTOR: "板块", THEME: "板块主题", CLASSIFICATION: "类型标签", CLASSIFICATION_TAG: "类型标签", MARKET: "市场", EXCHANGE: "交易所",
  PRODUCT: "产品", MATERIAL: "原材料", SUPPLY_CHAIN_FACT: "供应链事实", OWNERSHIP_FACT: "股权事实", BUSINESS_FACT: "经营往来", LEGAL_CASE: "司法事项", LEGAL_FACT: "司法事项", COMPANY_DISCLOSURE: "公司披露线索", NAVIGATION_CATEGORY: "一级信息分类",
  FINANCIAL_OBSERVATION: "财务观测", FINANCIAL_METRIC: "财务指标", FINANCIAL_REPORT: "财务报告", MARKET_OBSERVATION: "量价观测", MARKET_EVENT: "量价事件", MARKET_ANOMALY: "量价异常", PRICE_SERIES: "历史量价", REALTIME_QUOTE: "实时行情",
  SHAREHOLDER_ACTION: "股东行为", CORPORATE_EVENT: "公司事件", EXTERNAL_EVENT: "外部事件", POLICY_EVENT: "政策事件", EVENT_MENTION: "事件提及", MENTIONED_EVENT: "事件提及", CANDIDATE_EVENT: "候选事件",
  EVIDENCE: "证据文档", EVIDENCE_DOCUMENT: "证据文档", DOCUMENT: "证据文档", SOURCE_DOCUMENT: "原始文档", NEWS: "新闻", NOTICE: "公告", RESEARCH_REPORT: "研究报告", F10_SECTION: "F10资料分区", F10_ITEM: "F10具体资料", F10_SOURCE: "F10原始资料", DATA_SOURCE: "数据源", DATASET: "数据集", COMPANY_PROFILE: "公司资料",
};
const relationNames: Record<string, string> = {
  ISSUED_BY: "发行主体", LISTED_ON: "上市于", IN_INDUSTRY: "所属行业", SUB_INDUSTRY_OF: "上级行业", IN_SECTOR: "所属板块", HAS_CLASSIFICATION: "分类标签", HAS_THEME: "所属主题",
  HOLDS_EQUITY: "持有股权", OWNS: "持有股权", SHAREHOLDER_OF: "持有股权", CONTROLS: "控制关系", SUPPLIES_TO: "供应给", PURCHASES_FROM: "采购自", SELLS_TO: "销售给", PARTNERS_WITH: "合作关系", COMPETES_WITH: "竞争关系", USES_MATERIAL: "使用原材料", DEPENDS_ON: "依赖于",
  SUBJECT: "事实主体", OBJECT: "事实客体", HAS_FACT: "关联事实", HAS_LEGAL_CASE: "司法事项", HAS_DISCLOSURE: "相关披露", INVOLVED_IN: "涉及事项", SUPPORTED_BY: "证据支持", EVIDENCED_BY: "证据来源", FROM_SOURCE: "来自数据源", MENTIONS: "文档提及", RELATED_TO: "相关联", DESCRIBED_BY: "资料描述", HAS_RECORD: "包含记录",
  HAS_FINANCIAL_OBSERVATION: "财务观测", HAS_FINANCIAL_METRIC: "财务指标", HAS_FINANCIAL_REPORT: "财务报告", HAS_MARKET_OBSERVATION: "量价观测", HAS_MARKET_EVENT: "量价事件", HAS_MARKET_ANOMALY: "量价异常", HAS_PRICE_AND_VOLUME_SERIES: "历史量价", HAS_REALTIME_QUOTE: "实时行情", HAS_SHAREHOLDER_ACTION: "股东行为", HAS_SHAREHOLDER_EVENT: "股东事件", HAS_NEWS: "关联新闻", HAS_NOTICE: "关联公告", HAS_RESEARCH_REPORT: "研究报告", HAS_F10_SECTION: "包含F10资料", HAS_F10_ITEM: "包含具体资料", HAS_POLICY_EVENT: "政策事件", HAS_EXTERNAL_EVENT: "外部事件", HAS_INFORMATION_CATEGORY: "信息分类",
};
const stateNames: Record<string, string> = { ACCEPTED: "已审核", VERIFIED: "已验证", SOURCE: "来源记录", SOURCE_REPORTED: "来源披露", NORMALIZED: "已标准化", CANDIDATE: "候选待核实", PENDING: "待核实", PENDING_REVIEW: "待审核", REJECTED: "已驳回", EXPIRED: "已过期", DERIVED: "规则推导", RAW: "原始记录", PRESENT: "已有数据", MISSING: "暂无数据", NOT_COLLECTED: "尚未采集", PARTIAL: "部分覆盖", GOVERNED: "已治理", MAPPED: "已映射", DISCLOSURES_ONLY: "仅有披露线索", EVIDENCE_ONLY: "仅有文档证据", NOT_APPLICABLE: "不适用", AVAILABLE: "已有数据" };
const fieldNames: Record<string, string> = {
  name: "名称", symbol: "证券代码", market: "市场", exchange: "交易所", share_class: "证券类别", listing_status: "上市状态", currency: "币种", list_date: "上市日期", jurisdiction: "注册地区", registration_number: "登记号码", identifier_scheme: "标识类型", identifier_value: "主体标识", company_name: "公司名称", industry: "行业", business_scope: "经营范围", introduction: "公司介绍", website: "官方网站", address: "注册地址", registered_capital: "注册资本", legal_representative: "法定代表人", established_at: "成立日期", section: "资料分区编码", section_label: "资料分区", item_count: "具体条目数", field_count: "资料字段数", content_overview: "资料内容概览", latest_period: "最新资料期间", available_content_groups: "可用内容组", content_group: "内容组", item_index: "条目序号",
  dimension: "分类维度", code: "分类编码", label: "分类名称", definition: "名词释义", criteria: "判定标准", taxonomy: "分类体系", taxonomy_version: "体系版本", definition_version: "释义版本", classification_method: "分类方法", method: "形成方法", ratio: "持股比例（%）", holding_ratio: "持股比例（%）", quantity: "数量", shares: "持股数量", amount: "金额", product: "产品", product_or_material: "产品或原材料", metric_code: "指标", metric_name: "指标名称", value: "数值", unit: "单位", report_period: "报告期", period: "报告期", report_date: "报告日期", trade_date: "交易日期", date: "日期", published_at: "发布时间", observed_at: "采集时间", as_of: "数据日期", valid_from: "生效时间", valid_to: "失效时间", available_at: "可用时间", created_at: "记录时间", updated_at: "更新时间", source_name: "数据来源", source: "数据来源", source_url: "来源地址", source_table: "来源数据表", evidence_count: "证据数", confidence: "置信度", source_reliability: "来源可信度", extraction_confidence: "抽取置信度", extraction_method: "抽取方法", verification_status: "核验状态", status: "记录状态", fact_type: "事实类型", title: "标题", content: "内容", excerpt: "内容摘要", reason: "原因", note: "说明", mapping_status: "公司映射状态", mapping_reason: "映射说明", close: "收盘价", open: "开盘价", high: "最高价", low: "最低价", volume: "成交量", turnover: "成交额", turnover_rate: "换手率", pct_change: "涨跌幅", change_percent: "涨跌幅", revenue: "营业收入", net_profit: "净利润", total_assets: "总资产", pe: "市盈率", pb: "市净率", eps: "每股收益", roe: "净资产收益率", fact_only: "仅记录事实", restated_flag: "是否重述", original_type: "原始对象类型", coverage_note: "覆盖说明", announcement_date: "公告日期", case_number: "案号", case_type: "案件类型", court: "审理法院", canonical_security_id: "统一证券标识", canonical_company_id: "统一公司标识", identity_version: "身份规则版本",
};
const valueNames: Record<string, string> = { ...marketNames, ...stateNames, A_SHARE: "A股", H_SHARE: "H股", ORDINARY: "普通股", NEEQ_SHARE: "新三板股份", DEPOSITARY_RECEIPT: "存托凭证", CN: "中国内地", UNKNOWN: "尚未确认", RULE: "规则计算", LLM: "模型抽取", HUMAN: "人工审核", AGGREGATED: "聚合计算", EASTMONEY: "东方财富", HKEX: "香港交易所", CNINFO: "巨潮资讯", LARGE_CAP: "大盘股", MID_CAP: "中盘股", SMALL_CAP: "小盘股", BLUE_CHIP: "蓝筹股", RED_CHIP: "红筹股", GROWTH: "成长型", VALUE: "价值型", INDUSTRY: "行业", THEME: "主题", STYLE: "投资风格", SIZE: "市值规模", LISTING: "上市属性" };
Object.assign(stateNames, { OBSERVED: "来源记录", SOURCE_MISSING: "来源待补充", UNMAPPED: "尚未映射", CONFLICT: "映射待核实" });
Object.assign(valueNames, { OBSERVED: "来源记录", EQUITY_UNSPECIFIED: "权益证券（细分类待核）", EQUITY: "权益证券", LISTED: "上市", ACTIVE: "正常", DELISTED: "已退市", SUSPENDED: "停牌", CNY: "人民币", HKD: "港元", USD: "美元" });
Object.assign(fieldNames, { listed_at: "上市日期", current_price: "当前价格", close_price: "收盘价", quote_time: "行情时间", latest_close: "最新收盘价", baseline_volume: "基准成交量", baseline_window: "基准窗口", threshold_multiple: "倍数阈值", threshold_pct: "比例阈值（%）", calculation: "计算说明", raw_field: "原始指标字段", raw_value: "原始数值", adjust: "复权方式", fiscal_period: "财务周期", source_issuer_id: "来源主体编号", source_org_code: "来源机构编号", legal_name: "法定名称" });
Object.assign(fieldNames, { open_price: "开盘价", high_price: "最高价", low_price: "最低价", previous_close_price: "昨收价", change_amount: "涨跌额", change_pct: "涨跌幅（%）", amount: "成交额", baseline_records: "基准交易日数", series_start: "基准起始日", volume_unit: "成交量单位", amount_unit: "成交额单位", source_record_id: "来源记录编号", document_type: "文档类型", content_truncated: "内容是否截断", matched_keyword: "命中关键词", event_type: "线索类型", source_kind: "来源类型" });
Object.assign(fieldNames, { registered_name: "注册名称", english_name: "英文名称", company_full_name: "公司全称", registered_address: "注册地址", legal_form: "法律形式", registration_status: "登记状态", registration_number_raw: "来源登记号码", registration_verified: "是否完成登记核验", registry_verified: "是否完成工商核验", registration_source_kind: "登记资料来源", incorporated_on: "成立日期", main_business: "主营业务", business: "主要业务", description: "说明", primary_market: "主要上市市场", industry_label_raw: "来源行业名称", identity_basis: "主体识别依据", identity_source: "主体资料来源", ratio_basis: "持股比例口径", shares_unit: "持股数量单位", holder_rank: "股东排名", temporal_scope: "时间口径", semantic_scope: "事实口径", control_basis: "控制依据", procedure_type: "司法程序类型", party_role: "当事人角色", case_status: "案件状态", amount_type: "金额性质", contract_status: "合同状态", market_cap: "市值", definition_source: "释义来源", definition_status: "释义状态" });
Object.assign(valueNames, { KY: "开曼群岛", BM: "百慕大", VG: "英属维尔京群岛", AGGREGATOR: "聚合数据来源", SOURCE_IDENTIFIERS: "依据来源主体标识", SOURCE_SCOPED_IDENTIFIER: "依据数据提供方主体编号", REGISTERED_IDENTIFIER: "依据登记标识，尚需工商核验", UNREGISTERED: "尚未建立专属释义", SSE: "上海证券交易所", SZSE: "深圳证券交易所", BSE: "北京证券交易所", HKEX_SDW: "港交所结算披露", USCC: "统一社会信用代码", ORG_CODE: "来源机构编号", TENCENT: "腾讯财经", HKEXnews: "港交所披露易" });
const errorText = (error: unknown) => error instanceof Error ? error.message : "图谱加载失败，请稍后重试";
const isCandidate = (item: { status?: string }) => ["CANDIDATE", "PENDING", "PENDING_REVIEW"].includes(item.status || "");
const isCenterable = (item: NetworkNode) => ["SECURITY", "STOCK", "COMPANY", "LEGAL_ENTITY"].includes(item.type) && /^(security|company):/.test(item.id);
const nodeName = (node: NetworkNode) => typeNames[effectiveNodeType(node)] || node.type_label || (node.layer === "FACT" ? "事实记录" : node.layer === "EVIDENCE" ? "来源证据" : "关联对象");
const edgeName = (edge: NetworkEdge) => edge.label && /[\u3400-\u9fff]/.test(edge.label) ? edge.label : relationNames[edge.type] || "关联关系";
const color = (node: NetworkNode) => node.type === "SECURITY" || node.type === "STOCK" ? "#2c6fba" : ["COMPANY", "LEGAL_ENTITY"].includes(node.type) ? "#257359" : ["INDUSTRY", "SECTOR", "THEME", "CLASSIFICATION", "CLASSIFICATION_TAG"].includes(node.type) ? "#8966a8" : node.layer === "EVIDENCE" ? "#8b7962" : isCandidate(node) ? "#b78224" : "#47898a";
const textValue = (value: unknown): string => value == null || value === "" ? "未提供" : typeof value === "boolean" ? value ? "是" : "否" : typeof value === "number" ? value.toLocaleString("zh-CN", { maximumFractionDigits: 6 }) : Array.isArray(value) ? value.map(textValue).join("、") : typeof value === "object" ? Object.entries(value as Record<string, unknown>).filter(([key]) => fieldNames[key]).map(([key, v]) => `${fieldNames[key]}：${textValue(v)}`).join("；") || "结构化信息（可导出查看）" : valueNames[String(value)] || String(value);
const displayNumber = (value: unknown) => typeof value === "number" ? value.toLocaleString("zh-CN", { maximumFractionDigits: 6 }) : textValue(value);
function displayNodeLabel(node: NetworkNode): string {
  const props = { ...(node.properties_json || {}), ...(node.properties || {}) } as Record<string, unknown>;
  if (effectiveNodeType(node) === "FINANCIAL_OBSERVATION") {
    const metric = String(props.metric_name || node.label || "财务指标");
    const period = String(props.report_period || "").slice(0, 10);
    const value = props.value ?? props.raw_value;
    const unit = props.unit ? ` ${String(props.unit)}` : "";
    return `${metric} · ${displayNumber(value)}${unit}${period ? ` · ${period}` : ""}`;
  }
  if (effectiveNodeType(node) === "F10_ITEM") {
    const indicator = props["指标"] || props["指标名称"] || props.metric_name;
    const value = props["值"] ?? props["数值"] ?? props.value;
    const report = props["报告名称"] || props["报告日期"] || props.report_period;
    if (indicator || value != null) return `${String(indicator || props.content_group || "F10资料")} · ${displayNumber(value)}${report ? ` · ${String(report).slice(0, 10)}` : ""}`;
    const data = props["数据"];
    if (Array.isArray(data) && data.length) {
      const first = data[0] as Record<string, unknown>;
      return `${String(first["指标"] || props.content_group || "F10资料")} · ${displayNumber(first["值"] ?? first["数值"] ?? "已披露")}`;
    }
  }
  return node.label;
}
const safeUrl = (value?: string | null) => value && /^https?:\/\//i.test(value) ? value : undefined;

/** Deterministic force layout, with the selected identity fixed at the origin. */
function layout(nodes: NetworkNode[], edges: NetworkEdge[], centerId: string): Record<string, Point> {
  const points: Record<string, Point> = {};
  const count = Math.max(1, nodes.length - 1);
  nodes.forEach((node, index) => {
    const angle = index * 2.399963;
    const radius = 150 + Math.sqrt(index / count) * Math.max(160, Math.sqrt(count) * 42);
    points[node.id] = node.id === centerId ? { x: 0, y: 0 } : { x: Math.cos(angle) * radius, y: Math.sin(angle) * radius };
  });
  for (let iteration = 0; iteration < 130; iteration++) {
    const forces: Record<string, Point> = Object.fromEntries(nodes.map(node => [node.id, { x: 0, y: 0 }]));
    for (let a = 0; a < nodes.length; a++) for (let b = a + 1; b < nodes.length; b++) {
      const pa = points[nodes[a].id], pb = points[nodes[b].id];
      const dx = pa.x - pb.x || .1, dy = pa.y - pb.y || .1, distance = Math.max(20, Math.hypot(dx, dy));
      const force = Math.min(24, 9500 / (distance * distance));
      forces[nodes[a].id].x += dx / distance * force; forces[nodes[a].id].y += dy / distance * force;
      forces[nodes[b].id].x -= dx / distance * force; forces[nodes[b].id].y -= dy / distance * force;
    }
    edges.forEach(edge => {
      const a = points[edge.source], b = points[edge.target]; if (!a || !b || edge.source === edge.target) return;
      const distance = Math.max(1, Math.hypot(b.x - a.x, b.y - a.y)), force = (distance - 155) * .025;
      forces[edge.source].x += (b.x - a.x) / distance * force; forces[edge.source].y += (b.y - a.y) / distance * force;
      forces[edge.target].x -= (b.x - a.x) / distance * force; forces[edge.target].y -= (b.y - a.y) / distance * force;
    });
    nodes.forEach(node => { if (node.id === centerId) return; const p = points[node.id], f = forces[node.id]; p.x += Math.max(-12, Math.min(12, f.x - p.x * .002)); p.y += Math.max(-12, Math.min(12, f.y - p.y * .002)); });
  }
  return points;
}

function EvidenceCard({ item, initialExpanded = false }: { item: NetworkEvidence; initialExpanded?: boolean }) {
  const openDocument = initialExpanded || ["NEWS", "NOTICE"].includes(String(item.document_type || ""));
  const [expanded, setExpanded] = useState(openDocument), [detail, setDetail] = useState<NetworkEvidence | null>(null), [busy, setBusy] = useState(false), [error, setError] = useState("");
  useEffect(() => {
    if (!expanded || detail || !item.source_table || item.source_record_id == null) return;
    const controller = new AbortController(); setBusy(true); setError("");
    void knowledgeNetworkApi.evidence(item.source_table, item.source_record_id, controller.signal).then(value => { if (!controller.signal.aborted) setDetail(value); }).catch(error => { if (!controller.signal.aborted) setError(errorText(error)); }).finally(() => { if (!controller.signal.aborted) setBusy(false); });
    return () => controller.abort();
  }, [expanded, detail, item.source_table, item.source_record_id]);
  const url = safeUrl(item.source_url || item.url);
  return <article className="kg-evidence"><strong>{item.title || "来源记录"}</strong><small>{textValue(item.source_name || item.source_table || "来源未标注")}</small>{item.published_at && <small>发布：{item.published_at}</small>}{item.observed_at && <small>采集：{item.observed_at}</small>}{(item.excerpt || item.content) && <p>{item.excerpt || item.content?.slice(0, 1200)}</p>}{item.content_truncated && <small>此处展示摘要，完整内容以来源原文为准。</small>}{url && <a href={url} target="_blank" rel="noopener noreferrer">查看来源原文 ↗</a>}{item.source_table && item.source_record_id != null && <button type="button" className="kg-source-expand" onClick={() => setExpanded(old => !old)}>{expanded ? "收起入库原文" : "展开入库原文"}</button>}{expanded && <>{busy && <small role="status">正在读取原始资料…</small>}{error && <p className="kg-error" role="alert">{error}</p>}{detail && <div className="kg-source-content"><p>{detail.content || "此记录未提供正文。"}</p>{detail.content_truncated && <small>原文超过展示上限，可前往数据来源查看。</small>}</div>}</>}</article>;
}

export function KnowledgeGraphExplorer({ initialStock, initialCompanyId, initialGraphId, compact = false }: ExplorerProps) {
  const initialQuery: NetworkQuery | null = initialStock ? { market: initialStock.market, symbol: initialStock.symbol } : initialCompanyId ? { company_id: initialCompanyId } : initialGraphId ? { graph_id: initialGraphId } : null;
  const [center, setCenter] = useState<NetworkQuery | null>(initialQuery);
  const [history, setHistory] = useState<NetworkQuery[]>([]);
  const [data, setData] = useState<KnowledgeNetwork | null>(null), [loading, setLoading] = useState(false), [error, setError] = useState("");
  const [revision, setRevision] = useState(0), [depth, setDepth] = useState(1), [nodeLimit, setNodeLimit] = useState(120), [candidates, setCandidates] = useState(false);
  const [perCategory, setPerCategory] = useState(3);
  const [layers, setLayers] = useState<NetworkLayer[]>(["MASTER", "FACT"]), [selection, setSelection] = useState<Selection>(null), [labels, setLabels] = useState(false);
  const [hiddenTypes, setHiddenTypes] = useState<string[]>([]);
  const [navigation, setNavigation] = useState<"overview" | "all" | "domain" | "advanced">("overview");
  const [category, setCategory] = useState<GraphCategory | null>(null), [categoryPage, setCategoryPage] = useState(0), [categoryType, setCategoryType] = useState("");
  const [expandedCategories, setExpandedCategories] = useState<GraphCategory[]>([]);
  const [domainsExpanded, setDomainsExpanded] = useState(true), [inspectorExpanded, setInspectorExpanded] = useState(true), [fullscreen, setFullscreen] = useState(false);
  const [mode, setMode] = useState<"security" | "company">("security"), [query, setQuery] = useState(""), [searchQuery, setSearchQuery] = useState(""), [market, setMarket] = useState("");
  const [searchOpen, setSearchOpen] = useState(!initialQuery), [offset, setOffset] = useState(0), [searchRevision, setSearchRevision] = useState(0);
  const [results, setResults] = useState<Array<CompanySecurity | CompanySearchItem>>([]), [total, setTotal] = useState(0), [searchBusy, setSearchBusy] = useState(false), [searchError, setSearchError] = useState("");
  const autoSelect = useRef(!initialQuery);
  const [positions, setPositions] = useState<Record<string, Point>>({}), [camera, setCamera] = useState<Camera>({ x: 450, y: 300, scale: 1 });
  const [size, setSize] = useState({ width: 900, height: 590 });
  const svgRef = useRef<SVGSVGElement>(null), canvasRef = useRef<HTMLDivElement>(null), layoutRef = useRef<HTMLDivElement>(null);
  const pointer = useRef<{ id?: string; x: number; y: number; start: Point; camera: Camera; moved: boolean } | null>(null);
  const markerId = `kg-arrow-${useId().replace(/:/g, "")}`;
  useEffect(() => {
    const next = initialStock ? { market: initialStock.market, symbol: initialStock.symbol } : initialCompanyId ? { company_id: initialCompanyId } : initialGraphId ? { graph_id: initialGraphId } : null;
    autoSelect.current = !next;
    setCenter(next); setHistory([]); setHiddenTypes([]); setSearchOpen(!next); setNavigation("overview"); setCategory(null); setExpandedCategories([]); setCategoryPage(0); setCategoryType("");
  }, [initialStock?.market, initialStock?.symbol, initialCompanyId, initialGraphId]);
  useEffect(() => {
    if (!searchOpen) return;
    const controller = new AbortController(); setSearchBusy(true); setSearchError("");
    const params = { q: searchQuery, market, limit: 12, offset };
    const call = mode === "security" ? companyGraphApi.searchSecurities(params, controller.signal) : companyGraphApi.searchCompanies(params, controller.signal);
    void call.then(result => { if (controller.signal.aborted) return; setResults(result.items); setTotal(result.total); if (autoSelect.current && result.items.length) { const item = result.items[0] as CompanySecurity; setCenter({ market: item.market, symbol: item.symbol }); autoSelect.current = false; } }).catch(e => { if (!controller.signal.aborted) { setSearchError(errorText(e)); setResults([]); } }).finally(() => { if (!controller.signal.aborted) setSearchBusy(false); });
    return () => controller.abort();
  }, [mode, searchQuery, market, offset, searchOpen, searchRevision]);
  useEffect(() => {
    if (!center) { setData(null); setLoading(false); setError(""); return; }
    const controller = new AbortController(); setLoading(true); setError(""); setData(null); setSelection(null);
    const timeout = window.setTimeout(() => { controller.abort(); setLoading(false); setError("当前图谱读取超时，请重试或减少关联深度及对象上限。"); }, 60000);
    void knowledgeNetworkApi.explore({ ...center, depth, max_nodes: nodeLimit, max_edges: nodeLimit * 2, include_candidates: candidates, include_evidence: true, per_category: perCategory, ...(category && navigation === "domain" ? { category } : {}) }, controller.signal).then(result => { if (!controller.signal.aborted) setData(result); }).catch(e => { if (!controller.signal.aborted) setError(errorText(e)); }).finally(() => { window.clearTimeout(timeout); if (!controller.signal.aborted) setLoading(false); });
    return () => { window.clearTimeout(timeout); controller.abort(); };
  }, [center, depth, nodeLimit, perCategory, candidates, revision, category, navigation]);
  const domain = useMemo(() => buildDomainGraph(data, category || "identity", categoryPage, categoryType, 6), [data, category, categoryPage, categoryType]);
  const identity = useMemo(() => identityGraph(data), [data]);
  const groups = useMemo(() => buildNavigationGroups(data), [data]);
  const centerCategories = useMemo(() => categoriesForCenter(data), [data]);
  const allInformation = useMemo(() => buildAllInformationGraph(data), [data]);
  const domainDrill = useMemo(() => {
    if (navigation !== "domain" || selection?.kind !== "node" || !data) return null;
    const parent = data.nodes.find(node => node.id === selection.id);
    if (!parent || effectiveNodeType(parent) !== "F10_SECTION") return null;
    const childEdges = data.edges.filter(edge => edge.source === parent.id && edge.type === "HAS_F10_ITEM");
    const childIds = new Set(childEdges.map(edge => edge.target));
    const contextIds = new Set([...identity.nodes.map(node => node.id), parent.id, ...childIds]);
    const contextEdges = data.edges.filter(edge => contextIds.has(edge.source) && contextIds.has(edge.target)
      && (edge.type === "ISSUED_BY" || edge.type === "HAS_F10_SECTION" || edge.type === "HAS_F10_ITEM"));
    return { nodes: data.nodes.filter(node => contextIds.has(node.id)), edges: contextEdges };
  }, [data, identity.nodes, navigation, selection]);
  const visible = useMemo(() => {
    if (navigation === "overview") {
      const ids = new Set(identity.nodes.map(node => node.id));
      return { nodes: identity.nodes, edges: identity.edges.filter(edge => ids.has(edge.source) && ids.has(edge.target)) };
    }
    if (navigation === "all") return allInformation;
    if (navigation === "domain") {
      const base = allInformation;
      const expanded = !!category && expandedCategories.includes(category);
      const detail = domainDrill || { nodes: domain.nodes, edges: domain.edges };
      const categoryNodeId = category ? `ui-category:${category}` : "";
      const categoryNodes = base.nodes.filter(node => node.type === "NAVIGATION_CATEGORY");
      const selectedF10 = selection?.kind === "node" ? data?.nodes.find(node => node.id === selection.id) : undefined;
      const rawDetailNodes = expanded ? detail.nodes.filter(node => node.id !== data?.center_id && !identity.nodes.some(core => core.id === node.id)
        && !(category === "disclosure" && !domainDrill && effectiveNodeType(node) === "F10_ITEM")
        && (!selectedF10 || effectiveNodeType(selectedF10) !== "F10_SECTION" || domainDrill?.nodes.some(child => child.id === node.id))) : [];
      const detailNodes: NetworkNode[] = rawDetailNodes.map(node => ({ ...node, properties: { ...(node.properties || {}),
        ui_graph_level: domainDrill && node.id !== selection?.id ? 3 : 2 } as Record<string, unknown> }));
      const nodes = [...base.nodes, ...detailNodes].filter((node, index, all) => all.findIndex(item => item.id === node.id) === index);
      const detailEdges = expanded ? detail.edges.filter(edge => nodes.some(node => node.id === edge.source) && nodes.some(node => node.id === edge.target)) : [];
      const categoryTargets = domainDrill && selection?.kind === "node" ? detailNodes.filter(node => node.id === selection.id) : detailNodes;
      const categoryDetailEdges = expanded && categoryNodeId ? categoryTargets.map(node => ({
        id: `ui-category-detail:${categoryNodeId}:${node.id}`, source: categoryNodeId, target: node.id,
        type: "HAS_INFORMATION_CATEGORY", type_label: "分类明细", label: "展开明细", layer: "MASTER" as const,
        status: "UI_NAVIGATION", properties: { grouping_is_ui_only: true }, evidence_ids: [],
      })) : [];
      const edges = [...base.edges, ...detailEdges, ...categoryDetailEdges].filter((edge, index, all) => all.findIndex(item => item.id === edge.id) === index);
      return { nodes, edges, categoryNodes };
    }
    const nodes = (data?.nodes || []).filter(node => node.id === data?.center_id || (layers.includes(node.layer) && !hiddenTypes.includes(node.type)));
    const ids = new Set(nodes.map(node => node.id));
    return { nodes, edges: (data?.edges || []).filter(edge => ids.has(edge.source) && ids.has(edge.target) && (!edge.layer || layers.includes(edge.layer))) };
  }, [data, layers, hiddenTypes, navigation, identity, domain, domainDrill, allInformation, category, expandedCategories]);
  const arranged = useMemo(() => navigation === "overview" ? stableDomainLayout(visible.nodes, data?.center_id || "", identity.nodes.map(node => node.id), true) : navigation === "all" ? stableDomainLayout(visible.nodes, data?.center_id || "", identity.nodes.map(node => node.id)) : navigation === "domain" ? stableDomainLayout(visible.nodes, data?.center_id || "", identity.nodes.map(node => node.id), false, category && expandedCategories.includes(category) ? category : undefined) : layout(visible.nodes, visible.edges, data?.center_id || ""), [visible, data?.center_id, navigation, identity.nodes, category, expandedCategories]);
  function fitted(points: Record<string, Point>, dimensions = size): Camera {
    const values = Object.values(points); if (!values.length) return { x: dimensions.width / 2, y: dimensions.height / 2, scale: 1 };
    const xs = values.map(p => p.x), ys = values.map(p => p.y), left = Math.min(...xs) - 112, right = Math.max(...xs) + 112, top = Math.min(...ys) - 70, bottom = Math.max(...ys) + 70;
    const scale = Math.min(navigation === "advanced" ? 1.45 : 1.15, Math.max(.1, Math.min(dimensions.width / (right - left), dimensions.height / (bottom - top)) * .9));
    return { x: dimensions.width / 2 - (left + right) / 2 * scale, y: dimensions.height / 2 - (top + bottom) / 2 * scale, scale };
  }
  useEffect(() => { setPositions(arranged); setCamera(fitted(arranged)); }, [arranged, size.width, size.height]);
  useEffect(() => {
    const element = canvasRef.current; if (!element) return;
    const observer = new ResizeObserver(([entry]) => { const { width, height } = entry.contentRect; if (width && height) setSize({ width, height }); }); observer.observe(element); return () => observer.disconnect();
  }, []);
  useEffect(() => {
    const changed = () => setFullscreen(document.fullscreenElement === layoutRef.current);
    document.addEventListener("fullscreenchange", changed);
    return () => document.removeEventListener("fullscreenchange", changed);
  }, []);
  useEffect(() => {
    const svg = svgRef.current; if (!svg) return;
    const wheel = (event: WheelEvent) => { event.preventDefault(); const box = svg.getBoundingClientRect(), x = event.clientX - box.left, y = event.clientY - box.top; setCamera(old => { const scale = Math.min(4, Math.max(.08, old.scale * Math.exp(-event.deltaY * .0015))); return { x: x - (x - old.x) * scale / old.scale, y: y - (y - old.y) * scale / old.scale, scale }; }); };
    svg.addEventListener("wheel", wheel, { passive: false }); return () => svg.removeEventListener("wheel", wheel);
  }, []);
  function navigate(next: NetworkQuery) { autoSelect.current = false; if (center) setHistory(old => [...old.slice(-14), center]); setCenter(next); setHiddenTypes([]); setSearchOpen(false); setNavigation("overview"); setCategory(null); setExpandedCategories([]); setCategoryPage(0); setCategoryType(""); }
  function openOverview() { setNavigation("overview"); setCategory(null); setExpandedCategories([]); setCategoryPage(0); setCategoryType(""); setSelection(null); }
  function openCategory(key: GraphCategory) {
    if (navigation === "domain" && category === key) {
      setExpandedCategories(old => old.includes(key) ? old.filter(item => item !== key) : [...old, key]);
      setSelection(null);
      return;
    }
    setCategory(key); setExpandedCategories(old => old.includes(key) ? old : [...old, key]); setCategoryPage(0); setCategoryType(""); setSelection(null); setNavigation("domain");
  }
  function openAllInformation() { setNavigation("all"); setCategory(null); setExpandedCategories([]); setCategoryPage(0); setCategoryType(""); setSelection(null); }
  async function toggleFullscreen() {
    if (document.fullscreenElement === layoutRef.current) await document.exitFullscreen();
    else await layoutRef.current?.requestFullscreen();
  }
  function centerNode(node: NetworkNode) { if (isCenterable(node)) navigate({ center_id: node.id }); }
  function onSearch(event: FormEvent) { event.preventDefault(); autoSelect.current = false; setSearchQuery(query.trim()); setOffset(0); setSearchOpen(true); setSearchRevision(old => old + 1); }
  function pointerDown(event: ReactPointerEvent<SVGSVGElement>) {
    if (event.button !== 0) return;
    const node = (event.target as Element).closest("[data-node-id]");
    const id = node?.getAttribute("data-node-id") || undefined;
    pointer.current = { id, x: event.clientX, y: event.clientY, start: id ? positions[id] : { x: camera.x, y: camera.y }, camera, moved: false };
    // Preserve node/edge click targets while retaining pointer movement on drag.
    const captureTarget = node || (event.target as Element).closest(".kg-edge") || event.currentTarget;
    (captureTarget as SVGElement).setPointerCapture(event.pointerId);
  }
  function pointerMove(event: ReactPointerEvent<SVGSVGElement>) {
    const drag = pointer.current; if (!drag) return;
    const dx = event.clientX - drag.x, dy = event.clientY - drag.y; if (Math.hypot(dx, dy) > 3) drag.moved = true;
    if (drag.id) { const id = drag.id; setPositions(old => ({ ...old, [id]: { x: drag.start.x + dx / drag.camera.scale, y: drag.start.y + dy / drag.camera.scale } })); }
    else setCamera({ ...drag.camera, x: drag.start.x + dx, y: drag.start.y + dy });
  }
  function pointerUp(event: ReactPointerEvent<SVGSVGElement>) {
    if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId);
    pointer.current = null;
  }
  function zoom(factor: number) { setCamera(old => { const scale = Math.min(4, Math.max(.08, old.scale * factor)); return { x: size.width / 2 - (size.width / 2 - old.x) * scale / old.scale, y: size.height / 2 - (size.height / 2 - old.y) * scale / old.scale, scale }; }); }
  function download(kind: "json" | "svg") {
    if (!data) return;
    const body = kind === "json" ? JSON.stringify({ ...data, display: { layers, mode: navigation, category, page: domain.page, grouping_is_ui_only: true, nodes: visible.nodes.map(node => node.id), edges: visible.edges.map(edge => edge.id) } }, null, 2) : new XMLSerializer().serializeToString(svgRef.current!);
    const url = URL.createObjectURL(new Blob([body], { type: kind === "json" ? "application/json;charset=utf-8" : "image/svg+xml;charset=utf-8" }));
    const a = document.createElement("a"); a.href = url; a.download = `知识图谱-${data.center_id.replace(/[^\w-]/g, "_")}.${kind}`; a.click(); setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
  const item = selection?.kind === "node" ? data?.nodes.find(node => node.id === selection.id) : selection?.kind === "edge" ? data?.edges.find(edge => edge.id === selection.id) : undefined;
  const props: Record<string, unknown> = item ? { ...(item.properties_json || {}), ...(item.properties || {}), ...(item.valid_from ? { valid_from: item.valid_from } : {}), ...(item.valid_to ? { valid_to: item.valid_to } : {}), ...(item.observed_at ? { observed_at: item.observed_at } : {}), ...(item.extraction_method ? { extraction_method: item.extraction_method } : {}) } : {};
  const evidence = item ? Array.from(new Map([...(item.evidence || []), ...(data?.evidence || []).filter(e => item.evidence_ids?.includes(e.id))].map(e => [e.id, e])).values()) : [];
  const currentNode = data?.nodes.find(node => node.id === data.center_id);
  const selectedNode = selection?.kind === "node" ? item as NetworkNode | undefined : undefined;
  const selectedEdge = selection?.kind === "edge" ? item as NetworkEdge | undefined : undefined;
  const properties = Object.entries(props).filter(([key, value]) => fieldNames[key] && value != null && value !== "");
  const neighbors = new Set(selection?.kind === "node" ? [selection.id, ...visible.edges.filter(edge => edge.source === selection.id || edge.target === selection.id).flatMap(edge => [edge.source, edge.target])] : selection?.kind === "edge" ? visible.edges.filter(edge => edge.id === selection.id).flatMap(edge => [edge.source, edge.target]) : []);
  const domainInfo = groups.find(group => group.key === category);
  const types = Array.from(new Map((data?.nodes || []).filter(node => layers.includes(node.layer)).map(node => [node.type, node])).values());
  return <section className={`knowledge-explorer kg-view-${navigation}${compact ? " kg-compact" : ""}`} aria-label="知识图谱可视化">
    <header className="kg-heading"><div><span className="kg-kicker"><Network size={15} /> 证券 · 公司 · 事实 · 证据</span><h2>知识图谱</h2><p>沿股票与公司关系追溯数据，点击对象或关系查看来源。</p></div><div className="kg-heading-actions"><button type="button" disabled={!data} onClick={() => download("json")}><Download size={14} />导出数据</button><button type="button" disabled={!data} onClick={() => download("svg")}>导出画布</button></div></header>
    <form className="kg-search" onSubmit={onSearch}><select aria-label="图谱搜索对象" value={mode} onChange={event => { autoSelect.current = false; setResults([]); setTotal(0); setMode(event.target.value as "security" | "company"); setOffset(0); setSearchOpen(true); }}><option value="security">股票</option><option value="company">公司</option></select><select aria-label="图谱搜索市场" value={market} onChange={event => { autoSelect.current = false; setMarket(event.target.value); setOffset(0); setSearchOpen(true); }}><option value="">全部市场</option>{Object.entries(marketNames).map(([key, name]) => <option value={key} key={key}>{name}</option>)}</select><input aria-label="图谱搜索关键词" value={query} onChange={event => setQuery(event.target.value)} placeholder={mode === "security" ? "输入股票代码或名称" : "输入公司名称或主体标识"} /><button type="submit"><Search size={15} />搜索</button><button type="button" className="kg-result-toggle" onClick={() => setSearchOpen(old => !old)}>{searchOpen ? "收起列表" : "股票 / 公司列表"}</button></form>
    {searchOpen && <section className="kg-results" aria-label="图谱搜索结果"><div className="kg-results-heading"><span>{searchBusy ? "正在搜索…" : `共 ${total.toLocaleString()} 个${mode === "security" ? "证券" : "公司"}`}</span><div><button type="button" aria-label="图谱搜索上一页" disabled={!offset || searchBusy} onClick={() => setOffset(old => Math.max(0, old - 12))}><ChevronLeft size={14} /></button><span>{total ? `${offset + 1}–${Math.min(offset + 12, total)}` : "0"}</span><button type="button" aria-label="图谱搜索下一页" disabled={offset + 12 >= total || searchBusy} onClick={() => setOffset(old => old + 12)}><ChevronRight size={14} /></button></div></div>{searchError && <p className="kg-error" role="alert">{searchError}</p>}<div className="kg-result-grid">{!searchBusy && results.map(result => { const security = mode === "security" ? result as CompanySecurity : undefined, company = mode === "company" ? result as CompanySearchItem : undefined; return <button type="button" key={security ? `${security.market}:${security.symbol}` : company!.company_id} onClick={() => navigate(security ? { market: security.market, symbol: security.symbol } : { company_id: company!.company_id })}><strong>{result.name}</strong><small>{security ? `${marketNames[security.market] || security.market} · ${security.symbol}` : `${company!.securities.length} 条证券映射`}</small>{security?.mapping_status && security.mapping_status !== "MAPPED" && <small title={security.mapping_reason || undefined}>{stateNames[security.mapping_status] || "公司映射待核实"}</small>}</button>; })}</div>{!searchBusy && !results.length && !searchError && <p className="kg-empty">没有匹配记录，请调整关键词或市场。</p>}</section>}
    <div className="kg-controls"><div className="kg-focus"><button type="button" title="返回上一个中心" aria-label="返回上一个中心" disabled={!history.length} onClick={() => { setCenter(history[history.length - 1]); setHistory(old => old.slice(0, -1)); openOverview(); }}><ArrowLeft size={15} /></button><strong>{loading ? "正在构建当前子图…" : currentNode?.label || "请选择股票或公司"}</strong>{data && <span>{visible.nodes.length} 个对象 · {visible.edges.length} 条关系</span>}</div><div className="kg-query-controls"><button type="button" className="kg-secondary-nav" data-testid="graph-overview" aria-label="返回图谱总览" onClick={openOverview}>总览</button><button type="button" className="kg-secondary-nav" data-testid="graph-all-information" aria-label="查看全部信息" onClick={openAllInformation}>全部信息</button><button type="button" className="kg-secondary-nav" data-testid="graph-advanced" aria-label="打开原始关系视图" onClick={() => { setNavigation("advanced"); setSelection(null); }}>原始关系</button><button type="button" aria-label="刷新图谱" disabled={!center || loading} onClick={() => setRevision(old => old + 1)}><RefreshCw size={15} /></button></div></div>
    {navigation === "advanced" && <><div className="kg-filters"><span>高级设置 · 显示图层</span>{(Object.keys(layerNames) as NetworkLayer[]).map(layer => <label key={layer}><input type="checkbox" checked={layers.includes(layer)} onChange={() => setLayers(old => old.includes(layer) ? old.filter(item => item !== layer) : [...old, layer])} />{layerNames[layer]}</label>)}<label className="kg-candidate-toggle"><input type="checkbox" checked={candidates} onChange={event => setCandidates(event.target.checked)} />包含候选关系</label><label><input type="checkbox" checked={labels} onChange={event => setLabels(event.target.checked)} />关系名称</label><label>关联深度<select aria-label="关联深度" value={depth} onChange={e => setDepth(Number(e.target.value))}><option value={1}>1 层</option><option value={2}>2 层</option><option value={3}>3 层</option></select></label><label>对象上限<select aria-label="对象上限" value={nodeLimit} onChange={e => setNodeLimit(Number(e.target.value))}><option value={60}>60</option><option value={120}>120</option><option value={200}>200</option><option value={400}>400（明细上限）</option></select></label><label>每类资料<select aria-label="每类资料上限" value={perCategory} onChange={e => setPerCategory(Number(e.target.value))}><option value={3}>3 条</option><option value={5}>5 条</option><option value={10}>10 条</option></select></label><span className="kg-candidate-key">虚线表示待核实</span></div><p className="kg-advanced-note">原始关系用于核查底层明细，可能包含重复期间观测、F10 条目及证据节点；日常浏览建议使用“全部信息”逐类下钻。</p></>}
    {navigation === "domain" && category && <section className="kg-domain-browser" aria-label="领域图谱浏览"><nav className="kg-breadcrumb" aria-label="图谱层级"><button type="button" data-testid="graph-back-overview" aria-label="返回图谱总览" onClick={openOverview}>图谱总览</button><span>›</span><button type="button" className="kg-current-domain" aria-label="返回领域图谱" onClick={() => setSelection(null)}>{graphCategories.find(entry => entry.key === category)?.label}</button>{item && <><span>›</span><span className="kg-breadcrumb-record" title={item.label}>{selection?.kind === "edge" ? edgeName(item as NetworkEdge) : item.label}</span></>}<span className="kg-domain-count">{domainDrill ? `已展开 ${domainDrill.nodes.length - identity.nodes.length - 1} 条具体资料` : `已载入 ${domain.total} 个对象 · 第 ${domain.pages ? domain.page + 1 : 0}/${domain.pages || 0} 页`}</span></nav><div className="kg-domain-toolbar"><label>领域<select aria-label="切换图谱领域" value={category} onChange={event => openCategory(event.target.value as GraphCategory)}>{centerCategories.map(group => <option key={group.key} value={group.key}>{group.label}</option>)}</select></label><label>对象类型<select aria-label="筛选对象类型" value={categoryType} onChange={event => { setCategoryType(event.target.value); setCategoryPage(0); setSelection(null); }}><option value="">全部类型</option>{domain.types.map(type => <option value={type} key={type}>{typeNames[type] || "关联对象"}</option>)}</select></label><label className="kg-guided-candidate"><input type="checkbox" checked={candidates} onChange={event => setCandidates(event.target.checked)} />包含待核实线索</label><span className="kg-domain-pagination">每页 6 个对象<button type="button" data-testid="graph-domain-prev" aria-label="领域上一页" disabled={domain.page <= 0 || !!domainDrill} onClick={() => { setCategoryPage(page => Math.max(0, page - 1)); setSelection(null); }}><ChevronLeft size={14} /></button><button type="button" data-testid="graph-domain-next" aria-label="领域下一页" disabled={domain.page + 1 >= domain.pages || !!domainDrill} onClick={() => { setCategoryPage(page => page + 1); setSelection(null); }}><ChevronRight size={14} /></button></span></div>{domain.omittedContext && <p className="kg-warning">为保持画布清晰，部分中间路径未展开；点击对象后可查看属性和证据。</p>}{!loading && !error && !domain.total && <p className="kg-domain-empty">{domainInfo?.sourceCount ? "此范围已有来源记录，但当前样本尚无可展开的这类对象。可调整类型筛选或读取更多资料。" : "当前查询范围尚无可展示资料，未采集不代表不存在相关事实。"}</p>}<div className="kg-domain-scope"><span>{domainDrill ? "当前显示所选 F10 分区中的具体资料条目；点击条目可查看字段与原始证据。" : `${domainInfo?.coverage.map(row => `${row.label} ${row.count.toLocaleString()}`).join(" · ") || "来源详情以对象及证据中的记录为准"}。图谱对象数与原始记录数口径不同。`}</span><button type="button" disabled={loading || perCategory >= 10 && nodeLimit >= 200} onClick={() => { setPerCategory(old => Math.min(10, old + 5)); setNodeLimit(200); }}>读取更多资料</button></div></section>}
    {data?.truncated && <details className="kg-notes"><summary>当前为有限样本，查看范围说明</summary><p>动态资料每类最多读取 {perCategory} 条来源记录。领域中的分页仅浏览已载入对象，不代表全部历史数据；点击“读取更多资料”或切换具体证券可扩大范围。</p></details>}
    {!!center?.graph_id && !!data?.centers?.length && <label className="kg-legacy-centers">旧图谱中的股票<select aria-label="旧图谱中心股票" value={data.center_id} onChange={event => navigate({ graph_id: center.graph_id, center_id: event.target.value })}>{data.centers.map(item => <option key={item.id} value={item.id}>{item.label}</option>)}</select><small>双击公司或其他证券可进入其完整关联范围。</small></label>}
    {data?.notes?.length ? <details className="kg-notes"><summary>数据范围与缺口说明（{data.notes.length}）</summary>{data.notes.map((note, index) => <p key={index}>{note}</p>)}</details> : null}
    {navigation === "all" && <section className="kg-hierarchy-status"><strong>全量数据 · 核心分类</strong><span>当前以{currentNode?.type === "COMPANY" ? "公司" : "证券"}为中心；原始记录和证据不在此处铺开，点击分类继续下钻。</span></section>}
    <div className={`kg-layout${fullscreen ? " is-fullscreen" : ""}${inspectorExpanded ? "" : " inspector-collapsed"}`} ref={layoutRef}><div className="kg-canvas-column"><div className="kg-canvas" ref={canvasRef}>
      <svg ref={svgRef} xmlns="http://www.w3.org/2000/svg" width={size.width} height={size.height} viewBox={`0 0 ${size.width} ${size.height}`} aria-label="股票公司关系画布" onPointerDown={pointerDown} onPointerMove={pointerMove} onPointerUp={pointerUp} onPointerCancel={() => { pointer.current = null; }}>
        <defs><marker id={markerId} viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z" fill="#81918c" /></marker></defs><rect width="100%" height="100%" fill="#f7faf7" />
        <g transform={`translate(${camera.x} ${camera.y}) scale(${camera.scale})`}>
          {visible.edges.map(edge => {
            const a = positions[edge.source], b = positions[edge.target]; if (!a || !b) return null;
            const dx = b.x - a.x, dy = b.y - a.y, length = Math.max(1, Math.hypot(dx, dy));
            const radius = navigation === "advanced" ? edge.target === data?.center_id ? 35 : 25 : Math.min(93 / Math.max(.01, Math.abs(dx / length)), 33 / Math.max(.01, Math.abs(dy / length)));
            const siblings = visible.edges.filter(item => (item.source === edge.source && item.target === edge.target) || (item.source === edge.target && item.target === edge.source));
            const bend = (siblings.findIndex(item => item.id === edge.id) - (siblings.length - 1) / 2) * 46 * (edge.source < edge.target ? 1 : -1);
            const control = { x: (a.x + b.x) / 2 - dy / length * bend, y: (a.y + b.y) / 2 + dx / length * bend };
            const tangent = Math.max(1, Math.hypot(b.x - control.x, b.y - control.y));
            const end = { x: b.x - (b.x - control.x) / tangent * radius, y: b.y - (b.y - control.y) / tangent * radius };
            const path = edge.source === edge.target ? `M ${a.x + 19} ${a.y - 19} C ${a.x + 95} ${a.y - 105}, ${a.x - 95} ${a.y - 105}, ${a.x - 19} ${a.y - 19}` : `M ${a.x} ${a.y} Q ${control.x} ${control.y} ${end.x} ${end.y}`;
            const selected = selection?.id === edge.id, near = selection?.kind === "node" && (edge.source === selection.id || edge.target === selection.id);
            const shown = labels || selected || near || edge.type === "ISSUED_BY";
            return <g key={edge.id} className="kg-edge" role="button" tabIndex={0} opacity={selection && !selected && !near ? .18 : 1}
              aria-label={`${data?.nodes.find(n => n.id === edge.source)?.label || "主体"} ${edgeName(edge)} ${data?.nodes.find(n => n.id === edge.target)?.label || "客体"}`}
              onClick={() => setSelection({ kind: "edge", id: edge.id })}
              onKeyDown={event => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); setSelection({ kind: "edge", id: edge.id }); } }}>
              <title>{edgeName(edge)} · {stateNames[edge.status || ""] || "来源关系"}</title>
              <path d={path} fill="none" stroke="transparent" strokeWidth="16" />
              <path d={path} fill="none" stroke={selected || near ? "#257359" : isCandidate(edge) ? "#b78224" : "#a6b6af"} strokeWidth={selected || near ? 2.5 : 1.3}
                strokeDasharray={isCandidate(edge) ? "6 5" : undefined} markerEnd={`url(#${markerId})`} />
              {shown && <text x={(a.x + 2 * control.x + end.x) / 4} y={(a.y + 2 * control.y + end.y) / 4 - 7}
                textAnchor="middle" fontSize="10" fontFamily="sans-serif" fill="#53665d" paintOrder="stroke" stroke="#f7faf7" strokeWidth="4">{edgeName(edge)}</text>}
            </g>;
          })}
          {visible.nodes.map(node => {
            const point = positions[node.id]; if (!point) return null;
            const root = node.id === data?.center_id, selected = selection?.id === node.id, tone = node.type === "NAVIGATION_CATEGORY" ? graphCategories.find(group => group.key === node.properties?.category)?.tone || color(node) : color(node), r = root ? 33 : 23;
            const activate = () => node.type === "NAVIGATION_CATEGORY" && node.properties?.category ? openCategory(node.properties.category as GraphCategory) : selection?.kind === "node" && selection.id === node.id ? setSelection(null) : setSelection({ kind: "node", id: node.id });
            const nodeLabel = displayNodeLabel(node);
            return <g key={node.id} transform={`translate(${point.x} ${point.y})`} data-node-id={node.id} className="kg-node" role="button" tabIndex={0} aria-label={`${nodeName(node)}：${nodeLabel}`} opacity={selection && !neighbors.has(node.id) ? .28 : 1} onClick={activate} onDoubleClick={() => centerNode(node)} onKeyDown={event => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); activate(); } }}>
              <title>{nodeLabel} · {nodeName(node)}{node.definition || node.properties?.definition ? `\n${node.definition || node.properties?.definition}` : ""}{isCenterable(node) ? "\n双击以此对象为中心" : ""}</title>
              {navigation === "advanced" ? <>{selected && <circle r={r + 7} fill="none" stroke={tone} strokeWidth="2" opacity=".4" />}<circle r={r} fill={root ? tone : "#fff"} stroke={tone} strokeWidth={root ? 2 : 2.2} strokeDasharray={isCandidate(node) ? "5 3" : undefined} /><text textAnchor="middle" dy="4" fontSize={root ? "12" : "10"} fill={root ? "#fff" : tone} fontFamily="sans-serif">{nodeName(node).slice(0, 4)}</text><text textAnchor="middle" y={r + 18} fill="#253d32" fontSize="12" fontWeight={root ? "600" : "400"} fontFamily="sans-serif" paintOrder="stroke" stroke="#f7faf7" strokeWidth="4">{nodeLabel.length > 18 ? `${nodeLabel.slice(0, 17)}…` : nodeLabel}</text></> : <>
                {selected && <rect x={-98} y={-38} width="196" height="76" rx="12" fill="none" stroke={tone} strokeWidth="2" opacity=".35" />}
                <rect x={-93} y={-33} width="186" height="66" rx="9" fill={root ? tone : "#fff"} stroke={tone} strokeWidth={selected ? 2.5 : 1.4} strokeDasharray={isCandidate(node) ? "6 4" : undefined} />
                <text x={-80} y={-12} fontSize="10" fill={root ? "#e3edf8" : tone} fontFamily="sans-serif">{nodeName(node)}{root ? " · 当前中心" : ""}{isCandidate(node) ? " · 待核实" : ""}</text>
                <text x={-80} y={10} fontSize="13" fontWeight="550" fill={root ? "#fff" : "#253d32"} fontFamily="sans-serif">{nodeLabel.length > 18 ? `${nodeLabel.slice(0, 17)}…` : nodeLabel}</text>
              </>}
            </g>;
          })}
        </g>
      </svg>
      <div className="kg-canvas-tools"><button type="button" aria-label="放大图谱" onClick={() => zoom(1.3)}><ZoomIn size={17} /></button><button type="button" aria-label="缩小图谱" onClick={() => zoom(1 / 1.3)}><ZoomOut size={17} /></button><button type="button" aria-label="适配画布" onClick={() => setCamera(fitted(positions))}><Expand size={17} /></button><button type="button" aria-label={fullscreen ? "退出画布全屏" : "画布全屏"} onClick={() => void toggleFullscreen()}>{fullscreen ? <Minimize2 size={17} /> : <Maximize2 size={17} />}</button><button type="button" className="kg-tool-command" aria-label="加载全量数据" title="显示全部核心信息分类，点击分类后再读取明细" onClick={openAllInformation}><Database size={15} />全量数据</button>{navigation === "overview" && <button type="button" className="kg-tool-command" aria-expanded={domainsExpanded} aria-label={domainsExpanded ? "收起领域分类" : "展开领域分类"} onClick={() => setDomainsExpanded(value => !value)}>{domainsExpanded ? <ChevronUp size={15} /> : <ChevronDown size={15} />}{domainsExpanded ? "收起分类" : "展开分类"}</button>}<button type="button" aria-label="图谱原始比例" onClick={() => setCamera(old => ({ x: size.width / 2 - (size.width / 2 - old.x) / old.scale, y: size.height / 2 - (size.height / 2 - old.y) / old.scale, scale: 1 }))}>1:1</button>{selection && <button type="button" aria-label="清除对象高亮" onClick={() => setSelection(null)}>取消高亮</button>}<span>{Math.round(camera.scale * 100)}%</span></div>
      {(loading || error || !data || !visible.nodes.length) && <div className="kg-canvas-status" role={error ? "alert" : "status"}>{loading ? <><RefreshCw size={22} className="kg-spin" /><strong>正在读取当前对象及来源证据</strong></> : error ? <><strong>图谱暂时无法加载</strong><p>{error}</p><button type="button" onClick={() => setRevision(old => old + 1)}>重试</button></> : <><Network size={32} /><strong>{!center ? "从股票或公司开始探索" : "当前范围暂无可显示的对象"}</strong><p>使用上方搜索选择对象，或调整图层与候选关系筛选。</p></>}</div>}
      <span className="kg-canvas-hint">拖动对象调整位置 · 拖动画布平移 · 滚轮缩放 · 双击证券或公司切换中心</span>
    </div>
    {navigation === "overview" && <section className={`kg-domain-overview${domainsExpanded ? " expanded" : " collapsed"}`} aria-label="知识图谱领域导航"><div className="kg-domain-header"><div><strong>{currentNode?.type === "COMPANY" ? "公司信息分类" : "股票信息分类"}</strong><small>分类随当前中心变化，不新增经济关系。</small></div><button type="button" aria-expanded={domainsExpanded} aria-label={domainsExpanded ? "收起领域分类" : "展开领域分类"} onClick={() => setDomainsExpanded(value => !value)}>{domainsExpanded ? <ChevronUp size={15} /> : <ChevronDown size={15} />}{domainsExpanded ? "收起" : "展开"}</button></div>{domainsExpanded && <div className="kg-domain-grid">{groups.filter(group => centerCategories.some(centerCategory => centerCategory.key === group.key)).map(group => <button type="button" key={group.key} data-domain={group.key} data-testid={`graph-domain-${group.key}`} aria-label={`查看${group.label}`} className="kg-domain-card" disabled={!data || loading} onClick={() => openCategory(group.key)}><i style={{ background: group.tone }} /><strong>{group.label}<ChevronRight size={12} /></strong><small>{group.description}</small><span>{group.key === "evidence" ? `当前返回 ${data?.evidence?.length || 0} 份原文引用` : group.coverage.map(row => `${row.label} ${row.count.toLocaleString()}`).join(" · ") || "当前范围尚未采集"}</span><small>{group.loadedCount > 0 ? `当前已载入 ${group.loadedCount} 个对象` : group.sourceCount > 0 ? "已有资料，点击按领域加载" : "点击查看数据覆盖情况"}</small></button>)}</div>}</section>}
    {navigation === "advanced" && <div className="kg-legend"><small>点击图例筛选</small>{types.map(node => <button type="button" key={node.type} aria-pressed={!hiddenTypes.includes(node.type)} className={hiddenTypes.includes(node.type) ? "hidden" : ""} onClick={() => setHiddenTypes(old => old.includes(node.type) ? old.filter(type => type !== node.type) : [...old, node.type])}><i style={{ background: color(node) }} />{typeNames[node.type] || node.type_label || "关联对象"}</button>)}</div>}
    {navigation === "domain" && !!domain.records.length && <div className="kg-record-list" aria-label="本页对象列表"><small>本页对象 · 点击查看完整名称、属性和证据</small>{domain.records.map(node => <button type="button" key={node.id} className={selection?.id === node.id ? "selected" : ""} onClick={() => setSelection({ kind: "node", id: node.id })}><span style={{ color: color(node) }}>{nodeName(node)}</span><strong>{displayNodeLabel(node)}</strong>{isCandidate(node) && <em>待核实</em>}</button>)}</div>}
    </div>
    <aside className={`kg-inspector${inspectorExpanded ? "" : " collapsed"}`} aria-label="图谱对象详情">{!inspectorExpanded ? <button type="button" className="kg-inspector-expand" aria-label="展开右侧数据栏" onClick={() => setInspectorExpanded(true)}><PanelRightOpen size={17} /><span>展开数据栏</span></button> : <>{item ? <><div className="kg-inspector-heading"><span>{selection?.kind === "node" ? nodeName(item as NetworkNode) : "关系详情"}</span><div><button type="button" aria-label="收起右侧数据栏" onClick={() => setInspectorExpanded(false)}><PanelRightClose size={15} /></button><button type="button" aria-label="关闭图谱详情" onClick={() => setSelection(null)}><X size={15} /></button></div></div><h3>{selection?.kind === "edge" ? edgeName(item as NetworkEdge) : item.label}</h3>{item.status && <span className={`kg-status${isCandidate(item) ? " candidate" : ""}`}>{stateNames[item.status] || "来源记录"}</span>}{selectedEdge && <p className="kg-edge-description">{data?.nodes.find(n => n.id === selectedEdge.source)?.label}<span>↓ {edgeName(selectedEdge)}</span>{data?.nodes.find(n => n.id === selectedEdge.target)?.label}</p>}{selectedNode && isCenterable(selectedNode) && <button type="button" className="kg-center-button" onClick={() => centerNode(selectedNode)}>以此对象为中心</button>}{(item.definition || props.definition) && <p className="kg-definition">{String(item.definition || props.definition)}</p>}<dl className="kg-properties">{properties.map(([key, value]) => <div key={key}><dt>{fieldNames[key]}</dt><dd>{textValue(value)}</dd></div>)}</dl>{!properties.length && <p className="kg-muted">当前来源未提供更多标准属性。</p>}<h4>来源证据 · {evidence.length}</h4>{evidence.map(e => <EvidenceCard key={e.id} item={e} />)}{!evidence.length && <p className="kg-muted">当前对象未附带可展示的原文证据。身份标识或规则关联不代表新增的经济往来事实。</p>}<details className="kg-identity"><summary>对象标识</summary><code>{item.id}</code></details></> : <><div className="kg-inspector-heading"><span>数据覆盖</span><button type="button" aria-label="收起右侧数据栏" onClick={() => setInspectorExpanded(false)}><PanelRightClose size={15} /></button></div><h3>{currentNode?.label || "关系与证据"}</h3><p className="kg-muted">点击对象或关系，查看持股比例、观测指标、名词释义和来源。候选线索使用虚线展示。</p><div className="kg-coverage">{(data?.coverage || []).map(row => <div key={row.key} title={row.note}><strong>{row.label || fieldNames[row.key] || "数据维度"}</strong><span className={row.count ? "present" : "missing"}>{stateNames[row.status] || (row.count ? "已有数据" : "暂无数据")}{row.count > 0 ? ` · ${row.count}` : ""}</span>{row.pending_count ? <small>{row.pending_count} 条待审核</small> : null}</div>)}</div>{data && <p className="kg-muted">覆盖率按当前查询范围评估；尚未采集与已经证实不存在是不同状态。</p>}</>}</>}</aside></div>
  </section>;
}

export function KnowledgeGraphDialog({ close, ...props }: ExplorerProps & { close: () => void }) {
  const closeRef = useRef(close); closeRef.current = close;
  const dialogRef = useRef<HTMLElement>(null);
  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null; dialogRef.current?.focus();
    const keydown = (event: KeyboardEvent) => {
      if (event.key === "Escape") { event.stopPropagation(); closeRef.current(); }
      if (event.key !== "Tab") return;
      const elements = dialogRef.current?.querySelectorAll<HTMLElement>('button:not(:disabled), input, select, a[href], [tabindex="0"]');
      if (!elements?.length) return; const first = elements[0], last = elements[elements.length - 1];
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
    };
    document.addEventListener("keydown", keydown, true); return () => { document.removeEventListener("keydown", keydown, true); previous?.focus(); };
  }, []);
  return createPortal(<div className="kg-modal-backdrop" onClick={event => event.stopPropagation()}><section className="kg-modal" ref={dialogRef} role="dialog" aria-modal="true" aria-label="知识图谱浏览" tabIndex={-1}><header className="kg-modal-header"><strong><Network size={17} />知识图谱浏览</strong><button type="button" aria-label="关闭知识图谱" onClick={close}><X size={18} /></button></header><KnowledgeGraphExplorer {...props} compact /></section></div>, document.body);
}
