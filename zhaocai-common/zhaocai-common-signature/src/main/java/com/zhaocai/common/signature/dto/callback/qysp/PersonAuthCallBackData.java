package com.zhaocai.common.signature.dto.callback.qysp;


import lombok.Data;

/**
 * 个人身份验证回拨dto
 *
 * @author chenming
 * @date 2024-09-11
 */
@Data
public class PersonAuthCallBackData extends QiYueSuoCallBackData {

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 用户手机号
     */
    private String userMobile;

    /**
     * 用户邮箱
     */
    private String userEmail;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 第三方系统用户id
     */
    private String openUserId;

    /**
     * 认证来源
     */
    private String authSourceType;

    /**
     * 证件类型
     */
    private String paperType;

    /**
     * 个人内外部类型
     */
    private String tenantScope;

    /**
     * 认证状态
     */
    private String authStatus;

    /**
     * 业务数据
     */
    private String bizNo;

    /**
     * 认证失败原因
     */
    private String rejectReason;
}
