from __future__ import annotations

import asyncio
from collections.abc import Awaitable, Callable
from datetime import datetime
import inspect
import json
import os
from pathlib import Path
from typing import Any

from pydantic import ValidationError
from sqlalchemy.orm import Session

from app.db.session import SessionLocal
from app.models.ai_hub import GovernanceRun
from app.schemas.data_governance import (
    DataCleaningOutput,
    GovernanceStatus,
    PendingReview,
    QualityIssue,
    TargetTable,
)
from app.services.model_hub import ModelHubService
from app.services.skill_tools import validate_dw_records


SKILL_PATH = Path(__file__).resolve().parents[3] / "skill_docs" / "DATA_CLEANING_DW.SKILL.md"
DEFAULT_INSTANCE_CODE = "GEMINI_FLASH"

LlmCall = Callable[..., str | Awaitable[str]]
SessionFactory = Callable[[], Session]

TARGET_TABLE_CATALOG: dict[str, tuple[str, list[str]]] = {
    "BASIC": ("stock_symbol", ["market", "symbol"]),
    "BASIC_DATA": ("stock_symbol", ["market", "symbol"]),
    "NEWS": ("stock_news", ["market", "symbol", "news_time", "title", "source_id"]),
    "NOTICE": ("stock_notice", ["market", "symbol", "notice_date", "title", "source_id"]),
    "ANNOUNCEMENT": ("stock_notice", ["market", "symbol", "notice_date", "title", "source_id"]),
    "FINANCIAL": (
        "stock_financial_report",
        ["market", "symbol", "indicator", "report_period", "source_id"],
    ),
    "FINANCIAL_REPORT": (
        "stock_financial_report",
        ["market", "symbol", "indicator", "report_period", "source_id"],
    ),
    "PRICE": ("stock_kline", ["market", "symbol", "period", "adjust", "trade_date"]),
    "KLINE": ("stock_kline", ["market", "symbol", "period", "adjust", "trade_date"]),
    "VOLUME": ("stock_kline", ["market", "symbol", "period", "adjust", "trade_date"]),
    "SHAREHOLDER": ("stock_f10_cache", ["market", "symbol", "source_id"]),
    "F10": ("stock_f10_cache", ["market", "symbol", "source_id"]),
    "RESEARCH": ("research_report", ["market", "symbol", "created_at"]),
    "RESEARCH_REPORT": ("research_report", ["market", "symbol", "created_at"]),
}


def _read_skill_prompt() -> str:
    if not SKILL_PATH.is_file():
        raise FileNotFoundError(f"Data cleaning Skill file not found: {SKILL_PATH}")
    content = SKILL_PATH.read_text(encoding="utf-8").strip()
    if not content:
        raise ValueError(f"Data cleaning Skill file is empty: {SKILL_PATH}")
    return content


def _target_tables(category_counts: dict[str, int]) -> list[TargetTable]:
    targets: list[TargetTable] = []
    for category in category_counts:
        mapping = TARGET_TABLE_CATALOG.get(category)
        if not mapping:
            continue
        table_name, business_key = mapping
        targets.append(TargetTable(
            category=category,
            table_name=table_name,
            business_key=business_key,
            evidence_text=f"Validated mapping catalog: {category} -> {table_name}",
        ))
    return targets


def _pending_reviews(issues: list[QualityIssue]) -> list[PendingReview]:
    grouped: dict[str, list[QualityIssue]] = {}
    for issue in issues:
        grouped.setdefault(issue.record_id, []).append(issue)
    return [
        PendingReview(
            record_id=record_id,
            reason=", ".join(dict.fromkeys(item.issue_type for item in record_issues)),
            evidence_text="; ".join(item.evidence_text for item in record_issues),
        )
        for record_id, record_issues in grouped.items()
    ]


def _pending_reviews_from_precheck(precheck: dict[str, Any]) -> list[PendingReview]:
    issues = [QualityIssue.model_validate(item) for item in precheck["quality_issues"]]
    pending = _pending_reviews(issues)
    included = {item.record_id for item in pending}
    for record_id in precheck["issue_record_ids"]:
        if record_id in included:
            continue
        pending.append(PendingReview(
            record_id=record_id,
            reason="QUALITY_ISSUE_OUTSIDE_SAMPLE_LIMIT",
            evidence_text="validate_dw_records reported this record after the first 50 issue samples",
        ))
    return pending


