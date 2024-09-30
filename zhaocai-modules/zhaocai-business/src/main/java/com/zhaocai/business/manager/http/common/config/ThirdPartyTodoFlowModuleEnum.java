package com.zhaocai.business.manager.http.common.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ssy
 * @date 2024/8/5 14:59
 */
@Getter
@AllArgsConstructor
public enum ThirdPartyTodoFlowModuleEnum {

    /** 所属待办流程模块 */

    BID_MANAGE("bid_manage", "招标管理"),
    PROCUREMENT_PLAN("procurement_plan", "采购计划"),
    ;

    private final String code;

    private final String desc;

}
