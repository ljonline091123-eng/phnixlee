"""Local-data, human-reviewed stock selection and ten-session tracking.

The selection workflow deliberately keeps the hard filter in SQL/Python and
only stores bounded evidence for a later model call.  It never sends a market
universe to an LLM and it never fabricates a price when the local data is
missing.
"""

from __future__ import annotations

from datetime import datetime, time, timedelta, timezone
from math import isfinite
from types import SimpleNamespace
from typing import Any
from zoneinfo import ZoneInfo

from sqlalchemy import case, delete, func, or_, select
from sqlalchemy.orm import Session

from app.models.ai_hub import (
    KnowledgeDocument,
    KnowledgeBase,
    KnowledgeGraph,
    KnowledgeEntity,
    KnowledgeRelation,
    ModelInstance,
    ModelSkill,
    PredictionLedger,
)
from app.models.market_data import (
    DataSource,
    StockFinancialReport,
    StockKline,
    StockNews,
    StockNotice,
    StockRealtimeQuote,
    StockSymbol,
)
from app.models.selection import SelectionCandidate, SelectionRun, SelectionSnapshot, SelectionTracking
from app.schemas.selection import SelectionModelOutput, SelectionReviewRequest, SelectionRunCreate


def _now() -> datetime:
    return datetime.now(timezone.utc)


_MARKET_ZONES = {
    "CN_A": ZoneInfo("Asia/Shanghai"),
    "NEEQ": ZoneInfo("Asia/Shanghai"),
    "HK": ZoneInfo("Asia/Hong_Kong"),
    "HKEX": ZoneInfo("Asia/Hong_Kong"),
}
_MARKET_CLOSE = {"CN_A": time(15, 5), "NEEQ": time(15, 5), "HK": time(16, 15), "HKEX": time(16, 15)}


def _market_zone(market: str) -> ZoneInfo:
    return _MARKET_ZONES.get(str(market or "").upper(), ZoneInfo("Asia/Shanghai"))


def _market_local_date(market: str, as_of: datetime | None = None) -> str:
    moment = as_of or _now()
    if moment.tzinfo is None:
        moment = moment.replace(tzinfo=timezone.utc)
    return moment.astimezone(_market_zone(market)).date().isoformat()


def _last_completed_trade_date(market: str, as_of: datetime | None = None) -> str:
    """Return the latest local calendar date whose daily bar is final.

    Daily bars are excluded until a small post-close grace period has elapsed.
    This avoids using a still-forming bar when the scheduler runs before the
    exchange close or around midnight in a different UTC date.
    """
    moment = as_of or _now()
    if moment.tzinfo is None:
        moment = moment.replace(tzinfo=timezone.utc)
    local = moment.astimezone(_market_zone(market))
    day = local.date()
    close_time = _MARKET_CLOSE.get(str(market or "").upper(), time(15, 5))
    if local.time() < close_time:
        day -= timedelta(days=1)
    return day.isoformat()


def _as_float(value: Any) -> float | None:
    try:
        result = float(value) if value is not None else None
        return result if result is not None and isfinite(result) else None
    except (TypeError, ValueError):
        return None


def _quote_observation_date(quote: Any, market: str) -> str | None:
    """Extract the quote's own observation date, preserving the displayed snapshot."""
    quote_time = getattr(quote, "quote_time", None)
    if quote_time:
        value = str(quote_time).strip()
        if len(value) >= 10:
            try:
                return datetime.fromisoformat(value[:10]).date().isoformat()
            except ValueError:
                pass
    fetched_at = getattr(quote, "fetched_at", None)
    if isinstance(fetched_at, datetime):
        if fetched_at.tzinfo is None:
            fetched_at = fetched_at.replace(tzinfo=timezone.utc)
        return fetched_at.astimezone(_market_zone(market)).date().isoformat()
    return None


def _latest_quotes(
    db: Session,
    market: str,
    symbols: list[str] | None = None,
    as_of: datetime | None = None,
) -> list[StockRealtimeQuote]:
    query = select(StockRealtimeQuote).where(StockRealtimeQuote.market == market).order_by(
        StockRealtimeQuote.fetched_at.desc()
    )
    if as_of is not None:
        query = query.where(StockRealtimeQuote.fetched_at <= as_of)
    if symbols:
        query = query.where(StockRealtimeQuote.symbol.in_(symbols))
    rows = db.scalars(query).all()
    latest: dict[str, StockRealtimeQuote] = {}
    for row in rows:
        latest.setdefault(row.symbol, row)
    return list(latest.values())


def _latest_kline(
    db: Session,
    market: str,
    symbol: str,
    as_of: datetime | None = None,
) -> tuple[StockKline | None, StockKline | None, float | None]:
    cutoff = _last_completed_trade_date(market, as_of)
    rows = list(db.scalars(
        select(StockKline).where(
            StockKline.market == market,
            StockKline.symbol == symbol,
            StockKline.period == "daily",
            StockKline.adjust == "",
            StockKline.trade_date <= cutoff,
    ).order_by(StockKline.trade_date.desc()).limit(21)
    ).all())
    return _bar_summary(rows)


