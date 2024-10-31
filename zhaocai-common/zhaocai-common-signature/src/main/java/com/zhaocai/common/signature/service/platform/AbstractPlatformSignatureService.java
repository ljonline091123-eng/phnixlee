package com.zhaocai.common.signature.service.platform;

import com.zhaocai.common.signature.common.enums.AgreementSignStateEnum;
import com.zhaocai.common.signature.common.exception.SignatureValidateException;
import com.zhaocai.common.signature.common.utils.SpringBeanUtils;
import com.zhaocai.common.signature.domain.AgreementSign;
import com.zhaocai.common.signature.dto.sign.SignatureResponse;
import com.zhaocai.common.signature.dto.command.CreateAgreementDocumentCommandRequest;
import com.zhaocai.common.signature.dto.command.SignatureCommandRequest;
import com.zhaocai.common.signature.service.impl.AgreementSignService;
import com.zhaocai.common.signature.utils.JacksonUtil;
import lombok.extern.slf4j.Slf4j;


/**
 * 电子签章平台抽象类
 *
 * @author chenming
 * @date 2024-08-28
 */
@Slf4j
public abstract class AbstractPlatformSignatureService implements PlatformSignatureService {

    private final AgreementSignService agreementSignService = SpringBeanUtils.getBean(AgreementSignService.class);

    @Override
    public SignatureResponse createSignDocument(SignatureCommandRequest request) {
        AgreementSign agreementSign = agreementSignService.getAgreementSignByBusiness(request.getBusinessCode(),request.getBusinessId());
        if (agreementSign != null) {
            System.out.println("[合同签章信息对象toNodeJsonString转换前]\n"+agreementSign);
            agreementSignService.updateSignMessage(agreementSign.getId(), JacksonUtil.toNodeJsonString(agreementSign));
        } else {
            agreementSign = agreementSignService.saveAgreementSign((CreateAgreementDocumentCommandRequest) request);
        }
        log.info("[电子签章] - 新建电子签章签署信息成功，businessId:{}，开始执行创建电子签署文档逻辑..",request.getBusinessId());

        SignatureResponse response = doCreateSign(request,agreementSign);
        if (response.isSuccess()) {
            agreementSignService.updateSignState(AgreementSignStateEnum.CREATED.getState(),agreementSign.getId());
        }
        return response;
    }

    @Override
    public SignatureResponse getSignUrl(SignatureCommandRequest commandRequest) {
        AgreementSign agreementSign = agreementSignService.getAgreementSignByBusiness(commandRequest.getBusinessCode(), commandRequest.getBusinessId());
        if (agreementSign == null) {
            throw new SignatureValidateException("该合同对应的签署信息不存在，请联系开发确认");
        }
        if (!AgreementSignStateEnum.CREATED.equalsSignState(agreementSign.getSignState())) {
            throw new SignatureValidateException("合同签署状态不正确，无法签署，请联系开发确认");
        }

        return doGetSignUrl(commandRequest,agreementSign);
    }

    @Override
    public SignatureResponse downloadDocument(SignatureCommandRequest commandRequest) {
        AgreementSign agreementSign = agreementSignService.getAgreementSignByBusiness(commandRequest.getBusinessCode(), commandRequest.getBusinessId());
        if (agreementSign == null) {
            throw new SignatureValidateException("该合同对应的签署信息不存在，请联系开发确认");
        }
        if (AgreementSignStateEnum.INVALID.equalsSignState(agreementSign.getSignState())) {
            throw new SignatureValidateException("合同已作废，不提供下载服务");
        }

        return doDownloadDocument(commandRequest,agreementSign);
    }

    @Override
    public SignatureResponse cancelAgreement(SignatureCommandRequest commandRequest) {
        AgreementSign agreementSign = agreementSignService.getAgreementSignByBusiness(commandRequest.getBusinessCode(), commandRequest.getBusinessId());
        if (agreementSign == null) {
            throw new SignatureValidateException("该合同对应的签署信息不存在，请联系开发确认");
        }

        if (!AgreementSignStateEnum.SIGN_SUCCESS.equalsSignState(agreementSign.getSignState())) {
            throw new SignatureValidateException("合同签署状态不正确，无法作废，请联系开发确认");
        }

        SignatureResponse response =  doCancelAgreement(commandRequest,agreementSign);
        if (response.isSuccess()) {
            // 作废成功，变更为失效
            agreementSignService.updateSignState(AgreementSignStateEnum.INVALID.getState(),agreementSign.getId());
        }
        return response;
    }

    /**
     * 创建电子签署文档
     * @param request
     */
    protected abstract SignatureResponse doCreateSign(SignatureCommandRequest request,AgreementSign agreementSign);

    /**
     * 获取签署链接
     * @param commandRequest
     * @param agreementSign
     * @return
     */
    protected abstract SignatureResponse doGetSignUrl(SignatureCommandRequest commandRequest, AgreementSign agreementSign);

    /**
     * 下载电子签章文档
     * @param commandRequest
     * @param agreementSign
     * @return
     */
    protected abstract SignatureResponse doDownloadDocument(SignatureCommandRequest commandRequest,AgreementSign agreementSign);

    /**
     * 作废合同
     * @param commandRequest
     * @param agreementSign
     * @return
     */
    protected abstract SignatureResponse doCancelAgreement(SignatureCommandRequest commandRequest, AgreementSign agreementSign);
}
