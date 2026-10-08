"""Listing identity, product filtering and source matching regressions."""
import unittest
from datetime import datetime, timezone
from unittest.mock import patch

import pandas as pd
from fastapi import HTTPException
from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session

import app.models
from app.api.stocks import list_stock_symbols
from app.connectors.akshare_adapter import AkshareAdapter
from app.connectors.base import SymbolRecord
from app.connectors.listing_master import SseMasterAdapter, HkexSecuritiesAdapter
from app.db.base import Base
from app.models.market_data import DataInterface, DataSource, StockSymbol
from app.services.catalog import seed_default_catalog, select_data_source
from app.services.security_master import listing_metadata, normalized_ext, reconcile_master
from app.services.source_coverage import source_coverage
from app.services.stock_sync import StockSyncService


def official(code, sub="Equity Securities (Main Board)", category="Equity"):
    return {"Stock Code": code, "Category": category, "Sub-Category": sub, "Name of Securities": "Test issuer"}


def snapshot(rows):
    return {"records": rows, "source_url": "https://www.hkex.com.hk/list.xlsx", "retrieved_at": "2026-10-08T00:00:00+00:00", "content_sha256": "a" * 64, "source_date_label": "Updated as at 08/10/2026"}


class SecurityMasterBoardsTest(unittest.TestCase):
    def setUp(self):
        self.engine = create_engine("sqlite://")
        Base.metadata.create_all(self.engine)
        self.db = Session(self.engine)
        seed_default_catalog(self.db)
        self.source = self.db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE"))

    def tearDown(self):
        self.db.close()
        self.engine.dispose()

    def add(self, market, symbol, ext=None):
        row = StockSymbol(market=market, symbol=symbol, name="测试股票", exchange="legacy", source_id=self.source.id,
                          ext_json=normalized_ext(market, symbol, ext), raw_payload={"original": True})
        self.db.add(row)
        self.db.flush()
        return row

    def test_a_boards_and_neeq_same_prefix_have_separate_identity(self):
        cases = [("CN_A", "000001", "MAIN", "SZSE"), ("CN_A", "600000", "MAIN", "SSE"),
                 ("CN_A", "688256", "STAR", "SSE"), ("CN_A", "301001", "CHINEXT", "SZSE"),
                 ("CN_A", "302132", "CHINEXT", "SZSE"),
                 ("CN_A", "920000", "BSE", "BSE"), ("CN_A", "830799", "BSE", "BSE"),
                 ("NEEQ", "830799", "NEEQ_BASE", "NEEQ"), ("NEEQ_INNOVATION", "830799", "NEEQ_INNOVATION", "NEEQ")]
        for market, symbol, board, exchange in cases:
            with self.subTest(market=market, symbol=symbol):
                data = listing_metadata(market, symbol)
                self.assertEqual((data["listing_board"], data["exchange"]), (board, exchange))

    def test_hk_gem_requires_exact_official_code_and_funds_are_not_stocks(self):
        self.assertEqual(listing_metadata("HK", "08003")["classification_status"], "MISSING")
        for row, expected in [(official("08003", "Equity Securities (GEM)"), "STOCK"),
                              (official("02800", "ETFs", "Exchange Traded Products"), "ETF"),
                              (official("00823", "", "Real Estate Investment Trusts"), "REIT"),
                              (official("12345", "", "Derivative Warrants"), "WARRANT")]:
            proof = {"listing_classification": {"source_code": "HKEX_SECURITIES", "source_record": row}}
            data = listing_metadata("HK", row["Stock Code"], proof)
            self.assertEqual(data["security_type"], expected)
            if expected == "STOCK":
                self.assertEqual(data["listing_board"], "HK_GEM")
            self.assertEqual(listing_metadata("HK", "09999", proof)["classification_status"], "MISSING")

    def test_reconcile_is_idempotent_and_preserves_historical_keys_and_payload(self):
        a = self.add("CN_A", "920000", {"company_id": 123})
        hk = self.add("HK", "08003", {"profile": {"name": "原公司"}})
        ids = (a.id, hk.id)
        data = snapshot([official("08003", "Equity Securities (GEM)")])
        first = reconcile_master(self.db, data)
        second = reconcile_master(self.db, data)
        self.assertGreater(first["changed"], 0)
        self.assertEqual(second["changed"], 0)
        self.assertEqual((a.id, hk.id), ids)
        self.assertEqual((a.market, a.symbol, a.ext_json["company_id"], a.raw_payload), ("CN_A", "920000", 123, {"original": True}))
        self.assertEqual(hk.ext_json["profile"]["name"], "原公司")

    @patch("app.api.stocks._stock_pipeline_statuses", return_value={})
    def test_sql_filters_separate_boards_and_products_with_pagination(self, _):
        self.add("CN_A", "920000")
        self.add("CN_A", "688256")
        self.add("NEEQ", "830799")
        for row in [official("00700"), official("08003", "Equity Securities (GEM)"), official("02800", "ETFs", "Exchange Traded Products")]:
            self.add("HK", row["Stock Code"], {"listing_classification": {"source_code": "HKEX_SECURITIES", "source_record": row}})
        result = list_stock_symbols(market="CN_A", listing_board="BSE", db=self.db)
        self.assertEqual([r.symbol for r in result.items], ["920000"])
        result = list_stock_symbols(market="HK", listing_board="HK_GEM", db=self.db)
        self.assertEqual([r.symbol for r in result.items], ["08003"])
        result = list_stock_symbols(market="HK", security_type="EQUITY", page=2, page_size=1, db=self.db)
        self.assertEqual(result.total, 2)
        self.assertEqual([r.symbol for r in result.items], ["08003"])
        with self.assertRaises(HTTPException) as caught:
            list_stock_symbols(market="HK", listing_board="STAR", db=self.db)
        self.assertEqual(caught.exception.status_code, 422)

    def test_quote_master_refresh_preserves_company_and_official_evidence(self):
        original = self.add("HK", "08003", {"company_id": 345, "listing_classification": {"source_code": "HKEX_SECURITIES", "source_record": official("08003", "Equity Securities (GEM)")}})
        original.list_date = "2001-01-01"
        incoming = SymbolRecord(market="HK", symbol="08003", name="更新简称", exchange="HKEX", asset_type="STOCK", status="LISTED", list_date=None, ext_json={}, raw_payload={"quote": True})
        StockSyncService(self.db)._upsert_records(self.source.id, [incoming])
        self.assertEqual(original.ext_json["company_id"], 345)
        self.assertEqual(original.ext_json["listing"]["listing_board"], "HK_GEM")
        self.assertEqual(original.list_date, "2001-01-01")

    def test_sse_requires_star_manifest_and_failure_is_not_full_success(self):
        frames = [pd.DataFrame([{"证券代码": "600000", "证券简称": "浦发银行"}]), pd.DataFrame([{"证券代码": "688256", "证券简称": "寒武纪"}])]
        with patch("app.connectors.listing_master.fetch_exchange_table", side_effect=frames) as call:
            result = SseMasterAdapter().fetch_symbol_master("CN_A")
            self.assertEqual({r.symbol for r in result}, {"600000", "688256"})
            self.assertEqual([args.args[1]["symbol"] for args in call.call_args_list], ["主板A股", "科创板"])
        with patch("app.connectors.listing_master.fetch_exchange_table", side_effect=[frames[0], TimeoutError("timeout")]):
            with self.assertRaises(TimeoutError):
                SseMasterAdapter().fetch_symbol_master("CN_A")

    def test_hk_official_supplement_failure_keeps_quotes_and_reports_partial(self):
        row = SymbolRecord(market="HK", symbol="00700", name="腾讯", exchange="HKEX", asset_type="STOCK", status="LISTED", list_date=None, ext_json={}, raw_payload={})
        trace = []
        with patch.object(HkexSecuritiesAdapter, "fetch_snapshot", side_effect=TimeoutError("network")):
            records = StockSyncService(self.db)._supplement_hk_listings([row], trace)
        self.assertEqual(records, [row])
        self.assertEqual(trace[-1]["status"], "PARTIAL")

    def test_coverage_respects_source_and_interface_switches_and_real_gaps(self):
        def category(board, code):
            b = next(x for x in source_coverage(self.db)["boards"] if x["code"] == board)
            return next(x for x in b["capabilities"] if x["category"] == code)
        self.assertEqual(category("BSE", "QA")["status"], "PARTIAL")
        public = self.db.scalar(select(DataSource).where(DataSource.source_code == "P5W_PUBLIC"))
        public.enabled = False
        self.db.flush()
        self.assertEqual(category("BSE", "QA")["status"], "DISABLED")
        public.enabled = True
        self.db.flush()
        self.assertEqual(category("HK_GEM", "RESEARCH")["status"], "PARTIAL")
        hk_routes = category("HK_MAIN", "RESEARCH")["sources"]
        self.assertEqual({item["source_code"] for item in hk_routes}, {"ETNET_HK", "AASTOCKS_HK"})
        self.assertEqual(category("NEEQ_BASE", "KLINE")["status"], "PARTIAL")
        self.assertEqual(category("MAIN", "NOTICE")["status"], "AVAILABLE")
        interface = self.db.scalar(select(DataInterface).where(DataInterface.source_id == self.source.id, DataInterface.interface_code == "CN_A_FUND_FLOW_ON_DEMAND"))
        interface.enabled = False
        self.db.flush()
        self.assertEqual(category("MAIN", "FUND_FLOW")["status"], "DISABLED")
        seed_default_catalog(self.db)
        self.assertFalse(interface.enabled)

    def test_bse_uses_correct_vendor_routes(self):
        adapter = AkshareAdapter()
        self.assertEqual(adapter._financial_security_code("920000"), "920000.BJ")
        self.assertEqual(adapter._cninfo_market_params("920000"), ("bjse", "bj"))
        self.assertEqual(adapter._tencent_code("CN_A", "920000"), "bj920000")

    def test_source_mismatch_cannot_start_a_stock_sync(self):
        sse = self.db.scalar(select(DataSource).where(DataSource.source_code == "SSE_MASTER"))
        with self.assertRaisesRegex(ValueError, "不匹配"):
            StockSyncService(self.db).synchronize(sse, "HK")

    def test_source_selection_does_not_fallback_to_wrong_market(self):
        selected = select_data_source(self.db, "HK", "RESEARCH", fallback_code="AKSHARE")
        self.assertEqual(selected.source_code, "AASTOCKS_HK")
        selected.enabled = False
        self.db.flush()
        self.assertIsNone(select_data_source(self.db, "HK", "RESEARCH", fallback_code="AKSHARE"))

    def test_incomplete_neeq_snapshot_does_not_mark_omitted_rows_delisted(self):
        old = self.add("NEEQ", "830799")
        old.status = "LISTED"
        incoming = SymbolRecord(market="NEEQ", symbol="830800", name="新增", exchange="NEEQ", asset_type="STOCK", status="LISTED", list_date=None, ext_json={}, raw_payload={})
        StockSyncService(self.db)._upsert_records(self.source.id, [incoming], complete_snapshot=False)
        self.assertEqual(old.status, "LISTED")


if __name__ == "__main__":
    unittest.main()
