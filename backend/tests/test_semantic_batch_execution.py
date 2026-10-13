"""Unbounded selection, batched governance and persisted document progress."""
import json
from types import SimpleNamespace

import httpx
import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient
from jsonschema import Draft202012Validator
from sqlalchemy import select

from app.api.document_governance import router
from app.db.session import get_db
from app.models.market_data import StockNews, StockNotice, StockSymbol
from app.models.pipeline import PipelineRun
from app.schemas.stock_batch import StockBatchGovernanceRequest
from app.services import document_event_governance as governor, document_semantics as semantics
from app.services.stock_batch import _save_progress
from app.services import stock_batch
from app.api.stock_batch import submit_stock_batch_governance, get_stock_batch_governance_job
from app.jobs.worker import JobWorker
from app.models.pipeline import ScheduledJob
from app.services.resource_hub import seed_default_agents, seed_default_data_assets, seed_default_knowledge_bases
from app.services.model_hub import seed_default_skills
from app.models.ai_hub import ModelSkill, ModelSkillRevision
from tests.test_document_semantics import BODY, database, fake_model, setup_document, confirm_test_plan


def api_client(database):
    app = FastAPI()
    app.include_router(router)
    def session():
        with database() as db:
            yield db
    app.dependency_overrides[get_db] = session
    return TestClient(app)


def test_all_selection_and_analyze_cover_more_than_fifty_documents(database, monkeypatch):
    with database() as db:
        stock, _ = setup_document(db)
        rows = [StockNews(market=stock.market, symbol=stock.symbol, source_id=stock.source_id,
            news_time="2026-10-01", title=f"选中新闻{index}", content=BODY, content_json={}) for index in range(65)]
        db.add_all(rows)
        db.add(StockNews(market="HK", symbol="00700", source_id=stock.source_id,
            news_time="2026-10-01", title="选中但属于其他股票", content=BODY, content_json={}))
        db.add(StockNotice(market=stock.market, symbol=stock.symbol, source_id=stock.source_id,
            notice_date="2026-10-01", title="选中公告", content_json={"content": BODY}))
        db.commit()
        expected = {row.id for row in rows}
        for row in rows:
            confirm_test_plan(db, row)
    captured = []
    def submit(request, db):
        captured.append(request)
        return {"job_id": 1}
    monkeypatch.setattr("app.api.stock_batch.submit_stock_batch_governance", submit)
    with api_client(database) as client:
        selected = client.get("/document-governance/documents/selection", params={
            "market": "CN_A", "symbol": "000001", "kind": "NEWS", "q": "选中"})
        assert selected.status_code == 200
        documents = selected.json()["documents"]
        assert len(documents) == 65 and {item["record_id"] for item in documents} == expected
        assert client.post("/document-governance/analyze", json={
            "documents": documents, "idempotency_key": "all-pages"}).status_code == 202
    assert len(captured[0].document_targets) == 65


def test_scope_all_has_no_stock_or_document_cap_and_preview_is_bounded(database, monkeypatch):
    with database() as db:
        stock, _ = setup_document(db)
        for index in range(35):
            db.add(StockSymbol(market="CN_A", symbol=f"{100000 + index:06}", name=f"测试{index}",
                source_id=stock.source_id, exchange="SZSE"))
        db.add_all([StockNews(market=stock.market, symbol=stock.symbol, source_id=stock.source_id,
            news_time="2026-10-01", title=f"范围新闻{index}", content=BODY, content_json={}) for index in range(105)])
        db.commit()
    stocks = [{"market": "CN_A", "symbol": f"{100000 + i:06}"} for i in range(35)]
    stocks.append({"market": "CN_A", "symbol": "000001"})
    captured = []
    monkeypatch.setattr("app.api.stock_batch.submit_stock_batch_governance", lambda request, db: captured.append(request) or {"job_id": 1})
    with api_client(database) as client:
        scope = {"stocks": stocks, "kind": "NEWS"}
        preview = client.post("/document-governance/scope-preview", json=scope)
        assert preview.status_code == 200
        assert preview.json()["planned_count"] == 106 and preview.json()["stock_count"] == 36
        assert len(preview.json()["planned_documents"]) == 50 and preview.json()["preview_truncated"]
        assert client.post("/document-governance/plan-scope", json={**scope,
            "idempotency_key": "all-stocks"}).status_code == 202
    assert captured[0].structured_document_limit is None and len(captured[0].stocks) == 36
    assert captured[0].semantic_plan_only
    with database() as db:
        for row in db.scalars(select(StockNews)):
            confirm_test_plan(db, row)
    with api_client(database) as client:
        assert client.post("/document-governance/analyze-scope", json={"stocks": stocks, "kind": "NEWS",
            "idempotency_key": "confirmed-stocks"}).status_code == 202
    assert len(captured[1].stocks) == 1 and len(captured[1].document_targets) == 106
    assert StockBatchGovernanceRequest(stocks=stocks, structured_document_limit=500).structured_document_limit == 500


