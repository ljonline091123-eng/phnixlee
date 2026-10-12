"""Real configured model verification; all writes and approvals use an in-memory DB."""
from copy import deepcopy
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import shutil
import tempfile

from sqlalchemy import create_engine, select, inspect
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

import app.models  # noqa: F401
from app.db.base import Base
from app.db.session import SessionLocal
from app.models.ai_hub import ModelProvider, ModelInstance, ModelRouteRule, ModelSkill, ModelCallLog
from app.models.foundation import FoundationEntity, FoundationSecurity, FoundationListing, FoundationEvidence, FoundationFact
from app.models.market_data import DataSource, StockSymbol, StockNews
from app.models.governance import SkillExecutionRun
from app.services import skill_files, stock_batch
from app.services.resource_hub import seed_default_data_assets, seed_default_knowledge_bases, seed_default_agents
from app.services.document_semantic_planning import decide_plan, result_report, review_result
from app.services.document_semantic_review import versions_for


OUTPUT = Path(__file__).resolve().parents[1] / "validation" / "interactive-semantic"


def persist_result(result, *, feedback_loop=False):
    OUTPUT.mkdir(parents=True, exist_ok=True)
    filename = "real-model-loop-verification.json" if feedback_loop else "real-model-verification.json"
    content = json.dumps(result, ensure_ascii=False, indent=2, default=str)
    (OUTPUT / filename).write_text(content, encoding="utf-8")
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
    (OUTPUT / f"{Path(filename).stem}-{stamp}.json").write_text(content, encoding="utf-8")


def semantic_checks(analysis):
    events = analysis.get("events") or []
    metrics = [m for event in events for m in event.get("metrics") or []]
    def metric(value, qualifier=None):
        return any(str(m.get("normalized_value")) == value and (qualifier is None or m.get("qualifier") == qualifier) for m in metrics)
    return {
        "计提1.07亿元": metric("107000000.00"),
        "负经营现金流2.65亿元": metric("-265000000.00"),
        "利润负向影响": any(i.get("dimension") == "PROFIT" and i.get("direction") == "NEGATIVE" for e in events for i in e.get("impacts") or []),
        "质押借款2.4亿元为上限": metric("240000000.00", "UPPER_BOUND"),
        "银行借款4亿元为上限": metric("400000000.00", "UPPER_BOUND"),
        "套利评价为作者观点": any(e.get("assertion_type") == "AUTHOR_OPINION" and "套利" in (e.get("summary") or "") for e in events),
        "诉讼程序边界": any("驳回" in (e.get("summary") or "") and "受案范围" in (e.get("summary") or "") for e in events),
    }


def model_diagnostics(db, *, failed=False):
    # ModelHub stores a redacted, user-safe error_message. Never export request
    # payloads, provider configuration or credentials from the source database.
    return [{"id": log.id, "status": log.status, "error": log.error_message,
             "model_code": log.model_code,
             **({"response_text": log.response_text} if failed else {})}
            for log in db.scalars(select(ModelCallLog))]


def clone(db, row):
    db.merge(type(row)(**{column.key: deepcopy(getattr(row, column.key))
                         for column in inspect(type(row)).columns}))
    db.flush()


