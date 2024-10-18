package com.zhaocai.business.common.enums;

import com.zhaocai.business.bidding.enums.TenderNoticeStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * @author ssy
 * @date 2024/8/11 10:27
 */
@Getter
@AllArgsConstructor
public enum ApproveFlowPromptTemplateEnum {

    /**
     * 1、采购方案审批流程
     * 获取方案名称（如：桩基工程专业分包（二期））
     * 模板：您有采购方案：桩基工程专业分包（二期）在审核节点的审批
     *
     * 2、招标管理定标
     * 模板：您有招标管理定标：桩基工程专业分包（二期）在审核节点的审批
     *
     * 3、招标管理开标
     * 模板：您有一条桩基工程专业分包（二期）开标信息待确认，请及时处理
     *
     * 4、评标
     * 模板：您有一条桩基工程专业分包（二期）评标工作待处理，请及时处理
     *
     * 5、合同审批（获取合同名称）
     * 模板：您有合同签订：桩基工程专业分包（二期）在审核节点的审批
     *
     * 6、供应商注册审批
     * 模板：您有供应商（带上供应商名称）注册在审核节点的审批
     *
     * 7、供应商修改信息审批
     * 模板：您有供应商（带上供应商名称）信息修改在审核节点的审批！
     *
     * 8、供应商移入移出黑名单审批
     * 模板：您有供应商（带上供应商名称）移入移出黑名单在审核节点的审批！
     * 推送给主控端的展示信息需按上述模板修改下
     */

    PROCUREMENT_SCHEME("procurement_scheme", "您有采购方案：%s在审核节点的审批"),
    BID_CALIBRATION("bid_calibration", "您有招标管理定标：%s在审核节点的审批"),
    BID_OPEN("bid_open", "您有一条%s开标信息待确认，请及时处理"),
    BID_EVAL("bid_eval", "您有一条%s评标工作待处理，请及时处理"),
    CONTRACT_APPROVE("contract_approve", "您有合同签订：%s在审核节点的审批"),
    VENDOR_REGISTER_APPROVE("vendor_register_approve", "您有供应商（%s）注册在审核节点的审批"),
    VENDOR_CHANGE_APPROVE("vendor_change_approve", "您有供应商（%s）信息修改在审核节点的审批！"),
    VENDOR_BLACKLIST_APPROVE("vendor_blacklist_approve", "您有供应商（%s）移入移出黑名单在审核节点的审批！"),
    PROCUREMENT_PLAN_PUSH("procurement_plan_push", "您有一条（%s）合约规划，请及时发起采购计划"),
    PROCUREMENT_PLAN_NOTICE_PUSH("procurement_plan_notice_push", "发送人:%s，{最小核算项目=(%s)}，合同类型为{%s (%s)}合同将于近期开展，请您及时关注了解，采购计划如下：" +
            "                                                                           \n %s 招标时间为%s，进场时间为%s、采购人为%s，区域为%s"),


    ;

    private final String code;

    private final String desc;

}
