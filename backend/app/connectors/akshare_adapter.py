from __future__ import annotations

from datetime import date, datetime
from functools import lru_cache
import html
import json
import re
import time
from typing import Any
from urllib.parse import urlencode

import akshare as ak
import httpx
import pandas as pd

from app.core.markets import MARKET_CN_A, MARKET_HK, MARKET_NEEQ, MARKET_NEEQ_INNOVATION
try:
    from curl_cffi import requests as curl_requests
except Exception:  # pragma: no cover - optional browser-fingerprint fallback
    curl_requests = None

from app.connectors.base import (
    FinancialRecord,
    KlineRecord,
    MarketDataAdapter,
    NewsRecord,
    NoticeRecord,
    QuoteRecord,
    SymbolRecord,
)


class AkshareAdapter(MarketDataAdapter):
    adapter_type = "AKSHARE"

    def health_check(self) -> tuple[str, list[str]]:
        required_methods = ("stock_info_a_code_name", "stock_hk_spot_em", "stock_news_em")
        missing = [method for method in required_methods if not hasattr(ak, method)]
        if missing:
            raise RuntimeError(f"Installed AkShare package is missing methods: {', '.join(missing)}")
        return (
            "AkShare adapter is configured. Remote provider connectivity is verified during synchronization.",
            [
                "CN_A_SYMBOLS",
                "HK_SYMBOLS",
                "NEEQ_SYMBOLS",
                "NEEQ_INNOVATION_SYMBOLS",
                "A_KLINE_ON_DEMAND",
                "HK_KLINE_ON_DEMAND",
                "NEEQ_KLINE_ON_DEMAND",
                "QUOTE_ON_DEMAND",
                "NEWS_ON_DEMAND",
                "NOTICE_ON_DEMAND",
                "FINANCIAL_ON_DEMAND",
            ],
        )

    def fetch_symbol_master(self, market: str) -> list[SymbolRecord]:
        if market == MARKET_CN_A:
            return self._fetch_cn_a_symbols()
        if market == MARKET_HK:
            return self._fetch_hk_symbols()
        if market == MARKET_NEEQ:
            return self._fetch_neeq_symbols(layer="BASE")
        if market == MARKET_NEEQ_INNOVATION:
            return self._fetch_neeq_symbols(layer="INNOVATION")
        raise ValueError(f"Unsupported market: {market}")

    def fetch_kline(
        self,
        market: str,
        symbol: str,
        period: str,
        start_date: str,
        end_date: str,
        adjust: str,
    ) -> list[KlineRecord]:
        if market == "CN_A":
            dataframe = self._fetch_cn_a_kline(symbol=symbol, period=period, start_date=start_date, end_date=end_date, adjust=adjust)
            return self._normalize_a_kline(dataframe=dataframe, symbol=symbol, period=period, adjust=adjust)
        if market == "HK":
            dataframe = self._fetch_hk_kline(symbol=symbol, period=period, start_date=start_date, end_date=end_date, adjust=adjust)
            return self._normalize_hk_kline(dataframe=dataframe, symbol=symbol, period=period, adjust=adjust)
        if market in (MARKET_NEEQ, MARKET_NEEQ_INNOVATION):
            return self._fetch_neeq_kline(
                market=market,
                symbol=symbol,
                period=period,
                start_date=start_date,
                end_date=end_date,
                adjust=adjust,
            )
        raise ValueError(f"Unsupported market: {market}")

    def _fetch_cn_a_kline(
        self,
        symbol: str,
        period: str,
        start_date: str,
        end_date: str,
        adjust: str,
    ) -> pd.DataFrame:
        candidates: list[tuple[str, dict[str, Any]]] = [
            (
                "stock_zh_a_hist",
                {
                    "symbol": symbol,
                    "period": period,
                    "start_date": start_date,
                    "end_date": end_date,
                    "adjust": adjust,
                },
            ),
            (
                "stock_zh_a_hist_tx",
                {
                    "symbol": self._a_hist_tx_symbol(symbol),
                    "start_date": start_date,
                    "end_date": end_date,
                    "adjust": adjust,
                },
            ),
            (
                "stock_zh_a_daily",
                {
                    "symbol": self._a_daily_symbol(symbol),
                    "start_date": start_date,
                    "end_date": end_date,
                    "adjust": adjust,
                },
            ),
        ]
        last_error: Exception | None = None
        for method_name, kwargs in candidates:
            method = getattr(ak, method_name, None)
            if not method:
                continue
            try:
                dataframe = method(**kwargs)
                if dataframe is not None and not dataframe.empty:
                    frame = self._normalize_a_hist_dataframe(dataframe, method_name)
                    if "日期" in frame.columns:
                        dates = pd.to_datetime(frame["日期"], errors="coerce")
                        if start_date:
                            frame = frame[dates >= pd.to_datetime(start_date)]
                            dates = pd.to_datetime(frame["日期"], errors="coerce")
                        if end_date:
                            frame = frame[dates <= pd.to_datetime(end_date)]
                    return frame
            except Exception as exc:
                last_error = exc
        if last_error:
            raise last_error
        return pd.DataFrame()

    def fetch_financial_indicators(
        self,
        market: str,
        symbol: str,
        indicator: str,
    ) -> list[FinancialRecord]:
        if market == "CN_A":
            dataframe = ak.stock_financial_analysis_indicator_em(
                symbol=f"{symbol}.SZ" if symbol.startswith(("0", "3")) else f"{symbol}.SH",
                indicator=indicator,
            )
            return self._normalize_financial_dataframe(dataframe, market, symbol, indicator)
        if market == "HK":
            dataframe = ak.stock_financial_hk_analysis_indicator_em(symbol=symbol, indicator=indicator)
            return self._normalize_financial_dataframe(dataframe, market, symbol, indicator)
        if market in (MARKET_NEEQ, MARKET_NEEQ_INNOVATION):
            return self._fetch_neeq_financial_indicators(market=market, symbol=symbol, indicator=indicator)
        raise ValueError(f"Unsupported market: {market}")

    def fetch_notices(self, market: str, symbol: str, start_date: str, end_date: str) -> list[NoticeRecord]:
        if market == "CN_A":
            try:
                records = self._fetch_cn_a_notices(symbol=symbol, start_date=start_date, end_date=end_date)
                if records:
                    return records
            except Exception:
                pass
            return []
        if market == "HK":
            return self._fetch_hkex_notices(symbol=symbol, start_date=start_date, end_date=end_date)
        if market in (MARKET_NEEQ, MARKET_NEEQ_INNOVATION):
            return self._fetch_neeq_notices(market=market, symbol=symbol, start_date=start_date, end_date=end_date)
        raise ValueError(f"Unsupported market: {market}")

    def fetch_realtime_quote(self, market: str, symbol: str) -> QuoteRecord:
        if market in (MARKET_NEEQ, MARKET_NEEQ_INNOVATION):
            return self._fetch_neeq_quote(market, symbol)
        if market not in {"CN_A", "HK"}:
            raise ValueError(f"Unsupported market: {market}")
        providers = (self._fetch_realtime_quote_eastmoney, self._fetch_realtime_quote_tencent)
        last_error: Exception | None = None
        for provider in providers:
            try:
                return provider(market, symbol)
            except Exception as exc:
                last_error = exc
        if last_error:
            raise last_error
        raise ValueError(f"Unsupported market: {market}")

    def fetch_news(self, market: str, symbol: str) -> list[NewsRecord]:
        if market in (MARKET_NEEQ, MARKET_NEEQ_INNOVATION):
            return self._fetch_neeq_news(market=market, symbol=symbol)
        if market == MARKET_HK:
            return self._fetch_eastmoney_search_news(market, symbol)
        dataframe = ak.stock_news_em(symbol=symbol)
        return self._normalize_news_dataframe(dataframe, market, symbol)

    def _fetch_eastmoney_search_news(self, market: str, symbol: str) -> list[NewsRecord]:
        """Call Eastmoney search directly; avoids the upstream AkShare regex bug."""
        callback = "quantNewsCallback"
        inner = {"uid": "", "keyword": symbol, "type": ["cmsArticleWebOld"],
                 "client": "web", "clientType": "web", "clientVersion": "curr",
                 "param": {"cmsArticleWebOld": {"searchScope": "default", "sort": "default",
                     "pageIndex": 1, "pageSize": 30, "preTag": "<em>", "postTag": "</em>"}}}
        with httpx.Client(timeout=20.0, trust_env=False, headers={"User-Agent": "Mozilla/5.0",
                "Referer": f"https://so.eastmoney.com/news/s?keyword={symbol}"}) as client:
            response = client.get("https://search-api-web.eastmoney.com/search/jsonp",
                params={"cb": callback, "param": json.dumps(inner, ensure_ascii=False), "_": str(int(time.time() * 1000))})
            response.raise_for_status()
        match = re.match(r"^[^(]+\((.*)\)\s*;?$", response.text, re.S)
        if not match:
            raise ValueError("Eastmoney news response is not valid JSONP")
        rows = (json.loads(match.group(1)).get("result") or {}).get("cmsArticleWebOld") or []
        records: list[NewsRecord] = []
        for item in rows:
            title = re.sub(r"</?em>", "", html.unescape(str(item.get("title") or ""))).strip()
            content = re.sub(r"</?em>", "", html.unescape(str(item.get("content") or ""))).replace("\u3000", " ").strip()
            news_time = str(item.get("date") or "").strip()
            if title and news_time:
                records.append(NewsRecord(market=market, symbol=symbol, news_time=news_time, title=title,
                    content=content or None, source_name=str(item.get("mediaName") or "东方财富新闻搜索"),
                    url=str(item.get("url") or "") or None, content_json=self._json_safe(item)))
        return records

    def fetch_extended_data(self, market: str, symbol: str) -> dict[str, Any]:
        """Build the optional sections used by the stock-detail F10 drawer.

        These sections are intentionally fetched on demand. The stock master and
        price/K-line tables remain the source of truth for the core detail view,
        while this payload can degrade section-by-section when an upstream
        provider is unavailable.
        """
        if market == "CN_A":
            return self._fetch_cn_a_extended_data(symbol)
        if market == "HK":
            return self._fetch_hk_extended_data(symbol)
        if market in (MARKET_NEEQ, MARKET_NEEQ_INNOVATION):
            return self._fetch_neeq_extended_data(market=market, symbol=symbol)
        return {}

    def _fetch_cn_a_extended_data(self, symbol: str) -> dict[str, Any]:
        profile = self._safe_dataframe_first_row(
            getattr(ak, "stock_profile_cninfo", None),
            {"symbol": symbol},
            source="CNINFO",
        )
        major_holders = self._safe_holder_rows(
            getattr(ak, "stock_main_stock_holder", None),
            {"stock": symbol},
            date_columns=("截至日期", "截止日期"),
            source="新浪财经",
        )
        circulating_holders = self._safe_holder_rows(
            getattr(ak, "stock_circulate_stock_holder", None),
            {"symbol": symbol},
            date_columns=("截止日期", "截至日期"),
            source="新浪财经",
        )
        financial_summary = self._safe_cn_financial_summary(symbol)
        financial_statements = self._safe_cn_financial_statements(symbol)
        fund_flow = self._safe_cn_fund_flow(symbol)
        business_composition = self._safe_cn_business_composition(symbol)
        published_reports = self._safe_cn_financial_reports(symbol)
        return {
            "profile": profile,
            "holders": {
                "source": "新浪财经",
                "major": major_holders,
                "circulating": circulating_holders,
            },
            "fund_flow": fund_flow,
            "financial_summary": financial_summary,
            "financial_statements": financial_statements,
            "business_composition": business_composition,
            "published_reports": published_reports,
        }

    def _fetch_hk_extended_data(self, symbol: str) -> dict[str, Any]:
        company = self._safe_dataframe_first_row(
            getattr(ak, "stock_hk_company_profile_em", None),
            {"symbol": symbol},
            source="东方财富港股公司资料",
        )
        security = self._safe_dataframe_first_row(
            getattr(ak, "stock_hk_security_profile_em", None),
            {"symbol": symbol},
            source="东方财富港股证券资料",
        )
        indicator = self._safe_dataframe_first_row(
            getattr(ak, "stock_hk_financial_indicator_em", None),
            {"symbol": symbol},
            source="东方财富港股财务指标",
        )
        financial_summary = self._safe_hk_financial_summary(symbol)
        financial_statements = self._safe_hk_financial_statements(symbol)
        published_reports = self._safe_hk_financial_reports(symbol)
        business_composition = self._safe_hk_business_composition(symbol)
        profile_fields = self._normalize_hk_profile_fields(
            {
                **(company.get("fields") or {}),
                **(security.get("fields") or {}),
                **(indicator.get("fields") or {}),
            }
        )
        profile_sources = [item for item in (company.get("source"), security.get("source"), indicator.get("source")) if item]
        return {
            "profile": {
                "source": " / ".join(profile_sources) or "暂无",
                "fields": profile_fields,
            },
            "holders": {
                "source": "港交所 DI / CCASS 官方查询入口",
                "major": [],
                "circulating": [],
                "official_links": [
                    {
                        "name": "权益披露 DI",
                        "url": f"https://di.hkex.com.hk/di/NSSrchCorp.aspx?src=MAIN&lang=ZH&txtStockCode={symbol}",
                    },
                    {
                        "name": "CCASS 持股查询",
                        "url": f"https://www3.hkexnews.hk/sdw/search/searchsdw.aspx?stockcode={symbol}",
                    },
                ],
                "message": "港交所 DI/CCASS 是免费官方源，但公开页面限制程序化抓取；当前提供官方入口，后续如需自动化明细建议接授权数据源。",
            },
            "fund_flow": {
                **self._safe_hk_fund_flow(symbol),
                "mode": "HK_CONNECT_HOLDINGS",
            },
            "financial_summary": financial_summary,
            "financial_statements": financial_statements,
            "business_composition": business_composition,
            "published_reports": published_reports,
        }

    def _fetch_neeq_json(self, path: str, params: dict[str, Any] | None = None) -> Any:
        """Fetch one response from the Eastmoney New Third Board site.

        The xinsanban endpoints are separate from the AkShare A/HK endpoints.
        Keep this small client isolated so a provider change does not affect the
        rest of the adapter.
        """
        url = f"https://xinsanban.eastmoney.com{path}"
        headers = {
            "User-Agent": "Mozilla/5.0",
            "Accept": "application/json,text/plain,*/*",
            "Referer": "https://xinsanban.eastmoney.com/",
        }
        with httpx.Client(
            trust_env=False,
            timeout=20.0,
            headers=headers,
            follow_redirects=True,
        ) as client:
            response = client.get(url, params=params or {})
            response.raise_for_status()
            return response.json()

    @staticmethod
    def _neeq_result(payload: Any) -> list[dict[str, Any]]:
        if isinstance(payload, dict):
            result = payload.get("result")
            if isinstance(result, list):
                return [item for item in result if isinstance(item, dict)]
        return []

    @classmethod
    def _neeq_number(cls, value: Any) -> float | None:
        if value in (None, "", "-"):
            return None
        return cls._to_float(value)

    @staticmethod
    def _normalize_neeq_date(value: Any) -> str | None:
        if value in (None, ""):
            return None
        text = str(value).strip()
        if not text:
            return None
        for candidate in (text, text.replace("/", "-")):
            parsed = pd.to_datetime(candidate, errors="coerce")
            if not pd.isna(parsed):
                return parsed.date().isoformat()
        compact = re.sub(r"[^0-9]", "", text)
        if len(compact) >= 8:
            try:
                return datetime.strptime(compact[:8], "%Y%m%d").date().isoformat()
            except ValueError:
                pass
        return text.split(" ", 1)[0]

    @classmethod
    def _neeq_date_param(cls, value: str | None, default: str) -> str:
        normalized = cls._normalize_neeq_date(value)
        return normalized or default

    def _fetch_neeq_quote(self, market: str, symbol: str) -> QuoteRecord:
        try:
            quote_payload = self._fetch_neeq_json(
                "/api/QuoteCenter/stock/getstockquote",
                {"code": symbol},
            )
        except Exception:
            quote_payload = self._fetch_neeq_quote_push2(symbol)
        quote_rows = self._neeq_result(quote_payload)
        if not quote_rows:
            if isinstance(quote_payload, dict) and isinstance(quote_payload.get("data"), dict):
                quote_rows = [self._push2_quote_to_neeq_row(quote_payload["data"])]
            else:
                raise ValueError(f"No NEEQ quote returned for {symbol}")
        if not quote_rows:
            raise ValueError(f"No NEEQ quote returned for {symbol}")
        quote = quote_rows[0]

        company_payload: dict[str, Any] = {}
        try:
            company_payload = self._fetch_neeq_json(
                "/api/QuoteCenter/stock/GetZyzb",
                {"code": symbol},
            ) or {}
        except Exception:
            company_payload = {}
        company_rows = self._neeq_result(company_payload)
        company = company_rows[0] if company_rows else {}

        previous_close = self._neeq_number(quote.get("PreviousClose"))
        current_price = self._neeq_number(quote.get("Close"))
        if current_price is None:
            # NEEQ stocks can have no transaction on the current day. In that
            # case Eastmoney returns "-" for Close and the last close is the
            # most useful current snapshot.
            current_price = previous_close
        change_amount = self._neeq_number(quote.get("Change"))
        change_pct = self._neeq_number(quote.get("ChangePercent"))
        if change_pct is None:
            change_pct = self._neeq_number(company.get("CHANGEPCT1DAY"))
        quote_time = self._normalize_neeq_date(company.get("LATESTTRADEDAY"))
        if not quote_time:
            timestamp = self._to_float(quote.get("_push2_timestamp"))
            if timestamp:
                try:
                    quote_time = datetime.fromtimestamp(timestamp).date().isoformat()
                except (OverflowError, OSError, ValueError):
                    quote_time = None
        raw_payload = self._json_safe(
            {
                "quote": quote,
                "company_indicators": company,
                "latest_price_source": "Close" if self._neeq_number(quote.get("Close")) is not None else "PreviousClose",
                "no_trade_today": self._neeq_number(quote.get("Close")) is None,
                "total_market_cap": self._neeq_number(quote.get("MarketValue")),
                "float_market_cap": self._neeq_number(quote.get("FlowCapitalValue")),
                "pe_ratio": self._neeq_number(quote.get("PERation")),
                "pb_ratio": self._neeq_number(quote.get("PB")),
            }
        )
        return QuoteRecord(
            market=market,
            symbol=symbol,
            quote_time=quote_time,
            current_price=current_price,
            previous_close_price=previous_close,
            open_price=self._neeq_number(quote.get("Open")),
            high_price=self._neeq_number(quote.get("High")),
            low_price=self._neeq_number(quote.get("Low")),
            volume=self._neeq_number(quote.get("Volume")),
            amount=self._neeq_number(quote.get("Amount")),
            change_amount=change_amount,
            change_pct=change_pct,
            turnover_rate=self._neeq_number(quote.get("TurnoverRate")),
            raw_payload=raw_payload,
        )

    def _fetch_neeq_quote_push2(self, symbol: str) -> dict[str, Any]:
        params = {
            "secid": f"0.{symbol}",
            "fields": "f43,f44,f45,f46,f47,f48,f57,f58,f60,f86,f116,f117,f162,f167,f168,f169,f170,f171,f292",
            "cb": "jQuery123",
        }
        last_error: Exception | None = None
        for host in ("push2.eastmoney.com", "push2his.eastmoney.com", "82.push2his.eastmoney.com"):
            try:
                payload = self._request_eastmoney_json(
                    f"https://{host}/api/qt/stock/get",
                    params,
                    {
                        "User-Agent": "Mozilla/5.0",
                        "Accept": "application/json,text/plain,*/*",
                        "Referer": "https://xinsanban.eastmoney.com/",
                    },
                )
                if isinstance(payload, dict) and isinstance(payload.get("data"), dict):
                    return payload
            except Exception as exc:
                last_error = exc
        if last_error:
            raise last_error
        raise ValueError(f"No fallback NEEQ quote returned for {symbol}")

    @classmethod
    def _push2_quote_to_neeq_row(cls, data: dict[str, Any]) -> dict[str, Any]:
        def price(field: str, zero_is_missing: bool = False) -> float | str:
            value = data.get(field)
            if value in (None, "", "-"):
                return "-"
            numeric = cls._to_float(value)
            if zero_is_missing and numeric == 0:
                return "-"
            return numeric / 100 if numeric is not None else "-"

        f43 = cls._to_float(data.get("f43"))
        return {
            "Code": data.get("f57"),
            "Name": data.get("f58"),
            "Close": price("f43") if f43 not in (None, 0) else "-",
            "PreviousClose": price("f60"),
            "Open": price("f46", zero_is_missing=True),
            "High": price("f44", zero_is_missing=True),
            "Low": price("f45", zero_is_missing=True),
            "Volume": data.get("f47"),
            "Amount": data.get("f48"),
            "Change": price("f169"),
            "ChangePercent": (
                cls._to_float(data.get("f170")) / 100
                if cls._to_float(data.get("f170")) is not None
                else "-"
            ),
            "TurnoverRate": (
                cls._to_float(data.get("f168")) / 100
                if cls._to_float(data.get("f168")) is not None
                else "-"
            ),
            "MarketValue": data.get("f116"),
            "FlowCapitalValue": data.get("f117"),
            "PERation": (
                cls._to_float(data.get("f162")) / 100
                if cls._to_float(data.get("f162")) is not None
                else "-"
            ),
            "PB": (
                cls._to_float(data.get("f167")) / 100
                if cls._to_float(data.get("f167")) is not None
                else "-"
            ),
            "_push2_timestamp": data.get("f86"),
        }

    def _fetch_neeq_kline(
        self,
        market: str,
        symbol: str,
        period: str,
        start_date: str,
        end_date: str,
        adjust: str,
    ) -> list[KlineRecord]:
        period_code = {
            "daily": "101",
            "day": "101",
            "weekly": "102",
            "week": "102",
            "monthly": "103",
            "month": "103",
        }.get(str(period or "").lower(), "101")
        begin = re.sub(r"[^0-9]", "", str(start_date or "")) or "19900101"
        finish = re.sub(r"[^0-9]", "", str(end_date or "")) or "20991231"
        fqt = {"qfq": "1", "hfq": "2"}.get(str(adjust or "").lower(), "0")
        params = {
            "secid": f"0.{symbol}",
            "klt": period_code,
            "beg": begin,
            "end": finish,
            "fqt": fqt,
            "fields1": "f1,f2,f3,f4,f5,f6",
            "fields2": "f51,f52,f53,f54,f55,f56,f57,f58,f59,f60",
            "ut": "fa5fd1943c7b386f172d6893dbfba10b",
            "cb": "jQuery123",
        }
        payload: dict[str, Any] | None = None
        last_error: Exception | None = None
        for secid in (f"0.{symbol}", f"1.{symbol}", f"2.{symbol}"):
            params["secid"] = secid
            for host in ("push2his.eastmoney.com", "82.push2his.eastmoney.com", "push2.eastmoney.com"):
                try:
                    candidate = self._request_eastmoney_json(
                        f"https://{host}/api/qt/stock/kline/get", params,
                        {"User-Agent": "Mozilla/5.0", "Accept": "application/json,text/plain,*/*",
                         "Referer": "https://xinsanban.eastmoney.com/"})
                    data = (candidate or {}).get("data") or {}
                    if isinstance(data.get("klines"), list) and data.get("klines"):
                        payload = candidate
                        break
                except Exception as exc:
                    last_error = exc
            if payload is not None:
                break
        if payload is None:
            if last_error:
                raise last_error
            return []

        lines = ((payload.get("data") or {}).get("klines") or []) if isinstance(payload, dict) else []
        records: list[KlineRecord] = []
        for line in lines:
            values = str(line).split(",")
            if len(values) < 7:
                continue
            trade_date = str(values[0]).split(" ", 1)[0].strip()
            if not trade_date:
                continue
            records.append(
                KlineRecord(
                    market=market,
                    symbol=symbol,
                    period=period,
                    adjust=adjust,
                    trade_date=trade_date,
                    open_price=self._to_float(values[1]),
                    close_price=self._to_float(values[2]),
                    high_price=self._to_float(values[3]),
                    low_price=self._to_float(values[4]),
                    volume=self._to_float(values[5]),
                    amount=self._to_float(values[6]),
                    # NEEQ's kline payload uses field 8 for daily change pct,
                    # not turnover rate. Keep it in raw_payload rather than
                    # exposing it under the wrong column.
                    turnover_rate=None,
                    raw_payload=self._json_safe({"line": line, "fields": values}),
                )
            )
        return records

    def _fetch_neeq_financial_indicators(
        self,
        market: str,
        symbol: str,
        indicator: str,
    ) -> list[FinancialRecord]:
        try:
            payload = self._fetch_neeq_json(
                "/api/QuoteCenter/stock/GetCwzb",
                {"code": symbol},
            )
        except Exception:
            # The xinsanban F10 host is occasionally rate-limited. Keep a
            # useful quote-indicator snapshot available through the public
            # push2 endpoint instead of failing the entire detail refresh.
            push_payload = self._fetch_neeq_quote_push2(symbol)
            push_data = push_payload.get("data") if isinstance(push_payload, dict) else {}
            if not isinstance(push_data, dict):
                return []
            report_period = None
            timestamp = self._to_float(push_data.get("f86"))
            if timestamp:
                try:
                    report_period = datetime.fromtimestamp(timestamp).date().isoformat()
                except (OverflowError, OSError, ValueError):
                    report_period = None
            row = {
                "SECURITY_CODE": symbol,
                "REPORTDATE": report_period or datetime.now().date().isoformat(),
                "TOTALMVALUE": push_data.get("f116"),
                "CMVALUE": push_data.get("f117"),
                "PELYR": (
                    self._to_float(push_data.get("f162")) / 100
                    if self._to_float(push_data.get("f162")) is not None
                    else None
                ),
                "PBMRQ": (
                    self._to_float(push_data.get("f167")) / 100
                    if self._to_float(push_data.get("f167")) is not None
                    else None
                ),
                "source": "Eastmoney push2 quote indicators",
            }
            return [
                FinancialRecord(
                    market=market,
                    symbol=symbol,
                    indicator=indicator or "NEEQ_FINANCIAL_INDICATORS",
                    report_period=row["REPORTDATE"],
                    currency=None,
                    data_json=self._json_safe(row),
                )
            ]
        rows = self._neeq_result(payload)
        records: list[FinancialRecord] = []
        for row in rows:
            report_period = self._normalize_neeq_date(row.get("REPORTDATE")) or str(
                row.get("REPORTDATE") or ""
            )
            if not report_period:
                continue
            records.append(
                FinancialRecord(
                    market=market,
                    symbol=symbol,
                    indicator=indicator or "NEEQ_FINANCIAL_INDICATORS",
                    report_period=report_period,
                    currency=None,
                    data_json=self._json_safe(row),
                )
            )
        return records

    def _fetch_neeq_notices(
        self,
        market: str,
        symbol: str,
        start_date: str,
        end_date: str,
    ) -> list[NoticeRecord]:
        begin = self._neeq_date_param(start_date, "2000-01-01")
        end = self._neeq_date_param(end_date, "2099-12-31")
        records: list[NoticeRecord] = []
        seen: set[tuple[str, str]] = set()
        last_error: Exception | None = None
        # Eastmoney returns 20 rows per page.  The previous ten-page cap
        # silently dropped older annual/semi-annual reports (the endpoint can
        # return several hundred announcements for one NEEQ issuer).
        for notice_type in (0, 1):
            for page in range(1, 101):
                try:
                    payload = self._fetch_neeq_json(
                        "/api/gg/list",
                        {
                            "page_index": page,
                            "type": notice_type,
                            "begin": begin,
                            "end": end,
                            "securitycodes": symbol,
                            "content": "",
                            "sortRule": 1,
                        },
                    )
                except Exception as exc:
                    last_error = exc
                    break
                rows = payload.get("result") if isinstance(payload, dict) else []
                if not isinstance(rows, list):
                    break
                for row in rows:
                    if not isinstance(row, dict):
                        continue
                    notice_date = self._normalize_neeq_date(
                        row.get("notice_date") or row.get("display_time") or row.get("sort_date")
                    )
                    title = str(row.get("title_ch") or row.get("title") or "").strip()
                    if not notice_date or not title:
                        continue
                    if notice_date < begin or notice_date > end:
                        continue
                    key = (notice_date, title)
                    if key in seen:
                        continue
                    seen.add(key)
                    columns = row.get("columns")
                    notice_type_name = ""
                    if isinstance(columns, list) and columns and isinstance(columns[0], dict):
                        notice_type_name = str(columns[0].get("column_name") or "").strip()
                    records.append(
                        NoticeRecord(
                            market=market,
                            symbol=symbol,
                            notice_date=notice_date,
                            title=title,
                            notice_type=notice_type_name or ("financial_report" if notice_type == 1 else "announcement"),
                            url=f"https://xinsanban.eastmoney.com/Article/NoticeContent?id={row.get('art_code')}",
                            content_json=self._json_safe(row),
                        )
                    )
                total = self._safe_int(payload.get("total")) if isinstance(payload, dict) else 0
                page_size = len(rows)
                if not rows or (total and page * page_size >= total) or page_size < 20:
                    break
        if not records and last_error:
            raise last_error
        records.sort(key=lambda item: (item.notice_date, item.title), reverse=True)
        return records

    def _fetch_neeq_news(self, market: str, symbol: str) -> list[NewsRecord]:
        payload = self._fetch_neeq_json(
            "/api/QuoteCenter/stock/GetGsxw",
            {"Code": symbol},
        )
        rows = self._neeq_result(payload)
        records: list[NewsRecord] = []
        seen: set[tuple[str, str]] = set()
        for row in rows:
            news_time = str(row.get("Art_ShowTime") or "").strip()
            title = str(row.get("Art_Title") or "").strip()
            if not news_time or not title:
                continue
            key = (news_time, title)
            if key in seen:
                continue
            seen.add(key)
            records.append(
                NewsRecord(
                    market=market,
                    symbol=symbol,
                    news_time=news_time,
                    title=title,
                    content=None,
                    source_name="Eastmoney NEEQ",
                    url=str(row.get("Art_Url") or row.get("Art_OriginUrl") or "").strip() or None,
                    content_json=self._json_safe(row),
                )
            )
        records.sort(key=lambda item: item.news_time, reverse=True)
        return records

    @staticmethod
    def _is_neeq_report_notice(notice: NoticeRecord) -> bool:
        text = f"{notice.title} {notice.notice_type or ''}".replace(" ", "")
        # Inquiry letters, replies, corrections and cancellation notices are
        # announcements about a report, not the report itself.
        excluded = (
            "问询函",
            "问询回复",
            "回复公告",
            "回复函",
            "更正公告",
            "更正后",
            "取消公告",
            "延期披露",
            "摘要",
        )
        if any(keyword in text for keyword in excluded):
            return False
        return any(
            keyword in text
            for keyword in (
                "年报",
                "年度报告",
                "半年报",
                "半年度报告",
                "中报",
                "中期报告",
                "季度报告",
                "一季报",
                "三季报",
                "财务报告",
                "财务报表",
                "审计报告",
                "年度业绩",
                "中期业绩",
            )
        )

    def _fetch_neeq_extended_data(self, market: str, symbol: str) -> dict[str, Any]:
        """Build F10 sections from the Eastmoney New Third Board endpoints."""
        quote: QuoteRecord | None = None
        financial_records: list[FinancialRecord] = []
        company: dict[str, Any] = {}
        labels: Any = None
        notices: list[NoticeRecord] = []
        errors: list[str] = []
        try:
            quote = self._fetch_neeq_quote(market, symbol)
        except Exception as exc:
            errors.append(f"quote: {str(exc)[:160]}")
        try:
            financial_records = self._fetch_neeq_financial_indicators(
                market,
                symbol,
                "NEEQ_FINANCIAL_INDICATORS",
            )
        except Exception as exc:
            errors.append(f"financial: {str(exc)[:160]}")
        try:
            company_payload = self._fetch_neeq_json(
                "/api/QuoteCenter/stock/GetZyzb",
                {"code": symbol},
            )
            company_rows = self._neeq_result(company_payload)
            company = company_rows[0] if company_rows else {}
        except Exception as exc:
            errors.append(f"profile: {str(exc)[:160]}")
        try:
            labels = self._fetch_neeq_json(
                "/api/QuoteCenter/stock/GetStockLabel",
                {"code": symbol},
            )
        except Exception as exc:
            errors.append(f"labels: {str(exc)[:160]}")
        try:
            notices = self._fetch_neeq_notices(
                market,
                symbol,
                "20000101",
                datetime.now().strftime("%Y%m%d"),
            )
        except Exception as exc:
            errors.append(f"notices: {str(exc)[:160]}")

        quote_raw = quote.raw_payload if quote else {}
        quote_data = quote_raw.get("quote") if isinstance(quote_raw, dict) else {}
        if not isinstance(quote_data, dict):
            quote_data = {}
        profile_fields: dict[str, Any] = {}
        profile_values = {
            "level": company.get("LEVEL"),
            "trading_method": company.get("TRANSMETHOD"),
            "shareholder_count": self._neeq_number(company.get("TOTALSH")),
            "shareholder_change": self._neeq_number(company.get("SHCHANGE")),
            "shareholder_change_pct": self._neeq_number(company.get("SHCHANGEPCT")),
            "trading_days": self._neeq_number(company.get("TRADEDATES")),
            "listing_date": self._normalize_neeq_date(company.get("LISTINGDATE")),
            "first_trade_date": self._normalize_neeq_date(company.get("FIRSTTRADEDAY")),
            "latest_trade_date": self._normalize_neeq_date(company.get("LATESTTRADEDAY")),
            "latest_report_date": self._normalize_neeq_date(company.get("REPORTDATE")),
            "sponsor_broker": company.get("BROKER"),
            "market_maker_count": self._neeq_number(company.get("MAKERCOUNT")),
            "total_financing": self._neeq_number(company.get("SUMFINA")),
            "financing_count": self._neeq_number(company.get("COUNTFINA")),
            "labels": labels if isinstance(labels, str) else None,
            "current_price": quote.current_price if quote else None,
            "previous_close": quote.previous_close_price if quote else None,
            "market_value": self._neeq_number(quote_data.get("MarketValue")),
            "float_market_value": self._neeq_number(quote_data.get("FlowCapitalValue")),
            "pe_ratio": self._neeq_number(quote_data.get("PERation")),
            "pb_ratio": self._neeq_number(quote_data.get("PB")),
        }
        for key, value in profile_values.items():
            if value not in (None, ""):
                profile_fields[key] = self._json_safe(value)

        latest_financial = financial_records[0].data_json if financial_records else {}
        if not isinstance(latest_financial, dict):
            latest_financial = {}
        report_period = financial_records[0].report_period if financial_records else None
        financial_metrics = (
            ("revenue", "INCOME"),
            ("net_profit", "PROFIT"),
            ("eps", "EPSJB"),
            ("book_value_per_share", "BPS"),
            ("gross_margin_pct", "XSMLL"),
            ("net_margin_pct", "XSJLL"),
            ("debt_asset_ratio_pct", "ZCFZL"),
            ("roe_pct", "ROEKCJQ"),
            ("revenue_yoy_pct", "YSTZ"),
            ("profit_yoy_pct", "SJLTZ"),
            ("pe_ratio", "PELYR"),
            ("pb_ratio", "PBMRQ"),
        )
        summary_rows: list[dict[str, Any]] = []
        for metric, field in financial_metrics:
            value = self._neeq_number(latest_financial.get(field))
            if value is not None and report_period:
                summary_rows.append({"metric": metric, "data": {report_period: value}})
        financial_summary = {
            "source": "Eastmoney NEEQ financial indicators",
            "periods": [report_period] if report_period else [],
            "rows": summary_rows,
            "latest": {
                "report_date": report_period,
                "report_name": "NEEQ financial indicators",
            },
            "message": "" if summary_rows else "No NEEQ financial indicator data returned.",
        }

        report_entries: list[dict[str, Any]] = []
        for notice in notices:
            if not self._is_neeq_report_notice(notice):
                continue
            report_type = "financial_report"
            title = notice.title
            if "半年报" in title or "半年度报告" in title or "中报" in title or "中期报告" in title:
                report_type = "semiannual_report"
            elif "季度报告" in title or "一季报" in title or "三季报" in title:
                report_type = "quarterly_report"
            elif "年报" in title or "年度报告" in title:
                report_type = "annual_report"
            report_entries.append(
                {
                    "report_name": title,
                    "report_type": report_type,
                    "report_date": notice.notice_date,
                    "notice_date": notice.notice_date,
                    "url": notice.url,
                    "source_name": "Eastmoney NEEQ announcements",
                    "title": title,
                }
            )
        report_entries.sort(
            key=lambda row: (str(row.get("report_date") or ""), str(row.get("notice_date") or "")),
            reverse=True,
        )

        message_suffix = f" Remote warnings: {'; '.join(errors)}" if errors else ""
        return {
            "profile": {
                "source": "Eastmoney NEEQ",
                "fields": profile_fields,
                "message": message_suffix.strip(),
            },
            "holders": {
                "source": "Eastmoney NEEQ company indicators",
                "major": [
                    {
                        key: value
                        for key, value in {
                            "shareholder_count": self._neeq_number(company.get("TOTALSH")),
                            "shareholder_change": self._neeq_number(company.get("SHCHANGE")),
                            "shareholder_change_pct": self._neeq_number(company.get("SHCHANGEPCT")),
                            "latest_report_date": self._normalize_neeq_date(company.get("REPORTDATE")),
                        }.items()
                        if value not in (None, "")
                    }
                ],
                "circulating": [],
                "message": "NEEQ publishes aggregate shareholder indicators rather than a complete shareholder list.",
            },
            "fund_flow": {
                "source": "Eastmoney NEEQ",
                "rows": [],
                "message": "NEEQ individual fund-flow data is not exposed by the current public endpoint.",
            },
            "financial_summary": financial_summary,
            "financial_statements": {
                "source": "Eastmoney NEEQ",
                "balance_sheet": {"label": "Balance sheet", "periods": []},
                "income_statement": {"label": "Income statement", "periods": []},
                "cash_flow": {"label": "Cash flow statement", "periods": []},
                "message": "The public NEEQ endpoint currently exposes summary indicators; standard three-statement detail is unavailable.",
            },
            "business_composition": {
                "source": "Eastmoney NEEQ",
                "report_date": report_period,
                "sections": [],
                "message": "NEEQ business-composition detail is not exposed by the current public endpoint.",
            },
            "published_reports": {
                "source": "Eastmoney NEEQ announcements",
                "reports": report_entries,
                "message": "" if report_entries else "No NEEQ financial reports returned.",
            },
        }

    def _safe_cn_financial_summary(self, symbol: str) -> dict[str, Any]:
        method = getattr(ak, "stock_financial_analysis_indicator_em", None)
        if method:
            try:
                exchange_symbol = f"{symbol}.SZ" if symbol.startswith(("0", "3")) else f"{symbol}.SH"
                dataframe = method(symbol=exchange_symbol, indicator="按报告期")
                summary = self._build_financial_summary(
                    dataframe,
                    source="东方财富财务分析指标",
                    metric_map=(
                        ("EPSJB", "基本每股收益"),
                        ("EPSKCJB", "扣非每股收益"),
                        ("BPS", "每股净资产"),
                        ("MGZBGJ", "每股公积金"),
                        ("TOTALOPERATEREVE", "营业总收入"),
                        ("PARENTNETPROFIT", "归母净利润"),
                        ("ROE_DILUTED", "摊薄净资产收益率"),
                        ("ROEJQ", "净资产收益率"),
                        ("ROEKCJQ", "扣非净资产收益率"),
                        ("NET_PROFIT_RATIO", "销售净利率"),
                        ("GROSS_PROFIT_RATIO", "销售毛利率"),
                    ),
                )
                if summary["rows"]:
                    return summary
            except Exception:
                pass

        method = getattr(ak, "stock_financial_abstract", None)
        if not method:
            return {"source": "新浪财经", "rows": [], "message": "财务摘要接口不可用。"}
        try:
            dataframe = method(symbol=symbol)
            if dataframe is None or dataframe.empty:
                return {"source": "新浪财经", "rows": [], "message": "暂无财务摘要。"}
            period_columns = [
                str(column)
                for column in dataframe.columns
                if re.fullmatch(r"\d{8}", str(column))
            ][:8]
            rows: list[dict[str, Any]] = []
            for item in dataframe.to_dict(orient="records"):
                metric = str(item.get("指标") or "").strip()
                if not metric:
                    continue
                rows.append(
                    {
                        "指标": metric,
                        "数据": {period: self._json_safe(item.get(period)) for period in period_columns},
                    }
                )
            return {
                "source": "新浪财经关键指标",
                "periods": period_columns,
                "rows": rows[:40],
            }
        except Exception as exc:
            return {"source": "新浪财经关键指标", "rows": [], "message": str(exc)[:240]}

    def _safe_cn_financial_reports(self, symbol: str) -> dict[str, Any]:
        category_map = {
            "年报": "category_ndbg_szsh;",
            "半年报": "category_bndbg_szsh;",
            "一季报": "category_yjdbg_szsh;",
            "三季报": "category_sjdbg_szsh;",
        }
        org_id = self._fallback_cninfo_org_id(symbol) or ""
        column, plate = self._cninfo_market_params(symbol)
        stock_item = f"{symbol},{org_id}" if org_id else symbol
        page_size = 50
        end_date = datetime.now().strftime("%Y-%m-%d")
        headers = {"User-Agent": "Mozilla/5.0", "Referer": "https://www.cninfo.com.cn/"}
        url = "https://www.cninfo.com.cn/new/hisAnnouncement/query"
        try:
            reports_by_period: dict[tuple[str, str], dict[str, Any]] = {}
            with httpx.Client(trust_env=False, timeout=12.0, headers=headers) as client:
                for category, category_code in category_map.items():
                    base_payload = {
                        "pageNum": "1",
                        "pageSize": str(page_size),
                        "column": column,
                        "tabName": "fulltext",
                        "plate": plate,
                        "stock": stock_item,
                        "searchkey": "",
                        "secid": "",
                        "category": category_code,
                        "trade": "",
                        "seDate": f"2000-01-01~{end_date}",
                        "sortName": "",
                        "sortType": "",
                        "isHLtitle": "true",
                    }
                    response = client.post(url, data=base_payload)
                    response.raise_for_status()
                    payload = response.json() or {}
                    total_records = int(payload.get("totalAnnouncement") or payload.get("totalRecordNum") or 0)
                    total_pages = min(max(1, (total_records + page_size - 1) // page_size), 50)
                    rows = list(payload.get("announcements") or [])
                    for page in range(2, total_pages + 1):
                        page_payload = {**base_payload, "pageNum": str(page)}
                        page_response = client.post(url, data=page_payload)
                        page_response.raise_for_status()
                        rows.extend((page_response.json() or {}).get("announcements") or [])

                    for item in rows:
                        title = self._clean_cninfo_title(
                            item.get("announcementTitle") or item.get("shortTitle") or item.get("title") or ""
                        )
                        if not title or self._should_skip_financial_report(title):
                            continue
                        notice_date = self._parse_cninfo_notice_date(item.get("announcementTime"))
                        report_type = self._guess_report_type(title, category)
                        year_match = re.search(r"(20\d{2})年", title)
                        year = year_match.group(1) if year_match else (notice_date[:4] if notice_date else "")
                        if not year:
                            continue
                        report_date = self._report_period_end_date(year, report_type) or notice_date
                        row_org_id = str(item.get("orgId") or org_id or "").strip()
                        announcement_id = str(item.get("announcementId") or "").strip()
                        adjunct_url = str(item.get("adjunctUrl") or "").strip()
                        detail_url = self._build_cninfo_announcement_url(
                            symbol=symbol,
                            announcement_id=announcement_id,
                            org_id=row_org_id,
                            notice_date=notice_date,
                            adjunct_url=adjunct_url,
                        )
                        report = {
                            "report_name": title,
                            "report_type": report_type,
                            "report_date": report_date,
                            "notice_date": notice_date,
                            "url": detail_url,
                            "source_name": "巨潮资讯",
                            "title": title,
                        }
                        key = (report_date or year, report_type)
                        current = reports_by_period.get(key)
                        if current is None or self._report_title_priority(title) < self._report_title_priority(
                            str(current.get("report_name") or "")
                        ):
                            reports_by_period[key] = report
            reports = list(reports_by_period.values())
            reports.sort(key=lambda row: (row.get("report_date") or "", row.get("notice_date") or ""), reverse=True)
            return {
                "source": "巨潮资讯财报披露",
                "reports": reports,
                "message": "" if reports else "暂无已发布财报。",
            }
        except Exception as exc:
            return {
                "source": "巨潮资讯财报披露",
                "reports": [],
                "message": str(exc)[:240],
            }

    def _safe_hk_financial_summary(self, symbol: str) -> dict[str, Any]:
        method = getattr(ak, "stock_financial_hk_analysis_indicator_em", None)
        indicator_fields = self._safe_dataframe_first_row(
            getattr(ak, "stock_hk_financial_indicator_em", None),
            {"symbol": symbol},
            source="东方财富港股财务指标",
        )
        latest_fields = indicator_fields.get("fields") or {}
        if method:
            try:
                dataframe = method(symbol=symbol, indicator="报告期")
                summary = self._build_financial_summary(
                    dataframe,
                    source="东方财富港股财务分析指标",
                    metric_map=(
                        ("BASIC_EPS", "基本每股收益"),
                        ("EPS_TTM", "每股收益TTM"),
                        ("BPS", "每股净资产"),
                        ("PER_OI", "每股营业收入"),
                        ("OPERATE_INCOME", "营业总收入"),
                        ("HOLDER_PROFIT", "股东应占利润"),
                        ("ROE_AVG", "净资产收益率"),
                        ("ROA", "总资产收益率"),
                        ("NET_PROFIT_RATIO", "销售净利率"),
                        ("DEBT_ASSET_RATIO", "资产负债率"),
                    ),
                )
                if summary["rows"]:
                    self._append_hk_latest_indicator_rows(summary, symbol, latest_fields)
                    return summary
            except Exception:
                pass

        summary = {
            "source": indicator_fields.get("source") or "东方财富港股财务指标",
            "periods": [],
            "rows": [{"指标": key, "值": value} for key, value in latest_fields.items() if value not in (None, "")],
            "message": indicator_fields.get("message") or ("暂无港股财务数据。" if not latest_fields else ""),
        }
        self._append_hk_latest_indicator_rows(summary, symbol, latest_fields)
        return summary

    def _append_hk_latest_indicator_rows(
        self,
        summary: dict[str, Any],
        symbol: str,
        latest_fields: dict[str, Any],
    ) -> None:
        latest_metrics = {
            "市盈率(TTM)": latest_fields.get("市盈率"),
            "市净率": latest_fields.get("市净率"),
            "每股收益": latest_fields.get("基本每股收益(元)"),
            "每股净资产": latest_fields.get("每股净资产(元)"),
            "净资产收益率": latest_fields.get("股东权益回报率(%)"),
            "每股股息TTM": latest_fields.get("每股股息TTM(港元)"),
            "股息率TTM": latest_fields.get("股息率TTM(%)"),
        }
        reserve_per_share = self._safe_hk_reserve_per_share(symbol, latest_fields)
        if reserve_per_share is not None:
            latest_metrics["每股公积金"] = reserve_per_share
        latest_dividend = self._safe_hk_latest_dividend(symbol, latest_fields)
        if latest_dividend:
            latest_metrics["最新分红"] = latest_dividend

        rows = summary.setdefault("rows", [])
        existing_labels = {
            str(row.get("指标") or row.get("metric") or "").strip()
            for row in rows
            if isinstance(row, dict)
        }
        for label, value in latest_metrics.items():
            if value in (None, "") or label in existing_labels:
                continue
            rows.append({"指标": label, "值": self._json_safe(value)})
            existing_labels.add(label)

    def _safe_hk_latest_dividend(self, symbol: str, latest_fields: dict[str, Any]) -> str | None:
        method = getattr(ak, "stock_hk_dividend_payout_em", None)
        if method:
            try:
                dataframe = method(symbol=symbol)
                rows = self._safe_dataframe_records(dataframe, limit=1)
                if rows:
                    latest = rows[0]
                    plan = str(latest.get("分红方案") or "").strip()
                    notice_date = self._coerce_report_date(latest.get("最新公告日期"))
                    if plan:
                        return f"{plan}（{notice_date}）" if notice_date else plan
            except Exception:
                pass
        dividend_ttm = latest_fields.get("每股股息TTM(港元)")
        if dividend_ttm not in (None, ""):
            return f"每股股息TTM {dividend_ttm}港元"
        return None

    def _safe_hk_reserve_per_share(self, symbol: str, latest_fields: dict[str, Any]) -> float | None:
        shares = self._to_float(
            latest_fields.get("已发行股本(股)")
            or latest_fields.get("已发行股本-H股(股)")
            or latest_fields.get("法定股本(股)")
        )
        if not shares or shares <= 0:
            return None
        method = getattr(ak, "stock_financial_hk_report_em", None)
        if not method:
            return None
        try:
            dataframe = method(stock=symbol, symbol="资产负债表", indicator="报告期")
            if dataframe is None or dataframe.empty:
                return None
            records = self._safe_dataframe_records(dataframe, limit=5000)
            by_period: dict[str, list[dict[str, Any]]] = {}
            for item in records:
                report_date = self._coerce_report_date(item.get("REPORT_DATE") or item.get("STD_REPORT_DATE"))
                if report_date:
                    by_period.setdefault(report_date, []).append(item)
            for report_date in sorted(by_period.keys(), reverse=True):
                rows = by_period[report_date]
                reserve_row = next(
                    (
                        item
                        for item in rows
                        if str(item.get("STD_ITEM_NAME") or "").strip() in {"资本公积", "储备", "股份溢价", "股本溢价"}
                    ),
                    None,
                )
                if reserve_row is None:
                    reserve_row = next(
                        (
                            item
                            for item in rows
                            if any(keyword in str(item.get("STD_ITEM_NAME") or "") for keyword in ("公积", "储备", "溢价"))
                        ),
                        None,
                    )
                reserve = self._to_float(reserve_row.get("AMOUNT")) if reserve_row else None
                if reserve is not None:
                    return reserve / shares
        except Exception:
            return None
        return None

    def _safe_hk_financial_reports(self, symbol: str) -> dict[str, Any]:
        try:
            url = "https://datacenter.eastmoney.com/securities/api/data/v1/get"
            params = {
                "reportName": "RPT_CUSTOM_HKSK_APPFN_CASHFLOW_SUMMARY",
                "columns": (
                    "SECUCODE,SECURITY_CODE,SECURITY_NAME_ABBR,START_DATE,REPORT_DATE,"
                    "FISCAL_YEAR,CURRENCY,ACCOUNT_STANDARD,REPORT_TYPE"
                ),
                "quoteColumns": "",
                "filter": f'(SECUCODE="{symbol}.HK")',
                "source": "F10",
                "client": "PC",
                "v": "02092616586970355",
            }
            with httpx.Client(
                trust_env=False,
                timeout=20.0,
                headers={"User-Agent": "Mozilla/5.0", "Referer": "https://emweb.securities.eastmoney.com/"},
            ) as client:
                response = client.get(url, params=params)
                response.raise_for_status()
                payload = response.json()
            report_list = (((payload or {}).get("result") or {}).get("data") or [{}])[0].get("REPORT_LIST") or []
            reports_by_period: dict[tuple[str, str], dict[str, Any]] = {}
            for item in report_list:
                report_date = self._coerce_report_date(item.get("REPORT_DATE") or item.get("STD_REPORT_DATE"))
                report_type = self._normalize_hk_report_type(item.get("REPORT_TYPE"))
                if not report_date:
                    continue
                period = report_date[:10]
                title = f"{item.get('SECURITY_NAME_ABBR') or symbol}：{self._format_report_name(report_type, period)}"
                reports_by_period[(period, report_type)] = {
                    "report_name": title,
                    "report_type": report_type,
                    "report_date": period,
                    "notice_date": period,
                    "url": None,
                    "source_name": "东方财富港股财报",
                    "title": title,
                }
            official_reports = self._fetch_hkex_financial_report_notices(symbol)
            for item in official_reports:
                report_date = self._coerce_report_date(item.get("report_date") or item.get("notice_date"))
                report_type = self._normalize_hk_report_type(item.get("report_type"))
                if not report_date:
                    continue
                key = (report_date[:10], report_type)
                official_entry = self._normalize_hk_financial_report_entry(
                    symbol=symbol,
                    report_type=report_type,
                    report_date=report_date[:10],
                    notice_date=self._coerce_report_date(item.get("notice_date")) or report_date[:10],
                    url=item.get("url"),
                    source_name="HKEXnews",
                    title=item.get("title") or item.get("report_name"),
                )
                current = reports_by_period.get(key)
                reports_by_period[key] = (
                    official_entry if current is None else self._merge_hk_financial_report_entry(current, official_entry)
                )
            reports = list(reports_by_period.values())
            reports.sort(key=lambda row: row.get("report_date") or "", reverse=True)
            return {
                "source": "东方财富港股财报 / HKEXnews",
                "reports": reports,
                "message": "" if reports else "暂无已发布财报。",
            }
        except Exception as exc:
            official_reports = self._fetch_hkex_financial_report_notices(symbol)
            if official_reports:
                return {
                    "source": "HKEXnews",
                    "reports": official_reports,
                    "message": "",
                }
            return {
                "source": "东方财富港股财报",
                "reports": [],
                "message": str(exc)[:240],
            }

    def _safe_hk_business_composition(self, symbol: str) -> dict[str, Any]:
        try:
            payload = self._request_eastmoney_json(
                "https://datacenter.eastmoney.com/securities/api/data/v1/get",
                {
                    "reportName": "RPT_HKF10_ORG_BUSSINESS",
                    "columns": (
                        "SECUCODE,SECURITY_CODE,SECURITY_NAME_ABBR,ORG_CODE,SECURITY_INNER_CODE,"
                        "REPORT_DATE,BUSINESS_REVIEW"
                    ),
                    "quoteColumns": "",
                    "filter": f'(SECUCODE="{symbol}.HK")',
                    "pageNumber": "1",
                    "pageSize": "1",
                    "sortTypes": "-1",
                    "sortColumns": "REPORT_DATE",
                    "source": "F10",
                    "client": "PC",
                    "v": "07945646099062258",
                },
                {
                    "User-Agent": "Mozilla/5.0",
                    "Referer": "https://emweb.securities.eastmoney.com/PC_HKF10/pages/home/index.html",
                },
            )
            rows = (((payload or {}).get("result") or {}).get("data") or [])
            first = rows[0] if rows else {}
            review = str(first.get("BUSINESS_REVIEW") or "").strip()
            report_date = self._coerce_report_date(first.get("REPORT_DATE"))
            items = self._parse_hk_business_review_items(review)
            return {
                "source": "东方财富港股业务回顾",
                "report_date": report_date,
                "sections": [{"category": "业务回顾", "items": items}] if items else [],
                "review": review,
                "message": (
                    "免费源暂无稳定结构化主营构成，当前展示业务回顾。"
                    if review
                    else "港股主营构成暂无稳定公开数据接口。"
                ),
            }
        except Exception as exc:
            return {
                "source": "东方财富港股业务回顾",
                "report_date": None,
                "sections": [],
                "review": "",
                "message": str(exc)[:240],
            }

    @classmethod
    def _parse_hk_business_review_items(cls, review: str) -> list[dict[str, Any]]:
        if not review:
            return []
        parts = [
            match.group(1).strip()
            for match in re.finditer(
                r"(?:\([一二三四五六七八九十]+\)|（[一二三四五六七八九十]+）)(.*?)(?=(?:\([一二三四五六七八九十]+\)|（[一二三四五六七八九十]+）)|$)",
                review,
                flags=re.S,
            )
        ]
        if not parts:
            parts = [review]
        items: list[dict[str, Any]] = []
        for part in parts[:8]:
            cleaned = re.sub(r"\s+", "", part)
            if not cleaned:
                continue
            name = re.split(r"[。；;]", cleaned, maxsplit=1)[0][:48]
            items.append({"name": name or "业务回顾", "summary": cleaned[:260]})
        return items

    def _safe_hk_financial_statements(self, symbol: str) -> dict[str, Any]:
        result: dict[str, Any] = {
            "source": "AkShare 东方财富港股三大报表",
            "balance_sheet": self._empty_statement_section("资产负债表"),
            "income_statement": self._empty_statement_section("利润表"),
            "cash_flow": self._empty_statement_section("现金流量表"),
        }
        statement_map = (
            (
                "income_statement",
                "利润表",
                "利润表",
                (
                    ("营业额", ("营业额", "营运收入")),
                    ("毛利", ("毛利",)),
                    ("经营溢利", ("经营溢利",)),
                    ("除税前溢利", ("除税前溢利", "除税前溢利(业务利润)")),
                    ("股东应占溢利", ("股东应占溢利",)),
                    ("每股基本盈利", ("每股基本盈利",)),
                ),
            ),
            (
                "balance_sheet",
                "资产负债表",
                "资产负债表",
                (
                    ("总资产", ("总资产",)),
                    ("流动资产合计", ("流动资产合计",)),
                    ("非流动资产合计", ("非流动资产合计",)),
                    ("现金及等价物", ("现金及等价物",)),
                    ("流动负债合计", ("流动负债合计",)),
                    ("股东权益合计", ("总权益", "权益总额", "股东权益合计", "所有者权益合计")),
                ),
            ),
            (
                "cash_flow",
                "现金流量表",
                "现金流量表",
                (
                    ("经营产生现金", ("经营产生现金",)),
                    ("经营业务现金净额", ("经营业务现金净额",)),
                    ("投资业务现金净额", ("投资业务现金净额",)),
                    ("融资业务现金净额", ("融资业务现金净额",)),
                    ("现金及现金等价物增加", ("现金及现金等价物增加", "现金及等价物增加", "现金及现金等价物净增加额")),
                ),
            ),
        )
        for key, label, akshare_symbol, metric_map in statement_map:
            method = getattr(ak, "stock_financial_hk_report_em", None)
            if not method:
                result[key]["message"] = "港股三大报表接口不可用。"
                continue
            try:
                dataframe = method(stock=symbol, symbol=akshare_symbol, indicator="报告期")
                if dataframe is None or dataframe.empty:
                    dataframe = method(stock=symbol, symbol=akshare_symbol, indicator="年度")
                result[key] = self._build_hk_statement_section(
                    dataframe=dataframe,
                    label=label,
                    metric_map=metric_map,
                )
            except Exception as exc:
                result[key] = self._empty_statement_section(label, str(exc)[:240])
        return result

    @classmethod
    def _build_hk_statement_section(
        cls,
        dataframe: pd.DataFrame,
        label: str,
        metric_map: tuple[tuple[str, tuple[str, ...]], ...],
    ) -> dict[str, Any]:
        if dataframe is None or dataframe.empty:
            section = cls._empty_statement_section(label)
            section["source"] = "AkShare 东方财富港股三大报表"
            return section

        records = cls._safe_dataframe_records(dataframe, limit=5000)
        by_period: dict[str, list[dict[str, Any]]] = {}
        for item in records:
            report_date = cls._coerce_report_date(item.get("REPORT_DATE") or item.get("STD_REPORT_DATE"))
            if not report_date:
                continue
            by_period.setdefault(report_date, []).append(item)
        sorted_periods = sorted(by_period.keys(), reverse=True)
        period_payloads: list[dict[str, Any]] = []
        for report_date in sorted_periods[:6]:
            rows = cls._pick_hk_statement_rows(by_period[report_date], metric_map)
            if not rows:
                continue
            period_payloads.append(
                {
                    "报告日期": report_date,
                    "报告名称": label,
                    "数据": rows,
                }
            )
        latest = period_payloads[0] if period_payloads else {}
        latest_rows = cls._safe_dataframe_records(pd.DataFrame(latest.get("数据") or []), limit=30)
        return {
            "label": label,
            "source": "AkShare 东方财富港股三大报表",
            "report_date": latest.get("报告日期"),
            "report_name": latest.get("报告名称"),
            "rows": latest_rows,
            "periods": period_payloads,
            "message": "" if latest_rows else "暂无报表数据。",
        }

    @classmethod
    def _pick_hk_statement_rows(
        cls,
        rows: list[dict[str, Any]],
        metric_map: tuple[tuple[str, tuple[str, ...]], ...],
    ) -> list[dict[str, Any]]:
        picked: list[dict[str, Any]] = []
        for metric_label, aliases in metric_map:
            matched = next(
                (
                    item
                    for item in rows
                    if str(item.get("STD_ITEM_NAME") or "").strip() in aliases
                ),
                None,
            )
            if not matched:
                matched = next(
                    (
                        item
                        for item in rows
                        if any(alias in str(item.get("STD_ITEM_NAME") or "") for alias in aliases)
                    ),
                    None,
                )
            if matched and matched.get("AMOUNT") not in (None, ""):
                picked.append(
                    {
                        "指标": metric_label,
                        "值": cls._json_safe(matched.get("AMOUNT")),
                    }
                )
        return picked

    def _fetch_hkex_financial_report_notices(self, symbol: str) -> list[dict[str, Any]]:
        reports: dict[tuple[str, str], dict[str, Any]] = {}
        today = date.today()
        start_date = date(today.year - 8, 1, 1).isoformat()
        end_date = today.isoformat()
        for notice in self._fetch_hkex_notices(symbol, start_date, end_date):
            title = str(notice.title or "")
            notice_type = str(notice.notice_type or "")
            combined_text = f"{title} {notice_type}".strip()
            if not self._is_hk_financial_report_title(combined_text):
                continue
            priority = self._hk_report_notice_priority(combined_text)
            if priority < 0:
                continue
            report_type = self._normalize_hk_report_type(self._guess_hk_report_type(combined_text))
            report_date = self._guess_hk_report_date(combined_text, report_type, notice.notice_date)
            key = (report_date or notice.notice_date, report_type)
            if not key[0]:
                continue
            candidate = {
                "report_name": f"{notice.symbol}：{self._format_report_name(report_type, key[0])}",
                "report_type": report_type,
                "report_date": report_date or notice.notice_date,
                "notice_date": notice.notice_date,
                "url": notice.url,
                "source_name": "HKEXnews",
                "title": title or notice_type,
                "_priority": priority,
            }
            current = reports.get(key)
            if current is None or self._prefer_hk_report_notice(candidate, current):
                reports[key] = candidate
        result = list(reports.values())
        for item in result:
            item.pop("_priority", None)
        result.sort(key=lambda row: (row.get("report_date") or "", row.get("notice_date") or ""), reverse=True)
        return result

    @classmethod
    def _normalize_hk_financial_report_entry(
        cls,
        symbol: str,
        report_type: str,
        report_date: str,
        notice_date: str,
        url: Any,
        source_name: str,
        title: str | None,
    ) -> dict[str, Any]:
        report_name = f"{symbol}：{cls._format_report_name(report_type, report_date)}"
        return {
            "report_name": report_name,
            "report_type": report_type,
            "report_date": report_date,
            "notice_date": notice_date,
            "url": str(url).strip() or None,
            "source_name": source_name,
            "title": str(title or "").strip() or report_name,
        }

    @staticmethod
    def _merge_hk_financial_report_entry(
        base: dict[str, Any],
        official: dict[str, Any],
    ) -> dict[str, Any]:
        merged = dict(base)
        if official.get("notice_date") not in (None, ""):
            merged["notice_date"] = official["notice_date"]
        if official.get("url") not in (None, ""):
            merged["url"] = official["url"]
        if official.get("title") not in (None, ""):
            merged["title"] = official["title"]
        merged["source_name"] = "东方财富港股财报 / HKEXnews"
        return merged

    @staticmethod
    def _hk_report_notice_priority(text: str) -> int:
        normalized = re.sub(r"\s+", "", html.unescape(text or ""))
        if "月报" in normalized or "月報" in normalized:
            return -1000
        score = 0
        if "财务报表" in normalized or "財務報表" in normalized:
            score += 120
        if any(keyword in normalized for keyword in ("年度报告", "年度報告", "中期报告", "中期報告", "半年度报告", "半年度報告", "年报", "年報", "半年报", "半年報")):
            score += 100
        if "业绩" in normalized or "業績" in normalized:
            score += 60
        if "股息" in normalized or "分派" in normalized:
            score -= 20
        return score

    @classmethod
    def _prefer_hk_report_notice(cls, candidate: dict[str, Any], current: dict[str, Any]) -> bool:
        candidate_priority = int(candidate.get("_priority") or cls._hk_report_notice_priority(str(candidate.get("title") or "")))
        current_priority = int(current.get("_priority") or cls._hk_report_notice_priority(str(current.get("title") or "")))
        if candidate_priority != current_priority:
            return candidate_priority > current_priority
        candidate_has_link = bool(str(candidate.get("url") or "").strip())
        current_has_link = bool(str(current.get("url") or "").strip())
        if candidate_has_link != current_has_link:
            return candidate_has_link
        return str(candidate.get("notice_date") or "") > str(current.get("notice_date") or "")

    @staticmethod
    def _is_hk_financial_report_title(title: str) -> bool:
        normalized = re.sub(r"\s+", "", html.unescape(title or ""))
        if "月报" in normalized or "月報" in normalized:
            return False
        keywords = (
            "业绩公告",
            "業績公告",
            "业绩报告",
            "業績報告",
            "中期业绩",
            "中期業績",
            "年度业绩",
            "年度業績",
            "中期报告",
            "中期報告",
            "年度报告",
            "年度報告",
            "半年度报告",
            "半年度報告",
            "中报",
            "中報",
            "年报",
            "年報",
            "半年报",
            "半年報",
            "一季报",
            "一季報",
            "三季报",
            "三季報",
            "季度业绩",
            "季度業績",
            "财务报告",
            "財務報告",
            "财务报表",
            "財務報表",
            "末期业绩",
            "末期業績",
        )
        return any(keyword in normalized for keyword in keywords)

    def _fetch_hkex_notices_by_title(
        self,
        symbol: str,
        title: str,
        start_date: str,
        end_date: str,
    ) -> list[NoticeRecord]:
        stock_id = self._lookup_hkex_stock_id(symbol)
        if not stock_id:
            return []
        url = "https://www1.hkexnews.hk/search/titleSearchServlet.do"
        params = {
            "sortDir": "0",
            "sortByOptions": "DateTime",
            "category": "0",
            "market": "SEHK",
            "stockId": stock_id,
            "documentType": "",
            "fromDate": start_date,
            "toDate": end_date,
            "title": title,
            "searchType": "1",
            "t1code": "",
            "t2Gcode": "",
            "t2code": "",
            "rowRange": "60",
            "lang": "zh",
        }
        headers = {
            "User-Agent": "Mozilla/5.0",
            "Referer": "https://www1.hkexnews.hk/search/titlesearch.xhtml?lang=zh",
        }
        try:
            with httpx.Client(timeout=20.0, headers=headers, trust_env=False) as client:
                response = client.get(url, params=params)
                response.raise_for_status()
                payload = response.json()
        except Exception:
            return []
        result = (payload or {}).get("result")
        if isinstance(result, str):
            try:
                rows = json.loads(result or "[]")
            except Exception:
                return []
        else:
            rows = result or []
        if not isinstance(rows, list):
            return []
        return self._normalize_hkex_notice_rows(rows, symbol=symbol)

    @staticmethod
    def _guess_hk_report_type(title: str) -> str:
        normalized = re.sub(r"\s+", "", html.unescape(title or ""))
        if any(keyword in normalized for keyword in ("第一季度业绩", "第一季度报告", "一季报", "一季度报", "一季度業績", "一季度業績公告", "三月三十一日")):
            return "一季报"
        if any(keyword in normalized for keyword in ("第三季度业绩", "第三季度报告", "三季报", "三季度报", "三季度業績", "三季度業績公告", "九月三十日")):
            return "三季报"
        if any(keyword in normalized for keyword in ("中期业绩", "中期業績", "中期报告", "中期報告", "半年度报告", "半年度報告", "中期/半年度报告", "中期/半年度報告", "半年报", "半年報", "止六个月", "止六個月", "六月三十日")):
            return "半年报"
        if any(keyword in normalized for keyword in ("年度业绩", "年度業績", "年度报告", "年度報告", "年度之业绩", "年度之業績", "末期业绩", "末期業績", "年報", "年报", "全年业绩", "全年業績", "十二月三十一日")):
            return "年报"
        return "财报"

    @classmethod
    def _guess_hk_report_date(
        cls,
        title: str,
        report_type: str,
        fallback_notice_date: str | None = None,
    ) -> str | None:
        year_match = re.search(r"(20\d{2})", title)
        year = year_match.group(1) if year_match else cls._extract_chinese_year(title)
        if not year and fallback_notice_date and re.fullmatch(r"\d{4}-\d{2}-\d{2}", fallback_notice_date):
            year = fallback_notice_date[:4]
        if not year:
            return None
        suffix_map = {
            "一季报": "03-31",
            "半年报": "06-30",
            "三季报": "09-30",
            "年报": "12-31",
        }
        suffix = suffix_map.get(report_type)
        return f"{year}-{suffix}" if suffix else None

    @staticmethod
    def _normalize_hk_report_type(value: Any, fallback: str = "财报") -> str:
        normalized = re.sub(r"\s+", "", html.unescape(str(value or "")))
        if not normalized:
            return fallback
        if any(keyword in normalized for keyword in ("第一季度", "一季度", "一季报", "一季報", "三月三十一日")):
            return "一季报"
        if any(keyword in normalized for keyword in ("中期", "半年度", "半年报", "半年報", "中报", "中報", "止六个月", "止六個月", "六月三十日")):
            return "半年报"
        if any(keyword in normalized for keyword in ("第三季度", "三季度", "三季报", "三季報", "九月三十日")):
            return "三季报"
        if any(keyword in normalized for keyword in ("年度", "年报", "年報", "全年", "末期业绩", "末期業績", "十二月三十一日")):
            return "年报"
        return fallback

    @staticmethod
    def _safe_int(value: Any) -> int:
        try:
            return int(value)
        except (TypeError, ValueError):
            return 0

    @staticmethod
    def _extract_chinese_year(text: str) -> str | None:
        match = re.search(r"([二零〇一二三四五六七八九]{4})年", text)
        if not match:
            return None
        mapping = {
            "零": "0",
            "〇": "0",
            "一": "1",
            "二": "2",
            "三": "3",
            "四": "4",
            "五": "5",
            "六": "6",
            "七": "7",
            "八": "8",
            "九": "9",
        }
        return "".join(mapping.get(char, "") for char in match.group(1)) or None

    @classmethod
    def _build_financial_summary(
        cls,
        dataframe: pd.DataFrame,
        source: str,
        metric_map: tuple[tuple[str, str], ...],
    ) -> dict[str, Any]:
        if dataframe is None or dataframe.empty:
            return {"source": source, "periods": [], "rows": [], "message": "暂无财务数据。"}
        rows = dataframe.to_dict(orient="records")
        periods: list[str] = []
        period_rows: list[dict[str, Any]] = []
        for item in rows:
            raw_period = item.get("REPORT_DATE") or item.get("STD_REPORT_DATE") or item.get("报告期")
            period = str(raw_period or "").split(" ")[0][:10]
            if not period or period in periods:
                continue
            periods.append(period)
            period_rows.append(item)
            if len(periods) >= 8:
                break
        summary_rows: list[dict[str, Any]] = []
        for field, label in metric_map:
            values = {
                period: cls._json_safe(item.get(field))
                for period, item in zip(periods, period_rows)
                if item.get(field) not in (None, "")
            }
            if values:
                summary_rows.append({"指标": label, "数据": values})
        return {
            "source": source,
            "periods": periods,
            "rows": summary_rows,
            "latest": {
                "report_date": periods[0] if periods else None,
                "report_name": str(
                    period_rows[0].get("REPORT_DATE_NAME")
                    or period_rows[0].get("REPORT_TYPE")
                    or ""
                ).strip()
                or None
                if period_rows
                else None,
                "notice_date": cls._coerce_report_date(
                    period_rows[0].get("NOTICE_DATE")
                )
                if period_rows
                else None,
            },
            "message": "" if summary_rows else "暂无财务数据。",
        }

    def _safe_cn_financial_statements(self, symbol: str) -> dict[str, Any]:
        result: dict[str, Any] = {
            "source": "东方财富财务报表",
            "balance_sheet": self._empty_statement_section("资产负债表"),
            "income_statement": self._empty_statement_section("利润表"),
            "cash_flow": self._empty_statement_section("现金流量表"),
        }
        stock_code = f"SH{symbol}" if symbol.startswith("6") else f"SZ{symbol}"
        statement_map = (
            (
                "income_statement",
                "利润表",
                getattr(ak, "stock_profit_sheet_by_report_em", None),
                (
                    ("TOTAL_OPERATE_INCOME", "营业总收入"),
                    ("OPERATE_PROFIT", "营业利润"),
                    ("TOTAL_PROFIT", "利润总额"),
                    ("NETPROFIT", "净利润"),
                    ("PARENT_NETPROFIT", "归母净利润"),
                    ("TOTAL_OPERATE_INCOME_YOY", "营业总收入同比增长"),
                    ("PARENT_NETPROFIT_YOY", "归母净利润同比增长"),
                ),
            ),
            (
                "balance_sheet",
                "资产负债表",
                getattr(ak, "stock_balance_sheet_by_report_em", None),
                (
                    ("TOTAL_ASSETS", "资产合计"),
                    ("TOTAL_LIABILITIES", "负债合计"),
                    ("TOTAL_EQUITY", "所有者权益合计"),
                    ("TOTAL_CURRENT_ASSETS", "流动资产合计"),
                    ("TOTAL_CURRENT_LIAB", "流动负债合计"),
                    ("MONETARYFUNDS", "货币资金"),
                ),
            ),
            (
                "cash_flow",
                "现金流量表",
                getattr(ak, "stock_cash_flow_sheet_by_report_em", None),
                (
                    ("NETCASH_OPERATE", "经营现金流量净额"),
                    ("NETCASH_INVEST", "投资现金流量净额"),
                    ("NETCASH_FINANCE", "筹资现金流量净额"),
                    ("CCE_ADD", "现金及现金等价物净增加额"),
                    ("END_CASH", "期末现金及现金等价物余额"),
                ),
            ),
        )
        for key, statement_name, method, metric_map in statement_map:
            if not method:
                result[key]["message"] = "接口不可用。"
                continue
            try:
                dataframe = method(symbol=stock_code)
                result[key] = self._build_statement_section(
                    dataframe=dataframe,
                    source="东方财富财务报表",
                    label=statement_name,
                    metric_map=metric_map,
                )
            except Exception as exc:
                result[key] = self._empty_statement_section(statement_name, str(exc)[:240])
        return result

    @classmethod
    def _empty_statement_section(cls, label: str, message: str = "") -> dict[str, Any]:
        return {
            "label": label,
            "source": "东方财富财务报表",
            "report_date": None,
            "report_name": None,
            "rows": [],
            "periods": [],
            "message": message or "暂无报表数据。",
        }

    @classmethod
    def _build_statement_section(
        cls,
        dataframe: pd.DataFrame,
        source: str,
        label: str,
        metric_map: tuple[tuple[str, str], ...],
    ) -> dict[str, Any]:
        if dataframe is None or dataframe.empty:
            return cls._empty_statement_section(label)
        records = dataframe.to_dict(orient="records")
        latest = records[0] if records else {}
        report_date = cls._coerce_report_date(latest.get("REPORT_DATE"))
        report_name = str(latest.get("REPORT_DATE_NAME") or latest.get("REPORT_TYPE") or "").strip() or None
        rows: list[dict[str, Any]] = []
        for field, metric_label in metric_map:
            value = latest.get(field)
            if value not in (None, ""):
                rows.append({"指标": metric_label, "值": cls._json_safe(value)})
        periods = []
        if rows:
            periods.append(
                {
                    "报告日": report_date or "",
                    "报告名称": report_name or "",
                    "数据": rows,
                }
            )
        return {
            "label": label,
            "source": source,
            "report_date": report_date,
            "report_name": report_name,
            "rows": rows,
            "periods": periods,
            "message": "" if rows else "暂无报表数据。",
        }

    @staticmethod
    def _guess_report_type(title: str, fallback: str = "财报") -> str:
        if "半年度报告" in title or "半年报" in title:
            return "半年报"
        if "第一季度报告" in title or "一季报" in title:
            return "一季报"
        if "第三季度报告" in title or "三季报" in title:
            return "三季报"
        if "年度报告" in title or "年报" in title:
            return "年报"
        if fallback in {"年报", "半年报", "一季报", "三季报"}:
            return fallback
        return "财报"

    @staticmethod
    def _report_title_priority(title: str) -> int:
        if "摘要" in title or "英文版" in title or "修订" in title:
            return 1
        if "报告" in title:
            return 0
        return 2

    @staticmethod
    def _clean_cninfo_title(value: Any) -> str:
        title = html.unescape(str(value or "")).strip()
        title = re.sub(r"</?em>", "", title, flags=re.IGNORECASE)
        return re.sub(r"\s+", " ", title)

    @staticmethod
    def _should_skip_financial_report(title: str) -> bool:
        skip_keywords = ("摘要", "英文", "取消", "问询函", "意见", "审核", "审计报告")
        return any(keyword in title for keyword in skip_keywords)

    @staticmethod
    def _report_period_end_date(year: str, report_type: str) -> str | None:
        suffix_map = {
            "年报": "12-31",
            "半年报": "06-30",
            "中报": "06-30",
            "一季报": "03-31",
            "三季报": "09-30",
        }
        suffix = suffix_map.get(report_type)
        return f"{year}-{suffix}" if suffix else None

    @staticmethod
    def _build_cninfo_announcement_url(
        symbol: str,
        announcement_id: str,
        org_id: str,
        notice_date: str | None,
        adjunct_url: str,
    ) -> str | None:
        if announcement_id and org_id:
            return "https://www.cninfo.com.cn/new/disclosure/detail?" + urlencode(
                {
                    "stockCode": symbol,
                    "announcementId": announcement_id,
                    "orgId": org_id,
                    "announcementTime": notice_date or "",
                }
            )
        if adjunct_url:
            return f"https://static.cninfo.com.cn/{adjunct_url.lstrip('/')}"
        return None

    @staticmethod
    def _format_report_name(report_type: str, report_date: str) -> str:
        year = report_date[:4] if len(report_date) >= 4 else report_date
        report_name_map = {
            "年报": "年度报告",
            "中报": "半年度报告",
            "半年报": "半年度报告",
            "一季报": "第一季度报告",
            "三季报": "第三季度报告",
        }
        label = report_name_map.get(report_type, "财报")
        return f"{year}年{label}"

    def _safe_cn_business_composition(self, symbol: str) -> dict[str, Any]:
        method = getattr(ak, "stock_zygc_em", None)
        if not method:
            return {
                "source": "东方财富主营构成",
                "report_date": None,
                "sections": [],
                "message": "主营构成接口不可用。",
            }
        try:
            exchange_symbol = f"SH{symbol}" if symbol.startswith("6") else f"SZ{symbol}"
            dataframe = method(symbol=exchange_symbol)
            if dataframe is None or dataframe.empty:
                return {
                    "source": "东方财富主营构成",
                    "report_date": None,
                    "sections": [],
                    "message": "暂无主营构成数据。",
                }
            frame = dataframe.copy()
            report_dates = pd.to_datetime(frame["报告日期"], unit="ms", errors="coerce")
            report_dates = report_dates.fillna(pd.to_datetime(frame["报告日期"], errors="coerce"))
            frame["__report_date"] = report_dates
            latest_date = frame["__report_date"].dropna().max()
            if pd.notna(latest_date):
                frame = frame[frame["__report_date"] == latest_date]

            sections: list[dict[str, Any]] = []
            for category, group in frame.groupby("分类类型", dropna=False):
                items: list[dict[str, Any]] = []
                group = group.sort_values(by=["收入比例", "主营收入"], ascending=False)
                for item in group.to_dict(orient="records"):
                    name = str(item.get("主营构成") or "").strip()
                    if not name:
                        continue
                    items.append(
                        {
                            "name": name,
                            "revenue": self._to_float(item.get("主营收入")),
                            "revenue_ratio": self._to_float(item.get("收入比例")),
                            "cost": self._to_float(item.get("主营成本")),
                            "cost_ratio": self._to_float(item.get("成本比例")),
                            "profit": self._to_float(item.get("主营利润")),
                            "profit_ratio": self._to_float(item.get("利润比例")),
                            "gross_margin": self._to_float(item.get("毛利率")),
                        }
                    )
                if items:
                    sections.append(
                        {
                            "category": str(category) if category not in (None, "") else "主营构成",
                            "items": items[:8],
                        }
                    )
            return {
                "source": "东方财富主营构成",
                "report_date": self._coerce_report_date(latest_date),
                "sections": sections,
                "message": "" if sections else "暂无主营构成数据。",
            }
        except Exception as exc:
            return {
                "source": "东方财富主营构成",
                "report_date": None,
                "sections": [],
                "message": str(exc)[:240],
            }

    def _safe_cn_fund_flow(self, symbol: str) -> dict[str, Any]:
        method = getattr(ak, "stock_individual_fund_flow", None)
        if not method:
            return self._fetch_cn_fund_flow_http(symbol)
        market = "sh" if symbol.startswith("6") else ("bj" if symbol.startswith(("4", "8")) else "sz")
        try:
            dataframe = method(stock=symbol, market=market)
            rows = self._safe_dataframe_records(dataframe, limit=30)
            if not rows:
                return self._fetch_cn_fund_flow_http(symbol)
            return {
                "source": "东方财富个股资金流",
                "rows": rows,
                "message": "资金流按交易日返回，金额单位为元。" if rows else "暂无资金流数据。",
            }
        except Exception as exc:
            fallback = self._fetch_cn_fund_flow_http(symbol)
            if fallback["rows"]:
                return fallback
            return {"source": "东方财富个股资金流", "rows": [], "message": str(exc)[:240]}

    def _fetch_cn_fund_flow_http(self, symbol: str) -> dict[str, Any]:
        market_code = 1 if symbol.startswith("6") else 0
        params = {
            "lmt": "30",
            "klt": "101",
            "secid": f"{market_code}.{symbol}",
            "fields1": "f1,f2,f3,f7",
            "fields2": "f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61,f62,f63,f64,f65",
            "ut": "b2884a393a59ad64002292a3e90d46a5",
            "_": str(int(time.time() * 1000)),
        }
        headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            "Accept": "application/json,text/plain,*/*",
            "Referer": "https://data.eastmoney.com/zjlx/detail.html",
            "Connection": "close",
        }
        for host in ("push2his.eastmoney.com", "82.push2his.eastmoney.com"):
            for scheme in ("https", "http"):
                try:
                    url = f"{scheme}://{host}/api/qt/stock/fflow/daykline/get"
                    payload = self._request_eastmoney_json(url, params, headers)
                    rows = self._build_cn_fund_flow_rows(payload)
                    if rows:
                        return {
                            "source": "东方财富个股资金流 HTTP 备用接口",
                            "rows": rows,
                            "message": "资金流按交易日返回，金额单位为元。",
                        }
                except Exception:
                    continue
        return {"source": "东方财富个股资金流", "rows": [], "message": "资金流接口暂时不可用，请稍后重试。"}

    def _build_cn_fund_flow_rows(self, payload: dict[str, Any], limit: int = 30) -> list[dict[str, Any]]:
        columns = (
            "日期",
            "主力净流入-净额",
            "小单净流入-净额",
            "中单净流入-净额",
            "大单净流入-净额",
            "超大单净流入-净额",
            "主力净流入-净占比",
            "小单净流入-净占比",
            "中单净流入-净占比",
            "大单净流入-净占比",
            "超大单净流入-净占比",
            "收盘价",
            "涨跌幅",
        )
        lines = ((payload or {}).get("data") or {}).get("klines") or []
        rows: list[dict[str, Any]] = []
        for line in lines[-limit:]:
            values = str(line).split(",")
            row = {
                column: self._json_safe(values[index]) if index < len(values) else None
                for index, column in enumerate(columns)
            }
            for key in columns[1:]:
                if key == "日期":
                    continue
                row[key] = self._to_float(row[key])
            rows.append(row)
        return rows

    def _request_eastmoney_json(self, url: str, params: dict[str, Any], headers: dict[str, str]) -> dict[str, Any]:
        def parse_response(response: Any) -> dict[str, Any]:
            response.raise_for_status()
            try:
                payload = response.json()
            except Exception:
                text = str(response.text or "").strip()
                left = text.find("(")
                right = text.rfind(")")
                if left >= 0 and right > left:
                    payload = json.loads(text[left + 1 : right])
                else:
                    payload = json.loads(text)
            if not isinstance(payload, dict):
                raise ValueError("Eastmoney response is not a JSON object")
            return payload

        if curl_requests is not None:
            try:
                response = curl_requests.get(
                    url,
                    params=params,
                    headers=headers,
                    timeout=15.0,
                    impersonate="chrome124",
                )
                return parse_response(response)
            except Exception:
                # curl-cffi can fail during TLS/proxy negotiation even when
                # the same endpoint is reachable through a regular client.
                # Fall through to httpx instead of aborting the whole sync.
                pass
        with httpx.Client(trust_env=False, timeout=15.0, headers=headers, follow_redirects=True) as client:
            response = client.get(url, params=params)
        return parse_response(response)

    def _safe_hk_fund_flow(self, symbol: str) -> dict[str, Any]:
        method = getattr(ak, "stock_hsgt_individual_em", None)
        if not method:
            return {
                "source": "暂无稳定公开个股资金流接口",
                "rows": [],
                "message": "港股个股资金流暂无稳定公开接口。",
        }
        try:
            dataframe = method(symbol=symbol)
            if dataframe is not None and not dataframe.empty and "持股日期" in dataframe.columns:
                sorted_dates = pd.to_datetime(dataframe["持股日期"], errors="coerce")
                dataframe = dataframe.assign(_sort_date=sorted_dates).sort_values("_sort_date", ascending=False)
                dataframe = dataframe.drop(columns=["_sort_date"])
            rows = self._safe_dataframe_records(dataframe, limit=90)
            return {
                "source": "东方财富港股通持股数据",
                "rows": rows,
                "message": (
                    "港股暂无与 A 股同口径的个股资金流，当前展示港股通持股及市值变化参考。"
                    if rows
                    else "暂无港股通持股数据。"
                ),
            }
        except Exception as exc:
            return {"source": "东方财富港股通持股数据", "rows": [], "message": str(exc)[:240]}

    @staticmethod
    def _normalize_hk_profile_fields(fields: dict[str, Any]) -> dict[str, Any]:
        normalized: dict[str, Any] = {}
        for key, value in fields.items():
            if key in {"上市日期", "公司成立日期"}:
                parsed = pd.to_datetime(value, errors="coerce")
                if not pd.isna(parsed):
                    normalized[key] = parsed.date().isoformat()
                    continue
            normalized[key] = value
        return normalized

    @classmethod
    def _safe_dataframe_first_row(
        cls,
        method: Any,
        kwargs: dict[str, Any],
        source: str,
    ) -> dict[str, Any]:
        if not method:
            return {"source": source, "fields": {}, "message": "接口不可用。"}
        try:
            dataframe = method(**kwargs)
            records = cls._safe_dataframe_records(dataframe, limit=1)
            return {"source": source, "fields": records[0] if records else {}, "message": ""}
        except Exception as exc:
            return {"source": source, "fields": {}, "message": str(exc)[:240]}

    @classmethod
    def _safe_holder_rows(
        cls,
        method: Any,
        kwargs: dict[str, Any],
        date_columns: tuple[str, ...],
        source: str,
    ) -> list[dict[str, Any]]:
        if not method:
            return []
        try:
            dataframe = method(**kwargs)
            if dataframe is None or dataframe.empty:
                return []
            date_column = next((column for column in date_columns if column in dataframe.columns), None)
            if date_column:
                latest = dataframe[date_column].dropna().astype(str).max()
                if latest:
                    dataframe = dataframe[dataframe[date_column].astype(str) == latest]
            return cls._safe_dataframe_records(dataframe, limit=20)
        except Exception:
            return []

    @classmethod
    def _safe_dataframe_records(cls, dataframe: Any, limit: int = 20) -> list[dict[str, Any]]:
        if dataframe is None or not hasattr(dataframe, "to_dict") or dataframe.empty:
            return []
        return [
            {str(key): cls._json_safe(value) for key, value in row.items()}
            for row in dataframe.head(limit).to_dict(orient="records")
        ]

    def _fetch_cn_a_symbols(self) -> list[SymbolRecord]:
        exchange_records = self._fetch_cn_a_symbols_from_exchange_tables()
        if exchange_records:
            return exchange_records
        eastmoney_records = self._fetch_cn_a_symbols_from_eastmoney()
        if eastmoney_records:
            return eastmoney_records
        dataframe = ak.stock_info_a_code_name()
        return self._normalize_dataframe(dataframe=dataframe, market="CN_A")

    def _fetch_hk_symbols(self) -> list[SymbolRecord]:
        dataframe = ak.stock_hk_spot_em()
        return self._normalize_dataframe(dataframe=dataframe, market="HK")

    def _fetch_cn_a_symbols_from_exchange_tables(self) -> list[SymbolRecord]:
        records_by_symbol: dict[str, SymbolRecord] = {}
        for method_name in ("stock_info_sh_name_code", "stock_info_sz_name_code", "stock_info_bj_name_code"):
            method = getattr(ak, method_name, None)
            if not method:
                continue
            try:
                dataframe = method()
            except Exception:
                continue
            if dataframe is not None and not dataframe.empty:
                dataframe = dataframe.copy()
                dataframe["_source_method"] = method_name
                for record in self._normalize_dataframe(dataframe, MARKET_CN_A):
                    records_by_symbol[record.symbol] = record
        return list(records_by_symbol.values())

    def _fetch_cn_a_symbols_from_eastmoney(self) -> list[SymbolRecord]:
        url = "https://push2.eastmoney.com/api/qt/clist/get"
        headers = {
            "User-Agent": "Mozilla/5.0",
            "Referer": "https://quote.eastmoney.com/",
        }
        params = {
            "pn": "1",
            "pz": "5000",
            "po": "1",
            "np": "1",
            "ut": "bd1d9ddb04089700cf9c27f6f7426281",
            "fltt": "2",
            "invt": "2",
            "fid": "f12",
            "fs": "m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23,m:0+t:81+s:2048",
            "fields": "f12,f13,f14,f26,f100,f102,f103,f104,f105,f106,f107",
        }
        try:
            rows: list[dict[str, Any]] = []
            total = 0
            page = 1
            while True:
                payload = self._request_eastmoney_json(url, {**params, "pn": str(page)}, headers)
                data = payload.get("data") or {}
                diff = data.get("diff") or []
                if not isinstance(diff, list):
                    break
                rows.extend(item for item in diff if isinstance(item, dict))
                if page == 1:
                    total = self._safe_int(data.get("total"))
                if not diff or (total and len(rows) >= total):
                    break
                page += 1
                if page > 20:
                    break
        except Exception:
            return []

        records: list[SymbolRecord] = []
        for row in rows:
            raw_code = row.get("f12")
            raw_name = row.get("f14")
            if self._is_empty(raw_code) or self._is_empty(raw_name):
                continue
            symbol = self._normalize_symbol(raw_code, MARKET_CN_A)
            if not symbol:
                continue
            list_date = self._normalize_neeq_list_date(row.get("f26"))
            records.append(
                SymbolRecord(
                    market=MARKET_CN_A,
                    symbol=symbol,
                    exchange=self._infer_exchange(symbol, MARKET_CN_A),
                    name=str(raw_name).strip(),
                    asset_type="STOCK",
                    status="LISTED",
                    list_date=list_date,
                    ext_json={
                        "board": row.get("f100"),
                        "industry": row.get("f102") or row.get("f103"),
                        "region": row.get("f104"),
                        "source": "Eastmoney push2 clist",
                    },
                    raw_payload=self._json_safe(row),
                )
            )
        return records

    def _fetch_neeq_symbols(self, layer: str) -> list[SymbolRecord]:
        rows = self._fetch_neeq_rows()
        market = MARKET_NEEQ_INNOVATION if layer.upper() == "INNOVATION" else MARKET_NEEQ
        layer_name = "INNOVATION" if market == MARKET_NEEQ_INNOVATION else "BASE"
        records: list[SymbolRecord] = []
        for row in rows:
            raw_code = row.get("f12")
            raw_name = row.get("f14")
            if self._is_empty(raw_code) or self._is_empty(raw_name):
                continue

            # The legacy clist endpoint exposes the layer in f111 (512 means
            # innovation layer). The code-table fallback exposes it in flag
            # (32 means innovation, 16 means base).
            flags = self._safe_int(row.get("f111") or row.get("flag"))
            is_innovation = bool(flags & 512) or flags == 32 or str(row.get("layer") or "").upper() == "INNOVATION"
            if market == MARKET_NEEQ_INNOVATION:
                if not is_innovation:
                    continue
            else:
                if is_innovation:
                    continue

            symbol = self._normalize_symbol(raw_code, market)
            if not symbol:
                continue

            records.append(
                SymbolRecord(
                    market=market,
                    symbol=symbol,
                    exchange="NEEQ",
                    name=str(raw_name).strip(),
                    asset_type="STOCK",
                    status="LISTED",
                    list_date=self._normalize_neeq_list_date(row.get("f26")),
                    ext_json={
                        "board": "NEEQ",
                        "layer": layer_name,
                        "layer_flags": flags,
                        "source": str(row.get("source") or "Eastmoney xinsanban"),
                        "pinyin": row.get("pinyin"),
                        "inner_code": row.get("innerCode"),
                    },
                    raw_payload=self._json_safe(row),
                )
            )
        return records

    def _fetch_neeq_rows(self) -> list[dict[str, Any]]:
        url = "https://push2.eastmoney.com/api/qt/clist/get"
        headers = {
            "User-Agent": "Mozilla/5.0",
            "Referer": "https://xinsanban.eastmoney.com/",
        }
        params = {
            "ut": "c964af255f56da290419b605978b89db",
            "fltt": "1",
            "invt": "2",
            "np": "1",
            "pn": "1",
            "pz": "500",
            "fid": "f3|f5",
            "po": "1",
            "fs": "m:0+t:81+s:!2048",
            "fields": "f12,f13,f14,f26,f111,f152",
        }
        try:
            rows: list[dict[str, Any]] = []
            total = 0
            page = 1
            while True:
                payload = self._request_eastmoney_json(url, {**params, "pn": str(page)}, headers)
                data = payload.get("data") or {}
                diff = data.get("diff") or []
                if not isinstance(diff, list):
                    break
                rows.extend(item for item in diff if isinstance(item, dict))
                if page == 1:
                    total = self._safe_int(data.get("total"))
                if not diff or (total and len(rows) >= total):
                    break
                page += 1
                if page > 100:
                    break
            if len(rows) >= 100:
                return rows
        except Exception:
            pass
        return self._fetch_neeq_code_table_rows()

    def _fetch_neeq_code_table_rows(self) -> list[dict[str, Any]]:
        """Fetch NEEQ master data from Eastmoney's code-table endpoint.

        The quote clist endpoint is intermittently blocked in some network
        environments. The code-table service is lighter and returns stable
        code/name/pinyin/layer metadata in pages up to 1,000 rows.
        """
        url = "https://search-codetable.eastmoney.com/codetable/search/web"
        headers = {
            "User-Agent": "Mozilla/5.0",
            "Referer": "https://quote.eastmoney.com/",
        }
        prefixes = (
            *(str(value) for value in range(430, 440)),
            *(str(value) for value in range(830, 840)),
            *(str(value) for value in range(870, 880)),
            *(str(value) for value in range(880, 890)),
        )
        rows_by_code: dict[str, dict[str, Any]] = {}
        for prefix in prefixes:
            try:
                payload = self._request_eastmoney_json(
                    url,
                    {"keyword": prefix, "pageIndex": 1, "pageSize": 1000},
                    headers,
                )
            except Exception:
                continue
            for item in payload.get("result") or []:
                if not isinstance(item, dict):
                    continue
                code = str(item.get("code") or "").strip()
                if (
                    not code.isdigit()
                    or not code.startswith(prefix)
                    or self._safe_int(item.get("smallType")) != 81
                    or self._safe_int(item.get("status")) not in {0, 10}
                ):
                    continue
                flag = self._safe_int(item.get("flag"))
                rows_by_code[code] = {
                    **item,
                    "f12": code,
                    "f14": item.get("shortName") or code,
                    "f111": flag,
                    "source": "Eastmoney code-table",
                }
        return list(rows_by_code.values())

    def _normalize_dataframe(self, dataframe: pd.DataFrame, market: str) -> list[SymbolRecord]:
        if dataframe.empty:
            return []

        code_column = self._find_column(dataframe, ("代码", "证券代码", "A股代码", "code", "symbol"))
        name_column = self._find_column(dataframe, ("名称", "中文名称", "证券简称", "A股简称", "name"))
        list_date_column = self._find_column(
            dataframe,
            ("上市日期", "A股上市日期", "上市日", "挂牌日期", "list_date", "listing_date", "LIST_DATE"),
        )
        if not code_column or not name_column:
            raise ValueError(
                f"Cannot identify code/name columns for {market}; received columns: {list(dataframe.columns)}"
            )

        records: list[SymbolRecord] = []
        for item in dataframe.to_dict(orient="records"):
            raw_code = item.get(code_column)
            raw_name = item.get(name_column)
            if self._is_empty(raw_code) or self._is_empty(raw_name):
                continue

            symbol = self._normalize_symbol(raw_code, market)
            if not symbol:
                continue

            records.append(
                SymbolRecord(
                    market=market,
                    symbol=symbol,
                    exchange=self._infer_exchange(symbol, market),
                    name=str(raw_name).strip(),
                    asset_type="STOCK",
                    status="LISTED",
                    list_date=self._normalize_neeq_list_date(item.get(list_date_column)) if list_date_column else None,
                    ext_json={
                        key: self._json_safe(value)
                        for key, value in item.items()
                        if key not in {code_column, name_column}
                        and value is not None
                        and not (isinstance(value, float) and pd.isna(value))
                    },
                    raw_payload=self._json_safe(item),
                )
            )
        return records

    @staticmethod
    def _normalize_neeq_list_date(value: Any) -> str | None:
        if value is None or value == "":
            return None
        text = str(value).strip()
        if not text:
            return None
        if re.fullmatch(r"\d{8}", text):
            try:
                return datetime.strptime(text, "%Y%m%d").date().isoformat()
            except ValueError:
                return None
        if re.fullmatch(r"\d{4}-\d{2}-\d{2}", text):
            return text
        parsed = pd.to_datetime(text, errors="coerce")
        if pd.isna(parsed):
            return None
        return parsed.date().isoformat()

    def _normalize_a_kline(
        self, dataframe: pd.DataFrame, symbol: str, period: str, adjust: str
    ) -> list[KlineRecord]:
        if dataframe.empty:
            return []
        records: list[KlineRecord] = []
        for item in dataframe.to_dict(orient="records"):
            trade_date = str(item.get("日期") or "").split(" ")[0]
            if not trade_date:
                continue
            records.append(
                KlineRecord(
                    market="CN_A",
                    symbol=symbol,
                    period=period,
                    adjust=adjust,
                    trade_date=trade_date,
                    open_price=self._to_float(item.get("开盘")),
                    high_price=self._to_float(item.get("最高")),
                    low_price=self._to_float(item.get("最低")),
                    close_price=self._to_float(item.get("收盘")),
                    volume=self._to_float(item.get("成交量")),
                    amount=self._to_float(item.get("成交额")),
                    turnover_rate=self._to_float(item.get("换手率")),
                    raw_payload=self._json_safe(item),
                )
            )
        return records

    def _normalize_a_hist_dataframe(self, dataframe: pd.DataFrame, method_name: str) -> pd.DataFrame:
        frame = dataframe.copy()
        rename_map = {}
        for source, target in (
            ("date", "日期"),
            ("open", "开盘"),
            ("high", "最高"),
            ("low", "最低"),
            ("close", "收盘"),
            ("volume", "成交量"),
            ("amount", "成交额"),
            ("turnover", "换手率"),
        ):
            if source in frame.columns:
                rename_map[source] = target
        if rename_map:
            frame = frame.rename(columns=rename_map)
        return frame

    def _fetch_hk_kline(
        self,
        symbol: str,
        period: str,
        start_date: str,
        end_date: str,
        adjust: str,
    ) -> pd.DataFrame:
        candidates: list[tuple[str, dict[str, Any]]] = [
            (
                "stock_hk_hist",
                {
                    "symbol": symbol,
                    "period": period,
                    "start_date": start_date or "19700101",
                    "end_date": end_date or "22220101",
                    "adjust": adjust,
                },
            ),
            ("stock_hk_daily", {"symbol": symbol, "adjust": adjust}),
        ]
        last_error: Exception | None = None
        for method_name, kwargs in candidates:
            method = getattr(ak, method_name, None)
            if not method:
                continue
            try:
                dataframe = method(**kwargs)
                if dataframe is not None and not dataframe.empty:
                    frame = self._normalize_a_hist_dataframe(dataframe, method_name)
                    if "日期" in frame.columns:
                        dates = pd.to_datetime(frame["日期"], errors="coerce")
                        if start_date:
                            frame = frame[dates >= pd.to_datetime(start_date)]
                            dates = pd.to_datetime(frame["日期"], errors="coerce")
                        if end_date:
                            frame = frame[dates <= pd.to_datetime(end_date)]
                    return frame
            except Exception as exc:
                last_error = exc
        if last_error:
            raise last_error
        return pd.DataFrame()

    def _normalize_hk_kline(
        self, dataframe: pd.DataFrame, symbol: str, period: str, adjust: str
    ) -> list[KlineRecord]:
        if dataframe.empty:
            return []
        records: list[KlineRecord] = []
        for item in dataframe.to_dict(orient="records"):
            trade_date = str(item.get("日期") or "").split(" ")[0]
            if not trade_date:
                continue
            records.append(
                KlineRecord(
                    market="HK",
                    symbol=symbol,
                    period=period,
                    adjust=adjust,
                    trade_date=trade_date,
                    open_price=self._to_float(item.get("开盘")),
                    high_price=self._to_float(item.get("最高")),
                    low_price=self._to_float(item.get("最低")),
                    close_price=self._to_float(item.get("收盘")),
                    volume=self._to_float(item.get("成交量")),
                    amount=self._to_float(item.get("成交额")),
                    turnover_rate=self._to_float(item.get("换手率")),
                    raw_payload=self._json_safe(item),
                )
            )
        return records

    def _normalize_financial_dataframe(
        self, dataframe: pd.DataFrame, market: str, symbol: str, indicator: str
    ) -> list[FinancialRecord]:
        if dataframe.empty:
            return []
        records: list[FinancialRecord] = []
        for item in dataframe.to_dict(orient="records"):
            report_period = str(
                item.get("REPORT_DATE")
                or item.get("STD_REPORT_DATE")
                or item.get("报告期")
                or item.get("报告日期")
                or item.get("DATE")
                or ""
            )
            if not report_period:
                continue
            currency = item.get("CURRENCY") or item.get("货币") or item.get("币种")
            report_url = (
                item.get("url")
                or item.get("URL")
                or item.get("报告链接")
                or item.get("公告链接")
                or item.get("链接")
            )
            records.append(
                FinancialRecord(
                    market=market,
                    symbol=symbol,
                    indicator=indicator,
                    report_period=report_period,
                    currency=str(currency) if currency is not None else None,
                    data_json=self._json_safe(item),
                    url=str(report_url).strip() if report_url else None,
                )
            )
        return records

    def _normalize_notice_dataframe(self, dataframe: pd.DataFrame, market: str, symbol: str) -> list[NoticeRecord]:
        if dataframe.empty:
            return []
        records: list[NoticeRecord] = []
        seen_keys: set[tuple[str, str]] = set()
        for item in dataframe.to_dict(orient="records"):
            title = str(item.get("公告标题") or item.get("title") or item.get("NOTICE_TITLE") or "").strip()
            notice_date = str(item.get("公告日期") or item.get("date") or item.get("NOTICE_DATE") or "").strip()
            if not title or not notice_date:
                continue
            key = (notice_date, title)
            if key in seen_keys:
                continue
            seen_keys.add(key)
            records.append(
                NoticeRecord(
                    market=market,
                    symbol=symbol,
                    notice_date=notice_date,
                    title=title,
                    notice_type=str(item.get("公告类型") or item.get("notice_type") or "") or None,
                    url=str(item.get("公告链接") or item.get("url") or "") or None,
                    content_json=self._json_safe(item),
                )
            )
        return records

    def _fetch_hkex_notices(self, symbol: str, start_date: str, end_date: str) -> list[NoticeRecord]:
        stock_id = self._lookup_hkex_stock_id(symbol)
        if not stock_id:
            return []

        url = "https://www1.hkexnews.hk/search/titleSearchServlet.do"
        params = {
            "sortDir": "0",
            "sortByOptions": "DateTime",
            "category": "0",
            "market": "SEHK",
            "stockId": stock_id,
            "documentType": "",
            "fromDate": start_date,
            "toDate": end_date,
            "title": "",
            "searchType": "0",
            "t1code": "",
            "t2Gcode": "",
            "t2code": "",
            "rowRange": "100",
            "lang": "zh",
        }
        headers = {
            "User-Agent": "Mozilla/5.0",
            "Referer": "https://www1.hkexnews.hk/search/titlesearch.xhtml?lang=zh",
        }
        rows: list[dict[str, Any]] = []
        row_range = 100
        max_row_range = 1000
        with httpx.Client(timeout=20.0, headers=headers, trust_env=False) as client:
            while True:
                params["rowRange"] = str(row_range)
                response = client.get(url, params=params)
                response.raise_for_status()
                payload = response.json()

                result = (payload or {}).get("result")
                if isinstance(result, str):
                    try:
                        rows = json.loads(result or "[]")
                    except Exception:
                        return []
                else:
                    rows = result or []
                if not isinstance(rows, list):
                    return []

                record_cnt = self._safe_int(payload.get("recordCnt"))
                loaded_record = self._safe_int(payload.get("loadedRecord"))
                has_next_row = bool(payload.get("hasNextRow"))
                if not has_next_row or (record_cnt and loaded_record >= record_cnt) or row_range >= max_row_range:
                    break

                next_row_range = row_range + 100
                if record_cnt:
                    next_row_range = min(next_row_range, record_cnt)
                if next_row_range <= row_range:
                    break
                row_range = next_row_range
        return self._normalize_hkex_notice_rows(rows, symbol=symbol)

    def _normalize_hkex_notice_rows(self, rows: list[dict[str, Any]], symbol: str) -> list[NoticeRecord]:
        records: list[NoticeRecord] = []
        seen_keys: set[tuple[str, str]] = set()
        for row in rows:
            title = html.unescape(str(row.get("TITLE") or row.get("title") or "").strip())
            notice_date = self._parse_hkex_notice_date(str(row.get("DATE_TIME") or row.get("date_time") or ""))
            if not title or not notice_date:
                continue
            key = (notice_date, title)
            if key in seen_keys:
                continue
            seen_keys.add(key)

            file_link = str(row.get("FILE_LINK") or row.get("file_link") or "").strip()
            dod_web_path = str(row.get("DOD_WEB_PATH") or row.get("dod_web_path") or "").strip()
            url = f"https://www1.hkexnews.hk{file_link}" if file_link.startswith("/") else (file_link or dod_web_path or None)
            notice_type = self._clean_hkex_notice_type(
                row.get("SHORT_TEXT") or row.get("short_text") or row.get("LONG_TEXT") or row.get("long_text")
            )
            records.append(
                NoticeRecord(
                    market="HK",
                    symbol=symbol,
                    notice_date=notice_date,
                    title=title,
                    notice_type=notice_type,
                    url=url,
                    content_json=self._json_safe(row),
                )
            )
        return records

    @staticmethod
    @lru_cache(maxsize=1)
    def _hkex_stock_index() -> dict[str, str]:
        mapping: dict[str, str] = {}
        headers = {"User-Agent": "Mozilla/5.0"}
        for url in (
            "https://www1.hkexnews.hk/ncms/script/eds/activestock_sehk_e.json",
            "https://www1.hkexnews.hk/ncms/script/eds/inactivestock_sehk_e.json",
        ):
            try:
                with httpx.Client(timeout=30.0, headers=headers, trust_env=False) as client:
                    response = client.get(url)
                    response.raise_for_status()
                    rows = response.json()
            except Exception:
                continue
            if not isinstance(rows, list):
                continue
            for row in rows:
                if not isinstance(row, dict):
                    continue
                code = str(row.get("c") or "").strip().zfill(5)
                stock_id = str(row.get("i") or "").strip()
                if code and stock_id and code not in mapping:
                    mapping[code] = stock_id
        return mapping

    @classmethod
    def _lookup_hkex_stock_id(cls, symbol: str) -> str | None:
        normalized = cls._normalize_symbol(symbol, "HK")
        if not normalized:
            return None
        stock_id = cls._hkex_stock_index().get(normalized)
        if stock_id:
            return stock_id
        if normalized.isdigit():
            return cls._hkex_stock_index().get(normalized.lstrip("0").zfill(5))
        return None

    @staticmethod
    def _parse_hkex_notice_date(value: str) -> str | None:
        cleaned = value.strip()
        if not cleaned:
            return None
        for source_format in ("%d/%m/%Y %H:%M", "%d/%m/%Y", "%Y-%m-%d %H:%M", "%Y-%m-%d"):
            try:
                return datetime.strptime(cleaned, source_format).date().isoformat()
            except ValueError:
                continue
        return cleaned.split(" ", 1)[0] if cleaned else None

    @staticmethod
    def _clean_hkex_notice_type(value: Any) -> str | None:
        cleaned = re.sub(r"<br\s*/?>", "", str(value or ""), flags=re.IGNORECASE).strip()
        cleaned = html.unescape(cleaned)
        return cleaned or None

    def _fetch_realtime_quote_eastmoney(self, market: str, symbol: str) -> QuoteRecord:
        secid = f"1.{symbol}" if symbol.startswith("6") else f"0.{symbol}"
        if market == "HK":
            secid = f"116.{symbol}"
        url = (
            "https://push2.eastmoney.com/api/qt/stock/get"
            f"?secid={secid}&fields=f43,f44,f45,f46,f47,f48,f57,f58,f60,f86,f116,f117,f162,f167,f168,f169,f170,f171,f292"
        )
        with httpx.Client(
            trust_env=False,
            timeout=20.0,
            headers={"User-Agent": "Mozilla/5.0", "Referer": "https://quote.eastmoney.com/"},
        ) as client:
            response = client.get(url)
            response.raise_for_status()
            data = response.json()
        quote = (data or {}).get("data") or {}
        if not quote:
            raise ValueError(f"No realtime quote returned for {market}:{symbol}")
        total_market_cap = self._to_float(quote.get("f116"))
        float_market_cap = self._to_float(quote.get("f117"))
        # Eastmoney Push2 encodes PE/PB/turnover ratios in hundredths.
        pe_raw = self._to_float(quote.get("f162"))
        pb_raw = self._to_float(quote.get("f167"))
        pe_ratio = pe_raw / 100 if pe_raw is not None else None
        pb_ratio = pb_raw / 100 if pb_raw is not None else None
        volume_ratio = self._to_float(quote.get("f292"))
        return QuoteRecord(
            market=market,
            symbol=symbol,
            quote_time=None,
            current_price=self._eastmoney_price(quote.get("f43"), market),
            previous_close_price=self._eastmoney_price(quote.get("f60"), market),
            open_price=self._eastmoney_price(quote.get("f46"), market),
            high_price=self._eastmoney_price(quote.get("f44"), market),
            low_price=self._eastmoney_price(quote.get("f45"), market),
            volume=self._to_float(quote.get("f47")),
            amount=self._to_float(quote.get("f48")),
            change_amount=self._eastmoney_price(quote.get("f169"), market),
            change_pct=self._to_float(quote.get("f170")) / 100 if quote.get("f170") is not None else None,
            turnover_rate=self._to_float(quote.get("f168")) / 100 if quote.get("f168") is not None else None,
            raw_payload=self._json_safe(
                {
                    **quote,
                    "derived": {
                        "total_market_cap_yi": self._market_cap_to_yi(total_market_cap),
                        "float_market_cap_yi": self._market_cap_to_yi(float_market_cap),
                        "pe_ratio": pe_ratio,
                        "pb_ratio": pb_ratio,
                        "volume_ratio": volume_ratio,
                    },
                }
            ),
        )

    def _fetch_realtime_quote_tencent(self, market: str, symbol: str) -> QuoteRecord:
        code = self._tencent_code(market, symbol)
        url = f"https://qt.gtimg.cn/q={code}"
        with httpx.Client(trust_env=False, timeout=20.0, headers={"User-Agent": "Mozilla/5.0"}) as client:
            response = client.get(url)
            response.raise_for_status()
            text = response.text
        quote = self._parse_tencent_quote(text, market, symbol)
        if quote.current_price is None:
            raise ValueError(f"No realtime quote returned for {market}:{symbol}")
        return quote

    def _parse_tencent_quote(self, text: str, market: str, symbol: str) -> QuoteRecord:
        raw = text.split("=", 1)[-1].strip().strip(";").strip('"')
        parts = raw.split("~") if raw else []
        current_price = self._to_float(parts[3] if len(parts) > 3 else None)
        shares = self._to_float(parts[72] if len(parts) > 72 else None)
        total_market_cap = current_price * shares if current_price is not None and shares is not None else None
        float_market_cap = self._to_float(parts[44] if len(parts) > 44 else None)
        derived = {
            "total_market_cap_yi": self._market_cap_to_yi(total_market_cap),
            "float_market_cap_yi": self._market_cap_to_yi(float_market_cap),
            "pe_ratio": self._to_float(parts[39] if len(parts) > 39 else None),
            "pb_ratio": self._to_float(parts[46] if len(parts) > 46 else None),
            "volume_ratio": self._to_float(parts[49] if len(parts) > 49 else None),
        }
        return QuoteRecord(
            market=market,
            symbol=symbol,
            quote_time=self._normalize_quote_time(parts[30] if len(parts) > 30 and parts[30] else None),
            current_price=current_price,
            previous_close_price=self._to_float(parts[4] if len(parts) > 4 else None),
            open_price=self._to_float(parts[5] if len(parts) > 5 else None),
            high_price=self._to_float(parts[33] if len(parts) > 33 else None),
            low_price=self._to_float(parts[34] if len(parts) > 34 else None),
            volume=self._to_float(parts[6] if len(parts) > 6 else None),
            amount=self._parse_tencent_amount(parts, market),
            change_amount=self._to_float(parts[31] if len(parts) > 31 else None),
            change_pct=self._to_float(parts[32] if len(parts) > 32 else None),
            turnover_rate=self._to_float(parts[38] if market == "CN_A" and len(parts) > 38 else (parts[39] if len(parts) > 39 else None)),
            raw_payload={"raw": raw, "parts": parts, "derived": derived},
        )

    def _normalize_news_dataframe(self, dataframe: pd.DataFrame, market: str, symbol: str) -> list[NewsRecord]:
        if dataframe.empty:
            return []
        records: list[NewsRecord] = []
        seen_keys: set[tuple[str, str]] = set()
        for item in dataframe.to_dict(orient="records"):
            title = str(item.get("新闻标题") or item.get("title") or "").strip()
            news_time = str(item.get("发布时间") or item.get("time") or "").strip()
            if not title or not news_time:
                continue
            key = (news_time, title)
            if key in seen_keys:
                continue
            seen_keys.add(key)
            records.append(
                NewsRecord(
                    market=market,
                    symbol=symbol,
                    news_time=news_time,
                    title=title,
                    content=str(item.get("新闻内容") or item.get("content") or "").strip() or None,
                    source_name=str(item.get("文章来源") or item.get("source") or "").strip() or None,
                    url=str(item.get("新闻链接") or item.get("url") or "").strip() or None,
                    content_json=self._json_safe(item),
                )
            )
        return records

    def _fetch_cn_a_notices(self, symbol: str, start_date: str, end_date: str) -> list[NoticeRecord]:
        """Fetch A-share announcements from CNINFO official disclosure service."""
        security = self._lookup_cninfo_security(symbol)
        org_id = str((security or {}).get("orgId") or "").strip() or (self._fallback_cninfo_org_id(symbol) or "")
        if not org_id:
            return []
        column, plate = self._cninfo_market_params(symbol)
        url = "https://www.cninfo.com.cn/new/hisAnnouncement/query"
        base_params = {
            "stock": f"{symbol},{org_id}",
            "tabName": "fulltext",
            "pageSize": "30",
            "pageNum": "1",
            "column": column,
            "plate": plate,
            "seDate": f"{self._notice_date(start_date)}~{self._notice_date(end_date)}",
            "searchkey": "",
            "secid": "",
            "sortName": "",
            "sortType": "",
            "isHLtitle": "true",
        }
        headers = {"User-Agent": "Mozilla/5.0", "Referer": "https://www.cninfo.com.cn/"}
        records: list[NoticeRecord] = []
        with httpx.Client(timeout=20.0, headers=headers, trust_env=False) as client:
            first_response = client.post(url, data=base_params)
            first_response.raise_for_status()
            first_payload = first_response.json()
            data = first_payload or {}
            total_records = int(data.get("totalAnnouncement") or data.get("totalRecordNum") or 0)
            total_pages = min(max(1, (total_records + 29) // 30), 20)
            page_rows = data.get("announcements") or []
            for page in range(1, total_pages + 1):
                rows = page_rows if page == 1 else self._fetch_notice_page(client, url, base_params, page)
                records.extend(self._normalize_notice_rows(rows, market="CN_A", symbol=symbol))
        return records

    @staticmethod
    def _fetch_notice_page(
        client: httpx.Client,
        url: str,
        base_params: dict[str, str],
        page: int,
    ) -> list[dict[str, Any]]:
        params = {**base_params, "pageNum": str(page)}
        response = client.post(url, data=params)
        response.raise_for_status()
        return (response.json() or {}).get("announcements") or []

    def _normalize_notice_rows(
        self,
        rows: list[dict[str, Any]],
        market: str,
        symbol: str,
    ) -> list[NoticeRecord]:
        records: list[NoticeRecord] = []
        seen_keys: set[tuple[str, str]] = set()
        for row in rows:
            title = html.unescape(
                str(row.get("announcementTitle") or row.get("shortTitle") or row.get("title") or "").strip()
            )
            notice_date = self._parse_cninfo_notice_date(row.get("announcementTime"))
            if not title or not notice_date:
                continue
            key = (notice_date, title)
            if key in seen_keys:
                continue
            seen_keys.add(key)
            org_id = str(row.get("orgId") or "").strip() or self._fallback_cninfo_org_id(symbol) or ""
            announcement_id = str(row.get("announcementId") or "").strip()
            url = None
            if announcement_id and org_id:
                url = "https://www.cninfo.com.cn/new/disclosure/detail?" + urlencode(
                    {
                        "stockCode": symbol,
                        "orgId": org_id,
                        "announcementId": announcement_id,
                        "announcementTime": notice_date,
                    }
                )
            adjunct_url = str(row.get("adjunctUrl") or "").strip()
            if not url and adjunct_url:
                url = (
                    adjunct_url
                    if adjunct_url.startswith(("http://", "https://"))
                    else f"https://static.cninfo.com.cn/{adjunct_url.lstrip('/')}"
                )
            notice_type = str(row.get("announcementTypeName") or "公告").strip() or "公告"
            records.append(
                NoticeRecord(
                    market=market,
                    symbol=symbol,
                    notice_date=notice_date,
                    title=title,
                    notice_type=notice_type,
                    url=url,
                    content_json=self._json_safe(row),
                )
            )
        return records

    @classmethod
    @lru_cache(maxsize=2048)
    def _lookup_cninfo_security(cls, symbol: str) -> dict[str, Any] | None:
        normalized_symbol = cls._normalize_symbol(symbol, "CN_A")
        if not normalized_symbol:
            return None
        url = "https://www.cninfo.com.cn/new/information/topSearch/query"
        headers = {"User-Agent": "Mozilla/5.0", "Referer": "https://www.cninfo.com.cn/"}
        with httpx.Client(timeout=20.0, headers=headers, trust_env=False) as client:
            response = client.post(url, data={"keyWord": normalized_symbol})
            response.raise_for_status()
            payload = response.json()
        if not isinstance(payload, list):
            return None
        for item in payload:
            if isinstance(item, dict) and str(item.get("code") or "").zfill(6) == normalized_symbol:
                return item
        return None

    @staticmethod
    def _fallback_cninfo_org_id(symbol: str) -> str | None:
        normalized_symbol = AkshareAdapter._normalize_symbol(symbol, "CN_A")
        if not normalized_symbol:
            return None
        if normalized_symbol.startswith("6"):
            return f"gssh{normalized_symbol.zfill(7)}"
        if normalized_symbol.startswith(("0", "3")):
            return f"gssz{normalized_symbol.zfill(7)}"
        if normalized_symbol.startswith(("4", "8")):
            return f"gfbj{normalized_symbol}"
        return None

    @staticmethod
    def _cninfo_market_params(symbol: str) -> tuple[str, str]:
        normalized_symbol = AkshareAdapter._normalize_symbol(symbol, "CN_A")
        if normalized_symbol.startswith("6"):
            return "sse", "sh"
        if normalized_symbol.startswith(("4", "8")):
            return "bjse", "bj"
        return "szse", "sz"

    @staticmethod
    def _parse_cninfo_notice_date(value: Any) -> str | None:
        if value is None or value == "":
            return None
        if isinstance(value, (int, float)):
            try:
                return datetime.fromtimestamp(float(value) / 1000).date().isoformat()
            except (OverflowError, OSError, ValueError):
                return None
        cleaned = str(value).strip()
        if not cleaned:
            return None
        for source_format in (
            "%Y-%m-%d %H:%M:%S",
            "%Y-%m-%d %H:%M",
            "%Y-%m-%d",
            "%Y/%m/%d %H:%M:%S",
            "%Y/%m/%d",
        ):
            try:
                return datetime.strptime(cleaned, source_format).date().isoformat()
            except ValueError:
                continue
        return cleaned.split(" ", 1)[0]

    @staticmethod
    def _to_float(value: Any) -> float | None:
        if value is None or value == "":
            return None
        try:
            return float(value)
        except Exception:
            return None

    @classmethod
    def _eastmoney_price(cls, value: Any, market: str | None = None) -> float | None:
        parsed = cls._to_float(value)
        if parsed is None:
            return None
        return parsed / 1000 if market == "HK" else parsed / 100

    @staticmethod
    def _a_hist_tx_symbol(symbol: str) -> str:
        symbol = symbol.strip()
        if symbol.startswith(("sh", "sz", "bj")):
            return symbol
        if symbol.startswith("6"):
            return f"sh{symbol}"
        if symbol.startswith(("0", "3")):
            return f"sz{symbol}"
        return f"sz{symbol}"

    @staticmethod
    def _a_daily_symbol(symbol: str) -> str:
        return AkshareAdapter._a_hist_tx_symbol(symbol)

    @staticmethod
    def _tencent_code(market: str, symbol: str) -> str:
        clean_symbol = symbol.strip().upper()
        if market == "HK":
            return f"hk{clean_symbol}"
        if clean_symbol.startswith(("SH", "SZ", "BJ")):
            return clean_symbol.lower()
        if clean_symbol.startswith("6"):
            return f"sh{clean_symbol}"
        if clean_symbol.startswith(("0", "3", "4", "8")):
            return f"sz{clean_symbol}"
        return f"sz{clean_symbol}"

    @classmethod
    def _parse_tencent_amount(cls, parts: list[str], market: str) -> float | None:
        if market == "HK":
            return cls._to_float(parts[37] if len(parts) > 37 else None)
        if len(parts) > 35:
            candidate = parts[35]
            if "/" in candidate:
                amount_segment = candidate.split("/")
                if len(amount_segment) >= 3:
                    amount = cls._to_float(amount_segment[2])
                    if amount is not None:
                        return amount
        return cls._to_float(parts[37] if len(parts) > 37 else None)

    @staticmethod
    def _notice_date(value: str) -> str:
        stripped = value.replace("-", "").strip()
        if len(stripped) != 8:
            return value
        return f"{stripped[:4]}-{stripped[4:6]}-{stripped[6:]}"

    @staticmethod
    def _normalize_quote_time(value: str | None) -> str | None:
        if not value:
            return None
        for source_format in ("%Y%m%d%H%M%S", "%Y/%m/%d %H:%M:%S"):
            try:
                return datetime.strptime(value, source_format).isoformat()
            except ValueError:
                continue
        return value

    @staticmethod
    def _market_cap_to_yi(value: Any) -> float | None:
        numeric = AkshareAdapter._to_float(value)
        if numeric is None:
            return None
        return numeric / 100000000 if abs(numeric) > 100000000 else numeric

    @staticmethod
    def _coerce_report_date(value: Any) -> str | None:
        if value is None or value == "":
            return None
        if isinstance(value, (int, float)) and not pd.isna(value):
            parsed = pd.to_datetime(value, unit="ms", errors="coerce")
            if not pd.isna(parsed):
                return parsed.date().isoformat()
        parsed = pd.to_datetime(value, errors="coerce")
        if not pd.isna(parsed):
            return parsed.date().isoformat()
        cleaned = str(value).strip()
        return cleaned.split(" ", 1)[0] if cleaned else None

    @staticmethod
    def _find_column(dataframe: pd.DataFrame, candidates: tuple[str, ...]) -> str | None:
        lowered = {str(column).lower(): str(column) for column in dataframe.columns}
        for candidate in candidates:
            if candidate.lower() in lowered:
                return lowered[candidate.lower()]
        return None

    @staticmethod
    def _normalize_symbol(value: Any, market: str) -> str:
        symbol = str(value).strip()
        if market == "HK":
            return symbol.zfill(5) if symbol.isdigit() else symbol
        return symbol.zfill(6) if symbol.isdigit() else symbol

    @staticmethod
    def _infer_exchange(symbol: str, market: str) -> str:
        if market == "HK":
            return "HKEX"
        if symbol.startswith("6"):
            return "SSE"
        if symbol.startswith(("0", "3")):
            return "SZSE"
        if symbol.startswith(("4", "8")):
            return "BSE"
        return "CN_A"

    @staticmethod
    def _is_empty(value: Any) -> bool:
        return value is None or (isinstance(value, float) and pd.isna(value)) or str(value).strip() == ""

    @classmethod
    def _json_safe(cls, value: Any) -> Any:
        if isinstance(value, dict):
            return {str(key): cls._json_safe(item) for key, item in value.items()}
        if isinstance(value, list):
            return [cls._json_safe(item) for item in value]
        if isinstance(value, (datetime, date, pd.Timestamp)):
            return value.isoformat()
        if pd.isna(value):
            return None
        if hasattr(value, "item"):
            return value.item()
        return value
