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

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.ai_hub import ModelCallLog
from app.models.selection import SelectionCandidate, SelectionResearchFeedback, SelectionRun
from app.schemas.selection import (
    SelectionAiDirectedResearchOutput,
    SelectionAiDirectedResearchVerificationOutput,
    SelectionAiResearchInference,
    SelectionAiResearchConfirmRequest,
)
from app.services.model_hub import ModelHubService
from app.services.selection import (
    _model_candidate_context,
    _research_evidence_state,
    execute_selection_data_backfill,
)


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
    for inference in output.get("inference_candidates") or []:
        inference["supporting_evidence"] = clean(
            inference.get("supporting_evidence"), f"推理{inference.get('inference_id')}支持证据",
        )
        inference["counter_evidence"] = clean(
            inference.get("counter_evidence"), f"推理{inference.get('inference_id')}反证",
        )
    output["data_quality_notes"] = list(dict.fromkeys([
        *[_text(value, 400) for value in output.get("data_quality_notes") or []], *warnings,
    ]))[:10]
    return output, warnings


def _inference_candidates_from_directions(
    directions: list[dict[str, Any]], existing: Any = None,
) -> list[dict[str, Any]]:
    """Expose the model's current judgments as selectable preliminary conclusions.

    This is a shape conversion, not a new inference: the conclusion text comes
    from the model direction itself, and its confidence is capped until the
    user-approved evidence review completes. It prevents a failed second model
    call from erasing useful first-pass reasoning.
    """
    if isinstance(existing, list) and existing:
        # Keep model-authored conclusions, but enforce the planner contract at
        # the boundary.  A preliminary inference is never allowed to exceed
        # the confidence ceiling before the user selects a direction and the
        # verifier has checked the evidence again.
        direction_ids = {
            str(row.get("direction_id")) for row in directions
            if isinstance(row, dict) and row.get("direction_id")
        }
        result: list[dict[str, Any]] = []
        for row in existing[:8]:
            if not isinstance(row, dict):
                continue
            item = dict(row)
            if direction_ids and str(item.get("direction_id")) not in direction_ids:
                continue
            try:
                item["confidence"] = min(0.6, max(0.0, float(item.get("confidence") or 0.0)))
            except (TypeError, ValueError):
                item["confidence"] = 0.0
            if item.get("status") not in {"PRELIMINARY", "NEEDS_VERIFICATION", "LOW_CONFIDENCE"}:
                item["status"] = "NEEDS_VERIFICATION"
            result.append(item)
        if result:
            return result
    result: list[dict[str, Any]] = []
    for row in directions[:8]:
        if not isinstance(row, dict):
            continue
        direction_id = _text(row.get("direction_id"), 80)
        if not direction_id:
            continue
        conclusion = _text(row.get("current_judgment") or row.get("hypothesis"), 1200)
        if len(conclusion) < 2:
            continue
        supporting = list(row.get("supporting_evidence") or [])[:8]
        counter = list(row.get("counter_evidence") or [])[:8]
        result.append({
            "inference_id": f"inference_{direction_id}",
            "direction_id": direction_id,
            "title": _text(row.get("title") or row.get("hypothesis"), 180) or "AI初步推理",
            "conclusion": conclusion,
            "conclusion_type": "MIXED" if supporting and counter else "UNKNOWN",
            "rationale": _text(row.get("why_it_matters") or "模型已结合现有证据形成初步判断", 1200),
            "confidence": min(0.6, max(0.0, float(row.get("confidence") or 0.0))),
            "supporting_evidence": supporting,
            "counter_evidence": counter,
            "implications": ["这是基于当前数据的初步推理，不等同于已验证事实或投资建议。"],
            "suggested_followups": [
                _text(value, 300) for value in row.get("suggested_sources") or [] if _text(value, 300)
            ][:8],
            "status": "NEEDS_VERIFICATION",
        })
    return result


def _ensure_inference_candidates(state: dict[str, Any]) -> dict[str, Any]:
    state = deepcopy(state)
    state["inference_candidates"] = _inference_candidates_from_directions(
        list(state.get("directions") or []), state.get("inference_candidates"),
    )
    return state


