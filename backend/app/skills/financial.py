from __future__ import annotations

from typing import Any

from pydantic import BaseModel, Field
from sqlalchemy.orm import Session

from app.services.market_data import get_financial_reports
from app.skills.common import (
    compact_value,
    first_matching_number,
    first_number,
    managed_session,
    resolve_stock,
)


class FinancialAnalysisInput(BaseModel):
    symbol: str = Field(min_length=1, max_length=32)
    market: str | None = None


class FinancialAnalysisOutput(BaseModel):
    symbol: str
    market: str
    name: str
    periods: list[str] = Field(default_factory=list)
    metrics: dict[str, float | str | None] = Field(default_factory=dict)
    yoy_growth: dict[str, float | str | None] = Field(default_factory=dict)
    reports: list[dict[str, Any]] = Field(default_factory=list)
    source: str = "LOCAL_DB"
    data_quality: str = "LOW"
    warnings: list[str] = Field(default_factory=list)


def _metric(payload: dict[str, Any], aliases: tuple[str, ...], fragments: tuple[str, ...] = ()) -> float | None:
    value = first_number(payload, aliases)
    if value is not None or not fragments:
        return value
    return first_matching_number(payload, fragments)


def get_financial_analysis_skill(
    symbol: str,
    market: str | None = None,
    *,
    db: Session | None = None,
) -> FinancialAnalysisOutput:
    """Extract the latest F10 indicators and year-on-year growth values."""

    parsed_input = FinancialAnalysisInput(symbol=symbol, market=market)
    with managed_session(db) as session:
        stock = resolve_stock(session, parsed_input.symbol, parsed_input.market)
        rows = get_financial_reports(session, stock.market, stock.symbol, limit=12)

        # Some providers store the complete indicator table in one row while
        # others return one row per statement. Keep the newest period first.
        reports: list[dict[str, Any]] = []
        periods: list[str] = []
        latest_payload: dict[str, Any] = {}
        for row in rows:
            payload = dict(row.data_json or {})
            period = str(row.report_period or payload.get("REPORT_DATE") or "")
            if period and period not in periods:
                periods.append(period)
            if not latest_payload:
                latest_payload = payload
            reports.append(
                {
                    "indicator": row.indicator,
                    "report_period": period,
                    "currency": row.currency,
                    "data": compact_value(payload),
                }
            )

        aliases: dict[str, tuple[str, ...]] = {
            "revenue": (
                "OPERATE_INCOME",
                "OPERATING_INCOME",
                "TOTAL_OPERATE_INCOME",
                "TOTALOPERATEREVE",
                "INCOME",
                "营业收入",
            ),
            "net_profit": (
                "HOLDER_PROFIT",
                "NET_PROFIT",
                "NETPROFIT",
                "PARENTNETPROFIT",
                "PROFIT",
                "归母净利润",
                "净利润",
            ),
            "eps": ("BASIC_EPS", "EPS", "EPSJB", "EPSKCJB", "每股收益"),
            "roe": ("ROE_AVG", "ROE_YEARLY", "ROE", "ROEJQ", "ROEKCJQ", "净资产收益率"),
            "gross_margin": ("GROSS_PROFIT_RATIO", "GROSS_MARGIN", "XSMLL", "毛利率"),
            "net_margin": ("NET_PROFIT_RATIO", "NET_MARGIN", "XSJLL", "销售净利率"),
            "debt_asset_ratio": ("DEBT_ASSET_RATIO", "ZCFZL", "资产负债率"),
            "pe": ("PE_DYNAMIC", "PE_TTM", "PELYR", "市盈率"),
            "pb": ("PB", "PBMRQ", "市净率"),
        }
        # NEEQ financial records use English metric identifiers and may be
        # stored as one-row statement snapshots rather than full F10 tables.
        # Handle those aliases while retaining the same output contract.
        if stock.market in {"NEEQ", "NEEQ_INNOVATION"}:
            aliases.update(
                {
                    "revenue": (*aliases["revenue"], "OPERATE_REVENUE", "TOTAL_REVENUE"),
                    "net_profit": (*aliases["net_profit"], "NETPROFIT", "NET_PROFIT"),
                    "roe": (*aliases["roe"], "ROE_AVG", "ROE"),
                    "gross_margin": (*aliases["gross_margin"], "GROSS_MARGIN"),
                    "net_margin": (*aliases["net_margin"], "NET_MARGIN"),
                    "debt_asset_ratio": (*aliases["debt_asset_ratio"], "DEBT_RATIO"),
                }
            )
        metrics: dict[str, float | str | None] = {
            key: _metric(payload=latest_payload, aliases=aliases_for)
            for key, aliases_for in aliases.items()
        }

        yoy_aliases = {
            "revenue_yoy": (
                "OPERATE_INCOME_YOY",
                "OPERATING_INCOME_YOY",
                "TOTALOPERATEREVETZ",
                "YSTZ",
                "营业收入同比",
            ),
            "net_profit_yoy": (
                "HOLDER_PROFIT_YOY",
                "NET_PROFIT_YOY",
                "PARENTNETPROFITTZ",
                "SJLTZ",
                "净利润同比",
            ),
            "gross_profit_yoy": ("GROSS_PROFIT_YOY", "毛利润同比"),
        }
        yoy_growth: dict[str, float | str | None] = {
            key: _metric(latest_payload, aliases_for)
            for key, aliases_for in yoy_aliases.items()
        }

        available_values = [value for value in (*metrics.values(), *yoy_growth.values()) if value is not None]
        quality = "HIGH" if len(available_values) >= 5 else "MEDIUM" if available_values else "LOW"
        warnings: list[str] = []
        if not rows:
            warnings.append("本地财务表暂无记录，请先在股票详情页刷新 F10 财务数据。")
        if len(periods) < 2:
            warnings.append("可比较的财务期数不足，无法可靠计算趋势。")

        return FinancialAnalysisOutput(
            symbol=stock.symbol,
            market=stock.market,
            name=stock.name,
            periods=periods,
            metrics=metrics,
            yoy_growth=yoy_growth,
            reports=reports,
            data_quality=quality,
            warnings=warnings,
        )
