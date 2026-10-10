"""Audited Agent/Skill orchestration for bounded stock data governance.

The Agent controls sequencing and acceptance. Executable Skills delegate all
business writes to existing allow-listed services; neither the Agent nor a
model receives an arbitrary SQL or unrestricted database-write capability.
"""

from __future__ import annotations

from collections.abc import Callable
from datetime import datetime, timezone
from hashlib import sha256
import json
from typing import Any
from uuid import uuid4

from jsonschema import Draft202012Validator
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.ai_hub import (
    AgentDataAssetLink,
    AgentDefinition,
    AgentKnowledgeBaseLink,
    AgentSkillLink,
    KnowledgeBase,
    ModelSkill,
)
from app.models.governance import AgentExecutionRun, SkillExecutionRun, StockGovernanceDetail
from app.models.market_data import StockSymbol
from app.models.pipeline import PipelineStageRun
from app.schemas.stock_batch import AI_AGENT_SKILL_GOVERNANCE
from app.services.security_master import listing_metadata
from app.services.model_hub import ModelHubError, ModelHubService


AGENT_CODE = "STOCK_DATA_GOVERNANCE_AGENT"
SKILL_IDENTITY = "SECURITY_BOARD_IDENTITY_GOVERNOR"
SKILL_COLLECTION = "STOCK_SOURCE_COLLECTION"
SKILL_QUALITY = "STOCK_DATA_QUALITY_GATE"
SKILL_KNOWLEDGE = "LAKEHOUSE_KNOWLEDGE_PUBLISHER"
SKILL_ACCEPTANCE = "STOCK_GOVERNANCE_ACCEPTANCE"
SKILL_EVENTS = "NEWS_NOTICE_EVENT_GOVERNOR"


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


def _json_hash(value: Any) -> str:
    payload = json.dumps(value, ensure_ascii=False, sort_keys=True, default=str, separators=(",", ":"))
    return sha256(payload.encode("utf-8")).hexdigest()


def _bounded_evidence(value: Any, *, limit: int = 500) -> list[str]:
    evidence: list[str] = []

    def visit(item: Any, parent: str = "") -> None:
        if len(evidence) >= limit:
            return
        if isinstance(item, dict):
            for key, child in item.items():
                key_text = str(key)
                if child not in (None, "", [], {}) and (
                    key_text.endswith("_id")
                    or key_text in {"object_id", "graph_id", "lineage_batch_id", "fetch_log_id"}
                ):
                    evidence.append(f"{key_text}:{child}")
                elif isinstance(child, (dict, list)):
                    visit(child, key_text)
        elif isinstance(item, list):
            for child in item:
                visit(child, parent)

    visit(value)
    return list(dict.fromkeys(evidence))[:limit]


