"""Controlled forecast persistence and forward-only, append-only analyst validation.

Uses the existing PredictionLedger. ANALYST_* statuses are deliberately excluded
from the generic T+N reviewer, whose end-point scoring differs from target hits.
"""
from __future__ import annotations

from datetime import datetime, timezone
from zoneinfo import ZoneInfo
from sqlalchemy import select, func, case

from app.models.ai_hub import PredictionLedger
from app.models.market_data import StockKline
from app.models.stock_analyst import AiStockAnalysisRecord, AiAnalystForecast, AiAnalystForecastCheck

ZONE = ZoneInfo("Asia/Shanghai")
FINAL = {"PASSED", "FAILED", "INVALIDATED"}


def observations(analysis, revision=None):
    items = list(dict.fromkeys([
        *(analysis.result_json.get("watch_items") or []),
        *(revision.focus_points_json if revision else []),
    ]))
    return [{"observation_id": f"O{i+1}", "text": text} for i, text in enumerate(items)]


def validate_daily_contract(result, context, items):
    allow = set(context.get("evidence_allowlist", []))
    expected = {r["observation_id"] for r in items}
    checks = result.get("observation_checks", [])
    ids = [r["observation_id"] for r in checks]
    if len(ids) != len(set(ids)) or set(ids) - expected:
        raise ValueError("重点观察复核包含重复或未知条目，未发布本次AI判断")
    for row in [*checks, *result.get("forecasts", [])]:
        refs = row.get("evidence", [])
        if any(r["evidence_id"] not in allow for r in refs):
            raise ValueError("跟踪研判引用了本次证据白名单外的资料")
        if row.get("status") == "HIT" and not refs:
            raise ValueError("重点观察被标为命中但没有证据，不能据此发布预测")
        if row.get("status") == "HIT":
            evidence = {r["evidence_id"]: r for r in context.get("evidence", [])}
            window = context.get("monitor_window", {})
            recent = [evidence[r["evidence_id"]] for r in refs if r["evidence_id"] in evidence]
            if not any(
                (r["evidence_id"].startswith("market-watch:") and r.get("data", {}).get("fresh")) or
                (r.get("category") in {"NEWS", "NOTICE"} and
                 window.get("since", "0000") <= str(r.get("observed_at") or "")[:10] <= window.get("through", "9999"))
                for r in recent
            ):
                raise ValueError("观察命中缺少本次跟踪区间的新闻、公告或有效行情证据，不能用旧资料重复触发预测")
    labels = {r["observation_id"]: r["text"] for r in items}
    for row in checks:
        row["observation_text"] = labels[row["observation_id"]]
    return expected - set(ids)


