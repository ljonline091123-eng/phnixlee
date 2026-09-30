"""Versioned taxonomy master data and human-readable definitions."""

from __future__ import annotations

import unicodedata
from typing import Any

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.taxonomy import ClassificationDefinition


# Label-level master data takes precedence over dimension-level descriptions.
# Provider feeds often put industry hierarchy names in a ``BOARD`` field, so
# a dimension-only fallback such as "交易市场或交易所板块" is misleading for
# labels like 房地产开发.  These definitions describe the observed label and
# keep the provider taxonomy as provenance rather than silently asserting an
# official regulatory classification.
LABEL_DEFINITION_OVERRIDES = (
    dict(dimension="INDUSTRY", code="LABEL_REAL_ESTATE_INDUSTRY", label="房地产业", definition="以房地产开发经营、物业服务、房地产租赁及相关服务为主要业务的行业分类。", criteria="按来源行业字段归类；不代表公司全部收入均来自该行业。"),
    dict(dimension="INDUSTRY", code="LABEL_BANKING", label="银行业", definition="以吸收存款、发放贷款、支付结算及其他银行金融服务为主要业务的行业分类。", criteria="按来源行业字段归类；不构成对单只证券的投资建议。"),
    dict(dimension="INDUSTRY", code="LABEL_NON_BANK_FINANCE", label="非银金融", definition="以证券、保险、信托、期货或其他非银行金融服务为主要业务的行业分类。", criteria="按来源行业字段归类；具体子行业以来源版本为准。"),
    dict(dimension="INDUSTRY", code="LABEL_COMPUTER", label="计算机", definition="以计算机设备、软件、信息技术服务或相关数字化业务为主的行业分类。", criteria="按来源行业字段归类；不由概念名称推断经营因果。"),
    dict(dimension="INDUSTRY", code="LABEL_ELECTRONICS", label="电子", definition="以电子元器件、电子设备、集成电路或相关制造服务为主的行业分类。", criteria="按来源行业字段归类；具体业务以公司披露为准。"),
    dict(dimension="INDUSTRY", code="LABEL_PHARMACEUTICAL", label="医药生物", definition="以药品、医疗器械、生物技术或医疗服务相关业务为主的行业分类。", criteria="按来源行业字段归类；不等同于单一产品疗效判断。"),
    dict(dimension="INDUSTRY", code="LABEL_DEFENSE", label="国防军工", definition="以军工装备、国防科研生产或军民融合相关产品和服务为主的行业分类。", criteria="按来源行业字段归类；具体资质和订单以公开披露为准。"),
    dict(dimension="INDUSTRY", code="LABEL_NEW_ENERGY", label="电力设备", definition="以电力设备、电网设备、电池、储能或相关能源装备为主的行业分类。", criteria="按来源行业字段归类；不代表证券必然属于某一主题。"),
    dict(dimension="INDUSTRY", code="LABEL_AUTOMOBILE", label="汽车", definition="以整车、汽车零部件、汽车服务或相关出行产品为主的行业分类。", criteria="按来源行业字段归类；具体产品范围以来源版本为准。"),
    dict(dimension="INDUSTRY", code="LABEL_FOOD_BEVERAGE", label="食品饮料", definition="以食品、饮料、调味品或相关消费品生产经营为主的行业分类。", criteria="按来源行业字段归类；品牌标签与行业分类分开保存。"),
    dict(dimension="BOARD", code="LABEL_BOARD_REAL_ESTATE", label="房地产", definition="按房地产开发、经营、服务及相关产业链成分股编制的行业板块，不是交易所上市板块。", criteria="保留东方财富板块代码和采集时点，板块成分随来源调整。"),
    dict(dimension="BOARD", code="LABEL_BOARD_REAL_ESTATE_DEV", label="房地产开发", definition="按住宅、商业或综合不动产开发、销售及配套运营企业归集的细分行业板块。", criteria="按来源板块成分归集，不根据股票名称推断成员资格。"),
    dict(dimension="BOARD", code="LABEL_BOARD_RESIDENTIAL_DEV", label="住宅开发", definition="按住宅项目开发、建设、销售及相关运营企业归集的细分行业板块。", criteria="按来源板块成分归集，成员和走势以采集快照为准。"),
    dict(dimension="BOARD", code="LABEL_BOARD_SZ_MAIN", label="深交所主板", definition="深圳证券交易所主板上市板块，采用深交所主板的上市、交易和信息披露规则。", criteria="依据证券主数据中的交易所和上市板块字段确定。"),
    dict(dimension="BOARD", code="LABEL_BOARD_SH_MAIN", label="上交所主板", definition="上海证券交易所主板上市板块，采用上交所主板的上市、交易和信息披露规则。", criteria="依据证券主数据中的交易所和上市板块字段确定。"),
    dict(dimension="BOARD", code="LABEL_BOARD_CHINEXT", label="创业板", definition="深圳证券交易所创业板上市板块，面向成长型创新创业企业并适用相应交易规则。", criteria="依据证券主数据中的交易所和上市板块字段确定。"),
    dict(dimension="BOARD", code="LABEL_BOARD_STAR", label="科创板", definition="上海证券交易所科创板上市板块，面向符合定位的科技创新企业并适用相应交易规则。", criteria="依据证券主数据中的交易所和上市板块字段确定。"),
    dict(dimension="BOARD", code="LABEL_BOARD_BSE", label="北交所", definition="北京证券交易所上市板块，服务创新型中小企业并适用北交所上市交易规则。", criteria="依据证券主数据中的交易所和上市板块字段确定。"),
    dict(dimension="THEME", code="LABEL_THEME_SHENZHEN_SEZ", label="深圳特区", definition="与深圳经济特区区域发展、地方产业政策或深圳区域经营主体相关的来源主题。", criteria="仅表示来源主题标签，不代表公司全部业务或政策因果。"),
    dict(dimension="THEME", code="LABEL_THEME_ELDERLY", label="养老概念", definition="涉及养老服务、康养地产、养老金融、医疗照护或相关设施运营的来源主题。", criteria="以来源明确列示的成分或业务证据为准。"),
    dict(dimension="THEME", code="LABEL_THEME_SMART_HOME", label="智能家居", definition="涉及家庭物联网、智能家电、家居控制系统或相关软硬件产品的来源主题。", criteria="来源标签不等同于公司全部收入来自智能家居。"),
    dict(dimension="THEME", code="LABEL_THEME_SUPER_BRAND", label="超级品牌", definition="来源机构按品牌知名度、市场影响力或消费认知整理的品牌主题，不是监管分类。", criteria="保留来源标签和代码，不将品牌标签升级为经营事实。"),
    dict(dimension="THEME", code="LABEL_THEME_RENTAL_EQUALITY", label="租售同权", definition="与住房租赁服务、租赁权益保障及相关城市住房政策方向有关的来源主题。", criteria="仅表示主题关联，不直接证明公司已获得政策收益。"),
    dict(dimension="THEME", code="LABEL_THEME_ASSEMBLED_BUILDING", label="装配建筑", definition="采用预制部品、模块化施工或工业化建造方式的建筑产业来源主题。", criteria="以来源成分和公司披露为准，不由名称推断订单。"),
    dict(dimension="THEME", code="LABEL_THEME_REITS", label="REITs概念", definition="与基础设施或不动产投资信托基金设立、运营、资产管理或相关服务有关的来源主题。", criteria="主题标签不代表证券已发行或持有REITs产品。"),
    dict(dimension="THEME", code="LABEL_THEME_AH", label="AH股", definition="同一发行主体同时在境内A股和香港H股市场挂牌交易的证券关系标签。", criteria="需结合公司主体和跨市场证券映射确认，不按名称推断。"),
    dict(dimension="THEME", code="LABEL_THEME_MARGIN", label="融资融券", definition="证券被纳入融资融券业务标的或与融资融券交易机制有关的来源标签。", criteria="标签不等同于当日融资余额变化或资金方向。"),
    dict(dimension="THEME", code="LABEL_THEME_SHENZHEN_CONNECT", label="深股通", definition="证券符合深港股票市场交易互联互通机制下深股通投资范围的来源标签。", criteria="以交易所或来源名单为准，不代表北向资金当日净流入。"),
    dict(dimension="THEME", code="LABEL_THEME_BROKEN_BOOK", label="破净股", definition="按来源采集时点证券市场价格低于每股净资产的估值状态标签。", criteria="该状态随价格和财务报告变化，不代表未来收益或风险结论。"),
    dict(dimension="THEME", code="LABEL_THEME_MID_CAP", label="中盘股", definition="按来源口径以总市值或流通市值区间划分的中等规模证券主题。", criteria="必须结合来源口径和采集日期，不是永久固定属性。"),
    dict(dimension="THEME", code="LABEL_THEME_MID_VALUE", label="中盘价值", definition="按来源将中等市值规模与相对价值特征组合识别的风格主题。", criteria="风格标签不等同于法定行业或投资建议。"),
    dict(dimension="THEME", code="LABEL_THEME_LOW_PB", label="低市净率", definition="按来源采集时点市净率处于较低区间的估值风格主题。", criteria="应同时记录估值时点和计算口径，不代表未来收益。"),
    dict(dimension="THEME", code="LABEL_THEME_LARGE_VALUE", label="大盘价值", definition="按来源将较大市值规模与相对价值特征组合识别的投资风格主题，通常关注规模较大的公司及其估值、盈利或现金流特征。", criteria="必须同时保留来源代码、采集日期和市值/估值口径；主题标签不等同于法定行业或投资建议。"),
    dict(dimension="THEME", code="LABEL_THEME_LARGE_CAP", label="大盘股", definition="按来源总市值或流通市值处于较高区间归集的证券主题，成员会随市值和来源阈值变化。", criteria="以来源快照的市值字段、分位或阈值为准，不把大盘标签视为永久属性。"),
    dict(dimension="THEME", code="LABEL_THEME_VALUE", label="价值股", definition="按来源将相对估值较低、盈利或现金流相对稳定、分红或资产基础受到关注的证券归集的风格主题。", criteria="应记录估值指标、报告期和来源规则；价值标签不保证未来收益，也不替代财务分析。"),
    dict(dimension="THEME", code="LABEL_THEME_INSTITUTIONAL_HOLDING", label="机构重仓", definition="按来源统计期内被基金、保险、社保、券商或其他机构持有，且持仓规模或机构覆盖达到来源口径的证券主题。", criteria="必须保留统计期、机构类型、持股数量或比例及来源；不得仅凭股票名称推断机构持仓。"),
    dict(dimension="THEME", code="LABEL_THEME_CROSS_BORDER_PAYMENT", label="跨境支付", definition="与跨境收付款、国际结算、清算网络、外汇支付或为跨境交易提供支付技术和服务有关的来源主题。", criteria="仅表示来源主题关联，具体业务范围和收入贡献以公司公告、年报或业务数据为准。"),
    dict(dimension="THEME", code="LABEL_THEME_BLOCKCHAIN", label="区块链", definition="与分布式账本、区块链底层技术、数字身份、智能合约或区块链行业应用有关的来源主题。", criteria="需以来源成分名单或公司披露为证据；概念标签不证明已形成规模化收入或业务落地。"),
    dict(dimension="THEME", code="LABEL_THEME_INTERNET_FINANCE", label="互联网金融", definition="利用互联网或数字平台开展支付、信贷、财富管理、保险、证券信息服务或金融科技基础设施相关业务的来源主题。", criteria="需区分金融机构牌照业务、技术服务和概念关联，不能由名称直接推断经营范围。"),
)


