package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 供应商签订状态
 *
 * @author chenming
 * @date 2024-09-03
 */
@Getter
@AllArgsConstructor
public enum SignStateEnum {

    TO_SIGN(0,"待签章"),
    SIGN_SUCCESS(1,"签章成功"),
    SIGN_FAILED(2,"签章失败"),
    SIGN_EXPIRED(3,"签章失效"),
    ;

    /**
     * 状态
     */
    private final Integer state;

    /**
     * 描述
     */
    private final String desc;

    public static Boolean isCompleteSign(Integer state) {
        return SIGN_SUCCESS.getState().equals(state);
    }

    public Boolean equalsState(Integer state) {
        return this.getState().equals(state);
    }
}
