"""Conservative issuer linkage; search hits are evidence, not identity proof."""
from __future__ import annotations

import re
import unicodedata
from functools import lru_cache
from typing import Any


@lru_cache(maxsize=1)
def _script_converter():
    from opencc import OpenCC
    return OpenCC("t2s")


def normalized_issuer_text(value: str) -> str:
    """Compare scripts/width consistently while retaining source text separately."""
    return re.sub(r"\s+", "", _script_converter().convert(unicodedata.normalize("NFKC", value or ""))).casefold()


def issuer_names(name: str, ext: dict | None = None) -> list[str]:
    names = [str(name or "").strip()]
    ext = ext or {}
    for key in ("company_name", "公司名称", "公司全称", "中文名称", "issuer_name"):
        value = ext.get(key)
        if isinstance(value, str):
            names.append(value.strip())
    base = re.sub(r"^(?:\*?ST)", "", names[0], flags=re.I)
    base = re.sub(r"[-－](?:SW|W|S|B|R)$", "", base, flags=re.I)
    names.append(base)
    return list(dict.fromkeys(re.sub(r"\s+", "", unicodedata.normalize("NFKC", n)) for n in names if len(n) >= 2))


def check_news_identity(market: str, symbol: str, name: str, title: str,
                        content: str | None = None, ext: dict | None = None) -> dict[str, Any]:
    text = normalized_issuer_text(f"{title or ''} {content or ''}")
    matched = [n for n in issuer_names(name, ext) if normalized_issuer_text(n) in text]
    # A bare six/five-digit search term also matches indices and other markets.
    prefixes = {"CN_A": ("SH", "SZ", "BJ"), "HK": ("HK",),
                "NEEQ": ("NEEQ", "OC"), "NEEQ_INNOVATION": ("NEEQ", "OC")}.get(market, ())
    code_match = any(re.search(rf"(?<![a-z0-9]){p.lower()}[.:]?{re.escape(symbol)}(?!\d)", text)
                     or re.search(rf"(?<!\d){re.escape(symbol)}\.{p.lower()}(?![a-z])", text)
                     for p in prefixes)
    status = "MENTION_MATCHED" if matched or code_match else "CANDIDATE"
    return {"status": status, "method": "NAME_MENTION" if matched else "MARKET_CODE" if code_match else "SEARCH_ONLY",
            "normalization": "NFKC_WHITESPACE_OPENCC_T2S",
            "market": market, "symbol": symbol, "matched_names": matched,
            "message": "原文提及主体；事件内容仍需另行验证" if status == "MENTION_MATCHED"
                       else "仅搜索命中，尚无原文主体匹配证据"}


def partition_news(rows: list, stock: Any) -> tuple[list, list]:
    matched, candidates = [], []
    for row in rows:
        result = check_news_identity(row.market, row.symbol, getattr(stock, "name", ""),
                                     row.title, row.content, getattr(stock, "ext_json", {}))
        (matched if result["status"] == "MENTION_MATCHED" else candidates).append(row)
    return matched, candidates
