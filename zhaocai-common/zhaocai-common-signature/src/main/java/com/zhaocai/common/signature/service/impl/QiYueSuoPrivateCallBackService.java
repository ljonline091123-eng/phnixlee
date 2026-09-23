package com.zhaocai.common.signature.service.impl;

import com.zhaocai.common.signature.common.enums.AgreementSignStateEnum;
import com.zhaocai.common.signature.common.enums.QiYueSuoPrivateCallBackTypeEnum;
import com.zhaocai.common.signature.common.exception.SignatureValidateException;
import com.zhaocai.common.signature.common.utils.SpringBeanUtils;
import com.zhaocai.common.signature.domain.AgreementSign;
import com.zhaocai.common.signature.domain.AgreementSignQys;
import com.zhaocai.common.signature.domain.AgreementSignature;
import com.zhaocai.common.signature.dto.callback.qysp.CompanyAuthCallBackData;
import com.zhaocai.common.signature.dto.callback.qysp.PersonAuthCallBackData;
import com.zhaocai.common.signature.dto.callback.qysp.SignCallBackData;
import com.zhaocai.common.signature.dto.callback.qysp.SignatoryCallBackInfo;
import com.zhaocai.common.signature.service.SignatureCallBackBusinessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 契约锁回调处理服务
 *
 * @author chenming
 * @date 2024-09-11
 */
@Slf4j
@Service
public class QiYueSuoPrivateCallBackService {

    private final SignatureCallBackBusinessService businessService = SpringBeanUtils.getBean(SignatureCallBackBusinessService.class);

    @Autowired
    private AgreementSignatureService agreementSignatureService;

    @Autowired
    private AgreementSignService agreementSignService;

    @Autowired
    private AgreementSignQysService agreementSignQysService;

    /**
     * 个人授权认证回调处理
     * @param personAuthCallBack
     */
    public void personAuthCallBack(PersonAuthCallBackData personAuthCallBack) {
        log.info("[契约锁回调 - 个人授权认证] - 开始处理个人授权认证回调...");
        businessService.personAuthCallBack(personAuthCallBack);
        log.info("[契约锁回调 - 个人授权认证] - 个人授权认证回调处理完成...");
    }

    /**
     * 公司授权认证回调处理
     * @param companyAuthCallBackData
     */
    public void companyAuthCallBack(CompanyAuthCallBackData companyAuthCallBackData) {
        log.info("[契约锁回调 - 公司授权认证] - 开始处理公司授权认证回调...");
        businessService.companyAuthCallBack(companyAuthCallBackData);
        log.info("[契约锁回调 - 公司授权认证] - 公司授权认证回调处理完成...");
    }

    /**
     * 电子签章回调回调
     */
    public void signCallBack(SignCallBackData signCallBackData) {
        log.info("[契约锁回调 - 电子签章] - 开始处理电子签章回调...");
        // 获取签署方信息
        AgreementSignQys agreementSignQys = agreementSignQysService.getByContractId(signCallBackData.getContractId());
        if (agreementSignQys == null) {
            throw new SignatureValidateException(String.format("该 contractId[%s]对应的签署信息不存在",signCallBackData.getContractId()));
        }

        AgreementSign agreementSign = agreementSignService.getById(agreementSignQys.getSignId());

        if (QiYueSuoPrivateCallBackTypeEnum.CONTRACT_SIGN.equalsType(signCallBackData.getCallbackType())) {
            log.info("[契约锁回调 - 电子签章] - 处理电子签章回调,类型为：单个签署方签署完成，处理签署人员状态");
            // 单个签署方更新签署状态
            SignatoryCallBackInfo signatoryCallBackInfo = signCallBackData.getSignatoryInfo();
            AgreementSignature agreementSignature = agreementSignatureService.getBySignIdAndSignature(agreementSignQys.getSignId(),signatoryCallBackInfo.getReceiverContact());
            if (agreementSignature == null) {
                throw new SignatureValidateException(String.format("[%s-%s]对应的签署方信息不存在",agreementSignQys.getSignId(),signatoryCallBackInfo.getReceiverContact()));
            }
            if (!agreementSignature.getSignatureName().equals(signatoryCallBackInfo.getTenantName())) {
                throw new SignatureValidateException(String.format("签署方主体信息不一致[%s-%s]",agreementSignature.getSignatureName(),signatoryCallBackInfo.getTenantName()));
            }
            if (!agreementSignature.getSignatureContactName().equals(signatoryCallBackInfo.getReceiverName())) {
                throw new SignatureValidateException(String.format("签署方接收方名称不一致[%s-%s]",agreementSignature.getSignatureContactName(),signatoryCallBackInfo.getReceiverName()));
            }

            agreementSignatureService.updateSignCallBackInfo(agreementSignature.getId(), signCallBackData);
            businessService.signCallBack(signCallBackData,agreementSignature.getSignatureType(),agreementSign.getBusinessId());
        } else if (QiYueSuoPrivateCallBackTypeEnum.CONTRACT_COMPLETE.equalsType(signCallBackData.getCallbackType())) {
            log.info("[契约锁回调 - 电子签章] - 处理电子签章回调,类型为：文件签署完成");
            agreementSignQysService.setSignFinished(agreementSignQys.getId());
            agreementSignService.updateSignState(AgreementSignStateEnum.SIGN_SUCCESS.getState(), agreementSign.getId());

            businessService.signCallBack(signCallBackData,null,agreementSign.getBusinessId());
        }

        log.info("[契约锁回调 - 电子签章] - 电子签章回调处理完成...");
    }
}
