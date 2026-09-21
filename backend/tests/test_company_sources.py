import io
import json
from types import SimpleNamespace
from unittest.mock import patch

import httpx
import pytest
from pypdf import PdfWriter

from app.connectors.company_sources import (
    CompanySourcesClient, authorized_source_capabilities, extract_case_mentions, extract_pdf_text,
    extract_disclosure_mentions, select_supply_chain_disclosures, parse_cninfo_announcements,
    parse_company_profile, parse_market_cap, parse_shareholders, valid_cn_uscc,
)


def profile_payload(market="CN_A"):
    row = {"SECUCODE": "601398.SH" if market == "CN_A" else "01398.HK", "ORG_CODE": "10008224",
           "ORG_NAME": "中国工商银行股份有限公司", "REG_PLACE": "中国", "REG_NUM": "91100000100003962T",
           "REG_ADDRESS": "Example address", "BUSINESS_SCOPE": "Example business", "REG_CAPITAL": 10,
           "STR_CODEH": "01398.HK", "BOARD_CODE_BK_1LEVEL": "BK1283", "BOARD_NAME_1LEVEL": "银行",
           "BLGAINIAN": "Example One,Example Two", "BLGAINIAN_CODE": "BK0001,BK0002"}
    if market == "HK":
        row.pop("REG_NUM")
    return {"success": True, "result": {"data": [row]}}


def test_identity_provenance_and_unknown_publication_are_explicit():
    now = "2026-09-21T00:00:00+00:00"
    a = parse_company_profile(profile_payload(), "CN_A", "601398", now)[0]
    h = parse_company_profile(profile_payload("HK"), "HK", "01398", now)[0]
    assert a["company"]["source_issuer_id"] == h["company"]["source_issuer_id"]
    assert h["company"]["jurisdiction"] == "CN" and "identifier_value" not in h["company"]
    assert a["company"]["identifier_value"] == "91100000100003962T"
    assert a["profile"]["registration_verified"] is False
    assert a["profile"]["registered_capital_unit"] == "UNCONFIRMED_SOURCE_UNIT"
    assert a["evidence"]["published_at"] is None and a["evidence"]["available_at"] == now
    assert json.loads(a["evidence"]["content"]) == a["source_record"]
    assert a["industries"][0]["taxonomy_version"] == "PROVIDER_CURRENT_UNVERSIONED"
    assert len(a["themes"]) == 2


def test_invalid_registration_missing_identity_and_wrong_stock_are_not_guessed():
    payload = profile_payload()
    payload["result"]["data"][0]["REG_NUM"] = "91100000100003962A"
    row = parse_company_profile(payload, "CN_A", "601398", "2026-09-21T00:00:00Z")[0]
    assert "identifier_scheme" not in row["company"]
    assert row["profile"]["registration_checksum_valid"] is False
    assert valid_cn_uscc("91440300192185379H") and not valid_cn_uscc("unknown")
    assert parse_company_profile(payload, "CN_A", "000001", "2026-09-21T00:00:00Z") == []
    payload["result"]["data"][0].pop("ORG_CODE")
    assert parse_company_profile(payload, "CN_A", "601398", "2026-09-21T00:00:00Z") == []


def test_hk_jurisdiction_comes_from_registration():
    payload = profile_payload("HK")
    payload["result"]["data"][0]["REG_PLACE"] = "开曼群岛"
    row = parse_company_profile(payload, "HK", "01398", "2026-09-21T00:00:00Z")[0]
    assert row["company"]["jurisdiction"] == "KY"


def test_natural_person_controller_code_is_not_an_issuer_identity():
    payload = profile_payload()
    raw = payload["result"]["data"][0]
    raw.update(REAL_CONTROLER="Example person", REAL_CONTROLER_CODE="person-1", REAL_DIRECT_RATIO="20%")
    row = parse_company_profile(payload, "CN_A", "601398", "2026-09-21T00:00:00Z")[0]
    mention = row["controller_mentions"][0]
    assert mention["source_issuer_id"] is None
    assert mention["source_entity_code"] == "person-1" and mention["entity_type"] == "UNRESOLVED"


def test_shareholders_are_unresolved_register_mentions_not_ubos():
    payload = {"sdgd": [{"SECUCODE": "000001.SZ", "HOLDER_NAME": "香港中央结算有限公司", "HOLDER_RANK": 3,
                        "END_DATE": "2026-06-30 00:00:00", "HOLD_NUM": 100, "HOLD_NUM_RATIO": 3.8,
                        "SHARES_TYPE": "流通A股"}]}
    row = parse_shareholders(payload, "CN_A", "000001")[0]
    assert row["report_date"] == "2026-06-30" and row["published_at"] is None
    assert row["ratio_basis"] == "TOTAL_ISSUED_SHARES" and row["beneficial_owner_verified"] is False
    assert row["identity_status"] == "UNRESOLVED_REGISTER_HOLDER" and "source_issuer_id" not in row
    payload["sdgd"][0]["HOLD_NUM_RATIO"] = 120
    assert parse_shareholders(payload, "CN_A", "000001")[0]["ratio"] is None


