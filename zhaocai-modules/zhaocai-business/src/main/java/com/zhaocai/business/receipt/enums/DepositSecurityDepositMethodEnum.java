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
public enum DepositSecurityDepositMethodEnum {
    /**
     * 押金、保证金方式
     */
    DEPOSIT_SECURITY_DEPOSIT_METHOD_ONE("1", "现金"),
    DEPOSIT_SECURITY_DEPOSIT_METHOD_TWO("2", "保函"),
    DEPOSIT_SECURITY_DEPOSIT_METHOD_THREE("3", "担保"),

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
        for (DepositSecurityDepositMethodEnum typeEnum : values()) {
            if (typeEnum.state.equals(state)) {
                return typeEnum.getDesc();
            }
        }
        return null;
    }
}
