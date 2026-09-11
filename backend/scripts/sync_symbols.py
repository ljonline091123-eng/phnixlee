import argparse

from sqlalchemy import select

from app.db.base import Base
from app.db.session import SessionLocal, engine
from app.models.market_data import DataSource
from app.services.catalog import seed_default_catalog
from app.services.stock_sync import StockSyncService


def main() -> None:
    parser = argparse.ArgumentParser(description="Synchronize A-share/HK/NEEQ stock master data.")
    parser.add_argument(
        "--market",
        choices=("CN_A", "HK", "NEEQ", "NEEQ_INNOVATION", "ALL"),
        default="ALL",
    )
    parser.add_argument("--source-code", default="AKSHARE")
    args = parser.parse_args()

    Base.metadata.create_all(bind=engine)
    with SessionLocal() as db:
        seed_default_catalog(db)
        source = db.scalar(select(DataSource).where(DataSource.source_code == args.source_code))
        if not source:
            raise SystemExit(f"Data source not found: {args.source_code}")

        markets = ("CN_A", "HK", "NEEQ", "NEEQ_INNOVATION") if args.market == "ALL" else (args.market,)
        for market in markets:
            sync_log = StockSyncService(db).synchronize(source, market)
            route_trace = (sync_log.detail_json or {}).get("route_trace") or []
            route = " -> ".join(item.get("source_code", "?") for item in route_trace if isinstance(item, dict))
            print(
                f"{market} via {route or source.source_code}: {sync_log.status}, "
                f"total={sync_log.total_count}, inserted={sync_log.inserted_count}, "
                f"updated={sync_log.updated_count}"
            )
            if sync_log.error_message:
                print(f"error: {sync_log.error_message}")


if __name__ == "__main__":
    main()
