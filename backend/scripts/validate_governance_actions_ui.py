"""Read real stage evidence; exercise continuation/review UI with intercepted writes."""
from __future__ import annotations

import argparse
from copy import deepcopy
import json
from pathlib import Path
import re
from urllib.parse import urlparse

import requests
from playwright.sync_api import expect, sync_playwright

API = "http://127.0.0.1:8000/api/v1/stocks/batch-governance/jobs"
PAGE = "http://127.0.0.1:5173/#/operations/records"
OUTPUT = Path(__file__).resolve().parents[1] / "validation" / "governance-actions"


def main(job_id: int):
    OUTPUT.mkdir(parents=True, exist_ok=True)
    session = requests.Session()
    response = session.get(f"{API}/{job_id}", timeout=30)
    response.raise_for_status()
    actual = response.json()
    assert actual["job_status"] == "COMPLETED"
    report = actual["governance_report"]
    errors, checked = [], []
    with sync_playwright() as p:
        browser = p.chromium.launch(channel="msedge", headless=True)
        page = browser.new_page(viewport={"width": 1600, "height": 1100})
        page.on("pageerror", lambda exc: errors.append(str(exc)))
        page.goto(PAGE, wait_until="domcontentloaded")
        page.get_by_placeholder("搜索股票、批次号或市场").fill("000159")
        row = page.locator(".gr-table tbody tr").filter(has_text=f"任务 #{job_id}")
        expect(row).to_be_visible(timeout=60000)
        row.get_by_role("button", name="查看报告与成果").click()
        dialog = page.get_by_role("dialog", name=f"治理报告 · 任务 #{job_id}")
        expect(dialog).to_be_visible(timeout=30000)
        stage_section = dialog.locator(".gr-section").filter(has=page.get_by_role("heading", name="阶段成果明细", exact=True))
        for table in ("stock_news", "stock_notice"):
            stage = report["stage_results"]["STRUCTURED_EVENTS"]
            item = next(item for item in stage["completed_items"] if item["details"]["source_table"] == table and item["record_count"])
            response = session.get(f"{API}/{job_id}/stage-preview", params={"stage": "STRUCTURED_EVENTS", "item_code": item["code"]}, timeout=30)
            response.raise_for_status()
            facts = response.json()["items"]
            for fact in facts:
                assert fact["status"] == "PENDING"
                assert fact["properties_json"]["content_scope"] == "FULL_BODY"
                assert any(fact["properties_json"]["evidence_quote"] in ev["content"] for ev in fact["evidence"])
                assert fact["properties_json"]["model_call_log_id"] and fact["properties_json"]["skill_execution_run_id"]
            summary = stage_section.locator("summary").filter(has_text="新闻公告结构化事件")
            if not summary.locator("..").evaluate("node => node.open"):
                summary.click()
            item_row = stage_section.locator("tbody tr").filter(has=page.get_by_text(item["label"], exact=True)).first
            item_row.get_by_role("button", name="查看数据与证据").click()
            preview = dialog.locator('[aria-label="阶段成果数据预览"]')
            expect(preview).to_contain_text("原文证据", timeout=30000)
            props = preview.locator(".gr-data-fields > div").filter(has=page.locator("dt").filter(has_text="结构化事实字段")).first
            props.locator("dd > details > summary").click()
            expect(props).to_contain_text(facts[0]["properties_json"]["evidence_quote"])
            fields = props.locator("dd > details > .gr-data-fields")
            expect(fields).to_be_visible()
            assert fields.evaluate("node => getComputedStyle(node).display") == "block"
            assert fields.bounding_box()["width"] > 500, "Nested fields must have readable width"
            assert preview.evaluate("node => node.scrollWidth <= node.clientWidth + 2"), "Preview must not overflow horizontally"
            preview.scroll_into_view_if_needed()
            page.screenshot(path=str(OUTPUT / f"{table}-facts.png"))
            checked.append({"table": table, "fact_ids": [fact["id"] for fact in facts]})
            dialog.get_by_role("button", name="关闭成果详情").click()

        # Existing F10 payloads can contain NaN. The API reports these as missing,
        # while the preview remains readable and never invents a zero value.
        summary = stage_section.locator("summary").filter(has_text="业务数据采集")
        if not summary.locator("..").evaluate("node => node.open"):
            summary.click()
        f10 = next(item for item in report["stage_results"]["BUSINESS_DATA"]["incomplete_items"] if item["code"] == "CN_A:000159:F10")
        item_row = stage_section.locator("tbody tr").filter(has=page.get_by_text(f10["label"], exact=True)).first
        item_row.get_by_role("button", name="查看数据与证据").click()
        preview = dialog.locator('[aria-label="阶段成果数据预览"]')
        expect(preview).to_contain_text("无效数值数量", timeout=30000)
        expect(preview).to_contain_text("未修改原始数据")
        preview.get_by_text("查看本轮验收记录与质量", exact=True).click()
        preview.scroll_into_view_if_needed()
        page.screenshot(path=str(OUTPUT / "f10-stage-preview.png"))
        dialog.get_by_role("button", name="关闭成果详情").click()

        # Real version-specific lake preview and next-page request.
        summary = stage_section.locator("summary").filter(has_text="湖仓发布")
        summary.click()
        item_row = stage_section.locator("tbody tr").filter(has=page.get_by_text("公告", exact=True)).first
        item_row.get_by_role("button", name="查看数据与证据").click()
        preview = dialog.locator('[aria-label="阶段成果数据预览"]')
        expect(preview).to_contain_text("第 1 页", timeout=30000)
        preview.get_by_role("button", name="下一页", exact=True).click()
        expect(preview).to_contain_text("第 2 页", timeout=30000)
        page.screenshot(path=str(OUTPUT / "lake-page-2.png"))
        dialog.get_by_role("button", name=re.compile(r"任务 #528\b")).click()
        expect(page.get_by_role("dialog", name="治理报告 · 任务 #528")).to_be_visible(timeout=30000)
        page.screenshot(path=str(OUTPUT / "history-report-528.png"))
        page.close()

        # Every governance request on this page is fulfilled locally; no test
        # approval/rejection modifies a production Skill or submits a real job.
        page = browser.new_page(viewport={"width": 1600, "height": 1100})
        page.on("pageerror", lambda exc: errors.append(str(exc)))
        first, following = 999901, 999902
        state = {"continued": False, "polls": 0, "posts": []}
        drafts = [{"id": i, "skill_code": "NEWS_NOTICE_EVENT_GOVERNOR", "skill_name": f"新闻公告结构化事件治理测试{i}",
                   "base_skill_version": "1.0.0", "current_version": "1.0.0", "status": "PENDING_REVIEW",
                   "rationale": json.dumps({"失败原因": "原文缺失", "优化建议": "先补取正文再抽取"}, ensure_ascii=False),
                   "current_instructions": "原SOP", "proposed_instructions": "原SOP\n先补取正文再抽取"} for i in (1, 2)]

        def actions():
            return {"history": [{"job_id": first, "status": "COMPLETED", "result_status": "PARTIAL"}], "drafts": drafts, "continuations": []}

        def record(identity):
            value = deepcopy(actual)
            value.update(job_id=identity, governance_batch_id="browser-actions-isolated", pipeline_run_id=identity)
            if identity == following:
                state["polls"] += 1
                value.update(job_status="COMPLETED" if state["polls"] > 3 else "RUNNING", status="PARTIAL" if state["polls"] > 3 else "RUNNING", progress=100 if state["polls"] > 3 else 55)
            value["governance_report"]["job"] = {key: item for key, item in value.items() if key != "governance_report"}
            value["governance_report"]["actions"] = actions()
            return value

        def intercept(route):
            path = urlparse(route.request.url).path
            suffix = path.split("/jobs", 1)[1]
            if route.request.method == "POST":
                state["posts"].append(suffix)
                if suffix.endswith("/continue"):
                    assert route.request.post_data_json["idempotency_key"]
                    state["continued"] = True
                    payload = record(following)
                elif suffix.endswith("/skill-optimization"):
                    payload = actions()
                elif suffix.endswith("/test"):
                    drafts[0]["validation"] = {"status": "PASSED", "scope": "隔离浏览器测试", "checks": [{"name": "原文证据门禁", "passed": True}]}
                    payload = drafts[0]["validation"]
                elif suffix.endswith("/review"):
                    selected = drafts[int(suffix.split("/")[-2]) - 1]
                    selected["status"] = "APPROVED" if route.request.post_data_json["decision"] == "APPROVE" else "REJECTED"
                    payload = actions()
                else:
                    raise AssertionError(path)
            elif suffix.endswith("/continuation-plan"):
                payload = {"can_continue": True, "notes": ["按失败原因定向续作，保留历史报告"], "reasons": [{"stage": "STRUCTURED_EVENTS", "reason": "正文缺失"}], "request": {"business_types": [], "structure_documents": True, "run_graph": False}}
            elif suffix in (f"/{first}", f"/{following}"):
                payload = record(int(suffix[1:]))
            else:
                payload = {"items": ([record(following)] if state["continued"] else []) + [record(first)], "total": 2 if state["continued"] else 1}
            route.fulfill(status=202 if suffix.endswith("/continue") else 200, json=payload)

        page.route("**/api/v1/stocks/batch-governance/jobs**", intercept)
        page.goto(PAGE, wait_until="domcontentloaded")
        page.get_by_role("button", name="查看报告与成果").click()
        dialog = page.get_by_role("dialog", name=f"治理报告 · 任务 #{first}")
        expect(dialog).to_be_visible(timeout=30000)
        dialog.get_by_role("button", name="生成并审核 Skill 优化").click()
        optimization = dialog.locator('[aria-label="Skill优化审核"]')
        expect(optimization).to_be_visible(timeout=30000)
        optimization.get_by_role("button", name="执行离线检查").first.click()
        approve = optimization.get_by_role("button", name="批准并启用新版本").first
        expect(approve).to_be_enabled(timeout=30000)
        approve.click()
        expect(optimization).to_contain_text("已批准", timeout=30000)
        optimization.get_by_role("button", name="驳回草稿").click()
        expect(optimization).to_contain_text("已驳回", timeout=30000)
        optimization.scroll_into_view_if_needed()
        page.screenshot(path=str(OUTPUT / "skill-review-actions.png"))
        dialog.get_by_role("button", name="继续治理", exact=True).click()
        plan = dialog.locator('[aria-label="定向继续治理计划"]')
        expect(plan).to_contain_text("正文缺失", timeout=30000)
        plan.get_by_role("button", name="执行定向续作并保留旧报告").click()
        expect(page.get_by_role("dialog", name=f"治理报告 · 任务 #{following}")).to_be_visible(timeout=30000)
        expect(page.locator(".gr-notice")).to_contain_text("流程已结束", timeout=30000)
        page.screenshot(path=str(OUTPUT / "continuation-new-report.png"))
        assert len(state["posts"]) == 5, state
        assert not errors, errors
        print(json.dumps({"passed": True, "actual_job_id": job_id, "actual_evidence_checks": checked,
                          "isolated_action_posts": state["posts"], "page_errors": errors, "screenshots": str(OUTPUT)}, ensure_ascii=False))
        browser.close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--job-id", type=int, default=533)
    main(parser.parse_args().job_id)
