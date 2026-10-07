"""Default governance contracts seeded without rewriting governed data."""

from __future__ import annotations

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.governance import DataQualityRule


DEFAULT_QUALITY_RULES = (
    {
        "rule_code": "RAW_PROVENANCE_REQUIRED",
        "rule_name": "Raw对象来源与哈希完整性",
        "asset_scope": "LAKE_OBJECT",
        "target_code": "lake_object",
        "rule_type": "PROVENANCE_CONTRACT",
        "severity": "ERROR",
        "definition_json": {"evaluator": "REQUIRED_FIELDS", "fields": ["content_hash", "object_uri", "layer", "created_at"]},
        "description": "Raw对象必须保留原始位置、内容哈希、分层和归档时间；来源缺失时标记PARTIAL。",
    },
    {
        "rule_code": "DATASET_VERSION_QUALITY_GATE",
        "rule_name": "数据集版本质量门禁",
        "asset_scope": "LAKE_DATASET_VERSION",
        "target_code": "lake_dataset_version",
        "rule_type": "QUALITY_GATE",
        "severity": "ERROR",
        "definition_json": {"evaluator": "VERSION_QUALITY_STATUS", "accepted": ["PUBLISHED", "PARTIAL"]},
        "description": "数据集发布必须记录结构、行数、质量结果及明确的PUBLISHED/PARTIAL状态。",
    },
    {
        "rule_code": "DOCUMENT_SOURCE_REQUIRED",
        "rule_name": "知识文档来源可追溯",
        "asset_scope": "KNOWLEDGE_DOCUMENT",
        "target_code": "knowledge_document",
        "rule_type": "PROVENANCE_CONTRACT",
        "severity": "ERROR",
        "definition_json": {"evaluator": "REQUIRED_FIELDS", "fields": ["source_table", "title", "content"]},
        "description": "知识文档必须保留来源表、标题和原文；缺正文不得伪造为完整文档。",
    },
    {
        "rule_code": "CHUNK_LINEAGE_REQUIRED",
        "rule_name": "切片版本与血缘完整性",
        "asset_scope": "DOCUMENT_CHUNK",
        "target_code": "document_chunk_version",
        "rule_type": "LINEAGE_CONTRACT",
        "severity": "ERROR",
        "definition_json": {"evaluator": "CHUNK_LINEAGE", "required": ["chunk_version", "parser_version", "source_object_id", "lineage_batch_id"]},
        "description": "切片必须可回溯到原文对象和血缘批次，并保留章节及向量状态。",
    },
    {
        "rule_code": "HASH_VECTOR_TRUTHFUL",
        "rule_name": "哈希向量真实性标识",
        "asset_scope": "DOCUMENT_CHUNK",
        "target_code": "document_chunk_version",
        "rule_type": "SEMANTIC_TRUTH_CONTRACT",
        "severity": "CRITICAL",
        "definition_json": {"evaluator": "HASH_VECTOR_KIND", "hash_status": "HASH_ONLY", "hash_kind": "HASH"},
        "description": "HASH_EMBED类指纹只能标记为HASH/HASH_ONLY，不得标记为真实语义向量READY。",
    },
    {
        "rule_code": "FACT_EVIDENCE_REQUIRED",
        "rule_name": "统一事实证据完整性",
        "asset_scope": "FOUNDATION_FACT",
        "target_code": "foundation_fact",
        "rule_type": "EVIDENCE_CONTRACT",
        "severity": "CRITICAL",
        "definition_json": {"evaluator": "FACT_EVIDENCE_LINK", "minimum_evidence": 1},
        "description": "新闻、公告、市场、风险、股东及公司关系事实均必须绑定FoundationEvidence。",
    },
    {
        "rule_code": "SKILL_CONTROLLED_EXECUTION",
        "rule_name": "Skill受控执行契约",
        "asset_scope": "MODEL_SKILL",
        "target_code": "model_skill",
        "rule_type": "AI_EXECUTION_CONTRACT",
        "severity": "CRITICAL",
        "definition_json": {
            "evaluator": "SKILL_CONTRACT",
            "required": ["input_contract_json", "output_contract_json", "permission_policy_json", "side_effect_level", "idempotency_policy"],
            "arbitrary_sql": False,
        },
        "description": "启用Skill必须声明输入输出、权限、副作用、幂等、重试和错误策略；禁止任意SQL。",
    },
)


def seed_default_quality_rules(db: Session) -> None:
    for config in DEFAULT_QUALITY_RULES:
        row = db.scalar(select(DataQualityRule).where(
            DataQualityRule.rule_code == config["rule_code"],
            DataQualityRule.version == "1.0.0",
        ))
        if row is None:
            row = DataQualityRule(
                **config,
                version="1.0.0",
                lifecycle_status="ENABLED",
                enabled=True,
                threshold_json={},
            )
            db.add(row)
        else:
            row.rule_name = config["rule_name"]
            row.definition_json = config["definition_json"]
            row.description = config["description"]
            if row.lifecycle_status == "DRAFT" and not row.enabled:
                row.lifecycle_status = "ENABLED"
                row.enabled = True
    db.commit()
