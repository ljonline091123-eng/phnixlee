"""Background scheduler for the daily stock master-data refresh.

The detail endpoint owns on-demand quote/K-line/F10 refreshes.  Master data is
different: it is a small, complete universe snapshot and can be synchronized
once per day without making every detail request pay the network cost.
"""

from __future__ import annotations

import asyncio
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo

from sqlalchemy import select

from app.core.markets import MASTER_MARKETS
from app.db.session import SessionLocal
from app.models.market_data import DataSource
from app.services.catalog import seed_default_catalog
from app.services.stock_sync import StockSyncService


SHANGHAI = ZoneInfo("Asia/Shanghai")


def _seconds_until_next_midnight() -> float:
    """Return a positive delay until the next Asia/Shanghai midnight."""
    now = datetime.now(SHANGHAI)
    tomorrow = (now + timedelta(days=1)).replace(
        hour=0,
        minute=0,
        second=0,
        microsecond=0,
    )
    return max(1.0, (tomorrow - now).total_seconds())


def sync_master_data_once() -> None:
    """Synchronize all configured master markets using the primary source."""
    with SessionLocal() as db:
        # Keep this callable independently testable and self-healing when the
        # service is launched against an empty database.
        seed_default_catalog(db)
        source = db.scalar(
            select(DataSource).where(
                DataSource.source_code == "AKSHARE",
                DataSource.enabled.is_(True),
            )
        )
        if source is None:
            return
        for market in MASTER_MARKETS:
            try:
                StockSyncService(db).synchronize(source, market, enable_fallback=True)
            except Exception:
                # A failing market must not prevent the other markets from
                # being refreshed at the same scheduled run.
                db.rollback()


async def daily_master_sync_loop() -> None:
    """Run the master-data refresh at 00:00 China Standard Time forever."""
    while True:
        await asyncio.sleep(_seconds_until_next_midnight())
        await asyncio.to_thread(sync_master_data_once)
