"""T+N review uses trading-session closes; only prompt proposals reach the Meta Agent."""

from __future__ import annotations

import asyncio
import hashlib
import json
from datetime import datetime, timezone
from zoneinfo import ZoneInfo

from sqlalchemy import or_, select
from sqlalchemy.orm import Session

from app.models.ai_hub import ModelInstance, ModelProvider, ModelSkill, PredictionLedger, SkillOptimizationDraft
from app.models.market_data import DataSource, StockKline
from app.models.selection import SelectionCandidate
from app.schemas.skill_tools import PredictionOutcomeInput, PredictionScoreRequest
from app.services.daily_sync import _seconds_until_next_midnight
from app.services.model_hub import ModelHubService
from app.services.skill_tools import score_prediction_outcomes
from app.services.stock_on_demand import StockOnDemandService


SHANGHAI = ZoneInfo("Asia/Shanghai")


def _prediction_date(row: PredictionLedger) -> str:
    predicted_at = row.predicted_at
    if predicted_at.tzinfo is None:
        predicted_at = predicted_at.replace(tzinfo=timezone.utc)
    return predicted_at.astimezone(SHANGHAI).date().isoformat()


def _fetch_due_kline(db: Session, row: PredictionLedger) -> None:
    source = db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE", DataSource.enabled.is_(True)))
    if source is None:
        return
    StockOnDemandService(db).fetch_kline(
        source=source, market=row.market, symbol=row.stock_code,
        period="daily", adjust="", start_date=_prediction_date(row),
        end_date=datetime.now(SHANGHAI).date().isoformat(), persist=True,
    )


def review_predictions(db: Session, *, fetch_missing: bool = False, limit: int = 100) -> dict[str, int]:
    rows = list(db.scalars(select(PredictionLedger).where(
        PredictionLedger.status == "PENDING",
        # Selection predictions use their confirmed snapshot and ten-session
        # tracking workflow; the generic reviewer must not evaluate them twice.
        ~PredictionLedger.id.in_(select(SelectionCandidate.prediction_id).where(SelectionCandidate.prediction_id.is_not(None))),
    ).order_by(PredictionLedger.predicted_at).limit(limit)).all())
    fetched: set[tuple[str, str]] = set()
    evaluated = 0
    for row in rows:
        sessions = int(row.target_timeframe[2:])
        if fetch_missing and datetime.now(SHANGHAI).date().isoformat() > _prediction_date(row) and (row.market, row.stock_code) not in fetched:
            fetched.add((row.market, row.stock_code))
            try:
                _fetch_due_kline(db, row)
            except Exception:
                db.rollback()
        closes = list(db.scalars(select(StockKline).where(
            StockKline.market == row.market,
            StockKline.symbol == row.stock_code,
            StockKline.period == "daily",
            StockKline.adjust == "",
            StockKline.trade_date > _prediction_date(row),
            StockKline.close_price.is_not(None),
        ).order_by(StockKline.trade_date).limit(sessions)).all())
        if len(closes) < sessions:
            continue
        actual = closes[-1]
        if not actual.close_price or actual.close_price <= 0:
            continue
        close_time = "16:00:00" if row.market == "HK" else "15:00:00"
        price_as_of = datetime.fromisoformat(f"{actual.trade_date}T{close_time}+08:00")
        source = db.get(DataSource, actual.source_id)
        score = score_prediction_outcomes(PredictionScoreRequest(outcomes=[PredictionOutcomeInput(
            ledger_id=row.id, action_type=row.action_type, entry_price=row.entry_price,
            actual_price=actual.close_price, target_return_pct=row.target_return_pct,
            due_at=price_as_of, price_as_of=price_as_of,
            source=source.source_code if source else None,
        )]))["results"][0]
        if score["status"] != "EVALUATED":
            continue
        row.status = "EVALUATED"
        row.actual_price = actual.close_price
        row.actual_return_pct = score["actual_return_pct"]
        row.direction_hit = score["direction_hit"]
        row.price_as_of = price_as_of
        row.evaluation_json = score
        row.evaluated_at = datetime.now(timezone.utc)
        evaluated += 1
    db.commit()
    return {"checked": len(rows), "evaluated": evaluated, "pending": len(rows) - evaluated}


