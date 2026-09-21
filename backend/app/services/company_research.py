"""Read-only, optional bridge from accepted company facts into research."""

from datetime import datetime, timezone
from sqlalchemy import inspect, or_, select
from sqlalchemy.exc import SQLAlchemyError
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.models.foundation import FoundationEntity, FoundationFact, FoundationListing, FoundationSecurity
from app.services import foundation


def load_company_context(db: Session, market: str, symbol: str) -> dict:
    settings = get_settings()
    if not settings.data_foundation_enabled or not settings.company_research_context_enabled:
        return {}
    required = {"foundation_entity", "foundation_security", "foundation_listing",
                "foundation_fact", "foundation_fact_evidence", "foundation_evidence", "foundation_fact_review"}
    try:
        if not required.issubset(set(inspect(db.connection()).get_table_names())):
            return {"status": "NOT_INITIALIZED", "facts": [], "warnings": ["Company foundation is not initialized."]}
        with db.begin_nested():
            listing = db.scalar(select(FoundationListing).where(FoundationListing.market == market, FoundationListing.symbol == symbol))
            if not listing:
                return {"status": "UNMAPPED", "facts": [], "warnings": ["No issuer mapping for this security."]}
            security = db.get(FoundationSecurity, listing.security_id)
            company = db.get(FoundationEntity, security.entity_id)
            now = datetime.now(timezone.utc)
            temporal_scope = FoundationFact.properties_json["temporal_scope"].as_string()
            rows = list(db.scalars(foundation.facts_query(entity_id=company.id, status="ACCEPTED",
                as_of=now.date(), known_at=now).where(or_(
                    FoundationFact.fact_type != "HOLDS_EQUITY", temporal_scope.is_(None),
                    temporal_scope != "SOURCE_SNAPSHOT_UNKNOWN_BUSINESS_DATE",
                )).limit(21)))
            facts = []
            for row in rows[:20]:
                item = foundation.fact_read(db, row, known_at=now)
                item["evidence"] = [{"id": proof.id, "title": proof.title, "source": proof.source_name,
                    "url": proof.url, "published_at": foundation.aware(proof.published_at),
                    "available_at": foundation.aware(proof.available_at)}
                    for proof in foundation.fact_evidence(db, row.id)]
                facts.append(item)
            return {"status": "AVAILABLE", "company_id": company.id, "company_name": company.name,
                "listing_id": listing.id, "as_of": now.isoformat(), "facts": facts, "truncated": len(rows) > 20,
                "warnings": ["Source-reported observations only; ownership dates and scopes must be preserved.",
                    "Missing relationships or litigation are unknown, not negative evidence.",
                    "Pending legal or business disclosures are excluded from accepted research facts."]}
    except SQLAlchemyError:
        return {"status": "UNAVAILABLE", "facts": [], "warnings": ["Company relationship context unavailable; original research continues."]}


def company_context_markdown(context: dict) -> str:
    if context.get("status") != "AVAILABLE":
        return ""
    lines = ["", "## 公司关系证据", "", f"发行主体：{context['company_name']}。"]
    if not context.get("facts"):
        lines.append("当前暂无已接受的关系事实；不能据此判断不存在股权、经营或司法关系。")
    for item in context.get("facts", [])[:10]:
        lines.append(f"- {item['subject_name']} → {item['fact_type']} → {item.get('object_name') or '事项'}；{item['title']}。")
        if item.get("properties_json", {}).get("report_date"):
            lines.append(f"  披露观察期：{item['properties_json']['report_date']}，不代表持仓至今未变。")
        for proof in item.get("evidence", [])[:2]:
            lines.append(f"  证据：{proof['source']} / {proof['title']} / {proof['id']}。")
    lines.append("关系仅按所列来源与时间解释，不据此推断受益人、损失金额或价格因果。")
    return "\n".join(lines)
