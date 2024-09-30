package com.zhaocai.common.signature.dto.command;

import com.zhaocai.common.signature.dto.SignatureContact;
import com.zhaocai.common.signature.dto.SignatureCreator;
import com.zhaocai.common.signature.dto.SignatureStamper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 创建合同文档命令请求参数构造器
 *
 * @author chenming
 * @date 2024-09-14
 */
public class CreateAgreementDocumentCommandRequestBuilder {

    private CreateAgreementDocumentCommandRequest commandRequest;

    public CreateAgreementDocumentCommandRequestBuilder builder(String businessCode, Long businessId, String applicantName, String applicantMobile) {
        CommandParamValidation.commonParamsValidate(businessCode,businessId,applicantName,applicantMobile);

        this.commandRequest = new CreateAgreementDocumentCommandRequest();
        this.commandRequest.setBusinessCode(businessCode);
        this.commandRequest.setBusinessId(businessId);
        this.commandRequest.setApplicantName(applicantName);
        this.commandRequest.setApplicantMobile(applicantMobile);
        this.commandRequest.setSignatureContactList(new ArrayList<>());

        return this;
    }

    public CreateAgreementDocumentCommandRequestBuilder agreementFile(InputStream agreementFile) {
        this.commandRequest.setAgreementFile(agreementFile);
        return this;
    }

    public CreateAgreementDocumentCommandRequestBuilder agreementFileType(String agreementFileType) {
        this.commandRequest.setAgreementFileType(agreementFileType);
        return this;
    }

    public CreateAgreementDocumentCommandRequestBuilder agreementCode(String agreementCode) {
        this.commandRequest.setAgreementCode(agreementCode);
        return this;
    }

    public CreateAgreementDocumentCommandRequestBuilder agreementName(String agreementName) {
        this.commandRequest.setAgreementName(agreementName);
        return this;
    }

    public CreateAgreementDocumentCommandRequestBuilder signatureCreator(SignatureCreator signatureCreator){
        this.commandRequest.setSignatureCreator(signatureCreator);
        return this;
    }

    public CreateAgreementDocumentCommandRequestBuilder signatureContactPartyA(SignatureContact partyA) {
        this.commandRequest.getSignatureContactList().add(partyA);
        return this;
    }

    public CreateAgreementDocumentCommandRequestBuilder signatureContactPartyB(SignatureContact partyB) {
        this.commandRequest.getSignatureContactList().add(partyB);
        return this;
    }

    public CreateAgreementDocumentCommandRequestBuilder signatureStamperList(List<SignatureStamper> signatureStamperList) {
        this.commandRequest.setSignatureStamperList(signatureStamperList);
        return this;
    }

    public CreateAgreementDocumentCommandRequest build() {
        return this.commandRequest;
    }
}
