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
public enum InvoiceEnum {

    /**
     * 操作状态
     */
    OPERATIONAL_STATE_ONE("0", "生成发货单"),
    OPERATIONAL_STATE_TWO("1", "已生成"),
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
        for (InvoiceEnum typeEnum : values()) {
            if (typeEnum.state.equals(state)) {
                return typeEnum.getDesc();
            }
        }
        return null;
    }
}