def _adapt_provider_direction_format(payload: dict[str, Any], pack: dict[str, Any]) -> tuple[dict[str, Any], bool]:
    """Adapt a provider's prose-oriented direction shape without inventing citations.

    Some OpenAI-compatible endpoints ignore the requested JSON schema and
    return ``company_profile``/``research_directions``. We can preserve the
    useful planning text, while preserving only IDs that are present in the
    visible evidence allowlist. The result is still marked as needing citation
    review when the provider omits continuous source quotes.
    """
    if isinstance(payload.get("directions"), list):
        # Some providers return the old planner shape while still satisfying
        # the direction contract.  Fill the new user-facing inference section
        # from those judgments without inventing facts.
        if not isinstance(payload.get("inference_candidates"), list) or not payload.get("inference_candidates"):
            payload = dict(payload)
            payload["inference_candidates"] = _inference_candidates_from_directions(
                list(payload.get("directions") or []),
            )
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
    adapted = {
        "mode": "AI_DIRECTED_RESEARCH_V1",
        "summary": summary or "模型已读取现有证据，但只返回了方向规划文本。",
        "business_profile": business_profile or "模型未返回可核验的主营画像。",
        "key_findings": key_findings, "major_events": [], "directions": directions,
        "preliminary_conclusion": "；".join(_text(row.get("current_judgment"), 300) for row in rows[:5] if isinstance(row, dict)),
        "conclusion_status": "PARTIAL",
        "data_quality_notes": ["模型未遵循本次证据引用契约；已保留规划文本，但未把无ID的描述当作可验证证据。"],
    }
    adapted["inference_candidates"] = _inference_candidates_from_directions(directions)
    return adapted, True


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
        "你必须先给出主营/收入/利润/行业与产业链/公司行为/量价资金的公司画像，先形成3至6个可供用户参考的AI初步推理结论，再为每条结论关联一个可验证研究方向。\n"
        "每条初步推理必须包含明确结论、推理依据、支持证据、反证或不确定性、当前置信度、可能影响和建议深入研究内容；它是基于现有证据的临时判断，不是事实、最终结论或买卖建议。\n"
        "即使当前证据不完整，也要先给出合理的竞争性解释；‘证据不足’只能作为状态和置信度说明，不能抹掉初步推理。\n"
        "不要直接输出买卖建议，不得补造缺失数据；把事实、推断、假设、反证和数据缺口分开。\n"
        "严格只引用citation_allowlist中的DOCUMENT、FACT、RELATION ID。\n"
        "输出必须是调用方提供的JSON Schema对象。\n\n"
        + json.dumps(prompt_context, ensure_ascii=False, separators=(",", ":"))
    )
    analysis = deepcopy(candidate.analysis_json or {})
    previous = _ensure_inference_candidates(_state(candidate))
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
        cleaned = _ensure_inference_candidates(cleaned)
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
    state = _ensure_inference_candidates(_state(candidate))
    directions = [row for row in state.get("directions") or [] if isinstance(row, dict)]
    by_id = {str(row.get("direction_id")): row for row in directions}
    selected_direction_ids = list(payload.selected_direction_ids or state.get("selected_direction_ids") or [])
    unknown = [value for value in selected_direction_ids if value not in by_id]
    if unknown:
        raise ValueError(f"研究方向不存在或已过期：{', '.join(unknown)}")
    selected = set(selected_direction_ids)
    for row in directions:
        row["user_selected"] = str(row.get("direction_id")) in selected
        row["user_priority"] = "PRIORITY" if row["user_selected"] else "PAUSED"
    dimensions = sorted({
        str(dimension) for row in directions if row.get("user_selected")
        for dimension in row.get("suggested_dimensions") or []
        if str(dimension) in {"NEWS", "NOTICE", "FINANCIAL", "PRICE_VOLUME", "FUND_FLOW", "COMPANY", "RESEARCH", "GRAPH"}
    })
    if payload.action in {"CONFIRM_AND_RESEARCH", "REQUEST_MORE_EVIDENCE"}:
        status = "READY_FOR_EVIDENCE_COLLECTION"
    elif payload.action == "SAVE_DRAFT":
        status = "DIRECTION_DRAFT_SAVED"
    elif payload.action == "ACCEPT_RESULT":
        status = "COMPLETED"
    else:
        status = "STOPPED_BY_USER"
    if payload.action == "REQUEST_MORE_EVIDENCE" and not selected_direction_ids:
        raise ValueError("没有可继续验证的研究方向")
    state.update({
        "status": status,
        "review_phase": (
            "EVIDENCE_COLLECTION" if payload.action in {"CONFIRM_AND_RESEARCH", "REQUEST_MORE_EVIDENCE"}
            else "RESULT" if payload.action == "ACCEPT_RESULT" else "DIRECTION"
        ),
        "directions": directions,
        "selected_direction_ids": selected_direction_ids,
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
             "selected_direction_ids": selected_direction_ids, "reviewer": payload.reviewer},
        ][-10:],
    })
    feedback = SelectionResearchFeedback(
        run_id=candidate.run_id, candidate_id=candidate.id, phase="AI_DIRECTED_DIRECTION",
        action=payload.action, payload_json={"selected_direction_ids": selected_direction_ids, "comment": payload.comment},
        outcome_status="CONFIRMED" if payload.action == "ACCEPT_RESULT" else "PENDING",
        skill_code="AI_DIRECTED_RESEARCH_VERIFIER" if payload.action in {"ACCEPT_RESULT", "REQUEST_MORE_EVIDENCE"} else "AI_DIRECTED_RESEARCH_PLANNER",
        skill_version="AI_DIRECTED_RESEARCH_VERIFICATION_V1" if payload.action in {"ACCEPT_RESULT", "REQUEST_MORE_EVIDENCE"} else "AI_DIRECTED_RESEARCH_V1",
        reviewer=payload.reviewer,
    )
    db.add(feedback)
    analysis["ai_directed_research"] = state
    candidate.analysis_json = analysis
    db.flush()
    if payload.action in {"CONFIRM_AND_RESEARCH", "REQUEST_MORE_EVIDENCE"}:
        _queue_ai_directed_research(db, candidate, state)
    db.commit()
    if payload.action in {"CONFIRM_AND_RESEARCH", "REQUEST_MORE_EVIDENCE"}:
        from app.jobs.governance_runner import wake_governance_runner
        wake_governance_runner()
    db.refresh(candidate)
    return candidate


