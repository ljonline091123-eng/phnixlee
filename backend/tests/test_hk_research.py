from datetime import datetime, timedelta, timezone
from unittest.mock import patch

import httpx
import pytest
from fastapi import BackgroundTasks, FastAPI
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, func, select
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

import app.models  # noqa: F401
from app.api.stocks import router
from app.connectors.hk_research import (
    SUMMARY_NOTICE, _fetch_html, fetch_aastocks_report_detail,
    fetch_hk_research_sections, parse_aastocks_reports, parse_etnet_forecast,
)
from app.db.base import Base
from app.db.session import get_db
from app.models.market_data import DataFetchLog, DataInterface, DataSource, StockF10Cache, StockSymbol
from app.models.research_data import StockBrokerResearchReport, StockEarningsConsensus, StockInstitutionForecast
from app.services.catalog import seed_default_catalog
from app.services.f10 import build_research_contract, normalize_f10_sections
from app.services.hk_research import hk_research_needs_refresh, queue_hk_research_sync, sync_hk_research
from app.services.research_store import get_or_fetch_report_detail, load_research_sections, persist_research_sections


# Fixtures preserve the provider's colspan and units, which caused the original
# missing institution rows and are not represented by a simple mocked record.
ETNET_HTML = """<html><title>02533 黑芝麻智能 - 盈利预测</title>
<table><tr><td>财政年度</td><td>纯利/(亏损) (百万元人民币)</td><td>每股盈利 (分)</td><td>最高 (百万元人民币)</td><td>最低 (百万元人民币)</td></tr>
<tr><td>2026</td><td>-1,050.00</td><td>-147.00</td><td>-1050</td><td>-1050</td></tr>
<tr><td>2027</td><td>0</td><td>0</td><td>0</td><td>0</td></tr>
<tr><td>2028</td><td>--</td><td>--</td><td>--</td><td>--</td></tr></table>
<table><tr><td>财政年度</td><td>纯利/(亏损) (百万元人民币)</td><td>每股盈利 (分)</td><td>证券商</td><td>评级</td><td colspan="2">目标价 (港元)</td><td>更新日期</td></tr>
<tr><td>2026</td><td>-1050</td><td>-147</td><td>国泰君安</td><td>增持</td><td>22.58</td><td></td><td>03/06/2026</td></tr>
<tr><td>2027</td><td>-658</td><td>-92</td><td>国泰君安</td><td>--</td><td>--</td><td></td><td>03/06/2026</td></tr></table>
<table><tr><td>强烈买入(1) 0 买入(2) 1 持有(3) 0 沽出(4) 0 立即沽出(5) 0 平均评级 2.00</td></tr></table></html>"""

AASTOCKS_HTML = """<html><div ref="NOW.1517232"><div class="newshead4"><a href="/sc/stocks/analysis/stock-aafn-con/02533/AAFN/NOW.1517232/research-report">《大行》招商证券国际予黑芝麻智能「增持」评级</a></div><script>ConvertToLocalTime({dt:'2026/04/13 15:50'})</script><div class="newscontent4">智驾晶片业务增长。</div></div></html>"""


