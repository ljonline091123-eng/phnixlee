package com.zhaocai.common.signature.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.common.signature.common.enums.SignStateEnum;
import com.zhaocai.common.signature.common.enums.SignatureTypeEnum;
import com.zhaocai.common.signature.domain.AgreementSignature;
import com.zhaocai.common.signature.dto.SignatureContact;
import com.zhaocai.common.signature.dto.callback.qysp.SignCallBackData;
import com.zhaocai.common.signature.mapper.AgreementSignatureMapper;
import com.zhaocai.common.signature.utils.JacksonUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 合同签章信息Service业务层处理
 *
 * @author chenming
 * @date 2024-08-28
 */
@Service
public class AgreementSignatureService extends ServiceImpl<AgreementSignatureMapper, AgreementSignature> implements IService<AgreementSignature> {

    /**
     * 新增合同签署信息
     * @param signatureContactList
     * @param signId
     */
    public void saveAgreementSignature(List<SignatureContact> signatureContactList,Long signId) {
        baseMapper.deleteBySignId(signId);

        List<AgreementSignature> agreementSignatures = new ArrayList<>(signatureContactList.size());
        for (SignatureContact signatureContact : signatureContactList) {
            AgreementSignature agreementSignature = new AgreementSignature();
            agreementSignature.setSignId(signId);

            agreementSignature.setSignatureType(signatureContact.getSignatureType());
            agreementSignature.setSignatureId(signatureContact.getSignatureId());
            agreementSignature.setSignatureName(signatureContact.getSignatureName());

            agreementSignature.setSignatureContactId(signatureContact.getContactId());
            agreementSignature.setSignatureContactName(signatureContact.getContactName());
            agreementSignature.setSignatureContactPhone(signatureContact.getContactPhone());
            agreementSignature.setSignState(SignStateEnum.CREATED.getState());
            agreementSignature.setCreateTime(new Date());

            agreementSignatures.add(agreementSignature);
        }

        this.saveBatch(agreementSignatures);
    }

    /**
     * 根据签署 id 和类型获取签署方信息
     * @param signatureType
     * @param signId
     * @return
     */
    public AgreementSignature getBySignIdAndType(SignatureTypeEnum signatureType, Long signId) {
        return this.getOne(new LambdaQueryWrapper<AgreementSignature>()
                .eq(AgreementSignature::getSignatureType,signatureType.getType())
                .eq(AgreementSignature::getSignId,signId));
    }

    /**
     * 修改签署方状态
     * @param id
     * @param signCallBackData
     */
    public void updateSignCallBackInfo(Long id, SignCallBackData signCallBackData) {
        this.update(new LambdaUpdateWrapper<AgreementSignature>()
                .set(AgreementSignature::getSignState,SignStateEnum.SIGN_SUCCESS.getState())
                .set(AgreementSignature::getSignCallbackMessage, JacksonUtil.toJsonString(signCallBackData))
                .set(AgreementSignature::getUpdateTime,new Date())
                .eq(AgreementSignature::getId,id));
    }

    /**
     * 根据签章 id 和接收人联系方式获取签署方信息
     * @param signId
     * @param receiverContact
     * @return
     */
    public AgreementSignature getBySignIdAndSignature(Long signId, String receiverContact) {
        return this.getOne(new LambdaQueryWrapper<AgreementSignature>()
                .eq(AgreementSignature::getSignId,signId)
                .eq(AgreementSignature::getSignatureContactPhone,receiverContact));
    }
}
