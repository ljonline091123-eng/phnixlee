"""Browser acceptance for the execution workbench/report split.

All write requests are intercepted. This script never starts a production job.
"""
from __future__ import annotations

import json
from pathlib import Path
from urllib.parse import parse_qs, urlparse

from playwright.sync_api import expect, sync_playwright


OUTPUT = Path(__file__).resolve().parents[1] / "validation" / "governance-workbench"
PAGE = "http://127.0.0.1:5173/#/operations/workbench?job=910001"


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    errors: list[str] = []
    writes: list[str] = []
    state = {"continued": False, "polls": 0, "continue_calls": 0, "keys": [], "start_calls": 0}
    drafts = []

    base = {
        "job_id": 910001,
        "status": "PARTIAL",
        "job_status": "COMPLETED",
        "result_status": "PARTIAL",
        "stock_count": 2,
        "governance_mode": "AI_AGENT_SKILL_GOVERNANCE",
        "progress": 100,
        "current_stage": "COMPLETE",
        "created_at": "2026-10-12T15:00:00+08:00",
        "effective_options": {
            "collect_business_data": True,
            "structure_documents": True,
            "export_lakehouse": True,
            "archive_chunks": True,
            "run_graph": True,
            "governance_mode": "AI_AGENT_SKILL_GOVERNANCE",
        },
        "stage_results": {
            "BUSINESS_DATA": {"status": "SUCCESS", "message": "业务数据采集完成", "completed_items": [{"label": "行情", "reason": "已采集"}]},
            "STRUCTURED_EVENTS": {"status": "PARTIAL", "message": "3篇完成，1篇待处理", "completed_items": [{"label": "公告", "reason": "已抽取"}], "incomplete_items": [{"label": "新闻正文", "reason": "远端正文暂不可用"}]},
            "DOCUMENT_CHUNKS": {"status": "SUCCESS", "message": "切片归档完成"},
            "KNOWLEDGE_GRAPH": {"status": "PARTIAL", "message": "候选事实等待人工确认"},
        },
        "errors": ["1篇新闻正文暂不可用"],
    }

    def running_job(advance=False):
        if advance:
            state["polls"] += 1
        completed = state["polls"] >= 4
        return {
            **base,
            "job_id": 910002,
            "status": "SUCCESS" if completed else "RUNNING",
            "job_status": "COMPLETED" if completed else "RUNNING",
            "result_status": "SUCCESS" if completed else None,
            "progress": 100 if completed else 45,
            "current_stage": "COMPLETE" if completed else "STRUCTURED_EVENTS",
            "document_progress": {
                "total": 4, "processed": 4 if completed else 2, "succeeded": 4 if completed else 2,
                "failed": 0, "partial": 0, "skipped": 0, "phase": "STRUCTURED_EXTRACTION",
                "current_document": {"title": "测试公告原文", "source_table": "stock_notice", "record_id": 77, "market": "CN_A", "symbol": "000159"},
            },
            "errors": [],
            "stage_results": {"STRUCTURED_EVENTS": {"status": "SUCCESS" if completed else "RUNNING", "message": "4篇分析完成" if completed else "正在分析第3篇"}},
        }

    def intercept(route):
        url, method = route.request.url, route.request.method
        if method == "POST":
            writes.append(url)
        if url.endswith("/skill-optimization"):
            drafts.append({"id": 1, "skill_code": "NEWS_NOTICE_EVENT_GOVERNOR", "skill_name": "新闻公告治理", "status": "PENDING_REVIEW", "rationale": "补取缺失正文", "proposed_instructions": "核对完整原文，保留证据与历史版本。"})
            route.fulfill(json={"drafts": drafts, "history": [], "continuations": []})
        elif url.endswith("/skill-optimization/1/test"):
            drafts[0]["validation"] = {"status": "PASSED", "scope": "离线契约检查，不代表远程全量采集成功", "checks": []}
            route.fulfill(json=drafts[0]["validation"])
        elif url.endswith("/skill-optimization/1/review"):
            drafts[0]["status"] = "APPROVED"
            route.fulfill(json={"drafts": drafts, "history": [], "continuations": []})
        elif url.endswith("/continuation-plan"):
            route.fulfill(json={"can_continue": True, "notes": [], "reasons": [{"stage": "STRUCTURED_EVENTS", "reason": "补齐1篇缺失正文"}],
                                "request": {"structure_documents": True, "run_graph": True, "business_types": []}})
        elif url.endswith("/910001/continue"):
            state["continue_calls"] += 1
            state["keys"].append(route.request.post_data_json["idempotency_key"])
            if state["continue_calls"] == 1:
                route.fulfill(status=503, json={"detail": "续作服务暂不可用，请重试"})
                return
            state["continued"] = True
            route.fulfill(status=202, json={**running_job(), "job_status": "PENDING", "status": "PENDING", "progress": 0, "current_stage": "QUEUED"})
        elif url.endswith("/910001"):
            route.fulfill(json={**base, "governance_report": {"actions": {"drafts": drafts, "history": [], "continuations": []}}})
        elif url.endswith("/910002"):
            route.fulfill(json=running_job(advance=True))
        elif url.endswith("/910003/start"):
            state["start_calls"] += 1
            route.fulfill(json={**base, "job_id": 910003, "status": "RUNNING", "job_status": "RUNNING", "progress": 35})
        elif url.endswith("/910003"):
            route.fulfill(json={**base, "job_id": 910003, "status": "SUCCESS" if state["start_calls"] else "PENDING", "job_status": "COMPLETED" if state["start_calls"] else "PENDING"})
        elif "/stocks/batch-governance/jobs?" in url:
            current = running_job() if state["continued"] else base
            query = parse_qs(urlparse(url).query)
            offset = int(query.get("offset", ["0"])[0])
            route.fulfill(json={"items": [{**current, "job_id": current["job_id"] + i} for i in range(offset, min(offset + 20, 45))], "total": 45})
        elif method == "GET":
            route.continue_()
        else:
            route.abort()

    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(channel="msedge", headless=True)
        page = browser.new_page(viewport={"width": 1500, "height": 1000})
        page.on("pageerror", lambda error: errors.append(str(error)))
        page.route("**/api/v1/**", intercept)
        page.goto(PAGE, wait_until="domcontentloaded")
        expect(page.get_by_role("heading", name="治理工作台")).to_be_visible(timeout=30000)
        dialog = page.get_by_role("dialog", name="治理窗口 · 任务 #910001")
        expect(dialog).to_be_visible(timeout=30000)
        expect(dialog).to_contain_text("治理节点")
        dialog.get_by_role("button", name="Agent与Skill治理", exact=False).click()
        skill_detail = page.get_by_role("dialog", name="Agent与Skill治理", exact=True)
        skill_detail.get_by_role("button", name="生成Skill优化草稿").click()
        expect(skill_detail.get_by_role("button", name="测试优化草稿")).to_be_enabled()
        expect(skill_detail.get_by_role("button", name="审核通过并启用")).to_be_disabled()
        skill_detail.get_by_role("button", name="测试优化草稿").click()
        expect(skill_detail.get_by_role("button", name="审核通过并启用")).to_be_enabled()
        skill_detail.get_by_role("button", name="审核通过并启用").click()
        expect(skill_detail).to_contain_text("审核通过")
        # The response can update the draft before the follow-up report read
        # completes. Wait for the whole action before closing the dialog.
        expect(skill_detail.get_by_role("button", name="关闭节点明细")).to_be_enabled(timeout=15000)
        page.keyboard.press("Escape")
        expect(skill_detail).to_have_count(0)
        dialog.get_by_role("button", name="新闻公告知识结构化", exact=False).click()
        detail = page.get_by_role("dialog", name="新闻公告知识结构化")
        expect(detail).to_contain_text("远端正文暂不可用")
        page.keyboard.press("Escape")
        expect(detail).to_have_count(0)
        dialog.get_by_role("button", name="继续治理").click()
        confirm = page.get_by_role("dialog", name="确认定向续作计划")
        expect(confirm).to_contain_text("补齐1篇缺失正文")
        confirm.get_by_role("button", name="确认并开始续作").click()
        expect(confirm.get_by_role("alert")).to_contain_text("续作服务暂不可用")
        expect(confirm).to_be_visible()
        confirm.get_by_role("button", name="确认并开始续作").click()
        next_dialog = page.get_by_role("dialog", name="治理窗口 · 任务 #910002")
        expect(next_dialog).to_be_visible(timeout=15000)
        expect(next_dialog).to_contain_text("测试公告原文", timeout=15000)
        next_dialog.get_by_role("button", name="新闻公告知识结构化", exact=False).click()
        live_detail = page.get_by_role("dialog", name="新闻公告知识结构化", exact=True)
        expect(live_detail).to_contain_text("4篇分析完成", timeout=20000)
        semantic_link = live_detail.get_by_role("link", name="打开新闻公告交互治理窗口")
        semantic_scope = parse_qs(urlparse(semantic_link.get_attribute("href").split("#", 1)[1]).query)
        assert semantic_scope == {"job": ["910002"], "market": ["CN_A"], "symbol": ["000159"]}, semantic_scope
        page.keyboard.press("Escape")
        expect(next_dialog).to_contain_text("已完成")
        page.screenshot(path=str(OUTPUT / "continued-completed.png"), full_page=True)
        assert state["continued"], state
        assert state["keys"][0] == state["keys"][1], "网络失败重试必须复用幂等键"
        page.set_viewport_size({"width": 600, "height": 900})
        assert next_dialog.evaluate("node => node.scrollWidth <= node.clientWidth + 2"), "窄屏治理窗口溢出"
        page.keyboard.press("Escape")
        page.locator(".gw-pagination").get_by_role("button", name="下一页").click()
        expect(page.locator(".gw-pagination")).to_contain_text("第 2 / 3 页")
        page.get_by_label("搜索治理任务").fill("000159")
        expect(page.locator(".gw-pagination")).to_contain_text("第 1 / 3 页")
        page.goto("http://127.0.0.1:5173/#/operations/workbench?job=910003", wait_until="domcontentloaded")
        pending_dialog = page.get_by_role("dialog", name="治理窗口 · 任务 #910003")
        expect(pending_dialog).to_be_visible()
        pending_dialog.get_by_role("button", name="立即执行").click()
        expect(pending_dialog).to_contain_text("已完成", timeout=20000)
        assert state["start_calls"] == 1
        assert not errors, errors
        browser.close()

    result = {"passed": True, "isolated_writes": writes, "page_errors": errors,
              "checks": ["深链接打开治理窗口", "节点明细弹窗", "续作计划确认", "续作错误就地显示", "幂等重试", "创建续作任务", "实时文档进度", "明细随轮询更新", "终态可见", "Skill草稿测试审核", "逐层Esc关闭", "窄屏无溢出", "搜索分页", "立即执行", "专项治理链接携带当前任务及股票范围"]}
    (OUTPUT / "verification.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
