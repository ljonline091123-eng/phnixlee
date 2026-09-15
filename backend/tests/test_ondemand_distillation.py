from __future__ import annotations

import hashlib
from uuid import uuid4

import pytest
from fastapi import HTTPException
from pydantic import ValidationError
from sqlalchemy import create_engine
from sqlalchemy.orm import Session

from app.api.model_hub import approve_skill_draft
from app.db.base import Base
from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, ModelSkill
from app.models.market_data import StockNews
from app.schemas.distillation import DistillationOutput, KgTriplet
from app.services.ondemand_distillation import stage_distillation_review, trigger_ondemand_extraction


def test_distillation_output_rejects_unknown_fields_and_invalid_relationships() -> None:
    valid = {
        "kb_chunks": [{
            "chunk_text": "A 公司属于软件行业。",
            "metadata": {
                "knowledge_document_id": 1,
                "source_table": "stock_news",
                "source_record_id": 2,
                "stock_code": "000001",
                "question": "A 公司属于什么行业？",
                "answer": "软件行业。",
            },
        }],
        "kg_triplets": [{
            "head": {"kind": "Company", "key": "000001", "name": "A 公司"},
            "relation": "BELONGS_TO",
            "tail": {"kind": "Industry", "key": "软件", "name": "软件"},
            "confidence": 0.95,
            "evidence_text": "属于软件行业",
        }],
    }
    assert DistillationOutput.model_validate(valid).kg_triplets[0].relation == "BELONGS_TO"
    with pytest.raises(ValidationError):
        DistillationOutput.model_validate({**valid, "unapproved": True})
    wrong = {**valid, "kg_triplets": [{**valid["kg_triplets"][0], "relation": "SUPPLIES_TO"}]}
    with pytest.raises(ValidationError):
        DistillationOutput.model_validate(wrong)


def test_source_reader_checks_document_provenance_and_uses_real_text() -> None:
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(engine)
    with Session(engine) as db:
        kb = KnowledgeBase(kb_code="TEST_KB", kb_name="Test")
        news = StockNews(
            market="CN", symbol="000001", news_time="2026-09-15", title="Test",
            content="A company published a dated announcement.", source_id=1,
        )
        db.add_all([kb, news])
        db.flush()
        document = KnowledgeDocument(
            knowledge_base_id=kb.id, source_table="stock_news",
            source_record_id=news.id, symbol="000001", title="Test", content=news.content,
        )
        db.add(document)
        db.flush()

        source = trigger_ondemand_extraction("000001", document.id, db)
        assert source.content_raw == news.content
        assert source.content_sha256 == hashlib.sha256(news.content.encode("utf-8")).hexdigest()
        with pytest.raises(ValueError, match="Stock code does not match"):
            trigger_ondemand_extraction("000002", document.id, db)

        news.content = None
        db.flush()
        with pytest.raises(ValueError, match="no raw text"):
            trigger_ondemand_extraction("000001", document.id, db)


def test_entity_review_cannot_approve_a_skill_prompt_change() -> None:
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(engine)
    with Session(engine) as db:
        skill = ModelSkill(
            skill_code="TEST_DISTILLER", skill_name="Test", instructions="unchanged",
            skill_type="PROMPT_SOP",
        )
        db.add(skill)
        db.flush()
        triplet = KgTriplet.model_validate({
            "head": {"kind": "Company", "key": "000001", "name": "A 公司"},
            "relation": "BELONGS_TO",
            "tail": {"kind": "Industry", "key": "软件", "name": "软件"},
            "confidence": 0.65,
            "evidence_text": "属于软件行业",
        })
        task_id = uuid4()
        draft = stage_distillation_review(
            db, skill_id=skill.id, task_id=task_id,
            knowledge_document_id=1, triplet=triplet, reason="LOW_CONFIDENCE",
        )
        assert stage_distillation_review(
            db, skill_id=skill.id, task_id=task_id,
            knowledge_document_id=1, triplet=triplet, reason="LOW_CONFIDENCE",
        ).id == draft.id
        assert draft.failure_signature.startswith("DISTILLATION_REVIEW:")
        with pytest.raises(HTTPException) as error:
            approve_skill_draft(draft.id, db)
        assert error.value.status_code == 409
