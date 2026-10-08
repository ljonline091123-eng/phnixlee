"""Public NEEQ F10 enrichment, with issuer checks and original source evidence."""
from __future__ import annotations

from datetime import datetime, timezone
import hashlib
import json
import re
from typing import Callable

from bs4 import BeautifulSoup
import httpx

BASE = "https://xinsanban.eastmoney.com"

SUMMARY_FIELDS = {
    "营业总收入": "TOTALOPERATEREVE", "营业总收入同比": "YSTZ",
    "归属净利润": "PARENTNETPROFIT", "归属净利润同比": "SJLTZ",
    "扣非净利润": "KCFJCXSYJLR", "扣非净利润同比": "KFJLTZ",
    "每股收益": "EPSJB", "毛利率": "XSMLL", "加权净资产收益率": "ROEJQ",
    "净资产": "NETASSET",
}


def parse_home_summary(html_text: str, symbol: str, url: str) -> dict:
    """A real server-rendered fallback, explicitly preserving rounded precision."""
    soup = BeautifulSoup(html_text, "html.parser")
    identity = soup.select_one(".stockinfo .code")
    if not identity or identity.get_text(strip=True).strip("()（）") != symbol:
        raise ValueError("财务摘要页面缺少匹配的证券身份")
    period_node = soup.select_one(".cwzbtip")
    period = period_node.get_text(strip=True) if period_node else ""
    if not re.fullmatch(r"\d{4}-\d{2}-\d{2}", period):
        raise ValueError("财务摘要页面缺少明确报告期，不能使用页面更新时间替代")
    rows, raw_values = [], {"REPORTDATE": period}
    for tr in soup.select(".cwzy tr"):
        cells = tr.find_all("td", recursive=False)
        for index in range(0, len(cells) - 1, 2):
            label = cells[index].get_text(strip=True)
            original = cells[index + 1].get_text(strip=True)
            value = number(original)
            key = SUMMARY_FIELDS.get(re.sub(r"[（(].*", "", label))
            if value is None or not key:
                continue
            yuan = "万元" in label
            unit = "元" if yuan else "%" if "%" in label else "元/股"
            normalized = round(value * 10000, 2) if yuan else value
            raw_values[key] = normalized
            rows.append({"metric": label.replace("万元", "元"), "metric_code": key,
                         "data": {period: normalized}, "unit": unit, "source": url,
                         "source_value": original, "source_unit": "万元" if yuan else unit,
                         "precision": "SOURCE_ROUNDED", "rounding_step": 100 if yuan else 0.01})
    if not rows:
        raise ValueError("财务摘要页面没有具体数值，不能标记采集完成")
    return {"source": "东方财富新三板公开F10财务摘要（页面舍入值）", "source_url": url,
            "rows": rows, "periods": [period], "report_date": period,
            "raw_values": raw_values, "precision": "SOURCE_ROUNDED",
            "message": "公开页面金额原单位为万元、保留两位小数；已换算为元，精度仍为100元。"}


def fetch_home_summary(symbol: str) -> dict:
    url = BASE + f"/F10/{symbol}.html"
    with httpx.Client(timeout=15, trust_env=False, follow_redirects=True) as client:
        response = client.get(url, headers={"User-Agent": "Mozilla/5.0"})
        response.raise_for_status()
    summary = parse_home_summary(response.text, symbol, url)
    summary["source_evidence"] = [{"url": url, "fetched_at": datetime.now(timezone.utc).isoformat(),
        "response_sha256": hashlib.sha256(response.content).hexdigest(), "html": response.text}]
    return summary