class StockGovernanceAgentSession:
    """One immutable Agent execution with audited, allow-listed Skill calls."""

    def __init__(
        self,
        db: Session,
        *,
        governance_batch_id: str,
        pipeline_run_id: int | None,
        request_json: dict[str, Any],
    ):
        self.db = db
        self.governance_batch_id = governance_batch_id
        self.pipeline_run_id = pipeline_run_id
        self.structure_documents = bool(request_json.get("structure_documents"))
        self.agent = self._agent()
        self.skills = self._skills()
        self.execution = self._start(request_json)

    def _agent(self) -> AgentDefinition:
        agent = self.db.scalar(select(AgentDefinition).where(
            AgentDefinition.agent_code == AGENT_CODE,
            AgentDefinition.enabled.is_(True),
            AgentDefinition.lifecycle_status == "ENABLED",
        ))
        if agent is None:
            raise LookupError(f"治理 Agent {AGENT_CODE} 未启用或未初始化")
        return agent

    def _skills(self) -> dict[str, ModelSkill]:
        required = {
            SKILL_IDENTITY, SKILL_COLLECTION, SKILL_QUALITY,
            SKILL_KNOWLEDGE, SKILL_ACCEPTANCE,
        }
        if self.structure_documents:
            required.add(SKILL_EVENTS)
        rows = list(self.db.scalars(
            select(ModelSkill)
            .join(AgentSkillLink, AgentSkillLink.skill_id == ModelSkill.id)
            .where(
                AgentSkillLink.agent_id == self.agent.id,
                ModelSkill.skill_code.in_(required),
                ModelSkill.enabled.is_(True),
                ModelSkill.lifecycle_status == "ENABLED",
            )
        ).all())
        by_code = {row.skill_code: row for row in rows}
        missing = required - set(by_code)
        if missing:
            raise LookupError(f"治理 Agent 缺少已启用 Skill: {sorted(missing)}")
        for skill in rows:
            policy = dict(skill.permission_policy_json or {})
            if policy.get("arbitrary_sql") or policy.get("unrestricted_database_write"):
                raise PermissionError(f"Skill {skill.skill_code} 权限超出治理白名单")
        return by_code

    def _start(self, request_json: dict[str, Any]) -> AgentExecutionRun:
        stage_id = self.db.scalar(select(PipelineStageRun.id).where(
            PipelineStageRun.pipeline_run_id == self.pipeline_run_id,
            PipelineStageRun.stage_code == "stock_batch_governance",
        )) if self.pipeline_run_id else None
        asset_codes = list(self.db.scalars(
            select(AgentDataAssetLink.data_asset_id).where(
                AgentDataAssetLink.agent_id == self.agent.id,
            )
        ).all())
        knowledge_versions: dict[str, str] = {}
        for kb in self.db.scalars(
            select(KnowledgeBase)
            .join(AgentKnowledgeBaseLink, AgentKnowledgeBaseLink.knowledge_base_id == KnowledgeBase.id)
            .where(AgentKnowledgeBaseLink.agent_id == self.agent.id)
        ).all():
            knowledge_versions[kb.kb_code] = kb.version
        run = AgentExecutionRun(
            run_key=f"stock-governance:{self.governance_batch_id}:{self.pipeline_run_id or 'manual'}:{uuid4()}",
            governance_mode=AI_AGENT_SKILL_GOVERNANCE,
            governance_batch_id=self.governance_batch_id,
            pipeline_run_id=self.pipeline_run_id,
            pipeline_stage_run_id=stage_id,
            agent_id=self.agent.id,
            agent_code=self.agent.agent_code,
            agent_version=self.agent.version,
            model_instance_code=self.agent.model_instance_code,
            trigger_type="PIPELINE",
            status="RUNNING",
            correlation_id=self.governance_batch_id,
            input_hash=_json_hash(request_json),
            input_json=request_json,
            accessible_assets_json=[str(item) for item in asset_codes],
            knowledge_versions_json=knowledge_versions,
            skill_versions_json={code: skill.version for code, skill in self.skills.items()},
            audit_json={
                "arbitrary_sql": False,
                "unrestricted_database_write": False,
                "execution_policy": "ALLOW_LISTED_SKILLS_ONLY",
            },
        )
        self.db.add(run)
        self.db.commit()
        self.db.refresh(run)
        return run

    def run_skill(
        self,
        skill_code: str,
        *,
        input_json: dict[str, Any],
        idempotency_scope: str,
        operation: Callable[[SkillExecutionRun], dict[str, Any]],
    ) -> dict[str, Any]:
        skill = self.skills[skill_code]
        Draft202012Validator(skill.input_contract_json or {}).validate(input_json)
        policy = dict(skill.permission_policy_json or {})
        execution = SkillExecutionRun(
            governance_mode=AI_AGENT_SKILL_GOVERNANCE,
            governance_batch_id=self.governance_batch_id,
            agent_execution_run_id=self.execution.id,
            pipeline_stage_run_id=self.execution.pipeline_stage_run_id,
            skill_id=skill.id,
            skill_code=skill.skill_code,
            skill_version=skill.version,
            status="RUNNING",
            max_attempts=int((skill.retry_policy_json or {}).get("max_attempts") or 1),
            idempotency_key=f"{self.execution.id}:{skill_code}:{idempotency_scope}",
            input_hash=_json_hash(input_json),
            input_json=input_json,
            side_effect_level=skill.side_effect_level,
            write_scope_json=dict(policy.get("write_scope") or {}),
        )
        self.db.add(execution)
        self.db.commit()
        self.db.refresh(execution)
        previous_context = self.db.info.get("stock_governance")
        self.db.info["stock_governance"] = {
            "governance_mode": AI_AGENT_SKILL_GOVERNANCE,
            "governance_batch_id": self.governance_batch_id,
            "agent_execution_run_id": self.execution.id,
            "agent_version": self.agent.version,
            "skill_execution_run_id": execution.id,
            "skill_code": skill.skill_code,
            "skill_version": skill.version,
        }
        try:
            result = operation(execution)
            Draft202012Validator(skill.output_contract_json or {}).validate(result)
            status = str(result.get("status") or "SUCCESS").upper()
            execution.status = (
                "FAILED" if status in {"FAILED", "ERROR", "SOURCE_FAILED"}
                else "PARTIAL" if status in {"PARTIAL", "MISSING"}
                else "SUCCEEDED"
            )
            execution.output_json = result
            execution.output_hash = _json_hash(result)
            execution.evidence_ids_json = _bounded_evidence(result)
            execution.completed_at = utc_now()
            self.db.commit()
            return result
        except Exception as exc:
            self.db.rollback()
            persisted = self.db.get(SkillExecutionRun, execution.id)
            if persisted is not None:
                persisted.status = "FAILED"
                persisted.error_code = type(exc).__name__
                persisted.error_message = str(exc)[:4000]
                persisted.completed_at = utc_now()
                self.db.commit()
            raise
        finally:
            if previous_context is None:
                self.db.info.pop("stock_governance", None)
            else:
                self.db.info["stock_governance"] = previous_context

    def review_quality_with_model(self, quality_summary: dict[str, Any]) -> dict[str, Any]:
        """Ask a real configured model to review bounded deterministic results.

        The response is advisory. Deterministic quality states stay
        authoritative and the whole raw stock payload is never sent.
        """
        prompt = {
            "governance_batch_id": self.governance_batch_id,
            "authoritative_rule": (
                "硬规则状态不可由模型升级；只识别重试优先级、来源缺口和Skill修复建议"
            ),
            "quality_summary": quality_summary,
        }
        try:
            log = ModelHubService(self.db).chat(
                task_type="data_governance",
                instance_code=self.agent.model_instance_code,
                messages=[
                    {
                        "role": "system",
                        "content": (
                            "你是股票数据治理质量审阅模型。只审阅输入的确定性摘要，"
                            "不得修改通过状态、不得补造证券数据。请用中文输出简洁的缺口、"
                            "重试优先级和Skill修复建议。"
                        ),
                    },
                    {"role": "user", "content": json.dumps(prompt, ensure_ascii=False)},
                ],
                temperature=0.0,
                max_tokens=6000,
                metadata_json={
                    "agent_code": self.agent.agent_code,
                    "agent_version": self.agent.version,
                    "agent_execution_run_id": self.execution.id,
                    "governance_batch_id": self.governance_batch_id,
                    "candidate_count": int(quality_summary.get("stock_count") or 0),
                    "purpose": "ADVISORY_QUALITY_REVIEW",
                    "require_real_model": True,
                },
            )
        except (ModelHubError, ValueError) as exc:
            return {
                "status": "FAILED",
                "call_log_id": getattr(exc, "call_log_id", None),
                "error": str(exc),
                "authoritative": False,
            }
        self.execution.model_call_log_id = log.id
        self.execution.model_instance_code = log.instance_code
        self.execution.model_version = log.model_code
        self.db.commit()
        is_real_model = str(log.provider_code).upper() != "MOCK"
        has_review = bool(str(log.response_text or "").strip())
        return {
            "status": "SUCCESS" if log.status == "SUCCESS" and is_real_model and has_review else "PARTIAL",
            "call_log_id": log.id,
            "provider_code": log.provider_code,
            "instance_code": log.instance_code,
            "model_code": log.model_code,
            "response_text": log.response_text,
            "real_model": is_real_model,
            "has_review_content": has_review,
            "authoritative": False,
        }

    def finish(self, output: dict[str, Any], *, error: Exception | None = None) -> None:
        self.db.rollback()
        run = self.db.get(AgentExecutionRun, self.execution.id)
        if run is None:
            return
        if error is not None:
            run.status = "FAILED"
            run.error_code = type(error).__name__
            run.error_message = str(error)[:4000]
        else:
            result_status = str(output.get("result_status") or "FAILED").upper()
            run.status = "SUCCEEDED" if result_status == "COMPLETED" else result_status
            run.output_json = output
            run.output_hash = _json_hash(output)
            run.evidence_ids_json = _bounded_evidence(output)
        run.completed_at = utc_now()
        self.db.commit()


