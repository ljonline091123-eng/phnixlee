package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 供应商审批流程类型枚举
 *
 * @author chenming
 * @date 2024/05/30
 */
@Getter
@AllArgsConstructor
public enum VendorProcessTypeEnum {

    VENDOR_REGISTER(1,"供应商注册"),
    VENDOR_UPDATEINFO(2,"供应商信息修改"),
    VENDOR_MOVE_INOROUT_BLACK(3,"供应商黑名单"),
    VENDOR_UPDATE_LEVEL(4,"供应商修改等级")

    ;


    private final Integer state;

    private final String desc;

    public Boolean equalsState(Integer state) {
        return this.state.equals(state);
    }
}
