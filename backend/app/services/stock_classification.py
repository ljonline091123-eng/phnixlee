"""Read-only stock classification projection used by the F10 read model.

The stock detail page historically treated industry, exchange board, index
membership and investment themes as one comma separated ``板块`` field.  That
made an index (for example CSI 300) look like an industry/theme.  This module
keeps the provider facts intact and builds a small, explicitly typed display
projection instead.  It deliberately does not infer a classification when no
source record exists.
"""

from __future__ import annotations

import json
import re
import time
import unicodedata
from datetime import datetime, timezone
from typing import Any
from urllib.parse import urlencode

import httpx
from sqlalchemy import inspect, select, tuple_
from sqlalchemy.orm import Session

from app.connectors.base import QuoteRecord
from app.connectors.akshare_adapter import AkshareAdapter
from app.models.company_graph import SecurityClassification
from app.models.foundation import FoundationEvidence, FoundationListing, FoundationSecurity
from app.models.market_data import DataSource, StockRealtimeQuote, StockSymbol
from app.models.taxonomy import ClassificationDefinition
from app.services.stock_on_demand import StockOnDemandService, _parse_timestamp
from app.services.taxonomy import label_definition


def _split(value: Any) -> list[str]:
    if value in (None, ""):
        return []
    text = str(value)
    return [item.strip() for item in re.split(r"[,，;；|、]+", text) if item.strip()]


def _clean_industry(value: Any) -> list[str]:
    values = []
    for item in _split(value):
        # Symbol-master industry values sometimes carry an alphabetic CSRC
        # prefix (``K 房地产``).  The code is not the user-facing label.
        cleaned = re.sub(r"^[A-Za-z]\s+", "", item).strip()
        if cleaned and cleaned not in values:
            values.append(cleaned)
    return values


_EASTMONEY_BK_CODE_RE = re.compile(r"^BK\d{4,}$", re.IGNORECASE)


def _is_eastmoney_industry_code(code: Any) -> bool:
    """Return whether *code* is an Eastmoney BK provider code.

    Eastmoney uses the same ``BK`` namespace for industry and concept boards.
    The namespace alone is therefore not a semantic claim; callers should use
    this helper only when the source field/dimension says the value is an
    industry hierarchy (or when repairing a legacy BOARD row).
    """
    return bool(_EASTMONEY_BK_CODE_RE.fullmatch(str(code or "").strip().upper()))


_PROVIDER_INDUSTRY_LABEL_DEFINITIONS: dict[str, str] = {
    "银行": "以吸收存款、发放贷款、支付结算及其他银行金融服务为主要业务的东方财富行业板块。",
    "银行Ⅱ": "银行行业的第二级细分板块，通常用于进一步区分银行业成分证券。",
    "股份制银行Ⅲ": "银行行业下按股份制商业银行口径归集的第三级细分板块。",
    "货币金融服务": "以货币金融中介、银行及相关金融服务为主的来源行业板块。",
    "房地产": "按房地产开发、经营、物业服务及相关产业链成分证券归集的来源行业板块。",
    "房地产开发": "按住宅、商业或综合不动产开发、销售及配套运营成分证券归集的来源行业板块。",
}


def _provider_industry_definition(
    label: Any,
    code: Any = None,
    level: Any = None,
) -> dict[str, Any] | None:
    """Build a concrete, source-scoped definition for an industry label.

    The provider does not publish a prose glossary for every changing BK
    board.  We still expose a label-specific explanation and retain the exact
    provider code/level, instead of showing the misleading exchange-board
    definition used by older projections.
    """
    text = str(label or "").strip()
    if not text:
        return None
    if not (_is_eastmoney_industry_code(code) or text in _PROVIDER_INDUSTRY_LABEL_DEFINITIONS):
        return None
    base = _PROVIDER_INDUSTRY_LABEL_DEFINITIONS.get(
        text,
        f"按东方财富行业板块“{text}”归集的相关证券分类，具体成分以采集时点的来源名单为准。",
    )
    code_text = str(code or "").strip()
    level_text = f"第{level}级" if level not in (None, "") else ""
    detail = f"{base}"
    if code_text:
        detail += f"来源代码为{code_text}"
        if level_text:
            detail += f"，属于{level_text}行业层级"
        detail += "。"
    return {
        "taxonomy": "EASTMONEY_INDUSTRY",
        "dimension": "INDUSTRY",
        "code": code_text or f"LABEL_PROVIDER_INDUSTRY_{text}",
        "label": text,
        "definition": detail,
        "criteria": "仅表示来源行业板块归属；成员、层级和走势随来源快照变化，不等同于交易所上市板块或投资建议。",
        "source_name": "东方财富行业板块主数据",
        "definition_version": "EASTMONEY_INDUSTRY_LABEL_V1",
        "jurisdiction": "CN",
    }


def _is_legacy_industry_board(dimension: str, code: Any, properties: dict[str, Any]) -> bool:
    """Identify BK rows historically persisted under the BOARD dimension."""
    if str(dimension or "").upper() != "BOARD" or not _is_eastmoney_industry_code(code):
        return False
    taxonomy = str(properties.get("taxonomy") or properties.get("source_taxonomy") or "").upper()
    # A real exchange-board fact should carry an explicit listing taxonomy.
    # Older imports did not, so BK rows without that marker are treated as
    # provider industry hierarchy and exposed under INDUSTRY.
    return taxonomy not in {"LISTED_BOARD", "EXCHANGE_BOARD", "EXCHANGE_LISTING_BOARD"}


def _item(label: Any, *, code: Any = None, source: str = "证券主数据", status: str = "SOURCE",
          definition: str | None = None, criteria: str | None = None,
          source_name: str | None = None, definition_version: str | None = None,
          source_url: str | None = None, level: Any = None,
          classification_dimension: str | None = None) -> dict[str, Any] | None:
    text = str(label or "").strip()
    if not text:
        return None
    row: dict[str, Any] = {"label": text, "name": text, "code": str(code).strip() if code else None,
                           "source": source, "status": status}
    if definition:
        row["definition"] = definition
    if criteria:
        row["criteria"] = criteria
    if source_name:
        row["source_name"] = source_name
    if definition_version:
        row["definition_version"] = definition_version
    if source_url:
        row["source_url"] = source_url
    if level is not None:
        row["level"] = level
    if classification_dimension:
        row["classification_dimension"] = str(classification_dimension).upper()
    return row


