"""Correct only new source-scoped issuers from one explicitly selected import.

Default is read-only. An apply changes jurisdiction on the existing company and
its source alias, adds separate evidence/audit metadata, and retains all UUIDs,
listings and original evidence. Any failed precondition blocks the whole batch.
"""

import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import sqlite3
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.connectors.company_master_sources import expected_secucode
from app.connectors.company_sources import _jurisdiction
from app.models.company_graph import SourceIdentity
from app.models.company_mapping import CompanyMappingState
from app.models.foundation import FoundationEntity, FoundationEvidence, FoundationListing, FoundationSecurity
from app.models.market_data import StockSymbol
from app.models.pipeline import PipelineRun
from app.schemas.foundation import EvidenceCreate
from app.services import foundation
from app.services.company_master import _normalized
from scripts.migrate_foundation import target_engine


ALLOWED_SYMBOLS = frozenset({"600938", "600941", "688981", "688347", "688235", "688428"})
SOURCE = "EASTMONEY"
AUDIT_SOURCE = "COMPANY_MASTER_JURISDICTION_RECONCILIATION"
RULE = "SAME_SNAPSHOT_EXPLICIT_REG_PLACE_V1"
SOURCE_SCHEME = "SOURCE:" + hashlib.sha256(SOURCE.encode()).hexdigest()[:24].upper()


class ReconciliationSafetyError(ValueError):
    pass


def require(condition, reason):
    if not condition:
        raise ReconciliationSafetyError(reason)


def same_path(left, right):
    return bool(left and right) and Path(left).resolve() == Path(right).resolve()


def file_hash(path):
    digest = hashlib.sha256()
    with Path(path).open("rb") as handle:
        for part in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(part)
    return digest.hexdigest()


def baseline_entities(path):
    path = Path(path).resolve(strict=True)
    with sqlite3.connect(path.as_uri() + "?mode=ro", uri=True) as db:
        db.execute("PRAGMA query_only=ON")
        return {row[0] for row in db.execute("SELECT id FROM foundation_entity")}


