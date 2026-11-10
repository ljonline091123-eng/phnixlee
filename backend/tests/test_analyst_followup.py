"""Dialogue/confirmation and durable daily monitoring contracts, isolated from live data."""
from copy import deepcopy
from datetime import datetime, timedelta, timezone
import json
from types import SimpleNamespace
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient
from pydantic import ValidationError
from sqlalchemy import func, select, text, inspect
from sqlalchemy.orm import sessionmaker

from tests.test_stock_analyst import db, log_result, output, payload
from app.db.session import get_db
from app.main import app
from app.models.market_data import StockKline, StockNews, StockNotice, DataSource, DataInterface
from app.models.pipeline import ScheduledJob
from app.models.stock_analyst import AiAnalystDailyReview, AiAnalystReportRevision, AiStockAnalysisRecord, AiAnalystWatch, AiAnalystWatchSuggestion
from app.schemas.stock_analyst import AnalystDialogueCreate, AnalystReportConfirm, AnalystWatchConfig, AnalystDialogueOutput, AnalystSuggestionCreate, AnalystWatchSuggestionOutput
from app.services import analyst_followup as service
from app.services.stock_analyst import build_stock_analyst_context, queue_stock_analysis


@pytest.fixture
def analysis(db):
    row = queue_stock_analysis(db, payload())
    _, snapshot, coverage = build_stock_analyst_context(
        db, market=row.market, symbol=row.symbol, period_months=row.period_months, include_all=True,
    )
    row.evidence_snapshot_json = snapshot
    row.coverage_json = {**coverage, "review_mode": "FULL_SINGLE_PASS",
                         "reviewed_evidence_count": len(snapshot["evidence"])}
    row.status = "COMPLETED"
    row.result_json = {**output(snapshot["evidence_allowlist"][0]),
                       "watch_items": ["观察订单回款", "关注亏损收窄"]}
    db.commit()
    return row


@pytest.fixture(autouse=True)
def stub_followup_refresh(monkeypatch):
    monkeypatch.setattr("app.services.analyst_followup.sync_stock_data_for_analysis", lambda db, **kwargs: {
        "status": "SUCCESS", "items": [], "source_policy": "TEST_REGISTERED_SOURCES",
    })
    monkeypatch.setattr("app.services.analyst_followup._extract_material_document_bodies", lambda db, **kwargs: {
        "attempted": 0, "completed": 0, "failed": 0, "errors": [],
    })
    def review(_db, *, segments, instance_code, **kwargs):
        return ({"reviewed_segment_keys": [item["segment_key"] for item in segments],
                 "batch_summary": "测试中已完整审阅本批本地证据。", "material_events": [],
                 "cross_document_links": [], "contradictions": [], "limitations": []},
                SimpleNamespace(id=999, instance_code=instance_code, request_json={}))
    monkeypatch.setattr(service, "_review_evidence_batch", review)


def model_output(analysis):
    return {"reply": "综合现有证据，收入增长还需要订单回款和盈利改善共同验证。",
            "suggested_judgment": "维持观察判断，等待收入增长与盈利改善形成相互印证。",
            "focus_points": ["观察订单回款", "关注亏损收窄"],
            "evidence": [{"evidence_id": analysis.evidence_snapshot_json["evidence_allowlist"][0], "claim": "收入与盈利需结合验证"}]}


def followup_log(db, result, **kwargs):
    log = log_result(db, result, **kwargs)
    log.request_json = {"metadata_json": {"skill_chain": [{"skill_code": service.FOLLOWUP_SKILL, "skill_version": "2.3.4"}]}}
    db.commit()
    return log


@pytest.mark.parametrize("fault", ["unclosed", "misnested"])
def test_followup_retries_invalid_output_with_same_model_and_evidence(db, analysis, fault):
    valid = model_output(analysis)
    invalid = followup_log(db, valid)
    invalid.response_text = (json.dumps(valid, ensure_ascii=False)[:-1] if fault == "unclosed"
        else json.dumps({"conditions": valid}, ensure_ascii=False))
    repaired = followup_log(db, valid)
    with patch.object(service.ModelHubService, "chat", side_effect=[invalid, repaired]) as call:
        result, log = service._call_model(db, schema=AnalystDialogueOutput, prompt="相同证据集",
            instance="REAL_TEST", metadata={"analysis_record_id": analysis.id})
    assert result == valid and log.id == repaired.id
    assert call.call_count == 2
    assert all(c.kwargs["instance_code"] == "REAL_TEST" for c in call.call_args_list)
    retry = call.call_args_list[1].kwargs
    assert retry["messages"][0]["content"].startswith("相同证据集")
    assert "上次输出未通过格式校验" in retry["messages"][0]["content"]
    assert retry["metadata_json"]["previous_output_error"]
    assert retry["metadata_json"]["output_attempt"] == 2


def dialogue(db, analysis):
    row = service.queue_dialogue(db, analysis.id, AnalystDialogueCreate(question="什么证据会改变判断？"))
    with patch.object(service.ModelHubService, "chat", return_value=followup_log(db, model_output(analysis))):
        return service.execute_dialogue(db, row.id)


