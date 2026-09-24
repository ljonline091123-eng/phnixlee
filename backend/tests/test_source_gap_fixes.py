import json

from app.connectors.akshare_adapter import AkshareAdapter


class _Response:
    text = ""
    def raise_for_status(self):
        return None


def test_hk_news_direct_jsonp_parser_avoids_upstream_regex_bug(monkeypatch):
    row = {"date": "2026-09-23 12:00:00", "title": "腾讯控股(<em>00700</em>.HK)新闻",
           "content": "测试\u3000正文", "mediaName": "测试来源", "url": "https://example.test/news"}
    response = _Response()
    response.text = "callback(" + json.dumps({"result": {"cmsArticleWebOld": [row]}}) + ")"

    class Client:
        def __init__(self, *args, **kwargs): pass
        def __enter__(self): return self
        def __exit__(self, *args): pass
        def get(self, *args, **kwargs): return response

    monkeypatch.setattr("app.connectors.akshare_adapter.httpx.Client", Client)
    records = AkshareAdapter().fetch_news("HK", "00700")
    assert len(records) == 1
    assert records[0].title == "腾讯控股(00700.HK)新闻"
    assert records[0].market == "HK"


def test_invalid_ohlc_reference_is_not_accepted():
    from scripts.reconcile_hk_kline_quality import invalid_values
    assert invalid_values(10.0, 9.8, 9.5, 9.7) is True
    assert invalid_values(10.0, 10.2, 9.5, 9.7) is False
