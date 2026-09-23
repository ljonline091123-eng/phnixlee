package com.zhaocai.business.receipt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * @author cff
 * @date 2024/09/11
 */
@Getter
@AllArgsConstructor
public enum OperationalStateEnum {

    /**
     * 操作状态
     */
    OPERATIONAL_STATE_ONE("2", "待确认"),
    OPERATIONAL_STATE_TWO("3", "已确认"),
    ;

    private final String state;

    private final String desc;


    /**
     * 根据编码查询枚举
     *
     * @param state
     * @return
     */
    public static String getValueByCode(String state) {
        if (Objects.isNull(state)) {
            return null;
        }
        for (OperationalStateEnum typeEnum : values()) {
            if (typeEnum.state.equals(state)) {
                return typeEnum.getDesc();
            }
        }
        return null;
    }
}
