package com.zhaocai.common.signature.dto.sign;

import cn.hutool.core.util.IdUtil;
import com.zhaocai.common.signature.common.enums.RequestType;
import com.zhaocai.common.signature.dto.command.SignatureCommandRequest;
import lombok.Data;

/**
 * 电子签章命令 DTO
 *
 * @author chenming
 * @date 2024-08-19
 */
@Data
public class SignatureRequest {

    public SignatureRequest(SignatureCommandRequest request) {
        this.bizId = IdUtil.getSnowflakeNextIdStr();

        this.businessCode = request.getBusinessCode();
        this.businessId = request.getBusinessId();
        this.applicantName = request.getApplicantName();
        this.applicantMobile = request.getApplicantMobile();
    }

    /**
     * 业务 ID
     */
    private String bizId;

    /**
     * 请求类型
     */
    private RequestType requestType;

    /**
     * 业务编码
     */
    private String businessCode;

    /**
     * 业务 id
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
