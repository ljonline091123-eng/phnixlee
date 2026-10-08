"""Auditable board-to-interface coverage; configuration is not a data proof."""
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models.market_data import DataSource, DataInterface, StockSymbol
from app.services.security_master import BOARD_DEFINITIONS, board_expression

CATEGORIES = {
    "SYMBOL_MASTER": "证券身份与上市名单", "CLASSIFICATION": "上市板块与证券类型",
    "PROFILE": "公司基本资料", "QUOTE": "实时行情", "KLINE": "历史量价",
    "NOTICE": "公告与原文", "FINANCIAL": "财务指标与报表", "HOLDERS": "股东与持股",
    "NEWS": "新闻", "QA": "问董秘", "RESEARCH": "研报、预测与评级", "FUND_FLOW": "资金流向",
}
STATUS_NAMES = {"AVAILABLE": "已接入", "PARTIAL": "部分覆盖", "MISSING": "待接入", "DISABLED": "已停用"}


def _routes(board: str):
    a = board in {"MAIN", "STAR", "CHINEXT", "BSE"}
    hk = board in {"HK_MAIN", "HK_GEM"}
    # (source code, interface code, coverage limitation)
    if a:
        masters = {"MAIN": ["SSE_MASTER", "SZSE_MASTER"], "STAR": ["SSE_MASTER"], "CHINEXT": ["SZSE_MASTER"], "BSE": ["BSE_MASTER"]}[board]
        routes = {
            "SYMBOL_MASTER": [(code, "CN_A_SYMBOLS", "") for code in masters] + [("AKSHARE", "CN_A_SYMBOLS", "综合码表补齐，独立交易所名单用于核对")],
            "CLASSIFICATION": [("AKSHARE", "CN_A_SYMBOLS", "代码规则规范分类；不是主题概念标签，也不是历史转板证据")],
            "PROFILE": [("CNINFO_OFFICIAL", "F10_PROFILE_HOLDERS", "部分公司字段未披露")],
            "QUOTE": [("AKSHARE", "QUOTE_ON_DEMAND", "")],
            "KLINE": [("AKSHARE", "A_KLINE_ON_DEMAND", "")],
            "NOTICE": [("CNINFO_OFFICIAL", "NOTICE_ON_DEMAND", "")],
            "FINANCIAL": [("AKSHARE", "FINANCIAL_ON_DEMAND", "北交所已使用 .BJ 身份；供应商字段及个股覆盖仍需逐股核验" if board == "BSE" else "")],
            "HOLDERS": [("CNINFO_OFFICIAL", "F10_PROFILE_HOLDERS", "依赖披露期与供应商覆盖，非实时完整股东名册")],
            "NEWS": [("AKSHARE", "NEWS_ON_DEMAND", "新闻搜索并非全网新闻全集")],
            "RESEARCH": [("AKSHARE", "CN_A_RESEARCH_REPORT_ON_DEMAND", "仅公开研报，不能保证券商授权报告全集"), ("AKSHARE", "CN_A_PROFIT_FORECAST_ON_DEMAND", "无机构覆盖的个股可能没有预测")],
            "FUND_FLOW": [("AKSHARE", "CN_A_FUND_FLOW_ON_DEMAND", "按 F10 扩展分区采集；是供应商估算资金流，非真实账户流向")],
        }
        routes["QA"] = [("P5W_PUBLIC", "P5W_INVESTOR_QA", "公开公司互动（含待回复）；非完整交易所问答全集，个股须核验") ] if board == "BSE" else [("AKSHARE", "CN_A_SSE_QA_ON_DEMAND" if board == "STAR" else "CN_A_IRM_QA_ON_DEMAND", "")]
        if board == "MAIN":
            routes["QA"].append(("AKSHARE", "CN_A_SSE_QA_ON_DEMAND", ""))
        return routes
    if hk:
        return {
            "SYMBOL_MASTER": [("HKEX_SECURITIES", "HK_SYMBOLS", ""), ("AKSHARE", "HK_SYMBOLS", "行情名单作中文名称补充"), ("AKSHARE_HK_SINA", "HK_SYMBOLS", "备用名单")],
            "CLASSIFICATION": [("HKEX_SECURITIES", "HK_SYMBOLS", "")],
            "PROFILE": [("HKEXNEWS_OFFICIAL", "F10_PROFILE_HOLDERS", "公司资料使用东方财富转录，不能当作工商认证")],
            "QUOTE": [("AKSHARE", "QUOTE_ON_DEMAND", "公开行情可能有延迟")],
            "KLINE": [("AKSHARE", "HK_KLINE_ON_DEMAND", ""), ("AKSHARE_HK_SINA", "HK_KLINE_ON_DEMAND", "")],
            "NOTICE": [("HKEXNEWS_OFFICIAL", "NOTICE_ON_DEMAND", "")],
            "FINANCIAL": [("HKEXNEWS_OFFICIAL", "FINANCIAL_ON_DEMAND", "披露易原文与供应商指标分开；币种、会计期不强制转为 A 股口径")],
            "HOLDERS": [("HKEXNEWS_OFFICIAL", "F10_PROFILE_HOLDERS", "CCASS/权益披露入口及部分结构化持仓，非完整穿透股东"),
                        ("ETNET_HK", "HK_COMPANY_DISCLOSURES_ON_DEMAND", "公开主要股东、各自披露日期与持股比例；非完整十大名单，不推断控制或流通属性")],
            "NEWS": [("AKSHARE", "NEWS_ON_DEMAND", "代码搜索可能混入其他证券新闻，需证据核对")],
            "QA": [("HK_ISSUER_IR", "HK_INVESTOR_QA_ON_DEMAND", "仅已核验的小鹏、蔚来、理想汽车及百度官网常见问答；不是全市场互动平台或业绩会问答")],
            "RESEARCH": [("ETNET_HK", "HK_PROFIT_FORECAST_ON_DEMAND", "公开盈利和机构预测只覆盖部分证券商；评级为当前快照，非固定滚动窗口"),
                         ("AASTOCKS_HK", "HK_RESEARCH_REPORT_ON_DEMAND", "公开大行报告摘要，非券商原始全文；不保证所有机构及历史报告全集")],
            "FUND_FLOW": [],
        }
    return {
        "SYMBOL_MASTER": [("NEEQ_EASTMONEY", "NEEQ_INNOVATION_SYMBOLS" if board == "NEEQ_INNOVATION" else "NEEQ_SYMBOLS", "东方财富挂牌层级转录，官方层级变动接口待接入")],
        "CLASSIFICATION": [("NEEQ_EASTMONEY", "NEEQ_INNOVATION_SYMBOLS" if board == "NEEQ_INNOVATION" else "NEEQ_SYMBOLS", "挂牌层级以供应商标记为证，变更历史尚不完整")],
        "PROFILE": [("NEEQ_EASTMONEY", "F10_PROFILE_HOLDERS", "供应商资料字段覆盖有限")],
        "QUOTE": [("NEEQ_EASTMONEY", "NEEQ_QUOTE_ON_DEMAND", "低流动性、停牌或无成交时可能无报价")],
        "KLINE": [("NEEQ_EASTMONEY", "NEEQ_KLINE_ON_DEMAND", "无成交不生成虚构 K 线")],
        "NOTICE": [("NEEQ_EASTMONEY", "NOTICE_ON_DEMAND", "全国股转披露的供应商转录，官方直连尚待接入"),
                   ("P5W_PUBLIC", "P5W_PUBLIC_NOTICES", "公开公司公告备源，保留全国股转原文与日期差异；非官方直连")],
        "FINANCIAL": [("NEEQ_EASTMONEY", "FINANCIAL_ON_DEMAND", "部分公司指标缺失，保留原始财报")],
        "HOLDERS": [("NEEQ_EASTMONEY", "F10_PROFILE_HOLDERS", "按定期披露，结构化持股覆盖有限")],
        "NEWS": [("NEEQ_EASTMONEY", "NEEQ_NEWS_ON_DEMAND", "新闻覆盖有限")],
        "QA": [("P5W_PUBLIC", "P5W_INVESTOR_QA", "公开公司互动；该源无记录不能代表全网没有问答")], "RESEARCH": [], "FUND_FLOW": [],
    }


