"""Regression tests use isolated databases and Skill files; no production writes."""
import json
from datetime import datetime, timezone
from types import SimpleNamespace

import pytest
from fastapi import HTTPException
from fastapi import FastAPI
from fastapi.testclient import TestClient
from sqlalchemy import select, func

from tests.test_stock_batch_governance import _database
from app.api.stock_batch import _job_view, submit_stock_batch_governance, _normalize_request
from app.api.stock_batch import router as stock_batch_router
from app.db.session import get_db
from app.connectors.base import NewsRecord, NoticeRecord
from app.models.ai_hub import ModelSkill, ModelSkillRevision, KnowledgeBase, KnowledgeDocument
from app.models.lakehouse import DocumentChunkVersion
from app.models.foundation import FoundationEntity, FoundationSecurity, FoundationListing, FoundationFact, FoundationEvidence
from app.models.market_data import StockSymbol, StockNews, StockNotice, StockF10Cache
from app.models.pipeline import PipelineRun, ScheduledJob
from app.schemas.stock_batch import StockBatchGovernanceRequest
from app.services import document_event_governance as events, governance_actions as actions, skill_files
from app.services.model_hub import seed_default_skills
from app.services.stock_on_demand import StockOnDemandService


@pytest.fixture
def database(monkeypatch, tmp_path):
    monkeypatch.setattr(skill_files, "SKILL_ROOT", tmp_path / "skill_docs")
    temporary, engine, sessions = _database()
    try:
        with sessions() as db:
            seed_default_skills(db)
            db.commit()
        yield sessions
    finally:
        engine.dispose()
        temporary.cleanup()


def documents(db, *, notice_body=True):
    stock = db.scalar(select(StockSymbol).where(StockSymbol.symbol == "000001"))
    company = FoundationEntity(name="测试公司", entity_type="COMPANY", jurisdiction="CN")
    db.add(company)
    db.flush()
    security = FoundationSecurity(entity_id=company.id)
    identity = FoundationEvidence(entity_id=company.id, source_name="测试", source_key="identity", title="身份",
        content="identity", content_hash="identity", fingerprint="identity", version=1, available_at=datetime.now(timezone.utc))
    db.add_all([security, identity])
    db.flush()
    db.add(FoundationListing(security_id=security.id, stock_symbol_id=stock.id, market=stock.market,
        symbol=stock.symbol, name=stock.name, evidence_id=identity.id))
    published = datetime.now(timezone.utc).date().isoformat()
    body = "测试公司公布财务报告，报告披露营业收入同比上升。此处保存完整原文证据，未说明股价未来涨跌，也未说明具体增幅。"
    news = StockNews(market=stock.market, symbol=stock.symbol, source_id=stock.source_id,
        news_time=published, title="新闻财务披露", content=body, source_name="测试来源", content_json={})
    notice = StockNotice(market=stock.market, symbol=stock.symbol, source_id=stock.source_id,
        notice_date=published, title="财务报告公告", content_json={"announcementContent": body} if notice_body else {})
    db.add_all([news, notice])
    db.commit()
    return stock, news, notice


def model_output(*_args, **kwargs):
    docs = json.loads(kwargs["messages"][1]["content"])
    assert kwargs["metadata_json"]["require_real_model"] is True
    response = {"documents": [{"key": doc["key"], "events": [{"event_type": "财务披露", "summary": "营业收入同比上升",
        "evidence_quote": "报告披露营业收入同比上升", "direction": "UNKNOWN"}]} for doc in docs]}
    return SimpleNamespace(id=999, provider_code="TEST_REAL_PROVIDER", instance_code="test", model_code="model-v1",
        status="SUCCESS", response_text=json.dumps(response, ensure_ascii=False))


def test_news_notice_facts_evidence_idempotency_and_versions(database, monkeypatch):
    monkeypatch.setattr(events.ModelHubService, "chat", model_output)
    with database() as db:
        stock, news, _ = documents(db)
        context = {"skill_version": "1.0.0", "skill_execution_run_id": "isolated-test"}
        first = events.govern_stock_documents(db, stock, context=context)
        assert first["status"] == "SUCCESS" and first["completed"] == 2
        facts = db.scalars(select(FoundationFact)).all()
        assert {fact.fact_type for fact in facts} == {"NEWS_EVENT", "NOTICE_EVENT"}
        assert all(fact.status == "PENDING" and fact.properties_json["verification_status"] == "CANDIDATE" for fact in facts)
        for fact in facts:
            value = actions.fact_read(db, fact, full=True)
            assert fact.properties_json["evidence_quote"] in value["evidence"][0]["content"]
            assert fact.properties_json["model_call_log_id"] == 999
        second = events.govern_stock_documents(db, stock, context=context)
        assert second["processed_this_run"] == 0
        assert db.scalar(select(func.count()).select_from(FoundationFact)) == 2
        news.content += "新增原文版本。"
        db.commit()
        third = events.govern_stock_documents(db, stock, context=context)
        assert third["processed_this_run"] == 1
        assert db.scalar(select(func.count()).select_from(FoundationFact)) == 3
        assert len(news.content_json["event_governance_history"]) == 1
        events.govern_stock_documents(db, stock, context={"skill_version": "1.0.1"})
        assert db.scalar(select(func.count()).select_from(FoundationFact)) == 5