LABEL_DEFINITION_VERSION = "LABEL_GLOSSARY_V1"
LABEL_DEFINITION_SOURCE = "系统分类标签释义"


def _normalized_label(value: Any) -> str:
    """Normalize provider labels without changing their user-facing text."""
    return unicodedata.normalize("NFKC", str(value or "")).strip().casefold()


def _label_definition_record(item: dict[str, Any]) -> dict[str, Any]:
    """Return a complete, persistence-ready label definition record."""
    record = dict(item)
    record.setdefault("taxonomy", "CN_SECURITIES")
    record.setdefault("source_name", LABEL_DEFINITION_SOURCE)
    record.setdefault("definition_version", LABEL_DEFINITION_VERSION)
    record.setdefault("jurisdiction", "CN")
    return record


def label_definition(label: str | None, dimension: str | None = None) -> dict[str, Any] | None:
    """Return a concrete, label-level definition when one is registered.

    The lookup deliberately includes the classification dimension.  A label
    such as ``房地产`` can be a provider industry or a thematic board, and
    those meanings must not be silently merged.
    """
    normalized_label = _normalized_label(label)
    normalized_dimension = str(dimension or "").upper()
    for item in LABEL_DEFINITION_OVERRIDES:
        if (_normalized_label(item["label"]) == normalized_label
                and (not normalized_dimension or item["dimension"] == normalized_dimension)):
            return _label_definition_record(item)
    # A dimension-free lookup is useful for legacy callers.  When a caller
    # supplies an unknown dimension, however, do not return a definition from
    # another dimension and silently give the label the wrong meaning.
    if not normalized_dimension:
        for item in LABEL_DEFINITION_OVERRIDES:
            if _normalized_label(item["label"]) == normalized_label:
                return _label_definition_record(item)
    return None


