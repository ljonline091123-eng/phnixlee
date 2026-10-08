"""Dated, issuer-specific public HK news fallback, retaining page evidence."""
from datetime import datetime, timezone
import hashlib
import re
from urllib.parse import urljoin

from bs4 import BeautifulSoup
import httpx


def parse_aastocks_news(html_text: str, symbol: str, source_url: str) -> list[dict]:
    soup = BeautifulSoup(html_text, "html.parser")
    rows, seen = [], set()
    for link in soup.select('a[id*="repNews_lnkNews_"][href]'):
        href = link.get("href", "")
        if f"/stock-aafn-con/{symbol}/" not in href:
            continue
        title = link.get_text(" ", strip=True)
        container = link.find_parent("div", attrs={"ref": True})
        if not title or container is None:
            continue
        dates = re.findall(r"dt:\s*['\"](\d{4}/\d{2}/\d{2}\s+\d{2}:\d{2})['\"]", str(container))
        if len(set(dates)) != 1:
            continue
        published_at = datetime.strptime(dates[0], "%Y/%m/%d %H:%M").isoformat(sep=" ")
        url = urljoin(source_url, href)
        if url in seen:
            continue
        seen.add(url)
        summary = container.select_one(".newscontent4")
        rows.append({"title": title, "news_time": published_at, "url": url,
                     "content": summary.get_text(" ", strip=True) if summary else None,
                     "source_method": "AASTOCKS_PUBLIC_NEWS_HTML", "source_name": "AASTOCKS公开相关新闻",
                     "content_kind": "PROVIDER_MODEL_COMMENTARY" if "/AI/" in href else "NEWS_SUMMARY",
                     "time_zone": "Asia/Hong_Kong", "time_precision": "MINUTE",
                     "collection_status": "PARTIAL", "collection_scope": "公开列表的带日期新闻摘要，非全量新闻或全文"})
    return rows


def fetch_aastocks_news(symbol: str) -> list[dict]:
    if not re.fullmatch(r"\d{5}", symbol):
        raise ValueError("港股证券代码必须为五位数字")
    url = f"https://www.aastocks.com/sc/stocks/analysis/stock-aafn/{symbol}/0/hk-stock-news/1"
    with httpx.Client(timeout=15, trust_env=False, follow_redirects=True) as client:
        response = client.get(url, headers={"User-Agent": "Mozilla/5.0"})
        response.raise_for_status()
    rows = parse_aastocks_news(response.text, symbol, url)
    evidence = {"url": url, "fetched_at": datetime.now(timezone.utc).isoformat(),
                "response_sha256": hashlib.sha256(response.content).hexdigest(), "html": response.text}
    for row in rows:
        row["source_evidence"] = {k: v for k, v in evidence.items() if k != "html"}
    if rows:
        rows[0]["original_source_response"] = evidence
    return rows
