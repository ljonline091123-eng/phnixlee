"""Verify public-source status for NEEQ samples without writing business data.

An empty response is classified as ``UNAVAILABLE`` only after the official
issuer identity and three independent public market endpoints are checked.
Transport failures remain ``SOURCE_FAILED`` and are never converted to an
absence claim.
"""
from __future__ import annotations

from datetime import datetime, timezone
from hashlib import sha256
import json
from pathlib import Path
import re
import sys
import time
import os

import httpx

ROOT = Path(__file__).resolve().parents[1]
OUT = Path(os.environ.get(
    "QUANT_AUDIT_OUT",
    ROOT / "validation" / "all-boards-20261008",
))
sys.path.insert(0, str(ROOT))

from app.connectors.neeq_official import fetch_company


EASTMONEY_API = "https://xinsanban.eastmoney.com/api/F10/MarketQuotation/GetTransactionDetail"
EASTMONEY_PAGE = "https://xinsanban.eastmoney.com/F10/MarketQuotation/F10TradeStatistics/{symbol}.html"
THS_API = "https://quota-h.10jqka.com.cn/fuyao/common_hq_aggr/quote/v1/single_kline"
SINA_API = "https://quotes.sina.cn/cn/api/jsonp_v2.php/var _neeq=/CN_MarketDataService.getKLineData"


def _sha(value: str) -> str:
    return sha256(value.encode("utf-8", errors="replace")).hexdigest()


def _headers(referer: str | None = None) -> dict[str, str]:
    headers = {"User-Agent": "Mozilla/5.0", "Accept": "application/json,text/plain,*/*"}
    if referer:
        headers["Referer"] = referer
        headers["Origin"] = "https://stockpage.10jqka.com.cn"
    return headers


def _get_json_with_retry(client: httpx.Client, url: str, *, params: dict, headers: dict) -> tuple[dict, httpx.Response, int]:
    errors: list[str] = []
    for attempt in range(1, 4):
        try:
            response = client.get(url, params=params, headers=headers)
            response.raise_for_status()
            if not response.text.strip():
                raise ValueError("来源返回空响应体")
            payload = response.json()
            if not isinstance(payload, dict):
                raise ValueError("来源响应不是JSON对象")
            return payload, response, attempt
        except Exception as exc:
            errors.append(f"第{attempt}次：{exc}")
            if attempt < 3:
                time.sleep(attempt * 0.5)
    try:
        from curl_cffi import requests as curl_requests

        response = curl_requests.get(url, params=params, headers=headers, timeout=20, impersonate="chrome")
        response.raise_for_status()
        payload = response.json()
        if not isinstance(payload, dict):
            raise ValueError("浏览器兼容响应不是JSON对象")
        return payload, response, 4
    except Exception as exc:
        errors.append(f"浏览器兼容传输：{exc}")
        raise RuntimeError("；".join(errors)) from exc


