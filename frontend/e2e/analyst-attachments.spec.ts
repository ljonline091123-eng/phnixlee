import { expect, test } from "@playwright/test";

const base = process.env.E2E_BASE_URL || "http://127.0.0.1:5173";
const api = process.env.E2E_API_BASE_URL || "http://127.0.0.1:8000/api/v1";

test("粘贴图片、股票资料引用、多选分页、发送保留与历史回看", async ({ page, request }) => {
  await page.setViewportSize({ width: 1600, height: 1050 });
  const history = await (await request.get(`${api}/ai-stock-analyst/analyses?limit=50`)).json();
  const run = history.find((row: { status: string }) => row.status === "COMPLETED");
  expect(run).toBeTruthy();
  const f10 = await (await request.get(`${api}/stocks/${run.market}/${run.symbol}/f10?local_only=true&include_classification_members=false&kline_limit=10`)).json();
  f10.notices = [{ id: 88100, title: "股东回购及管理层任免公告", notice_date: "2026-11-10", source_name: "测试披露", content_json: { content: "该公告用于界面验收，不保存真实判断。" } }];
  f10.notice_total = 1;
  const stamp = new Date().toISOString();
  const documents = Array.from({ length: 14 }, (_, i) => ({ evidence_id: `notice:${88000 + i}`, category: "NOTICE", category_name: "公告",
    title: i === 13 ? "高管任免专项公告" : `回购股份进展公告${i + 1}`, source: "本地公告库", observed_at: "2026-11-10", excerpt: "公司披露公告正文摘要。",
    data: { content_status: i === 0 ? "TITLE_ONLY" : "LOCAL_TEXT" } }));
  const models = [
    { id: 8001, provider_id: 800, instance_code: "TEXT_TEST", model_code: "deepseek-v4-flash", model_name: "文本模型", enabled: true, availability_status: "AVAILABLE", availability_label: "可用", config_json: {} },
    { id: 8002, provider_id: 800, instance_code: "VISION_TEST", model_code: "gemini-3.6-flash", model_name: "图片模型", enabled: true, availability_status: "AVAILABLE", availability_label: "可用", config_json: {} },
  ];
  const state: any = { dialogues: [], revisions: [], latest_review: null, watch: null, runner: { available: true }, timezone: "Asia/Shanghai" };
  let sent: any;
  let sends = 0;
  let imageData = "";
  await page.addInitScript(id => {
    localStorage.setItem("ai-analyst-active", String(id));
    localStorage.setItem("ai-analyst-model", "TEXT_TEST");
  }, run.id);
  await page.route(`${api}/**`, async route => {
    const req = route.request();
    const url = new URL(req.url());
    const path = url.pathname;
    if (path.endsWith("/model-hub/instances")) return route.fulfill({ json: models });
    if (path.endsWith("/model-hub/providers")) return route.fulfill({ json: [{ id: 800, enabled: true, provider_type: "GEMINI_REST" }] });
    if (path.endsWith(`/stocks/${run.market}/${run.symbol}/f10`)) return route.fulfill({ json: f10 });
    if (path.endsWith("/followup")) return route.fulfill({ json: state });
    if (path.endsWith("/timeline") || path.endsWith("/tracking")) return route.fulfill({ json: { items: [], total: 0, stats: {} } });
    if (path.endsWith("/local-documents")) {
      const filtered = documents.filter(row => row.title.includes(url.searchParams.get("keyword") || ""));
      const offset = Number(url.searchParams.get("offset") || 0);
      return route.fulfill({ json: { items: filtered.slice(offset, offset + 12), total: filtered.length, offset, limit: 12 } });
    }
    if (path.endsWith("/dialogues") && req.method() === "POST") {
      sends++;
      sent = req.postDataJSON();
      if (sends === 1) return route.fulfill({ status: 409, json: { detail: "模拟连接失败，请重试；草稿应保留。" } });
      imageData = sent.images[0].data_url;
      const row = { id: 88001, status: "COMPLETED", question: sent.question, created_at: stamp, model_instance_code: sent.model_instance_code,
        references: sent.evidence_ids.map((id: string) => documents.find(doc => doc.evidence_id === id) || { evidence_id: id, category_name: "公告", title: "股东回购及管理层任免公告" }),
        images: [{ evidence_id: "user-image:test", name: sent.images[0].name, url: `${api}/mock-analyst-image`, width: 120, height: 60 }],
        reply_json: { reply: "图片与所选公告需要结合主营表现验证，图片内容作为用户材料核验。", suggested_judgment: "继续观察回购和主营兑现。", focus_points: ["回购进展"], evidence: [] } };
      state.dialogues.push(row);
      return route.fulfill({ status: 202, json: row });
    }
    if (path.endsWith("/mock-analyst-image")) return route.fulfill({ contentType: "image/png", body: Buffer.from(imageData.split(",")[1], "base64") });
    if (path.endsWith("/context-preview")) return route.continue(); // Read-only preview.
    if (req.method() !== "GET") return route.fulfill({ status: 409, json: { detail: "浏览器验收不写真实业务库" } });
    return route.continue();
  });
  await page.goto(`${base}/#/research/analyst`);
  await expect(page.getByRole("heading", { name: "AI研究伙伴" })).toBeVisible();
  await page.getByRole("button", { name: "全屏研究工作区", exact: true }).click();
  const input = page.getByLabel("针对这份报告继续提问");
  await input.fill("请结合截图和公告分析影响。");
  // Exercise the actual paste handler, not only the file picker.
  await input.evaluate(el => {
    const canvas = document.createElement("canvas");
    canvas.width = 120; canvas.height = 60;
    const ctx = canvas.getContext("2d")!;
    ctx.fillStyle = "white"; ctx.fillRect(0, 0, 120, 60);
    ctx.fillStyle = "black"; ctx.fillText("stock research", 5, 30);
    const bytes = Uint8Array.from(atob(canvas.toDataURL("image/png").split(",")[1]), c => c.charCodeAt(0));
    const clipboard = new DataTransfer();
    clipboard.items.add(new File([bytes], "粘贴截图.png", { type: "image/png" }));
    el.dispatchEvent(new ClipboardEvent("paste", { clipboardData: clipboard, bubbles: true, cancelable: true }));
  });
  await expect(page.getByLabel("待发送图片").locator("img")).toHaveCount(1);
  await expect(page.getByRole("alert")).toContainText("当前模型不支持图片理解");
  await expect(page.getByRole("button", { name: "发送问题", exact: true })).toBeDisabled();
  await page.getByRole("button", { name: "预览图片1" }).click();
  await expect(page.getByRole("dialog", { name: "对话图片预览" })).toBeVisible();
  expect(await page.evaluate(() => document.fullscreenElement?.contains(document.querySelector('[aria-label="对话图片预览"]')))).toBeTruthy();
  await page.getByRole("button", { name: "关闭图片预览" }).click();
  await expect.poll(() => page.evaluate(() => !!document.fullscreenElement)).toBeTruthy();
  await page.getByLabel("AI研究伙伴模型").selectOption("VISION_TEST");
  await expect(page.getByRole("alert")).toHaveCount(0);
  await page.getByRole("button", { name: "引用新闻 / 公告", exact: true }).click();
  const picker = page.getByRole("dialog", { name: "引用当前股票的新闻 / 公告" });
  await expect(picker.getByText("仅标题，正文待补全", { exact: false })).toBeVisible();
  await picker.getByRole("checkbox").first().check();
  await picker.getByRole("button", { name: "下一页", exact: true }).click();
  await picker.getByLabel("引用高管任免专项公告").check();
  await expect(picker).toContainText("已选 2/8 条");
  await picker.getByLabel("搜索引用资料").fill("任免");
  await expect(picker.getByRole("checkbox")).toHaveCount(1);
  await expect(picker.getByLabel("引用高管任免专项公告")).toBeChecked();
  await picker.getByRole("button", { name: "完成引用" }).click();
  await expect(page.getByLabel("已引用资料")).toContainText("高管任免专项公告");
  await page.getByRole("button", { name: "移除引用回购股份进展公告1", exact: true }).click();
  await page.getByRole("tab", { name: "股票资料", exact: true }).click();
  await page.locator(".stock-detail-drawer.embedded").getByRole("button", { name: "公告", exact: true }).click();
  await page.getByRole("button", { name: "引用到对话", exact: true }).click();
  await expect(page.getByLabel("已引用资料")).toContainText("股东回购及管理层任免公告");
  await page.getByRole("button", { name: "发送问题", exact: true }).click();
  await expect(page.locator(".analyst-followup > .analyst-notice")).toContainText("模拟连接失败");
  await expect(page.getByLabel("待发送图片").locator("img")).toHaveCount(1);
  await expect(input).toHaveValue("请结合截图和公告分析影响。");
  await input.press("Shift+Enter");
  await input.type("再补充一行");
  await expect(input).toHaveValue("请结合截图和公告分析影响。\n再补充一行");
  await input.evaluate(el => {
    el.dispatchEvent(new CompositionEvent("compositionstart", { bubbles: true }));
    el.dispatchEvent(new KeyboardEvent("keydown", { key: "Enter", isComposing: true, bubbles: true, cancelable: true }));
  });
  expect(sends).toBe(1);
  await input.evaluate(el => el.dispatchEvent(new CompositionEvent("compositionend", { bubbles: true })));
  await input.press("Enter");
  await expect(page.locator(".analyst-ai-bubble")).toContainText("图片与所选公告");
  expect(sent.evidence_ids).toEqual(["notice:88013", "notice:88100"]);
  expect(sent.images).toHaveLength(1);
  expect(sent.images[0].data_url).toMatch(/^data:image\/png;base64,/);
  expect(sent.model_instance_code).toBe("VISION_TEST");
  await expect(page.getByLabel("待发送图片")).toHaveCount(0);
  await page.getByRole("button", { name: "退出全屏研究工作区", exact: true }).click();
  await page.reload();
  await expect(page.locator(".analyst-message-references")).toContainText("高管任免专项公告");
  await expect(page.locator(".analyst-message-images img")).toBeVisible();
  await page.locator(".analyst-message-images button").click();
  await expect(page.getByRole("dialog", { name: "对话图片预览" })).toBeVisible();
  await page.keyboard.press("Escape");
  await expect(page.getByRole("dialog", { name: "对话图片预览" })).toHaveCount(0);
  await page.setViewportSize({ width: 768, height: 1024 });
  await page.getByRole("button", { name: "AI讨论与跟踪", exact: true }).click();
  await page.getByRole("button", { name: "引用新闻 / 公告", exact: true }).click();
  await expect(picker).toBeVisible();
  expect(await picker.evaluate(el => el.scrollWidth > el.clientWidth + 1)).toBeFalsy();
  const done = picker.getByRole("button", { name: "完成引用", exact: true });
  await expect(done).toBeVisible();
  expect(await done.evaluate(el => getComputedStyle(el).backgroundColor)).not.toBe("rgba(0, 0, 0, 0)");
  await page.screenshot({ path: "test-results/analyst-attachments-mobile.png", fullPage: true });
  await done.click();
  await page.setViewportSize({ width: 1600, height: 1050 });
  await page.getByRole("button", { name: "全屏研究工作区", exact: true }).click();
  await page.screenshot({ path: "test-results/analyst-attachments-desktop.png", fullPage: true });
});
