"""Source-identified imports and bounded company/security relationship views."""

from collections import deque
from datetime import date, datetime, timedelta
import hashlib
import json
from typing import Any
from uuid import NAMESPACE_URL, uuid5

from sqlalchemy import func, or_, select, update
from sqlalchemy.orm import Session

from app.models.company_graph import SecurityClassification, SourceIdentity
from app.models.company_mapping import CompanyMappingState
from app.models.taxonomy import ClassificationDefinition
from app.models.foundation import FoundationEntity, FoundationEvidence, FoundationFact, FoundationFactEvidence, FoundationListing, FoundationSecurity
from app.models.market_data import StockSymbol
from app.schemas.company_graph import SecurityClassificationCreate, SourceBatch, SourceEntity
from app.schemas.foundation import EntityCreate, EvidenceCreate, FactCreate, FactReviewCreate, SecurityMappingCreate
from app.services import foundation as base
from app.services.graph_identity import canonical_company_id, canonical_security_id, IDENTITY_VERSION


FACT_GROUPS = {
    "ownership": {"HOLDS_EQUITY", "CONTROLS"},
    "industry": {"IN_INDUSTRY", "MEMBER_OF_THEME", "HAS_CLASSIFICATION"},
    "business": {"SUPPLIES_TO", "PARTNERS_WITH", "GUARANTEES", "LENDS_TO", "COMPETES_WITH", "CONTRACT"},
    "legal": {"LEGAL_CASE"},
}
DIMENSIONS = {
    "industry": {"IN_INDUSTRY"}, "theme": {"MEMBER_OF_THEME"},
    "ownership": {"HOLDS_EQUITY", "CONTROLS"}, "supply_chain": {"SUPPLIES_TO"},
    "business": {"PARTNERS_WITH", "GUARANTEES", "LENDS_TO", "COMPETES_WITH", "CONTRACT"},
    "legal": {"LEGAL_CASE"},
}


def _stable_id(*values: Any) -> str:
    return str(uuid5(NAMESPACE_URL, json.dumps(values, ensure_ascii=False, sort_keys=True, default=str)))


def _source_entity(db: Session, source: SourceEntity, source_name: str) -> FoundationEntity:
    jurisdiction = source.jurisdiction.upper()
    keys = []
    if source.source_issuer_id:
        keys.append((source_name, source.source_issuer_id))
    if source.identifier_value:
        keys.append((f"REGISTRY:{jurisdiction}:{source.identifier_scheme.upper()}", source.identifier_value))
    resolved = set()
    for namespace, value in keys:
        alias = db.scalar(select(SourceIdentity).where(SourceIdentity.namespace == namespace,
            SourceIdentity.external_id == value, SourceIdentity.entity_type == source.entity_type))
        if alias:
            if alias.jurisdiction != jurisdiction:
                raise base.FoundationError("source identifier has conflicting legal jurisdictions", 409)
            resolved.add(alias.entity_id)
    if source.identifier_value:
        matched = db.scalar(select(FoundationEntity).where(FoundationEntity.jurisdiction == jurisdiction,
            FoundationEntity.identifier_scheme == source.identifier_scheme.upper(),
            FoundationEntity.identifier_value == source.identifier_value))
        if matched:
            resolved.add(matched.id)
    if len(resolved) > 1:
        raise base.FoundationError("source and registration identifiers resolve to different entities; manual reconciliation required", 409)
    if resolved:
        row = base.required(db, FoundationEntity, next(iter(resolved)))
        if row.entity_type != source.entity_type:
            raise base.FoundationError("source identity conflicts with an existing entity type", 409)
        if source.identifier_value:
            registry_namespace = f"REGISTRY:{jurisdiction}:{source.identifier_scheme.upper()}"
            prior_aliases = list(db.scalars(select(SourceIdentity.external_id).where(
                SourceIdentity.entity_id == row.id, SourceIdentity.namespace == registry_namespace)))
            if (row.identifier_scheme == source.identifier_scheme.upper() and row.identifier_value != source.identifier_value) or any(
                value != source.identifier_value for value in prior_aliases
            ):
                raise base.FoundationError("source issuer identifier conflicts with the company's existing registration identifier", 409)
    else:
        scheme = source.identifier_scheme or "SOURCE:" + hashlib.sha256(source_name.encode()).hexdigest()[:24].upper()
        properties = {**source.properties_json, "identity_basis": "REGISTERED_IDENTIFIER" if source.identifier_value else "SOURCE_SCOPED_IDENTIFIER",
            "identity_source": source_name, "source_issuer_id": source.source_issuer_id}
        row = base.create_entity(db, EntityCreate(name=source.name, entity_type=source.entity_type, jurisdiction=jurisdiction,
            identifier_scheme=scheme, identifier_value=source.identifier_value or source.source_issuer_id, properties_json=properties))
    for namespace, value in keys:
        alias = db.scalar(select(SourceIdentity).where(SourceIdentity.namespace == namespace,
            SourceIdentity.external_id == value, SourceIdentity.entity_type == source.entity_type))
        if alias is None:
            db.add(SourceIdentity(entity_id=row.id, namespace=namespace, external_id=value,
                entity_type=source.entity_type, jurisdiction=jurisdiction))
    db.flush()
    return row


