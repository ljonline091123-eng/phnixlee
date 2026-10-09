"""Exact issuer verification against the public NEEQ company directory."""
from datetime import datetime, timezone
import hashlib
import json
import re
import time

ENDPOINT = "https://www.neeq.com.cn/nqxxController/nqxxCnzq.do"


def parse_company_response(body, callback, symbol, observed_at=None):
    match = re.fullmatch(r"\s*" + re.escape(callback) + r"\((.*)\)\s*;?\s*", body, re.S)
    if not match:
        raise ValueError("全国股转公司名录响应不是预期JSONP")
    payload = json.loads(match[1])
    if not isinstance(payload, list) or len(payload) != 1 or not isinstance(payload[0], dict):
        raise ValueError("全国股转公司名录响应格式异常")
    page = payload[0]
    rows = page.get("content")
    if not isinstance(rows, list) or page.get("totalElements") != len(rows) or page.get("totalPages") not in (0, 1):
        raise ValueError("全国股转公司名录分页或记录数异常")
    matches = [row for row in rows if isinstance(row, dict) and str(row.get("xxzqdm")) == symbol]
    if len(matches) != 1 or len(rows) != 1:
        raise ValueError("全国股转公司名录未返回唯一匹配证券")
    row = matches[0]
    layer = {"0": "NEEQ_BASE", "1": "NEEQ_INNOVATION"}.get(str(row.get("xxfcbj")))
    if not layer or not str(row.get("xxzqjc") or "").strip():
        raise ValueError("全国股转公司名录缺少简称或有效市场层级")
    return {"source_code": "NEEQ_OFFICIAL", "source_name": "全国股转系统官方挂牌公司名录",
            "source_url": ENDPOINT, "observed_at": (observed_at or datetime.now(timezone.utc)).isoformat(),
            "response_sha256": hashlib.sha256(body.encode()).hexdigest(), "source_record": row,
            "listing_board": layer, "listing_board_name": "基础层" if layer == "NEEQ_BASE" else "创新层",
            "source_date": str(row.get("xxjsrq") or ""), "identity_status": "CODE_MATCH"}


def fetch_company(symbol):
    if not re.fullmatch(r"\d{6}", symbol):
        raise ValueError("全国股转证券代码必须为六位数字")
    import httpx
    callback = "quantNeeq" + str(int(time.time() * 1000))
    form = {"page": "0", "typejb": "T", "xxfcbj[]": ["0", "1"],
            "xxzqdm": symbol, "neeqhyfl": "1", "sortfield": "xxzqdm", "sorttype": "asc"}
    with httpx.Client(timeout=15, trust_env=False, follow_redirects=True, headers={
            "User-Agent": "Mozilla/5.0", "Referer": "https://www.neeq.com.cn/nq/listedcompany.html",
            "X-Requested-With": "XMLHttpRequest"}) as client:
        response = client.post(ENDPOINT, params={"callback": callback, "_": str(int(time.time() * 1000))},
                               data=form)
        response.raise_for_status()
    result = parse_company_response(response.text, callback, symbol)
    result["request"] = {"method": "POST", "form": form}
    result["original_source_response"] = response.text
    return result
