const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://127.0.0.1:8000/api/v1";

export type DataSource = {
  id: number;
  source_code: string;
  source_name: string;
  source_type: string;
  adapter_type: string;
  priority: number;
  enabled: boolean;
  config_json: Record<string, unknown>;
  description?: string;
};

export type DataInterface = {
  id: number;
  source_id: number;
  interface_code: string;
  interface_name: string;
  data_category: string;
  request_mode: "SYNC" | "ON_DEMAND";
  adapter_method: string;
  supported_markets: string[];
  enabled: boolean;
  description?: string;
};

export type StockSymbol = {
  id: number;
  market: string;
  symbol: string;
  exchange: string;
  name: string;
  asset_type: string;
  status: string;
  list_date?: string;
  source_id: number;
  ext_json: Record<string, unknown>;
  raw_payload: Record<string, unknown>;
  last_synced_at: string;
};

export type SymbolPage = {
  items: StockSymbol[];
  total: number;
  page: number;
  page_size: number;
};

export type SyncLog = {
  id: number;
  source_id: number;
  interface_code: string;
  market: string;
  status: string;
  total_count: number;
  inserted_count: number;
  updated_count: number;
  error_message?: string;
  detail_json: Record<string, unknown>;
  started_at: string;
  completed_at?: string;
};

export type StockKline = {
  id: number;
  market: string;
  symbol: string;
  period: string;
  adjust: string;
  trade_date: string;
  open_price?: number;
  high_price?: number;
  low_price?: number;
  close_price?: number;
  volume?: number;
  amount?: number;
  turnover_rate?: number;
  source_id: number;
  fetched_at: string;
};

export type StockFinancialReport = {
  id: number;
  market: string;
  symbol: string;
  indicator: string;
  report_period: string;
  currency?: string;
  data_json: Record<string, unknown>;
  url?: string | null;
  source_id: number;
  fetched_at: string;
};

export type StockNotice = {
  id: number;
  market: string;
  symbol: string;
  notice_date: string;
  title: string;
  notice_type?: string;
  url?: string;
  content_json: Record<string, unknown>;
  source_id: number;
  fetched_at: string;
};

export type StockQuote = {
  id: number;
  market: string;
  symbol: string;
  quote_time?: string;
  current_price?: number;
  previous_close_price?: number;
  open_price?: number;
  high_price?: number;
  low_price?: number;
  volume?: number;
  amount?: number;
  change_amount?: number;
  change_pct?: number;
  turnover_rate?: number;
  source_id: number;
  fetched_at: string;
  raw_payload: Record<string, unknown>;
};

export type StockNewsItem = {
  id: number;
  market: string;
  symbol: string;
  news_time: string;
  title: string;
  content?: string;
  source_name?: string;
  url?: string;
  content_json: Record<string, unknown>;
  source_id: number;
  fetched_at: string;
};

export type F10DataSection = Record<string, unknown>;

export type PublishedReport = {
  report_name?: string;
  report_type?: string;
  report_date?: string | null;
  notice_date?: string | null;
  url?: string | null;
  source_name?: string;
  title?: string;
};

export type StockF10 = {
  symbol: StockSymbol;
  realtime_quote?: StockQuote | null;
  recent_klines: StockKline[];
  financial_reports: StockFinancialReport[];
  notices: StockNotice[];
  news: StockNewsItem[];
  notice_total?: number;
  notice_page?: number;
  news_total?: number;
  news_page?: number;
  published_reports?: {
    source?: string;
    reports?: PublishedReport[];
    message?: string;
  };
  profile: F10DataSection;
  holders: F10DataSection;
  fund_flow: F10DataSection;
  financial_summary: F10DataSection;
  financial_statements: F10DataSection;
  business_composition: F10DataSection;
};

export type StockNoticePage = {
  items: StockNotice[];
  total: number;
  page: number;
  page_size: number;
};

export type StockNewsPage = {
  items: StockNewsItem[];
  total: number;
  page: number;
  page_size: number;
};

export type OnDemandFetchResponse = {
  fetch_log_id: number;
  source_code: string;
  status: string;
  total_count: number;
  persisted_count: number;
  items: Array<Record<string, unknown>>;
};

export type WatchlistItem = {
  id: number;
  market: string;
  symbol: string;
  note?: string;
  name?: string | null;
  current_price?: number | null;
  change_pct?: number | null;
  created_at: string;
};

