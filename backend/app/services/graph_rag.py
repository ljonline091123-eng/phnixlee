"""Bounded, versioned context for model-assisted stock analysis.

The online tables and the legacy graph remain the source of truth.  This
module only projects records that were available at ``as_of`` into a compact
context contract.  It deliberately does not call a model and it never writes
entities or facts.  Callers can therefore use the result as an auditable
GraphRAG boundary for LLA/agent prompts.
"""

from __future__ import annotations

import hashlib
import json
import ast
import calendar
from datetime import date, datetime, timezone
from typing import Any, Iterable
from zoneinfo import ZoneInfo

from sqlalchemy import and_, func, or_, select
from sqlalchemy.orm import Session

from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, KnowledgeEntity, KnowledgeGraph, KnowledgeRelation
from app.models.foundation import (
    FoundationEntity,
    FoundationFact,
    FoundationFactEvidence,
    FoundationEvidence,
    FoundationListing,
    FoundationSecurity,
)
from app.models.lakehouse import DocumentChunkVersion, LakeDataset, LakeDatasetVersion, LakeLineageEvent, LakeObject
from app.models.market_data import (
    StockF10Cache,
    StockFinancialReport,
    StockKline,
    StockRealtimeQuote,
    StockSymbol,
)
from app.models.research_data import (
    StockBrokerResearchReport,
    StockEarningsConsensus,
    StockInstitutionForecast,
    StockInvestorQA,
)
from app.services.graph_identity import canonical_company_id, canonical_security_id, resolve_many
from app.services.foundation import facts_query
from app.services.lakehouse import current_knowledge_document_chunks, knowledge_document_source_hash
from app.services.news_identity import check_news_identity


CONTEXT_VERSION = "GRAPH_RAG_CONTEXT_V5"


def _iso(value: Any) -> str | None:
    if value is None:
        return None
    if isinstance(value, (datetime, date)):
        return value.isoformat()
    return str(value)


def _aware(value: datetime | None) -> datetime | None:
    if value is None:
        return None
    return value.replace(tzinfo=timezone.utc) if value.tzinfo is None else value


def _known(value: Any, cutoff: datetime | None) -> bool:
    """Apply a conservative availability gate without assuming DB tz support."""
    if cutoff is None or value is None:
        return True
    if isinstance(value, str):
        try:
            value = datetime.fromisoformat(value.replace("Z", "+00:00"))
        except ValueError:
            return True
    if isinstance(value, date) and not isinstance(value, datetime):
        value = datetime.combine(value, datetime.min.time(), tzinfo=timezone.utc)
    if not isinstance(value, datetime):
        return True
    left, right = _aware(value), _aware(cutoff)
    return bool(left and right and left <= right)


def _date_known(value: Any, cutoff: datetime | None) -> bool:
    """Apply a calendar-date cutoff for exchange observations."""
    if cutoff is None or value in (None, ""):
        return True
    try:
        observed = date.fromisoformat(str(value)[:10])
    except ValueError:
        return True
    return observed <= cutoff.date()


def selection_period_bounds(as_of: datetime, period_months: int, market: str | None = None) -> tuple[date, date]:
    """Return a rolling calendar-month window while preserving month ends."""
    months = max(1, min(int(period_months), 60))
    aware_as_of = _aware(as_of)
    market_zones = {
        "CN_A": "Asia/Shanghai", "NEEQ": "Asia/Shanghai", "NEEQ_INNOVATION": "Asia/Shanghai",
        "HK": "Asia/Hong_Kong", "HKEX": "Asia/Hong_Kong",
    }
    zone_name = market_zones.get(str(market or "").upper())
    localized = aware_as_of.astimezone(ZoneInfo(zone_name)) if aware_as_of and zone_name else aware_as_of
    end = localized.date() if localized else as_of.date()
    month_index = end.year * 12 + end.month - 1 - months
    year, zero_based_month = divmod(month_index, 12)
    month = zero_based_month + 1
    day = min(end.day, calendar.monthrange(year, month)[1])
    return date(year, month, day), end


def _date_in_window(value: Any, start: date | None, end: date | None) -> bool:
    if start is None and end is None:
        return True
    if value in (None, ""):
        return False
    text = str(value).strip()
    try:
        observed = (datetime.strptime(text[:8], "%Y%m%d").date()
                    if len(text) >= 8 and text[:8].isdigit()
                    else date.fromisoformat(text[:10]))
    except ValueError:
        return False
    return (start is None or observed >= start) and (end is None or observed <= end)


def _hash(value: Any) -> str:
    payload = json.dumps(value, ensure_ascii=False, sort_keys=True, default=str, separators=(",", ":"))
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def _unique(values: Iterable[Any]) -> list[Any]:
    return list(dict.fromkeys(value for value in values if value not in (None, "")))


def _compact(value: Any, *, max_items: int = 24, max_chars: int = 1600) -> Any:
    """Keep source payloads useful to an LLA without copying whole raw rows."""
    if isinstance(value, str):
        return value if len(value) <= max_chars else value[:max_chars] + "..."
    if isinstance(value, dict):
        return {str(key): _compact(item, max_items=max_items, max_chars=max_chars)
                for key, item in list(value.items())[:max_items]}
    if isinstance(value, (list, tuple)):
        values = [_compact(item, max_items=max_items, max_chars=max_chars) for item in value[:max_items]]
        return values + ([f"... 共 {len(value)} 项"] if len(value) > max_items else [])
    return value


def _relation_provenance(
    row: KnowledgeRelation,
    subject: KnowledgeEntity | None,
    target: KnowledgeEntity | None,
    document: KnowledgeDocument | None,
) -> dict[str, Any]:
    """Expose conservative provenance for legacy relations without inventing facts."""
    sources: list[dict[str, Any]] = []
    for entity in (target, subject):
        properties = entity.properties_json if entity and isinstance(entity.properties_json, dict) else {}
        sources.append(properties)
        nested = properties.get("properties")
        if isinstance(nested, dict):
            sources.append(nested)
    if document and isinstance(document.metadata_json, dict):
        sources.append(document.metadata_json)

    def first(*keys: str) -> Any:
        for source in sources:
            for key in keys:
                value = source.get(key)
                if value not in (None, ""):
                    return value
        return None

    candidate = (
        str(row.predicate).startswith("MENTIONS_")
        or any(entity and entity.entity_type in {"MENTIONED_EVENT", "CANDIDATE_EVENT"}
               for entity in (subject, target))
    )
    raw_status = str(first("verification_status", "review_status", "status") or "").upper()
    if candidate or raw_status in {"PENDING", "PENDING_REVIEW", "CANDIDATE", "RAW"}:
        status = "CANDIDATE"
    elif raw_status in {"ACCEPTED", "VERIFIED", "GOVERNED", "LOCKED"}:
        status = "VERIFIED"
    elif raw_status:
        status = raw_status
    else:
        status = "SOURCE" if row.evidence_document_id else "OBSERVED"

    method = first("extraction_method", "classification_method")
    if method in (None, ""):
        method = "KEYWORD_MENTION" if candidate else ("SOURCE" if row.evidence_document_id else "SYSTEM")
    confidence = first("extraction_confidence", "confidence")
    return {
        "verification_status": status,
        "extraction_method": method,
        "confidence": confidence,
        "extraction_confidence": confidence,
        "source_reliability": first("source_reliability"),
    }


def document_content_state(content: str) -> str:
    if not content.strip():
        return "EMPTY"
    payload_start = content.find("{")
    if payload_start >= 0 and any(marker in content for marker in ("adjunctUrl", "disclosure_entry", "announcementContent")):
        try:
            payload = ast.literal_eval(content[payload_start:])
        except (ValueError, SyntaxError, RecursionError):
            payload = {}
        if not any(isinstance(payload.get(key), str) and payload[key].strip()
                   for key in ("announcementContent", "content", "text", "body", "governance_full_text")):
            return "METADATA_ONLY"
    return "TEXT_AVAILABLE"


