from datetime import datetime, timedelta, timezone
from unittest.mock import patch

import httpx
import pytest
from sqlalchemy import create_engine, select
from sqlalchemy.orm import sessionmaker

import app.models  # noqa: F401
from app.db.base import Base
from app.connectors.investor_qa import OfficialQAClient, QACollectionError, QAPage, _source_time, parse_sse_page
from app.jobs.worker import JobWorker
from app.models.market_data import DataInterface, DataSource, StockF10Cache
from app.models.pipeline import ScheduledJob
from app.models.research_data import StockInvestorQA
from app.services.f10 import normalize_f10_sections
from app.services.investor_qa import TASK_TYPE, enqueue_qa_sync, qa_sync_status, read_qa_section, sync_qa_pages
from app.services.stock_on_demand import _merge_f10_payload


HTML = """<div class="m_feed_item" id="item-1789770">
<div class="m_feed_detail"><div class="m_feed_face"><p>投资者甲</p></div>
<div class="m_feed_txt"><a href="user.do?uid=173882">寒武纪(688256)</a>公司是否作过相关沟通？</div>
<div class="m_feed_from"><span>2026年09月24日 14:13</span></div></div>
<div class="m_feed_detail m_qa"><div class="m_feed_face"><p>寒武纪</p></div>
<div class="m_feed_txt">公司从未进行过相关沟通。</div>
<div class="m_feed_from"><span>2026年09月30日 18:18</span></div></div>
<div class="feed_quote">收藏成功，点赞提示不应成为回答</div></div>"""


def test_sse_parser_uses_actual_reply_and_does_not_end_on_short_page():
    page = parse_sse_page(HTML, "688256", page=1, uid="173882", url="https://sns.sseinfo.com/ajax/userfeeds.do?page=1")
    assert not page.complete  # One row is not proof of EOF.
    assert page.cursor == {"page": 2, "uid": "173882"}
    row = page.rows[0]
    assert row["question"] == "公司是否作过相关沟通？"
    assert row["answer"] == "公司从未进行过相关沟通。"
    assert row["answered_at"] == "2026-09-30 18:18:00"
    assert row["asked_at"] == "2026-09-24 14:13:00"
    assert row["source_code"] == "SSE_EINTERACTION"
    assert "寒武纪(688256)" in row["raw_html"]
    assert len(row["content_hash"]) == 64


def test_sse_empty_is_distinct_from_error_and_identity_mismatch():
    assert parse_sse_page('<a class="m_feed_note">近1个月内无回复</a>', "688256", page=8, uid="1", url="https://sns.sseinfo.com").complete
    for text in ("", "<html>访问验证</html>", HTML.replace("688256", "600000")):
        with pytest.raises(QACollectionError):
            parse_sse_page(text, "688256", page=1, uid="1", url="https://sns.sseinfo.com")


def test_official_client_looks_up_company_and_retries_network_with_timeouts():
    calls = []
    def provider(request):
        calls.append(request)
        if len(calls) == 1:
            raise httpx.ConnectError("offline", request=request)
        if request.url.path == "/company.do":
            assert request.url.params["stockcode"] == "688256"
            return httpx.Response(200, text='ajax/userfeeds.do?typeCode=company&type=11&uid=173882&page=1')
        return httpx.Response(200, text=HTML)
    with OfficialQAClient() as client, patch("app.connectors.investor_qa.time.sleep"):
        client.client.close()
        client.client = httpx.Client(transport=httpx.MockTransport(provider), timeout=10)
        page = client.fetch_page("688256")
    assert len(calls) == 3
    assert calls[-1].url.params["uid"] == "173882"
    assert page.rows[0]["answer"]


def test_cninfo_uses_exact_identity_and_shanghai_timezone():
    def provider(request):
        if "queryKeyboardInfo" in request.url.path:
            return httpx.Response(200, json={"data": [{"stockCode": "000002", "secid": "gssz2"}]})
        return httpx.Response(200, json={"total": 1, "totalPage": 1, "rows": [{
            "stockCode": "000002", "indexId": "q1", "mainContent": "经营情况？",
            "attachedContent": "请查阅公告", "pubDate": 0, "updateDate": 1000,
        }]})
    with OfficialQAClient() as client:
        client.client.close()
        client.client = httpx.Client(transport=httpx.MockTransport(provider))
        page = client.fetch_page("000002")
    assert page.complete
    assert page.rows[0]["source_code"] == "CNINFO"
    assert _source_time(0) == "1970-01-01 08:00:00"


