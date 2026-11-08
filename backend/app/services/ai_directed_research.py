"""Evidence-first, human-gated research for the single-stock trial.

This module is intentionally separate from the legacy eight-dimension scorecard.
It stores only a versioned research view under ``SelectionCandidate.analysis_json``;
facts, relations and source documents remain owned by the existing evidence model.
"""

from __future__ import annotations

from copy import deepcopy
from datetime import datetime, timezone
import json
import re
from typing import Any

from sqlalchemy.orm import Session

from app.models.ai_hub import ModelCallLog
from app.models.selection import SelectionCandidate, SelectionResearchFeedback, SelectionRun
from app.schemas.selection import (
    SelectionAiDirectedResearchOutput,
    SelectionAiResearchConfirmRequest,
)
from app.services.model_hub import ModelHubService
from app.services.selection import _model_candidate_context


def _now() -> str:
    return datetime.now(timezone.utc).isoformat()


def _json_object(value: str | None) -> dict[str, Any]:
    raw = (value or "").strip()
    candidates = [raw]
    if raw.startswith("```"):
        candidates.insert(0, raw.split("\n", 1)[-1].rsplit("```", 1)[0].strip())
    start, end = raw.find("{"), raw.rfind("}")
    if start >= 0 and end > start:
        candidates.append(raw[start:end + 1])
    for text in candidates:
        try:
            payload = json.loads(text)
            if isinstance(payload, dict):
                return payload
        except (TypeError, ValueError):
            continue
    raise ValueError("AI主动研究模型未返回可解析的JSON对象")


def _text(value: Any, limit: int = 600) -> str:
    return str(value or "").strip()[:limit]


def _evidence_pack(candidate: SelectionCandidate) -> dict[str, Any]:
    item = {
        "id": candidate.id, "market": candidate.market, "symbol": candidate.symbol,
        "name": candidate.name, "entry_price": candidate.entry_price,
        "entry_date": candidate.entry_date, "hard_score": candidate.hard_score,
        "hard_rules_json": candidate.hard_rules_json or {},
        "evidence_json": candidate.evidence_json or {},
        "analysis_json": {},
    }
    context = _model_candidate_context(item)
    evidence = context.get("evidence_json") or {}
    # The model view is bounded again here because this endpoint is interactive.
    # The persisted candidate evidence remains complete and unchanged.
    docs = list(evidence.get("documents") or [])
    docs.sort(key=lambda row: str(row.get("published_at") or ""), reverse=True)
    evidence["documents"] = docs[:24]
    evidence["facts"] = list(evidence.get("facts") or [])[:24]
    evidence["chunks"] = list(evidence.get("chunks") or [])[:12]
    evidence["graph_paths"] = list(evidence.get("graph_paths") or [])[:12]
    evidence["relations"] = list(evidence.get("relations") or [])[:12]
    visible_document_ids = {row.get("id") for row in evidence["documents"] if row.get("id") is not None}
    visible_fact_ids = {str(row.get("id")) for row in evidence["facts"] if row.get("id") is not None}
    visible_relation_ids = {str(row.get("id")) for row in evidence["graph_paths"] if row.get("id") is not None}
    original_allow = evidence.get("citation_allowlist") or {}
    evidence["citation_allowlist"] = {
        **original_allow,
        "document_ids": sorted(visible_document_ids, key=str),
        "news_document_ids": [row.get("id") for row in evidence["documents"] if row.get("source_table") == "stock_news"],
        "notice_document_ids": [row.get("id") for row in evidence["documents"] if row.get("source_table") == "stock_notice"],
        "fact_ids": sorted(visible_fact_ids),
        "financial_fact_ids": [value for value in original_allow.get("financial_fact_ids") or [] if str(value) in visible_fact_ids],
        "market_fact_ids": [value for value in original_allow.get("market_fact_ids") or [] if str(value) in visible_fact_ids],
        "fund_flow_fact_ids": [value for value in original_allow.get("fund_flow_fact_ids") or [] if str(value) in visible_fact_ids],
        "business_fact_ids": [value for value in original_allow.get("business_fact_ids") or [] if str(value) in visible_fact_ids],
        "verified_relation_fact_ids": [value for value in original_allow.get("verified_relation_fact_ids") or [] if str(value) in visible_fact_ids],
        "relation_ids": sorted(visible_relation_ids),
    }
    context["evidence_json"] = evidence
    return context