def test_governance_processes_all_selected_documents_and_reports_each_result(database, monkeypatch):
    monkeypatch.setattr(semantics.ModelHubService, "chat", fake_model)
    with database() as db:
        stock, _ = setup_document(db)
        rows = [StockNews(market=stock.market, symbol=stock.symbol, source_id=stock.source_id,
            news_time="2026-10-01", title=f"全文治理{index}", content=BODY if index < 60 else "正文不足",
            content_json={}) for index in range(61)]
        db.add_all(rows); db.commit()
        targets = [{"source_table": "stock_news", "record_id": row.id} for row in rows]
        updates = []
        callback = lambda row, phase, status, error: updates.append((row.id, phase, status))
        result = governor.govern_stock_documents(db, stock, context={"skill_version": "9.0.0"},
            limit=1, semantic=True, document_targets=targets, progress_callback=callback)
        assert result["total"] == 61 and result["processed_this_run"] == 61
        assert result["completed"] == 60 and result["status"] == "PARTIAL"
        final = [item for item in updates if item[1] == "FINISHED"]
        assert len(final) == 61 and len({item[0] for item in final}) == 61
        assert sum(item[2] == "ANALYZED" for item in final) == 60
        assert any(item[1] == "SEMANTIC_READING" for item in updates)
        assert any(item[1] == "STRUCTURED_EXTRACTION" for item in updates)
        assert governor.govern_stock_documents(db, stock, context={"skill_version": "9.0.0"},
            semantic=True, document_targets=targets)["processed_this_run"] == 1


def test_missing_company_mapping_reports_every_document_and_progress(database, monkeypatch):
    monkeypatch.setattr(semantics.ModelHubService, "chat", fake_model)
    with database() as db:
        existing, _ = setup_document(db)
        stock = StockSymbol(market="CN_A", symbol="100001", name="未映射公司",
            source_id=existing.source_id, exchange="SZSE")
        db.add(stock)
        rows = [StockNews(market=stock.market, symbol=stock.symbol, source_id=stock.source_id,
            news_time="2026-10-01", title=f"未映射新闻{index}", content=BODY, content_json={}) for index in range(7)]
        db.add_all(rows); db.commit()
        updates = []
        result = governor.govern_stock_documents(db, stock, context={"skill_version": "9.0.0"},
            semantic=True, document_types=["NEWS"],
            progress_callback=lambda row, phase, status, error: updates.append((row.id, phase, status)))
        assert result["total"] == result["processed_this_run"] == result["failed"] == 7
        assert result["completed"] == 0 and not result["fact_ids"]
        finished = [update for update in updates if update[1] == "FINISHED"]
        assert len(result["documents"]) == len(finished) == 7
        assert {doc["source_record_id"] for doc in result["documents"]} == {row.id for row in rows}
        assert all(doc["status"] == "EXTRACTION_FAILED" and not doc["current_version_complete"] for doc in result["documents"])
        assert all(update[1:] == ("FINISHED", "EXTRACTION_FAILED") for update in finished)


def test_all_announcements_attempt_body_download_without_four_document_cap(database, monkeypatch):
    downloads = []
    monkeypatch.setattr(governor, "official_notice_body", lambda row: downloads.append(row.id) or (BODY, {}))
    def model(self, **kwargs):
        documents = json.loads(kwargs["messages"][-1]["content"])
        return SimpleNamespace(id=1, status="SUCCESS", provider_code="REAL", instance_code="test", model_code="test",
            response_text=json.dumps({"documents": [{"key": doc["key"], "events": []} for doc in documents]}))
    monkeypatch.setattr(governor.ModelHubService, "chat", model)
    with database() as db:
        stock, _ = setup_document(db)
        rows = [StockNotice(market=stock.market, symbol=stock.symbol, source_id=stock.source_id,
            notice_date="2026-10-01", title=f"待补正文{index}", url="https://example.test/a.pdf", content_json={}) for index in range(7)]
        db.add_all(rows); db.commit()
        result = governor.govern_stock_documents(db, stock, context={"skill_version": "9.0.0"}, document_types=["NOTICE"])
        assert len(downloads) == 7 and result["completed"] == 7 and result["status"] == "SUCCESS"


