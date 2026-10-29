"""Evidence provenance, signed item scores and conclusion publication regression."""
from copy import deepcopy
from datetime import datetime, timezone
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, select
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.db.base import Base
from app.db.session import get_db
from app.main import app
from app.models.pipeline import ScheduledJob
from app.models.selection import SelectionCandidate, SelectionRun
from app.schemas.selection import SelectionBackfillRequest, SelectionReviewRequest
from app.services.selection import (
    _normalize_business_composition_output,
    _validate_business_value_chain_output,
    execute_selection_data_backfill,
    queue_selection_data_backfill,
    review_candidate,
)
from app.services.selection_evidence import (
    build_evidence_sections, conclusion_publication, section_weights, typed_references, validate_assessments,
)
from tests.selection_samples import valid_scorecard


def dimension(code="NEWS", ids=None, assessments=None):
    return {"dimension": code, "status": "SUPPORTED", "direction": "POSITIVE", "summary": "测试分析",
            "evidence_document_ids": ids or [], "evidence_fact_ids": [], "evidence_relation_ids": [],
            "evidence_assessments": assessments or []}


def assessment(ref, score=40, direction="POSITIVE", kind="DOCUMENT"):
    return {"reference_type": kind, "reference_id": str(ref), "direction": direction, "score": score,
            "rationale": "依据实际披露的影响评价"}


def evidence():
    return {"documents": [{"id": n, "source_table": source, "title": f"测试{n}",
            "content": "公司披露已签署订单及交付安排，但收入影响仍需以履约和审计情况为准。",
            "fetched_at": f"2026-10-{n + 10:02d}T08:00:00+00:00"}
            for n, source in [(1, "stock_news"), (2, "stock_notice"), (3, "stock_financial_report")]],
            "facts": [{"id": "market:1", "fact_type": "MARKET_OBSERVATION", "observed_at": "2026-10-15T10:00:00Z",
                       "properties": {"close_price": 12.3, "volume": 500}}], "graph_paths": [{"id": 8, "predicate": "OWNS"}]}


def analysis():
    return {"model_output_status": "SUCCESS", "analysis_validation_status": "PASSED",
            "model": {"reasoning": "根据可追溯来源进行分析", "confidence": .7}, "scoring": valid_scorecard()}


def test_typed_cards_reject_cross_domain_documents_facts_and_relations():
    payload = evidence()
    for code, expected in [("NEWS", [1]), ("NOTICE", [2]), ("FINANCIAL", [3]), ("PRICE_VOLUME", []),
                           ("FUND_FLOW", []), ("COMPANY", []), ("GRAPH", [])]:
        row = dimension(code, [1, 2, 3, 1])
        row.update(evidence_fact_ids=["market:1"], evidence_relation_ids=[8])
        refs = typed_references(row, payload)
        assert refs["evidence_document_ids"] == expected
        assert refs["evidence_fact_ids"] == (["market:1"] if code == "PRICE_VOLUME" else [])
        assert refs["evidence_relation_ids"] == ([8] if code == "GRAPH" else [])


def test_related_and_metadata_only_documents_cannot_fill_current_stock_cards():
    payload = evidence()
    payload["documents"][0]["relation_scope"] = "RELATED_SECURITY"
    payload["documents"][1]["content_state"] = "METADATA_ONLY"
    assert not typed_references(dimension(ids=[1]), payload)["evidence_document_ids"]
    assert not typed_references(dimension("NOTICE", [2]), payload)["evidence_document_ids"]


def test_company_card_does_not_adopt_f10_cash_flow():
    payload = {"documents": [{"id": 1, "source_table": "stock_f10_cache", "title": "资金流向"},
                             {"id": 2, "source_table": "stock_f10_cache", "title": "股东资料"}]}
    assert typed_references(dimension("COMPANY", [1, 2]), payload)["evidence_document_ids"] == [2]


