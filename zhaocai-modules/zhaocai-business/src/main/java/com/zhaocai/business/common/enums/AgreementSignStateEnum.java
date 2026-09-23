package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 合同签约状态枚举
 *
 * @author chenming
 * @date 2024-06-21
 */
@Getter
@AllArgsConstructor
public enum AgreementSignStateEnum {

    PARTY_B_NOT_SIGN(0,"乙方未签约"),
    PARTY_A_NOT_SIGN(1,"甲方未签约"),
    ALL_SIGN(2,"甲方已签约"),

    ;

    private final Integer state;

    private final String desc;

    public Boolean equalsState(Integer signState) {
        return this.getState().equals(signState);
    }
}
