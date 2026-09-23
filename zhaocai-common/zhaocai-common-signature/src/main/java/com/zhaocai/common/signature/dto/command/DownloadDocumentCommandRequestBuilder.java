package com.zhaocai.common.signature.dto.command;

/**
 * 下载签署文档命令参数构造器
 *
 * @author chenming
 * @date 2024-09-20
 */
public class DownloadDocumentCommandRequestBuilder {

    private DownloadDocumentCommandRequest commandRequest;

    public DownloadDocumentCommandRequestBuilder builder(String businessCode, Long businessId, String applicantName, String applicantMobile) {
        CommandParamValidation.commonParamsValidate(businessCode,businessId,applicantName,applicantMobile);

        this.commandRequest = new DownloadDocumentCommandRequest();

        this.commandRequest.setBusinessCode(businessCode);
        this.commandRequest.setBusinessId(businessId);
        this.commandRequest.setApplicantName(applicantName);
        this.commandRequest.setApplicantMobile(applicantMobile);

        return this;
    }

    public DownloadDocumentCommandRequest build() {
        return this.commandRequest;
    }
}