def v5_item():
    return {"evidence_json": {
        "documents": [
            {"id": 1, "source_table": "stock_notice", "content_state": "FULLTEXT", "content": "公司披露技术研发及产品验证进展。"},
            {"id": 2, "source_table": "stock_news", "content_state": "FULLTEXT", "content": "关联公司宣布扩产。",
             "relation_scope": "RELATED_SECURITY", "related_via_fact_ids": ["relation-1"]},
        ],
        "facts": [
            {"id": "business-1", "fact_type": "BUSINESS_PROFILE", "status": "SOURCE",
             "properties": {"main_business": "测试设备研发与销售"}},
            {"id": "business-2", "fact_type": "BUSINESS_COMPOSITION", "status": "SOURCE",
             "properties": {"report_date": "2026-06-30", "segments": [{"category": "按产品", "items": [{
                 "name": "测试设备", "revenue": 100.0, "revenue_ratio": 0.6,
                 "profit": 30.0, "profit_ratio": 0.7, "gross_margin": 0.3,
             }]}]}},
            {"id": "relation-1", "fact_type": "SUPPLIES_TO", "status": "ACCEPTED",
             "properties": {"product": "测试设备"}},
        ],
    }}


def v5_output():
    return {
        "business_quality": {
            "status": "SUPPORTED", "main_business_summary": "主营测试设备研发与销售。",
            "products_services": ["测试设备"],
            "revenue_mix": [{"name": "测试设备", "report_date": "2026-06-30", "revenue": 100.0,
                             "revenue_ratio": 0.6, "profit": 30.0, "profit_ratio": 0.7,
                             "gross_margin": 0.3, "evidence_fact_id": "business-2"}],
            "technology_level": "UNKNOWN", "technology_assessment": "现有资料不足以评价技术领先性。",
            "competitive_moat": "主营构成可核验，壁垒仍待原文支持。", "commercialization": "收入已形成。",
            "market_position": "未取得权威市场地位数据。",
            "market_share": {"verification_status": "MISSING", "value": None,
                             "explanation": "缺少数值、口径和来源。", "evidence_document_ids": [], "evidence_fact_ids": []},
            "direction": "NEUTRAL", "impact_score": 0, "confidence": 0.6,
            "evidence_document_ids": [], "evidence_fact_ids": ["business-1", "business-2"],
            "risks": [], "watch_conditions": [],
        },
        "value_chain_analysis": {"status": "INSUFFICIENT", "summary": "暂无可核验传导。",
                                 "impacts": [], "risks": [], "watch_conditions": []},
    }


def test_v5_business_values_are_tied_to_business_composition_fact():
    _validate_business_value_chain_output(v5_output(), v5_item(), required=True)
    changed = v5_output()
    changed["business_quality"]["revenue_mix"][0]["revenue_ratio"] = 0.9
    with pytest.raises(ValueError, match="与来源事实不一致"):
        _validate_business_value_chain_output(changed, v5_item(), required=True)


def test_v5_business_display_rounding_restores_exact_source_values():
    item = v5_item()
    source = item["evidence_json"]["facts"][1]["properties"]["segments"][0]["items"][0]
    source.update(revenue=624516904.27, revenue_ratio=0.55362, gross_margin=0.282552)
    changed = v5_output()
    segment = changed["business_quality"]["revenue_mix"][0]
    segment.update(revenue=625000000.0, revenue_ratio=0.5536, gross_margin=0.2826)

    _normalize_business_composition_output(changed, item)
    _validate_business_value_chain_output(changed, item, required=True)

    assert segment["revenue"] == 624516904.27
    assert segment["revenue_ratio"] == 0.55362
    assert segment["gross_margin"] == 0.282552
    assert {row["field"] for row in changed["evidence_normalizations"]} == {
        "business_quality.revenue_mix.revenue",
        "business_quality.revenue_mix.revenue_ratio",
        "business_quality.revenue_mix.gross_margin",
    }
    assert all(row["reason"] == "DISPLAY_ROUNDING" for row in changed["evidence_normalizations"])
    assert len(changed["evidence_normalization_notes"]) == 3
    assert "evidence_validation_warnings" not in changed


def test_v5_business_large_numeric_error_is_not_normalized_or_accepted():
    changed = v5_output()
    changed["business_quality"]["revenue_mix"][0]["revenue_ratio"] = 0.9

    _normalize_business_composition_output(changed, v5_item())

    assert changed["business_quality"]["revenue_mix"][0]["revenue_ratio"] == 0.9
    assert "evidence_normalizations" not in changed
    with pytest.raises(ValueError, match="与来源事实不一致"):
        _validate_business_value_chain_output(changed, v5_item(), required=True)


def test_v5_business_unknown_source_is_not_normalized_or_accepted():
    changed = v5_output()
    changed["business_quality"]["revenue_mix"][0]["evidence_fact_id"] = "missing-fact"

    _normalize_business_composition_output(changed, v5_item())

    assert "evidence_normalizations" not in changed
    with pytest.raises(ValueError, match="未在引用的BUSINESS_COMPOSITION事实中找到"):
        _validate_business_value_chain_output(changed, v5_item(), required=True)