def ranked_documents(db: Session, query, *, per_source: int = 12) -> list[KnowledgeDocument]:
    """Rank by business publication time inside each source, never import order.

    A large price/history source must not hide all news before source balancing.
    The JSON expression compiles for SQLite and PostgreSQL through SQLAlchemy.
    """
    published = func.coalesce(*(KnowledgeDocument.metadata_json[key].as_string()
        for key in ("published_at", "news_time", "notice_date", "report_period", "trade_date")))
    ranked = query.with_only_columns(KnowledgeDocument.id, func.row_number().over(
        partition_by=KnowledgeDocument.source_table,
        order_by=(func.coalesce(published, "").desc(), KnowledgeDocument.updated_at.desc(), KnowledgeDocument.id.desc()),
    ).label("position")).subquery()
    return list(db.scalars(select(KnowledgeDocument).join(ranked, ranked.c.id == KnowledgeDocument.id)
        .where(ranked.c.position <= per_source).order_by(ranked.c.position, KnowledgeDocument.source_table)).all())


def _doc_payload(row: KnowledgeDocument, *, include_content: bool = True) -> dict[str, Any]:
    metadata = row.metadata_json or {}
    content = row.content or ""
    return {
        "id": row.id,
        "title": row.title,
        "source_table": row.source_table,
        "source_record_id": row.source_record_id,
        "market": row.market,
        "symbol": row.symbol,
        "graph_id": row.graph_id,
        "knowledge_base_id": row.knowledge_base_id,
        "content": content[:3000] if include_content else None,
        "content_state": document_content_state(content),
        "content_truncated": len(content) > 3000 or bool(metadata.get("content_truncated")),
        "content_hash": _hash(content),
        "metadata": metadata,
        "source_name": next((metadata[key] for key in ("source_name", "media_name", "source", "provider")
                             if metadata.get(key)), None),
        "published_at": next((metadata[key] for key in ("published_at", "news_time", "notice_date", "report_period", "trade_date") if metadata.get(key)), None),
        "url": metadata.get("url"),
        "fetched_at": metadata.get("fetched_at"),
        "updated_at": _iso(row.updated_at),
    }


def _identity(db: Session, market: str, symbol: str, cutoff: datetime | None) -> dict[str, Any]:
    pair = resolve_many(db, [(market, symbol)], known_at=cutoff).get((market.upper(), symbol.upper()), {})
    result = {
        "canonical_security_id": canonical_security_id(market, symbol),
        "canonical_company_id": pair.get("canonical_company_id"),
        "mapping_status": pair.get("mapping_status", "UNMAPPED"),
        "stock_symbol_id": pair.get("stock_symbol_id"),
        "security_id": pair.get("security_id"),
        "company_id": pair.get("company_id"),
        "listing_id": pair.get("listing_id"),
        "identity_version": pair.get("identity_version"),
    }
    stock = db.scalar(select(StockSymbol).where(StockSymbol.market == market, StockSymbol.symbol == symbol))
    if stock is not None:
        result["security"] = {
            "id": stock.id,
            "market": stock.market,
            "symbol": stock.symbol,
            "name": stock.name,
            "exchange": stock.exchange,
            "asset_type": stock.asset_type,
            "status": stock.status,
            "list_date": stock.list_date,
            "last_synced_at": _iso(stock.last_synced_at),
        }
    company_id = pair.get("company_id")
    if company_id:
        company = db.get(FoundationEntity, company_id)
        if company is not None and _known(company.created_at, cutoff):
            result["company"] = {
                "id": company.id,
                "canonical_id": canonical_company_id(company.id),
                "name": company.name,
                "entity_type": company.entity_type,
                "jurisdiction": company.jurisdiction,
                "identifier_scheme": company.identifier_scheme,
                "identifier_value": company.identifier_value,
                "properties": company.properties_json or {},
            }
    return result


