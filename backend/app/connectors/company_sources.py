"""Read-only public company disclosures with explicit provenance and coverage.

This module does not write business tables or infer approved graph facts.
Optional caches preserve downloaded responses for reproducible ingestion.
"""

from __future__ import annotations

import hashlib
import html
import io
import json
import math
import os
import re
from dataclasses import asdict, dataclass, field
from datetime import date, datetime, timezone
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Any, Mapping
from urllib.parse import urljoin, urlparse
from zoneinfo import ZoneInfo

import httpx


PROFILE_URL = "https://datacenter.eastmoney.com/securities/api/data/v1/get"
HOLDERS_BASE = "https://emweb.securities.eastmoney.com/PC_HSF10/ShareholderResearch/"
CNINFO_SEARCH = "https://www.cninfo.com.cn/new/information/topSearch/query"
CNINFO_NOTICES = "https://www.cninfo.com.cn/new/hisAnnouncement/query"
CNINFO_STATIC = "https://static.cninfo.com.cn/"
TENCENT_QUOTE = "https://qt.gtimg.cn/"
MAX_PDF_BYTES = 20 * 1024 * 1024
MAX_PDF_PAGES = 100
MAX_TEXT_CHARACTERS = 2000000


@dataclass
class SourceResult:
    source_code: str
    source_name: str
    source_kind: str
    source_url: str
    retrieved_at: str
    status: str = "SUCCESS"
    published_at: str | None = None
    request: dict[str, Any] = field(default_factory=dict)
    raw: dict[str, Any] = field(default_factory=dict)
    records: list[dict[str, Any]] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)


def _now() -> str:
    return datetime.now(timezone.utc).isoformat()


