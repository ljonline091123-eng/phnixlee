"""Foundation operations; callers own commit and rollback boundaries."""

import hashlib
import json
from datetime import date, datetime, timedelta, timezone
from typing import Any, get_args

from jsonschema import Draft202012Validator
from sqlalchemy import func, or_, select, update
from sqlalchemy.orm import Session

from app.models.ai_hub import KnowledgeDocument
from app.models.foundation import (
    FoundationEntity, FoundationEvidence, FoundationFact, FoundationFactEvidence,
    FoundationFactReview, FoundationListing, FoundationSecurity,
)
from app.models.market_data import StockSymbol
from app.schemas.foundation import EntityCreate, EntityType, EvidenceCreate, FactCreate, FactReviewCreate, FactType, SecurityMappingCreate


class FoundationError(ValueError):
    def __init__(self, message: str, status_code: int = 422):
        self.status_code = status_code
        super().__init__(message)


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


def aware(value: datetime | None) -> datetime | None:
    return value.replace(tzinfo=timezone.utc) if value and value.tzinfo is None else value


def required(db: Session, model, record_id):
    row = db.get(model, record_id)
    if row is None:
        raise FoundationError(f"{model.__tablename__} record not found: {record_id}", 404)
    return row


def _json(value: Any) -> str:
    try:
        result = json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"), allow_nan=False)
    except (TypeError, ValueError) as exc:
        raise FoundationError("properties must contain finite JSON values") from exc
    if len(result) > 100000:
        raise FoundationError("properties JSON exceeds 100000 characters")
    return result


def _schema(required_keys: list[str], properties: dict[str, Any]) -> dict[str, Any]:
    return {"type": "object", "required": required_keys, "properties": properties, "additionalProperties": True}


_TEXT = {"type": "string", "minLength": 1, "maxLength": 1024, "pattern": r"\S"}
_AMOUNT = {"type": "number", "minimum": 0}
_CURRENCY = {"type": "string", "pattern": "^[A-Z]{3}$"}
FACT_SCHEMAS = {
    "HOLDS_EQUITY": {
        **_schema([], {"ratio": {"type": "number", "minimum": 0, "maximum": 100}, "ratio_basis": _TEXT,
                       "shares": _AMOUNT, "shares_unit": _TEXT}),
        "anyOf": [{"required": ["ratio", "ratio_basis"]}, {"required": ["shares", "shares_unit"]}],
        "dependentRequired": {"ratio": ["ratio_basis"], "shares": ["shares_unit"]},
    },
    "CONTROLS": _schema(["control_basis"], {"control_basis": _TEXT}),
    "SUPPLIES_TO": _schema(["product"], {"product": _TEXT, "business_scope": _TEXT}),
    "PARTNERS_WITH": _schema(["business_scope"], {"business_scope": _TEXT}),
    "GUARANTEES": _schema(["amount", "currency"], {"amount": _AMOUNT, "currency": _CURRENCY}),
    "LENDS_TO": _schema(["amount", "currency"], {"amount": _AMOUNT, "currency": _CURRENCY}),
    "COMPETES_WITH": _schema(["business_scope"], {"business_scope": _TEXT}),
    "IN_INDUSTRY": _schema(["taxonomy", "taxonomy_version"], {"taxonomy": _TEXT, "taxonomy_version": _TEXT}),
    "MEMBER_OF_THEME": _schema(["classification_method"], {"classification_method": _TEXT}),
    "HAS_CLASSIFICATION": _schema(["classification_method"], {"classification_method": _TEXT}),
    "CONTRACT": {**_schema(["contract_status"], {
        "contract_status": {"enum": ["INTENT", "SIGNED", "ACTIVE", "COMPLETED", "TERMINATED"]},
        "amount": _AMOUNT, "currency": _CURRENCY,
    }), "dependentRequired": {"amount": ["currency"], "currency": ["amount"]}},
    "LEGAL_CASE": {**_schema(["case_number", "court", "jurisdiction", "procedure_type", "party_role", "case_status"], {
        "case_number": _TEXT, "court": _TEXT, "jurisdiction": _TEXT,
        "procedure_type": {"enum": ["CIVIL", "CRIMINAL", "ADMINISTRATIVE", "ARBITRATION", "ENFORCEMENT", "BANKRUPTCY", "OTHER"]},
        "party_role": {"enum": ["PLAINTIFF", "DEFENDANT", "THIRD_PARTY", "APPLICANT", "RESPONDENT", "DEBTOR", "CREDITOR", "OTHER"]},
        "case_status": {"enum": ["FILED", "PENDING", "JUDGMENT", "APPEAL", "SETTLED", "WITHDRAWN", "ENFORCEMENT", "CLOSED", "UNKNOWN"]},
        "amount": _AMOUNT, "currency": _CURRENCY,
        "amount_type": {"enum": ["CLAIMED", "JUDGMENT", "ENFORCEMENT", "SETTLEMENT", "PROVISION"]},
    }), "dependentRequired": {"amount": ["currency", "amount_type"], "currency": ["amount", "amount_type"], "amount_type": ["amount", "currency"]}},
}


