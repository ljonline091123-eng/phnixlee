from __future__ import annotations

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient
from pydantic import ValidationError
from sqlalchemy import create_engine, func, select
from sqlalchemy.orm import Session
from sqlalchemy.pool import StaticPool

from app.api import governance_control, lakehouse, model_hub
from app.db.base import Base
from app.db.session import get_db
from app.models.ai_hub import AgentDataAsset, AgentDataSourceLink, AgentDefinition, ModelSkill, PredictionLedger
from app.models.governance import AgentExecutionRun, SkillExecutionRun
from app.models.lakehouse import (
    DocumentChunkVersion,
    LakeDataset,
    LakeDatasetVersion,
    LakeLineageEvent,
    LakeObject,
)
from app.models.market_data import DataSource, StockSymbol
from app.orchestration.model_hub import (
    AgentExecutionCommand,
    AgentExecutionWorkflow,
    AgentUnavailableError,
    SkillToolWorkflow,
)
from app.schemas.resource_hub import AgentCreate
from app.services.governance_defaults import seed_default_quality_rules
from app.services.model_hub import seed_default_models
from app.services.prediction_ledger import record_agent_predictions


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
            assert run.json()["total_rules"] > 8
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


def test_enabled_custom_quality_rule_participates_in_gate_and_creates_issue():
    with Session(_engine()) as db:
        source = DataSource(
            source_code="QUALITY_RULE_SOURCE", source_name="规则测试", source_type="TEST", adapter_type="TEST",
        )
        db.add(source)
        db.flush()
        db.add(StockSymbol(
            market="CN_A", symbol="000002", exchange="SZSE", name="万科A",
            asset_type="STOCK", status="LISTED", source_id=source.id,
            ext_json={}, raw_payload={"source": "test"},
        ))
        db.add(AgentDataAsset(
            asset_code="QUALITY_RULE_STOCK", table_name="stock_symbol", display_name="股票主数据",
            governance_status="GOVERNED", enabled=True,
        ))
        db.commit()

        with _client(db) as client:
            rule = client.post("/api/v1/governance/quality/rules", json={
                "rule_code": "CUSTOM_REQUIRED_FIELD",
                "rule_name": "自定义字段完整性",
                "asset_scope": "SOURCE_TABLE",
                "target_code": "stock_symbol",
                "rule_type": "COMPLETENESS",
                "severity": "ERROR",
                "definition_json": {"evaluator": "REQUIRED_FIELDS", "fields": ["missing_custom_field"]},
                "lifecycle_status": "ENABLED",
                "enabled": True,
            })
            assert rule.status_code == 201, rule.text
            run = client.post("/api/v1/governance/quality/runs", json={
                "target_type": "SOURCE_TABLE",
                "target_id": "stock_symbol",
                "layer": "NORMALIZED",
                "idempotency_key": "custom-quality-rule-v1",
            })
            assert run.status_code == 200, run.text
            assert run.json()["status"] == "FAILED"
            detail = client.get(f"/api/v1/governance/quality/runs/{run.json()['id']}")
            assert detail.status_code == 200
            custom_issue = next(item for item in detail.json()["issues"] if item["rule_code"] == "CUSTOM_REQUIRED_FIELD")
            assert custom_issue["rule_id"] == rule.json()["id"]
            assert custom_issue["issue_code"] == "QUALITY_RULE_FAILED"