def ensure_stock_governance_details(
    db: Session,
    *,
    governance_batch_id: str,
    governance_mode: str,
    stocks: list[StockSymbol],
    pipeline_run_id: int | None,
    agent_execution_run_id: str | None,
    baseline_batch_id: str | None = "all-boards-20261008",
) -> list[StockGovernanceDetail]:
    rows: list[StockGovernanceDetail] = []
    for stock in stocks:
        row = db.scalar(select(StockGovernanceDetail).where(
            StockGovernanceDetail.governance_batch_id == governance_batch_id,
            StockGovernanceDetail.market == stock.market,
            StockGovernanceDetail.symbol == stock.symbol,
        ))
        if row is None:
            board = listing_metadata(stock.market, stock.symbol, stock.ext_json, stock.raw_payload)["listing_board"]
            row = StockGovernanceDetail(
                governance_batch_id=governance_batch_id,
                governance_mode=governance_mode,
                board_code=board,
                market=stock.market,
                symbol=stock.symbol,
                stock_name=stock.name,
                pipeline_run_id=pipeline_run_id,
                agent_execution_run_id=agent_execution_run_id,
                baseline_batch_id=baseline_batch_id if governance_mode == AI_AGENT_SKILL_GOVERNANCE else None,
                status="RUNNING",
            )
            db.add(row)
        else:
            row.governance_mode = governance_mode
            row.board_code = listing_metadata(
                stock.market, stock.symbol, stock.ext_json, stock.raw_payload
            )["listing_board"]
            row.stock_name = stock.name
            row.pipeline_run_id = pipeline_run_id
            row.agent_execution_run_id = agent_execution_run_id
            row.status = "RUNNING"
            row.started_at = utc_now()
            row.completed_at = None
            if governance_mode == AI_AGENT_SKILL_GOVERNANCE:
                row.baseline_batch_id = baseline_batch_id
        rows.append(row)
    db.commit()
    return rows


