from datetime import datetime, timezone
from unittest.mock import patch

from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import Session
from sqlalchemy.pool import StaticPool

from app.db.base import Base
from app.db.session import get_db
from app.main import app
from app.models.market_data import DataSource, StockKline, StockSymbol


def test_http_selection_review_tracking_contract():
    engine = create_engine("sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool)
    Base.metadata.create_all(engine)
    with Session(engine) as db:
        source = DataSource(source_code="DEMO", source_name="Demo", adapter_type="MOCK")
        db.add(source)
        db.flush()
        db.add(StockSymbol(market="CN_A", symbol="DEMO001", name="Demo", exchange="DEMO", source_id=source.id))
        for day, close in [("2025-01-03", 10), ("2025-01-06", 11)]:
            db.add(StockKline(market="CN_A", symbol="DEMO001", period="daily", adjust="",
                trade_date=day, close_price=close, volume=100, source_id=source.id))
        db.commit()
        app.dependency_overrides[get_db] = lambda: db
        client = TestClient(app)
        try:
            with patch("app.services.selection._now", return_value=datetime(2025, 1, 6, 10, tzinfo=timezone.utc)):
                response = client.post("/api/v1/selection/runs", json={"symbols": ["DEMO001"], "use_model": False, "data_mode": "MOCK"})
                assert response.status_code == 201, response.text
                run = response.json()
                item = run["candidates"][0]
                assert item["decision"] == "PENDING"
                assert client.post(f"/api/v1/selection/candidates/{item['id']}/review", json={"decision": "APPROVED"}).status_code == 422
                reviewed = client.post(f"/api/v1/selection/candidates/{item['id']}/review", json={"decision": "APPROVED", "target_price": 12})
                assert reviewed.status_code == 200, reviewed.text
                tracking_id = reviewed.json()["tracking_id"]
                details = client.get(f"/api/v1/selection/tracking/{tracking_id}").json()
                assert details["required_sessions"] >= 10 and details["snapshots"] == []
                assert details["confirmation_date"] == "2025-01-06"
                assert client.post("/api/v1/selection/refresh", json={"tracking_ids": [tracking_id]}).status_code == 200
                assert client.get(f"/api/v1/selection/runs/{run['id']}").json()["status"] == "TRACKING"
        finally:
            client.close()
            app.dependency_overrides.pop(get_db, None)
    engine.dispose()
