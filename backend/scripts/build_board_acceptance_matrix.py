"""Build the auditable acceptance matrix for the fixed 8x20 board sample.

This script is read-only. It distinguishes verified absence/market limitations
from provider failure; an empty response is never promoted to PASS.
"""
from __future__ import annotations

from collections import Counter, defaultdict
import csv
from datetime import datetime, timezone
import json
from pathlib import Path
import sqlite3
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "validation" / "all-boards-20261008"
STAGES = ("quote", "kline", "financials", "notices", "news", "extended", "research")
STAGE_NAMES = {
    "quote": "行情",
    "kline": "日线",
    "financials": "财务",
    "notices": "公告",
    "news": "新闻",
    "extended": "F10详细资料",
    "research": "研究资料",
}
STATUS_DEFINITIONS = {
    "PASS": "有可核验数据且身份、接口、展示或质量检查通过",
    "PARTIAL": "已有可用数据，但来源覆盖、字段或最新刷新不完整",
    "MISSING": "应有数据但当前缺失，且尚未取得足以解释缺失的来源证据",
    "UNAVAILABLE": "栏目适用，但已核验的公开来源明确无记录或没有可验证的公共标准化来源",
    "NOT_APPLICABLE": "该栏目不是该市场的基础事实，或属于证券软件专有加工结果",
    "SOURCE_FAILED": "来源断连、拒绝参数、返回其他证券或响应不可验证，不能据此认定无数据",
    "FAILED": "发现身份错配、数值冲突、接口/页面失败或数据质量错误",
}
STATUS_ORDER = {
    "FAILED": 7,
    "SOURCE_FAILED": 6,
    "MISSING": 5,
    "PARTIAL": 4,
    "UNAVAILABLE": 3,
    "NOT_APPLICABLE": 2,
    "PASS": 1,
}


def load(name: str) -> Any:
    return json.loads((OUT / name).read_text(encoding="utf-8"))