def source_coverage(db: Session):
    sources = {row.source_code: row for row in db.scalars(select(DataSource)).all()}
    interfaces = {(row.source_id, row.interface_code): row for row in db.scalars(select(DataInterface)).all()}
    counts = dict(db.execute(select(board_expression(), func.count()).select_from(StockSymbol).group_by(board_expression())).all())
    boards = []
    for board in BOARD_DEFINITIONS:
        capabilities = []
        routes = _routes(board["code"])
        for category, label in CATEGORIES.items():
            configured = []
            for source_code, interface_code, limitation in routes.get(category, []):
                source = sources.get(source_code)
                interface = interfaces.get((source.id, interface_code)) if source else None
                enabled = bool(source and source.enabled and interface and interface.enabled and board["market"] in interface.supported_markets)
                configured.append({"source_code": source_code, "source_name": source.source_name if source else source_code, "interface_code": interface_code,
                                   "enabled": enabled, "source_url": (source.config_json or {}).get("official_url") if source else None, "limitation": limitation})
            enabled = [item for item in configured if item["enabled"]]
            status = "MISSING" if not configured else "DISABLED" if not enabled else "PARTIAL" if all(item["limitation"] for item in enabled) else "AVAILABLE"
            capabilities.append({"category": category, "label": label, "status": status, "status_name": STATUS_NAMES[status], "sources": configured,
                                 "note": "已接入表示代码与启用接口匹配，不代表该板块每只股票均已采全。" if enabled else "需要匹配该市场的公开接口或取得供应商授权；不会借用 A 股数据填充其他市场。"})
        boards.append({**board, "record_count": counts.get(board["code"], 0), "capabilities": capabilities})
    return {"boards": boards, "unclassified_count": counts.get("UNCLASSIFIED", 0) + counts.get(None, 0), "notes": ["上市板块、行业/概念、蓝筹/红筹等风格标签分别管理。", "记录数量含保留的历史状态；前端列表可筛选在市股票。", "港股研究接入经济通预测及 AASTOCKS 摘要；港股问答仅覆盖已核验的发行人官网常见问答。新三板研究仍待接入。", "来源链接、空栏目和行情估值不等于股东明细、真实预测或财报；须以逐股采集与入库证据验收。"]}