# Provider concepts remain observations and are not silently treated as legal industries.
BUILTIN_DEFINITIONS = (
    dict(taxonomy="CN_SECURITIES", dimension="INDUSTRY", code="CSRC_LEVEL1", label="\u8bc1\u76d1\u4f1a\u884c\u4e1a\uff08\u4e00\u7ea7\uff09", definition="\u6309\u4e2d\u56fd\u8bc1\u76d1\u4f1a\u4e0a\u5e02\u516c\u53f8\u884c\u4e1a\u5206\u7c7b\u6807\u51c6\uff0c\u6839\u636e\u53d1\u884c\u4eba\u4e3b\u8425\u4e1a\u52a1\u5f52\u5165\u4e00\u7ea7\u884c\u4e1a\u3002", criteria="\u4ee5\u6765\u6e90\u673a\u6784\u63d0\u4f9b\u7684\u5206\u7c7b\u548c\u7248\u672c\u4e3a\u51c6\uff0c\u65e0\u8bc1\u636e\u65f6\u4e0d\u63a8\u65ad\u3002", source_name="\u4e2d\u56fd\u8bc1\u76d1\u4f1a\u884c\u4e1a\u5206\u7c7b\u6807\u51c6", definition_version="CSRC_CURRENT_V1", jurisdiction="CN"),
    dict(taxonomy="CN_SECURITIES", dimension="INDUSTRY", code="EASTMONEY_LEVEL1", label="\u6765\u6e90\u884c\u4e1a\uff08\u4e00\u7ea7\uff09", definition="\u6570\u636e\u63d0\u4f9b\u65b9\u6807\u6ce8\u7684\u884c\u4e1a\u540d\u79f0\uff0c\u53cd\u6620\u8bc1\u5238\u5728\u8be5\u6765\u6e90\u4e2d\u7684\u5f52\u5c5e\uff0c\u4e0d\u7b49\u4e8e\u5b98\u65b9\u5206\u7c7b\u6216\u6295\u8d44\u5efa\u8bae\u3002", criteria="\u4fdd\u7559\u6765\u6e90\u539f\u59cb\u4ee3\u7801\u548c\u7248\u672c\uff0c\u4e0d\u540c\u6765\u6e90\u540c\u540d\u4e0d\u81ea\u52a8\u5408\u5e76\u3002", source_name="\u4e1c\u65b9\u8d22\u5bcc\u516c\u5f00\u8bc1\u5238\u8d44\u6599", definition_version="EASTMONEY_CURRENT_V1", jurisdiction="CN"),
    dict(taxonomy="CN_SECURITIES", dimension="THEME", code="PROVIDER_CONCEPT", label="\u6765\u6e90\u4e3b\u9898\uff0f\u6982\u5ff5", definition="\u6570\u636e\u63d0\u4f9b\u65b9\u6839\u636e\u516c\u53f8\u4e1a\u52a1\u3001\u4ea7\u54c1\u3001\u516c\u544a\u6216\u5e02\u573a\u7ea6\u5b9a\u6574\u7406\u7684\u6295\u8d44\u4e3b\u9898\u96c6\u5408\uff0c\u4e00\u53ea\u8bc1\u5238\u53ef\u540c\u65f6\u5c5e\u4e8e\u591a\u4e2a\u4e3b\u9898\u3002\u5b83\u4e0d\u662f\u6cd5\u5b9a\u884c\u4e1a\uff0c\u4e5f\u4e0d\u8868\u793a\u516c\u53f8\u5168\u90e8\u6536\u5165\u6765\u81ea\u8be5\u4e3b\u9898\u3002", criteria="\u4ec5\u5c55\u793a\u6765\u6e90\u660e\u786e\u7ed9\u51fa\u7684\u4e3b\u9898\u53ca\u4ee3\u7801\uff0c\u4e0d\u6839\u636e\u4e3b\u9898\u540d\u79f0\u63a8\u65ad\u56e0\u679c\u3002", source_name="\u4e1c\u65b9\u8d22\u5bcc\u516c\u5f00\u8bc1\u5238\u8d44\u6599", definition_version="EASTMONEY_THEME_V1", jurisdiction="CN"),
    dict(taxonomy="CN_SECURITIES", dimension="TYPE", code="BLUE_CHIP", label="\u84dd\u7b79\u80a1", definition="\u901a\u5e38\u6307\u7ecf\u8425\u7a33\u5b9a\u3001\u89c4\u6a21\u8f83\u5927\u3001\u76c8\u5229\u548c\u6cbb\u7406\u8bb0\u5f55\u76f8\u5bf9\u6210\u719f\u7684\u4e0a\u5e02\u516c\u53f8\u80a1\u7968\u3002\u8be5\u79f0\u8c13\u6ca1\u6709\u7edf\u4e00\u6cd5\u5b9a\u9608\u503c\uff0c\u5fc5\u987b\u6ce8\u660e\u540d\u5355\u6216\u89c4\u5219\u3002", criteria="\u4ec5\u5728\u6709\u6765\u6e90\u540d\u5355\u6216\u5df2\u5ba1\u6838\u7684\u7248\u672c\u5316\u89c4\u5219\u65f6\u63a5\u53d7\u3002", source_name="\u7cfb\u7edf\u5206\u7c7b\u8bcd\u5178", definition_version="TYPE_GLOSSARY_V1", jurisdiction="GLOBAL"),
    dict(taxonomy="CN_SECURITIES", dimension="TYPE", code="RED_CHIP", label="\u7ea2\u7b79\u80a1", definition="\u901a\u5e38\u6307\u5728\u9999\u6e2f\u4e0a\u5e02\u3001\u7531\u4e2d\u56fd\u5185\u5730\u653f\u5e9c\u6216\u4f01\u4e1a\u63a7\u5236\u4e14\u4e3b\u8981\u4e1a\u52a1\u4e0e\u4e2d\u56fd\u5185\u5730\u76f8\u5173\u7684\u516c\u53f8\u80a1\u7968\u3002\u5177\u4f53\u8d44\u683c\u4ee5\u4ea4\u6613\u6240\u548c\u6765\u6e90\u53e3\u5f84\u4e3a\u51c6\u3002", criteria="\u9700\u6709\u4ea4\u6613\u6240\u5c5e\u6027\u53ca\u63a7\u5236\u6216\u4e1a\u52a1\u8bc1\u636e\uff0c\u4e0d\u51ed\u540d\u79f0\u8ba4\u5b9a\u3002", source_name="\u7cfb\u7edf\u5206\u7c7b\u8bcd\u5178", definition_version="TYPE_GLOSSARY_V1", jurisdiction="HK"),
    dict(taxonomy="CN_SECURITIES", dimension="SIZE", code="LARGE_CAP", label="\u5927\u76d8\u80a1", definition="\u6309\u6307\u5b9a\u65e5\u671f\u548c\u5e02\u503c\u53e3\u5f84\u5212\u5206\u7684\u8f83\u5927\u89c4\u6a21\u8bc1\u5238\uff0c\u4e0d\u662f\u5168\u5e02\u573a\u5206\u4f4d\u6392\u540d\u3002", criteria="\u5fc5\u987b\u8bb0\u5f55\u5e02\u503c\u6765\u6e90\u3001\u65f6\u70b9\u3001\u5e01\u79cd\u548c\u9608\u503c\u7248\u672c\u3002", source_name="\u7cfb\u7edf\u5e02\u503c\u5206\u7c7b\u89c4\u5219", definition_version="SIZE_ABSOLUTE_CNY_V1", jurisdiction="CN"),
    dict(taxonomy="CN_SECURITIES", dimension="SIZE", code="MID_CAP", label="\u4e2d\u76d8\u80a1", definition="\u6309\u6307\u5b9a\u5e02\u503c\u53e3\u5f84\u5212\u5206\u7684\u4e2d\u7b49\u89c4\u6a21\u8bc1\u5238\uff0c\u4e0d\u662f\u884c\u4e1a\u6216\u98ce\u683c\u6807\u7b7e\u3002", criteria="\u4f7f\u7528\u7248\u672c\u5316\u7684\u7edd\u5bf9\u5e02\u503c\u533a\u95f4\u5e76\u4fdd\u7559\u5feb\u7167\u8bc1\u636e\u3002", source_name="\u7cfb\u7edf\u5e02\u503c\u5206\u7c7b\u89c4\u5219", definition_version="SIZE_ABSOLUTE_CNY_V1", jurisdiction="CN"),
    dict(taxonomy="CN_SECURITIES", dimension="SIZE", code="SMALL_CAP", label="\u5c0f\u76d8\u80a1", definition="\u6309\u6307\u5b9a\u5e02\u503c\u53e3\u5f84\u5212\u5206\u7684\u8f83\u5c0f\u89c4\u6a21\u8bc1\u5238\uff0c\u4e0d\u662f\u5bf9\u672a\u6765\u6536\u76ca\u6216\u98ce\u9669\u7684\u5224\u65ad\u3002", criteria="\u4f7f\u7528\u7248\u672c\u5316\u7684\u7edd\u5bf9\u5e02\u503c\u533a\u95f4\u5e76\u4fdd\u7559\u5feb\u7167\u8bc1\u636e\u3002", source_name="\u7cfb\u7edf\u5e02\u503c\u5206\u7c7b\u89c4\u5219", definition_version="SIZE_ABSOLUTE_CNY_V1", jurisdiction="CN"),
    dict(taxonomy="CN_SECURITIES", dimension="LEGAL_LISTING_CLASS", code="A_SHARE", label="A\u80a1", definition="\u5728\u4e2d\u56fd\u5185\u5730\u8bc1\u5238\u4ea4\u6613\u6240\u4e0a\u5e02\u3001\u4ee5\u4eba\u6c11\u5e01\u4ea4\u6613\u7684\u666e\u901a\u80a1\u7c7b\u522b\u3002", criteria="\u6839\u636e\u8bc1\u5238\u5e02\u573a\u548c\u4ea4\u6613\u6240\u4e3b\u6570\u636e\u786e\u5b9a\u3002", source_name="\u8bc1\u5238\u4e3b\u6570\u636e", definition_version="LISTING_CLASS_V1", jurisdiction="CN"),
    dict(taxonomy="CN_SECURITIES", dimension="LEGAL_LISTING_CLASS", code="H_SHARE", label="H\u80a1", definition="\u5728\u9999\u6e2f\u4ea4\u6613\u6240\u4e0a\u5e02\u3001\u7531\u4e2d\u56fd\u5185\u5730\u6ce8\u518c\u516c\u53f8\u53d1\u884c\u7684\u5883\u5916\u4e0a\u5e02\u80a1\u4efd\u7c7b\u522b\u3002", criteria="\u6839\u636e\u5e02\u573a\u3001\u53d1\u884c\u4e3b\u4f53\u548c\u4ea4\u6613\u6240\u4e3b\u6570\u636e\u786e\u5b9a\u3002", source_name="\u8bc1\u5238\u4e3b\u6570\u636e", definition_version="LISTING_CLASS_V1", jurisdiction="HK"),
    dict(taxonomy="CN_SECURITIES", dimension="BOARD", code="LISTED_BOARD", label="\u4e0a\u5e02\u677f\u5757", definition="\u8bc1\u5238\u6240\u5c5e\u7684\u4ea4\u6613\u5e02\u573a\u6216\u4ea4\u6613\u6240\u677f\u5757\uff0c\u7528\u4e8e\u533a\u5206\u4e0a\u5e02\u5730\u70b9\u548c\u4ea4\u6613\u89c4\u5219\u3002", criteria="\u4ec5\u5c55\u793a\u8bc1\u5238\u4e3b\u6570\u636e\u6216\u5b98\u65b9\u8d44\u6599\u660e\u786e\u63d0\u4f9b\u7684\u677f\u5757\uff0c\u4e0d\u6839\u636e\u540d\u79f0\u731c\u6d4b\u3002", source_name="\u8bc1\u5238\u4e3b\u6570\u636e", definition_version="LISTED_BOARD_V1", jurisdiction="GLOBAL"),
    dict(taxonomy="CN_SECURITIES", dimension="INDEX", code="INDEX_MEMBERSHIP", label="\u6307\u6570\u6210\u5206", definition="\u8bc1\u5238\u88ab\u67d0\u4e2a\u6307\u6570\u7f16\u5236\u65b9\u6216\u6570\u636e\u63d0\u4f9b\u65b9\u5217\u4e3a\u6307\u6570\u6210\u5206\uff0c\u53cd\u6620\u6307\u6570\u7f16\u5236\u8303\u56f4\uff0c\u4e0d\u7b49\u4e8e\u4e2a\u80a1\u6295\u8d44\u5efa\u8bae\u3002", criteria="\u4fdd\u7559\u6307\u6570\u540d\u79f0\u3001\u6765\u6e90\u548c\u8bc1\u636e\u65e5\u671f\uff0c\u4e0d\u4e0e\u884c\u4e1a\u6216\u4e3b\u9898\u5408\u5e76\u3002", source_name="\u6307\u6570\u7f16\u5236\u65b9\u6216\u8bc1\u5238\u6570\u636e\u63d0\u4f9b\u65b9", definition_version="INDEX_MEMBERSHIP_V1", jurisdiction="GLOBAL"),
    dict(taxonomy="CN_SECURITIES", dimension="TYPE", code="SECURITY_TYPE", label="\u8bc1\u5238\u7c7b\u578b", definition="\u8bc1\u5238\u7684\u4ea7\u54c1\u6216\u5e02\u573a\u7c7b\u578b\uff0c\u4f8b\u5982\u666e\u901a\u80a1\u3001\u57fa\u91d1\u6216\u5176\u4ed6\u4ea4\u6613\u54c1\u79cd\u3002", criteria="\u4ec5\u5728\u8bc1\u5238\u4e3b\u6570\u636e\u6216\u4ea4\u6613\u6240\u5c5e\u6027\u660e\u786e\u65f6\u5c55\u793a\u3002", source_name="\u8bc1\u5238\u4e3b\u6570\u636e", definition_version="SECURITY_TYPE_V1", jurisdiction="GLOBAL"),
)


