"""Semantic governance contracts use isolated databases and fake model outputs."""
import json
from hashlib import sha256
from datetime import datetime, timezone
from types import SimpleNamespace

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient
from sqlalchemy import select

from app.api.document_governance import router
from app.db.session import get_db
from app.models.ai_hub import ModelSkill
from app.models.foundation import FoundationEntity, FoundationEvidence, FoundationFact, FoundationListing, FoundationSecurity
from app.models.market_data import StockNews, StockNotice, StockSymbol
from app.services import document_event_governance as governor, document_semantics as semantics, skill_files
from app.schemas.document_governance import DocumentReviewRequest
from app.services.document_semantic_review import apply_review, compare_versions, review_status, versions_for
from app.services.model_hub import seed_default_skills
from app.services.document_semantic_planning import decide_plan
from tests.test_stock_batch_governance import _database


@pytest.fixture
def database(monkeypatch, tmp_path):
    monkeypatch.setattr(skill_files, "SKILL_ROOT", tmp_path / "skills")
    temporary, engine, sessions = _database()
    try:
        with sessions() as db:
            seed_default_skills(db); db.commit()
        yield sessions
    finally:
        engine.dispose(); temporary.cleanup()


BODY = """2026年10月9日，测试公司基于税务争议计提预计负债1.07亿元，预计减少本期利润1.07亿元。
若最终实际补缴税款及滞纳金，将产生真实现金流流出，公司资金面将明显承压。
公司正式终止本次定增，这是近四年的第三版定增方案，三版均未落地。
本次募集上限6.62亿元，由关联方江苏融能全额认购；自有资金6190.46万元，自筹资金6亿元，包括股票质押借款不超过2.4亿元、银行借款不高于4亿元。
作者认为这种高杠杆资金运作存在风险，并评论“以套利为目的的定增方案最终搁浅或终止也就成了必然”。"""


def payload():
    reading = {"overview": "文章分析计提负债及第三次终止定增。", "sections": [
        {"topic": "预计负债", "summary": "税务争议引发负债计提。", "evidence_quotes": ["基于税务争议计提预计负债1.07亿元"]},
        {"topic": "定增与评论", "summary": "方案第三次终止，作者质疑高杠杆。", "evidence_quotes": ["这是近四年的第三版定增方案，三版均未落地", "以套利为目的的定增方案最终搁浅或终止也就成了必然"]},
    ], "uncertainties": ["借款是筹资安排而非已发生借款"]}
    extraction = {"participants": [
        {"key": "company", "name": "测试公司", "role": "上市公司", "aliases": [], "evidence_quote": "测试公司基于税务争议计提预计负债"},
        {"key": "subscriber", "name": "江苏融能", "role": "关联认购方", "aliases": [], "evidence_quote": "由关联方江苏融能全额认购"},
    ], "events": [
        {"key": "provision", "event_type": "计提预计负债", "summary": "因税务争议计提1.07亿元，预计等额减少本期利润。",
         "assertion_type": "REPORTED_FACT", "attribution": "公司披露", "event_time_raw": "2026年10月9日", "event_date": "2026-10-09",
         "evidence_quotes": ["基于税务争议计提预计负债1.07亿元，预计减少本期利润1.07亿元"], "participant_keys": ["company"],
         "metrics": [{"name": "预计负债", "raw_value": "1.07", "unit": "亿元", "qualifier": "EXACT", "period": "本期", "evidence_quote": "计提预计负债1.07亿元"}],
         "impacts": [{"dimension": "PROFIT", "direction": "NEGATIVE", "rationale": "预计等额减少本期利润。", "assertion_type": "REPORTED_FACT", "attribution": "公司披露", "evidence_quote": "预计减少本期利润1.07亿元", "uncertainty": "最终财务结果以报告为准"}]},
        {"key": "offering", "event_type": "第三次终止定增", "summary": "近四年第三版方案未落地。",
         "assertion_type": "REPORTED_FACT", "attribution": "新闻转述", "event_time_raw": None, "event_date": None,
         "evidence_quotes": ["这是近四年的第三版定增方案，三版均未落地"], "participant_keys": ["company", "subscriber"],
         "metrics": [{"name": "募集上限", "raw_value": "6.62", "unit": "亿元", "qualifier": "UPPER_BOUND", "period": "本次方案", "evidence_quote": "本次募集上限6.62亿元"}],
         "impacts": [{"dimension": "FINANCING", "direction": "NEGATIVE", "rationale": "融资方案终止。", "assertion_type": "REPORTED_FACT", "attribution": "新闻转述", "evidence_quote": "公司正式终止本次定增", "uncertainty": "不代表后续不能再融资"}]},
        {"key": "opinion", "event_type": "作者评价定增动机", "summary": "作者把方案描述为以套利为目的。",
         "assertion_type": "AUTHOR_OPINION", "attribution": "新闻作者", "event_time_raw": None, "event_date": None,
         "evidence_quotes": ["以套利为目的的定增方案最终搁浅或终止也就成了必然"], "participant_keys": ["company"], "metrics": [],
         "impacts": [{"dimension": "GOVERNANCE", "direction": "NEGATIVE", "rationale": "作者质疑方案动机。", "assertion_type": "AUTHOR_OPINION", "attribution": "新闻作者", "evidence_quote": "以套利为目的的定增方案最终搁浅或终止也就成了必然", "uncertainty": "不是监管认定或司法结论"}]},
    ], "relations": [
        {"source_key": "subscriber", "target_key": "offering", "predicate": "SUBSCRIBES", "explanation": "江苏融能拟全额认购", "assertion_type": "REPORTED_FACT", "attribution": "新闻转述", "evidence_quote": "由关联方江苏融能全额认购"},
        {"source_key": "company", "target_key": "provision", "predicate": "INVOLVES", "explanation": "测试公司计提预计负债", "assertion_type": "REPORTED_FACT", "attribution": "公司披露", "evidence_quote": "测试公司基于税务争议计提预计负债"},
    ], "uncertainties": ["未交叉核验作者观点"]}
    return reading, extraction


