from __future__ import annotations

from typing import Any

from pydantic import BaseModel, Field


class SkillEvaluationCaseCreate(BaseModel):
    case_code: str = Field(min_length=1, max_length=128)
    task_type: str = Field(min_length=1, max_length=64)
    description: str | None = Field(default=None, max_length=4000)
    input_json: dict[str, Any] = Field(default_factory=dict)
    expected_json: dict[str, Any] = Field(default_factory=dict)
    rubric_json: dict[str, Any] = Field(default_factory=dict)
    dataset_version: str = Field(default="v1", max_length=64)
    source: str = Field(default="HUMAN", max_length=32)


class SkillEvaluationRunCreate(BaseModel):
    skill_code: str = Field(min_length=1, max_length=64)
    task_type: str = Field(min_length=1, max_length=64)
    case_ids: list[int] = Field(default_factory=list, max_length=500)
    outputs: dict[str, dict[str, Any]] = Field(default_factory=dict)
    use_model: bool = False
    instance_code: str | None = Field(default=None, max_length=64)
    dataset_version: str = Field(default="v1", max_length=64)


class RetrospectiveRepairFlowRequest(BaseModel):
    """Optional frozen output for a retrospective regression run.

    When omitted, the service uses the output stored in the decision snapshot.
    The repair step remains review-only and never changes the active Skill.
    """

    evaluation_output: dict[str, Any] | None = None
    request_repair_draft: bool = True