def persist_forecasts(db, analysis, review, result):
    """Only validated typed model output enters the ledger through this service."""
    existing = list(db.scalars(select(AiAnalystForecast).where(AiAnalystForecast.review_id == review.id)).all())
    if existing:
        return [r.id for r in existing], []
    hit_labels = {r["label"] for r in review.conditions_json if r.get("status") == "HIT"}
    item_map = {r["observation_id"]: r["text"] for r in review.evidence_snapshot_json.get("tracked_observations", [])}
    hit_labels.update(item_map[r["observation_id"]] for r in result.get("observation_checks", [])
                      if r["status"] == "HIT" and r["observation_id"] in item_map)
    proposals = result.get("forecasts", [])
    if proposals and not hit_labels:
        return [], ["本次没有命中的观察项，未登记目标价预测"]
    market = next((r.get("data", {}) for r in review.evidence_snapshot_json.get("evidence", [])
                   if r["evidence_id"].startswith("market-watch:")), {})
    market_refs = {r["evidence_id"] for r in review.evidence_snapshot_json.get("evidence", [])
                   if r["evidence_id"].startswith("market-watch:") and r.get("data", {}).get("fresh")}
    trigger_refs = {r["label"]: set(r.get("evidence_ids") or (market_refs if r.get("kind") != "event" else set())) for r in review.conditions_json
                    if r.get("status") == "HIT"}
    trigger_refs.update({item_map[r["observation_id"]]: {e["evidence_id"] for e in r.get("evidence", [])}
                         for r in result.get("observation_checks", []) if r["status"] == "HIT"
                         and r["observation_id"] in item_map})
    entry = market.get("close_price")
    if proposals and (not market.get("fresh") or not entry or entry <= 0):
        return [], ["缺少近期有效收盘价，未登记目标价预测"]
    ids, rejected = [], []
    active = list(db.scalars(select(AiAnalystForecast).join(PredictionLedger,
        AiAnalystForecast.ledger_id == PredictionLedger.id).where(
        AiAnalystForecast.analysis_id == analysis.id, PredictionLedger.status == "ANALYST_PENDING")).all())
    active_keys = {(r.contract_json["direction"], tuple(sorted(r.contract_json["trigger_labels"]))) for r in active}
    created = datetime.now(timezone.utc)
    origin_day = created.astimezone(ZONE).date().isoformat()
    # Price data dated after publishing time would be look-ahead, including future-date sources.
    if proposals and (market.get("trade_date", "9999")[:10] > origin_day or review.business_date > origin_day):
        return [], ["行情或复核日期晚于预测发布时间，未登记目标价预测"]
    for ordinal, proposal in enumerate(proposals):
        up = proposal["direction"] == "BULLISH"
        target, stop = proposal["target_price"], proposal.get("invalidation_price")
        labels = proposal["trigger_labels"]
        if not set(labels) <= hit_labels:
            rejected.append("目标价未准确关联已命中的观察项，未登记该项预测")
            continue
        refs = {r["evidence_id"] for r in proposal["evidence"]}
        if not refs & market_refs or any(not refs & trigger_refs.get(label, set()) for label in labels):
            rejected.append("目标价未引用有效行情及对应命中证据，未登记该项预测")
            continue
        if not (target > entry if up else target < entry) or (stop is not None and not (stop < entry if up else stop > entry)):
            rejected.append("目标价或失效价与预测方向不一致，未登记该项预测")
            continue
        key = (proposal["direction"], tuple(sorted(labels)))
        if key in active_keys:
            rejected.append("相同观察方向已有目标价正在跟踪，继续验证原预测，未重复登记")
            continue
        ledger = PredictionLedger(market=analysis.market, stock_code=analysis.symbol,
            action_type="BUY" if up else "SELL", predicted_at=created,
            target_timeframe=f"T+{proposal['horizon_sessions']}", reasoning_logic=proposal["thesis"],
            skill_code="STOCK_ANALYST_FOLLOWUP", skill_version_used=review.skill_version,
            model_instance_code=review.model_instance_code, entry_price=entry,
            target_return_pct=round((target / entry - 1) * 100, 4), status="ANALYST_PENDING",
            evaluation_json={"workflow": "AI_ANALYST_TARGET_V1", "state": "PENDING"})
        db.add(ledger)
        db.flush()
        row = AiAnalystForecast(analysis_id=analysis.id, review_id=review.id, ledger_id=ledger.id,
            ordinal=ordinal, contract_json={**proposal, "base_trade_date": market["trade_date"],
                "base_source_id": market.get("source_id"), "currency": "港元" if analysis.market == "HK" else "元",
                "price_basis": "不复权日线收盘价", "published_date": origin_day,
                "model_call_log_id": review.model_call_log_id, "report_revision_id": review.report_revision_id,
                "config_version": review.config_snapshot_json.get("config_version"),
                "verification_rule": "发布次日起N个实际交易日内，收盘价先达目标为通过，先达失效价为失效；到期未达目标为未通过。日内触及不计。"})
        db.add(row)
        db.flush()
        ids.append(row.id)
        active_keys.add(key)
    return ids, rejected


