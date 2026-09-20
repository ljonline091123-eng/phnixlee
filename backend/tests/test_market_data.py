import unittest
from pathlib import Path
from tempfile import TemporaryDirectory
from datetime import date, datetime, timezone
from unittest.mock import patch

from fastapi import HTTPException
import pandas as pd
from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session, sessionmaker

from app.connectors.akshare_adapter import AkshareAdapter
from app.connectors.base import KlineRecord, SymbolRecord
from app.api.stocks import (
    add_watchlist,
    _hk_published_reports_need_refresh,
    _needs_f10_refresh,
    _resolve_watch_symbol,
    search_stock_symbols,
    _validate_watch_symbol,
)
from app.db.base import Base
from app.models.market_data import DataSource, StockKline, StockSymbol
from app.schemas.market_data import WatchlistItemCreate
from app.services.catalog import seed_default_catalog
from app.services.ipo_calendar import IpoCalendarService
from app.services.stock_on_demand import StockOnDemandService
from app.services.stock_sync import StockSyncService


class MarketDataFoundationTest(unittest.TestCase):
    temp_dir: TemporaryDirectory[str] | None = None
    db_path: Path | None = None

    @classmethod
    def setUpClass(cls) -> None:
        cls.temp_dir = TemporaryDirectory()
        cls.db_path = Path(cls.temp_dir.name) / "test_quant.db"
        cls.engine = create_engine(f"sqlite:///{cls.db_path}")
        Base.metadata.create_all(cls.engine)
        cls.session_factory = sessionmaker(bind=cls.engine)

    @classmethod
    def tearDownClass(cls) -> None:
        cls.engine.dispose()
        if cls.temp_dir:
            cls.temp_dir.cleanup()

    def setUp(self) -> None:
        self.db: Session = self.session_factory()
        seed_default_catalog(self.db)

    def tearDown(self) -> None:
        self.db.close()

    def test_default_catalog_is_seeded(self) -> None:
        source = self.db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE"))
        self.assertIsNotNone(source)
        self.assertEqual(source.adapter_type, "AKSHARE")
        self.assertEqual(len(source.interfaces), 8)
        fallback = self.db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE_HK_SINA"))
        self.assertIsNotNone(fallback)
        self.assertEqual(source.config_json["fallback_source_code"], fallback.source_code)

    def test_realtime_quote_and_news_helpers_exist(self) -> None:
        adapter = AkshareAdapter()
        quote = adapter._parse_tencent_quote(
            'v_sh688825="1~长鑫科技~688825~54.32~56.50~55.55~243949672~115076504~128873168~54.32~128~54.31~1039~54.30~6108~54.29~116~54.28~2614~54.33~1081~54.34~23~54.35~80~54.36~399~54.37~469~~20260902161438~-2.18~-3.86~55.88~54.00~54.32/243949672/13357687463~243949672~1335769~5.42~45.07~~55.88~54.00~3.33~2446.05~36874.64~18.34~67.80~45.20~0.91~7953~54.76~23.76~1966.80~~~1.96~1335768.7463~1734.3018~319275~A RA~GP-A-KCB~527.25~-3.17~0.00~60.73~25.38~61.80~38.11~-5.61~0.04~527.25~4503038971~67884099077~65.96~527.25~4503038971~~~527.25~-0.11~~CNY~0~___D__F__NY~54.26~5205~100";',
            "HK",
            "00700",
        )
        self.assertIsNotNone(quote.current_price)
        news = adapter._normalize_news_dataframe(
            pd.DataFrame(
                [
                    {
                        "关键词": "000001",
                        "新闻标题": "测试标题",
                        "新闻内容": "测试正文",
                        "发布时间": "2026-08-17 10:47:00",
                        "文章来源": "测试来源",
                        "新闻链接": "http://example.com",
                    }
                ]
            ),
            "CN_A",
            "000001",
        )
        self.assertEqual(news[0].title, "测试标题")

    def test_hkex_notice_rows_are_normalized(self) -> None:
        adapter = AkshareAdapter()
        notices = adapter._normalize_hkex_notice_rows(
            [
                {
                    "TITLE": "截至2026年6月30日止六個月中期業績公告",
                    "DATE_TIME": "31/08/2026 12:19",
                    "FILE_LINK": "/listedco/listconews/sehk/2026/0831/2026083100442_c.pdf",
                    "SHORT_TEXT": "公告及通告 - [中期業績]<br/>",
                }
            ],
            symbol="02533",
        )
        self.assertEqual(len(notices), 1)
        self.assertEqual(notices[0].market, "HK")
        self.assertEqual(notices[0].notice_date, "2026-08-31")
        self.assertTrue((notices[0].url or "").endswith("2026083100442_c.pdf"))
        self.assertIn("中期業績", notices[0].notice_type or "")

    def test_cninfo_notice_rows_are_normalized(self) -> None:
        adapter = AkshareAdapter()
        notices = adapter._normalize_notice_rows(
            [
                {
                    "secCode": "000008",
                    "secName": "神州高铁",
                    "orgId": "gssz0000008",
                    "announcementId": "1225525830",
                    "announcementTitle": "关于召开2026年半年度网上业绩说明会的公告",
                    "announcementTime": 1787932800000,
                    "adjunctUrl": "finalpage/2026-08-29/1225525830.PDF",
                    "announcementTypeName": None,
                }
            ],
            market="CN_A",
            symbol="000008",
        )
        self.assertEqual(len(notices), 1)
        self.assertEqual(notices[0].market, "CN_A")
        self.assertEqual(notices[0].notice_date, "2026-08-29")
        self.assertEqual(notices[0].notice_type, "公告")
        self.assertIn("cninfo.com.cn/new/disclosure/detail", notices[0].url or "")
        self.assertIn("announcementId=1225525830", notices[0].url or "")

    def test_cninfo_market_params_follow_exchange_code(self) -> None:
        self.assertEqual(AkshareAdapter._cninfo_market_params("000008"), ("szse", "sz"))
        self.assertEqual(AkshareAdapter._cninfo_market_params("600000"), ("sse", "sh"))
        self.assertEqual(AkshareAdapter._cninfo_market_params("830000"), ("bjse", "bj"))

    def test_hkex_stock_id_lookup_uses_internal_id_field(self) -> None:
        adapter = AkshareAdapter()
        with patch.object(AkshareAdapter, "_hkex_stock_index", return_value={"02533": "1000221013"}):
            self.assertEqual(adapter._lookup_hkex_stock_id("02533"), "1000221013")

    def test_a_share_and_hk_records_are_standardized(self) -> None:
        adapter = AkshareAdapter()
        a_records = adapter._normalize_dataframe(
            pd.DataFrame([{"code": 1, "name": "Ping An Bank"}]),
            "CN_A",
        )
        hk_records = adapter._normalize_dataframe(
            pd.DataFrame([{"代码": 5, "名称": "HSBC Holdings"}]),
            "HK",
        )
        self.assertEqual(a_records[0].symbol, "000001")
        self.assertEqual(a_records[0].exchange, "SZSE")
        self.assertEqual(hk_records[0].symbol, "00005")
        self.assertEqual(hk_records[0].exchange, "HKEX")

    def test_symbol_upsert_updates_existing_master_data(self) -> None:
        source = self.db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE"))
        service = StockSyncService(self.db)
        first_record = SymbolRecord(
            market="CN_A",
            symbol="000001",
            exchange="SZSE",
            name="Ping An Bank",
            asset_type="STOCK",
            status="LISTED",
            list_date=None,
            ext_json={},
            raw_payload={"name": "Ping An Bank"},
        )
        inserted, updated = service._upsert_records(source.id, [first_record])
        self.db.commit()
        self.assertEqual((inserted, updated), (1, 0))

        second_record = SymbolRecord(
            market="CN_A",
            symbol="000001",
            exchange="SZSE",
            name="Ping An Bank Updated",
            asset_type="STOCK",
            status="LISTED",
            list_date=None,
            ext_json={"verified_at": datetime.now(timezone.utc).isoformat()},
            raw_payload={"name": "Ping An Bank Updated"},
        )
        inserted, updated = service._upsert_records(source.id, [second_record])
        self.db.commit()
        saved = self.db.scalar(select(StockSymbol).where(StockSymbol.symbol == "000001"))
        self.assertEqual((inserted, updated), (0, 1))
        self.assertEqual(saved.name, "Ping An Bank Updated")

    def test_symbol_upsert_keeps_newer_local_master_data(self) -> None:
        source = self.db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE"))
        service = StockSyncService(self.db)
        initial_record = SymbolRecord(
            market="CN_A",
            symbol="688888",
            exchange="SSE",
            name="本地新主数据",
            asset_type="STOCK",
            status="LISTED",
            list_date=None,
            ext_json={},
            raw_payload={"updated_at": "2026-09-10T10:00:00+00:00"},
        )
        inserted, updated = service._upsert_records(source.id, [initial_record])
        self.db.commit()
        saved = self.db.scalar(select(StockSymbol).where(StockSymbol.symbol == "688888"))
        saved.last_synced_at = datetime(2026, 9, 10, 10, 0, tzinfo=timezone.utc)
        self.db.commit()
        self.assertEqual((inserted, updated), (1, 0))

        older_remote = SymbolRecord(
            market="CN_A",
            symbol="688888",
            exchange="SSE",
            name="远端旧主数据",
            asset_type="STOCK",
            status="LISTED",
            list_date=None,
            ext_json={},
            raw_payload={"updated_at": "2026-09-01T10:00:00+00:00"},
        )
        inserted, updated = service._upsert_records(source.id, [older_remote])
        self.db.commit()
        saved = self.db.scalar(select(StockSymbol).where(StockSymbol.symbol == "688888"))
        self.assertEqual((inserted, updated), (0, 0))
        self.assertEqual(saved.name, "本地新主数据")

        newer_remote = SymbolRecord(
            market="CN_A",
            symbol="688888",
            exchange="SSE",
            name="远端新主数据",
            asset_type="STOCK",
            status="LISTED",
            list_date=None,
            ext_json={},
            raw_payload={"updated_at": "2026-09-11T10:00:00+00:00"},
        )
        inserted, updated = service._upsert_records(source.id, [newer_remote])
        self.db.commit()
        saved = self.db.scalar(select(StockSymbol).where(StockSymbol.symbol == "688888"))
        self.assertEqual((inserted, updated), (0, 1))
        self.assertEqual(saved.name, "远端新主数据")

    def test_kline_upsert_updates_existing_data(self) -> None:
        source = self.db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE"))
        service = StockOnDemandService(self.db)
        record = KlineRecord(
            market="CN_A",
            symbol="000001",
            period="daily",
            adjust="",
            trade_date="2024-01-02",
            open_price=9.39,
            high_price=9.42,
            low_price=9.21,
            close_price=9.21,
            volume=1158366,
            amount=1075742000,
            turnover_rate=0.6,
            raw_payload={"close": 9.21},
        )
        self.assertEqual(service._upsert_klines(source.id, [record]), 1)
        self.db.commit()

        updated_record = KlineRecord(
            **{**record.__dict__, "close_price": 9.22, "raw_payload": {"close": 9.22}}
        )
        self.assertEqual(service._upsert_klines(source.id, [updated_record]), 1)
        self.db.commit()
        saved = self.db.scalar(select(StockKline).where(StockKline.symbol == "000001"))
        self.assertEqual(saved.close_price, 9.22)

    def test_f10_cache_keeps_newer_local_payload_until_remote_is_newer(self) -> None:
        source = self.db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE"))
        service = StockOnDemandService(self.db)
        service.upsert_f10_cache(
            source,
            "CN_A",
            "000001",
            "fund_flow",
            {"source": "local", "rows": [{"日期": "2026-09-10", "主力净流入": 2}]},
        )

        service.upsert_f10_cache(
            source,
            "CN_A",
            "000001",
            "fund_flow",
            {"source": "remote-old", "rows": [{"日期": "2026-09-01", "主力净流入": 1}]},
        )
        saved = service.load_f10_cache("CN_A", "000001")["fund_flow"]
        self.assertEqual(saved["rows"][0]["日期"], "2026-09-10")

        service.upsert_f10_cache(
            source,
            "CN_A",
            "000001",
            "fund_flow",
            {"source": "remote-new", "rows": [{"日期": "2026-09-11", "主力净流入": 3}]},
        )
        saved = service.load_f10_cache("CN_A", "000001")["fund_flow"]
        self.assertEqual(saved["rows"][0]["日期"], "2026-09-11")

    def test_api_endpoints_load_with_seeded_catalog(self) -> None:
        from fastapi.testclient import TestClient
        from app.db.session import get_db
        from app.main import app

        with patch.dict(app.dependency_overrides, {get_db: lambda: self.db}):
            client = TestClient(app)
            try:
                response = client.get("/health")
                self.assertEqual(response.status_code, 200)
                response = client.get("/api/v1/data-sources")
                self.assertEqual(response.status_code, 200)
                self.assertGreaterEqual(len(response.json()), 1)
            finally:
                client.close()

    def test_cn_fund_flow_payload_is_normalized(self) -> None:
        adapter = AkshareAdapter()
        payload = {
            "data": {
                "klines": [
                    "2026-09-04,-69603804.0,22862342.0,46741456.0,-76935841.0,7332037.0,-7.18,2.36,4.82,-7.93,0.76,11.89,0.08,0.00,0.00",
                    "2026-09-03,-1074306.0,-25201632.0,26275936.0,43992208.0,-45066514.0,-0.08,-1.90,1.98,3.32,-3.40,11.88,-0.25,0.00,0.00",
                ]
            }
        }
        rows = adapter._build_cn_fund_flow_rows(payload, limit=30)
        self.assertEqual(len(rows), 2)
        self.assertEqual(rows[0]["日期"], "2026-09-04")
        self.assertAlmostEqual(rows[0]["主力净流入-净额"], -69603804.0)
        self.assertAlmostEqual(rows[0]["超大单净流入-净额"], 7332037.0)
        self.assertAlmostEqual(rows[0]["收盘价"], 11.89)
        self.assertAlmostEqual(rows[1]["涨跌幅"], -0.25)

    def test_hk_fund_flow_is_sorted_by_latest_holding_date(self) -> None:
        adapter = AkshareAdapter()
        dataframe = pd.DataFrame(
            [
                {"持股日期": "2026-01-02", "持股市值": 100},
                {"持股日期": "2026-09-04", "持股市值": 300},
                {"持股日期": "2026-06-30", "持股市值": 200},
            ]
        )
        with patch("app.connectors.akshare_adapter.ak.stock_hsgt_individual_em", return_value=dataframe):
            rows = adapter._safe_hk_fund_flow("02533")["rows"]
        self.assertEqual(rows[0]["持股日期"], "2026-09-04")
        self.assertEqual(rows[-1]["持股日期"], "2026-01-02")

    def test_hk_report_title_recognizes_interim_results_announcement(self) -> None:
        adapter = AkshareAdapter()
        title = "截至2026年6月30日止六個月\n中期業績公告"
        self.assertTrue(adapter._is_hk_financial_report_title(title))
        self.assertEqual(adapter._guess_hk_report_type(title), "半年报")
        self.assertEqual(adapter._guess_hk_report_date(title, "半年报"), "2026-06-30")

    def test_hk_financial_reports_use_official_fallback_link(self) -> None:
        adapter = AkshareAdapter()
        eastmoney_payload = {
            "result": {
                "data": [
                    {
                        "REPORT_LIST": [
                            {
                                "REPORT_DATE": "2026-06-30",
                                "REPORT_TYPE": "中报",
                                "SECURITY_NAME_ABBR": "黑芝麻智能",
                            }
                        ]
                    }
                ]
            }
        }
        official_reports = [
            {
                "report_name": "截至2026年6月30日止六個月中期業績公告",
                "report_type": "半年报",
                "report_date": "2026-06-30",
                "notice_date": "2026-08-28",
                "url": "https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0828/2026082800001_c.pdf",
                "source_name": "HKEXnews",
                "title": "截至2026年6月30日止六個月中期業績公告",
            }
        ]

        class FakeResponse:
            def __init__(self, payload: dict) -> None:
                self._payload = payload

            def raise_for_status(self) -> None:
                return None

            def json(self) -> dict:
                return self._payload

        class FakeClient:
            def __init__(self, *args, **kwargs) -> None:
                pass

            def __enter__(self):
                return self

            def __exit__(self, exc_type, exc, tb):
                return False

            def get(self, url, params=None):
                return FakeResponse(eastmoney_payload)

        with patch("app.connectors.akshare_adapter.httpx.Client", FakeClient), patch.object(
            AkshareAdapter,
            "_fetch_hkex_financial_report_notices",
            return_value=official_reports,
        ):
            result = adapter._safe_hk_financial_reports("02533")

        reports = result["reports"]
        self.assertEqual(len(reports), 1)
        self.assertEqual(reports[0]["report_date"], "2026-06-30")
        self.assertEqual(reports[0]["report_type"], "半年报")
        self.assertEqual(reports[0]["url"], official_reports[0]["url"])
        self.assertEqual(reports[0]["source_name"], "东方财富港股财报 / HKEXnews")

    def test_hk_published_reports_refresh_when_cache_lags_summary(self) -> None:
        payload = {
            "reports": [
                {
                    "report_date": "2025-12-31",
                    "notice_date": "2026-03-01",
                    "url": "https://example.com/report.pdf",
                }
            ]
        }
        financial_summary = {"periods": ["2026-06-30", "2025-12-31"]}
        self.assertTrue(_hk_published_reports_need_refresh(payload, financial_summary, None))

    @patch("app.services.f10.date", wraps=date)
    def test_hk_snapshot_refreshes_when_fund_flow_cache_is_stale(self, mock_date) -> None:
        mock_date.today.return_value = date(2026, 9, 5)
        fresh_payload = {
            "profile": {"source": "test", "fields": {"上市日期": "1972-11-01", "证券简称": "测试港股"}},
            "holders": {"source": "test", "major": [{"name": "holder"}], "circulating": []},
            "fund_flow": {"source": "test", "rows": [{"date": "2026-09-04", "value": 1}]},
            "financial_summary": {"source": "test", "periods": ["2026-06-30"], "rows": [{"metric": "eps"}]},
            "financial_statements": {
                "source": "test",
                "balance_sheet": {"label": "Balance Sheet", "periods": [{"report_date": "2026-06-30"}]},
                "income_statement": {"label": "Income Statement", "periods": [{"report_date": "2026-06-30"}]},
                "cash_flow": {"label": "Cash Flow", "periods": [{"report_date": "2026-06-30"}]},
            },
            "business_composition": {
                "source": "test",
                "report_date": "2026-06-30",
                "sections": [{"category": "main", "items": [{"name": "service"}]}],
            },
            "published_reports": {
                "source": "test",
                "reports": [
                    {
                        "report_date": "2026-06-30",
                        "notice_date": "2026-08-27",
                        "url": "https://example.com/report.pdf",
                    }
                ],
            },
        }
        self.assertFalse(_needs_f10_refresh(fresh_payload, "HK"))

        stale_payload = dict(fresh_payload)
        stale_payload["fund_flow"] = {"source": "test", "rows": [{"date": "2026-08-01", "value": 1}]}
        self.assertTrue(_needs_f10_refresh(stale_payload, "HK"))

    def test_ipo_calendar_parses_cninfo_and_persists_placeholder_symbol(self) -> None:
        service = IpoCalendarService(self.db)
        frame = pd.DataFrame(
            [
                {
                    "证劵代码": "301686",
                    "证券简称": "中塑股份",
                    "上市日期": None,
                    "申购日期": "2026-09-10",
                    "发行价": "55.28",
                    "总发行数量": "1233.29",
                    "发行市盈率": "22.2",
                    "上网发行中签率": None,
                    "摇号结果公告日": "2026-09-14",
                    "中签公告日": "2026-09-11",
                    "中签缴款日": "2026-09-14",
                    "网上申购上限": "0.25",
                    "上网发行数量": "273.75",
                }
            ]
        )
        items = service._parse_cn_ipo_frame(frame, date(2026, 9, 8), date(2026, 9, 15), "TEST_CNINFO")
        self.assertEqual(len(items), 1)
        self.assertEqual(items[0]["symbol"], "301686")
        self.assertEqual(items[0]["apply_date"], "2026-09-10")

        service._upsert_ipo_symbols(items)
        saved = self.db.scalar(select(StockSymbol).where(StockSymbol.market == "CN_A", StockSymbol.symbol == "301686"))
        self.assertIsNotNone(saved)
        self.assertEqual(saved.status, "IPO")
        self.assertEqual(saved.name, "中塑股份")

    def test_ipo_calendar_parses_hk_public_offer_window(self) -> None:
        service = IpoCalendarService(self.db)
        frame = pd.DataFrame(
            [
                {
                    "Name/Code": "Example Biotech 01234.HK",
                    "Industry": "Healthcare",
                    "Offer Price4": "10.00-12.00",
                    "Lot Size": "200",
                    "Entry Fee": "2424.20",
                    "Closing Date": "2026/09/12",
                    "Listing Date": "2026/09/18",
                    "Related Info": "Detail",
                }
            ]
        )
        items = service._parse_hk_ipo_frame(frame, date(2026, 9, 8), date(2026, 9, 15))
        self.assertEqual(len(items), 1)
        self.assertEqual(items[0]["market"], "HK")
        self.assertEqual(items[0]["symbol"], "01234")
        self.assertEqual(items[0]["apply_date"], "2026-09-08")
        self.assertEqual(items[0]["apply_end_date"], "2026-09-12")

    def test_watchlist_symbol_validation_prevents_market_mismatch(self) -> None:
        self.assertEqual(_validate_watch_symbol("700", "HK"), "00700")
        self.assertEqual(_validate_watch_symbol("600015", "CN_A"), "600015")
        with self.assertRaises(HTTPException):
            _validate_watch_symbol("600015", "HK")
        with self.assertRaises(HTTPException):
            _validate_watch_symbol("700", "CN_A")

    def test_watchlist_resolves_unique_name_and_fuzzy_symbol(self) -> None:
        source = self.db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE"))
        self.db.add(
            StockSymbol(
                market="CN_A",
                symbol="688777",
                exchange="SSE",
                name="唯一模糊股份",
                status="LISTED",
                source_id=source.id,
            )
        )
        self.db.add(
            StockSymbol(
                market="HK",
                symbol="00700",
                exchange="HKEX",
                name="腾讯控股",
                status="LISTED",
                source_id=source.id,
            )
        )
        self.db.commit()

        self.assertEqual(_resolve_watch_symbol(self.db, "唯一模糊", "CN_A"), "688777")
        self.assertEqual(_resolve_watch_symbol(self.db, "777", "CN_A"), "688777")
        self.assertEqual(_resolve_watch_symbol(self.db, "腾讯", "HK"), "00700")

    def test_watchlist_fuzzy_resolution_requires_unique_candidate(self) -> None:
        source = self.db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE"))
        for symbol, name in (("600001", "测试股份"), ("600002", "测试科技")):
            self.db.add(
                StockSymbol(
                    market="CN_A",
                    symbol=symbol,
                    exchange="SSE",
                    name=name,
                    status="LISTED",
                    source_id=source.id,
                )
            )
        self.db.commit()

        with self.assertRaises(HTTPException) as context:
            _resolve_watch_symbol(self.db, "测试", "CN_A")
        self.assertEqual(context.exception.status_code, 409)
        self.assertEqual(len(context.exception.detail["candidates"]), 2)

    def test_a_share_short_numeric_input_does_not_silently_zero_fill(self) -> None:
        source = self.db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE"))
        for symbol, name in (("000519", "中兵红箭"), ("600519", "贵州茅台")):
            self.db.add(
                StockSymbol(
                    market="CN_A",
                    symbol=symbol,
                    exchange="SSE" if symbol.startswith("6") else "SZSE",
                    name=name,
                    status="LISTED",
                    source_id=source.id,
                )
            )
        self.db.commit()

        with self.assertRaises(HTTPException) as context:
            _resolve_watch_symbol(self.db, "519", "CN_A")
        self.assertEqual(context.exception.status_code, 409)
        self.assertEqual([item["symbol"] for item in context.exception.detail["candidates"]], ["000519", "600519"])

    def test_stock_search_and_watchlist_add_accept_fuzzy_name(self) -> None:
        source = self.db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE"))
        self.db.add(
            StockSymbol(
                market="CN_A",
                symbol="002415",
                exchange="SZSE",
                name="海康威视",
                status="LISTED",
                source_id=source.id,
            )
        )
        self.db.commit()

        candidates = search_stock_symbols("CN_A", "康威", db=self.db)
        self.assertEqual(len(candidates), 1)
        self.assertEqual(candidates[0].symbol, "002415")

        added = add_watchlist(WatchlistItemCreate(market="CN_A", symbol="海康"), db=self.db)
        self.assertEqual(added["symbol"], "002415")
        self.assertEqual(added["name"], "海康威视")


if __name__ == "__main__":
    unittest.main()