def _plan(db, snapshot, baseline_ids, snapshot_name, snapshot_sha256, symbol):
    require(symbol in ALLOWED_SYMBOLS, "symbol is outside the explicitly scoped correction set")
    require(snapshot.get("source_code") == "EASTMONEY_COMPANY_MASTER" and
        snapshot.get("source_name") == SOURCE, "snapshot source is not the expected issuer collector")
    records = snapshot.get("records") or []
    domestic = [row for row in records if row.get("market") == "CN_A" and row.get("symbol") == symbol]
    require(len(domestic) == 1, "expected one domestic source record")
    domestic = domestic[0]
    raw, company_record = domestic.get("source_record") or {}, domestic.get("company") or {}
    issuer, name = str(raw.get("ORG_CODE") or ""), raw.get("ORG_NAME")
    require(bool(issuer and name) and company_record.get("name") == name and
        str(company_record.get("source_issuer_id")) == issuer and domestic.get("source_name") == SOURCE,
        "domestic raw issuer ID and exact company name are not consistent")
    require(raw.get("SECUCODE") == expected_secucode("CN_A", symbol), "domestic raw security code differs")
    require(not raw.get("REG_PLACE") and not raw.get("REG_NUM") and
        not company_record.get("identifier_scheme") and not company_record.get("identifier_value") and
        company_record.get("jurisdiction") == "CN", "domestic record is not the source-only CN fallback")
    family = [row for row in records if str((row.get("source_record") or {}).get("ORG_CODE") or "") == issuer]
    require(all(row.get("source_name") == SOURCE and (row.get("source_record") or {}).get("ORG_NAME") == name
        and (row.get("company") or {}).get("name") == name
        and str((row.get("company") or {}).get("source_issuer_id")) == issuer for row in family),
        "same source issuer ID has conflicting company names or namespaces")
    hk = [row for row in family if row.get("market") == "HK"]
    require(bool(hk), "snapshot has no matching Hong Kong issuer record")
    jurisdictions = set()
    for record in hk:
        hk_raw, hk_company = record["source_record"], record["company"]
        explicit = hk_raw.get("REG_PLACE")
        require(isinstance(explicit, str) and bool(explicit.strip()), "Hong Kong record lacks explicit REG_PLACE")
        require(hk_raw.get("SECUCODE") == expected_secucode("HK", record["symbol"]), "Hong Kong security code differs")
        require(not hk_company.get("identifier_scheme") and not hk_company.get("identifier_value"),
            "Hong Kong identity contains a registration identifier requiring separate reconciliation")
        target = str(hk_company.get("jurisdiction") or "").upper()
        require(target == _jurisdiction(hk_raw, "HK").upper() and target not in {"", "CN", "UNKNOWN"},
            "normalized Hong Kong jurisdiction is not backed by explicit non-CN REG_PLACE")
        jurisdictions.add(target)
    require(len(jurisdictions) == 1, "Hong Kong records disagree on issuer jurisdiction")
    target = jurisdictions.pop()
    stock = db.scalar(select(StockSymbol).where(StockSymbol.market == "CN_A", StockSymbol.symbol == symbol))
    require(stock is not None, "target security does not exist")
    listing = db.scalar(select(FoundationListing).where(FoundationListing.stock_symbol_id == stock.id))
    require(listing is not None, "target security has no existing mapping to correct")
    security = db.get(FoundationSecurity, listing.security_id)
    require(security is not None, "target listing has no security")
    company = db.get(FoundationEntity, security.entity_id)
    require(company is not None and company.id not in baseline_ids, "company exists in the baseline or is missing; correction prohibited")
    props = company.properties_json or {}
    require(company.entity_type == "COMPANY" and company.name == name and company.identifier_scheme == SOURCE_SCHEME
        and company.identifier_value == issuer and props.get("identity_basis") == "SOURCE_SCOPED_IDENTIFIER"
        and props.get("identity_source") == SOURCE and str(props.get("source_issuer_id")) == issuer
        and not props.get("registration_number_raw") and not props.get("registration_place")
        and not props.get("registration_verified"), "company is not an unchanged source-only fallback identity")
    aliases = list(db.scalars(select(SourceIdentity).where(SourceIdentity.entity_id == company.id)))
    require(len(aliases) == 1 and aliases[0].namespace == SOURCE and aliases[0].external_id == issuer
        and aliases[0].entity_type == "COMPANY", "company has registry/other aliases outside correction scope")
    audit_key = f"{RULE}:{snapshot_sha256}:{company.id}"
    prior = db.scalar(select(FoundationEvidence).where(FoundationEvidence.source_name == AUDIT_SOURCE,
        FoundationEvidence.source_key == audit_key, FoundationEvidence.entity_id == company.id))
    already_applied = company.jurisdiction == target and aliases[0].jurisdiction == target
    if already_applied:
        require(prior is not None and props.get("jurisdiction_reconciliation", {}).get("evidence_id") == prior.id
            and prior.metadata_json.get("target_jurisdiction") == target, "jurisdiction already differs without this correction audit")
    else:
        require(company.jurisdiction == "CN" and all(alias.jurisdiction == "CN" for alias in aliases)
            and prior is None and "jurisdiction_reconciliation" not in props, "company/alias jurisdiction or audit changed since import")
    runs = [run for run in db.scalars(select(PipelineRun).where(PipelineRun.pipeline_type == "company_master_mapping",
        PipelineRun.status == "COMPLETED")) if same_path((run.input_json or {}).get("snapshot"), snapshot_name)]

    def created_in_snapshot_run(timestamp):
        return any(run.started_at and run.completed_at and foundation.aware(run.started_at) <= foundation.aware(timestamp)
            <= foundation.aware(run.completed_at) for run in runs)

    require(created_in_snapshot_run(company.created_at) and all(created_in_snapshot_run(alias.created_at) for alias in aliases),
        "company or source alias was not created during a completed import of this snapshot")
    listings = list(db.scalars(select(FoundationListing).join(FoundationSecurity,
        FoundationSecurity.id == FoundationListing.security_id).where(FoundationSecurity.entity_id == company.id)))
    require(bool(listings), "company has no listings")
    by_key = {}
    for record in family:
        by_key.setdefault((record["market"], record["symbol"]), []).append(record)
    proofs = []
    for item in listings:
        candidates = by_key.get((item.market, item.symbol), [])
        require(len(candidates) == 1, "an existing company listing is not uniquely present in this snapshot")
        linked_stock = db.get(StockSymbol, item.stock_symbol_id)
        linked_security = db.get(FoundationSecurity, item.security_id)
        state = db.get(CompanyMappingState, item.stock_symbol_id)
        require(linked_stock is not None and linked_stock.market == item.market and linked_stock.symbol == item.symbol
            and state is not None and state.status == "MAPPED" and same_path(state.source_snapshot, snapshot_name),
            "listing master data or mapping state is outside this snapshot")
        expected = _normalized(linked_stock, candidates[0], snapshot)
        proof = db.get(FoundationEvidence, item.evidence_id)
        require(proof is not None and proof.entity_id == company.id and proof.source_name == SOURCE
            and proof.source_key == expected.source_key and json.loads(proof.content) == json.loads(expected.evidence.content),
            "listing evidence does not match the exact snapshot source record")
        require(created_in_snapshot_run(item.created_at) and created_in_snapshot_run(linked_security.created_at)
            and created_in_snapshot_run(proof.created_at), "listing/security/evidence predates or is outside this snapshot import")
        proofs.append(proof.id)
    return {"company": company, "aliases": aliases, "listings": listings, "source_evidence_ids": proofs,
        "symbol": symbol, "target": target, "audit_key": audit_key, "prior": prior,
        "domestic_source": raw, "hk_sources": [row["source_record"] for row in hk],
        "source_urls": sorted({row.get("source_url", "") for row in [domestic, *hk]}),
        "already_applied": already_applied}


