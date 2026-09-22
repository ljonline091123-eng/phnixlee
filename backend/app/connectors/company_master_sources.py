"""Batch company identities with immutable source pages, never business DB writes.

Eastmoney's domestic profile table includes both exchange equities and NEEQ.
HKEX's security list is required to distinguish equity issuers from fund managers.
No issuer is inferred from a security abbreviation or an absent provider row.
"""
from __future__ import annotations

import hashlib
import io
import json
from pathlib import Path
import re
import time
from typing import Any, Callable, Iterable
from xml.etree import ElementTree
from zipfile import ZipFile

import httpx

from app.connectors.company_sources import PROFILE_URL, _jurisdiction, _now, parse_company_profile, valid_cn_uscc

HKEX_SECURITIES_URL = "https://www.hkex.com.hk/eng/services/trading/securities/securitieslists/ListOfSecurities.xlsx"
HKEX_SDW_URL = "https://www.hkexnews.hk/sdw/search/stocklist.aspx"
CODETABLE_URL = "https://search-codetable.eastmoney.com/codetable/search/web"
REPORTS = {"DOMESTIC": "RPT_F10_ORG_BASICINFO", "HK": "RPT_HKF10_INFO_ORGPROFILE"}
COMMON_COLUMNS = "SECUCODE,SECURITY_CODE,SECURITY_NAME_ABBR,ORG_CODE,ORG_NAME,FOUND_DATE,REG_ADDRESS,ORG_WEB,LISTING_DATE,ORG_TYPE"
COLUMNS = {
    "DOMESTIC": COMMON_COLUMNS + ",ORG_NAME_EN,REG_NUM,LEGAL_PERSON,STR_CODEA,STR_CODEH,SECURITY_TYPE,SECURITY_TYPE_CODE,SECURITY_INNER_CODE,LISTING_STATE,TRADE_MARKET,CSRC_INDUSTRY_NAME,BOARD_CODE_BK_1LEVEL,BOARD_NAME_1LEVEL,BOARD_CODE_BK_2LEVEL,BOARD_NAME_2LEVEL,BOARD_CODE_BK_3LEVEL,BOARD_NAME_3LEVEL,BLGAINIAN,BLGAINIAN_CODE",
    "HK": COMMON_COLUMNS + ",ORG_EN_ABBR,REG_PLACE,ISIN_CODE,BELONG_MARKET,BELONG_INDUSTRY,SECURITY_INNER_CODE",
}
HK_EQUITY_SUBCATEGORIES = {"Equity Securities (Main Board)", "Equity Securities (GEM)",
                         "Investment Companies", "Trading Only Securities", "Depositary Receipts"}
CN_EQUITY_TYPES = {"A股", "中国存托凭证"}


def _write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".partial")
    temporary.write_text(json.dumps(value, ensure_ascii=False, allow_nan=False), encoding="utf-8")
    temporary.replace(path)


def parse_hkex_securities(content: bytes) -> dict:
    """Read the public XLSX using stdlib; no optional Excel engine is required."""
    namespace = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
    with ZipFile(io.BytesIO(content)) as archive:
        if sum(item.file_size for item in archive.infolist()) > 100_000_000:
            raise ValueError("HKEX spreadsheet exceeds the extraction limit")
        strings = []
        if "xl/sharedStrings.xml" in archive.namelist():
            for item in ElementTree.fromstring(archive.read("xl/sharedStrings.xml")).findall("s:si", namespace):
                strings.append("".join(item.itertext()))
        sheet = ElementTree.fromstring(archive.read("xl/worksheets/sheet1.xml"))
        rows = []
        for row in sheet.findall(".//s:row", namespace):
            values = {}
            for cell in row.findall("s:c", namespace):
                column = re.sub(r"\d", "", cell.attrib.get("r", ""))
                value = cell.find("s:v", namespace)
                if cell.attrib.get("t") == "inlineStr":
                    inline = cell.find("s:is", namespace)
                    text = "".join(inline.itertext()) if inline is not None else ""
                elif value is None:
                    text = ""
                elif cell.attrib.get("t") == "s":
                    text = strings[int(value.text or "0")]
                else:
                    text = value.text or ""
                values[column] = text.strip()
            rows.append(values)
    headers = next((row for row in rows if "Stock Code" in row.values()), None)
    if not headers or "Category" not in headers.values() or "Sub-Category" not in headers.values():
        raise ValueError("HKEX security list schema changed")
    parsed = []
    for row in rows:
        record = {label: row.get(column, "") for column, label in headers.items() if label}
        code = record.get("Stock Code", "")
        if re.fullmatch(r"\d{1,5}", code):
            record["Stock Code"] = code.zfill(5)
            parsed.append(record)
    if not parsed:
        raise ValueError("HKEX security list has no security records")
    return {"records": parsed, "source_date_label": next((value for row in rows[:4] for value in row.values() if value.startswith("Updated")), None)}


