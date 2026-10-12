"""Human-in-the-loop review for versioned document semantic candidates.

The service deliberately keeps semantic candidates in FoundationFact and stores
human decisions as an append-only audit trail.  A confirmation does not silently
turn a media interpretation into an externally verified fact; it records that a
reviewer confirmed the current extraction.  Rejections use the existing
Foundation fact review service so graph/RAG consumers can exclude them.
"""
from __future__ import annotations

from copy import deepcopy
import re
from typing import Any
from uuid import NAMESPACE_URL, uuid4, uuid5

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.foundation import FoundationFact
from app.models.governance import DocumentSemanticReview
from app.schemas.document_governance import DocumentReviewRequest
from app.schemas.foundation import FactCreate, FactReviewCreate
from app.services.document_event_governance import _original_quote
from app.services.foundation import create_fact, fact_evidence, review_fact


REVIEW_ACTIONS = {"COMMENT", "CONFIRM", "REJECT", "REQUEST_CORRECTION", "MODIFY", "REANALYZE"}
STATUS_BY_ACTION = {
    "COMMENT": "PENDING",
    "CONFIRM": "HUMAN_CONFIRMED",
    "REJECT": "REJECTED",
    "REQUEST_CORRECTION": "NEEDS_CORRECTION",
    "MODIFY": "CORRECTED_CANDIDATE",
    "REANALYZE": "REANALYSIS_REQUESTED",
}


def version_key(state: dict[str, Any], index: int) -> str:
    return ":".join(str(state.get(key) or "") for key in ("content_hash", "skill_version", "semantic_schema_version")) + f":{index}:{state.get('observed_at') or ''}"


def versions_for(row) -> list[dict[str, Any]]:
    payload = row.content_json or {}
    return [*(payload.get("event_governance_history") or []), *(([payload["event_governance"]]) if payload.get("event_governance") else [])]


def _version_summary(state: dict[str, Any], index: int) -> dict[str, Any]:
    """Return only stable, non-content metadata for a version comparison."""
    return {
        "index": index,
        "version_key": version_key(state, index),
        "skill_version": state.get("skill_version"),
        "semantic_schema_version": state.get("semantic_schema_version"),
        "observed_at": state.get("observed_at"),
        "status": state.get("status"),
        "human_review_status": state.get("human_review_status"),
        "fact_count": len(state.get("fact_ids") or []),
    }


def _item_key(item: dict[str, Any], fallback: str) -> str:
    return str(item.get("key") or item.get("fact_id") or fallback)


def _relation_key(item: dict[str, Any], fallback: str) -> str:
    return ":".join(str(item.get(key) or "") for key in ("source_key", "predicate", "target_key")) or fallback


def _field_changes(old: dict[str, Any], new: dict[str, Any], ignored: set[str] | None = None) -> list[dict[str, Any]]:
    ignored = ignored or set()
    changes = []
    for field in sorted(set(old) | set(new)):
        if field in ignored or field.startswith("_") or old.get(field) == new.get(field):
            continue
        changes.append({"field": field, "from": old.get(field), "to": new.get(field)})
    return changes


def _public_item(item: dict[str, Any]) -> dict[str, Any]:
    return {key: value for key, value in item.items() if not key.startswith("_")}


def _collection_diff(old_items: list[dict[str, Any]], new_items: list[dict[str, Any]], key_fn) -> dict[str, Any]:
    old_map = {key_fn(item, str(index)): item for index, item in enumerate(old_items)}
    new_map = {key_fn(item, str(index)): item for index, item in enumerate(new_items)}
    added = [{"key": key, "value": _public_item(new_map[key])} for key in sorted(set(new_map) - set(old_map))]
    removed = [{"key": key, "value": _public_item(old_map[key])} for key in sorted(set(old_map) - set(new_map))]
    modified = []
    for key in sorted(set(old_map) & set(new_map)):
        changes = _field_changes(old_map[key], new_map[key], {"fact_id"})
        if changes:
            modified.append({"key": key, "changes": changes})
    return {"added": added, "removed": removed, "modified": modified,
            "counts": {"added": len(added), "removed": len(removed), "modified": len(modified)}}