def test_v5_market_share_without_source_is_rejected():
    changed = v5_output()
    changed["business_quality"]["market_share"] = {
        "verification_status": "SOURCE_VERIFIED", "value": 35, "unit": "%", "scope": "国内市场",
        "as_of": "2026", "source": "模型常识", "explanation": "无来源", "evidence_document_ids": [], "evidence_fact_ids": [],
    }
    with pytest.raises(ValueError, match="必须引用"):
        _validate_business_value_chain_output(changed, v5_item(), required=True)


def test_v5_verified_transmission_requires_accepted_relation_and_related_event():
    changed = v5_output()
    changed["value_chain_analysis"] = {"status": "SUPPORTED", "summary": "关联扩产可能传导至设备需求。", "risks": [], "watch_conditions": [], "impacts": [{
        "related_company": "关联公司", "related_market": "CN_A", "related_symbol": "000002",
        "relationship_type": "CUSTOMER", "relation_status": "VERIFIED", "relation_fact_ids": ["relation-1"],
        "current_security_document_ids": [], "related_security_document_ids": [2],
        "event_summary": "关联公司宣布扩产", "transmission_mechanism": "扩产可能增加测试设备需求。",
        "direction": "POSITIVE", "impact_score": 30, "horizon": "MEDIUM", "confidence": 0.7,
        "assumptions": ["扩产按计划执行"], "invalidation_conditions": ["资本开支取消"],
    }]}
    _validate_business_value_chain_output(changed, v5_item(), required=True)
    invalid = deepcopy(v5_item())
    invalid["evidence_json"]["facts"][-1]["status"] = "PENDING"
    with pytest.raises(ValueError, match="ACCEPTED"):
        _validate_business_value_chain_output(changed, invalid, required=True)


def test_v5_candidate_relation_cannot_receive_directional_score():
    changed = v5_output()
    changed["value_chain_analysis"] = {"status": "SUPPORTED", "summary": "候选线索", "risks": [], "watch_conditions": [], "impacts": [{
        "related_company": "候选公司", "relationship_type": "SAME_THEME", "relation_status": "CANDIDATE",
        "relation_fact_ids": [], "current_security_document_ids": [], "related_security_document_ids": [],
        "event_summary": "仅有题材相似线索", "transmission_mechanism": "尚未证实业务传导。",
        "direction": "POSITIVE", "impact_score": 20, "horizon": "UNKNOWN", "confidence": 0.2,
        "assumptions": [], "invalidation_conditions": [],
    }]}
    with pytest.raises(ValueError, match="候选或未验证关系"):
        _validate_business_value_chain_output(changed, v5_item(), required=True)


@pytest.mark.parametrize("score,direction", [(40, "POSITIVE"), (-12, "NEGATIVE"), (0, "NEUTRAL"), (0, "MIXED"), (None, "UNKNOWN")])
def test_valid_signed_assessments(score, direction):
    row = dimension(ids=[1], assessments=[assessment(1, score, direction)])
    validate_assessments({"dimensions": [row]}, evidence(), required=True)


@pytest.mark.parametrize("score,direction", [(-1, "POSITIVE"), (0, "POSITIVE"), (4, "NEGATIVE"), (101, "POSITIVE"),
    (float("nan"), "NEUTRAL"), (True, "POSITIVE"), (0, "UNKNOWN"), (None, "NEUTRAL")])
def test_invalid_sign_or_nonfinite_score_is_rejected(score, direction):
    with pytest.raises(ValueError):
        validate_assessments({"dimensions": [dimension(ids=[1], assessments=[assessment(1, score, direction)])]}, evidence(), required=True)


def test_missing_duplicate_or_foreign_item_score_is_rejected():
    for rows in ([], [assessment(1), assessment(1)], [assessment(2)]):
        with pytest.raises(ValueError):
            validate_assessments({"dimensions": [dimension(ids=[1], assessments=rows)]}, evidence(), required=True)


def test_scores_use_deduplicated_evidence_and_conserve_weight_budget():
    payload = evidence()
    payload["documents"].append({"id": 4, "source_table": "stock_news", "title": "风险提示"})
    row = dimension(ids=[1, 4, 1], assessments=[assessment(1, 60), assessment(4, -20, "NEGATIVE")])
    result = build_evidence_sections({**analysis(), "model": {"dimensions": [row]}}, payload)[0]
    assert len(result["items"]) == 2 and result["raw_score"] == 20
    assert result["weight"] == 6.25 and result["weighted_score"] == 1.25
    assert sum(section_weights(valid_scorecard()).values()) == 100