def catalog() -> dict:
    return {"entity_types": list(get_args(EntityType)), "fact_types": list(get_args(FactType)),
            "fact_schemas": FACT_SCHEMAS, "statuses": ["PENDING", "ACCEPTED", "REJECTED"],
            "valid_to_inclusive": False, "reviewer_is_authenticated_identity": False}


def create_entity(db: Session, payload: EntityCreate) -> FoundationEntity:
    values = payload.model_dump()
    values["jurisdiction"] = payload.jurisdiction.upper()
    if payload.identifier_scheme:
        values["identifier_scheme"] = payload.identifier_scheme.upper()
        duplicate = db.scalar(select(FoundationEntity).where(
            FoundationEntity.jurisdiction == values["jurisdiction"],
            FoundationEntity.identifier_scheme == values["identifier_scheme"],
            FoundationEntity.identifier_value == payload.identifier_value,
        ))
        if duplicate:
            raise FoundationError("external identifier already belongs to an entity", 409)
    _json(values["properties_json"])
    row = FoundationEntity(**values, created_at=utc_now())
    db.add(row)
    db.flush()
    return row


def entity_read(row: FoundationEntity) -> dict:
    result = {key: getattr(row, key) for key in ("id", "name", "entity_type", "jurisdiction", "identifier_scheme",
                                               "identifier_value", "properties_json", "created_at")}
    result["created_at"] = aware(row.created_at)
    return result


def create_evidence(db: Session, payload: EvidenceCreate, metadata: dict | None = None) -> FoundationEvidence:
    if payload.entity_id:
        required(db, FoundationEntity, payload.entity_id)
    now = utc_now()
    metadata = dict(metadata or {"content_scope": "SUBMITTED_TEXT", "completeness": "UNVERIFIED"})
    metadata["availability_basis"] = "PROVIDED" if payload.available_at else "RECORDED_AT"
    # Missing availability stays null in the fingerprint, so repeat submissions
    # deduplicate even though the first arrival receives a recorded timestamp.
    values = payload.model_dump(mode="json")
    stable_metadata = {key: value for key, value in metadata.items()
                       if key not in {"legacy_document_id", "legacy_graph_id"}}
    fingerprint_data = {**values, "metadata": stable_metadata}
    fingerprint = hashlib.sha256(json.dumps(fingerprint_data, sort_keys=True, ensure_ascii=False,
                                            separators=(",", ":"), allow_nan=False).encode("utf-8")).hexdigest()
    existing = db.scalar(select(FoundationEvidence).where(
        FoundationEvidence.source_name == payload.source_name, FoundationEvidence.source_key == payload.source_key,
        FoundationEvidence.fingerprint == fingerprint,
    ))
    if existing:
        return existing
    version = int(db.scalar(select(func.max(FoundationEvidence.version)).where(
        FoundationEvidence.source_name == payload.source_name, FoundationEvidence.source_key == payload.source_key,
    )) or 0) + 1
    available_at = payload.available_at or now
    if payload.published_at and available_at < payload.published_at:
        raise FoundationError("available_at must not precede published_at")
    row = FoundationEvidence(
        entity_id=payload.entity_id, source_name=payload.source_name, source_key=payload.source_key,
        title=payload.title, content=payload.content, url=str(payload.url) if payload.url else None,
        published_at=payload.published_at, available_at=available_at, version=version,
        content_hash=hashlib.sha256(payload.content.encode("utf-8")).hexdigest(), fingerprint=fingerprint,
        metadata_json=metadata, created_at=now,
    )
    db.add(row)
    db.flush()
    return row


