"""Evidence and connection boundaries independent of live providers."""
from tempfile import TemporaryDirectory
from pathlib import Path
from types import SimpleNamespace
import unittest
from unittest.mock import patch

from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session

from app.db.base import Base
from app.models.market_data import DataSource, StockF10Cache, StockSymbol, StockRealtimeQuote
from app.connectors.base import QuoteRecord
from app.services.stock_on_demand import StockOnDemandService
from app.services.stock_classification import enrich_classification_groups_with_members


class IngestionReadBoundaryTest(unittest.TestCase):
    def setUp(self):
        self.temp = TemporaryDirectory()
        self.engine = create_engine(f"sqlite:///{Path(self.temp.name) / 'test.db'}", pool_size=1, max_overflow=0)
        Base.metadata.create_all(self.engine)
        self.db = Session(self.engine)
        self.source = DataSource(source_code="BOUNDARY", source_name="边界测试", adapter_type="MOCK")
        self.db.add(self.source)
        self.db.flush()
        self.db.add(StockSymbol(market="CN_A", symbol="000001", exchange="SZSE", name="平安银行", source_id=self.source.id))
        self.db.commit()
        self.service = StockOnDemandService(self.db)

    def tearDown(self):
        self.db.close()
        self.engine.dispose()
        self.temp.cleanup()

    def test_stale_source_snapshot_is_archived_without_replacing_newer_cache(self):
        new = {"rows": [{"日期": "2026-10-08", "主力净流入": 2}]}
        old = {"rows": [{"日期": "2026-09-01", "主力净流入": 1}]}
        with patch("app.services.lakehouse.archive_ingestion_payload") as archive:
            self.service.upsert_f10_cache(self.source, "CN_A", "000001", "fund_flow", new)
            archive.assert_not_called()  # Derived/local persistence is not RAW.
            archive.return_value = {"object_id": 101, "batch_id": "source-old"}
            self.service.upsert_f10_cache(self.source, "CN_A", "000001", "fund_flow", old, archive_payload=old)
            received = archive.call_args.kwargs["records"][0]
            self.assertEqual(received["payload"], old)
            self.assertEqual(received["payload_kind"], "CONNECTOR_SNAPSHOT")
            saved = self.service.load_f10_cache("CN_A", "000001")["fund_flow"]
            self.assertEqual(saved["rows"], new["rows"])
            self.assertEqual(saved["_meta"]["raw_archive"]["object_id"], 101)

    def test_archive_failure_preserves_online_cache_and_records_failure(self):
        payload = {"fields": {"公司名称": "平安银行"}}
        with patch("app.services.lakehouse.archive_ingestion_payload", side_effect=OSError("对象存储不可达")):
            self.service.upsert_f10_cache(self.source, "CN_A", "000001", "profile", payload, archive_payload=payload)
        saved = self.db.scalar(select(StockF10Cache)).payload_json
        self.assertEqual(saved["fields"], payload["fields"])
        self.assertEqual(saved["_meta"]["raw_archive"]["status"], "FAILED")
        self.assertNotIn("object_id", saved["_meta"]["raw_archive"])

    def test_remote_fetches_release_pool_and_still_persist_quote(self):
        def empty(**kwargs):
            self.assertEqual(self.engine.pool.checkedout(), 0, "远程网络调用期间不应占用连接")
            return []
        def quote(**kwargs):
            self.assertEqual(self.engine.pool.checkedout(), 0)
            return QuoteRecord(market="CN_A", symbol="000001", current_price=10, quote_time="2026-10-08 15:00:00",
                               previous_close_price=9, open_price=9, high_price=10, low_price=9,
                               volume=100, amount=1000, change_amount=1, change_pct=11.11,
                               turnover_rate=1, raw_payload={"source": "TEST"})
        adapter = SimpleNamespace(fetch_kline=empty, fetch_financial_indicators=empty,
                                  fetch_notices=empty, fetch_news=empty, fetch_realtime_quote=quote)
        common = dict(source=self.source, market="CN_A", symbol="000001", persist=True)
        with patch("app.services.stock_on_demand.get_adapter", return_value=adapter), \
             patch("app.services.lakehouse.archive_ingestion_payload", return_value={"object_id": 1, "batch_id": "test"}):
            calls = [
                lambda: self.service.fetch_kline(**common, period="daily", adjust="", start_date="20260101", end_date="20261008"),
                lambda: self.service.fetch_financials(**common, indicator="按报告期"),
                lambda: self.service.fetch_notices(**common, start_date="20260101", end_date="20261008"),
                lambda: self.service.fetch_news(**common),
                lambda: self.service.fetch_quote(**common),
            ]
            for call in calls:
                log, rows = call()
                self.assertEqual(log.status, "SUCCESS")
        self.assertEqual(self.db.scalar(select(StockRealtimeQuote)).current_price, 10)

    def test_deferred_members_keep_full_statistics_and_full_read_contract(self):
        members = {("CN_A", str(i).zfill(6)): {"market": "CN_A", "symbol": str(i).zfill(6),
                   "name": f"测试{i}", "change_pct": i} for i in range(8)}
        def load(db, groups, buckets):
            buckets[("theme", "BKTEST")] = {k: dict(v) for k, v in members.items()}
        def groups():
            return [{"key": "theme", "items": [{"code": "BKTEST", "name": "测试概念"}]}]
        with patch("app.services.stock_classification._load_reviewed_members", side_effect=load), \
             patch("app.services.stock_classification._load_foundation_members"), \
             patch("app.services.stock_classification._fetch_remote_board_members", side_effect=AssertionError("只读禁止远程")):
            preview = enrich_classification_groups_with_members(self.db, groups(), include_members=False)[0]["items"][0]
            full = enrich_classification_groups_with_members(self.db, groups())[0]["items"][0]
        self.assertEqual(preview["member_count"], 8)
        self.assertEqual(preview["quote_observed_count"], 8)
        self.assertEqual(preview["trend_pct"], full["trend_pct"])
        self.assertEqual(len(preview["related_stocks"]), 5)
        self.assertTrue(preview["members_deferred"])
        self.assertEqual(len(full["related_stocks"]), 8)
        self.assertFalse(full["members_deferred"])


if __name__ == "__main__":
    unittest.main()