export type IpoCalendarItem = {
  market: string;
  symbol: string;
  name: string;
  apply_date: string;
  apply_end_date?: string | null;
  listing_date?: string | null;
  price?: string | null;
  lot_size?: string | null;
  entry_fee?: string | null;
  industry?: string | null;
  sponsor?: string | null;
  prospectus_url?: string | null;
  detail_url?: string | null;
  issue_total?: string | null;
  online_issue?: string | null;
  apply_limit?: string | null;
  pe_ratio?: string | null;
  raw: Record<string, unknown>;
  source: string;
};

export type IpoCalendarResponse = {
  items: IpoCalendarItem[];
  sources: Array<{
    source: string;
    market: string;
    status: string;
    message: string;
    total_count: number;
    item_count: number;
    fetched_at: string;
  }>;
  errors: string[];
  window_start: string;
  window_end: string;
  updated_at: string;
};

export type ModelProvider = {
  id: number;
  provider_code: string;
  provider_name: string;
  provider_type: "MOCK" | "OPENAI_COMPAT" | "GEMINI_REST" | "DEEPSEEK" | "ANTHROPIC";
  api_base_url?: string | null;
  api_key_configured: boolean;
  enabled: boolean;
  description?: string;
  config_json: Record<string, unknown>;
  created_at: string;
  updated_at: string;
};

export type ModelProviderPayload = {
  provider_code: string;
  provider_name: string;
  provider_type: ModelProvider["provider_type"];
  api_base_url?: string | null;
  api_key?: string;
  enabled: boolean;
  description?: string;
  config_json: Record<string, unknown>;
};

export type ModelInstance = {
  id: number;
  provider_id: number;
  instance_code: string;
  model_code: string;
  model_name: string;
  purpose: string;
  usage_type: "EXACT" | "CREATIVE";
  api_base_url?: string;
  api_path?: string;
  max_tokens: number;
  temperature: number;
  top_p: number;
  enabled: boolean;
  fallback_instance_code?: string;
  config_json: Record<string, unknown>;
  description?: string;
  api_key_configured: boolean;
  created_at: string;
  updated_at: string;
};

export type ModelInstancePayload = {
  provider_id: number;
  instance_code: string;
  model_code: string;
  model_name: string;
  purpose: string;
  usage_type: "EXACT" | "CREATIVE";
  api_key?: string;
  api_base_url?: string;
  api_path?: string;
  max_tokens: number;
  temperature: number;
  top_p: number;
  enabled: boolean;
  fallback_instance_code?: string;
  config_json: Record<string, unknown>;
  description?: string;
};

export type ModelRoute = {
  id: number;
  task_type: string;
  preferred_instance_code: string;
  fallback_chain_json: string[];
  route_policy: string;
  enabled: boolean;
  description?: string;
  created_at: string;
  updated_at: string;
};

export type ModelRoutePayload = {
  task_type: string;
  preferred_instance_code: string;
  fallback_chain_json: string[];
  route_policy: string;
  enabled: boolean;
  description?: string;
};

export type ModelCallLog = {
  id: number;
  task_type: string;
  provider_code?: string;
  instance_code?: string;
  model_code?: string;
  status: string;
  request_json: Record<string, unknown>;
  response_json: Record<string, unknown>;
  response_text?: string;
  error_message?: string;
  latency_ms?: number;
  started_at: string;
  completed_at?: string;
};

export type ModelSkill = {
  id: number;
  skill_code?: string;
  skill_name: string;
  description?: string | null;
  instructions: string;
  skill_type: "PROMPT_SOP" | "EXECUTABLE_TOOL";
  enabled: boolean;
  config_json: Record<string, unknown>;
  created_at: string;
  updated_at: string;
  version?: string;
  is_builtin?: boolean;
  file_path?: string | null;
  content_hash?: string | null;
  format?: string;
};

export type SkillOptimizationDraft = {
  id: number;
  skill_id: number;
  base_skill_version: string;
  prediction_ids: number[];
  proposed_instructions: string;
  rationale: string;
  status: string;
  created_at: string;
};

export type ModelSkillPayload = {
  skill_code?: string;
  skill_name: string;
  description?: string;
  instructions: string;
  skill_type: "PROMPT_SOP" | "EXECUTABLE_TOOL";
  expected_content_hash?: string;
  enabled: boolean;
  config_json: Record<string, unknown>;
  version?: string;
};

