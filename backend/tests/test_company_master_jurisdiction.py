from copy import deepcopy
from datetime import datetime, timedelta, timezone

import pytest
from sqlalchemy import create_engine, func, select
from sqlalchemy.orm import Session

import app.models
from app.db.base import Base
from app.models.company_graph import SourceIdentity
from app.models.company_mapping import CompanyMappingState
from app.models.foundation import FoundationEntity, FoundationEvidence, FoundationListing, FoundationSecurity
from app.models.market_data import DataSource, StockSymbol
from app.models.pipeline import PipelineRun
from app.services.company_master import import_master_batch
from scripts.reconcile_company_master_jurisdiction import AUDIT_SOURCE, reconcile


@pytest.fixture
def context(tmp_path):
    engine = create_engine("sqlite://")
    Base.metadata.create_all(engine)
    snapshot_name = str(tmp_path / "snapshot.json")
    with Session(engine) as db:
        source = DataSource(source_code="FIXTURE", source_name="Fixture", adapter_type="MOCK")
        db.add(source)
        db.flush()
        stocks = [StockSymbol(source_id=source.id, market=market, symbol=symbol, name="Issuer stock", exchange=market)
            for market, symbol in [("CN_A", "600938"), ("HK", "00883")]]
        db.add_all(stocks)
        run = PipelineRun(pipeline_type="company_master_mapping", status="RUNNING", started_at=datetime.now(timezone.utc),
            input_json={"snapshot": snapshot_name})
        db.add(run)
        db.flush()
        records = []
        for stock in stocks:
            hk = stock.market == "HK"
            raw = {"ORG_CODE": "ORG-1", "ORG_NAME": "Exact Registered Company",
                "SECUCODE": f"{stock.symbol}.{'HK' if hk else 'SH'}", "SECURITY_TYPE": "A股"}
            if hk:
                raw["REG_PLACE"] = "中国香港特别行政区"
            record = {"market": stock.market, "symbol": stock.symbol, "source_name": "EASTMONEY",
                "source_url": "https://example.test/public-profile", "observed_at": "2026-01-01T00:00:00Z",
                "source_record": raw, "company": {"name": raw["ORG_NAME"], "source_issuer_id": "ORG-1",
                    "jurisdiction": raw["REG_PLACE"] if hk else "CN", "properties_json": {"registered_name": raw["ORG_NAME"]}}}
            if hk:
                record["security_classification_evidence"] = {"source_name": "HKEX", "source_record": {
                    "Stock Code": stock.symbol, "Category": "Equity", "Sub-Category": "Equity Securities (Main Board)"}}
            records.append(record)
        snapshot = {"source_code": "EASTMONEY_COMPANY_MASTER", "source_name": "EASTMONEY", "records": records}
        import_master_batch(db, stocks[:1], snapshot, snapshot_name=snapshot_name)
        run.status, run.completed_at = "COMPLETED", datetime.now(timezone.utc)
        db.commit()
        kwargs = {"snapshot_name": snapshot_name, "snapshot_sha256": "a" * 64,
            "baseline_sha256": "b" * 64, "symbols": ["600938"]}
        yield db, snapshot, stocks, kwargs
    engine.dispose()


def test_dry_run_is_read_only_and_apply_preserves_identity_and_original_evidence(context):
    db, snapshot, stocks, kwargs = context
    company = db.scalar(select(FoundationEntity))
    listing = db.scalar(select(FoundationListing))
    alias = db.scalar(select(SourceIdentity))
    proof = db.get(FoundationEvidence, listing.evidence_id)
    original = (company.id, listing.id, listing.security_id, listing.evidence_id, alias.id,
        proof.content, deepcopy(proof.metadata_json), proof.content_hash)
    preview = reconcile(db, snapshot, set(), **kwargs)
    assert preview["passed"] and not preview["applied"] and preview["eligible"] == 1
    assert company.jurisdiction == "CN" and alias.jurisdiction == "CN" and not db.dirty
    result = reconcile(db, snapshot, set(), apply=True, **kwargs)
    db.commit()
    assert result["corrected"] == 1 and result["added_evidence"] == 1
    assert company.jurisdiction == "中国香港特别行政区" == alias.jurisdiction
    assert original == (company.id, listing.id, listing.security_id, listing.evidence_id, alias.id,
        proof.content, proof.metadata_json, proof.content_hash)
    audit = db.scalar(select(FoundationEvidence).where(FoundationEvidence.source_name == AUDIT_SOURCE))
    assert audit.id != proof.id and audit.entity_id == company.id
    assert audit.metadata_json["registration_verified"] is False
    assert company.properties_json["jurisdiction_reconciliation"]["evidence_id"] == audit.id
    again = reconcile(db, snapshot, set(), apply=True, **kwargs)
    db.commit()
    assert again["corrected"] == 0 and again["added_evidence"] == 0
    assert db.scalar(select(func.count()).select_from(FoundationEvidence)) == 2
    assert db.scalar(select(func.count()).select_from(PipelineRun)) == 2
    # The unchanged snapshot now maps the matching H security to the same UUID.
    hk_result = import_master_batch(db, stocks[1:], snapshot, snapshot_name=kwargs["snapshot_name"])
    assert hk_result["counts"] == {"MAPPED": 1}
    hk_listing = db.scalar(select(FoundationListing).where(FoundationListing.stock_symbol_id == stocks[1].id))
    assert db.get(FoundationSecurity, hk_listing.security_id).entity_id == company.id


