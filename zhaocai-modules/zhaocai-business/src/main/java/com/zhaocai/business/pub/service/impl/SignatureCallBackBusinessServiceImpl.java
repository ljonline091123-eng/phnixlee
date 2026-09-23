package com.zhaocai.business.pub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhaocai.business.agreement.domain.Agreement;
import com.zhaocai.business.agreement.service.IAgreementService;
import com.zhaocai.business.common.enums.AgreementStateEnum;
import com.zhaocai.business.common.enums.SignStateEnum;
import com.zhaocai.business.contract.service.IContractCompatibilityService;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.domain.VendorContact;
import com.zhaocai.business.vendor.service.IVendorContactService;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.signature.common.enums.QiYueSuoPrivateCallBackTypeEnum;
import com.zhaocai.common.signature.common.enums.SignatureTypeEnum;
import com.zhaocai.common.signature.common.exception.SignatureException;
import com.zhaocai.common.signature.dto.callback.CallBackData;
import com.zhaocai.common.signature.dto.callback.qysp.CompanyAuthCallBackData;
import com.zhaocai.common.signature.dto.callback.qysp.PersonAuthCallBackData;
import com.zhaocai.common.signature.dto.callback.qysp.SignCallBackData;
import com.zhaocai.common.signature.service.SignatureCallBackBusinessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 电子签章回调处理类
 *
 * @author chenming
 * @date 2024-09-11
 */
@Slf4j
@Service
public class SignatureCallBackBusinessServiceImpl implements SignatureCallBackBusinessService {

    @Autowired
    private IVendorContactService vendorContactService;

    @Autowired
    private IVendorService vendorService;

    @Autowired
    private IAgreementService agreementService;

    @Autowired
    private IContractCompatibilityService contractCompatibilityService;


    @Override
    public void personAuthCallBack(CallBackData callBackData) {
        PersonAuthCallBackData personAuthCallBackData = (PersonAuthCallBackData) callBackData;
        VendorContact vendorContact = getVendorContactByAuthInfo(personAuthCallBackData.getOpenUserId(),personAuthCallBackData.getUserMobile());
        if (QiYueSuoPrivateCallBackTypeEnum.USER_AUTH_SUCCESS.equalsType(personAuthCallBackData.getCallbackType())) {
            log.info("[契约锁回调 - 个人授权认证] - 授权状态为成功,contactId:{}",vendorContact.getId());

            vendorContactService.update(new LambdaUpdateWrapper<VendorContact>()
                    .set(VendorContact::getSignState, SignStateEnum.SIGN_SUCCESS.getState())
                    .set(VendorContact::getSignAuthFailReason,"")
                    .eq(VendorContact::getId,vendorContact.getId()));
        } else if(QiYueSuoPrivateCallBackTypeEnum.USER_AUTH_FAILED.equalsType(personAuthCallBackData.getCallbackType())){
            log.info("[契约锁回调 - 个人授权认证] - 授权状态为失败，失败原因:{},contactId:{}",personAuthCallBackData.getRejectReason(),vendorContact.getId());
            vendorContactService.update(new LambdaUpdateWrapper<VendorContact>()
                    .set(VendorContact::getSignState, SignStateEnum.SIGN_FAILED.getState())
                    .set(VendorContact::getSignAuthFailReason,personAuthCallBackData.getRejectReason())
                    .eq(VendorContact::getId,vendorContact.getId()));
        } else if (QiYueSuoPrivateCallBackTypeEnum.USER_AUTH_EXPIRED.equalsType(personAuthCallBackData.getCallbackType())) {
            log.info("[契约锁回调 - 个人授权认证] - 授权状态为失效,contactId:{}",vendorContact.getId());
            vendorContactService.update(new LambdaUpdateWrapper<VendorContact>()
                    .set(VendorContact::getSignState, SignStateEnum.SIGN_EXPIRED.getState())
                    .set(VendorContact::getSignAuthFailReason,"您的认证已失效，请重新完成认证")
                    .eq(VendorContact::getId,vendorContact.getId()));
        }
    }

