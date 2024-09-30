package com.zhaocai.common.signature.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 合同签署状态
 *
 * @author chenming
 * @date 2024-09-20
 */
@Getter
@AllArgsConstructor
public enum AgreementSignStateEnum {

    INVALID(0,"合同已作废"),
    CREATED(1,"合同已创建"),
    SIGN_SUCCESS(2,"合同已签署")
    ;

    private Integer state;

    private String desc;

    public Boolean equalsSignState(Integer state) {
        return this.getState().equals(state);
    }
}
