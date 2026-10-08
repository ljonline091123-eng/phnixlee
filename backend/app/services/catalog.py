from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.markets import MARKET_HK, MARKET_NEEQ, MARKET_NEEQ_INNOVATION
from app.models.market_data import DataInterface, DataSource

DEFAULT_SOURCES = (
    {
        "source_code": "AKSHARE",
        "source_name": "综合证券数据（AkShare、东方财富、交易所）",
        "source_type": "MARKET_DATA",
        "adapter_type": "AKSHARE",
        "priority": 100,
        "enabled": True,
        "config_json": {
            "fallback_source_code": "AKSHARE_HK_SINA",
            "market_scope": ["CN_A", "HK", MARKET_NEEQ, MARKET_NEEQ_INNOVATION],
            "capabilities": [
                "SYMBOL_MASTER", "KLINE", "QUOTE", "NEWS", "NOTICE", "FINANCIAL",
                "RESEARCH", "QA", "FORECAST", "RATING",
            ],
            "official_sources": [
                "AkShare", "Eastmoney", "CNINFO 互动易", "上证 e 互动", "同花顺盈利预测",
            ],
        },
        "description": "Market-data adapter using AkShare, official CNINFO A-share disclosures, and Eastmoney NEEQ master data.",
    },
    {
        "source_code": "CNINFO_OFFICIAL",
        "source_name": "巨潮资讯（CNINFO）官方披露",
        "source_type": "OFFICIAL_DISCLOSURE",
        "adapter_type": "AKSHARE",
        "priority": 110,
        "enabled": True,
        "config_json": {
            "market_scope": ["CN_A"],
            "capabilities": ["F10", "NOTICE", "FINANCIAL", "REPORT", "PROFILE", "HOLDERS"],
            "official_url": "https://www.cninfo.com.cn/",
            "route": "CN_A",
        },
        "description": "A 股上市公司官方公告、年报/半年报/季报披露入口。行情和 K 线仍由综合行情源提供。",
    },
    {
        "source_code": "HKEXNEWS_OFFICIAL",
        "source_name": "港交所披露易（HKEXnews）",
        "source_type": "OFFICIAL_DISCLOSURE",
        "adapter_type": "AKSHARE",
        "priority": 120,
        "enabled": True,
        "config_json": {
            "market_scope": ["HK"],
            "capabilities": ["F10", "NOTICE", "FINANCIAL", "REPORT", "HOLDERS"],
            "official_url": "https://www.hkexnews.hk/",
            "route": "HK",
        },
        "description": "港股发行人公告、业绩公告及财务报表的官方披露入口。",
    },
    {
        "source_code": "NEEQ_EASTMONEY",
        "source_name": "东方财富新三板披露",
        "source_type": "DISCLOSURE",
        "adapter_type": "AKSHARE",
        "priority": 130,
        "enabled": True,
        "config_json": {
            "market_scope": [MARKET_NEEQ, MARKET_NEEQ_INNOVATION],
            "capabilities": ["SYMBOL_MASTER", "KLINE", "QUOTE", "F10", "NOTICE", "FINANCIAL", "REPORT", "NEWS", "PROFILE", "HOLDERS"],
            "official_url": "https://xinsanban.eastmoney.com/",
            "route": "NEEQ",
        },
        "description": "新三板基础层、创新层公告、正式财报、财务指标和企业资料。",
    },
    {
        "source_code": "PYTDX",
        "source_name": "PyTDX / 通达信行情",
        "source_type": "MARKET_DATA",
        "adapter_type": "PYTDX",
        "priority": 160,
        "enabled": False,
        "config_json": {
            "market_scope": ["CN_A"],
            "capabilities": ["KLINE", "QUOTE"],
            "official_url": "https://github.com/rainx/pytdx",
            "provider_kind": "OPTIONAL_QUOTE",
            "notes": "仅提供 A 股实时行情与日 K；不提供公告、财报、港股或 NEEQ 数据。",
        },
        "description": "可选通达信行情节点适配器，用于补充 A 股实时行情和历史日 K。",
    },
    {
        "source_code": "NEEQ_OFFICIAL",
        "source_name": "全国股转系统官方公开数据",
        "source_type": "OFFICIAL_REFERENCE",
        "adapter_type": "OFFICIAL_REFERENCE",
        "priority": 320,
        "enabled": False,
        "config_json": {
            "market_scope": [MARKET_NEEQ, MARKET_NEEQ_INNOVATION],
            "capabilities": [
                "REFERENCE",
                "CAPITAL_RAISE",
                "MARKET_MAKER",
                "LAYER_CHANGE",
                "NOTICE",
                "REPORT",
            ],
            "official_url": "https://www.neeq.com.cn/",
            "provider_kind": "OFFICIAL_PENDING",
            "notes": "官方入口已登记；定增、做市商、层级变动等结构化抓取待按公开页面/API逐项接入。",
        },
        "description": "全国股转系统官方公开信息入口，当前作为待结构化接入的权威参考源。",
    },
    {
        "source_code": "AKSHARE_HK_SINA",
        "source_name": "新浪港股行情与主数据（备用）",
        "source_type": "MARKET_DATA",
        "adapter_type": "AKSHARE_HK_SINA",
        "priority": 200,
        "enabled": True,
        "config_json": {"market_scope": ["HK"], "fallback_source_code": "HKEX_SDW", "capabilities": ["SYMBOL_MASTER", "KLINE"]},
        "description": "Hong Kong stock master-data fallback using AkShare Sina endpoints.",
    },
    {
        "source_code": "HKEX_SDW",
        "source_name": "港交所 CCASS 证券清单（备用）",
        "source_type": "MARKET_DATA",
        "adapter_type": "HKEX_SDW",
        "priority": 300,
        "enabled": True,
        "config_json": {
            "market_scope": ["HK"],
            "capabilities": ["SYMBOL_MASTER"],
            "official_url": "https://www.hkex.com.hk/",
            "securities_list_url": "https://www.hkexnews.hk/sdw/search/stocklist.aspx",
            "ccass_disclosure_url": "https://www.hkexnews.hk/sdw/search/mutualmarket.aspx",
            "provider_kind": "OFFICIAL_SECURITIES_LIST",
            "notes": "港交所证券清单/CCASS 查询入口；公告与财报由 HKEXNEWS_OFFICIAL 提供。",
        },
        "description": "港交所官方证券清单与股东披露查询入口，作为港股主数据备用源。",
    },
) 