def evidence_read(row: FoundationEvidence, full: bool = False) -> dict:
    result = {key: getattr(row, key) for key in ("id", "entity_id", "source_name", "source_key", "title", "url",
              "published_at", "available_at", "content_hash", "version", "metadata_json", "created_at")}
    result["content_length"] = len(row.content)
    result["excerpt"] = row.content[:500]
    for key in ("published_at", "available_at", "created_at"):
        result[key] = aware(result[key])
    if full:
        result["content"] = row.content
    return result


def import_legacy_document(db: Session, document_id: int, entity_id: str | None = None) -> FoundationEvidence:
    document = required(db, KnowledgeDocument, document_id)
    original = dict(document.metadata_json or {})
    url = original.get("url")
    if not isinstance(url, str) or not url.startswith(("https://", "http://")):
        url = None
    source_name = str(original.get("source_name") or document.source_table)[:256]
    source_key = f"legacy:{document.source_table}:{document.source_record_id if document.source_record_id is not None else 'document-' + str(document.id)}"
    metadata = {
        "content_scope": "LEGACY_KNOWLEDGE_DOCUMENT", "completeness": "NOT_VERIFIED_AS_ORIGINAL_FULLTEXT",
        "legacy_document_id": document.id, "legacy_graph_id": document.graph_id,
        "source_table": document.source_table, "source_record_id": document.source_record_id,
        "legacy_metadata": original, "market": document.market, "symbol": document.symbol,
        "content_truncated": original.get("content_truncated", original.get("truncated")),
    }
    return create_evidence(db, EvidenceCreate(entity_id=entity_id, source_name=source_name, source_key=source_key,
                           title=document.title, content=document.content, url=url), metadata=metadata)


def listing_read(db: Session, listing: FoundationListing) -> dict:
    security = required(db, FoundationSecurity, listing.security_id)
    return {"id": listing.id, "entity_id": security.entity_id, "security_id": security.id,
            "stock_symbol_id": listing.stock_symbol_id, "market": listing.market, "symbol": listing.symbol,
            "name": listing.name, "share_class": security.share_class, "evidence_id": listing.evidence_id,
            "created_at": aware(listing.created_at)}


def create_mapping(db: Session, payload: SecurityMappingCreate) -> FoundationListing:
    entity = required(db, FoundationEntity, payload.entity_id)
    if entity.entity_type != "COMPANY":
        raise FoundationError("only a COMPANY can issue mapped securities")
    stock = required(db, StockSymbol, payload.stock_symbol_id)
    evidence = required(db, FoundationEvidence, payload.evidence_id)
    if evidence.entity_id and evidence.entity_id != entity.id:
        raise FoundationError("mapping evidence is assigned to another entity")
    existing = db.scalar(select(FoundationListing).where(FoundationListing.stock_symbol_id == stock.id))
    if existing:
        security = required(db, FoundationSecurity, existing.security_id)
        if security.entity_id != entity.id:
            raise FoundationError("stock is already mapped to another company", 409)
        if payload.share_class and security.share_class != payload.share_class:
            raise FoundationError("existing share class differs; mapping is immutable", 409)
        return existing
    security = FoundationSecurity(entity_id=entity.id, share_class=payload.share_class, created_at=utc_now())
    db.add(security)
    db.flush()
    listing = FoundationListing(security_id=security.id, stock_symbol_id=stock.id, market=stock.market,
                                symbol=stock.symbol, name=stock.name, evidence_id=evidence.id, created_at=utc_now())
    db.add(listing)
    db.flush()
    return listing


def _validate_endpoints(payload: FactCreate, subject: FoundationEntity, obj: FoundationEntity | None) -> None:
    expected_objects = {"HOLDS_EQUITY": {"COMPANY"}, "CONTROLS": {"COMPANY", "ORGANIZATION"},
                        "IN_INDUSTRY": {"INDUSTRY"}, "MEMBER_OF_THEME": {"THEME"},
                        "HAS_CLASSIFICATION": {"CLASSIFICATION"}}
    actor_types = {"COMPANY", "PERSON", "ORGANIZATION"}
    subject_types = {"COMPANY"} if payload.fact_type in {"IN_INDUSTRY", "MEMBER_OF_THEME", "HAS_CLASSIFICATION"} else actor_types
    if subject.entity_type not in subject_types:
        raise FoundationError("subject entity type is invalid for this fact")
    if obj and obj.entity_type not in expected_objects.get(payload.fact_type, actor_types | {"PROJECT"}):
        raise FoundationError("object entity type is invalid for this fact")