@pytest.mark.parametrize("issue", [
    "baseline_company", "different_name", "different_issuer", "missing_reg_place", "conflicting_reg_place",
    "domestic_reg_number", "registered_company", "other_alias", "other_snapshot", "modified_evidence",
    "earlier_company", "alias_jurisdiction_changed", "unexplained_prior_correction",
])
def test_strict_preconditions_block_without_partial_changes(context, issue):
    db, snapshot, stocks, kwargs = context
    snapshot = deepcopy(snapshot)
    company = db.scalar(select(FoundationEntity))
    alias = db.scalar(select(SourceIdentity))
    baseline = set()
    if issue == "baseline_company":
        baseline.add(company.id)
    elif issue == "different_name":
        snapshot["records"][1]["source_record"]["ORG_NAME"] = "Different issuer name"
    elif issue == "different_issuer":
        snapshot["records"][1]["source_record"]["ORG_CODE"] = "ORG-OTHER"
    elif issue == "missing_reg_place":
        snapshot["records"][1]["source_record"].pop("REG_PLACE")
    elif issue == "conflicting_reg_place":
        extra = deepcopy(snapshot["records"][1])
        extra["symbol"], extra["source_record"]["SECUCODE"] = "00884", "00884.HK"
        extra["source_record"]["REG_PLACE"] = "开曼群岛"
        extra["company"]["jurisdiction"] = "KY"
        snapshot["records"].append(extra)
    elif issue == "domestic_reg_number":
        snapshot["records"][0]["source_record"]["REG_NUM"] = "a registry reference"
    elif issue == "registered_company":
        company.identifier_scheme = "CN_USCC"
    elif issue == "other_alias":
        alias.namespace = "REGISTRY:CN:CN_USCC"
    elif issue == "other_snapshot":
        db.get(CompanyMappingState, stocks[0].id).source_snapshot = "different-snapshot.json"
    elif issue == "modified_evidence":
        db.scalar(select(FoundationEvidence)).content = "{}"
    elif issue == "earlier_company":
        company.created_at -= timedelta(days=1)
    elif issue == "alias_jurisdiction_changed":
        alias.jurisdiction = "OTHER"
    elif issue == "unexplained_prior_correction":
        company.jurisdiction = alias.jurisdiction = "中国香港特别行政区"
    db.commit()
    old_company, old_alias = company.jurisdiction, alias.jurisdiction
    result = reconcile(db, snapshot, baseline, apply=True, **kwargs)
    assert not result["passed"] and not result["applied"] and result["rejected"]
    assert company.jurisdiction == old_company and alias.jurisdiction == old_alias
    assert not db.dirty and not db.new
    assert db.scalar(select(func.count()).select_from(FoundationEvidence)) == 1
    assert db.scalar(select(func.count()).select_from(PipelineRun)) == 1


def test_one_invalid_target_blocks_entire_apply_batch(context):
    db, snapshot, _, kwargs = context
    kwargs["symbols"] = ["600938", "600941"]
    result = reconcile(db, snapshot, set(), apply=True, **kwargs)
    assert not result["passed"] and result["eligible"] == 1 and result["requested"] == 2
    assert db.scalar(select(FoundationEntity)).jurisdiction == "CN"
    assert db.scalar(select(func.count()).select_from(FoundationEvidence)) == 1
