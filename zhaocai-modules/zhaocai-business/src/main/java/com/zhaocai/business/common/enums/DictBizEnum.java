/*
 *      Copyright (c) 2018-2028, Chill Zhuang All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the dreamlu.net developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: Chill 庄骞 (smallchill@163.com)
 */
package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务字典枚举类
 *
 * @author Chill
 */
@Getter
@AllArgsConstructor
public enum DictBizEnum {
	PROCUREMENT_TYPE("procurement_type","采购方式"),
	PROCUREMENT_STATE("procurement_state","采购管理-状态"),
	PROCUREMENT_PLAN_TYPE("procurement_plan_type","采购计划类别"),
	PROCUREMENT_PLAN_STATE("procurement_plan_state","采购计划状态"),
	PROCUREMENT_SCHEME_STATE("procurement_scheme_state","采购方案状态"),
	PROCUREMENT_COUNTING_TYPE("procurement_counting_type","计数方式"),
	PROCUREMENT_PAYMENT_TYPE("procurement_payment_type","付款方式"),
	CONTRACT_BIDDING_STATE("contract_bidding_state","合约规划-招标状态"),
	CONTRACT_BIDDING_RESPONSIBLE_ORG("contract_bidding_responsible_org","合约规划-招标责任单位"),
	CONTRACT_BIDDING_METHOD("contract_bidding_method","合约规划-招标方式"),

	PRICE_TYPE("price_type","价格类型"),

	CURRENCY("currency","币种"),
	EXTERNAL("is_external","是否外部客商"),
	ATTRIBUTE("vendor_attribute","客商属性"),
	VENDORSTATUS("vendor_status","客商状态"),
	TAXPAYER_TYPE("taxpayer_type","增值税纳税人类型"),
	INVOICE_TYPE("invoice_type","发票类型"),
	ENTERPRISE_NATURE("enterprise_nature","企业性质"),
	VENDOR_CLASS("vendor_class","供应商类别"),
	VENDOR_LEVEL("vendor_level","供应商等级"),
	IS_LEGAL("is_legal","是否为法人"),
	CONTACT_STATE("contact_state","联系人状态"),
	IS_MANAGER("is_manager","是否管理员"),

	EXPERT_TYPE("expert_type", "专家类别"),
	EDUCATION_DEGREE("education_degree", "学历"),
	EXPERT_BUSINESS_TYPE("expert_business_type", "专家业态"),
	MARK_ITEM_TYPE("mark_item_type","评分模板项类型"),
	TECHNICAL_TITLES("technical_titles","技术职称"),
	REGISTERED_CERTIFICATE("registered_certificate","执业资格证"),

	ENTERPRISE_TYPE("enterpriseType","企业分类"),

	IS_RECEIVE_DEPOSIT("is_receive_deposit","是否收取保证金"),

	AGREEMENT_STATE("agreement_state","合同状态"),
	AGREEMENT_PAYMENT_CYCLE("payment_cycle","支付周期"),
	AGREEMENT_PAYMENT_WAY("payment_way","支付方式"),
	AGREEMENT_DEPOSIT_TYPE("deposit_type","保证金类型"),
	AGREEMENT_DEPOSIT_WAY("deposit_way","押金/保证金方式"),
	AGREEMENT_IS_RELATED_MY_STEEL("is_related_my_steel","是否关联我的钢铁网价格"),
	AGREEMENT_CURRENT_PAYMENT_POINT("current_payment_point","当前付款节点"),
	AGREEMENT_PAYMENT_TYPE("payment_type","价款类型"),
	AGREEMENT_PAYMENT_BASIS("payment_basis","付款基数"),
	AGREEMENT_DEPOSIT_BASE_AMOUNT("deposit_base_amount","保证金基数"),
	AGREEMENT_RENTAL_TYPE("rental_type","租赁方式"),
	AGREEMENT_RENTAL_METHOD("rental_Method","计租方式"),
	AGREEMENT_PRICE_FORM("price_form","价格形式"),
	AGREEMENT_STAMPER_TYPE("agreement_stamper_type","合同签章签署类型"),

	TEMPLATE_TYPE("template_type","模板分类"),

	UNDERLING_PAYMENT_CYCLE("PAYMENT_CYCLE","支付周期"),
	UNDERLING_PAYMENT_TYPE("PAYMENT_TYPE","支付方式"),
	UNDERLING_PRICE_FORM("PRICE_FORM","价格形式"),
	UNDERLING_SYS_CURRENCY("SYS_CURRENCY","币种"),
	UNDERLING_INVOICE_TYPE("INVOICE_TYPE","发票类型"),
	UNDERLING_PAYMENT_BASE_TYPE("PAYMENT_BASE_TYPE","付款基数"),
	UNDERLING_DEPOSIT_TYPE("DEPOSIT_TYPE","押金/保证金类型"),
	UNDERLING_DEPOSIT_MODE("DEPOSIT_MODE","押金/保证金方式"),
	UNDERLING_DEPOSIT_BASE_TYPE("DEPOSIT_BASE_TYPE","押金/保证金基数"),
	UNDERLING_DATALLER_WORK_TYPE("DATALLER_WORK_TYPE","工种"),
	UNDERLING_RENT_MODE("RENT_MODE","租赁方式"),
	UNDERLING_RENT_UNIT("RENT_UNIT","计租单位"),
	UNDERLING_RENT_TYPE("RENT_TYPE","计租方式"),
	UNDERLING_SETTLE_STATUS("SETTLE_STATUS","结算状态"),
	UNDERLING_MTR_MACH_TYPE("MTR_MACH_TYPE","台班类型"),
	UNDERLING_PROJECT_FORMAT("PROJECT_FORMAT","项目业态"),
	UNDERLING_CONTRACT_TYPE("CONTRACT_TYPE","合同/合约类型"),
	;

	private final String name;

	private final String desc;

}
