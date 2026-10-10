"""Public, read-only P5W issuer disclosures; not an exchange-wide census."""
from __future__ import annotations

from datetime import date, datetime, timezone
from dataclasses import replace
from hashlib import sha256
import json
import re
from typing import Any
from urllib.parse import urlparse

from bs4 import BeautifulSoup
import httpx

from app.connectors.base import MarketDataAdapter, NoticeRecord, NewsRecord

SOURCE_CODE = "P5W_PUBLIC"
SOURCE_NAME = "全景网公开投资者关系披露"
QA_INTERFACE = "P5W_INVESTOR_QA"
BASE = "https://mir.p5w.net"


def uses_p5w(market: str, symbol: str) -> bool:
    # Legacy BSE listings may retain their historical 4/8-prefixed codes
    # under the CN_A identity. Route them to the same public interaction
    # provider as 920 new-code BSE listings, never to CNINFO/SSE QA.
    return market in {"NEEQ", "NEEQ_INNOVATION"} or (
        market == "CN_A" and symbol.startswith(("920", "4", "8"))
    )


def identity(html: str, symbol: str) -> dict[str, Any]:
    soup = BeautifulSoup(html, "html.parser")
    title = soup.title.get_text(strip=True) if soup.title else ""
    match = re.search(r"^(.+?)[（(](\d{6})[）)]投资者关系", title)
    if not match or match[2] != symbol:
        raise ValueError("全景公司页面身份未匹配；未知页面不能证明没有数据")
    pid = re.search(r"companyRoadshowID\s*=\s*'([^']+)'", html)
    return {"symbol": symbol, "name": match[1], "pid": pid[1] if pid else None,
            "url": f"{BASE}/company/{symbol}.html", "page_hash": sha256(html.encode()).hexdigest()}


def disclosure_identity(html: str, symbol: str, url: str) -> dict[str, Any]:
    """Validate disclosures independently of registration on the IR platform."""
    soup = BeautifulSoup(html, "html.parser")
    fields = {}
    for item in soup.select("#security_info li"):
        label, value = item.select_one(".company-page-item-left"), item.select_one(".company-page-item-right")
        if label and value:
            fields[label.get_text(strip=True)] = value.get_text(" ", strip=True)
    if str(fields.get("证券代码", "")).split(".")[0] != symbol:
        raise ValueError("公告备用身份来源的证券代码不匹配")
    name = fields.get("证券简称")
    if not name or name in {"-", "--"}:
        raise ValueError("公告备用身份来源未提供证券简称")
    return {"symbol": symbol, "name": name, "url": url,
            "page_hash": sha256(html.encode()).hexdigest(), "identity_method": "INDEPENDENT_SECURITY_PROFILE",
            "original_identity_response": html}


def evidence(response: httpx.Response) -> dict[str, Any]:
    return {"source_code": SOURCE_CODE, "source_name": SOURCE_NAME, "source_url": str(response.url),
            "observed_at": datetime.now(timezone.utc).isoformat(), "sha256": sha256(response.content).hexdigest(),
            "http_status": response.status_code, "original_source_response": response.text}


def text(value: Any) -> str:
    return BeautifulSoup(str(value or ""), "html.parser").get_text(" ", strip=True)


def parse_qa(data: dict, company: dict, cursor: dict, proof: dict):
    from app.connectors.investor_qa import QAPage, _source_time
    items = data.get("data")
    empty = data.get("code") == 0 and data.get("message") == "没有更多数据了" and items == []
    if not empty and (data.get("code") != 1 or not isinstance(items, list) or not items):
        raise ValueError("全景问答返回异常，未将请求失败标记为空")
    rows = []
    for raw in items:
        if str(raw.get("companyCode")) != company["symbol"] or raw.get("companyPid") != company["pid"]:
            raise ValueError("全景问答公司身份不符，拒绝跨股票入库")
        question, answer = text(raw.get("content")), text(raw.get("replyContent")) or None
        if not question or not raw.get("questionId") or not raw.get("questionDate"):
            raise ValueError("全景问答缺少问题、标识或日期")
        asked, answered = _source_time(raw["questionDate"]), _source_time(raw.get("replyDate"))
        row = {**proof, "source_code": SOURCE_CODE, "source_name": SOURCE_NAME,
               "symbol": company["symbol"], "股票代码": company["symbol"],
               "question_id": str(raw["questionId"]), "question": question, "answer": answer,
               "questioner": raw.get("userName"), "answerer": raw.get("companyShortname") if answer else None,
               "asked_at": asked, "answered_at": answered, "updated_at": answered or asked,
               "source_url": f"{BASE}/question/{raw['questionId']}.html", "raw_payload": raw,
               "status": "ANSWERED" if answer else "PENDING_REPLY", "qa_kind": "PUBLIC_IR_INTERACTION",
               "qa_kind_name": "公开投资者互动（含待回复）", "fetched_at": proof.get("observed_at")}
        row["content_hash"] = sha256(json.dumps(raw, ensure_ascii=False, sort_keys=True).encode()).hexdigest()
        rows.append(row)
    return QAPage(rows, empty, {**cursor, "page": int(cursor.get("page", 1)) + 1,
                  "offset": int(cursor.get("offset", 0)) + len(items), "company": company}, proof["sha256"], proof)