LISTING_SOURCES = (
    {"source_code": "SSE_MASTER", "source_name": "上交所官方股票名单（主板、科创板）", "source_type": "OFFICIAL_MASTER", "adapter_type": "SSE_MASTER", "priority": 210, "enabled": True,
     "config_json": {"market_scope": ["CN_A"], "board_scope": ["MAIN", "STAR"], "capabilities": ["SYMBOL_MASTER"], "official_url": "https://www.sse.com.cn/assortment/stock/list/share/"}, "description": "分别请求主板 A 股和科创板名单，不能用默认主板名单代替全部沪市股票。"},
    {"source_code": "SZSE_MASTER", "source_name": "深交所官方股票名单（主板、创业板）", "source_type": "OFFICIAL_MASTER", "adapter_type": "SZSE_MASTER", "priority": 211, "enabled": True,
     "config_json": {"market_scope": ["CN_A"], "board_scope": ["MAIN", "CHINEXT"], "capabilities": ["SYMBOL_MASTER"], "official_url": "https://www.szse.cn/market/product/stock/list/index.html"}, "description": "深交所 A 股及 CDR 名单，主板与创业板分别规范分类。"},
    {"source_code": "BSE_MASTER", "source_name": "北交所官方上市公司名单", "source_type": "OFFICIAL_MASTER", "adapter_type": "BSE_MASTER", "priority": 212, "enabled": True,
     "config_json": {"market_scope": ["CN_A"], "board_scope": ["BSE"], "capabilities": ["SYMBOL_MASTER"], "official_url": "https://www.bse.cn/nq/listedcompany.html"}, "description": "北交所上市股票主数据；兼容 920 新代码，不将北交所归为新三板挂牌。"},
    {"source_code": "HKEX_SECURITIES", "source_name": "港交所官方证券名单与产品分类", "source_type": "OFFICIAL_MASTER", "adapter_type": "HKEX_SECURITIES", "priority": 213, "enabled": True,
     "config_json": {"market_scope": ["HK"], "board_scope": ["HK_MAIN", "HK_GEM"], "capabilities": ["SYMBOL_MASTER", "SECURITY_CLASSIFICATION"], "official_url": "https://www.hkex.com.hk/eng/services/trading/securities/securitieslists/ListOfSecurities.xlsx"}, "description": "官方 XLSX 识别主板、GEM、股票、存托凭证、ETF、权证及牛熊证；普通股名单排除非股票产品，保留历史产品记录。"},
)
DEFAULT_SOURCES = (*DEFAULT_SOURCES, *LISTING_SOURCES)

