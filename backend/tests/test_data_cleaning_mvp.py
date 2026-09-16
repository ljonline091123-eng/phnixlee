from __future__ import annotations

import asyncio
from datetime import datetime, timedelta, timezone
from pathlib import Path
from unittest.mock import patch

import pytest
from pydantic import ValidationError
from sqlalchemy import create_engine
from sqlalchemy.orm import Session, sessionmaker

from app.connectors.model_adapters import GeminiRestAdapter, OpenAICompatAdapter
from app.core.agent.data_cleaning_runner import run_data_cleaning_agent
from app.db.base import Base
from app.models.ai_hub import GovernanceRun, ModelInstance
from app.schemas.data_governance import DataCleaningOutput, GovernanceStatus
from app.services.skill_tools import validate_dw_records


def _mock_records() -> tuple[list[dict], set[str]]:
    now = datetime.now(timezone.utc)
    records: list[dict] = []
    for index in range(15):
        category = "NEWS" if index % 2 == 0 else "FINANCIAL_REPORT"
        records.append({
            "market": "CN_A",
            "stock_code": f"DEMO{index + 1:03d}",
            "category": category,
            "source": "DEMO_PROVIDER",
            "source_record_id": f"DEMO-NORMAL-{index + 1:03d}",
            "observed_at": (now - timedelta(hours=index + 1)).isoformat(),
            "payload_json": {},
        })

    invalid_ids: set[str] = set()
    for index in range(3):
        record_id = f"DEMO-MISSING-{index + 1:03d}"
        invalid_ids.add(record_id)
        records.append({
            "market": "CN_A",
            "stock_code": f"DEMO{index + 101:03d}",
            "category": "NEWS",
            "source": "",
            "source_record_id": record_id,
            "observed_at": (now - timedelta(hours=1)).isoformat(),
            "payload_json": {},
        })

    for index in range(2):
        record_id = f"DEMO-FUTURE-{index + 1:03d}"
        invalid_ids.add(record_id)
        records.append({
            "market": "CN_A",
            "stock_code": f"DEMO{index + 201:03d}",
            "category": "FINANCIAL_REPORT",
            "source": "DEMO_PROVIDER",
            "source_record_id": record_id,
            "observed_at": (now + timedelta(days=1, minutes=index)).isoformat(),
            "payload_json": {},
        })
    return records, invalid_ids


def test_data_contract_rejects_unknown_fields_and_inconsistent_completed_status() -> None:
    payload = {
        "as_of": "2026-09-15T10:00:00+08:00",
        "status": "COMPLETED",
        "category_counts": {"NEWS": 1},
        "quality_issues": [],
        "dedupe_keys": ["source_record_id"],
        "target_tables": [],
        "pending_review": [],
        "missing_data": [],
        "confidence": 1.0,
        "evidence_text": "DEMO contract validation",
    }
    assert DataCleaningOutput.model_validate(payload).status == GovernanceStatus.COMPLETED
    with pytest.raises(ValidationError):
        DataCleaningOutput.model_validate({**payload, "unknown": True})
    with pytest.raises(ValidationError):
        DataCleaningOutput.model_validate({**payload, "missing_data": ["DEMO001.source"]})


def test_hard_rules_find_the_five_expected_records() -> None:
    records, invalid_ids = _mock_records()
    result = validate_dw_records(records)
    assert result["issue_count"] == 5
    assert len(result["quality_issues"]) == 5
    assert set(result["issue_record_ids"]) == invalid_ids
    assert result["category_counts"] == {"NEWS": 11, "FINANCIAL_REPORT": 9}


def test_hard_rule_samples_are_bounded_but_all_issue_ids_are_retained() -> None:
    now = datetime.now(timezone.utc).isoformat()
    records = [{
        "market": "CN_A",
        "stock_code": f"DEMO{index:03d}",
        "category": "NEWS",
        "source": "",
        "source_record_id": f"DEMO-ISSUE-{index:03d}",
        "observed_at": now,
    } for index in range(60)]
    result = validate_dw_records(records)
    assert result["issue_count"] == 60
    assert len(result["quality_issues"]) == 50
    assert result["quality_issues_truncated"] is True
    assert len(result["issue_record_ids"]) == 60


def test_empty_batch_requires_review_without_negative_valid_count() -> None:
    result = validate_dw_records([])
    assert result["status"] == "PENDING_REVIEW"
    assert result["valid_record_count"] == 0
    assert result["quality_issues"][0]["issue_type"] == "EMPTY_BATCH"
    assert result["issue_record_ids"] == ["BATCH"]