def _bar_summary(rows: list[StockKline]) -> tuple[StockKline | None, StockKline | None, float | None]:
    # Multiple source records for the same date must not become extra sessions.
    rows = list({row.trade_date: row for row in reversed(rows)}.values())
    rows.sort(key=lambda row: row.trade_date, reverse=True)
    if not rows:
        return None, None, None
    latest = rows[0]
    previous = rows[1] if len(rows) > 1 else None
    volumes = [_as_float(row.volume) for row in rows[1:] if _as_float(row.volume) is not None and _as_float(row.volume) > 0]
    avg = sum(volumes) / len(volumes) if volumes else None
    return latest, previous, avg


def _screening_bars(db: Session, market: str, symbols: list[str], as_of: datetime) -> dict[str, list[StockKline]]:
    rank = select(StockKline.id, func.row_number().over(
        partition_by=StockKline.symbol, order_by=(StockKline.trade_date.desc(), StockKline.fetched_at.desc())
    ).label("position")).where(
        StockKline.market == market, StockKline.period == "daily", StockKline.adjust == "",
        StockKline.trade_date <= _last_completed_trade_date(market, as_of),
    )
    if symbols:
        rank = rank.where(StockKline.symbol.in_(symbols))
    ranked = rank.subquery()
    rows = db.scalars(select(StockKline).join(ranked, ranked.c.id == StockKline.id).where(
        ranked.c.position <= 21
    ).order_by(StockKline.symbol, StockKline.trade_date.desc(), StockKline.fetched_at.desc())).all()
    grouped: dict[str, list[StockKline]] = {}
    for row in rows:
        grouped.setdefault(row.symbol, []).append(row)
    return grouped


def _evidence(db: Session, market: str, symbol: str, kb_ids: list[int], graph_ids: list[int]) -> dict[str, Any]:
    doc_query = select(KnowledgeDocument).where(
        KnowledgeDocument.symbol == symbol,
        KnowledgeDocument.market == market,
        KnowledgeDocument.knowledge_base_id.in_(kb_ids),
    )
    if graph_ids:
        doc_query = doc_query.where(KnowledgeDocument.graph_id.in_(graph_ids))
    # Rank within each source so decades of K-lines cannot displace financial,
    # shareholder, news and policy evidence before the context is bounded.
    ranked = doc_query.with_only_columns(KnowledgeDocument.id, func.row_number().over(
        partition_by=KnowledgeDocument.source_table,
        order_by=(KnowledgeDocument.updated_at.desc(), KnowledgeDocument.id.desc()),
    ).label("position")).subquery()
    docs = list(db.scalars(select(KnowledgeDocument).join(ranked, ranked.c.id == KnowledgeDocument.id).where(
        ranked.c.position <= 2
    ).order_by(ranked.c.position, KnowledgeDocument.source_table)).all())[:16]
    # Graph entities are namespaced as ``{graph_id}:{market}:{symbol}`` (and
    # document entities end with the same market/symbol suffix).  Never query
    # the un-namespaced symbol: that would miss the stock node entirely.
    ent_query = select(KnowledgeEntity).where(
        KnowledgeEntity.knowledge_base_id.in_(kb_ids),
        KnowledgeEntity.graph_id.in_(graph_ids),
        or_(
            KnowledgeEntity.entity_key.like(f"%:{market}:{symbol}"),
            KnowledgeEntity.entity_key.like(f"%:{market}:{symbol}:%"),
        )
    )
    entities = list(db.scalars(ent_query.limit(100)).all())
    entity_ids = [item.id for item in entities]
    rel_query = select(KnowledgeRelation).where(
        or_(
            KnowledgeRelation.subject_entity_id.in_(entity_ids),
            KnowledgeRelation.object_entity_id.in_(entity_ids),
        )
    )
    if graph_ids:
        rel_query = rel_query.where(KnowledgeRelation.graph_id.in_(graph_ids))
    relations = list(db.scalars(rel_query.order_by(case(
        (KnowledgeRelation.predicate.in_(["HAS_RECORD", "HAS_NEWS", "HAS_NOTICE", "HAS_PRICE_AND_VOLUME_SERIES"]), 1), else_=0
    ), KnowledgeRelation.id.desc()).limit(30)).all()) if entities else []
    news_count = int(db.scalar(select(func.count(StockNews.id)).where(StockNews.market == market, StockNews.symbol == symbol)) or 0)
    notice_count = int(db.scalar(select(func.count(StockNotice.id)).where(StockNotice.market == market, StockNotice.symbol == symbol)) or 0)
    financial_count = int(db.scalar(select(func.count(StockFinancialReport.id)).where(StockFinancialReport.market == market, StockFinancialReport.symbol == symbol)) or 0)
    related_ids = {key for relation in relations for key in (relation.subject_entity_id, relation.object_entity_id)}
    related = {row.id: row for row in db.scalars(select(KnowledgeEntity).where(KnowledgeEntity.id.in_(related_ids))).all()}
    relation_doc_ids = {row.evidence_document_id for row in relations[:20] if row.evidence_document_id}
    relation_docs = {row.id: row for row in db.scalars(select(KnowledgeDocument).where(
        KnowledgeDocument.id.in_(relation_doc_ids), KnowledgeDocument.knowledge_base_id.in_(kb_ids),
        KnowledgeDocument.graph_id.in_(graph_ids),
    )).all()}
    relation_rows = [
        {"id": item.id, "predicate": item.predicate, "subject_entity_id": item.subject_entity_id,
         "head": related[item.subject_entity_id].entity_name if item.subject_entity_id in related else "",
         "tail": related[item.object_entity_id].entity_name if item.object_entity_id in related else "",
         "relation": item.predicate,
         "object_entity_id": item.object_entity_id, "evidence_document_id": item.evidence_document_id,
         "evidence_text": relation_docs[item.evidence_document_id].content[:500]
         if item.evidence_document_id in relation_docs else ""}
        for item in relations[:20]
    ]
    result = {
        "knowledge_documents": len(docs),
        "knowledge_entities": len(entities),
        "knowledge_relations": len(relations),
        "news_records": news_count,
        "notice_records": notice_count,
        "financial_records": financial_count,
        "document_ids": sorted({item.id for item in docs} | set(relation_docs)),
        "documents": [{"id": item.id, "title": item.title, "source_table": item.source_table,
                       "graph_id": item.graph_id,
                       "excerpt": (item.content or "")[:500],
                       "url": (item.metadata_json or {}).get("url")} for item in docs],
        "relations": relation_rows,
        "evidence_text": [item.title for item in docs[:5]],
        "missing_data": (["知识库缺少该股票资料"] if not docs else []) + (["图谱缺少该股票关系"] if not relations else []) + (["缺少财务指标"] if not financial_count else []),
        "sources": ["stock_realtime_quote", "stock_kline", "stock_news", "stock_notice", "stock_financial_report", "knowledge_document", "knowledge_entity", "knowledge_relation"],
    }
    # Enrich the historical evidence contract with a bounded, versioned
    # GraphRAG context.  The legacy keys above intentionally remain unchanged
    # for existing clients and tests.
    try:
        from app.services.graph_rag import build_selection_context

        context = build_selection_context(
            db, market, symbol, knowledge_base_ids=kb_ids, graph_ids=graph_ids,
            max_documents=12, max_relations=30, max_facts=30, max_chunks=8,
        )
        for key in ("facts", "chunks", "structured_observations", "graph_paths", "evidence_span", "lineage", "lakehouse_datasets",
                    "graph_version", "lakehouse_version", "document_version", "context_version", "context_hash",
                    "data_cutoff", "identity", "missing_data", "counts", "evidence_fact_ids", "evidence_source_ids",
                    "evidence_record_refs"):
            if key in context:
                result[key] = context[key]
        # Keep document IDs from the legacy scope as the model's allow-list;
        # context documents can include an explicitly linked relation proof.
        result["context_document_ids"] = context.get("evidence_document_ids", [])
        # The legacy ``document_ids`` key remains the old, explicitly scoped
        # allow-list for compatibility.  Model validation uses the union so a
        # relation-proof document discovered by GraphRAG is not rejected.
        result["model_document_ids"] = sorted(set(result.get("document_ids", [])) |
                                               set(result["context_document_ids"]))
        result["evidence_context"] = context
    except Exception as exc:
        result["context_error"] = str(exc)[:300]
        result.setdefault("missing_data", []).append("GraphRAG 上下文暂不可用")
    return result