def test_semantic_contract_preserves_opinion_metrics_and_impact():
    reading, extraction = payload()
    result = semantics.validate_analysis(semantics.Reading.model_validate(reading), semantics.Analysis.model_validate(extraction), BODY)
    assert result["events"][0]["direction"] == "NEGATIVE"
    assert result["events"][0]["metrics"][0]["normalized_value"] == "107000000.00"
    opinion = next(row for row in result["events"] if row["assertion_type"] == "AUTHOR_OPINION")
    assert opinion["attribution"] == "新闻作者" and opinion["impacts"][0]["uncertainty"].startswith("不是监管")
    with pytest.raises(ValueError, match="数值或单位"):
        extraction["events"][0]["metrics"][0]["raw_value"] = "1.70"
        semantics.validate_analysis(semantics.Reading.model_validate(reading), semantics.Analysis.model_validate(extraction), BODY)


def test_role_suffix_is_removed_only_when_surface_name_and_quote_support_it():
    reading, extraction = payload()
    extraction["participants"].append({"key": "author", "name": "测试公司作者", "role": "作者", "aliases": [],
        "evidence_quote": "测试公司基于税务争议计提预计负债"})
    value = semantics.validate_analysis(semantics.Reading.model_validate(reading), semantics.Analysis.model_validate(extraction), BODY)
    author = next(row for row in value["participants"] if row["key"] == "author")
    assert author["name"] == "测试公司" and author["model_name"] == "测试公司作者"
    assert value["quality_warnings"]


def test_chinese_basis_point_expression_is_preserved_and_normalized():
    value = semantics.normalize_metric({"name": "每日滞纳金比例", "raw_value": "万分之五", "unit": "%",
        "qualifier": "EXACT", "period": "自滞纳之日起",
        "evidence_quote": "自滞纳之日起按照每日万分之五缴纳滞纳金"}, "自滞纳之日起按照每日万分之五缴纳滞纳金")
    assert value["normalized_value"] == "0.0500" and value["normalized_unit"] == "%"
    assert value["raw_value"] == "万分之五"


