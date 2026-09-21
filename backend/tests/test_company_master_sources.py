import io
import json
from zipfile import ZipFile

import httpx
import pytest

from app.connectors.company_master_sources import (
    CompanyMasterSourcesClient, build_master_result, parse_hkex_securities, parse_master_record,
)

NOW = "2026-09-21T10:00:00+00:00"


def page(rows):
    return {"report_name": "test", "source_url": "https://example.test/master", "retrieved_at": NOW,
            "raw": {"result": {"data": rows}}}


def raw(code="000001.SZ", issuer="10004085", name="平安银行股份有限公司", **kwargs):
    return {"SECUCODE": code, "ORG_CODE": issuer, "ORG_NAME": name, "SECURITY_TYPE": "A股", **kwargs}


def hkex_row(symbol, category="Equity", subcategory="Equity Securities (Main Board)"):
    return {"Stock Code": symbol, "Category": category, "Sub-Category": subcategory}


def test_neeq_uses_explicit_nq_identity_and_shares_company_between_layers():
    source = raw("832327.NQ", "10035739", "烟台海颐软件股份有限公司", SECURITY_TYPE="三板股", REG_NUM="91440300192185379H")
    results = [parse_master_record(source, market, "832327", NOW) for market in ("NEEQ", "NEEQ_INNOVATION")]
    assert results[0]["company"] == results[1]["company"]
    assert results[0]["company"]["source_issuer_id"] == "10035739"
    assert results[0]["company"]["identifier_scheme"] == "CN_USCC"
    assert results[0]["source_record"]["SECUCODE"] == "832327.NQ"
    assert "832327.NQ" in results[0]["evidence"]["content"]
    assert parse_master_record(source, "CN_A", "832327", NOW) is None


@pytest.mark.parametrize("category,subcategory", [("Exchange Traded Products", "Exchange Traded Funds"), ("Debt Securities", "Bonds"), ("Equity", "Real Estate Investment Trusts")])
def test_fund_manager_bond_or_other_nonordinary_profile_is_not_a_company_mapping(category, subcategory):
    snapshot = {"records": [hkex_row("02800", category, subcategory)], "retrieved_at": NOW}
    result = build_master_result([{"market": "HK", "symbol": "02800"}],
        [page([raw("02800.HK", "fund-manager", "恒生投资管理有限公司")])], snapshot)
    assert result["records"] == []
    assert result["exclusions"][0]["reason"] == "NON_EQUITY"
    assert result["exclusions"][0]["source_name"] == "HKEX"
    assert result["exclusions"][0]["source_record"]["Category"] == category


def test_hk_explicit_ordinary_class_required_and_future_source_label_not_publication_date():
    target = [{"market": "HK", "symbol": "00700"}]
    pages = [page([raw("00700.HK", "100", "腾讯控股有限公司")])]
    assert build_master_result(target, pages, None)["unresolved"][0]["reason"] == "HKEX_SECURITY_CLASSIFICATION_MISSING"
    result = build_master_result(target, pages, {"records": [hkex_row("00700")], "retrieved_at": NOW, "source_date_label": "Updated as at 22/09/2026"})
    record = result["records"][0]
    assert record["source_name"] == "EASTMONEY"
    assert record["company"]["properties_json"]["source_namespace"] == "EASTMONEY"
    assert record["evidence"]["published_at"] is None
    assert record["evidence"]["available_at"] == NOW
    assert record["security_classification_evidence"]["source_date_label"].endswith("22/09/2026")


def test_domestic_depositary_receipt_keeps_issuer_and_does_not_assume_china_domicile():
    result = build_master_result([{"market": "CN_A", "symbol": "689009"}],
        [page([raw("689009.SH", "10000019193", "九号有限公司", SECURITY_TYPE="中国存托凭证")])], None)
    record = result["records"][0]
    assert record["company"]["jurisdiction"] == "UNKNOWN"
    assert record["security_type"] == record["share_class"] == "DEPOSITARY_RECEIPT"


@pytest.mark.parametrize("subcategory", ["Investment Companies", "Trading Only Securities", "Depositary Receipts"])
def test_hkex_explicit_other_equity_subcategories_are_not_non_equity(subcategory):
    result = build_master_result([{"market": "HK", "symbol": "00700"}],
        [page([raw("00700.HK", "100", "Company")])],
        {"records": [hkex_row("00700", subcategory=subcategory)], "retrieved_at": NOW})
    assert len(result["records"]) == 1 and not result["exclusions"]
    assert result["records"][0]["share_class"] == ("DEPOSITARY_RECEIPT" if subcategory == "Depositary Receipts" else "ORDINARY")


