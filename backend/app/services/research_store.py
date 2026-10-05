from __future__ import annotations

from datetime import datetime, timezone
from hashlib import sha1, sha256
from io import BytesIO
import re
from typing import Any

import httpx
from pypdf import PdfReader
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.connectors.sina_research import fetch_sina_report_detail
from app.models.research_data import (
    StockBrokerResearchReport,
    StockEarningsConsensus,
    StockInstitutionForecast,
    StockInvestorQA,
)


def _text(value: Any) -> str:
    return str(value or "").strip()


def _list(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


def _source_code(row: dict[str, Any], fallback: str) -> str:
    name = _text(row.get("source_code") or row.get("source_name")).upper()
    if "SINA" in name or "新浪" in name:
        return "SINA_FINANCE"
    if "THS" in name or "同花顺" in name:
        return "TONGHUASHUN"
    if "CNINFO" in name or "巨潮" in name:
        return "CNINFO"
    if "EAST" in name or "东方财富" in name or "东财" in name:
        return "EASTMONEY"
    return fallback


def _nonempty_update(target: Any, values: dict[str, Any]) -> None:
    for key, value in values.items():
        if value not in (None, "", [], {}):
            setattr(target, key, value)


def _report_external_id(row: dict[str, Any]) -> str:
    value = _text(row.get("external_id") or row.get("report_id"))
    if value:
        return value
    return sha1("|".join((
        _text(row.get("report_date") or row.get("日期")),
        _text(row.get("institution") or row.get("机构")),
        _text(row.get("title") or row.get("报告名称")),
    )).encode("utf-8")).hexdigest()[:24]


def _forecast_values(row: dict[str, Any]) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {"eps": {}, "net_profit": {}}
    for key, value in row.items():
        text = str(key)
        year_match = re.search(r"(20\d{2})", text)
        if not year_match or value in (None, ""):
            continue
        year = year_match.group(1)
        if re.search(r"每股收益|EPS|盈利预测.*收益", text, re.I):
            result["eps"][year] = value
        elif re.search(r"净利润|归母净利", text):
            result["net_profit"][year] = value
    return {key: values for key, values in result.items() if values}


def persist_research_sections(
    db: Session,
    market: str,
    symbol: str,
    research: dict[str, Any] | None,
) -> None:
    """Incrementally dual-write research aggregates without deleting legacy cache."""
    if not isinstance(research, dict):
        return
    now = datetime.now(timezone.utc)
    report_rows = [
        row for row in (*_list(research.get("reports")), *_list(research.get("latest_reports")))
        if isinstance(row, dict)
    ]
    seen_reports: set[tuple[str, str]] = set()
    for row in report_rows:
        source_code = _source_code(row, "EASTMONEY")
        external_id = _report_external_id(row)
        key = (source_code, external_id)
        if key in seen_reports:
            continue
        seen_reports.add(key)
        title = _text(row.get("title") or row.get("报告名称"))
        if not title:
            continue
        record = db.scalar(select(StockBrokerResearchReport).where(
            StockBrokerResearchReport.market == market,
            StockBrokerResearchReport.symbol == symbol,
            StockBrokerResearchReport.source_code == source_code,
            StockBrokerResearchReport.external_id == external_id,
        ))
        if record is None:
            record = StockBrokerResearchReport(
                market=market, symbol=symbol, source_code=source_code,
                external_id=external_id, title=title,
            )
            db.add(record)
        content = _text(row.get("content") or row.get("content_text"))
        analysts = row.get("analysts") if isinstance(row.get("analysts"), list) else [
            item for item in re.split(r"[/、,，]", _text(row.get("研究员"))) if item
        ]
        _nonempty_update(record, {
            "title": title,
            "report_date": _text(row.get("report_date") or row.get("报告日期") or row.get("日期"))[:10],
            "institution": _text(row.get("institution") or row.get("机构")) or None,
            "analysts_json": analysts,
            "rating": _text(row.get("rating") or row.get("评级") or row.get("东财评级")) or None,
            "summary": _text(row.get("summary")) or (content[:500] if content else None),
            "content_text": content or None,
            "content_hash": _text(row.get("content_hash")) or (sha256(content.encode("utf-8")).hexdigest() if content else None),
            "content_status": "READY" if content else _text(row.get("content_status")) or "PENDING",
            "fetch_error": _text(row.get("fetch_error")) or None,
            "source_url": _text(row.get("source_url")) or None,
            "detail_url": _text(row.get("detail_url") or row.get("url")) or None,
            "pdf_url": _text(row.get("pdf_url") or row.get("报告PDF链接")) or None,
            "source_updated_at": _text(row.get("source_updated_at") or row.get("report_date") or row.get("日期")) or None,
            "raw_payload": row,
        })
        record.fetched_at = now

    earnings = research.get("earnings_forecast")
    earnings_rows = earnings.get("rows") if isinstance(earnings, dict) else earnings
    for row in _list(earnings_rows):
        if not isinstance(row, dict):
            continue
        year = _text(row.get("forecast_year") or row.get("预测年度"))[:4]
        metric_name = _text(row.get("metric") or row.get("预测指标"))
        if not re.fullmatch(r"20\d{2}", year) or not metric_name:
            continue
        metric_code = "EPS" if "每股收益" in metric_name else "NET_PROFIT" if "净利润" in metric_name else metric_name.upper()
        source_code = _source_code(row, "TONGHUASHUN")
        record = db.scalar(select(StockEarningsConsensus).where(
            StockEarningsConsensus.market == market, StockEarningsConsensus.symbol == symbol,
            StockEarningsConsensus.source_code == source_code,
            StockEarningsConsensus.forecast_year == year,
            StockEarningsConsensus.metric_code == metric_code,
        ))
        if record is None:
            record = StockEarningsConsensus(
                market=market, symbol=symbol, source_code=source_code,
                forecast_year=year, metric_code=metric_code, metric_name=metric_name,
            )
            db.add(record)
        count = row.get("prediction_count") or row.get("预测机构数")
        _nonempty_update(record, {
            "metric_name": metric_name,
            "unit": "元/股" if metric_code == "EPS" else "亿元" if metric_code == "NET_PROFIT" else None,
            "prediction_count": int(float(count)) if count not in (None, "") else None,
            "minimum_value": _text(row.get("minimum") or row.get("最小值")) or None,
            "mean_value": _text(row.get("mean") or row.get("均值")) or None,
            "maximum_value": _text(row.get("maximum") or row.get("最大值")) or None,
            "industry_average": _text(row.get("industry_average") or row.get("行业平均数")) or None,
            "source_url": _text(row.get("source_url") or row.get("detail_url")) or None,
            "source_updated_at": _text(row.get("source_updated_at")) or now.isoformat(),
            "raw_payload": row,
        })
        record.fetched_at = now

    institutions = research.get("institution_forecast")
    institution_rows = institutions.get("rows") if isinstance(institutions, dict) else institutions
    for row in _list(institution_rows):
        if not isinstance(row, dict):
            continue
        institution = _text(row.get("institution") or row.get("机构") or row.get("机构名称"))
        report_date = _text(row.get("report_date") or row.get("报告日期"))[:10]
        forecast = _forecast_values(row)
        if not institution or not forecast:
            continue
        source_code = _source_code(row, "TONGHUASHUN")
        external_key = sha1("|".join((institution, report_date, _text(row.get("研究员")))).encode("utf-8")).hexdigest()[:32]
        record = db.scalar(select(StockInstitutionForecast).where(
            StockInstitutionForecast.market == market, StockInstitutionForecast.symbol == symbol,
            StockInstitutionForecast.source_code == source_code,
            StockInstitutionForecast.external_key == external_key,
        ))
        if record is None:
            record = StockInstitutionForecast(
                market=market, symbol=symbol, source_code=source_code,
                external_key=external_key, institution=institution,
            )
            db.add(record)
        analyst_text = _text(row.get("研究员") or row.get("分析师"))
        _nonempty_update(record, {
            "institution": institution,
            "analysts_json": row.get("analysts") if isinstance(row.get("analysts"), list) else [item for item in re.split(r"[/、,，]", analyst_text) if item],
            "report_date": report_date,
            "rating": _text(row.get("rating") or row.get("评级") or row.get("东财评级")) or None,
            "forecast_json": forecast,
            "report_external_id": _text(row.get("external_id") or row.get("report_id")) or None,
            "source_url": _text(row.get("source_url")) or None,
            "detail_url": _text(row.get("detail_url")) or None,
            "source_updated_at": _text(row.get("source_updated_at") or report_date) or None,
            "raw_payload": row,
        })
        record.fetched_at = now

    qa = research.get("qa")
    qa_rows = qa.get("rows") if isinstance(qa, dict) else qa
    for row in _list(qa_rows):
        if not isinstance(row, dict):
            continue
        question = _text(row.get("question") or row.get("问题"))
        external_id = _text(row.get("question_id") or row.get("external_id"))
        if not question or not external_id:
            continue
        source_code = _source_code(row, "CNINFO")
        record = db.scalar(select(StockInvestorQA).where(
            StockInvestorQA.market == market, StockInvestorQA.symbol == symbol,
            StockInvestorQA.source_code == source_code, StockInvestorQA.external_id == external_id,
        ))
        if record is None:
            record = StockInvestorQA(
                market=market, symbol=symbol, source_code=source_code,
                external_id=external_id, question=question,
            )
            db.add(record)
        incoming_updated = _text(row.get("updated_at") or row.get("answered_at") or row.get("asked_at"))
        if record.updated_time and incoming_updated and incoming_updated < record.updated_time:
            continue
        _nonempty_update(record, {
            "question": question,
            "answer": _text(row.get("answer") or row.get("回答内容")) or None,
            "questioner": _text(row.get("questioner") or row.get("提问者")) or None,
            "answerer": _text(row.get("answerer") or row.get("回答者")) or None,
            "asked_time": _text(row.get("asked_at") or row.get("提问时间")) or None,
            "answered_time": _text(row.get("answered_at") or row.get("回答时间")) or None,
            "updated_time": incoming_updated or None,
            "source_url": _text(row.get("source_url") or row.get("detail_url")) or None,
            "source_updated_at": incoming_updated or None,
            "raw_payload": row,
        })
        record.fetched_at = now
    db.flush()


def _report_dict(row: StockBrokerResearchReport) -> dict[str, Any]:
    return {
        **(row.raw_payload or {}),
        "source_code": row.source_code,
        "external_id": row.external_id,
        "report_id": f"{row.source_code}-{row.external_id}",
        "title": row.title,
        "报告名称": row.title,
        "report_date": row.report_date,
        "日期": row.report_date,
        "institution": row.institution,
        "机构": row.institution,
        "analysts": row.analysts_json,
        "rating": row.rating,
        "东财评级": row.rating,
        "summary": row.summary,
        "content": row.content_text,
        "content_status": row.content_status,
        "fetch_error": row.fetch_error,
        "source_url": row.source_url,
        "detail_url": row.detail_url,
        "pdf_url": row.pdf_url,
        "source_updated_at": row.source_updated_at,
    }


def load_research_sections(db: Session, market: str, symbol: str) -> dict[str, Any]:
    reports = list(db.scalars(select(StockBrokerResearchReport).where(
        StockBrokerResearchReport.market == market, StockBrokerResearchReport.symbol == symbol,
    ).order_by(StockBrokerResearchReport.report_date.desc(), StockBrokerResearchReport.id.desc())).all())
    consensus = list(db.scalars(select(StockEarningsConsensus).where(
        StockEarningsConsensus.market == market, StockEarningsConsensus.symbol == symbol,
    ).order_by(StockEarningsConsensus.forecast_year, StockEarningsConsensus.metric_code)).all())
    institutions = list(db.scalars(select(StockInstitutionForecast).where(
        StockInstitutionForecast.market == market, StockInstitutionForecast.symbol == symbol,
    ).order_by(StockInstitutionForecast.report_date.desc())).all())
    qa_rows = list(db.scalars(select(StockInvestorQA).where(
        StockInvestorQA.market == market, StockInvestorQA.symbol == symbol,
    ).order_by(StockInvestorQA.updated_time.desc(), StockInvestorQA.id.desc())).all())
    return {
        "reports": [_report_dict(row) for row in reports],
        "earnings_forecast": [{
            **(row.raw_payload or {}), "forecast_year": row.forecast_year,
            "预测年度": row.forecast_year, "metric": row.metric_name,
            "预测指标": row.metric_name, "prediction_count": row.prediction_count,
            "预测机构数": row.prediction_count, "minimum": row.minimum_value,
            "最小值": row.minimum_value, "mean": row.mean_value, "均值": row.mean_value,
            "maximum": row.maximum_value, "最大值": row.maximum_value,
            "industry_average": row.industry_average, "行业平均数": row.industry_average,
            "source_code": row.source_code, "source_url": row.source_url,
        } for row in consensus],
        "institution_forecast": [{
            **(row.raw_payload or {}), "institution": row.institution, "机构": row.institution,
            "analysts": row.analysts_json, "report_date": row.report_date,
            "报告日期": row.report_date, "rating": row.rating,
            "forecast": row.forecast_json, "source_code": row.source_code,
            "source_url": row.source_url, "detail_url": row.detail_url,
        } for row in institutions],
        "qa": [{
            **(row.raw_payload or {}), "question_id": row.external_id,
            "question": row.question, "问题": row.question, "answer": row.answer,
            "回答内容": row.answer, "questioner": row.questioner, "answerer": row.answerer,
            "asked_at": row.asked_time, "answered_at": row.answered_time,
            "updated_at": row.updated_time, "source_code": row.source_code,
            "source_url": row.source_url,
        } for row in qa_rows],
    }


def _fetch_pdf_text(url: str) -> str:
    response = httpx.get(url, timeout=20.0, follow_redirects=True, headers={"User-Agent": "Mozilla/5.0"})
    response.raise_for_status()
    reader = PdfReader(BytesIO(response.content))
    return "\n\n".join((page.extract_text() or "").strip() for page in reader.pages if (page.extract_text() or "").strip())


def get_or_fetch_report_detail(
    db: Session, market: str, symbol: str, source_code: str, external_id: str
) -> dict[str, Any] | None:
    row = db.scalar(select(StockBrokerResearchReport).where(
        StockBrokerResearchReport.market == market, StockBrokerResearchReport.symbol == symbol,
        StockBrokerResearchReport.source_code == source_code.upper(),
        StockBrokerResearchReport.external_id == external_id,
    ))
    if row is None:
        return None
    if row.content_text:
        return _report_dict(row)
    try:
        if row.source_code == "SINA_FINANCE":
            detail = fetch_sina_report_detail(row.external_id, detail_url=row.detail_url)
            content = _text(detail.get("content"))
            _nonempty_update(row, {
                "title": detail.get("title"), "report_date": detail.get("report_date"),
                "institution": detail.get("institution"), "analysts_json": detail.get("analysts"),
                "rating": detail.get("rating"), "summary": detail.get("summary"),
                "content_text": content, "content_hash": detail.get("content_hash"),
                "content_status": "READY", "fetch_error": None,
                "detail_url": detail.get("detail_url"), "source_updated_at": detail.get("source_updated_at"),
            })
        else:
            url = row.pdf_url or row.detail_url
            if not url or ".pdf" not in url.lower():
                raise ValueError("该来源未提供可直接解析的公开研报正文或 PDF")
            content = _fetch_pdf_text(url)
            if not content:
                raise ValueError("公开 PDF 未提取到文本正文")
            row.content_text = content
            row.summary = row.summary or content[:500]
            row.content_hash = sha256(content.encode("utf-8")).hexdigest()
            row.content_status = "READY"
            row.fetch_error = None
    except Exception as exc:
        row.content_status = "FAILED"
        row.fetch_error = str(exc)[:1000]
    row.fetched_at = datetime.now(timezone.utc)
    db.commit()
    return _report_dict(row)
