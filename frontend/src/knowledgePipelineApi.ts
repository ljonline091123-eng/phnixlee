import {
  request,
  type KnowledgeBase,
  type KnowledgeGraph,
} from "./api";

export type KnowledgePipelineRequest = {
  market: string;
  symbols: string[];
  knowledge_base_id?: number;
  graph_id?: number;
  export_lakehouse: boolean;
  archive_chunks: boolean;
  run_graph: boolean;
  run_agent_governance: boolean;
  governance_record_limit: number;
  governance_run_key?: string;
  include_company_tables: boolean;
  dataset_limit: number;
  graph_documents_per_stock: number | null;
  chunk_size: number;
  overlap: number;
  chunk_document_limit: number | null;
  idempotency_key?: string;
};

export type PipelineQualityResult = {
  passed?: boolean;
  level?: string;
  row_count?: number;
  warnings?: string[];
  error?: string;
  [key: string]: unknown;
};

export type PipelineExportResult = {
  status?: string;
  dataset_id?: number;
  dataset_code?: string;
  version?: string;
  row_count?: number;
  error?: string;
  [key: string]: unknown;
};

export type PipelineCompletionIssue = {
  code: string;
  stage: string;
  detail?: unknown;
};

export type PipelineCompletion = {
  status: string;
  issues: PipelineCompletionIssue[];
  requested_stages: {
    lakehouse_export: boolean;
    document_chunks: boolean;
    knowledge_graph: boolean;
    agent_skill_governance: boolean;
  };
  model_selection_ready: boolean;
  graph_governance_status?: string | null;
  next_action?: string | null;
};

export type KnowledgePipelineResult = {
  pipeline_run_id: number;
  status: string;
  market: string;
  symbols: string[];
  knowledge_base_id: number;
  graph_id?: number | null;
  base_graph_id?: number | null;
  identity: {
    requested: number;
    mapped: number;
    unmapped: number;
    identities?: Record<string, Record<string, unknown>>;
  };
  quality: Record<string, PipelineQualityResult>;
  agent_governance: {
    status?: string;
    run_id?: number;
    agent_skill?: string;
    error?: string;
    summary?: Record<string, unknown>;
  };
  exports: Record<string, PipelineExportResult>;
  chunks: {
    documents: number;
    documents_total?: number;
    documents_selected?: number;
    documents_with_chunks?: number;
    created_chunk_count?: number;
    reused_chunk_count?: number;
    failed_documents?: number;
    coverage_status?: string;
    document_selection_ratio?: number | null;
    security_selection_ratio?: number | null;
    truncated?: boolean;
    [key: string]: unknown;
  };
  graph: {
    status?: string;
    graph_id?: number;
    base_graph_id?: number;
    projection?: boolean;
    counts?: {
      documents_created?: number;
      entities_created?: number;
      relations_created?: number;
      [key: string]: unknown;
    };
    error?: string;
  };
  lineage_batch_id?: string;
  completion: PipelineCompletion;
};

export type PipelineStageRun = {
  id: number;
  pipeline_run_id: number;
  stage_code: string;
  status: string;
  attempt: number;
  output_json: Record<string, unknown>;
  error_code?: string | null;
  error_message?: string | null;
  created_at: string;
  completed_at?: string | null;
};

export type PipelineRunDetail = {
  run: {
    id: number;
    status: string;
    current_stage?: string | null;
    created_at: string;
    completed_at?: string | null;
    error_message?: string | null;
  };
  stages: PipelineStageRun[];
};

export type KnowledgePipelineCatalog = {
  knowledgeBases: KnowledgeBase[];
  graphs: KnowledgeGraph[];
};

export const knowledgePipelineApi = {
  async catalog(signal?: AbortSignal): Promise<KnowledgePipelineCatalog> {
    const [knowledgeBases, graphs] = await Promise.all([
      request<KnowledgeBase[]>("/resources/knowledge-bases", { signal }),
      request<KnowledgeGraph[]>("/resources/knowledge-graphs", { signal }),
    ]);
    return { knowledgeBases, graphs };
  },

  run(payload: KnowledgePipelineRequest) {
    return request<KnowledgePipelineResult>("/knowledge-pipeline/run", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },

  getRun(runId: number, signal?: AbortSignal) {
    return request<PipelineRunDetail>(`/knowledge-pipeline/runs/${runId}`, { signal });
  },
};
