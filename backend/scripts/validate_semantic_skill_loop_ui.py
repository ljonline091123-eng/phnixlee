"""Browser acceptance for failed-document diagnosis and generated Skill loop.

All writes are intercepted. This validates the interaction without creating
test Skills, model calls or governance jobs in the business database.
"""
from __future__ import annotations

import json
from pathlib import Path

from playwright.sync_api import expect, sync_playwright


OUTPUT = Path(__file__).resolve().parents[1] / "validation" / "semantic-skill-loop"
PAGE = "http://127.0.0.1:5173/#/operations/documents?market=CN_A&symbol=688627&table=stock_news&id=1111"


def plan(status="CONFIRMED", *, revision=2):
    return {
        "plan_id": "browser-recovery-plan", "plan_version": 2, "revision": revision, "status": status,
        "overview": "榜单新闻包含表格市场数据，需要严格核验表头、行列、主体和单位。",
        "primary_type": "MARKET_STATISTICS", "secondary_types": ["SECURITY_MARKET"],
        "relevance": "DIRECT", "target_scopes": ["MARKET", "SECTOR", "SECURITY"],
        "rationale": "原文包含市场、板块和个股三层观测。", "expected_knowledge": ["榜单统计", "个股观测"],
        "uncertainties": [], "user_confirmation_questions": [],
        "recommended_skill_codes": ["MARKET_OBSERVATION_GOVERN"],
        "capability_gaps": [] if status == "CONFIRMED" else [{
            "type_name": "表格证据专项治理", "suggested_skill_name": "表格证据专项治理",
            "reason": "现有Skill没有稳定约束表头、数据行、符号和单位。",
        }],
        "failure_diagnosis": None if status == "CONFIRMED" else {
            "failure_class": "SKILL_GAP", "summary": "现有市场观测Skill未覆盖复杂榜单表格的证据边界。",
            "root_causes": ["单位只出现在表头", "单篇存在多层级主体"],
            "existing_skill_limitations": ["缺少表格行列定位规则"], "requires_new_skill": True,
            "suggested_skill_name": "表格证据专项治理", "skill_goal": "核验表头、数据行和单位。",
            "acceptance_criteria": ["表头与数据行成对引用", "数值单位逐字一致"],
            "recommended_actions": ["创建并测试专项Skill"], "model_call_id": 99001,
            "observed_error": "指标数值或单位与原文不符：最新收盘价",
        },
    }


def detail(diagnosed=False, confirmed=False):
    current = plan("CONFIRMED" if not diagnosed or confirmed else "PROPOSED", revision=2 if not diagnosed or confirmed else 1)
    return {
        "source_table": "stock_news", "record_id": 1111, "market": "CN_A", "symbol": "688627",
        "title": "10.68亿元资金今日流出机械设备股", "published_at": "2026-09-30 17:24:00",
        "source_name": "证券时报网", "status": "EXTRACTION_FAILED",
        "error": "指标数值或单位与原文不符：最新收盘价", "body_characters": 1200, "body_available": True,
        "semantic_complete": False, "skill_version": "1.0.9", "current_skill_version": "1.0.9",
        "schema_version": "SEMANTIC_V2", "event_count": 0, "relation_count": 0, "version_count": 1,
        "workflow_status": "READY_TO_EXECUTE" if confirmed else "NEEDS_PLAN_CONFIRMATION" if diagnosed else "NEEDS_FAILURE_DIAGNOSIS",
        "body": "机械设备行业今日下跌，表格列示代码、名称、涨跌幅和主力资金。",
        "url": "https://example.test/news/1111", "selected_version": 0,
        "state": {"status": "EXTRACTION_FAILED", "error": "指标数值或单位与原文不符：最新收盘价", "attempts": 10,
                  "governance_plan_id": "old-plan", "governance_plan_revision": 2},
        "versions": [{"index": 0, "skill_version": "1.0.9", "semantic_schema_version": "SEMANTIC_V2",
                      "status": "EXTRACTION_FAILED", "error": "指标数值或单位与原文不符：最新收盘价"}],
        "facts": [], "analysis": None, "human_review": {"overall": "NOT_REVIEWED", "history": [], "review_count": 0},
        "governance_plan": current, "governance_plan_history": [], "result_report": None, "review_history": [],
        "note": "候选事实需要人工确认。",
    }


