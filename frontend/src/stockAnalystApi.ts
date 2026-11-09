import { request } from "./api";

export type AnalystRequest = { market: string; symbol: string; period_months: number; model_instance_code?: string };
export type AnalystCoverage = {
  market: string; symbol: string; name: string; period_months: number; as_of: string;
  counts: Record<string, number>; total_counts: Record<string, number>;
  latest_dates: Record<string, string | null>; available_categories: string[];
  missing_categories: string[]; warnings: string[]; evidence_count: number;
};
export type AnalystReference = { evidence_id: string; claim: string };
export type AnalystScenario = {
  horizon: "SHORT" | "MEDIUM" | "LONG"; horizon_label: string; direction: string;
  confidence: number; thesis: string; conditions: string[]; invalidation_conditions: string[];
  evidence: AnalystReference[];
};
export type AnalystResult = {
  executive_summary: string; current_assessment: string; overall_direction: string; overall_confidence: number;
  scenarios: AnalystScenario[]; core_drivers: string[]; catalysts: string[]; key_risks: string[];
  evidence_chain: AnalystReference[]; watch_items: string[]; data_limitations: string[]; disclaimer: string;
};
export type AnalystRun = {
  id: number; market: string; symbol: string; name: string; period_months: number; status: string;
  model_instance_code: string | null; model_call_log_id: number | null; skill_code: string; skill_version: string;
  coverage_json: AnalystCoverage; result_json: Partial<AnalystResult>; error_message: string | null;
  progress_json: { stage?: string; last_stage?: string; message?: string; events?: { stage: string; message: string; at: string }[] };
  created_at: string; completed_at: string | null;
};
export type AnalystEvidence = {
  evidence_id: string; category: string; category_name: string; title: string;
  observed_at?: string; source?: string; excerpt?: string; data?: Record<string, unknown>;
};
export const analystApi = {
  preview: (payload: AnalystRequest, signal?: AbortSignal) => request<AnalystCoverage>("/ai-stock-analyst/context-preview", { method: "POST", body: JSON.stringify(payload), signal }),
  create: (payload: AnalystRequest) => request<AnalystRun>("/ai-stock-analyst/analyses", { method: "POST", body: JSON.stringify(payload) }),
  list: () => request<AnalystRun[]>("/ai-stock-analyst/analyses?limit=50"),
  get: (id: number, signal?: AbortSignal) => request<AnalystRun>(`/ai-stock-analyst/analyses/${id}`, { signal }),
  cancel: (id: number) => request<AnalystRun>(`/ai-stock-analyst/analyses/${id}/cancel`, { method: "POST" }),
  evidence: async (id: number) => {
    const page = await request<{ items: AnalystEvidence[]; total: number }>(`/ai-stock-analyst/analyses/${id}/evidence?limit=100`);
    const items = [...page.items];
    while (items.length < page.total) {
      const next = await request<{ items: AnalystEvidence[]; total: number }>(`/ai-stock-analyst/analyses/${id}/evidence?limit=100&offset=${items.length}`);
      if (!next.items.length) throw new Error("证据快照未完整返回，请刷新后重试。");
      items.push(...next.items);
    }
    return { items, total: page.total };
  },
};