def test_default_quality_rules_have_executable_targets_and_evaluators():
    with Session(_engine()) as db:
        lake_object = LakeObject(
            object_uri="file:///tmp/raw/governed.json",
            layer="RAW",
            content_hash="a" * 64,
            content_type="application/json",
            byte_size=2,
            source_code="TEST_SOURCE",
            source_table="stock_news",
            source_record_id="news-1",
            metadata_json={"origin": "test"},
        )
        db.add(lake_object)
        db.flush()
        chunk = DocumentChunkVersion(
            id="chunk-governed",
            document_key="knowledge_document:1",
            document_id="1",
            chunk_index=0,
            chunk_version="v1",
            content_hash="b" * 64,
            chunk_text="受治理切片",
            parser_version="STRUCTURE_V1",
            embedding_model="HASH_EMBED_V1",
            embedding_kind="HASH",
            embedding_status="HASH_ONLY",
            section_status="NOT_DETECTED",
            source_object_id=lake_object.id,
            lineage_batch_id="batch-governed",
        )
        db.add(chunk)
        db.add(LakeLineageEvent(
            batch_id="batch-source",
            upstream_type="SOURCE_RECORD",
            upstream_id="stock_news:news-1",
            downstream_type="LAKE_OBJECT",
            downstream_id=lake_object.id,
            transformation="RAW_ARCHIVE",
            metadata_json={"test": True},
        ))
        db.add(LakeLineageEvent(
            batch_id="batch-governed",
            upstream_type="LAKE_OBJECT",
            upstream_id=lake_object.id,
            downstream_type="DOCUMENT_CHUNK",
            downstream_id=chunk.id,
            transformation="STRUCTURE_CHUNK",
            metadata_json={"test": True},
        ))
        db.add(ModelSkill(
            skill_code="QUALITY_CONTRACT_SKILL",
            skill_name="质量契约测试 Skill",
            instructions="只读分析。",
            enabled=True,
            lifecycle_status="ENABLED",
            input_contract_json={"type": "object"},
            output_contract_json={"type": "object"},
            permission_policy_json={
                "arbitrary_sql": False,
                "database_write": False,
                "allowed_operations": ["READ_GOVERNED_DATA"],
            },
            side_effect_level="READ_ONLY",
            idempotency_policy="OPTIONAL",
            retry_policy_json={"max_attempts": 1},
            error_policy_json={"on_error": "FAIL_CLOSED"},
        ))
        db.commit()
        seed_default_quality_rules(db)

        with _client(db) as client:
            cases = (
                ("LAKE_OBJECT", "lake_object", "lake-object-quality-v1"),
                ("DOCUMENT_CHUNK", "document_chunk_version", "chunk-quality-v1"),
                ("MODEL_SKILL", "model_skill", "skill-quality-v1"),
            )
            for target_type, target_id, key in cases:
                response = client.post("/api/v1/governance/quality/runs", json={
                    "target_type": target_type,
                    "target_id": target_id,
                    "idempotency_key": key,
                })
                assert response.status_code == 200, response.text
                payload = response.json()
                assert payload["status"] == "PASSED", payload
                configured = payload["summary"]["configured_rules"]
                assert configured
                assert all(item["passed"] for item in configured), configured
                assert all("configuration_error" not in item["details"] for item in configured)


def test_chunk_quality_fails_closed_for_missing_lineage_and_false_vector_label():
    with Session(_engine()) as db:
        db.add(DocumentChunkVersion(
            id="chunk-invalid",
            document_key="knowledge_document:missing",
            document_id="missing",
            chunk_index=0,
            chunk_version="v1",
            content_hash="c" * 64,
            chunk_text="缺少来源的切片",
            parser_version="STRUCTURE_V1",
            embedding_model="HASH_EMBED_V1",
            embedding_kind="SEMANTIC",
            embedding_status="READY",
            section_status="UNRESOLVED",
        ))
        db.commit()
        seed_default_quality_rules(db)

        with _client(db) as client:
            response = client.post("/api/v1/governance/quality/runs", json={
                "target_type": "DOCUMENT_CHUNK",
                "target_id": "chunk-invalid",
                "idempotency_key": "chunk-invalid-quality-v1",
            })
            assert response.status_code == 200, response.text
            assert response.json()["status"] == "FAILED"
            detail = client.get(f"/api/v1/governance/quality/runs/{response.json()['id']}").json()
            failed_codes = {item["rule_code"] for item in detail["issues"]}
            assert "CHUNK_LINEAGE_REQUIRED" in failed_codes
            assert "HASH_VECTOR_TRUTHFUL" in failed_codes


def test_enabled_quality_rule_rejects_unexecutable_evaluator():
    with Session(_engine()) as db:
        with _client(db) as client:
            response = client.post("/api/v1/governance/quality/rules", json={
                "rule_code": "UNEXECUTABLE_RULE",
                "rule_name": "不可执行规则",
                "asset_scope": "SOURCE_TABLE",
                "target_code": "stock_symbol",
                "rule_type": "UNKNOWN_EVALUATOR",
                "lifecycle_status": "ENABLED",
                "enabled": True,
            })
            assert response.status_code == 422
            assert "Unsupported quality evaluator" in response.json()["detail"]


