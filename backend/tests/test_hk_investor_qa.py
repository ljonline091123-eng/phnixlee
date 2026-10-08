from datetime import datetime, timedelta, timezone
from unittest.mock import patch

import httpx
import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, func, select
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

import app.models  # noqa: F401
from app.api.stocks import router
from app.connectors.hk_investor_qa import (
    INTERFACE_CODE, ISSUER_FAQS, SOURCE_CODE, HongKongQAClient,
    coverage, parse_issuer_faq,
)
from app.connectors.investor_qa import OfficialQAClient, QACollectionError
from app.db.base import Base
from app.db.session import get_db
from app.jobs.worker import JobWorker
from app.models.market_data import DataFetchLog, DataInterface, DataSource, StockF10Cache, StockSymbol
from app.models.pipeline import ScheduledJob
from app.models.research_data import StockInvestorQA
from app.services.f10 import normalize_f10_sections
from app.services.investor_qa import TASK_TYPE, enqueue_qa_sync, qa_sync_status, read_qa_section, sync_qa_pages
from app.services.research_store import load_research_sections
from app.services.stock_on_demand import _merge_f10_payload


def faq_html(symbol="09868", answer="小鹏官网披露的回答"):
    return f'''<html><title>{ISSUER_FAQS[symbol]["name"]} - 投资者常见问答</title>
    <div class="nir-faq--item-wrapper">
      <div class="nir-faq--question">公司在哪里上市？</div>
      <div class="nir-faq--answer"><p>{answer}</p><a href="/reports">报告原文</a></div>
    </div></html>'''


def faq_page(symbol="09868", answer="小鹏官网披露的回答"):
    return parse_issuer_faq(faq_html(symbol, answer), symbol, url=ISSUER_FAQS[symbol]["url"])


