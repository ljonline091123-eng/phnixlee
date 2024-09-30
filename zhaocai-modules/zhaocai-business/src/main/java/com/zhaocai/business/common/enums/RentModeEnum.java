package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 租赁类型枚举
 *
 * @author chenming
 * @date 2024-08-15
 */
@Getter
@AllArgsConstructor
public enum RentModeEnum {

    DAY("1","日"),
    MONTH("2","月"),
    WORK("3","工作量"),

    ;

    private final String model;

    private final String desc;

    public static Boolean isWork(String model) {
        return WORK.getModel().equals(model);
    }
}
