import { request, type GovernanceRun } from "./api";

export type KnowledgeSourceCoverage = {
  source_table: string; display_name: string; registered?: boolean; enabled?: boolean;
  table_available?: boolean; total_records: number; row_count_known?: boolean; status: string;
  documents_created?: number; limited?: boolean; materialization_status?: string;
  model_review?: { decision?: string; issues?: unknown[]; evidence_refs?: unknown[] };
};
export type KnowledgeGovernanceSummary = {
  run_key?: string; record_limit_per_source?: number; sample_scope?: string; complete?: boolean; error?: string;
  source_coverage?: KnowledgeSourceCoverage[];
  created_knowledge_bases?: Array<{ id: number; kb_code: string; kb_name: string; graph_id: number; documents_created: number; entities_created: number; relations_created: number; status: string }>;
  skill_snapshot?: { skill_code: string; version: string; content_hash: string };
  model_call_log_id?: number; model_status?: string;
};
export type KnowledgeGovernanceCatalog = {
  source_groups: Array<{ code: string; name: string; source_tables: string[] }>;
  source_coverage: KnowledgeSourceCoverage[]; latest_run: GovernanceRun | null;
};
export const knowledgeGovernanceApi = {
  catalog: (signal?: AbortSignal) => request<KnowledgeGovernanceCatalog>("/resources/knowledge-governance/catalog", { signal }),
  run: (recordLimit: number, runKey: string) => request<GovernanceRun>("/resources/knowledge-governance/run", {
    method: "POST", body: JSON.stringify({ record_limit_per_source: recordLimit, instance_code: null, run_key: runKey }),
  }),
};