@pytest.fixture
def sessions():
    engine = create_engine("sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool)
    Base.metadata.create_all(engine)
    factory = sessionmaker(bind=engine, autoflush=False)
    with factory() as db:
        source = DataSource(source_code=SOURCE_CODE, source_name="港股公司官网投资者问答",
                            source_type="OFFICIAL_PUBLIC", adapter_type=SOURCE_CODE, enabled=True)
        db.add(source)
        db.flush()
        db.add(DataInterface(source_id=source.id, interface_code=INTERFACE_CODE,
                             interface_name="港股公司投资者常见问答", data_category="QA",
                             request_mode="ON_DEMAND", adapter_method="official", supported_markets=["HK"], enabled=True))
        for symbol in ("09868", "09866", "02533"):
            db.add(StockSymbol(market="HK", symbol=symbol, name=coverage(symbol).get("issuer_name") or symbol,
                               exchange="HKEX", source_id=source.id))
        db.commit()
    yield factory
    engine.dispose()


@pytest.mark.parametrize("symbol", list(ISSUER_FAQS))
def test_faq_preserves_evidence_and_does_not_invent_published_dates(symbol):
    page = faq_page(symbol)
    row = page.rows[0]
    assert page.complete and page.cursor == {"page": 2}
    assert row["qa_kind"] == "OFFICIAL_FAQ" and row["evidence_status"] == "ISSUER_PUBLISHED"
    assert row["questioner"] is None and row["date_status"] == "UNDISCLOSED"
    assert all(row[key] is None for key in ("asked_at", "answered_at", "updated_at"))
    assert row["source_code"] == SOURCE_CODE and row["source_url"] == ISSUER_FAQS[symbol]["url"]
    assert row["raw_html"] and len(row["content_hash"]) == len(page.response_hash) == 64
    assert row["source_content_hash"] == page.response_hash and row["evidence_links"][0].startswith("https://ir.")
    assert row["question_id"] == faq_page(symbol, answer="更新的公司回答").rows[0]["question_id"]
    assert row["content_hash"] != faq_page(symbol, answer="更新的公司回答").rows[0]["content_hash"]


@pytest.mark.parametrize("html,url", [
    (faq_html().replace("小鹏", "其他公司"), ISSUER_FAQS["09868"]["url"]),
    (faq_html(), "https://other.test/faq"),
    ("<title>XPeng</title><p>访问验证</p>", ISSUER_FAQS["09868"]["url"]),
    (faq_html().replace('class="nir-faq--answer"', 'class="missing-answer"'), ISSUER_FAQS["09868"]["url"]),
])
def test_wrong_issuer_blocked_and_partial_html_are_errors_not_empty(html, url):
    with pytest.raises(QACollectionError):
        parse_issuer_faq(html, "09868", url=url)


def test_valid_empty_body_redirect_and_network_retry():
    requested = []
    def provider(request):
        requested.append(str(request.url))
        if len(requested) == 1:
            raise httpx.ConnectError("断网", request=request)
        if len(requested) == 2:
            return httpx.Response(301, headers={"Location": "/faq"})
        return httpx.Response(200, text=faq_html())
    with HongKongQAClient() as client, patch("app.connectors.investor_qa.time.sleep"):
        client.client.close()
        client.client = httpx.Client(transport=httpx.MockTransport(provider))
        assert client.fetch_page("9868").rows[0]["source_url"] == "https://ir.xiaopeng.com/faq"
    assert len(requested) == 3


@pytest.mark.parametrize("location", ["https://other.test/faq", "http://ir.xiaopeng.com/faq",
                                     "https://ir.xiaopeng.com:8443/faq", "https://user@ir.xiaopeng.com/faq", ""])
def test_unreviewed_redirect_is_rejected_before_requesting_target(location):
    requests = []
    def provider(request):
        requests.append(request)
        return httpx.Response(302, headers={"Location": location})
    with HongKongQAClient() as client:
        client.client.close()
        client.client = httpx.Client(transport=httpx.MockTransport(provider))
        with pytest.raises(QACollectionError):
            client.fetch_page("09868")
    assert len(requests) == 1


def test_queued_sync_is_idempotent_and_local_read_never_calls_a_share_client(sessions):
    with sessions() as db:
        first = enqueue_qa_sync(db, "HK", "09868", 1)
        assert first["job_id"] == enqueue_qa_sync(db, "HK", "09868", 1)["job_id"]
        payload = db.get(ScheduledJob, first["job_id"]).payload_json
        with patch.object(HongKongQAClient, "fetch_page", return_value=faq_page()), patch.object(OfficialQAClient, "fetch_page", side_effect=AssertionError("不应调用A股")):
            assert sync_qa_pages(db, payload)["status"] == "COMPLETE"
            assert sync_qa_pages(db, payload)["status"] == "COMPLETE"
        section = read_qa_section(db, "HK", "09868")
        assert section["title"] == "投资者问答" and section["status"] == "AVAILABLE"
        assert section["rows"][0]["date_status"] == "UNDISCLOSED"
        assert section["rows"][0]["source_code"] == SOURCE_CODE
        assert db.scalar(select(StockInvestorQA)).source_code == SOURCE_CODE
        assert section["rows"][0]["source_content_hash"] and section["rows"][0]["fetched_at"]
        assert not read_qa_section(db, "HK", "09866")["rows"]
        assert not qa_sync_status(db, "HK", "09868")["refresh_needed"]
        job = db.get(ScheduledJob, first["job_id"])
        job.status = "COMPLETED"
        db.commit()
        refresh = enqueue_qa_sync(db, "HK", "09868", 1, force=True)
        with patch.object(HongKongQAClient, "fetch_page", return_value=faq_page(answer="公司最新回答")):
            sync_qa_pages(db, db.get(ScheduledJob, refresh["job_id"]).payload_json)
        assert db.scalar(select(func.count()).select_from(StockInvestorQA)) == 1
        assert read_qa_section(db, "HK", "09868")["rows"][0]["answer"].startswith("公司最新回答")
        log = db.scalar(select(DataFetchLog))
        assert log.interface_code == INTERFACE_CODE and log.status == "SUCCESS"


def test_network_error_retries_durably_and_keeps_old_qa(sessions):
    with sessions() as db:
        db.add(StockInvestorQA(market="HK", symbol="09868", source_code=SOURCE_CODE, external_id="old",
                               question="历史问答", answer="已保存的公司回答"))
        db.commit()
        status = enqueue_qa_sync(db, "HK", "09868", 1)
        db.get(ScheduledJob, status["job_id"]).max_attempts = 1
        db.commit()
    def execute(_task, payload):
        with sessions() as db:
            return sync_qa_pages(db, payload)
    worker = JobWorker(session_factory=sessions, task_types=(TASK_TYPE,), task_executor=execute)
    with patch.object(HongKongQAClient, "fetch_page", side_effect=QACollectionError("网络中断")):
        assert worker.run_once()
    with sessions() as db:
        state = qa_sync_status(db, "HK", "09868")
        assert state["status"] == "RETRY" and state["stored_count"] == 1 and state["cursor"]["page"] == 1
        job = db.get(ScheduledJob, status["job_id"])
        assert job.status == "RETRY" and read_qa_section(db, "HK", "09868")["status"] == "PARTIAL"
        job.run_after = datetime.now(timezone.utc) - timedelta(seconds=1)
        db.commit()
    with patch.object(HongKongQAClient, "fetch_page", return_value=faq_page()):
        assert worker.run_once()
    with sessions() as db:
        assert qa_sync_status(db, "HK", "09868")["status"] == "COMPLETE"
        assert qa_sync_status(db, "HK", "09868")["stored_count"] == 2


def test_missing_black_sesame_source_is_honest_and_does_not_create_a_job(sessions):
    with sessions() as db:
        status = enqueue_qa_sync(db, "HK", "02533", 1)
        assert status["status"] == status["coverage_status"] == "MISSING"
        assert not status["supported"] and not status["refresh_needed"] and status["source_urls"]
        assert "不代表公司没有" in status["message"]
        assert db.scalar(select(ScheduledJob)) is None and db.scalar(select(StockInvestorQA)) is None
        assert read_qa_section(db, "HK", "02533")["status"] == "MISSING"


@pytest.mark.parametrize("target", ["source", "interface"])
def test_disabled_source_and_interface_keep_data_and_do_not_enqueue(sessions, target):
    with sessions() as db:
        record = db.get(DataSource, 1) if target == "source" else db.scalar(select(DataInterface))
        record.enabled = False
        db.commit()
        with patch.object(HongKongQAClient, "fetch_page") as fetch:
            assert enqueue_qa_sync(db, "HK", "09868", 1)["status"] == "DISABLED"
            assert qa_sync_status(db, "HK", "09868")["status"] == "DISABLED"
            assert not qa_sync_status(db, "HK", "09868")["refresh_needed"]
            assert read_qa_section(db, "HK", "09868")["status"] == "DISABLED"
            assert db.scalar(select(ScheduledJob)) is None
            fetch.assert_not_called()


def test_f10_projection_preserves_faq_metadata_and_independent_status(sessions):
    with sessions() as db:
        status = enqueue_qa_sync(db, "HK", "09868", 1)
        with patch.object(HongKongQAClient, "fetch_page", return_value=faq_page()):
            sync_qa_pages(db, db.get(ScheduledJob, status["job_id"]).payload_json)
        cache = db.scalar(select(StockF10Cache))
        merged = _merge_f10_payload("research_sections", cache.payload_json, {"qa": [], "qa_status": "UNAVAILABLE", "qa_message": "预览暂无问答"})
        assert merged["qa_status"] == "COMPLETE" and merged["qa_message"] == cache.payload_json["qa_message"]
        payload = {"research_sections": {**merged, **load_research_sections(db, "HK", "09868"), "market": "HK"}}
        projected = normalize_f10_sections(payload)
        qa = next(item for item in projected["research_sections"]["sections"] if item["key"] == "qa")
        assert qa["status"] == "AVAILABLE" and qa["title"] == "投资者问答"
        assert qa["rows"][0]["qa_kind"] == "OFFICIAL_FAQ" and qa["source_urls"]
        assert qa["rows"][0]["source_code"] == SOURCE_CODE


def test_api_normalizes_hk_symbols_and_isolates_issuers(sessions):
    app = FastAPI()
    app.include_router(router)
    def override_db():
        with sessions() as db:
            yield db
    app.dependency_overrides[get_db] = override_db
    with TestClient(app) as client:
        first = client.post("/stocks/HK/9868/research/qa/sync")
        assert first.status_code == 200 and first.json()["symbol"] == "09868"
        with sessions() as db, patch.object(HongKongQAClient, "fetch_page", return_value=faq_page()):
            sync_qa_pages(db, db.get(ScheduledJob, first.json()["job_id"]).payload_json)
        assert len(client.get("/stocks/HK/9868/research/qa").json()["rows"]) == 1
        assert not client.get("/stocks/HK/9866/research/qa").json()["rows"]
        assert client.post("/stocks/HK/2533/research/qa/sync").json()["status"] == "MISSING"
        assert client.post("/stocks/HK/99999/research/qa/sync").status_code == 404


def test_hk_batch_cannot_report_success_when_qa_source_is_missing(sessions):
    from app.services.stock_batch import _run_f10_refresh
    with sessions() as db:
        enqueue_qa_sync(db, "HK", "02533", 1)
        with patch("app.services.stock_batch._source_for", return_value=db.get(DataSource, 1)), patch("app.services.stock_batch._fetch_and_cache_f10_extended_data", return_value={"profile": {"fields": {"公司": "黑芝麻智能"}}}):
            result = _run_f10_refresh(db, "HK", "02533")
        assert result["status"] == "PARTIAL" and result["investor_qa_sync"]["status"] == "MISSING"
