from __future__ import annotations

import json
from dataclasses import asdict
from datetime import date, datetime, timedelta, timezone
from typing import Any

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.markets import MARKET_CN_A, MARKET_NEEQ, MARKET_NEEQ_INNOVATION
from app.connectors.base import FinancialRecord, KlineRecord, NewsRecord, NoticeRecord, QuoteRecord
from app.connectors.registry import get_adapter
from app.models.market_data import (
    DataFetchLog,
    DataSource,
    StockF10Cache,
    StockNews,
    StockRealtimeQuote,
    StockFinancialReport,
    StockKline,
    StockNotice,
    StockSymbol,
)


class OnDemandFetchError(RuntimeError):
    def __init__(self, message: str, fetch_log_id: int):
        super().__init__(message)
        self.fetch_log_id = fetch_log_id


def _parse_timestamp(value: Any) -> datetime | None:
    if isinstance(value, datetime):
        parsed = value
    else:
        text = str(value or "").strip()
        if not text:
            return None
        text = text.replace("Z", "+00:00").replace("/", "-")
        parsed = None
        for candidate in (text, text[:19], text[:10]):
            try:
                parsed = datetime.fromisoformat(candidate)
                break
            except ValueError:
                continue
        if parsed is None:
            for fmt in ("%d-%m-%Y %H:%M", "%Y%m%d"):
                try:
                    parsed = datetime.strptime(text, fmt)
                    break
                except ValueError:
                    continue
        if parsed is None:
            return None
    return parsed.replace(tzinfo=parsed.tzinfo or timezone.utc)


F10_DATE_KEYS = (
    "date",
    "日期",
    "trade_date",
    "TRADE_DATE",
    "持股日期",
    "report_date",
    "REPORT_DATE",
    "报告日期",
    "报告期",
    "notice_date",
    "NOTICE_DATE",
    "公告日期",
    "update_date",
    "UPDATE_DATE",
    "更新日期",
    "更新时间",
    "截止日期",
)

F10_SECTION_REQUIRES_FRESHNESS = {
    "published_reports",
    "fund_flow",
    "financial_summary",
    "financial_statements",
    "business_composition",
}


def _latest_timestamp(*values: Any) -> datetime | None:
    latest: datetime | None = None
    for value in values:
        if isinstance(value, dict):
            parsed = _latest_timestamp(*value.values())
        elif isinstance(value, (list, tuple, set)):
            parsed = _latest_timestamp(*value)
        else:
            parsed = _parse_timestamp(value)
        if parsed and (latest is None or parsed > latest):
            latest = parsed
    return latest


def _timestamp_from_mapping(mapping: Any, keys: tuple[str, ...]) -> datetime | None:
    if not isinstance(mapping, dict):
        return None
    lowered = {str(key).lower(): value for key, value in mapping.items()}
    values: list[Any] = []
    for key in keys:
        if key in mapping:
            values.append(mapping[key])
        lower_key = key.lower()
        if lower_key in lowered:
            values.append(lowered[lower_key])
    return _latest_timestamp(*values)


def _latest_timestamp_from_rows(rows: Any, keys: tuple[str, ...] = F10_DATE_KEYS) -> datetime | None:
    if not isinstance(rows, list):
        return None
    return _latest_timestamp(*(_timestamp_from_mapping(row, keys) if isinstance(row, dict) else row for row in rows))


