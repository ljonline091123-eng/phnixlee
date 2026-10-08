"""Disclosure identity, cross-market classification and persistence regressions."""

from unittest.mock import Mock, patch
import json

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, select, func
from sqlalchemy.orm import Session
from sqlalchemy.pool import StaticPool

from app.main import app
from app.db.base import Base
from app.db.session import get_db
from app.models.market_data import DataSource, StockSymbol, StockNotice, StockF10Cache, DataFetchLog
from app.connectors.akshare_adapter import AkshareAdapter
from app.connectors.base import NoticeRecord
from app.services.f10 import classify_notice, _merge_local_report_notices, _fetch_and_cache_f10_extended_data
from app.services.stock_on_demand import StockOnDemandService, _merge_f10_payload
from app.services.notice_read_model import normalize_notice_url
from app.api.stocks import get_stock_f10


@pytest.mark.parametrize("title,source_type,expected", [
    ("2026 中期報告", "財務報表/環境、社會及管治資料 - [中期/半年度報告]", "财务业绩"),
    ("截至2026年6月30日止六個月中期業績公告", "公告及通告 - [中期業績]", "财务业绩"),
    ("2025年報", "財務報表/環境、社會及管治資料 - [年報 / 環境、社會及管治資料/報告]", "财务业绩"),
    ("2025環境、社會及管治報告", "財務報表/環境、社會及管治資料 - [環境、社會及管治資料/報告]", "其他公告"),
    ("環境、社會及管治報告", "財務報表/環境、社會及管治資料", "其他公告"),
    ("翌日披露報表", "翌日披露報表 - [股份購回]", "增持回购"),
    ("截至2026年9月30日止月份股份發行人的證券變動月報表", "月報表", "其他公告"),
    ("盈利警告", "公告及通告 - [內幕消息]", "风险提示"),
    ("中期業績延期披露公告", None, "风险提示"),
    ("對外投資及收購事項", None, "对外投资"),
    ("關連交易", None, "重大事项"),
    ("股東特別大會通告", None, "重大事项"),
    ("根據一般授權認購新股份", None, "重大事项"),
    ("董事調任", None, "重大事项"),
    ("抵押擔保公告", None, "抵押担保"),
    ("2025年年度报告", None, "财务业绩"),
    ("关于为子公司提供担保的公告", None, "抵押担保"),
    ("关于回购公司股份的公告", None, "增持回购"),
    ("股票交易异常波动风险提示公告", None, "风险提示"),
    ("2026 Interim Results Announcement", None, "财务业绩"),
])
def test_cross_market_notice_categories(title, source_type, expected):
    assert classify_notice(title, source_type) == expected


def test_notice_url_normalization_treats_query_order_as_same_source():
    f10_url = (
        "https://www.cninfo.com.cn/new/disclosure/detail?stockCode=000001"
        "&announcementId=1225475344&orgId=gssz0000001&announcementTime=2026-08-15"
    )
    notice_url = (
        "https://www.cninfo.com.cn/new/disclosure/detail?stockCode=000001"
        "&orgId=gssz0000001&announcementId=1225475344&announcementTime=2026-08-15"
    )
    assert normalize_notice_url(f10_url) == normalize_notice_url(notice_url)


@pytest.fixture
def db():
    engine = create_engine("sqlite:///:memory:", connect_args={"check_same_thread": False}, poolclass=StaticPool)
    Base.metadata.create_all(engine)
    with Session(engine) as session:
        session.add(DataSource(source_code="AKSHARE", source_name="公告测试来源", adapter_type="MOCK"))
        session.flush()
        source = session.scalar(select(DataSource))
        session.add(StockSymbol(market="HK", symbol="02533", name="黑芝麻智能", exchange="HKEX", source_id=source.id))
        session.commit()
        yield session
    engine.dispose()


def filing(title="2026 中期報告", period="2026-06-30", published="2026-09-30", filename="2026093000987_c.pdf"):
    return {
        "title": title, "report_name": title, "report_type": "半年报",
        "report_date": period, "notice_date": published,
        "url": "https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0930/" + filename,
        "source_name": "HKEXnews",
        "raw_notice_type": "財務報表/環境、社會及管治資料 - [中期/半年度報告]",
        "content_json": {"TITLE": title, "DOCUMENT_ID": filename},
    }


def stored_notice(db, row, source_id=None):
    notice = StockNotice(
        market="HK", symbol="02533", notice_date=row["notice_date"], title=row["title"],
        notice_type=row["raw_notice_type"], url=row["url"], content_json=row["content_json"],
        source_id=source_id or db.scalar(select(DataSource.id)),
    )
    db.add(notice)
    db.flush()
    return notice