def create_classification(db: Session, payload: SecurityClassificationCreate) -> SecurityClassification:
    security = base.required(db, FoundationSecurity, payload.security_id)
    evidence = base.required(db, FoundationEvidence, payload.evidence_id)
    if evidence.entity_id and evidence.entity_id != security.entity_id:
        raise base.FoundationError("classification evidence belongs to another company")
    base._json(payload.properties_json)
    row_id = _stable_id("security-classification", payload.model_dump(mode="json"))
    existing = db.get(SecurityClassification, row_id)
    if existing:
        return existing
    now = base.utc_now()
    row = SecurityClassification(id=row_id, **payload.model_dump(), status="PENDING", reviews_json=[], created_at=now, updated_at=now)
    db.add(row)
    db.flush()
    return row


def classification_read(row: SecurityClassification, known_at: datetime | None = None) -> dict:
    result = {key: getattr(row, key) for key in ("id", "security_id", "evidence_id", "dimension", "code", "label",
        "definition_version", "method", "properties_json", "status", "valid_from", "valid_to", "created_at", "updated_at")}
    reviews = list(row.reviews_json or [])
    if known_at:
        reviews = [item for item in reviews if datetime.fromisoformat(item["created_at"]) <= known_at]
        result["status"] = reviews[-1]["decision"] if reviews else "PENDING"
        result["updated_at"] = datetime.fromisoformat(reviews[-1]["created_at"]) if reviews else row.created_at
    result["created_at"] = base.aware(result["created_at"])
    result["updated_at"] = base.aware(result["updated_at"])
    result["reviews"] = reviews
    return result


def review_classification(db: Session, record_id: str, payload: FactReviewCreate) -> SecurityClassification:
    row = base.required(db, SecurityClassification, record_id)
    if row.status != payload.expected_status or (row.status, payload.decision) not in {
        ("PENDING", "ACCEPTED"), ("PENDING", "REJECTED"), ("ACCEPTED", "REJECTED"),
    }:
        raise base.FoundationError("classification status changed or transition is invalid", 409)
    now = max(base.utc_now(), base.aware(row.updated_at) + timedelta(microseconds=1))
    reviews = [*(row.reviews_json or []), {"previous_status": row.status, "decision": payload.decision,
        "reason": payload.reason, "reviewer": payload.reviewer, "created_at": now.isoformat()}]
    changed = db.execute(update(SecurityClassification).where(SecurityClassification.id == row.id,
        SecurityClassification.status == payload.expected_status).values(status=payload.decision, reviews_json=reviews, updated_at=now)
        .execution_options(synchronize_session=False))
    if changed.rowcount != 1:
        raise base.FoundationError("classification changed; reload before reviewing", 409)
    db.flush()
    db.refresh(row)
    return row