def test_announcements_are_scoped_and_external_pdf_urls_are_rejected():
    rows = [
        {"secCode": "600221", "announcementId": "123", "announcementTitle": "<em>诉讼</em>进展",
         "announcementTime": 1741017600000, "adjunctUrl": "finalpage/2025-03-04/123.PDF"},
        {"secCode": "600221", "announcementId": "124", "announcementTitle": "Another",
         "announcementTime": 1741017600000, "adjunctUrl": "https://127.0.0.1/internal.pdf"},
        {"secCode": "000001", "announcementId": "125", "announcementTitle": "Wrong security"},
    ]
    records = parse_cninfo_announcements({"announcements": rows}, "600221")
    assert len(records) == 2 and records[0]["title"] == "诉讼进展"
    assert records[0]["url"] == "https://static.cninfo.com.cn/finalpage/2025-03-04/123.PDF"
    assert records[1]["url"] is None and records[0]["published_at"].endswith("+00:00")


def test_pdf_requires_valid_header_and_blank_pages_require_ocr():
    with pytest.raises(ValueError, match="not a PDF"):
        extract_pdf_text(b"<html>login required</html>")
    writer = PdfWriter()
    writer.add_blank_page(width=100, height=100)
    buffer = io.BytesIO()
    writer.write(buffer)
    result = extract_pdf_text(buffer.getvalue())
    assert result["extraction_status"] == "OCR_REQUIRED" and result["page_count"] == 1
    assert result["content_scope"] == "PDF_EXTRACTED_TEXT"


def test_pdf_character_bound_is_explicit_even_with_only_one_page():
    reader = SimpleNamespace(is_encrypted=False, pages=[SimpleNamespace(extract_text=lambda: "supplier and customer details")])
    with patch("pypdf.PdfReader", return_value=reader), \
         patch("app.connectors.company_sources.MAX_TEXT_CHARACTERS", 8):
        result = extract_pdf_text(b"%PDF-test-fixture")
    assert result["content"] == "supplier" and result["text_truncated"] is True
    assert result["page_count"] == 1 and result["pages"] == [{"page": 1, "text": "supplier"}]


def test_case_mentions_preserve_evidence_without_inventing_roles():
    pages = [{"page": 2, "text": "子公司作为第三人。海南省高院作出（2025）琼民终 9 号裁定，驳回上诉。"},
             {"page": 3, "text": "再次提及（2025）琼民终9号。"}]
    mentions = extract_case_mentions(pages)
    assert len(mentions) == 1 and mentions[0]["case_number"] == "（2025）琼民终9号"
    assert mentions[0]["page"] == 2 and mentions[0]["party_role"] is None
    assert mentions[0]["verification_status"] == "PENDING" and "子公司作为第三人" in mentions[0]["excerpt"]


def test_disclosure_keyword_locations_are_verbatim_bounded_and_not_facts():
    source_text = "甲" * 160 + "公司披露主要客户与供应商名单，签订采购合同。子公司发生诉讼。" + "乙" * 600
    pages = [{"page": number, "text": source_text} for number in range(1, 40)]
    mentions = extract_disclosure_mentions(pages)
    assert len(mentions) == 20
    assert {item["topic"] for item in mentions} == {"供应链", "经营往来", "司法事项"}
    assert all(item["verification_status"] == "PENDING" and len(item["excerpt"]) <= 500 for item in mentions)
    assert all(item["excerpt"] in pages[item["page"] - 1]["text"] for item in mentions)
    assert all(set(item) == {"topic", "page", "excerpt", "verification_status"} for item in mentions)


def test_supply_chain_selection_reserves_annual_and_operating_slots():
    titles = ["2025年年度报告", "关于2025年年度报告的业绩说明会", "2026年半年度报告摘要",
              "关于收购供应链公司股权暨关联交易的公告", "2025年年度报告（英文版）",
              "关于签订采购合同的公告", "2026年半年度报告", "2025年年度报告（修订版）",
              "关于2026年日常关联交易预计的公告", "关于2025年年度报告的审核意见"]
    records = [{"announcement_id": str(i), "title": title, "published_at": f"2026-09-{i + 1:02d}T00:00:00Z"}
               for i, title in enumerate(titles)]
    selected = select_supply_chain_disclosures(records, 2)
    assert [item["announcement_id"] for item in selected] == ["7", "8"]
    assert [item["selection_basis"] for item in selected] == ["完整年度报告原文", "直接供销经营披露"]
    assert [item["announcement_id"] for item in select_supply_chain_disclosures(records, 1)] == ["7"]
    assert {item["announcement_id"] for item in select_supply_chain_disclosures(records, 10)} == {"0", "5", "7", "8"}
    assert select_supply_chain_disclosures(records[1:5], 2) == []


