"""Add only the standalone analyst table/Skill; never rewrite legacy assets."""
from copy import deepcopy
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from sqlalchemy import inspect, select
from app.db.session import SessionLocal, engine
from app.models.ai_hub import ModelSkill
from app.models.stock_analyst import AiStockAnalysisRecord
from app.services.stock_analyst_skill import STOCK_ANALYST_SKILL
from app.services.skill_registry import sync_skill_from_file


def migrate():
    existing = inspect(engine).has_table(AiStockAnalysisRecord.__tablename__)
    AiStockAnalysisRecord.__table__.create(engine, checkfirst=True)
    with SessionLocal() as db:
        skill = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == STOCK_ANALYST_SKILL["skill_code"]))
        if skill is None:
            skill = ModelSkill(**deepcopy(STOCK_ANALYST_SKILL), is_builtin=True)
            db.add(skill)
            db.flush()
            sync_skill_from_file(db, skill)
        db.commit()
        return {"table": AiStockAnalysisRecord.__tablename__, "created": not existing,
                "skill_code": skill.skill_code, "skill_version": skill.version}


if __name__ == "__main__":
    print(json.dumps(migrate(), ensure_ascii=False))
