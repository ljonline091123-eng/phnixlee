import unittest
from unittest.mock import patch

import pandas as pd

from app.connectors.akshare_adapter import AkshareAdapter


class ResearchProjectionTest(unittest.TestCase):
    def _frame(self):
        return pd.DataFrame(
            [
                {
                    "股票代码": "000002",
                    "股票简称": "万科A",
                    "报告名称": "万科业绩点评",
                    "东财评级": "买入",
                    "机构": "测试证券",
                    "2026-盈利预测-收益": 1.23,
                    "2026-盈利预测-市盈率": 8.8,
                    "2027-盈利预测-收益": 1.50,
                    "2027-盈利预测-市盈率": 7.1,
                    "行业": "房地产开发",
                    "日期": "2026-09-01",
                    "报告PDF链接": "https://example.test/vanke.pdf",
                },
                {
                    "股票代码": "000001",
                    "股票简称": "平安银行",
                    "报告名称": "平安银行研报",
                    "东财评级": "增持",
                    "机构": "其他证券",
                    "2026-盈利预测-收益": 2.0,
                    "2026-盈利预测-市盈率": 5.0,
                    "行业": "银行",
                    "日期": "2026-09-02",
                    "报告PDF链接": "https://example.test/pingan.pdf",
                },
            ]
        )

    def test_research_rows_are_code_filtered_and_projected(self):
        adapter = AkshareAdapter()
        with patch("app.connectors.akshare_adapter.ak.stock_research_report_em", return_value=self._frame()):
            reports = adapter._safe_research_report_records("000002")

        self.assertEqual(len(reports), 1)
        self.assertEqual(reports[0]["股票代码"], "000002")
        self.assertEqual(reports[0]["报告名称"], "万科业绩点评")
        self.assertEqual(reports[0]["机构"], "测试证券")
        self.assertEqual(reports[0]["报告PDF链接"], "https://example.test/vanke.pdf")

        earnings = adapter._derive_research_earnings_forecast(reports, "000002")
        self.assertEqual({row["预测年度"] for row in earnings}, {"2026", "2027"})
        self.assertTrue(all(row["股票代码"] == "000002" for row in earnings))
        self.assertTrue(all("预测每股收益" in row for row in earnings))

        institutions = adapter._derive_institution_forecast(reports, "000002")
        self.assertEqual(len(institutions), 1)
        self.assertEqual(institutions[0]["机构"], "测试证券")
        self.assertEqual(institutions[0]["东财评级"], "买入")
        self.assertEqual(institutions[0]["评级数量"], 1)
        self.assertEqual(institutions[0]["股票代码"], "000002")

    def test_unknown_schema_fails_closed(self):
        adapter = AkshareAdapter()
        frame = pd.DataFrame([{"序号": 1, "报告名称": "不应展示"}])
        with patch("app.connectors.akshare_adapter.ak.stock_research_report_em", return_value=frame):
            self.assertEqual(adapter._safe_research_report_records("000002"), [])

    def test_extended_research_sections_are_derived_from_same_filtered_rows(self):
        adapter = AkshareAdapter()
        with patch.object(adapter, "_safe_research_report_records", return_value=[
            {
                "股票代码": "000002",
                "报告名称": "报告A",
                "机构": "机构A",
                "东财评级": "中性",
                "日期": "2026-09-01",
                "2026-盈利预测-收益": 0.2,
                "2026-盈利预测-市盈率": 12,
            }
        ]):
            with patch.object(adapter, "_safe_dataframe_first_row", return_value={"source": "测试", "fields": {}}):
                with patch.object(adapter, "_safe_holder_rows", return_value=[]), patch.object(adapter, "_safe_optional_records", return_value=[]), patch.object(adapter, "_safe_holder_count", return_value=[]), patch.object(adapter, "_safe_cn_margin_history", return_value=[]):
                    payload = adapter._fetch_cn_a_extended_sections("000002", {"fields": {}})

        self.assertEqual(payload["research_sections"]["earnings_forecast"][0]["股票代码"], "000002")
        self.assertEqual(payload["research_sections"]["institution_forecast"][0]["机构"], "机构A")
        self.assertFalse(any("000001" in str(row) for row in payload["research_sections"]["earnings_forecast"]))


if __name__ == "__main__":
    unittest.main()