def test_dialogue_requires_completed_analysis_and_serializes_pending(db, analysis):
    analysis.status = "FAILED"
    db.commit()
    with pytest.raises(ValueError, match="有效"):
        service.queue_dialogue(db, analysis.id, AnalystDialogueCreate(question="依据是什么"))
    analysis.status = "COMPLETED"
    db.commit()
    service.queue_dialogue(db, analysis.id, AnalystDialogueCreate(question="依据是什么"))
    with pytest.raises(ValueError, match="处理中"):
        service.queue_dialogue(db, analysis.id, AnalystDialogueCreate(question="再问一个问题"))


def test_local_document_retrieval_finds_traditional_board_notice_by_date_and_topic(db, analysis):
    notice = StockNotice(market="HK", symbol="02533", notice_date="2026-01-30",
        title="董事調任及行政總裁職能變更", source_id=db.scalar(select(DataSource.id)),
        content_json={"governance_full_text": "公司于2026年1月30日披露董事調任。劉衛紅由執行董事調任非執行董事，並不再擔任總裁。"})
    db.add(notice)
    db.commit()
    rows = service.local_document_matches(db, analysis,
        "2026年1月30日董事调任，刘卫红由执行董事改为非执行董事并不再担任总裁", limit=8)
    assert rows
    assert rows[0]["evidence_id"] == f"notice:{notice.id}"
    assert rows[0]["data"]["content_status"] == "SOURCE_TEXT"
    assert rows[0]["excerpt"] == notice.content_json["governance_full_text"]


def test_dialogue_syncs_then_reads_and_cites_matching_local_notice(db, analysis, monkeypatch):
    notice = StockNotice(market="HK", symbol="02533", notice_date="2026-01-30",
        title="董事調任及行政總裁職能變更", source_id=db.scalar(select(DataSource.id)),
        content_json={"governance_full_text": "公司于2026年1月30日披露董事調任。劉衛紅由執行董事調任非執行董事，並不再擔任總裁。"})
    db.add(notice)
    db.commit()
    row = service.queue_dialogue(db, analysis.id,
        AnalystDialogueCreate(question="2026年1月30日董事调任，刘卫红不再担任总裁，公告怎么说？"))
    result = model_output(analysis)
    result["evidence"] = [{"evidence_id": f"notice:{notice.id}", "claim": "公告正文披露董事职务调整及总裁变更"}]
    call = followup_log(db, result)
    reviewed = []
    def review(_db, *, segments, instance_code, **kwargs):
        reviewed.extend(segments)
        return ({"reviewed_segment_keys": [item["segment_key"] for item in segments],
                 "batch_summary": "本轮新增公告全文揭示联合创始人职务调整。",
                 "material_events": [{"event_type": "管理层调整", "summary": "联合创始人退出执行董事及总裁职务",
                    "impact_hypothesis": "可能影响业务拓展或战略执行，仍需经营数据验证",
                    "direction": "待核验", "evidence_ids": [f"notice:{notice.id}"]}],
                 "cross_document_links": [], "contradictions": [], "limitations": []},
                SimpleNamespace(id=998, instance_code=instance_code, request_json={}))
    monkeypatch.setattr(service, "_review_evidence_batch", review)
    progress_events = []
    original_progress = service._set_dialogue_progress
    def capture_progress(_db, dialogue_row, stage, message, **details):
        progress_events.append((stage, message, details))
        return original_progress(_db, dialogue_row, stage, message, **details)
    monkeypatch.setattr(service, "_set_dialogue_progress", capture_progress)
    def sync(_db, **kwargs):
        kwargs["progress_callback"]("SYNCING", "正在刷新登记来源：公告")
        return {"status": "SUCCESS", "items": [], "source_policy": "TEST_REGISTERED_SOURCES"}
    monkeypatch.setattr(service, "sync_stock_data_for_analysis", sync)
    with patch.object(service.ModelHubService, "chat", return_value=call) as model_call:
        saved = service.execute_dialogue(db, row.id)
    assert saved.status == "COMPLETED"
    assert saved.context_snapshot_json["automatically_matched_local_documents"] == [f"notice:{notice.id}"]
    sent_prompt = model_call.call_args.kwargs["messages"][0]["content"]
    assert notice.title in sent_prompt and notice.content_json["governance_full_text"] in sent_prompt
    assert any(item["evidence_id"] == f"notice:{notice.id}" and notice.content_json["governance_full_text"] in item["text"]
               for item in reviewed)
    assert "本轮新增公告全文揭示联合创始人职务调整" in sent_prompt
    coverage = saved.context_snapshot_json["full_local_review_coverage"]
    assert coverage["status"] == "COMPLETE" and coverage["added_evidence_count"] == 1
    assert coverage["reviewed_segment_count"] == coverage["total_segment_count"]
    assert "full_local_review_state" in saved.context_snapshot_json
    assert [item[0] for item in progress_events][:3] == ["SYNCING", "SYNCING", "READING"]
    assert any("第1/" in item[1] and item[0] == "REVIEWING" for item in progress_events)
    assert saved.context_snapshot_json["progress"]["stage"] == "COMPLETED"
    assert saved.reply_json["evidence"][0]["evidence_id"] == f"notice:{notice.id}"

    next_row = service.queue_dialogue(db, analysis.id,
        AnalystDialogueCreate(question="再核实一次2026年1月30日董事调任公告"))
    with patch.object(service, "_review_evidence_batch", side_effect=AssertionError("未变化记录不应重复调用审阅模型")), \
         patch.object(service.ModelHubService, "chat", return_value=followup_log(db, result)):
        next_saved = service.execute_dialogue(db, next_row.id)
    assert next_saved.status == "COMPLETED"
    next_coverage = next_saved.context_snapshot_json["full_local_review_coverage"]
    assert next_coverage["unchanged_evidence_count"] == next_coverage["current_evidence_count"]


