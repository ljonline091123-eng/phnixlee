"""Bounded source normalization, archival and explicitly audited acceptance."""

from datetime import date, datetime, timedelta, timezone
import hashlib
import json
import math
from pathlib import Path

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.foundation import FoundationEntity, FoundationEvidence, FoundationFact, FoundationListing, FoundationSecurity
from app.models.company_graph import SecurityClassification
from app.models.market_data import StockSymbol
from app.schemas.foundation import EvidenceCreate, FactReviewCreate
from app.services import company_graph, foundation
from app.services.company_sample import SAMPLE_BATCH, SAMPLE_SECURITIES


RULE_VERSION = "PUBLIC_STRUCTURED_OBSERVATION_V1"
MARKET_CAP_RULE_VERSION = "CNY_MARKET_CAP_ABSOLUTE_V1"


def latest_cache(cache_dir: Path, prefix: str, market: str, symbol: str) -> dict | None:
    paths = sorted(cache_dir.glob(f"{prefix}_{market}_{symbol}_*.json"))
    return json.loads(paths[-1].read_text(encoding="utf-8")) if paths else None


def profile_record(source: dict, stock_id: int, identities: dict[str, dict]) -> dict:
    record = source["records"][0]
    company = dict(record["company"])
    company["properties_json"] = {**company.get("properties_json", {}), "sample_batch": SAMPLE_BATCH,
        "controller_mentions": record.get("controller_mentions", [])}
    facts = []
    for industry in record.get("industries", []):
        facts.append({"source_key": "industry:" + industry["code"], "fact_type": "IN_INDUSTRY",
            "title": "来源行业归属：" + industry["name"],
            "object": {"name": industry["name"], "entity_type": "INDUSTRY", "jurisdiction": "GLOBAL",
                "source_issuer_id": industry["source_issuer_id"]},
            "properties_json": {"taxonomy": industry["taxonomy"], "taxonomy_version": industry["taxonomy_version"],
                "level": industry.get("level"),
                "acceptance_rule": RULE_VERSION, "semantic_scope": "PROVIDER_REPORTED_CLASSIFICATION"}})
    for mention in record.get("controller_mentions", []):
        related = identities.get(mention.get("source_issuer_id"))
        if not related or related["name"] != mention["name"] or mention["mention_type"] != "CONTROLLING_HOLDER":
            continue
        ratio = mention.get("direct_ratio")
        if ratio is not None:
            facts.append({"source_key": "reported-direct-holding:" + related["source_issuer_id"],
                "fact_type": "HOLDS_EQUITY", "direction": "INCOMING",
                "title": "来源披露直接持股：" + related["name"],
                "object": related, "properties_json": {"ratio": ratio,
                    "ratio_basis": mention["ratio_basis"],
                    "acceptance_rule": RULE_VERSION, "semantic_scope": "PROVIDER_REPORTED_DIRECT_RATIO",
                    "temporal_scope": "SOURCE_SNAPSHOT_UNKNOWN_BUSINESS_DATE",
                    "not_beneficial_ownership_verified": True}})
        facts.append({"source_key": "reported-control:" + related["source_issuer_id"], "fact_type": "CONTROLS",
            "direction": "INCOMING", "title": "来源披露控股主体：" + related["name"], "object": related,
            "properties_json": {"control_basis": "EASTMONEY explicit CONTROL_HOLDER and CONTROL_HOLDER_CODE fields; not inferred from ratio",
                "semantic_scope": "SOURCE_REPORTED_CONTROL_REQUIRES_REVIEW"}})
    classifications = [{"dimension": "LEGAL_LISTING_CLASS", "code": "A_SHARE" if record["market"] == "CN_A" else "H_SHARE",
        "label": "A股" if record["market"] == "CN_A" else "H股", "definition_version": "LISTING_IDENTITY_V1",
        "method": "SOURCE_LISTING_IDENTITY", "properties_json": {"acceptance_rule": RULE_VERSION}}]
    # H_SHARE applies here only to explicit mainland incorporated sample issuers.
    if record["market"] == "HK" and company["jurisdiction"] != "CN":
        classifications = []
    for theme in record.get("themes", []):
        classifications.append({"dimension": "THEME", "code": theme["code"], "label": theme["name"],
            "definition_version": theme["taxonomy_version"], "method": "EASTMONEY_SECURITY_CONCEPT_LIST",
            "properties_json": {"taxonomy": theme["taxonomy"],
                "scope": "PROVIDER_SECURITY_TAG_MAY_INCLUDE_TRADING_ELIGIBILITY", "acceptance_rule": RULE_VERSION}})
    proof = record["evidence"]
    return {"source_name": source["source_name"], "source_key": proof["source_key"],
        "source_url": source["source_url"], "source_kind": "AGGREGATOR", "observed_at": source["retrieved_at"],
        "company": company, "security": {"stock_symbol_id": stock_id,
            "share_class": "A_SHARE" if record["market"] == "CN_A" else "H_SHARE" if company["jurisdiction"] == "CN" else "ORDINARY"},
        "evidence": {key: proof.get(key) for key in ("title", "content", "published_at", "available_at")},
        "facts": facts, "classifications": classifications}