def expected_secucode(market: str, symbol: str) -> str:
    if market in {"NEEQ", "NEEQ_INNOVATION"}:
        return f"{symbol}.NQ"
    if market == "HK":
        return f"{symbol.zfill(5)}.HK"
    exchange = "SH" if symbol.startswith("6") else "BJ" if symbol.startswith(("4", "8", "92")) else "SZ"
    return f"{symbol}.{exchange}"


def parse_master_record(raw: dict, market: str, symbol: str, observed_at: str) -> dict | None:
    if raw.get("SECUCODE") != expected_secucode(market, symbol):
        return None
    if market in {"CN_A", "HK"}:
        records = parse_company_profile({"result": {"data": [raw]}}, market, symbol, observed_at)
        record = records[0] if records else None
        if record and market == "CN_A" and raw.get("SECURITY_TYPE") == "中国存托凭证":
            # A domestic trading venue does not establish a CDR issuer's domicile.
            record["company"]["jurisdiction"] = "UNKNOWN"
        return record
    name, issuer = str(raw.get("ORG_NAME") or "").strip(), str(raw.get("ORG_CODE") or "").strip()
    if not name or not issuer or raw.get("SECURITY_TYPE") != "三板股":
        return None
    registration = str(raw.get("REG_NUM") or "").strip().upper() or None
    profile = {"registered_name": name, "source_issuer_id": issuer, "source_namespace": "EASTMONEY",
        "english_name": raw.get("ORG_NAME_EN"), "registration_number_raw": registration,
        "registration_verified": False, "registration_source_kind": "AGGREGATOR",
        "registration_checksum_valid": valid_cn_uscc(registration) if registration else None,
        "registered_address": raw.get("REG_ADDRESS"), "legal_representative": raw.get("LEGAL_PERSON"),
        "incorporated_on": str(raw.get("FOUND_DATE") or "")[:10] or None,
        "website": raw.get("ORG_WEB"), "industry_label_raw": raw.get("CSRC_INDUSTRY_NAME")}
    company = {"name": name, "jurisdiction": "CN", "source_issuer_id": issuer, "properties_json": profile}
    if valid_cn_uscc(registration):
        company.update(identifier_scheme="CN_USCC", identifier_value=registration)
    industries = [{"source_issuer_id": f"industry:EASTMONEY:{raw[f'BOARD_CODE_BK_{level}LEVEL']}",
        "code": raw[f"BOARD_CODE_BK_{level}LEVEL"], "name": raw[f"BOARD_NAME_{level}LEVEL"],
        "taxonomy": "EASTMONEY_INDUSTRY", "taxonomy_version": "PROVIDER_CURRENT_UNVERSIONED", "level": level}
        for level in (1, 2, 3) if raw.get(f"BOARD_CODE_BK_{level}LEVEL") and raw.get(f"BOARD_NAME_{level}LEVEL")]
    return {"market": market, "symbol": symbol, "company": company, "profile": profile, "industries": industries,
        "themes": [], "controller_mentions": [], "source_record": raw,
        "evidence": {"source_key": f"company:{issuer}:{market}:{symbol}", "title": f"{name} 挂牌公司主体资料 ({symbol}.NQ)",
            "content": json.dumps(raw, ensure_ascii=False, sort_keys=True, indent=2),
            "published_at": None, "available_at": observed_at, "content_scope": "PROVIDER_STRUCTURED_RECORD"}}


