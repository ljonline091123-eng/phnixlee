"""Unit tests for L0 deterministic change detection.

Covers the pure detectors (positive / negative / boundary cases) and an
end-to-end run against a temporary SQLite database, including the
idempotency and stale-data guarantees.
"""

import unittest
from datetime import date, timedelta
from pathlib import Path
from tempfile import TemporaryDirectory

from fastapi.testclient import TestClient
from sqlalchemy import create_engine, select
from sqlalchemy.orm import sessionmaker

import app.models  # noqa: F401 - register every ORM model in Base.metadata
from app.db.base import Base
from app.db.session import get_db
from app.main import app
from app.models.change_event import StockChangeEvent
from app.models.market_data import DataSource, StockKline, StockNotice, WatchlistItem
from app.services.change_detection import (
    KlineBar,
    build_dedup_key,
    detect_breakout_20d,
    detect_ma_cross,
    detect_news_cluster,
    detect_notice_cluster,
    detect_notice_new,
    detect_price_move,
    detect_volume_spike,
    evaluate_symbol_changes,
    run_change_detection,
)


def _bars(closes, *, volumes=None, highs=None, lows=None, start="2026-09-25"):
    """Build ascending daily bars; with 21 values the last date is 2026-10-15."""
    origin = date.fromisoformat(start)
    bars = []
    for index, close in enumerate(closes):
        bars.append(
            KlineBar(
                trade_date=(origin + timedelta(days=index)).isoformat(),
                close=close,
                volume=volumes[index] if volumes else 1000.0,
                high=highs[index] if highs else close,
                low=lows[index] if lows else close,
            )
        )
    return bars


class PriceMoveTest(unittest.TestCase):
    def test_threshold_exactly_triggers(self):
        draft = detect_price_move(_bars([10.0, 10.3]), min_abs_change_pct=3.0)
        self.assertIsNotNone(draft)
        self.assertEqual(draft.event_type, "PRICE_MOVE")
        self.assertEqual(draft.direction, "UP")

    def test_below_threshold_is_silent(self):
        self.assertIsNone(detect_price_move(_bars([10.0, 10.2]), min_abs_change_pct=3.0))

    def test_down_direction(self):
        draft = detect_price_move(_bars([10.0, 9.6]), min_abs_change_pct=3.0)
        self.assertIsNotNone(draft)
        self.assertEqual(draft.direction, "DOWN")

    def test_needs_two_bars(self):
        self.assertIsNone(detect_price_move(_bars([10.0]), min_abs_change_pct=3.0))

    def test_missing_price_is_silent(self):
        bars = [KlineBar("2026-10-14", None, 1000.0), KlineBar("2026-10-15", 10.5, 1000.0)]
        self.assertIsNone(detect_price_move(bars, min_abs_change_pct=3.0))


class VolumeSpikeTest(unittest.TestCase):
    def test_spike_triggers(self):
        bars = _bars([10.0] * 20 + [10.2], volumes=[1000.0] * 20 + [2500.0])
        draft = detect_volume_spike(bars, min_volume_ratio=2.0, window=20)
        self.assertIsNotNone(draft)
        self.assertAlmostEqual(draft.signal["volume_ratio"], 2.5, places=4)

    def test_below_threshold_is_silent(self):
        bars = _bars([10.0] * 20 + [10.2], volumes=[1000.0] * 20 + [1900.0])
        self.assertIsNone(detect_volume_spike(bars, min_volume_ratio=2.0, window=20))

    def test_needs_full_window(self):
        bars = _bars([10.0] * 10, volumes=[1000.0] * 10)
        self.assertIsNone(detect_volume_spike(bars, min_volume_ratio=2.0, window=20))


class BreakoutTest(unittest.TestCase):
    def test_breaks_prior_high(self):
        bars = _bars([10.0] * 20 + [10.5])
        draft = detect_breakout_20d(bars, window=20)
        self.assertIsNotNone(draft)
        self.assertEqual(draft.direction, "UP")
        self.assertEqual(draft.signal["mode"], "HIGH")

    def test_equal_high_is_silent(self):
        bars = _bars([10.0] * 21)
        self.assertIsNone(detect_breakout_20d(bars, window=20))

    def test_breaks_prior_low(self):
        bars = _bars([10.0] * 20 + [9.0], lows=[10.0] * 20 + [9.0])
        draft = detect_breakout_20d(bars, window=20)
        self.assertIsNotNone(draft)
        self.assertEqual(draft.direction, "DOWN")
        self.assertEqual(draft.signal["mode"], "LOW")


