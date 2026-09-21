"""Versioned taxonomy master data and human-readable definitions."""

from __future__ import annotations

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.taxonomy import ClassificationDefinition


# Provider concepts remain observations and are not silently treated as legal industries.
BUILTIN_DEFINITIONS = (
    dict(taxonomy="CN_SECURITIES", dimension="INDUSTRY", code="CSRC_LEVEL1", label="\u8bc1\u76d1\u4f1a\u884c\u4e1a\uff08\u4e00\u7ea7\uff09", definition="\u6309\u4e2d\u56fd\u8bc1\u76d1\u4f1a\u4e0a\u5e02\u516c\u53f8\u884c\u4e1a\u5206\u7c7b\u6807\u51c6\uff0c\u6839\u636e\u53d1\u884c\u4eba\u4e3b\u8425\u4e1a\u52a1\u5f52\u5165\u4e00\u7ea7\u884c\u4e1a\u3002", criteria="\u4ee5\u6765\u6e90\u673a\u6784\u63d0\u4f9b\u7684\u5206\u7c7b\u548c\u7248\u672c\u4e3a\u51c6\uff0c\u65e0\u8bc1\u636e\u65f6\u4e0d\u63a8\u65ad\u3002", source_name="\u4e2d\u56fd\u8bc1\u76d1\u4f1a\u884c\u4e1a\u5206\u7c7b\u6807\u51c6", definition_version="CSRC_CURRENT_V1", jurisdiction="CN"),
    dict(taxonomy="CN_SECURITIES", dimension="INDUSTRY", code="EASTMONEY_LEVEL1", label="\u6765\u6e90\u884c\u4e1a\uff08\u4e00\u7ea7\uff09", definition="\u6570\u636e\u63d0\u4f9b\u65b9\u6807\u6ce8\u7684\u884c\u4e1a\u540d\u79f0\uff0c\u53cd\u6620\u8bc1\u5238\u5728\u8be5\u6765\u6e90\u4e2d\u7684\u5f52\u5c5e\uff0c\u4e0d\u7b49\u4e8e\u5b98\u65b9\u5206\u7c7b\u6216\u6295\u8d44\u5efa\u8bae\u3002", criteria="\u4fdd\u7559\u6765\u6e90\u539f\u59cb\u4ee3\u7801\u548c\u7248\u672c\uff0c\u4e0d\u540c\u6765\u6e90\u540c\u540d\u4e0d\u81ea\u52a8\u5408\u5e76\u3002", source_name="\u4e1c\u65b9\u8d22\u5bcc\u516c\u5f00\u8bc1\u5238\u8d44\u6599", definition_version="EASTMONEY_CURRENT_V1", jurisdiction="CN"),
    dict(taxonomy="CN_SECURITIES", dimension="THEME", code="PROVIDER_CONCEPT", label="\u6765\u6e90\u4e3b\u9898\uff0f\u6982\u5ff5", definition="\u6570\u636e\u63d0\u4f9b\u65b9\u6839\u636e\u516c\u53f8\u4e1a\u52a1\u3001\u4ea7\u54c1\u3001\u516c\u544a\u6216\u5e02\u573a\u7ea6\u5b9a\u6574\u7406\u7684\u6295\u8d44\u4e3b\u9898\u96c6\u5408\uff0c\u4e00\u53ea\u8bc1\u5238\u53ef\u540c\u65f6\u5c5e\u4e8e\u591a\u4e2a\u4e3b\u9898\u3002\u5b83\u4e0d\u662f\u6cd5\u5b9a\u884c\u4e1a\uff0c\u4e5f\u4e0d\u8868\u793a\u516c\u53f8\u5168\u90e8\u6536\u5165\u6765\u81ea\u8be5\u4e3b\u9898\u3002", criteria="\u4ec5\u5c55\u793a\u6765\u6e90\u660e\u786e\u7ed9\u51fa\u7684\u4e3b\u9898\u53ca\u4ee3\u7801\uff0c\u4e0d\u6839\u636e\u4e3b\u9898\u540d\u79f0\u63a8\u65ad\u56e0\u679c\u3002", source_name="\u4e1c\u65b9\u8d22\u5bcc\u516c\u5f00\u8bc1\u5238\u8d44\u6599", definition_version="EASTMONEY_THEME_V1", jurisdiction="CN"),
    dict(taxonomy="CN_SECURITIES", dimension="TYPE", code="BLUE_CHIP", label="\u84dd\u7b79\u80a1", definition="\u901a\u5e38\u6307\u7ecf\u8425\u7a33\u5b9a\u3001\u89c4\u6a21\u8f83\u5927\u3001\u76c8\u5229\u548c\u6cbb\u7406\u8bb0\u5f55\u76f8\u5bf9\u6210\u719f\u7684\u4e0a\u5e02\u516c\u53f8\u80a1\u7968\u3002\u8be5\u79f0\u8c13\u6ca1\u6709\u7edf\u4e00\u6cd5\u5b9a\u9608\u503c\uff0c\u5fc5\u987b\u6ce8\u660e\u540d\u5355\u6216\u89c4\u5219\u3002", criteria="\u4ec5\u5728\u6709\u6765\u6e90\u540d\u5355\u6216\u5df2\u5ba1\u6838\u7684\u7248\u672c\u5316\u89c4\u5219\u65f6\u63a5\u53d7\u3002", source_name="\u7cfb\u7edf\u5206\u7c7b\u8bcd\u5178", definition_version="TYPE_GLOSSARY_V1", jurisdiction="GLOBAL"),
    dict(taxonomy="CN_SECURITIES", dimension="TYPE", code="RED_CHIP", label="\u7ea2\u7b79\u80a1", definition="\u901a\u5e38\u6307\u5728\u9999\u6e2f\u4e0a\u5e02\u3001\u7531\u4e2d\u56fd\u5185\u5730\u653f\u5e9c\u6216\u4f01\u4e1a\u63a7\u5236\u4e14\u4e3b\u8981\u4e1a\u52a1\u4e0e\u4e2d\u56fd\u5185\u5730\u76f8\u5173\u7684\u516c\u53f8\u80a1\u7968\u3002\u5177\u4f53\u8d44\u683c\u4ee5\u4ea4\u6613\u6240\u548c\u6765\u6e90\u53e3\u5f84\u4e3a\u51c6\u3002", criteria="\u9700\u6709\u4ea4\u6613\u6240\u5c5e\u6027\u53ca\u63a7\u5236\u6216\u4e1a\u52a1\u8bc1\u636e\uff0c\u4e0d\u51ed\u540d\u79f0\u8ba4\u5b9a\u3002", source_name="\u7cfb\u7edf\u5206\u7c7b\u8bcd\u5178", definition_version="TYPE_GLOSSARY_V1", jurisdiction="HK"),
    dict(taxonomy="CN_SECURITIES", dimension="SIZE", code="LARGE_CAP", label="\u5927\u76d8\u80a1", definition="\u6309\u6307\u5b9a\u65e5\u671f\u548c\u5e02\u503c\u53e3\u5f84\u5212\u5206\u7684\u8f83\u5927\u89c4\u6a21\u8bc1\u5238\uff0c\u4e0d\u662f\u5168\u5e02\u573a\u5206\u4f4d\u6392\u540d\u3002", criteria="\u5fc5\u987b\u8bb0\u5f55\u5e02\u503c\u6765\u6e90\u3001\u65f6\u70b9\u3001\u5e01\u79cd\u548c\u9608\u503c\u7248\u672c\u3002", source_name="\u7cfb\u7edf\u5e02\u503c\u5206\u7c7b\u89c4\u5219", definition_version="SIZE_ABSOLUTE_CNY_V1", jurisdiction="CN"),
    dict(taxonomy="CN_SECURITIES", dimension="SIZE", code="MID_CAP", label="\u4e2d\u76d8\u80a1", definition="\u6309\u6307\u5b9a\u5e02\u503c\u53e3\u5f84\u5212\u5206\u7684\u4e2d\u7b49\u89c4\u6a21\u8bc1\u5238\uff0c\u4e0d\u662f\u884c\u4e1a\u6216\u98ce\u683c\u6807\u7b7e\u3002", criteria="\u4f7f\u7528\u7248\u672c\u5316\u7684\u7edd\u5bf9\u5e02\u503c\u533a\u95f4\u5e76\u4fdd\u7559\u5feb\u7167\u8bc1\u636e\u3002", source_name="\u7cfb\u7edf\u5e02\u503c\u5206\u7c7b\u89c4\u5219", definition_version="SIZE_ABSOLUTE_CNY_V1", jurisdiction="CN"),
    dict(taxonomy="CN_SECURITIES", dimension="SIZE", code="SMALL_CAP", label="\u5c0f\u76d8\u80a1", definition="\u6309\u6307\u5b9a\u5e02\u503c\u53e3\u5f84\u5212\u5206\u7684\u8f83\u5c0f\u89c4\u6a21\u8bc1\u5238\uff0c\u4e0d\u662f\u5bf9\u672a\u6765\u6536\u76ca\u6216\u98ce\u9669\u7684\u5224\u65ad\u3002", criteria="\u4f7f\u7528\u7248\u672c\u5316\u7684\u7edd\u5bf9\u5e02\u503c\u533a\u95f4\u5e76\u4fdd\u7559\u5feb\u7167\u8bc1\u636e\u3002", source_name="\u7cfb\u7edf\u5e02\u503c\u5206\u7c7b\u89c4\u5219", definition_version="SIZE_ABSOLUTE_CNY_V1", jurisdiction="CN"),
    dict(taxonomy="CN_SECURITIES", dimension="LEGAL_LISTING_CLASS", code="A_SHARE", label="A\u80a1", definition="\u5728\u4e2d\u56fd\u5185\u5730\u8bc1\u5238\u4ea4\u6613\u6240\u4e0a\u5e02\u3001\u4ee5\u4eba\u6c11\u5e01\u4ea4\u6613\u7684\u666e\u901a\u80a1\u7c7b\u522b\u3002", criteria="\u6839\u636e\u8bc1\u5238\u5e02\u573a\u548c\u4ea4\u6613\u6240\u4e3b\u6570\u636e\u786e\u5b9a\u3002", source_name="\u8bc1\u5238\u4e3b\u6570\u636e", definition_version="LISTING_CLASS_V1", jurisdiction="CN"),
    dict(taxonomy="CN_SECURITIES", dimension="LEGAL_LISTING_CLASS", code="H_SHARE", label="H\u80a1", definition="\u5728\u9999\u6e2f\u4ea4\u6613\u6240\u4e0a\u5e02\u3001\u7531\u4e2d\u56fd\u5185\u5730\u6ce8\u518c\u516c\u53f8\u53d1\u884c\u7684\u5883\u5916\u4e0a\u5e02\u80a1\u4efd\u7c7b\u522b\u3002", criteria="\u6839\u636e\u5e02\u573a\u3001\u53d1\u884c\u4e3b\u4f53\u548c\u4ea4\u6613\u6240\u4e3b\u6570\u636e\u786e\u5b9a\u3002", source_name="\u8bc1\u5238\u4e3b\u6570\u636e", definition_version="LISTING_CLASS_V1", jurisdiction="HK"),
)