def draft(status="DRAFT", phase="COMPLETED"):
    result = {
        "id": 99101, "skill_code": "DOC_SPECIALIST_TABLE_EVIDENCE", "skill_name": "表格证据专项治理",
        "instructions": "逐字核验表头、数据行、数值符号、单位和主体边界。", "lifecycle_status": status,
        "test_result": {"passed": True, "score": 94, "summary": "第二轮通过全部证据标准。",
                        "event_count": 3, "relation_count": 1, "note": "不写入正式事实", "iterations": 2,
                        "document_count": 4},
        "iteration_history": [
            {"iteration": 1, "skill_version": "1.0.0", "extraction_error": "单位与表头未对应",
             "evaluation": {"passed": False, "score": 55, "summary": "需要补充表格单位规则"}},
            {"iteration": 2, "skill_version": "1.0.1",
             "evaluation": {"passed": True, "score": 94, "summary": "改进版本通过"}},
        ],
    }
    result.update(test_job_id=99102, test_progress={"status": phase,
        "message": "第2轮 · 文档1/4 · 独立LLM复核质量" if phase == "RUNNING" else "测试已进入后台队列" if phase == "PENDING" else "模型服务不可用，验收未完成" if phase == "FAILED" else "全部文档验收通过"})
    if phase in {"PENDING", "RUNNING"}:
        result["test_result"] = None
    elif phase == "FAILED":
        result["test_result"] = {"passed": False, "score": 0, "summary": "模型服务不可用，验收未完成",
            "error": "HTTP 402：API账户余额不足", "failure_kind": "MODEL_UNAVAILABLE", "iterations": 1,
            "event_count": 0, "relation_count": 0, "document_count": 4, "processed_document_count": 1,
            "note": "已保留全部原文和Skill，剩余样本待处理。"}
        result["iteration_history"] = []
    return result


