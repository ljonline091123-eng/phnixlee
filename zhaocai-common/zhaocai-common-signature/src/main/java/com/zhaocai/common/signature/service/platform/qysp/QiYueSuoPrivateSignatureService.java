package com.zhaocai.common.signature.service.platform.qysp;

import com.zhaocai.common.signature.common.enums.QiYueSuoSignStepEnum;
import com.zhaocai.common.signature.common.exception.SignatureValidateException;
import com.zhaocai.common.signature.common.utils.SpringBeanUtils;
import com.zhaocai.common.signature.domain.AgreementSign;
import com.zhaocai.common.signature.domain.AgreementSignQys;
import com.zhaocai.common.signature.domain.AgreementSignature;
import com.zhaocai.common.signature.dto.command.*;
import com.zhaocai.common.signature.dto.sign.SignatureResponse;
import com.zhaocai.common.signature.dto.sign.qysp.*;
import com.zhaocai.common.signature.service.impl.AgreementSignQysService;
import com.zhaocai.common.signature.service.impl.AgreementSignatureService;
import com.zhaocai.common.signature.service.platform.AbstractPlatformSignatureService;
import lombok.extern.slf4j.Slf4j;
import net.qiyuesuo.v3sdk.model.v2document.response.V2DocumentCreatebyfileResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 契约锁电子签章服务
 *
 * @author chenming
 * @date 2024-08-19
 */
@Slf4j
@Component(value = "platformSignatureService")
@ConditionalOnProperty(name = "signature.platform",havingValue = "qiyuesuo-private")
public class QiYueSuoPrivateSignatureService extends AbstractPlatformSignatureService {

    @Autowired
    private AgreementSignQysService agreementSignQysService;

    @Autowired
    private AgreementSignatureService agreementSignatureService;

    @PostConstruct
    public void init() {
        log.info("完成契约锁电子签章 service 服务的初始化..");
    }

    @Override
    public SignatureResponse personAuth(SignatureCommandRequest commandRequest) {
        PersonAuthCommandRequest authCommandRequest = (PersonAuthCommandRequest) commandRequest;

        // 构建请求参数
        PersonAuthRequest personAuthRequest = new PersonAuthRequest(authCommandRequest);
        personAuthRequest.setUserName(authCommandRequest.getUserName());
        personAuthRequest.setUserPhone(authCommandRequest.getUserPhone());
        personAuthRequest.setUserId(authCommandRequest.getUserId());

        // 发送请求
        return SpringBeanUtils.getBean(PersonAuthService.class).sendSignRequest(personAuthRequest);
    }

    @Override
    public SignatureResponse companyAuth(SignatureCommandRequest commandRequest) {
        CompanyAuthCommandRequest authCommandRequest = (CompanyAuthCommandRequest) commandRequest;

        // 构建请求参数
        CompanyAuthRequest companyAuthRequest = new CompanyAuthRequest(authCommandRequest);
        companyAuthRequest.setCompanyId(authCommandRequest.getCompanyId());
        companyAuthRequest.setCompanyName(authCommandRequest.getCompanyName());
        companyAuthRequest.setRegisterNo(authCommandRequest.getSocialCreditCode());
        companyAuthRequest.setLegalPersonName(authCommandRequest.getLegalPersonName());
        companyAuthRequest.setChargerName(authCommandRequest.getApplicantName());
        companyAuthRequest.setChargerMobile(authCommandRequest.getApplicantMobile());

        // 发送请求
        return SpringBeanUtils.getBean(CompanyAuthService.class).sendSignRequest(companyAuthRequest);
    }

