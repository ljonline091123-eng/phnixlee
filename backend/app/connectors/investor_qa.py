"""Exchange-scoped official investor Q&A, with bounded requests and explicit EOF."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from hashlib import sha256
import json
import re
import time
from typing import Any
from zoneinfo import ZoneInfo

from bs4 import BeautifulSoup
import httpx


class QACollectionError(RuntimeError):
    """A failed request/invalid response must never be interpreted as no data."""


@dataclass
class QAPage:
    rows: list[dict[str, Any]]
    complete: bool
    cursor: dict[str, Any]
    response_hash: str
    response_evidence: dict[str, Any] | None = None


def qa_source(symbol: str) -> tuple[str, str]:
    if symbol.startswith(("920", "4", "8")):
        return "P5W_PUBLIC", "全景网公开投资者关系披露"
    return ("SSE_EINTERACTION", "上证 e 互动") if symbol.startswith("6") else ("CNINFO", "巨潮资讯互动易")


def _source_time(value: Any) -> str | None:
    if value in (None, ""):
        return None
    if isinstance(value, (int, float)) or re.fullmatch(r"\d{13}", str(value)):
        stamp = float(value)
        if stamp > 10**11:
            stamp /= 1000
        return datetime.fromtimestamp(stamp, timezone.utc).astimezone(ZoneInfo("Asia/Shanghai")).strftime("%Y-%m-%d %H:%M:%S")
    text = re.sub(r"\s+", " ", str(value)).strip()
    text = text.replace("年", "-").replace("月", "-").replace("日", "")
    for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%d %H:%M", "%Y-%m-%d"):
        try:
            return datetime.strptime(text, fmt).strftime("%Y-%m-%d %H:%M:%S")
        except ValueError:
            continue
    raise QACollectionError(f"官方问答日期无法解析：{text[:80]}")


def _stamp(row: dict[str, Any], symbol: str, source_url: str) -> dict[str, Any]:
    source_code, source_name = qa_source(symbol)
    row.update({
        "symbol": symbol, "股票代码": symbol, "source_code": source_code,
        "source_name": source_name, "source_url": source_url, "detail_url": source_url,
        "status": "ANSWERED" if row.get("answer") else "PENDING_REPLY",
        "fetched_at": datetime.now(timezone.utc).isoformat(),
    })
    row["content_hash"] = sha256(json.dumps({k: v for k, v in row.items() if k != "fetched_at"}, ensure_ascii=False, sort_keys=True).encode("utf-8")).hexdigest()
    return row


def parse_sse_page(text: str, symbol: str, *, page: int, uid: str, url: str) -> QAPage:
    soup = BeautifulSoup(text, "html.parser")
    items = soup.select('.m_feed_item[id^="item-"]')
    # An empty body, login page, captcha or changed markup is not an EOF.
    if not items:
        empty = soup.select_one(".m_feed_note")
        if empty and re.search(r"无回复|暂无|无提问|没有|无相关", empty.get_text()):
            return QAPage([], True, {"page": page + 1, "uid": uid}, sha256(text.encode()).hexdigest())
        raise QACollectionError("上证 e 互动返回未知页面，未将其标记为无数据")
    rows = []
    for item in items:
        raw_html = str(item)
        question = next((node for node in item.select(".m_feed_detail") if "m_qa" not in node.get("class", [])), None)
        answer = item.select_one(".m_feed_detail.m_qa")
        body = question.select_one(".m_feed_txt") if question else None
        if body is None:
            raise QACollectionError("上证 e 互动问题正文缺失")
        company_link = body.find("a")
        if company_link:
            # Verify the provider's security identity before stripping its prefix.
            codes = re.findall(r"\b\d{6}\b", company_link.get_text())
            if codes and symbol not in codes:
                raise QACollectionError("上证 e 互动返回了其他股票的数据")
            company_link.decompose()
        answer_body = answer.select_one(".m_feed_txt") if answer else None
        def read(node: Any, selector: str) -> str:
            found = node.select_one(selector) if node else None
            return found.get_text(" ", strip=True) if found else ""
        asked = _source_time(read(question, ".m_feed_from span"))
        answered = _source_time(read(answer, ".m_feed_from span"))
        row = {
            "question_id": str(item["id"]).removeprefix("item-"),
            "question": body.get_text(" ", strip=True),
            "answer": answer_body.get_text(" ", strip=True) if answer_body else None,
            "questioner": read(question, ".m_feed_face p"),
            "answerer": read(answer, ".m_feed_face p"),
            "asked_at": asked, "answered_at": answered, "updated_at": answered or asked,
            "raw_html": raw_html,
        }
        if not row["question"]:
            raise QACollectionError("上证 e 互动问题为空")
        rows.append(_stamp(row, symbol, f"{url}#item-{row['question_id']}"))
    return QAPage(rows, False, {"page": page + 1, "uid": uid}, sha256(text.encode()).hexdigest())


class OfficialQAClient:
    def __init__(self, *, timeout: float = 10.0, attempts: int = 3):
        self.attempts = attempts
        self.client = httpx.Client(timeout=timeout, follow_redirects=True, headers={"User-Agent": "Mozilla/5.0"})

    def __enter__(self) -> OfficialQAClient:
        return self

    def __exit__(self, *args: Any) -> None:
        self.client.close()

    def _request(self, method: str, url: str, **kwargs: Any) -> httpx.Response:
        for attempt in range(self.attempts):
            try:
                response = self.client.request(method, url, **kwargs)
                if kwargs.get("follow_redirects") is False and response.is_redirect:
                    # Valid redirects commonly have no body. The caller must
                    # review Location before requesting the target.
                    return response
                response.raise_for_status()
                if not response.content.strip():
                    raise QACollectionError("官方问答接口返回空响应")
                return response
            except (httpx.HTTPError, QACollectionError) as exc:
                if attempt + 1 == self.attempts:
                    raise QACollectionError(f"问答请求失败：{url}；{exc}") from exc
                time.sleep(0.5 * 2**attempt)
        raise AssertionError("unreachable")

    def fetch_page(self, symbol: str, cursor: dict[str, Any] | None = None) -> QAPage:
        if not re.fullmatch(r"\d{6}", symbol):
            raise ValueError("问董秘只接受六位 A 股证券代码")
        cursor = dict(cursor or {})
        if symbol.startswith(("920", "4", "8")):
            from app.connectors.p5w_public import P5WClient
            with P5WClient() as public:
                return public.fetch_page(symbol, cursor)
        page = int(cursor.get("page") or 1)
        if symbol.startswith("6"):
            feed_type = int(cursor.get("feed_type") or 11)
            company_url = f"https://sns.sseinfo.com/company.do?stockcode={symbol}"
            uid = cursor.get("uid")
            if not uid:
                text = self._request("GET", company_url).text
                match = re.search(r"userfeeds\.do\?[^\"'\s]*typeCode=company[^\"'\s]*uid=(\d+)", text)
                if not match:
                    raise QACollectionError("上证 e 互动未返回公司身份标识")
                uid = match.group(1)
            response = self._request("GET", "https://sns.sseinfo.com/ajax/userfeeds.do", params={
                "typeCode": "company", "type": feed_type, "pageSize": 10, "uid": uid, "page": page,
            }, headers={"Referer": company_url, "X-Requested-With": "XMLHttpRequest"})
            result = parse_sse_page(response.text, symbol, page=page, uid=str(uid), url=str(response.url))
            result.cursor["feed_type"] = feed_type
            if result.complete and feed_type == 11:
                # Also collect the question stream: newly asked, unanswered
                # questions are absent from the latest-replies stream.
                result.complete = False
                result.cursor = {"page": 1, "uid": str(uid), "feed_type": 10}
            return result
        org_id = cursor.get("org_id")
        if not org_id:
            data = self._request("POST", "https://irm.cninfo.com.cn/newircs/index/queryKeyboardInfo", data={"keyWord": symbol}).json()
            candidates = data.get("data")
            candidates = candidates if isinstance(candidates, list) else []
            org_id = next((r.get("secid") for r in candidates if r.get("stockCode") == symbol), None)
            if not org_id:
                raise QACollectionError("巨潮互动易未返回匹配的公司身份，不能判定为无数据")
        response = self._request("POST", "https://irm.cninfo.com.cn/newircs/company/question", params={
            "stockcode": symbol, "orgId": org_id, "pageSize": 100, "pageNum": page,
            "keyWord": "", "startDay": "", "endDay": "",
        })
        data = response.json()
        if not isinstance(data.get("rows"), list) or "total" not in data:
            raise QACollectionError("巨潮互动易返回未知格式，未将其标记为无数据")
        rows = []
        for raw in data["rows"]:
            if str(raw.get("stockCode") or "") != symbol:
                raise QACollectionError("巨潮互动易返回了其他股票的数据")
            if not raw.get("indexId") or not raw.get("mainContent"):
                raise QACollectionError("巨潮互动易问题编号或正文缺失")
            answer = raw.get("attachedContent")
            answered = raw.get("attachedPubDate")
            if raw.get("attachedId") and not answer:
                detail = self._request("GET", "https://irm.cninfo.com.cn/newircs/question/getQuestionDetail", params={"questionId": raw["indexId"]}).json().get("data") or {}
                if detail.get("stockCode") and detail["stockCode"] != symbol:
                    raise QACollectionError("巨潮互动易问答详情证券身份不一致")
                answer, answered = detail.get("replyContent"), detail.get("replyDate")
                if not answer:
                    raise QACollectionError("巨潮互动易已回复问题的回答正文暂未获取")
            row = {
                "question_id": str(raw["indexId"]), "question": raw["mainContent"],
                "answer": answer, "questioner": raw.get("authorName"),
                "answerer": raw.get("attachedAuthor"), "asked_at": _source_time(raw.get("pubDate")),
                "answered_at": _source_time(answered), "updated_at": _source_time(raw.get("updateDate")),
                "raw_record": raw,
            }
            rows.append(_stamp(row, symbol, f"https://irm.cninfo.com.cn/ircs/question/questionDetail?questionId={raw['indexId']}"))
        total = int(data["total"])
        complete = page >= int(data.get("totalPage") or max(1, (total + 99) // 100))
        if not rows and not complete:
            raise QACollectionError("巨潮互动易分页提前返回空页，等待重试")
        return QAPage(rows, complete, {"page": page + 1, "org_id": org_id}, sha256(response.content).hexdigest())