def _candidate_payload(db: Session, run: SelectionRun, quote: StockRealtimeQuote, bars: list[StockKline]) -> tuple[dict[str, Any] | None, list[str]]:
    latest, previous, avg_volume = _bar_summary(bars)
    entry = _as_float(quote.current_price)
    prev_close = _as_float(quote.previous_close_price)
    if entry is None and latest is not None:
        entry = _as_float(latest.close_price)
    if prev_close is None and previous is not None:
        prev_close = _as_float(previous.close_price)
    missing: list[str] = []
    if entry is None or entry <= 0:
        missing.append(f"{run.market}:{quote.symbol}:current_price")
        return None, missing
    change = _as_float(quote.change_pct)
    if change is None and prev_close and prev_close > 0:
        change = (entry / prev_close - 1) * 100
    volume = _as_float(quote.volume)
    volume_ratio = volume / avg_volume if volume is not None and avg_volume and avg_volume > 0 else None
    if change is None:
        missing.append(f"{run.market}:{quote.symbol}:change_pct")
    quote_date = _quote_observation_date(quote, run.market)
    rules = {
        "change_pct": round(change, 6) if change is not None else None,
        "volume_ratio": round(volume_ratio, 6) if volume_ratio is not None else None,
        "price_trigger": bool(change is not None and abs(change) >= run.criteria_json.get("min_abs_change_pct", 2.0)),
        "volume_trigger": bool(volume_ratio is not None and volume_ratio >= run.criteria_json.get("min_volume_ratio", 1.2)),
        "bar_date": latest.trade_date if latest else None,
        "quote_observed_date": quote_date,
    }
    if not rules["price_trigger"] and not rules["volume_trigger"]:
        return None, missing
    score = abs(change or 0) + (volume_ratio or 0)
    if quote_date is None:
        missing.append(f"{run.market}:{quote.symbol}:quote_observed_at")
    evidence: dict[str, Any] = {"data_mode": run.data_mode}
    analysis = {
        "source": "HARD_RULES_AND_LOCAL_EVIDENCE",
        "model_requested": run.analysis_mode == "MODEL_REQUESTED",
        "model_output_status": "NOT_CALLED" if run.analysis_mode != "MODEL_REQUESTED" else "PENDING_MODEL_CALL",
        "summary": "量价硬规则触发，证据已汇总，等待人工复选。",
        "thesis": "量价硬规则触发，结合本地知识库/图谱证据供人工复核。",
        "reasoning": {"hard_filter": rules},
        "evidence": evidence.get("evidence_text", []),
        "inference_boundary": "未使用证据之外的市场数据；未作自动交易决策。",
    }
    return {
        "market": run.market,
        "symbol": quote.symbol,
        "name": None,
        "entry_price": entry,
        "entry_date": quote_date or (latest.trade_date if latest else None),
        "hard_score": round(score, 6),
        "hard_rules_json": rules,
        "evidence_json": evidence,
        "analysis_json": analysis,
    }, missing