def import_batch(db: Session, batch: SourceBatch | dict) -> dict:
    """Import normalized source records. Caller controls one atomic transaction."""
    batch = SourceBatch.model_validate(batch)
    results = []
    for record in batch.records:
        base._json(record.company.properties_json)
        company = _source_entity(db, record.company, record.source_name)
        stable_content = {"company_id": company.id, "source_kind": record.source_kind,
            "title": record.evidence.title, "content": record.evidence.content, "url": str(record.source_url or ""),
            "published_at": record.evidence.published_at.isoformat() if record.evidence.published_at else None,
            "company_profile": record.company.properties_json}
        source_fingerprint = hashlib.sha256(json.dumps(stable_content, sort_keys=True, ensure_ascii=False, allow_nan=False).encode()).hexdigest()
        proof = db.scalar(select(FoundationEvidence).where(FoundationEvidence.source_name == record.source_name,
            FoundationEvidence.source_key == record.source_key,
            FoundationEvidence.metadata_json["source_record_fingerprint"].as_string() == source_fingerprint))
        if proof is None:
            proof = base.create_evidence(db, EvidenceCreate(entity_id=company.id, source_name=record.source_name,
            source_key=record.source_key, title=record.evidence.title, content=record.evidence.content,
            url=record.source_url, published_at=record.evidence.published_at,
            available_at=record.evidence.available_at or record.observed_at), metadata={
                "content_scope": "SOURCE_RECORD", "completeness": "SOURCE_FIELDS_ONLY",
                "source_kind": record.source_kind, "first_observed_at": record.observed_at.isoformat(),
                "source_record_fingerprint": source_fingerprint,
                "company_profile": record.company.properties_json,
                "identity_basis": company.properties_json.get("identity_basis"),
                "identity_verification": "SOURCE_IDENTIFIERS_ONLY_NOT_REGISTRY_VERIFICATION",
            })
        mapping = base.create_mapping(db, SecurityMappingCreate(entity_id=company.id,
            stock_symbol_id=record.security.stock_symbol_id, share_class=record.security.share_class, evidence_id=proof.id))
        fact_ids = []
        for fact in record.facts:
            if fact.fact_type == "HAS_CLASSIFICATION":
                raise base.FoundationError("security classifications must use the classifications collection")
            obj = _source_entity(db, fact.object, record.source_name) if fact.object else None
            subject_id = obj.id if fact.direction == "INCOMING" and obj else company.id
            object_id = company.id if fact.direction == "INCOMING" else obj.id if obj else None
            if fact.direction == "INCOMING" and obj is None:
                raise base.FoundationError("incoming relation requires an identified counterparty")
            fact_id = _stable_id("source-fact", record.source_name, record.source_key, fact.model_dump(mode="json"),
                company.id, obj.id if obj else None, proof.id)
            if db.get(FoundationFact, fact_id) is None:
                base.create_fact(db, FactCreate(fact_type=fact.fact_type, title=fact.title, subject_entity_id=subject_id,
                    object_entity_id=object_id, properties_json={**fact.properties_json,
                        "source_record_key": record.source_key, "source_fact_key": fact.source_key,
                        "source_kind": record.source_kind}, evidence_ids=[proof.id], valid_from=fact.valid_from,
                    valid_to=fact.valid_to), fact_id=fact_id)
            fact_ids.append(fact_id)
        classification_ids = [create_classification(db, SecurityClassificationCreate(
            **item.model_dump(), security_id=mapping.security_id, evidence_id=proof.id)).id for item in record.classifications]
        results.append({"stock_symbol_id": record.security.stock_symbol_id, "company_id": company.id,
            "security_id": mapping.security_id, "listing_id": mapping.id, "evidence_id": proof.id,
            "fact_ids": fact_ids, "classification_ids": classification_ids, "source_name": record.source_name,
            "source_key": record.source_key, "identity_basis": company.properties_json.get("identity_basis")})
    return {"imported_records": len(results), "records": results, "candidate_only": True}


def _listing(db: Session, row: FoundationListing) -> dict:
    item = base.listing_read(db, row)
    company = base.required(db, FoundationEntity, item["entity_id"])
    return {**item, "listing_id": row.id, "company_id": company.id, "company_name": company.name, "mapping_status": "MAPPED",
        "canonical_security_id": canonical_security_id(row.market, row.symbol),
        "canonical_company_id": canonical_company_id(company.id), "identity_version": IDENTITY_VERSION}


