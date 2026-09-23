package com.zhaocai.common.signature.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 签署方类型
 *
 * @author chenming
 * @date 2024-09-18
 */
@Getter
@AllArgsConstructor
public enum SignatureTypeEnum {

    PARTY_A(1,"甲方"),
    PARTY_B(2,"乙方")
    ;

    /**
     * 类型
     */
    private final Integer type;

    /**
     * 名称
     */
    private final String name;

    public Boolean equalsType(Integer type) {
        return this.getType().equals(type);
    }
}
