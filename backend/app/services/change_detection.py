"""L0 deterministic change detection.

Pure, replayable detectors (no LLM, no network) plus a thin service that
scans the watch pool, evaluates every detector and stores deduplicated
``stock_change_event`` rows.

Design rules (see docs/l0-event-catalog-and-semantic-path.md):

* detectors are pure functions over ascending daily bars / dated text rows;
* all thresholds come from :mod:`app.services.l0_profile`;
* one broken symbol must never stop the batch;
* re-running the same input is idempotent (dedup key per stock/type/day).
"""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import date, timedelta
from typing import Any, Sequence

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.change_event import StockChangeEvent
from app.models.market_data import StockKline, StockNews, StockNotice, WatchlistItem
from app.services.l0_profile import L0_PROFILE, L0_PROFILE_VERSION


# how many recent daily bars are loaded per symbol (covers the 20-bar window)
KLINE_LOOKBACK_BARS = 60
# text lookback buffer = max cluster window (3 days) + slack
TEXT_LOOKBACK_DAYS = 7
# stale-data guard: skip events whose bar is older than this many days
FRESHNESS_LIMIT_DAYS = 10
MAX_ERROR_SAMPLES = 10


@dataclass(frozen=True, slots=True)
class KlineBar:
    """One daily bar; detector input is always ascending by ``trade_date``."""

    trade_date: str
    close: float | None
    volume: float | None
    high: float | None = None
    low: float | None = None
    open: float | None = None


@dataclass(slots=True)
class ChangeEventDraft:
    """Detector output before persistence (no DB identifiers involved)."""

    event_type: str
    event_date: str
    direction: str  # UP / DOWN / NEUTRAL
    signal: dict[str, Any] = field(default_factory=dict)
    source_ids: dict[str, Any] = field(default_factory=dict)


def _pct_change(current: float, previous: float) -> float | None:
    if not previous:
        return None
    return (current / previous - 1) * 100


def _tail_values(bars: Sequence[KlineBar], size: int, attr: str) -> list[float] | None:
    """Return the last ``size`` values of ``attr`` or None if any is missing."""
    tail = bars[-size:]
    if len(tail) < size:
        return None
    values: list[float] = []
    for bar in tail:
        value = getattr(bar, attr)
        if value is None:
            return None
        values.append(float(value))
    return values


# ---------------------------------------------------------------------------
# A 组 · price/volume detectors
# ---------------------------------------------------------------------------

def detect_price_move(bars: Sequence[KlineBar], *, min_abs_change_pct: float) -> ChangeEventDraft | None:
    """A1 PRICE_MOVE: |close / previous_close - 1| >= threshold."""
    if len(bars) < 2:
        return None
    today, previous = bars[-1], bars[-2]
    if today.close is None or previous.close is None:
        return None
    change_pct = _pct_change(today.close, previous.close)
    if change_pct is None or abs(change_pct) < min_abs_change_pct:
        return None
    return ChangeEventDraft(
        event_type="PRICE_MOVE",
        event_date=today.trade_date,
        direction="UP" if change_pct > 0 else "DOWN",
        signal={
            "change_pct": round(change_pct, 4),
            "close": today.close,
            "previous_close": previous.close,
            "threshold_pct": min_abs_change_pct,
        },
    )


def detect_volume_spike(bars: Sequence[KlineBar], *, min_volume_ratio: float, window: int) -> ChangeEventDraft | None:
    """A2 VOLUME_SPIKE: volume / mean(previous N volumes) >= threshold."""
    values = _tail_values(bars, window + 1, "volume")
    if values is None:
        return None
    today_volume = values[-1]
    history = values[:-1]
    average = sum(history) / len(history)
    if average <= 0:
        return None
    ratio = today_volume / average
    if ratio < min_volume_ratio:
        return None
    return ChangeEventDraft(
        event_type="VOLUME_SPIKE",
        event_date=bars[-1].trade_date,
        direction="NEUTRAL",
        signal={
            "volume_ratio": round(ratio, 4),
            "volume": today_volume,
            "average_volume": round(average, 2),
            "window": window,
            "threshold_ratio": min_volume_ratio,
        },
    )