def ai_directed_research_read(candidate: SelectionCandidate, db: Session | None = None) -> dict[str, Any]:
    state = _state(candidate)
    if not state:
        return {"candidate_id": candidate.id, "status": "NOT_STARTED", "message": "尚未读取现有证据并提出研究方向"}
    # Older candidates were created before inference_candidates existed.  Make
    # the migration lazy and durable so reopening a historical candidate has
    # the same first-pass conclusions as a newly prepared one.
    enriched = _ensure_inference_candidates(state)
    if enriched.get("inference_candidates") != state.get("inference_candidates"):
        state = enriched
        if db is not None:
            analysis = deepcopy(candidate.analysis_json or {})
            analysis["ai_directed_research"] = state
            candidate.analysis_json = analysis
            db.commit()
    if db is not None and str(state.get("status") or "").upper() in {"QUEUED", "RUNNING"}:
        state = deepcopy(state)
        run = db.get(SelectionRun, candidate.run_id)
        backfill = ((run.criteria_json or {}).get("backfill_progress") or {}) if run else {}
        progress = dict(state.get("progress") or {})
        backfill_status = str(backfill.get("status") or "").upper()
        if backfill_status == "RUNNING":
            source_percent = max(0, min(100, int(backfill.get("percent") or 0)))
            progress.update({
                "status": "RUNNING", "percent": 10 + round(source_percent * 0.55),
                "message": str(backfill.get("message") or "正在调用受控数据源补证"),
                "current_operation": backfill.get("current_operation"),
            })
        elif backfill_status in {"COMPLETED", "PARTIAL"}:
            progress.update({"status": "RUNNING", "percent": 72,
                             "message": "数据补证已完成，正在执行证据增量比较和模型复核"})
        active_logs = list(db.scalars(select(ModelCallLog).where(
            ModelCallLog.task_type == "stock_screening",
            ModelCallLog.completed_at.is_(None),
        ).order_by(ModelCallLog.started_at.desc()).limit(20)).all())
        active = next((log for log in active_logs if str(
            (((log.request_json or {}).get("metadata_json") or {}).get("candidate_id")) or ""
        ) == str(candidate.id)), None)
        if active is not None:
            progress.update({"status": "RUNNING", "percent": 78,
                             "message": f"模型复核中：{active.instance_code or active.provider_code or '自动路由'}",
                             "model_call_log_id": active.id,
                             "model_instance_code": active.instance_code})
        state["progress"] = progress
    return state