def _classifications(db: Session, security_ids: list[str], as_of: date | None = None,
                     known_at: datetime | None = None) -> list[dict]:
    if not security_ids:
        return []
    query = select(SecurityClassification).join(FoundationEvidence,
        SecurityClassification.evidence_id == FoundationEvidence.id).where(SecurityClassification.security_id.in_(security_ids))
    if as_of:
        query = query.where(or_(SecurityClassification.valid_from.is_(None), SecurityClassification.valid_from <= as_of),
            or_(SecurityClassification.valid_to.is_(None), SecurityClassification.valid_to > as_of))
    if known_at:
        query = query.where(SecurityClassification.created_at <= known_at, FoundationEvidence.created_at <= known_at,
            FoundationEvidence.available_at <= known_at)
    rows = db.scalars(query.order_by(SecurityClassification.created_at.desc(), SecurityClassification.id)).all()
    definitions = db.scalars(select(ClassificationDefinition).where(ClassificationDefinition.status == "ACTIVE")).all()
    lookup = {(item.dimension, item.code, item.definition_version): item for item in definitions}
    output = []
    for row in rows:
        item = classification_read(row, known_at)
        definition = lookup.get((row.dimension, row.code, row.definition_version))
        if definition is None:
            # A provider label may be valid evidence without having a registered
            # semantic definition; expose that gap instead of inventing meaning.
            item["term_definition"] = {"status": "UNREGISTERED", "message": "术语定义尚未登记"}
        else:
            item["term_definition"] = {
                "status": definition.status, "id": definition.id, "label": definition.label,
                "definition": definition.definition, "criteria": definition.criteria,
                "source_name": definition.source_name, "source_url": definition.source_url,
                "definition_version": definition.definition_version,
            }
        output.append(item)
    return output


def coverage(db: Session, company_id: str | None, security_ids: list[str], *, as_of: date | None = None,
             known_at: datetime | None = None) -> dict:
    dimensions = []

    def add(key, count, pending_count=0, evidence_count=0):
        dimensions.append({"key": key, "status": "PRESENT" if count else "PENDING" if pending_count else "NOT_COLLECTED",
            "count": int(count), "pending_count": int(pending_count), "evidence_count": int(evidence_count)})

    add("identity", 1 if company_id else 0)
    add("securities", len(security_ids))
    evidence_query = select(func.count()).select_from(FoundationEvidence).where(FoundationEvidence.entity_id == company_id)
    if known_at:
        evidence_query = evidence_query.where(FoundationEvidence.created_at <= known_at, FoundationEvidence.available_at <= known_at)
    evidence_count = int(db.scalar(evidence_query) or 0) if company_id else 0
    classifications = _classifications(db, security_ids, as_of, known_at)
    disclosure_query = select(FoundationEvidence.metadata_json).where(FoundationEvidence.entity_id == company_id)
    if known_at:
        disclosure_query = disclosure_query.where(FoundationEvidence.created_at <= known_at, FoundationEvidence.available_at <= known_at)
    disclosures = [{"metadata_json": metadata} for metadata in db.scalars(disclosure_query)] if company_id else []
    for key, types in DIMENSIONS.items():
        counts = []
        id_queries = []
        for status in ("ACCEPTED", "PENDING"):
            query = base.facts_query(entity_id=company_id, status=status, as_of=as_of, known_at=known_at).where(FoundationFact.fact_type.in_(types))
            counts.append(int(db.scalar(select(func.count()).select_from(query.order_by(None).subquery())) or 0) if company_id else 0)
            id_queries.append(query.with_only_columns(FoundationFact.id).order_by(None))
        proofs = int(db.scalar(select(func.count(func.distinct(FoundationFactEvidence.evidence_id))).where(or_(
            FoundationFactEvidence.fact_id.in_(id_queries[0]), FoundationFactEvidence.fact_id.in_(id_queries[1])))) or 0) if company_id else 0
        if key in {"theme", "industry"}:
            matching = [row for row in classifications if row["dimension"] == key.upper()]
            counts[0] += sum(row["status"] == "ACCEPTED" for row in matching)
            counts[1] += sum(row["status"] == "PENDING" for row in matching)
            proofs += len({row["evidence_id"] for row in matching if row["status"] in {"ACCEPTED", "PENDING"}})
        add(key, *counts, evidence_count=proofs)
        kind = {"supply_chain": "SUPPLY_CHAIN_DISCLOSURE", "business": "BUSINESS_DISCLOSURE", "legal": "LEGAL_DISCLOSURE"}.get(key)
        if kind:
            dimension = dimensions[-1]
            dimension["disclosure_count"] = sum(_document_has_kind(row, kind, known_at) for row in disclosures)
            dimension["disclosure_status"] = "EVIDENCE_ONLY" if dimension["disclosure_count"] else "NOT_COLLECTED"
    add("classification", sum(row["status"] == "ACCEPTED" for row in classifications),
        sum(row["status"] == "PENDING" for row in classifications), len({row["evidence_id"] for row in classifications}))
    add("evidence", evidence_count, evidence_count=evidence_count)
    return {"dimensions": dimensions, "missing": [item["key"] for item in dimensions if item["status"] == "NOT_COLLECTED"],
        "pending": [item["key"] for item in dimensions if item["pending_count"]],
        "scope": "BUSINESS_DATE_FILTERED" if as_of else "ALL_RECORDED_PERIODS", "as_of": as_of,
        "absence_is_not_negative_fact": True}


