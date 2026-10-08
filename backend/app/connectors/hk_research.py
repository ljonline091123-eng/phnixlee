"""Security-scoped public Hong Kong research, with explicit source limitations."""
from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
from hashlib import sha256
import re
from typing import Any
from urllib.parse import urljoin, urlparse

from bs4 import BeautifulSoup
import httpx

from app.connectors.base import MarketDataAdapter

ETNET = "https://www.etnet.com.hk"
AASTOCKS = "https://www.aastocks.com"
SUMMARY_NOTICE = "AASTOCKS 公开大行报告摘要，非券商原始研报全文。"


def hk_symbol(symbol: str) -> str:
    text = str(symbol).strip().upper().removesuffix(".HK")
    if not re.fullmatch(r"\d{1,5}", text) or int(text) == 0:
        raise ValueError("港股代码须为 1 至 5 位数字")
    return text.zfill(5)


def _fetch_html(url: str) -> str:
    # Each provider has its own bounded request/retry. A failure never aborts
    # the other provider, and this client is only used outside the local read.
    with httpx.Client(timeout=httpx.Timeout(12, connect=6), trust_env=False,
                      follow_redirects=True, headers={"User-Agent": "Mozilla/5.0"}) as client:
        for attempt in range(2):
            try:
                response = client.get(url)
                response.raise_for_status()
                return response.content.decode("utf-8", errors="replace")
            except (httpx.TransportError, httpx.HTTPStatusError) as exc:
                retryable = not isinstance(exc, httpx.HTTPStatusError) or exc.response.status_code in (429, 500, 502, 503, 504)
                if attempt or not retryable:
                    raise
    raise RuntimeError("港股研究来源未返回响应")


def _text(node: Any) -> str:
    return re.sub(r"\s+", " ", node.get_text(" ", strip=True)).strip() if node else ""


def _number(value: Any) -> float | None:
    text = str(value or "").replace(",", "").strip()
    if text in {"", "--", "-", "N/A", "nan"}:
        return None
    match = re.fullmatch(r"\(?([-+]?\d+(?:\.\d+)?)\)?", text)
    return -abs(float(match[1])) if match and text.startswith("(") else float(match[1]) if match else None


def _date(value: str) -> str | None:
    for pattern in ("%d/%m/%Y", "%Y/%m/%d", "%Y-%m-%d"):
        try:
            return datetime.strptime(value[:10], pattern).date().isoformat()
        except ValueError:
            continue
    return None


def _provenance(source: str, name: str, url: str, html: str) -> dict[str, Any]:
    return {"source_code": source, "source_name": name, "source_url": url,
            "fetched_at": datetime.now(timezone.utc).isoformat(),
            "source_content_hash": sha256(html.encode("utf-8")).hexdigest()}


