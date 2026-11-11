"""Additive/idempotent migration. Rollback disables watches; retained history is safe."""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from sqlalchemy import inspect, select, text
from app.db.session import engine, SessionLocal
from app.db.table_comments import seed_table_comments
from app.models.ai_hub import ModelSkill
from app.models.stock_analyst import AiAnalystDialogue, AiAnalystReportRevision, AiAnalystWatch, AiAnalystDailyReview, AiAnalystWatchSuggestion, AiAnalystForecast, AiAnalystForecastCheck
from app.services.stock_analyst_skill import (
    STOCK_ANALYST_EVIDENCE_REVIEW_SKILL, STOCK_ANALYST_FOLLOWUP_SKILL, STOCK_ANALYST_SKILL,
)
from app.services.skill_registry import ensure_current_revision, save_skill_content, sync_skill_from_file


def _sync_builtin_skill(db, definition):
    skill = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == definition["skill_code"]))
    if skill is None:
        skill = ModelSkill(**definition, is_builtin=True)
        db.add(skill)
        db.flush()
        sync_skill_from_file(db, skill)
        return skill
    if not skill.is_builtin:
        return skill

    desired_config = definition.get("config_json") or {}
    resolved_contracts = {
        key: definition.get(key) for key in (
            "input_contract_json", "output_contract_json", "permission_policy_json",
            "retry_policy_json", "error_policy_json", "side_effect_level", "idempotency_policy",
        )
    }
    managed_changed = any(
        getattr(skill, key, None) != value for key, value in resolved_contracts.items() if value is not None
    ) or any(getattr(skill, key, None) != definition.get(key)
             for key in ("description", "enabled", "lifecycle_status"))
    managed_changed = managed_changed or any(
        (skill.config_json or {}).get(key) != value for key, value in desired_config.items()
    )
    ensure_current_revision(db, skill)
    for key, value in resolved_contracts.items():
        if value is not None:
            setattr(skill, key, value)
    for key in ("description", "enabled", "lifecycle_status", "skill_type", "format"):
        if key in definition:
            setattr(skill, key, definition[key])
    def version_tuple(value):
        try:
            return tuple(int(part) for part in str(value or "0").split(".")[:3])
        except ValueError:
            return (0,)
    desired_version = definition.get("version")
    if desired_version and version_tuple(skill.version) < version_tuple(desired_version):
        skill.version = desired_version
    skill.config_json = {**dict(skill.config_json or {}), **desired_config}
    file_changed = sync_skill_from_file(db, skill)
    if managed_changed and not file_changed:
        save_skill_content(db, skill, skill.instructions, source="CONTRACT", force_revision=True)
    return skill


def migrate():
    added = []
    for model in (AiAnalystDialogue, AiAnalystReportRevision, AiAnalystWatch, AiAnalystDailyReview, AiAnalystWatchSuggestion, AiAnalystForecast, AiAnalystForecastCheck):
        exists = inspect(engine).has_table(model.__tablename__)
        model.__table__.create(engine, checkfirst=True)
        if not exists:
            added.append(model.__tablename__)
        for index in model.__table__.indexes:
            index.create(engine, checkfirst=True)
    if "context_snapshot_json" not in {col["name"] for col in inspect(engine).get_columns("ai_analyst_dialogue")}:
        with engine.begin() as connection:
            connection.execute(text("ALTER TABLE ai_analyst_dialogue ADD COLUMN context_snapshot_json JSON NOT NULL DEFAULT '{}'"))
    with SessionLocal() as db:
        skills = [
            _sync_builtin_skill(db, definition)
            for definition in (STOCK_ANALYST_SKILL, STOCK_ANALYST_EVIDENCE_REVIEW_SKILL,
                              STOCK_ANALYST_FOLLOWUP_SKILL)
        ]
        skill_summary = [{"skill_code": skill.skill_code, "version": skill.version}
                         for skill in skills]
        db.commit()
        seed_table_comments(db)
    return {"added_tables": added, "skills": skill_summary}


if __name__ == "__main__":
    print(migrate())