def catalog(enabled=False):
    skills = [{"skill_code": "MARKET_OBSERVATION_GOVERN", "skill_name": "市场观测治理",
               "description": "市场及榜单观测", "version": "1.0.0", "enabled": True, "lifecycle_status": "ENABLED"}]
    if enabled:
        skills.append({"skill_code": "DOC_SPECIALIST_TABLE_EVIDENCE", "skill_name": "表格证据专项治理",
                       "description": "LLM生成并验证", "version": "1.0.1", "enabled": True, "lifecycle_status": "ENABLED"})
    return {"types": [
        {"code": "MARKET_STATISTICS", "name": "榜单与市场统计", "skill": "MARKET_OBSERVATION_GOVERN",
         "description": "市场榜单", "skill_status": "ENABLED"},
        {"code": "SECURITY_MARKET", "name": "证券市场观测", "skill": "MARKET_OBSERVATION_GOVERN",
         "description": "个股市场观测", "skill_status": "ENABLED"},
    ], "skills": skills, "note": "测试目录"}


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    actions, page_errors = [], []
    state = {"diagnosed": False, "enabled": False, "confirmed": False, "queued": False, "finish": False,
             "connection_lost": False, "unavailable": False}
    with sync_playwright() as p:
        browser = p.chromium.launch(channel="msedge", headless=True)
        page = browser.new_page(viewport={"width": 1440, "height": 1000})
        page.on("pageerror", lambda error: page_errors.append(str(error)))

        def route_api(route):
            request = route.request
            path = request.url.split("/api/v1", 1)[-1].split("?", 1)[0]
            method = request.method
            if path == "/document-governance/documents/stock_news/1111" and method == "GET":
                route.fulfill(json=detail(state["diagnosed"], state["confirmed"])); return
            if path == "/document-governance/planning-catalog" and method == "GET":
                route.fulfill(json=catalog(state["enabled"])); return
            if path == "/document-governance/documents" and method == "GET":
                route.fulfill(json={"items": [], "total": 0, "stock_name": "精智达", "current_skill_version": "1.0.9"}); return
            if path.endswith("/failure-diagnosis") and method == "POST":
                actions.append({"path": path, "payload": request.post_data_json}); state["diagnosed"] = True
                route.fulfill(json={"plan": plan("PROPOSED", revision=1), "diagnosis": plan("PROPOSED")["failure_diagnosis"]}); return
            if path.endswith("/skill-draft") and method == "POST":
                actions.append({"path": path, "payload": request.post_data_json})
                route.fulfill(json={**draft(), "test_result": None, "test_progress": None, "test_job_id": None}); return
            if path == "/document-governance/skill-drafts/99101/test-jobs" and method == "POST":
                actions.append({"path": path, "payload": request.post_data_json}); state["queued"] = True
                state["finish"] = False; state["unavailable"] = False
                route.fulfill(status=202, json={"job_id": 99102, "status": "PENDING", "draft": draft(phase="PENDING")}); return
            if path == "/document-governance/skill-test-jobs/99102" and method == "GET":
                if state["connection_lost"]:
                    route.abort("failed"); return
                phase = "FAILED" if state["unavailable"] else "COMPLETED" if state["finish"] else "RUNNING"
                route.fulfill(json={"job_id": 99102, "status": phase, "draft": draft(phase=phase),
                    "error": "HTTP 402：API账户余额不足" if phase == "FAILED" else None}); return
            if path == "/document-governance/skill-drafts/99101/enable" and method == "POST":
                actions.append({"path": path, "payload": request.post_data_json}); state["enabled"] = True
                route.fulfill(json=draft("ENABLED")); return
            if path.endswith("/skill-drafts") and method == "GET":
                route.fulfill(json={"0": draft("ENABLED" if state["enabled"] else "DRAFT",
                    "FAILED" if state["unavailable"] else "RUNNING" if state["queued"] and not state["finish"] else "COMPLETED")}); return
            if path.endswith("/plan-decision") and method == "POST":
                actions.append({"path": path, "payload": request.post_data_json}); state["confirmed"] = True
                route.fulfill(json=plan("CONFIRMED", revision=2)); return
            if "/stocks/search" in path:
                route.fulfill(json=[]); return
            if method == "GET":
                route.fulfill(json={}); return
            actions.append({"unexpected": path, "method": method})
            route.fulfill(status=422, json={"detail": "未登记的隔离写请求"})

        page.route("**/api/v1/**", route_api)
        page.goto(PAGE, wait_until="domcontentloaded")
        dialog = page.get_by_role("dialog", name="新闻公告知识结构化治理窗口")
        expect(dialog).to_be_visible(timeout=30000)
        expect(dialog).to_contain_text("先诊断失败，再决定是否创建或优化Skill")
        dialog.get_by_role("button", name="LLM诊断并自动构建／验证Skill").click()
        expect(dialog).to_contain_text("能力缺口与Skill闭环", timeout=30000)
        expect(dialog).to_contain_text("第2轮 · 文档1/4 · 独立LLM复核质量", timeout=10000)
        expect(dialog.get_by_role("button", name="人工审核通过并启用")).to_be_disabled()
        expect(dialog.get_by_role("button", name="确认方案并进入执行")).to_be_disabled()
        page.reload(wait_until="domcontentloaded")
        expect(dialog).to_contain_text("第2轮 · 文档1/4 · 独立LLM复核质量", timeout=10000)
        expect(dialog.get_by_role("progressbar", name="专项Skill后台治理进度")).to_be_visible()
        state["connection_lost"] = True
        warning = dialog.get_by_text("暂时无法读取进度，正在自动重连。后台任务仍会继续，请勿重复提交。")
        expect(warning).to_be_visible(timeout=10000)
        expect(dialog.get_by_role("button", name="人工审核通过并启用")).to_be_disabled()
        state["connection_lost"] = False
        expect(warning).to_have_count(0, timeout=10000)
        expect(dialog).to_contain_text("第2轮 · 文档1/4 · 独立LLM复核质量")
        state["unavailable"] = True
        expect(dialog).to_contain_text("模型服务不可用，验收未完成", timeout=10000)
        expect(dialog).to_contain_text("验收范围：4篇文档，本轮尝试1篇")
        expect(dialog.locator(".ds-skill-score span")).to_have_count(0)
        expect(dialog.get_by_role("button", name="人工审核通过并启用")).to_be_disabled()
        expect(dialog.get_by_role("button", name="确认方案并进入执行")).to_be_disabled()
        expect(dialog.get_by_role("button", name="继续自动优化并复核")).to_be_enabled()
        page.screenshot(path=str(OUTPUT / "model-unavailable.png"), full_page=True)
        dialog.get_by_role("button", name="继续自动优化并复核").click()
        expect(dialog).to_contain_text("第2轮 · 文档1/4 · 独立LLM复核质量", timeout=10000)
        state["finish"] = True
        expect(dialog).to_contain_text("94分 · 2轮")
        expect(dialog.get_by_role("button", name="人工审核通过并启用")).to_be_enabled()
        page.screenshot(path=str(OUTPUT / "diagnosed-and-tested.png"), full_page=True)
        page.set_viewport_size({"width": 390, "height": 844})
        dialog.get_by_role("button", name="确认方案并进入执行").scroll_into_view_if_needed()
        layout = dialog.evaluate("""node => {
            const close = node.querySelector('.ds-heading button').getBoundingClientRect();
            return {width: node.clientWidth, content: node.scrollWidth, closeTop: close.top, closeBottom: close.bottom};
        }""")
        assert layout["content"] <= layout["width"] + 1, layout
        assert 0 <= layout["closeTop"] < layout["closeBottom"] <= 844, layout
        page.screenshot(path=str(OUTPUT / "narrow-governance-window.png"), full_page=True)
        page.set_viewport_size({"width": 1440, "height": 1000})
        dialog.get_by_role("button", name="人工审核通过并启用").click()
        expect(dialog).to_contain_text("已启用", timeout=10000)
        dialog.get_by_role("button", name="确认方案并进入执行").click()
        expect(dialog).to_contain_text("按确认方案执行治理", timeout=10000)
        confirm = next(item for item in actions if item.get("path", "").endswith("/plan-decision"))
        assert "DOC_SPECIALIST_TABLE_EVIDENCE" in confirm["payload"]["skill_codes"]
        assert not page_errors, page_errors
        result = {"passed": True, "checks": [
            "失败文档不再显示盲目重试按钮", "一键触发LLM失败诊断", "自动创建专项Skill",
            "执行后独立LLM复核并显示2轮迭代", "后台阶段进度", "刷新恢复运行中的任务", "人工启用门禁", "确认方案携带新Skill",
            "断线自动重连后继续查看进度", "模型不可用显示验收未完成且不打零分", "模型恢复后可继续测试", "窄屏无水平溢出且关闭按钮始终可见",
        ], "isolated_actions": actions, "page_errors": page_errors}
        (OUTPUT / "browser-verification.json").write_text(
            json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
        browser.close()
        print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
