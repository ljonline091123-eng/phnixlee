"""Browser acceptance using the running app and persisted issuer Q&A."""
import json
from pathlib import Path
import re

from playwright.sync_api import expect, sync_playwright


OUTPUT = Path(__file__).resolve().parents[1] / "validation" / "hk-investor-qa-20261008"


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    with sync_playwright() as p:
        browser = p.chromium.launch(channel="msedge", headless=True)
        page = browser.new_page(viewport={"width": 1440, "height": 1100})
        errors = []
        page.on("pageerror", lambda error: errors.append(str(error)))
        page.goto("http://127.0.0.1:5173/#/data/master", wait_until="domcontentloaded")
        search = page.get_by_placeholder("按代码或名称模糊搜索")
        expect(search).to_be_visible(timeout=60000)
        search.locator("xpath=..").locator("select").first.select_option("HK")
        expect(search.locator("xpath=..").locator("select").first).to_have_value("HK")

        def open_stock(symbol):
            search.fill(symbol)
            with page.expect_response(lambda response: "/api/v1/stocks?" in response.url and f"keyword={symbol}" in response.url, timeout=60000) as loaded:
                search.locator("xpath=..").get_by_role("button", name="查询", exact=True).click()
            assert loaded.value.ok, loaded.value.status
            if not page.locator(".symbol-link").filter(has_text=symbol).count():
                print("Stock search response:", loaded.value.text()[:1000], flush=True)
            page.locator(".symbol-link").filter(has_text=symbol).click(timeout=60000)
            drawer = page.locator(".stock-detail-drawer")
            expect(drawer.locator(".detail-market-header h2")).to_contain_text(symbol)
            drawer.locator(".detail-primary-tabs").get_by_role("button", name="研究", exact=True).click(timeout=60000)
            return drawer, drawer.locator(".research-qa-section")

        drawer, qa = open_stock("09868")
        expect(qa).to_contain_text("投资者问答", timeout=60000)
        expect(qa.locator(".research-qa-item")).to_have_count(4, timeout=60000)
        expect(qa).to_contain_text("官网常见问答")
        expect(qa).to_contain_text("发布日期未披露")
        qa.get_by_role("button", name=re.compile("更多")).click()
        expect(qa.locator(".research-qa-item")).to_have_count(9)
        page.wait_for_function("""async () => {
            const r = await fetch('http://127.0.0.1:8000/api/v1/stocks/HK/09868/research/qa/sync');
            return (await r.json()).status === 'COMPLETE';
        }""", timeout=90000, polling=1000)
        expect(qa).to_contain_text("不是实时问董秘", timeout=15000)
        qa.scroll_into_view_if_needed()
        page.screenshot(path=str(OUTPUT / "xiaopeng-faq-list.png"))
        qa.locator(".research-qa-item").first.click()
        detail = page.locator(".research-detail-dialog")
        expect(detail).to_be_visible()
        expect(detail.locator(".research-qa-detail .answer p")).not_to_be_empty()
        expect(detail).to_contain_text("未披露")
        source_link = detail.locator('a[href^="https://ir.xiaopeng.com/"]')
        expect(source_link.first).to_be_visible()
        page.screenshot(path=str(OUTPUT / "xiaopeng-faq-detail.png"))
        detail.get_by_role("button", name="关闭研究详情").click()

        qa.get_by_role("button", name="更新问答", exact=True).click()
        # Wait for the actual durable worker via the real sync-status API.
        page.wait_for_function("""async () => {
            const r = await fetch('http://127.0.0.1:8000/api/v1/stocks/HK/09868/research/qa/sync');
            const d = await r.json();
            return d.status === 'COMPLETE' && d.job_id === null;
        }""", timeout=90000, polling=1000)
        expect(qa.locator(".research-qa-item")).to_have_count(9, timeout=15000)
        expect(qa.get_by_role("button", name="更新问答", exact=True)).to_be_enabled()
        drawer.locator(".detail-head-actions").get_by_role("button", name="关闭", exact=True).click()

        drawer, qa = open_stock("02533")
        expect(qa).to_contain_text("尚未找到可采集的公开问答文字原文", timeout=60000)
        expect(qa.locator(".research-qa-item")).to_have_count(0)
        expect(qa).to_contain_text("不代表公司没有举行投资者交流")
        expect(qa.locator('a[href^="https://ir.blacksesame.com/"]').first).to_be_visible()
        qa.scroll_into_view_if_needed()
        page.screenshot(path=str(OUTPUT / "black-sesame-source-gap.png"))
        drawer.locator(".detail-head-actions").get_by_role("button", name="关闭", exact=True).click()

        drawer, qa = open_stock("09888")
        expect(qa).to_contain_text("英文原文", timeout=60000)
        expect(qa.locator(".research-qa-item")).to_have_count(4)
        qa.get_by_role("button", name=re.compile("更多")).click()
        expect(qa.locator(".research-qa-item")).to_have_count(22)
        qa.scroll_into_view_if_needed()
        page.screenshot(path=str(OUTPUT / "baidu-original-language.png"))
        assert not errors, errors
        result = {"passed": True, "page_errors": errors,
                  "checks": ["issuer_faq_list", "full_answer_dialog", "source_links", "undisclosed_dates",
                             "manual_refresh_durable_worker", "missing_source_state", "english_original"],
                  "screenshots": [path.name for path in OUTPUT.glob("*.png")]}
        (OUTPUT / "browser-result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
        print(json.dumps(result, ensure_ascii=False))
        browser.close()


if __name__ == "__main__":
    main()
