from __future__ import annotations

from datetime import datetime
import os
from typing import Any

from app.connectors.base import (
    KlineRecord,
    MarketDataAdapter,
    QuoteRecord,
    SymbolRecord,
)


class PytdxAdapter(MarketDataAdapter):
    """Optional TongdaXin (PyTDX) adapter for mainland quote/K-line data.

    PyTDX is an行情 transport rather than a disclosure provider: it does not
    replace CNINFO, HKEXnews, or NEEQ announcement/financial-report sources.
    The dependency is imported lazily so an uninstalled optional provider does
    not prevent the rest of the data platform from starting.
    """

    adapter_type = "PYTDX"
    default_hosts = (
        ("119.147.212.81", 7709),
        ("101.227.73.20", 7709),
        ("14.17.75.71", 7709),
    )

    @staticmethod
    def _api_class():
        try:
            from pytdx.hq import TdxHq_API
        except ImportError:
            return None
        return TdxHq_API

    def health_check(self) -> tuple[str, list[str]]:
        if self._api_class() is None:
            return (
                "PyTDX 未安装；安装 pytdx 并配置可访问的通达信行情节点后启用。",
                [],
            )
        try:
            api = self._connect()
            self._disconnect(api)
        except Exception as exc:
            return (
                f"PyTDX 已安装，但当前通达信节点不可连接：{str(exc)[:180]}",
                [],
            )
        return (
            "PyTDX 已安装，可提供 A 股实时行情与历史 K 线；不提供公告、财报或港股数据。",
            ["QUOTE_ON_DEMAND", "A_KLINE_ON_DEMAND"],
        )

    def fetch_symbol_master(self, market: str) -> list[SymbolRecord]:
        raise NotImplementedError(
            "PyTDX 仅用于 A 股行情/K 线，股票主数据请使用 CNINFO/AKSHARE"
        )

    def fetch_kline(
        self,
        market: str,
        symbol: str,
        period: str,
        start_date: str,
        end_date: str,
        adjust: str,
    ) -> list[KlineRecord]:
        self._ensure_cn_a(market)
        if period not in {"daily", "D", "1d"}:
            raise ValueError("PyTDX 当前仅支持日线 K 线")
        api = self._connect()
        try:
            category = 9  # TDX daily bars
            records: list[KlineRecord] = []
            start = 0
            batch_size = 800
            while True:
                rows = api.get_security_bars(
                    category,
                    self._tdx_market(symbol),
                    self._clean_symbol(symbol),
                    start,
                    batch_size,
                )
                if not rows:
                    break
                for row in rows:
                    trade_date = self._date_text(row.get("datetime") or row.get("date"))
                    if not trade_date or trade_date < start_date[:10].replace("-", ""):
                        continue
                    if end_date and trade_date > end_date[:10].replace("-", ""):
                        continue
                    records.append(
                        KlineRecord(
                            market="CN_A",
                            symbol=self._clean_symbol(symbol),
                            period="daily",
                            adjust=adjust,
                            trade_date=f"{trade_date[:4]}-{trade_date[4:6]}-{trade_date[6:]}",
                            open_price=self._number(row.get("open")),
                            high_price=self._number(row.get("high")),
                            low_price=self._number(row.get("low")),
                            close_price=self._number(row.get("close")),
                            volume=self._number(row.get("vol")),
                            amount=self._number(row.get("amount")),
                            turnover_rate=None,
                            raw_payload=dict(row),
                        )
                    )
                if len(rows) < batch_size:
                    break
                start += len(rows)
            return sorted(records, key=lambda item: item.trade_date)
        finally:
            self._disconnect(api)

    def fetch_realtime_quote(self, market: str, symbol: str) -> QuoteRecord:
        self._ensure_cn_a(market)
        api = self._connect()
        try:
            rows = api.get_security_quotes(
                [(self._tdx_market(symbol), self._clean_symbol(symbol))]
            )
            if not rows:
                raise RuntimeError(f"PyTDX 未返回 {symbol} 的行情")
            row = rows[0]
            current = self._number(row.get("price"))
            previous = self._number(row.get("last_close"))
            change_amount = (
                current - previous
                if current is not None and previous is not None
                else None
            )
            change_pct = (
                change_amount / previous * 100
                if change_amount is not None and previous not in (None, 0)
                else None
            )
            quote_time = row.get("datetime") or row.get("time")
            return QuoteRecord(
                market="CN_A",
                symbol=self._clean_symbol(symbol),
                quote_time=str(quote_time) if quote_time else None,
                current_price=current,
                previous_close_price=previous,
                open_price=self._number(row.get("open")),
                high_price=self._number(row.get("high")),
                low_price=self._number(row.get("low")),
                volume=self._number(row.get("vol")),
                amount=self._number(row.get("amount")),
                change_amount=change_amount,
                change_pct=change_pct,
                turnover_rate=None,
                raw_payload=dict(row),
            )
        finally:
            self._disconnect(api)

    @staticmethod
    def _ensure_cn_a(market: str) -> None:
        if str(market).upper() != "CN_A":
            raise ValueError("PyTDX 仅支持 A 股行情与 K 线")

    def _connect(self):
        api_class = self._api_class()
        if api_class is None:
            raise RuntimeError("未安装 pytdx，请执行 pip install pytdx")
        api = api_class()
        last_error: Exception | None = None
        for host, port in self._hosts():
            try:
                if api.connect(host, port, time_out=3):
                    return api
            except Exception as exc:  # pragma: no cover - depends on remote node
                last_error = exc
        if last_error:
            raise RuntimeError(f"PyTDX 节点连接失败: {last_error}") from last_error
        raise RuntimeError("PyTDX 节点连接失败，请检查网络或配置节点")

    @classmethod
    def _hosts(cls) -> tuple[tuple[str, int], ...]:
        """Read optional ``host:port`` pairs from PYTDX_HOSTS.

        Example: ``PYTDX_HOSTS=119.147.212.81:7709,101.227.73.20:7709``.
        Invalid entries are ignored and the built-in public nodes remain as a
        fallback.
        """
        raw = os.getenv("PYTDX_HOSTS", "").strip()
        if not raw:
            return cls.default_hosts
        parsed: list[tuple[str, int]] = []
        for item in raw.replace(";", ",").split(","):
            host, separator, port = item.strip().partition(":")
            if not separator or not host:
                continue
            try:
                parsed.append((host, int(port)))
            except ValueError:
                continue
        return tuple(parsed) or cls.default_hosts

    @staticmethod
    def _disconnect(api) -> None:
        try:
            api.disconnect()
        except Exception:
            pass

    @staticmethod
    def _clean_symbol(symbol: str) -> str:
        return str(symbol).strip().upper().zfill(6)

    @classmethod
    def _tdx_market(cls, symbol: str) -> int:
        code = cls._clean_symbol(symbol)
        # Shanghai: 5/6/688 and Shenzhen: 0/1/2/3.
        return 1 if code.startswith(("5", "6", "688")) else 0

    @staticmethod
    def _number(value: Any) -> float | None:
        try:
            if value in (None, ""):
                return None
            return float(value)
        except (TypeError, ValueError):
            return None

    @staticmethod
    def _date_text(value: Any) -> str | None:
        text = str(value or "").strip().replace("/", "-")
        if not text:
            return None
        for candidate in (text[:10], text[:8]):
            try:
                parsed = datetime.strptime(candidate, "%Y-%m-%d")
                return parsed.strftime("%Y%m%d")
            except ValueError:
                pass
            try:
                parsed = datetime.strptime(candidate, "%Y%m%d")
                return parsed.strftime("%Y%m%d")
            except ValueError:
                pass
        return None
