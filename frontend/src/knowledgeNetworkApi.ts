import { request } from "./api";

export type NetworkLayer = "MASTER" | "FACT" | "EVIDENCE";
export type NetworkEvidence = {
  id: string; title: string; source_name?: string; source_url?: string | null; url?: string | null;
  source_table?: string; document_type?: string; published_at?: string | null; observed_at?: string | null; excerpt?: string;
  content?: string; content_truncated?: boolean; source_record_id?: string | number;
};
type NetworkDetail = {
  id: string; type: string; label: string; layer: NetworkLayer; status?: string;
  type_label?: string; properties?: Record<string, unknown>; properties_json?: Record<string, unknown>;
  evidence_ids?: string[]; evidence?: NetworkEvidence[]; definition?: string;
  valid_from?: string | null; valid_to?: string | null; observed_at?: string | null;
  extraction_method?: string; source_reliability?: number | null; extraction_confidence?: number | null;
};
export type NetworkNode = NetworkDetail;
export type NetworkEdge = NetworkDetail & { source: string; target: string };
export type NetworkCoverage = { key: string; label: string; status: string; count: number; note?: string; pending_count?: number };
export type KnowledgeNetwork = {
  center_id: string; nodes: NetworkNode[]; edges: NetworkEdge[]; evidence?: NetworkEvidence[];
  coverage: NetworkCoverage[]; stats: Record<string, unknown>; truncated: boolean;
  notes?: string[]; centers?: Array<{ id: string; label: string }>; generated_at?: string;
  category?: NetworkQuery["category"]; category_node_ids?: string[];
};
export type NetworkQuery = {
  market?: string; symbol?: string; company_id?: string; center_id?: string; graph_id?: number;
  depth?: number; max_nodes?: number; max_edges?: number; per_category?: number;
  include_candidates?: boolean; include_evidence?: boolean;
  category?: "identity" | "classification" | "ownership" | "financial" | "market" | "disclosure" | "business" | "legal" | "evidence";
};
export const knowledgeNetworkApi = {
  evidence: (sourceTable: string, recordId: string | number, signal?: AbortSignal) => request<NetworkEvidence>(`/knowledge-network/evidence?${new URLSearchParams({ source_table: sourceTable, record_id: String(recordId) })}`, { signal }),
  explore: async (query: NetworkQuery, signal?: AbortSignal) => {
    const params = new URLSearchParams();
    for (const [key, value] of Object.entries(query)) if (value !== undefined && value !== "") params.set(key, String(value));
    try { return await request<KnowledgeNetwork>(`/knowledge-network/explore?${params}`, { signal }); }
    catch (error) {
      if (error instanceof Error && (error.message === "Not Found" || error.message === "请求失败 (404)")) {
        throw new Error("知识图谱服务尚未就绪，请刷新后重试。若持续失败，请检查服务是否已更新。");
      }
      throw error;
    }
  },
};
