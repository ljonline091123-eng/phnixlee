package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProcessKeyEnum {

    /** 流程Key枚举 */
    ZHAOCAI_EXPERT_ADD("jiantou-zhaocai:{org}:ZHAOCAI_EXPERT_ADD","新增专家"),

    ZHAOCAI_VENDOR_REGISTER("jiantou-zhaocai:{org}:ZHAOCAI_VENDOR_REGISTER","供应商注册"),
    ZHAOCAI_VENDOR_ADDCONTACT("jiantou-zhaocai:{org}:ZHAOCAI_VENDOR_ADDCONTACT","供应商-新增联系人"),

    ZHAOCAI_VENDOR_UPDATEINFO("jiantou-zhaocai:{org}:ZHAOCAI_VENDOR_UPDATEINFO","供应商-修改信息"),

    ZHAOCAI_VENDOR_UPDATE_CERTIFICATION("jiantou-zhaocai:{org}:ZHAOCAI_VENDOR_UPDATE_CERTIFICATION","供应商-修改资质"),

    ZHAOCAI_VENDOR_MOVE_INOROUT_BLACK("jiantou-zhaocai:{org}:ZHAOCAI_VENDOR_MOVE_INOROUT_BLACK","供应商移入移出黑名单"),

    ZHAOCAI_PROCUREMENT_SCHEME("jiantou-zhaocai:{org}:ZHAOCAI_PROCUREMENT_SCHEME","采购方案"),


    ZHAOCAI_TENDER_CALIBRATE("jiantou-zhaocai:{org}:ZHAOCAI_TENDER_CALIBRATE","招标管理-定标"),


    ZHAOCAI_AGREEMENT_SIGN("jiantou-zhaocai:{org}:ZHAOCAI_AGREEMENT_SIGN","合同签订"),

    ZHAOCAI_VENDOR_UPDATE_LEVEL("jiantou-zhaocai:{org}:ZHAOCAI_VENDOR_UPDATE_LEVEL","供应商-修改等级");

    /**
     * 流程标识
     */
    private final String identifying;
    /**
     * 描述
     */
    private final String desc;
}