def test_incomplete_legacy_baseline_requires_full_review_and_fails_closed(db, analysis, monkeypatch):
    analysis.coverage_json = {"status": "FULL_LOCAL_SNAPSHOT_READY"}
    db.commit()
    row = service.queue_dialogue(db, analysis.id, AnalystDialogueCreate(question="请重新审阅该股票本地资料"))
    reviewed = []
    def incomplete_review(_db, *, segments, instance_code, **kwargs):
        reviewed.extend(segments)
        keys = [item["segment_key"] for item in segments]
        return ({"reviewed_segment_keys": keys[:-1], "batch_summary": "覆盖不完整",
                 "material_events": [], "cross_document_links": [], "contradictions": [], "limitations": []},
                SimpleNamespace(id=997, instance_code=instance_code, request_json={}))
    monkeypatch.setattr(service, "_review_evidence_batch", incomplete_review)
    with patch.object(service.ModelHubService, "chat") as model_call:
        saved = service.execute_dialogue(db, row.id)
    assert saved.status == "FAILED"
    assert "审阅覆盖不完整" in saved.error_message
    assert {item["evidence_id"] for item in reviewed} == {
        item["evidence_id"] for item in analysis.evidence_snapshot_json["evidence"]
    }
    model_call.assert_not_called()


def test_dialogue_audit_and_user_confirmed_versions_preserve_original(db, analysis):
    original = deepcopy(analysis.result_json)
    row = dialogue(db, analysis)
    assert row.status == "COMPLETED" and row.skill_version == "2.3.4" and row.model_call_log_id
    assert service.latest_revision(db, analysis.id) is None
    data = AnalystReportConfirm(dialogue_id=row.id, judgment=model_output(analysis)["suggested_judgment"], focus_points=["观察订单回款"])
    revision = service.confirm_report(db, analysis.id, data)
    assert revision.version == 1 and revision.report_json["confirmed_judgment"] == data.judgment
    assert service.confirm_report(db, analysis.id, data).id == revision.id
    with pytest.raises(ValueError, match="新版本"):
        service.confirm_report(db, analysis.id, data.model_copy(update={"judgment": "已有新判断，不应悄悄覆盖他人确认的版本内容"}))
    revision2 = service.confirm_report(db, analysis.id, data.model_copy(update={"expected_revision_id": revision.id, "judgment": "等待主营业务实现持续增长，再考虑提高关注程度"}))
    assert revision2.version == 2
    assert db.scalar(select(func.count()).select_from(AiAnalystReportRevision)) == 2
    assert db.get(AiStockAnalysisRecord, analysis.id).result_json == original
    other = queue_stock_analysis(db, payload())
    other.status = "COMPLETED"
    db.commit()
    with pytest.raises(ValueError, match="本次报告"):
        service.confirm_report(db, other.id, data)


def test_confirmed_dialogue_updates_current_watch_observations_without_overwriting_report(db, analysis):
    original = deepcopy(analysis.result_json)
    watch = service.save_watch(db, analysis.id, AnalystWatchConfig(
        selected_observations=["旧观察"], price_lower=6, event_conditions=[{"keywords": ["订单"]}]))
    row = dialogue(db, analysis)
    revision = service.confirm_report(db, analysis.id, AnalystReportConfirm(
        dialogue_id=row.id, judgment="对话确认后，将主营兑现和现金流变化作为当前判断核心。",
        focus_points=["主营订单兑现", "经营现金流改善"]))
    db.refresh(watch)
    assert revision.focus_points_json == ["主营订单兑现", "经营现金流改善"]
    assert watch.config_version == 2
    assert watch.config_json["selected_observations"] == revision.focus_points_json
    assert watch.config_json["price_lower"] == 6
    assert watch.config_json["observation_rules"][0]["event_conditions"][0]["keywords"] == ["订单"]
    assert watch.config_json["event_conditions"] == []
    assert analysis.result_json == original


def test_watch_selection_filters_daily_observations(db, analysis):
    analysis.result_json = {**analysis.result_json, "watch_items": ["观察订单兑现", "观察现金流", "观察股东行为"]}
    db.commit()
    selected = service.tracking.observations(analysis, None, ["观察现金流", "不存在的项目"])
    assert selected == [{"observation_id": "O1", "text": "观察现金流"}]


@pytest.mark.parametrize("mode", ["provider", "invalid_reference", "missing_references", "malformed"])
def test_dialogue_failure_does_not_modify_report_or_fabricate_reply(db, analysis, mode):
    row = service.queue_dialogue(db, analysis.id, AnalystDialogueCreate(question="请验证当前结论"))
    result = model_output(analysis)
    if mode == "invalid_reference":
        result["evidence"][0]["evidence_id"] = "news:999999"
    if mode == "missing_references":
        result["evidence"] = []
    log = followup_log(db, result, status="FAILED" if mode == "provider" else "SUCCESS")
    if mode == "malformed":
        log.response_text = "不是有效JSON"
    with patch.object(service.ModelHubService, "chat", return_value=log):
        saved = service.execute_dialogue(db, row.id)
    assert saved.status == "FAILED" and saved.reply_json == {} and saved.error_message
    assert service.latest_revision(db, analysis.id) is None
    assert saved.model_call_log_id == log.id