def test_agent_retries_then_downgrades_and_persists(tmp_path: Path) -> None:
    engine = create_engine(
        f"sqlite:///{(tmp_path / 'governance.db').as_posix()}",
        connect_args={"check_same_thread": False},
    )
    Base.metadata.create_all(engine)
    test_session = sessionmaker(bind=engine, autoflush=False, autocommit=False)
    with test_session() as db:
        run = GovernanceRun(target_type="DATA_ASSET", target_id=1, status="RUNNING")
        db.add(run)
        db.commit()
        run_id = run.id

    calls: list[dict] = []

    async def invalid_llm(**kwargs) -> str:
        calls.append(kwargs)
        return '{"status":"NOT_ALLOWED"}'

    records, invalid_ids = _mock_records()
    output = asyncio.run(run_data_cleaning_agent(
        records,
        llm_call=invalid_llm,
        governance_run_id=run_id,
        session_factory=test_session,
    ))

    assert len(calls) == 2
    assert len(output.quality_issues) == 5
    assert output.status == GovernanceStatus.PENDING_REVIEW
    assert invalid_ids.issubset({item.record_id for item in output.pending_review})
    assert "上一次输出未通过 Pydantic 校验" in calls[1]["user_prompt"]
    assert "DEMO-NORMAL-001" not in calls[0]["user_prompt"]

    with test_session() as db:
        saved = db.get(GovernanceRun, run_id)
        assert saved is not None
        assert saved.status == "PENDING_REVIEW"
        assert len(saved.summary_json["quality_issues"]) == 5
    engine.dispose()


def test_hard_rule_facts_override_a_valid_but_incorrect_model_answer() -> None:
    records, invalid_ids = _mock_records()
    calls = 0

    async def incorrect_llm(**_kwargs) -> str:
        nonlocal calls
        calls += 1
        return DataCleaningOutput(
            as_of=datetime.now(timezone.utc),
            status="COMPLETED",
            category_counts={},
            quality_issues=[],
            dedupe_keys=["source_record_id"],
            target_tables=[],
            pending_review=[],
            missing_data=[],
            confidence=0.99,
            evidence_text="DEMO model claimed that no issue exists",
        ).model_dump_json()

    output = asyncio.run(run_data_cleaning_agent(records, llm_call=incorrect_llm))
    assert calls == 1
    assert output.status == GovernanceStatus.PENDING_REVIEW
    assert len(output.quality_issues) == 5
    assert invalid_ids.issubset({item.record_id for item in output.pending_review})
    assert output.category_counts == {"NEWS": 11, "FINANCIAL_REPORT": 9}


def test_model_adapters_send_native_structured_output_schema() -> None:
    schema = DataCleaningOutput.model_json_schema()
    metadata = {
        "json_schema_output": schema,
        "json_schema_name": "data_cleaning_output",
        "native_structured_output": True,
    }
    openai = ModelInstance(
        provider_id=1,
        instance_code="OPENAI_STRUCTURED_TEST",
        model_code="gpt-test",
        model_name="OpenAI test",
        api_key="test-key",
        api_base_url="https://example.invalid/v1",
        config_json={},
    )
    with patch("app.connectors.model_adapters.httpx.Client") as client_type:
        post = client_type.return_value.__enter__.return_value.post
        post.return_value.json.return_value = {"choices": [{"message": {"content": "{}"}}]}
        OpenAICompatAdapter().chat(openai, [{"role": "user", "content": "test"}], metadata_json=metadata)
        response_format = post.call_args.kwargs["json"]["response_format"]
        assert response_format["type"] == "json_schema"
        assert response_format["json_schema"]["strict"] is True
        assert response_format["json_schema"]["schema"] == schema

    gemini = ModelInstance(
        provider_id=1,
        instance_code="GEMINI_STRUCTURED_TEST",
        model_code="gemini-test",
        model_name="Gemini test",
        api_key="test-key",
        api_base_url="https://generativelanguage.googleapis.com/v1beta",
        config_json={},
    )
    with patch("app.connectors.model_adapters.httpx.Client") as client_type:
        post = client_type.return_value.__enter__.return_value.post
        post.return_value.json.return_value = {"candidates": [{"content": {"parts": [{"text": "{}"}]}}]}
        GeminiRestAdapter().chat(gemini, [{"role": "user", "content": "test"}], metadata_json=metadata)
        generation_config = post.call_args.kwargs["json"]["generationConfig"]
        assert generation_config["responseMimeType"] == "application/json"
        assert generation_config["responseJsonSchema"] == schema
