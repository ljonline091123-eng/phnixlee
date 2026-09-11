from __future__ import annotations

from contextlib import contextmanager
from collections.abc import Iterator
from typing import Any

from sqlalchemy import or_, select
from sqlalchemy.orm import Session

from app.core.markets import MASTER_MARKETS, digit_length_for_market, normalize_market
from app.db.session import SessionLocal
from app.models.market_data import StockSymbol


@contextmanager
def managed_session(db: Session | None = None) -> Iterator[Session]:
    """Use the request session when supplied, otherwise create a short-lived one."""

    if db is not None:
        yield db
        return
    local_db = SessionLocal()
    try:
        yield local_db
    finally:
        local_db.close()


def normalize_symbol(market: str, symbol: str) -> str:
    text = str(symbol or "").strip().upper()
    if text.isdigit():
        return text.zfill(digit_length_for_market(market))
    return text


def resolve_stock(
    db: Session,
    symbol: str,
    market: str | None = None,
) -> StockSymbol:
    """Resolve a code or a fuzzy name against the locally stored master data."""

    query = str(symbol or "").strip()
    if not query:
        raise ValueError("symbol is required")

    markets: tuple[str, ...]
    if market:
        normalized_market = normalize_market(market)
        if normalized_market not in MASTER_MARKETS:
            raise ValueError(f"market must be one of: {', '.join(MASTER_MARKETS)}")
        markets = (normalized_market,)
    else:
        markets = tuple(MASTER_MARKETS)

    candidates: list[StockSymbol] = []
    if query.isdigit():
        for candidate_market in markets:
            normalized = normalize_symbol(candidate_market, query)
            item = db.scalar(
                select(StockSymbol).where(
                    StockSymbol.market == candidate_market,
                    StockSymbol.symbol == normalized,
                )
            )
            if item:
                candidates.append(item)

    if not candidates:
        pattern = f"%{query}%"
        candidates = list(
            db.scalars(
                select(StockSymbol).where(
                    StockSymbol.market.in_(markets),
                    or_(
                        StockSymbol.symbol.ilike(pattern),
                        StockSymbol.name.ilike(pattern),
                    ),
                )
            ).all()
        )

    if not candidates:
        raise ValueError(f"stock not found in local master data: {query}")

    upper_query = query.upper()

    def rank(item: StockSymbol) -> tuple[int, int, int, str]:
        normalized = normalize_symbol(item.market, query) if query.isdigit() else upper_query
        exact_code = item.symbol.upper() == normalized or item.symbol.upper() == upper_query
        exact_name = (item.name or "").strip().upper() == upper_query
        starts_code = item.symbol.upper().startswith(upper_query)
        starts_name = (item.name or "").strip().upper().startswith(upper_query)
        return (
            0 if exact_code else 1 if exact_name else 2 if starts_code else 3 if starts_name else 4,
            0 if item.status in {"LISTED", "IPO"} else 1,
            0 if item.market == "CN_A" else 1,
            item.symbol,
        )

    return sorted(candidates, key=rank)[0]


def number(value: Any) -> float | None:
    if isinstance(value, bool) or value is None:
        return None
    try:
        parsed = float(value)
    except (TypeError, ValueError):
        return None
    return parsed if parsed == parsed and abs(parsed) != float("inf") else None


def first_number(mapping: dict[str, Any], keys: tuple[str, ...]) -> float | None:
    for key in keys:
        value = number(mapping.get(key))
        if value is not None:
            return value
    lowered = {str(key).lower(): value for key, value in mapping.items()}
    for key in keys:
        value = number(lowered.get(key.lower()))
        if value is not None:
            return value
    return None


def first_matching_number(mapping: dict[str, Any], fragments: tuple[str, ...]) -> float | None:
    """Find a numeric value when an upstream provider changed the column label."""

    for key, value in mapping.items():
        key_text = str(key).lower()
        if all(fragment.lower() in key_text for fragment in fragments):
            parsed = number(value)
            if parsed is not None:
                return parsed
    return None


def compact_value(value: Any, limit: int = 180) -> Any:
    if isinstance(value, str) and len(value) > limit:
        return f"{value[:limit]}..."
    if isinstance(value, dict):
        return {str(key): compact_value(item, limit) for key, item in value.items()}
    if isinstance(value, list):
        return [compact_value(item, limit) for item in value[:50]]
    return value
