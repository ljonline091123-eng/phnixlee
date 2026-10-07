from __future__ import annotations

import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

from sqlalchemy import create_engine, inspect, select, text
from sqlalchemy.orm import sessionmaker

from app.db.base import Base
from app.db.migrations import ensure_compat_columns
from app.models.ai_hub import AgentDefinition, AgentDataAsset, ModelSkill
from app.models.governance import (
    AgentExecutionRun,
    DataQualityIssue,
    DataQualityRule,
    DataQualityRun,
    SkillExecutionRun,
)
from app.models.lakehouse import DocumentChunkVersion
from app.models.pipeline import PipelineStageRun, PipelineRun


class Task1GovernanceModelTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = TemporaryDirectory(prefix="task1-governance-")
        self.engine = create_engine(
            f"sqlite:///{Path(self.temp.name) / 'governance.db'}",
            connect_args={"check_same_thread": False},
        )
        ensure_compat_columns(self.engine)
        Base.metadata.create_all(self.engine)
        self.db = sessionmaker(bind=self.engine)()

    def tearDown(self) -> None:
        self.db.close()
        self.engine.dispose()
        self.temp.cleanup()

    def test_new_governance_tables_and_fields_are_registered(self) -> None:
        names = set(inspect(self.engine).get_table_names())
        self.assertTrue({
            "data_quality_rule", "data_quality_run", "data_quality_issue",
            "agent_execution_run", "skill_execution_run",
        }.issubset(names))
        self.assertTrue({
            "lifecycle_status", "input_contract_json", "output_contract_json",
            "permission_policy_json", "side_effect_level", "idempotency_policy",
            "retry_policy_json", "error_policy_json",
        }.issubset({column["name"] for column in inspect(self.engine).get_columns("model_skill")}))
        self.assertTrue({
            "section_title", "section_path_json", "section_status", "embedding_status",
            "embedding_kind", "source_object_id", "lineage_batch_id",
        }.issubset({column["name"] for column in inspect(self.engine).get_columns("document_chunk_version")}))

    def test_quality_and_execution_records_keep_contract_and_evidence(self) -> None:
        agent = AgentDefinition(
            agent_code="TASK1_TEST_AGENT", display_name="Task1 test", version="1.0.0",
            lifecycle_status="ENABLED", policy_json={"sql": "DENY"},
        )
        skill = ModelSkill(
            skill_code="TASK1_TEST_SKILL", skill_name="Task1 test", instructions="test",
            version="1.0.0", lifecycle_status="ENABLED",
            input_contract_json={"type": "object"}, output_contract_json={"type": "object"},
            permission_policy_json={"assets": ["stock_news"]}, side_effect_level="READ_ONLY",
        )
        self.db.add_all([agent, skill])
        self.db.flush()
        quality_rule = DataQualityRule(
            rule_code="DOC_CHUNK_SECTION", rule_name="章节状态", asset_scope="DOCUMENT_CHUNK",
            rule_type="COMPLETENESS", severity="WARN", lifecycle_status="ENABLED", enabled=True,
            definition_json={"field": "section_title"},
        )
        pipeline = PipelineRun(pipeline_type="TASK1_TEST", trigger_type="TEST")
        self.db.add_all([quality_rule, pipeline])
        self.db.flush()
        quality_run = DataQualityRun(
            run_key="task1-quality-1", target_type="DATASET", target_id="stock_news",
            target_version="v1", pipeline_run_id=pipeline.id, status="PARTIAL",
            total_rules=1, warning_rules=1,
        )
        self.db.add(quality_run)
        self.db.flush()
        issue = DataQualityIssue(
            quality_run_id=quality_run.id, rule_id=quality_rule.id,
            rule_code=quality_rule.rule_code, target_type="DOCUMENT_CHUNK",
            target_id="chunk-1", issue_code="MISSING_SECTION", severity="WARN",
            message="章节未识别", evidence_ids_json=["doc-1"],
        )
        self.db.add(issue)
        execution = AgentExecutionRun(
            run_key="task1-agent-1", agent_id=agent.id, agent_code=agent.agent_code,
            agent_version=agent.version, status="SUCCEEDED", input_hash="a" * 64,
            output_hash="b" * 64, skill_versions_json={skill.skill_code: skill.version},
            evidence_ids_json=["doc-1"],
        )
        self.db.add(execution)
        self.db.flush()
        skill_execution = SkillExecutionRun(
            agent_execution_run_id=execution.id, skill_id=skill.id,
            skill_code=skill.skill_code, skill_version=skill.version, status="SUCCEEDED",
            idempotency_key="task1-skill-1", side_effect_level="READ_ONLY",
            write_scope_json={"tables": []}, evidence_ids_json=["doc-1"],
        )
        self.db.add(skill_execution)
        self.db.commit()
        self.assertEqual(self.db.scalar(select(DataQualityIssue).where(DataQualityIssue.id == issue.id)).message, "章节未识别")
        self.assertEqual(self.db.scalar(select(SkillExecutionRun).where(SkillExecutionRun.id == skill_execution.id)).skill_version, "1.0.0")

    def test_compat_migration_is_idempotent_and_marks_hash_vectors(self) -> None:
        # Simulate a pre-task1 database containing only legacy tables/columns.
        legacy_engine = create_engine(f"sqlite:///{Path(self.temp.name) / 'legacy.db'}")
        with legacy_engine.begin() as connection:
            connection.execute(text("CREATE TABLE model_skill (id INTEGER PRIMARY KEY, enabled BOOLEAN NOT NULL DEFAULT 1)"))
            connection.execute(text("INSERT INTO model_skill (id, enabled) VALUES (1, 1)"))
            connection.execute(text("CREATE TABLE agent_definition (id INTEGER PRIMARY KEY, enabled BOOLEAN NOT NULL DEFAULT 1)"))
            connection.execute(text("INSERT INTO agent_definition (id, enabled) VALUES (1, 1)"))
            connection.execute(text(
                "CREATE TABLE document_chunk_version (id VARCHAR(36) PRIMARY KEY, embedding_model VARCHAR(128))"
            ))
            connection.execute(text("INSERT INTO document_chunk_version (id, embedding_model) VALUES ('c1', 'HASH_EMBED_V1')"))
        ensure_compat_columns(legacy_engine)
        ensure_compat_columns(legacy_engine)
        with legacy_engine.connect() as connection:
            self.assertEqual(connection.execute(text("SELECT lifecycle_status FROM model_skill WHERE id = 1")).scalar_one(), "ENABLED")
            self.assertEqual(connection.execute(text("SELECT lifecycle_status FROM agent_definition WHERE id = 1")).scalar_one(), "ENABLED")
            self.assertEqual(connection.execute(text("SELECT embedding_kind, embedding_status FROM document_chunk_version WHERE id = 'c1'")).one(), ("HASH", "HASH_ONLY"))
        legacy_engine.dispose()


if __name__ == "__main__":
    unittest.main()
