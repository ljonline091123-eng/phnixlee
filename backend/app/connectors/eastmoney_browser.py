"""Bounded public JSONP transport for the NEEQ chart's documented endpoint."""
from __future__ import annotations

from datetime import date, datetime, timezone
from hashlib import sha256
import json
import math
import re
import time
from threading import BoundedSemaphore
from urllib.parse import urlencode, urlparse, parse_qs

_SLOTS = BoundedSemaphore(1)
ENDPOINT = "https://push2his.eastmoney.com/api/qt/stock/kline/get"
CALLBACK = "quantPublicKline"


def kline_params(symbol, period, begin, end, adjust):
    if not re.fullmatch(r"\d{6}", symbol):
        raise ValueError("新三板证券代码必须为六位数字")
    periods = {"daily": "101", "day": "101", "weekly": "102", "week": "102", "monthly": "103", "month": "103"}
    if period not in periods or adjust not in {"", "qfq", "hfq"}:
        raise ValueError("不支持的K线周期或复权类型")
    for value in (begin, end):
        if not re.fullmatch(r"\d{8}", value):
            raise ValueError("K线查询日期必须为YYYYMMDD")
        datetime.strptime(value, "%Y%m%d")
    if begin > end:
        raise ValueError("K线起始日期晚于结束日期")
    return {"secid": f"0.{symbol}", "klt": periods[period], "beg": begin, "end": end,
            "fqt": {"": "0", "qfq": "1", "hfq": "2"}[adjust],
            "fields1": "f1,f2,f3,f4,f5,f6", "fields2": "f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61",
            "ut": "c964af255f56da290419b605978b89db", "cb": CALLBACK}


def parse_kline_response(body, symbol, params):
    """Never evaluate JSONP; validate identity, range and price invariants."""
    match = re.fullmatch(r"\s*" + re.escape(CALLBACK) + r"\((.*)\);?\s*", body, re.S)
    payload = json.loads(match[1] if match else body)
    data = payload.get("data") if isinstance(payload, dict) else None
    if not isinstance(payload, dict) or payload.get("rc") != 0 or not isinstance(data, dict) or str(data.get("code")) != symbol or data.get("market") != 0:
        raise ValueError("新三板行情响应未提供匹配的证券身份，不能标记无成交")
    lines = data.get("klines")
    if not isinstance(lines, list):
        raise ValueError("新三板行情响应未提供K线列表")
    seen = set()
    for line in lines:
        fields = str(line).split(",")
        if len(fields) < 7:
            raise ValueError("新三板K线缺少价格、成交量或成交额")
        trade = date.fromisoformat(fields[0])
        day = trade.strftime("%Y%m%d")
        if not params["beg"] <= day <= params["end"] or day in seen:
            raise ValueError("新三板K线日期超出查询范围或重复")
        seen.add(day)
        opening, closing, high, low, volume, amount = map(float, fields[1:7])
        if not all(math.isfinite(v) for v in (opening, closing, high, low, volume, amount)):
            raise ValueError("新三板K线包含非有限数值")
        if min(opening, closing, high, low, volume, amount) < 0 or high < max(opening, closing, low) or low > min(opening, closing):
            raise ValueError("新三板K线价格区间或成交量不合理")
    return payload


def fetch_neeq_browser_kline(symbol, params, *, timeout_ms=20000):
    # Accept only the fixed public chart request; this is not a general browser
    # executor and does not solve challenges or bypass authentication.
    expected = kline_params(symbol, {"101": "daily", "102": "weekly", "103": "monthly"}.get(params.get("klt"), ""),
                            params.get("beg", ""), params.get("end", ""),
                            {"0": "", "1": "qfq", "2": "hfq"}.get(params.get("fqt"), "INVALID"))
    if params != expected:
        raise ValueError("浏览器备源请求参数未匹配受控行情接口")
    from playwright.sync_api import sync_playwright

    if not _SLOTS.acquire(timeout=timeout_ms / 1000):
        raise TimeoutError("公开行情浏览器备源繁忙，需稍后重试")
    try:
        with sync_playwright() as playwright:
            try:
                browser = playwright.chromium.launch(channel="msedge", headless=True)
            except Exception:
                browser = playwright.chromium.launch(headless=True)
            try:
                page = browser.new_page()
                url = f"https://xinsanban.eastmoney.com/QuoteCenter/{symbol}.html"
                response = page.goto(url, wait_until="domcontentloaded", timeout=timeout_ms)
                if not response or response.status != 200:
                    raise ValueError("行情来源页面未成功返回")
                identity = page.locator(".code").first.inner_text(timeout=3000).strip("()（） ")
                if identity != symbol:
                    raise ValueError("行情来源页面的证券身份不匹配")
                # Let the issuer page establish its ordinary browser session.
                # Intercepting all resource requests breaks this public JSONP
                # route on some Edge versions; use the unmodified page request.
                page.wait_for_timeout(3500)
                remote = None
                for attempt in range(2):
                    requested_params = {**params, "_": str(int(time.time() * 1000))}
                    request_url = ENDPOINT + "?" + urlencode(requested_params)
                    def matches(response):
                        parsed = urlparse(response.url)
                        return parsed.netloc == "push2his.eastmoney.com" and parsed.path == "/api/qt/stock/kline/get" and parse_qs(parsed.query) == {k: [v] for k, v in requested_params.items()}
                    try:
                        with page.expect_response(matches, timeout=timeout_ms // 2) as requested:
                            page.evaluate("""url => {
                                let script = document.createElement('script');
                                window.quantPublicKline = value => {window.quantKlineResponse = value;};
                                script.src = url; document.head.appendChild(script);
                            }""", request_url)
                        remote = requested.value
                        break
                    except Exception:
                        if attempt:
                            raise
                        page.wait_for_timeout(1000)
                if remote is None:
                    raise TimeoutError("公开行情备源未返回响应")
                if remote.status != 200:
                    raise ValueError(f"公开行情备源HTTP{remote.status}，不作为无数据")
                body = remote.text()
                payload = parse_kline_response(body, symbol, params)
                proof = {"source_url": remote.url, "source_page_url": url, "params": requested_params,
                         "source_method": "PUBLIC_PAGE_JSONP", "http_status": remote.status,
                         "observed_at": datetime.now(timezone.utc).isoformat(),
                         "response_sha256": sha256(body.encode()).hexdigest(), "original_source_response": body,
                         "identity_status": "CODE_AND_MARKET_MATCH", "adjustment": params["fqt"]}
                return payload, proof
            finally:
                browser.close()
    finally:
        _SLOTS.release()