def seed_builtin_definitions(db: Session) -> int:
    created = 0
    for item in BUILTIN_DEFINITIONS:
        row = db.scalar(select(ClassificationDefinition).where(
            ClassificationDefinition.taxonomy == item["taxonomy"], ClassificationDefinition.dimension == item["dimension"],
            ClassificationDefinition.code == item["code"], ClassificationDefinition.definition_version == item["definition_version"]))
        if row is None:
            db.add(ClassificationDefinition(**item, status="ACTIVE", properties_json={}))
            created += 1
    if created:
        db.flush()
    return created


def definition_read(row: ClassificationDefinition) -> dict:
    return {"id": row.id, "taxonomy": row.taxonomy, "dimension": row.dimension, "code": row.code,
            "label": row.label, "definition": row.definition, "criteria": row.criteria,
            "parent_code": row.parent_code, "jurisdiction": row.jurisdiction, "source_name": row.source_name,
            "source_url": row.source_url, "definition_version": row.definition_version, "status": row.status,
            "valid_from": row.valid_from, "valid_to": row.valid_to, "properties_json": row.properties_json}


def list_definitions(db: Session, *, dimension: str | None = None, taxonomy: str | None = None,
                     code: str | None = None, q: str | None = None, active_only: bool = True) -> list[dict]:
    query = select(ClassificationDefinition)
    if dimension:
        query = query.where(ClassificationDefinition.dimension == dimension.upper())
    if taxonomy:
        query = query.where(ClassificationDefinition.taxonomy == taxonomy)
    if code:
        query = query.where(ClassificationDefinition.code == code)
    if q:
        query = query.where(ClassificationDefinition.label.contains(q, autoescape=True))
    if active_only:
        query = query.where(ClassificationDefinition.status == "ACTIVE")
    rows = db.scalars(query.order_by(ClassificationDefinition.dimension, ClassificationDefinition.label)).all()
    return [definition_read(row) for row in rows]


def get_definition(db: Session, definition_id: str) -> dict:
    row = db.get(ClassificationDefinition, definition_id)
    if row is None:
        from app.services.foundation import FoundationError
        raise FoundationError("classification definition not found", 404)
    return definition_read(row)


def get_definition(db: Session, definition_id: str) -> dict:
    row = db.get(ClassificationDefinition, definition_id)
    if row is None or row.status != "ACTIVE":
        from app.services.foundation import FoundationError
        raise FoundationError("taxonomy definition not found", 404)
    return definition_read(row)
