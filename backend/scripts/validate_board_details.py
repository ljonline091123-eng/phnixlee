"""Read-only 160-stock acceptance: independent quotes and every F10 sub-panel."""
from __future__ import annotations

import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import sqlite3
import sys
import time

import httpx

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from app.services.f10 import _section_has_payload, _has_observation_rows
from app.services.news_identity import check_news_identity

OUT = ROOT / "validation" / "all-boards-20261008"
API_BASE = os.environ.get("QUANT_AUDIT_API_URL", "http://127.0.0.1:8000").rstrip("/")


def save(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2, default=str), encoding="utf-8")


def n(value):
    try:
        return float(value)
    except (ValueError, TypeError):
        return None


def balance_checks(statement):
    """Check actual period row layouts, without treating absent totals as zero."""
    aliases = {
        "SUMASSET": "assets", "TOTAL_ASSETS": "assets", "资产合计": "assets", "总资产": "assets",
        "SUMLIAB": "liabilities", "TOTAL_LIABILITIES": "liabilities", "负债合计": "liabilities", "总负债": "liabilities",
        "SUMSHEQUITY": "equity", "TOTAL_EQUITY": "equity", "所有者权益合计": "equity", "股东权益合计": "equity",
    }
    periods = {}
    def collect(rows, period):
        for row in rows or []:
            if not isinstance(row, dict):
                continue
            key = aliases.get(row.get("metric_code") or row.get("指标") or row.get("metric"))
            if key is None:
                continue
            data = row.get("data") or row.get("数据")
            if isinstance(data, dict):
                for date, value in data.items():
                    periods.setdefault(date, {})[key] = n(value)
            elif period:
                periods.setdefault(period, {})[key] = n(row.get("值", row.get("value")))
    collect(statement.get("rows"), statement.get("report_date"))
    for item in statement.get("periods") or []:
        if isinstance(item, dict):
            collect(item.get("数据") or item.get("rows"), item.get("报告日") or item.get("报告日期") or item.get("report_date"))
    results = []
    for period, values in periods.items():
        missing = [key for key in ("assets", "liabilities", "equity") if values.get(key) is None]
        if missing:
            results.append({"period": period, "status": "MISSING_TOTALS", "missing": missing})
            continue
        delta = abs(values["assets"] - values["liabilities"] - values["equity"])
        results.append({"period": period, "delta_yuan": delta,
                        "status": "MATCH" if delta <= max(1, abs(values["assets"])*1e-7) else "MISMATCH"})
    return results


def sina_code(sample):
    symbol, market = sample["symbol"], sample["market"]
    if market == "HK":
        return "rt_hk" + symbol
    if market in {"NEEQ", "NEEQ_INNOVATION"}:
        return "sb" + symbol
    return ("sh" if symbol.startswith("6") else "bj" if sample["board"] == "BSE" else "sz") + symbol


def independent_quotes(samples):
    values = {}
    for start in range(0, len(samples), 20):
        batch = samples[start:start+20]
        url = "https://hq.sinajs.cn/list=" + ",".join(sina_code(s) for s in batch)
        record = {"url": url, "fetched_at": datetime.now(timezone.utc).isoformat(), "source": "新浪财经独立行情"}
        try:
            with httpx.Client(trust_env=False, timeout=15, headers={"User-Agent": "Mozilla/5.0", "Referer": "https://finance.sina.com.cn"}) as client:
                response = client.get(url)
                response.raise_for_status()
            response.encoding = "gb18030"
            record.update(body=response.text, sha256=hashlib.sha256(response.content).hexdigest(), status_code=response.status_code)
            parsed = dict(re.findall(r'var hq_str_([^=]+)="([^"]*)";', response.text))
            for sample in batch:
                fields = parsed.get(sina_code(sample), "").split(",")
                if sample["market"] == "HK" and len(fields) > 18:
                    value = {"name": fields[1], "close": n(fields[6]), "previous_close": n(fields[3]),
                             "volume": n(fields[12]), "amount": n(fields[11]), "date": fields[17].replace("/", "-"), "time": fields[18]}
                elif len(fields) > 31:
                    value = {"name": fields[0], "close": n(fields[3]), "previous_close": n(fields[2]),
                             "volume": n(fields[8]), "amount": n(fields[9]), "date": fields[30], "time": fields[31]}
                else:
                    value = {"status": "NO_QUOTE_UNVERIFIED", "reason": "独立来源无报价，不能判定无交易"}
                value.update(evidence_file=f"independent/sina-{start//20:02d}.json", code=sina_code(sample))
                values[sample["board"] + ":" + sample["symbol"]] = value
        except Exception as exc:
            record["error"] = str(exc)
            for sample in batch:
                values[sample["board"] + ":" + sample["symbol"]] = {"status": "SOURCE_FAILED", "error": str(exc)}
        save(OUT / "independent" / f"sina-{start//20:02d}.json", record)
    save(OUT / "independent-quotes.json", values)
    return values