@pytest.mark.parametrize("body,raw,accepted", [
    ("营业收入同比下降2.48%，以报告为准", "2.48", True),
    ("营业收入同比下降2.48%，以报告为准", "-2.48", False),
    ("经营活动现金流量净额为-2.65亿元", "-2.65", True),
    ("经营活动现金流量净额为-2.65亿元", "2.65", False),
    ("经营活动现金流量净额为+2.65亿元", "2.65", False),
])
def test_metric_keeps_original_sign_without_inventing_direction(body, raw, accepted):
    metric = {"name": "观测值", "raw_value": raw, "unit": "%" if "同比" in body else "亿元",
              "period": "本期", "evidence_quote": body}
    if accepted:
        result = semantics.normalize_metric(metric, body)
        assert result["raw_value"] == raw
    else:
        with pytest.raises(ValueError, match="数值或单位"):
            semantics.normalize_metric(metric, body)


def setup_document(db):
    stock = db.scalar(select(StockSymbol).where(StockSymbol.symbol == "000001"))
    company = FoundationEntity(name="测试公司", entity_type="COMPANY", jurisdiction="CN")
    db.add(company); db.flush()
    security = FoundationSecurity(entity_id=company.id)
    proof = FoundationEvidence(entity_id=company.id, source_name="test", source_key="identity", title="身份", content="identity", content_hash="a", fingerprint="a", version=1, available_at=datetime.now(timezone.utc))
    db.add_all([security, proof]); db.flush()
    db.add(FoundationListing(security_id=security.id, stock_symbol_id=stock.id, market=stock.market, symbol=stock.symbol, name=stock.name, evidence_id=proof.id))
    news = StockNews(market=stock.market, symbol=stock.symbol, source_id=stock.source_id, news_time="2026-10-09", title="语义新闻", content=BODY, source_name="测试来源", content_json={})
    db.add(news); db.commit()
    return stock, news


def fake_model(*args, **kwargs):
    reading, extraction = payload()
    phase = kwargs["metadata_json"]["phase"]
    return SimpleNamespace(id=100 if phase == "SEMANTIC_READING" else 101, provider_code="REAL", instance_code="real", model_code="test-model", status="SUCCESS", response_text=json.dumps(reading if phase == "SEMANTIC_READING" else extraction, ensure_ascii=False))


@pytest.mark.parametrize("repair_succeeds", [True, False])
def test_subject_evidence_repair_is_bounded_and_keeps_original_quote_gate(database, monkeypatch, repair_succeeds):
    calls = []
    def response(*args, **kwargs):
        reading, extraction = payload()
        phase = kwargs["metadata_json"]["phase"]
        calls.append(phase)
        for event in extraction["events"]:
            event.update(subject_scope="COMPANY", subject_name="测试公司")
            if phase == "STRUCTURED_EXTRACTION_REPAIR" and repair_succeeds:
                event["evidence_quotes"].append("测试公司基于税务争议计提预计负债1.07亿元")
        if phase == "STRUCTURED_EXTRACTION_REPAIR":
            received = json.loads(kwargs["messages"][-1]["content"])
            assert len(received["validation_feedback"]) == 3
            assert received["body"] == BODY
            assert len(received["previous_invalid_analysis"]["events"]) == 3
        return SimpleNamespace(id=len(calls), status="SUCCESS", provider_code="REAL", instance_code="isolated",
            model_code="test", response_text=json.dumps(reading if phase == "SEMANTIC_READING" else extraction, ensure_ascii=False))
    monkeypatch.setattr(semantics.ModelHubService, "chat", response)
    document = {"key": "isolated-subject-check", "title": "隔离证据测试", "body": BODY, "market": "CN_A",
                "symbol": "000001", "stock_name": "测试公司", "state": {"evidence_id": "isolated"},
                "governance_plan": {"plan_id": "isolated", "target_scopes": ["COMPANY"]}}
    with database() as db:
        if repair_succeeds:
            result = semantics.semantic_analysis(db, document, context={}, model_instance=None, instructions="")
            assert all("测试公司" in "\n".join(event["evidence_quotes"]) for event in result["events"])
            assert len(result["model_calls"]) == 3
        else:
            with pytest.raises(ValueError, match="主体证据修正仍未通过") as failed:
                semantics.semantic_analysis(db, document, context={}, model_instance=None, instructions="")
            assert len(failed.value.semantic_calls) == 3
    assert calls == ["SEMANTIC_READING", "STRUCTURED_EXTRACTION", "STRUCTURED_EXTRACTION_REPAIR"]