def detect_breakout_20d(bars: Sequence[KlineBar], *, window: int) -> ChangeEventDraft | None:
    """A3 BREAKOUT_20D: close strictly above prior-window high / below prior-window low."""
    highs = _tail_values(bars, window + 1, "high")
    lows = _tail_values(bars, window + 1, "low")
    today_close = bars[-1].close if bars else None
    if highs is None or lows is None or today_close is None:
        return None
    prior_high = max(highs[:-1])
    prior_low = min(lows[:-1])
    if today_close > prior_high:
        return ChangeEventDraft(
            event_type="BREAKOUT_20D",
            event_date=bars[-1].trade_date,
            direction="UP",
            signal={"mode": "HIGH", "close": today_close, "prior_high": prior_high, "window": window},
        )
    if today_close < prior_low:
        return ChangeEventDraft(
            event_type="BREAKOUT_20D",
            event_date=bars[-1].trade_date,
            direction="DOWN",
            signal={"mode": "LOW", "close": today_close, "prior_low": prior_low, "window": window},
        )
    return None


def detect_ma_cross(bars: Sequence[KlineBar], *, fast_window: int, slow_window: int) -> ChangeEventDraft | None:
    """A4 MA_CROSS: fast moving average crossing the slow one on the last bar."""
    closes = _tail_values(bars, slow_window + 1, "close")
    if closes is None:
        return None
    fast_today = sum(closes[-fast_window:]) / fast_window
    slow_today = sum(closes[-slow_window:]) / slow_window
    fast_previous = sum(closes[-(fast_window + 1):-1]) / fast_window
    slow_previous = sum(closes[-(slow_window + 1):-1]) / slow_window
    event_type = f"MA_CROSS_{fast_window}_{slow_window}"
    if fast_previous <= slow_previous and fast_today > slow_today:
        return ChangeEventDraft(
            event_type=event_type,
            event_date=bars[-1].trade_date,
            direction="UP",
            signal={
                "fast_ma": round(fast_today, 4),
                "slow_ma": round(slow_today, 4),
                "fast_window": fast_window,
                "slow_window": slow_window,
            },
        )
    if fast_previous >= slow_previous and fast_today < slow_today:
        return ChangeEventDraft(
            event_type=event_type,
            event_date=bars[-1].trade_date,
            direction="DOWN",
            signal={
                "fast_ma": round(fast_today, 4),
                "slow_ma": round(slow_today, 4),
                "fast_window": fast_window,
                "slow_window": slow_window,
            },
        )
    return None


# ---------------------------------------------------------------------------
# B 组 · text presence detectors (rows given as (row_id, date_string))
# ---------------------------------------------------------------------------

def _in_window(day: str, event_date: str, window_days: int) -> bool:
    try:
        current = date.fromisoformat(str(day)[:10])
        end = date.fromisoformat(str(event_date)[:10])
    except (TypeError, ValueError):
        return False
    start = end - timedelta(days=max(window_days - 1, 0))
    return start <= current <= end


def detect_notice_new(notice_items: Sequence[tuple[int, str]], *, event_date: str) -> ChangeEventDraft | None:
    """B1 NOTICE_NEW: at least one notice dated exactly on the event date."""
    matched = [row_id for row_id, day in notice_items if str(day)[:10] == str(event_date)[:10]]
    if not matched:
        return None
    return ChangeEventDraft(
        event_type="NOTICE_NEW",
        event_date=event_date,
        direction="NEUTRAL",
        signal={"notice_count": len(matched)},
        source_ids={"notice_ids": matched},
    )


def detect_notice_cluster(
    notice_items: Sequence[tuple[int, str]], *, min_count: int, window_days: int, event_date: str
) -> ChangeEventDraft | None:
    """B2 NOTICE_CLUSTER: notice count within the trailing window >= threshold."""
    matched = [row_id for row_id, day in notice_items if _in_window(day, event_date, window_days)]
    if len(matched) < min_count:
        return None
    return ChangeEventDraft(
        event_type="NOTICE_CLUSTER",
        event_date=event_date,
        direction="NEUTRAL",
        signal={"notice_count": len(matched), "window_days": window_days, "threshold_count": min_count},
        source_ids={"notice_ids": matched},
    )


def detect_news_cluster(
    news_items: Sequence[tuple[int, str]], *, min_count: int, window_days: int, event_date: str
) -> ChangeEventDraft | None:
    """B3 NEWS_CLUSTER: news count within the trailing window >= threshold."""
    matched = [row_id for row_id, day in news_items if _in_window(day, event_date, window_days)]
    if len(matched) < min_count:
        return None
    return ChangeEventDraft(
        event_type="NEWS_CLUSTER",
        event_date=event_date,
        direction="NEUTRAL",
        signal={"news_count": len(matched), "window_days": window_days, "threshold_count": min_count},
        source_ids={"news_ids": matched},
    )