def fetch_quote_page_news(symbol: str) -> list[dict]:
    """Server-rendered company news; never use undated announcement snippets."""
    url = BASE + f"/QuoteCenter/{symbol}.html"
    with httpx.Client(timeout=15, trust_env=False, follow_redirects=True) as client:
        response = client.get(url, headers={"User-Agent": "Mozilla/5.0"})
        response.raise_for_status()
    soup = BeautifulSoup(response.text, "html.parser")
    identity = soup.select_one(".code")
    if not identity or identity.get_text(strip=True).strip("()（）") != symbol:
        raise ValueError("新闻备用页面缺少匹配的证券身份")
    evidence = {"url": url, "fetched_at": datetime.now(timezone.utc).isoformat(),
                "response_sha256": hashlib.sha256(response.content).hexdigest(), "html": response.text}
    rows = []
    for link in soup.select(".newsList a[href]"):
        text = link.get_text(strip=True)
        dated = re.search(r"[（(](\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2})[）)]$", text)
        if not dated:
            continue
        rows.append({"title": text[:dated.start()].strip(), "news_time": dated.group(1),
                     "url": link.get("href"), "source_method": "PUBLIC_QUOTE_HTML",
                     "source_evidence": {k: v for k, v in evidence.items() if k != "html"},
                     "collection_status": "PARTIAL", "collection_scope": "公开公司行情页展示的新闻摘要，非全量新闻"})
    if rows:
        rows[0]["original_source_response"] = evidence
    return rows


def number(value):
    if value in (None, "", "-", "--"):
        return None
    try:
        return float(str(value).replace(",", ""))
    except (TypeError, ValueError):
        return None


def financial_table(payload, source):
    records = payload.get("result") or []
    periods = sorted({str(r.get("REPORTDATE"))[:10] for r in records if r.get("REPORTDATE")}, reverse=True)
    rows = []
    for field in payload.get("Trs") or []:
        key, label = field.get("Field"), field.get("Name")
        # The provider uses subTitle for real numeric totals as well as
        # headings. Keep every field with numeric source values, including
        # assets, liabilities, equity, revenue and cash-flow totals.
        if not key or not label:
            continue
        values = {str(r["REPORTDATE"])[:10]: number(r.get(key)) for r in records
                  if r.get("REPORTDATE") and number(r.get(key)) is not None}
        if values:
            rows.append({"metric": label, "metric_code": key, "data": values,
                         "unit": "元" if "(元)" in label else "%" if "%" in label else "来源标注",
                         "source": source})
    return {"source": source, "periods": periods, "rows": rows,
            "report_date": periods[0] if periods else None}


