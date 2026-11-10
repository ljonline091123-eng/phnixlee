import { expect, test } from "@playwright/test";

const base = process.env.E2E_BASE_URL || "http://127.0.0.1:5173";
const api = process.env.E2E_API_BASE_URL || "http://127.0.0.1:8000/api/v1";

// Mutations are intercepted: browser acceptance must never confirm an invented opinion in the real database.
test("对话、编辑确认、关注条件、每日时间轴与历史恢复", async ({ page, request }) => {
  await page.setViewportSize({ width: 1440, height: 1000 });
  const history = await (await request.get(`${api}/ai-stock-analyst/analyses?limit=50`)).json();
  const run = history.find((row: { status: string }) => row.status === "COMPLETED");
  expect(run).toBeTruthy();
  await page.addInitScript(id => localStorage.setItem("ai-analyst-active", String(id)), run.id);
  const stamp = new Date().toISOString();
  const state: any = { dialogues: [], revisions: [], latest_review: null, watch: null, runner: { available: true, message: "后台执行器已启动" }, timezone: "Asia/Shanghai" };
  const reviews: any[] = [];
  await page.route(`${api}/ai-stock-analyst/analyses/${run.id}/**`, async route => {
    const req = route.request();
    const path = new URL(req.url()).pathname;
    let body: any = null;
    let status = 200;
    if (path.endsWith("/followup")) body = state;
    else if (path.endsWith("/watch/suggestions")) { body = { id: 950, status: "COMPLETED", created_at: stamp, result_json: {} }; state.watch_suggestion = body; status = 202; }
    else if (path.endsWith("/dialogues") && req.method() === "POST") {
      const payload = req.postDataJSON();
      const row = { id: 901, question: payload.question, status: "COMPLETED", model_instance_code: payload.model_instance_code, created_at: stamp,
        reply_json: { reply: "回购可以传递信心，但仍需观察主营收入和现金流，不能单独据此判断后续涨幅。", suggested_judgment: "保持观察，等待主营改善与回购形成相互验证。", focus_points: ["主营收入增长", "回购执行进度"], evidence: [{ evidence_id: "news:1", claim: "观察回购兑现与现金流" }] } };
      state.dialogues.push(row); body = row; status = 202;
    } else if (path.endsWith("/report-confirmations")) {
      const payload = req.postDataJSON();
      expect(payload.expected_revision_id).toBeNull();
      body = { id: 801, version: 1, dialogue_id: payload.dialogue_id, judgment: payload.judgment, focus_points_json: payload.focus_points, created_at: stamp };
      state.revisions.unshift(body); status = 201;
    } else if (path.endsWith("/watch") && req.method() === "PUT") {
      const config = req.postDataJSON();
      body = { id: 701, enabled: config.enabled, config_json: config, config_version: (state.watch?.config_version || 0) + 1, model_instance_code: config.model_instance_code };
      state.watch = body;
    } else if (path.endsWith("/watch/check")) {
      body = { id: 601, business_date: "2026-11-09", attempt: 1, status: "PARTIAL", created_at: stamp, completed_at: stamp,
        model_instance_code: "GEMINI_FLASH", config_snapshot_json: { ...state.watch.config_json, config_version: state.watch.config_version },
        conditions_json: [{ kind: "price_lower", label: "跌至或低于关注下限", status: "HIT", value: 15, threshold: 20, currency: "港元", trade_date: "2026-11-09" }],
        result_json: { judgment: "价格触及观察区间，暂不足以证明主营判断，继续观察订单兑现。", verdict: "UNCHANGED", confidence: .5, changes: ["触及价位"], next_focus: ["订单兑现"] },
        progress_json: { collection: [{ category: "NEWS", status: "FAILED", error: "来源超时" }] }, error_message: "新闻来源暂不可用，保留实际证据" };
      reviews.push(body); state.latest_review = body; status = 202;
    } else if (path.endsWith("/timeline")) body = { items: reviews, total: reviews.length, offset: 0, limit: 10 };
    else if (/\/timeline\/601\/evidence$/.test(path)) body = { evidence: [{ evidence_id: "news:1", title: "回购股份进展", excerpt: "回购尚需结合经营表现验证", observed_at: stamp, source: "stock_news" }] };
    else return route.continue();
    await route.fulfill({ status, json: body });
  });
  await page.goto(`${base}/#/research/analyst`);
  await expect(page.getByRole("heading", { name: "AI研究伙伴" })).toBeVisible();
  await page.getByRole("button", { name: "全屏研究工作区", exact: true }).click();
  await expect.poll(() => page.evaluate(() => document.fullscreenElement?.classList.contains("analyst-unified-workspace"))).toBeTruthy();
  await expect(page.getByRole("heading", { name: "AI研究伙伴" })).toBeVisible();
  await page.getByRole("button", { name: "向右收起AI研究伙伴", exact: true }).click();
  await expect(page.locator(".analyst-unified-workspace")).toHaveClass(/partner-collapsed/);
  await page.getByRole("button", { name: "展开AI研究伙伴", exact: true }).click();
  await page.getByRole("button", { name: "退出全屏研究工作区", exact: true }).click();
  const topModel = page.getByLabel("证券分析模型");
  const partnerModel = page.getByLabel("AI研究伙伴模型");
  await expect.poll(() => partnerModel.locator("option:not([disabled])").count()).toBeGreaterThan(1);
  await expect(partnerModel).toHaveValue(await topModel.inputValue());
  const modelValues = await partnerModel.locator("option:not([disabled])").evaluateAll(options => options.map(option => (option as HTMLOptionElement).value));
  expect(modelValues.length).toBeGreaterThan(1);
  const initialModel = await topModel.inputValue();
  const alternateModel = modelValues.find(value => value !== initialModel) || modelValues[0];
  await partnerModel.selectOption(alternateModel);
  await expect(topModel).toHaveValue(alternateModel);
  await topModel.selectOption(initialModel, { force: true });
  await expect(partnerModel).toHaveValue(initialModel);
  await page.getByLabel("针对这份报告继续提问").fill("回购足以改变当前判断吗？");
  await page.getByRole("button", { name: "发送问题", exact: true }).click();
  await expect(page.locator(".analyst-ai-bubble")).toContainText("仍需观察主营收入和现金流");
  await page.getByRole("button", { name: "编辑并确认更新报告" }).click();
  await page.getByLabel("最终判断", { exact: true }).fill("用户编辑后确认：重点观察主营业务改善，回购作为辅助信号。");
  await page.getByLabel("确认关注点").fill("主营收入增长\n回购执行进度\n现金流改善");
  await page.getByRole("button", { name: "确认并更新报告", exact: true }).click();
  await expect(page.locator(".analyst-verdict")).toContainText("用户编辑后确认");
  await expect(page.locator(".analyst-verdict details")).toContainText(run.result_json.executive_summary);
  await page.getByRole("button", { name: "关注价位 / 条件" }).click();
  await expect(page.getByRole("group", { name: "选择需要每日跟踪的重点观察（可多选）" })).toContainText("主营收入增长");
  await page.getByLabel("重点观察2").uncheck();
  await page.getByLabel("关注下限", { exact: true }).fill("20");
  await page.getByLabel("关注上限", { exact: true }).fill("35");
  await page.getByLabel("量能条件").selectOption("EXPAND");
  await page.getByLabel("相对均量倍数").fill("2");
  await page.getByRole("button", { name: "＋为该观察增加触发维度" }).first().click();
  await page.getByLabel("观察1维度1").fill("订单兑现");
  await page.getByLabel("观察1来源1").selectOption("NOTICE");
  await page.getByLabel("观察1关键词1").fill("回购,订单");
  await page.getByRole("button", { name: "保存关注条件", exact: true }).click();
  await page.getByRole("tab", { name: "时间轴", exact: true }).click();
  await expect(page.locator(".analyst-watch-overview")).toContainText("每日监控已开启");
  expect(state.watch.config_json.price_lower).toBe(20);
  expect(state.watch.config_json.event_conditions).toEqual([]);
  expect(state.watch.config_json.observation_rules[0].event_conditions[0]).toMatchObject({ label: "订单兑现", category: "NOTICE", keywords: ["回购", "订单"] });
  expect(state.watch.config_json.selected_observations).toEqual(["主营收入增长", "现金流改善"]);
  await page.getByRole("button", { name: "立即检查", exact: true }).click();
  await expect(page.locator(".analyst-timeline-item")).toContainText("部分完成");
  await expect(page.locator(".analyst-condition-pills")).toContainText("已触发");
  await expect(page.locator(".analyst-daily-current")).toContainText("价格触及观察区间");
  await page.getByRole("button", { name: "查看本次证据", exact: true }).click();
  await expect(page.getByRole("dialog", { name: "资料依据" })).toContainText("回购股份进展");
  await page.keyboard.press("Escape");
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await page.screenshot({ path: "test-results/analyst-followup-timeline.png", fullPage: true });
  await page.getByRole("button", { name: "暂停监控", exact: true }).click();
  await expect(page.locator(".analyst-watch-overview")).toContainText("每日监控已暂停");
  await page.reload();
  await expect(page.locator(".analyst-verdict")).toContainText("用户编辑后确认");
  await page.setViewportSize({ width: 768, height: 1024 });
  await page.getByRole("button", { name: "AI讨论与跟踪", exact: true }).click();
  await page.getByRole("button", { name: "关注价位 / 条件" }).click();
  expect(await page.getByRole("dialog").evaluate(el => el.scrollWidth > el.clientWidth + 1)).toBeFalsy();
  await page.screenshot({ path: "test-results/analyst-followup-conditions.png", fullPage: true });
});