def archive_disclosures(db: Session, source: dict, company_id: str, document_kind: str) -> list[str]:
    archived = []
    for record in source.get("records", []):
        annotations = {key: record[key] for key in (
            "text_truncated", "page_count", "matched_search_terms", "disclosure_mentions", "selection_basis"
        ) if key in record}
        content = record.get("content") or json.dumps(record["source_record"], ensure_ascii=False, sort_keys=True)
        source_key = "announcement:" + record["announcement_id"]
        digest = hashlib.sha256(content.encode("utf-8")).hexdigest()
        existing = db.scalar(select(FoundationEvidence).where(FoundationEvidence.source_name == source["source_name"],
            FoundationEvidence.source_key == source_key, FoundationEvidence.content_hash == digest,
            FoundationEvidence.entity_id == company_id))
        if existing:
            metadata = dict(existing.metadata_json or {})
            kinds = list(metadata.get("document_kinds") or ([metadata["document_kind"]] if metadata.get("document_kind") else []))
            if document_kind not in kinds:
                assignments = list(metadata.get("document_kind_assignments") or [
                    {"kind": kind, "recorded_at": foundation.aware(existing.created_at).isoformat()} for kind in kinds
                ])
                kinds.append(document_kind)
                assignments.append({"kind": document_kind, "recorded_at": datetime.now(timezone.utc).isoformat()})
                metadata = {**metadata, "document_kinds": kinds, "document_kind_assignments": assignments}
            # Add extractor annotations without replacing the original text, provenance or prior annotations.
            for key, value in annotations.items():
                if key not in metadata:
                    metadata[key] = value
            if metadata != existing.metadata_json:
                existing.metadata_json = metadata
                db.flush()
            archived.append(existing.id)
            continue
        evidence = foundation.create_evidence(db, EvidenceCreate(entity_id=company_id, source_name=source["source_name"],
            source_key=source_key, title=record["title"], content=content, url=record.get("url"),
            published_at=record.get("published_at"), available_at=source["retrieved_at"]), metadata={
                "document_kind": document_kind, "document_kinds": [document_kind], "source_kind": "OFFICIAL_DISCLOSURE",
                "content_scope": record.get("content_scope", "ANNOUNCEMENT_METADATA_ONLY"),
                "completeness": record.get("extraction_status", "TITLE_ONLY"), "pages": record.get("pages", []),
                "case_mentions": record.get("case_mentions", []), "binary_sha256": record.get("binary_sha256"),
                **annotations,
                "association_role": "DISCLOSING_COMPANY_NOT_CONFIRMED_CASE_PARTY" if document_kind == "LEGAL_DISCLOSURE"
                    else "DISCLOSING_COMPANY_NOT_VERIFIED_TRANSACTION_PARTY",
                "party_resolution": "UNRESOLVED", "request": source["request"],
                "warnings": source.get("warnings", [])})
        archived.append(evidence.id)
    return archived


