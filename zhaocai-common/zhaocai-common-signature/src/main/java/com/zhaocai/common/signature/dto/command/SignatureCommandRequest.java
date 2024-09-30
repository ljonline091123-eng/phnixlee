package com.zhaocai.common.signature.dto.command;

import lombok.Data;

/**
 * 电子签章命令请求参数
 *
 * @author chenming
 * @date 2024-08-27
 */
@Data
public class SignatureCommandRequest {

    /**
     * 业务类型
     */
    private String businessCode;

    /**
     * 业务编码
     */
    private Long businessId;

    /**
     * 申请人名称
     */
    private String applicantName;

    /**
     * 申请人联系方式
     */
    private String applicantMobile;
}
