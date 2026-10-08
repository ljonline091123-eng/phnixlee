"""Preview/apply official listing classification without replacing stock identities."""
import argparse
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
from dataclasses import asdict
from datetime import datetime, timezone
import json
from pathlib import Path
import sys
from uuid import uuid4

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import app.models
from sqlalchemy import select
from app.connectors.listing_master import SseMasterAdapter, SzseMasterAdapter, BseMasterAdapter, HkexSecuritiesAdapter
from app.db.session import SessionLocal
from app.models.market_data import DataSource, DataSyncLog, StockSymbol
from app.models.lakehouse import LakeLineageEvent
from app.services.catalog import seed_default_catalog
from app.services.lakehouse import archive_ingestion_payload
from app.services.security_master import normalized_ext, reconcile_master
from app.services.stock_sync import StockSyncService


def fetch(cls):
    try:
        adapter = cls()
        if cls == HkexSecuritiesAdapter:
            manifest = adapter.fetch_snapshot()
            records = adapter.normalize_snapshot(manifest)
        else:
            manifest = None
            records = adapter.fetch_symbol_master("CN_A")
        return {"source_code": cls.adapter_type, "status": "SUCCESS", "records": records, "manifest": manifest,
                "total": len(records), "boards": dict(Counter(r.ext_json["listing"]["listing_board"] for r in records))}
    except Exception as exc:
        return {"source_code": cls.adapter_type, "status": "FAILED", "error": str(exc)[:500], "records": [], "manifest": None}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true", help="Apply after taking workspace/database backups")
    parser.add_argument("--max-additions", type=int, default=400)
    parser.add_argument("--report", required=True)
    args = parser.parse_args()
    if not 0 <= args.max_additions <= 1000:
        parser.error("max-additions must be 0..1000")
    with SessionLocal() as db:
        if args.apply:
            seed_default_catalog(db)
        source_rows = {s.source_code: s for s in db.scalars(select(DataSource)).all()}
        adapters = [cls for cls in [SseMasterAdapter, SzseMasterAdapter, BseMasterAdapter, HkexSecuritiesAdapter]
                    if cls.adapter_type not in source_rows or source_rows[cls.adapter_type].enabled]
        with ThreadPoolExecutor(max_workers=4) as pool:
            results = list(pool.map(fetch, adapters))
        known = {(s.market, s.symbol): s for s in db.scalars(select(StockSymbol)).all()}
        additions = [(part, r) for part in results for r in part["records"] if (r.market, r.symbol) not in known]
        report = {"applied": args.apply, "before": len(known), "missing_current_official_stocks": len(additions),
                  "sources": [{k: v for k, v in part.items() if k not in {"records", "manifest"}} for part in results],
                  "identity_preserved": True, "archive": []}
        if len(additions) > args.max_additions:
            raise ValueError(f"发现 {len(additions)} 个新增身份，超过增量保护上限 {args.max_additions}，未应用")
        if args.apply:
            for part in results:
                source = source_rows[part["source_code"]]
                count = 0
                for record in part["records"]:
                    stock = known.get((record.market, record.symbol))
                    if stock and record.market == "CN_A":
                        # Only the classification evidence is refreshed. Preserve
                        # localized names, company links, raw history and source IDs.
                        stock.ext_json = normalized_ext(stock.market, stock.symbol, {
                            **(stock.ext_json or {}), "listing_classification": record.ext_json["listing_classification"],
                        }, stock.raw_payload)
                        count += 1
                new_records = [r for p, r in additions if p is part]
                if new_records:
                    StockSyncService(db)._upsert_records(source.id, new_records)
                log = DataSyncLog(source_id=source.id, interface_code="HK_SYMBOLS" if source.source_code == "HKEX_SECURITIES" else "CN_A_SYMBOLS",
                                  market="HK" if source.source_code == "HKEX_SECURITIES" else "CN_A", status=part["status"], total_count=part.get("total", 0),
                                  inserted_count=len(new_records), updated_count=count, failed_count=1 if part["status"] == "FAILED" else 0,
                                  error_message=part.get("error"), completed_at=datetime.now(timezone.utc),
                                  detail_json={"operation": "INCREMENTAL_CLASSIFICATION", "boards": part.get("boards", {}), "identity_preserved": True})
                db.add(log)
                db.flush()
                part["log_id"] = log.id
            hk = next((part["manifest"] for part in results if part["source_code"] == "HKEX_SECURITIES" and part["manifest"]), None)
            report["reconciliation"] = reconcile_master(db, hk)
            db.commit()
            for part in results:
                if part["status"] != "SUCCESS":
                    continue
                try:
                    archive = archive_ingestion_payload(db, source_code=part["source_code"], log_type="data_sync_log", log_id=part["log_id"], records=[asdict(r) for r in part["records"]])
                    report["archive"].append({"source": part["source_code"], "object_id": archive["object_id"], "content_hash": archive["content_hash"]})
                    if part["manifest"]:
                        original = StockSyncService(db)._archive_hk_snapshot(part["manifest"], part["log_id"])
                        report["archive"].append({"source": part["source_code"], "original_xlsx_object_id": original["object_id"], "content_hash": original["content_hash"]})
                    batch_id = str(uuid4())
                    records = part["manifest"]["records"] if part["manifest"] else [r.raw_payload for r in part["records"]]
                    market = "HK" if part["manifest"] else "CN_A"
                    ids = {s.symbol: s.id for s in db.scalars(select(StockSymbol).where(StockSymbol.market == market)).all()}
                    for raw in records:
                        symbol = str(raw.get("Stock Code") or raw.get("证券代码") or raw.get("A股代码") or "")
                        if symbol not in ids:
                            continue
                        db.add(LakeLineageEvent(batch_id=batch_id, upstream_type="LAKE_OBJECT", upstream_id=str(original["object_id"] if part["manifest"] else archive["object_id"]),
                                                downstream_type="STOCK_SYMBOL", downstream_id=str(ids[symbol]), transformation="LISTING_CLASSIFICATION", parser_version="LISTING_TAXONOMY_V1",
                                                metadata_json={"market": market, "symbol": symbol, "source_code": part["source_code"], "sync_log_id": part["log_id"]}))
                    db.commit()
                except Exception as exc:
                    db.rollback()
                    report["archive"].append({"source": part["source_code"], "status": "PARTIAL", "error": str(exc)[:500]})
            report["after"] = len(db.scalars(select(StockSymbol.id)).all())
        else:
            db.rollback()
    target = Path(args.report)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(report, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, default=str))


if __name__ == "__main__":
    main()
