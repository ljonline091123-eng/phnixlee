"""Browser acceptance of the simplified workbench; never approves a live pick."""
import argparse
import asyncio
import json
from pathlib import Path

from playwright.async_api import async_playwright

OUTPUT = Path(__file__).resolve().parents[1] / "validation" / "twenty-stock-chain"


async def main(*, require_completed=False, start_real=False):
    OUTPUT.mkdir(parents=True, exist_ok=True)
    errors, checks = [], []
    async with async_playwright() as playwright:
        browser = await playwright.chromium.launch(channel="msedge", headless=True)
        page = await browser.new_page(viewport={"width": 1440, "height": 1000}, device_scale_factor=1)
        page.on("pageerror", lambda error: errors.append(str(error)))
        await page.goto("http://127.0.0.1:5173/#/research/selection")
        await page.get_by_role("heading", name="20 股全链路工作台").wait_for()
        if start_real:
            async with page.expect_response(lambda response: response.url.endswith("/selection/runs/start") and response.request.method == "POST", timeout=15000) as pending:
                await page.locator(".batch-actions .primary-button").click()
            response = await pending.value
            assert response.status == 202
            queued = await response.json()
            assert queued["status"] == "QUEUED" and queued["candidate_count"] == 0
            await page.get_by_role("progressbar", name="分析进度").wait_for()
            await page.reload()
            await page.get_by_role("progressbar", name="分析进度").wait_for()
            checks.append(f"页面真实发起后台任务#{queued['id']}，202受理、进度条和刷新恢复通过")
        await page.get_by_role("button", name="查看20股数据验收", exact=True).click()
        await page.get_by_role("heading", name="20股数据验收", exact=True).wait_for()
        data_card = page.locator(".batch-card").filter(has=page.get_by_role("heading", name="20股数据验收", exact=True))
        assert await data_card.locator("tbody tr").count() == 20
        assert await data_card.get_by_text("链路通过", exact=True).count() == 20
        checks.append("20只股票数据验收逐行通过")
        await page.screenshot(path=str(OUTPUT / "workbench-data-desktop.png"), full_page=True)
        await page.get_by_role("button", name="收起数据验收", exact=True).click()
        await page.screenshot(path=str(OUTPUT / "workbench-desktop.png"), full_page=True)
        if require_completed:
            assert await page.get_by_role("button", name="分析与证据", exact=True).count() == 20
            await page.get_by_role("button", name="分析与证据", exact=True).first.click()
            dialog = page.get_by_role("dialog")
            await dialog.wait_for()
            assert "模型未完成" not in await dialog.inner_text()
            await page.screenshot(path=str(OUTPUT / "workbench-analysis.png"))
            for tab in ("数据事实", "原文与引用", "关系与血缘"):
                await dialog.get_by_role("button", name=tab, exact=True).click()
                if tab == "数据事实":
                    assert await dialog.locator(".batch-fact").count() > 0
                    assert await dialog.locator(".batch-fact dd").count() > 0
                    assert await dialog.get_by_text("营业收入", exact=True).count() > 0
                    assert "亿元" in await dialog.inner_text()
                elif tab == "原文与引用":
                    assert await dialog.locator("details").count() > 0
                    await dialog.locator("details").first.locator("summary").click()
                    await dialog.get_by_text("展示数据库中保存的来源内容。", exact=True).first.wait_for()
                else:
                    assert "条血缘记录" in await dialog.inner_text()
                await page.screenshot(path=str(OUTPUT / f"workbench-detail-{tab}.png"))
            checks.append("20只模型结果及结论、数值、原文、血缘四个详情页签")
            await page.keyboard.press("Escape")
            assert await page.get_by_role("dialog").count() == 0
        else:
            await page.reload()
            await page.get_by_role("heading", name="20 股全链路工作台").wait_for()
            checks.append("刷新恢复当前批次")
        await page.get_by_role("button", name="高级工作台", exact=True).click()
        await page.get_by_role("button", name="← 返回20股简洁工作台", exact=True).wait_for()
        await page.get_by_role("button", name="← 返回20股简洁工作台", exact=True).click()
        checks.append("保留高级工作台并支持返回")
        await page.set_viewport_size({"width": 390, "height": 844})
        await page.screenshot(path=str(OUTPUT / "workbench-mobile.png"), full_page=True)
        horizontal_overflow = await page.locator(".batch-workbench").evaluate("el => el.scrollWidth > el.clientWidth + 1")
        assert not horizontal_overflow, "workbench page overflows mobile viewport"
        checks.append("手机布局无整页横向溢出")
        # Empty installation must render a useful state rather than crash.
        await page.route("**/api/v1/selection/acceptance", lambda route: route.fulfill(json={
            "prepared": False, "graph_id": None, "knowledge_base_id": None, "market": "CN_A",
            "symbols": [], "rows": [], "scope_notes": [], "datasets": {}, "prepared_at": None,
        }))
        await page.reload()
        await page.get_by_role("heading", name="20 股全链路工作台").wait_for()
        await page.wait_for_timeout(500)
        assert await page.locator(".batch-actions .primary-button").is_disabled()
        checks.append("未准备数据时可显示并禁止分析")
        assert not errors, errors
        await browser.close()
    report = {"passed": True, "checks": checks, "page_errors": errors,
              "scope": "read-only browser validation; no production approval"}
    (OUTPUT / "ui-acceptance.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=True), flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--require-completed", action="store_true")
    parser.add_argument("--start-real", action="store_true", help="Authorize one real model run through the UI (no pick approval)")
    options = parser.parse_args()
    asyncio.run(main(require_completed=options.require_completed, start_real=options.start_real))