export type AgentDefinition = {
  id: number;
  agent_code?: string;
  display_name: string;
  system_prompt: string;
  model_instance_code?: string | null;
  max_iterations: number;
  context_window_limit: number;
  json_schema_output: Record<string, unknown>;
  enabled: boolean;
  description?: string | null;
  version: string;
  child_agent_ids: number[];
  skill_ids: number[];
  knowledge_base_ids: number[];
  data_asset_ids: number[];
  data_source_ids: number[];
  created_at: string;
  updated_at: string;
};

export type AgentPayload = {
  agent_code?: string;
  display_name: string;
  system_prompt: string;
  model_instance_code?: string | null;
  max_iterations: number;
  context_window_limit: number;
  json_schema_output: Record<string, unknown>;
  enabled: boolean;
  description?: string;
  version?: string;
  child_agent_ids: number[];
  skill_ids: number[];
  knowledge_base_ids: number[];
  data_asset_ids: number[];
  data_source_ids: number[];
};

export type DataAsset = {
  id: number;
  asset_code: string;
  table_name: string;
  display_name: string;
  description?: string | null;
  allowed_columns: string[];
  columns: string[];
  governance_status: string;
  source_health: string;
  last_governed_at?: string | null;
  governance_report_json: Record<string, unknown>;
  row_count: number;
  enabled: boolean;
  last_inspected_at?: string | null;
  created_at: string;
  updated_at: string;
};

export type DataAssetPayload = {
  asset_code: string;
  table_name: string;
  display_name: string;
  description?: string;
  allowed_columns?: string[];
  enabled: boolean;
};

export type DataPreview = {
  table_name: string;
  columns: string[];
  row_count: number;
  rows: Array<Record<string, unknown>>;
};

export type KnowledgeBase = {
  id: number;
  kb_code: string;
  kb_name: string;
  description?: string | null;
  status: string;
  version: string;
  source_tables: string[];
  entity_count: number;
  relation_count: number;
  enabled: boolean;
  created_at: string;
  updated_at: string;
};

export type KnowledgeGraph = {
  id: number;
  knowledge_base_id: number;
  graph_code: string;
  graph_name: string;
  description?: string | null;
  symbol?: string | null;
  source_tables: string[];
  version: string;
  governance_status: string;
  enabled: boolean;
  entity_count: number;
  relation_count: number;
  last_governed_at?: string | null;
  governance_report_json: Record<string, unknown>;
};

export type KnowledgeGraphPayload = Pick<KnowledgeGraph, "knowledge_base_id" | "graph_code" | "graph_name" | "source_tables" | "version" | "enabled"> & {
  description?: string;
  symbol?: string | null;
};

export type GovernanceRun = {
  id: number;
  target_type: "ASSET" | "GRAPH";
  target_id: number;
  status: string;
  summary_json: Record<string, unknown>;
  model_call_log_id?: number | null;
  created_at: string;
};

export type GraphExplore = {
  nodes: Array<{ id: number; type: string; key: string; name: string; properties: Record<string, unknown> }>;
  relations: Array<{ id: number; from_id: number; to_id: number; type: string; evidence?: { document_id: number; title: string; source_table: string; excerpt: string } | null }>;
};

export type ModelTestResponse = {
  call_log_id: number;
  provider_code?: string;
  instance_code?: string;
  model_code?: string;
  status: string;
  message: string;
  response_text?: string;
};

export type ModelChatResponse = {
  call_log_id: number;
  task_type: string;
  provider_code?: string;
  instance_code?: string;
  model_code?: string;
  status: string;
  response_text: string;
  response_json: Record<string, unknown>;
};

export type ResearchFundamentalAgent = {
  agent: string;
  score: number;
  rating: string;
  commentary: string;
  positive_factors: string[];
  negative_factors: string[];
  data_gaps: string[];
  evidence: Record<string, unknown>;
};

export type ResearchTechnicalAgent = {
  agent: string;
  score: number;
  trend: string;
  capital_intent: string;
  commentary: string;
  key_levels: Record<string, number | null>;
  positive_factors: string[];
  negative_factors: string[];
  data_gaps: string[];
  evidence: Record<string, unknown>;
};

export type ResearchNews = {
  symbol: string;
  market: string;
  name: string;
  items: Array<{
    kind: string;
    title: string;
    published_at?: string | null;
    source?: string | null;
    url?: string | null;
    sentiment: string;
    relevance: number;
    excerpt?: string | null;
  }>;
  positive_count: number;
  negative_count: number;
  neutral_count: number;
  warnings: string[];
};

