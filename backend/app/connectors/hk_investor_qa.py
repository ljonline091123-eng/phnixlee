"""Issuer-published Hong Kong investor FAQs, with explicit coverage and evidence.

These are company FAQs, not exchange interaction feeds or earnings transcripts.
Only reviewed issuer/URL pairs are collected; a search/forum page is never an
issuer reply. New issuers require a reviewed entry rather than URL guessing.
"""
from __future__ import annotations

from datetime import datetime, timezone
from hashlib import sha256
import json
from typing import Any
from urllib.parse import urljoin, urlsplit

from bs4 import BeautifulSoup

from app.connectors.base import MarketDataAdapter
from app.connectors.hk_research import hk_symbol
from app.connectors.investor_qa import OfficialQAClient, QACollectionError, QAPage


SOURCE_CODE = "HK_ISSUER_IR"
SOURCE_NAME = "港股公司官网投资者问答"
INTERFACE_CODE = "HK_INVESTOR_QA_ON_DEMAND"
FAQ_NOTICE = "官网公开常见问答，不是实时问董秘或业绩会问答；原文未披露的发布日期不以采集日期代替。"

# The mapping is part of the connector contract, not an inference from names.
ISSUER_FAQS = {
    "09868": {"name": "小鹏汽车", "aliases": ("XPeng", "小鹏"), "language": "zh-CN",
              "url": "https://ir.xiaopeng.com/zh-hans/resources/investor-faqs"},
    "09866": {"name": "蔚来", "aliases": ("NIO", "蔚来"), "language": "zh-CN",
              "url": "https://ir.nio.com/zh-hans/resources/investor-faqs"},
    "02015": {"name": "理想汽车", "aliases": ("Li Auto", "理想汽车"), "language": "zh-CN",
              "url": "https://ir.lixiang.com/zh-hans/touzizhechangjianwenti"},
    "09888": {"name": "百度", "aliases": ("Baidu", "百度"), "language": "en",
              "url": "https://ir.baidu.com/shareholder-services/investor-faqs"},
}

# A bounded source search finding, NOT proof that the issuer has no Q&A.
SOURCE_FINDINGS = {
    "02533": {"checked_at": "2026-10-08", "issuer_name": "黑芝麻智能",
              "source_urls": ["https://ir.blacksesame.com/?lang=zh-cn",
                              "https://ir.blacksesame.com/report.html?lang=zh-cn",
                              "https://www.blacksesame.com/zh/list_8/"],
              "message": "已核验公司投关、业绩报告及活动页面，尚未找到可采集的公开问答文字原文；这不代表公司没有举行投资者交流。"},
}


def coverage(symbol: str) -> dict[str, Any]:
    symbol = hk_symbol(symbol)
    issuer = ISSUER_FAQS.get(symbol)
    return {
        "market": "HK", "symbol": symbol, "source_code": SOURCE_CODE,
        "source_name": SOURCE_NAME, "qa_kind": "OFFICIAL_FAQ",
        "coverage_status": "PARTIAL" if issuer else "MISSING",
        "supported": issuer is not None, "coverage_scope": "REVIEWED_ISSUER_FAQ",
        "source_urls": [issuer["url"]] if issuer else [],
        "message": FAQ_NOTICE if issuer else "尚未接入该公司的公开问答原文来源，不代表该公司确实没有问答。",
        **(SOURCE_FINDINGS.get(symbol) or {}),
    }


