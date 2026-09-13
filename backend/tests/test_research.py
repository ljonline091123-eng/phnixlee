import unittest
from datetime import datetime, timezone
from pathlib import Path
from tempfile import TemporaryDirectory

from fastapi.testclient import TestClient
from sqlalchemy import create_engine, select
from sqlalchemy.orm import sessionmaker

from app.db.base import Base
from app.db.session import get_db
from app.main import app
from app.models.ai_hub import ResearchReportRecord
from app.models.market_data import (
    DataSource,
    StockF10Cache,
    StockFinancialReport,
    StockKline,
    StockNews,
    StockNotice,
    StockRealtimeQuote,
    StockSymbol,
)
from app.services.catalog import seed_default_catalog
from app.services.model_hub import seed_default_models
from app.skills.capital_tech import get_capital_and_tech_skill
from app.skills.financial import get_financial_analysis_skill
from app.skills.news_rag import get_recent_news_rag_skill
from app.agents.workflow import ResearchWorkflow


class ResearchPhaseTest(unittest.TestCase):
    temp_dir: TemporaryDirectory[str] | None = None

    @classmethod
    def setUpClass(cls) -> None:
        cls.temp_dir = TemporaryDirectory()
        cls.db_path = Path(cls.temp_dir.name) / "research.db"
        cls.engine = create_engine(f"sqlite:///{cls.db_path}")
        Base.metadata.create_all(cls.engine)
        cls.session_factory = sessionmaker(bind=cls.engine)

    @classmethod
    def tearDownClass(cls) -> None:
        cls.engine.dispose()
        if cls.temp_dir:
            cls.temp_dir.cleanup()

    def setUp(self) -> None:
        Base.metadata.drop_all(self.engine)
        Base.metadata.create_all(self.engine)
        self.db = self.session_factory()
        seed_default_catalog(self.db)
        seed_default_models(self.db)
        source = self.db.scalar(select(DataSource).where(DataSource.source_code == "AKSHARE"))
        assert source is not None
        now = datetime.now(timezone.utc)
        self.db.add(
            StockSymbol(
                market="CN_A",
                symbol="000001",
                exchange="SZSE",
                name="平安银行",
                asset_type="STOCK",
                status="LISTED",
                list_date="19910403",
                source_id=source.id,
                ext_json={},
                raw_payload={},
                last_synced_at=now,
            )
        )
        self.db.add(
            StockFinancialReport(
                market="CN_A",
                symbol="000001",
                indicator="主要财务指标",
                report_period="2026-06-30",
                currency="CNY",
                data_json={
                    "TOTALOPERATEREVE": 70_000_000_000,
                    "PARENTNETPROFIT": 25_000_000_000,
                    "EPSJB": 1.2,
                    "ROEJQ": 12.0,
                    "XSJLL": 20.0,
                    "ZCFZL": 55.0,
                    "TOTALOPERATEREVETZ": 8.0,
                    "PARENTNETPROFITTZ": 10.0,
                },
                source_id=source.id,
                fetched_at=now,
            )
        )
        for index, close in enumerate((10.0, 10.2, 10.3, 10.4, 10.6), start=1):
            self.db.add(
                StockKline(
                    market="CN_A",
                    symbol="000001",
                    period="daily",
                    adjust="",
                    trade_date=f"2026-09-0{index}",
                    open_price=close - 0.1,
                    high_price=close + 0.2,
                    low_price=close - 0.2,
                    close_price=close,
                    volume=1000 + index,
                    amount=10000 + index,
                    source_id=source.id,
                    raw_payload={},
                    fetched_at=now,
                )
            )
        self.db.add(
            StockF10Cache(
                market="CN_A",
                symbol="000001",
                section="fund_flow",
                source_id=source.id,
                payload_json={
                    "rows": [
                        {"日期": "2026-09-04", "主力净流入-净额": 1000000},
                        {"日期": "2026-09-05", "主力净流入-净额": -200000},
                    ]
                },
                fetched_at=now,
            )
        )
        self.db.add(
            StockNews(
                market="CN_A",
                symbol="000001",
                news_time="2026-09-05 10:00:00",
                title="公司获得重大订单，收入增长预期改善",
                content="订单和增长",
                source_name="测试新闻源",
                url="https://example.com/news",
                content_json={},
                source_id=source.id,
                fetched_at=now,
            )
        )
        self.db.add(
            StockNotice(
                market="CN_A",
                symbol="000001",
                notice_date="2026-09-04",
                title="董事会公告",
                notice_type="公告",
                url="https://example.com/notice",
                content_json={},
                source_id=source.id,
                fetched_at=now,
            )
        )
        self.db.add(
            StockRealtimeQuote(
                market="CN_A",
                symbol="000001",
                quote_time="2026-09-05 15:00:00",
                current_price=10.6,
                previous_close_price=10.4,
                open_price=10.5,
                high_price=10.7,
                low_price=10.3,
                volume=1200,
                amount=12000,
                change_amount=0.2,
                change_pct=1.92,
                source_id=source.id,
                raw_payload={},
                fetched_at=now,
            )
        )
        self.db.commit()

    def tearDown(self) -> None:
        self.db.close()

    def test_skills_resolve_fuzzy_name_and_return_structured_data(self) -> None:
        financial = get_financial_analysis_skill("平安银行", db=self.db)
        self.assertEqual(financial.symbol, "000001")
        self.assertEqual(financial.metrics["revenue"], 70_000_000_000)
        self.assertEqual(financial.yoy_growth["net_profit_yoy"], 10.0)

        technical = get_capital_and_tech_skill("000001", "CN_A", db=self.db)
        self.assertEqual(technical.trend, "UP")
        self.assertAlmostEqual(technical.capital_summary["net_inflow_20d"], 800000)

        news = get_recent_news_rag_skill("000001", top_k=2, db=self.db)
        self.assertEqual(news.symbol, "000001")
        self.assertGreaterEqual(len(news.items), 1)

    def test_workflow_emits_agents_and_report_with_mock_fallback(self) -> None:
        events = list(ResearchWorkflow(self.db).stream("000001", "CN_A", top_k=3))
        event_names = [item["event"] for item in events]
        self.assertIn("agent", event_names)
        self.assertIn("report", event_names)
        self.assertEqual(event_names[-1], "done")
        self.assertIn("history", [item["data"].get("stage") for item in events if item["event"] == "stage"])
        report_text = "".join(item["data"].get("delta", "") for item in events if item["event"] == "report")
        self.assertIn("综合评级", report_text)
        self.assertIn("风险提示", report_text)
        report_id = events[-1]["data"].get("report_id")
        self.assertIsInstance(report_id, int)
        saved_report = self.db.get(ResearchReportRecord, report_id)
        self.assertIsNotNone(saved_report)
        self.assertEqual(saved_report.symbol, "000001")
        self.assertTrue(saved_report.report_markdown)

    def test_sse_endpoint_contract(self) -> None:
        def override_get_db():
            yield self.db

        app.dependency_overrides[get_db] = override_get_db
        try:
            with TestClient(app) as client:
                with client.stream(
                    "POST",
                    "/api/v1/research/analyze",
                    json={"symbol": "000001", "market": "CN_A", "top_k": 2},
                ) as response:
                    body = "\n".join(response.iter_lines())
            self.assertEqual(response.status_code, 200)
            self.assertTrue(response.headers["content-type"].startswith("text/event-stream"))
            self.assertIn("event: agent", body)
            self.assertIn("event: report", body)
            self.assertIn("event: done", body)
        finally:
            app.dependency_overrides.clear()


if __name__ == "__main__":
    unittest.main()
