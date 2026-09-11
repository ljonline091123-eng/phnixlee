from __future__ import annotations

from datetime import date, datetime, timedelta, timezone
from io import StringIO
import re
from typing import Any

import httpx
import pandas as pd
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.market_data import DataSource, StockSymbol


CNINFO_SOURCE = "AKSHARE_CNINFO_NEW_IPO"
THS_SOURCE = "AKSHARE_THS_IPO"
AASTOCKS_SOURCE = "AASTOCKS_HK_IPO"
AASTOCKS_IPO_URL = "https://www.aastocks.com/en/stocks/market/ipo/mainpage.aspx"


class IpoCalendarService:
    def __init__(self, db: Session | None = None):
        self.db = db

    def load(self, days: int = 7, today: date | None = None, persist: bool = True) -> dict[str, Any]:
        current_date = today or date.today()
        end_date = current_date + timedelta(days=days)
        items: list[dict[str, Any]] = []
        sources: list[dict[str, Any]] = []
        errors: list[str] = []

        try:
            source_items, total_count, message = self._fetch_cninfo_a_share_ipos(current_date, end_date)
            items.extend(source_items)
            sources.append(self._source_status(CNINFO_SOURCE, "CN_A", "SUCCESS", message, total_count, len(source_items)))
        except Exception as exc:
            message = f"{CNINFO_SOURCE} failed: {str(exc)[:500]}"
            errors.append(message)
            sources.append(self._source_status(CNINFO_SOURCE, "CN_A", "FAILED", message, 0, 0))
            try:
                source_items, total_count, message = self._fetch_ths_a_share_ipos(current_date, end_date)
                items.extend(source_items)
                sources.append(self._source_status(THS_SOURCE, "CN_A", "SUCCESS", message, total_count, len(source_items)))
            except Exception as fallback_exc:
                fallback_message = f"{THS_SOURCE} failed: {str(fallback_exc)[:500]}"
                errors.append(fallback_message)
                sources.append(self._source_status(THS_SOURCE, "CN_A", "FAILED", fallback_message, 0, 0))

        try:
            source_items, total_count, message = self._fetch_aastocks_hk_ipos(current_date, end_date)
            items.extend(source_items)
            sources.append(self._source_status(AASTOCKS_SOURCE, "HK", "SUCCESS", message, total_count, len(source_items)))
        except Exception as exc:
            message = f"{AASTOCKS_SOURCE} failed: {str(exc)[:500]}"
            errors.append(message)
            sources.append(self._source_status(AASTOCKS_SOURCE, "HK", "FAILED", message, 0, 0))

        normalized_items = self._dedupe_items(items)
        if persist and self.db is not None:
            self._upsert_ipo_symbols(normalized_items)

        return {
            "items": sorted(
                normalized_items,
                key=lambda item: (
                    item.get("apply_date") or "",
                    item.get("apply_end_date") or "",
                    item.get("market") or "",
                    item.get("symbol") or "",
                ),
            ),
            "sources": sources,
            "errors": errors,
            "window_start": current_date.isoformat(),
            "window_end": end_date.isoformat(),
            "updated_at": datetime.now(timezone.utc),
        }

    def _fetch_cninfo_a_share_ipos(self, today: date, end_date: date) -> tuple[list[dict[str, Any]], int, str]:
        frame = self._fetch_cninfo_frame_direct()
        message = "Cninfo 新股发行列表已读取"
        items = self._parse_cn_ipo_frame(frame, today, end_date, CNINFO_SOURCE)
        return items, len(frame), message

    def _fetch_cninfo_frame_direct(self) -> pd.DataFrame:
        import py_mini_racer
        from akshare.stock.stock_new_cninfo import _get_file_content_cninfo

        js_code = py_mini_racer.MiniRacer()
        js_code.eval(_get_file_content_cninfo("cninfo.js"))
        mcode = js_code.call("getResCode1")
        headers = {
            "Accept": "*/*",
            "Accept-Enckey": mcode,
            "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
            "Cache-Control": "no-cache",
            "Origin": "https://webapi.cninfo.com.cn",
            "Pragma": "no-cache",
            "Referer": "https://webapi.cninfo.com.cn/",
            "User-Agent": (
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126 Safari/537.36"
            ),
            "X-Requested-With": "XMLHttpRequest",
        }
        with httpx.Client(timeout=httpx.Timeout(12.0), headers=headers) as client:
            response = client.post(
                "https://webapi.cninfo.com.cn/api/sysapi/p_sysapi1097",
                params={"timetype": "36", "market": "ALL"},
            )
            response.raise_for_status()
        records = response.json().get("records") or []
        frame = pd.DataFrame(records)
        if frame.empty:
            return frame
        return frame.rename(
            columns={
                "SECCODE": "证劵代码",
                "SECNAME": "证券简称",
                "F006D": "上市日期",
                "F002D": "申购日期",
                "F008N": "发行价",
                "F003N": "总发行数量",
                "F013N": "发行市盈率",
                "F006N": "上网发行中签率",
                "F108D": "摇号结果公告日",
                "F109D": "中签公告日",
                "F037D": "中签缴款日",
                "F042N": "网上申购上限",
                "F043N": "上网发行数量",
            }
        )

    def _fetch_ths_a_share_ipos(self, today: date, end_date: date) -> tuple[list[dict[str, Any]], int, str]:
        frame = self._fetch_ths_frame_direct()
        items = self._parse_cn_ths_ipo_frame(frame, today, end_date)
        return items, len(frame), "同花顺新股申购列表已读取"

    def _fetch_ths_frame_direct(self) -> pd.DataFrame:
        from bs4 import BeautifulSoup

        headers = {
            "User-Agent": (
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126 Safari/537.36"
            ),
        }
        with httpx.Client(timeout=httpx.Timeout(12.0), headers=headers) as client:
            response = client.get("https://data.10jqka.com.cn/ipo/xgsgyzq/all/")
            response.raise_for_status()
        soup = BeautifulSoup(response.text, "lxml")
        table = soup.find("table", id="maintable") or soup.find("table", class_="m_table")
        if table is None:
            raise ValueError("同花顺新股表格结构不可用")
        headers_list = [cell.get_text(strip=True) for cell in table.find_all("th")]
        rows = []
        body = table.find("tbody") or table
        for row in body.find_all("tr"):
            cells = [cell.get_text(strip=True) for cell in row.find_all("td")]
            if cells:
                rows.append(cells)
        if not rows:
            return pd.DataFrame()
        if headers_list and len(headers_list) == len(rows[0]):
            return pd.DataFrame(rows, columns=headers_list)
        return pd.DataFrame(rows)

    def _fetch_aastocks_hk_ipos(self, today: date, end_date: date) -> tuple[list[dict[str, Any]], int, str]:
        headers = {
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
            "User-Agent": (
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126 Safari/537.36"
            ),
        }
        with httpx.Client(timeout=httpx.Timeout(12.0), follow_redirects=True, headers=headers) as client:
            response = client.get(AASTOCKS_IPO_URL)
            response.raise_for_status()
        tables = pd.read_html(StringIO(response.text))
        items: list[dict[str, Any]] = []
        total_count = 0
        for frame in tables:
            columns = [str(column) for column in frame.columns]
            if not any("Closing Date" in column for column in columns):
                continue
            total_count += len(frame)
            items.extend(self._parse_hk_ipo_frame(frame, today, end_date))
        if not items:
            return items, total_count, "AAStocks 当前未显示未来一周仍可公开认购的港股 IPO"
        return items, total_count, "AAStocks 港股公开招股列表已读取"

    def _parse_cn_ipo_frame(self, frame: pd.DataFrame, today: date, end_date: date, source: str) -> list[dict[str, Any]]:
        items: list[dict[str, Any]] = []
        for _, row in frame.iterrows():
            raw = self._clean_mapping(row.to_dict())
            symbol = self._normalize_a_symbol(self._first_text(raw, "证劵代码", "证券代码", fallback=self._series_text(row, 0)))
            name = self._first_text(raw, "证券简称", "股票简称", fallback=self._series_text(row, 1)) or symbol
            apply_date = self._parse_date(self._first_text(raw, "申购日期", fallback=self._series_text(row, 3)), today)
            if not symbol or not apply_date or not (today <= apply_date <= end_date):
                continue
            listing_date = self._parse_date(self._first_text(raw, "上市日期", fallback=self._series_text(row, 2)), today)
            item = self._base_item(
                market="CN_A",
                symbol=symbol,
                name=name,
                apply_date=apply_date,
                source=source,
                raw=raw,
            )
            item.update(
                {
                    "listing_date": listing_date.isoformat() if listing_date else None,
                    "price": self._first_text(raw, "发行价", "发行价格", fallback=self._series_text(row, 4)),
                    "issue_total": self._first_text(raw, "总发行数量", "发行总数（万股）", fallback=self._series_text(row, 5)),
                    "online_issue": self._first_text(raw, "上网发行数量", "网上发行（万股）", fallback=self._series_text(row, 12)),
                    "apply_limit": self._first_text(raw, "网上申购上限", "申购上限（万股）", fallback=self._series_text(row, 11)),
                    "pe_ratio": self._first_text(raw, "发行市盈率", fallback=self._series_text(row, 6)),
                    "detail_url": f"https://vip.stock.finance.sina.com.cn/corp/go.php/vISSUE_NewStock/stockid/{symbol}.phtml",
                }
            )
            items.append(item)
        return items

    def _parse_cn_ths_ipo_frame(self, frame: pd.DataFrame, today: date, end_date: date) -> list[dict[str, Any]]:
        items: list[dict[str, Any]] = []
        for _, row in frame.iterrows():
            raw = self._clean_mapping(row.to_dict())
            symbol = self._normalize_a_symbol(self._first_text(raw, "股票代码", fallback=self._series_text(row, 0)))
            name = self._first_text(raw, "股票简称", fallback=self._series_text(row, 1)) or symbol
            apply_date = self._parse_date(self._first_text(raw, "申购日期", fallback=self._series_text(row, 10)), today)
            if not symbol or not apply_date or not (today <= apply_date <= end_date):
                continue
            listing_date = self._parse_date(self._first_text(raw, "上市日期", fallback=self._series_text(row, 14)), today)
            item = self._base_item(
                market="CN_A",
                symbol=symbol,
                name=name,
                apply_date=apply_date,
                source=THS_SOURCE,
                raw=raw,
            )
            item.update(
                {
                    "listing_date": listing_date.isoformat() if listing_date else None,
                    "price": self._first_text(raw, "发行价格", fallback=self._series_text(row, 7)),
                    "issue_total": self._first_text(raw, "发行总数（万股）", fallback=self._series_text(row, 3)),
                    "online_issue": self._first_text(raw, "网上发行（万股）", fallback=self._series_text(row, 4)),
                    "apply_limit": self._first_text(raw, "申购上限（万股）", fallback=self._series_text(row, 5)),
                    "pe_ratio": self._first_text(raw, "发行市盈率", fallback=self._series_text(row, 8)),
                    "detail_url": f"https://vip.stock.finance.sina.com.cn/corp/go.php/vISSUE_NewStock/stockid/{symbol}.phtml",
                }
            )
            items.append(item)
        return items

    def _parse_hk_ipo_frame(self, frame: pd.DataFrame, today: date, end_date: date) -> list[dict[str, Any]]:
        items: list[dict[str, Any]] = []
        for _, row in frame.iterrows():
            raw = self._clean_mapping(row.to_dict())
            name_code = self._first_text(raw, "Name/Code", "Name▼ / Code▼", "Name▼ / Code▲", fallback=self._series_text(row, 0))
            if not name_code or re.search(r"\bno\s+(public|upcoming)", name_code, re.IGNORECASE):
                continue
            match = re.search(r"(?P<symbol>\d{4,5})\.HK", name_code, re.IGNORECASE)
            if not match:
                continue
            symbol = self._normalize_hk_symbol(match.group("symbol"))
            name = self._clean_hk_name(name_code[: match.start()]) or symbol
            closing_date = self._parse_date(self._first_text(raw, "Closing Date"), today)
            listing_date = self._parse_date(self._first_text(raw, "Listing Date", "Listing Date▲"), today)
            if not closing_date or closing_date < today or closing_date > end_date:
                continue
            item = self._base_item(
                market="HK",
                symbol=symbol,
                name=name,
                apply_date=today,
                source=AASTOCKS_SOURCE,
                raw=raw,
            )
            item.update(
                {
                    "apply_end_date": closing_date.isoformat(),
                    "listing_date": listing_date.isoformat() if listing_date else None,
                    "price": self._first_text(raw, "Offer Price", "Offer Price4", "Listing Price"),
                    "lot_size": self._first_text(raw, "Lot Size"),
                    "entry_fee": self._first_text(raw, "Entry Fee"),
                    "industry": self._first_text(raw, "Industry"),
                    "detail_url": f"https://www.aastocks.com/en/stocks/market/ipo/upcomingipo/ipo-info?symbol={symbol}#info",
                }
            )
            items.append(item)
        return items

    def _upsert_ipo_symbols(self, items: list[dict[str, Any]]) -> None:
        if self.db is None or not items:
            return
        source = self.db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE"))
        if not source:
            return
        for item in items:
            market = str(item["market"])
            symbol = str(item["symbol"])
            existing = self.db.scalar(select(StockSymbol).where(StockSymbol.market == market, StockSymbol.symbol == symbol))
            snapshot = self._ipo_snapshot(item)
            if existing:
                if not existing.name or existing.name == existing.symbol:
                    existing.name = str(item.get("name") or existing.name)
                if item.get("listing_date") and not existing.list_date:
                    existing.list_date = str(item["listing_date"])
                if existing.status != "LISTED":
                    existing.status = "IPO"
                    existing.asset_type = "IPO"
                ext_json = dict(existing.ext_json or {})
                ext_json["ipo_calendar"] = snapshot
                existing.ext_json = ext_json
                raw_payload = dict(existing.raw_payload or {})
                raw_payload["ipo_calendar"] = item.get("raw") or {}
                raw_payload["ipo_source"] = item.get("source")
                existing.raw_payload = raw_payload
                existing.last_synced_at = datetime.now(timezone.utc)
                continue

            self.db.add(
                StockSymbol(
                    market=market,
                    symbol=symbol,
                    exchange=self._infer_exchange(market, symbol),
                    name=str(item.get("name") or symbol),
                    asset_type="IPO",
                    status="IPO",
                    list_date=item.get("listing_date"),
                    source_id=source.id,
                    ext_json={"ipo_calendar": snapshot},
                    raw_payload={"ipo_calendar": item.get("raw") or {}, "ipo_source": item.get("source")},
                    last_synced_at=datetime.now(timezone.utc),
                )
            )
        self.db.commit()

    @staticmethod
    def _base_item(
        market: str,
        symbol: str,
        name: str,
        apply_date: date,
        source: str,
        raw: dict[str, Any],
    ) -> dict[str, Any]:
        return {
            "market": market,
            "symbol": symbol,
            "name": name,
            "apply_date": apply_date.isoformat(),
            "apply_end_date": None,
            "listing_date": None,
            "price": None,
            "lot_size": None,
            "entry_fee": None,
            "industry": None,
            "sponsor": None,
            "prospectus_url": None,
            "detail_url": None,
            "issue_total": None,
            "online_issue": None,
            "apply_limit": None,
            "pe_ratio": None,
            "raw": raw,
            "source": source,
        }

    @staticmethod
    def _source_status(source: str, market: str, status: str, message: str, total_count: int, item_count: int) -> dict[str, Any]:
        return {
            "source": source,
            "market": market,
            "status": status,
            "message": message,
            "total_count": total_count,
            "item_count": item_count,
            "fetched_at": datetime.now(timezone.utc),
        }

    @staticmethod
    def _clean_value(value: Any) -> str | None:
        if value is None:
            return None
        try:
            if pd.isna(value):
                return None
        except (TypeError, ValueError):
            pass
        text = str(value).replace("\xa0", " ").strip()
        text = re.sub(r"\s+", " ", text)
        if text.lower() in {"", "nan", "nat", "none", "-", "--"}:
            return None
        return text

    @classmethod
    def _clean_mapping(cls, mapping: dict[Any, Any]) -> dict[str, Any]:
        cleaned: dict[str, Any] = {}
        for key, value in mapping.items():
            text = cls._clean_value(value)
            if text is not None:
                cleaned[str(key)] = text
        return cleaned

    @classmethod
    def _first_text(cls, mapping: dict[str, Any], *keys: str, fallback: str | None = None) -> str | None:
        for key in keys:
            if key in mapping:
                value = cls._clean_value(mapping[key])
                if value is not None:
                    return value
        return cls._clean_value(fallback)

    @classmethod
    def _series_text(cls, row: pd.Series, index: int) -> str | None:
        try:
            return cls._clean_value(row.iloc[index])
        except IndexError:
            return None

    @staticmethod
    def _parse_date(value: Any, today: date) -> date | None:
        text = IpoCalendarService._clean_value(value)
        if not text:
            return None
        full_match = re.search(r"(20\d{2})[-/](\d{1,2})[-/](\d{1,2})", text)
        if full_match:
            year, month, day = (int(part) for part in full_match.groups())
            try:
                return date(year, month, day)
            except ValueError:
                return None
        short_match = re.search(r"(?<!\d)(\d{1,2})[-/](\d{1,2})(?!\d)", text)
        if not short_match:
            return None
        month, day = (int(part) for part in short_match.groups())
        year = today.year
        try:
            parsed = date(year, month, day)
        except ValueError:
            return None
        if today.month == 12 and month == 1:
            return date(year + 1, month, day)
        return parsed

    @staticmethod
    def _normalize_a_symbol(value: str | None) -> str:
        text = (value or "").strip().upper()
        return text.zfill(6) if text.isdigit() else text

    @staticmethod
    def _normalize_hk_symbol(value: str | None) -> str:
        text = (value or "").strip().upper().replace(".HK", "")
        return text.zfill(5) if text.isdigit() else text

    @staticmethod
    def _clean_hk_name(value: str) -> str:
        text = re.sub(r"\s+", " ", value.replace("▼", "").replace("▲", "")).strip()
        text = re.sub(r"(Grey Market Today|Detail Quote)$", "", text, flags=re.IGNORECASE).strip()
        return text

    @staticmethod
    def _infer_exchange(market: str, symbol: str) -> str:
        if market == "HK":
            return "HKEX"
        if symbol.startswith(("43", "83", "87", "88", "92")):
            return "BJSE"
        if symbol.startswith("6"):
            return "SSE"
        if symbol.startswith(("0", "3")):
            return "SZSE"
        return "CN"

    @staticmethod
    def _ipo_snapshot(item: dict[str, Any]) -> dict[str, Any]:
        return {key: value for key, value in item.items() if key != "raw"}

    @staticmethod
    def _dedupe_items(items: list[dict[str, Any]]) -> list[dict[str, Any]]:
        preferred_source = {CNINFO_SOURCE: 3, THS_SOURCE: 2, AASTOCKS_SOURCE: 3}
        by_key: dict[tuple[str, str, str], dict[str, Any]] = {}
        for item in items:
            key = (str(item.get("market") or ""), str(item.get("symbol") or ""), str(item.get("apply_date") or ""))
            current = by_key.get(key)
            if current is None:
                by_key[key] = item
                continue
            current_score = preferred_source.get(str(current.get("source")), 0)
            incoming_score = preferred_source.get(str(item.get("source")), 0)
            if incoming_score > current_score:
                merged = {**item}
                for field, value in current.items():
                    if merged.get(field) in (None, "") and value not in (None, ""):
                        merged[field] = value
                by_key[key] = merged
            else:
                for field, value in item.items():
                    if current.get(field) in (None, "") and value not in (None, ""):
                        current[field] = value
        return list(by_key.values())
