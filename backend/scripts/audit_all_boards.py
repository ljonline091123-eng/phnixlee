"""Resumable live ingestion acceptance, with fixed samples and isolated workers.

Run from backend: python scripts/audit_all_boards.py --workers 4
An empty response is NOT an acceptance pass. Network failures are kept as
failures, and unconfigured capabilities require a separately evidenced review.
"""
from __future__ import annotations

import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed
from contextlib import redirect_stdout, redirect_stderr
from datetime import date, datetime, timedelta, timezone
import hashlib
import io
import json
import os
from pathlib import Path
import sqlite3
import subprocess
import sys
import time
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
OUTPUT = ROOT / "validation" / "all-boards-20261008"
STAGES = ("quote", "kline", "financials", "notices", "news", "extended", "research")
API_BASE = os.environ.get("QUANT_AUDIT_API_URL", "http://127.0.0.1:8000").rstrip("/")
PREFERRED = {
    "MAIN": "000001 000002 000063 000333 000651 000725 000858 002415 002594 600000 600036 600519 600030 601318 601398 601288 601988 601899 601857 601166".split(),
    "STAR": "688001 688008 688012 688036 688041 688111 688169 688180 688187 688223 688256 688271 688396 688599 688981 688516 688017 688099 688506 688120".split(),
    "CHINEXT": "300001 300014 300015 300033 300059 300122 300124 300142 300347 300413 300433 300450 300498 300628 300750 300759 300760 300782 300896 301269".split(),
    "HK_MAIN": "02533 00981 00700 00883 00939 00941 00998 01211 01810 02015 03690 03988 06618 09618 09626 09866 09868 09888 09988 00001".split(),
}


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps(value, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
    temporary.replace(path)


def sample_path(sample):
    return OUTPUT / "stocks" / f"{sample['board']}-{sample['symbol']}.json"


def fixed_samples():
    manifest = OUTPUT / "samples.json"
    if manifest.exists():
        return json.loads(manifest.read_text(encoding="utf-8"))
    from sqlalchemy import select
    from app.db.session import SessionLocal
    from app.models.market_data import StockSymbol
    from app.services.security_master import BOARD_DEFINITIONS, board_expression, type_expression
    samples = []
    with SessionLocal() as db:
        for board in BOARD_DEFINITIONS:
            rows = db.scalars(select(StockSymbol).where(
                board_expression() == board["code"], type_expression() == "STOCK",
                StockSymbol.status == "LISTED",
            ).order_by(StockSymbol.symbol)).all()
            # Official BSE new-code identities prevent selecting duplicate old
            # and new codes of the same issuer as two independent samples.
            if board["code"] == "BSE":
                official = [s for s in rows if (s.ext_json or {}).get("listing", {}).get("classification_method") == "SOURCE"]
                rows = official or rows
            keyed = {s.symbol: s for s in rows}
            preferred = PREFERRED.get(board["code"], [])
            selected = [keyed[s] for s in preferred if s in keyed]
            if preferred and len(selected) != 20:
                raise RuntimeError(f"{board['code']} fixed preferred sample not present: {set(preferred)-set(keyed)}")
            if not preferred:
                if len(rows) < 20:
                    raise RuntimeError(f"{board['code']} has fewer than 20 listed stocks")
                selected = [rows[round(i * (len(rows)-1) / 19)] for i in range(20)]
            for stock in selected:
                samples.append({"board": board["code"], "board_name": board["name"], "market": stock.market,
                                "symbol": stock.symbol, "name": stock.name, "master_id": stock.id,
                                "listing_evidence": (stock.ext_json or {}).get("listing", {}),
                                "sampling": "fixed regression cases" if preferred else "evenly spaced listed securities"})
    write_json(manifest, samples)
    return samples


def install_bounded_http(trace, clock):
    """Bound third-party calls in this audit process; no production monkeypatch."""
    import httpx
    import requests
    original_request = requests.sessions.Session.request
    original_send = httpx.Client.send

    def url_label(url):
        parsed = urlsplit(str(url))
        return parsed.scheme + "://" + parsed.netloc + parsed.path

    def remaining():
        seconds = clock[0] - time.monotonic()
        if seconds <= 0:
            raise TimeoutError("本阶段采集预算耗尽；保留断点，不能标记无数据")
        return max(0.2, min(8.0, seconds))

    def request(self, method, url, **kwargs):
        deadline = remaining()
        timeout = kwargs.get("timeout")
        kwargs["timeout"] = min(timeout, deadline) if isinstance(timeout, (int, float)) else deadline
        self.trust_env = False
        started = time.monotonic()
        entry = {"url": url_label(url), "method": method, "at": datetime.now(timezone.utc).isoformat()}
        try:
            response = original_request(self, method, url, **kwargs)
            entry.update(status_code=response.status_code, response_hash=hashlib.sha256(response.content).hexdigest(), bytes=len(response.content))
            return response
        except Exception as exc:
            entry["error"] = str(exc)[:350]
            raise
        finally:
            entry["elapsed"] = round(time.monotonic()-started, 3)
            trace.append(entry)

    def send(self, request, **kwargs):
        deadline = remaining()
        request.extensions["timeout"] = {k: min(v, deadline) if isinstance(v, (int, float)) else deadline
                                         for k, v in request.extensions.get("timeout", {"connect": None, "read": None, "write": None, "pool": None}).items()}
        started = time.monotonic()
        entry = {"url": url_label(request.url), "method": request.method, "at": datetime.now(timezone.utc).isoformat()}
        try:
            response = original_send(self, request, **kwargs)
            if response.is_stream_consumed:
                entry.update(response_hash=hashlib.sha256(response.content).hexdigest(), bytes=len(response.content))
            entry["status_code"] = response.status_code
            return response
        except Exception as exc:
            entry["error"] = str(exc)[:350]
            raise
        finally:
            entry["elapsed"] = round(time.monotonic()-started, 3)
            trace.append(entry)
    requests.sessions.Session.request = request
    httpx.Client.send = send
    try:
        from curl_cffi.requests import Session as CurlSession
    except ImportError:
        return
    original_curl_request = CurlSession.request

    def curl_request(self, method, url, **kwargs):
        deadline = remaining()
        timeout = kwargs.get("timeout")
        kwargs["timeout"] = min(timeout, deadline) if isinstance(timeout, (int, float)) else deadline
        started = time.monotonic()
        entry = {"url": url_label(url), "method": method, "transport": "browser-compatible",
                 "at": datetime.now(timezone.utc).isoformat()}
        try:
            response = original_curl_request(self, method, url, **kwargs)
            entry.update(status_code=response.status_code, response_hash=hashlib.sha256(response.content).hexdigest(), bytes=len(response.content))
            return response
        except Exception as exc:
            entry["error"] = str(exc)[:350]
            raise
        finally:
            entry["elapsed"] = round(time.monotonic()-started, 3)
            trace.append(entry)
    CurlSession.request = curl_request


def worker(sample, stages):
    from app.db.session import SessionLocal
    from app.services.catalog import select_data_source
    from app.services.stock_on_demand import StockOnDemandService
    from app.services.f10 import _fetch_and_cache_f10_extended_data, _section_has_payload
    path = sample_path(sample)
    result = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {**sample, "attempts": {}}
    trace, clock = [], [time.monotonic()+60]
    install_bounded_http(trace, clock)
    end = date.today().strftime("%Y%m%d")
    start = (date.today()-timedelta(days=400)).strftime("%Y%m%d")
    for stage in stages:
        clock[0] = time.monotonic() + (130 if stage == "extended" else 90 if stage in {"research", "news"} else 42)
        started, trace_start = time.monotonic(), len(trace)
        outcome = {"at": datetime.now(timezone.utc).isoformat()}
        capability = {"quote": "QUOTE", "kline": "KLINE", "financials": "FINANCIAL", "notices": "NOTICE", "news": "NEWS", "extended": "F10", "research": "F10"}[stage]
        try:
            with SessionLocal() as db, redirect_stdout(io.StringIO()), redirect_stderr(io.StringIO()):
                source = select_data_source(db, sample["market"], capability)
                if source is None:
                    outcome.update(status="MISSING_UNVERIFIED", reason="当前没有匹配启用接口；需另行核查来源")
                else:
                    outcome.update(source_code=source.source_code, source_id=source.id, adapter_type=source.adapter_type)
                    service = StockOnDemandService(db)
                    args = dict(source=source, market=sample["market"], symbol=sample["symbol"], persist=True)
                    if stage == "research":
                        from app.connectors.akshare_adapter import AkshareAdapter
                        from app.connectors.hk_research import fetch_hk_research_sections
                        from app.services.research_store import persist_research_sections
                        from app.services.lakehouse import archive_ingestion_payload
                        from app.models.market_data import StockF10Cache
                        from sqlalchemy import select
                        if sample["market"] == "CN_A":
                            research = AkshareAdapter()._fetch_cn_a_research_sections(sample["symbol"])
                        elif sample["market"] == "HK":
                            research = fetch_hk_research_sections(sample["symbol"])
                        else:
                            raise ValueError("新三板研究来源尚未适配，不能声明已验证无数据")
                        stats = persist_research_sections(db, sample["market"], sample["symbol"], research)
                        db.commit()
                        service.upsert_f10_cache(source, sample["market"], sample["symbol"], "research_sections", research)
                        cache = db.scalar(select(StockF10Cache).where(StockF10Cache.market == sample["market"],
                            StockF10Cache.symbol == sample["symbol"], StockF10Cache.section == "research_sections"))
                        raw = archive_ingestion_payload(db, source_code=source.source_code,
                            log_type="stock_f10_cache", log_id=cache.id, records=[research])
                        outcome.update(status="FETCHED" if _section_has_payload("research_sections", research, sample["market"])
                                       else "EMPTY_UNVERIFIED", persisted=stats, object_id=raw["object_id"],
                                       counts={k: len(research.get(k) or []) for k in
                                           ("qa", "earnings_forecast", "institution_forecast", "reports")})
                    elif stage == "extended":
                        stats = {}
                        payload = _fetch_and_cache_f10_extended_data(db, source, sample["market"], sample["symbol"], stats)
                        outcome.update(status="FETCHED", sections={key: {"has_data": _section_has_payload(key, value, sample["market"]),
                            "message": value.get("message"), "source": value.get("source")}
                            for key, value in payload.items() if isinstance(value, dict)}, persisted=stats)
                    else:
                        if stage == "quote":
                            log, rows = service.fetch_quote(**args)
                        elif stage == "kline":
                            log, rows = service.fetch_kline(**args, period="daily", adjust="", start_date=start, end_date=end)
                        elif stage == "financials":
                            log, rows = service.fetch_financials(**args, indicator="按报告期" if sample["market"] == "CN_A" else "报告期")
                        elif stage == "notices":
                            log, rows = service.fetch_notices(**args, start_date=start, end_date=end)
                        else:
                            log, rows = service.fetch_news(**args)
                        outcome.update(status="FETCHED" if rows else "EMPTY_UNVERIFIED", count=len(rows), persisted=log.persisted_count,
                                       log_id=log.id, rows_hash=hashlib.sha256(json.dumps(rows, sort_keys=True, default=str).encode()).hexdigest())
        except Exception as exc:
            outcome.update(status="FAILED", error=str(exc)[:700])
        outcome.update(elapsed=round(time.monotonic()-started, 3), requests=trace[trace_start:])
        result["attempts"].setdefault(stage, []).append(outcome)
        write_json(path, result)
    return result


def inspect_local(sample):
    import httpx
    from app.services.f10 import _section_has_payload
    counts, issues = {}, []
    with sqlite3.connect(ROOT / "quant.db") as db:
        db.row_factory = sqlite3.Row
        for stage, table in {"quote": "stock_realtime_quote", "kline": "stock_kline", "financials": "stock_financial_report",
                             "notices": "stock_notice", "news": "stock_news", "extended": "stock_f10_cache"}.items():
            counts[stage] = db.execute(f'SELECT count(*) FROM "{table}" WHERE market=? AND symbol=?', (sample["market"], sample["symbol"])).fetchone()[0]
        counts["research"] = sum(db.execute(f'SELECT count(*) FROM "{table}" WHERE market=? AND symbol=?',
            (sample["market"], sample["symbol"])).fetchone()[0] for table in
            ("stock_investor_qa", "stock_earnings_consensus", "stock_institution_forecast", "stock_broker_research_report"))
        for row in db.execute("SELECT * FROM stock_kline WHERE market=? AND symbol=?", (sample["market"], sample["symbol"])):
            if row["high_price"] is not None and row["low_price"] is not None and row["high_price"] < row["low_price"]:
                issues.append({"kind": "OHLC_ORDER", "id": row["id"]})
            for key in ("volume", "amount"):
                if row[key] is not None and row[key] < 0:
                    issues.append({"kind": "NEGATIVE_"+key, "id": row["id"]})
        for row in db.execute("SELECT id,data_json FROM stock_financial_report WHERE market=? AND symbol=?", (sample["market"], sample["symbol"])):
            raw = json.loads(row["data_json"])
            if raw.get("source") == "Eastmoney push2 quote indicators":
                issues.append({"kind": "LEGACY_QUOTE_AS_FINANCIAL", "id": row["id"]})
    started = time.monotonic()
    with httpx.Client(timeout=75, trust_env=False) as client:
        response = client.get(f"{API_BASE}/api/v1/stocks/{sample['market']}/{sample['symbol']}/f10", params={"local_only": "true", "include_classification_members": "false"})
        response.raise_for_status()
        detail = response.json()
    identity = detail.get("symbol", {})
    if isinstance(identity, dict) and (identity.get("market"), identity.get("symbol")) != (sample["market"], sample["symbol"]):
        issues.append({"kind": "DISPLAY_IDENTITY_MISMATCH", "identity": identity})
    projection = {}
    for section in ("profile", "holders", "fund_flow", "financial_summary", "financial_statements", "business_composition", "published_reports", "research_sections"):
        payload = detail.get(section) or {}
        children = payload.get("sections") or payload.get("overview_sections") or payload.get("financial_sections") or []
        projection[section] = {"has_data": _section_has_payload(section, payload, sample["market"]), "source": payload.get("source"),
            "message": payload.get("message"), "children": [{"key": s.get("key"), "title": s.get("title"), "status": s.get("status"),
            "count": len(s.get("rows") or []), "message": s.get("message")} for s in children if isinstance(s, dict)]}
    return {"checked_at": datetime.now(timezone.utc).isoformat(), "counts": counts, "issues": issues,
            "api_status": response.status_code, "api_elapsed": round(time.monotonic()-started, 3), "projection": projection,
            "display": {key: len(detail.get(key) or []) for key in ("recent_klines", "financial_reports", "notices", "news")},
            "api_hash": hashlib.sha256(response.content).hexdigest()}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--workers", type=int, default=4)
    parser.add_argument("--limit-per-board", type=int, default=20)
    parser.add_argument("--offset", type=int, default=0)
    parser.add_argument("--boards", default="")
    parser.add_argument("--symbols", default="", help="仅复验指定代码，逗号分隔")
    parser.add_argument("--stages", default=",".join(STAGES))
    parser.add_argument("--retry", action="store_true")
    parser.add_argument("--force", action="store_true")
    parser.add_argument("--inspect-only", action="store_true")
    parser.add_argument("--child", default="")
    args = parser.parse_args()
    samples = fixed_samples()
    if args.child:
        sample = next(s for s in samples if f"{s['board']}:{s['symbol']}" == args.child)
        worker(sample, args.stages.split(","))
        return
    selected = []
    for board in dict.fromkeys(s["board"] for s in samples):
        if args.boards and board not in args.boards.split(","):
            continue
        selected.extend([s for s in samples if s["board"] == board][args.offset:args.limit_per_board])
    if args.symbols:
        selected = [s for s in selected if s["symbol"] in args.symbols.split(",")]

    def run(sample):
        path = sample_path(sample)
        result = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {**sample, "attempts": {}}
        wanted = args.stages.split(",")
        stages = [s for s in wanted if args.force or not result["attempts"].get(s) or (args.retry and result["attempts"][s][-1]["status"] != "FETCHED")]
        if stages and not args.inspect_only:
            command = [sys.executable, str(Path(__file__).resolve()), "--child", f"{sample['board']}:{sample['symbol']}", "--stages", ",".join(stages)]
            try:
                process = subprocess.run(command, cwd=ROOT, capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=410)
                if process.returncode:
                    result["worker_error"] = process.stderr[-1200:]
            except subprocess.TimeoutExpired:
                result["worker_error"] = "独立采集进程超时；已提交的各阶段数据及结果保留，未完成阶段需要继续"
        result = json.loads(path.read_text(encoding="utf-8")) if path.exists() else result
        try:
            result["local"] = inspect_local(sample)
            result.pop("inspection_error", None)
        except Exception as exc:
            result["inspection_error"] = str(exc)[:800]
        write_json(path, result)
        return result

    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        futures = {pool.submit(run, sample): sample for sample in selected}
        for n, future in enumerate(as_completed(futures), 1):
            result = future.result()
            print(json.dumps({"done": n, "total": len(selected), "board": result["board"], "symbol": result["symbol"],
                "stages": {k: v[-1]["status"] for k, v in result["attempts"].items()}, "counts": result.get("local", {}).get("counts"),
                "issues": result.get("local", {}).get("issues"), "error": result.get("inspection_error")}, ensure_ascii=False), flush=True)
    all_results = [json.loads(sample_path(s).read_text(encoding="utf-8")) for s in samples if sample_path(s).exists()]
    summary = []
    for board in dict.fromkeys(s["board"] for s in samples):
        values = [s for s in all_results if s["board"] == board]
        summary.append({"board": board, "audited": len(values), "expected": 20,
            "data_counts": {stage: sum(s.get("local", {}).get("counts", {}).get(stage, 0) for s in values) for stage in STAGES},
            "collected_stocks": {stage: sum(bool(s.get("local", {}).get("counts", {}).get(stage)) for s in values) for stage in STAGES},
            "failed_attempts": sum(v[-1]["status"] == "FAILED" for s in values for v in s["attempts"].values()),
            "unverified_empty": sum(v[-1]["status"].endswith("UNVERIFIED") for s in values for v in s["attempts"].values()),
            "quality_issues": sum(len(s.get("local", {}).get("issues", [])) for s in values)})
    write_json(OUTPUT / "summary.json", {"as_of": datetime.now(timezone.utc).isoformat(), "boards": summary,
        "status": "PARTIAL", "note": "只有实际记录及证据核查可通过；空响应、接口缺失及网络失败尚需逐项调查。"})
    print(json.dumps(summary, ensure_ascii=False), flush=True)


if __name__ == "__main__":
    main()