def test_client_caches_actual_request_and_raw_response(tmp_path):
    calls = []

    def handle(request):
        calls.append(request)
        return httpx.Response(200, json=profile_payload())

    with httpx.Client(transport=httpx.MockTransport(handle)) as http:
        result = CompanySourcesClient(client=http, cache_dir=tmp_path).fetch_company_profile("CN_A", "601398")
    assert result.status == "SUCCESS" and result.source_name == "EASTMONEY"
    assert result.source_kind == "AGGREGATOR" and result.published_at is None
    assert result.request["params"]["reportName"] == "RPT_F10_ORG_BASICINFO"
    cached = json.loads(next(tmp_path.glob("*.json")).read_text(encoding="utf-8"))
    assert cached["raw"] == profile_payload() and cached["request"]["method"] == "GET"
    assert "filter" in calls[0].url.params


def test_holders_use_actual_latest_returned_period():
    def handle(request):
        assert request.url.path.endswith("PageAjax") and "date" not in request.url.params
        return httpx.Response(200, json={"sdgd": [
            {"SECUCODE": "000001.SZ", "HOLDER_NAME": "Earlier", "END_DATE": "2025-12-31", "HOLD_NUM": 1},
            {"SECUCODE": "000001.SZ", "HOLDER_NAME": "Latest", "END_DATE": "2026-06-30", "HOLD_NUM": 2},
        ]})

    with httpx.Client(transport=httpx.MockTransport(handle)) as http:
        client = CompanySourcesClient(client=http)
        result = client.fetch_shareholders("CN_A", "000001")
        unsupported = client.fetch_shareholders("HK", "01398")
    assert result.status == "SUCCESS" and len(result.records) == 1
    assert result.records[0]["report_date"] == "2026-06-30"
    assert unsupported.status == "UNSUPPORTED" and unsupported.records == []


def test_access_denial_and_config_are_not_connection_success():
    with httpx.Client(transport=httpx.MockTransport(lambda _: httpx.Response(403))) as http:
        result = CompanySourcesClient(client=http).fetch_company_profile("CN_A", "601398")
    assert result.status == "AUTH_REQUIRED" and not result.records
    statuses = authorized_source_capabilities({})
    assert all(item["status"] == "AUTH_REQUIRED" and not item["connected"] for item in statuses)
    configured = authorized_source_capabilities({"COURT_DATA_API_URL": "https://example.invalid/cases", "COURT_DATA_API_TOKEN": "private"})
    assert configured[1]["status"] == "CONFIGURED_UNVERIFIED" and not configured[1]["connected"]
    assert "private" not in json.dumps(configured)


def test_legal_download_failure_stays_a_title_only_lead():
    def handle(request):
        if request.url.path.endswith("topSearch/query"):
            return httpx.Response(200, json=[{"code": "600221", "orgId": "gssh0600221"}])
        if request.url.path.endswith("hisAnnouncement/query"):
            return httpx.Response(200, json={"announcements": [{"secCode": "600221", "announcementId": "123", "announcementTitle": "诉讼",
                "announcementTime": 1741017600000, "adjunctUrl": "finalpage/2025-03-04/123.PDF"}]})
        return httpx.Response(403, text="Access denied")

    with httpx.Client(transport=httpx.MockTransport(handle)) as http:
        result = CompanySourcesClient(client=http).fetch_legal_disclosures("600221", end_date="2025-12-31", max_documents=1)
    assert result.status == "PARTIAL" and len(result.records) == 1
    assert result.records[0]["extraction_status"] == "TITLE_ONLY"
    assert result.records[0]["content"] is None and result.records[0]["case_mentions"] == []
    assert len(result.raw["queries"]) == 2


def test_business_search_preserves_raw_queries_without_inferring_facts():
    def handle(request):
        if request.url.path.endswith("topSearch/query"):
            return httpx.Response(200, json=[{"code": "601766", "orgId": "gssh0601766"}])
        if request.url.path.endswith("hisAnnouncement/query"):
            return httpx.Response(200, json={"announcements": [{"secCode": "601766", "announcementId": "321", "announcementTitle": "签订合同公告",
                "announcementTime": 1741017600000, "adjunctUrl": "finalpage/2025-03-04/321.PDF"}]})
        return httpx.Response(403, text="Access denied")

    with httpx.Client(transport=httpx.MockTransport(handle)) as http:
        result = CompanySourcesClient(client=http).fetch_business_disclosures("601766", end_date="2025-12-31", max_documents=1)
    assert result.source_code == "CNINFO_BUSINESS_DISCLOSURES" and result.source_kind == "OFFICIAL_DISCLOSURE"
    assert len(result.raw["queries"]) == 5 and len(result.records) == 1
    assert result.records[0]["content"] is None
    assert "facts" not in result.records[0] and not result.records[0]["case_mentions"]


