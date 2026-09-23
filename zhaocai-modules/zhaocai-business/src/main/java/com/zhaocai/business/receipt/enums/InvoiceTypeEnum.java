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
public enum InvoiceTypeEnum {
    /**
     * 发票类型
     */
    INVOICE_TYPE_ONE("1", "专票"),
    INVOICE_TYPE_TWO("2", "普票"),
    INVOICE_TYPE_THREE("3", "数电票"),

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
        for (InvoiceTypeEnum typeEnum : values()) {
            if (typeEnum.state.equals(state)) {
                return typeEnum.getDesc();
            }
        }
        return null;
    }
}
