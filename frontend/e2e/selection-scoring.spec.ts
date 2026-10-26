import { expect, test } from "@playwright/test";

const baseUrl = process.env.E2E_BASE_URL || "http://127.0.0.1:5173";

test("评价体系桌面端和移动端可用", async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.goto(`${baseUrl}/#/ai/scoring`);
  await expect(page.getByRole("heading", { name: "选股评价体系" })).toBeVisible();
  await expect(page.getByText("权重合计")).toBeVisible();
  await expect(page.locator("tbody tr")).toHaveCount(8);
  await page.getByRole("button", { name: /综合研究/ }).click();
  const firstWeight = page.getByLabel(/基本面与盈利质量权重/);
  await expect(firstWeight).toBeEnabled();
  await firstWeight.fill("21");
  await expect(page.getByText("101.0%", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "发布", exact: true })).toBeDisabled();
  await page.getByRole("button", { name: "复制新版本" }).click();
  await expect(page.getByRole("dialog", { name: "复制评价方案" })).toBeVisible();
  await expect(page.getByLabel("新版本号")).toHaveValue("1.1.0");
  await page.getByRole("button", { name: "取消" }).click();
  const desktopOverflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth + 1);
  expect(desktopOverflow).toBeFalsy();

  await page.setViewportSize({ width: 390, height: 844 });
  await page.reload();
  await expect(page.getByRole("heading", { name: "选股评价体系" })).toBeVisible();
  await expect(page.getByRole("button", { name: /短周期综合/ })).toBeVisible();
  const mobileOverflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth + 1);
  expect(mobileOverflow).toBeFalsy();
  await page.screenshot({ path: "test-results/selection-scoring-mobile.png", fullPage: true });
});

test("真实候选展示八维雷达图和证据下钻", async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.goto(`${baseUrl}/#/research/selection`);
  await expect(page.getByRole("heading", { name: "选股分析工作台" })).toBeVisible();
  await page.getByRole("button", { name: "分析与证据" }).first().click();
  await expect(page.getByRole("dialog")).toBeVisible();
  await expect(page.locator(".score-radar svg")).toBeVisible();
  await expect(page.getByText(/可信综合分/)).toBeVisible();
  await page.getByText("查看八维评分依据、覆盖与权重").click();
  await expect(page.locator(".scorecard-grid article")).toHaveCount(8);
  const dialogOverflow = await page.locator(".batch-detail").evaluate(element => element.scrollWidth > element.clientWidth + 1);
  expect(dialogOverflow).toBeFalsy();
  await page.screenshot({ path: "test-results/selection-radar-desktop.png", fullPage: false });
});
