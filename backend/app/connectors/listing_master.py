"""Official listing manifests, distinct from issuer and quote data."""
from dataclasses import replace
from datetime import datetime, timezone
from hashlib import sha256
from io import BytesIO
import json
import time

import httpx
import pandas as pd

from app.connectors.base import MarketDataAdapter, SymbolRecord
from app.connectors.company_master_sources import HKEX_SECURITIES_URL, parse_hkex_securities
from app.services.security_master import hk_classification, normalized_ext

EXCHANGE_TABLES = {
    "SSE": (("stock_info_sh_name_code", {"symbol": "主板A股"}), ("stock_info_sh_name_code", {"symbol": "科创板"})),
    "SZSE": (("stock_info_sz_name_code", {"symbol": "A股列表"}), ("stock_info_sz_name_code", {"symbol": "CDR列表"})),
    "BSE": (("stock_info_bj_name_code", {}),),
}


def fetch_exchange_table(method: str, kwargs: dict) -> pd.DataFrame:
    """Read official manifests with bounded requests and no process cache."""
    headers = {"User-Agent": "Mozilla/5.0"}
    observed_at = datetime.now(timezone.utc).isoformat()
    with httpx.Client(timeout=15, follow_redirects=True, headers=headers, transport=httpx.HTTPTransport(retries=1)) as client:
        if method == "stock_info_sh_name_code":
            url = "https://query.sse.com.cn/sseQuery/commonQuery.do"
            response = client.get(url, headers={"Referer": "https://www.sse.com.cn/assortment/stock/list/share/"}, params={
                "STOCK_TYPE": {"主板A股": "1", "科创板": "8"}[kwargs["symbol"]],
                "sqlId": "COMMON_SSE_CP_GPJCTPZ_GPLB_GP_L", "COMPANY_STATUS": "2,4,5,7,8",
                "type": "inParams", "isPagination": "true", "pageHelp.pageSize": "10000", "pageHelp.pageNo": "1",
            })
            response.raise_for_status()
            data = response.json()
            rows = data.get("result") or []
            total = (data.get("pageHelp") or {}).get("total")
            if total is not None and int(total) != len(rows):
                raise ValueError("上交所名单分页未采全")
            frame = pd.DataFrame(rows).rename(columns={"A_STOCK_CODE": "证券代码", "SEC_NAME_CN": "证券简称", "LIST_DATE": "上市日期"})
        elif method == "stock_info_sz_name_code":
            url = "https://www.szse.cn/api/report/ShowReport"
            response = client.get(url, params={"SHOWTYPE": "xlsx", "CATALOGID": "1110", "TABKEY": {"A股列表": "tab1", "CDR列表": "tab3"}[kwargs["symbol"]]})
            response.raise_for_status()
            frame = pd.read_excel(BytesIO(response.content), dtype=str)
            if kwargs["symbol"] == "CDR列表":
                frame = frame.rename(columns={"CDR代码": "证券代码", "CDR简称": "证券简称", "CDR上市日期": "上市日期"})
        elif method == "stock_info_bj_name_code":
            url = "https://www.bse.cn/nqxxController/nqxxCnzq.do"
            payload = {"page": "0", "typejb": "T", "xxfcbj[]": "2", "xxzqdm": "", "sortfield": "xxzqdm", "sorttype": "asc"}
            rows, total_pages = [], None
            deadline = time.monotonic() + 60
            page = 0
            while total_pages is None or page < total_pages:
                remaining = deadline - time.monotonic()
                if remaining <= 0:
                    raise TimeoutError("北交所名单采集超过 60 秒，请重试；未发布不完整名单")
                response = client.post(url, data={**payload, "page": str(page)}, timeout=min(15, remaining))
                response.raise_for_status()
                start = response.text.find("[")
                if start < 0:
                    raise ValueError("北交所名单响应格式不匹配")
                data, _ = json.JSONDecoder().raw_decode(response.text[start:])
                part = data[0]
                pages = int(part["totalPages"])
                if not 1 <= pages <= 100 or (total_pages is not None and pages != total_pages):
                    raise ValueError("北交所名单分页范围变化，需重新采集")
                total_pages = pages
                if not part["content"]:
                    raise ValueError("北交所名单出现空页，不能视为完成")
                rows.extend(part["content"])
                page += 1
            frame = pd.DataFrame(rows).rename(columns={"xxzqdm": "证券代码", "xxzqjc": "证券简称", "fxssrq": "上市日期", "xxhyzl": "所属行业"})
        else:
            raise ValueError(f"未注册的交易所名单方法：{method}")
    frame["_source_url"], frame["_source_method"], frame["_observed_at"] = url, method, observed_at
    frame["_source_board"] = kwargs.get("symbol", "北交所")
    return frame