def search(db: Session, *, mode: str = "security", q: str = "", market: str | None = None,
           mapped_only: bool = False, limit: int = 20, offset: int = 0) -> dict:
    if mode == "company":
        query = select(FoundationEntity).where(FoundationEntity.entity_type == "COMPANY")
        if q:
            query = query.where(or_(FoundationEntity.name.contains(q, autoescape=True), FoundationEntity.identifier_value.contains(q, autoescape=True)))
        def serialize_company(row):
            listings = db.scalars(select(FoundationListing).join(FoundationSecurity).where(FoundationSecurity.entity_id == row.id)).all()
            return {**base.entity_read(row), "company_id": row.id, "securities": [_listing(db, item) for item in listings]}
        return base.page(db, query.order_by(FoundationEntity.name, FoundationEntity.id), limit, offset, serialize_company)
    query = select(StockSymbol)
    if q:
        compact_name = func.replace(func.replace(StockSymbol.name, " ", ""), "　", "")
        compact_query = q.replace(" ", "").replace("　", "")
        query = query.where(or_(StockSymbol.symbol.contains(q, autoescape=True), StockSymbol.name.contains(q, autoescape=True),
            compact_name.contains(compact_query, autoescape=True)))
    if market:
        query = query.where(StockSymbol.market == market)
    if mapped_only:
        query = query.where(StockSymbol.id.in_(select(FoundationListing.stock_symbol_id)))
    total = int(db.scalar(select(func.count()).select_from(query.subquery())) or 0)
    stocks = list(db.scalars(query.order_by(StockSymbol.market, StockSymbol.symbol, StockSymbol.id).limit(limit).offset(offset)))
    # Search rows contain identity only. Coverage loads once for the selected company,
    # rather than extracting thousands of PDF pages for every search result.
    mappings = list(db.execute(select(FoundationListing, FoundationSecurity, FoundationEntity)
        .join(FoundationSecurity, FoundationListing.security_id == FoundationSecurity.id)
        .join(FoundationEntity, FoundationSecurity.entity_id == FoundationEntity.id)
        .where(FoundationListing.stock_symbol_id.in_([row.id for row in stocks]))))
    by_stock = {listing.stock_symbol_id: (listing, security, entity) for listing, security, entity in mappings}
    outcomes = {row.stock_symbol_id: row for row in db.scalars(select(CompanyMappingState)
        .where(CompanyMappingState.stock_symbol_id.in_([row.id for row in stocks])))}
    items = []
    for stock in stocks:
        mapped = by_stock.get(stock.id)
        outcome = outcomes.get(stock.id)
        item = {"stock_symbol_id": stock.id, "market": stock.market, "symbol": stock.symbol, "name": stock.name,
            "listing_id": None, "security_id": None, "company_id": None, "company_name": None,
            "mapping_status": outcome.status if outcome else "UNMAPPED", "mapping_reason": outcome.reason if outcome else "尚未建立发行主体映射",
            "canonical_security_id": canonical_security_id(stock.market, stock.symbol),
            "canonical_company_id": None, "identity_version": IDENTITY_VERSION}
        if mapped:
            listing, security, entity = mapped
            item.update(listing_id=listing.id, security_id=security.id, company_id=entity.id, company_name=entity.name,
                mapping_status="MAPPED", mapping_reason="已建立证券与发行主体映射", share_class=security.share_class,
                canonical_company_id=canonical_company_id(entity.id))
        else:
            item["coverage"] = {"dimensions": [], "missing": ["identity", "securities"], "pending": []}
        items.append(item)
    return {"items": items, "total": total, "limit": limit, "offset": offset}


def _security_item(db: Session, row: StockSymbol) -> dict:
    mapped = db.scalar(select(FoundationListing).where(FoundationListing.stock_symbol_id == row.id))
    if mapped:
        result = _listing(db, mapped)
        result["coverage"] = coverage(db, result["company_id"], [mapped.security_id])
        return result
    return {"stock_symbol_id": row.id, "listing_id": None, "security_id": None, "company_id": None,
        "name": row.name, "symbol": row.symbol, "market": row.market, "company_name": None,
        "mapping_status": "UNMAPPED", "coverage": coverage(db, None, [])}


