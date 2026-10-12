"""Read-only report windows: real report reads and isolated large-report checks."""
from __future__ import annotations

import json
from pathlib import Path
from urllib.parse import parse_qs, urlparse

import requests
from playwright.sync_api import expect, sync_playwright

OUTPUT = Path(__file__).resolve().parents[1] / "validation" / "governance-records-followup"
PAGE_URL = "http://127.0.0.1:5173/#/operations/records"
API_URL = "http://127.0.0.1:8000/api/v1/stocks/batch-governance/jobs"


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    errors, writes, checks = [], [], []
    session = requests.Session()
    with sync_playwright() as p:
        browser = p.chromium.launch(channel="msedge", headless=True)
        page = browser.new_page(viewport={"width": 1500, "height": 1000})
        page.on("pageerror", lambda error: errors.append(str(error)))

        def guard(route):
            if route.request.method in {"GET", "OPTIONS"}:
                route.continue_()
            else:
                writes.append(route.request.url)
                route.abort()

        page.route("**/api/v1/**", guard)
        for job_id in (528, 525):
            response = session.get(f"{API_URL}/{job_id}", timeout=60)
            response.raise_for_status()
            report = response.json()["governance_report"]
            page.goto(f"{PAGE_URL}?job={job_id}", wait_until="domcontentloaded")
            main_dialog = page.get_by_role("dialog", name=f"治理报告 · 任务 #{job_id}")
            expect(main_dialog).to_be_visible(timeout=60000)
            expect(main_dialog.locator(".gr-summary")).to_contain_text(str(report["summary"]["skill_execution_count"]))
            expect(main_dialog.get_by_role("button", name="立即执行")).to_have_count(0)
            expect(main_dialog.get_by_role("button", name="继续治理", exact=True)).to_have_count(0)
            expect(main_dialog.get_by_role("link", name="进入治理工作台处理未完成项")).to_have_attribute("href", f"#/operations/workbench?job={job_id}")
            # Main report is a summary, detailed source data is loaded on demand.
            expect(main_dialog.locator(".gr-data-fields")).to_have_count(0)
            chunks_row = main_dialog.locator(".gr-stage-table tbody tr").filter(has_text="文档切片")
            chunks_row.get_by_role("button", name="查看阶段明细").click()
            detail = page.locator(".gr-detail-modal")
            expect(detail).to_be_visible()
            runs = report["stage_results"]["DOCUMENT_CHUNKS"].get("market_runs", [])
            if runs:
                run = runs[0]
                expect(detail).to_contain_text(f"文档覆盖 {run['documents_with_chunks']}/{run['documents_total']} 份")
            preview_button = detail.get_by_role("button", name="查看数据与证据").first
            if preview_button.count():
                preview_button.click()
                preview = page.get_by_role("dialog", name="阶段成果数据预览")
                expect(preview).to_be_visible(timeout=60000)
                expect(preview).to_contain_text("查看本轮验收记录与质量")
                page.screenshot(path=str(OUTPUT / f"evidence-{job_id}.png"))
                page.keyboard.press("Escape")
                expect(preview).to_have_count(0)
                expect(detail).to_be_visible()
            page.keyboard.press("Escape")
            expect(detail).to_have_count(0)
            expect(main_dialog).to_be_visible()
            main_dialog.get_by_role("button", name="股票治理成果", exact=True).click()
            expect(page.locator(".gr-detail-modal")).to_contain_text("股票基本信息与治理成果")
            page.keyboard.press("Escape")
            main_dialog.get_by_role("button", name="失败原因与优化建议", exact=True).click()
            expect(page.locator(".gr-detail-modal")).to_contain_text("缺口记录可能来自同一原因")
            page.keyboard.press("Escape")
            page.screenshot(path=str(OUTPUT / f"report-{job_id}.png"))
            checks.append(f"真实报告#{job_id}：概要、阶段、股票、质量、证据与逐层关闭")
        page.close()

        # Validate pagination with an isolated fixture; production data is never written.
        page = browser.new_page(viewport={"width": 1440, "height": 1000})
        page.on("pageerror", lambda error: errors.append(str(error)))
        fixture = {"job_id": 999999, "status": "PARTIAL", "job_status": "COMPLETED",
                   "result_status": "PARTIAL", "progress": 100, "stock_count": 45,
                   "current_stage": "COMPLETE", "governance_mode": "AI_AGENT_SKILL_GOVERNANCE",
                   "governance_report": {"job": {"job_status": "COMPLETED", "progress": 100},
                       "stock_results": [{"market": "CN_A", "symbol": str(100000+i), "stock_name": f"测试股票{i}", "status": "PARTIAL"} for i in range(45)],
                       "stage_results": {}, "errors": [], "skills": [], "agent": None,
                       "summary": {"stock_count": 45, "completed_stocks": 0, "skill_execution_count": 0, "error_count": 0}}}

        def mock(route):
            if route.request.method not in {"GET", "OPTIONS"}:
                writes.append(route.request.url); route.abort(); return
            path = urlparse(route.request.url).path
            if "/stocks/batch-governance/jobs" not in path:
                route.continue_(); return
            if path.endswith("/999999"):
                route.fulfill(json=fixture)
            else:
                query = parse_qs(urlparse(route.request.url).query)
                offset = int(query.get("offset", ["0"])[0])
                limit = int(query.get("limit", ["20"])[0])
                route.fulfill(json={"items": [dict(fixture, job_id=999999+i) for i in range(offset, min(offset+limit, 45))], "total": 45})

        page.route("**/api/v1/**", mock)
        page.goto(PAGE_URL, wait_until="domcontentloaded")
        expect(page.locator(".gr-table tbody tr")).to_have_count(20, timeout=30000)
        page.locator(".gr-footer").get_by_role("button", name="下一页").click()
        expect(page.locator(".gr-footer")).to_contain_text("第 2 / 3 页")
        page.goto(f"{PAGE_URL}?job=999999", wait_until="domcontentloaded")
        main_dialog = page.locator(".gr-modal").first
        expect(main_dialog).to_be_visible()
        main_dialog.get_by_role("button", name="股票治理成果", exact=True).click()
        detail = page.locator(".gr-detail-modal")
        expect(detail.locator("tbody tr")).to_have_count(20)
        detail.get_by_role("button", name="下一页明细").click()
        expect(detail).to_contain_text("测试股票20")
        expect(detail).not_to_contain_text("测试股票0")
        detail.get_by_role("button", name="下一页明细").click()
        expect(detail.locator("tbody tr")).to_have_count(5)
        page.set_viewport_size({"width": 600, "height": 900})
        assert detail.evaluate("node => node.scrollWidth <= node.clientWidth + 2"), "报告明细窄屏溢出"
        page.screenshot(path=str(OUTPUT / "stock-detail-mobile.png"))
        page.keyboard.press("Escape")
        expect(detail).to_have_count(0)
        page.keyboard.press("Escape")
        expect(page.locator(".gr-modal-backdrop")).to_have_count(0)
        checks.extend(["报告列表分页", "45只股票明细分页", "窄屏无横向溢出", "只读页面无任何写请求"])
        assert not writes, writes
        assert not errors, errors
        browser.close()
    result = {"passed": True, "checks": checks, "actual_reports": [528, 525], "writes": writes, "page_errors": errors}
    (OUTPUT / "verification.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