def validate_forecasts(db, analysis, review):
    """No model scoring of price hits; only post-publication completed daily bars."""
    summaries = []
    rows = db.execute(select(AiAnalystForecast, PredictionLedger).join(PredictionLedger,
        AiAnalystForecast.ledger_id == PredictionLedger.id).where(
        AiAnalystForecast.analysis_id == analysis.id, AiAnalystForecast.review_id != review.id,
        PredictionLedger.status == "ANALYST_PENDING").order_by(AiAnalystForecast.id)).all()
    for forecast, ledger in rows:
        old = db.scalar(select(AiAnalystForecastCheck).where(
            AiAnalystForecastCheck.forecast_id == forecast.id, AiAnalystForecastCheck.review_id == review.id))
        if old:
            summaries.append({"forecast_id": forecast.id, **old.result_json})
            continue
        cfg = forecast.contract_json
        published = ledger.predicted_at
        if published.tzinfo is None:
            published = published.replace(tzinfo=timezone.utc)
        published_day = published.astimezone(ZONE).date().isoformat()
        day = min(review.business_date, datetime.now(ZONE).date().isoformat())
        bars = list(db.scalars(select(StockKline).where(
            StockKline.market == ledger.market, StockKline.symbol == ledger.stock_code,
            StockKline.period == "daily", StockKline.adjust == "",
            StockKline.trade_date > published_day, StockKline.trade_date <= day,
        ).order_by(StockKline.trade_date).limit(cfg["horizon_sessions"])).all())
        up = cfg["direction"] == "BULLISH"
        state, detail, final_bar = "PENDING", "尚未到期，继续收集预测发布后的交易日行情", None
        valid = []
        for bar in bars:
            # Today's unfinished daily candle is not a verified close.
            close_hour = 16 if ledger.market == "HK" else 15
            close_time = datetime.fromisoformat(f"{bar.trade_date[:10]}T{close_hour:02}:00:00+08:00")
            if close_time > datetime.now(ZONE):
                break
            if bar.close_price is None or bar.close_price <= 0:
                state, detail = "MISSING", "验证区间内存在缺失或无效收盘价，需补齐后重试"
                break
            valid.append(bar)
            price = bar.close_price
            stop = cfg.get("invalidation_price")
            if stop is not None and (price <= stop if up else price >= stop):
                state, detail, final_bar = "INVALIDATED", "收盘价先触及失效价位，原预测已失效", bar
                break
            if price >= cfg["target_price"] if up else price <= cfg["target_price"]:
                state, detail, final_bar = "PASSED", "发布后的实际收盘价已达到目标", bar
                break
        if state == "PENDING" and len(valid) >= cfg["horizon_sessions"]:
            state, detail, final_bar = "FAILED", "约定交易日内收盘价未达到目标，记录为未通过", valid[-1]
        if state == "PENDING" and (not valid or (datetime.fromisoformat(day).date() - datetime.fromisoformat(valid[-1].trade_date[:10]).date()).days > 4):
            state, detail = "MISSING", "暂无足够的新近有效行情，保持待验证，不计作预测失败"
        actual = final_bar or (valid[-1] if valid else None)
        result = {"state": state, "detail": detail, "sessions_observed": len(valid),
            "sessions_required": cfg["horizon_sessions"], "actual_price": actual.close_price if actual else None,
            "price_date": actual.trade_date if actual else None,
            "bar_evidence": [{"bar_id": b.id, "date": b.trade_date, "close": b.close_price, "source_id": b.source_id} for b in valid],
            "logic_chain": cfg["logic_chain"], "lesson": (
                "目标价路径通过，因果解释仍需独立证据验证，单次成功不代表规律。" if state == "PASSED" else
                "本次目标路径未实现，应复查前提、期限与竞争性解释，保留失败样本。" if state in {"FAILED", "INVALIDATED"} else
                "暂不形成有效性经验，继续跟踪。"),
            "learning_status": "CANDIDATE" if state in FINAL else "PENDING"}
        db.add(AiAnalystForecastCheck(forecast_id=forecast.id, review_id=review.id, status=state, result_json=result))
        ledger.evaluation_json = {"workflow": "AI_ANALYST_TARGET_V1", **result, "review_id": review.id}
        if state in FINAL:
            ledger.status = "ANALYST_" + state
            ledger.actual_price = actual.close_price
            ledger.actual_return_pct = round((actual.close_price / ledger.entry_price - 1) * 100, 4)
            ledger.direction_hit = actual.close_price > ledger.entry_price if up else actual.close_price < ledger.entry_price
            ledger.price_as_of = datetime.fromisoformat(f"{actual.trade_date[:10]}T{16 if ledger.market == 'HK' else 15}:00:00+08:00")
            ledger.evaluated_at = datetime.now(timezone.utc)
        summaries.append({"forecast_id": forecast.id, **result})
    db.commit()
    return summaries


