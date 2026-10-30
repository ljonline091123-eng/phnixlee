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

test("数据覆盖缺口与模型校验失败分开呈现", async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.goto(`${baseUrl}/#/research/selection`);
  await expect(page.getByRole("heading", { name: "选股分析工作台" })).toBeVisible();

  const dataPhase = page.locator(".selection-phase").filter({ hasText: "数据检查及同步" });
  await expect(dataPhase).toContainText("部分完成");
  await expect(dataPhase).toContainText("正文补取失败");

  await page.getByRole("button", { name: "分析与证据" }).first().click();
  const dialog = page.getByRole("dialog", { name: /分析与证据/ });
  await expect(dialog.getByText("模型分析失败，不能显示模型结论、方向、目标价或评分。")).toBeVisible();
  await expect(dialog.getByText("最低分析门槛已通过，不等于所有数据源均完整")).toBeVisible();
  await expect(dialog.getByText("本轮失败环节：模型输出与证据引用校验")).toBeVisible();
  await expect(dialog.getByText("模型输入容量限制（1）")).toBeVisible();
  await expect(dialog.getByText("可选增强数据尚未覆盖（4）")).toBeVisible();
  const overflow = await dialog.evaluate(element => element.scrollWidth > element.clientWidth + 1);
  expect(overflow).toBeFalsy();
});