def main(*, feedback_loop=False):
    engine = create_engine("sqlite://", poolclass=StaticPool, connect_args={"check_same_thread": False})
    Base.metadata.create_all(engine)
    isolated = sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)
    with tempfile.TemporaryDirectory(prefix="semantic-live-") as temporary:
        original_root = skill_files.SKILL_ROOT
        skill_files.SKILL_ROOT = Path(temporary) / "skill_docs"
        shutil.copytree(original_root, skill_files.SKILL_ROOT)
        try:
            with SessionLocal() as source, isolated() as target:
                # Source session remains read-only; credentials exist only in memory.
                for model in (ModelProvider, ModelInstance, ModelRouteRule, ModelSkill):
                    for row in source.scalars(select(model)):
                        clone(target, row)
                    target.flush()
                news = source.get(StockNews, 8718)
                assert news, "验证新闻不存在"
                stock = source.scalar(select(StockSymbol).where(StockSymbol.market == news.market, StockSymbol.symbol == news.symbol))
                clone(target, source.get(DataSource, stock.source_id))
                clone(target, source.get(DataSource, news.source_id))
                clone(target, stock)
                listing = source.scalar(select(FoundationListing).where(FoundationListing.stock_symbol_id == stock.id))
                assert listing, "证券公司映射缺失"
                security = source.get(FoundationSecurity, listing.security_id)
                clone(target, source.get(FoundationEntity, security.entity_id))
                clone(target, security)
                clone(target, source.get(FoundationEvidence, listing.evidence_id))
                clone(target, listing)
                clone(target, news)
                target.commit()
                copied = target.get(StockNews, news.id)
                # Keep original full text; exclude production governance from this isolated trial.
                copied.content_json = {key: value for key, value in copied.content_json.items()
                                       if key in {"governance_full_text", "governance_news_extraction"}}
                target.commit()
                seed_default_data_assets(target); seed_default_knowledge_bases(target); seed_default_agents(target)
                target_stock, target_news = stock.id, news.id
            stock_batch.SessionLocal = isolated
            request = {"stocks": [{"market": "CN_A", "symbol": "000159"}],
                       "governance_mode": "AI_AGENT_SKILL_GOVERNANCE", "structure_documents": True,
                       "semantic_analysis": True, "interactive_plan_required": True,
                       "document_targets": [{"source_table": "stock_news", "record_id": target_news}],
                       "collect_business_data": False, "export_lakehouse": False, "archive_chunks": False,
                       "run_graph": False, "run_agent_governance": False, "business_types": [],
                       "semantic_plan_only": True}
            print("真实模型：开始全文类型识别与方案编排", flush=True)
            planned = stock_batch.execute_stock_batch_governance({"request": request})
            with isolated() as db:
                row = db.get(StockNews, target_news)
                plan = (row.content_json or {}).get("semantic_governance_plan")
                if not plan:
                    result = {"passed": False, "status": "PLAN_FAILED",
                              "isolation": "in-memory SQLite; production read only",
                              "errors": planned.get("errors"), "model_diagnostics": model_diagnostics(db, failed=True)}
                    persist_result(result, feedback_loop=feedback_loop)
                    print(json.dumps(result, ensure_ascii=False, default=str), flush=True)
                    raise AssertionError("真实模型方案生成失败，诊断已保存")
                decide_plan(db, row, decision="CONFIRM", primary_type=plan["primary_type"],
                            secondary_types=plan["secondary_types"], skill_codes=plan["recommended_skill_codes"],
                            target_scopes=plan["target_scopes"], reviewer="隔离验证副本审核员",
                            reason="仅用于隔离链路验证，不确认生产文档", expected_plan_id=plan["plan_id"],
                            expected_revision=plan["revision"])
            print("真实模型：方案已在隔离副本确认，开始Agent+Skill语义治理", flush=True)
            request.update(semantic_plan_only=False, document_skill_codes=plan["recommended_skill_codes"])
            executed = stock_batch.execute_stock_batch_governance({"request": request})
            with isolated() as db:
                row = db.get(StockNews, target_news)
                state = row.content_json["event_governance"]
                report = result_report(db, row, state, len(versions_for(row)) - 1)
                analysis = deepcopy(state.get("semantic_analysis") or {})
                checks = semantic_checks(analysis)
                passed = state.get("status") == "ANALYZED" and all(checks.values())
                audit = [{"skill_code": r.skill_code, "status": r.status, "version": r.skill_version}
                         for r in db.scalars(select(SkillExecutionRun))]
                facts = list(db.scalars(select(FoundationFact)))
                assert all(f.status == "PENDING" for f in facts), "隔离验证不得自动发布"
                result = {"passed": passed, "isolation": "in-memory SQLite; production read only",
                          "stock_id": target_stock, "source_record_id": target_news,
                          "plan": plan, "report": report, "analysis": analysis, "semantic_checks": checks, "skill_audit": audit,
                          "model_calls": state.get("model_calls"), "fact_count": len(facts),
                          "model_diagnostics": model_diagnostics(db, failed=not passed),
                          "errors": executed.get("errors"), "status": state.get("status")}
                persist_result(result, feedback_loop=feedback_loop)
                print(json.dumps({k: result[k] for k in ("passed", "status", "fact_count", "errors")}, ensure_ascii=False), flush=True)
                assert passed, {"errors": result["errors"], "semantic_checks": checks}
                if feedback_loop:
                    target_event = next(e for e in analysis["events"] if "计提" in e["summary"] and
                        any(m.get("normalized_value") == "107000000.00" for m in e.get("metrics") or []))
                    target_key = target_event["key"]
                    original_index = len(versions_for(row)) - 1
                    feedback = review_result(db, row, version_index=original_index,
                        expected_version_key=report["version_key"], expected_plan_id=report["plan_id"],
                        reviewer="隔离副本模拟审核员", decision="CONFIRM", reason="仅验证交互闭环",
                        explicit_confirmations=[f"{p['point_type']}:{p['key']}" for p in report["knowledge_points"]
                            if p["confirmation_level"] == "REQUIRED" and p["key"] != target_key],
                        nonconforming=[{"point_type": "EVENT", "key": target_key,
                            "reason": "请在本知识点摘要明确区分会计计提与实际支付：1.07亿元是计提预计负债及预计利润减少，不代表已实际缴纳全部税款。保留原文金额、时间、利润影响和证据，不改动其他已确认知识点。"}])
                    assert feedback["status"] == "NEEDS_CORRECTION"
            if feedback_loop:
                print("真实模型：隔离副本提交用户意见，定向重分析一个知识点", flush=True)
                request.update(semantic_feedback=feedback["semantic_feedback"], semantic_review_id=feedback["semantic_review_id"])
                correction = stock_batch.execute_stock_batch_governance({"request": request})
                with isolated() as db:
                    row = db.get(StockNews, target_news)
                    state = row.content_json["event_governance"]
                    corrected = state.get("semantic_analysis") or {}
                    new_index = len(versions_for(row)) - 1
                    current_report = result_report(db, row, state, new_index)
                    stable_events = all(next((e for e in corrected.get("events") or [] if e["key"] == old["key"]), None) == old
                        for old in analysis["events"] if old["key"] != target_key)
                    new_event = next((e for e in corrected.get("events") or [] if e["key"] == target_key), {})
                    loop_checks = {"新历史版本": new_index > original_index,
                        "定向修正项保留身份": new_event.get("key") == target_key,
                        "修正项建立新事实": new_event.get("fact_id") != target_event.get("fact_id"),
                        "修正摘要有更新": new_event.get("summary") != target_event.get("summary"),
                        "其他已确认知识及事实ID保留": stable_events,
                        "已确认关系保留": corrected.get("relations") == analysis.get("relations"),
                        "语义专项门禁": all(semantic_checks(corrected).values()),
                        "模型修正通过": state.get("status") == "ANALYZED" and state.get("result_review_status") != "NEEDS_CORRECTION"}
                    result.update(correction_checks=loop_checks, correction_analysis=corrected,
                        correction_report=current_report, correction_errors=correction.get("errors"),
                        correction_model_calls=state.get("model_calls"),
                        model_diagnostics=model_diagnostics(db, failed=not all(loop_checks.values())),
                        user_decisions="模拟审核意见及确认仅作用于内存副本，未审核或发布生产文档",
                        passed=all(loop_checks.values()))
                    persist_result(result, feedback_loop=True)
                    assert result["passed"], loop_checks
                    final = review_result(db, row, version_index=new_index,
                        expected_version_key=current_report["version_key"], expected_plan_id=current_report["plan_id"],
                        reviewer="隔离副本模拟审核员", decision="CONFIRM", reason="仅验证发布事务与报告，非生产事实核验",
                        nonconforming=[], explicit_confirmations=[f"{p['point_type']}:{p['key']}" for p in current_report["knowledge_points"]
                            if p["confirmation_level"] == "REQUIRED"])
                    state = row.content_json["event_governance"]
                    result.update(isolated_publication=final, final_report=result_report(db, row, state, new_index),
                        history_version_count=len(versions_for(row)), model_diagnostics=model_diagnostics(db))
                    assert final["status"] == "PUBLISHED"
                    assert all(db.get(FoundationFact, identity).status == "ACCEPTED" for identity in final["fact_ids"])
                    result["correction_checks"]["隔离发布与事实事务"] = True
                    persist_result(result, feedback_loop=True)
                    print(json.dumps({"passed": True, "feedback_loop": True, "history_versions": len(versions_for(row)),
                        "published_points_in_memory": final["knowledge_point_count"]}, ensure_ascii=False), flush=True)
        finally:
            skill_files.SKILL_ROOT = original_root
            engine.dispose()


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--verify-feedback-loop", action="store_true")
    main(feedback_loop=parser.parse_args().verify_feedback_loop)
