from __future__ import annotations

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, func, select
from sqlalchemy.orm import Session
from sqlalchemy.pool import StaticPool

from app.api import governance_control, lakehouse, model_hub
from app.db.base import Base
from app.db.session import get_db
from app.models.ai_hub import AgentDataAsset, AgentDefinition, ModelSkill
from app.models.governance import AgentExecutionRun, SkillExecutionRun
from app.models.lakehouse import DocumentChunkVersion, LakeLineageEvent, LakeObject
from app.models.market_data import DataSource, StockSymbol
from app.orchestration.model_hub import AgentExecutionCommand, AgentExecutionWorkflow, SkillToolWorkflow
from app.services.model_hub import seed_default_models


def _engine():
    engine = create_engine(
        "sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool,
    )
    Base.metadata.create_all(engine)
    return engine


def _client(db: Session) -> TestClient:
    app = FastAPI()
    app.include_router(governance_control.router, prefix="/api/v1")
    app.include_router(lakehouse.router, prefix="/api/v1")
    app.include_router(model_hub.router, prefix="/api/v1")

    def override_db():
        yield db

    app.dependency_overrides[get_db] = override_db
    return TestClient(app)


def test_quality_rule_run_and_lakehouse_catalog_queries():
    with Session(_engine()) as db:
        source = DataSource(
            source_code="TEST", source_name="Test", source_type="TEST", adapter_type="TEST",
        )
        db.add(source)
        db.flush()
        db.add(StockSymbol(
            market="CN_A", symbol="000001", exchange="SZSE", name="平安银行",
            asset_type="STOCK", status="LISTED", source_id=source.id,
            ext_json={}, raw_payload={"source": "test"},
        ))
        db.add(AgentDataAsset(
            asset_code="STOCK_SYMBOL", table_name="stock_symbol", display_name="股票主数据",
            governance_status="GOVERNED", enabled=True,
        ))
        lake_object = LakeObject(
            object_uri="file:///tmp/raw/test.json", layer="RAW", content_hash="a" * 64,
            content_type="application/json", byte_size=2, source_table="stock_symbol",
            source_record_id="1", metadata_json={"origin": "test"},
        )
        db.add(lake_object)
        db.flush()
        db.add(DocumentChunkVersion(
            document_key="stock_symbol:1", document_id="1", chunk_index=0,
            chunk_version="v1", content_hash="b" * 64, chunk_text="测试切片",
            parser_version="STRUCTURE_V1", embedding_model="HASH_EMBED_V1",
            embedding_kind="HASH", embedding_status="HASH_ONLY",
            source_object_id=lake_object.id, lineage_batch_id="batch-test",
        ))
        db.add(LakeLineageEvent(
            batch_id="batch-test", upstream_type="LAKE_OBJECT", upstream_id=lake_object.id,
            downstream_type="DOCUMENT_CHUNK", downstream_id="chunk-test",
            transformation="STRUCTURE_CHUNK", metadata_json={"test": True},
        ))
        db.commit()
        with _client(db) as client:
            rule = client.post("/api/v1/governance/quality/rules", json={
                "rule_code": "STOCK_MASTER_REQUIRED", "rule_name": "股票主数据必填检查",
                "asset_scope": "SOURCE_TABLE", "target_code": "stock_symbol",
                "rule_type": "BUILTIN_CONTRACT", "lifecycle_status": "ENABLED", "enabled": True,
            })
            assert rule.status_code == 201
            run = client.post("/api/v1/governance/quality/runs", json={
                "target_type": "SOURCE_TABLE", "target_id": "stock_symbol",
                "layer": "NORMALIZED", "idempotency_key": "test-stock-quality-v1",
            })
            assert run.status_code == 200
            assert run.json()["status"] == "PASSED"
            repeated = client.post("/api/v1/governance/quality/runs", json={
                "target_type": "SOURCE_TABLE", "target_id": "stock_symbol",
                "layer": "NORMALIZED", "idempotency_key": "test-stock-quality-v1",
            })
            assert repeated.json()["id"] == run.json()["id"]
            detail = client.get(f"/api/v1/governance/quality/runs/{run.json()['id']}")
            assert detail.status_code == 200 and detail.json()["target_id"] == "stock_symbol"
            object_detail = client.get(f"/api/v1/lakehouse/objects/{lake_object.id}")
            assert object_detail.status_code == 200
            assert object_detail.json()["metadata"]["origin"] == "test"
            chunks = client.get("/api/v1/lakehouse/chunks", params={"document_id": "1"})
            assert chunks.status_code == 200
            assert chunks.json()[0]["embedding_kind"] == "HASH"
            assert chunks.json()[0]["embedding_status"] == "HASH_ONLY"
            lineage = client.get("/api/v1/lakehouse/lineage", params={
                "object_id": lake_object.id, "direction": "DOWNSTREAM",
            })
            assert lineage.status_code == 200 and len(lineage.json()) == 1


