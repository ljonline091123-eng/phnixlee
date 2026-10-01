"""Classification member and provenance projection tests."""

import json
from datetime import datetime, timezone
from types import SimpleNamespace

from app.services.stock_classification import (
    _classification_source_url,
    _fetch_tencent_changes,
    enrich_classification_groups_with_members,
)


class _Rows:
    def __init__(self, rows):
        self._rows = rows

    def all(self):
        return self._rows


class _DB:
    def __init__(self, rows):
        self.rows = rows

    def execute(self, _statement):
        return _Rows(self.rows)

    def scalars(self, _statement):
        return _Rows([])


def _evidence(symbol: str, name: str, *, industry: str, theme: str) -> tuple[object, object]:
    payload = {
        "SECUCODE": f"{symbol}.SZ",
        "SECURITY_CODE": symbol,
        "SECURITY_NAME_ABBR": name,
        "BOARD_CODE_BK_1LEVEL": industry,
        "BOARD_NAME_1LEVEL": "测试行业",
        "BLGAINIAN_CODE": theme,
        "BLGAINIAN": "测试概念",
    }
    evidence = SimpleNamespace(
        content=json.dumps(payload),
        source_name="EASTMONEY",
        url="https://example.test/eastmoney/profile",
        available_at=datetime(2026, 10, 1, tzinfo=timezone.utc),
    )
    return SimpleNamespace(), evidence


def test_classification_members_are_projected_from_immutable_profile_evidence():
    groups = [
        {"key": "industry", "title": "所属行业", "items": [{
            "label": "测试行业", "code": "BK1202", "classification_dimension": "INDUSTRY", "level": 1,
        }]},
        {"key": "theme", "title": "主题板块", "items": [{
            "label": "测试概念", "code": "BK0680", "classification_dimension": "THEME",
        }]},
    ]
    db = _DB([
        _evidence("000002", "万科A", industry="BK1202", theme="BK0680"),
        _evidence("000006", "深振业A", industry="BK1202", theme="BK0680"),
    ])

    result = enrich_classification_groups_with_members(db, groups, fetch_remote=False)
    industry = result[0]["items"][0]
    theme = result[1]["items"][0]
    assert industry["member_count"] == 2
    assert [row["symbol"] for row in industry["related_stocks"]] == ["000002", "000006"]
    assert "RPT_F10_ORG_BASICINFO" in industry["source_url"]
    assert industry["related_stocks"][0]["source_url"] == "https://example.test/eastmoney/profile"
    assert industry["as_of"].startswith("2026-10-01T00:00:00")
    assert industry["trend_pct"] is None and industry["trend_status"] == "UNAVAILABLE"
    assert theme["member_count"] == 2


def test_industry_source_url_is_a_reproducible_provider_query():
    url = _classification_source_url("BK1202", "INDUSTRY", 1)
    assert url is not None
    assert "RPT_F10_ORG_BASICINFO" in url
    assert "BOARD_CODE_BK_1LEVEL" in url
    assert "BK1202" in url


def test_tencent_batch_parser_keeps_observed_percent_change(monkeypatch):
    parts = [""] * 35
    parts[0] = "51"
    parts[1] = "测试A"
    parts[2] = "000002"
    parts[3] = "4.26"
    parts[4] = "4.08"
    parts[5] = "3.80"
    parts[30] = "20261001150000"
    parts[31] = "0.18"
    parts[32] = "4.41"
    parts[33] = "4.40"
    parts[34] = "3.67"
    raw_quote = "~".join(parts)

    class _Response:
        text = f'v_sz000002="{raw_quote}";'

        def raise_for_status(self):
            return None

    class _Client:
        def __enter__(self):
            return self

        def __exit__(self, *_args):
            return None

        def get(self, _url):
            return _Response()

    monkeypatch.setattr("app.services.stock_classification.httpx.Client", lambda **_kwargs: _Client())
    changes, source_url = _fetch_tencent_changes([("CN_A", "000002")])
    assert changes[("CN_A", "000002")] == 4.41
    assert source_url == "https://qt.gtimg.cn/q="