def parse_issuer_faq(text: str, symbol: str, *, url: str) -> QAPage:
    symbol = hk_symbol(symbol)
    issuer = ISSUER_FAQS.get(symbol)
    if issuer is None:
        raise QACollectionError("此证券尚未核验公司问答来源")
    if urlsplit(url).hostname != urlsplit(issuer["url"]).hostname:
        raise QACollectionError("公司问答来源域名与已核验证券身份不一致")
    soup = BeautifulSoup(text, "html.parser")
    title = soup.title.get_text(" ", strip=True) if soup.title else ""
    items = soup.select(".nir-faq--item-wrapper")
    body = " ".join(item.get_text(" ", strip=True) for item in items)
    # Navigation links can mention unrelated issuers. Validate the title and
    # the actual FAQ body, not the whole page or a numeric substring.
    if not any(alias.casefold() in (title + " " + body).casefold() for alias in issuer["aliases"]):
        raise QACollectionError("公司问答页面缺少匹配的发行主体身份")
    if not items:
        raise QACollectionError("公司问答页面结构变更或访问受限；不能判为无问答")
    document_hash = sha256(text.encode("utf-8")).hexdigest()
    fetched_at = datetime.now(timezone.utc).isoformat()
    rows = []
    for item in items:
        q, a = item.select_one(".nir-faq--question"), item.select_one(".nir-faq--answer")
        question = q.get_text(" ", strip=True) if q else ""
        answer = a.get_text(" ", strip=True) if a else ""
        if not question or not answer:
            raise QACollectionError("公司常见问答缺少问题或回答正文；保留历史数据并重试")
        identity = sha256((symbol + "|" + question).encode("utf-8")).hexdigest()
        raw_html = str(item)
        rows.append({
            "question_id": "faq:" + identity, "market": "HK", "symbol": symbol,
            "question": question, "answer": answer, "questioner": None,
            "answerer": issuer["name"] + "官网", "issuer_name": issuer["name"],
            "source_code": SOURCE_CODE, "source_name": issuer["name"] + "官网投资者常见问答",
            "source_url": url, "detail_url": url, "qa_kind": "OFFICIAL_FAQ",
            "qa_kind_name": "官网常见问答", "language": issuer["language"],
            "asked_at": None, "answered_at": None, "updated_at": None,
            "date_status": "UNDISCLOSED", "fetched_at": fetched_at,
            "status": "ISSUER_PUBLISHED", "evidence_status": "ISSUER_PUBLISHED",
            "content_hash": sha256(json.dumps([question, answer], ensure_ascii=False).encode("utf-8")).hexdigest(),
            "source_content_hash": document_hash, "raw_html": raw_html,
            "evidence_links": [urljoin(url, link["href"]) for link in item.select('a[href]')],
        })
    return QAPage(rows, True, {"page": 2}, document_hash)


class HongKongQAClient(OfficialQAClient):
    def fetch_page(self, symbol: str, cursor: dict[str, Any] | None = None) -> QAPage:
        symbol = hk_symbol(symbol)
        issuer = ISSUER_FAQS.get(symbol)
        if issuer is None:
            raise QACollectionError("此证券尚未接入已核验的公司问答原文来源")
        url = issuer["url"]
        allowed_host = urlsplit(url).hostname
        # Reject an unreviewed redirect before requesting it, including an
        # authentication/advertising page that happens to return HTTP 200.
        for _ in range(4):
            response = self._request("GET", url, follow_redirects=False)
            if response.is_redirect:
                location = response.headers.get("location")
                if not location:
                    raise QACollectionError("问答来源重定向缺少目标地址")
                url = urljoin(url, location)
                target = urlsplit(url)
                if target.scheme != "https" or target.hostname != allowed_host or target.port not in (None, 443) or target.username:
                    raise QACollectionError("问答页面跳转到未经核验的来源")
                continue
            return parse_issuer_faq(response.text, symbol, url=str(response.url))
        raise QACollectionError("问答来源重定向次数超限")


class HongKongQAAdapter(MarketDataAdapter):
    adapter_type = SOURCE_CODE

    def health_check(self) -> tuple[str, list[str]]:
        with HongKongQAClient() as client:
            page = client.fetch_page("09868")
        return f"已获取小鹏官网常见问答 {len(page.rows)} 条；仅覆盖已核验发行主体", ["QA"]

    def fetch_symbol_master(self, market: str):
        raise NotImplementedError("公司问答来源不提供股票主数据")

    def fetch_extended_data(self, market: str, symbol: str) -> dict[str, Any]:
        if market != "HK":
            raise ValueError("此来源仅用于港股公司问答")
        info = coverage(symbol)
        if not info["supported"]:
            return {"research_sections": {"qa": [], "qa_status": "MISSING", "qa_message": info["message"]}}
        with HongKongQAClient() as client:
            page = client.fetch_page(symbol)
        return {"research_sections": {"qa": page.rows, "qa_source": SOURCE_NAME, "qa_message": FAQ_NOTICE}}
