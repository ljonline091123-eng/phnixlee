"""Bounded source-to-database audit for the lakehouse MVP stock scope."""
from __future__ import annotations

import argparse
import json
from datetime import date, timedelta
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from sqlalchemy import func, select

from app.db.session import SessionLocal
from app.models.market_data import (
    DataSource, StockF10Cache, StockFinancialReport, StockKline, StockNews,
    StockNotice, StockRealtimeQuote,
)
from app.services.lakehouse_validation import select_scope
from app.connectors.registry import get_adapter
from app.services.stock_on_demand import StockOnDemandService


MODELS = {
    "quote": StockRealtimeQuote, "kline": StockKline, "financial": StockFinancialReport,
    "news": StockNews, "notice": StockNotice, "f10": StockF10Cache,
}


def count_rows(db, model, market: str, symbol: str) -> int:
    return int(db.scalar(select(func.count(model.id)).where(
        model.market == market, model.symbol == symbol)) or 0)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--count", type=int, default=20)
    parser.add_argument("--output", default="validation/source-e2e-latest.json")
    args = parser.parse_args()
    output = Path(args.output)
    if not output.is_absolute():
        output = Path(__file__).resolve().parents[1] / output
    output.parent.mkdir(parents=True, exist_ok=True)

    report = {"scope_count": args.count, "results": [], "coverage_before": [], "coverage_after": []}
    with SessionLocal() as db:
        source = db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE"))
        if source is None or not source.enabled:
            raise RuntimeError("AKSHARE source is unavailable or disabled")
        scope = select_scope(db, args.count)
        service = StockOnDemandService(db)
        today = date.today()
        for item in scope:
            market, symbol = item["market"], item["symbol"]
            before = {name: count_rows(db, model, market, symbol) for name, model in MODELS.items()}
            report["coverage_before"].append({"market": market, "symbol": symbol, **before})
            missing = [name for name, count in before.items() if count == 0]
            for domain in missing:
                result = {"market": market, "symbol": symbol, "domain": domain,
                          "source": source.source_code, "before": before[domain]}
                try:
                    if domain == "quote":
                        log, rows = service.fetch_quote(source, market, symbol, persist=True)
                    elif domain == "kline":
                        log, rows = service.fetch_kline(source, market, symbol, "daily", "",
                            (today - timedelta(days=3650)).strftime("%Y%m%d"), today.strftime("%Y%m%d"), True)
                    elif domain == "financial":
                        indicator = "报告期" if market == "HK" else "按报告期"
                        log, rows = service.fetch_financials(source, market, symbol, indicator, True)
                    elif domain == "news":
                        log, rows = service.fetch_news(source, market, symbol, True)
                    elif domain == "notice":
                        log, rows = service.fetch_notices(source, market, symbol,
                            (today - timedelta(days=3650)).strftime("%Y%m%d"), today.strftime("%Y%m%d"), True)
                    else:
                        payload = get_adapter(source.adapter_type).fetch_extended_data(market, symbol)
                        persisted = 0
                        for section, section_payload in payload.items():
                            if isinstance(section_payload, dict):
                                persisted += service.upsert_f10_cache(source, market, symbol, section, section_payload)
                        rows, log = list(payload), None
                        result["persisted_count"] = persisted
                    after = count_rows(db, MODELS[domain], market, symbol)
                    result.update({"status": "SUCCESS" if rows else "EMPTY", "returned_count": len(rows),
                                   "persisted_count": result.get("persisted_count", getattr(log, "persisted_count", 0)),
                                   "after": after, "fetch_log_id": getattr(log, "id", None)})
                except Exception as exc:
                    db.rollback()
                    result.update({"status": "FAILED", "returned_count": 0, "persisted_count": 0,
                                   "after": count_rows(db, MODELS[domain], market, symbol),
                                   "error": str(exc)[:500]})
                report["results"].append(result)
                print(json.dumps(result, ensure_ascii=False), flush=True)
            after_all = {name: count_rows(db, model, market, symbol) for name, model in MODELS.items()}
            report["coverage_after"].append({"market": market, "symbol": symbol, **after_all})
    report["summary"] = {
        status: sum(item["status"] == status for item in report["results"])
        for status in ("SUCCESS", "EMPTY", "FAILED")
    }
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"output": str(output), **report["summary"]}, ensure_ascii=False))


if __name__ == "__main__":
    main()
