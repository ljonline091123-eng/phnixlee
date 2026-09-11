from __future__ import annotations

from datetime import date, datetime, timedelta
from typing import Any

from sqlalchemy import desc, select
from sqlalchemy.orm import Session

from app.core.markets import LIVE_REFRESH_MARKETS, MARKET_HK, MASTER_MARKETS, digit_length_for_market
from app.connectors.registry import get_adapter
from app.models.market_data import (
    DataSource,
    StockFinancialReport,
    StockKline,
    StockNews,
    StockNotice,
    StockRealtimeQuote,
    StockSymbol,
)
from app.schemas.market_data import StockF10Read
from app.services.stock_on_demand import StockOnDemandService
from app.services.catalog import select_data_source


STOCK_TOOL_DEFINITIONS: tuple[dict[str, Any], ...] = (
    {
        "name": "get_realtime_quote",
        "description": "Load a realtime quote from the local database; set refresh=true to fetch and persist latest data.",
        "input_schema": {
            "type": "object",
            "required": ["market", "symbol"],
            "properties": {
                "market": {"type": "string", "enum": list(MASTER_MARKETS)},
                "symbol": {"type": "string", "description": "Stock code without exchange suffix."},
                "refresh": {"type": "boolean", "default": False},
            },
        },
    },
    {
        "name": "get_historical_kline",
        "description": "Load historical daily K-line data from the local database; set refresh=true to fetch and persist latest data.",
        "input_schema": {
            "type": "object",
            "required": ["market", "symbol"],
            "properties": {
                "market": {"type": "string", "enum": list(MASTER_MARKETS)},
                "symbol": {"type": "string", "description": "Stock code without exchange suffix."},
                "limit": {"type": "integer", "minimum": 1, "maximum": 500},
                "refresh": {"type": "boolean", "default": False},
            },
        },
    },
    {
        "name": "get_latest_news",
        "description": "Load latest stock news from the local database; set refresh=true to fetch and persist latest data.",
        "input_schema": {
            "type": "object",
            "required": ["market", "symbol"],
            "properties": {
                "market": {"type": "string", "enum": list(MASTER_MARKETS)},
                "symbol": {"type": "string", "description": "Stock code without exchange suffix."},
                "limit": {"type": "integer", "minimum": 1, "maximum": 100},
                "refresh": {"type": "boolean", "default": False},
            },
        },
    },
    {
        "name": "get_latest_notices",
        "description": "Load latest announcements from the local database; set refresh=true to fetch and persist latest data.",
        "input_schema": {
            "type": "object",
            "required": ["market", "symbol"],
            "properties": {
                "market": {"type": "string", "enum": list(MASTER_MARKETS)},
                "symbol": {"type": "string", "description": "Stock code without exchange suffix."},
                "limit": {"type": "integer", "minimum": 1, "maximum": 100},
                "refresh": {"type": "boolean", "default": False},
            },
        },
    },
)

F10_EXTENDED_SECTIONS: tuple[str, ...] = (
    "published_reports",
    "profile",
    "holders",
    "fund_flow",
    "financial_summary",
    "financial_statements",
    "business_composition",
)