def test_supply_chain_archives_bounded_annual_report_as_evidence_not_relationship(tmp_path):
    def handle(request):
        if request.url.path.endswith("topSearch/query"):
            return httpx.Response(200, json=[{"code": "601766", "orgId": "gssh0601766"}])
        if request.url.path.endswith("hisAnnouncement/query"):
            return httpx.Response(200, json={"announcements": [{"secCode": "601766", "announcementId": "456",
                "announcementTitle": "2025年年度报告", "announcementTime": 1741017600000,
                "adjunctUrl": "finalpage/2025-03-04/456.PDF"}]})
        return httpx.Response(200, content=b"%PDF-fixture")

    extraction = {"content": "Supplier A is anonymous", "pages": [{"page": 1, "text": "Supplier A is anonymous"}],
        "page_count": 180, "text_truncated": True, "binary_sha256": "fixture-sha",
        "binary_size": 12, "content_scope": "PDF_EXTRACTED_TEXT", "extraction_status": "TEXT_AVAILABLE"}
    with httpx.Client(transport=httpx.MockTransport(handle)) as http, \
         patch("app.connectors.company_sources.extract_pdf_text", return_value=extraction):
        result = CompanySourcesClient(client=http, cache_dir=tmp_path).fetch_supply_chain_disclosures(
            "601766", end_date="2025-12-31", max_documents=1)
    assert result.source_code == "CNINFO_SUPPLY_CHAIN_DISCLOSURES" and result.status == "PARTIAL"
    assert len(result.records) == 1 and len(result.raw["queries"]) == 7
    record = result.records[0]
    assert set(record["matched_search_terms"]) == {"供应商", "客户", "供应链", "年度报告", "采购", "销售", "日常关联交易"}
    assert record["selection_basis"] == "完整年度报告原文"
    assert record["text_truncated"] and record["page_count"] == 180
    assert record["case_mentions"] == [] and record["entity_mentions"] == [] and "facts" not in record
    assert any("Anonymous" in warning for warning in result.warnings)
    assert any("truncated" in warning for warning in result.warnings)
    cache = json.loads(next(tmp_path.glob("supply_chain_CN_A_601766_*.json")).read_text(encoding="utf-8"))
    assert cache["records"][0]["content"] == extraction["content"]


@pytest.mark.parametrize("http_status,expected", [(200, "EMPTY"), (403, "AUTH_REQUIRED"), (500, "UNAVAILABLE")])
def test_supply_chain_empty_or_failed_lookup_does_not_report_relations(http_status, expected):
    with httpx.Client(transport=httpx.MockTransport(lambda _: httpx.Response(http_status, json=[]))) as http:
        result = CompanySourcesClient(client=http).fetch_supply_chain_disclosures("601766", end_date="2025-12-31")
    assert result.status == expected and result.records == []


def test_market_cap_units_snapshot_and_cross_listing_scope():
    fields = [""] * 46
    fields[1], fields[2], fields[3], fields[30], fields[45] = "工商银行", "601398", "8.07", "20260918161434", "28761.98"
    text = 'v_sh601398="' + "~".join(fields) + '";'
    row = parse_market_cap(text, "CN_A", "601398")[0]
    assert row["total_market_cap"] == 2876198000000
    assert row["currency"] == "CNY" and row["source_unit"] == "CNY_100M"
    assert row["snapshot_at"] == "2026-09-18T16:14:34+08:00"
    assert row["cross_listing_additive"] is False
    assert parse_market_cap(text, "HK", "01398") == []
    assert parse_market_cap(text, "CN_A", "000001") == []
    fields[45] = "nan"
    assert parse_market_cap('v_sh601398="' + "~".join(fields) + '";', "CN_A", "601398") == []


def test_market_cap_hk_does_not_request_or_guess_scope():
    with httpx.Client(transport=httpx.MockTransport(lambda _: pytest.fail("HK should not fetch ambiguous cap"))) as http:
        result = CompanySourcesClient(client=http).fetch_market_cap("HK", "01398")
    assert result.status == "UNSUPPORTED" and result.records == []


@pytest.mark.parametrize("market,symbol", [("US", "AAPL"), ("CN_A", "601398x"), ("HK", "123456")])
def test_invalid_symbol_cannot_modify_source_filter(market, symbol):
    with pytest.raises(ValueError):
        parse_company_profile({}, market, symbol, "2026-09-21T00:00:00Z")
