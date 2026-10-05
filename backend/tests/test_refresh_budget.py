"""Bounded refresh and classification quote persistence tests."""

from datetime import datetime, timezone
from pathlib import Path
from tempfile import TemporaryDirectory
from types import SimpleNamespace

from sqlalchemy import create_engine, select
from sqlalchemy.orm import sessionmaker

from app.connectors.base import QuoteRecord
from app.db.base import Base
from app.models.market_data import DataSource, StockKline, StockNotice, StockRealtimeQuote, StockSymbol
from app.orchestration.f10 import F10Workflow
from app.services.stock_classification import _persist_classification_quotes, enrich_classification_groups_with_members
from app.services.stock_on_demand import StockOnDemandService


def _database():
    folder = TemporaryDirectory()
    engine = create_engine(
        f"sqlite:///{Path(folder.name) / 'refresh.db'}",
        connect_args={"check_same_thread": False},
    )
    Base.metadata.create_all(engine)
    sessions = sessionmaker(bind=engine)
    db = sessions()
    source = DataSource(
        source_code="AKSHARE",
        source_name="AkShare",
        source_type="MARKET_DATA",
        adapter_type="AKSHARE",
        enabled=True,
        config_json={},
    )
    db.add(source)
    db.flush()
    db.add(StockSymbol(
        market="CN_A", symbol="000002", exchange="SZSE", name="万科A",
        asset_type="STOCK", status="LISTED", list_date="19910129",
        source_id=source.id, ext_json={}, raw_payload={},
        last_synced_at=datetime.now(timezone.utc),
    ))
    db.commit()
    return folder, engine, sessions, db, source


def test_refresh_uses_incremental_kline_and_notice_boundaries(monkeypatch):
    folder, engine, _sessions, db, source = _database()
    try:
        db.add(StockKline(
            market="CN_A", symbol="000002", period="daily", adjust="",
            trade_date="2026-09-30", open_price=1, high_price=1, low_price=1,
            close_price=1, volume=1, amount=1, turnover_rate=1,
            source_id=source.id, raw_payload={}, fetched_at=datetime.now(timezone.utc),
        ))
        db.add(StockNotice(
            market="CN_A", symbol="000002", notice_date="2026-09-20",
            title="测试公告", notice_type="其他公告", url=None, content_json={},
            source_id=source.id, fetched_at=datetime.now(timezone.utc),
        ))
        db.commit()
        observed: dict[str, str] = {}
        success = (SimpleNamespace(total_count=0, persisted_count=0), [])
        monkeypatch.setattr(StockOnDemandService, "fetch_quote", lambda *_args, **_kwargs: success)
        monkeypatch.setattr(StockOnDemandService, "fetch_financials", lambda *_args, **_kwargs: success)
        monkeypatch.setattr(StockOnDemandService, "fetch_news", lambda *_args, **_kwargs: success)

        def fake_kline(_self, **kwargs):
            observed["kline"] = kwargs["start_date"]
            return success

        def fake_notices(_self, **kwargs):
            observed["notices"] = kwargs["start_date"]
            return success

        monkeypatch.setattr(StockOnDemandService, "fetch_kline", fake_kline)
        monkeypatch.setattr(StockOnDemandService, "fetch_notices", fake_notices)
        errors = StockOnDemandService(db).refresh_stock_data(source, "CN_A", "000002")

        assert errors == []
        assert observed["kline"] == "20260927"
        assert observed["notices"] == "20260906"
    finally:
        db.close()
        engine.dispose()
        folder.cleanup()


