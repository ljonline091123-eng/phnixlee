from datetime import datetime, timezone

from sqlalchemy import create_engine
from sqlalchemy.orm import Session
from sqlalchemy.pool import StaticPool

import app.models  # noqa: F401 - register all foreign-key targets
from app.db.base import Base
from app.models.foundation import FoundationEntity, FoundationEvidence
from app.schemas.foundation import FactCreate
from app.services import foundation


def _db() -> Session:
    engine = create_engine(
        "sqlite+pysqlite:///:memory:",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    Base.metadata.create_all(engine)
    return Session(engine)


def test_event_facts_reuse_foundation_fact_and_evidence_contract() -> None:
    with _db() as db:
        company = FoundationEntity(
            name="测试公司",
            entity_type="COMPANY",
            jurisdiction="CN",
            identifier_scheme="TEST",
            identifier_value="company-1",
        )
        db.add(company)
        db.flush()
        evidence = FoundationEvidence(
            entity_id=company.id,
            source_name="TEST_SOURCE",
            source_key="notice:1",
            title="回购公告",
            content="公司公告回购股份。",
            available_at=datetime.now(timezone.utc),
            content_hash="a" * 64,
            fingerprint="b" * 64,
            version=1,
        )
        db.add(evidence)
        db.flush()

        fact = foundation.create_fact(db, FactCreate(
            fact_type="SHAREHOLDER_EVENT",
            title="公司回购股份",
            subject_entity_id=company.id,
            evidence_ids=[evidence.id],
            properties_json={
                "event_type": "SHARE_REPURCHASE",
                "observed_at": "2026-10-07T01:00:00Z",
                "direction": "POSITIVE",
                "extraction_method": "SOURCE",
                "verification_status": "CANDIDATE",
                "market": "CN_A",
                "symbol": "000001",
                "source_reliability": 1.0,
                "extraction_confidence": 0.95,
                "quantity": 1000000,
                "quantity_unit": "SHARE",
            },
        ))

        assert fact.fact_type == "SHAREHOLDER_EVENT"
        assert fact.status == "PENDING"
        result = foundation.fact_read(db, fact, full=True)
        assert result["evidence_ids"] == [evidence.id]
        assert result["properties_json"]["verification_status"] == "CANDIDATE"


def test_event_fact_rejects_object_entity() -> None:
    with _db() as db:
        company = FoundationEntity(name="主体", entity_type="COMPANY", jurisdiction="CN")
        other = FoundationEntity(name="客体", entity_type="COMPANY", jurisdiction="CN")
        db.add_all([company, other])
        db.flush()
        try:
            FactCreate(
                fact_type="MARKET_EVENT",
                title="放量上涨",
                subject_entity_id=company.id,
                object_entity_id=other.id,
                evidence_ids=["evidence"],
                properties_json={
                    "event_type": "VOLUME_SPIKE",
                    "observed_at": "2026-10-07T01:00:00Z",
                    "direction": "POSITIVE",
                    "extraction_method": "RULE",
                    "verification_status": "CANDIDATE",
                },
            )
        except ValueError as exc:
            assert "uses subject participation" in str(exc)
        else:
            raise AssertionError("event fact must reject object_entity_id")
