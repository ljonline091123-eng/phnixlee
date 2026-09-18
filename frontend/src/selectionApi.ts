import { request } from "./api";

export type SelectionCandidate = {
  id: number; run_id: number; market: string; symbol: string; name: string | null;
  entry_price: number | null; entry_date: string | null; hard_score: number;
  hard_rules_json: Record<string, unknown>; evidence_json: Record<string, unknown>; analysis_json: Record<string, unknown>;
  decision: string; review_notes: string | null; target_price: number | null; stop_price: number | null;
  target_return_pct: number | null; confidence: number | null; prediction_id: number | null;
  reviewed_at: string | null; created_at: string; tracking_id: number | null;
};
export type SelectionRun = {
  id: number; market: string; universe_scope: string; candidate_limit: number; criteria_json: Record<string, unknown>;
  model_instance_code: string | null; knowledge_base_ids_json: number[]; graph_ids_json: number[];
  data_mode: string; analysis_mode: string; status: string; candidate_count: number; reviewed_count: number; tracking_count: number;
  missing_data_json: string[]; error_message: string | null; as_of: string; completed_at: string | null;
  created_at: string; updated_at: string; candidates: SelectionCandidate[];
};
export type SelectionSnapshot = { id: number; trade_date: string; close_price: number; return_pct: number; drawdown_pct: number; sequence: number; source: string; data_mode: string };
export type SelectionTracking = {
  id: number; candidate_id: number; market: string; symbol: string; entry_price: number; entry_date: string;
  confirmation_date: string; adjust: string;
  target_price: number | null; stop_price: number | null; target_return_pct: number | null; confidence: number | null;
  required_sessions: number; observed_sessions: number; status: string; latest_date: string | null; latest_price: number | null;
  cumulative_return_pct: number | null; max_drawdown_pct: number | null; target_hit: boolean | null; stop_hit: boolean | null; direction_hit: boolean | null;
  data_source: string; data_mode: string; review_json: Record<string, unknown>; started_at: string; completed_at: string | null;
  snapshots: SelectionSnapshot[];
};
export type SelectionRunCreate = {
  market: string; symbols: string[]; candidate_limit: number; min_abs_change_pct: number; min_volume_ratio: number;
  model_instance_code: string | null; knowledge_base_ids: number[]; graph_ids: number[]; use_model: boolean; data_mode: "REAL";
};
export type SelectionReview = { decision: "APPROVED" | "REJECTED"; target_price?: number; stop_price?: number; target_return_pct?: number; confidence?: number; notes?: string };
export const selectionApi = {
  listRuns: () => request<SelectionRun[]>("/selection/runs"),
  getRun: (id: number) => request<SelectionRun>(`/selection/runs/${id}`),
  createRun: (payload: SelectionRunCreate) => request<SelectionRun>("/selection/runs", { method: "POST", body: JSON.stringify(payload) }),
  review: (id: number, payload: SelectionReview) => request<SelectionCandidate>(`/selection/candidates/${id}/review`, { method: "POST", body: JSON.stringify(payload) }),
  listTracking: () => request<SelectionTracking[]>("/selection/tracking"),
  getTracking: (id: number) => request<SelectionTracking>(`/selection/tracking/${id}`),
  refresh: (tracking_ids: number[] = []) => request<Record<string, unknown>>("/selection/refresh", { method: "POST", body: JSON.stringify({ tracking_ids }) }),
};
