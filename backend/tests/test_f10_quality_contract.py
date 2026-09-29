from types import SimpleNamespace
from unittest.mock import Mock

import pandas as pd

from app.connectors.akshare_adapter import AkshareAdapter
from app.services.f10 import normalize_f10_sections, project_company_control_facts
from app.services.stock_classification import _attach_definition


def test_optional_security_panel_filters_rows_by_security_code() -> None:
    adapter = AkshareAdapter()
    method = Mock(return_value=pd.DataFrame([
        {"证券代码": "000002", "数值": 1},
        {"证券代码": "000001", "数值": 2},
    ]))

    rows = adapter._safe_security_optional_records(method, {}, symbol="000002")

    assert [row["数值"] for row in rows] == [1]
    method.assert_called_once_with()


def test_optional_security_panel_fails_closed_when_unscoped_response_has_no_code() -> None:
    adapter = AkshareAdapter()
    method = Mock(return_value=pd.DataFrame([{"机构": "other", "数值": 1}]))

    assert adapter._safe_security_optional_records(method, {}, symbol="000002") == []

    # A provider whose request URL is intrinsically security-scoped may omit
    # a code column; that opt-in still permits the rows.
    rows = adapter._safe_security_optional_records(
        method, {}, symbol="000002", provider_scoped=True
    )
    assert rows == [{"机构": "other", "数值": 1}]


def test_classification_fact_definition_is_not_overwritten_by_dimension_glossary() -> None:
    item = {"label": "人工智能", "definition": "该标签具体表示公司披露的人工智能业务。"}
    master = SimpleNamespace(
        definition="数据提供方主题标签的维度级解释。",
        criteria="仅按来源标签展示。",
        source_name="主题主数据",
        source_url="https://example.test/taxonomy",
        definition_version="THEME_V1",
        dimension="THEME",
        taxonomy="CN_SECURITIES",
    )

    result = _attach_definition(item, {("THEME", "PROVIDER_CONCEPT"): master}, "THEME")

    assert result["definition"] == "该标签具体表示公司披露的人工智能业务。"
    assert result["fact_definition"] == result["definition"]
    assert result["master_definition"] == "数据提供方主题标签的维度级解释。"
    assert result["definition_version"] == "THEME_V1"


def test_holder_section_contract_uses_per_section_source() -> None:
    payload = normalize_f10_sections({
        "profile": {"fields": {}},
        "holders": {
            "source": "聚合来源",
            "capital_structure": [{"总股本": 1}],
            "capital_structure_source": "CNINFO 股本变动",
            "restricted_release": [],
            "restricted_release_source": "东方财富 限售解禁",
            "institutional": [{"机构": "基金A"}],
            "institutional_source": "新浪机构持股",
            "holder_count": [],
            "holder_count_source": "东方财富 股东户数",
            "circulating": [],
            "circulating_source": "新浪十大流通股东",
            "major": [],
            "major_source": "新浪十大股东",
            "control": [],
            "control_source": "暂无可靠公开接口",
        },
        "financial_summary": {},
        "financial_statements": {},
        "business_composition": {},
        "research_sections": {"reports": []},
    })

    sections = {row["key"]: row for row in payload["holders"]["sections"]}
    assert sections["capital_structure"]["source"] == "CNINFO 股本变动"
    assert sections["institutional"]["source"] == "新浪机构持股"
    assert sections["control"]["source"] == "暂无可靠公开接口"


def test_classification_api_accepts_typed_index_board_and_security_type_dimensions() -> None:
    from app.schemas.company_graph import ClassificationInput

    for dimension in ("INDEX", "BOARD", "TYPE"):
        row = ClassificationInput(
            dimension=dimension, code="TEST", label="测试标签",
            definition_version="TEST_V1", method="SOURCE",
        )
        assert row.dimension == dimension


def test_control_projection_is_unavailable_without_company_mapping() -> None:
    from sqlalchemy import create_engine
    from sqlalchemy.orm import Session
    from app.db.base import Base
    from app.models.market_data import DataSource, StockSymbol

    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(engine)
    with Session(engine) as db:
        source = DataSource(source_code="F10_CONTROL_TEST", source_name="测试来源", adapter_type="MOCK")
        db.add(source)
        db.flush()
        stock = StockSymbol(market="CN_A", symbol="000001", name="测试股票", exchange="SZ", source_id=source.id)
        db.add(stock)
        db.flush()
        result = project_company_control_facts(db, stock)
        assert result["rows"] == []
        assert "暂无" in result["message"]
