package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 专家审批流程类型枚举
 *
 * @author chenming
 * @date 2024/05/30
 */
@Getter
@AllArgsConstructor
public enum ExpertProcessTypeEnum {

    EXPERT_ADD(1,"专家新增"),
    EXPERT_CHANGE(2,"专家修改"),

    ;


    private final Integer state;

    private final String desc;

    public Boolean equalsState(Integer state) {
        return this.state.equals(state);
    }
}
