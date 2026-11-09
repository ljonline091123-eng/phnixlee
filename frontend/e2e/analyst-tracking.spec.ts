import { expect, test, type Page, type APIRequestContext } from "@playwright/test";

const base = process.env.E2E_BASE_URL || "http://127.0.0.1:5173";
const api = process.env.E2E_API_BASE_URL || "http://127.0.0.1:8000/api/v1";

async function prepare(page: Page, request: APIRequestContext) {
  const runs = await (await request.get(`${api}/ai-stock-analyst/analyses?limit=50`)).json();
  const run = runs.find((r: { status: string }) => r.status === "COMPLETED");
  expect(run).toBeTruthy();
  await page.addInitScript(id => localStorage.setItem("ai-analyst-active", String(id)), run.id);
  const stamp = new Date().toISOString();
  const config = { enabled: true, price_lower: 6, price_upper: 8, volume_mode: "ANY", volume_ratio: 1.5,
    volume_window: 20, event_conditions: [], review_focus: "观察主营订单兑现", schedule_hour: 18, schedule_minute: 0 };
  const validation = { forecast_id: 901, state: "PASSED", detail: "发布后的收盘价达到上行目标", actual_price: 8.1, price_date: "2026-11-09" };
  const review = { id: 601, business_date: "2026-11-09", attempt: 1, status: "FAILED", created_at: stamp,
    model_instance_code: "TEST_AI", config_snapshot_json: { ...config, config_version: 2 }, conditions_json: [],
    error_message: "模型临时不可用；已保存行情验证", result_json: {}, progress_json: { forecast_validations: [validation] } };
  const state = { dialogues: [], revisions: [], latest_review: review, watch: { id: 701, enabled: true, config_json: config, config_version: 2 },
    runner: { available: true }, timezone: "Asia/Shanghai" };
  const forecast = { id: 901, review_id: 600, ledger_id: 900, status: "PASSED", entry_price: 7, target_return_pct: 14.2857,
    created_at: stamp, model_instance_code: "TEST_AI", skill_version: "1.1.0",
    contract: { direction: "BULLISH", target_price: 8, invalidation_price: 6, horizon_sessions: 10, confidence: .65,
      currency: "港元", base_trade_date: "2026-11-02", trigger_labels: ["主营订单兑现"], thesis: "订单兑现改善收入预期，需结合成交量持续观察。",
      pricing_basis: "近期阻力区间约8港元，使用不复权原币行情。", logic_chain: ["订单兑现改善收入可见度", "放量突破确认价格路径"],
      invalidation_conditions: ["订单取消或收盘价低于6港元"], evidence: [{ evidence_id: "notice:1", claim: "披露订单进展" }],
      verification_rule: "发布次日起实际交易日收盘价核验", model_call_log_id: 123 },
    latest_validation: { ...validation, sessions_observed: 5, lesson: "价格命中不代表订单与股价因果已经证实。" },
    checks: [{ ...validation, review_id: 601, created_at: stamp, lesson: "保留候选逻辑" }] };
  await page.route(`${api}/ai-stock-analyst/analyses/${run.id}/**`, async route => {
    const url = new URL(route.request().url());
    if (route.request().method() !== "GET") return route.fulfill({ status: 409, json: { detail: "验收禁止写入真实库" } });
    if (url.pathname.endsWith("/followup")) return route.fulfill({ json: state });
    if (url.pathname.endsWith("/timeline")) return route.fulfill({ json: { items: [review], total: 1 } });
    if (url.pathname.endsWith("/tracking")) {
      const offset = Number(url.searchParams.get("offset") || 0);
      return route.fulfill({ json: { items: [ { ...forecast, id: 901 + offset } ], total: 12, stats: { PASSED: 1, FAILED: 2, PENDING: 9 } } });
    }
    if (url.pathname.endsWith("/timeline/601/evidence")) return route.fulfill({ json: { evidence: [ { evidence_id: "notice:1", title: "主营订单进展", excerpt: "保留真实来源和原文" } ] } });
    return route.continue();
  });
  await page.goto(`${base}/#/research/analyst`);
  await expect(page.getByRole("heading", { name: "AI研究伙伴", exact: true })).toBeVisible();
}