def finalize_stock_governance_details(
    db: Session,
    *,
    governance_batch_id: str,
    output: dict[str, Any],
) -> None:
    business = {
        (str(item.get("market")), str(item.get("symbol"))): item
        for item in output.get("stock_results") or []
    }
    market_runs = {
        str(item.get("market")): item
        for item in ((output.get("stage_results") or {}).get("KNOWLEDGE_PIPELINE") or {}).get("market_runs") or []
    }
    rows = list(db.scalars(select(StockGovernanceDetail).where(
        StockGovernanceDetail.governance_batch_id == governance_batch_id,
    )).all())
    target_pairs = {
        (str(item["market"]), str(item["symbol"]))
        for item in output.get("target_stocks") or []
    }
    if not target_pairs:
        target_pairs = set(business)
    for row in rows:
        if (row.market, row.symbol) not in target_pairs:
            continue
        stock_result = business.get((row.market, row.symbol), {})
        market_result = market_runs.get(row.market, {})
        operations = stock_result.get("operations") or {}
        sources = [
            str(item.get("source_code")) for item in operations.values()
            if isinstance(item, dict) and item.get("source_code")
        ]
        evidence = [
            f"fetch_log:{item.get('fetch_log_id')}" for item in operations.values()
            if isinstance(item, dict) and item.get("fetch_log_id")
        ]
        evidence.extend(_bounded_evidence(market_result))
        business_status = str(
            stock_result.get("status")
            or (row.stage_status_json or {}).get("BUSINESS_DATA")
            or "SKIPPED"
        ).upper()
        knowledge_status = str(
            market_result.get("status")
            or (row.stage_status_json or {}).get("KNOWLEDGE_PIPELINE")
            or "SKIPPED"
        ).upper()
        if "FAILED" in {business_status, knowledge_status}:
            status = "FAILED"
        elif "PARTIAL" in {business_status, knowledge_status}:
            status = "PARTIAL"
        elif business_status in {"SUCCESS", "SKIPPED"} and knowledge_status in {"COMPLETED", "SUCCESS", "SKIPPED"}:
            status = "PASS"
        else:
            status = "MISSING"
        review = ((output.get("agent_skill_governance") or {}).get("model_quality_review") or {})
        event_run = next((item for item in (output.get("stage_results", {}).get("STRUCTURED_EVENTS") or {}).get("stock_runs") or []
                          if item.get("market") == row.market and item.get("symbol") == row.symbol), None)
        if event_run:
            evidence.extend(f"fact_id:{value}" for value in event_run.get("fact_ids") or [])
            if event_run.get("status") != "SUCCESS" and status == "PASS":
                status = "PARTIAL"
        if row.governance_mode == AI_AGENT_SKILL_GOVERNANCE and review.get("status") != "SUCCESS":
            status = "PARTIAL" if status == "PASS" else status
        row.status = status
        row.stage_status_json = {
            "BUSINESS_DATA": business_status,
            "KNOWLEDGE_PIPELINE": knowledge_status,
            "STRUCTURED_EVENTS": event_run.get("status") if event_run else "SKIPPED",
            "operations": {
                **((row.stage_status_json or {}).get("operations") or {}),
                **{key: value.get("status") for key, value in operations.items()},
            },
        }
        row.source_ids_json = list(dict.fromkeys([*(row.source_ids_json or []), *sources]))
        row.evidence_ids_json = list(dict.fromkeys([*(row.evidence_ids_json or []), *evidence]))[:500]
        row.quality_summary_json = {
            "result_status": output.get("result_status"),
            "errors": stock_result.get("errors") or [],
            "completion": market_result.get("completion") or {},
            "no_false_completion": True,
            "structured_events": {key: event_run.get(key) for key in ("total", "completed", "remaining", "processed_this_run")} if event_run else {},
            "model_review": {
                key: review.get(key) for key in ("status", "call_log_id", "real_model", "model_code")
            },
        }
        row.completed_at = utc_now()
    db.commit()