AKSHARE_INTERFACES = (
    {
        "interface_code": "CN_A_SYMBOLS",
        "interface_name": "A股全板块股票主数据",
        "data_category": "SYMBOL_MASTER",
        "request_mode": "SYNC",
        "adapter_method": "stock_info_a_code_name",
        "supported_markets": ["CN_A"],
        "description": "汇总上交所主板及科创板、深交所主板及创业板、北交所名单，并用东方财富及综合码表补齐。",
    },
    {
        "interface_code": "HK_SYMBOLS",
        "interface_name": "港股股票主数据",
        "data_category": "SYMBOL_MASTER",
        "request_mode": "SYNC",
        "adapter_method": "stock_hk_spot_em",
        "supported_markets": [MARKET_HK],
        "description": "Bulk synchronization of Hong Kong stock codes and names.",
    },
    {
        "interface_code": "A_KLINE_ON_DEMAND",
        "interface_name": "A-share historical K-line",
        "data_category": "KLINE",
        "request_mode": "ON_DEMAND",
        "adapter_method": "stock_zh_a_hist / Eastmoney push2his",
        "supported_markets": ["CN_A", MARKET_NEEQ, MARKET_NEEQ_INNOVATION],
        "description": "Symbol-level historical K-line retrieval for A-share and NEEQ markets.",
    },
    {
        "interface_code": "HK_KLINE_ON_DEMAND",
        "interface_name": "Hong Kong historical K-line",
        "data_category": "KLINE",
        "request_mode": "ON_DEMAND",
        "adapter_method": "stock_hk_hist",
        "supported_markets": ["HK"],
        "description": "按需采集港股历史 K 线；行情源无覆盖或无成交时不填造数据。",
    },
    {
        "interface_code": "NOTICE_ON_DEMAND",
        "interface_name": "Announcement retrieval",
        "data_category": "NOTICE",
        "request_mode": "ON_DEMAND",
        "adapter_method": "CNINFO / HKEXnews / Eastmoney xinsanban",
        "supported_markets": ["CN_A", MARKET_HK, MARKET_NEEQ, MARKET_NEEQ_INNOVATION],
        "description": "A-share announcements use CNINFO, Hong Kong announcements use HKEXnews, and NEEQ announcements use Eastmoney xinsanban.",
    },
    {
        "interface_code": "FINANCIAL_ON_DEMAND",
        "interface_name": "Financial report retrieval",
        "data_category": "FINANCIAL",
        "request_mode": "ON_DEMAND",
        "adapter_method": "AkShare / Eastmoney xinsanban",
        "supported_markets": ["CN_A", MARKET_HK, MARKET_NEEQ, MARKET_NEEQ_INNOVATION],
        "description": "Financial indicators from the configured A-share, Hong Kong, or NEEQ provider.",
    },
    {
        "interface_code": "QUOTE_ON_DEMAND",
        "interface_name": "Realtime stock quote",
        "data_category": "QUOTE",
        "request_mode": "ON_DEMAND",
        "adapter_method": "Eastmoney / Tencent quote",
        "supported_markets": ["CN_A", MARKET_HK, MARKET_NEEQ, MARKET_NEEQ_INNOVATION],
        "description": "Fetches the latest price, change, range, volume, and turnover snapshot.",
    },
    {
        "interface_code": "NEWS_ON_DEMAND",
        "interface_name": "Stock news retrieval",
        "data_category": "NEWS",
        "request_mode": "ON_DEMAND",
        "adapter_method": "stock_news_em / Eastmoney xinsanban",
        "supported_markets": ["CN_A", MARKET_HK, MARKET_NEEQ, MARKET_NEEQ_INNOVATION],
        "description": "Fetches recent company and market news for a selected stock.",
    },
    {
        "interface_code": "CN_A_IRM_QA_ON_DEMAND",
        "interface_name": "A股互动易问董秘",
        "data_category": "QA",
        "request_mode": "ON_DEMAND",
        "adapter_method": "OfficialQAClient (cninfo question / questionDetail)",
        "supported_markets": ["CN_A"],
        "description": "巨潮资讯互动易提问、回答及问题详情；保留问题编号和官方详情链接。",
    },
    {
        "interface_code": "CN_A_SSE_QA_ON_DEMAND",
        "interface_name": "沪市上证 e 互动问董秘",
        "data_category": "QA",
        "request_mode": "ON_DEMAND",
        "adapter_method": "OfficialQAClient (sseinfo company / userfeeds)",
        "supported_markets": ["CN_A"],
        "description": "上证 e 互动官方公司回复；动态解析公司身份、分页断点续采，保留问答原文、时间、来源链接及哈希。",
    },
    {
        "interface_code": "CN_A_RESEARCH_REPORT_ON_DEMAND",
        "interface_name": "A股个股研究报告",
        "data_category": "RESEARCH",
        "request_mode": "ON_DEMAND",
        "adapter_method": "stock_research_report_em",
        "supported_markets": ["CN_A"],
        "description": "东方财富个股研报列表，返回标题、日期、机构、评级及 PDF 原文链接。",
    },
    {
        "interface_code": "CN_A_PROFIT_FORECAST_ON_DEMAND",
        "interface_name": "A股盈利预测与机构明细",
        "data_category": "FORECAST",
        "request_mode": "ON_DEMAND",
        "adapter_method": "stock_profit_forecast_ths / stock_profit_forecast_em",
        "supported_markets": ["CN_A"],
        "description": "同花顺年度盈利预测和机构明细，东方财富近六个月评级汇总。",
    },
    {
        "interface_code": "CN_A_FUND_FLOW_ON_DEMAND", "interface_name": "A股个股资金流向",
        "data_category": "FUND_FLOW", "request_mode": "ON_DEMAND", "adapter_method": "_safe_cn_fund_flow / _fetch_cn_fund_flow_http",
        "supported_markets": ["CN_A"], "description": "F10 资金分区中的供应商估算资金流，金额为元；无数据保持缺失，不能解释为真实账户流向。",
    },
)