def _missing_data(issues: list[QualityIssue]) -> list[str]:
    return [
        f"{issue.record_id}.{issue.field_name}"
        for issue in issues
        if issue.issue_type in {"MISSING_REQUIRED_FIELD", "MISSING_SOURCE_RECORD_ID"}
    ]


def _unmapped_categories(category_counts: dict[str, int]) -> list[str]:
    return [category for category in category_counts if category not in TARGET_TABLE_CATALOG]


def _prompt_payload(precheck: dict[str, Any]) -> dict[str, Any]:
    return {
        "as_of": precheck["as_of"],
        "total_records": precheck["total_records"],
        "category_counts": precheck["category_counts"],
        "issue_count": precheck["issue_count"],
        "quality_issues": precheck["quality_issues"],
        "quality_issues_truncated": precheck["quality_issues_truncated"],
        "issue_record_count": len(precheck["issue_record_ids"]),
        "issue_record_id_sample": precheck["issue_record_ids"][:50],
        "valid_record_count": precheck["valid_record_count"],
        "duplicate_count": precheck["duplicate_count"],
        "dedupe_keys": precheck["dedupe_keys"],
        "target_table_catalog": {
            category: {"table_name": value[0], "business_key": value[1]}
            for category, value in TARGET_TABLE_CATALOG.items()
            if category in precheck["category_counts"]
        },
        "unmapped_categories": _unmapped_categories(precheck["category_counts"]),
    }


def _build_user_prompt(precheck: dict[str, Any], validation_feedback: list[dict[str, Any]] | None = None) -> str:
    payload = _prompt_payload(precheck)
    parts = [
        "根据下列硬规则预审摘要生成 DataCleaningOutput。",
        "不得推翻 category_counts、quality_issues、dedupe_keys、as_of 或异常记录 ID；不得假设已经写入数据库。",
        "仅输出符合给定 JSON Schema 的 JSON 对象。",
        json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
    ]
    if validation_feedback:
        parts.extend([
            "上一次输出未通过 Pydantic 校验。修正以下字段错误后重新返回完整 JSON：",
            json.dumps(validation_feedback, ensure_ascii=False, separators=(",", ":")),
        ])
    return "\n".join(parts)


async def _default_llm_call(*, system_prompt: str, user_prompt: str, json_schema: dict[str, Any]) -> str:
    instance_code = os.getenv("DATA_CLEANING_MODEL_INSTANCE", DEFAULT_INSTANCE_CODE).strip() or None

    def invoke() -> str:
        with SessionLocal() as db:
            log = ModelHubService(db).chat(
                task_type="data_governance",
                instance_code=instance_code,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt},
                ],
                temperature=0.0,
                max_tokens=4096,
                metadata_json={
                    "json_schema_output": json_schema,
                    "json_schema_name": "data_cleaning_output",
                    "native_structured_output": True,
                    "defer_schema_validation": True,
                    "source": "data_cleaning_runner",
                },
            )
            if log.status != "SUCCESS" or not log.response_text:
                raise RuntimeError(log.error_message or "All data-governance model routes failed")
            return log.response_text

    return await asyncio.to_thread(invoke)


async def _call_model(
    llm_call: LlmCall,
    *,
    system_prompt: str,
    user_prompt: str,
    json_schema: dict[str, Any],
) -> str:
    response = llm_call(
        system_prompt=system_prompt,
        user_prompt=user_prompt,
        json_schema=json_schema,
    )
    if inspect.isawaitable(response):
        response = await response
    if not isinstance(response, str):
        raise TypeError("LLM call must return a JSON string")
    return response