def fetch_neeq_f10(symbol: str, fetch_json: Callable) -> dict:
    output, evidence, errors = {}, [], []
    fetched_at = datetime.now(timezone.utc).isoformat()

    def request(path, params):
        url = BASE + path
        try:
            payload = fetch_json(path, params)
            if not isinstance(payload, dict) or payload.get("IsSuccess") not in (True, 1) or not isinstance(payload.get("result"), list):
                raise ValueError("公开F10响应格式或成功标志异常")
            for row in payload["result"]:
                for identity in ("SECURITYCODE", "MSECUCODE"):
                    if row.get(identity) and str(row[identity]).split(".")[0] != symbol:
                        raise ValueError("公开F10证券代码与请求不符")
            digest = hashlib.sha256(json.dumps(payload, sort_keys=True, ensure_ascii=False).encode()).hexdigest()
            evidence.append({"url": url, "params": params, "fetched_at": fetched_at,
                             "payload_sha256": digest, "payload": payload})
            return payload
        except Exception as exc:
            errors.append({"url": url, "params": params, "error": str(exc)[:300], "status": "FAILED"})
            return {}

    profile_url = BASE + f"/F10/CompanyInfo/Introduction/{symbol}.html"
    try:
        with httpx.Client(timeout=15, trust_env=False, follow_redirects=True) as client:
            response = client.get(profile_url, headers={"User-Agent": "Mozilla/5.0"})
            response.raise_for_status()
        soup = BeautifulSoup(response.text, "html.parser")
        fields = {}
        for item in soup.select("#security_info li, #company_info li"):
            label, value = item.select_one(".company-page-item-left"), item.select_one(".company-page-item-right")
            if label and value:
                text = value.get_text(" ", strip=True)
                if text and text not in {"-", "--"}:
                    fields[label.get_text(strip=True)] = text
        if str(fields.get("证券代码", "")).split(".")[0] != symbol:
            raise ValueError("公司资料页缺少匹配的证券身份，不能当作空数据")
        fields.update({"公司名称": fields.get("公司全称"), "所属行业": fields.get("行业分类"),
                       "上市日期": fields.get("挂牌日期"), "所属市场": "全国股转系统",
                       "所属板块": fields.get("市场分层")})
        output["profile"] = {"source": "东方财富新三板公司资料", "source_url": profile_url,
                             "fields": fields, "as_of": fetched_at}
        evidence.append({"url": profile_url, "fetched_at": fetched_at,
                         "response_sha256": hashlib.sha256(response.content).hexdigest(), "html": response.text})
    except Exception as exc:
        errors.append({"url": profile_url, "error": str(exc)[:300], "status": "FAILED"})

    top = request("/api/F10/EquityShareholders/GetHoldTop10", {"code": symbol, "rank": 10})
    major = []
    for group in top.get("result") or []:
        for row in group.get("list") or []:
            if str(row.get("SECURITYCODE", "")).split(".")[0] != symbol:
                errors.append({"url": BASE + "/api/F10/EquityShareholders/GetHoldTop10",
                               "status": "FAILED", "error": "十大股东证券身份不符，已隔离该记录",
                               "rejected_row": row})
                continue
            if row.get("SHAREHDNAME") and number(row.get("SHAREHDNUM")) is not None:
                major.append({"股东名称": row["SHAREHDNAME"], "报告期": row.get("ENDDATE"),
                              "持股数量": number(row.get("SHAREHDNUM")), "持股比例": number(row.get("SHAREHDNUMPER")),
                              "变动数量": number(row.get("SHAREHDNUM_CHANGE")), "变动方向": row.get("DIRECTION"),
                              "公告日期": row.get("NOTICEDATE"), "source_row": row})
    count = request("/api/F10/EquityShareholders/GetRSHold", {"code": symbol, "sortRule": 1, "sortType": "ENDDATE"})
    capital = request("/api/EquityShareholders/GetESH", {"code": symbol, "rank": ""})
    fields = (output.get("profile") or {}).get("fields") or {}
    output["holders"] = {
        "source": "东方财富新三板股本股东", "major": major,
        "holder_count": [{"报告期": r.get("ENDDATE"), "股东户数": number(r.get("TOTALSH")),
                          "变动数量": number(r.get("DIFFER")), "变动比例": number(r.get("RATE")), "source_row": r}
                         for r in count.get("result") or [] if number(r.get("TOTALSH")) is not None],
        "capital_structure": [{"变动日期": r.get("CHANGEDATE"), "变动原因": r.get("CHANGEREASON"),
                               **{label: number(r.get(key)) * 10000 if number(r.get(key)) is not None else None
                                  for key, label in (("TOTALSHARE", "总股本"), ("ASHARE", "流通股本"), ("ASHARER", "限售股本"))},
                               "unit": "股", "source_row": r} for r in capital.get("result") or []],
        "control": [{"主体名称": fields["实际控制人"], "关系": "实际控制人", "verification_status": "SOURCE_REPORTED",
                     "source_url": profile_url}] if fields.get("实际控制人") else [],
        "circulating": [], "institutional": [],
        "message": "十大股东与股本来自公开F10；不得将总股东名单当作十大流通股东或机构专属名单。",
    }
    output["financial_statements"] = {"source": "东方财富新三板财务报表（元）"}
    for key, path in (("balance_sheet", "zcfzb"), ("income_statement", "lrb"), ("cash_flow", "xjllb")):
        # Match the actual finance page's request. Its statement endpoints do
        # not send rank; adding that unsupported parameter can return 403 for
        # recently listed issuers even while the public report is available.
        payload = request("/api/F10/Finance/" + path, {"MSECUCODE": symbol, "dateType": 0})
        output["financial_statements"][key] = financial_table(payload, BASE + "/api/F10/Finance/" + path)
    summary = request("/api/F10/Finance/Cwzy", {"MSECUCODE": symbol, "dateType": 0, "rank": 10})
    output["financial_summary"] = financial_table(summary, "东方财富新三板财务摘要（元）")
    if not output["financial_summary"].get("rows"):
        try:
            fallback = fetch_home_summary(symbol)
            evidence.extend(fallback.pop("source_evidence"))
            output["financial_summary"] = fallback
        except Exception as exc:
            errors.append({"url": BASE + f"/F10/{symbol}.html", "error": str(exc)[:300], "status": "FAILED"})
    composition = []
    for category in ("行业", "产品", "地区"):
        payload = request("/api/F10/CompanyInfo/GetMainBusiness", {"code": symbol, "type": category})
        rows = payload.get("result") or []
        dates = sorted({str(r.get("REPORTDATE")) for r in rows if r.get("REPORTDATE")}, reverse=True)
        if dates:
            composition.append({"category": category, "report_date": dates[0], "items": [
                {"name": r.get("ITEMNAMEDEC") or r.get("ITEMNAME"), "营业收入": number(r.get("MBREVENUE")) * 10000
                 if number(r.get("MBREVENUE")) is not None else None,
                 "营业利润": number(r.get("MBPROFIT")) * 10000 if number(r.get("MBPROFIT")) is not None else None,
                 "毛利率": number(r.get("DEC_MAOLILV")), "收入占比": number(r.get("DEC_SHOURUGOUCHENG")),
                 "unit": "元", "source_row": r} for r in rows if str(r.get("REPORTDATE")) == dates[0]]})
    output["business_composition"] = {"source": "东方财富新三板主营构成", "sections": composition,
                                       "report_date": max((r["report_date"] for r in composition), default=None)}
    dividend_rows, seen_pages = [], set()
    for page in range(1, 21):
        params = {"MSECUCODE": symbol, "endDate": datetime.now().date().isoformat(),
                  "reportType": "", "page": page, "pageSize": 100,
                  "sortType": "PLANNOTICEDATE", "sortRule": -1}
        dividends = request("/api/F10/f10fxfp/fhpx", params)
        rows = dividends.get("result") or []
        signature = json.dumps(rows, sort_keys=True, ensure_ascii=False)
        if rows and signature in seen_pages:
            errors.append({"url": BASE + "/api/F10/f10fxfp/fhpx", "params": params,
                           "status": "PARTIAL", "error": "分红接口重复返回上一页，不能确认历史完整"})
            break
        seen_pages.add(signature)
        dividend_rows.extend(rows)
        if len(rows) < 100:
            break
    else:
        errors.append({"url": BASE + "/api/F10/f10fxfp/fhpx", "status": "PARTIAL",
                       "error": "分红历史达到20页采集上限，需要继续分页，不能声明全量"})
    if "profile" in output:
        output["profile"]["dividends"] = [{"公告日期": r.get("PLANNOTICEDATE"), "方案": r.get("ASSIGNDSCRPT"),
            "方案进度": r.get("STR_ASSIGNFEATURE"), "股权登记日": r.get("RIGHTREGDATE"),
            "除权日": r.get("EXDIVIDENDDATE"), "source_row": r} for r in dividend_rows]
        output["profile"]["dividend_source"] = "东方财富新三板分红派息"
    paths = {"profile": ("Introduction/", "/fhpx"),
             "holders": ("EquityShareholders",), "financial_statements": ("/zcfzb", "/lrb", "/xjllb"),
             "financial_summary": ("/Cwzy", f"/F10/{symbol}.html"), "business_composition": ("/GetMainBusiness",)}
    for section, value in output.items():
        relevant = paths.get(section, ())
        value["source_evidence"] = [{k: v for k, v in item.items() if k not in {"payload", "html"}}
                                    for item in evidence if any(p in item["url"] for p in relevant)]
        value["source_errors"] = [item for item in errors if any(p in item["url"] for p in relevant)]
        value["collected_at"] = fetched_at
        if value.get("report_date"):
            value["as_of"] = value["report_date"]
        elif section == "holders":
            periods = [str(r.get("报告期") or r.get("变动日期"))[:10]
                       for key in ("major", "holder_count", "capital_structure")
                       for r in value.get(key) or [] if r.get("报告期") or r.get("变动日期")]
            value["as_of"] = max(periods, default=None)
        else:
            value["as_of"] = None
        value["collection_status"] = "PARTIAL" if value["source_errors"] else "SOURCE_RESPONDED"
    # Preserve original source payloads once; the other sections carry hashes
    # referring to this inventory, rather than repeating HTML in every cache.
    output.setdefault("profile", {"fields": {}})["original_source_responses"] = evidence
    return output
