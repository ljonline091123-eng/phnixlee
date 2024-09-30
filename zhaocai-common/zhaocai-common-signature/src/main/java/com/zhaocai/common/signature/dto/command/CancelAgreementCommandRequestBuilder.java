package com.zhaocai.common.signature.dto.command;

/**
 * 作废签署合同命令参数
 *
 * @author chenming 构造器
 * @date 2024-09-20
 */
public class CancelAgreementCommandRequestBuilder {

    private CancelAgreementCommandRequest commandRequest;

    public CancelAgreementCommandRequestBuilder builder(String businessCode, Long businessId, String applicantName, String applicantMobile) {
        CommandParamValidation.commonParamsValidate(businessCode,businessId,applicantName,applicantMobile);

        this.commandRequest = new CancelAgreementCommandRequest();
        this.commandRequest.setBusinessCode(businessCode);
        this.commandRequest.setBusinessId(businessId);
        this.commandRequest.setApplicantName(applicantName);
        this.commandRequest.setApplicantMobile(applicantMobile);

        return this;
    }

    public CancelAgreementCommandRequestBuilder cancelReason(String cancelReason) {
        this.commandRequest.setCancelReason(cancelReason);
        return this;
    }

    public CancelAgreementCommandRequest build() {
        return this.commandRequest;
    }
}