def create_selection_run(db: Session, payload: SelectionRunCreate) -> SelectionRun:
    market = payload.market.upper()
    symbols = [str(item).strip() for item in payload.symbols if str(item).strip()]
    model_code = payload.model_instance_code
    if model_code and db.scalar(select(ModelInstance.id).where(ModelInstance.instance_code == model_code)) is None:
        raise ValueError("model_instance_code not found")
    bases = list(db.scalars(select(KnowledgeBase).where(KnowledgeBase.id.in_(payload.knowledge_base_ids))).all())
    graphs = list(db.scalars(select(KnowledgeGraph).where(KnowledgeGraph.id.in_(payload.graph_ids))).all())
    if len(bases) != len(set(payload.knowledge_base_ids)) or any(not row.enabled for row in bases):
        raise ValueError("Selected knowledge bases are missing or disabled")
    if len(graphs) != len(set(payload.graph_ids)) or any(not row.enabled or row.knowledge_base_id not in payload.knowledge_base_ids for row in graphs):
        raise ValueError("Selected graphs must be enabled and belong to the selected knowledge bases")
    # A live model may only be called when a governed KB/graph context is
    # explicitly selected.  MOCK runs remain available for deterministic unit
    # tests and local dry-runs; real runs without a governed context degrade to
    # the auditable hard-rule path instead of sending an unbounded prompt.
    governed_context = bool(bases and graphs) and all(
        row.governance_status in {"GOVERNED", "LOCKED"} for row in graphs
    )
    model_requested = bool(payload.use_model and (payload.data_mode == "MOCK" or governed_context))
    run = SelectionRun(
        market=market,
        universe_scope=payload.universe_scope,
        candidate_limit=payload.candidate_limit,
        criteria_json={"min_abs_change_pct": payload.min_abs_change_pct, "min_volume_ratio": payload.min_volume_ratio, "symbols": symbols},
        model_instance_code=model_code,
        knowledge_base_ids_json=list(payload.knowledge_base_ids),
        graph_ids_json=list(payload.graph_ids),
        data_mode=payload.data_mode,
        analysis_mode="MODEL_REQUESTED" if model_requested else "LOCAL_EVIDENCE",
        status="RUNNING",
        as_of=_now(),
    )
    db.add(run)
    db.flush()
    quotes = _latest_quotes(db, market, symbols, run.as_of)
    # SQL/Python scans the locally available universe. Only the final <=50
    # candidates are enriched with documents and sent to the model.
    bar_map = _screening_bars(db, market, symbols, run.as_of)
    stock_query = select(StockSymbol).where(StockSymbol.market == market, StockSymbol.status == "LISTED")
    if symbols:
        stock_query = stock_query.where(StockSymbol.symbol.in_(symbols))
    stock_map = {row.symbol: row for row in db.scalars(stock_query).all()}
    quotes = [row for row in quotes if row.symbol in stock_map]
    quote_symbols = {row.symbol for row in quotes}
    for symbol, bars in bar_map.items():
        if symbol in quote_symbols or symbol not in stock_map:
            continue
        latest, previous, _avg = _bar_summary(bars)
        close = _as_float(latest.close_price) if latest else None
        prev_close = _as_float(previous.close_price) if previous else None
        if close is not None:
            quotes.append(SimpleNamespace(
                market=market, symbol=symbol, current_price=close,
                previous_close_price=prev_close,
                change_pct=((close / prev_close - 1) * 100) if prev_close and prev_close > 0 else None,
                volume=latest.volume, quote_time=latest.trade_date, fetched_at=latest.fetched_at,
            ))
    run.criteria_json = {**run.criteria_json, "local_universe_count": len(stock_map), "priced_stock_count": len(quotes)}
    if not quotes and symbols:
        run.missing_data_json = [f"{market}:{symbol}:quote" for symbol in symbols]
    elif not quotes:
        run.missing_data_json = [f"{market}:universe:stock_realtime_quote"]
    candidates: list[tuple[dict[str, Any], float]] = []
    missing: list[str] = []
    for quote in quotes:
        item, item_missing = _candidate_payload(db, run, quote, bar_map.get(quote.symbol, []))
        missing.extend(item_missing)
        if item:
            stock = stock_map.get(quote.symbol)
            item["name"] = stock.name if stock else None
            candidates.append((item, item["hard_score"]))
    candidates.sort(key=lambda item: item[1], reverse=True)
    candidates = candidates[:run.candidate_limit]
    for item, _score in candidates:
        evidence = _evidence(db, market, item["symbol"], payload.knowledge_base_ids, payload.graph_ids)
        evidence["data_mode"] = run.data_mode
        evidence["missing_data"] += [f"图谱 {row.graph_name} 尚有待治理事项" for row in graphs if row.governance_status == "PENDING"]
        item["evidence_json"] = evidence
        item["analysis_json"]["evidence"] = evidence["evidence_text"]
    priced_symbols = {row.symbol for row in quotes}
    missing.extend(f"{market}:{symbol}:quote" for symbol in stock_map if symbol not in priced_symbols)
    if payload.use_model and not model_requested:
        missing.append("缺少已治理知识库/图谱，已降级为本地规则分析")
    if model_requested and candidates:
        _apply_model_analysis(db, run, candidates)
    # Make the Skill/model boundary explicit in the persisted evidence.  The
    # immutable audit row below repeats these values so a later Skill edit
    # cannot change the interpretation of this run.
    selected_skill = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == "STOCK_SELECTION_ANALYST"))
    for item, _score in candidates[: run.candidate_limit]:
        item["evidence_json"] = {
            **(item.get("evidence_json") or {}),
            "as_of": run.as_of.isoformat(),
            "data_cutoff": run.as_of.date().isoformat(),
            "skill_code": "STOCK_SELECTION_ANALYST",
            "skill_version": selected_skill.version if selected_skill else "UNREGISTERED",
            "model_version": run.model_instance_code,
        }
        item["analysis_json"] = {
            **(item.get("analysis_json") or {}),
            "skill_code": "STOCK_SELECTION_ANALYST",
            "skill_version": selected_skill.version if selected_skill else "UNREGISTERED",
            "model_version": run.model_instance_code,
        }
        candidate = SelectionCandidate(run_id=run.id, **item)
        db.add(candidate)
        db.flush()
        from app.services.selection_audit import capture_decision_snapshot
        capture_decision_snapshot(
            db, candidate, snapshot_kind="CANDIDATE", decision="PENDING",
            evidence=candidate.evidence_json, as_of=run.as_of,
        )
    run.candidate_count = min(len(candidates), run.candidate_limit)
    run.missing_data_json = sorted(set((run.missing_data_json or []) + missing))[:100]
    run.status = "REVIEW" if run.candidate_count else "PARTIAL"
    run.completed_at = _now()
    db.commit()
    db.refresh(run)
    return run


