from __future__ import annotations

from typing import Any

from pydantic import BaseModel, Field
from sqlalchemy.orm import Session

from app.services.market_data import get_fund_flow_cache, get_latest_quote, get_recent_klines
from app.skills.common import (
    compact_value,
    first_matching_number,
    first_number,
    managed_session,
    number,
    resolve_stock,
)


class CapitalAndTechInput(BaseModel):
    symbol: str = Field(min_length=1, max_length=32)
    market: str | None = None


class CapitalAndTechOutput(BaseModel):
    symbol: str
    market: str
    name: str
    latest_close: float | None = None
    moving_averages: dict[str, float | None] = Field(default_factory=dict)
    support_resistance: dict[str, float | None] = Field(default_factory=dict)
    recent_kline: list[dict[str, Any]] = Field(default_factory=list)
    capital_flow: list[dict[str, Any]] = Field(default_factory=list)
    capital_summary: dict[str, float | str | None] = Field(default_factory=dict)
    trend: str = "UNKNOWN"
    source: str = "LOCAL_DB"
    data_quality: str = "LOW"
    warnings: list[str] = Field(default_factory=list)


def _mean(values: list[float]) -> float | None:
    return sum(values) / len(values) if values else None


def get_capital_and_tech_skill(
    symbol: str,
    market: str | None = None,
    *,
    db: Session | None = None,
) -> CapitalAndTechOutput:
    """Extract 20-day capital-flow context and key K-line levels."""

    parsed_input = CapitalAndTechInput(symbol=symbol, market=market)
    with managed_session(db) as session:
        stock = resolve_stock(session, parsed_input.symbol, parsed_input.market)
        kline_rows = get_recent_klines(session, stock.market, stock.symbol, limit=60)
        ordered = list(reversed(kline_rows))
        closes = [number(row.close_price) for row in ordered if number(row.close_price) is not None]
        highs = [number(row.high_price) for row in ordered if number(row.high_price) is not None]
        lows = [number(row.low_price) for row in ordered if number(row.low_price) is not None]
        latest_close = closes[-1] if closes else None
        moving_averages = {
            "ma5": _mean(closes[-5:]),
            "ma10": _mean(closes[-10:]),
            "ma20": _mean(closes[-20:]),
        }
        recent_lows = lows[-20:]
        recent_highs = highs[-20:]
        support_resistance = {
            "support_20d": min(recent_lows) if recent_lows else None,
            "resistance_20d": max(recent_highs) if recent_highs else None,
            "latest_low": lows[-1] if lows else None,
            "latest_high": highs[-1] if highs else None,
        }

        payload = get_fund_flow_cache(session, stock.market, stock.symbol)
        flow_rows = payload.get("rows") if isinstance(payload.get("rows"), list) else []
        flow_rows = [dict(row) for row in flow_rows[-20:] if isinstance(row, dict)]
        net_values: list[float] = []
        for row in flow_rows:
            value = first_number(
                row,
                (
                    "主力净流入-净额",
                    "主力净流入",
                    "主力净额",
                    "net_inflow",
                    "主力净流入净额",
                ),
            )
            if value is None:
                value = first_matching_number(row, ("主力", "净"))
            if value is None:
                # Keep the tool useful for providers that use an opaque column
                # name: the first numeric amount is a conservative fallback.
                value = next((number(item) for item in row.values() if number(item) is not None), None)
            if value is not None:
                net_values.append(value)

        quote = get_latest_quote(session, stock.market, stock.symbol)
        total_net = sum(net_values) if net_values else None
        capital_summary: dict[str, float | str | None] = {
            "net_inflow_20d": total_net,
            "average_daily_net_inflow": _mean(net_values),
            "flow_days": len(net_values),
            "quote_change_pct": quote.change_pct if quote else None,
        }

        if latest_close is not None and moving_averages["ma20"] is not None:
            if latest_close > moving_averages["ma20"] and (
                moving_averages["ma5"] is None or moving_averages["ma5"] >= moving_averages["ma20"]
            ):
                trend = "UP"
            elif latest_close < moving_averages["ma20"]:
                trend = "DOWN"
            else:
                trend = "RANGE"
        else:
            trend = "UNKNOWN"

        warnings: list[str] = []
        if len(kline_rows) < 20:
            warnings.append("本地 K 线不足 20 个交易日，均线和支撑位仅供参考。")
        if not flow_rows:
            warnings.append("本地资金流向暂无记录，主力意图无法充分判断。")
        quality = "HIGH" if len(kline_rows) >= 20 and flow_rows else "MEDIUM" if kline_rows else "LOW"

        return CapitalAndTechOutput(
            symbol=stock.symbol,
            market=stock.market,
            name=stock.name,
            latest_close=latest_close,
            moving_averages=moving_averages,
            support_resistance=support_resistance,
            recent_kline=[
                {
                    "trade_date": row.trade_date,
                    "open_price": row.open_price,
                    "high_price": row.high_price,
                    "low_price": row.low_price,
                    "close_price": row.close_price,
                    "volume": row.volume,
                    "amount": row.amount,
                }
                for row in kline_rows[-20:]
            ],
            capital_flow=[compact_value(row) for row in flow_rows],
            capital_summary=capital_summary,
            trend=trend,
            data_quality=quality,
            warnings=warnings,
        )
