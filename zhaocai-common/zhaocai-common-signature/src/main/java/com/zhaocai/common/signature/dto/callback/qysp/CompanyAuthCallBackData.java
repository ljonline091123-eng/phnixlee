package com.zhaocai.common.signature.dto.callback.qysp;

import lombok.Data;

/**
 * 契约锁公司认证回调
 *
 * @author chenming
 * @date 2024-09-13
 */
@Data
public class CompanyAuthCallBackData extends QiYueSuoCallBackData {

    /**
     * 组织id
     */
    private String companyId;

    /**
     * 组织名称
     */
    private String companyName;

    /**
     * 第三方组织id
     */
    private String openCompanyId;

    /**
     * 工商注册号
     */
    private String registerNo;

    /**
     * 所属地区
     */
    private String corpArea;

    /**
     * 组织单位编码(仅内部组织)
     */
    private String orgCode;

    /**
     * 法人名称
     */
    private String legalPersonName;

    /**
     * 组织类型
     */
    private String companyType;

    /**
     * 组织内外部类型
     */
    private String tenantScope;

    /**
     * 认证状态
     */
    private String authStatus;

    /**
     * 申请人姓名
     */
    private String applicantName;

    /**
     * 申请人手机号
     */
    private String applicantPhone;

    /**
     * 申请人邮箱
     */
    private String applicantEmail;

    /**
     * 授权期限，时间戳
     */
    private String authEndTime;

    /**
     * 认证拒绝原因
     */
    private String rejectReason;

    /**
     * 认证失败步骤:1.基本信息审核失败 2.授权书审核失败 3.线下材料审核失败
     */
    private String rejectStep;

    /**
     * 认证失败步骤描述:1.基本信息审核失败 2.授权书审核失败 3.线下材料审核失败
     */
    private String rejectStepDesc;
}