def reconcile(db, snapshot, baseline_ids, *, snapshot_name, snapshot_sha256, baseline_sha256,
              symbols=None, apply=False):
    """Validate the full batch before adding audit or changing any identity."""
    symbols = sorted(set(symbols or ALLOWED_SYMBOLS))
    plans, rejected = [], []
    for symbol in symbols:
        try:
            plans.append(_plan(db, snapshot, baseline_ids, snapshot_name, snapshot_sha256, symbol))
        except (ValueError, KeyError, TypeError, foundation.FoundationError) as error:
            rejected.append({"symbol": symbol, "status": "REJECTED", "reason": str(error)[:500]})
    if len({plan["company"].id for plan in plans}) != len(plans):
        rejected.append({"status": "REJECTED", "reason": "multiple requested domestic securities resolve to the same company"})
    result = {"passed": not rejected, "applied": False, "requested": len(symbols), "eligible": len(plans),
        "rejected": rejected, "items": [{"symbol": plan["symbol"], "previous_jurisdiction": "CN",
            "target_jurisdiction": plan["target"], "existing_listings": len(plan["listings"]),
            "status": "ALREADY_APPLIED" if plan["already_applied"] else "ELIGIBLE"} for plan in plans]}
    if not apply or rejected:
        return result
    changed = [plan for plan in plans if not plan["already_applied"]]
    evidence_ids = []
    for plan in changed:
        company = plan["company"]
        content = {"rule": RULE, "previous_jurisdiction": "CN", "target_jurisdiction": plan["target"],
            "source_snapshot_sha256": snapshot_sha256, "baseline_sha256": baseline_sha256,
            "domestic_source_record": plan["domestic_source"], "hk_source_records": plan["hk_sources"],
            "original_listing_evidence_ids": plan["source_evidence_ids"], "source_urls": plan["source_urls"]}
        proof = foundation.create_evidence(db, EvidenceCreate(entity_id=company.id, source_name=AUDIT_SOURCE,
            source_key=plan["audit_key"], title="发行主体注册地纠错依据（同来源主体编号及完整公司名称）",
            content=json.dumps(content, ensure_ascii=False, sort_keys=True),
            url=plan["source_urls"][0] or None), metadata={"content_scope": "SOURCE_RECORD_COMPARISON",
                "completeness": "PROVIDER_FIELDS_ONLY", "rule": RULE, "source_kind": "AGGREGATOR_RECONCILIATION",
                "previous_jurisdiction": "CN", "target_jurisdiction": plan["target"],
                "source_snapshot_sha256": snapshot_sha256, "baseline_sha256": baseline_sha256,
                "registration_verified": False, "preserved_listing_ids": [item.id for item in plan["listings"]]})
        company.jurisdiction = plan["target"]
        company.properties_json = {**company.properties_json, "jurisdiction_reconciliation": {
            "rule": RULE, "evidence_id": proof.id, "source_snapshot_sha256": snapshot_sha256,
            "previous_jurisdiction": "CN", "target_jurisdiction": plan["target"]}}
        for alias in plan["aliases"]:
            alias.jurisdiction = plan["target"]
        evidence_ids.append(proof.id)
    if changed:
        now = datetime.now(timezone.utc)
        db.add(PipelineRun(pipeline_type="company_master_jurisdiction_reconciliation", trigger_type="MANUAL",
            status="COMPLETED", started_at=now, completed_at=now,
            input_json={"rule": RULE, "snapshot": snapshot_name, "snapshot_sha256": snapshot_sha256,
                "baseline_sha256": baseline_sha256, "symbols": symbols},
            output_json={"corrected": len(changed), "company_ids": [plan["company"].id for plan in changed],
                "evidence_ids": evidence_ids, "original_evidence_unchanged": True}))
        db.flush()
    result.update(applied=True, corrected=len(changed), added_evidence=len(evidence_ids))
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--database-url", required=True)
    parser.add_argument("--baseline", required=True)
    parser.add_argument("--snapshot", required=True)
    parser.add_argument("--symbols", nargs="+", choices=sorted(ALLOWED_SYMBOLS), default=sorted(ALLOWED_SYMBOLS))
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--report")
    args = parser.parse_args()
    snapshot_path = Path(args.snapshot).resolve(strict=True)
    baseline_path = Path(args.baseline).resolve(strict=True)
    snapshot = json.loads(snapshot_path.read_text(encoding="utf-8"))
    baseline_ids = baseline_entities(baseline_path)
    engine = target_engine(args.database_url, read_only=not args.apply)
    protected_paths = {snapshot_path, baseline_path}
    if engine.url.get_backend_name() == "sqlite":
        target = Path(engine.url.database.removeprefix("file:")).resolve()
        require(target != baseline_path, "target database cannot be the preservation baseline")
        protected_paths.add(target)
    if args.report:
        require(Path(args.report).resolve() not in protected_paths, "report cannot overwrite database/snapshot/baseline")
    try:
        with engine.connect() as connection, connection.begin():
            if args.apply and engine.dialect.name == "sqlite":
                connection.exec_driver_sql("BEGIN IMMEDIATE")
            elif engine.dialect.name == "postgresql":
                connection.exec_driver_sql("SELECT pg_advisory_xact_lock(710204002)" if args.apply else "SET TRANSACTION READ ONLY")
            with Session(bind=connection, autoflush=False) as db:
                result = reconcile(db, snapshot, baseline_ids, snapshot_name=str(snapshot_path),
                    snapshot_sha256=file_hash(snapshot_path), baseline_sha256=file_hash(baseline_path),
                    symbols=args.symbols, apply=args.apply)
                if args.apply and result["passed"]:
                    db.flush()
        if args.report:
            Path(args.report).write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
        print(json.dumps(result, ensure_ascii=True))
        return 0 if result["passed"] else 2
    finally:
        engine.dispose()


if __name__ == "__main__":
    raise SystemExit(main())
