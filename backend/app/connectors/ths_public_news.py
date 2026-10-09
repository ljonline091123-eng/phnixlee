"""Issuer-scoped public news from the server-rendered stock page.

The page is a limited news/announcement feed, not a complete news archive.
Announcements remain in the announcement ingestion path.
"""
from datetime import datetime, timezone
import hashlib
import json
import re
from urllib.parse import urlsplit

from app.connectors.base import NewsRecord


def parse_stock_page(body, market, symbol, *, observed_at=None):
    observed_at = observed_at or datetime.now(timezone.utc)
    matches = []

    def visit(value):
        if isinstance(value, dict):
            if "initialUpperState" in value and "stockCode" in value:
                matches.append(value)
            for child in value.values():
                visit(child)
        elif isinstance(value, list):
            for child in value:
                visit(child)

    for chunk in re.findall(r"self\.__next_f\.push\((.*?)\)</script>", body, re.S):
        try:
            envelope = json.loads(chunk)
        except ValueError:
            continue
        if not isinstance(envelope, list) or len(envelope) < 2 or not isinstance(envelope[1], str):
            continue
        for line in envelope[1].splitlines():
            prefix, separator, content = line.partition(":")
            if not separator or not re.fullmatch(r"[0-9a-f]+", prefix):
                continue
            try:
                visit(json.loads(content))
            except ValueError:
                continue
    if len(matches) != 1:
        raise ValueError("同花顺公开新闻页缺少唯一证券身份，不能判定无新闻")
    state = matches[0]
    if market not in {"NEEQ", "NEEQ_INNOVATION"} or str(state.get("stockCode")) != symbol or str(state.get("marketId")) != "145":
        raise ValueError("同花顺新闻页证券代码或市场不符，拒绝跨证券入库")
    name = re.sub(r"\s", "", str(state.get("stockName") or ""))
    if not name:
        raise ValueError("同花顺新闻页缺少主体名称")
    feed = state.get("initialUpperState") or {}
    if not isinstance(feed.get("items"), list) or feed.get("error"):
        raise ValueError("同花顺新闻页数据状态异常")
    source_url = f"https://stockpage.10jqka.com.cn/{symbol}/"
    evidence = {"url": source_url, "fetched_at": observed_at.isoformat(),
                "response_sha256": hashlib.sha256(body.encode("utf-8")).hexdigest(),
                "response_representation": "DECODED_UTF8_HTML"}
    records, rejected, seen = [], [], set()
    for item in feed["items"]:
        if not isinstance(item, dict) or item.get("type") != "regular":
            continue
        title, url = str(item.get("title") or "").strip(), str(item.get("jumpUrl") or "")
        if name not in re.sub(r"\s", "", title) and not re.search(rf"(?<!\d){re.escape(symbol)}(?!\d)", title):
            rejected.append({"id": item.get("id"), "reason": "主体提及未匹配"})
            continue
        parsed = urlsplit(url)
        if parsed.scheme not in {"https", "http"} or parsed.hostname != "news.10jqka.com.cn":
            rejected.append({"id": item.get("id"), "reason": "新闻链接来源不符"})
            continue
        label = str(item.get("timeLabel") or "")
        full = re.fullmatch(r"(\d{4}-\d{2}-\d{2})\s+(\d{2}:\d{2})", label)
        short = re.fullmatch(r"(\d{2}-\d{2})\s+(\d{2}:\d{2})", label)
        path_day = re.search(r"/(\d{8})/", parsed.path)
        try:
            if full:
                stamp = datetime.strptime(label, "%Y-%m-%d %H:%M")
                method = "PAGE_FULL_PUBLICATION_DATE"
            elif short and path_day and int(path_day[1][:4]) == observed_at.year:
                # The URL supports the year only. Publication day/time comes
                # from the page; URL dates can be syndication dates.
                stamp = datetime.strptime(f"{observed_at.year}-{label}", "%Y-%m-%d %H:%M")
                linked = datetime.strptime(path_day[1], "%Y%m%d")
                if abs((stamp.date() - linked.date()).days) > 1:
                    raise ValueError("年份或日期存在歧义")
                method = "PAGE_PUBLICATION_DATE_URL_YEAR"
            else:
                raise ValueError("缺少明确发布日期")
            if stamp.date() > observed_at.date():
                raise ValueError("发布日期在采集日之后")
        except ValueError as exc:
            rejected.append({"id": item.get("id"), "reason": str(exc)})
            continue
        key = (title, stamp.isoformat())
        if key in seen:
            continue
        seen.add(key)
        records.append(NewsRecord(market=market, symbol=symbol,
            news_time=stamp.strftime("%Y-%m-%d %H:%M:%S"), title=title,
            content=item.get("summary") or None,
            source_name=str(item.get("source") or "同花顺公开公司新闻"), url=url,
            content_json={"source_method": "THS_PUBLIC_STOCK_PAGE", "source_row": item,
                "source_identity": {"symbol": symbol, "market_id": "145", "name": state["stockName"]},
                "date_method": method, "source_evidence": evidence,
                "content_status": "SUMMARY_ONLY", "collection_status": "PARTIAL",
                "collection_scope": "公开个股页面展示的有限新闻，未证明全量覆盖"}))
    if not records:
        raise ValueError("同花顺公开页面没有主体和日期均可验证的新闻，不能证明无新闻")
    records[0].content_json.update(original_source_response={**evidence, "html": body},
                                   rejected_source_rows=rejected, has_more=feed.get("hasMore"))
    return sorted(records, key=lambda row: row.news_time, reverse=True)


def fetch_public_stock_news(market, symbol):
    # The same browser-compatible HTTP transport is used by existing public
    # sources. A rejected request is reported as a source error.
    from curl_cffi import requests
    url = f"https://stockpage.10jqka.com.cn/{symbol}/"
    response = requests.get(url, timeout=15, impersonate="chrome", headers={
        "User-Agent": "Mozilla/5.0", "Referer": "https://stockpage.10jqka.com.cn/"})
    response.raise_for_status()
    return parse_stock_page(response.text, market, symbol)
