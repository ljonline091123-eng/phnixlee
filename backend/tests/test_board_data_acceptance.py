import unittest
from unittest.mock import patch

from app.connectors.akshare_adapter import AkshareAdapter
from app.services.f10 import _section_has_payload, _display_section, normalize_f10_sections
from app.services.news_identity import check_news_identity


class BoardDataAcceptanceTest(unittest.TestCase):
    def test_controller_disclosure_is_identity_scoped_and_remains_pending(self):
        import httpx
        from app.connectors.company_sources import CompanySourcesClient
        from datetime import datetime, timezone

        provider = CompanySourcesClient(client=unittest.mock.Mock())
        from app.connectors.company_sources import SourceResult, parse_company_profile
        raw = {"success": True, "result": {"data": [
            {"SECUCODE": "688001.SH", "ORG_NAME": "华兴源创", "ORG_CODE": "ORG1",
             "CONTROL_HOLDER": "源华投资", "CONTROL_DIRECT_RATIO": "48.98%",
             "REAL_CONTROLER": "陈文源、张茜", "MAX_DATE": "2026-06-30 00:00:00"},
            {"SECUCODE": "000001.SZ", "ORG_NAME": "其他公司", "ORG_CODE": "ORG2",
             "CONTROL_HOLDER": "不能混入的股东"},
        ]}}
        result = SourceResult("EASTMONEY", "东方财富", "AGGREGATOR", "https://example.org/source",
                              datetime.now(timezone.utc).isoformat(), raw=raw)
        result.records = parse_company_profile(raw, "CN_A", "688001", result.retrieved_at)
        provider.fetch_company_profile = unittest.mock.Mock(return_value=result)
        with patch("app.connectors.company_sources.CompanySourcesClient", return_value=provider):
            data = AkshareAdapter()._safe_cn_control_disclosures("688001")
        self.assertEqual(data["status"], "SOURCE_REPORTED")
        self.assertEqual([r["主体名称"] for r in data["rows"]], ["源华投资", "陈文源、张茜"])
        self.assertEqual(data["rows"][0]["direct_ratio"], 48.98)
        self.assertTrue(all(r["verification_status"] == "PENDING" and r["published_at"] is None for r in data["rows"]))
        self.assertEqual(data["rows"][0]["报告期"], "2026-06-30")
        result.status, result.records = "EMPTY", []
        with patch("app.connectors.company_sources.CompanySourcesClient", return_value=provider):
            empty = AkshareAdapter()._safe_cn_control_disclosures("688001")
        self.assertEqual(empty["status"], "EMPTY_UNVERIFIED")
        self.assertEqual(empty["rows"], [])

    def test_hk_no_trade_snapshot_is_a_reference_not_a_new_trade(self):
        parts = [""] * 78
        for index, value in {2: "08059", 3: "0.025", 4: "0.025", 5: "0", 6: "0",
                             30: "20261008090000", 33: "0", 34: "0"}.items():
            parts[index] = value
        adapter = AkshareAdapter()
        quote = adapter._parse_tencent_quote('v_hk08059="' + "~".join(parts) + '";', "HK", "08059")
        self.assertTrue(quote.raw_payload["no_trade_today"])
        self.assertEqual(quote.raw_payload["latest_price_source"], "PREVIOUS_CLOSE_REFERENCE")
        parts[6] = "100"
        traded = adapter._parse_tencent_quote('v_hk08059="' + "~".join(parts) + '";', "HK", "08059")
        self.assertFalse(traded.raw_payload["no_trade_today"])

    def test_numeric_success_flag_in_disclosure_mirror(self):
        response = unittest.mock.Mock()
        response.json.return_value = {"success": 1, "data": {"list": [{"art_code": "AN1",
            "codes": [{"stock_code": "000001"}], "notice_date": "2026-09-30", "title": "平安银行：公告"}], "total_hits": 1}}
        with patch("app.connectors.akshare_adapter.httpx.Client") as client:
            client.return_value.__enter__.return_value.get.return_value = response
            rows = AkshareAdapter()._fetch_cn_a_notices_eastmoney("000001", "20260101", "20261008")
        self.assertEqual(len(rows), 1)
        self.assertEqual(rows[0].content_json["source_method"], "EASTMONEY_DISCLOSURE_MIRROR")

    def test_news_search_code_alone_is_not_issuer_proof(self):
        self.assertEqual(check_news_identity("CN_A", "000001", "平安银行", "上证指数ETF 000001 上涨")["status"], "CANDIDATE")
        self.assertEqual(check_news_identity("CN_A", "000001", "平安银行", "平安银行发布中报")["status"], "MENTION_MATCHED")
        self.assertEqual(check_news_identity("HK", "02533", "黑芝麻智能", "黑芝麻国际公告")["status"], "CANDIDATE")
        self.assertEqual(check_news_identity("HK", "02533", "黑芝麻智能", "02533.HK 公告")["status"], "MENTION_MATCHED")

    def test_legacy_household_count_is_reclassified_without_losing_evidence(self):
        raw = {"shareholder_count": 46, "report_date": "2026-06-30"}
        data = normalize_f10_sections({"holders": {"major": [raw]}})
        self.assertEqual(data["holders"]["major"], [])
        self.assertEqual(data["holders"]["holder_count"][0]["shareholder_count"], 46)
        self.assertEqual(data["holders"]["legacy_holder_count_rows"], [raw])
    def test_announcement_failures_do_not_become_successful_empty(self):
        adapter = AkshareAdapter()
        with patch.object(adapter, "_fetch_cn_a_notices", side_effect=TimeoutError("primary timeout")), \
             patch.object(adapter, "_fetch_cn_a_notices_eastmoney", side_effect=TimeoutError("mirror timeout")):
            with self.assertRaisesRegex(RuntimeError, "mirror timeout"):
                adapter.fetch_notices("CN_A", "000001", "20260101", "20261008")

    def test_neeq_quote_cannot_substitute_a_financial_report(self):
        adapter = AkshareAdapter()
        with patch.object(adapter, "_fetch_neeq_json", side_effect=TimeoutError("financial unavailable")), \
             patch("app.connectors.neeq_f10.fetch_home_summary", side_effect=TimeoutError("financial unavailable")), \
             patch.object(adapter, "_fetch_neeq_quote_push2") as quote:
            with self.assertRaises(TimeoutError):
                adapter.fetch_financial_indicators("NEEQ", "430019", "报告期")
            quote.assert_not_called()

    def test_placeholder_and_source_links_are_not_observations(self):
        cases = {
            "holders": {"major": [{}], "official_links": [{"url": "https://www.hkexnews.hk"}]},
            "financial_summary": {"rows": [{"metric": "net_profit", "data": {"2026-06-30": None}}]},
            "financial_statements": {"income_statement": {"periods": ["2026-06-30"], "rows": [{"metric": "net_profit"}]}},
            "business_composition": {"sections": [{"name": "主营", "items": []}]},
            "research_sections": {"sections": [{"key": "qa", "rows": []}]},
        }
        for section, payload in cases.items():
            with self.subTest(section=section):
                self.assertFalse(_section_has_payload(section, payload))

    def test_zero_is_a_real_observation_in_raw_and_read_contracts(self):
        row = {"metric": "net_profit", "data": {"2026-06-30": 0}}
        self.assertTrue(_section_has_payload("financial_summary", {"rows": [row]}))
        self.assertTrue(_section_has_payload("research_sections", {"earnings_forecast": {"rows": [row]}}))
        self.assertEqual(_display_section("indicators", "主要指标", rows=[row])["status"], "AVAILABLE")
        empty = {"metric": "net_profit", "data": {"2026-06-30": None}}
        self.assertEqual(_display_section("indicators", "主要指标", rows=[empty])["status"], "UNAVAILABLE")

    def test_neeq_holder_count_is_not_top_ten_holders(self):
        adapter = AkshareAdapter()
        with patch.object(adapter, "_fetch_neeq_quote", side_effect=TimeoutError("quote")), \
             patch.object(adapter, "_fetch_neeq_financial_indicators", return_value=[]), \
             patch.object(adapter, "_fetch_neeq_notices", return_value=[]), \
             patch("app.connectors.neeq_f10.fetch_neeq_f10", return_value={}), \
             patch.object(adapter, "_fetch_neeq_json", return_value={"result": [{"TOTALSH": 123, "REPORTDATE": "2026-06-30"}]}):
            payload = adapter._fetch_neeq_extended_data("NEEQ", "430019")
        self.assertEqual(payload["holders"]["major"], [])
        self.assertEqual(payload["holders"]["holder_count"][0]["shareholder_count"], 123)

    def test_tencent_units_identity_and_hk_market_cap(self):
        adapter = AkshareAdapter()
        fields = [""] * 78
        for i, value in {2: "02533", 3: "8.36", 6: "13307586", 43: "11.91", 44: "59.8491",
                         45: "59.8491", 46: "BLACK SESAME", 49: "8.325", 69: "715897906", 72: "0.000"}.items():
            fields[i] = value
        quote = adapter._parse_tencent_quote('v_hk02533="' + "~".join(fields) + '";', "HK", "02533")
        self.assertEqual(quote.raw_payload["derived"]["total_market_cap_yi"], 59.8491)
        self.assertIsNone(quote.raw_payload["derived"]["volume_ratio"])
        self.assertEqual(quote.volume, 13307586)
        with self.assertRaisesRegex(ValueError, "身份"):
            adapter._parse_tencent_quote('v_hk02533="' + "~".join(fields) + '";', "HK", "00700")
        fields[2], fields[6] = "000001", "1481686"
        quote = adapter._parse_tencent_quote('v_sz000001="' + "~".join(fields) + '";', "CN_A", "000001")
        self.assertEqual(quote.volume, 148168600)
        fields[2], fields[6] = "688001", "11502465"
        quote = adapter._parse_tencent_quote('v_sh688001="' + "~".join(fields) + '";', "CN_A", "688001")
        self.assertEqual(quote.volume, 11502465)
        self.assertEqual(quote.raw_payload["units"]["source_volume"], "股")
        fields[2], fields[6] = "920020", "2470"
        quote = adapter._parse_tencent_quote('v_bj920020="' + "~".join(fields) + '";', "CN_A", "920020")
        self.assertEqual(quote.volume, 247000)

    def test_neeq_financial_table_uses_real_values_and_source_labels(self):
        from app.connectors.neeq_f10 import financial_table
        payload = {"Trs": [{"Field": "PARENTNETPROFIT", "Name": "归母净利润(元)"}],
                   "result": [{"REPORTDATE": "2026-06-30", "PARENTNETPROFIT": "-3467166.72"}]}
        table = financial_table(payload, "public-source")
        self.assertEqual(table["rows"][0]["data"]["2026-06-30"], -3467166.72)
        self.assertEqual(table["rows"][0]["unit"], "元")

    def test_hkex_splits_overflowing_date_windows_without_losing_records(self):
        import json
        adapter = AkshareAdapter()
        first, second, third = (unittest.mock.Mock() for _ in range(3))
        first.json.return_value = {"result": "[]", "recordCnt": 1500, "loadedRecord": 100, "hasNextRow": True}
        second.json.return_value = {"result": json.dumps([{"TITLE": "2026中期報告", "DATE_TIME": "08/10/2026", "FILE_LINK": "/new.pdf"}]),
                                    "recordCnt": 1, "loadedRecord": 1, "hasNextRow": False}
        third.json.return_value = {"result": json.dumps([{"TITLE": "2025年報", "DATE_TIME": "01/10/2026", "FILE_LINK": "/old.pdf"}]),
                                   "recordCnt": 1, "loadedRecord": 1, "hasNextRow": False}
        with patch.object(adapter, "_lookup_hkex_stock_id", return_value="100"), \
             patch("app.connectors.akshare_adapter.httpx.Client") as factory:
            client = factory.return_value.__enter__.return_value
            client.get.side_effect = [first, second, third]
            rows = adapter._fetch_hkex_notices("00700", "2026-10-01", "2026-10-08")
        self.assertEqual(len(rows), 2)
        windows = [(call.kwargs["params"]["fromDate"], call.kwargs["params"]["toDate"]) for call in client.get.call_args_list]
        self.assertEqual(windows, [("20261001", "20261008"), ("20261005", "20261008"), ("20261001", "20261004")])

    def test_hkex_malformed_result_is_not_no_disclosures(self):
        adapter = AkshareAdapter()
        with patch.object(adapter, "_lookup_hkex_stock_id", return_value="100"), \
             patch("app.connectors.akshare_adapter.httpx.Client") as factory:
            factory.return_value.__enter__.return_value.get.return_value.json.return_value = {"error": "unavailable"}
            with self.assertRaisesRegex(ValueError, "不能标记无数据"):
                adapter._fetch_hkex_notices("00700", "2026-10-01", "2026-10-08")

    def test_neeq_public_browser_fallback_returns_json_after_empty_403(self):
        import httpx
        adapter = AkshareAdapter()
        response = httpx.Response(403, request=httpx.Request("GET", "https://xinsanban.eastmoney.com/api/test"))
        with patch("app.connectors.akshare_adapter.httpx.Client") as client, \
             patch("app.connectors.akshare_adapter.curl_requests") as curl:
            client.return_value.__enter__.return_value.get.side_effect = httpx.HTTPStatusError("403", request=response.request, response=response)
            browser = curl.Session.return_value.__enter__.return_value
            browser.get.return_value.json.return_value = {"result": [{"REPORTDATE": "2026-06-30", "PROFIT": 0}]}
            result = adapter._fetch_neeq_json("/api/test", {"code": "430019"})
        self.assertEqual(result["result"][0]["PROFIT"], 0)
        self.assertEqual(browser.get.call_args.kwargs["impersonate"], "chrome")

    def test_neeq_rounded_summary_preserves_period_precision_and_issuer(self):
        from app.connectors.neeq_f10 import parse_home_summary
        page = '<div class="stockinfo"><span class="code">(832649)</span></div><div class="cwzbtip">2026-06-30</div>' \
               '<div class="cwzy"><table><tr><td>归属净利润(万元)</td><td>-394.69</td><td>每股收益(元/股)</td><td>-0.12</td></tr></table></div>'
        result = parse_home_summary(page, "832649", "source")
        self.assertEqual(result["raw_values"]["PARENTNETPROFIT"], -3946900)
        self.assertEqual(result["report_date"], "2026-06-30")
        self.assertEqual(result["rows"][0]["rounding_step"], 100)
        self.assertEqual(result["rows"][0]["precision"], "SOURCE_ROUNDED")
        with self.assertRaisesRegex(ValueError, "身份"):
            parse_home_summary(page, "430019", "source")

    def test_neeq_report_period_does_not_use_disclosure_date(self):
        self.assertEqual(AkshareAdapter._neeq_report_period("2025年年度报告"), "2025-12-31")
        self.assertEqual(AkshareAdapter._neeq_report_period("2026年半年度报告"), "2026-06-30")
        self.assertIsNone(AkshareAdapter._neeq_report_period("年度报告更正公告"))

    def test_kline_lots_are_normalized_only_for_the_provider_returning_lots(self):
        import pandas as pd
        adapter = AkshareAdapter()
        source = pd.DataFrame([{"volume": 232, "date": "2026-10-08"}])
        self.assertEqual(adapter._normalize_a_hist_dataframe(source, "stock_zh_a_hist").iloc[0]["成交量"], 23200)
        self.assertEqual(adapter._normalize_a_hist_dataframe(source, "stock_zh_a_hist_tx").iloc[0]["成交量"], 232)

    def test_neeq_no_trade_reference_and_native_quote_units(self):
        adapter = AkshareAdapter()
        quote = {"Code": "430005", "Close": 0, "PreviousClose": 24.1, "Volume": 0}
        with patch.object(adapter, "_fetch_neeq_json", side_effect=[{"result": [quote]}, {"result": []}]):
            result = adapter._fetch_neeq_quote("NEEQ", "430005")
        self.assertEqual(result.current_price, 24.1)
        self.assertTrue(result.raw_payload["no_trade_today"])
        quote.update(Close=24.34, Volume=232)
        with patch.object(adapter, "_fetch_neeq_json", side_effect=[{"result": [quote]}, {"result": []}]):
            result = adapter._fetch_neeq_quote("NEEQ", "430005")
        self.assertEqual(result.volume, 23200)
        self.assertFalse(result.raw_payload["no_trade_today"])

    def test_issuer_match_handles_traditional_script_and_full_width(self):
        self.assertEqual(check_news_identity("HK", "00700", "腾讯控股", "騰訊控股公布中期業績")["status"], "MENTION_MATCHED")
        self.assertEqual(check_news_identity("CN_A", "000002", "万  科Ａ", "万科A发布公告")["status"], "MENTION_MATCHED")
        self.assertEqual(check_news_identity("HK", "00700", "腾讯控股", "700指数创新高")["status"], "CANDIDATE")

    def test_explicit_no_controller_disclosure_is_not_an_entity_or_an_empty_response(self):
        from app.connectors.company_sources import SourceResult
        record = {"source_record": {"CONTROL_HOLDER": "无", "REAL_CONTROLER": "无", "MAX_DATE": "2026-06-30"},
                  "controller_mentions": []}
        result = SourceResult("EASTMONEY", "来源", "PROFILE", "https://source", "2026-10-08", records=[record])
        with patch("app.connectors.company_sources.CompanySourcesClient.fetch_company_profile", return_value=result):
            output = AkshareAdapter()._safe_cn_control_disclosures("000002")
        self.assertEqual(output["status"], "SOURCE_REPORTED")
        self.assertEqual(len(output["rows"]), 2)
        self.assertEqual(output["rows"][0]["disclosure_state"], "SOURCE_REPORTED_NONE")
        self.assertNotIn("主体名称", output["rows"][0])
        self.assertEqual(output["rows"][0]["verification_status"], "PENDING")

    def test_malformed_neeq_notices_are_failure(self):
        with patch.object(AkshareAdapter, "_fetch_neeq_json", return_value={"error": "upstream down"}), \
             patch("app.connectors.p5w_public.P5WClient.fetch_notices", side_effect=ValueError("备源格式异常")):
            with self.assertRaisesRegex(ValueError, "格式异常"):
                AkshareAdapter()._fetch_neeq_notices("NEEQ", "430005", "20260101", "20261008")

    def test_empty_neeq_financial_response_uses_evidenced_summary(self):
        summary = {"raw_values": {"PARENTNETPROFIT": 100}, "source_evidence": [{"url": "source"}],
                   "rows": [{"metric": "归属净利润", "data": {"2026-06-30": 100}}], "report_date": "2026-06-30"}
        with patch.object(AkshareAdapter, "_fetch_neeq_json", return_value={"result": []}), \
             patch("app.connectors.neeq_f10.fetch_home_summary", return_value=summary):
            record = AkshareAdapter()._fetch_neeq_financial_indicators("NEEQ", "430005", "报告期")[0]
        self.assertEqual(record.currency, "CNY")
        self.assertEqual(record.data_json["source_precision"], "SOURCE_ROUNDED")

    def test_rounded_f10_fallback_cannot_replace_exact_period_or_duplicate_metric(self):
        from app.services.stock_on_demand import _merge_f10_payload
        exact = {"periods": ["2026-06-30"], "rows": [{"metric": "净利润(元)", "metric_code": "PROFIT",
                 "data": {"2026-06-30": -3946932.69}, "source": "full-statement"}]}
        rounded = {"periods": ["2026-06-30"], "precision": "SOURCE_ROUNDED", "rows": [
            {"metric": "归属净利润(元)", "metric_code": "PARENTNETPROFIT", "precision": "SOURCE_ROUNDED",
             "data": {"2026-06-30": -3946900}, "rounding_step": 100, "source": "html-summary"}]}
        merged = _merge_f10_payload("financial_summary", exact, rounded)
        self.assertEqual(len(merged["rows"]), 1)
        self.assertEqual(merged["rows"][0]["data"]["2026-06-30"], -3946932.69)
        self.assertEqual(merged["rows"][0]["source"], "full-statement")
        self.assertIn("2026-06-30", merged["rows"][0]["rounded_fallback_observations"])

    def test_hk_news_fallback_requires_dated_matching_stock_links(self):
        from app.connectors.hk_public_news import parse_aastocks_news
        page = '''<div ref="NOW.1"><a id="cp_repNews_lnkNews_0" href="/sc/stocks/analysis/stock-aafn-con/08431/AAFN/NOW.1/hk-stock-news">浩柏国际公布业绩</a>
          <script>ConvertToLocalTime({dt:'2026/10/07 10:12'})</script><div class="newscontent4">原文摘要</div></div>
          <div ref="NOW.2"><a id="cp_repNews_lnkNews_1" href="/sc/stocks/analysis/stock-aafn-con/08431/AAFN/NOW.2/hk-stock-news">未披露日期</a></div>
          <div ref="NOW.3"><a id="cp_repNews_lnkNews_2" href="/sc/stocks/analysis/stock-aafn-con/08613/AAFN/NOW.3/hk-stock-news">其他股票</a><script>ConvertToLocalTime({dt:'2026/10/07 10:12'})</script></div>'''
        rows = parse_aastocks_news(page, "08431", "https://www.aastocks.com/")
        self.assertEqual(len(rows), 1)
        self.assertEqual(rows[0]["news_time"], "2026-10-07 10:12:00")
        self.assertEqual(rows[0]["content"], "原文摘要")
        self.assertEqual(rows[0]["collection_status"], "PARTIAL")

    def _neeq_fixture(self, fetch_json):
        from app.connectors.neeq_f10 import fetch_neeq_f10
        response = unittest.mock.Mock()
        response.text = '<ul id="security_info"><li><span class="company-page-item-left">证券代码</span><span class="company-page-item-right">430005</span></li></ul>'
        response.content = response.text.encode()
        with patch("app.connectors.neeq_f10.httpx.Client") as factory, \
             patch("app.connectors.neeq_f10.fetch_home_summary", return_value={
                 "rows": [{"metric": "净利润", "data": {"2026-06-30": 0}}],
                 "report_date": "2026-06-30", "source_evidence": []}):
            factory.return_value.__enter__.return_value.get.return_value = response
            return fetch_neeq_f10("430005", fetch_json)

    def test_neeq_foreign_holder_is_quarantined_without_losing_good_data(self):
        def fetch(path, params):
            rows = []
            if path.endswith("GetHoldTop10"):
                rows = [{"list": [
                    {"SECURITYCODE": "430005", "SHAREHDNAME": "真实股东", "SHAREHDNUM": 123,
                     "ENDDATE": "2026-06-30", "SHAREHDNUMPER": 12.3},
                    {"SECURITYCODE": "430019", "SHAREHDNAME": "其他公司股东", "SHAREHDNUM": 456}]}]
            return {"IsSuccess": True, "result": rows}
        result = self._neeq_fixture(fetch)
        self.assertEqual([r["股东名称"] for r in result["holders"]["major"]], ["真实股东"])
        self.assertEqual(result["holders"]["source_errors"][0]["rejected_row"]["SECURITYCODE"], "430019")
        self.assertEqual(result["holders"]["collection_status"], "PARTIAL")
        self.assertEqual(result["holders"]["as_of"], "2026-06-30")
        self.assertEqual(result["financial_summary"]["as_of"], "2026-06-30")
        self.assertIsNone(result["profile"]["as_of"])

    def test_neeq_dividends_follow_pagination_and_preserve_originals(self):
        pages = []
        def fetch(path, params):
            rows = []
            if path.endswith("/fhpx"):
                pages.append(params["page"])
                rows = [{"PLANNOTICEDATE": "2026-01-01", "ASSIGNDSCRPT": f"方案{i}"}
                        for i in (range(100) if params["page"] == 1 else [100])]
            return {"IsSuccess": True, "result": rows}
        result = self._neeq_fixture(fetch)
        self.assertEqual(pages, [1, 2])
        self.assertEqual(len(result["profile"]["dividends"]), 101)
        original = [r for r in result["profile"]["original_source_responses"] if r["url"].endswith("/fhpx")]
        self.assertEqual([r["params"]["page"] for r in original], [1, 2])

    def test_neeq_repeated_dividend_page_is_partial_not_complete(self):
        def fetch(path, params):
            return {"IsSuccess": True, "result": [{"ASSIGNDSCRPT": f"方案{i}"} for i in range(100)]
                    if path.endswith("/fhpx") else []}
        result = self._neeq_fixture(fetch)
        self.assertEqual(len(result["profile"]["dividends"]), 100)
        self.assertEqual(result["profile"]["collection_status"], "PARTIAL")
        self.assertIn("重复", result["profile"]["source_errors"][0]["error"])

    def test_local_f10_read_never_writes_or_refreshes_even_when_refresh_is_requested(self):
        from sqlalchemy import create_engine, event
        from sqlalchemy.orm import Session
        from app.db.base import Base
        from app.models.market_data import DataSource, StockSymbol, StockRealtimeQuote
        from app.api.stocks import get_stock_f10
        engine = create_engine("sqlite://")
        Base.metadata.create_all(engine)
        with Session(engine) as db:
            source = DataSource(source_code="AKSHARE", source_name="只读测试", adapter_type="MOCK")
            db.add(source)
            db.flush()
            db.add(StockSymbol(market="NEEQ", symbol="430005", name="测试公司", exchange="NEEQ", source_id=source.id))
            db.add(StockRealtimeQuote(market="NEEQ", symbol="430005", source_id=source.id,
                                     current_price=10, volume=0, quote_time="2026-10-08"))
            db.commit()
            statements = []
            def record(conn, cursor, statement, parameters, context, executemany):
                statements.append(statement.lstrip().split(None, 1)[0].upper())
            event.listen(engine, "before_cursor_execute", record)
            with patch.object(db, "commit", side_effect=AssertionError("本地读取不得提交")), \
                 patch("app.orchestration.f10._hydrate_symbol_from_f10", side_effect=AssertionError("本地读取不得改主数据")), \
                 patch("app.api.stocks._fetch_and_cache_f10_extended_data", side_effect=AssertionError("本地读取不得远程采集")):
                for refresh in (False, True):
                    result = get_stock_f10("NEEQ", "430005", local_only=True, refresh=refresh, db=db)
                    self.assertEqual(result.symbol.symbol, "430005")
            self.assertFalse(set(statements) & {"INSERT", "UPDATE", "DELETE", "REPLACE"})
        engine.dispose()


if __name__ == "__main__":
    unittest.main()
