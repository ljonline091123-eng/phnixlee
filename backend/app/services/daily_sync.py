"""Background scheduler for the daily stock master-data refresh.

The detail endpoint owns on-demand quote/K-line/F10 refreshes.  Master data is
different: it is a small, complete universe snapshot and can be synchronized
once per day without making every detail request pay the network cost.
"""

from __future__ import annotations

import asyncio
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo



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
    """Compatibility wrapper; new callers use MasterDataSyncWorkflow."""
    from app.orchestration.background import MasterDataSyncWorkflow

    MasterDataSyncWorkflow().execute()


async def daily_master_sync_loop() -> None:
    """Run the master-data refresh at 00:00 China Standard Time forever."""
    while True:
        await asyncio.sleep(_seconds_until_next_midnight())
        await asyncio.to_thread(sync_master_data_once)
