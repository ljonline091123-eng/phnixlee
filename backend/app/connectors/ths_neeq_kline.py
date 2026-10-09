"""Validated daily NEEQ history from the public THS stock page API."""
from datetime import datetime, timedelta, timezone
import hashlib
import json
import math
import re

from app.connectors.base import KlineRecord

ENDPOINT = "https://quota-h.10jqka.com.cn/fuyao/common_hq_aggr/quote/v1/single_kline"
FIELDS = {"1": "timestamp_ms", "7": "open", "8": "high", "9": "low",
          "11": "close", "13": "volume_share", "19": "amount_yuan"}
CHINA_TZ = timezone(timedelta(hours=8))


def request_payload(symbol, *, lookback=400, end_time_ms=0):
    if not re.fullmatch(r"\d{6}", symbol):
        raise ValueError("新三板证券代码必须为六位数字")
    return {"code_list": [{"codes": [symbol], "market": "145"}],
            "trade_class": "intraday", "time_period": "day_1", "trade_date": -1,
            "begin_time": -lookback, "end_time": end_time_ms,
            "adjust_type": "forward", "gpid": 1}


def parse_response(payload, market, symbol, begin, end, *, source_body=None, requested=None, observed_at=None):
    if not isinstance(payload, dict) or payload.get("status_code") != 0:
        raise ValueError("同花顺新三板日线响应失败")
    data = payload.get("data")
    quotes = data.get("quote_data") if isinstance(data, dict) else None
    if not isinstance(quotes, list):
        raise ValueError("同花顺新三板日线响应格式异常")
    if not quotes:
        # fail_params means the provider explicitly rejected this request. It
        # is not evidence that the issuer has no trading history.
        if data.get("fail_params"):
            raise ValueError("同花顺新三板日线拒绝了查询参数，不能判定没有交易历史")
        raise ValueError("同花顺新三板日线返回空数据，不能判定没有交易历史")
    if len(quotes) != 1 or str(quotes[0].get("market")) != "145" or str(quotes[0].get("code")) != symbol:
        raise ValueError("同花顺新三板日线证券身份不符")
    field_codes = [str(value) for value in quotes[0].get("data_fields") or []]
    if field_codes != list(FIELDS):
        raise ValueError("同花顺新三板日线字段契约发生变化")
    source_body = source_body or json.dumps(payload, ensure_ascii=False, sort_keys=True)
    evidence = {"source_url": ENDPOINT, "request_json": requested or request_payload(symbol),
                "observed_at": (observed_at or datetime.now(timezone.utc)).isoformat(),
                "response_sha256": hashlib.sha256(source_body.encode()).hexdigest(),
                "original_source_response": source_body, "identity_status": "CODE_AND_MARKET_MATCH"}
    records, seen = [], set()
    for source_row in quotes[0].get("value") or []:
        if not isinstance(source_row, list) or len(source_row) != len(FIELDS):
            raise ValueError("同花顺新三板日线记录缺少字段")
        timestamp, opening, high, low, closing, volume, amount = source_row
        if not isinstance(timestamp, int):
            raise ValueError("同花顺新三板日线时间戳格式异常")
        # Provider timestamps are exchange-day epoch values. Interpret them in
        # the China market timezone; UTC would shift midnight back one day.
        day = datetime.fromtimestamp(timestamp / 1000, CHINA_TZ).date().isoformat()
        compact = day.replace("-", "")
        if day in seen:
            raise ValueError("同花顺新三板日线出现重复交易日")
        seen.add(day)
        numbers = [float(value) for value in (opening, high, low, closing, volume, amount)]
        if not all(math.isfinite(value) for value in numbers) or volume < 0 or amount < 0:
            raise ValueError("同花顺新三板日线包含非法数值")
        opening, high, low, closing, volume, amount = numbers
        if high < max(opening, closing, low) or low > min(opening, closing):
            raise ValueError("同花顺新三板日线价格上下界不符")
        if volume != round(volume):
            raise ValueError("同花顺新三板日线成交量不是整数股")
        if not begin <= compact <= end:
            continue
        records.append(KlineRecord(market=market, symbol=symbol, period="daily", adjust="qfq",
            trade_date=day, open_price=opening, high_price=high, low_price=low,
            close_price=closing, volume=volume, amount=round(amount, 2), turnover_rate=None,
            raw_payload={"source_method": "THS_PUBLIC_NEEQ_KLINE", "source_row": source_row,
                "source_fields": FIELDS, "adjustment": "forward", "units": {"volume": "股", "amount": "元"},
                "negative_adjusted_price": min(opening, high, low, closing) < 0,
                "source_evidence": evidence if not records else {k: v for k, v in evidence.items() if k != "original_source_response"}}))
    if not records:
        raise ValueError("同花顺新三板日线未返回查询区间内记录，不能判定没有交易历史")
    return records


def fetch_ths_neeq_kline(market, symbol, begin, end):
    if market not in {"NEEQ", "NEEQ_INNOVATION"}:
        raise ValueError("同花顺新三板日线仅支持全国股转市场")
    if begin > end:
        raise ValueError("日线起始日期晚于结束日期")
    from curl_cffi import requests
    requested = request_payload(symbol, end_time_ms=int(datetime.now(timezone.utc).timestamp() * 1000))
    response = requests.post(ENDPOINT, json=requested, timeout=15, impersonate="chrome", headers={
        "User-Agent": "Mozilla/5.0", "Referer": f"https://stockpage.10jqka.com.cn/{symbol}/",
        "Origin": "https://stockpage.10jqka.com.cn"})
    response.raise_for_status()
    return parse_response(response.json(), market, symbol, begin, end,
                          source_body=response.text, requested=requested)
