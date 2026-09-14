"""Deterministic hard-filter functions used by the three core Skill SOPs."""

from __future__ import annotations

from collections import Counter
from datetime import datetime, timezone
from typing import Any

from app.schemas.skill_tools import DwValidationRequest, PredictionScoreRequest, WatchFilterRequest


def _aware(value: datetime) -> datetime:
    return value if value.tzinfo else value.replace(tzinfo=timezone.utc)


def validate_dw_records(payload: DwValidationRequest) -> dict[str, Any]:
    required = ("market", "stock_code", "category", "source", "source_record_id", "observed_at")
    category_counts: Counter[str] = Counter()
    seen: set[tuple[str, ...]] = set()
    issues: list[dict[str, Any]] = []
    issue_count = 0
    issue_rows = 0
    now = datetime.now(timezone.utc)
    for index, record in enumerate(payload.records):
        category = str(record.get("category") or "UNKNOWN").upper()
        category_counts[category] += 1
        errors = [f"missing_{field}" for field in required if not str(record.get(field) or "").strip()]
        key = tuple(str(record.get(field) or "") for field in ("market", "stock_code", "category", "source", "source_record_id"))
        if key in seen:
            errors.append("duplicate_business_key")
        else:
            seen.add(key)
        try:
            observed = datetime.fromisoformat(str(record.get("observed_at") or ""))
            if _aware(observed) > now:
                errors.append("future_observed_at")
        except ValueError:
            errors.append("invalid_observed_at")
        facts = record.get("payload_json") or {}
        if isinstance(facts, dict):
            for field in ("price", "close", "open", "volume"):
                if field in facts:
                    try:
                        if float(facts[field]) < 0 or (field != "volume" and float(facts[field]) == 0):
                            errors.append(f"invalid_{field}")
                    except (TypeError, ValueError):
                        errors.append(f"invalid_{field}")
        if errors:
            issue_count += len(errors)
            issue_rows += 1
            if len(issues) < 50:
                issues.append({"row_index": index, "business_key": list(key), "codes": errors})
    return {
        "total_records": len(payload.records),
        "category_counts": dict(category_counts),
        "issue_count": issue_count,
        "quality_issues": issues,
        "quality_issues_truncated": issue_rows > len(issues),
        "dedupe_keys": ["market", "stock_code", "category", "source", "source_record_id"],
        "status": "PENDING_REVIEW" if issue_count else "VALIDATED",
    }


def filter_watch_candidates(payload: WatchFilterRequest) -> dict[str, Any]:
    candidates: list[dict[str, Any]] = []
    invalid_count = 0
    for item in payload.metrics:
        if item.price <= 0 or item.previous_close <= 0 or item.volume < 0 or item.avg_volume_20 <= 0:
            invalid_count += 1
            continue
        change_pct = (item.price / item.previous_close - 1) * 100
        volume_ratio = item.volume / item.avg_volume_20
        triggers = []
        if abs(change_pct) >= payload.min_abs_change_pct:
            triggers.append("PRICE_MOVE")
        if volume_ratio >= payload.min_volume_ratio:
            triggers.append("VOLUME_SPIKE")
        if not triggers:
            continue
        candidates.append({
            "market": item.market,
            "stock_code": item.stock_code,
            "change_pct": round(change_pct, 4),
            "volume_ratio": round(volume_ratio, 4),
            "triggers": triggers,
            "as_of": item.as_of.isoformat(),
            "score": round(abs(change_pct) + volume_ratio, 4),
        })
    candidates.sort(key=lambda item: item["score"], reverse=True)
    return {
        "total_checked": len(payload.metrics),
        "invalid_count": invalid_count,
        "matched_count": len(candidates),
        "candidates": candidates[: payload.max_candidates],
        "truncated": len(candidates) > payload.max_candidates,
    }


def score_prediction_outcomes(payload: PredictionScoreRequest) -> dict[str, Any]:
    now = datetime.now(timezone.utc)
    results: list[dict[str, Any]] = []
    scored = 0
    wins = 0
    for item in payload.outcomes:
        if _aware(item.due_at) > now or item.actual_price is None or item.price_as_of is None or _aware(item.price_as_of) < _aware(item.due_at):
            results.append({"ledger_id": item.ledger_id, "status": "PENDING"})
            continue
        actual_return_pct = (item.actual_price / item.entry_price - 1) * 100
        hit = (
            actual_return_pct > 0 if item.action_type == "BUY"
            else actual_return_pct < 0 if item.action_type == "SELL"
            else abs(actual_return_pct) <= 2.0
        )
        scored += 1
        wins += int(hit)
        results.append({
            "ledger_id": item.ledger_id,
            "status": "EVALUATED",
            "direction_hit": hit,
            "actual_return_pct": round(actual_return_pct, 4),
            "return_gap_pct_points": round(actual_return_pct - item.target_return_pct, 4) if item.target_return_pct is not None else None,
            "price_as_of": item.price_as_of.isoformat(),
            "source": item.source,
        })
    return {
        "evaluated_count": scored,
        "pending_count": len(payload.outcomes) - scored,
        "direction_win_rate": round(wins / scored, 4) if scored else None,
        "results": results,
    }