def holder_record(source: dict, profile: dict, stock_id: int) -> dict | None:
    if not source.get("records"):
        return None
    main = profile["records"][0]
    report_date = source["records"][0]["report_date"]
    key = f"holders:{main['company']['source_issuer_id']}:{report_date}"
    facts = []
    for holder in source["records"]:
        if holder.get("ratio") is None and holder.get("shares") is None:
            continue
        account_key = hashlib.sha256((main["company"]["source_issuer_id"] + ":" + holder["holder_name"]).encode()).hexdigest()
        props = {key: holder[key] for key in ("report_date", "ratio_basis", "shares_unit", "share_class",
            "identity_status", "beneficial_owner_verified") if key in holder}
        props.update({key: holder[key] for key in ("ratio", "shares") if holder.get(key) is not None})
        props.update(semantic_scope="DISCLOSED_REGISTER_ACCOUNT_HOLDING", temporal_scope="AS_OF_OBSERVATION", acceptance_rule=RULE_VERSION,
            not_complete_register=True, holder_rank=holder.get("holder_rank"))
        facts.append({"source_key": account_key, "fact_type": "HOLDS_EQUITY", "direction": "INCOMING",
            "title": f"{report_date} 披露股东账户持仓",
            "object": {"name": holder["holder_name"], "jurisdiction": "UNRESOLVED", "entity_type": "HOLDER_ACCOUNT",
                "source_issuer_id": "holder-account:" + account_key,
                "properties_json": {"identity_status": "UNRESOLVED_REGISTER_HOLDER",
                    "beneficial_owner_verified": False, "disclosing_issuer_id": main["company"]["source_issuer_id"]}},
            "properties_json": props, "valid_from": report_date,
            "valid_to": (date.fromisoformat(report_date) + timedelta(days=1)).isoformat()})
    return {"source_name": source["source_name"], "source_key": key, "source_url": source["source_url"],
        "source_kind": "AGGREGATOR", "observed_at": source["retrieved_at"], "company": main["company"],
        "security": {"stock_symbol_id": stock_id, "share_class": "A_SHARE"},
        "evidence": {"title": f"{main['company']['name']} {report_date} 披露股东账户",
            "content": json.dumps([item["source_record"] for item in source["records"]], ensure_ascii=False, sort_keys=True),
            "available_at": source["retrieved_at"]}, "facts": facts}


def marketcap_record(source: dict, company: dict, stock_id: int, *, market: str, symbol: str) -> dict:
    if source.get("status") != "SUCCESS" or source.get("source_code") != "TENCENT_MARKET_CAP" or source.get("source_name") != "TENCENT":
        raise ValueError("A successful Tencent market-cap source is required")
    if len(source.get("records", [])) != 1:
        raise ValueError("Exactly one market-cap observation is required")
    record = source["records"][0]
    if market != "CN_A" or record.get("market") != market or record.get("symbol") != symbol:
        raise ValueError("Market-cap observation does not match the A-share security")
    if record.get("currency") != "CNY" or record.get("unit") != "CNY":
        raise ValueError("Absolute market-cap thresholds require CNY values")
    if record.get("valuation_scope") != "PROVIDER_ISSUER_TOTAL_SHARES_AT_A_SHARE_PRICE" or record.get("cross_listing_additive") is not False:
        raise ValueError("Market-cap valuation scope must be explicit and non-additive")
    value = record.get("total_market_cap")
    if isinstance(value, bool) or not isinstance(value, (int, float)) or not math.isfinite(value) or value <= 0:
        raise ValueError("Market cap must be a finite positive value")
    snapshot = datetime.fromisoformat(record["snapshot_at"])
    retrieved = datetime.fromisoformat(source["retrieved_at"])
    if snapshot.tzinfo is None or retrieved.tzinfo is None or snapshot > retrieved:
        raise ValueError("Market-cap snapshot must be timezone-aware and no later than retrieval")
    as_of = date.fromisoformat(record["as_of"])
    if as_of != snapshot.astimezone(timezone(timedelta(hours=8))).date():
        raise ValueError("Market-cap business date must match the China-local snapshot date")
    # Tencent supplies no issuer identifier; reuse the already resolved company identity.
    if not company.get("identifier_scheme") or not company.get("identifier_value"):
        raise ValueError("A resolved company identifier is required for cross-source linkage")
    identity = {key: company[key] for key in ("name", "jurisdiction", "identifier_scheme", "identifier_value")}
    code, label = ("LARGE_CAP", "大盘") if value >= 100_000_000_000 else ("MID_CAP", "中盘") if value >= 20_000_000_000 else ("SMALL_CAP", "小盘")
    properties = {**record, "acceptance_rule": RULE_VERSION, "source_code": source["source_code"],
        "classification_basis": "ABSOLUTE_CNY_THRESHOLDS_NOT_PERCENTILES", "extraction_method": "COMPUTED_SOURCE_RULE",
        "thresholds_cny": {"large_cap_min": 100_000_000_000, "mid_cap_min": 20_000_000_000},
        "temporal_scope": "AS_OF_OBSERVATION"}
    return {"source_name": "TENCENT", "source_key": f"marketcap:{market}:{symbol}:{record['snapshot_at']}",
        "source_url": source["source_url"], "source_kind": "AGGREGATOR", "observed_at": source["retrieved_at"],
        "company": identity, "security": {"stock_symbol_id": stock_id, "share_class": "A_SHARE"},
        "evidence": {"title": f"{market}:{symbol} {as_of} 市值快照",
            "content": json.dumps({"record": record, "raw": source.get("raw", {}), "request": source.get("request", {})},
                ensure_ascii=False, sort_keys=True, allow_nan=False),
            "published_at": record["snapshot_at"], "available_at": source["retrieved_at"]},
        "classifications": [{"dimension": "SIZE", "code": code, "label": label,
            "definition_version": MARKET_CAP_RULE_VERSION, "method": "COMPUTED_SOURCE_RULE",
            "properties_json": properties, "valid_from": as_of.isoformat(),
            "valid_to": (as_of + timedelta(days=1)).isoformat()}]}


