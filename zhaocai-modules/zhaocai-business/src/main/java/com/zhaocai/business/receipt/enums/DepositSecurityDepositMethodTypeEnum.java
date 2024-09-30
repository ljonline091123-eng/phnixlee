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
public enum DepositSecurityDepositMethodTypeEnum {

    /**
     * 押金、保证金类型
     */
    DEPOSIT_SECURITY_DEPOSIT_METHOD_TYPE_ONE("1", "履约保证金"),
    JDEPOSIT_SECURITY_DEPOSIT_METHOD_TYPE_TWO("2", "质量保证金"),
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
        for (DepositSecurityDepositMethodTypeEnum typeEnum : values()) {
            if (typeEnum.state.equals(state)) {
                return typeEnum.getDesc();
            }
        }
        return null;
    }
}
