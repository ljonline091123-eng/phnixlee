from __future__ import annotations

import asyncio
import json
from pathlib import Path
from typing import Any

from pydantic import BaseModel, ConfigDict

from app.core.agent.base_agent import BaseAgent


class DemoOutput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    value: int


class DemoAgent(BaseAgent[DemoOutput]):
    def build_user_prompt(
        self,
        prepared_context: Any,
        *,
        validation_feedback: list[dict[str, Any]] | None,
        **kwargs: Any,
    ) -> str:
        return json.dumps({
            "context": prepared_context,
            "validation_feedback": validation_feedback,
        })

    def build_fallback(
        self,
        prepared_context: Any,
        *,
        failure: Exception | None,
        **kwargs: Any,
    ) -> DemoOutput:
        return DemoOutput(value=-1)


def _agent(prompt_path: Path, llm_call: Any) -> DemoAgent:
    return DemoAgent(
        prompt_path=prompt_path,
        response_schema=DemoOutput,
        task_type="demo_task",
        instance_code="DEMO_INSTANCE",
        llm_call=llm_call,
    )


def test_base_agent_loads_prompt_schema_and_retries_once(tmp_path: Path) -> None:
    prompt_path = tmp_path / "DEMO.SKILL.md"
    prompt_path.write_text("DEMO SYSTEM V1", encoding="utf-8")
    calls: list[dict[str, Any]] = []

    async def llm_call(**kwargs: Any) -> str:
        calls.append(kwargs)
        if len(calls) == 1:
            return '{"value":"invalid"}'
        return '{"value":2}'

    agent = _agent(prompt_path, llm_call)
    result = asyncio.run(agent.execute({"record_id": "DEMO001"}))

    assert result.value == 2
    assert len(calls) == 2
    assert calls[0]["system_prompt"] == "DEMO SYSTEM V1"
    assert calls[0]["json_schema"]["properties"]["value"]["type"] == "integer"
    retry_payload = json.loads(calls[1]["user_prompt"])
    assert retry_payload["validation_feedback"]

    prompt_path.write_text("DEMO SYSTEM V2", encoding="utf-8")
    calls.clear()
    result = asyncio.run(agent.execute({"record_id": "DEMO002"}))
    assert result.value == 2
    assert calls[0]["system_prompt"] == "DEMO SYSTEM V2"


def test_base_agent_falls_back_after_exactly_two_invalid_outputs(tmp_path: Path) -> None:
    prompt_path = tmp_path / "DEMO.SKILL.md"
    prompt_path.write_text("DEMO SYSTEM", encoding="utf-8")
    call_count = 0

    def llm_call(**_kwargs: Any) -> str:
        nonlocal call_count
        call_count += 1
        return '{"value":"invalid"}'

    result = asyncio.run(_agent(prompt_path, llm_call).execute({"record_id": "DEMO001"}))

    assert call_count == 2
    assert result == DemoOutput(value=-1)