test("切换历史报告时晚到的旧对话不覆盖当前确认判断", async ({ page, request }) => {
  const history = await (await request.get(`${api}/ai-stock-analyst/analyses?limit=50`)).json();
  const completed = history.filter((row: { status: string }) => row.status === "COMPLETED");
  expect(completed.length).toBeGreaterThan(1);
  const [first, second] = completed;
  await page.addInitScript(id => localStorage.setItem("ai-analyst-active", String(id)), first.id);
  let release!: () => void;
  const gate = new Promise<void>(resolve => { release = resolve; });
  let waiting = false;
  await page.route(`${api}/ai-stock-analyst/analyses/*/followup`, async route => {
    const old = new URL(route.request().url()).pathname.includes(`/analyses/${first.id}/`);
    if (old) { waiting = true; await gate; }
    await route.fulfill({ json: { dialogues: [], watch: null, latest_review: null,
      revisions: [{ id: old ? 1 : 2, version: 1, judgment: old ? "旧报告延迟返回的判断" : "当前报告独立确认的判断", focus_points_json: ["主营业务"], created_at: new Date().toISOString() }],
      runner: { available: true }, timezone: "Asia/Shanghai" } });
  });
  await page.goto(`${base}/#/research/analyst`);
  await expect.poll(() => waiting).toBeTruthy();
  await page.getByRole("button", { name: "分析历史", exact: true }).click();
  await page.locator(".analyst-history-row").nth(history.findIndex((row: { id: number }) => row.id === second.id)).click();
  await expect(page.locator(".analyst-verdict")).toContainText("当前报告独立确认的判断");
  release();
  await page.waitForLoadState("networkidle");
  await expect(page.locator(".analyst-verdict")).toContainText("当前报告独立确认的判断");
  await expect(page.locator(".analyst-verdict")).not.toContainText("旧报告延迟返回");
});

