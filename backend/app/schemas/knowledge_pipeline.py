from __future__ import annotations

from pydantic import BaseModel, Field


class KnowledgePipelineRequest(BaseModel):
    market: str = Field(default="CN_A", min_length=2, max_length=16)
    symbols: list[str] = Field(min_length=1, max_length=200)
    knowledge_base_id: int | None = Field(default=None, gt=0)
    graph_id: int | None = Field(default=None, gt=0)
    export_lakehouse: bool = True
    archive_chunks: bool = True
    run_graph: bool = False
    run_agent_governance: bool = False
    governance_record_limit: int = Field(default=50, ge=1, le=500)
    governance_run_key: str | None = Field(default=None, max_length=32)
    include_company_tables: bool = True
    dataset_limit: int = Field(default=10000, ge=1, le=100000)
    graph_documents_per_stock: int | None = Field(default=50, ge=1, le=5000)
    chunk_size: int = Field(default=1800, ge=300, le=10000)
    overlap: int = Field(default=180, ge=0, le=2000)
    # Keep the local MVP bounded by default, while allowing a deliberate
    # full-scope archival run with ``null`` when storage capacity permits.
    chunk_document_limit: int | None = Field(default=10000, ge=1, le=100000)
    idempotency_key: str | None = Field(default=None, max_length=192)

