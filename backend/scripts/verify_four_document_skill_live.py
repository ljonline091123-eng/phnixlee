"""Real-model four-document acceptance; production DB is strictly read-only."""
from copy import deepcopy
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import shutil
import tempfile

from sqlalchemy import create_engine, inspect, select
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

import app.models  # noqa: F401
from app.db.base import Base
from app.db.session import SessionLocal
from app.models.ai_hub import AgentDefinition, ModelProvider, ModelInstance, ModelRouteRule, ModelSkill, ModelCallLog
from app.models.foundation import FoundationEntity, FoundationSecurity, FoundationListing, FoundationEvidence
from app.models.market_data import DataSource, StockSymbol, StockNews
from app.services import skill_files, document_semantic_skills as specialist, stock_batch
from app.services.resource_hub import seed_default_data_assets, seed_default_knowledge_bases, seed_default_agents
from app.services.document_semantic_planning import decide_plan, result_report
from app.services.document_semantic_review import versions_for


OUTPUT = Path(__file__).resolve().parents[1] / "validation" / "semantic-skill-loop"
DOCUMENT_IDS = (1111, 1121, 1119, 1117)


def clone(db, row):
    db.merge(type(row)(**{c.key: deepcopy(getattr(row, c.key)) for c in inspect(type(row)).columns}))
    db.flush()


def persist(result, model):
    OUTPUT.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
    content = json.dumps(result, ensure_ascii=False, indent=2, default=str)
    (OUTPUT / f"four-document-{model}-{stamp}.json").write_text(content, encoding="utf-8")
    (OUTPUT / f"four-document-{model}-latest.json").write_text(content, encoding="utf-8")


