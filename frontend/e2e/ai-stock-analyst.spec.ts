import { expect, test } from "@playwright/test";

const base = process.env.E2E_BASE_URL || "http://127.0.0.1:5173";
const api = process.env.E2E_API_BASE_URL || "http://127.0.0.1:8000/api/v1";

test("独立分析入口、拼音搜索、数据预览与旧工作台分离", async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.goto(`${base}/#/research/analyst`);
  await expect(page.getByRole("heading", { name: "AI证券分析师", exact: true })).toBeVisible();
  await expect(page.getByRole("heading", { name: "选股分析工作台", exact: true })).toHaveCount(0);
  await expect(page.getByRole("combobox", { name: "证券分析模型" }).locator("option")).not.toHaveCount(1);
  await page.getByRole("textbox", { name: "搜索分析股票" }).fill("hz mzn".replace(/ /g, ""));
  const match = page.locator(".analyst-matches button").filter({ hasText: "02533" });
  await expect(match).toBeVisible({ timeout: 15000 });
  await match.click();
  await expect(page.locator(".analyst-preview")).toContainText("条资料摘要");
  await expect(page.getByRole("button", { name: "开始AI分析", exact: true })).toBeEnabled();
  await expect(page.locator(".ai-stock-analyst")).not.toContainText("雷达图");
  await page.screenshot({ path: "test-results/ai-stock-analyst-entry.png", fullPage: true });
  expect(await page.locator(".ai-stock-analyst").evaluate(el => el.scrollWidth > el.clientWidth + 1)).toBeFalsy();
});

test("真实历史可查询，证据快照与执行阶段可查看", async ({ page, request }) => {
  await page.setViewportSize({ width: 1440, height: 1000 });
  const response = await request.get(`${api}/ai-stock-analyst/analyses?limit=50`);
  expect(response.ok()).toBeTruthy();
  const runs = await response.json() as Array<{ id: number; status: string }>;
  expect(runs.length).toBeGreaterThan(0);
  const run = runs.find(row => row.status === "COMPLETED") || runs[0];
  await page.addInitScript(id => localStorage.setItem("ai-analyst-active", String(id)), run.id);
  await page.goto(`${base}/#/research/analyst`);
  await expect(page.locator(".analyst-run-header")).toContainText("02533");
  await page.getByRole("tab", { name: "依据与数据" }).click();
  await expect(page.getByRole("heading", { name: "本次实际分析输入" })).toBeVisible();
  await expect(page.locator(".analyst-evidence-row").first()).toBeVisible();
  await page.locator(".analyst-evidence-row > summary").first().click();
  await expect(page.locator(".analyst-evidence-row").first()).toContainText("证据编号");
  await page.getByRole("tab", { name: "执行记录" }).click();
  await expect(page.locator(".analyst-steps")).toContainText("读取本地资料");
  await expect(page.locator(".analyst-steps")).toContainText("AI综合研判");
  await expect(page.locator(".analyst-steps")).toContainText("核验证据与保存");
  if (run.status === "COMPLETED") {
    await page.getByRole("tab", { name: "分析结论" }).click();
    await expect(page.locator(".analyst-scenario")).toHaveCount(3);
    await expect(page.getByRole("heading", { name: "公司现状与投资逻辑" })).toBeVisible();
    await page.screenshot({ path: "test-results/ai-stock-analyst-result.png", fullPage: true });
  }
  await page.reload();
  await expect(page.locator(".analyst-run-header")).toContainText("02533");
  await expect(page.getByRole("textbox", { name: "搜索分析股票" })).toHaveValue(/黑芝麻/);
  await expect(page.getByRole("button", { name: "开始AI分析", exact: true })).toBeEnabled();
  await page.locator(".analyst-stock-link").click();
  await expect(page.locator(".stock-detail-drawer")).toBeVisible();
});

test("窄屏入口不会横向溢出", async ({ page }) => {
  await page.setViewportSize({ width: 768, height: 1024 });
  await page.goto(`${base}/#/research/analyst`);
  await expect(page.getByRole("heading", { name: "AI证券分析师", exact: true })).toBeVisible();
  expect(await page.locator(".ai-stock-analyst").evaluate(el => el.scrollWidth > el.clientWidth + 1)).toBeFalsy();
});
