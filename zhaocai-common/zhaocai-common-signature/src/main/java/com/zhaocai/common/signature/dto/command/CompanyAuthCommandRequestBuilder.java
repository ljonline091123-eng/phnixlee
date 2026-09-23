package com.zhaocai.common.signature.dto.command;

/**
 * 公司认证命令请求构造器
 *
 * @author chenming
 * @date 2024-09-09
 */
public class CompanyAuthCommandRequestBuilder {

    private  CompanyAuthCommandRequest commandRequest;

    public CompanyAuthCommandRequestBuilder builder(String businessCode, Long businessId, String applicantName, String applicantMobile,
                                                    String companyName, String socialCreditCode) {
        CommandParamValidation.companyAuthParamsValidate(businessCode,businessId,applicantName,applicantMobile,companyName,socialCreditCode);

        this.commandRequest = new CompanyAuthCommandRequest();
        this.commandRequest.setBusinessCode(businessCode);
        this.commandRequest.setBusinessId(businessId);
        this.commandRequest.setApplicantName(applicantName);
        this.commandRequest.setApplicantMobile(applicantMobile);

        this.commandRequest.setCompanyName(companyName);
        this.commandRequest.setSocialCreditCode(socialCreditCode);

        return this;
    }

    public CompanyAuthCommandRequestBuilder legalPersonName(String legalPersonName) {
        this.commandRequest.setLegalPersonName(legalPersonName);
        return this;
    }

    public CompanyAuthCommandRequestBuilder legalPersonPhone(String legalPersonPhone) {
        this.commandRequest.setLegalPersonPhone(legalPersonPhone);
        return this;
    }

    public CompanyAuthCommandRequestBuilder companyId(Long companyId) {
        this.commandRequest.setCompanyId(companyId);
        return this;
    }

    public CompanyAuthCommandRequest build() {
        return this.commandRequest;
    }
}
