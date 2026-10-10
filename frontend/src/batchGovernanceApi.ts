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
  structure_documents?: boolean;
  structured_document_limit?: number;
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
  actions?: GovernanceActions;
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
  summary?: {
    stock_count?: number;
    status_counts?: Record<string, number>;
    completed_stocks?: number;
    partial_stocks?: number;
    failed_stocks?: number;
    pending_stocks?: number;
    skill_execution_count?: number;
    skill_execution_scope?: "TASK" | "BATCH_HISTORY";
    evidence_count?: number;
    error_count?: number;
    progress?: number;
    stage_stats?: Record<string, { label?: string; status?: string; completed?: number; incomplete?: number; total?: number; completion_rate?: number | null; message?: string | null }>;
    [key: string]: unknown;
  };
  analysis?: {
    failure_reasons?: Array<{ code?: string; count?: number; reason?: string }>;
    recommendations?: string[];
    stage_stats?: Record<string, unknown>;
    [key: string]: unknown;
  };
};

export type GovernanceActions = {
  history: Array<{ job_id: number; parent_job_id?: number; created_at?: string; status: string; result_status?: string }>;
  drafts: Array<{ id: number; skill_code: string; skill_name: string; base_skill_version: string; current_version: string;
    status: string; rationale: string; proposed_instructions: string; current_instructions: string;
    validation?: { status: string; scope: string; checks: Array<{ name: string; passed: boolean; reason?: string }> } }>;
  continuations: Array<{ job_id: number; created_at?: string }>;
};

export type GovernanceStagePreview = { title: string; status: string; reason: string; note: string;
  report_snapshot: Record<string, unknown>; items: Array<Record<string, unknown>>; total: number; limit: number; offset: number };

export type GovernanceContinuationPlan = { can_continue: boolean; notes: string[];
  reasons: Array<{ stage: string; code?: string; reason?: string }>; request: Partial<BatchGovernanceRequest> };

export const batchGovernanceApi = {
  continuationPlan(jobId: number) {
    return request<GovernanceContinuationPlan>(`/stocks/batch-governance/jobs/${jobId}/continuation-plan`);
  },
  continueJob(jobId: number, key: string) {
    return request<BatchGovernanceJob>(`/stocks/batch-governance/jobs/${jobId}/continue`, { method: "POST", body: JSON.stringify({ idempotency_key: key }) });
  },
  stagePreview(jobId: number, stage: string, itemCode: string, offset = 0) {
    const query = new URLSearchParams({ stage, item_code: itemCode, offset: String(offset), limit: "20" });
    return request<GovernanceStagePreview>(`/stocks/batch-governance/jobs/${jobId}/stage-preview?${query}`);
  },
  optimize(jobId: number) {
    return request<GovernanceActions>(`/stocks/batch-governance/jobs/${jobId}/skill-optimization`, { method: "POST" });
  },
  testDraft(jobId: number, draftId: number) {
    return request(`/stocks/batch-governance/jobs/${jobId}/skill-optimization/${draftId}/test`, { method: "POST" });
  },
  reviewDraft(jobId: number, draftId: number, decision: "APPROVE" | "REJECT") {
    return request<GovernanceActions>(`/stocks/batch-governance/jobs/${jobId}/skill-optimization/${draftId}/review`, { method: "POST", body: JSON.stringify({ decision }) });
  },
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

  start(jobId: number) {
    return request<BatchGovernanceJob>(`/stocks/batch-governance/jobs/${jobId}/start`, {
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