AKSHARE_HK_SINA_INTERFACES = (
    {
        "interface_code": "HK_SYMBOLS",
        "interface_name": "Hong Kong stock master data",
        "data_category": "SYMBOL_MASTER",
        "request_mode": "SYNC",
        "adapter_method": "stock_hk_spot",
        "supported_markets": ["HK"],
        "description": "Fallback bulk synchronization of Hong Kong codes and names.",
    },
    {
        "interface_code": "HK_KLINE_ON_DEMAND",
        "interface_name": "Hong Kong historical K-line",
        "data_category": "KLINE",
        "request_mode": "ON_DEMAND",
        "adapter_method": "stock_hk_daily",
        "supported_markets": ["HK"],
        "description": "On-demand Hong Kong K-line retrieval through Sina endpoints.",
    },
)

HKEX_SDW_INTERFACES = (
    {
        "interface_code": "HK_SYMBOLS",
        "interface_name": "Hong Kong stock master data",
        "data_category": "SYMBOL_MASTER",
        "request_mode": "SYNC",
        "adapter_method": "HKEXnews stocklist",
        "supported_markets": ["HK"],
        "description": "Fallback synchronization of Hong Kong listed securities from HKEX.",
    },
    {
        "interface_code": "HKEX_CCASS_REFERENCE",
        "interface_name": "港股 CCASS 股东披露入口",
        "data_category": "HOLDERS_REFERENCE",
        "request_mode": "ON_DEMAND",
        "adapter_method": "HKEX SDW/CCASS official portal",
        "supported_markets": ["HK"],
        "description": "官方股东/中央结算系统查询入口；公告与财报由 HKEXNEWS_OFFICIAL 提供。",
    },
)