    @Override
    public void companyAuthCallBack(CallBackData callBackData) {
        CompanyAuthCallBackData companyAuthCallBackData = (CompanyAuthCallBackData) callBackData;
        Vendor vendor = getVendorByAuthInfo(companyAuthCallBackData.getOpenCompanyId(),companyAuthCallBackData.getRegisterNo());

        if (QiYueSuoPrivateCallBackTypeEnum.COMPANY_AUTH_SUCCESS.equalsType(companyAuthCallBackData.getCallbackType())) {
            log.info("[契约锁回调 - 公司授权认证] - 授权状态为成功,vendorId:{}",vendor.getId());

            vendorService.update(new LambdaUpdateWrapper<Vendor>()
                    .set(Vendor::getSignState, SignStateEnum.SIGN_SUCCESS.getState())
                    .set(Vendor::getSignAuthFailReason,"")
                    .eq(Vendor::getId,vendor.getId()));
        } else if(QiYueSuoPrivateCallBackTypeEnum.COMPANY_AUTH_FAILED.equalsType(companyAuthCallBackData.getCallbackType())){
            log.info("[契约锁回调 - 公司授权认证] - 授权状态为失败，失败原因:{},vendorId:{}",companyAuthCallBackData.getRejectStepDesc(),vendor.getId());

            vendorService.update(new LambdaUpdateWrapper<Vendor>()
                    .set(Vendor::getSignState, SignStateEnum.SIGN_FAILED.getState())
                    .set(Vendor::getSignAuthFailReason,companyAuthCallBackData.getRejectStepDesc())
                    .eq(Vendor::getId,vendor.getId()));
        } else if (QiYueSuoPrivateCallBackTypeEnum.COMPANY_AUTH_INVALID.equalsType(companyAuthCallBackData.getCallbackType())) {
            log.info("[契约锁回调 - 公司授权认证] - 授权状态为失效,vendorId:{}",vendor.getId());
            vendorService.update(new LambdaUpdateWrapper<Vendor>()
                    .set(Vendor::getSignState, SignStateEnum.SIGN_EXPIRED.getState())
                    .set(Vendor::getSignAuthFailReason,"您的认证已失效，请重新完成认证")
                    .eq(Vendor::getId,vendor.getId()));
        }
    }

    @Override
    public void signCallBack(CallBackData callBackData, Integer signatureType, Long businessId) {
        SignCallBackData signCallBackData = (SignCallBackData) callBackData;
        if (QiYueSuoPrivateCallBackTypeEnum.CONTRACT_SIGN.equalsType(signCallBackData.getCallbackType()) &&
                SignatureTypeEnum.PARTY_B.equalsType(signatureType)) {
            // 单方签署完成，且为乙方，则修改合同状态
            agreementService.update(new LambdaUpdateWrapper<Agreement>()
                    .set(Agreement::getAgreementState, AgreementStateEnum.PARTY_A_TO_SIGN.getState())
                    .eq(Agreement::getId,businessId));
        } else if (QiYueSuoPrivateCallBackTypeEnum.CONTRACT_COMPLETE.equalsType(signCallBackData.getCallbackType())) {
            // 文件签署完成
            agreementService.update(new LambdaUpdateWrapper<Agreement>()
                    .set(Agreement::getAgreementState, AgreementStateEnum.SIGN_SUCCESS.getState())
                    .eq(Agreement::getId,businessId));
        }
        contractCompatibilityService.syncSignCallback(businessId, signCallBackData);
    }

    /**
     * 获取供应商信息
     * @param openCompanyId
     * @param registerNo
     * @return
     */
    private Vendor getVendorByAuthInfo(String openCompanyId, String registerNo) {
        Vendor vendor = null;
        if (StringUtils.isNotBlank(openCompanyId)) {
            try{
                Long vendorId = Long.valueOf(openCompanyId);
                vendor = vendorService.getById(vendorId);
            }catch (NumberFormatException e) {
                log.warn("[契约锁回调] - openCompanyId[{}] 格式错误",openCompanyId);
            }
        }

        if (vendor == null && StringUtils.isNotBlank(registerNo)) {
            vendor = vendorService.getOne(new LambdaQueryWrapper<Vendor>()
                    .eq(Vendor::getSocialCreditCode,registerNo));
        }

        return Optional.ofNullable(vendor).orElseThrow(() -> new SignatureException(String.format("openCompanyId[%s]-registerNo[%s]对应的供应商信息不存在",openCompanyId,registerNo)));

    }

    /**
     * 获取供应商联系人
     * @param openUserId
     * @param userMobile
     * @return
     */
    private VendorContact getVendorContactByAuthInfo(String openUserId, String userMobile) {
        VendorContact vendorContact = null;
        if (StringUtils.isNotBlank(openUserId)) {
            // 先通过 openUserId
            try {
                Long contactId = Long.valueOf(openUserId);
                vendorContact = vendorContactService.getById(contactId);
            } catch (NumberFormatException e) {
                log.warn("[契约锁回调] - openUserId[{}] 格式错误",openUserId);
            }
        }

        if (vendorContact == null && StringUtils.isNotBlank(userMobile)) {
            vendorContact = vendorContactService.getOne(new LambdaQueryWrapper<VendorContact>()
                    .eq(VendorContact::getContactPhone,userMobile));
        }

        return Optional.ofNullable(vendorContact).orElseThrow(() -> new SignatureException(String.format("openUserId[%s]-userMobile[%s]对应的个人信息不存在",openUserId,userMobile)));
    }
}
