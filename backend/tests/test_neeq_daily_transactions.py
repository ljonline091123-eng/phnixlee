import pytest
from app.connectors.neeq_market import fetch_daily_transactions


def row(day="2026-10-08", **changes):
    return {"MSECUCODE": "430005.NQ", "TRADEDATE": day, "OPEN": "24.43", "HIGH": "24.43",
            "LOW": "24.3", "CLOSE": "24.34", "VOLUMN": "2.3246", "AMOUNT": "56.613282", **changes}


def page(rows, pages=1):
    return {"IsSuccess": True, "result": rows, "TotalPage": pages}


def fetch(callback, begin="20261001"):
    return fetch_daily_transactions("NEEQ_INNOVATION", "430005", begin, "20261009", callback)


def test_units_adjustment_range_and_original_evidence():
    calls = []
    def source(path, params):
        calls.append(params)
        return page([row(), row("2026-09-30")], 20)
    data = fetch(source)
    assert len(calls) == 1 and len(data) == 1
    assert data[0].volume == 23246 and data[0].amount == 566132.82
    assert data[0].close_price == 24.34 and data[0].adjust == ""
    assert data[0].raw_payload["original_source_responses"][0]["original_source_response"]
    assert data[0].raw_payload["coverage"]["status"] == "REQUESTED_RANGE_FETCHED"


@pytest.mark.parametrize("bad", [row(MSECUCODE="430006.NQ"), row(HIGH="1"), row(VOLUMN="-1"),
    row(LOW="nan"), row(TRADEDATE="bad"), row(VOLUMN="0.00001")])
def test_corrupt_values_and_cross_issuer_are_rejected(bad):
    with pytest.raises(ValueError):
        fetch(lambda *_: page([bad]))


def test_missing_or_repeated_pages_cannot_pass():
    with pytest.raises(ValueError, match="重复"):
        fetch(lambda *_: page([row()], 3))
    with pytest.raises(ValueError, match="中间页"):
        fetch(lambda *_: page([], 3))
    with pytest.raises(ValueError, match="不能判定"):
        fetch(lambda *_: page([], 0))


def test_zero_trade_days_retained_as_references_and_history_paginated():
    responses = iter([page([row(VOLUMN="0", AMOUNT="0")], 2), page([row("2026-10-01")], 2)])
    data = fetch(lambda *_: next(responses))
    assert len(data) == 2 and data[-1].raw_payload["no_trade_day"] is True
    assert data[0].raw_payload["coverage"]["pages"] == 2
