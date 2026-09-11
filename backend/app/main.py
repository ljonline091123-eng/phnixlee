import asyncio
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api import data_interfaces, data_sources, model_hub, research, resource_hub, stock_tools, stocks
from app.core.config import get_settings
from app.db.base import Base
from app.db.migrations import ensure_compat_columns
from app.db.session import SessionLocal, engine
from app.services.catalog import seed_default_catalog
from app.services.daily_sync import daily_master_sync_loop
from app.services.model_hub import seed_default_models, seed_default_skills
from app.services.resource_hub import seed_default_data_assets

settings = get_settings()


@asynccontextmanager
async def lifespan(_: FastAPI):
    ensure_compat_columns(engine)
    Base.metadata.create_all(bind=engine)
    with SessionLocal() as db:
        seed_default_catalog(db)
        seed_default_models(db)
        seed_default_skills(db)
        seed_default_data_assets(db)
    sync_task = asyncio.create_task(daily_master_sync_loop())
    try:
        yield
    finally:
        sync_task.cancel()
        try:
            await sync_task
        except asyncio.CancelledError:
            pass
        engine.dispose()


app = FastAPI(
    title=settings.app_name,
    version="0.1.0",
    description="Multi-source market-data management foundation.",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(data_sources.router, prefix=settings.api_v1_prefix)
app.include_router(data_interfaces.router, prefix=settings.api_v1_prefix)
app.include_router(stocks.router, prefix=settings.api_v1_prefix)
app.include_router(stock_tools.router, prefix=settings.api_v1_prefix)
app.include_router(model_hub.router, prefix=settings.api_v1_prefix)
app.include_router(resource_hub.router, prefix=settings.api_v1_prefix)
app.include_router(research.router, prefix=settings.api_v1_prefix)


@app.get("/health", tags=["System"])
def health_check() -> dict[str, str]:
    return {"status": "ok", "environment": settings.app_env}