def apply_registration_jurisdictions(records: list[dict], pages: Iterable[dict]) -> None:
    """Use an explicit registration field, only for an unambiguous issuer identity.

    Existing jurisdiction value conventions are preserved. This adds provenance;
    it does not rename persisted companies or use their trading venue as domicile.
    """
    issuers: dict[str, list[tuple[dict, dict]]] = {}
    for page in pages:
        for row in (page.get("raw", {}).get("result") or {}).get("data") or []:
            if str(row.get("SECUCODE") or "").endswith(".HK") and row.get("ORG_CODE") and row.get("REG_PLACE"):
                issuers.setdefault(str(row["ORG_CODE"]), []).append((row, page))
    for record in records:
        candidates = issuers.get(record["company"]["source_issuer_id"], [])
        if (not candidates or {row.get("ORG_NAME") for row, _ in candidates} != {record["company"]["name"]}
                or len({str(row["REG_PLACE"]).strip() for row, _ in candidates}) != 1):
            continue
        raw, page = candidates[0]
        jurisdiction = _jurisdiction(raw, "HK")
        evidence = {"source_name": "EASTMONEY", "source_url": page["source_url"],
            "observed_at": page["retrieved_at"], "source_record": raw,
            "registration_place": raw["REG_PLACE"], "jurisdiction": jurisdiction,
            "match_rule": "EXACT_ORG_CODE_AND_REGISTERED_NAME_UNIQUE_REG_PLACE"}
        record["registration_jurisdiction_evidence"] = evidence
        if record["company"].get("identifier_scheme") == "CN_USCC" and jurisdiction != "CN":
            record["jurisdiction_conflict"] = "CN_USCC_CONFLICTS_WITH_EXPLICIT_REG_PLACE"
            continue
        record["company"]["jurisdiction"] = jurisdiction
        record["company"].setdefault("properties_json", {})["registration_place"] = raw["REG_PLACE"]
        record["company"]["properties_json"]["jurisdiction_basis"] = evidence["match_rule"]


def codetable_classification(row: dict, symbol: str) -> str | None:
    """Only documented, independently checked stock/fund type combinations."""
    if row.get("market") != 116 or row.get("code") != symbol:
        return None
    types = row.get("securityType") or []
    if row.get("smallType") == 3 and 102 in types:
        return "ORDINARY_EQUITY"
    if row.get("smallType") == 1 and row.get("extSmallType") in {10, 16} and 6 in types and 102 not in types:
        return "NON_EQUITY"
    return None