def _queue_ai_directed_research(db: Session, candidate: SelectionCandidate, state: dict[str, Any]) -> None:
    """Queue one bounded evidence-collection and verification round.

    The queue is deliberately separate from the legacy eight-dimension run so
    an approved research direction cannot silently overwrite historical
    scorecards or facts. Collection itself is delegated to the existing
    controlled backfill service.
    """
    from app.models.pipeline import PipelineRun, PipelineStageRun, ScheduledJob
    # The JSON value was flushed immediately before this helper is called.
    # Never mutate that committed object in place, otherwise SQLAlchemy can
    # treat the queued state as unchanged and leave the UI at READY forever.
    state = deepcopy(state)

    run = db.get(SelectionRun, candidate.run_id)
    if run is None:
        raise LookupError("选股批次不存在")
    active = list(db.scalars(select(ScheduledJob).where(
        ScheduledJob.task_type == "selection_ai_directed_research",
        ScheduledJob.status.in_(("PENDING", "RETRY", "RUNNING")),
    )).all())
    if any(int((job.payload_json or {}).get("candidate_id", -1)) == candidate.id for job in active):
        raise ValueError("该股票已有AI主动研究任务正在执行")
    previous = list(db.scalars(select(ScheduledJob).where(
        ScheduledJob.task_type == "selection_ai_directed_research",
        ScheduledJob.idempotency_key.like(f"selection-ai-directed-research:{candidate.id}:%"),
    )).all())
    attempt = len(previous) + 1
    max_rounds = min(3, max(1, int(state.get("max_rounds") or 3)))
    payload = {
        "selection_run_id": candidate.run_id,
        "candidate_id": candidate.id,
        "selected_direction_ids": list(state.get("selected_direction_ids") or []),
        "max_rounds": max_rounds,
        "reuse_existing_data": bool(
            (state.get("verification_rounds") or [])
            and str((state.get("verification_rounds") or [])[-1].get("status") or "").upper()
            == "MODEL_FAILED"
        ),
        "attempt": attempt,
    }
    key = f"selection-ai-directed-research:{candidate.id}:{attempt}"
    pipeline = PipelineRun(
        pipeline_type="selection_ai_directed_research", trigger_type="API", status="PENDING",
        idempotency_key=key, input_json=payload, current_stage="selection_ai_directed_research",
    )
    db.add(pipeline)
    db.flush()
    db.add(PipelineStageRun(
        pipeline_run_id=pipeline.id, stage_code="selection_ai_directed_research", status="PENDING",
        max_attempts=3, input_json=payload, skill_code="AI_DIRECTED_RESEARCH_VERIFIER",
        skill_version="AI_DIRECTED_RESEARCH_VERIFICATION_V1",
    ))
    db.add(ScheduledJob(
        task_type="selection_ai_directed_research", status="PENDING", idempotency_key=key,
        pipeline_run_id=pipeline.id, payload_json=payload, max_attempts=3,
    ))
    state.update({
        "status": "QUEUED", "review_phase": "EVIDENCE_COLLECTION",
        "queued_at": _now(), "queue_attempt": attempt, "max_rounds": max_rounds,
        "progress": {"status": "QUEUED", "percent": 0, "message": "已排队：准备按用户确认方向补证并复核"},
    })
    analysis = deepcopy(candidate.analysis_json or {})
    analysis["ai_directed_research"] = state
    candidate.analysis_json = analysis
    criteria = dict(run.criteria_json or {})
    criteria["ai_directed_research_progress"] = {
        "status": "QUEUED", "candidate_id": candidate.id, "symbol": candidate.symbol,
        "attempt": attempt, "percent": 0, "message": "已排队：准备按用户确认方向补证并复核",
        "updated_at": _now(),
    }
    run.criteria_json = criteria


