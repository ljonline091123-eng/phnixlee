"""Canonical, source-aware read model for company announcements.

The storage table intentionally keeps one row per provider (the source id is
part of its uniqueness constraint).  A stock page, however, should not show
the same filing twice merely because it was returned by two providers.  This
module contains the small, transport-independent projection used by the API
and the F10 repository to collapse those provider variants while retaining
their provenance.
"""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
import re
import unicodedata
from typing import Any, Iterable
from urllib.parse import parse_qsl, urlencode, urlsplit, urlunsplit


def _value(row: Any, name: str, default: Any = None) -> Any:
    if isinstance(row, dict):
        return row.get(name, default)
    return getattr(row, name, default)


def normalize_notice_title(value: Any) -> str:
    """Normalize harmless provider formatting differences in a title."""

    text = unicodedata.normalize("NFKC", str(value or ""))
    text = re.sub(r"\s+", "", text)
    # Source feeds occasionally use different full-width punctuation or a
    # trailing period.  Keep semantic punctuation, but normalize its forms.
    text = text.replace("．", ".").replace("：", ":").replace("，", ",")
    text = text.replace("（", "(").replace("）", ")")
    return text.strip(" \t\r\n。．")


def normalize_notice_url(value: Any) -> str:
    """Return a stable URL for provenance comparison.

    Tracking query parameters are ignored, while the actual document id and
    path are preserved.  An empty URL is deliberately represented as an empty
    string so title/date remain the canonical fallback.
    """

    text = str(value or "").strip()
    if text.lower() in {"", "none", "null", "nan"}:
        return ""
    try:
        parts = urlsplit(text)
        if not parts.scheme or not parts.netloc:
            return text.rstrip("/")
        query = sorted(
            (key, val)
            for key, val in parse_qsl(parts.query, keep_blank_values=True)
            if key.lower() not in {"utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content"}
        )
        return urlunsplit((parts.scheme.lower(), parts.netloc.lower(), parts.path.rstrip("/"), urlencode(query), ""))
    except ValueError:
        return text.rstrip("/")


def notice_source_name(row: Any) -> str:
    """Name the actual disclosure provider, not a numeric source count."""
    host = urlsplit(normalize_notice_url(_value(row, "url", ""))).hostname or ""
    for domain, label in (
        ("hkexnews.hk", "港交所披露易"), ("cninfo.com.cn", "巨潮资讯"),
        ("sse.com.cn", "上海证券交易所"), ("szse.cn", "深圳证券交易所"),
        ("bse.cn", "北京证券交易所"), ("neeq.com.cn", "全国股转系统"),
        ("eastmoney.com", "东方财富"),
    ):
        if host == domain or host.endswith("." + domain):
            return label
    content = _value(row, "content_json", {})
    if isinstance(content, dict):
        label = content.get("source_name") or content.get("来源")
        if isinstance(label, str) and label.strip():
            return label.strip()
    return "公告披露源"


def canonical_notice_key(row: Any) -> tuple[str, str, str, str]:
    """Build a stable key for a notice row.

    Date + normalized title is the cross-source identity.  URL is included as
    a fallback for rows without a usable title and is exposed in the key so a
    caller can audit exactly which URL variant was observed.
    """

    market = str(_value(row, "market", "") or "").strip().upper()
    symbol = str(_value(row, "symbol", "") or "").strip().upper()
    notice_date = str(_value(row, "notice_date", "") or "").strip()[:10]
    title = normalize_notice_title(_value(row, "title", ""))
    url = normalize_notice_url(_value(row, "url", ""))
    identity = title or url
    # A title is the preferred cross-provider identity.  For malformed rows
    # with no title, the URL still prevents unrelated rows collapsing.
    return market, symbol, notice_date, identity


def _completeness(row: Any) -> tuple[int, int, float, int]:
    content = _value(row, "content_json", {})
    content_size = len(str(content or ""))
    url_score = 1 if normalize_notice_url(_value(row, "url", "")) else 0
    fetched = _value(row, "fetched_at")
    if isinstance(fetched, datetime):
        timestamp = fetched.timestamp()
    else:
        try:
            timestamp = datetime.fromisoformat(str(fetched).replace("Z", "+00:00")).timestamp()
        except (TypeError, ValueError, OverflowError):
            timestamp = 0.0
    try:
        row_id = int(_value(row, "id", 0) or 0)
    except (TypeError, ValueError):
        row_id = 0
    return url_score, content_size, timestamp, -row_id


@dataclass(frozen=True, slots=True)
class CanonicalNotice:
    """One display row plus all provider provenance for that row."""

    row: Any
    key: tuple[str, str, str, str]
    source_ids: tuple[int, ...]
    source_urls: tuple[str, ...]
    duplicate_count: int

    @property
    def source_count(self) -> int:
        return len(self.source_ids)

    def metadata(self) -> dict[str, Any]:
        return {
            "canonical_key": ":".join(self.key),
            "source_ids": list(self.source_ids),
            "source_urls": list(self.source_urls),
            "source_count": self.source_count,
            "duplicate_count": self.duplicate_count,
        }


def deduplicate_notice_rows(rows: Iterable[Any]) -> list[CanonicalNotice]:
    """Collapse cross-source variants and retain the best representative.

    The returned order is newest notice date first, then stable row id.  This
    makes pagination deterministic and ensures the latest marker is computed
    on the canonical set rather than on provider duplicates.
    """

    groups: dict[tuple[str, str, str, str], list[Any]] = {}
    for row in rows:
        key = canonical_notice_key(row)
        if not key[0] or not key[1] or not key[2] or not key[3]:
            # Keep malformed rows isolated instead of silently dropping them.
            key = (*key[:3], f"__row__{_value(row, 'id', '')}")
        groups.setdefault(key, []).append(row)

    result: list[CanonicalNotice] = []
    for key, variants in groups.items():
        representative = max(variants, key=_completeness)
        source_ids = sorted({int(_value(item, "source_id", 0) or 0) for item in variants})
        source_urls = sorted({normalize_notice_url(_value(item, "url", "")) for item in variants if normalize_notice_url(_value(item, "url", ""))})
        result.append(CanonicalNotice(
            row=representative,
            key=key,
            source_ids=tuple(source_ids),
            source_urls=tuple(source_urls),
            duplicate_count=max(0, len(variants) - 1),
        ))

    def order(item: CanonicalNotice) -> tuple[str, int]:
        date = str(_value(item.row, "notice_date", "") or "")
        try:
            row_id = int(_value(item.row, "id", 0) or 0)
        except (TypeError, ValueError):
            row_id = 0
        return date, row_id

    return sorted(result, key=order, reverse=True)

