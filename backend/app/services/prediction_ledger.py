"""Persist structured Agent recommendations and report ratings for later review."""

from __future__ import annotations

import json

from pydantic import ValidationError
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.ai_hub import ModelSkill, PredictionLedger, ResearchReportRecord
from app.schemas.predictions import PredictionCreate
from app.services.skill_registry import sync_skill_from_file


def record_agent_predictions(
    db: Session,
    response_text: str,
    *,
    allowed_skill_codes: set[str],
    model_instance_code: str | None,
) -> list[int]:
    """Capture explicit JSON advice; reject incomplete or unbound advice."""
    try:
        response = json.loads(response_text)
    except (TypeError, ValueError):
        return []
    if not isinstance(response, dict):
        return []
    raw = response.get("predictions")
    if raw is None and "prediction" in response:
        raw = [response["prediction"]]
    if raw is None:
        return []
    if not isinstance(raw, list) or len(raw) > 50:
        raise ValueError("Agent predictions must be an array of at most 50 structured recommendations")
    rows: list[PredictionLedger] = []
    for item in raw:
        try:
            recommendation = PredictionCreate.model_validate(item)
        except ValidationError as exc:
            raise ValueError(f"Agent prediction is incomplete: {exc.errors()[0]['loc']}") from exc
        if recommendation.skill_code not in allowed_skill_codes:
            raise ValueError("Agent prediction must cite a bound Skill")
        skill = db.scalar(select(ModelSkill).where(
            ModelSkill.skill_code == recommendation.skill_code, ModelSkill.enabled.is_(True),
        ))
        if skill is None:
            raise ValueError("Agent prediction cites a disabled or missing Skill")
        if recommendation.research_report_id and db.get(ResearchReportRecord, recommendation.research_report_id) is None:
            raise ValueError("Agent prediction cites a missing research report")
        sync_skill_from_file(db, skill)
        values = recommendation.model_dump(exclude={"model_instance_code"})
        rows.append(PredictionLedger(
            **values, skill_version_used=skill.version,
            model_instance_code=model_instance_code, status="PENDING",
        ))
    db.add_all(rows)
    db.commit()
    return [row.id for row in rows]


def record_report_rating(
    db: Session,
    report: ResearchReportRecord,
    *,
    entry_price: float | None,
) -> PredictionLedger | None:
    """Map the report's existing A/B/C/D rating to a visible T+5 ledger entry."""
    if entry_price is None or entry_price <= 0:
        return None
    skill = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == "STOCK_TREND_ADVISOR"))
    if skill is None:
        return None
    sync_skill_from_file(db, skill)
    action = "BUY" if report.rating in {"A", "B"} else "SELL" if report.rating == "D" else "HOLD"
    row = PredictionLedger(
        market=report.market, stock_code=report.symbol, action_type=action,
        target_timeframe="T+5", reasoning_logic=(
            f"Research report #{report.id}; rating {report.rating or 'C'}; score {report.score}; "
            f"{report.conclusion or ''}"
        )[:20000],
        skill_code=skill.skill_code, skill_version_used=skill.version,
        model_instance_code=report.model_instance, research_report_id=report.id,
        entry_price=entry_price, status="PENDING",
    )
    db.add(row)
    db.flush()
    return row