class MaCrossTest(unittest.TestCase):
    def test_golden_cross(self):
        draft = detect_ma_cross(_bars([10.0] * 20 + [15.0]), fast_window=5, slow_window=20)
        self.assertIsNotNone(draft)
        self.assertEqual(draft.direction, "UP")
        self.assertEqual(draft.event_type, "MA_CROSS_5_20")

    def test_death_cross(self):
        draft = detect_ma_cross(_bars([10.0] * 20 + [5.0]), fast_window=5, slow_window=20)
        self.assertIsNotNone(draft)
        self.assertEqual(draft.direction, "DOWN")

    def test_flat_series_is_silent(self):
        self.assertIsNone(detect_ma_cross(_bars([10.0] * 21), fast_window=5, slow_window=20))

    def test_needs_full_window(self):
        self.assertIsNone(detect_ma_cross(_bars([10.0] * 10), fast_window=5, slow_window=20))


class TextDetectorTest(unittest.TestCase):
    def test_notice_new_same_day(self):
        draft = detect_notice_new([(1, "2026-10-15")], event_date="2026-10-15")
        self.assertIsNotNone(draft)
        self.assertEqual(draft.source_ids["notice_ids"], [1])

    def test_notice_new_only_previous_day_is_silent(self):
        self.assertIsNone(detect_notice_new([(1, "2026-10-14")], event_date="2026-10-15"))

    def test_notice_cluster_inside_window(self):
        items = [(1, "2026-10-15"), (2, "2026-10-14"), (3, "2026-10-13")]
        draft = detect_notice_cluster(items, min_count=3, window_days=3, event_date="2026-10-15")
        self.assertIsNotNone(draft)
        self.assertEqual(draft.signal["notice_count"], 3)
        self.assertEqual(sorted(draft.source_ids["notice_ids"]), [1, 2, 3])

    def test_notice_cluster_outside_window_is_silent(self):
        items = [(1, "2026-10-15"), (2, "2026-10-14"), (3, "2026-10-12")]
        self.assertIsNone(detect_notice_cluster(items, min_count=3, window_days=3, event_date="2026-10-15"))

    def test_news_cluster_triggers(self):
        items = [(row_id, "2026-10-15") for row_id in range(5)]
        draft = detect_news_cluster(items, min_count=5, window_days=3, event_date="2026-10-15")
        self.assertIsNotNone(draft)
        self.assertEqual(draft.signal["news_count"], 5)

    def test_news_cluster_below_threshold_is_silent(self):
        items = [(row_id, "2026-10-15") for row_id in range(4)]
        self.assertIsNone(detect_news_cluster(items, min_count=5, window_days=3, event_date="2026-10-15"))

    def test_unparseable_dates_are_ignored(self):
        items = [(1, "not-a-date"), (2, "2026-10-15")]
        draft = detect_notice_cluster(items, min_count=2, window_days=3, event_date="2026-10-15")
        self.assertIsNone(draft)


class EvaluateCombinationTest(unittest.TestCase):
    def test_all_detectors_fire_on_synthetic_case(self):
        bars = _bars([10.0] * 20 + [10.6], volumes=[1000.0] * 20 + [3000.0])
        drafts = evaluate_symbol_changes(
            bars=bars,
            notice_items=[(1, "2026-10-15"), (2, "2026-10-15"), (3, "2026-10-13")],
            news_items=[],
            event_date="2026-10-15",
        )
        types = {draft.event_type for draft in drafts}
        self.assertEqual(
            types,
            {
                "PRICE_MOVE",
                "VOLUME_SPIKE",
                "BREAKOUT_20D",
                "MA_CROSS_5_20",
                "NOTICE_NEW",
                "NOTICE_CLUSTER",
            },
        )
        for draft in drafts:
            self.assertEqual(draft.event_date, "2026-10-15")


class ChangeDetectionDatabaseTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp_dir = TemporaryDirectory()
        db_path = Path(cls.temp_dir.name) / "test_change_detection.db"
        cls.engine = create_engine(f"sqlite:///{db_path}")
        cls.session_factory = sessionmaker(bind=cls.engine)

    @classmethod
    def tearDownClass(cls):
        cls.engine.dispose()
        if cls.temp_dir:
            cls.temp_dir.cleanup()

    def setUp(self):
        Base.metadata.drop_all(self.engine)
        Base.metadata.create_all(self.engine)

    def _seed(self, db):
        source = DataSource(source_code="TEST_SRC", source_name="测试数据源", adapter_type="TEST")
        db.add(source)
        db.flush()
        db.add(WatchlistItem(market="CN_A", symbol="000001", note="单测样本"))
        origin = date.fromisoformat("2026-09-25")
        for index, close in enumerate([10.0] * 20 + [10.6]):
            db.add(
                StockKline(
                    market="CN_A",
                    symbol="000001",
                    period="daily",
                    adjust="",
                    trade_date=(origin + timedelta(days=index)).isoformat(),
                    open_price=close,
                    high_price=close,
                    low_price=close,
                    close_price=close,
                    volume=3000.0 if index == 20 else 1000.0,
                    source_id=source.id,
                )
            )
        for index, day in enumerate(["2026-10-15", "2026-10-15", "2026-10-13"]):
            db.add(
                StockNotice(
                    market="CN_A",
                    symbol="000001",
                    notice_date=day,
                    title=f"测试公告 {index}",
                    source_id=source.id,
                )
            )
        db.commit()

    def test_run_creates_events_and_is_idempotent(self):
        with self.session_factory() as db:
            self._seed(db)
            first = run_change_detection(db, as_of="2026-10-15")
            self.assertEqual(first["events_created"], 6)
            self.assertEqual(first["duplicates"], 0)
            self.assertEqual(first["error_count"], 0)

            again = run_change_detection(db, as_of="2026-10-15")
            self.assertEqual(again["events_created"], 0)
            self.assertEqual(again["duplicates"], 6)

            rows = list(db.scalars(select(StockChangeEvent)).all())
            self.assertEqual(len(rows), 6)
            for row in rows:
                self.assertEqual(row.profile_version, "L0_PROFILE_V1")
                self.assertEqual(row.market, "CN_A")
                self.assertEqual(row.event_date, "2026-10-15")
                self.assertEqual(
                    row.dedup_key,
                    build_dedup_key("CN_A", "000001", row.event_type, "2026-10-15"),
                )

    def test_stale_bars_are_skipped(self):
        with self.session_factory() as db:
            self._seed(db)
            result = run_change_detection(db, as_of="2026-12-01", include_details=True)
            self.assertEqual(result["events_created"], 0)
            self.assertEqual(result["skipped_stale"], 1)
            self.assertEqual(result["symbols"][0]["status"], "SKIPPED_STALE")

    def test_include_details_classifies_symbols(self):
        with self.session_factory() as db:
            self._seed(db)
            db.add(WatchlistItem(market="CN_A", symbol="000002", note="无行情样本"))
            db.commit()
            result = run_change_detection(db, as_of="2026-10-15", include_details=True)
            by_key = {(entry["market"], entry["symbol"]): entry for entry in result["symbols"]}
            self.assertEqual(by_key[("CN_A", "000001")]["status"], "EVENTS")
            self.assertEqual(
                by_key[("CN_A", "000001")]["event_types"],
                ["BREAKOUT_20D", "MA_CROSS_5_20", "NOTICE_CLUSTER", "NOTICE_NEW", "PRICE_MOVE", "VOLUME_SPIKE"],
            )
            self.assertEqual(by_key[("CN_A", "000002")]["status"], "SKIPPED_NO_KLINE")

    def test_as_of_limits_bars(self):
        with self.session_factory() as db:
            self._seed(db)
            # as_of one day before the last bar: only 20 bars visible, no spike day
            result = run_change_detection(db, as_of="2026-10-14")
            self.assertEqual(result["events_created"], 0)

    def test_empty_pool_is_safe(self):
        with self.session_factory() as db:
            result = run_change_detection(db, as_of="2026-10-15")
            self.assertEqual(result["scanned"], 0)
            self.assertEqual(result["events_created"], 0)


class ChangeEventApiTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp_dir = TemporaryDirectory()
        db_path = Path(cls.temp_dir.name) / "test_change_event_api.db"
        cls.engine = create_engine(f"sqlite:///{db_path}")
        Base.metadata.create_all(cls.engine)
        cls.session_factory = sessionmaker(bind=cls.engine)

    @classmethod
    def tearDownClass(cls):
        cls.engine.dispose()
        if cls.temp_dir:
            cls.temp_dir.cleanup()

    def test_list_endpoint_returns_events(self):
        with self.session_factory() as db:
            db.add(
                StockChangeEvent(
                    market="CN_A",
                    symbol="000001",
                    event_date="2026-10-15",
                    event_type="PRICE_MOVE",
                    direction="UP",
                    signal_json={"change_pct": 4.2},
                    source_ids_json={},
                    dedup_key="CN_A:000001:PRICE_MOVE:2026-10-15",
                    profile_version="L0_PROFILE_V1",
                    status="DETECTED",
                )
            )
            db.commit()

        def override_get_db():
            db = self.session_factory()
            try:
                yield db
            finally:
                db.close()

        app.dependency_overrides[get_db] = override_get_db
        try:
            client = TestClient(app)
            response = client.get(
                "/api/v1/change-events",
                params={"market": "CN_A", "event_type": "PRICE_MOVE"},
            )
            self.assertEqual(response.status_code, 200)
            payload = response.json()
            self.assertEqual(len(payload), 1)
            self.assertEqual(payload[0]["symbol"], "000001")
            self.assertEqual(payload[0]["direction"], "UP")
            self.assertEqual(payload[0]["signal_json"]["change_pct"], 4.2)
            self.assertEqual(payload[0]["profile_version"], "L0_PROFILE_V1")

            empty = client.get("/api/v1/change-events", params={"market": "HK"})
            self.assertEqual(empty.status_code, 200)
            self.assertEqual(empty.json(), [])
        finally:
            app.dependency_overrides.clear()


if __name__ == "__main__":
    unittest.main()