def _f10_payload_latest_timestamp(section: str, payload: dict[str, Any] | None) -> datetime | None:
    if not isinstance(payload, dict):
        return None

    if section == "profile":
        fields = payload.get("fields") if isinstance(payload.get("fields"), dict) else {}
        return _timestamp_from_mapping(
            {**payload, **fields},
            ("updated_at", "last_updated_at", "update_time", "更新时间", "更新日期", "UPDATE_DATE"),
        )

    if section == "holders":
        return _latest_timestamp(
            _latest_timestamp_from_rows(payload.get("major")),
            _latest_timestamp_from_rows(payload.get("circulating")),
            _latest_timestamp_from_rows(payload.get("rows")),
        )

    if section == "fund_flow":
        return _latest_timestamp_from_rows(payload.get("rows"))

    if section == "published_reports":
        return _latest_timestamp_from_rows(payload.get("reports"))

    if section == "financial_summary":
        period_values = list(payload.get("periods") or []) if isinstance(payload.get("periods"), list) else []
        for row in payload.get("rows") or []:
            if not isinstance(row, dict):
                continue
            values = row.get("数据") or row.get("data")
            if isinstance(values, dict):
                period_values.extend(values.keys())
        return _latest_timestamp(*period_values)

    if section == "financial_statements":
        candidates: list[Any] = []
        for statement_key in ("balance_sheet", "income_statement", "cash_flow"):
            statement = payload.get(statement_key)
            if not isinstance(statement, dict):
                continue
            candidates.append(_latest_timestamp_from_rows(statement.get("periods")))
            candidates.append(_latest_timestamp_from_rows(statement.get("rows")))
        return _latest_timestamp(*candidates)

    if section == "business_composition":
        return _latest_timestamp(
            payload.get("report_date"),
            payload.get("REPORT_DATE"),
            _latest_timestamp_from_rows(payload.get("sections")),
        )

    return _timestamp_from_mapping(payload, F10_DATE_KEYS)


def _incoming_f10_payload_is_stale(section: str, existing: dict[str, Any] | None, incoming: dict[str, Any]) -> bool:
    existing_latest = _f10_payload_latest_timestamp(section, existing)
    incoming_latest = _f10_payload_latest_timestamp(section, incoming)
    if existing_latest and incoming_latest:
        return incoming_latest < existing_latest
    if existing_latest and not incoming_latest and section in F10_SECTION_REQUIRES_FRESHNESS:
        return True
    return False


def _row_key(row: Any, keys: tuple[str, ...]) -> str:
    if isinstance(row, dict):
        for key in keys:
            value = row.get(key)
            if value not in (None, ""):
                return f"{key}:{value}"
    return json.dumps(row, ensure_ascii=False, sort_keys=True, default=str)


def _merge_rows(
    existing_rows: Any,
    incoming_rows: Any,
    keys: tuple[str, ...],
) -> list[Any]:
    merged: dict[str, Any] = {}
    for row in existing_rows if isinstance(existing_rows, list) else []:
        merged[_row_key(row, keys)] = row
    for row in incoming_rows if isinstance(incoming_rows, list) else []:
        merged[_row_key(row, keys)] = row
    return list(merged.values())