def test_legacy_dataset_without_quality_snapshot_is_partial_not_false_complete():
    with Session(_engine()) as db:
        dataset = LakeDataset(
            dataset_code="legacy_dataset",
            dataset_name="历史数据集",
            layer="NORMALIZED",
            format="PARQUET",
            current_version="legacy-v1",
        )
        db.add(dataset)
        db.flush()
        db.add(LakeDatasetVersion(
            dataset_id=dataset.id,
            version="legacy-v1",
            row_count=10,
            schema_json={"id": "INTEGER"},
            quality_json={},
            status="PUBLISHED",
        ))
        db.commit()
        seed_default_quality_rules(db)

        with _client(db) as client:
            response = client.post("/api/v1/governance/quality/runs", json={
                "target_type": "LAKE_DATASET_VERSION",
                "target_id": "legacy_dataset",
                "target_version": "legacy-v1",
                "idempotency_key": "legacy-dataset-quality-v1",
            })
            assert response.status_code == 200, response.text
            payload = response.json()
            assert payload["target_id"] == "legacy_dataset"
            assert payload["status"] == "PARTIAL"
            detail = client.get(f"/api/v1/governance/quality/runs/{payload['id']}").json()
            assert any(item["severity"] == "WARNING" for item in detail["issues"])


def test_controlled_agent_and_skill_execution_are_audited():
    with Session(_engine()) as db:
        seed_default_models(db)
        source = DataSource(
            source_code="MANIFEST_SOURCE",
            source_name="清单测试数据源",
            source_type="TEST",
            adapter_type="TEST",
        )
        db.add(source)
        db.flush()
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
        db.flush()
        db.add(AgentDataSourceLink(agent_id=agent.id, data_source_id=source.id))
        db.commit()

        with _client(db) as client:
            manifest = client.get(f"/api/v1/governance/agents/{agent.id}/manifest")
            assert manifest.status_code == 200
            assert manifest.json()["data_sources"] == [{
                "id": source.id,
                "source_code": "MANIFEST_SOURCE",
                "source_name": "清单测试数据源",
                "source_type": "TEST",
                "enabled": True,
            }]

        result = SkillToolWorkflow(db).execute("validate_dw_records", {"records": []})
        assert result["total_records"] == 0
        skill_run = db.scalar(select(SkillExecutionRun).where(SkillExecutionRun.skill_id == skill.id))
        assert skill_run is not None and skill_run.status == "SUCCESS"
        assert skill_run.write_scope_json["direct_sql"] is False

        response = AgentExecutionWorkflow(db).execute(AgentExecutionCommand(
            agent_id=agent.id, task_type="general_chat",
            evidence_ids=["evidence-task1"], correlation_id="corr-task1",
            idempotency_key="agent-task1", request_source="OFFLINE_EVAL",
            messages=[{"role": "user", "content": "汇总已知信息"}],
        ))
        assert response.status == "SUCCESS"
        agent_run = db.scalar(select(AgentExecutionRun).where(AgentExecutionRun.agent_id == agent.id))
        assert agent_run is not None and agent_run.status == "SUCCESS"
        assert agent_run.model_call_log_id == response.call_log_id
        assert agent_run.audit_json["arbitrary_sql_allowed"] is False
        assert agent_run.audit_json["request_source"] == "OFFLINE_EVAL"
        assert agent_run.evidence_ids_json == ["evidence-task1"]
        repeated = AgentExecutionWorkflow(db).execute(AgentExecutionCommand(
            agent_id=agent.id, task_type="general_chat",
            messages=agent_run.input_json["messages"],
            evidence_ids=["evidence-task1"], correlation_id="corr-task1",
            idempotency_key="agent-task1", request_source="OFFLINE_EVAL",
        ))
        assert repeated.call_log_id == response.call_log_id
        assert db.scalar(select(func.count()).select_from(AgentExecutionRun)) == 1


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


def test_skill_cannot_enable_until_all_governance_contracts_are_complete():
    with Session(_engine()) as db:
        skill = ModelSkill(
            skill_code="INCOMPLETE_LIFECYCLE",
            skill_name="契约不完整技能",
            instructions="尚未配置重试和错误策略。",
            enabled=False,
            lifecycle_status="TESTING",
            input_contract_json={"type": "object"},
            output_contract_json={"type": "object"},
            permission_policy_json={"allowed_operations": ["READ_GOVERNED_DATA"]},
            side_effect_level="READ_ONLY",
            retry_policy_json={},
            error_policy_json={},
        )
        db.add(skill)
        db.commit()

        with _client(db) as client:
            response = client.post(f"/api/v1/model-hub/skills/{skill.id}/lifecycle", json={
                "lifecycle_status": "ENABLED",
                "reason": "不完整契约不得启用",
            })
            assert response.status_code == 422
            assert "complete governance contracts" in response.json()["detail"]


