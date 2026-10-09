import json
from datetime import datetime, timezone

import pytest

from app.connectors.neeq_official import parse_company_response
from app.services.security_master import listing_metadata


def response(symbol="830938", layer="1", name="可恩口腔"):
    page = {"content": [{"xxzqdm": symbol, "xxzqjc": name, "xxfcbj": layer,
                          "xxhyzl": "卫生", "xxjsrq": "20261008"}],
            "totalElements": 1, "totalPages": 1}
    return "cb(" + json.dumps([page], ensure_ascii=False) + ")"


def test_official_identity_layer_and_provenance():
    proof = parse_company_response(response(), "cb", "830938",
                                   observed_at=datetime(2026, 10, 9, tzinfo=timezone.utc))
    assert proof["listing_board"] == "NEEQ_INNOVATION"
    assert proof["source_record"]["xxzqjc"] == "可恩口腔"
    assert len(proof["response_sha256"]) == 64
    meta = listing_metadata("NEEQ_INNOVATION", "830938", {"neeq_official_verification": proof})
    assert meta["classification_method"] == "SOURCE"
    assert meta["classification_evidence"]["source_code"] == "NEEQ_OFFICIAL"


@pytest.mark.parametrize("body", [response(symbol="830939"), response(layer="2"),
    "bad(" + response()[3:], "cb([])"])
def test_invalid_or_cross_issuer_response_rejected(body):
    with pytest.raises(ValueError):
        parse_company_response(body, "cb", "830938")


def test_wrong_official_layer_does_not_override_historical_market_identity():
    proof = parse_company_response(response(layer="0"), "cb", "830938")
    meta = listing_metadata("NEEQ_INNOVATION", "830938", {"neeq_official_verification": proof})
    assert meta["classification_method"] == "SOURCE_LAYER"
