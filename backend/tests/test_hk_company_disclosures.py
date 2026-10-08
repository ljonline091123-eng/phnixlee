import unittest

from app.connectors.hk_company_disclosures import parse_etnet_disclosures


class HKCompanyDisclosuresTest(unittest.TestCase):
    def page(self, body):
        return '<div id="QuoteNameA">02533 黑芝麻智能</div>' + body

    def test_dates_ratios_and_shares_keep_disclosure_semantics(self):
        html = self.page('''<table><tr><td>主要股東</td><td><table>
            <tr><td>股東名稱</td><td>持股量</td><td>總股本</td><td>日期</td></tr>
            <tr><td>單記章及關連人士</td><td>91,287,468</td><td>12.81%</td><td>08/05/2026</td></tr>
            <tr><td>武岳峰科創</td><td>45,990,245</td><td>6.84%</td><td>06/03/2026</td></tr>
            </table></td></tr><tr><td>發行股數</td><td>715,207,989</td><td></td></tr></table>''')
        result = parse_etnet_disclosures(html, "02533", "https://source")
        self.assertEqual(result["major"][0]["持股数量"], 91287468)
        self.assertEqual(result["major"][0]["持股比例"], 12.81)
        self.assertEqual(result["major"][0]["披露日期"], "2026-05-08")
        self.assertEqual(result["major"][1]["披露日期"], "2026-03-06")
        self.assertEqual(result["major"][0]["verification_status"], "PENDING")
        self.assertEqual(result["capital_structure"][0]["value"], 715207989)
        self.assertIsNone(result["capital_structure"][0]["as_of"])
        self.assertEqual(result["collection_status"], "PARTIAL")
        self.assertNotIn("control", result)

    def test_other_security_or_challenge_page_is_rejected(self):
        for html in ('<div id="QuoteNameA">00700 腾讯</div>', '<h1>Checking browser</h1>'):
            with self.assertRaisesRegex(ValueError, "身份"):
                parse_etnet_disclosures(html, "02533", "https://source")

    def test_projection_does_not_call_public_list_top_ten(self):
        from app.services.f10 import normalize_f10_sections
        output = normalize_f10_sections({"holders": {"major_scope": "PUBLIC_MAJOR_SHAREHOLDER_DISCLOSURES",
            "major": [{"股东名称": "发行人股东", "持股数量": 12, "持股比例": 1.2,
                       "披露日期": "2026-05-08"}], "message": "非完整名单"}})
        section = next(s for s in output["holders"]["sections"] if s["key"] == "top_ten")
        self.assertEqual(section["title"], "主要股东（公开披露）")
        self.assertEqual(section["disclosure_scope"], "PUBLIC_MAJOR_SHAREHOLDER_DISCLOSURES")
        self.assertEqual(section["message"], "非完整名单")

    def test_empty_and_malformed_rows_do_not_prove_no_shareholders(self):
        self.assertEqual(parse_etnet_disclosures(self.page(''), "02533", "source")["collection_status"], "EMPTY_UNVERIFIED")
        html = self.page('''<table><tr><td>主要股東</td><td><table>
            <tr><td>不合法比例</td><td>12</td><td>120%</td><td>08/05/2026</td></tr>
            <tr><td>未知日期</td><td>12</td><td>1%</td><td>—</td></tr>
            </table></td></tr></table>''')
        result = parse_etnet_disclosures(html, "02533", "source")
        self.assertEqual(result["major"], [])
        self.assertEqual(len(result["source_errors"]), 2)
        self.assertEqual(result["collection_status"], "EMPTY_UNVERIFIED")