def test_http_bounded_refresh_only_runs_quote_and_defers_bulk_sources(monkeypatch):
    folder, engine, _sessions, db, source = _database()
    try:
        service = StockOnDemandService(db)
        monkeypatch.setattr(
            StockOnDemandService,
            "fetch_quote",
            lambda *_args, **_kwargs: (SimpleNamespace(total_count=1, persisted_count=1), [{"symbol": "000002"}]),
        )
        monkeypatch.setattr(StockOnDemandService, "fetch_kline", lambda *_args, **_kwargs: (_ for _ in ()).throw(AssertionError("deferred")))
        monkeypatch.setattr(StockOnDemandService, "fetch_financials", lambda *_args, **_kwargs: (_ for _ in ()).throw(AssertionError("deferred")))
        monkeypatch.setattr(StockOnDemandService, "fetch_news", lambda *_args, **_kwargs: (_ for _ in ()).throw(AssertionError("deferred")))
        monkeypatch.setattr(StockOnDemandService, "fetch_notices", lambda *_args, **_kwargs: (_ for _ in ()).throw(AssertionError("deferred")))

        result = service.refresh_stock_data_bounded(source, "CN_A", "000002")

        assert result.status == "PARTIAL"
        assert result.stages[0].status == "SUCCESS"
        assert [item.status for item in result.stages[1:]] == ["DEFERRED"] * 4
    finally:
        db.close()
        engine.dispose()
        folder.cleanup()


def test_extended_refresh_is_deferred_without_starting_provider_work():
    folder, engine, _sessions, db, source = _database()
    try:
        called = False

        def slow_extended(_db, _source, _market, _symbol):
            nonlocal called
            called = True
            return {"profile": {"fields": {"name": "late"}}}

        workflow = F10Workflow(
            db,
            fetch_extended_data=slow_extended,
            symbol_reader=lambda stock, _quote: {"market": stock.market, "symbol": stock.symbol},
        )
        payload, stage = workflow._fetch_extended_bounded(
            source, "CN_A", "000002", timeout_seconds=0.02,
        )

        assert payload is None
        assert stage["status"] == "DEFERRED"
        assert called is False
    finally:
        db.close()
        engine.dispose()
        folder.cleanup()


def test_classification_quote_is_persisted_and_reused_locally(monkeypatch):
    folder, engine, sessions, db, source = _database()
    try:
        quote = QuoteRecord(
            market="CN_A", symbol="000002", quote_time="2026-10-02T15:00:00",
            current_price=6.31, previous_close_price=6.20, open_price=6.19,
            high_price=6.35, low_price=6.10, volume=123, amount=456,
            change_amount=0.11, change_pct=1.77, turnover_rate=0.65,
            raw_payload={"provider": "tencent"},
        )
        assert _persist_classification_quotes(db, {("CN_A", "000002"): quote}) == {("CN_A", "000002")}
        tencent_source = db.scalar(select(DataSource).where(DataSource.source_code == "TENCENT_QUOTE"))
        assert tencent_source is not None

        with sessions() as reopened:
            persisted = reopened.scalar(select(StockRealtimeQuote).where(
                StockRealtimeQuote.market == "CN_A", StockRealtimeQuote.symbol == "000002"
            ))
            assert persisted is not None and persisted.change_pct == 1.77
            assert persisted.source_id == tencent_source.id

            def load_members(_db, groups, members, source_urls):
                members[("industry", "BK0001")] = {
                    ("CN_A", "000002"): {
                        "market": "CN_A", "symbol": "000002", "name": "万科A",
                        "source_url": "https://example.test", "as_of": "2026-10-02",
                    }
                }

            monkeypatch.setattr("app.services.stock_classification._load_foundation_members", load_members)
            monkeypatch.setattr("app.services.stock_classification._load_reviewed_members", lambda *_args: None)
            groups = [{
                "key": "industry", "items": [{
                    "code": "BK0001", "classification_dimension": "INDUSTRY", "level": 1,
                }],
            }]
            enriched = enrich_classification_groups_with_members(reopened, groups, fetch_remote=False)
            member = enriched[0]["items"][0]["related_stocks"][0]
            assert member["change_pct"] == 1.77
            assert member["quote_status"] == "CACHED"
            assert member["quote_time"] == "2026-10-02T15:00:00"
    finally:
        db.close()
        engine.dispose()
        folder.cleanup()
