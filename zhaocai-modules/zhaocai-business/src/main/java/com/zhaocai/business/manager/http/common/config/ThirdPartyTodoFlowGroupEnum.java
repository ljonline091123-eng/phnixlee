package com.zhaocai.business.manager.http.common.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ssy
 * @date 2024/8/5 14:27
 */
@Getter
@AllArgsConstructor
public enum ThirdPartyTodoFlowGroupEnum {

    /** 所属待办流程分组 */

    OPEN_BID("open_bid", "开标待办"),
    EXPERT_EVAL("expert_eval", "评标待办"),
    START_PROCUREMENT_PLAN("start_procurement_plan", "待发起采购计划待办"),
    ;

    private final String code;

    private final String desc;

}