def test_missing_body_and_forged_quote_fail_closed(database, monkeypatch):
    def forged(*args, **kwargs):
        result = model_output(*args, **kwargs)
        result.response_text = result.response_text.replace("报告披露营业收入同比上升", "原文不存在的伪造数值与引文")
        return result
    monkeypatch.setattr(events.ModelHubService, "chat", forged)
    monkeypatch.setattr(events, "official_notice_body", lambda _: (_ for _ in ()).throw(ValueError("官方PDF不可用")))
    with database() as db:
        stock, news, notice = documents(db, notice_body=False)
        result = events.govern_stock_documents(db, stock, context={"skill_version": "1.0.0"})
        assert result["status"] == "PARTIAL" and result["completed"] == 0
        assert news.content_json["event_governance"]["status"] == "EXTRACTION_FAILED"
        assert notice.content_json["event_governance"]["status"] == "MISSING_BODY"
        assert db.scalar(select(func.count()).select_from(FoundationFact)) == 0


def partial_job(db):
    request = StockBatchGovernanceRequest(stocks=[{"market": "CN_A", "symbol": "000001"}],
        governance_mode="AI_AGENT_SKILL_GOVERNANCE", export_lakehouse=False, archive_chunks=False, run_graph=False,
        business_types=["NEWS", "NOTICE"])
    result = submit_stock_batch_governance(request, db)
    job = db.get(ScheduledJob, result["job_id"])
    job.status = "COMPLETED"
    run = db.get(PipelineRun, job.pipeline_run_id)
    run.status = "COMPLETED"
    run.output_json = {"result_status": "PARTIAL", "progress": 100, "effective_options": request.model_dump(),
        "stock_results": [{"market": "CN_A", "symbol": "000001", "operations": {
            "NEWS": {"status": "SUCCESS", "total_count": 1}, "NOTICE": {"status": "FAILED", "message": "网络失败"}}}],
        "stage_results": {"BUSINESS_DATA": {"status": "PARTIAL"}}}
    db.commit()
    return job


def test_continuation_is_targeted_idempotent_and_keeps_old_report(database):
    with database() as db:
        job = partial_job(db)
        old = _job_view(db, job)["governance_report"]
        plan = actions.continuation_plan(db, job)
        assert plan["request"]["business_types"] == ["NOTICE"]
        assert plan["request"]["structure_documents"] is True
        next_job = actions.continue_job(db, job, "test-key")
        again = actions.continue_job(db, job, "test-key")
        assert next_job["job_id"] == again["job_id"] != job.id
        assert next_job["governance_batch_id"] != (job.payload_json["request"]["governance_batch_id"])
        old_after = _job_view(db, job)["governance_report"]
        old_after.pop("actions", None)
        old.pop("actions", None)
        from fastapi.encoders import jsonable_encoder
        assert jsonable_encoder(old) == old_after
        assert [row["job_id"] for row in actions.history(db, job)] == [job.id, next_job["job_id"]]
        with pytest.raises(HTTPException) as exc:
            actions.continue_job(db, job, "other-key")
        assert exc.value.status_code == 409


def test_scoped_preview_paging_and_invalid_items(database):
    with database() as db:
        _, news, _ = documents(db)
        job = partial_job(db)
        preview = actions.stage_preview(db, job, "BUSINESS_DATA", "CN_A:000001:NEWS", 1, 0)
        assert preview["items"][0]["id"] == news.id and preview["total"] == 1
        assert actions.stage_preview(db, job, "BUSINESS_DATA", "CN_A:000001:NEWS", 1, 1)["items"] == []
        with pytest.raises(HTTPException) as exc:
            actions.stage_preview(db, job, "BUSINESS_DATA", "HK:00700:NEWS")
        assert exc.value.status_code == 404