def samples(db: Session, *, q: str = "", market: str | None = None, limit: int = 30, offset: int = 0) -> dict:
    from app.services.company_sample import SAMPLE_BATCH, SAMPLE_SECURITIES

    expected = [item for item in SAMPLE_SECURITIES if not market or item["market"] == market]
    pairs = {(item["market"], item["symbol"]) for item in expected}
    stocks = list(db.scalars(select(StockSymbol).where(or_(*[
        (StockSymbol.market == venue) & (StockSymbol.symbol == symbol) for venue, symbol in pairs
    ])))) if pairs else []
    indexed = {(row.market, row.symbol): row for row in stocks}
    filtered = []
    for item in expected:
        row = indexed.get((item["market"], item["symbol"]))
        if q and q.casefold() not in (item["symbol"] + " " + (row.name if row else "")).casefold():
            continue
        filtered.append((item, row))
    items = []
    for item, row in filtered[offset:offset + limit]:
        if row:
            items.append(_security_item(db, row))
        else:
            items.append({"stock_symbol_id": None, "listing_id": None, "security_id": None, "company_id": None,
                "name": item["symbol"], "symbol": item["symbol"], "market": item["market"], "company_name": None,
                "mapping_status": "STOCK_MISSING", "coverage": coverage(db, None, [])})
    return {"items": items, "total": len(filtered), "limit": limit, "offset": offset,
        "sample_batch": SAMPLE_BATCH, "sample_size": len(SAMPLE_SECURITIES), "scope": "FIXED_ACCEPTANCE_SAMPLE"}


def company_profile(db: Session, company_id: str, *, as_of: date | None = None, known_at: datetime | None = None) -> dict:
    company = base.required(db, FoundationEntity, company_id)
    if company.entity_type != "COMPANY" or (known_at and base.aware(company.created_at) > known_at):
        raise base.FoundationError("company is not available at the requested time", 404)
    query = select(FoundationListing).join(FoundationSecurity).where(FoundationSecurity.entity_id == company_id)
    if known_at:
        query = query.where(FoundationListing.created_at <= known_at)
    listings = list(db.scalars(query.order_by(FoundationListing.market, FoundationListing.symbol)))
    securities = [_listing(db, row) for row in listings]
    fact_rows = list(db.scalars(base.facts_query(entity_id=company_id, status="ALL", as_of=as_of, known_at=known_at).limit(201)))
    groups = {key: [] for key in (*FACT_GROUPS, "other")}
    for row in fact_rows[:200]:
        group = next((key for key, types in FACT_GROUPS.items() if row.fact_type in types), "other")
        groups[group].append(base.fact_read(db, row, known_at=known_at))
    classifications = _classifications(db, [row.security_id for row in listings], as_of, known_at)
    fact_evidence_ids = select(FoundationFactEvidence.evidence_id).where(FoundationFactEvidence.fact_id.in_([row.id for row in fact_rows[:200]]))
    query = select(FoundationEvidence).where(or_(FoundationEvidence.entity_id == company_id,
        FoundationEvidence.id.in_(fact_evidence_ids), FoundationEvidence.id.in_([row["evidence_id"] for row in classifications])))
    if known_at:
        query = query.where(FoundationEvidence.created_at <= known_at, FoundationEvidence.available_at <= known_at)
    evidence_rows = list(db.scalars(query.order_by(FoundationEvidence.created_at.desc(), FoundationEvidence.id).limit(101)))
    evidence = [base.evidence_read(row) for row in evidence_rows[:100]]
    company_data = base.entity_read(company)
    company_data["canonical_company_id"] = canonical_company_id(company.id)
    profile_evidence = next((row for row in evidence_rows[:100] if row.entity_id == company_id
        and row.metadata_json.get("company_profile")), None)
    if profile_evidence:
        company_data["properties_json"] = {**company_data["properties_json"], **profile_evidence.metadata_json["company_profile"]}
    fact_dates = [item["updated_at"] for group in groups.values() for item in group]
    dates = [base.aware(company.created_at), *fact_dates, *[base.aware(row.created_at) for row in evidence_rows[:100]]]
    return {"company": company_data, "profile_evidence_id": profile_evidence.id if profile_evidence else None,
        "securities": securities, "facts": groups,
        "classifications": classifications, "evidence": evidence,
        "legal_disclosures": [{**item, "association_role": "DISCLOSING_COMPANY_NOT_CONFIRMED_CASE_PARTY"}
            for item in evidence if _document_has_kind(item, "LEGAL_DISCLOSURE", known_at)],
        "business_disclosures": [{**item, "association_role": "DISCLOSING_COMPANY_NOT_VERIFIED_TRANSACTION_PARTY"}
            for item in evidence if _document_has_kind(item, "BUSINESS_DISCLOSURE", known_at)],
        "supply_chain_disclosures": [{**item, "association_role": "DISCLOSING_COMPANY_NOT_VERIFIED_TRANSACTION_PARTY"}
            for item in evidence if _document_has_kind(item, "SUPPLY_CHAIN_DISCLOSURE", known_at)],
        "coverage": coverage(db, company_id, [row.security_id for row in listings], as_of=as_of, known_at=known_at),
        "sources": sorted({row.source_name for row in evidence_rows[:100]}), "updated_at": max(dates),
        "truncated": {"facts": len(fact_rows) > 200, "evidence": len(evidence_rows) > 100},
        "as_of": as_of, "known_at": known_at}


