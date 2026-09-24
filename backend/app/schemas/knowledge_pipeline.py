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
    include_company_tables: bool = True
    dataset_limit: int = Field(default=10000, ge=1, le=100000)
    chunk_size: int = Field(default=1800, ge=300, le=10000)
    overlap: int = Field(default=180, ge=0, le=2000)
    idempotency_key: str | None = Field(default=None, max_length=192)