def test_skill_drafts_test_review_and_stale_version(database):
    with database() as db:
        job = partial_job(db)
        first = actions.create_optimization_drafts(db, job)
        second = actions.create_optimization_drafts(db, job)
        assert [row["id"] for row in first["drafts"]] == [row["id"] for row in second["drafts"]]
        assert {row["skill_code"] for row in first["drafts"]} == {"STOCK_SOURCE_COLLECTION", events.SKILL_CODE}
        event_draft = next(row for row in first["drafts"] if row["skill_code"] == events.SKILL_CODE)
        base_instructions = event_draft["current_instructions"]
        assert actions.validate_draft(db, job, event_draft["id"])["status"] == "PASSED"
        actions.review_draft(db, job, event_draft["id"], "APPROVE")
        skill = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == events.SKILL_CODE))
        assert skill.version != event_draft["base_skill_version"]
        assert db.scalar(select(func.count()).select_from(ModelSkillRevision).where(ModelSkillRevision.skill_id == skill.id)) >= 2
        assert next(row for row in actions.report_actions(db, job)["drafts"] if row["id"] == event_draft["id"])["current_instructions"] == base_instructions
        collection_draft = next(row for row in first["drafts"] if row["skill_code"] == "STOCK_SOURCE_COLLECTION")
        collection = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == "STOCK_SOURCE_COLLECTION"))
        collection.version = "9.9.9"
        db.commit()
        with pytest.raises(HTTPException) as exc:
            actions.review_draft(db, job, collection_draft["id"], "APPROVE")
        assert exc.value.status_code == 409
        actions.review_draft(db, job, collection_draft["id"], "REJECT")
        assert next(row for row in actions.report_actions(db, job)["drafts"] if row["id"] == collection_draft["id"])["status"] == "REJECTED"


def test_ai_collection_defaults_to_document_structure_but_explicit_legacy_is_preserved():
    options = {"stocks": [{"market": "CN_A", "symbol": "000001"}], "governance_mode": "AI_AGENT_SKILL_GOVERNANCE"}
    assert _normalize_request(StockBatchGovernanceRequest.model_validate(options)).structure_documents is True
    assert _normalize_request(StockBatchGovernanceRequest.model_validate({**options, "structure_documents": False})).structure_documents is False


def test_remote_refresh_keeps_governance_history_and_invalidates_changed_pdf(database, monkeypatch):
    monkeypatch.setattr(events.ModelHubService, "chat", model_output)
    with database() as db:
        stock, news, notice = documents(db)
        events.govern_stock_documents(db, stock, context={"skill_version": "1.0.0"})
        news_state = news.content_json["event_governance"]
        notice_state = notice.content_json["event_governance"]
        notice.content_json = {**notice.content_json, "adjunctUrl": "old.pdf", "governance_full_text": "已缓存旧公告正文",
                               "governance_pdf_extraction": {"binary_sha256": "old"}}
        db.commit()
        collector = StockOnDemandService(db)
        collector._upsert_news(stock.source_id, [NewsRecord(stock.market, stock.symbol, news.news_time, news.title,
            news.content, news.source_name, news.url, {"remote": "updated"})])
        assert news.content_json["event_governance"] == news_state
        assert events._done(news, "1.0.0")
        collector._upsert_notices(stock.source_id, [NoticeRecord(stock.market, stock.symbol, notice.notice_date, notice.title,
            notice.notice_type, notice.url, {"adjunctUrl": "new.pdf", "announcementContent": ""})])
        assert notice.content_json["event_governance"] == notice_state
        assert "governance_full_text" not in notice.content_json and not events._done(notice, "1.0.0")
        assert db.scalar(select(func.count()).select_from(FoundationFact)) == 2


def test_search_excerpt_requires_full_article_and_refresh_preserves_full_body(database, monkeypatch):
    monkeypatch.setattr(events.ModelHubService, "chat", model_output)
    with database() as db:
        stock, news, _ = documents(db)
        news.url = "https://finance.eastmoney.com/a/example.html"
        news.content_json = {"source_method": "EASTMONEY_NEWS_SEARCH"}
        db.commit()
        monkeypatch.setattr(events, "official_news_body", lambda _: (_ for _ in ()).throw(ValueError("原文不可用")))
        first = events.govern_stock_documents(db, stock, context={"skill_version": "1.0.0"})
        assert first["completed"] == 1 and not events._done(news, "1.0.0")
        assert db.scalar(select(func.count()).select_from(FoundationFact).where(FoundationFact.fact_type == "NEWS_EVENT")) == 0
        full_body = news.content + "这段是从新闻原文页补取的完整正文，不能只分析检索摘要。"
        monkeypatch.setattr(events, "official_news_body", lambda _: (full_body, {"source_url": news.url}))
        assert events.govern_stock_documents(db, stock, context={"skill_version": "1.0.0"})["completed"] == 2
        assert events.document_body(news) == full_body and events._done(news, "1.0.0")
        StockOnDemandService(db)._upsert_news(stock.source_id, [NewsRecord(stock.market, stock.symbol, news.news_time,
            news.title, news.content, news.source_name, news.url, {"source_method": "EASTMONEY_NEWS_SEARCH"})])
        assert events.document_body(news) == full_body and events._done(news, "1.0.0")


