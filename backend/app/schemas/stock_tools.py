from __future__ import annotations

from typing import Any

from pydantic import BaseModel, Field


class StockToolDefinition(BaseModel):
    name: str
    description: str
    input_schema: dict[str, Any]


class StockToolExecuteRequest(BaseModel):
    tool_name: str = Field(min_length=1, max_length=64)
    arguments: dict[str, Any] = Field(default_factory=dict)


class StockToolExecuteResponse(BaseModel):
    tool_name: str
    status: str
    result: dict[str, Any]


class StockAnalysisRequest(BaseModel):
    market: str = Field(pattern=r"^(CN_A|HK|NEEQ|NEEQ_INNOVATION)$")
    symbol: str = Field(min_length=1, max_length=32)
    question: str = Field(
        default="请分析这只股票近期的行情、新闻和公告，并给出风险提示与观察建议。",
        min_length=1,
        max_length=4000,
    )
    refresh: bool = False
    instance_code: str | None = None


class StockAnalysisResponse(BaseModel):
    call_log_id: int
    task_type: str
    provider_code: str | None
    instance_code: str | None
    model_code: str | None
    status: str
    response_text: str
    tool_results: dict[str, Any]
