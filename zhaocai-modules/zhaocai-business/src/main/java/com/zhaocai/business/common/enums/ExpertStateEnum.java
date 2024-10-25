package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 专家状态枚举
 *
 * @author chenming
 * @date 2024/05/30
 */
@Getter
@AllArgsConstructor
public enum ExpertStateEnum {

    SAVE(0,"保存"),
    IN_APPROVAL(1,"审批中"),
    REJECT(2,"审批拒绝"),
    APPROVE(3,"审批通过")

    ;


    private final Integer state;

    private final String desc;

    public Boolean equalsState(Integer state) {
        return this.state.equals(state);
    }
}
