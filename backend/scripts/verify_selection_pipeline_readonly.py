"""Check historical selection views using real APIs and a read-only database.

Run Vite separately, then invoke from backend. No application lifespan or
background workers are started. Browser requests cannot write business data.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from fastapi.testclient import TestClient
from playwright.sync_api import expect, sync_playwright
from sqlalchemy import create_engine
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.main import app


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="http://127.0.0.1:5173")
    parser.add_argument("--database", type=Path, default=Path("quant.db"))
    parser.add_argument("--run-id", type=int, default=38)
    parser.add_argument("--report-dir", type=Path, default=Path("validation/selection-reliability-20261027"))
    args = parser.parse_args()
    args.report_dir.mkdir(parents=True, exist_ok=True)
    engine = create_engine(f"sqlite:///file:{args.database.resolve().as_posix()}?mode=ro&uri=true")

    def readonly_db():
        with Session(engine) as db:
            yield db
            assert not db.dirty and not db.new and not db.deleted

    app.dependency_overrides[get_db] = readonly_db
    # Deliberately do not use TestClient as a context manager: production
    # lifespan initializes the database and launches workers.
    client = TestClient(app)
    errors = []
    api_failures = []
    try:
        result = client.get(f"/api/v1/selection/runs/{args.run_id}")
        result.raise_for_status()
        payload = result.json()
        by_symbol = {row["symbol"]: row for row in payload["candidates"]}
        empty = by_symbol["300474"]["analysis_json"]
        populated = by_symbol["688627"]["analysis_json"]
        assert empty["model_output_status"] == "NOT_RUN" and "scoring" not in empty and "model" not in empty
        assert populated["model_output_status"] == "FAILED" and populated["data_gate_status"] == "READY"
        assert populated["model_failure_detail"]["category"] == "MODEL_SCHEMA"
        assert all(row["status"] != "RUNNING" for row in payload["criteria_json"]["progress"]["phases"].values())

        with sync_playwright() as playwright:
            browser = playwright.chromium.launch(channel="msedge", headless=True)
            page = browser.new_page(viewport={"width": 1440, "height": 1000})
            page.on("pageerror", lambda error: errors.append(str(error)))

            def proxy(route):
                request = route.request
                headers = {"access-control-allow-origin": "*", "content-type": "application/json"}
                if request.method == "OPTIONS":
                    route.fulfill(status=204, headers={**headers, "access-control-allow-methods": "GET, POST, OPTIONS", "access-control-allow-headers": "*"})
                    return
                if request.method != "GET" and not request.url.endswith("/selection/scope-preview"):
                    api_failures.append({"method": request.method, "url": request.url, "reason": "write blocked"})
                    route.fulfill(status=403, headers=headers, body='{"detail":"只读验收禁止写入"}')
                    return
                path = "/api/v1/" + request.url.split("/api/v1/", 1)[1]
                response = client.request(request.method, path, content=request.post_data,
                                          headers={"content-type": "application/json"})
                if response.status_code >= 400:
                    api_failures.append({"method": request.method, "path": path, "status": response.status_code})
                route.fulfill(status=response.status_code, headers=headers, body=response.content)

            page.route("**/api/v1/**", proxy)
            page.goto(f"{args.url}/#/research/selection", wait_until="domcontentloaded")
            history = page.get_by_label("历史分析批次")
            expect(history).to_be_visible(timeout=60_000)
            history.select_option(str(args.run_id))
            expect(page.get_by_role("region", name="分析失败处理")).to_contain_text("数据不足 2 只", timeout=30_000)
            timeline = page.get_by_role("region", name="后台处理三阶段")
            expect(timeline).to_be_visible()
            expect(timeline).to_contain_text("历史未记录")
            assert page.locator(".selection-phase.running").count() == 0
            assert page.evaluate("document.documentElement.scrollWidth <= innerWidth")
            page.screenshot(path=str(args.report_dir / "01-real-history.png"), full_page=True)
            table = page.locator(".batch-table-wrap tbody")
            table.locator("tr").filter(has_text="300474").get_by_role("button", name="分析与证据").click()
            dialog = page.get_by_role("dialog", name="景嘉微分析与证据")
            expect(dialog).to_contain_text("数据不足")
            assert dialog.locator(".analysis-dimension").count() == 0
            page.screenshot(path=str(args.report_dir / "02-no-evidence-no-conclusion.png"), full_page=True)
            dialog.get_by_role("button", name="关闭", exact=True).click()
            table.locator("tr").filter(has_text="688627").get_by_role("button", name="分析与证据").click()
            dialog = page.get_by_role("dialog", name="精智达分析与证据")
            expect(dialog).to_contain_text("契约")
            assert dialog.locator(".analysis-dimension").count() == 0
            page.screenshot(path=str(args.report_dir / "03-schema-failure-with-evidence.png"), full_page=True)
            dialog.get_by_role("button", name="数据事实", exact=True).click()
            page.screenshot(path=str(args.report_dir / "04-preserved-facts.png"), full_page=True)
            browser.close()

        report = {
            "run_id": args.run_id, "database_mode": "READ_ONLY", "browser_errors": errors,
            "api_failures": api_failures,
            "phases": {key: value["status"] for key, value in payload["criteria_json"]["progress"]["phases"].items()},
            "stocks": [{"symbol": row["symbol"], "name": row["name"],
                        "model_output_status": row["analysis_json"].get("model_output_status"),
                        "data_gate_status": row["analysis_json"].get("data_gate_status"),
                        "documents": len(row["evidence_json"].get("documents") or []),
                        "facts": len(row["evidence_json"].get("facts") or []),
                        "error_category": (row["analysis_json"].get("model_failure_detail") or {}).get("category")}
                       for row in payload["candidates"]],
        }
        assert not errors and not api_failures, report
        (args.report_dir / "readonly-browser-report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
        print(json.dumps(report, ensure_ascii=True))
    finally:
        client.close()
        app.dependency_overrides.pop(get_db, None)
        engine.dispose()


if __name__ == "__main__":
    main()
