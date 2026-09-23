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
public enum CostCollectionMethodEnum {

    /**
     * 成本归集方式
     */
    COST_COLLECTION_METHOD_ONE("1", "工程施工合同履约成本"),
    COST_COLLECTION_METHOD_TWO("2", "研发开支-资本化"),
    COST_COLLECTION_METHOD_THREE("3", "研发开支-费用化"),

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
        for (CostCollectionMethodEnum typeEnum : values()) {
            if (typeEnum.state.equals(state)) {
                return typeEnum.getDesc();
            }
        }
        return null;
    }
}
