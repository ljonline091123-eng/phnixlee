from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor, as_completed
from hashlib import sha256
from html import unescape
import re
from typing import Any
from urllib.parse import urljoin

import httpx


SINA_RESEARCH_BASE = "https://stock.finance.sina.com.cn"
SINA_RESEARCH_LIST = f"{SINA_RESEARCH_BASE}/stock/go.php/vReport_List/kind/search/index.phtml"


def _decode_response(response: httpx.Response) -> str:
    # Sina declares GBK/GB2312 and currently returns GBK-compatible bytes.
    # Decode bytes explicitly so a client-side charset guess cannot corrupt
    # titles, institutions or the report正文 before they are persisted.
    return response.content.decode("gb18030", errors="replace")


def _clean_html(fragment: str) -> str:
    text = re.sub(r"<br\s*/?>", "\n", fragment, flags=re.I)
    text = re.sub(r"</p\s*>", "\n", text, flags=re.I)
    text = re.sub(r"<[^>]+>", "", text)
    text = unescape(text).replace("\r", "")
    lines = [re.sub(r"[\t\u3000 ]+", " ", line).strip() for line in text.splitlines()]
    return "\n".join(line for line in lines if line).strip()


def _extract_rating(text: str) -> str | None:
    matches = re.findall(
        r"(?:维持|给予|首次覆盖(?:并给予)?|下调至|上调至)?[“\"']?"
        r"(强烈推荐|强烈买入|买入|增持|推荐|持有|中性|减持|卖出|回避)"
        r"[”\"']?(?:评级|建议)",
        text,
    )
    if not matches:
        return None
    value = matches[-1]
    aliases = {"强烈推荐": "买入", "强烈买入": "买入", "推荐": "买入", "回避": "卖出"}
    return aliases.get(value, value)


def _parse_report_rows(html: str, symbol: str) -> list[dict[str, Any]]:
    reports: list[dict[str, Any]] = []
    for row_html in re.findall(r"<tr\b[^>]*>(.*?)</tr>", html, flags=re.I | re.S):
        link = re.search(
            r"<a\b[^>]*title=[\"']([^\"']+)[\"'][^>]*href=[\"']([^\"']*vReport_Show[^\"']+)[\"']",
            row_html,
            flags=re.I | re.S,
        )
        if link is None:
            link = re.search(
                r"<a\b[^>]*href=[\"']([^\"']*vReport_Show[^\"']+)[\"'][^>]*title=[\"']([^\"']+)[\"']",
                row_html,
                flags=re.I | re.S,
            )
            if link is None:
                continue
            href, title = link.group(1), unescape(link.group(2)).strip()
        else:
            title, href = unescape(link.group(1)).strip(), link.group(2)
        external_match = re.search(r"/rptid/([^/]+)/", href)
        if not external_match or not title:
            continue
        cells = re.findall(r"<td\b[^>]*>(.*?)</td>", row_html, flags=re.I | re.S)
        cell_text = [_clean_html(item) for item in cells]
        report_date = next(
            (value[:10] for value in cell_text if re.fullmatch(r"20\d{2}-\d{2}-\d{2}", value[:10])),
            "",
        )
        institution = cell_text[4] if len(cell_text) > 4 else ""
        analysts = [item.strip() for item in re.split(r"[/、,，]", cell_text[5] if len(cell_text) > 5 else "") if item.strip()]
        detail_url = urljoin(SINA_RESEARCH_BASE, href)
        reports.append({
            "股票代码": symbol,
            "report_id": f"SINA-{external_match.group(1)}",
            "external_id": external_match.group(1),
            "source_code": "SINA_FINANCE",
            "source_name": "新浪财经机构研报",
            "title": title,
            "报告名称": title,
            "report_date": report_date,
            "日期": report_date,
            "institution": institution or None,
            "机构": institution or None,
            "analysts": analysts,
            "研究员": "/".join(analysts),
            "source_url": SINA_RESEARCH_LIST,
            "detail_url": detail_url,
            "url": detail_url,
            "source_updated_at": report_date or None,
            "content_status": "PENDING",
            "tags": [item for item in (institution, *analysts) if item],
        })
    deduped: dict[str, dict[str, Any]] = {}
    for item in reports:
        deduped.setdefault(str(item["external_id"]), item)
    return sorted(deduped.values(), key=lambda row: str(row.get("report_date") or ""), reverse=True)


