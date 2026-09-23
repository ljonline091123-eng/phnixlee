package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum PriceTypeEnum {

    /* 拆分清单列表同时存在两种价格类型时，采购计划就赋值为3，采购方案保存引用时会校验价格类型是一致的合约拆分归属的采购计划价格类型，不一致不能保存成功 */

    FIXED_PRICE(1, "固定价"),
    FLOAT_PRICE(2, "浮动价"),
    FIXED_FLOAT_PRICE(3, "固定价、浮动价"),
    FLOAT_RATE(4, "浮动率"),
    FIXED_FLOAT_RATE(5, "固定价、浮动率"),
    FLOAT_FLOAT_RATE(6, "浮动价、浮动率"),
    FIXED_FLOAT_FLOAT_RATE(7, "固定价、浮动价、浮动率");

    private final Integer type;

    private final String desc;

    public boolean equalsType(Integer priceType) {
        return this.getType().equals(priceType);
    }
}
