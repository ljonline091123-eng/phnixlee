"""Keep Skill prompt files, current records, and immutable revisions in sync."""

from __future__ import annotations

import hashlib
import re
from copy import deepcopy

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.ai_hub import ModelSkill, ModelSkillRevision
from app.services.skill_files import skill_file_path, write_skill_file


def content_hash(content: str) -> str:
    return hashlib.sha256(content.encode("utf-8")).hexdigest()


def next_version(current: str) -> str:
    match = re.fullmatch(r"(\d+)\.(\d+)\.(\d+)", current)
    if match:
        return f"{match.group(1)}.{match.group(2)}.{int(match.group(3)) + 1}"
    return "1.0.1"


GOVERNANCE_FIELDS = (
    "skill_type",
    "input_contract_json",
    "output_contract_json",
    "permission_policy_json",
    "side_effect_level",
    "idempotency_policy",
    "retry_policy_json",
    "error_policy_json",
)


def skill_governance_snapshot(skill: ModelSkill) -> dict:
    config = deepcopy(skill.config_json or {})
    # Lifecycle history is an audit trail, not executable contract state. It
    # must survive a rollback instead of being replaced by an older snapshot.
    config.pop("lifecycle_history", None)
    return {
        **{field_name: deepcopy(getattr(skill, field_name)) for field_name in GOVERNANCE_FIELDS},
        "config_json": config,
    }


def ensure_current_revision(db: Session, skill: ModelSkill) -> None:
    existing = db.scalar(
        select(ModelSkillRevision).where(
            ModelSkillRevision.skill_id == skill.id,
            ModelSkillRevision.version == skill.version,
        )
    )
    if not existing:
        db.add(ModelSkillRevision(
            skill_id=skill.id,
            version=skill.version,
            instructions=skill.instructions,
            content_hash=content_hash(skill.instructions),
            source="SEED",
            governance_json=skill_governance_snapshot(skill),
        ))
        db.flush()
    elif not existing.governance_json:
        # Only the current revision can be recovered safely from the current
        # row. Historical legacy revisions remain instruction-only.
        existing.governance_json = skill_governance_snapshot(skill)
        db.flush()


def _new_revision(db: Session, skill: ModelSkill, content: str, source: str) -> None:
    ensure_current_revision(db, skill)
    skill.version = next_version(skill.version)
    skill.instructions = content
    skill.content_hash = content_hash(content)
    db.add(ModelSkillRevision(
        skill_id=skill.id,
        version=skill.version,
        instructions=content,
        content_hash=skill.content_hash,
        source=source,
        governance_json=skill_governance_snapshot(skill),
    ))
    db.flush()


def sync_skill_from_file(db: Session, skill: ModelSkill) -> bool:
    """Import an external Markdown edit as a new revision; restore a missing file."""
    path = skill_file_path(skill.skill_code)
    if not path.exists():
        skill.file_path, skill.content_hash = write_skill_file(skill.skill_code, skill.instructions)
        ensure_current_revision(db, skill)
        return True
    content = path.read_text(encoding="utf-8")
    if not content.strip():
        raise ValueError(f"Skill file is empty: {path.name}")
    changed = content != skill.instructions
    if changed:
        _new_revision(db, skill, content, "FILE")
    else:
        ensure_current_revision(db, skill)
        skill.content_hash = content_hash(content)
    skill.file_path = str(path.relative_to(path.parent.parent))
    return changed


def save_skill_content(db: Session, skill: ModelSkill, content: str, source: str = "EDITOR",
                       *, force_revision: bool = False) -> None:
    if not content.strip():
        raise ValueError("Skill instructions cannot be empty")
    if content != skill.instructions or force_revision:
        _new_revision(db, skill, content, source)
    else:
        ensure_current_revision(db, skill)
    skill.file_path, skill.content_hash = write_skill_file(skill.skill_code, skill.instructions)


def rollback_skill(db: Session, skill: ModelSkill, revision_id: int) -> None:
    revision = db.scalar(select(ModelSkillRevision).where(
        ModelSkillRevision.id == revision_id,
        ModelSkillRevision.skill_id == skill.id,
    ))
    if revision is None:
        raise LookupError("Skill revision not found")
    ensure_current_revision(db, skill)
    snapshot = dict(revision.governance_json or {})
    for field_name in GOVERNANCE_FIELDS:
        if field_name in snapshot:
            setattr(skill, field_name, deepcopy(snapshot[field_name]))
    if "config_json" in snapshot:
        lifecycle_history = list((skill.config_json or {}).get("lifecycle_history") or [])
        skill.config_json = deepcopy(snapshot["config_json"] or {})
        if lifecycle_history:
            skill.config_json["lifecycle_history"] = lifecycle_history
    save_skill_content(db, skill, revision.instructions, source="ROLLBACK", force_revision=True)