def confirm_test_plan(db, row):
    """Confirm an isolated fixture; production confirmations are never automated."""
    plan = {"plan_id": f"fixture:{row.__tablename__}:{row.id}", "revision": 1, "plan_version": 1,
        "status": "PROPOSED", "content_hash": sha256(governor.document_body(row).encode()).hexdigest(),
        "overview": "隔离测试方案", "primary_type": "GENERAL_EVIDENCE", "secondary_types": [],
        "target_scopes": ["COMPANY"], "recommended_skill_codes": ["GENERIC_EVIDENCE_FACT_GOVERNOR"],
        "expected_knowledge": [], "uncertainties": [], "capability_gaps": []}
    row.content_json = {**(row.content_json or {}), "semantic_governance_plan": plan}
    db.commit()
    return decide_plan(db, row, primary_type=plan["primary_type"], secondary_types=[],
        skill_codes=plan["recommended_skill_codes"], target_scopes=plan["target_scopes"],
        reviewer="隔离测试审核员", reason="测试流程门禁", decision="CONFIRM",
        expected_plan_id=plan["plan_id"], expected_revision=1)


def test_semantic_skill_stores_events_relations_and_supersedes_old(database, monkeypatch):
    monkeypatch.setattr(semantics.ModelHubService, "chat", fake_model)
    with database() as db:
        stock, news = setup_document(db)
        old_evidence = FoundationEvidence(entity_id=None, source_name="old", source_key="old", title="旧", content=BODY, content_hash="b", fingerprint="b", version=1, available_at=datetime.now(timezone.utc))
        db.add(old_evidence); db.flush()
        old = FoundationFact(fact_type="NEWS_EVENT", title="旧摘要", subject_entity_id=db.scalar(select(FoundationSecurity.entity_id)), properties_json={"skill_code": governor.SKILL_CODE}, status="PENDING")
        db.add(old); db.flush(); news.content_json = {"event_governance": {"fact_ids": [old.id], "status": "ANALYZED"}}; db.commit()
        result = governor.govern_stock_documents(db, stock, context={"skill_version": "9.0.0"}, semantic=True, document_targets=[{"source_table": "stock_news", "record_id": news.id}])
        assert result["completed"] == 1 and len(result["fact_ids"]) == 5
        assert db.get(FoundationFact, old.id).status == "REJECTED"
        props = [db.get(FoundationFact, identity).properties_json for identity in result["fact_ids"]]
        assert sum(p["entry_kind"] == "EVENT" for p in props) == 3
        assert sum(p["entry_kind"] == "RELATION" for p in props) == 2
        state = news.content_json["event_governance"]
        assert state["semantic_schema_version"] == "SEMANTIC_V2" and state["model_calls"][0]["id"] == 100


def test_document_semantic_api_lists_detail_and_enqueues(database, monkeypatch):
    with database() as db:
        _, news = setup_document(db); identity = news.id
    app = FastAPI(); app.include_router(router)
    def session():
        with database() as db: yield db
    app.dependency_overrides[get_db] = session
    monkeypatch.setattr("app.api.stock_batch.submit_stock_batch_governance", lambda request, db: {"job_id": 7, "pipeline_run_id": 8,
        "status": "PENDING", "job_status": "PENDING", "stock_count": 1, "worker_required": True,
        "worker_task_type": "stock_batch_governance", "worker_command": "worker", "progress": 0,
        "effective_options": {}, "governance_mode": request.governance_mode})
    with TestClient(app) as client:
        listing = client.get("/document-governance/documents", params={"market": "CN_A", "symbol": "000001"})
        assert listing.status_code == 200 and listing.json()["items"][0]["record_id"] == identity
        detail = client.get(f"/document-governance/documents/stock_news/{identity}")
        assert detail.status_code == 200 and BODY in detail.json()["body"]
        unconfirmed = client.post("/document-governance/analyze", json={"documents": [{"source_table": "stock_news", "record_id": identity}], "idempotency_key": "unconfirmed"})
        assert unconfirmed.status_code == 409
        with database() as db:
            confirm_test_plan(db, db.get(StockNews, identity))
        created = client.post("/document-governance/analyze", json={"documents": [{"source_table": "stock_news", "record_id": identity}], "idempotency_key": "test"})
        assert created.status_code == 202 and created.json()["job_id"] == 7
        bypass = client.post("/document-governance/analyze", json={"documents": [{"source_table": "stock_news", "record_id": identity}],
            "idempotency_key": "bypass", "semantic_feedback": "绕过审核轨迹的反馈"})
        assert bypass.status_code == 422