def _allowlists(pack: dict[str, Any]) -> dict[str, set[str]]:
    evidence = pack.get("evidence_json") or {}
    allow = evidence.get("citation_allowlist") or {}
    docs = {str(value) for value in allow.get("document_ids") or []}
    docs.update(str(row.get("id")) for row in evidence.get("documents") or [] if row.get("id") is not None)
    facts = {str(value) for value in allow.get("fact_ids") or []}
    facts.update(str(row.get("id")) for row in evidence.get("facts") or [] if row.get("id") is not None)
    relations = {str(value) for value in allow.get("relation_ids") or []}
    relations.update(str(row.get("id")) for row in evidence.get("graph_paths") or [] if row.get("id") is not None)
    return {"DOCUMENT": docs, "FACT": facts, "RELATION": relations}


def _validate_references(payload: dict[str, Any], pack: dict[str, Any]) -> tuple[dict[str, Any], list[str]]:
    allow = _allowlists(pack)
    warnings: list[str] = []
    documents = {
        str(row.get("id")): row for row in (pack.get("evidence_json") or {}).get("documents") or []
        if row.get("id") is not None
    }

    def clean(rows: Any, label: str) -> list[dict[str, Any]]:
        result: list[dict[str, Any]] = []
        for row in rows if isinstance(rows, list) else []:
            if not isinstance(row, dict):
                continue
            ref_type = str(row.get("reference_type") or "").upper()
            ref_id = str(row.get("reference_id") or "")
            if ref_type not in allow or ref_id not in allow[ref_type]:
                warnings.append(f"{label}引用未在本次证据白名单中：{ref_type}:{ref_id}")
                continue
            item = dict(row)
            if ref_type == "DOCUMENT":
                quote = _text(item.get("quote"), 500)
                source = _text(documents.get(ref_id, {}).get("content") or documents.get(ref_id, {}).get("excerpt"), 4000)
                # Quotes are required for document evidence. A mismatch is
                # retained as a warning and the quote is removed, never forged.
                if quote and quote not in source:
                    normalized_quote = re.sub(r"\s+", "", quote)
                    normalized_source = re.sub(r"\s+", "", source)
                    if normalized_quote not in normalized_source:
                        item["quote"] = None
                        warnings.append(f"{label}文档#{ref_id}引句无法在已保存原文窗口中连续核对")
                if not quote:
                    warnings.append(f"{label}文档#{ref_id}未返回原文引句")
            result.append(item)
        return result

    output = deepcopy(payload)
    output["key_findings"] = clean(output.get("key_findings"), "关键事实")
    output["major_events"] = clean(output.get("major_events"), "重大事项")
    for direction in output.get("directions") or []:
        direction["supporting_evidence"] = clean(direction.get("supporting_evidence"), f"方向{direction.get('direction_id')}支持证据")
        direction["counter_evidence"] = clean(direction.get("counter_evidence"), f"方向{direction.get('direction_id')}反证")
    output["data_quality_notes"] = list(dict.fromkeys([
        *[_text(value, 400) for value in output.get("data_quality_notes") or []], *warnings,
    ]))[:10]
    return output, warnings