def _group(key: str, title: str, items: list[dict[str, Any]]) -> dict[str, Any] | None:
    unique: dict[tuple[str, str], dict[str, Any]] = {}
    for item in items:
        unique[(str(item.get("code") or ""), str(item.get("label") or ""))] = item
    if not unique:
        return None
    return {"key": key, "title": title, "items": list(unique.values())}


def _looks_like_index(label: Any, code: Any = None) -> bool:
    """Conservatively separate index membership from business themes."""
    text = unicodedata.normalize("NFKC", str(label or "")).strip().upper().replace(" ", "")
    code_text = str(code or "").strip().upper()
    if not text:
        return False
    # Eastmoney's ``BLGAINIAN`` feed mixes investable themes with index,
    # eligibility and trading-status labels.  The latter must not leak into
    # the concept/industry panels.  Keep this list deliberately conservative:
    # it removes well-known index families and status labels while preserving
    # business themes such as ``智能家居`` and ``养老概念``.
    if any(token in text for token in (
        "MSCI", "HS300", "沪深", "中证", "上证", "深证", "深成指", "国证", "巨潮",
        "富时", "恒生", "标准普尔", "央视", "指数", "深股通", "深市精选", "成分",
        "昨日涨停", "昨日首板", "高振幅", "破净", "热股", "多板",
        "风格",
    )):
        return True
    if re.search(r"(?:^|[A-Z])(?:A|R)?(?:50|100|300|500)(?:$|[A-Z])", text):
        return True
    if re.search(r"(?:50|100|300|500)R?$", text):
        return True
    return code_text.startswith(("BK05", "BK06", "BK07", "BK08")) and text.endswith("R")


_EASTMONEY_PROFILE_URL = "https://datacenter.eastmoney.com/securities/api/data/v1/get"
_EASTMONEY_BOARD_MEMBERS_URL = "https://push2.eastmoney.com/api/qt/clist/get"
_EASTMONEY_BOARD_URL = "https://quote.eastmoney.com/center/boardlist.html"
_TENCENT_QUOTE_URL = "https://qt.gtimg.cn/q="


def _as_of(value: Any) -> str | None:
    """Serialize provider availability without inventing a publication date."""
    if value is None:
        return None
    if hasattr(value, "isoformat"):
        return value.isoformat()
    text = str(value).strip()
    return text or None


def _freshness_status(as_of: str | None, *, max_age_days: int = 7) -> str:
    """Classify a source snapshot without treating an absent date as fresh."""
    if not as_of:
        return "UNKNOWN"
    parsed = _parse_timestamp(as_of)
    if parsed is None:
        return "UNKNOWN"
    try:
        now = datetime.now(timezone.utc)
        if parsed.tzinfo is None:
            parsed = parsed.replace(tzinfo=timezone.utc)
        age_seconds = (now - parsed).total_seconds()
    except (TypeError, ValueError):
        return "UNKNOWN"
    return "FRESH" if age_seconds <= max_age_days * 86400 else "STALE"


def _member_from_payload(payload: dict[str, Any], evidence: FoundationEvidence | None = None,
                         *, classification_code: str | None = None) -> dict[str, Any] | None:
    raw_code = payload.get("SECURITY_CODE") or payload.get("security_code")
    raw_name = payload.get("SECURITY_NAME_ABBR") or payload.get("SECURITY_NAME") or payload.get("ORG_NAME")
    if not raw_code or not raw_name:
        return None
    code = str(raw_code).strip().upper()
    secu_code = str(payload.get("SECUCODE") or "").upper()
    market = "HK" if secu_code.endswith(".HK") else "CN_A"
    # The public F10 filter is broader than the application's current
    # ``CN_A`` master universe: it may return Shanghai/Shenzhen B shares,
    # Beijing Exchange and NEEQ securities.  Do not silently relabel those
    # rows as A shares; their provider totals remain available for coverage
    # comparison and a future market adapter can ingest them explicitly.
    if market == "CN_A":
        normalized_code = code.zfill(6) if code.isdigit() else code
        if not _is_supported_cn_a_board_symbol(normalized_code):
            return None
    symbol = code.zfill(5 if market == "HK" else 6) if code.isdigit() else code
    member = {
        "market": market,
        "symbol": symbol,
        "name": str(raw_name).strip(),
        "classification_code": classification_code,
    }
    if evidence is not None:
        member.update(
            source_name=evidence.source_name,
            source_url=evidence.url,
            as_of=_as_of(evidence.available_at),
        )
    return member


def _classification_source_url(code: str, dimension: str, level: Any = None) -> str | None:
    """Return the exact public query used for a provider classification."""
    dimension = str(dimension or "").upper()
    if not code:
        return None
    if dimension == "INDUSTRY":
        try:
            level_number = int(level or 1)
        except (TypeError, ValueError):
            level_number = 1
        level_number = min(max(level_number, 1), 3)
        field = f"BOARD_CODE_BK_{level_number}LEVEL"
        params = {
            "reportName": "RPT_F10_ORG_BASICINFO",
            "columns": "SECUCODE,SECURITY_CODE,SECURITY_NAME_ABBR,BOARD_CODE_BK_1LEVEL,BOARD_NAME_1LEVEL,BOARD_CODE_BK_2LEVEL,BOARD_NAME_2LEVEL,BOARD_CODE_BK_3LEVEL,BOARD_NAME_3LEVEL",
            "pageNumber": "1", "pageSize": "500", "source": "F10", "client": "PC",
            "filter": f'({field}="{code}")',
        }
        return f"{_EASTMONEY_PROFILE_URL}?{urlencode(params)}"
    if dimension in {"THEME", "INDEX"}:
        return f"{_EASTMONEY_BOARD_URL}#concept_board/{code}"
    return None


def _target_specs(groups: list[dict[str, Any]]) -> dict[str, list[tuple[str, dict[str, Any]]]]:
    specs: dict[str, list[tuple[str, dict[str, Any]]]] = {}
    for group in groups:
        group_key = str(group.get("key") or "")
        for item in group.get("items") or []:
            code = str(item.get("code") or "").strip()
            if code:
                specs.setdefault(code, []).append((group_key, item))
    return specs


