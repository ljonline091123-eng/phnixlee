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
public enum ConTypeEnum {
    /**
     * 支出业务分类
     */
    EXPENSE_BUSINESS_CLASSIFICATION_ZERO("0", "其他"),
    EXPENSE_BUSINESS_CLASSIFICATION_ONE("1", "劳务分包"),
    EXPENSE_BUSINESS_CLASSIFICATION_TWO("2", "专业分包"),
    EXPENSE_BUSINESS_CLASSIFICATION_THREE("3", "购买材料"),
    EXPENSE_BUSINESS_CLASSIFICATION_FOUR("4", "租赁材料"),
    EXPENSE_BUSINESS_CLASSIFICATION_FIVE("5", "设备租赁（机械）)"),

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
        for (ConTypeEnum typeEnum : values()) {
            if (typeEnum.state.equals(state)) {
                return typeEnum.getDesc();
            }
        }
        return null;
    }
}