test("AI预填关注条件、保护手动编辑、失败保留和报告资料独立交流", async ({ page, request }) => {
  await page.setViewportSize({ width: 1600, height: 1000 });
  const history = await (await request.get(`${api}/ai-stock-analyst/analyses?limit=50`)).json();
  const run = history.find((r: { status: string }) => r.status === "COMPLETED");
  await page.addInitScript(id => localStorage.setItem("ai-analyst-active", String(id)), run.id);
  const stamp = new Date().toISOString();
  const conditions = { price_lower: 6, price_upper: 8, volume_mode: "EXPAND", volume_ratio: 1.8, volume_window: 20, event_conditions: [],
    observation_rules: [{ observation: run.result_json.watch_items[0], event_conditions: [{ category: "NOTICE", label: "订单与回购", keywords: ["订单", "回购"] }] }],
    review_focus: "持续验证主营增长与回购执行的共同影响。" };
  const state: any = { dialogues: [], revisions: [], latest_review: null, watch: null, watch_suggestion: null,
    runner: { available: true, message: "后台已启动" } };
  let requests = 0;
  let saves = 0;
  let sent: any;
  await page.route(`${api}/ai-stock-analyst/analyses/${run.id}/**`, async route => {
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/followup")) return route.fulfill({ json: state });
    if (path.endsWith("/timeline")) return route.fulfill({ json: { items: [], total: 0 } });
    if (path.endsWith("/watch/suggestions")) {
      requests++;
      if (requests === 3) {
        state.watch_suggestion = { id: 903, status: "FAILED", created_at: stamp, error_message: "模型服务暂不可用", result_json: {} };
        return route.fulfill({ status: 202, json: state.watch_suggestion });
      }
      state.watch_suggestion = { id: 900 + requests, status: "COMPLETED", created_at: stamp, model_instance_code: "TEST_AI",
        result_json: { summary: "基于近期真实行情及订单和回购事件推导关注区间。", conditions,
          price_reason: "基于近期原币收盘价与支撑阻力区间设置观察阈值。", volume_reason: "对比此前20个交易日均量观察突破是否伴随放量。", event_reason: "订单及回购直接关系主营和股东回报。" } };
      return route.fulfill({ status: 202, json: state.watch_suggestion });
    }
    if (path.endsWith("/watch") && route.request().method() === "PUT") { saves++; return route.fulfill({ status: 500, json: { detail: "测试禁止保存" } }); }
    if (path.endsWith("/dialogues") && route.request().method() === "POST") {
      sent = route.request().postDataJSON();
      const row = { id: 1234, status: "COMPLETED", question: sent.question, created_at: stamp,
        reply_json: { reply: "结合所选资料和最新本地数据，继续观察订单兑现。", suggested_judgment: "保持观察并持续跟踪主营订单进展。", focus_points: ["订单兑现"], evidence: [] } };
      state.dialogues.push(row);
      return route.fulfill({ status: 202, json: row });
    }
    return route.continue();
  });
  await page.goto(`${base}/#/research/analyst`);
  await expect(page.getByRole("heading", { name: "AI研究伙伴" })).toBeVisible();
  const input = page.getByLabel("针对这份报告继续提问");
  await input.fill("我正在整理自己的判断，请保留这段草稿。");
  await page.locator('.analyst-reader-scroll[aria-label="分析报告阅读区"]').evaluate(el => { el.scrollTop = 180; });
  await page.getByRole("tab", { name: "依据与数据" }).click();
  await expect(input).toHaveValue("我正在整理自己的判断，请保留这段草稿。");
  await page.getByRole("tab", { name: "股票资料", exact: true }).click();
  await expect(page.locator(".stock-detail-drawer.embedded")).toBeVisible();
  await expect(input).toHaveValue("我正在整理自己的判断，请保留这段草稿。");
  await page.getByRole("tab", { name: "分析结论", exact: true }).click();
  expect(await page.locator('[aria-label="分析报告阅读区"]').evaluate(el => el.scrollTop)).toBeGreaterThan(100);
  await page.getByRole("button", { name: "讨论这条结论" }).click();
  await expect(page.locator(".analyst-discussion-context")).toContainText("正在讨论");
  await page.getByRole("button", { name: "发送问题", exact: true }).click();
  await expect(page.locator(".analyst-ai-bubble")).toContainText("结合所选资料");
  expect(sent.question).toContain("当前结论");
  await page.getByRole("button", { name: "关注价位 / 条件", exact: true }).click();
  await expect(page.getByLabel("关注下限", { exact: true })).toHaveValue("6");
  await expect(page.getByLabel("关注上限", { exact: true })).toHaveValue("8");
  await expect(page.getByLabel("观察1关键词1")).toHaveValue("订单，回购");
  expect(saves).toBe(0);
  await page.getByLabel("关注下限", { exact: true }).fill("5.5");
  await page.getByRole("button", { name: "重新生成AI建议" }).click();
  await expect.poll(() => requests).toBe(2);
  await expect(page.getByLabel("关注下限", { exact: true })).toHaveValue("5.5");
  await page.getByRole("button", { name: "应用AI建议" }).click();
  await expect(page.getByLabel("关注下限", { exact: true })).toHaveValue("6");
  await page.getByLabel("关注下限", { exact: true }).fill("5.8");
  await page.getByRole("button", { name: "重新生成AI建议" }).click();
  await expect(page.getByRole("dialog")).toContainText("本次AI建议未完成");
  await expect(page.getByLabel("关注下限", { exact: true })).toHaveValue("5.8");
  await page.getByRole("button", { name: "关闭关注条件" }).click();
  await page.getByRole("button", { name: "刷新对话与跟踪" }).click();
  // A failed regeneration must not silently enable a watch or alter the report.
  expect(saves).toBe(0);
  await page.screenshot({ path: "test-results/analyst-workspace-desktop.png", fullPage: true });
  expect(await page.locator(".ai-stock-analyst").evaluate(el => el.scrollWidth > el.clientWidth + 1)).toBeFalsy();
});