def _has_live_meta_model(db: Session) -> bool:
    return db.scalar(select(ModelInstance.id).join(ModelProvider, ModelProvider.id == ModelInstance.provider_id).where(
        ModelInstance.enabled.is_(True), ModelProvider.enabled.is_(True),
        ModelProvider.provider_type != "MOCK",
        or_(ModelProvider.api_key_encrypted.is_not(None), ModelInstance.api_key.is_not(None)),
    ).limit(1)) is not None


def propose_failed_skill_revisions(db: Session) -> int:
    """Three consecutive failed, evaluated predictions create a reviewable draft only."""
    if not _has_live_meta_model(db):
        return 0
    count = 0
    skill_codes = db.scalars(select(PredictionLedger.skill_code).where(
        PredictionLedger.status == "EVALUATED",
    ).distinct()).all()
    for code in skill_codes:
        skill = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == code))
        if skill is None or not skill.enabled or skill.skill_type != "PROMPT_SOP":
            continue
        recent = list(db.scalars(select(PredictionLedger).where(
            PredictionLedger.skill_code == code,
            PredictionLedger.status == "EVALUATED",
        ).order_by(PredictionLedger.predicted_at.desc()).limit(3)).all())
        if len(recent) < 3 or any(item.direction_hit is not False for item in recent):
            continue
        signature = hashlib.sha256(f"{code}:{','.join(str(item.id) for item in recent)}".encode()).hexdigest()
        if db.scalar(select(SkillOptimizationDraft.id).where(SkillOptimizationDraft.failure_signature == signature)):
            continue
        evidence = [{
            "ledger_id": row.id, "market": row.market, "stock_code": row.stock_code,
            "action_type": row.action_type, "predicted_at": row.predicted_at.isoformat(),
            "target_timeframe": row.target_timeframe, "reasoning_logic": row.reasoning_logic[:4000],
            "skill_version_used": row.skill_version_used,
            "actual_return_pct": row.actual_return_pct, "price_as_of": row.price_as_of.isoformat() if row.price_as_of else None,
        } for row in recent]
        output_schema = {"type": "object", "required": ["proposed_instructions", "rationale"],
                         "properties": {"proposed_instructions": {"type": "string", "minLength": 20}, "rationale": {"type": "string", "minLength": 10}}}
        log = ModelHubService(db).chat(
            task_type="meta_review",
            messages=[
                {"role": "system", "content": "你是 Meta-Agent 裁判。只基于证据分析连续失败原因，返回 JSON 格式的完整改进版 Skill Prompt 与修正依据。不得自动启用或承诺收益。"},
                {"role": "user", "content": json.dumps({"current_skill_prompt": skill.instructions, "failed_cases": evidence}, ensure_ascii=False)},
            ],
            metadata_json={"json_schema_output": output_schema},
        )
        if log.status != "SUCCESS" or log.provider_code == "MOCK":
            continue
        proposal = json.loads(log.response_text or "{}")
        db.add(SkillOptimizationDraft(
            skill_id=skill.id, base_skill_version=skill.version,
            failure_signature=signature, prediction_ids=[row.id for row in recent],
            proposed_instructions=proposal["proposed_instructions"],
            rationale=proposal["rationale"], model_call_log_id=log.id,
        ))
        db.commit()
        count += 1
    return count


def review_predictions_once() -> None:
    """Compatibility wrapper; new callers use PredictionReviewWorkflow."""
    from app.orchestration.background import PredictionReviewWorkflow

    PredictionReviewWorkflow().execute()


async def daily_prediction_review_loop() -> None:
    while True:
        await asyncio.sleep(_seconds_until_next_midnight() + 600)
        try:
            await asyncio.to_thread(review_predictions_once)
        except Exception:
            # The next daily run retries pending ledger entries.
            pass
