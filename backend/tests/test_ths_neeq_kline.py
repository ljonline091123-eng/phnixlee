from datetime import datetime, timezone
import json

import pytest

from app.connectors.ths_neeq_kline import parse_response, request_payload


def payload(code="830938", market="145", fields=None, values=None, fail=None):
    quote = {"market": market, "code": code, "data_fields": fields or ["1", "7", "8", "9", "11", "13", "19"],
             "value": values if values is not None else [[1791388800000, .99, 1, .98, .99, 3900, 3853]]}
    return {"status_code": 0, "data": {"quote_data": [] if fail else [quote], "fail_params": fail}, "status_msg": "ok"}


def test_parses_units_range_identity_and_original_evidence():
    data = payload()
    body = json.dumps(data)
    rows = parse_response(data, "NEEQ_INNOVATION", "830938", "20261001", "20261009",
                          source_body=body, requested=request_payload("830938"),
                          observed_at=datetime(2026, 10, 9, tzinfo=timezone.utc))
    assert len(rows) == 1 and rows[0].trade_date == "2026-10-08"
    assert rows[0].volume == 3900 and rows[0].amount == 3853
    assert rows[0].adjust == "qfq"
    assert rows[0].raw_payload["source_evidence"]["original_source_response"] == body


@pytest.mark.parametrize("data", [payload(code="830939"), payload(market="33"),
    payload(fields=["1", "7"]), payload(values=[[1791388800000, .99, .8, .98, .99, 3900, 3853]]),
    payload(values=[[1791388800000, .99, 1, .98, .99, 3900.5, 3853]]),
    payload(values=[[1791388800000, .99, 1, .98, .99, 3900, -1]])])
def test_corrupt_or_cross_issuer_response_rejected(data):
    with pytest.raises(ValueError):
        parse_response(data, "NEEQ_INNOVATION", "830938", "20261001", "20261009")


def test_rejected_and_empty_are_not_treated_as_no_history():
    with pytest.raises(ValueError, match="拒绝了查询参数"):
        parse_response(payload(fail={"code": "830938"}), "NEEQ_INNOVATION", "830938", "20261001", "20261009")
    with pytest.raises(ValueError, match="不能判定"):
        parse_response(payload(values=[]), "NEEQ_INNOVATION", "830938", "20261001", "20261009")


def test_negative_forward_adjusted_price_is_retained_and_marked():
    rows = parse_response(payload(values=[[1791388800000, .97, .97, -.03, .73, 27500, 109135]]),
                          "NEEQ_INNOVATION", "830938", "20261001", "20261009")
    assert rows[0].low_price == -.03
    assert rows[0].raw_payload["negative_adjusted_price"] is True