def build_master_result(targets: Iterable[dict], pages: Iterable[dict], hkex: dict | None) -> dict:
    """Keep every target either explicitly mapped, excluded, or unresolved."""
    pages = list(pages)
    indexed: dict[str, list[tuple[dict, dict]]] = {}
    for page in pages:
        for row in (page.get("raw", {}).get("result") or {}).get("data") or []:
            indexed.setdefault(row.get("SECUCODE", ""), []).append((row, page))
    hk_rows = {row["Stock Code"]: row for row in (hkex or {}).get("records", [])}
    records, exclusions, unresolved = [], [], []
    seen = set()
    for target in targets:
        market, symbol = target["market"], target["symbol"]
        key = (market, symbol)
        if key in seen:
            continue
        seen.add(key)
        identity = {"market": market, "symbol": symbol}
        hk_row = hk_rows.get(symbol) if market == "HK" else None
        if market == "HK":
            original = target.get("raw_payload") or {}
            metadata = target.get("ext_json") or {}
            if isinstance(original, str):
                original = json.loads(original)
            if isinstance(metadata, str):
                metadata = json.loads(metadata)
            mainland_reference = re.search(r"\(A\s*#\s*(\d{6})\)\s*$", str(original.get("n") or ""))
            if (metadata.get("source_endpoint") == "HKEX_SDW" and
                    str(original.get("c") or "").zfill(5) == symbol and mainland_reference):
                exclusions.append({**identity, "reason": "NOT_HK_LISTED_SECURITY", "source_name": "HKEX_SDW",
                    "source_url": HKEX_SDW_URL, "observed_at": target.get("last_synced_at"),
                    "source_record": original, "referenced_market": "CN_A", "referenced_symbol": mainland_reference.group(1),
                    "evidence_scope": "EXISTING_OFFICIAL_SDW_RECORD_EXPLICIT_MAINLAND_SECURITY_REFERENCE"})
                continue
            if hk_row is None:
                unresolved.append({**identity, "reason": "HKEX_SECURITY_CLASSIFICATION_MISSING"})
                continue
            if hk_row.get("Category") != "Equity" or hk_row.get("Sub-Category") not in HK_EQUITY_SUBCATEGORIES:
                exclusions.append({**identity, "reason": "NON_EQUITY", "source_name": "HKEX",
                    "source_url": HKEX_SECURITIES_URL, "observed_at": hkex.get("retrieved_at"),
                    "source_date_label": hkex.get("source_date_label"), "source_record": hk_row})
                continue
        candidates = indexed.get(expected_secucode(market, symbol), [])
        issuers = {(str(row.get("ORG_CODE") or ""), str(row.get("ORG_NAME") or "")) for row, _ in candidates}
        if len(issuers) > 1:
            unresolved.append({**identity, "reason": "CONFLICTING_PROVIDER_IDENTITIES"})
            continue
        if not candidates:
            unresolved.append({**identity, "reason": "COMPANY_PROFILE_MISSING"})
            continue
        raw, page = candidates[0]
        if market == "CN_A" and raw.get("SECURITY_TYPE") not in CN_EQUITY_TYPES:
            unresolved.append({**identity, "reason": "SECURITY_CLASSIFICATION_MISMATCH"})
            continue
        record = parse_master_record(raw, market, symbol, page["retrieved_at"])
        if not record:
            unresolved.append({**identity, "reason": "INCOMPLETE_PROVIDER_IDENTITY"})
            continue
        is_dr = raw.get("SECURITY_TYPE") == "中国存托凭证" or (hk_row or {}).get("Sub-Category") == "Depositary Receipts"
        record.update(source_name="EASTMONEY", source_url=page["source_url"], observed_at=page["retrieved_at"],
            source_report=page["report_name"], security_type="DEPOSITARY_RECEIPT" if is_dr else "ORDINARY_EQUITY",
            share_class="DEPOSITARY_RECEIPT" if is_dr else "A_SHARE" if market == "CN_A" else "NEEQ_SHARE" if market.startswith("NEEQ") else "ORDINARY")
        if hk_row:
            record["security_classification_evidence"] = {"source_name": "HKEX", "source_url": HKEX_SECURITIES_URL,
                "observed_at": hkex.get("retrieved_at"), "source_date_label": hkex.get("source_date_label"), "source_record": hk_row}
        records.append(record)
    apply_registration_jurisdictions(records, pages)
    return {"source_code": "EASTMONEY_COMPANY_MASTER", "source_name": "EASTMONEY", "source_kind": "AGGREGATOR",
        "source_url": PROFILE_URL, "retrieved_at": _now(), "status": "PARTIAL" if unresolved else "SUCCESS",
        "records": records, "exclusions": exclusions, "unresolved": unresolved,
        "warnings": ["Company registry fields are provider transcriptions, not authenticated registration checks.",
            "Only explicit issuer identifiers and exact security codes are used; missing identities remain unresolved.",
            "HKEX classifies listed securities; exclusions are not missing ordinary-stock company mappings.",
            "Source publication/update labels are preserved separately from retrieval time; snapshots are not historical mapping evidence."]}