def test_sse_collects_questions_after_replies_and_does_not_drop_pending_questions():
    requested = []
    def provider(request):
        requested.append(request.url.params["type"])
        if request.url.params["type"] == "11":
            return httpx.Response(200, text='<a class="m_feed_note">近1个月内无回复</a>')
        return httpx.Response(200, text='<a class="m_feed_note">近1个月内无提问</a>')
    with OfficialQAClient() as client:
        client.client.close()
        client.client = httpx.Client(transport=httpx.MockTransport(provider))
        replies = client.fetch_page("688256", {"page": 7, "uid": "173882"})
        assert not replies.complete and replies.cursor["feed_type"] == 10
        assert client.fetch_page("688256", replies.cursor).complete
    assert requested == ["11", "10"]


@pytest.fixture
def sessions():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(engine)
    factory = sessionmaker(bind=engine, autoflush=False)
    with factory() as db:
        db.add(DataSource(source_code="AKSHARE", source_name="公开问答", source_type="MARKET_DATA", adapter_type="AKSHARE", enabled=True))
        db.commit()
    yield factory
    engine.dispose()


def _page(question_id, *, complete=False, page=2):
    rows = [] if question_id is None else [{
        "question_id": question_id, "question": "问题" + question_id, "answer": "官方回复",
        "source_name": "上证 e 互动", "source_code": "SSE_EINTERACTION",
        "answered_at": "2026-09-30 18:18:00", "股票代码": "688256",
    }]
    return QAPage(rows, complete, {"page": page, "uid": "173882"}, "response-hash")


def test_durable_retry_resumes_committed_page_without_losing_rows(sessions):
    with sessions() as db:
        first = enqueue_qa_sync(db, "CN_A", "688256", 1)
        second = enqueue_qa_sync(db, "CN_A", "688256", 1)
        assert first["job_id"] == second["job_id"]
        db.get(ScheduledJob, first["job_id"]).max_attempts = 1
        db.commit()
    def execute(_task, payload):
        with sessions() as db:
            return sync_qa_pages(db, payload)
    worker = JobWorker(session_factory=sessions, task_types=(TASK_TYPE,), task_executor=execute)
    with patch.object(OfficialQAClient, "fetch_page", side_effect=[_page("q1"), QACollectionError("断网")]):
        assert worker.run_once()
    with sessions() as db:
        state = qa_sync_status(db, "CN_A", "688256")
        assert state["status"] == "RETRY" and state["stored_count"] == 1
        assert state["cursor"]["page"] == 2
        job = db.get(ScheduledJob, first["job_id"])
        assert job.status == "RETRY"  # Network retry survives the normal attempt limit.
        job.run_after = datetime.now(timezone.utc) - timedelta(seconds=1)
        db.commit()
    # A new worker simulates a process restart. It reads the persisted cursor.
    worker = JobWorker(session_factory=sessions, task_types=(TASK_TYPE,), task_executor=execute)
    with patch.object(OfficialQAClient, "fetch_page", side_effect=[_page("q2", page=3), _page(None, complete=True, page=4)]) as fetch:
        assert worker.run_once()
        assert fetch.call_args_list[0].args[1]["page"] == 2
    with sessions() as db:
        state = qa_sync_status(db, "CN_A", "688256")
        assert state["status"] == "COMPLETE" and state["stored_count"] == 2
        assert db.get(ScheduledJob, first["job_id"]).status == "COMPLETED"
        assert {row.source_code for row in db.scalars(select(StockInvestorQA))} == {"SSE_EINTERACTION"}
        section = read_qa_section(db, "CN_A", "688256")
        assert section["status"] == "AVAILABLE" and len(section["rows"]) == 2
        assert {row["answer"] for row in section["rows"]} == {"官方回复"}