def main(model, iterations, model_code=None):
    engine = create_engine("sqlite://", poolclass=StaticPool, connect_args={"check_same_thread": False})
    Base.metadata.create_all(engine)
    isolated = sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)
    result = {"passed": False, "model_instance": model, "requested_model_code": model_code, "document_ids": list(DOCUMENT_IDS),
        "isolation": "内存数据库副本；生产数据库只读；人工启用和方案确认仅为隔离模拟，不发布生产事实"}
    with tempfile.TemporaryDirectory(prefix="four-document-skill-") as temporary:
        original_root = skill_files.SKILL_ROOT
        skill_files.SKILL_ROOT = Path(temporary) / "skill_docs"
        shutil.copytree(original_root, skill_files.SKILL_ROOT)
        try:
            with SessionLocal() as source, isolated() as db:
                for cls in (ModelProvider, ModelInstance, ModelRouteRule, ModelSkill, AgentDefinition):
                    for row in source.scalars(select(cls)):
                        clone(db, row)
                news = [source.get(StockNews, identity) for identity in DOCUMENT_IDS]
                assert all(news), "真实样本缺失"
                stock = source.scalar(select(StockSymbol).where(StockSymbol.market == news[0].market,
                    StockSymbol.symbol == news[0].symbol))
                for identity in {stock.source_id, *(n.source_id for n in news)}:
                    if identity:
                        clone(db, source.get(DataSource, identity))
                clone(db, stock)
                listing = source.scalar(select(FoundationListing).where(FoundationListing.stock_symbol_id == stock.id))
                assert listing, "证券公司映射缺失"
                security = source.get(FoundationSecurity, listing.security_id)
                for row in (source.get(FoundationEntity, security.entity_id), security,
                            source.get(FoundationEvidence, listing.evidence_id), listing):
                    clone(db, row)
                for row in news:
                    clone(db, row)
                db.commit()
                seed_default_data_assets(db); seed_default_knowledge_bases(db); seed_default_agents(db)
                agent = db.scalar(select(AgentDefinition).where(
                    AgentDefinition.agent_code == "DOCUMENT_SEMANTIC_GOVERNANCE_AGENT"))
                agent.model_instance_code = model
                instance = db.scalar(select(ModelInstance).where(ModelInstance.instance_code == model,
                    ModelInstance.enabled.is_(True)))
                assert instance, "隔离验证模型不存在或停用"
                if model_code:
                    instance.model_code = model_code  # Only the disposable copy changes.
                draft_codes = set()
                for identity in DOCUMENT_IDS:
                    row = db.get(StockNews, identity)
                    plan = row.content_json["semantic_governance_plan"]
                    draft_codes.update(plan.get("draft_skill_codes", {}).values())
                assert len(draft_codes) == 1, "四篇须绑定同一专项Skill验收"
                skill = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == next(iter(draft_codes))))
                skill.enabled = False; skill.lifecycle_status = "TESTING"
                skill.config_json = {**skill.config_json, "semantic_test_progress": {},
                    "semantic_draft_test": {}, "semantic_test_job_id": None,
                    "validation_sources": [{"source_table": "stock_news", "record_id": i} for i in DOCUMENT_IDS]}
                db.commit()
                print(f"真实模型{model}：四篇冻结原文专项Skill验收开始（隔离副本）", flush=True)
                acceptance = specialist.test_draft(db, skill, db.get(StockNews, DOCUMENT_IDS[0]),
                    reviewer="隔离副本模拟审核员", max_iterations=iterations)
                result["draft_acceptance"] = acceptance
                if (acceptance.get("test_result") or {}).get("passed"):
                    specialist.enable_draft(db, skill, reviewer="隔离副本模拟审核员")
                    for identity in DOCUMENT_IDS:
                        row = db.get(StockNews, identity)
                        plan = row.content_json["semantic_governance_plan"]
                        decide_plan(db, row, decision="CONFIRM", primary_type=plan["primary_type"],
                            secondary_types=plan.get("secondary_types", []), skill_codes=[skill.skill_code],
                            target_scopes=plan["target_scopes"], reviewer="隔离副本模拟审核员",
                            reason="仅验收链路；未确认生产文档", expected_plan_id=plan["plan_id"],
                            expected_revision=plan["revision"])
                    stock_batch.SessionLocal = isolated
                    print("隔离副本模拟启用和确认，开始正式Agent+Skill治理路径验证", flush=True)
                    executed = stock_batch.execute_stock_batch_governance({"request": {
                        "stocks": [{"market": stock.market, "symbol": stock.symbol}],
                        "governance_mode": "AI_AGENT_SKILL_GOVERNANCE", "structure_documents": True,
                        "semantic_analysis": True, "interactive_plan_required": True,
                        "document_targets": [{"source_table": "stock_news", "record_id": i} for i in DOCUMENT_IDS],
                        "collect_business_data": False, "export_lakehouse": False, "archive_chunks": False,
                        "run_graph": False, "run_agent_governance": False, "business_types": [],
                        "document_skill_codes": [skill.skill_code]}})
                    db.expire_all()
                    reports = []
                    for identity in DOCUMENT_IDS:
                        row = db.get(StockNews, identity)
                        state = row.content_json.get("event_governance") or {}
                        reports.append({"record_id": identity, "status": state.get("status"),
                            "error": state.get("error"), "analysis": state.get("semantic_analysis"),
                            "report": result_report(db, row, state, len(versions_for(row)) - 1)})
                    result.update(reports=reports, execution_errors=executed.get("errors"),
                        passed=all(r["status"] == "ANALYZED" and r["report"] is not None for r in reports))
                result["model_calls"] = [{"id": log.id, "model": log.model_code,
                    "status": log.status, "error": log.error_message}
                    for log in db.scalars(select(ModelCallLog))]
                persist(result, model)
                print(json.dumps({"passed": result["passed"], "model": model,
                    "test_result": (acceptance.get("test_result") or {}).get("summary"),
                    "model_failures": [l for l in result["model_calls"] if l["status"] != "SUCCESS"]}, ensure_ascii=False), flush=True)
        except Exception as exc:
            result["exception"] = str(exc)
            persist(result, model)
            raise
        finally:
            skill_files.SKILL_ROOT = original_root
            engine.dispose()
    return result["passed"]


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", required=True, choices=["QWEN_PLUS", "GEMINI_FLASH", "GEMINI_PRO", "DEEPSEEK_CHAT"])
    parser.add_argument("--iterations", type=int, default=3)
    parser.add_argument("--model-code", help="服务商建议的替代型号，仅用于隔离副本")
    args = parser.parse_args()
    raise SystemExit(0 if main(args.model, args.iterations, args.model_code) else 1)