def compare_versions(row, from_index: int | None = None, to_index: int | None = None) -> dict[str, Any]:
    """Build an auditable, field-level diff without changing either version.

    Semantic versions are append-only JSON snapshots on the source document.  The
    comparison intentionally ignores generated fact IDs and model-call IDs: those
    are lineage changes, not semantic changes.  The returned values are suitable
    for both the API and a human review screen.
    """
    versions = versions_for(row)
    if not versions:
        raise ValueError("文档尚无可比较的语义治理版本")
    if from_index is None or to_index is None:
        to_index = len(versions) - 1 if to_index is None else to_index
        from_index = max(0, to_index - 1) if from_index is None else from_index
    if from_index < 0 or to_index < 0 or from_index >= len(versions) or to_index >= len(versions):
        raise ValueError("治理版本不存在")
    before, after = versions[from_index], versions[to_index]
    old_analysis = before.get("semantic_analysis") or {}
    new_analysis = after.get("semantic_analysis") or {}
    scalar_fields = ("overview",)
    reading_changes = []
    for field in scalar_fields:
        old_value = (old_analysis.get("reading") or {}).get(field)
        new_value = (new_analysis.get("reading") or {}).get(field)
        if old_value != new_value:
            reading_changes.append({"field": field, "from": old_value, "to": new_value})
    sections = _collection_diff((old_analysis.get("reading") or {}).get("sections") or [],
                                (new_analysis.get("reading") or {}).get("sections") or [],
                                lambda item, fallback: str(item.get("topic") or fallback))
    events = _collection_diff(old_analysis.get("events") or [], new_analysis.get("events") or [],
                              lambda item, fallback: _item_key(item, fallback))
    participants = _collection_diff(old_analysis.get("participants") or [], new_analysis.get("participants") or [],
                                    lambda item, fallback: _item_key(item, fallback))
    relations = _collection_diff(old_analysis.get("relations") or [], new_analysis.get("relations") or [],
                                 lambda item, fallback: _relation_key(item, fallback))
    old_metrics = [{"_event_key": event.get("key"), **metric} for event in old_analysis.get("events") or [] for metric in event.get("metrics") or []]
    new_metrics = [{"_event_key": event.get("key"), **metric} for event in new_analysis.get("events") or [] for metric in event.get("metrics") or []]
    old_impacts = [{"_event_key": event.get("key"), **impact} for event in old_analysis.get("events") or [] for impact in event.get("impacts") or []]
    new_impacts = [{"_event_key": event.get("key"), **impact} for event in new_analysis.get("events") or [] for impact in event.get("impacts") or []]
    metrics = _collection_diff(old_metrics, new_metrics, lambda item, fallback: f"{item.get('_event_key') or fallback}:{item.get('name') or ''}:{item.get('period') or ''}")
    impacts = _collection_diff(old_impacts, new_impacts, lambda item, fallback: f"{item.get('_event_key') or fallback}:{item.get('dimension') or ''}:{item.get('evidence_quote') or ''}")
    fact_ids_before = set(before.get("fact_ids") or [])
    fact_ids_after = set(after.get("fact_ids") or [])
    summary = {
        "sections": sections["counts"], "events": events["counts"], "participants": participants["counts"],
        "relations": relations["counts"], "metrics": metrics["counts"], "impacts": impacts["counts"],
        "facts": {"added": len(fact_ids_after - fact_ids_before), "removed": len(fact_ids_before - fact_ids_after)},
        "reading_fields_changed": len(reading_changes),
    }
    changed = bool(reading_changes)
    for collection in (sections, events, participants, relations, metrics, impacts):
        changed = changed or any(collection["counts"].values())
    changed = changed or bool(fact_ids_after ^ fact_ids_before)
    return {
        "from": _version_summary(before, from_index), "to": _version_summary(after, to_index),
        "changed": changed, "summary": summary,
        "reading": {"fields": reading_changes, "sections": sections},
        "events": events, "participants": participants, "relations": relations,
        "metrics": metrics, "impacts": impacts,
    }