def test_controlled_agent_and_skill_execution_are_audited():
    with Session(_engine()) as db:
        seed_default_models(db)
        skill = ModelSkill(
            skill_code="TEST_DW_TOOL", skill_name="测试治理工具", instructions="确定性检查",
            skill_type="EXECUTABLE_TOOL", enabled=True, lifecycle_status="ENABLED",
            input_contract_json={"type": "object", "required": ["records"], "properties": {"records": {"type": "array"}}},
            output_contract_json={"type": "object"},
            permission_policy_json={"allowed_operations": ["READ_GOVERNED_DATA"]},
            side_effect_level="READ_ONLY", idempotency_policy="OPTIONAL",
            retry_policy_json={"max_attempts": 1}, error_policy_json={"on_error": "FAIL_CLOSED"},
            config_json={"function_spec": {"name": "validate_dw_records", "parameters": {"type": "object"}}},
        )
        db.add(skill)
        agent = AgentDefinition(
            agent_code="TEST_AGENT", display_name="测试智能体", system_prompt="只使用已有证据",
            model_instance_code="MOCK_GENERAL", enabled=True, lifecycle_status="ENABLED",
        )
        db.add(agent)
        db.commit()

        result = SkillToolWorkflow(db).execute("validate_dw_records", {"records": []})
        assert result["total_records"] == 0
        skill_run = db.scalar(select(SkillExecutionRun).where(SkillExecutionRun.skill_id == skill.id))
        assert skill_run is not None and skill_run.status == "SUCCESS"
        assert skill_run.write_scope_json["direct_sql"] is False

        response = AgentExecutionWorkflow(db).execute(AgentExecutionCommand(
            agent_id=agent.id, task_type="general_chat",
            messages=[{"role": "user", "content": "汇总已知信息"}],
        ))
        assert response.status == "SUCCESS"
        agent_run = db.scalar(select(AgentExecutionRun).where(AgentExecutionRun.agent_id == agent.id))
        assert agent_run is not None and agent_run.status == "SUCCESS"
        assert agent_run.model_call_log_id == response.call_log_id
        assert agent_run.audit_json["arbitrary_sql_allowed"] is False


def test_skill_contract_lifecycle_and_forbidden_sql_policy():
    with Session(_engine()) as db:
        skill = ModelSkill(
            skill_code="LIFECYCLE_TEST", skill_name="生命周期测试", instructions="只读测试",
            enabled=False, lifecycle_status="DRAFT",
            input_contract_json={"type": "object"}, output_contract_json={"type": "object"},
            permission_policy_json={"allowed_operations": ["READ_GOVERNED_DATA"]},
            side_effect_level="READ_ONLY", idempotency_policy="OPTIONAL",
            retry_policy_json={"max_attempts": 1}, error_policy_json={"on_error": "FAIL_CLOSED"},
        )
        db.add(skill)
        db.commit()
        with _client(db) as client:
            contract = client.get(f"/api/v1/model-hub/skills/{skill.id}/contract")
            assert contract.status_code == 200
            assert contract.json()["input_contract"] == {"type": "object"}
            testing = client.post(f"/api/v1/model-hub/skills/{skill.id}/lifecycle", json={
                "lifecycle_status": "TESTING", "reason": "离线验证",
            })
            assert testing.status_code == 200 and testing.json()["lifecycle_status"] == "TESTING"
            enabled = client.post(f"/api/v1/model-hub/skills/{skill.id}/lifecycle", json={
                "lifecycle_status": "ENABLED", "reason": "测试通过",
            })
            assert enabled.status_code == 200 and enabled.json()["enabled"] is True
            forbidden = client.post("/api/v1/model-hub/skills", json={
                "skill_code": "UNSAFE_SQL", "skill_name": "不安全写入", "instructions": "测试",
                "permission_policy_json": {"arbitrary_sql": True},
            })
            assert forbidden.status_code == 422


def _executable_test_skill(
    *,
    skill_code: str,
    side_effect_level: str = "READ_ONLY",
    permission_policy: dict | None = None,
    retry_policy: dict | None = None,
) -> ModelSkill:
    return ModelSkill(
        skill_code=skill_code,
        skill_name=f"{skill_code} test skill",
        instructions="Deterministic governance execution test.",
        skill_type="EXECUTABLE_TOOL",
        enabled=True,
        lifecycle_status="ENABLED",
        input_contract_json={
            "type": "object",
            "required": ["records"],
            "properties": {"records": {"type": "array"}},
        },
        output_contract_json={"type": "object"},
        permission_policy_json=permission_policy or {
            "allowed_operations": ["READ_GOVERNED_DATA"],
        },
        side_effect_level=side_effect_level,
        idempotency_policy="REQUIRED" if side_effect_level != "READ_ONLY" else "OPTIONAL",
        retry_policy_json=retry_policy or {"max_attempts": 1},
        error_policy_json={"on_error": "FAIL_CLOSED"},
        config_json={
            "function_spec": {
                "name": "validate_dw_records",
                "parameters": {"type": "object"},
            }
        },
    )