def _document_has_kind(evidence: dict, kind: str, known_at: datetime | None) -> bool:
    metadata = evidence.get("metadata_json") or {}
    assignments = metadata.get("document_kind_assignments")
    if known_at and assignments:
        return any(item["kind"] == kind and datetime.fromisoformat(item["recorded_at"]) <= known_at for item in assignments)
    return kind in (metadata.get("document_kinds") or [metadata.get("document_kind")])


def security_profile(db: Session, stock_symbol_id: int, **filters) -> dict:
    mapping = db.scalar(select(FoundationListing).where(FoundationListing.stock_symbol_id == stock_symbol_id))
    if not mapping:
        outcome = db.get(CompanyMappingState, stock_symbol_id)
        reason = f"：{outcome.reason}" if outcome else "，需要先补充发行主体资料"
        raise base.FoundationError("该证券尚无公司映射" + reason, 404)
    selected = _listing(db, mapping)
    known_at = filters.get("known_at")
    if known_at and base.aware(mapping.created_at) > known_at:
        raise base.FoundationError("mapping was not known at the requested time", 404)
    result = company_profile(db, selected["company_id"], **filters)
    result["selected_security"] = selected
    result["coverage"] = coverage(db, selected["company_id"], [mapping.security_id], **filters)
    return result


def status(db: Session) -> dict:
    def count(model, condition=None):
        query = select(func.count()).select_from(model)
        return int(db.scalar(query.where(condition) if condition is not None else query) or 0)
    return {"entity_count": count(FoundationEntity), "security_count": count(FoundationSecurity),
        "listing_count": count(FoundationListing), "accepted_fact_count": count(FoundationFact, FoundationFact.status == "ACCEPTED"),
        "pending_fact_count": count(FoundationFact, FoundationFact.status == "PENDING"),
        "classification_count": count(SecurityClassification), "scope": "MAPPED_SECURITIES", "data_mode": "DATABASE"}


