from __future__ import annotations

from typing import Final

MARKET_CN_A: Final[str] = "CN_A"
MARKET_HK: Final[str] = "HK"
MARKET_NEEQ: Final[str] = "NEEQ"
MARKET_NEEQ_INNOVATION: Final[str] = "NEEQ_INNOVATION"

MASTER_MARKETS: Final[tuple[str, ...]] = (
    MARKET_CN_A,
    MARKET_HK,
    MARKET_NEEQ,
    MARKET_NEEQ_INNOVATION,
)

LIVE_REFRESH_MARKETS: Final[tuple[str, ...]] = (
    MARKET_CN_A,
    MARKET_HK,
    MARKET_NEEQ,
    MARKET_NEEQ_INNOVATION,
)

MARKET_SYMBOL_DIGITS: Final[dict[str, int]] = {
    MARKET_CN_A: 6,
    MARKET_HK: 5,
    MARKET_NEEQ: 6,
    MARKET_NEEQ_INNOVATION: 6,
}

MARKET_LABELS: Final[dict[str, str]] = {
    MARKET_CN_A: "A股",
    MARKET_HK: "港股",
    MARKET_NEEQ: "新三板",
    MARKET_NEEQ_INNOVATION: "创新层",
}


def normalize_market(market: str) -> str:
    return str(market or "").strip().upper()


def is_master_market(market: str) -> bool:
    return normalize_market(market) in MASTER_MARKETS


def is_live_refresh_market(market: str) -> bool:
    return normalize_market(market) in LIVE_REFRESH_MARKETS


def digit_length_for_market(market: str) -> int:
    return MARKET_SYMBOL_DIGITS.get(normalize_market(market), 6)
