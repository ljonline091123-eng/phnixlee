package com.zhaocai.business.receipt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * @author cff
 * @date 2024/09/10
 */
@Getter
@AllArgsConstructor
public enum BaseEnum {
    /**
     * 押金、保证金基数
     */
    BASE_ONE("1", "签订金额"),
    BASE_TWO("2", "结算金额"),
    BASE_THREE("3", "次性缴纳"),

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
        for (BaseEnum typeEnum : values()) {
            if (typeEnum.state.equals(state)) {
                return typeEnum.getDesc();
            }
        }
        return null;
    }
}
