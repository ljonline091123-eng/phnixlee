"""增量关联历史 F10 披露文件与个股公告；默认只预览，不联网。"""

import argparse
import json
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from sqlalchemy import select

from app.db.session import SessionLocal
from app.models.market_data import DataFetchLog, DataSource, StockF10Cache, StockNotice
from app.services.f10 import _merge_local_report_notices
from app.services.stock_on_demand import StockOnDemandService


def reconcile(db, *, apply=False, market=None, symbol=None):
    query = select(StockF10Cache).where(StockF10Cache.section == "published_reports")
    if market:
        query = query.where(StockF10Cache.market == market.upper())
    if symbol:
        query = query.where(StockF10Cache.symbol == symbol)
    caches = list(db.scalars(query.order_by(StockF10Cache.id)))
    result = {"applied": apply, "stocks": [], "errors": []}
    service = StockOnDemandService(db)
    for cache in caches:
        identity = {"market": cache.market, "symbol": cache.symbol, "cache_id": cache.id}
        try:
            source = db.get(DataSource, cache.source_id)
            if source is None:
                raise ValueError("原始 F10 数据源不存在，保留缓存等待修复")
            notices = list(db.scalars(select(StockNotice).where(
                StockNotice.market == cache.market, StockNotice.symbol == cache.symbol,
            )))
            before = _merge_local_report_notices(cache.payload_json, notices, cache.market, cache.symbol)
            imported = 0
            if apply:
                imported = service.persist_report_notices(source, cache.market, cache.symbol, cache.payload_json)
                notices = list(db.scalars(select(StockNotice).where(
                    StockNotice.market == cache.market, StockNotice.symbol == cache.symbol,
                )))
            after = _merge_local_report_notices(cache.payload_json, notices, cache.market, cache.symbol)
            if apply and cache.payload_json != after:
                service.upsert_f10_cache(source, cache.market, cache.symbol, "published_reports", after)
                db.commit()
            result["stocks"].append({
                **identity, "linked_before": len(before["reports"]),
                "linked_after": len(after["reports"]), "imported_or_enriched": imported,
                "unlinked_documents_before": len(before["unlinked_documents"]),
                "unlinked_documents_after": len(after["unlinked_documents"]),
                "unlinked_periods": len(after["unlinked_periods"]), "status": after["status"],
            })
            if imported:
                log = db.scalar(select(DataFetchLog).where(
                    DataFetchLog.market == cache.market, DataFetchLog.symbol == cache.symbol,
                    DataFetchLog.interface_code == "NOTICE_FROM_F10",
                ).order_by(DataFetchLog.id.desc()))
                if log and (log.request_json or {}).get("lake_archive_error"):
                    result["errors"].append({**identity, "error": log.request_json["lake_archive_error"]})
        except Exception as exc:
            db.rollback()
            result["errors"].append({**identity, "error": str(exc)})
    result["stock_count"] = len(result["stocks"])
    result["imported_or_enriched"] = sum(row["imported_or_enriched"] for row in result["stocks"])
    result["unlinked_documents"] = sum(row["unlinked_documents_after"] for row in result["stocks"])
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true", help="完成 Git 和数据库备份后执行增量关联")
    parser.add_argument("--market", help="仅处理指定市场")
    parser.add_argument("--symbol", help="仅处理指定证券代码")
    args = parser.parse_args()
    with SessionLocal() as db:
        result = reconcile(db, apply=args.apply, market=args.market, symbol=args.symbol)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 1 if result["errors"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