def _merge_f10_payload(
    section: str,
    existing: dict[str, Any] | None,
    incoming: dict[str, Any],
) -> dict[str, Any]:
    if not existing:
        return incoming
    if not incoming:
        return existing
    if _incoming_f10_payload_is_stale(section, existing, incoming):
        if section != "published_reports":
            return existing
        existing_reports = existing.get("reports") if isinstance(existing, dict) else []
        incoming_reports = incoming.get("reports") if isinstance(incoming, dict) else []
        existing_has_formal = any(
            isinstance(row, dict)
            and str(row.get("report_type") or "").lower()
            not in {"financial_indicators", "announcement"}
            for row in (existing_reports or [])
        )
        incoming_has_formal = any(
            isinstance(row, dict)
            and str(row.get("report_type") or "").lower()
            not in {"financial_indicators", "announcement"}
            for row in (incoming_reports or [])
        )
        if not (incoming_has_formal and not existing_has_formal):
            return existing

    merged = dict(existing)
    merged.update({key: value for key, value in incoming.items() if value not in (None, "", [], {})})
    if "message" in incoming and incoming.get("message") is not None:
        # A successful refresh may intentionally clear an older transient
        # provider-error message while retaining the existing data rows.
        merged["message"] = incoming.get("message") or ""

    if section == "profile":
        fields = dict(existing.get("fields") or {})
        fields.update({key: value for key, value in (incoming.get("fields") or {}).items() if value not in (None, "")})
        merged["fields"] = fields
        return merged

    if section == "published_reports":
        # Formal-report cache must never contain the synthetic financial
        # indicator snapshot (or the old announcement fallback).  Those rows
        # belong to the financial/notice sections respectively.
        existing_reports = [
            row
            for row in (existing.get("reports") or [])
            if isinstance(row, dict)
            and str(row.get("report_type") or "").lower()
            not in {"financial_indicators", "announcement"}
        ]
        incoming_reports = [
            row
            for row in (incoming.get("reports") or [])
            if isinstance(row, dict)
            and str(row.get("report_type") or "").lower()
            not in {"financial_indicators", "announcement"}
        ]
        reports_by_key: dict[tuple[str, str, str], dict[str, Any]] = {}
        for row in existing_reports:
            key = (
                str(row.get("report_date") or row.get("notice_date") or "")[:10],
                str(row.get("report_type") or "financial_report"),
                str(row.get("title") or row.get("report_name") or ""),
            )
            if key[2]:
                reports_by_key[key] = row
        for row in incoming_reports:
            key = (
                str(row.get("report_date") or row.get("notice_date") or "")[:10],
                str(row.get("report_type") or "financial_report"),
                str(row.get("title") or row.get("report_name") or ""),
            )
            if not key[2]:
                continue
            current = reports_by_key.get(key)
            if current is None or (row.get("url") and not current.get("url")):
                reports_by_key[key] = row
            elif row.get("url") and current.get("source_name") != row.get("source_name"):
                # Prefer the newly fetched official source when both entries
                # describe the same filing.
                reports_by_key[key] = {**current, **row}
        merged["reports"] = list(reports_by_key.values())
        merged["reports"].sort(
            key=lambda row: (
                str(row.get("report_date") or "") if isinstance(row, dict) else "",
                str(row.get("notice_date") or "") if isinstance(row, dict) else "",
            ),
            reverse=True,
        )
        return merged

    if section == "financial_summary":
        merged["periods"] = sorted(
            {
                str(period)
                for period in [*(existing.get("periods") or []), *(incoming.get("periods") or [])]
                if str(period).strip()
            },
            reverse=True,
        )
        existing_rows = {str(row.get("指标") or row.get("metric") or ""): row for row in existing.get("rows") or [] if isinstance(row, dict)}
        for row in incoming.get("rows") or []:
            if not isinstance(row, dict):
                continue
            key = str(row.get("指标") or row.get("metric") or "")
            if not key:
                continue
            current = dict(existing_rows.get(key) or {})
            values = dict(current.get("数据") or current.get("data") or {})
            values.update(row.get("数据") or row.get("data") or {})
            current.update(row)
            if values:
                current["数据"] = values
            existing_rows[key] = current
        merged["rows"] = list(existing_rows.values())
        return merged

    if section == "financial_statements":
        for statement_key in ("balance_sheet", "income_statement", "cash_flow"):
            old_statement = existing.get(statement_key) or {}
            new_statement = incoming.get(statement_key) or {}
            if not isinstance(old_statement, dict) or not isinstance(new_statement, dict):
                continue
            statement = dict(old_statement)
            statement.update({key: value for key, value in new_statement.items() if value not in (None, "", [], {})})
            statement["periods"] = _merge_rows(
                old_statement.get("periods"),
                new_statement.get("periods"),
                ("报告日期", "report_date", "报告名称", "report_name"),
            )
            merged[statement_key] = statement
        return merged

    if section == "fund_flow":
        incoming_rows = incoming.get("rows")
        if isinstance(incoming_rows, list) and incoming_rows:
            merged["rows"] = incoming_rows
        elif existing.get("rows"):
            merged["rows"] = existing["rows"]
        return merged

    if section == "business_composition":
        incoming_date = str(incoming.get("report_date") or "")
        existing_date = str(existing.get("report_date") or "")
        if existing_date and incoming_date and incoming_date < existing_date:
            return existing
        return merged

    return merged


