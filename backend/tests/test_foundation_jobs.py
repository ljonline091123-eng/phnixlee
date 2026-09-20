import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, func, select
from sqlalchemy.orm import Session
from sqlalchemy.pool import StaticPool

from app.db.base import Base
from app.db.session import get_db
from app.main import app
from app.models.ai_hub import KnowledgeBase, KnowledgeDocument, KnowledgeGraph
from app.models.foundation import FoundationEvidence
from app.models.pipeline import ScheduledJob
from app.orchestration.foundation import ArchiveRequest, archive_batch, enqueue_archive


@pytest.fixture
def archive_db():
    engine = create_engine('sqlite://', connect_args={'check_same_thread': False}, poolclass=StaticPool)
    Base.metadata.create_all(engine)
    with Session(engine) as db:
        kb = KnowledgeBase(kb_code='ARCHIVE_TEST', kb_name='Archive test', source_tables=['stock_news'])
        db.add(kb)
        db.flush()
        graph = KnowledgeGraph(knowledge_base_id=kb.id, graph_code='ARCHIVE_TEST', graph_name='Archive test')
        db.add(graph)
        db.flush()
        db.add(KnowledgeDocument(knowledge_base_id=kb.id, graph_id=graph.id, source_table='stock_news',
            source_record_id=45, market='CN_A', symbol='TEST001', title='Archive test evidence',
            content='  Source text retains whitespace.\n', metadata_json={'content_truncated': True}))
        db.commit()
        yield db
    engine.dispose()


def test_archive_submission_and_worker_are_idempotent(archive_db):
    db = archive_db
    document = db.scalar(select(KnowledgeDocument))
    request = ArchiveRequest(document_ids=[document.id, document.id])
    first = enqueue_archive(db, request)
    second = enqueue_archive(db, request)
    assert first.id == second.id
    assert db.scalar(select(func.count()).select_from(ScheduledJob)) == 1
    initial = archive_batch(db, first.payload_json)
    db.commit()
    repeated = archive_batch(db, first.payload_json)
    db.commit()
    assert initial['evidence_ids'] == repeated['evidence_ids']
    evidence = db.get(FoundationEvidence, initial['evidence_ids'][0])
    assert evidence.content == document.content
    assert evidence.metadata_json['content_truncated'] is True
    assert db.scalar(select(func.count()).select_from(FoundationEvidence)) == 1


def test_archive_rejects_changed_or_reused_legacy_document_id(archive_db):
    db = archive_db
    document = db.scalar(select(KnowledgeDocument))
    job = enqueue_archive(db, ArchiveRequest(document_ids=[document.id]))
    document.content = 'Different source after graph rebuild'
    db.commit()
    with pytest.raises(ValueError, match='changed'):
        archive_batch(db, job.payload_json)
    assert db.scalar(select(func.count()).select_from(FoundationEvidence)) == 0
    new_job = enqueue_archive(db, ArchiveRequest(document_ids=[document.id]))
    assert new_job.id != job.id


def test_archive_endpoints_are_bounded_and_do_not_run_the_worker(archive_db):
    db = archive_db
    document = db.scalar(select(KnowledgeDocument))
    app.dependency_overrides[get_db] = lambda: db
    client = TestClient(app)
    try:
        response = client.post('/api/v1/foundation/evidence/archive-jobs', json={'document_ids': [document.id]})
        assert response.status_code == 202, response.text
        job = response.json()
        assert job['status'] == 'PENDING'
        assert job['document_count'] == 1
        assert db.scalar(select(func.count()).select_from(FoundationEvidence)) == 0
        detail = client.get(f'/api/v1/foundation/evidence/archive-jobs/{job["id"]}')
        assert detail.status_code == 200
        assert detail.json()['status'] == 'PENDING'
        excessive = client.post('/api/v1/foundation/evidence/archive-jobs', json={'document_ids': list(range(1, 102))})
        assert excessive.status_code == 422
        missing = client.post('/api/v1/foundation/evidence/archive-jobs', json={'document_ids': [999999]})
        assert missing.status_code == 404
        unknown = client.get('/api/v1/foundation/evidence/archive-jobs/999999')
        assert unknown.status_code == 404
    finally:
        client.close()
        app.dependency_overrides.clear()