def parse_etnet_forecast(html: str, symbol: str) -> dict[str, Any]:
    symbol = hk_symbol(symbol)
    url = f"{ETNET}/www/sc/stocks/realtime/quote_profit.php?code={symbol}"
    soup = BeautifulSoup(html, "html.parser")
    title = _text(soup.title)
    if title and not re.search(rf"(?<!\d){symbol}(?!\d)", title):
        raise ValueError("经济通页面股票代码与请求不一致")
    provenance = _provenance("ETNET_HK", "经济通港股盈利预测", url, html)
    common = {**provenance, "market": "HK", "symbol": symbol}
    consensus: list[dict[str, Any]] = []
    institutions: dict[tuple[str, str], dict[str, Any]] = {}
    relevant_tables = []
    rating: dict[str, Any] = {}
    for table in soup.find_all("table"):
        # Target-price headers span the value and an adjacent direction icon.
        # Expand colspan so the update-date column stays aligned with the data.
        rows = []
        for tr in table.find_all("tr"):
            cells = []
            for cell in tr.find_all(["td", "th"], recursive=False):
                cells.append(_text(cell))
                cells.extend([""] * (int(cell.get("colspan", 1)) - 1))
            rows.append(cells)
        rows = [row for row in rows if row]
        if not rows:
            continue
        header = rows[0]
        if "平均评级" in " ".join(header):
            text = " ".join(header)
            counts = []
            for label, index in (("强烈买入", 1), ("买入", 2), ("持有", 3), ("沽出", 4), ("立即沽出", 5)):
                match = re.search(rf"{label}\({index}\)\s*(\d+)", text)
                counts.append(int(match[1]) if match else None)
            if all(value is not None for value in counts):
                average = re.search(r"平均评级\s*([\d.]+)", text)
                rating = {**common, "buy": counts[0] + counts[1], "hold": counts[2],
                          "add": 0, "neutral": 0, "reduce": 0, "sell": counts[3] + counts[4],
                          "total": sum(counts), "average_rating": float(average[1]) if average else None,
                          "raw_counts": dict(zip(("strong_buy", "buy", "hold", "sell", "strong_sell"), counts)),
                          "reference_period": "CURRENT_SNAPSHOT", "reference_basis": "经济通当前评级快照（未提供滚动时间窗）",
                          "as_of": common["fetched_at"], "calculation_method": "PROVIDER_NATIVE_SNAPSHOT"}
                relevant_tables.append(str(table))
            continue
        if not any("财政年度" in cell for cell in header) or not any("纯利" in cell for cell in header):
            continue
        relevant_tables.append(str(table))
        broker_index = next((i for i, cell in enumerate(header) if "证券商" in cell), None)
        currency = next((code for text, code in (("人民币", "CNY"), ("港元", "HKD"), ("美元", "USD"), ("欧元", "EUR")) if text in header[1]), None)
        units = re.findall(r"[（(]([^()（）]+)[）)]", header[1])
        profit_unit = units[-1] if units else header[1]
        # The EPS column says 分, not 港仙. Do not silently label it as HKD
        # or convert it to A-share 元/股. Retain the provider's exact unit.
        eps_unit = "分/股"
        previous_broker = previous_date = ""
        for cells in rows[1:]:
            if len(cells) < len(header) or not re.fullmatch(r"20\d{2}", cells[0]):
                continue
            year = cells[0]
            profit, eps = _number(cells[1]), _number(cells[2])
            raw = {f"{i}:{name}": cells[i] for i, name in enumerate(header)}
            if broker_index is None:
                highest = next((_number(cells[i]) for i, name in enumerate(header) if "最高" in name), None)
                lowest = next((_number(cells[i]) for i, name in enumerate(header) if "最低" in name), None)
                for code, name, value, unit in (("NET_PROFIT", "纯利/亏损", profit, profit_unit), ("EPS", "每股盈利", eps, eps_unit)):
                    if value is not None:
                        consensus.append({**common, "forecast_year": year, "metric_code": code,
                                          "metric": name, "mean": value, "unit": unit, "currency": currency,
                                          "minimum": lowest if code == "NET_PROFIT" else None,
                                          "maximum": highest if code == "NET_PROFIT" else None,
                                          "value_scope": "CONSENSUS", "raw_fields": raw})
                continue
            broker = cells[broker_index] if cells[broker_index] not in {"", "--"} else previous_broker
            updated_index = next((i for i, name in enumerate(header) if "更新日期" in name), None)
            updated = (_date(cells[updated_index]) if updated_index is not None else None) or previous_date
            if not broker or not updated or (profit is None and eps is None):
                continue
            previous_broker, previous_date = broker, updated
            item = institutions.setdefault((broker, updated), {**common, "institution": broker,
                "report_date": updated, "source_updated_at": updated, "record_type": "INSTITUTION_FORECAST",
                "extraction_method": "SOURCE_TABLE", "forecast": {"eps": {}, "net_profit": {}},
                "forecast_units": {"eps": eps_unit, "net_profit": profit_unit}, "currency": currency,
                "raw_rows": []})
            if profit is not None:
                item["forecast"]["net_profit"][year] = profit
            if eps is not None:
                item["forecast"]["eps"][year] = eps
            rating_index = next((i for i, name in enumerate(header) if name == "评级"), None)
            if rating_index is not None and cells[rating_index] not in {"", "--"}:
                item["rating"] = cells[rating_index]
            target_index = next((i for i, name in enumerate(header) if "目标价" in name), None)
            if target_index is not None and _number(cells[target_index]) is not None:
                item.update(target_price=_number(cells[target_index]), target_price_currency="HKD")
            item["raw_rows"].append(raw)
    # Explicit empty pages are valid; a redesigned non-empty table is not.
    if not relevant_tables and "暂无" not in _text(soup) and "没有" not in _text(soup):
        raise ValueError("经济通盈利预测页面结构发生变化，未识别有效表格")
    return {"earnings_forecast": consensus, "institution_forecast": list(institutions.values()),
            "provider_rating_statistics": rating, "source_metadata": {**provenance, "raw_html": "\n".join(relevant_tables)}}


