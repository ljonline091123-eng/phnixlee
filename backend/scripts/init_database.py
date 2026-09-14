from app.db.base import Base
from app.db.session import SessionLocal, engine
from app.db.table_comments import seed_table_comments
from app.db.migrations import ensure_compat_columns
from app.services.catalog import seed_default_catalog
from app.services.model_hub import seed_default_models, seed_default_skills


def main() -> None:
    ensure_compat_columns(engine)
    Base.metadata.create_all(bind=engine)
    with SessionLocal() as db:
        seed_table_comments(db)
        seed_default_catalog(db)
        seed_default_models(db)
        seed_default_skills(db)
    print("Database tables and default data-source catalog are ready.")


if __name__ == "__main__":
    main()