def test_progress_subphase_persists_when_percentage_does_not_change(database):
    with database() as db:
        run = PipelineRun(pipeline_type="TEST", trigger_type="TEST", status="RUNNING")
        db.add(run); db.commit()
        identity = run.id
        output = {"document_progress": {"processed": 0, "phase": "BODY_FETCH"}}
        _save_progress(db, identity, output, stage="STRUCTURED_EVENTS", progress=5)
        output["document_progress"]["phase"] = "SEMANTIC_READING"
        _save_progress(db, identity, output, stage="STRUCTURED_EVENTS", progress=5)
    with database() as db:
        assert db.get(PipelineRun, identity).output_json["document_progress"]["phase"] == "SEMANTIC_READING"


def test_real_agent_skill_contract_queue_to_document_result(database, monkeypatch):
    """Exercise production Skill contracts too, rather than only the extraction function."""
    monkeypatch.setattr(stock_batch, "SessionLocal", database)
    def model(self, **kwargs):
        if kwargs["metadata_json"].get("phase"):
            return fake_model(self, **kwargs)
        return SimpleNamespace(id=None, provider_code="REAL", instance_code="test", model_code="test",
            status="SUCCESS", response_text="治理已通过证据门禁，候选待审核")
    monkeypatch.setattr(semantics.ModelHubService, "chat", model)
    with database() as db:
        seed_default_data_assets(db)
        seed_default_knowledge_bases(db)
        seed_default_agents(db)
        stock, news = setup_document(db)
        extra = [StockNews(market=stock.market, symbol=stock.symbol, source_id=stock.source_id,
            news_time="2026-10-01", title=f"真实契约验证{index}", content=BODY, content_json={}) for index in range(64)]
        db.add_all(extra); db.commit()
        documents = [{"source_table": "stock_news", "record_id": row.id} for row in [news, *extra]]
        job = submit_stock_batch_governance(StockBatchGovernanceRequest(
            stocks=[{"market": stock.market, "symbol": stock.symbol}],
            governance_mode="AI_AGENT_SKILL_GOVERNANCE", structure_documents=True,
            document_targets=documents,
            collect_business_data=False, export_lakehouse=False, archive_chunks=False, run_graph=False,
            business_types=[], idempotency_key="agent-contract"), db)
        identity = job["job_id"]
    worker = JobWorker(task_types=("stock_batch_governance",), session_factory=database,
        task_executor=lambda task, payload: stock_batch.execute_stock_batch_governance(payload))
    assert worker.run_once()
    with database() as db:
        job = db.get(ScheduledJob, identity)
        assert job.status == "COMPLETED", job.error_message
        report = get_stock_batch_governance_job(identity, db)
        assert report["document_progress"]["processed"] == report["document_progress"]["succeeded"] == 65
        assert report["stage_results"]["STRUCTURED_EVENTS"]["stock_runs"][0]["failed"] == 0
        assert report["result_status"] == "COMPLETED"


