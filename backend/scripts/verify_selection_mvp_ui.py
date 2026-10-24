"""Browser smoke check against a running local app; writes only screenshots by default.

Use --submit-rule-sample to additionally create one real, model-free queue task.
Existing candidates, decisions and historical runs are never changed.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from playwright.sync_api import expect, sync_playwright


def main() -> None:
    parser = argparse.ArgumentParser(description="选股 MVP 浏览器验证")
    parser.add_argument("--url", default="http://127.0.0.1:5173")
    parser.add_argument("--failed-run-id", type=int, default=13)
    parser.add_argument("--completed-run-id", type=int)
    parser.add_argument("--submit-rule-sample", action="store_true")
    parser.add_argument("--report-dir", type=Path, default=Path("validation/selection-mvp-20261022"))
    args = parser.parse_args()
    args.report_dir.mkdir(parents=True, exist_ok=True)
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(channel="msedge", headless=True)
        page = browser.new_page(viewport={"width": 1440, "height": 1000}, device_scale_factor=1)
        errors: list[str] = []
        page.on("pageerror", lambda error: errors.append(str(error)))
        page.goto(f"{args.url}/#/research/selection", wait_until="domcontentloaded")
        create = page.get_by_role("button", name="新建选股", exact=True)
        expect(create).to_be_enabled(timeout=60_000)
        completed_checks = {}
        if args.completed_run_id:
            page.get_by_label("历史分析批次").select_option(str(args.completed_run_id))
            summary = page.get_by_role("region", name="全链路进度")
            expect(summary).to_contain_text("20 / 20", timeout=30_000)
            expect(summary).to_contain_text("0 失败 · 0 待核对")
            pagination = page.get_by_role("navigation", name="分析结果分页")
            expect(pagination).to_contain_text("共 20 只 · 第 1 / 2 页")
            result_rows = page.locator(".batch-card .batch-table-wrap tbody tr")
            expect(result_rows).to_have_count(10)
            page.screenshot(path=str(args.report_dir / "05-twenty-stock-results.png"), full_page=True)
            pagination.get_by_role("button", name="下一页").click()
            expect(pagination).to_contain_text("第 2 / 2 页")
            expect(result_rows).to_have_count(10)
            pagination.get_by_role("button", name="上一页").click()
            result_rows.filter(has_text="000034").get_by_role("button", name="分析与证据").click()
            detail = page.get_by_role("dialog", name="神州数码分析与证据")
            expect(detail.locator(".analysis-dimension")).to_have_count(7)
            expect(detail.locator(".analysis-event").first).to_be_visible()
            page.screenshot(path=str(args.report_dir / "06-multisource-analysis.png"), full_page=True)
            detail.locator(".analysis-reference").first.click()
            source = detail.locator(".batch-source[open]").first
            expect(source.locator(".batch-source-content")).not_to_have_text("暂无正文", timeout=30_000)
            expect(source).to_contain_text("展示数据库中保存的来源内容", timeout=30_000)
            page.screenshot(path=str(args.report_dir / "07-cited-original.png"), full_page=True)
            detail.get_by_role("button", name="关系与血缘", exact=True).click()
            expect(detail).to_contain_text("图谱关系")
            expect(detail).to_contain_text("血缘追踪")
            expect(detail).to_contain_text("上下文版本")
            detail.get_by_role("button", name="关闭", exact=True).click()
            page.set_viewport_size({"width": 390, "height": 844})
            assert page.evaluate("document.documentElement.scrollWidth <= innerWidth")
            page.screenshot(path=str(args.report_dir / "08-mobile-results.png"), full_page=True)
            page.set_viewport_size({"width": 1440, "height": 1000})
            completed_checks = {"completed_run_id": args.completed_run_id, "twenty_passed": True,
                                "result_pagination": "PASSED", "seven_dimensions": "PASSED",
                                "cited_original": "PASSED", "graph_and_lineage": "PASSED"}
        if args.failed_run_id:
            page.get_by_label("历史分析批次").select_option(str(args.failed_run_id))
            recovery = page.get_by_role("region", name="分析失败处理")
            expect(recovery).to_be_visible(timeout=30_000)
            expect(recovery.get_by_role("button", name="重试 20 只失败股票")).to_be_enabled()
            recovery.locator("summary").click()
            expect(page.locator(".selection-failure-list article")).to_have_count(20)
            page.screenshot(path=str(args.report_dir / "01-failure-details.png"), full_page=True)

        create.click()
        dialog = page.get_by_role("dialog", name="新建选股", exact=True)
        search = dialog.get_by_label("选股股票搜索")
        search.fill("银行")
        all_check = dialog.locator(".selection-select-all input")
        expect(all_check).to_be_visible(timeout=30_000)
        boxes = dialog.locator(".selection-stock-results input[type=checkbox]")
        count = boxes.count()
        assert count > 1, count
        all_check.check()
        expect(dialog.locator(".selection-stock-selected button")).to_have_count(count)
        boxes.first.uncheck()
        expect(all_check).to_have_attribute("aria-checked", "mixed")
        assert all_check.evaluate("el => el.indeterminate")
        all_check.check()
        all_check.uncheck()
        expect(dialog.locator(".selection-stock-selected button")).to_have_count(0)

        search.fill("000026")
        expect(dialog.locator(".selection-stock-results label")).to_have_count(1)
        dialog.locator(".selection-stock-results input").check()
        expect(dialog.locator(".selection-stock-selected button")).to_have_count(1)
        expect(dialog.locator(".selection-scope-preview")).to_contain_text("篇投影文档", timeout=30_000)
        expect(dialog.locator(".selection-choice-count").first).not_to_contain_text("正在统计")
        submit = dialog.get_by_role("button", name="生成候选池", exact=True)
        expect(submit).to_be_enabled()
        page.screenshot(path=str(args.report_dir / "02-stock-and-evidence-options.png"), full_page=True)
        dialog.locator(".selection-scope-preview").scroll_into_view_if_needed()
        page.screenshot(path=str(args.report_dir / "03-evidence-coverage.png"), full_page=True)

        page.set_viewport_size({"width": 390, "height": 844})
        submit.scroll_into_view_if_needed()
        expect(submit).to_be_visible()
        overflow = page.evaluate("""() => ({width: innerWidth, scroll: document.documentElement.scrollWidth,
          elements: [...document.querySelectorAll('body *')].map(e => ({tag:e.tagName, cls:e.className,
          x:e.getBoundingClientRect().x, w:e.getBoundingClientRect().width, right:e.getBoundingClientRect().right}))
          .filter(e => e.w && (e.x < 0 || e.right > innerWidth + 1)).slice(0, 35)})""")
        assert overflow["scroll"] <= overflow["width"], json.dumps(overflow, ensure_ascii=True)
        page.screenshot(path=str(args.report_dir / "04-mobile-submit.png"), full_page=True)
        result = {"all_select_matches": count, "half_select": "PASSED", "scope_cards": "PASSED",
                  "submit_enabled": True, "mobile": "PASSED", "page_errors": errors, **completed_checks}
        if args.submit_rule_sample:
            dialog.get_by_label("调用模型综合分析候选证据").uncheck()
            with page.expect_response(lambda response: response.url.endswith("/selection/runs/start")
                                      and response.request.method == "POST", timeout=30_000) as request:
                submit.click()
            response = request.value
            assert response.status == 202, response.text()
            created = response.json()
            assert created["criteria_json"]["request"]["symbols"] == ["000026"]
            assert created["criteria_json"]["request"]["use_model"] is False
            expect(dialog).not_to_be_visible()
            result["created_rule_run_id"] = created["id"]
            result["created_status"] = created["status"]
        assert not errors, errors
        print(json.dumps(result, ensure_ascii=True))
        browser.close()


if __name__ == "__main__":
    main()