def parse_notices(data: dict, company: dict, market: str, proof: dict, *, quarantine=False) -> list[NoticeRecord]:
    if data.get("code") != 1 or not isinstance(data.get("rows"), list):
        raise ValueError("全景公告格式异常，不能标记无公告")
    result, rejected = [], []
    for raw in data["rows"]:
        title = text(raw.get("s3"))
        issuer = re.split(r"[:：]", title, maxsplit=1)[0]
        normalize = lambda name: re.sub(r"^(?:\*?ST|XD|XR|DR)", "", name, flags=re.I)
        if normalize(issuer) != normalize(company["name"]) or not raw.get("s1"):
            if not quarantine:
                raise ValueError("全景公告缺少匹配的公司名称或唯一标识")
            rejected.append({"source_record": raw, "reason": "公告发行人简称与已验证身份不匹配，需历史名称证据", "status": "QUARANTINED"})
            continue
        listed = str(raw.get("s4") or "")
        date.fromisoformat(listed)
        url = raw.get("s2")
        attached = None
        if url:
            parsed = urlparse(url)
            if parsed.scheme not in {"https", "http"} or parsed.netloc not in {"www.bse.cn", "www.neeq.com.cn", "www.neeq.cc"}:
                if not quarantine:
                    raise ValueError("公告原文未指向预期交易所，需人工核验")
                rejected.append({"source_record": raw, "reason": "附件未匹配官方来源白名单", "status": "QUARANTINED"})
                continue
            match = re.search(r"/disclosure/\d{4}/(\d{4}-\d{2}-\d{2})/", parsed.path)
            if match:
                attached = match[1]
                date.fromisoformat(attached)
        published = attached or listed
        result.append(NoticeRecord(market, company["symbol"], published, title, None, url,
            {**proof, "source_record": raw, "company_identity": company, "external_id": str(raw["s1"]),
             "source_list_date": listed, "attachment_path_date": attached,
             "date_status": "ATTACHMENT_PATH_MATCH" if attached == listed else "LIST_DATE_CONFLICT_ATTACHMENT_PATH" if attached else "SOURCE_LIST_DATE",
             "content_status": "LINK_AVAILABLE" if url else "MISSING_ORIGINAL",
             "link_status": "OFFICIAL_HTTP_SOURCE" if url and urlparse(url).scheme == "http" else "OFFICIAL_HTTPS_SOURCE" if url else "MISSING",
             "issuer_name_status": "EXACT_MATCH" if issuer == company["name"] else "TRADING_PREFIX_NORMALIZED",
             "verification_status": "PENDING", "coverage_scope": "PUBLIC_PROVIDER_LIST"}))
    if rejected:
        result = [replace(record, content_json={**record.content_json,
            "collection_status": "PARTIAL", "rejected_source_records": rejected}) for record in result]
    return result


def parse_news(data: dict, company: dict, market: str, proof: dict) -> list[NewsRecord]:
    if not isinstance(data.get("data"), list) or not isinstance(data.get("total"), int):
        raise ValueError("全景新闻响应格式异常，不能判定无新闻")
    result = []
    for raw in data["data"]:
        if company["symbol"] not in [str(s) for s in raw.get("STOCKCODE") or []]:
            raise ValueError("全景新闻证券代码不符，拒绝跨股票入库")
        title, content = text(raw.get("DOCTITLE")), text(raw.get("DOCABSTRACT"))
        published = str(raw.get("DOCRELTIME") or "")
        datetime.strptime(published, "%Y-%m-%d %H:%M:%S")
        if not title or not raw.get("DOCID"):
            raise ValueError("全景新闻缺少标题或标识")
        result.append(NewsRecord(market=market, symbol=company["symbol"], news_time=published,
            title=title, content=content or None, source_name=SOURCE_NAME,
            url=f"{BASE}/info/qjkx_info.html?id={raw['DOCID']}",
            content_json={**proof, "external_id": str(raw["DOCID"]), "source_record": raw,
                "company_identity": company, "collection_status": "PARTIAL",
                "coverage_scope": "PUBLIC_PROVIDER_NEWS_ARCHIVE", "content_status": "SUMMARY_ONLY"}))
    return result