def _apply_model_analysis(db: Session, run: SelectionRun, candidates: list[tuple[dict[str, Any], float]]) -> None:
    """Ask the selected model to rank only the bounded hard-filter pool."""
    import json

    from app.services.model_hub import ModelHubService

    selected = [item for item, _ in candidates[:50]]
    schema = SelectionModelOutput.model_json_schema()
    # Each batch stays below the model hub's 60k-character input guard.
    # Preserve all selected source types instead of dropping late candidates.
    batches: list[list[dict[str, Any]]] = []
    batch: list[dict[str, Any]] = []
    batch_size = 0
    for item in selected:
        size = len(json.dumps(item, ensure_ascii=False))
        if batch and (batch_size + size > 50_000 or len(batch) >= 5):
            batches.append(batch)
            batch, batch_size = [], 0
        batch.append(item)
        batch_size += size
    if batch:
        batches.append(batch)
    predictions: dict[str, tuple[dict[str, Any], int, str]] = {}
    try:
        for batch_index, batch in enumerate(batches):
            context = json.dumps(batch, ensure_ascii=False, separators=(",", ":"))
            log = ModelHubService(db).chat(
                task_type="stock_screening",
                instance_code=run.model_instance_code,
                messages=[
                    {"role": "system", "content": "本次为选股盯盘工作流。SOP 的硬筛选和上下文加载已由调度器执行。沿用 SOP 的证据边界与分析要求，输出格式采用以下本次调用的 JSON Schema，替代 SOP 中通用研报格式；返回且只返回此契约的 JSON：" + json.dumps(schema, ensure_ascii=False)},
                    {"role": "user", "content": "仅分析以下硬过滤候选，不要编造行情或外部事实。逐只综合 GraphRAG 上下文中的身份、结构化观测、已验证事实、图谱路径、文档切片和血缘，返回 WATCH/PASS、置信度、理由及引用的文档/事实/切片 ID。只引用候选上下文白名单中的 ID；区分 SOURCE/CANDIDATE/VERIFIED 状态。给出未来 10 个交易日的预测目标价和观察止损价，明确这些是推断；证据不足则价格返回 null 并说明缺口。候选上下文：" + context},
                ],
                max_tokens=8192,
                metadata_json={"candidate_count": len(batch), "batch_index": batch_index, "skill_code": "STOCK_SELECTION_ANALYST",
                               "json_schema_output": schema, "native_structured_output": True},
            )
            if log.status != "SUCCESS":
                raise ValueError(f"model analysis failed; log_id={log.id}")
            parsed = SelectionModelOutput.model_validate_json(log.response_text or "{}")
            by_symbol = {item.symbol: item.model_dump() for item in parsed.candidates}
            if len(by_symbol) != len(parsed.candidates) or set(by_symbol) != {item["symbol"] for item in batch}:
                raise ValueError("模型必须逐一返回候选池中的股票，不能遗漏、重复或增加股票")
            for item in batch:
                allowed_ids = set(item["evidence_json"].get("model_document_ids") or
                                  item["evidence_json"].get("document_ids", []))
                if not set(by_symbol[item["symbol"]]["evidence_document_ids"]).issubset(allowed_ids):
                    raise ValueError("模型引用了候选上下文之外的文档")
                allowed_facts = set(item["evidence_json"].get("evidence_fact_ids", []))
                returned_facts = set(by_symbol[item["symbol"]].get("evidence_fact_ids") or [])
                if not returned_facts.issubset(allowed_facts):
                    raise ValueError("模型引用了候选上下文之外的事实")
                allowed_chunks = {str(row.get("id")) for row in item["evidence_json"].get("chunks", [])
                                  if isinstance(row, dict) and row.get("id")}
                returned_chunks = {str(value) for value in (by_symbol[item["symbol"]].get("evidence_chunk_ids") or [])}
                if not returned_chunks.issubset(allowed_chunks):
                    raise ValueError("模型引用了候选上下文之外的文档切片")
                predictions[item["symbol"]] = (by_symbol[item["symbol"]], log.id, log.instance_code)
        for item, _score in candidates:
            prediction = predictions.get(str(item["symbol"]))
            if prediction is not None:
                output, log_id, instance_code = prediction
                item["analysis_json"].update({"model": output, "model_output_status": "SUCCESS", "model_call_log_id": log_id,
                    "summary": output["reasoning"], "thesis": output["reasoning"], "target_price": output["target_price"],
                    "stop_price": output["stop_price"], "confidence": output["confidence"], "recommendation": output["decision"],
                    "actual_instance_code": instance_code})
        run.model_instance_code = log.instance_code
        run.analysis_mode = "MODEL_COMPLETED"
    except Exception as exc:
        run.analysis_mode = "MODEL_FAILED_FALLBACK_LOCAL"
        run.error_message = f"model analysis failed: {str(exc)[:300]}"
    if run.analysis_mode == "MODEL_FAILED_FALLBACK_LOCAL":
        for item, _score in candidates:
            item["analysis_json"]["model_output_status"] = "FAILED"