def _payload_matches(payload: dict[str, Any], group_key: str, item: dict[str, Any], code: str) -> bool:
    dimension = str(item.get("classification_dimension") or "").upper()
    if not dimension:
        dimension = {"industry": "INDUSTRY", "theme": "THEME", "index": "INDEX"}.get(group_key, "")
    if dimension == "INDUSTRY":
        level = item.get("level")
        fields = [f"BOARD_CODE_BK_{int(level)}LEVEL"] if str(level or "").isdigit() else [
            "BOARD_CODE_BK_1LEVEL", "BOARD_CODE_BK_2LEVEL", "BOARD_CODE_BK_3LEVEL"
        ]
        return any(str(payload.get(field) or "").strip().upper() == code.upper() for field in fields)
    if dimension in {"THEME", "INDEX"}:
        return code.upper() in {part.upper() for part in _split(payload.get("BLGAINIAN_CODE"))}
    return False


def _load_foundation_members(db: Session, groups: list[dict[str, Any]],
                             members: dict[tuple[str, str], dict[tuple[str, str], dict[str, Any]]],
                             source_urls: dict[str, str]) -> None:
    """Project members from immutable EASTMONEY profile evidence.

    ``SecurityClassification`` is intentionally not the only source here:
    governance may leave an imported fact pending, while the same immutable
    profile evidence already contains the complete provider membership lists.
    """
    specs = _target_specs(groups)
    if not specs:
        return
    try:
        rows = db.execute(
            select(FoundationListing, FoundationEvidence)
            .join(FoundationEvidence, FoundationEvidence.id == FoundationListing.evidence_id)
        ).all()
    except Exception:
        return
    for listing, evidence in rows:
        try:
            payload = json.loads(evidence.content or "{}")
        except (TypeError, ValueError, json.JSONDecodeError):
            continue
        if not isinstance(payload, dict):
            continue
        member = _member_from_payload(payload, evidence)
        if member is None:
            continue
        for code, code_specs in specs.items():
            for group_key, item in code_specs:
                if not _payload_matches(payload, group_key, item, code):
                    continue
                member_copy = {**member, "classification_code": code}
                key = (str(group_key), code)
                members.setdefault(key, {}).setdefault((member_copy["market"], member_copy["symbol"]), member_copy)
                source_urls.setdefault(code, evidence.url or "")


def _load_reviewed_members(db: Session, groups: list[dict[str, Any]],
                           members: dict[tuple[str, str], dict[tuple[str, str], dict[str, Any]]]) -> None:
    """Keep reviewed/pending classification evidence as a compatibility path."""
    specs = _target_specs(groups)
    if not specs:
        return
    try:
        rows = db.execute(
            select(SecurityClassification, FoundationEvidence)
            .join(FoundationEvidence, FoundationEvidence.id == SecurityClassification.evidence_id)
            .where(SecurityClassification.code.in_(list(specs)), SecurityClassification.status.in_(("ACCEPTED", "PENDING")))
            .order_by(SecurityClassification.updated_at.desc())
        ).all()
    except Exception:
        return
    for classification, evidence in rows:
        classification_code = getattr(classification, "code", None)
        if not classification_code:
            continue
        try:
            payload = json.loads(evidence.content or "{}")
        except (TypeError, ValueError, json.JSONDecodeError):
            continue
        if not isinstance(payload, dict):
            continue
        member = _member_from_payload(payload, evidence, classification_code=str(classification_code))
        if member is None:
            continue
        for group_key, item in specs.get(str(classification.code), []):
            key = (str(group_key), str(classification.code))
            members.setdefault(key, {}).setdefault((member["market"], member["symbol"]), member)


def _fetch_remote_industry_members(groups: list[dict[str, Any]],
                                   members: dict[tuple[str, str], dict[tuple[str, str], dict[str, Any]]],
                                   source_urls: dict[str, str],
                                   provider_counts: dict[str, int] | None = None, *, timeout: float = 4.0,
                                   total_budget_seconds: float = 7.0,
                                   minimum_members: int = 10) -> None:
    """Fill an absent industry universe through the public Eastmoney F10 API."""
    requests: list[tuple[str, str, str]] = []
    for group in groups:
        group_key = str(group.get("key") or "")
        for item in group.get("items") or []:
            dimension = str(item.get("classification_dimension") or "").upper()
            code = str(item.get("code") or "").strip()
            # A local evidence snapshot can be complete or partial.  During
            # an explicit refresh always probe the provider and merge any
            # newer/additional members instead of treating a non-empty local
            # bucket as proof that its universe is complete.
            if dimension != "INDUSTRY" or not code:
                continue
            if minimum_members > 0 and len(members.get((group_key, code), {})) >= minimum_members:
                continue
            try:
                level = min(max(int(item.get("level") or 1), 1), 3)
            except (TypeError, ValueError):
                level = 1
            requests.append((group_key, code, f"BOARD_CODE_BK_{level}LEVEL"))
    if not requests:
        return
    base = {
        "reportName": "RPT_F10_ORG_BASICINFO",
        "columns": "SECUCODE,SECURITY_CODE,SECURITY_NAME_ABBR",
        "pageSize": "500", "source": "F10", "client": "PC",
    }
    deadline = time.monotonic() + max(total_budget_seconds, 0.1)
    try:
        with httpx.Client(timeout=min(timeout, total_budget_seconds), follow_redirects=True, trust_env=False,
                          headers={"User-Agent": "Mozilla/5.0", "Referer": "https://emweb.securities.eastmoney.com/"}) as client:
            for group_key, code, field in requests:
                remaining = deadline - time.monotonic()
                if remaining <= 0:
                    break
                key = (group_key, code)
                page_number = 1
                provider_total: int | None = None
                while True:
                    remaining = deadline - time.monotonic()
                    if remaining <= 0:
                        break
                    params = {
                        **base,
                        "pageNumber": str(page_number),
                        "filter": f'({field}="{code}")',
                    }
                    try:
                        response = client.get(
                            _EASTMONEY_PROFILE_URL,
                            params=params,
                            timeout=max(min(timeout, remaining), 0.1),
                        )
                        response.raise_for_status()
                        result = (response.json() or {}).get("result") or {}
                        rows = result.get("data") or []
                        if provider_total is None:
                            try:
                                provider_total = int(result.get("count") or 0)
                            except (TypeError, ValueError):
                                provider_total = None
                        if provider_counts is not None and provider_total is not None:
                            provider_counts[code] = provider_total
                    except (httpx.HTTPError, ValueError, TypeError):
                        break
                    for payload in rows:
                        member = _member_from_payload(payload)
                        if member is not None:
                            members.setdefault(key, {}).setdefault((member["market"], member["symbol"]), member)
                    source_urls.setdefault(code, str(response.url))
                    if not rows or len(rows) < int(base["pageSize"]):
                        break
                    if provider_total is not None and page_number * int(base["pageSize"]) >= provider_total:
                        break
                    page_number += 1
    except httpx.HTTPError:
        return