def test_scope_preview_filters_news_and_notice_and_caps_each_stock(database):
    with database() as db:
        stock, first_news = setup_document(db)
        db.add_all([
            StockNews(market=stock.market, symbol=stock.symbol, source_id=stock.source_id,
                      news_time="2026-10-08", title="第二篇语义新闻", content=BODY,
                      source_name="测试来源", content_json={}),
            StockNotice(market=stock.market, symbol=stock.symbol, source_id=stock.source_id,
                        notice_date="2026-10-07", title="一份公司公告", url="https://example.test/a.pdf",
                        content_json={"content": BODY}),
            StockNews(market="HK", symbol="00700", source_id=stock.source_id,
                      news_time="2026-10-06", title="港股语义新闻", content=BODY,
                      source_name="测试来源", content_json={}),
        ])
        db.commit()
    app = FastAPI(); app.include_router(router)
    def session():
        with database() as db: yield db
    app.dependency_overrides[get_db] = session
    with TestClient(app) as client:
        all_scope = client.post("/document-governance/scope-preview", json={
            "stocks": [{"market": "CN_A", "symbol": "000001"}, {"market": "HK", "symbol": "00700"}],
            "kind": "ALL", "disclosure_days": 730, "per_stock_limit": 1,
        })
        assert all_scope.status_code == 200
        body = all_scope.json()
        assert body["stock_count"] == 2 and body["planned_count"] == 2
        cn = next(item for item in body["stocks"] if item["symbol"] == "000001")
        assert cn["total"] == 3 and cn["planned"] == 1

        news_scope = client.post("/document-governance/scope-preview", json={
            "stocks": [{"market": "CN_A", "symbol": "000001"}],
            "kind": "NEWS", "disclosure_days": 730, "per_stock_limit": 20,
        })
        assert news_scope.status_code == 200 and news_scope.json()["stocks"][0]["total"] == 2

        notice_scope = client.post("/document-governance/scope-preview", json={
            "stocks": [{"market": "CN_A", "symbol": "000001"}],
            "kind": "NOTICE", "disclosure_days": 730, "per_stock_limit": 20,
        })
        assert notice_scope.status_code == 200 and notice_scope.json()["stocks"][0]["total"] == 1


def test_scope_analyze_queues_multiple_stocks_with_types_and_bounded_options(database, monkeypatch):
    with database() as db:
        stock, _ = setup_document(db)
        db.add(StockNews(market="HK", symbol="00700", source_id=stock.source_id,
                         news_time="2026-10-06", title="港股批量新闻", content=BODY,
                         source_name="测试来源", content_json={}))
        db.commit()
        for row in db.scalars(select(StockNews)):
            confirm_test_plan(db, row)
    app = FastAPI(); app.include_router(router)
    def session():
        with database() as db: yield db
    app.dependency_overrides[get_db] = session
    captured = []
    def submit(request, db):
        captured.append(request)
        return {"job_id": 77, "pipeline_run_id": 88, "status": "PENDING", "job_status": "PENDING",
                "stock_count": len(request.stocks), "worker_required": True,
                "worker_task_type": "stock_batch_governance", "worker_command": "worker", "progress": 0,
                "effective_options": {}, "governance_mode": request.governance_mode}
    monkeypatch.setattr("app.api.stock_batch.submit_stock_batch_governance", submit)
    with TestClient(app) as client:
        response = client.post("/document-governance/analyze-scope", json={
            "stocks": [{"market": "CN_A", "symbol": "000001"}, {"market": "HK", "symbol": "00700"}],
            "kind": "NEWS", "disclosure_days": 365, "per_stock_limit": 3,
            "idempotency_key": "multi-stock-news-1",
        })
        assert response.status_code == 202 and response.json()["stock_count"] == 2
    request = captured[0]
    assert request.structure_documents is True and request.semantic_analysis is True
    assert all(item.source_table == "stock_news" for item in request.document_targets)
    assert request.structured_document_limit == 2
    assert request.interactive_plan_required and len(request.document_targets) == 2
    assert request.collect_business_data is False and request.run_graph is False
    assert {(item.market, item.symbol) for item in request.stocks} == {("CN_A", "000001"), ("HK", "00700")}