def require_version(row, index: int) -> tuple[dict[str, Any], str, bool]:
    versions = versions_for(row)
    if index < 0 or index >= len(versions):
        raise ValueError("治理版本不存在")
    state = versions[index]
    return state, version_key(state, index), index == len(versions) - 1


def _review_rows(db: Session, table: str, record_id: int, version_key_value: str | None = None) -> list[DocumentSemanticReview]:
    query = select(DocumentSemanticReview).where(
        DocumentSemanticReview.source_table == table,
        DocumentSemanticReview.source_record_id == record_id,
    )
    if version_key_value:
        query = query.where(DocumentSemanticReview.version_key == version_key_value)
    return list(db.scalars(query.order_by(DocumentSemanticReview.created_at, DocumentSemanticReview.id)).all())


def _target_identity(payload: DocumentReviewRequest) -> str:
    return ":".join(str(value or "") for value in (payload.target_type, payload.target_key, payload.event_key, payload.item_index, payload.field))


def review_status(db: Session, table: str, record_id: int, version_key_value: str | None = None) -> dict[str, Any]:
    rows = _review_rows(db, table, record_id, version_key_value)
    latest: dict[str, DocumentSemanticReview] = {}
    for row in rows:
        key = ":".join(str(value or "") for value in (row.target_type, row.target_key, row.event_key, row.item_index, row.field_name))
        # A comment is an audit annotation, not a state transition.  It must
        # not silently clear an earlier correction/rejection/confirmation.
        if row.action != "COMMENT" or key not in latest:
            latest[key] = row
    document_row = latest.get("DOCUMENT::::")
    statuses = [row.resulting_status for row in latest.values()]
    if "NEEDS_CORRECTION" in statuses:
        overall = "NEEDS_CORRECTION"
    elif "REJECTED" in statuses:
        overall = "REJECTED"
    elif document_row and document_row.resulting_status == "HUMAN_CONFIRMED":
        overall = "HUMAN_CONFIRMED"
    elif statuses:
        overall = "IN_REVIEW"
    else:
        overall = "NOT_REVIEWED"
    return {
        "overall": overall,
        "review_count": len(rows),
        "latest": {
            key: {"action": row.action, "status": row.resulting_status, "reviewer": row.reviewer,
                  "reason": row.reason, "created_at": row.created_at.isoformat()}
            for key, row in latest.items()
        },
        "history": [
            {"id": row.id, "version_index": row.version_index, "version_key": row.version_key,
             "review_round": row.review_round, "action": row.action, "target_type": row.target_type,
             "target_key": row.target_key, "event_key": row.event_key, "item_index": row.item_index,
             "field": row.field_name, "old_value": row.old_value_json, "new_value": row.new_value_json,
             "reason": row.reason, "reviewer": row.reviewer, "resulting_status": row.resulting_status,
             "fact_ids": row.fact_ids_json, "corrected_fact_ids": row.corrected_fact_ids_json,
             "created_at": row.created_at.isoformat()}
            for row in rows
        ],
    }


def _find_target(analysis: dict[str, Any], payload: DocumentReviewRequest) -> tuple[dict[str, Any] | None, Any]:
    target = payload.target_type
    if target == "DOCUMENT":
        return analysis, analysis
    if target == "EVENT":
        event = next((item for item in analysis.get("events", []) if item.get("key") == payload.target_key), None)
        return event, event
    if target == "PARTICIPANT":
        item = next((value for value in analysis.get("participants", []) if value.get("key") == payload.target_key), None)
        return item, item
    if target == "RELATION":
        item = next((value for value in analysis.get("relations", []) if value.get("fact_id") == payload.target_key or value.get("source_key") + "->" + value.get("target_key") == payload.target_key), None)
        return item, item
    event = next((item for item in analysis.get("events", []) if item.get("key") == payload.event_key), None)
    if not event:
        return None, None
    collection = event.get("metrics", []) if target == "METRIC" else event.get("impacts", [])
    item = collection[payload.item_index] if payload.item_index is not None and payload.item_index < len(collection) else None
    return item, item


