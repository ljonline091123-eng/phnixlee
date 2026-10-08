"""Read-only browser audit of the fixed 160-stock manifest, with screenshots.

Automatic POST refreshes are deliberately blocked: collection is verified by
audit_all_boards.py, while this audit checks persisted data and actual rendering.
"""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, as_completed
import subprocess
import sys
import time

from playwright.sync_api import expect, sync_playwright

OUT = Path(__file__).resolve().parents[1] / "validation" / "all-boards-20261008"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--boards", default="")
    parser.add_argument("--limit", type=int, default=160)
    parser.add_argument("--workers", type=int, default=1)
    parser.add_argument("--result-name", default="browser-result.json")
    parser.add_argument("--resume-from", default="")
    args = parser.parse_args()
    samples = json.loads((OUT / "samples.json").read_text(encoding="utf-8"))
    samples = [s for s in samples if not args.boards or s["board"] in args.boards.split(",")][:args.limit]
    if args.workers > 1:
        boards = list(dict.fromkeys(s["board"] for s in samples))
        def run_board(board):
            command = [sys.executable, "-u", str(Path(__file__).resolve()), "--boards", board,
                       "--result-name", f"browser-{board}.json", "--resume-from", args.result_name]
            process = subprocess.run(command, capture_output=True, text=True, encoding="utf-8", errors="replace")
            if process.returncode:
                raise RuntimeError(f"{board}: {process.stderr[-1200:]}")
            print(json.dumps({"board": board, "browser_audit": "FINISHED"}), flush=True)
        with ThreadPoolExecutor(max_workers=args.workers) as pool:
            for future in as_completed([pool.submit(run_board, b) for b in boards]):
                future.result()
        batches = [json.loads((OUT / f"browser-{b}.json").read_text(encoding="utf-8")) for b in boards]
        (OUT / args.result_name).write_text(json.dumps({
            "checked_at": datetime.now(timezone.utc).isoformat(), "read_only": True,
            "data_accuracy_checked_separately": True, "expected": len(samples),
            "results": [r for batch in batches for r in batch["results"]],
            "page_errors": [e for batch in batches for e in batch["page_errors"]],
            "blocked_mutations": [e for batch in batches for e in batch["blocked_mutations"]],
        }, ensure_ascii=False, indent=2), encoding="utf-8")
        return
    (OUT / "screenshots").mkdir(exist_ok=True)
    results, errors, blocked = [], [], []
    resume_paths = [OUT / args.result_name]
    if args.resume_from:
        resume_paths.append(OUT / args.resume_from)
    keys = {(s["board"], s["symbol"]) for s in samples}
    passed = {}
    for resume_path in resume_paths:
        if resume_path.exists():
            previous = json.loads(resume_path.read_text(encoding="utf-8"))
            for row in previous["results"]:
                key = (row["board"], row["symbol"])
                if key in keys and row["status"] == "RENDER_PASS":
                    passed[key] = row
    results = list(passed.values())
    completed = {(r["board"], r["symbol"]) for r in results}
    with sync_playwright() as p:
        browser = p.chromium.launch(channel="msedge", headless=True)
        page = browser.new_page(viewport={"width": 1500, "height": 1100})
        page.on("pageerror", lambda error: errors.append(str(error)))

        def read_only(route):
            remote_refresh = "/f10?" in route.request.url and "local_only=true" not in route.request.url
            if route.request.method not in {"GET", "HEAD", "OPTIONS"} or remote_refresh:
                blocked.append({"url": route.request.url, "method": route.request.method})
                route.fulfill(status=409, content_type="application/json", body=json.dumps({
                    "detail": "只读浏览器验收：采集已由独立验证任务执行，此处不重复启动写入。"}, ensure_ascii=False))
            else:
                route.continue_()
        page.route("**/api/v1/**", read_only)
        page.goto("http://127.0.0.1:5173/#/data/master", wait_until="domcontentloaded")
        search = page.get_by_placeholder("按代码或名称模糊搜索")
        expect(search).to_be_visible(timeout=60000)
        current_market = None
        seen_boards = set()
        for index, sample in enumerate(samples, 1):
            if (sample["board"], sample["symbol"]) in completed:
                seen_boards.add(sample["board"])
                continue
            started, first_error = time.monotonic(), len(errors)
            item = {**sample, "panels": [], "issues": []}
            try:
                if current_market != sample["market"]:
                    search.locator("xpath=..").locator("select").first.select_option(sample["market"])
                    current_market = sample["market"]
                search.fill(sample["symbol"])
                with page.expect_response(lambda r: "/api/v1/stocks?" in r.url and
                    f"keyword={sample['symbol']}" in r.url, timeout=60000) as lookup:
                    search.locator("xpath=..").get_by_role("button", name="查询", exact=True).click()
                assert lookup.value.ok, f"股票查询HTTP{lookup.value.status}"
                link = page.locator(".symbol-link").filter(has_text=sample["symbol"])
                with page.expect_response(lambda r: f"/stocks/{sample['market']}/{sample['symbol']}/f10?" in r.url
                    and "local_only=true" in r.url, timeout=75000) as loaded:
                    link.first.click(timeout=60000)
                assert loaded.value.ok, f"详情HTTP{loaded.value.status}"
                # Large F10 responses can exceed Chromium's inspector body
                # cache. API identity/values are checked in the independent
                # detail audit; here verify the actual rendered stock header.
                drawer = page.locator(".stock-detail-drawer")
                expect(drawer.locator(".detail-market-header h2")).to_contain_text(sample["symbol"], timeout=60000)
                for tab in ("精选", "新闻", "公告", "资金", "F10", "研究"):
                    drawer.locator(".detail-primary-tabs").get_by_role("button", name=tab, exact=True).click()
                    expect(drawer.locator(".detail-primary-tabs button.active")).to_have_text(tab)
                    children = ("财务", "股东", "简况", "财报") if tab == "F10" else (None,)
                    for subtab in children:
                        if subtab:
                            drawer.locator(".detail-sub-tabs").get_by_role("button", name=subtab, exact=True).click()
                            expect(drawer.locator(".detail-sub-tabs button.active")).to_have_text(subtab)
                        body = drawer.locator(".detail-tab-page").inner_text()
                        assert body.strip(), f"{tab}/{subtab}页面空白"
                        for bad in ("[object Object]", "undefined", "NaN"):
                            if bad in body:
                                item["issues"].append({"kind": "INVALID_DISPLAY_TEXT", "panel": subtab or tab, "text": bad})
                        item["panels"].append({"tab": tab, "subtab": subtab, "text_length": len(body),
                                               "excerpt": body[:300], "rendered": True})
                        if sample["board"] not in seen_boards or tab == "研究":
                            drawer.locator(".detail-tab-page").scroll_into_view_if_needed()
                            screenshot = f"screenshots/{sample['board']}-{sample['symbol']}-{subtab or tab}.png"
                            page.screenshot(path=str(OUT / screenshot))
                            item["panels"][-1]["screenshot"] = screenshot
                seen_boards.add(sample["board"])
            except Exception as exc:
                item["issues"].append({"kind": "BROWSER_FAILURE", "error": str(exc)[:1200]})
                page.screenshot(path=str(OUT / "screenshots" / f"FAILED-{sample['board']}-{sample['symbol']}.png"))
            finally:
                item["page_errors"] = errors[first_error:]
                item["status"] = "RENDER_PASS" if not item["issues"] and not item["page_errors"] else "FAILED"
                item["seconds"] = round(time.monotonic()-started, 2)
                results.append(item)
                (OUT / args.result_name).write_text(json.dumps({
                    "checked_at": datetime.now(timezone.utc).isoformat(), "read_only": True,
                    "data_accuracy_checked_separately": True, "expected": len(samples),
                    "results": results, "page_errors": errors, "blocked_mutations": blocked,
                }, ensure_ascii=False, indent=2), encoding="utf-8")
                print(json.dumps({"done": index, "total": len(samples), "board": sample["board"],
                    "symbol": sample["symbol"], "status": item["status"], "issues": item["issues"]}, ensure_ascii=False), flush=True)
                close = page.locator(".stock-detail-drawer .detail-head-actions").get_by_role("button", name="关闭", exact=True)
                if close.count():
                    close.click()
        browser.close()


if __name__ == "__main__":
    main()
