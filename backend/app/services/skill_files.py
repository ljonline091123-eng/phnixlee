from __future__ import annotations

import hashlib
from pathlib import Path


SKILL_ROOT = Path(__file__).resolve().parents[2] / "skill_docs"


def _safe_skill_code(skill_code: str) -> str:
    normalized = skill_code.strip().upper()
    if not normalized or not all(char.isalnum() or char in {"_", "-"} for char in normalized):
        raise ValueError("Invalid skill code")
    return normalized


def skill_file_path(skill_code: str) -> Path:
    return SKILL_ROOT / f"{_safe_skill_code(skill_code)}.SKILL.md"


def write_skill_file(skill_code: str, content: str) -> tuple[str, str]:
    path = skill_file_path(skill_code)
    SKILL_ROOT.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    return str(path.relative_to(SKILL_ROOT.parent)), hashlib.sha256(content.encode("utf-8")).hexdigest()


def ensure_skill_file(skill_code: str, content: str) -> tuple[str, str]:
    path = skill_file_path(skill_code)
    if not path.exists():
        return write_skill_file(skill_code, content)
    existing = path.read_text(encoding="utf-8")
    return str(path.relative_to(SKILL_ROOT.parent)), hashlib.sha256(existing.encode("utf-8")).hexdigest()


def delete_skill_file(skill_code: str) -> None:
    path = skill_file_path(skill_code)
    if path.exists():
        path.unlink()