# ---------------------------------------------------------------------------
# Evaluation (pure) and persistence (DB)
# ---------------------------------------------------------------------------

def evaluate_symbol_changes(
    *,
    bars: Sequence[KlineBar],
    notice_items: Sequence[tuple[int, str]] = (),
    news_items: Sequence[tuple[int, str]] = (),
    event_date: str,
    profile: dict[str, dict[str, Any]] = L0_PROFILE,
) -> list[ChangeEventDraft]:
    """Run every detector for one symbol. Pure and replayable."""
    drafts: list[ChangeEventDraft] = []
    for detector, params in (
        (detect_price_move, profile["price_move"]),
        (detect_volume_spike, profile["volume_spike"]),
        (detect_breakout_20d, profile["breakout_20d"]),
        (detect_ma_cross, profile["ma_cross"]),
    ):
        draft = detector(bars, **params)
        if draft is not None:
            drafts.append(draft)
    if profile["notice_new"].get("enabled", True):
        draft = detect_notice_new(notice_items, event_date=event_date)
        if draft is not None:
            drafts.append(draft)
    draft = detect_notice_cluster(notice_items, event_date=event_date, **profile["notice_cluster"])
    if draft is not None:
        drafts.append(draft)
    draft = detect_news_cluster(news_items, event_date=event_date, **profile["news_cluster"])
    if draft is not None:
        drafts.append(draft)
    return drafts


def build_dedup_key(market: str, symbol: str, event_type: str, event_date: str) -> str:
    return f"{market}:{symbol}:{event_type}:{event_date}"


def _load_bars(db: Session, market: str, symbol: str, *, as_of: str | None = None) -> list[KlineBar]:
    stmt = select(StockKline).where(
        StockKline.market == market,
        StockKline.symbol == symbol,
        StockKline.period == "daily",
        StockKline.adjust == "",
    )
    if as_of:
        stmt = stmt.where(StockKline.trade_date <= as_of)
    stmt = stmt.order_by(StockKline.trade_date.desc()).limit(KLINE_LOOKBACK_BARS)
    rows = list(db.scalars(stmt).all())
    rows.reverse()
    return [
        KlineBar(
            trade_date=str(row.trade_date),
            close=row.close_price,
            volume=row.volume,
            high=row.high_price,
            low=row.low_price,
            open=row.open_price,
        )
        for row in rows
    ]


def _load_notice_items(db: Session, market: str, symbol: str, *, from_date: str, to_date: str) -> list[tuple[int, str]]:
    rows = db.execute(
        select(StockNotice.id, StockNotice.notice_date).where(
            StockNotice.market == market,
            StockNotice.symbol == symbol,
            StockNotice.notice_date >= from_date,
            StockNotice.notice_date <= to_date,
        )
    ).all()
    return [(int(row[0]), str(row[1])) for row in rows]


def _load_news_items(db: Session, market: str, symbol: str, *, from_date: str, to_date: str) -> list[tuple[int, str]]:
    rows = db.execute(
        select(StockNews.id, StockNews.news_time).where(
            StockNews.market == market,
            StockNews.symbol == symbol,
            StockNews.news_time >= from_date,
            StockNews.news_time <= to_date,
        )
    ).all()
    return [(int(row[0]), str(row[1])) for row in rows]


def collect_symbol_drafts(
    db: Session, *, market: str, symbol: str, as_of: str | None = None
) -> tuple[int, str | None, list[ChangeEventDraft]]:
    """Load data and evaluate detectors.

    Returns ``(bars_loaded, latest_bar_date, drafts)``.
    """
    bars = _load_bars(db, market, symbol, as_of=as_of)
    if len(bars) < 2:
        return len(bars), None, []
    event_date = bars[-1].trade_date
    try:
        from_day = (date.fromisoformat(event_date[:10]) - timedelta(days=TEXT_LOOKBACK_DAYS)).isoformat()
    except ValueError:
        return len(bars), None, []
    notices = _load_notice_items(db, market, symbol, from_date=from_day, to_date=event_date)
    news = _load_news_items(db, market, symbol, from_date=from_day, to_date=event_date)
    drafts = evaluate_symbol_changes(
        bars=bars,
        notice_items=notices,
        news_items=news,
        event_date=event_date,
    )
    return len(bars), event_date, drafts