def test_output_truncation_retries_only_once_with_larger_budget(db, analysis):
    row = service.queue_dialogue(db, analysis.id, AnalystDialogueCreate(question="请分析关键证据"))
    truncated = followup_log(db, model_output(analysis))
    truncated.response_json = {"candidates": [{"finishReason": "MAX_TOKENS"}]}
    good = followup_log(db, model_output(analysis))
    with patch.object(service.ModelHubService, "chat", side_effect=[truncated, good]) as call:
        saved = service.execute_dialogue(db, row.id)
    assert saved.status == "COMPLETED"
    assert [c.kwargs["max_tokens"] for c in call.call_args_list] == [8192, 12288]
    assert call.call_args.kwargs["metadata_json"]["strict_instance"] is True


@pytest.mark.parametrize("config", [
    {"price_lower": 20, "price_upper": 10}, {"price_lower": float("nan")},
    {"volume_mode": "SHRINK", "volume_ratio": 1.5}, {"volume_mode": "EXPAND", "volume_ratio": .5},
    {"event_conditions": [{"keywords": ["  "]}]}, {"volume_window": 4},
])
def test_invalid_conditions_rejected(config):
    with pytest.raises(ValidationError):
        AnalystWatchConfig(**config)


def test_real_prices_volume_baseline_body_keywords_and_market_isolation(db, analysis):
    source = db.scalar(select(DataSource))
    today = datetime.now(timezone.utc).date()
    trading_day = today if datetime.now(service.LOCAL_ZONE).hour >= 16 else today - timedelta(days=1)
    current = db.scalar(select(StockKline).where(StockKline.adjust == ""))
    current.trade_date, current.close_price, current.volume = trading_day.isoformat(), 500, 300
    for index in range(1, 6):
        db.add(StockKline(market="HK", symbol="02533", trade_date=(trading_day-timedelta(days=index)).isoformat(), adjust="", close_price=9, volume=100, source_id=source.id))
    db.add_all([
        StockNotice(market="HK", symbol="02533", notice_date=today.isoformat(), title="公司进展", content_json={"content": "公司决定回购股份"}, source_id=source.id),
        StockNews(market="CN_A", symbol="02533", news_time=today.isoformat(), title="收购事件不属于港股公司", source_id=source.id),
    ])
    db.commit()
    cfg = AnalystWatchConfig(price_lower=499, price_upper=500, volume_mode="EXPAND", volume_ratio=2, volume_window=5,
                             event_conditions=[{"category": "NOTICE", "keywords": ["回购"]}, {"keywords": ["收购"]}]).model_dump()
    conditions, matched = service.evaluate_conditions(db, analysis, cfg, today.isoformat(), today.isoformat())
    assert [c["status"] for c in conditions] == ["NOT_HIT", "HIT", "HIT", "HIT", "NOT_HIT"]
    assert conditions[2]["value"] == 3 and conditions[2]["baseline"] == 100
    assert len(matched) == 2 and matched[0]["evidence_id"].startswith("market-watch:")
    assert matched[1]["evidence_id"].startswith("notice:")
    stale, _ = service.evaluate_conditions(db, analysis, cfg, (today+timedelta(days=10)).isoformat(), today.isoformat())
    assert all(c["status"] == "UNKNOWN" for c in stale[:3])


def test_scheduler_idempotent_daily_queue_and_manual_attempt_history(db, analysis):
    watch = service.save_watch(db, analysis.id, AnalystWatchConfig())
    with patch("app.jobs.governance_runner.wake_governance_runner"):
        first = service.enqueue_review(db, watch, business_date="2026-11-09")
        assert service.enqueue_review(db, watch, business_date="2026-11-09").id == first.id
        assert service.enqueue_review(db, watch, business_date="2026-11-09", force=True).id == first.id
        assert db.scalar(select(func.count()).select_from(ScheduledJob)) == 1
        first.status = "COMPLETED"
        db.commit()
        watch = service.save_watch(db, analysis.id, AnalystWatchConfig(price_lower=8))
        second = service.enqueue_review(db, watch, business_date="2026-11-09", force=True)
    assert second.attempt == 2 and second.config_snapshot_json["config_version"] == 2
    assert first.config_snapshot_json.get("price_lower") is None
    assert db.scalar(select(func.count()).select_from(ScheduledJob)) == 2


@pytest.mark.parametrize("keyword,body", [("訂單履約", "新订单履约已完成"), ("订单履约", "新訂單履約已完成")])
def test_monitor_matches_simplified_and_traditional_without_changing_source(db, analysis, keyword, body):
    today = datetime.now(service.LOCAL_ZONE).date().isoformat()
    doc = StockNews(market="HK", symbol="02533", news_time=today, title="业务动态", content=body,
        source_id=db.scalar(select(DataSource.id)))
    db.add(doc)
    db.commit()
    cfg = AnalystWatchConfig(event_conditions=[{"category": "NEWS", "keywords": [keyword]},
        {"category": "NEWS", "keywords": ["股份注销"]}]).model_dump()
    conditions, evidence = service.evaluate_conditions(db, analysis, cfg, today, today)
    assert [r["status"] for r in conditions] == ["HIT", "NOT_HIT"]
    assert f"news:{doc.id}" in conditions[0]["evidence_ids"]
    assert next(r for r in evidence if r["evidence_id"] == f"news:{doc.id}")["excerpt"] == body
    assert cfg["event_conditions"][0]["keywords"] == [keyword]