def _adapt_provider_direction_format(payload: dict[str, Any], pack: dict[str, Any]) -> tuple[dict[str, Any], bool]:
    """Adapt a provider's prose-oriented direction shape without inventing citations.

    Some OpenAI-compatible endpoints ignore the requested JSON schema and
    return ``company_profile``/``research_directions``. We can preserve the
    useful planning text, while preserving only IDs that are present in the
    visible evidence allowlist. The result is still marked as needing citation
    review when the provider omits continuous source quotes.
    """
    if isinstance(payload.get("directions"), list):
        return payload, False
    rows = payload.get("research_directions")
    if not isinstance(rows, list):
        return payload, False
    allow = _allowlists(pack)
    profile = payload.get("company_profile") if isinstance(payload.get("company_profile"), dict) else {}
    profile_text = _text(profile.get("main_business"), 500)
    industry = _text(profile.get("industry"), 240)
    business_profile = "；".join(value for value in [profile_text, f"所属赛道：{industry}" if industry else ""] if value)
    directions: list[dict[str, Any]] = []
    for index, row in enumerate(rows[:5], start=1):
        if not isinstance(row, dict):
            continue
        raw_dims = [str(value).lower() for value in row.get("data_dimensions") or []]
        dims: list[str] = []
        if any("profit" in value or "financial" in value for value in raw_dims):
            dims.append("FINANCIAL")
        if any("market" in value or "trend" in value or "order" in value or "sales" in value for value in raw_dims):
            dims.extend(["NEWS", "RESEARCH"])
        if any("management" in value or "strategy" in value or "structure" in value for value in raw_dims):
            dims.extend(["NOTICE", "COMPANY"])
        if any("compet" in value or "chain" in value for value in raw_dims):
            dims.extend(["GRAPH", "RESEARCH"])
        def evidence_rows(raw: Any, stance: str) -> list[dict[str, Any]]:
            if isinstance(raw, str):
                return []
            result: list[dict[str, Any]] = []
            for ref_type, ref_id in (raw.items() if isinstance(raw, dict) else []):
                reference_type = str(ref_type).upper()
                reference_id = str(ref_id)
                if reference_type not in allow or reference_id not in allow[reference_type]:
                    continue
                result.append({
                    "reference_type": reference_type, "reference_id": reference_id,
                    "stance": stance, "claim": _text(row.get("current_judgment"), 500) or "模型方向引用",
                    "quote": None,
                })
            return result
        support = evidence_rows(row.get("supporting_evidence"), "SUPPORT")
        counter = evidence_rows(row.get("counter_evidence"), "COUNTER")
        gaps = [_text(row.get("gap"), 400), "模型未返回可连续核对的原文引句，需人工核对。"]
        if row.get("supporting_evidence") and not support:
            gaps.append("模型提供的支持证据ID不在本次可见证据白名单中。")
        if row.get("counter_evidence") and not counter:
            gaps.append("模型提供的反证ID不在本次可见证据白名单中。")
        directions.append({
            "direction_id": f"direction_{index}",
            "title": _text(row.get("hypothesis"), 150) or f"待验证研究方向{index}",
            "hypothesis": _text(row.get("hypothesis"), 800) or "模型未提供假设文本",
            "why_it_matters": _text(row.get("importance"), 800) or "模型未说明重要性",
            "current_judgment": _text(row.get("current_judgment"), 1000) or "尚未形成判断",
            "confidence": 0.35,
            "supporting_evidence": support,
            "counter_evidence": counter,
            "data_gaps": list(dict.fromkeys(gaps))[:8],
            "suggested_sources": [_text(row.get("suggestion_source"), 300)] if row.get("suggestion_source") else [],
            "suggested_dimensions": list(dict.fromkeys(dims))[:8],
            "recommended_priority": "PRIORITY" if int(row.get("priority") or 9) <= 1 else "NORMAL",
        })
    if len(directions) < 3:
        return payload, False
    def human_value(value: Any) -> str:
        if isinstance(value, (dict, list)):
            return json.dumps(value, ensure_ascii=False, separators=("，", "："))
        return _text(value, 300)

    evidence = pack.get("evidence_json") or {}
    facts = [row for row in evidence.get("facts") or [] if isinstance(row, dict)]
    business_fact = next((row for row in facts if str(row.get("fact_type") or "").upper() == "BUSINESS_PROFILE"), None)
    financial_fact = next((row for row in facts if str(row.get("fact_type") or "").upper() == "FINANCIAL_OBSERVATION"), None)
    key_findings: list[dict[str, Any]] = []
    if business_fact and profile_text:
        key_findings.append({
            "reference_type": "FACT", "reference_id": str(business_fact.get("id")), "stance": "FACT",
            "claim": profile_text, "quote": None,
        })
    revenue_text, profit_text = human_value(profile.get("revenue")), human_value(profile.get("profit"))
    if financial_fact and revenue_text:
        key_findings.append({
            "reference_type": "FACT", "reference_id": str(financial_fact.get("id")), "stance": "FACT",
            "claim": f"收入：{revenue_text}", "quote": None,
        })
    if financial_fact and profit_text:
        key_findings.append({
            "reference_type": "FACT", "reference_id": str(financial_fact.get("id")), "stance": "FACT",
            "claim": f"利润：{profit_text}", "quote": None,
        })
    summary = "；".join(value for value in [profile_text, revenue_text, profit_text] if value)
    return {
        "mode": "AI_DIRECTED_RESEARCH_V1",
        "summary": summary or "模型已读取现有证据，但只返回了方向规划文本。",
        "business_profile": business_profile or "模型未返回可核验的主营画像。",
        "key_findings": key_findings, "major_events": [], "directions": directions,
        "preliminary_conclusion": "；".join(_text(row.get("current_judgment"), 300) for row in rows[:5] if isinstance(row, dict)),
        "conclusion_status": "PARTIAL",
        "data_quality_notes": ["模型未遵循本次证据引用契约；已保留规划文本，但未把无ID的描述当作可验证证据。"],
    }, True