def _verification_prompt(candidate: SelectionCandidate, state: dict[str, Any], pack: dict[str, Any], round_no: int) -> str:
    selected = {str(value) for value in state.get("selected_direction_ids") or []}
    directions = [row for row in state.get("directions") or [] if str(row.get("direction_id")) in selected]
    context = {
        "candidate_id": candidate.id, "market": candidate.market, "symbol": candidate.symbol,
        "name": candidate.name, "round": round_no, "directions": directions,
        "evidence": pack.get("evidence_json") or {},
    }
    return (
        "你是AI主动研究复核Skill。用户已经确认了研究方向。请只基于输入证据，比较补证前后的支持证据、反证和缺口，"
        "逐个方向给出当前判断；不得把推测写成事实，不得生成买卖指令，不得引用白名单以外的ID。"
        "如果证据不足，必须使用INSUFFICIENT并降低confidence；新闻标题本身不能作为公司因果证据。"
        "严格只返回调用方提供的JSON Schema对象，不要Markdown或解释。\n\n"
        + json.dumps(context, ensure_ascii=False, separators=(",", ":"))
    )


def _adapt_verification_format(payload: dict[str, Any], state: dict[str, Any]) -> tuple[dict[str, Any], bool]:
    """Fail closed when a provider repeats the planning shape at verification.

    The prose is useful as a stage note, but it is not proof that collection
    changed the hypothesis. Preserve it with an INSUFFICIENT judgment and a
    capped confidence instead of manufacturing a successful verification.
    """
    if isinstance(payload.get("direction_results"), list):
        return payload, False
    rows = payload.get("directions")
    if not isinstance(rows, list):
        return payload, False
    selected = {str(value) for value in state.get("selected_direction_ids") or []}
    results: list[dict[str, Any]] = []
    for row in rows:
        if not isinstance(row, dict):
            continue
        direction_id = str(row.get("direction_id") or "")
        if direction_id not in selected:
            continue
        conclusion = _text(row.get("conclusion") or row.get("current_judgment"), 1200)
        results.append({
            "direction_id": direction_id,
            "judgment": "INSUFFICIENT",
            "conclusion": conclusion or "模型未形成符合复核契约的方向结论。",
            "confidence": min(0.25, float(row.get("confidence") or 0)),
            "supporting_evidence": list(row.get("supporting_evidence") or [])[:8],
            "counter_evidence": list(row.get("counter_evidence") or [])[:8],
            "remaining_gaps": list(dict.fromkeys([
                *[_text(value, 400) for value in row.get("data_gaps") or []],
                "模型返回的是方向规划结构，尚未证明补证后结论发生变化。",
            ]))[:8],
            "next_actions": [_text(value, 300) for value in row.get("suggested_sources") or []][:8],
        })
    if not results:
        return payload, False
    return {
        "mode": "AI_DIRECTED_RESEARCH_VERIFICATION_V1",
        "overall_summary": "；".join(row["conclusion"] for row in results)[:1600],
        "overall_status": "INSUFFICIENT",
        "direction_results": results,
        "risk_notes": ["本轮模型未遵循复核契约，不能据此形成投资结论。"],
        "data_quality_notes": ["已兼容保留模型返回的方向文本，但所有方向按证据不足处理。"],
    }, True