def test_f10_links_same_notice_and_preserves_results_and_full_report(db):
    full = stored_notice(db, filing())
    result = filing("截至2026年6月30日止六個月中期業績公告", published="2026-08-31", filename="2026083100442_c.pdf")
    result["raw_notice_type"] = "公告及通告 - [中期業績]"
    results = stored_notice(db, result)
    other = DataSource(source_code="SECOND", source_name="第二来源", adapter_type="MOCK")
    db.add(other)
    db.flush()
    stored_notice(db, filing(), other.id)
    monthly = filing("股份發行人的證券變動月報表", filename="monthly.pdf")
    monthly["raw_notice_type"] = "月報表"
    stored_notice(db, monthly)
    payload = _merge_local_report_notices({}, list(db.scalars(select(StockNotice))), "HK", "02533")
    assert len(payload["reports"]) == 2
    assert payload["projection_basis"] == "STOCK_NOTICE"
    for entry in payload["reports"]:
        notice = db.get(StockNotice, entry["notice_id"])
        assert entry["url"] == notice.url
        assert entry["notice_date"] == notice.notice_date
        assert entry["title"] == notice.title
        assert entry["report_date"] == "2026-06-30"
        assert entry["category"] == "财务业绩"
        assert entry["source_name"] == "港交所披露易"
    assert next(row for row in payload["reports"] if row["title"] == full.title)["source_count"] == 2
    assert any(row["notice_id"] == results.id for row in payload["reports"])


def test_periods_without_documents_are_not_formal_reports(db):
    notice = stored_notice(db, filing())
    legacy = {"reports": [
        filing(),
        {"title": "2026 半年报指标", "report_type": "半年报", "report_date": "2026-06-30", "url": None},
        {"title": "2023 年报指标", "report_type": "年报", "report_date": "2023-12-31", "notice_date": "2023-12-31", "url": "None"},
        filing("2025年報", "2025-12-31", "2026-04-27", "annual.pdf"),
    ]}
    result = _merge_local_report_notices(legacy, [notice], "HK", "02533")
    assert len(result["reports"]) == 1
    assert result["reports"][0]["notice_id"] == notice.id
    assert len(result["unlinked_periods"]) == 1
    assert result["unlinked_periods"][0]["report_date"] == "2023-12-31"
    assert result["unlinked_periods"][0]["status"] == "MISSING"
    assert len(result["unlinked_documents"]) == 1
    assert result["status"] == "PARTIAL"
    assert _merge_f10_payload("published_reports", legacy, result) == result
    assert _merge_local_report_notices(result, [notice], "HK", "02533") == result


def test_f10_import_is_idempotent_and_archives_original_disclosures(db):
    service = StockOnDemandService(db)
    source = db.scalar(select(DataSource))
    row = filing()
    payload = {"reports": [row, row, {**row, "title": "仅有指标", "url": None}, {**row, "title": "错误日期", "notice_date": "bad-date"}]}
    with patch("app.services.lakehouse.archive_ingestion_payload") as archive:
        assert service.persist_report_notices(source, "HK", "02533", payload) == 1
        archive.assert_called_once()
        assert archive.call_args.kwargs["records"][0]["content_json"] == row["content_json"]
        assert service.persist_report_notices(source, "HK", "02533", payload) == 0
    notice = db.scalar(select(StockNotice))
    assert notice.notice_type == row["raw_notice_type"]
    assert notice.content_json == row["content_json"]
    assert db.scalar(select(func.count()).select_from(DataFetchLog)) == 1
    # A sparse F10 cache must not replace richer source evidence.
    with patch("app.services.lakehouse.archive_ingestion_payload") as archive:
        service.persist_report_notices(source, "HK", "02533", {"reports": [{**row, "content_json": {"sparse": True}}]})
        archive.assert_not_called()
    assert notice.content_json == row["content_json"]