def validate(sample, independent):
    result = {**sample, "checked_at": datetime.now(timezone.utc).isoformat(), "panels": [], "issues": []}
    started = time.monotonic()
    with httpx.Client(trust_env=False, timeout=70) as client:
        response = client.get(f"{API_BASE}/api/v1/stocks/{sample['market']}/{sample['symbol']}/f10", params={"local_only": "true", "include_classification_members": "false"})
        response.raise_for_status()
        detail = response.json()
    result.update(api_status=response.status_code, api_seconds=round(time.monotonic()-started, 3), api_sha256=hashlib.sha256(response.content).hexdigest())
    identity = detail.get("symbol") or {}
    if isinstance(identity, dict) and (identity.get("market"), identity.get("symbol")) != (sample["market"], sample["symbol"]):
        result["issues"].append({"kind": "IDENTITY_MISMATCH"})
    for section, field in (("profile", "overview_sections"), ("holders", "sections"), ("financial_summary", "financial_sections"), ("research_sections", "sections")):
        payload = detail.get(section) or {}
        for item in payload.get(field) or []:
            rows = item.get("rows") or []
            has_values = _has_observation_rows(rows)
            declared = item.get("status")
            result["panels"].append({"group": section, "key": item.get("key"), "name": item.get("title"),
                "rows": len(rows), "has_values": has_values, "display_status": declared,
                "status": "DATA_PRESENT_REQUIRES_SOURCE_CHECK" if has_values else "MISSING_REQUIRES_SOURCE_REVIEW",
                "source": item.get("source") or payload.get("source"), "as_of": item.get("as_of"), "reason": item.get("message")})
            if declared == "AVAILABLE" and not has_values:
                result["issues"].append({"kind": "FALSE_AVAILABLE", "section": section, "panel": item.get("key")})
    result["sections"] = {key: _section_has_payload(key, detail.get(key) or {}, sample["market"]) for key in (
        "profile", "holders", "financial_summary", "financial_statements", "business_composition", "fund_flow", "published_reports", "research_sections")}
    result["core_counts"] = {key: len(detail.get(key) or []) for key in ("recent_klines", "financial_reports", "notices", "news")}
    result["news_candidates"] = detail.get("news_candidate_total", 0)
    with sqlite3.connect(ROOT / "quant.db") as db:
        master = db.execute("SELECT ext_json FROM stock_symbol WHERE id=?", (sample["master_id"],)).fetchone()
        issuer_ext = json.loads(master[0]) if master and master[0] else {}
    for row in detail.get("news") or []:
        check = check_news_identity(sample["market"], sample["symbol"], sample["name"], row.get("title", ""), row.get("content"), issuer_ext)
        if check["status"] != "MENTION_MATCHED":
            result["issues"].append({"kind": "UNMATCHED_NEWS_DISPLAY", "id": row.get("id")})
    with sqlite3.connect(ROOT / "quant.db") as db:
        db.row_factory = sqlite3.Row
        row = db.execute("SELECT * FROM stock_realtime_quote WHERE market=? AND symbol=? ORDER BY fetched_at DESC LIMIT 1", (sample["market"], sample["symbol"])).fetchone()
        if row:
            quote = dict(row)
            raw_quote = json.loads(quote.pop("raw_payload", "{}") or "{}")
            result["quote_provenance"] = {key: raw_quote.get(key) for key in ("source_method", "no_trade_today", "latest_price_source", "units") if key in raw_quote}
            result["quote"] = quote
    ext = independent.get(sample["board"] + ":" + sample["symbol"], {})
    result["independent_quote"] = ext
    quote = result.get("quote") or {}
    quote_date = str(quote.get("quote_time") or "")[:10]
    provenance = result.get("quote_provenance", {})
    if (
        sample["market"] in {"NEEQ", "NEEQ_INNOVATION"}
        and provenance.get("latest_price_source") == "PUBLIC_DAILY_TRANSACTION_TABLE"
        and quote_date == ext.get("date")
        and n(quote.get("current_price")) is not None
        and n(ext.get("previous_close")) is not None
        and abs(float(quote["current_price"]) - float(ext["previous_close"])) <= 0.001
        and n(quote.get("volume")) is not None and n(ext.get("volume")) is not None
        and abs(float(quote["volume"]) - float(ext["volume"])) <= 100
        and n(quote.get("amount")) is not None and n(ext.get("amount")) is not None
        and abs(float(quote["amount"]) - float(ext["amount"])) <= max(0.01, abs(float(ext["amount"])) * 0.0001)
    ):
        result["price_check"] = "MATCH_SOURCE_RECONCILIATION"
        result["source_reconciliation_note"] = "实时源缺少收盘价；同代码同日交易表补价，独立源昨收、成交量和成交额一致"
    elif provenance.get("no_trade_today"):
        result["price_check"] = "NO_TRADE_REFERENCE_ONLY"
        result["reference_date_note"] = "来源快照尚无成交，显示前收盘参考价，不作为当日成交价通过验证"
    elif ext.get("close") is not None and ext.get("close") > 0 and n(quote.get("current_price")) is not None:
        if quote_date == ext.get("date"):
            price_ok = abs(ext["close"] - float(quote["current_price"])) <= max(0.001, ext["close"] * 0.0001)
            result["price_check"] = "MATCH" if price_ok else "MISMATCH"
            if not price_ok:
                result["issues"].append({"kind": "INDEPENDENT_PRICE_MISMATCH", "local": quote["current_price"], "independent": ext["close"]})
            if ext.get("volume") is not None and n(quote.get("volume")) is not None:
                # The A-share provider rounds lots; at most 99 shares may be
                # discarded. Different close timestamps are retained as evidence.
                delta = abs(ext["volume"] - float(quote["volume"]))
                result["volume_check"] = "MATCH_WITH_LOT_ROUNDING" if delta <= 100 else "MISMATCH_REVIEW_TIME"
                if delta > max(100, ext["volume"] * 0.001):
                    result["issues"].append({"kind": "INDEPENDENT_VOLUME_MISMATCH", "local": quote["volume"], "independent": ext["volume"]})
        else:
            result["price_check"] = "TIME_MISMATCH_REQUIRES_REFRESH"
    elif ext.get("close") == 0 and ext.get("volume") == 0:
        result["price_check"] = "NO_TRADE_REFERENCE_ONLY"
    else:
        result["price_check"] = "INDEPENDENT_SOURCE_UNVERIFIED"
    for row in (detail.get("holders") or {}).get("major") or []:
        ratio = n(row.get("持股比例") or row.get("持股比例(%)") or row.get("比例"))
        if ratio is not None and not 0 <= ratio <= 100:
            result["issues"].append({"kind": "HOLDING_RATIO_OUT_OF_RANGE", "value": ratio})
    # Balance-sheet accounting check on periods carrying all three facts.
    statement = (detail.get("financial_statements") or {}).get("balance_sheet") or {}
    checks = balance_checks(statement)
    result["balance_checks"] = checks
    result["issues"].extend({"kind": "BALANCE_EQUATION", **c} for c in checks if c["status"] == "MISMATCH")
    result["status"] = "PARTIAL" if any(not p["has_values"] for p in result["panels"]) else "DATA_PRESENT"
    if result["issues"]:
        result["status"] = "FAILED"
    save(OUT / "details" / f"{sample['board']}-{sample['symbol']}.json", result)
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--boards", default="")
    parser.add_argument("--workers", type=int, default=3)
    parser.add_argument("--refresh-independent", action="store_true")
    parser.add_argument("--retry-failed", action="store_true")
    args = parser.parse_args()
    samples = json.loads((OUT / "samples.json").read_text(encoding="utf-8"))
    if args.boards:
        samples = [s for s in samples if s["board"] in args.boards.split(",")]
    previous = {}
    summary_path = OUT / "detail-summary.json"
    if summary_path.exists():
        previous = {(r["board"], r["symbol"]): r for r in json.loads(summary_path.read_text(encoding="utf-8"))["results"]}
    if args.retry_failed:
        samples = [s for s in samples if previous.get((s["board"], s["symbol"]), {}).get("status") == "FAILED"
                   or previous.get((s["board"], s["symbol"]), {}).get("price_check") == "TIME_MISMATCH_REQUIRES_REFRESH"]
    path = OUT / "independent-quotes.json"
    independent = independent_quotes(samples) if args.refresh_independent or not path.exists() else json.loads(path.read_text(encoding="utf-8"))
    results = []
    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        jobs = {pool.submit(validate, s, independent): s for s in samples}
        for index, future in enumerate(as_completed(jobs), 1):
            sample = jobs[future]
            try:
                result = future.result()
            except Exception as exc:
                result = {**sample, "status": "FAILED", "issues": [{"kind": "DETAIL_API_FAILURE", "error": str(exc)}]}
                save(OUT / "details" / f"{sample['board']}-{sample['symbol']}.json", result)
            key = (sample["board"], sample["symbol"])
            if key in previous:
                stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%f")
                save(OUT / "detail-history" / f"{sample['board']}-{sample['symbol']}-{stamp}.json", previous[key])
            previous[key] = result
            results.append(result)
            print(json.dumps({"done": index, "total": len(samples), "board": sample["board"], "symbol": sample["symbol"],
                "status": result["status"], "price": result.get("price_check"), "issues": result["issues"]}, ensure_ascii=False), flush=True)
    save(summary_path, {"checked_at": datetime.now(timezone.utc).isoformat(), "results": list(previous.values())})


if __name__ == "__main__":
    main()