def graph(db: Session, *, company_id: str | None = None, stock_symbol_id: int | None = None,
          depth: int = 2, max_nodes: int = 80, max_edges: int = 150, include_pending: bool = False,
          as_of: date | None = None, known_at: datetime | None = None) -> dict:
    if bool(company_id) == bool(stock_symbol_id):
        raise base.FoundationError("provide exactly one company_id or stock_symbol_id")
    mapping = None
    if stock_symbol_id:
        mapping = db.scalar(select(FoundationListing).where(FoundationListing.stock_symbol_id == stock_symbol_id))
        if not mapping or (known_at and base.aware(mapping.created_at) > known_at):
            raise base.FoundationError("security mapping is unavailable", 404)
        company_id = base.required(db, FoundationSecurity, mapping.security_id).entity_id
    root = base.required(db, FoundationEntity, company_id)
    if root.entity_type != "COMPANY" or (known_at and base.aware(root.created_at) > known_at):
        raise base.FoundationError("company is unavailable", 404)
    nodes = {root.id: {"id": root.id, "label": root.name, "type": root.entity_type,
        "canonical_company_id": canonical_company_id(root.id)}}
    edges = {}
    paths = []
    truncated = False

    def add_edge(edge: dict, additions: list[dict]) -> bool:
        nonlocal truncated
        if edge["id"] in edges:
            return True
        absent = {node["id"] for node in additions if node["id"] not in nodes}
        if len(edges) >= max_edges or len(nodes) + len(absent) > max_nodes:
            truncated = True
            return False
        for node in additions:
            nodes[node["id"]] = node
        edges[edge["id"]] = edge
        return True

    def add_listing(listing):
        if known_at and base.aware(listing.created_at) > known_at:
            return False
        issuer = base.required(db, FoundationSecurity, listing.security_id).entity_id
        return add_edge({"id": "issuer:" + listing.id, "source": listing.security_id, "target": issuer,
            "type": "ISSUED_BY", "status": "ACCEPTED", "evidence_ids": [listing.evidence_id]},
            [{"id": listing.security_id, "label": f"{listing.market}:{listing.symbol} {listing.name}", "type": "SECURITY",
                "canonical_security_id": canonical_security_id(listing.market, listing.symbol),
                "canonical_company_id": canonical_company_id(issuer), "company_id": issuer,
                "stock_symbol_id": listing.stock_symbol_id, "identity_version": IDENTITY_VERSION}])

    if mapping:
        add_listing(mapping)
    queue = deque([(root.id, 0, [root.id], [])])
    visited = {root.id}
    statuses = {"ACCEPTED", "PENDING"} if include_pending else {"ACCEPTED"}
    while queue and len(edges) < max_edges:
        entity_id, distance, node_path, edge_path = queue.popleft()
        if distance >= depth:
            continue
        rows = list(db.scalars(base.facts_query(entity_id=entity_id, status="ALL" if include_pending else "ACCEPTED",
            as_of=as_of, known_at=known_at).limit(max_edges + 1)))
        if len(rows) > max_edges:
            truncated = True
        for row in rows[:max_edges]:
            current_status = base.status_at(db, row, known_at)
            if current_status not in statuses:
                continue
            subject = base.required(db, FoundationEntity, row.subject_entity_id)
            if row.object_entity_id:
                obj = base.required(db, FoundationEntity, row.object_entity_id)
                object_id, label, node_type = obj.id, obj.name, obj.entity_type
            else:
                object_id, label, node_type = "fact:" + row.id, row.title, row.fact_type
            edge = {"id": row.id, "source": subject.id, "target": object_id, "type": row.fact_type,
                "status": current_status, "fact_id": row.id, "evidence_ids": [item.id for item in base.fact_evidence(db, row.id)],
                "valid_from": row.valid_from, "valid_to": row.valid_to,
                # Preserve typed relationship details on graph edges.  The UI can
                # show a disclosed holding ratio without guessing from a title.
                "properties_json": row.properties_json or {}}
            if not add_edge(edge, [{"id": subject.id, "label": subject.name, "type": subject.entity_type},
                                   {"id": object_id, "label": label, "type": node_type}]):
                continue
            other = object_id if entity_id == subject.id else subject.id
            if other not in visited:
                visited.add(other)
                paths.append({"node_ids": [*node_path, other], "edge_ids": [*edge_path, row.id]})
                if row.object_entity_id:
                    queue.append((other, distance + 1, [*node_path, other], [*edge_path, row.id]))
    for entity_id in list(nodes):
        listings = list(db.scalars(select(FoundationListing).join(FoundationSecurity).where(FoundationSecurity.entity_id == entity_id)))
        for listing in listings:
            if not add_listing(listing):
                continue
            for item in _classifications(db, [listing.security_id], as_of, known_at):
                if item["status"] not in statuses:
                    continue
                tag_id = "classification:" + _stable_id(item["dimension"], item["code"], item["definition_version"], item["method"])
                add_edge({"id": item["id"], "source": listing.security_id, "target": tag_id,
                    "type": "HAS_CLASSIFICATION", "status": item["status"], "evidence_ids": [item["evidence_id"]],
                    "valid_from": item["valid_from"], "valid_to": item["valid_to"]},
                    [{"id": tag_id, "label": item["label"], "type": "CLASSIFICATION"}])
    if mapping:
        for path in paths:
            path["node_ids"].insert(0, mapping.security_id)
            path["edge_ids"].insert(0, "issuer:" + mapping.id)
    for node in nodes.values():
        if node["type"] == "COMPANY":
            node["canonical_company_id"] = canonical_company_id(node["id"])
            node["identity_version"] = IDENTITY_VERSION
    return {"root_id": mapping.security_id if mapping else root.id,
        "nodes": list(nodes.values()), "edges": list(edges.values()), "paths": paths,
        "truncated": truncated or bool(queue), "depth": depth, "depth_scope": "ENTITY_RELATIONSHIP_HOPS",
        "as_of": as_of, "known_at": known_at, "include_pending": include_pending}
