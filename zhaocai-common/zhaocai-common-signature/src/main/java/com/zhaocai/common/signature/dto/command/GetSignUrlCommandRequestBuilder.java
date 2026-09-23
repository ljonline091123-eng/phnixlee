package com.zhaocai.common.signature.dto.command;


import com.zhaocai.common.signature.common.enums.SignatureTypeEnum;

/**
 * 获取签署 url 命令参数构造器
 *
 * @author chenming
 * @date 2024-09-19
 */
public class GetSignUrlCommandRequestBuilder {

    private GetSignUrlCommandRequest commandRequest;

    public GetSignUrlCommandRequestBuilder builder(String businessCode, Long businessId, String applicantName, String applicantMobile) {
        CommandParamValidation.commonParamsValidate(businessCode,businessId,applicantName,applicantMobile);

        this.commandRequest = new GetSignUrlCommandRequest();

        this.commandRequest.setBusinessCode(businessCode);
        this.commandRequest.setBusinessId(businessId);
        this.commandRequest.setApplicantName(applicantName);
        this.commandRequest.setApplicantMobile(applicantMobile);

        this.commandRequest.setSignatureContactName(applicantName);
        this.commandRequest.setSignatureContactPhone(applicantMobile);

        return this;
    }

    public GetSignUrlCommandRequestBuilder signatureType(SignatureTypeEnum signatureType) {
        this.commandRequest.setSignatureType(signatureType);
        return this;
    }

    public GetSignUrlCommandRequestBuilder signatureName(String signatureName) {
        this.commandRequest.setSignatureName(signatureName);

        return this;
    }


    public GetSignUrlCommandRequest build() {
        return this.commandRequest;
    }
}
