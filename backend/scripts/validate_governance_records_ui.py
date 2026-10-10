"""Read-only report acceptance and intercepted start/polling interaction.

Real production tasks are never submitted or restarted by this script.
"""
from __future__ import annotations

import json
from pathlib import Path

import requests
from playwright.sync_api import expect, sync_playwright
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry


OUTPUT = Path(__file__).resolve().parents[1] / "validation" / "governance-records-followup"
PAGE_URL = "http://127.0.0.1:5173/#/operations/records"
API_URL = "http://127.0.0.1:8000/api/v1/stocks/batch-governance/jobs"


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    errors: list[str] = []
    session = requests.Session()
    session.mount("http://", HTTPAdapter(max_retries=Retry(total=3, backoff_factor=0.5, allowed_methods=frozenset({"GET"}))))
    session.get(API_URL, params={"limit": 1}, timeout=30).raise_for_status()
    with sync_playwright() as p:
        browser = p.chromium.launch(channel="msedge", headless=True)
        page = browser.new_page(viewport={"width": 1600, "height": 1100})
        page.on("pageerror", lambda error: errors.append(str(error)))
        page.goto(PAGE_URL, wait_until="domcontentloaded")
        search = page.get_by_placeholder("搜索股票、批次号或市场")
        expect(search).to_be_visible(timeout=60000)
        for job_id, keyword in ((528, "000159"), (525, "ai-boards-20261009")):
            response = session.get(f"{API_URL}/{job_id}", timeout=30)
            response.raise_for_status()
            report = response.json()["governance_report"]
            search.fill(keyword)
            row = page.locator(".gr-table tbody tr").filter(has_text=f"任务 #{job_id}")
            expect(row).to_be_visible(timeout=30000)
            row.get_by_role("button", name="查看报告与成果").click()
            dialog = page.get_by_role("dialog", name=f"治理报告 · 任务 #{job_id}")
            expect(dialog).to_be_visible(timeout=30000)
            expect(dialog.locator(".gr-interpretation")).to_contain_text("流程进度 100% 不代表数据全部完成")
            expect(dialog.locator(".gr-summary")).to_contain_text("本任务 Skill 执行")
            skill_cell = dialog.locator(".gr-summary div").filter(has_text="本任务 Skill 执行")
            expect(skill_cell.locator("strong")).to_have_text(str(report["summary"]["skill_execution_count"]))
            chunks = report["stage_results"]["DOCUMENT_CHUNKS"]["market_runs"][0]
            expect(dialog).to_contain_text(f"文档覆盖 {chunks['documents_with_chunks']}/{chunks['documents_total']} 份")
            expect(dialog).to_contain_text(f"新增 {chunks['created_chunk_count']} 条切片")
            if job_id == 528:
                expect(dialog).to_contain_text("38 个实体、70 条关系")
                expect(dialog).to_contain_text("配置范围：政策与外部事件、公告")
                dialog.get_by_text("业务数据采集 · 6 个检查项", exact=True).click()
                expect(dialog).to_contain_text("问董秘等待后台采集（任务 #527）")
            else:
                stage_section = dialog.locator(".gr-section").filter(has=page.get_by_role("heading", name="阶段进度", exact=True))
                skipped = stage_section.locator("tbody tr").filter(has_text="业务数据采集")
                expect(skipped).to_contain_text("不适用")
                expect(dialog).to_contain_text("本次未执行；保留已有数据")
                expect(dialog).to_contain_text("数值有效性检查未通过")
            page.screenshot(path=str(OUTPUT / f"report-{job_id}.png"))
            dialog.get_by_role("button", name="关闭治理报告").click()
        page.close()

        # All governance API calls on this new page are fulfilled locally.
        page = browser.new_page(viewport={"width": 1440, "height": 1000})
        page.on("pageerror", lambda error: errors.append(str(error)))
        state = {"started": False, "polls": 0, "start_calls": 0, "result_status": "PARTIAL"}
        record = {
            "job_id": 999999, "pipeline_run_id": 999999, "governance_batch_id": "browser-isolated-test",
            "stock_count": 1, "governance_mode": "AI_AGENT_SKILL_GOVERNANCE",
            "effective_options": {"stocks": [{"market": "CN_A", "symbol": "000159"}]},
        }

        def intercept(route):
            if route.request.method == "POST":
                assert route.request.url.endswith("/999999/start"), route.request.url
                state["started"] = True
                state["start_calls"] += 1
                payload = {**record, "status": "PENDING", "job_status": "PENDING", "progress": 0, "current_stage": "QUEUED"}
            else:
                if state["started"]:
                    state["polls"] += 1
                lifecycle = "COMPLETED" if state["polls"] >= 3 else "RUNNING" if state["polls"] >= 2 else "PENDING"
                payload = {"items": [{**record, "status": state["result_status"] if lifecycle == "COMPLETED" else lifecycle,
                                       "job_status": lifecycle, "result_status": state["result_status"] if lifecycle == "COMPLETED" else None,
                                       "progress": 100 if lifecycle == "COMPLETED" else 45 if lifecycle == "RUNNING" else 0,
                                       "current_stage": "COMPLETE" if lifecycle == "COMPLETED" else "BUSINESS_DATA" if lifecycle == "RUNNING" else "QUEUED"}],
                           "total": 1}
            route.fulfill(status=202 if route.request.method == "POST" else 200, json=payload)

        page.route("**/api/v1/stocks/batch-governance/jobs**", intercept)
        page.goto(PAGE_URL, wait_until="domcontentloaded")
        start = page.get_by_role("button", name="立即执行", exact=True)
        expect(start).to_be_visible(timeout=30000)
        start.click()
        notice = page.locator(".gr-notice")
        expect(notice).to_contain_text("已提交后台队列", timeout=15000)
        page.screenshot(path=str(OUTPUT / "start-queued.png"))
        expect(notice).to_contain_text("正在执行", timeout=15000)
        page.screenshot(path=str(OUTPUT / "start-running.png"))
        expect(notice).to_contain_text("流程已结束", timeout=15000)
        expect(notice).to_contain_text("部分完成")
        expect(start).to_have_count(0)
        page.screenshot(path=str(OUTPUT / "start-completed.png"))
        state["result_status"] = "FAILED"
        page.reload(wait_until="domcontentloaded")
        expect(page.locator(".gr-table")).to_contain_text("执行失败", timeout=30000)
        expect(page.get_by_role("button", name="立即执行", exact=True)).to_have_count(0)
        expect(page.get_by_role("button", name="重试", exact=True)).to_be_visible()
        assert state["start_calls"] == 1, state
        assert not errors, errors
        print(json.dumps({"passed": True, "actual_reports": [528, 525], "isolated_start_calls": state["start_calls"],
                          "page_errors": errors, "screenshots": str(OUTPUT)}, ensure_ascii=False))
        browser.close()


if __name__ == "__main__":
    main()
