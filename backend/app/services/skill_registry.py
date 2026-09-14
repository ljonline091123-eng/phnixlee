"""Keep Skill prompt files, current records, and immutable revisions in sync."""

from __future__ import annotations

import hashlib
import re

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


def ensure_current_revision(db: Session, skill: ModelSkill) -> None:
    existing = db.scalar(
        select(ModelSkillRevision.id).where(
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
        ))
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


def save_skill_content(db: Session, skill: ModelSkill, content: str, source: str = "EDITOR") -> None:
    if not content.strip():
        raise ValueError("Skill instructions cannot be empty")
    if content != skill.instructions:
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
    save_skill_content(db, skill, revision.instructions, source="ROLLBACK")