def _state(candidate: SelectionCandidate) -> dict[str, Any]:
    analysis = candidate.analysis_json if isinstance(candidate.analysis_json, dict) else {}
    value = analysis.get("ai_directed_research")
    return dict(value) if isinstance(value, dict) else {}


def prepare_ai_directed_research(
    db: Session, candidate_id: int, *, model_instance_code: str | None = None,
) -> SelectionCandidate:
    candidate = db.get(SelectionCandidate, candidate_id)
    if candidate is None:
        raise LookupError("选股候选不存在")
    run = db.get(SelectionRun, candidate.run_id)
    if run is None:
        raise LookupError("选股批次不存在")
    pack = _evidence_pack(candidate)
    evidence = pack.get("evidence_json") or {}
    prompt_context = {
        "candidate_id": candidate.id,
        "market": candidate.market,
        "symbol": candidate.symbol,
        "name": candidate.name,
        "as_of": run.as_of.isoformat() if run.as_of else _now(),
        "evidence": evidence,
    }
    prompt = (
        "请先阅读下列单股证据包，为用户准备AI主动研究方向。\n"
        "你必须先给出主营/收入/利润/行业与产业链/公司行为/量价资金的公司画像，再列出3至5个最可能改变判断的研究方向。\n"
        "不要直接输出买卖建议，不得补造缺失数据；把事实、推断、假设、反证和数据缺口分开。\n"
        "严格只引用citation_allowlist中的DOCUMENT、FACT、RELATION ID。\n"
        "输出必须是调用方提供的JSON Schema对象。\n\n"
        + json.dumps(prompt_context, ensure_ascii=False, separators=(",", ":"))
    )
    analysis = deepcopy(candidate.analysis_json or {})
    previous = _state(candidate)
    log: ModelCallLog | None = None
    try:
        service = ModelHubService(db)
        log: ModelCallLog = service.chat(
            task_type="stock_screening",
            messages=[{"role": "user", "content": prompt}],
            instance_code=model_instance_code or run.model_instance_code,
            temperature=0.15,
            max_tokens=5200,
            metadata_json={
                "skill_codes": ["AI_DIRECTED_RESEARCH_PLANNER", "COUNTER_EVIDENCE_REVIEWER", "EVIDENCE_GAP_DETECTOR"],
                "require_skills": True,
                "require_real_model": True,
                "strict_instance": bool(model_instance_code),
                "candidate_count": 1,
                "candidate_id": candidate.id,
                "evidence_ids": list((evidence.get("citation_allowlist") or {}).get("document_ids") or []),
                "json_schema_output": SelectionAiDirectedResearchOutput.model_json_schema(),
                "bounded_schema_repair": True,
            },
        )
        payload = _json_object(log.response_text)
        payload, contract_repaired = _adapt_provider_direction_format(payload, pack)
        validated = SelectionAiDirectedResearchOutput.model_validate(payload)
        cleaned, citation_warnings = _validate_references(validated.model_dump(mode="json"), pack)
        state = {
            **cleaned,
            "status": "WAITING_USER_DIRECTION",
            "interaction_mode": "AI_FIRST_HUMAN_GATE",
            "review_phase": "DIRECTION",
            "candidate_id": candidate.id,
            "market": candidate.market,
            "symbol": candidate.symbol,
            "generated_at": _now(),
            "model_call_log_id": log.id,
            "model_instance_code": log.instance_code,
            "skill_code": "AI_DIRECTED_RESEARCH_PLANNER",
            "skill_version": "AI_DIRECTED_RESEARCH_V1",
            "evidence_snapshot": {
                "context_hash": evidence.get("context_hash"),
                "document_ids": sorted(str(value) for value in _allowlists(pack)["DOCUMENT"]),
                "fact_ids": sorted(_allowlists(pack)["FACT"]),
                "relation_ids": sorted(_allowlists(pack)["RELATION"]),
                "counts": evidence.get("counts") or {},
                "missing_data": evidence.get("missing_data") or [],
            },
            "citation_warnings": citation_warnings,
            "contract_repaired": contract_repaired,
            "history": [
                *list(previous.get("history") or []),
                {"at": _now(), "action": "PREPARE", "status": "WAITING_USER_DIRECTION", "model_call_log_id": log.id},
            ][-10:],
        }
    except Exception as exc:
        state = {
            **previous,
            "mode": "AI_DIRECTED_RESEARCH_V1",
            "status": "MODEL_FAILED",
            "candidate_id": candidate.id,
            "market": candidate.market,
            "symbol": candidate.symbol,
            "generated_at": _now(),
            "error": f"{type(exc).__name__}: {str(exc)[:800]}",
            "model_call_log_id": log.id if log is not None else None,
            "model_instance_code": log.instance_code if log is not None else model_instance_code,
            "model_error_message": log.error_message if log is not None else None,
            "next_action": "检查模型额度/连通性后重试；不会把失败响应当作结论。",
            "history": [
                *list(previous.get("history") or []),
                {"at": _now(), "action": "PREPARE", "status": "MODEL_FAILED", "error": str(exc)[:400]},
            ][-10:],
        }
    analysis["ai_directed_research"] = state
    candidate.analysis_json = analysis
    db.commit()
    db.refresh(candidate)
    return candidate