class StockOnDemandService:
    def __init__(self, db: Session):
        self.db = db

    def _touch_stock_last_synced(self, market: str, symbol: str) -> None:
        stock = self.db.scalar(
            select(StockSymbol).where(
                StockSymbol.market == market,
                StockSymbol.symbol == symbol,
            )
        )
        if not stock:
            return
        stock.last_synced_at = datetime.now(timezone.utc)
        self.db.commit()

    def refresh_stock_data(
        self,
        source: DataSource,
        market: str,
        symbol: str,
        list_date: str | None = None,
    ) -> list[str]:
        """Refresh all core stock data and persist each successful section."""
        errors: list[str] = []
        today = date.today()
        normalized_list_date = str(list_date or "").replace("-", "").strip()
        if len(normalized_list_date) != 8 or not normalized_list_date.isdigit():
            normalized_list_date = (today - timedelta(days=3650)).strftime("%Y%m%d")

        operations = (
            (
                "quote",
                lambda: self.fetch_quote(source, market, symbol, persist=True),
            ),
            (
                "kline",
                lambda: self.fetch_kline(
                    source=source,
                    market=market,
                    symbol=symbol,
                    period="daily",
                    adjust="",
                    start_date=normalized_list_date,
                    end_date=today.strftime("%Y%m%d"),
                    persist=True,
                ),
            ),
            (
                "financials",
                lambda: self.fetch_financials(
                    source=source,
                    market=market,
                    symbol=symbol,
                    indicator="按报告期" if market == "CN_A" else "报告期",
                    persist=True,
                ),
            ),
            (
                "news",
                lambda: self.fetch_news(source, market, symbol, persist=True),
            ),
            (
                "notices",
                lambda: self.fetch_notices(
                    source=source,
                    market=market,
                    symbol=symbol,
                    start_date=(today - timedelta(days=3650)).strftime("%Y%m%d"),
                    end_date=today.strftime("%Y%m%d"),
                    persist=True,
                ),
            ),
        )
        for section, operation in operations:
            try:
                operation()
            except Exception as exc:
                errors.append(f"{section}: {str(exc)[:240]}")
        return errors

    def fetch_kline(
        self,
        source: DataSource,
        market: str,
        symbol: str,
        period: str,
        adjust: str,
        start_date: str,
        end_date: str,
        persist: bool,
    ) -> tuple[DataFetchLog, list[dict[str, Any]]]:
        log = self._start_log(
            source=source,
            interface_code=(
                "PYTDX_KLINE_ON_DEMAND"
                if source.adapter_type == "PYTDX"
                else "A_KLINE_ON_DEMAND"
                if market == MARKET_CN_A
                else "NEEQ_KLINE_ON_DEMAND"
                if market in (MARKET_NEEQ, MARKET_NEEQ_INNOVATION)
                else "HK_KLINE_ON_DEMAND"
            ),
            market=market,
            symbol=symbol,
            request_json={
                "period": period,
                "adjust": adjust,
                "start_date": start_date,
                "end_date": end_date,
                "persist": persist,
            },
        )
        try:
            records = get_adapter(source.adapter_type).fetch_kline(
                market=market,
                symbol=symbol,
                period=period,
                start_date=start_date,
                end_date=end_date,
                adjust=adjust,
            )
            persisted_count = self._upsert_klines(source.id, records) if persist else 0
            result = self._finish_success(log, records, persisted_count)
            if persist:
                self._touch_stock_last_synced(market, symbol)
            return result
        except Exception as exc:
            raise self._finish_failure(log, exc) from exc

    def fetch_financials(
        self,
        source: DataSource,
        market: str,
        symbol: str,
        indicator: str,
        persist: bool,
    ) -> tuple[DataFetchLog, list[dict[str, Any]]]:
        log = self._start_log(
            source=source,
            interface_code="FINANCIAL_ON_DEMAND",
            market=market,
            symbol=symbol,
            request_json={"indicator": indicator, "persist": persist},
        )
        try:
            records = get_adapter(source.adapter_type).fetch_financial_indicators(
                market=market,
                symbol=symbol,
                indicator=indicator,
            )
            persisted_count = self._upsert_financials(source.id, records) if persist else 0
            result = self._finish_success(log, records, persisted_count)
            if persist:
                self._touch_stock_last_synced(market, symbol)
            return result
        except Exception as exc:
            raise self._finish_failure(log, exc) from exc

    def fetch_notices(
        self,
        source: DataSource,
        market: str,
        symbol: str,
        start_date: str,
        end_date: str,
        persist: bool,
    ) -> tuple[DataFetchLog, list[dict[str, Any]]]:
        log = self._start_log(
            source=source,
            interface_code="NOTICE_ON_DEMAND",
            market=market,
            symbol=symbol,
            request_json={"start_date": start_date, "end_date": end_date, "persist": persist},
        )
        try:
            records = get_adapter(source.adapter_type).fetch_notices(
                market=market,
                symbol=symbol,
                start_date=start_date,
                end_date=end_date,
            )
            persisted_count = self._upsert_notices(source.id, records) if persist else 0
            result = self._finish_success(log, records, persisted_count)
            if persist:
                self._touch_stock_last_synced(market, symbol)
            return result
        except Exception as exc:
            raise self._finish_failure(log, exc) from exc

    def fetch_quote(
        self,
        source: DataSource,
        market: str,
        symbol: str,
        persist: bool,
    ) -> tuple[DataFetchLog, list[dict[str, Any]]]:
        log = self._start_log(
            source=source,
            interface_code=(
                "PYTDX_QUOTE_ON_DEMAND"
                if source.adapter_type == "PYTDX"
                else "QUOTE_ON_DEMAND"
            ),
            market=market,
            symbol=symbol,
            request_json={"persist": persist},
        )
        try:
            record = get_adapter(source.adapter_type).fetch_realtime_quote(market=market, symbol=symbol)
            persisted_count = self._upsert_quote(source.id, record) if persist else 0
            result = self._finish_success(log, [record], persisted_count)
            if persist:
                self._touch_stock_last_synced(market, symbol)
            return result
        except Exception as exc:
            raise self._finish_failure(log, exc) from exc

    def fetch_news(
        self,
        source: DataSource,
        market: str,
        symbol: str,
        persist: bool,
    ) -> tuple[DataFetchLog, list[dict[str, Any]]]:
        log = self._start_log(
            source=source,
            interface_code="NEWS_ON_DEMAND",
            market=market,
            symbol=symbol,
            request_json={"persist": persist},
        )
        try:
            records = get_adapter(source.adapter_type).fetch_news(market=market, symbol=symbol)
            persisted_count = self._upsert_news(source.id, records) if persist else 0
            result = self._finish_success(log, records, persisted_count)
            if persist:
                self._touch_stock_last_synced(market, symbol)
            return result
        except Exception as exc:
            raise self._finish_failure(log, exc) from exc

    def upsert_f10_cache(
        self,
        source: DataSource,
        market: str,
        symbol: str,
        section: str,
        payload_json: dict[str, Any],
    ) -> int:
        existing = self.db.scalar(
            select(StockF10Cache).where(
                StockF10Cache.market == market,
                StockF10Cache.symbol == symbol,
                StockF10Cache.section == section,
            )
        )
        fetch_time = datetime.now(timezone.utc)
        changed = False
        if existing:
            existing_reports = (
                existing.payload_json.get("reports")
                if section == "published_reports"
                and isinstance(existing.payload_json, dict)
                else None
            )
            incoming_reports = (
                payload_json.get("reports")
                if section == "published_reports"
                and isinstance(payload_json, dict)
                else None
            )
            existing_has_formal_report = any(
                isinstance(row, dict)
                and str(row.get("report_type") or "").lower()
                not in {"financial_indicators", "announcement"}
                for row in (existing_reports or [])
            )
            incoming_has_formal_report = any(
                isinstance(row, dict)
                and str(row.get("report_type") or "").lower()
                not in {"financial_indicators", "announcement"}
                for row in (incoming_reports or [])
            )
            if (
                _incoming_f10_payload_is_stale(section, existing.payload_json, payload_json)
                and not (section == "published_reports" and incoming_has_formal_report and not existing_has_formal_report)
            ):
                existing.fetched_at = fetch_time
                self.db.flush()
                self.db.commit()
                return 0
            payload_json = _merge_f10_payload(section, existing.payload_json, payload_json)
            changed = existing.source_id != source.id or existing.payload_json != payload_json
            if changed:
                existing.source_id = source.id
                existing.payload_json = payload_json
            existing.fetched_at = fetch_time
        else:
            changed = True
            self.db.add(
                StockF10Cache(
                    market=market,
                    symbol=symbol,
                    section=section,
                    source_id=source.id,
                    payload_json=payload_json,
                    fetched_at=fetch_time,
                )
            )
        self.db.flush()
        self.db.commit()
        return 1 if changed else 0

    def load_f10_cache(self, market: str, symbol: str) -> dict[str, dict[str, Any]]:
        records = self.db.scalars(
            select(StockF10Cache)
            .where(StockF10Cache.market == market, StockF10Cache.symbol == symbol)
            .order_by(StockF10Cache.section.asc())
        ).all()
        return {item.section: item.payload_json for item in records}

    def _start_log(
        self,
        source: DataSource,
        interface_code: str,
        market: str,
        symbol: str,
        request_json: dict[str, Any],
    ) -> DataFetchLog:
        log = DataFetchLog(
            source_id=source.id,
            interface_code=interface_code,
            market=market,
            symbol=symbol,
            request_json=request_json,
            status="RUNNING",
        )
        self.db.add(log)
        self.db.commit()
        self.db.refresh(log)
        return log

    def _finish_success(
        self,
        log: DataFetchLog,
        records: list[KlineRecord] | list[FinancialRecord] | list[NoticeRecord] | list[QuoteRecord] | list[NewsRecord],
        persisted_count: int,
    ) -> tuple[DataFetchLog, list[dict[str, Any]]]:
        log.status = "SUCCESS"
        log.total_count = len(records)
        log.persisted_count = persisted_count
        log.completed_at = datetime.now(timezone.utc)
        self.db.commit()
        self.db.refresh(log)
        return log, [asdict(item) for item in records]

    def _finish_failure(self, log: DataFetchLog, exc: Exception) -> OnDemandFetchError:
        self.db.rollback()
        failed_log = self.db.get(DataFetchLog, log.id)
        if not failed_log:
            return OnDemandFetchError(str(exc), log.id)
        failed_log.status = "FAILED"
        failed_log.error_message = str(exc)[:4000]
        failed_log.completed_at = datetime.now(timezone.utc)
        self.db.commit()
        return OnDemandFetchError(str(exc), failed_log.id)

    def _upsert_klines(self, source_id: int, records: list[KlineRecord]) -> int:
        if not records:
            return 0
        first = records[0]
        existing = self.db.scalars(
            select(StockKline).where(
                StockKline.market == first.market,
                StockKline.symbol == first.symbol,
                StockKline.period == first.period,
                StockKline.adjust == first.adjust,
                StockKline.trade_date.in_([item.trade_date for item in records]),
            )
        ).all()
        existing_by_date = {item.trade_date: item for item in existing}
        fetch_time = datetime.now(timezone.utc)
        changed_count = 0
        for item in records:
            row = existing_by_date.get(item.trade_date)
            if row:
                changed = any(
                    (
                        row.open_price != item.open_price,
                        row.high_price != item.high_price,
                        row.low_price != item.low_price,
                        row.close_price != item.close_price,
                        row.volume != item.volume,
                        row.amount != item.amount,
                        row.turnover_rate != item.turnover_rate,
                        row.source_id != source_id,
                        row.raw_payload != item.raw_payload,
                    )
                )
                if changed:
                    row.open_price = item.open_price
                    row.high_price = item.high_price
                    row.low_price = item.low_price
                    row.close_price = item.close_price
                    row.volume = item.volume
                    row.amount = item.amount
                    row.turnover_rate = item.turnover_rate
                    row.source_id = source_id
                    row.raw_payload = item.raw_payload
                    changed_count += 1
                row.fetched_at = fetch_time
            else:
                self.db.add(
                    StockKline(
                        market=item.market,
                        symbol=item.symbol,
                        period=item.period,
                        adjust=item.adjust,
                        trade_date=item.trade_date,
                        open_price=item.open_price,
                        high_price=item.high_price,
                        low_price=item.low_price,
                        close_price=item.close_price,
                        volume=item.volume,
                        amount=item.amount,
                        turnover_rate=item.turnover_rate,
                        source_id=source_id,
                        raw_payload=item.raw_payload,
                        fetched_at=fetch_time,
                    )
                )
                changed_count += 1
        self.db.flush()
        return changed_count

    def _upsert_financials(self, source_id: int, records: list[FinancialRecord]) -> int:
        if not records:
            return 0
        first = records[0]
        existing = self.db.scalars(
            select(StockFinancialReport).where(
                StockFinancialReport.market == first.market,
                StockFinancialReport.symbol == first.symbol,
                StockFinancialReport.indicator == first.indicator,
                StockFinancialReport.source_id == source_id,
                StockFinancialReport.report_period.in_([item.report_period for item in records]),
            )
        ).all()
        existing_by_period = {item.report_period: item for item in existing}
        fetch_time = datetime.now(timezone.utc)
        changed_count = 0
        for item in records:
            row = existing_by_period.get(item.report_period)
            item_url = item.url or str(
                item.data_json.get("url")
                or item.data_json.get("URL")
                or item.data_json.get("公告链接")
                or ""
            ).strip() or None
            if row:
                changed = any(
                    (
                        row.currency != item.currency,
                        row.data_json != item.data_json,
                        row.url != item_url,
                    )
                )
                if changed:
                    row.currency = item.currency
                    row.data_json = item.data_json
                    row.url = item_url
                    changed_count += 1
                row.fetched_at = fetch_time
            else:
                self.db.add(
                    StockFinancialReport(
                        market=item.market,
                        symbol=item.symbol,
                        indicator=item.indicator,
                        report_period=item.report_period,
                        currency=item.currency,
                        data_json=item.data_json,
                        url=item_url,
                        source_id=source_id,
                        fetched_at=fetch_time,
                    )
                )
                changed_count += 1
        self.db.flush()
        return changed_count

    def _upsert_notices(self, source_id: int, records: list[NoticeRecord]) -> int:
        if not records:
            return 0
        # Providers can repeat an announcement in one response, especially when
        # the same filing belongs to multiple announcement categories. De-dupe
        # before adding rows so one duplicate cannot roll back the whole fetch.
        unique_records: list[NoticeRecord] = []
        seen_keys: set[tuple[str, str]] = set()
        for item in records:
            notice_date = str(item.notice_date or "").strip()
            title = str(item.title or "").strip()
            if not notice_date or not title:
                continue
            key = (notice_date, title)
            if key in seen_keys:
                continue
            seen_keys.add(key)
            unique_records.append(item)
        if not unique_records:
            return 0
        records = unique_records
        first = records[0]
        existing = self.db.scalars(
            select(StockNotice).where(
                StockNotice.market == first.market,
                StockNotice.symbol == first.symbol,
                StockNotice.source_id == source_id,
                StockNotice.notice_date.in_([item.notice_date for item in records]),
                StockNotice.title.in_([item.title for item in records]),
            )
        ).all()
        existing_keys = {(item.notice_date, item.title): item for item in existing}
        fetch_time = datetime.now(timezone.utc)
        changed_count = 0
        for item in records:
            row = existing_keys.get((item.notice_date, item.title))
            if row:
                changed = any(
                    (
                        row.notice_type != item.notice_type,
                        row.url != item.url,
                        row.content_json != item.content_json,
                    )
                )
                if changed:
                    row.notice_type = item.notice_type
                    row.url = item.url
                    row.content_json = item.content_json
                    changed_count += 1
                row.fetched_at = fetch_time
            else:
                self.db.add(
                    StockNotice(
                        market=item.market,
                        symbol=item.symbol,
                        notice_date=item.notice_date,
                        title=item.title,
                        notice_type=item.notice_type,
                        url=item.url,
                        content_json=item.content_json,
                        source_id=source_id,
                        fetched_at=fetch_time,
                    )
                )
                changed_count += 1
        self.db.flush()
        return changed_count

    def _upsert_quote(self, source_id: int, record: QuoteRecord) -> int:
        existing = self.db.scalar(
            select(StockRealtimeQuote).where(
                StockRealtimeQuote.market == record.market,
                StockRealtimeQuote.symbol == record.symbol,
            )
        )
        fetch_time = datetime.now(timezone.utc)
        if existing:
            existing_time = _parse_timestamp(existing.quote_time)
            incoming_time = _parse_timestamp(record.quote_time)
            if existing_time and incoming_time and incoming_time < existing_time:
                existing.fetched_at = fetch_time
                self.db.flush()
                return 0
            changed = any(
                (
                    existing.source_id != source_id,
                    existing.quote_time != record.quote_time,
                    existing.current_price != record.current_price,
                    existing.previous_close_price != record.previous_close_price,
                    existing.open_price != record.open_price,
                    existing.high_price != record.high_price,
                    existing.low_price != record.low_price,
                    existing.volume != record.volume,
                    existing.amount != record.amount,
                    existing.change_amount != record.change_amount,
                    existing.change_pct != record.change_pct,
                    existing.turnover_rate != record.turnover_rate,
                    existing.raw_payload != record.raw_payload,
                )
            )
            if changed:
                existing.source_id = source_id
                existing.quote_time = record.quote_time
                existing.current_price = record.current_price
                existing.previous_close_price = record.previous_close_price
                existing.open_price = record.open_price
                existing.high_price = record.high_price
                existing.low_price = record.low_price
                existing.volume = record.volume
                existing.amount = record.amount
                existing.change_amount = record.change_amount
                existing.change_pct = record.change_pct
                existing.turnover_rate = record.turnover_rate
                existing.raw_payload = record.raw_payload
            existing.fetched_at = fetch_time
            changed_count = 1 if changed else 0
        else:
            self.db.add(
                StockRealtimeQuote(
                    market=record.market,
                    symbol=record.symbol,
                    quote_time=record.quote_time,
                    current_price=record.current_price,
                    previous_close_price=record.previous_close_price,
                    open_price=record.open_price,
                    high_price=record.high_price,
                    low_price=record.low_price,
                    volume=record.volume,
                    amount=record.amount,
                    change_amount=record.change_amount,
                    change_pct=record.change_pct,
                    turnover_rate=record.turnover_rate,
                    source_id=source_id,
                    raw_payload=record.raw_payload,
                    fetched_at=fetch_time,
                )
            )
            changed_count = 1
        self.db.flush()
        return changed_count

    def _upsert_news(self, source_id: int, records: list[NewsRecord]) -> int:
        if not records:
            return 0
        unique_records: list[NewsRecord] = []
        seen_keys: set[tuple[str, str]] = set()
        for item in records:
            news_time = str(item.news_time or "").strip()
            title = str(item.title or "").strip()
            if not news_time or not title:
                continue
            key = (news_time, title)
            if key in seen_keys:
                continue
            seen_keys.add(key)
            unique_records.append(item)
        if not unique_records:
            return 0
        records = unique_records
        existing = self.db.scalars(
            select(StockNews).where(
                StockNews.market == records[0].market,
                StockNews.symbol == records[0].symbol,
                StockNews.source_id == source_id,
            )
        ).all()
        existing_keys = {(item.news_time, item.title): item for item in existing}
        fetch_time = datetime.now(timezone.utc)
        changed_count = 0
        for item in records:
            row = existing_keys.get((item.news_time, item.title))
            if row:
                changed = any(
                    (
                        row.source_id != source_id,
                        row.content != item.content,
                        row.source_name != item.source_name,
                        row.url != item.url,
                        row.content_json != item.content_json,
                    )
                )
                if changed:
                    row.source_id = source_id
                    row.content = item.content
                    row.source_name = item.source_name
                    row.url = item.url
                    row.content_json = item.content_json
                    changed_count += 1
                row.fetched_at = fetch_time
            else:
                self.db.add(
                    StockNews(
                        market=item.market,
                        symbol=item.symbol,
                        news_time=item.news_time,
                        title=item.title,
                        content=item.content,
                        source_name=item.source_name,
                        url=item.url,
                        content_json=item.content_json,
                        source_id=source_id,
                        fetched_at=fetch_time,
                    )
                )
                changed_count += 1
        self.db.flush()
        return changed_count