def _is_supported_cn_a_board_symbol(symbol: str) -> bool:
    """Return whether a public board code belongs to the CN_A universe.

    Eastmoney's board endpoint can mix A shares, B shares and Beijing/NEEQ
    securities.  The application has no ``CN_B`` market identity, so those
    rows must not be silently relabelled as A shares.  They remain visible in
    the provider audit (via ``provider_total``), while the stock detail
    projection only publishes securities that can be resolved by the current
    master-data contract.
    """
    value = str(symbol or "").strip().zfill(6)
    return bool(re.fullmatch(r"(?:000|001|002|003|300|301|600|601|603|605|688|689)\d{3}", value))


def _fetch_remote_board_members(
    groups: list[dict[str, Any]],
    members: dict[tuple[str, str], dict[tuple[str, str], dict[str, Any]]],
    source_urls: dict[str, str],
    provider_counts: dict[str, int] | None = None,
    *,
    timeout: float = 4.0,
    total_budget_seconds: float = 12.0,
    max_requests: int = 12,
) -> None:
    """Refresh provider-backed BK industry/theme membership with pagination.

    ``RPT_F10_ORG_BASICINFO`` is the identity/industry verification source;
    this public board endpoint is the source closest to securities terminals
    for a board's complete member list and current percentage change.  The
    result is merged with immutable local evidence, never used to delete it.
    Unsupported B-share/NEEQ rows are counted by the provider but are not
    relabelled as A shares in the application projection.
    """
    targets: list[tuple[str, str]] = []
    seen: set[str] = set()
    for group in groups:
        group_key = str(group.get("key") or "")
        if group_key not in {"industry", "theme"}:
            continue
        for item in group.get("items") or []:
            code = str(item.get("code") or "").strip().upper()
            dimension = str(item.get("classification_dimension") or "").upper()
            if not code.startswith("BK") or dimension not in {"INDUSTRY", "THEME"} or code in seen:
                continue
            bucket = members.get((group_key, code), {})
            # Cached members with both a price and percentage change already
            # have a usable local snapshot.  Only incomplete groups trigger
            # the bounded public board request on ordinary page reads.
            if bucket and all(
                member.get("change_pct") is not None and member.get("current_price") is not None
                for member in bucket.values()
            ):
                continue
            seen.add(code)
            targets.append((group_key, code))
            if len(targets) >= max(max_requests, 1):
                break
        if len(targets) >= max(max_requests, 1):
            break
    if not targets:
        return

    deadline = time.monotonic() + max(total_budget_seconds, 0.1)
    params_base = {
        "po": "1", "np": "1", "ut": "bd1d9ddb04089700cf9c27f6f7426281",
        "fltt": "2", "invt": "2", "fid": "f3", "pz": "500",
        "fields": "f12,f14,f2,f3,f100,f102",
    }
    try:
        with httpx.Client(
            timeout=min(timeout, total_budget_seconds),
            follow_redirects=True,
            trust_env=False,
            headers={
                "User-Agent": "Mozilla/5.0",
                "Referer": "https://quote.eastmoney.com/",
            },
        ) as client:
            for group_key, code in targets:
                page = 1
                provider_total: int | None = None
                while True:
                    remaining = deadline - time.monotonic()
                    if remaining <= 0:
                        return
                    params = {**params_base, "pn": str(page), "fs": f"b:{code}"}
                    try:
                        response = client.get(
                            _EASTMONEY_BOARD_MEMBERS_URL,
                            params=params,
                            timeout=max(min(timeout, remaining), 0.1),
                        )
                        response.raise_for_status()
                        data = (response.json() or {}).get("data") or {}
                        rows = data.get("diff") or []
                        if isinstance(rows, dict):
                            rows = list(rows.values())
                        if provider_total is None:
                            try:
                                provider_total = int(data.get("total") or 0)
                            except (TypeError, ValueError):
                                provider_total = None
                        if provider_counts is not None and provider_total is not None:
                            provider_counts[code] = provider_total
                    except (httpx.HTTPError, ValueError, TypeError):
                        break
                    source_url = str(response.url)
                    source_urls.setdefault(code, source_url)
                    bucket = members.setdefault((group_key, code), {})
                    for row in rows:
                        if not isinstance(row, dict):
                            continue
                        raw_symbol = str(row.get("f12") or "").strip()
                        if not raw_symbol or not _is_supported_cn_a_board_symbol(raw_symbol):
                            continue
                        symbol = raw_symbol.zfill(6)
                        raw_price = row.get("f2")
                        raw_change = row.get("f3")
                        try:
                            current_price = float(raw_price) if raw_price not in (None, "", "-") else None
                        except (TypeError, ValueError):
                            current_price = None
                        try:
                            change_pct = float(raw_change) if raw_change not in (None, "", "-") else None
                        except (TypeError, ValueError):
                            change_pct = None
                        member = {
                            "market": "CN_A",
                            "symbol": symbol,
                            "name": str(row.get("f14") or symbol).strip(),
                            "classification_code": code,
                            "current_price": current_price,
                            "change_pct": change_pct,
                            "quote_status": "OBSERVED_PROVIDER",
                            "quote_time": None,
                            "source_name": "东方财富板块行情接口",
                            "source_url": source_url,
                            "provider_total": provider_total,
                            "provider_rank": (page - 1) * 500 + len(bucket) + 1,
                        }
                        existing = bucket.get(("CN_A", symbol))
                        if existing is None:
                            bucket[("CN_A", symbol)] = member
                        else:
                            # Keep the evidence-backed identity and merge the
                            # newer provider quote/provenance fields into it.
                            existing.update({
                                key: value for key, value in member.items()
                                if value not in (None, "")
                            })
                    if not rows or len(rows) < 500:
                        break
                    if provider_total is not None and page * 500 >= provider_total:
                        break
                    page += 1
    except httpx.HTTPError:
        return