def _text(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text if text and text not in {"--", "-", "无", "null", "None"} else None


def _number(value: Any) -> float | None:
    if isinstance(value, bool):
        return None
    try:
        number = float(str(value).replace(",", "").removesuffix("%"))
    except (TypeError, ValueError):
        return None
    return number if math.isfinite(number) else None


def _date(value: Any) -> str | None:
    if value is None:
        return None
    try:
        return date.fromisoformat(str(value)[:10]).isoformat()
    except ValueError:
        return None


def valid_cn_uscc(value: str | None) -> bool:
    """GB 32100 checksum; passing it does not verify the registration itself."""
    alphabet = "0123456789ABCDEFGHJKLMNPQRTUWXY"
    weights = (1, 3, 9, 27, 19, 26, 16, 17, 20, 29, 25, 13, 8, 24, 10, 30, 28)
    if not value or len(value) != 18 or any(char not in alphabet for char in value):
        return False
    expected = (31 - sum(alphabet.index(char) * weight for char, weight in zip(value[:17], weights)) % 31) % 31
    return value[-1] == alphabet[expected]


def _security_code(market: str, symbol: str) -> tuple[str, str]:
    market = market.upper().strip()
    symbol = symbol.strip()
    if market == "CN_A" and re.fullmatch(r"\d{6}", symbol):
        exchange = "SH" if symbol.startswith("6") else "BJ" if symbol.startswith(("4", "8", "92")) else "SZ"
        return symbol, f"{symbol}.{exchange}"
    if market == "HK" and re.fullmatch(r"\d{1,5}", symbol):
        return symbol.zfill(5), f"{symbol.zfill(5)}.HK"
    raise ValueError("company sources support CN_A six-digit and HK five-digit securities")


def _jurisdiction(raw: dict[str, Any], market: str) -> str:
    if market == "CN_A":
        return "CN"
    registration = _text(raw.get("REG_PLACE"))
    return {"中国": "CN", "中国内地": "CN", "中国大陆": "CN", "香港": "HK", "中国香港": "HK",
            "开曼群岛": "KY", "百慕大": "BM", "英属维尔京群岛": "VG", "新加坡": "SG"}.get(registration, registration or "UNKNOWN")


def parse_company_profile(payload: dict[str, Any], market: str, symbol: str, retrieved_at: str) -> list[dict[str, Any]]:
    """Normalize explicit provider fields; never merge names or infer UBOs."""
    symbol, secucode = _security_code(market, symbol)
    rows = (payload.get("result") or {}).get("data") or []
    records = []
    for raw in rows:
        if not isinstance(raw, dict) or raw.get("SECUCODE") != secucode:
            continue
        name, issuer_id = _text(raw.get("ORG_NAME")), _text(raw.get("ORG_CODE"))
        if not name or not issuer_id:
            continue
        registration = _text(raw.get("REG_NUM"))
        registration = registration.upper() if registration else None
        profile = {
            "registered_name": name, "english_name": _text(raw.get("ORG_NAME_EN") or raw.get("ORG_EN_ABBR")),
            "registration_number_raw": registration, "registration_verified": False,
            "registration_source_kind": "AGGREGATOR", "legal_representative": _text(raw.get("LEGAL_PERSON")),
            "incorporated_on": _date(raw.get("FOUND_DATE")), "registered_address": _text(raw.get("REG_ADDRESS")),
            "business_scope": _text(raw.get("BUSINESS_SCOPE")), "main_business": _text(raw.get("MAIN_BUSINESS")),
            "registered_capital_raw": raw.get("REG_CAPITAL"), "registered_capital_unit": "UNCONFIRMED_SOURCE_UNIT",
            "company_description": _text(raw.get("ORG_PROFIE") or raw.get("ORG_PROFILE")),
            "website": _text(raw.get("ORG_WEB")), "registration_place": _text(raw.get("REG_PLACE")),
            "a_share_code": _text(raw.get("STR_CODEA")), "h_share_code": _text(raw.get("STR_CODEH")),
            "isin": _text(raw.get("ISIN_CODE")), "latest_reporting_date": _date(raw.get("MAX_DATE")),
            "source_issuer_id": issuer_id, "source_namespace": "EASTMONEY",
        }
        company = {"name": name, "jurisdiction": _jurisdiction(raw, market),
                   "source_issuer_id": issuer_id, "properties_json": profile}
        if valid_cn_uscc(registration):
            company.update(identifier_scheme="CN_USCC", identifier_value=registration)
            profile["registration_checksum_valid"] = True
        else:
            profile["registration_checksum_valid"] = False if registration else None
        industries = []
        for level in (1, 2, 3):
            code, label = _text(raw.get(f"BOARD_CODE_BK_{level}LEVEL")), _text(raw.get(f"BOARD_NAME_{level}LEVEL"))
            if code and label:
                industries.append({"source_issuer_id": f"industry:EASTMONEY:{code}", "code": code,
                    "name": label, "taxonomy": "EASTMONEY_INDUSTRY", "taxonomy_version": "PROVIDER_CURRENT_UNVERSIONED", "level": level})
        if _text(raw.get("CSRC_INDUSTRY_NAME") or raw.get("BELONG_INDUSTRY")):
            profile["industry_label_raw"] = _text(raw.get("CSRC_INDUSTRY_NAME") or raw.get("BELONG_INDUSTRY"))
        themes = []
        labels = str(raw.get("BLGAINIAN") or "").split(",")
        codes = str(raw.get("BLGAINIAN_CODE") or "").split(",")
        if len(labels) == len(codes):
            for code, label in zip(codes, labels):
                if code.strip() and label.strip():
                    themes.append({"source_issuer_id": f"theme:EASTMONEY:{code.strip()}", "code": code.strip(),
                                   "name": label.strip(), "taxonomy": "EASTMONEY_THEME", "taxonomy_version": "PROVIDER_CURRENT_UNVERSIONED"})
        controllers = []
        for prefix, name_key, code_key in (("CONTROL", "CONTROL_HOLDER", "CONTROL_HOLDER_CODE"),
                                           ("REAL", "REAL_CONTROLER", "REAL_CONTROLER_CODE")):
            holder_name, holder_code = _text(raw.get(name_key)), _text(raw.get(code_key))
            if holder_name:
                controllers.append({"name": holder_name, "source_issuer_id": holder_code if prefix == "CONTROL" else None,
                    "source_entity_code": holder_code, "entity_type": "UNRESOLVED",
                    "mention_type": "CONTROLLING_HOLDER" if prefix == "CONTROL" else "ACTUAL_CONTROLLER",
                    "direct_ratio": _number(raw.get(prefix + "_DIRECT_RATIO")),
                    "indirect_ratio": _number(raw.get(prefix + "_INDIRECT_RATIO")),
                    "ratio_basis": "PROVIDER_REPORTED_ISSUER_TOTAL_SHARES", "verification_status": "PENDING"})
        records.append({"market": market, "symbol": symbol, "company": company, "profile": profile,
            "industries": industries, "themes": themes, "controller_mentions": controllers, "source_record": raw,
            "evidence": {"source_key": f"company:{issuer_id}:{market}:{symbol}", "title": f"{name} company profile ({secucode})",
                "content": json.dumps(raw, ensure_ascii=False, sort_keys=True, indent=2),
                "published_at": None, "available_at": retrieved_at, "content_scope": "PROVIDER_STRUCTURED_RECORD"}})
    return records


def parse_shareholders(payload: dict[str, Any], market: str, symbol: str) -> list[dict[str, Any]]:
    """Preserve holder-account names. A register holder is not necessarily a UBO."""
    symbol, secucode = _security_code(market, symbol)
    records = []
    for row in payload.get("sdgd") or []:
        if not isinstance(row, dict) or row.get("SECUCODE") != secucode or not _text(row.get("HOLDER_NAME")):
            continue
        ratio, shares = _number(row.get("HOLD_NUM_RATIO")), _number(row.get("HOLD_NUM"))
        if ratio is not None and not 0 <= ratio <= 100:
            ratio = None
        if shares is not None and shares < 0:
            shares = None
        records.append({"market": market, "symbol": symbol, "holder_name": str(row["HOLDER_NAME"]).strip(),
            "holder_rank": row.get("HOLDER_RANK"), "report_date": _date(row.get("END_DATE")),
            "published_at": None, "shares": shares, "shares_unit": "SHARES", "ratio": ratio,
            "ratio_basis": "TOTAL_ISSUED_SHARES", "share_class": _text(row.get("SHARES_TYPE")),
            "identity_status": "UNRESOLVED_REGISTER_HOLDER", "beneficial_owner_verified": False,
            "source_record": row})
    return records


def parse_cninfo_announcements(payload: dict[str, Any], symbol: str) -> list[dict[str, Any]]:
    records, seen = [], set()
    for row in payload.get("announcements") or []:
        if not isinstance(row, dict) or str(row.get("secCode") or "").zfill(6) != symbol:
            continue
        record_id = str(row.get("announcementId") or "")
        title = html.unescape(re.sub(r"<[^>]+>", "", str(row.get("announcementTitle") or ""))).strip()
        if not record_id or record_id in seen or not title:
            continue
        try:
            published = datetime.fromtimestamp(float(row.get("announcementTime")) / 1000, timezone.utc).isoformat()
        except (TypeError, ValueError, OverflowError, OSError):
            published = None
        document_url = urljoin(CNINFO_STATIC, str(row.get("adjunctUrl") or ""))
        parsed = urlparse(document_url)
        if parsed.scheme != "https" or parsed.hostname != "static.cninfo.com.cn" or not parsed.path.lower().endswith(".pdf"):
            document_url = None
        records.append({"market": "CN_A", "symbol": symbol, "announcement_id": record_id, "title": title,
            "published_at": published, "url": document_url, "source_record": row})
        seen.add(record_id)
    return records


def parse_market_cap(text: str, market: str, symbol: str) -> list[dict[str, Any]]:
    """Tencent A-share total cap is quoted in CNY 100m, not a listing-only cap."""
    symbol, secucode = _security_code(market, symbol)
    if market != "CN_A":
        return []
    code = secucode[-2:].lower() + symbol
    match = re.search(r'v_' + re.escape(code) + r'="([^"\r\n]*)"', text)
    if not match:
        return []
    fields = match.group(1).split("~")
    if len(fields) <= 45 or fields[2] != symbol:
        return []
    try:
        cap = Decimal(fields[45])
        current_price = Decimal(fields[3])
        snapshot = datetime.strptime(fields[30], "%Y%m%d%H%M%S").replace(tzinfo=ZoneInfo("Asia/Shanghai"))
    except (ValueError, InvalidOperation):
        return []
    if not cap.is_finite() or cap <= 0 or not current_price.is_finite() or current_price <= 0:
        return []
    if snapshot > datetime.now(timezone.utc):
        return []
    return [{"market": market, "symbol": symbol, "name": fields[1], "current_price": float(current_price),
             "total_market_cap": float(cap * Decimal(100000000)), "source_value_100m": str(cap),
             "currency": "CNY", "unit": "CNY", "source_unit": "CNY_100M",
             "valuation_scope": "PROVIDER_ISSUER_TOTAL_SHARES_AT_A_SHARE_PRICE",
             "snapshot_at": snapshot.isoformat(), "as_of": snapshot.date().isoformat(),
             "cross_listing_additive": False, "source_field_indexes": {"price": 3, "snapshot": 30, "total_market_cap": 45}}]


def extract_pdf_text(binary: bytes) -> dict[str, Any]:
    from pypdf import PdfReader

    if not binary.startswith(b"%PDF"):
        raise ValueError("response is not a PDF document")
    if len(binary) > MAX_PDF_BYTES:
        raise ValueError("PDF exceeds the bounded download size")
    reader = PdfReader(io.BytesIO(binary), strict=False)
    if reader.is_encrypted:
        raise ValueError("encrypted PDF requires authorized decryption")
    total_pages = len(reader.pages)
    pages, length = [], 0
    text_truncated = total_pages > MAX_PDF_PAGES
    for index, page in enumerate(reader.pages[:MAX_PDF_PAGES]):
        text = page.extract_text() or ""
        remaining = MAX_TEXT_CHARACTERS - length
        if remaining <= 0:
            text_truncated = True
            break
        if len(text) > remaining:
            text_truncated = True
        pages.append({"page": index + 1, "text": text[:remaining]})
        length += len(text)
    content = "\n\n".join(item["text"] for item in pages)
    return {"content": content, "pages": pages, "page_count": total_pages,
            "text_truncated": text_truncated,
            "binary_sha256": hashlib.sha256(binary).hexdigest(), "binary_size": len(binary),
            "content_scope": "PDF_EXTRACTED_TEXT", "extraction_status": "TEXT_AVAILABLE" if content.strip() else "OCR_REQUIRED"}


_CASE_NUMBER = re.compile(r"[（(]\s*(?:19|20)\d{2}\s*[）)]\s*[\u4e00-\u9fff]{1,5}\s*\d{0,4}\s*[\u4e00-\u9fff]{1,6}\s*\d+\s*号")


def extract_case_mentions(pages: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """Case-number mentions, not consolidated cases or inferred party roles."""
    result, seen = [], set()
    for page in pages:
        text = str(page.get("text") or "")
        for match in _CASE_NUMBER.finditer(text):
            case_number = re.sub(r"\s+", "", match.group()).replace("(", "（").replace(")", "）")
            if case_number in seen:
                continue
            result.append({"case_number": case_number, "page": page.get("page"),
                           "excerpt": text[max(0, match.start() - 180):match.end() + 220],
                           "verification_status": "PENDING", "party_role": None})
            seen.add(case_number)
    return result


_DISCLOSURE_TOPICS = (
    ("供应链", re.compile(r"供应商|供应链|前五[大名]?客户|主要客户|客户集中|采购额|采购金额|销售额|销售金额")),
    ("经营往来", re.compile(r"合同|中标|订单|关联交易|担保|采购|供货")),
    ("司法事项", re.compile(r"诉讼|仲裁|判决|裁定|执行案件|被执行|立案")),
)


def extract_disclosure_mentions(pages: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """Bounded verbatim keyword locations, never resolved parties or graph facts."""
    mentions = []
    for page in pages:
        text = str(page.get("text") or "")
        matches = sorted((match.start(), topic) for topic, pattern in _DISCLOSURE_TOPICS
                         for match in pattern.finditer(text))
        last_end = {}
        for position, topic in matches:
            if position < last_end.get(topic, -1):
                continue
            start = max(0, position - 140)
            end = min(len(text), start + 500)
            mentions.append({"topic": topic, "page": page.get("page"), "excerpt": text[start:end],
                             "verification_status": "PENDING"})
            last_end[topic] = end
            if len(mentions) >= 20:
                return mentions
    return mentions


def select_supply_chain_disclosures(records: list[dict[str, Any]], max_documents: int) -> list[dict[str, Any]]:
    """Reserve annual-report and direct operating-disclosure slots before recency filling."""
    annual, direct = [], []
    for record in records:
        title = re.sub(r"\s+", "", record["title"])
        if re.search(r"摘要|英文|English|业绩说明会|业绩交流会|业绩发布会|业绩路演", title, re.IGNORECASE):
            continue
        # An annual-report discussion/opinion/notice is not the report itself.
        if re.search(r"(?:19|20)\d{2}年?年度报告(?:全文)?(?:[（(](?:修订|更新|更正|中文)[^）)]*[）)])?$", title):
            annual.append({**record, "selection_basis": "完整年度报告原文"})
            continue
        if re.search(r"半年度报告|半年度业绩|中期报告|季度报告", title):
            continue
        if re.search(r"(?:收购|购买|出售|转让|增资|认购).*?(?:股权|股份|资产)|(?:股权|股份|资产).*?(?:收购|购买|出售|转让|增资|认购)", title):
            continue
        if re.search(r"供应商|客户|供应链|供销|供货|采购|销售|中标|订单|经营合同|重大合同|日常关联交易", title):
            direct.append({**record, "selection_basis": "直接供销经营披露"})
    # Prefer the latest reporting year, then the latest revision of that report.
    annual.sort(key=lambda item: (max(re.findall(r"(?:19|20)\d{2}", item["title"]), default=""),
                                  item.get("published_at") or ""), reverse=True)
    direct.sort(key=lambda item: item.get("published_at") or "", reverse=True)
    selected = annual[:1] + direct[:1] + direct[1:] + annual[1:]
    return selected[:max_documents]


def authorized_source_capabilities(environment: Mapping[str, str] | None = None) -> list[dict[str, Any]]:
    env = os.environ if environment is None else environment
    results = []
    for code, label, prefix, portal in (
        ("COMPANY_REGISTRY_AUTHORIZED", "Authorized company registration provider", "COMPANY_REGISTRY", "https://www.gsxt.gov.cn/"),
        ("COURT_DATA_AUTHORIZED", "Authorized court case provider", "COURT_DATA", "https://wenshu.court.gov.cn/"),
    ):
        configured = bool(env.get(prefix + "_API_URL") and env.get(prefix + "_API_TOKEN"))
        results.append({"source_code": code, "name": label, "source_url": portal,
            "status": "CONFIGURED_UNVERIFIED" if configured else "AUTH_REQUIRED", "connected": False,
            "required_environment": [prefix + "_API_URL", prefix + "_API_TOKEN"],
            "reason": "Provider authorization and schema verification are required; public portal is not a bulk API."})
    return results


class CompanySourcesClient:
    def __init__(self, *, client: httpx.Client | None = None, cache_dir: str | Path | None = None):
        self._owned = client is None
        self.client = client or httpx.Client(timeout=httpx.Timeout(30.0, connect=15.0), follow_redirects=False,
            headers={"User-Agent": "Mozilla/5.0", "Referer": "https://www.cninfo.com.cn/"}, trust_env=True)
        self.cache_dir = Path(cache_dir) if cache_dir is not None else None

    def __enter__(self):
        return self

    def __exit__(self, *_):
        self.close()

    def close(self):
        if self._owned:
            self.client.close()

    def _result(self, code: str, name: str, kind: str, url: str, request: dict) -> SourceResult:
        return SourceResult(code, name, kind, url, _now(), request=request)

    def _finish(self, result: SourceResult, key: str) -> SourceResult:
        if self.cache_dir:
            self.cache_dir.mkdir(parents=True, exist_ok=True)
            stamp = re.sub(r"[^0-9]", "", result.retrieved_at)
            filename = re.sub(r"[^A-Za-z0-9_-]", "_", key)
            path = self.cache_dir / f"{filename}_{stamp}.json"
            path.write_text(json.dumps(result.to_dict(), ensure_ascii=False, indent=2, allow_nan=False), encoding="utf-8")
        return result

    @staticmethod
    def _failure(result: SourceResult, error: Exception):
        if isinstance(error, httpx.HTTPStatusError) and error.response.status_code in {401, 403}:
            result.status = "AUTH_REQUIRED"
            result.warnings.append(f"HTTP_{error.response.status_code}: provider access authorization is required")
        else:
            result.status = "UNAVAILABLE"
            result.warnings.append(type(error).__name__ + ": source request or decoding failed")

    def fetch_company_profile(self, market: str, symbol: str) -> SourceResult:
        market = market.upper().strip()
        symbol, secucode = _security_code(market, symbol)
        params = {"reportName": "RPT_HKF10_INFO_ORGPROFILE" if market == "HK" else "RPT_F10_ORG_BASICINFO",
                  "columns": "ALL", "filter": f'(SECUCODE="{secucode}")', "pageNumber": "1", "pageSize": "1", "source": "F10", "client": "PC"}
        request = {"method": "GET", "url": PROFILE_URL, "params": params}
        result = self._result("EASTMONEY_COMPANY_PROFILE", "EASTMONEY", "AGGREGATOR", PROFILE_URL, request)
        result.warnings = ["Registration fields are provider-transcribed, not authenticated company-register records.",
                           "Snapshot publication time is unavailable; retrieved_at is the information availability boundary."]
        try:
            response = self.client.get(PROFILE_URL, params=params)
            response.raise_for_status()
            result.source_url = str(response.url)
            result.raw = response.json()
            result.records = parse_company_profile(result.raw, market, symbol, result.retrieved_at)
            if not result.raw.get("success", True):
                result.status = "SCHEMA_CHANGED"
            elif not result.records:
                result.status = "EMPTY"
        except (httpx.HTTPError, ValueError, TypeError, AttributeError) as error:
            self._failure(result, error)
        return self._finish(result, f"profile_{market}_{symbol}")

    def fetch_shareholders(self, market: str, symbol: str, report_date: str | None = None) -> SourceResult:
        market = market.upper().strip()
        symbol, secucode = _security_code(market, symbol)
        if report_date is not None:
            report_date = date.fromisoformat(report_date).isoformat()
        url = HOLDERS_BASE + ("PageSDGD" if report_date else "PageAjax")
        params = {"code": secucode[-2:] + symbol}
        if report_date:
            params["date"] = report_date
        result = self._result("EASTMONEY_SHAREHOLDERS", "EASTMONEY", "AGGREGATOR", url,
                              {"method": "GET", "url": url, "params": params})
        result.warnings = ["Top-ten register holders are not a complete ownership register or verified beneficial owners.",
                           "No issuer-compatible holder ID or publication timestamp is supplied; names remain unresolved mentions."]
        if market != "CN_A":
            result.status = "UNSUPPORTED"
            result.source_url = f"https://di.hkex.com.hk/di/NSSrchCorp.aspx?src=MAIN&lang=ZH&txtStockCode={symbol}"
            result.warnings.append("HK shareholder extraction is not implemented; official DI link is a reference only.")
            return self._finish(result, f"holders_{market}_{symbol}")
        try:
            response = self.client.get(url, params=params)
            response.raise_for_status()
            result.source_url = str(response.url)
            result.raw = response.json()
            result.records = parse_shareholders(result.raw, market, symbol)
            if report_date is None and result.records:
                latest = max(item["report_date"] or "" for item in result.records)
                result.records = [item for item in result.records if item["report_date"] == latest]
            elif report_date:
                result.records = [item for item in result.records if item["report_date"] == report_date]
            result.status = "SUCCESS" if result.records else "EMPTY"
        except (httpx.HTTPError, ValueError, TypeError, AttributeError) as error:
            self._failure(result, error)
        return self._finish(result, f"holders_{market}_{symbol}")

    def _download_pdf(self, url: str) -> bytes:
        parsed = urlparse(url)
        if parsed.scheme != "https" or parsed.hostname != "static.cninfo.com.cn":
            raise ValueError("PDF URL must belong to the official disclosure CDN")
        binary = bytearray()
        with self.client.stream("GET", url) as response:
            response.raise_for_status()
            for chunk in response.iter_bytes():
                binary.extend(chunk)
                if len(binary) > MAX_PDF_BYTES:
                    raise ValueError("PDF exceeds bounded download size")
        return bytes(binary)

    def fetch_market_cap(self, market: str, symbol: str) -> SourceResult:
        market = market.upper().strip()
        symbol, secucode = _security_code(market, symbol)
        params = {"q": secucode[-2:].lower() + symbol}
        result = self._result("TENCENT_MARKET_CAP", "TENCENT", "MARKET_DATA_PROVIDER", TENCENT_QUOTE,
                              {"method": "GET", "url": TENCENT_QUOTE, "params": params})
        result.warnings = ["Total cap uses provider issuer-share scope at A-share price, not the isolated listing share class.",
                           "Do not add A/H market-cap observations. Use the quote snapshot date, not retrieval date, for size rules."]
        if market != "CN_A":
            result.status = "UNSUPPORTED"
            result.warnings.append("HK market-cap normalization is intentionally unavailable pending share-scope verification.")
            return self._finish(result, f"marketcap_{market}_{symbol}")
        try:
            response = self.client.get(TENCENT_QUOTE, params=params)
            response.raise_for_status()
            text = response.content.decode("gb18030", errors="replace")
            result.source_url = str(response.url)
            result.raw = {"text": text, "encoding": "gb18030"}
            result.records = parse_market_cap(text, market, symbol)
            result.status = "SUCCESS" if result.records else "EMPTY"
            if result.records:
                result.published_at = result.records[0]["snapshot_at"]
        except (httpx.HTTPError, ValueError) as error:
            self._failure(result, error)
        return self._finish(result, f"marketcap_{market}_{symbol}")

    def fetch_legal_disclosures(self, symbol: str, *, start_date: str = "2024-01-01",
                                end_date: str | None = None, max_documents: int = 3) -> SourceResult:
        return self._fetch_disclosures(symbol, start_date=start_date, end_date=end_date,
            max_documents=max_documents, source_code="CNINFO_LEGAL_DISCLOSURES", search_terms=("诉讼", "仲裁"))

    def fetch_business_disclosures(self, symbol: str, *, start_date: str = "2025-01-01",
                                   end_date: str | None = None, max_documents: int = 2) -> SourceResult:
        return self._fetch_disclosures(symbol, start_date=start_date, end_date=end_date,
            max_documents=max_documents, source_code="CNINFO_BUSINESS_DISCLOSURES", search_terms=("合同", "中标", "采购", "担保", "关联交易"))

    def fetch_supply_chain_disclosures(self, symbol: str, *, start_date: str = "2025-01-01",
                                       end_date: str | None = None, max_documents: int = 2) -> SourceResult:
        return self._fetch_disclosures(symbol, start_date=start_date, end_date=end_date,
            max_documents=max_documents, source_code="CNINFO_SUPPLY_CHAIN_DISCLOSURES",
            search_terms=("供应商", "客户", "供应链", "年度报告", "采购", "销售", "日常关联交易"))

    def _fetch_disclosures(self, symbol: str, *, start_date: str, end_date: str | None, max_documents: int,
                           source_code: str, search_terms: tuple[str, ...]) -> SourceResult:
        symbol, _ = _security_code("CN_A", symbol)
        start = date.fromisoformat(start_date)
        end = date.fromisoformat(end_date) if end_date else date.today()
        if end < start or end > date.today() or not 1 <= max_documents <= 10:
            raise ValueError("invalid disclosure interval or max_documents (1..10)")
        is_legal = source_code == "CNINFO_LEGAL_DISCLOSURES"
        prefix = {"CNINFO_LEGAL_DISCLOSURES": "legal", "CNINFO_BUSINESS_DISCLOSURES": "business",
                  "CNINFO_SUPPLY_CHAIN_DISCLOSURES": "supply_chain"}[source_code]
        cache_key = f"{prefix}_CN_A_{symbol}"
        result = self._result(source_code, "CNINFO", "OFFICIAL_DISCLOSURE", CNINFO_NOTICES,
            {"method": "POST", "url": CNINFO_NOTICES, "symbol": symbol, "start_date": start.isoformat(),
             "end_date": end.isoformat(), "search_terms": list(search_terms), "max_documents": max_documents})
        result.warnings = (["Issuer disclosures are not a complete court-case registry. Case mentions require party and procedure review.",
                            "A subsidiary case must not be reassigned to its listed parent."] if is_legal else
                           ["Business disclosures are evidence only; contract parties, amount basis, performance and revenue are not inferred.",
                            "A signed contract or successful bid does not establish completed supply or recognized revenue."])
        result.warnings.append("Bounded title-keyword search retrieves the first 30 matches per term, not the full disclosure history.")
        if prefix == "supply_chain":
            result.request["selection_policy"] = "LATEST_FULL_ANNUAL_REPORT_AND_DIRECT_OPERATING_DISCLOSURE_V1"
            result.warnings.extend([
                "Supplier/customer and annual-report disclosures are leads only; named counterparties and supply direction require evidence review.",
                "Annual reports may anonymize top-five suppliers/customers. Anonymous ranks must not be matched to named companies.",
                f"PDF extraction is bounded to {MAX_PDF_PAGES} pages and {MAX_TEXT_CHARACTERS} characters; later supplier/customer sections may be missing.",
            ])
        try:
            lookup = self.client.post(CNINFO_SEARCH, data={"keyWord": symbol})
            lookup.raise_for_status()
            lookup_payload = lookup.json()
            match = next((item for item in lookup_payload if isinstance(item, dict) and str(item.get("code")) == symbol), None)
            if not match or not _text(match.get("orgId")):
                result.status = "EMPTY"
                result.raw = {"company_lookup": lookup_payload}
                return self._finish(result, cache_key)
            column, plate = ("sse", "sh") if symbol.startswith("6") else ("bjse", "bj") if symbol.startswith(("4", "8", "92")) else ("szse", "sz")
            queries, all_records = [], {}
            for keyword in search_terms:
                params = {"stock": f"{symbol},{match['orgId']}", "tabName": "fulltext", "pageSize": "30", "pageNum": "1",
                          "column": column, "plate": plate, "seDate": f"{start.isoformat()}~{end.isoformat()}",
                          "searchkey": keyword, "secid": "", "sortName": "", "sortType": "", "isHLtitle": "false"}
                response = self.client.post(CNINFO_NOTICES, data=params)
                response.raise_for_status()
                payload = response.json()
                queries.append({"request": params, "response": payload})
                for item in parse_cninfo_announcements(payload, symbol):
                    existing = all_records.setdefault(item["announcement_id"], {**item, "matched_search_terms": []})
                    existing["matched_search_terms"].append(keyword)
            result.raw = {"company_lookup": lookup_payload, "queries": queries}
            ordered = sorted(all_records.values(), key=lambda item: item["published_at"] or "", reverse=True)
            selected = select_supply_chain_disclosures(ordered, max_documents) if prefix == "supply_chain" else ordered[:max_documents]
            if prefix == "supply_chain":
                result.raw["selection"] = {"candidate_count": len(ordered), "selected_ids": [item["announcement_id"] for item in selected]}
                if not any(item["selection_basis"] == "完整年度报告原文" for item in selected):
                    result.warnings.append("No full annual report was selected from the bounded search results.")
                if not any(item["selection_basis"] == "直接供销经营披露" for item in selected):
                    result.warnings.append("No direct supplier/customer operating announcement was selected from the bounded search results.")
            for record in selected:
                record.update(content=None, pages=[], case_mentions=[], entity_mentions=[], disclosure_mentions=[],
                              content_scope="ANNOUNCEMENT_METADATA_ONLY", extraction_status="TITLE_ONLY")
                try:
                    if not record["url"]:
                        raise ValueError("official PDF link unavailable")
                    binary = self._download_pdf(record["url"])
                    record.update(extract_pdf_text(binary))
                    if record.get("text_truncated"):
                        result.warnings.append(f"Announcement {record['announcement_id']} text is truncated; only the recorded pages and characters were extracted.")
                    record["case_mentions"] = extract_case_mentions(record["pages"]) if is_legal else []
                    record["disclosure_mentions"] = extract_disclosure_mentions(record["pages"])
                    if self.cache_dir:
                        self.cache_dir.mkdir(parents=True, exist_ok=True)
                        path = self.cache_dir / f"cninfo_{record['announcement_id']}_{record['binary_sha256'][:16]}.pdf"
                        path.write_bytes(binary)
                except Exception as error:
                    record["extraction_error"] = type(error).__name__
                    result.warnings.append(f"Announcement {record['announcement_id']} has no verified complete text extraction: {type(error).__name__}")
                result.records.append(record)
            if not result.records:
                result.status = "EMPTY"
            elif all(item["extraction_status"] == "TEXT_AVAILABLE" and not item.get("text_truncated") for item in result.records):
                result.status = "SUCCESS"
            else:
                result.status = "PARTIAL"
        except (httpx.HTTPError, ValueError, TypeError, AttributeError) as error:
            self._failure(result, error)
        return self._finish(result, cache_key)


def fetch_company_profile(market: str, symbol: str, *, cache_dir: str | Path | None = None) -> SourceResult:
    with CompanySourcesClient(cache_dir=cache_dir) as client:
        return client.fetch_company_profile(market, symbol)


def fetch_shareholders(market: str, symbol: str, report_date: str | None = None, *, cache_dir: str | Path | None = None) -> SourceResult:
    with CompanySourcesClient(cache_dir=cache_dir) as client:
        return client.fetch_shareholders(market, symbol, report_date)


def fetch_legal_disclosures(symbol: str, *, start_date: str = "2024-01-01", end_date: str | None = None,
                           max_documents: int = 3, cache_dir: str | Path | None = None) -> SourceResult:
    with CompanySourcesClient(cache_dir=cache_dir) as client:
        return client.fetch_legal_disclosures(symbol, start_date=start_date, end_date=end_date, max_documents=max_documents)


def fetch_business_disclosures(symbol: str, *, start_date: str = "2025-01-01", end_date: str | None = None,
                              max_documents: int = 2, cache_dir: str | Path | None = None) -> SourceResult:
    with CompanySourcesClient(cache_dir=cache_dir) as client:
        return client.fetch_business_disclosures(symbol, start_date=start_date, end_date=end_date, max_documents=max_documents)


def fetch_supply_chain_disclosures(symbol: str, *, start_date: str = "2025-01-01", end_date: str | None = None,
                                   max_documents: int = 2, cache_dir: str | Path | None = None) -> SourceResult:
    with CompanySourcesClient(cache_dir=cache_dir) as client:
        return client.fetch_supply_chain_disclosures(symbol, start_date=start_date, end_date=end_date, max_documents=max_documents)


def fetch_market_cap(market: str, symbol: str, *, cache_dir: str | Path | None = None) -> SourceResult:
    with CompanySourcesClient(cache_dir=cache_dir) as client:
        return client.fetch_market_cap(market, symbol)