@pytest.fixture
def sessions():
    engine = create_engine("sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool)
    Base.metadata.create_all(engine)
    factory = sessionmaker(bind=engine, autoflush=False)
    with factory() as db:
        seed_default_catalog(db)
        source = db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE"))
        db.add(StockSymbol(market="HK", symbol="02533", name="黑芝麻智能", exchange="HKEX", source_id=source.id))
        db.commit()
    yield factory
    engine.dispose()


def source_html(url):
    return ETNET_HTML if "etnet" in url else AASTOCKS_HTML


def test_etnet_colspan_keeps_broker_date_and_preserves_negative_zero_units():
    data = parse_etnet_forecast(ETNET_HTML, "2533.HK")
    assert len(data["earnings_forecast"]) == 4  # no invented 2028 estimate
    profit = data["earnings_forecast"][0]
    assert profit["mean"] == -1050 and profit["unit"] == "百万元人民币" and profit["currency"] == "CNY"
    assert data["earnings_forecast"][2]["mean"] == 0
    broker = data["institution_forecast"][0]
    assert broker["report_date"] == "2026-06-03"
    assert broker["forecast"]["net_profit"]["2027"] == -658
    assert broker["forecast_units"] == {"eps": "分/股", "net_profit": "百万元人民币"}
    assert broker["target_price"] == 22.58 and broker["target_price_currency"] == "HKD"
    assert data["provider_rating_statistics"]["reference_period"] == "CURRENT_SNAPSHOT"
    assert data["source_metadata"]["source_content_hash"]


def test_wrong_security_and_changed_forecast_pages_are_rejected():
    with pytest.raises(ValueError, match="股票代码"):
        parse_etnet_forecast(ETNET_HTML, "00700")
    with pytest.raises(ValueError, match="结构"):
        parse_etnet_forecast("<title>02533</title><p>验证码</p>", "02533")
    assert not parse_etnet_forecast("<title>02533</title><p>暂无预测</p>", "02533")["earnings_forecast"]
    with pytest.raises(ValueError, match="其他证券"):
        parse_aastocks_reports(AASTOCKS_HTML, "00700")


def test_aastocks_reports_and_details_are_security_scoped_and_labelled_summaries():
    reports = parse_aastocks_reports(AASTOCKS_HTML, "02533")
    assert len(reports) == 1 and reports[0]["report_date"] == "2026-04-13"
    assert reports[0]["institution"] == "招商证券国际" and reports[0]["rating"] == "增持"
    assert reports[0]["content_notice"] == SUMMARY_NOTICE
    html = '<div class="newshead5">大行报告</div><div class="newscontent5">公开报告摘要。<script>广告</script><span class="newsRelatedHeadline">相关推荐</span></div>'
    with patch("app.connectors.hk_research._fetch_html", return_value=html) as fetch:
        detail = fetch_aastocks_report_detail(reports[0]["external_id"], reports[0]["detail_url"], "02533")
        assert detail["content"] == "公开报告摘要。" and detail["content_hash"]
        for invalid in (reports[0]["detail_url"].replace("02533", "00700"), "https://other.test/x"):
            with pytest.raises(ValueError, match="不一致"):
                fetch_aastocks_report_detail(reports[0]["external_id"], invalid, "02533")
        assert fetch.call_count == 1


def test_empty_issuer_reports_ignore_sidebar_videos_but_require_scoped_identity_and_message():
    page = '<title>AASTOCKS 财经新闻 - 世大控股 (08003.HK)</title><aside><a class="newshead4" href="/aatv/sc/video/7622">相关推荐</a></aside><div id="cp_ucAAFNSearch_pMsg">暂时没有相关新闻。</div>'
    assert parse_aastocks_reports(page, "08003") == []
    with pytest.raises(ValueError, match="其他证券"):
        parse_aastocks_reports(page, "08031")
    with pytest.raises(ValueError, match="空状态"):
        parse_aastocks_reports(page.replace('id="cp_ucAAFNSearch_pMsg"', 'id="sidebar"'), "08003")
    generic = '<title>AASTOCKS 财经新闻</title><div id="cp_ucAAFNSearch_pMsg">暂时没有相关新闻。</div>'
    assert parse_aastocks_reports(generic, "08059") == []
    empty = '<title>08003 盈利预测</title><p>暂无预测</p>'
    assert parse_etnet_forecast(empty, "08003")["source_metadata"]["raw_html"] == empty


def test_independent_source_failure_retains_other_provider_and_does_not_fake_completion():
    def fail_etnet(url):
        if "etnet" in url:
            raise httpx.ConnectTimeout("连接失败")
        return AASTOCKS_HTML
    with patch("app.connectors.hk_research._fetch_html", side_effect=fail_etnet):
        data = fetch_hk_research_sections("02533")
    assert data["status"] == "PARTIAL" and len(data["reports"]) == 1
    assert next(row for row in data["source_metadata"] if row["source_code"] == "ETNET_HK")["status"] == "FAILED"
    assert not data["earnings_forecast"]
    with patch("app.connectors.hk_research._fetch_html", side_effect=RuntimeError("连接失败")):
        data = fetch_hk_research_sections("02533")
    assert data["status"] == "MISSING" and all(row["status"] == "FAILED" for row in data["source_metadata"])


def test_http_retries_are_bounded_and_non_retryable_errors_are_not_repeated():
    attempts = []
    def transient(request):
        attempts.append(request.url)
        return httpx.Response(503 if len(attempts) == 1 else 200, text="<p>OK</p>")
    client = httpx.Client(transport=httpx.MockTransport(transient))
    with patch("app.connectors.hk_research.httpx.Client", return_value=client):
        assert _fetch_html("https://www.etnet.com.hk/test") == "<p>OK</p>"
    assert len(attempts) == 2
    attempts.clear()
    def rejected(request):
        attempts.append(request.url)
        return httpx.Response(403)
    with patch("app.connectors.hk_research.httpx.Client", return_value=httpx.Client(transport=httpx.MockTransport(rejected))):
        with pytest.raises(httpx.HTTPStatusError):
            _fetch_html("https://www.etnet.com.hk/test")
    assert len(attempts) == 1


def test_sync_is_idempotent_preserves_units_and_normalized_broker_forecasts(sessions):
    with sessions() as db, patch("app.connectors.hk_research._fetch_html", side_effect=source_html):
        sync_hk_research(db, "2533")
        sync_hk_research(db, "02533")
        assert db.scalar(select(func.count()).select_from(StockBrokerResearchReport)) == 1
        assert db.scalar(select(func.count()).select_from(StockEarningsConsensus)) == 4
        assert db.scalar(select(func.count()).select_from(StockInstitutionForecast)) == 1
        loaded = load_research_sections(db, "HK", "02533")
        assert loaded["institution_forecast"][0]["forecast"]["net_profit"]["2026"] == -1050
        assert next(row for row in loaded["earnings_forecast"] if row["forecast_year"] == "2027")["mean"] == "0.0"
        assert {row["unit"] for row in loaded["earnings_forecast"]} == {"百万元人民币", "分/股"}
        assert {row["source_code"] for row in loaded["earnings_forecast"]} == {"ETNET_HK"}
        projected = normalize_f10_sections({"research_sections": {**loaded, "market": "HK", "provider_rating_statistics": parse_etnet_forecast(ETNET_HTML, "02533")["provider_rating_statistics"]}})
        institution = next(section for section in projected["research_sections"]["sections"] if section["key"] == "institution_forecast")
        assert institution["forecast_units"]["net_profit"] == "百万元人民币"
        assert next(section for section in projected["research_sections"]["sections"] if section["key"] == "qa")["status"] == "MISSING"
        # A current provider snapshot must never overwrite the six-month window.
        assert all(row.get("source_code") != "ETNET_HK" for row in institution["rating_statistics"])
        assert not hk_research_needs_refresh(db, "02533")


def test_failed_refresh_retains_history_and_retries_after_short_interval(sessions):
    with sessions() as db, patch("app.connectors.hk_research._fetch_html", side_effect=source_html):
        sync_hk_research(db, "02533")
    with sessions() as db, patch("app.connectors.hk_research._fetch_html", side_effect=RuntimeError("网络中断")):
        result = sync_hk_research(db, "02533")
        assert result["reports"] and result["earnings_forecast"] and result["provider_rating_statistics"]
        assert result["status"] == "PARTIAL" and "失败" in result["message"]
        cache = db.scalar(select(StockF10Cache))
        cache.payload_json = {**cache.payload_json, "fetched_at": (datetime.now(timezone.utc) - timedelta(minutes=16)).isoformat()}
        db.commit()
        assert hk_research_needs_refresh(db, "02533")


def test_disabled_forecast_and_rating_interfaces_are_respected_separately(sessions):
    with sessions() as db, patch("app.connectors.hk_research._fetch_html", side_effect=source_html):
        forecast = db.scalar(select(DataInterface).where(DataInterface.interface_code == "HK_PROFIT_FORECAST_ON_DEMAND"))
        forecast.enabled = False
        db.commit()
        data = sync_hk_research(db, "02533")
        assert not data["earnings_forecast"] and data["provider_rating_statistics"]
        assert db.scalar(select(func.count()).select_from(StockInstitutionForecast)) == 0
        rating = db.scalar(select(DataInterface).where(DataInterface.interface_code == "HK_INSTITUTION_RATING_ON_DEMAND"))
        rating.enabled = False
        db.commit()
    with sessions() as db, patch("app.connectors.hk_research._fetch_html", side_effect=source_html) as fetch:
        sync_hk_research(db, "02533")
        assert fetch.call_count == 1 and "aastocks" in fetch.call_args.args[0]


def test_report_detail_is_saved_and_second_read_is_local(sessions):
    report = parse_aastocks_reports(AASTOCKS_HTML, "02533")[0]
    with sessions() as db:
        persist_research_sections(db, "HK", "02533", {"reports": [report]})
        db.commit()
        with patch("app.connectors.hk_research._fetch_html", return_value='<div class="newshead5">大行报告</div><div class="newscontent5">公开摘要正文</div>') as fetch:
            first = get_or_fetch_report_detail(db, "HK", "02533", "AASTOCKS_HK", report["external_id"])
            second = get_or_fetch_report_detail(db, "HK", "02533", "AASTOCKS_HK", report["external_id"])
            assert fetch.call_count == 1 and first["content"] == second["content"] == "公开摘要正文"
            assert second["content_kind"] == "PUBLIC_BROKER_SUMMARY"
            assert get_or_fetch_report_detail(db, "HK", "00700", "AASTOCKS_HK", report["external_id"]) is None


def test_background_api_logs_results_and_local_status_does_not_call_remote(sessions):
    app = FastAPI()
    app.include_router(router, prefix="/api/v1")
    def db_dependency():
        with sessions() as db:
            yield db
    app.dependency_overrides[get_db] = db_dependency
    with TestClient(app) as client, patch("app.services.hk_research.SessionLocal", sessions), patch("app.connectors.hk_research._fetch_html", side_effect=source_html) as fetch:
        submitted = client.post("/api/v1/stocks/HK/02533/research/sync")
        assert submitted.status_code == 200 and submitted.json()["queued"]
        status = client.get("/api/v1/stocks/HK/02533/research/sync")
        assert status.json()["counts"]["reports"] == 1 and status.json()["status"] == "PARTIAL"
        again = client.post("/api/v1/stocks/HK/02533/research/sync")
        assert not again.json()["queued"] and fetch.call_count == 2
        assert client.post("/api/v1/stocks/CN_A/000001/research/sync").status_code == 422
        assert client.post("/api/v1/stocks/HK/09999/research/sync").status_code == 404
    with sessions() as db:
        log = db.scalar(select(DataFetchLog))
        assert log.status == "PARTIAL" and log.persisted_count == 6 and log.completed_at


def test_duplicate_background_submissions_do_not_schedule_concurrent_jobs(sessions):
    from app.services.hk_research import _SYNC_KEYS, _SYNC_LOCK
    with sessions() as db:
        tasks = BackgroundTasks()
        try:
            assert queue_hk_research_sync(db, "02533", tasks)["queued"]
            assert not queue_hk_research_sync(db, "02533", tasks, force=True)["queued"]
            assert len(tasks.tasks) == 1
        finally:
            with _SYNC_LOCK:
                _SYNC_KEYS.discard("02533")