def _report_fields(title: str, summary: str) -> dict[str, Any]:
    clean = re.sub(r"^《[^》]+》\s*", "", title)
    institution = next((name for name in ("招商证券国际", "招商证券", "美银证券", "浦银国际", "海通国际", "国泰君安国际",
                       "国泰君安", "中信证券", "摩根大通", "高盛", "野村", "中金", "瑞银", "大摩", "花旗", "摩通")
                        if clean.startswith(name)), None)
    if not institution:
        match = re.match(r"(.{2,20}?)(?:首予|首次|发表|维持|上调|下调|予|料|：|:)", clean)
        institution = match[1] if match else None
    match = re.search(r"[「『“\"]([^」』”\"]{1,16})[」』”\"]\s*评级", title)
    if not match:
        match = re.search(r"[「『“\"]([^」』”\"]{1,16})[」』”\"]\s*评级", summary)
    raw_rating = match[1] if match else None
    normalized = {"沽售": "卖出", "沽出": "卖出", "跑赢大市": "增持", "跑赢行业": "增持",
                  "优于大市": "增持", "跑输大市": "减持", "逊于大市": "减持"}.get(raw_rating, raw_rating)
    return {"institution": institution, "rating_raw": raw_rating, "rating": normalized,
            "rating_normalized": normalized}


def _report_time(node: Any) -> str | None:
    match = re.search(r"dt:\s*['\"](20\d{2}/\d{2}/\d{2}\s+\d{2}:\d{2})", str(node))
    return match[1].replace("/", "-") if match else None


def parse_aastocks_reports(html: str, symbol: str) -> list[dict[str, Any]]:
    symbol = hk_symbol(symbol)
    soup = BeautifulSoup(html, "html.parser")
    url = f"{AASTOCKS}/sc/stocks/analysis/stock-aafn/{symbol}/0/research-report/1"
    common = _provenance("AASTOCKS_HK", "AASTOCKS 港股大行报告摘要", url, html)
    reports = {}
    for anchor in soup.find_all("a", href=True):
        path = urlparse(urljoin(AASTOCKS, anchor["href"])).path
        match = re.fullmatch(rf"/sc/stocks/analysis/stock-aafn-con/{symbol}/[^/]+/([^/]+)/research-report", path)
        title = _text(anchor)
        if not match or not title:
            continue
        container = anchor.find_parent("div", attrs={"ref": match[1]})
        if container is None:
            continue
        report_time = _report_time(container)
        if report_time is None:
            raise ValueError("AASTOCKS 研报缺少可核验日期")
        summary = _text(container.select_one(".newscontent4"))
        reports[match[1]] = {**common, **_report_fields(title, summary), "market": "HK", "symbol": symbol,
            "external_id": match[1], "title": title, "report_date": report_time[:10], "report_time": report_time,
            "source_updated_at": report_time, "summary": summary, "detail_url": urljoin(AASTOCKS, path),
            "content_status": "PENDING", "content_kind": "PUBLIC_BROKER_SUMMARY", "content_notice": SUMMARY_NOTICE,
            "raw_html": str(container)}
    if not reports and soup.select(".newshead4"):
        raise ValueError("AASTOCKS 返回了其他证券内容或页面结构已变更")
    if not reports and not any(word in _text(soup) for word in ("没有", "暂无", "無相關", "无相关", "No related")):
        raise ValueError("AASTOCKS 未返回可识别的研报列表或明确空状态")
    return sorted(reports.values(), key=lambda row: row["report_time"], reverse=True)


def fetch_aastocks_report_detail(external_id: str, detail_url: str, symbol: str) -> dict[str, Any]:
    symbol = hk_symbol(symbol)
    parsed = urlparse(detail_url)
    expected = rf"/sc/stocks/analysis/stock-aafn-con/{symbol}/[^/]+/{re.escape(external_id)}/research-report"
    if parsed.scheme != "https" or parsed.netloc != "www.aastocks.com" or not re.fullmatch(expected, parsed.path):
        raise ValueError("港股研报详情地址与来源、证券代码或报告编号不一致")
    html = _fetch_html(detail_url)
    soup = BeautifulSoup(html, "html.parser")
    body = soup.select_one(".newscontent5")
    title = _text(soup.select_one(".newshead5"))
    if not body or not title:
        raise ValueError("AASTOCKS 未返回公开报告摘要正文")
    for node in body.select(".jsStock, .jsSS, .jssc, .newsRelatedHeadline, .urlfooter, script, style"):
        node.decompose()
    content = body.get_text("\n", strip=True)
    return {**_provenance("AASTOCKS_HK", "AASTOCKS 港股大行报告摘要", detail_url, html),
            **_report_fields(title, content), "external_id": external_id, "symbol": symbol, "market": "HK",
            "title": title, "content": content, "content_hash": sha256(content.encode("utf-8")).hexdigest(),
            "content_status": "READY", "content_kind": "PUBLIC_BROKER_SUMMARY", "content_notice": SUMMARY_NOTICE,
            "detail_url": detail_url, "report_date": (_report_time(soup) or "")[:10]}