def test_explicit_document_scope_accepts_more_than_ten_documents(database, monkeypatch):
    with database() as db:
        stock, _ = setup_document(db)
        rows = [StockNews(market=stock.market, symbol=stock.symbol, source_id=stock.source_id,
                          news_time=f"2026-09-{index + 1:02d}", title=f"批量新闻{index}", content=BODY,
                          source_name="测试来源", content_json={}) for index in range(11)]
        db.add_all(rows); db.commit(); ids = [row.id for row in rows]
        for row in rows:
            confirm_test_plan(db, row)
    app = FastAPI(); app.include_router(router)
    def session():
        with database() as db: yield db
    app.dependency_overrides[get_db] = session
    monkeypatch.setattr("app.api.stock_batch.submit_stock_batch_governance", lambda request, db: {
        "job_id": 91, "pipeline_run_id": 92, "status": "PENDING", "job_status": "PENDING", "stock_count": 1,
        "worker_required": True, "worker_task_type": "stock_batch_governance", "worker_command": "worker",
        "progress": 0, "effective_options": {}, "governance_mode": request.governance_mode,
    })
    with TestClient(app) as client:
        response = client.post("/document-governance/analyze", json={
            "documents": [{"source_table": "stock_news", "record_id": identity} for identity in ids],
            "idempotency_key": "eleven-documents",
        })
        assert response.status_code == 202


def test_human_review_correction_creates_versioned_candidate_and_keeps_gate(database, monkeypatch):
    monkeypatch.setattr(semantics.ModelHubService, "chat", fake_model)
    with database() as db:
        stock, news = setup_document(db)
        result = governor.govern_stock_documents(db, stock, context={"skill_version": "9.0.0"}, semantic=True,
            document_targets=[{"source_table": "stock_news", "record_id": news.id}])
        db.refresh(news)
        original = next(item for item in news.content_json["event_governance"]["semantic_analysis"]["events"] if item["key"] == "provision")
        old_fact_id = original["fact_id"]
        version = len(versions_for(news)) - 1
        requested = apply_review(db, news, DocumentReviewRequest(version_index=version, action="REQUEST_CORRECTION",
            target_type="EVENT", target_key="provision", reason="利润影响摘要需要写清预计性质", reviewer="审核员甲"), BODY)
        assert requested["resulting_status"] == "NEEDS_CORRECTION"
        assert review_status(db, "stock_news", news.id, requested["version_key"])["overall"] == "NEEDS_CORRECTION"
        apply_review(db, news, DocumentReviewRequest(version_index=version, action="COMMENT",
            target_type="EVENT", target_key="provision", reason="补充批注不应清除待修正状态", reviewer="审核员甲"), BODY)
        assert review_status(db, "stock_news", news.id, requested["version_key"])["overall"] == "NEEDS_CORRECTION"
        with pytest.raises(ValueError, match="不能确认整篇"):
            apply_review(db, news, DocumentReviewRequest(version_index=version, action="CONFIRM",
                target_type="DOCUMENT", reason="尚未修正时不得确认", reviewer="审核员甲"), BODY)

        corrected = apply_review(db, news, DocumentReviewRequest(version_index=version, action="MODIFY",
            target_type="EVENT", target_key="provision", field="summary",
            value="公司因税务争议计提预计负债1.07亿元，预计减少而非已经实现本期利润1.07亿元。",
            reason="区分预计影响与已实现亏损", reviewer="审核员甲"), BODY)
        assert corrected["resulting_status"] == "CORRECTED_CANDIDATE"
        db.refresh(news)
        current = news.content_json["event_governance"]
        revised = next(item for item in current["semantic_analysis"]["events"] if item["key"] == "provision")
        assert "预计减少而非已经实现" in revised["summary"]
        assert revised["fact_id"] in corrected["corrected_fact_ids"]
        assert db.get(FoundationFact, old_fact_id).status == "REJECTED"
        assert db.get(FoundationFact, revised["fact_id"]).status == "PENDING"

        final_version = len(versions_for(news)) - 1
        confirmed = apply_review(db, news, DocumentReviewRequest(version_index=final_version, action="CONFIRM",
            target_type="DOCUMENT", reason="已逐项对照原文，结构抽取正确", reviewer="审核员乙"), BODY)
        assert confirmed["review_status"]["overall"] == "HUMAN_CONFIRMED"
        assert db.get(FoundationFact, revised["fact_id"]).status == "PENDING", "人工结构确认不能冒充多来源验证"