def test_skill_revision_rollback_restores_complete_governance_contract(tmp_path, monkeypatch):
    monkeypatch.setattr("app.services.skill_files.SKILL_ROOT", tmp_path / "skill_docs")
    with Session(_engine()) as db:
        with _client(db) as client:
            created = client.post("/api/v1/model-hub/skills", json={
                "skill_code": "FULL_CONTRACT_ROLLBACK",
                "skill_name": "完整契约回滚",
                "instructions": "保持同一段指令，验证纯契约变更也会形成版本。",
                "lifecycle_status": "DRAFT",
                "input_contract_json": {"type": "object", "properties": {"version": {"const": 1}}},
                "output_contract_json": {"type": "object"},
                "permission_policy_json": {"allowed_operations": ["READ_GOVERNED_DATA"]},
                "side_effect_level": "READ_ONLY",
                "idempotency_policy": "OPTIONAL",
                "retry_policy_json": {"max_attempts": 1},
                "error_policy_json": {"on_error": "FAIL_CLOSED"},
                "config_json": {"contract_mode": "v1"},
            })
            assert created.status_code == 201, created.text
            skill_id = created.json()["id"]

            updated = client.put(f"/api/v1/model-hub/skills/{skill_id}", json={
                "input_contract_json": {"type": "object", "properties": {"version": {"const": 2}}},
                "permission_policy_json": {"allowed_operations": ["READ_GOVERNED_DATA", "READ_KNOWLEDGE"]},
                "retry_policy_json": {"max_attempts": 2},
                "config_json": {"contract_mode": "v2"},
            })
            assert updated.status_code == 200, updated.text
            assert updated.json()["version"] == "1.0.1"

            revisions = client.get(f"/api/v1/model-hub/skills/{skill_id}/revisions")
            assert revisions.status_code == 200
            original = next(item for item in revisions.json() if item["version"] == "1.0.0")
            assert original["governance_json"]["config_json"]["contract_mode"] == "v1"

            rolled = client.post(
                f"/api/v1/model-hub/skills/{skill_id}/revisions/{original['id']}/rollback"
            )
            assert rolled.status_code == 200, rolled.text
            payload = rolled.json()
            assert payload["version"] == "1.0.2"
            assert payload["input_contract_json"]["properties"]["version"]["const"] == 1
            assert payload["permission_policy_json"]["allowed_operations"] == ["READ_GOVERNED_DATA"]
            assert payload["retry_policy_json"] == {"max_attempts": 1}
            assert payload["config_json"]["contract_mode"] == "v1"


def test_new_skill_defaults_to_non_executable_draft(tmp_path, monkeypatch):
    monkeypatch.setattr("app.services.skill_files.SKILL_ROOT", tmp_path / "skill_docs")
    with Session(_engine()) as db:
        with _client(db) as client:
            response = client.post("/api/v1/model-hub/skills", json={
                "skill_code": "DRAFT_NOT_EXECUTABLE",
                "skill_name": "草稿不可执行",
                "instructions": "等待完成契约和离线测试。",
            })
            assert response.status_code == 201, response.text
            assert response.json()["lifecycle_status"] == "DRAFT"
            assert response.json()["enabled"] is False


def test_runtime_rejects_draft_skill_even_if_legacy_enabled_flag_is_true():
    with Session(_engine()) as db:
        skill = _executable_test_skill(skill_code="LEGACY_ENABLED_DRAFT")
        skill.lifecycle_status = "DRAFT"
        skill.enabled = True
        db.add(skill)
        db.commit()

        with pytest.raises(LookupError, match="not enabled"):
            SkillToolWorkflow(db).execute("validate_dw_records", {"records": []})


def test_runtime_rejects_draft_agent_even_if_legacy_enabled_flag_is_true():
    with Session(_engine()) as db:
        seed_default_models(db)
        agent = AgentDefinition(
            agent_code="DRAFT_AGENT",
            display_name="草稿智能体",
            system_prompt="尚未启用。",
            model_instance_code="MOCK_GENERAL",
            enabled=True,
            lifecycle_status="DRAFT",
        )
        db.add(agent)
        db.commit()

        with pytest.raises(AgentUnavailableError, match="Enabled agent not found"):
            AgentExecutionWorkflow(db).execute(AgentExecutionCommand(
                agent_id=agent.id,
                task_type="general_chat",
                messages=[{"role": "user", "content": "不应执行"}],
            ))


