"""Deterministic hard-filter functions used by the three core Skill SOPs."""

from __future__ import annotations

from collections import Counter
from datetime import datetime, timezone
from typing import Any

from app.schemas.skill_tools import PredictionScoreRequest, WatchFilterRequest


def _aware(value: datetime) -> datetime:
    return value if value.tzinfo else value.replace(tzinfo=timezone.utc)


def _record_id(record: dict[str, Any], index: int) -> str:
    return str(record.get("source_record_id") or "").strip() or f"BATCH-{index + 1:04d}"


def _quality_issue(
    *, record_id: str, field_name: str, issue_type: str, severity: str, evidence_text: str
) -> dict[str, str]:
    return {
        "record_id": record_id,
        "field_name": field_name,
        "issue_type": issue_type,
        "severity": severity,
        "evidence_text": evidence_text,
    }


def validate_dw_records(
    records: list[dict[str, Any]], *, as_of: datetime | None = None
) -> dict[str, Any]:
    if not isinstance(records, list):
        raise TypeError("records must be a list")
    if len(records) > 5000:
        raise ValueError("records cannot contain more than 5000 items")

    required = ("market", "stock_code", "category", "source", "source_record_id", "observed_at")
    category_counts: Counter[str] = Counter()
    seen: set[tuple[str, ...]] = set()
    issues: list[dict[str, Any]] = []
    issue_record_ids: list[str] = []
    effective_as_of = _aware(as_of or datetime.now(timezone.utc))

    for index, record in enumerate(records):
        if not isinstance(record, dict):
            record = {}
        record_id = _record_id(record, index)
        category = str(record.get("category") or "UNKNOWN").upper()
        category_counts[category] += 1
        record_issues: list[dict[str, str]] = []
        for field in required:
            if str(record.get(field) or "").strip():
                continue
            issue_type = "MISSING_SOURCE_RECORD_ID" if field == "source_record_id" else "MISSING_REQUIRED_FIELD"
            record_issues.append(_quality_issue(
                record_id=record_id,
                field_name=field,
                issue_type=issue_type,
                severity="HIGH",
                evidence_text=f"record[{index}].{field} is empty",
            ))

        key = tuple(str(record.get(field) or "") for field in ("market", "stock_code", "category", "source", "source_record_id"))
        if key in seen:
            record_issues.append(_quality_issue(
                record_id=record_id,
                field_name="market,stock_code,category,source,source_record_id",
                issue_type="DUPLICATE_BUSINESS_KEY",
                severity="HIGH",
                evidence_text=f"record[{index}] repeats an earlier validated business key",
            ))
        else:
            seen.add(key)

        try:
            observed_value = record.get("observed_at")
            observed = observed_value if isinstance(observed_value, datetime) else datetime.fromisoformat(str(observed_value or ""))
            if _aware(observed) > effective_as_of:
                record_issues.append(_quality_issue(
                    record_id=record_id,
                    field_name="observed_at",
                    issue_type="FUTURE_OBSERVED_AT",
                    severity="MEDIUM",
                    evidence_text=f"record[{index}].observed_at is later than batch as_of",
                ))
        except (TypeError, ValueError):
            if str(record.get("observed_at") or "").strip():
                record_issues.append(_quality_issue(
                    record_id=record_id,
                    field_name="observed_at",
                    issue_type="INVALID_OBSERVED_AT",
                    severity="HIGH",
                    evidence_text=f"record[{index}].observed_at is not a valid ISO 8601 datetime",
                ))

        facts = record.get("payload_json") or {}
        if isinstance(facts, dict):
            for field in ("price", "close", "open", "volume"):
                if field in facts:
                    try:
                        if float(facts[field]) < 0 or (field != "volume" and float(facts[field]) == 0):
                            record_issues.append(_quality_issue(
                                record_id=record_id,
                                field_name=f"payload_json.{field}",
                                issue_type=f"INVALID_{field.upper()}",
                                severity="HIGH",
                                evidence_text=f"record[{index}].payload_json.{field} is outside the accepted range",
                            ))
                    except (TypeError, ValueError):
                        record_issues.append(_quality_issue(
                            record_id=record_id,
                            field_name=f"payload_json.{field}",
                            issue_type=f"INVALID_{field.upper()}",
                            severity="HIGH",
                            evidence_text=f"record[{index}].payload_json.{field} is not numeric",
                        ))

        if record_issues and record_id not in issue_record_ids:
            issue_record_ids.append(record_id)
        issues.extend(record_issues)

    issue_samples = issues[:50]
    duplicate_count = sum(item["issue_type"] == "DUPLICATE_BUSINESS_KEY" for item in issues)
    return {
        "as_of": effective_as_of.isoformat(),
        "total_records": len(records),
        "category_counts": dict(category_counts),
        "issue_count": len(issues),
        "quality_issues": issue_samples,
        "quality_issues_truncated": len(issues) > len(issue_samples),
        "issue_record_ids": issue_record_ids,
        "valid_record_count": len(records) - len(issue_record_ids),
        "duplicate_count": duplicate_count,
        "dedupe_keys": ["market", "stock_code", "category", "source", "source_record_id"],
        "status": "PENDING_REVIEW" if issues else "COMPLETED",
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
