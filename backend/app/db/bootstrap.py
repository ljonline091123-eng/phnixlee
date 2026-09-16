"""Database schema/bootstrap entry point shared by API and CLI processes."""

from __future__ import annotations

import app.models  # noqa: F401 - register every ORM model in Base.metadata
from app.db.base import Base
from app.db.migrations import ensure_compat_columns
from app.db.table_comments import seed_table_comments
from app.db.session import SessionLocal, engine
from app.services.catalog import seed_default_catalog
from app.services.model_hub import seed_default_models, seed_default_skills
from app.services.resource_hub import (
    seed_default_agents,
    seed_default_data_assets,
    seed_default_graphs,
    seed_default_knowledge_bases,
)


def initialize_database(*, seed_defaults: bool = True) -> None:
    ensure_compat_columns(engine)
    Base.metadata.create_all(bind=engine)
    if not seed_defaults:
        return
    with SessionLocal() as db:
        seed_table_comments(db)
        seed_default_catalog(db)
        seed_default_models(db)
        seed_default_skills(db)
        seed_default_data_assets(db)
        seed_default_knowledge_bases(db)
        seed_default_graphs(db)
        seed_default_agents(db)