def test_time_order_uses_acquisition_and_never_invents_a_missing_time():
    payload = evidence()
    payload["documents"] += [{"id": 4, "source_table": "stock_news", "title": "无采集时间", "published_at": "2099-01-01"},
        {"id": 5, "source_table": "stock_news", "title": "已采集", "source_metadata": {"source_fetched_at": "2026-10-25T00:00:00Z"}}]
    row = dimension(ids=[4, 1, 5])
    before = deepcopy(payload)
    result = build_evidence_sections({"model": {"dimensions": [row]}}, payload)[0]
    assert [item["reference_id"] for item in result["items"]] == [5, 1, 4]
    assert result["items"][-1]["acquired_at"] is None
    assert result["weighted_score"] is None and result["raw_score"] is None
    assert payload == before


def test_contaminated_historical_summary_is_withdrawn_without_rewriting_history():
    value = {"model": {"dimensions": [dimension("FINANCIAL", [1, 3])]}}
    before = deepcopy(value)
    card = build_evidence_sections(value, evidence())[0]
    assert card["status"] == "INSUFFICIENT" and card["direction"] == "UNKNOWN"
    assert "混用" in card["summary"] and card["weighted_score"] is None
    assert value == before


def test_section_direction_follows_signed_scores_instead_of_stale_model_label():
    row = dimension(ids=[1], assessments=[assessment(1, -35, "NEGATIVE")])
    result = build_evidence_sections({**analysis(), "model": {"dimensions": [row]}}, evidence())[0]
    assert result["direction"] == "NEGATIVE" and result["weighted_score"] < 0


@pytest.mark.parametrize("mutation,state", [
    ({"model_output_status": "NOT_RUN"}, "DATA_INSUFFICIENT"),
    ({"model_output_status": "FAILED"}, "MODEL_FAILED"),
    ({"analysis_validation_status": "PARTIAL"}, "CONCLUSION_FAILED"),
    ({"model": {"reasoning": "", "confidence": .7}}, "CONCLUSION_FAILED"),
    ({"model": {"reasoning": "分析", "confidence": float("nan")}}, "CONCLUSION_FAILED"),
    ({"scoring": {"trusted_score": "bad", "dimensions": []}}, "SCORE_FAILED"),
    ({"scoring": {"trusted_score": 60, "dimensions": valid_scorecard()["dimensions"][:7]}}, "SCORE_FAILED"),
    ({}, "READY_REVIEW"),
])
def test_publication_distinguishes_transport_conclusion_and_radar(mutation, state):
    result = conclusion_publication({**analysis(), **mutation})
    assert result["status"] == state
    assert result["conclusion_available"] == (state == "READY_REVIEW")