def test_agent_contract_rejects_unknown_lifecycle_status():
    with pytest.raises(ValidationError):
        AgentCreate(display_name="非法状态智能体", lifecycle_status="UNKNOWN")


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


def _prediction_writer_skill(*, include_scope: bool = True) -> ModelSkill:
    permission_policy = {
        "arbitrary_sql": False,
        "database_write": True,
        "unrestricted_database_write": False,
        "allowed_operations": ["CREATE_PREDICTION_LEDGER"],
        "allowed_services": ["prediction_ledger_service"],
    }
    if include_scope:
        permission_policy["write_scope"] = {
            "tables": ["prediction_ledger"],
            "operations": ["INSERT"],
        }
    return ModelSkill(
        skill_code="TEST_PREDICTION_WRITER" if include_scope else "TEST_UNSCOPED_WRITER",
        skill_name="Prediction writer test",
        instructions="Persist validated candidates only.",
        skill_type="EXECUTABLE_TOOL",
        enabled=True,
        lifecycle_status="ENABLED",
        input_contract_json={"type": "object"},
        output_contract_json={"type": "object"},
        permission_policy_json=permission_policy,
        side_effect_level="CONTROLLED_WRITE",
        idempotency_policy="REQUIRED",
        retry_policy_json={"max_attempts": 1},
        error_policy_json={"on_error": "FAIL_CLOSED"},
    )


def _prediction_json() -> str:
    return (
        '{"prediction":{"market":"CN_A","stock_code":"000001","action_type":"BUY",'
        '"target_timeframe":"T+5","reasoning_logic":"Evidence-linked test",'
        '"skill_code":"TEST_ANALYSIS_SKILL","entry_price":10.0}}'
    )


def test_prediction_write_requires_explicit_scope_and_idempotency_key():
    with Session(_engine()) as db:
        analysis_skill = ModelSkill(
            skill_code="TEST_ANALYSIS_SKILL", skill_name="Analysis", instructions="Analyze only",
            enabled=True, lifecycle_status="ENABLED", side_effect_level="READ_ONLY",
        )
        scoped_writer = _prediction_writer_skill()
        unscoped_writer = _prediction_writer_skill(include_scope=False)
        db.add_all([analysis_skill, scoped_writer, unscoped_writer])
        db.commit()

        missing_key = record_agent_predictions(
            db, _prediction_json(), allowed_skill_codes={analysis_skill.skill_code},
            model_instance_code="TEST_MODEL", writer_skill=scoped_writer,
        )
        assert missing_key.status == "CANDIDATE_ONLY"
        assert missing_key.reason == "IDEMPOTENCY_KEY_REQUIRED"

        missing_scope = record_agent_predictions(
            db, _prediction_json(), allowed_skill_codes={analysis_skill.skill_code},
            model_instance_code="TEST_MODEL", writer_skill=unscoped_writer,
            idempotency_key="prediction-unscoped-v1",
        )
        assert missing_scope.status == "CANDIDATE_ONLY"
        assert missing_scope.reason == "PREDICTION_WRITE_SCOPE_NOT_DECLARED"
        assert db.scalar(select(func.count()).select_from(PredictionLedger)) == 0
        assert db.scalar(select(func.count()).select_from(SkillExecutionRun)) == 0