def _fetch_tencent_quotes(
    pairs: list[tuple[str, str]],
    *,
    timeout: float = 8.0,
    batch_size: int = 60,
    retries: int = 2,
    total_budget_seconds: float = 10.0,
) -> tuple[dict[tuple[str, str], QuoteRecord], str | None]:
    """Read complete quotes in bounded batches with per-batch retry."""
    result: dict[tuple[str, str], QuoteRecord] = {}
    if not pairs:
        return result, None
    normalized_pairs = list(dict.fromkeys(
        (market, symbol) for market, symbol in pairs if market in {"CN_A", "HK"}
    ))
    codes = [AkshareAdapter._tencent_code(market, symbol) for market, symbol in normalized_pairs]
    any_response = False
    deadline = time.monotonic() + max(total_budget_seconds, 0.1)
    try:
        parser = AkshareAdapter()
        with httpx.Client(
            timeout=min(timeout, total_budget_seconds),
            trust_env=False,
            headers={"User-Agent": "Mozilla/5.0"},
        ) as client:
            for start in range(0, len(codes), max(batch_size, 1)):
                if time.monotonic() >= deadline:
                    break
                batch = codes[start:start + max(batch_size, 1)]
                if not batch:
                    continue
                url = _TENCENT_QUOTE_URL + ",".join(batch)
                response = None
                for _attempt in range(max(retries, 1)):
                    remaining = deadline - time.monotonic()
                    if remaining <= 0:
                        break
                    try:
                        try:
                            response = client.get(url, timeout=max(min(timeout, remaining), 0.1))
                        except TypeError:  # lightweight test clients may not accept timeout=
                            response = client.get(url)
                        response.raise_for_status()
                        break
                    except httpx.HTTPError:
                        response = None
                if response is None:
                    continue
                response.encoding = "gb18030"
                any_response = True
                for line in response.text.split(";"):
                    match = re.search(r"v_([a-z]{2})(\d+)=\"([^\"]*)\"", line)
                    if not match:
                        continue
                    prefix, symbol, raw = match.groups()
                    market = "HK" if prefix == "hk" else "CN_A"
                    normalized = symbol.zfill(5 if market == "HK" else 6)
                    quote = parser._parse_tencent_quote(
                        f'v_{prefix}{symbol}="{raw}";', market, normalized
                    )
                    if quote.current_price is not None or quote.change_pct is not None:
                        result[(market, normalized)] = quote
        return result, _TENCENT_QUOTE_URL if any_response else None
    except (httpx.HTTPError, ValueError, TypeError):
        return result, None


def _fetch_tencent_changes(
    pairs: list[tuple[str, str]],
    *,
    timeout: float = 8.0,
) -> tuple[dict[tuple[str, str], float], str | None]:
    """Compatibility projection for callers that only need percent changes."""
    quotes, source_url = _fetch_tencent_quotes(pairs, timeout=timeout)
    return {
        key: float(quote.change_pct)
        for key, quote in quotes.items()
        if quote.change_pct is not None
    }, source_url


def _persist_classification_quotes(
    db: Session,
    quotes: dict[tuple[str, str], QuoteRecord],
) -> set[tuple[str, str]]:
    if not quotes:
        return set()
    try:
        source = db.scalar(
            select(DataSource)
            .where(
                DataSource.source_code == "TENCENT_QUOTE",
                DataSource.enabled.is_(True),
            )
            .limit(1)
        )
        if source is None:
            source = DataSource(
                source_code="TENCENT_QUOTE",
                source_name="腾讯证券实时行情",
                source_type="MARKET_DATA",
                adapter_type="AKSHARE",
                priority=140,
                enabled=True,
                config_json={
                    "provider": "TENCENT",
                    "capabilities": ["QUOTE"],
                    "official_url": _TENCENT_QUOTE_URL,
                },
                description="腾讯证券公开实时行情接口，用于分类成分股当日涨跌幅补充。",
            )
            db.add(source)
            db.flush()
        service = StockOnDemandService(db)
        persisted: set[tuple[str, str]] = set()
        for key, quote in quotes.items():
            service._upsert_quote(source.id, quote)
            persisted.add(key)
        db.commit()
        return persisted
    except Exception:
        db.rollback()
        return set()


