"""Additive/idempotent migration. Rollback disables watches; retained history is safe."""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from sqlalchemy import inspect, select
from app.db.session import engine, SessionLocal
from app.db.table_comments import seed_table_comments
from app.models.ai_hub import ModelSkill
from app.models.stock_analyst import AiAnalystDialogue, AiAnalystReportRevision, AiAnalystWatch, AiAnalystDailyReview
from app.services.stock_analyst_skill import STOCK_ANALYST_FOLLOWUP_SKILL
from app.services.skill_registry import ensure_current_revision, save_skill_content, sync_skill_from_file


def migrate():
    added = []
    for model in (AiAnalystDialogue, AiAnalystReportRevision, AiAnalystWatch, AiAnalystDailyReview):
        exists = inspect(engine).has_table(model.__tablename__)
        model.__table__.create(engine, checkfirst=True)
        if not exists:
            added.append(model.__tablename__)
        for index in model.__table__.indexes:
            index.create(engine, checkfirst=True)
    with SessionLocal() as db:
        skill = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == STOCK_ANALYST_FOLLOWUP_SKILL["skill_code"]))
        if skill is None:
            skill = ModelSkill(**STOCK_ANALYST_FOLLOWUP_SKILL, is_builtin=True)
            db.add(skill)
            db.flush()
            sync_skill_from_file(db, skill)
        elif skill.is_builtin and (skill.config_json or {}).get("managed_contract_version") == "ANALYST_FOLLOWUP_V1":
            # Preserve the old executable contract before upgrading this module's built-in Skill only.
            ensure_current_revision(db, skill)
            skill.output_contract_json = STOCK_ANALYST_FOLLOWUP_SKILL["output_contract_json"]
            skill.config_json = {**(skill.config_json or {}), **STOCK_ANALYST_FOLLOWUP_SKILL["config_json"]}
            save_skill_content(db, skill, STOCK_ANALYST_FOLLOWUP_SKILL["instructions"], source="CONTRACT", force_revision=True)
            sync_skill_from_file(db, skill)
        db.commit()
        seed_table_comments(db)
    return {"added_tables": added, "skill": STOCK_ANALYST_FOLLOWUP_SKILL["skill_code"]}


if __name__ == "__main__":
    print(migrate())
