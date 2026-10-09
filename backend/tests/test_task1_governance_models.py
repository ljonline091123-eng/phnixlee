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
from app.services.model_hub import seed_default_skills
from scripts.backfill_task1_chunk_metadata import backfill as backfill_chunk_metadata


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
            "agent_execution_run", "skill_execution_run", "stock_governance_detail",
        }.issubset(names))
        self.assertTrue({"governance_mode", "governance_batch_id"}.issubset(
            {column["name"] for column in inspect(self.engine).get_columns("pipeline_run")}
        ))
        self.assertTrue({
            "governance_mode", "governance_batch_id", "agent_execution_run_id",
            "skill_execution_run_id",
        }.issubset({column["name"] for column in inspect(self.engine).get_columns("data_fetch_log")}))
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
            connection.execute(text(
                "CREATE TABLE model_skill_revision (id INTEGER PRIMARY KEY, skill_id INTEGER, version VARCHAR(32), "
                "instructions TEXT, content_hash VARCHAR(128), source VARCHAR(16), created_at DATETIME)"
            ))
            connection.execute(text("CREATE TABLE agent_definition (id INTEGER PRIMARY KEY, enabled BOOLEAN NOT NULL DEFAULT 1)"))
            connection.execute(text("INSERT INTO agent_definition (id, enabled) VALUES (1, 1)"))
            connection.execute(text(
                "CREATE TABLE document_chunk_version (id VARCHAR(36) PRIMARY KEY, embedding_model VARCHAR(128))"
            ))
            connection.execute(text("INSERT INTO document_chunk_version (id, embedding_model) VALUES ('c1', 'HASH_EMBED_V1')"))
            connection.execute(text(
                "CREATE TABLE lake_lineage_event (id INTEGER PRIMARY KEY, batch_id VARCHAR(64), "
                "upstream_id VARCHAR(256), downstream_type VARCHAR(64), downstream_id VARCHAR(256))"
            ))
            connection.execute(text(
                "INSERT INTO lake_lineage_event "
                "(id, batch_id, upstream_id, downstream_type, downstream_id) "
                "VALUES (1, 'batch-1', 'object-1', 'DOCUMENT_CHUNK', 'c1')"
            ))
            # A table inspected after document_chunk_version reproduces the
            # former SQLite self-lock when migration inspection used a second
            # engine connection inside the write transaction.
            connection.execute(text("CREATE TABLE knowledge_document (id INTEGER PRIMARY KEY)"))
        ensure_compat_columns(legacy_engine)
        with legacy_engine.begin() as connection:
            connection.execute(text(
                "UPDATE document_chunk_version SET embedding_kind = 'NONE', embedding_status = 'MISSING' "
                "WHERE id = 'c1'"
            ))
        ensure_compat_columns(legacy_engine)
        with legacy_engine.connect() as connection:
            self.assertEqual(connection.execute(text("SELECT lifecycle_status FROM model_skill WHERE id = 1")).scalar_one(), "ENABLED")
            self.assertEqual(connection.execute(text("SELECT lifecycle_status FROM agent_definition WHERE id = 1")).scalar_one(), "ENABLED")
            self.assertEqual(
                connection.execute(text(
                    "SELECT embedding_kind, embedding_status, source_object_id, lineage_batch_id "
                    "FROM document_chunk_version WHERE id = 'c1'"
                )).one(),
                ("HASH", "HASH_ONLY", "object-1", "batch-1"),
            )
            indexes = {row[1] for row in connection.execute(text("PRAGMA index_list('lake_lineage_event')"))}
            self.assertIn("ix_lake_lineage_event_downstream_lookup", indexes)
            revision_columns = {row[1] for row in connection.execute(text("PRAGMA table_info(model_skill_revision)"))}
            self.assertIn("governance_json", revision_columns)
        legacy_engine.dispose()

    def test_chunk_backfill_preserves_legacy_hash_array_as_fingerprint(self) -> None:
        chunk = DocumentChunkVersion(
            document_key="task1:legacy-hash",
            document_id="doc-1",
            chunk_index=0,
            chunk_version="v1",
            content_hash="a" * 64,
            chunk_text="legacy hash payload",
            start_offset=0,
            end_offset=19,
            parser_version="test-v1",
            embedding_model="HASH_EMBED_V1",
            metadata_json={"embedding": [0.25, 0.75]},
        )
        self.db.add(chunk)
        self.db.commit()

        result = backfill_chunk_metadata(self.db)
        self.db.refresh(chunk)

        self.assertEqual(result["embedding_status_updated"], 1)
        self.assertEqual(chunk.embedding_kind, "HASH")
        self.assertEqual(chunk.embedding_status, "HASH_ONLY")
        self.assertEqual(chunk.metadata_json["embedding"]["legacy_hash_fingerprint"], [0.25, 0.75])
        self.assertNotIn("vector", chunk.metadata_json["embedding"])

    def test_every_seeded_skill_has_complete_governance_contract(self) -> None:
        seed_default_skills(self.db)
        skills = list(self.db.scalars(select(ModelSkill)).all())
        self.assertTrue(skills)
        for skill in skills:
            with self.subTest(skill=skill.skill_code):
                self.assertTrue(skill.version)
                self.assertTrue(skill.input_contract_json)
                self.assertTrue(skill.output_contract_json)
                self.assertTrue(skill.permission_policy_json)
                self.assertTrue(skill.side_effect_level)
                self.assertTrue(skill.idempotency_policy)
                self.assertTrue(skill.retry_policy_json)
                self.assertTrue(skill.error_policy_json)
                self.assertFalse(skill.permission_policy_json.get("arbitrary_sql"))
                self.assertFalse(skill.permission_policy_json.get("unrestricted_database_write"))

    def test_postgresql_migration_does_not_claim_unverified_semantic_vectors_ready(self) -> None:
        migration = (Path(__file__).resolve().parents[1] / "migrations" / "006_task1_governance.sql").read_text(
            encoding="utf-8"
        )
        self.assertIn("embedding_kind = 'SEMANTIC', embedding_status = 'PENDING'", migration)
        self.assertNotIn("embedding_kind = 'SEMANTIC', embedding_status = 'READY'", migration)
        self.assertIn("ix_lake_lineage_event_downstream_lookup", migration)
        self.assertIn("model_skill_revision ADD COLUMN IF NOT EXISTS governance_json", migration)


if __name__ == "__main__":
    unittest.main()
