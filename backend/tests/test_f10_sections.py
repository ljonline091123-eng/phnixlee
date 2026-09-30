import unittest

from app.services.f10 import NOTICE_CATEGORIES, build_rating_statistics, classify_notice, normalize_f10_sections


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

    def test_concepts_are_projected_from_typed_industry_theme_groups_not_indexes(self) -> None:
        payload = normalize_f10_sections(
            {
                "profile": {
                    "source": "分类主数据",
                    "fields": {},
                    # A legacy cache may contain only index-labelled concepts.
                    "concepts": [{"name": "沪深300"}],
                    "classification_groups": [
                        {"key": "industry", "title": "所属行业", "items": [
                            {"name": "房地产", "code": "IND-1", "definition": "行业定义"},
                        ]},
                        {"key": "theme", "title": "主题板块", "items": [
                            {"name": "保障性住房", "code": "THEME-1", "definition": "主题定义"},
                        ]},
                        {"key": "index", "title": "入选指数", "items": [
                            {"name": "沪深300", "code": "IDX-1", "definition": "指数定义"},
                        ]},
                    ],
                },
                "holders": {},
                "financial_summary": {},
                "financial_statements": {},
                "business_composition": {},
                "research_sections": {"reports": []},
            }
        )
        concepts = payload["profile"]["concepts"]
        self.assertEqual([item["name"] for item in concepts], ["房地产", "保障性住房"])
        self.assertEqual(concepts[1]["definition"], "主题定义")
        research_concepts = next(
            section for section in payload["research_sections"]["sections"]
            if section["key"] == "industry_concepts"
        )
        self.assertEqual(len(research_concepts["rows"]), 2)

    def test_provider_concept_placeholder_is_enriched_by_typed_glossary(self) -> None:
        payload = normalize_f10_sections({
            "profile": {
                "fields": {},
                "concepts": [{
                    "name": "人工智能",
                    "definition": "来源披露的行业/概念标签，需结合原文核验。",
                }],
                "classification_groups": [{
                    "key": "theme",
                    "title": "主题板块",
                    "items": [{
                        "name": "人工智能",
                        "code": "AI",
                        "definition": "与人工智能算法、模型、算力或应用产业链相关的主题标签。",
                        "criteria": "仅在来源主数据明确列示该主题时纳入。",
                        "source_name": "主题分类主数据",
                        "definition_version": "THEME_TEST_V1",
                    }],
                }],
            },
            "holders": {},
            "financial_summary": {},
            "financial_statements": {},
            "business_composition": {},
            "research_sections": {"reports": []},
        })
        concept = payload["profile"]["concepts"][0]
        self.assertEqual(concept["definition"], "与人工智能算法、模型、算力或应用产业链相关的主题标签。")
        self.assertEqual(concept["criteria"], "仅在来源主数据明确列示该主题时纳入。")
        self.assertEqual(concept["definition_version"], "THEME_TEST_V1")

    def test_rating_statistics_anchor_to_latest_observed_report(self) -> None:
        projection = build_rating_statistics([
            {"机构": "甲机构", "东财评级": "买入", "最新报告日期": "2024-04-30"},
            {"机构": "乙机构", "东财评级": "增持", "最新报告日期": "2024-04-15"},
            {"机构": "丙机构", "东财评级": "中性", "最新报告日期": "2023-06-01"},
        ])
        self.assertEqual(projection["reference_date"], "2024-04-30")
        self.assertEqual(projection["reference_basis"], "最新报告日期")
        buckets = {row["period"]: row for row in projection["buckets"]}
        self.assertEqual(buckets["1个月内"]["buy"], 1)
        self.assertEqual(buckets["1个月内"]["add"], 1)
        self.assertEqual(buckets["1个月内"]["total"], 2)
        self.assertEqual(buckets["1年内"]["neutral"], 1)

    def test_generic_theme_definition_is_replaced_by_label_definition(self) -> None:
        payload = normalize_f10_sections({
            "profile": {
                "fields": {},
                "concepts": [],
                "classification_groups": [{
                    "key": "theme",
                    "items": [{
                        "name": "智能家居",
                        "code": "BK0680",
                        "definition": "数据提供方根据公司业务、产品、公告或市场约定整理的投资主题集合，一只证券可同时属于多个主题。",
                        "master_definition": "数据提供方根据公司业务、产品、公告或市场约定整理的投资主题集合，一只证券可同时属于多个主题。",
                        "master_dimension": "THEME",
                    }],
                }],
            },
            "holders": {},
            "financial_summary": {},
            "financial_statements": {},
            "business_composition": {},
            "research_sections": {"reports": []},
        })
        concept = payload["profile"]["concepts"][0]
        self.assertIn("家庭物联网", concept["definition"])
        self.assertEqual(concept["definition_source"], "系统概念释义词典")
        self.assertEqual(concept["definition_version"], "CONCEPT_GLOSSARY_V1")


if __name__ == "__main__":
    unittest.main()