def test_batch_f10_reports_partial_until_qa_sync_finishes(sessions):
    from app.services.stock_batch import _run_f10_refresh
    with sessions() as db, patch("app.services.stock_batch._source_for", return_value=db.get(DataSource, 1)), patch("app.services.stock_batch._fetch_and_cache_f10_extended_data", return_value={"profile": {"fields": {"公司": "寒武纪"}}}):
        enqueue_qa_sync(db, "CN_A", "688256", 1)
        result = _run_f10_refresh(db, "CN_A", "688256")
        assert result["status"] == "PARTIAL"
        assert result["investor_qa_sync"]["status"] == "PENDING"


def test_full_history_continues_as_durable_job_and_repeated_refresh_is_idempotent(sessions):
    with sessions() as db:
        enqueue_qa_sync(db, "CN_A", "688256", 1)
        payload = db.scalar(select(ScheduledJob)).payload_json
        with patch.object(OfficialQAClient, "fetch_page", return_value=_page("q1")):
            sync_qa_pages(db, payload, page_budget=1)
        jobs = db.scalars(select(ScheduledJob).order_by(ScheduledJob.id)).all()
        assert len(jobs) == 2
        assert jobs[1].idempotency_key.endswith(":2")
        with patch.object(OfficialQAClient, "fetch_page", return_value=_page(None, complete=True, page=3)):
            sync_qa_pages(db, jobs[1].payload_json)
        assert qa_sync_status(db, "CN_A", "688256")["stored_count"] == 1


def test_empty_source_preserves_previous_rows_and_disabled_interface_is_respected(sessions):
    with sessions() as db:
        db.add(StockInvestorQA(market="CN_A", symbol="688256", source_code="SSE_EINTERACTION", external_id="old", question="历史提问", answer="历史回复"))
        db.add(DataInterface(source_id=1, interface_code="CN_A_SSE_QA_ON_DEMAND", interface_name="上证问答", data_category="QA", request_mode="ON_DEMAND", adapter_method="official", supported_markets=["CN_A"], enabled=False))
        db.commit()
        assert enqueue_qa_sync(db, "CN_A", "688256", 1)["status"] == "DISABLED"
        assert db.scalar(select(ScheduledJob)) is None
        db.scalar(select(DataInterface)).enabled = True
        db.commit()
        enqueue_qa_sync(db, "CN_A", "688256", 1)
        with patch.object(OfficialQAClient, "fetch_page", return_value=_page(None, complete=True)):
            sync_qa_pages(db, db.scalar(select(ScheduledJob)).payload_json)
        assert qa_sync_status(db, "CN_A", "688256")["stored_count"] == 1
        assert qa_sync_status(db, "CN_A", "688256")["status"] == "EMPTY"


def test_stopped_interface_can_resume_checkpoint_after_its_job_completed(sessions):
    with sessions() as db:
        enqueue_qa_sync(db, "CN_A", "688256", 1)
        original = db.scalar(select(ScheduledJob))
        payload = dict(original.payload_json)
        db.get(DataSource, 1).enabled = False
        db.commit()
        assert sync_qa_pages(db, payload)["status"] == "DISABLED"
        original.status = "COMPLETED"
        db.get(DataSource, 1).enabled = True
        db.commit()
        status = enqueue_qa_sync(db, "CN_A", "688256", 1)
        assert status["job_id"] != original.id
        assert status["job_status"] == "PENDING"
        assert status["cursor"]["page"] == 1


def test_qa_state_is_visible_and_slow_preview_cannot_replace_it():
    existing = {"qa_sync": {"status": "RETRY", "source_name": "上证 e 互动", "message": "已保存，后台重试"}, "qa_source": "上证 e 互动", "qa_message": "已保存，后台重试", "reports": [{"title": "旧研报"}]}
    merged = _merge_f10_payload("research_sections", existing, {"qa_message": "巨潮暂无数据", "qa": []})
    assert merged["qa_message"] == "已保存，后台重试"
    merged["qa"] = [{"question_id": "1", "question": "问题", "answer": "答复"}]
    data = normalize_f10_sections({"research_sections": merged})
    section = next(row for row in data["research_sections"]["sections"] if row["key"] == "qa")
    assert section["status"] == "PARTIAL"
    assert section["source"] == "上证 e 互动"
    assert section["sync"]["status"] == "RETRY"