class StockToolsService:
    def __init__(self, db: Session):
        self.db = db

    def get_stock_profile(self, market: str, symbol: str) -> dict[str, Any]:
        market = market.upper()
        normalized_symbol = self._normalize_symbol(market, symbol)
        stock = self._get_symbol(market, normalized_symbol)
        quote = self.db.scalar(
            select(StockRealtimeQuote)
            .where(StockRealtimeQuote.market == market, StockRealtimeQuote.symbol == normalized_symbol)
            .order_by(desc(StockRealtimeQuote.fetched_at))
        )
        news = list(
            self.db.scalars(
                select(StockNews)
                .where(StockNews.market == market, StockNews.symbol == normalized_symbol)
                .order_by(desc(StockNews.news_time))
                .limit(5)
            ).all()
        )
        notices = list(
            self.db.scalars(
                select(StockNotice)
                .where(StockNotice.market == market, StockNotice.symbol == normalized_symbol)
                .order_by(desc(StockNotice.notice_date))
                .limit(5)
            ).all()
        )
        return {
            "symbol": self._serialize_model(stock),
            "realtime_quote": self._serialize_model(quote) if quote else None,
            "news": [self._serialize_model(item) for item in news],
            "notices": [self._serialize_model(item) for item in notices],
        }

    def list_tools(self) -> list[dict[str, Any]]:
        return [dict(item) for item in STOCK_TOOL_DEFINITIONS]

    def execute_tool(self, tool_name: str, arguments: dict[str, Any]) -> dict[str, Any]:
        definition_names = {item["name"] for item in STOCK_TOOL_DEFINITIONS}
        if tool_name not in definition_names:
            raise ValueError(f"Unknown stock tool: {tool_name}")

        market = str(arguments.get("market") or "").upper()
        symbol = self._normalize_symbol(market, str(arguments.get("symbol") or ""))
        if market not in MASTER_MARKETS or not symbol:
            raise ValueError(f"market must be one of: {', '.join(MASTER_MARKETS)} and symbol is required")
        self._get_symbol(market, symbol)

        refresh = bool(arguments.get("refresh", False))
        limit = self._safe_limit(arguments.get("limit"), 60 if tool_name == "get_historical_kline" else 10)
        fetch_service = StockOnDemandService(self.db)
        source = self._default_source()
        can_refresh = refresh and market in LIVE_REFRESH_MARKETS

        if tool_name == "get_realtime_quote":
            fetch_log_id = None
            quote = self.db.scalar(
                select(StockRealtimeQuote)
                .where(StockRealtimeQuote.market == market, StockRealtimeQuote.symbol == symbol)
                .order_by(desc(StockRealtimeQuote.fetched_at))
            )
            if can_refresh and (refresh or quote is None):
                fetch_log, _ = fetch_service.fetch_quote(source, market, symbol, persist=True)
                fetch_log_id = fetch_log.id
            quote = self.db.scalar(
                select(StockRealtimeQuote)
                .where(StockRealtimeQuote.market == market, StockRealtimeQuote.symbol == symbol)
                .order_by(desc(StockRealtimeQuote.fetched_at))
            )
            return {
                "market": market,
                "symbol": symbol,
                "fetch_log_id": fetch_log_id,
                "quote": self._serialize_model(quote) if quote else None,
            }

        if tool_name == "get_historical_kline":
            fetch_log_id = None
            rows = list(
                self.db.scalars(
                    select(StockKline)
                    .where(StockKline.market == market, StockKline.symbol == symbol)
                    .order_by(StockKline.trade_date.desc())
                    .limit(limit)
                ).all()
            )
            if can_refresh and (refresh or not rows):
                fetch_log, _ = fetch_service.fetch_kline(
                    source=source,
                    market=market,
                    symbol=symbol,
                    period="daily",
                    adjust="",
                    start_date=(date.today() - timedelta(days=365)).strftime("%Y%m%d"),
                    end_date=date.today().strftime("%Y%m%d"),
                    persist=True,
                )
                fetch_log_id = fetch_log.id
            rows = list(
                self.db.scalars(
                    select(StockKline)
                    .where(StockKline.market == market, StockKline.symbol == symbol)
                    .order_by(StockKline.trade_date.desc())
                    .limit(limit)
                ).all()
            )
            return {
                "market": market,
                "symbol": symbol,
                "fetch_log_id": fetch_log_id,
                "items": [self._serialize_model(item) for item in rows],
            }

        if tool_name == "get_latest_news":
            fetch_log_id = None
            rows = list(
                self.db.scalars(
                    select(StockNews)
                    .where(StockNews.market == market, StockNews.symbol == symbol)
                    .order_by(desc(StockNews.news_time))
                    .limit(limit)
                ).all()
            )
            if can_refresh and (refresh or not rows):
                fetch_log, _ = fetch_service.fetch_news(source, market, symbol, persist=True)
                fetch_log_id = fetch_log.id
            rows = list(
                self.db.scalars(
                    select(StockNews)
                    .where(StockNews.market == market, StockNews.symbol == symbol)
                    .order_by(desc(StockNews.news_time))
                    .limit(limit)
                ).all()
            )
            return {
                "market": market,
                "symbol": symbol,
                "fetch_log_id": fetch_log_id,
                "items": [self._serialize_model(item) for item in rows],
            }

        fetch_log_id = None
        rows = list(
            self.db.scalars(
                select(StockNotice)
                .where(StockNotice.market == market, StockNotice.symbol == symbol)
                .order_by(desc(StockNotice.notice_date))
                .limit(limit)
            ).all()
        )
        if can_refresh and (refresh or not rows):
            fetch_log, _ = fetch_service.fetch_notices(
                source=source,
                market=market,
                symbol=symbol,
                start_date=(date.today() - timedelta(days=180)).strftime("%Y%m%d"),
                end_date=date.today().strftime("%Y%m%d"),
                persist=True,
            )
            fetch_log_id = fetch_log.id
        rows = list(
            self.db.scalars(
                select(StockNotice)
                .where(StockNotice.market == market, StockNotice.symbol == symbol)
                .order_by(desc(StockNotice.notice_date))
                .limit(limit)
            ).all()
        )
        return {
            "market": market,
            "symbol": symbol,
            "fetch_log_id": fetch_log_id,
            "items": [self._serialize_model(item) for item in rows],
        }

    def collect_analysis_context(self, market: str, symbol: str, refresh: bool = False) -> dict[str, Any]:
        context: dict[str, Any] = {}
        for definition in STOCK_TOOL_DEFINITIONS:
            arguments = {"market": market, "symbol": symbol, "refresh": refresh}
            if definition["name"] == "get_historical_kline":
                arguments["limit"] = 30
            else:
                arguments["limit"] = 8
            try:
                context[definition["name"]] = self.execute_tool(definition["name"], arguments)
            except Exception as exc:
                context[definition["name"]] = {"error": str(exc)}
        return context

    def get_f10_snapshot(self, market: str, symbol: str, persist: bool = False) -> StockF10Read:
        market = market.upper()
        normalized_symbol = self._normalize_symbol(market, symbol)
        stock = self._get_symbol(market, normalized_symbol)
        extended_data = self._load_f10_extended_data(market, normalized_symbol)

        source = select_data_source(
            self.db,
            market,
            "F10",
            fallback_code="AKSHARE",
        ) or self._default_source()
        has_cache = self._has_f10_extended_data(extended_data)
        should_refresh = (
            market in LIVE_REFRESH_MARKETS
            and (
                persist
                or not has_cache
                or (
                    market == MARKET_HK and self._hk_published_reports_need_refresh(extended_data.get("published_reports"))
                )
            )
        )
        if should_refresh:
            try:
                fetched_extended_data = get_adapter(source.adapter_type).fetch_extended_data(market, normalized_symbol)
                self._persist_f10_extended_data(source, market, normalized_symbol, fetched_extended_data)
                extended_data = {**self._empty_f10_extended_data(), **fetched_extended_data}
            except Exception as exc:
                self._mark_f10_refresh_failed(extended_data, str(exc)[:180])
            try:
                StockOnDemandService(self.db).fetch_quote(source=source, market=market, symbol=normalized_symbol, persist=True)
            except Exception as exc:
                self._mark_f10_refresh_failed(extended_data, str(exc)[:180])

        quote = self.db.scalar(
            select(StockRealtimeQuote)
            .where(StockRealtimeQuote.market == market, StockRealtimeQuote.symbol == normalized_symbol)
            .order_by(desc(StockRealtimeQuote.fetched_at))
        )
        klines = list(
            self.db.scalars(
                select(StockKline)
                .where(StockKline.market == market, StockKline.symbol == normalized_symbol)
                .order_by(StockKline.trade_date.desc())
                .limit(90)
            ).all()
        )
        financials = list(
            self.db.scalars(
                select(StockFinancialReport)
                .where(StockFinancialReport.market == market, StockFinancialReport.symbol == normalized_symbol)
                .order_by(StockFinancialReport.report_period.desc())
                .limit(8)
            ).all()
        )
        notices = list(
            self.db.scalars(
                select(StockNotice)
                .where(StockNotice.market == market, StockNotice.symbol == normalized_symbol)
                .order_by(StockNotice.notice_date.desc())
                .limit(8)
            ).all()
        )
        news = list(
            self.db.scalars(
                select(StockNews)
                .where(StockNews.market == market, StockNews.symbol == normalized_symbol)
                .order_by(StockNews.news_time.desc())
                .limit(8)
            ).all()
        )
        return StockF10Read(
            symbol=stock,
            realtime_quote=quote,
            recent_klines=klines,
            financial_reports=financials,
            notices=notices,
            news=news,
            published_reports=extended_data.get("published_reports") or {},
            profile=extended_data.get("profile") or {},
            holders=extended_data.get("holders") or {},
            fund_flow=extended_data.get("fund_flow") or {},
            financial_summary=extended_data.get("financial_summary") or {},
            financial_statements=extended_data.get("financial_statements") or {},
            business_composition=extended_data.get("business_composition") or {},
        )

    def generate_analysis_prompt(self, market: str, symbol: str) -> str:
        snapshot = self.get_stock_profile(market, symbol)
        quote = snapshot.get("realtime_quote") or {}
        news = snapshot.get("news") or []
        notices = snapshot.get("notices") or []
        return (
            f"Stock: {snapshot['symbol']['name']} ({snapshot['symbol']['symbol']})\n"
            f"Market: {snapshot['symbol']['market']}; exchange: {snapshot['symbol']['exchange']}\n"
            f"Latest price: {quote.get('current_price')}\n"
            f"Change: {quote.get('change_amount')} / {quote.get('change_pct')}\n"
            f"News count: {len(news)}; announcement count: {len(notices)}\n"
            "Please provide a concise investment analysis with trend, fundamentals, news impact, risks, and watch points."
        )

    def _empty_f10_extended_data(self, message: str = "Local F10 cache is empty. Click refresh to fetch latest data.") -> dict[str, dict[str, Any]]:
        return {
            "profile": {"source": "LOCAL_DB", "fields": {}, "message": message},
            "holders": {"source": "LOCAL_DB", "major": [], "circulating": [], "message": message},
            "fund_flow": {"source": "LOCAL_DB", "rows": [], "message": message},
            "financial_summary": {"source": "LOCAL_DB", "periods": [], "rows": [], "message": message},
            "financial_statements": {
                "source": "LOCAL_DB",
                "balance_sheet": {"label": "Balance Sheet", "periods": []},
                "income_statement": {"label": "Income Statement", "periods": []},
                "cash_flow": {"label": "Cash Flow", "periods": []},
                "message": message,
            },
            "business_composition": {"source": "LOCAL_DB", "report_date": None, "sections": [], "message": message},
            "published_reports": {"source": "LOCAL_DB", "reports": [], "message": message},
        }

    def _has_f10_extended_data(self, extended_data: dict[str, dict[str, Any]]) -> bool:
        profile = extended_data.get("profile")
        if isinstance(profile, dict) and bool(profile.get("fields")):
            return True
        holders = extended_data.get("holders")
        if isinstance(holders, dict) and (bool(holders.get("major")) or bool(holders.get("circulating"))):
            return True
        fund_flow = extended_data.get("fund_flow")
        if isinstance(fund_flow, dict) and bool(fund_flow.get("rows")):
            return True
        summary = extended_data.get("financial_summary")
        if isinstance(summary, dict) and bool(summary.get("rows")):
            return True
        statements = extended_data.get("financial_statements")
        if isinstance(statements, dict):
            for key in ("balance_sheet", "income_statement", "cash_flow"):
                statement = statements.get(key)
                if isinstance(statement, dict) and bool(statement.get("periods")):
                    return True
        composition = extended_data.get("business_composition")
        if isinstance(composition, dict) and bool(composition.get("sections")):
            return True
        reports = extended_data.get("published_reports")
        if isinstance(reports, dict) and bool(reports.get("reports")):
            return True
        return False

    def _load_f10_extended_data(self, market: str, symbol: str) -> dict[str, dict[str, Any]]:
        cached = StockOnDemandService(self.db).load_f10_cache(market, symbol)
        extended_data = self._empty_f10_extended_data()
        for section in F10_EXTENDED_SECTIONS:
            payload = cached.get(section)
            if isinstance(payload, dict):
                extended_data[section] = payload
        return extended_data

    def _persist_f10_extended_data(
        self,
        source: DataSource,
        market: str,
        symbol: str,
        extended_data: dict[str, Any],
    ) -> None:
        service = StockOnDemandService(self.db)
        for section in F10_EXTENDED_SECTIONS:
            payload = extended_data.get(section)
            if isinstance(payload, dict):
                service.upsert_f10_cache(source, market, symbol, section, payload)

    def _mark_f10_refresh_failed(self, extended_data: dict[str, dict[str, Any]], detail: str) -> None:
        message = f"Remote refresh failed; local cache was used. {detail}"
        for section in F10_EXTENDED_SECTIONS:
            payload = extended_data.get(section)
            if isinstance(payload, dict) and not payload.get("message"):
                payload["message"] = message

    @staticmethod
    def _hk_published_reports_need_refresh(payload: dict | None) -> bool:
        if not isinstance(payload, dict):
            return True
        reports = payload.get("reports")
        if not isinstance(reports, list) or not reports:
            return True
        valid_reports = [item for item in reports if isinstance(item, dict)]
        if not valid_reports:
            return True
        latest = max(
            valid_reports,
            key=lambda item: (
                str(item.get("report_date") or ""),
                str(item.get("notice_date") or ""),
            ),
        )
        return not bool(str(latest.get("url") or "").strip())

    def _get_symbol(self, market: str, symbol: str) -> StockSymbol:
        item = self.db.scalar(
            select(StockSymbol).where(
                StockSymbol.market == market,
                StockSymbol.symbol == symbol,
            )
        )
        if not item:
            raise ValueError(f"Stock symbol not found: {market}/{symbol}")
        return item

    @staticmethod
    def _normalize_symbol(market: str, symbol: str) -> str:
        clean_symbol = symbol.strip().upper()
        if clean_symbol.isdigit():
            return clean_symbol.zfill(digit_length_for_market(market))
        return clean_symbol

    @staticmethod
    def _safe_limit(value: Any, default: int) -> int:
        try:
            return min(max(int(value or default), 1), 500)
        except (TypeError, ValueError):
            return default

    @staticmethod
    def _serialize_value(value: Any) -> Any:
        if isinstance(value, (datetime, date)):
            return value.isoformat()
        if isinstance(value, dict):
            return {str(key): StockToolsService._serialize_value(item) for key, item in value.items()}
        if isinstance(value, list):
            return [StockToolsService._serialize_value(item) for item in value]
        return value

    @classmethod
    def _serialize_model(cls, item: Any) -> dict[str, Any]:
        if item is None:
            return {}
        return {
            column.name: cls._serialize_value(getattr(item, column.name))
            for column in item.__table__.columns
        }

    def _default_source(self) -> DataSource:
        source = self.db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE"))
        if not source:
            raise ValueError("Default market data source not found: AKSHARE")
        return source