def review_candidate(db: Session, candidate_id: int, payload: SelectionReviewRequest) -> SelectionCandidate:
    candidate = db.get(SelectionCandidate, candidate_id)
    if candidate is None:
        raise LookupError("selection candidate not found")
    if candidate.decision != "PENDING":
        raise ValueError("selection candidate has already been reviewed")
    if payload.decision == "APPROVED" and (not candidate.entry_price or not candidate.entry_date):
        raise ValueError("candidate has no dated real price; cannot start tracking")
    candidate.decision = payload.decision
    candidate.review_notes = payload.notes
    candidate.target_price = payload.target_price
    candidate.stop_price = payload.stop_price
    candidate.target_return_pct = payload.target_return_pct
    candidate.confidence = payload.confidence
    candidate.reviewed_at = _now()
    db.flush()
    run = db.get(SelectionRun, candidate.run_id)
    if run:
        run.reviewed_count = len(db.scalars(select(SelectionCandidate).where(
            SelectionCandidate.run_id == run.id,
            SelectionCandidate.decision != "PENDING",
        )).all())
    tracking = None
    if payload.decision == "APPROVED":
        if candidate.entry_price is None:
            raise ValueError("candidate has no real entry price")
        skill = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == "STOCK_SELECTION_ANALYST", ModelSkill.enabled.is_(True)))
        target_return = payload.target_return_pct
        if payload.target_price is not None:
            target_return = (payload.target_price / candidate.entry_price - 1) * 100
        prediction = PredictionLedger(
            market=candidate.market, stock_code=candidate.symbol,
            action_type="WATCH" if target_return is not None and target_return < 0 else "BUY",
            target_timeframe="T+10", reasoning_logic=str(candidate.analysis_json.get("thesis", "人工确认选股")) + (f" {payload.notes}" if payload.notes else ""),
            skill_code=skill.skill_code if skill else "STOCK_SELECTION_ANALYST",
            skill_version_used=skill.version if skill else "UNREGISTERED",
            model_instance_code=run.model_instance_code if run else None,
            entry_price=candidate.entry_price, target_return_pct=target_return, status="PENDING",
            predicted_at=_now(), evaluation_json={"workflow": "selection_tracking", "candidate_id": candidate.id},
        )
        db.add(prediction)
        db.flush()
        candidate.prediction_id = prediction.id
        entry_date = candidate.entry_date
        if not entry_date:
            raise ValueError("candidate has no trade date; cannot start tracking")
        candidate.target_return_pct = target_return
        tracking = SelectionTracking(
            candidate_id=candidate.id, market=candidate.market, symbol=candidate.symbol,
            entry_price=candidate.entry_price, entry_date=entry_date,
            # Confirmation is anchored to the latest completed local-market
            # session; the in-progress session is never part of T+10.
            confirmation_date=_market_local_date(candidate.market, _now()),
            target_price=payload.target_price, stop_price=payload.stop_price,
            target_return_pct=target_return, confidence=payload.confidence,
            data_mode=run.data_mode if run else "REAL",
        )
        db.add(tracking)
        db.flush()
        if run:
            run.tracking_count += 1
    from app.services.selection_audit import capture_decision_snapshot
    capture_decision_snapshot(
        db, candidate, snapshot_kind="REVIEW", decision=payload.decision,
        tracking_id=tracking.id if tracking else None,
        prediction_id=candidate.prediction_id,
        evidence=candidate.evidence_json,
        as_of=candidate.reviewed_at or _now(),
    )
    if run and run.reviewed_count >= run.candidate_count:
        run.status = "TRACKING" if run.tracking_count else "COMPLETED"
    db.commit()
    db.refresh(candidate)
    return candidate


