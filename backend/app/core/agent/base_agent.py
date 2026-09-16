from __future__ import annotations

import asyncio
import inspect
import json
import re
from abc import ABC, abstractmethod
from collections.abc import Awaitable, Callable
from pathlib import Path
from typing import Any, Generic, TypeVar, cast

from pydantic import BaseModel, ValidationError
from sqlalchemy.orm import Session

from app.db.session import SessionLocal
from app.services.model_hub import ModelHubService


ResponseModelT = TypeVar("ResponseModelT", bound=BaseModel)
LlmCall = Callable[..., str | Awaitable[str]]
SessionFactory = Callable[[], Session]


class BaseAgent(ABC, Generic[ResponseModelT]):
    """Shared lifecycle for Skill agents that require structured model output."""

    def __init__(
        self,
        *,
        prompt_path: str | Path,
        response_schema: type[ResponseModelT],
        task_type: str,
        instance_code: str | None = None,
        llm_call: LlmCall | None = None,
        model_session_factory: SessionFactory = SessionLocal,
        temperature: float = 0.0,
        max_tokens: int = 4096,
        schema_name: str | None = None,
    ) -> None:
        self.prompt_path = Path(prompt_path)
        self.response_schema = response_schema
        self.task_type = task_type
        self.instance_code = instance_code
        self.llm_call = llm_call
        self.model_session_factory = model_session_factory
        self.temperature = temperature
        self.max_tokens = max_tokens
        self.schema_name = schema_name or self._default_schema_name(response_schema.__name__)

    @staticmethod
    def _default_schema_name(model_name: str) -> str:
        snake_case = re.sub(r"(?<!^)(?=[A-Z])", "_", model_name).lower()
        return re.sub(r"[^a-z0-9_-]", "_", snake_case)

    def load_system_prompt(self) -> str:
        """Load the current Skill Markdown for every execution."""
        if not self.prompt_path.is_file():
            raise FileNotFoundError(f"Skill prompt not found: {self.prompt_path}")
        prompt = self.prompt_path.read_text(encoding="utf-8").strip()
        if not prompt:
            raise ValueError(f"Skill prompt is empty: {self.prompt_path}")
        return prompt

    async def execute(self, context_data: Any, **kwargs: Any) -> ResponseModelT:
        """Execute, retry once with validation errors, then build a safe fallback."""
        prepared_context = await self._resolve(
            self.prepare_context(context_data, **kwargs)
        )
        system_prompt = self.load_system_prompt()
        json_schema = self.response_schema.model_json_schema()
        validation_feedback: list[dict[str, Any]] | None = None
        failure: Exception | None = None
        result: ResponseModelT | None = None

        for attempt in range(2):
            user_prompt = self.build_user_prompt(
                prepared_context,
                validation_feedback=validation_feedback,
                **kwargs,
            )
            try:
                raw_response = await self._call_model(
                    system_prompt=system_prompt,
                    user_prompt=user_prompt,
                    json_schema=json_schema,
                )
                parsed = self.response_schema.model_validate_json(raw_response)
                reconciled = await self._resolve(
                    self.reconcile_response(parsed, prepared_context, **kwargs)
                )
                result = self.response_schema.model_validate(reconciled)
                break
            except ValidationError as exc:
                failure = exc
                if attempt == 0:
                    validation_feedback = cast(
                        list[dict[str, Any]],
                        json.loads(exc.json(include_url=False, include_input=False)),
                    )
                    continue
                break
            except Exception as exc:
                failure = exc
                break

        if result is None:
            fallback = await self._resolve(
                self.build_fallback(prepared_context, failure=failure, **kwargs)
            )
            result = self.response_schema.model_validate(fallback)
        return await self._finalize(result, prepared_context, **kwargs)

    def prepare_context(self, context_data: Any, **kwargs: Any) -> Any:
        """Run deterministic tools or context reduction before the model call."""
        return context_data

    @abstractmethod
    def build_user_prompt(
        self,
        prepared_context: Any,
        *,
        validation_feedback: list[dict[str, Any]] | None,
        **kwargs: Any,
    ) -> str:
        """Build the user message from prepared context."""

    def reconcile_response(
        self,
        response: ResponseModelT,
        prepared_context: Any,
        **kwargs: Any,
    ) -> ResponseModelT:
        """Make deterministic facts authoritative after schema validation."""
        return response

    @abstractmethod
    def build_fallback(
        self,
        prepared_context: Any,
        *,
        failure: Exception | None,
        **kwargs: Any,
    ) -> ResponseModelT | dict[str, Any]:
        """Build a deterministic result after model or validation failure."""

    def finalize_response(
        self,
        response: ResponseModelT,
        prepared_context: Any,
        **kwargs: Any,
    ) -> ResponseModelT:
        """Optionally persist or audit the final response."""
        return response

    async def _call_model(
        self,
        *,
        system_prompt: str,
        user_prompt: str,
        json_schema: dict[str, Any],
    ) -> str:
        caller = self.llm_call or self._default_llm_call
        result = caller(
            system_prompt=system_prompt,
            user_prompt=user_prompt,
            json_schema=json_schema,
        )
        if inspect.isawaitable(result):
            result = await result
        if not isinstance(result, str):
            raise TypeError("LLM call must return a JSON string")
        return result

    async def _default_llm_call(
        self,
        *,
        system_prompt: str,
        user_prompt: str,
        json_schema: dict[str, Any],
    ) -> str:
        def invoke() -> str:
            with self.model_session_factory() as db:
                log = ModelHubService(db).chat(
                    task_type=self.task_type,
                    instance_code=self.instance_code,
                    messages=[
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": user_prompt},
                    ],
                    temperature=self.temperature,
                    max_tokens=self.max_tokens,
                    metadata_json={
                        "json_schema_output": json_schema,
                        "json_schema_name": self.schema_name,
                        "native_structured_output": True,
                        # Pydantic owns validation and the correction retry.
                        "defer_schema_validation": True,
                        "source": type(self).__name__,
                    },
                )
                if log.status != "SUCCESS" or not log.response_text:
                    raise RuntimeError(log.error_message or "All model routes failed")
                return log.response_text

        return await asyncio.to_thread(invoke)

    async def _finalize(
        self,
        response: ResponseModelT,
        prepared_context: Any,
        **kwargs: Any,
    ) -> ResponseModelT:
        finalized = await self._resolve(
            self.finalize_response(response, prepared_context, **kwargs)
        )
        return self.response_schema.model_validate(finalized)

    @staticmethod
    async def _resolve(value: Any) -> Any:
        if inspect.isawaitable(value):
            return await value
        return value