def test_targets_partition_and_ambiguous_or_missing_issuer_is_not_guessed():
    targets = [{"market": "CN_A", "symbol": code} for code in ["000001", "000002", "000003", "000004", "000005"]]
    rows = [raw(), raw("000002.SZ", ""), raw("000003.SZ", "a"), raw("000003.SZ", "b"), raw("000004.SZ", SECURITY_TYPE="B股")]
    result = build_master_result(targets, [page(rows)], None)
    assert len(result["records"]) == 1
    assert len(result["unresolved"]) == 4
    assert {r["reason"] for r in result["unresolved"]} == {"INCOMPLETE_PROVIDER_IDENTITY", "CONFLICTING_PROVIDER_IDENTITIES", "SECURITY_CLASSIFICATION_MISMATCH", "COMPANY_PROFILE_MISSING"}


@pytest.mark.parametrize("reference", ["(A #605499)", "(A#605499)", "(A# 605499)"])
def test_ccass_explicit_mainland_reference_is_not_a_hong_kong_listing(reference):
    target = {"market": "HK", "symbol": "95499", "raw_payload": {"c": "95499", "n": "EASTROC " + reference},
        "ext_json": {"source_endpoint": "HKEX_SDW"}, "last_synced_at": NOW}
    result = build_master_result([target], [], None)
    excluded = result["exclusions"][0]
    assert excluded["reason"] == "NOT_HK_LISTED_SECURITY"
    assert excluded["referenced_symbol"] == "605499" and excluded["observed_at"] == NOW
    assert excluded["source_record"] == target["raw_payload"]
    assert not result["records"]
    # A number range, a name alone, or an unofficial original row is insufficient.
    target["ext_json"] = {}
    assert not build_master_result([target], [], None)["exclusions"]
    target["ext_json"] = {"source_endpoint": "HKEX_SDW"}
    target["raw_payload"]["n"] = "EASTROC"
    assert not build_master_result([target], [], None)["exclusions"]


def test_hkex_xlsx_header_and_source_date_parse_without_excel_dependency():
    stream = io.BytesIO()
    with ZipFile(stream, "w") as archive:
        archive.writestr("xl/sharedStrings.xml", '<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><si><t>Stock Code</t></si><si><t>Category</t></si><si><t>Sub-Category</t></si><si><t>Equity</t></si><si><t>Equity Securities (Main Board)</t></si></sst>')
        archive.writestr("xl/worksheets/sheet1.xml", '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData><row><c r="A1" t="inlineStr"><is><t>Updated as at 22/09/2026</t></is></c></row><row><c r="A3" t="s"><v>0</v></c><c r="B3" t="s"><v>1</v></c><c r="C3" t="s"><v>2</v></c></row><row><c r="A4"><v>700</v></c><c r="B4" t="s"><v>3</v></c><c r="C4" t="s"><v>4</v></c></row></sheetData></worksheet>')
    result = parse_hkex_securities(stream.getvalue())
    assert result["records"] == [hkex_row("00700")]
    assert result["source_date_label"] == "Updated as at 22/09/2026"


def test_batched_collection_resume_and_complete_page_count(tmp_path):
    requests = []
    def handler(request):
        requests.append(request)
        number = int(request.url.params["pageNumber"])
        return httpx.Response(200, json={"success": True, "result": {"count": 2, "pages": 2,
            "data": [raw(f"00000{number}.SZ")]}})
    with httpx.Client(transport=httpx.MockTransport(handler)) as http:
        client = CompanyMasterSourcesClient(tmp_path, client=http, page_size=1, delay=0)
        assert len(client.collect_profiles("DOMESTIC")) == 2
        assert len(client.collect_profiles("DOMESTIC")) == 2
    assert len(requests) == 2
    assert len(list((tmp_path / "pages").glob("*.json"))) == 2


def test_repeated_page_and_access_gate_are_not_silently_accepted(tmp_path):
    calls = []
    def handler(request):
        calls.append(request)
        return httpx.Response(200, json={"success": True, "result": {"count": 2, "pages": 2, "data": [raw()]}})
    with httpx.Client(transport=httpx.MockTransport(handler)) as http:
        client = CompanyMasterSourcesClient(tmp_path, client=http, page_size=1, delay=0)
        with pytest.raises(ValueError, match="repeated"):
            client.collect_profiles("DOMESTIC")
    calls.clear()
    def denied(request):
        calls.append(request)
        return httpx.Response(403)
    with httpx.Client(transport=httpx.MockTransport(denied)) as http:
        client = CompanyMasterSourcesClient(tmp_path / "denied", client=http, delay=0)
        result = client.collect([{"market": "CN_A", "symbol": "000001"}])
    assert len(calls) == 1
    assert result["status"] == "UNAVAILABLE"
    assert result["records"] == []
    assert json.loads((tmp_path / "denied" / "manifest.json").read_text(encoding="utf-8"))["failures"]