def accept_source_observations(db: Session, imported: dict) -> dict:
    """Only source-literal, typed records from this normalizer; never control or legal conclusions."""
    accepted = {"facts": 0, "classifications": 0}
    review = FactReviewCreate(decision="ACCEPTED", expected_status="PENDING",
        reviewer="RULE:" + RULE_VERSION,
        reason="Accepted as a literal structured source observation. Source identity, typed values and evidence retained; not official registry verification or inferred causality.")
    computed_review = FactReviewCreate(decision="ACCEPTED", expected_status="PENDING",
        reviewer="RULE:" + MARKET_CAP_RULE_VERSION,
        reason="Accepted by COMPUTED_SOURCE_RULE using explicit CNY absolute thresholds and a one-day provider snapshot; not a percentile classification, human verification, or additive cross-listing valuation.")
    for record in imported["records"]:
        for fact_id in record["fact_ids"]:
            fact = db.get(FoundationFact, fact_id)
            if fact.status == "PENDING" and fact.fact_type in {"IN_INDUSTRY", "HOLDS_EQUITY"} and fact.properties_json.get("acceptance_rule") == RULE_VERSION:
                foundation.review_fact(db, fact.id, review)
                accepted["facts"] += 1
        for classification_id in record["classification_ids"]:
            row = db.get(SecurityClassification, classification_id)
            computed_size = (row.dimension == "SIZE" and row.definition_version == MARKET_CAP_RULE_VERSION
                and row.method == "COMPUTED_SOURCE_RULE" and row.properties_json.get("source_code") == "TENCENT_MARKET_CAP"
                and row.valid_from is not None and row.valid_to == row.valid_from + timedelta(days=1))
            if row.status == "PENDING" and (row.dimension in {"THEME", "LEGAL_LISTING_CLASS"} or computed_size) and row.properties_json.get("acceptance_rule") == RULE_VERSION:
                company_graph.review_classification(db, row.id, computed_review if computed_size else review)
                accepted["classifications"] += 1
    return accepted