def fetch_sina_report_detail(
    external_id: str,
    *,
    detail_url: str | None = None,
    timeout: float = 10.0,
) -> dict[str, Any]:
    url = detail_url or (
        f"{SINA_RESEARCH_BASE}/stock/go.php/vReport_Show/kind/search/"
        f"rptid/{external_id}/index.phtml"
    )
    response = httpx.get(
        url,
        timeout=timeout,
        follow_redirects=True,
        headers={"User-Agent": "Mozilla/5.0 (compatible; GeminiQuantResearch/1.0)"},
    )
    response.raise_for_status()
    html = _decode_response(response)
    title_match = re.search(r"<div\s+class=[\"']content[\"']>.*?<h1>(.*?)</h1>", html, flags=re.I | re.S)
    metadata_match = re.search(r"<div\s+class=[\"']creab[\"']>(.*?)</div>", html, flags=re.I | re.S)
    content_match = re.search(r"<div\s+class=[\"']blk_container[\"']>(.*?)</div>", html, flags=re.I | re.S)
    content = _clean_html(content_match.group(1)) if content_match else ""
    if not content:
        raise ValueError("新浪研报详情页未返回可解析正文")
    metadata = _clean_html(metadata_match.group(1)) if metadata_match else ""
    report_date_match = re.search(r"日期[：:]\s*(20\d{2}-\d{2}-\d{2})", metadata)
    institution_match = re.search(r"机构[：:]\s*([^\n]+?)(?=研究员[：:]|日期[：:]|$)", metadata)
    analyst_match = re.search(r"研究员[：:]\s*([^\n]+?)(?=日期[：:]|$)", metadata)
    title = _clean_html(title_match.group(1)) if title_match else ""
    analysts = [
        item.strip()
        for item in re.split(r"[/、,，]", analyst_match.group(1) if analyst_match else "")
        if item.strip()
    ]
    return {
        "external_id": str(external_id),
        "source_code": "SINA_FINANCE",
        "source_name": "新浪财经机构研报",
        "title": title,
        "report_date": report_date_match.group(1) if report_date_match else "",
        "institution": institution_match.group(1).strip() if institution_match else None,
        "analysts": analysts,
        "rating": _extract_rating(content),
        "summary": content[:500],
        "content": content,
        "content_hash": sha256(content.encode("utf-8")).hexdigest(),
        "content_status": "READY",
        "source_url": SINA_RESEARCH_LIST,
        "detail_url": url,
        "source_updated_at": report_date_match.group(1) if report_date_match else None,
    }


def fetch_sina_research_reports(
    symbol: str,
    *,
    limit: int = 100,
    detail_limit: int = 10,
    timeout: float = 10.0,
) -> list[dict[str, Any]]:
    """Fetch bounded Sina report history and pre-hydrate newest report bodies.

    The endpoint exposes up to dozens of historical pages. We intentionally
    bound synchronous F10 traffic; Eastmoney remains the historical fallback,
    while Sina supplies the newer public rows that Eastmoney can miss.
    """
    rows: list[dict[str, Any]] = []
    page = 1
    page_size_observed = 0
    while len(rows) < limit:
        response = httpx.get(
            SINA_RESEARCH_LIST,
            params={"symbol": symbol, "t1": "all", "p": page},
            timeout=timeout,
            follow_redirects=True,
            headers={"User-Agent": "Mozilla/5.0 (compatible; GeminiQuantResearch/1.0)"},
        )
        response.raise_for_status()
        page_rows = _parse_report_rows(_decode_response(response), symbol)
        if not page_rows:
            break
        if page > 1 and page_rows[0].get("external_id") == rows[0].get("external_id"):
            break
        rows.extend(page_rows)
        page_size_observed = page_size_observed or len(page_rows)
        if len(page_rows) < page_size_observed:
            break
        page += 1
    deduped: dict[str, dict[str, Any]] = {}
    for item in rows:
        deduped.setdefault(str(item["external_id"]), item)
    result = sorted(deduped.values(), key=lambda row: str(row.get("report_date") or ""), reverse=True)[:limit]

    newest = result[: max(0, min(detail_limit, len(result)))]
    if newest:
        with ThreadPoolExecutor(max_workers=min(4, len(newest))) as executor:
            futures = {
                executor.submit(
                    fetch_sina_report_detail,
                    str(item["external_id"]),
                    detail_url=str(item.get("detail_url") or "") or None,
                    timeout=timeout,
                ): item
                for item in newest
            }
            for future in as_completed(futures):
                item = futures[future]
                try:
                    detail = future.result()
                except Exception as exc:  # each report is independently optional
                    item["content_status"] = "FAILED"
                    item["fetch_error"] = str(exc)[:500]
                    continue
                item.update({key: value for key, value in detail.items() if value not in (None, "")})
                item["报告日期"] = item.get("report_date")
                item["东财评级"] = item.get("rating")
    return result
