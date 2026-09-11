from __future__ import annotations

import json

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.schemas.stock_tools import (
    StockAnalysisRequest,
    StockAnalysisResponse,
    StockToolDefinition,
    StockToolExecuteRequest,
    StockToolExecuteResponse,
)
from app.services.model_hub import ModelHubService
from app.services.stock_tools import StockToolsService

router = APIRouter(prefix="/stock-tools", tags=["Stock Tools"])


@router.get("", response_model=list[StockToolDefinition])
def list_stock_tools(db: Session = Depends(get_db)) -> list[dict]:
    return StockToolsService(db).list_tools()


@router.post("/execute", response_model=StockToolExecuteResponse)
def execute_stock_tool(payload: StockToolExecuteRequest, db: Session = Depends(get_db)) -> StockToolExecuteResponse:
    try:
        result = StockToolsService(db).execute_tool(payload.tool_name, payload.arguments)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    return StockToolExecuteResponse(tool_name=payload.tool_name, status="SUCCESS", result=result)


@router.post("/analyze", response_model=StockAnalysisResponse)
def analyze_stock(payload: StockAnalysisRequest, db: Session = Depends(get_db)) -> StockAnalysisResponse:
    tools_service = StockToolsService(db)
    try:
        tool_results = tools_service.collect_analysis_context(payload.market, payload.symbol, refresh=payload.refresh)
        stock_profile = tools_service.get_stock_profile(payload.market, payload.symbol)
        messages = [
            {
                "role": "system",
                "content": (
                    "你是一个专业的股票分析助手。请结合工具返回的实时行情、K线、新闻和公告，"
                    "给出简洁、客观的投资分析。输出应包含：趋势判断、基本面提示、新闻/公告影响、风险点。"
                ),
            },
            {
                "role": "tool",
                "content": json.dumps(
                    {
                        "stock_profile": stock_profile,
                        "tool_results": tool_results,
                    },
                    ensure_ascii=False,
                    default=str,
                ),
            },
            {"role": "user", "content": payload.question},
        ]
        log = ModelHubService(db).chat(
            task_type="stock_analysis",
            instance_code=payload.instance_code,
            messages=messages,
            metadata_json={
                "market": payload.market,
                "symbol": payload.symbol,
                "tool_names": list(tool_results.keys()),
                "skill_code": "STOCK_TREND_ADVISOR",
            },
        )
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    return StockAnalysisResponse(
        call_log_id=log.id,
        task_type=log.task_type,
        provider_code=log.provider_code,
        instance_code=log.instance_code,
        model_code=log.model_code,
        status=log.status,
        response_text=log.response_text or log.error_message or "",
        tool_results=tool_results,
    )