def with_exchange_proof(record: SymbolRecord) -> SymbolRecord:
    raw = record.raw_payload
    source_code = {"stock_info_sh_name_code": "SSE_MASTER", "stock_info_sz_name_code": "SZSE_MASTER", "stock_info_bj_name_code": "BSE_MASTER"}.get(raw.get("_source_method"))
    ext = dict(record.ext_json or {})
    if source_code:
        ext["listing_classification"] = {"source_code": source_code, "source_url": raw.get("_source_url"), "observed_at": raw.get("_observed_at"),
                                         "source_board": raw.get("_source_board"), "source_record": raw}
    return replace(record, ext_json=normalized_ext(record.market, record.symbol, ext, raw))


class ExchangeMasterAdapter(MarketDataAdapter):
    exchange = ""

    def health_check(self):
        return f"{self.exchange} 官方股票名单采集方法已配置；连通性以实际同步日志为准。", ["SYMBOL_MASTER"]

    def fetch_symbol_master(self, market):
        from app.connectors.akshare_adapter import AkshareAdapter
        if market != "CN_A":
            raise ValueError("交易所 A 股名单仅支持 CN_A")
        records = {}
        for method, kwargs in EXCHANGE_TABLES[self.exchange]:
            frame = fetch_exchange_table(method, kwargs)
            if frame is None:
                raise ValueError(f"{method} 未返回有效名单")
            if frame.empty:
                if kwargs.get("symbol") == "CDR列表":
                    continue
                raise ValueError(f"{method} 返回空名单，不能视为完整同步")
            frame = frame.copy()
            frame["_source_method"] = method
            frame["_source_board"] = kwargs.get("symbol", "北交所")
            for row in AkshareAdapter()._normalize_dataframe(frame, market):
                records[row.symbol] = with_exchange_proof(row)
        return list(records.values())


class SseMasterAdapter(ExchangeMasterAdapter):
    adapter_type, exchange = "SSE_MASTER", "SSE"


class SzseMasterAdapter(ExchangeMasterAdapter):
    adapter_type, exchange = "SZSE_MASTER", "SZSE"


class BseMasterAdapter(ExchangeMasterAdapter):
    adapter_type, exchange = "BSE_MASTER", "BSE"


class HkexSecuritiesAdapter(MarketDataAdapter):
    adapter_type = "HKEX_SECURITIES"

    def health_check(self):
        return "港交所证券名单及主板/GEM、产品类型解析已配置；连通性以实际同步日志为准。", ["SYMBOL_MASTER", "SECURITY_CLASSIFICATION"]

    def fetch_snapshot(self):
        transport = httpx.HTTPTransport(retries=2)
        with httpx.Client(timeout=20, follow_redirects=True, transport=transport, headers={"User-Agent": "Mozilla/5.0"}) as client:
            response = client.get(HKEX_SECURITIES_URL)
            response.raise_for_status()
            if len(response.content) > 20_000_000:
                raise ValueError("港交所证券名单超过下载限制")
            parsed = parse_hkex_securities(response.content)
            if not parsed["records"]:
                raise ValueError("港交所证券名单为空")
            return {**parsed, "source_url": str(response.url), "retrieved_at": datetime.now(timezone.utc).isoformat(), "content_sha256": sha256(response.content).hexdigest(), "raw_content": response.content}

    def fetch_symbol_master(self, market):
        if market != "HK":
            raise ValueError("港交所名单仅支持港股")
        self.last_snapshot = self.fetch_snapshot()
        return self.normalize_snapshot(self.last_snapshot)

    @staticmethod
    def normalize_snapshot(snapshot):
        result = []
        for row in snapshot["records"]:
            board, kind = hk_classification(row)
            # The stock universe may include DRs; fund/warrant products remain
            # in their historical rows but are not added as ordinary stocks.
            if kind not in {"STOCK", "DEPOSITARY_RECEIPT"}:
                continue
            symbol = row["Stock Code"]
            proof = {"source_code": "HKEX_SECURITIES", "source_url": snapshot["source_url"], "observed_at": snapshot["retrieved_at"], "source_date_label": snapshot.get("source_date_label"), "content_hash": snapshot["content_sha256"], "source_record": row}
            ext = normalized_ext("HK", symbol, {"listing_classification": proof})
            result.append(SymbolRecord(market="HK", symbol=symbol, exchange="HKEX", name=row["Name of Securities"], asset_type="STOCK" if kind == "STOCK" else kind, status="LISTED", list_date=None, ext_json=ext, raw_payload=row))
        return result