    @Override
    public SignatureResponse doCreateSign(SignatureCommandRequest request, AgreementSign agreementSign) {
        /*
         * 创建电子签署，共需调用两个接口
         * 1. 创建签署文档
         * 2. 创建电子签约
         */
        AgreementSignQys agreementSignQys = agreementSignQysService.getBySignId(agreementSign.getId());
        if (agreementSignQys == null || !QiYueSuoSignStepEnum.CREATE_BY_FILE.equalsStep(agreementSignQys.getSignStep())) {
            // agreementSignQys 为空或者 step 为 create_by_file，则表明还没有创建签署文档，调用[创建签署文档]接口
            log.info("[电子签章-契约锁] - {},请求还未完成第一步[创建签署文档]，开始执行创建签署文档操作",request.getBusinessId());
            agreementSignQys = doCreateByFile(request,agreementSign);
        }

        if (QiYueSuoSignStepEnum.CREATE_BY_CATEGORY.equalsStep(agreementSignQys.getSignStep())) {
            //  signStep 为 create_by_category，则表明已完成创建签署文档，调用[创建电子签约]接口
            // 保存签署人相关信息
            CreateAgreementDocumentCommandRequest commandRequest = (CreateAgreementDocumentCommandRequest) request;
            agreementSignatureService.saveAgreementSignature(commandRequest.getSignatureContactList(),agreementSign.getId());

            // 发送请求
            return doCreateByCategory(commandRequest,agreementSignQys);
        }

        throw new SignatureValidateException("契约锁签章步骤处理有误，请联系开发确认");
    }

    @Override
    protected SignatureResponse doGetSignUrl(SignatureCommandRequest commandRequest, AgreementSign agreementSign) {
        // 获取签章信息
        AgreementSignQys agreementSignQys = agreementSignQysService.getBySignId(agreementSign.getId());
        if (!QiYueSuoSignStepEnum.TO_SIGN.equalsStep(agreementSignQys.getSignStep())) {
            throw new SignatureValidateException("该合同的签署状态不正确,无法进行签署操作，请联系开发确认");
        }

        GetSignUrlCommandRequest request = (GetSignUrlCommandRequest) commandRequest;
        // 获取签署方
        AgreementSignature agreementSignature = agreementSignatureService.getBySignIdAndType(request.getSignatureType(),agreementSign.getId());
        if (agreementSignature == null) {
            throw new SignatureValidateException("获取合同的签署方失败，请联系开发确认");
        }
        if (!agreementSignature.getSignatureName().equals(request.getSignatureName()) ||
                !agreementSignature.getSignatureContactName().equals(request.getSignatureContactName()) ||
                !agreementSignature.getSignatureContactPhone().equals(request.getSignatureContactPhone())) {
            throw new SignatureValidateException("签署信息与合同提供的签署方信息不一致，请联系开发确认");
        }

        GetSignUrlRequest signUrlRequest = new GetSignUrlRequest(commandRequest);
        signUrlRequest.setSignatureId(agreementSignature.getId());
        signUrlRequest.setContractId(Long.valueOf(agreementSignQys.getContractId()));
        signUrlRequest.setTenantName(request.getSignatureName());
        signUrlRequest.setReceiverName(request.getSignatureContactName());
        signUrlRequest.setContact(request.getSignatureContactPhone());

        SignatureResponse signatureResponse = SpringBeanUtils.getBean(GetSignUrlService.class).sendSignRequest(signUrlRequest);
        if (signatureResponse.isSuccess()) {
            return signatureResponse;
        }

        throw new SignatureValidateException(signatureResponse.getMessage());
    }

    @Override
    protected SignatureResponse doDownloadDocument(SignatureCommandRequest commandRequest,AgreementSign agreementSign) {
        AgreementSignQys agreementSignQys = agreementSignQysService.getBySignId(agreementSign.getId());
        if (agreementSignQys == null) {
            throw new SignatureValidateException("该合同对应的电子签章信息不存在，请联系开发确认");
        }

        DownloadDocumentRequest downloadDocumentRequest = new DownloadDocumentRequest(commandRequest);
        downloadDocumentRequest.setDocumentId(Long.valueOf(agreementSignQys.getDocumentId()));

        return SpringBeanUtils.getBean(DownloadDocumentService.class).sendSignRequest(downloadDocumentRequest);
    }