def _set_field(target: dict[str, Any], field: str, value: Any, body: str) -> tuple[Any, Any]:
    allowed = {
        "event_type", "summary", "assertion_type", "attribution", "event_time_raw", "event_date", "direction",
        "name", "role", "aliases", "raw_value", "unit", "qualifier", "period", "dimension", "rationale", "uncertainty",
        "predicate", "explanation", "evidence_quote",
    }
    if field not in allowed:
        raise ValueError("该字段不允许人工直接修改")
    if field == "evidence_quote":
        if not isinstance(value, str):
            raise ValueError("证据引文必须是文本")
        value = _original_quote(value, body)
    if field in {"summary", "rationale", "uncertainty", "explanation", "attribution", "event_type", "name", "role", "period"}:
        if not isinstance(value, str) or not value.strip() or len(value) > 1600:
            raise ValueError("文本字段不能为空或超过长度限制")
    if field == "direction" and value not in {"POSITIVE", "NEGATIVE", "NEUTRAL", "MIXED", "UNKNOWN"}:
        raise ValueError("影响方向不合法")
    if field == "assertion_type" and value not in {"REPORTED_FACT", "AUTHOR_OPINION", "MODEL_INFERENCE"}:
        raise ValueError("事实性质不合法")
    if field == "unit" and value not in {"元", "万元", "亿元", "%", "倍", "股", "万股", "亿股"}:
        raise ValueError("指标单位不合法")
    if field == "predicate" and value not in {"INVOLVES", "CAUSES", "CONTRIBUTES_TO", "PRECEDES", "FINANCES", "SUBSCRIBES", "RENAMED_TO", "CONTROLS", "RELATED_PARTY", "EXPLAINS", "DISPUTES"}:
        raise ValueError("关系类型不合法")
    if field == "qualifier" and value not in {"EXACT", "APPROXIMATE", "UPPER_BOUND", "LOWER_BOUND", "ESTIMATE"}:
        raise ValueError("指标性质不合法")
    if field == "raw_value" and (not isinstance(value, str) or not value.strip()):
        raise ValueError("指标原始值必须是原文数字文本")
    if field == "name" and re.sub(r"\s+", "", str(value)) not in re.sub(r"\s+", "", body):
        raise ValueError("主体或指标名称必须能够在原文中核对")
    if field == "aliases":
        if not isinstance(value, list) or any(not isinstance(item, str) or re.sub(r"\s+", "", item) not in re.sub(r"\s+", "", body) for item in value):
            raise ValueError("别名必须逐项出现在原文中")
    old = target.get(field)
    target[field] = value
    return old, value


def _target_fact_ids(analysis: dict[str, Any], payload: DocumentReviewRequest) -> list[str]:
    if payload.target_type == "DOCUMENT":
        return [item.get("fact_id") for item in [*analysis.get("events", []), *analysis.get("relations", [])] if item.get("fact_id")]
    if payload.target_type in {"EVENT", "METRIC", "IMPACT"}:
        key = payload.target_key if payload.target_type == "EVENT" else payload.event_key
        event = next((item for item in analysis.get("events", []) if item.get("key") == key), None)
        return [event["fact_id"]] if event and event.get("fact_id") else []
    if payload.target_type == "RELATION":
        link = next((item for item in analysis.get("relations", []) if item.get("fact_id") == payload.target_key or item.get("source_key") + "->" + item.get("target_key") == payload.target_key), None)
        return [link["fact_id"]] if link and link.get("fact_id") else []
    if payload.target_type == "PARTICIPANT":
        return [item["fact_id"] for item in analysis.get("events", [])
                if payload.target_key in item.get("participant_keys", []) and item.get("fact_id")]
    return []


