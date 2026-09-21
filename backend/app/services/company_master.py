"""Evidence-backed issuer mappings across the existing security universe.

Never manufactures issuer names from ticker abbreviations. The batch importer
only adds missing identities and records a reason for every unresolved stock.
"""

from collections import Counter, defaultdict
from datetime import datetime, timezone
import json
import re

from sqlalchemy import func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.models.company_mapping import CompanyMappingState
from app.models.foundation import FoundationEntity, FoundationListing, FoundationSecurity
from app.models.market_data import StockSymbol
from app.schemas.company_graph import SourceRecord
from app.services import company_graph, foundation
from app.connectors.company_master_sources import expected_secucode, HK_EQUITY_SUBCATEGORIES, CN_EQUITY_TYPES


def snapshot_index(snapshot: dict) -> tuple[dict, dict]:
    records = defaultdict(list)
    for record in snapshot.get("records", []):
        records[(record["market"], record["symbol"])].append(record)
    exclusions = {(row.get("market", "HK"), row["symbol"]): row for row in snapshot.get("exclusions", [])}
    return dict(records), exclusions


def _identity(record: dict) -> tuple:
    company = record["company"]
    return (record.get("source_name", "EASTMONEY"), company.get("source_issuer_id"),
        company.get("jurisdiction"), company.get("identifier_scheme"), company.get("identifier_value"))


def _normalized(stock: StockSymbol, record: dict, snapshot: dict) -> SourceRecord:
    if record.get("market") != stock.market or record.get("symbol") != stock.symbol:
        raise ValueError("来源证券与目标股票不一致")
    raw = record.get("source_record") or {}
    secucode = str(raw.get("SECUCODE") or "")
    if secucode != expected_secucode(stock.market, stock.symbol):
        raise ValueError("原始来源证券代码或市场不匹配")
    classification = record.get("security_classification_evidence")
    if stock.market == "HK":
        classified = (classification or {}).get("source_record", {})
        if (classified.get("Stock Code") != stock.symbol or classified.get("Category") != "Equity"
                or classified.get("Sub-Category") not in HK_EQUITY_SUBCATEGORIES):
            raise ValueError("港股缺少可核对的官方普通股分类证据")
    elif raw.get("SECURITY_TYPE") not in ({"三板股"} if stock.market.startswith("NEEQ") else CN_EQUITY_TYPES):
        raise ValueError("来源证券类型不是对应市场的公司股票")
    company = record["company"]
    if not raw.get("ORG_CODE") or str(raw["ORG_CODE"]) != str(company.get("source_issuer_id")):
        raise ValueError("缺少一致的来源发行主体编号")
    if not raw.get("ORG_NAME") or str(raw["ORG_NAME"]).strip() != company.get("name"):
        raise ValueError("缺少来源公司全称，不能按证券简称建立公司")
    observed = record.get("observed_at") or snapshot.get("retrieved_at")
    evidence = record.get("evidence") or {}
    source_name = record.get("source_name") or snapshot.get("source_name") or "EASTMONEY"
    is_dr = raw.get("SECURITY_TYPE") == "中国存托凭证" or (classification or {}).get("source_record", {}).get("Sub-Category") == "Depositary Receipts"
    share_class = "DEPOSITARY_RECEIPT" if is_dr else "A_SHARE" if stock.market == "CN_A" else "NEEQ_SHARE" if stock.market.startswith("NEEQ") else "ORDINARY"
    return SourceRecord.model_validate({
        "source_name": source_name, "source_key": evidence.get("source_key") or f"company:{raw['ORG_CODE']}:{stock.market}:{stock.symbol}",
        "source_url": record.get("source_url") or snapshot.get("source_url"), "source_kind": "AGGREGATOR",
        "observed_at": observed, "company": company,
        "security": {"stock_symbol_id": stock.id, "share_class": share_class},
        "evidence": {"title": f"{company['name']}（{stock.market}:{stock.symbol}）发行主体资料",
            "content": json.dumps({**raw, **({"_security_classification_evidence": classification} if classification else {})},
                ensure_ascii=False, sort_keys=True, allow_nan=False),
            "available_at": observed, "published_at": None},
    })


def _exclusion_result(stock: StockSymbol, exclusion: dict) -> dict:
    raw = exclusion.get("source_record") or {}
    valid, reason = False, "非普通股排除记录缺少一致的证券分类或结算编号证据"
    if stock.market == "HK" and exclusion.get("source_name") == "HKEX_SDW":
        original = stock.raw_payload or {}
        metadata = stock.ext_json or {}
        reference = re.search(r"\(A\s*#\s*(\d{6})\)\s*$", str(raw.get("n") or ""))
        valid = (metadata.get("source_endpoint") == "HKEX_SDW" and raw == original
            and str(raw.get("c") or "").zfill(5) == stock.symbol and reference is not None
            and exclusion.get("referenced_market") == "CN_A"
            and exclusion.get("referenced_symbol") == reference.group(1))
        if valid:
            reason = "港交所结算系统的内地证券编号，不是港股上市代码"
    elif stock.market == "HK" and exclusion.get("source_name") == "HKEX":
        valid = (raw.get("Stock Code") == stock.symbol and bool(raw.get("Category"))
            and (raw.get("Category") != "Equity" or (bool(raw.get("Sub-Category"))
                and raw.get("Sub-Category") not in HK_EQUITY_SUBCATEGORIES)))
        if valid:
            reason = "港交所分类为非普通股产品，不适用普通股发行公司映射"
    return {"status": "NOT_APPLICABLE" if valid else "CONFLICT", "reason": reason,
        "source_name": exclusion.get("source_name"), "source_record": raw,
        "source_url": exclusion.get("source_url"), "observed_at": exclusion.get("observed_at"),
        "exclusion_evidence": exclusion}


