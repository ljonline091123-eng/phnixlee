package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 协议状态枚举
 *
 * @author chenming
 * @date 2024-06-21
 */
@Getter
@AllArgsConstructor
public enum AgreementStateEnum {

    DRAFT(0,"自由态"),
    IN_APPROVAL(1,"审批中"),
    CANCELLATION(2,"已作废"),
    APPROVE(3,"已完成"),
    REJECT(4,"已驳回"),
    REVOKED(5,"已撤回"),
    VENDOR_VERIFY(6,"待供应商确认"),
    PUSH_SIGNATURE_PLATFORM(7,"待推送至电子签章"),
    PARTY_B_TO_SIGN(8,"待乙方签署"),
    PARTY_A_TO_SIGN(9,"待甲方签署"),
    SIGN_SUCCESS(10,"合同已签署"),
    REVOCATION_SIGN(19,"撤回签署合同"),
    CANCEL_SIGN(20,"作废签署合同"),

    ;

    private final Integer state;

    private final String desc;

    public Boolean equalsState(Integer state) {
        return this.getState().equals(state);
    }

    public static Boolean isSign(Integer state) {
        return PARTY_A_TO_SIGN.equalsState(state) ||
                SIGN_SUCCESS.equalsState(state);
    }

    /**
     * 是否可作废
     * @param state
     * @return
     */
    public static Boolean isCancel(Integer state) {
        return DRAFT.equalsState(state) || REVOKED.equalsState(state) || CANCEL_SIGN.equalsState(state);
    }
}