def tracking_page(db, analysis_id, offset=0, limit=10):
    stmt = select(AiAnalystForecast, PredictionLedger).join(PredictionLedger,
        AiAnalystForecast.ledger_id == PredictionLedger.id).where(AiAnalystForecast.analysis_id == analysis_id)
    total = db.scalar(select(func.count()).select_from(stmt.subquery())) or 0
    stats = dict(db.execute(select(PredictionLedger.status, func.count()).join(AiAnalystForecast,
        AiAnalystForecast.ledger_id == PredictionLedger.id).where(AiAnalystForecast.analysis_id == analysis_id)
        .group_by(PredictionLedger.status)).all())
    items = []
    for forecast, ledger in db.execute(stmt.order_by(AiAnalystForecast.id.desc()).offset(offset).limit(limit)).all():
        checks = db.scalars(select(AiAnalystForecastCheck).where(AiAnalystForecastCheck.forecast_id == forecast.id)
                           .order_by(AiAnalystForecastCheck.id.desc()).limit(30)).all()
        items.append({"id": forecast.id, "review_id": forecast.review_id, "ledger_id": ledger.id,
            "created_at": forecast.created_at, "status": ledger.status.removeprefix("ANALYST_"),
            "entry_price": ledger.entry_price, "target_return_pct": ledger.target_return_pct,
            "model_instance_code": ledger.model_instance_code, "skill_version": ledger.skill_version_used,
            "contract": forecast.contract_json, "latest_validation": ledger.evaluation_json,
            "checks": [{"review_id": r.review_id, "created_at": r.created_at, **r.result_json} for r in checks]})
    return {"items": items, "total": total, "offset": offset, "limit": limit,
            "stats": {k.removeprefix("ANALYST_"): v for k, v in stats.items()}}


def learning_context(db, analysis_id=None, *, market=None, symbol=None):
    """Reuse this security's successes AND failures across reports, never promote them to facts."""
    if analysis_id is not None:
        analysis = db.get(AiStockAnalysisRecord, analysis_id)
        if not analysis:
            return []
        market, symbol = analysis.market, analysis.symbol
    rows = db.execute(select(AiAnalystForecast, PredictionLedger).join(PredictionLedger,
        AiAnalystForecast.ledger_id == PredictionLedger.id).where(
        PredictionLedger.market == market, PredictionLedger.stock_code == symbol)
        .order_by(case((PredictionLedger.status == "ANALYST_PENDING", 1), else_=0), AiAnalystForecast.id.desc()).limit(8)).all()
    return [{"forecast_id": forecast.id, "analysis_id": forecast.analysis_id,
             "thesis": forecast.contract_json["thesis"][:400],
             "logic_chain": [s[:200] for s in forecast.contract_json["logic_chain"][:4]],
             "status": ledger.status.removeprefix("ANALYST_"), "learning_status": "CANDIDATE",
             "validation": {k: ledger.evaluation_json.get(k) for k in ("detail", "price_date", "actual_price", "lesson")}}
            for forecast, ledger in rows]
