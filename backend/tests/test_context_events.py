from datetime import datetime, timedelta, timezone

import pytest
from pydantic import ValidationError
from sqlalchemy import create_engine, func, select
from sqlalchemy.orm import Session

from app.db.base import Base
from app.models.context_event import StockContextEvent
from app.models.ai_hub import KnowledgeBase, KnowledgeGraph
from app.models.market_data import DataSource, StockSymbol
from app.schemas.context_events import ContextEventCreate, ContextEventRead
from app.services.context_events import import_context_event


def payload(**changes):
    values = dict(market="CN_A", symbol="DEMO001", event_type="POLICY", title="演示政策原文", content="此为测试用虚构政策原文，仅作导入验证，不代表实际事件。", source_name="演示机构", url="https://example.invalid/policy", published_at=datetime(2025, 1, 1, tzinfo=timezone.utc))
    return ContextEventCreate(**{**values, **changes})


@pytest.mark.parametrize("changes", [
    {"source_name": "  "}, {"content": " "}, {"url": "javascript:alert(1)"},
    {"published_at": datetime.now(timezone.utc) + timedelta(days=1)},
    {"published_at": datetime(2025, 1, 1)},
])
def test_rejects_untraceable_or_invalid_evidence(changes):
    with pytest.raises(ValidationError):
        payload(**changes)


def test_import_preserves_long_content_dedupes_and_marks_unlocked_graph_pending():
    engine = create_engine("sqlite://")
    Base.metadata.create_all(engine)
    try:
        with Session(engine) as db:
            source = DataSource(source_code="DEMO", source_name="演示", adapter_type="MOCK")
            db.add(source); db.flush()
            db.add(StockSymbol(market="CN_A", symbol="DEMO001", name="演示证券", exchange="DEMO", source_id=source.id))
            kb = KnowledgeBase(kb_code="DEMO", kb_name="演示", source_tables=["stock_context_event"])
            db.add(kb); db.flush()
            graph = KnowledgeGraph(knowledge_base_id=kb.id, graph_code="DEMO_G", graph_name="演示", governance_status="GOVERNED")
            locked = KnowledgeGraph(knowledge_base_id=kb.id, graph_code="LOCKED", graph_name="锁定", governance_status="LOCKED")
            db.add_all([graph, locked]); db.commit()
            content = "虚构测试内容" * 2000 + "完整结尾"
            row = import_context_event(db, payload(content=content))
            same = import_context_event(db, payload(content=content))
            assert row.id == same.id
            assert db.scalar(select(func.count()).select_from(StockContextEvent)) == 1
            assert ContextEventRead.model_validate(row).content == content
            assert graph.governance_status == "PENDING"
            assert locked.governance_status == "LOCKED"
            with pytest.raises(ValueError, match="主数据"):
                import_context_event(db, payload(symbol="UNKNOWN"))
    finally:
        engine.dispose()
