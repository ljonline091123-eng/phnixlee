"""Prepare recovery drafts for the four failed documents, without human approval.

Uses real APIs/model calls. Does not enable Skills, confirm plans or publish facts.
Every document must be attached before starting the shared background evaluation.
"""
import json
from pathlib import Path
import requests

BASE = "http://127.0.0.1:8000/api/v1/document-governance"
OUTPUT = Path(__file__).resolve().parents[1] / "validation" / "semantic-skill-loop"


def call(method, path, payload=None):
    response = requests.request(method, BASE + path, json=payload, timeout=300)
    response.raise_for_status()
    return response.json()


def main():
    prepared, errors, skills = [], [], {}
    for identity in (1111, 1121, 1119, 1117):
        path = f"/documents/stock_news/{identity}"
        print(f"真实模型诊断文档#{identity}", flush=True)
        try:
            response = call("POST", path + "/failure-diagnosis", {"reviewer": "system-remediation-check", "force": True})
            plan = response["plan"]
            drafts = []
            for index in range(len(plan.get("capability_gaps") or [])):
                draft = call("POST", path + "/skill-draft", {"gap_index": index,
                    "expected_plan_id": plan["plan_id"], "reviewer": "system-remediation-check"})
                drafts.append({k: draft[k] for k in ("id", "skill_code", "version", "enabled")})
                skills[draft["id"]] = draft
            prepared.append({"record_id": identity, "plan_id": plan["plan_id"], "drafts": drafts,
                "diagnosis": response["diagnosis"]})
            print(json.dumps({"record_id": identity, "drafts": drafts}, ensure_ascii=False), flush=True)
        except Exception as exc:
            errors.append({"record_id": identity, "error": str(exc)})
            print(json.dumps(errors[-1], ensure_ascii=False), flush=True)
    jobs = []
    if not errors:
        for skill_id in skills:
            job = call("POST", f"/skill-drafts/{skill_id}/test-jobs", {"reviewer": "system-remediation-check"})
            jobs.append({"skill_id": skill_id, "job_id": job["job_id"], "status": job["status"]})
    result = {"prepared": prepared, "errors": errors, "jobs": jobs,
        "note": "仅诊断、创建未启用草稿和后台离线验收；生产方案和知识点仍需用户确认。"}
    OUTPUT.mkdir(parents=True, exist_ok=True)
    (OUTPUT / "real-recovery-preparation.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"prepared_count": len(prepared), "jobs": jobs, "errors": errors}, ensure_ascii=False), flush=True)
    assert not errors, errors


if __name__ == "__main__":
    main()