def _refresh_one(db: Session, tracking: SelectionTracking) -> SelectionTracking:
    if tracking.status == "COMPLETED":
        return tracking
    today = _last_completed_trade_date(tracking.market, _now())
    bars = list(db.scalars(select(StockKline).where(
        StockKline.market == tracking.market,
        StockKline.symbol == tracking.symbol,
        StockKline.period == "daily",
        StockKline.adjust == tracking.adjust,
        StockKline.trade_date > tracking.confirmation_date,
        StockKline.trade_date <= today,
    ).order_by(StockKline.trade_date)).all())
    if tracking.data_mode == "REAL" and (not bars or bars[-1].trade_date < today):
        # Refresh only approved symbols, and keep the failure visible.  This
        # call is intentionally outside any LLM path; API/Worker callers can
        # choose to disable external adapters by using MOCK data.
        try:
            from app.services.stock_on_demand import StockOnDemandService
            from app.services.catalog import select_data_source
            source = select_data_source(db, tracking.market, "KLINE")
            if source:
                end = datetime.fromisoformat(today).date()
                StockOnDemandService(db).fetch_kline(
                    source, tracking.market, tracking.symbol, "daily", tracking.adjust,
                    tracking.confirmation_date.replace("-", ""), end.strftime("%Y%m%d"), True,
                )
                db.flush()
                bars = list(db.scalars(select(StockKline).where(
                    StockKline.market == tracking.market, StockKline.symbol == tracking.symbol,
                    StockKline.period == "daily", StockKline.adjust == tracking.adjust,
                    StockKline.trade_date > tracking.confirmation_date,
                    StockKline.trade_date <= end.isoformat(),
                ).order_by(StockKline.trade_date)).all())
        except Exception as exc:
            tracking.review_json = {**(tracking.review_json or {}), "data_fetch_error": str(exc)[:300]}
    if not bars:
        tracking.status = "PENDING_DATA"
        from app.services.selection_audit import capture_retrospective
        capture_retrospective(db, tracking)
        return tracking
    # Late-arriving bars are reconciled at their true chronological position.
    # Once the first ten valid sessions are complete, their result is frozen.
    by_date: dict[str, StockKline] = {}
    for bar in bars:
        close = _as_float(bar.close_price)
        volume = _as_float(bar.volume)
        try:
            weekday = datetime.fromisoformat(bar.trade_date[:10]).weekday()
        except ValueError:
            continue
        if close is None or close <= 0 or weekday >= 5 or volume is not None and volume <= 0:
            continue
        previous = by_date.get(bar.trade_date)
        if previous is None or bar.fetched_at > previous.fetched_at:
            by_date[bar.trade_date] = bar
    valid_bars = sorted(by_date.values(), key=lambda row: row.trade_date)[:tracking.required_sessions]
    if not valid_bars:
        tracking.status = "PENDING_DATA"
        from app.services.selection_audit import capture_retrospective
        capture_retrospective(db, tracking)
        return tracking
    db.execute(delete(SelectionSnapshot).where(SelectionSnapshot.tracking_id == tracking.id))
    peak = tracking.entry_price
    for sequence, bar in enumerate(valid_bars, 1):
        close = float(bar.close_price)
        peak = max(peak, close)
        ret = (close / tracking.entry_price - 1) * 100
        drawdown = (close / peak - 1) * 100 if peak else 0
        db.add(SelectionSnapshot(tracking_id=tracking.id, trade_date=bar.trade_date, close_price=close,
                                 return_pct=ret, drawdown_pct=drawdown, sequence=sequence, data_mode=tracking.data_mode))
    db.flush()
    all_snapshots = list(db.scalars(select(SelectionSnapshot).where(SelectionSnapshot.tracking_id == tracking.id).order_by(SelectionSnapshot.sequence)).all())
    if not all_snapshots:
        tracking.status = "PENDING_DATA"
        from app.services.selection_audit import capture_retrospective
        capture_retrospective(db, tracking)
        return tracking
    latest = all_snapshots[-1]
    tracking.observed_sessions = len(all_snapshots)
    tracking.latest_date = latest.trade_date
    tracking.latest_price = latest.close_price
    tracking.cumulative_return_pct = latest.return_pct
    tracking.max_drawdown_pct = min(item.drawdown_pct for item in all_snapshots)
    if tracking.target_price is not None:
        target_is_downside = tracking.target_price < tracking.entry_price
        tracking.target_hit = any(
            item.close_price <= tracking.target_price if target_is_downside else item.close_price >= tracking.target_price
            for item in all_snapshots
        )
    elif tracking.target_return_pct is not None:
        target_is_downside = tracking.target_return_pct < 0
        tracking.target_hit = any(
            item.return_pct <= tracking.target_return_pct if target_is_downside else item.return_pct >= tracking.target_return_pct
            for item in all_snapshots
        )
    else:
        tracking.target_hit = None
    tracking.stop_hit = (
        any(item.close_price <= tracking.stop_price for item in all_snapshots)
        if tracking.stop_price is not None else None
    )
    tracking.status = "COMPLETED" if len(all_snapshots) >= tracking.required_sessions else "TRACKING"
    target_gap: float | None = None
    if tracking.target_price is not None:
        target_gap = (latest.close_price / tracking.entry_price - 1) * 100 - ((tracking.target_price / tracking.entry_price - 1) * 100)
    elif tracking.target_return_pct is not None:
        target_gap = latest.return_pct - tracking.target_return_pct
    tracking.direction_hit = None
    if tracking.status == "COMPLETED":
        expected = tracking.target_return_pct or 0
        tracking.direction_hit = (latest.return_pct >= 0) if expected >= 0 else (latest.return_pct < 0)
        tracking.completed_at = tracking.completed_at or _now()
        tracking.review_json = {
            "required_sessions_met": True,
            "observed_sessions": len(all_snapshots),
            "target_gap_pct_points": round(target_gap, 6) if target_gap is not None else None,
            "evaluation": "TARGET_HIT" if tracking.target_hit else "TARGET_NOT_HIT",
            "adjustment_suggestion": "保留当前规则并复核证据" if tracking.direction_hit else "提高硬过滤门槛并复核失效证据",
            "summary": f"已完成 {len(all_snapshots)} 个有效交易日跟踪，期末收益 {latest.return_pct:.2f}%；目标价{'曾达到' if tracking.target_hit else '未达到'}，观察止损{'曾触及' if tracking.stop_hit else '未触及'}。",
            "adjustments": ["比较目标收益与实际收益偏差，复核当时引用的基本面与外部事件。", "若触及止损或方向判断偏离，应调整筛选阈值并补足缺失证据后再发起新预测。"],
            "target_hit": tracking.target_hit, "stop_hit": tracking.stop_hit,
            "price_basis": "UNADJUSTED_CLOSE", "confirmation_date": tracking.confirmation_date,
            "evidence": [item.trade_date for item in all_snapshots],
        }
        candidate = db.get(SelectionCandidate, tracking.candidate_id)
        prediction = db.get(PredictionLedger, candidate.prediction_id) if candidate and candidate.prediction_id else None
        if prediction:
            prediction.status = "EVALUATED"
            prediction.actual_price = latest.close_price
            prediction.actual_return_pct = latest.return_pct
            prediction.direction_hit = tracking.direction_hit
            prediction.price_as_of = datetime.fromisoformat(latest.trade_date).replace(tzinfo=timezone.utc)
            prediction.evaluated_at = _now()
            prediction.evaluation_json = {**tracking.review_json, "workflow": "selection_tracking", "tracking_id": tracking.id}
    # Append a review revision for both in-progress and completed states.  The
    # state hash makes repeated scheduler runs idempotent and preserves late
    # arriving-bar corrections as a new revision.
    from app.services.selection_audit import capture_retrospective
    capture_retrospective(db, tracking)
    return tracking


