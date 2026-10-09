"""Verify and enrich the fixed NEEQ samples from the official directory."""
from datetime import datetime, timezone
import json
from pathlib import Path
import re
import sys
import unicodedata

from sqlalchemy import select

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from app.connectors.neeq_official import fetch_company
from app.db.session import SessionLocal
from app.models.market_data import StockSymbol
from app.services.security_master import normalized_ext

OUT = ROOT / "validation" / "all-boards-20261008"


def normalized_name(value):
    return re.sub(r"\s+", "", unicodedata.normalize("NFKC", str(value or ""))).upper()


def main():
    samples = json.loads((OUT / "samples.json").read_text(encoding="utf-8"))
    samples = [row for row in samples if row["board"] in {"NEEQ_BASE", "NEEQ_INNOVATION"}]
    results = []
    for sample in samples:
        result = {**sample, "checked_at": datetime.now(timezone.utc).isoformat()}
        try:
            proof = fetch_company(sample["symbol"])
            row = proof["source_record"]
            name_ok = normalized_name(row["xxzqjc"]) == normalized_name(sample["name"])
            layer_ok = proof["listing_board"] == sample["board"]
            result.update(status="PASS" if name_ok and layer_ok else "CONFLICT",
                          name_match=name_ok, layer_match=layer_ok, evidence=proof)
            if result["status"] == "PASS":
                with SessionLocal() as db:
                    stock = db.scalar(select(StockSymbol).where(
                        StockSymbol.market == sample["market"], StockSymbol.symbol == sample["symbol"]))
                    if stock is None or stock.id != sample["master_id"]:
                        raise ValueError("本地主数据身份与固定样本不一致")
                    ext = dict(stock.ext_json or {})
                    ext["neeq_official_verification"] = {key: value for key, value in proof.items()
                                                          if key != "original_source_response"}
                    stock.ext_json = normalized_ext(stock.market, stock.symbol, ext, stock.raw_payload)
                    db.commit()
        except Exception as exc:
            result.update(status="FAILED", error=str(exc)[:700])
        results.append(result)
        print(json.dumps({key: result.get(key) for key in ("board", "symbol", "status", "name_match", "layer_match", "error")}, ensure_ascii=False), flush=True)
    report = {"checked_at": datetime.now(timezone.utc).isoformat(), "source": "全国股转系统官方挂牌公司名录",
              "results": results, "status": "PASS" if all(r["status"] == "PASS" for r in results) else "PARTIAL"}
    (OUT / "neeq-official-master.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"status": report["status"], "passed": sum(r["status"] == "PASS" for r in results),
                      "expected": len(results)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
