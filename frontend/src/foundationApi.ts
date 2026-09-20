import { request } from "./api";

export type FoundationPage<T> = { items: T[]; total: number; limit: number; offset: number };
export type FoundationStatus = {
  enabled: boolean;
  counts: { entities: number; securities: number; listings: number; evidence: number; facts: number; pending_facts: number; accepted_facts: number; rejected_facts: number };
  legacy_stock_count: number;
};
export type FoundationEntity = {
  id: string; name: string; entity_type: string; jurisdiction: string;
  identifier_scheme: string | null; identifier_value: string | null;
  properties_json: Record<string, unknown>; created_at: string;
};
export type FoundationListing = { id: string; security_id: string; stock_symbol_id: number; market: string; symbol: string; name: string; share_class: string | null; entity_id?: string };
export type FoundationEntityDetail = FoundationEntity & { listings: FoundationListing[] };
export type FoundationStockOption = { id: number; market: string; symbol: string; name: string; exchange: string | null; mapped_entity_id?: string | null };
export type FoundationEvidence = {
  id: string; entity_id: string | null; source_name: string; source_key: string; title: string;
  content?: string; url: string | null; published_at: string | null; available_at: string | null;
  content_hash: string; version: number; metadata_json: Record<string, unknown>; created_at: string;
};
export type FoundationReview = { id: string; previous_status: string; decision: string; reason: string; reviewer: string; created_at: string };
export type FoundationFact = {
  id: string; fact_type: string; title: string; subject_entity_id: string; object_entity_id: string | null;
  subject_name: string; object_name: string | null; properties_json: Record<string, unknown>;
  status: string; evidence_ids: string[]; valid_from: string | null; valid_to: string | null;
  created_at: string; updated_at: string;
};
export type FoundationFactDetail = FoundationFact & { evidence: FoundationEvidence[]; reviews: FoundationReview[] };
export type FoundationCatalog = { entity_types: string[]; fact_types: string[]; fact_schemas: Record<string, Record<string, unknown>>; statuses: string[] };
export type EntityInput = Pick<FoundationEntity, "name" | "entity_type" | "jurisdiction" | "identifier_scheme" | "identifier_value" | "properties_json">;
export type EvidenceInput = Pick<FoundationEvidence, "entity_id" | "source_name" | "source_key" | "title" | "url" | "published_at" | "available_at"> & { content: string };
export type FactInput = Pick<FoundationFact, "fact_type" | "title" | "subject_entity_id" | "object_entity_id" | "properties_json" | "evidence_ids" | "valid_from" | "valid_to">;

type Query = Record<string, string | number | undefined>;
function query(values: Query) {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(values)) if (value !== undefined && value !== "") params.set(key, String(value));
  return params.toString();
}
const prefix = "/foundation";
export const foundationApi = {
  status: () => request<FoundationStatus>(`${prefix}/status`),
  catalog: () => request<FoundationCatalog>(`${prefix}/catalog`),
  entities: (params: Query = {}) => request<FoundationPage<FoundationEntity>>(`${prefix}/entities?${query(params)}`),
  entity: (id: string) => request<FoundationEntityDetail>(`${prefix}/entities/${encodeURIComponent(id)}`),
  createEntity: (body: EntityInput) => request<FoundationEntity>(`${prefix}/entities`, { method: "POST", body: JSON.stringify(body) }),
  stocks: (params: Query = {}) => request<FoundationPage<FoundationStockOption>>(`${prefix}/stock-options?${query(params)}`),
  mapSecurity: (body: { entity_id: string; stock_symbol_id: number; evidence_id: string; share_class?: string }) => request<FoundationListing>(`${prefix}/security-mappings`, { method: "POST", body: JSON.stringify(body) }),
  evidence: (params: Query = {}) => request<FoundationPage<FoundationEvidence>>(`${prefix}/evidence?${query(params)}`),
  evidenceDetail: (id: string) => request<FoundationEvidence>(`${prefix}/evidence/${encodeURIComponent(id)}`),
  createEvidence: (body: EvidenceInput) => request<FoundationEvidence>(`${prefix}/evidence`, { method: "POST", body: JSON.stringify(body) }),
  importEvidence: (body: { document_id: number; entity_id?: string }) => request<FoundationEvidence>(`${prefix}/evidence/import-legacy`, { method: "POST", body: JSON.stringify(body) }),
  facts: (params: Query = {}) => request<FoundationPage<FoundationFact>>(`${prefix}/facts?${query(params)}`),
  fact: (id: string) => request<FoundationFactDetail>(`${prefix}/facts/${encodeURIComponent(id)}`),
  createFact: (body: FactInput) => request<FoundationFact>(`${prefix}/facts`, { method: "POST", body: JSON.stringify(body) }),
  review: (id: string, body: { decision: "ACCEPTED" | "REJECTED"; reason: string; reviewer: string; expected_status: string }) => request<FoundationFact>(`${prefix}/facts/${encodeURIComponent(id)}/review`, { method: "POST", body: JSON.stringify(body) }),
};