    @Override
    protected SignatureResponse doCancelAgreement(SignatureCommandRequest commandRequest, AgreementSign agreementSign) {
        AgreementSignQys agreementSignQys = agreementSignQysService.getBySignId(agreementSign.getId());
        if (agreementSignQys == null) {
            throw new SignatureValidateException("该合同对应的电子签章信息不存在，请联系开发确认");
        }

        CancelAgreementCommandRequest cancelAgreementCommandRequest = (CancelAgreementCommandRequest) commandRequest;
        CancelContractRequest cancelContractRequest = new CancelContractRequest(commandRequest);
        cancelContractRequest.setReason(cancelAgreementCommandRequest.getCancelReason());
        cancelContractRequest.setContractId(Long.valueOf(agreementSignQys.getContractId()));

        return SpringBeanUtils.getBean(CancelContractService.class).sendSignRequest(cancelContractRequest);
    }

    /**
     * 创建签署文档
     * @param request
     * @param agreementSign
     */
    private AgreementSignQys doCreateByFile(SignatureCommandRequest request, AgreementSign agreementSign) {
        AgreementSignQys agreementSignQys = agreementSignQysService.addAgreementSignQys(agreementSign.getId(),request.getBusinessCode(),request.getBusinessId());

        // 调用创建签署文档接口
        CreateAgreementDocumentCommandRequest commandRequest = (CreateAgreementDocumentCommandRequest) request;
        CreateByFileRequest createByFileRequest = new CreateByFileRequest(commandRequest);
        createByFileRequest.setFile(commandRequest.getAgreementFile());
        createByFileRequest.setTitle(commandRequest.getAgreementName());
        createByFileRequest.setFileType(commandRequest.getAgreementFileType());

        SignatureResponse signatureResponse = SpringBeanUtils.getBean(CreateByFileService.class).sendSignRequest(createByFileRequest);
        if (signatureResponse.isSuccess()) {
            log.info("[电子签章-契约锁] - {},执行创建签署文档操作成功...",request.getBusinessId());
            V2DocumentCreatebyfileResponse response = (V2DocumentCreatebyfileResponse) signatureResponse.getResponseResult();

            // 保存 documentId
            agreementSignQysService.setDocumentId(agreementSignQys.getId(),response.getDocumentId());
            agreementSignQys.setDocumentId(response.getDocumentId());
            agreementSignQys.setSignStep(QiYueSuoSignStepEnum.CREATE_BY_CATEGORY.getStep());

            return agreementSignQys;
        }
        throw new SignatureValidateException(signatureResponse.getMessage());
    }

    /**
     * 创建电子签约
     * @param commandRequest
     * @param agreementSignQys
     */
    private SignatureResponse doCreateByCategory(CreateAgreementDocumentCommandRequest commandRequest, AgreementSignQys agreementSignQys) {
        CreateByCategoryRequest createByCategoryRequest = new CreateByCategoryRequest(commandRequest);

        createByCategoryRequest.setDocuments(Long.valueOf(agreementSignQys.getDocumentId()));
        createByCategoryRequest.setDocumentCode(commandRequest.getAgreementCode());
        createByCategoryRequest.setSubject(commandRequest.getAgreementName());
        createByCategoryRequest.setSignatureCreator(commandRequest.getSignatureCreator());
        createByCategoryRequest.setSignatureContactList(commandRequest.getSignatureContactList());
        createByCategoryRequest.setSignatureStamperList(commandRequest.getSignatureStamperList());

        SignatureResponse signatureResponse = SpringBeanUtils.getBean(CreateByCategoryService.class).sendSignRequest(createByCategoryRequest);
        if (signatureResponse.isSuccess()) {
            log.info("[电子签章-契约锁] - {},执行创建电子签约操作成功...",commandRequest.getBusinessId());
            CreateByCategoryResponse response = (CreateByCategoryResponse) signatureResponse.getResponseResult();
            // 设置 contractId
            agreementSignQysService.setContractId(response.getContractId(),agreementSignQys.getId());
            return signatureResponse;
        } else {
            throw new SignatureValidateException(signatureResponse.getMessage());
        }
    }
}