def test_remote_f10_persists_announcements_and_preserves_historical_cache(db):
    source = db.scalar(select(DataSource))
    old = filing("2025年報", "2025-12-31", "2026-04-27", "annual.pdf")
    old["report_type"] = "年报"
    gap = {"report_date": "2023-12-31", "report_type": "年报", "title": "2023指标", "url": None}
    db.add(StockF10Cache(market="HK", symbol="02533", section="published_reports", source_id=source.id, payload_json={"reports": [old, gap]}))
    db.commit()
    adapter = Mock()
    adapter.fetch_extended_data.return_value = {"published_reports": {"reports": [filing()]}}
    with patch("app.services.f10.get_adapter", return_value=adapter), patch("app.services.lakehouse.archive_ingestion_payload"):
        result = _fetch_and_cache_f10_extended_data(db, source, "HK", "02533")
        assert len(result["published_reports"]["reports"]) == 2
        assert len(result["published_reports"]["unlinked_periods"]) == 1
        assert db.scalar(select(func.count()).select_from(StockNotice)) == 2
        snapshot = get_stock_f10("HK", "02533", local_only=True, db=db)
        assert len(snapshot.published_reports["reports"]) == 2
        assert all(db.get(StockNotice, item["notice_id"]) for item in snapshot.published_reports["reports"])
        # Provider outages retain all local documents and explicit gaps.
        adapter.fetch_extended_data.return_value = {"published_reports": {"reports": [], "message": "上游暂不可用"}}
        retry = _fetch_and_cache_f10_extended_data(db, source, "HK", "02533")
        assert len(retry["published_reports"]["reports"]) == 2
        assert len(retry["published_reports"]["unlinked_periods"]) == 1
        assert db.scalar(select(func.count()).select_from(StockNotice)) == 2


def test_notice_detail_and_filtered_page_use_the_same_database_record(db):
    notice = stored_notice(db, filing())
    db.commit()
    original = dict(app.dependency_overrides)
    app.dependency_overrides[get_db] = lambda: db
    try:
        client = TestClient(app)
        detail = client.get(f"/api/v1/stocks/HK/02533/notices/{notice.id}")
        assert detail.status_code == 200, detail.text
        assert detail.json()["title"] == notice.title
        page = client.get("/api/v1/stocks/HK/02533/notices/page", params={"category": "财务业绩"})
        assert page.status_code == 200, page.text
        assert page.json()["items"][0]["id"] == detail.json()["id"]
        assert page.json()["items"][0]["url"] == detail.json()["url"]
        assert client.get(f"/api/v1/stocks/HK/00001/notices/{notice.id}").status_code == 404
    finally:
        app.dependency_overrides.clear()
        app.dependency_overrides.update(original)


def test_a_share_report_period_is_not_publication_date(db):
    row = StockNotice(market="CN_A", symbol="000001", title="2025年年度报告", notice_date="2026-04-30", notice_type="年报", url="https://static.cninfo.com.cn/finalpage/annual.pdf", content_json={}, source_id=db.scalar(select(DataSource.id)))
    db.add(row)
    db.flush()
    projection = _merge_local_report_notices({}, [row], "CN_A", "000001")
    entry = projection["reports"][0]
    assert entry["report_date"] == "2025-12-31"
    assert entry["notice_date"] == "2026-04-30"
    assert entry["notice_id"] == row.id


def test_older_disclosures_fill_gaps_without_overwriting_latest():
    current = {"reports": [filing()]}
    old = filing("2025年報", "2025-12-31", "2026-04-27", "annual.pdf")
    merged = _merge_f10_payload("published_reports", current, {"reports": [old]})
    assert len(merged["reports"]) == 2
    assert merged["reports"][0]["report_date"] == "2026-06-30"


def test_hkex_financial_fetch_keeps_results_full_report_and_original_source_labels():
    adapter = AkshareAdapter()
    full = filing()
    results = filing("截至2026年6月30日止六個月中期業績公告", published="2026-08-31", filename="results.pdf")
    results["raw_notice_type"] = "公告及通告 - [中期業績]"
    records = [NoticeRecord("HK", "02533", row["notice_date"], row["title"], row["raw_notice_type"], row["url"], row["content_json"]) for row in (full, results)]
    with patch.object(adapter, "_fetch_hkex_notices", return_value=records):
        reports = adapter._fetch_hkex_financial_report_notices("02533")
    assert len(reports) == 2
    assert {row["raw_notice_type"] for row in reports} == {full["raw_notice_type"], results["raw_notice_type"]}
    assert all(row["content_json"] for row in reports)
    assert not adapter._is_hk_financial_report_title("董事會會議日期 - 中期業績")
    assert not adapter._is_hk_financial_report_title("環境、社會及管治報告 財務報表/環境、社會及管治資料 - [年報 / 環境、社會及管治資料/報告]")
    assert adapter._is_hk_financial_report_title("2024年全年業績公布")
    assert not adapter._is_hk_financial_report_title("致現有登記股東之函件及更改回條 - 刊發2024年年報")
    assert adapter._guess_hk_report_date("年報", "年报", "2025-04-23") is None


