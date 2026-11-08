"""Evidence provenance, signed item scores and conclusion publication regression."""
from copy import deepcopy
from datetime import datetime, timezone
import json
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, select
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.db.base import Base
from app.db.session import get_db
from app.main import app
from app.models.ai_hub import KnowledgeBase, KnowledgeGraph, ModelCallLog, ModelSkill, SkillOptimizationDraft
from app.models.foundation import FoundationFact
from app.models.pipeline import ScheduledJob
from app.models.selection import SelectionCandidate, SelectionResearchFeedback, SelectionRun
from app.schemas.selection import (
    SelectionBackfillRequest,
    SelectionHypothesisDirectionRequest,
    SelectionHypothesisResearchRequest,
    SelectionResearchReviewRequest,
    SelectionReviewRequest,
)
from app.services.selection import (
    _normalize_business_composition_output,
    _sanitize_investment_thesis_evidence,
    _validate_business_value_chain_output,
    _validate_investment_thesis_output,
    execute_selection_data_backfill,
    _apply_research_confidence_policy,
    _build_hypothesis_research_plan,
    _hypothesis_research_model_context,
    _research_source_outcome,
    _research_questions,
    _resolve_research_feedback,
    execute_selection_hypothesis_research,
    prepare_selection_hypothesis_review,
    queue_selection_hypothesis_research,
    queue_selection_data_backfill,
    record_selection_hypothesis_direction,
    record_selection_research_review,
    review_candidate,
    selection_research_learning_summary,
)
from app.services.selection_evidence import (
    build_evidence_sections, conclusion_publication, evidence_coverage_summary, section_weights, typed_references,
    validate_assessments,
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
            {"id": "market-1", "fact_type": "MARKET_OBSERVATION", "status": "SOURCE",
             "properties": {"period_summary": {"return_20d_pct": 5.0, "bar_count": 245}}},
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


def v6_output():
    return {
        "investment_thesis": {
            "status": "SUPPORTED",
            "primary_business": "测试设备研发与销售",
            "earning_engine_summary": "测试设备形成主要收入，利润取决于订单交付和毛利率。",
            "profit_quality_summary": "已有主营收入构成，现金流持续性仍需跟踪。",
            "industry_chain_summary": "位于测试设备环节，关联扩产是需求线索而非已确认收入。",
            "valuation_summary": "缺少同口径估值比较，不据此给出方向。",
            "institution_view_summary": "机构覆盖不足，仅作为待补充预期。",
            "market_timing_summary": "交易数据不足，不能替代基本面结论。",
            "overall_logic": "主营收入已形成，但成长假设仍取决于客户扩产、订单兑现和盈利质量。",
            "classifications": [{
                "role": "PRIMARY_INDUSTRY", "name": "专用测试设备", "code": "TEST_EQUIPMENT",
                "relevance_score": 0.9, "rationale": "与主营产品和收入构成直接一致。",
                "evidence_document_ids": [], "evidence_fact_ids": ["business-1", "business-2"],
                "evidence_relation_ids": [],
            }],
            "hypotheses": [{
                "title": "客户扩产带动设备需求", "thesis": "若客户扩产按计划执行，设备订单可能增加。",
                "status": "SUPPORTED", "direction": "POSITIVE", "horizon": "MEDIUM", "confidence": 0.65,
                "supporting_document_ids": [2], "supporting_fact_ids": ["relation-1"],
                "supporting_relation_ids": [], "counter_document_ids": [], "counter_fact_ids": [],
                "counter_relation_ids": [], "invalidation_conditions": ["客户取消资本开支"],
                "watch_conditions": ["新增订单及交付进度"],
            }],
            "confidence": 0.68, "evidence_document_ids": [1, 2],
            "evidence_fact_ids": ["business-1", "business-2", "relation-1"], "evidence_relation_ids": [],
        },
    }


def test_v6_holistic_thesis_accepts_business_grounded_logic():
    _validate_investment_thesis_output(v6_output(), v5_item(), required=True)


def test_v6_requires_holistic_thesis_for_new_analysis_contract():
    with pytest.raises(ValueError, match="V6分析必须返回"):
        _validate_investment_thesis_output({}, v5_item(), required=True)


def test_v6_core_theme_cannot_use_unrelated_relation_as_business_evidence():
    changed = v6_output()
    changed["investment_thesis"]["classifications"][0].update(
        role="CORE_THEME", evidence_fact_ids=["relation-1"], evidence_document_ids=[],
    )
    with pytest.raises(ValueError, match="必须引用主营构成事实"):
        _validate_investment_thesis_output(changed, v5_item(), required=True)


def test_v6_low_relevance_tag_cannot_be_promoted_to_core_theme():
    changed = v6_output()
    changed["investment_thesis"]["classifications"][0].update(role="CORE_THEME", relevance_score=0.49)
    with pytest.raises(ValueError, match="相关度低于50%"):
        _validate_investment_thesis_output(changed, v5_item(), required=True)


def test_v6_supported_hypothesis_requires_invalidation_conditions():
    changed = v6_output()
    changed["investment_thesis"]["hypotheses"][0]["invalidation_conditions"] = []
    with pytest.raises(ValueError, match="必须明确逻辑失效条件"):
        _validate_investment_thesis_output(changed, v5_item(), required=True)


def test_v6_rejects_evidence_outside_current_snapshot():
    changed = v6_output()
    changed["investment_thesis"]["hypotheses"][0]["supporting_document_ids"] = [999]
    with pytest.raises(ValueError, match="本次上下文之外的文档"):
        _validate_investment_thesis_output(changed, v5_item(), required=True)


def v7_output():
    output = deepcopy(v6_output())
    thesis = output["investment_thesis"]
    thesis.update(
        primary_track_outlook="测试设备赛道需求取决于客户资本开支，当前只能确认主营匹配。",
        company_position_summary="公司已形成测试设备收入，但市场份额和客户集中度仍待核验。",
        second_curve_summary="暂无独立第二曲线的可核验收入贡献。",
    )
    evidence_by_code = {
        "BUSINESS_OUTLOOK": (["business-1", "business-2"], [], "NEUTRAL"),
        "INDUSTRY_OUTLOOK": (["business-1"], [1], "MIXED"),
        "COMPETITIVE_POSITION": (["business-2"], [1], "NEUTRAL"),
        "VALUE_CHAIN_RESILIENCE": (["business-1", "relation-1"], [], "MIXED"),
        "VALUATION": ([], [], "UNKNOWN"),
        "INSTITUTION_EXPECTATION": ([], [], "UNKNOWN"),
        "MARKET_AND_CAPITAL": (["market-1"], [], "NEUTRAL"),
        "GOVERNANCE_AND_SHAREHOLDER": ([], [], "UNKNOWN"),
        "SECOND_CURVE": (["business-1"], [], "NEUTRAL"),
    }
    thesis["deep_assessments"] = [{
        "assessment_code": code, "conclusion": f"{code}的一年期证据结论。",
        "direction": direction, "confidence": 0.6 if direction != "UNKNOWN" else 0.2,
        "time_horizon": "MULTI_PERIOD", "evidence_document_ids": document_ids,
        "evidence_fact_ids": fact_ids, "evidence_relation_ids": [],
        "supporting_points": [], "risks": [], "watch_conditions": [],
    } for code, (fact_ids, document_ids, direction) in evidence_by_code.items()]
    thesis["hypotheses"][0]["evidence_items"] = [{
        "reference_type": "DOCUMENT", "reference_id": "2", "stance": "SUPPORT",
        "claim": "关联公司宣布扩产", "company_specific_effect": "若已审核供货关系持续，可能形成设备需求线索。",
        "quote": "关联公司宣布扩产。", "rationale": "扩产与设备需求假设方向一致，但不等于订单已经兑现。",
    }]
    return output


def v8_research_questions():
    return [
        {
            "research_code": "BUSINESS_MODEL", "question": "公司靠什么赚钱，收入利润能否持续？",
            "status": "COMPLETE", "conclusion": "主营收入事实已核验，持续性仍需跟踪订单和现金流。", "confidence": .7,
            "known_facts": [{"reference_type": "FACT", "reference_id": "business-1", "verification_status": "VERIFIED",
                             "fact": "公司主营测试设备研发与销售。", "significance": "用于识别收入发动机。"}],
            "hypotheses": [], "missing_evidence": [], "external_research_plan": [], "tracking_metrics": ["主营收入和经营现金流"],
            "evidence_document_ids": [], "evidence_fact_ids": ["business-1"], "evidence_relation_ids": [],
        },
        {"research_code": "MANAGEMENT_CHANGE", "question": "重大管理层调整如何影响经营连续性？", "status": "NOT_APPLICABLE",
         "conclusion": "本轮证据包未识别相关管理层调整。", "confidence": .2, "known_facts": [], "hypotheses": [],
         "missing_evidence": [], "external_research_plan": [], "tracking_metrics": [], "evidence_document_ids": [],
         "evidence_fact_ids": [], "evidence_relation_ids": []},
        {"research_code": "ACQUISITION_SECOND_CURVE", "question": "并购是否形成可兑现的第二曲线？", "status": "NOT_APPLICABLE",
         "conclusion": "本轮证据包未识别并购事件。", "confidence": .2, "known_facts": [], "hypotheses": [],
         "missing_evidence": [], "external_research_plan": [], "tracking_metrics": [], "evidence_document_ids": [],
         "evidence_fact_ids": [], "evidence_relation_ids": []},
        {"research_code": "CAPITAL_ACTION_INTENT", "question": "回购、注销或锁定行为的可能动因是什么？", "status": "NOT_APPLICABLE",
         "conclusion": "本轮证据包未识别相关资本行为。", "confidence": .2, "known_facts": [], "hypotheses": [],
         "missing_evidence": [], "external_research_plan": [], "tracking_metrics": [], "evidence_document_ids": [],
         "evidence_fact_ids": [], "evidence_relation_ids": []},
    ]


def v8_output():
    output = v7_output()
    output["investment_thesis"]["research_questions"] = v8_research_questions()
    return output


def test_v8_requires_all_four_research_questions():
    _validate_investment_thesis_output(v8_output(), v5_item(), required=True,
                                       require_deep_assessments=True, require_research_questions=True)
    missing = v8_output()
    missing["investment_thesis"]["research_questions"].pop()
    with pytest.raises(ValueError, match="V8分析缺少研究问题"):
        _validate_investment_thesis_output(missing, v5_item(), required=True,
                                           require_deep_assessments=True, require_research_questions=True)


def test_v8_partial_question_requires_gap_and_plan():
    changed = v8_output()
    question = changed["investment_thesis"]["research_questions"][0]
    question.update(status="PARTIAL", missing_evidence=[], external_research_plan=[])
    with pytest.raises(ValueError, match="证据缺口"):
        _validate_investment_thesis_output(changed, v5_item(), required=True,
                                           require_deep_assessments=True, require_research_questions=True)


def test_v8_research_question_not_applicable_cannot_hide_identified_event():
    changed = v8_output()
    # The validator reads the evidence snapshot from the item, so add an event
    # to a copied item rather than mutating the shared fixture.
    item = deepcopy(v5_item())
    item["evidence_json"]["governance_event_timeline"] = [{"event_types": ["CORPORATE_ACQUISITION"]}]
    with pytest.raises(ValueError, match="不能标为不适用"):
        _validate_investment_thesis_output(changed, item, required=True,
                                           require_deep_assessments=True, require_research_questions=True)


def test_v8_publication_distinguishes_research_incomplete_from_model_failure():
    complete = {**analysis(), "analysis_contract": "FUNDAMENTAL_RESEARCH_V8",
                "model": {"reasoning": "基于证据完成研究", "confidence": .7,
                          "investment_thesis": {"research_questions": v8_research_questions()}},}
    assert conclusion_publication(complete)["status"] == "READY_REVIEW"
    incomplete_questions = deepcopy(v8_research_questions())
    incomplete_questions[0]["status"] = "PARTIAL"
    incomplete_questions[0]["missing_evidence"] = [{"gap_code": "CASH_FLOW", "description": "缺现金流", "impact_on_conclusion": "影响持续性", "resolution_status": "LOCAL_SOURCE_PENDING"}]
    incomplete_questions[0]["external_research_plan"] = ["补取最新财报"]
    incomplete = {**complete, "model": {**complete["model"], "investment_thesis": {"research_questions": incomplete_questions}}}
    result = conclusion_publication(incomplete)
    assert result["status"] == "RESEARCH_INCOMPLETE"
    assert result["provisional_conclusion_available"] is True and result["conclusion_available"] is False


def test_hypothesis_research_plan_maps_event_questions_to_existing_sources():
    questions = v8_research_questions()
    questions[1].update(status="PARTIAL", confidence=.6, hypotheses=[{
        "title": "市场开拓不力", "thesis": "管理层变化可能与市场开拓结果有关。",
        "status": "INSUFFICIENT", "direction": "UNKNOWN", "horizon": "MEDIUM", "confidence": .45,
        "supporting_document_ids": [], "supporting_fact_ids": [], "supporting_relation_ids": [],
        "counter_document_ids": [], "counter_fact_ids": [], "counter_relation_ids": [],
        "evidence_items": [], "invalidation_conditions": ["收入和订单持续增长"], "watch_conditions": ["客户进展"],
    }], missing_evidence=[{
        "gap_code": "MANAGEMENT_REASON", "description": "缺少管理层调整原因",
        "impact_on_conclusion": "影响对经营连续性的判断", "required_evidence": ["去年营收和客户进展"],
        "preferred_sources": ["公告", "新闻", "财报"], "search_queries": ["总裁 市场开拓 营收"],
        "resolution_status": "LOCAL_SOURCE_PENDING",
    }], external_research_plan=["补取公告、新闻和财务数据"])
    candidate = SelectionCandidate(id=99, run_id=1, market="HK", symbol="02533",
        analysis_json={"model": {"investment_thesis": {"research_questions": questions}}}, evidence_json={})
    plan = _build_hypothesis_research_plan(candidate, round_no=1)
    management = next(row for row in plan["questions"] if row["research_code"] == "MANAGEMENT_CHANGE")
    assert {"NOTICE", "NEWS", "FINANCIAL", "COMPANY", "RESEARCH"}.issubset(set(management["dimensions"]))
    assert plan["baseline_evidence"]["fingerprint"]


def test_hypothesis_research_no_increment_decays_and_closes_secondary_hypothesis():
    questions = v8_research_questions()
    questions[1].update(status="PARTIAL", confidence=.55, hypotheses=[{
        "title": "市场开拓不力", "thesis": "管理层变化可能与市场开拓结果有关。",
        "status": "INSUFFICIENT", "direction": "UNKNOWN", "horizon": "MEDIUM", "confidence": .60,
        "supporting_document_ids": [], "supporting_fact_ids": [], "supporting_relation_ids": [],
        "counter_document_ids": [], "counter_fact_ids": [], "counter_relation_ids": [],
        "evidence_items": [], "invalidation_conditions": ["订单增长"], "watch_conditions": ["管理层说明"],
    }], missing_evidence=[{
        "gap_code": "MANAGEMENT_REASON", "description": "缺少直接说明",
        "impact_on_conclusion": "影响动因判断", "required_evidence": [], "preferred_sources": [],
        "search_queries": [], "resolution_status": "LOCAL_SOURCE_PENDING",
    }], external_research_plan=["补取已接来源"])
    candidate = SelectionCandidate(id=100, run_id=1, market="HK", symbol="02533",
        analysis_json={"model": {"investment_thesis": {"research_questions": questions}}}, evidence_json={})
    policy = _apply_research_confidence_policy(candidate, evidence_added=False,
                                               consecutive_no_increment=2, terminal=True)
    hypothesis = _research_questions(candidate)[1]["hypotheses"][0]
    assert hypothesis["confidence"] <= .20
    assert policy["low_confidence_closed_count"] == 1


def test_hypothesis_research_context_inherits_prior_questions_and_round_receipts():
    questions = v8_research_questions()
    questions[1].update(status="PARTIAL", confidence=.55, hypotheses=[{
        "title": "市场开拓不力", "thesis": "管理层调整可能与市场开拓有关。",
        "status": "INSUFFICIENT", "direction": "UNKNOWN", "confidence": .42,
        "supporting_document_ids": [11], "supporting_fact_ids": ["f-1"],
        "counter_document_ids": [], "counter_fact_ids": [],
        "invalidation_conditions": ["收入与订单持续增长"], "watch_conditions": ["管理层说明"],
    }])
    context = _hypothesis_research_model_context({
        "model": {"investment_thesis": {"research_questions": questions}},
        "hypothesis_research": {
            "status": "RUNNING", "round": 1, "max_rounds": 3,
            "history": [{"round": 1, "status": "VERIFIED_WITH_INCREMENT",
                         "new_document_ids": [12], "new_fact_ids": ["f-2"],
                         "source_outcome": {"status": "SOURCE_QUERIED"}}],
        },
    })
    management = next(row for row in context["questions"] if row["research_code"] == "MANAGEMENT_CHANGE")
    assert management["hypotheses"][0]["title"] == "市场开拓不力"
    assert management["hypotheses"][0]["confidence"] == .42
    assert context["completed_rounds"][0]["new_document_ids"] == [12]


def collaborative_research_questions(hypothesis_count=3):
    questions = v8_research_questions()
    hypotheses = [
        {
            "title": title,
            "thesis": thesis,
            "status": "INSUFFICIENT",
            "direction": direction,
            "horizon": "MEDIUM",
            "confidence": confidence,
            "supporting_document_ids": [],
            "supporting_fact_ids": [],
            "supporting_relation_ids": [],
            "counter_document_ids": [],
            "counter_fact_ids": [],
            "counter_relation_ids": [],
            "evidence_items": [],
            "invalidation_conditions": ["后续经营数据与该解释不一致"],
            "watch_conditions": ["管理层说明与经营指标"],
        }
        for title, thesis, direction, confidence in [
            ("市场开拓不力", "管理层调整可能与市场开拓结果有关。", "NEGATIVE", .62),
            ("战略转型需要", "管理层调整可能服务于新业务转型。", "POSITIVE", .55),
            ("正常治理安排", "管理层调整也可能只是职责分工变化。", "NEUTRAL", .35),
        ][:hypothesis_count]
    ]
    questions[1].update(
        status="PARTIAL",
        confidence=.55,
        hypotheses=hypotheses,
        missing_evidence=[{
            "gap_code": "MANAGEMENT_REASON",
            "description": "缺少管理层调整动因的直接和间接证据",
            "impact_on_conclusion": "影响经营连续性和战略转型判断",
            "required_evidence": ["去年营收", "客户进展", "管理层说明"],
            "preferred_sources": ["公告", "新闻", "财报"],
            "search_queries": ["总裁 调整 营收 客户 战略"],
            "resolution_status": "LOCAL_SOURCE_PENDING",
        }],
        external_research_plan=["补取公告、新闻、财务和公司资料"],
    )
    return questions


def add_collaborative_candidate(db, *, hypothesis_count=3, symbol="02533"):
    run = SelectionRun(market="HK", status="PARTIAL", analysis_mode="MODEL_PARTIAL", data_mode="REAL")
    db.add(run)
    db.flush()
    candidate = SelectionCandidate(
        run_id=run.id,
        market="HK",
        symbol=symbol,
        name="黑芝麻智能",
        analysis_json={
            "model_output_status": "SUCCESS",
            "model": {"investment_thesis": {"research_questions": collaborative_research_questions(hypothesis_count)}},
        },
        evidence_json={"documents": [], "facts": [], "graph_paths": [], "structured_observations": {}},
    )
    db.add(candidate)
    db.commit()
    return run, candidate


def test_first_analysis_waits_for_user_and_confirming_direction_queues_research(isolated):
    db = isolated
    run, candidate = add_collaborative_candidate(db, hypothesis_count=2)
    app.dependency_overrides[get_db] = lambda: db
    try:
        client = TestClient(app)
        prepared = client.post(f"/api/v1/selection/candidates/{candidate.id}/hypothesis-direction/prepare")
        assert prepared.status_code == 200
        prepared_state = prepared.json()["analysis_json"]["hypothesis_research"]
        assert prepared_state["status"] == "WAITING_USER_DIRECTION"
        hypotheses = prepared_state["direction_review"]["questions"][0]["hypotheses"]

        response = client.post(
            f"/api/v1/selection/candidates/{candidate.id}/hypothesis-direction",
            json={
                "action": "CONFIRM_AND_RESEARCH",
                "directions": [
                    {"research_code": "MANAGEMENT_CHANGE", "hypothesis_key": hypotheses[0]["hypothesis_key"], "decision": "PRIORITY"},
                    {"research_code": "MANAGEMENT_CHANGE", "hypothesis_key": hypotheses[1]["hypothesis_key"], "decision": "PAUSED"},
                ],
                "max_rounds": 3,
            },
        )
        assert response.status_code == 202
        queued_state = response.json()["analysis_json"]["hypothesis_research"]
        assert queued_state["status"] == "QUEUED"
        # With only two explanations, pausing one would create confirmation
        # bias.  The service automatically retains it as a normal challenger.
        retained = queued_state["direction_review"]["questions"][0]["hypotheses"]
        assert [row["decision"] for row in retained] == ["PRIORITY", "NORMAL"]
        assert retained[1]["guardrail_retained"] is True
        job = db.scalar(select(ScheduledJob).where(ScheduledJob.task_type == "selection_hypothesis_research"))
        assert job is not None and job.payload_json["candidate_ids"] == [candidate.id]
        assert db.scalars(select(FoundationFact)).all() == []
        assert db.scalars(select(SelectionResearchFeedback)).all()
        db.refresh(run)
        assert run.criteria_json["hypothesis_research_progress"]["status"] == "QUEUED"
    finally:
        app.dependency_overrides.pop(get_db, None)


def test_failed_full_model_contract_recovers_only_valid_questions_for_user_direction(isolated):
    db = isolated
    run = SelectionRun(market="HK", status="PARTIAL", analysis_mode="MODEL_FAILED_FALLBACK_LOCAL", data_mode="REAL")
    db.add(run)
    db.flush()
    questions = deepcopy(v8_research_questions())
    base_hypotheses = collaborative_research_questions(2)[1]["hypotheses"]
    for index, question in enumerate(questions):
        question.update({
            "status": "PARTIAL",
            "conclusion": "模型提出竞争性解释，仍需按用户确认方向补数验证。",
            "confidence": .45,
            "hypotheses": [
                {**deepcopy(row), "title": f"{question['research_code']}-{index + 1}-{row['title']}"}
                for row in base_hypotheses
            ],
            "missing_evidence": [{
                "gap_code": f"{question['research_code']}_EVIDENCE",
                "description": "缺少能够区分竞争性解释的证据",
                "impact_on_conclusion": "当前不能形成投资结论",
                "required_evidence": ["已接数据源中的公告、新闻和结构化事实"],
                "preferred_sources": ["公告", "新闻", "财报"],
                "search_queries": [question["question"]],
                "resolution_status": "LOCAL_SOURCE_PENDING",
            }],
            "external_research_plan": ["先查询已接数据源，再比较支持证据与反证"],
        })
    # Deliberately omit the complete recommendation fields. The full output is
    # invalid, while each research question independently satisfies its closed
    # schema and can therefore be shown only as a direction draft.
    response_text = json.dumps({
        "candidates": [{
            "symbol": "02533",
            "investment_thesis": {"research_questions": questions},
        }],
    }, ensure_ascii=False)
    call = ModelCallLog(
        task_type="stock_screening", status="SUCCESS", instance_code="TEST_MODEL",
        response_text=response_text,
    )
    db.add(call)
    db.flush()
    candidate = SelectionCandidate(
        run_id=run.id, market="HK", symbol="02533", name="黑芝麻智能",
        evidence_json=evidence(),
        analysis_json={
            "model_output_status": "FAILED",
            "model_failure_reason": "完整模型输出未通过评分契约",
            "model_attempts": [{"model_call_log_id": call.id, "status": "SUCCESS"}],
        },
    )
    db.add(candidate)
    db.commit()

    prepared = prepare_selection_hypothesis_review(db, candidate.id)

    state = prepared.analysis_json["hypothesis_research"]
    assert state["status"] == "WAITING_USER_DIRECTION"
    assert state["direction_seed_origin"] == "RECOVERED_MODEL_CALL"
    assert state["direction_seed_model_call_log_id"] == call.id
    assert state["direction_seed_is_conclusion"] is False
    assert len(state["research_question_snapshot"]) == 4
    assert len(state["direction_review"]["questions"]) == 4
    assert prepared.analysis_json["model_output_status"] == "FAILED"
    assert db.scalars(select(FoundationFact)).all() == []
    db.refresh(run)
    assert run.analysis_mode == "RESEARCH_DIRECTION_REVIEW"
    assert run.criteria_json["result_counts"]["research_pending_count"] == 1
    assert run.criteria_json["result_counts"]["failed_count"] == 0

    review = state["direction_review"]
    first_question = review["questions"][0]
    first_hypothesis = first_question["hypotheses"][0]
    revised_thesis = "用户确认后，优先验证主营收入与现金流是否支持该解释。"
    with patch("app.jobs.governance_runner.wake_governance_runner"):
        response = TestClient(app)
        app.dependency_overrides[get_db] = lambda: db
        try:
            confirmed = response.post(
                f"/api/v1/selection/candidates/{candidate.id}/hypothesis-direction",
                json={
                    "action": "CONFIRM_AND_RESEARCH",
                    "directions": [{
                        "research_code": first_question["research_code"],
                        "hypothesis_key": first_hypothesis["hypothesis_key"],
                        "decision": "PRIORITY",
                        "revised_thesis": revised_thesis,
                    }],
                    "new_hypotheses": [{
                        "research_code": first_question["research_code"],
                        "title": "用户补充的竞争性解释",
                        "thesis": "该变化也可能由客户结构和交付节奏共同造成。",
                        "direction": "MIXED",
                        "horizon": "MEDIUM",
                    }],
                    "max_rounds": 3,
                },
            )
        finally:
            app.dependency_overrides.pop(get_db, None)
    assert confirmed.status_code == 202
    queued_state = confirmed.json()["analysis_json"]["hypothesis_research"]
    assert queued_state["status"] == "QUEUED"
    queued_question = next(
        row for row in queued_state["research_question_snapshot"]
        if row["research_code"] == first_question["research_code"]
    )
    assert queued_question["hypotheses"][0]["thesis"] == revised_thesis
    assert any(row["title"] == "用户补充的竞争性解释" for row in queued_question["hypotheses"])
    job = db.scalar(select(ScheduledJob).where(ScheduledJob.task_type == "selection_hypothesis_research"))
    assert job is not None and job.payload_json["candidate_ids"] == [candidate.id]
    assert db.scalars(select(FoundationFact)).all() == []


def test_fresh_successful_analysis_drops_stale_failed_research_runtime(isolated):
    db = isolated
    _run, candidate = add_collaborative_candidate(db, hypothesis_count=2, symbol="02539")
    candidate.analysis_json = {
        **candidate.analysis_json,
        "hypothesis_research": {
            "status": "MODEL_FAILED",
            "round": 2,
            "last_error": "旧模型失败",
            "current_plan": {"status": "FAILED"},
        },
    }
    db.commit()

    prepare_selection_hypothesis_review(db, candidate.id)

    state = candidate.analysis_json["hypothesis_research"]
    assert state["status"] == "WAITING_USER_DIRECTION"
    assert state["review_phase"] == "DIRECTION"
    assert "last_error" not in state and "current_plan" not in state and "round" not in state


def test_paused_hypothesis_is_excluded_from_research_plan_when_other_challenger_remains(isolated):
    db = isolated
    _run, candidate = add_collaborative_candidate(db, hypothesis_count=3, symbol="02534")
    prepare_selection_hypothesis_review(db, candidate.id)
    review = candidate.analysis_json["hypothesis_research"]["direction_review"]
    hypotheses = review["questions"][0]["hypotheses"]
    record_selection_hypothesis_direction(
        db,
        candidate.id,
        SelectionHypothesisDirectionRequest(
            action="CONFIRM_AND_RESEARCH",
            directions=[
                {"research_code": "MANAGEMENT_CHANGE", "hypothesis_key": hypotheses[0]["hypothesis_key"], "decision": "PRIORITY"},
                {"research_code": "MANAGEMENT_CHANGE", "hypothesis_key": hypotheses[1]["hypothesis_key"], "decision": "PAUSED"},
                {"research_code": "MANAGEMENT_CHANGE", "hypothesis_key": hypotheses[2]["hypothesis_key"], "decision": "NORMAL"},
            ],
        ),
    )
    plan = _build_hypothesis_research_plan(candidate, round_no=1)
    planned = plan["questions"][0]["hypotheses"]
    assert [row["title"] for row in planned] == ["市场开拓不力", "正常治理安排"]
    assert [row["research_priority"] for row in planned] == ["PRIORITY", "NORMAL"]


def test_model_failed_research_restores_delegated_plan_and_can_queue_next_attempt(isolated):
    db = isolated
    run, candidate = add_collaborative_candidate(db, hypothesis_count=3, symbol="02538")
    prepare_selection_hypothesis_review(db, candidate.id)
    record_selection_hypothesis_direction(
        db,
        candidate.id,
        SelectionHypothesisDirectionRequest(action="DELEGATE_TO_SYSTEM", max_rounds=3),
    )
    original_plan = _build_hypothesis_research_plan(candidate, round_no=1)
    analysis = deepcopy(candidate.analysis_json)
    analysis.pop("model", None)
    state = analysis["hypothesis_research"]
    # Simulate a failed model revalidation removing the current response. The
    # confirmed direction and bounded plan must remain retryable.
    state.update({
        "status": "MODEL_FAILED",
        "round": 1,
        "terminal_reason": "MODEL_FAILED",
        "current_plan": original_plan,
        "history": [{"round": 1, "status": "MODEL_FAILED", "questions": original_plan["questions"]}],
    })
    candidate.analysis_json = analysis
    db.commit()

    restored = _build_hypothesis_research_plan(candidate, round_no=2)
    assert restored["questions"]
    assert restored["questions"][0]["research_code"] == "MANAGEMENT_CHANGE"
    assert restored["questions"][0]["hypotheses"][0]["research_priority"] == "PRIORITY"
    assert candidate.analysis_json["hypothesis_research"]["direction_review"]["status"] == "DELEGATED"

    with patch("app.jobs.governance_runner.wake_governance_runner"):
        queued = queue_selection_hypothesis_research(
            db, run.id, SelectionHypothesisResearchRequest(candidate_ids=[candidate.id], max_rounds=3),
        )
    db.refresh(candidate)
    assert queued.id == run.id
    assert candidate.analysis_json["hypothesis_research"]["status"] == "QUEUED"
    job = db.scalar(select(ScheduledJob).where(
        ScheduledJob.task_type == "selection_hypothesis_research",
        ScheduledJob.payload_json["selection_run_id"].as_integer() == run.id,
    ))
    assert job is not None and job.payload_json["candidate_ids"] == [candidate.id]


def test_conflicting_evidence_pauses_for_human_review_and_run_reports_waiting_user(isolated):
    db = isolated
    run, candidate = add_collaborative_candidate(db, hypothesis_count=2, symbol="02535")
    prepare_selection_hypothesis_review(db, candidate.id)
    review = candidate.analysis_json["hypothesis_research"]["direction_review"]
    hypotheses = review["questions"][0]["hypotheses"]
    record_selection_hypothesis_direction(
        db,
        candidate.id,
        SelectionHypothesisDirectionRequest(
            action="CONFIRM_AND_RESEARCH",
            directions=[
                {"research_code": "MANAGEMENT_CHANGE", "hypothesis_key": hypotheses[0]["hypothesis_key"], "decision": "PRIORITY"},
                {"research_code": "MANAGEMENT_CHANGE", "hypothesis_key": hypotheses[1]["hypothesis_key"], "decision": "NORMAL"},
            ],
        ),
    )
    backfill = {
        "status": "SUCCESS",
        "results": [{"candidate_id": candidate.id, "status": "SUCCESS", "operations": {"NOTICE": {"status": "SUCCESS"}}}],
    }

    def reanalyse(_payload):
        changed = deepcopy(candidate.analysis_json)
        question = changed["model"]["investment_thesis"]["research_questions"][1]
        hypothesis = question["hypotheses"][0]
        hypothesis.update(
            status="MIXED",
            confidence=.55,
            supporting_document_ids=[101],
            counter_document_ids=[102],
        )
        candidate.analysis_json = changed
        candidate.evidence_json = {
            "documents": [
                {"id": 101, "content": "经营数据显示客户拓展取得进展，支持市场能力改善的解释。"},
                {"id": 102, "content": "管理层公告同时披露职责调整，仍存在战略转型的竞争性解释。"},
            ],
            "facts": [], "graph_paths": [], "structured_observations": {},
        }
        db.commit()
        return {"selection_run_id": run.id, "candidate_count": 1, "research_failed_count": 0, "status": "COMPLETED"}

    with patch("app.services.selection.execute_selection_data_backfill", return_value=backfill), \
            patch("app.services.selection.execute_failed_selection_reanalysis", side_effect=reanalyse), \
            patch("app.services.selection_audit.capture_decision_snapshot"):
        result = execute_selection_hypothesis_research({
            "selection_run_id": run.id, "candidate_ids": [candidate.id], "max_rounds": 3,
        })

    db.refresh(candidate)
    db.refresh(run)
    assert result["status"] == "WAITING_USER"
    assert candidate.analysis_json["hypothesis_research"]["status"] == "WAITING_EVIDENCE_REVIEW"
    assert run.criteria_json["hypothesis_research_progress"]["status"] == "WAITING_USER"

    # Accepting an unchanged conflict must not trap the user in the same
    # evidence-review screen.  Only a changed support/counter set may ask
    # for another evidence review.
    record_selection_research_review(
        db,
        candidate.id,
        SelectionResearchReviewRequest(
            phase="EVIDENCE",
            action="ACCEPT",
            comment="已确认该组支持与反证，继续完成下一轮验证。",
            max_rounds=2,
        ),
    )
    with patch("app.services.selection.execute_selection_data_backfill", return_value=backfill), \
            patch("app.services.selection.execute_failed_selection_reanalysis", side_effect=reanalyse), \
            patch("app.services.selection_audit.capture_decision_snapshot"):
        resumed = execute_selection_hypothesis_research({
            "selection_run_id": run.id, "candidate_ids": [candidate.id], "max_rounds": 2,
        })
    db.refresh(candidate)
    assert resumed["status"] == "WAITING_USER"
    assert candidate.analysis_json["hypothesis_research"]["status"] == "WAITING_CONCLUSION_REVIEW"


def test_terminal_collaborative_round_waits_for_conclusion_review(isolated):
    db = isolated
    run, candidate = add_collaborative_candidate(db, hypothesis_count=2, symbol="02536")
    prepare_selection_hypothesis_review(db, candidate.id, max_rounds=1)
    review = candidate.analysis_json["hypothesis_research"]["direction_review"]
    hypotheses = review["questions"][0]["hypotheses"]
    record_selection_hypothesis_direction(
        db,
        candidate.id,
        SelectionHypothesisDirectionRequest(
            action="CONFIRM_AND_RESEARCH",
            directions=[
                {"research_code": "MANAGEMENT_CHANGE", "hypothesis_key": hypotheses[0]["hypothesis_key"], "decision": "PRIORITY"},
                {"research_code": "MANAGEMENT_CHANGE", "hypothesis_key": hypotheses[1]["hypothesis_key"], "decision": "NORMAL"},
            ],
            max_rounds=1,
        ),
    )
    analysis = deepcopy(candidate.analysis_json)
    analysis["hypothesis_research"]["last_error"] = "上一轮模型重验失败"
    analysis["hypothesis_research"]["terminal_reason"] = "MODEL_FAILED"
    candidate.analysis_json = analysis
    db.commit()
    backfill = {
        "status": "SUCCESS",
        "results": [{"candidate_id": candidate.id, "status": "SUCCESS", "operations": {"NOTICE": {"status": "SUCCESS"}}}],
    }
    with patch("app.services.selection.execute_selection_data_backfill", return_value=backfill), \
            patch("app.services.selection.execute_failed_selection_reanalysis", return_value={"research_failed_count": 0}), \
            patch("app.services.selection_audit.capture_decision_snapshot"):
        result = execute_selection_hypothesis_research({
            "selection_run_id": run.id, "candidate_ids": [candidate.id], "max_rounds": 1,
        })
    db.refresh(candidate)
    assert result["status"] == "WAITING_USER"
    assert candidate.analysis_json["hypothesis_research"]["status"] == "WAITING_CONCLUSION_REVIEW"
    assert "last_error" not in candidate.analysis_json["hypothesis_research"]


def test_validated_user_corrections_create_review_draft_without_writing_facts_or_mutating_skill(isolated):
    db = isolated
    _run, candidate = add_collaborative_candidate(db, hypothesis_count=2, symbol="02537")
    skill = ModelSkill(
        skill_code="HYPOTHESIS_RESEARCH_ORCHESTRATOR",
        skill_name="重大事件假设研究编排",
        instructions="生产版受控研究指令。",
        version="9.0.0",
        lifecycle_status="ACTIVE",
        enabled=True,
    )
    db.add(skill)
    db.commit()
    original_version = skill.version
    original_instructions = skill.instructions
    for index in range(3):
        record_selection_research_review(
            db,
            candidate.id,
            SelectionResearchReviewRequest(
                phase="EVIDENCE",
                action="CORRECT",
                research_code="MANAGEMENT_CHANGE",
                comment=f"第{index + 1}次纠正原解释",
                correction="应同时验证市场开拓与战略转型两个方向。",
            ),
        )
    assert db.scalars(select(FoundationFact)).all() == []

    _resolve_research_feedback(db, candidate, round_no=2, evidence_added=True, terminal=False)
    db.commit()
    db.refresh(skill)
    feedback = db.scalars(select(SelectionResearchFeedback).where(
        SelectionResearchFeedback.action == "CORRECT",
    )).all()
    draft = db.scalar(select(SkillOptimizationDraft))
    summary = selection_research_learning_summary(db, candidate.id)

    assert len(feedback) == 3
    assert {row.outcome_status for row in feedback} == {"VALIDATED_CORRECTION"}
    assert db.scalars(select(FoundationFact)).all() == []
    assert draft is not None and draft.status == "PENDING_REVIEW"
    assert draft.base_skill_version == original_version
    assert skill.version == original_version
    assert skill.instructions == original_instructions
    assert summary["skill"]["draft_status"] == "PENDING_REVIEW"
    assert summary["skill"]["production_skill_changed"] is False


def test_source_failure_is_not_treated_as_exhausted_evidence():
    failed = _research_source_outcome({
        "results": [{"candidate_id": 7, "status": "PARTIAL", "operations": {
            "NEWS": {"status": "FAILED", "error": "provider timeout"},
            "FINANCIAL": {"status": "SUCCESS"},
        }}],
    }, 7)
    assert failed["status"] == "SOURCE_FAILED"
    assert failed["failed_operations"][0]["operation"] == "NEWS"
    exhausted = _research_source_outcome({
        "results": [{"candidate_id": 7, "status": "PARTIAL", "operations": {
            "NEWS": {"status": "MISSING"},
            "FINANCIAL": {"status": "SUCCESS"},
        }}],
    }, 7)
    assert exhausted["status"] == "SOURCE_QUERIED"
    assert exhausted["exhausted_operations"][0]["operation"] == "NEWS"


def test_hypothesis_research_model_exception_is_persisted_as_retryable_failure(isolated):
    db = isolated
    questions = v8_research_questions()
    questions[1].update(
        status="PARTIAL", confidence=.55,
        hypotheses=[{
            "title": "市场开拓不力", "thesis": "管理层变化可能与市场开拓结果有关",
            "status": "INSUFFICIENT", "direction": "UNKNOWN", "confidence": .45,
            "supporting_document_ids": [], "supporting_fact_ids": [],
            "counter_document_ids": [], "counter_fact_ids": [],
            "invalidation_conditions": ["收入和订单持续增长"], "watch_conditions": ["客户进展"],
        }],
        missing_evidence=[{
            "gap_code": "MANAGEMENT_REASON", "description": "缺少管理层调整原因",
            "impact_on_conclusion": "影响经营连续性判断", "required_evidence": ["去年营收"],
            "preferred_sources": ["公告", "财报"], "search_queries": ["总裁 市场开拓 营收"],
            "resolution_status": "LOCAL_SOURCE_PENDING",
        }],
        external_research_plan=["补取公告与财务数据"],
    )
    run = SelectionRun(market="HK", status="PARTIAL", analysis_mode="MODEL_PARTIAL", data_mode="REAL")
    db.add(run)
    db.flush()
    candidate = SelectionCandidate(
        run_id=run.id, market="HK", symbol="02533", name="黑芝麻智能",
        analysis_json={"model": {"investment_thesis": {"research_questions": questions}}},
        evidence_json={},
    )
    db.add(candidate)
    db.commit()
    backfill = {
        "status": "PARTIAL",
        "results": [{
            "candidate_id": candidate.id, "status": "PARTIAL",
            "operations": {"NOTICE": {"status": "SUCCESS"}, "FINANCIAL": {"status": "SUCCESS"}},
        }],
    }

    with patch("app.services.selection.execute_selection_data_backfill", return_value=backfill), \
            patch("app.services.selection.execute_failed_selection_reanalysis",
                  side_effect=ValueError("最终汇总上下文超过安全预算")):
        result = execute_selection_hypothesis_research({
            "selection_run_id": run.id, "candidate_ids": [candidate.id], "max_rounds": 3,
        })

    db.refresh(candidate)
    db.refresh(run)
    state = candidate.analysis_json["hypothesis_research"]
    assert result["status"] == "PARTIAL"
    assert state["status"] == "MODEL_FAILED"
    assert state["terminal_reason"] == "MODEL_FAILED"
    assert state["history"][-1]["reanalysis"]["error_type"] == "ValueError"
    assert state.get("consecutive_no_increment", 0) == 0
    assert run.criteria_json["hypothesis_research_progress"]["status"] == "PARTIAL"


def test_publication_reports_retryable_research_failures_without_closing_hypotheses():
    questions = v8_research_questions()
    questions[1].update(status="PARTIAL", confidence=.6)
    base = {**analysis(), "analysis_contract": "FUNDAMENTAL_RESEARCH_V8",
        "model": {"reasoning": "主营证据具备，管理层动因仍在验证。", "confidence": .7,
                  "investment_thesis": {"research_questions": questions}}}
    source_failed = {**base, "hypothesis_research": {"status": "SOURCE_FAILED", "round": 1}}
    result = conclusion_publication(source_failed)
    assert result["status"] == "RESEARCH_INCOMPLETE"
    assert "数据源" in result["reason"] and "重试" in result["reason"]
    model_failed = {**base, "hypothesis_research": {"status": "MODEL_FAILED", "round": 1}}
    result = conclusion_publication(model_failed)
    assert result["status"] == "RESEARCH_INCOMPLETE"
    assert "模型" in result["reason"] and "重试" in result["reason"]


def test_publication_allows_low_confidence_secondary_hypothesis_after_bounded_loop():
    questions = v8_research_questions()
    questions[1].update(status="PARTIAL", confidence=.15, conclusion="多轮验证未发现直接证据。")
    completed = {**analysis(), "analysis_contract": "FUNDAMENTAL_RESEARCH_V8",
        "hypothesis_research": {"status": "COMPLETED", "round": 3, "max_rounds": 3,
                                "open_high_confidence_count": 0, "low_confidence_closed_count": 1},
        "model": {"reasoning": "核心主营证据成立；管理层动因按低置信关闭。", "confidence": .7,
                  "investment_thesis": {"research_questions": questions}}}
    result = conclusion_publication(completed)
    assert result["status"] == "READY_REVIEW_WITH_UNCERTAINTY"
    assert result["conclusion_available"] is True


def test_publication_keeps_business_model_as_hard_gate_after_research_loop():
    questions = v8_research_questions()
    questions[0].update(status="INSUFFICIENT", confidence=.1)
    completed = {**analysis(), "analysis_contract": "FUNDAMENTAL_RESEARCH_V8",
        "hypothesis_research": {"status": "COMPLETED", "round": 3, "max_rounds": 3,
                                "open_high_confidence_count": 0, "low_confidence_closed_count": 2},
        "model": {"reasoning": "主营证据仍缺失。", "confidence": .5,
                  "investment_thesis": {"research_questions": questions}}}
    result = conclusion_publication(completed)
    assert result["status"] == "RESEARCH_INCOMPLETE"
    assert result["conclusion_available"] is False


def test_v7_requires_complete_deep_assessments_and_auditable_document_stance():
    _validate_investment_thesis_output(
        v7_output(), v5_item(), required=True, require_deep_assessments=True,
    )
    missing = v7_output()
    missing["investment_thesis"]["deep_assessments"].pop()
    with pytest.raises(ValueError, match="缺少深度评价维度"):
        _validate_investment_thesis_output(
            missing, v5_item(), required=True, require_deep_assessments=True,
        )


def test_v7_rejects_supporting_quote_misfiled_as_counter_evidence():
    changed = v7_output()
    hypothesis = changed["investment_thesis"]["hypotheses"][0]
    hypothesis["supporting_document_ids"] = []
    hypothesis["counter_document_ids"] = [2]
    with pytest.raises(ValueError, match="标为支持的证据明细"):
        _validate_investment_thesis_output(
            changed, v5_item(), required=True, require_deep_assessments=True,
        )


def test_v5_business_values_are_tied_to_business_composition_fact():
    _validate_business_value_chain_output(v5_output(), v5_item(), required=True)
    changed = v5_output()
    changed["business_quality"]["revenue_mix"][0]["revenue_ratio"] = 0.9
    with pytest.raises(ValueError, match="与来源事实不一致"):
        _validate_business_value_chain_output(changed, v5_item(), required=True)


def test_v5_business_accepts_documents_from_bounded_evidence_context():
    item = v5_item()
    item["evidence_json"]["evidence_context"] = {
        "documents": [{
            "id": 9, "source_table": "stock_news", "content_state": "FULLTEXT",
            "content": "公司披露主营产品量产及客户验证进展。",
        }],
    }
    changed = v5_output()
    changed["business_quality"]["evidence_document_ids"] = [9]

    _validate_business_value_chain_output(changed, item, required=True)


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


def test_v5_business_unknown_source_is_withdrawn_without_inventing_a_segment():
    changed = v5_output()
    changed["business_quality"]["revenue_mix"][0]["evidence_fact_id"] = "missing-fact"

    _normalize_business_composition_output(changed, v5_item())

    assert "evidence_normalizations" not in changed
    assert changed["business_quality"]["revenue_mix"] == []
    assert "已撤销该条目" in changed["evidence_normalization_notes"][0]
    _validate_business_value_chain_output(changed, v5_item(), required=True)


def test_v7_invalid_hypothesis_quote_is_withdrawn_and_hypothesis_downgraded():
    output = {"investment_thesis": {"status": "SUPPORTED", "hypotheses": [{
        "title": "毛利率相对优势", "status": "SUPPORTED", "direction": "POSITIVE", "confidence": .8,
        "supporting_document_ids": [101], "supporting_fact_ids": [], "supporting_relation_ids": [],
        "counter_document_ids": [], "counter_fact_ids": [], "counter_relation_ids": [],
        "evidence_items": [{
            "reference_type": "DOCUMENT", "reference_id": "101", "stance": "SUPPORT",
            "claim": "毛利率逆势提升", "company_specific_effect": "相对竞争力改善",
            "quote": "原文中不存在的改写句", "rationale": "行业承压下的公司相对优势",
        }],
    }]}}
    item = {"evidence_json": {
        "documents": [{"id": 101, "content": "行业总体利润承压，但公司毛利率同比提升。"}],
        "facts": [], "graph_paths": [],
    }}

    _sanitize_investment_thesis_evidence(output, item)

    hypothesis = output["investment_thesis"]["hypotheses"][0]
    assert hypothesis["supporting_document_ids"] == []
    assert hypothesis["evidence_items"] == []
    assert hypothesis["status"] == "INSUFFICIENT"
    assert hypothesis["direction"] == "UNKNOWN"
    assert output["investment_thesis"]["status"] == "PARTIAL"


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


def test_financial_dimension_accepts_scored_earnings_and_institution_forecasts():
    payload = {"facts": [
        {"id": "stock_earnings_forecast:15", "fact_type": "EARNINGS_FORECAST"},
        {"id": "stock_institution_forecast:16", "fact_type": "INSTITUTION_FORECAST"},
    ]}
    row = dimension("FINANCIAL")
    row.update(
        evidence_fact_ids=["stock_earnings_forecast:15", "stock_institution_forecast:16"],
        evidence_assessments=[
            assessment("stock_earnings_forecast:15", kind="FACT"),
            assessment("stock_institution_forecast:16", kind="FACT"),
        ],
    )
    assert typed_references(row, payload)["evidence_fact_ids"] == [
        "stock_earnings_forecast:15", "stock_institution_forecast:16",
    ]
    validate_assessments({"dimensions": [row]}, payload, required=True)


def test_company_dimension_accepts_company_classification_observation():
    payload = {"facts": [{
        "id": "stock_symbol:5092", "fact_type": "CLASSIFICATION_OBSERVATION",
        "properties": {"industry": "专用设备", "themes": ["存储芯片"]},
    }]}
    row = dimension("COMPANY")
    row.update(
        evidence_fact_ids=["stock_symbol:5092"],
        evidence_assessments=[assessment("stock_symbol:5092", kind="FACT")],
    )
    assert typed_references(row, payload)["evidence_fact_ids"] == ["stock_symbol:5092"]
    validate_assessments({"dimensions": [row]}, payload, required=True)


def test_coverage_summary_does_not_report_context_budget_as_collection_failure():
    result = evidence_coverage_summary({
        "data_gate": {"status": "READY"},
        "counts": {"period_documents": 155, "period_documents_included": 80},
        "missing_data": [
            "分析周期内文档超过上下文预算（已载入 80/155 篇）",
            "文档切片",
            "结构化财务观测",
            "文档#23703正文补取失败：缺少可读取的官方公告 PDF 地址",
        ],
    }, {"model_output_status": "FAILED", "model_failure_reason": "模型证据引用校验未通过"})
    assert result["minimum_analysis_ready"] is True
    assert result["requires_data_backfill"] is True
    assert len(result["context_limits"]) == 1
    assert len(result["optional_enrichment_gaps"]) == 2
    assert len(result["source_failures"]) == 1
    assert result["acquisition_gaps"] == []
    assert result["model_validation_error"] == "模型证据引用校验未通过"


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


def test_run_data_phase_is_partial_for_real_source_failure_not_context_limit(isolated):
    db = isolated
    run = SelectionRun(
        market="CN_A", status="PARTIAL", analysis_mode="MODEL_FAILED_FALLBACK_LOCAL", data_mode="REAL",
        completed_at=datetime.now(timezone.utc),
        criteria_json={"progress": {"phases": {
            "DATA": {"status": "SUCCESS", "percent": 100, "activities": []},
            "MODEL_PREFLIGHT": {"status": "SUCCESS", "percent": 100, "activities": []},
            "ANALYSIS": {"status": "FAILED", "percent": 100, "activities": []},
        }}},
    )
    db.add(run)
    db.flush()
    db.add(SelectionCandidate(
        run_id=run.id, market="CN_A", symbol="688627",
        analysis_json={"model_output_status": "FAILED", "data_gate": {"status": "READY"},
                       "model_failure_reason": "逐条评分校验未通过"},
        evidence_json={
            **evidence(), "data_gate": {"status": "READY"},
            "missing_data": [
                "分析周期内文档超过上下文预算（已载入 80/155 篇）",
                "文档#23703正文补取失败：缺少可读取的官方公告 PDF 地址",
            ],
            "counts": {"period_documents": 155, "period_documents_included": 80},
        },
    ))
    db.commit()
    app.dependency_overrides[get_db] = lambda: db
    try:
        value = TestClient(app).get(f"/api/v1/selection/runs/{run.id}").json()
        phase = value["criteria_json"]["progress"]["phases"]["DATA"]
        assert phase["status"] == "PARTIAL"
        assert "正文补取失败" in phase["coverage_message"]
        coverage = value["candidates"][0]["analysis_json"]["evidence_coverage"]
        assert len(coverage["source_failures"]) == 1
        assert len(coverage["context_limits"]) == 1
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


def test_hypothesis_backfill_uses_lightweight_projection_while_normal_backfill_keeps_lakehouse(isolated):
    db = isolated
    knowledge_base = KnowledgeBase(
        kb_code="selection-projection-test", kb_name="选股投影测试",
        status="ACTIVE", source_tables=["stock_news"],
    )
    db.add(knowledge_base)
    db.flush()
    graph = KnowledgeGraph(
        knowledge_base_id=knowledge_base.id, graph_code="selection-projection-test",
        graph_name="选股投影测试", source_tables=["stock_news"],
    )
    db.add(graph)
    db.flush()
    run = SelectionRun(
        market="CN_A", status="PARTIAL", analysis_mode="MODEL_PARTIAL", data_mode="REAL",
        knowledge_base_ids_json=[knowledge_base.id], graph_ids_json=[graph.id],
    )
    db.add(run)
    db.flush()
    candidate = SelectionCandidate(run_id=run.id, market="CN_A", symbol="000001", evidence_json={})
    db.add(candidate)
    db.commit()

    pipeline_calls = []

    def run_pipeline(_db, **kwargs):
        pipeline_calls.append(kwargs)
        return {
            "graph_id": graph.id,
            "graph": {"status": "BUILT", "counts": {"coverage": {"complete": True}}},
        }

    common = {
        "selection_run_id": run.id, "candidate_ids": [candidate.id],
        "dimensions_by_candidate": {str(candidate.id): ["NEWS"]},
        "period_months": 12, "rerun_analysis": False,
    }
    with patch("app.services.stock_batch._fetch_one", return_value={"status": "SUCCESS"}), \
            patch("app.services.knowledge_pipeline.run_stock_pipeline", side_effect=run_pipeline):
        execute_selection_data_backfill({**common, "research_round": 1, "attempt": "research"})
        execute_selection_data_backfill({**common, "research_round": 0, "attempt": "normal"})

    assert len(pipeline_calls) == 2
    assert pipeline_calls[0]["export_lakehouse"] is False
    assert pipeline_calls[0]["archive_chunks"] is False
    assert pipeline_calls[0]["run_graph"] is True
    assert pipeline_calls[0]["idempotency_key"] == (
        f"selection-hypothesis-projection-v2:{run.id}:{candidate.id}:1"
    )
    assert pipeline_calls[1]["export_lakehouse"] is True
    assert pipeline_calls[1]["archive_chunks"] is True
    assert pipeline_calls[1]["run_graph"] is True
    assert pipeline_calls[1]["idempotency_key"] == f"selection-backfill-knowledge:{run.id}:normal"