def import_master_batch(db: Session, stocks: list[StockSymbol], snapshot: dict, *,
                        records: dict | None = None, exclusions: dict | None = None,
                        snapshot_name: str = "") -> dict:
    """Flush only; caller commits bounded batches and records the final run."""
    if records is None or exclusions is None:
        records, exclusions = snapshot_index(snapshot)
    unresolved = {(item["market"], item["symbol"]): item for item in snapshot.get("unresolved", [])}
    items = []
    for stock in stocks:
        key = (stock.market, stock.symbol)
        mapping = db.scalar(select(FoundationListing).where(FoundationListing.stock_symbol_id == stock.id))
        row = {"stock_symbol_id": stock.id, "market": stock.market, "symbol": stock.symbol}
        if mapping:
            row.update(status="MAPPED", reason="已有关联保持不变", company_id=db.get(FoundationSecurity, mapping.security_id).entity_id)
        elif key in exclusions:
            row.update(_exclusion_result(stock, exclusions[key]))
        elif key not in records:
            issue = unresolved.get(key, {})
            reason = {"HKEX_SECURITY_CLASSIFICATION_MISSING": "当前港交所证券名单中缺少该代码，需补充历史证券分类及主体证据",
                "COMPANY_PROFILE_MISSING": "来源缺少该市场及代码的公司资料，待补充发行主体证据",
                "CONFLICTING_PROVIDER_IDENTITIES": "来源返回多个不同发行主体，需核对身份",
                "SECURITY_CLASSIFICATION_MISMATCH": "来源证券类型与目标市场不符，需核对身份"}.get(issue.get("reason"),
                    "本次主数据快照未提供可核对的发行主体资料，待补充来源")
            status = "CONFLICT" if issue.get("reason") in {"CONFLICTING_PROVIDER_IDENTITIES", "SECURITY_CLASSIFICATION_MISMATCH"} else "SOURCE_MISSING"
            row.update(status=status, reason=reason, source_issue=issue)
        else:
            candidates = records[key]
            if len({_identity(record) for record in candidates}) != 1:
                row.update(status="CONFLICT", reason="同一证券返回多个不同发行主体标识，需要人工核对")
            else:
                record = candidates[0]
                try:
                    normalized = _normalized(stock, record, snapshot)
                    # A single invalid record rolls back only its own new objects.
                    with db.begin_nested():
                        imported = company_graph.import_batch(db, {"records": [normalized]})["records"][0]
                    row.update(status="MAPPED", reason="依据来源主体编号及公司全称建立映射", company_id=imported["company_id"],
                        evidence_id=imported["evidence_id"], source_name=normalized.source_name)
                except (ValueError, foundation.FoundationError, IntegrityError) as error:
                    row.update(status="CONFLICT", reason="来源身份校验未通过：" + str(error)[:800])
        state = db.get(CompanyMappingState, stock.id)
        if state is None:
            state = CompanyMappingState(stock_symbol_id=stock.id, attempt_count=0)
            db.add(state)
        state.status, state.reason = row["status"], row["reason"]
        state.source_name = row.get("source_name")
        state.source_snapshot = snapshot_name[:512]
        state.attempt_count += 1
        state.attempted_at = datetime.now(timezone.utc)
        state.details_json = {key: value for key, value in row.items() if key not in {"reason", "status"}}
        items.append(row)
    db.flush()
    return {"items": items, "counts": dict(Counter(row["status"] for row in items))}


def mapping_status(db: Session) -> dict:
    markets = list(db.execute(select(StockSymbol.market, func.count(StockSymbol.id), func.count(FoundationListing.id))
        .outerjoin(FoundationListing, FoundationListing.stock_symbol_id == StockSymbol.id).group_by(StockSymbol.market)))
    total = sum(row[1] for row in markets)
    mapped = sum(row[2] for row in markets)
    outcomes = list(db.execute(select(CompanyMappingState.status, func.count()).group_by(CompanyMappingState.status)))
    return {"total": total, "mapped": mapped, "unmapped": total - mapped,
        "companies": db.scalar(select(func.count()).select_from(FoundationEntity).where(FoundationEntity.entity_type == "COMPANY")),
        "by_market": [{"market": market, "total": count, "mapped": linked} for market, count, linked in markets],
        "outcomes": [{"status": status, "count": count} for status, count in outcomes],
        "identity_basis": "SOURCE_IDENTIFIERS_NOT_OFFICIAL_REGISTRY_VERIFICATION"}