def check_symbol(sample: dict) -> dict:
    symbol = sample["symbol"]
    checked_at = datetime.now(timezone.utc).isoformat()
    result = {"board": sample["board"], "market": sample["market"], "symbol": symbol,
              "name": sample["name"], "checked_at": checked_at, "sources": []}

    try:
        official = fetch_company(symbol)
        source_record = official.get("source_record") or {}
        identity_ok = str(source_record.get("xxzqdm")) == symbol and str(source_record.get("xxzqjc") or "").strip() == sample["name"]
        result["sources"].append({"source_code": "NEEQ_OFFICIAL", "status": "PASS" if identity_ok else "FAILED",
                                   "identity_ok": identity_ok, "response_sha256": official.get("response_sha256"),
                                   "source_date": official.get("source_date")})
        if not identity_ok:
            result.update(status="SOURCE_FAILED", reason="全国股转官方名录身份不匹配")
            return result
    except Exception as exc:
        result["sources"].append({"source_code": "NEEQ_OFFICIAL", "status": "FAILED", "error": str(exc)[:500]})
        result.update(status="SOURCE_FAILED", reason="全国股转官方名录调用失败")
        return result

    with httpx.Client(timeout=20, trust_env=False, follow_redirects=True) as client:
        try:
            payload, response, attempts = _get_json_with_retry(
                client, EASTMONEY_API,
                params={"code": symbol, "page": 1, "pagesize": 100, "sortRule": -1, "sortType": "TRADEDATE"},
                headers=_headers("https://xinsanban.eastmoney.com/"),
            )
            rows = payload.get("result") if isinstance(payload, dict) else None
            pages = payload.get("TotalPage") if isinstance(payload, dict) else None
            empty = response.status_code == 200 and payload.get("IsSuccess") in (True, 1) and pages == 0 and rows == []
            result["sources"].append({"source_code": "NEEQ_EASTMONEY_DAILY", "status": "EMPTY" if empty else "DATA" if rows else "FAILED",
                                       "http_status": response.status_code, "total_pages": pages,
                                       "rows": len(rows) if isinstance(rows, list) else None,
                                       "response_sha256": _sha(response.text), "attempts": attempts})
        except Exception as exc:
            result["sources"].append({"source_code": "NEEQ_EASTMONEY_DAILY", "status": "FAILED", "error": str(exc)[:500]})
            empty = False
            rows = None

        try:
            page_response = client.get(EASTMONEY_PAGE.format(symbol=symbol), headers=_headers("https://xinsanban.eastmoney.com/"))
            title_match = re.search(rf"<title>[^<]*\({re.escape(symbol)}\)", page_response.text, re.I) is not None
            code_match = re.search(rf'var CODE\s*=\s*["\']{re.escape(symbol)}["\']', page_response.text) is not None
            result["sources"].append({"source_code": "NEEQ_EASTMONEY_PAGE", "status": "PASS" if page_response.status_code == 200 and title_match and code_match else "FAILED",
                                       "http_status": page_response.status_code, "identity_ok": title_match and code_match,
                                       "response_sha256": _sha(page_response.text)})
        except Exception as exc:
            result["sources"].append({"source_code": "NEEQ_EASTMONEY_PAGE", "status": "FAILED", "error": str(exc)[:500]})
            page_response = None

        try:
            request_payload = {"code_list": [{"codes": [symbol], "market": "145"}], "trade_class": "intraday",
                               "time_period": "day_1", "trade_date": -1, "begin_time": -7000, "end_time": 0,
                               "adjust_type": "forward", "gpid": 1}
            response = client.post(THS_API, json=request_payload, headers=_headers(f"https://stockpage.10jqka.com.cn/{symbol}/"))
            payload = response.json()
            data = payload.get("data") if isinstance(payload, dict) else None
            quotes = data.get("quote_data") if isinstance(data, dict) else None
            fail = data.get("fail_params") if isinstance(data, dict) else None
            ths_empty = response.status_code == 200 and payload.get("status_code") == 0 and quotes == [] and isinstance(fail, dict)
            result["sources"].append({"source_code": "THS_PUBLIC_NEEQ_KLINE", "status": "EMPTY" if ths_empty else "DATA" if quotes else "FAILED",
                                       "http_status": response.status_code, "rows": len(quotes) if isinstance(quotes, list) else None,
                                       "response_sha256": _sha(response.text), "request": request_payload})
        except Exception as exc:
            result["sources"].append({"source_code": "THS_PUBLIC_NEEQ_KLINE", "status": "FAILED", "error": str(exc)[:500]})
            ths_empty = False
            quotes = None

        try:
            response = client.get(SINA_API, params={"symbol": f"sb{symbol}", "scale": 240, "ma": "no", "datalen": 1023},
                                  headers={"User-Agent": "Mozilla/5.0", "Referer": "https://finance.sina.com.cn/"})
            body = response.text
            sina_empty = response.status_code == 200 and ("=(null)" in body or "=null" in body)
            result["sources"].append({"source_code": "SINA_NEEQ_KLINE", "status": "EMPTY" if sina_empty else "DATA" if body else "FAILED",
                                       "http_status": response.status_code, "response_sha256": _sha(body),
                                       "empty_response": sina_empty})
        except Exception as exc:
            result["sources"].append({"source_code": "SINA_NEEQ_KLINE", "status": "FAILED", "error": str(exc)[:500]})
            sina_empty = False

    public_page_ok = any(item.get("source_code") == "NEEQ_EASTMONEY_PAGE" and item.get("status") == "PASS" for item in result["sources"])
    if empty and ths_empty and sina_empty and public_page_ok:
        result.update(status="UNAVAILABLE", reason="官方身份、东方财富交易表、同花顺公开K线和新浪公开K线均核验；三条市场源无历史记录")
    else:
        result.update(status="SOURCE_FAILED", reason="公开来源未形成完整的可审计空结果集合")
    return result


def main() -> None:
    samples = json.loads((OUT / "samples.json").read_text(encoding="utf-8"))
    samples = [sample for sample in samples if sample["board"].startswith("NEEQ_") and not json.loads(
        (OUT / "stocks" / f"{sample['board']}-{sample['symbol']}.json").read_text(encoding="utf-8")
    ).get("local", {}).get("counts", {}).get("kline")]
    results = [check_symbol(sample) for sample in samples]
    report = {"checked_at": datetime.now(timezone.utc).isoformat(), "scope": "NEEQ fixed samples",
              "status_definitions": {"UNAVAILABLE": "已核验公开来源明确无可用日线记录；不是无交易的事实", "SOURCE_FAILED": "来源失败，不能判定无数据"},
              "results": results, "status": "PASS" if all(row["status"] in {"UNAVAILABLE", "PASS"} for row in results) else "PARTIAL"}
    (OUT / "neeq-kline-public-status.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"status": report["status"], "unavailable": sum(row["status"] == "UNAVAILABLE" for row in results),
                      "source_failed": sum(row["status"] == "SOURCE_FAILED" for row in results)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