def test_suggestion_volume_baseline_matches_monitor_and_excludes_latest_bar(db, analysis):
    today = datetime.now(service.LOCAL_ZONE).date()
    trading_day = today if datetime.now(service.LOCAL_ZONE).hour >= 16 else today - timedelta(days=1)
    current = db.scalar(select(StockKline).where(StockKline.adjust == ""))
    current.trade_date, current.close_price, current.volume = trading_day.isoformat(), 500, 900
    for index in range(1, 7):
        db.add(StockKline(market="HK", symbol="02533", adjust="", source_id=current.source_id,
            trade_date=(trading_day-timedelta(days=index)).isoformat(), close_price=500, volume=index * 100))
    db.commit()
    market = service._suggestion_market(db, analysis)
    baseline = market["data"]["volume_baselines"]["5"]
    cfg = AnalystWatchConfig(volume_mode="EXPAND", volume_window=5, volume_ratio=2).model_dump()
    conditions, _ = service.evaluate_conditions(db, analysis, cfg, today.isoformat(), today.isoformat())
    assert baseline["complete"] and baseline["average_volume"] == conditions[0]["baseline"] == 300
    assert conditions[0]["value"] == 3
    assert baseline["end_date"] == (trading_day-timedelta(days=1)).isoformat()
    assert not market["data"]["volume_baselines"]["10"]["complete"]
    previous = db.scalar(select(StockKline).where(StockKline.adjust == "",
        StockKline.trade_date == (trading_day-timedelta(days=2)).isoformat()))
    previous.volume = None
    db.commit()
    incomplete = service._suggestion_market(db, analysis)["data"]["volume_baselines"]["5"]
    assert not incomplete["complete"] and incomplete["average_volume"] is None


def daily_output(analysis):
    return {"summary": "本次复核尚未发现足以改变整体判断的新证据。", "judgment": "继续观察收入质量和订单兑现，不因关键词命中直接提高置信度。",
            "verdict": "UNCHANGED", "confidence": .5, "changes": [], "next_focus": ["观察主营业务"],
            "evidence": model_output(analysis)["evidence"]}


def test_daily_review_partial_unknown_provenance_and_failure_retains_history(db, analysis):
    watch = service.save_watch(db, analysis.id, AnalystWatchConfig(event_conditions=[{"keywords": ["收购"]}]))
    with patch("app.jobs.governance_runner.wake_governance_runner"):
        review = service.enqueue_review(db, watch)
    collection = [{"category": "NEWS", "status": "FAILED", "error": "来源超时"}, {"category": "NOTICE", "status": "SUCCESS", "count": 0}]
    with patch.object(service, "refresh_monitor_data", return_value=collection), patch.object(service.ModelHubService, "chat", return_value=followup_log(db, daily_output(analysis))):
        result = service.run_daily_review(db, review.id)
    assert result["status"] == "PARTIAL"
    assert review.conditions_json[0]["status"] == "UNKNOWN"
    assert review.skill_version == "2.3.4" and review.model_call_log_id
    assert review.evidence_snapshot_json["evidence"]
    first_result = deepcopy(review.result_json)
    with patch("app.jobs.governance_runner.wake_governance_runner"):
        retry = service.enqueue_review(db, watch, force=True)
    with patch.object(service, "refresh_monitor_data", return_value=collection), patch.object(service.ModelHubService, "chat", return_value=followup_log(db, {}, status="FAILED")):
        result = service.run_daily_review(db, retry.id)
    assert result["status"] == "FAILED" and retry.result_json == {}
    assert review.result_json == first_result and review.status == "PARTIAL"


def test_due_scheduler_and_pause_respects_local_time(db, analysis):
    watch = service.save_watch(db, analysis.id, AnalystWatchConfig(schedule_hour=18))
    factory = sessionmaker(bind=db.get_bind(), expire_on_commit=False)
    with patch.object(service, "SessionLocal", factory), patch("app.jobs.governance_runner.wake_governance_runner"):
        service.enqueue_due_reviews(datetime(2026, 11, 9, 17, 59, tzinfo=service.LOCAL_ZONE))
        assert db.scalar(select(func.count()).select_from(AiAnalystDailyReview)) == 0
        service.enqueue_due_reviews(datetime(2026, 11, 9, 18, 0, tzinfo=service.LOCAL_ZONE))
        service.enqueue_due_reviews(datetime(2026, 11, 9, 18, 1, tzinfo=service.LOCAL_ZONE))
        assert db.scalar(select(func.count()).select_from(AiAnalystDailyReview)) == 1
        service.save_watch(db, analysis.id, AnalystWatchConfig(enabled=False))
        service.enqueue_due_reviews(datetime(2026, 11, 10, 19, 0, tzinfo=service.LOCAL_ZONE))
        assert db.scalar(select(func.count()).select_from(AiAnalystDailyReview)) == 1