def test_version_compare_reports_semantic_field_changes(database, monkeypatch):
    monkeypatch.setattr(semantics.ModelHubService, "chat", fake_model)
    with database() as db:
        stock, news = setup_document(db)
        governor.govern_stock_documents(db, stock, context={"skill_version": "9.0.0"}, semantic=True,
            document_targets=[{"source_table": "stock_news", "record_id": news.id}])
        db.refresh(news)
        initial = len(versions_for(news)) - 1
        unchanged = compare_versions(news, initial, initial)
        assert unchanged["changed"] is False
        apply_review(db, news, DocumentReviewRequest(version_index=initial, action="MODIFY",
            target_type="EVENT", target_key="provision", field="summary",
            value="税务争议导致公司计提预计负债，预计减少本期利润1.07亿元，并非已经确认的实际亏损。",
            reason="明确预计影响边界", reviewer="审核员甲"), BODY)
        db.refresh(news)
        comparison = compare_versions(news, initial, initial + 1)
        assert comparison["changed"] is True
        changed_event = next(item for item in comparison["events"]["modified"] if item["key"] == "provision")
        summary = next(item for item in changed_event["changes"] if item["field"] == "summary")
        assert "并非已经确认" in summary["to"] and "计提1.07亿元" in summary["from"]


def test_review_api_enforces_history_and_fact_gate_without_two_round_limit(database, monkeypatch):
    monkeypatch.setattr(semantics.ModelHubService, "chat", fake_model)
    with database() as db:
        stock, news = setup_document(db)
        governor.govern_stock_documents(db, stock, context={"skill_version": "9.0.0"}, semantic=True,
            document_targets=[{"source_table": "stock_news", "record_id": news.id}])
        identity = news.id
    app = FastAPI(); app.include_router(router)
    def session():
        with database() as db: yield db
    app.dependency_overrides[get_db] = session
    queued = []
    def submit(request, db):
        queued.append(request)
        return {"job_id": len(queued), "pipeline_run_id": len(queued), "status": "PENDING", "job_status": "PENDING",
                "stock_count": 1, "worker_required": True, "worker_task_type": "stock_batch_governance",
                "worker_command": "worker", "progress": 0, "effective_options": {}, "governance_mode": request.governance_mode}
    monkeypatch.setattr("app.api.stock_batch.submit_stock_batch_governance", submit)
    base = f"/document-governance/documents/stock_news/{identity}"
    with TestClient(app) as client:
        reviews = client.get(base + "/reviews", params={"version_index": 0})
        assert reviews.status_code == 200 and reviews.json()["review_count"] == 0
        missing = client.post(base + "/reviews", json={"version_index": 0, "action": "CONFIRM", "target_type": "EVENT",
            "target_key": "missing", "reason": "测试不存在目标", "reviewer": "接口审核员"})
        assert missing.status_code == 422
        corrected = client.post(base + "/reviews", json={"version_index": 0, "action": "MODIFY", "target_type": "EVENT",
            "target_key": "provision", "field": "summary", "value": "预计负债计提预计减少本期利润1.07亿元，最终影响仍待财报确认。",
            "reason": "补充预计与最终值边界", "reviewer": "接口审核员"})
        assert corrected.status_code == 200 and corrected.json()["resulting_status"] == "CORRECTED_CANDIDATE"
        historical = client.post(base + "/reviews", json={"version_index": 0, "action": "MODIFY", "target_type": "EVENT",
            "target_key": "provision", "field": "summary", "value": "禁止覆盖历史版本。", "reason": "测试历史保护", "reviewer": "接口审核员"})
        assert historical.status_code == 422
        historical_reanalysis = client.post(base + "/reviews", json={"version_index": 0, "action": "REANALYZE", "target_type": "DOCUMENT",
            "reason": "历史版本禁止重分析", "reviewer": "接口审核员"})
        assert historical_reanalysis.status_code == 422
        comparison = client.get(base + "/compare", params={"from_index": 0, "to_index": 1})
        assert comparison.status_code == 200 and comparison.json()["events"]["counts"]["modified"] >= 1
        detail = client.get(base).json()
        current = detail["selected_version"]
        assert detail["review_history"] and detail["review_history"][0]["version_index"] == 1
        fact_id = detail["analysis"]["events"][0]["fact_id"]
        confirmed = client.post(base + "/reviews", json={"version_index": current, "action": "CONFIRM", "target_type": "DOCUMENT",
            "reason": "逐字段对照原文无误", "reviewer": "接口审核员"})
        assert confirmed.status_code == 200 and confirmed.json()["review_status"]["overall"] == "HUMAN_CONFIRMED"
        with database() as db:
            assert db.get(FoundationFact, fact_id).status == "PENDING"
        for index in range(2):
            response = client.post(base + "/reviews", json={"version_index": current, "action": "REANALYZE", "target_type": "DOCUMENT",
                "reason": f"第{index + 1}轮定向补充现金流条件", "reviewer": "接口审核员"})
            assert response.status_code == 200 and response.json()["job"]["job_id"] == index + 1
        third = client.post(base + "/reviews", json={"version_index": current, "action": "REANALYZE", "target_type": "DOCUMENT",
            "reason": "第三轮仍需补充原文支持", "reviewer": "接口审核员"})
        assert third.status_code == 200 and third.json()["job"]["job_id"] == 3
        assert [item.semantic_feedback for item in queued] == ["第1轮定向补充现金流条件", "第2轮定向补充现金流条件", "第三轮仍需补充原文支持"]


