import type { KnowledgeNetwork, NetworkEdge, NetworkNode } from "./knowledgeNetworkApi";

export type GraphCategory = "identity" | "classification" | "ownership" | "financial" | "market" | "disclosure" | "business" | "legal" | "evidence";
export const graphCategories: Array<{ key: GraphCategory; label: string; description: string; tone: string; coverageKeys: string[] }> = [
  { key: "classification", label: "行业 · 板块 · 类型", description: "所属行业、投资主题与股票风格", tone: "#8061aa", coverageKeys: ["industry", "theme", "classification"] },
  { key: "ownership", label: "股权与控制", description: "股东、持股比例与控制关系", tone: "#327a64", coverageKeys: ["ownership", "shareholder_actions"] },
  { key: "financial", label: "财务与估值", description: "报告期、财务指标与原始财报", tone: "#4a75a4", coverageKeys: ["financial"] },
  { key: "market", label: "量价与交易", description: "行情快照、成交量与异常观测", tone: "#34828a", coverageKeys: ["market", "realtime"] },
  { key: "disclosure", label: "新闻与公告", description: "新闻、公告、研报、F10 与事件线索", tone: "#b18641", coverageKeys: ["news", "notices", "research", "f10", "external_events"] },
  { key: "business", label: "供应链与经营", description: "上下游、产品、合同与经营往来", tone: "#8c6a50", coverageKeys: ["supply_chain", "business"] },
  { key: "legal", label: "司法事项", description: "案件、当事人及司法披露资料", tone: "#a16b73", coverageKeys: ["legal"] },
  { key: "identity", label: "关联主体", description: "关联公司、上市证券与市场身份", tone: "#5c7f9d", coverageKeys: ["identity", "company"] },
  { key: "evidence", label: "来源与证据", description: "可追溯的原文记录和数据来源", tone: "#7d8170", coverageKeys: [] },
];
const ownershipTypes = new Set(["HOLDS_EQUITY", "OWNS", "SHAREHOLDER_OF", "CONTROLS", "HAS_SHAREHOLDER_ACTION", "HAS_SHAREHOLDER_EVENT"]);
const documentTypes = new Set(["NEWS", "NOTICE", "RESEARCH_REPORT", "F10_SECTION", "CORPORATE_EVENT", "EXTERNAL_EVENT", "POLICY_EVENT", "EVENT_MENTION", "MENTIONED_EVENT", "CANDIDATE_EVENT"]);
export const securityCategoryKeys: GraphCategory[] = ["identity", "classification", "ownership", "financial", "market", "disclosure", "evidence"];
export const companyCategoryKeys: GraphCategory[] = ["identity", "classification", "ownership", "business", "legal", "disclosure", "evidence"];
export function categoriesForCenter(data: KnowledgeNetwork | null) {
  const center = data?.nodes.find(node => node.id === data.center_id);
  const keys = center?.type === "COMPANY" || data?.center_id.startsWith("company:") ? companyCategoryKeys : securityCategoryKeys;
  return graphCategories.filter(category => keys.includes(category.key));
}
export function effectiveNodeType(node: NetworkNode): string {
  const props = { ...node.properties_json, ...node.properties };
  return typeof props.document_type === "string" ? props.document_type : node.type;
}
/** UI membership only: this function never adds a business relation or changes a fact. */
export function belongsToCategory(node: NetworkNode, category: GraphCategory, edges: NetworkEdge[]): boolean {
  const type = effectiveNodeType(node);
  switch (category) {
    case "identity": return ["SECURITY", "STOCK", "COMPANY", "LEGAL_ENTITY", "ORGANIZATION", "MARKET", "EXCHANGE"].includes(type);
    case "classification": return ["INDUSTRY", "SECTOR", "THEME", "CLASSIFICATION", "CLASSIFICATION_TAG"].includes(type);
    case "ownership": return ["HOLDER_ACCOUNT", "PERSON", "OWNERSHIP_FACT", "SHAREHOLDER_ACTION"].includes(type) || edges.some(edge => ownershipTypes.has(edge.type) && (edge.source === node.id || edge.target === node.id));
    case "financial": return ["FINANCIAL_OBSERVATION", "FINANCIAL_METRIC", "FINANCIAL_REPORT"].includes(type);
    case "market": return ["MARKET_OBSERVATION", "MARKET_EVENT", "MARKET_ANOMALY", "PRICE_SERIES", "REALTIME_QUOTE"].includes(type);
    case "disclosure": return documentTypes.has(type) || ["F10_ITEM", "F10_SOURCE"].includes(type);
    case "business": return ["SUPPLY_CHAIN_FACT", "BUSINESS_FACT", "PRODUCT", "MATERIAL", "CONTRACT"].includes(type);
    case "legal": return ["LEGAL_CASE", "LEGAL_FACT"].includes(type);
    case "evidence": return node.layer === "EVIDENCE" || ["DATA_SOURCE", "DATASET", "DOCUMENT", "SOURCE_DOCUMENT"].includes(type);
  }
}
export function buildAllInformationGraph(data: KnowledgeNetwork | null) {
  const core = identityGraph(data);
  if (!data) return { nodes: core.nodes, edges: core.edges };
  const categories = categoriesForCenter(data);
  const categoryNodes: NetworkNode[] = categories.map(category => ({
    id: `ui-category:${category.key}`, type: "NAVIGATION_CATEGORY", type_label: "一级信息分类",
    label: category.label, layer: "MASTER", status: "UI_NAVIGATION",
    properties: { category: category.key, description: category.description, grouping_is_ui_only: true }, evidence_ids: [],
  }));
  const edges: NetworkEdge[] = [...core.edges, ...categoryNodes.map(node => ({
    id: `ui-category-edge:${data.center_id}:${node.id}`, source: data.center_id, target: node.id,
    type: "HAS_INFORMATION_CATEGORY", type_label: "信息分类", label: "信息分类", layer: "MASTER" as const,
    status: "UI_NAVIGATION", properties: { grouping_is_ui_only: true }, evidence_ids: [],
  }))];
  return { nodes: [...core.nodes, ...categoryNodes], edges };
}
export function identityGraph(data: KnowledgeNetwork | null) {
  if (!data) return { nodes: [] as NetworkNode[], edges: [] as NetworkEdge[] };
  const root = data.nodes.find(node => node.id === data.center_id);
  const issuerEdges = data.edges.filter(edge => edge.type === "ISSUED_BY" && (edge.source === data.center_id || edge.target === data.center_id)).slice(0, 2);
  const ids = new Set([data.center_id, ...issuerEdges.flatMap(edge => [edge.source, edge.target])]);
  return { nodes: [root, ...data.nodes.filter(node => node.id !== data.center_id && ids.has(node.id))].filter((node): node is NetworkNode => !!node), edges: issuerEdges };
}
export function buildNavigationGroups(data: KnowledgeNetwork | null) {
  const core = new Set(identityGraph(data).nodes.map(node => node.id));
  return graphCategories.map(category => {
    const provided = data?.category === category.key && data.category_node_ids ? new Set(data.category_node_ids) : null;
    const nodes = (data?.nodes || []).filter(node => !core.has(node.id) && (provided ? provided.has(node.id) : belongsToCategory(node, category.key, data?.edges || [])));
    const coverage = (data?.coverage || []).filter(row => category.coverageKeys.includes(row.key));
    return { ...category, nodes, loadedCount: nodes.length, coverage, sourceCount: coverage.reduce((total, row) => total + row.count, 0) };
  });
}
/** Keep real edges and the shortest real context paths. The displayed graph is bounded independently of source coverage. */
export function buildDomainGraph(data: KnowledgeNetwork | null, category: GraphCategory, page = 0, subtype = "", pageSize = 8) {
  const empty = { nodes: [] as NetworkNode[], edges: [] as NetworkEdge[], records: [] as NetworkNode[], total: 0, pages: 0, page: 0, types: [] as string[], omittedContext: false };
  if (!data) return empty;
  const core = identityGraph(data), coreIds = new Set(core.nodes.map(node => node.id));
  const provided = data.category === category && data.category_node_ids ? new Set(data.category_node_ids) : null;
  const all = data.nodes.filter(node => !coreIds.has(node.id) && (provided ? provided.has(node.id) : belongsToCategory(node, category, data.edges)));
  const types = [...new Set(all.map(effectiveNodeType))].sort();
  const filtered = all.filter(node => !subtype || effectiveNodeType(node) === subtype).sort((a, b) => a.label.localeCompare(b.label, "zh-CN") || a.id.localeCompare(b.id));
  pageSize = Math.max(1, Math.min(8, Math.floor(pageSize) || 8));
  const pages = Math.ceil(filtered.length / pageSize), actualPage = Math.max(0, Math.min(Math.floor(page) || 0, Math.max(0, pages - 1)));
  const records = filtered.slice(actualPage * pageSize, (actualPage + 1) * pageSize);
  const ids = new Set([...coreIds, ...records.map(node => node.id)]);
  const adjacency = new Map<string, string[]>();
  const permittedEdges = data.edges.filter(edge => category === "evidence" || edge.type !== "FROM_SOURCE" && (category === "disclosure" || edge.layer !== "EVIDENCE"));
  permittedEdges.forEach(edge => { adjacency.set(edge.source, [...(adjacency.get(edge.source) || []), edge.target]); adjacency.set(edge.target, [...(adjacency.get(edge.target) || []), edge.source]); });
  const parent = new Map<string, string | null>([[data.center_id, null]]), queue = [data.center_id];
  for (let i = 0; i < queue.length; i++) for (const next of adjacency.get(queue[i]) || []) if (!parent.has(next)) { parent.set(next, queue[i]); queue.push(next); }
  let omittedContext = false;
  records.forEach(record => {
    if (!parent.has(record.id)) { omittedContext = true; return; }
    const path: string[] = []; let cursor = parent.get(record.id);
    while (cursor && !ids.has(cursor)) { path.push(cursor); cursor = parent.get(cursor); }
    if (ids.size + path.length <= 16) path.forEach(id => ids.add(id)); else omittedContext = true;
  });
  return { nodes: data.nodes.filter(node => ids.has(node.id)), edges: permittedEdges.filter(edge => ids.has(edge.source) && ids.has(edge.target)), records, total: filtered.length, pages, page: actualPage, types, omittedContext };
}
/** Readable rows replace the force layout in guided views; coordinates remain stable across selections. */
export function stableDomainLayout(nodes: NetworkNode[], centerId: string, coreIds: string[] = [], overview = false): Record<string, { x: number; y: number }> {
  const points: Record<string, { x: number; y: number }> = {};
  if (overview) {
    nodes.forEach((node, i) => { points[node.id] = { x: (i - (nodes.length - 1) / 2) * 280, y: 0 }; });
    return points;
  }
  const core = nodes.filter(node => coreIds.includes(node.id) || node.id === centerId), others = nodes.filter(node => !core.includes(node));
  core.forEach((node, i) => { points[node.id] = { x: (i - (core.length - 1) / 2) * 280, y: -235 }; });
  const columns = others.length > 6 ? 3 : 2, rows = Math.ceil(others.length / columns);
  others.forEach((node, i) => { points[node.id] = { x: (i % columns - (columns - 1) / 2) * 215, y: -75 + Math.floor(i / columns) * Math.min(130, 400 / Math.max(1, rows)) }; });
  return points;
}
