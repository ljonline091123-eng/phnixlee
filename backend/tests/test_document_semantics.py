"""Semantic governance contracts use isolated databases and fake model outputs."""
import json
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
from app.models.market_data import StockNews, StockSymbol
from app.services import document_event_governance as governor, document_semantics as semantics, skill_files
from app.services.model_hub import seed_default_skills
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
        created = client.post("/document-governance/analyze", json={"documents": [{"source_table": "stock_news", "record_id": identity}], "idempotency_key": "test"})
        assert created.status_code == 202 and created.json()["job_id"] == 7