def test_corrected_full_report_is_formal_but_correction_notice_is_not(db):
    source_id = db.scalar(select(DataSource.id))
    corrected = StockNotice(market="CN_A", symbol="000001", title="2025年年度报告（更正后）", notice_date="2026-06-13", notice_type="年报", url="https://static.cninfo.com.cn/corrected.pdf", content_json={}, source_id=source_id)
    correction_notice = StockNotice(market="CN_A", symbol="000001", title="关于2025年年度报告的更正公告", notice_date="2026-06-13", notice_type="更正公告", url="https://static.cninfo.com.cn/correction.pdf", content_json={}, source_id=source_id)
    db.add_all([corrected, correction_notice])
    db.flush()
    projection = _merge_local_report_notices({}, [corrected, correction_notice], "CN_A", "000001")
    assert [row["notice_id"] for row in projection["reports"]] == [corrected.id]
    assert projection["reports"][0]["report_date"] == "2025-12-31"


def test_legacy_esg_document_is_not_report_gap(db):
    legacy = {"reports": [{
        "title": "2024年環境、社會及管治報告", "report_type": "财报",
        "report_date": "2024-12-31", "notice_date": "2025-04-08",
        "url": "https://www1.hkexnews.hk/esg.pdf",
    }]}
    result = _merge_local_report_notices(legacy, [], "HK", "02533")
    assert result["reports"] == []
    assert result["unlinked_documents"] == []
    assert result["other_disclosures"][0]["title"] == legacy["reports"][0]["title"]
    assert _merge_local_report_notices(result, [], "HK", "02533") == result


def test_hkex_date_contract_and_pagination_preserve_latest_reports():
    adapter = AkshareAdapter()
    recent = {"TITLE": "2026 中期報告", "DATE_TIME": "30/09/2026 16:30", "FILE_LINK": "/recent.pdf", "SHORT_TEXT": "中期/半年度報告"}
    old = {"TITLE": "2024 中期報告", "DATE_TIME": "30/08/2024 16:30", "FILE_LINK": "/old.pdf", "SHORT_TEXT": "中期/半年度報告"}
    response1, response2 = Mock(), Mock()
    response1.json.return_value = {"result": json.dumps([recent]), "recordCnt": 116, "loadedRecord": 100, "hasNextRow": True}
    response2.json.return_value = {"result": json.dumps([recent, old]), "recordCnt": 116, "loadedRecord": 116, "hasNextRow": False}
    client = Mock()
    client.get.side_effect = [response1, response2]
    with patch.object(adapter, "_lookup_hkex_stock_id", return_value="1000221013"), patch("app.connectors.akshare_adapter.httpx.Client") as factory:
        factory.return_value.__enter__.return_value = client
        notices = adapter._fetch_hkex_notices("02533", "2024-01-01", "2026-10-08")
    assert {r.notice_date for r in notices} == {"2026-09-30", "2024-08-30"}
    assert client.get.call_count == 2
    params = client.get.call_args.kwargs["params"]
    assert params["fromDate"] == "20240101"
    assert params["toDate"] == "20261008"
    assert params["rowRange"] == "116"


def test_hkex_title_search_also_uses_compact_dates():
    adapter = AkshareAdapter()
    client = Mock()
    client.get.return_value.json.return_value = {"result": "[]"}
    with patch.object(adapter, "_lookup_hkex_stock_id", return_value="1000221013"), patch("app.connectors.akshare_adapter.httpx.Client") as factory:
        factory.return_value.__enter__.return_value = client
        assert adapter._fetch_hkex_notices_by_title("02533", "中期報告", "2024-01-01", "2026-10-08") == []
    params = client.get.call_args.kwargs["params"]
    assert params["fromDate"] == "20240101"
    assert params["toDate"] == "20261008"
    assert adapter._hkex_query_date("20261008") == "20261008"
    with pytest.raises(ValueError):
        adapter._hkex_query_date("2026-99-99")


def test_legacy_backfill_is_incremental_and_reports_archive_failures(db):
    from scripts.backfill_notice_report_links import reconcile

    db.add(StockF10Cache(market="HK", symbol="02533", section="published_reports", source_id=db.scalar(select(DataSource.id)), payload_json={"reports": [filing()]}))
    db.commit()
    preview = reconcile(db)
    assert preview["unlinked_documents"] == 1
    assert db.scalar(select(func.count()).select_from(StockNotice)) == 0
    with patch("app.services.lakehouse.archive_ingestion_payload", side_effect=RuntimeError("存储暂不可用")):
        result = reconcile(db, apply=True)
    assert result["imported_or_enriched"] == 1
    assert result["unlinked_documents"] == 0
    assert "存储暂不可用" in result["errors"][0]["error"]
    with patch("app.services.lakehouse.archive_ingestion_payload") as archive:
        repeat = reconcile(db, apply=True)
        archive.assert_not_called()
    assert repeat["imported_or_enriched"] == 0
    assert db.scalar(select(func.count()).select_from(StockNotice)) == 1
    assert db.scalar(select(func.count()).select_from(DataFetchLog)) == 1