def refresh_tracking(db: Session, tracking_ids: list[int] | None = None) -> dict[str, Any]:
    query = select(SelectionTracking).order_by(SelectionTracking.id)
    if tracking_ids:
        query = query.where(SelectionTracking.id.in_(tracking_ids))
    rows = list(db.scalars(query).all())
    for row in rows:
        _refresh_one(db, row)
    # Close the parent selection run only when every human-approved tracking
    # record has completed its required sessions.  Rejected/pending candidates
    # keep the run in their existing review state.
    candidate_ids = {row.candidate_id for row in rows}
    if candidate_ids:
        candidates = list(db.scalars(select(SelectionCandidate).where(SelectionCandidate.id.in_(candidate_ids))).all())
        run_ids = {candidate.run_id for candidate in candidates}
        for run_id in run_ids:
            run = db.get(SelectionRun, run_id)
            if run is None or run.reviewed_count < run.candidate_count:
                continue
            all_run_candidates = list(db.scalars(select(SelectionCandidate).where(SelectionCandidate.run_id == run_id)).all())
            approved_ids = [candidate.id for candidate in all_run_candidates if candidate.decision == "APPROVED"]
            tracked = list(db.scalars(select(SelectionTracking).where(SelectionTracking.candidate_id.in_(approved_ids))).all()) if approved_ids else []
            run.tracking_count = len(tracked)
            if approved_ids and len(tracked) == len(approved_ids) and all(item.status == "COMPLETED" for item in tracked):
                run.status = "COMPLETED"
            elif tracked:
                run.status = "TRACKING"
    db.commit()
    return {
        "status": "COMPLETED",
        "refreshed_count": len(rows),
        "completed_count": sum(row.status == "COMPLETED" for row in rows),
        "pending_data_count": sum(row.status == "PENDING_DATA" for row in rows),
        "tracking_count": sum(row.status == "TRACKING" for row in rows),
    }