def test_stale_market_is_partial_even_without_price_or_volume_conditions(db, analysis):
    watch = service.save_watch(db, analysis.id, AnalystWatchConfig())
    db.scalar(select(StockKline).where(StockKline.adjust == "")).trade_date = "2000-01-01"
    db.commit()
    with patch("app.jobs.governance_runner.wake_governance_runner"):
        review = service.enqueue_review(db, watch)
    collection = [{"category": c, "status": "SUCCESS", "count": 1} for c in ("QUOTE", "KLINE", "NEWS", "NOTICE")]
    with patch.object(service, "refresh_monitor_data", return_value=collection), patch.object(service.ModelHubService, "chat", return_value=followup_log(db, daily_output(analysis))) as call:
        result = service.run_daily_review(db, review.id)
    assert result["status"] == "PARTIAL" and "日线" in review.error_message
    assert review.progress_json["quality_warnings"]
    assert "持股比例下降或持股市值下降不等于净资金流出" in call.call_args.kwargs["messages"][0]["content"]


def test_additive_migration_upgrades_only_builtin_contract_and_retains_revision(db):
    from app.models.ai_hub import ModelSkill, ModelSkillRevision
    from app.services.skill_registry import ensure_current_revision
    from scripts import migrate_analyst_followup as migration
    from app.services.stock_analyst_skill import STOCK_ANALYST_FOLLOWUP_SKILL
    skill = ModelSkill(**{**STOCK_ANALYST_FOLLOWUP_SKILL, "output_contract_json": {"type": "object"},
        "config_json": {"managed_contract_version": "ANALYST_FOLLOWUP_V1"},
        "idempotency_policy": "SNAPSHOT"}, is_builtin=True)
    db.add(skill)
    db.flush()
    ensure_current_revision(db, skill)
    old_id = db.scalar(select(ModelSkillRevision.id).where(ModelSkillRevision.skill_id == skill.id))
    db.commit()
    # Simulate the previously deployed dialogue schema in this isolated in-memory database.
    legacy = dialogue(db, queue_completed_analysis(db))
    legacy_id, legacy_question = legacy.id, legacy.question
    db.execute(text("ALTER TABLE ai_analyst_dialogue DROP COLUMN context_snapshot_json"))
    db.commit()
    factory = sessionmaker(bind=db.get_bind(), expire_on_commit=False)
    # File writes are excluded from the isolated migration test; contracts/revisions use the real registry.
    with patch.object(migration, "engine", db.get_bind()), patch.object(migration, "SessionLocal", factory), \
         patch.object(migration, "seed_table_comments"), patch.object(migration, "sync_skill_from_file", return_value=False), \
         patch("app.services.skill_registry.write_skill_file", return_value=("test-skill.md", "test-hash")):
        migration.migrate()
        migration.migrate()
    db.expire_all()
    assert "context_snapshot_json" in {c["name"] for c in inspect(db.get_bind()).get_columns("ai_analyst_dialogue")}
    assert db.get(service.AiAnalystDialogue, legacy_id).question == legacy_question
    assert db.get(service.AiAnalystDialogue, legacy_id).context_snapshot_json == {}
    assert db.get(ModelSkillRevision, old_id).governance_json["output_contract_json"] == {"type": "object"}
    assert db.get(ModelSkill, skill.id).config_json["managed_contract_version"] == "ANALYST_FOLLOWUP_V10_INCREMENTAL_FULL_LOCAL_REVIEW"
    assert db.get(ModelSkill, skill.id).version == "1.7.1"
    assert db.get(ModelSkill, skill.id).description == STOCK_ANALYST_FOLLOWUP_SKILL["description"]
    assert db.get(ModelSkill, skill.id).instructions == STOCK_ANALYST_FOLLOWUP_SKILL["instructions"]
    assert db.get(ModelSkill, skill.id).idempotency_policy == "REQUIRED"
    assert db.scalar(select(func.count()).select_from(ModelSkillRevision).where(ModelSkillRevision.skill_id == skill.id)) == 2


def queue_completed_analysis(db):
    row = queue_stock_analysis(db, payload())
    _, snapshot, coverage = build_stock_analyst_context(
        db, market=row.market, symbol=row.symbol, period_months=row.period_months, include_all=True,
    )
    row.evidence_snapshot_json = snapshot
    row.coverage_json = coverage
    row.status = "COMPLETED"
    row.result_json = output(snapshot["evidence_allowlist"][0])
    db.commit()
    return row


def test_prompt_budget_preserves_json_and_actual_reference_whitelist(analysis):
    context = {"evidence": [{"evidence_id": str(i), "excerpt": "数据" * 12000} for i in range(6)]}
    data = {**context, "question": "用户问题" * 1000}
    prompt = service._prompt(AnalystDialogueOutput, "回答问题", data, context)
    assert len(prompt) <= 55000
    sent = json.loads(prompt.split("\n资料:", 1)[1])
    assert sent["evidence_allowlist"] == context["evidence_allowlist"]
    assert [r["evidence_id"] for r in sent["evidence"]] == context["evidence_allowlist"]


def test_source_selection_uses_enabled_market_interfaces_and_fallback(db, analysis):
    source = db.scalar(select(DataSource))
    source.adapter_type = "PRIMARY_TEST"
    source.priority = 1
    other = DataSource(source_code="SECONDARY_TEST", source_name="备用", adapter_type="SECONDARY_TEST", priority=2)
    db.add(other)
    db.flush()
    for s in (source, other):
        db.add(DataInterface(source_id=s.id, interface_code=f"{s.id}_news", interface_name="新闻", data_category="NEWS", adapter_method="fetch_news", supported_markets=["HK"]))
    db.commit()
    from types import SimpleNamespace
    def fetch(s, market, symbol, persist):
        assert market == "HK" and symbol == "02533" and persist
        if s.id == source.id:
            raise ValueError("主源断连")
        return SimpleNamespace(id=123, status="SUCCESS"), []
    with patch("app.services.stock_on_demand.StockOnDemandService.fetch_news", side_effect=fetch):
        rows = service.refresh_monitor_data(db, analysis)
    assert next(r for r in rows if r["category"] == "NEWS")["source"] == "SECONDARY_TEST"
    assert next(r for r in rows if r["category"] == "KLINE")["status"] == "MISSING"


