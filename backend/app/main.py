from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api import data_interfaces, data_sources, model_hub, research, resource_hub, selection, stock_tools, stocks
from app.api import context_events
from app.api import foundation, foundation_jobs
from app.api import company_graph, company_governance, taxonomy
from app.api import knowledge_network, lakehouse
from app.api import knowledge_governance
from app.api import skill_evaluation, knowledge_pipeline
from app.core.config import get_settings
from app.db.bootstrap import initialize_database
from app.db.session import engine

settings = get_settings()


@asynccontextmanager
async def lifespan(_: FastAPI):
    initialize_database(seed_defaults=settings.seed_defaults_on_startup)
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
app.include_router(context_events.router, prefix=settings.api_v1_prefix)
app.include_router(research.router, prefix=settings.api_v1_prefix)
app.include_router(selection.router, prefix=settings.api_v1_prefix)
app.include_router(foundation_jobs.router, prefix=settings.api_v1_prefix)
app.include_router(foundation.router, prefix=settings.api_v1_prefix)
app.include_router(company_graph.router, prefix=settings.api_v1_prefix)
app.include_router(company_governance.router, prefix=settings.api_v1_prefix)
app.include_router(taxonomy.router, prefix=settings.api_v1_prefix)
app.include_router(knowledge_network.router, prefix=settings.api_v1_prefix)
app.include_router(lakehouse.router, prefix=settings.api_v1_prefix)
app.include_router(knowledge_governance.router, prefix=settings.api_v1_prefix)
app.include_router(skill_evaluation.router, prefix=settings.api_v1_prefix)
app.include_router(knowledge_pipeline.router, prefix=settings.api_v1_prefix)


@app.get("/health", tags=["System"])
def health_check() -> dict[str, str]:
    return {"status": "ok", "environment": settings.app_env}
