"""Unadjusted daily prices from the issuer's public daily transaction table."""
from datetime import datetime, timezone
from decimal import Decimal, InvalidOperation
from hashlib import sha256
import json

from app.connectors.base import KlineRecord

PATH = "/api/F10/MarketQuotation/GetTransactionDetail"
BASE = "https://xinsanban.eastmoney.com"


def numeric(value):
    try:
        number = Decimal(str(value).replace(",", ""))
    except InvalidOperation as exc:
        raise ValueError("每日交易来源缺少具体数值") from exc
    if not number.is_finite() or number < 0:
        raise ValueError("每日交易来源包含非法价格、成交量或成交额")
    return number


def fetch_daily_transactions(market, symbol, begin, end, fetch_json):
    # Public page columns: VOLUMN is 万股, AMOUNT is 万元. CLOSE is
    # unadjusted; CLOSEQF alone cannot provide adjusted OHLC observations.
    records, evidence, seen_dates, signatures = [], [], set(), set()
    complete = False
    for page in range(1, 201):
        params = {"code": symbol, "page": page, "pagesize": 100,
                  "sortRule": -1, "sortType": "TRADEDATE"}
        payload = fetch_json(PATH, params)
        if not isinstance(payload, dict) or payload.get("IsSuccess") not in (True, 1) or not isinstance(payload.get("result"), list):
            raise ValueError("每日交易接口响应异常，不能标记无数据")
        source_rows = payload["result"]
        body = json.dumps(payload, ensure_ascii=False, sort_keys=True)
        digest = sha256(json.dumps(source_rows, sort_keys=True).encode()).hexdigest()
        if source_rows and digest in signatures:
            raise ValueError("每日交易接口重复分页，不能确认日期范围完整")
        signatures.add(digest)
        evidence.append({"source_url": BASE + PATH, "params": params,
            "observed_at": datetime.now(timezone.utc).isoformat(),
            "response_sha256": sha256(body.encode()).hexdigest(),
            "response_representation": "PARSED_JSON_SERIALIZATION", "original_source_response": body})
        dates = []
        for raw in source_rows:
            if str(raw.get("MSECUCODE") or "") != symbol + ".NQ":
                raise ValueError("每日交易接口证券身份不符，拒绝跨股票入库")
            day = str(raw.get("TRADEDATE") or "")[:10]
            compact = datetime.strptime(day, "%Y-%m-%d").strftime("%Y%m%d")
            dates.append(compact)
            if day in seen_dates:
                raise ValueError("每日交易接口出现重复日期，需核查分页")
            seen_dates.add(day)
            if not begin <= compact <= end:
                continue
            opening, closing, high, low = [numeric(raw.get(k)) for k in ("OPEN", "CLOSE", "HIGH", "LOW")]
            if high < max(opening, closing, low) or low > min(opening, closing):
                raise ValueError("每日交易来源的价格上下界不符")
            volume, amount = numeric(raw.get("VOLUMN")) * 10000, numeric(raw.get("AMOUNT")) * 10000
            if volume != volume.to_integral_value():
                raise ValueError("每日交易成交量换算后不是整数股")
            records.append(KlineRecord(market=market, symbol=symbol, period="daily", adjust="",
                trade_date=day, open_price=float(opening), close_price=float(closing),
                high_price=float(high), low_price=float(low), volume=float(volume),
                amount=float(amount.quantize(Decimal("0.01"))), turnover_rate=None,
                raw_payload={"source_method": "PUBLIC_DAILY_TRANSACTION_TABLE", "source_row": raw,
                    "adjustment": "0", "no_trade_day": volume == 0,
                    "units": {"volume": "股", "source_volume": "万股", "amount": "元", "source_amount": "万元"},
                    "source_evidence": {k: v for k, v in evidence[-1].items() if k != "original_source_response"}}))
        if dates != sorted(dates, reverse=True):
            raise ValueError("每日交易来源未按日期倒序，不能提前结束分页")
        try:
            total_pages = int(payload["TotalPage"])
        except (KeyError, ValueError, TypeError) as exc:
            raise ValueError("每日交易来源缺少有效分页总数") from exc
        if total_pages < 0 or (source_rows and total_pages < page):
            raise ValueError("每日交易来源分页总数与数据不符")
        if not source_rows and page < total_pages:
            raise ValueError("每日交易来源中间页为空，不能标记历史完整")
        if page >= total_pages or (dates and dates[-1] < begin):
            complete = True
            break
    if not complete:
        raise ValueError("每日交易来源达到分页上限，未覆盖完整日期范围")
    if not records:
        # A successful empty table does not establish issuer identity or prove
        # that no trading exists in other sources. Keep it retryable.
        raise ValueError("每日交易来源未返回范围内可验证日线；不能判定没有交易历史")
    records.sort(key=lambda r: r.trade_date)
    records[0].raw_payload["original_source_responses"] = evidence
    records[0].raw_payload["coverage"] = {"begin": begin, "end": end, "pages": len(evidence),
        "status": "REQUESTED_RANGE_FETCHED", "source_page_url": BASE + f"/F10/MarketQuotation/F10TradeStatistics/{symbol}.html"}
    return records
