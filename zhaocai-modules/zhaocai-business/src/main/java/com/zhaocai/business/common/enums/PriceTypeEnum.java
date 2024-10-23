package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum PriceTypeEnum {

    FIXED_PRICE(1,"固定价"),
    FLOAT_PRICE(2,"浮动价"),
    FIXED_FLOAT_PRICE(3,"固定、浮动价")
    ;

    private final Integer type;

    private final String desc;

    public boolean equalsType(Integer priceType) {
        return this.getType().equals(priceType);
    }
}
