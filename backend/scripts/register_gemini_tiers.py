"""Incrementally sync Gemini instances; preserve IDs, keys, routes and history.

Run from backend after the required Git/database backups:
  python scripts/register_gemini_tiers.py --backup backups/quant-before-gemini-models-YYYYMMDD-HHMMSS.db

No network calls or API quota are consumed by registration.
"""
from __future__ import annotations

import argparse
import json
import sqlite3
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from sqlalchemy import inspect, select
from app.db.session import SessionLocal, engine
from app.models.ai_hub import ModelInstance, ModelInstanceDeletion, ModelProvider, ModelRouteRule
from app.services.model_hub import sync_gemini_model_catalog


def main() -> None:
    parser = argparse.ArgumentParser(description="增量接入 Gemini 模型，保留现有任务路由。")
    parser.add_argument("--backup", required=True, type=Path, help="修改前的一致性数据库备份")
    args = parser.parse_args()
    backup = args.backup.resolve(strict=True)
    live_path = engine.url.database
    if not live_path or backup == Path(live_path).resolve():
        raise SystemExit("备份必须是独立文件")
    with sqlite3.connect(backup.as_uri() + "?mode=ro", uri=True) as source:
        if source.execute("PRAGMA quick_check").fetchall() != [("ok",)]:
            raise SystemExit("备份完整性检查未通过")
        if not source.execute("SELECT 1 FROM model_provider WHERE provider_code='GEMINI'").fetchone():
            raise SystemExit("备份中没有 Gemini 配置")

    with SessionLocal() as db:
        provider = db.scalar(select(ModelProvider).where(ModelProvider.provider_code == "GEMINI"))
        if provider is None:
            raise SystemExit("尚无 Gemini 供应商，请先在模型管理中配置。")
        routes = list(db.scalars(select(ModelRouteRule).order_by(ModelRouteRule.id)))
        before_routes = [(r.id, r.preferred_instance_code, r.fallback_chain_json, r.enabled, r.description) for r in routes]
        deleted = set(db.scalars(select(ModelInstanceDeletion.instance_code))) if inspect(engine).has_table("model_instance_deletion") else set()
        before = {r.instance_code: r.id for r in db.scalars(select(ModelInstance))}
        sync_gemini_model_catalog(db, provider, deleted)
        db.flush()
        after = list(db.scalars(select(ModelInstance)))
        assert all(before.get(r.instance_code, r.id) == r.id for r in after), "已有实例 ID 不得变化"
        assert before_routes == [(r.id, r.preferred_instance_code, r.fallback_chain_json, r.enabled, r.description) for r in routes], "任务路由不得变化"
        db.commit()
        rows = [{"id": r.id, "instance_code": r.instance_code, "model_code": r.model_code, "enabled": r.enabled} for r in after if r.provider_id == provider.id]
        print(json.dumps({"backup": str(backup), "models": rows, "routes_preserved": len(routes)}, ensure_ascii=True, indent=2))


if __name__ == "__main__":
    main()
