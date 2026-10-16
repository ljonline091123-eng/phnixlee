"""Unit tests for the watch-pool data refresh workflow."""

import unittest
from datetime import date
from pathlib import Path
from tempfile import TemporaryDirectory
from types import SimpleNamespace
from unittest.mock import patch

import app.models  # noqa: F401 - register every ORM model in Base.metadata
from sqlalchemy import create_engine, select
from sqlalchemy.orm import sessionmaker

from app.db.base import Base
from app.models.market_data import DataSource, StockKline, WatchlistItem
from app.models.pipeline import ScheduledJob
from app.orchestration.watch_pool import WatchPoolRefreshWorkflow, compute_kline_window


class KlineWindowTest(unittest.TestCase):
    def test_first_fill_window(self):
        start, end = compute_kline_window(None, date(2026, 10, 16))
        self.assertEqual(start, "20260718")
        self.assertEqual(end, "20261016")

    def test_incremental_window_overlaps_latest_bar(self):
        start, end = compute_kline_window("2026-10-14", date(2026, 10, 16))
        self.assertEqual(start, "20261011")
        self.assertEqual(end, "20261016")

    def test_very_old_latest_is_capped(self):
        start, end = compute_kline_window("2025-01-01", date(2026, 10, 16))
        self.assertEqual(start, "20251011")
        self.assertEqual(end, "20261016")

    def test_unparseable_latest_falls_back_to_first_fill(self):
        start, _ = compute_kline_window("not-a-date", date(2026, 10, 16))
        self.assertEqual(start, "20260718")


class WatchPoolRefreshWorkflowTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp_dir = TemporaryDirectory()
        db_path = Path(cls.temp_dir.name) / "test_watch_pool.db"
        cls.engine = create_engine(f"sqlite:///{db_path}")
        Base.metadata.create_all(cls.engine)
        cls.session_factory = sessionmaker(bind=cls.engine)

    @classmethod
    def tearDownClass(cls):
        cls.engine.dispose()
        if cls.temp_dir:
            cls.temp_dir.cleanup()

    def setUp(self):
        Base.metadata.drop_all(self.engine)
        Base.metadata.create_all(self.engine)

    def _seed(self):
        with self.session_factory() as db:
            source = DataSource(source_code="TEST_SRC", source_name="测试数据源", adapter_type="TEST")
            db.add(source)
            db.flush()
            db.add(WatchlistItem(market="CN_A", symbol="000001", note="池样本1"))
            db.add(WatchlistItem(market="CN_A", symbol="000002", note="池样本2"))
            db.add(
                StockKline(
                    market="CN_A",
                    symbol="000001",
                    period="daily",
                    adjust="",
                    trade_date="2026-10-14",
                    open_price=10.0,
                    high_price=10.0,
                    low_price=10.0,
                    close_price=10.0,
                    volume=1000.0,
                    source_id=source.id,
                )
            )
            db.commit()

    def test_refresh_calls_services_and_tolerates_failure(self):
        self._seed()
        fake_source = SimpleNamespace(id=1, source_code="FAKE", adapter_type="FAKE")
        kline_log = SimpleNamespace(persisted_count=3, id=11)
        notice_log = SimpleNamespace(persisted_count=2, id=12)
        news_log = SimpleNamespace(persisted_count=1, id=13)

        def kline_side_effect(*, source, market, symbol, period, adjust, start_date, end_date, persist):
            if symbol == "000002":
                raise RuntimeError("upstream reset")
            return kline_log, []

        with (
            patch("app.orchestration.watch_pool.SessionLocal", self.session_factory),
            patch("app.orchestration.watch_pool.select_data_source", return_value=fake_source),
            patch(
                "app.services.stock_on_demand.StockOnDemandService.fetch_kline",
                side_effect=kline_side_effect,
            ) as kline_mock,
            patch(
                "app.services.stock_on_demand.StockOnDemandService.fetch_notices",
                return_value=(notice_log, []),
            ) as notice_mock,
            patch(
                "app.services.stock_on_demand.StockOnDemandService.fetch_news",
                return_value=(news_log, []),
            ) as news_mock,
        ):
            result = WatchPoolRefreshWorkflow().execute({"business_date": "2026-10-16", "delay_seconds": 0})

        self.assertEqual(result["pool_size"], 2)
        self.assertEqual(result["status"], "PARTIAL")
        self.assertEqual(result["kline_persisted"], 3)
        self.assertEqual(result["notice_persisted"], 4)
        self.assertEqual(result["news_persisted"], 2)
        self.assertEqual(result["symbols_with_errors"], 1)

        by_key = {(item["market"], item["symbol"]): item for item in result["symbols"]}
        self.assertEqual(by_key[("CN_A", "000001")]["kline_persisted"], 3)
        self.assertTrue(by_key[("CN_A", "000002")]["errors"][0].startswith("kline:"))

        first_call = next(call for call in kline_mock.call_args_list if call.kwargs["symbol"] == "000001")
        self.assertEqual(first_call.kwargs["start_date"], "20261011")
        self.assertEqual(first_call.kwargs["end_date"], "20261016")
        self.assertTrue(first_call.kwargs["persist"])
        self.assertEqual(kline_mock.call_count, 2)
        self.assertEqual(notice_mock.call_count, 2)
        self.assertEqual(news_mock.call_count, 2)

        second_call = next(call for call in kline_mock.call_args_list if call.kwargs["symbol"] == "000002")
        self.assertEqual(second_call.kwargs["start_date"], "20260718")

        # the refresh chains the L0 detection job with the scheduler's idempotency key
        self.assertTrue(result["detection_chained"])
        with self.session_factory() as db:
            chained = db.scalar(
                select(ScheduledJob).where(ScheduledJob.idempotency_key == "l0_change_detection:2026-10-16")
            )
            self.assertIsNotNone(chained)
            self.assertEqual(chained.task_type, "l0_change_detection")
            self.assertEqual(chained.status, "PENDING")

    def test_chain_detection_can_be_disabled(self):
        """Manual refreshes must not consume the daily detection key."""
        self._seed()
        with (
            patch("app.orchestration.watch_pool.SessionLocal", self.session_factory),
            patch("app.orchestration.watch_pool.select_data_source", return_value=None),
        ):
            result = WatchPoolRefreshWorkflow().execute(
                {"business_date": "2026-10-16", "delay_seconds": 0, "chain_detection": False}
            )
        self.assertFalse(result["detection_chained"])
        with self.session_factory() as db:
            chained = db.scalar(
                select(ScheduledJob).where(ScheduledJob.idempotency_key == "l0_change_detection:2026-10-16")
            )
            self.assertIsNone(chained)

    def test_empty_pool_is_skipped(self):
        with patch("app.orchestration.watch_pool.SessionLocal", self.session_factory):
            result = WatchPoolRefreshWorkflow().execute({"business_date": "2026-10-16"})
        self.assertEqual(result["status"], "SKIPPED")
        self.assertEqual(result["reason"], "watchlist is empty; add pool symbols first")
        self.assertIsNone(result.get("detection_chained"))


if __name__ == "__main__":
    unittest.main()