def test_prediction_controlled_write_is_audited_and_idempotent():
    with Session(_engine()) as db:
        analysis_skill = ModelSkill(
            skill_code="TEST_ANALYSIS_SKILL", skill_name="Analysis", instructions="Analyze only",
            enabled=True, lifecycle_status="ENABLED", side_effect_level="READ_ONLY",
        )
        writer = _prediction_writer_skill()
        db.add_all([analysis_skill, writer])
        db.commit()

        first = record_agent_predictions(
            db, _prediction_json(), allowed_skill_codes={analysis_skill.skill_code},
            model_instance_code="TEST_MODEL", writer_skill=writer,
            idempotency_key="prediction-controlled-v1", evidence_ids=["doc-1"],
        )
        repeated = record_agent_predictions(
            db, _prediction_json(), allowed_skill_codes={analysis_skill.skill_code},
            model_instance_code="TEST_MODEL", writer_skill=writer,
            idempotency_key="prediction-controlled-v1", evidence_ids=["doc-1"],
        )

        assert first.status == "PERSISTED"
        assert repeated.status == "PERSISTED_REUSED"
        assert repeated.prediction_ids == first.prediction_ids
        assert db.scalar(select(func.count()).select_from(PredictionLedger)) == 1
        assert db.scalar(select(func.count()).select_from(SkillExecutionRun)) == 1
        run = db.scalar(select(SkillExecutionRun))
        assert run.status == "SUCCESS"
        assert run.write_scope_json["tables"] == ["prediction_ledger"]
        assert run.write_scope_json["database_operations"] == ["INSERT"]
        assert run.write_scope_json["direct_sql"] is False
        assert run.evidence_ids_json == ["doc-1"]

        with pytest.raises(ValueError, match="different input"):
            record_agent_predictions(
                db, _prediction_json(), allowed_skill_codes={analysis_skill.skill_code},
                model_instance_code="TEST_MODEL", writer_skill=writer,
                idempotency_key="prediction-controlled-v1", evidence_ids=["doc-2"],
            )

        run.status = "FAILED"
        db.commit()
        non_reusable = record_agent_predictions(
            db, _prediction_json(), allowed_skill_codes={analysis_skill.skill_code},
            model_instance_code="TEST_MODEL", writer_skill=writer,
            idempotency_key="prediction-controlled-v1", evidence_ids=["doc-1"],
        )
        assert non_reusable.status == "CANDIDATE_ONLY"
        assert non_reusable.reason == "IDEMPOTENT_WRITE_NOT_REUSABLE"


def test_prediction_cannot_cite_the_controlled_writer_as_its_analysis_skill():
    with Session(_engine()) as db:
        writer = _prediction_writer_skill()
        db.add(writer)
        db.commit()
        response_text = _prediction_json().replace("TEST_ANALYSIS_SKILL", writer.skill_code)

        with pytest.raises(ValueError, match="analysis Skill"):
            record_agent_predictions(
                db, response_text, allowed_skill_codes={writer.skill_code},
                model_instance_code="TEST_MODEL", writer_skill=writer,
                idempotency_key="writer-cannot-analyze-v1",
            )
        assert db.scalar(select(func.count()).select_from(PredictionLedger)) == 0
        assert db.scalar(select(func.count()).select_from(SkillExecutionRun)) == 0


@pytest.mark.parametrize(
    ("permission_policy", "idempotency_policy", "expected_detail"),
    [
        (
            {"allowed_operations": ["CREATE_PREDICTION_LEDGER"]},
            "REQUIRED",
            "database_write=true",
        ),
        (
            {
                "database_write": True,
                "allowed_operations": ["CREATE_PREDICTION_LEDGER"],
                "write_scope": {"tables": ["prediction_ledger"], "operations": ["INSERT"]},
            },
            "REQUIRED",
            "allowed_services",
        ),
        (
            {
                "database_write": True,
                "allowed_operations": ["CREATE_PREDICTION_LEDGER"],
                "allowed_services": ["prediction_ledger_service"],
            },
            "REQUIRED",
            "write_scope.tables and write_scope.operations",
        ),
        (
            {
                "database_write": True,
                "allowed_operations": ["CREATE_PREDICTION_LEDGER"],
                "allowed_services": ["prediction_ledger_service"],
                "write_scope": {"tables": ["prediction_ledger"], "operations": ["INSERT"]},
            },
            "OPTIONAL",
            "idempotency_policy=REQUIRED",
        ),
    ],
)
def test_controlled_database_write_skill_contract_must_declare_scope_and_idempotency(
    permission_policy: dict, idempotency_policy: str, expected_detail: str,
):
    with Session(_engine()) as db:
        with _client(db) as client:
            response = client.post("/api/v1/model-hub/skills", json={
                "skill_code": "UNSCOPED_DATABASE_WRITER",
                "skill_name": "Unscoped database writer",
                "instructions": "Must not become executable.",
                "skill_type": "EXECUTABLE_TOOL",
                "config_json": {
                    "function_spec": {
                        "name": "unscoped_database_writer",
                        "parameters": {"type": "object"},
                    },
                },
                "input_contract_json": {"type": "object"},
                "output_contract_json": {"type": "object"},
                "permission_policy_json": permission_policy,
                "side_effect_level": "CONTROLLED_WRITE",
                "idempotency_policy": idempotency_policy,
                "retry_policy_json": {"max_attempts": 1},
                "error_policy_json": {"on_error": "FAIL_CLOSED"},
            })

        assert response.status_code == 422
        assert expected_detail in response.json()["detail"]


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
