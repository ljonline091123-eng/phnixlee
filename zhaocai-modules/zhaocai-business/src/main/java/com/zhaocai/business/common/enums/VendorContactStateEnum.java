package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 供应商联系人状态
 *
 * @author chenming
 * @date 2024/06/04
 */
@Getter
@AllArgsConstructor
public enum VendorContactStateEnum {
    IN_APPROVAL(0,"审批中"),
    VALID(1,"有效的"),
    INVALID(2,"无效"),
    APPROVAL_REJECTION(3,"审批拒绝"),
    ;

    private final Integer state;

    private final String desc;

    public Boolean equalsState(Integer state) {
        return this.state.equals(state);
    }
}