def enrich_classification_groups_with_members(
    db: Session,
    groups: list[dict[str, Any]],
    *,
    limit_per_group: int | None = None,
    fetch_remote: bool = False,
    refresh_quotes: bool = False,
    include_members: bool = True,
) -> list[dict[str, Any]]:
    """Attach source-backed member stocks, counts and observed group trends.

    Membership comes from immutable EASTMONEY profile evidence first.  During
    a remote F10 refresh an absent industry universe is filled from the same
    provider's public F10 query, and current changes are read in one bounded
    Tencent batch.  A missing remote response remains explicitly unavailable;
    it is never replaced with a fabricated zero or inferred trend.
    """
    if not groups:
        return groups
    members: dict[tuple[str, str], dict[tuple[str, str], dict[str, Any]]] = {}
    source_urls: dict[str, str] = {}
    provider_counts: dict[str, int] = {}
    _load_reviewed_members(db, groups, members)
    _load_foundation_members(db, groups, members, source_urls)
    if fetch_remote:
        _fetch_remote_industry_members(
            groups,
            members,
            source_urls,
            provider_counts,
            minimum_members=0 if refresh_quotes else 10,
        )
        _fetch_remote_board_members(groups, members, source_urls, provider_counts)

    def _limited_pairs(bucket: dict[tuple[str, str], dict[str, Any]]) -> list[tuple[str, str]]:
        pairs = list(bucket)
        if limit_per_group is not None and limit_per_group > 0:
            return pairs[:limit_per_group]
        return pairs

    pairs = list(dict.fromkeys(
        pair
        for bucket in members.values()
        for pair in _limited_pairs(bucket)
    ))
    quote_map: dict[tuple[str, str], float] = {}
    price_map: dict[tuple[str, str], float] = {}
    quote_times: dict[tuple[str, str], str] = {}
    quote_statuses: dict[tuple[str, str], str] = {}
    trend_url: str | None = None
    try:
        if pairs:
            # Do not use independent ``market IN`` and ``symbol IN`` filters:
            # they can cross-match an HK code with a CN_A market row.  The
            # composite predicate keeps identity stable for every member.
            quotes = db.scalars(select(StockRealtimeQuote).where(
                tuple_(StockRealtimeQuote.market, StockRealtimeQuote.symbol).in_(pairs),
            )).all()
            for quote in quotes:
                key = (quote.market, quote.symbol)
                if quote.current_price is not None:
                    price_map[key] = float(quote.current_price)
                if quote.change_pct is not None:
                    quote_map[key] = float(quote.change_pct)
                    quote_statuses[key] = "CACHED"
                else:
                    quote_statuses[key] = "CACHED_NO_CHANGE_PCT"
                observed_at = quote.quote_time or (
                    quote.fetched_at.isoformat() if quote.fetched_at else None
                )
                if observed_at:
                    quote_times[key] = observed_at
    except Exception:
        quote_map = {}
        quote_times = {}
        quote_statuses = {}
    if fetch_remote and pairs:
        remote_pairs = pairs if refresh_quotes else [
            pair for pair in pairs if pair not in quote_map
        ]
        remote_quotes, trend_url = _fetch_tencent_quotes(remote_pairs)
        persisted_keys = _persist_classification_quotes(db, remote_quotes)
        for key, quote in remote_quotes.items():
            cached_time = quote_times.get(key)
            incoming_time = quote.quote_time
            cached_timestamp = _parse_timestamp(cached_time)
            incoming_timestamp = _parse_timestamp(incoming_time)
            if cached_timestamp and incoming_timestamp and incoming_timestamp < cached_timestamp:
                continue
            if cached_time and not incoming_time and key in quote_map:
                continue
            if quote.change_pct is not None:
                quote_map[key] = float(quote.change_pct)
                quote_statuses[key] = (
                    "OBSERVED_PERSISTED" if key in persisted_keys else "REMOTE_NOT_PERSISTED"
                )
            else:
                quote_statuses[key] = (
                    "OBSERVED_NO_CHANGE_PCT_PERSISTED"
                    if key in persisted_keys else "REMOTE_NOT_PERSISTED"
                )
            if quote.quote_time:
                quote_times[key] = quote.quote_time
            if quote.current_price is not None:
                price_map[key] = float(quote.current_price)

    for group in groups:
        group_key = str(group.get("key") or "")
        for item in group.get("items") or []:
            code = str(item.get("code") or "").strip()
            bucket_map = members.get((group_key, code), {}) if code else {}
            for member in bucket_map.values():
                pair = (member["market"], member["symbol"])
                if pair in price_map:
                    member["current_price"] = price_map[pair]
                if pair in quote_map:
                    member["change_pct"] = quote_map[pair]
                elif "change_pct" not in member:
                    member["change_pct"] = None
                if pair in quote_times:
                    member["quote_time"] = quote_times[pair]
                else:
                    member.setdefault("quote_time", None)
                if pair in quote_statuses:
                    member["quote_status"] = quote_statuses[pair]
                else:
                    member.setdefault("quote_status", "UNAVAILABLE")
            bucket = list(bucket_map.values())
            if limit_per_group is not None and limit_per_group > 0:
                bucket = bucket[:limit_per_group]
            if any(member.get("current_price") is not None for member in bucket):
                bucket.sort(key=lambda member: (
                    member.get("current_price") is None,
                    -(float(member.get("current_price")) if member.get("current_price") is not None else 0.0),
                    str(member.get("symbol") or ""),
                ))
            preview = bucket if include_members else bucket[:5]
            item["related_stocks"] = preview
            item["members_deferred"] = len(preview) < len(bucket)
            item["group_key"] = group_key
            item["member_count"] = len(bucket_map)
            item["returned_count"] = len(preview)
            item["member_truncated"] = len(preview) < len(bucket_map)
            provider_total = provider_counts.get(code)
            if provider_total is not None:
                item["provider_member_count"] = provider_total
                item["provider_missing_count"] = max(provider_total - len(bucket_map), 0)
            changes = [member["change_pct"] for member in bucket if member.get("change_pct") is not None]
            item["quote_observed_count"] = len(changes)
            item["quote_unavailable_count"] = max(len(bucket) - len(changes), 0)
            item["quote_coverage_ratio"] = round(len(changes) / len(bucket), 6) if bucket else None
            item["trend_pct"] = round(sum(changes) / len(changes), 2) if changes else None
            if not item.get("source_url"):
                item["source_url"] = _classification_source_url(
                    code, str(item.get("classification_dimension") or ""), item.get("level")
                ) or source_urls.get(code)
            observed_dates = [str(member.get("as_of")) for member in bucket_map.values() if member.get("as_of")]
            if observed_dates:
                # A classification is a point-in-time snapshot; expose the
                # latest observed evidence boundary while retaining each
                # member's own ``as_of`` for audit detail.
                item["as_of"] = max(observed_dates)
            item["classification_as_of"] = item.get("as_of")
            item["freshness_status"] = _freshness_status(item.get("classification_as_of"))
            quote_dates = [str(member.get("quote_time")) for member in bucket if member.get("quote_time")]
            item["quote_as_of"] = max(quote_dates) if quote_dates else None
            item["quote_freshness_status"] = _freshness_status(item.get("quote_as_of"), max_age_days=1)
            item["trend_source_url"] = trend_url
            item["trend_status"] = "OBSERVED" if changes else "UNAVAILABLE"
            item["member_status"] = "OBSERVED" if bucket_map else "UNAVAILABLE"
    return groups


def _definition_lookup(db: Session | None) -> dict[tuple[str, str], ClassificationDefinition]:
    if db is None:
        return {}
    try:
        rows = list(db.scalars(select(ClassificationDefinition).where(ClassificationDefinition.status == "ACTIVE")))
    except Exception:
        return {}
    result: dict[tuple[str, str], ClassificationDefinition] = {}
    for row in rows:
        result.setdefault((str(row.dimension).upper(), str(row.code).upper()), row)
    return result