def test_legacy_skill_limits_migrate_with_historical_revision_preserved(database):
    with database() as db:
        setup_document(db)
        seed_default_skills(db)
        codes = {
            "NEWS_NOTICE_EVENT_GOVERNOR": ("document_targets", [{"source_table": "stock_news", "record_id": i + 1} for i in range(65)]),
            "SECURITY_BOARD_IDENTITY_GOVERNOR": ("stocks", [{"market": "CN_A", "symbol": str(i)} for i in range(36)]),
            "STOCK_DATA_QUALITY_GATE": ("stock_results", [{} for _ in range(36)]),
            "LAKEHOUSE_KNOWLEDGE_PUBLISHER": ("symbols", [str(i) for i in range(36)]),
        }
        for code, (field, values) in codes.items():
            skill = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == code))
            original_version = skill.version
            original_revision = db.scalar(select(ModelSkillRevision).where(
                ModelSkillRevision.skill_id == skill.id, ModelSkillRevision.version == original_version))
            original_snapshot = json.loads(json.dumps(original_revision.governance_json))
            old_contract = json.loads(json.dumps(skill.input_contract_json))
            old_contract["properties"][field]["maxItems"] = 10 if field == "document_targets" else 30
            skill.input_contract_json = old_contract
            skill.config_json = {**skill.config_json, "managed_contract_version": "LEGACY_LIMITED"}
            db.commit()
            seed_default_skills(db); db.commit()
            assert skill.version != original_version
            assert original_revision.governance_json == original_snapshot
            payload = {"governance_batch_id": "large", field: values}
            if field == "document_targets":
                payload.update(market="CN_A", symbol="000001")
            if field == "symbols":
                payload["market"] = "CN_A"
            Draft202012Validator(skill.input_contract_json).validate(payload)
            Draft202012Validator(skill.config_json["function_spec"]["parameters"]).validate(payload)


def test_selection_matches_literal_keyword_on_metadata_pages(database):
    with database() as db:
        stock, _ = setup_document(db)
        row = StockNews(market=stock.market, symbol=stock.symbol, source_id=stock.source_id,
            news_time="2026-10-01", title="利润增长10%_目标", content=BODY, content_json={})
        db.add(row); db.commit()
        identity = row.id
    with api_client(database) as client:
        params = {"market": "CN_A", "symbol": "000001", "kind": "NEWS", "q": "%_"}
        page = client.get("/document-governance/documents", params=params).json()
        selection = client.get("/document-governance/documents/selection", params=params).json()
        assert page["total"] == selection["total"] == 1
        assert page["items"][0]["record_id"] == selection["documents"][0]["record_id"] == identity


def test_legacy_news_search_excerpt_downloads_full_publisher_text(database, monkeypatch):
    monkeypatch.setattr(semantics.ModelHubService, "chat", fake_model)
    downloads = []
    monkeypatch.setattr(governor, "official_news_body", lambda row: downloads.append(row.id) or (BODY, {
        "source_url": row.url, "table_layout_version": 1,
    }))
    with database() as db:
        stock, news = setup_document(db)
        news.content = "检索片段，不能作为全文分析依据。" * 4
        news.content_json = {"date": "2026-10-01", "code": "000001", "content": news.content,
            "mediaName": "新闻来源", "url": "http://stock.eastmoney.com/a/test.html"}
        news.url = news.content_json["url"]
        db.commit()
        assert governor._search_excerpt(news)
        result = governor.govern_stock_documents(db, stock, context={"skill_version": "9.0.0"}, semantic=True,
            document_targets=[{"source_table": "stock_news", "record_id": news.id}])
        assert downloads == [news.id] and result["completed"] == 1
        assert news.content != BODY and news.content_json["governance_full_text"] == BODY


@pytest.mark.parametrize("location,allowed", [
    ("http://finance.eastmoney.com/a/article.html", True),
    ("http://127.0.0.1/a/article.html", False),
    ("https://unregistered.example/a/article.html", False),
    ("https://finance.eastmoney.com:9000/a/article.html", False),
])
def test_publisher_redirect_is_followed_only_to_registered_article(monkeypatch, location, allowed):
    requests = []
    def respond(request):
        requests.append(str(request.url))
        if request.url.host == "stock.eastmoney.com":
            return httpx.Response(302, headers={"Location": location})
        return httpx.Response(200, text=f'<div id="ContentBody"><p>{BODY}</p></div>')
    client_class = httpx.Client
    monkeypatch.setattr(governor.httpx, "Client", lambda **kwargs: client_class(
        **kwargs, transport=httpx.MockTransport(respond)))
    row = StockNews(url="http://stock.eastmoney.com/a/article.html")
    if allowed:
        body, provenance = governor.official_news_body(row)
        assert body == BODY
        assert requests == ["https://stock.eastmoney.com/a/article.html", "https://finance.eastmoney.com/a/article.html"]
        assert provenance["source_url"] == requests[-1] and len(provenance["redirect_chain"]) == 1
    else:
        with pytest.raises(ValueError, match="已登记正文来源"):
            governor.official_news_body(row)
        assert len(requests) == 1
