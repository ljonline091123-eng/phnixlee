import unittest

from app.services.f10 import NOTICE_CATEGORIES, classify_notice, normalize_f10_sections


class F10SectionContractTest(unittest.TestCase):
    def test_notice_categories_are_stable_and_classified(self) -> None:
        self.assertEqual(
            NOTICE_CATEGORIES,
            ("全部", "财务业绩", "重大事项", "风险提示", "抵押担保", "增持回购", "对外投资", "其他公告"),
        )
        self.assertEqual(classify_notice("2025年年度报告"), "财务业绩")
        self.assertEqual(classify_notice("关于为子公司提供担保的公告"), "抵押担保")
        self.assertEqual(classify_notice("关于回购公司股份的公告"), "增持回购")
        self.assertEqual(classify_notice("关于设立子公司的对外投资公告"), "对外投资")
        self.assertEqual(classify_notice("股票交易异常波动风险提示公告"), "风险提示")

    def test_legacy_payload_gets_fixed_section_shapes_and_report_projection(self) -> None:
        payload = normalize_f10_sections(
            {
                "profile": {
                    "source": "测试来源",
                    "fields": {"公司名称": "测试公司", "所属行业": "软件"},
                    "concepts": [{"name": "人工智能", "definition": "测试定义"}],
                    "margin_history": [],
                    "margin_source": "暂无公开个股融资融券明细",
                },
                "holders": {"source": "测试来源", "major": [{"股东名称": "甲"}], "circulating": []},
                "financial_summary": {"source": "测试来源", "rows": []},
                "financial_statements": {"source": "测试来源"},
                "business_composition": {"source": "测试来源", "sections": []},
                "research_sections": {
                    "source": "测试来源",
                    "reports": [{"title": str(index)} for index in range(12)],
                    # Legacy snapshots could contain the full list here.
                    "latest_reports": [{"title": "旧缓存"} for _ in range(50)],
                    "qa": [],
                    "earnings_forecast": [],
                    "institution_forecast": [],
                },
            }
        )
        self.assertEqual(len(payload["profile"]["overview_sections"]), 7)
        self.assertEqual(len(payload["holders"]["sections"]), 7)
        self.assertEqual(len(payload["financial_summary"]["financial_sections"]), 5)
        self.assertEqual(len(payload["research_sections"]["sections"]), 6)
        self.assertEqual(len(payload["research_sections"]["latest_reports"]), 10)
        self.assertEqual(payload["research_sections"]["latest_reports"][0]["title"], "0")
        anomaly = next(item for item in payload["profile"]["overview_sections"] if item["key"] == "anomaly")
        self.assertEqual(anomaly["action_label"], "融资融券近一个月")
        self.assertEqual(anomaly["detail_title"], "融资融券近一个月")


if __name__ == "__main__":
    unittest.main()
