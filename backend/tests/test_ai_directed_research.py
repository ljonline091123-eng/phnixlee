from datetime import datetime, timezone

from sqlalchemy import create_engine, func, select
from sqlalchemy.orm import Session
from sqlalchemy.pool import StaticPool

from app.db.base import Base
from app.models.foundation import FoundationFact
from app.models.selection import SelectionCandidate, SelectionResearchFeedback, SelectionRun
from app.schemas.selection import SelectionAiDirectedResearchOutput, SelectionAiResearchConfirmRequest
from app.services.ai_directed_research import (
    _adapt_provider_direction_format,
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
        assert state["status"] == "READY_FOR_EVIDENCE_COLLECTION"
        assert state["collection_plan"]["dimensions"] == ["FINANCIAL", "RESEARCH"]
        assert (db.scalar(select(func.count(FoundationFact.id))) or 0) == before
        feedback = db.scalar(select(SelectionResearchFeedback))
        assert feedback is not None
        assert feedback.skill_code == "AI_DIRECTED_RESEARCH_PLANNER"
    engine.dispose()