def create_fact(db: Session, payload: FactCreate) -> FoundationFact:
    subject = required(db, FoundationEntity, payload.subject_entity_id)
    obj = required(db, FoundationEntity, payload.object_entity_id) if payload.object_entity_id else None
    _validate_endpoints(payload, subject, obj)
    _json(payload.properties_json)
    errors = sorted(Draft202012Validator(FACT_SCHEMAS[payload.fact_type]).iter_errors(payload.properties_json),
                    key=lambda item: str(list(item.path)))
    if errors:
        raise FoundationError(f"invalid {payload.fact_type} properties: {errors[0].message}")
    for evidence_id in set(payload.evidence_ids):
        required(db, FoundationEvidence, evidence_id)
    now = utc_now()
    row = FoundationFact(**payload.model_dump(exclude={"evidence_ids"}), status="PENDING", created_at=now, updated_at=now)
    db.add(row)
    db.flush()
    db.add_all([FoundationFactEvidence(fact_id=row.id, evidence_id=value) for value in sorted(set(payload.evidence_ids))])
    db.flush()
    return row


def fact_evidence(db: Session, fact_id: str) -> list[FoundationEvidence]:
    return list(db.scalars(select(FoundationEvidence).join(FoundationFactEvidence,
        FoundationFactEvidence.evidence_id == FoundationEvidence.id).where(FoundationFactEvidence.fact_id == fact_id)
        .order_by(FoundationEvidence.created_at, FoundationEvidence.id)))


def reviews_for(db: Session, fact_id: str, known_at: datetime | None = None) -> list[FoundationFactReview]:
    query = select(FoundationFactReview).where(FoundationFactReview.fact_id == fact_id)
    if known_at:
        query = query.where(FoundationFactReview.created_at <= known_at)
    return list(db.scalars(query.order_by(FoundationFactReview.created_at, FoundationFactReview.id)))


def status_at(db: Session, row: FoundationFact, known_at: datetime | None) -> str:
    if not known_at:
        return row.status
    reviews = reviews_for(db, row.id, known_at)
    return reviews[-1].decision if reviews else "PENDING"


def fact_read(db: Session, row: FoundationFact, *, full: bool = False, known_at: datetime | None = None) -> dict:
    subject = required(db, FoundationEntity, row.subject_entity_id)
    obj = required(db, FoundationEntity, row.object_entity_id) if row.object_entity_id else None
    evidence = fact_evidence(db, row.id)
    result = {key: getattr(row, key) for key in ("id", "fact_type", "title", "subject_entity_id", "object_entity_id",
              "properties_json", "valid_from", "valid_to", "created_at", "updated_at")}
    result.update(subject_name=subject.name, object_name=obj.name if obj else None,
                  status=status_at(db, row, known_at), evidence_ids=[item.id for item in evidence])
    if known_at:
        history = reviews_for(db, row.id, known_at)
        result["updated_at"] = history[-1].created_at if history else row.created_at
    result["created_at"] = aware(result["created_at"])
    result["updated_at"] = aware(result["updated_at"])
    if full:
        result["evidence"] = [evidence_read(item, full=True) for item in evidence]
        result["reviews"] = [{**{key: getattr(item, key) for key in
            ("id", "previous_status", "decision", "reason", "reviewer")}, "created_at": aware(item.created_at)}
            for item in reviews_for(db, row.id, known_at)]
    return result


