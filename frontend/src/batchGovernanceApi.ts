import { request } from "./api";

export type BatchGovernanceBusinessType =
  | "QUOTE"
  | "KLINE"
  | "FINANCIAL"
  | "NEWS"
  | "NOTICE"
  | "F10";

export type GovernanceMode = "SYSTEM_GOVERNANCE" | "AI_AGENT_SKILL_GOVERNANCE";

export type BatchGovernanceStock = {
  market: string;
  symbol: string;
};

export type BatchGovernanceRequest = {
  stocks: BatchGovernanceStock[];
  governance_mode: GovernanceMode;
  governance_batch_id?: string | null;
  collect_business_data: boolean;
  export_lakehouse: boolean;
  archive_chunks: boolean;
  run_graph: boolean;
  run_agent_governance: boolean;
  business_types: BatchGovernanceBusinessType[];
  knowledge_base_id?: number | null;
  graph_id?: number | null;
  kline_days: number;
  disclosure_days: number;
  idempotency_key?: string;
};

export type StockPipelineContinuationRequest = {
  stage: "data_collection" | "knowledge_base" | "knowledge_graph";
  item_codes: string[];
  idempotency_key?: string;
  kline_days?: number;
  disclosure_days?: number;
};

export type BatchGovernanceStageResult = {
  status?: string;
  message?: string;
  processed?: number;
  succeeded?: number;
  failed?: number;
  skipped?: number;
  [key: string]: unknown;
};

export type BatchGovernanceStockResult = {
  market: string;
  symbol: string;
  status?: string;
  message?: string;
  errors?: Array<string | { data_type?: string; message?: string; [key: string]: unknown }>;
  [key: string]: unknown;
};

export type BatchGovernanceJob = {
  job_id: number;
  pipeline_run_id?: number | null;
  status: string;
  job_status?: string | null;
  result_status?: string | null;
  stock_count: number;
  governance_mode?: GovernanceMode;
  governance_batch_id?: string | null;
  agent_execution_run_id?: string | null;
  progress?: number;
  current_stage?: string | null;
  stages?: Record<string, boolean> | string[];
  effective_options?: Partial<BatchGovernanceRequest>;
  stage_results?: Record<string, BatchGovernanceStageResult>;
  stock_results?: BatchGovernanceStockResult[];
  errors?: Array<string | Record<string, unknown>>;
  pipeline_run_ids?: number[];
  graph_ids?: number[];
  error_message?: string | null;
  created_at?: string;
  started_at?: string | null;
  completed_at?: string | null;
  run_after?: string | null;
  worker_required?: boolean;
  worker_task_type?: string | null;
  worker_command?: string | null;
  [key: string]: unknown;
};

export type GovernanceRecord = BatchGovernanceJob & {
  stock_symbols?: string[];
  stock_names?: string[];
  stock_status_counts?: Record<string, number>;
};

export type GovernanceReport = {
  job: {
    job_id: number;
    pipeline_run_id?: number | null;
    governance_mode?: string | null;
    governance_batch_id?: string | null;
    status?: string | null;
    job_status?: string | null;
    result_status?: string | null;
    current_stage?: string | null;
    progress?: number;
    created_at?: string | null;
    started_at?: string | null;
    completed_at?: string | null;
  };
  stock_results: Array<Record<string, unknown>>;
  stage_results: Record<string, BatchGovernanceStageResult>;
  errors: Array<string | Record<string, unknown>>;
  agent: Record<string, unknown> | null;
  skills: Array<Record<string, unknown>>;
};

export const batchGovernanceApi = {
  create(payload: BatchGovernanceRequest) {
    return request<BatchGovernanceJob>("/stocks/batch-governance/jobs", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },

  get(jobId: number, signal?: AbortSignal) {
    return request<BatchGovernanceJob>(`/stocks/batch-governance/jobs/${jobId}`, { signal });
  },

  retry(jobId: number) {
    return request<BatchGovernanceJob>(`/stocks/batch-governance/jobs/${jobId}/retry`, {
      method: "POST",
    });
  },

  list(limit = 20, signal?: AbortSignal) {
    return request<BatchGovernanceJob[] | { items: BatchGovernanceJob[]; total?: number }>(`/stocks/batch-governance/jobs?limit=${limit}`, { signal });
  },

  listRecords(params: {
    limit?: number;
    offset?: number;
    status?: string;
    governanceMode?: GovernanceMode | "";
    keyword?: string;
  } = {}, signal?: AbortSignal) {
    const query = new URLSearchParams();
    query.set("limit", String(params.limit ?? 50));
    query.set("offset", String(params.offset ?? 0));
    if (params.status) query.set("status_filter", params.status);
    if (params.governanceMode) query.set("governance_mode", params.governanceMode);
    if (params.keyword?.trim()) query.set("keyword", params.keyword.trim());
    return request<{ items: GovernanceRecord[]; total: number; limit: number; offset: number }>(
      `/stocks/batch-governance/jobs?${query.toString()}`, { signal },
    );
  },

  report(jobId: number, signal?: AbortSignal) {
    return request<BatchGovernanceJob & { governance_report?: GovernanceReport }>(
      `/stocks/batch-governance/jobs/${jobId}`, { signal },
    );
  },

  continueStockPipeline(
    market: string,
    symbol: string,
    payload: StockPipelineContinuationRequest,
  ) {
    return request<BatchGovernanceJob>(
      `/stocks/${encodeURIComponent(market)}/${encodeURIComponent(symbol)}/pipeline-status/continue`,
      { method: "POST", body: JSON.stringify(payload) },
    );
  },
};