def _definition_for(
    definitions: dict[tuple[str, str], ClassificationDefinition],
    dimension: str,
    code: Any = None,
    label: Any = None,
    level: Any = None,
) -> ClassificationDefinition | dict[str, Any] | None:
    normalized_dimension = str(dimension or "").upper()
    normalized_code = str(code or "").upper()
    label_master = label_definition(label, normalized_dimension)
    if label_master and str(label_master.get("dimension") or "").upper() == normalized_dimension:
        return label_master
    # Concrete definitions (for example SIZE/LARGE_CAP or
    # LEGAL_LISTING_CLASS/A_SHARE) are more precise than the dimension-level
    # fallback.  Prefer them whenever the fact carries a code.
    if normalized_code:
        exact = definitions.get((normalized_dimension, normalized_code))
        if exact is not None:
            return exact
    # ``BOARD_NAME_*LEVEL`` is an Eastmoney industry hierarchy, even though
    # the upstream field name contains BOARD.  Resolve it to a concrete
    # industry explanation before falling back to the dimension-level glossary
    # (which describes an exchange listing board and would be misleading).
    provider_definition = _provider_industry_definition(label, normalized_code, level)
    if provider_definition and normalized_dimension == "INDUSTRY":
        return provider_definition
    candidates = {
        "INDUSTRY": ("EASTMONEY_LEVEL1", "CSRC_LEVEL1"),
        "THEME": ("PROVIDER_CONCEPT",),
        "INDEX": ("INDEX_MEMBERSHIP", "PROVIDER_CONCEPT"),
        "BOARD": ("LISTED_BOARD", "EASTMONEY_LEVEL1"),
        "TYPE": ("SECURITY_TYPE", "A_SHARE", "H_SHARE"),
    }.get(normalized_dimension, ())
    return next((definitions.get((normalized_dimension, candidate)) for candidate in candidates
                 if definitions.get((normalized_dimension, candidate))), None)


def _definition_value(definition: ClassificationDefinition | dict[str, Any], key: str) -> Any:
    if isinstance(definition, dict):
        return definition.get(key)
    return getattr(definition, key, None)


def _attach_definition(
    item: dict[str, Any], definitions: dict[tuple[str, str], ClassificationDefinition], dimension: str,
) -> dict[str, Any]:
    """Attach glossary metadata while preserving a fact's precise definition.

    ``SecurityClassification.properties_json`` may contain a definition for a
    concrete provider label (for example a particular theme).  The built-in
    dimension definition explains only the taxonomy as a whole and must not
    overwrite that more specific fact.  Keep both values explicit so callers
    can show the precise definition first and the master-data explanation as a
    fallback.
    """
    label_master = label_definition(item.get("label") or item.get("name"), dimension)
    if label_master and str(label_master.get("dimension") or "").upper() != str(dimension or "").upper():
        label_master = None
    definition = label_master or _definition_for(
        definitions,
        dimension,
        item.get("code"),
        item.get("label") or item.get("name"),
        item.get("level"),
    )
    if definition is not None:
        # Preserve concrete fact-level values.  The ``master_*`` fields are
        # intentionally separate and versioned, making provenance clear to
        # both the UI and downstream model consumers.
        concrete_definition = item.get("definition")
        master_definition = _definition_value(definition, "definition")
        if label_master is not None:
            # The label-level explanation is user-facing. Preserve provider
            # wording separately so the observed fact remains auditable.
            if concrete_definition not in (None, "") and concrete_definition != master_definition:
                item.setdefault("fact_definition", concrete_definition)
            item["definition"] = master_definition
        elif concrete_definition in (None, ""):
            item["definition"] = master_definition
        elif concrete_definition != master_definition:
            item.setdefault("fact_definition", concrete_definition)
        concrete_criteria = item.get("criteria")
        master_criteria = _definition_value(definition, "criteria")
        if concrete_criteria in (None, ""):
            item["criteria"] = master_criteria
        elif concrete_criteria != master_criteria:
            item.setdefault("fact_criteria", concrete_criteria)
        if item.get("source_name") in (None, ""):
            item["source_name"] = _definition_value(definition, "source_name")
        item.setdefault("source_url", _definition_value(definition, "source_url"))
        definition_version = _definition_value(definition, "definition_version")
        if label_master is not None:
            existing_version = item.get("definition_version")
            if existing_version not in (None, "", definition_version):
                item.setdefault("fact_definition_version", existing_version)
            item["definition_version"] = definition_version
        else:
            item.setdefault("definition_version", definition_version)
        item["master_definition"] = master_definition
        item["master_criteria"] = master_criteria
        item["master_source_name"] = _definition_value(definition, "source_name")
        item["master_source_url"] = _definition_value(definition, "source_url")
        item["master_definition_version"] = definition_version
        # ``dimension`` is the master-data dimension for legacy callers.  A
        # typed fact can retain its original dimension separately so SIZE,
        # STYLE and LEGAL_LISTING_CLASS remain auditable inside the combined
        # display group.
        fact_dimension = item.get("classification_dimension")
        item["dimension"] = str(fact_dimension or _definition_value(definition, "dimension"))
        item["master_dimension"] = _definition_value(definition, "dimension")
        item["taxonomy"] = _definition_value(definition, "taxonomy")
        item["definition_source"] = _definition_value(definition, "source_name")
        item["definition_status"] = (
            "SOURCE_DERIVED"
            if str(definition_version or "").startswith("EASTMONEY_INDUSTRY_LABEL")
            else "MASTER_DATA"
        )
    return item