@pytest.fixture
def isolated():
    engine = create_engine("sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool)
    Base.metadata.create_all(engine)
    factory = sessionmaker(bind=engine, expire_on_commit=False)
    with factory() as db, patch("app.db.session.SessionLocal", factory), patch("app.jobs.governance_runner.wake_governance_runner"):
        yield db
    engine.dispose()


def test_summary_details_and_follow_gate_agree_for_invalid_conclusions(isolated):
    db = isolated
    run = SelectionRun(market="CN_A", status="PARTIAL", analysis_mode="MODEL_PARTIAL", data_mode="REAL",
                       completed_at=datetime.now(timezone.utc))
    db.add(run)
    db.flush()
    for index, mutation in enumerate([{"analysis_validation_status": "PARTIAL"}, {"scoring": {}}, {"model_output_status": "FAILED"}, {}]):
        db.add(SelectionCandidate(run_id=run.id, market="CN_A", symbol=str(index), analysis_json={**analysis(), **mutation}, evidence_json=evidence()))
    db.commit()
    app.dependency_overrides[get_db] = lambda: db
    try:
        client = TestClient(app)
        value = client.get(f"/api/v1/selection/runs/{run.id}").json()
        assert value["criteria_json"]["result_counts"]["passed_count"] == 1
        assert value["criteria_json"]["result_counts"]["failed_count"] == 3
        for candidate in value["candidates"]:
            if candidate["symbol"] == "3":
                assert candidate["analysis_json"]["publication"]["status"] == "READY_REVIEW"
            else:
                assert not candidate["analysis_json"]["conclusion_available"]
                with pytest.raises(ValueError):
                    review_candidate(db, candidate["id"], SelectionReviewRequest(decision="APPROVED", target_price=15))
        assert not db.dirty
    finally:
        app.dependency_overrides.pop(get_db, None)


def test_score_repair_is_allowed_but_invalid_conclusion_cannot_be_rescored(isolated):
    db = isolated
    run = SelectionRun(market="CN_A", status="PARTIAL", data_mode="REAL", analysis_mode="MODEL_PARTIAL")
    db.add(run)
    db.flush()
    candidate = SelectionCandidate(run_id=run.id, market="CN_A", symbol="SCORE_TEST",
        analysis_json={**analysis(), "scoring": {}, "data_gate": {"status": "READY"}}, evidence_json=evidence())
    db.add(candidate)
    db.commit()
    app.dependency_overrides[get_db] = lambda: db
    try:
        client = TestClient(app)
        with patch("app.api.selection.resolve_profile", return_value=object()), \
                patch("app.api.selection.serialize_profile", return_value={}), \
                patch("app.api.selection.score_candidate", return_value=valid_scorecard()) as calculate, \
                patch("app.api.selection.refresh_candidate_evaluations", return_value=[]):
            response = client.post(f"/api/v1/selection/candidates/{candidate.id}/scorecard")
            assert response.status_code == 200 and calculate.call_count == 1
            response = client.post(f"/api/v1/selection/runs/{run.id}/scorecards")
            assert response.status_code == 200 and calculate.call_count == 2
            candidate.analysis_json = {**candidate.analysis_json, "analysis_validation_status": "PARTIAL"}
            db.commit()
            response = client.post(f"/api/v1/selection/candidates/{candidate.id}/scorecard")
            assert response.status_code == 409 and calculate.call_count == 2
    finally:
        app.dependency_overrides.pop(get_db, None)


def test_scorecard_read_rejects_stale_scores_on_invalid_conclusion(isolated):
    db = isolated
    run = SelectionRun(market="CN_A", status="PARTIAL", data_mode="REAL", analysis_mode="MODEL_PARTIAL")
    db.add(run)
    db.flush()
    candidate = SelectionCandidate(run_id=run.id, market="CN_A", symbol="STALE_SCORE",
        analysis_json={**analysis(), "model": {"reasoning": "", "confidence": .7}}, evidence_json=evidence())
    db.add(candidate)
    db.commit()
    before = deepcopy(candidate.analysis_json)
    app.dependency_overrides[get_db] = lambda: db
    try:
        response = TestClient(app).get(f"/api/v1/selection/candidates/{candidate.id}/scorecard")
        assert response.status_code == 409
        assert "完整性校验未通过" in response.json()["detail"]
        assert candidate.analysis_json == before and not db.dirty
    finally:
        app.dependency_overrides.pop(get_db, None)


def test_backfill_acknowledges_then_reports_running_before_network_and_keeps_missing(isolated):
    db = isolated
    run = SelectionRun(market="CN_A", status="PARTIAL", analysis_mode="MODEL_PARTIAL", data_mode="REAL")
    db.add(run)
    db.flush()
    candidate = SelectionCandidate(run_id=run.id, market="CN_A", symbol="000001", evidence_json={})
    db.add(candidate)
    db.commit()
    queue_selection_data_backfill(db, run.id, SelectionBackfillRequest(candidate_ids=[candidate.id], dimensions=["FUND_FLOW"], rerun_analysis=False))
    assert run.criteria_json["backfill_progress"]["status"] == "QUEUED"
    job = db.scalar(select(ScheduledJob).where(ScheduledJob.task_type == "selection_data_backfill"))

    def fetch(*args):
        db.refresh(run)
        progress = run.criteria_json["backfill_progress"]
        assert progress["status"] == "RUNNING" and progress["current_operation"] == "F10_FUND_FLOW"
        assert "资金流向" in progress["message"]
        return {"status": "SUCCESS", "sections": ["profile", "research"]}

    with patch("app.services.stock_batch._run_f10_refresh", side_effect=fetch):
        result = execute_selection_data_backfill(job.payload_json)
    assert result["status"] == "PARTIAL"
    assert result["results"][0]["operations"]["F10_FUND_FLOW"]["status"] == "MISSING"
    db.refresh(run)
    assert "仍有 1 只" in run.criteria_json["backfill_progress"]["message"]
    assert candidate.evidence_json == {}