def _clone_corrected_facts(db: Session, impacted_ids: list[str], analysis: dict[str, Any], review_id: str, old_to_new: dict[str, str]) -> list[str]:
    new_ids: list[str] = []
    for old_id in impacted_ids:
        old = db.get(FoundationFact, old_id)
        if not old:
            continue
        new_id = str(uuid5(NAMESPACE_URL, f"document-review:{review_id}:{old_id}"))
        if not db.get(FoundationFact, new_id):
            props = deepcopy(old.properties_json or {})
            source = next((value for value in [*analysis.get("events", []), *analysis.get("relations", [])]
                           if value.get("fact_id") == old_id), None)
            if source:
                props.update(deepcopy(source))
            props["fact_id"] = new_id
            props.update({"human_review_status": "CORRECTED_CANDIDATE", "review_parent_fact_id": old_id, "review_id": review_id, "verification_status": "CANDIDATE", "extraction_method": "HUMAN"})
            if source and source.get("event_type"):
                title = source["event_type"] + " · " + old.title.split(" · ", 1)[-1]
            elif source and source.get("explanation"):
                title = source["explanation"][:500]
            else:
                title = old.title
            create_fact(db, FactCreate(
                fact_type=old.fact_type, title=title, subject_entity_id=old.subject_entity_id,
                object_entity_id=old.object_entity_id, properties_json=props,
                evidence_ids=[item.id for item in fact_evidence(db, old_id)],
                valid_from=old.valid_from, valid_to=old.valid_to,
            ), fact_id=new_id)
        new_ids.append(new_id); old_to_new[old_id] = new_id
        if old.status in {"PENDING", "ACCEPTED"}:
            review_fact(db, old_id, FactReviewCreate(expected_status=old.status, decision="REJECTED", reviewer="HUMAN_REVIEW",
                reason=f"人工修正生成新候选版本，原事实保留并退出当前版本；review_id={review_id}"))
    return new_ids


