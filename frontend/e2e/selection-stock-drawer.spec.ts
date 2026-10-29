import { expect, test } from "@playwright/test";

const baseUrl = process.env.E2E_BASE_URL || "http://127.0.0.1:5173";

test("选股结果和高级工作台可以打开股票详情抽屉", async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.goto(`${baseUrl}/#/research/selection`);
  await expect(page.getByRole("heading", { name: "选股分析工作台" })).toBeVisible();

  const stockLinks = page.locator('button[title="打开股票详细信息"]');
  await expect(stockLinks.first()).toBeVisible();
  await stockLinks.first().click();
  await expect(page.locator(".stock-detail-drawer")).toBeVisible();
  await expect(page.locator(".stock-detail-drawer h2")).toContainText("688627");
  await page.locator(".stock-detail-drawer .detail-head-actions").getByRole("button", { name: "关闭" }).click();
  await expect(page.locator(".stock-detail-drawer")).toHaveCount(0);

  await page.getByRole("button", { name: "高级工作台" }).click();
  await expect(page.getByRole("heading", { name: "候选池与人工复选" })).toBeVisible();
  const advancedLinks = page.locator('button[title="打开股票详细信息"]');
  await expect(advancedLinks.first()).toBeVisible();
  await advancedLinks.first().click();
  await expect(page.locator(".stock-detail-drawer")).toBeVisible();
  await expect(page.locator(".stock-detail-drawer h2")).toContainText("688627");
});