def test_semantic_reanalysis_receives_bounded_human_feedback(database, monkeypatch):
    calls = []
    def capture(*args, **kwargs):
        calls.append(kwargs)
        return fake_model(*args, **kwargs)
    monkeypatch.setattr(semantics.ModelHubService, "chat", capture)
    with database() as db:
        stock, news = setup_document(db)
        result = governor.govern_stock_documents(db, stock, context={"skill_version": "9.0.0"}, semantic=True,
            semantic_feedback="补充说明现金流影响是有条件的，保留其他已核验字段。",
            document_targets=[{"source_table": "stock_news", "record_id": news.id}])
        assert result["completed"] == 1
        user_payload = json.loads(calls[0]["messages"][1]["content"])
        assert user_payload["human_review_feedback"].startswith("补充说明现金流")
        assert "reading_instruction" in user_payload
        assert "review_instruction" not in user_payload and "previous_analysis" not in user_payload
        extraction_payload = json.loads(calls[1]["messages"][1]["content"])
        assert "review_instruction" in extraction_payload


def test_reading_contract_bounds_table_citations_without_rewriting_source():
    quotes = [f"原文表格第{index}行连续证据文字" for index in range(10)]
    payload = json.dumps({
        "overview": "完整阅读市场统计表格。",
        "sections": [{"topic": "榜单", "summary": "逐行阅读榜单。", "evidence_quotes": quotes}],
        "uncertainties": [],
    }, ensure_ascii=False)
    reading = semantics.load_contract(payload, semantics.Reading)
    assert reading.sections[0].evidence_quotes == [*quotes[:7], quotes[-1]]
    assert any("超过8条" in item for item in reading.uncertainties)