def apply_review(db: Session, row, payload: DocumentReviewRequest, body: str, *, commit: bool = True) -> dict[str, Any]:
    if payload.action not in REVIEW_ACTIONS:
        raise ValueError("不支持的审核动作")
    state, version_key_value, is_latest = require_version(row, payload.version_index)
    if state.get("publication_status") == "PUBLISHED" and payload.action != "COMMENT":
        raise ValueError("已发布成果不可原位修改，请重新制定治理方案并保留历史版本")
    analysis = deepcopy(state.get("semantic_analysis") or {})
    if not analysis and payload.action not in {"COMMENT", "REANALYZE"}:
        raise ValueError("当前版本没有可审核的语义成果")
    target, _ = _find_target(analysis, payload)
    if payload.action in {"MODIFY", "CONFIRM", "REJECT", "REQUEST_CORRECTION"} and target is None:
        raise ValueError("审核目标不存在")
    if payload.action == "MODIFY" and not is_latest:
        raise ValueError("历史版本不可直接修改，请先以当前版本重新分析")
    if payload.action == "REANALYZE" and not is_latest:
        raise ValueError("历史版本不可发起定向重分析，请在当前版本提交反馈")
    if payload.action == "CONFIRM" and payload.target_type == "DOCUMENT":
        unresolved = {item.get("status") for item in review_status(db, row.__tablename__, row.id, version_key_value)["latest"].values()}
        if unresolved & {"NEEDS_CORRECTION", "REJECTED"}:
            raise ValueError("当前版本仍有需要修正或已驳回的对象，不能确认整篇结构")
    review_id = str(uuid4())
    old_value = target.get(payload.field) if target and payload.field else None
    new_value = None
    corrected_ids: list[str] = []
    fact_ids = list(state.get("fact_ids") or [])
    if payload.action == "MODIFY":
        if payload.target_type == "DOCUMENT":
            raise ValueError("请在具体事件、指标、影响、主体或关系上修改字段")
        if not payload.field:
            raise ValueError("人工修改必须指定字段")
        if payload.target_type == "EVENT" and payload.field == "evidence_quote":
            if not isinstance(payload.value, str):
                raise ValueError("证据引文必须是文本")
            new_value = _original_quote(payload.value, body)
            quotes = list(target.get("evidence_quotes") or [])
            old_value = quotes[0] if quotes else None
            if quotes:
                quotes[0] = new_value
            else:
                quotes.append(new_value)
            target["evidence_quotes"] = quotes
        else:
            _, new_value = _set_field(target, payload.field, payload.value, body)
        if payload.target_type == "METRIC":
            from app.services.document_semantics import normalize_metric
            target.update(normalize_metric({key: target[key] for key in ("name", "raw_value", "unit", "qualifier", "period", "evidence_quote")}, body))
        new_state = deepcopy(state)
        new_state["semantic_analysis"] = analysis
        new_state["status"] = "ANALYZED"
        new_state["semantic_schema_version"] = state.get("semantic_schema_version") or "SEMANTIC_V2"
        new_state["human_review_status"] = "CORRECTED_CANDIDATE"
        new_state["review_parent_version"] = payload.version_index
        new_state["review_round"] = max([int(item.get("review_round") or 0) for item in review_status(db, row.__tablename__, row.id, version_key_value)["history"]] + [0]) + 1
        old_to_new: dict[str, str] = {}
        impacted_ids = _target_fact_ids(analysis, payload)
        if not impacted_ids:
            raise ValueError("审核目标没有关联候选事实")
        corrected_ids = _clone_corrected_facts(db, impacted_ids, analysis, review_id, old_to_new)
        new_state["fact_ids"] = [old_to_new.get(item, item) for item in state.get("fact_ids") or []]
        new_state["event_fact_ids"] = [old_to_new.get(item, item) for item in state.get("event_fact_ids") or []]
        new_state["relation_fact_ids"] = [old_to_new.get(item, item) for item in state.get("relation_fact_ids") or []]
        for item in [*analysis.get("events", []), *analysis.get("relations", [])]:
            if item.get("fact_id") in old_to_new:
                item["fact_id"] = old_to_new[item["fact_id"]]
        payload_json = dict(row.content_json or {})
        current = payload_json.get("event_governance") or {}
        history = list(payload_json.get("event_governance_history") or [])
        if current:
            history.append(current)
        payload_json["event_governance_history"] = history
        payload_json["event_governance"] = new_state
        row.content_json = payload_json
        payload.version_index = len(history)
        version_key_value = version_key(new_state, payload.version_index)
    elif payload.action == "REJECT":
        selected_ids = _target_fact_ids(analysis, payload)
        for fact_id in selected_ids:
            fact = db.get(FoundationFact, fact_id)
            if fact and fact.status == "PENDING":
                review_fact(db, fact_id, FactReviewCreate(expected_status="PENDING", decision="REJECTED", reviewer=payload.reviewer, reason=payload.reason))
    # Reanalysis is explicitly user-driven and versioned.  It may repeat until
    # all knowledge points conform or the user chooses not to govern; every
    # round remains append-only in this review table and source version history.
    review = DocumentSemanticReview(
        id=review_id, source_table=row.__tablename__, source_record_id=row.id, version_index=payload.version_index,
        version_key=version_key_value, review_round=max([int(item.get("review_round") or 0) for item in review_status(db, row.__tablename__, row.id, version_key_value)["history"]] + [0]) + 1,
        action=payload.action, target_type=payload.target_type, target_key=payload.target_key,
        event_key=payload.event_key, item_index=payload.item_index, field_name=payload.field,
        old_value_json=old_value, new_value_json=new_value, reason=payload.reason, reviewer=payload.reviewer,
        resulting_status=STATUS_BY_ACTION[payload.action], fact_ids_json=fact_ids, corrected_fact_ids_json=corrected_ids,
    )
    db.add(review)
    if commit:
        db.commit()
    else:
        db.flush()
    return {"review_id": review_id, "action": payload.action, "resulting_status": STATUS_BY_ACTION[payload.action],
            "version_index": payload.version_index, "version_key": version_key_value, "fact_ids": fact_ids,
            "corrected_fact_ids": corrected_ids, "review_status": review_status(db, row.__tablename__, row.id, version_key_value)}
