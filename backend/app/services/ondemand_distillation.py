"""Deterministic, allowlisted source reader for on-demand distillation.

This module deliberately does not call a model, vector database, or Neo4j.
The scheduler must split long source text before any model call.
"""

from __future__ import annotations

import hashlib
import json
from typing import Any, Literal, cast
from uuid import UUID

from sqlalchemy import inspect, select, text
from sqlalchemy.orm import Session

from app.db.session import SessionLocal
from app.models.ai_hub import KnowledgeDocument, ModelSkill, SkillOptimizationDraft
from app.schemas.distillation import ExtractionSource, KgTriplet, SourceTable


SOURCE_TABLES: frozenset[str] = frozenset(
    {"stock_news", "stock_notice", "stock_financial_report"}
)


def _text_from_payload(payload: Any) -> str | None:
    if isinstance(payload, str):
        try:
            payload = json.loads(payload)
        except json.JSONDecodeError:
            return None
    if not isinstance(payload, dict):
        return None
    for key in ("content", "body", "text"):
        value = payload.get(key)
        if isinstance(value, str) and value.strip():
            return value.strip()
    return None


def _load(stock_code: str, doc_id: int, db: Session) -> ExtractionSource:
    if not stock_code.strip() or doc_id <= 0:
        raise ValueError("stock_code and a positive knowledge_document.id are required")
    document = db.get(KnowledgeDocument, doc_id)
    if document is None:
        raise LookupError(f"Knowledge document {doc_id} does not exist")
    if document.symbol != stock_code:
        raise ValueError("Stock code does not match the knowledge document")
    source_table = document.source_table
    if source_table not in SOURCE_TABLES or document.source_record_id is None:
        raise ValueError("Knowledge document has no supported business source")

    # Table name comes only from the fixed allowlist above; values remain bound.
    row = db.execute(
        text(f'SELECT * FROM "{source_table}" WHERE id = :id AND symbol = :symbol'),
        {"id": document.source_record_id, "symbol": stock_code},
    ).mappings().one_or_none()
    if row is None:
        raise LookupError("Source record no longer exists for this stock")
    raw = row.get("content_raw")
    if not isinstance(raw, str) or not raw.strip():
        raw = row.get("content") if source_table == "stock_news" else None
    if not isinstance(raw, str) or not raw.strip():
        payload_key = "data_json" if source_table == "stock_financial_report" else "content_json"
        raw = _text_from_payload(row.get(payload_key))
    if not isinstance(raw, str) or not raw.strip():
        raise ValueError("Source has no raw text; fetch full content before distillation")
    raw = raw.strip()
    return ExtractionSource(
        knowledge_document_id=document.id,
        source_table=cast(SourceTable, source_table),
        source_record_id=document.source_record_id,
        stock_code=stock_code,
        title=document.title,
        content_raw=raw,
        content_sha256=hashlib.sha256(raw.encode("utf-8")).hexdigest(),
        source_url=row.get("url"),
        fetched_at=row.get("fetched_at"),
    )


def trigger_ondemand_extraction(
    stock_code: str, doc_id: int, db: Session | None = None
) -> ExtractionSource:
    """Read one provenance-linked document; never query the full market.

    ``doc_id`` is knowledge_document.id, not the business table's row ID.
    A provided Session lets a caller join this read to a task transaction.
    """
    if db is not None:
        return _load(stock_code, doc_id, db)
    with SessionLocal() as session:
        return _load(stock_code, doc_id, session)


ReviewReason = Literal["LOW_CONFIDENCE", "NEW_NODE", "CONFLICT"]


def stage_distillation_review(
    db: Session,
    *,
    skill_id: int,
    task_id: UUID,
    knowledge_document_id: int,
    triplet: KgTriplet,
    reason: ReviewReason,
) -> SkillOptimizationDraft:
    """Stage an entity decision in the existing draft table, without a commit.

    The ordinary Skill-approval API rejects this draft by signature and type.
    A separate entity-review action must make the final graph decision.
    """
    if reason == "LOW_CONFIDENCE" and triplet.confidence >= 0.8:
        raise ValueError("LOW_CONFIDENCE review requires confidence below 0.8")
    skill = db.get(ModelSkill, skill_id)
    if skill is None:
        raise LookupError("Distillation Skill not found")
    context = {
        "task_id": str(task_id),
        "knowledge_document_id": knowledge_document_id,
        "reason": reason,
        "triplet": triplet.model_dump(mode="json"),
    }
    context_json = json.dumps(context, ensure_ascii=False, sort_keys=True)
    signature = "DISTILLATION_REVIEW:" + hashlib.sha256(
        context_json.encode("utf-8")
    ).hexdigest()
    existing = db.scalar(select(SkillOptimizationDraft).where(
        SkillOptimizationDraft.failure_signature == signature
    ))
    if existing is not None:
        return existing
    draft = SkillOptimizationDraft(
        skill_id=skill.id,
        base_skill_version=skill.version,
        failure_signature=signature,
        prediction_ids=[],
        proposed_instructions=skill.instructions,
        rationale=context_json,
        status="PENDING_REVIEW",
    )
    db.add(draft)
    db.flush()
    columns = {column["name"] for column in inspect(db.connection()).get_columns("skill_optimization_draft")}
    if db.get_bind().dialect.name == "postgresql" and {"draft_type", "review_context_json"}.issubset(columns):
        db.execute(
            text("UPDATE skill_optimization_draft SET draft_type = 'DISTILLATION_REVIEW', "
                 "review_context_json = CAST(:context AS jsonb) WHERE id = :draft_id"),
            {"context": context_json, "draft_id": draft.id},
        )
    return draft