test("两区域原生全屏、全屏内弹窗及退出恢复", async ({ page, request }) => {
  await page.setViewportSize({ width: 1440, height: 1000 });
  await prepare(page, request);
  await page.getByRole("button", { name: "全屏分析结论", exact: true }).click();
  await expect.poll(() => page.evaluate(() => document.fullscreenElement?.tagName)).toBe("MAIN");
  await expect(page.getByRole("button", { name: "退出全屏分析结论" })).toBeVisible();
  await page.getByRole("button", { name: "退出全屏分析结论" }).click();
  await expect.poll(() => page.evaluate(() => document.fullscreenElement)).toBeNull();
  await page.getByRole("button", { name: "全屏研究伙伴", exact: true }).click();
  await expect.poll(() => page.evaluate(() => document.fullscreenElement?.tagName)).toBe("SECTION");
  await page.getByRole("button", { name: "关注价位 / 条件" }).click();
  await expect(page.getByRole("dialog")).toBeVisible();
  expect(await page.getByRole("dialog").evaluate(el => document.fullscreenElement?.contains(el))).toBeTruthy();
  await page.getByRole("button", { name: "关闭关注条件" }).click();
  await page.screenshot({ path: "test-results/analyst-chat-fullscreen.png" });
  await page.getByRole("button", { name: "退出全屏研究伙伴", exact: true }).click();
  await expect.poll(() => page.evaluate(() => document.fullscreenElement)).toBeNull();
});

test("全屏降级、观察时间轴、模型失败仍显示目标提醒及复盘分页", async ({ page, request }) => {
  await page.addInitScript(() => { Element.prototype.requestFullscreen = () => Promise.reject(new Error("模拟宿主不支持")); });
  await page.setViewportSize({ width: 1440, height: 1000 });
  await prepare(page, request);
  const tabs = page.getByRole("tablist", { name: "分析内容" });
  await expect(tabs.getByRole("tab")).toHaveText(["分析结论", "股票资料", /依据与数据/, "执行记录", "时间轴", "跟踪复盘"]);
  await page.getByRole("button", { name: "全屏分析结论", exact: true }).click();
  await expect(page.locator(".analyst-main.analyst-pane-fullscreen")).toBeVisible();
  await page.getByRole("button", { name: "讨论这条结论" }).click();
  await expect(page.locator(".analyst-pane-fullscreen")).toHaveCount(0);
  await page.getByRole("tab", { name: "时间轴", exact: true }).click();
  await expect(page.locator(".analyst-observation-overview")).toContainText("来自报告的重点观察");
  await expect(page.locator(".analyst-timeline-item")).toContainText("验证通过");
  await expect(page.locator(".analyst-timeline-item")).toContainText("模型临时不可用");
  await page.getByRole("button", { name: "全屏分析结论", exact: true }).click();
  await page.getByRole("button", { name: "查看本次证据" }).click();
  await expect(page.locator(".analyst-main.analyst-pane-fullscreen").getByRole("dialog")).toBeVisible();
  await page.keyboard.press("Escape");
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await expect(page.locator(".analyst-main.analyst-pane-fullscreen")).toBeVisible();
  await page.keyboard.press("Escape");
  await expect(page.locator(".analyst-pane-fullscreen")).toHaveCount(0);
  await page.getByRole("tab", { name: "跟踪复盘", exact: true }).click();
  await expect(page.locator(".analyst-tracking-stats")).toContainText("12");
  await expect(page.locator(".analyst-forecast")).toContainText("目标 8.00 港元");
  await expect(page.locator(".analyst-forecast")).toContainText("目标价依据");
  await page.locator(".analyst-forecast").getByText("验证历程 · 1次", { exact: true }).click();
  await expect(page.locator(".analyst-forecast-check")).toContainText("发布后的收盘价达到上行目标");
  await page.getByRole("button", { name: "下一页", exact: true }).click();
  await expect(page.locator(".analyst-tracking .analyst-pagination")).toContainText("2 / 2页");
  await page.screenshot({ path: "test-results/analyst-tracking-desktop.png", fullPage: true });
  await page.setViewportSize({ width: 768, height: 1024 });
  expect(await page.locator(".ai-stock-analyst").evaluate(el => el.scrollWidth > el.clientWidth + 1)).toBeFalsy();
  await page.screenshot({ path: "test-results/analyst-tracking-tablet.png", fullPage: true });
});
