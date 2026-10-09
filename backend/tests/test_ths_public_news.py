import json
from datetime import datetime, timezone

import pytest

from app.connectors.ths_public_news import parse_stock_page


NOW = datetime(2026, 10, 9, tzinfo=timezone.utc)


def page(items, **identity):
    state = {"stockCode": "834500", "marketId": "145", "stockName": "猫诚股份",
             "initialUpperState": {"items": items, "hasMore": True, "error": None}, **identity}
    value = '1f:' + json.dumps(["$", "$L20", "834500", state], ensure_ascii=False) + '\n'
    return '<script>self.__next_f.push(' + json.dumps([1, value], ensure_ascii=False) + ')</script>'


def item(label="08-20 17:45", **values):
    return {"id": "1_123", "title": "猫诚股份2026年半年度盈利98.99万", "timeLabel": label,
            "jumpUrl": "https://news.10jqka.com.cn/20260820/c123.shtml", "type": "regular", **values}


def test_real_next_envelope_preserves_publication_and_original():
    html = page([item(), item(type="announcement"), item(title="其他公司新闻"),
                 item("2024-04-18 18:32", id="2", jumpUrl="https://news.10jqka.com.cn/20250126/c2.shtml")])
    rows = parse_stock_page(html, "NEEQ", "834500", observed_at=NOW)
    assert [r.news_time for r in rows] == ["2026-08-20 17:45:00", "2024-04-18 18:32:00"]
    assert rows[0].content_json["original_source_response"]["html"] == html
    assert rows[0].content_json["collection_status"] == "PARTIAL"
    assert rows[0].content_json["rejected_source_rows"][0]["reason"] == "主体提及未匹配"


@pytest.mark.parametrize("identity", [{"stockCode": "836503"}, {"marketId": "33"}])
def test_mismatched_identity_rejected(identity):
    with pytest.raises(ValueError, match="证券代码或市场不符"):
        parse_stock_page(page([item()], **identity), "NEEQ", "834500", observed_at=NOW)


@pytest.mark.parametrize("row", [item(type="announcement"), item("08-20 17:45", jumpUrl="https://news.10jqka.com.cn/20250126/c2.shtml"), item("2027-01-01 09:00"), item(jumpUrl="https://example.com/20260820/123")])
def test_unverifiable_feed_is_not_no_news(row):
    with pytest.raises(ValueError, match="不能证明无新闻"):
        parse_stock_page(page([row]), "NEEQ", "834500", observed_at=NOW)
