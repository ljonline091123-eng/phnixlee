from types import SimpleNamespace
from unittest.mock import Mock

import pandas as pd

from app.connectors.akshare_adapter import AkshareAdapter
from app.services.f10 import normalize_f10_sections, project_company_control_facts
from app.services.company_governance import profile_record
from app.services.stock_classification import _attach_definition, _looks_like_index, build_classification_groups
from app.services.taxonomy import LABEL_DEFINITION_OVERRIDES, label_definition, seed_builtin_definitions


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


def test_financial_source_payload_rounds_amounts_but_preserves_ratios_and_eps() -> None:
    adapter = AkshareAdapter()
    payload = adapter._normalize_financial_payload({
        "TOTAL_OPERATE_INCOME": 123456789.126,
        "PARENT_NETPROFIT": -987654321.125,
        "TOTAL_OPERATE_INCOME_YOY": 12.345678,
        "EPS": 1.234567,
        "ROE": 8.765432,
        "REPORT_DATE": "2026-06-30",
    })

    assert payload["TOTAL_OPERATE_INCOME"] == 123456789.13
    assert payload["PARENT_NETPROFIT"] == -987654321.13
    assert payload["TOTAL_OPERATE_INCOME_YOY"] == 12.345678
    assert payload["EPS"] == 1.234567
    assert payload["ROE"] == 8.765432


def test_holder_source_payload_keeps_shares_and_rounds_percentages() -> None:
    adapter = AkshareAdapter()
    payload = adapter._normalize_holder_payload({
        "持股数量": 9618540236.789,
        "持股比例": 49.5667,
        "平均持股数": 25578.1234,
        "股东名称": "测试主体",
    })

    assert payload["持股数量"] == 9618540236.79
    assert payload["持股比例"] == 49.57
    assert payload["平均持股数"] == 25578.12
    assert payload["股东名称"] == "测试主体"


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


def test_control_section_preserves_pending_fact_status() -> None:
    payload = normalize_f10_sections({
        "profile": {"fields": {}},
        "holders": {
            "control": [{"主体名称": "待核验主体", "status": "PENDING"}],
            "control_source": "公司关系图谱（来源证据）",
        },
        "financial_summary": {},
        "financial_statements": {},
        "business_composition": {},
        "research_sections": {},
    })
    control = next(row for row in payload["holders"]["sections"] if row["key"] == "control")
    assert control["status"] == "PENDING"


def test_profile_record_keeps_actual_controller_role_separate() -> None:
    source = {
        "source_name": "测试来源",
        "source_key": "profile:test",
        "source_url": "https://example.test/profile",
        "retrieved_at": "2026-09-30T00:00:00+00:00",
        "records": [{
            "market": "CN_A",
            "company": {"name": "测试公司", "jurisdiction": "CN", "source_issuer_id": "ISSUER"},
            "industries": [],
            "themes": [],
            "controller_mentions": [{
                "name": "实际控制人甲", "source_issuer_id": "CONTROLLER",
                "mention_type": "ACTUAL_CONTROLLER",
            }],
            "evidence": {"source_key": "evidence:test", "title": "主体资料", "content": "原文", "available_at": "2026-09-30T00:00:00+00:00"},
            "source_record": {},
        }],
    }
    identities = {"CONTROLLER": {"name": "实际控制人甲", "source_issuer_id": "CONTROLLER", "jurisdiction": "CN"}}
    result = profile_record(source, 1, identities)
    control = next(item for item in result["facts"] if item["fact_type"] == "CONTROLS")
    assert control["properties_json"]["control_role"] == "ACTUAL_CONTROLLER"


def test_index_projection_does_not_drop_real_theme_labels() -> None:
    assert _looks_like_index("沪深300") is True
    assert _looks_like_index("融资融券") is False
    assert _looks_like_index("红利低波") is False
    assert _looks_like_index("精选消费") is False


def test_label_definition_overrides_cover_industry_board_and_theme() -> None:
    expected = {
        ("INDUSTRY", "房地产业"): "房地产开发经营",
        ("BOARD", "房地产开发"): "不动产开发",
        ("BOARD", "深交所主板"): "深圳证券交易所主板",
        ("THEME", "智能家居"): "家庭物联网",
    }
    assert len(LABEL_DEFINITION_OVERRIDES) >= len(expected)
    for (dimension, label), fragment in expected.items():
        definition = label_definition(label, dimension)
        assert definition is not None
        assert fragment in definition["definition"]
        assert definition["definition_version"] == "LABEL_GLOSSARY_V1"


def test_stock_classification_uses_label_master_data_without_database() -> None:
    from app.models.market_data import StockSymbol

    stock = StockSymbol(market="CN_A", symbol="000001", exchange="SZ", name="测试股票")
    groups = build_classification_groups(stock=stock, db=None, profile_fields={
        "所属行业": "房地产业",
        "板块": "深交所主板",
        "所属概念": "智能家居",
    })
    items = {
        (item["classification_dimension"], item["label"]): item
        for group in groups
        for item in group["items"]
    }
    assert "房地产开发经营" in items[("INDUSTRY", "房地产业")]["definition"]
    assert "深圳证券交易所主板" in items[("BOARD", "深交所主板")]["definition"]
    assert "家庭物联网" in items[("THEME", "智能家居")]["definition"]


def test_seed_writes_label_definitions_to_taxonomy_table() -> None:
    from sqlalchemy import create_engine, select
    from sqlalchemy.orm import Session

    from app.db.base import Base
    from app.models.taxonomy import ClassificationDefinition

    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(engine)
    with Session(engine) as db:
        created = seed_builtin_definitions(db)
        assert created >= len(LABEL_DEFINITION_OVERRIDES)
        row = db.scalar(select(ClassificationDefinition).where(
            ClassificationDefinition.dimension == "BOARD",
            ClassificationDefinition.code == "LABEL_BOARD_SZ_MAIN",
        ))
        assert row is not None
        assert row.label == "深交所主板"
        assert "深圳证券交易所主板" in row.definition
        assert row.definition_version == "LABEL_GLOSSARY_V1"