def _verify_ai_directed_research(
    db: Session, candidate: SelectionCandidate, state: dict[str, Any], round_no: int,
) -> tuple[dict[str, Any], ModelCallLog]:
    pack = _evidence_pack(candidate)
    service = ModelHubService(db)
    run = db.get(SelectionRun, candidate.run_id)
    log = service.chat(
        task_type="stock_screening",
        messages=[{"role": "user", "content": _verification_prompt(candidate, state, pack, round_no)}],
        instance_code=state.get("model_instance_code") or (run.model_instance_code if run else None),
        temperature=0.1, max_tokens=4800,
        metadata_json={
            "skill_codes": ["AI_DIRECTED_RESEARCH_VERIFIER", "COUNTER_EVIDENCE_REVIEWER", "CONCLUSION_QUALITY_GATE"],
            "require_skills": True, "require_real_model": True, "bounded_schema_repair": True,
            "defer_schema_validation": True, "strict_instance": True, "transport_max_attempts": 1,
            "candidate_id": candidate.id, "research_round": round_no,
            "json_schema_output": SelectionAiDirectedResearchVerificationOutput.model_json_schema(),
            "evidence_ids": list((pack.get("evidence_json") or {}).get("citation_allowlist", {}).get("document_ids") or []),
        },
    )
    payload = _json_object(log.response_text)
    payload, contract_repaired = _adapt_verification_format(payload, state)
    validated = SelectionAiDirectedResearchVerificationOutput.model_validate(payload)
    refs_payload = {
        "key_findings": [], "major_events": [],
        "directions": [row.model_dump(mode="json") for row in validated.direction_results],
        "data_quality_notes": [*validated.data_quality_notes],
    }
    cleaned, warnings = _validate_references(refs_payload, pack)
    results = []
    for row in cleaned.get("directions") or []:
        item = dict(row)
        if warnings:
            item["confidence"] = min(float(item.get("confidence") or 0), 0.35)
        results.append(item)
    output = {
        "mode": "AI_DIRECTED_RESEARCH_VERIFICATION_V1",
        "overall_summary": validated.overall_summary,
        "overall_status": validated.overall_status,
        "direction_results": results,
        "risk_notes": list(validated.risk_notes),
        "data_quality_notes": list(dict.fromkeys([*validated.data_quality_notes, *warnings]))[:10],
        "model_call_log_id": log.id, "model_instance_code": log.instance_code,
        "generated_at": _now(), "contract_repaired": contract_repaired,
    }
    return output, log