export type ResearchAgentSnapshot = {
  fundamental?: ResearchFundamentalAgent | null;
  technical?: ResearchTechnicalAgent | null;
  news?: ResearchNews | null;
};

export type ResearchAnalyzeDone = {
  symbol: string;
  market: string;
  name: string;
  model_provider?: string | null;
  model_instance?: string | null;
  warnings: string[];
  score?: number | null;
  rating?: string | null;
  conclusion?: string | null;
  report_id?: number | null;
  history_evaluation?: Record<string, unknown>;
};

export type ResearchReportSummary = {
  id: number;
  symbol: string;
  market: string;
  name: string;
  title: string;
  rating?: string | null;
  score?: number | null;
  conclusion?: string | null;
  model_provider?: string | null;
  model_instance?: string | null;
  created_at: string;
  updated_at: string;
};

export type ResearchReportDetail = ResearchReportSummary & {
  report_markdown: string;
  data_sources_json: string[];
  knowledge_base_ids_json: number[];
  agent_snapshot_json: Record<string, unknown>;
  history_evaluation_json: Record<string, unknown>;
  warnings_json: string[];
};

type RequestOptions = RequestInit & { body?: BodyInit | null };

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    cache: "no-store",
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
  });
  if (!response.ok) {
    const text = await response.text().catch(() => "");
    let body: { detail?: string | { message?: string } } = {};
    if (text) {
      try {
        body = JSON.parse(text);
      } catch {
        body = { detail: text };
      }
    }
    const detail = typeof body.detail === "string" ? body.detail : body.detail?.message;
    throw new Error(detail || `请求失败 (${response.status})`);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export const api = {
  health: () => fetch(API_BASE_URL.replace("/api/v1", "/health")).then((response) => response.json()),
  listSources: () => request<DataSource[]>("/data-sources"),
  testSource: (id: number) => request<{ message: string }>(`/data-sources/${id}/test`, { method: "POST" }),
  updateSource: (id: number, payload: Partial<DataSource>) =>
    request<DataSource>(`/data-sources/${id}`, { method: "PUT", body: JSON.stringify(payload) }),
  listInterfaces: () => request<DataInterface[]>("/data-interfaces"),
  listSymbols: (params: URLSearchParams) => request<SymbolPage>(`/stocks?${params.toString()}`),
  searchStocks: (market: string, keyword: string, limit = 12, options: RequestOptions = {}) =>
    request<StockSymbol[]>(
      `/stocks/search?market=${encodeURIComponent(market)}&keyword=${encodeURIComponent(keyword)}&limit=${limit}`,
      options,
    ),
  listWatchlist: () => request<WatchlistItem[]>("/stocks/watchlist"),
  addWatchlist: (payload: { market:string; symbol:string; note?:string }) =>
    request<WatchlistItem>("/stocks/watchlist", { method:"POST", body: JSON.stringify(payload) }),
  removeWatchlist: (market:string, symbol:string) => request(`/stocks/watchlist/${market}/${symbol}`, { method:"DELETE" }),
  listIpoCalendar: () => request<IpoCalendarResponse>("/stocks/ipo-calendar"),
  getStockF10: (
    market: string,
    symbol: string,
    options: {
      refresh?: boolean;
      localOnly?: boolean;
      klineLimit?: number;
      financialLimit?: number;
      noticeLimit?: number;
      newsLimit?: number;
      noticePage?: number;
      newsPage?: number;
    } = {},
  ) => {
    const params = new URLSearchParams({
      kline_limit: String(options.klineLimit ?? 5000),
      financial_limit: String(options.financialLimit ?? 8),
      notice_limit: String(options.noticeLimit ?? 8),
      news_limit: String(options.newsLimit ?? 8),
      notice_page: String(options.noticePage ?? 1),
      news_page: String(options.newsPage ?? 1),
      refresh: options.refresh ? "true" : "false",
      local_only: options.localOnly ? "true" : "false",
    });
    return request<StockF10>(`/stocks/${market}/${symbol}/f10?${params.toString()}`);
  },
  listStockNoticesPage: (market: string, symbol: string, page = 1, pageSize = 8) =>
    request<StockNoticePage>(`/stocks/${market}/${symbol}/notices/page?page=${page}&page_size=${pageSize}`),
  listStockNewsPage: (market: string, symbol: string, page = 1, pageSize = 8) =>
    request<StockNewsPage>(`/stocks/${market}/${symbol}/news/page?page=${page}&page_size=${pageSize}`),
  getStockKlines: (market: string, symbol: string, limit = 5000) =>
    request<StockKline[]>(`/stocks/${market}/${symbol}/kline?limit=${limit}`),
  fetchStockKline: (market: string, symbol: string, payload: Record<string, unknown> = {}) =>
    request<OnDemandFetchResponse>(`/stocks/${market}/${symbol}/kline/fetch`, {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  fetchStockFinancials: (market: string, symbol: string, payload: Record<string, unknown> = {}) =>
    request<OnDemandFetchResponse>(`/stocks/${market}/${symbol}/financials/fetch`, {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  fetchStockNotices: (market: string, symbol: string, payload: Record<string, unknown> = {}) =>
    request<OnDemandFetchResponse>(`/stocks/${market}/${symbol}/notices/fetch`, {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  fetchStockQuote: (market: string, symbol: string, payload: Record<string, unknown> = {}) =>
    request<OnDemandFetchResponse>(`/stocks/${market}/${symbol}/quote/fetch`, {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  fetchStockNews: (market: string, symbol: string, payload: Record<string, unknown> = {}) =>
    request<OnDemandFetchResponse>(`/stocks/${market}/${symbol}/news/fetch`, {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  synchronize: (market: string, sourceCode: string) =>
    request<{ status: string; message: string; sync_log_ids: number[]; attempted_source_codes: string[] }>("/stocks/sync", {
      method: "POST",
      body: JSON.stringify({ market, source_code: sourceCode, enable_fallback: true }),
    }),
  listSyncLogs: () => request<SyncLog[]>("/stocks/sync-logs/list?limit=8"),
  listModelProviders: () => request<ModelProvider[]>("/model-hub/providers"),
  createModelProvider: (payload: ModelProviderPayload) =>
    request<ModelProvider>("/model-hub/providers", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  updateModelProvider: (id: number, payload: Partial<ModelProviderPayload>) =>
    request<ModelProvider>(`/model-hub/providers/${id}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    }),
  deleteModelProvider: (id: number) => request<void>(`/model-hub/providers/${id}`, { method: "DELETE" }),
  testModelProvider: (id: number) =>
    request<ModelTestResponse>(`/model-hub/providers/${id}/test`, { method: "POST" }),
  listModelInstances: () => request<ModelInstance[]>("/model-hub/instances"),
  createModelInstance: (payload: ModelInstancePayload) =>
    request<ModelInstance>("/model-hub/instances", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  updateModelInstance: (id: number, payload: Partial<ModelInstancePayload>) =>
    request<ModelInstance>(`/model-hub/instances/${id}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    }),
  deleteModelInstance: (id: number) => request<void>(`/model-hub/instances/${id}`, { method: "DELETE" }),
  testModelInstance: (id: number) =>
    request<ModelTestResponse>(`/model-hub/instances/${id}/test`, { method: "POST" }),
  listModelRoutes: () => request<ModelRoute[]>("/model-hub/routes"),
  createModelRoute: (payload: ModelRoutePayload) =>
    request<ModelRoute>("/model-hub/routes", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  updateModelRoute: (id: number, payload: Partial<ModelRoutePayload>) =>
    request<ModelRoute>(`/model-hub/routes/${id}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    }),
  deleteModelRoute: (id: number) => request<void>(`/model-hub/routes/${id}`, { method: "DELETE" }),
  listModelSkills: () => request<ModelSkill[]>("/model-hub/skills"),
  createModelSkill: (payload: ModelSkillPayload) =>
    request<ModelSkill>("/model-hub/skills", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  updateModelSkill: (id: number, payload: Partial<ModelSkillPayload>) =>
    request<ModelSkill>(`/model-hub/skills/${id}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    }),
  deleteModelSkill: (id: number) => request<void>(`/model-hub/skills/${id}`, { method: "DELETE" }),
  listSkillRevisions: (id: number) => request<Array<{ id: number; version: string; source: string; created_at: string }>>(`/model-hub/skills/${id}/revisions`),
  rollbackSkill: (id: number, revisionId: number) => request<ModelSkill>(`/model-hub/skills/${id}/revisions/${revisionId}/rollback`, { method: "POST" }),
  listSkillDrafts: () => request<SkillOptimizationDraft[]>("/model-hub/skill-drafts?status_filter=PENDING_REVIEW"),
  approveSkillDraft: (id: number) => request<ModelSkill>(`/model-hub/skill-drafts/${id}/approve`, { method: "POST" }),
  rejectSkillDraft: (id: number) => request<SkillOptimizationDraft>(`/model-hub/skill-drafts/${id}/reject`, { method: "POST" }),
  listAgents: () => request<AgentDefinition[]>("/resources/agents"),
  createAgent: (payload: AgentPayload) =>
    request<AgentDefinition>("/resources/agents", { method: "POST", body: JSON.stringify(payload) }),
  updateAgent: (id: number, payload: Partial<AgentPayload>) =>
    request<AgentDefinition>(`/resources/agents/${id}`, { method: "PUT", body: JSON.stringify(payload) }),
  deleteAgent: (id: number) => request<void>(`/resources/agents/${id}`, { method: "DELETE" }),
  listDataAssets: () => request<DataAsset[]>("/resources/data-assets"),
  listAssetSourceTables: () => request<string[]>("/resources/data-assets/source-tables"),
  createDataAsset: (payload: DataAssetPayload) => request<DataAsset>("/resources/data-assets", { method: "POST", body: JSON.stringify(payload) }),
  previewDataAsset: (id: number, limit = 20) => request<DataPreview>(`/resources/data-assets/${id}/preview?limit=${limit}`),
  setAssetGovernanceState: (id: number, governance_status: string) => request<DataAsset>(`/resources/data-assets/${id}/governance-state`, { method: "PUT", body: JSON.stringify({ governance_status }) }),
  governDataAsset: (id: number, source_asset_ids: number[], agent_id?: number) => request<GovernanceRun>(`/resources/data-assets/${id}/govern`, { method: "POST", body: JSON.stringify({ source_asset_ids, agent_id }) }),
  batchGovernAssets: (target_ids: number[], source_asset_ids: number[] = [], agent_id?: number) => request<{ results: Array<{ target_id: number; status: string; message?: string }> }>("/resources/data-assets/govern/batch", { method: "POST", body: JSON.stringify({ target_ids, source_asset_ids, agent_id }) }),
  listGovernanceRuns: (target_type: "ASSET" | "GRAPH", target_id: number) => request<GovernanceRun[]>(`/resources/governance-runs?target_type=${target_type}&target_id=${target_id}`),
  updateDataAsset: (id: number, payload: Partial<DataAsset>) =>
    request<DataAsset>(`/resources/data-assets/${id}`, { method: "PUT", body: JSON.stringify(payload) }),
  inspectDataAsset: (id: number) =>
    request<DataAsset>(`/resources/data-assets/${id}/inspect`, { method: "POST" }),
  listKnowledgeBases: () => request<KnowledgeBase[]>("/resources/knowledge-bases"),
  searchKnowledgeBase: (id: number, q = "") => request<Array<{ document_id: number; source_table: string; symbol?: string; title: string; content: string }>>(`/resources/knowledge-bases/${id}/search?q=${encodeURIComponent(q)}`),
  listKnowledgeGraphs: () => request<KnowledgeGraph[]>("/resources/knowledge-graphs"),
  createKnowledgeGraph: (payload: KnowledgeGraphPayload) => request<KnowledgeGraph>("/resources/knowledge-graphs", { method: "POST", body: JSON.stringify(payload) }),
  updateKnowledgeGraph: (id: number, payload: Partial<KnowledgeGraphPayload>) => request<KnowledgeGraph>(`/resources/knowledge-graphs/${id}`, { method: "PUT", body: JSON.stringify(payload) }),
  deleteKnowledgeGraph: (id: number) => request<void>(`/resources/knowledge-graphs/${id}`, { method: "DELETE" }),
  exploreKnowledgeGraph: (id: number, q = "") => request<GraphExplore>(`/resources/knowledge-graphs/${id}/explore?q=${encodeURIComponent(q)}`),
  setGraphGovernanceState: (id: number, governance_status: string) => request<KnowledgeGraph>(`/resources/knowledge-graphs/${id}/governance-state`, { method: "PUT", body: JSON.stringify({ governance_status }) }),
  governKnowledgeGraph: (id: number, source_asset_ids: number[], agent_id?: number) => request<GovernanceRun>(`/resources/knowledge-graphs/${id}/govern`, { method: "POST", body: JSON.stringify({ source_asset_ids, agent_id }) }),
  batchGovernGraphs: (target_ids: number[], source_asset_ids: number[] = [], agent_id?: number) => request<{ results: Array<{ target_id: number; status: string; message?: string }> }>("/resources/knowledge-graphs/govern/batch", { method: "POST", body: JSON.stringify({ target_ids, source_asset_ids, agent_id }) }),
  createKnowledgeBase: (payload: {
    kb_code: string;
    kb_name: string;
    description?: string;
    source_tables: string[];
    version?: string;
    enabled: boolean;
  }) => request<KnowledgeBase>("/resources/knowledge-bases", { method: "POST", body: JSON.stringify(payload) }),
  updateKnowledgeBase: (id: number, payload: Partial<KnowledgeBase>) =>
    request<KnowledgeBase>(`/resources/knowledge-bases/${id}`, { method: "PUT", body: JSON.stringify(payload) }),
  deleteKnowledgeBase: (id: number) => request<void>(`/resources/knowledge-bases/${id}`, { method: "DELETE" }),
  buildKnowledgeBase: (id: number) =>
    request<{ knowledge_base_id: number; status: string; documents_created: number; entities_created: number; relations_created: number; message: string }>(
      `/resources/knowledge-bases/${id}/build`,
      { method: "POST" },
    ),
  chatWithModel: (payload: {
    task_type: string;
    instance_code?: string;
    messages: Array<{ role: "system" | "user" | "assistant" | "tool"; content: string }>;
    temperature?: number;
    max_tokens?: number;
    metadata_json?: Record<string, unknown>;
  }) =>
    request<ModelChatResponse>("/model-hub/chat", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  listModelCallLogs: (limit = 20) => request<ModelCallLog[]>(`/model-hub/call-logs?limit=${limit}`),
  analyzeResearch: async (
    payload: {
      symbol: string;
      market?: string;
      top_k?: number;
      refresh?: boolean;
      data_source_codes?: string[];
      knowledge_base_ids?: number[];
    },
    handlers: {
      onStage?: (data: { stage: string; message: string }) => void;
      onAgent?: (data: ResearchFundamentalAgent | ResearchTechnicalAgent) => void;
      onReport?: (delta: string) => void;
      onDone?: (data: ResearchAnalyzeDone) => void;
      onError?: (message: string) => void;
    } = {},
  ) => {
    const response = await fetch(`${API_BASE_URL}/research/analyze`, {
      method: "POST",
      cache: "no-store",
      headers: { "Content-Type": "application/json", Accept: "text/event-stream" },
      body: JSON.stringify(payload),
    });
    if (!response.ok || !response.body) {
      const text = await response.text().catch(() => "");
      throw new Error(text || `研究请求失败 (${response.status})`);
    }
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    const dispatch = (block: string) => {
      const event = block.match(/^event:\s*(.+)$/m)?.[1]?.trim() || "message";
      const dataText = block.match(/^data:\s*(.+)$/m)?.[1] || "{}";
      let data: Record<string, unknown> = {};
      try {
        data = JSON.parse(dataText) as Record<string, unknown>;
      } catch {
        data = { message: dataText };
      }
      if (event === "stage") handlers.onStage?.(data as { stage: string; message: string });
      if (event === "agent") handlers.onAgent?.(data as unknown as ResearchFundamentalAgent | ResearchTechnicalAgent);
      if (event === "report") handlers.onReport?.(String(data.delta || ""));
      if (event === "done") handlers.onDone?.(data as unknown as ResearchAnalyzeDone);
      if (event === "error") handlers.onError?.(String(data.message || "研究分析失败"));
    };
    while (true) {
      const { done, value } = await reader.read();
      buffer += decoder.decode(value || new Uint8Array(), { stream: !done });
      const blocks = buffer.split("\n\n");
      buffer = blocks.pop() || "";
      blocks.filter(Boolean).forEach(dispatch);
      if (done) break;
    }
    if (buffer.trim()) dispatch(buffer);
  },
  listResearchReports: (params: URLSearchParams = new URLSearchParams()) =>
    request<ResearchReportSummary[]>(`/research/reports${params.toString() ? `?${params.toString()}` : ""}`),
  getResearchReport: (id: number) => request<ResearchReportDetail>(`/research/reports/${id}`),
};
