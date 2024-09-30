package com.zhaocai.common.signature.dto.command;

/**
 * 个人认证授权命令请求生成器
 *
 * @author chenming
 * @date 2024-09-10
 */
public class PersonAuthCommandRequestBuilder {

    private PersonAuthCommandRequest personAuthCommandRequest;

    public PersonAuthCommandRequestBuilder(String businessCode,Long businessId,String applicantName,String applicantMobile) {
        CommandParamValidation.commonParamsValidate(businessCode,businessId,applicantName,applicantMobile);

        this.personAuthCommandRequest = new PersonAuthCommandRequest();

        this.personAuthCommandRequest.setBusinessCode(businessCode);
        this.personAuthCommandRequest.setBusinessId(businessId);
        this.personAuthCommandRequest.setApplicantName(applicantName);
        this.personAuthCommandRequest.setApplicantMobile(applicantMobile);

        this.personAuthCommandRequest.setUserName(applicantName);
        this.personAuthCommandRequest.setUserPhone(applicantMobile);
    }

    public PersonAuthCommandRequestBuilder userId(String userId) {
        this.personAuthCommandRequest.setUserId(userId);
        return this;
    }

    public PersonAuthCommandRequest build() {
        return this.personAuthCommandRequest;
    }

}