def confirm_ai_directed_research(
    db: Session, candidate_id: int, payload: SelectionAiResearchConfirmRequest,
) -> SelectionCandidate:
    candidate = db.get(SelectionCandidate, candidate_id)
    if candidate is None:
        raise LookupError("选股候选不存在")
    analysis = deepcopy(candidate.analysis_json or {})
    state = _state(candidate)
    directions = [row for row in state.get("directions") or [] if isinstance(row, dict)]
    by_id = {str(row.get("direction_id")): row for row in directions}
    unknown = [value for value in payload.selected_direction_ids if value not in by_id]
    if unknown:
        raise ValueError(f"研究方向不存在或已过期：{', '.join(unknown)}")
    selected = set(payload.selected_direction_ids)
    for row in directions:
        row["user_selected"] = str(row.get("direction_id")) in selected
        row["user_priority"] = "PRIORITY" if row["user_selected"] else "PAUSED"
    dimensions = sorted({
        str(dimension) for row in directions if row.get("user_selected")
        for dimension in row.get("suggested_dimensions") or []
        if str(dimension) in {"NEWS", "NOTICE", "FINANCIAL", "PRICE_VOLUME", "FUND_FLOW", "COMPANY", "RESEARCH", "GRAPH"}
    })
    status = "READY_FOR_EVIDENCE_COLLECTION" if payload.action == "CONFIRM_AND_RESEARCH" else (
        "DIRECTION_DRAFT_SAVED" if payload.action == "SAVE_DRAFT" else "STOPPED_BY_USER"
    )
    state.update({
        "status": status,
        "review_phase": "EVIDENCE_COLLECTION" if payload.action == "CONFIRM_AND_RESEARCH" else "DIRECTION",
        "directions": directions,
        "selected_direction_ids": list(payload.selected_direction_ids),
        "user_comment": payload.comment,
        "reviewer": payload.reviewer,
        "confirmed_at": _now(),
        "collection_plan": {
            "dimensions": dimensions,
            "sources": sorted({source for row in directions if row.get("user_selected") for source in row.get("suggested_sources") or []}),
            "data_gaps": sorted({gap for row in directions if row.get("user_selected") for gap in row.get("data_gaps") or []}),
            "note": "本计划只允许调用现有受控采集服务；尚未将推断写入事实表。",
        },
        "history": [
            *list(state.get("history") or []),
            {"at": _now(), "action": payload.action, "status": status,
             "selected_direction_ids": list(payload.selected_direction_ids), "reviewer": payload.reviewer},
        ][-10:],
    })
    feedback = SelectionResearchFeedback(
        run_id=candidate.run_id, candidate_id=candidate.id, phase="AI_DIRECTED_DIRECTION",
        action=payload.action, payload_json={"selected_direction_ids": list(payload.selected_direction_ids), "comment": payload.comment},
        outcome_status="PENDING", skill_code="AI_DIRECTED_RESEARCH_PLANNER", skill_version="AI_DIRECTED_RESEARCH_V1",
        reviewer=payload.reviewer,
    )
    db.add(feedback)
    analysis["ai_directed_research"] = state
    candidate.analysis_json = analysis
    db.commit()
    db.refresh(candidate)
    return candidate


def ai_directed_research_read(candidate: SelectionCandidate) -> dict[str, Any]:
    state = _state(candidate)
    if not state:
        return {"candidate_id": candidate.id, "status": "NOT_STARTED", "message": "尚未读取现有证据并提出研究方向"}
    return state
