from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api import data_interfaces, data_sources, model_hub, research, resource_hub, stock_tools, stocks
from app.core.config import get_settings
from app.db.bootstrap import initialize_database
from app.db.session import engine

settings = get_settings()


@asynccontextmanager
async def lifespan(_: FastAPI):
    initialize_database()
    try:
        yield
    finally:
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
