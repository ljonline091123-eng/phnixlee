package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 采购计划状态枚举
 *
 * @author chenming
 * @date 2024/05/27
 */
@Getter
@AllArgsConstructor
public enum ProcurementPlanStateEnum {

    DRAFT(0,"自由态"),
    SUBMITTED(1,"已完成"),
    CANCELLATION(2,"已作废")

    ;

    /**
     * 状态
     */
    private final Integer state;

    /**
     * 描述
     */
    private final String desc;

    /**
     * 比较状态
     * @param state
     * @return
     */
    public Boolean equalsState(Integer state) {
        return this.getState().equals(state);
    }
}
