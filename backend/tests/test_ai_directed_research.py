from datetime import datetime, timezone

from sqlalchemy import create_engine, func, select
from sqlalchemy.orm import Session
from sqlalchemy.pool import StaticPool

from app.db.base import Base
from app.models.foundation import FoundationFact
from app.models.selection import SelectionCandidate, SelectionResearchFeedback, SelectionRun
from app.models.pipeline import ScheduledJob
from app.schemas.selection import SelectionAiDirectedResearchOutput, SelectionAiResearchConfirmRequest
from app.services.ai_directed_research import (
    _adapt_provider_direction_format,
    _adapt_verification_format,
    _ensure_inference_candidates,
    _inference_candidates_from_directions,
    _validate_references,
    confirm_ai_directed_research,
)


def test_provider_direction_shape_keeps_only_visible_evidence_ids():
    pack = {
        "evidence_json": {
            "documents": [{"id": 7, "source_table": "stock_news", "content": "订单已经签署。"}],
            "facts": [{"id": "fact:1", "fact_type": "BUSINESS_PROFILE"}],
            "graph_paths": [],
            "citation_allowlist": {"document_ids": [7], "fact_ids": ["fact:1"], "relation_ids": []},
        },
    }
    raw = {
        "company_profile": {"main_business": "车规级芯片", "industry": "智能驾驶"},
        "research_directions": [
            {"hypothesis": f"方向{i}", "importance": "影响收入", "current_judgment": "等待验证",
             "supporting_evidence": {"DOCUMENT": "7", "FACT": "fact:1"},
             "counter_evidence": {"DOCUMENT": "999"}, "gap": "缺少后续数据",
             "suggestion_source": "公告", "data_dimensions": ["sales_data"], "priority": i}
            for i in range(1, 4)
        ],
    }
    adapted, repaired = _adapt_provider_direction_format(raw, pack)
    validated = SelectionAiDirectedResearchOutput.model_validate(adapted)
    cleaned, warnings = _validate_references(validated.model_dump(mode="json"), pack)

    assert repaired is True
    assert len(cleaned["directions"]) == 3
    assert {row["reference_id"] for row in cleaned["directions"][0]["supporting_evidence"]} == {"7", "fact:1"}
    assert cleaned["directions"][0]["counter_evidence"] == []
    assert any("未返回原文引句" in warning for warning in warnings)
    assert len(adapted["inference_candidates"]) == 3
    assert all(row["status"] == "NEEDS_VERIFICATION" for row in adapted["inference_candidates"])
    assert all(row["confidence"] <= 0.6 for row in adapted["inference_candidates"])


def test_preliminary_inferences_are_derived_and_capped_without_inventing_evidence():
    directions = [{
        "direction_id": "management_change", "title": "管理层变化",
        "hypothesis": "管理层调整可能影响战略执行", "why_it_matters": "需要观察交接",
        "current_judgment": "现有公告显示发生调整，但原因仍需验证", "confidence": 0.97,
        "supporting_evidence": [{"reference_type": "DOCUMENT", "reference_id": "9", "stance": "SUPPORT", "claim": "公告披露职务调整"}],
        "counter_evidence": [], "suggested_sources": ["后续公告"],
    }]
    result = _inference_candidates_from_directions(directions)
    assert len(result) == 1
    assert result[0]["direction_id"] == "management_change"
    assert result[0]["confidence"] == 0.6
    assert result[0]["status"] == "NEEDS_VERIFICATION"
    assert result[0]["supporting_evidence"][0]["reference_id"] == "9"


def test_existing_inferences_survive_failed_second_pass_and_are_bounded():
    state = _ensure_inference_candidates({
        "directions": [{"direction_id": "business", "current_judgment": "主营可能受益"}],
        "inference_candidates": [{
            "inference_id": "inference_business", "direction_id": "business", "title": "主营判断",
            "conclusion": "主营可能受益，但仍需验证", "conclusion_type": "POSITIVE",
            "rationale": "已有业务事实", "confidence": 0.95,
            "supporting_evidence": [], "counter_evidence": [], "implications": [],
            "suggested_followups": [], "status": "PRELIMINARY",
        }],
    })
    assert state["inference_candidates"][0]["conclusion"] == "主营可能受益，但仍需验证"
    assert state["inference_candidates"][0]["confidence"] == 0.6
    assert state["inference_candidates"][0]["status"] == "PRELIMINARY"


def test_direction_confirmation_writes_audit_but_not_foundation_fact():
    engine = create_engine("sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool)
    Base.metadata.create_all(engine)
    with Session(engine) as db:
        run = SelectionRun(
            market="HK", status="PARTIAL", analysis_mode="AI_DIRECTED_RESEARCH",
            criteria_json={}, as_of=datetime(2026, 11, 9, tzinfo=timezone.utc),
        )
        db.add(run)
        db.flush()
        candidate = SelectionCandidate(
            run_id=run.id, market="HK", symbol="02533", name="黑芝麻智能",
            evidence_json={}, analysis_json={"ai_directed_research": {
                "mode": "AI_DIRECTED_RESEARCH_V1", "status": "WAITING_USER_DIRECTION",
                "directions": [{
                    "direction_id": "profitability", "title": "盈利改善",
                    "suggested_dimensions": ["FINANCIAL", "RESEARCH"],
                    "suggested_sources": ["财务报告"], "data_gaps": ["成本结构"],
                }],
            }},
        )
        db.add(candidate)
        db.commit()
        before = db.scalar(select(func.count(FoundationFact.id))) or 0

        updated = confirm_ai_directed_research(db, candidate.id, SelectionAiResearchConfirmRequest(
            action="CONFIRM_AND_RESEARCH", selected_direction_ids=["profitability"], comment="优先验证盈利改善",
        ))

        state = updated.analysis_json["ai_directed_research"]
        assert state["status"] == "QUEUED"
        assert state["collection_plan"]["dimensions"] == ["FINANCIAL", "RESEARCH"]
        assert (db.scalar(select(func.count(FoundationFact.id))) or 0) == before
        feedback = db.scalar(select(SelectionResearchFeedback))
        assert feedback is not None
        assert feedback.skill_code == "AI_DIRECTED_RESEARCH_PLANNER"
        assert db.execute(select(ScheduledJob).where(
            ScheduledJob.task_type == "selection_ai_directed_research",
        )).scalar_one_or_none() is not None
    engine.dispose()


def test_verification_planning_shape_is_preserved_as_insufficient():
    payload = {"directions": [{
        "direction_id": "profitability", "current_judgment": "收入增长但仍亏损",
        "confidence": 0.8, "supporting_evidence": [], "counter_evidence": [],
        "data_gaps": ["成本结构"], "suggested_sources": ["财务报告"],
    }]}
    adapted, repaired = _adapt_verification_format(payload, {
        "selected_direction_ids": ["profitability"],
    })

    assert repaired is True
    assert adapted["overall_status"] == "INSUFFICIENT"
    assert adapted["direction_results"][0]["judgment"] == "INSUFFICIENT"
    assert adapted["direction_results"][0]["confidence"] == 0.25