def detect_symbol_changes(db: Session, *, market: str, symbol: str, as_of: str | None = None) -> list[ChangeEventDraft]:
    """Convenience wrapper used by tools and tests."""
    return collect_symbol_drafts(db, market=market, symbol=symbol, as_of=as_of)[2]


def persist_drafts(
    db: Session, *, market: str, symbol: str, drafts: Sequence[ChangeEventDraft]
) -> tuple[int, int]:
    """Insert new drafts, skipping existing dedup keys. Returns (created, duplicates)."""
    created = duplicates = 0
    for draft in drafts:
        key = build_dedup_key(market, symbol, draft.event_type, draft.event_date)
        existing = db.scalar(select(StockChangeEvent.id).where(StockChangeEvent.dedup_key == key))
        if existing is not None:
            duplicates += 1
            continue
        db.add(
            StockChangeEvent(
                market=market,
                symbol=symbol,
                event_date=draft.event_date,
                event_type=draft.event_type,
                direction=draft.direction,
                signal_json=draft.signal,
                source_ids_json=draft.source_ids,
                dedup_key=key,
                profile_version=L0_PROFILE_VERSION,
                status="DETECTED",
            )
        )
        created += 1
    return created, duplicates


def run_change_detection(
    db: Session, *, as_of: str | None = None, market: str | None = None, include_details: bool = False
) -> dict[str, Any]:
    """Scan the watch pool, detect changes and persist deduplicated events.

    ``as_of`` limits bars to dates <= as_of (deterministic replay of a past
    business date); when omitted, all available bars are used.
    ``include_details`` adds a per-symbol ``symbols`` list describing every
    pool member's scan result (why it was skipped or which events it fired).
    """
    stmt = select(WatchlistItem).order_by(WatchlistItem.market, WatchlistItem.symbol)
    if market:
        stmt = stmt.where(WatchlistItem.market == market)
    pool = list(db.scalars(stmt).all())

    result: dict[str, Any] = {
        "workflow": "l0_change_detection",
        "profile_version": L0_PROFILE_VERSION,
        "scanned": 0,
        "skipped_no_kline": 0,
        "skipped_stale": 0,
        "no_events": 0,
        "events_created": 0,
        "duplicates": 0,
        "error_count": 0,
        "errors": [],
    }

    try:
        cutoff = date.fromisoformat((as_of or date.today().isoformat())[:10])
    except ValueError:
        cutoff = None

    symbols: list[dict[str, Any]] = []
    for item in pool:
        result["scanned"] += 1
        entry: dict[str, Any] = {"market": item.market, "symbol": item.symbol, "status": "ERROR"}
        try:
            bars_loaded, event_date, drafts = collect_symbol_drafts(
                db, market=item.market, symbol=item.symbol, as_of=as_of
            )
            entry["event_date"] = event_date
            if bars_loaded < 2 or event_date is None:
                result["skipped_no_kline"] += 1
                entry["status"] = "SKIPPED_NO_KLINE"
            elif cutoff is not None and _is_stale(event_date, cutoff):
                result["skipped_stale"] += 1
                entry["status"] = "SKIPPED_STALE"
            elif not drafts:
                result["no_events"] += 1
                entry["status"] = "NO_EVENTS"
            else:
                created, duplicates = persist_drafts(db, market=item.market, symbol=item.symbol, drafts=drafts)
                db.commit()
                result["events_created"] += created
                result["duplicates"] += duplicates
                entry["status"] = "EVENTS"
                entry["event_types"] = sorted({draft.event_type for draft in drafts})
                entry["created"] = created
                entry["duplicates"] = duplicates
        except Exception as exc:  # one bad symbol must never stop the batch
            db.rollback()
            result["error_count"] += 1
            entry["status"] = "ERROR"
            entry["error"] = f"{exc.__class__.__name__}: {exc}"
            if len(result["errors"]) < MAX_ERROR_SAMPLES:
                result["errors"].append(f"{item.market}:{item.symbol}: {entry['error']}")
        if include_details:
            symbols.append(entry)
    if include_details:
        result["symbols"] = symbols
    return result


def _is_stale(event_date: str, cutoff: date) -> bool:
    try:
        return (cutoff - date.fromisoformat(event_date[:10])).days > FRESHNESS_LIMIT_DAYS
    except ValueError:
        return True
