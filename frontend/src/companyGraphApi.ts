import { request } from "./api";
import type { FoundationEntity, FoundationEvidence, FoundationFact, FoundationPage } from "./foundationApi";

export type CompanyCoverage = {
  dimensions: Array<{ key: string; status: "PRESENT" | "PENDING" | "NOT_COLLECTED" | "DISCLOSURES_ONLY"; count: number; pending_count: number; evidence_count: number; disclosure_count?: number; disclosure_status?: string }>;
  missing: string[]; pending: string[];
};
export type CompanySecurity = {
  stock_symbol_id: number | null; listing_id: string | null; security_id: string | null; company_id: string | null;
  name: string; symbol: string; market: string; company_name?: string;
  mapping_status?: string; mapping_reason?: string | null; share_class?: string | null; coverage?: CompanyCoverage;
  canonical_security_id?: string; canonical_company_id?: string | null; identity_version?: string;
};
export type CompanySearchItem = {
  company_id: string; name: string; jurisdiction: string; identifier_scheme: string | null;
  identifier_value: string | null; securities: CompanySecurity[];
};
export type CompanyClassification = {
  id: string; security_id: string; dimension: string; code: string; label: string;
  definition_version: string; method: string; properties_json: Record<string, unknown>;
  status: string; valid_from: string | null; valid_to: string | null; evidence_id: string;
  created_at: string; updated_at: string;
  reviews: Array<{ decision: string; reason: string; reviewer: string; created_at: string }>;
  /** Optional master-data term definition attached by newer APIs. */
  definition?: ClassificationDefinition | null;
};
export type ClassificationDefinition = {
  id?: string;
  taxonomy?: string;
  dimension: string;
  code: string;
  label: string;
  definition: string;
  criteria?: string | null;
  parent_code?: string | null;
  jurisdiction?: string;
  source_name?: string;
  source_url?: string | null;
  definition_version?: string;
  status?: string;
  valid_from?: string | null;
  valid_to?: string | null;
  properties_json?: Record<string, unknown>;
};
export type CompanyProfile = {
  company: FoundationEntity & { canonical_company_id?: string }; securities: CompanySecurity[]; selected_security?: CompanySecurity;
  facts: { ownership: FoundationFact[]; industry: FoundationFact[]; business: FoundationFact[]; legal: FoundationFact[]; other: FoundationFact[] };
  classifications: CompanyClassification[]; evidence: FoundationEvidence[];
  legal_disclosures?: Array<FoundationEvidence & { association_role: string }>;
  business_disclosures?: Array<FoundationEvidence & { association_role: string }>;
  supply_chain_disclosures?: Array<FoundationEvidence & { association_role: string }>;
  coverage: CompanyCoverage; truncated: { facts: boolean; evidence: boolean };
  sources: string[]; updated_at: string | null;
  profile_evidence_id?: string | null;
};
export type CompanyGraphData = {
  nodes: Array<{ id: string; label: string; type: string; canonical_security_id?: string; canonical_company_id?: string | null; identity_version?: string }>;
  edges: Array<{ id: string; source: string; target: string; type: string; status: string; fact_id?: string; evidence_ids: string[]; valid_from?: string | null; valid_to?: string | null; properties_json?: Record<string, unknown> }>;
  paths: Array<{ node_ids: string[]; edge_ids: string[] }>;
  truncated: boolean; as_of: string | null; known_at: string | null;
};
export type CompanyGraphStatus = {
  entity_count: number; security_count: number; listing_count: number; accepted_fact_count: number;
  pending_fact_count: number; classification_count: number; scope: string; data_mode: string;
};
export type CompanyGovernanceResult = {
  requested?: number; mapped?: number; source_status?: string;
  items?: Array<{ market: string; symbol: string; stock_symbol_id?: number; company_id?: string; sources?: Record<string, { status?: string; warnings?: string[]; request?: Record<string, unknown>; disclosure_count?: number }> }>;
};
export type CompanyGovernanceJob = { id: number; status: string; attempts?: number; error_message: string | null; result: CompanyGovernanceResult | null; created_at?: string; completed_at?: string | null };
export type CompanyGovernanceStatus = {
  sources: Array<{ code: string; name: string; enabled: boolean; capabilities: string[]; authorization_status: string; description: string }>;
  restricted_sources: Array<{ source_code: string; name: string; source_url: string; status: string; connected: boolean }>;
  latest_run: CompanyGovernanceJob | null; max_stocks_per_job: number; default_accept_structured: boolean;
};
type Query = Record<string, string | number | boolean | undefined>;
function params(values: Query) {
  const output = new URLSearchParams();
  for (const [key, value] of Object.entries(values)) if (value !== undefined && value !== "") output.set(key, String(value));
  return output.toString();
}
const base = "/company-graph";
export const companyGraphApi = {
  status: () => request<CompanyGraphStatus>(`${base}/status`),
  samples: (query: Query = {}, signal?: AbortSignal) => request<FoundationPage<CompanySecurity>>(`${base}/samples?${params(query)}`, { signal }),
  searchSecurities: (query: Query = {}, signal?: AbortSignal) => request<FoundationPage<CompanySecurity>>(`${base}/search?${params({ ...query, mode: "security" })}`, { signal }),
  searchCompanies: (query: Query = {}, signal?: AbortSignal) => request<FoundationPage<CompanySearchItem>>(`${base}/search?${params({ ...query, mode: "company" })}`, { signal }),
  company: (id: string, signal?: AbortSignal) => request<CompanyProfile>(`${base}/companies/${encodeURIComponent(id)}`, { signal }),
  security: (id: number, signal?: AbortSignal) => request<CompanyProfile>(`${base}/securities/${id}`, { signal }),
  bySymbol: (market: string, symbol: string, signal?: AbortSignal) => request<CompanyProfile>(`${base}/by-symbol?${params({ market, symbol })}`, { signal }),
  graph: (query: Query) => request<CompanyGraphData>(`${base}/graph?${params(query)}`),
  /** Versioned industry/theme/type glossary. Empty/404 is handled by callers. */
  taxonomyDefinitions: (query: Query = {}) => request<ClassificationDefinition[]>(`${base}/taxonomies?${params(query)}`),
  governanceStatus: () => request<CompanyGovernanceStatus>("/company-governance/status"),
  governanceJob: (id: number) => request<CompanyGovernanceJob>(`/company-governance/jobs/${id}`),
  submitGovernance: (body: { stock_symbol_ids: number[]; include_holders: boolean; include_legal: boolean; include_business: boolean; include_supply_chain: boolean; accept_structured: boolean; request_key: string }) => request<CompanyGovernanceJob>("/company-governance/jobs", { method: "POST", body: JSON.stringify(body) }),
};