def fetch_hk_research_sections(symbol: str, enabled_sources: set[str] | None = None) -> dict[str, Any]:
    symbol = hk_symbol(symbol)
    enabled = {"ETNET_HK", "AASTOCKS_HK"} if enabled_sources is None else enabled_sources
    result: dict[str, Any] = {"source": "港股公开研究：经济通 / AASTOCKS", "market": "HK", "symbol": symbol,
        "reports": [], "earnings_forecast": [], "institution_forecast": [], "qa": [],
        "qa_message": "港股未接入与 A 股问董秘相同的公开问答接口。", "qa_status": "UNAVAILABLE", "source_metadata": [],
        "report_source": "AASTOCKS 港股大行报告摘要", "earnings_forecast_source": "经济通港股盈利预测",
        "institution_forecast_source": "经济通港股机构预测", "fetched_at": datetime.now(timezone.utc).isoformat(),
        "message": "公开来源仅覆盖部分机构；报告内容为公开摘要，缺失指标保留为空。"}
    def etnet() -> dict[str, Any]:
        return parse_etnet_forecast(_fetch_html(f"{ETNET}/www/sc/stocks/realtime/quote_profit.php?code={symbol}"), symbol)
    def aastocks() -> dict[str, Any]:
        url = f"{AASTOCKS}/sc/stocks/analysis/stock-aafn/{symbol}/0/research-report/1"
        html = _fetch_html(url)
        return {"reports": parse_aastocks_reports(html, symbol),
                "source_metadata": _provenance("AASTOCKS_HK", "AASTOCKS 港股大行报告摘要", url, html)}
    with ThreadPoolExecutor(max_workers=2) as executor:
        jobs = {code: executor.submit(fetch) for code, fetch in (("ETNET_HK", etnet), ("AASTOCKS_HK", aastocks)) if code in enabled}
        for code, job in jobs.items():
            try:
                data = job.result()
                metadata = data.pop("source_metadata", {})
                available = any(data.get(key) for key in ("reports", "earnings_forecast", "institution_forecast"))
                result.update(data)
                result["source_metadata"].append({**metadata, "source_code": code, "status": "AVAILABLE" if available else "MISSING"})
            except Exception as exc:
                result["source_metadata"].append({"source_code": code, "status": "FAILED", "error": str(exc)[:500]})
    for code in {"ETNET_HK", "AASTOCKS_HK"} - enabled:
        result["source_metadata"].append({"source_code": code, "status": "DISABLED", "error": "数据源或对应接口已停用"})
    result["status"] = "PARTIAL" if any(result[key] for key in ("reports", "earnings_forecast", "institution_forecast")) else "MISSING"
    return result


class EtNetResearchAdapter(MarketDataAdapter):
    adapter_type = "ETNET_HK"

    def health_check(self) -> tuple[str, list[str]]:
        data = fetch_hk_research_sections("02533", {self.adapter_type})
        if any(item["status"] == "FAILED" for item in data["source_metadata"]):
            raise ValueError(str(data["source_metadata"]))
        return "经济通港股公开研究接口连接成功", ["FORECAST", "RATING"]

    def fetch_symbol_master(self, market: str) -> list:
        raise NotImplementedError("该来源只提供港股研究数据，不提供证券名单")

    def fetch_extended_data(self, market: str, symbol: str) -> dict[str, Any]:
        if market != "HK":
            raise ValueError("港股研究来源不支持其他市场")
        return {"research_sections": fetch_hk_research_sections(symbol, {self.adapter_type})}


class AastocksResearchAdapter(EtNetResearchAdapter):
    adapter_type = "AASTOCKS_HK"

    def health_check(self) -> tuple[str, list[str]]:
        super().health_check()
        return "AASTOCKS 港股公开大行报告摘要接口连接成功", ["RESEARCH"]
