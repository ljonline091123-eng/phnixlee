"""Durable company governance workflow using the existing worker protocol."""

from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import tempfile

from pydantic import Field, StrictInt
from sqlalchemy import select

from app.core.config import get_settings
from app.db.session import SessionLocal
from app.jobs.dispatcher import DatabaseJobDispatcher
from app.models.ai_hub import AgentDefinition, ModelSkill
from app.models.market_data import DataSource, StockSymbol
from app.schemas.foundation import StrictInput
from app.services.company_sample import SAMPLE_SECURITIES
from app.services.company_registry import register_company_resources


TASK_TYPE = "company_graph_governance"


class GovernanceRequest(StrictInput):
    stock_symbol_ids: list[StrictInt] = Field(min_length=1, max_length=30)
    include_holders: bool = True
    include_legal: bool = True
    include_business: bool = True
    include_supply_chain: bool = False
    include_marketcap: bool = True
    accept_structured: bool = False
    request_key: str = Field(min_length=1, max_length=100)


def enqueue_company_governance(db, request: GovernanceRequest):
    if not get_settings().data_foundation_enabled:
        raise ValueError("Data foundation is disabled")
    ids = sorted(set(request.stock_symbol_ids))
    if any(isinstance(value, bool) or value <= 0 for value in ids):
        raise ValueError("Stock IDs must be positive integers")
    stocks = list(db.scalars(select(StockSymbol).where(StockSymbol.id.in_(ids))))
    if len(stocks) != len(ids):
        raise LookupError("Some requested securities do not exist")
    if any(stock.market not in {"CN_A", "HK"} for stock in stocks):
        raise ValueError("Company source workflow supports CN_A and HK only")
    payload = request.model_dump()
    payload["stock_symbol_ids"] = ids
    digest = hashlib.sha256(json.dumps(payload, sort_keys=True).encode()).hexdigest()
    return DatabaseJobDispatcher(db).enqueue(task_type=TASK_TYPE,
        idempotency_key=TASK_TYPE + ":" + digest, payload=payload,
        pipeline_type=TASK_TYPE, trigger_type="MANUAL", max_attempts=2)


class CompanyGovernanceWorkflow:
    def execute(self, payload=None):
        from app.connectors.company_sources import CompanySourcesClient
        from app.services.company_governance import import_cached_sources
        request = GovernanceRequest.model_validate(payload or {})
        if not get_settings().data_foundation_enabled:
            raise ValueError("Data foundation is disabled")
        with SessionLocal() as db:
            stocks = list(db.scalars(select(StockSymbol).where(StockSymbol.id.in_(request.stock_symbol_ids))))
            samples = [{"market": row.market, "symbol": row.symbol} for row in stocks]
            if len(stocks) != len(set(request.stock_symbol_ids)):
                raise LookupError("Requested stock no longer exists")
            resources = register_company_resources(db)
            agent = db.get(AgentDefinition, resources["agent_id"])
            skills = list(db.scalars(select(ModelSkill).where(ModelSkill.id.in_(resources["skill_ids"]))))
            if not agent.enabled or any(not skill.enabled for skill in skills):
                raise ValueError("Company governance agent or required skill is disabled")
            source_enabled = {row.source_code: row.enabled for row in db.scalars(select(DataSource))}
            if not source_enabled.get("EASTMONEY_COMPANY_PROFILE"):
                raise ValueError("Company profile source is disabled")
            versions = {skill.skill_code: skill.version for skill in skills}
            db.commit()
        cache = Path(tempfile.mkdtemp(prefix="company-governance-"))
        with CompanySourcesClient(cache_dir=cache) as client:
            for item in samples:
                client.fetch_company_profile(**item)
                if request.include_holders:
                    client.fetch_shareholders(**item)
                if request.include_marketcap and source_enabled.get("TENCENT_MARKET_CAP"):
                    client.fetch_market_cap(**item)
                if item["market"] == "CN_A":
                    if request.include_legal and source_enabled.get("CNINFO_LEGAL_DISCLOSURES"):
                        client.fetch_legal_disclosures(item["symbol"], max_documents=2)
                    if request.include_business and source_enabled.get("CNINFO_BUSINESS_DISCLOSURES"):
                        client.fetch_business_disclosures(item["symbol"], max_documents=2)
                    if request.include_supply_chain and source_enabled.get("CNINFO_SUPPLY_CHAIN_DISCLOSURES"):
                        client.fetch_supply_chain_disclosures(item["symbol"], max_documents=2)
        with SessionLocal() as db:
            result = import_cached_sources(db, cache, accept_structured=request.accept_structured, samples=samples)
            optional_sources = (
                ("legal", request.include_legal, "CNINFO_LEGAL_DISCLOSURES"),
                ("business", request.include_business, "CNINFO_BUSINESS_DISCLOSURES"),
                ("supply_chain", request.include_supply_chain, "CNINFO_SUPPLY_CHAIN_DISCLOSURES"),
                ("marketcap", request.include_marketcap, "TENCENT_MARKET_CAP"),
            )
            # A missing cache does not distinguish omitted, disabled and unsupported requests.
            for item in result["items"]:
                for key, requested, source_code in optional_sources:
                    if not requested:
                        item["sources"][key] = {"status": "NOT_REQUESTED", "warnings": []}
                    elif not source_enabled.get(source_code):
                        item["sources"][key] = {"status": "DISABLED", "warnings": [
                            f"Requested source {source_code} is disabled; no collection was attempted."]}
                    elif key in {"legal", "business", "supply_chain"} and item.get("market") == "HK":
                        item["sources"][key] = {"status": "UNSUPPORTED", "warnings": [
                            f"Requested source {source_code} does not support HK disclosures in this workflow."]}
            result["skill_versions"] = versions
            result["workflow"] = TASK_TYPE
            result["source_status"] = "PARTIAL" if any(
                item["status"] != "MAPPED" for item in result["items"]) or any(
                source.get("status") not in {"SUCCESS", "EMPTY", "NOT_REQUESTED"}
                for item in result["items"] for source in item["sources"].values()) else "COMPLETE_WITH_COVERAGE_LIMITS"
            db.commit()
            return result