class P5WClient:
    def __init__(self, *, timeout: float = 12):
        self.client = httpx.Client(timeout=timeout, follow_redirects=True, headers={"User-Agent": "Mozilla/5.0"})

    def __enter__(self):
        return self

    def __exit__(self, *args):
        self.client.close()

    def request(self, method, url, **kwargs):
        response = self.client.request(method, url, **kwargs)
        response.raise_for_status()
        if not response.content.strip():
            raise ValueError("全景网返回空响应，不能判断无数据")
        return response

    def company(self, symbol):
        if not re.fullmatch(r"\d{6}", symbol):
            raise ValueError("全景公司代码必须是六位")
        response = self.request("GET", f"{BASE}/company/{symbol}.html")
        return identity(response.text, symbol)

    def disclosure_company(self, market, symbol):
        try:
            return self.company(symbol)
        except (ValueError, httpx.HTTPError):
            if market not in {"NEEQ", "NEEQ_INNOVATION"}:
                raise
            url = f"https://xinsanban.eastmoney.com/F10/CompanyInfo/Introduction/{symbol}.html"
            response = self.request("GET", url)
            return disclosure_identity(response.text, symbol, url)

    def fetch_page(self, symbol, cursor=None):
        cursor = dict(cursor or {})
        company = cursor.get("company") or self.company(symbol)
        if company.get("symbol") != symbol or not company.get("pid"):
            raise ValueError("全景未提供匹配的公司互动身份，不视为无问答")
        response = self.request("POST", f"{BASE}/company/getQuestionByCid.html",
                                data={"companypid": company["pid"], "rows": int(cursor.get("offset", 0))})
        return parse_qa(response.json(), company, cursor, evidence(response))

    def fetch_notices(self, market, symbol, start_date, end_date, *, max_pages=100):
        company, result, seen, rejected = self.disclosure_company(market, symbol), [], set(), []
        begin, end = date.fromisoformat(start_date), date.fromisoformat(end_date)
        def finish():
            rows = sorted(result, key=lambda r: r.notice_date, reverse=True)
            if rejected and not rows:
                raise ValueError("公告记录未通过发行人身份或附件来源验证，不能标记为无公告")
            if rejected:
                rows = [replace(r, content_json={**r.content_json, "collection_status": "PARTIAL",
                    "rejected_source_records": rejected}) for r in rows]
            return rows
        for page in range(1, max_pages + 1):
            response = self.request("GET", f"{BASE}/api/data/getinterimannouncementlist.html",
                params={"code": symbol, "page": page, "pagesize": 10, "type": "ggzy"})
            original = response.json()
            records = parse_notices(original, company, market, evidence(response), quarantine=True)
            accepted = {r.content_json["external_id"] for r in records}
            rejected.extend({"source_record": raw, "source_url": str(response.url), "status": "QUARANTINED",
                             "reason": "发行人简称或附件来源未通过验证"} for raw in original["rows"] if str(raw.get("s1")) not in accepted)
            if not original["rows"]:
                if page == 1:
                    raise ValueError("全景首屏无公告，公开源覆盖不足，不能证明公司未披露")
                return finish()
            ids = [str(raw.get("s1")) for raw in original["rows"]]
            if all(key in seen for key in ids):
                raise ValueError("全景公告重复分页，未证明完整范围")
            for record in records:
                key = record.content_json["external_id"]
                if key not in seen and begin <= date.fromisoformat(record.notice_date) <= end:
                    result.append(record)
            seen.update(ids)
            # The provider mislabels some report periods as publication dates;
            # do not stop on a single old row or rely on its inaccurate total.
            if records and len(records) == len(original["rows"]) and all(date.fromisoformat(r.notice_date) < begin for r in records):
                return finish()
        raise ValueError("全景公告达到分页上限，不能标记范围完整")

    def fetch_news(self, market, symbol, *, max_pages=100):
        company, result, seen = self.disclosure_company(market, symbol), [], set()
        for page in range(1, max_pages + 1):
            response = self.request("GET", f"{BASE}/info/weyt.html",
                                    params={"stock_code": symbol, "page": page, "size": 20})
            records = parse_news(response.json(), company, market, evidence(response))
            if not records:
                return result
            ids = [r.content_json["external_id"] for r in records]
            if all(key in seen for key in ids):
                raise ValueError("全景新闻重复分页，不能标记范围完整")
            result.extend(record for record, key in zip(records, ids) if key not in seen)
            seen.update(ids)
        raise ValueError("全景新闻达到分页上限，保留公开来源覆盖不足状态")


class P5WPublicAdapter(MarketDataAdapter):
    adapter_type = SOURCE_CODE

    def health_check(self):
        with P5WClient() as client:
            client.fetch_page("920000")
        return "全景网公开公司互动接口连接成功", ["QA", "NOTICE", "NEWS"]

    def fetch_symbol_master(self, market):
        raise NotImplementedError("全景公开来源不提供完整证券主数据")

    def fetch_notices(self, market, symbol, start_date, end_date):
        if market not in {"CN_A", "NEEQ", "NEEQ_INNOVATION"}:
            raise ValueError("全景公开公告适配不支持该市场")
        with P5WClient() as client:
            return client.fetch_notices(market, symbol, start_date, end_date)

    def fetch_news(self, market, symbol):
        if market not in {"CN_A", "NEEQ", "NEEQ_INNOVATION"}:
            raise ValueError("全景公开新闻不支持该市场")
        with P5WClient() as client:
            return client.fetch_news(market, symbol)