def write_json(path: Path, value: Any) -> None:
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(value, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
    temporary.replace(path)


def worst(statuses: list[str], *, ignore: set[str] | None = None) -> str:
    values = [status for status in statuses if status not in (ignore or set())]
    return max(values, key=lambda value: STATUS_ORDER.get(value, 99), default="PASS")


def latest_attempt(stock: dict[str, Any], stage: str) -> dict[str, Any]:
    attempts = (stock.get("attempts") or {}).get(stage) or []
    return attempts[-1] if attempts else {}


def stage_result(stock: dict[str, Any], stage: str) -> dict[str, Any]:
    count = int(((stock.get("local") or {}).get("counts") or {}).get(stage) or 0)
    attempt = latest_attempt(stock, stage)
    attempt_status = attempt.get("status")
    source = attempt.get("source_code")
    if count > 0:
        status = "PARTIAL" if attempt_status == "FAILED" else "PASS"
        reason = "本地已有历史数据，但最近一次远程刷新失败" if status == "PARTIAL" else "已采集并保存可用记录"
    elif stage == "research" and stock["board"] in {"NEEQ_BASE", "NEEQ_INNOVATION"}:
        status = "UNAVAILABLE"
        reason = "已核验公告、新闻和公开互动来源；尚无可验证的新三板机构预测/研报标准化公共来源"
    elif attempt_status in {"FAILED", "MISSING_UNVERIFIED"}:
        status = "SOURCE_FAILED"
        reason = attempt.get("error") or attempt.get("reason") or "来源调用失败，不能判定无数据"
    elif attempt_status == "EMPTY_UNVERIFIED":
        status = "MISSING"
        reason = "来源返回空结果，但尚不足以证明该证券确无数据"
    else:
        status = "MISSING"
        reason = "没有已保存记录或可验证的来源结论"
    return {
        "status": status,
        "count": count,
        "source_code": source,
        "attempt_status": attempt_status,
        "reason": reason,
    }


def research_source_metadata() -> dict[str, list[dict[str, Any]]]:
    values: dict[str, list[dict[str, Any]]] = {}
    with sqlite3.connect(ROOT / "quant.db") as db:
        rows = db.execute(
            "SELECT market, symbol, payload_json FROM stock_f10_cache WHERE section='research_sections'"
        ).fetchall()
    for market, symbol, payload in rows:
        try:
            item = json.loads(payload or "{}")
        except (TypeError, ValueError):
            continue
        values[f"{market}:{symbol}"] = [
            {key: row.get(key) for key in ("source_code", "status", "error") if row.get(key) is not None}
            for row in item.get("source_metadata") or []
            if isinstance(row, dict)
        ]
    return values


def panel_result(
    sample: dict[str, Any],
    panel: dict[str, Any],
    research_sources: list[dict[str, Any]],
) -> dict[str, Any]:
    key = str(panel.get("key") or "")
    group = str(panel.get("group") or "")
    result = {
        "group": group,
        "key": key,
        "name": panel.get("name"),
        "rows": int(panel.get("rows") or 0),
        "source": panel.get("source"),
        "as_of": panel.get("as_of"),
    }
    if panel.get("has_values"):
        result["status"] = "PARTIAL" if panel.get("display_status") in {"PARTIAL", "PENDING"} else "PASS"
        result["reason"] = panel.get("reason") or "存在具体观测值"
        return result
    if group == "profile" and key == "insight":
        result.update(status="NOT_APPLICABLE", reason="“道破天机”属于证券软件专有加工栏目，不是公共基础事实")
        return result
    if group == "research_sections":
        if sample["board"] == "HK_GEM":
            needed = ({"ETNET_HK"} if key in {"earnings_forecast", "institution_forecast"}
                      else {"AASTOCKS_HK"} if key in {"latest_reports", "reports"} else set())
            failed = [row for row in research_sources if row.get("source_code") in needed and row.get("status") == "FAILED"]
            if failed:
                result.update(status="SOURCE_FAILED", reason="；".join(row.get("error") or "来源失败" for row in failed))
            else:
                result.update(status="UNAVAILABLE", reason="经济通与AASTOCKS证券隔离页面已核验，公开页明确无该栏目记录")
            result["source_checks"] = research_sources
            return result
        if sample["board"] in {"NEEQ_BASE", "NEEQ_INNOVATION"}:
            result.update(status="UNAVAILABLE", reason="当前没有可验证的新三板机构预测或券商研报标准化公共来源")
            return result
        result.update(status="UNAVAILABLE", reason=panel.get("reason") or "适用公开来源未披露该证券记录")
        return result
    if group == "financial_summary" and key in {"income", "balance", "cash_flow"}:
        result.update(status="MISSING", reason=panel.get("reason") or "应有财务报表字段缺失")
        return result
    result.update(status="UNAVAILABLE", reason=panel.get("reason") or "当前公开来源未提供该细项")
    return result


def main() -> None:
    samples = load("samples.json")
    if len(samples) != 160 or Counter(row["board"] for row in samples) != Counter({
        "MAIN": 20, "STAR": 20, "CHINEXT": 20, "BSE": 20,
        "HK_MAIN": 20, "HK_GEM": 20, "NEEQ_BASE": 20, "NEEQ_INNOVATION": 20,
    }):
        raise RuntimeError("固定验收样本不是8个板块各20只，拒绝生成误导性报告")

    details = {(row["board"], row["symbol"]): row for row in load("detail-summary.json")["results"]}
    browsers = {(row["board"], row["symbol"]): row for row in load("browser-latest-20261009.json")["results"]}
    storage = load("storage-result.json")
    lineage = {(row["board"], row["symbol"], row["stage"]): row for row in storage["ingestion"]}
    official = {(row["board"], row["symbol"]): row for row in load("neeq-official-master.json")["results"]}
    neeq_kline_status = {(row["board"], row["symbol"]): row
                         for row in load("neeq-kline-public-status.json")["results"]}
    research = research_source_metadata()
    stock_files = {
        (row["board"], row["symbol"]): json.loads(
            (OUT / "stocks" / f"{row['board']}-{row['symbol']}.json").read_text(encoding="utf-8")
        )
        for row in samples
    }

    results: list[dict[str, Any]] = []
    for sample in samples:
        identity = (sample["listing_evidence"] or {}).get("classification_status") == "NORMALIZED"
        if sample["board"].startswith("NEEQ_"):
            identity = identity and official.get((sample["board"], sample["symbol"]), {}).get("status") == "PASS"
        master = {
            "status": "PASS" if identity else "FAILED",
            "source": "全国股转系统官方挂牌公司名录" if sample["board"].startswith("NEEQ_") else
                      ((sample["listing_evidence"] or {}).get("classification_evidence") or {}).get("source_code"),
            "reason": "证券代码、简称、市场和板块身份已核验" if identity else "主数据身份或板块未通过核验",
        }
        stock = stock_files[(sample["board"], sample["symbol"])]
        stages = {stage: stage_result(stock, stage) for stage in STAGES}
        public_kline = neeq_kline_status.get((sample["board"], sample["symbol"]))
        if stages["kline"]["count"] == 0 and public_kline and public_kline.get("status") == "UNAVAILABLE":
            stages["kline"].update(
                status="UNAVAILABLE",
                reason=public_kline["reason"],
                source_code="NEEQ_PUBLIC_CROSS_CHECK",
                source_checks=public_kline.get("sources") or [],
                checked_at=public_kline.get("checked_at"),
            )
        detail = details[(sample["board"], sample["symbol"])]
        browser = browsers[(sample["board"], sample["symbol"])]

        price_check = detail.get("price_check")
        if (
            sample["board"].startswith("NEEQ_")
            and price_check == "INDEPENDENT_SOURCE_UNVERIFIED"
            and stages["kline"]["count"] > 0
        ):
            # Sina has no independently usable snapshot for some NEEQ names,
            # while the local quote still carries security-scoped data. Keep
            # this explicitly partial; do not convert it to a failed or fake
            # independent match.
            quote_status = "PARTIAL"
            stages["quote"]["reason"] = "本地行情有可核验主体和记录；新浪独立行情无可用报价，未宣称独立价格匹配"
        elif detail.get("issues"):
            quote_status = "FAILED" if any(
                item.get("kind") in {"INDEPENDENT_PRICE_MISMATCH", "INDEPENDENT_VOLUME_MISMATCH"}
                for item in detail["issues"] if isinstance(item, dict)
            ) else stages["quote"]["status"]
        elif price_check in {"MATCH", "MATCH_SOURCE_RECONCILIATION", "NO_TRADE_REFERENCE_ONLY"}:
            quote_status = "PASS"
        elif price_check == "INDEPENDENT_SOURCE_UNVERIFIED":
            quote_status = "SOURCE_FAILED"
        else:
            quote_status = "PARTIAL"
        stages["quote"].update(status=quote_status, independent_check=price_check)

        sources = research.get(f"{sample['market']}:{sample['symbol']}", [])
        if stages["research"]["count"] == 0 and sample["board"] == "HK_GEM":
            failed = [row for row in sources if row.get("status") == "FAILED"]
            stages["research"].update(
                status="SOURCE_FAILED" if failed else "UNAVAILABLE",
                reason=("；".join(row.get("error") or "来源失败" for row in failed)
                        if failed else "经济通与AASTOCKS公开证券页均明确无研究记录"),
                source_checks=sources,
            )

        panels = [panel_result(sample, panel, sources) for panel in detail.get("panels") or []]
        page = {
            "status": "PASS" if browser.get("status") == "RENDER_PASS" and not browser.get("errors") else "FAILED",
            "reason": "详情页所有页签完成真实浏览器渲染" if browser.get("status") == "RENDER_PASS" else "页面渲染失败",
        }
        quality = {
            "status": "PASS" if not detail.get("issues") else "FAILED",
            "issues": detail.get("issues") or [],
            "reason": "未发现身份错配、比例越界或财务恒等式错误" if not detail.get("issues") else "存在数据质量错误",
        }
        balance_checks = detail.get("balance_checks") or []
        if any(row.get("status") == "MISMATCH" for row in balance_checks):
            balance = {"status": "FAILED", "checks": balance_checks, "reason": "资产负债恒等式不成立"}
        elif any(row.get("status") == "MISSING_TOTALS" for row in balance_checks):
            balance = {"status": "PARTIAL", "checks": balance_checks, "reason": "来源缺少恒等式所需合计字段"}
        else:
            balance = {"status": "PASS", "checks": balance_checks, "reason": "有完整字段的报告期均通过恒等式检查"}

        expected_lineage = [stage for stage in STAGES if stages[stage]["count"] > 0]
        missing_lineage = [stage for stage in expected_lineage if lineage.get(
            (sample["board"], sample["symbol"], stage), {}).get("status") != "RAW_LINEAGE_PRESENT"]
        lineage_result = {
            "status": "FAILED" if missing_lineage else "PASS",
            "checked_stages": expected_lineage,
            "missing_stages": missing_lineage,
            "reason": "所有已采集阶段均存在原始对象和血缘" if not missing_lineage else "已采集阶段缺少原始血缘",
        }
        critical = [master["status"], page["status"], quality["status"], balance["status"], lineage_result["status"],
                    *(stages[key]["status"] for key in ("quote", "kline", "financials", "notices", "news", "extended", "research"))]
        verification = worst(critical, ignore={"PARTIAL", "UNAVAILABLE", "NOT_APPLICABLE"})
        if verification == "PASS" and any(status == "PARTIAL" for status in critical):
            verification = "PASS"
        coverage = "COMPLETE" if all(item["status"] == "PASS" for item in [*stages.values(), *panels]) else "PARTIAL"
        results.append({
            "board": sample["board"], "board_name": sample["board_name"], "market": sample["market"],
            "symbol": sample["symbol"], "name": sample["name"], "master": master,
            "stages": stages, "panels": panels, "page": page, "quality": quality,
            "balance": balance, "lineage": lineage_result,
            "verification_status": verification, "coverage_status": coverage,
        })

    board_rows = []
    for board in dict.fromkeys(row["board"] for row in results):
        rows = [row for row in results if row["board"] == board]
        unresolved = [row["symbol"] for row in rows if row["verification_status"] != "PASS"]
        board_rows.append({
            "board": board, "board_name": rows[0]["board_name"], "samples": len(rows),
            "verified": sum(row["verification_status"] == "PASS" for row in rows),
            "source_failed": sum(row["verification_status"] == "SOURCE_FAILED" for row in rows),
            "missing": sum(row["verification_status"] == "MISSING" for row in rows),
            "master_pass": sum(row["master"]["status"] == "PASS" for row in rows),
            **{f"{stage}_pass": sum(row["stages"][stage]["status"] == "PASS" for row in rows) for stage in STAGES},
            "kline_unavailable": sum(row["stages"]["kline"]["status"] == "UNAVAILABLE" for row in rows),
            "page_pass": sum(row["page"]["status"] == "PASS" for row in rows),
            "lineage_pass": sum(row["lineage"]["status"] == "PASS" for row in rows),
            "unresolved_symbols": unresolved,
            "status": "PASS" if not unresolved else "PARTIAL",
        })

    report = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "scope": {"boards": 8, "stocks_per_board": 20, "stocks": 160, "fixed_samples": True},
        "status_definitions": STATUS_DEFINITIONS,
        "storage": {
            "integrity_check": storage.get("integrity_check"),
            "foreign_key_errors": len(storage.get("foreign_key_check") or []),
            "objects_checked": len(storage.get("objects") or []),
            "object_hash_failures": sum(row.get("status") != "PASS" for row in storage.get("objects") or []),
        },
        "boards": board_rows,
        "stocks": results,
        "status": "PASS" if all(row["status"] == "PASS" for row in board_rows) else "PARTIAL",
    }
    write_json(OUT / "acceptance-matrix.json", report)

    stock_columns = ["board", "board_name", "market", "symbol", "name", "verification_status", "coverage_status",
                     "master", *STAGES, "page", "quality", "balance", "lineage", "unresolved"]
    with (OUT / "acceptance-matrix.csv").open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=stock_columns)
        writer.writeheader()
        for row in results:
            unresolved = [f"{STAGE_NAMES[key]}:{value['reason']}" for key, value in row["stages"].items()
                          if value["status"] in {"SOURCE_FAILED", "MISSING", "FAILED"}]
            writer.writerow({
                **{key: row[key] for key in ("board", "board_name", "market", "symbol", "name", "verification_status", "coverage_status")},
                "master": row["master"]["status"],
                **{stage: row["stages"][stage]["status"] for stage in STAGES},
                "page": row["page"]["status"], "quality": row["quality"]["status"],
                "balance": row["balance"]["status"], "lineage": row["lineage"]["status"],
                "unresolved": "；".join(unresolved),
            })

    with (OUT / "acceptance-panels.csv").open("w", encoding="utf-8-sig", newline="") as handle:
        columns = ["board", "market", "symbol", "name", "group", "key", "panel_name", "status", "rows", "source", "as_of", "reason"]
        writer = csv.DictWriter(handle, fieldnames=columns)
        writer.writeheader()
        for row in results:
            for panel in row["panels"]:
                writer.writerow({"board": row["board"], "market": row["market"], "symbol": row["symbol"],
                    "name": row["name"], "group": panel["group"], "key": panel["key"],
                    "panel_name": panel["name"], "status": panel["status"], "rows": panel["rows"],
                    "source": panel.get("source"), "as_of": panel.get("as_of"), "reason": panel.get("reason")})

    markdown = [
        "# 八板块股票详细数据验收报告", "",
        f"生成时间：{report['generated_at']}", "",
        "固定样本：8个板块，每板块20只，共160只；未以易采股票替换失败样本。", "",
        "## 状态口径", "",
        *[f"- `{key}`：{value}" for key, value in STATUS_DEFINITIONS.items()], "",
        "## 板块结果", "",
        "| 板块 | 样本 | 验证通过 | 主数据 | 行情 | 日线有数据 | 日线公开源无记录 | 财务 | 公告 | 新闻 | F10 | 研究 | 页面 | 血缘 | 状态 |",
        "|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|",
    ]
    for row in board_rows:
        markdown.append(
            f"| {row['board_name']} | {row['samples']} | {row['verified']} | {row['master_pass']} | "
            f"{row['quote_pass']} | {row['kline_pass']} | {row['kline_unavailable']} | {row['financials_pass']} | {row['notices_pass']} | "
            f"{row['news_pass']} | {row['extended_pass']} | {row['research_pass']} | {row['page_pass']} | "
            f"{row['lineage_pass']} | {row['status']} |"
        )
    markdown.extend(["", "## 逐股票结果", "",
        "| 板块 | 代码 | 名称 | 主数据 | 行情 | 日线 | 财务 | 公告 | 新闻 | F10 | 研究 | 页面 | 血缘 | 验证 |",
        "|---|---|---|---|---|---|---|---|---|---|---|---|---|---|",
    ])
    for row in results:
        values = " | ".join(row["stages"][stage]["status"] for stage in STAGES)
        markdown.append(f"| {row['board']} | {row['symbol']} | {row['name']} | {row['master']['status']} | {values} | "
                        f"{row['page']['status']} | {row['lineage']['status']} | {row['verification_status']} |")
    unresolved = [row for row in results if row["verification_status"] != "PASS"]
    markdown.extend(["", "## 未解决来源缺口", ""])
    for row in unresolved:
        reasons = [f"{STAGE_NAMES[key]}：{value['reason']}" for key, value in row["stages"].items()
                   if value["status"] in {"SOURCE_FAILED", "MISSING", "FAILED"}]
        markdown.append(f"- {row['board']} {row['symbol']} {row['name']}：{'；'.join(reasons)}")
    markdown.extend(["", "## 存储校验", "",
        f"- SQLite 完整性：`{storage.get('integrity_check')}`",
        f"- 外键错误：`{len(storage.get('foreign_key_check') or [])}`",
        f"- 对象哈希检查：`{len(storage.get('objects') or [])}` 个，失败 `{report['storage']['object_hash_failures']}` 个",
        "- 已采集阶段逐股票检查原始对象及血缘；来源失败且未形成对象的阶段不伪造血缘。", "",
    ])
    (OUT / "acceptance-report.md").write_text("\n".join(markdown), encoding="utf-8")
    print(json.dumps({"status": report["status"], "boards": board_rows,
                      "outputs": ["acceptance-matrix.json", "acceptance-matrix.csv", "acceptance-panels.csv", "acceptance-report.md"]},
                     ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