def _reconcile_with_precheck(output: DataCleaningOutput, precheck: dict[str, Any]) -> DataCleaningOutput:
    issues = [QualityIssue.model_validate(item) for item in precheck["quality_issues"]]
    pending_by_id = {item.record_id: item for item in output.pending_review}
    for item in _pending_reviews_from_precheck(precheck):
        pending_by_id[item.record_id] = item

    missing_data = list(dict.fromkeys([*output.missing_data, *_missing_data(issues)]))
    unmapped = _unmapped_categories(precheck["category_counts"])
    missing_data.extend(f"target_table_mapping:{category}" for category in unmapped)
    missing_data = list(dict.fromkeys(missing_data))
    unresolved = bool(issues or pending_by_id or missing_data)
    evidence = (
        f"validate_dw_records checked {precheck['total_records']} records and found "
        f"{precheck['issue_count']} hard-rule issues (up to 50 detailed samples retained); "
        f"model evidence: {output.evidence_text}"
    )
    return DataCleaningOutput.model_validate({
        **output.model_dump(mode="json"),
        "as_of": precheck["as_of"],
        "status": GovernanceStatus.PENDING_REVIEW.value if unresolved else GovernanceStatus.COMPLETED.value,
        "category_counts": precheck["category_counts"],
        "quality_issues": [item.model_dump(mode="json") for item in issues],
        "dedupe_keys": precheck["dedupe_keys"],
        "target_tables": [item.model_dump(mode="json") for item in _target_tables(precheck["category_counts"])],
        "pending_review": [item.model_dump(mode="json") for item in pending_by_id.values()],
        "missing_data": missing_data,
        "evidence_text": evidence,
    })


def _fallback_output(precheck: dict[str, Any], failure: Exception) -> DataCleaningOutput:
    issues = [QualityIssue.model_validate(item) for item in precheck["quality_issues"]]
    pending = _pending_reviews_from_precheck(precheck)
    pending.append(PendingReview(
        record_id="BATCH",
        reason="LLM_STRUCTURED_OUTPUT_VALIDATION_FAILED",
        evidence_text=f"{type(failure).__name__}: {str(failure)[:500]}",
    ))
    missing_data = _missing_data(issues)
    missing_data.extend(
        f"target_table_mapping:{category}" for category in _unmapped_categories(precheck["category_counts"])
    )
    return DataCleaningOutput(
        as_of=datetime.fromisoformat(precheck["as_of"]),
        status=GovernanceStatus.PENDING_REVIEW,
        category_counts=precheck["category_counts"],
        quality_issues=issues,
        dedupe_keys=precheck["dedupe_keys"],
        target_tables=_target_tables(precheck["category_counts"]),
        pending_review=pending,
        missing_data=list(dict.fromkeys(missing_data)),
        confidence=0.0,
        evidence_text=(
            f"Deterministic fallback after structured model output failed; "
            f"validate_dw_records checked {precheck['total_records']} records."
        ),
    )


def _persist_governance_output(
    output: DataCleaningOutput,
    governance_run_id: int,
    session_factory: SessionFactory,
) -> None:
    with session_factory() as db:
        run = db.get(GovernanceRun, governance_run_id)
        if not run:
            raise LookupError(f"Governance run not found: {governance_run_id}")
        run.status = output.status.value
        run.summary_json = output.model_dump(mode="json")
        db.commit()


async def run_data_cleaning_agent(
    records: list[dict[str, Any]],
    *,
    llm_call: LlmCall | None = None,
    governance_run_id: int | None = None,
    session_factory: SessionFactory | None = None,
) -> DataCleaningOutput:
    """Run deterministic validation, structured model review, retry, and persistence."""

    precheck = validate_dw_records(records)
    system_prompt = _read_skill_prompt()
    schema = DataCleaningOutput.model_json_schema()
    caller = llm_call or _default_llm_call
    output: DataCleaningOutput

    try:
        response_text = await _call_model(
            caller,
            system_prompt=system_prompt,
            user_prompt=_build_user_prompt(precheck),
            json_schema=schema,
        )
        try:
            output = _reconcile_with_precheck(
                DataCleaningOutput.model_validate_json(response_text), precheck
            )
        except ValidationError as first_error:
            feedback = json.loads(first_error.json(include_url=False, include_input=False))
            retry_text = await _call_model(
                caller,
                system_prompt=system_prompt,
                user_prompt=_build_user_prompt(precheck, feedback),
                json_schema=schema,
            )
            output = _reconcile_with_precheck(
                DataCleaningOutput.model_validate_json(retry_text), precheck
            )
    except Exception as failure:
        output = _fallback_output(precheck, failure)

    if governance_run_id is not None:
        _persist_governance_output(
            output,
            governance_run_id,
            session_factory or SessionLocal,
        )
    return output