def test_followup_api_pagination_confirmation_validation_and_scope(db, analysis):
    app.dependency_overrides[get_db] = lambda: db
    client = TestClient(app)
    root = f"/api/v1/ai-stock-analyst/analyses/{analysis.id}"
    try:
        assert client.get(root+"/followup").json()["revisions"] == []
        assert client.post(root+"/watch/check").status_code == 409
        assert client.put(root+"/watch", json={"price_lower": 20, "price_upper": 10}).status_code == 422
        assert client.put(root+"/watch", json={"enabled": False}).status_code == 200
        with patch("app.services.analyst_followup.run_dialogue_background") as worker:
            response = client.post(root+"/dialogues", json={"question": "请分析回购影响"})
        assert response.status_code == 202 and worker.call_count == 1
        assert client.get(root+"/timeline?limit=1").json()["total"] == 0
        assert client.get(root+"/timeline/999999/evidence").status_code == 404
        assert client.get("/api/v1/ai-stock-analyst/analyses/999999/followup").status_code == 404
    finally:
        app.dependency_overrides.clear()


def suggestion_output(analysis, market_id):
    selected = service.tracking.observations(analysis)
    return {"summary": "根据近期行情和主营经营逻辑设置可调整的观察条件。",
        "conditions": {"price_lower": 480, "price_upper": 520, "volume_mode": "ANY", "volume_ratio": 1.5,
            "volume_window": 20, "event_conditions": [], "observation_rules": [{"observation": item["text"],
                "event_conditions": [{"category": "ANY", "label": "订单与回购", "keywords": ["订单", "回购"]}]} for item in selected],
            "review_focus": "结合订单回款与主营盈利变化，持续验证原报告判断。"},
        "price_reason": "以近期不复权收盘价为基准设置上下观察区间，不作为收益承诺。",
        "volume_reason": "已有完整量能样本不足，因此暂不限定放量或缩量。",
        "event_reason": "跟踪与主营订单及股东回报有关的公司实际事件。", "limitations": ["仅为观察阈值，可由用户调整"],
        "evidence": [{"evidence_id": market_id, "claim": "近期行情支持观察区间"}]}


def test_ai_suggestion_is_audited_draft_and_never_enables_or_replaces_watch(db, analysis):
    original = deepcopy(analysis.result_json)
    watch = service.save_watch(db, analysis.id, AnalystWatchConfig(enabled=False, price_lower=450))
    before = deepcopy(watch.config_json)
    current = db.scalar(select(StockKline).where(StockKline.adjust == ""))
    local_now = datetime.now(service.LOCAL_ZONE)
    current.trade_date = (local_now.date() if local_now.hour >= 16 else local_now.date() - timedelta(days=1)).isoformat()
    db.commit()
    row = service.queue_watch_suggestion(db, analysis.id, AnalystSuggestionCreate(model_instance_code="REAL_TEST"))
    assert service.queue_watch_suggestion(db, analysis.id, AnalystSuggestionCreate()).id == row.id
    result = suggestion_output(analysis, service._suggestion_market(db, analysis)["evidence_id"])
    with patch.object(service.ModelHubService, "chat", return_value=followup_log(db, result)) as call:
        saved = service.execute_watch_suggestion(db, row.id)
    assert saved.status == "COMPLETED" and saved.model_call_log_id and saved.skill_version == "2.3.4"
    assert saved.evidence_snapshot_json["evidence"] and saved.result_json["conditions"]["price_lower"] == 480
    assert watch.config_json == before and not watch.enabled and watch.config_version == 1
    assert analysis.result_json == original and service.latest_revision(db, analysis.id) is None
    assert call.call_args.kwargs["metadata_json"]["strict_instance"] is True
    sent = json.loads(call.call_args.kwargs["messages"][0]["content"].split("\n资料:", 1)[1])
    assert sent["current_conditions"]["price_lower"] == 450
    assert not {"enabled", "schedule_hour", "schedule_minute", "model_instance_code"}.intersection(sent["current_conditions"])


def test_observation_rules_bind_multiple_event_dimensions_to_each_focus(db, analysis):
    today = datetime.now(service.LOCAL_ZONE).date().isoformat()
    db.add_all([
        StockNotice(market="HK", symbol="02533", notice_date=today, title="首批订单完成交付", content_json={}, source_id=db.scalar(select(DataSource.id))),
        StockNews(market="HK", symbol="02533", news_time=today, title="客户回款进度改善", content="经营现金流有望改善", source_id=db.scalar(select(DataSource.id))),
    ])
    db.commit()
    cfg = AnalystWatchConfig(selected_observations=["订单兑现", "现金流改善"], observation_rules=[
        {"observation": "订单兑现", "event_conditions": [
            {"category": "NOTICE", "label": "订单交付", "keywords": ["完成交付"]},
            {"category": "ANY", "label": "客户验收", "keywords": ["客户验收"]}]},
        {"observation": "现金流改善", "event_conditions": [
            {"category": "NEWS", "label": "客户回款", "keywords": ["回款进度"]}]},
    ]).model_dump()
    conditions, _ = service.evaluate_conditions(db, analysis, cfg, today, today)
    assert [(row["observation"], row["dimension"], row["status"]) for row in conditions] == [
        ("订单兑现", "订单交付", "HIT"), ("订单兑现", "客户验收", "NOT_HIT"), ("现金流改善", "客户回款", "HIT")]
    assert all(row["kind"] == "event" for row in conditions)