DISCLOSURE_INTERFACES = (
    {
        "interface_code": "NOTICE_ON_DEMAND", "interface_name": "官方公告与披露原文",
        "data_category": "NOTICE", "request_mode": "ON_DEMAND", "adapter_method": "fetch_notices (CNINFO / HKEXnews / Eastmoney NEEQ)",
        "supported_markets": ["CN_A", MARKET_HK, MARKET_NEEQ, MARKET_NEEQ_INNOVATION],
        "description": "按来源市场路由至巨潮、披露易或新三板转录公告，保留原文链接与来源；新三板不是全国股转官方直连。",
    },
    {
        "interface_code": "FINANCIAL_ON_DEMAND", "interface_name": "财务指标与报表",
        "data_category": "FINANCIAL", "request_mode": "ON_DEMAND", "adapter_method": "fetch_financial_reports / financial_statements",
        "supported_markets": ["CN_A", MARKET_HK, MARKET_NEEQ, MARKET_NEEQ_INNOVATION],
        "description": "财务指标使用对应市场供应商数据，正式报告原文单独归档；不将供应商财务数值标成官方工商认证。",
    },
    {
        "interface_code": "F10_REPORTS",
        "interface_name": "正式财报与披露文件",
        "data_category": "REPORT",
        "request_mode": "ON_DEMAND",
        "adapter_method": "published_reports",
        "supported_markets": ["CN_A", MARKET_HK, MARKET_NEEQ, MARKET_NEEQ_INNOVATION],
        "description": "返回带原文链接的年报、半年报、季报、审计报告；财务指标单独归档到财务页签。",
    },
    {
        "interface_code": "F10_PROFILE_HOLDERS",
        "interface_name": "F10 简况与股东",
        "data_category": "F10",
        "request_mode": "ON_DEMAND",
        "adapter_method": "profile / holders",
        "supported_markets": ["CN_A", MARKET_HK, MARKET_NEEQ, MARKET_NEEQ_INNOVATION],
        "description": "公司简况、主要股东及官方股东查询入口。",
    },
)


