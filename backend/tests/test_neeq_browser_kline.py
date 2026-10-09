import json
import pytest

from app.connectors.eastmoney_browser import kline_params, parse_kline_response
from app.connectors.akshare_adapter import AkshareAdapter


def params(adjust=""):
    return kline_params("430005", "daily", "20261001", "20261008", adjust)


def response(line="2026-10-08,25,25.5,26,24,100,252500,8,2,0.5,0.03", **changes):
    return {"rc": 0, "data": {"code": "430005", "market": 0, "klines": [line], **changes}}


def test_jsonp_identity_range_and_real_empty_are_validated():
    payload = response()
    assert parse_kline_response("quantPublicKline(" + json.dumps(payload) + ");", "430005", params()) == payload
    for bad in (response(code="430006"), response(market=1), response(line="2026-09-30,25,25,26,24,1,1"),
                response(line="2026-10-08,25,25,24,26,1,1"), response(line="2026-10-08,25,25,26,24,-1,1"),
                response(line="2026-10-08,nan,25,26,24,1,1"), {"rc": 0, "data": None}, None):
        with pytest.raises(ValueError):
            parse_kline_response(json.dumps(bad), "430005", params())
    assert parse_kline_response(json.dumps(response(klines=[])), "430005", params())["data"]["klines"] == []
    with pytest.raises(ValueError):
        parse_kline_response('evil(' + json.dumps(payload) + ')', "430005", params())


def test_browser_fallback_preserves_adjustment_units_and_original(monkeypatch):
    def fail(*args, **kwargs):
        raise ConnectionError("public HTTP disconnected")
    monkeypatch.setattr(AkshareAdapter, "_request_eastmoney_json", fail)
    monkeypatch.setattr(AkshareAdapter, "_fetch_neeq_json", fail)
    import app.connectors.ths_neeq_kline as ths_source
    monkeypatch.setattr(ths_source, "fetch_ths_neeq_kline", fail)
    import app.connectors.eastmoney_browser as source
    received = []
    def fetch(symbol, requested):
        received.append(requested)
        return response(), {"original_source_response": "original JSONP", "source_url": source.ENDPOINT}
    monkeypatch.setattr(source, "fetch_neeq_browser_kline", fetch)
    rows = AkshareAdapter().fetch_kline("NEEQ_INNOVATION", "430005", "daily", "20261001", "20261008", "")
    assert received[0]["fqt"] == "0" and received[0]["secid"] == "0.430005"
    assert rows[0].volume == 10000 and rows[0].amount == 252500 and rows[0].turnover_rate == 0.03
    assert rows[0].raw_payload["source_evidence"]["original_source_response"] == "original JSONP"
    assert rows[0].raw_payload["source_evidence"]["primary_source_error"] == "public HTTP disconnected"
    assert params("qfq")["fqt"] == "1" and params("hfq")["fqt"] == "2"


def test_invalid_period_and_query_parameters_are_rejected():
    for args in (("430005", "daily", "20260230", "20261008", ""),
                 ("430005", "daily", "20261008", "20261001", ""),
                 ("430005", "bad", "20261001", "20261008", ""),
                 ("https://example.test", "daily", "20261001", "20261008", "")):
        with pytest.raises(ValueError):
            kline_params(*args)