@pytest.mark.parametrize("fault", ["stale", "missing", "no_market_reference", "volume_gap", "provider"])
def test_suggestion_rejects_unsupported_prices_or_volume_and_retains_watch(db, analysis, fault):
    watch = service.save_watch(db, analysis.id, AnalystWatchConfig(enabled=False, price_lower=400))
    current = db.scalar(select(StockKline).where(StockKline.adjust == ""))
    market_id = f"watch-market:{current.id}"
    result = suggestion_output(analysis, market_id)
    if fault == "stale": current.trade_date = "2000-01-01"
    if fault == "missing": db.delete(current)
    if fault == "no_market_reference": result["evidence"] = model_output(analysis)["evidence"]
    if fault == "volume_gap":
        today = datetime.now(timezone.utc).date()
        for i in range(1, 9):
            db.add(StockKline(market="HK", symbol="02533", trade_date=(today-timedelta(days=i)).isoformat(),
                             adjust="", close_price=500, volume=None if i == 2 else 100, source_id=current.source_id))
        result["conditions"].update(volume_mode="EXPAND", volume_window=5)
    db.commit()
    row = service.queue_watch_suggestion(db, analysis.id, AnalystSuggestionCreate())
    log = followup_log(db, result, status="FAILED" if fault == "provider" else "SUCCESS")
    with patch.object(service.ModelHubService, "chat", return_value=log):
        saved = service.execute_watch_suggestion(db, row.id)
    assert saved.status == "FAILED" and saved.result_json == {} and saved.error_message
    assert watch.config_json["price_lower"] == 400 and not watch.enabled


def test_suggestion_without_prices_is_valid_when_market_is_missing(db, analysis):
    db.delete(db.scalar(select(StockKline).where(StockKline.adjust == "")))
    db.commit()
    result = suggestion_output(analysis, analysis.evidence_snapshot_json["evidence_allowlist"][0])
    result["conditions"].update(price_lower=None, price_upper=None)
    row = service.queue_watch_suggestion(db, analysis.id, AnalystSuggestionCreate())
    with patch.object(service.ModelHubService, "chat", return_value=followup_log(db, result)):
        assert service.execute_watch_suggestion(db, row.id).status == "COMPLETED"
    assert db.scalar(select(func.count()).select_from(AiAnalystWatch)) == 0
    with pytest.raises(ValidationError):
        AnalystWatchSuggestionOutput.model_validate({**result, "conditions": {**result["conditions"], "enabled": True}})


def test_dialogue_uses_selected_evidence_and_latest_local_data_with_auditable_snapshot(db, analysis):
    chosen = analysis.evidence_snapshot_json["evidence_allowlist"][0]
    news = StockNews(market="HK", symbol="02533", news_time=datetime.now(timezone.utc).isoformat(),
        title="分析之后的新订单进展", content="新增签约已经落地，应重新评估收入持续性。", source_id=db.scalar(select(DataSource.id)))
    db.add(news); db.commit()
    row = service.queue_dialogue(db, analysis.id, AnalystDialogueCreate(question="新订单如何影响报告？", evidence_ids=[chosen]))
    with patch.object(service.ModelHubService, "chat", return_value=followup_log(db, model_output(analysis))) as call:
        saved = service.execute_dialogue(db, row.id)
    assert saved.status == "COMPLETED"
    assert chosen in saved.context_snapshot_json["evidence_allowlist"]
    assert f"news:{news.id}" in saved.context_snapshot_json["evidence_allowlist"]
    assert "分析之后的新订单进展" in call.call_args.kwargs["messages"][0]["content"]
    with pytest.raises(ValueError, match="不属于当前股票"):
        service.queue_dialogue(db, analysis.id, AnalystDialogueCreate(question="不属于报告的资料", evidence_ids=["news:999999"]))


def test_suggestion_api_async_idempotency_and_evidence_scope(db, analysis):
    app.dependency_overrides[get_db] = lambda: db
    client = TestClient(app)
    root = f"/api/v1/ai-stock-analyst/analyses/{analysis.id}"
    try:
        with patch.object(service, "run_suggestion_background") as worker:
            response = client.post(root+"/watch/suggestions", json={"model_instance_code": "REAL_TEST"})
            again = client.post(root+"/watch/suggestions", json={})
        assert response.status_code == 202 and again.json()["id"] == response.json()["id"]
        assert worker.call_count == 2 and "evidence_snapshot_json" not in response.json()
        suggestion_id = response.json()["id"]
        assert client.get(root+"/followup").json()["watch_suggestion"]["id"] == suggestion_id
        other = queue_stock_analysis(db, payload())
        assert client.get(f"/api/v1/ai-stock-analyst/analyses/{other.id}/watch/suggestions/{suggestion_id}/evidence").status_code == 404
        assert client.get(root+"/dialogues/99999/evidence").status_code == 404
    finally:
        app.dependency_overrides.clear()

