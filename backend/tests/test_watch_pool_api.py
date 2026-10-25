"""API tests for the watch-pool surface (status / refresh / detect / jobs)."""

import unittest
from datetime import datetime
from pathlib import Path
from tempfile import TemporaryDirectory

import app.models  # noqa: F401 - register every ORM model in Base.metadata
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, select
from sqlalchemy.orm import sessionmaker

from app.db.base import Base
from app.db.session import get_db
from app.main import app
from app.models.market_data import DataSource, StockKline, WatchlistItem
from app.models.pipeline import ScheduledJob


class WatchPoolApiTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp_dir = TemporaryDirectory()
        db_path = Path(cls.temp_dir.name) / "test_watch_pool_api.db"
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

        def override_get_db():
            db = self.session_factory()
            try:
                yield db
            finally:
                db.close()

        app.dependency_overrides[get_db] = override_get_db
        self.client = TestClient(app)

    def tearDown(self):
        app.dependency_overrides.clear()

    def _seed_pool(self):
        today = datetime.now().date().isoformat()
        with self.session_factory() as db:
            source = DataSource(source_code="TEST_SRC", source_name="测试数据源", adapter_type="TEST")
            db.add(source)
            db.flush()
            db.add(WatchlistItem(market="CN_A", symbol="000001", note="API 样本"))
            db.add(
                StockKline(
                    market="CN_A",
                    symbol="000001",
                    period="daily",
                    adjust="",
                    trade_date=today,
                    open_price=10.0,
                    high_price=10.0,
                    low_price=10.0,
                    close_price=10.0,
                    volume=1000.0,
                    source_id=source.id,
                )
            )
            db.commit()

    def test_status_empty_pool(self):
        response = self.client.get("/api/v1/watch-pool/status")
        self.assertEqual(response.status_code, 200)
        payload = response.json()
        self.assertEqual(payload["summary"]["pool_size"], 0)
        self.assertEqual(payload["rows"], [])

    def test_status_reports_fresh_symbol(self):
        self._seed_pool()
        response = self.client.get("/api/v1/watch-pool/status")
        self.assertEqual(response.status_code, 200)
        payload = response.json()
        self.assertEqual(payload["summary"]["pool_size"], 1)
        self.assertEqual(payload["summary"]["fresh"], 1)
        row = payload["rows"][0]
        self.assertEqual((row["market"], row["symbol"]), ("CN_A", "000001"))
        self.assertEqual(row["lag_days"], 0)
        self.assertEqual(row["status"], "FRESH")

        filtered = self.client.get("/api/v1/watch-pool/status", params={"market": "HK"})
        self.assertEqual(filtered.json()["summary"]["pool_size"], 0)

    def test_submit_refresh_creates_job(self):
        response = self.client.post("/api/v1/watch-pool/refresh", json={"market": "CN_A"})
        self.assertEqual(response.status_code, 202)
        job = response.json()
        self.assertEqual(job["task_type"], "watch_pool_data_refresh")
        self.assertEqual(job["status"], "PENDING")
        self.assertEqual(job["payload"]["market"], "CN_A")
        self.assertFalse(job["payload"]["chain_detection"])

        status_response = self.client.get(f"/api/v1/watch-pool/jobs/{job['id']}")
        self.assertEqual(status_response.status_code, 200)
        self.assertEqual(status_response.json()["id"], job["id"])

        listing = self.client.get("/api/v1/watch-pool/jobs")
        self.assertEqual(listing.status_code, 200)
        self.assertEqual(len(listing.json()), 1)

    def test_submit_detection_without_body(self):
        response = self.client.post("/api/v1/watch-pool/detect")
        self.assertEqual(response.status_code, 202)
        job = response.json()
        self.assertEqual(job["task_type"], "l0_change_detection")
        self.assertEqual(job["status"], "PENDING")

        with self.session_factory() as db:
            stored = db.scalar(select(ScheduledJob).where(ScheduledJob.id == job["id"]))
            self.assertIsNotNone(stored)
            self.assertTrue(stored.idempotency_key.startswith("l0_change_detection:api:"))

    def test_job_not_found(self):
        response = self.client.get("/api/v1/watch-pool/jobs/999999")
        self.assertEqual(response.status_code, 404)


if __name__ == "__main__":
    unittest.main()