def test_skill_idempotency_reuses_same_successful_input_without_a_second_run():
    with Session(_engine()) as db:
        skill = _executable_test_skill(skill_code="IDEMPOTENT_SAME_INPUT")
        db.add(skill)
        db.commit()

        workflow = SkillToolWorkflow(db)
        first = workflow.execute(
            "validate_dw_records", {"records": []}, idempotency_key="skill:same-input:v1",
        )
        repeated = workflow.execute(
            "validate_dw_records", {"records": []}, idempotency_key="skill:same-input:v1",
        )

        assert repeated == first
        assert db.scalar(select(func.count()).select_from(SkillExecutionRun)) == 1
        run = db.scalar(select(SkillExecutionRun))
        assert run is not None
        assert run.status == "SUCCESS"
        assert run.attempt == 1


def test_skill_idempotency_rejects_same_key_with_different_input():
    with Session(_engine()) as db:
        skill = _executable_test_skill(skill_code="IDEMPOTENT_DIFFERENT_INPUT")
        db.add(skill)
        db.commit()

        workflow = SkillToolWorkflow(db)
        workflow.execute(
            "validate_dw_records", {"records": []}, idempotency_key="skill:conflict:v1",
        )

        with pytest.raises(ValueError, match="different Skill input"):
            workflow.execute(
                "validate_dw_records", {"records": [{}]}, idempotency_key="skill:conflict:v1",
            )
        assert db.scalar(select(func.count()).select_from(SkillExecutionRun)) == 1


def test_controlled_write_skill_requires_an_idempotency_key_before_dispatch():
    with Session(_engine()) as db:
        skill = _executable_test_skill(
            skill_code="CONTROLLED_WRITE_REQUIRES_KEY",
            side_effect_level="CONTROLLED_WRITE",
            permission_policy={
                "allowed_operations": ["WRITE_GOVERNED_DATA"],
                "allowed_services": ["foundation_fact_service"],
            },
        )
        db.add(skill)
        db.commit()

        with pytest.raises(PermissionError, match="require an idempotency key"):
            SkillToolWorkflow(db).execute("validate_dw_records", {"records": []})
        assert db.scalar(select(func.count()).select_from(SkillExecutionRun)) == 0


@pytest.mark.parametrize("forbidden_permission", ["arbitrary_sql", "unrestricted_database_write"])
def test_runtime_rejects_forbidden_database_permissions(forbidden_permission: str):
    with Session(_engine()) as db:
        skill = _executable_test_skill(
            skill_code=f"FORBIDDEN_{forbidden_permission.upper()}",
            permission_policy={
                "allowed_operations": ["READ_GOVERNED_DATA"],
                forbidden_permission: True,
            },
        )
        db.add(skill)
        db.commit()

        with pytest.raises(PermissionError, match="arbitrary SQL or unrestricted database writes"):
            SkillToolWorkflow(db).execute("validate_dw_records", {"records": []})
        assert db.scalar(select(func.count()).select_from(SkillExecutionRun)) == 0


def test_retrying_state_is_committed_before_a_retry_succeeds(monkeypatch):
    with Session(_engine()) as db:
        skill = _executable_test_skill(
            skill_code="RETRY_STATE_AUDIT",
            retry_policy={"max_attempts": 3, "retry_on": ["RuntimeError"]},
        )
        db.add(skill)
        db.commit()

        workflow = SkillToolWorkflow(db)
        dispatch_attempts = 0

        def flaky_dispatch(tool_code, context_data):
            nonlocal dispatch_attempts
            dispatch_attempts += 1
            if dispatch_attempts == 1:
                raise RuntimeError("temporary governed service failure")
            return {"status": "COMPLETED", "attempt": dispatch_attempts}

        committed_states: list[tuple[str, int]] = []
        original_commit = db.commit

        def tracking_commit():
            original_commit()
            run = db.scalar(select(SkillExecutionRun).where(SkillExecutionRun.skill_id == skill.id))
            if run is not None:
                committed_states.append((run.status, run.attempt))

        monkeypatch.setattr(workflow, "_dispatch", flaky_dispatch)
        monkeypatch.setattr(db, "commit", tracking_commit)

        result = workflow.execute(
            "validate_dw_records", {"records": []}, idempotency_key="skill:retry:v1",
        )

        assert result == {"status": "COMPLETED", "attempt": 2}
        assert dispatch_attempts == 2
        assert ("RETRYING", 1) in committed_states
        run = db.scalar(select(SkillExecutionRun).where(SkillExecutionRun.skill_id == skill.id))
        assert run is not None
        assert run.status == "SUCCESS"
        assert run.attempt == 2
        assert run.max_attempts == 3
        assert run.completed_at is not None
