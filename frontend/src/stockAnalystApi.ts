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

export type AnalystDialogue = {
  id: number; status: string; question: string; reply_json: { reply?: string; suggested_judgment?: string; focus_points?: string[]; evidence?: AnalystReference[] };
  error_message?: string; model_instance_code?: string; created_at: string; completed_at?: string;
};
export type AnalystRevision = { id: number; version: number; judgment: string; focus_points_json: string[]; created_at: string; dialogue_id: number };
export type AnalystWatchConfig = {
  enabled: boolean; price_lower: number | null; price_upper: number | null; volume_mode: "ANY" | "EXPAND" | "SHRINK";
  volume_ratio: number; volume_window: number; event_conditions: { category: "NEWS" | "NOTICE" | "ANY"; keywords: string[] }[];
  review_focus: string; schedule_hour: number; schedule_minute: number; model_instance_code?: string;
};
export type AnalystWatch = { id: number; enabled: boolean; config_json: AnalystWatchConfig; config_version: number; model_instance_code: string };
export type AnalystFollowupState = { dialogues: AnalystDialogue[]; revisions: AnalystRevision[]; latest_review: AnalystDailyReview | null; watch: AnalystWatch | null; runner: { available: boolean; message: string }; timezone: string };
export type AnalystDailyReview = {
  id: number; status: string; business_date: string; attempt: number; created_at: string; completed_at?: string;
  error_message?: string; progress_json: { stage?: string; message?: string; collection?: { category: string; status: string; count?: number; error?: string; source?: string; fetch_log_id?: number }[] };
  config_snapshot_json: AnalystWatchConfig & { config_version: number };
  conditions_json: { kind: string; label: string; status: string; threshold?: number; value?: number; currency?: string; trade_date?: string; matched_count?: number; scanned_count?: number; reason?: string }[];
  result_json: { summary?: string; judgment?: string; verdict?: string; confidence?: number; changes?: string[]; next_focus?: string[]; evidence?: AnalystReference[] };
  model_instance_code?: string; model_call_log_id?: number;
};
export const analystFollowupApi = {
  state: (id: number) => request<AnalystFollowupState>(`/ai-stock-analyst/analyses/${id}/followup`),
  send: (id: number, question: string, model_instance_code: string) => request<AnalystDialogue>(`/ai-stock-analyst/analyses/${id}/dialogues`, { method: "POST", body: JSON.stringify({ question, model_instance_code }) }),
  confirm: (id: number, payload: { dialogue_id: number; expected_revision_id: number | null; judgment: string; focus_points: string[] }) => request<AnalystRevision>(`/ai-stock-analyst/analyses/${id}/report-confirmations`, { method: "POST", body: JSON.stringify(payload) }),
  saveWatch: (id: number, payload: AnalystWatchConfig) => request<AnalystWatch>(`/ai-stock-analyst/analyses/${id}/watch`, { method: "PUT", body: JSON.stringify(payload) }),
  check: (id: number) => request<AnalystDailyReview>(`/ai-stock-analyst/analyses/${id}/watch/check`, { method: "POST" }),
  timeline: (id: number, offset = 0) => request<{ items: AnalystDailyReview[]; total: number }>(`/ai-stock-analyst/analyses/${id}/timeline?offset=${offset}&limit=10`),
  evidence: (id: number, review: number) => request<{ evidence: AnalystEvidence[] }>(`/ai-stock-analyst/analyses/${id}/timeline/${review}/evidence`),
};