def execute_ai_directed_research(payload: dict[str, Any]) -> dict[str, Any]:
    """Run one user-approved collection/review round and stop at result review."""
    from app.db.session import SessionLocal

    candidate_id = int(payload["candidate_id"])
    run_id = int(payload["selection_run_id"])
    max_rounds = min(3, max(1, int(payload.get("max_rounds") or 3)))
    with SessionLocal() as db:
        candidate = db.get(SelectionCandidate, candidate_id)
        run = db.get(SelectionRun, run_id)
        if candidate is None or run is None or candidate.run_id != run_id:
            raise LookupError("AI主动研究任务引用的候选不存在")
        state = _ensure_inference_candidates(_state(candidate))
        selected = set(str(value) for value in state.get("selected_direction_ids") or payload.get("selected_direction_ids") or [])
        directions = [row for row in state.get("directions") or [] if str(row.get("direction_id")) in selected]
        if not directions:
            raise ValueError("没有用户确认的研究方向，不能执行补证")
        rounds = [row for row in state.get("verification_rounds") or [] if isinstance(row, dict)]
        round_no = len(rounds) + 1
        before = _research_evidence_state(candidate)
        dimensions = sorted({
            str(dimension) for row in directions for dimension in row.get("suggested_dimensions") or []
            if str(dimension) in {"NEWS", "NOTICE", "FINANCIAL", "PRICE_VOLUME", "FUND_FLOW", "COMPANY", "RESEARCH", "GRAPH"}
        })
        state.update({"status": "RUNNING", "review_phase": "EVIDENCE_COLLECTION", "round": round_no,
                      "max_rounds": max_rounds, "progress": {"status": "RUNNING", "percent": 10,
                      "message": "正在按用户确认方向补齐受控数据"}, "updated_at": _now()})
        analysis = deepcopy(candidate.analysis_json or {})
        analysis["ai_directed_research"] = state
        candidate.analysis_json = analysis
        criteria = dict(run.criteria_json or {})
        criteria["ai_directed_research_progress"] = {"status": "RUNNING", "candidate_id": candidate.id,
            "symbol": candidate.symbol, "round": round_no, "percent": 10,
            "message": "正在按用户确认方向补齐受控数据", "updated_at": _now()}
        run.criteria_json = criteria
        period_months = int((run.criteria_json or {}).get("period_months") or 12)
        db.commit()

    if payload.get("reuse_existing_data"):
        backfill = {"selection_run_id": run_id, "status": "REUSED", "results": [],
                    "message": "上一轮数据补证已完成，本轮仅重试模型复核"}
    else:
        backfill = execute_selection_data_backfill({
            "selection_run_id": run_id, "candidate_ids": [candidate_id],
            "dimensions_by_candidate": {str(candidate_id): dimensions},
            "period_months": period_months,
            "force_refresh": True, "rerun_analysis": False,
            "attempt": f"ai-directed-{candidate_id}-{round_no}", "research_round": round_no,
        })

    with SessionLocal() as db:
        candidate = db.get(SelectionCandidate, candidate_id)
        run = db.get(SelectionRun, run_id)
        if candidate is None or run is None:
            raise LookupError("补证完成后候选不存在")
        state = _ensure_inference_candidates(_state(candidate))
        after = _research_evidence_state(candidate)
        added = {
            "documents": sorted(set(after["document_ids"]) - set(before["document_ids"])),
            "facts": sorted(set(after["fact_ids"]) - set(before["fact_ids"])),
            "relations": sorted(set(after["relation_ids"]) - set(before["relation_ids"])),
            "structured_count_delta": after["structured_count"] - before["structured_count"],
        }
        source_failed = bool(backfill.get("status") == "FAILED")
        try:
            if source_failed:
                raise RuntimeError("受控数据补证失败，未进入模型复核")
            output, log = _verify_ai_directed_research(db, candidate, state, round_no)
        except Exception as exc:
            history = [*list(state.get("verification_rounds") or []), {
                "round": round_no, "status": "SOURCE_FAILED" if source_failed else "MODEL_FAILED",
                "added_evidence": added, "backfill": backfill, "error": str(exc)[:1200], "at": _now(),
            }]
            state.update({"status": "SOURCE_FAILED" if source_failed else "MODEL_FAILED",
                          "review_phase": "EVIDENCE_COLLECTION", "verification_rounds": history,
                          "last_error": str(exc)[:1200], "updated_at": _now(),
                          "progress": {"status": "FAILED", "percent": 100,
                                       "message": "补证或模型复核失败，上一轮结果保持不变"}})
            analysis = deepcopy(candidate.analysis_json or {})
            analysis["ai_directed_research"] = state
            candidate.analysis_json = analysis
            run.criteria_json = {**(run.criteria_json or {}), "ai_directed_research_progress": {
                "status": state["status"], "candidate_id": candidate.id, "symbol": candidate.symbol,
                "round": round_no, "percent": 100, "message": state["progress"]["message"],
                "error": state["last_error"], "updated_at": _now(),
            }}
            db.commit()
            return {"candidate_id": candidate.id, "status": state["status"], "round": round_no}
        history = [*list(state.get("verification_rounds") or []), {
            "round": round_no, "status": "WAITING_CONCLUSION_REVIEW", "added_evidence": added,
            "backfill": backfill, "verification": output, "at": _now(),
        }]
        state.update({"status": "WAITING_CONCLUSION_REVIEW", "review_phase": "RESULT",
                      "verification_rounds": history, "latest_verification": output,
                      "last_error": None, "updated_at": _now(),
                      "progress": {"status": "WAITING_CONCLUSION_REVIEW", "percent": 100,
                                   "message": "补证和AI复核已完成，等待用户确认阶段结论"}})
        analysis = deepcopy(candidate.analysis_json or {})
        analysis["ai_directed_research"] = state
        candidate.analysis_json = analysis
        run.criteria_json = {**(run.criteria_json or {}), "ai_directed_research_progress": {
            "status": "WAITING_CONCLUSION_REVIEW", "candidate_id": candidate.id, "symbol": candidate.symbol,
            "round": round_no, "percent": 100, "message": "补证和AI复核已完成，等待用户确认阶段结论",
            "updated_at": _now(),
        }}
        db.commit()
        return {"candidate_id": candidate.id, "status": state["status"], "round": round_no,
                "model_call_log_id": output.get("model_call_log_id")}