def import_cached_sources(db: Session, cache_dir: Path, *, accept_structured: bool = False, samples: list[dict] | None = None) -> dict:
    samples = SAMPLE_SECURITIES if samples is None else samples
    profiles = {(item["market"], item["symbol"]): latest_cache(cache_dir, "profile", **item) for item in samples}
    identities = {record["company"]["source_issuer_id"]: record["company"] for source in profiles.values()
        if source and source.get("status") == "SUCCESS" for record in source.get("records", [])}
    result = {"sample_batch": SAMPLE_BATCH, "requested": len(samples), "mapped": 0, "items": [],
        "rule_version": RULE_VERSION, "accept_structured": accept_structured}
    for sample in samples:
        market, symbol = sample["market"], sample["symbol"]
        item = {"market": market, "symbol": symbol, "sources": {}, "evidence_ids": [], "status": "PENDING"}
        result["items"].append(item)
        stock = db.scalar(select(StockSymbol).where(StockSymbol.market == market, StockSymbol.symbol == symbol))
        source = profiles[market, symbol]
        item["sources"]["profile"] = {"status": source.get("status", "UNAVAILABLE") if source else "NOT_REQUESTED",
            "retrieved_at": source.get("retrieved_at") if source else None,
            "warnings": source.get("warnings", []) if source else ["No company profile was retrieved."]}
        if not stock or not source or source.get("status") != "SUCCESS" or not source.get("records"):
            item["status"] = "STOCK_MISSING" if not stock else "PROFILE_UNAVAILABLE"
            continue
        item["stock_symbol_id"] = stock.id
        imported = company_graph.import_batch(db, {"records": [profile_record(source, stock.id, identities)]})
        mapped = imported["records"][0]
        item.update(company_id=mapped["company_id"], security_id=mapped["security_id"], status="MAPPED")
        item["evidence_ids"].append(mapped["evidence_id"])
        item["accepted"] = accept_source_observations(db, imported) if accept_structured else {}
        result["mapped"] += 1
        holders = latest_cache(cache_dir, "holders", market, symbol)
        item["sources"]["holders"] = {"status": holders["status"] if holders else "NOT_REQUESTED",
            "warnings": holders.get("warnings", []) if holders else []}
        if holders and holders["status"] == "SUCCESS":
            normalized = holder_record(holders, source, stock.id)
            if normalized:
                holding_import = company_graph.import_batch(db, {"records": [normalized]})
                item["evidence_ids"].append(holding_import["records"][0]["evidence_id"])
                if accept_structured:
                    item["accepted_holdings"] = accept_source_observations(db, holding_import)
        marketcap = latest_cache(cache_dir, "marketcap", market, symbol)
        item["sources"]["marketcap"] = {"status": marketcap["status"] if marketcap else "NOT_REQUESTED",
            "warnings": marketcap.get("warnings", []) if marketcap else []}
        if marketcap and marketcap["status"] == "SUCCESS":
            company = db.get(FoundationEntity, mapped["company_id"])
            identity = {key: getattr(company, key) for key in ("name", "jurisdiction", "identifier_scheme", "identifier_value")}
            try:
                normalized = marketcap_record(marketcap, identity, stock.id, market=market, symbol=symbol)
            except (KeyError, TypeError, ValueError, OverflowError) as error:
                item["sources"]["marketcap"].update(status="INVALID", warnings=[str(error)])
            else:
                cap_import = company_graph.import_batch(db, {"records": [normalized]})
                item["evidence_ids"].append(cap_import["records"][0]["evidence_id"])
                if accept_structured:
                    item["accepted_marketcap"] = accept_source_observations(db, cap_import)
        for prefix, kind in (("legal", "LEGAL_DISCLOSURE"), ("business", "BUSINESS_DISCLOSURE"),
                             ("supply_chain", "SUPPLY_CHAIN_DISCLOSURE")):
            disclosure = latest_cache(cache_dir, prefix, market, symbol)
            item["sources"][prefix] = {"status": disclosure["status"] if disclosure else "NOT_REQUESTED",
                "warnings": disclosure.get("warnings", []) if disclosure else [],
                "request": disclosure.get("request", {}) if disclosure else {}}
            if disclosure:
                archived = archive_disclosures(db, disclosure, mapped["company_id"], kind)
                item["evidence_ids"].extend(archived)
                item["sources"][prefix]["disclosure_count"] = len(archived)
    result["source_status"] = "PARTIAL" if any(item["status"] != "MAPPED" for item in result["items"]) or any(
        entry.get("status") not in {"SUCCESS", "EMPTY", "NOT_REQUESTED"}
        for item in result["items"] for entry in item["sources"].values()) else "COMPLETE_WITH_COVERAGE_LIMITS"
    db.flush()
    return result
