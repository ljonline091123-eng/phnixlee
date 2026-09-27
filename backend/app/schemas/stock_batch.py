"""Contracts for bounded, user-triggered stock batch governance jobs."""

from __future__ import annotations

from datetime import datetime
from typing import Any, Literal

from pydantic import BaseModel, Field, model_validator


MASTER_BUSINESS_TYPES = ("QUOTE", "KLINE", "FINANCIAL", "NEWS", "NOTICE", "F10")


class StockBatchTarget(BaseModel):
    market: str = Field(min_length=2, max_length=16)
    symbol: str = Field(min_length=1, max_length=32)


class StockBatchGovernanceRequest(BaseModel):
    """A bounded batch request submitted from the stock master-data page.

    The request is deliberately explicit about stages.  A graph run implies
    the lakehouse and document stages because a graph without its evidence
    chain is not publishable.
    """

    stocks: list[StockBatchTarget] = Field(min_length=1, max_length=30)
    collect_business_data: bool = True
    export_lakehouse: bool = True
    archive_chunks: bool = True
    run_graph: bool = True
    run_agent_governance: bool = False
    business_types: list[Literal["QUOTE", "KLINE", "FINANCIAL", "NEWS", "NOTICE", "F10"]] = Field(
        default_factory=lambda: list(MASTER_BUSINESS_TYPES)
    )
    knowledge_base_id: int | None = Field(default=None, gt=0)
    graph_id: int | None = Field(default=None, gt=0)
    kline_days: int = Field(default=365, ge=1, le=3650)
    disclosure_days: int = Field(default=730, ge=1, le=3650)
    idempotency_key: str | None = Field(default=None, min_length=1, max_length=192)

    @model_validator(mode="after")
    def normalize_request(self) -> "StockBatchGovernanceRequest":
        seen: set[tuple[str, str]] = set()
        unique: list[StockBatchTarget] = []
        for item in self.stocks:
            pair = (item.market.strip().upper(), item.symbol.strip().upper())
            if pair in seen:
                continue
            seen.add(pair)
            unique.append(StockBatchTarget(market=pair[0], symbol=pair[1]))
        if not unique:
            raise ValueError("至少选择一只股票")
        if len(unique) > 30:
            raise ValueError("单批最多处理30只股票")
        self.stocks = unique
        self.business_types = list(dict.fromkeys(self.business_types))
        if self.collect_business_data and not self.business_types:
            raise ValueError("采集业务数据时至少选择一种数据类型")
        if self.run_graph:
            self.export_lakehouse = True
            self.archive_chunks = True
        if self.archive_chunks:
            self.export_lakehouse = True
        if not any((
            self.collect_business_data,
            self.export_lakehouse,
            self.archive_chunks,
            self.run_graph,
            self.run_agent_governance,
        )):
            raise ValueError("至少选择一个采集或治理阶段")
        return self


class StockBatchGovernanceResponse(BaseModel):
    job_id: int
    pipeline_run_id: int
    status: str
    job_status: str
    result_status: str | None = None
    stock_count: int
    current_stage: str | None = None
    progress: int = 0
    effective_options: dict[str, Any] = Field(default_factory=dict)
    worker_required: bool
    worker_task_type: str
    worker_command: str
    created_at: datetime | None = None