class CompanyMasterSourcesClient:
    def __init__(self, cache_dir: Path, *, client: httpx.Client | None = None, page_size: int = 500,
                 delay: float = 0.25, attempts: int = 3, progress: Callable[[dict], None] | None = None):
        if not 1 <= page_size <= 1000 or not 1 <= attempts <= 5 or not 0 <= delay <= 10:
            raise ValueError("Invalid batch collection limits")
        self.cache_dir, self.page_size, self.delay, self.attempts = Path(cache_dir), page_size, delay, attempts
        self.client = client or httpx.Client(timeout=45, follow_redirects=True, headers={"User-Agent": "Mozilla/5.0"})
        self._own_client = client is None
        self.progress = progress or (lambda value: None)

    def __enter__(self):
        return self

    def __exit__(self, *_):
        if self._own_client:
            self.client.close()

    def _get(self, url: str, **kwargs) -> httpx.Response:
        for attempt in range(self.attempts):
            try:
                time.sleep(self.delay)
                response = self.client.get(url, **kwargs)
                response.raise_for_status()
                return response
            except httpx.HTTPError as error:
                if isinstance(error, httpx.HTTPStatusError) and error.response.status_code in {401, 403, 429}:
                    raise  # Do not retry authorization gates or exceed provider rate limits.
                if attempt + 1 == self.attempts:
                    raise
                time.sleep(min(2 ** attempt, 4))
        raise AssertionError("unreachable")

    def collect_profiles(self, group: str, *, resume: bool = True, max_pages: int = 200) -> list[dict]:
        report = REPORTS[group]
        pages, first_count, previous_codes = [], None, set()
        for number in range(1, max_pages + 1):
            params = {"reportName": report, "columns": COLUMNS[group], "pageNumber": number,
                "pageSize": self.page_size, "source": "F10", "client": "PC", "sortColumns": "SECUCODE", "sortTypes": "1"}
            path = self.cache_dir / "pages" / f"{group}_{number:04}.json"
            page = json.loads(path.read_text(encoding="utf-8")) if resume and path.exists() else None
            if page is None or page.get("request") != params:
                response = self._get(PROFILE_URL, params=params)
                raw = response.json()
                if raw.get("success") is not True or not isinstance(raw.get("result"), dict):
                    raise ValueError(f"{report} returned an unsuccessful response")
                page = {"report_name": report, "source_name": "EASTMONEY", "source_url": str(response.url),
                    "retrieved_at": _now(), "request": params, "raw": raw}
                _write_json(path, page)
            result = page["raw"]["result"]
            rows = result.get("data") or []
            count = int(result.get("count") or 0)
            if first_count is None:
                first_count = count
            if count != first_count:
                raise ValueError(f"{report} universe changed during pagination; start a fresh snapshot")
            codes = {row.get("SECUCODE") for row in rows}
            if not rows or previous_codes.intersection(codes):
                raise ValueError(f"{report} empty or repeated page before completion")
            previous_codes.update(codes)
            pages.append(page)
            self.progress({"group": group, "page": number, "pages": result.get("pages"), "rows": len(rows), "total": count})
            if sum(len(p["raw"]["result"].get("data") or []) for p in pages) >= count:
                return pages
        raise ValueError(f"{report} exceeds the bounded page limit")

    def collect_hkex(self, *, resume: bool = True) -> dict:
        path = self.cache_dir / "hkex_security_classification.json"
        if resume and path.exists():
            return json.loads(path.read_text(encoding="utf-8"))
        response = self._get(HKEX_SECURITIES_URL)
        content = response.content
        if len(content) > 20_000_000:
            raise ValueError("HKEX spreadsheet exceeds download limit")
        parsed = parse_hkex_securities(content)
        parsed.update(source_name="HKEX", source_url=HKEX_SECURITIES_URL, retrieved_at=_now(),
            content_sha256=hashlib.sha256(content).hexdigest())
        self.cache_dir.mkdir(parents=True, exist_ok=True)
        (self.cache_dir / "hkex_list_of_securities.xlsx").write_bytes(content)
        _write_json(path, parsed)
        self.progress({"group": "HKEX", "rows": len(parsed["records"]), "source_date_label": parsed["source_date_label"]})
        return parsed

    def collect(self, targets: Iterable[dict], *, resume: bool = True) -> dict:
        targets = list(targets)
        pages, failures, hkex = [], [], None
        markets = {target["market"] for target in targets}
        for group in (["DOMESTIC"] if markets - {"HK"} else []) + (["HK"] if "HK" in markets else []):
            try:
                pages.extend(self.collect_profiles(group, resume=resume))
            except (httpx.HTTPError, ValueError, KeyError, TypeError) as error:
                failures.append({"source": REPORTS[group], "error": str(error)[:500]})
                self.progress({"group": group, "status": "FAILED", "error": str(error)[:200]})
        if "HK" in markets:
            try:
                hkex = self.collect_hkex(resume=resume)
            except (httpx.HTTPError, ValueError, KeyError, ElementTree.ParseError) as error:
                failures.append({"source": "HKEX", "error": str(error)[:500]})
        result = build_master_result(targets, pages, hkex)
        result["failures"] = failures
        if failures:
            result["status"] = "PARTIAL" if result["records"] else "UNAVAILABLE"
        _write_json(self.cache_dir / "result.json", result)
        manifest = {key: value for key, value in result.items() if key not in {"records", "exclusions", "unresolved"}}
        manifest.update(target_count=len(targets), record_count=len(result["records"]), exclusion_count=len(result["exclusions"]),
            unresolved_count=len(result["unresolved"]), result_path=str(self.cache_dir / "result.json"))
        _write_json(self.cache_dir / "manifest.json", manifest)
        return result

    def collect_supplement(self, targets: Iterable[dict], *, max_requests: int = 100, resume: bool = True) -> dict:
        """Supplement unresolved codes, keeping the original snapshot untouched."""
        targets = list(targets)
        original = json.loads((self.cache_dir / "result.json").read_text(encoding="utf-8"))
        missing = {(row["market"], row["symbol"]) for row in original["unresolved"]}
        pages = [json.loads(path.read_text(encoding="utf-8")) for path in sorted((self.cache_dir / "pages").glob("*.json"))
                 if path.name.startswith(("DOMESTIC_", "HK_"))]
        hkex_path = self.cache_dir / "hkex_security_classification.json"
        hkex = json.loads(hkex_path.read_text(encoding="utf-8")) if hkex_path.exists() else None
        selected = [row for row in targets if (row["market"], row["symbol"]) in missing]
        supplement = build_master_result(selected, pages, hkex)
        failures, resolved, attempts = [], set(), 0
        for missing_row in supplement["unresolved"]:
            if missing_row["market"] != "HK" or attempts >= max_requests:
                continue
            symbol = missing_row["symbol"]
            params = {"keyword": symbol, "pageIndex": 1, "pageSize": 20}
            path = self.cache_dir / "pages" / f"CODETABLE_{symbol}.json"
            try:
                source = json.loads(path.read_text(encoding="utf-8")) if resume and path.exists() else None
                if source is None:
                    attempts += 1
                    response = self._get(CODETABLE_URL, params=params)
                    payload = response.json()
                    if not isinstance(payload.get("result"), list):
                        raise ValueError("Code-table response has no result array")
                    source = {"source_name": "EASTMONEY_CODETABLE", "source_url": str(response.url),
                        "observed_at": _now(), "request": params, "raw": payload}
                    _write_json(path, source)
                rows = [row for row in source["raw"]["result"] if row.get("market") == 116 and row.get("code") == symbol]
                missing_row["classification_source_attempt"] = {"source_name": source["source_name"],
                    "source_url": source["source_url"], "observed_at": source["observed_at"], "exact_rows": len(rows)}
                if len(rows) != 1:
                    continue
                raw = rows[0]
                category = codetable_classification(raw, symbol)
                proof = {key: source[key] for key in ("source_name", "source_url", "observed_at")}
                proof["source_record"] = raw
                if category == "NON_EQUITY":
                    supplement["exclusions"].append({"market": "HK", "symbol": symbol, "reason": "NON_EQUITY", **proof})
                    resolved.add(("HK", symbol))
                elif category == "ORDINARY_EQUITY":
                    candidates = [(row, page) for page in pages for row in page["raw"]["result"]["data"]
                                  if row.get("SECUCODE") == f"{symbol}.HK"]
                    if len(candidates) != 1:
                        continue
                    company_raw, page = candidates[0]
                    record = parse_master_record(company_raw, "HK", symbol, page["retrieved_at"])
                    if record is None:
                        continue
                    record.update(source_name="EASTMONEY", source_url=page["source_url"], observed_at=page["retrieved_at"],
                        source_report=page["report_name"], security_type="ORDINARY_EQUITY", share_class="ORDINARY",
                        security_classification_evidence=proof)
                    supplement["records"].append(record)
                    resolved.add(("HK", symbol))
                self.progress({"group": "CODETABLE", "symbol": symbol, "classification": category})
            except (httpx.HTTPError, ValueError, KeyError, TypeError) as error:
                failures.append({"source": "EASTMONEY_CODETABLE", "symbol": symbol, "error": str(error)[:500]})
                if isinstance(error, httpx.HTTPStatusError) and error.response.status_code in {401, 403, 429}:
                    break
        supplement["unresolved"] = [row for row in supplement["unresolved"] if (row["market"], row["symbol"]) not in resolved]
        supplement["failures"] = failures
        supplement["status"] = "PARTIAL" if failures or supplement["unresolved"] else "SUCCESS"
        apply_registration_jurisdictions(supplement["records"], pages)
        _write_json(self.cache_dir / "supplemental_result.json", supplement)
        combined = build_master_result(targets, pages, hkex)
        for kind in ("records", "exclusions"):
            by_key = {(row["market"], row["symbol"]): row for row in combined[kind]}
            by_key.update({(row["market"], row["symbol"]): row for row in supplement[kind]})
            combined[kind] = list(by_key.values())
        combined["unresolved"] = [row for row in combined["unresolved"] if (row["market"], row["symbol"]) not in resolved]
        combined["failures"] = failures
        combined["status"] = "PARTIAL" if failures or combined["unresolved"] else "SUCCESS"
        _write_json(self.cache_dir / "combined_result.json", combined)
        return supplement
