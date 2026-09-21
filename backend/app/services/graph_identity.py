"""Shared identities for the legacy evidence graph and the company graph.

Legacy integer IDs and source keys remain local graph identifiers.  Canonical
security identifiers deliberately do not depend on whether an issuer has been
resolved yet; an issuer UUID is exposed only through an existing listing.
"""

from collections.abc import Iterable
from datetime import datetime, timezone
from typing import Any

from sqlalchemy import inspect, select, tuple_
from sqlalchemy.orm import Session

from app.models.ai_hub import KnowledgeEntity
from app.models.foundation import FoundationEntity, FoundationEvidence, FoundationListing, FoundationSecurity
from app.models.market_data import StockSymbol


IDENTITY_VERSION = "SECURITY_COMPANY_V1"
IDENTITY_FIELDS = (
    "canonical_security_id", "canonical_company_id", "security_id", "company_id",
    "stock_symbol_id", "listing_id", "mapping_status", "identity_version",
)
Pair = tuple[str, str]


def normalize_pair(market: str | None, symbol: str | None) -> Pair | None:
    """Normalize spelling only: never collapse markets or strip code zeroes."""
    pair = (str(market or "").strip().upper(), str(symbol or "").strip().upper())
    return pair if all(pair) and not any(":" in part for part in pair) else None


def canonical_security_id(market: str, symbol: str) -> str:
    pair = normalize_pair(market, symbol)
    if pair is None:
        raise ValueError("canonical security identity requires an explicit market and symbol")
    return f"security:{pair[0]}:{pair[1]}"


def canonical_company_id(company_id: str) -> str:
    return f"company:{company_id}"


def resolve_many(db: Session, pairs: Iterable[tuple[str | None, str | None]], *,
                 known_at: datetime | None = None) -> dict[Pair, dict[str, Any]]:
    """Resolve a batch without changing data or requiring migrated legacy DBs.

    ``known_at`` bounds both the recorded mapping and its evidence. Missing or
    future mappings never imply a company identity. NEEQ and its innovation
    segment remain separate until a source explicitly resolves their identity.
    """
    normalized = sorted({pair for market, symbol in pairs if (pair := normalize_pair(market, symbol))})
    result = {pair: {
        "canonical_security_id": canonical_security_id(*pair),
        "canonical_company_id": None, "security_id": None, "company_id": None,
        "stock_symbol_id": None, "listing_id": None, "mapping_status": "UNMAPPED",
        "identity_version": IDENTITY_VERSION,
    } for pair in normalized}
    if not normalized:
        return result
    cutoff = known_at or datetime.now(timezone.utc)
    # Inspect the session connection, not a second pooled connection: legacy
    # in-memory tests and callers may have an active uncommitted transaction.
    tables = set(inspect(db.connection()).get_table_names())
    has_stocks = StockSymbol.__tablename__ in tables
    has_foundation = {model.__tablename__ for model in (
        FoundationListing, FoundationSecurity, FoundationEntity, FoundationEvidence,
    )}.issubset(tables)
    for start in range(0, len(normalized), 300):
        batch = normalized[start:start + 300]
        if has_stocks:
            stock_query = select(StockSymbol.id, StockSymbol.market, StockSymbol.symbol).where(
                tuple_(StockSymbol.market, StockSymbol.symbol).in_(batch), StockSymbol.created_at <= cutoff)
            for stock in db.execute(stock_query):
                result[(stock.market, stock.symbol)]["stock_symbol_id"] = stock.id
        if not has_foundation:
            continue
        query = select(FoundationListing, FoundationSecurity.entity_id).join(
            FoundationSecurity, FoundationSecurity.id == FoundationListing.security_id).join(
            FoundationEntity, FoundationEntity.id == FoundationSecurity.entity_id).join(
            FoundationEvidence, FoundationEvidence.id == FoundationListing.evidence_id).where(
            tuple_(FoundationListing.market, FoundationListing.symbol).in_(batch),
            FoundationEntity.entity_type == "COMPANY")
        query = query.where(FoundationListing.created_at <= cutoff,
            FoundationSecurity.created_at <= cutoff, FoundationEntity.created_at <= cutoff,
            FoundationEvidence.created_at <= cutoff, FoundationEvidence.available_at <= cutoff)
        matched: set[Pair] = set()
        for listing, entity_id in db.execute(query):
            pair = (listing.market, listing.symbol)
            item = result[pair]
            if pair in matched:
                # Corrupt/imported duplicate listing keys must not silently pick
                # a company based on database row order.
                item.update(canonical_company_id=None, security_id=None, company_id=None,
                    listing_id=None, mapping_status="CONFLICT")
                continue
            matched.add(pair)
            item.update(canonical_company_id=canonical_company_id(entity_id),
                security_id=listing.security_id, company_id=entity_id,
                stock_symbol_id=listing.stock_symbol_id, listing_id=listing.id,
                mapping_status="MAPPED")
    return result


def legacy_stock_pair(node: KnowledgeEntity) -> Pair | None:
    if node.entity_type != "STOCK":
        return None
    props = node.properties_json or {}
    pair = normalize_pair(props.get("market"), props.get("symbol"))
    # Older stock nodes sometimes stored their pair only in graph:market:symbol.
    parts = node.entity_key.split(":")
    key_pair = normalize_pair(parts[1], parts[2]) if len(parts) == 3 and parts[0] == str(node.graph_id) else None
    if pair and key_pair and pair != key_pair:
        return None
    return pair or key_pair


def enrich_legacy_nodes(db: Session, nodes: Iterable[KnowledgeEntity], *,
                        known_at: datetime | None = None) -> list[dict[str, Any]]:
    """Serialize current aliases at read time without mutating stored evidence."""
    nodes = list(nodes)
    pairs = {node.id: legacy_stock_pair(node) for node in nodes}
    identities = resolve_many(db, (pair for pair in pairs.values() if pair), known_at=known_at)
    result = []
    for node in nodes:
        identity = identities.get(pairs[node.id], {})
        if node.entity_type == "STOCK" and not identity:
            identity = {**dict.fromkeys(IDENTITY_FIELDS), "mapping_status": "UNRESOLVED",
                "identity_version": IDENTITY_VERSION}
        result.append({"id": node.id, "type": node.entity_type, "key": node.entity_key,
            "name": node.entity_name, "properties": {**(node.properties_json or {}), **identity}, **identity})
    return result


def backfill_legacy_identity(db: Session, *, apply: bool = False, batch_size: int = 300) -> dict[str, int]:
    """Only add/refresh identity properties; caller controls commit and backup."""
    if not 1 <= batch_size <= 1000:
        raise ValueError("batch_size must be between 1 and 1000")
    counts = {"scanned": 0, "updated": 0, "unchanged": 0, "unresolved": 0, "mapped": 0}
    last_id = 0
    while True:
        nodes = list(db.scalars(select(KnowledgeEntity).where(
            KnowledgeEntity.entity_type == "STOCK", KnowledgeEntity.id > last_id
        ).order_by(KnowledgeEntity.id).limit(batch_size)))
        if not nodes:
            break
        last_id = nodes[-1].id
        reads = enrich_legacy_nodes(db, nodes)
        for node, item in zip(nodes, reads):
            counts["scanned"] += 1
            if not item.get("canonical_security_id"):
                counts["unresolved"] += 1
                continue
            if item.get("mapping_status") == "MAPPED":
                counts["mapped"] += 1
            if node.properties_json == item["properties"]:
                counts["unchanged"] += 1
            else:
                counts["updated"] += 1
                if apply:
                    node.properties_json = item["properties"]
        if apply:
            db.flush()
    return counts
