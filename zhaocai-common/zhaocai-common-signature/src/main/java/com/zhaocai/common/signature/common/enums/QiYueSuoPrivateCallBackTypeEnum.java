package com.zhaocai.common.signature.common.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 契约锁回调类型枚举
 *
 * @author chenming
 * @date 2024-09-12
 */
@Getter
@AllArgsConstructor
public enum QiYueSuoPrivateCallBackTypeEnum {

    CALLBACK_CHECK("CALLBACK_CHECK","回调认证"),

    USER_AUTH_SUCCESS("USER_AUTH_SUCCESS","个人认证成功"),
    USER_AUTH_FAILED("USER_AUTH_FAILED","个人认证失败"),
    USER_AUTH_EXPIRED("USER_AUTH_EXPIRED","个人认证失效"),

    COMPANY_AUTH_SUCCESS("COMPANY_AUTH_SUCCESS","组织认证成功"),
    COMPANY_AUTH_FAILED("COMPANY_AUTH_FAILED","组织认证失败"),
    COMPANY_AUTH_INVALID("COMPANY_AUTH_INVALID","组织认证失效"),

    CONTRACT_SIGN("CONTRACT_SIGN","单个签署方签署完成"),
    CONTRACT_COMPLETE("CONTRACT_COMPLETE","文件签署完成"),
    ;

    /**
     * 回调类型
     */
    private final String callBackType;

    /**
     * 回调类型名称
     */
    private final String callBackName;

    public Boolean equalsType(String callBackType) {
        return this.getCallBackType().equals(callBackType);
    }

    /**
     * 判断是否为回调校验
     * @param callBackType
     * @return
     */
    public static Boolean isCallbackCheck(String callBackType) {
        return CALLBACK_CHECK.equalsType(callBackType);
    }
}