def test_legacy_excerpt_candidates_are_withdrawn_with_review_history(database, monkeypatch):
    monkeypatch.setattr(events.ModelHubService, "chat", model_output)
    with database() as db:
        stock, news, _ = documents(db)
        events.govern_stock_documents(db, stock, context={"skill_version": "1.0.0"})
        old_state = news.content_json["event_governance"]
        old_id = old_state["fact_ids"][0]
        news.content_json = {**news.content_json, "source_method": "EASTMONEY_NEWS_SEARCH"}
        db.commit()
        monkeypatch.setattr(events, "official_news_body", lambda _: (_ for _ in ()).throw(ValueError("原文不可用")))
        result = events.govern_stock_documents(db, stock, context={"skill_version": "1.0.0", "governance_batch_id": "correction"})
        assert result["retired_excerpt_fact_ids"] == [old_id]
        old_fact = actions.fact_read(db, db.get(FoundationFact, old_id), full=True)
        assert old_fact["status"] == "REJECTED" and len(old_fact["reviews"]) == 1
        assert "检索摘要" in old_fact["reviews"][0]["reason"]
        assert old_fact["evidence"] and old_fact["properties_json"]["evidence_quote"] in old_fact["evidence"][0]["content"]
        assert old_state in news.content_json["event_governance_history"]
        second = events.govern_stock_documents(db, stock, context={"skill_version": "1.0.0"})
        assert not second["retired_excerpt_fact_ids"]
        assert len(actions.fact_read(db, db.get(FoundationFact, old_id), full=True)["reviews"]) == 1


def test_continuation_reserves_capacity_for_failures_and_untouched_documents():
    failed = [SimpleNamespace(content=None, content_json={"event_governance": {"status": "EXTRACTION_FAILED", "attempts": 2}}) for _ in range(12)]
    fresh = [SimpleNamespace(content_json={}) for _ in range(40)]
    selected = events._select_pending([*failed, *fresh], 20)
    assert len(selected) == 20 and sum(row is value for row in selected for value in failed) == 10
    assert events._select_pending(failed, 20) == failed
    assert len(events._select_pending([failed[0], *fresh], 20)) == 20
    deferred = SimpleNamespace(content_json={"event_governance": {"status": "BODY_DOWNLOAD_DEFERRED", "attempts": 1}})
    assert len(events._select_pending([*failed, deferred, *fresh], 20)) == 20


def test_pdf_layout_quote_restores_exact_original_without_changing_numbers():
    body = "公司为该笔贷款提供\n连带责任保证担保，金额为 1.07 亿元。"
    quote = "公司为该笔贷款提供连带责任保证担保"
    restored = events._original_quote(quote, body)
    assert restored in body and "\n" in restored
    assert events._original_quote("金额为1.07亿元。", body) in body
    with pytest.raises(ValueError, match="引文"):
        events._original_quote("金额为1.70亿元。", body)
    with pytest.raises(ValueError, match="引文"):
        events._original_quote("公司为贷款提供有限责任保证担保", body)


def test_article_download_rejects_unregistered_hosts_and_navigation(monkeypatch):
    import httpx
    page = '<nav>站点导航</nav><div id="ContentBody"><p>' + '真实新闻正文。' * 10 + '</p><script>irrelevant()</script></div>'
    client = httpx.Client(transport=httpx.MockTransport(lambda request: httpx.Response(200, text=page)))
    monkeypatch.setattr(events.httpx, "Client", lambda **kwargs: client)
    row = SimpleNamespace(url="http://finance.eastmoney.com/a/test.html")
    body, metadata = events.official_news_body(row)
    assert '真实新闻正文' in body and '导航' not in body and 'irrelevant' not in body
    assert metadata["source_url"].startswith("https://") and len(metadata["binary_sha256"]) == 64
    row.url = "https://localhost/private.html"
    with pytest.raises(ValueError, match="正文来源"):
        events.official_news_body(row)