def seed_default_catalog(db: Session) -> None:
    sources: dict[str, DataSource] = {}
    for item in DEFAULT_SOURCES:
        source = db.scalar(select(DataSource).where(DataSource.source_code == item["source_code"]))
        if not source:
            source = DataSource(**item)
            db.add(source)
            db.flush()
        else:
            # Keep operator-provided keys while allowing newly registered
            # capabilities/sources to be added on subsequent boots.  The old
            # merge order silently preserved a stale ``capabilities`` list,
            # making the research interfaces invisible in existing catalogs.
            existing_config = dict(source.config_json or {})
            default_config = dict(item["config_json"] or {})
            merged_config = {**default_config, **existing_config}
            for list_key in ("capabilities", "official_sources"):
                values = list(dict.fromkeys([
                    *(existing_config.get(list_key) or []),
                    *(default_config.get(list_key) or []),
                ]))
                if values:
                    merged_config[list_key] = values
            source.config_json = merged_config
            source.source_name = item["source_name"]
            source.source_type = item["source_type"]
            source.adapter_type = item["adapter_type"]
            source.description = item["description"]
        sources[source.source_code] = source

    for source_code, interfaces in {
        "AKSHARE": AKSHARE_INTERFACES,
        "CNINFO_OFFICIAL": DISCLOSURE_INTERFACES,
        "HKEXNEWS_OFFICIAL": DISCLOSURE_INTERFACES,
        "NEEQ_EASTMONEY": (*DISCLOSURE_INTERFACES, *({
            "interface_code": code, "interface_name": name, "data_category": category, "request_mode": mode,
            "adapter_method": method, "supported_markets": [MARKET_NEEQ, MARKET_NEEQ_INNOVATION], "description": description,
        } for code, name, category, mode, method, description in (
            ("NEEQ_SYMBOLS", "新三板基础层股票名单", "SYMBOL_MASTER", "SYNC", "_fetch_neeq_symbols(BASE)", "东方财富挂牌码表；保留来源标记与原始层级，不用于北交所股票。"),
            ("NEEQ_INNOVATION_SYMBOLS", "新三板创新层股票名单", "SYMBOL_MASTER", "SYNC", "_fetch_neeq_symbols(INNOVATION)", "根据创新层来源标记采集，保留 NEEQ_INNOVATION 历史身份。"),
            ("NEEQ_KLINE_ON_DEMAND", "新三板历史量价", "KLINE", "ON_DEMAND", "_fetch_neeq_kline", "新三板历史成交数据；无成交不补造 K 线。"),
            ("NEEQ_QUOTE_ON_DEMAND", "新三板实时行情", "QUOTE", "ON_DEMAND", "_fetch_neeq_quote", "挂牌证券最新行情；停牌或无成交时保留缺失状态。"),
            ("NEEQ_NEWS_ON_DEMAND", "新三板公司新闻", "NEWS", "ON_DEMAND", "_fetch_neeq_news", "新三板公司新闻转录，保留发布者与原文链接。"),
        ))),
        "AKSHARE_HK_SINA": AKSHARE_HK_SINA_INTERFACES,
        "HKEX_SDW": HKEX_SDW_INTERFACES,
        **{item["source_code"]: ({
            "interface_code": "HK_SYMBOLS" if item["source_code"] == "HKEX_SECURITIES" else "CN_A_SYMBOLS",
            "interface_name": item["source_name"], "data_category": "SYMBOL_MASTER", "request_mode": "SYNC",
            "adapter_method": {"SSE_MASTER": "stock_info_sh_name_code(主板A股/科创板)", "SZSE_MASTER": "stock_info_sz_name_code(A股列表/CDR列表)", "BSE_MASTER": "stock_info_bj_name_code", "HKEX_SECURITIES": "ListOfSecurities.xlsx / parse_hkex_securities"}[item["source_code"]],
            "supported_markets": item["config_json"]["market_scope"], "description": item["description"],
        },) for item in LISTING_SOURCES},
        "PYTDX": (
            {
                "interface_code": "PYTDX_QUOTE_ON_DEMAND",
                "interface_name": "PyTDX A股实时行情",
                "data_category": "QUOTE",
                "request_mode": "ON_DEMAND",
                "adapter_method": "get_security_quotes",
                "supported_markets": ["CN_A"],
                "description": "可选通达信行情节点实时行情。",
            },
            {
                "interface_code": "PYTDX_KLINE_ON_DEMAND",
                "interface_name": "PyTDX A股日K",
                "data_category": "KLINE",
                "request_mode": "ON_DEMAND",
                "adapter_method": "get_security_bars(category=9)",
                "supported_markets": ["CN_A"],
                "description": "可选通达信行情节点历史日 K 线。",
            },
        ),
        "NEEQ_OFFICIAL": (
            {
                "interface_code": "NEEQ_CAPITAL_RAISE",
                "interface_name": "新三板定增信息（官方）",
                "data_category": "CAPITAL_RAISE",
                "request_mode": "ON_DEMAND",
                "adapter_method": "待接入官方公开页面/API",
                "supported_markets": [MARKET_NEEQ, MARKET_NEEQ_INNOVATION],
                "description": "全国股转系统官方定增信息结构化接口，当前为待接入状态。",
            },
            {
                "interface_code": "NEEQ_MARKET_MAKER",
                "interface_name": "新三板做市商明细（官方）",
                "data_category": "MARKET_MAKER",
                "request_mode": "ON_DEMAND",
                "adapter_method": "待接入官方公开页面/API",
                "supported_markets": [MARKET_NEEQ, MARKET_NEEQ_INNOVATION],
                "description": "全国股转系统官方做市商明细结构化接口，当前为待接入状态。",
            },
            {
                "interface_code": "NEEQ_LAYER_CHANGE",
                "interface_name": "新三板层级变动（官方）",
                "data_category": "LAYER_CHANGE",
                "request_mode": "ON_DEMAND",
                "adapter_method": "待接入官方公开页面/API",
                "supported_markets": [MARKET_NEEQ, MARKET_NEEQ_INNOVATION],
                "description": "全国股转系统官方基础层/创新层/精选层变动接口，当前为待接入状态。",
            },
        ),
    }.items():
        source = sources[source_code]
        if source_code in {"CNINFO_OFFICIAL", "HKEXNEWS_OFFICIAL", "NEEQ_EASTMONEY"}:
            scoped_markets = list((source.config_json or {}).get("market_scope") or [])
            interfaces = tuple(
                {**item, "supported_markets": scoped_markets}
                for item in interfaces
            )
        existing_interfaces = {
            item.interface_code: item
            for item in db.scalars(select(DataInterface).where(DataInterface.source_id == source.id)).all()
        }
        for item in interfaces:
            existing = existing_interfaces.get(item["interface_code"])
            if existing:
                existing.interface_name = item["interface_name"]
                existing.data_category = item["data_category"]
                existing.request_mode = item["request_mode"]
                existing.adapter_method = item["adapter_method"]
                existing.supported_markets = item["supported_markets"]
                existing.description = item["description"]
                # Preserve the operator's interface enable/disable choice.
                continue
            if item["interface_code"] not in existing_interfaces:
                db.add(
                    DataInterface(
                        source_id=source.id,
                        input_schema={},
                        output_schema={},
                        enabled=True,
                        **item,
                    )
                )
    db.commit()