def review_fact(db: Session, fact_id: str, payload: FactReviewCreate) -> FoundationFact:
    row = required(db, FoundationFact, fact_id)
    if row.status != payload.expected_status:
        raise FoundationError("fact status changed; reload before reviewing", 409)
    allowed = {("PENDING", "ACCEPTED"), ("PENDING", "REJECTED"), ("ACCEPTED", "REJECTED")}
    if (payload.expected_status, payload.decision) not in allowed:
        raise FoundationError("invalid or repeated review transition", 409)
    # A logical microsecond preserves review order when clock resolution is
    # coarse or two decisions share a timestamp; UUIDs are not sequence numbers.
    now = max(utc_now(), aware(row.updated_at) + timedelta(microseconds=1))
    changed = db.execute(update(FoundationFact).where(FoundationFact.id == fact_id,
        FoundationFact.status == payload.expected_status).values(status=payload.decision, updated_at=now)
        .execution_options(synchronize_session=False))
    if changed.rowcount != 1:
        raise FoundationError("fact status changed; reload before reviewing", 409)
    db.add(FoundationFactReview(fact_id=fact_id, previous_status=payload.expected_status,
        decision=payload.decision, reason=payload.reason, reviewer=payload.reviewer, created_at=now))
    db.flush()
    db.refresh(row)
    return row


def facts_query(*, status: str = "ACCEPTED", entity_id: str | None = None, fact_type: str | None = None,
                q: str = "", as_of: date | None = None, known_at: datetime | None = None):
    query = select(FoundationFact)
    if entity_id:
        query = query.where(or_(FoundationFact.subject_entity_id == entity_id, FoundationFact.object_entity_id == entity_id))
    if fact_type:
        query = query.where(FoundationFact.fact_type == fact_type)
    if q:
        query = query.where(FoundationFact.title.contains(q, autoescape=True))
    if as_of:
        query = query.where(or_(FoundationFact.valid_from.is_(None), FoundationFact.valid_from <= as_of),
                            or_(FoundationFact.valid_to.is_(None), FoundationFact.valid_to > as_of))
    if known_at:
        latest_decision = select(FoundationFactReview.decision).where(
            FoundationFactReview.fact_id == FoundationFact.id, FoundationFactReview.created_at <= known_at,
        ).order_by(FoundationFactReview.created_at.desc(), FoundationFactReview.id.desc()).limit(1).correlate(FoundationFact).scalar_subquery()
        status_value = func.coalesce(latest_decision, "PENDING")
        unknown_evidence = select(FoundationFactEvidence.fact_id).join(FoundationEvidence,
            FoundationEvidence.id == FoundationFactEvidence.evidence_id).where(
            FoundationFactEvidence.fact_id == FoundationFact.id,
            or_(FoundationEvidence.available_at > known_at, FoundationEvidence.created_at > known_at))
        known_entities = select(FoundationEntity.id).where(FoundationEntity.created_at <= known_at)
        query = query.where(FoundationFact.created_at <= known_at, ~unknown_evidence.exists(),
            FoundationFact.subject_entity_id.in_(known_entities),
            or_(FoundationFact.object_entity_id.is_(None), FoundationFact.object_entity_id.in_(known_entities)))
    else:
        status_value = FoundationFact.status
    if status != "ALL":
        query = query.where(status_value == status)
    return query.order_by(FoundationFact.created_at.desc(), FoundationFact.id)


def page(db: Session, query, limit: int, offset: int, serialize) -> dict:
    total = int(db.scalar(select(func.count()).select_from(query.order_by(None).subquery())) or 0)
    rows = db.scalars(query.limit(limit).offset(offset)).all()
    return {"items": [serialize(row) for row in rows], "total": total, "limit": limit, "offset": offset}


def foundation_status(db: Session, enabled: bool) -> dict:
    if not enabled:
        return {"enabled": False, "counts": {}, "legacy_stock_count": None,
                "reviewer_is_authenticated_identity": False}
    counts = {}
    for key, model in (("entities", FoundationEntity), ("securities", FoundationSecurity), ("listings", FoundationListing),
                       ("evidence", FoundationEvidence), ("facts", FoundationFact)):
        counts[key] = int(db.scalar(select(func.count()).select_from(model)) or 0)
    for status in ("PENDING", "ACCEPTED", "REJECTED"):
        counts[f"{status.lower()}_facts"] = int(db.scalar(select(func.count()).select_from(FoundationFact)
                                                        .where(FoundationFact.status == status)) or 0)
    return {"enabled": enabled, "counts": counts,
            "legacy_stock_count": int(db.scalar(select(func.count()).select_from(StockSymbol)) or 0),
            "reviewer_is_authenticated_identity": False}