def _foundation_classifications(
    db: Session,
    stock: StockSymbol,
) -> tuple[
    list[dict[str, Any]],  # industries
    list[dict[str, Any]],  # themes
    list[dict[str, Any]],  # indexes
    list[dict[str, Any]],  # listed boards
    list[dict[str, Any]],  # security/type classifications
]:
    """Read provider classifications already imported into the company graph.

    Some older test databases do not contain foundation tables.  In that case
    the function simply returns empty lists and the F10 provider fields remain
    the source of truth.
    """
    try:
        tables = set(inspect(db.connection()).get_table_names())
        required = {FoundationListing.__tablename__, FoundationSecurity.__tablename__,
                    FoundationEvidence.__tablename__, SecurityClassification.__tablename__}
        if not required.issubset(tables):
            return [], [], [], [], []
        listing = db.scalar(select(FoundationListing).where(FoundationListing.stock_symbol_id == stock.id))
        if listing is None:
            return [], [], [], [], []
        rows = list(db.scalars(select(SecurityClassification).where(
            SecurityClassification.security_id == listing.security_id,
            SecurityClassification.status.in_(("ACCEPTED", "PENDING")),
        ).order_by(SecurityClassification.created_at.desc())))
        industries: list[dict[str, Any]] = []
        themes: list[dict[str, Any]] = []
        indexes: list[dict[str, Any]] = []
        boards: list[dict[str, Any]] = []
        type_values: list[dict[str, Any]] = []
        for row in rows:
            dimension = str(row.dimension or "").upper()
            props = row.properties_json if isinstance(row.properties_json, dict) else {}
            # Older governance runs stored the provider's BK industry
            # hierarchy as BOARD because the source column is named
            # ``BOARD_NAME_*LEVEL``.  Re-project those facts as INDUSTRY while
            # retaining the original dimension for auditability.
            legacy_industry_board = _is_legacy_industry_board(dimension, row.code, props)
            if dimension == "INDUSTRY" or legacy_industry_board:
                target = industries
            elif dimension == "THEME":
                target = themes
            elif dimension == "INDEX":
                target = indexes
            elif dimension == "BOARD":
                target = boards
            elif dimension in {"SIZE", "STYLE", "QUALITY", "LEGAL_LISTING_CLASS", "LIQUIDITY", "RISK", "TYPE"}:
                target = type_values
            else:
                target = None
            if target is None:
                continue
            value = _item(
                row.label,
                code=row.code,
                source=("东方财富行业板块（原上市板块字段）" if legacy_industry_board else "公司/证券分类主数据"),
                status=row.status,
                definition=props.get("definition"),
                criteria=props.get("criteria"),
                source_name=props.get("source_name"),
                source_url=props.get("source_url"),
                definition_version=row.definition_version,
                level=props.get("level"),
                classification_dimension=("INDUSTRY" if legacy_industry_board else dimension),
            )
            if value:
                if legacy_industry_board:
                    value["provider_dimension"] = "BOARD"
                target.append(value)
        # The imported evidence contains the complete Eastmoney industry/theme
        # hierarchy even when classifications were not reviewed yet.  The
        # BOARD_NAME fields are industry levels, not exchange listing boards.
        evidence = db.get(FoundationEvidence, listing.evidence_id)
        raw = json.loads(evidence.content) if evidence and evidence.content else {}
        if isinstance(raw, dict):
            for level in (1, 2, 3):
                label, code = raw.get(f"BOARD_NAME_{level}LEVEL"), raw.get(f"BOARD_CODE_BK_{level}LEVEL")
                value = _item(
                    label,
                    code=code,
                    source="东方财富行业板块主数据",
                    status="SOURCE",
                    level=level,
                    classification_dimension="INDUSTRY",
                )
                if value:
                    industries.append(value)
            labels = _split(raw.get("BLGAINIAN"))
            codes = _split(raw.get("BLGAINIAN_CODE"))
            for index, label in enumerate(labels):
                value = _item(label, code=codes[index] if index < len(codes) else None,
                              source="东方财富主题板块", status="SOURCE")
                if value:
                    (indexes if _looks_like_index(label, value.get("code")) else themes).append(value)
        return industries, themes, indexes, boards, type_values
    except (ValueError, TypeError, json.JSONDecodeError):
        return [], [], [], [], []


def build_classification_groups(db: Session | None, stock: StockSymbol,
                                profile_fields: dict[str, Any] | None = None) -> list[dict[str, Any]]:
    fields = profile_fields if isinstance(profile_fields, dict) else {}
    groups: list[dict[str, Any]] = []
    definitions = _definition_lookup(db)
    industry_values, theme_values, index_values, board_values, type_values = (
        _foundation_classifications(db, stock) if db is not None else ([], [], [], [], [])
    )

    if not industry_values:
        for index, value in enumerate(_clean_industry(fields.get("所属行业") or fields.get("行业"))):
            item = _item(value, source="公司资料 / F10", status="SOURCE", level=index + 1,
                         classification_dimension="INDUSTRY")
            if item:
                industry_values.append(item)
    industry_values = [_attach_definition(item, definitions, "INDUSTRY") for item in industry_values]
    if industry_values:
        # Keep only the hierarchy labels in the industry group; themes are
        # displayed separately below and indexes never enter either group.
        groups.append(_group("industry", "所属行业", industry_values))

    board_values = board_values or []
    board_values.extend(
        _item(value, source="证券主数据 / F10", status="SOURCE", classification_dimension="BOARD")
        for value in _split(fields.get("板块") or fields.get("上市板块") or fields.get("所属市场"))
    )
    board_items = [item for item in board_values if item]
    board_items = [item for item in board_items if item]
    if board_items:
        groups.append(_group("board", "上市板块", board_items))

    if not theme_values:
        # ``所属概念`` is accepted only as a theme fallback.  ``入选指数`` is
        # intentionally not read here, since an index is its own dimension.
        for value in _split(fields.get("所属概念") or fields.get("概念")):
            item = _item(value, source="F10 概念字段", status="SOURCE", classification_dimension="THEME")
            if item:
                theme_values.append(item)
    theme_values = [_attach_definition(item, definitions, "THEME") for item in theme_values if not _looks_like_index(item.get("label"), item.get("code"))]
    if theme_values:
        groups.append(_group("theme", "主题板块", theme_values))

    index_items = [_attach_definition(item, definitions, "INDEX") for item in index_values]
    index_items.extend(_attach_definition(_item(value, source="F10 入选指数", status="SOURCE", classification_dimension="INDEX"), definitions, "INDEX") for value in _split(fields.get("入选指数")))
    index_items = [item for item in index_items if item]
    if index_items:
        groups.append(_group("index", "入选指数", index_items))

    type_items: list[dict[str, Any]] = list(type_values)
    if stock.asset_type:
        item = _attach_definition(_item(stock.asset_type, source="证券主数据", status="SOURCE",
                                        classification_dimension="TYPE"), definitions, "TYPE")
        if item:
            type_items.append(item)
    type_items = [_attach_definition(item, definitions, item.get("classification_dimension") or "TYPE")
                  for item in type_items if item]
    if type_items:
        groups.append(_group("type", "证券类型", type_items))
    # Enrich every emitted label with the versioned glossary.  Applying this
    # at the final projection boundary also covers legacy foundation facts and
    # avoids mutating their source payloads.
    for group in groups:
        for item in group.get("items", []):
            # A combined display group (notably ``type``) can contain SIZE,
            # STYLE, LEGAL_LISTING_CLASS, etc. facts.  Resolve the glossary
            # using each fact's original dimension instead of overwriting its
            # master metadata with the UI group dimension.
            dimension = str(item.get("classification_dimension") or "").upper()
            if not dimension:
                dimension = {"industry": "INDUSTRY", "board": "BOARD", "theme": "THEME",
                             "index": "INDEX", "type": "TYPE"}.get(str(group.get("key")), "")
            if dimension:
                _attach_definition(item, definitions, dimension)
    return [item for item in groups if item]
