import { expect, test } from "@playwright/test";

const baseUrl = process.env.E2E_BASE_URL || "http://127.0.0.1:5173";

test("选股结果和高级工作台可以打开股票详情抽屉", async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.goto(`${baseUrl}/#/research/selection`);
  await expect(page.getByRole("heading", { name: "选股分析工作台" })).toBeVisible();

  const stockLinks = page.locator('button[title="打开股票详细信息"]');
  await expect(stockLinks.first()).toBeVisible();
  const firstSymbol = (await stockLinks.first().locator("small").textContent())?.trim() || "";
  await stockLinks.first().click();
  await expect(page.locator(".stock-detail-drawer")).toBeVisible();
  await expect(page.locator(".stock-detail-drawer h2")).toContainText(firstSymbol);
  await page.locator(".stock-detail-drawer .detail-head-actions").getByRole("button", { name: "关闭" }).click();
  await expect(page.locator(".stock-detail-drawer")).toHaveCount(0);

  await page.getByRole("button", { name: "高级工作台" }).click();
  await expect(page.getByRole("heading", { name: "候选池与人工复选" })).toBeVisible();
  const advancedLinks = page.locator('button[title="打开股票详细信息"]');
  await expect(advancedLinks.first()).toBeVisible();
  const advancedSymbolText = (await advancedLinks.first().locator("small").textContent())?.trim() || "";
  const advancedSymbol = advancedSymbolText.match(/[0-9A-Za-z]{5,}/)?.[0] || advancedSymbolText;
  await advancedLinks.first().click();
  await expect(page.locator(".stock-detail-drawer")).toBeVisible();
  await expect(page.locator(".stock-detail-drawer h2")).toContainText(advancedSymbol);
});

test("结论、数据诊断和一年期深度分析分开呈现", async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.goto(`${baseUrl}/#/research/selection`);
  await expect(page.getByRole("heading", { name: "选股分析工作台" })).toBeVisible();

  await page.getByRole("button", { name: "分析与证据" }).first().click();
  const dialog = page.getByRole("dialog", { name: /分析与证据/ });
  await expect(dialog.getByRole("heading", { name: "整体投资逻辑" })).toBeVisible();
  await expect(dialog.getByText("第一步 · 主营赛道")).toBeVisible();
  await expect(dialog.locator(".analysis-deep-assessments article")).toHaveCount(9);
  await expect(dialog.getByText("数据覆盖与分析输入", { exact: true })).toHaveCount(0);
  await expect(dialog.getByText("事件前后联动尚未完全核验", { exact: true })).toHaveCount(0);
  await expect(dialog.getByText(/verified_relation_fact_ids/)).toHaveCount(0);

  await dialog.getByRole("button", { name: "数据事实" }).click();
  await expect(dialog.getByText("数据覆盖与分析输入", { exact: true })).toBeVisible();
  await expect(dialog.getByText("事件前后联动尚未完全核验", { exact: true })).toBeVisible();
  await expect(dialog.getByText(/部分前后事件的发生顺序|部分事件尚未找到可相互核验/)).toBeVisible();
  await expect(dialog.getByText("source_document_ids", { exact: true })).toHaveCount(0);
  const overflow = await dialog.evaluate(element => element.scrollWidth > element.clientWidth + 1);
  expect(overflow).toBeFalsy();
});