def _related_security_documents(
    db: Session,
    *,
    company_id: str | None,
    market: str,
    symbol: str,
    kb_ids: list[int],
    graph_ids: list[int],
    cutoff: datetime,
    period_start: date | None,
    period_end: date | None,
    max_documents: int = 12,
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    """Load disclosures from securities connected by reviewed company facts.

    Sector membership is intentionally not used here. A related security may
    enter the selection prompt only when an ACCEPTED FoundationFact connects
    the two issuer entities and both entities have canonical listings.
    """
    if not company_id or max_documents <= 0:
        return [], []
    relation_facts = list(db.scalars(facts_query(
        status="ACCEPTED", entity_id=company_id, as_of=cutoff.date(), known_at=cutoff,
    ).limit(120)).all())
    related_fact_ids: dict[str, list[str]] = {}
    related_fact_details: dict[str, list[dict[str, Any]]] = {}
    for fact in relation_facts:
        other_id = fact.object_entity_id if fact.subject_entity_id == company_id else fact.subject_entity_id
        if other_id and other_id != company_id:
            related_fact_ids.setdefault(other_id, []).append(fact.id)
            related_fact_details.setdefault(other_id, []).append({
                "fact_id": fact.id,
                "fact_type": fact.fact_type,
                "title": fact.title,
                "subject_entity_id": fact.subject_entity_id,
                "object_entity_id": fact.object_entity_id,
                "properties": _compact(fact.properties_json or {}),
                "status": "ACCEPTED",
                "valid_from": _iso(fact.valid_from),
                "valid_to": _iso(fact.valid_to),
            })
    if not related_fact_ids:
        return [], []

    listing_rows = list(db.execute(select(
        FoundationListing, FoundationSecurity.entity_id,
    ).join(
        FoundationSecurity, FoundationSecurity.id == FoundationListing.security_id,
    ).where(
        FoundationSecurity.entity_id.in_(related_fact_ids),
        FoundationListing.created_at <= cutoff,
        FoundationSecurity.created_at <= cutoff,
    )).all())
    pairs = {(listing.market, listing.symbol) for listing, _entity_id in listing_rows
             if (listing.market, listing.symbol) != (market, symbol)}
    if not pairs:
        return [], []
    masters = {(row.market, row.symbol): row for row in db.scalars(select(StockSymbol).where(
        or_(*[and_(StockSymbol.market == pair[0], StockSymbol.symbol == pair[1]) for pair in pairs]),
    )).all()}
    securities = []
    pair_metadata: dict[tuple[str, str], dict[str, Any]] = {}
    for listing, entity_id in listing_rows:
        pair = (listing.market, listing.symbol)
        if pair not in pairs or pair in pair_metadata:
            continue
        master = masters.get(pair)
        metadata = {
            "market": listing.market,
            "symbol": listing.symbol,
            "name": master.name if master else listing.name,
            "company_id": entity_id,
            "related_via_fact_ids": sorted(set(related_fact_ids.get(entity_id, []))),
            "related_via_facts": related_fact_details.get(entity_id, []),
        }
        pair_metadata[pair] = metadata
        securities.append(metadata)

    doc_query = select(KnowledgeDocument).where(
        KnowledgeDocument.source_table.in_(("stock_news", "stock_notice")),
        KnowledgeDocument.created_at <= cutoff,
        KnowledgeDocument.updated_at <= cutoff,
        or_(*[
            and_(KnowledgeDocument.market == pair[0], KnowledgeDocument.symbol == pair[1])
            for pair in pair_metadata
        ]),
    )
    if kb_ids:
        doc_query = doc_query.where(KnowledgeDocument.knowledge_base_id.in_(kb_ids))
    if graph_ids:
        doc_query = doc_query.where(KnowledgeDocument.graph_id.in_(graph_ids))
    candidates = list(db.scalars(doc_query.order_by(
        KnowledgeDocument.updated_at.desc(), KnowledgeDocument.id.desc(),
    ).limit(max(200, max_documents * 12))).all())
    selected: list[dict[str, Any]] = []
    per_pair: dict[tuple[str, str], int] = {}
    for row in candidates:
        pair = (row.market, row.symbol)
        master = masters.get(pair)
        payload = _doc_payload(row)
        if pair not in pair_metadata or not _date_known(payload["published_at"], cutoff):
            continue
        if not _date_in_window(payload["published_at"], period_start, period_end):
            continue
        if row.source_table == "stock_news" and (
            master is None or check_news_identity(
                row.market, row.symbol, master.name, row.title, row.content, master.ext_json,
            )["status"] != "MENTION_MATCHED"
        ):
            continue
        if per_pair.get(pair, 0) >= 3:
            continue
        selected.append({
            **payload,
            "relation_scope": "RELATED_SECURITY",
            "related_security": pair_metadata[pair],
            "related_via_fact_ids": pair_metadata[pair]["related_via_fact_ids"],
        })
        per_pair[pair] = per_pair.get(pair, 0) + 1
        if len(selected) >= max_documents:
            break
    return securities, selected


def _first_text(source: dict[str, Any], *keys: str) -> str | None:
    for key in keys:
        value = source.get(key)
        if value not in (None, ""):
            text = str(value).strip()
            if text:
                return text
    return None


def _business_profile_properties(payload: dict[str, Any]) -> dict[str, Any]:
    """Normalize the stable, research-relevant part of a provider F10 profile.

    Provider payloads can contain several hundred fields and long histories.
    The original JSON stays in ``stock_f10_cache``; GraphRAG receives only the
    fields needed to reason about the issuer's actual business.  Missing fields
    remain missing and are never inferred from the security name or industry.
    """
    fields = payload.get("fields") if isinstance(payload.get("fields"), dict) else payload
    return _compact({
        "section": "profile",
        "source": payload.get("source"),
        "company_name": _first_text(fields, "公司名称", "公司全称", "company_name", "name"),
        "industry": _first_text(fields, "所属行业", "行业", "industry"),
        "main_business": _first_text(fields, "主营业务", "主營業務", "main_business", "principal_business"),
        "business_scope": _first_text(fields, "经营范围", "業務範圍", "business_scope"),
        "company_overview": _first_text(fields, "公司简介", "公司介绍", "公司沿革", "company_overview", "introduction"),
        "website": _first_text(fields, "官方网站", "公司网址", "website"),
        "fact_only": True,
        "inference_boundary": "公司概况仅代表来源披露；技术领先、国产替代、客户关系和市场份额必须另有原文证据。",
    })


def _business_composition_properties(payload: dict[str, Any]) -> dict[str, Any]:
    """Return a bounded but numeric-preserving main-business composition."""
    sections: list[dict[str, Any]] = []
    for raw_section in payload.get("sections") or []:
        if not isinstance(raw_section, dict):
            continue
        items = [item for item in (raw_section.get("items") or []) if isinstance(item, dict)]
        items.sort(key=lambda item: float(item.get("revenue_ratio") or 0), reverse=True)
        normalized_items = []
        for item in items[:6]:
            normalized_items.append({key: item.get(key) for key in (
                "name", "revenue", "revenue_ratio", "cost", "cost_ratio",
                "profit", "profit_ratio", "gross_margin",
            ) if item.get(key) is not None})
        if normalized_items:
            sections.append({
                "category": raw_section.get("category"),
                "items": normalized_items,
            })
        if len(sections) >= 4:
            break
    return _compact({
        "section": "business_composition",
        "source": payload.get("source"),
        "report_date": payload.get("report_date"),
        "segments": sections,
        "fact_only": True,
        "ratio_basis": "来源披露比例；revenue_ratio/gross_margin 以 0—1 小数保存。",
        "inference_boundary": "主营构成能证明收入结构，不能单独证明技术领先、市场份额或客户供应关系。",
    })


def _structured_observations(
    db: Session,
    market: str,
    symbol: str,
    cutoff: datetime | None,
    limit: int,
    period_start: date | None = None,
    period_end: date | None = None,
) -> dict[str, Any]:
    """Project bounded source-table observations into the GraphRAG contract.

    These are facts backed by the normalized market tables, not model
    inferences.  Keeping a stable ``source_table:source_record_id`` reference
    lets a reviewer jump from an LLA claim back to the source row even when no
    legacy KnowledgeDocument was generated for that row.
    """
    limit = max(1, min(int(limit or 1), 50))
    facts: list[dict[str, Any]] = []
    by_category: dict[str, list[dict[str, Any]]] = {
        "business": [], "financial": [], "market": [], "realtime": [], "shareholder": [], "fund_flow": [],
        # Normalized research rows are source facts, not LLM conclusions.
        "research": [],
    }
    source_record_refs: list[str] = []
    source_tables: set[str] = set()

    def ref(table: str, record_id: Any) -> str:
        return f"{table}:{record_id}"

    def add(category: str, row: Any, fact_type: str, title: str, properties: dict[str, Any], observed: Any) -> None:
        table = row.__tablename__
        record_ref = ref(table, row.id)
        item = {
            "id": record_ref,
            "fact_type": fact_type,
            "title": title,
            "source_table": table,
            "source_record_id": row.id,
            "source_id": getattr(row, "source_id", None),
            "observed_at": _iso(observed),
            "status": "SOURCE",
            "properties": _compact(properties),
            "evidence": [{
                "kind": "STRUCTURED_RECORD",
                "source_table": table,
                "source_record_id": row.id,
                "source_id": getattr(row, "source_id", None),
            }],
        }
        facts.append(item)
        by_category[category].append(item)
        source_record_refs.append(record_ref)
        source_tables.add(table)

    financial_query = select(StockFinancialReport).where(
        StockFinancialReport.market == market,
        StockFinancialReport.symbol == symbol,
    )
    if cutoff is not None:
        financial_query = financial_query.where(StockFinancialReport.fetched_at <= cutoff)
    financial_rows = list(db.scalars(financial_query.order_by(
        StockFinancialReport.report_period.desc(), StockFinancialReport.fetched_at.desc(),
    ).limit(limit * 2)).all())
    for row in financial_rows:
        if not _known(row.fetched_at, cutoff):
            continue
        data = row.data_json or {}
        add("financial", row, "FINANCIAL_OBSERVATION", f"{symbol} {row.indicator} · {row.report_period}", {
            "indicator": row.indicator,
            "report_period": row.report_period,
            "currency": row.currency,
            "data": data,
            "url": row.url,
            "fact_only": True,
        }, row.fetched_at)

    # The F10 UI reads normalized research tables, but the original context
    # builder only projected market/F10 rows.  Add the same source records to
    # the auditable fact contract so scoring and model citations can see them.
    def research_rows(model: Any, *where: Any, order: tuple[Any, ...], row_limit: int) -> list[Any]:
        query = select(model).where(*where)
        if cutoff is not None:
            query = query.where(model.fetched_at <= cutoff)
        return list(db.scalars(query.order_by(*order).limit(row_limit)).all())

    for row in research_rows(
        StockEarningsConsensus,
        StockEarningsConsensus.market == market,
        StockEarningsConsensus.symbol == symbol,
        order=(StockEarningsConsensus.forecast_year.desc(), StockEarningsConsensus.metric_code.asc()),
        row_limit=max(limit * 4, 24),
    ):
        add("research", row, "EARNINGS_FORECAST", f"{symbol} earnings forecast {row.forecast_year} {row.metric_name}", {
            "forecast_year": row.forecast_year, "metric_code": row.metric_code,
            "metric_name": row.metric_name, "unit": row.unit,
            "prediction_count": row.prediction_count,
            "minimum_value": row.minimum_value, "mean_value": row.mean_value,
            "maximum_value": row.maximum_value, "industry_average": row.industry_average,
            "source_code": row.source_code, "source_url": row.source_url,
            "source_updated_at": row.source_updated_at, "raw_payload": row.raw_payload or {},
            "is_forecast": True, "fact_only": True,
        }, row.fetched_at)

    for row in research_rows(
        StockInstitutionForecast,
        StockInstitutionForecast.market == market,
        StockInstitutionForecast.symbol == symbol,
        order=(StockInstitutionForecast.report_date.desc(), StockInstitutionForecast.id.desc()),
        row_limit=max(limit * 3, 18),
    ):
        add("research", row, "INSTITUTION_FORECAST", f"{symbol} institution forecast {row.institution} {row.report_date}", {
            "institution": row.institution, "report_date": row.report_date,
            "rating": row.rating, "analysts": row.analysts_json or [],
            "forecast": row.forecast_json or {}, "report_external_id": row.report_external_id,
            "source_code": row.source_code, "source_url": row.source_url,
            "detail_url": row.detail_url, "source_updated_at": row.source_updated_at,
            "raw_payload": row.raw_payload or {}, "is_forecast": True, "fact_only": True,
        }, row.fetched_at)

    for row in research_rows(
        StockInvestorQA,
        StockInvestorQA.market == market,
        StockInvestorQA.symbol == symbol,
        order=(StockInvestorQA.updated_time.desc(), StockInvestorQA.id.desc()),
        row_limit=max(limit * 2, 12),
    ):
        add("research", row, "INVESTOR_QA", f"{symbol} investor Q&A {row.asked_time or row.updated_time}", {
            "question": row.question, "answer": row.answer,
            "questioner": row.questioner, "answerer": row.answerer,
            "asked_at": row.asked_time, "answered_at": row.answered_time,
            "updated_at": row.updated_time, "source_code": row.source_code,
            "source_url": row.source_url, "source_updated_at": row.source_updated_at,
            "qa_status": "ANSWERED" if row.answer else "PENDING_REPLY",
            "raw_payload": row.raw_payload or {}, "fact_only": True,
        }, row.fetched_at)

    for row in research_rows(
        StockBrokerResearchReport,
        StockBrokerResearchReport.market == market,
        StockBrokerResearchReport.symbol == symbol,
        order=(StockBrokerResearchReport.report_date.desc(), StockBrokerResearchReport.id.desc()),
        row_limit=max(limit * 2, 12),
    ):
        add("research", row, "RESEARCH_REPORT", f"{symbol} research report {row.title}", {
            "title": row.title, "report_date": row.report_date,
            "institution": row.institution, "rating": row.rating,
            "summary": row.summary, "content_status": row.content_status,
            "source_code": row.source_code, "source_url": row.source_url,
            "detail_url": row.detail_url, "pdf_url": row.pdf_url,
            "source_updated_at": row.source_updated_at, "raw_payload": row.raw_payload or {},
            "fact_only": True,
        }, row.fetched_at)

    # Classification labels are evidence for industry/theme context only.  Do
    # not infer a supplier/customer relation from shared membership.
    master = db.scalar(select(StockSymbol).where(StockSymbol.market == market, StockSymbol.symbol == symbol))
    if master is not None:
        try:
            from app.services.stock_classification import build_classification_groups
            latest_profile = db.scalar(select(StockF10Cache).where(
                StockF10Cache.market == market, StockF10Cache.symbol == symbol,
                StockF10Cache.section == "profile",
            ).order_by(StockF10Cache.fetched_at.desc(), StockF10Cache.id.desc()))
            fields = (latest_profile.payload_json or {}).get("fields") if latest_profile else {}
            groups = build_classification_groups(db, master, fields if isinstance(fields, dict) else {})
            rows = [{"group": group.get("key"), "title": group.get("title"), "items": [
                {key: item.get(key) for key in (
                    "name", "label", "code", "dimension", "classification_dimension", "definition",
                    "definition_version", "source_name", "source_url", "level", "relation_caveat",
                ) if item.get(key) not in (None, "")}
                for item in group.get("items") or []
            ]} for group in groups if group.get("items")]
            if rows:
                add("research", master, "CLASSIFICATION_OBSERVATION", f"{symbol} industry and theme classification", {
                    "groups": rows, "source": "security master classification evidence",
                    "fact_only": True,
                    "inference_boundary": "Classification does not prove a supplier, customer, or causal relation.",
                }, latest_profile.fetched_at if latest_profile else master.updated_at)
        except Exception:
            # Optional enrichment must not block the core evidence package.
            pass

    # Company profile and main-business composition are long-horizon issuer
    # facts.  They are selected as the latest source record known at cutoff,
    # not filtered out merely because the user chose a short event window.
    business_rows = list(db.scalars(select(StockF10Cache).where(
        StockF10Cache.market == market,
        StockF10Cache.symbol == symbol,
        StockF10Cache.section.in_(("profile", "business_composition")),
        StockF10Cache.fetched_at <= cutoff if cutoff is not None else StockF10Cache.id > 0,
    ).order_by(StockF10Cache.fetched_at.desc(), StockF10Cache.id.desc()).limit(6)).all())
    seen_business_sections: set[str] = set()
    for row in business_rows:
        if row.section in seen_business_sections or not _known(row.fetched_at, cutoff):
            continue
        seen_business_sections.add(row.section)
        payload = row.payload_json if isinstance(row.payload_json, dict) else {}
        if row.section == "profile":
            properties = _business_profile_properties(payload)
            has_content = any(properties.get(key) for key in ("main_business", "business_scope", "company_overview"))
            fact_type, title = "BUSINESS_PROFILE", f"{symbol} 主营业务与公司概况"
        else:
            properties = _business_composition_properties(payload)
            has_content = bool(properties.get("segments"))
            fact_type, title = "BUSINESS_COMPOSITION", f"{symbol} 主营构成 · {properties.get('report_date') or '报告期未披露'}"
        if has_content:
            add("business", row, fact_type, title, properties, row.fetched_at)

    kline_query = select(StockKline).where(
        StockKline.market == market,
        StockKline.symbol == symbol,
        StockKline.period == "daily",
    )
    if cutoff is not None:
        kline_query = kline_query.where(
            StockKline.fetched_at <= cutoff,
            StockKline.trade_date <= cutoff.date().isoformat(),
        )
    if period_start is not None:
        kline_query = kline_query.where(StockKline.trade_date >= period_start.isoformat())
    if period_end is not None:
        kline_query = kline_query.where(StockKline.trade_date <= period_end.isoformat())
    kline_rows = list(db.scalars(kline_query.order_by(
        StockKline.trade_date.desc(), StockKline.fetched_at.desc(),
    ).limit(max(40, limit * 2))).all())
    kline_rows = [row for row in kline_rows if _known(row.fetched_at, cutoff) and _date_known(row.trade_date, cutoff)]
    if kline_rows:
        latest = kline_rows[0]
        bars = [{key: getattr(row, key) for key in (
            "trade_date", "open_price", "high_price", "low_price", "close_price",
            "volume", "amount", "turnover_rate", "adjust",
        )} for row in kline_rows[:20]]
        add("market", latest, "MARKET_OBSERVATION", f"{symbol} 日线量价 · {latest.trade_date}", {
            "period": latest.period,
            "adjust": latest.adjust,
            "latest": bars[0],
            "recent_bars": bars,
            "window_size": len(bars),
            "fact_only": True,
        }, latest.fetched_at)

    quote_query = select(StockRealtimeQuote).where(
        StockRealtimeQuote.market == market,
        StockRealtimeQuote.symbol == symbol,
    )
    if cutoff is not None:
        quote_query = quote_query.where(StockRealtimeQuote.fetched_at <= cutoff)
    quote_rows = list(db.scalars(quote_query.order_by(
        StockRealtimeQuote.fetched_at.desc(),
    ).limit(limit)).all())
    quote = next((row for row in quote_rows if _known(row.fetched_at, cutoff)), None)
    if quote is not None:
        add("realtime", quote, "REALTIME_QUOTE", f"{symbol} 实时行情 · {quote.quote_time or '时间未说明'}", {
            key: getattr(quote, key) for key in (
                "quote_time", "current_price", "previous_close_price", "open_price", "high_price",
                "low_price", "volume", "amount", "change_amount", "change_pct", "turnover_rate",
            )
        } | {"fact_only": True}, quote.fetched_at)

    # F10 section names are the available structured shareholder signal.  The
    # section payload remains source data; no ownership percentage is inferred
    # when the source does not provide one.
    shareholder_terms = ("股东", "回购", "增持", "减持", "股本", "shareholder", "holder")
    f10_query = select(StockF10Cache).where(
        StockF10Cache.market == market,
        StockF10Cache.symbol == symbol,
        or_(*[StockF10Cache.section.ilike(f"%{term}%") for term in shareholder_terms]),
    )
    if cutoff is not None:
        f10_query = f10_query.where(StockF10Cache.fetched_at <= cutoff)
    f10_rows = list(db.scalars(f10_query.order_by(
        StockF10Cache.fetched_at.desc(), StockF10Cache.id.desc(),
    ).limit(limit * 2)).all())
    for row in f10_rows:
        if not _known(row.fetched_at, cutoff):
            continue
        add("shareholder", row, "SHAREHOLDER_OBSERVATION", f"{symbol} F10 · {row.section}", {
            "section": row.section,
            "payload": row.payload_json or {},
            "fact_only": True,
            "note": "F10 原始分区资料；仅在原文有明确数值时解释持股比例，不作名称推断。",
        }, row.fetched_at)

    fund_flow_rows = list(db.scalars(select(StockF10Cache).where(
        StockF10Cache.market == market,
        StockF10Cache.symbol == symbol,
        StockF10Cache.section == "fund_flow",
        StockF10Cache.fetched_at <= cutoff if cutoff is not None else StockF10Cache.id > 0,
    ).order_by(StockF10Cache.fetched_at.desc(), StockF10Cache.id.desc()).limit(2)).all())
    for row in fund_flow_rows:
        if not _known(row.fetched_at, cutoff):
            continue
        add("fund_flow", row, "FUND_FLOW_OBSERVATION", f"{symbol} 资金流向", {
            "section": row.section,
            "payload": row.payload_json or {},
            "fact_only": True,
            "note": "供应商资金流估算，不代表可验证的真实账户买卖。",
        }, row.fetched_at)

    return {
        "facts": facts[: max(limit * 3, 1)],
        "by_category": by_category,
        "source_record_refs": _unique(source_record_refs),
        "source_tables": source_tables,
        "counts": {key: len(value) for key, value in by_category.items()},
    }


def _dataset_versions(
    db: Session,
    source_tables: set[str],
    *,
    market: str,
    symbol: str,
    cutoff: datetime | None,
) -> list[dict[str, Any]]:
    """Resolve the latest published version matching this security and cutoff."""
    rows = list(db.scalars(select(LakeDataset).order_by(LakeDataset.dataset_code)).all())
    normalized_sources = {str(item).lower() for item in source_tables}
    result: list[dict[str, Any]] = []
    for dataset in rows:
        marker = f"{dataset.dataset_code} {dataset.description or ''}".lower()
        selected: tuple[LakeDatasetVersion, LakeObject | None, set[str], dict[str, Any], list[Any], bool] | None = None
        versions = list(db.scalars(select(LakeDatasetVersion).where(
            LakeDatasetVersion.dataset_id == dataset.id,
            LakeDatasetVersion.status == "PUBLISHED",
        ).order_by(LakeDatasetVersion.created_at.desc(), LakeDatasetVersion.id.desc())).all())
        for version_row in versions:
            if not _known(version_row.created_at, cutoff):
                continue
            obj = db.get(LakeObject, version_row.object_id) if version_row.object_id else None
            if obj is not None and not _known(obj.created_at, cutoff):
                continue
            quality = version_row.quality_json if isinstance(version_row.quality_json, dict) else {}
            metadata = obj.metadata_json if obj is not None and isinstance(obj.metadata_json, dict) else {}
            declared_sources = {
                str(value).lower() for value in (
                    quality.get("source_table"),
                    obj.source_table if obj is not None else None,
                    metadata.get("source_table"),
                ) if value
            }
            text_match = any(table in marker for table in normalized_sources)
            object_match = bool(declared_sources & normalized_sources)
            if normalized_sources and not (object_match or text_match):
                continue
            scope_pairs = quality.get("scope_pairs")
            if not isinstance(scope_pairs, list):
                scope_pairs = metadata.get("scope_pairs") if isinstance(metadata.get("scope_pairs"), list) else []
            scope_applied = quality.get("scope_applied")
            if scope_applied is None:
                scope_applied = metadata.get("scope_applied")
            if scope_applied is True and not any(
                isinstance(pair, (list, tuple)) and len(pair) >= 2
                and str(pair[0]).upper() == market and str(pair[1]).upper() == symbol
                for pair in scope_pairs
            ):
                continue
            selected = (version_row, obj, declared_sources, quality, scope_pairs, bool(scope_applied))
            break
        if selected is None:
            continue
        row, obj, declared_sources, quality, scope_pairs, scope_applied = selected
        result.append({
            "dataset_id": dataset.id,
            "dataset_code": dataset.dataset_code,
            "layer": dataset.layer,
            "format": dataset.format,
            "version": row.version,
            "version_id": row.id,
            "object_id": row.object_id,
            "object_uri": obj.object_uri if obj is not None else None,
            "source_tables": sorted(declared_sources) or sorted(
                table for table in source_tables if table.lower() in marker
            ),
            "scope_pairs": scope_pairs,
            "scope_applied": scope_applied,
            "quality": quality,
            "row_count": row.row_count,
        })
    return result


def _lineage_rows(
    db: Session,
    *,
    market: str,
    symbol: str,
    source_tables: set[str],
    document_ids: list[int],
    chunk_ids: list[str],
    structured_refs: list[str],
    datasets: list[dict[str, Any]],
    cutoff: datetime | None,
) -> list[dict[str, Any]]:
    """Return lineage touching this security's docs, records or lake objects."""
    document_id_set = {str(item) for item in document_ids if item not in (None, "")}
    chunk_id_set = {str(item) for item in chunk_ids if item not in (None, "")}
    structured_id_set = {str(item) for item in structured_refs if item not in (None, "")}
    dataset_versions = {
        f"{item.get('dataset_code')}:{item.get('version')}" for item in datasets
        if item.get("dataset_code") and item.get("version")
    }
    object_ids = {str(item.get("object_id")) for item in datasets if item.get("object_id")}
    metadata_ids = document_id_set | chunk_id_set | structured_id_set | object_ids
    source_names = {str(item) for item in source_tables if item}
    conditions = []
    if document_id_set:
        conditions.append(and_(
            LakeLineageEvent.upstream_type == "KNOWLEDGE_DOCUMENT",
            LakeLineageEvent.upstream_id.in_(document_id_set),
        ))
    if chunk_id_set:
        conditions.append(and_(
            LakeLineageEvent.downstream_type == "DOCUMENT_CHUNK",
            LakeLineageEvent.downstream_id.in_(chunk_id_set),
        ))
    if object_ids:
        conditions.extend([
            and_(LakeLineageEvent.upstream_type == "LAKE_OBJECT",
                 LakeLineageEvent.upstream_id.in_(object_ids)),
            and_(LakeLineageEvent.downstream_type == "LAKE_OBJECT",
                 LakeLineageEvent.downstream_id.in_(object_ids)),
        ])
    if dataset_versions:
        conditions.append(and_(
            LakeLineageEvent.downstream_type == "DATASET_VERSION",
            LakeLineageEvent.downstream_id.in_(dataset_versions),
        ))
    if source_names:
        conditions.append(and_(
            LakeLineageEvent.upstream_type == "SOURCE_TABLE",
            LakeLineageEvent.upstream_id.in_(source_names),
        ))
    # Pipeline events carry a market plus a symbols array rather than an
    # individual downstream record identifier. They remain a bounded set and
    # are filtered against that scope below.
    conditions.append(LakeLineageEvent.upstream_type == "PIPELINE_RUN")
    lineage_query = select(LakeLineageEvent).where(or_(*conditions))
    if cutoff is not None:
        lineage_query = lineage_query.where(LakeLineageEvent.created_at <= cutoff)
    candidates = list(db.scalars(lineage_query.order_by(
        LakeLineageEvent.created_at.desc(), LakeLineageEvent.id.desc()).limit(5000)).all())
    result: list[dict[str, Any]] = []
    for row in candidates:
        if not _known(row.created_at, cutoff):
            continue
        metadata = row.metadata_json if isinstance(row.metadata_json, dict) else {}
        scope_pairs = metadata.get("scope_pairs") or []
        pair_matches = not scope_pairs or any(
            isinstance(pair, (list, tuple)) and len(pair) >= 2 and
            str(pair[0]).upper() == market and str(pair[1]).upper() == symbol
            for pair in scope_pairs
        )
        metadata_symbols = metadata.get("symbols") if isinstance(metadata.get("symbols"), list) else []
        metadata_matches = (
            str(metadata.get("market", "")).upper() == market and (
                str(metadata.get("symbol", "")).upper() == symbol or
                symbol in {str(item).upper() for item in metadata_symbols}
            )
        )
        scoped_source_match = bool(scope_pairs) and (
            row.upstream_type == "SOURCE_TABLE" and str(row.upstream_id) in source_names
        )
        metadata_id_match = any(
            str(metadata.get(key)) in metadata_ids
            for key in ("document_id", "source_record_id", "object_id")
            if metadata.get(key) not in (None, "")
        )
        touched = (
            (row.upstream_type == "KNOWLEDGE_DOCUMENT" and str(row.upstream_id) in document_id_set) or
            (row.downstream_type == "DOCUMENT_CHUNK" and str(row.downstream_id) in chunk_id_set) or
            (row.upstream_type == "LAKE_OBJECT" and str(row.upstream_id) in object_ids) or
            (row.downstream_type == "LAKE_OBJECT" and str(row.downstream_id) in object_ids) or
            (row.downstream_type == "DATASET_VERSION" and str(row.downstream_id) in dataset_versions) or
            scoped_source_match or metadata_matches or metadata_id_match
        )
        if touched and pair_matches:
            result.append({
                "id": row.id,
                "batch_id": row.batch_id,
                "upstream_type": row.upstream_type,
                "upstream_id": row.upstream_id,
                "downstream_type": row.downstream_type,
                "downstream_id": row.downstream_id,
                "transformation": row.transformation,
                "parser_version": row.parser_version,
                "dataset_version": row.dataset_version,
                "metadata": _compact(metadata),
                "created_at": _iso(row.created_at),
            })
        if len(result) >= 120:
            break
    return result


def _document_source_hash(document: KnowledgeDocument) -> str:
    return knowledge_document_source_hash(document)


def _eligible_chunk_rows(
    db: Session,
    documents: list[KnowledgeDocument],
    cutoff: datetime | None,
) -> list[DocumentChunkVersion]:
    """Return current, ready chunks that existed at the point-in-time cutoff."""
    grouped = current_knowledge_document_chunks(db, documents, known_at=cutoff)
    return [row for document in documents for row in grouped.get(str(document.id), [])]


def _chunk_payload(row: DocumentChunkVersion) -> dict[str, Any]:
    metadata = row.metadata_json or {}
    embedding = metadata.get("embedding") or {}
    kind = (row.embedding_kind or "NONE").upper()
    quality = embedding.get("model_quality")
    is_hash = kind == "HASH" or quality == "HASH_ONLY" or "HASH" in (row.embedding_model or "").upper()
    embedding_status = "HASH_ONLY" if is_hash else (
        "AVAILABLE" if kind == "SEMANTIC" and embedding.get("vector") else "MISSING"
    )
    return {
        "id": row.id,
        "document_id": row.document_id,
        "chunk_index": row.chunk_index,
        "chunk_version": row.chunk_version,
        "content_hash": row.content_hash,
        "text": row.chunk_text[:1800],
        "start_offset": row.start_offset,
        "end_offset": row.end_offset,
        "parser_version": row.parser_version,
        "embedding_model": row.embedding_model,
        "embedding_status": embedding_status,
        "embedding_kind": kind,
        "embedding_quality": quality,
        "section_title": row.section_title or metadata.get("section_title"),
        "section_status": row.section_status,
        "boundary_type": metadata.get("boundary_type"),
        "metadata": metadata,
    }


def _chunks(
    rows: list[DocumentChunkVersion],
    document_ids: list[int],
    limit: int,
) -> list[dict[str, Any]]:
    """Bound context chunks fairly so one long document cannot crowd out all others."""
    if not rows or not document_ids or limit <= 0:
        return []
    grouped: dict[str, list[DocumentChunkVersion]] = {}
    for row in rows:
        grouped.setdefault(str(row.document_id), []).append(row)
    for items in grouped.values():
        items.sort(key=lambda row: row.chunk_index)
    ordered_ids = [str(item) for item in document_ids]
    selected: list[DocumentChunkVersion] = []
    offset = 0
    while len(selected) < limit:
        added = False
        for document_id in ordered_ids:
            items = grouped.get(document_id, [])
            if offset < len(items):
                selected.append(items[offset])
                added = True
                if len(selected) >= limit:
                    break
        if not added:
            break
        offset += 1
    return [_chunk_payload(row) for row in selected]


def _chunk_coverage(
    document_ids: list[int],
    eligible_rows: list[DocumentChunkVersion],
    included_chunks: list[dict[str, Any]],
) -> dict[str, Any]:
    """Report both ready-chunk availability and actual prompt inclusion."""
    requested = {str(item) for item in document_ids if item not in (None, "")}
    if not requested:
        return {
            "scope": "BOUNDED_SELECTION_CONTEXT",
            "coverage_status": "NO_DOCUMENTS",
            "documents_requested": 0,
            "documents_with_chunks": 0,
            "documents_included": 0,
            "coverage_ratio": None,
            "available_chunk_coverage_ratio": None,
            "context_inclusion_ratio": None,
            "missing_document_ids": [],
            "omitted_from_context_document_ids": [],
        }
    covered = {str(row.document_id) for row in eligible_rows if row.document_id not in (None, "")}
    included = {
        str(row.get("document_id")) for row in included_chunks if row.get("document_id") not in (None, "")
    }
    available_ratio = round(len(requested & covered) / len(requested), 6)
    included_ratio = round(len(requested & included) / len(requested), 6)
    return {
        "scope": "BOUNDED_SELECTION_CONTEXT",
        "coverage_status": "COMPLETE" if requested <= covered else "PARTIAL",
        "documents_requested": len(requested),
        "documents_with_chunks": len(requested & covered),
        "documents_included": len(requested & included),
        # Compatibility alias for clients already reading coverage_ratio.
        "coverage_ratio": available_ratio,
        "available_chunk_coverage_ratio": available_ratio,
        "context_inclusion_ratio": included_ratio,
        "missing_document_ids": sorted(requested - covered)[:50],
        "omitted_from_context_document_ids": sorted((requested & covered) - included)[:50],
    }


def build_selection_context(
    db: Session,
    market: str,
    symbol: str,
    *,
    knowledge_base_ids: list[int] | None = None,
    graph_ids: list[int] | None = None,
    as_of: datetime | None = None,
    period_start: date | None = None,
    period_end: date | None = None,
    max_documents: int = 40,
    max_relations: int = 40,
    max_facts: int = 40,
    max_chunks: int = 32,
) -> dict[str, Any]:
    """Build a bounded, reproducible context contract for one security.

    ``as_of`` is the information cutoff.  The result includes identifiers and
    hashes for every versioned layer so a later review can prove what the LLA
    actually saw.  Empty source areas are represented explicitly as gaps.
    """
    market, symbol = market.strip().upper(), symbol.strip().upper()
    cutoff = as_of or datetime.now(timezone.utc)
    kb_ids = sorted(set(int(item) for item in (knowledge_base_ids or []) if item))
    graph_ids = sorted(set(int(item) for item in (graph_ids or []) if item))
    identity = _identity(db, market, symbol, cutoff)
    master = db.scalar(select(StockSymbol).where(StockSymbol.market == market, StockSymbol.symbol == symbol))

    def usable_document(row):
        if row.source_table != "stock_news":
            return True
        return master is not None and check_news_identity(
            market, symbol, master.name, row.title, row.content, master.ext_json,
        )["status"] == "MENTION_MATCHED"

    doc_query = select(KnowledgeDocument).where(
        KnowledgeDocument.market == market,
        KnowledgeDocument.symbol == symbol,
        KnowledgeDocument.created_at <= cutoff,
        KnowledgeDocument.updated_at <= cutoff,
    )
    if kb_ids:
        doc_query = doc_query.where(KnowledgeDocument.knowledge_base_id.in_(kb_ids))
    if graph_ids:
        doc_query = doc_query.where(KnowledgeDocument.graph_id.in_(graph_ids))
    doc_candidates = ranked_documents(db, doc_query, per_source=max(500, max_documents * 4))
    by_source: dict[str, list[KnowledgeDocument]] = {}
    undated_documents = 0
    for row in doc_candidates:
        published_at = _doc_payload(row, include_content=False)["published_at"]
        if not usable_document(row) or not _date_known(published_at, cutoff):
            continue
        if not _date_in_window(published_at, period_start, period_end):
            if published_at in (None, ""):
                undated_documents += 1
            continue
        by_source.setdefault(row.source_table, []).append(row)
    period_source_counts = {source: len(rows) for source, rows in by_source.items()}
    period_document_count = sum(period_source_counts.values())
    period_disclosure_count = sum(period_source_counts.get(source, 0)
                                  for source in ("stock_news", "stock_notice"))
    priority = ["stock_news", "stock_notice", "stock_context_event", "research_report"]
    source_keys = sorted(by_source, key=lambda key: (priority.index(key) if key in priority else len(priority), key))
    docs: list[KnowledgeDocument] = []
    for source in ("stock_news", "stock_notice"):
        for row in by_source.get(source, []):
            if len(docs) >= max_documents:
                break
            docs.append(row)
    source_keys = [source for source in source_keys if source not in {"stock_news", "stock_notice"}]
    offset = 0
    while len(docs) < max_documents and source_keys:
        added = False
        for source in source_keys:
            rows = by_source[source]
            if offset < len(rows):
                docs.append(rows[offset])
                added = True
                if len(docs) >= max_documents:
                    break
        if not added:
            break
        offset += 1
    document_ids = [row.id for row in docs]

    entity_query = select(KnowledgeEntity).where(
        KnowledgeEntity.knowledge_base_id.in_(kb_ids) if kb_ids else KnowledgeEntity.id > 0,
    )
    if graph_ids:
        entity_query = entity_query.where(KnowledgeEntity.graph_id.in_(graph_ids))
    # Graph builder keys are namespaced (for example
    # ``{graph_id}:stock_symbol:{market}:{symbol}``).  Match the suffix rather
    # than assuming the namespace starts at position zero.
    suffixes = (f"%:{market}:{symbol}", f"%:{market}:{symbol}:%")
    entity_query = entity_query.where(or_(
        KnowledgeEntity.entity_key.like(suffixes[0]),
        KnowledgeEntity.entity_key.like(suffixes[1]),
    ))
    if cutoff is not None:
        entity_query = entity_query.where(
            KnowledgeEntity.created_at <= cutoff,
            KnowledgeEntity.updated_at <= cutoff,
        )
    entities = [
        row for row in db.scalars(entity_query.limit(360)).all()
        if _known(row.created_at, cutoff)
    ][:120]
    entity_ids = [row.id for row in entities]
    relations: list[KnowledgeRelation] = []
    if entity_ids:
        relation_query = select(KnowledgeRelation).where(
            or_(KnowledgeRelation.subject_entity_id.in_(entity_ids), KnowledgeRelation.object_entity_id.in_(entity_ids)),
        )
        if kb_ids:
            relation_query = relation_query.where(KnowledgeRelation.knowledge_base_id.in_(kb_ids))
        if graph_ids:
            relation_query = relation_query.where(KnowledgeRelation.graph_id.in_(graph_ids))
        if cutoff is not None:
            relation_query = relation_query.where(
                KnowledgeRelation.created_at <= cutoff,
                KnowledgeRelation.updated_at <= cutoff,
            )
        relations = [
            row for row in db.scalars(
                relation_query.order_by(KnowledgeRelation.id.desc()).limit(max_relations * 4)
            ).all()
            if _known(row.created_at, cutoff)
        ][:max_relations]
    all_relation_entity_ids = _unique(
        [value for relation in relations for value in (relation.subject_entity_id, relation.object_entity_id)]
    )
    relation_entity_query = select(KnowledgeEntity).where(
        KnowledgeEntity.id.in_(all_relation_entity_ids)
    )
    if cutoff is not None:
        relation_entity_query = relation_entity_query.where(
            KnowledgeEntity.created_at <= cutoff,
            KnowledgeEntity.updated_at <= cutoff,
        )
    relation_entities = {
        row.id: row for row in db.scalars(relation_entity_query).all()
        if _known(row.created_at, cutoff) and _known(row.updated_at, cutoff)
    } if all_relation_entity_ids else {}
    relation_doc_ids = _unique([row.evidence_document_id for row in relations])
    relation_doc_query = select(KnowledgeDocument).where(
        KnowledgeDocument.id.in_(relation_doc_ids)
    )
    if kb_ids:
        relation_doc_query = relation_doc_query.where(KnowledgeDocument.knowledge_base_id.in_(kb_ids))
    if graph_ids:
        relation_doc_query = relation_doc_query.where(KnowledgeDocument.graph_id.in_(graph_ids))
    if cutoff is not None:
        relation_doc_query = relation_doc_query.where(
            KnowledgeDocument.created_at <= cutoff,
            KnowledgeDocument.updated_at <= cutoff,
        )
    relation_docs = {
        row.id: row for row in db.scalars(relation_doc_query).all()
        if _known(row.created_at, cutoff) and _known(row.updated_at, cutoff) and usable_document(row)
        and _date_in_window(_doc_payload(row, include_content=False)["published_at"], period_start, period_end)
    } if relation_doc_ids else {}
    relations = [
        row for row in relations
        if row.subject_entity_id in relation_entities
        and row.object_entity_id in relation_entities
        and row.predicate != "HAS_NEWS_CANDIDATE"
        and (row.evidence_document_id is None or row.evidence_document_id in relation_docs)
    ]
    graph_paths = []
    for row in relations:
        subject = relation_entities.get(row.subject_entity_id)
        target = relation_entities.get(row.object_entity_id)
        document = relation_docs.get(row.evidence_document_id)
        graph_paths.append({
            "id": row.id,
            "subject_entity_id": row.subject_entity_id,
            "object_entity_id": row.object_entity_id,
            "subject_name": subject.entity_name if subject else None,
            "object_name": target.entity_name if target else None,
            "predicate": row.predicate,
            "created_at": _iso(row.created_at),
            "evidence_document_id": row.evidence_document_id,
            "evidence_excerpt": (document.content or "")[:800] if document else None,
            **_relation_provenance(row, subject, target, document),
        })

    company_id = identity.get("company_id")
    facts: list[dict[str, Any]] = []
    fact_evidence_ids: list[str] = []
    if company_id:
        fact_query = facts_query(
            status="ACCEPTED", entity_id=company_id,
            as_of=cutoff.date(), known_at=cutoff,
        ).limit(max_facts)
        for fact in db.scalars(fact_query).all():
            links = list(db.scalars(select(FoundationFactEvidence).where(
                FoundationFactEvidence.fact_id == fact.id,
            )).all())
            proof_ids = [link.evidence_id for link in links]
            proofs = list(db.scalars(select(FoundationEvidence).where(
                FoundationEvidence.id.in_(proof_ids),
            )).all()) if proof_ids else []
            proofs = [proof for proof in proofs if _known(proof.available_at, cutoff)]
            fact_evidence_ids.extend(proof.id for proof in proofs)
            facts.append({
                "id": fact.id,
                "fact_type": fact.fact_type,
                "title": fact.title,
                "subject_entity_id": fact.subject_entity_id,
                "object_entity_id": fact.object_entity_id,
                "properties": fact.properties_json or {},
                "status": fact.status,
                "valid_from": _iso(fact.valid_from),
                "valid_to": _iso(fact.valid_to),
                "created_at": _iso(fact.created_at),
                "evidence": [{
                    "id": proof.id,
                    "title": proof.title,
                    "source_name": proof.source_name,
                    "source_url": proof.url,
                    "published_at": _iso(proof.published_at),
                    "available_at": _iso(proof.available_at),
                    "excerpt": proof.content[:900],
                    "content_hash": proof.content_hash,
                } for proof in proofs],
            })

    structured = _structured_observations(
        db, market, symbol, cutoff, max_facts,
        period_start=period_start, period_end=period_end,
    )
    # Keep the original FoundationFact shape while adding normalized market
    # observations to the same bounded fact channel.  Consumers can use
    # ``fact_type`` to distinguish source observations from reviewed company
    # relationships.
    fact_limit = max(0, int(max_facts))
    facts = (facts + structured["facts"])[:fact_limit]

    context_documents = list(docs)
    documents = [_doc_payload(row) for row in context_documents]
    # Include relation proof documents in the evidence set, but do not let them
    # displace the source-balanced primary documents above.
    for row in relation_docs.values():
        if row.id not in document_ids and len(documents) < max_documents:
            context_documents.append(row)
            documents.append(_doc_payload(row))
            document_ids.append(row.id)
    related_securities, related_documents = _related_security_documents(
        db,
        company_id=company_id,
        market=market,
        symbol=symbol,
        kb_ids=kb_ids,
        graph_ids=graph_ids,
        cutoff=cutoff,
        period_start=period_start,
        period_end=period_end,
    )
    reviewed_fact_ids = {str(row.get("id")) for row in facts if row.get("status") == "ACCEPTED"}
    related_securities = [
        {**row, "related_via_fact_ids": [fact_id for fact_id in row["related_via_fact_ids"]
                                           if fact_id in reviewed_fact_ids]}
        for row in related_securities
    ]
    related_securities = [row for row in related_securities if row["related_via_fact_ids"]]
    allowed_related_pairs = {(row["market"], row["symbol"]) for row in related_securities}
    related_documents = [
        {**row, "related_via_fact_ids": [fact_id for fact_id in row["related_via_fact_ids"]
                                          if fact_id in reviewed_fact_ids]}
        for row in related_documents
        if (row.get("market"), row.get("symbol")) in allowed_related_pairs
    ]
    related_documents = [row for row in related_documents if row["related_via_fact_ids"]]
    for row in related_documents:
        if row["id"] not in document_ids:
            documents.append(row)
            document_ids.append(row["id"])
    eligible_chunks = _eligible_chunk_rows(db, context_documents, cutoff)
    chunks = _chunks(eligible_chunks, document_ids, max_chunks)
    chunk_coverage = _chunk_coverage(document_ids, eligible_chunks, chunks)
    source_tables = ({row.source_table for row in context_documents}
                     | {str(row["source_table"]) for row in related_documents}
                     | set(structured["source_tables"]))
    datasets = _dataset_versions(
        db, source_tables, market=market, symbol=symbol, cutoff=cutoff,
    )
    lineage_rows = _lineage_rows(
        db,
        market=market,
        symbol=symbol,
        source_tables=source_tables,
        document_ids=document_ids,
        chunk_ids=[str(item["id"]) for item in chunks],
        structured_refs=structured["source_record_refs"],
        datasets=datasets,
        cutoff=cutoff,
    )

    graph_versions = []
    if graph_ids:
        graph_versions = [{"id": row.id, "code": row.graph_code, "version": row.version,
                           "status": row.governance_status} for row in db.scalars(
                               select(KnowledgeGraph).where(KnowledgeGraph.id.in_(graph_ids))
                           ).all() if _known(row.created_at, cutoff)]
    kb_versions = [{"id": row.id, "code": row.kb_code, "version": row.version, "status": row.status}
                   for row in db.scalars(select(KnowledgeBase).where(KnowledgeBase.id.in_(kb_ids))).all()
                   if _known(row.created_at, cutoff)] if kb_ids else []
    graph_version = _hash({"graphs": graph_versions, "kbs": kb_versions, "relations": [row.id for row in relations]})[:32]
    lakehouse_version = _hash({
        "datasets": datasets,
        "lineage": [row["id"] for row in lineage_rows],
        "structured_records": structured["source_record_refs"],
    })[:32]
    document_version = _hash({"documents": [(row.get("id"), row.get("updated_at"), row.get("content_hash"))
                                              for row in documents],
                              "chunks": [(row["id"], row["chunk_version"]) for row in chunks]})[:32]
    evidence_span = [{
        "document_id": row["id"],
        "start": 0,
        "end": min(len(row.get("content") or ""), 1200),
        "quote": row.get("content") or "",
        "content_hash": row.get("content_hash"),
    } for row in documents[:max_documents]]
    missing = []
    if not documents:
        missing.append("分析周期内缺少知识库文档")
    if period_document_count > len(documents):
        missing.append(f"分析周期内文档超过上下文预算（已载入 {len(documents)}/{period_document_count} 篇）")
    if undated_documents:
        missing.append(f"{undated_documents} 篇文档缺少可验证发布日期，未纳入周期分析")
    if not facts and not graph_paths:
        missing.append("公司关系事实/图谱关系")
    if not chunks:
        missing.append("文档切片")
    elif (chunk_coverage.get("available_chunk_coverage_ratio") or 0) < 1:
        missing.append(
            f"部分知识文档尚未切片（{chunk_coverage['documents_with_chunks']}/"
            f"{chunk_coverage['documents_requested']}）"
        )
    if chunks and (chunk_coverage.get("context_inclusion_ratio") or 0) < (
        chunk_coverage.get("available_chunk_coverage_ratio") or 0
    ):
        missing.append(
            f"部分可用切片因上下文预算未载入（{chunk_coverage['documents_included']}/"
            f"{chunk_coverage['documents_with_chunks']} 篇文档）"
        )
    if any(item.get("embedding_status") != "AVAILABLE" for item in chunks):
        missing.append("部分切片尚未生成向量")
    category_labels = {
        "business": "结构化主营业务与主营构成",
        "financial": "结构化财务观测",
        "market": "结构化量价观测",
        "realtime": "实时行情观测",
        "shareholder": "股东/F10观测",
        "fund_flow": "资金流观测",
        "research": "研究数据（盈利预测、机构预测、问董秘、研报与行业概念）",
    }
    for category, label in category_labels.items():
        if not structured["by_category"].get(category):
            missing.append(label)
    context = {
        "context_version": CONTEXT_VERSION,
        "as_of": cutoff.isoformat(),
        "data_cutoff": cutoff.date().isoformat(),
        "analysis_period": {
            "start": period_start.isoformat() if period_start else None,
            "end": period_end.isoformat() if period_end else cutoff.date().isoformat(),
        },
        "identity": identity,
        "graph_version": graph_version,
        "lakehouse_version": lakehouse_version,
        "document_version": document_version,
        "knowledge_base_versions": kb_versions,
        "graph_versions": graph_versions,
        "lakehouse_datasets": datasets,
        "lineage": lineage_rows,
        "documents": documents,
        "related_securities": related_securities,
        "related_security_documents": related_documents,
        "facts": facts,
        "structured_observations": structured["by_category"],
        "graph_paths": graph_paths,
        "chunks": chunks,
        "chunk_coverage": chunk_coverage,
        "evidence_span": evidence_span,
        "evidence_document_ids": document_ids,
        # Structured source rows are valid fact references even when the
        # bounded ``facts`` array is full of reviewed company facts.  They are
        # also present under ``structured_observations`` for prompt context.
        "evidence_fact_ids": _unique([item["id"] for item in facts] + structured["source_record_refs"]),
        "evidence_source_ids": _unique(fact_evidence_ids + structured["source_record_refs"]),
        "evidence_record_refs": structured["source_record_refs"],
        "missing_data": missing,
        "counts": {
            "documents": len(documents), "facts": len(facts), "graph_paths": len(graph_paths),
            "chunks": len(chunks), "chunk_documents": chunk_coverage["documents_with_chunks"],
            "lineage": len(lineage_rows), "datasets": len(datasets),
            "structured": structured["counts"],
            "period_documents": period_document_count,
            "period_disclosures": period_disclosure_count,
            "period_disclosures_included": sum(
                row.get("source_table") in {"stock_news", "stock_notice"} for row in documents
                if row.get("relation_scope") != "RELATED_SECURITY"
            ),
            "period_source_counts": period_source_counts,
            "period_documents_included": len(documents),
            "undated_documents_excluded": undated_documents,
            "related_securities": len(related_securities),
            "related_security_documents": len(related_documents),
        },
    }
    context["context_hash"] = _hash({key: value for key, value in context.items() if key != "context_hash"})
    return context


def context_for_candidate(db: Session, candidate: Any, *, as_of: datetime | None = None) -> dict[str, Any]:
    """Convenience wrapper used by the selection workflow."""
    run = getattr(candidate, "run", None)
    if run is None:
        from app.models.selection import SelectionRun
        run = db.get(SelectionRun, candidate.run_id)
    cutoff = as_of or getattr(run, "as_of", None) or datetime.now(timezone.utc)
    period_months = int(((getattr(run, "criteria_json", {}) or {}).get("period_months") or 3))
    period_start, period_end = selection_period_bounds(cutoff, period_months, candidate.market)
    return build_selection_context(
        db,
        candidate.market,
        candidate.symbol,
        knowledge_base_ids=list(getattr(run, "knowledge_base_ids_json", []) or []),
        graph_ids=list(getattr(run, "graph_ids_json", []) or []),
        as_of=cutoff,
        period_start=period_start,
        period_end=period_end,
        max_documents=80,
    )

