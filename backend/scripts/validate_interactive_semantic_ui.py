"""Browser contract checks with intercepted writes; never approve production data."""
from copy import deepcopy
import json
from pathlib import Path
from urllib.parse import urlparse, parse_qs

from playwright.sync_api import expect, sync_playwright

OUTPUT = Path(__file__).resolve().parents[1] / "validation" / "interactive-semantic"
PAGE = "http://127.0.0.1:5173/#/operations/documents?table=stock_news&id=999991"


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    actions, errors, blocked = [], [], []
    plan = {"plan_id": "browser-isolated-plan", "plan_version": 1, "revision": 1, "status": "PROPOSED",
            "overview": "公司预计计提负债，涉及利润影响和融资安排。", "primary_type": "FINANCIAL_DISCLOSURE",
            "secondary_types": [], "relevance": "DIRECT", "target_scopes": ["COMPANY"], "rationale": "原文披露预计金额及终止定增事项。",
            "expected_knowledge": ["预计负债及影响", "终止定增"], "uncertainties": ["预计与已发生需要区分"],
            "user_confirmation_questions": ["请核对预计负债性质"], "recommended_skill_codes": ["FINANCIAL_DISCLOSURE_GOVERNOR"],
            "capability_gaps": [{"type_name": "专项融资争议", "suggested_skill_name": "融资争议治理", "reason": "需要补充认购资金来源的证据核验规则"}]}
    catalog = {"types": [{"code": code, "name": label, "skill": skill, "description": label, "skill_status": "ENABLED"}
                         for code, label, skill in [("FINANCIAL_DISCLOSURE", "财务披露与业绩", "FINANCIAL_DISCLOSURE_GOVERNOR"),
                                                   ("CAPITAL_OPERATION", "资本运作", "CAPITAL_OPERATION_GOVERNOR")]],
               "skills": [{"skill_code": code, "skill_name": label, "description": label, "version": "1.0.0", "enabled": True, "lifecycle_status": "ENABLED"}
                          for code, label in [("FINANCIAL_DISCLOSURE_GOVERNOR", "财务披露治理"), ("CAPITAL_OPERATION_GOVERNOR", "资本运作治理"),
                                              ("DOC_SPECIALIST_BROWSER", "融资争议专项治理")]]}
    points = [{"point_type": "EVENT", "key": "provision", "knowledge_type": "预计负债计提", "summary": "预计计提1.07亿元负债，利润可能由盈转亏。",
               "subjects": ["隔离测试公司"], "event_time": "2026年度", "metrics": ["预计负债：1.07 亿元"], "direction": "NEGATIVE",
               "assertion_type": "REPORTED_FACT", "evidence_quotes": ["公司预计计提1.07亿元负债，利润可能由盈转亏。"],
               "source_url": "https://example.com/original", "confirmation_level": "REQUIRED", "confirmation_reason": "请确认预计与实际计提的区别", "review_status": "NOT_REVIEWED"},
              {"point_type": "EVENT", "key": "financing", "knowledge_type": "终止定增", "summary": "公司终止定向增发安排。",
               "subjects": ["隔离测试公司"], "event_time": "2026年", "metrics": [], "direction": "NEUTRAL", "assertion_type": "REPORTED_FACT",
               "evidence_quotes": ["公司终止定向增发安排。"], "source_url": "https://example.com/original", "confirmation_level": "NORMAL", "review_status": "NOT_REVIEWED"}]
    state = {"workflow": "NEEDS_PLAN_CONFIRMATION", "versions": [], "reports": [], "draft": None, "job": None}

    def job():
        return {"job_id": 999991, "status": "PENDING", "job_status": "PENDING", "progress": 0, "current_stage": "QUEUED"}

    def add_report():
        index = len(state["reports"])
        items = deepcopy(points)
        if index:
            items[0]["summary"] = "预计计提1.07亿元负债，尚非已发生的实际计提；利润可能受负面影响。"
            items[1]["review_status"] = "HUMAN_CONFIRMED"
        state["versions"].append({"index": index, "skill_version": "1.0.0", "semantic_schema_version": "3.0", "observed_at": "2026-10-12T12:00:00", "status": "ANALYZED"})
        state["reports"].append({"version_key": f"browser-version-{index}", "plan_id": plan["plan_id"], "plan_revision": plan["revision"],
                                 "version_index": index, "title": "隔离验收：预计负债与终止定增", "source_name": "隔离验收来源",
                                 "source_url": "https://example.com/original", "overview": "负债计提和融资安排需要结合原文核对。",
                                 "knowledge_points": items, "uncertainties": [], "workflow_status": "NEEDS_RESULT_CONFIRMATION",
                                 "publication_status": "CANDIDATE", "note": "确认本批结果才会发布，关闭页面不会自动确认。"})
        state["workflow"] = "NEEDS_RESULT_CONFIRMATION"

    def detail(index=None):
        index = len(state["reports"]) - 1 if index is None else index
        return {"source_table": "stock_news", "record_id": 999991, "market": "CN_A", "symbol": "000159",
                "title": "隔离验收：预计负债与终止定增", "published_at": "2026-10-12", "source_name": "隔离验收来源",
                "status": "ANALYZED" if index >= 0 else "NOT_PROCESSED", "body_characters": 100, "body_available": True,
                "semantic_complete": index >= 0, "event_count": 2 if index >= 0 else 0, "relation_count": 0,
                "version_count": len(state["versions"]), "selected_version": index, "versions": deepcopy(state["versions"]),
                "body": "公司预计计提1.07亿元负债，利润可能由盈转亏。公司终止定向增发安排。",
                "url": "https://example.com/original", "analysis": None, "state": {"content_scope": "FULL_BODY"},
                "human_review": {"overall": "NOT_REVIEWED", "review_count": 0, "history": []}, "review_history": [],
                "governance_plan": deepcopy(plan), "governance_plan_history": [deepcopy(plan)],
                "result_report": deepcopy(state["reports"][index]) if index >= 0 else None, "workflow_status": state["workflow"], "note": "浏览器隔离验收"}

    def handle(route):
        req = route.request
        path, query = urlparse(req.url).path, parse_qs(urlparse(req.url).query)
        payload = req.post_data_json if req.method == "POST" and req.post_data else {}
        if req.method not in {"GET", "OPTIONS"}:
            actions.append({"path": path, "payload": payload})
        value = None
        if "/document-governance/planning-catalog" in path:
            value = catalog
        elif path.endswith("/skill-drafts"):
            value = {"0": state["draft"]} if state["draft"] else {}
        elif path.endswith("/skill-draft"):
            state["draft"] = {"id": 999991, "skill_code": "DOC_SPECIALIST_BROWSER", "skill_name": "融资争议专项治理", "instructions": "逐字核对认购方资金来源，区分事实和观点。", "lifecycle_status": "DRAFT"}
            value = state["draft"]
        elif "/skill-drafts/" in path:
            if path.endswith("/test"):
                state["draft"]["test_result"] = {"passed": True, "event_count": 2, "relation_count": 0, "note": "隔离契约测试通过"}
            elif path.endswith("/enable"):
                state["draft"]["lifecycle_status"] = "ENABLED"
            value = state["draft"]
        elif path.endswith("/plan-decision"):
            assert payload["expected_plan_id"] == plan["plan_id"]
            plan.update(status="SKIPPED" if payload["decision"] == "SKIP" else "CONFIRMED", revision=plan["revision"] + 1,
                        primary_type=payload["primary_type"], secondary_types=payload["secondary_types"], recommended_skill_codes=payload["skill_codes"])
            state["workflow"] = "SKIPPED" if plan["status"] == "SKIPPED" else "READY_TO_EXECUTE"
            value = plan
        elif path.endswith("/analyze"):
            state["job"] = "extract"
            value = job()
        elif path.endswith("/result-review"):
            if payload["decision"] == "SKIP":
                plan["status"] = "SKIPPED"; state["workflow"] = "SKIPPED"
                value = {"status": "SKIPPED"}
            elif payload["nonconforming"]:
                assert payload["nonconforming"][0]["key"] == "provision"
                state["workflow"], state["job"] = "NEEDS_REANALYSIS", "fail_correction"
                value = {"job": job()}
            else:
                assert "EVENT:provision" in payload["explicit_confirmations"]
                state["workflow"] = "PUBLISHED"
                state["reports"][-1].update(publication_status="PUBLISHED", workflow_status="PUBLISHED")
                value = {"status": "PUBLISHED"}
        elif path.endswith("/retry-correction"):
            state["job"] = "correct"
            value = job()
        elif "/stocks/batch-governance/jobs/" in path:
            phase = state["job"]
            if phase in {"extract", "correct"}:
                add_report()
            state["job"] = None
            value = {**job(), "status": "COMPLETED", "job_status": "FAILED" if phase == "fail_correction" else "COMPLETED", "result_status": "PARTIAL" if phase == "fail_correction" else "SUCCESS", "progress": 100, "current_stage": "COMPLETE"}
        elif path.endswith("/documents"):
            value = {"items": [detail()], "total": 1, "stock_name": "国际实业"}
        elif "/document-governance/documents/stock_news/999991" in path:
            value = detail(int(query["version_index"][0]) if "version_index" in query else None)
        if value is not None:
            route.fulfill(json=value)
        elif req.method in {"GET", "OPTIONS"}:
            route.continue_()
        else:
            blocked.append(path); route.abort()

    with sync_playwright() as p:
        browser = p.chromium.launch(channel="msedge", headless=True)
        page = browser.new_page(viewport={"width": 1600, "height": 1100})
        page.on("pageerror", lambda error: errors.append(str(error)))
        page.route("**/api/v1/**", handle)
        page.goto(PAGE, wait_until="domcontentloaded")
        flow = page.get_by_role("region", name="交互式治理流程")
        expect(flow.get_by_text("第二步：确认文档类型与治理Skill")).to_be_visible(timeout=30000)
        flow.get_by_label("主要类型", exact=True).select_option("CAPITAL_OPERATION")
        flow.get_by_label("资本运作治理").check()
        flow.get_by_role("button", name="创建专项Skill草稿").click()
        flow.get_by_role("button", name="测试草稿（不入事实库）").click()
        expect(flow.get_by_text("证据与契约测试通过", exact=False)).to_be_visible()
        flow.get_by_role("button", name="审核通过并启用").click()
        expect(flow.get_by_text("融资争议专项治理 · 已启用")).to_be_visible()
        page.reload(wait_until="domcontentloaded")
        expect(flow.get_by_text("融资争议专项治理 · 已启用")).to_be_visible(timeout=30000)
        expect(flow.get_by_label("融资争议专项治理", exact=False)).to_be_checked()
        flow.get_by_label("主要类型", exact=True).select_option("CAPITAL_OPERATION")
        flow.get_by_label("资本运作治理").check()
        flow.screenshot(path=str(OUTPUT / "plan-confirmation.png"))
        flow.get_by_role("button", name="确认方案并进入执行").click()
        expect(flow.get_by_role("button", name="执行结构化治理")).to_be_visible()
        flow.get_by_role("button", name="执行结构化治理").click()
        expect(flow.get_by_text("第四步：确认结构化知识点")).to_be_visible(timeout=30000)
        expect(flow.get_by_role("button", name="确认本批结果并发布")).to_be_disabled()
        cards = flow.locator(".ds-knowledge-points article")
        expect(cards).to_have_count(2)
        cards.first.get_by_label("该知识点不符合").check()
        cards.first.locator("textarea").fill("请区分预计计提与已经发生的实际计提，不要混淆。")
        flow.get_by_role("button", name="提交1项修正并重分析").click()
        expect(flow.get_by_role("button", name="按原意见继续修正")).to_be_enabled(timeout=30000)
        flow.get_by_role("button", name="按原意见继续修正").click()
        expect(flow.get_by_text("预计计提1.07亿元负债，尚非已发生的实际计提；利润可能受负面影响。", exact=True)).to_be_visible(timeout=30000)
        cards.first.get_by_label("我已核对并确认该重点项").check()
        expect(flow.get_by_role("button", name="确认本批结果并发布")).to_be_enabled()
        flow.screenshot(path=str(OUTPUT / "knowledge-review.png"))
        flow.get_by_role("button", name="确认本批结果并发布").click()
        expect(flow.get_by_text("第五步：最终成果与知识结构化报告")).to_be_visible(timeout=30000)
        expect(flow.get_by_role("button", name="确认本批结果并发布")).to_have_count(0)
        flow.screenshot(path=str(OUTPUT / "published-report.png"))
        page.set_viewport_size({"width": 430, "height": 932})
        flow.screenshot(path=str(OUTPUT / "published-report-mobile.png"))
        assert flow.evaluate("node => node.scrollWidth <= node.clientWidth + 2"), "治理报告在窄屏溢出"
        page.set_viewport_size({"width": 1600, "height": 1100})
        page.get_by_text("展开专业核验、原文与历史版本", exact=True).click()
        page.get_by_label("治理版本", exact=True).select_option("0")
        expect(flow.get_by_text("历史候选成果（只读）")).to_be_visible()
        expect(flow.get_by_label("该知识点不符合")).to_have_count(0)
        expect(flow.get_by_role("button", name="确认本批结果并发布")).to_have_count(0)
        # Start a separate isolated fixture to exercise the user stop path.
        plan.update(status="PROPOSED", revision=plan["revision"] + 1)
        state.update(workflow="NEEDS_PLAN_CONFIRMATION", versions=[], reports=[], job=None)
        page.reload(wait_until="domcontentloaded")
        expect(flow.get_by_text("第二步：确认文档类型与治理Skill")).to_be_visible(timeout=30000)
        flow.get_by_role("button", name="本篇不治理", exact=True).click()
        expect(flow.get_by_text("本篇已选择不治理", exact=True)).to_be_visible()
        expect(flow.get_by_role("button", name="执行结构化治理")).to_have_count(0)
        assert not errors, errors
        assert not blocked, blocked
        browser.close()
    result = {"passed": True, "mode": "browser mocks; all write requests intercepted", "actions": actions,
              "checks": ["方案类型和Skill修改", "专项草稿测试启用与刷新恢复", "真实页面执行入口和任务进度", "重点确认门禁", "不符合项定向反馈", "失败后原意见重试", "发布成果", "历史只读", "窄屏报告无水平溢出", "用户选择不治理后停止"], "page_errors": errors}
    (OUTPUT / "browser-verification.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"passed": True, "checks": len(result["checks"]), "intercepted_writes": len(actions)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