def seed_builtin_definitions(db: Session) -> int:
    created = 0
    seed_items = tuple(BUILTIN_DEFINITIONS) + tuple(
        _label_definition_record(item) for item in LABEL_DEFINITION_OVERRIDES
    )
    for item in seed_items:
        row = db.scalar(select(ClassificationDefinition).where(
            ClassificationDefinition.taxonomy == item["taxonomy"], ClassificationDefinition.dimension == item["dimension"],
            ClassificationDefinition.code == item["code"], ClassificationDefinition.definition_version == item["definition_version"]))
        if row is None:
            db.add(ClassificationDefinition(
                **item,
                status="ACTIVE",
                properties_json={},
            ))
            created += 1
    if created:
        db.flush()
    return created


def definition_read(row: ClassificationDefinition) -> dict:
    return {"id": row.id, "taxonomy": row.taxonomy, "dimension": row.dimension, "code": row.code,
            "label": row.label, "definition": row.definition, "criteria": row.criteria,
            "parent_code": row.parent_code, "jurisdiction": row.jurisdiction, "source_name": row.source_name,
            "source_url": row.source_url, "definition_version": row.definition_version, "status": row.status,
            "valid_from": row.valid_from, "valid_to": row.valid_to, "properties_json": row.properties_json}


def list_definitions(db: Session, *, dimension: str | None = None, taxonomy: str | None = None,
                     code: str | None = None, q: str | None = None, active_only: bool = True) -> list[dict]:
    query = select(ClassificationDefinition)
    if dimension:
        query = query.where(ClassificationDefinition.dimension == dimension.upper())
    if taxonomy:
        query = query.where(ClassificationDefinition.taxonomy == taxonomy)
    if code:
        query = query.where(ClassificationDefinition.code == code)
    if q:
        query = query.where(ClassificationDefinition.label.contains(q, autoescape=True))
    if active_only:
        query = query.where(ClassificationDefinition.status == "ACTIVE")
    rows = db.scalars(query.order_by(ClassificationDefinition.dimension, ClassificationDefinition.label)).all()
    return [definition_read(row) for row in rows]


def get_definition(db: Session, definition_id: str) -> dict:
    row = db.get(ClassificationDefinition, definition_id)
    if row is None:
        from app.services.foundation import FoundationError
        raise FoundationError("classification definition not found", 404)
    return definition_read(row)


def get_definition(db: Session, definition_id: str) -> dict:
    row = db.get(ClassificationDefinition, definition_id)
    if row is None or row.status != "ACTIVE":
        from app.services.foundation import FoundationError
        raise FoundationError("taxonomy definition not found", 404)
    return definition_read(row)