def test_chunk_preview_excludes_old_versions_and_other_document_domains(database, monkeypatch):
    from app.services.lakehouse import knowledge_document_source_hash
    with database() as db:
        job = partial_job(db)
        kb = KnowledgeBase(kb_code="PREVIEW", kb_name="预览测试", source_tables=["stock_news"])
        db.add(kb); db.flush()
        doc = KnowledgeDocument(knowledge_base_id=kb.id, source_table="stock_news", source_record_id=1,
            market="CN_A", symbol="000001", title="当前文档", content="当前正文")
        db.add(doc); db.flush()
        current_hash = knowledge_document_source_hash(doc)
        for domain, version, text in (("knowledge_document", f"test:{current_hash}", "当前正文"),
                                     ("knowledge_document", "test:stale", "历史正文"),
                                     ("notice", f"test:{current_hash}", "其他领域同号正文")):
            db.add(DocumentChunkVersion(document_key=f"{domain}:{doc.id}", document_id=str(doc.id), chunk_index=0,
                chunk_version=version, content_hash=text, chunk_text=text, parser_version="test"))
        db.commit()
        monkeypatch.setattr("app.api.stock_batch._job_view", lambda *_args, **_kwargs: {"governance_report": {
            "stage_results": {"DOCUMENT_CHUNKS": {"completed_items": [{"code": "CN_A:CHUNKS", "label": "文档切片", "status": "SUCCESS"}]}}}})
        preview = actions.stage_preview(db, job, "DOCUMENT_CHUNKS", "CN_A:CHUNKS")
        assert preview["total"] == 1
        assert [chunk["chunk_text"] for chunk in preview["items"][0]["chunks"]] == ["当前正文"]


def test_preview_marks_nested_nonfinite_as_missing_and_keeps_original(database, monkeypatch):
    with database() as db:
        job = partial_job(db)
        stock = db.scalar(select(StockSymbol).where(StockSymbol.symbol == "000001"))
        f10 = StockF10Cache(market=stock.market, symbol=stock.symbol, section="test_nonfinite", source_id=stock.source_id,
            payload_json={"sections": [{"items": [{"cost": float('nan'), "change": float('inf'), "income": 1.07}]}]})
        db.add(f10); db.commit()
        monkeypatch.setattr("app.api.stock_batch._job_view", lambda *_args, **_kwargs: {"governance_report": {
            "stage_results": {"BUSINESS_DATA": {"completed_items": [{"code": "CN_A:000001:F10", "label": "F10", "status": "SUCCESS"}]}}}})
        preview = actions.stage_preview(db, job, "BUSINESS_DATA", "CN_A:000001:F10")
        json.dumps(preview, default=str, allow_nan=False)
        item = next(row for row in preview["items"] if row["id"] == f10.id)
        assert item["payload_json"]["sections"][0]["items"][0] == {"cost": None, "change": None, "income": 1.07}
        assert preview["report_snapshot"]["invalid_numeric_count"] == 2 and "未修改原始数据" in preview["note"]
        import math
        assert math.isnan(f10.payload_json["sections"][0]["items"][0]["cost"])


def test_governance_action_http_contracts(database):
    with database() as db:
        _, news, _ = documents(db)
        job_id = partial_job(db).id
        news_id = news.id
    app = FastAPI(); app.include_router(stock_batch_router)
    def session():
        with database() as db:
            yield db
    app.dependency_overrides[get_db] = session
    with TestClient(app) as client:
        root = f"{stock_batch_router.prefix}/{job_id}"
        assert client.get(root + "/history").json()["items"][0]["job_id"] == job_id
        preview = client.get(root + "/stage-preview", params={"stage": "BUSINESS_DATA", "item_code": "CN_A:000001:NEWS", "limit": 1})
        assert preview.status_code == 200 and preview.json()["items"][0]["id"] == news_id
        assert client.get(root + "/stage-preview", params={"stage": "BUSINESS_DATA", "item_code": "HK:00700:NEWS"}).status_code == 404
        assert client.post(root + "/continue", json={}).status_code == 422
        assert client.get(root + "/continuation-plan").json()["can_continue"]
        drafts = client.post(root + "/skill-optimization").json()["drafts"]
        draft = next(row for row in drafts if row["skill_code"] == events.SKILL_CODE)
        base = f"{root}/skill-optimization/{draft['id']}"
        assert client.post(base + "/test").json()["status"] == "PASSED"
        assert client.post(base + "/review", json={"decision": "APPROVE"}).status_code == 200
        assert client.post(base + "/review", json={"decision": "APPROVE"}).status_code == 409
        created = client.post(root + "/continue", json={"idempotency_key": "http-test"})
        assert created.status_code == 202 and created.json()["job_id"] != job_id
        assert client.post(root + "/continue", json={"idempotency_key": "http-test"}).json()["job_id"] == created.json()["job_id"]
