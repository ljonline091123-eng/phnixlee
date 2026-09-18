import unittest
from datetime import datetime, timezone
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import patch

from fastapi.testclient import TestClient
from sqlalchemy import create_engine, select
from sqlalchemy.orm import sessionmaker

from app.api.stocks import get_stock_f10
from app.db.base import Base
from app.db.session import get_db
from app.main import app
from app.models.market_data import DataSource, StockF10Cache, StockKline, StockNews, StockNotice, StockRealtimeQuote, StockSymbol
from app.services.catalog import seed_default_catalog
from app.services.model_hub import seed_default_models
from app.services.stock_on_demand import StockOnDemandService
from app.services.stock_tools import StockToolsService


class StockToolsFoundationTest(unittest.TestCase):
    temp_dir: TemporaryDirectory[str] | None = None

    @classmethod
    def setUpClass(cls) -> None:
        cls.temp_dir = TemporaryDirectory()
        cls.db_path = Path(cls.temp_dir.name) / "test_quant_tools.db"
        cls.engine = create_engine(f"sqlite:///{cls.db_path}")
        Base.metadata.create_all(cls.engine)
        cls.session_factory = sessionmaker(bind=cls.engine)

    @classmethod
    def tearDownClass(cls) -> None:
        cls.engine.dispose()
        if cls.temp_dir:
            cls.temp_dir.cleanup()

    def setUp(self) -> None:
        self.db = self.session_factory()
        # Keep API tests isolated from the developer's running database and
        # configured provider keys.  Otherwise this test can spend credits on
        # a real model instead of exercising the seeded mock provider.
        app.dependency_overrides[get_db] = lambda: self.db
        Base.metadata.drop_all(self.engine)
        Base.metadata.create_all(self.engine)
        seed_default_catalog(self.db)
        seed_default_models(self.db)
        source = self.db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE"))
        stock = StockSymbol(
            market="CN_A",
            symbol="000001",
            exchange="SZSE",
            name="平安银行",
            asset_type="STOCK",
            status="LISTED",
            list_date="19910403",
            source_id=source.id,
            ext_json={},
            raw_payload={},
            last_synced_at=datetime.now(timezone.utc),
        )
        self.db.add(stock)
        self.db.add(
            StockRealtimeQuote(
                market="CN_A",
                symbol="000001",
                quote_time="2026-09-03T11:30:00",
                current_price=11.98,
                previous_close_price=11.91,
                open_price=11.88,
                high_price=12.06,
                low_price=11.83,
                volume=704621,
                amount=846996918,
                change_amount=0.07,
                change_pct=0.59,
                turnover_rate=5.35,
                source_id=source.id,
                raw_payload={},
                fetched_at=datetime.now(timezone.utc),
            )
        )
        self.db.add(
            StockKline(
                market="CN_A",
                symbol="000001",
                period="daily",
                adjust="",
                trade_date="2026-09-02",
                open_price=11.88,
                high_price=12.06,
                low_price=11.83,
                close_price=11.98,
                volume=704621,
                amount=846996918,
                turnover_rate=5.35,
                source_id=source.id,
                raw_payload={},
                fetched_at=datetime.now(timezone.utc),
            )
        )
        self.db.add(
            StockNews(
                market="CN_A",
                symbol="000001",
                news_time="2026-09-03T09:30:00",
                title="平安银行发布最新经营动态",
                content="测试新闻内容",
                source_name="测试来源",
                url="https://example.com/news",
                content_json={},
                source_id=source.id,
                fetched_at=datetime.now(timezone.utc),
            )
        )
        self.db.add(
            StockNotice(
                market="CN_A",
                symbol="000001",
                notice_date="2026-09-03",
                title="平安银行公告测试",
                notice_type="公告",
                url="https://example.com/notice",
                content_json={},
                source_id=source.id,
                fetched_at=datetime.now(timezone.utc),
            )
        )
        self.db.commit()

    def tearDown(self) -> None:
        app.dependency_overrides.pop(get_db, None)
        self.db.close()

    def test_stock_tools_catalog_is_exposed(self) -> None:
        with TestClient(app) as client:
            response = client.get("/api/v1/stock-tools")
            self.assertEqual(response.status_code, 200)
            names = {item["name"] for item in response.json()}
            self.assertTrue({"get_realtime_quote", "get_historical_kline", "get_latest_news", "get_latest_notices"}.issubset(names))

    def test_stock_analysis_uses_mock_model(self) -> None:
        with TestClient(app) as client:
            response = client.post(
                "/api/v1/stock-tools/analyze",
                json={
                    "market": "CN_A",
                    "symbol": "000001",
                    "refresh": False,
                    "question": "请给出简短分析",
                },
            )
            self.assertEqual(response.status_code, 200)
            payload = response.json()
            self.assertEqual(payload["status"], "SUCCESS")
            self.assertIn("已收到", payload["response_text"])
            self.assertIn("get_realtime_quote", payload["tool_results"])


    def test_f10_snapshot_prefers_local_cache_without_refresh(self) -> None:
        source = self.db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE"))
        self.db.add(
            StockF10Cache(
                market="CN_A",
                symbol="000001",
                section="profile",
                source_id=source.id,
                payload_json={"source": "unit-test", "fields": {"company": "Ping An Bank"}},
                fetched_at=datetime.now(timezone.utc),
            )
        )
        self.db.commit()

        with patch("app.services.stock_tools.get_adapter") as mocked_get_adapter:
            snapshot = StockToolsService(self.db).get_f10_snapshot("CN_A", "000001", persist=False)

        mocked_get_adapter.assert_not_called()
        self.assertEqual(snapshot.profile["fields"]["company"], "Ping An Bank")

    def test_f10_refresh_returns_rows_written_during_refresh(self) -> None:
        source = self.db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE"))

        def fake_refresh(
            service: StockOnDemandService,
            source: DataSource,
            market: str,
            symbol: str,
            list_date: str | None = None,
        ) -> list[str]:
            quote = service.db.scalar(
                select(StockRealtimeQuote).where(
                    StockRealtimeQuote.market == market,
                    StockRealtimeQuote.symbol == symbol,
                )
            )
            quote.current_price = 12.34
            quote.quote_time = "2026-09-06T10:00:00"
            service.db.add(
                StockNotice(
                    market=market,
                    symbol=symbol,
                    notice_date="2026-09-06",
                    title="刷新后公告",
                    notice_type="公告",
                    url="https://example.com/refreshed-notice",
                    content_json={},
                    source_id=source.id,
                    fetched_at=datetime.now(timezone.utc),
                )
            )
            service.db.commit()
            return []

        with patch.object(
            StockOnDemandService,
            "refresh_stock_data",
            autospec=True,
            side_effect=fake_refresh,
        ), patch("app.api.stocks._fetch_and_cache_f10_extended_data", return_value={}):
            snapshot = get_stock_f10(
                market="CN_A",
                symbol="000001",
                refresh=True,
                db=self.db,
            )

        self.assertIsNotNone(snapshot.realtime_quote)
        self.assertEqual(snapshot.realtime_quote.current_price, 12.34)
        self.assertEqual(snapshot.notices[0].title, "刷新后公告")

    def test_f10_local_only_does_not_trigger_remote_refresh(self) -> None:
        with patch.object(StockOnDemandService, "refresh_stock_data") as mocked_refresh, patch(
            "app.api.stocks._fetch_and_cache_f10_extended_data"
        ) as mocked_f10:
            snapshot = get_stock_f10(
                market="CN_A",
                symbol="000001",
                local_only=True,
                db=self.db,
            )

        mocked_refresh.assert_not_called()
        mocked_f10.assert_not_called()
        self.assertIsNotNone(snapshot.realtime_quote)
        self.assertGreater(len(snapshot.news), 0)

    def test_hk_f10_refresh_then_reopen_keeps_latest_snapshot(self) -> None:
        source = self.db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE"))
        self.db.add(
            StockSymbol(
                market="HK",
                symbol="00001",
                exchange="HKEX",
                name="长和",
                asset_type="STOCK",
                status="LISTED",
                list_date="1972-11-01",
                source_id=source.id,
                ext_json={},
                raw_payload={},
                last_synced_at=datetime.now(timezone.utc),
            )
        )
        self.db.add(
            StockF10Cache(
                market="HK",
                symbol="00001",
                section="published_reports",
                source_id=source.id,
                payload_json={
                    "source": "local",
                    "reports": [
                        {
                            "report_name": "旧：2025年年报",
                            "report_type": "年报",
                            "report_date": "2025-12-31",
                            "notice_date": "2026-04-17",
                            "url": "https://example.com/old.pdf",
                            "source_name": "local",
                            "title": "old",
                        }
                    ],
                },
                fetched_at=datetime.now(timezone.utc),
            )
        )
        self.db.add(
            StockF10Cache(
                market="HK",
                symbol="00001",
                section="fund_flow",
                source_id=source.id,
                payload_json={"source": "local", "rows": [{"date": "2026-08-01", "value": 1}]},
                fetched_at=datetime.now(timezone.utc),
            )
        )
        self.db.commit()

        def fake_refresh(
            service: StockOnDemandService,
            source: DataSource,
            market: str,
            symbol: str,
            list_date: str | None = None,
        ) -> list[str]:
            report = service.db.scalar(
                select(StockF10Cache).where(
                    StockF10Cache.market == market,
                    StockF10Cache.symbol == symbol,
                    StockF10Cache.section == "published_reports",
                )
            )
            if report:
                report.payload_json = {
                    "source": "HKEXnews",
                    "reports": [
                        {
                            "report_name": "新：2026年半年报",
                            "report_type": "半年报",
                            "report_date": "2026-06-30",
                            "notice_date": "2026-08-27",
                            "url": "https://example.com/new.pdf",
                            "source_name": "HKEXnews",
                            "title": "2026年中期报告",
                        }
                    ],
                }
                report.fetched_at = datetime.now(timezone.utc)
            fund = service.db.scalar(
                select(StockF10Cache).where(
                    StockF10Cache.market == market,
                    StockF10Cache.symbol == symbol,
                    StockF10Cache.section == "fund_flow",
                )
            )
            if fund:
                fund.payload_json = {"source": "local", "rows": [{"date": "2026-09-06", "value": 2}]}
                fund.fetched_at = datetime.now(timezone.utc)
            stock = service.db.scalar(
                select(StockSymbol).where(
                    StockSymbol.market == market,
                    StockSymbol.symbol == symbol,
                )
            )
            if stock:
                stock.last_synced_at = datetime.now(timezone.utc)
            service.db.commit()
            return []

        with patch.object(
            StockOnDemandService,
            "refresh_stock_data",
            autospec=True,
            side_effect=fake_refresh,
        ), patch("app.api.stocks._fetch_and_cache_f10_extended_data", return_value={}):
            refreshed = get_stock_f10(market="HK", symbol="00001", refresh=True, db=self.db)
            reopened = get_stock_f10(market="HK", symbol="00001", refresh=False, db=self.db)

        self.assertEqual(refreshed.published_reports["reports"][0]["report_date"], "2026-06-30")
        self.assertEqual(reopened.published_reports["reports"][0]["report_date"], "2026-06-30")
        self.assertEqual(refreshed.fund_flow["rows"][0]["date"], "2026-09-06")
        self.assertEqual(reopened.fund_flow["rows"][0]["date"], "2026-09-06")


if __name__ == "__main__":
    unittest.main()