def select_data_source(
    db: Session,
    market: str,
    capability: str,
    fallback_code: str = "AKSHARE",
) -> DataSource | None:
    """Select the highest-priority enabled source advertised for a market/capability."""
    normalized_market = str(market or "").upper()
    normalized_capability = str(capability or "").upper()
    candidates = list(
        db.scalars(
            select(DataSource)
            .where(DataSource.enabled.is_(True))
            .order_by(DataSource.priority, DataSource.source_code)
        ).all()
    )
    category_aliases = {"F10": {"F10", "PROFILE", "HOLDERS_REFERENCE", "NOTICE", "FINANCIAL"}, "FORECAST": {"FORECAST"}, "RESEARCH": {"RESEARCH"}, "QA": {"QA"}}
    wanted_categories = category_aliases.get(normalized_capability, {normalized_capability})

    def has_market_interface(source: DataSource) -> bool:
        rows = db.scalars(select(DataInterface).where(DataInterface.source_id == source.id, DataInterface.enabled.is_(True))).all()
        # Keep compatibility with isolated/unit deployments and older catalog
        # rows created before interface records existed.  Production seeded
        # sources have interfaces, so they still require an exact route.
        if not rows:
            return True
        return any(
            normalized_market in {str(item).upper() for item in (row.supported_markets or [])}
            and (str(row.data_category or "").upper() in wanted_categories or row.interface_code.upper() == normalized_capability)
            for row in rows
        )
    # Prefer a source whose declared route explicitly matches the market.
    for source in candidates:
        config = source.config_json or {}
        markets = {str(item).upper() for item in (config.get("market_scope") or [])}
        capabilities = {str(item).upper() for item in (config.get("capabilities") or [])}
        route = str(config.get("route") or "").upper()
        if (
            normalized_market in markets
            and normalized_capability in capabilities
            and route
            and route in {normalized_market, "NEEQ" if normalized_market in {MARKET_NEEQ, MARKET_NEEQ_INNOVATION} else normalized_market}
        ):
            if has_market_interface(source):
                return source
    for source in candidates:
        config = source.config_json or {}
        markets = {str(item).upper() for item in (config.get("market_scope") or [])}
        capabilities = {str(item).upper() for item in (config.get("capabilities") or [])}
        if normalized_market in markets and normalized_capability in capabilities:
            if has_market_interface(source):
                return source
    # A fallback is only safe when it explicitly advertises both the market
    # and capability.  Returning an arbitrary default (historically AKSHARE)
    # caused HK/NEEQ requests to be routed to an A-share-only or unrelated
    # connector and made missing coverage look like success.
    fallback = db.scalar(select(DataSource).where(DataSource.source_code == fallback_code, DataSource.enabled.is_(True)))
    if fallback:
        config = fallback.config_json or {}
        markets = {str(item).upper() for item in (config.get("market_scope") or [])}
        capabilities = {str(item).upper() for item in (config.get("capabilities") or [])}
        if normalized_market in markets and normalized_capability in capabilities and has_market_interface(fallback):
            return fallback
    return None
